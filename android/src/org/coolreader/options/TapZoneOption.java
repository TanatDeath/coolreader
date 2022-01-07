package org.coolreader.options;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.GotoPageDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.RepeatOnTouchListener;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.Utils;
import org.coolreader.readerview.ReaderView;

import java.util.Locale;

public class TapZoneOption extends SubmenuOption {

	final BaseActivity mActivity;
	final ReaderView mReaderView;
	SeekBar seekLeft;
	SeekBar seekRight;
	ImageButton btnMinusLeft;
	ImageButton btnMinusRight;
	ImageButton btnPlusLeft;
	ImageButton btnPlusRight;
	TextView txtPercLeft;
	TextView txtPercRight;
	int screenWidth;

	public TapZoneOption(BaseActivity activity, OptionOwner owner, String label, String property, String addInfo, String filter ) {
		super( owner, label, property, addInfo, filter);
		mActivity = activity;
		mReaderView = ((CoolReader) mActivity).getmReaderView();
		screenWidth = mActivity.getWindowManager().getDefaultDisplay().getWidth();
		ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;
		for ( ReaderAction a : actions )
			this.updateFilteredMark(a.id, mActivity.getString(a.nameId), mActivity.getString(a.addInfoR));
	}
	View grid;
	private void initTapZone(View view, final int tapZoneId)
	{
		if (view == null)
			return;
		final TextView text = view.findViewById(R.id.tap_zone_action_text_short);
		final TextView longtext = view.findViewById(R.id.tap_zone_action_text_long);
		final ImageView iv = view.findViewById(R.id.zone_icon);
		final String propName = property + "." + tapZoneId;
		final String longPropName = property + ".long." + tapZoneId;
		final ReaderAction action = ReaderAction.findById( mProperties.getProperty(propName) );
		if ((iv != null)&&(action != null)) {
			int iconId = action.iconId;
			if (iconId == 0) {
				iconId = Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
			}
			Drawable d = mActivity.getResources().getDrawable(action.getIconIdWithDef(mActivity));
			iv.setImageDrawable(d);
			mActivity.tintViewIcons(iv,true);
		}
		final ReaderAction longAction = ReaderAction.findById(mProperties.getProperty(longPropName));
		final ImageView ivl = (ImageView)view.findViewById(R.id.zone_icon_long);
		if ((ivl != null)&&(longAction != null)) {
			int iconId = longAction.iconId;
			if (iconId == 0) {
				iconId = Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
			}
			Drawable d = mActivity.getResources().getDrawable(longAction.getIconIdWithDef(mActivity));
			ivl.setImageDrawable(d);
			mActivity.tintViewIcons(ivl, true);
		}
		text.setText(mActivity.getString(action.nameId));
		longtext.setText(mActivity.getString(longAction.nameId));
		int colorIcon = themeColors.get(R.attr.colorIcon);
		text.setTextColor(mActivity.getTextColor(colorIcon));
		longtext.setTextColor(mActivity.getTextColor(colorIcon));

		view.setLongClickable(true);
		final String filt = this.lastFilteredValue;
		view.setOnClickListener(v -> {
			// TODO: i18n
			ActionOption option = new ActionOption(mActivity, mOwner, mActivity.getString(R.string.options_app_tap_action_short), propName, true,
					false, mActivity.getString(action.addInfoR), filt);
			option.setIconId(action.getIconId());
			option.setOnChangeHandler(() -> {
				ReaderAction action1 = ReaderAction.findById(mProperties.getProperty(propName));
				text.setText(mActivity.getString(action1.nameId));
				int iconId = action1.iconId;
				if (iconId == 0) {
					iconId = Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
				}
				Drawable d = mActivity.getResources().getDrawable(action1.getIconIdWithDef(mActivity));
				iv.setImageDrawable(d);
				mActivity.tintViewIcons(iv,true);
			});
			option.onSelect();
		});
		view.setOnLongClickListener(v -> {
			// TODO: i18n
			ActionOption option = new ActionOption(mActivity, mOwner, mActivity.getString(R.string.options_app_tap_action_long), longPropName, true,
					true, mActivity.getString(longAction.addInfoR), filt);
			option.setIconId(action.getIconId());
			option.setOnChangeHandler(() -> {
				ReaderAction longAction1 = ReaderAction.findById( mProperties.getProperty(longPropName) );
				longtext.setText(mActivity.getString(longAction1.nameId));
				int iconId = longAction1.iconId;
				if (iconId == 0) {
					iconId = Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
				}
				Drawable d = mActivity.getResources().getDrawable(longAction1.getIconIdWithDef(mActivity));
				ivl.setImageDrawable(d);
				mActivity.tintViewIcons(ivl, true);
			});
			option.onSelect();
			return true;
		});
	}

