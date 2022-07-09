package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class PageColorsOption extends SubmenuOption {

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	public PageColorsOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_PAGECOLORS_TITLE, addInfo, filter);
		mActivity = activity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("PageColorsOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		listView.add(new NightModeOption(mOwner, mActivity.getString(R.string.options_inverse_view), Settings.PROP_NIGHT_MODE,
				mActivity.getString(R.string.options_inverse_view_add_info), this.lastFilteredValue).setIconIdByAttr(R.attr.cr3_option_night_drawable, R.drawable.cr3_option_night));
		mOptionsDialog.mTitleBarFontColor2 = new ColorOption(mOwner,
				mActivity.getString(R.string.options_page_titlebar_font_color)+" ("+
						mActivity.getString(R.string.options_page_titlebar_short)+")", Settings.PROP_STATUS_FONT_COLOR, 0x000000, mActivity.getString(R.string.option_add_info_empty_text),
				this.lastFilteredValue).setIconIdByAttr(R.attr.attr_icons8_font_color,
				R.drawable.icons8_font_color);
		listView.add(mOptionsDialog.mTitleBarFontColor2);
		listView.add(new ColorOption(mOwner, mActivity.getString(R.string.options_color_text), Settings.PROP_FONT_COLOR, 0x000000,
				mActivity.getString(R.string.options_color_text_add_info), this.lastFilteredValue).setIconIdByAttr(R.attr.attr_icons8_font_color, R.drawable.icons8_font_color));
		listView.add(new ColorOption(mOwner, mActivity.getString(R.string.options_color_background), Settings.PROP_BACKGROUND_COLOR, 0xFFFFFF,
				mActivity.getString(R.string.options_color_background_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_paint_palette1, R.drawable.icons8_paint_palette1));
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			listView.add(new ColorOption(mOwner, mActivity.getString(R.string.options_view_color_selection), Settings.PROP_HIGHLIGHT_SELECTION_COLOR,
					0xCCCCCC, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).noIcon());
			listView.add(new ColorOption(mOwner, mActivity.getString(R.string.options_view_color_bookmark_comment), Settings.PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT,
					0xFFFF40, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).noIcon());
			listView.add(new ColorOption(mOwner, mActivity.getString(R.string.options_view_color_bookmark_correction), Settings.PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION,
					0xFF8000, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).noIcon());
		}
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_app_settings_icons_is_custom_color), Settings.PROP_APP_ICONS_IS_CUSTOM_COLOR,
					mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).setDefaultValue("0")
						.setIconIdByAttr(R.attr.attr_icons8_paint_palette1, R.drawable.icons8_paint_palette1));
			listView.add(new ColorOption(mOwner, mActivity.getString(R.string.options_app_settings_icons_custom_color), Settings.PROP_APP_ICONS_CUSTOM_COLOR, 0x000000,
					mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue)
						.setIconIdByAttr(R.attr.attr_icons8_paint_palette1, R.drawable.icons8_paint_palette1));
		}
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_inverse_view), Settings.PROP_NIGHT_MODE,
				mActivity.getString(R.string.options_inverse_view_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_color_text), Settings.PROP_FONT_COLOR,
				mActivity.getString(R.string.options_color_text_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_color_background), Settings.PROP_BACKGROUND_COLOR,
				mActivity.getString(R.string.options_color_background_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_view_color_selection), Settings.PROP_HIGHLIGHT_SELECTION_COLOR,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_view_color_bookmark_comment), Settings.PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_view_color_bookmark_correction), Settings.PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_settings_icons_is_custom_color), Settings.PROP_APP_ICONS_IS_CUSTOM_COLOR,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_settings_icons_custom_color), Settings.PROP_APP_ICONS_CUSTOM_COLOR,
				mActivity.getString(R.string.option_add_info_empty_text));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
