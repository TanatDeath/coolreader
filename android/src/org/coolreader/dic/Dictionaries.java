package org.coolreader.dic;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DicSearchHistoryEntry;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.onyx.OnyxTranslate;
import org.coolreader.options.OptionsDialog;
import org.coolreader.crengine.ProgressDialog;
import org.coolreader.readerview.ReaderView;
import org.coolreader.crengine.SelectionToolbarDlg;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;
import org.coolreader.dic.wiki.WikiSearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
//import com.abbyy.mobile.lingvo.api.MinicardContract;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import okhttp3.OkHttpClient;

public class Dictionaries {

	public static final String YND_DIC_ONLINE = "https://translate.yandex.net/api/v1.5/tr/translate";
	public static final String YND_DIC_ONLINE_2 = "https://translate.api.cloud.yandex.net/translate/v2/translate";
	public static final String YND_DIC_GETLANGS = "https://translate.yandex.net/api/v1.5/tr/getLangs";
	public static final String YND_DIC_GETLANGS_2 = "https://translate.api.cloud.yandex.net/translate/v2/languages";
	public static final String LINGVO_DIC_ONLINE = "https://developers.lingvolive.com/api";
	public static final String YND_DIC_GET_TOKEN = "https://iam.api.cloud.yandex.net/iam/v1/tokens";
	public static final String DEEPL_DIC_ONLINE = "https://api.deepl.com/v2";
	public static final String DEEPL_DIC_ONLINE_FREE = "https://api-free.deepl.com/v2";
	public static final String DICTCC_DIC_ONLINE = "https://{langpair}.dict.cc";
	public static final String GOOGLE_DIC_ONLINE = "https://translate.googleapis.com/translate_a/single";
	public static final String LINGUEE_DIC_ONLINE = "https://www.linguee.com/{src_lang_name}-{dst_lang_name}/search";
	public static final String GRAMOTA_RU_ONLINE = "http://www.gramota.ru/slovari/dic?lop=x&bts=x&ro=x&zar=x&ag=x&ab=x&sin=x&lv=x&az=x&pe=x";
	public static final String GLOSBE_ONLINE = "https://glosbe.com/{src_lang}/{dst_lang}/";
	public static final String TURENG_ONLINE = "https://tureng.com/en/{langpair}";
	public static final String URBAN_ONLINE = "https://urbandictionary.com/define.php";

	public static DeeplTranslate deeplTranslate = null;
	public static DictCCTranslate dictCCTranslate = null;
	public static GoogleTranslate googleTranslate = null;
	public static LingvoTranslate lingvoTranslate = null;
	public static WikiSearch wikiSearch = null;
	public static YandexTranslate yandexTranslate = null;
	public static LingueeTranslate lingueeTranslate = null;
	public static GramotaTranslate gramotaTranslate = null;
	public static GlosbeTranslate glosbeTranslate = null;
	public static TurengTranslate turengTranslate = null;
	public static UrbanTranslate urbanTranslate = null;
	public static OfflineDicTranslate offlineTranslate = null;
	public static OnyxapiTranslate onyxapiTranslate = null;

	public static OkHttpClient client = new OkHttpClient.Builder().
		connectTimeout(20,TimeUnit.SECONDS).
		writeTimeout(40, TimeUnit.SECONDS).
		readTimeout(40, TimeUnit.SECONDS).
//			socketFactory(new TLSSocketFactory()).
//		connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT,
//							 new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//									 .allEnabledTlsVersions().allEnabledCipherSuites().build())
//        ).
//		connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT,
//							 new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
//									 .allEnabledTlsVersions().allEnabledCipherSuites().build())
//        ).
		build();

	public static ArrayList<String> langCodes = new ArrayList<>();

	public static String dslStructToString(DicStruct dsl) {
		try {
			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			final String prettyJson = gson.toJson(dsl);
			return prettyJson;
		} catch (Exception e) {
		}
		return "";
	}

	public static class PopupFrameMetric {
		public final int Height;
		public final int HeightMore;
		public final int Gravity;
		public final int heightPixels;
		public final int widthPixels;

		PopupFrameMetric(DisplayMetrics metrics, int selectionTop, int selectionBottom) {
			heightPixels = metrics.heightPixels;
			widthPixels = metrics.widthPixels;
			final int topSpace = selectionTop;
			final int bottomSpace = metrics.heightPixels - selectionBottom;
			final boolean showAtBottom = bottomSpace >= topSpace;
			final int space = (showAtBottom ? bottomSpace : topSpace) - metrics.densityDpi / 12;
			// dont rememver what it was, but shown is too small on 7.8 inch einks
			//final int maxHeight = Math.min(metrics.densityDpi * 20 / 12, heightPixels * 2 / 3);
			//final int minHeight = Math.min(metrics.densityDpi * 10 / 12, heightPixels * 2 / 3);
			final int maxHeight = heightPixels * 2 / 3;
			final int minHeight = heightPixels * 2 / 3;

			Height = Math.max(minHeight, Math.min(maxHeight, space));
			HeightMore = ((Height * 2) / 3) * 2; //((Height * 2) / 5) * 3;
			Gravity = showAtBottom ? android.view.Gravity.BOTTOM : android.view.Gravity.TOP;
		}
	}

	private Activity mActivity;

	public Integer isiDic2IsActive() {
		return iDic2IsActive;
	}

	public void setiDic2IsActive(Integer iDic2IsActive) {
		log.i( "setiDic2IsActive: " + iDic2IsActive);
		this.iDic2IsActive = iDic2IsActive;
	}

	public void setAdHocDict(DictInfo dict) {
		this.currentDictionaryTmp = dict;
	}

	public void setAdHocFromTo(String sFrom, String sTo) {
		this.currentFromLangTmp = sFrom;
		this.currentToLangTmp = sTo;
	}

	private Integer iDic2IsActive = 0;

	public Dictionaries(Activity activity) {
		mActivity = activity;
		currentDictionary = defaultDictionary();
		currentDictionary2 = defaultDictionary();
		currentDictionary3 = noneDictionary();
		currentDictionary4 = noneDictionary();
		currentDictionary5 = noneDictionary();
		currentDictionary6 = noneDictionary();
		currentDictionary7 = noneDictionary();
		currentDictionary8 = noneDictionary();
		currentDictionary9 = noneDictionary();
		currentDictionary10 = noneDictionary();
	}

	public static ProgressDialog progressDlg;

	public DictInfo currentDictionary;
	public DictInfo currentDictionary2;
	public DictInfo currentDictionaryTmp;
	public String currentFromLangTmp;
	public String currentToLangTmp;
	public DictInfo currentDictionary3;
	public DictInfo currentDictionary4;
	public DictInfo currentDictionary5;
	public DictInfo currentDictionary6;
	public DictInfo currentDictionary7;
	public DictInfo currentDictionary8;
	public DictInfo currentDictionary9;
	public DictInfo currentDictionary10;
	public List<DictInfo> diRecent = new ArrayList<DictInfo>();

