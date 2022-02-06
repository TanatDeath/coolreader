package org.coolreader.eink;

import android.content.Context;
import android.util.Log;
import android.view.View;

import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.utils.Utils;
import com.onyx.android.sdk.api.device.epd.UpdateMode;
import com.onyx.android.sdk.api.device.epd.UpdateOption;
import com.onyx.android.sdk.api.device.EpdDeviceManager;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;

import java.util.Arrays;
import java.util.List;

public class EinkScreenOnyx implements EinkScreen {

	public static final Logger log = L.create("onyx", Log.VERBOSE);

	private EinkUpdateMode mUpdateMode = EinkUpdateMode.Unspecified;
	private int mUpdateInterval;
	private int mRefreshNumber = -1;
	private boolean mInFastMode = false;
	private boolean mInA2Mode = false;
	// Front light levels
	private List<Integer> mFrontLineLevels = null;
	private List<Integer> mWarmLightLevels = null;
	private UpdateOption mOnyxUpdateOption = UpdateOption.NORMAL;
	private int mExtraDelayFullRefresh = 0;
	private int mScreenFullUpdateMethod = 0;
	private boolean mIsAppOptimizationEnabled = false;
	private boolean mNeedCallByPass = false;
	private boolean mNeedDeepGC = false;
	private boolean mRegal = true;
	private boolean mSelectionActive = false;

	private com.onyx.android.sdk.device.BaseDevice curDev;

	private boolean onyxD_enableScreenUpdate(View view, boolean enable) {
		return curDev.enableScreenUpdate(view, enable);
	}

	private boolean onyxD_isAppOptimizationEnabled() {
		return org.eink_onyx_reflections.OnyxDevice.currentDevice().isAppOptimizationEnabled();
	}

	private boolean onyxD_setViewDefaultUpdateMode(View view, UpdateMode mode) {
		return curDev.setViewDefaultUpdateMode(view, mode);
	}

	private void onyxD_byPass(int count) {
		curDev.byPass(count);
	}

	private int onyxD_getWarmLightConfigValue(Context context) {
		return curDev.getWarmLightConfigValue(context);
	}

	private int onyxD_getColdLightConfigValue(Context context) {
		return curDev.getColdLightConfigValue(context);
	}

	private int onyxD_getFrontLightDeviceValue(Context context) {
		return curDev.getFrontLightDeviceValue(context);
	}

	private boolean onyxD_setWarmLightDeviceValue(Context context, int value) {
		return curDev.setWarmLightDeviceValue(context, value);
	}

	private boolean onyxD_setColdLightDeviceValue(Context context, int value) {
		return curDev.setColdLightDeviceValue(context, value);
	}

	private boolean onyxD_setFrontLightDeviceValue(Context context, int value) {
		return curDev.setFrontLightDeviceValue(context, value);
	}

	private boolean onyxD_setFrontLightConfigValue(Context context, int value) {
		return curDev.setFrontLightConfigValue(context, value);
	}

	private List<Integer> onyxD_getFrontLightValueList(Context context) {
		return curDev.getFrontLightValueList(context);
	}

	private List<Integer> onyxD_getColdLightValues(Context context) {
		Integer[] list = curDev.getColdLightValues(context);
		if (list.length > 1)
			return Arrays.asList(list);
		return null;
	}

