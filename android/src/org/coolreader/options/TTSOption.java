package org.coolreader.options;

import android.annotation.TargetApi;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.tts.OnTTSCreatedListener;

import java.util.Locale;

public class TTSOption extends SubmenuOption {

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static void fillTTSLanguages(OptionsDialog od, ListOption listOption) {
		listOption.clear();
		if (null != od.mTTSBinder) {
			od.mTTSBinder.retrieveAvailableLocales(list -> {
				BackgroundThread.instance().executeGUI(() -> {
					for (Locale locale : list) {
						String language = locale.getDisplayLanguage();
						String country = locale.getDisplayCountry();
						if (country.length() > 0)
							language += " (" + country + ")";
						listOption.add(locale.toString(), language, "");
					}
					listOption.noIcon();
					listOption.refreshList();
				});
			});
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static void fillTTSVoices(OptionsDialog od, ListOption listOption, String language) {
		listOption.clear();
		if (null != od.mTTSBinder) {
			od.mTTSBinder.retrieveAvailableVoices(new Locale(language), list -> {
				BackgroundThread.instance().executeGUI(() -> {
					for (Voice voice : list) {
						String quality;
						int qualityInt = voice.getQuality();
						if (qualityInt >= Voice.QUALITY_VERY_HIGH)
							quality = od.activity.getString(R.string.options_tts_voice_quality_very_high);
						else if (qualityInt >= Voice.QUALITY_HIGH)
							quality = od.activity.getString(R.string.options_tts_voice_quality_high);
						else if (qualityInt >= Voice.QUALITY_NORMAL)
							quality = od.activity.getString(R.string.options_tts_voice_quality_normal);
						else if (qualityInt >= Voice.QUALITY_LOW)
							quality = od.activity.getString(R.string.options_tts_voice_quality_low);
						else
							quality = od.activity.getString(R.string.options_tts_voice_quality_very_low);
						listOption.add(voice.getName(), voice.getName(), quality);
					}
					listOption.noIcon();
					listOption.refreshList();
				});
			});
		}
	}

	public TTSOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_TTS_TITLE, addInfo, filter);
		mActivity = activity;
		mOptionsDialog = od;
	}

