package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.utils.Utils;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class BrowserViewLayout extends ViewGroup {
	private CoolReader activity;
	private FileBrowser contentView;
	private View titleView;
	private View filterView;
	private CRToolBar toolbarView;
	private boolean filterIsShown;
	private LinearLayout bottomBar;
	private LinearLayout bottomBar1Btn;
	private LinearLayout bottomBarLitres;
	private Button btnPop;
	private Button btnNew;

	private Button btnStateDir;
	private Button btnStateNoMark;
	private Button btnStateToRead;
	private Button btnStateReading;
	private Button btnStateFinished;

	private boolean bStateDir = true;
	private boolean bStateNoMark = true;
	private boolean bStateToRead = true;
	private boolean bStateReading = true;
	private boolean bStateFinished = true;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	private boolean newBooks = true;

	private void setNewOrPopChecked(Button btn) {
		if (contentView == null) return;
		if (contentView.saveParams == null) return;
		if (btn != null) {
			if (btn == btnNew) {
				newBooks = true;
			}
			if (btn == btnPop) {
				newBooks = false;

			}
		}
		int colorGrayC;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast});
		colorGrayC = a.getColor(0, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		activity.tintViewIcons(btnNew, PorterDuff.Mode.CLEAR,true);
		activity.tintViewIcons(btnPop, PorterDuff.Mode.CLEAR,true);
		btnNew.setBackgroundColor(colorGrayCT);
		btnPop.setBackgroundColor(colorGrayCT);
		if (newBooks) {
			btnNew.setBackgroundColor(colorGrayCT2);
			activity.tintViewIcons(btnNew,true);
			contentView.saveParams.newOrPop = 0;
			if (btn != null) ((CoolReader)activity).showBrowser(FileInfo.LITRES_TAG, contentView.saveParams);
		}
		if (!newBooks) {
			btnPop.setBackgroundColor(colorGrayCT2);
			activity.tintViewIcons(btnPop,true);
			contentView.saveParams.newOrPop = 1;
			if (btn != null) ((CoolReader)activity).showBrowser(FileInfo.LITRES_TAG, contentView.saveParams);
		}
	}

	private void paintStateButtons() {
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		activity.tintViewIcons(btnStateDir, PorterDuff.Mode.CLEAR,true);
		activity.tintViewIcons(btnStateNoMark, PorterDuff.Mode.CLEAR,true);
		activity.tintViewIcons(btnStateToRead, PorterDuff.Mode.CLEAR,true);
		activity.tintViewIcons(btnStateReading, PorterDuff.Mode.CLEAR,true);
		activity.tintViewIcons(btnStateFinished, PorterDuff.Mode.CLEAR,true);
		if (isEInk) colorGrayCT = Color.WHITE;
		if (bStateDir) {
			btnStateDir.setBackgroundColor(colorGrayCT2);
			activity.tintViewIcons(btnStateDir,true);
			if (isEInk) Utils.setSolidButtonEink(btnStateDir);
		} else btnStateDir.setBackgroundColor(colorGrayCT);
		if (bStateNoMark) {
			btnStateNoMark.setBackgroundColor(colorGrayCT2);
			activity.tintViewIcons(btnStateNoMark,true);
			if (isEInk) Utils.setSolidButtonEink(btnStateNoMark);
		} else btnStateNoMark.setBackgroundColor(colorGrayCT);
		if (bStateToRead) {
			btnStateToRead.setBackgroundColor(colorGrayCT2);
			activity.tintViewIcons(btnStateToRead,true);
			if (isEInk) Utils.setSolidButtonEink(btnStateToRead);
		}  else btnStateToRead.setBackgroundColor(colorGrayCT);
		if (bStateReading) {
			btnStateReading.setBackgroundColor(colorGrayCT2);
			activity.tintViewIcons(btnStateReading,true);
			if (isEInk) Utils.setSolidButtonEink(btnStateReading);
		} else btnStateReading.setBackgroundColor(colorGrayCT);
		if (bStateFinished) {
			btnStateFinished.setBackgroundColor(colorGrayCT2);
			activity.tintViewIcons(btnStateFinished,true);
			if (isEInk) Utils.setSolidButtonEink(btnStateFinished);
		} else btnStateFinished.setBackgroundColor(colorGrayCT);
	}

	private void refreshCurrentDir() {
		activity.showDirectory(dir, "");
	}

	public BrowserViewLayout(BaseActivity context, FileBrowser contentView, CRToolBar toolbar, View titleView) {
		super(context);
		this.activity = (CoolReader) context;
		this.contentView = contentView;
		this.titleView = titleView;
		this.titleView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		this.toolbarView = toolbar;
		this.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		this.addView(titleView);
		this.addView(toolbarView);
		this.addView(contentView);
		bStateDir = activity.settings().getBool(Settings.PROP_APP_SHOW_FILES_DIR, true);
		bStateNoMark = activity.settings().getBool(Settings.PROP_APP_SHOW_FILES_NO_MARK, true);
		bStateToRead = activity.settings().getBool(Settings.PROP_APP_SHOW_FILES_TO_READ, true);
		bStateReading = activity.settings().getBool(Settings.PROP_APP_SHOW_FILES_READING, true);
		bStateFinished = activity.settings().getBool(Settings.PROP_APP_SHOW_FILES_FINISHED, true);
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors((CoolReader) activity, isEInk);
		LayoutInflater inflater = LayoutInflater.from(activity);
		bottomBar = (LinearLayout) inflater.inflate(R.layout.browser_bottom_bar, null);
		bottomBar1Btn = (LinearLayout) inflater.inflate(R.layout.browser_bottom_bar_1btn, null);
		bottomBarLitres = (LinearLayout) inflater.inflate(R.layout.browser_bottom_bar_litres, null);
		btnNew = bottomBarLitres.findViewById(R.id.btn_new);
		btnPop = bottomBarLitres.findViewById(R.id.btn_pop);
		Drawable imgP = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable imgP1 = imgP.getConstantState().newDrawable().mutate();
		Drawable imgP2 = imgP.getConstantState().newDrawable().mutate();
		btnNew.setCompoundDrawablesWithIntrinsicBounds(imgP1, null, null, null);
		btnPop.setCompoundDrawablesWithIntrinsicBounds(imgP2, null, null, null);
		setNewOrPopChecked(null);
		btnNew.setOnClickListener(v -> setNewOrPopChecked(btnNew));
		btnPop.setOnClickListener(v -> setNewOrPopChecked(btnPop));
		bottomBar.addView(bottomBar1Btn);
		ImageButton btnMenu = bottomBar1Btn.findViewById(R.id.btn_show_menu);
		if (btnMenu != null) {
			btnMenu.setOnClickListener(v -> toolbarView.showOverflowMenu());
		}
		ImageButton btnScrollUp = bottomBar1Btn.findViewById(R.id.btn_scroll_up);

		if (btnScrollUp != null) {
			btnScrollUp.setOnClickListener(v -> {
					int firstVisiblePosition = contentView.mListView.getFirstVisiblePosition();
					int lastVisiblePosition = contentView.mListView.getLastVisiblePosition();
					int diff = lastVisiblePosition - firstVisiblePosition;
					if (isEInk)
						contentView.mListView.setSelection(firstVisiblePosition - ((diff / 4) * 3));
					else
						contentView.mListView.smoothScrollToPosition(firstVisiblePosition - ((diff / 4) * 3));
				}
			);
		}
		ImageButton btnScrollDown = bottomBar1Btn.findViewById(R.id.btn_scroll_down);
		if (btnScrollDown != null) {
			btnScrollDown.setOnClickListener(v -> {
					int firstVisiblePosition = contentView.mListView.getFirstVisiblePosition();
					int lastVisiblePosition = contentView.mListView.getLastVisiblePosition();
					int diff = lastVisiblePosition - firstVisiblePosition;
					if (isEInk)
						contentView.mListView.setSelection(lastVisiblePosition+ ((diff/4) * 3));
					else
						contentView.mListView.smoothScrollToPosition(lastVisiblePosition+ ((diff/4) * 3));
				}
			);
		}
		ImageButton btnMenu2 = bottomBarLitres.findViewById(R.id.btn_show_menu);
		if (btnMenu2 != null)
			btnMenu2.setOnClickListener(v -> toolbarView.showOverflowMenu());
		this.addView(bottomBar);

		this.onThemeChanged(context.getCurrentTheme());
		titleView.setFocusable(false);
		titleView.setFocusableInTouchMode(false);
		toolbarView.setFocusable(false);
		toolbarView.setFocusableInTouchMode(false);
		contentView.setFocusable(false);
		contentView.setFocusableInTouchMode(false);
		setFocusable(true);
		setFocusableInTouchMode(true);
		activity.tintViewIcons(contentView);
		activity.tintViewIcons(toolbarView);
		activity.tintViewIcons(titleView);
		activity.tintViewIcons(bottomBar);
		activity.tintViewIcons(bottomBar1Btn);
		activity.tintViewIcons(bottomBarLitres);
	}
	
	private String browserTitle = "";
	private FileInfo dir = null;
	private ArrayList<TextView> arrLblPaths = new ArrayList<TextView>();

	private void switchFilter(boolean filt) {
		final LinearLayout ll_path = titleView.findViewById(R.id.ll_path);
		LayoutInflater inflater = LayoutInflater.from(activity);
		if (filt) {
			filterIsShown = true;
			ll_path.removeAllViews();
			filterView = inflater.inflate(R.layout.browser_status_bar_filter, null);
			ll_path.addView(filterView);
			filterView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT));
			EditText filter_edit = filterView.findViewById(R.id.filter_edit);
			filter_edit.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
					contentView.filterUpdated(cs.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

				@Override
				public void afterTextChanged(Editable arg0) {}
			});

			filter_edit.requestFocus();
			BackgroundThread.instance().postGUI(() -> BackgroundThread.instance()
					.postBackground(() -> BackgroundThread.instance()
							.postGUI(() -> KeyboardUtils.showKeyboard(activity))), 200);
		} else {
			filterIsShown = false;
			KeyboardUtils.hideKeyboard(activity, filterView);
			if (filterView!=null) ll_path.removeView(filterView);
			ll_path.removeAllViews();
			for (TextView tv : arrLblPaths) {
				if (tv!=null)
					ll_path.addView(tv);
			}
		}
	}

	public void setBrowserBottomBar(boolean isLitres) {
		bottomBar.removeAllViews();
		if (!isLitres)
			bottomBar.addView(bottomBar1Btn);
		else {
			bottomBar.addView(bottomBarLitres);
			setNewOrPopChecked(null);
		}
	}

	public void setBrowserTitle(String title, FileInfo dir) {
		this.browserTitle = title;

		btnStateDir  = titleView.findViewById(R.id.book_state_dir);
		btnStateDir.setOnClickListener(v -> {
			bStateDir = !bStateDir;
			paintStateButtons();
			Properties props = new Properties(activity.settings());
			props.setProperty(Settings.PROP_APP_SHOW_FILES_DIR, bStateDir?"1":"0");
			activity.setSettings(props, -1, true);
			refreshCurrentDir();
		});
		btnStateNoMark  = titleView.findViewById(R.id.book_state_nomark);
		btnStateNoMark.setOnClickListener(v -> {
			bStateNoMark = !bStateNoMark;
			paintStateButtons();
			Properties props = new Properties(activity.settings());
			props.setProperty(Settings.PROP_APP_SHOW_FILES_NO_MARK, bStateNoMark?"1":"0");
			activity.setSettings(props, -1, true);
			refreshCurrentDir();
		});
		btnStateToRead  = titleView.findViewById(R.id.book_state_toread);
		btnStateToRead.setOnClickListener(v -> {
			bStateToRead = !bStateToRead;
			paintStateButtons();
			Properties props = new Properties(activity.settings());
			props.setProperty(Settings.PROP_APP_SHOW_FILES_TO_READ, bStateToRead?"1":"0");
			activity.setSettings(props, -1, true);
			refreshCurrentDir();
		});
		btnStateReading  = titleView.findViewById(R.id.book_state_reading);
		btnStateReading.setOnClickListener(v -> {
			bStateReading = !bStateReading;
			paintStateButtons();
			Properties props = new Properties(activity.settings());
			props.setProperty(Settings.PROP_APP_SHOW_FILES_READING, bStateReading?"1":"0");
			activity.setSettings(props, -1, true);
			refreshCurrentDir();
		});
		btnStateFinished = titleView.findViewById(R.id.book_state_finished);
		btnStateFinished.setOnClickListener(v -> {
			bStateFinished = !bStateFinished;
			paintStateButtons();
			Properties props = new Properties(activity.settings());
			props.setProperty(Settings.PROP_APP_SHOW_FILES_FINISHED, bStateFinished?"1":"0");
			activity.setSettings(props, -1, true);
			refreshCurrentDir();
		});
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		Drawable img3 = img.getConstantState().newDrawable().mutate();
		Drawable img4 = img.getConstantState().newDrawable().mutate();
		Drawable img5 = img.getConstantState().newDrawable().mutate();
		btnStateDir.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		btnStateNoMark.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		btnStateToRead.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
		btnStateReading.setCompoundDrawablesWithIntrinsicBounds(img4, null, null, null);
		btnStateFinished.setCompoundDrawablesWithIntrinsicBounds(img5, null, null, null);

		int colorIcon = themeColors.get(R.attr.colorIcon);
		int colorBlue = themeColors.get(R.attr.colorThemeBlue);
		int colorGreen = themeColors.get(R.attr.colorThemeGreen);
		int colorGray = themeColors.get(R.attr.colorThemeGray);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);

		btnStateDir.setTextColor(activity.getTextColor(colorIcon));
		btnStateNoMark.setTextColor(activity.getTextColor(colorIcon));
		int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
