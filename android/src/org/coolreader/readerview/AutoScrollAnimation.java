package org.coolreader.readerview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.ReaderCommand;
import org.coolreader.utils.Utils;

public class AutoScrollAnimation {

	public static final Logger log = L.create("rvasa", Log.VERBOSE);
	public static final Logger alog = L.create("rvasa", Log.WARN);

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
	public final static int ANIMATION_INTERVAL_EINK = 1000;

	final ReaderView mReaderView;
	final CoolReader mActivity;

	boolean mIsSimple = false;

	public AutoScrollAnimation(ReaderView rv, final int startProgress, boolean isSimple) {
		mReaderView = rv;
		mActivity = rv.getActivity();
		progress = startProgress;
		mIsSimple = isSimple;
		startAnimationProgress = mReaderView.AUTOSCROLL_START_ANIMATION_PERCENT * 100;
		mReaderView.currentAutoScrollAnimation = this;

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
				mReaderView.currentAutoScrollAnimation = null;
			}
		});
	}

	private int calcProgressPercent() {
		long duration = Utils.timeInterval(pageTurnStart);
		long estimatedFullDuration = 60000 * charCount / mReaderView.autoScrollSpeed;
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
		if (mIsSimple) {
			alog.v("currentSimpleAutoScrollTick(), onTimer(simple)");
			//BackgroundThread.instance().executeGUI(() -> {
			mReaderView.currentSimpleAutoScrollTick();
			//});
			mReaderView.scheduleGc();
			return true;
		}
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
			if (mReaderView.currentAutoScrollAnimation != AutoScrollAnimation.this) {
				log.v("timer is cancelled - GUI");
				return;
			}
			BackgroundThread.instance().postBackground(() -> {
				if (mReaderView.currentAutoScrollAnimation != AutoScrollAnimation.this) {
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
		if (mIsSimple) return true;
		mReaderView.cancelGc();
		log.v("initPageTurn(startProgress = " + startProgress + ")");
		pageTurnStart = Utils.timeStamp();
		progress = startProgress;
		currPos = mReaderView.doc.getPositionProps(null, true);
		charCount = currPos.charCount;
		pageCount = currPos.pageMode;
		if (charCount < 150)
			charCount = 150;
		isScrollView = currPos.pageMode == 0;
		log.v("initPageTurn(charCount = " + charCount + ")");
		if (isScrollView) {
			image1 = mReaderView.preparePageImage(0);
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
			image2 = mReaderView.preparePageImage(pos1 - pos0);
			if (image2 == null) {
				log.v("ScrollViewAnimation -- not started: image is null");
				return false;
			}
		} else {
			int page1 = currPos.pageNumber;
			int page2 = currPos.pageNumber + 1;
			if (page2 < 0 || page2 >= currPos.pageCount) {
				mReaderView.currentAnimation = null;
				return false;
			}
			image1 = mReaderView.preparePageImage(0);
			image2 = mReaderView.preparePageImage(1);
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
		mReaderView.currentAutoScrollAnimation = this;
		draw();
		return true;
	}


	private boolean donePageTurn(boolean turnPage) {
		if (mIsSimple) return true;
		log.v("donePageTurn()");
		if (turnPage) {
			if (isScrollView)
				mReaderView.doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, nextPos);
			else
				mReaderView.doc.doCommand(ReaderCommand.DCMD_PAGEDOWN.nativeId, 1);
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
		try {
			mReaderView.drawCallback(this::draw, null, isPartially);
		} catch (Exception e) {
			log.w("Cannot draw page - " + e.getMessage());
		}
	}

	public void stop() {
		mReaderView.currentAutoScrollAnimation = null;
		BackgroundThread.instance().executeBackground(() -> {
			donePageTurn(wantPageTurn());
			//redraw();
			mReaderView.drawPage(null, false);
			mReaderView.scheduleSaveCurrentPositionBookmark(mReaderView.getDefSavePositionInterval());
		});
		mReaderView.scheduleGc();
	}

	private boolean wantPageTurn() {
		return (progress > (startAnimationProgress + MAX_PROGRESS) / 2);
	}

	private void drawGradient(Canvas canvas, Rect rc, Paint[] paints, int startIndex, int endIndex ) {
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
			mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
		}
		if (d < h) {
			Rect src2 = new Rect(src.left, src.top + d, src.right, src.bottom);
			Rect dst2 = new Rect(dst.left, dst.top + d, dst.right, dst.bottom);
			mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src2, dst2);
		}
		if (scrollPercent > 0 && scrollPercent < 10000) {
			Rect shadowRect = new Rect(src.left, src.top + div, src.right, src.top + div + shadowHeight);
			drawShadow(canvas, shadowRect);
		}
	}

	public void draw(Canvas canvas) {
		if (mReaderView.currentAutoScrollAnimation != this)
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
