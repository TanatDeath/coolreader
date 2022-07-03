package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;

public class StyleEditorOption extends SubmenuOption {

	private final String prefix;
	final BaseActivity mActivity;
	final Context mContext;
	public StyleEditorOption(BaseActivity activity, Context context, OptionOwner owner, String label, String prefix, String addInfo, String filter ) {
		super(owner, label, "dummy.prop", addInfo, filter);
		mActivity = activity;
		mContext = context;
		this.prefix = prefix;
	}

	public void onSelect() {
		SelectOrFilter(true);
	}

	public boolean updateFilterEnd() {
		SelectOrFilter(false);
		return this.lastFiltered;
	}

	public void SelectOrFilter(boolean isSelect) {
		BaseDialog dlg = null;
		if (isSelect) {
			dlg = new BaseDialog("StyleEditorDialog", mActivity, label, false, false);
		}
		OptionsListView listView = new OptionsListView(mContext, this);
		String[] firstLineOptions = {"", "text-align: justify", "text-align: left", "text-align: center", "text-align: right", };
		int empty = R.string.option_add_info_empty_text;
		int[] addInfos = {empty, empty, empty, empty, empty, };
		int[] firstLineOptionNames = {
				R.string.options_css_inherited,
				R.string.options_css_text_align_justify,
				R.string.options_css_text_align_left,
				R.string.options_css_text_align_center,
				R.string.options_css_text_align_right,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_text_align), prefix + ".align",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(firstLineOptions,
				firstLineOptionNames, addInfos).setIconIdByAttr(R.attr.cr3_option_text_align_drawable, R.drawable.cr3_option_text_align));

