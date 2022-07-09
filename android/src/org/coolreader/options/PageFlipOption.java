package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class PageFlipOption extends SubmenuOption {

	int[] mFlippingType = new int[] {
			0, 1, 2, 3
	};

	int[] mFlippingTypeTitles = new int[] {
			R.string.page_flipping_type_0,
			R.string.page_flipping_type_1,
			R.string.page_flipping_type_2,
			R.string.page_flipping_type_3
	};

	int[] mFlippingTypeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mFlippingSensivity = new int[] {
			1, 2, 3, 4, 5
	};

	int[] mFlippingSensivityTitles = new int[] {
			R.string.page_flipping_sensivity_1,
			R.string.page_flipping_sensivity_2,
			R.string.page_flipping_sensivity_3,
			R.string.page_flipping_sensivity_4,
			R.string.page_flipping_sensivity_5
	};

	int[] mFlippingSensivityAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mAutoflipType = new int[] {
			1, 2
	};

	int[] mAutoflipTypeTitles = new int[] {
			R.string.autopage_flipping_type_1,
			R.string.autopage_flipping_type_2
	};

	int[] mAutoflipTypeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	final BaseActivity mActivity;
	final Context mContext;

	public PageFlipOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_PAGE_FLIP_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("PageFlipOption", mActivity, label, false, false);

		// replaced by new implementation
		//		mOptionsControls.add(new ListOption(this, mActivity.getString(R.string.option_controls_gesture_page_flipping_enabled),
		//				Settings.PROP_APP_GESTURE_PAGE_FLIPPING, mActivity.getString(R.string.option_controls_gesture_page_flipping_enabled_add_info), filter).add(
		//				mPagesPerFullSwipe, mPagesPerFullSwipeTitles, mPagesPerFullSwipeAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_gesture, R.drawable.icons8_gesture));
		OptionsListView listView = new OptionsListView(mContext, this);

		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
			listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_controls_enable_volume_keys), Settings.PROP_CONTROLS_ENABLE_VOLUME_KEYS,
					mActivity.getString(R.string.options_controls_enable_volume_keys_add_info), this.lastFilteredValue, false).setDefaultValue("1").
					setIconIdByAttr(R.attr.attr_icons8_speaker_buttons,R.drawable.icons8_speaker_buttons));

		OptionBase loPC = new ListOption(mOwner, mActivity.getString(R.string.page_flipping_page_count),
				Settings.PROP_APP_GESTURE_PAGE_FLIPPING_PAGE_COUNT, mActivity.getString(R.string.page_flipping_page_count_add_info), this.lastFilteredValue).add(
				new int[] {
						1, 2, 3, 4, 5, 6, 7, 8, 9 ,10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20
				}
		).setDefaultValue("5").setIconIdByAttr(R.attr.attr_icons8_document_4pages, R.drawable.icons8_document_4pages);

		listView.add(new ListOption(mOwner, mActivity.getString(R.string.page_flipping_type),
				Settings.PROP_APP_GESTURE_PAGE_FLIPPING_NEW, mActivity.getString(R.string.page_flipping_type_add_info), this.lastFilteredValue).add(
				mFlippingType, mFlippingTypeTitles, mFlippingTypeAddInfos).setDefaultValue("1").
				setIconIdByAttr(R.attr.attr_icons8_gesture, R.drawable.icons8_gesture).
				setOnChangeHandler(() -> {
					int value1 = mProperties.getInt(Settings.PROP_APP_GESTURE_PAGE_FLIPPING_NEW, 0);
					loPC.setEnabled(value1 > 1);
				}));
		int value1 = mProperties.getInt(Settings.PROP_APP_GESTURE_PAGE_FLIPPING_NEW, 0);
		loPC.setEnabled(value1 > 1);
		listView.add(loPC);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.page_flipping_sensivity),
				Settings.PROP_APP_GESTURE_PAGE_FLIPPING_SENSIVITY, mActivity.getString(R.string.page_flipping_sensivity_add_info), this.lastFilteredValue).add(
				mFlippingSensivity, mFlippingSensivityTitles, mFlippingSensivityAddInfos).setDefaultValue("3").
				setIconIdByAttr(R.attr.attr_icons8_gesture_sensivity, R.drawable.icons8_gesture_sensivity));
		String autoType = "1";
		String autoShow = "1";
		if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			autoType = "2";
			autoShow = "0";
		}
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.autopage_flipping_type),
				Settings.PROP_APP_VIEW_AUTOSCROLL_TYPE, mActivity.getString(R.string.autopage_flipping_type_add_info), this.lastFilteredValue).add(
						mAutoflipType, mAutoflipTypeTitles, mAutoflipTypeAddInfos).setDefaultValue(autoType).
				setIconIdByAttr(R.attr.attr_icons8_autoflip_page, R.drawable.icons8_autoflip_page));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.autopage_flipping_show_speed), Settings.PROP_APP_VIEW_AUTOSCROLL_SHOW_SPEED,
				mActivity.getString(R.string.autopage_flipping_show_speed_add_info), this.lastFilteredValue, false).setDefaultValue("1").
				setIconIdByAttr(R.attr.attr_icons8_autoflip_page_show_speed,R.drawable.icons8_autoflip_page_show_speed));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.autopage_flipping_show_progress), Settings.PROP_APP_VIEW_AUTOSCROLL_SHOW_PROGRESS,
				mActivity.getString(R.string.autopage_flipping_show_progress_add_info), this.lastFilteredValue, false).setDefaultValue(autoShow).
				setIconIdByAttr(R.attr.attr_icons8_autoflip_page_show_speed,R.drawable.icons8_autoflip_page_show_speed));
		listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_flip_simple_speed), Settings.PROP_APP_VIEW_AUTOSCROLL_SIMPLE_SPEED,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(1,180).setDefaultValue("8").
				setIconIdByAttr(R.attr.attr_icons8_autoflip_page_show_speed,R.drawable.icons8_autoflip_page_show_speed));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_controls_enable_volume_keys), Settings.PROP_CONTROLS_ENABLE_VOLUME_KEYS,
				mActivity.getString(R.string.options_controls_enable_volume_keys_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.page_flipping_page_count), Settings.PROP_APP_GESTURE_PAGE_FLIPPING_PAGE_COUNT,
				mActivity.getString(R.string.page_flipping_page_count_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.page_flipping_type), Settings.PROP_APP_GESTURE_PAGE_FLIPPING_NEW,
				mActivity.getString(R.string.page_flipping_type_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.page_flipping_sensivity), Settings.PROP_APP_GESTURE_PAGE_FLIPPING_SENSIVITY,
				mActivity.getString(R.string.page_flipping_sensivity_add_info));
		for (int i: mFlippingTypeTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mFlippingTypeAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mFlippingSensivityTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mFlippingSensivityAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.autopage_flipping_type),
				Settings.PROP_APP_VIEW_AUTOSCROLL_TYPE, mActivity.getString(R.string.autopage_flipping_type_add_info));
		for (int i: mAutoflipTypeAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mFlippingSensivityAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.autopage_flipping_show_speed), Settings.PROP_APP_VIEW_AUTOSCROLL_SHOW_SPEED,
				mActivity.getString(R.string.autopage_flipping_show_speed_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.autopage_flipping_show_progress), Settings.PROP_APP_VIEW_AUTOSCROLL_SHOW_PROGRESS,
				mActivity.getString(R.string.autopage_flipping_show_progress_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
