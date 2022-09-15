package org.coolreader.readerview;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.CloudSync;

import org.coolreader.cloud.litres.LitresJsons;
import org.coolreader.crengine.BackgroundTextureInfo;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.BookInfoDialog;
import org.coolreader.crengine.BookTag;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.BookmarkEditDialog;
import org.coolreader.crengine.CalibreCatalogEditDialog;
import org.coolreader.crengine.CoverpageManager;
import org.coolreader.crengine.CustomLog;
import org.coolreader.crengine.DelayedExecutor;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DeviceOrientation;
import org.coolreader.crengine.ExternalDocCameDialog;
import org.coolreader.crengine.TagsEditDialog;
import org.coolreader.dic.DictsDlg;
import org.coolreader.crengine.DocProperties;
import org.coolreader.crengine.DocView;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.eink.EinkScreen;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FindNextDlg;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.GotoPageDialog;
import org.coolreader.crengine.HelpFileGenerator;
import org.coolreader.crengine.ImageInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.OrientationToolbarDlg;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.ReaderCallback;
import org.coolreader.crengine.ReaderCommand;
import org.coolreader.crengine.ResizeHistory;
import org.coolreader.crengine.Scanner;
import org.coolreader.crengine.SearchDlg;
import org.coolreader.crengine.Selection;
import org.coolreader.crengine.SelectionToolbarDlg;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.SomeButtonsToolbarDlg;
import org.coolreader.onyx.OnyxLibrary;
import org.coolreader.options.BoolOption;
import org.coolreader.userdic.UserDicPanel;
import org.coolreader.utils.StrUtils;
import org.coolreader.crengine.SwitchProfileDialog;
import org.coolreader.crengine.TOCDlg;
import org.coolreader.crengine.TOCItem;
import org.coolreader.utils.Utils;
import org.coolreader.crengine.VMRuntimeHack;
import org.coolreader.crengine.ViewMode;
import org.coolreader.dic.DicToastView;
import org.coolreader.dic.Dictionaries;
import org.coolreader.dic.OfflineDicsDlg;
import org.coolreader.options.BacklightOption;
import org.coolreader.options.OptionsDialog;
import org.coolreader.tts.TTSToolbarDlg;
import org.coolreader.userdic.UserDicDlg;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.ClipboardManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ReaderView implements android.view.SurfaceHolder.Callback, Settings, DocProperties,
		OnKeyListener, OnTouchListener, OnFocusChangeListener {

	public static final Logger log = L.create("rv", Log.VERBOSE);
	public static final Logger alog = L.create("ra", Log.WARN);
	private static final String TAG = "ReaderView";

	public final SurfaceView surface;
	public final BookView bookView;
	public SurfaceView getSurface() {
		return surface;
	}

	public static int backgrNormalizedColor = 0; // color of the background - to decide which icon color use

	private int wasX = 0;
	private int wasY = 0;

	public long lastSelTime = 0;
	private int curBlackpageInterval = 0; //for periodic blackpage draw
	private int blackpageDuration = 300;

	public ArrayList<String> getArrAllPages() {
		return arrAllPages;
	}

	private ArrayList<String> arrAllPages = null;

	public interface BookView {
		void draw();
		void draw(boolean isPartially);
		void draw(boolean isPartially, boolean isBlack);
		void invalidate();
		void onPause();
		void onResume();
	}

	public void CheckAllPagesLoadVisual() {
		if (arrAllPages == null) {
			showProgress(10000, R.string.progress_please_wait);
			CheckAllPagesLoad();
			hideProgress();
		}
	}

	public void CheckAllPagesLoad() {
		if (arrAllPages!=null)
			if (arrAllPages.size()>0)
				return;
		arrAllPages = new ArrayList<String>();
		if (getDoc() == null) return;
		int iPageCnt = getDoc().getPageCount();
		for (int i = 0; i < iPageCnt; i++) {
			String sPage = getDoc().getPageText(false, i);
			if (sPage == null) sPage = "";
			arrAllPages.add(sPage);
		}
		//getActivity().showToast("load page cnt " + iPageCnt);
	}

	void showCenterPopup(String val) {
		mActivity.showCenterPopup(surface, val, false);
	};

	private void showBottomPopup(String val) {
		mActivity.showBottomPopup(surface, val, false);
	};

	void showCenterPopup(String val, int millis, boolean bBrightness) {
		mActivity.showPopup(surface, val, millis, false, true, bBrightness);
	}

	private void showBottomPopup(String val, int millis, boolean bBrightness) {
		mActivity.showPopup(surface, val, millis, false, false, bBrightness);
	}

	void showCenterPopupFont(String val, String val2, int fontSize) {
		mActivity.showCenterPopupFont(surface, val, val2, fontSize);
	}

	public String getPageTextFromEngine(int page) {
		int iPageCnt = getDoc().getPageCount();
		if (page < iPageCnt) return StrUtils.getNonEmptyStr(getDoc().getPageText(false, page),false);
		return "";
	}

	public DocView getDoc() {
		return doc;
	}

	public DocView doc;

	// additional key codes for Nook
	public static final int NOOK_KEY_PREV_LEFT = 96;
	public static final int NOOK_KEY_PREV_RIGHT = 98;
	public static final int NOOK_KEY_NEXT_RIGHT = 97;
	public static final int NOOK_KEY_SHIFT_UP = 101;
	public static final int NOOK_KEY_SHIFT_DOWN = 100;

	// nook 1 & 2
	public static final int NOOK_12_KEY_NEXT_LEFT = 95;

	// Nook touch buttons
	public static final int KEYCODE_PAGE_BOTTOMLEFT = 0x5d; // fwd = 93 (
	//    public static final int KEYCODE_PAGE_BOTTOMRIGHT = 158; // 0x5f; // fwd = 95
	public static final int KEYCODE_PAGE_TOPLEFT = 0x5c; // back = 92
	public static final int KEYCODE_PAGE_TOPRIGHT = 0x5e; // back = 94

	public static final int SONY_DPAD_UP_SCANCODE = 105;
	public static final int SONY_DPAD_DOWN_SCANCODE = 106;
	public static final int SONY_DPAD_LEFT_SCANCODE = 125;
	public static final int SONY_DPAD_RIGHT_SCANCODE = 126;

	public static final int KEYCODE_ESCAPE = 111; // KeyEvent constant since API 11

	//    public static final int SONY_MENU_SCANCODE = 357;
//    public static final int SONY_BACK_SCANCODE = 158;
//    public static final int SONY_HOME_SCANCODE = 102;

	public static final int PAGE_ANIMATION_NONE = 0;
	public static final int PAGE_ANIMATION_PAPER = 1;
	public static final int PAGE_ANIMATION_SLIDE = 2;
	public static final int PAGE_ANIMATION_SLIDE2 = 3;
	public static final int PAGE_ANIMATION_BLUR = 4;
	public static final int PAGE_ANIMATION_DIM = 5;
	public static final int PAGE_ANIMATION_BLUR_DIM = 6;
	public static final int PAGE_ANIMATION_MAG = 7;
	public static final int PAGE_ANIMATION_MAG_DIM = 8;
	public static final int PAGE_ANIMATION_MAX = 8;

	public static final int SEL_CMD_SELECT_FIRST_SENTENCE_ON_PAGE = 1;
	public static final int SEL_CMD_NEXT_SENTENCE = 2;
	public static final int SEL_CMD_PREV_SENTENCE = 3;

	// Double tap selections within this radius are are assumed to be attempts to select a single point
	public static final int DOUBLE_TAP_RADIUS = 60;

	public final static int BRIGHTNESS_TYPE_LEFT_SIDE = 0;
	public final static int BRIGHTNESS_TYPE_RIGHT_SIDE = 1;
	public final static int BRIGHTNESS_TYPE_COLD_VAL = 2;
	public final static int BRIGHTNESS_TYPE_WARM_VAL = 3;

	//KR warning! we have our own implementation of this - mmaybe we'll switch to CR's - not sure
	/// Always sync this constants with crengine/include/lvdocview.h!
	/// Battery state: no battery
	public static final int BATTERY_STATE_NO_BATTERY = -2;
	/// Battery state: battery is charging
	public static final int BATTERY_STATE_CHARGING = -1;
	/// Battery state: battery is discharging
	public static final int BATTERY_STATE_DISCHARGING = -3;
	/// Battery charger connection: no connection
	public static final int BATTERY_CHARGER_NO = 1;
	/// Battery charger connection: AC adapter
	public static final int BATTERY_CHARGER_AC = 2;
	/// Battery charger connection: USB
	public static final int BATTERY_CHARGER_USB = 3;
	/// Battery charger connection: Wireless
	public static final int BATTERY_CHARGER_WIRELESS = 4;

	public ViewMode viewMode = ViewMode.PAGES;

	private void execute( Engine.EngineTask task )
	{
		mEngine.execute(task);
	}

	void post(Engine.EngineTask task)
	{
		mEngine.post(task);
	}

	public static class Sync<T> extends Object {
		private volatile T result = null;
		private volatile boolean completed = false;
		public void set(T res) {
			log.d("sync.set() called from " + Thread.currentThread().getName());
			result = res;
			completed = true;
			synchronized(this) {
				notify();
			}
			log.d("sync.set() returned from notify " + Thread.currentThread().getName());
		}
		public T get() {
			log.d("sync.get() called from " + Thread.currentThread().getName());
			while ( !completed ) {
				try {
					log.d("sync.get() before wait " + Thread.currentThread().getName());
					synchronized(this) {
						if (!completed)
							wait();
					}
					log.d("sync.get() after wait wait " + Thread.currentThread().getName());
				} catch (InterruptedException e) {
					log.d("sync.get() exception", e);
					// ignore
				} catch (Exception e) {
					log.d("sync.get() exception", e);
					// ignore
				}
			}
			log.d("sync.get() returning " + Thread.currentThread().getName());
			return result;
		}
	}

	private final CoolReader mActivity;
	public final Engine mEngine;
	public final EinkScreen mEinkScreen;

	public Selection lastSelection;
	public long lastDuration;
	public Bookmark hyplinkBookmark;

	public BookInfo mBookInfo;
	private Bookmark lastSavedToGdBookmark;
	public long lastBookId;
	public long lastTimePageTurn;
	public long maxPageReadInterval = 300000; // 5 mins
	public long lastCalendarSaveTime;
	public long calendarSaveInterval = 180000; // 3 mins
	public long curReadingTime;

	public Properties mSettings = new Properties();

	public int [] locationFindNext = new int[2];
	public int heightFindNext;
	public int widthFindNext;

	public Engine getEngine() {
		return mEngine;
	}

	public CoolReader getActivity() {
		return mActivity;
	}

	private int lastResizeTaskId = 0;

	public boolean isBookLoaded() {
		return mOpened;
	}

	public int getOrientation()	{
		int angle = mSettings.getInt(PROP_APP_SCREEN_ORIENTATION, 0);
		if (angle == 4)
			angle = mActivity.getOrientationFromSensor();
		return angle;
	}

	private int overrideKey(int keyCode) {
		return keyCode;
	}

	public int getTapZone(int x, int y, int dx, int dy) {
		int nonSensL = mActivity.settings().getInt(Settings.PROP_APP_TAP_ZONE_NON_SENS_LEFT, 0);
		nonSensL = nonSensL * dx / 2000;
		int nonSensR = mActivity.settings().getInt(Settings.PROP_APP_TAP_ZONE_NON_SENS_RIGHT, 0);
		nonSensR = nonSensR * dx / 2000;
		if (x < nonSensL) return 0;
		if (x > (dx - nonSensR)) return 0;
		int x1 = (dx - nonSensL - nonSensR) / 3;
		int x2 = (dx - nonSensL - nonSensR) * 2 / 3;
		int y1 = dy / 3;
		int y2 = dy * 2 / 3;
		int zone = 0;
		if (y < y1) {
			if (x < x1 + nonSensL)
				zone = 1;
			else if (x < x2 + nonSensL)
				zone = 2;
			else
				zone = 3;
		} else if (y < y2) {
			if (x < x1 + nonSensL)
				zone = 4;
			else if (x < x2 + nonSensL)
				zone = 5;
			else
				zone = 6;
		} else {
			if (x < x1 + nonSensL)
				zone = 7;
			else if (x < x2 + nonSensL)
				zone = 8;
			else
				zone = 9;
		}
		return zone;
	}

	public ReaderAction findTapZoneAction(int zone, int tapActionType) {
		ReaderAction action = ReaderAction.NONE;
		boolean isSecondaryAction = (secondaryTapActionType == tapActionType);
		if (tapActionType == TAP_ACTION_TYPE_SHORT) {
			action = ReaderAction.findForTap(zone, mSettings);
		} else {
			if (isSecondaryAction)
				action = ReaderAction.findForLongTap(zone, mSettings);
			else if (doubleTapSelectionEnabled || tapActionType == TAP_ACTION_TYPE_LONGPRESS)
				action = ReaderAction.START_SELECTION;
		}
		return action;
	}

	public FileInfo getOpenedFileInfo() {
		if (isBookLoaded() && mBookInfo != null)
			return mBookInfo.getFileInfo();
		return null;
	}

	public final int LONG_KEYPRESS_TIME = 900;
	public final int AUTOREPEAT_KEYPRESS_TIME = 700;
	public final int DOUBLE_CLICK_INTERVAL = 400;
	public final int PREVENT_CLICK_INTERVAL = 0;
	private ReaderAction currentDoubleClickAction = null;
	private ReaderAction currentSingleClickAction = null;
	private long currentDoubleClickActionStart = 0;
	private int currentDoubleClickActionKeyCode = 0;
//	boolean VOLUME_KEYS_ZOOM = false;

	//private boolean backKeyDownHere = false;


	private long statStartTime;
	private long statTimeElapsed;

	public void startStats() {
		if (statStartTime == 0) {
			statStartTime = android.os.SystemClock.uptimeMillis();
			log.d("stats: started reading");
		}
	}

	public void stopStats() {
		if (statStartTime > 0) {
			statTimeElapsed += android.os.SystemClock.uptimeMillis() - statStartTime;
			statStartTime = 0;
			log.d("stats: stopped reading");
		}
	}

	public long getTimeElapsed() {
		if (statStartTime > 0)
			return statTimeElapsed + android.os.SystemClock.uptimeMillis() - statStartTime;
		else
			return statTimeElapsed++;
	}

	public void setTimeElapsed(long timeElapsed) {
		statTimeElapsed = timeElapsed;
	}

	public void onAppPause() {
		stopTracking();
		if (isAutoScrollActive())
			stopAutoScroll();
		Bookmark bmk = getCurrentPositionBookmark();
		if (bmk != null)
			savePositionBookmark(bmk);
		if (!mAvgDrawAnimationStats.isEmpty())
			setSetting(PROP_APP_VIEW_ANIM_DURATION, String.valueOf(mAvgDrawAnimationStats.average()), false, true, false);
		log.i("calling bookView.onPause()");
		boolean bNeedSave = true;
		try {
			if (lastSavedToGdBookmark != null) {
				if (bmk.getStartPos() != null)
					if ((bmk.getStartPos().equals(lastSavedToGdBookmark.getStartPos()))) {
						bNeedSave = false;
					}
			}
			if (bNeedSave) {
				int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant > 0)
					CloudSync.saveJsonInfoFileOrCloud(((CoolReader) mActivity),
							CloudSync.CLOUD_SAVE_READING_POS, true, iSyncVariant == 1, true);
				lastSavedToGdBookmark = bmk;
			}
		} catch (Exception e) {
			log.i("onAppPause bookmark exception: " + e.getMessage());
		}
		bookView.onPause();
		appPaused = true;
	}

	private long lastAppResumeTs = 0;

	public void onAppResume() {
		lastAppResumeTs = System.currentTimeMillis();
		log.i("calling bookView.onResume()");
		bookView.onResume();
		appPaused = false;
	}

	private boolean startTrackingKey(KeyEvent event) {
		if (event.getRepeatCount() == 0) {
			stopTracking();
			trackedKeyEvent = event;
			return true;
		}
		return false;
	}

	private void stopTracking() {
		trackedKeyEvent = null;
		actionToRepeat = null;
		repeatActionActive = false;
		if (currentTapHandler != null)
			currentTapHandler.cancel();
	}

	private boolean isTracked(KeyEvent event) {
		if (trackedKeyEvent != null) {
			int tkeKc = trackedKeyEvent.getKeyCode();
			int eKc = event.getKeyCode();
			// check if tracked key and current key are the same
			if (tkeKc == eKc) {
				long tkeDt = trackedKeyEvent.getDownTime();
				long eDt = event.getDownTime();
				// empirical value (could be changed or moved to constant)
				long delta = 300l;
				// time difference between tracked and current event
				long diff = eDt - tkeDt;
				// needed for correct function on HTC Desire for CENTER_KEY
				if (delta > diff)
					return true;
			}
			else {
				log.v("isTracked( trackedKeyEvent=" + trackedKeyEvent + ", event=" + event + " )");
			}
		}
		stopTracking();
		return false;
	}

	private KeyEvent trackedKeyEvent = null;
	private ReaderAction actionToRepeat = null;
	private boolean repeatActionActive = false;
	private SparseArray<Long> keyDownTimestampMap = new SparseArray<Long>();

	public int translateKeyCode(int keyCode) {
		if (DeviceInfo.REVERT_LANDSCAPE_VOLUME_KEYS && (mActivity.getScreenOrientation() & 1) != 0) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				return KeyEvent.KEYCODE_VOLUME_UP;
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				return KeyEvent.KEYCODE_VOLUME_DOWN;
		}
		return keyCode;
	}

	private int nextUpdateId = 0;

	public boolean selectionStarted = false;
	public boolean needSwitchMode = false;

	void updateSelection(int startX, int startY, int endX, int endY, final boolean isUpdateEnd) {
		if (isUpdateEnd) {
			lastSelTime = System.currentTimeMillis();
			selectionStarted = false;
		} else {
			if (!selectionStarted) toggleScreenUpdateModeMode(true);
			selectionStarted = true;
		}
		final Selection sel = new Selection();
//		getActivity().showToast("upd: "+nextUpdateId+" "+isUpdateEnd);
//		log.v("upd: "+nextUpdateId+" "+isUpdateEnd);
		final int myId = ++nextUpdateId;
		sel.startX = startX;
		sel.startY = startY;
		sel.endX = endX;
		sel.endY = endY;
		final boolean selMode = selectionModeActive || inspectorModeActive;
		mEngine.execute(new Task() {
			@Override
			public void work() throws Exception {
				if (myId != nextUpdateId && !isUpdateEnd)
					return;
				doc.updateSelection(sel);
				if (!sel.isEmpty()) {
					invalidImages = true;
					BitmapInfo bi = preparePageImage(0);
					if (bi != null) {
						bookView.draw(true);
					}
				}
			}

			@Override
			public void done() {
				selectionStarted = false;
				if (isUpdateEnd) {
					String text = sel.text;
					if (text != null && text.length() > 0) {
						onSelectionComplete(sel, false, selMode);
					} else {
						clearSelection();
					}
				}
			}
		});
	}

	public static boolean isMultiSelection(Selection sel) {
		String str = sel.text;
		if(str != null){
			for(int i = 0; i < str.length(); i++){
				if(Character.isWhitespace(str.charAt(i))){
					return true;
				}
			}
		}
		return false;
	}

	public int mSelectionAction = SELECTION_ACTION_TOOLBAR;
	public int mSelectionActionLong = SELECTION_ACTION_TOOLBAR;
	public int mBookmarkActionSendTo = SEND_TO_ACTION_NONE;
	public int mMultiSelectionAction = SELECTION_ACTION_TOOLBAR;
	public int mSelection2Action = SELECTION_ACTION_SAME_AS_COMMON;
	public int mSelection2ActionLong = SELECTION_ACTION_SAME_AS_COMMON;
	public int mMultiSelection2Action = SELECTION_ACTION_SAME_AS_COMMON;
	public int mSelection3Action = SELECTION_ACTION_SAME_AS_COMMON;
	public int mSelection3ActionLong = SELECTION_ACTION_SAME_AS_COMMON;
	public int mMultiSelection3Action = SELECTION_ACTION_SAME_AS_COMMON;

	private void showDic(Selection sel, boolean fullScreen, boolean bSkipDic, Dictionaries.DictInfo dict) {
		getActivity().mDictionaries.setAdHocDict(dict);
		if ((!isMultiSelection(sel))&&(mActivity.ismDictWordCorrrection())) {
			if (!bSkipDic)
				mActivity.findInDictionary(StrUtils.dictWordCorrection(sel.text), fullScreen, null);
		} else {
			if (
					((!isMultiSelection(sel))&&(!bSkipDic))
							||
							(isMultiSelection(sel))
			)
				mActivity.findInDictionary(sel.text, fullScreen, null);
		}
		if (!getSettings().getBool(PROP_APP_SELECTION_PERSIST, false))
			clearSelection();
	}

	private void onSelectionComplete(Selection sel, boolean fullScreen, boolean selMode) {
		//mActivity.showToast("startPos: "+sel.startPos+"; endPos: "+sel.endPos+
		//		"startX: "+sel.startX+"; startY: "+sel.startY+"; chapter: "+sel.chapter);
		boolean bSkipDic = mActivity.skipFindInDic;
		mActivity.skipFindInDic = false;
		lastSelection = sel;
		int shortAction = mSelectionAction;
		if ((selectionModeWasActive || selMode) && (mSelection2Action != SELECTION_ACTION_SAME_AS_COMMON)) shortAction = mSelection2Action;
		if ((inspectorModeActive) && (mSelection3Action != SELECTION_ACTION_SAME_AS_COMMON)) shortAction = mSelection3Action;
		int longAction = mSelectionActionLong;
		if ((selectionModeWasActive || selMode) && (mSelection2ActionLong != SELECTION_ACTION_SAME_AS_COMMON)) longAction = mSelection2ActionLong;
		if ((inspectorModeActive) && (mSelection3ActionLong != SELECTION_ACTION_SAME_AS_COMMON)) longAction = mSelection3ActionLong;
		int multiAction = mMultiSelectionAction;
		if ((selectionModeWasActive || selMode) && (mMultiSelection2Action != SELECTION_ACTION_SAME_AS_COMMON)) multiAction = mMultiSelection2Action;
		if ((inspectorModeActive) && (mMultiSelection3Action != SELECTION_ACTION_SAME_AS_COMMON)) multiAction = mMultiSelection3Action;
		int iSelectionAction;
		int iSelectionAction1 = (lastDuration > getDoubleClickInterval()) ? longAction : shortAction;
		iSelectionAction = isMultiSelection(sel) ? multiAction : iSelectionAction1;
		//if (selMode) iSelectionAction = mMultiSelectionAction;
		switch (iSelectionAction) {
			case SELECTION_ACTION_TOOLBAR:
				SelectionToolbarDlg.showDialog(mActivity, ReaderView.this, sel);
				break;
			case SELECTION_ACTION_TOOLBAR_SHORT:
				SelectionToolbarDlg.showDialogShort(mActivity, ReaderView.this, sel);
				break;
			case SELECTION_ACTION_COPY:
				copyToClipboardAndToast(sel.text);
				clearSelection();
				break;
			case SELECTION_ACTION_DICTIONARY:
				if ((!isMultiSelection(sel))&&(mActivity.ismDictWordCorrrection())) {
					if (!bSkipDic)
						mActivity.findInDictionary(StrUtils.dictWordCorrection(sel.text), fullScreen, null);
				} else {
					if (
							((!isMultiSelection(sel))&&(!bSkipDic))
									||
									(isMultiSelection(sel))
					)
						mActivity.findInDictionary(sel.text, fullScreen, null);
				}
				if (!getSettings().getBool(PROP_APP_SELECTION_PERSIST, false))
					clearSelection();
				break;
			case SELECTION_ACTION_DICTIONARY_1:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary);
				break;
			case SELECTION_ACTION_DICTIONARY_2:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary2);
				break;
			case SELECTION_ACTION_DICTIONARY_3:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary3);
				break;
			case SELECTION_ACTION_DICTIONARY_4:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary4);
				break;
			case SELECTION_ACTION_DICTIONARY_5:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary5);
				break;
			case SELECTION_ACTION_DICTIONARY_6:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary6);
				break;
			case SELECTION_ACTION_DICTIONARY_7:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary7);
				break;
			case SELECTION_ACTION_DICTIONARY_8:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary8);
				break;
			case SELECTION_ACTION_DICTIONARY_9:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary9);
				break;
			case SELECTION_ACTION_DICTIONARY_10:
				showDic(sel, fullScreen, bSkipDic, getActivity().mDictionaries.currentDictionary10);
				break;
			case SELECTION_ACTION_BOOKMARK:
				clearSelection();
				showNewBookmarkDialog( sel, Bookmark.TYPE_COMMENT, "" );
				break;
			case SELECTION_ACTION_BOOKMARK_QUICK:
				if (sel != null) {
					clearSelection();
					Bookmark bmk = new Bookmark();
					bmk.setType(Bookmark.TYPE_COMMENT);
					bmk.setPosText(sel.text);
					bmk.setStartPos(sel.startPos);
					bmk.setEndPos(sel.endPos);
					bmk.setPercent(sel.percent);
					bmk.setTitleText(sel.chapter);
					bmk.setIsCustomColor(0);
					bmk.setCustomColor(Utils.colorToHex(0));
					bmk.setShortContext(BookmarkEditDialog.getContextText(mActivity, sel.text));
					bmk.setFullContext(BookmarkEditDialog.getFullContextText(mActivity));
					addBookmark(bmk);
				}
				break;
			case SELECTION_ACTION_FIND:
				clearSelection();
				showSearchDialog(sel.text);
				break;
			case SELECTION_ACTION_SEARCH_WEB:
				final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
				emailIntent.putExtra(SearchManager.QUERY, sel.text.trim());
				mActivity.startActivity(emailIntent);
				break;
			case SELECTION_ACTION_SEND_TO:
				sendQuotationInEmail(sel);
				break;
			case SELECTION_ACTION_USER_DIC:
				if ((!FlavourConstants.PRO_FEATURES)&&(!FlavourConstants.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				showNewBookmarkDialog(sel, Bookmark.TYPE_USER_DIC, "");
				break;
			case SELECTION_ACTION_CITATION:
				if ((!FlavourConstants.PRO_FEATURES)&&(!FlavourConstants.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				showNewBookmarkDialog(sel, Bookmark.TYPE_CITATION, "");
				break;
			case SELECTION_ACTION_DICTIONARY_LIST:
				DictsDlg dlg = new DictsDlg(mActivity, this, sel.text, null, false);
				dlg.show();
				if (!getSettings().getBool(PROP_APP_SELECTION_PERSIST, false))
					clearSelection();
				break;
			case SELECTION_ACTION_COMBO:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					return;
				}
				Dictionaries.DictInfo di = mActivity.getCurOrFirstOnlineDic();
				if (di != null) {
					if (!StrUtils.isEmptyStr(sel.text)) {
						mActivity.mDictionaries.setAdHocDict(di);
						mActivity.findInDictionary(sel.text, fullScreen,null, new CoolReader.DictionaryCallback() {

							@Override
							public boolean showDicToast() {
								return true;
							}

							@Override
							public boolean saveToHist() {
								return false;
							}

							@Override
							public void done(String result, String dslStruct) {
								SelectionToolbarDlg.saveUserDic(false, result, sel, mActivity, ReaderView.this, dslStruct);
							}

							@Override
							public void fail(Exception e, String msg) {
								mActivity.showToast(msg);
							}
						});
					}
				}
				break;
			case SELECTION_ACTION_SUPER_COMBO:
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					return;
				}
				di = mActivity.getCurOrFirstOnlineDic();
				if (di != null) {
					if (!StrUtils.isEmptyStr(sel.text)) {
						mActivity.mDictionaries.setAdHocDict(di);
						mActivity.findInDictionary(sel.text, false, null, new CoolReader.DictionaryCallback() {

							@Override
							public boolean showDicToast() {
								return false;
							}

							@Override
							public boolean saveToHist() {
								return false;
							}

							@Override
							public void done(String result, String dslStruct) {
								SelectionToolbarDlg.saveUserDic(false, result, sel, mActivity, ReaderView.this, dslStruct);
								SelectionToolbarDlg.sendTo2(result, sel, mActivity);
							}

							@Override
							public void fail(Exception e, String msg) {
								mActivity.showToast(msg);
							}
						});
					}
				}
				break;

			default:
				clearSelection();
				break;
		}

	}

	public void showNewBookmarkDialog(Selection sel, int chosenType, String commentText) {
		if (mBookInfo == null)
			return;
		Bookmark bmk = new Bookmark();
		bmk.setType(Bookmark.TYPE_COMMENT);
		bmk.setPosText(sel.text);
		bmk.setStartPos(sel.startPos);
		bmk.setEndPos(sel.endPos);
		bmk.setPercent(sel.percent);
		bmk.setTitleText(sel.chapter);
		BookmarkEditDialog dlg = new BookmarkEditDialog(mActivity, this, bmk, true, chosenType, commentText);
		dlg.show();
	}

	public void sendQuotationInEmail(Selection sel) {
		String s = Utils.getBookInfoToSend(sel);
		//mActivity.sendBookFragment(mBookInfo, s + "\n" + sel.text + "\n");
		mActivity.sendBookFragment(mBookInfo, s, sel.text);
	}

	public void copyToClipboard(String text) {
		if (text != null && text.length() > 0) {
			ClipboardManager cm = mActivity.getClipboardmanager();
			cm.setText(text);
			log.i("Setting clipboard text: " + text);
			//mActivity.showToast(mActivity.getString(R.string.copied_to_cb));
		}
	}

	public void copyToClipboardAndToast(String text) {
		if (text != null && text.length() > 0) {
			ClipboardManager cm = mActivity.getClipboardmanager();
			cm.setText(text);
			log.i("Setting clipboard text: " + text);
			mActivity.showToast(mActivity.getString(R.string.copied_to_cb));
		}
	}

//	private void cancelSelection() {
//		//
//		selectionInProgress = false;
//		clearSelection();
//	}

	public int isBacklightControlFlick = 1;
	private boolean backlightFixDelta = false;

	private boolean isTouchScreenEnabled = true;
	//	private boolean isManualScrollActive = false;