//		btnStateDir.setBackgroundColor(colorGrayCT);
//		btnStateNoMark.setBackgroundColor(colorGrayCT);
//		btnStateToRead.setBackgroundColor(colorGrayCT);
//		btnStateReading.setBackgroundColor(colorGrayCT);
//		btnStateFinished.setBackgroundColor(colorGrayCT);
		paintStateButtons();
		btnStateReading.setTextColor(colorGreen);
		btnStateToRead.setTextColor(colorBlue);
		btnStateFinished.setTextColor(colorGray);

		if (filterIsShown) switchFilter(false);
		if (dir!=null) this.dir = dir;
		((TextView)titleView.findViewById(R.id.title)).setText(title);
		arrLblPaths.clear();
		arrLblPaths.add(titleView.findViewById(R.id.path1));
		arrLblPaths.add(titleView.findViewById(R.id.path2));
		arrLblPaths.add(titleView.findViewById(R.id.path3));
		arrLblPaths.add(titleView.findViewById(R.id.path4));
		arrLblPaths.add(titleView.findViewById(R.id.path5));
		arrLblPaths.add(titleView.findViewById(R.id.path6));
		arrLblPaths.add(titleView.findViewById(R.id.path7));
		arrLblPaths.add(titleView.findViewById(R.id.path8));
		arrLblPaths.add(titleView.findViewById(R.id.path9));
		arrLblPaths.add(titleView.findViewById(R.id.path10));

		int i = 0;
		FileInfo dir1 = this.dir;
		FileInfo dir2 = dir1;
		if (!filterIsShown)
			for (TextView tv : arrLblPaths) {
				i++;
				if (dir2!=null) dir2 = dir2.parent;
				tv.setText("");
				tv.setTextColor(activity.getTextColor(colorIcon));
				if ((dir2 != null)&&(!dir2.isRootDir())) {
					tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
					final FileInfo dir3 = dir2;
					tv.setText(FileInfo.getDisplayName(activity, String.valueOf(dir2.getFilename())));
					tv.setOnClickListener(v -> activity.showDirectory(dir3, ""));
				} else
					if (i==1) {
						if (this.dir != null)
							if (this.dir.isOPDSDir()) {
								tv.setPaintFlags(0);
								tv.setText(activity.getString(R.string.downloaded_from_catalog) +
										" " + this.dir.book_downloaded);
								tv.setOnClickListener(v -> {
								});
							}
					}
			}
		ImageButton btnQpNext = (ImageButton)titleView.findViewById(R.id.btn_qp_next1);
		if (DeviceInfo.isEinkScreen(activity.getScreenForceEink()))
			if (btnQpNext != null)
				btnQpNext.setOnClickListener(v -> {
					int firstVisiblePosition = contentView.mListView.getFirstVisiblePosition();
					int lastVisiblePosition = contentView.mListView.getLastVisiblePosition();
					int diff = lastVisiblePosition - firstVisiblePosition;
					if (isEInk)
						contentView.mListView.setSelection(lastVisiblePosition+ ((diff/4) * 3));
					else
						contentView.mListView.smoothScrollToPosition(lastVisiblePosition+ ((diff/4) * 3));
				});
		activity.tintViewIcons(btnQpNext,true);
		ImageButton btnQpPrev = (ImageButton)titleView.findViewById(R.id.btn_qp_prev1);
		if (DeviceInfo.isEinkScreen(activity.getScreenForceEink()))
			if (btnQpPrev != null)
				btnQpPrev.setOnClickListener(v -> {
					int firstVisiblePosition = contentView.mListView.getFirstVisiblePosition();
					int lastVisiblePosition = contentView.mListView.getLastVisiblePosition();
					int diff = lastVisiblePosition - firstVisiblePosition;
					if (isEInk)
						contentView.mListView.setSelection(firstVisiblePosition-((diff/4) * 3));
					else
						contentView.mListView.smoothScrollToPosition(firstVisiblePosition-((diff/4) * 3));
				});
		activity.tintViewIcons(btnQpPrev,true);
		LinearLayout llButtons = titleView.findViewById(R.id.llButtons);
		if (!DeviceInfo.isEinkScreen(activity.getScreenForceEink())) {
			llButtons.removeView(btnQpNext);
			llButtons.removeView(btnQpPrev);
		}
		if (titleView.findViewById(R.id.btn_filter) != null) {
			((ImageButton) titleView.findViewById(R.id.btn_filter)).setOnClickListener(v -> switchFilter(!filterIsShown));
			activity.tintViewIcons(titleView.findViewById(R.id.btn_filter), true);
		}
	}

	private boolean progressStatusEnabled = false;
	public void setBrowserProgressStatus(boolean enable) {
		progressStatusEnabled = enable;
		ProgressBar progressBar = titleView.findViewById(R.id.progress);
		progressBar.setVisibility((enable && (!isEInk)) ? View.VISIBLE : View.GONE);
	}

	public void onThemeChanged(InterfaceTheme theme) {
		//titleView.setBackgroundResource(theme.getBrowserStatusBackground());
		//toolbarView.setButtonAlpha(theme.getToolbarButtonAlpha());
		LayoutInflater inflater = LayoutInflater.from(activity);// activity.getLayoutInflater();
		removeView(titleView);
		titleView = inflater.inflate(R.layout.browser_status_bar, null);
		addView(titleView);
		setBrowserTitle(browserTitle, null);
		setBrowserProgressStatus(progressStatusEnabled);
		toolbarView.setBackgroundResource(theme.getBrowserToolbarBackground(toolbarView.isVertical()));
		toolbarView.onThemeChanged(theme);
		requestLayout();
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		r -= l;
		b -= t;
		t = 0;
		l = 0;

		int full_l = l;
		int full_r = r;
		int full_t = t;
		int full_b = b;

		int marg = activity.settings().getInt(Settings.PROP_GLOBAL_MARGIN, 0);
		l = l + marg;
		r = r - marg;
		t = t + marg;
		b = b - marg;

		int titleHeight = titleView.getMeasuredHeight();
		int contentHeight = contentView.getMeasuredHeight();
		int bottomHeight = bottomBar.getMeasuredHeight();
		if (toolbarView.isVertical()) {
			int tbWidth = toolbarView.getMeasuredWidth();
			titleView.layout(l + tbWidth, t, r, t + titleHeight);
			toolbarView.layout(l, t, l + tbWidth, b);
			contentView.layout(l + tbWidth, t + titleHeight, r, b);
			//bottomBar.layout(l + tbWidth, t + titleHeight + contentHeight, r, b);
			bottomBar.layout(l + tbWidth, b - bottomHeight, r, b);
			toolbarView.setBackgroundResource(activity.getCurrentTheme().getBrowserToolbarBackground(true));
		} else {
			int tbHeight = toolbarView.getMeasuredHeight();
			toolbarView.layout(l, t, r, t + tbHeight);
			titleView.layout(l, t + tbHeight, r, t + titleHeight + tbHeight);
			contentView.layout(l, t + titleHeight + tbHeight, r, b);
			//bottomBar.layout(l, t + titleHeight + tbHeight + contentHeight, r, b);
			bottomBar.layout(l, b - bottomHeight, r, b);
			toolbarView.setBackgroundResource(activity.getCurrentTheme().getBrowserToolbarBackground(false));
		}
	}

	private static int prevH = 0;
	private static int prevW = 0;
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);

		//boolean v = toolbarView.isVertical();
		boolean v = w > h;
		boolean needChange = true;

		if ((prevH!=0) && (prevW!=0)) {
			if ((prevW==w) && (prevH == h)) {
				needChange = false;
			}
		}

		if (needChange) v = w > h;

		prevH = h;
		prevW = w;
		
		toolbarView.setVertical(v);
		if (v) {
			// landscape
			//toolbarView.setVertical(true);
			toolbarView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
			int tbWidth = toolbarView.getMeasuredWidth();
			//titleView.measure(MeasureSpec.makeMeasureSpec(w - tbWidth, MeasureSpec.AT_MOST),
			//		MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			titleView.measure(widthMeasureSpec - tbWidth,
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			int titleHeight = titleView.getMeasuredHeight();
			bottomBar.measure(MeasureSpec.makeMeasureSpec(w - tbWidth, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
			int bottomHeight = bottomBar.getMeasuredHeight();
			contentView.measure(MeasureSpec.makeMeasureSpec(w - tbWidth, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h - titleHeight - bottomHeight, MeasureSpec.AT_MOST));
		} else {
			// portrait
			//toolbarView.setVertical(false);
			toolbarView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
			int tbHeight = toolbarView.getMeasuredHeight();
			titleView.measure(widthMeasureSpec, 
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			int titleHeight = titleView.getMeasuredHeight();
			bottomBar.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
			int bottomHeight = bottomBar.getMeasuredHeight();
			try {
				contentView.measure(widthMeasureSpec,
						MeasureSpec.makeMeasureSpec(h - titleHeight - tbHeight - bottomHeight, MeasureSpec.AT_MOST));
			} catch (Exception e) {

			}
		}
        setMeasuredDimension(w, h);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}


	private long menuDownTs = 0;
	private long backDownTs = 0;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
			//L.v("BrowserViewLayout.onKeyDown(" + keyCode + ")");
			if (event.getRepeatCount() == 0)
				menuDownTs = Utils.timeStamp();
			return true;
		}
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			//L.v("BrowserViewLayout.onKeyDown(" + keyCode + ")");
			if (event.getRepeatCount() == 0)
				backDownTs = Utils.timeStamp();
			return true;
		}
		if (contentView.onKeyDown(keyCode, event))
			return true;
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
			long duration = Utils.timeInterval(menuDownTs);
			L.v("BrowserViewLayout.onKeyUp(" + keyCode + ") duration = " + duration);
			if (duration > 700 && duration < 10000)
				activity.showBrowserOptionsDialog();
			else
				toolbarView.showOverflowMenu();
			return true;
		}
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			long duration = Utils.timeInterval(backDownTs);
			L.v("BrowserViewLayout.onKeyUp(" + keyCode + ") duration = " + duration);
			if (duration > 700 && duration < 10000) {
				activity.finish();
			} else {
				contentView.showParentDirectory();
			}
			return true;
		}
		if (contentView.onKeyUp(keyCode, event))
			return true;
		return super.onKeyUp(keyCode, event);
	}


}


