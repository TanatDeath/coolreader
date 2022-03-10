package org.coolreader.options;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.coolreader.CoolReader;
import org.coolreader.cloud.CloudFileInfo;
import org.coolreader.crengine.BackgroundTextureInfo;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.InitAppDialog;
import org.coolreader.crengine.L;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Properties;
import org.coolreader.readerview.ReaderView;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.crengine.SwitchProfileDialog;
import org.coolreader.utils.Utils;
import org.coolreader.dic.Dictionaries;
import org.coolreader.dic.Dictionaries.DictInfo;
import org.coolreader.R;
import org.coolreader.cloud.CloudSync;
import org.coolreader.tts.OnTTSCreatedListener;
import org.coolreader.tts.TTSControlBinder;
import org.coolreader.tts.TTSControlServiceAccessor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

public class OptionsDialog extends BaseDialog implements TabContentFactory, OptionOwner, Settings {

	public static int toastShowCnt = 0;

	ReaderView mReaderView;
	BaseActivity mActivity;
	String optionFilter;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;
	public String selectedOption;
	public int selectedTab = -1;

	TTSControlServiceAccessor mTTSControl;
	TTSControlBinder mTTSBinder;
	ClickOption optBT;

	public static int[] mFontSizes = new int[] {
			9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60,
			61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
			91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
			121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150,
			170, 200, 230, 260, 300, 340
	};

	int[] mStatusFontSizes = new int[] {
			9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60,
			61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
			91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
			121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150,
			170, 200, 230, 260, 300, 340
		};
		
	int[] filterFontSizes(int[] fontSizes) {
	    ArrayList<Integer> list = new ArrayList<>();
		for (int sz : fontSizes) {
	        if (sz >= mActivity.getMinFontSize() && sz <= mActivity.getMaxFontSize())
    	        list.add(sz);
	    }
	    int[] res = new int[list.size()];
	    for (int i = 0; i < list.size(); i++)
	        res[i] = list.get(i);
	    return res;
	}

	public static int getFontSizeShift(int curSize, int shift) {
		if (curSize + shift < 0) return 20;
		if (curSize + shift >= 150) return 150;
		return curSize + shift;
	}

	public static int getFontSizeShiftOld(int curSize, int shift) {
		for (int i = 0; i < mFontSizes.length; i++)
			if (mFontSizes[i] == curSize) {
				if (i + shift < 0) return mFontSizes[0];
				if (i + shift >= mFontSizes.length) return mFontSizes[mFontSizes.length-1];
				return mFontSizes[i + shift];
			}
		return curSize;
	}

