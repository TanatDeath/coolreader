package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class FontTweaksOption extends SubmenuOption {

	int[] mShaping = new int[] {
			0, 1, 2
	};
	int[] mShapingTitles = new int[] {
			R.string.options_text_shaping_simple, R.string.options_text_shaping_light,
			R.string.options_text_shaping_full
	};
	int[] mShapingTitlesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	double[] mGammas = new double[] {
			0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.5, 1.9
	};

	int[] mHinting = new int[] {
			0, 1, 2
	};
	int[] mHintingTitles = new int[] {
			R.string.options_font_hinting_disabled, R.string.options_font_hinting_bytecode,
			R.string.options_font_hinting_auto
	};
	int[] mHintingTitlesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mEmboldenAlg = new int[] {
			0, 1
	};
	int[] mEmboldenAlgTitles = new int[] {
			R.string.options_font_embolden_alg0, R.string.options_font_embolden_alg1
	};
	int[] mEmboldenAlgAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	public static String[] mFineEmboldenTitles;
	public static int[] mFineEmboldenValues;

	final BaseActivity mActivity;
	final Context mContext;

	public FontTweaksOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_FONTTWEAKS_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
		mFineEmboldenTitles = activity.getResources().getStringArray(R.array.fine_embolden_titles);
		mFineEmboldenValues = activity.getResources().getIntArray(R.array.fine_embolden_values);
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("FontTweaksOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_text_shaping), Settings.PROP_FONT_SHAPING,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mShaping, mShapingTitles, mShapingTitlesAddInfos).setDefaultValue("1").
				setIconIdByAttr(R.attr.cr3_option_text_ligatures_drawable, R.drawable.cr3_option_text_ligatures));
		listView.add(new BoolOption(mActivity, mOwner, mActivity.getString(R.string.options_font_kerning), Settings.PROP_FONT_KERNING_ENABLED,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).setDefaultValue("1").setIconIdByAttr(R.attr.cr3_option_text_kerning_drawable, R.drawable.cr3_option_text_kerning));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_render_font_gamma), Settings.PROP_FONT_GAMMA,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mGammas).setDefaultValue("1.0").setIconIdByAttr(R.attr.cr3_option_font_gamma_drawable, R.drawable.cr3_option_font_gamma));
		//asdf - у хинтинга появилась иконка, подумать
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_font_hinting), Settings.PROP_FONT_HINTING,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mHinting, mHintingTitles, mHintingTitlesAddInfos).setDefaultValue("2")
				.noIcon());
		//.setIconIdByAttr(R.attr.cr3_option_text_hinting_drawable, R.drawable.cr3_option_text_hinting_hc));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_font_embolden_alg), Settings.PROP_FONT_EMBOLDEN_ALG,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mEmboldenAlg, mEmboldenAlgTitles, mEmboldenAlgAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.cr3_option_text_bold_drawable, R.drawable.cr3_option_text_bold));
		listView.add(new FlowListOption(mOwner, mActivity.getString(R.string.options_font_fine_embolden), Settings.PROP_FONT_FINE_EMBOLDEN,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mFineEmboldenValues, mFineEmboldenTitles).setDefaultValue("0").
				setIconIdByAttr(R.attr.cr3_option_text_bold_drawable, R.drawable.cr3_option_text_bold));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_text_shaping), Settings.PROP_FONT_SHAPING,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mShapingTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mShapingTitlesAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_font_kerning), Settings.PROP_FONT_KERNING_ENABLED,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_render_font_gamma), Settings.PROP_FONT_GAMMA,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_font_hinting), Settings.PROP_FONT_HINTING,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_font_embolden_alg), Settings.PROP_FONT_EMBOLDEN_ALG,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_font_fine_embolden), Settings.PROP_FONT_FINE_EMBOLDEN,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mHintingTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mHintingTitlesAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mEmboldenAlgTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mEmboldenAlgAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mFineEmboldenValues) this.updateFilteredMark(String.valueOf(i));
		for (String s: mFineEmboldenTitles) this.updateFilteredMark(s);
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
