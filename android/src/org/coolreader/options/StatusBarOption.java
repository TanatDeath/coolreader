package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class StatusBarOption extends SubmenuOption {

	int[] mStatusPositions = new int[] {
			Settings.VIEWER_STATUS_NONE,
			//Settings.VIEWER_STATUS_TOP, Settings.VIEWER_STATUS_BOTTOM,
			Settings.VIEWER_STATUS_PAGE_HEADER,
			Settings.VIEWER_STATUS_PAGE_FOOTER,
			Settings.VIEWER_STATUS_PAGE_2LINES_HEADER,
			Settings.VIEWER_STATUS_PAGE_2LINES_FOOTER
	};
	int[] mStatusPositionsTitles = new int[] {
			R.string.options_page_show_titlebar_hidden,
			//R.string.options_page_show_titlebar_top, R.string.options_page_show_titlebar_bottom,
			R.string.options_page_show_titlebar_page_header,
			R.string.options_page_show_titlebar_page_footer,
			R.string.options_page_show_titlebar_page_header_2lines,
			R.string.options_page_show_titlebar_page_footer_2lines
	};

	int[] mStatusPositionsAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mScreenMod = new int[] {
			0, 1, 2
	};
	int[] mScreenModAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};
	int[] mScreenModTitles = new int[] {
			R.string.screen_mod_0, R.string.screen_mod_1,
			R.string.screen_mod_2,
	};

	int[] mRoundedCornersMargins = new int[] {
			0, 5, 10, 15, 20, 30, 40, 50, 60, 70,80, 90, 100, 120, 140, 160
	};

	int[] mRoundedCornersMarginPos = new int[] {
			0, 1, 2
	};
	int[] mRoundedCornersMarginPosAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};
	int[] mRoundedCornersMarginPosTitles = new int[] {
			R.string.rounded_corners_margin_pos_0, R.string.rounded_corners_margin_pos_1,
			R.string.rounded_corners_margin_pos_2,
	};

	int[] mExtFullscreenMargin = new int[] {
			0, 1, 2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 80, 90, 100
	};
	int[] mExtFullscreenMarginAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mExtFullscreenMarginPosTitles = new int[] {
			R.string.ext_fullscreen_margin_0, R.string.ext_fullscreen_margin_1,
			R.string.ext_fullscreen_margin_3,
			-5, -10, -15, -20, -25, -30, -35, -40, -45, -50, -55, -60, -65, -70, -80, -90, -100
	};

	final OptionsDialog mOptionsDialog;

	public StatusBarOption(OptionsDialog od, OptionOwner owner, String label, String property, String addInfo, String filter ) {
		super(owner, label, property, addInfo, filter);
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("StatusBarDialog", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar), Settings.PROP_STATUS_LOCATION, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mStatusPositions,
				mStatusPositionsTitles, mStatusPositionsAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_document_r_title,
				R.drawable.icons8_document_r_title));
		listView.add(new FontSelectOption(mOwner, mActivity.getString(R.string.options_page_titlebar_font_face), Settings.PROP_STATUS_FONT_FACE,
				mActivity.getString(R.string.option_add_info_empty_text), false, this.lastFilteredValue).setIconIdByAttr(R.attr.cr3_option_font_face_drawable,
				R.drawable.cr3_option_font_face));
		//listView.add(new NumberPickerOption(mOwner, mActivity.getString(R.string.options_page_titlebar_font_size), PROP_STATUS_FONT_SIZE).setMinValue(mActivity.getMinFontSize()).setMaxValue(mActivity.getMaxFontSize()).setDefaultValue("18").setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size));
		FlowListOption optFontSize = (FlowListOption)
				new FlowListOption(mOwner, mActivity.getString(R.string.options_page_titlebar_font_size),
				Settings.PROP_STATUS_FONT_SIZE, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue)
				.setDefaultValue("18").setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size);
		for (int i = 0; i <= OptionsDialog.mFontSizes.length-1; i++) optFontSize.add(""+OptionsDialog.mFontSizes[i], ""+OptionsDialog.mFontSizes[i],"");
		listView.add(optFontSize);
		mOptionsDialog.mTitleBarFontColor1 = new ColorOption(mOwner,
				mActivity.getString(R.string.options_page_titlebar_font_color)+" ("+
						mActivity.getString(R.string.options_page_titlebar_short)+")", Settings.PROP_STATUS_FONT_COLOR, 0x000000, mActivity.getString(R.string.option_add_info_empty_text),
				this.lastFilteredValue).setIconIdByAttr(R.attr.attr_icons8_font_color,
				R.drawable.icons8_font_color);
		listView.add(mOptionsDialog.mTitleBarFontColor1);
