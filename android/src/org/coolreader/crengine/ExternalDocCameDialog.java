package org.coolreader.crengine;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

	private LayoutInflater mInflater;
	private String stype;
	private Uri uri;
	private InputStream istream = null;
	private InputStream istreamTxt = null;
	private String sUri = "";
	private String downlDir = "";
	private String sExistingName = "";
	private Document docJsoup = null;
	private Boolean bThisIsHTML = true;
	private String sTitle = "";

	public static final Logger log = L.create("edcd");

	public String extractSuggestedName(String sText) {
		String sLastSeg = StrUtils.getNonEmptyStr(sText,true);
		int len1 = sLastSeg.split("/").length;
		if (len1 > 1) sLastSeg = sLastSeg.split("/")[len1 - 1];
		sLastSeg = sLastSeg.replace("&", "_").replace("#", "_")
				.replace("?", "_").replace("%", "_")
				.replace(":","/")
				.replace("/","_").replace("\\","")
				.replace("\\\\","");
		return sLastSeg.trim();
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
					((CoolReader)activity).processIntent(intent), 200));
			return true;
		}
		return false;
	}

	public void doDownloadHttp(String sLink) {
		if (!StrUtils.isEmptyStr(sLink)) {
			HttpUrl hurl = HttpUrl.parse(sLink);
			if (hurl == null) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					activity.showToast("Download error - cannot parse link");
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
							onPositiveButtonClick();
							PictureCameDialog dlg = new PictureCameDialog(activity, istream, stype, extractSuggestedName(sLink));
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
									edtFileName.setText(extractSuggestedName(sTitle));
								}, 100));
							} catch (Exception e) {
								docJsoup = null;
								BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
									btnAsText.setEnabled(false);
									log.e(activity.getString(R.string.error_open_as_text)+": "+
											e.getClass().getSimpleName()+" "+e.getMessage());
									activity.showToast(activity.getString(R.string.error_open_as_text)+": "+
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
						activity.showToast("Download error: "+ef.getMessage()+
								" ["+ef.getClass().getSimpleName()+"]");
						onPositiveButtonClick();
					}, 200));
				}
			});
		}
	}

	public ExternalDocCameDialog(CoolReader activity, String stype, Object obj)
	{
		super("ExternalDocCameDialog", activity, activity.getString(R.string.external_doc_came), false, true);
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		this.mActivity = activity;
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
	TextView tvDownloadPath;
	TextView tvDocType;
	Button btnOpenFromStream;
	Button btnSave;
	EditText edtFileExt;
	EditText edtFileName;
	TextView tvExistingPath;
	Button btnOpenExisting;
	Button btnAsHTML;
	Button btnAsText;

	private String queryName(ContentResolver resolver, Uri uri) {
		Cursor returnCursor =
				resolver.query(uri, null, null, null, null);
		assert returnCursor != null;
		int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
		returnCursor.moveToFirst();
		String name = returnCursor.getString(nameIndex);
		returnCursor.close();
		return name;
	}

	private void hideExistingFileControls(ViewGroup view) {
		TableLayout tl = view.findViewById(R.id.table);
		TableRow trowExists1 = view.findViewById(R.id.trow_file_exists1);
		TableRow trowExists2 = view.findViewById(R.id.trow_file_exists2);
		TableRow trowExists3 = view.findViewById(R.id.trow_file_exists3);
		tl.removeView(trowExists1);
		tl.removeView(trowExists2);
		tl.removeView(trowExists3);
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
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGray = a.getColor(0, Color.GRAY);
		int colorGrayC = a.getColor(1, Color.GRAY);
		int colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
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
		if ((btnAsHTML!=null)&&(bThisIsHTML)) {
			btnAsHTML.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(btnAsHTML, true);
			edtFileExt.setText(".html");
		}
		if ((btnAsText!=null)&&(!bThisIsHTML)) {
			btnAsText.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(btnAsText, true);
			edtFileExt.setText(".txt");
		}
	}

	private void setDashedButton(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_dashed_border);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
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

	private void openExistingClick() {
		BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
				((CoolReader) activity).loadDocumentExt(sExistingName, sUri), 500));
		onPositiveButtonClick();
	}
	private void countdownTick() {
		secondCountdown--;
		if (secondCountdown>0) {
			btnOpenExisting.setText(activity.getString(R.string.open_existing) + " (" + secondCountdown + ")");
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
        
        TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorIcon,
				 R.attr.colorThemeGray2Contrast
				});
		int colorIcon = a.getColor(0, Color.GRAY);
		int colorGrayC = a.getColor(1, Color.GRAY);
		a.recycle();
		tvExtPath = view.findViewById(R.id.ext_path);
		if (uri != null) sUri = StrUtils.getNonEmptyStr(uri.toString(),false);
		tvExtPath.setText(sUri);
		tvDocType = view.findViewById(R.id.doc_type);
		tvDocType.setText(stype);
		tvDownloadPath = view.findViewById(R.id.download_path);
		downlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
		tvDownloadPath.setText(downlDir);
		edtFileName = view.findViewById(R.id.file_name);
		String sBaseName = "";
		if (uri != null) {
			sBaseName = StrUtils.getNonEmptyStr(queryName(activity.getApplicationContext().getContentResolver(), uri), true);
			if (sBaseName.endsWith(".")) sBaseName = sBaseName.substring(0,sBaseName.length()-1);
		}
		String sDocFormat = DocumentFormat.extByMimeType(stype);
		if (StrUtils.isEmptyStr(sDocFormat)) sDocFormat = Utils.getFileExtension(sBaseName);
		if (uri == null) sDocFormat = "html";
		final String sFDocFormat = sDocFormat;
		if (sBaseName.endsWith("."+sDocFormat)) sBaseName = sBaseName.substring(0,sBaseName.length()-1-sDocFormat.length());
		sExistingName = "";
		if (StrUtils.isEmptyStr(sBaseName)) sBaseName = "CoolReader_Downloaded";
		else {
			if (!StrUtils.isEmptyStr(sDocFormat)) {
				File f = new File(downlDir + "/" + sBaseName + "." + sDocFormat);
				if (f.exists()) sExistingName = downlDir + "/" + sBaseName + "." + sDocFormat;
			} else {
				File f = new File(downlDir + "/" + sBaseName);
				if (f.exists()) sExistingName = downlDir + "/" + sBaseName;
			}
		}
		int i = 0;
		Boolean exs = true;
		String sBName = sBaseName;
		while (exs) {
			sBName = sBaseName;
			if (i>0) sBName = sBName + " (" + i + ")";
			File fEx = new File(downlDir+"/"+sBName+"."+sDocFormat);
			exs = fEx.exists();
			i++;
		}
		edtFileName.setText(sBName);
		int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		edtFileName.setBackgroundColor(colorGrayCT);

		edtFileExt = (EditText) view.findViewById(R.id.file_ext);
		edtFileExt.setText("."+sDocFormat);
		edtFileExt.setBackgroundColor(colorGrayCT);

		btnOpenFromStream = view.findViewById(R.id.btn_open_from_stream);
		setDashedButton(btnOpenFromStream);
		//btnOpenFromStream.setBackgroundColor(colorGrayC);
		btnOpenFromStream.setOnClickListener(v -> {
			if (uri != null) {
				if (stype.equals("application/zip")) {
					ContentResolver contentResolver = mActivity.getContentResolver();
					InputStream inputStream = null;
					ArrayList<String> arcFontNames = new ArrayList<String>();
					try {
						inputStream = contentResolver.openInputStream(uri);
						ByteArrayOutputStream baos = Utils.inputStreamToBaos(inputStream);
						InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
						arcFontNames = getFontNames(is1);
						if (arcFontNames.size()==0) {
							final InputStream is2 = new ByteArrayInputStream(baos.toByteArray());
							BackgroundThread.instance().postBackground(() ->
									BackgroundThread.instance().postGUI(() -> ((CoolReader) activity).loadDocumentFromStreamExt(is2, sUri), 500));
							onPositiveButtonClick();
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
										BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
												((CoolReader) activity).loadDocumentFromStreamExt(is2, sUri), 500));
										onPositiveButtonClick();
									});
						}
					} catch (Exception e) {
					}
				} else {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
							((CoolReader) activity).loadDocumentFromUriExt(uri, sUri), 500));
					onPositiveButtonClick();
				}
			}
			else {
				if ((istream != null) && (bThisIsHTML)) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> ((CoolReader) activity).loadDocumentFromStreamExt(istream, sUri), 500));
				}
				if ((istreamTxt != null) && (!bThisIsHTML)) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> ((CoolReader) activity).loadDocumentFromStreamExt(istreamTxt, sUri), 500));
				}
				onPositiveButtonClick();
			}
		});
		// ODF file must be saved for later convert
		if ((stype.contains("opendocument")) && (!stype.contains("opendocument.text"))) hideExistingFromStreamControls(view);
		btnSave = (Button)view.findViewById(R.id.btn_save);
		setDashedButton(btnSave);
		//btnOpenFromStream.setBackgroundColor(colorGrayC);
		btnSave.setOnClickListener(v -> {
			String fName = downlDir+"/"+edtFileName.getText()+edtFileExt.getText();
			File fEx = new File(fName);
			if (fEx.exists()) {
				activity.showToast(activity.getString(R.string.pic_file_exists));
			} else {
				if (
					(uri != null) ||
					((istream != null) && (bThisIsHTML)) ||
					((istreamTxt != null) && (!bThisIsHTML))
				) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(fName);
							BufferedOutputStream out = new BufferedOutputStream(fos);
							InputStream in = null;
							if (uri != null) in = activity.getApplicationContext().getContentResolver().openInputStream(uri);
							if ((istream != null) && (bThisIsHTML)) in = istream;
							if ((istreamTxt != null) && (!bThisIsHTML)) in = istreamTxt;
							Utils.copyStreamContent(out, in);
							out.flush();
							fos.getFD().sync();
							if ((stype.contains("opendocument")) && (!stype.contains("opendocument.text"))) {
								DocConvertDialog dlgConv = new DocConvertDialog((CoolReader)activity, fName);
								dlgConv.show();
							} else {
								BackgroundThread.instance().postBackground(() ->
										BackgroundThread.instance().postGUI(() -> ((CoolReader) activity).loadDocumentExt(fName, sUri), 500));
							}
							onPositiveButtonClick();
						} catch (Exception e) {
							log.e("Error creating file: " + e.getClass().getSimpleName()+": "+e.getLocalizedMessage());
							activity.showToast("Error creating file: " + e.getClass().getSimpleName()+": "+e.getLocalizedMessage());
						}
					}, 200));
				}
			}
		});
		tvExistingPath = view.findViewById(R.id.existing_path);
		tvExistingPath.setText(sExistingName);
		btnOpenExisting = view.findViewById(R.id.btn_open_existing);
		Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
		btnOpenExisting.setTypeface(boldTypeface);
		setDashedButton(btnOpenExisting);
		btnOpenExisting.setOnClickListener(v -> {
			openExistingClick();
		});
		if (StrUtils.isEmptyStr(sExistingName))
			hideExistingFileControls(view);
		else {
			secondCountdown = 10;
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					countdownTick(),
					1000));
		}

		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();

		btnAsHTML = view.findViewById(R.id.btn_as_html);
		btnAsHTML.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);

		setDashedButton(btnAsHTML);
		btnAsHTML.setOnClickListener(v -> switchHTML(true));
		btnAsText = view.findViewById(R.id.btn_as_text);
		btnAsText.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);

		setDashedButton(btnAsText);
		btnAsText.setOnClickListener(v -> switchHTML(false));
		if (uri != null) hideExistingHttpControls(view);
			else {
				switchHTML(true);
				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> switchHTML(true), 200));
		}
		setView(view);
		// if link is http
		if (uri==null) doDownloadHttp(sUri);
	}

	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	protected void onOkButtonClick() {
		super.onPositiveButtonClick();
	}

}

