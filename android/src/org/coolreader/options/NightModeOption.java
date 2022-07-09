package org.coolreader.options;

import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.OptionOwner;

public class NightModeOption extends BoolOption {
	public NightModeOption(OptionOwner owner, String label, String property, String addInfo, String filter) {
		super(owner, label, property, addInfo, filter, false);
		this.updateFilteredMark("night");
	}
	public void onSelect() {
		if (!enabled)
			return;
		OptionsDialog.toggleDayNightMode(mProperties);
		refreshList();
	}
}