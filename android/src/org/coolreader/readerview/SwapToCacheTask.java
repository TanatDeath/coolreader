package org.coolreader.readerview;

import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.DocView;

public class SwapToCacheTask extends Task {
	boolean isTimeout;
	long startTime;

	final ReaderView mReaderView;
	public SwapToCacheTask(ReaderView rv) {
		startTime = System.currentTimeMillis();
		mReaderView = rv;
	}

	public void reschedule() {
		if (this != mReaderView.currentSwapTask)
			return;
		BackgroundThread.instance().postGUI(() -> mReaderView.post(SwapToCacheTask.this), 2000);
	}
	@Override
	public void work() throws Exception {
		if (this != mReaderView.currentSwapTask)
			return;
		int res = mReaderView.doc.swapToCache();
		isTimeout = res== DocView.SWAP_TIMEOUT;
		long duration = System.currentTimeMillis() - startTime;
		if (!isTimeout) {
			log.i("swapToCacheInternal is finished with result " + res + " in " + duration + " ms");
		} else {
			log.d("swapToCacheInternal exited by TIMEOUT in " + duration + " ms: rescheduling");
		}
	}
	@Override
	public void done() {
		if (isTimeout)
			reschedule();
	}

}