//	private boolean isBrightnessControlActive = false;
//	private int manualScrollStartPosX = -1;
//	private int manualScrollStartPosY = -1;
//	volatile private boolean touchEventIgnoreNextUp = false;
//	volatile private int longTouchId = 0;
//	volatile private long currentDoubleTapActionStart = 0;
//	private boolean selectionInProgress = false;
//	private int selectionStartX = 0;
//	private int selectionStartY = 0;
//	private int selectionEndX = 0;
//	private int selectionEndY = 0;
	public boolean doubleTapSelectionEnabled = false;
	public int mBounceTapInterval = 150;
	public int mGesturePageFlipSwipeN;
	public int mGesturePageFlipSensivity;
	public int mGesturePageFlipPageCount;
	private boolean mIsPageMode;
	public boolean mDisableTwoPointerGestures;
	private int secondaryTapActionType = TAP_ACTION_TYPE_LONGPRESS;
	public boolean selectionModeActive = false;
	public boolean selectionModeWasActive = false;
	public boolean inspectorModeActive = false;
	public long lastTimeTap = 0L;
	private long lastTimeKey = 0L;

	public void toggleScreenUpdateModeMode(boolean force) {
		if (!mActivity.mOnyxSwitchToA2) return;
		if ((force) || (selectionModeActive) || (inspectorModeActive) || (SelectionToolbarDlg.isVisibleNow)) {
			mEinkScreen.setupController(EinkScreen.EinkUpdateMode.FastA2, 999, surface, true, true);
		}
		else {
			updMode = EinkScreen.EinkUpdateMode.byCode(mActivity.settings().getInt(PROP_APP_SCREEN_UPDATE_MODE, EinkScreen.EinkUpdateMode.Normal.code));
			updInterval = mActivity.settings().getInt(PROP_APP_SCREEN_UPDATE_INTERVAL, 10);
			mEinkScreen.setupController(updMode, updInterval, surface, false, true);
		}
	}

	public void toggleSelectionMode() {
		selectionModeActive = !selectionModeActive;
		inspectorModeActive = false;
		toggleScreenUpdateModeMode(false);
		mActivity.updateSavingMark(
						mActivity.getString(selectionModeActive ?
								R.string.action_toggle_selection_mode_on : R.string.action_toggle_selection_mode_off));
		bookView.draw(false);
	}

	public void toggleInspectorMode() {
		inspectorModeActive = !inspectorModeActive;
		selectionModeActive = false;
		toggleScreenUpdateModeMode(false);
		mActivity.updateSavingMark(
			mActivity.getString(inspectorModeActive ?
				R.string.action_toggle_inspector_mode_on : R.string.action_toggle_inspector_mode_off));
		bookView.draw(false);
	}

	public ImageViewer currentImageViewer;

	void startImageViewer(ImageInfo image) {
		currentImageViewer = new ImageViewer(mActivity, ReaderView.this, image);
		Properties settings = getSettings();
		boolean custCol = settings.getBool(Settings.PROP_IMG_CUSTOM_BACKGROUND, false);
		if (custCol) {
			int col = settings.getColor(Settings.PROP_IMG_CUSTOM_BACKGROUND_COLOR, Color.BLACK);
			int colBg = settings.getColor(Settings.PROP_BACKGROUND_COLOR, Color.BLACK);
			String tx = settings.getProperty(Settings.PROP_PAGE_BACKGROUND_IMAGE, "(NONE)");
			settings.setColor(PROP_BACKGROUND_COLOR, col);
			settings.setColor(PROP_BACKGROUND_COLOR_SAVE, colBg);
			settings.setProperty(PROP_PAGE_BACKGROUND_IMAGE_SAVE, tx);
			settings.setProperty(PROP_PAGE_BACKGROUND_IMAGE, "(NONE)");
			settings.setBool(PROP_BACKGROUND_COLOR_SAVE_WAS, true);
			mActivity.setSettings(settings, 2000, true);
		}
		drawPage();
		if (mActivity.getmReaderFrame() != null)
			BackgroundThread.instance().postGUI(() -> {
				mActivity.getmReaderFrame().updateCRToolbar(mActivity);
			}, 300);
	}

	private boolean isImageViewMode() {
		return currentImageViewer != null;
	}

	private void stopImageViewer() {
		if (currentImageViewer != null)
			currentImageViewer.close();
	}

	public boolean disableTouch = false;

	public TapHandler currentTapHandler = null;
	public long firstTapTimeStamp;

	public void showTOC()
	{
		BackgroundThread.ensureGUI();
		final ReaderView view = this;
		mEngine.post(new Task() {
			TOCItem toc;
			PositionProperties pos;
			public void work() {
				BackgroundThread.ensureBackground();
				toc = doc.getTOC();
				pos = doc.getPositionProps(null, false);
			}
			public void done() {
				BackgroundThread.ensureGUI();
				if (toc != null && pos != null) {
					TOCDlg dlg = new TOCDlg(mActivity, view, toc, pos.pageNumber);
					dlg.show();
				} else {
					mActivity.showToast("No Table of Contents found");
				}
			}
		});
	}

	public void showSearchDialog(String initialText)
	{
		if (initialText != null && initialText.length() > 40)
			initialText = initialText.substring(0, 40);
		BackgroundThread.ensureGUI();
		this.getSurface().getLocationOnScreen(locationFindNext);
		heightFindNext = this.getSurface().getHeight();
		widthFindNext = this.getSurface().getWidth();
		SearchDlg dlg = new SearchDlg( mActivity, this, initialText);
		dlg.show();
	}

	public void findText(final Bookmark fromPage, final String pattern, final boolean reverse, final boolean caseInsensitive) {
		findText(fromPage, pattern, reverse, caseInsensitive, false);
	}

	public void findText(final Bookmark fromPage, final String pattern, final boolean reverse, final boolean caseInsensitive, final boolean forUserDic)
	{
		BackgroundThread.ensureGUI();
		final ReaderView view = this;
		mEngine.execute(new Task() {
			public void work() throws Exception {
				BackgroundThread.ensureBackground();
				boolean res = doc.findText( pattern, forUserDic?0:1, reverse?1:0, caseInsensitive?1:0);
				if (!forUserDic) {
					if (!res)
						res = doc.findText(pattern, -1, reverse ? 1 : 0, caseInsensitive ? 1 : 0);
					if (!res) {
						doc.clearSelection();
						throw new Exception("pattern not found");
					}
				}
			}
			public void done() {
				BackgroundThread.ensureGUI();
				drawPage();
				if (!forUserDic)
					FindNextDlg.showDialog(fromPage, mActivity, view, pattern, caseInsensitive, false);
			}
			public void fail(Exception e) {
				if (!forUserDic) {
					BackgroundThread.ensureGUI();
					mActivity.showToast(R.string.pattern_not_found);
				}
			}

		});
	}

	public void findNext(final String pattern, final boolean reverse, final boolean caseInsensitive)
	{
		BackgroundThread.ensureGUI();
		mEngine.execute(new Task() {
			public void work() throws Exception {
				BackgroundThread.ensureBackground();
				boolean res = doc.findText( pattern, 1, reverse?1:0, caseInsensitive?1:0);
				if (!res)
					res = doc.findText( pattern, -1, reverse?1:0, caseInsensitive?1:0);
				if (!res) {
					doc.clearSelection();
					throw new Exception("pattern not found");
				}
			}
			public void done() {
				BackgroundThread.ensureGUI();
				drawPage(true);
			}
		});
	}

	private boolean flgHighlightBookmarks = false;
	public boolean flgHighlightUserDic = true;

	public void clearSelection()
	{
		clearSelection(true, true);
	}

	public void clearSelection(boolean doToggleScreen, boolean doRedrawPage)
	{
		BackgroundThread.ensureGUI();
		if (doToggleScreen)
			toggleScreenUpdateModeMode(false);
		if (mBookInfo == null || !isBookLoaded())
			return;
		mEngine.post(new Task() {
			public void work() throws Exception {
				doc.clearSelection();
				invalidImages = true;
			}
			public void done() {
				if (surface.isShown())
					if (doRedrawPage) drawPage(true);
			}
		});
	}

	public void highlightBookmarks(boolean doRedraw) {
		BackgroundThread.ensureGUI();
		if (mBookInfo == null || !isBookLoaded())
			return;
		int count = mBookInfo.getBookmarkCount();
		final Bookmark[] list = (count > 0 && flgHighlightBookmarks) ? new Bookmark[count] : null;
		for (int i=0; i<count && flgHighlightBookmarks; i++)
			list[i] = mBookInfo.getBookmark(i);
		mEngine.post(new Task() {
			public void work() throws Exception {
				doc.hilightBookmarks(list);
				invalidImages = true;
			}
			public void done() {
				if (surface.isShown())
					if (doRedraw)
						drawPage(true);
			}
		});
	}

	public void goToBookmark(Bookmark bm) {
		BackgroundThread.ensureGUI();
		final String pos = bm.getStartPos();
		mEngine.execute(new Task() {
			public void work() {
				BackgroundThread.ensureBackground();
				doc.goToPosition(pos, true);
			}
			public void done() {
				BackgroundThread.ensureGUI();
				drawPage();
			}
		});
	}

	public boolean goToBookmark(final int shortcut) {
		BackgroundThread.ensureGUI();
		if (mBookInfo != null) {
			Bookmark bm = mBookInfo.findShortcutBookmark(shortcut);
			if (bm == null) {
				addBookmark(shortcut);
				return true;
			} else {
				// go to bookmark
				goToBookmark( bm );
				return false;
			}
		}
		return false;
	}

	public Bookmark removeBookmark(final Bookmark bookmark) {
		Bookmark removed = mBookInfo.removeBookmark(bookmark);
		if (removed != null) {
			if (removed.getId() != null) {
				mActivity.getDB().deleteBookmark(removed);
			}
			highlightBookmarks(true);
		}
		return removed;
	}

	public Bookmark updateBookmark(final Bookmark bookmark) {
		Bookmark bm = mBookInfo.updateBookmark(bookmark);
		if (bm != null) {
			scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			highlightBookmarks(true);
		}
		return bm;
	}

	public void addBookmark(final Bookmark bookmark) {
		mBookInfo.addBookmark(bookmark);
		highlightBookmarks(true);
		scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
	}

	public void addBookmark(final int shortcut) {
		addBookmark(shortcut, false);
	}

	public void addBookmark(final int shortcut, boolean isQuick) {
		BackgroundThread.ensureGUI();
		// set bookmark instead
		mEngine.execute(new Task() {
			Bookmark bm;
			public void work() {
				BackgroundThread.ensureBackground();
				if (mBookInfo != null) {
					bm = doc.getCurrentPageBookmark();
					bm.setShortcut(shortcut);
				}
			}
			public void done() {
				if (mBookInfo != null && bm != null) {
					if (shortcut == 0)
						mBookInfo.addBookmark(bm);
					else
						mBookInfo.setShortcutBookmark(shortcut, bm);
					mActivity.getDB().saveBookInfo(mBookInfo);
					String s;
					if (shortcut == 0)
						s = mActivity.getString(R.string.toast_position_bookmark_is_set);
					else {
						s = mActivity.getString(R.string.toast_shortcut_bookmark_is_set);
						s.replace("$1", String.valueOf(shortcut));
					}
					if (!isQuick) highlightBookmarks(true);
					mActivity.showToast(s);
					scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
				}
			}
		});
	}

	public boolean onMenuItem(final int itemId) {
		BackgroundThread.ensureGUI();
		ReaderAction action = ReaderAction.findByMenuId(itemId);
		if (action.isNone())
			return false;
		onAction(action);
		return true;
	}

	public void onAction(final ReaderAction action)	{
		onAction(action, null);
	}

	public void onAction(final ReaderAction action, final Runnable onFinishHandler)	{
		BackgroundThread.ensureGUI();
		if (action.cmd != ReaderCommand.DCMD_NONE)
			onCommand(action.cmd, action.param, action, onFinishHandler);
	}

	public void saveCurrentBookToOnyxLib() {
		if (mActivity.mOnyxDontUpdateLibrary) return;
		try {
			Bookmark bmk = getCurrentPositionBookmark();
			boolean res = OnyxLibrary.saveBookInfo(mActivity, bmk, getBookInfo());
		} catch (Exception e) {
			//do nothing
		}
	}

	public void toggleDayNightMode() {
//		StarDictDlg sdd = new StarDictDlg(mActivity);
//		sdd.show();
//		EpdController.repaintEveryThing(UpdateMode.GC);
//		Bookmark bmk =  getCurrentPositionBookmark();
//		mActivity.showToast(bmk.getPosText());
//		boolean res = OnyxLibrary.saveBookInfo(mActivity, bmk, getBookInfo());
//		mActivity.showToast("res: " + res);
//		if (1==1) return;
		Properties settings = getSettings();
		OptionsDialog.toggleDayNightMode(settings);
		//setSettings(settings, mActivity.settings());
		mActivity.setSettings(settings, 2000, true);
		invalidImages = true;
		if (getBookInfo() != null)
			if (getBookInfo().getFileInfo() != null)
				if (getBookInfo().getFileInfo().askedShownStylesInfo<3) {
					getBookInfo().getFileInfo().askedShownStylesInfo++;
					checkOpenBookStyles(true, true);
				}
		if (mActivity.getmReaderFrame() != null)
			mActivity.getmReaderFrame().updateCRToolbar(((CoolReader) mActivity));
		mActivity.applyFullscreen(mActivity.getWindow());
//		if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
//			mActivity.setCurrentTheme(mActivity.getEinkThemeName());
	}

	public boolean isNightMode() {
		return mSettings.getBool(PROP_NIGHT_MODE, false);
	}

	public String getSetting(String name) {
		return mSettings.getProperty(name);
	}

	public void setSetting(String name, String value, boolean invalidateImages, boolean save, boolean apply) {
		mActivity.setSetting(name, value, apply);
		invalidImages = true;
	}

	public void setSetting(String name, String value) {
		setSetting(name, value, true, false, true);
	}

	public void setViewModeNonPermanent(ViewMode mode) {
		if (mode != viewMode) {
			if (mode == ViewMode.SCROLL) {
				doc.doCommand(ReaderCommand.DCMD_TOGGLE_PAGE_SCROLL_VIEW.nativeId, 0);
				viewMode = mode;
				mIsPageMode = false;
			} else {
				doc.doCommand(ReaderCommand.DCMD_TOGGLE_PAGE_SCROLL_VIEW.nativeId, 0);
				viewMode = mode;
				mIsPageMode = true;
			}
		}
	}

	public void saveSetting(String name, String value) {
		bNeedRedrawOnce = true;
		//	mActivity.showToast("bNeedRedrawOnce");
		setSetting(name, value, true, true, true);
	}

	public void toggleScreenOrientation() {
		int orientation = mActivity.getScreenOrientation();
		orientation = ( orientation==0 )? 1 : 0;
		saveSetting(PROP_APP_SCREEN_ORIENTATION, String.valueOf(orientation));
		bNeedRedrawOnce = true;
		//	mActivity.showToast("bNeedRedrawOnce");
		mActivity.setScreenOrientation(orientation);
	}

	public void toggleFullscreen() {
		//mEinkScreen.setupController(EinkScreen.EinkUpdateMode.A2, 999, surface);
		boolean newBool = !mActivity.isFullscreen();
		String newValue = newBool ? "1" : "0";
		saveSetting(PROP_APP_FULLSCREEN, newValue);
		bNeedRedrawOnce = true;
		//	mActivity.showToast("bNeedRedrawOnce");
		mActivity.setFullscreen(newBool);
		boolean bDontAsk = mActivity.settings().getBool(Settings.PROP_APP_HIDE_CSS_WARNING, false);
		int showUD = mActivity.settings().getInt(Settings.PROP_APP_SHOW_USER_DIC_PANEL, 0);
		if ((!bDontAsk) && (showUD == 0))
			BackgroundThread.instance().postGUI(() -> {
				ArrayList<String> sButtons = new ArrayList<String>();
				int efMode = mActivity.settings().getInt(Settings.PROP_EXT_FULLSCREEN_MARGIN, 0);
				if ((newBool) &&(efMode != 0))
					sButtons.add(mActivity.getString(R.string.ext_fullscreen_margin_text_off));
				if ((newBool) &&(efMode != 1))
					sButtons.add(mActivity.getString(R.string.ext_fullscreen_margin_text1));
				if ((newBool) &&(efMode != 2))
					sButtons.add(mActivity.getString(R.string.ext_fullscreen_margin_text2));
				sButtons.add(mActivity.getString(R.string.options_page_titlebar_new));
				SomeButtonsToolbarDlg.showDialog(mActivity, ReaderView.this.getSurface(), 5,true,
						"",
						sButtons, null, new SomeButtonsToolbarDlg.ButtonPressedCallback() {
							@Override
							public void done(Object o, String btnPressed) {
								if (btnPressed.equals(mActivity.getString(R.string.ext_fullscreen_margin_text_off))) {
									mActivity.setSetting(PROP_EXT_FULLSCREEN_MARGIN, "0", true);
								}
								if (btnPressed.equals(mActivity.getString(R.string.ext_fullscreen_margin_text1))) {
									mActivity.setSetting(PROP_EXT_FULLSCREEN_MARGIN, "1", true);
								}
								if (btnPressed.equals(mActivity.getString(R.string.ext_fullscreen_margin_text2))) {
									mActivity.setSetting(PROP_EXT_FULLSCREEN_MARGIN, "2", true);
								}
								if (btnPressed.equals(mActivity.getString(R.string.options_page_titlebar_new))) {
									mActivity.optionsFilter = "";
									mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_APP_TITLEBAR_NEW);
								}
							}
						});
			}, 200);
	}

	private void initTapZone(final BaseDialog dlg, View view, final int tapZoneId) {
		if (view == null)
			return;
		final TextView text = view.findViewById(R.id.tap_zone_action_text_short);
		final TextView longtext = view.findViewById(R.id.tap_zone_action_text_long);
		final ImageView iv = view.findViewById(R.id.zone_icon);
		final String propName = PROP_APP_TAP_ZONE_ACTIONS_TAP + "." + tapZoneId;
		final String longPropName = PROP_APP_TAP_ZONE_ACTIONS_TAP + ".long." + tapZoneId;
		Properties properties = new Properties(mActivity.settings());
		final ReaderAction action = ReaderAction.findById(properties.getProperty(propName));
		if ((iv != null)&&(action != null)) {
			int iconId = action.iconId;
			if (iconId == 0) {
				iconId = Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
			}
			Drawable d = mActivity.getResources().getDrawable(action.getIconIdWithDef(mActivity));
			iv.setImageDrawable(d);
		}
		final ReaderAction longAction = ReaderAction.findById(properties.getProperty(longPropName));
		final ImageView ivl = view.findViewById(R.id.zone_icon_long);
		if ((ivl != null)&&(longAction != null)) {
			int iconId = longAction.iconId;
			if (iconId == 0) {
				iconId = Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
			}
			Drawable d = mActivity.getResources().getDrawable(longAction.getIconIdWithDef(mActivity));
			ivl.setImageDrawable(d);
		}
		text.setText(action.getNameText(mActivity));
		longtext.setText(longAction.getNameText(mActivity));

		int colorIcon;
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorIcon});
		colorIcon = a.getColor(0, Color.GRAY);
		a.recycle();
		text.setTextColor(mActivity.getTextColor(colorIcon));
		longtext.setTextColor(mActivity.getTextColor(colorIcon));

		view.setOnClickListener(v -> dlg.cancel());
	}

	public void showReadingPositionPopup() {
		if (mBookInfo==null)
			return;
		final StringBuilder buf = new StringBuilder();
//		if (mActivity.isFullscreen()) {
		buf.append( Utils.formatTime(mActivity, System.currentTimeMillis()) +  " ");
		buf.append(" [" + mBatteryChargeLevel + "%]; ");
//		}
		execute( new Task() {
			Bookmark bm;
			@Override
			public void work() {
				bm = doc.getCurrentPageBookmark();
				if (bm != null) {
					PositionProperties prop = doc.getPositionProps(bm.getStartPos(), true);
					if (prop.pageMode != 0) {
						buf.append("" + (prop.pageNumber+1) + " / " + prop.pageCount + "   ");
					}
					int percent = (int)(10000 * (long)prop.y / prop.fullHeight);
					buf.append("" + (percent/100) + "." + (percent%100) + "%" );

					// Show chapter details if book has more than one chapter
					TOCItem toc = doc.getTOC();
					if (toc != null && toc.getChildCount() > 1) {
						TOCItem chapter = toc.getChapterAtPage(prop.pageNumber);

						String chapterName = chapter.getName();
						if (chapterName!=null && chapterName.length()>30)
							chapterName = chapterName.substring(0, 30) + "...";

						TOCItem nextChapter = chapter.getNextChapter();
						int iChapterEnd = (nextChapter != null) ? nextChapter.getPage() : prop.pageCount;

						String chapterPos = null;
						if (prop.pageMode != 0) {
							int iChapterStart = chapter.getPage();
							int iChapterLen = iChapterEnd - iChapterStart;
							int iChapterPage = prop.pageNumber - iChapterStart + 1;

							chapterPos = "  (" + iChapterPage + " / " + iChapterLen + ")";
						}

						if (chapterName != null && chapterName.length() > 0)
							buf.append("\n" + chapterName);
						if (chapterPos != null && chapterPos.length() > 0)
							buf.append(chapterPos);

						File f = mActivity.getSettingsFileF(mActivity.getCurrentProfile());
						String sF = f.getAbsolutePath();
						sF = sF.replace("/storage/","/s/").replace("/emulated/","/e/");
						String sprof = mActivity.getCurrentProfileName();
						if (!StrUtils.isEmptyStr(sprof)) sprof = sprof + " - ";
						buf.append("\n" + mActivity.getString(R.string.settings_profile)+": "+sprof + sF);
					}
					mActivity.showToast(buf.toString());
				}
			}
			public void done() {
				BackgroundThread.instance().executeGUI((Runnable) () -> {
					BaseDialog dlg = new BaseDialog("ReadingPositionPopup",
							mActivity, "", false, false);
					dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
					//ColorDrawable c = new ColorDrawable(android.graphics.Color.TRANSPARENT);
					final String[] mFontFaces = Engine.getFontFaceList();
					LayoutInflater li = LayoutInflater.from(mActivity.getApplicationContext());
					View grid = (View)li.inflate(R.layout.options_tap_zone_grid_show, null);
					int colorGray;
					int colorGrayC;
					int colorIcon;
					TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
							{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
					colorGray = a.getColor(0, Color.GRAY);
					colorGrayC = a.getColor(1, Color.GRAY);
					colorIcon = a.getColor(2, Color.GRAY);
					a.recycle();
					ColorDrawable c = new ColorDrawable(colorGrayC);
					c.setAlpha(220);
					dlg.getWindow().setBackgroundDrawable(c);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell1), 1);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell2), 2);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell3), 3);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell4), 4);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell5), 5);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell6), 6);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell7), 7);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell8), 8);
					initTapZone(dlg, grid.findViewById(R.id.tap_zone_grid_cell9), 9);
					mActivity.tintViewIcons(grid,true);
					TextView tvLabel = (TextView) grid.findViewById(R.id.lbl_selection_text);
					tvLabel.setText(buf);
					tvLabel.setTextColor(mActivity.getTextColor(colorIcon));
					final BaseDialog dlg1 = dlg;
					grid.findViewById(R.id.lay_bottom_text).setOnClickListener(v -> dlg1.cancel());
					if (isBacklightControlFlick == BACKLIGHT_CONTROL_FLICK_NONE) {
						((ImageButton) grid.findViewById(R.id.tap_zone_show_btn_left)).setImageDrawable(null);
						((ImageButton) grid.findViewById(R.id.tap_zone_show_btn_right)).setImageDrawable(null);
					}
					//TODO: настройки яркости я все равно хочу менять, тут надо будет это пересмотреть
					if ((isBacklightControlFlick == BACKLIGHT_CONTROL_FLICK_LEFT) || (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT)) {
						((ImageButton) grid.findViewById(R.id.tap_zone_show_btn_right)).setImageDrawable(null);
					}
					if ((isBacklightControlFlick == BACKLIGHT_CONTROL_FLICK_RIGHT) || (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT)) {
						((ImageButton) grid.findViewById(R.id.tap_zone_show_btn_left)).setImageDrawable(null);
					}
					dlg.setView(grid);
					dlg.show();
				});
				//mActivity.showToast(buf.toString());
			}
		});
	}

	public void toggleTitlebar() {
		String sPos = getSetting(PROP_STATUS_LOCATION);
		if (sPos.equals("0")) sPos = "3";
		else if (sPos.equals("3")) sPos = "4";
		else sPos = "0";
		mActivity.setSetting(PROP_STATUS_LOCATION, sPos, true);
	}

	public void toggleDocumentStyles() {
		if (mOpened && mBookInfo != null) {
			log.d("toggleDocumentStyles()");
			boolean disableInternalStyles = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
			disableInternalStyles = !disableInternalStyles;
			mBookInfo.getFileInfo().setFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG, disableInternalStyles);
			doEngineCommand(ReaderCommand.DCMD_SET_INTERNAL_STYLES, disableInternalStyles ? 0 : 1);
			doEngineCommand(ReaderCommand.DCMD_REQUEST_RENDER, 1);
			mActivity.getDB().saveBookInfo(mBookInfo);
		}
	}

	public void toggleEmbeddedFonts() {
		if (mOpened && mBookInfo != null) {
			log.d("toggleEmbeddedFonts()");
			boolean enableInternalFonts = mBookInfo.getFileInfo().getFlag(FileInfo.USE_DOCUMENT_FONTS_FLAG);
			enableInternalFonts = !enableInternalFonts;
			mBookInfo.getFileInfo().setFlag(FileInfo.USE_DOCUMENT_FONTS_FLAG, enableInternalFonts);
			doEngineCommand( ReaderCommand.DCMD_SET_DOC_FONTS, enableInternalFonts ? 1 : 0);
			doEngineCommand( ReaderCommand.DCMD_REQUEST_RENDER, 1);
			mActivity.getDB().saveBookInfo(mBookInfo);
		}
	}

	public boolean isTextAutoformatEnabled() {
		if (mOpened && mBookInfo != null) {
			boolean disableTextReflow = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
			return !disableTextReflow;
		}
		return true;
	}

	public boolean isTextFormat() {
		if (mOpened && mBookInfo != null) {
			DocumentFormat fmt = mBookInfo.getFileInfo().format;
			return fmt == DocumentFormat.TXT || fmt == DocumentFormat.HTML || fmt == DocumentFormat.PDB;
		}
		return false;
	}

	public boolean isFormatWithEmbeddedFonts() {
		if (mOpened && mBookInfo != null) {
			DocumentFormat fmt = mBookInfo.getFileInfo().format;
			return fmt == DocumentFormat.EPUB;
		}
		return false;
	}

	public boolean isFormatWithEmbeddedStyles() {
		if (mOpened && mBookInfo != null) {
			DocumentFormat fmt = mBookInfo.getFileInfo().format;
			return fmt == DocumentFormat.EPUB || fmt == DocumentFormat.HTML || fmt == DocumentFormat.CHM || fmt == DocumentFormat.FB2 || fmt == DocumentFormat.FB3;
		}
		return false;
	}

	public boolean isHtmlFormat() {
		if (mOpened && mBookInfo != null) {
			DocumentFormat fmt = mBookInfo.getFileInfo().format;
			return fmt == DocumentFormat.EPUB || fmt == DocumentFormat.HTML || fmt == DocumentFormat.PDB || fmt == DocumentFormat.CHM;
		}
		return false;
	}

	public int getDOMVersion() {
		if (mOpened && mBookInfo != null) {
			return mBookInfo.getFileInfo().domVersion;
		}
		return Engine.DOM_VERSION_CURRENT;
	}

	public void setDOMVersion(int version) {
		if (null != mBookInfo) {
			mBookInfo.getFileInfo().domVersion = version;
			doEngineCommand(ReaderCommand.DCMD_SET_REQUESTED_DOM_VERSION, version);
			mActivity.getDB().saveBookInfo(mBookInfo);
			if (mOpened)
				reloadDocument();
		}
	}

	public int getBlockRenderingFlags() {
		if (mOpened && mBookInfo != null) {
			return mBookInfo.getFileInfo().blockRenderingFlags;
		}
		return 0;
	}

	public void setBlockRenderingFlags(int flags) {
		if (null != mBookInfo) {
			mBookInfo.getFileInfo().blockRenderingFlags = flags;
			doEngineCommand(ReaderCommand.DCMD_SET_RENDER_BLOCK_RENDERING_FLAGS, flags);
			mActivity.getDB().saveBookInfo(mBookInfo);
			if (mOpened)
				reloadDocument();
		}
	}

	public void toggleTextFormat() {
		if (mOpened && mBookInfo != null) {
			log.d("toggleDocumentStyles()");
			if (!isTextFormat())
				return;
			boolean disableTextReflow = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
			disableTextReflow = !disableTextReflow;
			mBookInfo.getFileInfo().setFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG, disableTextReflow);
			mActivity.getDB().saveBookInfo(mBookInfo);
			reloadDocument();
		}
	}

	public boolean getDocumentStylesEnabled() {
		if (mOpened && mBookInfo != null) {
			boolean flg = !mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
			return flg;
		}
		return true;
	}

	public boolean getDocumentFontsEnabled() {
		if (mOpened && mBookInfo != null) {
			boolean flg = mBookInfo.getFileInfo().getFlag(FileInfo.USE_DOCUMENT_FONTS_FLAG);
			return flg;
		}
		return true;
	}

	static private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

	public void showBookInfo() {
		mActivity.showBookInfo(mBookInfo, BookInfoDialog.BOOK_INFO, null, null);
	}

	public int autoScrollSpeed = 1500; // chars / minute
	public int autoScrollType = 1; // animated
	private int autoScrollNotificationId = 0;
	public AutoScrollAnimation currentAutoScrollAnimation = null;
	private boolean appPaused = false;
	private boolean currentSimpleAutoScrollActive = false;
	public static int currentSimpleAutoScrollSecTotal = 8; // sec
	public static int currentSimpleAutoScrollSecCnt = currentSimpleAutoScrollSecTotal;

	public boolean isAutoScrollActive() {
		return (currentAutoScrollAnimation != null) || currentSimpleAutoScrollActive;
	}

	public void stopAutoScroll() {
		if (!isAutoScrollActive())
			return;
		mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		surface.setKeepScreenOn(false);
		log.d("stopAutoScroll()");
		notifyAutoscroll(mActivity.getString(R.string.autoscroll_stopped), true);
		BackgroundThread.instance().postGUI(() -> {
			mActivity.updateSavingMark("#");
			mActivity.clearRVBottomSepView();
		}, 200);

		if (currentAutoScrollAnimation != null) currentAutoScrollAnimation.stop();
		currentSimpleAutoScrollActive = false;
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
			bookView.draw(true);
	}

	public static final int AUTOSCROLL_START_ANIMATION_PERCENT = 5;

	private void einkIndicatorBlink(boolean off) {
		final LinearLayout ll = mActivity.getRVBottomView();
		if (ll != null) {
			View separator = mActivity.getRVBottomSepView();
			if (separator != null) {
				log.v("einkIndicatorBlink");
				separator.setBackgroundColor(Color.BLACK);
				if (off) {
					separator.setVisibility(View.INVISIBLE);
				}
				else {
					if (separator.getVisibility() == View.VISIBLE)
						separator.setVisibility(View.INVISIBLE);
					else
						separator.setVisibility(View.VISIBLE);
				}
				ll.invalidate();
			}
		}
	}

	public void currentSimpleAutoScrollTick() {
		log.v("currentSimpleAutoScrollTick(), " + currentSimpleAutoScrollActive +
				", " + currentSimpleAutoScrollSecCnt + ", " + currentSimpleAutoScrollSecTotal);
		if (currentSimpleAutoScrollActive) {
			currentSimpleAutoScrollSecCnt--;
			if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
				if (autoscrollProgressNotificationEnabled)
					bookView.draw(true);
			} else {
				BackgroundThread.instance().postGUI(() -> {
					einkIndicatorBlink(false);
				}, 500);
				BackgroundThread.instance().postGUI(() -> {
					einkIndicatorBlink(false);
				}, 2000);
			}
			if ((autoscrollProgressNotificationEnabled) && (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())))
				mActivity.updateSavingMark("# " + currentSimpleAutoScrollSecCnt);
			if (currentSimpleAutoScrollSecCnt <= 0) {
				currentSimpleAutoScrollSecCnt = currentSimpleAutoScrollSecTotal;
				onCommand(ReaderCommand.DCMD_PAGEDOWN, 1, null, () -> {
					BackgroundThread.instance().postGUI(() -> {
						mActivity.onUserActivity();
						currentSimpleAutoScrollTick();
					}, 1000);
				});
			} else {
				BackgroundThread.instance().postGUI(() -> {
					mActivity.onUserActivity();
					currentSimpleAutoScrollTick();
				}, 1000);
			}
		}
	}

	private void startAutoScroll(boolean isSimple) {
		if (isAutoScrollActive())
			return;
		log.d("startAutoScroll()");
		mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		surface.setKeepScreenOn(true);
		if (!isSimple) {
			currentAutoScrollAnimation = new AutoScrollAnimation(ReaderView.this, AUTOSCROLL_START_ANIMATION_PERCENT * 100, isSimple);
			nextHiliteId++;
			hiliteRect = null;
		} else {
			currentSimpleAutoScrollSecCnt = currentSimpleAutoScrollSecTotal;
			currentSimpleAutoScrollActive = true;
			//currentAutoScrollAnimation = new AutoScrollAnimation(ReaderView.this, AUTOSCROLL_START_ANIMATION_PERCENT * 100, isSimple);
			BackgroundThread.instance().postGUI(() -> {
				mActivity.onUserActivity();
				currentSimpleAutoScrollTick();
			}, 1000);
		}
	}

	private void toggleAutoScroll(boolean isSimple) {
		if (isAutoScrollActive())
			stopAutoScroll();
		else
			startAutoScroll(isSimple);
	}

	private boolean autoscrollSpeedNotificationEnabled = true;
	private boolean autoscrollProgressNotificationEnabled = true;

	private void notifyAutoscroll(final String msg, boolean isFinished) {
		log.d("notifyAutoscroll()");
		if (isFinished) { // notify when autocroll is stopped always
			mActivity.showSToast(msg);
			return;
		}
//		if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
//			return; // disable toast for eink // moved to settings
		if (autoscrollSpeedNotificationEnabled) {
			final int myId = ++autoScrollNotificationId;
			BackgroundThread.instance().postGUI(() -> {
				if (myId == autoScrollNotificationId)
					mActivity.showSToast(msg);
			}, 1000);
		}
	}

	private void notifyAutoscrollSpeed() {
		if (!currentSimpleAutoScrollActive) {
			final String msg = mActivity.getString(R.string.lbl_autoscroll_speed).replace("$1", String.valueOf(autoScrollSpeed));
			notifyAutoscroll(msg, false);
		} else {
			final String msg = mActivity.getString(R.string.lbl_autoscroll_speed_simple).replace("$1",
					String.valueOf(currentSimpleAutoScrollSecTotal));
			notifyAutoscroll(msg, false);
		}
	}

	private void changeAutoScrollSpeed(int delta) {
		if (currentSimpleAutoScrollActive) {
			if (currentSimpleAutoScrollSecTotal - delta >= 1)
				currentSimpleAutoScrollSecTotal -= delta;
			if (currentSimpleAutoScrollSecCnt - delta >= 1)
				currentSimpleAutoScrollSecCnt -= delta;
			setSetting(PROP_APP_VIEW_AUTOSCROLL_SIMPLE_SPEED, String.valueOf(currentSimpleAutoScrollSecTotal), false, true, false);
			notifyAutoscrollSpeed();
		} else {
			if (autoScrollSpeed < 300)
				delta *= 10;
			else if (autoScrollSpeed < 500)
				delta *= 20;
			else if (autoScrollSpeed < 1000)
				delta *= 40;
			else if (autoScrollSpeed < 2000)
				delta *= 80;
			else if (autoScrollSpeed < 5000)
				delta *= 200;
			else
				delta *= 300;
			autoScrollSpeed += delta;
			if (autoScrollSpeed < 200)
				autoScrollSpeed = 200;
			if (autoScrollSpeed > 10000)
				autoScrollSpeed = 10000;
			setSetting(PROP_APP_VIEW_AUTOSCROLL_SPEED, String.valueOf(autoScrollSpeed), false, true, false);
			notifyAutoscrollSpeed();
		}
	}

	public void onCommand(final ReaderCommand cmd, final int param, final ReaderAction ra) {
		onCommand(cmd, param, ra, null);
	}

	private void navigateByHistory(final ReaderCommand cmd) {
		BackgroundThread.instance().postBackground(() -> {
			final boolean res = doc.doCommand(cmd.nativeId, 0);
			BackgroundThread.instance().postGUI(() -> {
				if (res) {
					// successful
					drawPage();
				} else {
					// cannot navigate - no data on stack
					if (cmd == ReaderCommand.DCMD_LINK_BACK) {
						// TODO: exit from activity in some cases?
						if (mActivity.isPreviousFrameHome())
							mActivity.showRootWindow();
						else
							mActivity.showBrowser(!mActivity.isBrowserCreated() ? getOpenedFileInfo() : null, "");
					}
				}
			});
		});
	}

	public void setTimeLeft() {
		if (doc != null) {
			String sLeft = "";
			if (getBookInfo() != null)
				if (getBookInfo().getFileInfo() != null)
					sLeft = mActivity.getReadingTimeLeft(getBookInfo().getFileInfo(), false);
			doc.setTimeLeft(sLeft);
			// for safe mode
			if (doc.getCurPage()>2) {
				String sFile = mActivity.getSettingsFileF(0).getParent() + "/cur_pos0.json";
				File f = new File(sFile);
				if (f.exists()) f.delete();
			}
		}
	}

	private int lastNavBmkIndex = -1;

	public void onCommand(final ReaderCommand cmd, final int param,
						  final ReaderAction ra,
						  final Runnable onFinishHandler) {
		BackgroundThread.ensureGUI();
		log.i("On command " + cmd + (param!=0?" ("+param+")":" "));
		boolean eink = false;
		if ((cmd != ReaderCommand.DCMD_NEXT_BOOKMARK) && (cmd != ReaderCommand.DCMD_PREV_BOOKMARK)) lastNavBmkIndex = -1;
		switch (cmd) {
			case DCMD_FILE_BROWSER_ROOT:
				mActivity.showRootWindow();
				break;
			case DCMD_ABOUT:
				mActivity.showAboutDialog();
				break;
			case DCMD_SWITCH_PROFILE:
				showSwitchProfileDialog();
				break;
			case DCMD_TOGGLE_AUTOSCROLL:
				toggleAutoScroll(autoScrollType == 2);
				break;
			case DCMD_AUTOSCROLL_SPEED_INCREASE:
				changeAutoScrollSpeed(1);
				break;
			case DCMD_AUTOSCROLL_SPEED_DECREASE:
				changeAutoScrollSpeed(-1);
				break;
			case DCMD_SHOW_DICTIONARY:
				DictsDlg dlg = new DictsDlg(mActivity, this, "", null, true);
				dlg.show();
				break;
			case DCMD_OPEN_PREVIOUS_BOOK:
				mActivity.loadPreviousDocument(() -> {
					// do nothing
				});
				break;
			case DCMD_BOOK_INFO:
				if (isBookLoaded())
					showBookInfo();
				break;
			case DCMD_USER_MANUAL:
				showManual();
				break;
			case DCMD_TTS_PLAY:
			{
				if ((!FlavourConstants.PRO_FEATURES)&&(!FlavourConstants.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				log.i("DCMD_TTS_PLAY: initializing TTS");
				mActivity.initTTS(ttsacc -> BackgroundThread.instance().executeGUI(() -> {
					log.i("TTS created: opening TTS toolbar");
					ttsToolbar = TTSToolbarDlg.showDialog(mActivity, ReaderView.this, ttsacc);
					ttsToolbar.setOnCloseListener(() -> ttsToolbar = null);
					ttsToolbar.setAppSettings(mSettings, null);
				}), false);
			}
			break;
			case DCMD_TOGGLE_DOCUMENT_STYLES:
				if (isBookLoaded())
					toggleDocumentStyles();
				break;
			case DCMD_EXPERIMENTAL_FEATURE:
				TagsEditDialog dlg1 = new TagsEditDialog(mActivity, null, true, null);
				dlg1.show();

				if (0==1) {
					Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
					mActivity.sendBroadcast(intent);
					BackgroundThread.instance().postGUI(() -> {
						// Go home
//					mActivity.startActivity(
//							new Intent(Intent.ACTION_MAIN)
//									.addCategory(Intent.CATEGORY_HOME)
//									.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//									.setComponent(new ComponentName("com.onyx", "com.onyx.StartupActivity"))
//					);
						// Go back
//					mActivity.sendBroadcast(
//							new Intent("onyx.android.intent.send.key.event").putExtra("key_code", 4)
//					);
						// recent
						//mActivity.sendBroadcast(new Intent("toggle_recent_screen"));
						//refresh screen
						//mActivity.sendBroadcast(new Intent("onyx.android.intent.action.REFRESH_SCREEN"));
						//screenshot
						mActivity.sendBroadcast(new Intent("onyx.android.intent.screenshot"));
					}, 100);
				}
				break;
			case DCMD_SHOW_HOME_SCREEN:
				mActivity.showHomeScreen();
				break;
			case DCMD_TOGGLE_ORIENTATION:
				toggleScreenOrientation();
				break;
			case DCMD_TOGGLE_FULLSCREEN:
				toggleFullscreen();
				break;
			case DCMD_TOGGLE_TITLEBAR:
				toggleTitlebar();
				break;
			case DCMD_SHOW_POSITION_INFO_POPUP:
				if (isBookLoaded())
					showReadingPositionPopup();
				break;
			case DCMD_TOGGLE_SELECTION_MODE:
				if (isBookLoaded())
					toggleSelectionMode();
				break;
			case DCMD_TOGGLE_INSPECTOR_MODE:
				if (isBookLoaded())
					toggleInspectorMode();
				break;
			case DCMD_TOGGLE_TOUCH_SCREEN_LOCK:
				isTouchScreenEnabled = !isTouchScreenEnabled;
				if (isTouchScreenEnabled)
					mActivity.showToast(R.string.action_touch_screen_enabled_toast);
				else
					mActivity.showToast(R.string.action_touch_screen_disabled_toast);
				break;
			case DCMD_LINK_BACK:
			case DCMD_LINK_FORWARD:
				navigateByHistory(cmd);
				break;
			case DCMD_ZOOM_OUT:
				doEngineCommand( ReaderCommand.DCMD_ZOOM_OUT, param);
				syncViewSettings(getSettings(), true, true);
				break;
			case DCMD_ZOOM_IN:
				doEngineCommand( ReaderCommand.DCMD_ZOOM_IN, param);
				syncViewSettings(getSettings(), true, true);
				break;
			case DCMD_FONT_SELECT:
				mActivity.optionsFilter = "";
				mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_FONT_FACE);
				break;
			case DCMD_FONT_BOLD:
				mActivity.optionsFilter = "";
				mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_FONT_BASE_WEIGHT);
				break;
			case DCMD_FONT_NEXT:
				switchFontFace(1);
				break;
			case DCMD_FONT_PREVIOUS:
				switchFontFace(-1);
				break;
			case DCMD_CHOOSE_TEXTURE:
				mActivity.optionsFilter = "";
				mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_PAGE_BACKGROUND_IMAGE);
				break;
			case DCMD_MOVE_BY_CHAPTER:
				if (isBookLoaded())
					doEngineCommand(cmd, param, onFinishHandler);
				setTimeLeft();
				drawPage();
				break;
			case DCMD_PAGEDOWN:
				if (isBookLoaded()) {
					checkCalendarStats();
					final FileInfo fileInfo = mBookInfo.getFileInfo();
					if (fileInfo != null) {
						PositionProperties currpos = doc.getPositionProps(null, true);
						if (null != currpos) {
							fileInfo.lastTimeSaved = System.currentTimeMillis();
							fileInfo.lastPageSet = currpos.pageNumber;
							ReadingStat rs = new ReadingStat();
							rs.pageNumber = currpos.pageNumber;
							rs.pageCount = doc.getPageCount();
							rs.pageSymbolCount = StrUtils.getNonEmptyStr(doc.getPageText(false, currpos.pageNumber-1),true).length();
							rs.readingBeginTS = fileInfo.lastTimeSaved;
							rs.readingEndTS = 0;
							rs.speedKoef = 0.0;
							fileInfo.stats.add(rs);
							while (fileInfo.stats.size()>100) fileInfo.stats.remove(0);
							for (ReadingStat rs1: fileInfo.stats) {
								if ((rs1.pageNumber == rs.pageNumber - 1) && (rs1.readingEndTS == 0)) {
									rs1.readingEndTS = System.currentTimeMillis();
									if (rs1.pageSymbolCount > 0)
										rs1.speedKoef = ((double)(rs1.readingEndTS - rs1.readingBeginTS)) / ((double) rs1.pageSymbolCount);
								}
							}
						}
					}
					eink = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
					if (param == 1 && !eink)
						animatePageFlip(1, onFinishHandler);
					else {
						if ((mActivity.getScreenBlackpageInterval() != 0) &&
								(curBlackpageInterval >= mActivity.getScreenBlackpageInterval() - 1)) {
							curBlackpageInterval = 0;
							bookView.draw(false, true);
							BackgroundThread.instance().postGUI(() -> {
								//drawPage();
								doEngineCommand(cmd, param, onFinishHandler);
							}, blackpageDuration);
						} else {
							if (mActivity.getScreenBlackpageInterval() != 0) curBlackpageInterval++;
							doEngineCommand(cmd, param, onFinishHandler);
						}
					}
				}
				setTimeLeft();
				break;
			case DCMD_PAGEUP:
				if (isBookLoaded()) {
					eink = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
					if (param == 1 && !eink)
						animatePageFlip(-1, onFinishHandler);
					else {
						if ((mActivity.getScreenBlackpageInterval() != 0) &&
								(curBlackpageInterval >= mActivity.getScreenBlackpageInterval() - 1)) {
							curBlackpageInterval = 0;
							bookView.draw(false, true);
							BackgroundThread.instance().postGUI(() -> {
								//drawPage();
								doEngineCommand(cmd, param, onFinishHandler);
							}, blackpageDuration);
						} else {
							if (mActivity.getScreenBlackpageInterval() != 0) curBlackpageInterval++;
							doEngineCommand(cmd, param, onFinishHandler);
						}
					}
				}
				setTimeLeft();
				break;
			case DCMD_NEXT_BOOKMARK:
				if (lastNavBmkIndex >= 0) {
					if (lastNavBmkIndex < mBookInfo.getBookmarkCount()) {
						if (lastNavBmkIndex + 1 < mBookInfo.getBookmarkCount()) {
							lastNavBmkIndex++;
						} else lastNavBmkIndex = 0;
						this.goToBookmark(mBookInfo.getBookmark(lastNavBmkIndex));
					} else
						lastNavBmkIndex = -1;
				} else {
					Bookmark bmkC = getCurrentPositionBookmark();
					int minP = 999999999;
					for (int i = 0; i < mBookInfo.getBookmarkCount(); i++) {
						if (mBookInfo.getBookmark(i).getPercent() > bmkC.getPercent())
							if (minP > mBookInfo.getBookmark(i).getPercent() - bmkC.getPercent()) {
								minP = mBookInfo.getBookmark(i).getPercent() - bmkC.getPercent();
								lastNavBmkIndex = i;
							}
					}
					if (lastNavBmkIndex >= 0) this.goToBookmark(mBookInfo.getBookmark(lastNavBmkIndex));
				}
				break;
			case DCMD_PREV_BOOKMARK:
				if (lastNavBmkIndex >= 0) {
					if (lastNavBmkIndex < mBookInfo.getBookmarkCount()) {
						if (lastNavBmkIndex - 1 >= 0) {
							lastNavBmkIndex--;
						} else lastNavBmkIndex = mBookInfo.getBookmarkCount() - 1;
						this.goToBookmark(mBookInfo.getBookmark(lastNavBmkIndex));
					} else
						lastNavBmkIndex = -1;
				} else {
					Bookmark bmkC = getCurrentPositionBookmark();
					int minP = 999999999;
					for (int i = 0; i < mBookInfo.getBookmarkCount(); i++) {
						if (bmkC.getPercent() > mBookInfo.getBookmark(i).getPercent())
							if (minP > bmkC.getPercent() - mBookInfo.getBookmark(i).getPercent()) {
								minP = bmkC.getPercent() - mBookInfo.getBookmark(i).getPercent();
								lastNavBmkIndex = i;
							}
					}
					if (lastNavBmkIndex >= 0) this.goToBookmark(mBookInfo.getBookmark(lastNavBmkIndex));
				}
				break;
			case DCMD_BEGIN:
				setTimeLeft();
			case DCMD_END:
				if (isBookLoaded())
					doEngineCommand(cmd, param);
				setTimeLeft();
				break;
			case DCMD_RECENT_BOOKS_LIST:
				mActivity.showRecentBooks();
				break;
			case DCMD_SEARCH:
				if (isBookLoaded())
					showSearchDialog(null);
				break;
			case DCMD_SKIM:
				if (isBookLoaded())
					FindNextDlg.showDialog(doc.getCurrentPageBookmark(), mActivity, this, "", true, true );
				break;
			case DCMD_EXIT:
				mActivity.finish();
				break;
			case DCMD_HIDE:
				mActivity.onBackPressed();
				break;
			case DCMD_BOOKMARKS:
				if (isBookLoaded())
					mActivity.showBookmarksDialog(false, null);
				break;
			//	case DCMD_GO_PERCENT_DIALOG:
			//		if (isBookLoaded())
			//			showGoToPercentDialog();
			//		break;
			case DCMD_GO_PAGE_DIALOG:
				if (isBookLoaded())
					showGoToPageDialog();
				break;
			case DCMD_TOC_DIALOG:
				if (isBookLoaded())
					showTOC();
				break;
			case DCMD_FILE_BROWSER:
				boolean needBrowser = true;
				if (mBookInfo != null) {
					final FileInfo fileInfo = mBookInfo.getFileInfo();
					if (fileInfo != null) {
						final Bookmark bmk = doc != null ? doc.getCurrentPageBookmark() : null;
						final PositionProperties props = bmk != null ? doc.getPositionProps(bmk.getStartPos(), true) : null;
						if (props != null) {
							needBrowser = !(mActivity.willBeCheckAskReading(fileInfo, props, 7));
							mActivity.checkAskReading(fileInfo, props, 7, true);
						}
					}
				}
				if (needBrowser)
					mActivity.showBrowser(!mActivity.isBrowserCreated() ? getOpenedFileInfo() : null, "");
				break;
			case DCMD_CURRENT_BOOK_DIRECTORY:
				mActivity.showBrowser(getOpenedFileInfo(), "");
				break;
			case DCMD_OPTIONS_DIALOG:
				mActivity.optionsFilter = "";
				mActivity.showOptionsDialog(OptionsDialog.Mode.READER);
				break;
			case DCMD_OPTIONS_DIALOG_FILTERED:
				mActivity.showFilterDialog();
				break;
			case DCMD_READER_MENU:
				mActivity.showReaderMenu();
				break;
			case DCMD_TOGGLE_DAY_NIGHT_MODE:
				toggleDayNightMode();
				break;
			case DCMD_TOGGLE_DICT_ONCE:
				log.i("Next dictionary will be the 2nd for one time");
				mActivity.showToast(mActivity.getString(R.string.next_dict_will_be_2nd));
				mActivity.mDictionaries.setiDic2IsActive(2);
				break;
			case DCMD_TOGGLE_DICT:
				if (mActivity.mDictionaries.isiDic2IsActive() > 0) {
					mActivity.mDictionaries.setiDic2IsActive(0);
				}
				else {
					mActivity.mDictionaries.setiDic2IsActive(1);
				}
				log.i("Switched to dictionary (from 0): "+mActivity.mDictionaries.isiDic2IsActive());
				mActivity.showToast("Switched to dictionary: "+ (mActivity.mDictionaries.isiDic2IsActive() + 1));
				break;
			case DCMD_SAVE_SETTINGS_TO_CLOUD:
				log.i("Save settings to CLOUD");
				int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant == 0) {
					mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
					mActivity.optionsFilter = "";
					mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
				} else {
					CloudSync.saveSettingsToFilesOrCloud(((CoolReader) mActivity), false, iSyncVariant == 1);
				}
				break;
			case DCMD_LOAD_SETTINGS_FROM_CLOUD:
				log.i("Load settings from CLOUD");
				int iSyncVariant2 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant2 == 0) {
					mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
					mActivity.optionsFilter = "";
					mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
				} else {
					if (iSyncVariant2 == 1) CloudSync.loadSettingsFiles((mActivity),false);
					else
						CloudSync.loadFromJsonInfoFileList(mActivity,
								CloudSync.CLOUD_SAVE_SETTINGS, false, iSyncVariant2 == 1, CloudAction.NO_SPECIAL_ACTION, false);
				}
				break;
			case DCMD_SAVE_READING_POS:
				log.i("Save reading pos to CLOUD");
				iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant == 0) {
					mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
					mActivity.optionsFilter = "";
					mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
				} else {
					CloudSync.saveJsonInfoFileOrCloud(((CoolReader) mActivity),
							CloudSync.CLOUD_SAVE_READING_POS, false, iSyncVariant == 1, false);
					Bookmark bmk = getCurrentPositionBookmark();
					lastSavedToGdBookmark = bmk;
				}
				break;
			case DCMD_LOAD_READING_POS:
				log.i("Load rpos from CLOUD");
				int iSyncVariant3 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant3 == 0) {
					mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
					mActivity.optionsFilter = "";
					mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
				} else {
					CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
							CloudSync.CLOUD_SAVE_READING_POS, false, iSyncVariant3 == 1, CloudAction.NO_SPECIAL_ACTION, false);
				}
				break;
			case DCMD_SAVE_BOOKMARKS:
				log.i("Save bookmarks to CLOUD");
				int iSyncVariant5 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant5 == 0) {
					mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
					mActivity.optionsFilter = "";
					mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
				} else {
					CloudSync.saveJsonInfoFileOrCloud(((CoolReader) mActivity),
							CloudSync.CLOUD_SAVE_BOOKMARKS, false, iSyncVariant5 == 1, false);
				};
				break;
			case DCMD_LOAD_BOOKMARKS:
				log.i("Load bookmarks from CLOUD");
				int iSyncVariant4 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant4 == 0) {
					mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
					mActivity.optionsFilter = "";
					mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
				} else {
					CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
							CloudSync.CLOUD_SAVE_BOOKMARKS, false, iSyncVariant4 == 1, CloudAction.NO_SPECIAL_ACTION, false);
				}
				break;
			case DCMD_SAVE_CURRENT_BOOK_TO_CLOUD_YND:
				log.i("Save current book to CLOUD_YND");
				FileInfo fi = mActivity.getReaderView().getBookInfo().getFileInfo();
				CloudAction.yndOpenBookDialog(mActivity, fi,true);
				break;
			//case DCMD_OPEN_BOOK_FROM_CLOUD:
			//	log.i("Open book from CLOUD");
			//	mActivity.showToast("To come...");
			//((CoolReader)mActivity).mGoogleDriveTools.signInAndDoAnAction(((CoolReader)mActivity).mGoogleDriveTools.REQUEST_CODE_LOAD_BOOKS_FOLDER_CONTENTS, this);
			//	break;
			case DCMD_OPEN_BOOK_FROM_CLOUD_YND:
				log.i("Open book from CLOUD_YND");
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					break;
				}
				CloudAction.yndOpenBookDialog(mActivity, null,true);
				break;
			case DCMD_OPEN_BOOK_FROM_CLOUD_DBX:
				log.i("Open book from CLOUD_DBX");
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					break;
				}
				CloudAction.dbxOpenBookDialog(mActivity);
				break;
			case DCMD_SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL:
				log.i("DCMD_SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL");
				CloudAction.emailSendBook(mActivity, null);
				break;
			case DCMD_CLOUD_MENU:
				log.i("CLOUD menu");
				ReaderAction[] actions = {
						ReaderAction.SAVE_SETTINGS_TO_CLOUD,
						ReaderAction.LOAD_SETTINGS_FROM_CLOUD,
						ReaderAction.SAVE_READING_POS,
						ReaderAction.LOAD_READING_POS,
						ReaderAction.SAVE_BOOKMARKS,
						ReaderAction.LOAD_BOOKMARKS,
						ReaderAction.OPEN_BOOK_FROM_CLOUD_YND,
						ReaderAction.OPEN_BOOK_FROM_CLOUD_DBX,
						ReaderAction.SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL,
						ReaderAction.SAVE_CURRENT_BOOK_TO_CLOUD_YND //,
						//ReaderAction.OPEN_BOOK_FROM_GD
				};
				mActivity.showActionsToolbarMenu(actions, item -> {
					if (item == ReaderAction.SAVE_SETTINGS_TO_CLOUD) {
						log.i("Save settings to CLOUD");
						int iSyncVariant1 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant1 == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.saveSettingsToFilesOrCloud(((CoolReader) mActivity), false, iSyncVariant1 == 1);
						}
						return true;
					} else if (item == ReaderAction.LOAD_SETTINGS_FROM_CLOUD) {
						log.i("Load settings from CLOUD");
						int iSyncVariant1 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant1 == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							if (iSyncVariant1 == 1) CloudSync.loadSettingsFiles(((CoolReader)mActivity),false);
							else
								CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
										CloudSync.CLOUD_SAVE_SETTINGS, false, iSyncVariant1 == 1, CloudAction.NO_SPECIAL_ACTION, false);
						}
						return true;
					} else if (item == ReaderAction.SAVE_READING_POS) {
						log.i("Save reading pos to CLOUD");
						int iSyncVariant1 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant1 == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.saveJsonInfoFileOrCloud(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_READING_POS, false, iSyncVariant1 == 1, false);
						}
						Bookmark bmk = getCurrentPositionBookmark();
						lastSavedToGdBookmark = bmk;
						return true;
					} else if (item == ReaderAction.LOAD_READING_POS) {
						log.i("Load reading pos from CLOUD");
						int iSyncVariant21 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant21 == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_READING_POS, false, iSyncVariant21 == 1, CloudAction.NO_SPECIAL_ACTION, false);
						}
						return true;
					} else if (item == ReaderAction.SAVE_BOOKMARKS) {
						log.i("Save bookmarks to CLOUD");
						int iSyncVariant1 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant1 == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.saveJsonInfoFileOrCloud(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_BOOKMARKS, false, iSyncVariant1 == 1, false);
						}
						return true;
					} else if (item == ReaderAction.LOAD_BOOKMARKS) {
						log.i("Load bookmarks from CLOUD");
						int iSyncVariant31 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant31 == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_BOOKMARKS, false, iSyncVariant31 == 1, CloudAction.NO_SPECIAL_ACTION, false);
						}
						return true;
					} else if (item == ReaderAction.SAVE_CURRENT_BOOK_TO_CLOUD_YND) {
						log.i("Save current book to YND");
						FileInfo fi1 = mActivity.getReaderView().getBookInfo().getFileInfo();
						CloudAction.yndOpenBookDialog(mActivity, fi1,true);
						return true;
					} else if (item == ReaderAction.OPEN_BOOK_FROM_CLOUD_YND) {
						log.i("Open book from CLOUD_YND");
						if (!FlavourConstants.PREMIUM_FEATURES) {
							mActivity.showToast(R.string.only_in_premium);
							return true;
						}
						CloudAction.yndOpenBookDialog(mActivity, null,true);
						return true;
					} else if (item == ReaderAction.OPEN_BOOK_FROM_CLOUD_DBX) {
						log.i("Open book from CLOUD_DBX");
						if (!FlavourConstants.PREMIUM_FEATURES) {
							mActivity.showToast(R.string.only_in_premium);
							return true;
						}
						CloudAction.dbxOpenBookDialog(mActivity);
						return true;
					} else if (item == ReaderAction.SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL) {
						log.i("SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL");
						CloudAction.emailSendBook(mActivity, null);
						return true;
					}
					return false;
				});
				break;
			case DCMD_FONTS_MENU:
				log.i("Fonts menu");
				ReaderAction[] fonts_actions = {
						ReaderAction.FONT_PREVIOUS,
						ReaderAction.FONT_NEXT,
						ReaderAction.ZOOM_IN,
						ReaderAction.ZOOM_OUT,
						ReaderAction.FONT_SELECT,
						ReaderAction.FONT_BOLD,
						ReaderAction.CHOOSE_TEXTURE
				};
				mActivity.showActionsToolbarMenu(fonts_actions, item -> {
					if (item == ReaderAction.FONT_PREVIOUS) {
						skipFallbackWarning = false;
						switchFontFace(-1);
						return true;
					} else if (item == ReaderAction.FONT_NEXT) {
						skipFallbackWarning = false;
						switchFontFace(1);
						return true;
					} else if (item == ReaderAction.ZOOM_IN) {
						doEngineCommand( ReaderCommand.DCMD_ZOOM_IN, param);
						syncViewSettings(getSettings(), true, true);
						return true;
					} else if (item == ReaderAction.ZOOM_OUT) {
						doEngineCommand( ReaderCommand.DCMD_ZOOM_OUT, param);
						syncViewSettings(getSettings(), true, true);
						return true;
					} else if (item == ReaderAction.FONT_SELECT) {
						mActivity.optionsFilter = "";
						skipFallbackWarning = false;
						mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_FONT_FACE);
						return true;
					} else if (item == ReaderAction.FONT_BOLD) {
						mActivity.optionsFilter = "";
						mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_FONT_BASE_WEIGHT);
						return true;
					} else if (item == ReaderAction.CHOOSE_TEXTURE) {
						mActivity.optionsFilter = "";
						mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_PAGE_BACKGROUND_IMAGE);
						return true;
					}
					return false;
				});
				break;
			case DCMD_SAVE_BOOKMARK_LAST_SEL:
				if (lastSelection!=null) {
					if (!lastSelection.isEmpty()) {
						clearSelection();
						showNewBookmarkDialog(lastSelection, Bookmark.TYPE_COMMENT, "");
					}
				}
				break;
			case DCMD_SAVE_BOOKMARK_QUICK:
				addBookmark(0, true);
				break;
			case DCMD_BACKLIGHT_SET_DEFAULT:
				skipFallbackWarning = true;
				setSetting(PROP_APP_SCREEN_BACKLIGHT, "-1");		// system default backlight level
				skipFallbackWarning = true;
				setSetting(PROP_APP_SCREEN_WARM_BACKLIGHT, "-1"); // and dont forget the warm one
				break;
			case DCMD_SHOW_SYSTEM_BACKLIGHT_DIALOG:
				if (DeviceInfo.EINK_HAVE_FRONTLIGHT) {
					if (DeviceInfo.EINK_ONYX) {
						mActivity.sendBroadcast(new Intent("action.show.brightness.dialog"));
					} else {
						// TODO: other eink devices with frontlight
					}
				}
				break;
			case DCMD_SHOW_USER_DIC:
				if ((!FlavourConstants.PRO_FEATURES)&&(!FlavourConstants.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				UserDicDlg dlgU = new UserDicDlg(mActivity,0);
				dlgU.show();
				break;
			case DCMD_SAVE_BOOKMARK_LAST_SEL_USER_DIC:
				if ((!FlavourConstants.PRO_FEATURES)&&(!FlavourConstants.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				if (lastSelection!=null) {
					if (!lastSelection.isEmpty()) {
						clearSelection();
						showNewBookmarkDialog(lastSelection, Bookmark.TYPE_USER_DIC, "");
					}
				}
				break;
			case DCMD_SHOW_CITATIONS:
				if ((!FlavourConstants.PRO_FEATURES)&&(!FlavourConstants.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				UserDicDlg dlg2 = new UserDicDlg(((CoolReader)mActivity),1);
				dlg2.show();
				break;
			case DCMD_TOGGLE_PAGE_VIEW_MODE:
				String oldViewSetting = this.getSetting( ReaderView.PROP_PAGE_VIEW_MODE );
				boolean newBool = false;
				if (this.getSetting( ReaderView.PROP_PAGE_VIEW_MODE ) != null)
					newBool = this.getSetting( ReaderView.PROP_PAGE_VIEW_MODE ).equals("0");
				String newValue = newBool ? "1" : "0";
				saveSetting(PROP_PAGE_VIEW_MODE, newValue);
				break;
			case DCMD_WHOLE_PAGE_TO_DIC:
				log.i("Whole page to dic");
				String s = mActivity.getmReaderFrame().getUserDicPanel().getCurPageText(0, false);
				mActivity.findInDictionary(s, false, null);
				break;
			case DCMD_GOOGLEDRIVE_SYNC:
				if (0 == param) {							// sync to
					mActivity.forceSyncToGoogleDrive();
				} else if (1 == param) {					// sync from
					mActivity.forceSyncFromGoogleDrive();
				}
				break;
			case DCMD_SAVE_LOGCAT:
				mActivity.createLogcatFile();
				break;
			case DCMD_BRIGHTNESS_DOWN:
				startBrightnessControl(0,0, BRIGHTNESS_TYPE_COLD_VAL);
				stopBrightnessControl(0, surface.getHeight() / 20, BRIGHTNESS_TYPE_COLD_VAL);
				break;
			case DCMD_BRIGHTNESS_UP:
				startBrightnessControl(0,surface.getHeight() / 20, BRIGHTNESS_TYPE_COLD_VAL);
				stopBrightnessControl(0, 0, BRIGHTNESS_TYPE_COLD_VAL);
				break;
			case DCMD_BRIGHTNESS_WARM_DOWN:
				startBrightnessControl(0,0, BRIGHTNESS_TYPE_WARM_VAL);
				stopBrightnessControl(0, surface.getHeight() / 20, BRIGHTNESS_TYPE_WARM_VAL);
				break;
			case DCMD_BRIGHTNESS_WARM_UP:
				startBrightnessControl(0,surface.getHeight() / 20, BRIGHTNESS_TYPE_WARM_VAL);
				stopBrightnessControl(0, 0, BRIGHTNESS_TYPE_WARM_VAL);
				break;
			case DCMD_OPTION:
				if (ra.actionOption instanceof BoolOption) {
					Properties settings = getSettings();
					boolean val = settings.getBool(ra.actionOption.property, false);
					settings.setBool(ra.actionOption.property, !val);
					mActivity.setSettings(settings, 500, true);
				}
				break;
			case DCMD_EINK_ONYX_BACK:
				mActivity.sendBroadcast(
					new Intent("onyx.android.intent.send.key.event").putExtra("key_code", 4)
				);
				break;
			case DCMD_EINK_ONYX_HOME:
				mActivity.startActivity(
					new Intent(Intent.ACTION_MAIN)
						.addCategory(Intent.CATEGORY_HOME)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						.setComponent(new ComponentName("com.onyx", "com.onyx.StartupActivity"))
				);
				break;
			case DCMD_EINK_ONYX_RECENT:
				mActivity.sendBroadcast(new Intent("toggle_recent_screen"));
				break;
			case DCMD_EINK_ONYX_REPAINT_SCREEN:
				mActivity.sendBroadcast(new Intent("onyx.android.intent.action.REFRESH_SCREEN"));
				break;
			case DCMD_EINK_ONYX_SCREENSHOT:
				mActivity.sendBroadcast(new Intent("onyx.android.intent.screenshot"));
				break;
			case DCMD_ADD_BOOK_TAGS:
				FileInfo fiTED = mActivity.getReaderView().getBookInfo().getFileInfo();
				TagsEditDialog dlgTagsEditDialog = new TagsEditDialog(mActivity, fiTED, true, new TagsEditDialog.TagsEditDialogCloseCallback() {
					@Override
					public void onOk() {
						mActivity.waitForCRDBService(() ->
							mActivity.getDB().loadTags(fiTED, tags -> {
								String stags = "";
								for (BookTag bookTag : tags) {
									if (bookTag.isSelected)
										stags = stags + '|' + bookTag.name;
								}
								if (!StrUtils.isEmptyStr(stags))
									fiTED.setTags(stags.substring(1));
								else
									fiTED.setTags("");
							}));
					}

					@Override
					public void onCancel() {

					}
				});
				dlgTagsEditDialog.show();
				break;
			default:
				// do nothing
				break;
		}
	}
	boolean firstShowBrowserCall = true;

	public TTSToolbarDlg ttsToolbar;

	public void pauseTTS() {
		if (ttsToolbar != null)
			ttsToolbar.pause();
	}

	public boolean isTTSActive() {
		return ttsToolbar != null;
	}

	public TTSToolbarDlg getTTSToolbar() {
		return ttsToolbar;
	}

	public void doEngineCommand(final ReaderCommand cmd, final int param)
	{
		doEngineCommand( cmd, param, null );
	}

	public void doEngineCommand(final ReaderCommand cmd, final int param, final Runnable doneHandler)
	{
		BackgroundThread.ensureGUI();
		log.d("doCommand("+ cmd + ", " + param +")");
		post(new Task() {
			boolean res;
			boolean isMoveCommand;
			public void work() {
				BackgroundThread.ensureBackground();
				res = doc.doCommand(cmd.nativeId, param);
				switch (cmd) {
					case DCMD_BEGIN:
					case DCMD_LINEUP:
					case DCMD_PAGEUP:
					case DCMD_PAGEDOWN:
					case DCMD_LINEDOWN:
					case DCMD_LINK_FORWARD:
					case DCMD_LINK_BACK:
					case DCMD_LINK_NEXT:
					case DCMD_LINK_PREV:
					case DCMD_LINK_GO:
					case DCMD_END:
					case DCMD_GO_POS:
					case DCMD_GO_PAGE:
					case DCMD_MOVE_BY_CHAPTER:
					case DCMD_GO_SCROLL_POS:
					case DCMD_LINK_FIRST:
					case DCMD_SCROLL_BY:
						isMoveCommand = true;
						break;
					default:
						// do nothing
						break;
				}
				if (isMoveCommand && isBookLoaded())
					updateCurrentPositionStatus();
			}
			public void done() {
				if (res) {
					invalidImages = true;
					drawPage(doneHandler, false);
				}
				if (isMoveCommand && isBookLoaded())
					scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			}
		});
	}

	// update book and position info in status bar
	void updateCurrentPositionStatus() {
		if (mBookInfo == null)
			return;
		// in background thread
		final FileInfo fileInfo = mBookInfo.getFileInfo();
		if (fileInfo == null)
			return;
		final Bookmark bmk = doc != null ? doc.getCurrentPageBookmark() : null;
		final PositionProperties props = bmk != null ? doc.getPositionProps(bmk.getStartPos(), false) : null;
		if (props != null) BackgroundThread.instance().postGUI(() -> {
			mActivity.updateCurrentPositionStatus(fileInfo, bmk, props);

			String fname = mBookInfo.getFileInfo().getBasePath();
			if (fname != null && fname.length() > 0)
				setBookPositionForExternalShell(fname, props.pageNumber, props.pageCount);
		});
	}

	public void doCommandFromBackgroundThread(final ReaderCommand cmd, final int param)
	{
		log.d("doCommandFromBackgroundThread("+cmd + ", " + param +")");
		BackgroundThread.ensureBackground();
		boolean res = doc.doCommand(cmd.nativeId, param);
		if (res) {
			BackgroundThread.instance().executeGUI(this::drawPage);
		}
	}

	volatile public boolean mInitialized = false;
	volatile public boolean mOpened = false;

	//private File historyFile;

	void updateLoadedBookInfo(boolean updatePath)
	{
		BackgroundThread.ensureBackground();
		// get title, authors, genres, etc.
		doc.updateBookInfo(mBookInfo, updatePath);
		updateCurrentPositionStatus();
		// check whether current book properties updated on another devices
		// TODO: fix and reenable
		//syncUpdater.syncExternalChanges(mBookInfo);
	}

	EinkScreen.EinkUpdateMode updMode = EinkScreen.EinkUpdateMode.Unspecified;
	int updInterval = -1;

	public boolean skipFallbackWarning = false;

	void applySettings(Properties props) {
		bNeedRedrawOnce = true;
		//mActivity.showToast("bNeedRedrawOnce");
		props = new Properties(props); // make a copy
		props.remove(PROP_TXT_OPTION_PREFORMATTED);
		props.remove(PROP_EMBEDDED_STYLES);
		props.remove(PROP_EMBEDDED_FONTS);
		props.remove(PROP_REQUESTED_DOM_VERSION);
		props.remove(PROP_RENDER_BLOCK_RENDERING_FLAGS);
		BackgroundThread.ensureBackground();
		log.v("applySettings()");
		boolean isFullScreen = props.getBool(PROP_APP_FULLSCREEN, false );
		props.setBool(PROP_SHOW_BATTERY, isFullScreen);
		props.setBool(PROP_SHOW_TIME, isFullScreen);
		String backgroundImageId = props.getProperty(PROP_PAGE_BACKGROUND_IMAGE);
		int backgroundColor = props.getColor(PROP_BACKGROUND_COLOR, 0xFFFFFF);
		setBackgroundTexture(backgroundImageId, backgroundColor);
		int statusLocation = props.getInt(PROP_STATUS_LOCATION, VIEWER_STATUS_PAGE_HEADER);
		int statusLine = 0;
		switch (statusLocation) {
			case VIEWER_STATUS_PAGE_HEADER:
				statusLine = 1;
				break;
			case VIEWER_STATUS_PAGE_FOOTER:
				statusLine = 2;
				break;
			case VIEWER_STATUS_PAGE_2LINES_HEADER:
				statusLine = 3;
				break;
			case VIEWER_STATUS_PAGE_2LINES_FOOTER:
				statusLine = 4;
				break;
		}
		props.setInt(PROP_STATUS_LINE, statusLine);

		if (!inDisabledFullRefresh()) {
			// If this function is called when new settings loaded from the cloud are applied,
			// we must prohibit changing the e-ink screen refresh mode, as this will lead to
			// a periodic full screen refresh when drawing the next phase of the progress bar.
			int updModeCode = props.getInt(PROP_APP_SCREEN_UPDATE_MODE, EinkScreen.EinkUpdateMode.Normal.code);
			int updInterval = props.getInt(PROP_APP_SCREEN_UPDATE_INTERVAL, 10);
			mActivity.setScreenUpdateMode(EinkScreen.EinkUpdateMode.byCode(updModeCode), surface);
			mActivity.setScreenUpdateInterval(updInterval, surface);
		}

		int blackpageInterval  = props.getInt(PROP_APP_SCREEN_BLACKPAGE_INTERVAL, 0);
		blackpageDuration  = props.getInt(PROP_APP_SCREEN_BLACKPAGE_DURATION, 300);
		mActivity.setScreenBlackpageInterval(blackpageInterval);
		mActivity.setScreenBlackpageDuration(blackpageDuration);

		getActivity().readResizeHistory();

		if (null != mBookInfo) {
			FileInfo fileInfo = mBookInfo.getFileInfo();
			final String bookLanguage = fileInfo.getLanguage();
			final String fontFace = props.getProperty(PROP_FONT_FACE);
			if (null != bookLanguage && bookLanguage.length() > 0) {
				if (props.getBool(PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false))
					props.setProperty(PROP_TEXTLANG_MAIN_LANG, bookLanguage);
				final String langDescr = Engine.getHumanReadableLocaleName(bookLanguage);
				if (null != langDescr && langDescr.length() > 0) {
					Engine.font_lang_compat compat = Engine.checkFontLanguageCompatibility(fontFace, bookLanguage);
					log.d("Checking font \"" + fontFace + "\" for compatibility with language \"" + bookLanguage + "\" fcLangCode=" + langDescr + ": compat=" + compat);
					switch (compat) {
						case font_lang_compat_invalid_tag:
							log.w("Can't find compatible language code in embedded FontConfig catalog: language=\"" + bookLanguage + "\", filename=\"" + fileInfo + "\"");
							skipFallbackWarning = true;
							break;
						case font_lang_compat_none:
							if (!skipFallbackWarning)
								BackgroundThread.instance().executeGUI(() ->
								{
									mActivity.showToast(R.string.font_not_compat_with_language, fontFace, langDescr);
									skipFallbackWarning = true;
								});
							break;
						case font_lang_compat_partial:
							if (!skipFallbackWarning)
								BackgroundThread.instance().executeGUI(() ->
								{
									mActivity.showToast(R.string.font_compat_partial_with_language, fontFace, langDescr);
									skipFallbackWarning = true;
								});
							break;
						case font_lang_compat_full:
							// good, do nothing
							skipFallbackWarning = true;
							break;
					}
				} else {
					log.d("Invalid language tag: \"" + bookLanguage + "\", filename=\"" + fileInfo + "\"");
					skipFallbackWarning = true;
				}
			}
		}
		//skipFallbackWarning = false; // Too often, do it in our way
		doc.applySettings(props);
		//if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
			drawPage();
		//syncViewSettings(props, save, saveDelayed);
		if (mActivity.getmReaderFrame() != null) {
			BackgroundThread.instance().postGUI(() -> {
				mActivity.getmReaderFrame().updateCRToolbar(((CoolReader) mActivity));
			}, 300);
		}
	}

	public static boolean eq(Object obj1, Object obj2)
	{
		if (obj1 == null && obj2 == null)
			return true;
		if (obj1 == null || obj2 == null)
			return false;
		return obj1.equals(obj2);
	}

	public void saveSettings( Properties settings )
	{
		mActivity.setSettings(settings, 0, false);
	}

	/**
	 * Read JNI view settings, update and save if changed
	 */
	private void syncViewSettings(final Properties currSettings, final boolean save, final boolean saveDelayed) {
		post( new Task() {
			Properties props;
			public void work() {
				BackgroundThread.ensureBackground();
				java.util.Properties internalProps = doc.getSettings();
				props = new Properties(internalProps);
			}
			public void done() {
				Properties changedSettings = props.diff(currSettings);
				for ( Map.Entry<Object, Object> entry : changedSettings.entrySet() ) {
					currSettings.setProperty((String)entry.getKey(), (String)entry.getValue());
				}
				mSettings = currSettings;
				if (save) {
					mActivity.setSettings(mSettings, saveDelayed ? 5000 : 0, false);
				} else {
					mActivity.setSettings(mSettings, -1, false);
				}
			}
		});
	}

	public Properties getSettings()
	{
		return mActivity.settings(); //new Properties(mSettings); // it was slow )
	}

	static public int stringToInt(String value, int defValue) {
		if (value == null)
			return defValue;
		try {
			return Integer.valueOf(value);
		} catch ( NumberFormatException e ) {
			return defValue;
		}
	}

	private String getManualFileName() {
		Scanner s = Services.getScanner();
		if (s != null) {
			FileInfo fi = s.getDownloadDirectory();
			if (fi != null) {
				File bookDir = new File(fi.getPathName());
				return HelpFileGenerator.getHelpFileName(bookDir, mActivity.getCurrentLanguage()).getAbsolutePath();
			}
		}
		log.e("cannot get manual file name!");
		return null;
	}

	private File generateManual() {
		HelpFileGenerator generator = new HelpFileGenerator(mActivity, mEngine, getSettings(), mActivity.getCurrentLanguage());
		FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
		File bookDir;
		if (downloadDir != null)
			bookDir = new File(Services.getScanner().getDownloadDirectory().getPathName());
		else {
			log.e("cannot download directory file name!");
			bookDir = new File("/tmp/");
		}
		int settingsHash = generator.getSettingsHash();
		String helpFileContentId = mActivity.getCurrentLanguage() + settingsHash + "v" + mActivity.getVersion();
		String lastHelpFileContentId = mActivity.getLastGeneratedHelpFileSignature();
		File manual = generator.getHelpFileName(bookDir);
		if ((!manual.exists()) || (lastHelpFileContentId == null) || (!lastHelpFileContentId.equals(helpFileContentId))) {
			log.d("Generating help file " + manual.getAbsolutePath());
			mActivity.setLastGeneratedHelpFileSignature(helpFileContentId);
			manual = generator.generateHelpFile(bookDir);
		}
		return manual;
	}

	int getPageFlipAnimationSpeedMs() {
		return pageFlipAnimationMode!=PAGE_ANIMATION_NONE ? pageFlipAnimationSpeed : 0;
	}

	/**
	 * Generate help file (if necessary) and show it.
	 * @return true if opened successfully
	 */
	public boolean showManual() {
		return loadDocument(getManualFileName(), "", null, () -> mActivity.showToast(R.string.manual_open_error));
	}

	public boolean hiliteTapZoneOnTap = false;
	private boolean enableVolumeKeys = true;
	static private final int DEF_PAGE_FLIP_MS = 300;

	public void applyAppSetting(String key, String value) {
		boolean flg = "1".equals(value);
		if (key.equals(PROP_APP_TAP_ZONE_HILIGHT)) {
			hiliteTapZoneOnTap = flg;
		} else if (key.equals(PROP_APP_DOUBLE_TAP_SELECTION)) {
			doubleTapSelectionEnabled = flg;
		} else if (key.equals(PROP_APP_BOUNCE_TAP_INTERVAL)) {
			mBounceTapInterval = Utils.parseInt(value, -1, 50, 250);
			//} else if (key.equals(PROP_APP_GESTURE_PAGE_FLIPPING)) {
			//	mGesturePageFlipsPerFullSwipe = Integer.valueOf(value);
		} else if (key.equals(PROP_APP_GESTURE_PAGE_FLIPPING_NEW)) {
			mGesturePageFlipSwipeN = Integer.valueOf(value);
		} else if (key.equals(PROP_APP_GESTURE_PAGE_FLIPPING_SENSIVITY)) {
			mGesturePageFlipSensivity = Integer.valueOf(value);
		} else if (key.equals(PROP_APP_GESTURE_PAGE_FLIPPING_PAGE_COUNT)) {
			mGesturePageFlipPageCount = Integer.valueOf(value);
		} else if (key.equals(PROP_APP_DISABLE_TWO_POINTER_GESTURES)) {
			mDisableTwoPointerGestures = "1".equals(value);
		}else if (key.equals(PROP_APP_SECONDARY_TAP_ACTION_TYPE)) {
			secondaryTapActionType = flg ? TAP_ACTION_TYPE_DOUBLE : TAP_ACTION_TYPE_LONGPRESS;
		} else if (key.equals(PROP_APP_FLICK_BACKLIGHT_CONTROL)) {
			isBacklightControlFlick = 0;
			try {
				int n = Integer.parseInt(value);
				isBacklightControlFlick = n;
			} catch (NumberFormatException e) {
				// ignore
			}
//			isBacklightControlFlick = "1".equals(value) ? 1 : ("2".equals(value) ? 2 : 0);
//		} else if (key.equals(PROP_APP_FLICK_WARMLIGHT_CONTROL)) {
//			isWarmBacklightControlFlick = "1".equals(value) ? 1 : ("2".equals(value) ? 2 : 0);
		} else if (key.equals(PROP_APP_SCREEN_BACKLIGHT_FIX_DELTA)) {
			backlightFixDelta = false;
			try {
				int n = Integer.parseInt(value);
				backlightFixDelta = n == 1;
			} catch (NumberFormatException e) {
				// ignore
			}
		} else if (PROP_APP_HIGHLIGHT_BOOKMARKS.equals(key)) {
			flgHighlightBookmarks = !"0".equals(value);
			clearSelection();
		} else if (PROP_APP_HIGHLIGHT_USER_DIC.equals(key)) {
			flgHighlightUserDic = !"0".equals(value);
			clearSelection();
		} else if (PROP_APP_VIEW_AUTOSCROLL_SPEED.equals(key)) {
			autoScrollSpeed = Utils.parseInt(value, 1500, 200, 10000);
		} else if (PROP_APP_VIEW_AUTOSCROLL_TYPE.equals(key)) {
			autoScrollType = Utils.parseInt(value, 1);
		} else if (PROP_APP_VIEW_AUTOSCROLL_SIMPLE_SPEED.equals(key)) {
			currentSimpleAutoScrollSecTotal = Utils.parseInt(value, 8, 1, 100);
		} else if (PROP_APP_VIEW_AUTOSCROLL_SHOW_SPEED.equals(key)) {
			autoscrollSpeedNotificationEnabled = Utils.parseInt(value, 0) == 0? false: true;
		} else if (PROP_APP_VIEW_AUTOSCROLL_SHOW_PROGRESS.equals(key)) {
			autoscrollProgressNotificationEnabled = Utils.parseInt(value, 0) == 0? false: true;
		} else if (PROP_PAGE_ANIMATION.equals(key)) {
			pageFlipAnimationMode = Utils.parseInt(value, PAGE_ANIMATION_SLIDE2, PAGE_ANIMATION_NONE, PAGE_ANIMATION_MAX);
		} else if (PROP_PAGE_ANIMATION_SPEED.equals(key)) {
			pageFlipAnimationSpeed = Utils.parseInt(value, 300, 100, 800);
		} else if (PROP_DOUBLE_CLICK_INTERVAL.equals(key)) {
			doubleClickInterval = Utils.parseInt(value, 400, 100, 1000);
		} else if (PROP_PREVENT_CLICK_INTERVAL.equals(key)) {
			preventClickInterval =  Utils.parseInt(value, 0, 0, 1000);
		} else if (PROP_CONTROLS_ENABLE_VOLUME_KEYS.equals(key)) {
			enableVolumeKeys = flg;
		} else if (PROP_APP_SELECTION_ACTION.equals(key)) {
			mSelectionAction = Utils.parseInt(value, Settings.SELECTION_ACTION_TOOLBAR);
		} else if (PROP_APP_MULTI_SELECTION_ACTION.equals(key)) {
			mMultiSelectionAction = Utils.parseInt(value, Settings.SELECTION_ACTION_TOOLBAR);
		} else if (PROP_APP_SELECTION_ACTION_LONG.equals(key)) {
			mSelectionActionLong = Utils.parseInt(value, Settings.SELECTION_ACTION_TOOLBAR);
		} else if (PROP_APP_BOOKMARK_ACTION_SEND_TO.equals(key)) {
			mBookmarkActionSendTo = Utils.parseInt(value, -1);
		} else if (PROP_APP_SELECTION2_ACTION.equals(key)) {
			mSelection2Action = Utils.parseInt(value, Settings.SELECTION_ACTION_SAME_AS_COMMON);
		} else if (PROP_APP_MULTI_SELECTION2_ACTION.equals(key)) {
			mMultiSelection2Action = Utils.parseInt(value, Settings.SELECTION_ACTION_SAME_AS_COMMON);
		} else if (PROP_APP_SELECTION2_ACTION_LONG.equals(key)) {
			mSelection2ActionLong = Utils.parseInt(value, Settings.SELECTION_ACTION_SAME_AS_COMMON);
		}  else if (PROP_APP_SELECTION3_ACTION.equals(key)) {
			mSelection3Action = Utils.parseInt(value, Settings.SELECTION_ACTION_SAME_AS_COMMON);
		} else if (PROP_APP_MULTI_SELECTION3_ACTION.equals(key)) {
			mMultiSelection3Action = Utils.parseInt(value, Settings.SELECTION_ACTION_SAME_AS_COMMON);
		} else if (PROP_APP_SELECTION3_ACTION_LONG.equals(key)) {
			mSelection3ActionLong = Utils.parseInt(value, Settings.SELECTION_ACTION_SAME_AS_COMMON);
		} else if (PROP_APP_SHOW_USER_DIC_PANEL.equals(key)) {
			try {
				mActivity.mReaderFrame.removeView(mActivity.mReaderFrame.userDicView);
				mActivity.mReaderFrame.userDicView = new UserDicPanel(mActivity);
				mActivity.mReaderFrame.userDicView.setBackground(mActivity.mReaderFrame.statusBackground);
				mActivity.mReaderFrame.addView(mActivity.mReaderFrame.userDicView);
				mActivity.mReaderFrame.userDicView.setVisibility(mActivity.ismShowUserDicPanel() ? View.VISIBLE : View.GONE);
				BackgroundThread.instance().postGUI(() -> {
					mActivity.mReaderFrame.userDicView.updateSettings(mActivity.settings());
					mActivity.updateUserDicWords();
				}, 500);
			} catch (Exception e) {
				//do nothing
			}
		} else {
			//mActivity.applyAppSetting(key, value);
		}
		//
	}

	public void setAppSettings(Properties newSettings, Properties oldSettings)
	{
		log.v("setAppSettings()"); //|| keyCode == KeyEvent.KEYCODE_DPAD_LEFT
		BackgroundThread.ensureGUI();
		if (oldSettings == null)
			oldSettings = mSettings;
		Properties changedSettings = newSettings.diff(oldSettings);
		boolean viewModeAutoChanged = false;
		for ( Map.Entry<Object, Object> entry : changedSettings.entrySet() ) {
			String key = (String)entry.getKey();
			String value = (String)entry.getValue();
			if (PROP_PAGE_VIEW_MODE_AUTOCHANGED.equals(key)) {
				viewModeAutoChanged = "1".equals(value);
				newSettings.setBool(ReaderView.PROP_PAGE_VIEW_MODE_AUTOCHANGED, false);
				break;
			}
		}
		for (Map.Entry<Object, Object> entry : changedSettings.entrySet()) {
			String key = (String)entry.getKey();
			String value = (String)entry.getValue();
			applyAppSetting(key, value);
			if (PROP_APP_FULLSCREEN.equals(key)) {
				boolean flg = mSettings.getBool(PROP_APP_FULLSCREEN, false);
				newSettings.setBool(PROP_SHOW_BATTERY, flg);
				newSettings.setBool(PROP_SHOW_TIME, flg);
			} else if (PROP_PAGE_VIEW_MODE.equals(key)) {
				boolean flg = "1".equals(value);
				if (viewModeAutoChanged)
					viewMode = (!flg) ? ViewMode.PAGES : ViewMode.SCROLL;
				else viewMode = flg ? ViewMode.PAGES : ViewMode.SCROLL;
				if (viewModeAutoChanged) {
					final String sVal = (viewMode == ViewMode.PAGES) ? "1" : "0";
					BackgroundThread.instance().postGUI(() -> {
						saveSetting(PROP_PAGE_VIEW_MODE, sVal);
						saveSetting(PROP_PAGE_VIEW_MODE_AUTOCHANGED, "0");
					}, 2000);
				}
			} else if (PROP_APP_SCREEN_ORIENTATION.equals(key)
					|| PROP_PAGE_ANIMATION.equals(key)
					|| PROP_CONTROLS_ENABLE_VOLUME_KEYS.equals(key)
					|| PROP_APP_SHOW_COVERPAGES.equals(key)
					|| PROP_APP_COVERPAGE_SIZE.equals(key)
					|| PROP_APP_SCREEN_BACKLIGHT.equals(key)
					|| PROP_APP_SCREEN_WARM_BACKLIGHT.equals(key)
					|| PROP_APP_USE_EINK_FRONTLIGHT.equals(key)
					|| PROP_APP_BOOK_PROPERTY_SCAN_ENABLED.equals(key)
					|| PROP_APP_SCREEN_BACKLIGHT_LOCK.equals(key)
					|| PROP_APP_TAP_ZONE_HILIGHT.equals(key)
					|| PROP_APP_DICTIONARY.equals(key)
					|| PROP_APP_DOUBLE_TAP_SELECTION.equals(key)
					|| PROP_APP_BOUNCE_TAP_INTERVAL.equals(key)
					|| PROP_APP_FLICK_BACKLIGHT_CONTROL.equals(key)
//					|| PROP_APP_FLICK_WARMLIGHT_CONTROL.equals(key)
					|| PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS.equals(key)
					|| PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES.equals(key)
					|| PROP_APP_SELECTION_ACTION.equals(key)
					|| PROP_APP_FILE_BROWSER_SIMPLE_MODE.equals(key)
					|| PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE.equals(key)
					|| PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_AUTHOR.equals(key)
					|| PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_SERIES.equals(key)
					|| PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_GENRES.equals(key)
					|| PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_TAGS.equals(key)
					|| PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_DATES.equals(key)

					|| PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_TAGS.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_RATING.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_STATE.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_DATES.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_SEARCH.equals(key)

					|| PROP_APP_GESTURE_PAGE_FLIPPING.equals(key)
					|| PROP_APP_DISABLE_TWO_POINTER_GESTURES.equals(key)
					|| PROP_APP_HIGHLIGHT_BOOKMARKS.equals(key)
					|| PROP_HIGHLIGHT_SELECTION_COLOR.equals(key)
					|| PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT.equals(key)
					|| PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION.equals(key)
				// TODO: redesign all this mess!
			) {
				newSettings.setProperty(key, value);
			}
		}
	}

	public ViewMode getViewMode()
	{
		return viewMode;
	}

	/**
	 * Change settings.
	 * @param newSettings are new settings
	 */
	public void updateSettings(Properties newSettings)
	{
		log.v("updateSettings() " + newSettings.toString());
		log.v("oldNightMode=" + mSettings.getProperty(PROP_NIGHT_MODE) + " newNightMode=" + newSettings.getProperty(PROP_NIGHT_MODE));
		BackgroundThread.ensureGUI();
		final Properties currSettings = new Properties(mSettings);
		if (null != ttsToolbar) {
			// ignore all non TTS options if TTS is active...
			ttsToolbar.setAppSettings(newSettings, currSettings);
			Properties changedSettings = newSettings.diff(currSettings);
			currSettings.setAll(changedSettings);
			mSettings = currSettings;
		} else {
			setAppSettings(newSettings, currSettings);
			Properties changedSettings = newSettings.diff(currSettings);
			currSettings.setAll(changedSettings);
			mSettings = currSettings;
			BackgroundThread.instance().postBackground(() -> applySettings(currSettings));
		}
	}

	private void setBackgroundTexture(String textureId, int color) {
		BackgroundTextureInfo[] textures = mEngine.getAvailableTextures();
		for ( BackgroundTextureInfo item : textures ) {
			if (item.id.equals(textureId)) {
				setBackgroundTexture(item, color);
				return;
			}
		}
		setBackgroundTexture(Engine.NO_TEXTURE, color);
	}

	private void setBackgroundTexture(BackgroundTextureInfo texture, int color) {
		log.v("setBackgroundTexture(" + texture + ", " + color + ")");
		int col = color;
		if (!currentBackgroundTexture.equals(texture) || currentBackgroundColor != col) {
			log.d("setBackgroundTexture( " + texture + " )");
			currentBackgroundColor = col;
			currentBackgroundTexture = texture;
			byte[] data = mEngine.getImageData(currentBackgroundTexture);
			doc.setPageBackgroundTexture(data, texture.tiled ? 1 : 0);
			currentBackgroundTextureTiled = texture.tiled;
			if (data != null && data.length > 0) {
				if (currentBackgroundTextureBitmap != null)
					currentBackgroundTextureBitmap.recycle();
				try {
					currentBackgroundTextureBitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
				} catch (Exception e) {
					log.e("Exception while decoding image data", e);
					currentBackgroundTextureBitmap = null;
				}
			} else {
				currentBackgroundTextureBitmap = null;
			}
		}
	}

	BackgroundTextureInfo currentBackgroundTexture = Engine.NO_TEXTURE;
	Bitmap currentBackgroundTextureBitmap = null;
	boolean currentBackgroundTextureTiled = false;
	int currentBackgroundColor = 0;

	public void closeIfOpened(final FileInfo fileInfo) {
		if (this.mBookInfo != null && this.mBookInfo.getFileInfo().pathname.equals(fileInfo.pathname) && mOpened) {
			close();
		}
	}

	public boolean reloadDocument() {
		if (this.mBookInfo!=null && this.mBookInfo.getFileInfo() != null) {
			save(); // save current position
			post(new LoadDocumentTask(ReaderView.this, this.mBookInfo, null, null, null));
			return true;
		}
		return false;
	}

	private void postLoadTask(final FileInfo fileInfo, final Runnable doneHandler, final Runnable errorHandler) {
		Bookmark bmk = new Bookmark();
		bmk.bookFile = StrUtils.getNonEmptyStr(fileInfo.getFilename(),true);
		bmk.bookPath = StrUtils.getNonEmptyStr(fileInfo.getPathName(),true);
		bmk.bookFileArc = StrUtils.getNonEmptyStr(fileInfo.getArchiveName(),true);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String prettyJson = gson.toJson(bmk);
		mActivity.saveCurPosFile(true, prettyJson);
		Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), fileInfo, bookInfo -> {
			log.v("posting LoadDocument task to background thread");
			BackgroundThread.instance().postBackground(() -> {
				log.v("posting LoadDocument task to GUI thread");
				BackgroundThread.instance().postGUI(() -> {
					log.v("synced posting LoadDocument task to GUI thread");
					post(new LoadDocumentTask(ReaderView.this, bookInfo, null, doneHandler, errorHandler));
				});
			});
		});
	}

	public boolean loadDocument(final FileInfo fileInfo, final Runnable doneHandler, final Runnable errorHandler) {
		log.v("loadDocument(" + fileInfo.getPathName() + ")");
		if (fileInfo.documentFile != null) {
			ExternalDocCameDialog dlg = new ExternalDocCameDialog(mActivity,
				fileInfo.documentFile.getType(), fileInfo.documentFile.getUri(), "");
			dlg.show();
			return true;
		}
		if (this.mBookInfo != null && this.mBookInfo.getFileInfo().pathname.equals(fileInfo.pathname) && mOpened) {
			log.d("trying to load already opened document");
			mActivity.showReader();
			if (null != doneHandler)
				doneHandler.run();
			lastCachedBitmap = null;
			drawPage();
			return false;
		}
		boolean needAsk = false;
		Bookmark bmk = getActivity().readCurPosFile(true);
		if (bmk != null) {
			if (
					StrUtils.getNonEmptyStr(bmk.bookFile,true).equals(
							StrUtils.getNonEmptyStr(fileInfo.getFilename(),true)) &&
							StrUtils.getNonEmptyStr(bmk.bookPath,true).equals(
									StrUtils.getNonEmptyStr(fileInfo.getPathName(),true)) &&
							StrUtils.getNonEmptyStr(bmk.bookFileArc,true).equals(
									StrUtils.getNonEmptyStr(fileInfo.getArchiveName(),true))
			) {
				needAsk = true;
			}
		}
		if ((needAsk) && (!mActivity.settings().getBool(Settings.PROP_APP_DISABLE_SAFE_MODE, false))) {
			BackgroundThread.instance().postGUI(() -> {
				ArrayList<String> sButtons = new ArrayList<String>();
				sButtons.add("*"+mActivity.getString(R.string.warn_hang)+" "+bmk.bookFile);
				sButtons.add(mActivity.getString(R.string.str_yes));
				sButtons.add(mActivity.getString(R.string.safe_mode_clear_cache));
				sButtons.add(mActivity.getString(R.string.str_no));
				SomeButtonsToolbarDlg.showDialog(mActivity, mActivity.getReaderView().getSurface(), 10, true,
						mActivity.getString(R.string.options_app_safe_mode),
						sButtons, null, (o22, btnPressed) -> {
							if (
									(btnPressed.equals(getActivity().getString(R.string.str_yes))) ||
											(btnPressed.equals("{{timeout}}"))
							) {
								postLoadTask(fileInfo, doneHandler, errorHandler);
							} else
							if (btnPressed.equals(getActivity().getString(R.string.safe_mode_clear_cache))) {
								File fSett = mActivity.getSettingsFileF(0);
								File fCR3E = fSett.getParentFile();
								if (fCR3E != null) {
									File[] allContents = fCR3E.listFiles();
									for (File file : allContents) {
										if ((file.getName().equals("cache")) && (file.isDirectory())) Utils.deleteDirectory(file);
									}
								}
								postLoadTask(fileInfo, doneHandler, errorHandler);
							} else {
								mActivity.showRootWindow();
								if (doc != null) {
									String sFile = mActivity.getSettingsFileF(0).getParent() + "/cur_pos0.json";
									File f = new File(sFile);
									if (f.exists()) f.delete();
								}
							}
						});
			}, 200);
		} else postLoadTask(fileInfo, doneHandler, errorHandler);
		return true;
	}

	private void postLoadTaskStream(final ByteArrayOutputStream outputStream, final FileInfo fileInfo,
									final Runnable doneHandler, final Runnable errorHandler) {
		Bookmark bmk = new Bookmark();
		bmk.bookFile = "stream";
		bmk.bookPath = "stream";
		bmk.bookFileArc = "stream";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String prettyJson = gson.toJson(bmk);
		mActivity.saveCurPosFile(true, prettyJson);
		byte[] docBuffer = outputStream.toByteArray();
		// Don't search in DB this memory stream before opening it
		BookInfo bookInfo = new BookInfo(fileInfo);
		log.v("posting LoadDocument task to background thread");
		BackgroundThread.instance().postBackground(() -> {
			log.v("posting LoadDocument task to GUI thread");
			BackgroundThread.instance().postGUI(() -> {
				log.v("synced posting LoadDocument task to GUI thread");
				post(new LoadDocumentTask(ReaderView.this, bookInfo, docBuffer, doneHandler, errorHandler));
			});
		});
	}

	public boolean loadDocumentFromStream(final InputStream inputStream, final FileInfo fileInfo, final Runnable doneHandler, final Runnable errorHandler) {
		log.v("loadDocumentFromStream(" + fileInfo.getPathName() + ")");
		// When the document is opened from the stream at this moment,
		// we do not know the real path to the file, since it will be
		// changed after the successful opening of the document,
		// so here we cannot compare the path to the document currently
		// open with the fileinfo argument.

		// Copy data from input stream to byte array
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		boolean copyOk = false;
		try {
			byte [] buf = new byte [4096];
			int readBytes;
			while (true) {
				readBytes = inputStream.read(buf);
				if (readBytes > 0)
					outputStream.write(buf, 0, readBytes);
				else
					break;
			}
			copyOk = true;
		} catch (IOException e1) {
			log.e("I/O error while copying content from input stream to buffer. Interrupted.");
		} catch (OutOfMemoryError e2) {
			log.e("Out of memory while copying content from input stream to buffer. Interrupted.");
		}
		boolean needAsk = false;
		Bookmark bmk = getActivity().readCurPosFile(true);
		if (bmk != null) {
			if (
					StrUtils.getNonEmptyStr(bmk.bookFile,true).equals("stream") &&
							StrUtils.getNonEmptyStr(bmk.bookPath,true).equals("stream") &&
							StrUtils.getNonEmptyStr(bmk.bookFileArc,true).equals("stream")
			) {
				needAsk = true;
			}
		}
		if (copyOk) {
			if ((needAsk) && (!mActivity.settings().getBool(Settings.PROP_APP_DISABLE_SAFE_MODE, false))) {
				BackgroundThread.instance().postGUI(() -> {
					ArrayList<String> sButtons = new ArrayList<String>();
					sButtons.add("*" + mActivity.getString(R.string.warn_hang) + " (stream)");
					sButtons.add(mActivity.getString(R.string.str_yes));
					sButtons.add(mActivity.getString(R.string.safe_mode_clear_cache));
					sButtons.add(mActivity.getString(R.string.str_no));
					SomeButtonsToolbarDlg.showDialog(mActivity, mActivity.getReaderView().getSurface(), 10, true,
							mActivity.getString(R.string.options_app_safe_mode),
							sButtons, null, (o22, btnPressed) -> {
								if (
										(btnPressed.equals(getActivity().getString(R.string.str_yes))) ||
												(btnPressed.equals("{{timeout}}"))
								) {
									postLoadTaskStream(outputStream, fileInfo, doneHandler, errorHandler);
								} else if (btnPressed.equals(getActivity().getString(R.string.safe_mode_clear_cache))) {
									File fSett = mActivity.getSettingsFileF(0);
									File fCR3E = fSett.getParentFile();
									if (fCR3E != null) {
										File[] allContents = fCR3E.listFiles();
										for (File file : allContents) {
											if ((file.getName().equals("cache")) && (file.isDirectory()))
												Utils.deleteDirectory(file);
										}
									}
									postLoadTask(fileInfo, doneHandler, errorHandler);
								} else {
									mActivity.showRootWindow();
									if (doc != null) {
										String sFile = mActivity.getSettingsFileF(0).getParent() + "/cur_pos0.json";
										File f = new File(sFile);
										if (f.exists()) f.delete();
									}
								}
							});
				}, 200);
			} else postLoadTaskStream(outputStream, fileInfo, doneHandler, errorHandler);
			return true;
		}
		return false;
	}

	public boolean loadDocument(String fileName, Object fileLink, final Runnable doneHandler, final Runnable errorHandler) {
		lastSelection = null;
		hyplinkBookmark = null;
		lastSavedToGdBookmark = null;
		skipFallbackWarning = false;
		BackgroundThread.ensureGUI();
		save();
		log.i("loadDocument(" + fileName + ")");
		if (fileName == null) {
			log.v("loadDocument() : no filename specified");
			if (errorHandler != null)
				errorHandler.run();
			return false;
		}
		if ("@manual".equals(fileName)) {
			fileName = getManualFileName();
			log.i("Manual document: " + fileName);
		}
		String normalized = mEngine.getPathCorrector().normalizeIfPossible(fileName);
		if (normalized == null) {
			log.e("Trying to load book from non-standard path " + fileName);
			//mActivity.showSToast("Trying to load book from non-standard path " + fileName); // this cause Onyx Darwin 5 to shutdown CR
			// this cause not to try to load book
			//hideProgress();
			//if (errorHandler != null)
			//	errorHandler.run();
			//return false;
		} else if (!normalized.equals(fileName)) {
			log.w("Filename normalized to " + normalized);
			fileName = normalized;
		}
		if (fileName.equals(getManualFileName())) {
			// ensure manual file is up to date
			if (generateManual() == null) {
				log.v("loadDocument() : no filename specified");
				if (errorHandler != null)
					errorHandler.run();
				return false;
			}
		}
		BookInfo book = Services.getHistory().getBookInfo(fileName);
		if (book != null)
			log.v("loadDocument() : found book in history : " + book);
		FileInfo fi;
		if (book == null) {
			log.v("loadDocument() : book not found in history, looking for location directory");
			FileInfo dir = Services.getScanner().findParent(new FileInfo(fileName), Services.getScanner().getRoot());
			if (dir != null) log.v("loadDocument() : parent dir is: "+dir.pathname);
			if (dir == null) log.v("loadDocument() : parent dir is NULL");
			if (dir != null) {
				log.v("loadDocument() : document location found : " + dir);
				fi = dir.findItemByPathName(fileName);
				log.v("loadDocument() : item inside location : " + fi);
			} else {
				/*File tfile = new File(fileName);
				if (tfile.exists())
				*/
				log.v("loadDocument() : dir == null, BUT : " + fileName);
				fi = new FileInfo(fileName);
			}
			if (fi == null) {
				log.v("loadDocument() : no file item " + fileName + " found inside " + dir);
				if (errorHandler != null) errorHandler.run();
				return false;
			}
			if (fi.isDirectory) {
				log.v("loadDocument() : is a directory, opening browser");
				mActivity.showBrowser(fi, "");
				return true;
			}
		} else {
			fi = book.getFileInfo();
			log.v("loadDocument() : item from history : " + fi);
		}
		// We'll save the "content" link
		if (fileLink != null) {
			if (fileLink instanceof String)
				if (!StrUtils.isEmptyStr((String) fileLink)) {
					fi.opdsLink = (String) fileLink;
					mActivity.getDB().saveBookInfo(new BookInfo(fi));
					mActivity.getDB().flush();
				}
			if (fileLink instanceof DocumentFile) {
				fi.opdsLink = ((DocumentFile) fileLink).getUri().toString();
				fi.documentFile = (DocumentFile) fileLink;
				mActivity.getDB().saveBookInfo(new BookInfo(fi));
				mActivity.getDB().flush();
			}
		}
		return loadDocument(fi, doneHandler, errorHandler);
	}

	public boolean loadDocumentFromStream(InputStream inputStream, String contentPath, final Runnable doneHandler, final Runnable errorHandler) {
		lastSelection = null;
		hyplinkBookmark = null;
		lastSavedToGdBookmark = null;
		skipFallbackWarning = false;
		BackgroundThread.ensureGUI();
		save();
		log.i("loadDocument(" + contentPath + ")");
		if (contentPath == null || inputStream == null) {
			log.v("loadDocument() : no filename or stream specified");
			if (errorHandler != null)
				errorHandler.run();
			return false;
		}
		FileInfo fi = new FileInfo(contentPath);
		return loadDocumentFromStream(inputStream, fi, doneHandler, errorHandler);
	}

	public BookInfo getBookInfo() {
		BackgroundThread.ensureGUI();
		return mBookInfo;
	}

	private int mBatteryState = BATTERY_STATE_DISCHARGING;
	private int mBatteryChargingConn = BATTERY_CHARGER_NO;
	private int mBatteryChargeLevel = 0;

//KR implementation, we'll try to switch to CR
//	public void setBatteryState(int state) {
//		if (state != mBatteryState) {
//			log.i("Battery state changed: " + state);
//			int diff = Math.abs(state - mBatteryState);
//			mBatteryState = state;
//			if (
//					(
//						(!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
//						||
//						(diff > 9000)
//					)
//					&& (!isAutoScrollActive())) {
//				drawPage();
//			}
//		}
//	}

	public void setBatteryState(int state, int chargingConn, int level) {
		boolean needUpdate = false;
		if (state != mBatteryState) {
			log.i("Battery state changed: " + state);
			mBatteryState = state;
			needUpdate = true;
		}
		if (chargingConn != mBatteryChargingConn) {
			log.i("Battery charging connection changed: " + chargingConn);
			mBatteryChargingConn = chargingConn;
			needUpdate = true;
		}
		if (level != mBatteryChargeLevel) {
			log.i("Battery charging level changed: " + level);
			mBatteryChargeLevel = level;
			needUpdate = true;
		}
		if (needUpdate) {
			if (!DeviceInfo.EINK_SCREEN && !isAutoScrollActive()) {
				redraw();
			}
		}
	}

	public int getBatteryState() {
		return mBatteryState;
	}

	public int getBatteryChargingConnection() {
		return mBatteryChargingConn;
	}

	public int getBatteryChargeLevel() {
		return mBatteryChargeLevel;
	}

	public void onTimeTickReceived() {
		if (!DeviceInfo.EINK_SCREEN && !isAutoScrollActive()) {
			if (doc.isTimeChanged()) {
				log.i("The current time has been changed (minutes), redrawing is scheduled.");
				redraw();
			}
		}
	}

	public String getBatteryStateText() {
		int level = mBatteryChargeLevel;
		if (mBatteryState == BATTERY_STATE_CHARGING) {
			if (mBatteryChargingConn == BATTERY_CHARGER_WIRELESS) {
				return "" + level + "% (" + getActivity().getString(R.string.wireless_charging) + ")";
			}
			if (mBatteryChargingConn == BATTERY_CHARGER_AC) {
				return "" + level + "% (" + getActivity().getString(R.string.ac_charging) + ")";
			}
			if (mBatteryChargingConn == BATTERY_CHARGER_USB) {
				return "" + level + "% (" + getActivity().getString(R.string.usb_charging) + ")";
			}
			return "" + level + "% (" + getActivity().getString(R.string.charging) + ")";
		}
		return "" + mBatteryChargeLevel + "%";
	}

	private static final VMRuntimeHack runtime = new VMRuntimeHack();
	BitmapFactory factory = new BitmapFactory(runtime);

	public Bitmap lastCachedBitmap = null;

	public BitmapInfo mCurrentPageInfo;
	public BitmapInfo mNextPageInfo;
	/**
	 * Prepare and cache page image.
	 * Cache is represented by two slots: mCurrentPageInfo and mNextPageInfo.
	 * If page already exists in cache, returns it (if current page requested,
	 *  ensures that it became stored as mCurrentPageInfo; if another page requested,
	 *  no mCurrentPageInfo/mNextPageInfo reordering made).
	 * @param offset is kind of page: 0==current, -1=previous, 1=next page
	 * @return page image and properties, null if requested page is unavailable (e.g. requested next/prev page is out of document range)
	 */
	BitmapInfo preparePageImage(int offset) {
		lastCachedBitmap = null;
		BackgroundThread.ensureBackground();
		log.v("preparePageImage( "+offset+")");
		//if (offset == 0) {
//			// DEBUG stack trace
//			try {
//				if (currentAutoScrollAnimation!=null)
//					log.v("preparePageImage from autoscroll");
//				throw new Exception("stack trace");
//			} catch (Exception e) {
//				Log.d("cr3", "stack trace", e);
//			}
		//}
		if (invalidImages) {
			if (mCurrentPageInfo != null)
				mCurrentPageInfo.recycle();
			mCurrentPageInfo = null;
			if (mNextPageInfo != null)
				mNextPageInfo.recycle();
			mNextPageInfo = null;
			invalidImages = false;
		}

		if (internalDX == 0 || internalDY == 0) {
			if (requestedWidth > 0 && requestedHeight > 0) {
				internalDX = requestedWidth;
				internalDY = requestedHeight;
				doc.resize(internalDX, internalDY);
			} else {
				internalDX = surface.getWidth();
				internalDY = surface.getHeight();
				doc.resize(internalDX, internalDY);
			}
		}

		if (currentImageViewer != null)
			return currentImageViewer.prepareImage();

		PositionProperties currpos = doc.getPositionProps(null, false);
		if (null == currpos)
			return null;

		boolean isPageView = currpos.pageMode!=0;

		BitmapInfo currposBitmap = null;
		if (mCurrentPageInfo != null && mCurrentPageInfo.position != null && mCurrentPageInfo.position.equals(currpos) && mCurrentPageInfo.imageInfo == null)
			currposBitmap = mCurrentPageInfo;
		else if (mNextPageInfo != null && mNextPageInfo.position != null && mNextPageInfo.position.equals(currpos) && mNextPageInfo.imageInfo == null)
			currposBitmap = mNextPageInfo;
		if (offset == 0) {
			// Current page requested
			if (currposBitmap != null) {
				if (mNextPageInfo == currposBitmap) {
					// reorder pages
					BitmapInfo tmp = mNextPageInfo;
					mNextPageInfo = mCurrentPageInfo;
					mCurrentPageInfo = tmp;
				}
				// found ready page image
				return mCurrentPageInfo;
			}
			if (mCurrentPageInfo != null) {
				mCurrentPageInfo.recycle();
				mCurrentPageInfo = null;
			}
			BitmapInfo bi = new BitmapInfo(factory);
			bi.position = currpos;
			bi.bitmap = factory.get(internalDX > 0 ? internalDX : requestedWidth,
					internalDY > 0 ? internalDY : requestedHeight);
			doc.setBatteryState(mBatteryState, mBatteryChargingConn, mBatteryChargeLevel);
			doc.getPageImage(bi.bitmap);
			mCurrentPageInfo = bi;
			return mCurrentPageInfo;
		}
		if (isPageView) {
			// PAGES: one of next or prev pages requested, offset is specified as param
			int cmd1 = offset > 0 ? ReaderCommand.DCMD_PAGEDOWN.nativeId : ReaderCommand.DCMD_PAGEUP.nativeId;
			int cmd2 = offset > 0 ? ReaderCommand.DCMD_PAGEUP.nativeId : ReaderCommand.DCMD_PAGEDOWN.nativeId;
			if (offset < 0)
				offset = -offset;
			if (doc.doCommand(cmd1, offset)) {
				// can move to next page
				PositionProperties nextpos = doc.getPositionProps(null, false);
				BitmapInfo nextposBitmap = null;
				if (mCurrentPageInfo != null && mCurrentPageInfo.position != null && mCurrentPageInfo.position.equals(nextpos))
					nextposBitmap = mCurrentPageInfo;
				else if (mNextPageInfo != null && mNextPageInfo.position != null && mNextPageInfo.position.equals(nextpos))
					nextposBitmap = mNextPageInfo;
				if (nextposBitmap == null) {
					// existing image not found in cache, overriding mNextPageInfo
					if (mNextPageInfo != null)
						mNextPageInfo.recycle();
					mNextPageInfo = null;
					BitmapInfo bi = new BitmapInfo(factory);
					bi.position = nextpos;
					bi.bitmap = factory.get(internalDX, internalDY);
					doc.setBatteryState(mBatteryState, mBatteryChargingConn, mBatteryChargeLevel);
					doc.getPageImage(bi.bitmap);
					mNextPageInfo = bi;
					nextposBitmap = bi;
					//log.v("Prepared new current page image " + mNextPageInfo);
				}
				// return back to previous page
				doc.doCommand(cmd2, offset);
				return nextposBitmap;
			} else {
				// cannot move to page: out of document range
				return null;
			}
		} else {
			// SCROLL next or prev page requested, with pixel offset specified
			int y = currpos.y + offset;
			if (doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, y)) {
				PositionProperties nextpos = doc.getPositionProps(null, false);
				BitmapInfo nextposBitmap = null;
				if (mCurrentPageInfo != null && mCurrentPageInfo.position != null && mCurrentPageInfo.position.equals(nextpos))
					nextposBitmap = mCurrentPageInfo;
				else if (mNextPageInfo != null && mNextPageInfo.position != null && mNextPageInfo.position.equals(nextpos))
					nextposBitmap = mNextPageInfo;
				if (nextposBitmap == null) {
					// existing image not found in cache, overriding mNextPageInfo
					if (mNextPageInfo != null)
						mNextPageInfo.recycle();
					mNextPageInfo = null;
					BitmapInfo bi = new BitmapInfo(factory);
					bi.position = nextpos;
					bi.bitmap = factory.get(internalDX, internalDY);
					doc.setBatteryState(mBatteryState, mBatteryChargingConn, mBatteryChargeLevel);
					doc.getPageImage(bi.bitmap);
					mNextPageInfo = bi;
					nextposBitmap = bi;
				}
				// return back to prev position
				doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, currpos.y);
				return nextposBitmap;
			} else {
				return null;
			}
		}

	}

	public int lastDrawTaskId = 0;

	static class ReaderSurfaceView extends SurfaceView {

		public ReaderSurfaceView(Context context) {
			super(context);
		}

	}

	public int getRequestedWidth() {
		return requestedWidth;
	}
	public int getLastsetWidth() {
		return lastsetWidth;
	}

	//	private boolean mIsOnFront = false;
	private int requestedWidth = 0;
	public int lastsetWidth = 0;
	private int lastsetOrientation = -1;

	public long getRequestedResTime() {
		return requestedResTime;
	}
	private long requestedResTime;

	public int getRequestedHeight() {
		return requestedHeight;
	}
	public int getLastsetHeight() {
		return lastsetHeight;
	}

	private int requestedHeight = 0;
	private int lastsetHeight = 0;
	public long getLastsetResTime() {
		return lastsetResTime;
	}
	private long lastsetResTime;

	OrientationToolbarDlg orientationToolbarDlg = null;

