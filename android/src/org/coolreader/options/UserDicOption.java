package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class UserDicOption extends SubmenuOption {

	int[] mUserDicPanelKind = new int[] {
			0, 1, 2
	};

	int[] mUserDicPanelKindTitles = new int[] {
			R.string.user_dic_panel0, R.string.user_dic_panel1, R.string.user_dic_panel2
	};

	int[] mUserDicPanelKindAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mUserDicContent = new int[] {
			0, 1
	};

	int[] mUserDicContentTitles = new int[] {
			R.string.user_dic_content_0, R.string.user_dic_content_1
	};

	int[] mUserDicContentInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mWordsDontSaveIfMore = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20
	};

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	public UserDicOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_ADD_DIC_TITLE, addInfo, filter);
		mActivity = activity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("UserDicOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_app_show_user_dic_panel), Settings.PROP_APP_SHOW_USER_DIC_PANEL,
				mActivity.getString(R.string.options_app_show_user_dic_panel_add_info), this.lastFilteredValue).
				add(mUserDicPanelKind, mUserDicPanelKindTitles, mUserDicPanelKindAddInfos).
				setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_user_dic_panel,R.drawable.icons8_user_dic_panel));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.user_dic_content), Settings.PROP_APP_SHOW_USER_DIC_CONTENT,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mUserDicContent, mUserDicContentTitles, mUserDicContentInfos).setDefaultValue("0").noIcon());
		FlowListOption optFontSize = new FlowListOption(mOwner, mActivity.getString(R.string.options_font_size_user_dic), Settings.PROP_FONT_SIZE_USER_DIC,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
		for (int i = 0; i <= OptionsDialog.mFontSizes.length-1; i++) optFontSize.add(""+OptionsDialog.mFontSizes[i], ""+OptionsDialog.mFontSizes[i],"");
		optFontSize.setDefaultValue("24").setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size);
		listView.add(optFontSize);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.dict_dont_save_if_more),
				Settings.PROP_APP_DICT_DONT_SAVE_IF_MORE, mActivity.getString(R.string.dict_dont_save_if_more_add_info), this.lastFilteredValue).
				add(mWordsDontSaveIfMore).setDefaultValue("0").
				noIcon());
		listView.add(OptionsDialog.getOption(Settings.PROP_INSPECTOR_MODE_NO_DIC_HISTORY, this.lastFilteredValue));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_app_show_user_dic_panel), Settings.PROP_APP_SHOW_USER_DIC_PANEL,
				mActivity.getString(R.string.options_app_show_user_dic_panel_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_font_size_user_dic), Settings.PROP_FONT_SIZE_USER_DIC,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mUserDicPanelKindTitles) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.dict_dont_save_if_more), Settings.PROP_APP_DICT_DONT_SAVE_IF_MORE,
				mActivity.getString(R.string.dict_dont_save_if_more_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.inspector_mode_no_dic_history), Settings.PROP_INSPECTOR_MODE_NO_DIC_HISTORY,
				mActivity.getString(R.string.option_add_info_empty_text));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
