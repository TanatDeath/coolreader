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
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ResizeHistory;
import org.coolreader.crengine.Settings;
import org.coolreader.dic.Dictionaries;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.util.List;

public class DicMultiListOption extends SubmenuOption {

	final Context mContext;

	public DicMultiListOption(Context context, OptionOwner owner, String label, String addInfo, String filter) {
		super(owner, label, Settings.PROP_DIC_LIST_MULTI, addInfo,filter);
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("DicMultiListOption", mActivity, label, false, false);
		View view = mInflater.inflate(R.layout.searchable_listview, null);
		LinearLayout viewList = view.findViewById(R.id.lv_list);
		final EditText tvSearchText = view.findViewById(R.id.search_text);
		ImageButton ibSearch = view.findViewById(R.id.btn_search);
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
		List<Dictionaries.DictInfo> dicts = Dictionaries.getDictList(mActivity);
		for (Dictionaries.DictInfo dict : dicts) {
			boolean installed = mActivity.isPackageInstalled(dict.packageName) || StrUtils.isEmptyStr(dict.packageName);
			String sAdd = mActivity.getString(R.string.options_app_dictionary_not_installed);
			String sAdd2 = dict.getAddText(mActivity);
			if (!StrUtils.isEmptyStr(sAdd2)) sAdd2 = ": " + sAdd2;
			if (StrUtils.isEmptyStr(dict.packageName)) sAdd = "";
			if (((dict.internal==1)||(dict.internal==6)) &&
					(dict.packageName.equals("com.socialnmobile.colordict")) && (!installed)) {
				installed = mActivity.isPackageInstalled("mobi.goldendict.android")||
						mActivity.isPackageInstalled("mobi.goldendict.androie")
						|| StrUtils.isEmptyStr(dict.packageName); // changed package name - 4pda version 2.0.1b7
				String sMinicard="";
				if (dict.internal==6) sMinicard=" (minicard)";
				String sInfo = "";
				if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
					sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
				else sInfo = "Link: " + dict.httpLink;
				listView.add(new BoolOption(mOwner, (installed ? "GoldenDict" + sMinicard: dict.name + sAdd2 + " " + sAdd),
						Settings.PROP_DIC_LIST_MULTI+"."+dict.id, sInfo, this.lastFilteredValue, false).setDefaultValue("0").
						setIconId(0));
			} else {
				String sInfo = "";
				if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
					sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
				else sInfo = "Link: " + dict.httpLink;
				listView.add(new BoolOption(mOwner, dict.name + sAdd2 + (installed ? "" : " " + sAdd),
						Settings.PROP_DIC_LIST_MULTI+"."+dict.id, sInfo, this.lastFilteredValue, false).setDefaultValue("0").
						setIconId(0));
			}
		}

		viewList.addView(listView);
		ibSearch.setOnClickListener(v -> {
			tvSearchText.setText("");
			listView.listUpdated("");
		});
		dlg.setView(view);
		ibSearch.requestFocus();
		dlg.show();
	}

	public boolean updateFilterEnd() {
		List<Dictionaries.DictInfo> dicts = Dictionaries.getDictList(mActivity);
		for (Dictionaries.DictInfo dict : dicts) {
			boolean installed = mActivity.isPackageInstalled(dict.packageName) || StrUtils.isEmptyStr(dict.packageName);
			String sAdd = mActivity.getString(R.string.options_app_dictionary_not_installed);
			String sAdd2 = dict.getAddText(mActivity);
			if (!StrUtils.isEmptyStr(sAdd2)) sAdd2 = ": " + sAdd2;
			if (StrUtils.isEmptyStr(dict.packageName)) sAdd = "";
			if (((dict.internal==1)||(dict.internal==6)) &&
					(dict.packageName.equals("com.socialnmobile.colordict")) && (!installed)) {
				installed = mActivity.isPackageInstalled("mobi.goldendict.android")||
						mActivity.isPackageInstalled("mobi.goldendict.androie")
						|| StrUtils.isEmptyStr(dict.packageName); // changed package name - 4pda version 2.0.1b7
				String sMinicard="";
				if (dict.internal==6) sMinicard=" (minicard)";
				String sInfo = "";
				if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
					sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
				else sInfo = "Link: " + dict.httpLink;
				this.updateFilteredMark(sInfo);
				this.updateFilteredMark(Settings.PROP_DIC_LIST_MULTI+"."+dict.id);
				this.updateFilteredMark((installed ? "GoldenDict" + sMinicard: dict.name + sAdd2 + " " + sAdd));
			} else {
				String sInfo = "";
				if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
					sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
				else sInfo = "Link: " + dict.httpLink;
				this.updateFilteredMark(sInfo);
				this.updateFilteredMark(Settings.PROP_DIC_LIST_MULTI+"."+dict.id);
				this.updateFilteredMark(dict.name + sAdd2 + (installed ? "" : " " + sAdd));
			}
		}
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
