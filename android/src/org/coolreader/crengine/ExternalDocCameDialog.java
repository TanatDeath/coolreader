package org.coolreader.crengine;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.utils.FileUtils;
import org.coolreader.utils.StorageDirectory;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ExternalDocCameDialog extends BaseDialog {

	private CoolReader mActivity;
	private int mWindowSize;

	private String fileDir;
	private String fileName;
	private String fileExt;

	private String mLogFileRoot = "";

	private LayoutInflater mInflater;
	private String stype;
	private Uri uri;
	private String mFileToOpen;
	private InputStream istream = null;
	private InputStream istreamTxt = null;
	private String sUri = "";
	private String sUriUpd = "";
	public String sExistingName = "";
	private Document docJsoup = null;
	private Boolean bThisIsHTML = true;
	private String sTitle = "";

	private TableRow trSaveToLib;
	private TableRow trMoveToLib;
	private Button btnSave;
	private Button btnMove;

	public static final Logger log = L.create("edcd");

	public String extractSuggestedName(String sText) {
		String sLastSeg = StrUtils.getNonEmptyStr(sText,true);
		int len1 = sLastSeg.split("/").length;
		if (len1 > 1) sLastSeg = sLastSeg.split("/")[len1 - 1];
		sLastSeg = sLastSeg.replace("&", "_").replace("#", "_")
				.replace("?", "_").replace("%", "_")
				.replace(":","/")
				.replace(".","_")
				.replace("/","_").replace("\\","")
				.replace("\\\\","");
		return sLastSeg.trim();
	}

	public String replaceInvalidChars(String s) {
		return StrUtils.getNonEmptyStr(s, true).replaceAll("[\\\\/:*?\"<>|]", "");
	}

	private boolean checkIntentImgUrl(String sUrl) {
		// link example:
		//https://www.google.com/imgres?imgurl=https://images.pexels.com/photos/1226302/pexels-photo-1226302.jpeg?auto%3Dcompress%26cs%3Dtinysrgb%26dpr%3D1%26w%3D500&imgrefurl=https://www.pexels.com/search/galaxy%2520wallpaper/&tbnid=ch8IfDIaCMZBvM&vet=1&docid=nvJrYCoGRBCahM&w=500&h=667&q=wallpaper&source=sh/x/im
		if (sUrl.contains("imgurl=")) {
			sUrl=sUrl.substring(sUrl.indexOf("imgurl=")+7);
			if (sUrl.contains("?")) sUrl = sUrl.split("\\?")[0];
			final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
			intent.putExtra(android.content.Intent.EXTRA_TEXT, sUrl);
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					mActivity.processIntent(intent), 200));
			return true;
		}
		return false;
	}

	public void doDownloadHttp(String sLink) {
		if (!StrUtils.isEmptyStr(sLink)) {
			HttpUrl hurl = HttpUrl.parse(sLink);
			if (hurl == null) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					mActivity.showToast("Download error - cannot parse link");
					onPositiveButtonClick();
				}, 100));
				return;
			}
			final HttpUrl.Builder urlBuilder = hurl.newBuilder();
			final String url = urlBuilder.build().toString();
			Request request = new Request.Builder()
					.url(url)
					.build();
			OkHttpClient client = new OkHttpClient();
			Call call = client.newCall(request);
			call.enqueue(new okhttp3.Callback() {
				public void onResponse(Call call, Response response)
						throws IOException {
					String sUrl = response.request().url().toString();
					if (checkIntentImgUrl(sUrl)) {
						BackgroundThread.instance().postBackground(() ->
								BackgroundThread.instance().postGUI(() -> onPositiveButtonClick(), 100));
						return;
					}
					stype = StrUtils.getNonEmptyStr(response.body().contentType().toString(),true);
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> tvDocType.setText(stype), 100));
					InputStream is = response.body().byteStream();
					ByteArrayOutputStream baos = Utils.inputStreamToBaos(is);
					istream = new ByteArrayInputStream(baos.toByteArray());
					if (stype.startsWith("image/")) {
						BackgroundThread.instance().postBackground(() ->
								BackgroundThread.instance().postGUI(() -> {
							PictureCameDialog dlg = new PictureCameDialog(mActivity, istream, stype, extractSuggestedName(sLink));
							onPositiveButtonClick();
							dlg.show();
						}, 100));
					} else {
						BackgroundThread.instance().postBackground(() -> {
							try {
								docJsoup = Jsoup.parse(urlBuilder.build().url(), 180000); // three minutes
								istreamTxt = new ByteArrayInputStream(docJsoup.body().text().replace((char)0,' ').getBytes());
								Elements resultLinks = docJsoup.select("html > head > title");
								if (resultLinks.size()>0) sTitle = resultLinks.text();
								BackgroundThread.instance().postBackground(() ->
										BackgroundThread.instance().postGUI(() -> {
									tvExtPath.setText(tvExtPath.getText()+"; "+sTitle);
									fileName = replaceInvalidChars(extractSuggestedName(sTitle));
								}, 100));
							} catch (Exception e) {
								docJsoup = null;
								BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
									btnAsText.setEnabled(false);
									log.e(mActivity.getString(R.string.error_open_as_text)+": "+
											e.getClass().getSimpleName()+" "+e.getMessage());
									mActivity.showToast(mActivity.getString(R.string.error_open_as_text)+": "+
											e.getClass().getSimpleName()+" "+e.getMessage());
								}, 100));
							}
						});
					}
				}

				public void onFailure(Call call, IOException e) {
					final IOException ef = e;
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						log.e("Download error: "+ef.getMessage()+
								" ["+ef.getClass().getSimpleName()+"]");
						mActivity.showToast("Download error: "+ef.getMessage()+
								" ["+ef.getClass().getSimpleName()+"]");
						onPositiveButtonClick();
					}, 200));
				}
			});
		}
	}

	public ExternalDocCameDialog(CoolReader activity, String stype, Object obj, String fileToOpen)
	{
		super(activity, activity.getString(R.string.external_doc_came), false, true);
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		this.mActivity = activity;
		this.mFileToOpen = fileToOpen;
		mLogFileRoot = activity.getSettingsFileF(0).getParent() + "/";
		this.stype = stype;
		this.uri = null;
		if (obj instanceof Uri) this.uri = (Uri) obj;
		if (obj instanceof String) sUri = (String) obj;
		if(getWindow().getAttributes().softInputMode==WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
		    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		}
	}

	@Override
	protected void onCreate() {
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        super.onCreate();
		L.v("ExternalDocCameDialog is created");
	}

	TextView tvExtPath;
	TextView tvDocType;
	Button btnOpenFromStream;
	TextView tvExistingPath;
	Button btnOpenExisting;
	Button btnAsHTML;
	Button btnAsText;

	private String queryName(ContentResolver resolver, Uri uri) {
		Cursor returnCursor =
				resolver.query(uri, null, null, null, null);
		if (returnCursor == null) return "";
		try {
			int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
			returnCursor.moveToFirst();
			String name = returnCursor.getString(nameIndex);
			returnCursor.close();
			return name;
		} catch (Exception e) {
			return "";
		}
	}

	private String queryPath(ContentResolver resolver, Uri uri) {
		Cursor returnCursor =
				resolver.query(uri, null, null, null, null);
		if (returnCursor == null) return uri.getPath();
		returnCursor.moveToFirst();
		try {
			int idx = returnCursor
					.getColumnIndex(MediaStore.Files.FileColumns.DATA);
			return returnCursor.getString(idx);
		} catch (Exception e) {
		}
		return "";
	}

	private String queryPathM(ContentResolver resolver, Uri uri) {
		Cursor returnCursor =
				resolver.query(uri, null, null, null, null);
		if (returnCursor == null) return uri.getPath();
		returnCursor.moveToFirst();
		try {
			int idx = returnCursor
					.getColumnIndex(MediaStore.MediaColumns.DATA);
			return returnCursor.getString(idx);
		} catch (Exception e) {
		}
		return "";
	}

	private void hideExistingFileControls(ViewGroup view) {
		TableLayout tl = view.findViewById(R.id.table);
		TableRow trowExists1 = view.findViewById(R.id.trow_file_exists1);
		TableRow trowExists2 = view.findViewById(R.id.trow_file_exists2);
		TableRow trowExists3 = view.findViewById(R.id.trow_file_exists3);
		tl.removeView(trowExists1);
		tl.removeView(trowExists2);
		tl.removeView(trowExists3);
		Utils.removeView(trMoveToLib);
	}

	private void hideExistingHttpControls(ViewGroup view) {
		TableLayout tl = view.findViewById(R.id.table);
		TableRow trowExists1 = view.findViewById(R.id.trow_text_or_html);
		TableRow trowExists2 = view.findViewById(R.id.trow_text_or_html2);
		tl.removeView(trowExists1);
		tl.removeView(trowExists2);
	}

	private void hideExistingFromStreamControls(ViewGroup view) {
		TableLayout tl = view.findViewById(R.id.table);
		TableRow trowExists1 = view.findViewById(R.id.trow_from_stream1);
		TableRow trowExists2 = view.findViewById(R.id.trow_from_stream2);
		tl.removeView(trowExists1);
		tl.removeView(trowExists2);
	}

	private void switchHTML(boolean isHTML) {
		bThisIsHTML = isHTML;
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		if (btnAsHTML!=null) {
			btnAsHTML.setBackgroundColor(colorGrayCT);
			mActivity.tintViewIcons(btnAsHTML, PorterDuff.Mode.CLEAR,true);
		}
		if (btnAsText!=null) {
			btnAsText.setBackgroundColor(colorGrayCT);
			mActivity.tintViewIcons(btnAsText, PorterDuff.Mode.CLEAR,true);
		}
		if ((btnAsHTML!=null) && (bThisIsHTML)) {
			btnAsHTML.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(btnAsHTML, true);
			if ((StrUtils.isEmptyStr(fileExt)) || (fileExt.equals(".txt")))
				fileExt = ".html";
		}
		if ((btnAsText!=null)&&(!bThisIsHTML)) {
			btnAsText.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(btnAsText, true);
			fileExt = ".txt";
		}
	}

	private ArrayList<String> getFontNames(InputStream is) {
		ArrayList<String> fontNames = new ArrayList<String>();
		try {
			ZipInputStream zis = new ZipInputStream(is);
			ZipEntry entry;
			ArrayList<String> fontDirs = Engine.getFontsDirsReal();
			while ((entry = zis.getNextEntry()) != null) {
				if ((entry.getName().toUpperCase().endsWith(".TTF"))||
						(entry.getName().toUpperCase().endsWith(".OTF"))) {
					boolean bFound = false;
					for (String s: fontDirs) {
						File f = new File (s+"/"+entry.getName());
						if (f.exists()) {
							bFound = true;
							break;
						}
					}
					if (!bFound) fontNames.add(entry.getName());
				}
				// consume all the data from this entry
				//while (zis.available() > 0)
				//	zis.read();
				// I could close the entry, but getNextEntry does it automatically
				// zis.closeEntry()
			}
		} catch (Exception e) {
			log.e("Handling of zip: " + e.getMessage());
		}
		return fontNames;
	};

	private int copyFonts(ArrayList<String> fontNames, InputStream is) {
		int filesCopied = 0;
		try {
			ZipInputStream zis = new ZipInputStream(is);
			ZipEntry entry;
			ArrayList<String> fontDirs = Engine.getFontsDirsReal();
			boolean hasSystemFolder = false;
			boolean hasNonSystemFolder = false;
			for (String s: fontDirs) {
				if (s.toLowerCase().startsWith("/system")) hasSystemFolder = true;
					else hasNonSystemFolder = true;
			}
			// try to create most common folder
			if ((hasSystemFolder) && (!hasNonSystemFolder)) {
				File f = new File("/storage/emulated/0/fonts");
				f.mkdir();
				fontDirs = Engine.getFontsDirsReal();
				hasSystemFolder = false;
				hasNonSystemFolder = false;
				for (String s: fontDirs) {
					if (s.toLowerCase().startsWith("/system")) hasSystemFolder = true;
					else hasNonSystemFolder = true;
				}
			}
			while ((entry = zis.getNextEntry()) != null) {
				if (fontNames.contains(entry.getName())) {
					boolean copied = false;
					for (String s: fontDirs) {
						if ((hasNonSystemFolder) && (s.toLowerCase().startsWith("/system"))) continue;
						if (!copied) {
							try {
								log.i("try to copy file: "+s + "/" + entry.getName());
								Utils.saveStreamToFileDontClose(zis, s + "/" + entry.getName());
								copied = true;
							} catch (Exception e) {
								log.e("cannot copy file: "+e.getMessage());
							}
						}
					}
					if (copied) filesCopied++;
					zis.closeEntry();
				}
			}
		} catch (Exception e) {
			log.e("Handling of zip: " + e.getMessage());
		}
		return filesCopied;
	};

	int secondCountdown;
	boolean stopCount = false;

	public void openExistingClick() {
		Runnable task = () -> {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					mActivity.loadDocumentExt(sExistingName, sUriUpd), 500));
			onPositiveButtonClick();
		};
		mActivity.runInReader(task);
	}


	private void countdownTick() {
		if (stopCount) {
			btnOpenExisting.setText(mActivity.getString(R.string.open_existing));
			return;
		}
		secondCountdown--;
		if (secondCountdown>0) {
			btnOpenExisting.setText(mActivity.getString(R.string.open_existing) + " (" + secondCountdown + ")");
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
							countdownTick(),
					1000));
		} else {
			openExistingClick();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mInflater = LayoutInflater.from(getContext());
        ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.external_doc_came_dialog, null);
        
		tvExtPath = view.findViewById(R.id.ext_path);
		sExistingName = "";
		String sBaseName = "";
		String sQueryPath = "";
		sUriUpd = sUri;
		if (!StrUtils.isEmptyStr(mFileToOpen)) {
			sExistingName = mFileToOpen;
			fileDir = new File(mFileToOpen).getParent();
			sUriUpd = mFileToOpen;
		} else
			if (uri != null) {
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"DOC CAME, Uri is: "+StrUtils.getNonEmptyStr(uri.toString(),false));
				sUri = java.net.URLDecoder.decode(StrUtils.getNonEmptyStr(uri.toString(),false));
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"Uri encoded is: "+sUri);
				sUriUpd = sUri;
				if (sUriUpd.contains("file://")) { // X-plore File Manager support
					sUriUpd = sUriUpd.substring(sUriUpd.indexOf("file://") + "file://".length());
					CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
							"Uri contains 'file://', updated uri: "+sUriUpd);
					sUriUpd = java.net.URLDecoder.decode(sUriUpd);
					CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
							"Uri contains 'file://', updated uri decoded: "+sUriUpd);
				}
				sBaseName = StrUtils.getNonEmptyStr(queryName(mActivity.getApplicationContext().getContentResolver(), uri), true);
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"BaseName is: "+sBaseName);
				sQueryPath = StrUtils.getNonEmptyStr(queryPath(mActivity.getApplicationContext().getContentResolver(), uri), true);
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"QueryPath1 is: "+sQueryPath);
				//content://com.amaze.filemanager.debug/storage_root/storage/1CE4-1508/Book1/Zagadka_ryzhej_lisy..fb2.zip
				if (StrUtils.isEmptyStr(sQueryPath)) {
					ArrayList<StorageDirectory> arrStorages = mActivity.getStorageDirectories();
					for (StorageDirectory storageDirectory: arrStorages) {
						if (sUri.contains(storageDirectory.mPath)) {
							sQueryPath = sUri.substring(sUri.indexOf(storageDirectory.mPath));
							break;
						}
					}
				}
				File f = FileUtils.getFile(sQueryPath);
				if (f != null) {
					CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
							"file found by QueryPath1");
					sUriUpd = sQueryPath;
					sExistingName = f.getAbsolutePath();
					fileDir = f.getParent();
				}
				if (StrUtils.isEmptyStr(sExistingName)) {
					sQueryPath = StrUtils.getNonEmptyStr(queryPathM(mActivity.getApplicationContext().getContentResolver(), uri), true);
					CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
							"QueryPath2 is: "+sQueryPath);
					f = FileUtils.getFile(sQueryPath);
					if (f != null) {
						CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
								"file found by QueryPath2");
						sUriUpd = sQueryPath;
						sExistingName = f.getAbsolutePath();
						fileDir = f.getParent();
					}
				}
				if (StrUtils.isEmptyStr(sExistingName))
					if (sUriUpd.contains(sBaseName)) {
						sUriUpd = sUriUpd.substring(0, sUriUpd.indexOf(sBaseName) + sBaseName.length()); // X-plore File Manager support
						CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
								"Uri updated due to found value of baseName, new uri is = " + sUriUpd);
					}
				if (sBaseName.endsWith(".")) sBaseName = sBaseName.substring(0,sBaseName.length()-1);
			}
		tvExtPath.setText(sUriUpd);
		tvDocType = view.findViewById(R.id.doc_type);
		tvDocType.setText(stype);
		String sDocFormat = DocumentFormat.extByMimeType(stype);
		if (StrUtils.isEmptyStr(sDocFormat)) sDocFormat = Utils.getFileExtension(sBaseName);
		if (StrUtils.getNonEmptyStr(sUriUpd, true).startsWith("http")) {
			if (StrUtils.isEmptyStr(sDocFormat)) sDocFormat = DocumentFormat.nameEndWithExt(sUriUpd);
			if (StrUtils.isEmptyStr(sDocFormat)) {
				if (bThisIsHTML)
					sDocFormat = "html";
				else
					sDocFormat = "txt";
			}
			if (StrUtils.isEmptyStr(sBaseName)) sBaseName = extractSuggestedName(sUriUpd);
		}
		sDocFormat = StrUtils.getNonEmptyStr(sDocFormat,true);
		if (sBaseName.toLowerCase().endsWith("."+sDocFormat)) {
			sBaseName = sBaseName.substring(0, sBaseName.length() - sDocFormat.length() - 1);
		}
		if ((uri == null) && (StrUtils.isEmptyStr(sDocFormat))) sDocFormat = "html";
		if (sBaseName.endsWith("."+sDocFormat)) sBaseName = sBaseName.substring(0,sBaseName.length()-1-sDocFormat.length());
		if (StrUtils.isEmptyStr(sBaseName)) sBaseName = "KnownReader_Downloaded";
		// try to detect file from file provider
		if (StrUtils.isEmptyStr(sExistingName) && (sUriUpd.contains("fileprovider/root"))) {
			String path = sUriUpd.substring(sUriUpd.indexOf("fileprovider/root") + "fileprovider/root".length());
			CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
					"Try to find file (fileprovider/root) = " + path);
			File f = FileUtils.getFile(path);
			if (f != null) {
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"Found file (fileprovider/root) = " + path);
				sUriUpd = path;
				sExistingName = f.getAbsolutePath();
				fileDir = f.getParent();
			}
		}
		if (StrUtils.isEmptyStr(sExistingName) && (sUriUpd.contains("fileprovider"))) {
			String path = sUriUpd.substring(sUriUpd.indexOf("fileprovider") + "fileprovider".length());
			CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
					"Try to find file (fileprovider) = " + path);
			File f = FileUtils.getFile(path);
			if (f != null) {
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"Found file (fileprovider) = " + path);
				sUriUpd = path;
				sExistingName = f.getAbsolutePath();
				fileDir = f.getParent();
			}
		}
		if (StrUtils.isEmptyStr(sExistingName) && (sUriUpd.contains("storage/emulated"))) {
			String path = "/" + sUriUpd.substring(sUriUpd.indexOf("storage/emulated") );
			CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
					"Try to find file (storage/emulated) = " + path);
			File f = FileUtils.getFile(path);
			if (f != null) {
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"Found file (storage/emulated) = " + path);
				sUriUpd = path;
				sExistingName = f.getAbsolutePath();
				fileDir = f.getParent();
			}
		}
		if (StrUtils.isEmptyStr(sExistingName) && (!StrUtils.isEmptyStr(sUriUpd))) {
			CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
					"Try to find file (sUriUpd) = " + sUriUpd);
			try {
				File f = FileUtils.getFile(sUriUpd);
				if (f != null) {
					CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
							"Found file (sUriUpd) = " + sUriUpd);
					sExistingName = f.getAbsolutePath();
					fileDir = f.getParent();
				}
			} catch (Exception e) {
				//do nothing
			}
		}
		//ArrayList<FileInfo> folders1 = Services.getFileSystemFolders().getFileSystemFolders();
		try {
			if ((StrUtils.isEmptyStr(sExistingName) && (sUriUpd.contains("fileprovider/external")))) {
				String path = sUriUpd.substring(sUriUpd.indexOf("fileprovider/external") + "fileprovider/external".length());
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"Try to find file (fileprovider/external) = " + path);
				ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
				if (folders != null) {
					CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
							"Got getFileSystemFolders, count = " + folders.size());
					for (FileInfo fi : folders) {
						if (fi.getType() == FileInfo.TYPE_FS_ROOT) {
							String path2 = fi.getPathName() + "/" + path;
							CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
									"Try to find file (fileprovider/external, path2) = " + path2);
							File f = FileUtils.getFile(path2);
							if (f != null) {
								CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
										"Found file (fileprovider/external, path2) = " + path2);
								sExistingName = f.getAbsolutePath();
								fileDir = f.getParent();
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {

		}
		if ((StrUtils.isEmptyStr(sExistingName) && (sUriUpd.contains("fileprovider/external")))) {
			String path = sUriUpd.substring(sUriUpd.indexOf("fileprovider/external") + "fileprovider/external".length());
			CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
					"Try to find file (fileprovider/external) se0 = " + path);
			String path2 = "/storage/emulated/0/" + path;
			CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
					"Try to find file (fileprovider/external, path2, se0) = " + path2);
			File f = FileUtils.getFile(path2);
			if (f != null) {
				CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
						"Found file (fileprovider/external, path2, se0) = " + path2);
				sExistingName = f.getAbsolutePath();
				fileDir = f.getParent();
			}
		}
		if (StrUtils.isEmptyStr(sExistingName) && (!sBaseName.equals("KnownReader_Downloaded"))) {
			if (StrUtils.isEmptyStr(sExistingName)) {
				if (!StrUtils.isEmptyStr(sDocFormat)) {
					CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
							"Try to find file = " + fileDir + "/" + sBaseName + "." + sDocFormat);
					File f = FileUtils.getFile(fileDir + "/" + sBaseName + "." + sDocFormat);
					if (f != null) {
						sExistingName = f.getAbsolutePath();
						CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
								"File found by (3) = " + fileDir + "/" + sBaseName + "." + sDocFormat);
					}
				} else {
					CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
							"Try to find file = " + fileDir + "/" + sBaseName);
					File f = FileUtils.getFile(fileDir + "/" + sBaseName);
					if (f != null) {
						sExistingName = f.getAbsolutePath();
						CustomLog.doLog(mLogFileRoot, "log_ext_doc_came.log",
								"File found by (2) = " + fileDir + "/" + sBaseName);
					}
				}
			}
		}
		//\
		int i = 0;
		String sBName = sBaseName;
		fileName = replaceInvalidChars(sBName);
		fileExt = "."+replaceInvalidChars(sDocFormat);
		btnOpenFromStream = view.findViewById(R.id.btn_open_from_stream);
		Utils.setDashedButton(btnOpenFromStream);
		//btnOpenFromStream.setBackgroundColor(colorGrayC);
		String finalSUriUpd = sUriUpd;
		btnOpenFromStream.setOnClickListener(v -> {
			stopCount = true;
			if (uri != null) {
				if (stype.equals("application/zip")) {
					ContentResolver contentResolver = mActivity.getContentResolver();
					InputStream inputStream;
					ArrayList<String> arcFontNames;
					try {
						inputStream = contentResolver.openInputStream(uri);
						ByteArrayOutputStream baos = Utils.inputStreamToBaos(inputStream);
						InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
						arcFontNames = getFontNames(is1);
						if (arcFontNames.size()==0) {
							final InputStream is2 = new ByteArrayInputStream(baos.toByteArray());
							Runnable task = () -> {
								BackgroundThread.instance().postBackground(() ->
										BackgroundThread.instance().postGUI(() -> mActivity.loadDocumentFromStreamExt(is2, finalSUriUpd), 500));
								onPositiveButtonClick();
							};
							mActivity.runInReader(task);
						} else {
							final ArrayList<String> arcFontNamesF = arcFontNames;
							mActivity.askConfirmation(mActivity.getString(R.string.new_fonts,
									String.valueOf(arcFontNames.size())), () -> {
										InputStream is11 = new ByteArrayInputStream(baos.toByteArray());
										int filesCopied = copyFonts(arcFontNamesF, is11);
										mActivity.showToast(mActivity.getString(R.string.fonts_copied,
												String.valueOf(filesCopied)));
										//if (filesCopied>0) {
										//}
										onPositiveButtonClick();
									}, () -> {
										final InputStream is2 = new ByteArrayInputStream(baos.toByteArray());
										Runnable task = () -> {
											BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
													mActivity.loadDocumentFromStreamExt(is2, finalSUriUpd), 500));
											onPositiveButtonClick();
										};
										mActivity.runInReader(task);
									});
						}
					} catch (Exception e) {
					}
				} else {
					Runnable task = () -> {
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
								mActivity.loadDocumentFromUriExt(uri, finalSUriUpd), 500));
						onPositiveButtonClick();
					};
					mActivity.runInReader(task);
				}
			}
			else {
				if ((istream != null) && (bThisIsHTML)) {
					Runnable task = () -> {
						BackgroundThread.instance().postBackground(() ->
								BackgroundThread.instance().postGUI(() -> mActivity.loadDocumentFromStreamExt(istream, finalSUriUpd), 500));
					};
					mActivity.runInReader(task);
				}
				if ((istreamTxt != null) && (!bThisIsHTML)) {
					Runnable task = () -> {
						BackgroundThread.instance().postBackground(() ->
								BackgroundThread.instance().postGUI(() -> mActivity.loadDocumentFromStreamExt(istreamTxt, finalSUriUpd), 500));
					};
					mActivity.runInReader(task);
				}
				onPositiveButtonClick();
			}
		});
		// ODF file must be saved for later convert
		if ((stype.contains("opendocument")) && (!stype.contains("opendocument.text"))) hideExistingFromStreamControls(view);
		tvExistingPath = view.findViewById(R.id.existing_path);
		tvExistingPath.setText(sExistingName);
		btnOpenExisting = view.findViewById(R.id.btn_open_existing);
		//btnMoveToBooksThenOpen = view.findViewById(R.id.btn_move_to_books_then_open); // asdf
		Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
		btnOpenExisting.setTypeface(boldTypeface);
		Utils.setDashedButton(btnOpenExisting);
		btnOpenExisting.setOnClickListener(v -> {
			stopCount = true;
			openExistingClick();
		});
		boolean immediateClose = false;

		trSaveToLib = view.findViewById(R.id.tr_save_to_lib);
		trMoveToLib = view.findViewById(R.id.tr_move_to_lib);
		btnSave = view.findViewById(R.id.btn_save);
		btnSave.setOnClickListener(v -> {
			super.onPositiveButtonClick();
			stopCount = true;
			SaveDocDialog dlg = new SaveDocDialog(mActivity, false,
					fileDir, sExistingName, fileName, fileExt, sUriUpd, uri, stype);
			dlg.setHTMLData(bThisIsHTML, istream, istreamTxt);
			onPositiveButtonClick();
			dlg.show();
		});
		Utils.setDashedButton(btnSave);
		btnMove = view.findViewById(R.id.btn_move);
		btnMove.setOnClickListener(v -> {
			super.onPositiveButtonClick();
			stopCount = true;
			SaveDocDialog dlg = new SaveDocDialog(mActivity, true,
					fileDir, sExistingName, fileName, fileExt, sUriUpd, uri, stype);
			onPositiveButtonClick();
			dlg.show();
		});
		Utils.setDashedButton(btnMove);

		if (StrUtils.isEmptyStr(sExistingName))
			hideExistingFileControls(view);
		else {
			secondCountdown = mActivity.settings().getInt(Settings.PROP_APP_EXT_DOC_CAME_TIMEOUT, 0);
			int sec = 1000;
			if (secondCountdown == 0) {
				sec = 300;
				immediateClose = true;
			}
			int finalSec = sec;
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					countdownTick(),
					finalSec));
		}

		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();

		btnAsHTML = view.findViewById(R.id.btn_as_html);
		btnAsHTML.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);

		Utils.setDashedButton(btnAsHTML);
		btnAsHTML.setOnClickListener(v -> { stopCount = true; switchHTML(true); });
		btnAsText = view.findViewById(R.id.btn_as_text);
		btnAsText.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);

		Utils.setDashedButton(btnAsText);
		btnAsText.setOnClickListener(v -> { stopCount = true; switchHTML(false); });
		if (uri != null) hideExistingHttpControls(view);
			else {
				switchHTML(true);
				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> switchHTML(true), 200));
		}
		if (!StrUtils.isEmptyStr(mFileToOpen)) hideExistingFromStreamControls(view);
		if (!immediateClose) setView(view);
		// if link is http
		if (uri==null) doDownloadHttp(sUriUpd);
	}

	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
		stopCount = true;
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
		stopCount = true;
	}

	protected void onOkButtonClick() {
		super.onPositiveButtonClick();
		stopCount = true;
	}

}

