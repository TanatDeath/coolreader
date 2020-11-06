package org.coolreader.crengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.CloudSync;

import org.coolreader.crengine.InputDialog.InputHandler;
import org.coolreader.dic.Dictionaries;
import org.coolreader.eink.sony.android.ebookdownloader.SonyBookSelector;
import org.coolreader.graphics.FastBlur;
import org.coolreader.tts.TTSToolbarDlg;

import android.app.SearchManager;
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
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.ClipboardManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onyx.android.sdk.device.Device;

public class ReaderView implements android.view.SurfaceHolder.Callback, Settings, DocProperties, OnKeyListener, OnTouchListener, OnFocusChangeListener {

	public static final Logger log = L.create("rv", Log.VERBOSE);
	public static final Logger alog = L.create("ra", Log.WARN);
	private static final String TAG = "ReaderView";

	private final SurfaceView surface;
	private final BookView bookView;
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

	private void showCenterPopup(String val) {
		mActivity.showCenterPopup(surface, val, false);
	};

	private void showCenterPopup(String val, int millis) {
		mActivity.showCenterPopup(surface, val, millis, false);
	}

	private void showCenterPopupFont(String val, String val2, int fontSize) {
		mActivity.showCenterPopupFont(surface, val, val2, fontSize);
	}

	public String getPageTextFromEngine(int page) {
		int iPageCnt = getDoc().getPageCount();
		if (page < iPageCnt) return StrUtils.getNonEmptyStr(getDoc().getPageText(false, page),false);
		return "";
	}

	public class ReaderSurface extends SurfaceView implements BookView {

		public ReaderSurface(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onPause() {

		}
		@Override
		public void onResume() {

		}
		@Override
		protected void onDraw(Canvas canvas) {
			try {
				log.d("onDraw() called");
				draw();
			} catch ( Exception e ) {
				log.e("exception while drawing", e);
			}
		}

		@Override
		protected void onDetachedFromWindow() {
			super.onDetachedFromWindow();
			log.d("View.onDetachedFromWindow() is called");
		}

		@Override
		public boolean onTrackballEvent(MotionEvent event) {
			log.d("onTrackballEvent(" + event + ")");
			if (mSettings.getBool(PROP_APP_TRACKBALL_DISABLED, false)) {
				log.d("trackball is disabled in settings");
				return true;
			}
			mActivity.onUserActivity();
			return super.onTrackballEvent(event);
		}

		@Override
		protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
			log.i("onSizeChanged(" + w + ", " + h + ")" + " activity.isDialogActive=" + getActivity().isDialogActive());
			super.onSizeChanged(w, h, oldw, oldh);
			requestResize(w, h);
		}

		@Override
		public void onWindowVisibilityChanged(int visibility) {
			if (visibility == VISIBLE) {
				mActivity.einkRefresh();
				startStats();
				checkSize();
			} else
				stopStats();
			super.onWindowVisibilityChanged(visibility);
		}

		@Override
		public void onWindowFocusChanged(boolean hasWindowFocus) {
			if (hasWindowFocus) {
				mActivity.einkRefresh();
				startStats();
				checkSize();
			} else
				stopStats();
			super.onWindowFocusChanged(hasWindowFocus);
		}

