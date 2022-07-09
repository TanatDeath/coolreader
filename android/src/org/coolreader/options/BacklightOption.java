package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class BacklightOption extends SubmenuOption {

	int[] mBacklightTimeout = new int[] {
			0, 2, 3, 4, 5, 6
	};
	int[] mBacklightTimeoutTitles = new int[] {
			R.string.options_app_backlight_timeout_0, R.string.options_app_backlight_timeout_2, R.string.options_app_backlight_timeout_3, R.string.options_app_backlight_timeout_4, R.string.options_app_backlight_timeout_5, R.string.options_app_backlight_timeout_6
	};

	public static final int[] mBacklightLevels = new int[] {
			-1,  1,  2,  3,  4,  5,  6,  7,  8,  9,
			10, 12, 15, 20, 25, 30, 35, 40, 45, 50,
			55, 60, 65, 70, 75, 80, 85, 90, 95, 100
	};
	public static final String[] mBacklightLevelsTitles = new String[] {
			"Default", "1%", "2%", "3%", "4%", "5%", "6%", "7%", "8%", "9%",
			"10%", "12%", "15%", "20%", "25%", "30%", "35%", "40%", "45%", "50%",
			"55%", "60%", "65%", "70%", "75%", "80%", "85%", "90%", "95%", "100%",
	};
	public static final int[] mBacklightLevelsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
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

	int[] mFlickBrightness = new int[] {
			0, 1, 2, 3
	};
	int[] mFlickBrightnessTitles = new int[] {
			R.string.options_controls_flick_brightness_none, R.string.options_controls_flick_brightness_left, R.string.options_controls_flick_brightness_right,
			R.string.options_controls_flick_brightness_both
	};

	int[] mFlickBrightnessAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mFlickBrightness2 = new int[] {
			0, 4, 5, 6, 7, 8, 9, 10
	};
	int[] mFlickBrightness2Titles = new int[] {
			R.string.options_controls_flick_brightness_none,
			R.string.options_controls_flick_brightness_left_cold,
			R.string.options_controls_flick_brightness_left_warm,
			R.string.options_controls_flick_brightness_left_both_warm,
			R.string.options_controls_flick_brightness_right_both_warm,
			R.string.options_controls_flick_brightness_left_both_cold,
			R.string.options_controls_flick_brightness_right_both_cold,
			R.string.options_controls_flick_brightness_both_both
	};

	int[] mFlickBrightness2AddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mSwipeSensivity = new int[] {
			1, 2, 0, 3, 4
	};
	int[] mSwipeSensivityTitles = new int[] {
			R.string.brightness_swipe_sensivity_1, R.string.brightness_swipe_sensivity_2,
			R.string.brightness_swipe_sensivity_0,
			R.string.brightness_swipe_sensivity_3, R.string.brightness_swipe_sensivity_4
	};
	int[] mSwipeSensivityAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	final BaseActivity mActivity;
	final Context mContext;

	private Integer roundBackLight(float bl) {
		if (bl < 20) return (int) bl;
		return (int) (5*(Math.ceil(Math.abs(bl/5))));
	}

	public BacklightOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_BACKLIGHT_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("BacklightOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		// common screen or not onyx
		if (
				(!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) ||
						((DeviceInfo.SCREEN_CAN_CONTROL_BRIGHTNESS) && (!DeviceInfo.ONYX_HAVE_FRONTLIGHT) && (!DeviceInfo.ONYX_HAVE_NATURAL_BACKLIGHT))
		) {
			listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_app_backlight_timeout), Settings.PROP_APP_SCREEN_BACKLIGHT_LOCK,
					mActivity.getString(R.string.options_app_backlight_timeout_add_info), this.lastFilteredValue).
					add(mBacklightTimeout, mBacklightTimeoutTitles, mBacklightLevelsAddInfos).setDefaultValue("3").setIconIdByAttr(R.attr.attr_icons8_sun_1, R.drawable.icons8_sun_1));
			mBacklightLevelsTitles[0] = mActivity.getString(R.string.options_app_backlight_screen_default);
			listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_app_backlight_screen), Settings.PROP_APP_SCREEN_BACKLIGHT,
					mActivity.getString(R.string.options_app_backlight_screen_add_info), this.lastFilteredValue).add(mBacklightLevels, mBacklightLevelsTitles, mBacklightLevelsAddInfos).
					setDefaultValue("-1").
					setIconIdByAttr(R.attr.attr_icons8_sun, R.drawable.icons8_sun));
		}
		// screen with touch - later think better, possible "else" branch is not correct
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) || DeviceInfo.EINK_HAVE_FRONTLIGHT || DeviceInfo.SCREEN_CAN_CONTROL_BRIGHTNESS) {
			if (!DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT) {
				OptionBase mBacklightControl = new ListOption(mOwner, mActivity.getString(R.string.options_controls_flick_brightness), Settings.PROP_APP_FLICK_BACKLIGHT_CONTROL,
						mActivity.getString(R.string.options_controls_flick_brightness_add_info), this.lastFilteredValue).
						add(mFlickBrightness, mFlickBrightnessTitles, mFlickBrightnessAddInfos).setDefaultValue("1").
						setIconIdByAttr(R.attr.attr_icons8_sunrise, R.drawable.icons8_sunrise);
				listView.add(mBacklightControl);
			} else {
				OptionBase mBacklightControl = new ListOption(mOwner, mActivity.getString(R.string.options_controls_flick_brightness), Settings.PROP_APP_FLICK_BACKLIGHT_CONTROL,
						mActivity.getString(R.string.options_controls_flick_brightness_add_info), this.lastFilteredValue).
						add(mFlickBrightness2, mFlickBrightness2Titles, mFlickBrightness2AddInfos).setDefaultValue("4").
						setIconIdByAttr(R.attr.attr_icons8_sunrise, R.drawable.icons8_sunrise);
				listView.add(mBacklightControl);
			}
		}
		// eink screen with api
		if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			if ( DeviceInfo.EINK_HAVE_FRONTLIGHT ) {
				// read onyx current brightness
				int initialBacklight = mEinkScreen.getFrontLightValue(mActivity);
				int initialWarmBacklight = mEinkScreen.getWarmLightValue(mActivity);
				if (initialBacklight != -1)
					mProperties.setInt(Settings.PROP_APP_SCREEN_BACKLIGHT, Utils.findNearestValue(mEinkScreen.getFrontLightLevels(mActivity), initialBacklight));
				if (initialWarmBacklight != -1)
					mProperties.setInt(Settings.PROP_APP_SCREEN_WARM_BACKLIGHT, Utils.findNearestValue(mEinkScreen.getWarmLightLevels(mActivity), initialWarmBacklight));
				//}
				List<Integer> frontLightLevels = mEinkScreen.getFrontLightLevels(mActivity);
				if (null != frontLightLevels && frontLightLevels.size() > 0) {
					ArrayList<String> levelsTitles = new ArrayList<>();
					ArrayList<Integer> levels = new ArrayList<>();
					ArrayList<Integer> addInfos = new ArrayList<>();
					levels.add(-1);
					addInfos.add(R.string.option_add_info_empty_text);
					levelsTitles.add(mActivity.getString(R.string.options_app_backlight_screen_default));
					for (Integer level : frontLightLevels) {
						float percentLevel = 100 * level / (float) DeviceInfo.MAX_SCREEN_BRIGHTNESS_VALUE;
						if (levelsTitles.contains(roundBackLight(percentLevel) + "%"))
							levelsTitles.add((roundBackLight(percentLevel) + 1) + "%");
						else
							levelsTitles.add(roundBackLight(percentLevel) + "%");
						levels.add(level);
						addInfos.add(R.string.option_add_info_empty_text);
					}
					listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_app_backlight_screen), Settings.PROP_APP_SCREEN_BACKLIGHT,
							mActivity.getString(R.string.options_app_backlight_screen_add_info), this.lastFilteredValue).add(levels, levelsTitles, addInfos).
							setDefaultValue("-1").
							setIconIdByAttr(R.attr.attr_icons8_sun, R.drawable.icons8_sun));
				}
				if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT) {
					List<Integer> warmLightLevels = mEinkScreen.getWarmLightLevels(mActivity);
					if (null != warmLightLevels && warmLightLevels.size() > 0) {
						ArrayList<String> levelsTitles = new ArrayList<>();
						ArrayList<Integer> levels = new ArrayList<>();
						ArrayList<Integer> addInfos = new ArrayList<>();
						levels.add(-1);
						levelsTitles.add(mActivity.getString(R.string.options_app_backlight_screen_default));
						addInfos.add(R.string.option_add_info_empty_text);
						for (Integer level : warmLightLevels) {
							float percentLevel = 100 * level / (float) DeviceInfo.MAX_SCREEN_BRIGHTNESS_WARM_VALUE;
							if (levelsTitles.contains(roundBackLight(percentLevel) + "%"))
								levelsTitles.add((roundBackLight(percentLevel) + 1) + "%");
							else
								levelsTitles.add(roundBackLight(percentLevel) + "%");
							levels.add(level);
							addInfos.add(R.string.option_add_info_empty_text);
						}
						listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_app_warm_backlight_screen), Settings.PROP_APP_SCREEN_WARM_BACKLIGHT,
								mActivity.getString(R.string.options_app_backlight_screen_add_info), this.lastFilteredValue).add(levels, levelsTitles, addInfos).
								setDefaultValue("-1").
								setIconIdByAttr(R.attr.attr_icons8_sun, R.drawable.icons8_sun));
					}
					listView.add(OptionsDialog.getOption(Settings.PROP_APP_SCREEN_BACKLIGHT_FIX_DELTA, this.lastFilteredValue));
				}
			}
		}
		listView.add(OptionsDialog.getOption(Settings.PROP_APP_KEY_BACKLIGHT_OFF, this.lastFilteredValue));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.brightness_swipe_sensivity), Settings.PROP_APP_BACKLIGHT_SWIPE_SENSIVITY,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSwipeSensivity, mSwipeSensivityTitles, mSwipeSensivityAddInfos).setDefaultValue("2").
				setIconIdByAttr(R.attr.attr_icons8_backlight_swipe_sensitivity, R.drawable.icons8_backlight_swipe_sensitivity));

		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_app_backlight_timeout), Settings.PROP_APP_SCREEN_BACKLIGHT_LOCK,
				mActivity.getString(R.string.options_app_backlight_timeout_add_info));
		for (int i: mBacklightTimeoutTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mBacklightLevelsAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_backlight_screen), Settings.PROP_APP_SCREEN_BACKLIGHT,
				mActivity.getString(R.string.options_app_backlight_screen_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_warm_backlight_screen), Settings.PROP_APP_SCREEN_WARM_BACKLIGHT,
				mActivity.getString(R.string.options_app_backlight_screen_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.use_eink_backlight_control), Settings.PROP_APP_USE_EINK_FRONTLIGHT,
				mActivity.getString(R.string.use_eink_backlight_control_add_info));
		for (String s: OptionsDialog.mMotionTimeoutsTitles) this.updateFilteredMark(s);
		this.updateFilteredMark(mActivity.getString(R.string.options_controls_flick_brightness), Settings.PROP_APP_FLICK_BACKLIGHT_CONTROL,
				mActivity.getString(R.string.options_controls_flick_brightness_add_info));
		//this.updateFilteredMark(mActivity.getString(R.string.options_controls_flick_warm), Settings.PROP_APP_FLICK_WARMLIGHT_CONTROL,
		//		mActivity.getString(R.string.options_controls_flick_brightness_add_info));
		for (int i: mFlickBrightnessTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mFlickBrightnessAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_key_backlight_off), Settings.PROP_APP_KEY_BACKLIGHT_OFF,
				mActivity.getString(R.string.options_app_key_backlight_off_add_info));
		//this.updateFilteredMark(mActivity.getString(R.string.options_app_get_backlight_from_system), Settings.PROP_APP_SCREEN_GET_BACKLIGHT_FROM_SYSTEM,
		//		mActivity.getString(R.string.options_app_get_backlight_from_system_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
