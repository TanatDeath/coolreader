package org.coolreader.options;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.Utils;

import java.util.HashMap;
import java.util.Locale;

public class DeviceTurnDialog extends BaseDialog {

    private final View mGrid;
    private final CoolReader mCoolReader;
    private final TextView tvCoord1;
    private final TextView tvCoord2;
    private final TextView tvCoord3;
    private final TextView tvCoord1Fix;
    private final TextView tvCoord2Fix;
    private final TextView tvCoord3Fix;
    private final TextView tvCoord1FixBack;
    private final TextView tvCoord2FixBack;
    private final TextView tvCoord3FixBack;
    private final ImageView ivCoord1FixUse;
    private final ImageView ivCoord2FixUse;
    private final ImageView ivCoord3FixUse;
    private final ImageView ivCoord1FixBackUse;
    private final ImageView ivCoord2FixBackUse;
    private final ImageView ivCoord3FixBackUse;
    private final Button btnFixTurn;
    private final Button btnFixTurnBack;
    private final SeekBar seekPrecision;
    private final TextView tvTestArea;
    private float[] mGravity;
    private float[] mGravityFix;
    private float[] mGravityFixBack;
    boolean isEInk = false;
    HashMap<Integer, Integer> themeColors;
    private final Properties mProperties;

    public DeviceTurnDialog(String dlgName, BaseActivity activity, Properties properties,
                            String title, View grid, boolean showNegativeButton, boolean windowed) {
        super(dlgName, activity, title, showNegativeButton, windowed);
        mProperties = properties;
        isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
        themeColors = Utils.getThemeColors(mActivity, isEInk);
        mGrid = grid;
        tvCoord1 = mGrid.findViewById(R.id.tv_device_turn_coord1);
        tvCoord2 = mGrid.findViewById(R.id.tv_device_turn_coord2);
        tvCoord3 = mGrid.findViewById(R.id.tv_device_turn_coord3);
        tvCoord1Fix = mGrid.findViewById(R.id.tv_device_turn_coord1_fix);
        tvCoord2Fix = mGrid.findViewById(R.id.tv_device_turn_coord2_fix);
        tvCoord3Fix = mGrid.findViewById(R.id.tv_device_turn_coord3_fix);
        tvCoord1Fix.setText(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".coord_1"));
        tvCoord2Fix.setText(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".coord_2"));
        tvCoord3Fix.setText(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".coord_3"));
        tvCoord1FixBack = mGrid.findViewById(R.id.tv_device_turn_coord1_fix_back);
        tvCoord2FixBack = mGrid.findViewById(R.id.tv_device_turn_coord2_fix_back);
        tvCoord3FixBack = mGrid.findViewById(R.id.tv_device_turn_coord3_fix_back);
        tvCoord1FixBack.setText(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".back_coord_1"));
        tvCoord2FixBack.setText(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".back_coord_2"));
        tvCoord3FixBack.setText(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".back_coord_3"));
        ivCoord1FixUse = mGrid.findViewById(R.id.iv_device_turn_coord1_fix_use);
        ivCoord1FixUse.setOnClickListener(view1 -> toggleCheckedOption((ImageView) view1));
        ivCoord2FixUse = mGrid.findViewById(R.id.iv_device_turn_coord2_fix_use);
        ivCoord2FixUse.setOnClickListener(view1 -> toggleCheckedOption((ImageView) view1));
        ivCoord3FixUse = mGrid.findViewById(R.id.iv_device_turn_coord3_fix_use);
        ivCoord3FixUse.setOnClickListener(view1 -> toggleCheckedOption((ImageView) view1));
        ivCoord1FixBackUse = mGrid.findViewById(R.id.iv_device_turn_coord1_fix_back_use);
        ivCoord1FixBackUse.setOnClickListener(view1 -> toggleCheckedOption((ImageView) view1));
        ivCoord2FixBackUse = mGrid.findViewById(R.id.iv_device_turn_coord2_fix_back_use);
        ivCoord2FixBackUse.setOnClickListener(view1 -> toggleCheckedOption((ImageView) view1));
        ivCoord3FixBackUse = mGrid.findViewById(R.id.iv_device_turn_coord3_fix_back_use);
        ivCoord3FixBackUse.setOnClickListener(view1 -> toggleCheckedOption((ImageView) view1));
        setCheckedOption(ivCoord1FixUse, "1".equals(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_coord_1")));
        setCheckedOption(ivCoord2FixUse, "1".equals(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_coord_2")));
        setCheckedOption(ivCoord3FixUse, "1".equals(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_coord_3")));
        setCheckedOption(ivCoord1FixBackUse, "1".equals(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_back_coord_1")));
        setCheckedOption(ivCoord2FixBackUse, "1".equals(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_back_coord_2")));
        setCheckedOption(ivCoord3FixBackUse, "1".equals(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_back_coord_3")));
        seekPrecision = mGrid.findViewById(R.id.seek_precision);
        seekPrecision.setProgress(Utils.parseInt(mProperties.getProperty(Settings.PROP_APP_DEVICE_TURN + ".precision"), 10));
        tvTestArea = mGrid.findViewById(R.id.tv_test_area);
        int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
        btnFixTurn = mGrid.findViewById(R.id.fix_turn);
        btnFixTurnBack = mGrid.findViewById(R.id.fix_turn_back);
        btnFixTurn.setBackgroundColor(colorGrayC);
        btnFixTurn.setOnClickListener(view1 -> {
            if (mGravity != null) {
                mGravityFix = mGravity.clone();
                if (mGravityFix.length >= 0) {
                    tvCoord1Fix.setText("" + Math.round(mGravityFix[0] * 100));
                }
                if (mGravityFix.length >= 1) {
                    tvCoord2Fix.setText("" + Math.round(mGravityFix[1] * 100));
                }
                if (mGravityFix.length >= 2) {
                    tvCoord3Fix.setText("" + Math.round(mGravityFix[2] * 100));
                }
            }
        });
        if (isEInk) Utils.setSolidButtonEink(btnFixTurn);
        btnFixTurnBack.setBackgroundColor(colorGrayC);
        btnFixTurnBack.setOnClickListener(view1 -> {
            if (mGravity != null) {
                mGravityFixBack = mGravity.clone();
                if (mGravityFixBack.length >= 0) {
                    tvCoord1FixBack.setText("" + Math.round(mGravityFixBack[0] * 100));
                }
                if (mGravityFixBack.length >= 1) {
                    tvCoord2FixBack.setText("" + Math.round(mGravityFixBack[1] * 100));
                }
                if (mGravityFixBack.length >= 2) {
                    tvCoord3FixBack.setText("" + Math.round(mGravityFixBack[2] * 100));
                }
            }
        });
        if (isEInk) Utils.setSolidButtonEink(btnFixTurnBack);
        mCoolReader = (CoolReader) activity;
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

    public boolean getCheckedOption(ImageView v) {
        Object tag = v.getTag();
        if (tag instanceof String) return tag.equals("1");
        return false;
    }

    public void toggleCheckedOption(ImageView v) {
        Object tag = v.getTag();
        boolean checked = !("1".equals(tag));
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

    private boolean closing = false;

    private long lastTurn = 0;
    private boolean inTurnPosNow = false;
    private boolean inTurnBackPosNow = false;

    private void checkTestArea() {
        double dCoord1 = 999.0;
        double dCoord2 = 999.0;
        double dCoord3 = 999.0;
        double dCoordFix1 = 999.0;
        double dCoordFix2 = 999.0;
        double dCoordFix3 = 999.0;
        double dCoordFixBack1 = 999.0;
        double dCoordFixBack2 = 999.0;
        double dCoordFixBack3 = 999.0;
        dCoord1 = Utils.parseDouble(tvCoord1.getText().toString(), 999.0);
        dCoord2 = Utils.parseDouble(tvCoord2.getText().toString(), 999.0);
        dCoord3 = Utils.parseDouble(tvCoord3.getText().toString(), 999.0);
        boolean checked = getCheckedOption(ivCoord1FixUse);
        if (checked) dCoordFix1 = Utils.parseDouble(tvCoord1Fix.getText().toString(), 999.0);
        checked = getCheckedOption(ivCoord2FixUse);
        if (checked) dCoordFix2 = Utils.parseDouble(tvCoord2Fix.getText().toString(), 999.0);
        checked = getCheckedOption(ivCoord3FixUse);
        if (checked) dCoordFix3 = Utils.parseDouble(tvCoord3Fix.getText().toString(), 999.0);
        checked = getCheckedOption(ivCoord1FixBackUse);
        if (checked) dCoordFixBack1 = Utils.parseDouble(tvCoord1FixBack.getText().toString(), 999.0);
        checked = getCheckedOption(ivCoord2FixBackUse);
        if (checked) dCoordFixBack2 = Utils.parseDouble(tvCoord2FixBack.getText().toString(), 999.0);
        checked = getCheckedOption(ivCoord3FixBackUse);
        if (checked) dCoordFixBack3 = Utils.parseDouble(tvCoord3FixBack.getText().toString(), 999.0);
        double pres = seekPrecision.getProgress();
        // will we check turn
        boolean inTurn = false;
        boolean inTurnBack = false;
        double gap = 0.0;
        if ((dCoordFix1 < 998.0) || (dCoordFix2 < 998.0) || (dCoordFix3 < 998.0)) {
            inTurn = true;
            if ((dCoord1 < 998.0) && (dCoordFix1 < 998.0)) {
                gap = pres * 10.0;
                if ((dCoordFix1 < dCoord1 - gap) || (dCoordFix1 > dCoord1 + gap)) inTurn = false;
            }
            if (inTurn) {
                if ((dCoord2 < 998.0) && (dCoordFix2 < 998.0)) {
                    gap = Math.abs(dCoord2) * pres / 100.0;
                    if ((dCoordFix2 < dCoord2 - gap) || (dCoordFix2 > dCoord2 + gap)) inTurn = false;
                }
            }
            if (inTurn) {
                if ((dCoord3 < 998.0) && (dCoordFix3 < 998.0)) {
                    gap = Math.abs(dCoord3) * pres / 100.0;
                    if ((dCoordFix3 < dCoord3 - gap) || (dCoordFix3 > dCoord3 + gap)) inTurn = false;
                }
            }
        }
        long thisTime = System.currentTimeMillis();
        if ((!inTurnPosNow) && (inTurn) && (thisTime - lastTurn > 1000)) { // we were not in turn, but we are now - so turn
            inTurnPosNow = true;
            inTurnBackPosNow = false;
            lastTurn = System.currentTimeMillis();
            tvTestArea.setText( "" + (Utils.parseInt(tvTestArea.getText().toString(), 1000) + 1));
        }
        if (!inTurn) inTurnPosNow = false;
        if (!inTurn) { // if we are not in turn now - so check back turn
            if ((dCoordFixBack1 < 998.0) || (dCoordFixBack2 < 998.0) || (dCoordFixBack3 < 998.0)) {
                inTurnBack = true;
                if ((dCoord1 < 998.0) && (dCoordFixBack1 < 998.0)) {
                    gap = pres * 10.0;
                    if ((dCoordFixBack1 < dCoord1 - gap) || (dCoordFixBack1 > dCoord1 + gap)) inTurnBack = false;
                }
                if (inTurnBack) {
                    if ((dCoord2 < 998.0) && (dCoordFixBack2 < 998.0)) {
                        gap = Math.abs(dCoord2) * pres / 100.0;
                        if ((dCoordFixBack2 < dCoord2 - gap) || (dCoordFixBack2 > dCoord2 + gap)) inTurnBack = false;
                    }
                }
                if (inTurnBack) {
                    if ((dCoord3 < 998.0) && (dCoordFixBack3 < 998.0)) {
                        gap = Math.abs(dCoord3) * pres / 100.0;
                        if ((dCoordFixBack3 < dCoord3 - gap) || (dCoordFixBack3 > dCoord3 + gap)) inTurnBack = false;
                    }
                }
            }
            thisTime = System.currentTimeMillis();
            if ((!inTurnBackPosNow) && (inTurnBack) && (thisTime - lastTurn > 1000)) { // we were not in turn, but we are now - so turn
                inTurnPosNow = false;
                inTurnBackPosNow = true;
                lastTurn = System.currentTimeMillis();
                tvTestArea.setText( "" + (Utils.parseInt(tvTestArea.getText().toString(), 1000) - 1));
            }
            if (!inTurnBack) inTurnBackPosNow = false;
        }
        BackgroundThread.instance().postGUI(() -> {
            checkTestArea();
        }, 200);
    }

    private void updateCoords() {
        if (closing) return;
        if (mCoolReader.mGravity != null)
            mGravity = mCoolReader.mGravity.clone();
        if (mGravity != null) {
            if (mGravity.length >= 0) {
                tvCoord1.setText("" + Math.round(mGravity[0] * 100));
            }
            if (mGravity.length >= 1) {
                tvCoord2.setText("" + Math.round(mGravity[1] * 100));
            }
            if (mGravity.length >= 2) {
                tvCoord3.setText("" + Math.round(mGravity[2] * 100));
            }
        }
        BackgroundThread.instance().postGUI(() -> {
            updateCoords();
        }, 500);
    }

    @Override
    protected void whenShow() {
        super.whenShow();
        updateCoords();
        checkTestArea();
    }

    @Override
    public void dismiss() {
        closing = true;
        super.dismiss();
    }

    @Override
    protected void onPositiveButtonClick() {
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_coord_1", getCheckedOption(ivCoord1FixUse)? "1": "0");
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_coord_2", getCheckedOption(ivCoord2FixUse)? "1": "0");
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_coord_3", getCheckedOption(ivCoord3FixUse)? "1": "0");
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_back_coord_1", getCheckedOption(ivCoord1FixBackUse)? "1": "0");
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_back_coord_2", getCheckedOption(ivCoord2FixBackUse)? "1": "0");
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".fix_turn_back_coord_3", getCheckedOption(ivCoord3FixBackUse)? "1": "0");
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".precision", "" + seekPrecision.getProgress());
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".coord_1", "" + tvCoord1Fix.getText());
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".coord_2", "" + tvCoord2Fix.getText());
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".coord_3", "" + tvCoord3Fix.getText());
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".back_coord_1", "" + tvCoord1FixBack.getText());
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".back_coord_2", "" + tvCoord2FixBack.getText());
        mProperties.setProperty(Settings.PROP_APP_DEVICE_TURN + ".back_coord_3", "" + tvCoord3FixBack.getText());
        super.onPositiveButtonClick();
    }

}