	int[] mSynthWeights;
	public static int findBacklightSettingIndex(int value) {
		int bestIndex = 0;
		int bestDiff = -1;
		for ( int i=0; i<BacklightOption.mBacklightLevels.length; i++ ) {
			int diff = BacklightOption.mBacklightLevels[i] - value;
			if (diff<0)
				diff = -diff;
			if (bestDiff == -1 || diff < bestDiff) {
				bestDiff = diff;
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	public static int[] mMotionTimeouts;
	public static String[] mMotionTimeoutsTitles;
	public static int[] mMotionTimeoutsAddInfos;
	public static int[] mMotionTimeoutsSec;
	public static String[] mMotionTimeoutsTitlesSec;
	public static int[] mMotionTimeoutsAddInfosSec;
	public static int[] mMotionTimeouts1;
    public static String[] mMotionTimeoutsTitles1;
    public static int[] mMotionTimeoutsAddInfos1;
	public static int[] mPagesPerFullSwipe;
	public static int[] mPagesPerFullSwipeAddInfos;
	public static String[] mPagesPerFullSwipeTitles;
	public static int[] mForceTTS;
	public static String[] mForceTTSTitles;
	public static int[] mForceTTSAddInfos;

	int[] mTapSecondaryActionType = new int[] {
			Settings.TAP_ACTION_TYPE_LONGPRESS, Settings.TAP_ACTION_TYPE_DOUBLE
	};

	int[] mTapSecondaryActionTypeTitles = new int[] {
			R.string.options_controls_tap_type_long, R.string.options_controls_tap_type_double
		};
	int[] mTapSecondaryActionTypeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mHighlightMode = new int[] {
			0, 1, 2
		};
	int[] mHighlightModeTitles = new int[] {
			R.string.options_view_bookmarks_highlight_none, R.string.options_view_bookmarks_highlight_solid,
			R.string.options_view_bookmarks_highlight_underline
		};
	int[] mHighlightModeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	public static int getBookmarkSendToTitle(int v) {
		for (int i = 0; i < BookmarkSendToOption.mBookmarkSendToAction.length; i++)
			if (v == BookmarkSendToOption.mBookmarkSendToAction[i]) return BookmarkSendToOption.mBookmarkSendToAction[i];
		return 0;
	}

	// possible values see in crengine/include/lvfont.h: enum font_antialiasing_t
	int[] mAntialias = new int[] {
			0, 1, 2
		};
	int[] mAntialiasTitles = new int[] {
			R.string.options_font_antialias_off, R.string.options_font_antialias_on_for_big, R.string.options_font_antialias_on_for_all
		};
	int[] mAntialiasAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int [] mGoogleDriveAutoSavePeriod = new int[] {
			0, 1, 2, 3, 4, 5, 10, 15, 20, 30
	};
	int [] mGoogleDriveAutoSavePeriodTitles = new int[] {
			R.string.autosave_period_off,
			R.string.autosave_period_1min,
			R.string.autosave_period_2min,
			R.string.autosave_period_3min,
			R.string.autosave_period_4min,
			R.string.autosave_period_5min,
			R.string.autosave_period_10min,
			R.string.autosave_period_15min,
			R.string.autosave_period_20min,
			R.string.autosave_period_30min
	};

	int[] mDoubleClickIntervals = new int[] {
			100, 200, 300, 400, 500, 600, 800, 1000
	};

	int[] mPreventClickIntervals = new int[] {
			0, 50, 100, 200, 300, 400, 500, 600, 800, 1000
	};

	int [] mCloudBookmarksKeepAlive = new int [] {
			0, 1, 2, 3, 4, 5, 6,
			7, 14, 30, 91, 182, 365
	};
	int [] mCloudBookmarksKeepAliveTitles = new int [] {
			R.string.bookmarks_keepalive_off,
			R.string.bookmarks_keepalive_1day,
			R.string.bookmarks_keepalive_2days,
			R.string.bookmarks_keepalive_3days,
			R.string.bookmarks_keepalive_4days,
			R.string.bookmarks_keepalive_5days,
			R.string.bookmarks_keepalive_6days,
			R.string.bookmarks_keepalive_1week,
			R.string.bookmarks_keepalive_2weeks,
			R.string.bookmarks_keepalive_1month,
			R.string.bookmarks_keepalive_1quarter,
			R.string.bookmarks_keepalive_half_a_year,
			R.string.bookmarks_keepalive_1year
	};

	int [] mCloudBookmarksKeepAliveAddInfos = new int [] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mUIFontScale = new int[] {
			3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
	};

	String[] mUIFontScaleTitles = new String[] {
			"x0.3",
			"x0.4",
			"x0.5",
			"x0.6",
			"x0.7",
			"x0.8",
			"x0.9",
			"x1",
			"x1.1",
			"x1.2",
			"x1.3",
			"x1.4",
			"x1.5",
			"x1.6",
			"x1.7",
			"x1.8",
			"x1.9",
			"x2.0",
			"x2.1",
			"x2.2",
			"x2.3",
			"x2.4",
			"x2.5"
	};

	int[] mUIFontScaleAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	ViewGroup mContentView;
	TabHost mTabs;
	LayoutInflater mInflater;
	public Properties mProperties;
	ArrayList<String> mFilteredProps;
	Properties mOldProperties;
	OptionsListView mOptionsStyles;
	OptionsListView mOptionsCSS;
	OptionsListView mOptionsPage;
	OptionsListView mOptionsApplication;
	OptionsListView mOptionsControls;
	OptionsListView mOptionsBrowser;
	OptionsListView mOptionsCloudSync = null;
	OptionsListView mOptionsTTS;

	// Disable options
	OptionBase mHyphDictOption;
	OptionBase mEmbedFontsOptions;
	OptionBase mIgnoreDocMargins;
	OptionBase mAppScreenBacklightEink;
	OptionBase mFootNotesOption;
	OptionBase mEnableMultiLangOption;
	OptionBase mEnableHyphOption;
	OptionBase mGoogleDriveEnableSettingsOption;
	OptionBase mGoogleDriveEnableBookmarksOption;
	OptionBase mGoogleDriveEnableCurrentBookInfoOption;
	OptionBase mGoogleDriveEnableCurrentBookBodyOption;
	OptionBase mCloudSyncAskConfirmationsOption;
	OptionBase mGoogleDriveAutoSavePeriodOption;
	OptionBase mCloudSyncDataKeepAliveOptions;
	ListOption mTTSEngineOption;
	OptionBase mTTSUseDocLangOption;
	ListOption mTTSLanguageOption;
	ListOption mTTSVoiceOption;
	ListOption mFontWeightOption;
	OptionBase mFontHintingOption;
	ListOption mBounceProtectionOption; //KR: We have our own implementation, but name is the same

	//public final static int OPTION_VIEW_TYPE_COUNT = 4;

	// This is an engine limitation, see lvfreetypefontman.cpp, lvfreetypeface.cpp
	public static final int MAX_FALLBACK_FONTS_COUNT = 32;

	public BaseActivity getActivity() { return mActivity; }
	public Properties getProperties() { return mProperties; }
	public LayoutInflater getInflater() { return mInflater; }

	OptionBase mTitleBarFontColor1 = null;
	OptionBase mTitleBarFontColor2 = null;

	public static boolean showIcons = true;
	public static boolean isTextFormat = false;
	public static boolean isEpubFormat = false;
	public static boolean isFormatWithEmbeddedStyle = false;
	public static boolean isHtmlFormat = false;
	public Mode mode;

	public interface ClickCallback {
		void click(View view, String optionLabel, String optionValue);
	}

	static public void saveColor( Properties mProperties, boolean night )
	{
		if (night) {
			mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE_NIGHT, mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE, "(NONE)"));
			mProperties.setColor(PROP_BACKGROUND_COLOR_NIGHT, mProperties.getColor(PROP_BACKGROUND_COLOR, 0x000000));
			mProperties.setColor(PROP_FONT_COLOR_NIGHT, mProperties.getColor(PROP_FONT_COLOR, 0xFFFFFF));
			mProperties.setColor(PROP_STATUS_FONT_COLOR_NIGHT, mProperties.getColor(PROP_STATUS_FONT_COLOR, 0xFFFFFF));
			mProperties.setInt(PROP_APP_SCREEN_BACKLIGHT_NIGHT, mProperties.getInt(PROP_APP_SCREEN_BACKLIGHT, -1));
			mProperties.setProperty(PROP_FONT_GAMMA_NIGHT, mProperties.getProperty(PROP_FONT_GAMMA, "1.0"));
			mProperties.setProperty(PROP_APP_THEME_NIGHT, mProperties.getProperty(PROP_APP_THEME, "BLACK"));
			mProperties.setInt(PROP_APP_HIGHLIGHT_BOOKMARKS_NIGHT, mProperties.getInt(PROP_APP_HIGHLIGHT_BOOKMARKS, 1));
			mProperties.setColor(PROP_HIGHLIGHT_SELECTION_COLOR_NIGHT, mProperties.getColor(PROP_HIGHLIGHT_SELECTION_COLOR, 0xCCCCCC));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_NIGHT, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, 0xFFFF40));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_NIGHT, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, 0xFF8000));
			mProperties.setColor(PROP_APP_ICONS_CUSTOM_COLOR_NIGHT, mProperties.getColor(PROP_APP_ICONS_CUSTOM_COLOR, Color.GRAY));
		} else {
			mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE_DAY, mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE, "(NONE)"));
			mProperties.setColor(PROP_BACKGROUND_COLOR_DAY, mProperties.getColor(PROP_BACKGROUND_COLOR, 0xFFFFFF));
			mProperties.setColor(PROP_FONT_COLOR_DAY, mProperties.getColor(PROP_FONT_COLOR, 0x000000));
			mProperties.setColor(PROP_STATUS_FONT_COLOR_DAY, mProperties.getColor(PROP_STATUS_FONT_COLOR, 0x000000));
			mProperties.setInt(PROP_APP_SCREEN_BACKLIGHT_DAY, mProperties.getInt(PROP_APP_SCREEN_BACKLIGHT, -1));
			mProperties.setProperty(PROP_FONT_GAMMA_DAY, mProperties.getProperty(PROP_FONT_GAMMA, "1.0"));
			mProperties.setProperty(PROP_APP_THEME_DAY, mProperties.getProperty(PROP_APP_THEME, "WHITE"));
			mProperties.setInt(PROP_APP_HIGHLIGHT_BOOKMARKS_DAY, mProperties.getInt(PROP_APP_HIGHLIGHT_BOOKMARKS, 1));
			mProperties.setColor(PROP_HIGHLIGHT_SELECTION_COLOR_DAY, mProperties.getColor(PROP_HIGHLIGHT_SELECTION_COLOR, 0xCCCCCC));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_DAY, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, 0xFFFF40));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_DAY, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, 0xFF8000));
			mProperties.setColor(PROP_APP_ICONS_CUSTOM_COLOR_DAY, mProperties.getColor(PROP_APP_ICONS_CUSTOM_COLOR, Color.GRAY));
		}
		for (String code : styleCodes) {
			String styleName = "styles." + code + ".color";
			String v = mProperties.getProperty(styleName); 
			if (v != null) {
				if (night)
					mProperties.setProperty(styleName + ".night", v);
				else
					mProperties.setProperty(styleName + ".day", v);
			}
		}
	}

	static public void restoreColor( Properties mProperties,  boolean night )
	{
		if (night) {
			mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE, mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE_NIGHT, "(NONE)"));
			mProperties.setColor(PROP_BACKGROUND_COLOR, mProperties.getColor(PROP_BACKGROUND_COLOR_NIGHT, 0x000000));
			mProperties.setColor(PROP_FONT_COLOR, mProperties.getColor(PROP_FONT_COLOR_NIGHT, 0xFFFFFF));
			mProperties.setColor(PROP_STATUS_FONT_COLOR, mProperties.getColor(PROP_STATUS_FONT_COLOR_NIGHT, 0xFFFFFF));
			mProperties.setInt(PROP_APP_SCREEN_BACKLIGHT, mProperties.getInt(PROP_APP_SCREEN_BACKLIGHT_NIGHT, 70));
			mProperties.setProperty(PROP_FONT_GAMMA, mProperties.getProperty(PROP_FONT_GAMMA_NIGHT, "1.0"));
			mProperties.setProperty(PROP_APP_THEME, mProperties.getProperty(PROP_APP_THEME_NIGHT, "BLACK"));
			mProperties.setInt(PROP_APP_HIGHLIGHT_BOOKMARKS, mProperties.getInt(PROP_APP_HIGHLIGHT_BOOKMARKS_NIGHT, 1));
			mProperties.setColor(PROP_HIGHLIGHT_SELECTION_COLOR, mProperties.getColor(PROP_HIGHLIGHT_SELECTION_COLOR_NIGHT, 0xCCCCCC));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_NIGHT, 0xFFFF40));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_NIGHT, 0xFF8000));
			mProperties.setColor(PROP_APP_ICONS_CUSTOM_COLOR, mProperties.getColor(PROP_APP_ICONS_CUSTOM_COLOR_NIGHT, Color.GRAY));
		} else {
			mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE, mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE_DAY, "(NONE)"));
			mProperties.setColor(PROP_BACKGROUND_COLOR, mProperties.getColor(PROP_BACKGROUND_COLOR_DAY, 0xFFFFFF));
			mProperties.setColor(PROP_FONT_COLOR, mProperties.getColor(PROP_FONT_COLOR_DAY, 0x000000));
			mProperties.setColor(PROP_STATUS_FONT_COLOR, mProperties.getColor(PROP_STATUS_FONT_COLOR_DAY, 0x000000));
			mProperties.setInt(PROP_APP_SCREEN_BACKLIGHT, mProperties.getInt(PROP_APP_SCREEN_BACKLIGHT_DAY, 80));
			mProperties.setProperty(PROP_FONT_GAMMA, mProperties.getProperty(PROP_FONT_GAMMA_DAY, "1.0"));
			mProperties.setProperty(PROP_APP_THEME, mProperties.getProperty(PROP_APP_THEME_DAY, "WHITE"));
			mProperties.setInt(PROP_APP_HIGHLIGHT_BOOKMARKS, mProperties.getInt(PROP_APP_HIGHLIGHT_BOOKMARKS_DAY, 1));
			mProperties.setColor(PROP_HIGHLIGHT_SELECTION_COLOR, mProperties.getColor(PROP_HIGHLIGHT_SELECTION_COLOR_DAY, 0xCCCCCC));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_DAY, 0xFFFF40));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_DAY, 0xFF8000));
			mProperties.setColor(PROP_APP_ICONS_CUSTOM_COLOR, mProperties.getColor(PROP_APP_ICONS_CUSTOM_COLOR_DAY, Color.GRAY));
		}
		for (String code : styleCodes) {
			String styleName = "styles." + code + ".color";
			String pname = night ? styleName + ".night" : styleName + ".day";
			String v = mProperties.getProperty(pname);
			if (v != null)
				mProperties.setProperty(styleName, mProperties.getProperty(pname));
		}
	}

	static public void toggleDayNightMode( Properties mProperties ) {
		boolean oldMode = mProperties.getBool(PROP_NIGHT_MODE, false);
		saveColor(mProperties, oldMode);
		boolean newMode = !oldMode;
		restoreColor(mProperties, newMode);
		mProperties.setBool(PROP_NIGHT_MODE, newMode);
	}

	public enum KeyActionFlag {
		KEY_ACTION_FLAG_NORMAL,
		KEY_ACTION_FLAG_LONG,
		KEY_ACTION_FLAG_DOUBLE
	}

	public static class Three {
		public String value;
		public String label;
		public String addInfo;
		public Three(String value, String label, String addInfo) {
			this.value = value;
			this.label = label;
			this.addInfo = addInfo;
		}
	}

	// KR - since we have our own font select dialog we'll disable this - maybe later
	/*
	protected class FontSelectOption extends ListOption {
		protected ArrayList<Pair> sourceList;
		private String langTag;
		private String langDescr;
		private ListOptionAdapter listAdapter;

		public FontSelectOption(OptionOwner owner, String label, String property ) {
			super(owner, label, property);
			langTag = null;
			langDescr = null;
			BookInfo bookInfo = mReaderView.getBookInfo();
			if (null != bookInfo) {
				FileInfo fileInfo = bookInfo.getFileInfo();
				if (null != fileInfo) {
					langTag = fileInfo.language;
					langDescr = Engine.getHumanReadableLocaleName(langTag);
				}
			}
		}

		private void asyncFilterFontsByLanguage(String langTag, FontScanCompleted onComplete) {
			BackgroundThread.ensureGUI();
			final Scanner.ScanControl control = new Scanner.ScanControl();
			final Engine.ProgressControl progress = Services.getEngine().createProgress(R.string.scanning_font_files, control);
			final ArrayList<Pair> filtered = new ArrayList<Pair>();
			BackgroundThread.instance().postBackground(() -> {
				int i = 0;
				for (Pair pair : list) {
					if (control.isStopped())
						break;
					String faceName = pair.value;
					Engine.font_lang_compat status = Engine.checkFontLanguageCompatibility(faceName, langTag);
					switch (status) {
						case font_lang_compat_full:
						case font_lang_compat_partial:
							filtered.add(new Pair(faceName, faceName));
							break;
						default:
							break;
					}
					i++;
					progress.setProgress(10000*i/list.size());
				}
				onComplete.onComplete(filtered, control.isStopped());
				progress.hide();
			});
		}

		public void onSelect() {
			if (!enabled)
				return;
			final BaseDialog dlg = new BaseDialog(mActivity, label, false, false);

			LinearLayout layout = new LinearLayout(mActivity);
			layout.setOrientation(LinearLayout.VERTICAL);

			View panel = mInflater.inflate(R.layout.option_lang_filter, null);
			layout.addView(panel);
			CompoundButton filter_by_lang = panel.findViewById(R.id.filter_by_lang);
			if (null != langDescr && langDescr.length() > 0) {
				filter_by_lang.setText(mActivity.getString(R.string.filter_by_book_language_s, langDescr));
			} else {
				filter_by_lang.setText(mActivity.getString(R.string.filter_by_book_language_s, mActivity.getString(R.string.undetermined)));
				filter_by_lang.setEnabled(false);
			}
			final ListView listView = new BaseListView(mActivity, false);
			listAdapter = new ListOptionAdapter(listView, list);
			int selItem = getSelectedItemIndex();
			if ( selItem<0 )
				selItem = 0;
			listView.setAdapter(listAdapter);
			listView.setSelection(selItem);
			layout.addView(listView);

			listView.setOnItemClickListener((adapter, listview, position, id) -> {
				Pair item = (Pair) listAdapter.getItem(position);
				onClick(item);
				dlg.dismiss();
				closed();
			});

			filter_by_lang.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if (isChecked) {
					asyncFilterFontsByLanguage(langTag, (list, canceled) -> {
						if (!canceled) {
							BackgroundThread.instance().executeGUI(() -> {
								FontSelectOption.this.sourceList = FontSelectOption.this.list;
								FontSelectOption.this.list = list;
								listAdapter = new ListOptionAdapter(listView, list);
								int selindex = getSelectedItemIndex();
								if ( selindex<0 )
									selindex = 0;
								listView.setAdapter(listAdapter);
								listView.setSelection(selindex);
							});
						} else {
							BackgroundThread.instance().executeGUI(() -> {
								filter_by_lang.setChecked(false);
							});
						}
					});
				} else {
					if (null != sourceList) {
						list = sourceList;
						listAdapter = new ListOptionAdapter(listView, list);
						int selindex = getSelectedItemIndex();
						if (selindex < 0)
							selindex = 0;
						listView.setAdapter(listAdapter);
						listView.setSelection(selindex);
					}
				}
			});

			dlg.setOnDismissListener(dialog -> closed());

			// TODO: set checked for for filter_by_lang (save in settings)

			dlg.setView(layout);
			dlg.show();
		}

		protected void closed() {
			if (null != sourceList)
				list = sourceList;
		}
	}
	*/

	public static String updDicValue(String value, Properties mProperties, CoolReader mActivity, boolean isShort) {
		String sfind = mActivity.getString(R.string.options_selection_action_dictionary);
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_1))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_2))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_2, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_3))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_3, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_4))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_4, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_5))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_5, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_6))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_6, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_7))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_7, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_8))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_8, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_9))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_9, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_10))) {
			String val = mProperties.getProperty(PROP_APP_DICTIONARY_10, "");
			if (!StrUtils.isEmptyStr(val)) value = value + ": " + val;
			DictInfo di = Dictionaries.findById(val, mActivity);
			if (di != null) {
				String sAdd = di.getAddText(mActivity);
				if (!StrUtils.isEmptyStr(sAdd)) value = value + " (" + sAdd + ")";
			}
		}
		if (isShort) value = StrUtils.getNonEmptyStr(value,true).replace(sfind+":","").trim();
		if (isShort) value = StrUtils.getNonEmptyStr(value,true).replace(sfind,"").trim();
		return value;
	}

	public static DictInfo getDicValue(String value, Properties mProperties, CoolReader mActivity) {
		String sfind = mActivity.getString(R.string.options_selection_action_dictionary);
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary))) {
			return mActivity.mDictionaries.currentDictionary;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_1))) {
			return mActivity.mDictionaries.currentDictionary;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_2))) {
			return mActivity.mDictionaries.currentDictionary2;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_3))) {
			return mActivity.mDictionaries.currentDictionary3;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_4))) {
			return mActivity.mDictionaries.currentDictionary4;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_5))) {
			return mActivity.mDictionaries.currentDictionary5;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_6))) {
			return mActivity.mDictionaries.currentDictionary6;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_7))) {
			return mActivity.mDictionaries.currentDictionary7;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_8))) {
			return mActivity.mDictionaries.currentDictionary8;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_9))) {
			return mActivity.mDictionaries.currentDictionary9;
		}
		if (StrUtils.getNonEmptyStr(value,false).equals(mActivity.getString(R.string.options_selection_action_dictionary_10))) {
			return mActivity.mDictionaries.currentDictionary10;
		}
		return null;
	}

	class ThumbnailCache {
		final int maxcount;
		final int dx;
		final int dy;
		class Item {
			Drawable drawable;
			Bitmap bmp;
			String path;
			int id;
			public void clear() {
				if (bmp !=null) {
					//bmp.recycle();
					bmp = null;
				}
				if (drawable != null)
					drawable = null;
			}
		}
		ArrayList<Item> list = new ArrayList<>();
		public ThumbnailCache( int dx, int dy, int maxcount ) {
			this.dx = dx;
			this.dy = dy;
			this.maxcount = maxcount;
		}
		private void remove( int maxsize ) {
			while ( list.size()>maxsize ) {
				Item item = list.remove(0);
				item.clear();
			}
		}
		private Drawable createDrawable( String path ) {
			File f = new File(path);
			if (!f.isFile() || !f.exists())
				return null;
			try { 
				BitmapDrawable drawable = (BitmapDrawable)BitmapDrawable.createFromPath(path);
				if (drawable == null)
					return null;
				Bitmap src = drawable.getBitmap();
				Bitmap bmp = Bitmap.createScaledBitmap(src, dx, dy, true);
				//Canvas canvas = new Canvas(bmp);
				BitmapDrawable res = new BitmapDrawable(bmp);
				//src.recycle();
				Item item = new Item();
				item.path = path;
				item.drawable = res; //drawable;
				item.bmp = bmp;
				list.add(item);
				remove(maxcount);
				return drawable;
			} catch ( Exception e ) {
				return null;
			}
		}
		private Drawable createDrawable( int resourceId ) {
			try { 
				//Drawable drawable = mReaderView.getActivity().getResources().getDrawable(resourceId);
				InputStream is = getContext().getResources().openRawResource(resourceId);
				if (is == null)
					return null;
				BitmapDrawable src = new BitmapDrawable(is);
				Item item = new Item();
				item.id = resourceId;
				Bitmap bmp = Bitmap.createScaledBitmap(src.getBitmap(), dx, dy, true);
				BitmapDrawable res = new BitmapDrawable(bmp);
				item.drawable = res;
				item.bmp = bmp;
				list.add(item);
				remove(maxcount);
				return res;
			} catch ( Exception e ) {
				return null;
			}
		}
		public Drawable getImage( String path ) {
			if (path == null || !path.startsWith("/"))
				return null;
			// find existing
			for ( int i=0; i<list.size(); i++ ) {
				if (path.equals(list.get(i).path)) {
					Item item = list.remove(i);
					list.add(item);
					return item.drawable;
				}
			}
			return createDrawable( path ); 
		}
		public Drawable getImage( int resourceId ) {
			if (resourceId == 0)
				return null;
			// find existing
			for ( int i=0; i<list.size(); i++ ) {
				if (list.get(i).id == resourceId) {
					Item item = list.remove(i);
					list.add(item);
					return item.drawable;
				}
			}
			return createDrawable( resourceId ); 
		}
		public void clear() {
			remove(0);
		}
	}
	
	ThumbnailCache textureSampleCache = new ThumbnailCache(64, 64, 100);
	
	class TextureOptions extends ListOption
	{
		public TextureOptions( OptionOwner owner, String label, String addInfo, String filter )
		{
			super( owner, label, PROP_PAGE_BACKGROUND_IMAGE, addInfo, filter );
			setDefaultValue("(NONE)");
			BackgroundTextureInfo[] textures = Services.getEngine().getAvailableTextures();
			for ( BackgroundTextureInfo item : textures )
				add( item.id, item.name, item.id);
		}

		protected void closed() {
			textureSampleCache.clear();
		}

		protected int getItemLayoutId() {
			return R.layout.option_value_image; 
		}

		@Override
		public Three OnPreClick ( Three item ) {
			File f = new File(item.value);
			if (f.exists()) return item;
			File f1 = new File(item.value.replace("/textures/", "/backgrounds/"));
			if (f1.exists()) {
				item.value = item.value.replace("/textures/", "/backgrounds/");
				return item;
			}
			File f2 = new File(item.value.replace("/backgrounds/", "/textures/"));
			if (f2.exists()) {
				item.value = item.value.replace("/backgrounds/", "/textures/");
				return item;
			}
			return item;
		}

		@Override
		public String onSelectDismiss(String propValue) {
			File f = new File(propValue);
			if (f.exists()) return propValue;
			File f1 = new File(propValue.replace("/textures/", "/backgrounds/"));
			if (f1.exists()) {
				propValue = propValue.replace("/textures/", "/backgrounds/");
				return propValue;
			}
			File f2 = new File(propValue.replace("/backgrounds/", "/textures/"));
			if (f2.exists()) {
				propValue = propValue.replace("/backgrounds/", "/textures/");
				return propValue;
			}
			return propValue;
		}

		protected void updateItemContents( final View layout, final Three item, final ListView listView, final int position ) {
			super.updateItemContents(layout, item, listView, position);
			ImageView img = layout.findViewById(R.id.option_value_image);
            ImageView imgT = layout.findViewById(R.id.option_value_type);
			ImageView imgAddInfo = layout.findViewById(R.id.btn_option_add_info);
			ImageView imgDel = layout.findViewById(R.id.option_value_del);
			ImageView cb = layout.findViewById(R.id.option_value_check);
			if (StrUtils.isEmptyStr(item.addInfo)) {
				imgAddInfo.setVisibility(View.INVISIBLE);
				imgDel.setVisibility(View.INVISIBLE);
			}
			else {
				File f = new File(item.addInfo);
				if (!f.exists()) {
					imgAddInfo.setVisibility(View.INVISIBLE);
					imgDel.setVisibility(View.INVISIBLE);
				}
				else {
					imgAddInfo.setVisibility(View.VISIBLE);
					imgDel.setVisibility(View.VISIBLE);
				}
			}
			if (cb.getTag().equals("1")) imgDel.setVisibility(View.INVISIBLE);
			imgDel.setOnClickListener(v -> {
				if (cb.getTag().equals("1")) {
					return;
				}
				String sfile = item.addInfo;
				File f = new File(sfile);
				if (!f.exists()) return;
				mActivity.askConfirmation(R.string.delete_texture, () -> {
					if (f.delete()) {
						mActivity.showToast(R.string.texture_deleted);
						Three forRemove = null;
						for (Three t: list)
							if (t.addInfo.equals(item.addInfo)) forRemove = t;
						if (forRemove != null) list.remove(forRemove);
						forRemove = null;
						for (Three t: listFiltered)
							if (t.addInfo.equals(item.addInfo)) forRemove = t;
						if (forRemove != null) listFiltered.remove(forRemove);
						listUpdated("");
					}
				});
			});
			int cl = mProperties.getColor(PROP_BACKGROUND_COLOR, Color.WHITE);
			final BackgroundTextureInfo texture = Services.getEngine().getTextureInfoById(item.value);
			img.setBackgroundColor(cl);
			if (texture.tiled) imgT.setImageResource(Utils.resolveResourceIdByAttr(OptionsDialog.this.mActivity,
                    R.attr.attr_icons8_texture, R.drawable.icons8_texture));
			else
                imgT.setImageResource(Utils.resolveResourceIdByAttr(OptionsDialog.this.mActivity,
                        R.attr.attr_icons8_fullscreen, R.drawable.icons8_fullscreen));
			OptionsDialog.this.mActivity.tintViewIcons(imgT,true);
			if (imgAddInfo != null) OptionsDialog.this.mActivity.tintViewIcons(imgAddInfo,true);
			if (imgDel != null) OptionsDialog.this.mActivity.tintViewIcons(imgDel,true);
			imgT.setOnClickListener(v -> {
				final BackgroundTextureInfo texture1 = Services.getEngine().getTextureInfoById(item.value);
				if ((texture1.resourceId == 0)&&(!texture1.id.equals(BackgroundTextureInfo.NO_TEXTURE_ID))) {
					OptionsDialog.this.mActivity.askConfirmation(R.string.texture_switch_mode, () -> {
						ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.TexturesDirs, true);
						tDirs = Engine.getDataDirsExt(Engine.DataDirType.BackgroundsDirs, true);
						File f = new File(texture1.id);
						String sNewName = texture1.id;
						if (texture1.id.contains("/textures/"))
							sNewName = sNewName.replace("/textures/", "/backgrounds/");
						else
							sNewName = sNewName.replace("/backgrounds/", "/textures/");
						File fTo = new File(sNewName);
						if (!f.renameTo(fTo))
							OptionsDialog.this.mActivity.showToast(OptionsDialog.this.mActivity.getString(R.string.pic_problem));
						else {
							BackgroundTextureInfo[] textures = Services.getEngine().getAvailableTextures();
							ArrayList<Three> listTemp = new ArrayList<Three>();
							for (Three th : list) {
								if (th.value.equals(texture1.id))
									th.value=sNewName;
								listTemp.add(th);
							}
							list=listTemp;
							listTemp = new ArrayList<Three>();
							for (Three th : listFiltered) {
								if (th.value.equals(texture1.id))
									th.value=sNewName;
								listTemp.add(th);
							}
							listFiltered = listTemp;
							if (!texture1.tiled) ((ImageView)v).setImageResource(Utils.resolveResourceIdByAttr(OptionsDialog.this.mActivity,
									R.attr.attr_icons8_texture, R.drawable.icons8_texture));
							else
								((ImageView)v).setImageResource(Utils.resolveResourceIdByAttr(OptionsDialog.this.mActivity,
										R.attr.attr_icons8_fullscreen, R.drawable.icons8_fullscreen));
							mActivity.tintViewIcons(v);
							listAdapter.notifyDataSetChanged();
							listAdapter.notifyDataSetInvalidated();
						}
					});
				}
			});
			if (texture.resourceId != 0) {
				Drawable drawable = textureSampleCache.getImage(texture.resourceId);
				if (drawable != null) {
					img.setImageResource(0);
					img.setImageDrawable(drawable);
					img.setBackgroundColor(Color.TRANSPARENT);
				} else {
					img.setBackgroundColor(cl);
					img.setImageResource(0);
					img.setImageDrawable(null);
				}
			} else {
				// load image from file
				Drawable drawable = textureSampleCache.getImage(texture.id);
				if (drawable != null) {
					img.setImageResource(0);
					img.setImageDrawable(drawable);
					img.setBackgroundColor(Color.TRANSPARENT);
				} else {
					img.setBackgroundColor(cl);
					img.setImageResource(0);
					img.setImageDrawable(null);
				}
			}
		}
	}

