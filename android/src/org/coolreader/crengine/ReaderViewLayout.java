package org.coolreader.crengine;

import android.graphics.Rect;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.readerview.ReaderView;
import org.coolreader.userdic.UserDicPanel;

import java.util.ArrayList;

public class ReaderViewLayout extends ViewGroup implements Settings {

		public static final Logger log = L.create("rvl");

		private CoolReader activity;
		private ReaderView contentView;
		private StatusBar statusView;
		private UserDicPanel userDicView;
		private CRToolBar toolbarView;
		private LinearLayout llLeft;
		private LinearLayout llRight;
		private LinearLayout llTop;
		private LinearLayout llBottom;
		private int statusBarLocation;
		private int toolbarLocation;
		private int userDicLocation;
		private boolean hideToolbarInFullscren;
		private boolean fullscreen;
		private boolean nightMode;
		ReaderView.ToolbarBackgroundDrawable toolbarBackground;
		ReaderView.ToolbarBackgroundDrawable statusBackground;
		ReaderView.ToolbarBackgroundDrawable llLeftBackground;
		ReaderView.ToolbarBackgroundDrawable llRightBackground;
		ReaderView.ToolbarBackgroundDrawable llTopBackground;
		ReaderView.ToolbarBackgroundDrawable llBottomBackground;

		public CRToolBar getToolBar() {
			return toolbarView;
		}
		
		public StatusBar getStatusBar() {
			return statusView;
		}

		public UserDicPanel getUserDicPanel() {
		return userDicView;
	}
		
		public void updateFullscreen(boolean fullscreen) {
			if (this.fullscreen == fullscreen)
				return;
			this.fullscreen = fullscreen;
			statusView.updateFullscreen(fullscreen);
			userDicView.updateFullscreen(fullscreen);
			requestLayout();
		}
		
		public boolean isToolbarVisible() {
			return toolbarLocation != VIEWER_TOOLBAR_NONE && (!fullscreen || !hideToolbarInFullscren);
		}
		
		public boolean isStatusbarVisible() {
			return statusBarLocation == VIEWER_STATUS_BOTTOM || statusBarLocation == VIEWER_STATUS_TOP;
		}

		public boolean isUserDicVisible() {
			return activity.ismShowUserDicPanel();
		}
		
		public void updateSettings(Properties settings) {
			log.d("ReaderViewLayout.updateSettings()");
			nightMode = settings.getBool(PROP_NIGHT_MODE, false);
			statusBarLocation = settings.getInt(PROP_STATUS_LOCATION, VIEWER_STATUS_TOP);
			toolbarLocation = settings.getInt(PROP_TOOLBAR_LOCATION, VIEWER_TOOLBAR_SHORT_SIDE);
			hideToolbarInFullscren = settings.getBool(PROP_TOOLBAR_HIDE_IN_FULLSCREEN, true);
			statusView.setVisibility(isStatusbarVisible() ? VISIBLE : GONE);
			statusView.updateSettings(settings);
			userDicView.setVisibility(isUserDicVisible() ? VISIBLE : GONE);
			userDicView.updateSettings(settings);
			toolbarView.updateNightMode(nightMode);
			toolbarView.setVisibility(isToolbarVisible() ? VISIBLE : GONE);
			requestLayout();
		}
		
		public void showMenu() {
			if (isToolbarVisible())
				toolbarView.showOverflowMenu();
			else
				toolbarView.showAsPopup(this, item -> {
					activity.getReaderView().onAction(item);
					return true;
				}, null);
//			new OnOverflowHandler() {
//					@Override
//					public boolean onOverflowActions(ArrayList<ReaderAction> actions) {
//						toolbarView.showOverflowMenu();
////						activity.showActionsPopupMenu(actions, new OnActionHandler() {
////							@Override
////							public boolean onActionSelected(ReaderAction item) {
////								activity.getReaderView().onAction(item);
////								return true;
////							}
////						});
//						return false;
//					}
//				});
		}
		
