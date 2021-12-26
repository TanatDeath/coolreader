package org.coolreader.readerview;

abstract class ViewAnimationBase implements ReaderView.ViewAnimationControl {
	//long startTimeStamp;
	boolean started;
	public boolean isStarted()
	{
		return started;
	}

	ReaderView mReaderView;

	public ViewAnimationBase(ReaderView rv)
	{
		//startTimeStamp = android.os.SystemClock.uptimeMillis();
		mReaderView = rv;
		mReaderView.cancelGc();
	}

	public void close()
	{
		mReaderView.animationScheduler.cancel();
		mReaderView.currentAnimation = null;
		mReaderView.scheduleSaveCurrentPositionBookmark(mReaderView.getDefSavePositionInterval());
		mReaderView.lastSavedBookmark = null;
		mReaderView.updateCurrentPositionStatus();

		mReaderView.scheduleGc();
	}

	public void draw()
	{
		draw(false);
	}

	public void draw(boolean isPartially)
	{
		//	long startTs = android.os.SystemClock.uptimeMillis();
		mReaderView.drawCallback(this::draw, null, isPartially);
	}
}