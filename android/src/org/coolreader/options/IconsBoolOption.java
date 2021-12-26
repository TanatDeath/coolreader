package org.coolreader.options;

import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.OptionOwner;

public class IconsBoolOption extends BoolOption {

	final OptionsDialog mOptionsDialog;
	public IconsBoolOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String property, String addInfo, String filter) {
		super(activity, owner, label, property, addInfo, filter);
		mOptionsDialog = od;
	}
	public void onSelect() {
		if (!enabled)
			return;
		mProperties.setProperty(property, "1".equals(mProperties.getProperty(property)) ? "0" : "1");
		if (null != onChangeHandler)
			onChangeHandler.run();
		mOptionsDialog.showIcons = mProperties.getBool(property, true);
		refreshList();
		mOptionsDialog.mOptionsStyles.refresh();
		mOptionsDialog.mOptionsCSS.refresh();
		mOptionsDialog.mOptionsPage.refresh();
		mOptionsDialog.mOptionsApplication.refresh();
		mOptionsDialog.mOptionsControls.refresh();
		if (null != mOptionsDialog.mOptionsCloudSync) {
			mOptionsDialog.mOptionsCloudSync.refresh();
		}
	}
}
