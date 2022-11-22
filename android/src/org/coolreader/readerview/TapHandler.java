package org.coolreader.readerview;

import android.util.Log;
import android.view.MotionEvent;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.BookmarkEditDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.ImageInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;
import org.coolreader.crengine.ViewMode;
import org.coolreader.options.OptionsDialog;
import org.coolreader.options.PageMarginsOption;

import java.io.File;
import java.net.URL;

public class TapHandler {

	final ReaderView mReaderView;
	final CoolReader mActivity;
	final BookInfo mBookInfo;

	private static final String TAG = "ReaderViewTapHandler";

	public static final Logger log = L.create("rvth", Log.VERBOSE);

	public TapHandler(ReaderView rv) {
		mReaderView = rv;
		mActivity = rv.getActivity();
		mBookInfo = rv.mBookInfo;
	}

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
	private final static int STATE_TWO_POINTERS_DOWN = 13;  // Two fingers move down - next chapter (todo)
	private final static int STATE_TWO_POINTERS_UP = 14; // Two fingers move up - previous chapter (todo)
	private final static int STATE_THREE_POINTERS = 15; // three finger events

	public final static int PINCH_TRESHOLD_COUNT = 15;

	private final static int EXPIRATION_TIME_MS = 180000;

	int state = STATE_INITIAL;
	int brightness_side = ReaderView.BRIGHTNESS_TYPE_LEFT_SIDE;
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

	private boolean endEvent() {
		state = STATE_DONE;
		mReaderView.unhiliteTapZone();
		mReaderView.currentTapHandler = new TapHandler(mReaderView);
		return true;
	}

	public boolean isInitialState() {
		return state == STATE_INITIAL;
	}
	public void checkExpiration() {
		if (state != STATE_INITIAL && Utils.timeInterval(firstDown) > EXPIRATION_TIME_MS)
			cancel();
	}