		protected void doDraw(Canvas canvas)
		{
			try {

				log.d("doDraw() called");
				if (isProgressActive()) {
					log.d("onDraw() -- drawing progress " + (currentProgressPosition / 100));
					drawPageBackground(canvas);
					doDrawProgress(canvas, currentProgressPosition, currentProgressTitle);
				} else if (mInitialized && mCurrentPageInfo != null && mCurrentPageInfo.bitmap != null) {
					log.d("onDraw() -- drawing page image");

					if (currentAutoScrollAnimation != null) {
						currentAutoScrollAnimation.draw(canvas);
						return;
					}

					if (currentAnimation != null) {
						currentAnimation.draw(canvas);
						return;
					}

					Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
					Rect src = new Rect(0, 0, mCurrentPageInfo.bitmap.getWidth(), mCurrentPageInfo.bitmap.getHeight());
					if (dontStretchWhileDrawing) {
						if (dst.right > src.right)
							dst.right = src.right;
						if (dst.bottom > src.bottom)
							dst.bottom = src.bottom;
						if (src.right > dst.right)
							src.right = dst.right;
						if (src.bottom > dst.bottom)
							src.bottom = dst.bottom;
						if (centerPageInsteadOfResizing) {
							int ddx = (canvas.getWidth() - dst.width()) / 2;
							int ddy = (canvas.getHeight() - dst.height()) / 2;
							dst.left += ddx;
							dst.right += ddx;
							dst.top += ddy;
							dst.bottom += ddy;
						}
					}
					if (dst.width() != canvas.getWidth() || dst.height() != canvas.getHeight())
						canvas.drawColor(Color.rgb(32, 32, 32));
					drawDimmedBitmap(canvas, mCurrentPageInfo.bitmap, src, dst);
					if (isCloudSyncProgressActive()) {
						// draw progressbar on top
						doDrawProgress(canvas, currentCloudSyncProgressPosition, currentCloudSyncProgressTitle, true);
					}
				} else {
					log.d("onDraw() -- drawing empty screen");
					drawPageBackground(canvas);
					if (isCloudSyncProgressActive()) {
						// draw progressbar on top
						doDrawProgress(canvas, currentCloudSyncProgressPosition, currentCloudSyncProgressTitle, true);
					}
				}
				if (selectionModeActive) {
					Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
					int textColor = mSettings.getColor(PROP_FONT_COLOR, 0x000000);
					int newTextSize = 12;
					float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
							newTextSize, getResources().getDisplayMetrics());
					String sText = "";
					int title =  OptionsDialog.getSelectionActionTitle(mSelection2Action == -1 ? mSelectionAction : mSelection2Action);
					if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mSettings, mActivity, true);
					sText = sText + " / ";
					title =  OptionsDialog.getSelectionActionTitle(mMultiSelection2Action == -1 ? mMultiSelectionAction : mMultiSelection2Action);
					if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mSettings, mActivity, true);
					sText = sText + " / ";
					title =  OptionsDialog.getSelectionActionTitle(mSelection2ActionLong == -1 ? mSelectionActionLong : mSelection2ActionLong);
					if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mSettings, mActivity, true);
					Utils.drawFrame2(canvas, dst, Utils.createSolidPaint(0xC0000000 | textColor), 4,
							textSize, sText);
				}
				if (inspectorModeActive) {
					Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
					int textColor = mSettings.getColor(PROP_FONT_COLOR, 0x000000);
					int newTextSize = 12;
					float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
							newTextSize, getResources().getDisplayMetrics());
					String sText = "";
					int title =  OptionsDialog.getSelectionActionTitle(mSelection3Action == -1 ? mSelectionAction : mSelection3Action);
					if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mSettings, mActivity, true);
					sText = sText + " / ";
					title =  OptionsDialog.getSelectionActionTitle(mMultiSelection3Action == -1 ? mMultiSelectionAction : mMultiSelection3Action);
					if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mSettings, mActivity, true);
					sText = sText + " / ";
					title =  OptionsDialog.getSelectionActionTitle(mSelection3ActionLong == -1 ? mSelectionActionLong : mSelection3ActionLong);
					if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mSettings, mActivity, true);
					Utils.drawFrame3(canvas, dst, Utils.createSolidPaint(0xC0000000 | textColor), 4,
							textSize, sText);
				}
			} catch ( Exception e ) {
				log.e("exception while drawing", e);
			}
		}

		protected void doDrawBlack(Canvas canvas)
		{
			try {
				log.d("doDrawBlack() called");
				if (isProgressActive()) {
					log.d("onDrawBlack() -- drawing progress " + (currentProgressPosition / 100));
					drawBlackPageBackground(canvas);
					doDrawProgress(canvas, currentProgressPosition, currentProgressTitle);
				} else if (mInitialized && mCurrentPageInfo != null && mCurrentPageInfo.bitmap != null) {
					log.d("onDrawBlack() -- drawing page image");

					if (currentAutoScrollAnimation != null) {
						currentAutoScrollAnimation.draw(canvas);
						return;
					}

					if (currentAnimation != null) {
						currentAnimation.draw(canvas);
						return;
					}
					//canvas.drawColor(Color.rgb(32, 32, 32));
					drawBlackPageBackground(canvas);
				} else {
					log.d("onDrawBlack() -- drawing empty screen");
					drawBlackPageBackground(canvas);
				}
			} catch ( Exception e ) {
				log.e("exception while drawing", e);
			}
		}

		@Override
		public void draw() {
			draw(false);
		}
		@Override
		public void draw(boolean isPartially) {
			drawCallback(this::doDraw, null, isPartially);
		}

		@Override
		public void draw(boolean isPartially, boolean isBlack) {
			drawCallback(this::doDrawBlack, null, isPartially);
		}

		@Override
		public void invalidate() {
			super.invalidate();
		}

	}

    public DocView getDoc() {
        return doc;
    }

    private DocView doc;

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

	private ViewMode viewMode = ViewMode.PAGES;

	private void execute( Engine.EngineTask task )
	{
		mEngine.execute(task);
	}

	private void post( Engine.EngineTask task )
	{
		mEngine.post(task);
	}

	private abstract class Task implements Engine.EngineTask {

		public void done() {
			// override to do something useful
		}

		public void fail(Exception e) {
			// do nothing, just log exception
			// override to do custom action
			log.e("Task " + this.getClass().getSimpleName() + " is failed with exception " + e.getMessage(), e);
		}
	}

	static class Sync<T> extends Object {
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
	private final Engine mEngine;

	public Selection lastSelection;
	public long lastDuration;
	private Bookmark hyplinkBookmark;

	public BookInfo mBookInfo;
	private Bookmark lastSavedToGdBookmark;

	private Properties mSettings = new Properties();

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
		int x1 = dx / 3;
		int x2 = dx * 2 / 3;
		int y1 = dy / 3;
		int y2 = dy * 2 / 3;
		int zone = 0;
		if (y < y1) {
			if (x < x1)
				zone = 1;
			else if (x < x2)
				zone = 2;
			else
				zone = 3;
		} else if (y < y2) {
			if (x < x1)
				zone = 4;
			else if (x < x2)
				zone = 5;
			else
				zone = 6;
		} else {
			if (x < x1)
				zone = 7;
			else if (x < x2)
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
		if (currentAutoScrollAnimation != null)
			stopAutoScroll();
		Bookmark bmk = getCurrentPositionBookmark();
		if (bmk != null)
			savePositionBookmark(bmk);
		log.i("calling bookView.onPause()");
		boolean bNeedSave = true;
		if (lastSavedToGdBookmark != null) {
			if (bmk.getStartPos() != null)
				if ((bmk.getStartPos().equals(lastSavedToGdBookmark.getStartPos()))) {
					bNeedSave = false;
				}
		}
		if (bNeedSave) {
			int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
			if (iSyncVariant > 0)
				CloudSync.saveJsonInfoFileOrCloud(((CoolReader)mActivity),
						CloudSync.CLOUD_SAVE_READING_POS, true, iSyncVariant == 1, true);
			lastSavedToGdBookmark = bmk;
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

	private int translateKeyCode(int keyCode) {
		if (DeviceInfo.REVERT_LANDSCAPE_VOLUME_KEYS && (mActivity.getScreenOrientation() & 1) != 0) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				return KeyEvent.KEYCODE_VOLUME_UP;
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				return KeyEvent.KEYCODE_VOLUME_DOWN;
		}
		return keyCode;
	}

	private int nextUpdateId = 0;
	private void updateSelection(int startX, int startY, int endX, int endY, final boolean isUpdateEnd) {
		if (isUpdateEnd)
			lastSelTime = System.currentTimeMillis();
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
				if (isUpdateEnd) {
					String text = sel.text;
					if (text != null && text.length() > 0) {
						onSelectionComplete( sel, selMode );
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

	private int mSelectionAction = SELECTION_ACTION_TOOLBAR;
	private int mSelectionActionLong = SELECTION_ACTION_TOOLBAR;
	private int mMultiSelectionAction = SELECTION_ACTION_TOOLBAR;
	private int mSelection2Action = SELECTION_ACTION_SAME_AS_COMMON;
	private int mSelection2ActionLong = SELECTION_ACTION_SAME_AS_COMMON;
	private int mMultiSelection2Action = SELECTION_ACTION_SAME_AS_COMMON;
	private int mSelection3Action = SELECTION_ACTION_SAME_AS_COMMON;
	private int mSelection3ActionLong = SELECTION_ACTION_SAME_AS_COMMON;
	private int mMultiSelection3Action = SELECTION_ACTION_SAME_AS_COMMON;

	private void showDic(Selection sel, boolean bSkipDic, Dictionaries.DictInfo dict) {
		getActivity().mDictionaries.setAdHocDict(dict);
		if ((!isMultiSelection(sel))&&(mActivity.ismDictWordCorrrection())) {
			if (!bSkipDic)
				mActivity.findInDictionary(StrUtils.dictWordCorrection(sel.text), null);
		} else {
			if (
					((!isMultiSelection(sel))&&(!bSkipDic))
							||
							(isMultiSelection(sel))
			)
				mActivity.findInDictionary(sel.text, null);
		}
		if (!getSettings().getBool(PROP_APP_SELECTION_PERSIST, false))
			clearSelection();
	}

	private void onSelectionComplete(Selection sel, boolean selMode) {

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
		int iSelectionAction1 = (lastDuration > DOUBLE_CLICK_INTERVAL) ? longAction : shortAction;
		iSelectionAction = isMultiSelection(sel) ? multiAction : iSelectionAction1;
		//if (selMode) iSelectionAction = mMultiSelectionAction;
		switch (iSelectionAction) {
			case SELECTION_ACTION_TOOLBAR:
				SelectionToolbarDlg.showDialog(mActivity, ReaderView.this, sel);
				break;
			case SELECTION_ACTION_COPY:
				copyToClipboardAndToast(sel.text);
				clearSelection();
				break;
			case SELECTION_ACTION_DICTIONARY:
				if ((!isMultiSelection(sel))&&(mActivity.ismDictWordCorrrection())) {
					if (!bSkipDic)
						mActivity.findInDictionary(StrUtils.dictWordCorrection(sel.text), null);
				} else {
					if (
							((!isMultiSelection(sel))&&(!bSkipDic))
							||
							(isMultiSelection(sel))
						)
						mActivity.findInDictionary(sel.text, null);
				}
				if (!getSettings().getBool(PROP_APP_SELECTION_PERSIST, false))
					clearSelection();
				break;
			case SELECTION_ACTION_DICTIONARY_1:
				showDic(sel, bSkipDic, getActivity().mDictionaries.currentDictionary);
				break;
			case SELECTION_ACTION_DICTIONARY_2:
				showDic(sel, bSkipDic, getActivity().mDictionaries.currentDictionary2);
				break;
			case SELECTION_ACTION_DICTIONARY_3:
				showDic(sel, bSkipDic, getActivity().mDictionaries.currentDictionary3);
				break;
			case SELECTION_ACTION_DICTIONARY_4:
				showDic(sel, bSkipDic, getActivity().mDictionaries.currentDictionary4);
				break;
			case SELECTION_ACTION_DICTIONARY_5:
				showDic(sel, bSkipDic, getActivity().mDictionaries.currentDictionary5);
				break;
			case SELECTION_ACTION_DICTIONARY_6:
				showDic(sel, bSkipDic, getActivity().mDictionaries.currentDictionary6);
				break;
			case SELECTION_ACTION_DICTIONARY_7:
				showDic(sel, bSkipDic, getActivity().mDictionaries.currentDictionary7);
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
				if ((!BaseActivity.PRO_FEATURES)&&(!BaseActivity.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				showNewBookmarkDialog(sel, Bookmark.TYPE_USER_DIC, "");
				break;
			case SELECTION_ACTION_CITATION:
				if ((!BaseActivity.PRO_FEATURES)&&(!BaseActivity.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				showNewBookmarkDialog(sel, Bookmark.TYPE_CITATION, "");
				break;
            case SELECTION_ACTION_DICTIONARY_LIST:
                DictsDlg dlg = new DictsDlg(mActivity, this, sel.text, null);
                dlg.show();
                if (!getSettings().getBool(PROP_APP_SELECTION_PERSIST, false))
                    clearSelection();
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

	public void sendQuotationInEmail( Selection sel ) {
		StringBuilder buf = new StringBuilder();
		if (mBookInfo.getFileInfo().getAuthors()!=null)
			buf.append("|" + mBookInfo.getFileInfo().getAuthors() + "\n");
		if (mBookInfo.getFileInfo().title!=null)
			buf.append("|" + mBookInfo.getFileInfo().title + "\n");
		if (sel.chapter!=null && sel.chapter.length()>0)
			buf.append("|" + sel.chapter + "\n");
		buf.append(sel.text + "\n");
		mActivity.sendBookFragment(mBookInfo, buf.toString());
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

	private int isBacklightControlFlick = 1;
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
	private boolean doubleTapSelectionEnabled = false;
	private int mGesturePageFlipsPerFullSwipe;
	private boolean mIsPageMode;
	private boolean mDisableTwoPointerGestures;
	private int secondaryTapActionType = TAP_ACTION_TYPE_LONGPRESS;
	private boolean selectionModeActive = false;
	private boolean selectionModeWasActive = false;
	private boolean inspectorModeActive = false;

	public void toggleSelectionMode() {
		selectionModeActive = !selectionModeActive;
		inspectorModeActive = false;
		if (mActivity.getmReaderFrame()!=null)
			if (mActivity.getmReaderFrame().getUserDicPanel()!=null)
				mActivity.getmReaderFrame().getUserDicPanel().updateSavingMark(
						mActivity.getString(selectionModeActive ?
								R.string.action_toggle_selection_mode_on : R.string.action_toggle_selection_mode_off));
                bookView.draw(false);
	}

	public void toggleInspectorMode() {
		inspectorModeActive = !inspectorModeActive;
		selectionModeActive = false;
		if (mActivity.getmReaderFrame()!=null)
			if (mActivity.getmReaderFrame().getUserDicPanel()!=null)
				mActivity.getmReaderFrame().getUserDicPanel().updateSavingMark(
						mActivity.getString(inspectorModeActive ?
								R.string.action_toggle_inspector_mode_on : R.string.action_toggle_inspector_mode_off));
		bookView.draw(false);
	}

	private ImageViewer currentImageViewer;
	private class ImageViewer extends SimpleOnGestureListener {
		private ImageInfo currentImage;
		final GestureDetector detector;
		int oldOrientation;
		public ImageViewer(ImageInfo image) {
			lockOrientation();
			detector = new GestureDetector(this);
			if (image.bufHeight / image.height >= 2 && image.bufWidth / image.width >= 2) {
				image.scaledHeight *= 2;
				image.scaledWidth *= 2;
			}
			centerIfLessThanScreen(image);
			currentImage = image;
		}

		private void lockOrientation() {
			oldOrientation = mActivity.getScreenOrientation();
			if (oldOrientation == 4)
				mActivity.setScreenOrientation(mActivity.getOrientationFromSensor());
		}

		private void unlockOrientation() {
			if (oldOrientation == 4)
				mActivity.setScreenOrientation(oldOrientation);
		}

		private void centerIfLessThanScreen(ImageInfo image) {
			if (image.scaledHeight < image.bufHeight)
				image.y = (image.bufHeight - image.scaledHeight) / 2;
			if (image.scaledWidth < image.bufWidth)
				image.x = (image.bufWidth - image.scaledWidth) / 2;
		}

		private void fixScreenBounds(ImageInfo image) {
			if (image.scaledHeight > image.bufHeight) {
				if (image.y < image.bufHeight - image.scaledHeight)
					image.y = image.bufHeight - image.scaledHeight;
				if (image.y > 0)
					image.y = 0;
			}
			if (image.scaledWidth > image.bufWidth) {
				if (image.x < image.bufWidth - image.scaledWidth)
					image.x = image.bufWidth - image.scaledWidth;
				if (image.x > 0)
					image.x = 0;
			}
		}

		private void updateImage(ImageInfo image) {
			centerIfLessThanScreen(image);
			fixScreenBounds(image);
			if (!currentImage.equals(image)) {
				currentImage = image;
				drawPage();
			}
		}

		public void zoomIn() {
			ImageInfo image = new ImageInfo(currentImage);
			if (image.scaledHeight >= image.height) {
				int scale = image.scaledHeight / image.height;
				if (scale < 4)
					scale++;
				image.scaledHeight = image.height * scale;
				image.scaledWidth = image.width * scale;
			} else {
				int scale = image.height / image.scaledHeight;
				if (scale > 1)
					scale--;
				image.scaledHeight = image.height / scale;
				image.scaledWidth = image.width / scale;
			}
			updateImage(image);
		}

		public void zoomOut() {
			ImageInfo image = new ImageInfo(currentImage);
			if (image.scaledHeight > image.height) {
				int scale = image.scaledHeight / image.height;
				if (scale > 1)
					scale--;
				image.scaledHeight = image.height * scale;
				image.scaledWidth = image.width * scale;
			} else {
				int scale = image.height / image.scaledHeight;
				if (image.scaledHeight > image.bufHeight || image.scaledWidth > image.bufWidth)
					scale++;
				image.scaledHeight = image.height / scale;
				image.scaledWidth = image.width / scale;
			}
			updateImage(image);
		}

		public int getStep() {
			ImageInfo image = currentImage;
			int max = image.bufHeight;
			if (max < image.bufWidth)
				max = image.bufWidth;
			return max / 10;
		}

		public void moveBy(int dx, int dy) {
			ImageInfo image = new ImageInfo(currentImage);
			image.x += dx;
			image.y += dy;
			updateImage(image);
		}

		public boolean onKeyDown(int keyCode, final KeyEvent event) {
			if (keyCode == 0)
				keyCode = event.getScanCode();
			switch (keyCode) {
				case KeyEvent.KEYCODE_VOLUME_UP:
					zoomIn();
					return true;
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					zoomOut();
					return true;
				case KeyEvent.KEYCODE_DPAD_CENTER:
				case KeyEvent.KEYCODE_BACK:
				case KeyEvent.KEYCODE_ENDCALL:
					close();
					return true;
				case KeyEvent.KEYCODE_DPAD_LEFT:
					moveBy(getStep(), 0);
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					moveBy(-getStep(), 0);
					return true;
				case KeyEvent.KEYCODE_DPAD_UP:
					moveBy(0, getStep());
					return true;
				case KeyEvent.KEYCODE_DPAD_DOWN:
					moveBy(0, -getStep());
					return true;
			}
			return false;
		}

		public boolean onKeyUp(int keyCode, final KeyEvent event) {
			if (keyCode == 0)
				keyCode = event.getScanCode();
			switch (keyCode) {
				case KeyEvent.KEYCODE_BACK:
				case KeyEvent.KEYCODE_ENDCALL:
					close();
					return true;
			}
			return false;
		}

		public boolean onTouchEvent(MotionEvent event) {
//			int aindex = event.getActionIndex();
//			if (event.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
//				log.v("ACTION_POINTER_DOWN");
//			}
			return detector.onTouchEvent(event);
		}



		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
							   float velocityY) {
			log.v("onFling()");
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
								float distanceX, float distanceY) {
			log.v("onScroll() " + distanceX + ", " + distanceY);
			int dx = (int)distanceX;
			int dy = (int)distanceY;
			moveBy(-dx, -dy);
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			log.v("onSingleTapConfirmed()");
			ImageInfo image = new ImageInfo(currentImage);

			int x = (int)e.getX();
			int y = (int)e.getY();

			int zone = 0;
			int zw = mActivity.getDensityDpi() / 2;
			int w = image.bufWidth;
			int h = image.bufHeight;
			if (image.rotation == 0) {
				if (x < zw && y > h - zw)
					zone = 1;
				if (x > w - zw && y > h - zw)
					zone = 2;
			} else {
				if (x < zw && y < zw)
					zone = 1;
				if (x < zw && y > h - zw)
					zone = 2;
			}
			if (zone != 0) {
				if (zone == 1)
					zoomIn();
				else
					zoomOut();
				return true;
			}

			close();
			return super.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		public void close() {
			if (currentImageViewer == null)
				return;
			currentImageViewer = null;
			unlockOrientation();
			BackgroundThread.instance().postBackground(() -> doc.closeImage());
			drawPage();
		}

		public BitmapInfo prepareImage() {
			// called from background thread
			ImageInfo img = currentImage;
			img.bufWidth = internalDX;
			img.bufHeight = internalDY;
			if (mCurrentPageInfo != null) {
				if (img.equals(mCurrentPageInfo.imageInfo))
					return mCurrentPageInfo;
				mCurrentPageInfo.recycle();
				mCurrentPageInfo = null;
			}
			PositionProperties currpos = doc.getPositionProps(null, false);
			BitmapInfo bi = new BitmapInfo();
			bi.imageInfo = new ImageInfo(img);
			bi.bitmap = factory.get(internalDX, internalDY);
			bi.position = currpos;
			doc.drawImage(bi.bitmap, bi.imageInfo);
			mCurrentPageInfo = bi;
			return mCurrentPageInfo;
		}

	}

	private void startImageViewer(ImageInfo image) {
		currentImageViewer = new ImageViewer(image);
		drawPage();
	}

	private boolean isImageViewMode() {
		return currentImageViewer != null;
	}

	private void stopImageViewer() {
		if (currentImageViewer != null)
			currentImageViewer.close();
	}

	private TapHandler currentTapHandler = null;
	public class TapHandler {

		private final static int STATE_INITIAL = 0; // no events yet
		private final static int STATE_DOWN_1 = 1; // down first time
		private final static int STATE_SELECTION = 3; // selection is started
		private final static int STATE_FLIPPING = 4; // flipping is in progress
		private final static int STATE_WAIT_FOR_DOUBLE_CLICK = 5; // flipping is in progress
		private final static int STATE_DONE = 6; // done: no more tracking
		private final static int STATE_BRIGHTNESS = 7; // brightness change in progress
		private final static int STATE_FLIP_TRACKING = 8; // pages flip tracking in progress
		private final static int STATE_TWO_POINTERS = 9; // two finger events
		private final static int STATE_TWO_POINTERS_VERT_MARGINS = 10; // Vertical margins control
		private final static int STATE_TWO_POINTERS_HORZ_MARGINS = 11; // Horizontal margins control
		private final static int STATE_TWO_POINTERS_FONT_SCALE = 12;  // Font scaling
		private final static int STATE_TWO_POINTERS_DOWN = 13;  // Two fingers move down - next chapter
		private final static int STATE_TWO_POINTERS_UP = 14; // Two fingers move down - previous chapter
		private final static int STATE_THREE_POINTERS = 15; // three finger events

		private final static int PINCH_TRESHOLD_COUNT = 15;

		private final static int EXPIRATION_TIME_MS = 180000;

		int state = STATE_INITIAL;
		int stateInsp = STATE_INITIAL;

		int start_x = 0;
		int start_y = 0;
		int start_x2 = 0;
		int start_y2 = 0;
		int now_x = 0;
		int now_y = 0;
		int now_x2 = 0;
		int now_y2 = 0;
		int width = 0;
		int height = 0;
		int marginBegin = 0;
		int marginToSet = -1;
		int fontSizeBegin = 0;
		int fontSizeToSet = -1;
		ReaderAction shortTapAction = ReaderAction.NONE;
		ReaderAction longTapAction = ReaderAction.NONE;
		ReaderAction doubleTapAction = ReaderAction.NONE;
		long firstDown;
		String curPageText = "";

		/// handle unexpected event for state: stop tracking
		private boolean unexpectedEvent() {
			cancel();
			return true; // ignore
		}

		public boolean isInitialState() {
			return state == STATE_INITIAL;
		}
		public void checkExpiration() {
			if (state != STATE_INITIAL && Utils.timeInterval(firstDown) > EXPIRATION_TIME_MS)
				cancel();
		}

		/// cancel current action and reset touch tracking state
		private boolean cancel() {
			if (state == STATE_INITIAL)
				return true;
			switch (state) {
				case STATE_DOWN_1:
				case STATE_SELECTION:
					clearSelection();
					break;
				case STATE_FLIPPING:
					stopAnimation(-1, -1);
					break;
				case STATE_WAIT_FOR_DOUBLE_CLICK:
				case STATE_DONE:
				case STATE_BRIGHTNESS:
				case STATE_FLIP_TRACKING:
					stopBrightnessControl(-1, -1, leftSideBrightness);
					break;
			}
			state = STATE_DONE;
			unhiliteTapZone();
			currentTapHandler = new TapHandler();
			return true;
		}

		private void adjustStartValuesOnDrag(int swipeDistance, int distanceForFlip) {
			if (Math.abs(swipeDistance) < Math.abs(distanceForFlip)) {
				return; // Nothing to do
			}
			int direction = swipeDistance > 0 ? 1 : -1; // Left-to-right or right-to-left swipe?
			int value = direction * distanceForFlip;
			Log.i(TAG, "adjustStartValuesOnDrag: initial start_x=" + start_x + ", swipeDistance=" + swipeDistance);
			while (Math.abs(swipeDistance) >= Math.abs(distanceForFlip)) {
				if (distanceForFlip > 0) {
					start_x += value;
					swipeDistance -= value;
				} else {
					start_x -= value;
					swipeDistance += value;
				}
				Log.i(TAG, "adjustStartValuesOnDrag: start_x=" + start_x + ", swipeDistance=" + swipeDistance);
			}
		}

		private void updatePageFlipTracking(final int x, final int y) {
			if (!mOpened)
				return;
			final int swipeDistance = mIsPageMode ? x - start_x : y - start_y;
			final int distanceForFlip = surface.getWidth() / mGesturePageFlipsPerFullSwipe;
			int pagesToFlip = swipeDistance / distanceForFlip;
			if (pagesToFlip == 0) {
				return; // Nothing to do
			}
			adjustStartValuesOnDrag(swipeDistance, distanceForFlip);
			ReaderAction action = pagesToFlip > 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP;
			while (pagesToFlip != 0) {
				onAction(action);
				if (pagesToFlip > 0) {
					pagesToFlip--;
				} else {
					pagesToFlip++;
				}
			}
		}

		/// perform action and reset touch tracking state
		private boolean performAction(final ReaderAction action, boolean checkForLinks) {
			log.d("performAction on touch: " + action);
			state = STATE_DONE;

			currentTapHandler = new TapHandler();

			if (!checkForLinks) {
				onAction(action);
				return true;
			}

			// check link before executing action
			mEngine.execute(new Task() {
				String link;
				ImageInfo image;
				Bookmark bookmark;
				public void work() {
					image = new ImageInfo();
					image.bufWidth = internalDX;
					image.bufHeight = internalDY;
					image.bufDpi = mActivity.getDensityDpi();
					if (doc.checkImage(start_x, start_y, image)) {
						return;
					}
					image = null;
					link = doc.checkLink(start_x, start_y, mActivity.getPalmTipPixels() / 2 );
					if (link != null) {
						if (link.startsWith("#")) {
							log.d("go to " + link);
							doc.goLink(link);
							drawPage();
						}
						return;
					}
					bookmark = doc.checkBookmark(start_x, start_y);
					if (bookmark != null && bookmark.getType() == Bookmark.TYPE_POSITION)
						bookmark = null;
				}
				public void done() {
					if (bookmark != null)
						bookmark = mBookInfo.findBookmark(bookmark);
					if (link == null && image == null && bookmark == null) {
						onAction(action);
					} else if (image != null) {
						startImageViewer(image);
					} else if (bookmark != null) {
						BookmarkEditDialog dlg = new BookmarkEditDialog(mActivity, ReaderView.this, bookmark, false, Bookmark.TYPE_COMMENT, "");
						dlg.show();
					} else if (!link.startsWith("#")) {
						log.d("external link " + link);
						if (link.startsWith("http://")||link.startsWith("https://")) {
							mActivity.openURL(link);
						} else {
							// absolute path to file
							FileInfo fi = new FileInfo(link);
							if (fi.exists()) {
								mActivity.loadDocument(fi, true);
								return;
							}
							File baseDir = null;
							if (mBookInfo!=null && mBookInfo.getFileInfo()!=null) {
								if (!mBookInfo.getFileInfo().isArchive) {
									// relatively to base directory
									File f = new File(mBookInfo.getFileInfo().getBasePath());
									baseDir = f.getParentFile();
									String url = link;
									while (baseDir!=null && url!=null && url.startsWith("../")) {
										baseDir = baseDir.getParentFile();
										url = url.substring(3);
									}
									if (baseDir!=null && url!=null && url.length()>0) {
										fi = new FileInfo(baseDir.getAbsolutePath()+"/"+url);
										if (fi.exists()) {
											mActivity.loadDocument(fi, true);
											return;
										}
									}
								} else {
									// from archive
									fi = new FileInfo(mBookInfo.getFileInfo().getArchiveName() + FileInfo.ARC_SEPARATOR + link);
									if (fi.exists()) {
										mActivity.loadDocument(fi, true);
										return;
									}
								}
							}
							mActivity.showSToast("Cannot open link " + link);
						}
					}
				}
			});
			return true;
		}

		private boolean startSelection() {
			state = STATE_SELECTION;
			// check link before executing action
			mEngine.execute(new Task() {
				ImageInfo image;
				Bookmark bookmark;
				public void work() {
					image = new ImageInfo();
					image.bufWidth = internalDX;
					image.bufHeight = internalDY;
					image.bufDpi = mActivity.getDensityDpi();
					if (!doc.checkImage(start_x, start_y, image))
						image = null;
					bookmark = doc.checkBookmark(start_x, start_y);
					if (bookmark != null && bookmark.getType() == Bookmark.TYPE_POSITION)
						bookmark = null;
				}public void done() {
					if (bookmark != null)
						bookmark = mBookInfo.findBookmark(bookmark);
					if (image != null) {
						cancel();
						startImageViewer(image);
					} else if (bookmark != null) {
						cancel();
						boolean bMenuShow = false;
						if (hyplinkBookmark != null) {
							if (hyplinkBookmark.getLinkPos().equals(bookmark.getStartPos())) {
								bMenuShow=true;
							}
						}
						if ((StrUtils.isEmptyStr(bookmark.getCommentText()))&&(!bMenuShow)&&(StrUtils.isEmptyStr(bookmark.getLinkPos()))) {
							mActivity.skipFindInDic = true;
							BookmarkEditDialog dlg = new BookmarkEditDialog(mActivity, ReaderView.this, bookmark, false, Bookmark.TYPE_COMMENT, "");
							dlg.show();
						} else {
							if (!StrUtils.isEmptyStr(bookmark.getCommentText()))
								mActivity.showSToast(StrUtils.updateText(bookmark.getCommentText(),true));
							if (bMenuShow) {
								ReaderAction[] actions = {
										ReaderAction.GO_BACK
								};
								mActivity.showActionsPopupMenu(actions, new CRToolBar.OnActionHandler() {
									@Override
									public boolean onActionSelected(ReaderAction item) {
										if (item == ReaderAction.GO_BACK) {
											goToBookmark(hyplinkBookmark);
											hyplinkBookmark=null;
											return true;
										}
										return false;
									}
								});
							} else {
								String sL = bookmark.getLinkPos();
								if (!StrUtils.isEmptyStr(sL)) {
									for (Bookmark bm : mBookInfo.getAllBookmarks()) {
										if (bm.getStartPos().equals(bookmark.getLinkPos())) {
											goToBookmark(bm);
											hyplinkBookmark = bookmark;
											break;
										}
									}
								}
							}
							mActivity.skipFindInDic = true;
						}
					} else {
						long curTime = System.currentTimeMillis();
						// avoid too fast selection calling
						if (curTime - lastSelTime > 300)
							updateSelection( start_x, start_y, start_x, start_y, false );
					}
				}
			});
			return true;
		}

		private boolean trackDoubleTap() {
			state = STATE_WAIT_FOR_DOUBLE_CLICK;
			BackgroundThread.instance().postGUI(() -> {
				if (currentTapHandler == TapHandler.this && state == STATE_WAIT_FOR_DOUBLE_CLICK)
					performAction(shortTapAction, false);
			}, DOUBLE_CLICK_INTERVAL);
			return true;
		}

		private boolean trackDoubleTapInsp() {
			stateInsp = STATE_WAIT_FOR_DOUBLE_CLICK;
			BackgroundThread.instance().postGUI(() -> stateInsp = STATE_INITIAL, DOUBLE_CLICK_INTERVAL);
			return true;
		}

		private boolean trackLongTap() {
			BackgroundThread.instance().postGUI(() -> {
				if (currentTapHandler == TapHandler.this && state == STATE_DOWN_1) {
					if (longTapAction == ReaderAction.START_SELECTION)
						startSelection();
					else
						performAction(longTapAction, true);
				}
			}, LONG_KEYPRESS_TIME);
			return true;
		}

		boolean leftSideBrightness = true;

		public boolean onTouchEvent(MotionEvent event) {
			int index = event.getActionIndex();
			int x = (int)event.getX();
			int y = (int)event.getY();
			curPageText = mActivity.getmReaderFrame().getUserDicPanel().getCurPageText(0, false);
			if ((event.getPointerCount() > 1) && (state == STATE_DOWN_1) && (!mDisableTwoPointerGestures)) {
				if (event.getPointerCount() == 3) {
					state = STATE_THREE_POINTERS;
				} else {
					state = STATE_TWO_POINTERS;
					x = (int) event.getX(index);
					y = (int) event.getY(index);
					if (index == 1) {
						start_x2 = x;
						start_y2 = y;
					}
					int minSize = width;
					if (height < minSize) minSize = height;
					int pinchTreshold = minSize / PINCH_TRESHOLD_COUNT;
					// lets detect pinch type
					state = STATE_TWO_POINTERS_FONT_SCALE;
					fontSizeToSet = fontSizeBegin;
					if (Math.abs(start_x - start_x2) < pinchTreshold * 2) {
						state = STATE_TWO_POINTERS_VERT_MARGINS;
						int marg1 = mActivity.settings().getInt(Settings.PROP_PAGE_MARGIN_TOP, 0);
						int marg2 = mActivity.settings().getInt(Settings.PROP_PAGE_MARGIN_BOTTOM, 0);
						marginBegin = marg1;
						if (marginBegin > marg2) marginBegin = marg2;
						marginToSet = marginBegin;
					} else if (Math.abs(start_y - start_y2) < pinchTreshold * 2) {
						state = STATE_TWO_POINTERS_HORZ_MARGINS;
						int marg1 = mActivity.settings().getInt(Settings.PROP_PAGE_MARGIN_LEFT, 0);
						int marg2 = mActivity.settings().getInt(Settings.PROP_PAGE_MARGIN_RIGHT, 0);
						marginBegin = marg1;
						if (marginBegin > marg2) marginBegin = marg2;
						marginToSet = marginBegin;
					} else {
						fontSizeBegin = mActivity.settings().getInt(Settings.PROP_FONT_SIZE, 0);
						fontSizeToSet = fontSizeBegin;
					}
				}
			}
			if ((DeviceInfo.getSDKLevel() >= 19) && mActivity.isFullscreen() && (event.getAction() == MotionEvent.ACTION_DOWN)) {
				if ((y < 30) || (y > (getSurface().getHeight() - 30)))
					return unexpectedEvent();
			}

			if (state == STATE_INITIAL && event.getAction() != MotionEvent.ACTION_DOWN)
				return unexpectedEvent(); // ignore unexpected event

			// Uncomment to disable user interaction during cloud sync
			//if (isCloudSyncProgressActive())
			//	return unexpectedEvent();

			if (event.getAction() == MotionEvent.ACTION_UP) {
				long duration = Utils.timeInterval(firstDown);
				lastDuration = duration;
				switch (state) {
					case STATE_DOWN_1:
						if (hiliteTapZoneOnTap) {
							hiliteTapZone( true, x, y, width, height );
							scheduleUnhilite( LONG_KEYPRESS_TIME );
						}
						if (duration > LONG_KEYPRESS_TIME) {
							if (longTapAction == ReaderAction.START_SELECTION)
								return startSelection();
							return performAction(longTapAction, true);
						}
						if (doubleTapAction.isNone())
							return performAction(shortTapAction, false);
						// start possible double tap tracking
						return trackDoubleTap();
					case STATE_FLIPPING:
						stopAnimation(x, y);
						state = STATE_DONE;
						return cancel();
					case STATE_BRIGHTNESS:
						stopBrightnessControl(start_y, now_y, leftSideBrightness);
						state = STATE_DONE;
						return cancel();
					case STATE_SELECTION:
						// If the second tap is within a radius of the first tap point, assume the user is trying to double tap on the same point
						if (start_x-x <= DOUBLE_TAP_RADIUS && x-start_x <= DOUBLE_TAP_RADIUS && y-start_y <= DOUBLE_TAP_RADIUS && start_y-y <= DOUBLE_TAP_RADIUS) {
							//log.v("upd2: "+nextUpdateId+" ");
							updateSelection(start_x, start_y, start_x, start_y, true);
						}
						else {
							//log.v("upd3: "+nextUpdateId+" ");
							updateSelection(start_x, start_y, x, y, true);
						}
						selectionModeWasActive = selectionModeActive;
						selectionModeActive = false;
						if (inspectorModeActive) {
							boolean res = cancel();
							currentTapHandler.trackDoubleTapInsp();
							return res;
						} else {
							state = STATE_DONE;
							return cancel();
						}
					case STATE_FLIP_TRACKING:
						updatePageFlipTracking(x, y);
						state = STATE_DONE;
						return cancel();
					case STATE_THREE_POINTERS:
						return performAction(ReaderAction.READER_MENU, false);
					case STATE_TWO_POINTERS_VERT_MARGINS:
						if (marginToSet>=0) {
							state = STATE_DONE;
							Properties props = new Properties(mActivity.settings());
							props.setProperty(Settings.PROP_PAGE_MARGIN_TOP, "" + marginToSet);
							props.setProperty(Settings.PROP_PAGE_MARGIN_BOTTOM, "" + marginToSet);
							mActivity.setSettings(props, -1, true);
						}
						return cancel();
					case STATE_TWO_POINTERS_HORZ_MARGINS:
						if (marginToSet>=0) {
							state = STATE_DONE;
							Properties props = new Properties(mActivity.settings());
							props = new Properties(mActivity.settings());
							props.setProperty(Settings.PROP_PAGE_MARGIN_LEFT, "" + marginToSet);
							props.setProperty(Settings.PROP_PAGE_MARGIN_RIGHT, "" + marginToSet);
							mActivity.setSettings(props, -1, true);
						}
						return cancel();
					case STATE_TWO_POINTERS_FONT_SCALE:
						if (fontSizeToSet >= 0) {
							state = STATE_DONE;
							Properties props = new Properties(mActivity.settings());
							props = new Properties(mActivity.settings());
							props.setProperty(Settings.PROP_FONT_SIZE, "" + fontSizeToSet);
							mActivity.setSettings(props, -1, true);
						}
						return cancel();
				}
			} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if ((inspectorModeActive) && (stateInsp == STATE_WAIT_FOR_DOUBLE_CLICK)) {
					toggleInspectorMode();
					return cancel();
				}
				switch (state) {
					case STATE_INITIAL:
						start_x = x;
						start_y = y;
						now_y = 0;
						now_x = 0;
						now_x2 = 0;
						now_y2 = 0;
						marginToSet = -1;
						marginBegin = 0;
						fontSizeBegin = 0;
						fontSizeToSet = -1;
						width = surface.getWidth();
						height = surface.getHeight();
						int zone = getTapZone(x, y, width, height);
						shortTapAction = findTapZoneAction(zone, TAP_ACTION_TYPE_SHORT);
						longTapAction = findTapZoneAction(zone, TAP_ACTION_TYPE_LONGPRESS);
						doubleTapAction = findTapZoneAction(zone, TAP_ACTION_TYPE_DOUBLE);
						firstDown = Utils.timeStamp();
						if (selectionModeActive || inspectorModeActive) {
							startSelection();
						} else {
							state = STATE_DOWN_1;
							trackLongTap();
						}
						return true;
					case STATE_DOWN_1:
					case STATE_BRIGHTNESS:
					case STATE_FLIPPING:
					case STATE_SELECTION:
					case STATE_FLIP_TRACKING:
						return unexpectedEvent();
					case STATE_WAIT_FOR_DOUBLE_CLICK:
						if (inspectorModeActive) {
							toggleInspectorMode();
							return true;
						}
						else {
							if (doubleTapAction == ReaderAction.START_SELECTION)
								return startSelection();
							return performAction(doubleTapAction, true);
						}
				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				int dx = 0;
				int dy = 0;
				int adx = 0;
				int ady = 0;
				int distance = 0;
				int dx2 = 0;
				int dy2 = 0;
				int adx2 = 0;
				int ady2 = 0;

				if (event.getPointerCount()>1) {
					now_x = (int)event.getX(0);
					now_y = (int)event.getY(0);
					now_x2 = (int)event.getX(1);
					now_y2 = (int)event.getY(1);
				} else {
					now_x = x;
					now_y = y;
				}

				if (now_x != 0)
					dx = now_x - start_x;
				if (now_y != 0)
					dy = now_y - start_y;
				adx = dx > 0 ? dx : -dx;
				ady = dy > 0 ? dy : -dy;
				distance = adx + ady;
				if (now_x2 != 0)
					dx2 = now_x2 - start_x2;
				if (now_y2 != 0)
					dy2 = now_y2 - start_y2;
				adx2 = dx2 > 0 ? dx2 : -dx2;
				ady2 = dy2 > 0 ? dy2 : -dy2;
				int dragThreshold = mActivity.getPalmTipPixels();
				switch (state) {
					case STATE_DOWN_1:
						if (distance < dragThreshold)
							return true;
						if ((DeviceInfo.SCREEN_CAN_CONTROL_BRIGHTNESS || DeviceInfo.EINK_HAVE_FRONTLIGHT) && isBacklightControlFlick != BACKLIGHT_CONTROL_FLICK_NONE && ady > adx) {
							// backlight control enabled
							if (DeviceInfo.ONYX_BRIGHTNESS_WARM && DeviceInfo.ONYX_BRIGHTNESS) {
								if (start_x < dragThreshold * 170 / 100 && isBacklightControlFlick > 0
										|| start_x > width - dragThreshold * 170 / 100 && isBacklightControlFlick > 0) {
									// brightness
									state = STATE_BRIGHTNESS;
									leftSideBrightness = start_x < dragThreshold * 170 / 100;
									startBrightnessControl(start_y, leftSideBrightness);
									return true;
								}
							} else {
								if (start_x < dragThreshold * 170 / 100 && isBacklightControlFlick == 1
										|| start_x > width - dragThreshold * 170 / 100 && isBacklightControlFlick == 2) {
									// brightness
									state = STATE_BRIGHTNESS;
									leftSideBrightness = start_x < dragThreshold * 170 / 100;
									startBrightnessControl(start_y, leftSideBrightness);
									return true;
								}
							}
						}


						boolean isPageMode = mSettings.getInt(PROP_PAGE_VIEW_MODE, 1) == 1;
						int dir = isPageMode ? x - start_x : y - start_y;
						if (Math.abs(mGesturePageFlipsPerFullSwipe) == 1) {
							dir *= mGesturePageFlipsPerFullSwipe; // Change sign of page flip direction according to user setting
							if (getPageFlipAnimationSpeedMs() == 0 || DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
								// no animation
								return performAction(dir < 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP, false);
							}
							startAnimation(start_x, start_y, width, height, x, y);
							updateAnimation(x, y);
							state = STATE_FLIPPING;
						}
							if (Math.abs(mGesturePageFlipsPerFullSwipe) > 1) {
							state = STATE_FLIP_TRACKING;
							updatePageFlipTracking(start_x, start_y);
						}
						return true;
					case STATE_FLIPPING:
						updateAnimation(x, y);
						return true;
					case STATE_BRIGHTNESS:
						updateBrightnessControl(start_y, now_y, leftSideBrightness);
						return true;
					case STATE_FLIP_TRACKING:
						updatePageFlipTracking(x, y);
						return true;	
					case STATE_WAIT_FOR_DOUBLE_CLICK:
						return true;
					case STATE_SELECTION:
						updateSelection(start_x, start_y, x, y, false);
						break;
					case STATE_TWO_POINTERS_VERT_MARGINS:
						if (event.getPointerCount()>1) {
							int distance1 = (int) (Math.sqrt(Math.abs(start_x2 - start_x) * Math.abs(start_x2 - start_x) +
									Math.abs(start_y2 - start_y) * Math.abs(start_y2 - start_y)));
							int distance2 = (int) (Math.sqrt(Math.abs(now_x2 - now_x) * Math.abs(now_x2 - now_x) +
									Math.abs(now_y2 - now_y) * Math.abs(now_y2 - now_y)));
							int aval = distance1 - distance2;
							aval = aval / PINCH_TRESHOLD_COUNT;
							marginToSet = OptionsDialog.getMarginShift(marginBegin, aval);
							showCenterPopup(mActivity.getString(R.string.vert_margin_control) + " " + marginToSet);
						}
						break;
					case STATE_TWO_POINTERS_HORZ_MARGINS:
						if (event.getPointerCount()>1) {
							int distance1 = (int) (Math.sqrt(Math.abs(start_x2 - start_x) * Math.abs(start_x2 - start_x) +
									Math.abs(start_y2 - start_y) * Math.abs(start_y2 - start_y)));
							int distance2 = (int) (Math.sqrt(Math.abs(now_x2 - now_x) * Math.abs(now_x2 - now_x) +
									Math.abs(now_y2 - now_y) * Math.abs(now_y2 - now_y)));
							int aval = distance1 - distance2;
							aval = aval / PINCH_TRESHOLD_COUNT;
							marginToSet = OptionsDialog.getMarginShift(marginBegin, aval);
							showCenterPopup(mActivity.getString(R.string.horz_margin_control)+" "+marginToSet);
						}
						break;
					case STATE_TWO_POINTERS_FONT_SCALE:
						if (event.getPointerCount()>1) {
							int distance1 = (int) (Math.sqrt(Math.abs(start_x2 - start_x) * Math.abs(start_x2 - start_x) +
									Math.abs(start_y2 - start_y) * Math.abs(start_y2 - start_y)));
							int distance2 = (int) (Math.sqrt(Math.abs(now_x2 - now_x) * Math.abs(now_x2 - now_x) +
									Math.abs(now_y2 - now_y) * Math.abs(now_y2 - now_y)));
							int aval = distance1 - distance2;
							aval = aval / PINCH_TRESHOLD_COUNT;
							fontSizeToSet = OptionsDialog.getFontSizeShift(fontSizeBegin, -aval);
							String s = StrUtils.getNonEmptyStr(curPageText,true);
							if (s.length()>50) s=s.substring(0,50)+"...";
							if (StrUtils.isEmptyStr(s)) s = "lorem ipsum";
							showCenterPopupFont(mActivity.getString(R.string.font_size_control)+" "+fontSizeToSet, s, fontSizeToSet);
						}
						break;
				}

			} else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
				return unexpectedEvent();
			}
			return true;
		}
	}


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

	public void findText(final String pattern, final boolean reverse, final boolean caseInsensitive)
	{
		BackgroundThread.ensureGUI();
		final ReaderView view = this;
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
				drawPage();
				FindNextDlg.showDialog( mActivity, view, pattern, caseInsensitive );
			}
			public void fail(Exception e) {
				BackgroundThread.ensureGUI();
				mActivity.showToast("Pattern not found");
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
//				drawPage();
				drawPage(true);
			}
		});
	}

	private boolean flgHighlightBookmarks = false;
	public void clearSelection()
	{
		BackgroundThread.ensureGUI();
		if (mBookInfo == null || !isBookLoaded())
			return;
		mEngine.post(new Task() {
			public void work() throws Exception {
				doc.clearSelection();
				invalidImages = true;
			}
			public void done() {
				if (surface.isShown())
					drawPage(true);
			}
		});
	}

	public void highlightBookmarks() {
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
			highlightBookmarks();
		}
		return removed;
	}

	public Bookmark updateBookmark(final Bookmark bookmark) {
		Bookmark bm = mBookInfo.updateBookmark(bookmark);
		if (bm != null) {
			scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			highlightBookmarks();
		}
		return bm;
	}

	public void addBookmark(final Bookmark bookmark) {
		mBookInfo.addBookmark(bookmark);
		highlightBookmarks();
		scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
	}

	public void addBookmark(final int shortcut) {
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
					highlightBookmarks();
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
			onCommand( action.cmd, action.param, onFinishHandler );
	}

	public void toggleDayNightMode() {
		Properties settings = getSettings();
		OptionsDialog.toggleDayNightMode(settings);
		//setSettings(settings, mActivity.settings());
		mActivity.setSettings(settings, 2000, true);
		invalidImages = true;
		if (getBookInfo() != null)
			if (getBookInfo().getFileInfo() != null)
			  if (getBookInfo().getFileInfo().askedShownStylesInfo<3) {
				  getBookInfo().getFileInfo().askedShownStylesInfo++;
				  checkOpenBookStyles(true);
			  }
		if (mActivity.getmReaderFrame() != null)
			 mActivity.getmReaderFrame().updateCRToolbar(((CoolReader) mActivity));
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
		boolean newBool = !mActivity.isFullscreen();
		String newValue = newBool ? "1" : "0";
		saveSetting(PROP_APP_FULLSCREEN, newValue);
		bNeedRedrawOnce = true;
	//	mActivity.showToast("bNeedRedrawOnce");
		mActivity.setFullscreen(newBool);
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
			SomeButtonsToolbarDlg.showDialog(mActivity, ReaderView.this, 5,true,
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
		Properties mProperties = new Properties(mActivity.settings());
		final ReaderAction action = ReaderAction.findById( mProperties.getProperty(propName) );
		if ((iv != null)&&(action != null)) {
			int iconId = action.iconId;
			if (iconId == 0) {
				iconId = Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
			}
			Drawable d = mActivity.getResources().getDrawable(action.getIconIdWithDef(mActivity));
			iv.setImageDrawable(d);
		}
		final ReaderAction longAction = ReaderAction.findById( mProperties.getProperty(longPropName) );
		final ImageView ivl = view.findViewById(R.id.zone_icon_long);
		if ((ivl != null)&&(longAction != null)) {
			int iconId = longAction.iconId;
			if (iconId == 0) {
				iconId = Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
			}
			Drawable d = mActivity.getResources().getDrawable(longAction.getIconIdWithDef(mActivity));
			ivl.setImageDrawable(d);
		}
		text.setText(mActivity.getResources().getText(action.nameId));
		longtext.setText(mActivity.getResources().getText(longAction.nameId));

		int colorIcon;
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorIcon});
		colorIcon = a.getColor(0, Color.GRAY);
		a.recycle();
		text.setTextColor(colorIcon);
		longtext.setTextColor(colorIcon);

		view.setOnClickListener(v -> dlg.cancel());
	}

	public void showReadingPositionPopup() {
		if (mBookInfo==null)
			return;
		final StringBuilder buf = new StringBuilder();
//		if (mActivity.isFullscreen()) {
		buf.append( Utils.formatTime(mActivity, System.currentTimeMillis()) +  " ");
		if (mBatteryState>=0)
			buf.append(" [" + mBatteryState + "%]; ");
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
					tvLabel.setTextColor(colorIcon);
					final BaseDialog dlg1 = dlg;
					grid.findViewById(R.id.lay_bottom_text).setOnClickListener(v -> dlg1.cancel());
					if (isBacklightControlFlick == BACKLIGHT_CONTROL_FLICK_NONE) {
						((ImageButton) grid.findViewById(R.id.tap_zone_show_btn_left)).setImageDrawable(null);
						((ImageButton) grid.findViewById(R.id.tap_zone_show_btn_right)).setImageDrawable(null);
					}
					if ((isBacklightControlFlick == BACKLIGHT_CONTROL_FLICK_LEFT) || (DeviceInfo.ONYX_BRIGHTNESS_WARM)) {
						((ImageButton) grid.findViewById(R.id.tap_zone_show_btn_right)).setImageDrawable(null);
					}
					if ((isBacklightControlFlick == BACKLIGHT_CONTROL_FLICK_RIGHT) || (DeviceInfo.ONYX_BRIGHTNESS_WARM)) {
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

	private int autoScrollSpeed = 1500; // chars / minute
	private int autoScrollNotificationId = 0;
	private AutoScrollAnimation currentAutoScrollAnimation = null;
	private boolean appPaused = false;

	private boolean isAutoScrollActive() {
		return currentAutoScrollAnimation != null;
	}

	private void stopAutoScroll() {
		if (!isAutoScrollActive())
			return;
		log.d("stopAutoScroll()");
		//notifyAutoscroll("Autoscroll is stopped");
		currentAutoScrollAnimation.stop();
	}

	public static final int AUTOSCROLL_START_ANIMATION_PERCENT = 5;

	private void startAutoScroll() {
		if (isAutoScrollActive())
			return;
		log.d("startAutoScroll()");
		currentAutoScrollAnimation = new AutoScrollAnimation(AUTOSCROLL_START_ANIMATION_PERCENT * 100);
		nextHiliteId++;
		hiliteRect = null;
	}

	private void toggleAutoScroll() {
		if (isAutoScrollActive())
			stopAutoScroll();
		else
			startAutoScroll();
	}

	private final static boolean AUTOSCROLL_SPEED_NOTIFICATION_ENABLED = false;
	private void notifyAutoscroll(final String msg) {
		if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
			return; // disable toast for eink
		if (AUTOSCROLL_SPEED_NOTIFICATION_ENABLED) {
			final int myId = ++autoScrollNotificationId;
			BackgroundThread.instance().postGUI(() -> {
				if (myId == autoScrollNotificationId)
					mActivity.showSToast(msg);
			}, 1000);
		}
	}

	private void notifyAutoscrollSpeed() {
		final String msg = mActivity.getString(R.string.lbl_autoscroll_speed).replace("$1", String.valueOf(autoScrollSpeed));
		notifyAutoscroll(msg);
	}

	private void changeAutoScrollSpeed(int delta) {
		if (autoScrollSpeed<300)
			delta *= 10;
		else if (autoScrollSpeed<500)
			delta *= 20;
		else if (autoScrollSpeed<1000)
			delta *= 40;
		else if (autoScrollSpeed<2000)
			delta *= 80;
		else if (autoScrollSpeed<5000)
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

	class AutoScrollAnimation {

		boolean isScrollView;
		BitmapInfo image1;
		BitmapInfo image2;
		PositionProperties currPos;
		int progress;
		int pageCount;
		int charCount;
		int timerInterval;
		long pageTurnStart;
		int nextPos;

		Paint[] shadePaints;
		Paint[] hilitePaints;

		final int startAnimationProgress;

		public static final int MAX_PROGRESS = 10000;
		public final static int ANIMATION_INTERVAL_NORMAL = 30;
		public final static int ANIMATION_INTERVAL_EINK = 5000;

		public AutoScrollAnimation(final int startProgress) {
			progress = startProgress;
			startAnimationProgress = AUTOSCROLL_START_ANIMATION_PERCENT * 100;
			currentAutoScrollAnimation = this;

			final int numPaints = 32;
			shadePaints = new Paint[numPaints];
			hilitePaints = new Paint[numPaints];
			for (int i=0; i<numPaints; i++) {
				shadePaints[i] = new Paint();
				hilitePaints[i] = new Paint();
				hilitePaints[i].setStyle(Paint.Style.FILL);
				shadePaints[i].setStyle(Paint.Style.FILL);
				if (mActivity.isNightMode()) {
					shadePaints[i].setColor(Color.argb((i+1)*128 / numPaints, 0, 0, 0));
					hilitePaints[i].setColor(Color.argb((i+1)*128 / numPaints, 128, 128, 128));
				} else {
					shadePaints[i].setColor(Color.argb((i+1)*128 / numPaints, 0, 0, 0));
					hilitePaints[i].setColor(Color.argb((i+1)*128 / numPaints, 255, 255, 255));
				}
			}

			BackgroundThread.instance().postBackground(() -> {
				if (initPageTurn(startProgress)) {
					log.d("AutoScrollAnimation: starting autoscroll timer");
					timerInterval = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) ? ANIMATION_INTERVAL_EINK : ANIMATION_INTERVAL_NORMAL;
					startTimer(timerInterval);
				} else {
					currentAutoScrollAnimation = null;
				}
			});
		}

		private int calcProgressPercent() {
			long duration = Utils.timeInterval(pageTurnStart);
			long estimatedFullDuration = 60000 * charCount / autoScrollSpeed;
			int percent = (int)(10000 * duration / estimatedFullDuration);
//			if (duration > estimatedFullDuration - timerInterval / 3)
//				percent = 10000;
			if (percent > 10000)
				percent = 10000;
			if (percent < 0)
				percent = 0;
			return percent;
		}

		private boolean onTimer() {
			int newProgress = calcProgressPercent();
			alog.v("onTimer(progress = " + newProgress + ")");
			mActivity.onUserActivity();
			progress = newProgress;
			if (progress == 0 || progress >= startAnimationProgress) {
				if (image1 != null && image2 != null) {
					if (image1.isReleased() || image2.isReleased()) {
						log.d("Images lost! Recreating images...");
						initPageTurn(progress);
					}
					draw();
				}
			}
			if (progress >= 10000) {
				if (!donePageTurn(true)) {
					stop();
					return false;
				}
				initPageTurn(0);
			}
			return true;
		}

		class AutoscrollTimerTask implements Runnable {
			final long interval;
			public AutoscrollTimerTask(long interval) {
				this.interval = interval;
				mActivity.onUserActivity();
				BackgroundThread.instance().postGUI(this, interval);
			}
			@Override
			public void run() {
				if (currentAutoScrollAnimation != AutoScrollAnimation.this) {
					log.v("timer is cancelled - GUI");
					return;
				}
				BackgroundThread.instance().postBackground(() -> {
					if (currentAutoScrollAnimation != AutoScrollAnimation.this) {
						log.v("timer is cancelled - BackgroundThread");
						return;
					}
					if (onTimer())
						BackgroundThread.instance().postGUI(AutoscrollTimerTask.this, interval);
					else
						log.v("timer is cancelled - onTimer returned false");
				});
			}
		}

		private void startTimer(final int interval) {
			new AutoscrollTimerTask(interval);
		}

		private boolean initPageTurn(int startProgress) {
			cancelGc();
			log.v("initPageTurn(startProgress = " + startProgress + ")");
			pageTurnStart = Utils.timeStamp();
			progress = startProgress;
			currPos = doc.getPositionProps(null, true);
			charCount = currPos.charCount;
			pageCount = currPos.pageMode;
			if (charCount < 150)
				charCount = 150;
			isScrollView = currPos.pageMode == 0;
			log.v("initPageTurn(charCount = " + charCount + ")");
			if (isScrollView) {
				image1 = preparePageImage(0);
				if (image1 == null) {
					log.v("ScrollViewAnimation -- not started: image is null");
					return false;
				}
				int pos0 = image1.position.y;
				int pos1 = pos0 + image1.position.pageHeight * 9/10;
				if (pos1 > image1.position.fullHeight - image1.position.pageHeight)
					pos1 = image1.position.fullHeight - image1.position.pageHeight;
				if (pos1 < 0)
					pos1 = 0;
				nextPos = pos1;
				image2 = preparePageImage(pos1 - pos0);
				if (image2 == null) {
					log.v("ScrollViewAnimation -- not started: image is null");
					return false;
				}
			} else {
				int page1 = currPos.pageNumber;
				int page2 = currPos.pageNumber + 1;
				if (page2 < 0 || page2 >= currPos.pageCount) {
					currentAnimation = null;
					return false;
				}
				image1 = preparePageImage(0);
				image2 = preparePageImage(1);
				if (page1 == page2) {
					log.v("PageViewAnimation -- cannot start animation: not moved");
					return false;
				}
				if (image1 == null || image2 == null) {
					log.v("PageViewAnimation -- cannot start animation: page image is null");
					return false;
				}

			}
			long duration = android.os.SystemClock.uptimeMillis() - pageTurnStart;
			log.v("AutoScrollAnimation -- page turn initialized in " + duration + " millis");
			currentAutoScrollAnimation = this;
			draw();
			return true;
		}


		private boolean donePageTurn(boolean turnPage) {
			log.v("donePageTurn()");
			if (turnPage) {
				if (isScrollView)
					doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, nextPos);
				else
					doc.doCommand(ReaderCommand.DCMD_PAGEDOWN.nativeId, 1);
			}
			progress = 0;
			//draw();
			return currPos.canMoveToNextPage();
		}

		public void draw()
		{
			draw(true);
		}

		public void draw(boolean isPartially)
		{
			//	long startTs = android.os.SystemClock.uptimeMillis();
			drawCallback(this::draw, null, isPartially);
		}

		public void stop() {
			currentAutoScrollAnimation = null;
			BackgroundThread.instance().executeBackground(() -> {
				donePageTurn(wantPageTurn());
				//redraw();
				drawPage(null, false);
				scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			});
			scheduleGc();
		}

		private boolean wantPageTurn() {
			return (progress > (startAnimationProgress + MAX_PROGRESS) / 2);
		}

		private void drawGradient( Canvas canvas, Rect rc, Paint[] paints, int startIndex, int endIndex ) {
			//log.v("drawShadow");
			int n = (startIndex<endIndex) ? endIndex-startIndex+1 : startIndex-endIndex + 1;
			int dir = (startIndex<endIndex) ? 1 : -1;
			int dx = rc.bottom - rc.top;
			Rect rect = new Rect(rc);
			for (int i=0; i<n; i++) {
				int index = startIndex + i*dir;
				int x1 = rc.top + dx*i/n;
				int x2 = rc.top + dx*(i+1)/n;
				if (x1 < 0)
					x1 = 0;
				if (x2 > canvas.getHeight())
					x2 = canvas.getHeight();
				rect.top = x1;
				rect.bottom = x2;
				if (x2 > x1) {
					//log.v("drawShadow : " + x1 + ", " + x2 + ", " + index);
					canvas.drawRect(rect, paints[index]);
				}
			}
		}

		private void drawShadow( Canvas canvas, Rect rc ) {
			drawGradient(canvas, rc, shadePaints, shadePaints.length * 3 / 4, 0);
		}

		void drawPageProgress(Canvas canvas, int scrollPercent, Rect dst, Rect src) {
			int shadowHeight = 32;
			int h = dst.height();
			int div = (h + shadowHeight) * scrollPercent / 10000 - shadowHeight;
			//log.v("drawPageProgress() div = " + div + ", percent = " + scrollPercent);
			int d = Math.max(div, 0);
			if (d > 0) {
				Rect src1 = new Rect(src.left, src.top, src.right, src.top + d);
				Rect dst1 = new Rect(dst.left, dst.top, dst.right, dst.top + d);
				drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
			}
			if (d < h) {
				Rect src2 = new Rect(src.left, src.top + d, src.right, src.bottom);
				Rect dst2 = new Rect(dst.left, dst.top + d, dst.right, dst.bottom);
				drawDimmedBitmap(canvas, image1.bitmap, src2, dst2);
			}
			if (scrollPercent > 0 && scrollPercent < 10000) {
				Rect shadowRect = new Rect(src.left, src.top + div, src.right, src.top + div + shadowHeight);
				drawShadow(canvas, shadowRect);
			}
		}

		public void draw(Canvas canvas) {
			if (currentAutoScrollAnimation != this)
				return;
			alog.v("AutoScrollAnimation.draw(" + progress + ")");
			if (progress!=0 && progress<startAnimationProgress)
				return; // don't draw page w/o started animation
			int scrollPercent = 10000 * (progress - startAnimationProgress) / (MAX_PROGRESS - startAnimationProgress);
			if (scrollPercent < 0)
				scrollPercent = 0;
			int w = image1.bitmap.getWidth();
			int h = image1.bitmap.getHeight();
			if (isScrollView) {
				// scroll
				drawPageProgress(canvas, scrollPercent, new Rect(0, 0, w, h), new Rect(0, 0, w, h));
			} else {
				if (image1.isReleased() || image2.isReleased())
					return;
				if (pageCount==2) {
					if (scrollPercent<5000) {
						// < 50%
						scrollPercent = scrollPercent * 2;
						drawPageProgress(canvas, scrollPercent, new Rect(0, 0, w/2, h), new Rect(0, 0, w/2, h));
						drawPageProgress(canvas, 0, new Rect(w/2, 0, w, h), new Rect(w/2, 0, w, h));
					} else {
						// >=50%
						scrollPercent = (scrollPercent - 5000) * 2;
						drawPageProgress(canvas, 10000, new Rect(0, 0, w/2, h), new Rect(0, 0, w/2, h));
						drawPageProgress(canvas, scrollPercent, new Rect(w/2, 0, w, h), new Rect(w/2, 0, w, h));
					}
				} else {
					drawPageProgress(canvas, scrollPercent, new Rect(0, 0, w, h), new Rect(0, 0, w, h));
				}
			}
		}
	}

	public void onCommand(final ReaderCommand cmd, final int param) {
		onCommand( cmd, param, null );
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

	public void onCommand(final ReaderCommand cmd, final int param, final Runnable onFinishHandler) {
		BackgroundThread.ensureGUI();
		log.i("On command " + cmd + (param!=0?" ("+param+")":" "));
		boolean eink = false;
		switch ( cmd ) {
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
			toggleAutoScroll();
			break;
		case DCMD_AUTOSCROLL_SPEED_INCREASE:
			changeAutoScrollSpeed(1);
			break;
		case DCMD_AUTOSCROLL_SPEED_DECREASE:
			changeAutoScrollSpeed(-1);
			break;
		case DCMD_SHOW_DICTIONARY:
			mActivity.showDictionary();
			break;
		case DCMD_OPEN_PREVIOUS_BOOK:
			loadPreviousDocument(() -> {
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
				if ((!BaseActivity.PRO_FEATURES)&&(!BaseActivity.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				log.i("DCMD_TTS_PLAY: initializing TTS");
				if (!mActivity.initTTS(tts -> {
					log.i("TTS created: opening TTS toolbar");
					ttsToolbar = TTSToolbarDlg.showDialog(mActivity, ReaderView.this, tts);
					ttsToolbar.setOnCloseListener(() -> ttsToolbar = null);
				})) {
					log.e("Cannot initialize TTS");
				}
			}
			break;
		case DCMD_TOGGLE_DOCUMENT_STYLES:
			if (isBookLoaded())
				toggleDocumentStyles();
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
			Properties props1 = new Properties(mActivity.settings());
			props1.setProperty(PROP_FONT_WEIGHT_EMBOLDEN,
					props1.getBool(PROP_FONT_WEIGHT_EMBOLDEN,false)?"0":"1");
			mActivity.setSettings(props1, -1, true);
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
							(curBlackpageInterval > mActivity.getScreenBlackpageInterval() - 1)) {
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
		case DCMD_GO_PERCENT_DIALOG:
			if (isBookLoaded())
				showGoToPercentDialog();
			break;
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
			showFilterDialog();
			break;
		case DCMD_READER_MENU:
			mActivity.showReaderMenu();
			break;
		case DCMD_TOGGLE_DAY_NIGHT_MODE:
			//ExternalDocCameDialog dlgE = new ExternalDocCameDialog(mActivity,"sd","dsf");
			//dlgE.show();
//			ArrayList<String[]> vl = new ArrayList<String[]>();
//			String[] arrS1 = {"val1", "hint1"};
//			vl.add(arrS1);
//			String[] arrS2 = {"val2", "hint2"};
//			vl.add(arrS2);
//			AskSomeValuesDialog dlgA = new AskSomeValuesDialog(mActivity, "asd", "asd 2", vl,null);
//			dlgA.show();
//			ArrayList<String> sButtons = new ArrayList<String>();
//			sButtons.add("adfad");
//			sButtons.add("*adfad2");
//			sButtons.add("adfad3");
//			sButtons.add("adfad4");
//			SomeButtonsToolbarDlg.showDialog(mActivity, ReaderView.this, true, "title",
//					sButtons,null);
			//CloudAction.yndCheckCrFolder(mActivity);
			//final String sF = s;
			toggleDayNightMode();
//			OrientationToolbarDlg.showDialog(mActivity, ReaderView.this,
//					0, true);

//			mActivity.geoLastData.lastStation =mActivity.geoLastData.tempStation;
//			mActivity.geoLastData.lastStop =mActivity.geoLastData.tempStop;
//			mActivity.geoLastData.doSignal(false,false);
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
				if (iSyncVariant2 == 1) CloudSync.loadSettingsFiles(((CoolReader)mActivity),false);
				else
					CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
							CloudSync.CLOUD_SAVE_SETTINGS, false, iSyncVariant2 == 1, false, false);
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
						CloudSync.CLOUD_SAVE_READING_POS, false, iSyncVariant3 == 1, false, false);
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
						CloudSync.CLOUD_SAVE_BOOKMARKS, false, iSyncVariant4 == 1, false, false);
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
			if (!BaseActivity.PREMIUM_FEATURES) {
				mActivity.showToast(R.string.only_in_premium);
				break;
			}
			CloudAction.yndOpenBookDialog(mActivity, null,true);
			break;
		case DCMD_OPEN_BOOK_FROM_CLOUD_DBX:
			log.i("Open book from CLOUD_DBX");
			if (!BaseActivity.PREMIUM_FEATURES) {
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
			mActivity.showActionsToolbarMenu(actions, new CRToolBar.OnActionHandler() {
				@Override
				public boolean onActionSelected(ReaderAction item) {
					if (item == ReaderAction.SAVE_SETTINGS_TO_CLOUD) {
						log.i("Save settings to CLOUD");
						int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.saveSettingsToFilesOrCloud(((CoolReader) mActivity), false, iSyncVariant == 1);
						}
						return true;
					} else if (item == ReaderAction.LOAD_SETTINGS_FROM_CLOUD) {
						log.i("Load settings from CLOUD");
						int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							if (iSyncVariant == 1) CloudSync.loadSettingsFiles(((CoolReader)mActivity),false);
							else
								CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
										CloudSync.CLOUD_SAVE_SETTINGS, false, iSyncVariant == 1, false, false);
						}
						return true;
					} else if (item == ReaderAction.SAVE_READING_POS) {
						log.i("Save reading pos to CLOUD");
						int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.saveJsonInfoFileOrCloud(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_READING_POS, false, iSyncVariant == 1, false);
						}
						Bookmark bmk = getCurrentPositionBookmark();
						lastSavedToGdBookmark = bmk;
						return true;
					} else if (item == ReaderAction.LOAD_READING_POS) {
						log.i("Load reading pos from CLOUD");
						int iSyncVariant2 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant2 == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_READING_POS, false, iSyncVariant2 == 1, false, false);
						}
						return true;
					} else if (item == ReaderAction.SAVE_BOOKMARKS) {
						log.i("Save bookmarks to CLOUD");
						int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.saveJsonInfoFileOrCloud(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_BOOKMARKS, false, iSyncVariant == 1, false);
						}
						return true;
					} else if (item == ReaderAction.LOAD_BOOKMARKS) {
						log.i("Load bookmarks from CLOUD");
						int iSyncVariant3 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
						if (iSyncVariant3 == 0) {
							mActivity.showToast(mActivity.getString(R.string.cloud_sync_variant1_v));
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_CLOUD_TITLE);
						} else {
							CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_BOOKMARKS, false, iSyncVariant3 == 1, false, false);
						}
						return true;
					} else if (item == ReaderAction.SAVE_CURRENT_BOOK_TO_CLOUD_YND) {
						log.i("Save current book to YND");
						FileInfo fi = mActivity.getReaderView().getBookInfo().getFileInfo();
						CloudAction.yndOpenBookDialog(mActivity, fi,true);
						return true;
					} else if (item == ReaderAction.OPEN_BOOK_FROM_CLOUD_YND) {
						log.i("Open book from CLOUD_YND");
						if (!BaseActivity.PREMIUM_FEATURES) {
							mActivity.showToast(R.string.only_in_premium);
							return true;
						}
						CloudAction.yndOpenBookDialog(mActivity, null,true);
						return true;
					} else if (item == ReaderAction.OPEN_BOOK_FROM_CLOUD_DBX) {
						log.i("Open book from CLOUD_DBX");
						if (!BaseActivity.PREMIUM_FEATURES) {
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
				}
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
				mActivity.showActionsToolbarMenu(fonts_actions, new CRToolBar.OnActionHandler() {
					@Override
					public boolean onActionSelected(ReaderAction item) {
						if (item == ReaderAction.FONT_PREVIOUS) {
							switchFontFace(-1);
							return true;
						} else if (item == ReaderAction.FONT_NEXT) {
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
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_FONT_FACE);
							return true;
						} else if (item == ReaderAction.FONT_BOLD) {
							Properties props = new Properties(mActivity.settings());
							props.setProperty(PROP_FONT_WEIGHT_EMBOLDEN,
									props.getBool(PROP_FONT_WEIGHT_EMBOLDEN,false)?"0":"1");
							mActivity.setSettings(props, -1, true);
							return true;
						} else if (item == ReaderAction.CHOOSE_TEXTURE) {
							mActivity.optionsFilter = "";
							mActivity.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_PAGE_BACKGROUND_IMAGE);
							return true;
						}
						return false;
					}
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
				addBookmark(0);
				break;
			case DCMD_SHOW_USER_DIC:
				if ((!BaseActivity.PRO_FEATURES)&&(!BaseActivity.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				UserDicDlg dlg = new UserDicDlg(((CoolReader)mActivity),0);
				dlg.show();
				break;
			case DCMD_SAVE_BOOKMARK_LAST_SEL_USER_DIC:
				if ((!BaseActivity.PRO_FEATURES)&&(!BaseActivity.PREMIUM_FEATURES)) {
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
				if ((!BaseActivity.PRO_FEATURES)&&(!BaseActivity.PREMIUM_FEATURES)) {
					mActivity.showToast(R.string.only_in_pro);
					break;
				}
				UserDicDlg dlg2 = new UserDicDlg(((CoolReader)mActivity),1);
				dlg2.show();
				break;
			case DCMD_TOGGLE_PAGE_VIEW_MODE:
				String oldViewSetting = this.getSetting( ReaderView.PROP_PAGE_VIEW_MODE );
                boolean newBool = this.getSetting( ReaderView.PROP_PAGE_VIEW_MODE ).equals("0");
                String newValue = newBool ? "1" : "0";
                saveSetting(PROP_PAGE_VIEW_MODE, newValue);
				break;
			case DCMD_WHOLE_PAGE_TO_DIC:
				log.i("Whole page to dic");
				String s = mActivity.getmReaderFrame().getUserDicPanel().getCurPageText(0, false);
				//mActivity.showToast(s.substring(0,100));
				mActivity.findInDictionary( s , null);
				//mActivity.mDictionaries.setiDic2IsActive(2);
				break;
			case DCMD_GOOGLEDRIVE_SYNC:
				if (0 == param) {							// sync to
					mActivity.forceSyncToGoogleDrive();
				} else if (1 == param) {					// sync from
					mActivity.forceSyncFromGoogleDrive();
				}
				break;
			default:
				// do nothing
				break;
		}
	}
	boolean firstShowBrowserCall = true;


	public TTSToolbarDlg ttsToolbar;
	public void stopTTS() {
		if (ttsToolbar != null)
			ttsToolbar.pause();
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
					drawPage( doneHandler, false );
				}
				if (isMoveCommand && isBookLoaded())
					scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			}
		});
	}

	// update book and position info in status bar
	private void updateCurrentPositionStatus() {
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

	volatile private boolean mInitialized = false;
	volatile private boolean mOpened = false;

	//private File historyFile;

	private void updateLoadedBookInfo()
	{
		BackgroundThread.ensureBackground();
		// get title, authors, etc.
		doc.updateBookInfo(mBookInfo);
		updateCurrentPositionStatus();
		// check whether current book properties updated on another devices
		// TODO: fix and reenable
		//syncUpdater.syncExternalChanges(mBookInfo);
	}

	private void applySettings(Properties props)
	{
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
		props.setInt(PROP_STATUS_LINE, props.getInt(PROP_STATUS_LOCATION, VIEWER_STATUS_TOP) == VIEWER_STATUS_PAGE ? 0 : 1);
		if (props.getInt(PROP_STATUS_LOCATION, VIEWER_STATUS_TOP) == VIEWER_STATUS_PAGE_2LINES)
			props.setInt(PROP_STATUS_LINE, 2);
		int updMode      = props.getInt(PROP_APP_SCREEN_UPDATE_MODE, 0);
		int updInterval  = props.getInt(PROP_APP_SCREEN_UPDATE_INTERVAL, 10);

		int blackpageInterval  = props.getInt(PROP_APP_SCREEN_BLACKPAGE_INTERVAL, 0);
        blackpageDuration  = props.getInt(PROP_APP_SCREEN_BLACKPAGE_DURATION, 300);
		mActivity.setScreenUpdateMode(updMode, surface);
		mActivity.setScreenUpdateInterval(updInterval, surface);
		mActivity.setScreenBlackpageInterval(blackpageInterval);
		mActivity.setScreenBlackpageDuration(blackpageDuration);

		getActivity().readResizeHistory();

		if (null != mBookInfo) {
			FileInfo fileInfo = mBookInfo.getFileInfo();
			final String bookLanguage = fileInfo.getLanguage();
			final String fontFace = props.getProperty(PROP_FONT_FACE);
			String fcLangCode = null;
			if (null != bookLanguage && bookLanguage.length() > 0) {
				fcLangCode = Engine.findCompatibleFcLangCode(bookLanguage);
				if (props.getBool(PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false))
					props.setProperty(PROP_TEXTLANG_MAIN_LANG, bookLanguage);
			}
			if (null != fcLangCode && fcLangCode.length() > 0) {
				boolean res = Engine.checkFontLanguageCompatibility(fontFace, fcLangCode);
				log.d("Checking font \"" + fontFace + "\" for compatibility with language \"" + bookLanguage + "\" fcLangCode=" + fcLangCode + ": res=" + res);
				if (!res) {
					BackgroundThread.instance().executeGUI(() -> mActivity.showToast(R.string.font_not_compat_with_language, fontFace, bookLanguage));
				}
			} else {
				if (null != bookLanguage)
					log.d("Can't find compatible language code in embedded FontConfig catalog: language=\"" + bookLanguage + "\" bookInfo=" + fileInfo);
			}
		}
		doc.applySettings(props);
		//syncViewSettings(props, save, saveDelayed);
		drawPage();
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
		return new Properties(mSettings);
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
		if (!manual.exists() || lastHelpFileContentId == null || !lastHelpFileContentId.equals(helpFileContentId)) {
			log.d("Generating help file " + manual.getAbsolutePath());
			mActivity.setLastGeneratedHelpFileSignature(helpFileContentId);
			manual = generator.generateHelpFile(bookDir);
		}
		return manual;
	}

	private int getPageFlipAnimationSpeedMs() {
		return pageFlipAnimationMode!=PAGE_ANIMATION_NONE ? pageFlipAnimationSpeed : 0;
	}

	/**
	 * Generate help file (if necessary) and show it.
	 * @return true if opened successfully
	 */
	public boolean showManual() {
		return loadDocument(getManualFileName(), "", null, () -> mActivity.showToast(R.string.manual_open_error));
	}

	private boolean hiliteTapZoneOnTap = false;
	private boolean enableVolumeKeys = true;
	static private final int DEF_PAGE_FLIP_MS = 300;
	public void applyAppSetting(String key, String value) {
		boolean flg = "1".equals(value);
		if (key.equals(PROP_APP_TAP_ZONE_HILIGHT)) {
			hiliteTapZoneOnTap = flg;
		} else if (key.equals(PROP_APP_DOUBLE_TAP_SELECTION)) {
			doubleTapSelectionEnabled = flg;
		} else if (key.equals(PROP_APP_GESTURE_PAGE_FLIPPING)) {
			mGesturePageFlipsPerFullSwipe = Integer.valueOf(value);
		} else if (key.equals(PROP_APP_DISABLE_TWO_POINTER_GESTURES)) {
			mDisableTwoPointerGestures = "1".equals(value);
		}else if (key.equals(PROP_APP_SECONDARY_TAP_ACTION_TYPE)) {
			secondaryTapActionType = flg ? TAP_ACTION_TYPE_DOUBLE : TAP_ACTION_TYPE_LONGPRESS;
		} else if (key.equals(PROP_APP_FLICK_BACKLIGHT_CONTROL)) {
			isBacklightControlFlick = "1".equals(value) ? 1 : ("2".equals(value) ? 2 : 0);
		} else if (PROP_APP_HIGHLIGHT_BOOKMARKS.equals(key)) {
			flgHighlightBookmarks = !"0".equals(value);
			clearSelection();
		} else if (PROP_APP_VIEW_AUTOSCROLL_SPEED.equals(key)) {
			int n = 1500;
			try {
				n = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				// ignore
			}
			if (n < 200)
				n = 200;
			if (n > 10000)
				n = 10000;
			autoScrollSpeed = n;
		} else if (PROP_PAGE_ANIMATION.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				if (n < 0 || n > PAGE_ANIMATION_MAX)
					n = PAGE_ANIMATION_SLIDE2;
				pageFlipAnimationMode = n;
			} catch ( Exception e ) {
				// ignore
			}
			//pageFlipAnimationSpeedMs = pageFlipAnimationMode!=PAGE_ANIMATION_NONE ? DEF_PAGE_FLIP_MS : 0;
		} else if (PROP_PAGE_ANIMATION_SPEED.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				pageFlipAnimationSpeed = n;
			} catch ( Exception e ) {
				// ignore
			}
			//pageFlipAnimationSpeedMs = pageFlipAnimationMode!=PAGE_ANIMATION_NONE ? DEF_PAGE_FLIP_MS : 0;
		} else if (PROP_CONTROLS_ENABLE_VOLUME_KEYS.equals(key)) {
			enableVolumeKeys = flg;
		} else if (PROP_APP_SELECTION_ACTION.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mSelectionAction = n;
			} catch ( Exception e ) {
				// ignore
			}
		} else if (PROP_APP_MULTI_SELECTION_ACTION.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mMultiSelectionAction = n;
			} catch ( Exception e ) {
				// ignore
			}
		} else if (PROP_APP_SELECTION_ACTION_LONG.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mSelectionActionLong = n;
			} catch ( Exception e ) {
				// ignore
			}
		}  else if (PROP_APP_SELECTION2_ACTION.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mSelection2Action = n;
			} catch ( Exception e ) {
				// ignore
			}
		} else if (PROP_APP_MULTI_SELECTION2_ACTION.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mMultiSelection2Action = n;
			} catch ( Exception e ) {
				// ignore
			}
		} else if (PROP_APP_SELECTION2_ACTION_LONG.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mSelection2ActionLong = n;
			} catch ( Exception e ) {
				// ignore
			}
		}  else if (PROP_APP_SELECTION3_ACTION.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mSelection3Action = n;
			} catch ( Exception e ) {
				// ignore
			}
		} else if (PROP_APP_MULTI_SELECTION3_ACTION.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mMultiSelection3Action = n;
			} catch ( Exception e ) {
				// ignore
			}
		} else if (PROP_APP_SELECTION3_ACTION_LONG.equals(key)) {
			try {
				int n = Integer.valueOf(value);
				mSelection3ActionLong = n;
			} catch ( Exception e ) {
				// ignore
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
			applyAppSetting( key, value );
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
					|| PROP_APP_SCREEN_BACKLIGHT_WARM.equals(key)
					|| PROP_APP_USE_EINK_FRONTLIGHT.equals(key)
					|| PROP_APP_SCREEN_BACKLIGHT_EINK.equals(key)
					|| PROP_APP_BOOK_PROPERTY_SCAN_ENABLED.equals(key)
					|| PROP_APP_SCREEN_BACKLIGHT_LOCK.equals(key)
					|| PROP_APP_TAP_ZONE_HILIGHT.equals(key)
					|| PROP_APP_DICTIONARY.equals(key)
					|| PROP_APP_DOUBLE_TAP_SELECTION.equals(key)
					|| PROP_APP_FLICK_BACKLIGHT_CONTROL.equals(key)
					|| PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS.equals(key)
					|| PROP_APP_SELECTION_ACTION.equals(key)
					|| PROP_APP_FILE_BROWSER_SIMPLE_MODE.equals(key)
					|| PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE.equals(key)

					|| PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES.equals(key)
					|| PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES.equals(key)
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
		setAppSettings( newSettings, currSettings );
		Properties changedSettings = newSettings.diff(currSettings);
		currSettings.setAll(changedSettings);
		mSettings = currSettings;
		BackgroundThread.instance().postBackground(() -> applySettings(currSettings));
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
		if (!currentBackgroundTexture.equals(texture) || currentBackgroundColor != color) {
			log.d("setBackgroundTexture( " + texture + " )");
			currentBackgroundColor = color;
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
	class CreateViewTask extends Task
	{
		Properties props = new Properties();
		public CreateViewTask( Properties props ) {
			this.props = props;
			Properties oldSettings = new Properties(); // may be changed by setAppSettings
			setAppSettings(props, oldSettings);
			props.setAll(oldSettings);
			mSettings = props;
		}
		public void work() throws Exception {
			BackgroundThread.ensureBackground();
			log.d("CreateViewTask - in background thread");
//			BackgroundTextureInfo[] textures = mEngine.getAvailableTextures();
//			byte[] data = mEngine.getImageData(textures[3]);
			byte[] data = mEngine.getImageData(currentBackgroundTexture);
			doc.setPageBackgroundTexture(data, currentBackgroundTexture.tiled?1:0);

			//File historyDir = activity.getDir("settings", Context.MODE_PRIVATE);
			//File historyDir = new File(Environment.getExternalStorageDirectory(), ".cr3");
			//historyDir.mkdirs();
			//File historyFile = new File(historyDir, "cr3hist.ini");

			//File historyFile = new File(activity.getDir("settings", Context.MODE_PRIVATE), "cr3hist.ini");
			//if ( historyFile.exists() ) {
			//log.d("Reading history from file " + historyFile.getAbsolutePath());
			//readHistoryInternal(historyFile.getAbsolutePath());
			//}
			String css = mEngine.loadResourceUtf8(R.raw.fb2);
			if (css != null && css.length()>0)
				doc.setStylesheet(css);
			applySettings(props);
			mInitialized = true;
			log.i("CreateViewTask - finished");
		}
		public void done() {
			log.d("InitializationFinishedEvent");
			//BackgroundThread.ensureGUI();
			//setSettings(props, new Properties());
		}
		public void fail( Exception e )
		{
			log.e("CoolReader engine initialization failed. Exiting.", e);
			mEngine.fatalError("Failed to init CoolReader engine");
		}
	}

	public void closeIfOpened(final FileInfo fileInfo) {
		if (this.mBookInfo != null && this.mBookInfo.getFileInfo().pathname.equals(fileInfo.pathname) && mOpened) {
			close();
		}
	}

	public boolean reloadDocument() {
		if (this.mBookInfo!=null && this.mBookInfo.getFileInfo() != null) {
			save(); // save current position
			post(new LoadDocumentTask(this.mBookInfo, null, null, null));
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
					post(new LoadDocumentTask(bookInfo, null, doneHandler, errorHandler));
				});
			});
		});
	}

	public boolean loadDocument(final FileInfo fileInfo, final Runnable doneHandler, final Runnable errorHandler) {
		log.v("loadDocument(" + fileInfo.getPathName() + ")");
		if (this.mBookInfo != null && this.mBookInfo.getFileInfo().pathname.equals(fileInfo.pathname) && mOpened) {
			log.d("trying to load already opened document");
			mActivity.showReader();
			if (null != doneHandler)
				doneHandler.run();
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
			mActivity.askConfirmation(mActivity.getString(R.string.warn_hang)+" "+bmk.bookFile,
					() -> postLoadTask(fileInfo, doneHandler, errorHandler), () -> {
				mActivity.showRootWindow();
				if (doc != null) {
					String sFile = mActivity.getSettingsFileF(0).getParent() + "/cur_pos0.json";
					File f = new File(sFile);
					if (f.exists()) f.delete();
				}
			});
		} else postLoadTask(fileInfo, doneHandler, errorHandler);
		return true;
	}

	private void postLoadTaskStream(final InputStream inputStream, final FileInfo fileInfo,
									final Runnable doneHandler, final Runnable errorHandler) {
		Bookmark bmk = new Bookmark();
		bmk.bookFile = "stream";
		bmk.bookPath = "stream";
		bmk.bookFileArc = "stream";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String prettyJson = gson.toJson(bmk);
		mActivity.saveCurPosFile(true, prettyJson);
		Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), fileInfo, bookInfo -> {
			log.v("posting LoadDocument task to background thread");
			BackgroundThread.instance().postBackground(() -> {
				log.v("posting LoadDocument task to GUI thread");
				BackgroundThread.instance().postGUI(() -> {
					log.v("synced posting LoadDocument task to GUI thread");
					post(new LoadDocumentTask(bookInfo, inputStream, doneHandler, errorHandler));
				});
			});
		});
	}

	public boolean loadDocumentFromStream(final InputStream inputStream, final FileInfo fileInfo, final Runnable doneHandler, final Runnable errorHandler) {
		log.v("loadDocument(" + fileInfo.getPathName() + ")");
		if (this.mBookInfo != null && this.mBookInfo.getFileInfo().pathname.equals(fileInfo.pathname) && mOpened) {
			log.d("trying to load already opened document");
			mActivity.showReader();
			if (null != doneHandler)
				doneHandler.run();
			drawPage();
			return false;
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
		if ((needAsk) && (!mActivity.settings().getBool(Settings.PROP_APP_DISABLE_SAFE_MODE, false))) {
			mActivity.askConfirmation(mActivity.getString(R.string.warn_hang)+" (stream)",
					() -> postLoadTaskStream(inputStream, fileInfo, doneHandler, errorHandler), () -> {
						mActivity.showRootWindow();
						if (doc != null) {
							String sFile = mActivity.getSettingsFileF(0).getParent() + "/cur_pos0.json";
							File f = new File(sFile);
							if (f.exists()) f.delete();
						}
					});
		} else postLoadTaskStream(inputStream, fileInfo, doneHandler, errorHandler);
		return true;
	}

	/**
	 * When current book is opened, switch to previous book.
	 * @param errorHandler
	 * @return
	 */
	public boolean loadPreviousDocument(final Runnable errorHandler) {
		BackgroundThread.ensureGUI();
		BookInfo bi = Services.getHistory().getPreviousBook();
		if (bi!=null && bi.getFileInfo()!=null) {
			save();
			log.i("loadPreviousDocument() is called, prevBookName = " + bi.getFileInfo().getPathName());
			return loadDocument(bi.getFileInfo().getPathName(), "", null, errorHandler);
		}
		errorHandler.run();
		return false;
	}

	public boolean loadDocument(String fileName, String fileLink, final Runnable doneHandler, final Runnable errorHandler) {
		lastSelection = null;
		hyplinkBookmark = null;
		lastSavedToGdBookmark = null;
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
		//Fix for Onyx - not needed
//		if (fileName.contains("/mnt/sdcard")) {
//			File file = new File(fileName);
//			if (!file.exists()) {
//				File tfile = new File(fileName.replace("/mnt/sdcard", "/storage/emulated/0"));
//				if (tfile.exists()) {
//					fileName = fileName.replace("/mnt/sdcard", "/storage/emulated/0");
//				}
//			}
//		}
		String normalized = mEngine.getPathCorrector().normalize(fileName);
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
		if (!StrUtils.isEmptyStr(fileLink)) {
			fi.opdsLink = fileLink;
			mActivity.getDB().saveBookInfo(new BookInfo(fi));
			mActivity.getDB().flush();
		}
		return loadDocument(fi, doneHandler, errorHandler);
	}

	public boolean loadDocumentFromStream(InputStream inputStream, String contentPath, final Runnable doneHandler, final Runnable errorHandler) {
		BackgroundThread.ensureGUI();
		save();
		log.i("loadDocument(" + contentPath + ")");
		if (contentPath == null || inputStream == null) {
			log.v("loadDocument() : no filename or stream specified");
			if (errorHandler != null)
				errorHandler.run();
			return false;
		}
		BookInfo book = Services.getHistory().getBookInfo(contentPath);
		if (book != null)
			log.v("loadDocument() : found book in history : " + book);
		FileInfo fi = null;
		if (book == null) {
			log.v("loadDocument() : book not found in history, building FileInfo by Uri...");
			fi = new FileInfo(contentPath);
		} else {
			fi = book.getFileInfo();
			log.v("loadDocument() : item from history : " + fi);
		}
		return loadDocumentFromStream(inputStream, fi, doneHandler, errorHandler);
	}

	public BookInfo getBookInfo() {
		BackgroundThread.ensureGUI();
		return mBookInfo;
	}

	private int currentCloudSyncProgressPosition = -1;
	private String currentCloudSyncProgressTitle;

	public void showCloudSyncProgress(int progress) {
		log.v("showClodSyncProgress(" + progress + ")");
		boolean update = false;
		if (null == currentCloudSyncProgressTitle) {
			currentCloudSyncProgressTitle = mActivity.getString(R.string.cloud_synchronization_);
			update = true;
		}
		if (currentCloudSyncProgressPosition != progress) {
			currentCloudSyncProgressPosition = progress;
			update = true;
		}
		if (update)
			bookView.draw(true);
	}

	public void hideSyncProgress() {
		//hideProgress();
		log.v("hideSyncProgress()");
		if (currentCloudSyncProgressTitle != null || currentCloudSyncProgressPosition != -1) {
			currentCloudSyncProgressPosition = -1;
			currentCloudSyncProgressTitle = null;
			bookView.draw(false);
		}
	}

	private boolean isCloudSyncProgressActive() {
		return currentCloudSyncProgressPosition > 0;
	}

	private int mBatteryState = 100;
	public void setBatteryState(int state) {
		if (state != mBatteryState) {
			log.i("Battery state changed: " + state);
			mBatteryState = state;
			if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) && !isAutoScrollActive()) {
				drawPage();
			}
		}
	}

	public int getBatteryState() {
		return mBatteryState;
	}

	private static final VMRuntimeHack runtime = new VMRuntimeHack();

	private static class BitmapFactory {
		public static final int MAX_FREE_LIST_SIZE=2;
		ArrayList<Bitmap> freeList = new ArrayList<Bitmap>();
		ArrayList<Bitmap> usedList = new ArrayList<Bitmap>();
		public synchronized Bitmap get(int dx, int dy) {
			for (int i=0; i<freeList.size(); i++) {
				Bitmap bmp = freeList.get(i);
				if (bmp.getWidth() == dx && bmp.getHeight() == dy) {
					// found bitmap of proper size
					freeList.remove(i);
					usedList.add(bmp);
					//log.d("BitmapFactory: reused free bitmap, used list = " + usedList.size() + ", free list=" + freeList.size());
					return bmp;
				}
			}
			for (int i=freeList.size()-1; i>=0; i--) {
				Bitmap bmp = freeList.remove(i);
				runtime.trackAlloc(bmp.getWidth() * bmp.getHeight() * 2);
				//log.d("Recycling free bitmap "+bmp.getWidth()+"x"+bmp.getHeight());
				//bmp.recycle(); //20110109 
			}
			Bitmap bmp = Bitmap.createBitmap(dx, dy, DeviceInfo.getBufferColorFormat(BaseActivity.getScreenForceEink()));
			runtime.trackFree(dx*dy*2);
			//bmp.setDensity(0);
			usedList.add(bmp);
			//log.d("Created new bitmap "+dx+"x"+dy+". New bitmap list size = " + usedList.size());
			return bmp;
		}
		public synchronized void compact() {
			while ( freeList.size()>0 ) {
				//freeList.get(0).recycle();//20110109
				Bitmap bmp = freeList.remove(0);
				runtime.trackAlloc(bmp.getWidth() * bmp.getHeight() * 2);
			}
		}
		public synchronized void release(Bitmap bmp) {
			for (int i=0; i<usedList.size(); i++) {
				if (usedList.get(i) == bmp) {
					freeList.add(bmp);
					usedList.remove(i);
					while ( freeList.size()>MAX_FREE_LIST_SIZE ) {
						//freeList.get(0).recycle(); //20110109
						Bitmap b = freeList.remove(0);
						runtime.trackAlloc(b.getWidth() * b.getHeight() * 2);
						//b.recycle();
					}
					log.d("BitmapFactory: bitmap released, used size = " + usedList.size() + ", free size=" + freeList.size());
					return;
				}
			}
			// unknown bitmap, just recycle
			//bmp.recycle();//20110109
		}
	};
	BitmapFactory factory = new BitmapFactory();

	class BitmapInfo {
		Bitmap bitmap;
		PositionProperties position;
		ImageInfo imageInfo;
		void recycle()
		{
			factory.release(bitmap);
			bitmap = null;
			position = null;
			imageInfo = null;
		}
		boolean isReleased() {
			return bitmap == null;
		}
		@Override
		public String toString() {
			return "BitmapInfo [position=" + position + "]";
		}

	}

	private BitmapInfo mCurrentPageInfo;
	private BitmapInfo mNextPageInfo;
	/**
	 * Prepare and cache page image.
	 * Cache is represented by two slots: mCurrentPageInfo and mNextPageInfo.  
	 * If page already exists in cache, returns it (if current page requested, 
	 *  ensures that it became stored as mCurrentPageInfo; if another page requested, 
	 *  no mCurrentPageInfo/mNextPageInfo reordering made).
	 * @param offset is kind of page: 0==current, -1=previous, 1=next page
	 * @return page image and properties, null if requested page is unavailable (e.g. requested next/prev page is out of document range)
	 */
	private BitmapInfo preparePageImage(int offset) {
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
				//if (checkNeedRedraw(internalDX, internalDY))
					doc.resize(internalDX, internalDY);
			} else {
				internalDX = surface.getWidth();
				internalDY = surface.getHeight();
				//if (checkNeedRedraw(internalDX, internalDY))
					doc.resize(internalDX, internalDY);
			}
