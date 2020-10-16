package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;

import okhttp3.HttpUrl;

public class OPDSCatalogEditDialog extends BaseDialog {

	private final CoolReader mActivity;
	private final LayoutInflater mInflater;
	private final FileInfo mItem;
	private final EditText nameEdit;
	private final ImageButton imgBtn;
	private final EditText urlEdit;
	private final EditText usernameEdit;
	private final EditText passwordEdit;
	private final EditText proxyaddrEdit;
	private final EditText proxyportEdit;
	private final EditText proxyunameEdit;
	private final EditText proxypasswEdit;
	private final CheckBox onionDefProxyChb;
	private final Runnable mOnUpdate;

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		String imgFName;
		ImageButton bmImage;

		public DownloadImageTask(String imgFName, ImageButton bmImage) {
			this.imgFName = imgFName;
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
				File file = new File(imgFName);
				OutputStream fOut = new FileOutputStream(file);
				mIcon11.compress(Bitmap.CompressFormat.PNG, 100, fOut);
				fOut.flush();
				fOut.close();
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			if ((bmImage!=null)&&(result!=null))
				if (bmImage.getDrawable()!=null) {
				Bitmap resizedBitmap = Bitmap.createScaledBitmap(
						result, bmImage.getDrawable().getIntrinsicWidth(), bmImage.getDrawable().getIntrinsicHeight(), false);
				bmImage.setImageBitmap(resizedBitmap);
				bmImage.setColorFilter(null);
				imgBtn.setEnabled(true);
				imgBtn.setVisibility(View.VISIBLE);
			}
		}
	}

	public OPDSCatalogEditDialog(CoolReader activity, FileInfo item, Runnable onUpdate) {
		super("OPDSCatalogEditDialog", activity, activity.getString((item.id == null) ? R.string.dlg_catalog_add_title
				: R.string.dlg_catalog_edit_title), true,
				false);
		mActivity = activity;
		mItem = item;
		mOnUpdate = onUpdate;
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.catalog_edit_dialog, null);
		nameEdit = view.findViewById(R.id.catalog_name);
		imgBtn = view.findViewById(R.id.test_catalog_btn);
		imgBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (!StrUtils.isEmptyStr(urlEdit.getText().toString())) {
					imgBtn.setEnabled(false);
					imgBtn.setVisibility(View.INVISIBLE);
					BackgroundThread.instance().postGUI(() -> {
						imgBtn.setEnabled(true);
						imgBtn.setVisibility(View.VISIBLE);
					}, 5000);
					HttpUrl url = HttpUrl.parse(urlEdit.getText().toString());
					if (url==null) {
						activity.showToast("Bad URL");
					} else {
						final HttpUrl.Builder urlBuilder = url.newBuilder();
						BackgroundThread.instance().postBackground(() -> {
							Document doc = null;
							try {
								doc = Jsoup.parse(urlBuilder.build().url(), 60000);
								if (doc != null) {
									for (Element el : doc.getAllElements()) {
										if (el.tag().getName().equals("title")) {
											if (el.parentNode() != null) {
												Element par = (Element) el.parentNode();
												if (par.tag().getName().equals("feed")) {
													BackgroundThread.instance().postGUI(() -> {
														nameEdit.setText(el.text());
														activity.showToast(activity.getString(R.string.ok));
													}, 200);
												}
											}
										}
										if (el.tag().getName().equals("icon")) {
											if (el.parentNode() != null) {
												Element par = (Element) el.parentNode();
												if (par.tag().getName().equals("feed")) {
													final String sUrl = urlEdit.getText().toString();
													CRC32 crc = new CRC32();
													crc.update(sUrl.getBytes());
													final String sFName = crc.getValue() + "_icon.png";
													String sDir = "";
													ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.IconDirs, true);
													if (tDirs.size() > 0) sDir = tDirs.get(0);
													if (!StrUtils.isEmptyStr(sDir))
														if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\")))
															sDir = sDir + "/";
													if (!StrUtils.isEmptyStr(sDir)) {
														try {
															File f = new File(sDir + sFName);
															if (f.exists()) f.delete();
															new DownloadImageTask(sDir + sFName, imgBtn).execute(el.text());
														} catch (Exception e) {
															BackgroundThread.instance().postGUI(() -> activity.showToast(activity.getString(R.string.pic_problem) + " " +
																	e.getMessage()), 200);
														}
													}
												}
											}
										} // if icon
									}
								} // if doc is not null
								//mActivity.showToast(doc.body().text());
							} catch (IOException e) {
								mActivity.showToast(activity.getString(R.string.error) + ": " + e.getMessage());
								Log.e("opds", "OPDS check catalog: " + e.getMessage());
							}
						});
					}
