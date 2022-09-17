package org.coolreader.options;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.AskSomeValuesDialog;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;

import java.util.ArrayList;

public class DicOnlineSevicesOption extends SubmenuOption {

	final CoolReader mActivity;
	final OptionsDialog mOptionsDialog;

	public DicOnlineSevicesOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_ADD_DIC_TITLE, addInfo, filter);
		mActivity = (CoolReader) activity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("DicOnlineSevicesOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		listView.add(new WikiOption(mOwner, mActivity.getString(R.string.options_app_wiki1), Settings.PROP_CLOUD_WIKI1_ADDR,
				mActivity.getString(R.string.options_app_wiki1_add_info), this.lastFilteredValue).setDefaultValue("https://en.wikipedia.org").
				setIconIdByAttr(R.attr.attr_icons8_wiki1, R.drawable.icons8_wiki1));
		listView.add(new WikiOption(mOwner, mActivity.getString(R.string.options_app_wiki2), Settings.PROP_CLOUD_WIKI2_ADDR,
				mActivity.getString(R.string.options_app_wiki2_add_info), this.lastFilteredValue).setDefaultValue("https://ru.wikipedia.org").
				setIconIdByAttr(R.attr.attr_icons8_wiki2, R.drawable.icons8_wiki2));
		listView.add(OptionsDialog.getOption(Settings.PROP_CLOUD_WIKI_SAVE_HISTORY, this.lastFilteredValue));
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.ynd_translate_settings),
				Settings.PROP_CLOUD_YND_TRANSLATE_OPTIONS, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					ArrayList<String[]> vl = new ArrayList<>();
					mActivity.readYndCloudSettings();
					String[] arrS1 = {mActivity.getString(R.string.ynd_oauth), mActivity.getString(R.string.ynd_oauth),
							StrUtils.getNonEmptyStr(mActivity.yndCloudSettings.oauthToken, true)};
					vl.add(arrS1);
					String[] arrS2 = {mActivity.getString(R.string.ynd_folder_id), mActivity.getString(R.string.ynd_folder_id),
							StrUtils.getNonEmptyStr(mActivity.yndCloudSettings.folderId, true)};
					vl.add(arrS2);
					AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
							mActivity,
							mActivity.getString(R.string.ynd_cloud_settings),
							mActivity.getString(R.string.ynd_cloud_settings),
							vl, results -> {
						if (results != null) {
							if (results.size() >= 1)
								mActivity.yndCloudSettings.oauthToken = StrUtils.getNonEmptyStr(results.get(0), true);
							if (results.size() >= 2)
								mActivity.yndCloudSettings.folderId = StrUtils.getNonEmptyStr(results.get(1), true);
						}
						Gson gson = new GsonBuilder().setPrettyPrinting().create();
						final String prettyJson = gson.toJson(mActivity.yndCloudSettings);
						mActivity.saveYndCloudSettings(prettyJson);
					});
					dlgA.show();
				}, true).
				noIcon());
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.lingvo_settings),
				Settings.PROP_CLOUD_LINGVO_OPTIONS, mActivity.getString(R.string.lingvo_settings_add_info), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					ArrayList<String[]> vl = new ArrayList<>();
					mActivity.readLingvoCloudSettings();
					String[] arrS1 = {mActivity.getString(R.string.lingvo_token), mActivity.getString(R.string.lingvo_token),
							StrUtils.getNonEmptyStr(mActivity.lingvoCloudSettings.lingvoToken, true)};
					vl.add(arrS1);
					AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
							mActivity,
							mActivity.getString(R.string.lingvo_settings),
							mActivity.getString(R.string.lingvo_settings),
							vl, results -> {
						if (results != null) {
							if (results.size() >= 1)
								mActivity.lingvoCloudSettings.lingvoToken = StrUtils.getNonEmptyStr(results.get(0), true);
						}
						Gson gson = new GsonBuilder().setPrettyPrinting().create();
						final String prettyJson = gson.toJson(mActivity.lingvoCloudSettings);
						mActivity.saveLingvoCloudSettings(prettyJson);
					});
					dlgA.show();
				}, true).
				noIcon());
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.deepl_settings),
				Settings.PROP_CLOUD_DEEPL_OPTIONS, mActivity.getString(R.string.deepl_settings_add_info), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					ArrayList<String[]> vl = new ArrayList<>();
					mActivity.readLingvoCloudSettings();
					String[] arrS1 = {mActivity.getString(R.string.deepl_token), mActivity.getString(R.string.deepl_token),
							StrUtils.getNonEmptyStr(mActivity.deeplCloudSettings.deeplToken, true)};
					vl.add(arrS1);
					AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
							mActivity,
							mActivity.getString(R.string.deepl_settings),
							mActivity.getString(R.string.deepl_settings),
							vl, results -> {
						if (results != null) {
							if (results.size() >= 1)
								mActivity.deeplCloudSettings.deeplToken = StrUtils.getNonEmptyStr(results.get(0), true);
						}
						Gson gson = new GsonBuilder().setPrettyPrinting().create();
						final String prettyJson = gson.toJson(mActivity.deeplCloudSettings);
						mActivity.saveDeeplCloudSettings(prettyJson);
					});
					dlgA.show();
				}, true).
				noIcon());
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_app_wiki1), Settings.PROP_CLOUD_WIKI1_ADDR,
				mActivity.getString(R.string.options_app_wiki1_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_app_wiki2), Settings.PROP_CLOUD_WIKI2_ADDR,
				mActivity.getString(R.string.options_app_wiki2_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.wiki_save_history), Settings.PROP_CLOUD_WIKI_SAVE_HISTORY,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.ynd_translate_settings), Settings.PROP_CLOUD_YND_TRANSLATE_OPTIONS,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.lingvo_settings), Settings.PROP_CLOUD_LINGVO_OPTIONS,
				mActivity.getString(R.string.lingvo_settings_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.deepl_settings), Settings.PROP_CLOUD_DEEPL_OPTIONS,
				mActivity.getString(R.string.deepl_settings_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
