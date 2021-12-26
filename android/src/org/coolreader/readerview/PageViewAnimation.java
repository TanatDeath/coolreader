package org.coolreader.readerview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.ReaderCommand;
import org.coolreader.graphics.FastBlur;

public class PageViewAnimation extends ViewAnimationBase {

	int startX;
	int maxX;
	int page1;
	int page2;
	int direction;
	int currShift;
	int destShift;
	int pageCount;
	Paint divPaint;
	Paint[] shadePaints;
	Paint[] hilitePaints;
	int pageFlipAnimationM;

	BitmapInfo image1;
	BitmapInfo image2;
	Bitmap image1scaled;
	Bitmap image2scaled;

	final CoolReader mActivity;

	public static final Logger log = L.create("rvpva", Log.VERBOSE);
	public static final Logger alog = L.create("rvpva", Log.WARN);

	PageViewAnimation(ReaderView rv, int startX, int maxX, int direction) {
		super(rv);
		mReaderView = rv;
		mActivity = rv.getActivity();
		this.startX = startX;
		this.maxX = maxX;
		this.direction = direction;
		this.currShift = 0;
		this.destShift = 0;
		this.pageFlipAnimationM = mReaderView.pageFlipAnimationMode;

		long start = android.os.SystemClock.uptimeMillis();
		log.v("PageViewAnimation -- creating: drawing two pages to buffer");

		PositionProperties currPos = mReaderView.mCurrentPageInfo == null ? null : mReaderView.mCurrentPageInfo.position;
		if (currPos == null)
			currPos = mReaderView.doc.getPositionProps(null, false);
		page1 = currPos.pageNumber;
		page2 = currPos.pageNumber + direction;
		if (page2 < 0 || page2 >= currPos.pageCount) {
			mReaderView.currentAnimation = null;
			return;
		}
		this.pageCount = currPos.pageMode;
		image1 = mReaderView.preparePageImage(0);
		image1scaled = null;
		if (image1!=null)
			image1scaled = Bitmap.createScaledBitmap(
					image1.bitmap, image1.bitmap.getWidth()/4, image1.bitmap.getHeight()/4, false);
		image2 = mReaderView.preparePageImage(direction);
		image2scaled = null;
		if (image2!=null)
			image2scaled = Bitmap.createScaledBitmap(
					image2.bitmap, image2.bitmap.getWidth()/4, image2.bitmap.getHeight()/4, false);
		if (image1 == null || image2 == null) {
			log.v("PageViewAnimation -- cannot start animation: page image is null");
			return;
		}
		if (page1 == page2) {
			log.v("PageViewAnimation -- cannot start animation: not moved");
			return;
		}
		page2 = image2.position.pageNumber;
		mReaderView.currentAnimation = this;
		divPaint = new Paint();
		divPaint.setStyle(Paint.Style.FILL);
		divPaint.setColor(mActivity.isNightMode() ? Color.argb(96, 64, 64, 64) : Color.argb(128, 128, 128, 128));
		final int numPaints = 16;
		shadePaints = new Paint[numPaints];
		hilitePaints = new Paint[numPaints];
		for (int i=0; i<numPaints; i++) {
			shadePaints[i] = new Paint();
			hilitePaints[i] = new Paint();
			hilitePaints[i].setStyle(Paint.Style.FILL);
			shadePaints[i].setStyle(Paint.Style.FILL);
			if (mActivity.isNightMode()) {
				shadePaints[i].setColor(Color.argb((i+1)*96 / numPaints, 0, 0, 0));
				hilitePaints[i].setColor(Color.argb((i+1)*96 / numPaints, 64, 64, 64));
			} else {
				shadePaints[i].setColor(Color.argb((i+1)*96 / numPaints, 0, 0, 0));
				hilitePaints[i].setColor(Color.argb((i+1)*96 / numPaints, 255, 255, 255));
			}
		}


		long duration = android.os.SystemClock.uptimeMillis() - start;
		log.d("PageViewAnimation -- created in " + duration + " millis");
	}

