package org.coolreader.options;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.coolreader.R;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Utils;
import org.coolreader.layouts.FlowLayout;

public class FlowListOption extends ListOption {
	public FlowListOption(OptionOwner owner, String label, String property, String addInfo, String filter ) {
		super( owner, label, property, addInfo, filter);
	}
	ViewGroup cont;
	FlowLayout fl;

	public void onSelect() {
		if (!enabled)
			return;
		int colorIcon = themeColors.get(R.attr.colorIcon);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGray = themeColors.get(R.attr.colorThemeGray2);
		whenOnSelect();
		BaseDialog dlg = new BaseDialog("FlowListDialog", mActivity, label, false, false);
		cont = (ViewGroup)mInflater.inflate(R.layout.options_flow_layout, null);
		fl = cont.findViewById(R.id.optionsFlowList);
		fl.removeAllViews();
		for (int i = 0; i < list.size(); i++) {
			ViewGroup opt = (ViewGroup) mInflater.inflate(R.layout.option_flow_value, null);
			Button dicButton1 = opt.findViewById(R.id.btn_item);
			Button dicButton = new Button(mActivity);
			dicButton.setText(list.get(i).label);
			dicButton.setTextSize(dicButton1.getTextSize());
			dicButton.setTextColor(mActivity.getTextColor(colorIcon));

			String currValue = mProperties.getProperty(property);
			boolean isSelected = list.get(i).value!=null && currValue!=null && list.get(i).value.equals(currValue) ;
			if (isSelected)
				dicButton.setBackgroundColor(colorGray);
			else {
				dicButton.setBackgroundColor(colorGrayC);
				Utils.setDashedButton1(dicButton);
			}
			LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			llp.setMargins(8, 4, 4, 8);
			dicButton.setLayoutParams(llp);
			dicButton.setMaxLines(1);
			fl.addView(dicButton);
			final OptionsDialog.Three item = list.get(i);
			dicButton.setOnClickListener(v -> {
				onClick(item);
				dlg.dismiss();
			});
		}
		dlg.setView(cont);
		dlg.show();
	}
}
