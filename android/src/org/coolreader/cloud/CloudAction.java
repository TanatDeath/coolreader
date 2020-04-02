package org.coolreader.cloud;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.dropbox.core.v2.users.FullAccount;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.dropbox.DBXConfig;
import org.coolreader.cloud.dropbox.DBXPerformAction;
import org.coolreader.cloud.yandex.YNDConfig;
import org.coolreader.cloud.yandex.YNDListFiles;
import org.coolreader.cloud.yandex.YNDPerformAction;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.ChooseReadingPosDlg;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.PictureCameDialog;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.StrUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CloudAction {

    private static final String TAG = "CloudAction";

    public static final int NO_ACTION = 0;
    public static final int DBX_GET_CURRENT_ACCOUNT = 20000;
    public static final int DBX_LIST_FOLDER = 20001;
    public static final int DBX_LIST_FOLDER_THEN_OPEN_DLG = 20002;
    public static final int DBX_LIST_FOLDER_IN_DLG = 20003;
    public static final int DBX_DOWNLOAD_FILE = 20004;

    public static final int YND_LIST_FOLDER = 20101;
    public static final int YND_LIST_FOLDER_THEN_OPEN_DLG = 20102;
    public static final int YND_LIST_FOLDER_IN_DLG = 20103;
    public static final int YND_GET_DOWNLOAD_LINK = 20104;
    public static final int YND_DOWNLOAD_FILE = 20105;
    public static final int YND_CHECK_CR_FOLDER = 20106;
    public static final int YND_CREATE_CR_FOLDER = 20107;
    public static final int YND_SAVE_TO_FILE_GET_LINK = 20108;
    public static final int YND_SAVE_STRING_TO_FILE = 20109;
    public static final int YND_LIST_JSON_FILES = 20110;
    public static final int YND_DOWNLOAD_FILE_TO_STRING = 20111;
    public static final String CLOUD_COMPLETE_LIST_FOLDER_RESULT = "ListFolderResult";
    public static final String CLOUD_COMPLETE_FULL_ACCOUNT = "FullAccount";
    public static final String CLOUD_COMPLETE_GET_DOWNLOAD_LINK = "GetDownloadLink";
    public static final String CLOUD_COMPLETE_CHECK_CR_FOLDER = "CheckCrFolder";
    public static final String CLOUD_COMPLETE_CREATE_CR_FOLDER = "CreateCrFolder";
    public static final String CLOUD_COMPLETE_DOWNLOAD_FILE = "DownloadFile";
    public static final String CLOUD_COMPLETE_SAVE_TO_FILE_GET_LINK = "SaveToFileGetLink";
    public static final String CLOUD_COMPLETE_SAVE_STRING_TO_FILE = "SaveStringToFile";
    public static final String CLOUD_COMPLETE_LIST_JSON_FILES = "ListJsonFiles";
    public static final String CLOUD_COMPLETE_DOWNLOAD_FILE_TO_STRING = "DownloadFileToString";

    public int action; // action, that will be performed
    public String param; // param(s) passed to an action...
    public String param2; // param(s) passed to an action...
    public String bookCRC; // Book's CRC
    public com.dropbox.core.v2.files.Metadata mDbxMd; // metadata for dropbox
    public CloudFileInfo mYndMd; // metadata for yandex
    public String mFile;

    public CoolReader mActivity;

    public CloudAction(CoolReader cr, int a) {
        action = a;
        param = "";
        mActivity = cr;
    }

    public CloudAction(CoolReader cr, int a, String p) {
        action = a;
        param = p;
        mActivity = cr;
    }

    public CloudAction(CoolReader cr, int a, String p, String p2) {
        action = a;
        param = p;
        mActivity = cr;
        param2 = p2;
    }

    public CloudAction(CoolReader cr, int a, String p, com.dropbox.core.v2.files.Metadata md, String sFile) {
        action = a;
        param = p;
        mDbxMd = md;
        mActivity = cr;
        mFile = sFile;
    }

    public CloudAction(CoolReader cr, int a, String p, CloudFileInfo md, String sFile) {
        action = a;
        param = p;
        mYndMd = md;
        mActivity = cr;
        mFile = sFile;
    }

    @SuppressWarnings("unchecked")
    public static void onDBXComplete(CoolReader cr, DBXPerformAction a, String res, Object o, Object dlg) {
        System.out.println("DBX:" + a.mCurAction.action);
        System.out.println("DBX:" + a.mCurAction.param);
        if (CLOUD_COMPLETE_FULL_ACCOUNT.equals(res)) {
            FullAccount fa = (FullAccount) o;
        }
        if ((CLOUD_COMPLETE_LIST_FOLDER_RESULT.equals(res))&&(a.mCurAction.action == CloudAction.DBX_LIST_FOLDER_THEN_OPEN_DLG)) {
            List<com.dropbox.core.v2.files.Metadata> l = (List<com.dropbox.core.v2.files.Metadata>) o;
            onDBXListFolderResultThenOpenDlg(cr, l);
        }
        if ((CLOUD_COMPLETE_LIST_FOLDER_RESULT.equals(res))&&(a.mCurAction.action == CloudAction.DBX_LIST_FOLDER_IN_DLG)) {
            List<com.dropbox.core.v2.files.Metadata> l = (List<com.dropbox.core.v2.files.Metadata>) o;
            onDBXListFolderResultInDlg(cr, l, (OpenBookFromCloudDlg) dlg);
        }
        a.DoNextAction();
    }

    public static void onDBXError(CoolReader cr, DBXPerformAction a, String res, Exception e) {
        Log.e(e.getClass().getName(), "Cloud operation error: "+res, e);
        final CoolReader crf = cr;
        final String resf = res;
        BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                if (crf!=null) crf.showToast("Cloud operation error: "+resf);
            }}, 200);
    }

    public static void onDBXListFolderResultThenOpenDlg(CoolReader cr,
                                                        List<com.dropbox.core.v2.files.Metadata> lfr) {
        OpenBookFromCloudDlg dlg = new OpenBookFromCloudDlg(cr, lfr);
        dlg.show();
    }

    public static void onDBXListFolderResultInDlg(CoolReader cr, List<com.dropbox.core.v2.files.Metadata> lfr,
                                                  OpenBookFromCloudDlg dlg) {
        if (dlg.isShowing()) {
            dlg.setDBXLfrList(lfr);
            dlg.listUpdated();
        }
    }

    public static void onYNDListFolderResultThenOpenDlg(CoolReader cr, YNDListFiles lfr) {
        OpenBookFromCloudDlg dlg = new OpenBookFromCloudDlg(cr, lfr);
        dlg.show();
    }

    public static void onYNDListFolderResultInDlg(CoolReader cr, YNDListFiles lfr, OpenBookFromCloudDlg dlg) {
        if (dlg.isShowing()) {
            dlg.setYNDLfrList(lfr);
            dlg.listUpdated();
        }
    }

    public static void dbxOpenBookDialog(final CoolReader cr) {
        try {
            cr.showToast(R.string.cloud_begin);
            if (!DBXConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.DBX_GET_CURRENT_ACCOUNT);
            al.add(ca1);
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_LIST_FOLDER_THEN_OPEN_DLG);
            al.add(ca2);
            final DBXPerformAction a = new DBXPerformAction(cr, al, new DBXPerformAction.Callback() {
                @Override
                public void onComplete(DBXPerformAction a, String res, Object o) {
                    CloudAction.onDBXComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(DBXPerformAction a, String res, Exception e) {
                    CloudAction.onDBXError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void onYNDComplete(CoolReader cr, YNDPerformAction a, String res, Object o, Object dlg) {
        System.out.println("YND:" + a.mCurAction.action);
        System.out.println("YND:" + a.mCurAction.param);
        if ((CLOUD_COMPLETE_LIST_FOLDER_RESULT.equals(res))&&(a.mCurAction.action == CloudAction.YND_LIST_FOLDER_THEN_OPEN_DLG)) {
            final YNDListFiles l = (YNDListFiles) o;
            final CoolReader crf = cr;
            BackgroundThread.instance().postGUI(new Runnable() {
                @Override
                public void run() {
                    onYNDListFolderResultThenOpenDlg(crf, l);
                }}, 200);
        }
        if ((CLOUD_COMPLETE_LIST_FOLDER_RESULT.equals(res))&&(a.mCurAction.action == CloudAction.YND_LIST_FOLDER_IN_DLG)) {
            final YNDListFiles l = (YNDListFiles) o;
            final CoolReader crf = cr;
            final OpenBookFromCloudDlg dlgf = (OpenBookFromCloudDlg) dlg;
            BackgroundThread.instance().postGUI(new Runnable() {
                @Override
                public void run() {
                    onYNDListFolderResultInDlg(crf, l, dlgf);
                }}, 200);
        }
        if ((CLOUD_COMPLETE_GET_DOWNLOAD_LINK.equals(res))&&(a.mCurAction.action == CloudAction.YND_GET_DOWNLOAD_LINK)) {
            if (a.mActionList.size()>0) {
                // setting download link param
                a.mActionList.get(0).param2 = a.mCurAction.param2;
            }
        }
        if ((CLOUD_COMPLETE_CHECK_CR_FOLDER.equals(res))&&(a.mCurAction.action == CloudAction.YND_CHECK_CR_FOLDER)) {
            if (a.mActionList.size()>0) {
                // setting download link param
                a.mActionList.get(0).param2 = "skip";
                if (o instanceof YNDListFiles) {
                    YNDListFiles lf = (YNDListFiles) o;
                    boolean needCreate = (lf.fileList.size()==0);
                    if (lf.fileList.size()==1)
                        if (!StrUtils.getNonEmptyStr(lf.fileList.get(0).type,true).equals("dir")) needCreate = true;
                    if (needCreate) a.mActionList.get(0).param2 = "create";
                }
            }
        }
        if ((CLOUD_COMPLETE_SAVE_TO_FILE_GET_LINK.equals(res))&&(a.mCurAction.action == CloudAction.YND_SAVE_TO_FILE_GET_LINK)) {
            if (a.mActionList.size()>0) {
                // setting download link param
                if (o instanceof String)
                    a.mActionList.get(0).param2 = (String) o;
            }
        }
        if ((CLOUD_COMPLETE_LIST_JSON_FILES.equals(res))&&(a.mCurAction.action == CloudAction.YND_LIST_JSON_FILES)) {
            if (o instanceof YNDListFiles) {
                final YNDListFiles o1 = (YNDListFiles) o;
                if (StrUtils.getNonEmptyStr(a.mCurAction.param,true).equals("_rpos_")) {
                    BackgroundThread.instance().postGUI(new Runnable() {
                        @Override
                        public void run() {
                            ChooseReadingPosDlg dlg2 = new ChooseReadingPosDlg(cr, o1);
                            dlg2.show();
                        }}, 200);
                }
            }
        }
        if ((CLOUD_COMPLETE_DOWNLOAD_FILE_TO_STRING.equals(res))&&(a.mCurAction.action == CloudAction.YND_DOWNLOAD_FILE_TO_STRING)) {
            if (o instanceof String) {
                final String sContent = (String) o;
                    Log.i("CLOUD", "Complete action: "+ a.mCurAction.action);
                    if (a.mCurAction.action == YND_DOWNLOAD_FILE_TO_STRING) {
                        BackgroundThread.instance().postGUI(new Runnable() {
                            @Override
                            public void run() {
                                cr.showToast(cr.getString(R.string.cloud_ok) + ": successfully loaded");
                                int iSaveType = CloudSyncFolder.CLOUD_SAVE_READING_POS;
                                if (a.mCurAction.param.contains("_bmk_")) iSaveType = CloudSyncFolder.CLOUD_SAVE_BOOKMARKS;
                                CloudSyncFolder.applyRPosOrBookmarks(cr, iSaveType, ((String) o),
                                    false);
                            }}, 200);
                    }

            }
        }
        a.DoNextAction();
    }

    public static void onYNDError(CoolReader cr, YNDPerformAction a, String res, Exception e) {
        Log.e(e.getClass().getName(), "Cloud operation error: "+res, e);
        final CoolReader crf = cr;
        final String resf = res;
        BackgroundThread.instance().postGUI(new Runnable() {
            @Override
            public void run() {
                if (crf!=null) crf.showToast("Cloud operation error: "+resf);
            }}, 200);
    }

    public static void yndOpenBookDialog(final CoolReader cr) {
        try {
            cr.showToast(R.string.cloud_begin);
            if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.YND_LIST_FOLDER_THEN_OPEN_DLG);
            al.add(ca1);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndSaveJsonFile(final CoolReader cr, String sFName, String prettyJson) {
        try {
            cr.showToast(R.string.cloud_begin);
            if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.YND_CHECK_CR_FOLDER);
            al.add(ca1);
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_CREATE_CR_FOLDER);
            al.add(ca2);
            CloudAction ca3 = new CloudAction(cr, CloudAction.YND_SAVE_TO_FILE_GET_LINK);
            ca3.param = sFName;
            al.add(ca3);
            CloudAction ca4 = new CloudAction(cr, CloudAction.YND_SAVE_STRING_TO_FILE);
            ca4.param = prettyJson;
            al.add(ca4);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndLoadJsonFileList(final CoolReader cr, String fileMark, String sCRC) {
        try {
            cr.showToast(R.string.cloud_begin);
            if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca1 = new CloudAction(cr, CloudAction.YND_CHECK_CR_FOLDER);
            al.add(ca1);
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_LIST_JSON_FILES);
            ca2.param = fileMark;
            // ca2.param2 - will contain skip if folder exists. Means "skip create, because exists"
            ca2.bookCRC = sCRC;
            al.add(ca2);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void dbxLoadFolderContents(final CoolReader cr, final OpenBookFromCloudDlg dlg,
                                             final String sFolder, final String sFindStr) {
        try {
            cr.showToast(R.string.cloud_begin);
            if (StrUtils.isEmptyStr(sFolder))
                if (!DBXConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_LIST_FOLDER_IN_DLG, sFolder, sFindStr);
            al.add(ca2);
            final DBXPerformAction a = new DBXPerformAction(cr, al, new DBXPerformAction.Callback() {
                @Override
                public void onComplete(DBXPerformAction a, String res, Object o) {
                    CloudAction.onDBXComplete(cr, a, res, o, dlg);
                }

                @Override
                public void onError(DBXPerformAction a, String res, Exception e) {
                    CloudAction.onDBXError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void dbxDownloadFile(final CoolReader cr, final OpenBookFromCloudDlg dlg,
            final String sFolder, com.dropbox.core.v2.files.Metadata md) {
        try {
            final FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
            final String fName = downloadDir.pathname+"/Dropbox/"+md.getName();
            final File fPath = new File(downloadDir.pathname+"/Dropbox/");
            if (!fPath.exists()) fPath.mkdir();
            final File fBook = new File(fName);
            cr.showToast(R.string.cloud_begin);
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.DBX_DOWNLOAD_FILE,
                    (sFolder+"/"+md.getName()).replace("//","/"), md, fName);
            al.add(ca2);
            final DBXPerformAction a = new DBXPerformAction(cr, al, new DBXPerformAction.Callback() {
                @Override
                public void onComplete(DBXPerformAction a, String res, Object o) {
                    CloudAction.onDBXComplete(cr, a, res, o, dlg);
                    cr.showToast(cr.getString(R.string.cloud_ok) + ": successfully saved to " + fName);
                    if (PictureCameDialog.isFileIsPicture(fName)) {
                        cr.pictureCame(fName);
                    } else {
                        FileInfo fi = new FileInfo(fBook);
                        FileInfo dir = Services.getScanner().findParent(fi, downloadDir);
                        if (dir == null)
                            dir = downloadDir;
                        Services.getScanner().listDirectory(dir);
                        FileInfo item = dir.findItemByPathName(fBook.getAbsolutePath());
                        if (item != null)
                            cr.loadDocument(item);
                        else
                            cr.loadDocument(fi);
                    }
                }

                @Override
                public void onError(DBXPerformAction a, String res, Exception e) {
                    CloudAction.onDBXError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("DBBOX err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndLoadFolderContents(final CoolReader cr, final OpenBookFromCloudDlg dlg,
                                             final String sFolder, final String sFindStr) {
        try {
            cr.showToast(R.string.cloud_begin);
            if (StrUtils.isEmptyStr(sFolder))
                if (!YNDConfig.init(cr)) return;
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_LIST_FOLDER_IN_DLG, sFolder, sFindStr);
            al.add(ca2);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, dlg);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void yndDownloadFile(final CoolReader cr, final OpenBookFromCloudDlg dlg,
                                       final String sFolder, CloudFileInfo md) {
        try {
            final FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
            final String fName = downloadDir.pathname+"/YandexDisc/"+md.name;
            final File fPath = new File(downloadDir.pathname+"/YandexDisc");
            if (!fPath.exists()) fPath.mkdir();
            final File fBook = new File(fName);
            cr.showToast(R.string.cloud_begin);
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_GET_DOWNLOAD_LINK,
                    (sFolder+"/"+md.name).replace("//","/"), md, fName);
            al.add(ca2);
            CloudAction ca3 = new CloudAction(cr, CloudAction.YND_DOWNLOAD_FILE,
                    (sFolder+"/"+md.name).replace("//","/"), md, fName);
            al.add(ca3);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    if (a.mCurAction.action == YND_DOWNLOAD_FILE) {
                        BackgroundThread.instance().postGUI(new Runnable() {
                            @Override
                            public void run() {
                                cr.showToast(cr.getString(R.string.cloud_ok) + ": successfully saved to " + fName);
                                if (PictureCameDialog.isFileIsPicture(fName)) {
                                    cr.pictureCame(fName);
                                } else {
                                    FileInfo fi = new FileInfo(fBook);
                                    FileInfo dir = Services.getScanner().findParent(fi, downloadDir);
                                    if (dir == null)
                                        dir = downloadDir;
                                    Services.getScanner().listDirectory(dir);
                                    FileInfo item = dir.findItemByPathName(fBook.getAbsolutePath());
                                    if (item != null)
                                        cr.loadDocument(item);
                                    else
                                        cr.loadDocument(fi);
                                }
                            }
                        }, 200);
                    }
                    CloudAction.onYNDComplete(cr, a, res, o, dlg);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }

    public static void emailSendBook(CoolReader cr, BookInfo bi) {
        Log.d(TAG, "Starting emailSendBook...");

        final ReaderView rv = cr.getReaderView();
        if ((rv == null)&&(bi == null)) {
            cr.showToast(cr.getString(R.string.cloud_error) + ": book was not found");
            return;
        }
        try {
            BookInfo bookinfo = bi;
            if (bi == null) bookinfo = rv.getBookInfo();
            String sBookFN = bookinfo.getFileInfo().pathname;
            if (!StrUtils.isEmptyStr(bookinfo.getFileInfo().arcname))
                sBookFN = bookinfo.getFileInfo().arcname;
            final String sBookFName = sBookFN;
            File bookFile = new File(sBookFName);
            //Uri path = Uri.fromFile(bookFile);
            String sPkg = cr.getApplicationContext().getPackageName();
            Uri path = FileProvider.getUriForFile(cr, "org.coolreader.mod.plotn.fileprovider", bookFile);
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            // set the type to 'email'
            emailIntent.setType("vnd.android.cursor.dir/email");
            // the attachment
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);
            // the mail subject
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, bookinfo.getFileInfo().title + " " +
                bookinfo.getFileInfo().getAuthors());
            cr.startActivity(Intent.createChooser(emailIntent, "Send book by email..."));
        } catch (Exception e) {
            cr.showToast("Could not email book: "+e.getMessage());
            e.printStackTrace();
        }
    }

    public static void yndLoadJsonFile(CoolReader cr, int iSaveType, String filePath, boolean bQuiet,
                                String name, int cloudMode) {
        try {
            cr.showToast(R.string.cloud_begin);
            ArrayList<CloudAction> al = new ArrayList<CloudAction>();
            CloudAction ca2 = new CloudAction(cr, CloudAction.YND_GET_DOWNLOAD_LINK, filePath);
            al.add(ca2);
            CloudAction ca3 = new CloudAction(cr, CloudAction.YND_DOWNLOAD_FILE_TO_STRING, filePath);
            al.add(ca3);
            final YNDPerformAction a = new YNDPerformAction(cr, al, new YNDPerformAction.Callback() {
                @Override
                public void onComplete(YNDPerformAction a, String res, Object o) {
                    CloudAction.onYNDComplete(cr, a, res, o, null);
                }

                @Override
                public void onError(YNDPerformAction a, String res, Exception e) {
                    CloudAction.onYNDError(cr, a, res, e);
                }
            });
            a.DoNextAction();
        } catch (Exception e) {
            cr.showToast(cr.getString(R.string.cloud_begin)+" "+e.getClass().toString()+" "+e.getMessage());
            System.out.println("YND err:"+ e.getClass().toString()+" "+e.getMessage());
        }
    }
}
