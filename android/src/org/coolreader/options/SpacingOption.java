package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class SpacingOption extends SubmenuOption {

	int[] mInterlineSpaces = new int[] {
			70, 72, 74, 76, 78, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
			91, 92, 93, 94, 95, 96, 97, 98, 99, 100,
			102, 103, 104, 105, 106, 107, 108, 109, 110,
			111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
			122, 124, 126, 128, 130, 132, 134, 136, 138, 140,
			142, 144, 146, 148, 150, 152, 154, 156, 158, 160,
			162, 164, 166, 168, 170, 172, 174, 176, 178, 180,
			182, 184, 186, 188, 190, 192, 194, 196, 198, 200,
			202, 204, 206, 208, 200, 212, 214, 216, 218, 220
	};
	int[] mMinSpaceWidths = new int[] {
			25, 30, 40, 50, 60, 70, 80, 90, 100
	};

	int[] mWordExpansion = new int[] {
			0, 5, 15, 20
	};

	int[] mWordExpansionTitles = new int[] {
			R.string.options_word_expanion1, R.string.options_word_expanion2, R.string.options_word_expanion3, R.string.options_word_expanion4
	};

	int[] mWordExpansionAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mCharCompress = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
	};

	int[] mSpaceWidthScalePercent = new int[] {
			65, 75, 95, 100, 110, 120, 130
	};

	int[] mSpaceCondensingPercent = new int[] {
			30, 40, 50, 75, 90, 100
	};

	int[] mUnusedSpacePercent = new int[] {
			1, 2, 3, 5, 10, 15, 20, 35, 30, 40, 50, 70
	};

	final BaseActivity mActivity;
	final Context mContext;

	public SpacingOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_SPACING_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("SpacingOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_interline_space), Settings.PROP_INTERLINE_SPACE,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				addPercents(mInterlineSpaces).setDefaultValue("100").
				setIconIdByAttr(R.attr.cr3_option_line_spacing_drawable, R.drawable.cr3_option_line_spacing));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_format_min_space_width_percent), Settings.PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				addPercents(mMinSpaceWidths).setDefaultValue("50").setIconIdByAttr(R.attr.cr3_option_text_width_drawable, R.drawable.cr3_option_text_width));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_word_expanion), Settings.PROP_FORMAT_MAX_ADDED_LETTER_SPACING_PERCENT,
				mActivity.getString(R.string.options_word_expanion_add_info), this.lastFilteredValue).
				add(mWordExpansion, mWordExpansionTitles, mWordExpansionAddInfos)
				.setDefaultValue("0")
				.noIcon()
		);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_char_space_compress), Settings.PROP_FONT_CHAR_SPACE_COMPRESS,
				mActivity.getString(R.string.options_char_space_compress_add_info), this.lastFilteredValue).
				add(mCharCompress)
				.setDefaultValue("0")
				.noIcon()
		);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_word_spacing_scaling), Settings.PROP_FORMAT_SPACE_WIDTH_SCALE_PERCENT,
				mActivity.getString(R.string.options_word_spacing_scaling_add_info), this.lastFilteredValue).
				add(mSpaceWidthScalePercent)
				.setDefaultValue("95")
				.noIcon()
		);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_format_unused_space_thres), Settings.PROP_FORMAT_UNUSED_SPACE_THRESHOLD_PERCENT,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mUnusedSpacePercent)
				.setDefaultValue("5")
				.noIcon()
		);
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_interline_space), Settings.PROP_INTERLINE_SPACE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_format_min_space_width_percent), Settings.PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_word_expanion), Settings.PROP_FORMAT_MAX_ADDED_LETTER_SPACING_PERCENT,
				mActivity.getString(R.string.options_word_expanion_add_info));
		for (int i: mWordExpansionTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mWordExpansionAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_word_spacing_scaling), Settings.PROP_FORMAT_SPACE_WIDTH_SCALE_PERCENT,
				mActivity.getString(R.string.options_word_spacing_scaling_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_format_unused_space_thres), Settings.PROP_FORMAT_UNUSED_SPACE_THRESHOLD_PERCENT,
				mActivity.getString(R.string.option_add_info_empty_text));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
