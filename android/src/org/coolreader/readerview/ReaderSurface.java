package org.coolreader.readerview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceView;

import org.coolreader.CoolReader;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.Utils;
import org.coolreader.options.OptionsDialog;
import org.coolreader.options.SelectionModesOption;

public class ReaderSurface extends SurfaceView implements ReaderView.BookView {

	public static final Logger log = L.create("rvrs", Log.VERBOSE);

	final ReaderView mReaderView;
	final CoolReader mActivity;

	public ReaderSurface(ReaderView rv, Context context) {
		super(context);
		mReaderView = rv;
		mActivity = rv.getActivity();
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
		if (mReaderView.mSettings.getBool(Settings.PROP_APP_TRACKBALL_DISABLED, false)) {
			log.d("trackball is disabled in settings");
			return true;
		}
		mActivity.onUserActivity();
		return super.onTrackballEvent(event);
	}

	@Override
	protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
		log.i("onSizeChanged(" + w + ", " + h + ")" + " activity.isDialogActive=" + mActivity.isDialogActive());
		super.onSizeChanged(w, h, oldw, oldh);
		mReaderView.requestResize(w, h);
	}

	@Override
	public void onWindowVisibilityChanged(int visibility) {
		if (visibility == VISIBLE) {
			if (DeviceInfo.isEinkScreen(mActivity.getScreenForceEink()))
				mReaderView.mEinkScreen.refreshScreen(mReaderView.surface);
			mReaderView.startStats();
			mReaderView.checkSize();
		} else
			mReaderView.stopStats();
		super.onWindowVisibilityChanged(visibility);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (hasWindowFocus) {
			if (DeviceInfo.isEinkScreen(mActivity.getScreenForceEink()))
				BackgroundThread.instance().postGUI(() -> mReaderView.mEinkScreen.refreshScreen(mReaderView.surface), 400);
			mReaderView.startStats();
			mReaderView.checkSize();
		} else
			mReaderView.stopStats();
		super.onWindowFocusChanged(hasWindowFocus);
	}

	protected void doDraw(Canvas canvas)
	{
		try {

			log.d("doDraw() called");
			if (mReaderView.isProgressActive()) {
				log.d("onDraw() -- drawing progress " + (mReaderView.currentProgressPosition / 100));
				mReaderView.drawPageBackground(canvas);
				mReaderView.doDrawProgress(canvas, mReaderView.currentProgressPosition, mReaderView.currentProgressTitle);
			} else if (mReaderView.mInitialized && mReaderView.mCurrentPageInfo != null && mReaderView.mCurrentPageInfo.bitmap != null) {
				log.d("onDraw() -- drawing page image");

				if (mReaderView.currentAutoScrollAnimation != null) {
					mReaderView.currentAutoScrollAnimation.draw(canvas);
				} else if (mReaderView.currentAnimation != null) {
					mReaderView.currentAnimation.draw(canvas);
				} else {
					Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
					Rect src = new Rect(0, 0, mReaderView.mCurrentPageInfo.bitmap.getWidth(), mReaderView.mCurrentPageInfo.bitmap.getHeight());
					if (mReaderView.dontStretchWhileDrawing) {
						if (dst.right > src.right)
							dst.right = src.right;
						if (dst.bottom > src.bottom)
							dst.bottom = src.bottom;
						if (src.right > dst.right)
							src.right = dst.right;
						if (src.bottom > dst.bottom)
							src.bottom = dst.bottom;
						if (mReaderView.centerPageInsteadOfResizing) {
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
					mReaderView.drawDimmedBitmap(canvas, mReaderView.mCurrentPageInfo.bitmap, src, dst);
				}
				if (mReaderView.isCloudSyncProgressActive()) {
					// draw progressbar on top
					mReaderView.doDrawCloudSyncProgress(canvas, mReaderView.currentCloudSyncProgressPosition);
				}
			} else {
				log.d("onDraw() -- drawing empty screen");
				mReaderView.drawPageBackground(canvas);
				if (mReaderView.isCloudSyncProgressActive()) {
					// draw progressbar on top
					mReaderView.doDrawCloudSyncProgress(canvas, mReaderView.currentCloudSyncProgressPosition);
				}
			}
			if (mReaderView.selectionModeActive) {
				Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
				int textColor = mReaderView.mSettings.getColor(Settings.PROP_FONT_COLOR, 0x000000);
				int newTextSize = 12;
				float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
						newTextSize, getResources().getDisplayMetrics());
				String sText = "";
				int title =  SelectionModesOption.getSelectionActionTitle(mReaderView.mSelection2Action == -1 ?
						mReaderView.mSelectionAction : mReaderView.mSelection2Action);
				if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mReaderView.mSettings, mActivity, true);
				sText = sText + " / ";
				title =  SelectionModesOption.getSelectionActionTitle(mReaderView.mMultiSelection2Action == -1 ?
						mReaderView.mMultiSelectionAction : mReaderView.mMultiSelection2Action);
				if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mReaderView.mSettings, mActivity, true);
				//sText = sText + " / ";
				//title =  OptionsDialog.getSelectionActionTitle(mSelection2ActionLong == -1 ? mSelectionActionLong : mSelection2ActionLong);
				//if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mSettings, mActivity, true);
				Utils.drawFrame2(canvas, dst, Utils.createSolidPaint(0xC0000000 | textColor), 4,
						textSize, sText);
			}
			if (mReaderView.inspectorModeActive) {
				Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
				int textColor = mReaderView.mSettings.getColor(Settings.PROP_FONT_COLOR, 0x000000);
				int newTextSize = 12;
				float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
						newTextSize, getResources().getDisplayMetrics());
				String sText = "";
				int title =  SelectionModesOption.getSelectionActionTitle(mReaderView.mSelection3Action == -1 ?
						mReaderView.mSelectionAction : mReaderView.mSelection3Action);
				if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mReaderView.mSettings, mActivity, true);
				sText = sText + " / ";
				title =  SelectionModesOption.getSelectionActionTitle(mReaderView.mMultiSelection3Action == -1 ?
						mReaderView.mMultiSelectionAction : mReaderView.mMultiSelection3Action);
				if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mReaderView.mSettings, mActivity, true);
				//sText = sText + " / ";
				//title =  OptionsDialog.getSelectionActionTitle(mSelection3ActionLong == -1 ? mSelectionActionLong : mSelection3ActionLong);
				//if (title != 0) sText = sText + OptionsDialog.updDicValue(mActivity.getString(title), mSettings, mActivity, true);
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
			if (mReaderView.isProgressActive()) {
				log.d("onDrawBlack() -- drawing progress " + (mReaderView.currentProgressPosition / 100));
				mReaderView.drawBlackPageBackground(canvas);
				mReaderView.doDrawProgress(canvas, mReaderView.currentProgressPosition, mReaderView.currentProgressTitle);
			} else if (mReaderView.mInitialized && mReaderView.mCurrentPageInfo != null && mReaderView.mCurrentPageInfo.bitmap != null) {
				log.d("onDrawBlack() -- drawing page image");

				if (mReaderView.currentAutoScrollAnimation != null) {
					mReaderView.currentAutoScrollAnimation.draw(canvas);
					return;
				}

				if (mReaderView.currentAnimation != null) {
					mReaderView.currentAnimation.draw(canvas);
					return;
				}
				//canvas.drawColor(Color.rgb(32, 32, 32));
				mReaderView.drawBlackPageBackground(canvas);
			} else {
				log.d("onDrawBlack() -- drawing empty screen");
				mReaderView.drawBlackPageBackground(canvas);
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
		mReaderView.drawCallback(this::doDraw, null, isPartially);
	}

	@Override
	public void draw(boolean isPartially, boolean isBlack) {
		mReaderView.drawCallback(this::doDrawBlack, null, isPartially);
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

}
