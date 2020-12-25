package org.coolreader.crengine;

import org.coolreader.R;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

public class FindNextDlg {
	PopupWindow mWindow;
	View mAnchor;
	int mAnchorHeight = 0;
	//CoolReader mCoolReader;
	ReaderView mReaderView;
	View mPanel;
	final String pattern;
	final boolean caseInsensitive;
	static public void showDialog( BaseActivity coolReader, ReaderView readerView, final String pattern, final boolean caseInsensitive, final boolean skim )
	{
		FindNextDlg dlg = new FindNextDlg(coolReader, readerView, pattern, caseInsensitive, skim);
		//dlg.mWindow.update(dlg.mAnchor, width, height)
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.showAtLocation(readerView, Gravity.LEFT|Gravity.TOP, readerView.getLeft()+50, readerView.getTop()+50);
		//dlg.showAsDropDown(readerView);
		//dlg.update();
	}
	public FindNextDlg( BaseActivity coolReader, ReaderView readerView, final String pattern, final boolean caseInsensitive, final boolean skim )
	{
		this.pattern = pattern;
		this.caseInsensitive = caseInsensitive;
		//mCoolReader = coolReader;
		mReaderView = readerView;
		mAnchor = readerView.getSurface();
		mAnchorHeight = mAnchor.getHeight();

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.search_popup, null));
		panel.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		//mReaderView.getS
		
		mWindow = new PopupWindow( mAnchor.getContext() );
		mWindow.setTouchInterceptor((v, event) -> {
			if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
				mReaderView.clearSelection();
				mWindow.dismiss();
				return true;
			}
			return false;
		});
		//super(panel);
		mReaderView.mBookInfo.sortBookmarks();
		mPanel = panel;
		mPanel.findViewById(R.id.search_btn_prev).setOnClickListener(v -> mReaderView.findNext(pattern, true, caseInsensitive));
		mPanel.findViewById(R.id.search_btn_next).setOnClickListener(v -> mReaderView.findNext(pattern, false, caseInsensitive));
		mPanel.findViewById(R.id.search_btn_plus_1).setOnClickListener(v -> mReaderView.onCommand(ReaderCommand.DCMD_PAGEDOWN, 1));
		mPanel.findViewById(R.id.search_btn_plus_10).setOnClickListener(v -> mReaderView.onCommand(ReaderCommand.DCMD_PAGEDOWN, 10));
		mPanel.findViewById(R.id.search_btn_minus_1).setOnClickListener(v -> mReaderView.onCommand(ReaderCommand.DCMD_PAGEUP, 1));
		mPanel.findViewById(R.id.search_btn_minus_10).setOnClickListener(v -> mReaderView.onCommand(ReaderCommand.DCMD_PAGEUP, 10));
		mPanel.findViewById(R.id.search_btn_plus_ch).setOnClickListener(v -> mReaderView.onCommand(ReaderCommand.DCMD_MOVE_BY_CHAPTER, 1));
		mPanel.findViewById(R.id.search_btn_plus_ch).setOnLongClickListener(v ->
				{
					mReaderView.goToPage(mReaderView.getDoc().getPageCount());
					return true;
				}
		);
		mPanel.findViewById(R.id.search_btn_minus_ch).setOnClickListener(v -> mReaderView.onCommand(ReaderCommand.DCMD_MOVE_BY_CHAPTER, -1));
		mPanel.findViewById(R.id.search_btn_minus_ch).setOnLongClickListener(v ->
				{
					mReaderView.goToPage(1);
					return true;
				}
		);
		mPanel.findViewById(R.id.search_btn_plus_bmk).setOnClickListener(v -> mReaderView.onCommand(ReaderCommand.DCMD_NEXT_BOOKMARK, 1));
		mPanel.findViewById(R.id.search_btn_minus_bmk).setOnClickListener(v -> mReaderView.onCommand(ReaderCommand.DCMD_PREV_BOOKMARK, -1));

		int colorGrayC;
		int colorGray;
		TypedArray a = mReaderView.getActivity().getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorGray = a.getColor(1, Color.GRAY);
		a.recycle();

		ColorDrawable c = new ColorDrawable(colorGrayC);
		c.setAlpha(130);
		mPanel.findViewById(R.id.search_btn_prev).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_prev).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_plus_1).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_plus_1).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_plus_10).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_plus_10).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_plus_ch).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_plus_ch).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_plus_bmk).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_plus_bmk).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_next).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_next).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_minus_1).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_minus_1).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_minus_10).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_minus_10).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_minus_ch).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_minus_ch).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_minus_bmk).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.search_btn_minus_bmk).setPadding(6, 15, 6, 15);

		if (skim) {
			Utils.hideView(mPanel.findViewById(R.id.search_btn_prev));
			Utils.hideView(mPanel.findViewById(R.id.search_btn_next));
		}

		mPanel.findViewById(R.id.search_btn_close).setOnClickListener(v -> {
			mReaderView.clearSelection();
			mWindow.dismiss();
		});
		mPanel.findViewById(R.id.search_btn_close).setBackgroundDrawable(c);
		coolReader.tintViewIcons(mPanel,true);
		mPanel.setFocusable(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if ( event.getAction()==KeyEvent.ACTION_UP ) {
				switch ( keyCode ) {
					case KeyEvent.KEYCODE_BACK:
						mReaderView.clearSelection();
						mWindow.dismiss();
						return true;
					case KeyEvent.KEYCODE_DPAD_LEFT:
					case KeyEvent.KEYCODE_DPAD_UP:
						mReaderView.findNext(pattern, true, caseInsensitive);
						return true;
					case KeyEvent.KEYCODE_DPAD_RIGHT:
					case KeyEvent.KEYCODE_DPAD_DOWN:
						mReaderView.findNext(pattern, false, caseInsensitive);
						return true;
				}
			} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
				switch ( keyCode ) {
					case KeyEvent.KEYCODE_BACK:
					case KeyEvent.KEYCODE_DPAD_LEFT:
					case KeyEvent.KEYCODE_DPAD_UP:
					case KeyEvent.KEYCODE_DPAD_RIGHT:
					case KeyEvent.KEYCODE_DPAD_DOWN:
						return true;
				}
			}
			return keyCode == KeyEvent.KEYCODE_BACK;
		});

		mWindow.setOnDismissListener(() -> mReaderView.clearSelection());
		
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		//mWindow.setAnimationStyle(android.R.style.Animation_Toast);
		mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
//		setWidth(panel.getWidth());
//		setHeight(panel.getHeight());
		
		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		panel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
		mWindow.setContentView(panel);
		
		
		//int [] location = new int[2];
		//mAnchor.getLocationOnScreen(location);
		//mWindow.update(location[0], location[1], mPanel.getWidth(), mPanel.getHeight() );
		//mWindow.setWidth(mPanel.getWidth());
		//mWindow.setHeight(mPanel.getHeight());
		mWindow.setWidth(mReaderView.getSurface().getWidth());
		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL,
				mReaderView.locationFindNext[0],
				mAnchorHeight - mPanel.getHeight());
				//mReaderView.locationFindNext[1] + mReaderView.heightFindNext - mPanel.getHeight());
//		if ( mWindow.isShowing() )
//			mWindow.update(mAnchor, 50, 50);
		//dlg.mWindow.showAsDropDown(dlg.mAnchor);
	
	}
	
}
