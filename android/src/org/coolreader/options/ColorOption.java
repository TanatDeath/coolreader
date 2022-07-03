package org.coolreader.options;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.ColorPickerDialog;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

public class ColorOption extends OptionBase {
	final int defColor;

	public ColorOption(OptionOwner owner, String label, String property, int defColor, String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
		String[] colorNames = mActivity.getResources().getStringArray(R.array.colorNames);
		for(int i=0; i<colorNames.length; i++)
			this.updateFilteredMark(colorNames[i]);
		this.defColor = defColor;
	}

	public String getValueLabel() { return mProperties.getProperty(property); }

	public void onSelect()
	{
		if (!enabled)
			return;
		ColorPickerDialog dlg = new ColorPickerDialog(mActivity, color -> {
			mProperties.setColor(property, color);
			if (property.equals(Settings.PROP_BACKGROUND_COLOR)) {
				String texture = mProperties.getProperty(Settings.PROP_PAGE_BACKGROUND_IMAGE, Engine.NO_TEXTURE.id);
				if (texture != null && !texture.equals(Engine.NO_TEXTURE.id)) {
					// reset background image
					mProperties.setProperty(Settings.PROP_PAGE_BACKGROUND_IMAGE, Engine.NO_TEXTURE.id);
					// TODO: show notification?
				}
			}
			refreshList();
		}, mProperties.getColor(property, defColor), label);
		dlg.show();
	}
	public int getItemViewType() {
		return OPTION_VIEW_TYPE_COLOR;
	}
	public View getView(View convertView, ViewGroup parent) {
		View view;
		convertView = myView;
		if (convertView == null) {
			//view = new TextView(getContext());
			view = mInflater.inflate(R.layout.option_item_color, null);
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
		ImageView valueView = view.findViewById(R.id.option_value_color);
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
					//	Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
					//	toast.show();
					mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
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
		int cl = mProperties.getColor(property, defColor);
		valueView.setBackgroundColor(cl);
		setupIconView(view.findViewById(R.id.option_icon));
		view.setEnabled(enabled);
		mActivity.tintViewIcons(view,false);
		return view;
	}
}