		public ReaderViewLayout(CoolReader context, ReaderView contentView) {
			super(context);
			this.activity = context;
			this.contentView = contentView;
			this.statusView = new StatusBar(context);
			statusBackground = contentView.createToolbarBackgroundDrawable();
			this.statusView.setBackgroundDrawable(statusBackground);
			this.userDicView = new UserDicPanel(context);
			this.userDicView.setBackgroundDrawable(statusBackground);
			toolbarBackground = contentView.createToolbarBackgroundDrawable();
			ArrayList<ReaderAction> actionsList = ReaderAction.createList(
					ReaderAction.GO_BACK,
					ReaderAction.TOC,
					ReaderAction.BOOK_INFO,
					ReaderAction.FONTS_MENU,
					ReaderAction.SEARCH,
					ReaderAction.OPTIONS,
					ReaderAction.BOOKMARKS,
					ReaderAction.FILE_BROWSER_ROOT,
					ReaderAction.TOGGLE_DAY_NIGHT,
					ReaderAction.TOGGLE_SELECTION_MODE,
					ReaderAction.GO_PAGE,
					//ReaderAction.GO_PERCENT,
					ReaderAction.FILE_BROWSER,
					ReaderAction.TTS_PLAY,
					ReaderAction.GO_FORWARD,
					ReaderAction.RECENT_BOOKS,
					ReaderAction.OPEN_PREVIOUS_BOOK,
					ReaderAction.TOGGLE_AUTOSCROLL,
					ReaderAction.SAVE_LOGCAT);
			if (DeviceInfo.EINK_HAVE_FRONTLIGHT) {
				if (DeviceInfo.EINK_ONYX && DeviceInfo.ONYX_HAVE_BRIGHTNESS_SYSTEM_DIALOG) {
					actionsList.add(7, ReaderAction.SHOW_SYSTEM_BACKLIGHT_DIALOG);
				}
				// TODO: add other e-ink devices with backlight support
			}
			if (BuildConfig.GSUITE_AVAILABLE && DeviceInfo.getSDKLevel() >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				actionsList.add(ReaderAction.GDRIVE_SYNCTO);
				actionsList.add(ReaderAction.GDRIVE_SYNCFROM);
			}
			actionsList.add(ReaderAction.ABOUT);
			actionsList.add(ReaderAction.HIDE);
			this.toolbarView = new CRToolBar(context, actionsList, false, false, false, false);
			this.toolbarView.setBackgroundDrawable(toolbarBackground);
			this.toolbarView.useBackgrColor = true;
			this.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			this.addView(toolbarView);
			this.addView(contentView.getSurface());
			this.addView(statusView);
			this.addView(userDicView);
			llLeft = new LinearLayout(context);
			llLeft.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			llLeft.setOrientation(LinearLayout.HORIZONTAL);
			llLeftBackground = contentView.createToolbarBackgroundDrawable();
			llLeft.setBackgroundDrawable(llLeftBackground);
			llRight = new LinearLayout(context);
			llRight.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			llRight.setOrientation(LinearLayout.HORIZONTAL);
			llRightBackground = contentView.createToolbarBackgroundDrawable();
			llRight.setBackgroundDrawable(llRightBackground);
			llTop = new LinearLayout(context);
			llTop.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			llTop.setOrientation(LinearLayout.HORIZONTAL);
			llTopBackground = contentView.createToolbarBackgroundDrawable();
			llTop.setBackgroundDrawable(llTopBackground);
			llBottom = new LinearLayout(context);
			llBottom.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			llBottom.setOrientation(LinearLayout.HORIZONTAL);
			llBottomBackground = contentView.createToolbarBackgroundDrawable();
			llBottom.setBackgroundDrawable(llBottomBackground);
			this.addView(llLeft);
			this.addView(llRight);
			this.addView(llTop);
			this.addView(llBottom);
			toolbarView.setFocusable(false);
			statusView.setFocusable(false);
			toolbarView.setFocusableInTouchMode(false);
			statusView.setFocusableInTouchMode(false);
			userDicView.setFocusable(false);
			userDicView.setFocusableInTouchMode(false);
			contentView.getSurface().setFocusable(true);
			contentView.getSurface().setFocusableInTouchMode(true);
			updateFullscreen(activity.isFullscreen());
			updateSettings(context.settings());
			onThemeChanged(activity.getCurrentTheme());
		}