	private void drawGradient(Canvas canvas, Rect rc, Paint[] paints, int startIndex, int endIndex) {
		int n = (startIndex<endIndex) ? endIndex-startIndex+1 : startIndex-endIndex + 1;
		int dir = (startIndex<endIndex) ? 1 : -1;
		int dx = rc.right - rc.left;
		Rect rect = new Rect(rc);
		for (int i=0; i<n; i++) {
			int index = startIndex + i*dir;
			int x1 = rc.left + dx*i/n;
			int x2 = rc.left + dx*(i+1)/n;
			if (x2 > rc.right)
				x2 = rc.right;
			rect.left = x1;
			rect.right = x2;
			if (x2 > x1) {
				canvas.drawRect(rect, paints[index]);
			}
		}
	}

	private void drawShadow(Canvas canvas, Rect rc) {
		drawGradient(canvas, rc, shadePaints, shadePaints.length/2, shadePaints.length/10);
	}

	private final static int DISTORT_PART_PERCENT = 30;
	private void drawDistorted( Canvas canvas, Bitmap bmp, Rect src, Rect dst, int dir) {
		int srcdx = src.width();
		int dstdx = dst.width();
		int dx = srcdx - dstdx;
		int maxdistortdx = srcdx * DISTORT_PART_PERCENT / 100;
		int maxdx = maxdistortdx * (mReaderView.PI_DIV_2 - mReaderView.SIN_TABLE_SCALE) / mReaderView.SIN_TABLE_SCALE;
		int maxdistortsrc = maxdistortdx * mReaderView.PI_DIV_2 / mReaderView.SIN_TABLE_SCALE;

		int distortdx = dx < maxdistortdx ? dx : maxdistortdx;
		int distortsrcstart = -1;
		int distortsrcend = -1;
		int distortdststart = -1;
		int distortdstend = -1;
		int distortanglestart = -1;
		int distortangleend = -1;
		int normalsrcstart = -1;
		int normalsrcend = -1;
		int normaldststart = -1;
		int normaldstend = -1;

		if (dx < maxdx) {
			// start
			int index = dx>=0 ? dx * mReaderView.SIN_TABLE_SIZE / maxdx : 0;
			if (index > mReaderView.DST_TABLE.length)
				index = mReaderView.DST_TABLE.length;
			int dstv = mReaderView.DST_TABLE[index] * maxdistortdx / mReaderView.SIN_TABLE_SCALE;
			distortdststart = distortsrcstart = dstdx - dstv;
			distortsrcend = srcdx;
			distortdstend = dstdx;
			normalsrcstart = normaldststart = 0;
			normalsrcend = distortsrcstart;
			normaldstend = distortdststart;
			distortanglestart = 0;
			distortangleend = mReaderView.SRC_TABLE[index];
			distortdx = maxdistortdx;
		} else if (dstdx>maxdistortdx) {
			// middle
			distortdststart = distortsrcstart = dstdx - maxdistortdx;
			distortsrcend = distortsrcstart + maxdistortsrc;
			distortdstend = dstdx;
			normalsrcstart = normaldststart = 0;
			normalsrcend = distortsrcstart;
			normaldstend = distortdststart;
			distortanglestart = 0;
			distortangleend = mReaderView.PI_DIV_2;
		} else {
			// end
			normalsrcstart = normaldststart = normalsrcend = normaldstend = -1;
			distortdx = dstdx;
			distortsrcstart = 0;
			int n = maxdistortdx >= dstdx ? maxdistortdx - dstdx : 0;
			distortsrcend = mReaderView.ASIN_TABLE[mReaderView.SIN_TABLE_SIZE * n/maxdistortdx ] * maxdistortsrc / mReaderView.SIN_TABLE_SCALE;
			distortdststart = 0;
			distortdstend = dstdx;
			distortangleend = mReaderView.PI_DIV_2;
			n = maxdistortdx >= distortdx ? maxdistortdx - distortdx : 0;
			distortanglestart = mReaderView.ASIN_TABLE[mReaderView.SIN_TABLE_SIZE * (maxdistortdx - distortdx)/maxdistortdx ];
		}

		Rect srcrc = new Rect(src);
		Rect dstrc = new Rect(dst);
		if (normalsrcstart < normalsrcend) {
			if (dir > 0) {
				srcrc.left = src.left + normalsrcstart;
				srcrc.right = src.left + normalsrcend;
				dstrc.left = dst.left + normaldststart;
				dstrc.right = dst.left + normaldstend;
			} else {
				srcrc.right = src.right - normalsrcstart;
				srcrc.left = src.right - normalsrcend;
				dstrc.right = dst.right - normaldststart;
				dstrc.left = dst.right - normaldstend;
			}
			mReaderView.drawDimmedBitmap(canvas, bmp, srcrc, dstrc);
		}
		if (distortdststart < distortdstend) {
			int n = distortdx / 5 + 1;
			int dst0 = mReaderView.SIN_TABLE[distortanglestart * mReaderView.SIN_TABLE_SIZE / mReaderView.PI_DIV_2] * maxdistortdx / mReaderView.SIN_TABLE_SCALE;
			int src0 = distortanglestart * maxdistortdx / mReaderView.SIN_TABLE_SCALE;
			for (int i=0; i<n; i++) {
				int angledelta = distortangleend - distortanglestart;
				int startangle = distortanglestart + i * angledelta / n;
				int endangle = distortanglestart + (i+1) * angledelta / n;
				int src1 = startangle * maxdistortdx / mReaderView.SIN_TABLE_SCALE - src0;
				int src2 = endangle * maxdistortdx / mReaderView.SIN_TABLE_SCALE - src0;
				int dst1 = mReaderView.SIN_TABLE[startangle * mReaderView.SIN_TABLE_SIZE / mReaderView.PI_DIV_2] * maxdistortdx / mReaderView.SIN_TABLE_SCALE - dst0;
				int dst2 = mReaderView.SIN_TABLE[endangle * mReaderView.SIN_TABLE_SIZE / mReaderView.PI_DIV_2] * maxdistortdx / mReaderView.SIN_TABLE_SCALE - dst0;
				int hiliteIndex = startangle * hilitePaints.length / mReaderView.PI_DIV_2;
				Paint[] paints;
				if (dir > 0) {
					dstrc.left = dst.left + distortdststart + dst1;
					dstrc.right = dst.left + distortdststart + dst2;
					srcrc.left = src.left + distortsrcstart + src1;
					srcrc.right = src.left + distortsrcstart + src2;
					paints = hilitePaints;
				} else {
					dstrc.right = dst.right - distortdststart - dst1;
					dstrc.left = dst.right - distortdststart - dst2;
					srcrc.right = src.right - distortsrcstart - src1;
					srcrc.left = src.right - distortsrcstart - src2;
					paints = shadePaints;
				}
				mReaderView.drawDimmedBitmap(canvas, bmp, srcrc, dstrc);
				canvas.drawRect(dstrc, paints[hiliteIndex]);
			}
		}
	}

