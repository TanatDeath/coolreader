package org.coolreader.options;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.eink.EinkScreen;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class OptionBase {

	public final static int OPTION_VIEW_TYPE_NORMAL = 0;
	public final static int OPTION_VIEW_TYPE_BOOLEAN = 1;
	public final static int OPTION_VIEW_TYPE_COLOR = 2;
	public final static int OPTION_VIEW_TYPE_SUBMENU = 3;
	public final static int OPTION_VIEW_TYPE_NUMBER = 4;

	protected View myView;
	protected int enabledColor;
	Properties mProperties;
	public CoolReader mActivity;
	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors = null;
	EinkScreen mEinkScreen;
	OptionOwner mOwner;
	LayoutInflater mInflater;
	public boolean enabled = true;
	public String label;
	public String property;
	public String defaultValue;
	public String disabledNote;
	public String addInfo;
	public ArrayList<String> quickFilters = new ArrayList<>();
	public HashMap<String, String> usefulLinks = new HashMap<String, String>();
	public boolean lastFiltered = false;
	public String lastFilteredValue = "";
	public int drawableAttrId = R.attr.cr3_option_other_drawable;
	public int fallbackIconId = R.drawable.cr3_option_other;
	public int fallbackIconId2 = R.drawable.cr3_option_other;
	public OptionsListView optionsListView;
	protected Runnable onChangeHandler;
	public OptionBase( OptionOwner owner, String label, String property, String addInfo, String filter) {
		this.mOwner = owner;
		this.mActivity = (CoolReader) owner.getActivity();
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors((CoolReader) mActivity, isEInk);
		this.mEinkScreen = this.mActivity.getEinkScreen();
		this.mInflater = owner.getInflater();
		this.mProperties = owner.getProperties();
		this.label = label;
		this.property = property;
		this.addInfo = addInfo;
		this.setFilteredMark(filter);
	}

	public void setCheckedValue(ImageView v, boolean checked) {
		if (checked) {
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_normal);
			v.setImageDrawable(d);
			v.setTag("1");
			mActivity.tintViewIconsForce(v);
		} else {
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_empty);
			v.setImageDrawable(d);
			v.setTag("0");
			mActivity.tintViewIcons(v, PorterDuff.Mode.CLEAR,true);
		}
	}

	public void setCheckedOption(ImageView v, boolean checked) {
		if (checked) {
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_checked_checkbox);
			v.setImageDrawable(d);
			v.setTag("1");
			mActivity.tintViewIconsForce(v);
		} else {
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_unchecked_checkbox);
			v.setImageDrawable(d);
			v.setTag("0");
			mActivity.tintViewIcons(v, PorterDuff.Mode.CLEAR,true);
		}
	}

	public boolean setFilteredMark(String filter) {
		this.lastFilteredValue = filter.toLowerCase();
		if (filter.trim().equals("")) this.lastFiltered = true;
		else {
			this.lastFiltered = this.label.toLowerCase().contains(filter.toLowerCase());
			this.lastFiltered = this.lastFiltered || this.property.toLowerCase().contains(filter.toLowerCase());
			this.lastFiltered = this.lastFiltered || this.addInfo.toLowerCase().contains(filter.toLowerCase());
		}
		return this.lastFiltered;
	}

	public boolean updateFilteredMark(String val) {
		if (this.lastFilteredValue.trim().equals("")) this.lastFiltered = true;
		else {
			this.lastFiltered = this.lastFiltered || val.toLowerCase().contains(this.lastFilteredValue);
		}
		return this.lastFiltered;
	}

	public boolean updateFilteredMark(String val1, String val2, String val3) {
		if (this.lastFilteredValue.trim().equals("")) this.lastFiltered = true;
		else {
			this.lastFiltered = this.lastFiltered || val1.toLowerCase().contains(this.lastFilteredValue);
			this.lastFiltered = this.lastFiltered || val2.toLowerCase().contains(this.lastFilteredValue);
			this.lastFiltered = this.lastFiltered || val3.toLowerCase().contains(this.lastFilteredValue);
		}
		return this.lastFiltered;
	}

	public boolean updateFilteredMark(boolean val) {
		if (this.lastFilteredValue.trim().equals("")) this.lastFiltered = true;
		else {
			this.lastFiltered = this.lastFiltered || val;
		}
		return this.lastFiltered;
	}

	public OptionBase setIconId(int id) {
		drawableAttrId = 0;
		fallbackIconId = id;
		return this;
	}
	public OptionBase setIcon2Id(int id) {
		fallbackIconId2 = id;
		return this;
	}
	public OptionBase setIconIdByAttr(int drawableAttrId, int fallbackIconId) {
		this.drawableAttrId = drawableAttrId;
		this.fallbackIconId = fallbackIconId;
		return this;
	}
	public OptionBase noIcon() {
		drawableAttrId = 0;
		fallbackIconId = 0;
		fallbackIconId2 = 0;
		return this;
	}
	public OptionBase setDefaultValue(String value) {
		this.defaultValue = value;
		if (mProperties.getProperty(property) == null)
			mProperties.setProperty(property, value);
		return this;
	}

	public OptionBase setDisabledNote(String note) {
		disabledNote = note;
		return this;
	}

	public OptionBase setOnChangeHandler( Runnable handler ) {
		onChangeHandler = handler;
		return this;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		refreshItem();
	}

	public int getItemViewType() {
		return OPTION_VIEW_TYPE_NORMAL;
	}

	protected void refreshItem()
	{
		getView(null, null).invalidate();
		//if ( optionsListView!=null )
		//	optionsListView.refresh();
	}

	protected void refreshList()
	{
		getView(null, null).invalidate();
		if (optionsListView != null)
			optionsListView.refresh();
	}

	public void setupIconView(ImageView icon) {
		if (null == icon)
			return;
		int resId = 0;
		if (OptionsDialog.showIcons) {
			if (drawableAttrId != 0) {
				resId = Utils.resolveResourceIdByAttr(mActivity, drawableAttrId, fallbackIconId);
			} else if (fallbackIconId != 0) {
				resId = fallbackIconId;
			}
		}
		if (resId != 0) {
			icon.setImageResource(resId);
			icon.setVisibility(View.VISIBLE);
			mActivity.tintViewIcons(icon,true);
		} else {
			icon.setImageResource(0);
			icon.setVisibility(View.INVISIBLE);
		}
	}

	protected void setup2IconView(ImageView icon1, ImageView icon2) {
		if (null != icon1) {
			int resId = 0;
			if (OptionsDialog.showIcons) {
				if (drawableAttrId != 0) {
					resId = Utils.resolveResourceIdByAttr(mActivity, drawableAttrId, fallbackIconId);
				} else if (fallbackIconId != 0) {
					resId = fallbackIconId;
				}
			}
			if (resId != 0) {
				icon1.setImageResource(resId);
				icon1.setVisibility(View.VISIBLE);
				mActivity.tintViewIcons(icon1,true);
			} else {
				icon1.setImageResource(0);
				icon1.setVisibility(View.INVISIBLE);
			}
		}
		if (null != icon2) {
			int resId = 0;
			if (OptionsDialog.showIcons) {
				resId = fallbackIconId2;
			}
			if (resId != 0) {
				icon2.setImageResource(resId);
				mActivity.tintViewIcons(icon2,true);
				icon2.setVisibility(View.VISIBLE);
			} else {
				icon2.setImageResource(0);
				icon2.setVisibility(View.INVISIBLE);
			}
		}
	}

	protected void valueIncDec(boolean isInc) {

	}

	public View getView(View convertView, ViewGroup parent) {
		View view;
		convertView = myView;
		if (convertView == null) {
			//view = new TextView(getContext());
			if (
					((this instanceof ListOption) || (this instanceof FlowListOption))
							&& (!(this instanceof OptionsDialog.TextureOptions))
			)
				view = mInflater.inflate(R.layout.option_item_list, null);
			else
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
		ImageView decButton = view.findViewById(R.id.option_btn_dec);
		ImageView incButton = view.findViewById(R.id.option_btn_inc);
		if (decButton != null) {
			mActivity.tintViewIcons(decButton);
			decButton.setOnClickListener(v -> {
				if (!enabled)
					return;
				valueIncDec(false);
			});
		}
		if (incButton != null) {
			mActivity.tintViewIcons(incButton);
			incButton.setOnClickListener(v -> {
				if (!enabled)
					return;
				valueIncDec(true);
			});
		}
		int colorIcon = themeColors.get(R.attr.colorIcon);
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
					//mActivity.showToast("to come...");
					//Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
					//toast.show();
					mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
					//ToastView.showToast(
					//	view1, addInfo, Toast.LENGTH_LONG);
					//	Toast.LENGTH_LONG, 20);
				});
		}
		labelView.setText(label);
		if (labelView.isEnabled()) enabledColor = labelView.getCurrentTextColor();
		labelView.setEnabled(enabled);
		int colorIconT= Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		if (!enabled)
			labelView.setTextColor(colorIconT);
		else
			labelView.setTextColor(enabledColor);
		if (valueView != null) {
			String valueLabel = getValueLabel();
			if (!enabled && null != disabledNote && disabledNote.length() > 0) {
				if (null != valueLabel && valueLabel.length() > 0)
					valueLabel = valueLabel + " (" + disabledNote + ")";
				else
					valueLabel = disabledNote;
			}
			if (null != valueLabel && valueLabel.length() > 0) {
				valueView.setText(valueLabel);
				valueView.setVisibility(View.VISIBLE);
			} else {
				valueView.setText("");
				valueView.setVisibility(View.INVISIBLE);
			}
			valueView.setEnabled(enabled);
		}
		setupIconView(view.findViewById(R.id.option_icon));
		mActivity.tintViewIcons(view,false);
		return view;
	}

	public String getValueLabel() { return mProperties.getProperty(property); }
	public void onSelect() {
		if (!enabled)
			return;
		refreshList();
	}

	public boolean onLongClick(View v) {
		if (!enabled)
			return false;
		if (!StrUtils.isEmptyStr(addInfo))
			mActivity.showToast(addInfo, Toast.LENGTH_LONG, v, true, 0);
		return true;
	}
}