//					FileWriter fw = new FileWriter(fFName + ".txt");
//					fw.write(.replace((char) 0, ' '));
//					fw.close();
				}
			}
		});
		urlEdit = view.findViewById(R.id.catalog_url);
		usernameEdit = view.findViewById(R.id.catalog_username);
		passwordEdit = view.findViewById(R.id.catalog_password);
		proxyaddrEdit = view.findViewById(R.id.edt_proxy_addr);
		proxyportEdit = view.findViewById(R.id.edt_proxy_port);
		proxyunameEdit = view.findViewById(R.id.edt_proxy_uname);
		proxypasswEdit = view.findViewById(R.id.edt_proxy_passw);
		onionDefProxyChb = view.findViewById(R.id.chb_onion_def_proxy);
		nameEdit.setText(mItem.getFilename());
		urlEdit.setText(mItem.getOPDSUrl());
		usernameEdit.setText(mItem.username);
		passwordEdit.setText(mItem.password);
		proxyaddrEdit.setText(mItem.proxy_addr);
		proxyportEdit.setText(mItem.proxy_port);
		proxyunameEdit.setText(mItem.proxy_uname);
		proxypasswEdit.setText(mItem.proxy_passw);
		onionDefProxyChb.setChecked(mItem.onion_def_proxy==1);
		setThirdButtonImage(
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_minus, R.drawable.icons8_minus),
				//R.drawable.icons8_minus,
				R.string.mi_catalog_delete);
		setView(view);
	}

	@Override
	protected void onPositiveButtonClick() {
		String url = urlEdit.getText().toString();
		boolean blacklist = checkBlackList(url);
		if (OPDSConst.BLACK_LIST_MODE == OPDSConst.BLACK_LIST_MODE_FORCE) {
			mActivity.showToast(R.string.black_list_enforced);
		} else if (OPDSConst.BLACK_LIST_MODE == OPDSConst.BLACK_LIST_MODE_WARN) {
			mActivity.askConfirmation(R.string.black_list_warning, () -> {
				save();
				OPDSCatalogEditDialog.super.onPositiveButtonClick();
			}, () -> onNegativeButtonClick());
		} else {
			save();
			super.onPositiveButtonClick();
		}
	}
	
	private boolean checkBlackList(String url) {
		for (String s : OPDSConst.BLACK_LIST) {
			if (s.equals(url))
				return true;
		}
		return false;
	}
	
	private void save() {
		activity.getDB().saveOPDSCatalog(mItem.id,
				urlEdit.getText().toString(), nameEdit.getText().toString(), 
				usernameEdit.getText().toString(), passwordEdit.getText().toString(),
				proxyaddrEdit.getText().toString(), proxyportEdit.getText().toString(),
				proxyunameEdit.getText().toString(), proxypasswEdit.getText().toString(),
				onionDefProxyChb.isChecked()?1:0);
		mOnUpdate.run();
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	@Override
	protected void onThirdButtonClick() {
		mActivity.askDeleteCatalog(mItem);
		super.onThirdButtonClick();
	}

	
}