//	public void setOnFront(boolean front) {
//		if (mIsOnFront == front)
//			return;
//		mIsOnFront = front;
//		log.d("setOnFront(" + front + ")");
//		if (mIsOnFront) {
//			checkSize();
//		} else {
//			// save position immediately
//			scheduleSaveCurrentPositionBookmark(0);
//		}
//	}

	void requestResize(int width, int height) {
		boolean bNeed = (width != lastsetWidth) || (height != lastsetHeight);
		// check if orientation was switched
		boolean bSwitched = false;
		if (
				(
						((width>height) && (lastsetWidth<lastsetHeight))
								||
								((width<height) && (lastsetWidth>lastsetHeight))
				) &&
						(width != lastsetWidth) &&
						(height != lastsetHeight)
		) bSwitched = true;
		Iterator it = mActivity.getmBaseDialog().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			try {
				BaseDialog sVal = (BaseDialog) pair.getValue();
				if (sVal.isShowing()) {
					bSwitched = false;
				}
			} catch (Exception e) {
				log.w("Could not check the dialogs...");
			}
		}
		int iOrnt = getSettings().getInt(Settings.PROP_APP_SCREEN_ORIENTATION, 0);
		int curOrientation = getActivity().sensorCurRot;
		if ((bSwitched)&&(iOrnt==4)) {
			int iSett = getSettings().getInt(Settings.PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION, 10);
			int iExtFS = getSettings().getInt(Settings.PROP_EXT_FULLSCREEN_MARGIN, 0);
			int iExtFSM = getSettings().getInt(Settings.PROP_EXT_FULLSCREEN_MOD, 0);
			if (orientationToolbarDlg!=null)
				if (orientationToolbarDlg.mWindow != null)
					orientationToolbarDlg.mWindow.dismiss();
			if ((iSett > 0) && (!isTTSActive())) {
				long curTime = System.currentTimeMillis();
				long interv = 3000L; // 3 seconds
				if (curTime - OrientationToolbarDlg.lastOrientationShowTime > interv) {
					OrientationToolbarDlg.lastOrientationShowTime = curTime;
					orientationToolbarDlg = OrientationToolbarDlg.showDialog(mActivity, ReaderView.this,
							curOrientation, false);
				}
			}
			boolean isLandscape =
					(curOrientation == DeviceOrientation.ORIENTATION_LANDSCAPE) ||
							(curOrientation == DeviceOrientation.ORIENTATION_LANDSCAPE_REVERSE);
			boolean needExtraEdges = false;
			needExtraEdges = needExtraEdges || (((iExtFSM == 0) || (iExtFSM == 2)) && (iExtFS > 0) && (isLandscape));
			needExtraEdges = needExtraEdges || (((iExtFSM == 0) || (iExtFSM == 1)) && (iExtFS > 0) && (!isLandscape));
			mActivity.setCutoutMode(needExtraEdges?1:0);
		}
		lastsetOrientation = getActivity().getScreenOrientation();
		requestedWidth = width;
		requestedHeight = height;
		if (requestedWidth <= 0)
			requestedWidth = 80;
		if (requestedHeight <= 0)
			requestedHeight = 80;
		requestedResTime = System.currentTimeMillis();
		if (!checkNeedRedraw(width,height)) {
			mActivity.updateSavingMark(mActivity.getString(R.string.request_resize)+": "+width+", "+height);
			internalDX = requestedWidth;
			internalDY = requestedHeight;
			return;
		}
		lastsetWidth = width;
		lastsetHeight = height;
		lastsetResTime = System.currentTimeMillis();
		if (bNeed) {
			mActivity.updateSavingMark(mActivity.getString(R.string.resizing_to)+": "+width+", "+height);
		}
		checkSize();
	}

	public void resized() {
		boolean bSwitched = true;
		Iterator it = mActivity.getmBaseDialog().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			try {
				BaseDialog sVal = (BaseDialog) pair.getValue();
				if (sVal.isShowing()) {
					bSwitched = false;
				}
			} catch (Exception e) {
				log.w("Could not check the dialogs...");
			}
		}
		if (bSwitched) {
			int iSett = getSettings().getInt(Settings.PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION, 10);
			int iOrnt = getSettings().getInt(Settings.PROP_APP_SCREEN_ORIENTATION, 0);
			int curOrientation = getActivity().sensorCurRot;
			if (orientationToolbarDlg!=null)
				if (orientationToolbarDlg.mWindow != null)
					orientationToolbarDlg.mWindow.dismiss();
			if (((iSett > 0) && (iOrnt != 4)) && (!isTTSActive())) {
				long curTime = System.currentTimeMillis();
				long interv = 3000L; // 3 seconds
				if (curTime - OrientationToolbarDlg.lastOrientationShowTime > interv) {
					OrientationToolbarDlg.lastOrientationShowTime = curTime;
					orientationToolbarDlg = OrientationToolbarDlg.showDialog(mActivity, ReaderView.this,
							curOrientation, true);
				}
			}
		}
	}

	void checkSize() {
		boolean changed = (requestedWidth != internalDX) || (requestedHeight != internalDY);
		if (!changed)
			return;
		//NB: buggins method of active dialog tracking - possibly not needed for me. Disabled
//		if (getActivity().isDialogActive()) {
//			log.d("checkSize() : dialog is active, skipping resize");
//			return;
//		}
//		if (mIsOnFront || !mOpened) {
		log.d("checkSize() : calling resize");
		resize();
//		} else {
//			log.d("Skipping resize request");
//		}
	}

	private void resize() {
		final int thisId = ++lastResizeTaskId;
//	    if ( w<h && mActivity.isLandscape() ) {
//	    	log.i("ignoring size change to portrait since landscape is set");
//	    	return;
//	    }
//		if ( mActivity.isPaused() ) {
//			log.i("ignoring size change since activity is paused");
//			return;
//		}
		// update size with delay: chance to avoid extra unnecessary resizing

		Runnable task = () -> {
			if (thisId != lastResizeTaskId) {
				log.d("skipping duplicate resize request in GUI thread");
				return;
			}
			post(new Task() {
				public void work() {
					BackgroundThread.ensureBackground();
					if (thisId != lastResizeTaskId) {
						log.d("skipping duplicate resize request");
						return;
					}
					internalDX = requestedWidth;
					internalDY = requestedHeight;
					log.d("ResizeTask: resizeInternal(" + internalDX + "," + internalDY + ")");
					doc.resize(internalDX, internalDY);
				}
				public void done() {
					clearImageCache();
					drawPage(null, false);
				}
			});
		};

		boolean needDelay = ((internalDX == 0) && (internalDY == 0));
		needDelay = needDelay || ((internalDX == 100) && (internalDY == 100)); // hack when we do default resize

		long timeSinceLastResume = System.currentTimeMillis() - lastAppResumeTs;
		int delay = 300;

		if (timeSinceLastResume < 1000)
			delay = 1000;

		if (mOpened || needDelay) {
			log.d("scheduling delayed resize task id=" + thisId + " for " + delay + " ms");
			BackgroundThread.instance().postGUI(task, delay);
		} else {
			log.d("executing resize without delay");
			task.run();
		}
	}

	int hackMemorySize = 0;
	// SurfaceView callbacks
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, final int width,
							   final int height) {
		log.i("surfaceChanged(" + width + ", " + height + ")");

		if (hackMemorySize <= 0) {
			hackMemorySize = width * height * 2;
			runtime.trackFree(hackMemorySize);
		}


		surface.invalidate();
		bookView.draw();
	}

	boolean mSurfaceCreated = false;
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		log.i("surfaceCreated()");
		mSurfaceCreated = true;
		//draw();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		log.i("surfaceDestroyed()");
		mSurfaceCreated = false;
		if (hackMemorySize > 0) {
			runtime.trackAlloc(hackMemorySize);
			hackMemorySize = 0;
		}
	}

	enum AnimationType {
		SCROLL, // for scroll mode
		PAGE_SHIFT, // for simple page shift
	}



	public ViewAnimationControl currentAnimation = null;

	//private int pageFlipAnimationSpeedMs = DEF_PAGE_FLIP_MS; // if 0 : no animation
	public int pageFlipAnimationMode = PAGE_ANIMATION_SLIDE2; //PAGE_ANIMATION_PAPER; // if 0 : no animation
	public int pageFlipAnimationSpeed = DEF_PAGE_FLIP_MS;
	public int doubleClickInterval = DOUBLE_CLICK_INTERVAL;
	public int preventClickInterval = PREVENT_CLICK_INTERVAL;

	int getDoubleClickInterval() {
		if (doubleClickInterval > 0) return doubleClickInterval;
		return DOUBLE_CLICK_INTERVAL;
	}

	int getPreventClickInterval() {
		if (preventClickInterval > 0) return preventClickInterval;
		return PREVENT_CLICK_INTERVAL;
	}
	//	private void animatePageFlip( final int dir ) {
