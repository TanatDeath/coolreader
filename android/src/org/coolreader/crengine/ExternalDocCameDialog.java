package org.coolreader.crengine;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
		String sLastSeg = sText;
		int len1 = sLastSeg.split("/").length;
		if (len1 > 1) sLastSeg = sLastSeg.split("/")[len1 - 1];
		sLastSeg = sLastSeg.replace("&", "_").replace("#", "_")
				.replace("?", "_").replace("%", "_")
				.replace(":","/")
				.replace("/","_").replace("\\","")
				.replace("\\\\","");
		return sLastSeg;
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
			BackgroundThread.instance().postBackground(new Runnable() {
				@Override
				public void run() {
					BackgroundThread.instance().postGUI(new Runnable() {
						@Override
						public void run() {
							((CoolReader)activity).processIntent(intent);
						}
					}, 200);
				}
			});
			return true;
		}
		return false;
	}

	public void doDownloadHttp(String sLink) {
		if (!StrUtils.isEmptyStr(sLink)) {
			HttpUrl hurl = HttpUrl.parse(sLink);
			if (hurl == null) {
				BackgroundThread.instance().postBackground(new Runnable() {
					@Override
					public void run() {
						BackgroundThread.instance().postGUI(new Runnable() {
							@Override
							public void run() {
								activity.showToast("Download error - cannot parse link");
								onPositiveButtonClick();
							}
						}, 100);
					}
				});
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
						BackgroundThread.instance().postBackground(new Runnable() {
							@Override
							public void run() {
								BackgroundThread.instance().postGUI(new Runnable() {
									@Override
									public void run() {
										onPositiveButtonClick();
									}
								}, 100);
							}
						});
						return;
					}
					stype = StrUtils.getNonEmptyStr(response.body().contentType().toString(),true);
					BackgroundThread.instance().postBackground(new Runnable() {
						@Override
						public void run() {
							BackgroundThread.instance().postGUI(new Runnable() {
								@Override
								public void run() {
									tvDocType.setText(stype);
								}
							}, 100);
						}
					});
					InputStream is = response.body().byteStream();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					int len;
					while ((len = is.read(buffer)) > -1 ) {
						baos.write(buffer, 0, len);
					}
					baos.flush();
					istream = new ByteArrayInputStream(baos.toByteArray());
					if (stype.startsWith("image/")) {
						BackgroundThread.instance().postBackground(new Runnable() {
							@Override
							public void run() {
								BackgroundThread.instance().postGUI(new Runnable() {
									@Override
									public void run() {
										onPositiveButtonClick();
										PictureCameDialog dlg = new PictureCameDialog(activity, istream, stype, extractSuggestedName(sLink));
										dlg.show();
									}
								}, 100);
							}
						});
					} else {
						BackgroundThread.instance().postBackground(new Runnable() {
							@Override
							public void run() {
								try {
									docJsoup = Jsoup.parse(urlBuilder.build().url(), 180000); // three minutes
									istreamTxt = new ByteArrayInputStream(docJsoup.body().text().replace((char)0,' ').getBytes());
									Elements resultLinks = docJsoup.select("html > head > title");
									if (resultLinks.size()>0) sTitle = resultLinks.text();
									BackgroundThread.instance().postBackground(new Runnable() {
										@Override
										public void run() {
											BackgroundThread.instance().postGUI(new Runnable() {
												@Override
												public void run() {
													tvExtPath.setText(tvExtPath.getText()+"; "+sTitle);
													edtFileName.setText(extractSuggestedName(sTitle));
												}
											}, 100);
										}
									});
								} catch (Exception e) {
									docJsoup = null;
									BackgroundThread.instance().postBackground(new Runnable() {
										@Override
										public void run() {
											BackgroundThread.instance().postGUI(new Runnable() {
												@Override
												public void run() {
													btnAsText.setEnabled(false);
													log.e(activity.getString(R.string.error_open_as_text)+": "+
															e.getClass().getSimpleName()+" "+e.getMessage());
													activity.showToast(activity.getString(R.string.error_open_as_text)+": "+
															e.getClass().getSimpleName()+" "+e.getMessage());
												}
											}, 100);
										}
									});
								}
							}
						});
					}
				}

				public void onFailure(Call call, IOException e) {
					final IOException ef = e;
					BackgroundThread.instance().postBackground(new Runnable() {
						@Override
						public void run() {
							BackgroundThread.instance().postGUI(new Runnable() {
								@Override
								public void run() {
									log.e("Download error: "+ef.getMessage()+
											" ["+ef.getClass().getSimpleName()+"]");
									activity.showToast("Download error: "+ef.getMessage()+
											" ["+ef.getClass().getSimpleName()+"]");
									onPositiveButtonClick();
								}
							}, 200);
						}
					});
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
		TableLayout tl = (TableLayout) view.findViewById(R.id.table);
		TableRow trowExists1 = (TableRow)view.findViewById(R.id.trow_file_exists1);
		TableRow trowExists2 = (TableRow)view.findViewById(R.id.trow_file_exists2);
		TableRow trowExists3 = (TableRow)view.findViewById(R.id.trow_file_exists3);
		tl.removeView(trowExists1);
		tl.removeView(trowExists2);
		tl.removeView(trowExists3);
	}

	private void hideExistingHttpControls(ViewGroup view) {
		TableLayout tl = (TableLayout) view.findViewById(R.id.table);
		TableRow trowExists1 = (TableRow)view.findViewById(R.id.trow_text_or_html);
		TableRow trowExists2 = (TableRow)view.findViewById(R.id.trow_text_or_html2);
		tl.removeView(trowExists1);
		tl.removeView(trowExists2);
	}

	private void hideExistingFromStreamControls(ViewGroup view) {
		TableLayout tl = (TableLayout) view.findViewById(R.id.table);
		TableRow trowExists1 = (TableRow)view.findViewById(R.id.trow_from_stream1);
		TableRow trowExists2 = (TableRow)view.findViewById(R.id.trow_from_stream2);
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
		if (btnAsHTML!=null) btnAsHTML.setBackgroundColor(colorGrayCT);
		if (btnAsText!=null) btnAsText.setBackgroundColor(colorGrayCT);
		if ((btnAsHTML!=null)&&(bThisIsHTML)) {
			btnAsHTML.setBackgroundColor(colorGrayCT2);
			edtFileExt.setText(".html");
		}
		if ((btnAsText!=null)&&(!bThisIsHTML)) {
			btnAsText.setBackgroundColor(colorGrayCT2);
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
		tvExtPath = (TextView)view.findViewById(R.id.ext_path);
		if (uri != null) sUri = StrUtils.getNonEmptyStr(uri.toString(),false);
		tvExtPath.setText(sUri);
		tvDocType = (TextView)view.findViewById(R.id.doc_type);
		tvDocType.setText(stype);
		tvDownloadPath = (TextView)view.findViewById(R.id.download_path);
		downlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
		tvDownloadPath.setText(downlDir);
		edtFileName = (EditText)view.findViewById(R.id.file_name);
		String sBaseName = "";
		if (uri != null)
			sBaseName = StrUtils.getNonEmptyStr(queryName(activity.getApplicationContext().getContentResolver(),uri),true);
		String sDocFormat = DocumentFormat.extByMimeType(stype);
		if (uri == null) sDocFormat = "html";
		final String sFDocFormat = sDocFormat;
		if (sBaseName.endsWith("."+sDocFormat)) sBaseName = sBaseName.substring(0,sBaseName.length()-1-sDocFormat.length());
		sExistingName = "";
		if (StrUtils.isEmptyStr(sBaseName)) sBaseName = "CoolReader_Downloaded";
		else {
			File f = new File(downlDir+"/"+sBaseName+"."+sDocFormat);
			if (f.exists()) sExistingName = downlDir+"/"+sBaseName+"."+sDocFormat;
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

		btnOpenFromStream = (Button)view.findViewById(R.id.btn_open_from_stream);
		setDashedButton(btnOpenFromStream);
		//btnOpenFromStream.setBackgroundColor(colorGrayC);
		btnOpenFromStream.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (uri != null) {
					((CoolReader) activity).loadDocumentFromUriExt(uri, sUri);
					onPositiveButtonClick();
				}
				else {
					if ((istream != null) && (bThisIsHTML))
						((CoolReader) activity).loadDocumentFromStreamExt(istream, sUri);
					if ((istreamTxt != null) && (!bThisIsHTML))
						((CoolReader) activity).loadDocumentFromStreamExt(istreamTxt, sUri);
					onPositiveButtonClick();
				}
			}
		});
		// ODF file must be saved for later convert
		if (stype.contains("opendocument")) hideExistingFromStreamControls(view);
		btnSave = (Button)view.findViewById(R.id.btn_save);
		setDashedButton(btnSave);
		//btnOpenFromStream.setBackgroundColor(colorGrayC);
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
						BackgroundThread.instance().postBackground(new Runnable() {
							@Override
							public void run() {
								BackgroundThread.instance().postGUI(new Runnable() {
									@Override
									public void run() {
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
											if (stype.contains("opendocument")) {
												DocConvertDialog dlgConv = new DocConvertDialog((CoolReader)activity, fName);
												dlgConv.show();
											} else {
												((CoolReader) activity).loadDocumentExt(fName, sUri);
											}
											onPositiveButtonClick();
										} catch (Exception e) {
											log.e("Error creating file: " + e.getClass().getSimpleName()+": "+e.getLocalizedMessage());
											activity.showToast("Error creating file: " + e.getClass().getSimpleName()+": "+e.getLocalizedMessage());
										}
									}
								}, 200);
							}
						});
					}
				}
			}
		});
		tvExistingPath = (TextView)view.findViewById(R.id.existing_path);
		tvExistingPath.setText(sExistingName);
		btnOpenExisting = (Button)view.findViewById(R.id.btn_open_existing);
		setDashedButton(btnOpenExisting);
		btnOpenExisting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((CoolReader) activity).loadDocumentExt(sExistingName, sUri);
				onPositiveButtonClick();
			}
		});
		if (StrUtils.isEmptyStr(sExistingName)) hideExistingFileControls(view);
		btnAsHTML = (Button)view.findViewById(R.id.btn_as_html);
		setDashedButton(btnAsHTML);
		btnAsHTML.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switchHTML(true);
			}
		});
		btnAsText = (Button)view.findViewById(R.id.btn_as_text);
		setDashedButton(btnAsText);
		btnAsText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switchHTML(false);
			}
		});
		if (uri != null) hideExistingHttpControls(view); else switchHTML(true);
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

