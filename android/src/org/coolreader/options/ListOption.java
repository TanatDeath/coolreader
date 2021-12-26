package org.coolreader.options;

import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.List;
import java.util.Map;

public class ListOption extends OptionBase {
	protected ArrayList<OptionsDialog.Three> list = new ArrayList<>();
	protected ArrayList<OptionsDialog.Three> listFiltered = new ArrayList<>();

	class ListOptionAdapter extends BaseAdapter {

		private final ListView mListView;
		private final List<OptionsDialog.Three> mListFiltered;

		ListOptionAdapter(ListView listView, List<OptionsDialog.Three> listFiltered) {
			super();
			mListView = listView;
			mListFiltered = listFiltered;
		}

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int position) {
			return true;
		}

		public int getCount() {
			return mListFiltered.size();
		}

		public Object getItem(int position) {
			return mListFiltered.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewGroup layout;
			if ( convertView==null ) {
				layout = (ViewGroup)mInflater.inflate(getItemLayoutId(), null);
			} else {
				layout = (ViewGroup)convertView;
			}
			final OptionsDialog.Three item = mListFiltered.get(position);
			updateItemContents(layout, item, mListView, position);
			return layout;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mListFiltered.size()==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}

	};

	protected BaseAdapter listAdapter;
	protected ListView listView;

	public ListOption(OptionOwner owner, String label, String property, String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
	}

	public ListOption add(String value, String label, String addInfo) {
		list.add( new OptionsDialog.Three(value, label, addInfo) );
		listFiltered.add( new OptionsDialog.Three(value, label, addInfo) );
		this.updateFilteredMark(value);
		this.updateFilteredMark(label);
		this.updateFilteredMark(addInfo);
		return this;
	}

	public ListOption add(int value, int labelID, String addInfo) {
		String str_value = String.valueOf(value);
		String label = mActivity.getString(labelID);
		list.add( new OptionsDialog.Three(str_value, label, addInfo) );
		listFiltered.add(new OptionsDialog.Three(str_value, label, addInfo));
		this.updateFilteredMark(str_value);
		this.updateFilteredMark(label);
		this.updateFilteredMark(addInfo);
		return this;
	}

	public ListOption add(int value, int labelID, int addInfo) {
		String str_value = String.valueOf(value);
		String label = mActivity.getString(labelID);
		list.add( new OptionsDialog.Three(str_value, label,  mActivity.getString(addInfo)) );
		listFiltered.add( new OptionsDialog.Three(str_value, label, mActivity.getString(addInfo)) );
		this.updateFilteredMark(str_value);
		this.updateFilteredMark(label);
		this.updateFilteredMark(mActivity.getString(addInfo));
		return this;
	}

	public ListOption add(int value, String label, String addInfo) {
		String str_value = String.valueOf(value);
		list.add(new OptionsDialog.Three(str_value, label, addInfo));
		listFiltered.add(new OptionsDialog.Three(str_value, label, addInfo));
		this.updateFilteredMark(str_value);
		this.updateFilteredMark(label);
		this.updateFilteredMark(addInfo);
		return this;
	}

	public ListOption add(String[]values) {
		for (String item : values) {
			add(item, item, mActivity.getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(item);
		}
		return this;
	}
	public ListOption add(double[]values) {
		for (double item : values) {
			String s = String.valueOf(item);
			add(s, s, mActivity.getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(s);
		}
		return this;
	}
	public ListOption add(int[]values) {
		for (int item : values) {
			String s = String.valueOf(item);
			add(s, s, mActivity.getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(s);
		}
		return this;
	}

	public ListOption add(int[]values, int[]labelIDs, int[]addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = String.valueOf(values[i]);
			String label = mActivity.getString(labelIDs[i]);
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
			this.updateFilteredMark(value);
			this.updateFilteredMark(label);
			this.updateFilteredMark(addInfo);
		}
		return this;
	}

	public ListOption add(int[]values, String[]labelIDs) {
		for ( int i=0; i<values.length; i++ ) {
			String value = String.valueOf(values[i]);
			String label = labelIDs[i];
			String addInfo = "";
			add(value, label, addInfo);
			this.updateFilteredMark(value);
			this.updateFilteredMark(label);
			this.updateFilteredMark(addInfo);
		}
		return this;
	}

	public ListOption addSkip1(int[]values, int[]labelIDs, int[]addInfos) {
		if (values.length > 1)
			for ( int i=1; i<values.length; i++ ) {
				String value = String.valueOf(values[i]);
				String label = mActivity.getString(labelIDs[i]);
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
				this.updateFilteredMark(value);
				this.updateFilteredMark(label);
				this.updateFilteredMark(addInfo);
			}
		return this;
	}

	protected void whenOnSelect() {

	}

	public ListOption add2(int[]values, int labelID, int[]labelIDs, int[]addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = String.valueOf(values[i]);
			String labelConst = mActivity.getString(labelID);
			String sLab = "";
			if (labelIDs[i]<0) sLab = labelConst.trim()+" " + String.valueOf((int)-labelIDs[i]);
			else sLab = mActivity.getString(labelIDs[i]);
			String label = sLab;
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
			this.updateFilteredMark(value);
			this.updateFilteredMark(label);
			this.updateFilteredMark(addInfo);
		}
		return this;
	}
	public ListOption add(String[]values, int[]labelIDs, int[]addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = values[i];
			String label = mActivity.getString(labelIDs[i]);
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
			this.updateFilteredMark(value);
			this.updateFilteredMark(label);
			this.updateFilteredMark(addInfo);
		}
		return this;
	}
	public ListOption add(String[]values, String[]labels, int[]addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = values[i];
			String label = labels[i];
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
			this.updateFilteredMark(value);
			this.updateFilteredMark(label);
			this.updateFilteredMark(addInfo);
		}
		return this;
	}
	public ListOption add(int[]values, String[]labels, int[] addInfos) {
		for ( int i=0; i<values.length; i++ ) {
			String value = String.valueOf(values[i]);
			String label = labels[i];
			String addInfo = mActivity.getString(addInfos[i]);
			add(value, label, addInfo);
			this.updateFilteredMark(value);
			this.updateFilteredMark(label);
			this.updateFilteredMark(addInfo);
		}
		return this;
	}

	public ListOption add(List<?> values, List<String> labels, List<?> addInfos) {
		for ( int i=0; i < values.size(); i++ ) {
			String value = String.valueOf(values.get(i));
			String label = labels.get(i);
			String addInfo = String.valueOf(addInfos.get(i));
			if (!addInfo.equals("0")) addInfo = mActivity.getString(Integer.valueOf(addInfo));
			else addInfo = "";
			add(value, label, addInfo);
			this.updateFilteredMark(value);
			this.updateFilteredMark(label);
			this.updateFilteredMark(addInfo);
		}
		return this;
	}

	public ListOption addPercents(int[]values) {
		for ( int item : values ) {
			String s = String.valueOf(item);
			add(s, s + "%", mActivity.getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(s);
		}
		return this;
	}

	public void clear() {
		list.clear();
		refreshList();
	}

	public String findValueLabel( String value ) {
		for ( OptionsDialog.Three three: list ) {
			if (three.value.equals(value))
				return updateValue(three.label);
		}
		return updateValue(value); // for delayed loading list ...
		//return null;
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

	@Override
	protected void valueIncDec(boolean isInc) {
		String currValue = mProperties.getProperty(property);
		for (int i = 0; i < list.size(); i++) {
			if ((list.get(i).value.equals(currValue)) || (currValue == null)) {
				int ii = i + 1;
				if (!isInc) ii = ii - 2;
				if (currValue == null) ii = 0;
				if ((ii >= 0) && (ii < list.size())) {
					mProperties.setProperty(property, String.valueOf(list.get(ii).value));
					View view1 = getView(null, null);
					TextView editText = view1.findViewById(R.id.option_value);
					if (editText != null)
						editText.setText(String.valueOf(list.get(ii).label));
				}
				break;
			}
		}
	}

	public int getSelectedItemIndex() {
		return findValue(mProperties.getProperty(property));
	}

	protected void closed() {
	}

	protected int getItemLayoutId() {
		return R.layout.option_value;
	}

	protected String updateValue(String value) {
		return value;
	}

	protected void updateItemContents(final View layout, final OptionsDialog.Three item, final ListView listView, final int position) {
		TextView view;
		ImageView cb;
		//iv = (ImageView) = layout.findViewById(R.id.option_value_text);
		view = layout.findViewById(R.id.option_value_text);
		cb = layout.findViewById(R.id.option_value_check);
		ImageView btnOptionAddInfo = layout.findViewById(R.id.btn_option_add_info);

		if (item.addInfo.trim().equals("")) {
			if (btnOptionAddInfo!=null)
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
		} else {
			if (btnOptionAddInfo!=null) {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
			}
			final View view1 = layout;
			if (btnOptionAddInfo != null)
				btnOptionAddInfo.setOnClickListener(v -> {
					//Toast toast = Toast.makeText(mActivity, item.addInfo, Toast.LENGTH_LONG);
					//toast.show();
					mActivity.showToast(item.addInfo, Toast.LENGTH_LONG, view1, true, 0);
				});
		}
		view.setText(updateValue(item.label));
		mActivity.tintViewIcons(view,false);
		String currValue = mProperties.getProperty(property);
		boolean isSelected = item.value!=null && currValue!=null && item.value.equals(currValue) ;//getSelectedItemIndex()==position;

		//cb.setChecked(isSelected);
		setCheckedValue(cb, isSelected);
		cb.setOnClickListener(v -> {
			AdapterView.OnItemClickListener listener = listView.getOnItemClickListener();
			if (null != listener)
				listener.onItemClick(listView, listView, position, 0);
		});
	}

	public String getValueLabel() { return findValueLabel(mProperties.getProperty(property)); }

	protected boolean addToQuickFilters(ViewGroup qfView) {
		return false;

	}

	public void listUpdatedAfter() {
		listAdapter = new ListOptionAdapter(listView, listFiltered);
		int selItem = getSelectedItemIndex();
		if (selItem < 0)
			selItem = 0;
		listView.setAdapter(listAdapter);
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//				Utils.setScrollBarsFadeOff(listView);
//			}
		listAdapter.notifyDataSetChanged();
		listView.setSelection(selItem);
	}

	public void listUpdated(ArrayList<OptionsDialog.Three> listF) {
		listFiltered = listF;
		listUpdatedAfter();
	}

	public void listUpdated(String sText) {
		listFiltered.clear();
		for(int i=0;i<list.size();i++){
			if (
					((list.get(i).label.toLowerCase()).contains(sText.toLowerCase()))||
							((list.get(i).value.toLowerCase()).contains(sText.toLowerCase()))||
							((list.get(i).addInfo.toLowerCase()).contains(sText.toLowerCase()))
			) {
				OptionsDialog.Three item = new OptionsDialog.Three(
						list.get(i).value, list.get(i).label, list.get(i).addInfo);
				listFiltered.add(item);
			}
		}
		listUpdatedAfter();
	}

	public void onSelect() {
		if (!enabled)
			return;
		whenOnSelect();
		final BaseDialog dlg = new BaseDialog("ListOptionDialog", mActivity, label, false, false);
		View view = mInflater.inflate(R.layout.searchable_listview, null);
		LinearLayout viewList = view.findViewById(R.id.lv_list);
		final EditText tvSearchText = view.findViewById(R.id.search_text);
		tvSearchText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				listUpdated(cs.toString());
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

		ImageButton ibSearch = (ImageButton)view.findViewById(R.id.btn_search);
		listView = new BaseListView(mActivity, false);
		listUpdated("");
		int colorGray = themeColors.get(R.attr.colorThemeGray2);
		int newTextSize = mActivity.settings().getInt(Settings.PROP_STATUS_FONT_SIZE, 16);

		LinearLayout llUL = view.findViewById(R.id.ll_useful_links);

		if (usefulLinks.size()>0) {
			for (Map.Entry<String, String> entry : usefulLinks.entrySet()) {
				//System.out.println(entry.getKey() + " = " + entry.getValue());
				TextView ulText = new TextView(mActivity);
				ulText.setText(entry.getKey());
				ulText.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
				ulText.setTextColor(mActivity.getTextColor(colorIcon));
				ulText.setPaintFlags(ulText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				ulText.setOnClickListener(view1 -> {
					Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(entry.getValue()));
					mActivity.startActivity(myIntent);
				});
				llUL.addView(ulText);
			}
		}  else {
			((ViewGroup)llUL.getParent()).removeView(llUL);
		}

		LinearLayout llQF = view.findViewById(R.id.ll_quick_filters);

		if (quickFilters.size()>0) {
			for (String s: quickFilters) {
				Button qfButton = new Button(mActivity);
				qfButton.setText(s);
				qfButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
				qfButton.setTextColor(mActivity.getTextColor(colorIcon));
				qfButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
				qfButton.setPadding(1, 1, 1, 1);
				LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				llp.setMargins(4, 1, 1, 4);
				qfButton.setLayoutParams(llp);
				qfButton.setMaxLines(1);
				qfButton.setEllipsize(TextUtils.TruncateAt.END);
				llQF.addView(qfButton);
				qfButton.setOnClickListener(v -> tvSearchText.setText(qfButton.getText()));
			}
			addToQuickFilters(llQF);
		} else {
			if (!addToQuickFilters(llQF))
				((ViewGroup)llQF.getParent()).removeView(llQF);
		}
		viewList.addView(listView);
		ibSearch.setOnClickListener(v -> {
			tvSearchText.setText("");
			listUpdated("");
		});
		dlg.setView(view);
		ibSearch.requestFocus();
		//final AlertDialog d = dlg.create();
		listView.setOnItemClickListener((adapter, listview, position, id) -> {
			OptionsDialog.Three item = listFiltered.get(position);
			onClick(item);
			dlg.dismiss();
			closed();
		});
		dlg.setOnDismissListener(dialog -> {
			String sOldProp = StrUtils.getNonEmptyStr(mProperties.getProperty(property),false);
			String sNewProp = StrUtils.getNonEmptyStr(onSelectDismiss(sOldProp),false);
			if (!sNewProp.equals(sOldProp))
				mProperties.setProperty(property, sNewProp);
		});
		dlg.show();
	}

	public String onSelectDismiss(String propValue) {
		return propValue;
	}

	public OptionsDialog.Three OnPreClick (OptionsDialog.Three item ) {
		return item;
	}

	public void onClick( OptionsDialog.Three item ) {
		mProperties.setProperty(property, OnPreClick(item).value);
		refreshList();
		if (onChangeHandler != null)
			onChangeHandler.run();
		if (optionsListView != null)
			optionsListView.refresh();
	}
}
