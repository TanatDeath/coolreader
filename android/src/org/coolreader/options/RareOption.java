package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class RareOption extends SubmenuOption {

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	public RareOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_RARE_TITLE, addInfo, filter);
		mActivity = activity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("RareOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		OptionBase srO = new SkippedResOption(mOptionsDialog.getContext(),
				mOwner, mActivity.getString(R.string.skipped_res), mActivity.getString(R.string.skipped_res_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_resolution,R.drawable.icons8_resolution);
		((SkippedResOption)srO).updateFilterEnd();
		listView.add(srO);
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_app_tapzone_hilite), Settings.PROP_APP_TAP_ZONE_HILIGHT,
				mActivity.getString(R.string.options_app_tapzone_hilite_add_info), this.lastFilteredValue).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_highlight_tap_zone, R.drawable.	icons8_highlight_tap_zone));
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
			listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_app_trackball_disable), Settings.PROP_APP_TRACKBALL_DISABLED,
					mActivity.getString(R.string.options_app_trackball_disable_add_info), this.lastFilteredValue).setDefaultValue("0").
					setIconIdByAttr(R.attr.attr_icons8_disable_trackball,R.drawable.icons8_disable_trackball));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_app_hide_state_dialogs), Settings.PROP_APP_HIDE_STATE_DIALOGS,
				mActivity.getString(R.string.options_app_hide_state_dialogs_add_info), this.lastFilteredValue).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_no_questions,R.drawable.icons8_no_dialogs));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_app_hide_css_warning), Settings.PROP_APP_HIDE_CSS_WARNING,
				mActivity.getString(R.string.options_app_hide_css_warning_add_info), this.lastFilteredValue).setDefaultValue(
				(!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) ? "1": "0").
				setIconIdByAttr(R.attr.attr_icons8_no_dialogs,R.drawable.icons8_no_dialogs));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_app_disable_safe_mode), Settings.PROP_APP_DISABLE_SAFE_MODE,
				mActivity.getString(R.string.options_app_disable_safe_mode_add_info), this.lastFilteredValue).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_no_safe_mode,R.drawable.icons8_no_safe_mode));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.simple_font_select_dialog), Settings.PROP_APP_USE_SIMPLE_FONT_SELECT_DIALOG,
				mActivity.getString(R.string.simple_font_select_dialog_add_info), this.lastFilteredValue).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_font_select_dialog,R.drawable.icons8_font_select_dialog));
		listView.add(new IconsBoolOption(mOptionsDialog, mOwner,
				mActivity.getString(R.string.options_app_settings_icons), Settings.PROP_APP_SETTINGS_SHOW_ICONS,
				mActivity.getString(R.string.options_app_settings_icons_add_info), this.lastFilteredValue).setDefaultValue("1").
				setIconIdByAttr(R.attr.attr_icons8_alligator,R.drawable.icons8_alligator));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.skipped_res), "",
				mActivity.getString(R.string.skipped_res_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.force_tts_koef), Settings.PROP_APP_TTS_FORCE_KOEF,
				mActivity.getString(R.string.force_tts_koef_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_tapzone_hilite), Settings.PROP_APP_TAP_ZONE_HILIGHT,
				mActivity.getString(R.string.options_app_tapzone_hilite_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_trackball_disable), Settings.PROP_APP_TRACKBALL_DISABLED,
				mActivity.getString(R.string.options_app_trackball_disable_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_hide_state_dialogs), Settings.PROP_APP_HIDE_STATE_DIALOGS,
				mActivity.getString(R.string.options_app_hide_state_dialogs_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_hide_css_warning), Settings.PROP_APP_HIDE_CSS_WARNING,
				mActivity.getString(R.string.options_app_hide_css_warning_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_disable_safe_mode), Settings.PROP_APP_DISABLE_SAFE_MODE,
				mActivity.getString(R.string.options_app_disable_safe_mode_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_settings_icons), Settings.PROP_APP_SETTINGS_SHOW_ICONS,
				mActivity.getString(R.string.options_app_settings_icons_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
