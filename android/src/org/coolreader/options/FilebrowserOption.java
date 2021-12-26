package org.coolreader.options;

import android.content.Context;
import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.db.CRDBService;
import org.coolreader.library.AuthorAlias;

public class FilebrowserOption extends SubmenuOption {

	public static String[] sortOrderValues = {
			FileInfo.SortOrder.FILENAME.name(),
			FileInfo.SortOrder.FILENAME_DESC.name(),
			FileInfo.SortOrder.AUTHOR_TITLE.name(),
			FileInfo.SortOrder.AUTHOR_TITLE_DESC.name(),
			FileInfo.SortOrder.TITLE_AUTHOR.name(),
			FileInfo.SortOrder.TITLE_AUTHOR_DESC.name(),
			FileInfo.SortOrder.TIMESTAMP.name(),
			FileInfo.SortOrder.TIMESTAMP_DESC.name(),
	};

	public static int[] sortOrderLabels = {
			FileInfo.SortOrder.FILENAME.resourceId,
			FileInfo.SortOrder.FILENAME_DESC.resourceId,
			FileInfo.SortOrder.AUTHOR_TITLE.resourceId,
			FileInfo.SortOrder.AUTHOR_TITLE_DESC.resourceId,
			FileInfo.SortOrder.TITLE_AUTHOR.resourceId,
			FileInfo.SortOrder.TITLE_AUTHOR_DESC.resourceId,
			FileInfo.SortOrder.TIMESTAMP.resourceId,
			FileInfo.SortOrder.TIMESTAMP_DESC.resourceId,
	};

	public static int[] sortOrderAddInfos = {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};

	int[] mCoverPageSizes = new int[] {
			0, 1, 2//, 2, 3
	};
	int[] mCoverPageSizeTitles = new int[] {
			R.string.options_app_cover_page_size_small, R.string.options_app_cover_page_size_medium, R.string.options_app_cover_page_size_big
	};
	int[] mCoverPageSizeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mBrowserAction = new int[] {
			0, 1, 2, 4
	};
	int[] mBrowserActionTitles = new int[] {
			R.string.browser_tap_option1, R.string.browser_tap_option2,
			R.string.browser_tap_option3, R.string.browser_tap_option4
	};
	int[] mBrowserActionAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int [] mExternalDocumentCameDialogTimeout = new int[] {
			0, 1, 2, 3, 4, 5, 8, 10, 15, 20, 30, 45, 60
	};

	int [] mZipScan = new int[] {
			0, 1, 2
	};

	int [] mZipScanTitles = new int[] {
			R.string.detect_zip_0, R.string.detect_zip_1, R.string.detect_zip_2
	};

	int [] mZipScanAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.detect_zip1_add_info, R.string.detect_zip1_add_info
	};

	int[] mScanDepths = new int[] {
			0, 2, 1
	};

	int[] mScanDepthTitles = new int[] {
			R.string.scan_depth0, R.string.scan_depth1,
			R.string.scan_depth2
	};

	int[] mScanDepthAddInfos = new int[] {
			R.string.scan_depth0_add_info, R.string.scan_depth1_add_info,
			R.string.scan_depth2_add_info
	};

	public static int[] mBrowserMaxGroupItems;

	final BaseActivity mActivity;
	final Context mContext;

	public FilebrowserOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_FILEBROWSER_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("FilebrowserOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.mi_book_sort_order), Settings.PROP_APP_BOOK_SORT_ORDER,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue)
				.add(sortOrderValues, sortOrderLabels, sortOrderAddInfos).setDefaultValue(FileInfo.SortOrder.TITLE_AUTHOR.name()).
						setIconIdByAttr(R.attr.attr_icons8_alphabetical_sorting, R.drawable.icons8_alphabetical_sorting));
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_app_show_cover_pages), Settings.PROP_APP_SHOW_COVERPAGES,
				mActivity.getString(R.string.options_app_show_cover_pages_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_book, R.drawable.icons8_book));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_app_cover_page_size),
				Settings.PROP_APP_COVERPAGE_SIZE, mActivity.getString(R.string.options_app_cover_page_size_add_info), this.lastFilteredValue).
				add(mCoverPageSizes, mCoverPageSizeTitles, mCoverPageSizeAddInfos).
				setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_book_big_and_small, R.drawable.icons8_book_big_and_small));
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_app_scan_book_props),
				Settings.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED,
				mActivity.getString(R.string.options_app_scan_book_props_add_info), this.lastFilteredValue).
				setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_book_scan_properties,R.drawable.icons8_book_scan_properties));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.scan_depth),
				Settings.PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS, mActivity.getString(R.string.scan_depth_add_info), this.lastFilteredValue).
				add(mScanDepths, mScanDepthTitles, mScanDepthAddInfos).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_folder_scan, R.drawable.icons8_folder_scan));
