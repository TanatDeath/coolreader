package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class FilebrowserSecGroupOption extends SubmenuOption {

	int[] mSecGroupCommon = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9
	};

	int[] mSecGroupCommonTitles = new int[] {
			R.string.folder_name_books_by_author,
			R.string.folder_name_books_by_series,
			R.string.folder_name_books_by_genre,
			R.string.folder_name_books_by_bookdate,
			R.string.folder_name_books_by_docdate,
			R.string.folder_name_books_by_publyear,
			R.string.folder_name_books_by_filedate,
			R.string.folder_name_books_by_rating,
			R.string.folder_name_books_by_state,
			R.string.folder_name_books_by_title
	};

	int[] mSecGroupCommonTitlesAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mSecGroupCommon2 = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
	};

	int[] mSecGroupCommonTitles2 = new int[] {
			R.string.same_as_common,
			R.string.folder_name_books_by_author,
			R.string.folder_name_books_by_series,
			R.string.folder_name_books_by_genre,
			R.string.folder_name_books_by_bookdate,
			R.string.folder_name_books_by_docdate,
			R.string.folder_name_books_by_publyear,
			R.string.folder_name_books_by_filedate,
			R.string.folder_name_books_by_rating,
			R.string.folder_name_books_by_state,
			R.string.folder_name_books_by_title
	};

	int[] mSecGroupCommonTitlesAddInfos2 = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mSecGroupCommon3 = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7
	};

	int[] mSecGroupCommonTitles3 = new int[] {
			R.string.same_as_common,
			R.string.folder_name_books_by_author,
			R.string.folder_name_books_by_series,
			R.string.folder_name_books_by_genre,
			R.string.folder_name_books_by_date,
			R.string.folder_name_books_by_rating,
			R.string.folder_name_books_by_state,
			R.string.folder_name_books_by_title
	};

	int[] mSecGroupCommonTitlesAddInfos3 = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	final BaseActivity mActivity;
	final Context mContext;

	public FilebrowserSecGroupOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_FILEBROWSER_SEC_GROUP, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("FilebrowserSecGroupOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sec_group_common), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSecGroupCommon, mSecGroupCommonTitles, mSecGroupCommonTitlesAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_group2, R.drawable.icons8_group2));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sec_group_author), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_folder_author, R.drawable.icons8_folder_author));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sec_group_series), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_folder_hash, R.drawable.icons8_folder_hash));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sec_group_genres), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_theatre_mask, R.drawable.icons8_theatre_mask));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sec_group_rating), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_RATING,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_folder_stars, R.drawable.icons8_folder_stars));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sec_group_state), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_STATE,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_folder_num, R.drawable.icons8_folder_num));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sec_group_dates), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_DATES,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSecGroupCommon3, mSecGroupCommonTitles3, mSecGroupCommonTitlesAddInfos3).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_folder_year, R.drawable.icons8_folder_year));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sec_group_search), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_SEARCH,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_folder_scan, R.drawable.icons8_folder_scan));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.sec_group_common), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.sec_group_author), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.sec_group_dates), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_DATES,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.sec_group_genres), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.sec_group_rating), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_RATING,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.sec_group_search), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_SEARCH,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.sec_group_series), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.sec_group_state), Settings.PROP_APP_FILE_BROWSER_SEC_GROUP_STATE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.browser_tap_option_tap), Settings.PROP_APP_FILE_BROWSER_TAP_ACTION,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.browser_tap_option_longtap), Settings.PROP_APP_FILE_BROWSER_LONGTAP_ACTION,
				mActivity.getString(R.string.option_add_info_empty_text));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
