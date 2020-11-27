package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.dic.Dictionaries;
import org.coolreader.R;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SelectionToolbarDlg {
	PopupWindow mWindow;
	View mAnchor;
	CoolReader mCoolReader;
	ReaderView mReaderView;
	View mPanel;
	Selection selection;

	static public void showDialog( CoolReader coolReader, ReaderView readerView, final Selection selection )
	{
		SelectionToolbarDlg dlg = new SelectionToolbarDlg(coolReader, readerView, selection);
		//dlg.mWindow.update(dlg.mAnchor, width, height)
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.showAtLocation(readerView, Gravity.LEFT|Gravity.TOP, readerView.getLeft()+50, readerView.getTop()+50);
		//dlg.showAsDropDown(readerView);
		//dlg.update();
	}

	private boolean pageModeSet = false;
	private boolean changedPageMode;
	private void setReaderMode()
	{
		if (pageModeSet)
			return;
		//if (DeviceInfo.EINK_SCREEN) { return; } // switching to scroll view doesn't work well on eink screens
		
		String oldViewSetting = mReaderView.getSetting( ReaderView.PROP_PAGE_VIEW_MODE );
		if ( "1".equals(oldViewSetting) ) {
			changedPageMode = true;
			mReaderView.setViewModeNonPermanent(ViewMode.SCROLL);
		}
		pageModeSet = true;
	}
	
	private void restoreReaderMode()
	{
		if ( changedPageMode ) {
			mReaderView.setViewModeNonPermanent(ViewMode.PAGES);
		}
	}
	
	private void changeSelectionBound(boolean start, int delta) {
		L.d("changeSelectionBound(" + (start?"start":"end") + ", " + delta + ")");
		setReaderMode();
		ReaderCommand cmd = start ? ReaderCommand.DCMD_SELECT_MOVE_LEFT_BOUND_BY_WORDS : ReaderCommand.DCMD_SELECT_MOVE_RIGHT_BOUND_BY_WORDS;
		mReaderView.moveSelection(cmd, delta, new ReaderView.MoveSelectionCallback() {
			
			@Override
			public void onNewSelection(Selection selection) {
				Log.d("cr3", "onNewSelection: " + selection.text);
				SelectionToolbarDlg.this.selection = selection;
			}
			
			@Override
			public void onFail() {
				Log.d("cr3", "fail()");
				//currentSelection = null;
			}
		});
	}
	
	private final static int SELECTION_CONTROL_STEP = 10;
	private final static int SELECTION_SMALL_STEP = 1;
	private final static int SELECTION_NEXT_SENTENCE_STEP = 999;
	private class BoundControlListener implements OnSeekBarChangeListener {

		public BoundControlListener(SeekBar sb, boolean start) {
			this.start = start;
			this.sb = sb;
			sb.setOnSeekBarChangeListener(this);
		}
		final boolean start;
		final SeekBar sb;
		int lastProgress = 50;
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			sb.setProgress(50);
			lastProgress = 50;
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			sb.setProgress(50);
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (!fromUser)
				return;
			int diff = (progress - lastProgress) / SELECTION_CONTROL_STEP * SELECTION_CONTROL_STEP;
			if (diff!=0) {
				lastProgress += diff;
				changeSelectionBound(start, diff/SELECTION_CONTROL_STEP);
			}
		}
	};
	
	private void closeDialog(boolean clearSelection) {
		if (clearSelection)
			mReaderView.clearSelection();
		restoreReaderMode();
		mWindow.dismiss();
	}

	public SelectionToolbarDlg(CoolReader coolReader, ReaderView readerView, Selection sel )
	{
		this.selection = sel;
		mCoolReader = coolReader;
		mReaderView = readerView;
		mAnchor = readerView.getSurface();

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar, null));
		panel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		//mReaderView.getS
		
		mWindow = new PopupWindow( mAnchor.getContext() );

		mWindow.setTouchInterceptor((v, event) -> {
			if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
				closeDialog(true);
				return true;
			}
			return false;
		});
		//super(panel);
		int colorGrayC;
		int colorGray;
		int colorIcon;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorGray = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.BLACK);
		a.recycle();


		ColorDrawable c = new ColorDrawable(colorGrayC);
		c.setAlpha(130);
		//mWindow.setBackgroundDrawable(c);

		mPanel = panel;
		panel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));

		//mPanel.findViewById(R.id.selection_copy).setBackgroundColor(colorGrayC);
		mPanel.findViewById(R.id.selection_copy).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_copy).setOnClickListener(v -> {
			mReaderView.copyToClipboard(selection.text);
			closeDialog(true);
		});

		//recent dics
		LinearLayout llRecentDics = mPanel.findViewById(R.id.recentDics);
		Properties props = new Properties(mCoolReader.settings());
		int newTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
		int iCntRecent = 0;
		if (llRecentDics!=null) {
			for (final Dictionaries.DictInfo di : mCoolReader.mDictionaries.diRecent) {
				if (!di.equals(mCoolReader.mDictionaries.getCurDictionary())) {
					iCntRecent++;
				}
			}
		}
		if (iCntRecent == 0) iCntRecent++;
		if (llRecentDics!=null) {
			for (final Dictionaries.DictInfo di: mCoolReader.mDictionaries.diRecent) {
				if (!di.equals(mCoolReader.mDictionaries.getCurDictionary())) {
					Button dicButton = new Button(mCoolReader);
					dicButton.setText(di.name);
					dicButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
					//dicButton.setHeight(dicButton.getHeight()-4);
					dicButton.setTextColor(colorIcon);
					dicButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
					dicButton.setPadding(1, 1, 1, 1);
					//dicButton.setBackground(null);
					LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					llp.setMargins(4, 1, 1, 4);
					dicButton.setLayoutParams(llp);
					dicButton.setMaxWidth((mReaderView.getRequestedWidth()-20) / iCntRecent);
					dicButton.setMaxLines(1);
					dicButton.setEllipsize(TextUtils.TruncateAt.END);
					llRecentDics.addView(dicButton);
					dicButton.setOnClickListener(v -> {
						mCoolReader.mDictionaries.setAdHocDict(di);
						String sSText = selection.text;
						mCoolReader.findInDictionary(sSText, null);
						if (!mReaderView.getSettings().getBool(mReaderView.PROP_APP_SELECTION_PERSIST, false))
							mReaderView.clearSelection();
						closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
					});
				}
			}
		}

		mPanel.findViewById(R.id.selection_dict).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_dict).setOnClickListener(v -> {
			//PositionProperties currpos = mReaderView.getDoc().getPositionProps(null);
			//Log.e("CURPOS", currpos.pageText);
			if (mCoolReader.ismDictLongtapChange()) {
				DictsDlg dlg = new DictsDlg(mCoolReader, mReaderView, selection.text, null);
				dlg.show();
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			} else {
				mCoolReader.findInDictionary( selection.text , null);
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			}
		});

		mPanel.findViewById(R.id.selection_dict).setOnLongClickListener(v -> {
			if (!mCoolReader.ismDictLongtapChange()) {
				DictsDlg dlg = new DictsDlg(mCoolReader, mReaderView, selection.text, null);
				dlg.show();
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			} else {
				mCoolReader.findInDictionary( selection.text , null);
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			}
			return true;
		});

		mPanel.findViewById(R.id.selection_bookmark).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_bookmark).setOnClickListener(v -> {
			mReaderView.showNewBookmarkDialog(selection,Bookmark.TYPE_COMMENT, "");
			closeDialog(true);
		});

		mPanel.findViewById(R.id.selection_bookmark).setOnLongClickListener(v -> {
			BookmarksDlg dlg = new BookmarksDlg(mCoolReader, mReaderView, false, null);
			dlg.show();
			closeDialog(true);
			return true;
		});

		mPanel.findViewById(R.id.selection_email).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_email).setOnClickListener(v -> {
			mReaderView.sendQuotationInEmail(selection);
			closeDialog(true);
		});
		mPanel.findViewById(R.id.selection_find).setOnClickListener(v -> {
			mReaderView.showSearchDialog(selection.text.trim());
			closeDialog(true);
		});

		mPanel.findViewById(R.id.selection_find).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_find).setOnLongClickListener(v -> {
			final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
			emailIntent.putExtra(SearchManager.QUERY, selection.text.trim());
			mCoolReader.startActivity(emailIntent);
			closeDialog(true);
			return true;
		});
		mPanel.findViewById(R.id.selection_cancel).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_cancel).setOnClickListener(v -> closeDialog(true));
		new BoundControlListener(mPanel.findViewById(R.id.selection_left_bound_control), true);
		new BoundControlListener(mPanel.findViewById(R.id.selection_right_bound_control), false);

		mPanel.findViewById(R.id.btn_next1).setOnClickListener(v -> changeSelectionBound(true, SELECTION_SMALL_STEP));
		mPanel.findViewById(R.id.btn_next2).setOnClickListener(v -> changeSelectionBound(false, SELECTION_SMALL_STEP));
		mPanel.findViewById(R.id.btn_prev1).setOnClickListener(v -> changeSelectionBound(true, -SELECTION_SMALL_STEP));
		mPanel.findViewById(R.id.btn_prev2).setOnClickListener(v -> changeSelectionBound(false, -SELECTION_SMALL_STEP));
		mPanel.findViewById(R.id.btn_next_sent1).setOnClickListener(v -> changeSelectionBound(true, SELECTION_NEXT_SENTENCE_STEP));
		mPanel.findViewById(R.id.btn_next_sent2).setOnClickListener(v -> changeSelectionBound(false, SELECTION_NEXT_SENTENCE_STEP));
		mPanel.findViewById(R.id.btn_prev_sent1).setOnClickListener(v -> changeSelectionBound(true, -SELECTION_NEXT_SENTENCE_STEP));
		mPanel.findViewById(R.id.btn_prev_sent2).setOnClickListener(v -> changeSelectionBound(false, -SELECTION_NEXT_SENTENCE_STEP));
		mPanel.setFocusable(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if ( event.getAction()==KeyEvent.ACTION_UP ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_BACK:
					closeDialog(true);
					return true;
//					case KeyEvent.KEYCODE_DPAD_LEFT:
//					case KeyEvent.KEYCODE_DPAD_UP:
//						//mReaderView.findNext(pattern, true, caseInsensitive);
//						return true;
//					case KeyEvent.KEYCODE_DPAD_RIGHT:
//					case KeyEvent.KEYCODE_DPAD_DOWN:
//					*	//mReaderView.findNext(pattern, false, caseInsensitive);
//						return true;
				}
			} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
					switch ( keyCode ) {
//						case KeyEvent.KEYCODE_BACK:
//						case KeyEvent.KEYCODE_DPAD_LEFT:
//						case KeyEvent.KEYCODE_DPAD_UP:
//						case KeyEvent.KEYCODE_DPAD_RIGHT:
//						case KeyEvent.KEYCODE_DPAD_DOWN:
//							return true;
					}
				}
			if ( keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
			return false;
		});

		mWindow.setOnDismissListener(() -> {
			restoreReaderMode();
			mReaderView.clearSelection();
		});
		
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		//mWindow.setAnimationStyle(android.R.style.Animation_Toast);
		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