	/// cancel current action and reset touch tracking state
	boolean cancel() {
		if (state == STATE_INITIAL)
			return true;
		switch (state) {
			case STATE_DOWN_1:
			case STATE_SELECTION:
				mReaderView.clearSelection();
				break;
			case STATE_FLIPPING:
				mReaderView.stopAnimation(-1, -1);
				break;
			case STATE_WAIT_FOR_DOUBLE_CLICK:
			case STATE_DONE:
			case STATE_BRIGHTNESS:
			case STATE_FLIP_TRACKING:
				//mReaderView.stopBrightnessControl(-1, -1, leftSideBrightness);
				mReaderView.stopBrightnessControl(-1, -1, brightness_side);
				break;
		}
		state = STATE_DONE;
		mReaderView.unhiliteTapZone();
		mReaderView.currentTapHandler = new TapHandler(mReaderView);
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
		if (!mReaderView.mOpened)
			return;
		//plotn - some strange behavior with mIsPageMode
		//final int swipeDistance = mIsPageMode ? x - start_x : y - start_y;
		//final int swipeDistance = mReaderView.viewMode == ViewMode.PAGES ? x - start_x : y - start_y;
		final int swipeDistance = x - start_x;
		//final int swipeDistance = x - start_x;
		final int distanceForFlip = - ((mReaderView.surface.getWidth() -
				(mActivity.getPalmTipPixelsK(mReaderView.mGesturePageFlipSensivity) / 2)) / mReaderView.mGesturePageFlipPageCount);
		int pagesToFlip = swipeDistance / distanceForFlip;
		if (pagesToFlip == 0) {
			return; // Nothing to do
		}
		adjustStartValuesOnDrag(swipeDistance, distanceForFlip);
		ReaderAction action = pagesToFlip > 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP;
		while (pagesToFlip != 0) {
			mReaderView.onAction(action);
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

		mReaderView.currentTapHandler = new TapHandler(mReaderView);

		if (!checkForLinks) {
			mReaderView.onAction(action);
			return true;
		}

		// check link before executing action
		mReaderView.mEngine.execute(new Task() {
			String link;
			ImageInfo image;
			Bookmark bookmark;
			public void work() {
				image = new ImageInfo();
				image.bufWidth = mReaderView.internalDX;
				image.bufHeight = mReaderView.internalDY;
				image.bufDpi = mActivity.getDensityDpi();
				if (mReaderView.doc.checkImage(start_x, start_y, image)) {
					return;
				}
				image = null;
				link = mReaderView.doc.checkLink(start_x, start_y, mActivity.getPalmTipPixels() / 2 );
				if (link != null) {
					if (link.startsWith("#")) {
						log.d("go to " + link);
						mReaderView.doc.goLink(link);
						mReaderView.drawPage();
					}
					return;
				}
				bookmark = mReaderView.doc.checkBookmark(start_x, start_y);
				if (bookmark != null && bookmark.getType() == Bookmark.TYPE_POSITION)
					bookmark = null;
			}
			public void done() {
				if (bookmark != null)
					bookmark = mBookInfo.findBookmark(bookmark);
				if (link == null && image == null && bookmark == null) {
					mReaderView.onAction(action);
				} else if (image != null) {
					mReaderView.startImageViewer(image);
				} else if (bookmark != null) {
					BookmarkEditDialog dlg = new BookmarkEditDialog(mActivity, mReaderView, bookmark, false, Bookmark.TYPE_COMMENT, "");
					dlg.show();
				} else if (!link.startsWith("#")) {
					log.d("external link " + link);
					if (link.startsWith("http://")||link.startsWith("https://")) {
						mActivity.openURL(link, true);
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
						if (StrUtils.getNonEmptyStr(mBookInfo.getFileInfo().opdsLink,false).startsWith("http")) {
							try {
								String slink = mBookInfo.getFileInfo().opdsLink;
								URL baseUrl = new URL(slink);
								URL targetUrl = new URL(baseUrl, link);
								mActivity.openURL(targetUrl.toString(), true);
							} catch (Exception e) {
								log.e("exception while building an url", e);
							}
						} else {
							mActivity.showSToast(mActivity.getString(R.string.open_url_cannot, link));
						}
					}
				}
			}
		});
		return true;
	}

	private boolean startSelection() {
		state = STATE_SELECTION;
		// check link before executing action
		mReaderView.mEngine.execute(new Task() {
			ImageInfo image;
			Bookmark bookmark;
			public void work() {
				image = new ImageInfo();
				image.bufWidth = mReaderView.internalDX;
				image.bufHeight = mReaderView.internalDY;
				image.bufDpi = mActivity.getDensityDpi();
				if (!mReaderView.doc.checkImage(start_x, start_y, image))
					image = null;
				bookmark = mReaderView.doc.checkBookmark(start_x, start_y);
				if (bookmark != null && bookmark.getType() == Bookmark.TYPE_POSITION)
					bookmark = null;
				//plotn link experiments ...
				//String link = mReaderView.doc.checkLink(start_x, start_y, 30);
				//if (!StrUtils.isEmptyStr(link))
				//	mActivity.showSToast(StrUtils.updateText(link,true));
			}public void done() {
				if (bookmark != null)
					bookmark = mBookInfo.findBookmark(bookmark);
				if (image != null) {
					cancel();
					mReaderView.startImageViewer(image);
				} else if (bookmark != null) {
					cancel();
					boolean bMenuShow = false;
					if (mReaderView.hyplinkBookmark != null) {
						if (mReaderView.hyplinkBookmark.getLinkPos().equals(bookmark.getStartPos())) {
							bMenuShow=true;
						}
					}
					if ((StrUtils.isEmptyStr(bookmark.getCommentText()))&&(!bMenuShow)&&(StrUtils.isEmptyStr(bookmark.getLinkPos()))) {
						mActivity.skipFindInDic = true;
						BookmarkEditDialog dlg = new BookmarkEditDialog(mActivity, mReaderView, bookmark, false, Bookmark.TYPE_COMMENT, "");
						dlg.show();
					} else {
						if (!StrUtils.isEmptyStr(bookmark.getCommentText()))
							mActivity.showSToast(StrUtils.updateText(bookmark.getCommentText(),true));
						if (bMenuShow) {
							ReaderAction[] actions = {
									ReaderAction.GO_BACK
							};
							mActivity.showActionsPopupMenu(actions, item -> {
								if (item == ReaderAction.GO_BACK) {
									mReaderView.goToBookmark(mReaderView.hyplinkBookmark);
									mReaderView.hyplinkBookmark=null;
									return true;
								}
								return false;
							});
						} else {
							String sL = bookmark.getLinkPos();
							if (!StrUtils.isEmptyStr(sL)) {
								for (Bookmark bm : mBookInfo.getAllBookmarks()) {
									if (bm.getStartPos().equals(bookmark.getLinkPos())) {
										mReaderView.goToBookmark(bm);
										mReaderView.hyplinkBookmark = bookmark;
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
					if (curTime - mReaderView.lastSelTime > 300)
						mReaderView.updateSelection( start_x, start_y, start_x, start_y, false );
				}
			}
		});
		return true;
	}

	private boolean trackDoubleTap() {
		state = STATE_WAIT_FOR_DOUBLE_CLICK;
		BackgroundThread.instance().postGUI(() -> {
			if (mReaderView.currentTapHandler == TapHandler.this && state == STATE_WAIT_FOR_DOUBLE_CLICK)
				performAction(shortTapAction, false);
		}, mReaderView.getDoubleClickInterval());
		return true;
	}

	private boolean trackDoubleTapInsp() {
		stateInsp = STATE_WAIT_FOR_DOUBLE_CLICK;
		BackgroundThread.instance().postGUI(() -> stateInsp = STATE_INITIAL, mReaderView.getDoubleClickInterval());
		return true;
	}

	private boolean trackLongTap() {
		BackgroundThread.instance().postGUI(() -> {
			if (mReaderView.currentTapHandler == TapHandler.this && state == STATE_DOWN_1) {
				if (longTapAction == ReaderAction.START_SELECTION)
					startSelection();
				else
					performAction(longTapAction, true);
			}
		}, mReaderView.LONG_KEYPRESS_TIME);
		return true;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (mReaderView.disableTouch) {
			mReaderView.disableTouch = false;
			return false;
		}
		int index = event.getActionIndex();
		int x = (int)event.getX();
		int y = (int)event.getY();
		curPageText = mActivity.getmReaderFrame().getUserDicPanel().getCurPageText(0, false);
		if ((event.getPointerCount() > 1) && (state == STATE_DOWN_1) && (!mReaderView.mDisableTwoPointerGestures) &&
				(!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))) {
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
			if ((y < 30) || (y > (mReaderView.getSurface().getHeight() - 30)))
				return unexpectedEvent();
		}

		if (state == STATE_INITIAL && event.getAction() != MotionEvent.ACTION_DOWN)
			return unexpectedEvent(); // ignore unexpected event

		//KR warning. This is CR implementation of preventing accidential taps, we have our own
//			if (!doubleTapSelectionEnabled && secondaryTapActionType != TAP_ACTION_TYPE_DOUBLE) {
//				// filter bounce (only when double taps not enabled)
//				if (event.getAction() == MotionEvent.ACTION_DOWN) {
//					if (state == STATE_INITIAL && Utils.timeInterval(firstTapTimeStamp) < mBounceTapInterval)
//						return unexpectedEvent(); // ignore bounced taps
//				}
//			}

		// Uncomment to disable user interaction during cloud sync
		//if (isCloudSyncProgressActive())
		//	return unexpectedEvent();

		if (event.getAction() == MotionEvent.ACTION_UP) {
			long duration = Utils.timeInterval(firstDown);
			mReaderView.lastDuration = duration;
			switch (state) {
				case STATE_DOWN_1:
					if (mReaderView.hiliteTapZoneOnTap) {
						mReaderView.hiliteTapZone( true, x, y, width, height );
						mReaderView.scheduleUnhilite( mReaderView.LONG_KEYPRESS_TIME );
					}
					if (duration > mReaderView.LONG_KEYPRESS_TIME) {
						if (longTapAction == ReaderAction.START_SELECTION)
							return startSelection();
						return performAction(longTapAction, true);
					}
					if (doubleTapAction.isNone())
						return performAction(shortTapAction, false);
					// start possible double tap tracking
					return trackDoubleTap();
				case STATE_FLIPPING:
					mReaderView.stopAnimation(x, y);
					state = STATE_DONE;
					return cancel();
				case STATE_BRIGHTNESS:
					//mReaderView.stopBrightnessControl(start_y, now_y, leftSideBrightness);
					mReaderView.stopBrightnessControl(x, y, brightness_side);
					state = STATE_DONE;
					return cancel();
				case STATE_SELECTION:
					// If the second tap is within a radius of the first tap point, assume the user is trying to double tap on the same point
					if (start_x-x <= mReaderView.DOUBLE_TAP_RADIUS && x-start_x <= mReaderView.DOUBLE_TAP_RADIUS &&
							y-start_y <= mReaderView.DOUBLE_TAP_RADIUS && start_y-y <= mReaderView.DOUBLE_TAP_RADIUS) {
						//log.v("upd2: "+nextUpdateId+" ");
						mReaderView.updateSelection(start_x, start_y, start_x, start_y, true);
					}
					else {
						//log.v("upd3: "+nextUpdateId+" ");
						mReaderView.updateSelection(start_x, start_y, x, y, true);
					}
					mReaderView.selectionModeWasActive = mReaderView.selectionModeActive;
					mReaderView.selectionModeActive = false;
					mReaderView.toggleScreenUpdateModeMode(true);
					if (mReaderView.inspectorModeActive) {
						boolean res = cancel();
						mReaderView.currentTapHandler.trackDoubleTapInsp();
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
						props.setProperty(Settings.PROP_FONT_SIZE, "" + fontSizeToSet);
						mActivity.setSettings(props, -1, true);
					}
					return cancel();
			}
		} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
			long curTime = System.currentTimeMillis();
			if ((!mReaderView.doubleTapSelectionEnabled) && (mReaderView.getPreventClickInterval() > 0)
					&& ((curTime - mReaderView.lastTimeTap) < mReaderView.getPreventClickInterval())) {
				log.i("Ignore phantom taps, ms = " + (curTime - mReaderView.lastTimeTap));
				mReaderView.lastTimeTap = curTime;
				return true;
			}
			mReaderView.lastTimeTap = curTime;
			if ((mReaderView.inspectorModeActive) && (stateInsp == STATE_WAIT_FOR_DOUBLE_CLICK)) {
				mReaderView.toggleInspectorMode();
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
					width = mReaderView.surface.getWidth();
					height = mReaderView.surface.getHeight();
					int zone = mReaderView.getTapZone(x, y, width, height);
					shortTapAction = mReaderView.findTapZoneAction(zone, mReaderView.TAP_ACTION_TYPE_SHORT);
					longTapAction = mReaderView.findTapZoneAction(zone, mReaderView.TAP_ACTION_TYPE_LONGPRESS);
					doubleTapAction = mReaderView.findTapZoneAction(zone, mReaderView.TAP_ACTION_TYPE_DOUBLE);
					firstDown = Utils.timeStamp();
					mReaderView.firstTapTimeStamp = firstDown;
					if (mReaderView.selectionModeActive || mReaderView.inspectorModeActive) {
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
					if (mReaderView.inspectorModeActive) {
						mReaderView.toggleInspectorMode();
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
			int dragThreshold = mActivity.getPalmTipPixels();
			int dragThresholdK = mActivity.getPalmTipPixelsK(mReaderView.mGesturePageFlipSensivity);
			boolean isVertical = (Math.abs(start_x - x) * 2) < Math.abs(start_y - y);
			boolean isVerticalStrict = (Math.abs(start_x - x) * 3) < Math.abs(start_y - y);
			switch (state) {
				case STATE_DOWN_1:
					if (distance < Math.min(dragThreshold, dragThresholdK))
						return true;
					int nonSensL = mActivity.settings().getInt(Settings.PROP_APP_TAP_ZONE_NON_SENS_LEFT, 0);
					nonSensL = nonSensL * width / 2000;
					int nonSensR = mActivity.settings().getInt(Settings.PROP_APP_TAP_ZONE_NON_SENS_RIGHT, 0);
					nonSensR = nonSensR * width / 2000;
					if ((!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) ||
							DeviceInfo.SCREEN_CAN_CONTROL_BRIGHTNESS ||
							DeviceInfo.EINK_HAVE_FRONTLIGHT) && mReaderView.isBacklightControlFlick != mReaderView.BACKLIGHT_CONTROL_FLICK_NONE && ady > adx) {
						// backlight control enabled
						boolean bLeftCold = mReaderView.getBacklightEnabled(mReaderView.isBacklightControlFlick, true, true);
						boolean bRightCold = mReaderView.getBacklightEnabled(mReaderView.isBacklightControlFlick, false, true);
						boolean leftOk = (start_x >= nonSensL) && (start_x < nonSensL + (dragThreshold * 170 / 100));
						boolean rightOk = (start_x <= width - nonSensR) && (start_x > (width - (dragThreshold * 170 / 100) - nonSensR));
						if (
							((leftOk && bLeftCold) || (rightOk && bRightCold)) && (isVertical)
						) {
							// brightness
							state = STATE_BRIGHTNESS;
							brightness_side = mReaderView.BRIGHTNESS_TYPE_RIGHT_SIDE;
							if (leftOk) brightness_side = mReaderView.BRIGHTNESS_TYPE_LEFT_SIDE;
							mReaderView.startBrightnessControl(start_x, start_y, brightness_side);
							return true;
						}
					}
					if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT && mReaderView.isBacklightControlFlick != mReaderView.BACKLIGHT_CONTROL_FLICK_NONE && ady > adx) {
						// warm backlight control enabled
						boolean bLeftWarm = mReaderView.getBacklightEnabled(mReaderView.isBacklightControlFlick, true, false);
						boolean bRightWarm = mReaderView.getBacklightEnabled(mReaderView.isBacklightControlFlick, false, false);
						boolean leftOk = (start_x >= nonSensL) && (start_x < nonSensL + (dragThreshold * 170 / 100));
						boolean rightOk = (start_x <= width - nonSensR) && (start_x > (width - (dragThreshold * 170 / 100) - nonSensR));
						if (
								((leftOk && bLeftWarm) || (rightOk && bRightWarm)) && (isVertical)
						) {
							// warm backlight brightness
							state = STATE_BRIGHTNESS;
							brightness_side = mReaderView.BRIGHTNESS_TYPE_RIGHT_SIDE;
							if (leftOk) brightness_side = mReaderView.BRIGHTNESS_TYPE_LEFT_SIDE;
							mReaderView.startBrightnessControl(start_x, start_y, brightness_side);
							return true;
						}
					}
					if (distance < dragThresholdK)
						return true;
					//boolean isPageMode = mSettings.getInt(PROP_PAGE_VIEW_MODE, 1) == 1;
					//int dir = isPageMode ? x - start_x : y - start_y;
					//plotn - some strange behavior with mIsPageMode
					//int dir = mIsPageMode ? x - start_x : y - start_y;
					int dir = mReaderView.viewMode == ViewMode.PAGES ? x - start_x : y - start_y;
					boolean isPageTurnSwipe = adx > ((ady * 10) / 17);
					//log.i("isPageTurnSwipe: " + isPageTurnSwipe);
					boolean menuShown = false;
					// to work to poke3 ?
					//mActivity.showToast(" coords: " + y + " max " + mReaderView.surface.getHeight());
					//int dir = x - start_x;
					if (mReaderView.viewMode == ViewMode.PAGES) {
						if (mReaderView.mGesturePageFlipSwipeN == 1) {
							if (
									(x <= (width / 2) + dragThresholdK) &&
									(x >= (width / 2) - dragThresholdK) &&
									(start_x <= (width / 2) + dragThresholdK) &&
									(start_x >= (width / 2) - dragThresholdK) &&
									isVerticalStrict // x shoud be three times shorter than y
							) {
								menuShown = true;
								mActivity.showReaderMenu();
								return endEvent();
							} else if (isPageTurnSwipe)
								return performAction(dir < 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP, false);
							else
								return endEvent();
						}
						if (mReaderView.mGesturePageFlipSwipeN == 2) {
							//dir *= mGesturePageFlipsPerFullSwipe; // Change sign of page flip direction according to user setting
							if (mReaderView.getPageFlipAnimationSpeedMs() == 0 || DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
								// no animation
								if (
										(x <= (width / 2) + dragThresholdK) &&
										(x >= (width / 2) - dragThresholdK) &&
										(start_x <= (width / 2) + dragThresholdK) &&
										(start_x >= (width / 2) - dragThresholdK) &&
										isVerticalStrict // x shoud be three times shorter than y
								) {
									menuShown = true;
									mActivity.showReaderMenu();
									return endEvent();
								} else if (isPageTurnSwipe)
									return performAction(dir < 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP, false);
								else
									return endEvent();
							}
							mReaderView.startAnimation(start_x, start_y, width, height, x, y);
							mReaderView.updateAnimation(x, y);
							state = STATE_FLIPPING;
						}
					}
					if (mReaderView.viewMode == ViewMode.SCROLL) {
						if (isPageTurnSwipe)
							return performAction(dir < 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP, false);
						mReaderView.startAnimation(start_x, start_y, width, height, x, y);
						mReaderView.updateAnimation(x, y);
						state = STATE_FLIPPING;
						return true;
					}
					if (mReaderView.mGesturePageFlipSwipeN == 3) {
						state = STATE_FLIP_TRACKING;
						updatePageFlipTracking(start_x, start_y);
					}
					if (
							(mReaderView.viewMode == ViewMode.PAGES) &&
							(x <= (width / 2) + dragThresholdK) &&
							(x >= (width / 2) - dragThresholdK) &&
							(start_x <= (width / 2) + dragThresholdK) &&
							(start_x >= (width / 2) - dragThresholdK) &&
							((Math.abs(start_x - x) * 2) < Math.abs(start_y - y)) &&
							(!menuShown)
					) {
						mActivity.showReaderMenu();
						return endEvent();
					}
					return true;
				case STATE_FLIPPING:
					mReaderView.updateAnimation(x, y);
					return true;
				case STATE_BRIGHTNESS:
					//updateBrightnessControl(start_y, now_y, leftSideBrightness);
					mReaderView.updateBrightnessControl(y, brightness_side);
					return true;
				case STATE_FLIP_TRACKING:
					updatePageFlipTracking(x, y);
					return true;
				case STATE_WAIT_FOR_DOUBLE_CLICK:
					return true;
				case STATE_SELECTION:
					mReaderView.updateSelection(start_x, start_y, x, y, false);
					break;
				case STATE_TWO_POINTERS_VERT_MARGINS:
					if (event.getPointerCount()>1) {
						int distance1 = (int) (Math.sqrt(Math.abs(start_x2 - start_x) * Math.abs(start_x2 - start_x) +
								Math.abs(start_y2 - start_y) * Math.abs(start_y2 - start_y)));
						int distance2 = (int) (Math.sqrt(Math.abs(now_x2 - now_x) * Math.abs(now_x2 - now_x) +
								Math.abs(now_y2 - now_y) * Math.abs(now_y2 - now_y)));
						int aval = distance1 - distance2;
						aval = aval / PINCH_TRESHOLD_COUNT;
						marginToSet = PageMarginsOption.getMarginShift(marginBegin, aval);
						mReaderView.showCenterPopup(mActivity.getString(R.string.vert_margin_control) + " " + marginToSet);
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
						marginToSet = PageMarginsOption.getMarginShift(marginBegin, aval);
						mReaderView.showCenterPopup(mActivity.getString(R.string.horz_margin_control)+" "+marginToSet);
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
						mReaderView.showCenterPopupFont(mActivity.getString(R.string.font_size_control)+" "+fontSizeToSet, s, fontSizeToSet);
					}
					break;
			}

		} else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
			return unexpectedEvent();
		}
		return true;
	}
}