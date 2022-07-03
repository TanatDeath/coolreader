package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class ImageScalingOption extends SubmenuOption {

	int[] mImageScalingModes = new int[] {
			0, 1, 2
	};
	int[] mImageScalingModesTitles = new int[] {
			R.string.options_format_image_scaling_mode_disabled, R.string.options_format_image_scaling_mode_integer_factor, R.string.options_format_image_scaling_mode_arbitrary
	};

	int[] mImageScalingModesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mImageScalingFactors = new int[] {
			0, 1, 2, 3
	};
	int[] mImageScalingFactorsTitles = new int[] {
			R.string.options_format_image_scaling_scale_auto, R.string.options_format_image_scaling_scale_1, R.string.options_format_image_scaling_scale_2, R.string.options_format_image_scaling_scale_3
	};

	int[] mImageScalingFactorsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	final BaseActivity mActivity;
	final Context mContext;

	public ImageScalingOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}
	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("ImageScalingDialog", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_format_image_scaling_block_mode), Settings.PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mImageScalingModes, mImageScalingModesTitles, mImageScalingModesAddInfos).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_expand,
				R.drawable.icons8_expand));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_format_image_scaling_block_scale), Settings.PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mImageScalingFactors,
				mImageScalingFactorsTitles, mImageScalingFactorsAddInfos).
				setDefaultValue("2").setIconIdByAttr(R.attr.attr_icons8_expand,
				R.drawable.icons8_expand));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_format_image_scaling_inline_mode), Settings.PROP_IMG_SCALING_ZOOMIN_INLINE_MODE, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mImageScalingModes,
				mImageScalingModesTitles, mImageScalingModesAddInfos).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_expand,
				R.drawable.icons8_expand));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_format_image_scaling_inline_scale), Settings.PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE, mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mImageScalingFactors,
				mImageScalingFactorsTitles, mImageScalingFactorsAddInfos).
				setDefaultValue("2").setIconIdByAttr(R.attr.attr_icons8_expand,
				R.drawable.icons8_expand));
		listView.add(new BoolOption(mOwner, mActivity.getString(R.string.options_format_image_background), Settings.PROP_IMG_CUSTOM_BACKGROUND,
				mActivity.getString(R.string.options_format_image_background_add_info), this.lastFilteredValue).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_paint_palette1,
						R.drawable.icons8_paint_palette1));
		listView.add(new ColorOption(mOwner, mActivity.getString(R.string.options_format_image_background_color), Settings.PROP_IMG_CUSTOM_BACKGROUND_COLOR,
				0xffffff,
				mActivity.getString(R.string.options_format_image_background_add_info), this.lastFilteredValue).
				setIconIdByAttr(R.attr.attr_icons8_paint_palette1, R.drawable.icons8_paint_palette1));
		dlg.setView(listView);
		dlg.show();
	}

	private void copyProperty( String to, String from ) {
		mProperties.put(to, mProperties.get(from));
	}

	protected void closed() {
		copyProperty(Settings.PROP_IMG_SCALING_ZOOMOUT_BLOCK_MODE, Settings.PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE);
		copyProperty(Settings.PROP_IMG_SCALING_ZOOMOUT_INLINE_MODE, Settings.PROP_IMG_SCALING_ZOOMIN_INLINE_MODE);
		copyProperty(Settings.PROP_IMG_SCALING_ZOOMOUT_BLOCK_SCALE, Settings.PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE);
		copyProperty(Settings.PROP_IMG_SCALING_ZOOMOUT_INLINE_SCALE, Settings.PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE);
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_format_image_scaling_block_mode), Settings.PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_format_image_scaling_block_scale), Settings.PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_format_image_scaling_inline_mode), Settings.PROP_IMG_SCALING_ZOOMIN_INLINE_MODE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_format_image_scaling_inline_scale), Settings.PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_format_image_background), Settings.PROP_IMG_CUSTOM_BACKGROUND,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(mActivity.getString(R.string.options_format_image_background_color), Settings.PROP_IMG_CUSTOM_BACKGROUND_COLOR,
				mActivity.getString(R.string.option_add_info_empty_text));
		this.updateFilteredMark(Settings.PROP_IMG_SCALING_ZOOMOUT_BLOCK_MODE);
		this.updateFilteredMark(Settings.PROP_IMG_SCALING_ZOOMOUT_INLINE_MODE);
		this.updateFilteredMark(Settings.PROP_IMG_SCALING_ZOOMOUT_BLOCK_SCALE);
		this.updateFilteredMark(Settings.PROP_IMG_SCALING_ZOOMOUT_INLINE_SCALE);
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
