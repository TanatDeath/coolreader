package org.coolreader.dic;

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.TLSSocketFactory;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DicSearchHistoryEntry;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.ProgressDialog;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import android.widget.Toast;

import com.abbyy.mobile.lingvo.api.MinicardContract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import okhttp3.Call;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class Dictionaries {

	public static final String YND_DIC_ONLINE = "https://translate.yandex.net/api/v1.5/tr/translate";
	public static final String YND_DIC_ONLINE_2 = "https://translate.api.cloud.yandex.net/translate/v2/translate";
	public static final String YND_DIC_GETLANGS = "https://translate.yandex.net/api/v1.5/tr/getLangs";
	public static final String YND_DIC_GETLANGS_2 = "https://translate.api.cloud.yandex.net/translate/v2/languages";
	public static final String LINGVO_DIC_ONLINE = "https://developers.lingvolive.com/api";
	public static final String YND_DIC_GET_TOKEN = "https://iam.api.cloud.yandex.net/iam/v1/tokens";
	public static final String DEEPL_DIC_ONLINE = "https://api.deepl.com/v2";

//	public static OkHttpClient client = new OkHttpClient();

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
	public static String sLingvoToken = "";
	public static String sYandexIAM = "";
	public static String sYandexIAMexpiresAt = "";
	public static int unauthCnt = 0;
	public static int unauthCntY = 0;
	public static int unauthCntDeepl = 0;
	public static boolean deeplAuthenticated = false;

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
			final int maxHeight = Math.min(metrics.densityDpi * 20 / 12, heightPixels * 2 / 3);
			final int minHeight = Math.min(metrics.densityDpi * 10 / 12, heightPixels * 2 / 3);

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
	}

	public static ProgressDialog progressDlg;
	
	public DictInfo currentDictionary;
	public DictInfo currentDictionary2;
	public DictInfo currentDictionaryTmp;
	public DictInfo currentDictionary3;
	public DictInfo currentDictionary4;
	public DictInfo currentDictionary5;
	public DictInfo currentDictionary6;
	public DictInfo currentDictionary7;
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
		public String dataKey = SearchManager.QUERY;

		public boolean isInstalled() {
			return isInstalled;
		}

		public boolean isOnline() {
			if (id.equals("YandexTranslateOnline") ||
				id.equals("LingvoOnline") ||
				id.equals("LingvoOnline Extended") ||
				id.equals("Wikipedia 1 (online)") ||
				id.equals("Wikipedia 2 (online)")
			) return true;
			return false;
		}

		public void setInstalled(boolean installed) {
			isInstalled = installed;
		}

		public boolean isInstalled = false;
		public DictInfo ( String id, String name, String packageName, String className, String action, Integer internal,
						  int dicIcon, Drawable icon, String httpLink, boolean isOnline) {
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
		}
		public DictInfo setDataKey(String key) { this.dataKey = key; return this; }
	}

	static final DictInfo dicts[] = {
		new DictInfo("NONE", "(NONE)", "", "",
				Intent.ACTION_SEARCH, 0, 0, null, "", false),
		new DictInfo("Fora", "Fora Dictionary", "com.ngc.fora", "com.ngc.fora.ForaDictionary",
				Intent.ACTION_SEARCH, 0, R.drawable.fora, null, "", false),
		new DictInfo("ColorDict", "ColorDict", "com.socialnmobile.colordict", "com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 0, R.drawable.colordict, null, "", false),
		new DictInfo("ColorDictApi", "ColorDict new / GoldenDict", "com.socialnmobile.colordict", "com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 1, R.drawable.goldendict, null, "'", false),
		new DictInfo("ColorDictApi (minicard)", "ColorDict new / GoldenDict (minicard)", "com.socialnmobile.colordict", "com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 6, R.drawable.goldendict, null, "", false),
		new DictInfo("AardDict", "Aard Dictionary", "aarddict.android", "aarddict.android.Article",
				Intent.ACTION_SEARCH, 0, R.drawable.aarddict, null, "", false),
		new DictInfo("AardDictLookup", "Aard Dictionary Lookup", "aarddict.android", "aarddict.android.Lookup",
				Intent.ACTION_SEARCH, 0, R.drawable.aarddict, null, "", false),
		new DictInfo("Aard2", "Aard 2 Dictionary", "itkach.aard2", "aard2.lookup",
				Intent.ACTION_SEARCH, 3, R.drawable.aard2, null, "", false),
		new DictInfo("Dictan", "Dictan Dictionary", "info.softex.dictan", null,
				Intent.ACTION_VIEW, 2, R.drawable.dictan, null, "", false),
		new DictInfo("FreeDictionary.org", "Free Dictionary . org", "org.freedictionary", "org.freedictionary.MainActivity",
				"android.intent.action.VIEW", 0, R.drawable.freedictionary, null, "", false),
		new DictInfo("ABBYYLingvo", "ABBYY Lingvo", "com.abbyy.mobile.lingvo.market", null /*com.abbyy.mobile.lingvo.market.MainActivity*/,
				"com.abbyy.mobile.lingvo.intent.action.TRANSLATE", 0, R.drawable.lingvo, null, "", false)
				.setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		new DictInfo("ABBYYLingvo (minicard)", "ABBYY Lingvo (minicard)", "com.abbyy.mobile.lingvo.market", null,
				"com.abbyy.mobile.lingvo.intent.action.TRANSLATE", 5, R.drawable.lingvo, null, "", false)
				.setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		//new DictInfo("ABBYYLingvoLive", "ABBYY Lingvo Live", "com.abbyy.mobile.lingvolive", null, "com.abbyy.mobile.lingvo.intent.action.TRANSLATE", 0).setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		new DictInfo("LingoQuizLite", "Lingo Quiz Lite", "mnm.lite.lingoquiz", "mnm.lite.lingoquiz.ExchangeActivity",
				"lingoquiz.intent.action.ADD_WORD", 0, R.drawable.lingo_quiz, null, "", false).setDataKey("EXTRA_WORD"),
		new DictInfo("LingoQuiz", "Lingo Quiz", "mnm.lingoquiz", "mnm.lingoquiz.ExchangeActivity",
				"lingoquiz.intent.action.ADD_WORD", 0, R.drawable.lingo_quiz, null, "", false).setDataKey("EXTRA_WORD"),
		new DictInfo("LEODictionary", "LEO Dictionary", "org.leo.android.dict", "org.leo.android.dict.LeoDict",
				"android.intent.action.SEARCH", 0, R.drawable.leo, null, "", false).setDataKey("query"),
		new DictInfo("PopupDictionary", "Popup Dictionary", "com.barisatamer.popupdictionary", "com.barisatamer.popupdictionary.MainActivity",
				"android.intent.action.VIEW", 0,R.drawable.popup, null, "", false),
		new DictInfo("GoogleTranslate", "Google Translate", "com.google.android.apps.translate", "com.google.android.apps.translate.TranslateActivity",
				Intent.ACTION_SEND, 10, R.drawable.googledic, null, "", false),
		new DictInfo("YandexTranslate", "Yandex Translate", "ru.yandex.translate", "ru.yandex.translate.ui.activities.MainActivity",
				Intent.ACTION_SEND, 10, R.drawable.ytr_ic_launcher, null, "", false),
		new DictInfo("Wikipedia", "Wikipedia", "org.wikipedia", "org.wikipedia.search.SearchActivity",
				Intent.ACTION_SEND, 10, R.drawable.wiki, null, "", false),
		new DictInfo("YandexTranslateOnline", "Yandex Translate Online", "", "",
				Intent.ACTION_SEND, 7, R.drawable.ytr_ic_launcher, null, YND_DIC_ONLINE, true),
		new DictInfo("LingvoOnline", "Lingvo Online", "", "",
					Intent.ACTION_SEND, 8, R.drawable.lingvo, null, LINGVO_DIC_ONLINE, true),
		new DictInfo("LingvoOnline Extended", "Lingvo Online Extended", "", "",
					Intent.ACTION_SEND, 8, R.drawable.lingvo, null, LINGVO_DIC_ONLINE, true),
		new DictInfo("Wikipedia 1 (online)", "Wikipedia 1 (online)", "", "",
				Intent.ACTION_SEND, 9, R.drawable.wiki, null, "", true),
		new DictInfo("Wikipedia 2 (online)", "Wikipedia 2 (online)", "", "",
				Intent.ACTION_SEND, 9, R.drawable.wiki, null, "", true),
		new DictInfo("MDict", "MDict", "cn.mdict", "",
				Intent.ACTION_SEARCH, 10, R.drawable.mdict, null, "", false),
		new DictInfo("Deepl", "Deepl.com", "", "",
				Intent.ACTION_SEND, 11, R.drawable.deepl, null, DEEPL_DIC_ONLINE, true),
	};

	public static List<DictInfo> dictsSendTo = new ArrayList<>();

	public static final String DEFAULT_DICTIONARY_ID = "Fora";
	
	static DictInfo findById(String id, BaseActivity act) {
		if (act == null) {
			for (DictInfo d : dicts) {
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
	
	static DictInfo defaultDictionary() {
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
		//		log.i("resolveInfos: " + ri.activityInfo.packageName + " / " + ri.activityInfo.name +
		//				ri.activityInfo.loadLabel(pm).toString());
				Drawable icon = null;
				try {
					icon = pm.getApplicationIcon(ri.activityInfo.packageName);
//					Drawable fakeIcon = act.getResources().getDrawable(R.drawable.lingvo);
//					Bitmap b = ((BitmapDrawable)icon).getBitmap();
//					Bitmap bitmapResized = Bitmap.createScaledBitmap(b,
//							fakeIcon.getIntrinsicWidth(), fakeIcon.getIntrinsicHeight(), false);
//					icon = new BitmapDrawable(act.getResources(), bitmapResized);
				}
				catch (PackageManager.NameNotFoundException e) {
					e.printStackTrace();
				}
				if ((!packageName.contains("coolreader"))&&(!packageName.contains("knownreader"))) {
					DictInfo di = new DictInfo(ri.activityInfo.name,
							ri.activityInfo.loadLabel(pm).toString(),
							packageName, ri.activityInfo.name,
							Intent.ACTION_SEND,4,0, icon, "", false);
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
		for (DictInfo di: dicts) ldi.add(di);
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
				if ((dict.internal == 7)||(dict.internal == 8)||(dict.internal == 9)) dict.setInstalled(true);
			}
		}
		for (DictInfo dict : dicts) {
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
		log.i("getCurDictionary, currentDictionary: "+getCDinfo(currentDictionary)+", iDic2IsActive = "+
				iDic2IsActive+", currentDictionary2" + getCDinfo(currentDictionary2) +
				", currentDictionaryTmp = " + getCDinfo(currentDictionaryTmp));
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

	private void lingvoAuthThenTranslate(String s, String langf, String lang, boolean extended, DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(LINGVO_DIC_ONLINE+"/v1.1/authenticate").newBuilder();
		String url = urlBuilder.build().toString();
		final CoolReader crf2 = (CoolReader) mActivity;
		crf2.readLingvoCloudSettings();
		String token = BuildConfig.LINGVO;
		if (!StrUtils.isEmptyStr(crf2.lingvoCloudSettings.lingvoToken)) token = crf2.lingvoCloudSettings.lingvoToken;
		RequestBody body = RequestBody.create(
				MediaType.parse("text/plain"), "");
		Request request = new Request.Builder()
				.header("Authorization","Basic " + token)
				.post(body)
				.url(url)
				.build();
		Call call = client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				if (StrUtils.getNonEmptyStr(response.message(), true).equals("Unauthorized")) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									crf2.showToast(crf2.getString(R.string.lingvo_unauth));
								else {
									if (dcb.showDicToast()) crf2.showToast(crf2.getString(R.string.lingvo_unauth));
									dcb.fail(null, crf2.getString(R.string.lingvo_unauth));
								}
							}, 100));
					return;
				}
				if (response.code() != 200) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									crf2.showToast(crf2.getString(R.string.http_error) + " " + response.code());
								else {
									if (dcb.showDicToast()) crf2.showToast(crf2.getString(R.string.http_error) + " " + response.code());
									dcb.fail(null, crf2.getString(R.string.http_error) + " " + response.code());
								}
							}, 100));
					return;
				}
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					sLingvoToken = sBody;
					lingvoTranslate(s, lingvoGetDefLangCode(langf), lingvoGetDefLangCode(lang), extended, curDict, view, dcb);
				}, 100));
			}
			public void onFailure(Call call, IOException e) {
				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									crf2.showToast(e.getMessage());
								else {
									if (dcb.showDicToast()) crf2.showToast(e.getMessage());
									dcb.fail(e, e.getMessage());
								}
							}, 100));
			}
		});
	};

	public void yandexAuthThenTranslate(String s, String langf, String lang, DictInfo curDict, View view, LangListCallback llc, CoolReader.DictionaryCallback dcb)  {
		final CoolReader crf2 = (CoolReader) mActivity;
		crf2.readYndCloudSettings();
		HttpUrl.Builder urlBuilder = HttpUrl.parse(YND_DIC_GET_TOKEN).newBuilder();
		String url = urlBuilder.build().toString();
		JSONObject json = new JSONObject();
		try {
			json.put("yandexPassportOauthToken", crf2.yndCloudSettings.oauthToken);
		} catch (JSONException e) {
			log.e("yandexAuthThenTranslate json error", e);
		}
		RequestBody body = RequestBody.create(
				MediaType.parse("application/json"), json.toString());
		Request request = new Request.Builder()
				.post(body)
				.url(url)
				.build();
		Call call = client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					try {
						JSONObject jso = new JSONObject(sBody);
						if (jso.has("iamToken")) sYandexIAM = jso.getString("iamToken");
						if (jso.has("expiresAt")) sYandexIAMexpiresAt = jso.getString("expiresAt");
						yandexTranslate(s, yndGetDefLangCode(langf), yndGetDefLangCode(lang), curDict ,view, llc, dcb);
					} catch (Exception e) {
						if (dcb == null)
							crf2.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
						else {
							if (dcb.showDicToast())
								crf2.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
							dcb.fail(e, e.getMessage());
						}
					}
					//BackgroundThread.instance().postBackground(() ->
					//		BackgroundThread.instance().postGUI(() -> crf2.showToast(sYandexIAM), 100));
				}, 100));
			}
			public void onFailure(Call call, IOException e) {
				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> crf2.showToast(e.getMessage()), 100));
			}
		});
	}

	public interface LangListCallback {
		void click(TreeMap<String, String> lst);
	}

	public void yandexTranslate(String s, String langf, String lang, DictInfo curDict, View view, LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		CoolReader cr = (CoolReader) mActivity;
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showToast(R.string.only_in_premium);
				return;
			}
			if (StrUtils.isEmptyStr(lang)) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showToast(cr.getString(R.string.translate_lang_not_set) + ": ["
								+ langf + "] -> [" + lang + "]"), 100));
				return;
			}
		}
		if ((StrUtils.isEmptyStr(sYandexIAM))) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showToast(cr.getString(R.string.cloud_need_authorization)), 100));
			cr.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_DICTIONARY_TITLE);
			return;
		}
		if (StrUtils.isEmptyStr(cr.yndCloudSettings.folderId)) cr.readYndCloudSettings();
		if (StrUtils.isEmptyStr(cr.yndCloudSettings.folderId)) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showToast(cr.getString(R.string.cloud_need_authorization)), 100));
			cr.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_DICTIONARY_TITLE);
			return;
		}
		HttpUrl.Builder urlBuilder;
		if (llc == null)
			urlBuilder = HttpUrl.parse(YND_DIC_ONLINE_2).newBuilder();
		else
			urlBuilder = HttpUrl.parse(YND_DIC_GETLANGS_2).newBuilder();
		String url = urlBuilder.build().toString();
		JSONObject json = new JSONObject();
		try {
			json.put("folder_id", cr.yndCloudSettings.folderId);
			if (llc == null) {
				json.put("texts", s);
				if (!StrUtils.isEmptyStr(langf))
					if (!langf.equals("#"))
						json.put("sourceLanguageCode", langf);
				json.put("targetLanguageCode", lang);
			}
		} catch (JSONException e) {
			log.e("yandexAuthThenTranslate json error", e);
		}
		RequestBody body = RequestBody.create(
				MediaType.parse("application/json"), json.toString());
		Request request = new Request.Builder()
				.header("Authorization","Bearer "+sYandexIAM)
				.post(body)
				.url(url)
				.build();
		Call call = client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					if (llc == null) {
						try {
							String sText = "";
							String sLang = "";
							JSONObject jso = new JSONObject(sBody);
							if (jso.has("translations")) {
								JSONArray jsoa = jso.getJSONArray("translations");
								if (jsoa.length() > 0) {
									JSONObject jso2 = (JSONObject) jsoa.get(0);
									if (jso2.has("text")) sText = jso2.getString("text");
									if (jso2.has("detectedLanguageCode"))
										sLang = jso2.getString("detectedLanguageCode");
								}
								if (dcb == null) {
									cr.showDicToast(s, sText, DicToastView.IS_YANDEX, sLang);
									saveToDicSearchHistory(s, sText, curDict);
								} else {
									dcb.done(sText);
									if (dcb.showDicToast()) {
										cr.showDicToast(s, sText, DicToastView.IS_YANDEX, sLang);
									}
									if (dcb.saveToHist()) {
										saveToDicSearchHistory(s, sText, curDict);
									}
								}
							} else {
								if (dcb == null)
									cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
								else {
									dcb.fail(null, sBody);
									if (dcb.showDicToast())
										cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
								}
							}
						} catch (Exception e) {
							if (dcb == null)
								cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
							else {
								dcb.fail(e, e.getMessage());
								if (dcb.showDicToast())
									cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
							}
						}
					} else {
						TreeMap<String, String> langs = new TreeMap<>();
						try {
							String sLang = "";
							String sName = "";
							JSONObject jso = new JSONObject(sBody);
							if (jso.has("languages")) {
								JSONArray jsoa = jso.getJSONArray("languages");
								for (int i = 0; i < jsoa.length(); i++) {
									JSONObject jso2 = (JSONObject) jsoa.get(i);
									if (jso2.has("code")) sLang = jso2.getString("code");
									if (jso2.has("name")) sName = jso2.getString("name");
									if (!StrUtils.isEmptyStr(sLang)) {
										langs.put(sLang, sName);
									}
								}
								llc.click(langs);
							} else cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
						} catch (Exception e) {
							cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
						}
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				sYandexIAM = "";
				if (unauthCntY == 0) {
					unauthCntY++;
					yandexAuthThenTranslate(s, langf, lang, curDict, view, llc, dcb);
				} else {
					cr.showToast(e.getMessage());
					unauthCntY = 0;
				}
			}
		});
	};

	private void deeplAuthThenTranslate(String s, String langf, String lang, DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		final CoolReader crf2 = (CoolReader) mActivity;
		deeplAuthenticated = false;
		crf2.readDeeplCloudSettings();
		HttpUrl.Builder urlBuilder = HttpUrl.parse(DEEPL_DIC_ONLINE+"/usage").newBuilder().addQueryParameter("auth_key", crf2.deeplCloudSettings.deeplToken);
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.url(url)
				.build();
		Call call = client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				if ((response.code() == 403) || (response.code() == 456)) {
					String sErr = "";
					if (response.code() == 403) sErr = crf2.getString(R.string.deepl_unauth);
					if (response.code() == 456) sErr = crf2.getString(R.string.deepl_quoata);
					String finalSErr = sErr;
					BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> {
							if (dcb == null)
								crf2.showToast(finalSErr);
							else {
								if (dcb.showDicToast()) crf2.showToast(finalSErr);
								dcb.fail(null, finalSErr);
							}
						}, 100));
					return;
				}
				if (response.code() != 200) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									crf2.showToast(crf2.getString(R.string.http_error) + " " + response.code());
								else {
									if (dcb.showDicToast()) crf2.showToast(crf2.getString(R.string.http_error) + " " + response.code());
									dcb.fail(null, crf2.getString(R.string.http_error) + " " + response.code());
								}
							}, 100));
					return;
				}
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					deeplAuthenticated = true;
					deeplTranslate(s, deeplGetDefLangCode(langf, true), deeplGetDefLangCode(lang, false), curDict, view, dcb);
				}, 100));
			}
			public void onFailure(Call call, IOException e) {
				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> {
							if (dcb == null)
								crf2.showToast(e.getMessage());
							else {
								if (dcb.showDicToast()) crf2.showToast(e.getMessage());
								dcb.fail(e, e.getMessage());
							}
						}, 100));
			}
		});
	};

	public HttpUrl.Builder wikiUrlBuilder(String s, String link, int curAction, int listSkipCount, int prevAction) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(link).newBuilder();
		if (curAction == WIKI_FIND_TITLE) {
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "extracts");
			urlBuilder.addQueryParameter("titles", s);
			urlBuilder.addQueryParameter("exintro", "1");
			urlBuilder.addQueryParameter("explaintext", "1");
		}
		if (curAction == WIKI_FIND_TITLE_FULL) {
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "extracts");
			urlBuilder.addQueryParameter("titles", s);
			urlBuilder.addQueryParameter("explaintext", "1");
		}
		if (curAction == WIKI_FIND_LIST) {
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("list", "search");
			urlBuilder.addQueryParameter("srsearch", s);
			urlBuilder.addQueryParameter("srwhat", "text");
			if (listSkipCount > 0) {
				urlBuilder.addQueryParameter("sroffset", "" + listSkipCount);
			}
		}
		if (curAction == WIKI_SHOW_PAGE_ID) {
			String ss = s;
			if (ss.contains("~")) ss=s.split("~")[0];
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "extracts");
			urlBuilder.addQueryParameter("pageids", ss);
			urlBuilder.addQueryParameter("explaintext", "1");
			urlBuilder.addQueryParameter("exintro", "1");
		}
		if (curAction == WIKI_SHOW_PAGE_FULL_ID) {
			String ss = s;
			if (ss.contains("~")) ss=s.split("~")[0];
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "extracts");
			urlBuilder.addQueryParameter("pageids", ss);
			urlBuilder.addQueryParameter("explaintext", "1");
		}
		if ((curAction == WIKI_FIND_PIC_INFO)
				&& ((prevAction == WIKI_FIND_TITLE)||(prevAction == WIKI_FIND_TITLE_FULL))) {
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "pageimages");
			urlBuilder.addQueryParameter("titles", s);
			int i = 200;
			CoolReader cr = (CoolReader)mActivity;
			if (cr.getReaderView() != null)
				if (cr.getReaderView().getSurface() != null)
					i = Math.min(cr.getReaderView().getSurface().getWidth(),cr.getReaderView().getSurface().getHeight()) / 3;
			urlBuilder.addQueryParameter("pithumbsize", String.valueOf(i));
		}
		if ((curAction == WIKI_FIND_PIC_INFO)
				&& ((prevAction == WIKI_SHOW_PAGE_ID)||(prevAction == WIKI_SHOW_PAGE_FULL_ID))) {
			String ss = s;
			if (ss.contains("~")) ss=s.split("~")[0];
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "pageimages");
			urlBuilder.addQueryParameter("pageids", ss);
			int i = 200;
			CoolReader cr = (CoolReader)mActivity;
			if (cr.getReaderView() != null)
				if (cr.getReaderView().getSurface() != null)
					i = Math.min(cr.getReaderView().getSurface().getWidth(),cr.getReaderView().getSurface().getHeight()) / 5 * 2;
			urlBuilder.addQueryParameter("pithumbsize", String.valueOf(i));
		}
		return urlBuilder;
	}

	public static int WIKI_FIND_TITLE = 1;
	public static int WIKI_FIND_LIST = 2;
	public static int WIKI_SHOW_PAGE_ID = 3;
	public static int WIKI_SHOW_PAGE_FULL_ID = 4;
	public static int WIKI_FIND_TITLE_FULL = 5;
	public static int WIKI_FIND_PIC_INFO = 6;

	private String wikiTitleText = "";
	private String wikiLink = "";

	public void wikiTranslate(CoolReader cr, DictInfo curDict, View view, String s, String link, String link2,
							  int curAction, boolean useFirstLink, CoolReader.DictionaryCallback dcb) {
		wikiTranslate(cr, curDict, view, s, link, link2, curAction, 0, useFirstLink, 0, "", dcb);
	}

	public void wikiTranslate(CoolReader cr, DictInfo curDict, View view, String s, String link, String link2,
							  int curAction, boolean useFirstLink, int prevAction, String articleText, CoolReader.DictionaryCallback dcb) {
		wikiTranslate(cr, curDict, view, s, link, link2, curAction, 0, useFirstLink, prevAction, articleText, dcb);
	}

	public void wikiTranslate(CoolReader cr, DictInfo curDict, View view, String s, String link, String link2,
							  int curAction, int listSkipCount, boolean useFirstLink,
							  int prevAction, String articleText, CoolReader.DictionaryCallback dcb) {
		if (StrUtils.isEmptyStr(link)) return;
		boolean saveHist = cr.getReaderView().getSettings().getBool(Settings.PROP_CLOUD_WIKI_SAVE_HISTORY,false);
		final String sLinkF = link;
		final String sLinkF2 = link2;
		String sLink = link + "/w/api.php";
		String sLink2 = link2 + "/w/api.php";
		HttpUrl.Builder urlBuilder = wikiUrlBuilder(s, useFirstLink ? sLink : sLink2, curAction,
				listSkipCount, prevAction);
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.url(url)
				.build();
		Call call = client.newCall(request);
		final CoolReader crf2 = cr;
		final DictInfo curDictF2 = curDict;
		if ((curAction == WIKI_FIND_TITLE_FULL) || (curAction == WIKI_SHOW_PAGE_FULL_ID)) {
			progressDlg = ProgressDialog.show(mActivity,
					mActivity.getString(R.string.network_op),
					mActivity.getString(R.string.network_op),
					true, false, null);
		}
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				Document docJsoup = Jsoup.parse(sBody, sLinkF);
				BackgroundThread.instance().postGUI(() -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
				});
				wikiTitleText = "";
				if ((curAction == WIKI_FIND_TITLE)
						|| (curAction == WIKI_SHOW_PAGE_ID)
						|| (curAction == WIKI_SHOW_PAGE_FULL_ID)
						|| (curAction == WIKI_FIND_TITLE_FULL)
				) {
					Elements results = docJsoup.select("api > query > pages > page > extract");
					if (results.size() > 0) wikiTitleText = results.text(); else
						wikiTitleText = Utils.cleanupHtmlTags(sBody);
				}
				final String sTranslF = wikiTitleText;
				// if found article
				if ((!StrUtils.isEmptyStr(wikiTitleText)) &&
						(curAction == WIKI_FIND_TITLE) || (curAction == WIKI_FIND_TITLE_FULL)
				) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						wikiLink = ((curAction == WIKI_FIND_TITLE) || (curAction == WIKI_FIND_TITLE_FULL)) ? sLinkF : sLinkF2;
