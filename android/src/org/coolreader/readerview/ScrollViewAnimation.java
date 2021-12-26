package org.coolreader.readerview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.ReaderCommand;

public class ScrollViewAnimation extends ViewAnimationBase {
	int startY;
	int maxY;
	int pageHeight;
	int fullHeight;
	int pointerStartPos;
	int pointerDestPos;
	int pointerCurrPos;
	BitmapInfo image1;
	BitmapInfo image2;

	final CoolReader mActivity;

	public static final Logger log = L.create("rvsva", Log.VERBOSE);
	public static final Logger alog = L.create("rvsva", Log.WARN);
	ScrollViewAnimation(ReaderView rv, int startY, int maxY) {
		super(rv);
		mReaderView = rv;
		mActivity = rv.getActivity();
		this.startY = startY;
		this.maxY = maxY;
		long start = android.os.SystemClock.uptimeMillis();
		log.v("ScrollViewAnimation -- creating: drawing two pages to buffer");
		PositionProperties currPos = mReaderView.doc.getPositionProps(null, false);
		int pos = currPos.y;
		int pos0 = pos - (maxY - startY);
		if (pos0 < 0)
			pos0 = 0;
		pointerStartPos = pos;
		pointerCurrPos = pos;
		pointerDestPos = startY;
		pageHeight = currPos.pageHeight;
		fullHeight = currPos.fullHeight;
		mReaderView.doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, pos0);
		image1 = mReaderView.preparePageImage(0);
		if (image1 == null) {
			log.v("ScrollViewAnimation -- not started: image is null");
			return;
		}
		image2 = mReaderView.preparePageImage(image1.position.pageHeight);
		mReaderView.doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, pos);
		if (image2 == null) {
			log.v("ScrollViewAnimation -- not started: image is null");
			return;
		}
		long duration = android.os.SystemClock.uptimeMillis() - start;
		log.v("ScrollViewAnimation -- created in " + duration + " millis");
		mReaderView.currentAnimation = this;
	}

	@Override
	public void stop(int x, int y) {
		if (mReaderView.currentAnimation == null)
			return;
		//if ( started ) {
		if (y != -1) {
			int delta = startY - y;
			pointerCurrPos = pointerStartPos + delta;
		}
		if (pointerCurrPos < 0)
			pointerCurrPos = 0;
		if (pointerCurrPos > fullHeight - pageHeight)
			pointerCurrPos = fullHeight - pageHeight;
		pointerDestPos = pointerCurrPos;
		draw();
		mReaderView.doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, pointerDestPos);
		//}
		mReaderView.scheduleSaveCurrentPositionBookmark(mReaderView.getDefSavePositionInterval());
		close();
	}

	@Override
	public void move(int duration, boolean accelerated) {
		if (duration > 0  && mReaderView.getPageFlipAnimationSpeedMs() != 0) {
			int steps = (int)(duration / mReaderView.getAvgAnimationDrawDuration()) + 2;
			//log.i("STEPS: " + steps);
			int x0 = pointerCurrPos;
			int x1 = pointerDestPos;
			if ((x0 - x1) < 10 && (x0 - x1) > -10)
				steps = 2;
			for (int i = 1; i < steps; i++) {
				int x = x0 + (x1-x0) * i / steps;
				pointerCurrPos = accelerated ? mReaderView.accelerate( x0, x1, x ) : x;
				if (pointerCurrPos < 0)
					pointerCurrPos = 0;
				if (pointerCurrPos > fullHeight - pageHeight)
					pointerCurrPos = fullHeight - pageHeight;
				draw();
			}
		}
		pointerCurrPos = pointerDestPos;
		draw();
	}

	@Override
	public void update(int x, int y) {
		int delta = startY - y;
		pointerDestPos = pointerStartPos + delta;
		if (pointerDestPos < 0)
			pointerDestPos = 0;
		if (pointerDestPos > fullHeight - pageHeight)
			pointerDestPos = fullHeight - pageHeight;
	}

	@Override
	public void animate()
	{
		//log.d("animate() is called");
		if (pointerDestPos != pointerCurrPos) {
			if (!started)
				started = true;
			if (mReaderView.getPageFlipAnimationSpeedMs() == 0)
				pointerCurrPos = pointerDestPos;
			else {
				int delta = pointerCurrPos-pointerDestPos;
				if (delta < 0)
					delta = -delta;
				long avgDraw = mReaderView.getAvgAnimationDrawDuration();
				//int maxStep = (int)(maxY * PAGE_ANIMATION_DURATION / avgDraw);
				int maxStep = mReaderView.getPageFlipAnimationSpeedMs() > 0 ? (int)(maxY * 1000 / avgDraw / mReaderView.getPageFlipAnimationSpeedMs()) : maxY;
				int step;
				if (delta > maxStep * 2)
					step = maxStep;
				else
					step = (delta + 3) / 4;
				//int step = delta<3 ? 1 : (delta<5 ? 2 : (delta<10 ? 3 : (delta<15 ? 6 : (delta<25 ? 10 : (delta<50 ? 15 : 30)))));
				if (pointerCurrPos < pointerDestPos)
					pointerCurrPos += step;
				else
					pointerCurrPos -= step;
				log.d("animate("+pointerCurrPos + " => " + pointerDestPos + "  step=" + step + ")");
			}
			//pointerCurrPos = pointerDestPos;
			draw();
			if (pointerDestPos != pointerCurrPos)
				mReaderView.scheduleAnimation();
		}
	}

	public void draw(Canvas canvas)
	{
//			BitmapInfo image1 = mCurrentPageInfo;
//			BitmapInfo image2 = mNextPageInfo;
		if (image1 == null || image1.isReleased() || image2 == null || image2.isReleased())
			return;
		int h = image1.position.pageHeight;
		int rowsFromImg1 = image1.position.y + h - pointerCurrPos;
		int rowsFromImg2 = h - rowsFromImg1;
		Rect src1 = new Rect(0, h-rowsFromImg1, mReaderView.mCurrentPageInfo.bitmap.getWidth(), h);
		Rect dst1 = new Rect(0, 0, mReaderView.mCurrentPageInfo.bitmap.getWidth(), rowsFromImg1);
		mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
		if (image2 != null) {
			Rect src2 = new Rect(0, 0, mReaderView.mCurrentPageInfo.bitmap.getWidth(), rowsFromImg2);
			Rect dst2 = new Rect(0, rowsFromImg1, mReaderView.mCurrentPageInfo.bitmap.getWidth(), h);
			mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
		}
		//log.v("anim.drawScroll( pos=" + pointerCurrPos + ", " + src1 + "=>" + dst1 + ", " + src2 + "=>" + dst2 + " )");
	}
}
