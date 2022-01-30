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

public class SubmenuOption extends ListOption {
	public SubmenuOption(OptionOwner owner, String label, String property, String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
	}
	public int getItemViewType() {
		return OPTION_VIEW_TYPE_SUBMENU;
	}
	public View getView(View convertView, ViewGroup parent) {
		View view;
		convertView = myView;
		if (convertView == null) {
			//view = new TextView(getContext());
			view = mInflater.inflate(R.layout.option_item_submenu, null);
			mActivity.tintViewIcons(view);
		} else {
			view = convertView;
		}
		myView = view;
		TextView labelView = view.findViewById(R.id.option_label);
		labelView.setText(label);
		if (labelView.isEnabled()) enabledColor = labelView.getCurrentTextColor();
		labelView.setEnabled(enabled);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		int colorIconT= Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		if (!enabled)
			labelView.setTextColor(colorIconT);
		else
			labelView.setTextColor(enabledColor);
		setupIconView(view.findViewById(R.id.option_icon));
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
		mActivity.tintViewIcons(view, false);
		return view;
	}
}