//		animatePageFlip(dir, null);
//	}
	private void animatePageFlip(final int dir, final Runnable onFinishHandler) {
		if (!mOpened)
			return;
		if (mAvgDrawAnimationStats != null) mAvgDrawAnimationStats.mUpdCnt++;
		BackgroundThread.instance().executeBackground(() -> {
			BackgroundThread.ensureBackground();
			if (currentAnimation == null) {
				PositionProperties currPos = doc.getPositionProps(null, false);
				if (currPos == null)
					return;
				if (mCurrentPageInfo == null)
					return;
				int w = currPos.pageWidth;
				int h = currPos.pageHeight;
				int dir2 = dir;
//					if (currPos.pageMode == 2)
//						if ( dir2==1 )
//							dir2 = 2;
//						else if ( dir2==-1 )
//							dir2 = -2;
				int speed = getPageFlipAnimationSpeedMs();
				if (onFinishHandler != null)
					speed = getPageFlipAnimationSpeedMs() / 2;
				if (currPos.pageMode != 0) {
					int fromX = dir2>0 ? w : 0;
					int toX = dir2>0 ? 0 : w;
					new PageViewAnimation(ReaderView.this, fromX, w, dir2);
					if (currentAnimation != null) {
						if (currentAnimation != null) {
							nextHiliteId++;
							hiliteRect = null;
							currentAnimation.update(toX, h/2);
							//currentAnimation.move(speed, true);
							// plotn: experiments
							currentAnimation.move(speed, true);
							currentAnimation.stop(-1, -1);
						}
						if (onFinishHandler != null)
							BackgroundThread.instance().executeGUI(onFinishHandler);
					}
				} else {
					//new ScrollViewAnimation(startY, maxY);
					int fromY = dir>0 ? h*7/8 : 0;
					int toY = dir>0 ? 0 : h*7/8;
					new ScrollViewAnimation(ReaderView.this, fromY, h);
					if (currentAnimation != null) {
						if (currentAnimation != null) {
							nextHiliteId++;
							hiliteRect = null;
							currentAnimation.update(w/2, toY);
							// plotn: experiments
							//currentAnimation.move(speed, true);
							currentAnimation.move(speed, true);
							currentAnimation.stop(-1, -1);
						}
						if (onFinishHandler!=null)
							BackgroundThread.instance().executeGUI(onFinishHandler);
					}
				}
			}
		});
	}

	static private Rect tapZoneBounds(int startX, int startY, int maxX, int maxY) {
		if (startX < 0)
			startX=0;
		if (startY < 0)
			startY = 0;
		if (startX > maxX)
			startX = maxX;
		if (startY > maxY)
			startY = maxY;
		int dx = (maxX + 2) / 3;
		int dy = (maxY + 2) / 3;
		int x0 = startX / dx * dx;
		int y0 = startY / dy * dy;
		return new Rect(x0, y0, x0+dx, y0+dy);
	}

	volatile public int nextHiliteId = 0;
	private final static int HILITE_RECT_ALPHA = 64;
	private Rect hiliteRect = null;

	void unhiliteTapZone() {
		hiliteTapZone( false, 0, 0, surface.getWidth(), surface.getHeight() );
	}

	void hiliteTapZone(final boolean hilite, final int startX, final int startY, final int maxX, final int maxY) {
		alog.d("highliteTapZone(" + startX + ", " + startY + ")");
		final int myHiliteId = ++nextHiliteId;
		int txcolor = mSettings.getColor(PROP_FONT_COLOR, Color.BLACK);
		final int color = (txcolor & 0xFFFFFF) | (HILITE_RECT_ALPHA << 24);
		BackgroundThread.instance().executeBackground(() -> {
			if (myHiliteId != nextHiliteId || (!hilite && hiliteRect == null))
				return;

			if (currentAutoScrollAnimation != null) {
				hiliteRect = null;
				return;
			}

			BackgroundThread.ensureBackground();
			final BitmapInfo pageImage = preparePageImage(0);
			if (pageImage != null && pageImage.bitmap != null && pageImage.position != null) {
				//PositionProperties currPos = pageImage.position;
				final Rect rc = hilite ? tapZoneBounds(startX, startY, maxX, maxY) : hiliteRect;
				if (hilite)
					hiliteRect = rc;
				else
					hiliteRect = null;
				if (rc != null)
					drawCallback(canvas -> {
						if (mInitialized && mCurrentPageInfo != null)
							if (mCurrentPageInfo.bitmap != null) {
								log.d("onDraw() -- drawing page image");
								Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
								Rect src = new Rect(0, 0, mCurrentPageInfo.bitmap.getWidth(), mCurrentPageInfo.bitmap.getHeight());
								drawDimmedBitmap(canvas, mCurrentPageInfo.bitmap, src, dst);
								if (hilite) {
									Paint p = new Paint();
									p.setStyle(Paint.Style.FILL);
									p.setColor(color);
									int w = (int)(2.0f*mActivity.getDensityFactor());
	//					    			if ( true ) {
									canvas.drawRect(new Rect(rc.left, rc.top, rc.right - w, rc.top + w), p);
									canvas.drawRect(new Rect(rc.left, rc.top + w, rc.left + w, rc.bottom - w), p);
									canvas.drawRect(new Rect(rc.right - w - w, rc.top + w, rc.right - w, rc.bottom - w), p);
									canvas.drawRect(new Rect(rc.left + w, rc.bottom - w - w, rc.right - w - w, rc.bottom - w), p);
	//					    			} else {
	//					    				canvas.drawRect(rc, p);
	//					    			}
								}
							} else
								log.w("onDraw() -- cannot draw - page bitmap is null");
					}, rc, false);
			}
		});
	}

	void scheduleUnhilite(int delay) {
		final int myHiliteId = nextHiliteId;
		BackgroundThread.instance().postGUI(() -> {
			if (myHiliteId == nextHiliteId && hiliteRect != null)
				unhiliteTapZone();
		}, delay);
	}

	boolean getBacklightEnabled(int backLightSetting, boolean isLeft, boolean isCold) {
		if ((isLeft) && (isCold))
			if (
					(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_BOTH) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_COLD_RIGHT_WARM) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_BOTH_RIGHT_COLD) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_BOTH_RIGHT_WARM) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_RIGHT_BOTH_LEFT_COLD) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_BOTH_BOTH)
			) return true;
		if ((!isLeft) && (isCold))
			if (
					(backLightSetting == BACKLIGHT_CONTROL_FLICK_RIGHT) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_BOTH) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_WARM_RIGHT_COLD) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_RIGHT_BOTH_LEFT_WARM) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_BOTH_RIGHT_COLD) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_RIGHT_BOTH_LEFT_COLD) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_BOTH_BOTH)
			) return true;
		if ((isLeft) && (!isCold))
			if (
					(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_BOTH) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_WARM_RIGHT_COLD) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_BOTH_RIGHT_COLD) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_BOTH_RIGHT_WARM) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_RIGHT_BOTH_LEFT_WARM) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_BOTH_BOTH)
			) return true;
		if ((!isLeft) && (!isCold))
			if (
					(backLightSetting == BACKLIGHT_CONTROL_FLICK_RIGHT) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_BOTH) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_COLD_RIGHT_WARM) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_RIGHT_BOTH_LEFT_COLD) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_LEFT_BOTH_RIGHT_WARM) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_RIGHT_BOTH_LEFT_WARM) ||
							(backLightSetting == BACKLIGHT_CONTROL_FLICK_BOTH_BOTH)
			) return true;
		return false;
	}

	int currentBrightnessValueIndex = -1;
	int currentBrightnessValue = -1;
	int currentBrightnessValueIndexCold = -1;
	int currentBrightnessValueCold = -1;
	int currentBrightnessValueIndexWarm = -1;
	int currentBrightnessValueWarm = -1;
	int currentBrightnessPrevYPos = -1;

	void startBrightnessControl(final int startX, final int startY, int type) {
		// for onyx double light type will be overriden with exact values for both sides
		// not eink or not onyx
		log.i("startBrightnessControl called");
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) || (!DeviceInfo.EINK_ONYX)) {
			log.i("startBrightnessControl: device is not onyx or not eink");
			currentBrightnessValue = mActivity.getScreenBacklightLevel();
			currentBrightnessValueIndex = OptionsDialog.findBacklightSettingIndex(currentBrightnessValue);
			if (0 == currentBrightnessValueIndex) {		// system backlight level
				// A trick that allows you to reduce the brightness of the backlight
				// if the brightness is set to the same as in the system.
				currentBrightnessValue = 50;
				currentBrightnessValueIndex = OptionsDialog.findBacklightSettingIndex(currentBrightnessValue);
			}
			currentBrightnessValueCold = currentBrightnessValue;
			currentBrightnessValueIndexCold = currentBrightnessValueIndex;
		} else {
			log.i("startBrightnessControl: device is onyx");
			if (DeviceInfo.EINK_HAVE_FRONTLIGHT) {
				int initialBacklight = mEinkScreen.getFrontLightValue(mActivity);
				if (initialBacklight != -1) currentBrightnessValueCold = initialBacklight;
				else currentBrightnessValueCold = mActivity.getScreenBacklightLevel();
				if (null != mEinkScreen)
					currentBrightnessValueIndexCold = Utils.findNearestIndex(mEinkScreen.getFrontLightLevels(mActivity), currentBrightnessValueCold);
			}
			if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT) {
				int initialWarmBacklight = mEinkScreen.getWarmLightValue(mActivity);
				if (initialWarmBacklight != -1) currentBrightnessValueWarm = initialWarmBacklight;
				else currentBrightnessValueWarm = mActivity.getScreenBacklightLevel();
				if (null != mEinkScreen)
					currentBrightnessValueIndexWarm = Utils.findNearestIndex(mEinkScreen.getWarmLightLevels(mActivity), currentBrightnessValueWarm);
			}
		}
		currentBrightnessPrevYPos = startY;
		updateBrightnessControl(startY, type);
	}

	void updateBrightnessControl(final int y, int type) {
		boolean bLeftCold = getBacklightEnabled(isBacklightControlFlick, true, true);
		boolean bRightCold = getBacklightEnabled(isBacklightControlFlick, false, true);
		boolean bLeftWarm = getBacklightEnabled(isBacklightControlFlick, true, false);
		boolean bRightWarm = getBacklightEnabled(isBacklightControlFlick, false, false);
		boolean bOnyxLight = false;
		boolean bOnyxWarmLight = false;
//		log.i("bLeftCold = " + bLeftCold);
//		log.i("bRightCold = " + bRightCold);
//		log.i("bLeftWarm = " + bLeftWarm);
//		log.i("bRightWarm = " + bRightWarm);

		List<Integer> levelListCold = null;
		int countCold = 0;
		List<Integer> levelListWarm = null;
		int countWarm = 0;
		if ((!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) || (!DeviceInfo.EINK_ONYX)) {
			countCold = BacklightOption.mBacklightLevels.length;
			log.i("updateBrightnessControl: device is not onyx or not eink, count = " + countCold);
		} else {
			log.i("updateBrightnessControl: device is onyx");
			if (DeviceInfo.EINK_HAVE_FRONTLIGHT) {
				bOnyxLight = true;
				if (null != mEinkScreen)
					levelListCold = mEinkScreen.getFrontLightLevels(mActivity);
				if (null != levelListCold) {
					countCold = levelListCold.size();
				}
			}
			if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT) {
				bOnyxWarmLight = true;
				if (null != mEinkScreen)
					levelListWarm = mEinkScreen.getWarmLightLevels(mActivity);
				if (null != levelListWarm) {
					countWarm = levelListWarm.size();
				}
			}
		}
		// plotn: added 1.3 koef so it was very slow change - through all the screen height
		int sens = mSettings.getInt(PROP_APP_BACKLIGHT_SWIPE_SENSIVITY, 0);
		float koef = 1.4f;
		if (sens == 1) koef = 1.0f;
		if (sens == 2) koef = 1.2f;
		if (sens == 3) koef = 1.7f;
		if (sens == 4) koef = 2.0f;

		int diffCold = (int) koef * countCold * (currentBrightnessPrevYPos - y)/surface.getHeight();
		int diffWarm = (int) koef * countWarm * (currentBrightnessPrevYPos - y)/surface.getHeight();
		log.i("countWarm = " + countWarm);
		log.i("countCold = " + countCold);
		log.i("diffWarm = " + diffWarm);
		log.i("diffCold = " + diffCold);
		log.i("currentBrightnessPrevYPos = " + currentBrightnessPrevYPos);
		log.i("y = " + y);
		if ((diffCold == 0) && (diffWarm == 0)) return;
		int indexCold = currentBrightnessValueIndexCold;
		if ((!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) || (!DeviceInfo.EINK_ONYX)) {
			indexCold = currentBrightnessValueIndex;
			log.i("updateBrightnessControl: device is not onyx or not eink, index = " + indexCold);
		}
		int indexWarm = currentBrightnessValueIndexWarm;
		if (type == BRIGHTNESS_TYPE_LEFT_SIDE) {
			if (bLeftCold) indexCold = currentBrightnessValueIndexCold + diffCold;
			if (bLeftWarm) indexWarm = currentBrightnessValueIndexWarm + diffWarm;
			if ((bLeftCold) && (bLeftWarm) && (backlightFixDelta)) {
				while ((indexCold < 0) || (indexWarm < 0)) {
					indexCold++; indexWarm++;
				}
				while ((indexCold >= countCold) || (indexWarm >= countWarm)) {
					indexCold--; indexWarm--;
				}
			}
		}
		if (type == BRIGHTNESS_TYPE_RIGHT_SIDE) {
			if (bRightCold) indexCold = currentBrightnessValueIndexCold + diffCold;
			if (bRightWarm) indexWarm = currentBrightnessValueIndexWarm + diffWarm;
			if ((bLeftCold) && (bLeftWarm) && (backlightFixDelta)) {
				while ((indexCold < 0) || (indexWarm < 0)) {
					indexCold++; indexWarm++;
				}
				while ((indexCold >= countCold) || (indexWarm >= countWarm)) {
					indexCold--; indexWarm--;
				}
			}
		}
		if (type == BRIGHTNESS_TYPE_COLD_VAL) {
			indexCold = currentBrightnessValueIndexCold + diffCold;
			indexWarm = currentBrightnessValueIndexWarm;
		}
		if (type == BRIGHTNESS_TYPE_WARM_VAL) {
			indexCold = currentBrightnessValueIndexCold;
			indexWarm = currentBrightnessValueIndexWarm + diffWarm;
		}
		if ((type == BRIGHTNESS_TYPE_COLD_VAL) || (type == BRIGHTNESS_TYPE_WARM_VAL)) {
			if (backlightFixDelta) {
				while ((indexCold < 0) || (indexWarm < 0)) {
					indexCold++; indexWarm++;
				}
				while ((indexCold >= countCold) || (indexWarm >= countWarm)) {
					indexCold--; indexWarm--;
				}
			}
		}
		if (indexCold < 0) {
			indexCold = 0;
		}
		else if (indexCold >= countCold)
			indexCold = countCold - 1;
		if (indexWarm < 0)
			indexWarm = 0;
		else if (indexWarm >= countWarm)
			indexWarm = countWarm - 1;
		if ((!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) && (!DeviceInfo.EINK_ONYX)) {
			if (indexCold == 0) {
				// ignore system brightness level on non eink devices
				currentBrightnessPrevYPos = y;
				return;
			}
		}
		log.i("indexCold = " + indexCold);
		log.i("currentBrightnessValueIndexCold = " + currentBrightnessValueIndexCold);
		if (indexCold != currentBrightnessValueIndexCold) {
//			log.i("setting cold from " + currentBrightnessValueIndexCold + " to " + indexCold);
			currentBrightnessValueIndexCold = indexCold;
			if (levelListCold == null)
				currentBrightnessValueCold = BacklightOption.mBacklightLevels[currentBrightnessValueIndexCold];
			else
				currentBrightnessValueCold = levelListCold.get(currentBrightnessValueIndexCold);
			currentBrightnessValueIndex = currentBrightnessValueIndexCold;
			currentBrightnessValue = currentBrightnessValueCold;
			mActivity.setScreenBacklightLevel(currentBrightnessValueCold);
		}
		if (bOnyxWarmLight)
			if (indexWarm != currentBrightnessValueIndexWarm) {
				//			log.i("setting warm from " + currentBrightnessValueIndexWarm + " to " + indexWarm);
				currentBrightnessValueIndexWarm = indexWarm;
				currentBrightnessValueWarm = levelListWarm.get(currentBrightnessValueIndexWarm);
				mActivity.setScreenWarmBacklightLevel(currentBrightnessValueWarm);
			}
		currentBrightnessPrevYPos = y;
		if ((!bOnyxLight) && (!bOnyxWarmLight)) {
			if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
				if ((type != BRIGHTNESS_TYPE_COLD_VAL) && (type != BRIGHTNESS_TYPE_WARM_VAL))
					showCenterPopup(currentBrightnessValue + "%", 2000, true);
			// mr Kaz asked to turn off backlight on eink - so let it be
			//else
			//	showBottomPopup(currentBrightnessValue + "%");
		} else {
			float percentLevelCold;
			float percentLevelWarm;
			percentLevelCold = 100 * currentBrightnessValueCold / (float) DeviceInfo.MAX_SCREEN_BRIGHTNESS_VALUE;
			percentLevelWarm = 100 * currentBrightnessValueWarm / (float) DeviceInfo.MAX_SCREEN_BRIGHTNESS_WARM_VALUE;
			String sVal = currentBrightnessValueCold + "% / " + currentBrightnessValueWarm + "%";
			if (percentLevelCold < 10)
				sVal = String.format("%1$.1f%%", percentLevelCold);
			else
				sVal = String.format("%1$.0f%%", percentLevelCold);
			if (percentLevelWarm < 10)
				sVal = sVal + " / " + String.format("%1$.1f%%", percentLevelWarm);
			else
				sVal = sVal + " / " + String.format("%1$.0f%%", percentLevelWarm);
			// mr Katz asked to turn off backlight indication on eink - so let it be
			// showBottomPopup(sVal);
		}
	}

	void stopBrightnessControl(final int x, final int y, int type) {
		if (
				(currentBrightnessValueIndex >= 0) ||
						(currentBrightnessValueIndexCold >= 0) ||
						(currentBrightnessValueIndexWarm >= 0)
		) {
			if (x >= 0 && y >= 0) {
				updateBrightnessControl(y, type);
			}
			if (currentBrightnessValueIndex >= 0)
				mSettings.setInt(PROP_APP_SCREEN_BACKLIGHT, currentBrightnessValue);
			if (currentBrightnessValueIndexCold >= 0)
				mSettings.setInt(PROP_APP_SCREEN_BACKLIGHT, currentBrightnessValueCold);
			if (currentBrightnessValueIndexWarm >= 0)
				mSettings.setInt(PROP_APP_SCREEN_WARM_BACKLIGHT, currentBrightnessValueWarm);
			if (showBrightnessFlickToast) {
				BacklightOption.mBacklightLevelsTitles[0] = mActivity.getString(R.string.options_app_backlight_screen_default);
				String s = BacklightOption.mBacklightLevelsTitles[currentBrightnessValueIndex];
				mActivity.showToast(s);
			}
			if (!DeviceInfo.EINK_SCREEN)
				saveSettings(mSettings);
			currentBrightnessValue = -1;
			currentBrightnessValueIndex = -1;
			currentBrightnessValueCold = -1;
			currentBrightnessValueIndexCold = -1;
			currentBrightnessValueWarm = -1;
			currentBrightnessValueIndexWarm = -1;
			currentBrightnessPrevYPos = -1;
		}
	}

	private static final boolean showBrightnessFlickToast = false;


	void startAnimation(final int startX, final int startY, final int maxX, final int maxY, final int newX, final int newY) {
		if (!mOpened)
			return;
		alog.d("startAnimation("+startX + ", " + startY+")");
		BackgroundThread.instance().executeBackground(() -> {
			BackgroundThread.ensureBackground();
			PositionProperties currPos = doc.getPositionProps(null, false);
			if (currPos != null && currPos.pageMode != 0) {
				//int dir = startX > maxX/2 ? currPos.pageMode : -currPos.pageMode;
				//int dir = startX > maxX/2 ? 1 : -1;
				int dir = newX - startX < 0 ? 1 : -1;
				int sx = startX;
//					if ( dir<0 )
//						sx = 0;
				new PageViewAnimation(ReaderView.this, sx, maxX, dir);
			} else {
				new ScrollViewAnimation(ReaderView.this, startY, maxY);
			}
			if (currentAnimation != null) {
				nextHiliteId++;
				hiliteRect = null;
			}
		});
	}

	private volatile int updateSerialNumber = 0;
	private class AnimationUpdate {
		private int x;
		private int y;
		//ViewAnimationControl myAnimation;
		public void set(int x, int y) {
			this.x = x;
			this.y = y;
		}
		public AnimationUpdate(int x, int y) {
			this.x = x;
			this.y = y;
			//this.myAnimation = currentAnimation;
			scheduleUpdate();
		}
		private void scheduleUpdate() {
			BackgroundThread.instance().postBackground(() -> {
				alog.d("updating("+x + ", " + y+")");
				boolean animate = false;
				synchronized (AnimationUpdate.class) {

					if (currentAnimation != null && currentAnimationUpdate == AnimationUpdate.this) {
						currentAnimationUpdate = null;
						currentAnimation.update(x, y);
						animate = true;
					}
				}
				if (animate)
					currentAnimation.animate();
			});
		}

	}
	private AnimationUpdate currentAnimationUpdate;
	void updateAnimation(final int x, final int y) {
		if (!mOpened)
			return;
		alog.d("updateAnimation("+x + ", " + y+")");
		synchronized(AnimationUpdate.class) {
			if (currentAnimationUpdate != null)
				currentAnimationUpdate.set(x, y);
			else
				currentAnimationUpdate = new AnimationUpdate(x, y);
		}
		try {
			// give a chance to background thread to process event faster
			Thread.sleep(0);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	void stopAnimation(final int x, final int y) {
		if (!mOpened)
			return;
		alog.d("stopAnimation("+x+", "+y+")");
		BackgroundThread.instance().executeBackground(() -> {
			if (currentAnimation != null) {
				currentAnimation.stop(x, y);
			}
		});
	}

	DelayedExecutor animationScheduler = DelayedExecutor.createBackground("animation");
	void scheduleAnimation() {
		if (!mOpened)
			return;
		animationScheduler.post(() -> {
			if (currentAnimation != null) {
				currentAnimation.animate();
			}
		});
	}

	interface ViewAnimationControl {
		public void update(int x, int y);
		public void stop(int x, int y);
		public void animate();
		public void move(int duration, boolean accelerated);
		public boolean isStarted();
		abstract void draw( Canvas canvas );
	}

//	private Object surfaceLock = new Object();

	private static final int[] accelerationShape = new int[] {
			0, 6, 24, 54, 95, 146, 206, 273, 345, 421, 500, 578, 654, 726, 793, 853, 904, 945, 975, 993, 1000
	};
	static public int accelerate(int x0, int x1, int x) {
		if (x < x0)
			x = x0;
		if (x > x1)
			x = x1;
		int intervals = accelerationShape.length - 1;
		int pos = x1 > x0 ? 100 * intervals * (x - x0) / (x1-x0) : x1;
		int interval = pos / 100;
		int part = pos % 100;
		if (interval<0)
			interval = 0;
		else if (interval > intervals)
			interval = intervals;
		int y = interval == intervals ? 100000 : accelerationShape[interval] * 100 + (accelerationShape[interval + 1]-accelerationShape[interval]) * part;
		return x0 + (x1 - x0) * y / 100000;
	}

	public void drawCallback(DrawCanvasCallback callback, Rect rc, boolean isPartially)
	{
		if (!mSurfaceCreated)
			return;
		//synchronized(surfaceLock) { }
		//log.v("draw() - in thread " + Thread.currentThread().getName());
		final SurfaceHolder holder = surface.getHolder();
		//log.v("before synchronized(surfaceLock)");
		if (holder != null)
		//synchronized(surfaceLock)
		{
			Canvas canvas = null;
			long startTs = android.os.SystemClock.uptimeMillis();
			if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
				// pre draw update
				//BackgroundThread.instance().executeGUI(() -> EinkScreen.PrepareController(surface, isPartially));
				mEinkScreen.prepareController(surface, isPartially);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				try {
					canvas = holder.lockHardwareCanvas();
				} catch (Exception e) {
					log.e("drawCallback() -> lockHardwareCanvas(): " + e.toString());
				}
			}
			try {
				if (canvas == null)
					canvas = holder.lockCanvas(rc);
				//log.v("before draw(canvas)");
				if (canvas != null) {
					callback.drawTo(canvas);
				}
			} finally {
				//log.v("exiting finally");
				if (canvas != null && surface.getHolder() != null) {
					//log.v("before unlockCanvasAndPost");
					holder.unlockCanvasAndPost(canvas);
					if ( rc == null && currentAnimation != null ) {

						long endTs = android.os.SystemClock.uptimeMillis();
						updateAnimationDurationStats(endTs - startTs);
					}
					if (DeviceInfo.EINK_SCREEN) {
						// post draw update
						mEinkScreen.updateController(surface, isPartially);
					}
					//log.v("after unlockCanvasAndPost");
				}
			}
		}
		//log.v("exiting draw()");
	}

	//private static final int PAGE_ANIMATION_DURATION = 3000;

	public final static int SIN_TABLE_SIZE = 1024;
	public final static int SIN_TABLE_SCALE = 0x10000;
	public final static int PI_DIV_2 = (int)(Math.PI / 2 * SIN_TABLE_SCALE);
	/// sin table, for 0..PI/2
	public static int[] SIN_TABLE = new int[SIN_TABLE_SIZE+1];
	public static int[] ASIN_TABLE = new int[SIN_TABLE_SIZE+1];
	// mapping of 0..1 shift to angle
	public static int[] SRC_TABLE = new int[SIN_TABLE_SIZE+1];
	// mapping of 0..1 shift to sin(angle)
	public static int[] DST_TABLE = new int[SIN_TABLE_SIZE+1];
	// for dx=0..1 find such alpha (0..pi/2) that alpha - sin(alpha) = dx
	private static double shiftfn( double dx ) {
		double a = 0;
		double b = Math.PI/2;
		double c = 0;
		for (int i=0; i<15; i++) {
			c = (a + b) / 2;
			double cq = c - Math.sin(c);
			if (cq < dx)
				a = c;
			else
				b = c;
		}
		return c;
	}
	static {
		for (int i=0; i<=SIN_TABLE_SIZE; i++) {
			double angle = Math.PI / 2 * i / SIN_TABLE_SIZE;
			int s = (int)Math.round(Math.sin(angle) * SIN_TABLE_SCALE);
			SIN_TABLE[i] = s;
			double x = (double)i / SIN_TABLE_SIZE;
			s = (int)Math.round(Math.asin(x) * SIN_TABLE_SCALE);
			ASIN_TABLE[i] = s;

			double dx = i * (Math.PI/2 - 1.0) / SIN_TABLE_SIZE;
			angle = shiftfn( dx );
			SRC_TABLE[i] = (int)Math.round(angle * SIN_TABLE_SCALE);
			DST_TABLE[i] = (int)Math.round(Math.sin(angle) * SIN_TABLE_SCALE);
		}
	}

	private String mLogFileRoot = "";

	RingBuffer mAvgDrawAnimationStats = new RingBuffer(this, 32, 50);

	long getAvgAnimationDrawDuration() {
		CustomLog.doLog(mLogFileRoot, "log_animation.log",
				"getAvgAnimationDrawDuration: " + mAvgDrawAnimationStats.average());
		return mAvgDrawAnimationStats.average();
	}

	private void updateAnimationDurationStats(long duration) {
		if (duration <= 0)
			duration = 1;
		else if (duration > 1000)
			return;
		CustomLog.doLog(mLogFileRoot, "log_animation.log",
				"mAvgDrawAnimationStats.add: " + duration);
		mAvgDrawAnimationStats.add(duration);
	}

	private boolean checkNeedRedraw() {
		return checkNeedRedraw(internalDX, internalDY);
	}

	public boolean bNeedRedrawOnce = false;

	private boolean checkNeedRedraw(int x, int y) {
		boolean bSkipRedraw = false;
		Iterator it = mActivity.getmBaseDialog().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			try {
				BaseDialog sVal = (BaseDialog) pair.getValue();
				if (sVal.isShowing()) {
					log.i("Dialog "+pair.getKey()+" is shown now, skip redraw reader view");
					return false;
				}
			} catch (Exception e) {
				log.w("Could not check the dialogs...");
			}
		}
		if (bSkipRedraw) return true;
		boolean bChanged = false;
		String sProp = x+"."+y;
		boolean bNeedRedraw = !getSettings().getBool(Settings.PROP_SKIPPED_RES+"."+sProp,false);
		boolean bFound = false;
		for (ResizeHistory rh: getActivity().getResizeHist()) {
			if (
					(rh.X == x) && (rh.Y == y)
			) {
				bFound = true;
			}
		}
		if (!bFound) {
			ResizeHistory rh = getActivity().getNewResizeHistory();
			rh.X=x;
			rh.Y=y;
			rh.wasX=wasX;
			rh.wasY=wasY;
			rh.cnt=0;
			rh.lastSet=System.currentTimeMillis();
			getActivity().getResizeHist().add(rh);
			bChanged = true;
		}
		if (bChanged) getActivity().saveResizeHistory();
		return bNeedRedraw;
	}

	void drawPage()
	{
		drawPage(null, false);
	}

	private void drawPage(boolean isPartially)
	{
		drawPage(null, isPartially);
	}

	void drawPage(Runnable doneHandler, boolean isPartially)
	{
		if (!mInitialized)
			return;
		if ((nowFormatting) || (docIsLoading))
			return;
		log.v("drawPage() : submitting DrawPageTask");
		// evaluate if we need to redraw page on this resolution
		if (mOpened)
			scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
		post(new DrawPageTask(ReaderView.this, doneHandler, isPartially));
	}

	public int internalDX = 300;
	public int internalDY = 300;

	public byte[] coverPageBytes = null;
	void findCoverPage()
	{
		log.d("document is loaded succesfull, checking coverpage data");
		byte[] coverpageBytes = doc.getCoverPageData();
		if (coverpageBytes != null) {
			log.d("Found cover page data: " + coverpageBytes.length + " bytes");
			coverPageBytes = coverpageBytes;
		}
	}

	public int currentProgressPosition = 1;
	private int currentProgressTitleId = R.string.progress_loading;
	public String currentProgressTitle = null;
	public int currentCloudSyncProgressPosition = -1;
	private int savedEinkUpdateInterval = -1;
	private final HashSet<Integer> einkModeClients = new HashSet<Integer>();

	private void requestDisableFullRefresh(int id) {
		if (-1 == savedEinkUpdateInterval) {
			savedEinkUpdateInterval = mEinkScreen.getUpdateInterval();
			// current e-ink screen update mode without full refresh
			mEinkScreen.setupController(mEinkScreen.getUpdateMode(), 0, surface, false, false);
		}
		einkModeClients.add(id);
	}

	private void releaseDisableFullRefresh(int id) {
		einkModeClients.remove(id);
		if (einkModeClients.isEmpty()) {
			// restore e-ink full screen refresh period
			mEinkScreen.setupController(mEinkScreen.getUpdateMode(), savedEinkUpdateInterval, surface, false, false);
			savedEinkUpdateInterval = -1;
		}
	}

	private boolean inDisabledFullRefresh() {
		return !einkModeClients.isEmpty();
	}

	private void showProgress(int position, int titleResource) {
		int pos = position / 100;
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
			showCenterPopup(pos + "% " + mActivity.getString(titleResource), -1, false);
		else
			showBottomPopup(pos + "% " + mActivity.getString(titleResource), -1, false);
		boolean first = currentProgressTitleId == 0;
		if (currentProgressPosition != position || currentProgressTitleId != titleResource) {
			currentProgressPosition = position;
			currentProgressTitleId = titleResource;
			currentProgressTitle = mActivity.getString(currentProgressTitleId);
			//if (first) bookView.draw(!first);
		}
	}

