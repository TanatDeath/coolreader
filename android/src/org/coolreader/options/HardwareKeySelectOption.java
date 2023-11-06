package org.coolreader.options;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import org.coolreader.R;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.utils.Utils;

import java.util.ArrayList;
import java.util.EnumSet;

public class HardwareKeySelectOption extends ListOption {

	private ArrayList<HardwareKeyInfo> hwKeys;

	private void addKey(HardwareKeySelectOption list, int keyCode, String keyName,
						EnumSet<OptionsDialog.KeyActionFlag> keyFl) {
		EnumSet<OptionsDialog.KeyActionFlag> keyFlags = keyFl;
		if (keyFlags == null) {
			keyFlags = EnumSet.allOf(OptionsDialog.KeyActionFlag.class);
		}
		if (keyFlags.contains(OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_NORMAL)) {
			final String propName = ReaderAction.getKeyProp(keyCode, ReaderAction.NORMAL);
			add(propName, keyName, "");
		}
		if (keyFlags.contains(OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_LONG)) {
			final String longPropName = ReaderAction.getKeyProp(keyCode, ReaderAction.LONG);
			add(longPropName, keyName  + " " + mActivity.getString(R.string.options_app_key_long_press), "");
		}
		if (keyFlags.contains(OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_DOUBLE)) {
			final String dblPropName = ReaderAction.getKeyProp(keyCode, ReaderAction.DOUBLE);
			add(dblPropName, keyName  + " " + mActivity.getString(R.string.options_app_key_double_press), "");
		}
	}

	public HardwareKeySelectOption(OptionOwner owner, String label, String property,
								   String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
		hwKeys = KeyMapOption.fillHWKeys(mProperties);
		addKey(this, 0, "NONE",
				EnumSet.of(OptionsDialog.KeyActionFlag.KEY_ACTION_FLAG_NORMAL));
		for (HardwareKeyInfo hwKey: hwKeys) {
			addKey(this, hwKey.keyCode, hwKey.keyName, hwKey.keyFlags);
		}
		if (mProperties.getProperty(property) == null)
			mProperties.setProperty(property,
				ReaderAction.getKeyProp(0, ReaderAction.NORMAL));
	}

	protected int getItemLayoutId() {
		return R.layout.option_value;
	}

	@SuppressLint("UseCompatLoadingForDrawables")
	protected void updateItemContents(final View layout, final OptionsDialog.Three item, final ListView listView, final int position) {
		super.updateItemContents(layout, item, listView, position);
		boolean iconIsSet = false;
		ImageView img = (ImageView) layout.findViewById(R.id.option_value_icon);
		if (hwKeys != null) {
			//ImageView imgAddInfo = (ImageView) layout.findViewById(R.id.btn_option_add_info);
			for (HardwareKeyInfo hwKey: hwKeys) {
				String propName = ReaderAction.getKeyProp(hwKey.keyCode, ReaderAction.NORMAL);
				String longPropName = ReaderAction.getKeyProp(hwKey.keyCode, ReaderAction.LONG);
				String dblPropName = ReaderAction.getKeyProp(hwKey.keyCode, ReaderAction.DOUBLE);
				if (item.value.equals(propName)) {
					int resId = 0;
					if (hwKey.drawableAttrId != 0) {
						resId = Utils.resolveResourceIdByAttr(mActivity,
							hwKey.drawableAttrId, hwKey.fallbackIconId);
					} else if (hwKey.fallbackIconId != 0) {
						resId = hwKey.fallbackIconId;
					}
					if (resId != 0) {
						img.setImageDrawable(mActivity.getResources().getDrawable(resId));
						mActivity.tintViewIcons(img, true);
						iconIsSet = true;
					}
				}
				if (item.value.equals(longPropName)) {
					int resId = 0;
					if (hwKey.drawableAttrId_long != 0) {
						resId = Utils.resolveResourceIdByAttr(mActivity,
								hwKey.drawableAttrId_long, hwKey.fallbackIconId_long);
					} else if (hwKey.fallbackIconId_long != 0) {
						resId = hwKey.fallbackIconId_long;
					}
					if (resId != 0) {
						img.setImageDrawable(mActivity.getResources().getDrawable(resId));
						mActivity.tintViewIcons(img, true);
						iconIsSet = true;
					}
				}
				if (item.value.equals(dblPropName)) {
					int resId = 0;
					if (hwKey.drawableAttrId_double != 0) {
						resId = Utils.resolveResourceIdByAttr(mActivity,
								hwKey.drawableAttrId_double, hwKey.fallbackIconId_double);
					} else if (hwKey.fallbackIconId_double != 0) {
						resId = hwKey.fallbackIconId_double;
					}
					if (resId != 0) {
						img.setImageDrawable(mActivity.getResources().getDrawable(resId));
						mActivity.tintViewIcons(img, true);
						iconIsSet = true;
					}
				}
			}
		}
		if (!iconIsSet) {
			img.setImageDrawable(mActivity.getResources().getDrawable(
					Utils.resolveResourceIdByAttr(mActivity,
							R.attr.attr_icons8_1, R.drawable.icons8_1)));
			mActivity.tintViewIcons(img, true);
		};
	}
}
