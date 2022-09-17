package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class AddDicsOption extends SubmenuOption {

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	public AddDicsOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_ADD_DIC_TITLE, addInfo, filter);
		mActivity = activity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("AddDicsOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 3", Settings.PROP_APP_DICTIONARY_3,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 4", Settings.PROP_APP_DICTIONARY_4,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 5", Settings.PROP_APP_DICTIONARY_5,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 6", Settings.PROP_APP_DICTIONARY_6,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 7", Settings.PROP_APP_DICTIONARY_7,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 8", Settings.PROP_APP_DICTIONARY_8,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 9", Settings.PROP_APP_DICTIONARY_9,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 10", Settings.PROP_APP_DICTIONARY_10,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary)+" 3", Settings.PROP_APP_DICTIONARY_3,
				mActivity.getString(R.string.options_app_dictionary3_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary)+" 4", Settings.PROP_APP_DICTIONARY_4,
				mActivity.getString(R.string.options_app_dictionary3_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary)+" 5", Settings.PROP_APP_DICTIONARY_5,
				mActivity.getString(R.string.options_app_dictionary3_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary)+" 6", Settings.PROP_APP_DICTIONARY_6,
				mActivity.getString(R.string.options_app_dictionary3_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary)+" 7", Settings.PROP_APP_DICTIONARY_7,
				mActivity.getString(R.string.options_app_dictionary3_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary)+" 8", Settings.PROP_APP_DICTIONARY_8,
				mActivity.getString(R.string.options_app_dictionary3_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary)+" 9", Settings.PROP_APP_DICTIONARY_9,
				mActivity.getString(R.string.options_app_dictionary3_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary)+" 10", Settings.PROP_APP_DICTIONARY_10,
				mActivity.getString(R.string.options_app_dictionary3_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
