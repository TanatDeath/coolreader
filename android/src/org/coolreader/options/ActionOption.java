package org.coolreader.options;

import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.utils.Utils;

public class ActionOption extends ListOption {

	final BaseActivity mActivity;
	public ActionOption(BaseActivity activity, OptionOwner owner, String label, String property, boolean isTap, boolean allowRepeat,
						String addInfo, String filter) {
		super(owner, label, property, addInfo, filter);
		mActivity = activity;
		ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;
		for ( ReaderAction a : actions )
			if (!isTap || a.mayAssignOnTap())
				add(a.id, mActivity.getString(a.nameId), mActivity.getString(a.addInfoR));
		if (allowRepeat)
			add(ReaderAction.REPEAT.id, mActivity.getString(ReaderAction.REPEAT.nameId), mActivity.getString(ReaderAction.REPEAT.addInfoR));
		if (mProperties.getProperty(property) == null)
			mProperties.setProperty(property, ReaderAction.NONE.id);
	}

	protected int getItemLayoutId() {
		return R.layout.option_value;
	}

	protected void updateItemContents(final View layout, final OptionsDialog.Three item, final ListView listView, final int position ) {
		super.updateItemContents(layout, item, listView, position);
		ImageView img = (ImageView) layout.findViewById(R.id.option_value_icon);
		ImageView imgAddInfo = (ImageView) layout.findViewById(R.id.btn_option_add_info);
		ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;
		for ( ReaderAction a : actions )
			if (item.value.equals(a.id)) {
				if (a.getIconId()!=0) {
					img.setImageDrawable(mActivity.getResources().getDrawable(
							a.getIconId()));
					mActivity.tintViewIcons(img, true);
				}
				final String addInfo = mActivity.getString(a.addInfoR);
				if (!addInfo.equals("")) {
					imgAddInfo.setImageDrawable(
							mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
									R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
					mActivity.tintViewIcons(imgAddInfo);
					imgAddInfo.setVisibility(View.VISIBLE);
					imgAddInfo.setOnClickListener(v -> mActivity.showToast(addInfo, Toast.LENGTH_LONG, layout, true, 0));
				}
			}
	}
}
