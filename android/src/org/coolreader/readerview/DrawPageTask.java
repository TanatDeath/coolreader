package org.coolreader.readerview;

import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.DeviceInfo;

public class DrawPageTask extends Task {
	final int id;
	BitmapInfo bi;
	Runnable doneHandler;
	boolean isPartially;
	final ReaderView mReaderView;
	DrawPageTask(ReaderView rv, Runnable doneHandler, boolean isPartially)
	{
		mReaderView = rv;
		this.id = ++mReaderView.lastDrawTaskId;
		this.doneHandler = doneHandler;
		this.isPartially = isPartially;
		mReaderView.cancelGc();
	}
	public void work() {
		BackgroundThread.ensureBackground();
		if (this.id != mReaderView.lastDrawTaskId) {
			log.d("skipping duplicate drawPage request");
			return;
		}
		mReaderView.nextHiliteId++;
		if (mReaderView.currentAnimation != null) {
			log.d("skipping drawPage request while scroll animation is in progress");
			return;
		}
		log.e("DrawPageTask.work("+mReaderView.internalDX+","+mReaderView.internalDY+")");
		bi = mReaderView.preparePageImage(0);
		if (bi != null) {
			if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
				boolean needDraw = true;
				if (!isPartially)
					if (mReaderView.lastCachedBitmap != null)
						if (bi.bitmap.sameAs(mReaderView.lastCachedBitmap)) needDraw = false;
				if (needDraw) {
					mReaderView.lastCachedBitmap = bi.bitmap.copy(bi.bitmap.getConfig(), true);
					mReaderView.bookView.draw(isPartially);
				} else log.v("Unnessesary redraw skipped");
			} else
				mReaderView.bookView.draw(isPartially);
		}
	}
	@Override
	public void done()
	{
		BackgroundThread.ensureGUI();
//			log.d("drawPage : bitmap is ready, invalidating view to draw new bitmap");
//			if (bi != null) {
//				setBitmap( bi.bitmap );
//				invalidate();
//			}
//    		if (mOpened)
		//hideProgress();
		if (doneHandler != null)
			doneHandler.run();
		mReaderView.scheduleGc();
	}
	@Override
	public void fail(Exception e) {
		mReaderView.hideProgress();
	}
};