	private void setTxtText(TextView txtPerc, int progress, boolean isLeft) {
		if (screenWidth == 0) return;
		float perc = ((float) progress * (float) screenWidth) / 2000.0F;
		if (!isLeft) perc = ((1000.0F - (float)progress) * (float)screenWidth) / 2000.0F;
		perc = perc * 100.0F / (float) screenWidth;
		//txtPerc.setText(String.format(Locale.getDefault(), "%d%", perc));
		txtPerc.setText(String.format("%1$.1f%%", perc));
		//txtPerc.setText("" + perc + "%");
	}

	public String getValueLabel() { return ">"; }
	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("TapZoneDialog", mActivity, label, false, false);
		grid = (View)mInflater.inflate(R.layout.options_tap_zone_grid, null);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell1), 1);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell2), 2);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell3), 3);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell4), 4);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell5), 5);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell6), 6);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell7), 7);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell8), 8);
		initTapZone(grid.findViewById(R.id.tap_zone_grid_cell9), 9);
		seekLeft = grid.findViewById(R.id.seek_margin_l);
		seekRight = grid.findViewById(R.id.seek_margin_r);
		btnMinusLeft = grid.findViewById(R.id.btn_minus_left);
		btnMinusLeft.setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> {
					if (screenWidth == 0) return;
					int step = (screenWidth / 2) / 100;
					if (step < 1) step = 1;
					seekLeft.setProgress(seekLeft.getProgress() + step);
				}
		));
		btnMinusRight = grid.findViewById(R.id.btn_minus_right);
		btnMinusRight.setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> {
					if (screenWidth == 0) return;
					int step = (screenWidth / 2) / 100;
					if (step < 1) step = 1;
					seekRight.setProgress(seekRight.getProgress() - step);
				}
		));
		btnPlusLeft = grid.findViewById(R.id.btn_plus_left);
		btnPlusLeft.setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> {
					if (screenWidth == 0) return;
					int step = (screenWidth / 2) / 100;
					if (step < 1) step = 1;
					seekLeft.setProgress(seekLeft.getProgress() - step);
				}
		));
		btnPlusRight = grid.findViewById(R.id.btn_plus_right);
		btnPlusRight.setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> {
					if (screenWidth == 0) return;
					int step = (screenWidth / 2) / 100;
					if (step < 1) step = 1;
					seekRight.setProgress(seekRight.getProgress() + step);
				}
		));
		txtPercLeft = grid.findViewById(R.id.txt_perc_left);
		txtPercRight = grid.findViewById(R.id.txt_perc_right);

		int l = mProperties.getInt(Settings.PROP_APP_TAP_ZONE_NON_SENS_LEFT, 0);
		int r = mProperties.getInt(Settings.PROP_APP_TAP_ZONE_NON_SENS_RIGHT, 0);
		seekLeft.setProgress(l);
		setTxtText(txtPercLeft, l, true);
		seekLeft.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				mProperties.setInt(Settings.PROP_APP_TAP_ZONE_NON_SENS_LEFT, progress);
				setTxtText(txtPercLeft, progress, true);
			}
		});
		seekRight.setProgress(1000-r);
		setTxtText(txtPercRight, 1000-r, false);
		seekRight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				mProperties.setInt(Settings.PROP_APP_TAP_ZONE_NON_SENS_RIGHT, 1000-progress);
				setTxtText(txtPercRight, progress, false);
			}
		});
		dlg.setView(grid);
		dlg.show();
	}
}
