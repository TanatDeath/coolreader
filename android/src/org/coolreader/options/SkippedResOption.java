package org.coolreader.options;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ResizeHistory;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.Utils;

public class SkippedResOption extends SubmenuOption {

	private ListView listView;

	final BaseActivity mActivity;
	final Context mContext;

	public SkippedResOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_SKIPPED_RES, addInfo,filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("SkippedResDialog", mActivity, label, false, false);
		View view = mInflater.inflate(R.layout.searchable_listview, null);
		LinearLayout viewList = (LinearLayout)view.findViewById(R.id.lv_list);
		final EditText tvSearchText = (EditText)view.findViewById(R.id.search_text);
		ImageButton ibSearch = (ImageButton)view.findViewById(R.id.btn_search);
		final OptionsListView listView = new OptionsListView(mContext, this);
		tvSearchText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				listView.listUpdated(cs.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

			@Override
			public void afterTextChanged(Editable arg0) {}
		});

		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT= Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		tvSearchText.setBackgroundColor(colorGrayCT);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		tvSearchText.setTextColor(mActivity.getTextColor(colorIcon));
		int colorIcon128 = Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		tvSearchText.setHintTextColor(colorIcon128);
		if (isEInk) Utils.setSolidEditEink(tvSearchText);

		((CoolReader)mActivity).readResizeHistory();
		for (ResizeHistory rh: ((CoolReader)mActivity).getResizeHist()) {
			String sProp = rh.X+"."+rh.Y;
			String sText = rh.X+" x "+rh.Y+" ("+ Utils.formatDate2(mActivity, rh.lastSet)+" "+
					Utils.formatTime(mActivity, rh.lastSet)+")";
			listView.add(new BoolOption(mActivity, mOwner, sText, Settings.PROP_SKIPPED_RES+"."+sProp, "", this.lastFilteredValue).setDefaultValue("0").
					setIconId(0));
		}
		viewList.addView(listView);
		ibSearch.setOnClickListener(v -> {
			tvSearchText.setText("");
			listView.listUpdated("");
		});
		dlg.setView(view);
		ibSearch.requestFocus();
		//dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		for (ResizeHistory rh: ((CoolReader)mActivity).getResizeHist()) {
			String sProp = rh.X+"."+rh.Y;
			String sText = rh.X+" x "+rh.Y+" ("+Utils.formatDate2(mActivity, rh.lastSet)+" "+
					Utils.formatTime(mActivity, rh.lastSet)+")";
			this.updateFilteredMark(sText);
			this.updateFilteredMark(Settings.PROP_SKIPPED_RES+"."+sProp);
		}
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
