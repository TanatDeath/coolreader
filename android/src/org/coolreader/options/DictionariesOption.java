package org.coolreader.options;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.AskSomeValuesDialog;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.dic.TranslationDirectionDialog;
import org.coolreader.dic.OfflineDicsDlg;

import java.io.File;
import java.util.ArrayList;

public class DictionariesOption extends SubmenuOption {

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
	final CoolReader mCoolReader;
	final OptionsDialog mOptionsDialog;

	public DictionariesOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_DICTIONARY_TITLE, addInfo, filter);
		mActivity = activity;
		mCoolReader = (CoolReader) mActivity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("DictionaryOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		if (mOptionsDialog.mReaderView != null)
			if (mOptionsDialog.mReaderView.getBookInfo() != null)
				if (mOptionsDialog.mReaderView.getBookInfo().getFileInfo() != null)
				{
					FileInfo fi = mOptionsDialog.mReaderView.getBookInfo().getFileInfo();
					String lfrom = "[empty]";
					if (!StrUtils.isEmptyStr(fi.lang_from)) lfrom = fi.lang_from;
					String lto = "[empty]";
					if (!StrUtils.isEmptyStr(fi.lang_to)) lto = fi.lang_to;
					mOptionsDialog.optBT = new ClickOption(mOwner, mActivity.getString(R.string.book_info_section_book_translation2),
							Settings.PROP_APP_TRANSLATE_DIR, mActivity.getString(R.string.book_info_section_book_translation2_add_info), this.lastFilteredValue,
							(view, optionLabel, optionValue) ->
							{
								String lang = StrUtils.getNonEmptyStr(fi.lang_to,true);
								String langf = StrUtils.getNonEmptyStr(fi.lang_from, true);
								FileInfo dfi = fi.parent;
								if (dfi == null) {
									dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
								}
								if (dfi == null) {
									File f = new File(fi.pathname);
									String par = f.getParent();
									if (par != null) {
										File df = new File(par);
										dfi = new FileInfo(df);
									}
								}
								if (dfi != null) {
									mCoolReader.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL, true,
											view, dfi, fi, langf, lang, "", null, TranslationDirectionDialog.FOR_COMMON_OPTIONS
											, v -> {
												FileInfo fi2 = mOptionsDialog.mReaderView.getBookInfo().getFileInfo();
												String lfrom2 = "[empty]";
												if (!StrUtils.isEmptyStr(fi2.lang_from)) lfrom2 = fi2.lang_from;
												String lto2 = "[empty]";
												if (!StrUtils.isEmptyStr(fi2.lang_to)) lto2 = fi2.lang_to;
												mOptionsDialog.optBT.setDefaultValue(lfrom2 + " -> " + lto2);
												mOptionsDialog.optBT.refreshItem();
											});
								} else {
									mActivity.showToast(mCoolReader.getString(R.string.file_not_found)+": "+fi.getFilename());
								}
							}, true);
					mOptionsDialog.optBT.setDefaultValue(lfrom + " -> " + lto).noIcon();
					listView.add(mOptionsDialog.optBT);
				}
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_selection_keep_selection_after_dictionary), Settings.PROP_APP_SELECTION_PERSIST,
				mActivity.getString(R.string.options_selection_keep_selection_after_dictionary_add_info), this.lastFilteredValue).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection_lock, R.drawable.icons8_document_selection_lock));
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.offline_dics_dialog),
				Settings.PROP_APP_OFFLINE_DICS, mActivity.getString(R.string.offline_dics_dialog_add_info), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					OfflineDicsDlg sdd = new OfflineDicsDlg(mCoolReader);
					sdd.show();
				}, false).
				setIconIdByAttr(0, R.drawable.icons8_offline_dics1));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary), Settings.PROP_APP_DICTIONARY,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate, R.drawable.icons8_google_translate));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary2), Settings.PROP_APP_DICTIONARY_2,
				mActivity.getString(R.string.options_app_dictionary2_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 3", Settings.PROP_APP_DICTIONARY_3,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 4", Settings.PROP_APP_DICTIONARY_4,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 5", Settings.PROP_APP_DICTIONARY_5,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 6", Settings.PROP_APP_DICTIONARY_6,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 7", Settings.PROP_APP_DICTIONARY_7,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 8", Settings.PROP_APP_DICTIONARY_8,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 9", Settings.PROP_APP_DICTIONARY_9,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new DicListOption(mOwner, mActivity.getString(R.string.options_app_dictionary)+" 10", Settings.PROP_APP_DICTIONARY_10,
				mActivity.getString(R.string.options_app_dictionary3_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.quick_translation_dirs),
				Settings.PROP_APP_QUICK_TRANSLATION_DIRS, mActivity.getString(R.string.quick_translation_dirs_info), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					ArrayList<String[]> vl = new ArrayList<>();
					String sVal = mProperties.getProperty(Settings.PROP_APP_QUICK_TRANSLATION_DIRS);
					String[] sVals = StrUtils.getNonEmptyStr(sVal, true).split(";");
					String[] sValsFull = {"", "", "", "", "", "", "", "", "", ""};
					int i = 0;
					for (String s: sVals) {
						if (i > 9) break;
						sValsFull[i] = s;
						i++;
					}
					i = 0;
					for (String s: sValsFull) {
						i++;
						String[] arrS1 = 	{mActivity.getString(R.string.lang_pair) + " " + i,
								mActivity.getString(R.string.lang_pair) + " " + i, s};
						vl.add(arrS1);
					}
					AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
							mCoolReader,
							mActivity.getString(R.string.quick_translation_dirs),
							mActivity.getString(R.string.quick_translation_dirs_info),
							vl, results -> {
						if (results != null) {
							String res = "";
							for (String s: results)
								if (!StrUtils.isEmptyStr(s))
									res = res + ";" + s;
							if (res.length()>0) res = res.substring(1);
							mProperties.setProperty(Settings.PROP_APP_QUICK_TRANSLATION_DIRS, res);
						}
					});
					dlgA.show();
				}, false).
				setIconIdByAttr(R.attr.attr_icons8_quick_transl_dir, R.drawable.icons8_quick_transl_dir));
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.online_offline_dics),
				Settings.PROP_APP_ONLINE_OFFLINE_DICS, mActivity.getString(R.string.online_offline_dics_add_info), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					ArrayList<String[]> vl = new ArrayList<>();
					String sVal = mProperties.getProperty(Settings.PROP_APP_ONLINE_OFFLINE_DICS);
					String[] sVals = StrUtils.getNonEmptyStr(sVal, true).split(";");
					String[] sValsFull = {"", "", "", "", "", "", "", "", "", ""};
					int i = 0;
					for (String s: sVals) {
						if (i > 9) break;
						sValsFull[i] = s;
						i++;
					}
					i = 0;
					for (String s: sValsFull) {
						i++;
						String[] arrS1 = {mActivity.getString(R.string.conformity) + " " + i,
								mActivity.getString(R.string.conformity) + " " + i, s};
						vl.add(arrS1);
					}
					AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
							mCoolReader,
							mActivity.getString(R.string.online_offline_dics),
							mActivity.getString(R.string.online_offline_dics_add_info_ext),
							vl, results -> {
						if (results != null) {
							String res = "";
							for (String s: results) res = res + ";" + s;
							for (int ii=0; ii<10; ii++) if (res.endsWith(";;")) res = res.substring(0,res.length()-1);
							if (res.endsWith(";")) res = res.substring(0,res.length()-1);
							if (res.length()>0) res = res.substring(1);
							mProperties.setProperty(Settings.PROP_APP_ONLINE_OFFLINE_DICS, res);
						}
					});
					dlgA.show();
				}, false).
				setIconIdByAttr(R.attr.attr_icons8_airplane_mode_on, R.drawable.icons8_airplane_mode_on));
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_app_dict_longtap_change),
				Settings.PROP_APP_DICT_LONGTAP_CHANGE, mActivity.getString(R.string.options_app_dict_longtap_change_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_single_double_tap, R.drawable.icons8_single_double_tap));
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
//			listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_font_size_user_dic), PROP_FONT_SIZE_USER_DIC,
//					mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(filterFontSizes(mFontSizes)).setDefaultValue("24").
//					setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size));
		listView.add(new WikiOption(mActivity, mOwner, mActivity.getString(R.string.options_app_wiki1), Settings.PROP_CLOUD_WIKI1_ADDR,
				mActivity.getString(R.string.options_app_wiki1_add_info), this.lastFilteredValue).setDefaultValue("https://en.wikipedia.org").
				setIconIdByAttr(R.attr.attr_icons8_wiki1, R.drawable.icons8_wiki1));
		listView.add(new WikiOption(mActivity, mOwner, mActivity.getString(R.string.options_app_wiki2), Settings.PROP_CLOUD_WIKI2_ADDR,
				mActivity.getString(R.string.options_app_wiki2_add_info), this.lastFilteredValue).setDefaultValue("https://ru.wikipedia.org").
				setIconIdByAttr(R.attr.attr_icons8_wiki2, R.drawable.icons8_wiki2));
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.ynd_translate_settings),
				Settings.PROP_CLOUD_YND_TRANSLATE_OPTIONS, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					ArrayList<String[]> vl = new ArrayList<>();
					mCoolReader.readYndCloudSettings();
					String[] arrS1 = {mActivity.getString(R.string.ynd_oauth), mActivity.getString(R.string.ynd_oauth),
							StrUtils.getNonEmptyStr(mCoolReader.yndCloudSettings.oauthToken, true)};
					vl.add(arrS1);
					String[] arrS2 = {mActivity.getString(R.string.ynd_folder_id), mActivity.getString(R.string.ynd_folder_id),
							StrUtils.getNonEmptyStr(mCoolReader.yndCloudSettings.folderId, true)};
					vl.add(arrS2);
					AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
							mCoolReader,
							mActivity.getString(R.string.ynd_cloud_settings),
							mActivity.getString(R.string.ynd_cloud_settings),
							vl, results -> {
						if (results != null) {
							if (results.size() >= 1)
								mCoolReader.yndCloudSettings.oauthToken = StrUtils.getNonEmptyStr(results.get(0), true);
							if (results.size() >= 2)
								mCoolReader.yndCloudSettings.folderId = StrUtils.getNonEmptyStr(results.get(1), true);
						}
						Gson gson = new GsonBuilder().setPrettyPrinting().create();
						final String prettyJson = gson.toJson(mCoolReader.yndCloudSettings);
						mCoolReader.saveYndCloudSettings(prettyJson);
					});
					dlgA.show();
				}, true).
				noIcon());
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.lingvo_settings),
				Settings.PROP_CLOUD_LINGVO_OPTIONS, mActivity.getString(R.string.lingvo_settings_add_info), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					ArrayList<String[]> vl = new ArrayList<>();
					mCoolReader.readLingvoCloudSettings();
					String[] arrS1 = {mActivity.getString(R.string.lingvo_token), mActivity.getString(R.string.lingvo_token),
							StrUtils.getNonEmptyStr(mCoolReader.lingvoCloudSettings.lingvoToken, true)};
					vl.add(arrS1);
					AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
							mCoolReader,
							mActivity.getString(R.string.lingvo_settings),
							mActivity.getString(R.string.lingvo_settings),
							vl, results -> {
						if (results != null) {
							if (results.size() >= 1)
								mCoolReader.lingvoCloudSettings.lingvoToken = StrUtils.getNonEmptyStr(results.get(0), true);
						}
						Gson gson = new GsonBuilder().setPrettyPrinting().create();
						final String prettyJson = gson.toJson(mCoolReader.lingvoCloudSettings);
						mCoolReader.saveLingvoCloudSettings(prettyJson);
					});
					dlgA.show();
				}, true).
				noIcon());
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.deepl_settings),
				Settings.PROP_CLOUD_DEEPL_OPTIONS, mActivity.getString(R.string.deepl_settings_add_info), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					ArrayList<String[]> vl = new ArrayList<>();
					mCoolReader.readLingvoCloudSettings();
					String[] arrS1 = {mActivity.getString(R.string.deepl_token), mActivity.getString(R.string.deepl_token),
							StrUtils.getNonEmptyStr(mCoolReader.deeplCloudSettings.deeplToken, true)};
					vl.add(arrS1);
					AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
							mCoolReader,
							mActivity.getString(R.string.deepl_settings),
							mActivity.getString(R.string.deepl_settings),
							vl, results -> {
						if (results != null) {
							if (results.size() >= 1)
								mCoolReader.deeplCloudSettings.deeplToken = StrUtils.getNonEmptyStr(results.get(0), true);
						}
						Gson gson = new GsonBuilder().setPrettyPrinting().create();
						final String prettyJson = gson.toJson(mCoolReader.deeplCloudSettings);
						mCoolReader.saveDeeplCloudSettings(prettyJson);
					});
					dlgA.show();
				}, true).
				noIcon());
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.wiki_save_history),
				Settings.PROP_CLOUD_WIKI_SAVE_HISTORY, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				noIcon());
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.dict_dont_save_if_more),
				Settings.PROP_APP_DICT_DONT_SAVE_IF_MORE, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mWordsDontSaveIfMore).setDefaultValue("0").
				noIcon());
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.inspector_mode_no_dic_history),
				Settings.PROP_INSPECTOR_MODE_NO_DIC_HISTORY, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				noIcon());
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_app_dict_word_correction),
				Settings.PROP_APP_DICT_WORD_CORRECTION, mActivity.getString(R.string.options_app_dict_word_correction_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_l_h,R.drawable.icons8_l_h));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_selection_keep_selection_after_dictionary), Settings.PROP_APP_SELECTION_PERSIST,
				mActivity.getString(R.string.options_selection_keep_selection_after_dictionary_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary), "",
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dictionary2), "",
				mActivity.getString(R.string.options_app_dictionary2_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dict_word_correction), Settings.PROP_APP_DICT_WORD_CORRECTION,
				mActivity.getString(R.string.options_app_dict_word_correction_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dict_longtap_change), Settings.PROP_APP_DICT_LONGTAP_CHANGE,
				mActivity.getString(R.string.options_app_dict_longtap_change_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_show_user_dic_panel), Settings.PROP_APP_SHOW_USER_DIC_PANEL,
				mActivity.getString(R.string.options_app_show_user_dic_panel_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_font_size_user_dic), Settings.PROP_FONT_SIZE_USER_DIC,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_wiki1), Settings.PROP_CLOUD_WIKI1_ADDR,
				mActivity.getString(R.string.options_app_wiki1_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_wiki2), Settings.PROP_CLOUD_WIKI2_ADDR,
				mActivity.getString(R.string.options_app_wiki2_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.ynd_translate_settings), Settings.PROP_CLOUD_YND_TRANSLATE_OPTIONS,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.lingvo_settings), Settings.PROP_CLOUD_LINGVO_OPTIONS,
				mActivity.getString(R.string.lingvo_settings_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.deepl_settings), Settings.PROP_CLOUD_DEEPL_OPTIONS,
				mActivity.getString(R.string.deepl_settings_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.wiki_save_history), Settings.PROP_CLOUD_WIKI_SAVE_HISTORY,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.dict_dont_save_if_more), Settings.PROP_APP_DICT_DONT_SAVE_IF_MORE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.inspector_mode_no_dic_history), Settings.PROP_INSPECTOR_MODE_NO_DIC_HISTORY,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dict_word_correction), Settings.PROP_APP_DICT_WORD_CORRECTION,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mUserDicPanelKindTitles) this.updateFilteredMark(mActivity.getString(i));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
