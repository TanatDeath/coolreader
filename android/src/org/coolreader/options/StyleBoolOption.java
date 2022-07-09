package org.coolreader.options;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.coolreader.R;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.utils.Utils;

public class StyleBoolOption extends OptionBase {
	private String onValue;
	private String offValue;
	public StyleBoolOption(OptionOwner owner, String label, String property, String addInfo, String filter, String onValue, String offValue) {
		super(owner, label, property, addInfo, filter, false);
		this.onValue = onValue;
		this.offValue = offValue;
	}
	private boolean getValueBoolean() {
		return onValue.equals(mProperties.getProperty(property));
	}
	public OptionBase setDefaultValueBoolean(boolean value) {
		defaultValue = value ? onValue : offValue;
		if ( mProperties.getProperty(property)==null )
			mProperties.setProperty(property, defaultValue);
		return this;
	}
	public void onSelect() {
		if (!enabled)
			return;
		// Toggle the state
		mProperties.setProperty(property, getValueBoolean() ? offValue : onValue);
		refreshList();
	}
	public int getItemViewType() {
		return OPTION_VIEW_TYPE_BOOLEAN;
	}
	public View getView(View convertView, ViewGroup parent) {
		View view;
		convertView = myView;
		if ( convertView==null ) {
			//view = new TextView(getContext());
			view = mInflater.inflate(R.layout.option_item_boolean, null);
		} else {
			view = convertView;
		}
		myView = view;
		TextView labelView = view.findViewById(R.id.option_label);
		ImageView valueView = view.findViewById(R.id.option_value_cb);
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
				btnOptionAddInfo.setOnClickListener(v -> {
					String commentLabel = addInfo;
					if (!enabled && null != disabledNote && disabledNote.length() > 0) {
						if (null != commentLabel && commentLabel.length() > 0)
							commentLabel = commentLabel + " (" + disabledNote + ")";
						else
							commentLabel = disabledNote;
					}
					mActivity.showToast(commentLabel, Toast.LENGTH_LONG, view1, true, 0);
				});
		}
		labelView.setText(label);
		if (labelView.isEnabled()) enabledColor = labelView.getCurrentTextColor();
		labelView.setEnabled(enabled);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		int colorIconT= Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		if (!enabled)
			labelView.setTextColor(colorIconT);
		else
			labelView.setTextColor(enabledColor);
		setCheckedOption(valueView,getValueBoolean());
		setupIconView(view.findViewById(R.id.option_icon));
		valueView.setEnabled(enabled);
		mActivity.tintViewIcons(view,false);
		return view;
	}
}
