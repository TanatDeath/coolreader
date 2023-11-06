package org.coolreader.options;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class RootScreenOption extends SubmenuOption {

	final CoolReader mActivity;
	final OptionsDialog mOptionsDialog;

	int [] mSectionPos = new int [] {
			1, 2, 3, 4, 5, 6
	};
	int [] mSectionPosTitles = new int [] {
			R.string.root_screen_section_pos_1,
			R.string.root_screen_section_pos_2,
			R.string.root_screen_section_pos_3,
			R.string.root_screen_section_pos_4,
			R.string.root_screen_section_pos_5,
			R.string.root_screen_section_pos_hide
	};

	int [] mSectionPosAddInfos = new int [] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int [] mSectionType = new int [] {
			1, 2
	};
	int [] mSectionTypeTitles = new int [] {
			R.string.root_screen_section_type_1,
			R.string.root_screen_section_type_2
	};

	int [] mSectionTypeAddInfos = new int [] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int [] mSectionPos1 = new int [] {
			1, 2, 3, 4, 5
	};
	int [] mSectionPosTitles1 = new int [] {
			R.string.root_screen_section_pos_1,
			R.string.root_screen_section_pos_2,
			R.string.root_screen_section_pos_3,
			R.string.root_screen_section_pos_4,
			R.string.root_screen_section_pos_5
	};

	int [] mSectionPosAddInfos1 = new int [] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	public RootScreenOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_ROOT_SCREEN_TITLE, addInfo, filter);
		mActivity = (CoolReader) activity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("RareOption", mActivity, label, false, false) {
			public void onClose()
			{
				if (mActivity.mHomeFrame != null) {
					mActivity.mHomeFrame.createViews();
				}
				super.onClose();
			}
		};
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_screen_section_pos_current_book),
				Settings.PROP_APP_ROOT_VIEW_CURRENT_BOOK_SECTION_POS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSectionPos1, mSectionPosTitles1, mSectionPosAddInfos1).setDefaultValue("1").
				noIcon());

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_screen_section_pos_recent),
				Settings.PROP_APP_ROOT_VIEW_RECENT_SECTION_POS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSectionPos, mSectionPosTitles, mSectionPosAddInfos).setDefaultValue("2").
				noIcon());

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_screen_section_pos_file_system),
				Settings.PROP_APP_ROOT_VIEW_FS_SECTION_POS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSectionPos, mSectionPosTitles, mSectionPosAddInfos).setDefaultValue("3").
				noIcon());

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_screen_section_pos_library),
				Settings.PROP_APP_ROOT_VIEW_LIBRARY_SECTION_POS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSectionPos, mSectionPosTitles, mSectionPosAddInfos).setDefaultValue("4").
				noIcon());

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_screen_section_pos_online_catalogs),
				Settings.PROP_APP_ROOT_VIEW_OPDS_SECTION_POS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSectionPos, mSectionPosTitles, mSectionPosAddInfos).setDefaultValue("5").
				noIcon());

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_screen_section_type_file_system),
				Settings.PROP_APP_ROOT_VIEW_FS_SECTION_TYPE,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSectionType, mSectionTypeTitles, mSectionTypeAddInfos).setDefaultValue("1").
				noIcon());

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_screen_section_type_library),
				Settings.PROP_APP_ROOT_VIEW_LIBRARY_SECTION_TYPE,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSectionType, mSectionTypeTitles, mSectionTypeAddInfos).setDefaultValue("1").
				noIcon());

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_screen_section_type_online_catalogs),
				Settings.PROP_APP_ROOT_VIEW_OPDS_SECTION_TYPE,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSectionType, mSectionTypeTitles, mSectionTypeAddInfos).setDefaultValue("1").
				noIcon());

		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.root_screen_section_pos_1));
		this.updateFilteredMark(mActivity.getString(R.string.root_screen_section_pos_2));
		this.updateFilteredMark(mActivity.getString(R.string.root_screen_section_pos_3));
		this.updateFilteredMark(mActivity.getString(R.string.root_screen_section_pos_4));
		this.updateFilteredMark(mActivity.getString(R.string.root_screen_section_pos_5));
		this.updateFilteredMark(mActivity.getString(R.string.root_screen_section_pos_hide));

		this.updateFilteredMark(mActivity.getString(R.string.options_screen_section_pos_current_book));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_section_pos_recent));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_section_pos_file_system));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_section_pos_library));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_section_pos_online_catalogs));

		this.updateFilteredMark(mActivity.getString(R.string.options_screen_section_type_file_system));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_section_type_library));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_section_type_online_catalogs));

		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
