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
import androidx.annotation.ColorInt;
import android.text.ClipboardManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.DictsDlg;
import org.coolreader.crengine.DownloadImageTask;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.MaxHeightLinearLayout;
import org.coolreader.crengine.MaxHeightScrollView;
import org.coolreader.readerview.ReaderView;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.dic.struct.DictEntry;
import org.coolreader.dic.struct.ExampleLine;
import org.coolreader.dic.struct.LinePair;
import org.coolreader.dic.struct.TranslLine;
import org.coolreader.dic.wiki.WikiArticle;
import org.coolreader.dic.wiki.WikiArticles;
import org.coolreader.dic.wiki.WikiSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.co.deanwild.flowtextview.FlowTextView;

public class DicToastView {

    public static int IS_YANDEX = 0;
    public static int IS_LINGVO = 1;
    public static int IS_WIKI = 2;
    public static int IS_DEEPL = 3;
    public static int IS_DICTCC = 4;
    public static int IS_GOOGLE = 5;
    public static int IS_LINGUEE = 6;
    public static int IS_GRAMOTA = 7;
    public static int IS_GLOSBE = 8;
    public static int IS_TURENG = 9;
    public static int IS_URBAN = 10;

    public static int mColorIconL = Color.GRAY;
    public static PopupWindow curWindow = null;

    private static Dictionaries.DictInfo mListCurDict;
    private static String mListLink;
    private static String mListLink2;
    private static boolean mListUseFirstLink;

    private static class Toast {
        private View anchor;
        private String sFindText;
        private String msg;
        private int duration;
        private WikiArticles wikiArticles = null;
        private DicStruct dicStruct = null;
        WikiArticlesList mWikiArticlesList = null;
        ExtDicList mExtDicList = null;
        private int dicType;
        private String mDicName;
        private Dictionaries.DictInfo mCurDict;
        private String mLink;
        private String mLink2;
        private int mCurAction;
        private boolean mUseFirstLink;
        private String mPicAddr;

        private Toast(View anchor,
                      String sFindText, String msg, int duration, Object dicStructObject,
                      int dicType, String dicName, Dictionaries.DictInfo curDict,
                      String link, String link2, int curAction, boolean useFirstLink,
                      String picAddr) {
            this.anchor = anchor;
            this.sFindText = sFindText;
            this.msg = msg;
            this.duration = duration;
            if (dicStructObject instanceof WikiArticles)
                this.wikiArticles = (WikiArticles) dicStructObject;
            if (dicStructObject instanceof DicStruct)
                this.dicStruct = (DicStruct) dicStructObject;
            this.dicType = dicType;
            this.mDicName = dicName;
            this.mCurDict = curDict;
            this.mLink = link;
            this. mLink2 = link2;
            this.mCurAction = curAction;
            this.mUseFirstLink = useFirstLink;
            this.mPicAddr = picAddr;
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
    private static CoolReader mActivity;
    private static LayoutInflater mInflater;
    private static String sFindText;
    private static int mListSkipCount = 0;

    private static void dismissAndCheckDicList() {
        if (!StrUtils.isEmptyStr(mActivity.lastDicText)) {
            if (!mActivity.lastDicSkip) {
                BackgroundThread.instance().postGUI(() -> {
                            DictsDlg dlg = new DictsDlg(mActivity, mActivity.getReaderView(),
                                    mActivity.lastDicText, null, true);
                            dlg.show();
                        }
                        , 300);
            }
            mActivity.lastDicSkip = false;
        }
        window.dismiss();
    }

    private static Runnable handleDismiss = () -> {
        if (window != null) {
            window.dismiss();
            curWindow=null;
            show();
            if (queue.size() == 0) dismissAndCheckDicList();
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
            else if (curToast.wikiArticles == null)
                return 0;
            else return curToast.wikiArticles.wikiArticleList.size();
        }

        public Object getItem(int position) {
            if (curToast == null)
                return 0;
            if (curToast.wikiArticles == null)
                return 0;
               if (position < 0 || position >= curToast.wikiArticles.wikiArticleList.size())
                    return null;
                return curToast.wikiArticles.wikiArticleList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public final static int ITEM_POSITION=0;

        public int getItemViewType(int position) {
            return ITEM_POSITION;
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
            } else {
                labelView.setText(sTitle);
                titleTextView.setText(sSnippet);
            }
            if (!StrUtils.isEmptyStr(sFindText))
                Utils.setHighLightedText(titleTextView, sFindText, mColorIconL);
            return view;
        }

        public boolean hasStableIds() {
            return true;
        }

        public boolean isEmpty() {
            if (curToast == null)
                return true;
            else if (curToast.wikiArticles == null)
                return true;
            else return curToast.wikiArticles.wikiArticleList.size() == 0;
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

        private List<WikiArticle> arrWA;

        public WikiArticlesList(Context context, List<WikiArticle> arrWA ) {
            super(context, true);
            this.arrWA = arrWA;
            setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            setLongClickable(true);
            setAdapter(new WikiArticlesAdapter());
            setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
                //openContextMenu(DictList.this);
                return true;
            });
        }

        @Override
        public boolean performItemClick(View view, int position, long id) {
            WikiArticle wa = arrWA.get(position);
            if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
            ((CoolReader) mActivity).lastDicSkip = true;
            Dictionaries.wikiSearch.wikiTranslate((CoolReader) mActivity, mListCurDict, null,
                    wa.pageId +"~"+sFindText, mListLink, mListLink2,
                    Dictionaries.wikiSearch.WIKI_SHOW_PAGE_ID, mListUseFirstLink, null);
            mHandler.postDelayed(handleDismiss, 100);
            return true;
        }
    }

