package org.coolreader.options;

import android.content.Context;

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

	final BaseActivity mActivity;
	final CoolReader mCoolReader;
	final OptionsDialog mOptionsDialog;

	public DictionariesOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter) {
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
		listView.add(OptionsDialog.getOption(Settings.PROP_APP_SELECTION_PERSIST, this.lastFilteredValue));
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
		AddDicsOption adddicso = (AddDicsOption) new AddDicsOption(this.mActivity, mOptionsDialog, mOwner,
				mActivity.getString(R.string.add_dics_settings),
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue)
				.setIconIdByAttr(R.attr.attr_icons8_google_translate, R.drawable.icons8_google_translate);
		adddicso.updateFilterEnd();
		listView.add(adddicso);
		UserDicOption userdicso = (UserDicOption) new UserDicOption(this.mActivity, mOptionsDialog, mOwner,
				mActivity.getString(R.string.user_dic_settings),
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue)
				.setIconIdByAttr(R.attr.attr_icons8_user_dic_panel, R.drawable.icons8_user_dic_panel);
		userdicso.updateFilterEnd();
		listView.add(userdicso);
		DicOnlineSevicesOption diconlineo = (DicOnlineSevicesOption) new DicOnlineSevicesOption(this.mActivity, mOptionsDialog, mOwner,
				mActivity.getString(R.string.dic_online_services_settings),
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue)
				.setIconIdByAttr(R.attr.attr_icons8_web_search, R.drawable.icons8_web_search);
		diconlineo.updateFilterEnd();
		listView.add(diconlineo);
		DicMultiListOption dicmlo = (DicMultiListOption) new DicMultiListOption(mOptionsDialog.getContext(),
				mOwner,
				mActivity.getString(R.string.dics_on_panel_settings),
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue)
				.setIconIdByAttr(R.attr.attr_icons8_dic_list, R.drawable.icons8_dic_list);
		dicmlo.updateFilterEnd();
		listView.add(dicmlo);
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
		listView.add(OptionsDialog.getOption(Settings.PROP_APP_DICT_LONGTAP_CHANGE, this.lastFilteredValue));
		listView.add(OptionsDialog.getOption(Settings.PROP_APP_DICT_WORD_CORRECTION, this.lastFilteredValue));
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
		this.updateFilteredMark(mActivity.getString(R.string.options_app_dict_word_correction), Settings.PROP_APP_DICT_WORD_CORRECTION,
				mActivity.getString(R.string.option_add_info_empty_text));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
