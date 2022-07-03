package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.plugins.OnlineStorePluginManager;

public class PluginsOption extends SubmenuOption {

	// This is legacy and isnt used for time

	final BaseActivity mActivity;
	final Context mContext;

	public PluginsOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_APP_PLUGIN_ENABLED, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}
	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("PluginsDialog", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext,this);
		boolean defEnableLitres = mActivity.getCurrentLanguage().toLowerCase().startsWith("ru") && !DeviceInfo.POCKETBOOK;
		listView.add(new BoolOption(mOwner, "LitRes", Settings.PROP_APP_PLUGIN_ENABLED + "." +
				OnlineStorePluginManager.PLUGIN_PKG_LITRES,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).setDefaultValue(defEnableLitres ? "1" : "0"));
		dlg.setView(listView);
		dlg.show();
	}

	public String getValueLabel() { return ">"; }
}