	public List<DictInfo> getAddDicts() {
		List<DictInfo> diAddDicts = new ArrayList<DictInfo>();
		if (currentDictionary != null)
			if (!currentDictionary.id.equals("NONE")) diAddDicts.add(currentDictionary);
		if (currentDictionary2 != null)
			if (!currentDictionary2.id.equals("NONE")) diAddDicts.add(currentDictionary2);
		if (currentDictionary3 != null)
			if (!currentDictionary3.id.equals("NONE")) diAddDicts.add(currentDictionary3);
		if (currentDictionary4 != null)
			if (!currentDictionary4.id.equals("NONE")) diAddDicts.add(currentDictionary4);
		if (currentDictionary5 != null)
			if (!currentDictionary5.id.equals("NONE")) diAddDicts.add(currentDictionary5);
		if (currentDictionary6 != null)
			if (!currentDictionary6.id.equals("NONE")) diAddDicts.add(currentDictionary6);
		if (currentDictionary7 != null)
			if (!currentDictionary7.id.equals("NONE")) diAddDicts.add(currentDictionary7);
		if (currentDictionary8 != null)
			if (!currentDictionary8.id.equals("NONE")) diAddDicts.add(currentDictionary8);
		if (currentDictionary9 != null)
			if (!currentDictionary9.id.equals("NONE")) diAddDicts.add(currentDictionary9);
		if (currentDictionary10 != null)
			if (!currentDictionary10.id.equals("NONE")) diAddDicts.add(currentDictionary10);
		return diAddDicts;
	}

	public static class DictInfo {
		public final String id;
		public final String name;
		public final String packageName;
		public final String className;
		public final String action;
		public final Integer internal;
		public final int dicIcon;
		public Drawable icon;
		public final String httpLink;
		public final boolean isOnline;
		public final String shortName;
		public String dataKey = SearchManager.QUERY;

		public boolean isInstalled() {
			return isInstalled;
		}

		public String getAddText(CoolReader cr) {
			if (id.equals("Wikipedia 1 (online)")) {
				String s = cr.settings().getProperty(Settings.PROP_CLOUD_WIKI1_ADDR);
				if (!StrUtils.isEmptyStr(s)) {
					s = s.replace(".wikipedia.org", "");
					s = s.replace("https://", "");
					s = s.replace("http://", "");
					return s;
				}
			}
			if (id.equals("Wikipedia 2 (online)")) {
				String s = cr.settings().getProperty(Settings.PROP_CLOUD_WIKI2_ADDR);
				if (!StrUtils.isEmptyStr(s)) {
					s = s.replace(".wikipedia.org", "");
					s = s.replace("https://", "");
					s = s.replace("http://", "");
					return s;
				}
			}
			return "";
		}

		public boolean isOnline() {
			return isOnline;
		}

		public void setInstalled(boolean installed) {
			isInstalled = installed;
		}

		public boolean isInstalled = false;
		public DictInfo (String id, String name, String packageName, String className, String action, Integer internal,
						  int dicIcon, Drawable icon, String httpLink, boolean isOnline, String shortName) {
			this.id = id;
			this.name = name;
			this.packageName = packageName;
			this.className = className;
			this.action = action;
			this.internal = internal;
			this.dicIcon = dicIcon;
			this.icon = icon;
			this.httpLink = httpLink;
			this.isOnline = isOnline;
			this.shortName = shortName;
		}
		public DictInfo setDataKey(String key) { this.dataKey = key; return this; }
	}

