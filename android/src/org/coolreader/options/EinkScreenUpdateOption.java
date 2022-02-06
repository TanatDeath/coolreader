package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.eink.EinkScreen;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class EinkScreenUpdateOption extends SubmenuOption {

	int[] mScreenFullUpdateInterval = new int[] {
			1, 2, 3, 4, 5, 7, 10, 15, 20, 30, 40, 50
	};
	int[] mScreenBlackPageInterval = new int[] {
			0, 1, 2, 3, 4, 5, 7, 10, 15, 20
	};
	int[] mScreenBlackPageDuration = new int[] {
			0, 100, 200, 300, 500, 700, 1000, 2000, 3000, 4000, 5000
	};
	int[] mScreenUpdateModes = new int[] {
			EinkScreen.EinkUpdateMode.Normal.code, EinkScreen.EinkUpdateMode.FastQuality.code, EinkScreen.EinkUpdateMode.Active.code
	};
	int[] mScreenUpdateModesTitles = new int[] {
			R.string.options_screen_update_mode_normal, R.string.options_screen_update_mode_fast_quality, R.string.options_screen_update_mode_fast
	};

	int[] mOnyxScreenUpdateModes = new int[] {
			EinkScreen.EinkUpdateMode.Normal.code, EinkScreen.EinkUpdateMode.FastQuality.code, EinkScreen.EinkUpdateMode.FastA2.code, EinkScreen.EinkUpdateMode.FastX.code
	};
	int[] mOnyxScreenUpdateModesTitles = new int[] {
			R.string.options_screen_update_mode_normal, R.string.options_screen_update_mode_fast_quality, R.string.options_screen_update_mode_fast, R.string.options_screen_update_mode_fast_x
	};
	int[] mScreenUpdateModesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mEinkOnyxNeedBypass = new int[] {
			0, 1, 2
	};

	int[] mEinkOnyxNeedBypassTitles = new int[] {
			R.string.eink_onyx_bypass0,
			R.string.eink_onyx_bypass1,
			R.string.eink_onyx_bypass2
	};

	int[] mEinkOnyxNeedBypassAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mEinkFullScreenUpdateMode = new int[] {
			0, 1, 2, 3
	};

	int[] mEinkFullScreenUpdateModeTitles = new int[] {
			R.string.eink_full_update_auto,
			R.string.eink_full_update_method1,
			R.string.eink_full_update_method2,
			R.string.eink_full_update_method3
	};

	int[] mEinkFullScreenUpdateModeAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mEinkOnyxExtraDelayFullRefresh = new int[] {
			-1, 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 230, 260, 300, 400, 500
	};

	int[] mEinkOnyxExtraDelayFullRefreshTitles = new int[] {
			R.string.eink_onyx_add_delay_full_refresh_auto,
			R.string.eink_onyx_add_delay_full_refresh_0,
			R.string.eink_onyx_add_delay_full_refresh_10,
			R.string.eink_onyx_add_delay_full_refresh_20,
			R.string.eink_onyx_add_delay_full_refresh_30,
			R.string.eink_onyx_add_delay_full_refresh_40,
			R.string.eink_onyx_add_delay_full_refresh_50,
			R.string.eink_onyx_add_delay_full_refresh_60,
			R.string.eink_onyx_add_delay_full_refresh_70,
			R.string.eink_onyx_add_delay_full_refresh_80,
			R.string.eink_onyx_add_delay_full_refresh_90,
			R.string.eink_onyx_add_delay_full_refresh_100,
			R.string.eink_onyx_add_delay_full_refresh_110,
			R.string.eink_onyx_add_delay_full_refresh_120,
			R.string.eink_onyx_add_delay_full_refresh_130,
			R.string.eink_onyx_add_delay_full_refresh_140,
			R.string.eink_onyx_add_delay_full_refresh_150,
			R.string.eink_onyx_add_delay_full_refresh_160,
			R.string.eink_onyx_add_delay_full_refresh_170,
			R.string.eink_onyx_add_delay_full_refresh_180,
			R.string.eink_onyx_add_delay_full_refresh_190,
			R.string.eink_onyx_add_delay_full_refresh_200,
			R.string.eink_onyx_add_delay_full_refresh_230,
			R.string.eink_onyx_add_delay_full_refresh_260,
			R.string.eink_onyx_add_delay_full_refresh_300,
			R.string.eink_onyx_add_delay_full_refresh_400,
			R.string.eink_onyx_add_delay_full_refresh_500
	};

	int[] mEinkOnyxExtraDelayFullRefreshAddInfos = new int[] {
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

	public EinkScreenUpdateOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_EINKSCREENUPDATE_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("EinkScreenUpdateOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		//if (DeviceInfo.EINK_SCREEN_UPDATE_MODES_SUPPORTED) {
		//if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
		if (DeviceInfo.EINK_SCREEN_UPDATE_MODES_SUPPORTED) {
			ListOption optionMode;
			OptionBase optionInterval = new ListOption(mOwner, mActivity.getString(R.string.options_screen_update_interval), Settings.PROP_APP_SCREEN_UPDATE_INTERVAL,
					mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add("0",
					mActivity.getString(R.string.options_screen_update_interval_none), mActivity.getString(R.string.option_add_info_empty_text)).
					add(mScreenFullUpdateInterval).setDefaultValue("10").
					setDisabledNote(mActivity.getString(R.string.options_eink_app_optimization_enabled)).
					setIconIdByAttr(R.attr.attr_icons8_blackpage_interval, R.drawable.icons8_blackpage_interval);
			boolean intervalAdded = false;
			if ( DeviceInfo.EINK_ONYX ) {
				OptionBase optionRegal =
						new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_screen_update_mode_onyx_regal), Settings.PROP_APP_EINK_ONYX_REGAL,
								mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
								setDefaultValue("1").
								setIconIdByAttr(R.attr.attr_icons8_eink_snow, R.drawable.icons8_eink_snow);
				listView.add(optionRegal);
				optionMode = new ListOption(mOwner, mActivity.getString(R.string.options_screen_update_mode), Settings.PROP_APP_SCREEN_UPDATE_MODE,
						mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
				if (DeviceInfo.EINK_SCREEN_REGAL)
					optionMode = optionMode.add(mOnyxScreenUpdateModes[0], mOnyxScreenUpdateModesTitles[0], R.string.option_add_info_empty_text);
				optionMode = optionMode.add(mOnyxScreenUpdateModes[1], mOnyxScreenUpdateModesTitles[1], R.string.option_add_info_empty_text);
				optionMode = optionMode.add(mOnyxScreenUpdateModes[2], mOnyxScreenUpdateModesTitles[2], R.string.option_add_info_empty_text);
				optionMode = optionMode.add(mOnyxScreenUpdateModes[3], mOnyxScreenUpdateModesTitles[3], R.string.option_add_info_empty_text);
				listView.add(optionMode.setDefaultValue(String.valueOf(DeviceInfo.EINK_SCREEN_REGAL ? EinkScreen.EinkUpdateMode.Regal.code : EinkScreen.EinkUpdateMode.Normal.code))
						.setDisabledNote(mActivity.getString(R.string.options_eink_app_optimization_enabled))
						.setIconIdByAttr(R.attr.attr_icons8_eink_snow, R.drawable.icons8_eink_snow));
				intervalAdded = true;
				listView.add(optionInterval);
				OptionBase optionNeedBypass =
						new ListOption(mOwner, mActivity.getString(R.string.eink_onyx_bypass), Settings.PROP_APP_EINK_ONYX_NEED_BYPASS,
								mActivity.getString(R.string.eink_onyx_bypass_add_info), this.lastFilteredValue).
								add(mEinkOnyxNeedBypass, mEinkOnyxNeedBypassTitles, mEinkOnyxNeedBypassAddInfos).
								setDefaultValue("0").
								setIconIdByAttr(R.attr.attr_icons8_eink_sett, R.drawable.icons8_eink_sett);
				listView.add(optionNeedBypass);

				OptionBase optionNeedDeepGC =
						new BoolOption(mActivity, mOwner, mActivity.getString(R.string.eink_onyx_deepgc), Settings.PROP_APP_EINK_ONYX_NEED_DEEPGC,
								mActivity.getString(R.string.eink_onyx_deepgc_add_info), this.lastFilteredValue).
								setDefaultValue("0").
								setIconIdByAttr(R.attr.attr_icons8_eink_sett, R.drawable.icons8_eink_sett);
				listView.add(optionNeedDeepGC);


				OptionBase optionFSUMethod =
						new ListOption(mOwner, mActivity.getString(R.string.eink_onyx_full_update), Settings.PROP_APP_EINK_ONYX_FULL_SCREEN_UPDATE_METHOD,
								mActivity.getString(R.string.eink_onyx_full_update_add_info), this.lastFilteredValue).
								add(mEinkFullScreenUpdateMode, mEinkFullScreenUpdateModeTitles, mEinkFullScreenUpdateModeAddInfos).
								setDefaultValue("0").
								setIconIdByAttr(R.attr.attr_icons8_eink_sett, R.drawable.icons8_eink_sett);
				listView.add(optionFSUMethod);

				OptionBase optionExtraDelayFullRefresh =
						new FlowListOption(mOwner, mActivity.getString(R.string.eink_onyx_add_delay_full_refresh), Settings.PROP_APP_EINK_ONYX_EXTRA_DELAY_FULL_REFRESH,
								mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
								add(mEinkOnyxExtraDelayFullRefresh, mEinkOnyxExtraDelayFullRefreshTitles, mEinkOnyxExtraDelayFullRefreshAddInfos).
								setDefaultValue("-1").
								setIconIdByAttr(R.attr.attr_icons8_blackpage_duration, R.drawable.icons8_blackpage_duration);
				listView.add(optionExtraDelayFullRefresh);
			} else {
				optionMode = new ListOption(mOwner, mActivity.getString(R.string.options_screen_update_mode), Settings.PROP_APP_SCREEN_UPDATE_MODE,
						mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
				optionMode.add(mScreenUpdateModes, mScreenUpdateModesTitles, mScreenUpdateModesAddInfos).setDefaultValue("0")
						.setDisabledNote(mActivity.getString(R.string.options_eink_app_optimization_enabled))
						.setIconIdByAttr(R.attr.attr_icons8_eink_snow, R.drawable.icons8_eink_snow);
				listView.add(optionMode);
			}
			if (!intervalAdded) listView.add(optionInterval);
			optionMode.setEnabled(!mActivity.getEinkScreen().isAppOptimizationEnabled());
			optionInterval.setEnabled(!mActivity.getEinkScreen().isAppOptimizationEnabled());
		}
		if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			listView.addExt(new ListOption(mOwner, mActivity.getString(R.string.options_screen_blackpage_interval), Settings.PROP_APP_SCREEN_BLACKPAGE_INTERVAL,
					mActivity.getString(R.string.options_screen_blackpage_interval_add_info), this.lastFilteredValue).
					add(mScreenBlackPageInterval).setIconIdByAttr(R.attr.attr_icons8_blackpage_interval, R.drawable.icons8_blackpage_interval).
					setDefaultValue("0"),"eink");
			listView.addExt(new ListOption(mOwner, mActivity.getString(R.string.options_screen_blackpage_duration), Settings.PROP_APP_SCREEN_BLACKPAGE_DURATION,
					mActivity.getString(R.string.options_screen_blackpage_duration_add_info), this.lastFilteredValue).
					add(mScreenBlackPageDuration).
					setIconIdByAttr(R.attr.attr_icons8_blackpage_duration, R.drawable.icons8_blackpage_duration).
					setDefaultValue("300"),"eink");
		}
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_update_mode), Settings.PROP_APP_SCREEN_UPDATE_MODE,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mScreenUpdateModesTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mScreenUpdateModesAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_update_interval), Settings.PROP_APP_SCREEN_UPDATE_INTERVAL,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_blackpage_interval), Settings.PROP_APP_SCREEN_BLACKPAGE_INTERVAL,
				mActivity.getString(R.string.options_screen_blackpage_interval_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_screen_blackpage_duration), Settings.PROP_APP_SCREEN_BLACKPAGE_DURATION,
				mActivity.getString(R.string.options_screen_blackpage_duration_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.eink_onyx_bypass), Settings.PROP_APP_EINK_ONYX_NEED_BYPASS,
				mActivity.getString(R.string.eink_onyx_bypass_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.eink_onyx_deepgc), Settings.PROP_APP_EINK_ONYX_NEED_DEEPGC,
				mActivity.getString(R.string.eink_onyx_deepgc_add_info));
		for (int i: mEinkOnyxNeedBypassTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.eink_onyx_add_delay_full_refresh), Settings.PROP_APP_EINK_ONYX_EXTRA_DELAY_FULL_REFRESH,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mEinkOnyxExtraDelayFullRefreshTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.eink_onyx_full_update), Settings.PROP_APP_EINK_ONYX_FULL_SCREEN_UPDATE_METHOD,
				mActivity.getString(R.string.eink_onyx_full_update_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
