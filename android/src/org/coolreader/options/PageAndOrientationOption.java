package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.readerview.ReaderView;
import org.coolreader.crengine.Settings;

public class PageAndOrientationOption extends SubmenuOption {

	int[] mLandscapePages = new int[] {
			1, 2
	};
	int[] mLandscapePagesTitles = new int[] {
			R.string.options_page_landscape_pages_one, R.string.options_page_landscape_pages_two
	};
	int[] mLandscapePagesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mViewModes = new int[] {
			1, 0
	};
	int[] mViewModeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mViewModeTitles = new int[] {
			R.string.options_view_mode_pages, R.string.options_view_mode_scroll
	};

	int[] mOrientations = new int[] {
			0, 1, 4, 5
	};
	int[] mOrientationsTitles = new int[] {
			R.string.options_page_orientation_0, R.string.options_page_orientation_90,
			R.string.options_page_orientation_sensor, R.string.options_page_orientation_system
	};
	int[] mOrientationsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mOrientations_API9 = new int[] {
			0, 1, 2, 3, 4, 5
	};
	int[] mOrientationsTitles_API9 = new int[] {
			R.string.options_page_orientation_0, R.string.options_page_orientation_90, R.string.options_page_orientation_180, R.string.options_page_orientation_270
			,R.string.options_page_orientation_sensor,R.string.options_page_orientation_system
	};
	int[] mOrientationsAddInfos_API9 = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	public static int[] mOrient;
	public static String[] mOrientTitles;
	public static int[] mOrientAddInfos;

	int[] mPageAnimationSpeed = new int[] {
			100, 200, 300, 500, 800
	};
	int[] mPageAnimationSpeedAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mPageAnimationSpeedTitles = new int[] {
			R.string.page_animation_speed_1, R.string.page_animation_speed_2, R.string.page_animation_speed_3,
			R.string.page_animation_speed_4, R.string.page_animation_speed_5
	};