    public static void hideToast(BaseActivity act) {
        try {
            mHandler.postDelayed(handleDismiss, 100);
        } catch (Exception e) {

        }
    }
    
    private static void showToastInternal(BaseActivity act, View anchor, String msg, int duration,
                                 int dicT, String dicName, Object dicStructObject,
                                 Dictionaries.DictInfo curDict, String link, String link2, int curAction,
                                 boolean useFirstLink, String picAddr) {
        TypedArray a = act.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon, R.attr.colorIconL});
        colorGray = a.getColor(0, Color.GRAY);
        colorGrayC = a.getColor(1, Color.GRAY);
        colorIcon = a.getColor(2, Color.GRAY);
        mColorIconL = a.getColor(0, Color.GRAY);

        a.recycle();

        mReaderView = anchor;
        mActivity = (CoolReader) act;
        mListCurDict = curDict;
        mListLink = link;
        mListLink2 = link2;
        mListUseFirstLink = useFirstLink;
        try {
            queue.put(new Toast(anchor, sFindText, msg,
                    duration, dicStructObject, dicT, dicName, curDict, link, link2, curAction, useFirstLink, picAddr));
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
                null, null, null, 0, false, "");
    }

    public static void showToastExt(BaseActivity act, View anchor, String s, String msg, int duration,
                                 int dicT, String dicName, Dictionaries.DictInfo curDict, Object dicStructObject) {
        sFindText = s;
        showToastInternal(act, anchor, msg, duration, dicT, dicName, dicStructObject,
                curDict, null, null, 0, false, "");
    }

    public static void showToastWiki(BaseActivity act, View anchor, String s, String msg, int duration,
                                 int dicT, String dicName,
                                 Dictionaries.DictInfo curDict, String link, String link2, int curAction,
                                 boolean useFirstLink,
                                 String picAddr) {
        sFindText = s;
        showToastInternal(act, anchor, msg, duration, dicT, dicName, null,
                curDict, link, link2, curAction, useFirstLink, picAddr);
    }

    public static void showWikiListToast(BaseActivity act, View anchor, String s, String msg,
                                         int dicT, String dicName, ArrayList<WikiArticle> arrWA,
                                         Dictionaries.DictInfo curDict, String link, String link2, int curAction,
                                         int listSkipCount, boolean useFirstLink) {
        sFindText = s;
        mListSkipCount = listSkipCount;
        showToastInternal(act, anchor, msg, android.widget.Toast.LENGTH_LONG, dicT, dicName,
                arrWA, curDict, link, link2, curAction, useFirstLink, "");
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
        curWindow = window;
        window.setTouchInterceptor((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                if (mActivity.getmReaderView() != null)
                    mActivity.getmReaderView().disableTouch = true;
                dismissAndCheckDicList();
                curWindow = null;
                showing.compareAndSet(true, false);
                return true;
            }
            return false;
        });
        curToast = t;
        if (t.wikiArticles != null) wikiListToast(t);
            else if (t.dicStruct != null) extListToast(t);
                else simpleToast(t);
    }

    private static void simpleToast(Toast t) {
        window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
        window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        window.setTouchable(true);
        window.setFocusable(false);
        window.setOutsideTouchable(true);
        window.setBackgroundDrawable(null);
        mInflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (StrUtils.isEmptyStr(t.mPicAddr))
            window.setContentView(mInflater.inflate(R.layout.dic_toast, null, true));
        else
            window.setContentView(mInflater.inflate(R.layout.dic_toast_with_pic, null, true));
        TableRow tr1 = window.getContentView().findViewById(R.id.tr_upper_row);
        TableRow tr2 = window.getContentView().findViewById(R.id.tr_upper_sep_row);
        MaxHeightScrollView sv =  window.getContentView().findViewById(R.id.dic_scrollV);
        CoolReader cr=(CoolReader) mActivity;
        if (cr.getReaderView() != null) {
            if (cr.getReaderView().getSurface() != null) {
                sv.setMaxHeight(cr.getReaderView().getSurface().getHeight() * 3 / 4);
            }
        } else {
            sv.setMaxHeight(t.anchor.getHeight() * 3 / 4);
        }
        //TableRow tr3 = window.getContentView().findViewById(R.id.dic_row);
        //LinearLayout dicLL = window.getContentView().findViewById(R.id.dic_ll);
        int colorGray;
        TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2});
        colorGray = a.getColor(0, Color.GRAY);
        a.recycle();
        int colr2 = colorGray;
        Button tvMore = window.getContentView().findViewById(R.id.upper_row_tv_more);
        tvMore.setBackgroundColor(colr2);
        Button tvClose = window.getContentView().findViewById(R.id.upper_row_tv_close);
        tvClose.setBackgroundColor(colr2);
        Button tvFull = window.getContentView().findViewById(R.id.upper_row_tv_full);
        tvFull.setBackgroundColor(colr2);
        Button tvFullWeb = window.getContentView().findViewById(R.id.upper_row_tv_full_web);
        tvFullWeb.setBackgroundColor(colr2);
        TextView tvLblDic = window.getContentView().findViewById(R.id.lbl_dic);
        if (tvLblDic != null) {
            if (t.mCurDict != null)
                tvLblDic.setText(t.mCurDict.shortName);
            else {
                if (StrUtils.getNonEmptyStr(t.mDicName, true).equals("[HIDE]")) tvLblDic.setText("");
                else tvLblDic.setText(t.mDicName);
            }
        }
        //tvMore.setPaintFlags(tvMore.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvMore.setOnClickListener(v -> {
            String ss = sFindText;
            if (ss.contains("~")) ss = ss.split("~")[1];
            if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
            cr.lastDicSkip = true;
            Dictionaries.wikiSearch.wikiTranslate((CoolReader) mActivity, t.mCurDict, mReaderView, ss, t.mLink, t.mLink2,
                     Dictionaries.wikiSearch.WIKI_FIND_LIST, t.mUseFirstLink, null);
            mHandler.postDelayed(handleDismiss, 100);
        });
        if (t.dicType != IS_WIKI) ((ViewGroup)tvMore.getParent()).removeView(tvMore);
        //tvClose.setPaintFlags(tvClose.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvClose.setOnClickListener(v -> mHandler.postDelayed(handleDismiss, 100));
        //tvFull.setPaintFlags(tvClose.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvFull.setOnClickListener(v -> {
            if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
            ((CoolReader) mActivity).lastDicSkip = true;
            if (t.mCurAction == Dictionaries.wikiSearch.WIKI_FIND_TITLE)
                Dictionaries.wikiSearch.wikiTranslate((CoolReader) mActivity, t.mCurDict, mReaderView, sFindText, t.mLink, t.mLink2,
                        Dictionaries.wikiSearch.WIKI_FIND_TITLE_FULL, t.mUseFirstLink, null);
            else
                Dictionaries.wikiSearch.wikiTranslate((CoolReader) mActivity, t.mCurDict, mReaderView, sFindText, t.mLink, t.mLink2,
                    Dictionaries.wikiSearch.WIKI_SHOW_PAGE_FULL_ID, t.mUseFirstLink, null);
            mHandler.postDelayed(handleDismiss, 100);
        });
        //tvFullWeb.setPaintFlags(tvClose.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvFullWeb.setOnClickListener(v -> {
            String link =
                    t.mUseFirstLink ? t.mLink : t.mLink2;
            String sFt = sFindText;
            if (sFt.contains("~")) {
                sFt = sFt.split("~")[0];
                link = link + "/?curid=" + sFt;
            } else {
                link = link + "/w/index.php?search="+sFt;
            }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link));
            mActivity.startActivity(i);
            mHandler.postDelayed(handleDismiss, 100);
        });
        if (t.dicType != IS_WIKI) ((ViewGroup)tvFullWeb.getParent()).removeView(tvFullWeb);
        if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
        if ((t.mCurAction == Dictionaries.wikiSearch.WIKI_SHOW_PAGE_FULL_ID)||
                (t.mCurAction == Dictionaries.wikiSearch.WIKI_FIND_TITLE_FULL)||
                (t.dicType != IS_WIKI))
            ((ViewGroup) tvFull.getParent()).removeView(tvFull);
        //if (t.dicType != IS_WIKI) {
        //    ((ViewGroup) tr1.getParent()).removeView(tr1);
        //    ((ViewGroup) tr2.getParent()).removeView(tr2);
        //}
        LinearLayout toast_ll = (LinearLayout) window.getContentView().findViewById(R.id.dic_toast_ll);
        TextView tv = null;
        FlowTextView tv2 = null;
        if (StrUtils.isEmptyStr(t.mPicAddr)) {
            tv = window.getContentView().findViewById(R.id.dic_text);
            tv.setText(t.msg);
            if (!StrUtils.isEmptyStr(sFindText)) Utils.setHighLightedText(tv,sFindText, mColorIconL);
        } else {
            tv2 = window.getContentView().findViewById(R.id.dic_text_flow);
            tv2.setText(t.msg);
        }
        if (tv2 != null) {
            ImageView iv = window.getContentView().findViewById(R.id.dic_pic);
            if (cr.getReaderView() != null) {
                if (cr.getReaderView().getSurface() != null) {
                    iv.setMinimumWidth(cr.getReaderView().getSurface().getWidth() / 5 * 2);
                    iv.setMinimumHeight(cr.getReaderView().getSurface().getWidth() / 5 * 2);
                }
            } else {
                iv.setMinimumWidth(t.anchor.getWidth() / 5 * 2);
                iv.setMinimumHeight(t.anchor.getWidth() / 5 * 2);
            }
            new DownloadImageTask(iv).execute(t.mPicAddr);
        }
        if (tv != null) tv.setTextColor(mActivity.getTextColor(colorIcon));
        if (tv2 != null) tv2.setTextColor(mActivity.getTextColor(colorIcon));
        //toast_ll.setBackgroundColor(colorGrayC);
        int tSize = mActivity.settings().getInt(ReaderView.PROP_FONT_SIZE, 20);
        if (tSize>0) {
            if (tv != null) tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tSize);
            if (tv2 != null) tv2.setTextSize(/*TypedValue.COMPLEX_UNIT_PX,*/ tSize);
        }
        // SvyatKV - https://github.com/plotn/coolreader/issues/411