//		setWidth(panel.getWidth());
//		setHeight(panel.getHeight());
		
		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		mWindow.setContentView(panel);
		
		
		int [] location = new int[2];
		mAnchor.getLocationOnScreen(location);
		//mWindow.update(location[0], location[1], mPanel.getWidth(), mPanel.getHeight() );
		//mWindow.setWidth(mPanel.getWidth());
		//mWindow.setHeight(mPanel.getHeight());

		int popupY = location[1] + mAnchor.getHeight() - mPanel.getHeight();
		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
//		if ( mWindow.isShowing() )
//			mWindow.update(mAnchor, 50, 50);
		//dlg.mWindow.showAsDropDown(dlg.mAnchor);
		int y = sel.startY;
		if (y > sel.endY)
			y = sel.endY;
		int maxy = mReaderView.getSurface().getHeight() * 4 / 5;
		//plotn - since we have a transparent toolbar - this is not nessesary
//		if (y > maxy) {
//			setReaderMode(); // selection is overlapped by toolbar: set scroll mode and move
//			BackgroundThread.instance().postGUI(new Runnable() {
//				@Override
//				public void run() {
//					//mReaderView.doEngineCommand(ReaderCommand.DCMD_REQUEST_RENDER, 0);
//					BackgroundThread.instance().postBackground(new Runnable() {
//						@Override
//						public void run() {
//							BackgroundThread.instance().postGUI(new Runnable() {
//								@Override
//								public void run() {
//									mReaderView.doEngineCommand(ReaderCommand.DCMD_SCROLL_BY, mReaderView.getSurface().getHeight() / 3);
//									mReaderView.redraw();
//								}
//							});
//						}
//					});
//				}
//			});
//		}
		mCoolReader.tintViewIcons(mPanel);
	}
	
}