	static final DictInfo dicts[] = {
		new DictInfo("NONE", "(NONE)", "", "",
				Intent.ACTION_SEARCH, 0, 0, null, "", false, "None"),
		new DictInfo("Fora", "Fora Dictionary", "com.ngc.fora", "com.ngc.fora.ForaDictionary",
				Intent.ACTION_SEARCH, 0, R.drawable.fora, null, "", false, "Fora"),
		new DictInfo("ColorDict", "ColorDict", "com.socialnmobile.colordict",
				"com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 0, R.drawable.colordict, null, "", false, "Color"),
		new DictInfo("ColorDictApi", "ColorDict new / GoldenDict", "com.socialnmobile.colordict",
				"com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 1, R.drawable.goldendict, null, "'", false, "Golden"),
		new DictInfo("ColorDictApi (minicard)", "ColorDict new / GoldenDict (minicard)",
				"com.socialnmobile.colordict", "com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 6, R.drawable.goldendict, null, "", false, "Golden MC"),
		new DictInfo("AardDict", "Aard Dictionary", "aarddict.android", "aarddict.android.Article",
				Intent.ACTION_SEARCH, 0, R.drawable.aarddict, null, "", false, "Aard"),
		new DictInfo("AardDictLookup", "Aard Dictionary Lookup", "aarddict.android", "aarddict.android.Lookup",
				Intent.ACTION_SEARCH, 0, R.drawable.aarddict, null, "", false, "AardL"),
		new DictInfo("Aard2 lookup", "Aard 2 Dictionary Lookup", "itkach.aard2", "aard2.lookup",
				Intent.ACTION_SEARCH, 3, R.drawable.aard2, null, "", false, "Aard2L"),
		new DictInfo("Aard2 search", "Aard 2 Dictionary Search", "itkach.aard2", "aard2.search",
				Intent.ACTION_SEARCH, 3, R.drawable.aard2, null, "", false, "Aard2S"),
		new DictInfo("OnyxDictOld", "ONYX Dictionary (Old)", "com.onyx.dict",
				"com.onyx.dict.activity.DictMainActivity",
				Intent.ACTION_VIEW, 0, R.drawable.onyx_dictionary, null, "", false, "OnyxOld")
				.setDataKey("android.intent.action.SEARCH"),
		new DictInfo("OnyxDict", "ONYX Dictionary", "com.onyx.dict", "com.onyx.dict.main.ui.DictMainActivity",
				Intent.ACTION_VIEW, 0, R.drawable.onyx_dictionary, null, "", false, "Onyx")
				.setDataKey("android.intent.action.SEARCH"),
		new DictInfo("OnyxDictWindowed", "ONYX Dictionary (Windowed)", "com.onyx.dict", "com.onyx.dict.translation.ui.ProcessTextActivity",
				Intent.ACTION_VIEW, 0, R.drawable.onyx_dictionary, null, "", false, "OnyxW")
				.setDataKey("android.intent.extra.PROCESS_TEXT"),
			new DictInfo("Dictan", "Dictan Dictionary", "info.softex.dictan", null,
				Intent.ACTION_VIEW, 2, R.drawable.dictan, null, "", false, "Dictan"),
		new DictInfo("FreeDictionary.org", "Free Dictionary . org", "org.freedictionary",
				"org.freedictionary.MainActivity",
				"android.intent.action.VIEW", 0, R.drawable.freedictionary, null, "",
				false, "FreeD"),
		new DictInfo("ABBYYLingvo", "ABBYY Lingvo", "com.abbyy.mobile.lingvo.market",
				null /*com.abbyy.mobile.lingvo.market.MainActivity*/,
				"com.abbyy.mobile.lingvo.intent.action.TRANSLATE",
				0, R.drawable.lingvo, null, "", false, "Abbyy")
				.setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		new DictInfo("ABBYYLingvo (minicard)", "ABBYY Lingvo (minicard)",
				"com.abbyy.mobile.lingvo.market", null,
				"com.abbyy.mobile.lingvo.intent.action.TRANSLATE", 5, R.drawable.lingvo,
				null, "", false, "Abbyy MC")
				.setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		//new DictInfo("ABBYYLingvoLive", "ABBYY Lingvo Live", "com.abbyy.mobile.lingvolive", null, "com.abbyy.mobile.lingvo.intent.action.TRANSLATE", 0).setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		new DictInfo("LingoQuizLite", "Lingo Quiz Lite", "mnm.lite.lingoquiz",
				"mnm.lite.lingoquiz.ExchangeActivity",
				"lingoquiz.intent.action.ADD_WORD", 0, R.drawable.lingo_quiz, null,
				"", false, "LingoQuizLite").setDataKey("EXTRA_WORD"),
		new DictInfo("LingoQuiz", "Lingo Quiz", "mnm.lingoquiz", "mnm.lingoquiz.ExchangeActivity",
				"lingoquiz.intent.action.ADD_WORD", 0, R.drawable.lingo_quiz, null,
				"", false, "LingoQuiz").setDataKey("EXTRA_WORD"),
		new DictInfo("LEODictionary", "LEO Dictionary", "org.leo.android.dict",
				"org.leo.android.dict.LeoDict",
				"android.intent.action.SEARCH", 0, R.drawable.leo, null,
				"", false, "LEO").setDataKey("query"),
		new DictInfo("PopupDictionary", "Popup Dictionary",
				"com.barisatamer.popupdictionary", "com.barisatamer.popupdictionary.MainActivity",
				"android.intent.action.VIEW", 0,R.drawable.popup, null,
				"", false, "Popup"),
		new DictInfo("GoogleTranslate", "Google Translate",
				"com.google.android.apps.translate", "com.google.android.apps.translate.TranslateActivity",
				Intent.ACTION_SEND, 10, R.drawable.googledic, null, "", false, "Google"),
		new DictInfo("GoogleOnline", "Google Translate Online", "", "",
				Intent.ACTION_SEND, 13, R.drawable.googledic, null, GOOGLE_DIC_ONLINE, true, "GoogleO"),
		new DictInfo("GoogleOnline Extended", "Google Translate Online Extended", "", "",
					Intent.ACTION_SEND, 13, R.drawable.googledic, null, GOOGLE_DIC_ONLINE, true, "GoogleO Ex"),
		new DictInfo("YandexTranslate", "Yandex Translate", "ru.yandex.translate",
				"ru.yandex.translate.ui.activities.MainActivity",
				Intent.ACTION_SEND, 10, R.drawable.ytr_ic_launcher, null, "", false, "Yandex"),
		new DictInfo("Wikipedia", "Wikipedia", "org.wikipedia", "org.wikipedia.search.SearchActivity",
				Intent.ACTION_SEND, 10, R.drawable.wiki, null, "", false, "Wiki"),
		new DictInfo("YandexTranslateOnline", "Yandex Translate Online", "", "",
				Intent.ACTION_SEND, 7, R.drawable.ytr_ic_launcher, null, YND_DIC_ONLINE, true, "YandexO"),
		new DictInfo("LingvoOnline", "Lingvo Online", "", "",
					Intent.ACTION_SEND, 8, R.drawable.lingvo, null, LINGVO_DIC_ONLINE, true, "LingvoO"),
		new DictInfo("LingvoOnline Extended", "Lingvo Online Extended", "", "",
					Intent.ACTION_SEND, 8, R.drawable.lingvo, null, LINGVO_DIC_ONLINE, true, "LingvoO Ex"),
		new DictInfo("Wikipedia 1 (online)", "Wikipedia 1 (online)", "", "",
				Intent.ACTION_SEND, 9, R.drawable.wiki, null, "", true, "WikiO 1"),
		new DictInfo("Wikipedia 2 (online)", "Wikipedia 2 (online)", "", "",
				Intent.ACTION_SEND, 9, R.drawable.wiki, null, "", true, "WikiO 2"),
		new DictInfo("MDict", "MDict", "cn.mdict", "",
				Intent.ACTION_SEARCH, 10, R.drawable.mdict, null, "", false, "MDict"),
		new DictInfo("Deepl", "Deepl.com", "", "",
				Intent.ACTION_SEND, 11, R.drawable.deepl, null, DEEPL_DIC_ONLINE, true, "DeeplO"),
		new DictInfo("Dict.cc (online)", "Dict.cc (online)", "", "",
				Intent.ACTION_SEND, 12, R.drawable.dictcc, null, DICTCC_DIC_ONLINE, true, "DictCC_O"),
		new DictInfo("Linguee (online)", "Linguee (online)", "", "",
					Intent.ACTION_SEND, 14, R.drawable.linguee, null, LINGUEE_DIC_ONLINE, true, "LingueeO"),
		new DictInfo("Gramota.ru (online)", "Gramota.ru (online)", "", "",
				Intent.ACTION_SEND, 15, R.drawable.gramotaru, null, GRAMOTA_RU_ONLINE, true, "GramotaO"),
		new DictInfo("Glosbe (online)", "Glosbe (online)", "", "",
				Intent.ACTION_SEND, 16, R.drawable.glosbe, null, GLOSBE_ONLINE, true, "GlosbeO"),
		new DictInfo("Tureng (online)", "Tureng (online)", "", "",
				Intent.ACTION_SEND, 17, R.drawable.tureng, null, TURENG_ONLINE, true, "TurengO"),
		new DictInfo("Urban dictionary (online)", "Urban dictionary (online)", "", "",
				Intent.ACTION_SEND, 18, R.drawable.urban_dict, null, URBAN_ONLINE, true, "UrbanO"),
		new DictInfo("Offline dictionaries", "Offline dictionaries", "", "",
				Intent.ACTION_SEND, 19, R.drawable.icons8_offline_dics2, null, "", true, "Offline"),
		new DictInfo("OnyxDictAPI", "OnyxDict API", "", "",
				Intent.ACTION_SEND, 20, R.drawable.onyx_dictionary, null, "", true, "OnyxApi"),
	};

	public static List<DictInfo> dictsSendTo = new ArrayList<>();

	public static final String DEFAULT_DICTIONARY_ID = "Fora";
	public static final String DEFAULT_ONYX_DICTIONARY_ID = "OnyxDictWindowed";

	public static DictInfo findById(String id, BaseActivity act) {
		if (act == null) {
			for (DictInfo d : dicts) {
				if ((!d.id.equals("OnyxDictAPI")) || (OnyxTranslate.ONYX_API_TRANSLATE_AVAIL))
					if (d.id.equals(id))
						return d;
			}
		} else {
			for (DictInfo d : getDictList(act)) {
				if (d.id.equals(id))
					return d;
			}
		}
		return null;
	}

	public static DictInfo defaultDictionary() {
		if (DeviceInfo.EINK_ONYX)
			return findById(DEFAULT_ONYX_DICTIONARY_ID, null);
		return findById(DEFAULT_DICTIONARY_ID, null);
	}

	static DictInfo noneDictionary() {
		return findById("NONE", null);
	}

	public static void addDicsSendTo(BaseActivity act, List<DictInfo> ldi) {
		if (dictsSendTo.size()==0) {
			//intent for adding other apps
			Intent queryIntent = new Intent(Intent.ACTION_SEND);
			queryIntent.setType("text/plain");
			PackageManager pm = act.getPackageManager();
			List<ResolveInfo> resolveInfos = pm.queryIntentActivities(queryIntent, 0);
			for (int i = 0; i < resolveInfos.size(); i++) {
				ResolveInfo ri = resolveInfos.get(i);
				String packageName = ri.activityInfo.packageName;
				Drawable icon = null;
				try {
					icon = pm.getApplicationIcon(ri.activityInfo.packageName);
				}
				catch (PackageManager.NameNotFoundException e) {
					e.printStackTrace();
				}
				if ((!packageName.contains("coolreader"))&&(!packageName.contains("knownreader"))) {
					DictInfo di = new DictInfo(ri.activityInfo.name,
							ri.activityInfo.loadLabel(pm).toString(),
							packageName, ri.activityInfo.name,
							Intent.ACTION_SEND,4,0, icon, "", false,
							ri.activityInfo.loadLabel(pm).toString());
					dictsSendTo.add(di);
					ldi.add(di);
				}
			}

		} else {
			for (DictInfo di: dictsSendTo) ldi.add(di);
		}
	}

	public static List<DictInfo> getDictList(BaseActivity act) {
		List<DictInfo> ldi = new ArrayList<DictInfo>();
		for (DictInfo di: dicts)
			if ((!di.id.equals("OnyxDictAPI")) || (OnyxTranslate.ONYX_API_TRANSLATE_AVAIL))
				ldi.add(di);
		addDicsSendTo(act, ldi);
		return ldi;
	}


	public static List<DictInfo> getDictListExt(BaseActivity act, boolean bOnlyInstalled) {
		boolean bNeedSearchInstalled = true;
		for (DictInfo dict : dicts) {
			if (dict.isInstalled()) bNeedSearchInstalled = false;
		}
		ArrayList<DictInfo> dlist = new ArrayList<DictInfo>();

		if (bNeedSearchInstalled) {
			for (DictInfo dict : dicts) {
				dict.setInstalled(act.isPackageInstalled(dict.packageName));
				if (((dict.internal == 1)||(dict.internal == 6)) &&
						(dict.packageName.equals("com.socialnmobile.colordict")) && (!dict.isInstalled())) {
					dict.setInstalled(
							act.isPackageInstalled("mobi.goldendict.android")||
							act.isPackageInstalled("mobi.goldendict.androie") // changed package name - 4pda version 2.0.1b7
					);
				}
				if (dict.isOnline) dict.setInstalled(true);
			}
		}
		for (DictInfo dict : dicts) {
			if ((!dict.id.equals("OnyxDictAPI")) || (OnyxTranslate.ONYX_API_TRANSLATE_AVAIL))
				if ((dict.isInstalled()) || (!bOnlyInstalled)) dlist.add(dict);
		}
		addDicsSendTo(act, dlist);
		return dlist;
	}

	public void setDict( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary = d;
	}

	public void setDict2( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary2 = d;
	}

	public void setDict3( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary3 = d;
	}

	public void setDict4( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary4 = d;
	}

	public void setDict5( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary5 = d;
	}

	public void setDict6( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary6 = d;
	}

	public void setDict7( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary7 = d;
	}

	public void setDict8( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary8 = d;
	}

	public void setDict9( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary9 = d;
	}

	public void setDict10( String id, BaseActivity act ) {
		DictInfo d = findById(id, act);
		if (d != null)
			currentDictionary10 = d;
	}

	public boolean isPackageInstalled(String packageName) {
        PackageManager pm = mActivity.getPackageManager();
        try
        {
            pm.getPackageInfo(packageName, 0); //PackageManager.GET_ACTIVITIES);
            return true;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

	private final static int DICTAN_ARTICLE_REQUEST_CODE = 100;

	private final static String DICTAN_ARTICLE_WORD = "article.word";

	private final static String DICTAN_ERROR_MESSAGE = "error.message";

	private final static int FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;

	public static final Logger log = L.create("cr3dict");

	@SuppressWarnings("serial")
	public static class DictionaryException extends Exception {
		public DictionaryException(String msg) {
			super(msg);
		}
	}

	public String getCDinfo(DictInfo cd) {
		if (cd == null) return "null";
		return cd.name;
	}

	public DictInfo getCurDictionary() {
//		log.i("getCurDictionary, currentDictionary: "+getCDinfo(currentDictionary)+", iDic2IsActive = "+
//				iDic2IsActive+", currentDictionary2" + getCDinfo(currentDictionary2) +
//				", currentDictionaryTmp = " + getCDinfo(currentDictionaryTmp));
		DictInfo curDict = currentDictionary;
		if (iDic2IsActive > 0 && currentDictionary2 != null)
			curDict = currentDictionary2;
		if (currentDictionaryTmp != null)
			curDict = currentDictionaryTmp;
		return curDict;
	}

	private DictInfo saveCurrentDictionary;
	private DictInfo saveCurrentDictionary2;
	private DictInfo saveCurrentDictionaryTmp;
	private String saveFromLangTmp;
	private String saveToLangTmp;
	public DictInfo lastDicCalled;
	public String lastDicFromLang;
	public String lastDicToLang;
	public View lastDicView;
	public CoolReader.DictionaryCallback lastDC;
	private int saveIDic2IsActive;

	private void checkLangCodes() {
		if (langCodes.size()==0){
			try {
				InputStream is = mActivity.getResources().openRawResource(R.raw.lang_codes);
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String str = "";
				while ((str = reader.readLine()) != null) langCodes.add(str);
				is.close();
			} catch (Exception e) {
				log.e("load lang_codes file", e);
			}
		}
	}

	public interface LangListCallback {
		void click(TreeMap<String, String> lst);
	}

	public String get2dig(String s) {
		if (StrUtils.getNonEmptyStr(s,true).length()>2)
			return StrUtils.getNonEmptyStr(s,true).substring(0,2);
		return s;
	}

	public static void saveToDicSearchHistory(CoolReader cr, String searchText, String translateT, DictInfo curDict,
											  DicStruct dsl) {
		saveToDicSearchHistory(cr, searchText, translateT, curDict, dslStructToString(dsl));
	}

	public static void saveToDicSearchHistory(CoolReader cr, String searchText, String translateT, DictInfo curDict,
											  String dslStruct) {
		if (cr.mCurrentFrame != cr.mReaderFrame) return; // since we have dic on main screen too
		int iDont = cr.settings().getInt(Settings.PROP_APP_DICT_DONT_SAVE_IF_MORE, 0);
		boolean bDont2 = cr.settings().getBool(Settings.PROP_INSPECTOR_MODE_NO_DIC_HISTORY, false);
		if ((cr.getReaderView().inspectorModeActive) && (bDont2)) return;
		if (iDont>0) {
			if (StrUtils.getNonEmptyStr(searchText,true).split(" ").length > iDont) return;
		}
		DicSearchHistoryEntry dshe = new DicSearchHistoryEntry();
		dshe.setId(0L);
		String translateText = translateT;
		if (StrUtils.getNonEmptyStr(translateText, true).length()>1000)
			translateText = translateText.substring(0,999) + " ...";
		dshe.setSearch_text(searchText);
		dshe.setText_translate(translateText);
		dshe.setDslStruct(dslStruct);
		String sBookFName = "";
		String sLangFrom = "";
		String sLangTo = "";
		if (cr.getReaderView()!=null) {
			if (cr.getReaderView().getBookInfo()!=null)
				if (cr.getReaderView().getBookInfo().getFileInfo()!=null)
				{
					sBookFName = cr.getReaderView().getBookInfo().getFileInfo().getFilename();
					sLangFrom = cr.getReaderView().getBookInfo().getFileInfo().lang_from;
					sLangTo = cr.getReaderView().getBookInfo().getFileInfo().lang_to;
				}
		}
		CRC32 crc = new CRC32();
		if (!sBookFName.equals("")) {
			crc.update(sBookFName.getBytes());
			dshe.setSearch_from_book(String.valueOf(crc.getValue()));
		} else dshe.setSearch_from_book("");
		if (curDict == null)
			dshe.setDictionary_used("UserDic");
		else
			dshe.setDictionary_used(curDict.id);
		dshe.setCreate_time(System.currentTimeMillis());
		dshe.setLast_access_time(System.currentTimeMillis());
		dshe.setLanguage_from(sLangFrom);
		dshe.setLanguage_to(sLangTo);
		dshe.setSeen_count(1L);
		cr.getDB().updateDicSearchHistory(dshe, DicSearchHistoryEntry.ACTION_SAVE, cr);
	}

	public DictInfo getCurDict() {
		DictInfo curDict = currentDictionary;
		if (iDic2IsActive > 0 && currentDictionary2 != null)
			curDict = currentDictionary2;
		if (iDic2IsActive > 1)
			iDic2IsActive = 0;
		if (currentDictionaryTmp != null)
			curDict = currentDictionaryTmp;
		return curDict;
	}

	public DictInfo curDictByNum(String num, DictInfo curDict) {
		if (StrUtils.isEmptyStr(num)) return curDict;
		if (num.equals("1")) return currentDictionary;
		if (num.equals("2")) return currentDictionary2;
		if (num.equals("3")) return currentDictionary3;
		if (num.equals("4")) return currentDictionary4;
		if (num.equals("5")) return currentDictionary5;
		if (num.equals("6")) return currentDictionary6;
		if (num.equals("7")) return currentDictionary7;
		if (num.equals("8")) return currentDictionary8;
		if (num.equals("9")) return currentDictionary9;
		if (num.equals("10")) return currentDictionary10;
		return curDict;
	}

	public DictInfo getOfflineDicComformity(String sConformity, DictInfo curDict) {
		if (StrUtils.isEmptyStr(sConformity)) return curDict;
		String[] sConformityArr = sConformity.split(";");
		if (currentDictionary == curDict)
			if (sConformityArr.length>=0)
				return curDictByNum(sConformityArr[0], curDict);
		if (currentDictionary2 == curDict)
			if (sConformityArr.length>=1)
				return curDictByNum(sConformityArr[1], curDict);
		if (currentDictionary3 == curDict)
			if (sConformityArr.length>=2)
				return curDictByNum(sConformityArr[2], curDict);
		if (currentDictionary4 == curDict)
			if (sConformityArr.length>=3)
				return curDictByNum(sConformityArr[3], curDict);
		if (currentDictionary5 == curDict)
			if (sConformityArr.length>=4)
				return curDictByNum(sConformityArr[4], curDict);
		if (currentDictionary6 == curDict)
			if (sConformityArr.length>=5)
				return curDictByNum(sConformityArr[5], curDict);
		if (currentDictionary7 == curDict)
			if (sConformityArr.length>=6)
				return curDictByNum(sConformityArr[6], curDict);
		if (currentDictionary8 == curDict)
			if (sConformityArr.length>=7)
				return curDictByNum(sConformityArr[7], curDict);
		if (currentDictionary9 == curDict)
			if (sConformityArr.length>=8)
				return curDictByNum(sConformityArr[8], curDict);
		if (currentDictionary10 == curDict)
			if (sConformityArr.length>=9)
				return curDictByNum(sConformityArr[9], curDict);
		return curDict;
	}

	private FileInfo getFileParent(FileInfo fi) {
		FileInfo dfi = fi.parent;
		if (dfi == null) {
			dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
		}
		if (dfi == null) {
			File f = new File(fi.pathname);
			String par = f.getParent();
			if (par != null) {
				File df = new File(par);
				dfi = new FileInfo(df);
			}
		}
		return dfi;
	}

	private void editTransl(CoolReader cr, boolean fullScreen,
							FileInfo dfi, FileInfo fi, String langf, String lang, String s,
					   		int forWhat) {
		if (dfi != null) {
			currentDictionary = saveCurrentDictionary;
			currentDictionary2 = saveCurrentDictionary2;
			currentDictionaryTmp = saveCurrentDictionaryTmp;
			currentFromLangTmp = saveFromLangTmp;
			currentToLangTmp = saveToLangTmp;
			iDic2IsActive = saveIDic2IsActive;
			cr.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL, fullScreen, null, dfi, fi, langf, lang, s, null,
					forWhat, null);
		} else {
			cr.showToast(cr.getString(R.string.file_not_found)+": "+fi.getFilename());
		}
	}

	@SuppressLint("NewApi")
	public void findInDictionary(String s, boolean fullScreen, View view, boolean extended,
								 CoolReader.DictionaryCallback dcb) throws DictionaryException {
//		log.d("lookup in dictionary: " + s);
//		log.i("currentDictionary: "+getCDinfo(currentDictionary)+", iDic2IsActive = "+
//				iDic2IsActive+", currentDictionary2" + getCDinfo(currentDictionary2) +
//				", currentDictionaryTmp = " + getCDinfo(currentDictionaryTmp) +
//				", saveCurrentDictionary = " + getCDinfo(saveCurrentDictionary) +
//				", saveCurrentDictionary2 = " + getCDinfo(saveCurrentDictionary2) +
//				", saveCurrentDictionaryTmp = " + getCDinfo(saveCurrentDictionaryTmp) +
//				", saveIDic2IsActive = " + saveIDic2IsActive);
		// save - if we ask for transl direction
		saveCurrentDictionary = currentDictionary;
		saveCurrentDictionary2 = currentDictionary2;
		saveCurrentDictionaryTmp = currentDictionaryTmp;
		saveFromLangTmp = currentFromLangTmp;
		saveToLangTmp = currentToLangTmp;
		saveIDic2IsActive = iDic2IsActive;
		//
		DictInfo curDict = getCurDict();
		currentDictionaryTmp = null;
		String sFromLang = StrUtils.getNonEmptyStr(currentFromLangTmp, true);
		currentFromLangTmp = null;
		String sToLang = StrUtils.getNonEmptyStr(currentToLangTmp, true);
		currentToLangTmp = null;
		log.i("Chosen dic = "+getCDinfo(curDict));
		String lang = "?";
		String langf = "?";
		String sLang = "?";
		if (((CoolReader)mActivity).getReaderView() != null) {
			BookInfo book = ((CoolReader) mActivity).getReaderView().getBookInfo();
			lang = StrUtils.getNonEmptyStr(book.getFileInfo().lang_to, true);
			langf = StrUtils.getNonEmptyStr(book.getFileInfo().lang_from, true);
			sLang = StrUtils.getNonEmptyStr(book.getFileInfo().language,true);
		}
		if (!StrUtils.isEmptyStr(sToLang)) lang = sToLang;
		if (!StrUtils.isEmptyStr(sFromLang)) langf = sFromLang;
		if (StrUtils.isEmptyStr(langf)) langf = sLang;
		// play with network availability
		if (!((CoolReader)mActivity).isNetworkAvailable()) {
			String sConformity = ((CoolReader)mActivity).settings().getProperty(Settings.PROP_APP_ONLINE_OFFLINE_DICS);
			if (!StrUtils.isEmptyStr(sConformity)) curDict = getOfflineDicComformity(sConformity, curDict);
		}
		lastDicCalled = curDict;
		lastDicFromLang = langf;
		lastDicToLang = lang;
		lastDicView = view;
		lastDC = dcb;
		//\
		if (null == curDict) {
			((CoolReader)mActivity).optionsFilter = "";
			((CoolReader)mActivity).showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_DICTIONARY_TITLE);
			throw new DictionaryException(mActivity.getString(R.string.invalid_dic));
		}
		if (curDict.id.equals("NONE")) {
			((CoolReader)mActivity).optionsFilter = "";
			((CoolReader)mActivity).showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_DICTIONARY_TITLE);
			throw new DictionaryException(mActivity.getString(R.string.invalid_dic));
		}
		if (diRecent.contains(curDict)) diRecent.remove(curDict);
		diRecent.add(0,curDict);
		if (diRecent.size() > 5)
			while (diRecent.size() > 5) diRecent.remove(5);
		boolean isDouble = false;
		//save to dic search history
		CoolReader cr = (CoolReader) mActivity;
		isDouble = (cr.settings().getInt(Settings.PROP_LANDSCAPE_PAGES,1)==2) &&
				(cr.settings().getInt(Settings.PROP_LANDSCAPE_PAGES,1)==2);

		if (!curDict.isOnline)
			saveToDicSearchHistory(cr, s, "", curDict, "");

		final String SEARCH_ACTION  = "colordict.intent.action.SEARCH";
		final String EXTRA_QUERY   = "EXTRA_QUERY";
		final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";

		switch (curDict.internal) {
			case 0:
				Intent intent0 = new Intent(curDict.action);
				if (curDict.className != null) {
					intent0.setComponent(new ComponentName(
							curDict.packageName, curDict.className));
				} else {
					intent0.setPackage(curDict.packageName);
				}
				intent0.addFlags(DeviceInfo.getSDKLevel() >= 7 ? Intent.FLAG_ACTIVITY_CLEAR_TASK : Intent.FLAG_ACTIVITY_NEW_TASK);
				if (s!=null)
					intent0.putExtra(curDict.dataKey, s);
				try {
					mActivity.startActivity( intent0 );
				} catch ( ActivityNotFoundException e ) {
					throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
				} catch ( Exception e ) {
					throw new DictionaryException("Can't open dictionary \"" + curDict.name + "\"");
				}
				break;
			case 1:
				Intent intent1 = new Intent(SEARCH_ACTION);
				if (s!=null)
					intent1.putExtra(EXTRA_QUERY, s); //Search Query
				intent1.putExtra(EXTRA_FULLSCREEN, true); //
				try
				{
					mActivity.startActivity(intent1);
				} catch ( ActivityNotFoundException e ) {
					throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
				}
				break;
			case 2:
				// Dictan support
				Intent intent2 = new Intent("android.intent.action.VIEW");
				// Add custom category to run the Dictan external dispatcher
				intent2.addCategory("info.softex.dictan.EXTERNAL_DISPATCHER");

				// Don't include the dispatcher in activity
				// because it doesn't have any content view.
				intent2.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

				intent2.putExtra(DICTAN_ARTICLE_WORD, s);

				try {
					mActivity.startActivityForResult(intent2, DICTAN_ARTICLE_REQUEST_CODE);
				} catch (ActivityNotFoundException e) {
					throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
				}
				break;
			case 3:
				Intent intent3 = new Intent(curDict.className);
				intent3.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent3.putExtra(SearchManager.QUERY, s);
				intent3.putExtra("SENDER_ACTION", "KnownReader.sendText");
				try
				{
					mActivity.startActivity(intent3);
				} catch ( ActivityNotFoundException e ) {
					throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
				}
				break;
			case 4:
				Intent intent4 = new Intent(android.content.Intent.ACTION_SEND);
				intent4.setType("text/plain");
				ReaderView rv = ((CoolReader) mActivity).getReaderView();
				String subj = "";
				if (rv != null)
					if (rv.getBookInfo() != null) {
						String chapt = "";
						if (SelectionToolbarDlg.stSel != null) {
							chapt = Utils.getBookInfoToSend(SelectionToolbarDlg.stSel);
						}
						subj = rv.getBookInfo().getFileInfo().getAuthors() + " " + rv.getBookInfo().getFileInfo().getTitle() + ": " + chapt;
				}
				intent4.putExtra(android.content.Intent.EXTRA_SUBJECT, subj);
				intent4.putExtra(android.content.Intent.EXTRA_TEXT, s);
				intent4.setComponent(new ComponentName(curDict.packageName, curDict.className));
				try
				{
					mActivity.startActivity(intent4);
				} catch ( ActivityNotFoundException e ) {
					throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
				}
				break;
			case 5:
				Intent intent5 = new Intent(curDict.action);
				final DisplayMetrics metrics = new DisplayMetrics();
				mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
				int selectionTop = 0;
				int selectionBottom = 0;
				int selectionX1 = 0;
				int selectionX2 = 0;
				if (mActivity instanceof CoolReader) {
					if (cr.getReaderView()!=null) {
						if (cr.getReaderView().lastSelection != null) {
							selectionTop = cr.getReaderView().lastSelection.startY;
							selectionBottom = cr.getReaderView().lastSelection.endY;
							selectionX1 = cr.getReaderView().lastSelection.startX;
							selectionX2 = cr.getReaderView().lastSelection.endX;
						}
						if (cr.getReaderView().getBookInfo()!=null) {
							if (StrUtils.isEmptyStr(langf)) {
								if (sLang.toUpperCase().contains("РУССК")) sLang = "ru";
									else if (sLang.toUpperCase().startsWith("EN")) sLang = "en";
										else sLang = "";
								langf = sLang;
							}
							// ask book translation direction
							if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
								if (cr.getReaderView().mBookInfo!=null) {
									FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
									FileInfo dfi = getFileParent(fi);
									editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_LINGVO);
								};
								return;
							}
							//if (lang.equals("")) lang = "en";
							intent5.putExtra(LingvoConstants.EXTRA_LANGUAGE_TO, lang);
							intent5.putExtra(LingvoConstants.EXTRA_LANGUAGE_FROM, langf);
						}
					}
				}
				if (selectionBottom<selectionTop) {
					int dummy = selectionBottom;
					selectionBottom = selectionTop;
					selectionTop = dummy;
				}
				final PopupFrameMetric frameMetrics =
						new PopupFrameMetric(metrics, selectionTop, selectionBottom);

				if ((isDouble)&&(frameMetrics.widthPixels>frameMetrics.heightPixels)) {
					int iPage = 0;
					if ((selectionX1<(frameMetrics.widthPixels / 2)) &&
							(selectionX2<(frameMetrics.widthPixels / 2))) iPage = 1;
					if ((selectionX1>(frameMetrics.widthPixels / 2)) &&
							(selectionX2>(frameMetrics.widthPixels / 2))) iPage = 2;
					if (iPage == 1) {
						intent5.putExtra(LingvoConstants.EXTRA_WIDTH, frameMetrics.widthPixels / 2);
						intent5.putExtra(LingvoConstants.EXTRA_GRAVITY, Gravity.BOTTOM | Gravity.RIGHT);
					} else if (iPage  == 2) {
						intent5.putExtra(LingvoConstants.EXTRA_WIDTH, frameMetrics.widthPixels / 2);
						intent5.putExtra(LingvoConstants.EXTRA_GRAVITY, Gravity.BOTTOM | Gravity.LEFT);
					} else {
						intent5.putExtra(LingvoConstants.EXTRA_GRAVITY, frameMetrics.Gravity);
						if (
								((selectionBottom>frameMetrics.HeightMore)&&(selectionTop>frameMetrics.HeightMore)) ||
								((selectionBottom<(frameMetrics.heightPixels-frameMetrics.HeightMore))&&
										(selectionTop<(frameMetrics.heightPixels-frameMetrics.HeightMore)))
						   ) intent5.putExtra(LingvoConstants.EXTRA_HEIGHT, frameMetrics.HeightMore); else
						intent5.putExtra(LingvoConstants.EXTRA_HEIGHT, frameMetrics.Height);
					}
				} else {
					intent5.putExtra(LingvoConstants.EXTRA_GRAVITY, frameMetrics.Gravity);
					intent5.putExtra(LingvoConstants.EXTRA_HEIGHT, frameMetrics.Height);
				}

				intent5.putExtra(LingvoConstants.EXTRA_FORCE_LEMMATIZATION, true);
				intent5.putExtra(LingvoConstants.EXTRA_TRANSLATE_VARIANTS, true);
				intent5.putExtra(LingvoConstants.EXTRA_ENABLE_SUGGESTIONS, true);
				//intent5.putExtra(LingvoConstants.EXTRA_LIGHT_THEME, true);
				intent5.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				intent5.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				if (curDict.className != null || DeviceInfo.getSDKLevel() == 3) {
					intent5.setComponent(new ComponentName(
							curDict.packageName, curDict.className));
				} else {
					intent5.setPackage(curDict.packageName);
				}
				intent5.addFlags(DeviceInfo.getSDKLevel() >= 7 ? Intent.FLAG_ACTIVITY_CLEAR_TASK : Intent.FLAG_ACTIVITY_NEW_TASK);
				if (s!=null)
					intent5.putExtra(curDict.dataKey, s);
				try {
					mActivity.startActivity( intent5 );
				} catch ( ActivityNotFoundException e ) {
					throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
				} catch ( Exception e ) {
					throw new DictionaryException("Can't open dictionary \"" + curDict.name + "\"");
				}
				break;
			case 6:
				final String EXTRA_HEIGHT  = "EXTRA_HEIGHT";
				final String EXTRA_WIDTH   = "EXTRA_WIDTH";
				final String EXTRA_GRAVITY  = "EXTRA_GRAVITY";
	//			final String EXTRA_MARGIN_LEFT = "EXTRA_MARGIN_LEFT";
	//			final String EXTRA_MARGIN_TOP  = "EXTRA_MARGIN_TOP";
	//			final String EXTRA_MARGIN_BOTTOM = "EXTRA_MARGIN_BOTTOM";
	//			final String EXTRA_MARGIN_RIGHT = "EXTRA_MARGIN_RIGHT";

				Intent intent6 = new Intent(SEARCH_ACTION);
				if (s!=null)
					intent6.putExtra(EXTRA_QUERY, s); //Search Query
				final DisplayMetrics metrics2 = new DisplayMetrics();
				mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics2);
				int selectionTop2 = 0;
				int selectionBottom2 = 0;
				int selectionX1_2 = 0;
				int selectionX2_2 = 0;
				if (mActivity instanceof CoolReader) {
					if (cr.getReaderView()!=null) {
						if (cr.getReaderView().lastSelection != null) {
							selectionTop2 = cr.getReaderView().lastSelection.startY;
							selectionBottom2 = cr.getReaderView().lastSelection.endY;
							selectionX1_2 = cr.getReaderView().lastSelection.startX;
							selectionX2_2 = cr.getReaderView().lastSelection.endX;
						}
					}
				}

