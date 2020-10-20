package org.coolreader.crengine;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.util.IOUtils;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.eink.sony.android.ebookdownloader.SonyBookSelector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class FileUtils {

    public static final Logger log = L.create("fileutils");

    private static void copyData(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

    /* Get uri related content real local file path. */
    public static String getPath(Context ctx, Uri uri) {
        String ret;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Android OS above sdk version 19.
                ret = getUriRealPathAboveKitkat(ctx, uri);
            } else {
                // Android OS below sdk version 19
                ret = getRealPath(ctx.getContentResolver(), uri, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("DREG", "FilePath Catch: " + e);
            ret = getFilePathFromURI(ctx, uri);
        }
        return ret;
    }

    private static String getFilePathFromURI(Context context, Uri contentUri) {
        //copy file and send new file path
        String fileName = getFileName(contentUri);
        if (!TextUtils.isEmpty(fileName)) {
            String TEMP_DIR_PATH = Environment.getExternalStorageDirectory().getPath();
            File copyFile = new File(TEMP_DIR_PATH + File.separator + fileName);
            Log.d("DREG", "FilePath copyFile: " + copyFile);
            copy(context, contentUri, copyFile);
            return copyFile.getAbsolutePath();
        }
        return null;
    }

    public static String getFileName(Uri uri) {
        if (uri == null) return null;
        String fileName = null;
        String path = uri.getPath();
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            fileName = path.substring(cut + 1);
        }
        return fileName;
    }

    public static void copy(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            copyData(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) { // IOException
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static String getUriRealPathAboveKitkat(Context ctx, Uri uri) {
        String ret = "";

        if (ctx != null && uri != null) {

            if (isContentUri(uri)) {
                if (isGooglePhotoDoc(uri.getAuthority())) {
                    ret = uri.getLastPathSegment();
                } else {
                    ret = getRealPath(ctx.getContentResolver(), uri, null);
                }
            } else if (isFileUri(uri)) {
                ret = uri.getPath();
            } else if (isDocumentUri(ctx, uri)) {

                // Get uri related document id.
                String documentId = DocumentsContract.getDocumentId(uri);

                // Get uri authority.
                String uriAuthority = uri.getAuthority();

                if (isMediaDoc(uriAuthority)) {
                    String idArr[] = documentId.split(":");
                    if (idArr.length == 2) {
                        // First item is document type.
                        String docType = idArr[0];

                        // Second item is document real id.
                        String realDocId = idArr[1];

                        // Get content uri by document type.
                        Uri mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        if ("image".equals(docType)) {
                            mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        } else if ("video".equals(docType)) {
                            mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        } else if ("audio".equals(docType)) {
                            mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        }

                        // Get where clause with real document id.
                        String whereClause = MediaStore.Images.Media._ID + " = " + realDocId;

                        ret = getRealPath(ctx.getContentResolver(), mediaContentUri, whereClause);
                    }

                } else if (isDownloadDoc(uriAuthority)) {
                    // Build download uri.
                    Uri downloadUri = Uri.parse("content://downloads/public_downloads");

                    // Append download document id at uri end.
                    Uri downloadUriAppendId = ContentUris.withAppendedId(downloadUri, Long.valueOf(documentId));

                    ret = getRealPath(ctx.getContentResolver(), downloadUriAppendId, null);

                } else if (isExternalStoreDoc(uriAuthority)) {
                    String idArr[] = documentId.split(":");
                    if (idArr.length == 2) {
                        String type = idArr[0];
                        String realDocId = idArr[1];

                        if ("primary".equalsIgnoreCase(type)) {
                            ret = Environment.getExternalStorageDirectory() + "/" + realDocId;
                        }
                    }
                }
            }
        }

        return ret;
    }

    /* Check whether this uri represent a document or not. */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean isDocumentUri(Context ctx, Uri uri) {
        boolean ret = false;
        if (ctx != null && uri != null) {
            ret = DocumentsContract.isDocumentUri(ctx, uri);
        }
        return ret;
    }

    /* Check whether this uri is a content uri or not.
     *  content uri like content://media/external/images/media/1302716
     *  */
    private static boolean isContentUri(Uri uri) {
        boolean ret = false;
        if (uri != null) {
            String uriSchema = uri.getScheme();
            if ("content".equalsIgnoreCase(uriSchema)) {
                ret = true;
            }
        }
        return ret;
    }

    /* Check whether this uri is a file uri or not.
     *  file uri like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
     * */
    private static boolean isFileUri(Uri uri) {
        boolean ret = false;
        if (uri != null) {
            String uriSchema = uri.getScheme();
            if ("file".equalsIgnoreCase(uriSchema)) {
                ret = true;
            }
        }
        return ret;
    }

    /* Check whether this document is provided by ExternalStorageProvider. */
    private static boolean isExternalStoreDoc(String uriAuthority) {
        boolean ret = false;

        if ("com.android.externalstorage.documents".equals(uriAuthority)) {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by DownloadsProvider. */
    private static boolean isDownloadDoc(String uriAuthority) {
        boolean ret = false;

        if ("com.android.providers.downloads.documents".equals(uriAuthority)) {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by MediaProvider. */
    private static boolean isMediaDoc(String uriAuthority) {
        boolean ret = false;

        if ("com.android.providers.media.documents".equals(uriAuthority)) {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by google photos. */
    private static boolean isGooglePhotoDoc(String uriAuthority) {
        boolean ret = false;

        if ("com.google.android.apps.photos.content".equals(uriAuthority)) {
            ret = true;
        }

        return ret;
    }

    /* Return uri represented document file real local path.*/
    @SuppressLint("Recycle")
    private static String getRealPath(ContentResolver contentResolver, Uri uri, String whereClause) {
        String ret = "";

        // Query the uri with condition.
        Cursor cursor = contentResolver.query(uri, null, whereClause, null, null);

        if (cursor != null) {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {

                // Get columns name by uri type.
                String columnName = MediaStore.Images.Media.DATA;

                if (uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Images.Media.DATA;
                } else if (uri == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Audio.Media.DATA;
                } else if (uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Video.Media.DATA;
                }

                // Get column index.
                int columnIndex = cursor.getColumnIndex(columnName);

                // Get column value which is the uri related file local path.
                ret = cursor.getString(columnIndex);
            }
        }

        return ret;
    }

    public static String getRealPathFromURI(ContentResolver contentResolver, Uri contentURI) {
        String result;
        Cursor cursor = contentResolver.query(contentURI, null, null, null, null);
        if (cursor == null) { //checking
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public static boolean isArchive(File f) {
        int fileSignature = 0;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "r");
            fileSignature = raf.readInt();
        } catch (IOException e) {
            // handle if you like
        } finally {
            IOUtils.closeQuietly(raf);
        }
        return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
    }

    public static void fileDownloadEndThenOpen(
            String type, String initial_url, String url, File file,
            CoolReader mActivity, Engine mEngine, String lastOPDScatalogURL, Scanner mScanner,
            FileInfo downloadDir, FileInfo fileOrDir, FileInfo currDirectory,
            String annot
      ) {
        if (DeviceInfo.EINK_SONY) {
            SonyBookSelector selector = new SonyBookSelector(mActivity);
            selector.notifyScanner(file.getAbsolutePath());
        }
        mEngine.hideProgress();
        mActivity.getDB().updateOPDSCatalog(lastOPDScatalogURL, "books_downloaded", "max");
        //fileOrDir.parent.pathname // @opds:http://89.179.127.112/opds/search/books/u/0/
        FileInfo fi = new FileInfo(file);
        boolean isArch = FileUtils.isArchive(file);
        FileInfo dir = mScanner.findParent(fi, downloadDir);
        if (dir == null)
            dir = downloadDir;
        String sPath = file.getAbsolutePath();
        String sPathZ = sPath;
        log.d("onDownloadEnd: sPath = " + sPath);
        if ((isArch)&&(!sPathZ.toLowerCase().endsWith(".zip"))
                &&(!sPathZ.toLowerCase().endsWith(".epub"))) {
            int i = 0;
            sPathZ = sPath + ".zip";
            File fileZ = new File(sPathZ);
            boolean bExists = fileZ.exists();
            while (bExists) {
                i++;
                sPathZ = sPath + " ("+i+").zip";
                fileZ = new File(sPathZ);
                bExists = fileZ.exists();
            }
            file.renameTo(fileZ);
        }
        mScanner.listDirectory(dir);
        FileInfo item1 = dir.findItemByPathName(sPathZ);
        if (item1 == null) item1 = new FileInfo(sPathZ);
        final FileInfo item = item1;
        log.d("onDownloadEnd: sPathZ = " + sPathZ);
        BackgroundThread.ensureGUI();
        item.opdsLink = initial_url;
        fileOrDir.pathnameR = item.pathname;
        fileOrDir.arcnameR = item.arcname;
        fileOrDir.pathR = item.path;
        if (StrUtils.isEmptyStr(fileOrDir.getFilename())) fileOrDir.setFilename(item.getFilename());
        mActivity.getDB().saveBookInfo(new BookInfo(fileOrDir));
        mActivity.getDB().flush();
        if (item.getTitle() == null) {
            if (!mEngine.scanBookProperties(item))
                mActivity.showToast("Could not read file properties, possibly format is unsupported");
            if (item.getTitle() != null) {
                mActivity.getDB().saveBookInfo(new BookInfo(item));
                mActivity.getDB().flush();
            }
        }
        if (item != null) {
            Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), item,
                    bookInfo -> {
                        BookInfo bif2 = bookInfo;

                        if (bif2 == null) bif2 = new BookInfo(item);
                        final BookInfo bif = bif2;
                        if (StrUtils.isEmptyStr(bif.getFileInfo().annotation))
                            bif.getFileInfo().setAnnotation(annot);
                        if (bif.getFileInfo().getTitle() != null) {
                            if (mActivity.settings().getBool(Settings.PROP_APP_MARK_DOWNLOADED_TO_READ, false)) {
                                bif.getFileInfo().setReadingState(FileInfo.STATE_TO_READ);
                                mActivity.showToast(item.pathname+" "+
                                        mActivity.getString(R.string.mi_opds_mark_as_toread));
                            }
                            mActivity.getDB().saveBookInfo(bif);
                            mActivity.getDB().flush();
                        }
                        FileInfo dir1 =bif.getFileInfo().parent;
                        if (dir1 == null) {
                            dir1 = mScanner.findParent(bif.getFileInfo(), downloadDir);
                            bif.getFileInfo().parent = dir1;
                        }
                        if (dir1!=null) {
                            final FileInfo dir2 = dir1;
                            bif.getFileInfo().setFileProperties(bif.getFileInfo());
                            dir2.setFile(bif.getFileInfo());
                            mActivity.directoryUpdated(dir2, bif.getFileInfo());
                        }
                        mActivity.showBookInfo(bif, BookInfoDialog.OPDS_FINAL_INFO, currDirectory, fileOrDir);
                    });
        }
        else
            mActivity.loadDocument(fi, true);
    }

}