//	private void showProgress(int position, int titleResource) {
//		log.v("showProgress(" + position + ")");
//		boolean first = currentProgressTitleId == 0;
//		boolean update = false;
//		if (null == currentProgressTitle || currentProgressTitleId != titleResource) {
//			currentProgressTitleId = titleResource;
//			currentProgressTitle = mActivity.getString(currentProgressTitleId);
//			update = true;
//		}
//		if (currentProgressPosition != position || currentProgressTitleId != titleResource) {
//			currentProgressPosition = position;
//			update = true;
//		}
//		if (update) {
//			if (DeviceInfo.EINK_SCREEN)
//				requestDisableFullRefresh(1);
//			bookView.draw(!first);
//		}
//	}

	void hideProgress() {
		mActivity.scheduleHideWindowCenterPopup(1);
		if (currentProgressTitleId != 0) {
			currentProgressPosition = -1;
			currentProgressTitleId = 0;
			currentProgressTitle = null;
			//bookView.draw(false);
		}
	}

//	private void hideProgress() {
//		log.v("hideProgress()");
//		if (currentProgressTitleId != 0) {
//			currentProgressPosition = -1;
//			currentProgressTitleId = 0;
//			currentProgressTitle = null;
//			if (DeviceInfo.EINK_SCREEN)
//				releaseDisableFullRefresh(1);
//			bookView.draw(false);
//		}
//	}

	boolean isProgressActive() {
		return currentProgressPosition > 0;
	}

	public void showCloudSyncProgress(int progress) {
		log.v("showClodSyncProgress(" + progress + ")");
		if (currentCloudSyncProgressPosition != progress) {
			currentCloudSyncProgressPosition = progress;
			if (DeviceInfo.EINK_SCREEN)
				requestDisableFullRefresh(2);
			bookView.draw(true);
		}
	}

	public void hideCloudSyncProgress() {
		log.v("hideCloudSyncProgress()");
		if (currentCloudSyncProgressPosition != -1) {
			currentCloudSyncProgressPosition = -1;
			if (DeviceInfo.EINK_SCREEN)
				releaseDisableFullRefresh(2);
			bookView.draw(false);
		}
	}

	boolean isCloudSyncProgressActive() {
		return currentCloudSyncProgressPosition > 0;
	}

	public void checkOpenBookStyles(boolean force, boolean withDisableButton) {
		boolean bDontAsk = mActivity.settings().getBool(Settings.PROP_APP_HIDE_CSS_WARNING, false);
		int showUD = mActivity.settings().getInt(Settings.PROP_APP_SHOW_USER_DIC_PANEL, 0);
		bDontAsk = bDontAsk || (showUD != 0);
		if (!withDisableButton) bDontAsk = false;
		if (!bDontAsk)
			BackgroundThread.instance().postGUI(() -> {
				if (getBookInfo() != null) {
					int iCurPage = getDoc().getCurPage();
					if ((iCurPage<3) || (force)) {
						ArrayList<String> sButtons = new ArrayList<String>();
						boolean isTextFormat = isTextFormat();
						boolean isEpubFormat = isFormatWithEmbeddedFonts();
						boolean isHtmlFormat = isHtmlFormat();
						String yes = mActivity.getString(R.string.str_yes).toUpperCase();
						String no = mActivity.getString(R.string.str_no).toUpperCase();
						String styles = getDocumentStylesEnabled()? yes:no;
						sButtons.add("*"+styles+": "+mActivity.getString(R.string.opened_doc_props_styles));
						if (getDocumentStylesEnabled()) {
							sButtons.add("*"+mActivity.getString(R.string.opened_doc_props_styles_ext));
						}
						if (isEpubFormat) {
							String fonts = getDocumentFontsEnabled()? yes:no;
							sButtons.add("*"+fonts+": "+mActivity.getString(R.string.opened_doc_props_fonts));
						}
						if (isTextFormat) {
							String texta = isTextAutoformatEnabled()? yes:no;
							sButtons.add("*" + texta+ ": "+ mActivity.getString(R.string.opened_doc_props_text));
						}
						if (isHtmlFormat) {
							int preset = getBlockRenderingFlags();
							int dom = getDOMVersion();
							String spreset = "?";
							String sdom = "?";
							if (preset == Engine.BLOCK_RENDERING_FLAGS_LEGACY) spreset = mActivity.getString(R.string.options_rendering_preset_legacy);
							if (preset == Engine.BLOCK_RENDERING_FLAGS_FLAT) spreset = mActivity.getString(R.string.options_rendering_preset_flat);
							if (preset == Engine.BLOCK_RENDERING_FLAGS_BOOK) spreset = mActivity.getString(R.string.options_rendering_preset_book);
							if (preset == Engine.BLOCK_RENDERING_FLAGS_WEB) spreset = mActivity.getString(R.string.options_rendering_preset_web);
							if (dom == 0) sdom = mActivity.getString(R.string.options_requested_dom_level_legacy);
							if (dom != 0) sdom = mActivity.getString(R.string.options_requested_dom_level_newest);
							sButtons.add("*" + spreset +": " + mActivity.getString(R.string.options_rendering_preset));
							sButtons.add("*" + sdom +": " + mActivity.getString(R.string.options_requested_dom_level));
						}
						sButtons.add(mActivity.getString(R.string.str_change));
						sButtons.add("*"+mActivity.getString(R.string.later_css));
						if (withDisableButton)
							sButtons.add(mActivity.getString(R.string.str_disable_this_dialog));
						SomeButtonsToolbarDlg.showDialog(mActivity, ReaderView.this.getSurface(), withDisableButton? 10: 0, true,
								mActivity.getString(R.string.opened_doc_props),
								sButtons, null, (o, btnPressed) -> {
									if (btnPressed.equals(mActivity.getString(R.string.str_change))) {
										mActivity.optionsFilter = "";
										mActivity.showOptionsDialogTab(OptionsDialog.Mode.READER, 1);
									}
									if (btnPressed.equals(mActivity.getString(R.string.str_disable_this_dialog))) {
										mActivity.settings().setBool(Settings.PROP_APP_HIDE_CSS_WARNING, true);
									}
								});
					}
				}
			}, 200);
	}

	public final static boolean dontStretchWhileDrawing = true;
	public final static boolean centerPageInsteadOfResizing = true;

	private void dimRect( Canvas canvas, Rect dst ) {
		if ((DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))&&(!DeviceInfo.SCREEN_CAN_CONTROL_BRIGHTNESS))
			return; // no backlight
		int alpha = dimmingAlpha;
		if (alpha != 255) {
			Paint p = new Paint();
			p.setColor((255-alpha)<<24);
			canvas.drawRect(dst, p);
		}
	}

	private void dimRectAlpha( Canvas canvas, Rect dst, int alpha ) {
		if ((DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))&&(!DeviceInfo.SCREEN_CAN_CONTROL_BRIGHTNESS))
			return; // no backlight
		if (alpha != 255) {
			Paint p = new Paint();
			p.setColor((255-alpha)<<24);
			canvas.drawRect(dst, p);
		}
	}

	void drawDimmedBitmap(Canvas canvas, Bitmap bmp, Rect src, Rect dst) {
		canvas.drawBitmap(bmp, src, dst, null);
		dimRect( canvas, dst );
	}

	void drawDimmedBitmapAlpha(Canvas canvas, Bitmap bmp, Rect src, Rect dst, int alpha) {
		canvas.drawBitmap(bmp, src, dst, null);
		dimRectAlpha( canvas, dst, alpha );
	}

	private void drawDimmedBitmap2( Canvas canvas, Bitmap bmp, Rect src, Rect dst ) {
		Paint p = new Paint(Color.RED);
		//ColorFilter filter = new LightingColorFilter(0xFFFFFFFF , 0x00222222); // lighten
		ColorFilter filter = new LightingColorFilter(0xFFAFAFAF, 0x00111111);    // darken
		p.setColorFilter(filter);
		canvas.drawBitmap(bmp, src, dst, p);
		dimRect( canvas, dst );
	}

	protected void drawPageBackground(Canvas canvas, Rect dst, int side, boolean addDarken) {
		Bitmap bmp = currentBackgroundTextureBitmap;
		if (bmp != null) {
			backgrNormalizedColor = CoverpageManager.getDominantColor(bmp);
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mActivity.getWindow().setNavigationBarColor(backgrNormalizedColor);
			}
			int h = bmp.getHeight();
			int w = bmp.getWidth();
			Rect src = new Rect(0, 0, w, h);
			if (currentBackgroundTextureTiled) {
				// TILED
				for (int x = 0; x < dst.width(); x += w) {
					int ww = w;
					if (x + ww > dst.width())
						ww = dst.width() - x;
					for (int y = 0; y < dst.height(); y += h) {
						int hh = h;
						if (y + hh > dst.height())
							hh = dst.height() - y;
						Rect d = new Rect(x, y, x + ww, y + hh);
						Rect s = new Rect(0, 0, ww, hh);
						if (addDarken)
							drawDimmedBitmap2(canvas, bmp, s, d);
						else
							drawDimmedBitmap(canvas, bmp, s, d);
					}
				}
			} else {
				// STRETCHED
				if (side == VIEWER_TOOLBAR_LONG_SIDE)
					side = canvas.getWidth() > canvas.getHeight() ? VIEWER_TOOLBAR_TOP : VIEWER_TOOLBAR_LEFT;
				else if (side == VIEWER_TOOLBAR_SHORT_SIDE)
					side = canvas.getWidth() < canvas.getHeight() ? VIEWER_TOOLBAR_TOP : VIEWER_TOOLBAR_LEFT;
				switch(side) {
					case VIEWER_TOOLBAR_LEFT:
					{
						int d = dst.width() * dst.height() / h;
						if (d > w)
							d = w;
						src.left = src.right - d;
					}
					break;
					case VIEWER_TOOLBAR_RIGHT:
					{
						int d = dst.width() * dst.height() / h;
						if (d > w)
							d = w;
						src.right = src.left + d;
					}
					break;
					case VIEWER_TOOLBAR_TOP:
					{
						int d = dst.height() * dst.width() / w;
						if (d > h)
							d = h;
						src.top = src.bottom - d;
					}
					break;
					case VIEWER_TOOLBAR_BOTTOM:
					{
						int d = dst.height() * dst.width() / w;
						if (d > h)
							d = h;
						src.bottom = src.top + d;
					}
					break;
				}
				if (addDarken)
					drawDimmedBitmap2(canvas, bmp, src, dst);
				else
					drawDimmedBitmap(canvas, bmp, src, dst);
			}
		} else {
			canvas.drawColor(currentBackgroundColor | 0xFF000000);
			backgrNormalizedColor = currentBackgroundColor;
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mActivity.runOnUiThread(() -> mActivity.getWindow().setNavigationBarColor(backgrNormalizedColor));
			}
		}
	}

	protected void drawBlackPageBackground(Canvas canvas, Rect dst, int side, boolean addDarken) {
		canvas.drawColor(Color.rgb(0, 0, 0));
	}

	protected void drawPageBackground(Canvas canvas) {
		Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
		drawPageBackground(canvas, dst, VIEWER_TOOLBAR_NONE, false);
	}

	protected void drawBlackPageBackground(Canvas canvas) {
		Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
		drawBlackPageBackground(canvas, dst, VIEWER_TOOLBAR_NONE, false);
	}

	public class ToolbarBackgroundDrawable extends Drawable {
		private int location = VIEWER_TOOLBAR_NONE;
		private int alpha;
		public void setLocation(int location) {
			this.location = location;
		}
		@Override
		public void draw(Canvas canvas) {
			Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
			try {
				boolean addDarken = false;
//				if (mActivity instanceof CoolReader) {
//					int optionAppearance = Integer.valueOf(((CoolReader)mActivity).getToolbarAppearance());
//					switch (optionAppearance) {
//						case Settings.VIEWER_TOOLBAR_100_gray:      // 1
//							addDarken = true;
//							break;
//						case Settings.VIEWER_TOOLBAR_75_gray:       // 3
//							addDarken = true;
//							break;
//						case Settings.VIEWER_TOOLBAR_50_gray:       // 5
//							addDarken = true;
//							break;
//					}
//				}
				drawPageBackground(canvas, dst, location, addDarken);
			} catch (Exception e) {
				L.e("Exception in ToolbarBackgroundDrawable.draw", e);
			}
		}
		@Override
		public int getOpacity() {
			return 255 - alpha;
		}
		@Override
		public void setAlpha(int alpha) {
			this.alpha = alpha;

		}
		@Override
		public void setColorFilter(ColorFilter cf) {
			// not supported
		}
	}

	public ToolbarBackgroundDrawable createToolbarBackgroundDrawable() {
		return new ToolbarBackgroundDrawable();
	}

	protected void doDrawProgress(Canvas canvas, int position, String title) {
		// nothing to do - everithing in show progress
		return;
	}

	protected void doDrawProgress(Canvas canvas, int position, String title, boolean transparentFrame) {
		// nothing to do - everithing in show progress
		return;
	}

	protected void doDrawProgressOld(Canvas canvas, int position, String title) {
		log.v("doDrawProgress(" + position + ")");
		if (null == title)
			return;
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		int mins = Math.min(w, h) * 7 / 10;
		int ph = mins / 20;
		int textColor = mSettings.getColor(PROP_FONT_COLOR, 0x000000);
		int fontSize = 15;			// 12pt
		float factor = mActivity.getDensityFactor();
		Rect rc = new Rect(w / 2 - mins / 2, h / 2 - ph / 2, w / 2 + mins / 2, h / 2 + ph / 2);
		Utils.drawFrame(canvas, rc, Utils.createSolidPaint(0xC0000000 | textColor));
		//canvas.drawRect(rc, createSolidPaint(0xFFC0C0A0));
		rc.left += 2;
		rc.right -= 2;
		rc.top += 2;
		rc.bottom -= 2;
		int x = rc.left + (rc.right - rc.left) * position / 10000;
		Rect rc1 = new Rect(rc);
		rc1.right = x;
		canvas.drawRect(rc1, Utils.createSolidPaint(0x80000000 | textColor));
		Paint textPaint = Utils.createSolidPaint(0xFF000000 | textColor);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(fontSize*factor);
		textPaint.setSubpixelText(true);
		canvas.drawText(title, (rc.left + rc.right) / 2, rc1.top - fontSize * factor, textPaint);
		//canvas.drawText(String.valueOf(position * 100 / 10000) + "%", rc.left + 4, rc1.bottom - 4, textPaint);
//		Rect rc2 = new Rect(rc);
//		rc.left = x;
//		canvas.drawRect(rc2, createSolidPaint(0xFFC0C0A0));
	}

	protected void doDrawCloudSyncProgress(Canvas canvas, int position) {
		log.v("doDrawCloudSyncProgress(" + position + ")");
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		int ph = Math.min(w, h)/100;
		if (ph < 5)
			ph = 5;
		int textColor = mSettings.getColor(PROP_FONT_COLOR, 0x000000);
		int pageHeaderPos = mSettings.getInt(PROP_STATUS_LOCATION, VIEWER_STATUS_PAGE_HEADER);
		Rect rc;
		if (VIEWER_STATUS_PAGE_FOOTER == pageHeaderPos)
			rc = new Rect(0, h - ph, w - 1, h - 2);
		else
			rc = new Rect(0, 1, w - 1, ph);
		int x = rc.left + (rc.right - rc.left) * position / 10000;
		Rect rc1 = new Rect(rc);
		rc1.right = x;
		canvas.drawRect(rc1, Utils.createSolidPaint(0x40000000 | textColor));
	}

	public int dimmingAlpha = 255; // no dimming
	public void setDimmingAlpha(int alpha) {
		if (alpha > 255)
			alpha = 255;
		if (alpha < 32)
			alpha = 32;
		if (dimmingAlpha != alpha) {
			dimmingAlpha = alpha;
			mEngine.execute(new Task() {
				@Override
				public void work() throws Exception {
					bookView.draw();
				}

			});
		}
	}

	void restorePositionBackground(String pos, boolean doRedraw) {
		BackgroundThread.ensureBackground();
		if (pos != null) {
			BackgroundThread.ensureBackground();
			doc.goToPosition(pos, false);
			preparePageImage(0);
			if (doRedraw) drawPage();
			updateCurrentPositionStatus();
			checkOpenBookStyles(false, true);
			if (doc.getCurPage()>2) {
				// for safe mode
				String sFile = mActivity.getSettingsFileF(0).getParent() + "/cur_pos0.json";
				File f = new File(sFile);
				if (f.exists()) f.delete();
			}
		}
	}

	private int lastSavePositionTaskId = 0;
	private int lastSavePositionCloudTaskId = 0;

	public int getDefSavePositionInterval() {
		int i = getSettings().getInt(ReaderView.PROP_SAVE_POS_TIMEOUT, 0);
		return (i>=1000) ?i : i*1000*60;
	}

	public int getDefSavePositionIntervalSpeak() {
		return
				(getSettings().getInt(ReaderView.PROP_SAVE_POS_SPEAK_TIMEOUT, 0))*1000;
	}

	private int lastSceduledDelay = 0;
	private int lastSceduledCloudDelay = 0;
	private long lastSceduledTime = 0;
	private long lastSceduledCloudTime = 0;

	public void scheduleSaveCurrentPositionBookmark(final int delayMillis) {
		long nowTime = System.currentTimeMillis();
		long millisSpan = delayMillis;
		if (lastSceduledDelay == delayMillis) {
			if (lastSceduledTime != 0) {
				millisSpan = delayMillis - (nowTime - lastSceduledTime);
				if (millisSpan < 0) millisSpan = 1;
			}
			if (lastSceduledTime == 0) lastSceduledTime = nowTime;
		} else {
			lastSceduledDelay = delayMillis;
			lastSceduledTime = nowTime;
		}
		// GUI thread required
		long finalMillisSpan = millisSpan;
		BackgroundThread.instance().executeGUI(() -> {
			final int mylastSavePositionTaskId = ++lastSavePositionTaskId;
			if (isBookLoaded() && mBookInfo != null) {
				final Bookmark bmk = getCurrentPositionBookmark();
				if (bmk == null)
					return;
				final BookInfo bookInfo = mBookInfo;
				if (delayMillis <= 1) {
					if (bookInfo != null && mActivity.getDB() != null) {
						log.v("saving last position immediately");
						if (savePositionBookmark(bmk)) {
							Services.getHistory().updateBookAccess(bookInfo, getTimeElapsed());
						}
					}
				} else {
					BackgroundThread.instance().postGUI(() -> {
						if (mylastSavePositionTaskId == lastSavePositionTaskId) {
							if (bookInfo != null) {
								log.v("saving last position");
								if (!Services.isStopped()) {
									// this delayed task can be completed after calling CoolReader.onDestroy(),
									// which in turn calls Services.stopServices().
									if (savePositionBookmark(bmk)) {
										Services.getHistory().updateBookAccess(bookInfo, getTimeElapsed());
									}
								}
							}
							lastSceduledTime = 0;
						}
					}, finalMillisSpan);
					boolean bNeedSave = !appPaused;
					int autosaveInterval = 0;
					if (bNeedSave)
						if (lastSavedToGdBookmark!=null) {
							if ((bmk.getStartPos().equals(lastSavedToGdBookmark.getStartPos()))) {
								bNeedSave = false;
							}
						}
					if (bNeedSave) {
						autosaveInterval = (getSettings().getInt(ReaderView.PROP_SAVE_POS_TO_CLOUD_TIMEOUT, 0)) * 1000 * 60;
						bNeedSave = autosaveInterval > 0;
					}
					if (bNeedSave) {
						final int mylastSavePositionCloudTaskId = ++lastSavePositionCloudTaskId;
						long millisCloudSpan = autosaveInterval;
						if (lastSceduledCloudDelay == autosaveInterval) {
							if (lastSceduledCloudTime != 0) {
								millisCloudSpan = autosaveInterval - (nowTime - lastSceduledCloudTime);
								if (millisCloudSpan < 0) millisCloudSpan = 1;
							}
							if (lastSceduledCloudTime == 0) lastSceduledCloudTime = nowTime;
						} else {
							lastSceduledCloudDelay = autosaveInterval;
							lastSceduledCloudTime = nowTime;
						}
						if (millisCloudSpan > 0)
							BackgroundThread.instance().postGUI(() -> {
								if (mylastSavePositionCloudTaskId == lastSavePositionCloudTaskId) {
									if (bookInfo != null) {
										if (!appPaused) {
											mActivity.updateSavingMark("&");
											log.i("Save reading pos to CLOUD");
											lastSavedToGdBookmark = bmk;
											int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
											if (iSyncVariant > 0) {
												CloudSync.saveJsonInfoFileOrCloud(mActivity,
														CloudSync.CLOUD_SAVE_READING_POS, true, iSyncVariant == 1, true);
											}
										}
									}
									lastSceduledCloudTime = 0;
								}
							}, millisCloudSpan);
					}
				}
			}
		});

//    	if (DeviceInfo.EINK_SONY && isBookLoaded()) {
//    		getCurrentPositionProperties(new PositionPropertiesCallback() {
//				@Override
//				public void onPositionProperties(PositionProperties props,
//						String positionText) {
//					// update position for Sony T2
//					if (props != null && mBookInfo != null) {
//						String fname = mBookInfo.getFileInfo().getBasePath();
//						if (fname != null && fname.length() > 0)
//							setBookPositionForExternalShell(fname, props.pageNumber, props.pageCount);
//					}
//				}
//    		});
//    	}
	}

	// Sony T2 update position method - by Jotas
	public void setBookPositionForExternalShell(String filename, long current_page , long total_pages) {
		if (DeviceInfo.EINK_SONY) {
			log.d("Trying to update last book and position in Sony T2 shell: file=" + filename + " currentPage=" + current_page + " totalPages=" + total_pages);
			File f = new File(filename);
			if( f.exists() ) {
				String file_path = f.getAbsolutePath();
				try {
					file_path = f.getCanonicalPath();
				} catch( Exception e ) {
					Log.d("cr3Sony" , "setBookPosition getting filename/path", e);
				}

				try {
					Uri uri = Uri.parse("content://com.sony.drbd.ebook.internal.provider/continuereading");
					ContentValues contentvalues = new ContentValues();
					contentvalues.put("file_path" , file_path);
					contentvalues.put("current_page" , current_page);
					contentvalues.put("total_pages" , total_pages);
					if (mActivity.getContentResolver().insert(uri, contentvalues) != null)
						Log.d("cr3Sony" , "setBookPosition: filename = " + filename + "start=" + current_page + "end=" + total_pages);
					else
						Log.d("crsony" , "setBookPosition : error inserting in database!");

				} catch( Exception e ) {
					Log.d("cr3Sony" , "setBookPositon parse/values!", e);
				}
			}
		}
	}


	public interface PositionPropertiesCallback {
		void onPositionProperties(PositionProperties props, String positionText);
	}
	public void getCurrentPositionProperties(final PositionPropertiesCallback callback) {
		BackgroundThread.instance().postBackground(() -> {
			final Bookmark bmk = (doc != null) ? doc.getCurrentPageBookmarkNoRender() : null;
			final PositionProperties props = (bmk != null) ? doc.getPositionProps(bmk.getStartPos(), true) : null;
			BackgroundThread.instance().postBackground(() -> {
				String posText = null;
				if (props != null) {
					int percent = (int)(10000 * (long)props.y / props.fullHeight);
					String percentText = "" + (percent/100) + "." + (percent%10) + "%";
					posText = "" + props.pageNumber + " / " + props.pageCount + " (" + percentText + ")";
				}
				callback.onPositionProperties(props, posText);
			});
		});
	}


	public Bookmark getCurrentPositionBookmark() {
		if (!mOpened)
			return null;
		Bookmark bmk = doc.getCurrentPageBookmarkNoRender();
		if (bmk != null) {
			bmk.setTimeStamp(System.currentTimeMillis());
			bmk.setType(Bookmark.TYPE_LAST_POSITION);
			if (mBookInfo != null)
				mBookInfo.setLastPosition(bmk);
		}
		return bmk;
	}

	Bookmark lastSavedBookmark = null;

	public boolean savePositionBookmark(Bookmark bmk) {
		if (bmk != null && mBookInfo != null && isBookLoaded()) {
			//setBookPosition();
			if (lastSavedBookmark == null || !lastSavedBookmark.getStartPos().equals(bmk.getStartPos())) {
				if (!Services.isStopped()) {
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					Bookmark bmk2 = new Bookmark(bmk);
					bmk2.bookFile = mBookInfo.getFileInfo().getFilename();
					bmk2.bookPath = mBookInfo.getFileInfo().pathname;
					bmk2.bookFileArc = "";
					if (mBookInfo.getFileInfo().isArchive)
						bmk2.bookFileArc = mBookInfo.getFileInfo().arcname;
					final String prettyJson = gson.toJson(bmk2);
					getActivity().saveCurPosFile(false, prettyJson);
					Services.getHistory().updateRecentDir();
					mActivity.getDB().saveBookInfo(mBookInfo);
					mActivity.getDB().flush();
					lastSavedBookmark = bmk;
					mActivity.updateSavingMark("*");
					saveCurrentBookToOnyxLib();
					return true;
				}
			}
			if (curReadingTime > 0) {
				long curTime = System.currentTimeMillis();
				lastCalendarSaveTime = curTime;
				updateCalendarEntry(curReadingTime);
				curReadingTime = 0;
			}
		}
		return false;
	}

	public Bookmark saveCurrentPositionBookmarkSync(final boolean saveToDB) {
		++lastSavePositionTaskId;
		Bookmark bmk = BackgroundThread.instance().callBackground(new Callable<Bookmark>() {
			@Override
			public Bookmark call() throws Exception {
				if (!mOpened)
					return null;
				return doc.getCurrentPageBookmark();
			}
		});
		if (bmk != null) {
			//setBookPosition();
			bmk.setTimeStamp(System.currentTimeMillis());
			bmk.setType(Bookmark.TYPE_LAST_POSITION);
			if (mBookInfo != null)
				mBookInfo.setLastPosition(bmk);
			if (saveToDB) {
				Services.getHistory().updateRecentDir();
				mActivity.getDB().saveBookInfo(mBookInfo);
				mActivity.getDB().flush();
			}
		}
		return bmk;
	}

	public void save()
	{
		BackgroundThread.ensureGUI();
		if (isBookLoaded() && mBookInfo != null) {
			if (!Services.isStopped()) {
				log.v("saving last immediately");
				//log.d("bookmark count 1 = " + mBookInfo.getBookmarkCount());
				Services.getHistory().updateBookAccess(mBookInfo, getTimeElapsed());
				//log.d("bookmark count 2 = " + mBookInfo.getBookmarkCount());
				mActivity.getDB().saveBookInfo(mBookInfo);
				//log.d("bookmark count 3 = " + mBookInfo.getBookmarkCount());
				mActivity.getDB().flush();
			}
		}
		//scheduleSaveCurrentPositionBookmark(0);
		//post( new SavePositionTask() );
	}

	public void close()
	{
		BackgroundThread.ensureGUI();
		log.i("ReaderView.close() is called");
		if (!mOpened)
			return;
		cancelSwapTask();
		stopImageViewer();
		save();
		//scheduleSaveCurrentPositionBookmark(0);
		//save();
		post( new Task() {
			public void work() {
				BackgroundThread.ensureBackground();
				if (mOpened) {
					mOpened = false;
					log.i("ReaderView().close() : closing current document");
					doc.doCommand(ReaderCommand.DCMD_CLOSE_BOOK.nativeId, 0);
				}
			}
			public void done() {
				BackgroundThread.ensureGUI();
				if (currentAnimation == null) {
					if (mCurrentPageInfo != null) {
						mCurrentPageInfo.recycle();
						mCurrentPageInfo = null;
					}
					if (mNextPageInfo != null) {
						mNextPageInfo.recycle();
						mNextPageInfo = null;
					}
				} else
					invalidImages = true;
				factory.compact();
				mCurrentPageInfo = null;
			}
		});
	}

	public void destroy()
	{
		log.i("ReaderView.destroy() is called");
		if (mInitialized) {
			//close();
			BackgroundThread.instance().postBackground(() -> {
				BackgroundThread.ensureBackground();
				if (mInitialized) {
					log.i("ReaderView.destroyInternal() calling");
					doc.destroy();
					mInitialized = false;
					currentBackgroundTexture = Engine.NO_TEXTURE;
				}
			});
			//engine.waitTasksCompletion();
			if (null != ttsToolbar)
				ttsToolbar.stopAndClose();
		}
	}

	private String getAddCssValues() {
		String addV = "";
		boolean custBk = mActivity.settings().getBool(Settings.PROP_IMG_CUSTOM_BACKGROUND, false);
		if (custBk) {
			String col = mActivity.settings().getProperty(Settings.PROP_IMG_CUSTOM_BACKGROUND_COLOR, "0x000000");
			col = col.replace("0x", "#");
			addV = addV + "\n" + "image {background-color: " + col + " }";
		}
		return addV;
	}

	private String getCSSForFormat( DocumentFormat fileFormat )
	{
		if (fileFormat == null)
			fileFormat = DocumentFormat.FB2;
		File[] dataDirs = Engine.getDataDirectories(null, false, false);
		String defaultCss = mEngine.loadResourceUtf8(fileFormat.getCSSResourceId());
		String addV = getAddCssValues();
		if (!StrUtils.isEmptyStr(addV))
			if (!addV.startsWith("\n")) addV = "\n" + addV;
		for ( File dir : dataDirs ) {
			File file = new File( dir, fileFormat.getCssName() );
			if (file.exists()) {
				String css = Engine.loadFileUtf8(file);
				if (css != null) {
					int p1 = css.indexOf("@import");
					if (p1 < 0)
						p1 = css.indexOf("@include");
					int p2 = css.indexOf("\";");
					if (p1 >= 0 && p2 >= 0 && p1 < p2 ) {
						css = css.substring(0, p1) + "\n" + defaultCss + "\n" + css.substring(p2+2);
					}
					if (!StrUtils.isEmptyStr(addV)) css = css + addV;
					return css;
				}
			}
		}
		if (!StrUtils.isEmptyStr(addV)) defaultCss = defaultCss + addV;
		return defaultCss;
	}

	public boolean nowFormatting = false;
	public boolean docIsLoading = false;

	boolean enable_progress_callback = true;
	ReaderCallback readerCallback = new ReaderCallback() {

		public boolean OnExportProgress(int percent) {
			log.d("readerCallback.OnExportProgress " + percent);
			return true;
		}

		public void OnExternalLink(String url, String nodeXPath) {
		}

		public void OnFormatEnd() {
			log.d("readerCallback.OnFormatEnd");
			//mEngine.hideProgress();
			arrAllPages = null;
			hideProgress();
			nowFormatting = false;
			drawPage();
			scheduleSwapTask();
		}

		public boolean OnFormatProgress(final int percent) {
			if (enable_progress_callback) {
				log.d("readerCallback.OnFormatProgress " + percent);
				showProgress( percent*4/10 + 5000, R.string.progress_formatting);
			}
//			executeSync( new Callable<Object>() {
//				public Object call() {
//					BackgroundThread.ensureGUI();
//			    	log.d("readerCallback.OnFormatProgress " + percent);
//			    	showProgress( percent*4/10 + 5000, R.string.progress_formatting);
//			    	return null;
//				}
//			});
			return true;
		}

		public void OnFormatStart() {
			log.d("readerCallback.OnFormatStart");
			nowFormatting = true;
			arrAllPages = null;
		}

		public void OnLoadFileEnd() {
			log.d("readerCallback.OnLoadFileEnd");
			docIsLoading = false;
			if (internalDX == 0 && internalDY == 0) {
				internalDX = requestedWidth;
				internalDY = requestedHeight;
				log.d("OnLoadFileEnd: resizeInternal(" + internalDX + "," + internalDY + ")");
				doc.resize(internalDX, internalDY);
			}
		}

		public void OnLoadFileError(String message) {
			log.d("readerCallback.OnLoadFileError(" + message + ")");
			docIsLoading = false;
			arrAllPages = null;
		}

		public void OnLoadFileFirstPagesReady() {
			log.d("readerCallback.OnLoadFileFirstPagesReady");
		}

		public String OnLoadFileFormatDetected(final DocumentFormat fileFormat) {
			log.i("readerCallback.OnLoadFileFormatDetected " + fileFormat);
			if (fileFormat != null) {
				return getCSSForFormat(fileFormat);
			}
			return null;
//
//			String res = executeSync( new Callable<String>() {
//				public String call() {
//					BackgroundThread.ensureGUI();
//					log.i("readerCallback.OnLoadFileFormatDetected " + fileFormat);
//					if (fileFormat != null) {
//						String s = getCSSForFormat(fileFormat);
//						log.i("setting .css for file format " + fileFormat + " from resource " + fileFormat.getCssName());
//						return s;
//					}
//			    	return null;
//				}
//			});
////			int internalStyles = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG) ? 0 : 1;
////			int txtReflow = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG) ? 0 : 2;
////			log.d("internalStyles: " + internalStyles);
////			doc.doCommand(ReaderCommand.DCMD_SET_INTERNAL_STYLES.nativeId, internalStyles | txtReflow);
//			return res;
		}
		public boolean OnLoadFileProgress(final int percent) {
			BackgroundThread.ensureBackground();
			if (enable_progress_callback) {
				log.d("readerCallback.OnLoadFileProgress " + percent);
				showProgress( percent*4/10 + 1000, R.string.progress_loading);
			}
//			executeSync( new Callable<Object>() {
//				public Object call() {
//					BackgroundThread.ensureGUI();
//			    	log.d("readerCallback.OnLoadFileProgress " + percent);
//			    	showProgress( percent*4/10 + 1000, R.string.progress_loading);
//			    	return null;
//				}
//			});
			return true;
		}

		public void OnLoadFileStart(String filename) {
			docIsLoading = true;
			cancelSwapTask();
			BackgroundThread.ensureBackground();
			log.d("readerCallback.OnLoadFileStart " + filename);
			if (enable_progress_callback) {
				showProgress(1000, R.string.progress_loading);
			}
			arrAllPages = null;
		}
		/// Override to handle external links
		public void OnImageCacheClear() {
			//log.d("readerCallback.OnImageCacheClear");
			clearImageCache();
		}
		public boolean OnRequestReload() {
			//reloadDocument();
			return true;
		}

	};

	public volatile SwapToCacheTask currentSwapTask;

	private void scheduleSwapTask() {
		currentSwapTask = new SwapToCacheTask(ReaderView.this);
		currentSwapTask.reschedule();
	}

	private void cancelSwapTask() {
		currentSwapTask = null;
	}

	private boolean invalidImages = true;
	public void clearImageCache() {
		BackgroundThread.instance().postBackground(() -> invalidImages = true);
	}

	public void setStyleSheet(final String css) {
		BackgroundThread.ensureGUI();
		if (css != null && css.length() > 0) {
			post(new Task() {
				public void work() {
					doc.setStylesheet(css);
				}
			});
		}
	}

	public void goToPosition(int position) {
		BackgroundThread.ensureGUI();
		doEngineCommand(ReaderCommand.DCMD_GO_POS, position);
	}

	public void moveBy(final int delta) {
		BackgroundThread.ensureGUI();
		log.d("moveBy(" + delta + ")");
		post(new Task() {
			public void work() {
				BackgroundThread.ensureBackground();
				doc.doCommand(ReaderCommand.DCMD_SCROLL_BY.nativeId, delta);
				scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			}
			public void done() {
				drawPage();
			}
		});
		setTimeLeft();
	}

	public void goToPage(int pageNumber) {
		BackgroundThread.ensureGUI();
		doEngineCommand(ReaderCommand.DCMD_GO_PAGE, pageNumber-1);
		setTimeLeft();
	}

	public void goToPercent(final int percent) {
		BackgroundThread.ensureGUI();
		if (percent >= 0 && percent <= 100)
			post( new Task() {
				public void work() {
					PositionProperties pos = doc.getPositionProps(null, true);
					if (pos != null && pos.pageCount > 0) {
						int pageNumber = pos.pageCount * percent / 100;
						doCommandFromBackgroundThread(ReaderCommand.DCMD_GO_PAGE, pageNumber);
					}
				}
			});
		setTimeLeft();
	}

	public interface MoveSelectionCallback {
		// selection is changed
		public void onNewSelection( Selection selection );
		// cannot move selection
		public void onFail();
	}

	public void moveSelection(final ReaderCommand command, final int param, final MoveSelectionCallback callback) {
		moveSelection(command, param, callback, false);
	}

	public void moveSelection(final ReaderCommand command, final int param, final MoveSelectionCallback callback, boolean noRepaint) {
		post( new Task() {
			private boolean res;
			private Selection selection = new Selection();
			@Override
			public void work() throws Exception {
				res = doc.moveSelection(selection, command.nativeId, param);
			}

			@Override
			public void done() {
				if (callback != null) {
					clearImageCache();
					surface.invalidate();
					drawPage(noRepaint);
					if (res)
						callback.onNewSelection(selection);
					else
						callback.onFail();
				}
			}

			@Override
			public void fail(Exception e) {
				if (callback != null)
					callback.onFail();
			}



		});
	}

	private void showSwitchProfileDialog() {
		SwitchProfileDialog dlg = new SwitchProfileDialog(mActivity, this, null);
		dlg.show();
	}