				if (selectionBottom2<selectionTop2) {
					int dummy = selectionBottom2;
					selectionBottom2 = selectionTop2;
					selectionTop2 = dummy;
				}
				final PopupFrameMetric frameMetrics2 =
						new PopupFrameMetric(metrics2, selectionTop2, selectionBottom2);
				intent6.putExtra(EXTRA_FULLSCREEN, false);
				if ((isDouble)&&(frameMetrics2.widthPixels>frameMetrics2.heightPixels)) {
					int iPage = 0;
					if ((selectionX1_2<(frameMetrics2.widthPixels / 2)) &&
					   (selectionX2_2<(frameMetrics2.widthPixels / 2))) iPage = 1;
					if ((selectionX1_2>(frameMetrics2.widthPixels / 2)) &&
							(selectionX2_2>(frameMetrics2.widthPixels / 2))) iPage = 2;
					if (iPage == 1) {
						intent6.putExtra(EXTRA_WIDTH, frameMetrics2.widthPixels / 2);
						intent6.putExtra(EXTRA_GRAVITY, Gravity.BOTTOM | Gravity.RIGHT);
					} else if (iPage  == 2) {
						intent6.putExtra(EXTRA_WIDTH, frameMetrics2.widthPixels / 2);
						intent6.putExtra(EXTRA_GRAVITY, Gravity.BOTTOM | Gravity.LEFT);
					} else {
						intent6.putExtra(EXTRA_HEIGHT, frameMetrics2.Height);
						intent6.putExtra(EXTRA_GRAVITY, frameMetrics2.Gravity);
					}
				} else {
					intent6.putExtra(EXTRA_GRAVITY, frameMetrics2.Gravity);
					//((CoolReader) mActivity).showToast("coords: "+selectionBottom2+" "+selectionTop2+" "+frameMetrics2.HeightMore+" "+
						//frameMetrics2.heightPixels+" "+frameMetrics2.Height);
					if (
							((selectionBottom2>frameMetrics2.HeightMore)&&(selectionTop2>frameMetrics2.HeightMore)) ||
									((selectionBottom2<(frameMetrics2.heightPixels-frameMetrics2.HeightMore))&&
											(selectionTop2<(frameMetrics2.heightPixels-frameMetrics2.HeightMore)))
							) {
						intent6.putExtra(EXTRA_HEIGHT, frameMetrics2.HeightMore);
						//((CoolReader) mActivity).showToast("more");
					} else
						intent6.putExtra(EXTRA_HEIGHT, frameMetrics2.Height);
				}