	int[] mAnimation = new int[] {
			ReaderView.PAGE_ANIMATION_NONE, ReaderView.PAGE_ANIMATION_SLIDE, ReaderView.PAGE_ANIMATION_SLIDE2,
			ReaderView.PAGE_ANIMATION_PAPER, ReaderView.PAGE_ANIMATION_BLUR, ReaderView.PAGE_ANIMATION_DIM,
			ReaderView.PAGE_ANIMATION_BLUR_DIM, ReaderView.PAGE_ANIMATION_MAG, ReaderView.PAGE_ANIMATION_MAG_DIM
	};
	int[] mAnimationTitles = new int[] {
			R.string.options_page_animation_none, R.string.options_page_animation_slide, R.string.options_page_animation_slide_2_pages,
			R.string.options_page_animation_paperbook,
			R.string.options_page_animation_blur, R.string.options_page_animation_dim,
			R.string.options_page_animation_blur_dim, R.string.options_page_animation_mag, R.string.options_page_animation_mag_dim
	};
	int[] mAnimationAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	public PageAndOrientationOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_PAGEANDORIENTATION_TITLE, addInfo, filter);
		mActivity = activity;
		mOptionsDialog = od;
		mOrientTitles = activity.getResources().getStringArray(R.array.orient_titles);
		mOrient = activity.getResources().getIntArray(R.array.orient_values);
		mOrientAddInfos = activity.getResources().getIntArray(R.array.orient_add_infos);
		for (int i=0;i<mOrientAddInfos.length; i++) mOrientAddInfos[i] = R.string.option_add_info_empty_text;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("PageAndOrientationOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_view_mode), Settings.PROP_PAGE_VIEW_MODE,
				mActivity.getString(R.string.options_view_mode_add_info), this.lastFilteredValue).add(mViewModes, mViewModeTitles, mViewModeAddInfos).
				setDefaultValue("1").setIconIdByAttr(R.attr.cr3_option_view_mode_scroll_drawable, R.drawable.cr3_option_view_mode_scroll).
				setOnChangeHandler(() -> {
					int value = mProperties.getInt(Settings.PROP_PAGE_VIEW_MODE, 1);
					mOptionsDialog.mFootNotesOption.setEnabled(value == 1);
				}));
		if (DeviceInfo.getSDKLevel() >= 9) {
			listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_page_orientation), Settings.PROP_APP_SCREEN_ORIENTATION,
					mActivity.getString(R.string.options_page_orientation_add_info), this.lastFilteredValue).add(mOrientations_API9, mOrientationsTitles_API9, mOrientationsAddInfos_API9).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_page_orientation_landscape_drawable, R.drawable.cr3_option_page_orientation_landscape));
			listView.add(new ListOption(mOwner, mActivity.getString(R.string.orientation_popup_toolbar_duration),
					Settings.PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION,
					mActivity.getString(R.string.orient_add_info), this.lastFilteredValue).
					add(mOrient, mOrientTitles, mOrientAddInfos).setDefaultValue("10").setIconIdByAttr(R.attr.attr_icons8_disable_toolbar, R.drawable.icons8_disable_toolbar));
		}
		else
			listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_page_orientation), Settings.PROP_APP_SCREEN_ORIENTATION,
					mActivity.getString(R.string.options_page_orientation_add_info), this.lastFilteredValue).add(mOrientations, mOrientationsTitles, mOrientationsAddInfos).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_page_orientation_landscape_drawable, R.drawable.cr3_option_page_orientation_landscape));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_page_landscape_pages), Settings.PROP_LANDSCAPE_PAGES,
				mActivity.getString(R.string.options_page_landscape_pages_add_info), this.lastFilteredValue).add(mLandscapePages, mLandscapePagesTitles, mLandscapePagesAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.cr3_option_pages_two_drawable, R.drawable.cr3_option_pages_two));
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			listView.add(new ListOption(mOwner, mActivity.getString(R.string.page_animation_speed), Settings.PROP_PAGE_ANIMATION_SPEED,
					mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mPageAnimationSpeed, mPageAnimationSpeedTitles, mPageAnimationSpeedAddInfos).setDefaultValue("300").
					setIconIdByAttr(R.attr.attr_icons8_page_animation_speed, R.drawable.icons8_page_animation_speed));
			listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_page_animation), Settings.PROP_PAGE_ANIMATION,
					mActivity.getString(R.string.options_page_animation_add_info), this.lastFilteredValue).
					add(mAnimation, mAnimationTitles, mAnimationAddInfos).setDefaultValue("1").
					setIconIdByAttr(R.attr.attr_icons8_page_animation, R.drawable.icons8_page_animation));
		}
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_view_mode), Settings.PROP_PAGE_VIEW_MODE,
				mActivity.getString(R.string.options_view_mode_add_info));
		for (int i: mViewModeTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mViewModeAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_orientation), Settings.PROP_APP_SCREEN_ORIENTATION,
				mActivity.getString(R.string.options_page_orientation_add_info));
		for (int i: mOrientationsTitles_API9) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mOrientationsAddInfos_API9) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mOrientationsTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mOrientationsAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.orientation_popup_toolbar_duration), Settings.PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION,
				mActivity.getString(R.string.orient_add_info));
		for (String s: mOrientTitles) this.updateFilteredMark(s);
		for (int i: mOrientAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_landscape_pages), Settings.PROP_LANDSCAPE_PAGES,
				mActivity.getString(R.string.options_page_landscape_pages_add_info));
		for (int i: mLandscapePagesTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mLandscapePagesAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.page_animation_speed), Settings.PROP_PAGE_ANIMATION_SPEED,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mPageAnimationSpeedTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mPageAnimationSpeedAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_animation), Settings.PROP_PAGE_ANIMATION,
				mActivity.getString(R.string.options_page_animation_add_info));
		for (int i: mAnimationTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mAnimationAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