//			listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_app_browser_hide_empty_dirs), Settings.PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS,
//					mActivity.getString(R.string.options_app_browser_hide_empty_dirs_add_info), this.lastFilteredValue).
//					//setComment(mActivity.getString(R.string.options_hide_empty_dirs_slowdown)).
//					setDefaultValue("0").noIcon());
		//CR implementation
		//listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_app_browser_hide_empty_genres), Settings.PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES,
		//		mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).setDefaultValue("0").noIcon());
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.mi_book_browser_simple_mode), Settings.PROP_APP_FILE_BROWSER_SIMPLE_MODE,
				mActivity.getString(R.string.mi_book_browser_simple_mode_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_file,R.drawable.icons8_file));
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.authors_aliases_load),
				Settings.PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_LOAD, mActivity.getString(R.string.authors_aliases_load_add_info), this.lastFilteredValue,
				view ->
				{
					try {
						CoolReader cr = (CoolReader) mActivity;
						AuthorAlias.initAliasesList(cr);
						if (AuthorAlias.AUTHOR_ALIASES.size()>0) {
							cr.showCenterPopup(view, mActivity.getString(R.string.authors_aliases_loading), true);
							cr.waitForCRDBService(() -> {
								mActivity.getDB().saveAuthorsAliasesInfo(AuthorAlias.AUTHOR_ALIASES, new CRDBService.AuthorsAliasesLoadingCallback() {
									@Override
									public void onAuthorsAliasesLoaded(int cnt) {
										cr.showPopup(view, mActivity.getString(R.string.authors_aliases_loaded) + " " + cnt, 1000, true, true, false);
										cr.showToast(mActivity.getString(R.string.authors_aliases_loaded) + " " + cnt);
									}

									@Override
									public void onAuthorsAliasesLoadProgress(int percent) {
										cr.showPopup(view, mActivity.getString(R.string.authors_aliases_loading) + ", " + percent + "%", 1000, true, true, false);
									}
								});
							});
						}
					} catch (Exception e) {
						Log.e("OPTIONS", "exception while init authors aliases list", e);
					}
				}, true).setDefaultValue(mActivity.getString(R.string.authors_aliases_load_add_info)).
				setIconIdByAttr(R.attr.attr_icons8_anon_load, R.drawable.icons8_anon_load));
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.authors_aliases_enabled), Settings.PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_ENABLED,
				mActivity.getString(R.string.authors_aliases_enabled_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_anon, R.drawable.icons8_anon));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.browser_tap_option_tap), Settings.PROP_APP_FILE_BROWSER_TAP_ACTION,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mBrowserAction, mBrowserActionTitles, mBrowserActionAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_book_tap, R.drawable.icons8_book_tap));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.browser_tap_option_longtap), Settings.PROP_APP_FILE_BROWSER_LONGTAP_ACTION,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mBrowserAction, mBrowserActionTitles, mBrowserActionAddInfos).setDefaultValue("1").
				setIconIdByAttr(R.attr.attr_icons8_book_long_tap, R.drawable.icons8_book_long_tap));
		mBrowserMaxGroupItems = mActivity.getResources().getIntArray(R.array.browser_max_group_items);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.mi_book_browser_max_group_size), Settings.PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE,
				mActivity.getString(R.string.mi_book_browser_max_group_size_add_info), this.lastFilteredValue).
				add(mBrowserMaxGroupItems).setDefaultValue("8").setIconIdByAttr(R.attr.attr_icons8_group, R.drawable.icons8_group));
		listView.add(new FilebrowserSecGroupOption(mActivity, mContext, mOwner, mActivity.getString(R.string.sec_group),
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_group2, R.drawable.icons8_group2));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.external_doc_came_dialog_timeout),
				Settings.PROP_APP_EXT_DOC_CAME_TIMEOUT, mActivity.getString(R.string.external_doc_came_dialog_timeout_add_info), this.lastFilteredValue).
				add(mExternalDocumentCameDialogTimeout).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_blackpage_interval, R.drawable.icons8_blackpage_interval));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.detect_zip),
				Settings.PROP_APP_FILE_BROWSER_ZIP_SCAN, mActivity.getString(R.string.detect_zip_add_info), this.lastFilteredValue).
				add(mZipScan, mZipScanTitles, mZipScanAddInfos).
				setDefaultValue("0").setIconIdByAttr(R.attr.cr3_browser_folder_zip_drawable, R.drawable.icons8_zip));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.mi_book_sort_order), Settings.PROP_APP_BOOK_SORT_ORDER,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_show_cover_pages), Settings.PROP_APP_SHOW_COVERPAGES,
				mActivity.getString(R.string.options_app_show_cover_pages_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_cover_page_size), Settings.PROP_APP_COVERPAGE_SIZE,
				mActivity.getString(R.string.options_app_cover_page_size_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_scan_book_props), Settings.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED,
				mActivity.getString(R.string.options_app_scan_book_props_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_browser_hide_empty_dirs), Settings.PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS,
				mActivity.getString(R.string.options_app_browser_hide_empty_dirs_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_hide_empty_dirs_slowdown));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_browser_hide_empty_genres), Settings.PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.mi_book_browser_simple_mode), Settings.PROP_APP_FILE_BROWSER_SIMPLE_MODE,
				mActivity.getString(R.string.mi_book_browser_simple_mode_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.mi_book_browser_max_group_size), Settings.PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE,
				mActivity.getString(R.string.mi_book_browser_max_group_size_add_info));
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
		this.updateFilteredMark(mActivity.getString(R.string.sec_group), Settings.PROP_FILEBROWSER_SEC_GROUP,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.browser_tap_option_longtap), Settings.PROP_APP_FILE_BROWSER_LONGTAP_ACTION,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.detect_zip), Settings.PROP_APP_FILE_BROWSER_ZIP_SCAN,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.detect_zip_0), mActivity.getString(R.string.detect_zip_1),
				mActivity.getString(R.string.detect_zip_2));
		this.updateFilteredMark(mActivity.getString(R.string.detect_zip_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