//			internalDX=200;
//			internalDY=300;
//			doc.resize(internalDX, internalDY);
//			BackgroundThread.instance().postGUI(new Runnable() {
//				@Override
//				public void run() {
//					log.d("invalidating view due to resize");
//					//ReaderView.this.invalidate();
//					drawPage(null, false);
//					//redraw();
//				}
//			});
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
			BitmapInfo bi = new BitmapInfo();
			bi.position = currpos;
			bi.bitmap = factory.get(internalDX > 0 ? internalDX : requestedWidth,
					internalDY > 0 ? internalDY : requestedHeight);
			doc.setBatteryState(mBatteryState);
			doc.getPageImage(bi.bitmap);
			mCurrentPageInfo = bi;
			//log.v("Prepared new current page image " + mCurrentPageInfo);
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
					BitmapInfo bi = new BitmapInfo();
					bi.position = nextpos;
					bi.bitmap = factory.get(internalDX, internalDY);
					doc.setBatteryState(mBatteryState);
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
					BitmapInfo bi = new BitmapInfo();
					bi.position = nextpos;
					bi.bitmap = factory.get(internalDX, internalDY);
					doc.setBatteryState(mBatteryState);
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

	private int lastDrawTaskId = 0;
	private class DrawPageTask extends Task {
		final int id;
		BitmapInfo bi;
		Runnable doneHandler;
		boolean isPartially;
		DrawPageTask(Runnable doneHandler, boolean isPartially)
		{
//			// DEBUG stack trace
//			try {
//				throw new Exception("DrawPageTask() stack trace");
//			} catch (Exception e) {
//				Log.d("cr3", "stack trace", e);
//			}
			this.id = ++lastDrawTaskId;
			this.doneHandler = doneHandler;
			this.isPartially = isPartially;
			cancelGc();
		}
		public void work() {
			BackgroundThread.ensureBackground();
			if (this.id != lastDrawTaskId) {
				log.d("skipping duplicate drawPage request");
				return;
			}
			nextHiliteId++;
			if (currentAnimation != null) {
				log.d("skipping drawPage request while scroll animation is in progress");
				return;
			}
			log.e("DrawPageTask.work("+internalDX+","+internalDY+")");
			bi = preparePageImage(0);
			if (bi != null) {
				bookView.draw(isPartially);
			}
		}
		@Override
		public void done()
		{
			BackgroundThread.ensureGUI();
//			log.d("drawPage : bitmap is ready, invalidating view to draw new bitmap");
//			if (bi != null) {
//				setBitmap( bi.bitmap );
//				invalidate();
//			}
//    		if (mOpened)
			//hideProgress();
			if (doneHandler != null)
				doneHandler.run();
			scheduleGc();
		}
		@Override
		public void fail(Exception e) {
			hideProgress();
		}
	};

	static class ReaderSurfaceView extends SurfaceView {
		public ReaderSurfaceView( Context context )
		{
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
	private int lastsetWidth = 0;
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

	private void requestResize(int width, int height) {
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
			if (iSett > 0) {
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
			//getActivity().showToast("requestResize (and skipped): "+width+", "+height);
            if (mActivity.getmReaderFrame()!=null)
                if (mActivity.getmReaderFrame().getUserDicPanel()!=null)
                    mActivity.getmReaderFrame().getUserDicPanel().updateSavingMark(mActivity.getString(R.string.request_resize)+": "+width+", "+height);
			return;
		}
		lastsetWidth = width;
		lastsetHeight = height;
		lastsetResTime = System.currentTimeMillis();
		if (bNeed) {
		    if (mActivity.getmReaderFrame()!=null)
             	if (mActivity.getmReaderFrame().getUserDicPanel()!=null)
					mActivity.getmReaderFrame().getUserDicPanel().updateSavingMark(mActivity.getString(R.string.resizing_to)+": "+width+", "+height);
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
			if ((iSett > 0) && (iOrnt != 4)) {
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

	private void checkSize() {
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
					//if (checkNeedRedraw(internalDX, internalDY))
						doc.resize(internalDX, internalDY);
//	    		        if (mOpened) {
//	    					log.d("ResizeTask: done, drawing page");
//	    			        drawPage();
//	    		        }
				}
				public void done() {
					clearImageCache();
					drawPage(null, false);
					//redraw();
				}
			});
		};

		long timeSinceLastResume = System.currentTimeMillis() - lastAppResumeTs;
		int delay = 300;

		if (timeSinceLastResume < 1000)
			delay = 1000;

		if (mOpened) {
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
		//if (!isProgressActive())
		bookView.draw();
		//requestResize(width, height);
		//draw();
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



	private ViewAnimationControl currentAnimation = null;

	//private int pageFlipAnimationSpeedMs = DEF_PAGE_FLIP_MS; // if 0 : no animation
	private int pageFlipAnimationMode = PAGE_ANIMATION_SLIDE2; //PAGE_ANIMATION_PAPER; // if 0 : no animation
	private int pageFlipAnimationSpeed = DEF_PAGE_FLIP_MS;
	//	private void animatePageFlip( final int dir ) {
//		animatePageFlip(dir, null);
//	}
	private void animatePageFlip(final int dir, final Runnable onFinishHandler) {
		if (!mOpened)
			return;
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
					new PageViewAnimation(fromX, w, dir2);
					if (currentAnimation != null) {
						if (currentAnimation != null) {
							nextHiliteId++;
							hiliteRect = null;
							currentAnimation.update(toX, h/2);
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
					new ScrollViewAnimation(fromY, h);
					if (currentAnimation != null) {
						if (currentAnimation != null) {
							nextHiliteId++;
							hiliteRect = null;
							currentAnimation.update(w/2, toY);
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

	volatile private int nextHiliteId = 0;
	private final static int HILITE_RECT_ALPHA = 32;
	private Rect hiliteRect = null;
	private void unhiliteTapZone() {
		hiliteTapZone( false, 0, 0, surface.getWidth(), surface.getHeight() );
	}
	private void hiliteTapZone(final boolean hilite, final int startX, final int startY, final int maxX, final int maxY) {
		alog.d("highliteTapZone("+startX + ", " + startY+")");
		final int myHiliteId = ++nextHiliteId;
		int txcolor = mSettings.getColor(PROP_FONT_COLOR, Color.BLACK);
		final int color = (txcolor & 0xFFFFFF) | (HILITE_RECT_ALPHA<<24);
		BackgroundThread.instance().executeBackground(() -> {
			if (myHiliteId != nextHiliteId || (!hilite && hiliteRect == null))
				return;

			if (currentAutoScrollAnimation!=null) {
				hiliteRect = null;
				return;
			}

			BackgroundThread.ensureBackground();
			final BitmapInfo pageImage = preparePageImage(0);
			if (pageImage != null && pageImage.bitmap != null && pageImage.position != null) {
				//PositionProperties currPos = pageImage.position;
				final Rect rc = hilite ? tapZoneBounds( startX, startY, maxX, maxY ) : hiliteRect;
				if (hilite)
					hiliteRect = rc;
				else
					hiliteRect = null;
				if (rc != null)
					drawCallback(canvas -> {
						if (mInitialized && mCurrentPageInfo != null) {
							log.d("onDraw() -- drawing page image");
							drawDimmedBitmap(canvas, mCurrentPageInfo.bitmap, rc, rc);
							if (hilite) {
								Paint p = new Paint();
								p.setColor(color);
//					    			if ( true ) {
								canvas.drawRect(new Rect(rc.left, rc.top, rc.right-2, rc.top+2), p);
								canvas.drawRect(new Rect(rc.left, rc.top+2, rc.left+2, rc.bottom-2), p);
								canvas.drawRect(new Rect(rc.right-2-2, rc.top+2, rc.right-2, rc.bottom-2), p);
								canvas.drawRect(new Rect(rc.left+2, rc.bottom-2-2, rc.right-2-2, rc.bottom-2), p);
//					    			} else {
//					    				canvas.drawRect(rc, p);
//					    			}
							}
						}
					}, rc, false);
			}
		});
	}
	private void scheduleUnhilite(int delay) {
		final int myHiliteId = nextHiliteId;
		BackgroundThread.instance().postGUI(() -> {
			if (myHiliteId == nextHiliteId && hiliteRect != null)
				unhiliteTapZone();
		}, delay);
	}

	int currentBrightnessValueIndex = -1;
	int currentBrightnessValueWarmIndex = -1;
	int lastBrightnessValueIndex = -1;
	int lastBrightnessValueIndexWarm = -1;

	private void startBrightnessControl(final int startY, final boolean leftSide)
	{
		int currentBrightnessValue = mActivity.getScreenBacklightLevel();
		int currentBrightnessValueWarm = mActivity.getScreenBacklightLevelWarm();
		boolean onyxWarm = (!leftSide) && DeviceInfo.ONYX_BRIGHTNESS_WARM && (!mActivity.isOnyxBrightControl());
		if (!onyxWarm) {
			if (!mActivity.isOnyxBrightControl())
				currentBrightnessValueIndex = OptionsDialog.findBacklightSettingIndex(currentBrightnessValue);
			else
				currentBrightnessValueIndex = Utils.findNearestIndex(EinkScreen.getFrontLightLevels(mActivity), currentBrightnessValue);
			if (lastBrightnessValueIndex == -1) lastBrightnessValueIndex = currentBrightnessValueIndex;
		} else {
			if (!mActivity.isOnyxBrightControl())
				currentBrightnessValueWarmIndex = OptionsDialog.findBacklightSettingIndex(currentBrightnessValueWarm);
			else
				currentBrightnessValueWarmIndex = Utils.findNearestIndex(EinkScreen.getFrontLightLevels(mActivity), currentBrightnessValueWarm);
			if (lastBrightnessValueIndexWarm == -1) lastBrightnessValueIndexWarm = currentBrightnessValueWarmIndex;
		}
		updateBrightnessControl(startY, startY, leftSide);
	}

	private void updateBrightnessControl(final int y_start, final int y, final boolean leftSide) {
		List<Integer> levelList = null;
		int count = 0;
		if (!mActivity.isOnyxBrightControl())
			count = OptionsDialog.mBacklightLevels.length;
		else {
			levelList = EinkScreen.getFrontLightLevels(mActivity);
			for (int i: levelList) log.i("levelList " + i);
			if (null != levelList)
				count = levelList.size();
			else
				return;
		}
		if (0 == count)
			return;
		boolean onyxWarm = (!leftSide) && DeviceInfo.ONYX_BRIGHTNESS_WARM && (!mActivity.isOnyxBrightControl());
		int index1 = count - 1 - y * count / surface.getHeight();
		int index2 = count - 1 - y_start * count / surface.getHeight();
		log.i("index1 " + index1);
		log.i("index2 " + index2);
		log.i("count " + count);

		//index1 = index1 / 4 * 3;
		//index2 = index2 / 4 * 3;
		int index = 0;
		if (!onyxWarm)
			index = index1 - index2 + lastBrightnessValueIndex;
		if (onyxWarm)
			index = index1 - index2 + lastBrightnessValueIndexWarm;
		//int index = currentBrightnessValueIndex + (currentBrightnessValueIndex * aval) / 100;
		if (index < 0)
			index = 0;
		else if (index >= count)
			index = count-1;
		log.i("index " + index);
		int curBrightnessValueIndex = -1;
		if (!onyxWarm) curBrightnessValueIndex = currentBrightnessValueIndex;
		if (onyxWarm) curBrightnessValueIndex = currentBrightnessValueWarmIndex;
		if (index != curBrightnessValueIndex) {
			if (!onyxWarm) currentBrightnessValueIndex = index;
			else
				currentBrightnessValueWarmIndex = index;
			int newValue = 0;
			if (mActivity.isOnyxBrightControl())
				newValue = levelList.get(index);
			else
				newValue = OptionsDialog.mBacklightLevels[index];
			mActivity.setScreenBacklightLevel(newValue, leftSide);
			//if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			if (newValue < 1) newValue = 1;
			if (onyxWarm) {
				if (leftSide) {
					int indexR = 0;
					indexR = lastBrightnessValueIndexWarm;
					if (indexR < 0)
						indexR = 0;
					else if (indexR >= count)
						indexR = count-1;
					int newValueR = 0;
					if (!mActivity.isOnyxBrightControl())
						newValueR = OptionsDialog.mBacklightLevels[indexR];
					else
						newValueR = levelList.get(indexR);
					if (newValueR>=0)
						showCenterPopup(newValue + "% / " + newValueR + "%", 2000);
					else
						showCenterPopup(newValue + "%", 2000);
				} else {
					int indexL = 0;
					indexL = lastBrightnessValueIndex;
					if (indexL < 0)
						indexL = 0;
					else if (indexL >= count)
						indexL = count-1;
					int newValueL = 0;
					if (!mActivity.isOnyxBrightControl())
						newValueL = OptionsDialog.mBacklightLevels[indexL];
					else
						newValueL = Utils.findNearestIndex(EinkScreen.getFrontLightLevels(mActivity), indexL);
					if (newValueL>=0)
						showCenterPopup(newValueL + "% / " + newValue + "%", 2000);
					else
						showCenterPopup(newValue + "%", 2000);
				}
			} else {
				showCenterPopup(newValue + "%");
			}
			//}
		}

	}

	private void stopBrightnessControl(final int y_start, final int y, final boolean leftSide) {
		boolean onyxWarm = (!leftSide) && DeviceInfo.ONYX_BRIGHTNESS_WARM && (!mActivity.isOnyxBrightControl());
		int curBrightnessValueIndex = currentBrightnessValueIndex;
		if (onyxWarm) curBrightnessValueIndex = currentBrightnessValueWarmIndex;
		if (curBrightnessValueIndex >= 0) {
			if (y_start >= 0 && y >= 0) {
				updateBrightnessControl(y_start, y, leftSide);
			}
			if (mActivity.isOnyxBrightControl()) {
				mSettings.setInt(PROP_APP_SCREEN_BACKLIGHT_EINK, OptionsDialog.findBacklightSettingIndex(curBrightnessValueIndex));
			} else {
				if (!onyxWarm)
					mSettings.setInt(PROP_APP_SCREEN_BACKLIGHT, OptionsDialog.mBacklightLevels[curBrightnessValueIndex]);
				else
					mSettings.setInt(PROP_APP_SCREEN_BACKLIGHT_WARM, OptionsDialog.mBacklightLevels[curBrightnessValueIndex]);
			}
			if (showBrightnessFlickToast) {
				OptionsDialog.mBacklightLevelsTitles[0] = mActivity.getString(R.string.options_app_backlight_screen_default);
				String s = OptionsDialog.mBacklightLevelsTitles[currentBrightnessValueIndex];
				mActivity.showToast(s);
			}
			saveSettings(mSettings);
			if (!onyxWarm) {
				lastBrightnessValueIndex = curBrightnessValueIndex;
				currentBrightnessValueIndex = -1;
			}
			else {
				lastBrightnessValueIndexWarm = curBrightnessValueIndex;
				currentBrightnessValueWarmIndex = -1;
			}
		}
	}

	private static final boolean showBrightnessFlickToast = false;


	private void startAnimation(final int startX, final int startY, final int maxX, final int maxY, final int newX, final int newY) {
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
				new PageViewAnimation(sx, maxX, dir);
			} else {
				new ScrollViewAnimation(startY, maxY);
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
	private void updateAnimation(final int x, final int y) {
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

	private void stopAnimation(final int x, final int y) {
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
	private void scheduleAnimation() {
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

	private interface DrawCanvasCallback {
		void drawTo(Canvas c);
	}
	private void drawCallback(DrawCanvasCallback callback, Rect rc, boolean isPartially)
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
			try {
				canvas = holder.lockCanvas(rc);
				//log.v("before draw(canvas)");
				if (canvas != null) {
					if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())){
						EinkScreen.PrepareController(surface, isPartially);
					}
					callback.drawTo(canvas);
					if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())){
						EinkScreen.UpdateController(surface, isPartially);
					}
				}
			} finally {
				//log.v("exiting finally");
				if (canvas != null && surface.getHolder() != null) {
					//log.v("before unlockCanvasAndPost");
					if (canvas != null && holder != null) {
						holder.unlockCanvasAndPost(canvas);
						//if ( rc==null ) {
						long endTs = android.os.SystemClock.uptimeMillis();
						updateAnimationDurationStats(endTs - startTs);
						//}
					}
					//log.v("after unlockCanvasAndPost");
				}
			}
		}
		//log.v("exiting draw()");
	}

	abstract class ViewAnimationBase implements ViewAnimationControl {
		//long startTimeStamp;
		boolean started;
		public boolean isStarted()
		{
			return started;
		}

		ViewAnimationBase()
		{
			//startTimeStamp = android.os.SystemClock.uptimeMillis();
			cancelGc();
		}

		public void close()
		{
			animationScheduler.cancel();
			currentAnimation = null;
			scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			lastSavedBookmark = null;
			updateCurrentPositionStatus();

			scheduleGc();
		}

		public void draw()
		{
			draw(false);
		}

		public void draw(boolean isPartially)
		{
			//	long startTs = android.os.SystemClock.uptimeMillis();
			drawCallback(this::draw, null, isPartially);
		}
	}

	//private static final int PAGE_ANIMATION_DURATION = 3000;
	class ScrollViewAnimation extends ViewAnimationBase {
		int startY;
		int maxY;
		int pageHeight;
		int fullHeight;
		int pointerStartPos;
		int pointerDestPos;
		int pointerCurrPos;
		BitmapInfo image1;
		BitmapInfo image2;
		ScrollViewAnimation(int startY, int maxY) {
			super();
			this.startY = startY;
			this.maxY = maxY;
			long start = android.os.SystemClock.uptimeMillis();
			log.v("ScrollViewAnimation -- creating: drawing two pages to buffer");
			PositionProperties currPos = doc.getPositionProps(null, false	);
			int pos = currPos.y;
			int pos0 = pos - (maxY - startY);
			if (pos0 < 0)
				pos0 = 0;
			pointerStartPos = pos;
			pointerCurrPos = pos;
			pointerDestPos = startY;
			pageHeight = currPos.pageHeight;
			fullHeight = currPos.fullHeight;
			doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, pos0);
			image1 = preparePageImage(0);
			if (image1 == null) {
				log.v("ScrollViewAnimation -- not started: image is null");
				return;
			}
			image2 = preparePageImage(image1.position.pageHeight);
			doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, pos);
			if (image2 == null) {
				log.v("ScrollViewAnimation -- not started: image is null");
				return;
			}
			long duration = android.os.SystemClock.uptimeMillis() - start;
			log.v("ScrollViewAnimation -- created in " + duration + " millis");
			currentAnimation = this;
		}

		@Override
		public void stop(int x, int y) {
			if (currentAnimation == null)
				return;
			//if ( started ) {
			if (y != -1) {
				int delta = startY - y;
				pointerCurrPos = pointerStartPos + delta;
			}
			if (pointerCurrPos < 0)
				pointerCurrPos = 0;
			if (pointerCurrPos > fullHeight - pageHeight)
				pointerCurrPos = fullHeight - pageHeight;
			pointerDestPos = pointerCurrPos;
			draw();
			doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, pointerDestPos);
			//}
			scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			close();
		}

		@Override
		public void move(int duration, boolean accelerated) {
			if (duration > 0  && getPageFlipAnimationSpeedMs() != 0) {
				int steps = (int)(duration / getAvgAnimationDrawDuration()) + 2;
				int x0 = pointerCurrPos;
				int x1 = pointerDestPos;
				if ((x0 - x1) < 10 && (x0 - x1) > -10)
					steps = 2;
				for (int i = 1; i < steps; i++) {
					int x = x0 + (x1-x0) * i / steps;
					pointerCurrPos = accelerated ? accelerate( x0, x1, x ) : x;
					if (pointerCurrPos < 0)
						pointerCurrPos = 0;
					if (pointerCurrPos > fullHeight - pageHeight)
						pointerCurrPos = fullHeight - pageHeight;
					draw();
				}
			}
			pointerCurrPos = pointerDestPos;
			draw();
		}

		@Override
		public void update(int x, int y) {
			int delta = startY - y;
			pointerDestPos = pointerStartPos + delta;
			if (pointerDestPos < 0)
				pointerDestPos = 0;
			if (pointerDestPos > fullHeight - pageHeight)
				pointerDestPos = fullHeight - pageHeight;
		}

		@Override
		public void animate()
		{
			//log.d("animate() is called");
			if (pointerDestPos != pointerCurrPos) {
				if (!started)
					started = true;
				if (getPageFlipAnimationSpeedMs() == 0)
					pointerCurrPos = pointerDestPos;
				else {
					int delta = pointerCurrPos-pointerDestPos;
					if (delta < 0)
						delta = -delta;
					long avgDraw = getAvgAnimationDrawDuration();
					//int maxStep = (int)(maxY * PAGE_ANIMATION_DURATION / avgDraw);
					int maxStep = getPageFlipAnimationSpeedMs() > 0 ? (int)(maxY * 1000 / avgDraw / getPageFlipAnimationSpeedMs()) : maxY;
					int step;
					if (delta > maxStep * 2)
						step = maxStep;
					else
						step = (delta + 3) / 4;
					//int step = delta<3 ? 1 : (delta<5 ? 2 : (delta<10 ? 3 : (delta<15 ? 6 : (delta<25 ? 10 : (delta<50 ? 15 : 30))))); 
					if (pointerCurrPos < pointerDestPos)
						pointerCurrPos += step;
					else
						pointerCurrPos -= step;
					log.d("animate("+pointerCurrPos + " => " + pointerDestPos + "  step=" + step + ")");
				}
				//pointerCurrPos = pointerDestPos;
				draw();
				if (pointerDestPos != pointerCurrPos)
					scheduleAnimation();
			}
		}

		public void draw(Canvas canvas)
		{
//			BitmapInfo image1 = mCurrentPageInfo;
//			BitmapInfo image2 = mNextPageInfo;
			if (image1 == null || image1.isReleased() || image2 == null || image2.isReleased())
				return;
			int h = image1.position.pageHeight;
			int rowsFromImg1 = image1.position.y + h - pointerCurrPos;
			int rowsFromImg2 = h - rowsFromImg1;
			Rect src1 = new Rect(0, h-rowsFromImg1, mCurrentPageInfo.bitmap.getWidth(), h);
			Rect dst1 = new Rect(0, 0, mCurrentPageInfo.bitmap.getWidth(), rowsFromImg1);
			drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
			if (image2 != null) {
				Rect src2 = new Rect(0, 0, mCurrentPageInfo.bitmap.getWidth(), rowsFromImg2);
				Rect dst2 = new Rect(0, rowsFromImg1, mCurrentPageInfo.bitmap.getWidth(), h);
				drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
			}
			//log.v("anim.drawScroll( pos=" + pointerCurrPos + ", " + src1 + "=>" + dst1 + ", " + src2 + "=>" + dst2 + " )");
		}
	}

	private final static int SIN_TABLE_SIZE = 1024;
	private final static int SIN_TABLE_SCALE = 0x10000;
	private final static int PI_DIV_2 = (int)(Math.PI / 2 * SIN_TABLE_SCALE);
	/// sin table, for 0..PI/2
	private static int[] SIN_TABLE = new int[SIN_TABLE_SIZE+1];
	private static int[] ASIN_TABLE = new int[SIN_TABLE_SIZE+1];
	// mapping of 0..1 shift to angle
	private static int[] SRC_TABLE = new int[SIN_TABLE_SIZE+1];
	// mapping of 0..1 shift to sin(angle)
	private static int[] DST_TABLE = new int[SIN_TABLE_SIZE+1];
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

	class PageViewAnimation extends ViewAnimationBase {
		int startX;
		int maxX;
		int page1;
		int page2;
		int direction;
		int currShift;
		int destShift;
		int pageCount;
		Paint divPaint;
		Paint[] shadePaints;
		Paint[] hilitePaints;
		int pageFlipAnimationM;

		BitmapInfo image1;
		BitmapInfo image2;
		Bitmap image1scaled;
		Bitmap image2scaled;

		PageViewAnimation(int startX, int maxX, int direction) {
			super();
			this.startX = startX;
			this.maxX = maxX;
			this.direction = direction;
			this.currShift = 0;
			this.destShift = 0;
			this.pageFlipAnimationM = pageFlipAnimationMode;

			long start = android.os.SystemClock.uptimeMillis();
			log.v("PageViewAnimation -- creating: drawing two pages to buffer");

			PositionProperties currPos = mCurrentPageInfo == null ? null : mCurrentPageInfo.position;
			if (currPos == null)
				currPos = doc.getPositionProps(null, false);
			page1 = currPos.pageNumber;
			page2 = currPos.pageNumber + direction;
			if (page2 < 0 || page2 >= currPos.pageCount) {
				currentAnimation = null;
				return;
			}
			this.pageCount = currPos.pageMode;
			image1 = preparePageImage(0);
			image1scaled = null;
			if (image1!=null)
				image1scaled = Bitmap.createScaledBitmap(
					image1.bitmap, image1.bitmap.getWidth()/4, image1.bitmap.getHeight()/4, false);
			image2 = preparePageImage(direction);
			image2scaled = null;
			if (image2!=null)
				image2scaled = Bitmap.createScaledBitmap(
					image2.bitmap, image2.bitmap.getWidth()/4, image2.bitmap.getHeight()/4, false);
			if (image1 == null || image2 == null) {
				log.v("PageViewAnimation -- cannot start animation: page image is null");
				return;
			}
			if (page1 == page2) {
				log.v("PageViewAnimation -- cannot start animation: not moved");
				return;
			}
			page2 = image2.position.pageNumber;
			currentAnimation = this;
			divPaint = new Paint();
			divPaint.setStyle(Paint.Style.FILL);
			divPaint.setColor(mActivity.isNightMode() ? Color.argb(96, 64, 64, 64) : Color.argb(128, 128, 128, 128));
			final int numPaints = 16;
			shadePaints = new Paint[numPaints];
			hilitePaints = new Paint[numPaints];
			for (int i=0; i<numPaints; i++) {
				shadePaints[i] = new Paint();
				hilitePaints[i] = new Paint();
				hilitePaints[i].setStyle(Paint.Style.FILL);
				shadePaints[i].setStyle(Paint.Style.FILL);
				if (mActivity.isNightMode()) {
					shadePaints[i].setColor(Color.argb((i+1)*96 / numPaints, 0, 0, 0));
					hilitePaints[i].setColor(Color.argb((i+1)*96 / numPaints, 64, 64, 64));
				} else {
					shadePaints[i].setColor(Color.argb((i+1)*96 / numPaints, 0, 0, 0));
					hilitePaints[i].setColor(Color.argb((i+1)*96 / numPaints, 255, 255, 255));
				}
			}


			long duration = android.os.SystemClock.uptimeMillis() - start;
			log.d("PageViewAnimation -- created in " + duration + " millis");
		}

		private void drawGradient(Canvas canvas, Rect rc, Paint[] paints, int startIndex, int endIndex) {
			int n = (startIndex<endIndex) ? endIndex-startIndex+1 : startIndex-endIndex + 1;
			int dir = (startIndex<endIndex) ? 1 : -1;
			int dx = rc.right - rc.left;
			Rect rect = new Rect(rc);
			for (int i=0; i<n; i++) {
				int index = startIndex + i*dir;
				int x1 = rc.left + dx*i/n;
				int x2 = rc.left + dx*(i+1)/n;
				if (x2 > rc.right)
					x2 = rc.right;
				rect.left = x1;
				rect.right = x2;
				if (x2 > x1) {
					canvas.drawRect(rect, paints[index]);
				}
			}
		}

		private void drawShadow(Canvas canvas, Rect rc) {
			drawGradient(canvas, rc, shadePaints, shadePaints.length/2, shadePaints.length/10);
		}

		private final static int DISTORT_PART_PERCENT = 30;
		private void drawDistorted( Canvas canvas, Bitmap bmp, Rect src, Rect dst, int dir) {
			int srcdx = src.width();
			int dstdx = dst.width();
			int dx = srcdx - dstdx;
			int maxdistortdx = srcdx * DISTORT_PART_PERCENT / 100;
			int maxdx = maxdistortdx * (PI_DIV_2 - SIN_TABLE_SCALE) / SIN_TABLE_SCALE;
			int maxdistortsrc = maxdistortdx * PI_DIV_2 / SIN_TABLE_SCALE;

			int distortdx = dx < maxdistortdx ? dx : maxdistortdx;
			int distortsrcstart = -1;
			int distortsrcend = -1;
			int distortdststart = -1;
			int distortdstend = -1;
			int distortanglestart = -1;
			int distortangleend = -1;
			int normalsrcstart = -1;
			int normalsrcend = -1;
			int normaldststart = -1;
			int normaldstend = -1;

			if (dx < maxdx) {
				// start
				int index = dx>=0 ? dx * SIN_TABLE_SIZE / maxdx : 0;
				if (index > DST_TABLE.length)
					index = DST_TABLE.length;
				int dstv = DST_TABLE[index] * maxdistortdx / SIN_TABLE_SCALE;
				distortdststart = distortsrcstart = dstdx - dstv;
				distortsrcend = srcdx;
				distortdstend = dstdx;
				normalsrcstart = normaldststart = 0;
				normalsrcend = distortsrcstart;
				normaldstend = distortdststart;
				distortanglestart = 0;
				distortangleend = SRC_TABLE[index];
				distortdx = maxdistortdx;
			} else if (dstdx>maxdistortdx) {
				// middle
				distortdststart = distortsrcstart = dstdx - maxdistortdx;
				distortsrcend = distortsrcstart + maxdistortsrc;
				distortdstend = dstdx;
				normalsrcstart = normaldststart = 0;
				normalsrcend = distortsrcstart;
				normaldstend = distortdststart;
				distortanglestart = 0;
				distortangleend = PI_DIV_2;
			} else {
				// end
				normalsrcstart = normaldststart = normalsrcend = normaldstend = -1;
				distortdx = dstdx;
				distortsrcstart = 0;
				int n = maxdistortdx >= dstdx ? maxdistortdx - dstdx : 0;
				distortsrcend = ASIN_TABLE[SIN_TABLE_SIZE * n/maxdistortdx ] * maxdistortsrc / SIN_TABLE_SCALE;
				distortdststart = 0;
				distortdstend = dstdx;
				distortangleend = PI_DIV_2;
				n = maxdistortdx >= distortdx ? maxdistortdx - distortdx : 0;
				distortanglestart = ASIN_TABLE[SIN_TABLE_SIZE * (maxdistortdx - distortdx)/maxdistortdx ];
			}

			Rect srcrc = new Rect(src);
			Rect dstrc = new Rect(dst);
			if (normalsrcstart < normalsrcend) {
				if (dir > 0) {
					srcrc.left = src.left + normalsrcstart;
					srcrc.right = src.left + normalsrcend;
					dstrc.left = dst.left + normaldststart;
					dstrc.right = dst.left + normaldstend;
				} else {
					srcrc.right = src.right - normalsrcstart;
					srcrc.left = src.right - normalsrcend;
					dstrc.right = dst.right - normaldststart;
					dstrc.left = dst.right - normaldstend;
				}
				drawDimmedBitmap(canvas, bmp, srcrc, dstrc);
			}
			if (distortdststart < distortdstend) {
				int n = distortdx / 5 + 1;
				int dst0 = SIN_TABLE[distortanglestart * SIN_TABLE_SIZE / PI_DIV_2] * maxdistortdx / SIN_TABLE_SCALE;
				int src0 = distortanglestart * maxdistortdx / SIN_TABLE_SCALE;
				for (int i=0; i<n; i++) {
					int angledelta = distortangleend - distortanglestart;
					int startangle = distortanglestart + i * angledelta / n;
					int endangle = distortanglestart + (i+1) * angledelta / n;
					int src1 = startangle * maxdistortdx / SIN_TABLE_SCALE - src0;
					int src2 = endangle * maxdistortdx / SIN_TABLE_SCALE - src0;
					int dst1 = SIN_TABLE[startangle * SIN_TABLE_SIZE / PI_DIV_2] * maxdistortdx / SIN_TABLE_SCALE - dst0;
					int dst2 = SIN_TABLE[endangle * SIN_TABLE_SIZE / PI_DIV_2] * maxdistortdx / SIN_TABLE_SCALE - dst0;
					int hiliteIndex = startangle * hilitePaints.length / PI_DIV_2;
					Paint[] paints;
					if (dir > 0) {
						dstrc.left = dst.left + distortdststart + dst1;
						dstrc.right = dst.left + distortdststart + dst2;
						srcrc.left = src.left + distortsrcstart + src1;
						srcrc.right = src.left + distortsrcstart + src2;
						paints = hilitePaints;
					} else {
						dstrc.right = dst.right - distortdststart - dst1;
						dstrc.left = dst.right - distortdststart - dst2;
						srcrc.right = src.right - distortsrcstart - src1;
						srcrc.left = src.right - distortsrcstart - src2;
						paints = shadePaints;
					}
					drawDimmedBitmap(canvas, bmp, srcrc, dstrc);
					canvas.drawRect(dstrc, paints[hiliteIndex]);
				}
			}
		}

		@Override
		public void move(int duration, boolean accelerated) {
			if (duration > 0 && getPageFlipAnimationSpeedMs() != 0) {
				int steps = (int)(duration / getAvgAnimationDrawDuration()) + 2;
				int x0 = currShift;
				int x1 = destShift;
				if ((x0 - x1) < 10 && (x0 - x1) > -10)
					steps = 2;
				for (int i = 1; i < steps; i++) {
					int x = x0 + (x1 - x0) * i / steps;
					currShift = accelerated ? accelerate( x0, x1, x ) : x;
					draw();
				}
			}
			currShift = destShift;
			draw();
		}

		@Override
		public void stop(int x, int y) {
			if (currentAnimation == null)
				return;
			alog.v("PageViewAnimation.stop(" + x + ", " + y + ")");
			//if ( started ) {
			boolean moved = false;
			if (x != -1) {
				int threshold = mActivity.getPalmTipPixels() * 7/8;
				if (direction > 0) {
					// |  <=====  |
					int dx = startX - x;
					if (dx > threshold)
						moved = true;
				} else {
					// |  =====>  |
					int dx = x - startX;
					if (dx > threshold)
						moved = true;
				}
				int duration;
				if (moved) {
					destShift = maxX;
					duration = 300; // 500 ms forward
				} else {
					destShift = 0;
					duration = 200; // 200 ms cancel
				}
				move( duration, false );
			} else {
				moved = true;
			}
			doc.doCommand(ReaderCommand.DCMD_GO_PAGE_DONT_SAVE_HISTORY.nativeId, moved ? page2 : page1);
			//}
			scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
			close();
			// preparing images for next page flip
			preparePageImage(0);
			preparePageImage(direction);
			updateCurrentPositionStatus();
			//if ( started )
			//	drawPage();
		}

		@Override
		public void update(int x, int y) {
			alog.v("PageViewAnimation.update(" + x + ", " + y + ")");
			int delta = direction>0 ? startX - x : x - startX;
			if (delta <= 0)
				destShift = 0;
			else if (delta < maxX)
				destShift = delta;
			else
				destShift = maxX;
		}

		public void animate()
		{
			alog.v("PageViewAnimation.animate("+currShift + " => " + destShift + ") speed=" + getPageFlipAnimationSpeedMs());
			//log.d("animate() is called");
			if (currShift != destShift) {
				started = true;
				if (getPageFlipAnimationSpeedMs() == 0)
					currShift = destShift;
				else {
					int delta = currShift - destShift;
					if (delta < 0)
						delta = -delta;
					long avgDraw = getAvgAnimationDrawDuration();
					int maxStep = getPageFlipAnimationSpeedMs() > 0 ? (int)(maxX * 1000 / avgDraw / getPageFlipAnimationSpeedMs()) : maxX;
					int step;
					if (delta > maxStep * 2)
						step = maxStep;
					else
						step = (delta + 3) / 4;
					//int step = delta<3 ? 1 : (delta<5 ? 2 : (delta<10 ? 3 : (delta<15 ? 6 : (delta<25 ? 10 : (delta<50 ? 15 : 30))))); 
					if (currShift < destShift)
						currShift+=step;
					else if (currShift > destShift)
						currShift-=step;
					alog.v("PageViewAnimation.animate("+currShift + " => " + destShift + "  step=" + step + ")");
				}
				//pointerCurrPos = pointerDestPos;
				draw();
				if (currShift != destShift)
					scheduleAnimation();
			}
		}

		public void draw(Canvas canvas)
		{
			alog.v("PageViewAnimation.draw("+currShift + ")");
//			BitmapInfo image1 = mCurrentPageInfo;
//			BitmapInfo image2 = mNextPageInfo;
			if (image1.isReleased() || image2.isReleased())
				return;
			int w = image1.bitmap.getWidth();
			int h = image1.bitmap.getHeight();
			int div;
			if (direction > 0) {
				// FORWARD
				div = w-currShift;
				Rect shadowRect = new Rect(div, 0, div+w/10, h);
				if (pageFlipAnimationM ==  PAGE_ANIMATION_PAPER) {
					if (this.pageCount == 2) {
						int w2 = w/2;
						if (div < w2) {
							// left - part of old page
							Rect src1 = new Rect(0, 0, div, h);
							Rect dst1 = new Rect(0, 0, div, h);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
							// left, resized part of new page
							Rect src2 = new Rect(0, 0, w2, h);
							Rect dst2 = new Rect(div, 0, w2, h);
							//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
							drawDistorted(canvas, image2.bitmap, src2, dst2, -1);
							// right, new page
							Rect src3 = new Rect(w2, 0, w, h);
							Rect dst3 = new Rect(w2, 0, w, h);
							drawDimmedBitmap(canvas, image2.bitmap, src3, dst3);

						} else {
							// left - old page
							Rect src1 = new Rect(0, 0, w2, h);
							Rect dst1 = new Rect(0, 0, w2, h);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
							// right, resized old page
							Rect src2 = new Rect(w2, 0, w, h);
							Rect dst2 = new Rect(w2, 0, div, h);
							//canvas.drawBitmap(image1.bitmap, src2, dst2, null);
							drawDistorted(canvas, image1.bitmap, src2, dst2, 1);
							// right, new page
							Rect src3 = new Rect(div, 0, w, h);
							Rect dst3 = new Rect(div, 0, w, h);
							drawDimmedBitmap(canvas, image2.bitmap, src3, dst3);

							if (div > 0 && div < w)
								drawShadow(canvas, shadowRect);
						}
					} else {
						Rect src1 = new Rect(0, 0, w, h);
						Rect dst1 = new Rect(0, 0, w-currShift, h);
						//log.v("drawing " + image1);
						//canvas.drawBitmap(image1.bitmap, src1, dst1, null);
						drawDistorted(canvas, image1.bitmap, src1, dst1, 1);
						Rect src2 = new Rect(w-currShift, 0, w, h);
						Rect dst2 = new Rect(w-currShift, 0, w, h);
						//log.v("drawing " + image1);
						drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);

						if (div > 0 && div < w)
							drawShadow(canvas, shadowRect);
					}
				} else {
					if (pageFlipAnimationM == PAGE_ANIMATION_BLUR) {
						int defRadius = 20;
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int radius = (diff * defRadius) / w2;
						if (div < w2) {
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							Bitmap blurredBmp = null;
							if (defRadius - radius > 0)
								if (image2scaled != null)
									blurredBmp = FastBlur.doBlur(image2scaled, defRadius - radius, false);
							drawDimmedBitmap(canvas, blurredBmp == null ? image2.bitmap : blurredBmp, null, dst1);
						} else {
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							Bitmap blurredBmp = null;
							if (defRadius - radius > 0)
								if (image1scaled != null)
									blurredBmp = FastBlur.doBlur(image1scaled, defRadius - radius, false);
							drawDimmedBitmap(canvas, blurredBmp == null ? image1.bitmap : blurredBmp, null, dst2);
						}
					}
					else if (pageFlipAnimationM == PAGE_ANIMATION_BLUR_DIM) {
						int defDim = dimmingAlpha;
						int defRadius = 20;
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int dim = (diff * defDim) / w2;
						int radius = (diff * defRadius) / w2;
						if (div < w2) {
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							Bitmap blurredBmp = null;
							if (defRadius - radius > 0)
								if (image2scaled != null)
									blurredBmp = FastBlur.doBlur(image2scaled, defRadius - radius, false);
							drawDimmedBitmapAlpha(canvas, blurredBmp == null ? image2.bitmap : blurredBmp, null, dst1, dim);
						} else {
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							Bitmap blurredBmp = null;
							if (defRadius - radius > 0)
								if (image1scaled != null)
									blurredBmp = FastBlur.doBlur(image1scaled, defRadius - radius, false);
							drawDimmedBitmapAlpha(canvas, blurredBmp == null ? image1.bitmap : blurredBmp, null, dst2, dim);
						}
					}
					else if (pageFlipAnimationM == PAGE_ANIMATION_DIM) {
						int defDim = dimmingAlpha;
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int dim = (diff * defDim) / w2;
						if (div < w2) {
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							drawDimmedBitmapAlpha(canvas, image2.bitmap, null, dst1, dim);
						} else {
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							drawDimmedBitmapAlpha(canvas, image1.bitmap, null, dst2,  dim);
						}
					}
					else if (pageFlipAnimationM == PAGE_ANIMATION_MAG) {
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int defMaxW = w/4;
						int defMaxH = h/4;
						int curW = defMaxW - (diff * defMaxW) / w2;
						int curH = defMaxH - (diff * defMaxH) / w2;
						if (div < w2) {
							Rect src1 = new Rect(curW, curH, w - curW,  h - curH);
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
						} else {
							Rect src2 = new Rect(curW, curH, w - curW,  h - curH);
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							drawDimmedBitmap(canvas, image1.bitmap, src2, dst2);
						}
					}
					else if (pageFlipAnimationM == PAGE_ANIMATION_MAG_DIM) {
						int defDim = dimmingAlpha;
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int dim = (diff * defDim) / w2;
						int defMaxW = w/4;
						int defMaxH = h/4;
						int curW = defMaxW - (diff * defMaxW) / w2;
						int curH = defMaxH - (diff * defMaxH) / w2;
						if (div < w2) {
							Rect src1 = new Rect(curW, curH, w - curW,  h - curH);
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							drawDimmedBitmapAlpha(canvas, image2.bitmap, src1, dst1, dim);
						} else {
							Rect src2 = new Rect(curW, curH, w - curW,  h - curH);
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							drawDimmedBitmapAlpha(canvas, image1.bitmap, src2, dst2,  dim);
						}
					}
					else {
						if (pageFlipAnimationM == PAGE_ANIMATION_SLIDE2) {
							Rect src1 = new Rect(currShift, 0, w, h);
							Rect dst1 = new Rect(0, 0, w-currShift, h);
							//log.v("drawing " + image1);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
							Rect src2 = new Rect(0, 0, currShift, h);
							Rect dst2 = new Rect(w-currShift, 0, w, h);
							//log.v("drawing " + image2);
							drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
						} else {
							Rect src1 = new Rect(currShift, 0, w, h);
							Rect dst1 = new Rect(0, 0, w - currShift, h);
							//log.v("drawing " + image1);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
							Rect src2 = new Rect(w - currShift, 0, w, h);
							Rect dst2 = new Rect(w - currShift, 0, w, h);
							//log.v("drawing " + image2);
							drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
						}
					}
				}
			} else {
				// BACK
				div = currShift;
				Rect shadowRect = new Rect(div, 0, div+10, h);
				if (pageFlipAnimationM ==  PAGE_ANIMATION_PAPER) {
					if (this.pageCount == 2) {
						int w2 = w/2;
						if (div < w2) {
							// left - part of old page
							Rect src1 = new Rect(0, 0, div, h);
							Rect dst1 = new Rect(0, 0, div, h);
							drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
							// left, resized part of new page
							Rect src2 = new Rect(0, 0, w2, h);
							Rect dst2 = new Rect(div, 0, w2, h);
							//canvas.drawBitmap(image1.bitmap, src2, dst2, null);
							drawDistorted(canvas, image1.bitmap, src2, dst2, -1);
							// right, new page
							Rect src3 = new Rect(w2, 0, w, h);
							Rect dst3 = new Rect(w2, 0, w, h);
							drawDimmedBitmap(canvas, image1.bitmap, src3, dst3);
						} else {
							// left - old page
							Rect src1 = new Rect(0, 0, w2, h);
							Rect dst1 = new Rect(0, 0, w2, h);
							drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
							// right, resized old page
							Rect src2 = new Rect(w2, 0, w, h);
							Rect dst2 = new Rect(w2, 0, div, h);
							//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
							drawDistorted(canvas, image2.bitmap, src2, dst2, 1);
							// right, new page
							Rect src3 = new Rect(div, 0, w, h);
							Rect dst3 = new Rect(div, 0, w, h);
							drawDimmedBitmap(canvas, image1.bitmap, src3, dst3);

							if (div > 0 && div < w)
								drawShadow(canvas, shadowRect);
						}
					} else {
						Rect src1 = new Rect(currShift, 0, w, h);
						Rect dst1 = new Rect(currShift, 0, w, h);
						drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(0, 0, w, h);
						Rect dst2 = new Rect(0, 0, currShift, h);
						//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
						drawDistorted(canvas, image2.bitmap, src2, dst2, 1);

						if (div > 0 && div < w)
							drawShadow(canvas, shadowRect);
					}
				} else {
					if (pageFlipAnimationM ==  PAGE_ANIMATION_BLUR) {
						int defRadius = 20;
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int radius = (diff * defRadius) / w2;
						if (div < w2) {
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							Bitmap blurredBmp = null;
							if (defRadius - radius > 0)
								if (image1scaled != null)
									blurredBmp = FastBlur.doBlur(image1scaled, defRadius - radius, false);
							drawDimmedBitmap(canvas, blurredBmp == null ? image1.bitmap : blurredBmp, null, dst1);
						} else {
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							Bitmap blurredBmp = null;
							if (defRadius - radius > 0)
								if (image2scaled != null)
									blurredBmp = FastBlur.doBlur(image2scaled, defRadius - radius, false);
							drawDimmedBitmap(canvas, blurredBmp == null ? image2.bitmap : blurredBmp, null, dst2);
						}
					}
					else if (pageFlipAnimationM == PAGE_ANIMATION_BLUR_DIM) {
						int defDim = dimmingAlpha;
						int defRadius = 20;
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int dim = (diff * defDim) / w2;
						int radius = (diff * defRadius) / w2;
						if (div < w2) {
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							Bitmap blurredBmp = null;
							if (defRadius - radius > 0)
								if (image2scaled != null)
									blurredBmp = FastBlur.doBlur(image1scaled, defRadius - radius, false);
							drawDimmedBitmapAlpha(canvas, blurredBmp == null ? image1.bitmap : blurredBmp, null, dst1, dim);
						} else {
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							Bitmap blurredBmp = null;
							if (defRadius - radius > 0)
								if (image1scaled != null)
									blurredBmp = FastBlur.doBlur(image2scaled, defRadius - radius, false);
							drawDimmedBitmapAlpha(canvas, blurredBmp == null ? image2.bitmap : blurredBmp, null, dst2, dim);
						}
					}
					else if (pageFlipAnimationM == PAGE_ANIMATION_DIM) {
						int defDim = dimmingAlpha;
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int dim = (diff * defDim) / w2;
						if (div < w2) {
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							drawDimmedBitmapAlpha(canvas, image1.bitmap, null, dst1, dim);
						} else {
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							drawDimmedBitmapAlpha(canvas, image2.bitmap, null, dst2,  dim);
						}

					} else if (pageFlipAnimationM == PAGE_ANIMATION_MAG) {
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int defMaxW = w/4;
						int defMaxH = h/4;
						int curW = defMaxW - (diff * defMaxW) / w2;
						int curH = defMaxH - (diff * defMaxH) / w2;
						if (div < w2) {
							Rect src1 = new Rect(curW, curH, w - curW,  h - curH);
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						} else {
							Rect src2 = new Rect(curW, curH, w - curW,  h - curH);
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
						}
					}  else if (pageFlipAnimationM == PAGE_ANIMATION_MAG_DIM) {
						int defDim = dimmingAlpha;
						int w2 = w / 2;
						int diff = Math.abs(div - w2);
						int dim = (diff * defDim) / w2;
						int defMaxW = w/4;
						int defMaxH = h/4;
						int curW = defMaxW - (diff * defMaxW) / w2;
						int curH = defMaxH - (diff * defMaxH) / w2;
						if (div < w2) {
							Rect src1 = new Rect(curW, curH, w - curW,  h - curH);
							Rect dst1 = new Rect(0, 0, w, h);
							//log.v("drawing " + image1);
							drawDimmedBitmapAlpha(canvas, image1.bitmap, src1, dst1, dim);
						} else {
							Rect src2 = new Rect(curW, curH, w - curW,  h - curH);
							Rect dst2 = new Rect(0, 0, w, h);
							//log.v("drawing " + image2);
							drawDimmedBitmapAlpha(canvas, image2.bitmap, src2, dst2, dim);
						}
					} else {
						if (pageFlipAnimationM ==  PAGE_ANIMATION_SLIDE2) {
							Rect src1 = new Rect(0, 0, w - currShift, h);
							Rect dst1 = new Rect(currShift, 0, w, h);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
							Rect src2 = new Rect(w - currShift, 0, w, h);
							Rect dst2 = new Rect(0, 0, currShift, h);
							drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
						} else {
							Rect src1 = new Rect(currShift, 0, w, h);
							Rect dst1 = new Rect(currShift, 0, w, h);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
							Rect src2 = new Rect(w - currShift, 0, w, h);
							Rect dst2 = new Rect(0, 0, currShift, h);
							drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
						}
					}
				}
			}
			if (div > 0 && div < w) {
				canvas.drawLine(div, 0, div, h, divPaint);
			}
		}
	}

	private static final class RingBuffer {
		private long [] mArray;
		private long mSum;
		private long mAvg;
		private int mPos;
		private int mCount;
		private int mSize;

		public RingBuffer(int size, long initialAvg) {
			mSize = size;
			mArray = new long[size];
			mPos = 0;
			mCount = 0;
			mAvg = initialAvg;
			mSum = 0;
		}

		public long average() {
			return mAvg;
		}

		public void add(long val) {
			if (mCount < mSize)
				mCount++;
			else							// array is full
				mSum -= mArray[mPos];		// subtract from sum the value to replace
			mArray[mPos] = val;				// write new value
			mSum += val;					// update sum
			mAvg = mSum /mCount;			// calculate average value
			mPos++;
			if (mPos >= mSize)
				mPos = 0;
		}
	}

	RingBuffer mAvgDrawAnimationStats = new RingBuffer(16, 50);

	private long getAvgAnimationDrawDuration() {
		return mAvgDrawAnimationStats.average();
	}

	private void updateAnimationDurationStats(long duration) {
		if (duration <= 0)
			duration = 1;
		else if (duration > 1000)
			return;
		mAvgDrawAnimationStats.add(duration);
	}

	private boolean checkNeedRedraw() {
		return checkNeedRedraw(internalDX, internalDY);
	}

	public boolean bNeedRedrawOnce = false;

	private boolean checkNeedRedraw(int x, int y) {
//		if (bNeedRedrawOnce) {
//			bNeedRedrawOnce = false;
//			return true;
//		}
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
		for (CoolReader.ResizeHistory rh: getActivity().getResizeHist()) {
			if (
					(rh.X == x) && (rh.Y == y)
					) {
				bFound = true;
			}
		}
		if (!bFound) {
			CoolReader.ResizeHistory rh = getActivity().getNewResizeHistory();
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

	private void drawPage()
	{
		drawPage(null, false);
	}
	private void drawPage(boolean isPartially)
	{
		drawPage(null, isPartially);
	}
	private void drawPage( Runnable doneHandler, boolean isPartially )
	{
		if (!mInitialized)
			return;
		log.v("drawPage() : submitting DrawPageTask");
		// evaluate if we need to redraw page on this resolution
		if (mOpened)
			scheduleSaveCurrentPositionBookmark(getDefSavePositionInterval());
		post( new DrawPageTask(doneHandler, isPartially) );
	}

	private int internalDX = 0;
	private int internalDY = 0;

	private byte[] coverPageBytes = null;
	private void findCoverPage()
	{
		log.d("document is loaded succesfull, checking coverpage data");
		byte[] coverpageBytes = doc.getCoverPageData();
		if (coverpageBytes != null) {
			log.d("Found cover page data: " + coverpageBytes.length + " bytes");
			coverPageBytes = coverpageBytes;
		}
	}

	private int currentProgressPosition = 1;
	private int currentProgressTitleId = R.string.progress_loading;
	private String currentProgressTitle = null;

	private void showProgress(int position, int titleResource) {
		int pos = position / 100;
		showCenterPopup(pos + "% " + mActivity.getString(titleResource), -1);
		boolean first = currentProgressTitleId == 0;
		if (currentProgressPosition != position || currentProgressTitleId != titleResource) {
			currentProgressPosition = position;
			currentProgressTitleId = titleResource;
			currentProgressTitle = mActivity.getString(currentProgressTitleId);
			//if (first) bookView.draw(!first);
		}
	}

//		private void showProgress(int position, int titleResource) {
//			log.v("showProgress(" + position + ")");
//			boolean first = currentProgressTitleId == 0;
//			boolean update = false;
//			if (null == currentProgressTitle || currentProgressTitleId != titleResource) {
//				currentProgressTitleId = titleResource;
//				currentProgressTitle = mActivity.getString(currentProgressTitleId);
//				update = true;
//			}
//			if (currentProgressPosition != position || currentProgressTitleId != titleResource) {
//				currentProgressPosition = position;
//				update = true;
//			}
//			if (update)
//				bookView.draw(!first);
//		}

	private void hideProgress() {
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
//			bookView.draw(false);
//		}
//	}

	private boolean isProgressActive() {
		return currentProgressPosition > 0;
	}

	private void checkOpenBookStyles(boolean force) {
		boolean bDontAsk = mActivity.settings().getBool(Settings.PROP_APP_HIDE_CSS_WARNING, false);
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
						SomeButtonsToolbarDlg.showDialog(mActivity, ReaderView.this, 10, true,
								mActivity.getString(R.string.opened_doc_props),
								sButtons, null, new SomeButtonsToolbarDlg.ButtonPressedCallback() {
									@Override
									public void done(Object o, String btnPressed) {
										if (btnPressed.equals(mActivity.getString(R.string.str_change))) {
											mActivity.optionsFilter = "";
											mActivity.showOptionsDialogTab(OptionsDialog.Mode.READER, 1);
										}
									}
								});
					}
				}
			}, 200);
	}

	private class LoadDocumentTask extends Task
	{
		String filename;
		String path;
		InputStream inputStream;
		Runnable doneHandler;
		Runnable errorHandler;
		String pos;
		int profileNumber;
		boolean disableInternalStyles;
		boolean disableTextAutoformat;
		LoadDocumentTask(BookInfo bookInfo, InputStream inputStream, Runnable doneHandler, Runnable errorHandler) {
			BackgroundThread.ensureGUI();
			mBookInfo = bookInfo;
			FileInfo fileInfo = bookInfo.getFileInfo();
			log.v("LoadDocumentTask for " + fileInfo);
			if (fileInfo.getTitle() == null && inputStream == null) {
				// As a book 'should' have a title, no title means we should
				// retrieve the book metadata from the engine to get the
				// book language.
				// Is it OK to do this here???  Should we use isScanned?
				// Should we use another fileInfo flag or a new flag?
				mEngine.scanBookProperties(fileInfo);
			}
			String language = fileInfo.getLanguage();
			log.v("update hyphenation language: " + language + " for " + fileInfo.getTitle());
			this.filename = fileInfo.getPathName();
			this.path = fileInfo.arcname != null ? fileInfo.arcname : fileInfo.pathname;
			this.inputStream = inputStream;
			this.doneHandler = doneHandler;
			this.errorHandler = errorHandler;
			//FileInfo fileInfo = new FileInfo(filename);
			disableInternalStyles = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
			if (mBookInfo.getFileInfo().flags == 0) {
				boolean embed = mSettings.getBool(PROP_EMBEDDED_STYLES_DEF, false);
				disableInternalStyles = !embed;
				mBookInfo.getFileInfo().setFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG, disableInternalStyles);
			}
			disableTextAutoformat = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
			profileNumber = mBookInfo.getFileInfo().getProfileId();
			//Properties oldSettings = new Properties(mSettings);
			// TODO: enable storing of profile per book
			int curProf = mActivity.getCurrentProfile();
			if (curProf != profileNumber) {
				showCenterPopup(mActivity.getString(R.string.settings_profile) + ":" +profileNumber, -1);
			}
			mActivity.setCurrentProfile(profileNumber);
			if (profileNumber == 0) { // if there is no book profile, then set it to current
				if (mActivity.getCurrentProfile() != 0)
					if (mBookInfo != null && mBookInfo.getFileInfo() != null) {
						mBookInfo.getFileInfo().setProfileId(mActivity.getCurrentProfile());
						mActivity.getDB().saveBookInfo(mBookInfo);
					}
			}
			log.v("BookProfileNumber : "+ profileNumber);
			if (mBookInfo!=null && mBookInfo.getLastPosition()!=null)
				pos = mBookInfo.getLastPosition().getStartPos();
			log.v("LoadDocumentTask : book info " + mBookInfo);
			log.v("LoadDocumentTask : last position = " + pos);
			if (mBookInfo != null && mBookInfo.getLastPosition() != null)
				setTimeElapsed(mBookInfo.getLastPosition().getTimeElapsed());
			//mBitmap = null;
			//showProgress(1000, R.string.progress_loading);
			//draw();
			BackgroundThread.instance().postGUI(() -> bookView.draw(false));
			//init();
			// close existing document
			log.v("LoadDocumentTask : closing current book");
	        close();
			final Properties currSettings = new Properties(mSettings);
			//setAppSettings(props, oldSettings);
			BackgroundThread.instance().postBackground(() -> {
				log.v("LoadDocumentTask : switching current profile");
				applySettings(currSettings); //enforce settings reload
				log.i("Switching done");
			});
		}

		@Override
		public void work() throws IOException {
			BackgroundThread.ensureBackground();
			coverPageBytes = null;
			log.i("Loading document " + filename);
			doc.doCommand(ReaderCommand.DCMD_SET_INTERNAL_STYLES.nativeId, disableInternalStyles ? 0 : 1);
			doc.doCommand(ReaderCommand.DCMD_SET_TEXT_FORMAT.nativeId, disableTextAutoformat ? 0 : 1);
			doc.doCommand(ReaderCommand.DCMD_SET_REQUESTED_DOM_VERSION.nativeId, mBookInfo.getFileInfo().domVersion);
			if (0 == mBookInfo.getFileInfo().domVersion) {
				doc.doCommand(ReaderCommand.DCMD_SET_RENDER_BLOCK_RENDERING_FLAGS.nativeId, 0);
			} else {
				doc.doCommand(ReaderCommand.DCMD_SET_RENDER_BLOCK_RENDERING_FLAGS.nativeId, mBookInfo.getFileInfo().blockRenderingFlags);
			}
			boolean success;
			if (null != inputStream)
				success = doc.loadDocumentFromStream(inputStream, filename);
			else
				success = doc.loadDocument(filename);
			if (success) {
				log.v("loadDocumentInternal completed successfully");

				doc.requestRender();

				findCoverPage();
				log.v("requesting page image, to render");
				if (internalDX == 0 || internalDY == 0) {
					internalDX = surface.getWidth();
					internalDY = surface.getHeight();
					log.d("LoadDocument task: no size defined, resizing using widget size");
					doc.resize(internalDX, internalDY);
				}
				preparePageImage(0);
				log.v("updating loaded book info");
				updateLoadedBookInfo();
				log.i("Document " + filename + " is loaded successfully");
				if (pos == null) {
					Bookmark bmk = getActivity().readCurPosFile(false);
					if (bmk!=null) {
						boolean bSameBook=true;
						if (!bmk.bookFile.equals(mBookInfo.getFileInfo().getFilename())) bSameBook=false;
						if (!bmk.bookPath.equals(mBookInfo.getFileInfo().pathname)) bSameBook=false;
						if (!StrUtils.isEmptyStr(bmk.bookFileArc))
							if (!bmk.bookFileArc.equals(mBookInfo.getFileInfo().arcname))
								bSameBook=false;
						if (bSameBook) {
							pos=bmk.getStartPos();
							getActivity().showToast(mActivity.getString(R.string.pos_recovered));
						}
					}
				}
				if (pos != null) {
					log.i("Restoring position : " + pos);
					restorePositionBackground(pos);
				} else {
					checkOpenBookStyles(false);
				}
				CoolReader.dumpHeapAllocation();
			} else {
				log.e("Error occurred while trying to load document " + filename);
				throw new IOException("Cannot read document");
			}
		}

		@Override
		public void done()
		{
			BackgroundThread.ensureGUI();
			log.d("LoadDocumentTask, GUI thread is finished successfully");
			if (Services.getHistory() != null) {
				Services.getHistory().updateBookAccess(mBookInfo, getTimeElapsed());
				mActivity.waitForCRDBService(() -> mActivity.getDB().saveBookInfo(mBookInfo));
				if (coverPageBytes != null && mBookInfo != null && mBookInfo.getFileInfo() != null) {
					// TODO: fix it
					/*
					DocumentFormat format = mBookInfo.getFileInfo().format;
					if (null != format) {
						if (format.needCoverPageCaching()) {
//		        			if (mActivity.getBrowser() != null)
//		        				mActivity.getBrowser().setCoverpageData(new FileInfo(mBookInfo.getFileInfo()), coverPageBytes);
						}
					}
					*/
					if (DeviceInfo.EINK_NOOK)
						updateNookTouchCoverpage(mBookInfo.getFileInfo().getPathName(), coverPageBytes);
					//mEngine.setProgressDrawable(coverPageDrawable);
				}
				if (DeviceInfo.EINK_SONY) {
					SonyBookSelector selector = new SonyBookSelector(mActivity);
					long l = selector.getContentId(path);
					if(l != 0) {
						selector.setReadingTime(l);
						selector.requestBookSelection(l);
					}
				}
				mOpened = true;

				highlightBookmarks();

				hideProgress();

				selectionModeActive = false;
				selectionModeWasActive = false;
				inspectorModeActive = false;

				drawPage(); //plotn - possibly it is unnesessary - due to new progress. But maybe not - page was empty last time

				BackgroundThread.instance().postGUI(() -> {
					mActivity.showReader();
					if (null != doneHandler)
						doneHandler.run();
					final String booknameF = getBookInfo().getFileInfo().getFilename();
					BackgroundThread.instance().postGUI(() -> {
						String bookname = getBookInfo().getFileInfo().getFilename();
						if (bookname.equals(booknameF)) {
							log.i("Load last rpos from CLOUD");
							int iSyncVariant3 = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
							if (iSyncVariant3 != 0) {
								if (mActivity.mCurrentFrame == mActivity.getmReaderFrame())
									CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
										CloudSync.CLOUD_SAVE_READING_POS, true, iSyncVariant3 == 1, true, true);
							}
						}
					}, 5000);
				});

				// Save last opened book ONLY if book opened from real file not stream.
				if (null == inputStream)
					mActivity.setLastBook(filename);
				UserDicDlg.updDicSearchHistoryAll(mActivity);
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
		public void fail( Exception e )
		{
			BackgroundThread.ensureGUI();
			close();
			log.v("LoadDocumentTask failed for " + mBookInfo, e);
			mActivity.waitForCRDBService(() -> {
				if (Services.getHistory() != null)
					Services.getHistory().removeBookInfo(mActivity.getDB(), mBookInfo.getFileInfo(), true, false);
			});
			mBookInfo = null;
			log.d("LoadDocumentTask is finished with exception " + e.getMessage());
	        mOpened = false;
	        BackgroundThread.instance().executeBackground(() -> {
				doc.createDefaultDocument(mActivity.getString(R.string.error), mActivity.getString(R.string.error_while_opening, filename));
				doc.requestRender();
				preparePageImage(0);
				drawPage();
			});
			hideProgress();
			mActivity.showToast("Error while loading document");
			if (errorHandler != null) {
				log.e("LoadDocumentTask: Calling error handler");
				errorHandler.run();
			}
		}
	}

	private final static boolean dontStretchWhileDrawing = true;
	private final static boolean centerPageInsteadOfResizing = true;

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

	private void drawDimmedBitmap( Canvas canvas, Bitmap bmp, Rect src, Rect dst ) {
		canvas.drawBitmap(bmp, src, dst, null);
		dimRect( canvas, dst );
	}

	private void drawDimmedBitmapAlpha( Canvas canvas, Bitmap bmp, Rect src, Rect dst, int alpha ) {
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
				mActivity.getWindow().setNavigationBarColor(backgrNormalizedColor);
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
				if (mActivity instanceof CoolReader) {
					int optionAppearance = Integer.valueOf(((CoolReader)mActivity).getToolbarAppearance());
					switch (optionAppearance) {
						case Settings.VIEWER_TOOLBAR_100_gray:      // 1
							addDarken = true;
							break;
						case Settings.VIEWER_TOOLBAR_75_gray:       // 3
							addDarken = true;
							break;
						case Settings.VIEWER_TOOLBAR_50_gray:       // 5
							addDarken = true;
							break;
					}
				}
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
		doDrawProgressOld(canvas, position, title, false);
	}

	protected void doDrawProgressOld(Canvas canvas, int position, String title, boolean transparentFrame) {
		log.v("doDrawProgress(" + position + ")");
		if (null == title)
			return;
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		int mins = Math.min(w, h) * 7 / 10;
		int ph = mins / 20;
		int textColor = mSettings.getColor(PROP_FONT_COLOR, 0x000000);
		int fontSize = 12;			// 12pt
		float factor = mActivity.getDensityFactor();
		Rect rc = new Rect(w / 2 - mins / 2, h / 2 - ph / 2, w / 2 + mins / 2, h / 2 + ph / 2);
		if (transparentFrame) {
			int frameColor = mSettings.getColor(PROP_BACKGROUND_COLOR, 0xFFFFFF);
			float lumi = Utils.colorLuminance(frameColor);
			if (Utils.colorLuminance(frameColor) >= 0.5f)
				frameColor = Utils.darkerColor(frameColor, 150);
			else
				frameColor = Utils.lighterColor(frameColor, 200);
			Rect frameRc = new Rect(rc);
			frameRc.left -= ph/2;
			frameRc.right += ph/2;
			frameRc.top -= 2*fontSize*factor + ph/2;
			frameRc.bottom += ph/2;
			canvas.drawRect(frameRc, Utils.createSolidPaint(0xE0000000 | (frameColor & 0x00FFFFFF)));
		}

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
		textPaint.setTextSize(15f * factor);
		textPaint.setSubpixelText(true);
		canvas.drawText(title, (rc.left + rc.right) / 2, rc1.top - fontSize * factor, textPaint);
		//canvas.drawText(String.valueOf(position * 100 / 10000) + "%", rc.left + 4, rc1.bottom - 4, textPaint);
//		Rect rc2 = new Rect(rc);
//		rc.left = x;
//		canvas.drawRect(rc2, createSolidPaint(0xFFC0C0A0));
	}

	private int dimmingAlpha = 255; // no dimming
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

	private void restorePositionBackground(String pos) {
		BackgroundThread.ensureBackground();
		if (pos != null) {
			BackgroundThread.ensureBackground();
			doc.goToPosition(pos, false);
			preparePageImage(0);
			drawPage();
			updateCurrentPositionStatus();
			checkOpenBookStyles(false);
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

	public void scheduleSaveCurrentPositionBookmark(final int delayMillis) {
		// GUI thread required
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
						savePositionBookmark(bmk);
						Services.getHistory().updateBookAccess(bookInfo, getTimeElapsed());
					}
				} else {
					BackgroundThread.instance().postGUI(() -> {
						if (mylastSavePositionTaskId == lastSavePositionTaskId) {
							if (bookInfo != null) {
								log.v("saving last position");
								if (Services.getHistory() != null) {
									savePositionBookmark(bmk);
									Services.getHistory().updateBookAccess(bookInfo, getTimeElapsed());
								}
							}
						}
					}, delayMillis);
					boolean bNeedSave = !appPaused;
					if (lastSavedToGdBookmark!=null) {
						if ((bmk.getStartPos().equals(lastSavedToGdBookmark.getStartPos()))) {
							bNeedSave = false;
						}
					}
					if (bNeedSave) {
						final int mylastSavePositionCloudTaskId = ++lastSavePositionCloudTaskId;
						int autosaveInterval = (getSettings().getInt(ReaderView.PROP_SAVE_POS_TO_CLOUD_TIMEOUT, 0)) * 1000 * 60;
						if (autosaveInterval > 0)
							BackgroundThread.instance().postGUI((Runnable) () -> {
								if (mylastSavePositionCloudTaskId == lastSavePositionCloudTaskId) {
									if (bookInfo != null) {
										if (!appPaused) {
											mActivity.getmReaderFrame().getUserDicPanel().updateSavingMark("&");
											log.i("Save reading pos to CLOUD");
											lastSavedToGdBookmark = bmk;
											int iSyncVariant = mSettings.getInt(PROP_CLOUD_SYNC_VARIANT, 0);
											if (iSyncVariant > 0)
												CloudSync.saveJsonInfoFileOrCloud(mActivity,
														CloudSync.CLOUD_SAVE_READING_POS, true, iSyncVariant == 1, true);
										}
									}
								}
							}, autosaveInterval);
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

	public void savePositionBookmark(Bookmark bmk) {
		if (bmk != null && mBookInfo != null && isBookLoaded()) {
			//setBookPosition();
			if (lastSavedBookmark == null || !lastSavedBookmark.getStartPos().equals(bmk.getStartPos())) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Bookmark bmk2 = new Bookmark(bmk);
				bmk2.bookFile=mBookInfo.getFileInfo().getFilename();
				bmk2.bookPath=mBookInfo.getFileInfo().pathname;
				bmk2.bookFileArc="";
				if (mBookInfo.getFileInfo().isArchive)
				  bmk2.bookFileArc=mBookInfo.getFileInfo().arcname;
				final String prettyJson = gson.toJson(bmk2);
  			    getActivity().saveCurPosFile(false, prettyJson);
				Services.getHistory().updateRecentDir();
				mActivity.getDB().saveBookInfo(mBookInfo);
				mActivity.getDB().flush();
				lastSavedBookmark = bmk;
				mActivity.getmReaderFrame().getUserDicPanel().updateSavingMark("*");
			}
		}
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
		mActivity.einkRefresh();
		BackgroundThread.ensureGUI();
		if (isBookLoaded() && mBookInfo != null) {
			log.v("saving last immediately");
			log.d("bookmark count 1 = " + mBookInfo.getBookmarkCount());
			Services.getHistory().updateBookAccess(mBookInfo, getTimeElapsed());
			log.d("bookmark count 2 = " + mBookInfo.getBookmarkCount());
			mActivity.getDB().saveBookInfo(mBookInfo);
			log.d("bookmark count 3 = " + mBookInfo.getBookmarkCount());
			mActivity.getDB().flush();
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

	private String getCSSForFormat( DocumentFormat fileFormat )
	{
		if (fileFormat == null)
			fileFormat = DocumentFormat.FB2;
		File[] dataDirs = Engine.getDataDirectories(null, false, false);
		String defaultCss = mEngine.loadResourceUtf8(fileFormat.getCSSResourceId());
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
					return css;
				}
			}
		}
		return defaultCss;
	}

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
			arrAllPages = null;
		}

		public void OnLoadFileEnd() {
			log.d("readerCallback.OnLoadFileEnd");
			if (internalDX == 0 && internalDY == 0) {
				internalDX = requestedWidth;
				internalDY = requestedHeight;
				log.d("OnLoadFileEnd: resizeInternal(" + internalDX + "," + internalDY + ")");
				doc.resize(internalDX, internalDY);
			}
		}

		public void OnLoadFileError(String message) {
			log.d("readerCallback.OnLoadFileError(" + message + ")");
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

	private volatile SwapToCacheTask currentSwapTask;

	private void scheduleSwapTask() {
		currentSwapTask = new SwapToCacheTask();
		currentSwapTask.reschedule();
	}

	private void cancelSwapTask() {
		currentSwapTask = null;
	}

	private class SwapToCacheTask extends Task {
		boolean isTimeout;
		long startTime;
		public SwapToCacheTask() {
			startTime = System.currentTimeMillis();
		}
		public void reschedule() {
			if (this != currentSwapTask)
				return;
			BackgroundThread.instance().postGUI(() -> post(SwapToCacheTask.this), 2000);
		}
		@Override
		public void work() throws Exception {
			if (this != currentSwapTask)
				return;
			int res = doc.swapToCache();
			isTimeout = res==DocView.SWAP_TIMEOUT;
			long duration = System.currentTimeMillis() - startTime;
			if (!isTimeout) {
				log.i("swapToCacheInternal is finished with result " + res + " in " + duration + " ms");
			} else {
				log.d("swapToCacheInternal exited by TIMEOUT in " + duration + " ms: rescheduling");
			}
		}
		@Override
		public void done() {
			if (isTimeout)
				reschedule();
		}

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
					drawPage();
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
	private void updateNookTouchCoverpage(String bookFileName,
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

	public void showInputDialog(final String title, final String prompt, final boolean isNumberEdit, final int minValue, final int maxValue, final int lastValue, final InputHandler handler) {
		BackgroundThread.instance().executeGUI(() -> {
			final InputDialog dlg = new InputDialog(mActivity, title, prompt, isNumberEdit, minValue, maxValue, lastValue, handler);
			dlg.show();
		});
	}

	public void showFilterDialog() {
		showInputDialog(mActivity.getString(R.string.mi_filter_option), mActivity.getString(R.string.mi_filter_option),
				false,
				1, 2, 3,
				new InputDialog.InputHandler() {
					@Override
					public boolean validate(String s) {
						return true;
					}
					@Override
					public boolean validateNoCancel(String s) {
						return true;
					}
					@Override
					public void onOk(String s) {
						mActivity.optionsFilter = s;
						mActivity.showOptionsDialog(OptionsDialog.Mode.READER);
					}
					@Override
					public void onCancel() {
					}
				});
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
			String prompt = mActivity.getString(R.string.dlg_goto_input_page_number);
			showGoToPageDialog(mActivity.getString(R.string.mi_goto_page), pos + "\n" + prompt, true,
					1, props.pageCount, props.pageNumber,
					new GotoPageDialog.GotoPageHandler() {
						int pageNumber = 0;
						@Override
						public boolean validate(String s) {
							pageNumber = Integer.parseInt(s);
							return pageNumber>0 && pageNumber <= props.pageCount;
						}
						@Override
						public void onOk(String s) {
							goToPage(pageNumber);
						}
						@Override
						public void onOkPage(String s) {
							goToPage(pageNumber);
						}
						@Override
						public void onCancel() {
						}
					});
		});
	}


	public void showGoToPercentDialog() {
		getCurrentPositionProperties((props, positionText) -> {
			if (props == null)
				return;
			String pos = mActivity.getString(R.string.dlg_goto_current_position) + " " + positionText;
			String prompt = mActivity.getString(R.string.dlg_goto_input_percent);
			showGoToPageDialog(mActivity.getString(R.string.mi_goto_percent), pos + "\n" + prompt, true,
					0, 100, props.y * 100 / props.fullHeight,
					new GotoPageDialog.GotoPageHandler() {
						int percent = 0;
						@Override
						public boolean validate(String s) {
							percent = Integer.valueOf(s);
							//return percent>=0 && percent<=100;
							return percent>=0;
						}
						@Override
						public void onOk(String s) {
							if (percent>=0 && percent<=100)
							goToPercent(percent);
						}
						@Override
						public void onOkPage(String s) {
							goToPage(percent);
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

		if (keyCode == 0)
			keyCode = event.getScanCode();
		keyCode = translateKeyCode(keyCode);

		mActivity.onUserActivity();

		if (currentImageViewer != null)
			return currentImageViewer.onKeyDown(keyCode, event);

//		backKeyDownHere = false;
		if (event.getRepeatCount() == 0) {
			log.v("onKeyDown("+keyCode + ", " + event +")");
			keyDownTimestampMap.put(keyCode, System.currentTimeMillis());

			if (keyCode == KeyEvent.KEYCODE_BACK) {
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

		keyCode = overrideKey( keyCode );
		ReaderAction action = ReaderAction.findForKey( keyCode, mSettings );
		ReaderAction longAction = ReaderAction.findForLongKey( keyCode, mSettings );
		//ReaderAction dblAction = ReaderAction.findForDoubleKey( keyCode, mSettings );

		if (event.getRepeatCount() == 0) {
			if (keyCode == currentDoubleClickActionKeyCode && currentDoubleClickActionStart + DOUBLE_CLICK_INTERVAL > android.os.SystemClock.uptimeMillis()) {
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
					onAction(currentSingleClickAction);
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
				}, DOUBLE_CLICK_INTERVAL);
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
			currentTapHandler = new TapHandler();
		currentTapHandler.checkExpiration();
		return currentTapHandler.onTouchEvent(event);
	}

	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		stopTracking();
		if (currentAutoScrollAnimation != null)
			stopAutoScroll();
	}

	public void redraw() {
		//BackgroundThread.instance().executeBackground(new Runnable() {
		BackgroundThread.instance().executeGUI(() -> {
			surface.invalidate();
			invalidImages = true;
			//preparePageImage(0);
			bookView.draw();
		});
	}

	public ReaderView(CoolReader activity, Engine engine, Properties props)
	{
		//super(activity);
		log.i("Creating normal SurfaceView");
		surface = new ReaderSurface(activity);

		bookView = (BookView)surface;
		surface.setOnTouchListener(this);
		surface.setOnKeyListener(this);
		surface.setOnFocusChangeListener(this);
		doc = new DocView(Engine.lock, activity);
		doc.setReaderCallback(readerCallback);
		SurfaceHolder holder = surface.getHolder();
		holder.addCallback(this);

		BackgroundThread.ensureGUI();
        this.mActivity = activity;
        this.mEngine = engine;
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
		post(new CreateViewTask( props ));

	}

}
