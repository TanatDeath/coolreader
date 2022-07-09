package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class HyphRendOption extends SubmenuOption {

	int[] mRenderingPresets = new int[] {
			Engine.BLOCK_RENDERING_FLAGS_LEGACY, Engine.BLOCK_RENDERING_FLAGS_FLAT,
			Engine.BLOCK_RENDERING_FLAGS_BOOK, Engine.BLOCK_RENDERING_FLAGS_WEB
	};
	int[] mRenderingPresetsTitles = new int[] {
			R.string.options_rendering_preset_legacy, R.string.options_rendering_preset_flat, R.string.options_rendering_preset_book, R.string.options_rendering_preset_web
	};
	int[] mRenderingPresetsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mDOMVersionPresets = new int[] {
			0, Engine.DOM_VERSION_CURRENT
	};
	int[] mDOMVersionPresetTitles = new int[] {
			R.string.options_requested_dom_level_legacy, R.string.options_requested_dom_level_newest
	};

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	public HyphRendOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_HYPH_REND_TITLE, addInfo, filter);
		mActivity = activity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("HyphRendOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);
		int rendFlags = mProperties.getInt(Settings.PROP_RENDER_BLOCK_RENDERING_FLAGS, 0);
		boolean legacyRender = rendFlags == 0 ||
				mProperties.getInt(Settings.PROP_REQUESTED_DOM_VERSION, 0) < Engine.DOM_VERSION_CURRENT;
		Runnable renderindChangeListsner = () -> {
			int rendFlags1 = mProperties.getInt(Settings.PROP_RENDER_BLOCK_RENDERING_FLAGS, 0);
			int curDOM = mProperties.getInt(Settings.PROP_REQUESTED_DOM_VERSION, 0);
			boolean legacyRender1 = rendFlags1 == 0 ||
					curDOM < Engine.DOM_VERSION_CURRENT;
			mOptionsDialog.mEnableMultiLangOption.setEnabled(!legacyRender1);
			if (legacyRender1) {
				mOptionsDialog.mHyphDictOption.setEnabled(true);
				mOptionsDialog.mEnableHyphOption.setEnabled(false);
			} else {
				boolean embeddedLang = mProperties.getBool(Settings.PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false);
				mOptionsDialog.mHyphDictOption.setEnabled(!embeddedLang);
				mOptionsDialog.mEnableHyphOption.setEnabled(embeddedLang);
			}
		};
		mOptionsDialog.optRenderingPreset = new ListOption(mOwner, mActivity.getString(R.string.options_rendering_preset), Settings.PROP_RENDER_BLOCK_RENDERING_FLAGS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mRenderingPresets, mRenderingPresetsTitles, mRenderingPresetsAddInfos)
				.setDefaultValue(Integer.valueOf(Engine.BLOCK_RENDERING_FLAGS_WEB).toString())
				.noIcon()
				.setOnChangeHandler(renderindChangeListsner);
		mOptionsDialog.optDOMVersion = new ListOption(mOwner, mActivity.getString(R.string.options_requested_dom_level), Settings.PROP_REQUESTED_DOM_VERSION,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mDOMVersionPresets, mDOMVersionPresetTitles, mRenderingPresetsAddInfos)
				.setDefaultValue(Integer.valueOf(Engine.DOM_VERSION_CURRENT).toString())
				.noIcon()
				.setOnChangeHandler(renderindChangeListsner);
		if (mOptionsDialog.mReaderView != null)
			if (mOptionsDialog.isFormatWithEmbeddedStyle) {
				listView.add(mOptionsDialog.optRenderingPreset);
				listView.add(mOptionsDialog.optDOMVersion);
			}

		mOptionsDialog.mEnableMultiLangOption = new BoolOption(mOwner, mActivity.getString(R.string.options_style_multilang),
				Settings.PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_text_multilang_drawable,
				R.drawable.cr3_option_text_multilang).
				setOnChangeHandler(() -> {
					boolean value = mProperties.getBool(Settings.PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false);
					mOptionsDialog.mHyphDictOption.setEnabled(!value);
					mOptionsDialog.mEnableHyphOption.setEnabled(value);
				});
		mOptionsDialog.mEnableMultiLangOption.enabled = !legacyRender;
		mOptionsDialog.mEnableMultiLangOption.setDisabledNote(mActivity.getString(R.string.options_legacy_rendering_enabled));
		listView.add(mOptionsDialog.mEnableMultiLangOption);
		mOptionsDialog.mEnableHyphOption = new BoolOption(mOwner, mActivity.getString(R.string.options_style_enable_hyphenation), Settings.PROP_TEXTLANG_HYPHENATION_ENABLED,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue, false).setDefaultValue("0").
				setIconIdByAttr(R.attr.cr3_option_text_hyphenation_drawable, R.drawable.cr3_option_text_hyphenation);
		mOptionsDialog.mEnableHyphOption.enabled = !legacyRender && mProperties.getBool(Settings.PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false);
		mOptionsDialog.mEnableHyphOption.setDisabledNote(mActivity.getString(R.string.options_multilingual_disabled));
		listView.add(mOptionsDialog.mEnableHyphOption);
		mOptionsDialog.mHyphDictOption = new HyphenationOptions(
				mActivity, mOwner, mActivity.getString(R.string.options_hyphenation_dictionary),
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				setIconIdByAttr(R.attr.cr3_option_text_hyphenation_drawable, R.drawable.cr3_option_text_hyphenation);
		mOptionsDialog.mHyphDictOption.enabled = legacyRender || !mProperties.getBool(Settings.PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false);
		mOptionsDialog.mHyphDictOption.setDisabledNote(mActivity.getString(R.string.options_multilingual_enabled));
		listView.add(mOptionsDialog.mHyphDictOption);
		if (mOptionsDialog.mReaderView != null) {
			listView.add(new BoolOption(mOwner, mActivity.getString(R.string.mi_book_styles_enable), Settings.PROP_EMBEDDED_STYLES,
					mActivity.getString(R.string.mi_book_styles_enable_add_info), this.lastFilteredValue, false).setDefaultValue("0").noIcon()
					.setOnChangeHandler(() -> {
						boolean value = mProperties.getBool(Settings.PROP_EMBEDDED_STYLES, false);
						mOptionsDialog.mEmbedFontsOptions.setEnabled(mOptionsDialog.isEpubFormat && value);
						mOptionsDialog.mEmbedFontsOptions.setDisabledNote(mActivity.getString(R.string.options_disabled_document_styles));
						mOptionsDialog.mIgnoreDocMargins.setEnabled(mOptionsDialog.isFormatWithEmbeddedStyle && value);
						mOptionsDialog.mIgnoreDocMargins.setDisabledNote(mActivity.getString(R.string.options_disabled_document_styles));
					}) );
			mOptionsDialog.mEmbedFontsOptions = new BoolOption(mOwner, mActivity.getString(R.string.options_font_embedded_document_font_enabled), Settings.PROP_EMBEDDED_FONTS,
					mActivity.getString(R.string.mi_book_styles_enable_add_info), this.lastFilteredValue, false).setDefaultValue("1").noIcon();
			boolean value = mProperties.getBool(Settings.PROP_EMBEDDED_STYLES, false);
			mOptionsDialog.mEmbedFontsOptions.setEnabled(mOptionsDialog.isEpubFormat && value);
			mOptionsDialog.mEmbedFontsOptions.setDisabledNote(mActivity.getString(R.string.options_disabled_document_styles));
			listView.add(mOptionsDialog.mEmbedFontsOptions);
			mOptionsDialog.mIgnoreDocMargins = new StyleBoolOption(mOwner, mActivity.getString(R.string.options_ignore_document_margins), "styles.body.margin", "", this.lastFilteredValue,
					"margin: 0em !important", "").setDefaultValueBoolean(false).noIcon();
			mOptionsDialog.mIgnoreDocMargins.setEnabled(mOptionsDialog.isFormatWithEmbeddedStyle && value);
			mOptionsDialog.mIgnoreDocMargins.setDisabledNote(mActivity.getString(R.string.options_disabled_document_styles));
			listView.add(mOptionsDialog.mIgnoreDocMargins);
//				OptionBase tmp = new StyleBoolOption(mOwner, mActivity.getString(R.string.more), "styles.image.background-color", "", this.lastFilteredValue,
//						"#c8c8c8", "#ffffff").setDefaultValueBoolean(false).noIcon();
//				listView.add(tmp);
			if (mOptionsDialog.isTextFormat) {
				listView.add(new BoolOption(mOwner, mActivity.getString(R.string.mi_text_autoformat_enable), Settings.PROP_TXT_OPTION_PREFORMATTED,
						mActivity.getString(R.string.mi_book_styles_enable_add_info), this.lastFilteredValue, false).setDefaultValue("1").noIcon());
			}
		}
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(Settings.PROP_RENDER_BLOCK_RENDERING_FLAGS, Settings.PROP_RENDER_BLOCK_RENDERING_FLAGS,
				Settings.PROP_RENDER_BLOCK_RENDERING_FLAGS);
		this.updateFilteredMark(Settings.PROP_REQUESTED_DOM_VERSION, Settings.PROP_REQUESTED_DOM_VERSION,
				Settings.PROP_REQUESTED_DOM_VERSION);
		this.updateFilteredMark(mActivity.getString(R.string.options_rendering_preset), Settings.PROP_RENDER_BLOCK_RENDERING_FLAGS,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mRenderingPresetsTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mRenderingPresetsAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_requested_dom_level), Settings.PROP_REQUESTED_DOM_VERSION,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mDOMVersionPresetTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mRenderingPresetsAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.options_style_multilang), Settings.PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_style_enable_hyphenation), Settings.PROP_TEXTLANG_HYPHENATION_ENABLED,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_hyphenation_dictionary), "",
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.mi_book_styles_enable), Settings.PROP_EMBEDDED_STYLES,
				mActivity.getString(R.string.mi_book_styles_enable_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_font_embedded_document_font_enabled), Settings.PROP_EMBEDDED_FONTS,
				mActivity.getString(R.string.mi_book_styles_enable_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.mi_text_autoformat_enable), Settings.PROP_TXT_OPTION_PREFORMATTED,
				mActivity.getString(R.string.mi_book_styles_enable_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_ignore_document_margins), "margin",
				"");
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