	public void whenSelect() {
		BaseDialog dlg = new BaseDialog("TTSOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.save_pos_timeout_speak),
				Settings.PROP_SAVE_POS_SPEAK_TIMEOUT, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mOptionsDialog.mMotionTimeoutsSec, mOptionsDialog.mMotionTimeoutsTitlesSec, mOptionsDialog.mMotionTimeoutsAddInfosSec).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_position_to_disk_interval, R.drawable.icons8_position_to_disk_interval));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			mOptionsDialog.mTTSEngineOption = new ListOption(mOwner, mActivity.getString(R.string.options_tts_engine), Settings.PROP_APP_TTS_ENGINE,
					mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
			mOptionsDialog.mTTSBinder.retrieveAvailableEngines(list -> {
				BackgroundThread.instance().executeGUI(() -> {
					for (TextToSpeech.EngineInfo info : list) {
						mOptionsDialog.mTTSEngineOption.add(info.name, info.label, "");
					}
					String tts_package = mProperties.getProperty(Settings.PROP_APP_TTS_ENGINE, "");
					mOptionsDialog.mTTSEngineOption.setDefaultValue(tts_package);
					mOptionsDialog.mTTSEngineOption.refreshList();
				});
			});
			listView.add(mOptionsDialog.mTTSEngineOption.noIcon());
			mOptionsDialog.mTTSEngineOption.setOnChangeHandler(() -> {
				String tts_package = mProperties.getProperty(Settings.PROP_APP_TTS_ENGINE, "");
				mOptionsDialog.mTTSBinder.initTTS(tts_package, new OnTTSCreatedListener() {
					@Override
					public void onCreated() {
						if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
							BackgroundThread.instance().executeGUI(() -> {
								if (null != mOptionsDialog.mTTSLanguageOption)
									fillTTSLanguages(mOptionsDialog, mOptionsDialog.mTTSLanguageOption);
								if (null != mOptionsDialog.mTTSVoiceOption)
									mOptionsDialog.mTTSVoiceOption.clear();
							});
						}
					}
					@Override
					public void onFailed() {
						if (null != mOptionsDialog.mTTSLanguageOption)
							mOptionsDialog.mTTSLanguageOption.clear();
					}
					@Override
					public void onTimedOut() {
						if (null != mOptionsDialog.mTTSLanguageOption)
							mOptionsDialog.mTTSLanguageOption.clear();
					}
				});
			});
		}
		mOptionsDialog.mTTSUseDocLangOption = new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_tts_use_doc_lang), Settings.PROP_APP_TTS_USE_DOC_LANG,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_document_lang,R.drawable.icons8_document_lang);
		listView.add(mOptionsDialog.mTTSUseDocLangOption);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			boolean useDocLang = mProperties.getBool(Settings.PROP_APP_TTS_USE_DOC_LANG, true);
			mOptionsDialog.mTTSLanguageOption = new ListOption(mOwner, mActivity.getString(R.string.options_tts_language), Settings.PROP_APP_TTS_FORCE_LANGUAGE,
					mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
			fillTTSLanguages(mOptionsDialog, mOptionsDialog.mTTSLanguageOption);
			mOptionsDialog.mTTSLanguageOption.setEnabled(!useDocLang);
			listView.add(mOptionsDialog.mTTSLanguageOption);
			// onchange handler
			String lang = mProperties.getProperty (Settings.PROP_APP_TTS_FORCE_LANGUAGE, "");
			mOptionsDialog.mTTSUseDocLangOption.setOnChangeHandler(() -> {
				boolean value = mProperties.getBool(Settings.PROP_APP_TTS_USE_DOC_LANG, true);
				mOptionsDialog.mTTSLanguageOption.setEnabled(!value);
				mOptionsDialog.mTTSVoiceOption.setEnabled(!value);
			});
			mOptionsDialog.mTTSLanguageOption.setOnChangeHandler(() -> {
				String value = mProperties.getProperty(Settings.PROP_APP_TTS_FORCE_LANGUAGE, "");
				fillTTSVoices(mOptionsDialog, mOptionsDialog.mTTSVoiceOption, value);
			});

			mOptionsDialog.mTTSVoiceOption = new ListOption(mOwner, mActivity.getString(R.string.options_tts_voice),
					Settings.PROP_APP_TTS_VOICE, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
			fillTTSVoices(mOptionsDialog, mOptionsDialog.mTTSVoiceOption, lang);
			mOptionsDialog.mTTSVoiceOption.setEnabled(!useDocLang);
			listView.add(mOptionsDialog.mTTSVoiceOption);
		}
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.tts_panel_background), Settings.PROP_APP_OPTIONS_TTS_TOOLBAR_BACKGROUND,
				mActivity.getString(R.string.sel_panel_add_info), this.lastFilteredValue).
				add(ToolbarOption.mSelPanelBackground, ToolbarOption.mSelPanelBackgroundTitles, ToolbarOption.mSelPanelBackgroundAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_toolbar_background,R.drawable.icons8_toolbar_background));
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.tts_panel_transp_buttons), Settings.PROP_APP_OPTIONS_TTS_TOOLBAR_TRANSP_BUTTONS,
				mActivity.getString(R.string.sel_panel_add_info), this.lastFilteredValue).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_transp_buttons,R.drawable.icons8_transp_buttons));
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_tts_google_abbr_workaround),
				Settings.PROP_APP_TTS_GOOGLE_END_OF_SENTENCE_ABBR, mActivity.getString(R.string.options_tts_google_abbr_workaround_comment), lastFilteredValue)
				.setDefaultValue("1").noIcon());
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_app_tts_stop_motion_timeout), Settings.PROP_APP_MOTION_TIMEOUT,
				mActivity.getString(R.string.options_app_tts_stop_motion_timeout_add_info), this.lastFilteredValue).
				add(mOptionsDialog.mMotionTimeouts, mOptionsDialog.mMotionTimeoutsTitles, mOptionsDialog.mMotionTimeoutsAddInfos).
				setDefaultValue(Integer.toString(mOptionsDialog.mMotionTimeouts[0])).
				setIconIdByAttr(R.attr.attr_icons8_moving_sensor_n,R.drawable.icons8_moving_sensor));
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.tts_page_mode_dont_change), Settings.PROP_PAGE_VIEW_MODE_TTS_DONT_CHANGE,
				mActivity.getString(R.string.tts_page_mode_dont_change_add_info), this.lastFilteredValue).setDefaultValue("0").
				setIconIdByAttr(R.attr.cr3_option_view_mode_scroll_drawable, R.drawable.cr3_option_view_mode_scroll));
		// plotn - disabled, since removed from tts code
