package org.coolreader.options;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.CloudFileInfo;
import org.coolreader.cloud.CloudSync;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ResizeHistory;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class RestoreSettingsOption extends SubmenuOption {

	private ListView listView;

	final BaseActivity mActivity;
	final Context mContext;

	public RestoreSettingsOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter) {
		super(owner, label, Settings.PROP_APP_RESTORE_SETTINGS, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("RestoreSettingOptionDialog", mActivity, label, false, false);
		View view = mInflater.inflate(R.layout.searchable_listview, null);
		LinearLayout viewList = view.findViewById(R.id.lv_list);
		final EditText tvSearchText = view.findViewById(R.id.search_text);
		ImageButton ibSearch = view.findViewById(R.id.btn_search);
		final OptionsListView listView = new OptionsListView(mContext, this);
		tvSearchText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				listView.listUpdated(cs.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

			@Override
			public void afterTextChanged(Editable arg0) {}
		});

		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT= Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		tvSearchText.setBackgroundColor(colorGrayCT);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		tvSearchText.setTextColor(mActivity.getTextColor(colorIcon));
		int colorIcon128 = Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		tvSearchText.setHintTextColor(colorIcon128);
		if (isEInk) Utils.setSolidEditEink(tvSearchText);

		File toFile = mActivity.getSettingsFileExt("[DEFAULT]", 0);
		File newFile = new File(toFile.getParent(), "settings");

		File[] newFile2 = newFile.listFiles();
		ArrayList<File> newFileList = new ArrayList<>();
		for (File f: newFile2) newFileList.add(f);

		Comparator<File> compareByName = (o1, o2) -> -(o1.getName().compareTo(o2.getName()));
		Collections.sort(newFileList, compareByName);

		for (File f: newFileList) {
			listView.add(new ClickOption(mOwner, f.getName(),
				Settings.PROP_APP_RESTORE_SETTINGS+"."+f.getName(), "",
				"", (view1, optionLabel, optionValue) -> {
					mActivity.askConfirmation(R.string.click_to_restore_warn, () -> {
						File toFileR = mActivity.getSettingsFileExt("[DEFAULT]", 0);
						File newFileR = new File(toFile.getParent(), "settings");
						File newFileR2 = new File(newFileR, optionLabel);
						if (newFileR2.exists()) {
							String arrS = Utils.readFileToStringOrEmpty(newFileR2.getPath());
							if (!StrUtils.isEmptyStr(arrS)) {
								boolean bWasErr = CloudAction.restoreSettingsFromTxt((CoolReader) mActivity, arrS, false);
								if (!bWasErr) {
									mActivity.showToast(mActivity.getString(R.string.ok) + ": " +
											mActivity.getString(R.string.settings_were_restored));
									mActivity.finish();
								} else {
									mActivity.showCloudToast(mActivity.getString(R.string.error) + ": " +
											mActivity.getString(R.string.settings_restore_error), true);
									mActivity.finish();
								}
							}
						}
					});
				}, true).setDefaultValue(mActivity.getString(R.string.click_to_restore)).
				setIconId(0));
		}
		viewList.addView(listView);
		ibSearch.setOnClickListener(v -> {
			tvSearchText.setText("");
			listView.listUpdated("");
		});
		dlg.setView(view);
		ibSearch.requestFocus();
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(Settings.PROP_APP_RESTORE_SETTINGS);
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