//	private int currentProfile = 0;
//	public int getCurrentProfile() {
//		if (currentProfile == 0) {
//			currentProfile = mSettings.getInt(PROP_PROFILE_NUMBER, 1);
//			if (currentProfile < 1 || currentProfile > MAX_PROFILES)
//				currentProfile = 1;
//		}
//		return currentProfile;
//	}

	public void setCurrentProfile(int profile) {
		if (mActivity.getCurrentProfile() == profile)
			return;
		if (mBookInfo != null && mBookInfo.getFileInfo() != null) {
			mBookInfo.getFileInfo().setProfileId(profile);
			mActivity.getDB().saveBookInfo(mBookInfo);
		}
		log.i("Apply new profile settings");
		mActivity.setCurrentProfile(profile);
	}

	private final static String NOOK_TOUCH_COVERPAGE_DIR = "/media/screensavers/currentbook";
	void updateNookTouchCoverpage(String bookFileName,
								  byte[] coverpageBytes) {
		try {
			String imageFileName;
			int lastSlash = bookFileName.lastIndexOf("/");
			// exclude path and extension
			if (lastSlash >= 0 && lastSlash < bookFileName.length()) {
				imageFileName = bookFileName.substring(lastSlash);
			} else {
				imageFileName = bookFileName;
			}
			int lastDot = imageFileName.lastIndexOf(".");
			if (lastDot > 0) {
				imageFileName = imageFileName.substring(0, lastDot);
			}
			// guess image type
			if (coverpageBytes.length > 8 // PNG signature length
					&& coverpageBytes[0] == (byte)0x89 // PNG signature start 4 bytes
					&& coverpageBytes[1] == 0x50
					&& coverpageBytes[2] == 0x4E
					&& coverpageBytes[3] == 0x47) {
				imageFileName += ".png";
			} else if (coverpageBytes.length > 3 // Checking only the first 3
					// bytes of JPEG header
					&& coverpageBytes[0] == (byte)0xFF
					&& coverpageBytes[1] == (byte)0xD8
					&& coverpageBytes[2] == (byte)0xFF) {
				imageFileName += ".jpg";
			} else if (coverpageBytes.length > 3 // Checking only the first 3
					// bytes of GIF header
					&& coverpageBytes[0] == 0x47
					&& coverpageBytes[1] == 0x49
					&& coverpageBytes[2] == 0x46) {
				imageFileName += ".gif";
			} else if (coverpageBytes.length > 2 // Checking only the first 2
					// bytes of BMP signature
					&& coverpageBytes[0] == 0x42 && coverpageBytes[1] == 0x4D) {
				imageFileName += ".bmp";
			} else {
				imageFileName += ".jpg"; // default image type
			}
			// create directory if it does not exist
			File d = new File(NOOK_TOUCH_COVERPAGE_DIR);
			if (!d.exists()) {
				d.mkdir();
			}
			// create file only if file with same name does not exist
			File f = new File(d, imageFileName);
			if (!f.exists()) {
				// delete other files in directory so that only current cover is
				// shown all the time
				File[] files = d.listFiles();
				for (File oldFile : files) {
					oldFile.delete();
				}
				// write the image file
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(coverpageBytes);
				fos.close();
			}
		} catch (Exception ex) {
			log.e("Error writing cover page: ", ex);
		}
	}

	private static final int GC_INTERVAL = 15000; // 15 seconds
	DelayedExecutor gcTask = DelayedExecutor.createGUI("gc");
	public void scheduleGc() {
		try {
			gcTask.postDelayed(() -> {
				log.v("Initiating garbage collection");
				System.gc();
			}, GC_INTERVAL);
		} catch (Exception e) {
			// ignore
		}
	}
	public void cancelGc() {
		try {
			gcTask.cancel();
		} catch (Exception e) {
			// ignore
		}
	}

	private void switchFontFace(int direction) {
		String currentFontFace = mSettings.getProperty(PROP_FONT_FACE, "");
		String[] mFontFaces = Engine.getFontFaceList();
		int index = 0;
		int countFaces = mFontFaces.length;
		for (int i = 0; i < countFaces; i++) {
			if (mFontFaces[i].equals(currentFontFace)) {
				index = i;
				break;
			}
		}
		index += direction;
		if (index < 0)
			index = countFaces - 1;
		else if (index >= countFaces)
			index = 0;
		saveSetting(PROP_FONT_FACE, mFontFaces[index]);
		syncViewSettings(getSettings(), true, true);
	}

	public void showGoToPageDialog(final String title, final String prompt, final boolean isNumberEdit, final int minValue, final int maxValue, final int lastValue, final GotoPageDialog.GotoPageHandler handler) {
		BackgroundThread.instance().executeGUI(() -> {
			final GotoPageDialog dlg = new GotoPageDialog(mActivity, title, prompt, isNumberEdit, minValue, maxValue, lastValue, handler);
			dlg.show();
		});
	}

	public void showGoToPageDialog() {
		getCurrentPositionProperties((props, positionText) -> {
			if (props == null)
				return;
			String pos = mActivity.getString(R.string.dlg_goto_current_position) + " " + positionText;
			String prompt = mActivity.getString(R.string.dlg_goto_input_value);
			showGoToPageDialog(mActivity.getString(R.string.mi_goto_page), pos + "\n" + prompt, true,
					1, props.pageCount, props.pageNumber,
					new GotoPageDialog.GotoPageHandler() {
						int val = 0;
						@Override
						public boolean validate(String s, boolean isPercent) {
							if (isPercent) {
								try {
									val = Integer.parseInt(s);
								} catch (Exception e) {
									val = 0;
								}
								return val>=0;
							} else {
								try {
									val = Integer.parseInt(s);
								} catch (Exception e) {
									val = 0;
								}
								return val > 0 && val <= props.pageCount;
							}
						}
						@Override
						public void onOk(String s, boolean isPercent) {
							if (isPercent) {
								if (val>=0 && val<=100)
									goToPercent(val);
							} else
								goToPage(val);
						}
						@Override
						public void onOkPage(String s) {
							goToPage(val);
						}
						@Override
						public void onCancel() {
						}
					});
		});
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (event.getAction() == KeyEvent.ACTION_DOWN)
			return onKeyDown(keyCode, event);
		else if (event.getAction() == KeyEvent.ACTION_UP)
			return onKeyUp(keyCode, event);
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return onTouchEvent(event);
	}

	public boolean onKeyDown(int keyCode, final KeyEvent event) {

		boolean shouldIgnore = false;

		long curTime = System.currentTimeMillis();
		if ((getPreventClickInterval() > 0) && ((curTime - lastTimeKey) < getPreventClickInterval())) {
			log.i("Ignore phantom clicks, ms = " + (curTime - lastTimeKey));
			shouldIgnore = true;
		}
		lastTimeKey = curTime;

		if (keyCode == 0)
			keyCode = event.getScanCode();
		keyCode = translateKeyCode(keyCode);

		mActivity.onUserActivity();

		if (currentImageViewer != null) {
			if (shouldIgnore) return true;
			return currentImageViewer.onKeyDown(keyCode, event);
		}

//		backKeyDownHere = false;
		if (event.getRepeatCount() == 0) {
			log.v("onKeyDown("+keyCode + ", " + event +")");
			keyDownTimestampMap.put(keyCode, System.currentTimeMillis());

			if (keyCode == KeyEvent.KEYCODE_BACK) {
				// hide dictionary popup
				if (DicToastView.toastWindow != null)
					if (DicToastView.toastWindow instanceof PopupWindow) {
						PopupWindow w = (PopupWindow) DicToastView.toastWindow;
						if (w.isShowing()) {
							DicToastView.hideToast(mActivity);
							return true;
						}
				}
				// force saving position on BACK key press
				scheduleSaveCurrentPositionBookmark(1);
			}
		}
		if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_ENDCALL) {
			mActivity.releaseBacklightControl();
			return false;
		}

		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (isAutoScrollActive()) {
				if (shouldIgnore) return true;
				if (keyCode==KeyEvent.KEYCODE_VOLUME_UP)
					changeAutoScrollSpeed(1);
				else
					changeAutoScrollSpeed(-1);
				return true;
			}
			if (!enableVolumeKeys) {
				return false;
			}
		}

		if (isAutoScrollActive())
			return true; // autoscroll will be stopped in onKeyUp

		keyCode = overrideKey(keyCode);
		ReaderAction action = ReaderAction.findForKey(keyCode, mSettings);
		ReaderAction longAction = ReaderAction.findForLongKey(keyCode, mSettings);
		//ReaderAction dblAction = ReaderAction.findForDoubleKey( keyCode, mSettings );

		if (event.getRepeatCount() == 0) {
			if (keyCode == currentDoubleClickActionKeyCode && currentDoubleClickActionStart + getDoubleClickInterval() > android.os.SystemClock.uptimeMillis()) {
				if (currentDoubleClickAction != null) {
					log.d("executing doubleclick action " + currentDoubleClickAction);
					onAction(currentDoubleClickAction);
				}
				currentDoubleClickActionStart = 0;
				currentDoubleClickActionKeyCode = 0;
				currentDoubleClickAction = null;
				currentSingleClickAction = null;
				return true;
			} else {
				if (currentSingleClickAction != null) {
					if (!shouldIgnore) onAction(currentSingleClickAction);
				}
				currentDoubleClickActionStart = 0;
				currentDoubleClickActionKeyCode = 0;
				currentDoubleClickAction = null;
				currentSingleClickAction = null;
			}
		}

		if (event.getRepeatCount() > 0) {
			if (!isTracked(event))
				return true; // ignore
			// repeating key down
			boolean isLongPress = (event.getEventTime()-event.getDownTime())>=AUTOREPEAT_KEYPRESS_TIME;
			if (isLongPress) {
				if (actionToRepeat != null) {
					if (!repeatActionActive) {
						log.v("autorepeating action : " + actionToRepeat );
						repeatActionActive = true;
						onAction(actionToRepeat, () -> {
							if (trackedKeyEvent != null && trackedKeyEvent.getDownTime()==event.getDownTime()) {
								log.v("action is completed : " + actionToRepeat );
								repeatActionActive = false;
							}
						});
					}
				} else {
					stopTracking();
					log.v("executing action on long press : " + longAction );
					onAction(longAction);
				}
			}
			return true;
		}

		if (!action.isNone() && action.canRepeat() && longAction.isRepeat()) {
			// start tracking repeat
			startTrackingKey(event);
			actionToRepeat = action;
			log.v("running action with scheduled autorepeat : " + actionToRepeat );
			repeatActionActive = true;
			onAction(actionToRepeat, () -> {
				if (trackedKeyEvent == event) {
					log.v("action is completed : " + actionToRepeat );
					repeatActionActive = false;
				}
			});
			return true;
		} else {
			actionToRepeat = null;
		}
		