		String[] identOptions = {"", // inherited
				"text-indent: 0em",
				"text-indent: 1.2em",
				"text-indent: 2em",
				"text-indent: -1.2em",
				"text-indent: -2em"};
		int[] identOptionNames = {
				R.string.options_css_inherited,
				R.string.options_css_text_indent_no_indent,
				R.string.options_css_text_indent_small_indent,
				R.string.options_css_text_indent_big_indent,
				R.string.options_css_text_indent_small_outdent,
				R.string.options_css_text_indent_big_outdent};
		int[] addInfosI = {empty, empty, empty, empty, empty, empty};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_text_indent), prefix + ".text-indent",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(identOptions,
				identOptionNames, addInfosI).setIconIdByAttr(R.attr.cr3_option_text_indent_drawable, R.drawable.cr3_option_text_indent));

		listView.add(new FontSelectOption(mOwner, mActivity.getString(R.string.options_css_font_face), prefix + ".font-face",
				mActivity.getString(R.string.option_add_info_empty_text), true, this.lastFilteredValue).
				setIconIdByAttr(R.attr.cr3_option_font_face_drawable, R.drawable.cr3_option_font_face));

		String[] fontSizeStyles = {
				"", // inherited
				"font-size: 110%",
				"font-size: 120%",
				"font-size: 150%",
				"font-size: 90%",
				"font-size: 80%",
				"font-size: 70%",
				"font-size: 60%",
		};
		int[] fontSizeStyleNames = {
				R.string.options_css_inherited,
				R.string.options_css_font_size_110p,
				R.string.options_css_font_size_120p,
				R.string.options_css_font_size_150p,
				R.string.options_css_font_size_90p,
				R.string.options_css_font_size_80p,
				R.string.options_css_font_size_70p,
				R.string.options_css_font_size_60p,
		};
		int[] fontSizeStyleAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_font_size), prefix + ".font-size",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(fontSizeStyles, fontSizeStyleNames, fontSizeStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size));

		String[] fontWeightStyles = {
				"", // inherited
				"font-weight: normal",
				"font-weight: bold",
				"font-weight: bolder",
				"font-weight: lighter",
		};
		int[] fontWeightStyleNames = {
				R.string.options_css_inherited,
				R.string.options_css_font_weight_normal,
				R.string.options_css_font_weight_bold,
				R.string.options_css_font_weight_bolder,
				R.string.options_css_font_weight_lighter,
		};
		int[] fontWeightStyleAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_font_weight), prefix + ".font-weight",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(fontWeightStyles, fontWeightStyleNames, fontWeightStyleAddInfos).setIconIdByAttr(
				R.attr.cr3_option_text_bold_drawable, R.drawable.cr3_option_text_bold));

		String[] fontStyleStyles = {
				"", // inherited
				"font-style: normal",
				"font-style: italic",
		};
		int[] fontStyleStyleNames = {
				R.string.options_css_inherited,
				R.string.options_css_font_style_normal,
				R.string.options_css_font_style_italic,
		};
		int[] fontStyleStyleAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_font_style), prefix + ".font-style",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(fontStyleStyles, fontStyleStyleNames, fontStyleStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_text_italic_drawable, R.drawable.cr3_option_text_italic));

		String[] lineHeightStyles = {
				"", // inherited
				"line-height: 75%",
				"line-height: 80%",
				"line-height: 85%",
				"line-height: 90%",
				"line-height: 95%",
				"line-height: 100%",
				"line-height: 110%",
				"line-height: 120%",
				"line-height: 130%",
				"line-height: 140%",
				"line-height: 150%",
		};
		String[] lineHeightStyleNames = {
				"-",
				"75%",
				"80%",
				"85%",
				"90%",
				"95%",
				"100%",
				"110%",
				"120%",
				"130%",
				"140%",
				"150%",
		};
		int[] lineHeightStyleAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_interline_space), prefix + ".line-height",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(lineHeightStyles, lineHeightStyleNames, lineHeightStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_line_spacing_drawable, R.drawable.cr3_option_line_spacing));

		String[] textDecorationStyles = {
				"", // inherited
				"text-decoration: none",
				"text-decoration: underline",
				"text-decoration: line-through",
				"text-decoration: overline",
		};
		int[] textDecorationStyleNames = {
				R.string.options_css_inherited,
				R.string.options_css_text_decoration_none,
				R.string.options_css_text_decoration_underline,
				R.string.options_css_text_decoration_line_through,
				R.string.options_css_text_decoration_overlineline,
		};
		int[] textDecorationStyleAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_font_decoration), prefix + ".text-decoration",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(textDecorationStyles, textDecorationStyleNames, textDecorationStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_text_underline_drawable, R.drawable.cr3_option_text_underline));

		String[] verticalAlignStyles = {
				"", // inherited
				"vertical-align: baseline",
				"vertical-align: sub",
				"vertical-align: super",
		};
		int[] verticalAlignStyleNames = {
				R.string.options_css_inherited,
				R.string.options_css_text_valign_baseline,
				R.string.options_css_text_valign_subscript,
				R.string.options_css_text_valign_superscript,
		};
		int[] verticalAlignStyleAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_text_valign), prefix + ".vertical-align",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(verticalAlignStyles, verticalAlignStyleNames, verticalAlignStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_text_superscript_drawable, R.drawable.cr3_option_text_superscript));

		String[] fontColorStyles = {
				"", // inherited
				"color: black",
				"color: green",
				"color: silver",
				"color: lime",
				"color: gray",
				"color: olive",
				"color: white",
				"color: yellow",
				"color: maroon",
				"color: navy",
				"color: red",
				"color: blue",
				"color: purple",
				"color: teal",
				"color: fuchsia",
				"color: aqua",
		};
		String[] fontColorStyleNames = {
				"-",
				"Black",
				"Green",
				"Silver",
				"Lime",
				"Gray",
				"Olive",
				"White",
				"Yellow",
				"Maroon",
				"Navy",
				"Red",
				"Blue",
				"Purple",
				"Teal",
				"Fuchsia",
				"Aqua",
		};
		int[] fontColorStyleAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_text_color), prefix + ".color",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(fontColorStyles, fontColorStyleNames, fontColorStyleAddInfos).setIconIdByAttr(R.attr.attr_icons8_font_color, R.drawable.icons8_font_color));

		String[] marginTopOptions = {"", // inherited
				"margin-top: 0em",
				"margin-top: 0.2em",
				"margin-top: 0.3em",
				"margin-top: 0.5em",
				"margin-top: 1em",
				"margin-top: 2em"};
		String[] marginBottomOptions = {"", // inherited
				"margin-bottom: 0em",
				"margin-bottom: 0.2em",
				"margin-bottom: 0.3em",
				"margin-bottom: 0.5em",
				"margin-bottom: 1em",
				"margin-bottom: 2em"};
		int[] marginTopBottomOptionNames = {
				R.string.options_css_inherited,
				R.string.options_css_margin_0,
				R.string.options_css_margin_02em,
				R.string.options_css_margin_03em,
				R.string.options_css_margin_05em,
				R.string.options_css_margin_1em,
				R.string.options_css_margin_15em,
		};
		int[] marginTopBottomOptionAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		String[] marginLeftOptions = {
				"", // inherited
				"margin-left: 0em",
				"margin-left: 0.5em",
				"margin-left: 1em",
				"margin-left: 1.5em",
				"margin-left: 2em",
				"margin-left: 4em",
				"margin-left: 5%",
				"margin-left: 10%",
				"margin-left: 15%",
				"margin-left: 20%",
				"margin-left: 30%"};
		String[] marginRightOptions = {
				"", // inherited
				"margin-right: 0em",
				"margin-right: 0.5em",
				"margin-right: 1em",
				"margin-right: 1.5em",
				"margin-right: 2em",
				"margin-right: 4em",
				"margin-right: 5%",
				"margin-right: 10%",
				"margin-right: 15%",
				"margin-right: 20%",
				"margin-right: 30%"};
		int[] marginLeftRightOptionNames = {
				R.string.options_css_inherited,
				R.string.options_css_margin_0,
				R.string.options_css_margin_05em,
				R.string.options_css_margin_1em,
				R.string.options_css_margin_15em,
				R.string.options_css_margin_2em,
				R.string.options_css_margin_4em,
				R.string.options_css_margin_5p,
				R.string.options_css_margin_10p,
				R.string.options_css_margin_15p,
				R.string.options_css_margin_20p,
				R.string.options_css_margin_30p,
		};
		int[] marginLeftRightOptionAddInfos = {
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
				R.string.option_add_info_empty_text,
		};
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_margin_top), prefix + ".margin-top",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(marginTopOptions, marginTopBottomOptionNames, marginTopBottomOptionAddInfos).setIconIdByAttr(R.attr.cr3_option_text_margin_top_drawable, R.drawable.cr3_option_text_margin_top));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_margin_bottom), prefix + ".margin-bottom",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(marginBottomOptions, marginTopBottomOptionNames, marginTopBottomOptionAddInfos).setIconIdByAttr(R.attr.cr3_option_text_margin_bottom_drawable, R.drawable.cr3_option_text_margin_bottom));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_margin_left), prefix + ".margin-left",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(marginLeftOptions, marginLeftRightOptionNames, marginLeftRightOptionAddInfos).setIconIdByAttr(R.attr.cr3_option_text_margin_left_drawable, R.drawable.cr3_option_text_margin_left));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_css_margin_right), prefix + ".margin-right",
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(marginRightOptions, marginLeftRightOptionNames, marginLeftRightOptionAddInfos).setIconIdByAttr(R.attr.cr3_option_text_margin_right_drawable, R.drawable.cr3_option_text_margin_right));

		if (isSelect) {
			dlg.setTitle(label);
			dlg.setView(listView);
			dlg.show();
		}
	}

	public String getValueLabel() { return ">"; }
}
