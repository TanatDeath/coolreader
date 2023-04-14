package org.coolreader.options;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.readerview.ReaderView;

import java.util.List;

public class DeviceTurnOption extends SubmenuOption {

	final BaseActivity mActivity;

	public DeviceTurnOption(BaseActivity activity, OptionOwner owner, String label, String property, String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
		mActivity = activity;
		this.mProperties = owner.getProperties();
	}

	View grid;

	public String getValueLabel() { return ">"; }

	public void onSelect() {
		if (!enabled)
			return;
		grid = mInflater.inflate(R.layout.device_turn_options, null);
		BaseDialog dlg = new DeviceTurnDialog("DeviceTurnDialog",
				mActivity, mProperties, label, grid, false, false);
		dlg.setView(grid);
		dlg.show();
	}
}