		public void updateCRToolbar(CoolReader context) {
			this.toolbarView.createActionsLists(null,false);
			toolbarView.updateNightMode(nightMode);
			toolbarView.setButtonAlpha(activity.getCurrentTheme().getToolbarButtonAlpha());
			toolbarView.onThemeChanged(activity.getCurrentTheme());
			this.toolbarView.calcLayout();
			requestLayout();
		}

		public void onThemeChanged(InterfaceTheme theme) {
//			if (DeviceInfo.EINK_SCREEN) {
//				statusView.setBackgroundColor(0xFFFFFFFF);
//				toolbarView.setBackgroundColor(0xFFFFFFFF);
//			} else if (nightMode) {
//				statusView.setBackgroundColor(0xFF000000);
//				toolbarView.setBackgroundColor(0xFF000000);
//			} else {
//				statusView.setBackgroundResource(theme.getReaderStatusBackground());
//				toolbarView.setBackgroundResource(theme.getReaderToolbarBackground(toolbarView.isVertical()));
//			}
			toolbarView.updateNightMode(nightMode);
			toolbarView.setButtonAlpha(theme.getToolbarButtonAlpha());
			toolbarView.onThemeChanged(theme);
			statusView.onThemeChanged(theme);
			userDicView.onThemeChanged(theme);
		}

		
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			log.v("onLayout(" + l + ", " + t + ", " + r + ", " + b + ")");

		//	for (StackTraceElement ste: Thread.currentThread().getStackTrace()) {
		//		log.v(ste.toString());
		//	};
			r -= l;
			b -= t;
			t = 0;
			l = 0;

			int full_l = l;
			int full_r = r;
			int full_t = t;
			int full_b = b;

			int marg = activity.settings().getInt(PROP_GLOBAL_MARGIN, 0);
			l = l + marg;
			r = r - marg;
			t = t + marg;
			b = b - marg;
			if (marg > 0) {
				llLeft.layout(0, full_t, marg, full_b);
				llRight.layout(full_r-marg, full_t, full_r, full_b);
				llTop.layout(full_l, 0, full_r, marg);
				llBottom.layout(full_l, full_b - marg, full_r, full_b);
			}

			statusView.setVisibility(isStatusbarVisible() ? VISIBLE : GONE);
			userDicView.setVisibility(isUserDicVisible() ? VISIBLE : GONE);
			toolbarView.setVisibility(isToolbarVisible() ? VISIBLE : GONE);
			
			boolean toolbarVisible = toolbarLocation != VIEWER_TOOLBAR_NONE && (!fullscreen || !hideToolbarInFullscren);
			boolean landscape = r > b;
			Rect toolbarRc = new Rect(l, t, r, b);
			if (toolbarVisible) {
				int location = toolbarLocation;
				if (location == VIEWER_TOOLBAR_SHORT_SIDE)
					location = landscape ? VIEWER_TOOLBAR_LEFT : VIEWER_TOOLBAR_TOP;
				else if (location == VIEWER_TOOLBAR_LONG_SIDE)
					location = landscape ? VIEWER_TOOLBAR_TOP : VIEWER_TOOLBAR_LEFT;
				switch (location) {
				case VIEWER_TOOLBAR_LEFT:
					//toolbarView.setBackgroundResource(activity.getCurrentTheme().getReaderToolbarBackground(true));
					toolbarRc.right = l + toolbarView.getMeasuredWidth();
					//toolbarView.layout(l, t, l + toolbarView.getMeasuredWidth(), b);
					l += toolbarView.getMeasuredWidth();
					break;
				case VIEWER_TOOLBAR_RIGHT:
					//toolbarView.setBackgroundResource(activity.getCurrentTheme().getReaderToolbarBackground(true));
					toolbarRc.left = r - toolbarView.getMeasuredWidth();
					//toolbarView.layout(r - toolbarView.getMeasuredWidth(), t, r, b);
					r -= toolbarView.getMeasuredWidth();
					break;
				case VIEWER_TOOLBAR_TOP:
					//toolbarView.setBackgroundResource(activity.getCurrentTheme().getReaderToolbarBackground(false));
					toolbarRc.bottom = t + toolbarView.getMeasuredHeight();
					//toolbarView.layout(l, t, r, t + toolbarView.getMeasuredHeight());
					t += toolbarView.getMeasuredHeight();
					break;
				case VIEWER_TOOLBAR_BOTTOM:
					//toolbarView.setBackgroundResource(activity.getCurrentTheme().getReaderToolbarBackground(false));
					toolbarRc.top = b - toolbarView.getMeasuredHeight();
					//toolbarView.layout(l, b - toolbarView.getMeasuredHeight(), r, b);
					b -= toolbarView.getMeasuredHeight();
					break;
				}
				toolbarBackground.setLocation(location);
			}
			Rect statusRc = new Rect(l, t, r, b);
			if (statusBarLocation == VIEWER_STATUS_TOP) {
				statusRc.bottom = t + statusView.getMeasuredHeight();
				//statusView.layout(l, t, r, t + statusView.getMeasuredHeight());
				t += statusView.getMeasuredHeight();
			} else if (statusBarLocation == VIEWER_STATUS_BOTTOM) {
				statusRc.top = b - statusView.getMeasuredHeight();
				//statusView.layout(l, b - statusView.getMeasuredHeight(), r, b);
				b -= statusView.getMeasuredHeight();
			}