	@Override
	public void move(int duration, boolean accelerated) {
		if (duration > 0 && mReaderView.getPageFlipAnimationSpeedMs() != 0) {
			int steps = (int)(duration / mReaderView.getAvgAnimationDrawDuration()) + 2;
			//log.i("STEPS: " + steps);
			int x0 = currShift;
			int x1 = destShift;
			if ((x0 - x1) < 10 && (x0 - x1) > -10)
				steps = 2;
			for (int i = 1; i < steps; i++) {
				int x = x0 + (x1 - x0) * i / steps;
				currShift = accelerated ? mReaderView.accelerate( x0, x1, x ) : x;
				//log.i("CURRSHIFT: "+currShift);
				draw();
			}
		}
		currShift = destShift;
		draw();
	}

	@Override
	public void stop(int x, int y) {
		if (mReaderView.currentAnimation == null)
			return;
		alog.v("PageViewAnimation.stop(" + x + ", " + y + ")");
		//if ( started ) {
		boolean moved = false;
		if (x != -1) {
			int threshold = mActivity.getPalmTipPixels() * 7/8;
			if (direction > 0) {
				// |  <=====  |
				int dx = startX - x;
				if (dx > threshold)
					moved = true;
			} else {
				// |  =====>  |
				int dx = x - startX;
				if (dx > threshold)
					moved = true;
			}
			int duration;
			if (moved) {
				destShift = maxX;
				duration = 300; // 500 ms forward
			} else {
				destShift = 0;
				duration = 200; // 200 ms cancel
			}
			move( duration, false );
		} else {
			moved = true;
		}
		mReaderView.doc.doCommand(ReaderCommand.DCMD_GO_PAGE_DONT_SAVE_HISTORY.nativeId, moved ? page2 : page1);
		//}
		mReaderView.scheduleSaveCurrentPositionBookmark(mReaderView.getDefSavePositionInterval());
		close();
		// preparing images for next page flip
		mReaderView.preparePageImage(0);
		mReaderView.preparePageImage(direction);
		mReaderView.updateCurrentPositionStatus();
		//if ( started )
		//	drawPage();
	}

