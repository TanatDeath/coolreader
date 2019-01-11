package org.coolreader.crengine;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.coolreader.CoolReader;
import org.coolreader.R;

public class OrientationToolbarDlg {
	PopupWindow mWindow;
	View mAnchor;
	CoolReader mCoolReader;
	ReaderView mReaderView;
	View mPanel;
	private Handler mHandler = new Handler();

	private Runnable handleDismiss = new Runnable() {
		@Override
		public void run() {
			if (mWindow != null) {
				mWindow.dismiss();
			}
		}
	};

	static public OrientationToolbarDlg showDialog( CoolReader coolReader, ReaderView readerView,
													int curOrientation, boolean isShortMode)
	{
		OrientationToolbarDlg dlg = new OrientationToolbarDlg(coolReader, readerView, curOrientation, isShortMode);
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		return dlg;
	}

	private boolean pageModeSet;
	private boolean changedPageMode;
	private void setReaderMode()
	{
		if (pageModeSet)
			return;

		String oldViewSetting = mReaderView.getSetting( ReaderView.PROP_PAGE_VIEW_MODE );
		if ( "1".equals(oldViewSetting) ) {
			changedPageMode = true;
			mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE, "0");
		}
		pageModeSet = true;
	}

	private void restoreReaderMode()
	{
		if ( changedPageMode ) {
			mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE, "1");
		}
	}

	private void closeDialog() {
		mWindow.dismiss();
	}

	public String getOrientationString(int orient) {
		String ornt = "0";
		if (orient == DeviceOrientation.ORIENTATION_PORTRAIT) ornt = "0";
		if (orient == DeviceOrientation.ORIENTATION_LANDSCAPE) ornt = "1";
		if (orient == DeviceOrientation.ORIENTATION_PORTRAIT_REVERSE) ornt = "2";
		if (orient == DeviceOrientation.ORIENTATION_LANDSCAPE_REVERSE) ornt = "3";
		return ornt;
	}

	public OrientationToolbarDlg(CoolReader coolReader, ReaderView readerView, int curOrientation, boolean isShortMode)
	{
		mCoolReader = coolReader;
		mReaderView = readerView;
		mAnchor = readerView.getSurface();

		View panel = null;
		if (!isShortMode) panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.orientation_toolbar, null));
		else panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.orientation_toolbar_short, null));
		panel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		mWindow = new PopupWindow( mAnchor.getContext() );

		mWindow.setTouchInterceptor(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
					closeDialog();
					return true;
				}
				return false;
			}
		});
		int colorGrayC;
		int colorGray;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorGray = a.getColor(1, Color.GRAY);
		a.recycle();

		ColorDrawable c = new ColorDrawable(colorGrayC);
		c.setAlpha(130);

		mPanel = panel;
		panel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));

		ImageButton btnFixNew = mPanel.findViewById(R.id.fix_new_orientation);
		boolean isLandscape =
				(curOrientation == DeviceOrientation.ORIENTATION_LANDSCAPE) ||
				(curOrientation == DeviceOrientation.ORIENTATION_LANDSCAPE_REVERSE);
		if (isLandscape)
			btnFixNew.setImageResource(
				Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_icons8_fix_landsc, R.drawable.icons8_fix_landsc)
			);
		else
			btnFixNew.setImageResource(
					Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_icons8_fix_port, R.drawable.icons8_fix_port)
			);
		btnFixNew.setBackgroundDrawable(c);
		btnFixNew.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (DeviceInfo.getSDKLevel() >= 9) {
					int iSett = mCoolReader.settings().getInt(Settings.PROP_APP_SCREEN_ORIENTATION, 0);
					Properties props1 = new Properties(mCoolReader.settings());
					String ornt = getOrientationString(mCoolReader.sensorCurRot);
					props1.setProperty(Settings.PROP_APP_SCREEN_ORIENTATION, ornt);
					mCoolReader.setSettings(props1, -1, true);
					closeDialog();
				}
			}
		});

		ImageButton btnFixOld = mPanel.findViewById(R.id.fix_old_orientation);
		if (btnFixOld!=null) {
			if (isLandscape)
				btnFixOld.setImageResource(
						Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_icons8_landsc_to_port, R.drawable.icons8_landsc_to_port)
				);
			else
				btnFixOld.setImageResource(
						Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_icons8_port_to_landsc, R.drawable.icons8_port_to_landsc)
				);

			btnFixOld.setBackgroundDrawable(c);
			btnFixOld.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (DeviceInfo.getSDKLevel() >= 9) {
						int iSett = mCoolReader.settings().getInt(Settings.PROP_APP_SCREEN_ORIENTATION, 0);
						Properties props1 = new Properties(mCoolReader.settings());
						String ornt = getOrientationString(mCoolReader.sensorPrevRot);
						props1.setProperty(Settings.PROP_APP_SCREEN_ORIENTATION, ornt);
						mCoolReader.setSettings(props1, -1, true);
						closeDialog();
					}
				}
			});
		}

		ImageButton btnDisablePopup = mPanel.findViewById(R.id.disable_popup_toolbar);
		btnDisablePopup.setBackgroundDrawable(c);
		btnDisablePopup.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Properties props1 = new Properties(mCoolReader.settings());
				props1.setProperty(Settings.PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION, "0");
				mCoolReader.setSettings(props1, -1, true);
				closeDialog();
			}
		});

		ImageButton btnSett = mPanel.findViewById(R.id.btn_orientation_settings);
		btnSett.setBackgroundDrawable(c);
		btnSett.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mCoolReader.optionsFilter = "";
				mCoolReader.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_APP_SCREEN_ORIENTATION);
				closeDialog();
			}
		});

		mPanel.findViewById(R.id.orientation_cancel).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.orientation_cancel).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				closeDialog();
			}
		});

		mPanel.setFocusable(true);
		mPanel.setOnKeyListener( new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ( event.getAction()==KeyEvent.ACTION_UP ) {
					switch ( keyCode ) {
					case KeyEvent.KEYCODE_BACK:
						closeDialog();
						return true;
					}
				} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
						switch ( keyCode ) {
						}
					}
				if ( keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
				return false;
			}
			
		});

		mWindow.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				restoreReaderMode();
				mReaderView.clearSelection();
			}
		});
		
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		mWindow.setContentView(panel);
		
		
		int [] location = new int[2];
		mAnchor.getLocationOnScreen(location);

		int popupY = location[1] + mAnchor.getHeight() - mPanel.getHeight();
		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
		mCoolReader.tintViewIcons(mPanel);
		int dur =mCoolReader.settings().getInt(Settings.PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION,10);
		mHandler.postDelayed(handleDismiss,dur==0?7000:dur*1000);
	}
	
}
