package org.coolreader.eink;

import android.content.Context;
import android.util.Log;
import android.view.View;

import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.N2EpdController;

import java.util.List;

public class EinkScreenNook implements EinkScreen {

	public static final Logger log = L.create("nook", Log.VERBOSE);

	/// variables
	protected EinkUpdateMode mUpdateMode = EinkUpdateMode.Unspecified;
	// 0 - Clear, set only for old_mode == 2
	// 1 - Fast, always set in prepare
	// 2 - Active, set in prepare
	protected int mUpdateInterval;
	protected int mRefreshNumber = -1;
	protected boolean mIsSleep = false;

	private boolean mSelectionActive = false;

	@Override
	public void setupController(EinkUpdateMode mode, int updateInterval, View view, boolean noRegal) {
		mUpdateInterval = updateInterval;
		if (mUpdateMode.equals(mode))
			return;
		log.d("EinkScreenNookTolino.setupController(): mode=" + mode);
		switch (mode) {
			case Normal:
				if (mUpdateMode == EinkUpdateMode.Active) {
					mRefreshNumber = -1;
				} else {
					mRefreshNumber = 0;
				}
				break;
			case FastQuality:
				mRefreshNumber = 0;
				break;
			default:
				mRefreshNumber = -1;
		}
		mUpdateMode = mode;
	}

	@Override
	public void setNeedBypass(int needBypass) {

	}

	@Override
	public void setDeepUpdateInterval(int deepUpdateInterval) {

	}

	@Override
	public void setRegal(boolean regal) {

	}

	@Override
	public void setExtraDelayFullRefresh(int extraDelayFullRefresh) {

	}

	@Override
	public void setScreenFullUpdateMethod(int screenFullUpdateMethod) {

	}

	@Override
	public void prepareController(View view, boolean isPartially) {
		//System.err.println("Sleep = " + isPartially);
		if (isPartially || mIsSleep != isPartially) {
			nookSleepController(isPartially, view);
//				if (isPartially)
			return;
		}
		if (mRefreshNumber == -1) {
			switch (mUpdateMode) {
				case Normal:
					nookSetMode(view, mUpdateMode);
					break;
				case Active:
					if (mUpdateInterval == 0) {
						nookSetMode(view, mUpdateMode);
					}
					break;
			}
			mRefreshNumber = 0;
			return;
		}
		if (mUpdateMode == EinkUpdateMode.Normal) {
			nookSetMode(view, mUpdateMode);
			return;
		}
		if (mUpdateInterval > 0 || mUpdateMode == EinkUpdateMode.FastQuality) {
			if (mRefreshNumber == 0 || (mUpdateMode == EinkUpdateMode.FastQuality && mRefreshNumber < mUpdateInterval)) {
				switch (mUpdateMode) {
					case Active:
						nookSetMode(view, mUpdateMode);
						break;
					case FastQuality:
						nookSetMode(view, mUpdateMode);
						break;
				}
			} else if (mUpdateInterval <= mRefreshNumber) {
				nookSetMode(view, EinkUpdateMode.Normal);
				mRefreshNumber = -1;
			}
			if (mUpdateInterval > 0) {
				mRefreshNumber++;
			}
		}
	}

	@Override
	public void updateController(View view, boolean isPartially) {
		// do nothing...
	}

	@Override
	public void refreshScreen(View view) {
		mRefreshNumber = -1;
	}

	@Override
	public EinkUpdateMode getUpdateMode() {
		return mUpdateMode;
	}

	@Override
	public int getUpdateInterval() {
		return mUpdateInterval;
	}

	@Override
	public int getDeepUpdateInterval() {
		return 0;
	}

	@Override
	public int getFrontLightValue(Context context) {
		return 0;
	}

	@Override
	public boolean setFrontLightValue(Context context, int value) {
		return false;
	}

	@Override
	public int getWarmLightValue(Context context) {
		return 0;
	}

	@Override
	public boolean setWarmLightValue(Context context, int value) {
		return false;
	}

	@Override
	public List<Integer> getFrontLightLevels(Context context) {
		return null;
	}

	@Override
	public List<Integer> getWarmLightLevels(Context context) {
		return null;
	}

	@Override
	public boolean isAppOptimizationEnabled() {
		return false;
	}


	// private methods
	private void nookSleepController(boolean toSleep, View view) {
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
						nookSetMode(view, EinkUpdateMode.Normal);
						mRefreshNumber = -1;
				}
			} else {
				setupController(mUpdateMode, mUpdateInterval, view, false);
			}
		}
	}

	private void nookSetMode(View view, EinkUpdateMode mode) {
		switch (mode) {
			case Normal:
				N2EpdController.setMode(N2EpdController.REGION_APP_3,
						N2EpdController.WAVE_GC,
						N2EpdController.MODE_ONESHOT_ALL);
//				N2EpdController.MODE_CLEAR, view);
				break;
			case FastQuality:
				N2EpdController.setMode(N2EpdController.REGION_APP_3,
						N2EpdController.WAVE_GU,
						N2EpdController.MODE_ONESHOT_ALL);
//				N2EpdController.MODE_ONESHOT_ALL, view);
				break;
			case Active:
				N2EpdController.setMode(N2EpdController.REGION_APP_3,
						N2EpdController.WAVE_GL16,
						N2EpdController.MODE_ACTIVE_ALL);
//				N2EpdController.MODE_ACTIVE_ALL, view);
				break;
		}
	}

	@Override
	public void setSelectionActive(boolean selectionActive) {
		mSelectionActive = selectionActive;
	}

}
