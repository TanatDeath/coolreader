package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class BrowserViewLayout extends ViewGroup {
	private BaseActivity activity;
	private FileBrowser contentView;
	private View titleView;
	private CRToolBar toolbarView;

	public BrowserViewLayout(BaseActivity context, FileBrowser contentView, CRToolBar toolbar, View titleView) {
		super(context);
		this.activity = context;
		this.contentView = contentView;
		this.titleView = titleView;
		this.titleView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		this.toolbarView = toolbar;
		this.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		this.addView(titleView);
		this.addView(toolbarView);
		this.addView(contentView);
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
	}
	
	private String browserTitle = "";
	private FileInfo dir = null;
	private ArrayList<TextView> arrLblPaths = new ArrayList<TextView>();

	public void setBrowserTitle(String title, FileInfo dir) {
		this.browserTitle = title;
		if (dir!=null) this.dir = dir;
		((TextView)titleView.findViewById(R.id.title)).setText(title);
		arrLblPaths.clear();
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path1));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path2));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path3));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path4));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path5));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path6));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path7));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path8));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path9));
		arrLblPaths.add((TextView)titleView.findViewById(R.id.path10));
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorIcon});
		int colorIcon = a.getColor(0, Color.GRAY);
		a.recycle();

		int i = 0;
		FileInfo dir1 = this.dir;
		FileInfo dir2 = dir1;
		for (TextView tv : arrLblPaths) {
			i++;
			if (dir2!=null) dir2 = dir2.parent;
			tv.setText(String.valueOf(""));
			tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			if ((dir2 != null)&&(!dir2.isRootDir())) {
				final FileInfo dir3 = dir2;
				tv.setText(String.valueOf(dir2.filename));
				tv.setTextColor(colorIcon);
				tv.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						((CoolReader)activity).showDirectory(dir3);
					}
				});
			}
		}
		((ImageButton)titleView.findViewById(R.id.btn_qp_next1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				int firstVisiblePosition = contentView.mListView.getFirstVisiblePosition();
				int lastVisiblePosition = contentView.mListView.getLastVisiblePosition();
				int diff = lastVisiblePosition - firstVisiblePosition;
				contentView.mListView.smoothScrollToPosition(lastVisiblePosition+ ((diff/4) * 3));
			}
		});
		activity.tintViewIcons(((ImageButton)titleView.findViewById(R.id.btn_qp_next1)),true);
		((ImageButton)titleView.findViewById(R.id.btn_qp_prev1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				int firstVisiblePosition = contentView.mListView.getFirstVisiblePosition();
				int lastVisiblePosition = contentView.mListView.getLastVisiblePosition();
				int diff = lastVisiblePosition - firstVisiblePosition;
				contentView.mListView.smoothScrollToPosition(firstVisiblePosition-((diff/4) * 3));
			}
		});
		activity.tintViewIcons(((ImageButton)titleView.findViewById(R.id.btn_qp_prev1)),true);
	}

	public void onThemeChanged(InterfaceTheme theme) {
		//titleView.setBackgroundResource(theme.getBrowserStatusBackground());
		//toolbarView.setButtonAlpha(theme.getToolbarButtonAlpha());
		LayoutInflater inflater = LayoutInflater.from(activity);// activity.getLayoutInflater();
		removeView(titleView);
		titleView = inflater.inflate(R.layout.browser_status_bar, null);
		addView(titleView);
		setBrowserTitle(browserTitle, null);
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
		int titleHeight = titleView.getMeasuredHeight();
		if (toolbarView.isVertical()) {
			int tbWidth = toolbarView.getMeasuredWidth();
			titleView.layout(l + tbWidth, t, r, t + titleHeight);
			toolbarView.layout(l, t, l + tbWidth, b);
			contentView.layout(l + tbWidth, t + titleHeight, r, b);
			toolbarView.setBackgroundResource(activity.getCurrentTheme().getBrowserToolbarBackground(true));
		} else {
			int tbHeight = toolbarView.getMeasuredHeight();
			toolbarView.layout(l, t, r, t + tbHeight);
			titleView.layout(l, t + tbHeight, r, t + titleHeight + tbHeight);
			contentView.layout(l, t + titleHeight + tbHeight, r, b);
			toolbarView.setBackgroundResource(activity.getCurrentTheme().getBrowserToolbarBackground(false));
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);

		
		toolbarView.setVertical(w > h);
		if (w > h) {
			// landscape
			toolbarView.setVertical(true);
			toolbarView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
			int tbWidth = toolbarView.getMeasuredWidth();
			titleView.measure(MeasureSpec.makeMeasureSpec(w - tbWidth, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			int titleHeight = titleView.getMeasuredHeight();
			contentView.measure(MeasureSpec.makeMeasureSpec(w - tbWidth, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h - titleHeight, MeasureSpec.AT_MOST));
		} else {
			// portrait
			toolbarView.setVertical(false);
			toolbarView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
			int tbHeight = toolbarView.getMeasuredHeight();
			titleView.measure(widthMeasureSpec, 
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			int titleHeight = titleView.getMeasuredHeight();
			contentView.measure(widthMeasureSpec, 
					MeasureSpec.makeMeasureSpec(h - titleHeight - tbHeight, MeasureSpec.AT_MOST));
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
				return true;
			} else {
				contentView.showParentDirectory();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}


}