//  commented out - not used in KR, but maintain in actual state
//	class NumberPickerOption extends OptionBase {
//		private int minValue = 9;
//		private int maxValue = 340;
//		public NumberPickerOption(OptionOwner owner, String label, String property ) {
//			super(owner, label, property);
//		}
//		public int getItemViewType() {
//			return OPTION_VIEW_TYPE_NUMBER;
//		}
//		private int getValueInt() {
//			int res = 0;
//			try {
//				res = Integer.parseInt(mProperties.getProperty(property));
//			} catch (NumberFormatException ignored) {}
//			return res;
//		}
//		NumberPickerOption setMinValue(int minValue) {
//			this.minValue = minValue;
//			return this;
//		}
//		NumberPickerOption setMaxValue(int maxValue) {
//			this.maxValue = maxValue;
//			return this;
//		}
//		public void onSelect() {
//			if (!enabled)
//				return;
//			InputDialog dlg = new InputDialog(mActivity, label, false, "", true, minValue, maxValue, getValueInt(), new InputDialog.InputHandler() {
//				@Override
//				public boolean validate(String s) throws Exception {
//					int value = Integer.parseInt(s);
//					return value >= minValue && value <= maxValue;
//				}
//
//				@Override
//				public void onOk(String s) throws Exception {
//					getProperties().setProperty(property, s);
//					refreshItem();
//				}
//
//				@Override
//				public void onCancel() {
//				}
//			});
//			dlg.show();
//		}
//	}

	//byte[] fakeLongArrayForDebug;
	
	public enum Mode {
		READER,
		BROWSER,
		TTS,
	}

	public static ArrayList<String> filterProfileSettings(Properties settings) {
		ArrayList<String> props = new ArrayList<String>();
		for (Object k : settings.keySet()) {
			String key = (String)k;
			boolean found = false;
			for (String pattern : Settings.PROFILE_SETTINGS) {
				if (pattern.endsWith("*")) {
					if (key.startsWith(pattern.substring(0, pattern.length()-1))) {
						found = true;
						break;
					}
				} else if (pattern.equalsIgnoreCase(key)) {
					found = true;
					break;
				} else if (key.startsWith("styles.")) {
					found = true;
					break;
				}
			}
			if (found) props.add(key);
		}
		return props;
	}

	public OptionsDialog(BaseActivity activity, Mode mode, ReaderView readerView, String[] fontFaces, String[] fontFacesFiles, TTSControlBinder ttsbinder) {
		super("OptionsDialog", activity, null, false, false);
		init(activity, mode, readerView, fontFaces, fontFacesFiles, ttsbinder, null);
	}

	public OptionsDialog(BaseActivity activity, Mode mode, ReaderView readerView, String[] fontFaces, String[] fontFacesFiles,
						 TTSControlBinder ttsbinder, TTSControlServiceAccessor ttscontrol) {
		super("OptionsDialog", activity, null, false, false);
		init(activity, mode, readerView, fontFaces, fontFacesFiles, ttsbinder, ttscontrol);
	}

	private void init(BaseActivity activity, Mode mode, ReaderView readerView, String[] fontFaces, String[] fontFacesFiles,
						 TTSControlBinder ttsbinder, TTSControlServiceAccessor ttscontrol)
	{
//		Log.i("cr3optionsdlg", "EINK_SCREEN = " + DeviceInfo.EINK_SCREEN);
//		Log.i("cr3optionsdlg", "SCREEN_CAN_CONTROL_BRIGHTNESS = " + DeviceInfo.SCREEN_CAN_CONTROL_BRIGHTNESS);
//		Log.i("cr3optionsdlg", "ONYX_BRIGHTNESS_FILE = " + DeviceInfo.ONYX_BRIGHTNESS_FILE);
//		Log.i("cr3optionsdlg", "ONYX_BRIGHTNESS_WARM_FILE = " + DeviceInfo.ONYX_BRIGHTNESS_WARM_FILE);
//		Log.i("cr3optionsdlg", "EINK_HAVE_FRONTLIGHT = " + DeviceInfo.EINK_HAVE_FRONTLIGHT);
//		Log.i("cr3optionsdlg", "EINK_HAVE_NATURAL_BACKLIGHT = " + DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT);
//		Log.i("cr3optionsdlg", "EINK_SCREEN_UPDATE_MODES_SUPPORTED = " + DeviceInfo.EINK_SCREEN_UPDATE_MODES_SUPPORTED);
//		Log.i("cr3optionsdlg", "NOOK_NAVIGATION_KEYS = " + DeviceInfo.NOOK_NAVIGATION_KEYS);
//		Log.i("cr3optionsdlg", "EINK_NOOK1 = " + DeviceInfo.EINK_NOOK1);
//		Log.i("cr3optionsdlg", "EINK_NOOK2 = " + DeviceInfo.EINK_NOOK2);
//		Log.i("cr3optionsdlg", "EINK_NOOK = " + DeviceInfo.EINK_NOOK);
//		Log.i("cr3optionsdlg", "EINK_NOOK_120 = " + DeviceInfo.EINK_NOOK_120);
//		Log.i("cr3optionsdlg", "EINK_ONYX = " + DeviceInfo.EINK_ONYX);

		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors((CoolReader) activity, isEInk);

		String filter = "";
		if (activity instanceof CoolReader) filter = ((CoolReader)activity).optionsFilter;
// 		removed due to https://github.com/plotn/coolreader/issues/520, make it simple
//		File f = activity.getSettingsFileF(activity.getCurrentProfile());
//		String sF = f.getAbsolutePath();
//		sF = sF.replace("/storage/","/s/").replace("/emulated/","/e/");
		String sprof = activity.getCurrentProfileName();
		if (StrUtils.isEmptyStr(sprof)) sprof = getString(R.string.profile)+" "+activity.getCurrentProfile();
		String sF = "\n"+getString(R.string.settings_info_short);
		if (!filter.trim().equals("")) sF = sF + "\n"+getString(R.string.mi_filter_option) + ": "+filter;
		this.upperText = sprof + sF;
		this.searchEnabled = true;
		mActivity = activity;
		mReaderView = readerView;
		FontSelectOption.mFontFaces = fontFaces;
		FontSelectOption.mFontFacesFiles = fontFacesFiles;
		mTTSControl = ttscontrol;
		if (ttsbinder == null) {
			if (mTTSControl != null)
				mTTSControl.bind(ttsb -> {
					mTTSBinder = ttsb;
				});
		} else mTTSBinder = ttsbinder;
		mProperties = new Properties(mActivity.settings()); //  readerView.getSettings();
		mFilteredProps = filterProfileSettings(mProperties);
		mOldProperties = new Properties(mProperties);
		if (mode == Mode.READER) {
			if (mReaderView != null) {
				mProperties.setBool(PROP_TXT_OPTION_PREFORMATTED, mReaderView.isTextAutoformatEnabled());
				mProperties.setBool(PROP_EMBEDDED_STYLES, mReaderView.getDocumentStylesEnabled());
				mProperties.setBool(PROP_EMBEDDED_FONTS, mReaderView.getDocumentFontsEnabled());
				mProperties.setInt(PROP_REQUESTED_DOM_VERSION, mReaderView.getDOMVersion());
				mProperties.setInt(PROP_RENDER_BLOCK_RENDERING_FLAGS, mReaderView.getBlockRenderingFlags());
				isTextFormat = readerView.isTextFormat();
				isEpubFormat = readerView.isFormatWithEmbeddedFonts();
				isFormatWithEmbeddedStyle = readerView.isFormatWithEmbeddedStyles();
				isHtmlFormat = readerView.isHtmlFormat();
			}
		}
		showIcons = mProperties.getBool(PROP_APP_SETTINGS_SHOW_ICONS, true);
		mSynthWeights = Engine.getAvailableSynthFontWeight();
		if (null == mSynthWeights)
			mSynthWeights = new int[] {};
		this.mode = mode;
	}

	public View createTabContent(String tag) {
		if ("App".equals(tag) )
			return mOptionsApplication;
		else if ("Styles".equals(tag) )
			return mOptionsStyles;
		else if ("CSS".equals(tag) )
			return mOptionsCSS;
		else if ("Controls".equals(tag) )
			return mOptionsControls;
		else if ("Page".equals(tag))
			return mOptionsPage;
		// KR: CR's sync is not implemented since we have our own via Yandex.Disk and DropBox
//		if ( "Clouds".equals(tag) )
//			if (null != mOptionsCloudSync) {
//				return mOptionsCloudSync;
//			}
		return null;
	}

	private String getString(int resourceId)
	{
		return getContext().getResources().getString(resourceId); 
	}

	public String getString( int resourceId, Object... formatArgs ) {
		return getContext().getResources().getString(resourceId, formatArgs);
	}

	private ListOption createStyleEditor(String styleCode, int titleId, int addInfo, String filter) {
		ListOption res = new StyleEditorOption(this.mActivity, getContext(), this, getString(titleId), "styles." + styleCode,
				getString(addInfo), filter);
		res.noIcon();
		return res;
	}

	final static private String[] styleCodes = {
		"def",
		"title",
		"subtitle",
		"pre",
		"link",
		"cite",
		"epigraph",
		"poem",
		"text-author",
		"footnote",
		"footnote-link",
		"footnote-title",
		"annotation",
	};
	
	final static private int[] styleTitles = {
		R.string.options_css_def,
		R.string.options_css_title,
		R.string.options_css_subtitle,
		R.string.options_css_pre,
		R.string.options_css_link,
		R.string.options_css_cite,
		R.string.options_css_epigraph,
		R.string.options_css_poem,
		R.string.options_css_textauthor,
		R.string.options_css_footnote,
		R.string.options_css_footnotelink,
		R.string.options_css_footnotetitle,
		R.string.options_css_annotation,
	};

	final static private int[] styleAddInfos = {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};

	public OptionBase optRenderingPreset = null;
	public OptionBase optDOMVersion = null;

	private void fillStyleEditorOptions(String filter) {
		mOptionsCSS = new OptionsListView(getContext(),null);
		//mProperties.setBool(PROP_TXT_OPTION_PREFORMATTED, mReaderView.isTextAutoformatEnabled());
		//mProperties.setBool(PROP_EMBEDDED_STYLES, mReaderView.getDocumentStylesEnabled());
		mOptionsCSS.add(new BoolOption(this.mActivity, this, getString(R.string.mi_book_styles_enable_def), PROP_EMBEDDED_STYLES_DEF,
				getString(R.string.mi_book_styles_enable_def_add_info), filter).setDefaultValue("0").noIcon()
		);
		for (int i=0; i<styleCodes.length; i++) {
			StyleEditorOption seO = (StyleEditorOption) createStyleEditor(styleCodes[i], styleTitles[i], styleAddInfos[i], filter);
			seO.updateFilterEnd();
			mOptionsCSS.add(seO);
		}
	}
	
	private void setupBrowserOptions(String filter)
	{
        mInflater = LayoutInflater.from(getContext());
        ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.options_browser, null);
        ViewGroup body = view.findViewById(R.id.body);
        
        
        mOptionsBrowser = new OptionsListView(getContext(), null);

		mOptionsBrowser.add(new ListOption(this, getString(R.string.mi_book_sort_order), PROP_APP_BOOK_SORT_ORDER,
				getString(R.string.option_add_info_empty_text), filter).
				add(FilebrowserOption.sortOrderValues, FilebrowserOption.sortOrderLabels, FilebrowserOption.sortOrderAddInfos).
				setDefaultValue(FileInfo.SortOrder.TITLE_AUTHOR.name()).noIcon());
		OptionBase sbO5 = new FilebrowserOption(this.mActivity, getContext(), this, getString(R.string.filebrowser_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_library, R.drawable.icons8_library);
		((FilebrowserOption)sbO5).updateFilterEnd();
		mOptionsBrowser.add(sbO5);
		mOptionsBrowser.add(new ListOption(this, getString(R.string.options_app_backlight_screen), PROP_APP_SCREEN_BACKLIGHT,
				getString(R.string.options_app_backlight_screen_add_info), filter).
				add(BacklightOption.mBacklightLevels, BacklightOption.mBacklightLevelsTitles, BacklightOption.mBacklightLevelsAddInfos).setDefaultValue("-1").noIcon());
		mOptionsBrowser.add(new LangOption(this.mActivity, this, filter).noIcon());
		//mOptionsBrowser.add(new PluginsOption(this, getString(R.string.options_app_plugins), getString(R.string.option_add_info_empty_text), filter).noIcon());
		mOptionsBrowser.add(new BoolOption(this.mActivity, this, getString(R.string.options_app_fullscreen), PROP_APP_FULLSCREEN,
				getString(R.string.options_app_fullscreen_add_info), filter).setIconIdByAttr(R.attr.cr3_option_fullscreen_drawable, R.drawable.cr3_option_fullscreen));
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			mOptionsBrowser.add(new NightModeOption(this.mActivity, this, getString(R.string.options_inverse_view), PROP_NIGHT_MODE,
					getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_night_drawable, R.drawable.cr3_option_night));
		}
		if (!DeviceInfo.isForceHCTheme(false)) {
		//plotn - when setting EINK manually, hc doesnt work ... still dont know why
			mOptionsBrowser.add(new ThemeOptions(this.mActivity,this, getString(R.string.options_app_ui_theme),
					getString(R.string.options_app_ui_theme_add_info), filter).setIconIdByAttr(R.attr.attr_icons8_change_theme_1,
                    R.drawable.icons8_change_theme_1));
		}
		mOptionsBrowser.refresh();
		
		body.addView(mOptionsBrowser);
		this.mActivity.tintViewIcons(view,false);
		setView(view);
	}

	private void setupTTSOptions(String filter) {
		// KR - we do not use reader and tts options, prefering full set of options, so the code below was
		// copied to TTSOptions
		mInflater = LayoutInflater.from(getContext());
		ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.options_tts, null);
		ViewGroup body = view.findViewById(R.id.body);

		mOptionsTTS = new OptionsListView(getContext(), null);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			mTTSEngineOption = new ListOption(this, getString(R.string.options_tts_engine), PROP_APP_TTS_ENGINE,
					getString(R.string.option_add_info_empty_text), filter);
			mTTSBinder.retrieveAvailableEngines(list -> {
				BackgroundThread.instance().executeGUI(() -> {
					for (TextToSpeech.EngineInfo info : list) {
						mTTSEngineOption.add(info.name, info.label, "");
					}
					String tts_package = mProperties.getProperty(PROP_APP_TTS_ENGINE, "");
					mTTSEngineOption.setDefaultValue(tts_package);
					mTTSEngineOption.refreshList();
				});
			});
			mOptionsTTS.add(mTTSEngineOption.noIcon());
			mTTSEngineOption.setOnChangeHandler(() -> {
				String tts_package = mProperties.getProperty(PROP_APP_TTS_ENGINE, "");
				mTTSBinder.initTTS(tts_package, new OnTTSCreatedListener() {
					@Override
					public void onCreated() {
						if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
							BackgroundThread.instance().executeGUI(() -> {
								if (null != mTTSLanguageOption)
									TTSOption.fillTTSLanguages(OptionsDialog.this, mTTSLanguageOption);
								if (null != mTTSVoiceOption)
									mTTSVoiceOption.clear();
							});
						}
					}
					@Override
					public void onFailed() {
						if (null != mTTSLanguageOption)
							mTTSLanguageOption.clear();
					}
					@Override
					public void onTimedOut() {
						if (null != mTTSLanguageOption)
							mTTSLanguageOption.clear();
					}
				});
			});
		}
		mTTSUseDocLangOption = new BoolOption(this.mActivity, this, getString(R.string.options_tts_use_doc_lang), PROP_APP_TTS_USE_DOC_LANG,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("1").noIcon();
		mOptionsTTS.add(mTTSUseDocLangOption);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			boolean useDocLang = mProperties.getBool(PROP_APP_TTS_USE_DOC_LANG, true);
			mTTSLanguageOption = new ListOption(this, getString(R.string.options_tts_language), PROP_APP_TTS_FORCE_LANGUAGE,
					getString(R.string.option_add_info_empty_text), filter);
			TTSOption.fillTTSLanguages(OptionsDialog.this, mTTSLanguageOption);
			mTTSLanguageOption.setEnabled(!useDocLang);
			mOptionsTTS.add(mTTSLanguageOption);
			// onchange handler
			String lang = mProperties.getProperty (PROP_APP_TTS_FORCE_LANGUAGE, "");
			mTTSUseDocLangOption.setOnChangeHandler(() -> {
				boolean value = mProperties.getBool(PROP_APP_TTS_USE_DOC_LANG, true);
				mTTSLanguageOption.setEnabled(!value);
				mTTSVoiceOption.setEnabled(!value);
			});
			mTTSLanguageOption.setOnChangeHandler(() -> {
				String value = mProperties.getProperty(PROP_APP_TTS_FORCE_LANGUAGE, "");
				TTSOption.fillTTSVoices(OptionsDialog.this, mTTSVoiceOption, value);
			});

			mTTSVoiceOption = new ListOption(this, getString(R.string.options_tts_voice),
					PROP_APP_TTS_VOICE, getString(R.string.option_add_info_empty_text), filter);
			TTSOption.fillTTSVoices(OptionsDialog.this, mTTSVoiceOption, lang);
			mTTSVoiceOption.setEnabled(!useDocLang);
			mOptionsTTS.add(mTTSVoiceOption);
		}
		mOptionsTTS.add(new BoolOption(this.mActivity, this, getString(R.string.options_tts_google_abbr_workaround),
				PROP_APP_TTS_GOOGLE_END_OF_SENTENCE_ABBR, getString(R.string.options_tts_google_abbr_workaround_comment), filter).
				setDefaultValue("1").noIcon());
		mOptionsTTS.add(new ListOption(this, getString(R.string.options_app_tts_stop_motion_timeout), PROP_APP_MOTION_TIMEOUT,
				getString(R.string.option_add_info_empty_text), filter).
				add(mMotionTimeouts, mMotionTimeoutsTitles).setDefaultValue(Integer.toString(mMotionTimeouts[0])).noIcon());
		mOptionsTTS.refresh();
		body.addView(mOptionsTTS);
		this.mActivity.tintViewIcons(view,false);
		setView(view);
	}

	private String getWeightName(int weight) {
		String name = "";
		switch (weight) {
			case 100:
				name = getString(R.string.font_weight_thin);
				break;
			case 200:
				name = getString(R.string.font_weight_extralight);
				break;
			case 300:
				name = getString(R.string.font_weight_light);
				break;
			case 350:
				name = getString(R.string.font_weight_book);
				break;
			case 400:
				name = getString(R.string.font_weight_regular);
				break;
			case 500:
				name = getString(R.string.font_weight_medium);
				break;
			case 600:
				name = getString(R.string.font_weight_semibold);
				break;
			case 700:
				name = getString(R.string.font_weight_bold);
				break;
			case 800:
				name = getString(R.string.font_weight_extrabold);
				break;
			case 900:
				name = getString(R.string.font_weight_black);
				break;
			case 950:
				name = getString(R.string.font_weight_extrablack);
				break;
		}
		return name;
	}

	private void updateFontWeightValues(ListOption option, String faceName) {
		// get available weight for font faceName
		int[] nativeWeights = Engine.getAvailableFontWeight(faceName);
		if (null == nativeWeights || 0 == nativeWeights.length) {
			// invalid font
			option.clear();
			return;
		}
		ArrayList<Integer> nativeWeightsArray = new ArrayList<>();	// for search
		for (int w : nativeWeights)
			nativeWeightsArray.add(w);
		// combine with synthetic weights
		ArrayList<Integer> weights = new ArrayList<>();
		int synth_idx = 0;
		int i, j;
		int weight = 0, prev_weight = 0;
		for (i = 0; i < nativeWeights.length; i++) {
			weight = nativeWeights[i];
			for (j = synth_idx; j < mSynthWeights.length; j++) {
				int synth_weight = mSynthWeights[j];
				if (synth_weight < weight) {
					if (synth_weight > prev_weight)
						weights.add(synth_weight);
				}
				else
					break;
			}
			synth_idx = j;
			weights.add(weight);
			prev_weight = weight;
		}
		for (j = synth_idx; j < mSynthWeights.length; j++) {
			if (mSynthWeights[j] > weight)
				weights.add(mSynthWeights[j]);
		}
		// fill items
		option.clear();
		for (i = 0; i < weights.size(); i++) {
			weight = weights.get(i);
			String label = String.valueOf(weight);
			String descr = getWeightName(weight);
			if (!nativeWeightsArray.contains(weight)) {
				if (descr.length() > 0)
					descr += ", " + getString(R.string.font_weight_fake);
				else
					descr = getString(R.string.font_weight_fake);
			}
			// if (descr.length() > 0)
			//	label += " (" + descr + ")"; - better put it to addinfo
			option.add(weight, label, descr);
		}
		// enable/disable font hinting option
		//int base_weight = mProperties.getInt(PROP_FONT_BASE_WEIGHT, 400);
		//mFontHintingOption.setEnabled(nativeWeightsArray.contains(base_weight));
	}

	private void setupReaderOptions(String filter)
	{
        mInflater = LayoutInflater.from(getContext());
        mTabs = (TabHost) mInflater.inflate(R.layout.options, null);
		int colorGray = themeColors.get(R.attr.colorThemeGray2);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		mTabs.setOnTabChangedListener(tabId -> {
			for (int i = 0; i < mTabs.getTabWidget().getChildCount(); i++) {
				try {
					mTabs.getTabWidget().getChildAt(i)
						.setBackgroundColor(isEInk? Color.WHITE: colorGrayC); // unselected
				} catch (Exception e) {
					Log.e("OPTIONSDLG", "setupReaderOptions 1", e);
				}
			}
			try {
				mTabs.getTabWidget().getChildAt(mTabs.getCurrentTab())
						.setBackgroundColor(isEInk? colorGrayC: colorGray); // selected
			} catch (Exception e) {
				Log.e("OPTIONSDLG", "setupReaderOptions 2", e);
			}
			if (mTitleBarFontColor1 != null) mTitleBarFontColor1.refreshItem();
			if (mTitleBarFontColor2 != null) mTitleBarFontColor2.refreshItem();
			//asdf TODO: refresh backlight items
			//if (mBacklightControl1 != null) mBacklightControl1.refreshItem();
			//if (mBacklightControl2 != null) mBacklightControl2.refreshItem();
			mProperties.setProperty(Settings.PROP_APP_OPTIONS_PAGE_SELECTED, "" + mTabs.getCurrentTab());
		});
		// setup tabs
		//setView(R.layout.options);
		//setContentView(R.layout.options);
		//mTabs = (TabHost)findViewById(android.R.id.tabhost); 
		mTabs.setup();
		
		//tabWidget.
		//new TabHost(getContext());

		boolean legacyRender = mProperties.getInt(PROP_RENDER_BLOCK_RENDERING_FLAGS, 0) == 0 ||
				mProperties.getInt(PROP_REQUESTED_DOM_VERSION, 0) < Engine.DOM_VERSION_CURRENT;

		mOptionsStyles = new OptionsListView(getContext(), null);
		OptionBase fontOption = new FontSelectOption(this.mActivity,this, getString(R.string.options_font_face), PROP_FONT_FACE,
				getString(R.string.option_add_info_empty_text), false, filter).
				setIconIdByAttr(R.attr.cr3_option_font_face_drawable, R.drawable.cr3_option_font_face);
		mOptionsStyles.add(fontOption);
		FlowListOption optFontSize = new FlowListOption(this, getString(R.string.options_font_size), PROP_FONT_SIZE,
				getString(R.string.option_add_info_empty_text), filter);
		for (int i = 0; i <= mFontSizes.length-1; i++) optFontSize.add(""+mFontSizes[i], ""+mFontSizes[i],"");
		optFontSize.setDefaultValue("24").setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size);
		mOptionsStyles.add(optFontSize);
		mOptionsStyles.add(new BoolOption(this.mActivity, this, getString(R.string.options_font_italicize), PROP_FONT_ITALICIZE,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_text_italic_drawable, R.drawable.cr3_option_text_italic));
		mFontWeightOption = (ListOption) new ListOption(this,
				getString(R.string.options_font_weight), PROP_FONT_BASE_WEIGHT, getString(R.string.option_add_info_empty_text), filter).
				setIconIdByAttr(R.attr.cr3_option_text_bold_drawable, R.drawable.cr3_option_text_bold);
		updateFontWeightValues(mFontWeightOption, mProperties.getProperty(PROP_FONT_FACE, ""));
		mOptionsStyles.add(mFontWeightOption);
		fontOption.setOnChangeHandler(() -> {
			String faceName = mProperties.getProperty(PROP_FONT_FACE, "");
			updateFontWeightValues(mFontWeightOption, faceName);
		});
		/*
		mFontWeightOption.setOnChangeHandler(() -> {
			// enable/disable font hinting option
			String faceName = mProperties.getProperty(PROP_FONT_FACE, "");
			int[] nativeWeights = Engine.getAvailableFontWeight(faceName);
			if (null != nativeWeights && 0 != nativeWeights.length) {
				ArrayList<Integer> nativeWeightsArray = new ArrayList<>();    // for search
				for (int w : nativeWeights)
					nativeWeightsArray.add(w);
				//int base_weight = mProperties.getInt(PROP_FONT_BASE_WEIGHT, 400);
				//mFontHintingOption.setEnabled(nativeWeightsArray.contains(base_weight));
			}
		});
		 */
		OptionBase sbFFO = new FallbackFontsOptions(this.mActivity, OptionsDialog.this,
				this, getString(R.string.options_font_fallback_faces), getString(R.string.option_add_info_empty_text), filter)
			.setIconIdByAttr(R.attr.cr3_option_font_face_drawable, R.drawable.cr3_option_font_face);
		((FallbackFontsOptions)sbFFO).updateFilterEnd();
		mOptionsStyles.add(sbFFO);
		mOptionsStyles.add(new ListOption(this, getString(R.string.options_font_antialias), PROP_FONT_ANTIALIASING,
				getString(R.string.option_add_info_empty_text), filter).add(mAntialias, mAntialiasTitles, mAntialiasAddInfos).setDefaultValue("2").setIconIdByAttr(R.attr.cr3_option_text_antialias_drawable, R.drawable.cr3_option_text_antialias));
		mOptionsStyles.add(new BoolOption(this.mActivity, this, getString(R.string.options_style_floating_punctuation), PROP_FLOATING_PUNCTUATION,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("1").setIconIdByAttr(R.attr.cr3_option_text_floating_punct_drawable, R.drawable.cr3_option_text_other));
		OptionBase sb13 = new FontTweaksOption(this.mActivity, getContext(),this, getString(R.string.font_tweaks),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_text_ligatures_drawable, R.drawable.cr3_option_text_ligatures);
		((FontTweaksOption)sb13).updateFilterEnd();
		mOptionsStyles.add(sb13);
		OptionBase sb14 = new SpacingOption(this.mActivity, getContext(), this, getString(R.string.spacing_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_text_width_drawable, R.drawable.cr3_option_text_width);
		((SpacingOption)sb14).updateFilterEnd();
		mOptionsStyles.add(sb14);
		OptionBase sb15 = new HyphRendOption(this.mActivity, OptionsDialog.this,this, getString(R.string.hyph_rend_options),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_text_hyphenation_drawable, R.drawable.cr3_option_text_hyphenation);
		((HyphRendOption)sb15).updateFilterEnd();
		mOptionsStyles.add(sb15);
		OptionBase isO = new ImageScalingOption(this.mActivity, getContext(),this, getString(R.string.options_format_image_scaling), getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_images_drawable, R.drawable.cr3_option_images);
		((ImageScalingOption)isO).updateFilterEnd();
		mOptionsStyles.add(isO);
		mOptionsPage = new OptionsListView(getContext(), null);
		mOptionsPage.add(new BoolOption(this.mActivity, this, getString(R.string.options_app_fullscreen), PROP_APP_FULLSCREEN,
				getString(R.string.options_app_fullscreen_add_info), filter).setIconIdByAttr(R.attr.cr3_option_fullscreen_drawable, R.drawable.cr3_option_fullscreen));
		OptionBase sb12 = new PageAndOrientationOption(this.mActivity, OptionsDialog.this,this, getString(R.string.pageandorientation_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_pages_two_drawable, R.drawable.cr3_option_pages_two);
		((PageAndOrientationOption)sb12).updateFilterEnd();
		mOptionsPage.add(sb12);
		OptionBase sbO8 = new PageMarginsOption(this.mActivity, getContext(), this, getString(R.string.page_margins_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_text_margin_left_drawable, R.drawable.cr3_option_text_margin_left);
		((PageMarginsOption)sbO8).updateFilterEnd();
		mOptionsPage.add(sbO8);
		OptionBase sbO7 = new PageColorsOption(this.mActivity, OptionsDialog.this, this, getString(R.string.page_color_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_paint_palette1, R.drawable.icons8_paint_palette1);
		((PageColorsOption)sbO7).updateFilterEnd();
		mOptionsPage.add(sbO7);
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
			mOptionsPage.add(new TextureOptions(this, getString(R.string.options_background_texture),
					getString(R.string.options_background_texture_add_info), filter).
					setIconIdByAttr(R.attr.attr_icons8_texture, R.drawable.icons8_texture));
		if ((DeviceInfo.EINK_SCREEN_UPDATE_MODES_SUPPORTED) || (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))) {
			OptionBase sb16 = new EinkScreenUpdateOption(this.mActivity, getContext(), this, getString(R.string.eink_screen_update_options),
					getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_eink, R.drawable.icons8_eink);
			((EinkScreenUpdateOption) sb16).updateFilterEnd();
			mOptionsPage.add(sb16);
		}
		OptionBase sbO2 = new StatusBarOption(mActivity, OptionsDialog.this, this, getString(R.string.options_page_titlebar_new), PROP_APP_TITLEBAR_NEW,
				getString(R.string.options_page_titlebar_add_info), filter).setIconIdByAttr(R.attr.attr_icons8_document_r_title, R.drawable.icons8_document_r_title);
		((StatusBarOption)sbO2).updateFilterEnd();
		mOptionsPage.add(sbO2);
		mFootNotesOption = new BoolOption(this.mActivity, this, getString(R.string.options_page_footnotes), PROP_FOOTNOTES,
				getString(R.string.options_page_footnotes_add_info), filter).setDefaultValue("1")
				.setIconIdByAttr(R.attr.attr_icons8_book_title2, R.drawable.icons8_book_title2);
		int value = mProperties.getInt(PROP_PAGE_VIEW_MODE, 1);
		mFootNotesOption.enabled = value == 1;
		mOptionsPage.add(mFootNotesOption);
		mOptionsPage.add(new ListOption(this, getString(R.string.options_view_bookmarks_highlight), PROP_APP_HIGHLIGHT_BOOKMARKS,
				getString(R.string.options_view_bookmarks_highlight_add_info), filter).add(mHighlightMode, mHighlightModeTitles, mHighlightModeAddInfos).setDefaultValue("1").
				setIconIdByAttr(R.attr.attr_icons8_bookmark_simple_color,R.drawable.icons8_bookmark_simple_color));
		mOptionsPage.add(new BoolOption(this.mActivity, this, getString(R.string.options_view_highlight_user_dic), PROP_APP_HIGHLIGHT_USER_DIC,
				getString(R.string.options_view_bookmarks_user_dic_add_info), filter).setDefaultValue("1").
				setIconIdByAttr(R.attr.attr_icons8_google_translate_user,R.drawable.icons8_google_translate_user));
		OptionBase sbO = new ToolbarOption(this.mActivity, getContext(), this, getString(R.string.toolbar),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_navigation_toolbar_top, R.drawable.icons8_navigation_toolbar_top);
		((ToolbarOption)sbO).updateFilterEnd();
		mOptionsPage.add(sbO);
		if (FlavourConstants.PREMIUM_FEATURES) {
			mOptionsPage.add(new GeoOption(this.mActivity, this, filter).
					setIconIdByAttr(R.attr.attr_icons8_train_headphones, R.drawable.train_headphones));
		}
		mOptionsControls = new OptionsListView(getContext(), null);
		OptionBase kmO = new KeyMapOption(this.mActivity, getContext(),this, getString(R.string.options_app_key_actions),
				getString(R.string.options_app_key_actions_add_info), filter).setIconIdByAttr(R.attr.cr3_option_controls_keys_drawable, R.drawable.cr3_option_controls_keys);
		((KeyMapOption)kmO).updateFilterEnd();
		mOptionsControls.add(kmO);
		mOptionsControls.add(new TapZoneOption(this.mActivity,this, getString(R.string.options_app_tapzones_normal), PROP_APP_TAP_ZONE_ACTIONS_TAP,
				getString(R.string.options_app_tapzones_normal_add_info), filter).setIconIdByAttr(R.attr.cr3_option_controls_tapzones_drawable, R.drawable.cr3_option_controls_tapzones));
		Runnable doubleTapOnChange = () -> {
			int type = mProperties.getInt(PROP_APP_SECONDARY_TAP_ACTION_TYPE, TAP_ACTION_TYPE_LONGPRESS);
			boolean dblText = mProperties.getBool(PROP_APP_DOUBLE_TAP_SELECTION, false);
			mBounceProtectionOption.setEnabled(type == TAP_ACTION_TYPE_LONGPRESS && !dblText);
		};
		mOptionsControls.add(new ListOption(this, getString(R.string.options_controls_tap_secondary_action_type), PROP_APP_SECONDARY_TAP_ACTION_TYPE,
				getString(R.string.options_controls_tap_secondary_action_type_add_info), filter).add(mTapSecondaryActionType, mTapSecondaryActionTypeTitles, mTapSecondaryActionTypeAddInfos).setDefaultValue(String.valueOf(TAP_ACTION_TYPE_LONGPRESS)).
				setIconIdByAttr(R.attr.attr_icons8_double_tap, R.drawable.icons8_double_tap));
		mOptionsControls.add(new BoolOption(this.mActivity, this, getString(R.string.options_app_double_tap_selection), PROP_APP_DOUBLE_TAP_SELECTION,
				getString(R.string.options_app_double_tap_selection_add_info), filter).
				//setComment(getString(R.string.options_app_double_tap_selection_slowdown)).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_double_click_tap, R.drawable.icons8_double_click_tap));
		mOptionsControls.add(new ListOption(this, getString(R.string.double_tap_interval), PROP_DOUBLE_CLICK_INTERVAL,
				getString(R.string.option_add_info_empty_text), filter).
				add(mDoubleClickIntervals).
				setDefaultValue("400").setIconIdByAttr(R.attr.attr_icons8_double_click_tap_interval, R.drawable.icons8_double_click_tap_interval));
		mBounceProtectionOption = new ListOption(this, getString(R.string.prevent_tap_interval), PROP_PREVENT_CLICK_INTERVAL,
				getString(R.string.prevent_tap_interval_add_info), filter);
		mBounceProtectionOption.add(mPreventClickIntervals).
				setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_prevent_accidental_tap_interval, R.drawable.icons8_prevent_accidental_tap_interval);
		mOptionsControls.add(mBounceProtectionOption);
		doubleTapOnChange.run();
		//if ( !DeviceInfo.EINK_SCREEN  || DeviceInfo.EINK_HAVE_FRONTLIGHT ) // nook glowlight has this option. 20201022 Still commented, not tested
		//asdf TODO: work with backlight
//		mBacklightControl2 = new ListOption(this, getString(R.string.options_controls_flick_brightness), PROP_APP_FLICK_BACKLIGHT_CONTROL,
//					getString(R.string.options_controls_flick_brightness_add_info), filter).
//					add(mFlickBrightness, mFlickBrightnessTitles, mFlickBrightnessAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_sunrise,R.drawable.icons8_sunrise);
//		mOptionsControls.add(mBacklightControl2);
		OptionBase sbPFO = new PageFlipOption(this.mActivity, getContext(), this, getString(R.string.page_flipping_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_gesture, R.drawable.icons8_gesture);
		((PageFlipOption)sbPFO).updateFilterEnd();
		mOptionsControls.add(sbPFO);

		mOptionsControls.add(new BoolOption(this.mActivity, this, getString(R.string.disable_two_pointer_gestures), PROP_APP_DISABLE_TWO_POINTER_GESTURES,
				getString(R.string.two_pointer_gestures_add_info), filter).setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_two_fingers, R.drawable.icons8_two_fingers));
		OptionBase sb11 = new SelectionModesOption(this.mActivity, getContext(),this, getString(R.string.selectionmodes_settings),
				getString(R.string.selectionmodes_settings_add_info), filter).setIconIdByAttr(R.attr.attr_icons8_select_all, R.drawable.icons8_select_all);
		((SelectionModesOption)sb11).updateFilterEnd();
		mOptionsControls.add(sb11);
		OptionBase sbBookmarkSendToOption = new BookmarkSendToOption(this.mActivity, OptionsDialog.this,this, getString(R.string.options_bookmark_action_send_to_option),
				getString(R.string.option_add_info_empty_text), filter)
				.setIconIdByAttr(R.attr.attr_icons8_send_to_action, R.drawable.icons8_send_to_action);
		((BookmarkSendToOption)sbBookmarkSendToOption).updateFilterEnd();
		mOptionsControls.add(sbBookmarkSendToOption);
		mOptionsApplication = new OptionsListView(getContext(), null);
		mOptionsApplication.add(new LangOption(this.mActivity, this, filter).setIconIdByAttr(R.attr.attr_icons8_system_lang, R.drawable.icons8_system_lang));
		CoolReader cr = (CoolReader)mActivity;
		if (!DeviceInfo.isForceHCTheme(false)) {
			//plotn - when setting EINK manually, hc doesnt work ... still dont know why
			mOptionsApplication.add(new ThemeOptions(this.mActivity,this, getString(R.string.options_app_ui_theme), getString(R.string.options_app_ui_theme_add_info), filter).setIconIdByAttr(R.attr.attr_icons8_change_theme_1,
					R.drawable.icons8_change_theme_1));
		}
		mOptionsApplication.add(new FlowListOption(this, getString(R.string.ui_font_scale), PROP_APP_FONT_SCALE,
				getString(R.string.ui_font_scale_restart_app), filter).
				add(mUIFontScale, mUIFontScaleTitles, mUIFontScaleAddInfos).setDefaultValue("10").
					setIconIdByAttr(R.attr.attr_icons8_font_scale, R.drawable.icons8_font_scale));
		mOptionsApplication.add(new BoolOption(this.mActivity, this, getString(R.string.options_screen_force_eink), PROP_APP_SCREEN_FORCE_EINK,
				getString(R.string.options_screen_force_eink_add_info), filter).
				setIconIdByAttr(R.attr.attr_icons8_eink, R.drawable.icons8_eink).
				setDefaultValue("0")
				.setOnChangeHandler(() -> {
					mActivity.showToast(R.string.force_eink_warn);
				})
		);
		mOptionsApplication.add(new ListOption(this, getString(R.string.save_pos_timeout),
				PROP_SAVE_POS_TIMEOUT, getString(R.string.save_pos_timeout_add_info), filter).add(mMotionTimeouts1, mMotionTimeoutsTitles1, mMotionTimeoutsAddInfos1).setDefaultValue(Integer.toString(mMotionTimeouts1[2])).
				setIconIdByAttr(R.attr.attr_icons8_position_to_disk_interval, R.drawable.icons8_position_to_disk_interval));
		OptionBase sbO5 = new FilebrowserOption(this.mActivity, getContext(), this, getString(R.string.filebrowser_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_library, R.drawable.icons8_library);
		((FilebrowserOption)sbO5).updateFilterEnd();
		mOptionsApplication.add(sbO5);
		OptionBase sbO4 = new DictionariesOption(this.mActivity, OptionsDialog.this,this, getString(R.string.dictionary_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_google_translate, R.drawable.icons8_google_translate);
		((DictionariesOption)sbO4).updateFilterEnd();
		mOptionsApplication.add(sbO4);
		OptionBase sbO3 = new CloudOption(this.mActivity, getContext(), this, getString(R.string.cloud_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_cloud_storage, R.drawable.icons8_cloud_storage);
		((CloudOption)sbO3).updateFilterEnd();
		mOptionsApplication.add(sbO3);
		OptionBase sbO9 = new TTSOption(mActivity, OptionsDialog.this,this, getString(R.string.tts_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_button_tts_drawable, R.drawable.cr3_button_tts);
		((TTSOption)sbO9).updateFilterEnd();
		mOptionsApplication.add(sbO9);
		OptionBase sb10 = new BacklightOption(this.mActivity, getContext(),this, getString(R.string.backlight_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_sun, R.drawable.icons8_sun);
		((BacklightOption)sb10).updateFilterEnd();
		mOptionsApplication.add(sb10);
		OptionBase sbO6 = new RareOption(this.mActivity, OptionsDialog.this,this, getString(R.string.rare_settings),
				getString(R.string.option_add_info_empty_text), filter)
				.setIconIdByAttr(R.attr.attr_icons8_physics, R.drawable.icons8_physics);
		((RareOption)sbO6).updateFilterEnd();
		mOptionsApplication.add(sbO6);
		if (cr.settingsMayBeMigratedLastInd>=0) {
			mOptionsApplication.add(new ClickOption(this, getString(R.string.migrate_cr_settings),
					PROP_APP_MIGRATE_SETTINGS, getString(R.string.migrate_cr_settings_add_info), filter,
					(view1, optionLabel, optionValue) ->
					{
						mActivity.askConfirmation(R.string.migrate_cr_settings_q, () -> {
							ArrayList<CloudFileInfo> afi = new ArrayList<>();
							for (int i = 0; i <= cr.settingsMayBeMigratedLastInd; i++) {
								if (cr.getSettingsFileExtExists(".cr3", i))
									if (cr.getSettingsFileExt(".cr3", i).isFile()) {
										CloudFileInfo cfi = new CloudFileInfo();
										cfi.name = cr.getSettingsFileExt(".cr3", i).getName();
										cfi.path = cr.getSettingsFileExt(".cr3", i).getPath();
										afi.add(cfi);
									}
							}
							CloudSync.restoreSettingsFiles(cr, null, afi, false);
						});
					}, true).setDefaultValue(getString(R.string.migrate_cr_settings_profiles) + " " + (cr.settingsMayBeMigratedLastInd + 1)).
					setIconIdByAttr(R.attr.coolreader_logo_button_drawable, R.drawable.cr3_logo_button));
		}
		mOptionsApplication.add(new RestoreSettingsOption(this.mActivity, getContext(), this,
				getString(R.string.restore_settings), getString(R.string.option_add_info_empty_text), filter).
				setIconIdByAttr(R.attr.attr_icons8_settings_from_hist, R.drawable.icons8_settings_from_hist));
		mOptionsApplication.add(new ClickOption(this, getString(R.string.init_app),
				PROP_APP_INIT, "", filter,
				(view1, optionLabel, optionValue) ->
				{
					InitAppDialog iad = new InitAppDialog((CoolReader) mActivity);
					iad.show();
				}, true).setDefaultValue(getString(R.string.init_app_add_info)).
				setIconIdByAttr(R.attr.attr_icons8_delete_database, R.drawable.icons8_delete_database));

//		if (BuildConfig.GSUITE_AVAILABLE && DeviceInfo.getSDKLevel() >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//			mOptionsCloudSync = new OptionsListView(getContext());
//			Runnable onGoogleDriveEnable = () -> {
//				boolean syncEnabled = mProperties.getBool(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_ENABLED, false);
//				boolean syncBookInfoEnabled = mProperties.getBool(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_INFO, false);
//				mCloudSyncAskConfirmationsOption.setEnabled(syncEnabled);
//				mGoogleDriveEnableSettingsOption.setEnabled(syncEnabled);
//				mGoogleDriveEnableBookmarksOption.setEnabled(syncEnabled);
//				mGoogleDriveEnableCurrentBookInfoOption.setEnabled(syncEnabled);
//				mGoogleDriveEnableCurrentBookBodyOption.setEnabled(syncEnabled && syncBookInfoEnabled);
//				mGoogleDriveAutoSavePeriodOption.setEnabled(syncEnabled);
//				// mCloudSyncBookmarksKeepAliveOptions should be enabled regardless of PROP_APP_CLOUDSYNC_GOOGLEDRIVE_ENABLED
//			};
//			mOptionsCloudSync.add(new BoolOption(this, getString(R.string.options_app_googledrive_sync_auto), PROP_APP_CLOUDSYNC_GOOGLEDRIVE_ENABLED).setDefaultValue("0").noIcon()
//					.setOnChangeHandler(onGoogleDriveEnable));
//			mCloudSyncAskConfirmationsOption = new BoolOption(this, getString(R.string.options_app_cloudsync_confirmations), PROP_APP_CLOUDSYNC_CONFIRMATIONS).setDefaultValue("1").noIcon();
//			mGoogleDriveEnableSettingsOption = new BoolOption(this, getString(R.string.options_app_googledrive_sync_settings), PROP_APP_CLOUDSYNC_GOOGLEDRIVE_SETTINGS).setDefaultValue("0").noIcon();
//			mGoogleDriveEnableBookmarksOption = new BoolOption(this, getString(R.string.options_app_googledrive_sync_bookmarks), PROP_APP_CLOUDSYNC_GOOGLEDRIVE_BOOKMARKS).setDefaultValue("0").noIcon();
//			mGoogleDriveEnableCurrentBookInfoOption = new BoolOption(this, getString(R.string.options_app_googledrive_sync_currentbook_info), PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_INFO).setDefaultValue("0").noIcon();
//			mGoogleDriveEnableCurrentBookInfoOption.setOnChangeHandler(() -> {
//				boolean syncBookInfoEnabled = mProperties.getBool(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_INFO, false);
//				mGoogleDriveEnableCurrentBookBodyOption.setEnabled(syncBookInfoEnabled);
//			});
//			mGoogleDriveEnableCurrentBookBodyOption = new BoolOption(this, getString(R.string.options_app_googledrive_sync_currentbook_body), PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_BODY).setDefaultValue("0").noIcon();
//			mGoogleDriveEnableCurrentBookInfoOption.onChangeHandler.run();
//			mGoogleDriveAutoSavePeriodOption = new ListOption(this, getString(R.string.autosave_period), PROP_APP_CLOUDSYNC_GOOGLEDRIVE_AUTOSAVEPERIOD).add(mGoogleDriveAutoSavePeriod, mGoogleDriveAutoSavePeriodTitles).setDefaultValue(Integer.valueOf(5).toString()).noIcon();
//			mCloudSyncDataKeepAliveOptions = new ListOption(this, getString(R.string.sync_data_keepalive_), PROP_APP_CLOUDSYNC_DATA_KEEPALIVE).add(mCloudBookmarksKeepAlive, mCloudBookmarksKeepAliveTitles).setDefaultValue(Integer.valueOf(14).toString()).noIcon();
//			onGoogleDriveEnable.run();
//			mOptionsCloudSync.add(mCloudSyncAskConfirmationsOption);
//			mOptionsCloudSync.add(mGoogleDriveEnableSettingsOption);
//			mOptionsCloudSync.add(mGoogleDriveEnableBookmarksOption);
//			mOptionsCloudSync.add(mGoogleDriveEnableCurrentBookInfoOption);
//			mOptionsCloudSync.add(mGoogleDriveEnableCurrentBookBodyOption);
//			mOptionsCloudSync.add(mGoogleDriveAutoSavePeriodOption);
//			mOptionsCloudSync.add(mCloudSyncDataKeepAliveOptions);
//		}

		fillStyleEditorOptions(filter);

		mOptionsStyles.refresh();
		mOptionsCSS.refresh();
		mOptionsPage.refresh();
		mOptionsApplication.refresh();
		if (null != mOptionsCloudSync) {
			mOptionsCloudSync.refresh();
		}

		addTab("Styles",
				Utils.resolveResourceIdByAttr(this.mActivity, R.attr.attr_icons8_type_filled, R.drawable.icons8_type_filled));
		addTab("CSS",
				Utils.resolveResourceIdByAttr(this.mActivity, R.attr.attr_icons8_css, R.drawable.icons8_css));
		addTab("Page",
				Utils.resolveResourceIdByAttr(this.mActivity, R.attr.attr_icons8_page, R.drawable.icons8_page));
		addTab("Controls",
				Utils.resolveResourceIdByAttr(this.mActivity, R.attr.attr_icons8_cursor, R.drawable.icons8_cursor));
		addTab("App",
				Utils.resolveResourceIdByAttr(this.mActivity, R.attr.attr_icons8_settings, R.drawable.icons8_settings));
		if (null != mOptionsCloudSync) {
			addTab("Clouds", R.drawable.cr3_tab_clouds);
		}
		setView(mTabs);
		this.mActivity.tintViewIcons(mTabs);
		mTabs.invalidate();
		mTabs.setCurrentTab(4);
		if (mOptionsApplication.mOptions.size()>0) mTabs.setCurrentTab(4); else
		if (mOptionsStyles.mOptions.size()>0) mTabs.setCurrentTab(0); else
		if (mOptionsCSS.mOptions.size()>0) mTabs.setCurrentTab(1); else
		if (mOptionsPage.mOptions.size()>0) mTabs.setCurrentTab(2); else
		if (mOptionsControls.mOptions.size()>0) mTabs.setCurrentTab(3); else {
			mTabs.setCurrentTab(4);
			this.mActivity.showToast(getString(R.string.mi_no_options) +" "+filter);
		}
		if (StrUtils.isEmptyStr(filter)) {
			int iTab = mActivity.settings().getInt(Settings.PROP_APP_OPTIONS_PAGE_SELECTED, 4);
			mTabs.setCurrentTab(iTab);
		}
	}

	private void addTab(String name, int imageDrawable) {
		TabHost.TabSpec ts = mTabs.newTabSpec(name);
		Drawable icon = getContext().getResources().getDrawable(imageDrawable);
		this.mActivity.tintViewIcons(icon,true);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
			// replace too small icons in tabs in Theme.Holo
			View tabIndicator = mInflater.inflate(R.layout.tab_indicator, null);
			ImageView imageView = tabIndicator.findViewById(R.id.tab_icon);
			imageView.setImageDrawable(icon);
			ts.setIndicator(tabIndicator);
		} else {
			ts.setIndicator("", icon);
		}
		ts.setContent(this);
		mTabs.addTab(ts);
	}

	@Override
	protected void onStart() {
		if (!StrUtils.isEmptyStr(selectedOption)) {
			BackgroundThread.instance().postGUI(() -> {
				OptionBase selOpt = null;
				if (mOptionsStyles != null)
					for (OptionBase opt : mOptionsStyles.mOptions) {
						if (opt.property.equals(selectedOption)) selOpt = opt;
					}
				if (mOptionsCSS != null)
					for (OptionBase opt : mOptionsCSS.mOptions) {
						if (opt.property.equals(selectedOption)) selOpt = opt;
					}
				if (mOptionsPage != null)
					for (OptionBase opt : mOptionsPage.mOptions) {
						if (opt.property.equals(selectedOption)) selOpt = opt;
					}
				if (mOptionsApplication != null)
					for (OptionBase opt : mOptionsApplication.mOptions) {
						if (opt.property.equals(selectedOption)) selOpt = opt;
					}
				if (mOptionsControls != null)
					for (OptionBase opt : mOptionsControls.mOptions) {
						if (opt.property.equals(selectedOption)) selOpt = opt;
					}
				if (mOptionsBrowser != null)
					for (OptionBase opt : mOptionsBrowser.mOptions) {
						if (opt.property.equals(selectedOption)) selOpt = opt;
					}
				if (selOpt != null) {
					selOpt.onSelect();
					if (selOpt.optionsListView != null) {
						if (selOpt.optionsListView == mOptionsStyles) mTabs.setCurrentTab(0);
						if (selOpt.optionsListView == mOptionsCSS) mTabs.setCurrentTab(1);
						if (selOpt.optionsListView == mOptionsPage) mTabs.setCurrentTab(2);
						if (selOpt.optionsListView == mOptionsControls) mTabs.setCurrentTab(3);
						if (selOpt.optionsListView == mOptionsApplication)
							mTabs.setCurrentTab(4);
					}
				}
				selectedOption = "";
			}, 100);
		} else {
			if (selectedTab != -1) {
				BackgroundThread.instance().postGUI(() -> {
					mTabs.setCurrentTab(selectedTab);
					selectedTab = -1;
				}, 100);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		L.v("creating OptionsDialog");
		CoolReader.dumpHeapAllocation();
		L.v("calling gc");
		System.gc();
		CoolReader.dumpHeapAllocation();
		L.v("creating options dialog");
		setCancelable(true);
		setCanceledOnTouchOutside(true);

		mMotionTimeoutsTitles = this.mActivity.getResources().getStringArray(R.array.motion_timeout_titles);
		mMotionTimeouts = this.mActivity.getResources().getIntArray(R.array.motion_timeout_values);
		mMotionTimeoutsAddInfos = this.mActivity.getResources().getIntArray(R.array.motion_timeout_add_infos);
		mMotionTimeoutsTitlesSec = this.mActivity.getResources().getStringArray(R.array.motion_timeout_sec_titles);
		mMotionTimeoutsSec = this.mActivity.getResources().getIntArray(R.array.motion_timeout_sec_values);
		mMotionTimeoutsAddInfosSec = this.mActivity.getResources().getIntArray(R.array.motion_timeout_sec_add_infos);
		for (int i=0;i<mMotionTimeouts.length; i++) mMotionTimeoutsAddInfos[i] = R.string.option_add_info_empty_text;
		for (int i=0;i<mMotionTimeoutsSec.length; i++) mMotionTimeoutsAddInfosSec[i] = R.string.option_add_info_empty_text;
		mMotionTimeoutsTitles1 = Arrays.copyOfRange(mMotionTimeoutsTitles,1,mMotionTimeoutsTitles.length-1);
		mMotionTimeouts1 = Arrays.copyOfRange(mMotionTimeouts,1,mMotionTimeouts.length-1);
		mMotionTimeoutsAddInfos1 = Arrays.copyOfRange(mMotionTimeoutsAddInfos,1,mMotionTimeoutsAddInfos.length-1);

		mPagesPerFullSwipeTitles = this.mActivity.getResources().getStringArray(R.array.pages_per_full_swipe_titles);
		mPagesPerFullSwipe = this.mActivity.getResources().getIntArray(R.array.pages_per_full_swipe_values);
		mPagesPerFullSwipeAddInfos = this.mActivity.getResources().getIntArray(R.array.pages_per_full_swipe_add_infos);
		for (int i=0;i<mPagesPerFullSwipeAddInfos.length; i++) mPagesPerFullSwipeAddInfos[i] = R.string.option_add_info_empty_text;

		mForceTTSTitles = this.mActivity.getResources().getStringArray(R.array.force_tts_titles);
		mForceTTS = this.mActivity.getResources().getIntArray(R.array.force_tts_values);
		mForceTTSAddInfos = this.mActivity.getResources().getIntArray(R.array.force_tts_add_infos);
		for (int i=0;i<mForceTTSAddInfos.length; i++) mForceTTSAddInfos[i] = R.string.option_add_info_empty_text;

		String filter = "";
		if (this.mActivity instanceof CoolReader) filter = ((CoolReader) this.mActivity).optionsFilter;
		this.optionFilter = filter;

		switch (mode) {
			case READER:
				setupReaderOptions(filter);
				break;
			case BROWSER:
				setupBrowserOptions("");
				break;
			case TTS:
				setupTTSOptions(filter);
				break;
		}

		setOnCancelListener(dialog -> onPositiveButtonClick());

		ImageButton positiveButton = view.findViewById(R.id.options_btn_back);
		positiveButton.setOnClickListener(v -> onPositiveButtonClick());

//		ImageButton negativeButton = (ImageButton)mTabs.findViewById(R.id.options_btn_cancel);
//		negativeButton.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				onNegativeButtonClick();
//			}
//		});
		String filterOptions = "";
		if (this.mActivity instanceof CoolReader) {
			CoolReader cr = ((CoolReader) this.mActivity);
			filterOptions = cr.optionsFilter;
			if (StrUtils.isEmptyStr(filterOptions)) {
				if (upperTextLayout != null) {
					TextView lbl = (TextView) upperTextLayout.findViewById(R.id.base_dlg_upper_text);
					if (lbl != null) {
						if (cr.getReaderView()!=null)
							lbl.setPaintFlags(lbl.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
						lbl.setOnClickListener(v -> {
							if (cr.getReaderView()!=null) {
								SwitchProfileDialog dlg = new SwitchProfileDialog(cr, cr.getReaderView(), OptionsDialog.this);
								dlg.show();
							} else {
								cr.showToast(cr.getString(R.string.switch_profile_note));
							}
						});
					}
				}
			}
		}
		super.onCreate(savedInstanceState);
		L.v("OptionsDialog is created");
	}

//	private void askApply()
//	{
//		Properties diff = mProperties.diff(mOldProperties);
//		if ( diff.size()>0 ) {
//			L.d("Some properties were changed, ask user whether to apply");
//			AlertDialog.Builder dlg = new AlertDialog.Builder(getContext());
//			dlg.setTitle(R.string.win_title_options_apply);
//			dlg.setPositiveButton(R.string.dlg_button_ok, new OnClickListener() {
//				public void onClick(DialogInterface arg0, int arg1) {
//					onPositiveButtonClick();
//				}
//			});
//			dlg.setNegativeButton(R.string.dlg_button_cancel, new OnClickListener() {
//				public void onClick(DialogInterface arg0, int arg1) {
//					onNegativeButtonClick();
//				}
//			});
//			dlg.show();
//		}
//	}

	public void apply() {
		if (mode == Mode.READER) {
			int domVersion = mProperties.getInt(PROP_REQUESTED_DOM_VERSION, Engine.DOM_VERSION_CURRENT);
			int rendFlags = mProperties.getInt(PROP_RENDER_BLOCK_RENDERING_FLAGS, Engine.BLOCK_RENDERING_FLAGS_WEB);
			if (mReaderView != null) {
				if (mProperties.getBool(PROP_TXT_OPTION_PREFORMATTED, true) != mReaderView.isTextAutoformatEnabled()) {
					mReaderView.toggleTextFormat();
				}
				if (mProperties.getBool(PROP_EMBEDDED_STYLES, true) != mReaderView.getDocumentStylesEnabled()) {
					mReaderView.toggleDocumentStyles();
				}
				if (mProperties.getBool(PROP_EMBEDDED_FONTS, true) != mReaderView.getDocumentFontsEnabled()) {
					mReaderView.toggleEmbeddedFonts();
				}
				if (domVersion != mReaderView.getDOMVersion()) {
					mReaderView.setDOMVersion(domVersion);
				}
				if (rendFlags != mReaderView.getBlockRenderingFlags()) {
					mReaderView.setBlockRenderingFlags(rendFlags);
				}
			}
		}
		mActivity.setSettings(mProperties, 0, true);
		try {
			if (mActivity instanceof CoolReader) {
				if (((CoolReader) mActivity).getmReaderFrame() != null)
					((CoolReader) mActivity).getmReaderFrame().updateCRToolbar(((CoolReader) mActivity));
				mActivity.setCutoutMode(mActivity.settings().getInt(PROP_EXT_FULLSCREEN_MARGIN, 0));
			}
		} catch (Exception e) {
		}
		//mReaderView.setSettings(mProperties, mOldProperties);
	}

	@Override
	protected void onPositiveButtonClick() {
		apply();
		((CoolReader)mActivity).backupSettings();
		if (((CoolReader)mActivity).getmReaderView() != null)
			((CoolReader)mActivity).getmReaderView().skipFallbackWarning = false;
		dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		onPositiveButtonClick();
	}

	@Override
	protected void onSearchClick() {
		BackgroundThread.instance().postGUI(() -> {
			((CoolReader) this.mActivity).showFilterDialog();
		}, 500);
		dismiss();
	}

	@Override
	protected void onStop() {
		//L.d("OptionsDialog.onStop() : calling gc()");
		//System.gc();
		super.onStop();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			return true;
		}
		if (mode == Mode.READER) {
			if (mTabs.getCurrentView().onKeyDown(keyCode, event))
				return true;
		} else {
			if (view.onKeyDown(keyCode, event))
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			onPositiveButtonClick();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}