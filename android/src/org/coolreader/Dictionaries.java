package org.coolreader;

import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DicSearchHistoryEntry;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.UserDicEntry;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.view.Gravity;

import com.abbyy.mobile.lingvo.api.MinicardContract;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class Dictionaries {

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
		this.iDic2IsActive = iDic2IsActive;
	}

	public void setAdHocDict(DictInfo dict) {
		this.currentDictionary3 = dict;
	}

	private Integer iDic2IsActive = 0;

	public Dictionaries(Activity activity) {
		mActivity = activity;
		currentDictionary = defaultDictionary();
		currentDictionary2 = defaultDictionary();
	}
	
	public DictInfo currentDictionary;
	public DictInfo currentDictionary2;
	public DictInfo currentDictionary3;

	public static class DictInfo {
		public final String id; 
		public final String name;
		public final String packageName;
		public final String className;
		public final String action;
		public final Integer internal;
		public final int dicIcon;
		public String dataKey = SearchManager.QUERY;

		public boolean isInstalled() {
			return isInstalled;
		}

		public void setInstalled(boolean installed) {
			isInstalled = installed;
		}

		public boolean isInstalled = false;
		public DictInfo ( String id, String name, String packageName, String className, String action, Integer internal,
						  int dicIcon) {
			this.id = id;
			this.name = name;
			this.packageName = packageName;
			this.className = className;
			this.action = action;
			this.internal = internal;
			this.dicIcon = dicIcon;
		}
		public DictInfo setDataKey(String key) { this.dataKey = key; return this; }
	}

	static final DictInfo dicts[] = {
		new DictInfo("Fora", "Fora Dictionary", "com.ngc.fora", "com.ngc.fora.ForaDictionary",
				Intent.ACTION_SEARCH, 0, R.drawable.fora),
		new DictInfo("ColorDict", "ColorDict", "com.socialnmobile.colordict", "com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 0, R.drawable.colordict),
		new DictInfo("ColorDictApi", "ColorDict new / GoldenDict", "com.socialnmobile.colordict", "com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 1, R.drawable.goldendict),
		new DictInfo("ColorDictApi (minicard)", "ColorDict new / GoldenDict (minicard)", "com.socialnmobile.colordict", "com.socialnmobile.colordict.activity.Main",
				Intent.ACTION_SEARCH, 6, R.drawable.goldendict),
		new DictInfo("AardDict", "Aard Dictionary", "aarddict.android", "aarddict.android.Article",
				Intent.ACTION_SEARCH, 0, R.drawable.aarddict),
		new DictInfo("AardDictLookup", "Aard Dictionary Lookup", "aarddict.android", "aarddict.android.Lookup",
				Intent.ACTION_SEARCH, 0, R.drawable.aarddict),
		new DictInfo("Aard2", "Aard 2 Dictionary", "itkach.aard2", "aard2.lookup",
				Intent.ACTION_SEARCH, 3, R.drawable.aard2),
		new DictInfo("Dictan", "Dictan Dictionary", "info.softex.dictan", null,
				Intent.ACTION_VIEW, 2, R.drawable.dictan),
		new DictInfo("FreeDictionary.org", "Free Dictionary . org", "org.freedictionary", "org.freedictionary.MainActivity",
				"android.intent.action.VIEW", 0, R.drawable.freedictionary),
		new DictInfo("ABBYYLingvo", "ABBYY Lingvo", "com.abbyy.mobile.lingvo.market", null /*com.abbyy.mobile.lingvo.market.MainActivity*/,
				"com.abbyy.mobile.lingvo.intent.action.TRANSLATE", 0, R.drawable.lingvo).setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		new DictInfo("ABBYYLingvo (minicard)", "ABBYY Lingvo (minicard)", "com.abbyy.mobile.lingvo.market", null,
				"com.abbyy.mobile.lingvo.intent.action.TRANSLATE", 5, R.drawable.lingvo).setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		//new DictInfo("ABBYYLingvoLive", "ABBYY Lingvo Live", "com.abbyy.mobile.lingvolive", null, "com.abbyy.mobile.lingvo.intent.action.TRANSLATE", 0).setDataKey("com.abbyy.mobile.lingvo.intent.extra.TEXT"),
		new DictInfo("LingoQuizLite", "Lingo Quiz Lite", "mnm.lite.lingoquiz", "mnm.lite.lingoquiz.ExchangeActivity",
				"lingoquiz.intent.action.ADD_WORD", 0, R.drawable.lingo_quiz).setDataKey("EXTRA_WORD"),
		new DictInfo("LingoQuiz", "Lingo Quiz", "mnm.lingoquiz", "mnm.lingoquiz.ExchangeActivity",
				"lingoquiz.intent.action.ADD_WORD", 0, R.drawable.lingo_quiz).setDataKey("EXTRA_WORD"),
		new DictInfo("LEODictionary", "LEO Dictionary", "org.leo.android.dict", "org.leo.android.dict.LeoDict",
				"android.intent.action.SEARCH", 0, R.drawable.leo).setDataKey("query"),
		new DictInfo("PopupDictionary", "Popup Dictionary", "com.barisatamer.popupdictionary", "com.barisatamer.popupdictionary.MainActivity",
				"android.intent.action.VIEW", 0,R.drawable.popup),
		new DictInfo("GoogleTranslate", "Google Translate", "com.google.android.apps.translate", "com.google.android.apps.translate.TranslateActivity",
				Intent.ACTION_SEND, 4, R.drawable.googledic),
		new DictInfo("YandexTranslate", "Yandex Translate", "ru.yandex.translate", "ru.yandex.translate.ui.activities.MainActivity",
				Intent.ACTION_SEND, 4, R.drawable.ytr_ic_launcher),
		new DictInfo("Wikipedia", "Wikipedia", "org.wikipedia", "org.wikipedia.main.MainActivity",
				Intent.ACTION_SEND, 4, R.drawable.wiki),
	};

	public static final String DEFAULT_DICTIONARY_ID = "com.ngc.fora";
	
	static DictInfo findById(String id) {
		for(DictInfo d: dicts) {
			if (d.id.equals(id))
				return d;
		}
		return null;
	}
	
	static DictInfo defaultDictionary() {
		return findById(DEFAULT_DICTIONARY_ID);
	}
		
	
	public static DictInfo[] getDictList() {
		return dicts;
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
			}
		}
		for (DictInfo dict : dicts) {
			if ((dict.isInstalled()) || (!bOnlyInstalled)) dlist.add(dict);
		}
		return dlist;
	}

	public void setDict( String id ) {
		DictInfo d = findById(id);
		if (d != null)
			currentDictionary = d;
	}

	public void setDict2( String id ) {
		DictInfo d = findById(id);
		if (d != null)
			currentDictionary2 = d;
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
	
	@SuppressLint("NewApi")
	public void findInDictionary(String s) throws DictionaryException {
		log.d("lookup in dictionary: " + s);
		DictInfo curDict = currentDictionary;
		if (iDic2IsActive > 0 && currentDictionary2 != null)
			curDict = currentDictionary2;
		if (iDic2IsActive > 1)
			iDic2IsActive = 0;
		if (currentDictionary3 != null)
			curDict = currentDictionary3;
		currentDictionary3 = null;
		if (null == curDict) {
			throw new DictionaryException("Current dictionary are invalid!");
		}
		boolean isDouble = false;
		//save to dic search history
		if (mActivity instanceof CoolReader) {
			DicSearchHistoryEntry dshe = new DicSearchHistoryEntry();
			dshe.setId(0L);
			dshe.setSearch_text(s);
			dshe.setText_translate("");
			String sBookFName = "";
			String sLangFrom = "";
			String sLangTo = "";
			CoolReader cr = (CoolReader) mActivity;
			if (cr.getReaderView()!=null) {
				if (cr.getReaderView().getBookInfo()!=null)
					if (cr.getReaderView().getBookInfo().getFileInfo()!=null)
					{
						sBookFName = cr.getReaderView().getBookInfo().getFileInfo().filename;
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
			cr.getDB().updateDicSearchHistory(dshe, DicSearchHistoryEntry.ACTION_SAVE);
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
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
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
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
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
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
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
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
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
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
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
				CoolReader cr = (CoolReader) mActivity;
				if (cr.getReaderView()!=null) {
					if (cr.getReaderView().lastSelection != null) {
						selectionTop = cr.getReaderView().lastSelection.startY;
						selectionBottom = cr.getReaderView().lastSelection.endY;
						selectionX1 = cr.getReaderView().lastSelection.startX;
						selectionX2 = cr.getReaderView().lastSelection.endX;
					}
					if (cr.getReaderView().getBookInfo()!=null) {
						BookInfo book = cr.getReaderView().getBookInfo();
						String lang = book.getFileInfo().lang_to;
						if (lang==null) lang = "";
						if (lang.equals("")) lang = "ru";
						String langf = book.getFileInfo().lang_from;
						if (langf==null) langf = "";
						if (langf.equals("")) langf = book.getFileInfo().language;
						if (langf==null) langf = "";
						if (lang.equals("")) lang = "en";
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
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
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
				CoolReader cr = (CoolReader) mActivity;
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
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
			}
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