/*		if ( keyCode>=KeyEvent.KEYCODE_0 && keyCode<=KeyEvent.KEYCODE_9 ) {
			// will process in keyup handler
			startTrackingKey(event);
			return true;
		}*/
		if (action.isNone() && longAction.isNone())
			return false;
		startTrackingKey(event);
		return true;
	}

	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		if (keyCode == 0)
			keyCode = event.getScanCode();
		mActivity.onUserActivity();
		keyCode = translateKeyCode(keyCode);
		if (currentImageViewer != null)
			return currentImageViewer.onKeyUp(keyCode, event);
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (isAutoScrollActive())
				return true;
			if (!enableVolumeKeys)
				return false;
		}
		if (isAutoScrollActive()) {
			stopAutoScroll();
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_ENDCALL) {
			mActivity.releaseBacklightControl();
			return false;
		}
		boolean tracked = isTracked(event);
//		if ( keyCode!=KeyEvent.KEYCODE_BACK )
//			backKeyDownHere = false;

		if (keyCode == KeyEvent.KEYCODE_BACK && !tracked)
			return true;
		//backKeyDownHere = false;

		// apply orientation
		keyCode = overrideKey( keyCode );
		boolean isLongPress = false;
		Long keyDownTs = keyDownTimestampMap.get(keyCode);
		if (keyDownTs != null && System.currentTimeMillis()-keyDownTs >= LONG_KEYPRESS_TIME)
			isLongPress = true;
		ReaderAction action = ReaderAction.findForKey( keyCode, mSettings );
		ReaderAction longAction = ReaderAction.findForLongKey( keyCode, mSettings );
		ReaderAction dblAction = ReaderAction.findForDoubleKey( keyCode, mSettings );
		stopTracking();