//			listView.add(new ListOption(mOwner, mActivity.getString(R.string.force_tts_koef),
//					Settings.PROP_APP_TTS_FORCE_KOEF, mActivity.getString(R.string.force_tts_koef_add_info), this.lastFilteredValue).
//					add(mForceTTS, mForceTTSTitles, mForceTTSAddInfos).
//					setDefaultValue("0").
//					setIconIdByAttr(R.attr.attr_icons8_speaker_koef, R.drawable.icons8_speaker_koef));
		dlg.setView(listView);
		dlg.show();
	}

	public void onSelect() {
		if (!enabled)
			return;
		String tts_package = mProperties.getProperty(Settings.PROP_APP_TTS_ENGINE, "");
		mOptionsDialog.mTTSBinder.initTTS(tts_package, new OnTTSCreatedListener() {
			@Override
			public void onCreated() {
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
					BackgroundThread.instance().executeGUI(() -> {
						if (null != mOptionsDialog.mTTSLanguageOption)
							fillTTSLanguages(mOptionsDialog, mOptionsDialog.mTTSLanguageOption);
						if (null != mOptionsDialog.mTTSVoiceOption)
							mOptionsDialog.mTTSVoiceOption.clear();
					});
				}
				whenSelect();
			}
			@Override
			public void onFailed() {
				if (null != mOptionsDialog.mTTSLanguageOption)
					mOptionsDialog.mTTSLanguageOption.clear();
				whenSelect();
			}
			@Override
			public void onTimedOut() {
				if (null != mOptionsDialog.mTTSLanguageOption)
					mOptionsDialog.mTTSLanguageOption.clear();
				whenSelect();
			}
		});
	}

	public boolean updateFilterEnd() {
		for (String s: mOptionsDialog.mMotionTimeoutsTitlesSec) this.updateFilteredMark(s);
		this.updateFilteredMark(mActivity.getString(R.string.save_pos_timeout_speak), Settings.PROP_SAVE_POS_SPEAK_TIMEOUT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.force_tts_koef), Settings.PROP_APP_TTS_FORCE_KOEF,
				mActivity.getString(R.string.force_tts_koef_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_tts_google_abbr_workaround), Settings.PROP_APP_TTS_GOOGLE_END_OF_SENTENCE_ABBR,
				mActivity.getString(R.string.options_tts_google_abbr_workaround_comment));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_tts_stop_motion_timeout), Settings.PROP_APP_MOTION_TIMEOUT,
				mActivity.getString(R.string.options_app_tts_stop_motion_timeout_add_info));
		for (String s: mOptionsDialog.mMotionTimeoutsTitles) this.updateFilteredMark(s);
		this.updateFilteredMark(mActivity.getString(R.string.force_tts_koef), Settings.PROP_APP_TTS_FORCE_KOEF, mActivity.getString(R.string.force_tts_koef_add_info));
		for (String s: mOptionsDialog.mForceTTSTitles) this.updateFilteredMark(s);
		for (int i: mOptionsDialog.mForceTTSAddInfos) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_tts_engine), Settings.PROP_APP_TTS_ENGINE,
				mActivity.getString(R.string.option_add_info_empty_text));
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) && (mOptionsDialog.mTTSEngineOption != null)) {
			for (OptionsDialog.Three t: mOptionsDialog.mTTSEngineOption.list) {
				this.updateFilteredMark(t.label);
				this.updateFilteredMark(t.value);
				this.updateFilteredMark(t.addInfo);
			}
		}
		this.updateFilteredMark(mActivity.getString(R.string.options_tts_use_doc_lang), Settings.PROP_APP_TTS_USE_DOC_LANG,
				mActivity.getString(R.string.option_add_info_empty_text));
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			this.updateFilteredMark(mActivity.getString(R.string.options_tts_language), Settings.PROP_APP_TTS_FORCE_LANGUAGE,
					mActivity.getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(Settings.PROP_APP_TTS_FORCE_LANGUAGE);
			this.updateFilteredMark(Settings.PROP_APP_TTS_USE_DOC_LANG);
			this.updateFilteredMark(Settings.PROP_APP_TTS_FORCE_LANGUAGE);
		}
		this.updateFilteredMark(mActivity.getString(R.string.tts_panel_background), Settings.PROP_APP_OPTIONS_TTS_TOOLBAR_BACKGROUND,
				mActivity.getString(R.string.sel_panel_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.tts_panel_transp_buttons), Settings.PROP_APP_OPTIONS_TTS_TOOLBAR_TRANSP_BUTTONS,
				mActivity.getString(R.string.sel_panel_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_background_1));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_background_2));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_background_3));
		this.updateFilteredMark(mActivity.getString(R.string.tts_page_mode_dont_change), Settings.PROP_PAGE_VIEW_MODE_TTS_DONT_CHANGE,
				mActivity.getString(R.string.tts_page_mode_dont_change_add_info));
		return this.lastFiltered;
	}
	public String getValueLabel() { return ">"; }
}
