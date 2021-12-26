package org.coolreader.options;

import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.coolreader.R;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

import java.util.ArrayList;

public class ActionOptionExt extends OptionBase {
	private ArrayList<OptionsDialog.Three> list = new ArrayList<OptionsDialog.Three>();

	final ActionClickedCallback mButtonClick;
	public ActionOptionExt(ActionClickedCallback buttonClick, OptionOwner owner, String label, String property, String addInfo, String filter ) {
		super(owner, label, property, addInfo, filter);
		mButtonClick = buttonClick;
	}
	public void add(String value, String label, String addInfo) {
		list.add( new OptionsDialog.Three(value, label, addInfo) );
	}
	public ActionOptionExt add(String[]values) {
		for ( String item : values ) {
			add(item, item, mActivity.getString(R.string.option_add_info_empty_text));
		}
		return this;
	}
	public ActionOptionExt add(double[]values) {
		for ( double item : values ) {
			String s = String.valueOf(item);
			add(s, s, mActivity.getString(R.string.option_add_info_empty_text));
		}
		return this;
	}
	public ActionOptionExt add(int[]values) {
		for ( int item : values ) {
			String s = String.valueOf(item);
			add(s, s, mActivity.getString(R.string.option_add_info_empty_text));
		}
		return this;
	}
	public ActionOptionExt add(int[]values, int[]labelIDs, int[]addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = String.valueOf(values[i]);
			String label = mActivity.getString(labelIDs[i]);
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
		}
		return this;
	}
	public ActionOptionExt add(String[]values, int[]labelIDs, int[]addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = values[i];
			String label = mActivity.getString(labelIDs[i]);
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
		}
		return this;
	}
	public ActionOptionExt add(String[]values, String[]labels, int[]addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = values[i];
			String label = labels[i];
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
		}
		return this;
	}
	public ActionOptionExt add(int[]values, String[]labels, int[]addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = String.valueOf(values[i]);
			String label = labels[i];
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
		}
		return this;
	}
	public ActionOptionExt addPercents(int[]values) {
		for ( int item : values ) {
			String s = String.valueOf(item);
			add(s, s + "%", mActivity.getString(R.string.option_add_info_empty_text));
		}
		return this;
	}
	public String findValueLabel( String value ) {
		for ( OptionsDialog.Three three : list ) {
			if (value != null && three.value.equals(value))
				return three.label;
		}
		return null;
	}
	public int findValue( String value ) {
		if (value == null)
			return -1;
		for ( int i=0; i<list.size(); i++ ) {
			if (value.equals(list.get(i).value))
				return i;
		}
		return -1;
	}

	public int getSelectedItemIndex() {
		return findValue(mProperties.getProperty(property));
	}

	protected void closed() {

	}

	protected int getItemLayoutId() {
		return R.layout.option_value;
	}

	@Override
	public View getView(View convertView, ViewGroup parent) {
		View view;
		convertView = myView;
		if (convertView == null) {
			view = mInflater.inflate(R.layout.option_item_action_ext, null);
			if (view != null) {
				TextView label = (TextView) view.findViewById(R.id.option_label);
				if (label != null)
					if (mOwner instanceof OptionsDialog) {
						if (!Settings.isSettingBelongToProfile(property)) {
							if (!StrUtils.isEmptyStr(property))
								label.setTypeface(null, Typeface.ITALIC);
						}
					}
			}
		} else {
			view = (View)convertView;
		}
		myView = view;
		TextView labelView = view.findViewById(R.id.option_label);
		TextView labelView2 = view.findViewById(R.id.option_label2);
		ImageView icon2 = view.findViewById(R.id.option_icon2);
		TextView valueView = view.findViewById(R.id.option_value);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		if (valueView != null) valueView.setTextColor(mActivity.getTextColor(colorIcon));
		ImageView icon1 = view.findViewById(R.id.option_icon);
		setup2IconView(icon1,icon2);
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
					//Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
					//toast.show();
					mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
				});
		}
		String lab1 = label;
		String lab2 = "";
		if (lab1.contains("~")) {
			lab2 = lab1.split("~")[1];
			lab1 = lab1.split("~")[0];
		}
		labelView.setText(lab1);
		if (label.contains("~")) labelView2.setText(lab2);
			else labelView2.setText(mActivity.getText(R.string.long_tap)+": "+mActivity.getText(R.string.action_none));
		if (valueView != null) {
			String valueLabel = getValueLabel();
			if (valueLabel != null && valueLabel.length() > 0) {
				valueView.setText(valueLabel);
				valueView.setVisibility(View.VISIBLE);
			} else {
				valueView.setText("");
				valueView.setVisibility(View.INVISIBLE);
			}
		}
		Button toToolbar = view.findViewById(R.id.btn_to_toolbar);
		Button toMenu = view.findViewById(R.id.btn_to_menu);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		toToolbar.setOnClickListener(v -> {
			mButtonClick.onActionClick(this, 0);
		});
		toMenu.setOnClickListener(v -> {
			mButtonClick.onActionClick(this, 1);
		});
		llp.setMargins(8, 4, 4, 4);
		toToolbar.setLayoutParams(llp);
		toMenu.setLayoutParams(llp);
		toToolbar.setPadding(10, 20, 10, 20);
		toMenu.setPadding(10, 20, 10, 20);
		int colorGray;
		colorGray = themeColors.get(R.attr.colorThemeGray2);
		toToolbar.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
		toMenu.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
		//label = label.replace("~", " / ");
		setupIconView(view.findViewById(R.id.option_icon));
		mActivity.tintViewIcons(view,false);
		return view;
	}

	protected void updateItemContents(final View layout, final OptionsDialog.Three item, final ListView listView, final int position ) {
		TextView view;
		view = layout.findViewById(R.id.option_value_text);
		ImageView btnOptionAddInfo = layout.findViewById(R.id.btn_option_add_info);

		if (item.addInfo.trim().equals("")) {
			btnOptionAddInfo.setVisibility(View.INVISIBLE);
		} else {
			btnOptionAddInfo.setImageDrawable(
					mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
							R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
			mActivity.tintViewIcons(btnOptionAddInfo);
			final View view1 = layout;
			if (btnOptionAddInfo != null)
				btnOptionAddInfo.setOnClickListener(v -> {
					//Toast toast = Toast.makeText(mActivity, item.addInfo, Toast.LENGTH_LONG);
					//toast.show();
					mActivity.showToast(item.addInfo, Toast.LENGTH_LONG, view1, true, 0);
				});
		}
		view.setText(item.label);
	}

	public String getValueLabel() { return findValueLabel(mProperties.getProperty(property)); }

	public void onSelect() {
		return;
	}

	public void onClick( OptionsDialog.Three item ) {
		mProperties.setProperty(property, item.value);
		refreshList();
		if (onChangeHandler != null)
			onChangeHandler.run();
		if (optionsListView != null)
			optionsListView.refresh();
	}
}