	private List<Integer> onyxD_getWarmLightValues(Context context) {
		Integer[] list = curDev.getWarmLightValues(context);
		if (list.length > 1)
			return Arrays.asList(list);
		return null;
	}



@Override
	public void setupController(EinkUpdateMode mode, int updateInterval, View view, boolean noRegal) {
		curDev = com.onyx.android.sdk.device.Device.currentDevice();
		switch (curDev.getSystemRefreshMode()) {
			case NORMAL:
				mUpdateMode = EinkUpdateMode.Normal;
				break;
			case FAST_QUALITY:
				mUpdateMode = EinkUpdateMode.FastQuality;
				break;
			case FAST:
				mUpdateMode = EinkUpdateMode.FastA2;
				break;
			case FAST_X:
				mUpdateMode = EinkUpdateMode.FastX;
				break;
			default:
				mUpdateMode = EinkUpdateMode.Unspecified;
				break;
		}
		mIsAppOptimizationEnabled = onyxD_isAppOptimizationEnabled();
		if (mIsAppOptimizationEnabled) {
			log.i("ONYX App Optimization is enabled");
		} else {
			log.i("ONYX App Optimization is disabled");
		}
		mUpdateInterval = updateInterval;
//		if (mUpdateMode.equals(mode))
//			return;
		log.d("EinkScreenOnyx.setupController(): mode=" + mode);
		onyxD_enableScreenUpdate(view, true);
		mRefreshNumber = 0;
		switch (com.onyx.android.sdk.device.Device.currentDeviceIndex()) {
			case Rk32xx:
			case Rk33xx:
			case SDM:
				mNeedCallByPass = !mIsAppOptimizationEnabled;
				break;
		}
		switch (com.onyx.android.sdk.device.Device.currentDeviceIndex()) {
			// TODO: check other ONYX devices & platforms
			case SDM:
				// Hack, use additional delay before full screen update
				mExtraDelayFullRefresh = 40;
				break;
		}
		curDev.enableRegal(mRegal && (!noRegal));
		switch (mode) {
			case Normal:            // Quality
				curDev.setSystemRefreshMode(UpdateOption.NORMAL);
				mOnyxUpdateOption = UpdateOption.NORMAL;
				break;
			case FastQuality:       // Fast
				curDev.setSystemRefreshMode(UpdateOption.FAST_QUALITY);
				mOnyxUpdateOption = UpdateOption.FAST_QUALITY;
				break;
			case FastA2:            // A2 mode
				curDev.setSystemRefreshMode(UpdateOption.FAST);
				mOnyxUpdateOption = UpdateOption.FAST;
				break;
			case FastX:             // X mode
				curDev.setSystemRefreshMode(UpdateOption.FAST_X);
				mOnyxUpdateOption = UpdateOption.FAST_X;
				break;
			default:
				curDev.setSystemRefreshMode(UpdateOption.NORMAL);
				mOnyxUpdateOption = UpdateOption.NORMAL;
		}
		if (null != view) {
			onyxD_setViewDefaultUpdateMode(view, (mRegal  && (!noRegal))? UpdateMode.REGAL: UpdateMode.GU);
			BackgroundThread.instance().executeGUI(view::invalidate);
		}
		mUpdateMode = mode;
	}

	@Override
	public void setNeedBypass(int needBypass) {
		if (needBypass == 1) mNeedCallByPass = true;
		if (needBypass == 2) mNeedCallByPass = false;
	}

	@Override
	public void setNeedDeepGC(boolean needDeepGC) {
		mNeedDeepGC = needDeepGC;
	}

	@Override
	public void setRegal(boolean regal) {
		mRegal = regal;
	}

	@Override
	public void setSelectionActive(boolean selectionActive) {
		mSelectionActive = selectionActive;
	}

	@Override
	public void setExtraDelayFullRefresh(int extraDelayFullRefresh) {
		if (extraDelayFullRefresh >= 0) mExtraDelayFullRefresh = extraDelayFullRefresh;
	}

	@Override
	public void setScreenFullUpdateMethod(int screenFullUpdateMethod) {
		mScreenFullUpdateMethod = screenFullUpdateMethod;
	}

	@Override
	public void prepareController(View view, boolean isPartially) {
		if (mIsAppOptimizationEnabled)
			return;
		//if (mSelectionActive) onyxEnableA2Mode(view, true);
		if (isPartially)
			return;
		if (mRefreshNumber == -1) {
			mRefreshNumber = 0;
			onyxRepaintEveryThing(view, false);
			return;
		}
		if (mUpdateInterval > 0) {
			mRefreshNumber++;
			if (mRefreshNumber >= mUpdateInterval) {
				mRefreshNumber = 0;
				return;
			}
		}
		if (mRefreshNumber > 0 || mUpdateInterval == 0) {
			onyxD_setViewDefaultUpdateMode(view, mRegal? UpdateMode.REGAL: UpdateMode.GU);
			if (mNeedCallByPass) {
				// Hack, without it, Regal NOT work (if app optimization is disabled).
				// But if app optimization is enabled this cause flickering: after screen drawn - screen cleared and then image restored
				// Also, without it, on rk3288 with firmware 2.1 & 3.0 the image will not updated.
				onyxD_byPass(0);
			}
		}
	}

	@Override
	public void updateController(View view, boolean isPartially) {
		if (mIsAppOptimizationEnabled)
			return;
		if (isPartially)
			return;
		if (0 == mRefreshNumber && mUpdateInterval > 0) {
			if (mExtraDelayFullRefresh > 0) {
				// Hack, on ONYX devices with SDM platform without this delay full screen refresh runs too early
				// (before new page appears on screen)
				// This functions called after android.view.SurfaceHolder.unlockCanvasAndPost()
				//   See https://developer.android.com/reference/android/view/SurfaceHolder#unlockCanvasAndPost(android.graphics.Canvas)
				// which guarantees that by this time the new image will be on the screen
				// But in fact on com.onyx.android.sdk.device.Device.DeviceIndex.SDM need extra delay.
//				try {
//					Thread.sleep(mExtraDelayFullRefresh);
//				} catch (InterruptedException ignored) {
//				}
				BackgroundThread.instance().postGUI(() -> onyxRepaintEveryThing(view, true),  mExtraDelayFullRefresh);
			} else onyxRepaintEveryThing(view, true);
		}
	}

