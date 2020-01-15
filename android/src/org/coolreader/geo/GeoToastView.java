package org.coolreader.geo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;
import org.coolreader.geo.GeoLastData;
import org.coolreader.geo.MetroStation;
import org.coolreader.geo.TransportStop;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class GeoToastView {
    private static class Toast {
        private View anchor;
        private String msg;
        private int duration;
        private MetroStation metroStation;
        private TransportStop transportStop;
        private Double msDist;
        private Double tsDist;
        private MetroStation msBefore;
        private boolean sameStation;
        private boolean sameStop;

        private Toast(View anchor, String msg, int duration, MetroStation ms, TransportStop ts, Double msDist, Double tsDist, MetroStation msBefore,
                      boolean bSameStation, boolean bSameStop) {
            this.anchor = anchor;
            this.msg = msg;
            this.duration = duration;
            this.metroStation = ms;
            this.transportStop = ts;
            this.msDist = msDist;
            this.tsDist = tsDist;
            this.msBefore = msBefore;
            this.sameStation = bSameStation;
            this.sameStop = bSameStop;
        }
    }

	private static View mReaderView;
    private static LinkedBlockingQueue<Toast> queue = new LinkedBlockingQueue<Toast>();
    private static AtomicBoolean showing = new AtomicBoolean(false);
    private static Handler mHandler = new Handler();
    private static PopupWindow window = null;
    private static int colorGray;
    private static int colorGrayC;
    private static int colorIcon;
    private static BaseActivity mActivity;

    private static Runnable handleDismiss = new Runnable() {
        @Override
        public void run() {
            if (window != null) {
                window.dismiss();
                show();
            }
        }
    };

    static int fontSize = 24;
    public static void showToast(BaseActivity act, View anchor, String msg, int duration, int textSize,
                                 MetroStation ms, TransportStop ts, Double msDist, Double tsDist, MetroStation msBefore,
                                 boolean bSameStation, boolean bSameStop) {
        TypedArray a = act.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
        colorGray = a.getColor(0, Color.GRAY);
        colorGrayC = a.getColor(1, Color.GRAY);
        colorIcon = a.getColor(2, Color.GRAY);
        a.recycle();

        mReaderView = anchor;
        mActivity = act;
    	fontSize = textSize;
        try {
            queue.put(new Toast(anchor, msg, duration, ms, ts, msDist, tsDist, msBefore,
                bSameStation, bSameStop));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (showing.compareAndSet(false, true)) {
            show();
        }
    }

    @ColorInt
    static int darkenColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.7f;
        return Color.HSVToColor(hsv);
    }

    @ColorInt
    static int lightenColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 1.3f;
        return Color.HSVToColor(hsv);
    }

    @ColorInt
    public static int manipulateColor(@ColorInt int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r,255),
                Math.min(g,255),
                Math.min(b,255));
    }

    @ColorInt
    static int updColor(@ColorInt int color) {
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
        if(darkness<0.5){
            return manipulateColor(color, 0.8F); // It's a light color
        }else{
            return manipulateColor(color, 1.2F); // It's a dark color
        }
    }

    private static void show() {
        if (queue.size() == 0) {
            showing.compareAndSet(true, false);
            return;
        }
        Toast t = queue.poll();
        window = new PopupWindow(t.anchor.getContext());
        window.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
                    window.dismiss();
                    return true;
                }
                return false;
            }
        });
        window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
        window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        window.setTouchable(true);
        window.setFocusable(false);
        window.setOutsideTouchable(true);
        window.setBackgroundDrawable(null);
        LayoutInflater inflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        window.setContentView(inflater.inflate(R.layout.geo_toast, null, true));
        LinearLayout toast_ll = (LinearLayout) window.getContentView().findViewById(R.id.geo_toast_ll);
        //toast_ll.setBackgroundColor(colorGrayC);
        toast_ll.setBackgroundColor(Color.argb(220, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC)));
        TableLayout geoTable = (TableLayout) window.getContentView().findViewById(R.id.geo_table);
        if ((t.transportStop != null)||(t.metroStation != null)) {
            TableRow getSettTRow = (TableRow) inflater.inflate(R.layout.geo_sett_item, null);
            geoTable.addView(getSettTRow);
            Properties props = new Properties(mActivity.settings());
            int newTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
            Button btnGeoSett = (Button) window.getContentView().findViewById(R.id.btn_geo_sett);
            btnGeoSett.setText(mActivity.getString(R.string.options_app_geo_sett));
            btnGeoSett.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ((CoolReader) mActivity).optionsFilter = "";
                    ((CoolReader) mActivity).showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_APP_GEO);
                    window.dismiss();
                }
            });
            Button btnGeoOff = (Button) window.getContentView().findViewById(R.id.btn_geo_off);
            btnGeoOff.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (DeviceInfo.getSDKLevel() >= 9) {
                        int iSett = mActivity.settings().getInt(Settings.PROP_APP_GEO, 1);
                        Properties props1 = new Properties(mActivity.settings());
                        props1.setProperty(Settings.PROP_APP_GEO, "1");
                        mActivity.setSettings(props1, -1, true);
                        window.dismiss();
                    }
                }
            });
            btnGeoOff.setText(mActivity.getString(R.string.options_app_geo_off));
            Button[] btns = new Button[] {btnGeoSett, btnGeoOff};
            for (Button btn: btns) {
                btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
                //dicButton.setHeight(dicButton.getHeight()-4);
                btn.setTextColor(colorIcon);
                btn.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
                btn.setPadding(3, 3, 3, 3);
                //dicButton.setBackground(null);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                llp.setMargins(4, 1, 1, 4);
                //btn.setLayoutParams(llp);
                btn.setMaxLines(1);
                btn.setEllipsize(TextUtils.TruncateAt.END);
                mActivity.tintViewIcons(btn, true);
            }
        }
        if (t.metroStation != null) {
            TableRow getSepRow = (TableRow) inflater.inflate(R.layout.geo_sep_item, null);
            geoTable.addView(getSepRow);
            View sep = window.getContentView().findViewById(R.id.sepRow);
            sep.setBackgroundColor(updColor(colorIcon));
            TableRow getMetroTRow = (TableRow) inflater.inflate(R.layout.geo_metro_item, null);
            geoTable.addView(getMetroTRow);
            TextView tv = (TextView) window.getContentView().findViewById(R.id.center_station_name);
            tv.setTextSize(fontSize);
            if (!t.sameStation)
                tv.setTextColor(colorIcon);
            else
                tv.setTextColor(updColor(colorIcon));
            tv.setGravity(Gravity.CENTER);
            tv.setText(t.metroStation.name);
            View metro = window.getContentView().findViewById(R.id.metroColor);
            String sColor = GeoLastData.getStationHexColor(t.metroStation);
            metro.setBackgroundColor(Color.parseColor("#"+sColor));
            //float inPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, mActivity.getResources().getDisplayMetrics());
            //metro.setLayoutParams(new TableRow.LayoutParams((int) inPixels, (int) inPixels));
            TextView tvD = (TextView) window.getContentView().findViewById(R.id.center_station_dist);
            tvD.setTextSize(fontSize-4);
            tvD.setTextColor(updColor(colorIcon));
            tvD.setGravity(Gravity.CENTER);
            if (t.msDist != null)
                tvD.setText("(" + t.msDist.intValue() + "m)");
            else
                tvD.setText("");
            MetroStation prevSt = GeoLastData.getPrevNextStation(t.metroStation,true);
            MetroStation nextSt = GeoLastData.getPrevNextStation(t.metroStation,false);
            boolean bDetectedRoute = false;
            if (t.msBefore != null) {
                if (t.msBefore.equals(prevSt)) {
                    bDetectedRoute = true;
                }
                if (t.msBefore.equals(nextSt)) {
                    bDetectedRoute = true;
                    MetroStation dummy = prevSt;
                    prevSt = nextSt;
                    nextSt = dummy;
                }
            }
            if (prevSt != null) {
                TextView tvP = (TextView) window.getContentView().findViewById(R.id.left_station_name);
                tvP.setTextSize(fontSize - 4);
                tvP.setTextColor(updColor(colorIcon));
                tvP.setGravity(Gravity.CENTER);
                tvP.setText(prevSt.name);
            }
            if (nextSt != null) {
                TextView tvP = (TextView) window.getContentView().findViewById(R.id.right_station_name);
                tvP.setTextSize(fontSize - 4);
                tvP.setTextColor(updColor(colorIcon));
                tvP.setGravity(Gravity.CENTER);
                tvP.setText(nextSt.name);
            }
            if (bDetectedRoute) {
                TextView tvP = (TextView) window.getContentView().findViewById(R.id.left_mark_name);
                tvP.setText(">");
                TextView tvP2 = (TextView) window.getContentView().findViewById(R.id.right_mark_name);
                tvP2.setText(">");
            }
            List<String> interchangeColors = GeoLastData.getStationInterchangeColors(t.metroStation);
            if (interchangeColors.size()>0) {
                TableRow getInterchangesTRow = (TableRow) inflater.inflate(R.layout.geo_metro_interchange_item, null);
                geoTable.addView(getInterchangesTRow);
                LinearLayout llInter = (LinearLayout) window.getContentView().findViewById(R.id.interchanges_ll);
                for (String sCol: interchangeColors) {
                    View v = new View(mActivity);
                    v.setBackgroundColor(Color.parseColor("#"+sCol));
                    float inPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, mActivity.getResources().getDisplayMetrics());
                    v.setLayoutParams(new LinearLayout.LayoutParams((int) inPixels, (int) inPixels));
                    llInter.addView(v);
                }
            }
        }
        if (t.transportStop != null) {
            TableRow getSepRow = (TableRow) inflater.inflate(R.layout.geo_sep_item, null);
            geoTable.addView(getSepRow);
            View sep = window.getContentView().findViewById(R.id.sepRow);
            sep.setBackgroundColor(updColor(colorIcon));
            TableRow getStopTRow = (TableRow) inflater.inflate(R.layout.geo_stop_item, null);
            geoTable.addView(getStopTRow);
            TextView tv = (TextView) window.getContentView().findViewById(R.id.center_stop_name);
            String s = t.transportStop.name;
            s = s.replaceAll("\\([0-9]*\\)","").trim();
            tv.setTextSize(fontSize-2);
            if (!t.sameStop)
                tv.setTextColor(colorIcon);
            else
                tv.setTextColor(updColor(colorIcon));
            tv.setGravity(Gravity.CENTER);
            tv.setText(s);
            TextView tvD = (TextView) window.getContentView().findViewById(R.id.center_stop_dist);
            tvD.setTextSize(fontSize-4);
            tvD.setTextColor(updColor(colorIcon));
            tvD.setGravity(Gravity.CENTER);
            if (t.tsDist != null)
                tvD.setText("(" + t.tsDist.intValue() + "m)");
            else
                tvD.setText("");
            TextView tvP = (TextView) window.getContentView().findViewById(R.id.left_stop_name);
            tvP.setTextSize(fontSize - 4);
            tvP.setTextColor(updColor(colorIcon));
            tvP.setGravity(Gravity.CENTER);
            tvP.setText(t.transportStop.district);
            TextView tvStr = (TextView) window.getContentView().findViewById(R.id.stop_street);
            tvStr.setTextSize(fontSize-6);
            tvStr.setTextColor(updColor(colorIcon));
            tvStr.setGravity(Gravity.CENTER);
            tvStr.setText(t.transportStop.street);
            TextView tvP2 = (TextView) window.getContentView().findViewById(R.id.right_stop_name);
            tvP2.setTextSize(fontSize - 4);
            tvP2.setTextColor(updColor(colorIcon));
            tvP2.setGravity(Gravity.CENTER);
            tvP2.setText(t.transportStop.routeNumbers);
        }
        int [] location = new int[2];
        t.anchor.getLocationOnScreen(location);
        int popupY = location[1] + t.anchor.getHeight() - toast_ll.getHeight();
        window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
        mHandler.postDelayed(handleDismiss, t.duration == 0 ? 3000 : 5000);
    }
}