			Rect userDicRc = new Rect(l, t, r, b);
			if (isUserDicVisible()) {
				userDicRc.top = b - userDicView.getMeasuredHeight();
				b -= userDicView.getMeasuredHeight();
			}

			statusBackground.setLocation(statusBarLocation);
			contentView.getSurface().layout(l, t, r, b);
			toolbarView.layout(toolbarRc.left, toolbarRc.top, toolbarRc.right, toolbarRc.bottom);
			statusView.layout(statusRc.left, statusRc.top, statusRc.right, statusRc.bottom);
			userDicView.layout(userDicRc.left, userDicRc.top, userDicRc.right, userDicRc.bottom);
			
			if (activity.isFullscreen()) {
				BackgroundThread.instance().postGUI(() -> {
					log.v("Invalidating toolbar ++++++++++");
					toolbarView.forceLayout();
					contentView.getSurface().invalidate();
					toolbarView.invalidate();
				}, 100);
			}
			
			//			toolbarView.invalidate();
//			toolbarView.requestLayout();
			//invalidate();
		}
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int w = MeasureSpec.getSize(widthMeasureSpec);
			int h = MeasureSpec.getSize(heightMeasureSpec);
	        setMeasuredDimension(w, h);

			boolean statusVisible = statusBarLocation == VIEWER_STATUS_BOTTOM || statusBarLocation == VIEWER_STATUS_TOP;
			boolean userDicVisible = true;
			boolean toolbarVisible = toolbarLocation != VIEWER_TOOLBAR_NONE && (!fullscreen || !hideToolbarInFullscren);
			boolean landscape = w > h;
			if (toolbarVisible) {
				int location = toolbarLocation;
				if (location == VIEWER_TOOLBAR_SHORT_SIDE)
					location = landscape ? VIEWER_TOOLBAR_LEFT : VIEWER_TOOLBAR_TOP;
				else if (location == VIEWER_TOOLBAR_LONG_SIDE)
					location = landscape ? VIEWER_TOOLBAR_TOP : VIEWER_TOOLBAR_LEFT;
				switch (location) {
				case VIEWER_TOOLBAR_LEFT:
				case VIEWER_TOOLBAR_RIGHT:
					toolbarView.setVertical(true);
					toolbarView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
							MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
					w -= toolbarView.getMeasuredWidth();
					break;
				case VIEWER_TOOLBAR_TOP:
				case VIEWER_TOOLBAR_BOTTOM:
					toolbarView.setVertical(false);
					toolbarView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
							MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
					h -= toolbarView.getMeasuredHeight();
					break;
				}
			}
			if (statusVisible) {
				statusView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
				h -= statusView.getMeasuredHeight();
			}

			if (userDicVisible) {
				userDicView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
				h -= userDicView.getMeasuredHeight();
			}
			
			contentView.getSurface().measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			requestLayout();
		}
	}