//        if (t.dicType == IS_WIKI) {
//            if (tv != null)
//                tv.setOnClickListener(v -> mHandler.postDelayed(handleDismiss, 100));
//            if (tv2 != null)
//                tv2.setOnClickListener(v -> mHandler.postDelayed(handleDismiss, 100));
//        }
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
                    if (tv != null) tv.setTypeface(tf);
                    if (tv2 != null) tv2.setTypeface(tf);
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
                        if (tv != null) tv.setTypeface(tf);
                        if (tv2 != null) tv2.setTypeface(tf);
                    } catch (Exception e) {

                    }
                    break;
                }
            }
        toast_ll.setBackgroundColor(Color.argb(255, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC)));
        TableLayout dicTable = (TableLayout) window.getContentView().findViewById(R.id.dic_table);
        if (t.dicType == IS_YANDEX) {
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
            tvYnd1.setOnClickListener(v -> {
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
            });
            if (!StrUtils.isEmptyStr(t.mDicName)) {
                tvYnd1.setText(t.mDicName + ": " + tvYnd1.getText());
            }
            ImageButton btnTransl = window.getContentView().findViewById(R.id.btnTransl);
            btnTransl.setOnClickListener(v -> {
                if (cr.getReaderView().mBookInfo!=null) {
                    String lang = StrUtils.getNonEmptyStr(cr.getReaderView().mBookInfo.getFileInfo().lang_to,true);
                    String langf = StrUtils.getNonEmptyStr(cr.getReaderView().mBookInfo.getFileInfo().lang_from, true);
                    FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
                    FileInfo dfi = fi.parent;
                    if (dfi == null) {
                        dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
                    }
                    if (dfi != null) {
                        cr.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL, null, dfi, fi, langf, lang, "", null,
                                TranslationDirectionDialog.FOR_COMMON, null);
                    }
                };
                dismissAndCheckDicList();
                curWindow = null;
                showing.compareAndSet(true, false);
            });
            ImageButton btnToUserDic = (ImageButton) window.getContentView().findViewById(R.id.btn_to_user_dic);
            btnToUserDic.setOnClickListener(v -> {
                if (cr.getReaderView().mBookInfo!=null) {
                    if (cr.getReaderView().lastSelection!=null) {
                        if (!cr.getReaderView().lastSelection.isEmpty()) {
                            cr.getReaderView().clearSelection();
                            cr.getReaderView().showNewBookmarkDialog(cr.getReaderView().lastSelection, Bookmark.TYPE_USER_DIC, t.msg);
                        }
                    }
                };
                dismissAndCheckDicList();
                curWindow = null;
                showing.compareAndSet(true, false);
            });
            ImageButton btnCopyToCb = window.getContentView().findViewById(R.id.btn_copy_to_cb);
            btnCopyToCb.setOnClickListener(v -> {
                ClipboardManager cm = mActivity.getClipboardmanager();
                String s = StrUtils.getNonEmptyStr(t.msg,true);
                if (cr.getReaderView().lastSelection != null) {
                    if (s.startsWith(cr.getReaderView().lastSelection.text+":")) s = s.substring(cr.getReaderView().lastSelection.text.length()+1);
                }
                cm.setText(StrUtils.getNonEmptyStr(s,true));
                dismissAndCheckDicList();
                curWindow = null;
                showing.compareAndSet(true, false);
            });
            mActivity.tintViewIcons(yndRow,true);
        } else
            if (!StrUtils.getNonEmptyStr(t.mDicName,true).equals("[HIDE]"))
            {
                TableRow getSepRow = (TableRow) mInflater.inflate(R.layout.geo_sep_item, null);
                dicTable.addView(getSepRow);
                View sep = window.getContentView().findViewById(R.id.sepRow);
                sep.setBackgroundColor(updColor(colorIcon));
                TableRow yndRow = (TableRow) mInflater.inflate(R.layout.dic_ynd_item, null);
                dicTable.addView(yndRow);
                TextView tvYnd1 = (TextView) window.getContentView().findViewById(R.id.ynd_tv1);
                if ((t.dicType == IS_DICTCC) || (t.dicType == IS_LINGUEE) ||
                        (t.dicType == IS_GRAMOTA) || (t.dicType == IS_GLOSBE) ||
                        (t.dicType == IS_TURENG)) {
                    String s = StrUtils.getNonEmptyStr(t.mDicName,true);
                    if (s.contains("?")) s = s.substring(0, s.indexOf("?"));
                    tvYnd1.setText(s);
                } else
                    tvYnd1.setText(t.mDicName);
                Integer tSizeI = tSize;
                Double tSizeD = Double.valueOf(tSizeI.doubleValue() *0.8);
                if (tSize > 0) {
                    tvYnd1.setTextSize(TypedValue.COMPLEX_UNIT_PX, tSizeD.intValue());
                }
                tvYnd1.setPaintFlags(tvYnd1.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                tvYnd1.setOnClickListener(v -> {
                    mHandler.postDelayed(handleDismiss, 100);
                    String sLink = "";
                    if (t.dicType == IS_LINGVO) sLink = "https://developers.lingvolive.com/";
                    if (t.dicType == IS_DEEPL) sLink = "https://www.deepl.com/";
                    if (t.dicType == IS_WIKI) sLink = t.mDicName;
                    if (t.dicType == IS_DICTCC) sLink = t.mDicName;
                    if (t.dicType == IS_LINGUEE) sLink = t.mDicName;
                    if (t.dicType == IS_GRAMOTA) sLink = t.mDicName;
                    if (t.dicType == IS_GLOSBE) sLink = t.mDicName;
                    if (t.dicType == IS_GOOGLE) sLink = "https://translate.google.com/";
                    if (t.dicType == IS_TURENG) sLink = t.mDicName;
                    Uri uri = Uri.parse(sLink);
                    Context context = t.anchor.getContext();
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID,
                            context.getPackageName());
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {

                    }
                });
                ImageButton btnTransl = window.getContentView().findViewById(R.id.btnTransl);
                btnTransl.setOnClickListener(v -> {
                    if (cr.getReaderView().mBookInfo!=null) {
                        String lang = StrUtils.getNonEmptyStr(cr.getReaderView().mBookInfo.getFileInfo().lang_to,true);
                        String langf = StrUtils.getNonEmptyStr(cr.getReaderView().mBookInfo.getFileInfo().lang_from, true);
                        FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
                        FileInfo dfi = fi.parent;
                        if (dfi == null) {
                            dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
                        }
                        if (dfi != null) {
                            cr.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL, null, dfi, fi, langf, lang, "", null,
                                    TranslationDirectionDialog.FOR_COMMON, null);
                        }
                    };
                    dismissAndCheckDicList();
                    curWindow = null;
                    showing.compareAndSet(true, false);
                });
                if ((cr.getReaderView() == null) || (cr.mCurrentFrame != cr.mReaderFrame))
                    Utils.hideView(btnTransl);
                ImageButton btnToUserDic = window.getContentView().findViewById(R.id.btn_to_user_dic);
                btnToUserDic.setOnClickListener(v -> {
                    if (cr.getReaderView().mBookInfo!=null) {
                        if (cr.getReaderView().lastSelection!=null) {
                            if (!cr.getReaderView().lastSelection.isEmpty()) {
                                cr.getReaderView().clearSelection();
                                cr.getReaderView().showNewBookmarkDialog(cr.getReaderView().lastSelection, Bookmark.TYPE_USER_DIC,
                                        (t.dicType == IS_LINGVO ? "{{lingvo}}" : "") + t.msg);
                            }
                        }
                    };
                    dismissAndCheckDicList();
                    curWindow = null;
                    showing.compareAndSet(true, false);
                });
                if ((cr.getReaderView() == null) || (cr.mCurrentFrame != cr.mReaderFrame))
                    Utils.hideView(btnToUserDic);
                ImageButton btnCopyToCb = window.getContentView().findViewById(R.id.btn_copy_to_cb);
                btnCopyToCb.setOnClickListener(v -> {
                    ClipboardManager cm = mActivity.getClipboardmanager();
                    String s = StrUtils.getNonEmptyStr(t.msg,true);
                    if (cr.getReaderView() != null)
                        if (cr.getReaderView().lastSelection != null) {
                            if (s.startsWith(cr.getReaderView().lastSelection.text+":")) s = s.substring(cr.getReaderView().lastSelection.text.length()+1);
                        }
                    cm.setText(StrUtils.getNonEmptyStr(s,true));
                    dismissAndCheckDicList();
                    curWindow = null;
                    showing.compareAndSet(true, false);
                });
                mActivity.tintViewIcons(yndRow,true);
            }
        int [] location = new int[2];
        t.anchor.getLocationOnScreen(location);
        int popupY = location[1] + t.anchor.getHeight() - toast_ll.getHeight();
        mActivity.tintViewIcons(window,false);
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
        TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2});
        colorGray = a.getColor(0, Color.GRAY);
        a.recycle();
        int colr2 = colorGray;
        ll1.setBackgroundColor(Color.argb(255, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC)));
        Button tvMore = window.getContentView().findViewById(R.id.upper_row_tv_more);
        tvMore.setBackgroundColor(colr2);
        //tvMore.setBackgroundColor(colorGray);
        Button tvClose = window.getContentView().findViewById(R.id.upper_row_tv_close);
        tvClose.setBackgroundColor(colr2);
        TextView tvLblDic = window.getContentView().findViewById(R.id.lbl_dic);
        if (tvLblDic != null) {
            if (t.mCurDict != null)
                tvLblDic.setText(t.mCurDict.shortName);
            else
                tvLblDic.setText(t.mDicName);
        }
        //tvMore.setPaintFlags(tvMore.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvMore.setOnClickListener(v -> {
            Dictionaries dicts = new Dictionaries(mActivity);
            String ss = sFindText;
            if (ss.contains("~")) ss = ss.split("~")[1];
            if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
            ((CoolReader) mActivity).lastDicSkip = true;
            Dictionaries.wikiSearch.wikiTranslate((CoolReader) mActivity, t.mCurDict, mReaderView, ss, t.mLink, t.mLink2,
                    t.mCurAction, mListSkipCount + t.wikiArticles.wikiArticleList.size(), t.mUseFirstLink, 0 ,"", null);
            mHandler.postDelayed(handleDismiss, 100);
        });
        int sz = 0;
        if (t.wikiArticles != null) sz = t.wikiArticles.wikiArticleList.size();
        if (sz == 0) ((ViewGroup) tvMore.getParent()).removeView(tvMore);
        tvClose.setOnClickListener(v -> mHandler.postDelayed(handleDismiss, 100));
        ViewGroup body = window.getContentView().findViewById(R.id.articles_list);
        CoolReader cr=(CoolReader) mActivity;
        if (cr.getReaderView() != null) {
            if (cr.getReaderView().getSurface() != null) {
                ((MaxHeightLinearLayout) body).setMaxHeight(cr.getReaderView().getSurface().getHeight() * 3 / 4);
            }
        } else {
            ((MaxHeightLinearLayout) body).setMaxHeight(t.anchor.getHeight() * 3 / 4);
        }
        t.mWikiArticlesList = new WikiArticlesList(mActivity, t.wikiArticles.wikiArticleList);
        body.addView(t.mWikiArticlesList);
        int [] location = new int[2];
        t.anchor.getLocationOnScreen(location);
        int popupY = location[1] + t.anchor.getHeight() - ll1.getHeight();
        mActivity.tintViewIcons(window,false);
        window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
    }

    static class ExtDicAdapter extends BaseAdapter {
        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEnabled(int arg0) {
            return true;
        }

        public int getCount() {
            if (curToast == null)
                return 0;
            else if (curToast.dicStruct == null)
                return 0;
            else
                return curToast.dicStruct.getCount();
        }

        public Object getItem(int position) {
            if (curToast == null)
                return 0;
            if (curToast.dicStruct == null)
                return 0;
            if (position < 0 || position >= curToast.dicStruct.getCount())
                return null;
            return curToast.dicStruct.getByNum(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public final static int ITEM_POSITION=0;

        public int getItemViewType(int position) {
            return ITEM_POSITION;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            //if (mInflater == null) return null;
            Object o = getItem(position);
            if (o == null) {
                int res = R.layout.ext_dic_entry;
                view = mInflater.inflate(res, null);
                return view;
            }
            if (o instanceof DictEntry) {
                DictEntry de = (DictEntry) o;
                int res = R.layout.ext_dic_entry;
                view = mInflater.inflate(res, null);
                TextView labelView = view.findViewById(R.id.ext_dic_entry);
                String text = StrUtils.getNonEmptyStr(de.dictLinkText, true);
                if (!StrUtils.isEmptyStr(de.tagType))
                    text = text + "; " + de.tagType.trim();
                if (!StrUtils.isEmptyStr(de.tagWordType))
                    text = text + "; " + de.tagWordType.trim();
                labelView.setText(text);
                if (!StrUtils.isEmptyStr(sFindText))
                    Utils.setHighLightedText(labelView, sFindText, mColorIconL);
                return view;
            }

            if (o instanceof String) {
                String s = (String) o;
                if (s.startsWith("~")) {
                    int res = R.layout.ext_dic_transl_group;
                    view = mInflater.inflate(res, null);
                    TextView labelView = view.findViewById(R.id.ext_dic_transl_group);
                    String text = StrUtils.getNonEmptyStr(s.substring(1), true);
                    labelView.setText(text);
                    if (!StrUtils.isEmptyStr(sFindText))
                        Utils.setHighLightedText(labelView, sFindText, mColorIconL);
                    return view;
                }
            }

            if (o instanceof TranslLine) {
                TranslLine tl = (TranslLine) o;
                int res = R.layout.ext_dic_transl_line;
                view = mInflater.inflate(res, null);
                TextView labelView = view.findViewById(R.id.ext_dic_transl_line);
                String text = StrUtils.getNonEmptyStr(tl.transText, true);
                if (!StrUtils.isEmptyStr(tl.transType))
                    text = text + "; " + tl.transType.trim();
                labelView.setText(text);
                if (!StrUtils.isEmptyStr(sFindText))
                    Utils.setHighLightedText(labelView, sFindText, mColorIconL);
                return view;
            }

            if (o instanceof ExampleLine) {
                ExampleLine el = (ExampleLine) o;
                int res = R.layout.ext_dic_example_line;
                view = mInflater.inflate(res, null);
                TextView labelView = view.findViewById(R.id.ext_dic_example_line);
                String text = StrUtils.getNonEmptyStr(el.line, true);
                labelView.setText(text);
                if (!StrUtils.isEmptyStr(sFindText))
                    Utils.setHighLightedText(labelView, sFindText, mColorIconL);
                return view;
            }
            if (o instanceof LinePair) {
                LinePair lp = (LinePair) o;
                int res = R.layout.ext_dic_res_pair;
                view = mInflater.inflate(res, null);
                TextView labelView = view.findViewById(R.id.ext_dic_res_pair1);
                String text = StrUtils.getNonEmptyStr(lp.leftPart, true);
                labelView.setText(text);
                if (!StrUtils.isEmptyStr(sFindText))
                    Utils.setHighLightedText(labelView, sFindText, mColorIconL);
                TextView labelView2 = view.findViewById(R.id.ext_dic_res_pair2);
                String text2 = StrUtils.getNonEmptyStr(lp.rightPart, true);
                labelView2.setText(text2);
                if (!StrUtils.isEmptyStr(sFindText))
                    Utils.setHighLightedText(labelView2, sFindText, mColorIconL);
                return view;
            }

            int res = R.layout.ext_dic_entry;
            view = mInflater.inflate(res, null);
            return view;
        }

        public boolean hasStableIds() {
            return true;
        }

        public boolean isEmpty() {
            if (curToast == null)
                return true;
            else if (curToast.wikiArticles == null)
                return true;
            else return curToast.wikiArticles.wikiArticleList.size() == 0;
        }

        private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

        public void registerDataSetObserver(DataSetObserver observer) {
            observers.add(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            observers.remove(observer);
        }
    }

    static class ExtDicList extends BaseListView {

        private DicStruct dicStruct;
        private String findText;

        public ExtDicList(Context context, DicStruct dsl, String sFindText) {
            super(context, true);
            this.dicStruct = dsl;
            this.findText = sFindText;
            setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            setLongClickable(true);
            setAdapter(new ExtDicAdapter());
            setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
                //openContextMenu(DictList.this);
                return true;
            });
        }

        @Override
        public boolean performItemClick(View view, int position, long id) {
            Dictionaries.saveToDicSearchHistory((CoolReader) mActivity, findText, dicStruct.getTranslation(position), mListCurDict);
            if ((((CoolReader) mActivity).getReaderView() == null) ||
                    (((CoolReader) mActivity).mCurrentFrame != ((CoolReader) mActivity).mReaderFrame)) {
                ClipboardManager cm = mActivity.getClipboardmanager();
                String s = StrUtils.getNonEmptyStr(findText + ": " + dicStruct.getTranslation(position), true);
                cm.setText(StrUtils.getNonEmptyStr(s, true));
            }
            mHandler.postDelayed(handleDismiss, 100);
            return true;
        }
    }

    private static void extListToast(Toast t) {
        window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
        window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        //window.setHeight(10000);
        window.setTouchable(true);
        window.setFocusable(false);
        window.setOutsideTouchable(true);
        window.setBackgroundDrawable(null);
        mInflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        window.setContentView(mInflater.inflate(R.layout.ext_dic_dlg, null, true));
        LinearLayout ll1 = window.getContentView().findViewById(R.id.items_list_ll1);
        TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2});
        colorGray = a.getColor(0, Color.GRAY);
        a.recycle();
        int colr2 = colorGray;
        ll1.setBackgroundColor(Color.argb(255, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC)));
        Button tvClose = window.getContentView().findViewById(R.id.upper_row_tv_close);
        tvClose.setBackgroundColor(colr2);
        tvClose.setOnClickListener(v -> mHandler.postDelayed(handleDismiss, 100));
        TextView tvLblDic = window.getContentView().findViewById(R.id.lbl_dic);
        if (tvLblDic != null) {
            if (t.mCurDict != null)
                tvLblDic.setText(t.mCurDict.shortName);
            else
                tvLblDic.setText(t.mDicName);
        }
        ViewGroup body = window.getContentView().findViewById(R.id.items_list);
        CoolReader cr= mActivity;
        if (cr.getReaderView() != null) {
            if (cr.getReaderView().getSurface() != null) {
                ((MaxHeightLinearLayout) body).setMaxHeight(cr.getReaderView().getSurface().getHeight() * 3 / 4);
            }
        } else {
            ((MaxHeightLinearLayout) body).setMaxHeight(t.anchor.getHeight() * 3 / 4);
        }
        t.mExtDicList = new ExtDicList(mActivity, t.dicStruct, t.sFindText);
        body.addView(t.mExtDicList);
        int [] location = new int[2];
        t.anchor.getLocationOnScreen(location);
        int popupY = location[1] + t.anchor.getHeight() - ll1.getHeight();
        mActivity.tintViewIcons(window,false);
        window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
    }
}