				try
				{
					mActivity.startActivity(intent6);
				} catch ( ActivityNotFoundException e ) {
					throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
				}
				break;
			case 7:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				if (StrUtils.isEmptyStr(langf)) langf = sLang;
				if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
					if (cr.getReaderView().mBookInfo != null) {
						FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
						FileInfo dfi = getFileParent(fi);
						editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_YND);
					};
					return;
				}
				if (yandexTranslate == null) yandexTranslate = new YandexTranslate();
				if (StrUtils.isEmptyStr(yandexTranslate.sYandexIAM))
					yandexTranslate.yandexAuthThenTranslate(cr, s, fullScreen, langf, lang, curDict, view, null, dcb);
				else
					yandexTranslate.yandexTranslate(cr, s, fullScreen,
							yandexTranslate.yndGetDefLangCode(langf), yandexTranslate.yndGetDefLangCode(lang),
							curDict, view, null, dcb);
				break;
			case 8:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				checkLangCodes();
				if (StrUtils.isEmptyStr(langf)) langf = sLang;
				if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
					if (cr.getReaderView() != null)
						if (cr.getReaderView().mBookInfo != null) {
							FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
							FileInfo dfi = getFileParent(fi);
							editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_LINGVO);
						};
					return;
				}
				if (lingvoTranslate == null) lingvoTranslate = new LingvoTranslate();
				if (lingvoTranslate.sLingvoToken.equals(""))
					lingvoTranslate.lingvoAuthThenTranslate(cr, s, fullScreen,
							langf, lang, curDict.id.contains("Extended"), curDict, view, dcb);
					else lingvoTranslate.lingvoTranslate(cr, s, fullScreen,
						lingvoTranslate.lingvoGetDefLangCode(langf), lingvoTranslate.lingvoGetDefLangCode(lang),
						curDict.id.contains("Extended"), curDict, view, dcb);
				break;
			case 9:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				String sLink = "";
				String sLink2 = "";
				if (mActivity instanceof CoolReader) {
					cr = (CoolReader) mActivity;
					if (curDict.id.contains("1")) {
						sLink = cr.settings().getProperty(Settings.PROP_CLOUD_WIKI1_ADDR, "https://en.wikipedia.org");
						sLink2 = cr.settings().getProperty(Settings.PROP_CLOUD_WIKI2_ADDR, "https://en.wikipedia.org");
					}
					else {
						sLink = cr.settings().getProperty(Settings.PROP_CLOUD_WIKI2_ADDR, "https://en.wikipedia.org");
						sLink2 = cr.settings().getProperty(Settings.PROP_CLOUD_WIKI1_ADDR, "https://en.wikipedia.org");
					}
				}
				if (wikiSearch == null) wikiSearch = new WikiSearch();
				wikiSearch.wikiTranslate(cr, fullScreen, curDict, view, s, sLink, sLink2,
						wikiSearch.WIKI_FIND_TITLE, true, dcb);
				break;
			case 10:
					Intent intent7 = new Intent();
					intent7.setType("text/plain");
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)  {
						intent7.setAction(Intent.ACTION_PROCESS_TEXT);
						intent7.putExtra(Intent.EXTRA_PROCESS_TEXT, s);
						intent7.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
					}else{
						intent7.setAction(Intent.ACTION_SEND);
						intent7.putExtra(Intent.EXTRA_TEXT, s);
					}
					for (ResolveInfo resolveInfo : mActivity.getPackageManager().queryIntentActivities(intent7, 0)) {

						if( resolveInfo.activityInfo.packageName.contains(curDict.packageName)){
							intent7.setComponent(new ComponentName(
									resolveInfo.activityInfo.packageName,
									resolveInfo.activityInfo.name));
							try
							{
								mActivity.startActivity(intent7);
							} catch ( ActivityNotFoundException e ) {
								throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
							}
						}

					}
					break;
			case 11:
					if (!FlavourConstants.PREMIUM_FEATURES) {
						cr.showToast(R.string.only_in_premium);
						return;
					}
					checkLangCodes();
					if (StrUtils.isEmptyStr(langf)) langf = sLang;
					if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
						if (cr.getReaderView() != null)
							if (cr.getReaderView().mBookInfo != null) {
								FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
								FileInfo dfi = getFileParent(fi);
								editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_COMMON);
							};
						return;
					}
					if (deeplTranslate == null) deeplTranslate = new DeeplTranslate();
					if (!deeplTranslate.deeplAuthenticated)
						deeplTranslate.deeplAuthThenTranslate(cr, s, fullScreen, langf, lang, curDict, view, dcb);
					else deeplTranslate.deeplTranslate(cr, s, fullScreen, deeplTranslate.deeplGetDefLangCode(langf, true),
								deeplTranslate.deeplGetDefLangCode(lang, false), curDict, view, dcb);
					break;
			case 12:
					if (!FlavourConstants.PREMIUM_FEATURES) {
						cr.showToast(R.string.only_in_premium);
						return;
					}
					if (StrUtils.isEmptyStr(langf)) langf = sLang;
					if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
						if (cr.getReaderView() != null)
							if (cr.getReaderView().mBookInfo != null) {
								FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
								FileInfo dfi = getFileParent(fi);
								editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_COMMON);
							};
						return;
					}
					if (dictCCTranslate == null) dictCCTranslate = new DictCCTranslate();
					dictCCTranslate.dictCCTranslate(cr, s, fullScreen, dictCCTranslate.dictccGetDefLangCode(langf),
							dictCCTranslate.dictccGetDefLangCode(lang), curDict, view, null, dcb);
					break;
			case 13:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				if (StrUtils.isEmptyStr(langf)) langf = sLang;
				if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
					if (cr.getReaderView() != null)
						if (cr.getReaderView().mBookInfo != null) {
							FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
							FileInfo dfi = getFileParent(fi);
							editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_COMMON);
						};
					return;
				}
				if (googleTranslate == null) googleTranslate = new GoogleTranslate();
				googleTranslate.googleTranslate(cr, s, fullScreen, googleTranslate.googleGetDefLangCode(langf),
						googleTranslate.googleGetDefLangCode(lang), curDict.id.contains("Extended"), curDict, view, null, dcb);
				break;
			case 14:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				if (StrUtils.isEmptyStr(langf)) langf = sLang;
				if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
					if (cr.getReaderView() != null)
						if (cr.getReaderView().mBookInfo != null) {
							FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
							FileInfo dfi = getFileParent(fi);
							editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_COMMON);
						};
					return;
				}
				if (lingueeTranslate == null) lingueeTranslate = new LingueeTranslate();
				lingueeTranslate.lingueeTranslate(cr, s, fullScreen,
						lingueeTranslate.lingueeGetDefLangCode(langf),
						lingueeTranslate.lingueeGetDefLangCode(lang), curDict, view, null, dcb);
				break;
			case 15:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				if (gramotaTranslate == null) gramotaTranslate = new GramotaTranslate();
				gramotaTranslate.gramotaTranslate(cr, s, fullScreen,
						"ru",
						"ru", curDict, view, null, dcb);
				break;
			case 16:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				if (StrUtils.isEmptyStr(langf)) langf = sLang;
				if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
					if (cr.getReaderView() != null)
						if (cr.getReaderView().mBookInfo != null) {
							FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
							FileInfo dfi = getFileParent(fi);
							editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_COMMON);
						};
					return;
				}
				if (glosbeTranslate == null) glosbeTranslate = new GlosbeTranslate();
				glosbeTranslate.glosbeTranslate(cr, s, fullScreen,
						glosbeTranslate.glosbeGetDefLangCode(langf),
						glosbeTranslate.glosbeGetDefLangCode(lang), curDict, view, null, dcb);
				break;
			case 17:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				if (StrUtils.isEmptyStr(langf)) langf = sLang;
				if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
					if (cr.getReaderView() != null)
						if (cr.getReaderView().mBookInfo != null) {
							FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
							FileInfo dfi = getFileParent(fi);
							editTransl(cr, fullScreen, dfi, fi, langf, lang, s, TranslationDirectionDialog.FOR_COMMON);
						};
					return;
				}
				if (turengTranslate == null) turengTranslate = new TurengTranslate();
				turengTranslate.turengTranslate(cr, s, fullScreen, langf, lang, curDict, view, null, dcb);
				break;
			case 18:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				if (urbanTranslate == null) urbanTranslate = new UrbanTranslate();
				urbanTranslate.urbanTranslate(cr, s, fullScreen, curDict, view, null, dcb);
				break;
			case 19:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				if (offlineTranslate == null) offlineTranslate = new OfflineDicTranslate();
				offlineTranslate.offlineDicTranslate(cr, s, fullScreen,
						langf, lang, curDict, view, extended, null, dcb);
				break;
			case 20:
				if (onyxapiTranslate == null) onyxapiTranslate = new OnyxapiTranslate();
				onyxapiTranslate.onyxapiTranslate(cr, s, fullScreen,
						langf, lang, curDict, view, extended, null, dcb);
				break;
			}


	}

    public void onActivityResult(int requestCode, int resultCode, Intent intent) throws DictionaryException {
        if (requestCode == DICTAN_ARTICLE_REQUEST_CODE) {
	       	switch (resultCode) {
	        	
	        	// The article has been shown, the intent is never expected null
			case Activity.RESULT_OK:
				break;
					
			// Error occured
			case Activity.RESULT_CANCELED: 
				String errMessage = "Unknown Error.";
				if (intent != null) {
					errMessage = "The Requested Word: " + 
					intent.getStringExtra(DICTAN_ARTICLE_WORD) + 
					". Error: " + intent.getStringExtra(DICTAN_ERROR_MESSAGE);
				}
				throw new DictionaryException(errMessage);
					
			// Must never occur
			default: 
				throw new DictionaryException("Unknown Result Code: " + resultCode);
			}
        }
	}
	
}
