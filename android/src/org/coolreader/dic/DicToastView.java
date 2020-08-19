package org.coolreader.dic;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.provider.Browser;
import android.support.annotation.ColorInt;
import android.text.ClipboardManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.MaxHeightLinearLayout;
import org.coolreader.crengine.MaxHeightScrollView;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DicToastView {

    public static int IS_YANDEX = 0;
    public static int IS_LINGVO = 1;
    public static int IS_WIKI = 2;

    public static int mColorIconL = Color.GRAY;

    private static class Toast {
        private View anchor;
        private String msg;
        private int duration;
        private ArrayList<WikiArticle> arrWA;
        WikiArticlesList mList = null;

        private Toast(View anchor, String msg, int duration, ArrayList<WikiArticle> arrWA) {
            this.anchor = anchor;
            this.msg = msg;
            this.duration = duration;
            this.arrWA = arrWA;
        }
    }

	private static View mReaderView;
    private static LinkedBlockingQueue<Toast> queue = new LinkedBlockingQueue<Toast>();
    private static Toast curToast = null;
    private static AtomicBoolean showing = new AtomicBoolean(false);
    private static Handler mHandler = new Handler();
    private static PopupWindow window = null;
    private static int colorGray;
    private static int colorGrayC;
    private static int colorIcon;
    private static BaseActivity mActivity;
    private static int dicType;
    private static String mDicName;
    private static LayoutInflater mInflater;
    private static String sFindText;
    private static int mListSkipCount = 0;
    private static Dictionaries.DictInfo mCurDict;
    private static String mLink;
    private static String mLink2;
    private static int mCurAction;

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

    static class WikiArticlesAdapter extends BaseAdapter {
        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEnabled(int arg0) {
            return true;
        }

        public int getCount() {
            if (curToast == null)
                return 0;
            else if (curToast.arrWA == null)
                return 0;
            else return curToast.arrWA.size();
        }

        public Object getItem(int position) {
            if (curToast == null)
                return 0;
            if (curToast.arrWA == null)
                return 0;
               if (position < 0 || position >= curToast.arrWA.size())
                    return null;
                return curToast.arrWA.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public final static int ITEM_POSITION=0;

        public int getItemViewType(int position) {
            return ITEM_POSITION;
        }

        public void setHighLightedText(TextView tv, String textToHighlight) {
            if (StrUtils.isEmptyStr(textToHighlight)) return;
            String tvt = tv.getText().toString();
            if (StrUtils.isEmptyStr(tvt)) return;
            int ofe = tvt.toUpperCase().indexOf(textToHighlight.toUpperCase(), 0);
            Spannable wordToSpan = new SpannableString(tv.getText());
            for (int ofs = 0; ofs < tvt.length() && ofe != -1; ofs = ofe + 1) {
                ofe = tvt.toUpperCase().indexOf(textToHighlight.toUpperCase(), ofs);
                if (ofe == -1)
                    break;
                else {
                    // set color here
                    wordToSpan.setSpan(new BackgroundColorSpan(mColorIconL), ofe, ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
                }
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            //if (mInflater == null) return null;
            int res = R.layout.wiki_articles_item;
            view = mInflater.inflate(res, null);
            TextView labelView = (TextView)view.findViewById(R.id.page_item_title);
            TextView titleTextView = (TextView)view.findViewById(R.id.page_item_snippet);
            String sTitle = "";
            String sSnippet = "";
            WikiArticle wa = (WikiArticle) getItem(position);
            boolean bNot = wa == null;
            if (!bNot) {
                sTitle = wa.title;
                sSnippet = Utils.cleanupHtmlTags(wa.snippet);
                labelView.setText(sTitle);
                titleTextView.setText(sSnippet);
                if (!StrUtils.isEmptyStr(sFindText))
                    setHighLightedText(titleTextView,sFindText);
            } else {
                labelView.setText(sTitle);
                titleTextView.setText(sSnippet);
            }
            return view;
        }

        public boolean hasStableIds() {
            return true;
        }

        public boolean isEmpty() {
            if (curToast == null)
                return true;
            else if (curToast.arrWA == null)
                return true;
            else return curToast.arrWA.size() == 0;
        }

        private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

        public void registerDataSetObserver(DataSetObserver observer) {
            observers.add(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            observers.remove(observer);
        }
    }

    static class WikiArticlesList extends BaseListView {

        private ArrayList<WikiArticle> arrWA;

        public WikiArticlesList(Context context, ArrayList<WikiArticle> arrWA ) {
            super(context, true);
            this.arrWA = arrWA;
            setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            setLongClickable(true);
            setAdapter(new WikiArticlesAdapter());
            setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                               int position, long arg3) {
                    //openContextMenu(DictList.this);
                    return true;
                }
            });
        }

        @Override
        public boolean performItemClick(View view, int position, long id) {
            WikiArticle wa = arrWA.get(position);
            Dictionaries dicts = new Dictionaries(mActivity);
            dicts.wikiTranslate((CoolReader) mActivity, mCurDict, null, String.valueOf(wa.pageId)+"~"+sFindText, mLink, mLink2,
                    mCurAction == Dictionaries.WIKI_FIND_LIST ? Dictionaries.WIKI_SHOW_PAGE_ID : Dictionaries.WIKI_SHOW_PAGE_ID_2);
            mHandler.postDelayed(handleDismiss, 100);
            return true;
        }
    }
    
    private static void showToastInternal(BaseActivity act, View anchor, String msg, int duration,
                                 int dicT, String dicName, ArrayList<WikiArticle> arrWA,
                                 Dictionaries.DictInfo curDict, String link, String link2, int curAction) {
        TypedArray a = act.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon, R.attr.colorIconL});
        colorGray = a.getColor(0, Color.GRAY);
        colorGrayC = a.getColor(1, Color.GRAY);
        colorIcon = a.getColor(2, Color.GRAY);
        mColorIconL = a.getColor(0, Color.GRAY);

        a.recycle();

        mReaderView = anchor;
        mActivity = act;
        dicType = dicT;
        mDicName = dicName;
        mCurDict = curDict;
        mLink = link;
        mLink2 = link2;
        mCurAction = curAction;
        try {
            queue.put(new Toast(anchor, msg, duration, arrWA));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (showing.compareAndSet(false, true)) {
            show();
        }
    }

    public static void showToast(BaseActivity act, View anchor, String s, String msg, int duration,
                                 int dicT, String dicName) {
        sFindText = s;
        showToastInternal(act, anchor, msg, duration, dicT, dicName, null,
                null, null, null, 0);
    }

    public static void showToastWiki(BaseActivity act, View anchor, String s, String msg, int duration,
                                 int dicT, String dicName,
                                 Dictionaries.DictInfo curDict, String link, String link2, int curAction) {
        sFindText = s;
        showToastInternal(act, anchor, msg, duration, dicT, dicName, null,
                curDict, link, link2, curAction);
    }

    public static void showWikiListToast(BaseActivity act, View anchor, String s, String msg,
                                         int dicT, String dicName, ArrayList<WikiArticle> arrWA,
                                         Dictionaries.DictInfo curDict, String link, String link2, int curAction,
                                         int listSkipCount) {
        sFindText = s;
        mListSkipCount = listSkipCount;
        showToastInternal(act, anchor, msg, android.widget.Toast.LENGTH_LONG, dicT, dicName,
                arrWA, curDict, link, link2, curAction);
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
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    window.dismiss();
                    showing.compareAndSet(true, false);
                    return true;
                }
                return false;
            }
        });
        curToast = t;
        if (t.arrWA == null) simpleToast(t);
        else wikiListToast(t);
    }

    private static void simpleToast(Toast t) {
        window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
        window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        window.setTouchable(true);
        window.setFocusable(false);
        window.setOutsideTouchable(true);
        window.setBackgroundDrawable(null);
        mInflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        window.setContentView(mInflater.inflate(R.layout.dic_toast, null, true));
        TableRow tr1 = window.getContentView().findViewById(R.id.tr_upper_row);
        TableRow tr2 = window.getContentView().findViewById(R.id.tr_upper_sep_row);
        MaxHeightScrollView sv =  window.getContentView().findViewById(R.id.dic_scrollV);
        CoolReader cr=(CoolReader) mActivity;
        if (cr.getReaderView() != null)
            if (cr.getReaderView().getSurface() != null) {
                sv.setMaxHeight(cr.getReaderView().getSurface().getHeight() * 3 / 4);
            }
        //TableRow tr3 = window.getContentView().findViewById(R.id.dic_row);
        //LinearLayout dicLL = window.getContentView().findViewById(R.id.dic_ll);
        TextView tvMore = window.getContentView().findViewById(R.id.upper_row_tv_more);
        TextView tvClose = window.getContentView().findViewById(R.id.upper_row_tv_close);
        TextView tvFull = window.getContentView().findViewById(R.id.upper_row_tv_full);
        tvMore.setPaintFlags(tvMore.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dictionaries dicts = new Dictionaries(mActivity);
                String ss = sFindText;
                if (ss.contains("~")) ss = ss.split("~")[1];
                dicts.wikiTranslate((CoolReader) mActivity, mCurDict, mReaderView, ss, mLink, mLink2,
                        ((mCurAction == Dictionaries.WIKI_SHOW_PAGE_ID) ||
                         (mCurAction == Dictionaries.WIKI_SHOW_PAGE_FULL_ID)) ? Dictionaries.WIKI_FIND_LIST : Dictionaries.WIKI_FIND_LIST_2);
                mHandler.postDelayed(handleDismiss, 100);
            }
        });
        tvClose.setPaintFlags(tvClose.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.postDelayed(handleDismiss, 100);
            }
        });
        tvFull.setPaintFlags(tvClose.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dictionaries dicts = new Dictionaries(mActivity);
                if ((mCurAction == Dictionaries.WIKI_FIND_TITLE) || (mCurAction == Dictionaries.WIKI_FIND_TITLE_LINK_2))
                    dicts.wikiTranslate((CoolReader) mActivity, mCurDict, mReaderView, sFindText, mLink, mLink2,
                            mCurAction == Dictionaries.WIKI_FIND_TITLE ? Dictionaries.WIKI_FIND_TITLE_FULL : Dictionaries.WIKI_FIND_TITLE_FULL_LINK_2);
                else
                    dicts.wikiTranslate((CoolReader) mActivity, mCurDict, mReaderView, sFindText, mLink, mLink2,
                        mCurAction == Dictionaries.WIKI_SHOW_PAGE_ID ? Dictionaries.WIKI_SHOW_PAGE_FULL_ID : Dictionaries.WIKI_SHOW_PAGE_FULL_ID_2);
                mHandler.postDelayed(handleDismiss, 100);
            }
        });
        if ((mCurAction == Dictionaries.WIKI_SHOW_PAGE_FULL_ID)||(mCurAction == Dictionaries.WIKI_SHOW_PAGE_FULL_ID_2)||
                (mCurAction == Dictionaries.WIKI_FIND_TITLE_FULL)||(mCurAction == Dictionaries.WIKI_FIND_TITLE_FULL_LINK_2))
            ((ViewGroup) tvFull.getParent()).removeView(tvFull);
        if (dicType != IS_WIKI) {
            ((ViewGroup) tr1.getParent()).removeView(tr1);
            ((ViewGroup) tr2.getParent()).removeView(tr2);
        }
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
        if (dicType == IS_YANDEX) {
            TableRow getSepRow = (TableRow) mInflater.inflate(R.layout.geo_sep_item, null);
            dicTable.addView(getSepRow);
            View sep = window.getContentView().findViewById(R.id.sepRow);
            sep.setBackgroundColor(updColor(colorIcon));
            TableRow yndRow = (TableRow) mInflater.inflate(R.layout.dic_ynd_item, null);
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
                TableRow getSepRow = (TableRow) mInflater.inflate(R.layout.geo_sep_item, null);
                dicTable.addView(getSepRow);
                View sep = window.getContentView().findViewById(R.id.sepRow);
                sep.setBackgroundColor(updColor(colorIcon));
                TableRow yndRow = (TableRow) mInflater.inflate(R.layout.dic_ynd_item, null);
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
                        String sLink = "";
                        if (dicType == IS_LINGVO) sLink = "https://www.lingvo.ru/";
                        if (dicType == IS_WIKI) sLink = mDicName;
                        Uri uri = Uri.parse(sLink);
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

    private static void wikiListToast(Toast t) {
        window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
        window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        window.setTouchable(true);
        window.setFocusable(false);
        window.setOutsideTouchable(true);
        window.setBackgroundDrawable(null);
        mInflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        window.setContentView(mInflater.inflate(R.layout.wiki_articles_dlg, null, true));
        LinearLayout ll1 = window.getContentView().findViewById(R.id.articles_list_ll1);
        ll1.setBackgroundColor(Color.argb(255, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC)));
        TextView tvMore = window.getContentView().findViewById(R.id.upper_row_tv_more);
        TextView tvClose = window.getContentView().findViewById(R.id.upper_row_tv_close);
        tvMore.setPaintFlags(tvMore.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dictionaries dicts = new Dictionaries(mActivity);
                String ss = sFindText;
                if (ss.contains("~")) ss = ss.split("~")[1];
                dicts.wikiTranslate((CoolReader) mActivity, mCurDict, mReaderView, ss, mLink, mLink2,
                        mCurAction, mListSkipCount + t.arrWA.size());
                mHandler.postDelayed(handleDismiss, 100);
            }
        });
        int sz = 0;
        if (t.arrWA != null) sz = t.arrWA.size();
        if (sz == 0) ((ViewGroup) tvMore.getParent()).removeView(tvMore);
        tvClose.setPaintFlags(tvClose.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.postDelayed(handleDismiss, 100);
            }
        });
        ViewGroup body = window.getContentView().findViewById(R.id.articles_list);
        CoolReader cr=(CoolReader) mActivity;
        if (cr.getReaderView() != null)
            if (cr.getReaderView().getSurface() != null) {
                ((MaxHeightLinearLayout)body).setMaxHeight(cr.getReaderView().getSurface().getHeight() * 3 / 4);
            }
        t.mList = new WikiArticlesList(mActivity, t.arrWA);
        body.addView(t.mList);
        int [] location = new int[2];
        t.anchor.getLocationOnScreen(location);
        int ll1_heig = ll1.getHeight();
        int popupY = location[1] + t.anchor.getHeight() - ll1.getHeight();
        window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
        // mHandler.postDelayed(handleDismiss, t.duration == 0 ? 3000 : 5000);
    }
}
