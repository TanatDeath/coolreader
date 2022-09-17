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

import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DownloadImageTask;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.MaxHeightLinearLayout;
import org.coolreader.crengine.MaxHeightScrollView;
import org.coolreader.crengine.Settings;
import org.coolreader.layouts.FlowLayout;
import org.coolreader.readerview.ReaderView;
import org.coolreader.crengine.Services;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.dic.wiki.WikiArticle;
import org.coolreader.dic.wiki.WikiArticles;
import org.coolreader.dic.wiki.WikiSearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    public static int IS_OFFLINE = 11;
    public static int IS_USERDIC = 12;
    public static int IS_ONYXAPI = 13;
    public static int IS_REVERSO = 14;

    public static int mColorIconL = Color.GRAY;

    public static boolean isEInk;
    public static HashMap<Integer, Integer> themeColors;

    public static Dictionaries.DictInfo mListCurDict;
    private static String mListLink;
    private static String mListLink2;
    private static boolean mListUseFirstLink;

    private static void setBtnBackgroundColor(Button btn, int col) {
        if (!isEInk)
            btn.setBackgroundColor(col);
        else
            Utils.setSolidButtonEink(btn);
    }

    public static class Toast {
        public View anchor;
        public String sFindText;
        private String msg;
        private int duration;
        public WikiArticles wikiArticles = null;
        public DicStruct dicStruct = null;
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
    private static Toast curToast = null;
    public static Handler mHandler = new Handler();
    public static Object toastWindow = null;
    private static int colorGray;
    private static int colorGrayC;
    private static int colorIcon;
    public static CoolReader mActivity;
    private static LayoutInflater mInflater;
    private static String sFindText;
    private static int mListSkipCount = 0;

    public static void doDismiss() {
        if (toastWindow != null) {
            if (toastWindow instanceof PopupWindow)
                ((PopupWindow) toastWindow).dismiss();
            if (toastWindow instanceof DicArticleDlg)
                ((DicArticleDlg) toastWindow).dismiss();
        }
        toastWindow = null;
        curToast = null;
    }

    public static Runnable handleDismiss = () -> {
        doDismiss();
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
        private boolean fullScreen = false;

        public WikiArticlesList(Context context, List<WikiArticle> arrWA, boolean fullScreen) {
            super(context, true);
            this.arrWA = arrWA;
            this.fullScreen = fullScreen;
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
            Dictionaries.wikiSearch.wikiTranslate(mActivity,
                    fullScreen, mListCurDict, null,
                    wa.pageId +"~"+sFindText, mListLink, mListLink2,
                    Dictionaries.wikiSearch.WIKI_SHOW_PAGE_ID, mListUseFirstLink, null);
            doDismiss();
            return true;
        }
    }

    public static void hideToast(BaseActivity act) {
        try {
            doDismiss();
        } catch (Exception e) {

        }
    }
    
    private static void showToastInternal(BaseActivity act, View anchor, String msg, int duration,
                                 int dicT, String dicName, Object dicStructObject,
                                 Dictionaries.DictInfo curDict, String link, String link2, int curAction,
                                 boolean useFirstLink, String picAddr, boolean fullScreen) {
        doDismiss();
        TypedArray a = act.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon, R.attr.colorIconL});
        colorGray = a.getColor(0, Color.GRAY);
        colorGrayC = a.getColor(1, Color.GRAY);
        colorIcon = a.getColor(2, Color.GRAY);
        mColorIconL = a.getColor(0, Color.GRAY);

        a.recycle();

        mReaderView = anchor;
        mActivity = (CoolReader) act;
        isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
        themeColors = Utils.getThemeColors(mActivity, isEInk);
        mListCurDict = curDict;
        mListLink = link;
        mListLink2 = link2;
        mListUseFirstLink = useFirstLink;
        String msg1 = msg;
        if (StrUtils.isEmptyStr(msg1))
            msg1 = mActivity.getString(R.string.not_found);
        curToast = new Toast(anchor, sFindText, msg1,
            duration, dicStructObject, dicT, dicName, curDict, link, link2, curAction, useFirstLink, picAddr);
        show(fullScreen);
    }

    public static void showToast(BaseActivity act, View anchor, String s, String msg, int duration,
                                 int dicT, String dicName, Dictionaries.DictInfo curDict, boolean fullScreen) {
        sFindText = s;
        showToastInternal(act, anchor, msg, duration, dicT, dicName, null,
                curDict, null, null, 0, false, "", fullScreen);
    }

    public static void showToastExt(BaseActivity act, View anchor, String s, String msg, int duration,
                                 int dicT, String dicName, Dictionaries.DictInfo curDict,
                                    Object dicStructObject, boolean fullScreen) {
        sFindText = s;
        showToastInternal(act, anchor, msg, duration, dicT, dicName, dicStructObject,
                curDict, null, null, 0, false, "", fullScreen);
    }

    public static void showToastWiki(BaseActivity act, View anchor, String s, String msg, int duration,
                                 int dicT, String dicName,
                                 Dictionaries.DictInfo curDict, String link, String link2, int curAction,
                                 boolean useFirstLink,
                                 String picAddr, boolean fullScreen) {
        sFindText = s;
        showToastInternal(act, anchor, msg, duration, dicT, dicName, null,
                curDict, link, link2, curAction, useFirstLink, picAddr, fullScreen);
    }

    public static void showWikiListToast(BaseActivity act, View anchor, String s, String msg,
                                         int dicT, String dicName, WikiArticles arrWA,
                                         Dictionaries.DictInfo curDict, String link, String link2, int curAction,
                                         int listSkipCount, boolean useFirstLink, boolean fullScreen) {
        sFindText = s;
        mListSkipCount = listSkipCount;
        showToastInternal(act, anchor, msg, android.widget.Toast.LENGTH_LONG, dicT, dicName,
                arrWA, curDict, link, link2, curAction, useFirstLink, "",
                fullScreen);
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

    private static void show(boolean fullScreen) {
        Toast t = curToast;
        if (t == null) return;
        doDismiss();
        if (!fullScreen) {
            PopupWindow window = new PopupWindow(t.anchor.getContext());
            toastWindow = window;
            window.setTouchInterceptor((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    if (mActivity.getmReaderView() != null)
                        mActivity.getmReaderView().disableTouch = true;
                    doDismiss();
                    return true;
                }
                return false;
            });
        }
        curToast = t;
        if (t.wikiArticles != null) wikiListToast(t, fullScreen);
            else if (t.dicStruct != null) extListToast(t, fullScreen);
                else simpleToast(t, fullScreen);
    }

    private static String updTerm(String s) {
        if (s == null) return "";
        if (s.contains("~")) {
            String[] arrs = s.split("~");
            return arrs[arrs.length-1];
        }
        return s;
    }

    private static void initRecentDics(ViewGroup vg, String findText, boolean fullScreen) {
        int newTextSize = mActivity.settings().getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
        int iCntRecent = 0;
        vg.removeAllViews();
        for (final Dictionaries.DictInfo di : mActivity.mDictionaries.diRecent) {
            if (!di.equals(mActivity.mDictionaries.getCurDictionary())) {
                iCntRecent++;
            }
        }
        if (iCntRecent == 0) iCntRecent++;
        List<Dictionaries.DictInfo> diAllDicts = new ArrayList<>();
        for (final Dictionaries.DictInfo di: mActivity.mDictionaries.diRecent) {
            diAllDicts.add(di);
        }
        for (final Dictionaries.DictInfo di: mActivity.mDictionaries.getAddDicts()) {
            diAllDicts.add(di);
        }
        List<Dictionaries.DictInfo> dicts = Dictionaries.getDictList(mActivity);
        for (Dictionaries.DictInfo dict : dicts) {
            boolean bUseDic = mActivity.settings().getBool(Settings.PROP_DIC_LIST_MULTI+"."+dict.id,false);
            if (bUseDic) {
                boolean bWas = false;
                for (Dictionaries.DictInfo di: diAllDicts) {
                    if (di.id.equals(dict.id)) {
                        bWas = true;
                        break;
                    }
                }
                if (!bWas) diAllDicts.add(dict);
            }
        }
        ArrayList<String> added = new ArrayList<>();
        for (final Dictionaries.DictInfo di: diAllDicts) {
            if (!added.contains(di.id)) {
                added.add(di.id);
                Button dicButton = new Button(mActivity);
                String sAdd = di.getAddText(mActivity);
                String sName = di.shortName;
                if (StrUtils.isEmptyStr(sName)) sName = di.name;
                if (StrUtils.isEmptyStr(sAdd))
                    dicButton.setText(sName);
                else
                    dicButton.setText(sName + ": " + sAdd);
                dicButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
                dicButton.setTextColor(mActivity.getTextColor(colorIcon));
                dicButton.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
                Utils.setSolidButton1(dicButton);
                if (isEInk) Utils.setSolidButtonEink(dicButton);
                dicButton.setPadding(10, 20, 10, 20);
                //dicButton.setBackground(null);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                llp.setMargins(8, 4, 4, 8);
                dicButton.setLayoutParams(llp);
                //dicButton.setMaxWidth((mReaderView.getRequestedWidth() - 20) / iCntRecent); // This is not needed anymore - since we use FlowLayout
                dicButton.setMaxLines(1);
                dicButton.setEllipsize(TextUtils.TruncateAt.END);
                TextView tv = new TextView(mActivity);
                tv.setText(" ");
                tv.setPadding(5, 10, 5, 10);
                tv.setLayoutParams(llp);
                tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
                tv.setTextColor(mActivity.getTextColor(colorIcon));
                vg.addView(dicButton);
                vg.addView(tv);
                dicButton.setOnClickListener(v -> {
                    mActivity.mDictionaries.setAdHocDict(di);
                    mActivity.mDictionaries.setAdHocFromTo(
                            mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                    doDismiss();
                    try {
                        mActivity.mDictionaries.findInDictionary(findText, fullScreen,
                                mActivity.mDictionaries.lastDicView, false, mActivity.mDictionaries.lastDC);
                    } catch (Dictionaries.DictionaryException e) {
                        // do nothing
                    }
                });
                dicButton.setOnLongClickListener(v -> {
//                    mActivity.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_ONLY_CHOOSE_QUICK, false,
//                            mReaderView.getSurface(), mReaderView.getBookInfo().getFileInfo().parent,
//                            mReaderView.getBookInfo().getFileInfo(),
//                            StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_from, true),
//                            StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_to, true),
//                            "", null, TranslationDirectionDialog.FOR_COMMON, s -> {
//                                mActivity.mDictionaries.setAdHocDict(di);
//                                String sSText = selection.text;
//                                mActivity.findInDictionary(sSText, false, null);
//                                if (!mReaderView.getSettings().getBool(mReaderView.PROP_APP_SELECTION_PERSIST, false))
//                                    mReaderView.clearSelection();
//                                closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
//                            });
                    return true;
                });
                if (isEInk) Utils.setBtnBackground(dicButton, null, isEInk);
            }
        }
    }

    private static void simpleToast(Toast t, boolean fullScreen) {
        mInflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dicContent;
        if (StrUtils.isEmptyStr(t.mPicAddr)) {
            if (fullScreen)
                dicContent = mInflater.inflate(R.layout.dic_toast, null, true);
            else
                dicContent = mInflater.inflate(R.layout.dic_toast_pop, null, true);
        }
        else {
            if (fullScreen)
                dicContent = mInflater.inflate(R.layout.dic_toast_with_pic, null, true);
            else
                dicContent = mInflater.inflate(R.layout.dic_toast_with_pic_pop, null, true);
        }
        DicArticleDlg dad = null;
        TableRow tr1;
        TableRow tr2;
        Button tvMore;
        Button tvClose;
        TextView tvTerm;
        Button btnRemove3sym;
        Button btnRemove2sym;
        Button btnRemove1sym;
        Button btnDicExtended;
        Button tvFull;
        Button tvFullWeb;
        TextView tvLblDic;
        ViewGroup toast_ll;
        TextView tv = null;
        FlowTextView tv2 = null;
        ImageView iv = null;
        TableLayout dicTable;
        View sep;
        TextView tvYnd1;
        ImageButton btnToUserDic;
        ImageButton btnCopyToCb;
        ImageButton btnTransl;
        LinearLayout llUpperRowRecent;
        MaxHeightScrollView sv = null;
        if (fullScreen) {
            dad = new DicArticleDlg("DicArticleDlg",
                    DicToastView.mActivity, curToast, -1, dicContent);
            toastWindow = dad;
            //ll1 = dad.mBody.findViewById(R.id.items_list_ll1);
            tr1 = dad.mBody.findViewById(R.id.tr_upper_row);
            tr2 = dad.mBody.findViewById(R.id.tr_upper_sep_row);
            tvMore = dad.mBody.findViewById(R.id.upper_row_tv_more);
            tvClose = dad.mBody.findViewById(R.id.upper_row_tv_close);
            tvTerm = dad.mBody.findViewById(R.id.lbl_term);
            btnRemove3sym = dad.mBody.findViewById(R.id.remove3sym);
            btnRemove2sym = dad.mBody.findViewById(R.id.remove2sym);
            btnRemove1sym = dad.mBody.findViewById(R.id.remove1sym);
            btnDicExtended = dad.mBody.findViewById(R.id.btnDicExtended);
            tvFull = dad.mBody.findViewById(R.id.upper_row_tv_full);
            tvFullWeb = dad.mBody.findViewById(R.id.upper_row_tv_full_web);
            tvLblDic = dad.mBody.findViewById(R.id.lbl_dic);
            toast_ll = dad.mBody.findViewById(R.id.dic_toast_ll);
            if (StrUtils.isEmptyStr(t.mPicAddr))
                tv = dad.mBody.findViewById(R.id.dic_text);
            else {
                tv2 = dad.mBody.findViewById(R.id.dic_text_flow);
                iv = dad.mBody.findViewById(R.id.dic_pic);
            }
            dicTable = dad.mBody.findViewById(R.id.dic_table);
            llUpperRowRecent = dad.mBody.findViewById(R.id.upper_row_ll_recent);
        } else {
            PopupWindow window = (PopupWindow) toastWindow;
            window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
            window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            window.setTouchable(true);
            window.setFocusable(false);
            window.setOutsideTouchable(true);
            window.setBackgroundDrawable(null);
            window.setContentView(dicContent);
            //ll1 = window.getContentView().findViewById(R.id.items_list_ll1);
            tr1 = window.getContentView().findViewById(R.id.tr_upper_row);
            tr2 = window.getContentView().findViewById(R.id.tr_upper_sep_row);
            tvMore = window.getContentView().findViewById(R.id.upper_row_tv_more);
            tvClose = window.getContentView().findViewById(R.id.upper_row_tv_close);
            tvTerm = window.getContentView().findViewById(R.id.lbl_term);
            btnRemove3sym = window.getContentView().findViewById(R.id.remove3sym);
            btnRemove2sym = window.getContentView().findViewById(R.id.remove2sym);
            btnRemove1sym = window.getContentView().findViewById(R.id.remove1sym);
            btnDicExtended = window.getContentView().findViewById(R.id.btnDicExtended);
            tvFull = window.getContentView().findViewById(R.id.upper_row_tv_full);
            tvFullWeb = window.getContentView().findViewById(R.id.upper_row_tv_full_web);
            tvLblDic = window.getContentView().findViewById(R.id.lbl_dic);
            toast_ll = window.getContentView().findViewById(R.id.dic_toast_ll);
            if (StrUtils.isEmptyStr(t.mPicAddr))
                tv = window.getContentView().findViewById(R.id.dic_text);
            else {
                tv2 = window.getContentView().findViewById(R.id.dic_text_flow);
                iv = window.getContentView().findViewById(R.id.dic_pic);
            }
            dicTable = window.getContentView().findViewById(R.id.dic_table);
            sv =  window.getContentView().findViewById(R.id.dic_scrollV);
            llUpperRowRecent = window.getContentView().findViewById(R.id.upper_row_ll_recent);
        }
        CoolReader cr = mActivity;
        if (!fullScreen) {
            if (cr.getReaderView() != null) {
                if (cr.getReaderView().getSurface() != null) {
                    sv.setMaxHeight(cr.getReaderView().getSurface().getHeight() * 8 / 12);
                }
            } else {
                sv.setMaxHeight(t.anchor.getHeight() * 8 / 12);
            }
        }
        int colorGray;
        TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
                {R.attr.colorThemeGray2});
        colorGray = a.getColor(0, Color.GRAY);
        a.recycle();
        int colr2 = colorGray;
        setBtnBackgroundColor(tvMore, colr2);
        setBtnBackgroundColor(tvClose, colr2);
        if (tvTerm != null) {
            final String findText = StrUtils.getNonEmptyStr(t.sFindText, true);
            tvTerm.setText(updTerm(t.sFindText));
            tvTerm.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            setBtnBackgroundColor(tvClose, colr2);
            if (llUpperRowRecent != null) {
                ViewGroup recentDicsPanel = (ViewGroup) mInflater.inflate(R.layout.recent_dic_panel_scroll, null, true);
                LinearLayout ll = recentDicsPanel.findViewById(R.id.ll_dic_buttons);
                initRecentDics(ll, findText, fullScreen);
                llUpperRowRecent.addView(recentDicsPanel);
            }
            btnRemove1sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove1sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove1sym);
            btnRemove2sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove2sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove2sym);
            btnRemove3sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove3sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove3sym);
            Utils.setSolidButton1(btnDicExtended);
            if (isEInk) Utils.setSolidButtonEink(btnDicExtended);
            btnDicExtended.setOnClickListener(v -> {
                try {
                    mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                    mActivity.mDictionaries.setAdHocFromTo(
                            mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                    doDismiss();
                    mActivity.mDictionaries.findInDictionary(findText, fullScreen,
                            mActivity.mDictionaries.lastDicView, true, mActivity.mDictionaries.lastDC);
                } catch (Dictionaries.DictionaryException e) {
                    // do nothing
                }
            });
            if ((t.dicType != IS_OFFLINE) || (t.mCurDict == null)) Utils.hideView(btnDicExtended);
            if ((findText.length() <= 1)  || (t.mCurDict == null)) Utils.hideView(btnRemove1sym);
            else
                btnRemove1sym.setOnClickListener(v -> {
                    if (findText.length()>1) {
                        String findText1 = findText.substring(0, findText.length() - 1);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                    mActivity.mDictionaries.lastDicView, false, mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
            if ((findText.length() <= 2)  || (t.mCurDict == null)) Utils.hideView(btnRemove2sym);
            else
                btnRemove2sym.setOnClickListener(v -> {
                    if (findText.length()>2) {
                        String findText1 = findText.substring(0, findText.length() - 2);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                    mActivity.mDictionaries.lastDicView, false, mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
            if ((findText.length() <= 3) || (t.mCurDict == null)) Utils.hideView(btnRemove3sym);
            else
                btnRemove3sym.setOnClickListener(v -> {
                    if (findText.length()>3) {
                        String findText1 = findText.substring(0, findText.length() - 3);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                    mActivity.mDictionaries.lastDicView, false, mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
        }
        setBtnBackgroundColor(tvFull, colr2);
        setBtnBackgroundColor(tvFullWeb, colr2);
        if (tvLblDic != null) {
            if (t.mCurDict != null)
                tvLblDic.setText(t.mCurDict.shortName);
            else {
                if (StrUtils.getNonEmptyStr(t.mDicName, true).equals("[HIDE]")) tvLblDic.setText("");
                else tvLblDic.setText(t.mDicName);
            }
            if (!fullScreen) {
                tvLblDic.setPaintFlags(tvLblDic.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                tvLblDic.setOnClickListener(v -> {
                    DictsDlg dlg = new DictsDlg(mActivity, mActivity.getReaderView(), t.sFindText, null, false);
                    doDismiss();
                    dlg.show();
                });
            }
        }
        tvMore.setOnClickListener(v -> {
            String ss = sFindText;
            if (ss.contains("~")) ss = ss.split("~")[1];
            if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
            Dictionaries.wikiSearch.wikiTranslate(mActivity,
                    fullScreen,
                    t.mCurDict, mReaderView, ss, t.mLink, t.mLink2,
                    Dictionaries.wikiSearch.WIKI_FIND_LIST, t.mUseFirstLink, null);
            doDismiss();
        });
        if (t.dicType != IS_WIKI) Utils.hideView(tvMore);
            else Utils.hideView(llUpperRowRecent);
        tvClose.setOnClickListener(v -> doDismiss());
        tvFull.setOnClickListener(v -> {
            if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
            if (t.mCurAction == Dictionaries.wikiSearch.WIKI_FIND_TITLE)
                Dictionaries.wikiSearch.wikiTranslate(mActivity, fullScreen,
                    t.mCurDict, mReaderView, sFindText, t.mLink, t.mLink2,
                    Dictionaries.wikiSearch.WIKI_FIND_TITLE_FULL, t.mUseFirstLink, null);
            else
                Dictionaries.wikiSearch.wikiTranslate(mActivity, fullScreen,
                    t.mCurDict, mReaderView, sFindText, t.mLink, t.mLink2,
                    Dictionaries.wikiSearch.WIKI_SHOW_PAGE_FULL_ID, t.mUseFirstLink, null);
            doDismiss();
        });
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
            doDismiss();
        });
        if (t.dicType != IS_WIKI) ((ViewGroup)tvFullWeb.getParent()).removeView(tvFullWeb);
        if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
        if ((t.mCurAction == Dictionaries.wikiSearch.WIKI_SHOW_PAGE_FULL_ID)||
                (t.mCurAction == Dictionaries.wikiSearch.WIKI_FIND_TITLE_FULL)||
                (t.dicType != IS_WIKI))
            ((ViewGroup) tvFull.getParent()).removeView(tvFull);
        if (tv != null) {
            tv.setText(t.msg);
            if (!StrUtils.isEmptyStr(sFindText)) Utils.setHighLightedText(tv, sFindText, mColorIconL);
        }
        if (tv2 != null) {
            tv2.setText(t.msg);
            if (cr.getReaderView() != null) {
                if (cr.getReaderView().getSurface() != null) {
                    iv.setMinimumWidth(cr.getReaderView().getSurface().getWidth() / 5 * 2);
                    iv.setMinimumHeight(cr.getReaderView().getSurface().getWidth() / 5 * 2);
                }
            } else {
                int w = t.anchor.getWidth() / 5 * 2;
                int h = t.anchor.getHeight() / 5 * 2;
                iv.setMinimumWidth(w);
                iv.setMinimumHeight(h);
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
        int colorFill = colorGrayC;
        if (isEInk) colorFill = Color.WHITE;
        if (fullScreen)
            toast_ll.setBackgroundColor(Color.argb(0, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));
        else
            toast_ll.setBackgroundColor(Color.argb(255, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));
        if (t.dicType == IS_YANDEX) {
            TableRow getSepRow = (TableRow) mInflater.inflate(R.layout.geo_sep_item, null);
            dicTable.addView(getSepRow);
            sep = dicTable.findViewById(R.id.sepRow);
            sep.setBackgroundColor(updColor(colorIcon));
            TableRow yndRow = (TableRow) mInflater.inflate(R.layout.dic_ynd_item, null);
            dicTable.addView(yndRow);
            tvYnd1 = dicTable.findViewById(R.id.ynd_tv1);
            btnToUserDic = dicTable.findViewById(R.id.btn_to_user_dic);
            btnCopyToCb = dicTable.findViewById(R.id.btn_copy_to_cb);
            btnTransl = dicTable.findViewById(R.id.btnTransl);
            Integer tSizeI = tSize;
            Double tSizeD = Double.valueOf(tSizeI.doubleValue() *0.8);
            if (tSize > 0) {
                tvYnd1.setTextSize(TypedValue.COMPLEX_UNIT_PX, tSizeD.intValue());
            }
            tvYnd1.setPaintFlags(tvYnd1.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            tvYnd1.setOnClickListener(v -> {
                doDismiss();
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
                        cr.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL, fullScreen,
                                null, dfi, fi, langf, lang, "", null,
                                TranslationDirectionDialog.FOR_COMMON, null);
                    }
                };
                doDismiss();
            });
            btnToUserDic.setOnClickListener(v -> {
                if (cr.getReaderView().mBookInfo!=null) {
                    if (cr.getReaderView().lastSelection!=null) {
                        if (!cr.getReaderView().lastSelection.isEmpty()) {
                            cr.getReaderView().clearSelection();
                            cr.getReaderView().showNewBookmarkDialog(cr.getReaderView().lastSelection,
                                    Bookmark.TYPE_USER_DIC, t.msg);
                        }
                    }
                };
                doDismiss();
            });
            btnCopyToCb.setOnClickListener(v -> {
                String s = StrUtils.getNonEmptyStr(t.msg,true);
                if (cr.getReaderView().lastSelection != null) {
                    if (s.startsWith(cr.getReaderView().lastSelection.text+":")) s = s.substring(cr.getReaderView().lastSelection.text.length()+1);
                }
                Utils.copyToClipboard(mActivity, StrUtils.getNonEmptyStr(s,true));
                doDismiss();
            });
            mActivity.tintViewIcons(yndRow,true);
        } else
            if (!StrUtils.getNonEmptyStr(t.mDicName,true).equals("[HIDE]"))
            {
                TableRow getSepRow = (TableRow) mInflater.inflate(R.layout.geo_sep_item, null);
                dicTable.addView(getSepRow);
                sep = dicTable.findViewById(R.id.sepRow);
                sep.setBackgroundColor(updColor(colorIcon));
                TableRow yndRow = (TableRow) mInflater.inflate(R.layout.dic_ynd_item, null);
                dicTable.addView(yndRow);
                tvYnd1 = dicTable.findViewById(R.id.ynd_tv1);
                btnToUserDic = dicTable.findViewById(R.id.btn_to_user_dic);
                btnCopyToCb = dicTable.findViewById(R.id.btn_copy_to_cb);
                btnTransl = dicTable.findViewById(R.id.btnTransl);
                if ((t.dicType == IS_DICTCC) || (t.dicType == IS_LINGUEE) ||
                        (t.dicType == IS_GRAMOTA) || (t.dicType == IS_GLOSBE) ||
                        (t.dicType == IS_TURENG) || (t.dicType == IS_URBAN) ||
                        (t.dicType == IS_ONYXAPI) || (t.dicType == IS_REVERSO)) {
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
                    doDismiss();
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
                    if (t.dicType == IS_URBAN) sLink = t.mDicName;
                    if (t.dicType == IS_ONYXAPI) sLink = t.mDicName;
                    if (t.dicType == IS_REVERSO) sLink = t.mDicName;
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
                            cr.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL, fullScreen,
                                    null, dfi, fi, langf, lang, "", null,
                                    TranslationDirectionDialog.FOR_COMMON, null);
                        }
                    };
                    doDismiss();
                });
                if ((cr.getReaderView() == null) || (cr.mCurrentFrame != cr.mReaderFrame))
                    Utils.hideView(btnTransl);
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
                    doDismiss();
                });
                if ((cr.getReaderView() == null) || (cr.mCurrentFrame != cr.mReaderFrame))
                    Utils.hideView(btnToUserDic);
                btnCopyToCb.setOnClickListener(v -> {
                    String s = StrUtils.getNonEmptyStr(t.msg,true);
                    if (cr.getReaderView() != null)
                        if (cr.getReaderView().lastSelection != null) {
                            if (s.startsWith(cr.getReaderView().lastSelection.text+":")) s = s.substring(cr.getReaderView().lastSelection.text.length()+1);
                        }
                    Utils.copyToClipboard(mActivity, StrUtils.getNonEmptyStr(s,true));
                    doDismiss();
                });
                mActivity.tintViewIcons(yndRow,true);
            }
        if (fullScreen) {
            mActivity.tintViewIcons(dad.mBody, false);
            dad.show();
        } else {
            PopupWindow window = (PopupWindow) toastWindow;
            int [] location = new int[2];
            t.anchor.getLocationOnScreen(location);
            int popupY = location[1] + t.anchor.getHeight() - toast_ll.getHeight();
            mActivity.tintViewIcons(window,false);
            window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
        }
    }

    private static void wikiListToast(Toast t, boolean fullScreen) {
        TextView tvTerm;
        Button btnRemove3sym;
        Button btnRemove2sym;
        Button btnRemove1sym;
        Button btnDicExtended;
        mInflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dicContent = mInflater.inflate(R.layout.wiki_articles_dlg, null, true);
        DicArticleDlg dad = null;
        LinearLayout ll1;
        colorGray = themeColors.get(R.attr.colorThemeGray2);
        Button tvMore;
        Button tvClose;
        TextView tvLblDic;
        ViewGroup body;
        ViewGroup body2;
        if (fullScreen) {
            dad = new DicArticleDlg("DicArticleDlg",
                    DicToastView.mActivity, curToast, -1, dicContent);
            toastWindow = dad;
            ll1 = dad.mBody.findViewById(R.id.articles_list_ll1);
            tvMore = dad.mBody.findViewById(R.id.upper_row_tv_more);
            tvClose = dad.mBody.findViewById(R.id.upper_row_tv_close);
            tvLblDic = dad.mBody.findViewById(R.id.lbl_dic);
            tvTerm = dad.mBody.findViewById(R.id.lbl_term);
            btnRemove3sym = dad.mBody.findViewById(R.id.remove3sym);
            btnRemove2sym = dad.mBody.findViewById(R.id.remove2sym);
            btnRemove1sym = dad.mBody.findViewById(R.id.remove1sym);
            btnDicExtended = dad.mBody.findViewById(R.id.btnDicExtended);
            body = dad.mBody.findViewById(R.id.articles_list);
            body2 = dad.mBody.findViewById(R.id.articles_list2);
        } else {
            PopupWindow window = (PopupWindow) toastWindow;
            window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
            window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            window.setTouchable(true);
            window.setFocusable(false);
            window.setOutsideTouchable(true);
            window.setBackgroundDrawable(null);
            window.setContentView(dicContent);
            ll1 = window.getContentView().findViewById(R.id.articles_list_ll1);
            tvMore = window.getContentView().findViewById(R.id.upper_row_tv_more);
            tvClose = window.getContentView().findViewById(R.id.upper_row_tv_close);
            tvLblDic = window.getContentView().findViewById(R.id.lbl_dic);
            tvTerm = window.getContentView().findViewById(R.id.lbl_term);
            btnRemove3sym = window.getContentView().findViewById(R.id.remove3sym);
            btnRemove2sym = window.getContentView().findViewById(R.id.remove2sym);
            btnRemove1sym = window.getContentView().findViewById(R.id.remove1sym);
            btnDicExtended = window.getContentView().findViewById(R.id.btnDicExtended);
            body = window.getContentView().findViewById(R.id.articles_list);
            body2 = window.getContentView().findViewById(R.id.articles_list2);
        }
        int colr2 = colorGray;
        int colorFill = colorGrayC;
        if (isEInk) colorFill = Color.WHITE;
        if (fullScreen)
            ll1.setBackgroundColor(Color.argb(0, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));
        else
            ll1.setBackgroundColor(Color.argb(255, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));
        setBtnBackgroundColor(tvMore, colr2);
        setBtnBackgroundColor(tvClose, colr2);
//        if (fullScreen) {
//            Utils.setSolidButton1(tvMore);
//            Utils.setSolidButton1(tvClose);
//        }
        if (tvLblDic != null) {
            if (t.mCurDict != null)
                tvLblDic.setText(t.mCurDict.shortName);
            else
                tvLblDic.setText(t.mDicName);
            if (!fullScreen) {
                tvLblDic.setPaintFlags(tvLblDic.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                tvLblDic.setOnClickListener(v -> {
                    DictsDlg dlg = new DictsDlg(mActivity, mActivity.getReaderView(), t.sFindText, null, false);
                    doDismiss();
                    dlg.show();
                });
            }
        }
        //tvMore.setPaintFlags(tvMore.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvMore.setOnClickListener(v -> {
            Dictionaries dicts = new Dictionaries(mActivity);
            String ss = sFindText;
            if (ss.contains("~")) ss = ss.split("~")[1];
            if (Dictionaries.wikiSearch == null) Dictionaries.wikiSearch = new WikiSearch();
            Dictionaries.wikiSearch.wikiTranslate(mActivity, fullScreen, t.mCurDict, mReaderView, ss, t.mLink, t.mLink2,
                    t.mCurAction, mListSkipCount + t.wikiArticles.wikiArticleList.size(),
                    t.mUseFirstLink, 0 ,"", null);
            doDismiss();
        });
        int sz = 0;
        if (t.wikiArticles != null) sz = t.wikiArticles.wikiArticleList.size();
        if (sz == 0) ((ViewGroup) tvMore.getParent()).removeView(tvMore);
        tvClose.setOnClickListener(v -> doDismiss());
        CoolReader cr = mActivity;
        if (!fullScreen)
            if (cr.getReaderView() != null) {
                if (cr.getReaderView().getSurface() != null) {
                    ((MaxHeightLinearLayout) body).setMaxHeight(cr.getReaderView().getSurface().getHeight() * 8 / 12);
                }
            } else {
                ((MaxHeightLinearLayout) body).setMaxHeight(t.anchor.getHeight() * 8 / 12);
            }
        t.mWikiArticlesList = new WikiArticlesList(mActivity, t.wikiArticles.wikiArticleList, fullScreen);
        if (fullScreen)
            body2.addView(t.mWikiArticlesList);
        else
            body.addView(t.mWikiArticlesList);
        if (tvTerm != null) {
            tvTerm.setText(updTerm(t.sFindText));
            tvTerm.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            setBtnBackgroundColor(tvClose, colr2);
            setBtnBackgroundColor(tvClose, colr2);
            setBtnBackgroundColor(tvClose, colr2);
            btnRemove1sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove1sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove1sym);
            btnRemove2sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove2sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove2sym);
            btnRemove3sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove3sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove3sym);
            final String findText = StrUtils.getNonEmptyStr(t.sFindText, true);
            Utils.setSolidButton1(btnDicExtended);
            if (isEInk) Utils.setSolidButtonEink(btnDicExtended);
            btnDicExtended.setOnClickListener(v -> {
                try {
                    mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                    mActivity.mDictionaries.setAdHocFromTo(
                            mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                    doDismiss();
                    mActivity.mDictionaries.findInDictionary(findText, fullScreen,
                            mActivity.mDictionaries.lastDicView, true,
                            mActivity.mDictionaries.lastDC);
                } catch (Dictionaries.DictionaryException e) {
                    // do nothing
                }
            });
            if ((t.dicType != IS_OFFLINE) || (t.mCurDict == null)) Utils.hideView(btnDicExtended);
            if ((findText.length() <= 1) || (t.mCurDict == null)) Utils.hideView(btnRemove1sym);
            else
                btnRemove1sym.setOnClickListener(v -> {
                    if (findText.length() > 1) {
                        String findText1 = findText.substring(0, findText.length() - 1);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                    mActivity.mDictionaries.lastDicView, false,
                                    mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
            if ((findText.length() <= 2) || (t.mCurDict == null)) Utils.hideView(btnRemove2sym);
            else
                btnRemove2sym.setOnClickListener(v -> {
                    if (findText.length() > 2) {
                        String findText1 = findText.substring(0, findText.length() - 2);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                    mActivity.mDictionaries.lastDicView, false,
                                    mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
            if ((findText.length() <= 3) || (t.mCurDict == null)) Utils.hideView(btnRemove3sym);
            else
                btnRemove3sym.setOnClickListener(v -> {
                    if (findText.length() > 3) {
                        String findText1 = findText.substring(0, findText.length() - 3);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                    mActivity.mDictionaries.lastDicView, false,
                                    mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
        }
        if (fullScreen) {
            mActivity.tintViewIcons(dad.mBody, false);
            Utils.hideView(body);
            dad.show();
        } else {
            PopupWindow window = (PopupWindow) toastWindow;
            int[] location = new int[2];
            t.anchor.getLocationOnScreen(location);
            int popupY = location[1] + t.anchor.getHeight() - ll1.getHeight();
            mActivity.tintViewIcons(window, false);
            Utils.hideView(body2);
            window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
        }
    }

    private static void extListToast(Toast t, boolean fullScreen) {
        mInflater = (LayoutInflater) t.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dicContent = mInflater.inflate(R.layout.ext_dic_dlg, null, true);
        colorGray = themeColors.get(R.attr.colorThemeGray2);
        int colr2 = colorGray;
        int colorFill = colorGrayC;
        if (isEInk) colorFill = Color.WHITE;
        LinearLayout ll1;
        LinearLayout upperRow = null;
        Button tvClose;
        TextView tvLblDic;
        TextView tvTerm;
        Button btnRemove3sym;
        Button btnRemove2sym;
        Button btnRemove1sym;
        Button btnDicExtended;
        EditText edtDicTranls;
        ViewGroup body;
        ViewGroup body2;
        LinearLayout llUpperRowRecent;
        DicArticleDlg dad = null;
        if (fullScreen) {
            dad = new DicArticleDlg("DicArticleDlg",
                    DicToastView.mActivity, curToast, -1, dicContent);
            toastWindow = dad;
            ll1 = dad.mBody.findViewById(R.id.items_list_ll1);
            upperRow = dad.mBody.findViewById(R.id.upper_row_ll);
            tvClose = dad.mBody.findViewById(R.id.upper_row_tv_close);
            tvLblDic = dad.mBody.findViewById(R.id.lbl_dic);
            tvTerm = dad.mBody.findViewById(R.id.lbl_term);
            btnRemove3sym = dad.mBody.findViewById(R.id.remove3sym);
            btnRemove2sym = dad.mBody.findViewById(R.id.remove2sym);
            btnRemove1sym = dad.mBody.findViewById(R.id.remove1sym);
            btnDicExtended = dad.mBody.findViewById(R.id.btnDicExtended);
            edtDicTranls = dad.mBody.findViewById(R.id.edt_dic_tranls);
            body = dad.mBody.findViewById(R.id.items_list);
            body2 = dad.mBody.findViewById(R.id.items_list2);
            llUpperRowRecent = dad.mBody.findViewById(R.id.upper_row_ll_recent);
        } else {
            PopupWindow window = (PopupWindow) toastWindow;
            window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
            window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            window.setTouchable(true);
            window.setFocusable(false);
            window.setOutsideTouchable(true);
            window.setBackgroundDrawable(null);
            window.setContentView(dicContent);
            ll1 = window.getContentView().findViewById(R.id.items_list_ll1);
            tvClose = window.getContentView().findViewById(R.id.upper_row_tv_close);
            tvLblDic = window.getContentView().findViewById(R.id.lbl_dic);
            tvTerm = window.getContentView().findViewById(R.id.lbl_term);
            btnRemove3sym = window.getContentView().findViewById(R.id.remove3sym);
            btnRemove2sym = window.getContentView().findViewById(R.id.remove2sym);
            btnRemove1sym = window.getContentView().findViewById(R.id.remove1sym);
            btnDicExtended = window.getContentView().findViewById(R.id.btnDicExtended);
            edtDicTranls = window.getContentView().findViewById(R.id.edt_dic_tranls);
            body = window.getContentView().findViewById(R.id.items_list);
            body2 = window.getContentView().findViewById(R.id.items_list2);
            llUpperRowRecent = window.getContentView().findViewById(R.id.upper_row_ll_recent);
        }
        if (fullScreen)
            ll1.setBackgroundColor(Color.argb(0, Color.red(colorFill), Color.green(colorFill), Color.blue(colorFill)));
        else
            ll1.setBackgroundColor(Color.argb(255, Color.red(colorFill), Color.green(colorFill), Color.blue(colorFill)));
        setBtnBackgroundColor(tvClose, colr2);
        tvClose.setOnClickListener(v -> doDismiss());
        if (tvLblDic != null) {
            if (t.mCurDict != null)
                tvLblDic.setText(t.mCurDict.shortName);
            else
                tvLblDic.setText(t.mDicName);
            if (!fullScreen) {
                tvLblDic.setPaintFlags(tvLblDic.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                tvLblDic.setOnClickListener(v -> {
                    DictsDlg dlg = new DictsDlg(mActivity, mActivity.getReaderView(), t.sFindText, null, false);
                    doDismiss();
                    dlg.show();
                });
            }
        }
        if (tvTerm != null) {
            tvTerm.setText(updTerm(t.sFindText));
            if (llUpperRowRecent != null) {
                ViewGroup recentDicsPanel = (ViewGroup) mInflater.inflate(R.layout.recent_dic_panel_scroll, null, true);
                LinearLayout ll = recentDicsPanel.findViewById(R.id.ll_dic_buttons);
                initRecentDics(ll, updTerm(t.sFindText), fullScreen);
                llUpperRowRecent.addView(recentDicsPanel);
            }
            tvTerm.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            setBtnBackgroundColor(tvClose, colr2);
            btnRemove1sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove1sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove1sym);
            btnRemove2sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove2sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove2sym);
            btnRemove3sym.setTextColor(mActivity.getTextColor(themeColors.get(R.attr.colorIcon)));
            Utils.setSolidButton1(btnRemove3sym);
            if (isEInk) Utils.setSolidButtonEink(btnRemove3sym);
            final String findText = StrUtils.getNonEmptyStr(t.sFindText, true);
            Utils.setSolidButton1(btnDicExtended);
            if (isEInk) Utils.setSolidButtonEink(btnDicExtended);
            btnDicExtended.setOnClickListener(v -> {
                try {
                    mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                    mActivity.mDictionaries.setAdHocFromTo(
                            mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                    doDismiss();
                    mActivity.mDictionaries.findInDictionary(findText, fullScreen,
                            mActivity.mDictionaries.lastDicView, true,
                            mActivity.mDictionaries.lastDC);
                } catch (Dictionaries.DictionaryException e) {
                    // do nothing
                }
            });
            if ((t.dicType != IS_OFFLINE) || (t.mCurDict == null)) Utils.hideView(btnDicExtended);
            if ((fullScreen) || (t.mCurDict != null) || (StrUtils.isEmptyStr(t.msg))) Utils.hideView(edtDicTranls);
            if (!StrUtils.isEmptyStr(t.msg))
                if (edtDicTranls != null) {
                    edtDicTranls.setText(t.msg);
                    Utils.setHighLightedText(edtDicTranls, sFindText, mColorIconL);
                }
            if ((t.dicType != IS_OFFLINE) || (t.mCurDict == null)) Utils.hideView(btnDicExtended);
            if ((findText.length() <= 1) || (t.mCurDict == null)) Utils.hideView(btnRemove1sym);
            else
                btnRemove1sym.setOnClickListener(v -> {
                    if (findText.length() > 1) {
                        String findText1 = findText.substring(0, findText.length() - 1);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                    mActivity.mDictionaries.lastDicView, false,
                                    mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
            if ((findText.length() <= 2) || (t.mCurDict == null)) Utils.hideView(btnRemove2sym);
            else
                btnRemove2sym.setOnClickListener(v -> {
                    if (findText.length() > 2) {
                        String findText1 = findText.substring(0, findText.length() - 2);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                mActivity.mDictionaries.lastDicView, false,
                                mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
            if ((findText.length() <= 3) || (t.mCurDict == null)) Utils.hideView(btnRemove3sym);
            else
                btnRemove3sym.setOnClickListener(v -> {
                    if (findText.length() > 3) {
                        String findText1 = findText.substring(0, findText.length() - 3);
                        mActivity.mDictionaries.setAdHocDict(mActivity.mDictionaries.lastDicCalled);
                        mActivity.mDictionaries.setAdHocFromTo(
                                mActivity.mDictionaries.lastDicFromLang, mActivity.mDictionaries.lastDicToLang);
                        doDismiss();
                        try {
                            mActivity.mDictionaries.findInDictionary(findText1, fullScreen,
                                    mActivity.mDictionaries.lastDicView, false,
                                    mActivity.mDictionaries.lastDC);
                        } catch (Dictionaries.DictionaryException e) {
                            // do nothing
                        }
                    }
                });
        }
        int eight = 8;
        if ((t.mCurDict == null) && (!StrUtils.isEmptyStr(t.msg))) eight = 7;
        if (!fullScreen)
            if (mActivity.getReaderView() != null) {
                if (mActivity.getReaderView().getSurface() != null) {
                    ((MaxHeightLinearLayout) body).setMaxHeight(mActivity.getReaderView().getSurface().getHeight() * eight / 12);
                }
            } else {
                ((MaxHeightLinearLayout) body).setMaxHeight(t.anchor.getHeight() * eight / 12);
            }
        t.mExtDicList = new ExtDicList(mActivity, t, null, mHandler, handleDismiss, false);
        if (fullScreen)
            body2.addView(t.mExtDicList);
        else
            body.addView(t.mExtDicList);
        if (fullScreen) {
            mActivity.tintViewIcons(dad.mBody, false);
            Utils.hideView(upperRow);
            Utils.hideView(body);
            dad.show();
        } else {
            PopupWindow window = (PopupWindow) toastWindow;
            int[] location = new int[2];
            t.anchor.getLocationOnScreen(location);
            int popupY = location[1] + t.anchor.getHeight() - ll1.getHeight();
            mActivity.tintViewIcons(window, false);
            Utils.hideView(body2);
            window.showAtLocation(t.anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
        }
    }
}