	@Override
	public void refreshScreen(View view) {
		onyxRepaintEveryThing(view, true);
		mRefreshNumber = 0;
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
	public int getFrontLightValue(Context context) {
		int res = 0;
		try {
			if (DeviceInfo.ONYX_HAVE_NATURAL_BACKLIGHT) {
				res = onyxD_getColdLightConfigValue(context);
			} else {
				res = onyxD_getFrontLightDeviceValue(context);
			}
		} catch (Exception ignored) {}
		return res;
	}

	@Override
	public boolean setFrontLightValue(Context context, int value) {
		boolean res = false;
		if (DeviceInfo.ONYX_HAVE_FRONTLIGHT) {
			if (value >= 0) {
				Integer alignedValue = Utils.findNearestValue(getFrontLightLevels(context), value);
				if (null != alignedValue) {
					if (DeviceInfo.ONYX_HAVE_NATURAL_BACKLIGHT) {
						res = onyxD_setColdLightDeviceValue(context, alignedValue);
					} else {
						if (onyxD_setFrontLightDeviceValue(context, alignedValue))
							res = onyxD_setFrontLightConfigValue(context, alignedValue);
					}
				}
			} else {
				// system default, just ignore
			}
		}
		return res;
	}

	@Override
	public int getWarmLightValue(Context context) {
		int res = 0;
		try {
			if (DeviceInfo.ONYX_HAVE_NATURAL_BACKLIGHT) {
				res = onyxD_getWarmLightConfigValue(context);
			}
		} catch (Exception ignored) {}
		return res;
	}

	@Override
	public boolean setWarmLightValue(Context context, int value) {
		boolean res = false;
		if (DeviceInfo.ONYX_HAVE_NATURAL_BACKLIGHT) {
			if (value >= 0) {
				Integer alignedValue = Utils.findNearestValue(getWarmLightLevels(context), value);
				if (null != alignedValue) {
					res = onyxD_setWarmLightDeviceValue(context, alignedValue);
				}
			} else {
				// system default, just ignore
			}
		}
		return res;
	}

	@Override
	public List<Integer> getFrontLightLevels(Context context) {
		if (DeviceInfo.ONYX_HAVE_FRONTLIGHT || DeviceInfo.ONYX_HAVE_NATURAL_BACKLIGHT) {
			if (null == mFrontLineLevels) {
				try {
					mFrontLineLevels = onyxD_getFrontLightValueList(context);
				} catch (Exception ignored) { }
				if (null == mFrontLineLevels || mFrontLineLevels.size() == 0) {
					mFrontLineLevels = onyxD_getColdLightValues(context);
				}
			}
		}
		return mFrontLineLevels;
	}

	@Override
	public List<Integer> getWarmLightLevels(Context context) {
		if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT) {
			if (null == mWarmLightLevels) {
				if (DeviceInfo.ONYX_HAVE_NATURAL_BACKLIGHT) {
					mWarmLightLevels = onyxD_getWarmLightValues(context);
				}
			}
		}
		return mWarmLightLevels;
	}

	@Override
	public boolean isAppOptimizationEnabled() {
		return mIsAppOptimizationEnabled;
	}

	private void onyxRepaintEveryThing(View view, boolean invalidate) {
		switch (com.onyx.android.sdk.device.Device.currentDeviceIndex()) {
			case Rk31xx:
			case Rk32xx:
			case Rk33xx:
			case SDM:
				if (mScreenFullUpdateMethod == 1) {
					curDev.enableRegal(false);
					curDev.setViewDefaultUpdateMode(view,
							mNeedDeepGC? UpdateMode.DEEP_GC: UpdateMode.GC);
					curDev.enableRegal(mRegal);
					break;
				} else
					if (mScreenFullUpdateMethod == 3) {
						if (null != view) {
							curDev.enableRegal(false);
							curDev.setViewDefaultUpdateMode(view,
									mNeedDeepGC? UpdateMode.DEEP_GC: UpdateMode.GC);
							curDev.invalidate(view,
									mNeedDeepGC? UpdateMode.DEEP_GC: UpdateMode.GC);
							curDev.enableRegal(mRegal);
						}
						break;
					} else
					{
						if (null != view) {
							curDev.enableRegal(false);
							curDev.setViewDefaultUpdateMode(view,
									mNeedDeepGC? UpdateMode.DEEP_GC: UpdateMode.GC);
							if (invalidate)
								view.postInvalidate();
							curDev.enableRegal(mRegal);
						}
						break;
					}
			default:
				if (null != view) {
					curDev.setViewDefaultUpdateMode(view,
							mNeedDeepGC? UpdateMode.DEEP_GC: UpdateMode.GC);
					if (invalidate)
						view.postInvalidate();
				}
				break;
		}
	}
}