//						crf2.showDicToastWiki(s, sTranslF, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
//								curDict, link, link2, curAction, useFirstLink);
						if (saveHist) saveToDicSearchHistory(s, sTranslF, curDict);
						wikiTranslate(cr, curDict, view, s, link, link2,
								WIKI_FIND_PIC_INFO, useFirstLink, curAction, sTranslF, dcb);
					}, 100));
					return;
				}
				// not found in first link - try in 2nd
				if ((StrUtils.isEmptyStr(wikiTitleText)) && ((curAction == WIKI_FIND_TITLE) || (curAction == WIKI_FIND_TITLE_FULL)) &&
						(!StrUtils.isEmptyStr(sLink2)) && (useFirstLink)) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						wikiLink = link2;
						wikiTranslate(cr, curDict, view, s, link, link2,
								(curAction == WIKI_FIND_TITLE) ? WIKI_FIND_TITLE : WIKI_FIND_TITLE_FULL, false, dcb);
					}, 100));
					return;
				}
				// not found - try to show list
				if ((StrUtils.isEmptyStr(wikiTitleText)) &&
						((curAction == WIKI_FIND_TITLE) || (curAction == WIKI_FIND_TITLE_FULL))
				) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
							wikiTranslate(cr, curDict, view, s, link, link2, WIKI_FIND_LIST, true, dcb), 100));
					return;
				}
				if (curAction == WIKI_FIND_LIST) {
					Elements results = docJsoup.select("api > query > search > p");
					ArrayList<WikiArticle> arrWA = new ArrayList<>();
					for (Element el: results) {
						WikiArticle wa = new WikiArticle(el.attr("title"), Long.valueOf(el.attr("pageid")), el.attr("snippet"));
						arrWA.add(wa);
					}
					if (arrWA.size() > 0) {
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
								crf2.showWikiListToast(s, sTranslF, view, DicToastView.IS_WIKI, wikiLink,
								arrWA, curDict, link, link2, curAction, listSkipCount, useFirstLink), 100));
						return;
					} else {
						if ((curAction == WIKI_FIND_LIST) && (useFirstLink)) {
							BackgroundThread.instance().postBackground(
									() -> BackgroundThread.instance().postGUI(() ->
											wikiTranslate(cr, curDict, view, s, link, link2, WIKI_FIND_LIST, false, dcb), 100)
							);
							return;
						} else
							BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(
									() -> crf2.showToast(mActivity.getString(R.string.not_found)), 100)
							);
					}
				}
				if ((!StrUtils.isEmptyStr(wikiTitleText)) &&
						(
								(curAction == WIKI_SHOW_PAGE_ID) ||
								(curAction == WIKI_SHOW_PAGE_FULL_ID)
						)
				) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						wikiLink = useFirstLink ? sLinkF : sLinkF2;
						//crf2.showDicToastWiki(s, sTranslF, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
						//		curDict, link, link2, curAction, useFirstLink);
						if (s.contains("~")) {
							if (saveHist) saveToDicSearchHistory(s.split("~")[1], sTranslF, curDict);
						}
						wikiTranslate(cr, curDict, view, s, link, link2,
								WIKI_FIND_PIC_INFO, useFirstLink, curAction, sTranslF, dcb);
					}, 100));
					return;
				}
				if ((curAction == WIKI_FIND_PIC_INFO)
				) {
					Elements results = docJsoup.select("api > query > pages > page > thumbnail");
					if (results.size() > 0) {
						Element el = results.get(0);
						String addr = el.attr("source");
						// showing with pic
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
							if (dcb == null)
								crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
									curDict, link, link2, prevAction, useFirstLink, addr);
							else {
								dcb.done(articleText);
								if (dcb.showDicToast()) {
									crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
											curDict, link, link2, prevAction, useFirstLink, addr);
								}
							}
						}, 100));
					} else {
						// showing with no pic
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
							if (dcb == null)
								crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
									curDict, link, link2, prevAction, useFirstLink, "");
							else {
								dcb.done(articleText);
								if (dcb.showDicToast()) {
									crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
											curDict, link, link2, prevAction, useFirstLink, "");
								}
							}
						}, 100));
					};
				}
			}

			public void onFailure(Call call, IOException e) {
				// showing with no pic
				BackgroundThread.instance().postGUI(() -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
				});
				if (curAction == WIKI_FIND_PIC_INFO)
					// showing with no pic
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						if (dcb == null)
							crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
								curDict, link, link2, prevAction, useFirstLink, "");
						else {
							dcb.done(articleText);
							if (dcb.showDicToast())
								crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
										curDict, link, link2, prevAction, useFirstLink, "");
						}
					}, 100));
				else
					// nothing to show - article did not load
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						crf2.showToast(e.getMessage()), 100)
				);
			}
		});
	}

	private String lingvoGetDefLangCode(String langCode) {
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("en")) return "en-us";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("az")) return "az-az";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("nl")) return "nl-nl";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("fr")) return "fr-fr";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("de")) return "de-de";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("it")) return "it-it";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("ms")) return "ms-my";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("pt")) return "pt-pt";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("es")) return "es-es";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("uz")) return "uz-uz";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("sv")) return "sv-se";
		return langCode;
	}

	private String yndGetDefLangCode(String langCode) {
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("en-us")) return "en";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("az-az")) return "az";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("nl-nl")) return "nl";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("fr-fr")) return "fr";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("de-de")) return "de";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("it-it")) return "it";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("ms-my")) return "ms";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("pt-pt")) return "pt";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("es-es")) return "es";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("uz-uz")) return "uz";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("sv-se")) return "sv";
		return langCode;
	}

	private String deeplGetDefLangCode(String langCode, boolean isSrc) {
		if (isSrc) {
			if (StrUtils.getNonEmptyStr(langCode,true).toLowerCase().startsWith("en-")) return "en";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("nl-")) return "nl";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("fr-")) return "fr";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("de-")) return "de";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("it-")) return "it";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("es-")) return "es";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("pt-")) return "pt";
		} else {
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().equalsIgnoreCase("en")) return "en-us";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("nl-")) return "nl";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("fr-")) return "fr";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("de-")) return "de";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("it-")) return "it";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().equalsIgnoreCase("pt")) return "pt-pt";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("es-")) return "es";
		}
		return StrUtils.getNonEmptyStr(langCode, true).toUpperCase();
	}

	private void lingvoTranslate(String s, String langf, String lang, boolean extended, DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		CoolReader cr = (CoolReader) mActivity;
		if (!FlavourConstants.PREMIUM_FEATURES) {
			cr.showToast(R.string.only_in_premium);
			return;
		}
		if ((StrUtils.isEmptyStr(langf))||(StrUtils.isEmptyStr(lang))) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showToast(cr.getString(R.string.translate_lang_not_set)+": ["
				+langf+"] -> ["+lang + "]"), 100));
			return;
		}
		int ilangf = 0;
		int ilang = 0;
		try {
			ilangf = Integer.valueOf(langf);
		} catch (Exception e) {
			ilangf = 0;
		}
		try {
			ilang = Integer.valueOf(lang);
		} catch (Exception e) {
			ilang = 0;
		}
		if ((ilangf == 0) || (ilang == 0)) {
			for (String lc: langCodes) {
				if (lc.split("~").length == 6) {
					String slang = lc.split("~")[2];
					if (StrUtils.isEmptyStr(slang)) slang = lc.split("~")[1];
					if (ilangf == 0) {
						try {
							if (langf.toUpperCase().equals(slang.toUpperCase()))
								ilangf = Integer.valueOf(lc.split("~")[3]);
						} catch (Exception e) {

						}
					}
					if (ilang == 0) {
						try {
							if (lang.toUpperCase().equals(slang.toUpperCase()))
								ilang = Integer.valueOf(lc.split("~")[3]);
						} catch (Exception e) {

						}
					}
				}
			}
			for (String lc: langCodes) {
				if (lc.split("~").length == 6) {
					String slang = lc.split("~")[1];
					if (ilangf == 0) {
						try {
							if (langf.toUpperCase().equals(slang.toUpperCase()))
								ilangf = Integer.valueOf(lc.split("~")[3]);
						} catch (Exception e) {

						}
					}
					if (ilang == 0) {
						try {
							if (lang.toUpperCase().equals(slang.toUpperCase()))
								ilang = Integer.valueOf(lc.split("~")[3]);
						} catch (Exception e) {

						}
					}
				}
			}
		}
		final int ilangfF = ilangf;
		final int ilangF = ilang;
		if ((ilangf == 0) || (ilang == 0)) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showToast(cr.getString(R.string.translate_lang_not_found)+": ["
					+langf+ " {" + ilangfF + "}" + "] -> ["+lang + " {" + ilangF + "}]"), 100));
			return;
		}
		HttpUrl.Builder urlBuilder = HttpUrl.parse(LINGVO_DIC_ONLINE+"/v1/Minicard").newBuilder();
		urlBuilder.addQueryParameter("text", s);
		urlBuilder.addQueryParameter("srcLang", String.valueOf(ilangf));
		urlBuilder.addQueryParameter("dstLang", String.valueOf(ilang));
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.header("Authorization","Bearer "+sLingvoToken)
				.url(url)
				.build();
		Call call = client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					try {
						JSONObject jso = new JSONObject(sBody);
						if (jso.has("Translation")) {
							JSONObject jsoT = jso.getJSONObject("Translation");
							if (jsoT.has("Translation")) {
								String sHeading = jsoT.getString("Heading");
								String sTrans = jsoT.getString("Translation");
								String sDic = "";
								if (jsoT.has("DictionaryName"))
									sDic = jsoT.getString("DictionaryName");
								if (!StrUtils.isEmptyStr(sTrans)) {
									if (!StrUtils.isEmptyStr(sHeading))
										sTrans = sHeading.replace(":", " ") + ": " + sTrans;
									if (!extended) {
										if (dcb == null) {
											cr.showDicToast(s, sTrans, Toast.LENGTH_LONG, view, DicToastView.IS_LINGVO, sDic);
											saveToDicSearchHistory(s, sTrans, curDict);
										} else {
											dcb.done(sTrans);
											if (dcb.showDicToast()) {
												cr.showDicToast(s, sTrans, Toast.LENGTH_LONG, view, DicToastView.IS_LINGVO, sDic);
											}
											if (dcb.saveToHist()) {
												saveToDicSearchHistory(s, sTrans, curDict);
											}
 										}
									}
									else lingvoExtended(s, ilangfF, ilangF, sTrans, false, sDic, curDict, view, dcb);
								}
							} else {
								if (dcb != null) dcb.fail(null, sBody);
							}
						}
					} catch (Exception e) {
						boolean bShowToast = dcb == null;
						if (!bShowToast) bShowToast = dcb.showDicToast();
						if (bShowToast) {
							cr.showDicToast(s, sBody, DicToastView.IS_LINGVO, "");
							if ((sBody.contains("for direction")) && (sBody.contains("not found")))
								BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
									if (cr.getReaderView().mBookInfo != null) {
										FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
										FileInfo dfi = fi.parent;
										if (dfi == null) {
											dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
										}
										if (dfi != null) {
											cr.editBookTransl(dfi, fi, langf, lang, s, null, TranslationDirectionDialog.FOR_LINGVO);
										}
									}
									;
								}, 1000));
						} else dcb.fail(e, e.getMessage());
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				sLingvoToken = "";
				if (unauthCnt == 0) {
					unauthCnt++;
					lingvoAuthThenTranslate(s, langf, lang, extended, curDict, view, dcb);
				} else {
					if (dcb == null)
						cr.showToast(e.getMessage());
					else {
						dcb.fail(e, e.getMessage());
						if (dcb.showDicToast())
							cr.showToast(e.getMessage());
					}
					unauthCnt = 0;
				}
			}
		});
	};

	private void deeplTranslate(String s, String langf, String lang, DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		CoolReader cr = (CoolReader) mActivity;
		if (!FlavourConstants.PREMIUM_FEATURES) {
			cr.showToast(R.string.only_in_premium);
			return;
		}
		if ((StrUtils.isEmptyStr(langf))||(StrUtils.isEmptyStr(lang))) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showToast(cr.getString(R.string.translate_lang_not_set)+": ["
							+langf+"] -> ["+lang + "]"), 100));
			return;
		}
		if (
			(!(
			langf.equals("DE")||langf.equals("EN")||langf.equals("FR")||langf.equals("IT")||langf.equals("JA")||
					langf.equals("ES")||langf.equals("NL")||langf.equals("PL")||langf.equals("PT")||langf.equals("RU")||
					langf.equals("ZH")
			))
			||
			(!(
					lang.equals("DE")||lang.equals("EN-GB")||lang.equals("EN-US")||lang.equals("EN")||lang.equals("FR")||lang.equals("IT")||
							lang.equals("JA")||lang.equals("ES")||lang.equals("NL")||lang.equals("PL")||
							lang.equals("PT-PT")||lang.equals("PT-BR")||lang.equals("PT")||lang.equals("RU")||
							lang.equals("ZH")
			))
		) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showToast(cr.getString(R.string.translate_lang_not_found)+": ["
							+langf + "] -> ["+lang + "}]"), 100));
			return;
		}
		final CoolReader crf2 = (CoolReader) mActivity;
		HttpUrl.Builder urlBuilder = HttpUrl.parse(DEEPL_DIC_ONLINE+"/v2/translate").newBuilder();
		urlBuilder.addQueryParameter("auth_key", crf2.deeplCloudSettings.deeplToken);
		urlBuilder.addQueryParameter("text", s);
		urlBuilder.addQueryParameter("target_lang", lang);
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.url(url)
				.build();
		Call call = client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				if ((response.code() == 403) || (response.code() == 456)) {
					String sErr = "";
					if (response.code() == 403) sErr = crf2.getString(R.string.deepl_unauth);
					if (response.code() == 456) sErr = crf2.getString(R.string.deepl_quoata);
					String finalSErr = sErr;
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									crf2.showToast(finalSErr);
								else {
									if (dcb.showDicToast()) crf2.showToast(finalSErr);
									dcb.fail(null, finalSErr);
								}
							}, 100));
					deeplAuthenticated = false;
					return;
				}
				if (response.code() != 200) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									crf2.showToast(crf2.getString(R.string.http_error) + " " + response.code());
								else {
									if (dcb.showDicToast()) crf2.showToast(crf2.getString(R.string.http_error) + " " + response.code());
									dcb.fail(null, crf2.getString(R.string.http_error) + " " + response.code());
								}
							}, 100));
					deeplAuthenticated = false;
					return;
				}
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					try {
						JSONObject jso = new JSONObject(sBody);
						if (jso.has("translations")) {
							String sDic = "";
							JSONArray jsoT = jso.getJSONArray("translations");
							if (jsoT.length() > 0) {
								if (jsoT.getJSONObject(0).has("detected_source_language"))
									sDic = crf2.getString(R.string.translated_from) + " " + jsoT.getJSONObject(0).getString("detected_source_language");
								if (jsoT.getJSONObject(0).has("text")) {
									String sTrans = jsoT.getJSONObject(0).getString("text");
									if (!StrUtils.isEmptyStr(sTrans)) {
										if (dcb == null) {
											cr.showDicToast(s, sTrans, Toast.LENGTH_LONG, view, DicToastView.IS_DEEPL, sDic);
											saveToDicSearchHistory(s, sTrans, curDict);
										} else {
											dcb.done(sTrans);
											if (dcb.showDicToast()) {
												cr.showDicToast(s, sTrans, Toast.LENGTH_LONG, view, DicToastView.IS_DEEPL, sDic);
											}
											if (dcb.saveToHist()) {
												saveToDicSearchHistory(s, sTrans, curDict);
											}
										}
									}
								} else {
									if (dcb != null) dcb.fail(null, sBody);
								}
							}
						}
					} catch (Exception e) {
						boolean bShowToast = dcb == null;
						if (!bShowToast) bShowToast = dcb.showDicToast();
						if (bShowToast) {
							cr.showDicToast(s, sBody, DicToastView.IS_DEEPL, "");
						} else dcb.fail(e, e.getMessage());
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				deeplAuthenticated = false;
				if (unauthCntDeepl == 0) {
					unauthCntDeepl++;
					deeplAuthThenTranslate(s, langf, lang, curDict, view, dcb);
				} else {
					if (dcb == null)
						cr.showToast(e.getMessage());
					else {
						dcb.fail(e, e.getMessage());
						if (dcb.showDicToast())
							cr.showToast(e.getMessage());
					}
					unauthCntDeepl = 0;
				}
			}
		});
	};

	private void lingvoExtended(String s, int ilangf, int ilang,
								String sTrans, boolean isYnd, String sDic, DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(LINGVO_DIC_ONLINE+"/v1/Translation").newBuilder();
		CoolReader cr = (CoolReader) mActivity;
		urlBuilder.addQueryParameter("text", s);
		urlBuilder.addQueryParameter("srcLang", String.valueOf(ilangf));
		urlBuilder.addQueryParameter("dstLang", String.valueOf(ilang));
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.header("Authorization","Bearer "+sLingvoToken)
				.url(url)
				.build();
		Call call = client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					try {
						String sAdd = "";
						JSONArray jsa = new JSONArray(sBody);
						JSONObject jso = null;
						if (jsa.length()>0)
							jso = jsa.getJSONObject(0);
						if (jso.has("Body")) {
							JSONArray jsa2 = jso.getJSONArray("Body");
							if (jsa2.length()>0) {
								jso = jsa2.getJSONObject(0);
								JSONArray jsa3 = jso.getJSONArray("Markup");
								for (int i=0; i<jsa3.length(); i++) {
									jso = jsa3.getJSONObject(i);
									if ((jso.has("Node"))&&(jso.has("Text"))) {
										if (StrUtils.getNonEmptyStr(jso.getString("Node"), true).equals("Transcription")) {
											sAdd = " ["+StrUtils.getNonEmptyStr(jso.getString("Text"), true)+"]";
											if (dcb == null) {
												cr.showDicToast(s, sTrans + sAdd, Toast.LENGTH_LONG, view, DicToastView.IS_LINGVO, sDic);
												saveToDicSearchHistory(s, sTrans + sAdd, curDict);
											} else {
												dcb.done(sTrans + sAdd);
												if (dcb.showDicToast()) {
													cr.showDicToast(s, sTrans + sAdd, Toast.LENGTH_LONG, view, DicToastView.IS_LINGVO, sDic);
												}
												if (dcb.saveToHist()) {
													saveToDicSearchHistory(s, sTrans + sAdd, curDict);
												}
											}
											return;
										}
									}
								}
							}
						}
						if (dcb == null) {
							cr.showDicToast(s, sTrans + sAdd, DicToastView.IS_LINGVO, sDic);
							saveToDicSearchHistory(s, sTrans + sAdd, curDict);
						} else  {
							dcb.done(sTrans + sAdd);
							if (dcb.showDicToast()) {
								cr.showDicToast(s, sTrans + sAdd, DicToastView.IS_LINGVO, sDic);
							}
							if (dcb.saveToHist()) {
								saveToDicSearchHistory(s, sTrans + sAdd, curDict);
							}
						}
					} catch (Exception e) {
						if (dcb == null) {
							cr.showDicToast(s, sTrans, DicToastView.IS_LINGVO, sDic);
							saveToDicSearchHistory(s, sTrans, curDict);
						} else {
							dcb.done(sTrans);
							if (dcb.showDicToast()) {
								cr.showDicToast(s, sTrans, DicToastView.IS_LINGVO, sDic);
							}
							if (dcb.saveToHist()) {
								saveToDicSearchHistory(s, sTrans, curDict);
							}
						}
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				if (dcb == null)
					cr.showDicToast(s, sTrans, DicToastView.IS_LINGVO, sDic);
				else {
					dcb.done(sTrans);
					if (dcb.showDicToast())
						cr.showDicToast(s, sTrans, DicToastView.IS_LINGVO, sDic);
				}
			}
		});
	}

	public String get2dig(String s) {
		if (StrUtils.getNonEmptyStr(s,true).length()>2)
			return StrUtils.getNonEmptyStr(s,true).substring(0,2);
		return s;
	}

	public void saveToDicSearchHistory(String searchText, String translateText, DictInfo curDict) {
		CoolReader cr = null;
		if (mActivity instanceof CoolReader) {
			//asdf
			cr = (CoolReader) mActivity;
			int iDont = cr.settings().getInt(Settings.PROP_APP_DICT_DONT_SAVE_IF_MORE, 0);
			if (iDont>0) {
				if (StrUtils.getNonEmptyStr(searchText,true).split(" ").length > iDont) return;
			}
			DicSearchHistoryEntry dshe = new DicSearchHistoryEntry();
			dshe.setId(0L);
			dshe.setSearch_text(searchText);
			dshe.setText_translate(translateText);
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
			dshe.setDictionary_used(curDict.id);
			dshe.setCreate_time(System.currentTimeMillis());
			dshe.setLast_access_time(System.currentTimeMillis());
			dshe.setLanguage_from(sLangFrom);
			dshe.setLanguage_to(sLangTo);
			dshe.setSeen_count(1L);
			cr.getDB().updateDicSearchHistory(dshe, DicSearchHistoryEntry.ACTION_SAVE, cr);
		}
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

	@SuppressLint("NewApi")
	public void findInDictionary(String s, View view, CoolReader.DictionaryCallback dcb) throws DictionaryException {
		log.d("lookup in dictionary: " + s);
		log.i("currentDictionary: "+getCDinfo(currentDictionary)+", iDic2IsActive = "+
				iDic2IsActive+", currentDictionary2" + getCDinfo(currentDictionary2) +
				", currentDictionaryTmp = " + getCDinfo(currentDictionaryTmp) +
				", saveCurrentDictionary = " + getCDinfo(saveCurrentDictionary) +
				", saveCurrentDictionary2 = " + getCDinfo(saveCurrentDictionary2) +
				", saveCurrentDictionaryTmp = " + getCDinfo(saveCurrentDictionaryTmp) +
				", saveIDic2IsActive = " + saveIDic2IsActive);
		// save - if we ask for transl direction
		saveCurrentDictionary = currentDictionary;
		saveCurrentDictionary2 = currentDictionary2;
		saveCurrentDictionaryTmp = currentDictionaryTmp;
		saveIDic2IsActive = iDic2IsActive;
		//
		DictInfo curDict = getCurDict();
		currentDictionaryTmp = null;
		log.i("Chosen dic = "+getCDinfo(curDict));
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
		if (diRecent.size()>5)
			while (diRecent.size()>5) diRecent.remove(5);
		boolean isDouble = false;
		//save to dic search history
		CoolReader cr = null;
		if (!((curDict.internal == 7) || (curDict.internal == 8) || (curDict.internal == 9)))
			saveToDicSearchHistory(s, "", curDict);
		if (mActivity instanceof CoolReader) {
			cr = (CoolReader) mActivity;
			isDouble = (cr.getReaderView().getSettings().getInt(Settings.PROP_LANDSCAPE_PAGES,1)==2) &&
					(cr.getReaderView().getSettings().getInt(Settings.PROP_LANDSCAPE_PAGES,1)==2);
		}

		final String SEARCH_ACTION  = "colordict.intent.action.SEARCH";
		final String EXTRA_QUERY   = "EXTRA_QUERY";
		final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";

		switch (curDict.internal) {
		case 0:
			Intent intent0 = new Intent(curDict.action);
			if (curDict.className != null || DeviceInfo.getSDKLevel() == 3) {
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
			Intent intent3 = new Intent("aard2.lookup");
			intent3.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent3.putExtra(SearchManager.QUERY, s);
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
			intent4.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
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
						BookInfo book = cr.getReaderView().getBookInfo();
						String lang = StrUtils.getNonEmptyStr(book.getFileInfo().lang_to,true);
						String langf = StrUtils.getNonEmptyStr(book.getFileInfo().lang_from, true);
						if (StrUtils.isEmptyStr(langf)) {
							String sLang = StrUtils.getNonEmptyStr(book.getFileInfo().language,true);
							if (sLang.toUpperCase().contains("РУССК")) sLang = "ru";
								else if (sLang.toUpperCase().startsWith("EN")) sLang = "en";
									else sLang = "";
							langf = sLang;
						}
						// ask book translation direction
						if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
							if (cr.getReaderView().mBookInfo!=null) {
								FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
								FileInfo dfi = fi.parent;
								if (dfi == null) {
									dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
								}
								if (dfi != null) {
									currentDictionary = saveCurrentDictionary;
									currentDictionary2 = saveCurrentDictionary2;
									currentDictionaryTmp = saveCurrentDictionaryTmp;
									iDic2IsActive = saveIDic2IsActive;
									cr.editBookTransl(dfi, fi, langf, lang, s, null, TranslationDirectionDialog.FOR_LINGVO);
								}
							};
							return;
						}
						//if (lang.equals("")) lang = "en";
						intent5.putExtra(MinicardContract.EXTRA_LANGUAGE_TO, lang);
						intent5.putExtra(MinicardContract.EXTRA_LANGUAGE_FROM, langf);
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
					intent5.putExtra(MinicardContract.EXTRA_WIDTH, frameMetrics.widthPixels / 2);
					intent5.putExtra(MinicardContract.EXTRA_GRAVITY, Gravity.BOTTOM | Gravity.RIGHT);
				} else if (iPage  == 2) {
					intent5.putExtra(MinicardContract.EXTRA_WIDTH, frameMetrics.widthPixels / 2);
					intent5.putExtra(MinicardContract.EXTRA_GRAVITY, Gravity.BOTTOM | Gravity.LEFT);
				} else {
					intent5.putExtra(MinicardContract.EXTRA_GRAVITY, frameMetrics.Gravity);
					if (
							((selectionBottom>frameMetrics.HeightMore)&&(selectionTop>frameMetrics.HeightMore)) ||
							((selectionBottom<(frameMetrics.heightPixels-frameMetrics.HeightMore))&&
									(selectionTop<(frameMetrics.heightPixels-frameMetrics.HeightMore)))
					   ) intent5.putExtra(MinicardContract.EXTRA_HEIGHT, frameMetrics.HeightMore); else
					intent5.putExtra(MinicardContract.EXTRA_HEIGHT, frameMetrics.Height);
				}
			} else {
				intent5.putExtra(MinicardContract.EXTRA_GRAVITY, frameMetrics.Gravity);
				intent5.putExtra(MinicardContract.EXTRA_HEIGHT, frameMetrics.Height);
			}

			intent5.putExtra(MinicardContract.EXTRA_FORCE_LEMMATIZATION, true);
			intent5.putExtra(MinicardContract.EXTRA_TRANSLATE_VARIANTS, true);
			intent5.putExtra(MinicardContract.EXTRA_ENABLE_SUGGESTIONS, true);
			//intent5.putExtra(MinicardContract.EXTRA_LIGHT_THEME, true);
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
			//final String targetLanguage = DictionaryUtil.TargetLanguageOption.getValue();
			//if (!Language.ANY_CODE.equals(targetLanguage)) {
			//	intent.putExtra(MinicardContract.EXTRA_LANGUAGE_TO, targetLanguage);
			//}
			//InternalUtil.startDictionaryActivity(fbreader, intent, this);
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
			BookInfo book = cr.getReaderView().getBookInfo();
			String lang = StrUtils.getNonEmptyStr(book.getFileInfo().lang_to,true);
			String langf = StrUtils.getNonEmptyStr(book.getFileInfo().lang_from, true);
			if (StrUtils.isEmptyStr(langf)) langf = book.getFileInfo().language;
			if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
				if (cr.getReaderView().mBookInfo!=null) {
					FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
					FileInfo dfi = fi.parent;
					if (dfi == null) {
						dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
					}
					if (dfi != null) {
						currentDictionary = saveCurrentDictionary;
						currentDictionary2 = saveCurrentDictionary2;
						currentDictionaryTmp = saveCurrentDictionaryTmp;
						iDic2IsActive = saveIDic2IsActive;
						cr.editBookTransl(dfi, fi, langf, lang, s, null, TranslationDirectionDialog.FOR_YND);
					}
				};
				return;
			}
			if (StrUtils.isEmptyStr(sYandexIAM))
				yandexAuthThenTranslate(s, langf, lang, curDict, view, null, dcb);
			else
				yandexTranslate(s, yndGetDefLangCode(langf), yndGetDefLangCode(lang), curDict, view, null, dcb);
			// old yandex translate
