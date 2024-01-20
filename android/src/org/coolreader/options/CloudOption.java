package org.coolreader.options;

import android.content.Context;
import android.widget.Toast;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.CloudSync;
import org.coolreader.cloud.dropbox.DBXInputTokenDialog;
import org.coolreader.cloud.litres.LitresCredentialsDialog;
import org.coolreader.cloud.yandex.YNDInputTokenDialog;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class CloudOption extends SubmenuOption {

	int[] mCloudSyncVariants = new int[] {
			Settings.CLOUD_SYNC_VARIANT_DISABLED, Settings.CLOUD_SYNC_VARIANT_FILESYSTEM, Settings.CLOUD_SYNC_VARIANT_YANDEX
	};
	int[] mCloudSyncVariantsTitles = new int[] {
			R.string.cloud_sync_variant1, R.string.cloud_sync_variant2, R.string.cloud_sync_variant3
	};
	int[] mCloudSyncVariantsAddInfos = new int[] {
			R.string.cloud_sync_variant1_v, R.string.cloud_sync_variant2_v, R.string.cloud_sync_variant3_v
	};

	final CoolReader mActivity;
	final Context mContext;

	public CloudOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_CLOUD_TITLE, addInfo, filter);
		mActivity = (CoolReader) activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("CloudDialog", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.yandex_settings),
				Settings.PROP_CLOUD_YND_SETTINGS, mActivity.getString(R.string.yandex_settings_v), this.lastFilteredValue,
				(view, optionLabel, optionValue) ->
				{
					mActivity.yndInputTokenDialog = new YNDInputTokenDialog(mActivity);
					mActivity.yndInputTokenDialog.show();
				}, true).setDefaultValue(mActivity.getString(R.string.yandex_settings_v)).
				setIconIdByAttr(R.attr.attr_icons8_yandex, R.drawable.icons8_yandex_logo));
		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.ynd_home_folder),
				Settings.PROP_CLOUD_YND_HOME_FOLDER, mActivity.getString(R.string.ynd_home_folder_hint), this.lastFilteredValue,
				(view, optionLabel, optionValue) ->
						mActivity.showToast(mActivity.getString(R.string.ynd_home_folder_hint), Toast.LENGTH_LONG, view, true, 0), false).
				setDefaultValue("/").
				setIconIdByAttr(R.attr.cr3_browser_folder_root_drawable, R.drawable.cr3_browser_folder_root));

		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.show_ynd_in_file_systems_list), Settings.PROP_APP_SHOW_YND_IN_FILESYSTEM_CONTAINER_ENABLE,
				mActivity.getString(R.string.show_ynd_in_file_systems_list), this.lastFilteredValue, false).setDefaultValue("1").
				noIcon());

		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.dropbox_settings),
				Settings.PROP_CLOUD_DBX_SETTINGS, mActivity.getString(R.string.dropbox_settings_v), this.lastFilteredValue,
				(view, optionLabel, optionValue) ->
				{
					mActivity.dbxInputTokenDialog = new DBXInputTokenDialog(mActivity);
					mActivity.dbxInputTokenDialog.show();
				}, true).setDefaultValue(mActivity.getString(R.string.dropbox_settings)).
				setIconIdByAttr(R.attr.attr_icons8_dropbox_filled, R.drawable.icons8_dropbox_filled));

		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.show_dbx_in_file_systems_list), Settings.PROP_APP_SHOW_DBX_IN_FILESYSTEM_CONTAINER_ENABLE,
				mActivity.getString(R.string.show_dbx_in_file_systems_list), this.lastFilteredValue, false).setDefaultValue("1").
				noIcon());

		listView.add(new ClickOption(mOwner, mActivity.getString(R.string.litres_settings),
				Settings.PROP_CLOUD_LITRES_SETTINGS, mActivity.getString(R.string.litres_settings_add_info), this.lastFilteredValue,
				(view, optionLabel, optionValue) ->
				{
					mActivity.litresCredentialsDialog = new LitresCredentialsDialog(mActivity);
					mActivity.litresCredentialsDialog.show();
				}, true).setDefaultValue(mActivity.getString(R.string.litres_settings_add_info)).setIconIdByAttr(R.attr.attr_litres_en_logo_2lines, R.drawable.litres_en_logo_2lines));

		OptionBase optSaveToCloud = new ClickOption(mOwner, mActivity.getString(R.string.save_settings_to_cloud),
				mActivity.getString(R.string.save_settings_to_cloud_v), mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					int iSyncVariant = mProperties.getInt(Settings.PROP_CLOUD_SYNC_VARIANT, 0);
					if (iSyncVariant == 0) {
						mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
					} else {
						CloudSync.saveSettingsToFilesOrCloud(mActivity, false, iSyncVariant == 1);
					}
					return;
				}, true).
				setIconIdByAttr(R.attr.attr_icons8_settings_to_gd, R.drawable.icons8_settings_to_gd);
		OptionBase optLoadFromCloud = new ClickOption(mOwner, mActivity.getString(R.string.load_settings_from_cloud),
				mActivity.getString(R.string.load_settings_from_cloud_v), mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue,
				(view, optionLabel, optionValue) -> {
					int iSyncVariant = mProperties.getInt(Settings.PROP_CLOUD_SYNC_VARIANT, 0);
					if (iSyncVariant == 0) {
						mActivity.showToast(R.string.cloud_sync_variant1_v);
					} else {
						if (iSyncVariant == 1) CloudSync.loadSettingsFiles(mActivity,false);
						else
							CloudSync.loadFromJsonInfoFileList(mActivity,
									CloudSync.CLOUD_SAVE_SETTINGS, false, iSyncVariant == 1, CloudAction.NO_SPECIAL_ACTION, false);
					}
					return;
				}, true).
				setIconIdByAttr(R.attr.attr_icons8_settings_from_gd, R.drawable.icons8_settings_from_gd);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.cloud_sync_variant),
				Settings.PROP_CLOUD_SYNC_VARIANT, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mCloudSyncVariants, mCloudSyncVariantsTitles, mCloudSyncVariantsAddInfos).setDefaultValue(Integer.toString(mCloudSyncVariants[0])).
				setIconIdByAttr(R.attr.attr_icons8_cloud_storage, R.drawable.icons8_cloud_storage).
				setOnChangeHandler(() -> {
					int value = mProperties.getInt(Settings.PROP_CLOUD_SYNC_VARIANT, 0);
					optSaveToCloud.setEnabled(value != 0);
					optLoadFromCloud.setEnabled(value != 0);
				}));
		listView.add(optSaveToCloud);
		listView.add(optLoadFromCloud);
		int value = mProperties.getInt(Settings.PROP_CLOUD_SYNC_VARIANT, 0);
		optSaveToCloud.setEnabled(value != 0);
		optLoadFromCloud.setEnabled(value != 0);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.save_pos_to_cloud_timeout),
				Settings.PROP_SAVE_POS_TO_CLOUD_TIMEOUT, mActivity.getString(R.string.save_pos_to_cloud_timeout_add_info), this.lastFilteredValue).
				add(OptionsDialog.mMotionTimeouts, OptionsDialog.mMotionTimeoutsTitles, OptionsDialog.mMotionTimeoutsAddInfos).
					setDefaultValue(Integer.toString(OptionsDialog.mMotionTimeouts[0])).
				setIconIdByAttr(R.attr.attr_icons8_position_to_gd_interval, R.drawable.icons8_position_to_gd_interval));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.yandex_settings), "",
				mActivity.getString(R.string.yandex_settings_v));
		this.updateFilteredMark(mActivity.getString(R.string.dropbox_settings), "",
				mActivity.getString(R.string.dropbox_settings_v));
		this.updateFilteredMark(mActivity.getString(R.string.cloud_sync_variant), Settings.PROP_CLOUD_SYNC_VARIANT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.save_settings_to_cloud), "",
				mActivity.getString(R.string.save_settings_to_cloud_v));
		this.updateFilteredMark(mActivity.getString(R.string.load_settings_from_cloud), "",
				mActivity.getString(R.string.load_settings_from_cloud_v));
		this.updateFilteredMark(mActivity.getString(R.string.save_pos_to_cloud_timeout), Settings.PROP_SAVE_POS_TO_CLOUD_TIMEOUT,
				mActivity.getString(R.string.save_pos_to_cloud_timeout_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
