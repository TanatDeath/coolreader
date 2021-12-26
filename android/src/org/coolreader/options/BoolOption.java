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
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

public class BoolOption extends OptionBase {

	private boolean inverse = false;
	private String comment;
	final BaseActivity mActivity;

	public BoolOption(BaseActivity activity, OptionOwner owner, String label, String property, String addInfo, String filter ) {
		super(owner, label, property, addInfo, filter);
		mActivity = activity;
	}
	private boolean getValueBoolean() { return "1".equals(mProperties.getProperty(property)) ^ inverse; }
	public String getValueLabel() { return getValueBoolean()  ? mActivity.getString(R.string.options_value_on) : mActivity.getString(R.string.options_value_off); }
	public void onSelect() {
		if (!enabled)
			return;
		mProperties.setProperty(property, "1".equals(mProperties.getProperty(property)) ? "0" : "1");
		if (null != onChangeHandler)
			onChangeHandler.run();
		refreshList();
	}
	public BoolOption setInverse() { inverse = true; return this; }
	public BoolOption setComment(String comment) {
		this.comment = comment;
		this.updateFilteredMark(comment);
		return this;
	}
	public int getItemViewType() {
		return OPTION_VIEW_TYPE_BOOLEAN;
	}
	public View getView(View convertView, ViewGroup parent) {
		View view;
		convertView = myView;
		if (convertView == null) {
			//view = new TextView(getContext());
			view = mInflater.inflate(R.layout.option_item_boolean, null);
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
		TextView commentView = view.findViewById(R.id.option_comment);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		if (commentView != null)
			commentView.setTextColor(mActivity.getTextColor(colorIcon));
		ImageView valueView = view.findViewById(R.id.option_value_cb);
		ImageView btnOptionAddInfo = view.findViewById(R.id.btn_option_add_info);
		String commentLabel = addInfo;
		if (!enabled && null != disabledNote && disabledNote.length() > 0) {
			if (null != commentLabel && commentLabel.length() > 0)
				commentLabel = commentLabel + " (" + disabledNote + ")";
			else
				commentLabel = disabledNote;
		}
		if (StrUtils.isEmptyStr(commentLabel)) {
			btnOptionAddInfo.setVisibility(View.INVISIBLE);
		} else {
			btnOptionAddInfo.setImageDrawable(
					mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
							R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
			mActivity.tintViewIcons(btnOptionAddInfo);
			final View view1 = view;
			if (btnOptionAddInfo != null) {
				String finalCommentLabel = commentLabel;
				btnOptionAddInfo.setOnClickListener(v -> {
					//Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
					//toast.show();
					mActivity.showToast(finalCommentLabel, Toast.LENGTH_LONG, view1, true, 0);
				});
			}
		}
		labelView.setText(label);
		if (labelView.isEnabled()) enabledColor = labelView.getCurrentTextColor();
		labelView.setEnabled(enabled);
		int colorIconT= Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		if (!enabled)
			labelView.setTextColor(colorIconT);
		else
			labelView.setTextColor(enabledColor);
		if (null != comment) {
			commentView.setText(comment);
			commentView.setVisibility(View.VISIBLE);
		}
		setCheckedOption(valueView,getValueBoolean());
		valueView.setOnClickListener((v) -> onSelect());
		setupIconView(view.findViewById(R.id.option_icon));
		mActivity.tintViewIcons(view,false);
		return view;
	}
}