	@Override
	public void update(int x, int y) {
		alog.v("PageViewAnimation.update(" + x + ", " + y + ")");
		int delta = direction>0 ? startX - x : x - startX;
		if (delta <= 0)
			destShift = 0;
		else if (delta < maxX)
			destShift = delta;
		else
			destShift = maxX;
	}

	public void animate()
	{
		alog.v("PageViewAnimation.animate("+currShift + " => " + destShift + ") speed=" + mReaderView.getPageFlipAnimationSpeedMs());
		//log.d("animate() is called");
		if (currShift != destShift) {
			started = true;
			if (mReaderView.getPageFlipAnimationSpeedMs() == 0)
				currShift = destShift;
			else {
				int delta = currShift - destShift;
				if (delta < 0)
					delta = -delta;
				long avgDraw = mReaderView.getAvgAnimationDrawDuration();
				int maxStep = mReaderView.getPageFlipAnimationSpeedMs() > 0 ? (int)(maxX * 1000 / avgDraw / mReaderView.getPageFlipAnimationSpeedMs()) : maxX;
				int step;
				if (delta > maxStep * 2)
					step = maxStep;
				else
					step = (delta + 3) / 4;
				//int step = delta<3 ? 1 : (delta<5 ? 2 : (delta<10 ? 3 : (delta<15 ? 6 : (delta<25 ? 10 : (delta<50 ? 15 : 30)))));
				if (currShift < destShift)
					currShift+=step;
				else if (currShift > destShift)
					currShift-=step;
				alog.v("PageViewAnimation.animate("+currShift + " => " + destShift + "  step=" + step + ")");
			}
			//pointerCurrPos = pointerDestPos;
			draw();
			if (currShift != destShift)
				mReaderView.scheduleAnimation();
		}
	}

