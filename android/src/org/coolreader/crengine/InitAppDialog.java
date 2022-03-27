package org.coolreader.crengine;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.BaseDB;
import org.coolreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class InitAppDialog extends BaseDialog {

	public static ProgressDialog progressDlg;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final ViewGroup mDialog;
	private final Button mBtnClearCache;
	private final Button mBtnClearSettings;
	private final Button mBtnClearDB;
	private final Button mBtnPerformSelected;
	private boolean clearCache = false;
	private boolean clearSettings = false;
	private boolean clearDB = false;
	private int iErrCnt = 0;

	public InitAppDialog(CoolReader activity)
	{
		super(activity, activity.getString(R.string.init_app), false, false);
		mCoolReader = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mCoolReader, isEInk);
		setTitle(mCoolReader.getString(R.string.init_app));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.init_app_dialog, null);
		mDialog = (ViewGroup) view;
		//llScanLibrary = view.findViewById(R.id.ll_scan_library_dlg);
		mBtnClearCache = view.findViewById(R.id.btn_clear_cache);
		mBtnClearSettings = view.findViewById(R.id.btn_clear_settings);
		mBtnClearDB = view.findViewById(R.id.btn_clear_db);
		mBtnPerformSelected = view.findViewById(R.id.btn_perform_selected);

		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		Drawable img3 = img.getConstantState().newDrawable().mutate();
		mBtnClearCache.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		mBtnClearSettings.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		mBtnClearDB.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
		mBtnClearCache.setOnClickListener(v -> {
			buttonPressed(mBtnClearCache);
		});
		mBtnClearSettings.setOnClickListener(v -> {
			buttonPressed(mBtnClearSettings);
		});
		mBtnClearDB.setOnClickListener(v -> {
			buttonPressed(mBtnClearDB);
		});
		Utils.setDashedButton(mBtnPerformSelected);
		if (isEInk) Utils.setDashedButtonEink(mBtnPerformSelected);
		mBtnPerformSelected.setOnClickListener(v -> {
			performSelected();
		});
		buttonPressed(null);
		setView(view);
	}

	private void buttonPressed(Button btn) {
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		if (isEInk) colorGrayCT = Color.WHITE;
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mCoolReader.tintViewIcons(mBtnClearCache, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnClearSettings, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnClearDB, PorterDuff.Mode.CLEAR,true);
		if (btn == mBtnClearCache) clearCache = !clearCache;
		if (btn == mBtnClearSettings) clearSettings = !clearSettings;
		if (btn == mBtnClearDB) clearDB = !clearDB;
		if (clearCache) {
			mBtnClearCache.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnClearCache,true);
		} else mBtnClearCache.setBackgroundColor(colorGrayCT);
		if (isEInk) Utils.setSolidButtonEink(mBtnClearCache);
		if (clearSettings) {
			mBtnClearSettings.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnClearSettings,true);
		} else mBtnClearSettings.setBackgroundColor(colorGrayCT);
		if (isEInk) Utils.setSolidButtonEink(mBtnClearSettings);
		if (clearDB) {
			mBtnClearDB.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnClearDB,true);
		} else mBtnClearDB.setBackgroundColor(colorGrayCT);
		if (isEInk) Utils.setSolidButtonEink(mBtnClearDB);
	}

	boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		boolean succ = directoryToBeDeleted.delete();
		if (!succ) iErrCnt++;
		return succ;
	}

	private void performSelected() {
		iErrCnt = 0;
		if ((!clearCache) && (!clearSettings) && (!clearDB)) {
			mCoolReader.showToast(mCoolReader.getString(R.string.actions_selected)+" 0. ");
			return;
		}
		int selected = 0;
		if (clearCache) selected++;
		if (clearSettings) selected++;
		if (clearDB) selected++;
		mCoolReader.askQuestion(mCoolReader.getString(R.string.confirm_to_proceed),
				mCoolReader.getString(R.string.actions_selected) + " " +selected+ ". " + mCoolReader.getString(R.string.confirm_to_proceed),
				() -> {
					if (clearCache) {
						boolean bCacheFound = false;
						File fSett = mCoolReader.getSettingsFileF(0);
						File fCR3E = fSett.getParentFile();
						if (fCR3E != null) {
							File[] allContents = fCR3E.listFiles();
							for (File file : allContents) {
								if ((file.getName().equals("cache")) && (file.isDirectory())) {
									bCacheFound = true;
									deleteDirectory(file);
								}
							}
						}
						if (!bCacheFound) iErrCnt++;
					}
					if (clearSettings) {
						int iSettClount = 1;
						ArrayList<File> arrSett = new ArrayList<>();
						File fSett = mCoolReader.getSettingsFileF(0);
						while (fSett.exists()) {
							arrSett.add(fSett);
							fSett = mCoolReader.getSettingsFileF(iSettClount);
							iSettClount++;
						}
						for (File settFile: arrSett)
							if (!settFile.delete()) iErrCnt++;
					}
					if (clearDB) {
						mCoolReader.waitForCRDBService(() -> {
									BaseDB db = Services.getHistory().getMainDB(mCoolReader.getDB());
									BaseDB cdb = Services.getHistory().getCoverDB(mCoolReader.getDB());
									File dbFile = db.getFileName();
									if (dbFile.exists()) {
										db.close();
										if (!dbFile.delete()) iErrCnt++;
									}
									File cdbFile = cdb.getFileName();
									if (cdbFile.exists()) {
										cdb.close();
										if (!cdbFile.delete()) iErrCnt++;
									}
								}
						);
					}
					if (iErrCnt == 0)
						mCoolReader.showToast(R.string.actions_performed);
					else {
						mCoolReader.showToast(mCoolReader.getString(R.string.actions_performed_err) + " " + iErrCnt +
								". " + mCoolReader.getString(R.string.closing_app));
					}
					mCoolReader.finish();
				}, null);
	}

	@Override
	public void onPositiveButtonClick() {
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}
}
