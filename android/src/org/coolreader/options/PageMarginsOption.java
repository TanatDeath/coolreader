package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class PageMarginsOption extends SubmenuOption {

	int[] mScreenMargins = new int[] {
			0, 5, 10, 15, 20, 30, 40, 50, 60, 70, 80, 90, 100
	};

	public static int[] mMargins = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 18, 20, 23, 25, 28, 30, 35, 40, 45, 50, 55, 60, 70, 80, 90, 100, 110, 120, 130, 150, 200, 300
	};

	public static int getMarginShift(int curSize, int shift) {
		for (int i = 0; i < mMargins.length; i++) {
			boolean thisSize = (mMargins[i] == curSize);
			if (!thisSize)
				if (i>0) thisSize = (mMargins[i] >= curSize) && (mMargins[i - 1] < curSize);
			if (thisSize) {
				if (i + shift < 0) return mMargins[0];
				if (i + shift >= mMargins.length) return mMargins[mMargins.length - 1];
				return mMargins[i + shift];
			}
		}
		return curSize;
	}

	final BaseActivity mActivity;
	final Context mContext;

	public PageMarginsOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_PAGEMARGINS_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("PageMarginsOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.global_margin),
				Settings.PROP_GLOBAL_MARGIN, mActivity.getString(R.string.global_margin_add_info), this.lastFilteredValue)
				.add(mScreenMargins).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_text_margin_left_drawable, R.drawable.cr3_option_text_margins));
		listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_page_margin_left), Settings.PROP_PAGE_MARGIN_LEFT,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mMargins).setDefaultValue("5").
				setIconIdByAttr(R.attr.cr3_option_text_margin_left_drawable, R.drawable.cr3_option_text_margin_left));
		listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_page_margin_right), Settings.PROP_PAGE_MARGIN_RIGHT,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mMargins).setDefaultValue("5").
				setIconIdByAttr(R.attr.cr3_option_text_margin_right_drawable, R.drawable.cr3_option_text_margin_right));
		listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_page_margin_top), Settings.PROP_PAGE_MARGIN_TOP,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mMargins).setDefaultValue("5").
				setIconIdByAttr(R.attr.cr3_option_text_margin_top_drawable, R.drawable.cr3_option_text_margin_top));
		listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_page_margin_bottom), Settings.PROP_PAGE_MARGIN_BOTTOM,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mMargins).setDefaultValue("5").
				setIconIdByAttr(R.attr.cr3_option_text_margin_bottom_drawable, R.drawable.cr3_option_text_margin_bottom));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.global_margin), Settings.PROP_GLOBAL_MARGIN,
				mActivity.getString(R.string.global_margin_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_margin_left), Settings.PROP_PAGE_MARGIN_LEFT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_margin_right), Settings.PROP_PAGE_MARGIN_RIGHT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_margin_top), Settings.PROP_PAGE_MARGIN_TOP,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_page_margin_bottom), Settings.PROP_PAGE_MARGIN_BOTTOM,
				mActivity.getString(R.string.option_add_info_empty_text));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
