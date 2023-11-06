package org.coolreader.options;

import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TextView;

import org.coolreader.R;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.Settings;
import org.coolreader.layouts.FlowLayout;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReaderToolbarOption extends SubmenuOption implements TabHost.TabContentFactory, ActionClickedCallback {

	private FlowLayout mFlToolbar;
	private FlowLayout mFlMenu;
	private FlowLayout mFlGroup;
	ArrayList<Button> btnGroupNums = new ArrayList<>();
	private int curGroupNum = 1;


	public static final int[] mToolbarButtons = new int[] {
			0, 4, 5, 6, 1, 2, 3, 7, 8, 9, 10, 11, 12
	};
	public static final int[] mToolbarButtonsTitles = new int[] {
			R.string.option_toolbar_buttons_none,
			R.string.option_toolbar_buttons_toolbar_1st,
			R.string.option_toolbar_buttons_more_1st,
			R.string.option_toolbar_buttons_both_1st,
			R.string.option_toolbar_buttons_toolbar,
			R.string.option_toolbar_buttons_more,
			R.string.option_toolbar_buttons_both,
			R.string.option_toolbar_buttons_toolbar_3rd,
			R.string.option_toolbar_buttons_more_3rd,
			R.string.option_toolbar_buttons_both_3rd,
			R.string.option_toolbar_buttons_toolbar_4th,
			R.string.option_toolbar_buttons_more_4th,
			R.string.option_toolbar_buttons_both_4th
	};

	public static final int[] mToolbarAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	public ReaderToolbarOption(OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_TOOLBAR_BUTTONS, addInfo, filter);
	}

	private void addAction(OptionsListView list, ReaderAction action) {

		boolean bIsDef = false;

		for (ReaderAction act: ReaderAction.getDefReaderActions())
			if (act.cmd.nativeId == action.cmd.nativeId) {
				bIsDef = true;
				break;
			}

		String lab1 = action.getNameText(mActivity);
		int mirrIcon = 0;
		if (action.getMirrorAction()!=null) {
			lab1 = lab1 + "~" + mActivity.getString(R.string.long_tap) + ": " + action.getMirrorAction().getNameText(mActivity);
			mirrIcon = action.getMirrorAction().iconId;
		}
		OptionBase toolbopt = new ActionOptionExt(this, mOwner,
				lab1, Settings.PROP_TOOLBAR_BUTTONS+"."
				+ action.cmd.nativeId +"."+ action.param,
				mActivity.getString(action.addInfoR), this.lastFilteredValue)
				.add(mToolbarButtons, mToolbarButtonsTitles, mToolbarAddInfos).setDefaultValue(
						bIsDef ?  Integer.toString(mToolbarButtons[3]) : Integer.toString(mToolbarButtons[0])).
						setIconId(action.iconId).setIcon2Id(mirrIcon);
		list.add(toolbopt);
	}

	private void addTab(TabHost tabs, String name) {
		TabHost.TabSpec ts = tabs.newTabSpec(name);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
			// replace too small icons in tabs in Theme.Holo
			View tabIndicator = mInflater.inflate(R.layout.tab_text_indicator, null);
			TextView tv = tabIndicator.findViewById(R.id.tab_text);
			tv.setText(name);
			ts.setIndicator(tabIndicator);
		} else {
			ts.setIndicator("name", null);
		}
		ts.setContent(this);
		tabs.addTab(ts);
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("ReaderToolbarDialog", mActivity, label, false, false);
		TabHost tabs = (TabHost) mInflater.inflate(R.layout.toolbar_menu_buttons, null);
		tabs.setup();
		addTab(tabs, mActivity.getString(R.string.all_actions));
		addTab(tabs, mActivity.getString(R.string.toolbar_actions));
		addTab(tabs, mActivity.getString(R.string.menu_actions));
		addTab(tabs, mActivity.getString(R.string.group_actions));
		tabs.invalidate();
		tabs.setCurrentTab(0);
		dlg.setView(tabs);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		List<ReaderAction> actions = ReaderAction.getAvailActions(true);

		for (ReaderAction a : actions)
			if (
				((a != ReaderAction.NONE) && (a != ReaderAction.EXIT) && (a != ReaderAction.ABOUT))
			) {
				String lab1 = a.getNameText(mActivity);
				this.updateFilteredMark(lab1);
				this.updateFilteredMark(Settings.PROP_TOOLBAR_BUTTONS+"."
						+String.valueOf(a.cmd.nativeId)+"."+String.valueOf(a.param));
				this.updateFilteredMark(mActivity.getString(a.addInfoR));
			}
		for (int i: mToolbarButtonsTitles) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mToolbarAddInfos) this.updateFilteredMark(mActivity.getString(i));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }

	public enum ForWhat {
		forMenu,
		forToolbar,
		forGroup
	}

	private void groupButtonClick() {
		int i = 0;
		int colorGray = themeColors.get(R.attr.colorThemeGray2);
		for (Button btn: btnGroupNums) {
			i++;
			if (i == curGroupNum) {
				btn.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
				if (isEInk) Utils.setSolidButtonEink(btn);
			}
			else {
				btn.setBackgroundColor(themeColors.get(R.attr.colorThemeGray2Contrast));
				if (isEInk) btn.setBackgroundColor(Color.WHITE);
			}
		}
	}

	@Override
	public View createTabContent(String tag) {
		if (tag.equals(mActivity.getString(R.string.all_actions))) {
			View view = mInflater.inflate(R.layout.searchable_listview, null);
			LinearLayout viewList = view.findViewById(R.id.lv_list);
			final EditText tvSearchText = view.findViewById(R.id.search_text);

			LinearLayout llQF = view.findViewById(R.id.ll_quick_filters);

			int colorIcon = themeColors.get(R.attr.colorIcon);

			((ViewGroup) llQF.getParent()).removeView(llQF);

			ImageButton ibSearch = view.findViewById(R.id.btn_search);
			final OptionsListView listView = new OptionsListView(mActivity, this);
			tvSearchText.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
					listView.listUpdated(cs.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				}

				@Override
				public void afterTextChanged(Editable arg0) {
				}
			});

			int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
			int colorGrayCT = Color.argb(128, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC));
			tvSearchText.setBackgroundColor(colorGrayCT);
			tvSearchText.setTextColor(mActivity.getTextColor(colorIcon));
			int colorIcon128 = Color.argb(128, Color.red(colorIcon), Color.green(colorIcon), Color.blue(colorIcon));
			tvSearchText.setHintTextColor(colorIcon128);
			if (isEInk) Utils.setSolidEditEink(tvSearchText);

			List<ReaderAction> actions = ReaderAction.getAvailActions(true);

			for (ReaderAction a : actions)
				// decided to add all actions due to allow sort their priority
				if (
						(a != ReaderAction.NONE) //&& (a != ReaderAction.EXIT) && (a != ReaderAction.ABOUT))
				)
					addAction(listView, a);

			viewList.addView(listView);
			ibSearch.setOnClickListener(v -> {
				tvSearchText.setText("");
				listView.listUpdated("");
			});
			ibSearch.requestFocus();
			return view;
		} else {
			ForWhat forWhat = ForWhat.forToolbar;
			if (tag.equals(mActivity.getString(R.string.menu_actions))) forWhat = ForWhat.forMenu;
			if (tag.equals(mActivity.getString(R.string.group_actions))) forWhat = ForWhat.forGroup;
			LinearLayout ll = (LinearLayout) mInflater.inflate(R.layout.toolbar_menu_buttons_fl, null);
			FlowLayout flToolbarMenu = ll.findViewById(R.id.fl_toolbar_menu_buttons);
			if (forWhat == ForWhat.forToolbar) mFlToolbar = flToolbarMenu;
			if (forWhat == ForWhat.forMenu) mFlMenu = flToolbarMenu;
			if (forWhat == ForWhat.forGroup) mFlGroup = flToolbarMenu;
			int colorGray = themeColors.get(R.attr.colorThemeGray2);
			TableLayout tlNums = ll.findViewById(R.id.group_num);
			if (forWhat == ForWhat.forGroup) {
				btnGroupNums.clear();
				Button btnGroupNum_1 = ll.findViewById(R.id.group_num_1);
				btnGroupNums.add(btnGroupNum_1);
				Button btnGroupNum_2 = ll.findViewById(R.id.group_num_2);
				btnGroupNums.add(btnGroupNum_2);
				Button btnGroupNum_3 = ll.findViewById(R.id.group_num_3);
				btnGroupNums.add(btnGroupNum_3);
				Button btnGroupNum_4 = ll.findViewById(R.id.group_num_4);
				btnGroupNums.add(btnGroupNum_4);
				Button btnGroupNum_5 = ll.findViewById(R.id.group_num_5);
				btnGroupNums.add(btnGroupNum_5);
				Button btnGroupNum_6 = ll.findViewById(R.id.group_num_6);
				btnGroupNums.add(btnGroupNum_6);
				Button btnGroupNum_7 = ll.findViewById(R.id.group_num_7);
				btnGroupNums.add(btnGroupNum_7);
				Button btnGroupNum_8 = ll.findViewById(R.id.group_num_8);
				btnGroupNums.add(btnGroupNum_8);
				Button btnGroupNum_9 = ll.findViewById(R.id.group_num_9);
				btnGroupNums.add(btnGroupNum_9);
				Button btnGroupNum_10 = ll.findViewById(R.id.group_num_10);
				btnGroupNums.add(btnGroupNum_10);
				int i = 0;
				for (Button btn: btnGroupNums) {
					i++;
					int finalI = i;
					btn.setOnClickListener(v -> {
						curGroupNum = finalI;
						groupButtonClick();
						updateButtonsView(ForWhat.forGroup);
					});
					groupButtonClick();
				}
			} else {
				Utils.removeView(tlNums);
			}
			updateButtonsView(forWhat);
			Button btnDef = ll.findViewById(R.id.btn_def);
			btnDef.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
			if (isEInk) Utils.setSolidButtonEink(btnDef);
			ForWhat finalForWhat = forWhat;
			btnDef.setOnClickListener(v -> {
				addButton("", finalForWhat, 6);
				updateButtonsView(finalForWhat);
			});
			if (forWhat == ForWhat.forGroup) Utils.removeView(btnDef);
			Button btnClear = ll.findViewById(R.id.btn_clear);
			btnClear.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
			btnClear.setOnClickListener(v -> {
				addButton("", finalForWhat, 5);
				updateButtonsView(finalForWhat);
			});
			if (isEInk) Utils.setSolidButtonEink(btnClear);
			return ll;
		}
	}

	private void addButton(String btnId, ForWhat forWhat, int loc) {
		// loc - 0 = в конец, 1 = в начало, 2 = влево, 3 = вправо, 4 = удалить, 5 = очистить, 6 = добавить по умолчанию
		String sSett = Settings.PROP_TOOLBAR_BUTTONS;
		if (forWhat == ForWhat.forMenu) sSett = Settings.PROP_READING_MENU_BUTTONS;
		if (forWhat == ForWhat.forGroup) sSett = Settings.PROP_GROUP_BUTTONS + "_" + curGroupNum;
		if (loc == 5) {
			mProperties.setProperty(sSett, "");
			return;
		}
		if (loc == 6) {
			String res = "";
			for (ReaderAction ra: ReaderAction.getDefReaderActions())
				if (res.isEmpty()) res = ra.cmd.nativeId +"."+ ra.param;
				else res = res + "," + ra.cmd.nativeId +"."+ ra.param;
			mProperties.setProperty(sSett, res);
			return;
		}
		String sProp = mProperties.getProperty(sSett);
		sProp = StrUtils.getNonEmptyStr(sProp, true);
		int pos = -1;
		String[] props = sProp.split(",");
		for (int i=0; i<props.length; i++)
			if (props[i].equals(btnId)) {
				pos = i;
				break;
			}
		ArrayList<String> propsA = new ArrayList<>(Arrays.asList(props));
		if (pos == -1) propsA.add(btnId);
		else {
			if (loc == 4)
				propsA.remove(pos);
			if (loc == 0) {
				propsA.remove(pos);
				propsA.add(btnId);
			}
			if (loc == 1) {
				propsA.remove(pos);
				propsA.add(0, btnId);
			}
			if ((loc == 2) && (pos>0)) {
				propsA.remove(pos);
				propsA.add(pos-1, btnId);
			}
			if ((loc == 3) && (pos < propsA.size()-1)) {
				propsA.remove(pos);
				propsA.add(pos+1, btnId);
			}
		}
		String res = "";
		for (String p: propsA)
			if (res.isEmpty()) res = p; else res = res + "," + p;
		mProperties.setProperty(sSett, res);
	}

	private void updateButtonsView(ForWhat forWhat) {
		String sProp = mProperties.getProperty(Settings.PROP_TOOLBAR_BUTTONS);
		FlowLayout fl = mFlToolbar;
		if (forWhat == ForWhat.forMenu) {
			sProp = mProperties.getProperty(Settings.PROP_READING_MENU_BUTTONS);
			fl = mFlMenu;
		}
		if (forWhat == ForWhat.forGroup) {
			sProp = mProperties.getProperty(Settings.PROP_GROUP_BUTTONS + "_" + curGroupNum);
			fl = mFlGroup;
		}
		if (fl == null) return;
		fl.removeAllViews();
		sProp = StrUtils.getNonEmptyStr(sProp, true);
		List<ReaderAction> actions = ReaderAction.getAvailActions(true);
		for (String btn: sProp.split(",")) {
			for (ReaderAction ra: actions) {
				if ((ra.cmd.nativeId +"."+ ra.param).equals(btn)) {
					View view = mInflater.inflate(R.layout.toolbar_button_item, null);
					TextView tv = view.findViewById(R.id.tv_middle);
					String title = ra.getNameText(mActivity);
					tv.setText(title);
					final String btnId = ra.cmd.nativeId +"."+ ra.param;
					ImageView iv = view.findViewById(R.id.iv_middle);
					iv.setOnClickListener(v -> {
						addButton(btnId, forWhat, 4);
						updateButtonsView(forWhat);
					});

					LinearLayout llAction = view.findViewById(R.id.ll_action);
					//TableLayout tlAction = view.findViewById(R.id.tl_action);
					int colorGray = themeColors.get(R.attr.colorThemeGray2);
					llAction.setBackgroundColor(Color.argb(70, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));

					ra.setupIconView(mActivity, iv);
					ImageButton btnLeft = view.findViewById(R.id.btn_left);
					btnLeft.setOnClickListener(v -> {
						addButton(btnId, forWhat, 2);
						updateButtonsView(forWhat);
					});
					ImageButton btnRight = view.findViewById(R.id.btn_right);
					btnRight.setOnClickListener(v -> {
						addButton(btnId, forWhat, 3);
						updateButtonsView(forWhat);
					});
					ImageButton btnUp = view.findViewById(R.id.btn_up);
					btnUp.setOnClickListener(v -> {
						addButton(btnId, forWhat, 1);
						updateButtonsView(forWhat);
					});
					ImageButton btnDown = view.findViewById(R.id.btn_down);
					btnDown.setOnClickListener(v -> {
						addButton(btnId, forWhat, 0);
						updateButtonsView(forWhat);
					});
					mActivity.tintViewIcons(view);
					fl.addView(view);
					break;
				}
			}
		}
	}

	@Override
	public void onActionClick(ActionOptionExt actionOption, ForWhat forWhat) {
		mActivity.showToast(R.string.value_saved);
		String btn = actionOption.property.replace(Settings.PROP_TOOLBAR_BUTTONS+".", "");
		addButton(btn, forWhat, 0);
		updateButtonsView(forWhat);
	}
}
