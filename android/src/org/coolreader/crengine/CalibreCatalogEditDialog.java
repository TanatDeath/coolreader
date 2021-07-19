package org.coolreader.crengine;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.CRC32;

import okhttp3.HttpUrl;

public class CalibreCatalogEditDialog extends BaseDialog {

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final FileInfo mItem;
	private final EditText nameEdit;
	private final Button btnIsLocal;
	private final Button btnIsRemoteYD;
	public final EditText edtLocalFolder;
	private final EditText edtRemoteFolderYD;
	private final ImageButton btnTestCatalog;
	private final ImageButton btnChooseCatalog;
	boolean isEInk;
	HashMap<Integer, Integer> themeColors;
	private final Runnable mOnUpdate;

	private boolean getCheckedFromTag(Object o) {
		if (o == null) return false;
		if (!(o instanceof String)) return false;
		if (o.equals("1")) return true;
		return false;
	}

	private void setCheckedTag(Button b) {
		if (b == null) return;
		btnIsLocal.setTag("0");
		btnIsRemoteYD.setTag("0");
		b.setTag("1");
	}

	private void paintScopeButtons() {
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mCoolReader.tintViewIcons(btnIsLocal, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(btnIsRemoteYD, PorterDuff.Mode.CLEAR,true);
		if (getCheckedFromTag(btnIsLocal.getTag())) {
			btnIsLocal.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnIsLocal,true);
		} else btnIsLocal.setBackgroundColor(colorGrayCT);
		if (getCheckedFromTag(btnIsRemoteYD.getTag())) {
			btnIsRemoteYD.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnIsRemoteYD,true);
		} else btnIsRemoteYD.setBackgroundColor(colorGrayCT);
	}

	public CalibreCatalogEditDialog(CoolReader activity, FileInfo item, Runnable onUpdate) {
		super("CalibreCatalogEditDialog", activity, activity.getString((item.id == null) ? R.string.dlg_catalog_add_title
				: R.string.dlg_calibre_catalog_edit_title), true,
				false);
		mCoolReader = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(activity, isEInk);
		mItem = item;
		mOnUpdate = onUpdate;
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.calibre_catalog_edit_dialog, null);
		nameEdit = view.findViewById(R.id.catalog_name);
		nameEdit.setText(StrUtils.getNonEmptyStr(item.getFilename(), true));
		btnIsLocal = view.findViewById(R.id.btn_is_local);
		btnIsLocal.setOnClickListener(v -> { setCheckedTag(btnIsLocal); paintScopeButtons(); });
		btnIsRemoteYD = view.findViewById(R.id.btn_is_remote_yd);
		btnIsRemoteYD.setOnClickListener(v -> { setCheckedTag(btnIsRemoteYD); paintScopeButtons(); });
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		btnIsLocal.setTag("1");
		btnIsLocal.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		btnIsRemoteYD.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		edtLocalFolder = view.findViewById(R.id.catalog_local_folder);
		btnTestCatalog = view.findViewById(R.id.test_catalog_btn);
		btnChooseCatalog = view.findViewById(R.id.choose_catalog_btn);
		if (item.id != null)
			edtLocalFolder.setText(StrUtils.getNonEmptyStr(item.pathname, true).replace(FileInfo.CALIBRE_DIR_PREFIX, ""));
		btnTestCatalog.setOnClickListener(v -> {
			boolean ex = false;
			try {
				File f = new File(edtLocalFolder.getText().toString());
				if ((f.exists()) && (f.isDirectory())) ex = true;
			} finally {
				// do nothing
			}
			if (!ex) mCoolReader.showToast(R.string.folder_not_exists);
			else mCoolReader.showToast(R.string.folder_exists);
		});
		btnChooseCatalog.setOnClickListener(v -> {
			final Intent chooserIntent = new Intent(
					mCoolReader,
					DirectoryChooserActivity.class);

			final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
					.newDirectoryName("NewFolder")
					.allowReadOnlyDirectory(true)
					.allowNewDirectoryNameModification(true)
					.build();

			chooserIntent.putExtra(
					DirectoryChooserActivity.EXTRA_CONFIG,
					config);
			mCoolReader.cced=this;
			mCoolReader.startActivityForResult(chooserIntent, CoolReader.REQUEST_CODE_CHOOSE_DIR);
		});
		edtRemoteFolderYD = view.findViewById(R.id.catalog_remote_folder);
 	    setThirdButtonImage(
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_minus, R.drawable.icons8_minus),
				//R.drawable.icons8_minus,
				R.string.mi_catalog_delete);
		paintScopeButtons();
		setView(view);
	}

	@Override
	protected void onPositiveButtonClick() {
		save();
		super.onPositiveButtonClick();
	}
	
	private void save() {
		boolean isLocal = getCheckedFromTag(btnIsLocal.getTag());
		activity.getDB().saveCalibreCatalog(mItem.id,
				nameEdit.getText().toString(),
				isLocal,
				edtLocalFolder.getText().toString(),
				edtRemoteFolderYD.getText().toString());
		mOnUpdate.run();
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	@Override
	protected void onThirdButtonClick() {
		mCoolReader.askDeleteCalibreCatalog(mItem);
		super.onThirdButtonClick();
	}

	
}
