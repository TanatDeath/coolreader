package org.coolreader.crengine;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: Victor Soskin
 * Date: 11/3/11
 * Time: 2:51 PM
 */
public class ToastView {
    private static class Toast {
        private View anchor;
        private String msg;
        private int duration;

        private Toast(View anchor, String msg, int duration) {
            this.anchor = anchor;
            this.msg = msg;
            this.duration = duration;
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
    public static void showToast(BaseActivity act, View anchor, String msg, int duration, int textSize) {
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
            queue.put(new Toast(anchor, msg, duration));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (showing.compareAndSet(false, true)) {
            show();
        }
    }

    private static void show() {
        if (queue.size() == 0) {
            showing.compareAndSet(true, false);
            return;
        }
        Toast t = queue.poll();
        window = new PopupWindow(t.anchor.getContext());
        window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
        window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        window.setTouchable(false);
        window.setFocusable(false);
        window.setOutsideTouchable(true);
        window.setBackgroundDrawable(null);
        /* LinearLayout ll = new LinearLayout(t.anchor.getContext());
        ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tv = new TextView(t.anchor.getContext());
        tv.setText(t.msg);
        ll.setGravity(Gravity.CENTER);
        ll.addView(tv);*/
        LayoutInflater inflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        window.setContentView(inflater.inflate(R.layout.custom_toast, null, true));
        LinearLayout toast_ll = (LinearLayout) window.getContentView().findViewById(R.id.toast_ll);
        toast_ll.setBackgroundColor(colorGrayC);
        TextView tv = (TextView) window.getContentView().findViewById(R.id.toast);
        tv.setTextSize(fontSize); //Integer.valueOf(Services.getSettings().getInt(ReaderView.PROP_FONT_SIZE, 20) ) );
        String msg = t.msg;
        if (msg.startsWith("*")) {
            msg=msg.substring(1);
            final String msg1 = msg;
            if (mActivity != null)
                if (mActivity instanceof CoolReader) {
                    tv.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            DictsDlg dlg = new DictsDlg((CoolReader) mActivity, ((CoolReader) mActivity).getReaderView(), msg1);
                            dlg.show();
                        }
                    });
                }
        }
        tv.setText(msg);

        tv.setTextColor(colorIcon);
        tv.setGravity(Gravity.CENTER);

        int [] location = new int[2];
        t.anchor.getLocationOnScreen(location);
        int popupY = location[1] + t.anchor.getHeight() - toast_ll.getHeight();
        window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
       // window.showAtLocation(t.anchor, Gravity.NO_GRAVITY, 0, 0);
        mHandler.postDelayed(handleDismiss, t.duration == 0 ? 2000 : 3000);
    }
}
