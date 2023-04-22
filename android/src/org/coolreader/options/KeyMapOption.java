package org.coolreader.options;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.readerview.ReaderView;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.util.EnumSet;

public class KeyMapOption extends SubmenuOption {

	final BaseActivity mActivity;
	final Context mContext;
	private ListView listView;

	public KeyMapOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_APP_KEY_ACTIONS_PRESS, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}
	private void addKey( OptionsListView list, int keyCode, String keyName) {
		addKey(list, keyCode, keyName, EnumSet.allOf(OptionsDialog.KeyActionFlag.class),
				0, 0,
				0, 0,
				0, 0);
	}
	private void addKey( OptionsListView list, int keyCode, String keyName,
						 int drawableAttrId, int fallbackIconId,
						 int drawableAttrId_long, int fallbackIconId_long,
						 int drawableAttrId_double, int fallbackIconId_double) {
		addKey(list, keyCode, keyName, EnumSet.allOf(OptionsDialog.KeyActionFlag.class),
				drawableAttrId, fallbackIconId,
				drawableAttrId_long, fallbackIconId_long,
				drawableAttrId_double, fallbackIconId_double);
	}

	private void addKey(OptionsListView list, int keyCode, String keyName, EnumSet<OptionsDialog.KeyActionFlag> keyFlags) {
		addKey(list, keyCode, keyName, keyFlags, 0, 0, 0, 0, 0, 0);
	}

	private void addKey(OptionsListView list, int keyCode, String keyName, EnumSet<OptionsDialog.KeyActionFlag> keyFlags,
						 int drawableAttrId, int fallbackIconId,
						 int drawableAttrId_long, int fallbackIconId_long,
						 int drawableAttrId_double, int fallbackIconId_double) {
		if (keyFlags.contains(OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_NORMAL)) {
			final String propName = ReaderAction.getKeyProp(keyCode, ReaderAction.NORMAL);
			OptionBase ac = new ActionOption(mOwner, keyName, propName, false, false,
					mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
			list.add(ac.setIconIdByAttr(drawableAttrId, fallbackIconId));
		}
		if (keyFlags.contains(OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_LONG)) {
			final String longPropName = ReaderAction.getKeyProp(keyCode, ReaderAction.LONG);
			OptionBase ac = new ActionOption(mOwner, keyName + " " + mContext.getString(R.string.options_app_key_long_press),
					longPropName, false, true, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
			list.add(ac.setIconIdByAttr(drawableAttrId_long, fallbackIconId_long));
		}
		if (keyFlags.contains(OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_DOUBLE)) {
			final String dblPropName = ReaderAction.getKeyProp(keyCode, ReaderAction.DOUBLE);
			OptionBase ac = new ActionOption(mOwner, keyName + " " + mContext.getString(R.string.options_app_key_double_press),
					dblPropName, false, false, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
			list.add(ac.setIconIdByAttr(drawableAttrId_double, fallbackIconId_double));
		}
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("KeyMapDialog", mActivity, label, false, false);
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

		if (DeviceInfo.NOOK_NAVIGATION_KEYS) {
			addKey(listView, ReaderView.KEYCODE_PAGE_TOPLEFT, "Top left navigation button");
			addKey(listView, ReaderView.KEYCODE_PAGE_BOTTOMLEFT, "Bottom left navigation button");
			addKey(listView, ReaderView.KEYCODE_PAGE_TOPRIGHT, "Top right navigation button");
			addKey(listView, ReaderView.NOOK_12_KEY_NEXT_LEFT, "Bottom right navigation button");
//				addKey(listView, ReaderView.KEYCODE_PAGE_BOTTOMRIGHT, "Bottom right navigation button");

			// on rooted Nook, side navigation keys may be reassigned on some standard android keycode
			addKey(listView, KeyEvent.KEYCODE_MENU, "Menu",
					R.attr.attr_icons8_menu_key, R.drawable.icons8_menu_key,
					R.attr.attr_icons8_menu_key_long, R.drawable.icons8_menu_key_long,
					R.attr.attr_icons8_menu_key_double, R.drawable.icons8_menu_key_double
			);
			addKey(listView, KeyEvent.KEYCODE_BACK, "Back",
					R.attr.attr_icons8_back_key, R.drawable.icons8_back_key,
					R.attr.attr_icons8_back_key_long, R.drawable.icons8_back_key_long,
					R.attr.attr_icons8_back_key_double, R.drawable.icons8_back_key_double
			);
			addKey(listView, KeyEvent.KEYCODE_SEARCH, "Search",
					R.attr.attr_icons8_search_key, R.drawable.icons8_search_key,
					R.attr.attr_icons8_search_key_long, R.drawable.icons8_search_key_long,
					R.attr.attr_icons8_search_key_double, R.drawable.icons8_search_key_double);

			addKey(listView, KeyEvent.KEYCODE_HOME, "Home");

			addKey(listView, KeyEvent.KEYCODE_2, "Up",
					R.attr.attr_icons8_up_key, R.drawable.icons8_up_key,
					R.attr.attr_icons8_up_key_long, R.drawable.icons8_up_key_long,
					R.attr.attr_icons8_up_key_double, R.drawable.icons8_up_key_double);
			addKey(listView, KeyEvent.KEYCODE_8, "Down",
					R.attr.attr_icons8_down_key, R.drawable.icons8_down_key,
					R.attr.attr_icons8_down_key_long, R.drawable.icons8_down_key_long,
					R.attr.attr_icons8_down_key_double, R.drawable.icons8_down_key_double);
		} else if (DeviceInfo.SONY_NAVIGATION_KEYS) {
//				addKey(listView, KeyEvent.KEYCODE_DPAD_UP, "Prev button");
//				addKey(listView, KeyEvent.KEYCODE_DPAD_DOWN, "Next button");
			addKey(listView, ReaderView.SONY_DPAD_UP_SCANCODE, "Prev button");
			addKey(listView, ReaderView.SONY_DPAD_DOWN_SCANCODE, "Next button");
			addKey(listView, ReaderView.SONY_DPAD_LEFT_SCANCODE, "Left button",
					R.attr.attr_icons8_left_key, R.drawable.icons8_left_key,
					R.attr.attr_icons8_left_key_long, R.drawable.icons8_left_key_long,
					R.attr.attr_icons8_left_key_double, R.drawable.icons8_left_key_double);
			addKey(listView, ReaderView.SONY_DPAD_RIGHT_SCANCODE, "Right button",
					R.attr.attr_icons8_right_key, R.drawable.icons8_right_key,
					R.attr.attr_icons8_right_key_long, R.drawable.icons8_right_key_long,
					R.attr.attr_icons8_right_key_double, R.drawable.icons8_right_key_double);
//				addKey(listView, ReaderView.SONY_MENU_SCANCODE, "Menu");
//				addKey(listView, ReaderView.SONY_BACK_SCANCODE, "Back");
//				addKey(listView, ReaderView.SONY_HOME_SCANCODE, "Home");
			addKey(listView, KeyEvent.KEYCODE_MENU, "Menu",
					R.attr.attr_icons8_menu_key, R.drawable.icons8_menu_key,
					R.attr.attr_icons8_menu_key_long, R.drawable.icons8_menu_key_long,
					R.attr.attr_icons8_menu_key_double, R.drawable.icons8_menu_key_double
			);
			addKey(listView, KeyEvent.KEYCODE_BACK, "Back",
					R.attr.attr_icons8_back_key, R.drawable.icons8_back_key,
					R.attr.attr_icons8_back_key_long, R.drawable.icons8_back_key_long,
					R.attr.attr_icons8_back_key_double, R.drawable.icons8_back_key_double
			);
			addKey(listView, KeyEvent.KEYCODE_HOME, "Home");
		} else {
			EnumSet<OptionsDialog.KeyActionFlag> keyFlags;
			if (DeviceInfo.EINK_ONYX && DeviceInfo.ONYX_BUTTONS_LONG_PRESS_NOT_AVAILABLE) {
				keyFlags = EnumSet.of(
						OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_NORMAL,
						OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_DOUBLE
				);
			} else {
				keyFlags = EnumSet.allOf(OptionsDialog.KeyActionFlag.class);
			}

			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_MENU))
				addKey(listView, KeyEvent.KEYCODE_MENU, "Menu", keyFlags,
						R.attr.attr_icons8_menu_key, R.drawable.icons8_menu_key,
						R.attr.attr_icons8_menu_key_long, R.drawable.icons8_menu_key_long,
						R.attr.attr_icons8_menu_key_double, R.drawable.icons8_menu_key_double
				);
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK))
				addKey(listView, KeyEvent.KEYCODE_BACK, "Back", keyFlags,
						R.attr.attr_icons8_back_key, R.drawable.icons8_back_key,
						R.attr.attr_icons8_back_key_long, R.drawable.icons8_back_key_long,
						R.attr.attr_icons8_back_key_double, R.drawable.icons8_back_key_double
				);
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_LEFT))
				addKey(listView, KeyEvent.KEYCODE_DPAD_LEFT, "Left", keyFlags,
						R.attr.attr_icons8_left_key, R.drawable.icons8_left_key,
						R.attr.attr_icons8_left_key_long, R.drawable.icons8_left_key_long,
						R.attr.attr_icons8_left_key_double, R.drawable.icons8_left_key_double);
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_RIGHT))
				addKey(listView, KeyEvent.KEYCODE_DPAD_RIGHT, "Right", keyFlags,
						R.attr.attr_icons8_right_key, R.drawable.icons8_right_key,
						R.attr.attr_icons8_right_key_long, R.drawable.icons8_right_key_long,
						R.attr.attr_icons8_right_key_double, R.drawable.icons8_right_key_double);
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_UP))
				addKey(listView, KeyEvent.KEYCODE_DPAD_UP, "Up", keyFlags,
						R.attr.attr_icons8_up_key, R.drawable.icons8_up_key,
						R.attr.attr_icons8_up_key_long, R.drawable.icons8_up_key_long,
						R.attr.attr_icons8_up_key_double, R.drawable.icons8_up_key_double
				);
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_DOWN))
				addKey(listView, KeyEvent.KEYCODE_DPAD_DOWN, "Down", keyFlags,
						R.attr.attr_icons8_down_key, R.drawable.icons8_down_key,
						R.attr.attr_icons8_down_key_long, R.drawable.icons8_down_key_long,
						R.attr.attr_icons8_down_key_double, R.drawable.icons8_down_key_double
				);
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_CENTER))
				addKey(listView, KeyEvent.KEYCODE_DPAD_CENTER, "Center", keyFlags);
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_SEARCH))
				addKey(listView, KeyEvent.KEYCODE_SEARCH, "Search", keyFlags,
						R.attr.attr_icons8_search_key, R.drawable.icons8_search_key,
						R.attr.attr_icons8_search_key_long, R.drawable.icons8_search_key_long,
						R.attr.attr_icons8_search_key_double, R.drawable.icons8_search_key_double
				);
			if (DeviceInfo.EINK_ONYX) {
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_UP))
					addKey(listView, KeyEvent.KEYCODE_VOLUME_UP, "Left Side Button (Volume Up)", keyFlags,
							R.attr.attr_icons8_volume_up_key, R.drawable.icons8_volume_up_key,
							R.attr.attr_icons8_volume_up_key_long, R.drawable.icons8_volume_up_key_long,
							R.attr.attr_icons8_volume_up_key_double, R.drawable.icons8_volume_up_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN))
					addKey(listView, KeyEvent.KEYCODE_VOLUME_DOWN, "Right Side Button (Volume Down)", keyFlags,
							R.attr.attr_icons8_volume_down_key, R.drawable.icons8_volume_down_key,
							R.attr.attr_icons8_volume_down_key_long, R.drawable.icons8_volume_down_key_long,
							R.attr.attr_icons8_volume_down_key_double, R.drawable.icons8_volume_down_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_UP))
					addKey(listView, KeyEvent.KEYCODE_PAGE_UP, "Left Side Button", keyFlags,
							R.attr.attr_icons8_page_up_key, R.drawable.icons8_page_up_key,
							R.attr.attr_icons8_page_up_key_long, R.drawable.icons8_page_up_key_long,
							R.attr.attr_icons8_page_up_key_double, R.drawable.icons8_page_up_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_DOWN))
					addKey(listView, KeyEvent.KEYCODE_PAGE_DOWN, "Right Side Button", keyFlags,
							R.attr.attr_icons8_page_down_key, R.drawable.icons8_page_down_key,
							R.attr.attr_icons8_page_down_key_long, R.drawable.icons8_page_down_key_long,
							R.attr.attr_icons8_page_down_key_double, R.drawable.icons8_page_down_key_double
					);
			}
			else {
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_UP))
					addKey(listView, KeyEvent.KEYCODE_VOLUME_UP, "Volume Up",
							R.attr.attr_icons8_volume_up_key, R.drawable.icons8_volume_up_key,
							R.attr.attr_icons8_volume_up_key_long, R.drawable.icons8_volume_up_key_long,
							R.attr.attr_icons8_volume_up_key_double, R.drawable.icons8_volume_up_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN))
					addKey(listView, KeyEvent.KEYCODE_VOLUME_DOWN, "Volume Down",
							R.attr.attr_icons8_volume_down_key, R.drawable.icons8_volume_down_key,
							R.attr.attr_icons8_volume_down_key_long, R.drawable.icons8_volume_down_key_long,
							R.attr.attr_icons8_volume_down_key_double, R.drawable.icons8_volume_down_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_UP))
					addKey(listView, KeyEvent.KEYCODE_PAGE_UP, "Page Up",
							R.attr.attr_icons8_page_up_key, R.drawable.icons8_page_up_key,
							R.attr.attr_icons8_page_up_key_long, R.drawable.icons8_page_up_key_long,
							R.attr.attr_icons8_page_up_key_double, R.drawable.icons8_page_up_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_DOWN))
					addKey(listView, KeyEvent.KEYCODE_PAGE_DOWN, "Page Down",
							R.attr.attr_icons8_page_down_key, R.drawable.icons8_page_down_key,
							R.attr.attr_icons8_page_down_key_long, R.drawable.icons8_page_down_key_long,
							R.attr.attr_icons8_page_down_key_double, R.drawable.icons8_page_down_key_double
					);
			}
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_CAMERA))
				addKey(listView, KeyEvent.KEYCODE_CAMERA, "Camera", keyFlags,
						R.attr.attr_icons8_camera_key, R.drawable.icons8_camera_key,
						R.attr.attr_icons8_camera_key_long, R.drawable.icons8_camera_key_long,
						R.attr.attr_icons8_camera_key_double, R.drawable.icons8_camera_key_double
				);
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_ESCAPE))
				addKey(listView, ReaderView.KEYCODE_ESCAPE, "Escape", keyFlags,
						R.attr.attr_icons8_esc_key, R.drawable.icons8_esc_key,
						R.attr.attr_icons8_esc_key_long, R.drawable.icons8_esc_key_long,
						R.attr.attr_icons8_esc_key_double, R.drawable.icons8_esc_key_double
				);
			addKey(listView, KeyEvent.KEYCODE_HEADSETHOOK, "Headset Hook",
					R.attr.attr_icons8_headset_key, R.drawable.icons8_headset_key,
					R.attr.attr_icons8_headset_key_long, R.drawable.icons8_headset_key_long,
					R.attr.attr_icons8_headset_key_double, R.drawable.icons8_headset_key_double
			);
		}
		String keys = StrUtils.getNonEmptyStr(mProperties.getProperty(Settings.PROP_APP_HARDWARE_KEYS), true);
		for (String k: keys.split("\\|"))
			if (!StrUtils.isEmptyStr(k))
				addKey(listView,
						-Integer.valueOf(StrUtils.getNonEmptyStr(k.split(",")[0], true)),
						StrUtils.getNonEmptyStr(k.split(",")[1], true),
						R.attr.attr_icons8_1, R.drawable.icons8_1,
						R.attr.attr_icons8_1, R.drawable.icons8_1,
						R.attr.attr_icons8_1, R.drawable.icons8_1);
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
		if (DeviceInfo.NOOK_NAVIGATION_KEYS) {
			this.updateFilteredMark("Top left navigation button");
			this.updateFilteredMark("Bottom left navigation button");
			this.updateFilteredMark("Top right navigation button");
			this.updateFilteredMark("Bottom right navigation button");

			// on rooted Nook, side navigation keys may be reassigned on some standard android keycode
			this.updateFilteredMark( "Menu");
			this.updateFilteredMark("Back");
			this.updateFilteredMark("Search");
			this.updateFilteredMark("Home");
			this.updateFilteredMark( "Up");
			this.updateFilteredMark("Down");
		} else if (DeviceInfo.SONY_NAVIGATION_KEYS) {
			this.updateFilteredMark("Prev button");
			this.updateFilteredMark("Next button");
			this.updateFilteredMark("Left button");
			this.updateFilteredMark("Right button");
			this.updateFilteredMark( "Menu");
			this.updateFilteredMark("Back");
			this.updateFilteredMark("Home");
		} else {
			this.updateFilteredMark("Menu");
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK))
				this.updateFilteredMark("Back");
			this.updateFilteredMark("Left");
			this.updateFilteredMark("Right");
			this.updateFilteredMark("Up");
			this.updateFilteredMark("Down");
			this.updateFilteredMark("Center");
			this.updateFilteredMark("Search");
			if (DeviceInfo.EINK_ONYX) {
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_UP))
					this.updateFilteredMark("Left Side Button (Volume Up)");
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN))
					this.updateFilteredMark("Right Side Button (Volume Down)");
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_UP))
					this.updateFilteredMark("Left Side Button");
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_DOWN))
					this.updateFilteredMark("Right Side Button");
			}
			else {
				this.updateFilteredMark("Volume Up");
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN))
					this.updateFilteredMark("Volume Down");
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_UP))
					this.updateFilteredMark("Page Up");
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_DOWN))
					this.updateFilteredMark("Page Down");
			}
			this.updateFilteredMark("Camera");
			if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_ESCAPE))
				this.updateFilteredMark("Escape");
			this.updateFilteredMark("Headset Hook");
		}
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
