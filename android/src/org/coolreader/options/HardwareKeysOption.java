package org.coolreader.options;

import android.view.View;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;

public class HardwareKeysOption extends SubmenuOption {

	final BaseActivity mActivity;

	public HardwareKeysOption(BaseActivity activity, OptionOwner owner, String label, String property, String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
		mActivity = activity;
		this.mProperties = owner.getProperties();
	}

	View grid;

	public String getValueLabel() { return ">"; }

	public void onSelect() {
		if (!enabled)
			return;
		grid = mInflater.inflate(R.layout.hardware_keys_options, null);
		BaseDialog dlg = new HardwareKeysDialog("HardwareKeysDialog",
				mActivity, mProperties, label, grid, true, false);
		dlg.setView(grid);
		dlg.show();
	}
}