//			HttpUrl.Builder urlBuilder = HttpUrl.parse(YND_DIC_ONLINE).newBuilder();
//			urlBuilder.addQueryParameter("key", BuildConfig.YND_TRANSLATE);
//			urlBuilder.addQueryParameter("text", s);
//			String llang = get2dig(lang);
//			if (!StrUtils.isEmptyStr(langf)) llang = get2dig(langf)+"-"+get2dig(lang);
//			if (llang.startsWith("#-")) llang = llang.substring(2);
//			if (llang.startsWith("##-")) llang = llang.substring(3);
//			urlBuilder.addQueryParameter("lang", llang);
//			urlBuilder.addQueryParameter("format", "plain");
//			String url = urlBuilder.build().toString();
//			Request request = new Request.Builder()
//					.url(url)
//					.build();
//			Call call = client.newCall(request);
//			final CoolReader crf = cr;
//			final DictInfo curDictF = curDict;
//			call.enqueue(new okhttp3.Callback() {
//				public void onResponse(Call call, Response response)
//						throws IOException {
//					String sBody = response.body().string();
//					Document docJsoup = Jsoup.parse(sBody, YND_DIC_ONLINE);
//					Elements results = docJsoup.select("Translation > text");
//					String sTransl = "";
//					if (results.size()>0) sTransl = results.text(); else {
//						if ((StrUtils.getNonEmptyStr(sBody,true).contains("40"))
//							&&
//						(StrUtils.getNonEmptyStr(sBody,true).contains("exceeded")))
//						sTransl = mActivity.getString(R.string.online_dic_exceeded);
//						else sTransl = sBody;
//					}
//					final String sTranslF = sTransl;
//					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
//						crf.showDicToast(s, sTranslF, Toast.LENGTH_LONG, view, DicToastView.IS_YANDEX, "");
//						saveToDicSearchHistory(s, sTranslF, curDictF);
//					}, 100));
//				}
//
//				public void onFailure(Call call, IOException e) {
//					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
//							crf.showToast(e.getMessage()), 100));
//				}
//			});
			break;
		case 8:
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showToast(R.string.only_in_premium);
				return;
			}
			checkLangCodes();
			book = cr.getReaderView().getBookInfo();
			lang = StrUtils.getNonEmptyStr(book.getFileInfo().lang_to,true);
			langf = StrUtils.getNonEmptyStr(book.getFileInfo().lang_from, true);
			if (StrUtils.isEmptyStr(langf)) langf = book.getFileInfo().language;
			if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
				if (cr.getReaderView().mBookInfo!=null) {
					FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
					FileInfo dfi = fi.parent;
					if (dfi == null) {
						dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
					}
					if (dfi != null) {
						currentDictionary = saveCurrentDictionary;
						currentDictionary2 = saveCurrentDictionary2;
						currentDictionaryTmp = saveCurrentDictionaryTmp;
						iDic2IsActive = saveIDic2IsActive;
						cr.editBookTransl(dfi, fi, langf, lang, s, null, TranslationDirectionDialog.FOR_LINGVO);
					}
				};
				return;
			}
			if (sLingvoToken.equals("")) lingvoAuthThenTranslate(s, langf, lang, curDict.id.contains("Extended"), curDict, view, dcb);
				else lingvoTranslate(s, lingvoGetDefLangCode(langf), lingvoGetDefLangCode(lang), curDict.id.contains("Extended"), curDict, view, dcb);
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
					sLink = cr.getReaderView().getSettings().getProperty(Settings.PROP_CLOUD_WIKI1_ADDR, "https://en.wikipedia.org");
					sLink2 = cr.getReaderView().getSettings().getProperty(Settings.PROP_CLOUD_WIKI2_ADDR, "https://en.wikipedia.org");
				}
				else {
					sLink = cr.getReaderView().getSettings().getProperty(Settings.PROP_CLOUD_WIKI2_ADDR, "https://en.wikipedia.org");
					sLink2 = cr.getReaderView().getSettings().getProperty(Settings.PROP_CLOUD_WIKI1_ADDR, "https://en.wikipedia.org");
				}
			}
			wikiTranslate(cr, curDict, view, s, sLink, sLink2, WIKI_FIND_TITLE, true, dcb);
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
				//intent7.setComponent(new ComponentName(curDict.packageName, curDict.className));
				break;
		case 11:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					cr.showToast(R.string.only_in_premium);
					return;
				}
				checkLangCodes();
				book = cr.getReaderView().getBookInfo();
				lang = StrUtils.getNonEmptyStr(book.getFileInfo().lang_to,true);
				langf = StrUtils.getNonEmptyStr(book.getFileInfo().lang_from, true);
				if (StrUtils.isEmptyStr(langf)) langf = book.getFileInfo().language;
				if (StrUtils.isEmptyStr(lang)||StrUtils.isEmptyStr(langf)) {
					if (cr.getReaderView().mBookInfo!=null) {
						FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
						FileInfo dfi = fi.parent;
						if (dfi == null) {
							dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
						}
						if (dfi != null) {
							currentDictionary = saveCurrentDictionary;
							currentDictionary2 = saveCurrentDictionary2;
							currentDictionaryTmp = saveCurrentDictionaryTmp;
							iDic2IsActive = saveIDic2IsActive;
							cr.editBookTransl(dfi, fi, langf, lang, s, null, TranslationDirectionDialog.FOR_COMMON);
						}
					};
					return;
				}
				if (!deeplAuthenticated) deeplAuthThenTranslate(s, langf, lang, curDict, view, dcb);
				else deeplTranslate(s, lingvoGetDefLangCode(langf), lingvoGetDefLangCode(lang), curDict, view, dcb);
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