	public void draw(Canvas canvas)
	{
		alog.v("PageViewAnimation.draw("+currShift + ")");
//			BitmapInfo image1 = mCurrentPageInfo;
//			BitmapInfo image2 = mNextPageInfo;
		if (image1.isReleased() || image2.isReleased())
			return;
		int w = image1.bitmap.getWidth();
		int h = image1.bitmap.getHeight();
		int div;
		if (direction > 0) {
			// FORWARD
			div = w-currShift;
			Rect shadowRect = new Rect(div, 0, div+w/10, h);
			if (pageFlipAnimationM ==  mReaderView.PAGE_ANIMATION_PAPER) {
				if (this.pageCount == 2) {
					int w2 = w/2;
					if (div < w2) {
						// left - part of old page
						Rect src1 = new Rect(0, 0, div, h);
						Rect dst1 = new Rect(0, 0, div, h);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						// left, resized part of new page
						Rect src2 = new Rect(0, 0, w2, h);
						Rect dst2 = new Rect(div, 0, w2, h);
						//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
						drawDistorted(canvas, image2.bitmap, src2, dst2, -1);
						// right, new page
						Rect src3 = new Rect(w2, 0, w, h);
						Rect dst3 = new Rect(w2, 0, w, h);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src3, dst3);

					} else {
						// left - old page
						Rect src1 = new Rect(0, 0, w2, h);
						Rect dst1 = new Rect(0, 0, w2, h);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						// right, resized old page
						Rect src2 = new Rect(w2, 0, w, h);
						Rect dst2 = new Rect(w2, 0, div, h);
						//canvas.drawBitmap(image1.bitmap, src2, dst2, null);
						drawDistorted(canvas, image1.bitmap, src2, dst2, 1);
						// right, new page
						Rect src3 = new Rect(div, 0, w, h);
						Rect dst3 = new Rect(div, 0, w, h);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src3, dst3);

						if (div > 0 && div < w)
							drawShadow(canvas, shadowRect);
					}
				} else {
					Rect src1 = new Rect(0, 0, w, h);
					Rect dst1 = new Rect(0, 0, w-currShift, h);
					//log.v("drawing " + image1);
					//canvas.drawBitmap(image1.bitmap, src1, dst1, null);
					drawDistorted(canvas, image1.bitmap, src1, dst1, 1);
					Rect src2 = new Rect(w-currShift, 0, w, h);
					Rect dst2 = new Rect(w-currShift, 0, w, h);
					//log.v("drawing " + image1);
					mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);

					if (div > 0 && div < w)
						drawShadow(canvas, shadowRect);
				}
			} else {
				if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_BLUR) {
					int defRadius = 20;
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int radius = (diff * defRadius) / w2;
					if (div < w2) {
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						Bitmap blurredBmp = null;
						if (defRadius - radius > 0)
							if (image2scaled != null)
								blurredBmp = FastBlur.doBlur(image2scaled, defRadius - radius, false);
						mReaderView.drawDimmedBitmap(canvas, blurredBmp == null ? image2.bitmap : blurredBmp, null, dst1);
					} else {
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						Bitmap blurredBmp = null;
						if (defRadius - radius > 0)
							if (image1scaled != null)
								blurredBmp = FastBlur.doBlur(image1scaled, defRadius - radius, false);
						mReaderView.drawDimmedBitmap(canvas, blurredBmp == null ? image1.bitmap : blurredBmp, null, dst2);
					}
				}
				else if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_BLUR_DIM) {
					int defDim = mReaderView.dimmingAlpha;
					int defRadius = 20;
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int dim = (diff * defDim) / w2;
					int radius = (diff * defRadius) / w2;
					if (div < w2) {
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						Bitmap blurredBmp = null;
						if (defRadius - radius > 0)
							if (image2scaled != null)
								blurredBmp = FastBlur.doBlur(image2scaled, defRadius - radius, false);
						mReaderView.drawDimmedBitmapAlpha(canvas, blurredBmp == null ? image2.bitmap : blurredBmp, null, dst1, dim);
					} else {
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						Bitmap blurredBmp = null;
						if (defRadius - radius > 0)
							if (image1scaled != null)
								blurredBmp = FastBlur.doBlur(image1scaled, defRadius - radius, false);
						mReaderView.drawDimmedBitmapAlpha(canvas, blurredBmp == null ? image1.bitmap : blurredBmp, null, dst2, dim);
					}
				}
				else if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_DIM) {
					int defDim = mReaderView.dimmingAlpha;
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int dim = (diff * defDim) / w2;
					if (div < w2) {
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						mReaderView.drawDimmedBitmapAlpha(canvas, image2.bitmap, null, dst1, dim);
					} else {
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						mReaderView.drawDimmedBitmapAlpha(canvas, image1.bitmap, null, dst2,  dim);
					}
				}
				else if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_MAG) {
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int defMaxW = w/4;
					int defMaxH = h/4;
					int curW = defMaxW - (diff * defMaxW) / w2;
					int curH = defMaxH - (diff * defMaxH) / w2;
					if (div < w2) {
						Rect src1 = new Rect(curW, curH, w - curW,  h - curH);
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
					} else {
						Rect src2 = new Rect(curW, curH, w - curW,  h - curH);
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src2, dst2);
					}
				}
				else if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_MAG_DIM) {
					int defDim = mReaderView.dimmingAlpha;
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int dim = (diff * defDim) / w2;
					int defMaxW = w/4;
					int defMaxH = h/4;
					int curW = defMaxW - (diff * defMaxW) / w2;
					int curH = defMaxH - (diff * defMaxH) / w2;
					if (div < w2) {
						Rect src1 = new Rect(curW, curH, w - curW,  h - curH);
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						mReaderView.drawDimmedBitmapAlpha(canvas, image2.bitmap, src1, dst1, dim);
					} else {
						Rect src2 = new Rect(curW, curH, w - curW,  h - curH);
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						mReaderView.drawDimmedBitmapAlpha(canvas, image1.bitmap, src2, dst2,  dim);
					}
				}
				else {
					if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_SLIDE2) {
						Rect src1 = new Rect(currShift, 0, w, h);
						Rect dst1 = new Rect(0, 0, w-currShift, h);
						//log.v("drawing " + image1);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(0, 0, currShift, h);
						Rect dst2 = new Rect(w-currShift, 0, w, h);
						//log.v("drawing " + image2);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					} else {
						Rect src1 = new Rect(currShift, 0, w, h);
						Rect dst1 = new Rect(0, 0, w - currShift, h);
						//log.v("drawing " + image1);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(w - currShift, 0, w, h);
						Rect dst2 = new Rect(w - currShift, 0, w, h);
						//log.v("drawing " + image2);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					}
				}
			}
		} else {
			// BACK
			div = currShift;
			Rect shadowRect = new Rect(div, 0, div+10, h);
			if (pageFlipAnimationM ==  mReaderView.PAGE_ANIMATION_PAPER) {
				if (this.pageCount == 2) {
					int w2 = w/2;
					if (div < w2) {
						// left - part of old page
						Rect src1 = new Rect(0, 0, div, h);
						Rect dst1 = new Rect(0, 0, div, h);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
						// left, resized part of new page
						Rect src2 = new Rect(0, 0, w2, h);
						Rect dst2 = new Rect(div, 0, w2, h);
						//canvas.drawBitmap(image1.bitmap, src2, dst2, null);
						drawDistorted(canvas, image1.bitmap, src2, dst2, -1);
						// right, new page
						Rect src3 = new Rect(w2, 0, w, h);
						Rect dst3 = new Rect(w2, 0, w, h);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src3, dst3);
					} else {
						// left - old page
						Rect src1 = new Rect(0, 0, w2, h);
						Rect dst1 = new Rect(0, 0, w2, h);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
						// right, resized old page
						Rect src2 = new Rect(w2, 0, w, h);
						Rect dst2 = new Rect(w2, 0, div, h);
						//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
						drawDistorted(canvas, image2.bitmap, src2, dst2, 1);
						// right, new page
						Rect src3 = new Rect(div, 0, w, h);
						Rect dst3 = new Rect(div, 0, w, h);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src3, dst3);

						if (div > 0 && div < w)
							drawShadow(canvas, shadowRect);
					}
				} else {
					Rect src1 = new Rect(currShift, 0, w, h);
					Rect dst1 = new Rect(currShift, 0, w, h);
					mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
					Rect src2 = new Rect(0, 0, w, h);
					Rect dst2 = new Rect(0, 0, currShift, h);
					//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
					drawDistorted(canvas, image2.bitmap, src2, dst2, 1);

					if (div > 0 && div < w)
						drawShadow(canvas, shadowRect);
				}
			} else {
				if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_BLUR) {
					int defRadius = 20;
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int radius = (diff * defRadius) / w2;
					if (div < w2) {
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						Bitmap blurredBmp = null;
						if (defRadius - radius > 0)
							if (image1scaled != null)
								blurredBmp = FastBlur.doBlur(image1scaled, defRadius - radius, false);
						mReaderView.drawDimmedBitmap(canvas, blurredBmp == null ? image1.bitmap : blurredBmp, null, dst1);
					} else {
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						Bitmap blurredBmp = null;
						if (defRadius - radius > 0)
							if (image2scaled != null)
								blurredBmp = FastBlur.doBlur(image2scaled, defRadius - radius, false);
						mReaderView.drawDimmedBitmap(canvas, blurredBmp == null ? image2.bitmap : blurredBmp, null, dst2);
					}
				}
				else if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_BLUR_DIM) {
					int defDim = mReaderView.dimmingAlpha;
					int defRadius = 20;
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int dim = (diff * defDim) / w2;
					int radius = (diff * defRadius) / w2;
					if (div < w2) {
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						Bitmap blurredBmp = null;
						if (defRadius - radius > 0)
							if (image2scaled != null)
								blurredBmp = FastBlur.doBlur(image1scaled, defRadius - radius, false);
						mReaderView.drawDimmedBitmapAlpha(canvas, blurredBmp == null ? image1.bitmap : blurredBmp, null, dst1, dim);
					} else {
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						Bitmap blurredBmp = null;
						if (defRadius - radius > 0)
							if (image1scaled != null)
								blurredBmp = FastBlur.doBlur(image2scaled, defRadius - radius, false);
						mReaderView.drawDimmedBitmapAlpha(canvas, blurredBmp == null ? image2.bitmap : blurredBmp, null, dst2, dim);
					}
				}
				else if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_DIM) {
					int defDim = mReaderView.dimmingAlpha;
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int dim = (diff * defDim) / w2;
					if (div < w2) {
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						mReaderView.drawDimmedBitmapAlpha(canvas, image1.bitmap, null, dst1, dim);
					} else {
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						mReaderView.drawDimmedBitmapAlpha(canvas, image2.bitmap, null, dst2,  dim);
					}

				} else if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_MAG) {
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int defMaxW = w/4;
					int defMaxH = h/4;
					int curW = defMaxW - (diff * defMaxW) / w2;
					int curH = defMaxH - (diff * defMaxH) / w2;
					if (div < w2) {
						Rect src1 = new Rect(curW, curH, w - curW,  h - curH);
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
					} else {
						Rect src2 = new Rect(curW, curH, w - curW,  h - curH);
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					}
				}  else if (pageFlipAnimationM == mReaderView.PAGE_ANIMATION_MAG_DIM) {
					int defDim = mReaderView.dimmingAlpha;
					int w2 = w / 2;
					int diff = Math.abs(div - w2);
					int dim = (diff * defDim) / w2;
					int defMaxW = w/4;
					int defMaxH = h/4;
					int curW = defMaxW - (diff * defMaxW) / w2;
					int curH = defMaxH - (diff * defMaxH) / w2;
					if (div < w2) {
						Rect src1 = new Rect(curW, curH, w - curW,  h - curH);
						Rect dst1 = new Rect(0, 0, w, h);
						//log.v("drawing " + image1);
						mReaderView.drawDimmedBitmapAlpha(canvas, image1.bitmap, src1, dst1, dim);
					} else {
						Rect src2 = new Rect(curW, curH, w - curW,  h - curH);
						Rect dst2 = new Rect(0, 0, w, h);
						//log.v("drawing " + image2);
						mReaderView.drawDimmedBitmapAlpha(canvas, image2.bitmap, src2, dst2, dim);
					}
				} else {
					if (pageFlipAnimationM ==  mReaderView.PAGE_ANIMATION_SLIDE2) {
						Rect src1 = new Rect(0, 0, w - currShift, h);
						Rect dst1 = new Rect(currShift, 0, w, h);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(w - currShift, 0, w, h);
						Rect dst2 = new Rect(0, 0, currShift, h);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					} else {
						Rect src1 = new Rect(currShift, 0, w, h);
						Rect dst1 = new Rect(currShift, 0, w, h);
						mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(w - currShift, 0, w, h);
						Rect dst2 = new Rect(0, 0, currShift, h);
						mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					}
				}
			}
		}
		if (div > 0 && div < w) {
			if (
					(pageFlipAnimationM ==  mReaderView.PAGE_ANIMATION_PAPER) ||
							(pageFlipAnimationM ==  mReaderView.PAGE_ANIMATION_SLIDE) ||
							(pageFlipAnimationM ==  mReaderView.PAGE_ANIMATION_SLIDE2)
			)
				canvas.drawLine(div, 0, div, h, divPaint);
		}
	}
}
