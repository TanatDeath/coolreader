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
	final ReaderView mReaderView;
	int screenWidth;

	public DeviceTurnOption(BaseActivity activity, OptionOwner owner, String label, String property, String addInfo, String filter ) {
		super(owner, label, property, addInfo, filter);
		mActivity = activity;
		mReaderView = ((CoolReader) mActivity).getmReaderView();
		screenWidth = mActivity.getWindowManager().getDefaultDisplay().getWidth();
		this.mProperties = owner.getProperties();
		List<ReaderAction> actions = ReaderAction.getAvailActions(true);
		for (ReaderAction a : actions) {
			Log.i("TAG", "TapZoneOption: " + a.id);
			this.updateFilteredMark(a.id, a.getNameText(activity), mActivity.getString(a.addInfoR));
		}
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
