package org.coolreader.readerview;

import android.graphics.Color;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.coolreader.CoolReader;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.ImageInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;

public class ImageViewer extends GestureDetector.SimpleOnGestureListener {

	public static final Logger log = L.create("rviv", Log.VERBOSE);

	private ImageInfo currentImage;
	final GestureDetector detector;
	int oldOrientation;

	final CoolReader mActivity;
	final ReaderView mReaderView;

	public ImageViewer(CoolReader activity, ReaderView rv, ImageInfo image) {
		currentImage = image;
		mActivity = activity;
		mReaderView = rv;
		lockOrientation();
		detector = new GestureDetector(this);
		if (image.bufHeight / image.height >= 2 && image.bufWidth / image.width >= 2) {
			image.scaledHeight *= 2;
			image.scaledWidth *= 2;
		}
		centerIfLessThanScreen(image);
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
			mReaderView.drawPage();
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

	private final static int STATE_INITIAL = 0; // no events yet
	private final static int STATE_DOWN_1 = 1; // down first time
	private final static int STATE_TWO_POINTERS = 9; // two finger events
	private final static int STATE_TWO_POINTERS_FONT_SCALE = 12;  // Font scaling

	int state = STATE_INITIAL;
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

	int begDistance = 0;
	int lastAval = 0;

	public boolean onTouchEvent(MotionEvent event) {
		int index = event.getActionIndex();
		int x = (int)event.getX();
		int y = (int)event.getY();
		width = mReaderView.surface.getWidth();
		height = mReaderView.surface.getHeight();
		int minSize = mReaderView.lastsetWidth;
		if (height < minSize) minSize = height;
		int pinchTreshold = (minSize / TapHandler.PINCH_TRESHOLD_COUNT) * 2;
		if ((event.getPointerCount() > 1) && (state == STATE_DOWN_1) && (!mReaderView.mDisableTwoPointerGestures) &&
				(!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))) {
			begDistance = 0;
			lastAval = 0;
			state = STATE_TWO_POINTERS;
			x = (int) event.getX(index);
			y = (int) event.getY(index);
			if (index == 1) {
				start_x2 = x;
				start_y2 = y;
			}
			if (index == 0) {
				start_x = x;
				start_y = y;
			}
			// lets detect pinch type
			state = STATE_TWO_POINTERS_FONT_SCALE;
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN) state = STATE_DOWN_1;
		else if (event.getAction() == MotionEvent.ACTION_UP) state = STATE_INITIAL;
		else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (event.getPointerCount()>1) {
				now_x = (int)event.getX(0);
				now_y = (int)event.getY(0);
				now_x2 = (int)event.getX(1);
				now_y2 = (int)event.getY(1);
			} else {
				now_x = x;
				now_y = y;
			}
			if (event.getPointerCount()>1) {
				int distance1 = (int) (Math.sqrt(Math.abs(start_x2 - start_x) * Math.abs(start_x2 - start_x) +
						Math.abs(start_y2 - start_y) * Math.abs(start_y2 - start_y)));
				int distance2 = (int) (Math.sqrt(Math.abs(now_x2 - now_x) * Math.abs(now_x2 - now_x) +
						Math.abs(now_y2 - now_y) * Math.abs(now_y2 - now_y)));
				int aval = distance1 - distance2;
				if (begDistance == 0) {
					begDistance = aval;
					lastAval = aval;
				}
//							Log.i(TAG, "onTouchEvent distance1 : " + distance1);
//							Log.i(TAG, "onTouchEvent distance2 : " + distance2);
//							Log.i(TAG, "onTouchEvent aval : " + aval);
//							Log.i(TAG, "onTouchEvent begDistance : " + begDistance);
				//aval = aval / pinchTreshold;
				if (Math.abs(lastAval - aval)> pinchTreshold) {
					if (aval < begDistance) zoomIn();
					if (aval > begDistance) zoomOut();
					lastAval = aval;
				}
			}
		}
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
		if (mReaderView.currentImageViewer == null)
			return;
		mReaderView.currentImageViewer = null;
		Properties settings = mReaderView.getSettings();
		unlockOrientation();
		BackgroundThread.instance().postBackground(() -> mReaderView.doc.closeImage());
		boolean bgWas = settings.getBool(Settings.PROP_BACKGROUND_COLOR_SAVE_WAS, false);
		if (bgWas) {
			int col = settings.getColor(Settings.PROP_BACKGROUND_COLOR_SAVE, Color.BLACK);
			String tx = settings.getProperty(Settings.PROP_PAGE_BACKGROUND_IMAGE_SAVE, "(NONE)");
			settings.setColor(Settings.PROP_BACKGROUND_COLOR, col);
			settings.setProperty(Settings.PROP_PAGE_BACKGROUND_IMAGE, tx);
			settings.setBool(Settings.PROP_BACKGROUND_COLOR_SAVE_WAS, false);
			mActivity.setSettings(settings, 2000, true);
		}
		mReaderView.drawPage();
		if (mActivity.getmReaderFrame() != null)
			BackgroundThread.instance().postGUI(() -> {
				mActivity.getmReaderFrame().updateCRToolbar(((CoolReader) mActivity));
			}, 300);
	}

	public BitmapInfo prepareImage() {
		// called from background thread
		ImageInfo img = currentImage;
		img.bufWidth = mReaderView.internalDX;
		img.bufHeight = mReaderView.internalDY;
		if (mReaderView.mCurrentPageInfo != null) {
			if (img.equals(mReaderView.mCurrentPageInfo.imageInfo))
				return mReaderView.mCurrentPageInfo;
			mReaderView.mCurrentPageInfo.recycle();
			mReaderView.mCurrentPageInfo = null;
		}
		PositionProperties currpos = mReaderView.doc.getPositionProps(null, false);
		BitmapInfo bi = new BitmapInfo(mReaderView.factory);
		bi.imageInfo = new ImageInfo(img);
		bi.bitmap = mReaderView.factory.get(mReaderView.internalDX, mReaderView.internalDY);
		bi.position = currpos;
		mReaderView.doc.drawImage(bi.bitmap, bi.imageInfo);
		mReaderView.mCurrentPageInfo = bi;
		return mReaderView.mCurrentPageInfo;
	}

}
