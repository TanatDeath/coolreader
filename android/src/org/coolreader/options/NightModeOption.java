package org.coolreader.options;

import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.OptionOwner;

public class NightModeOption extends BoolOption {
	public NightModeOption(BaseActivity activity, OptionOwner owner, String label, String property, String addInfo, String filter) {
		super(activity, owner, label, property, addInfo, filter);
		this.updateFilteredMark("night");
	}
	public void onSelect() {
		if (!enabled)
			return;
		OptionsDialog.toggleDayNightMode(mProperties);
		refreshList();
	}
}