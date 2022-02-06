package org.coolreader.eink;

import android.view.View;

import org.coolreader.crengine.TolinoEpdController;

public class EinkScreenTolino extends EinkScreenNook {

	@Override
	public void prepareController(View view, boolean isPartially) {
		//System.err.println("Sleep = " + isPartially);
		if (isPartially || mIsSleep != isPartially) {
			tolinoSleepController(isPartially, view);
//			if (isPartially)
			return;
		}
		if (mRefreshNumber == -1) {
			switch (mUpdateMode) {
				case Normal:
					tolinoSetMode(view, mUpdateMode);
					break;
				case Active:
					if (mUpdateInterval == 0) {
						tolinoSetMode(view, mUpdateMode);
					}
					break;
			}
			mRefreshNumber = 0;
			return;
		}
		if (mUpdateMode == EinkUpdateMode.Normal) {
			tolinoSetMode(view, mUpdateMode);
			return;
		}
		if (mUpdateInterval > 0 || mUpdateMode == EinkUpdateMode.FastQuality) {
			if (mRefreshNumber == 0 || (mUpdateMode == EinkUpdateMode.FastQuality && mRefreshNumber < mUpdateInterval)) {
				switch (mUpdateMode) {
					case Active:
						tolinoSetMode(view, mUpdateMode);
						break;
					case FastQuality:
						tolinoSetMode(view, mUpdateMode);
						break;
				}
			} else if (mUpdateInterval <= mRefreshNumber) {
				tolinoSetMode(view, EinkUpdateMode.Normal);
				mRefreshNumber = -1;
			}
			if (mUpdateInterval > 0) {
				mRefreshNumber++;
			}
		}
	}


	// private methods
	private void tolinoSleepController(boolean toSleep, View view) {
		if (toSleep != mIsSleep) {
			log.d("+++SleepController " + toSleep);
			mIsSleep = toSleep;
			if (mIsSleep) {
				switch (mUpdateMode) {
					case Normal:
						break;
					case FastQuality:
						break;
					case Active:
						tolinoSetMode(view, EinkUpdateMode.Normal);
						mRefreshNumber = -1;
				}
			} else {
				setupController(mUpdateMode, mUpdateInterval, view, false);
			}
		}
	}

	private void tolinoSetMode(View view, EinkUpdateMode mode) {
		TolinoEpdController.setMode(view, mode);
	}
}