//			listView.add(new ColorOption(mOwner, mActivity.getString(R.string.options_page_titlebar_font_color), PROP_STATUS_FONT_COLOR, 0x000000, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
//					setIconIdByAttr(R.attr.attr_icons8_font_color,
//							R.drawable.icons8_font_color));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar_title), Settings.PROP_SHOW_TITLE,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_book_title2,
				R.drawable.icons8_book_title2));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar_page_number), Settings.PROP_SHOW_PAGE_NUMBER, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).
				setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_page_num,
				R.drawable.icons8_page_num));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar_page_count), Settings.PROP_SHOW_PAGE_COUNT, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).
				setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_pages_total,
				R.drawable.icons8_pages_total));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar_pages_to_chapter), Settings.PROP_SHOW_PAGES_TO_CHAPTER,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).
				setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_page_num,
				R.drawable.icons8_page_num));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar_time_left), Settings.PROP_SHOW_TIME_LEFT,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).
				setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_page_num,
				R.drawable.icons8_page_num));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar_percent), Settings.PROP_SHOW_POS_PERCENT, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_page_percent,
				R.drawable.icons8_page_percent));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar_chapter_marks), Settings.PROP_STATUS_CHAPTER_MARKS, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).
				setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_chapter_marks,
				R.drawable.icons8_chapter_marks));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_page_show_titlebar_battery_percent), Settings.PROP_SHOW_BATTERY_PERCENT, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).
				setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_battery_percent,
				R.drawable.icons8_battery_percent));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_rounded_corners_margin), Settings.PROP_ROUNDED_CORNERS_MARGIN,
				mActivity.getString(R.string.options_rounded_corners_margin_add_info), this.lastFilteredValue).add(mRoundedCornersMargins).setDefaultValue("0")
				.setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin, R.drawable.icons8_rounded_corners_margin));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.rounded_corners_margin_pos_text), Settings.PROP_ROUNDED_CORNERS_MARGIN_POS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mRoundedCornersMarginPos,
				mRoundedCornersMarginPosTitles, mRoundedCornersMarginPosAddInfos).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin2, R.drawable.icons8_rounded_corners_margin2));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.rounded_corners_margin_mod_text), Settings.PROP_ROUNDED_CORNERS_MARGIN_MOD,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mScreenMod,
				mScreenModTitles, mScreenModAddInfos).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin2, R.drawable.icons8_rounded_corners_margin2));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.rounded_corners_margin_fullscreen_only), Settings.PROP_ROUNDED_CORNERS_MARGIN_FSCR,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin2, R.drawable.icons8_rounded_corners_margin2));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.ext_fullscreen_margin_text), Settings.PROP_EXT_FULLSCREEN_MARGIN,
				mActivity.getString(R.string.ext_fullscreen_margin_add_info), this.lastFilteredValue).add2(mExtFullscreenMargin,
				R.string.ext_fullscreen_margin_2,
				mExtFullscreenMarginPosTitles, mExtFullscreenMarginAddInfos).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_ext_fullscreen, R.drawable.icons8_ext_fullscreen));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.ext_fullscreen_margin_mod), Settings.PROP_EXT_FULLSCREEN_MOD,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mScreenMod,
				mScreenModTitles, mScreenModAddInfos).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin2, R.drawable.icons8_rounded_corners_margin2));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_page_show_titlebar), Settings.PROP_STATUS_LOCATION,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_titlebar_font_face), Settings.PROP_STATUS_FONT_FACE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_titlebar_font_size), Settings.PROP_STATUS_FONT_SIZE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_titlebar_font_color), Settings.PROP_STATUS_FONT_COLOR,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_show_titlebar_title), Settings.PROP_SHOW_TITLE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_show_titlebar_page_number), Settings.PROP_SHOW_PAGE_NUMBER,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_show_titlebar_page_count), Settings.PROP_SHOW_PAGE_COUNT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_show_titlebar_pages_to_chapter), Settings.PROP_SHOW_PAGES_TO_CHAPTER,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_show_titlebar_percent), Settings.PROP_SHOW_POS_PERCENT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_show_titlebar_chapter_marks), Settings.PROP_STATUS_CHAPTER_MARKS,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_show_titlebar_battery_percent), Settings.PROP_SHOW_BATTERY_PERCENT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.ext_fullscreen_margin_text), Settings.PROP_EXT_FULLSCREEN_MARGIN,
				mActivity.getString(R.string.ext_fullscreen_margin_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.ext_fullscreen_margin_mod), Settings.PROP_EXT_FULLSCREEN_MOD,
				mActivity.getString(R.string.option_add_info_empty_text));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
