package org.coolreader.options;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.coolreader.R;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

public class ClickOption extends OptionBase {
	private boolean inverse = false;
	private OptionsDialog.ClickCallback ccb;
	private boolean defValue = false;

	public ClickOption(OptionOwner owner, String label, String property, String addInfo, String filter,
					   OptionsDialog.ClickCallback ccb, boolean defValue) {
		super(owner, label, property, addInfo, filter);
		this.ccb = ccb;
		this.defValue = defValue;
	}

	public int getItemViewType() {
		return OPTION_VIEW_TYPE_NORMAL;
	}
	public View getView(View convertView, ViewGroup parent) {
		View view;
		convertView = myView;
		if (convertView == null) {
			view = mInflater.inflate(R.layout.option_item, null);
			if (view != null) {
				TextView label = view.findViewById(R.id.option_label);
				if (label != null)
					if (mOwner instanceof OptionsDialog) {
						if (!Settings.isSettingBelongToProfile(property)) {
							if (!StrUtils.isEmptyStr(property))
								label.setTypeface(null, Typeface.ITALIC);
						}
					}
			}
		} else {
			view = convertView;
		}
		myView = view;
		TextView labelView = view.findViewById(R.id.option_label);
		TextView valueView = view.findViewById(R.id.option_value);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		valueView.setTextColor(mActivity.getTextColor(colorIcon));
		ImageView btnOptionAddInfo = view.findViewById(R.id.btn_option_add_info);
		if (addInfo.trim().equals("")) {
			btnOptionAddInfo.setVisibility(View.INVISIBLE);
		} else {
			btnOptionAddInfo.setImageDrawable(
					mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
							R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
			mActivity.tintViewIcons(btnOptionAddInfo);
			final View view1 = view;
			if (btnOptionAddInfo != null)
				btnOptionAddInfo.setOnClickListener(v -> mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0));
		}
		labelView.setText(label);
		if (labelView.isEnabled()) enabledColor = labelView.getCurrentTextColor();
		labelView.setEnabled(enabled);
		int colorIconT= Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		if (!enabled)
			labelView.setTextColor(colorIconT);
		else
			labelView.setTextColor(enabledColor);
		String currValue = "";
		if (defValue) currValue = defaultValue;
		else
			currValue = mProperties.getProperty(property);
		valueView.setText(currValue);
		if (ccb != null)
			view.setOnClickListener(v -> {
				ccb.click(view);
				refreshList();
				//final View view1 = view;
				//mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
				return;
			});
		view.setOnLongClickListener(v-> {
			if (!StrUtils.isEmptyStr(addInfo))
				mActivity.showToast(addInfo, Toast.LENGTH_LONG, view, true, 0);
			return true;
		});
		setupIconView(view.findViewById(R.id.option_icon));
		mActivity.tintViewIcons(view,false);
		return view;
	}
}