/*		if ( keyCode>=KeyEvent.KEYCODE_0 && keyCode<=KeyEvent.KEYCODE_9 && tracked ) {
			// goto/set shortcut bookmark
			int shortcut = keyCode - KeyEvent.KEYCODE_0;
			if ( shortcut==0 )
				shortcut = 10;
			if ( isLongPress )
				addBookmark(shortcut);
			else
				goToBookmark(shortcut);
			return true;
		}*/
		if (action.isNone() || !tracked) {
			return false;
		}
		if (!action.isNone() && action.canRepeat() && longAction.isRepeat()) {
			// already processed by onKeyDown()
			return true;
		}

		if (isLongPress) {
			action = longAction;
		} else {
			if (!dblAction.isNone()) {
				// wait for possible double click
				currentDoubleClickActionStart = android.os.SystemClock.uptimeMillis();
				currentDoubleClickAction = dblAction;
				currentSingleClickAction = action;
				currentDoubleClickActionKeyCode = keyCode;
				final int myKeyCode = keyCode;
				BackgroundThread.instance().postGUI(() -> {
					if (currentSingleClickAction != null && currentDoubleClickActionKeyCode == myKeyCode) {
						log.d("onKeyUp: single click action " + currentSingleClickAction.id + " found for key " + myKeyCode + " single click");
						onAction( currentSingleClickAction );
					}
					currentDoubleClickActionStart = 0;
					currentDoubleClickActionKeyCode = 0;
					currentDoubleClickAction = null;
					currentSingleClickAction = null;
				}, getDoubleClickInterval());
				// posted
				return true;
			}
		}
		if (!action.isNone()) {
			log.d("onKeyUp: action " + action.id + " found for key " + keyCode + (isLongPress?" (long)" : "") );
			onAction( action );
			return true;
		}

		// not processed
		return false;
	}

	public boolean onTouchEvent(MotionEvent event) {

		if (!isTouchScreenEnabled) {
			return true;
		}
		if (event.getX()==0 && event.getY()==0)
			return true;
		mActivity.onUserActivity();

		if (currentImageViewer != null)
			return currentImageViewer.onTouchEvent(event);

		if (isAutoScrollActive()) {
			//if (currentTapHandler != null && currentTapHandler.isInitialState()) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				int x = (int)event.getX();
				int y = (int)event.getY();
				int z = getTapZone(x, y, surface.getWidth(), surface.getHeight());
				if (z == 7)
					changeAutoScrollSpeed(-1);
				else if (z == 9)
					changeAutoScrollSpeed(1);
				else
					stopAutoScroll();
			}
			return true;
		}

		if (currentTapHandler == null)
			currentTapHandler = new TapHandler(ReaderView.this);
		currentTapHandler.checkExpiration();
		return currentTapHandler.onTouchEvent(event);
	}

	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		stopTracking();
		if (isAutoScrollActive())
			stopAutoScroll();
	}

	public void redraw() {
		BackgroundThread.instance().executeGUI(() -> {
			surface.invalidate();
			invalidImages = true;
			drawPage();
		});
	}

	public ReaderView(CoolReader activity, Engine engine, Properties props)
	{
		//super(activity);
		log.i("Creating normal SurfaceView");
		this.mActivity = activity;
		mLogFileRoot = mActivity.getSettingsFileF(0).getParent() + "/";
		surface = new ReaderSurface(ReaderView.this, activity);

		bookView = (BookView) surface;
		surface.setOnTouchListener(this);
		surface.setOnKeyListener(this);
		surface.setOnFocusChangeListener(this);
		doc = new DocView(Engine.lock, activity);
		doc.setReaderCallback(readerCallback);
		SurfaceHolder holder = surface.getHolder();
		holder.addCallback(this);

		BackgroundThread.ensureGUI();
		this.mEngine = engine;
		this.mEinkScreen = activity.getEinkScreen();
		surface.setFocusable(true);
		surface.setFocusableInTouchMode(true);
		// set initial size to exclude java.lang.IllegalArgumentException in Bitmap.createBitmap(0, 0)
		// surface.getWidth() at this point return 0
		requestResize(600, 800);
		requestedWidth = 100;
		requestedHeight = 100;

		BackgroundThread.instance().postBackground(() -> {
			log.d("ReaderView - in background thread: calling createInternal()");
			doc.create();
			mInitialized = true;
		});

		log.i("Posting create view task");
		post(new CreateViewTask(ReaderView.this, props));

		if (mAvgDrawAnimationStats != null) mAvgDrawAnimationStats.readRingBuffer();

	}

	public void checkCalendarStats() {
		try {
			if ((mBookInfo != null) && (mActivity != null)) {
				if (lastBookId != mBookInfo.getFileInfo().id) {
					if (curReadingTime > 0) {
						updateCalendarEntry(curReadingTime);
					}
					lastBookId = mBookInfo.getFileInfo().id;
					lastTimePageTurn = 0;
					lastCalendarSaveTime = 0;
					curReadingTime = 0;
				} else {
					long curTime = System.currentTimeMillis();
					long pageTurnTime = curTime - lastTimePageTurn;
					lastTimePageTurn = curTime;
					if (pageTurnTime < maxPageReadInterval)
						curReadingTime = curReadingTime + (pageTurnTime / 1000);
					if ((curTime - lastCalendarSaveTime > calendarSaveInterval) &&
							(curReadingTime > 0)) {
						lastCalendarSaveTime = curTime;
						updateCalendarEntry(curReadingTime);
						curReadingTime = 0;
					}
				}
			}
		} catch (Exception e){
			//do nothing
		}
	}

	public void updateCalendarEntry(Long time_spent_sec) {
		if ((mBookInfo != null) && (mActivity != null)) {
			mActivity.waitForCRDBService(() -> {
				String sdate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new java.util.Date());
				mActivity.getDB().updateCalendarEntry(lastBookId,
					StrUtils.parseDateLong(sdate),time_spent_sec);
			});
		}
	}

}
