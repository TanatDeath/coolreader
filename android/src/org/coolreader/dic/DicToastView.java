package org.coolreader.dic;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.provider.Browser;
import android.support.annotation.ColorInt;
import android.text.ClipboardManager;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.StrUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DicToastView {
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
    private static boolean isYndx;
    private static String mDicName;

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
    public static void showToast(BaseActivity act, View anchor, String msg, int duration,
                                 boolean isYnd, String dicName) {
        TypedArray a = act.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
        colorGray = a.getColor(0, Color.GRAY);
        colorGrayC = a.getColor(1, Color.GRAY);
        colorIcon = a.getColor(2, Color.GRAY);
        a.recycle();

        mReaderView = anchor;
        mActivity = act;
    	isYndx = isYnd;
    	mDicName = dicName;
        try {
            queue.put(new Toast(anchor, msg, duration));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (showing.compareAndSet(false, true)) {
            show();
        }
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
                    showing.compareAndSet(true, false);
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
        window.setContentView(inflater.inflate(R.layout.dic_toast, null, true));
        LinearLayout toast_ll = (LinearLayout) window.getContentView().findViewById(R.id.dic_toast_ll);
        TextView tv = (TextView) window.getContentView().findViewById(R.id.dic_text);
        tv.setText(t.msg);
        //toast_ll.setBackgroundColor(colorGrayC);
        int tSize = mActivity.settings().getInt(ReaderView.PROP_FONT_SIZE, 20);
        if (tSize>0) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tSize);
        }
        tv.setOnClickListener(new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                mHandler.postDelayed(handleDismiss, 100);
            }
        });
        String sFace = mActivity.settings().getProperty(ReaderView.PROP_FONT_FACE, "");
        final String[] mFontFacesFiles = Engine.getFontFaceAndFileNameList();
        boolean found = false;
        for (int i=0; i<mFontFacesFiles.length; i++) {
            String s = mFontFacesFiles[i];
            if ((s.startsWith(sFace+"~"))&&(!s.toUpperCase().contains("BOLD"))&&(!s.toUpperCase().contains("ITALIC"))) {
                found = true;
                String sf = mFontFacesFiles[i];
                if (sf.contains("~")) {
                    sf = sf.split("~")[1];
                }
                try {
                    Typeface tf = null;
                    tf = Typeface.createFromFile(sf);
                    tv.setTypeface(tf);
                } catch (Exception e) {

                }
                break;
            }
        }
        if (!found)
            for (int i=0; i<mFontFacesFiles.length; i++) {
                String s = mFontFacesFiles[i];
                if (s.startsWith(sFace+"~")) {
                    found = true;
                    String sf = mFontFacesFiles[i];
                    if (sf.contains("~")) {
                        sf = sf.split("~")[1];
                    }
                    try {
                        Typeface tf = null;
                        tf = Typeface.createFromFile(sf);
                        tv.setTypeface(tf);
                    } catch (Exception e) {

                    }
                    break;
                }
            }
        toast_ll.setBackgroundColor(Color.argb(255, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC)));
        TableLayout dicTable = (TableLayout) window.getContentView().findViewById(R.id.dic_table);
        if (isYndx) {
            TableRow getSepRow = (TableRow) inflater.inflate(R.layout.geo_sep_item, null);
            dicTable.addView(getSepRow);
            View sep = window.getContentView().findViewById(R.id.sepRow);
            sep.setBackgroundColor(updColor(colorIcon));
            TableRow yndRow = (TableRow) inflater.inflate(R.layout.dic_ynd_item, null);
            dicTable.addView(yndRow);
            TextView tvYnd1 = (TextView) window.getContentView().findViewById(R.id.ynd_tv1);
            Integer tSizeI = tSize;
            Double tSizeD = Double.valueOf(tSizeI.doubleValue() *0.8);
            if (tSize > 0) {
                tvYnd1.setTextSize(TypedValue.COMPLEX_UNIT_PX, tSizeD.intValue());
            }
            tvYnd1.setPaintFlags(tvYnd1.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            tvYnd1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHandler.postDelayed(handleDismiss, 100);
                    Uri uri = Uri.parse("http://translate.yandex.ru/");
                    Context context = t.anchor.getContext();
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID,
                            context.getPackageName());
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {

                    }
                }
            });
            ImageButton btnTransl = (ImageButton) window.getContentView().findViewById(R.id.btnTransl);
            final CoolReader cr = (CoolReader)mActivity;
            btnTransl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (cr.getReaderView().mBookInfo!=null) {
                        String lang = StrUtils.getNonEmptyStr(cr.getReaderView().mBookInfo.getFileInfo().lang_to,true);
                        String langf = StrUtils.getNonEmptyStr(cr.getReaderView().mBookInfo.getFileInfo().lang_from, true);
                        FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
                        FileInfo dfi = fi.parent;
                        if (dfi == null) {
                            dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
                        }
                        if (dfi != null) {
                            cr.editBookTransl(dfi, fi, langf, lang, "", null, TranslationDirectionDialog.FOR_COMMON);
                        }
                    };
                    window.dismiss();
                    showing.compareAndSet(true, false);
                }
            });
            ImageButton btnToUserDic = (ImageButton) window.getContentView().findViewById(R.id.btn_to_user_dic);
            btnToUserDic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (cr.getReaderView().mBookInfo!=null) {
                        if (cr.getReaderView().lastSelection!=null) {
                            if (!cr.getReaderView().lastSelection.isEmpty()) {
                                cr.getReaderView().clearSelection();
                                cr.getReaderView().showNewBookmarkDialog(cr.getReaderView().lastSelection, Bookmark.TYPE_USER_DIC, t.msg);
                            }
                        }
                    };
                    window.dismiss();
                    showing.compareAndSet(true, false);
                }
            });
            ImageButton btnCopyToCb = (ImageButton) window.getContentView().findViewById(R.id.btn_copy_to_cb);
            btnCopyToCb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager cm = mActivity.getClipboardmanager();
                    String s = StrUtils.getNonEmptyStr(t.msg,true);
                    if (cr.getReaderView().lastSelection != null) {
                        if (s.startsWith(cr.getReaderView().lastSelection.text+":")) s = s.substring(cr.getReaderView().lastSelection.text.length()+1);
                    }
                    cm.setText(StrUtils.getNonEmptyStr(s,true));
                    window.dismiss();
                    showing.compareAndSet(true, false);
                }
            });
            mActivity.tintViewIcons(yndRow,true);
        } else
            if (!StrUtils.getNonEmptyStr(mDicName,true).equals("[HIDE]"))
            {
                TableRow getSepRow = (TableRow) inflater.inflate(R.layout.geo_sep_item, null);
                dicTable.addView(getSepRow);
                View sep = window.getContentView().findViewById(R.id.sepRow);
                sep.setBackgroundColor(updColor(colorIcon));
                TableRow yndRow = (TableRow) inflater.inflate(R.layout.dic_ynd_item, null);
                dicTable.addView(yndRow);
                TextView tvYnd1 = (TextView) window.getContentView().findViewById(R.id.ynd_tv1);
                tvYnd1.setText(mDicName);
                Integer tSizeI = tSize;
                Double tSizeD = Double.valueOf(tSizeI.doubleValue() *0.8);
                if (tSize > 0) {
                    tvYnd1.setTextSize(TypedValue.COMPLEX_UNIT_PX, tSizeD.intValue());
                }
                tvYnd1.setPaintFlags(tvYnd1.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                tvYnd1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mHandler.postDelayed(handleDismiss, 100);
                        Uri uri = Uri.parse("https://www.lingvo.ru/");
                        Context context = t.anchor.getContext();
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID,
                                context.getPackageName());
                        try {
                            context.startActivity(intent);
                        } catch (Exception e) {

                        }
                    }
                });
                ImageButton btnTransl = (ImageButton) window.getContentView().findViewById(R.id.btnTransl);
                final CoolReader cr = (CoolReader)mActivity;
                btnTransl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cr.getReaderView().mBookInfo!=null) {
                            String lang = StrUtils.getNonEmptyStr(cr.getReaderView().mBookInfo.getFileInfo().lang_to,true);
                            String langf = StrUtils.getNonEmptyStr(cr.getReaderView().mBookInfo.getFileInfo().lang_from, true);
                            FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
                            FileInfo dfi = fi.parent;
                            if (dfi == null) {
                                dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
                            }
                            if (dfi != null) {
                                cr.editBookTransl(dfi, fi, langf, lang, "", null, TranslationDirectionDialog.FOR_COMMON);
                            }
                        };
                        window.dismiss();
                        showing.compareAndSet(true, false);
                    }
                });
                ImageButton btnToUserDic = (ImageButton) window.getContentView().findViewById(R.id.btn_to_user_dic);
                btnToUserDic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cr.getReaderView().mBookInfo!=null) {
                            if (cr.getReaderView().lastSelection!=null) {
                                if (!cr.getReaderView().lastSelection.isEmpty()) {
                                    cr.getReaderView().clearSelection();
                                    cr.getReaderView().showNewBookmarkDialog(cr.getReaderView().lastSelection, Bookmark.TYPE_USER_DIC, t.msg);
                                }
                            }
                        };
                        window.dismiss();
                        showing.compareAndSet(true, false);
                    }
                });
                ImageButton btnCopyToCb = (ImageButton) window.getContentView().findViewById(R.id.btn_copy_to_cb);
                btnCopyToCb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager cm = mActivity.getClipboardmanager();
                        String s = StrUtils.getNonEmptyStr(t.msg,true);
                        if (cr.getReaderView().lastSelection != null) {
                            if (s.startsWith(cr.getReaderView().lastSelection.text+":")) s = s.substring(cr.getReaderView().lastSelection.text.length()+1);
                        }
                        cm.setText(StrUtils.getNonEmptyStr(s,true));
                        window.dismiss();
                        showing.compareAndSet(true, false);
                    }
                });
                mActivity.tintViewIcons(yndRow,true);
            }
        int [] location = new int[2];
        t.anchor.getLocationOnScreen(location);
        int popupY = location[1] + t.anchor.getHeight() - toast_ll.getHeight();
        window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
       // mHandler.postDelayed(handleDismiss, t.duration == 0 ? 3000 : 5000);
    }
}
