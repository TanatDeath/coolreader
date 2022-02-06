package org.coolreader.eink;

import android.content.Context;
import android.view.View;

import org.coolreader.eink.EinkScreen;

import java.util.List;

public class EinkScreenDummy implements EinkScreen {

	private boolean mSelectionActive = false;

	@Override
	public void setupController(EinkUpdateMode mode, int updateInterval, View view, boolean noRegal) {
	}

	@Override
	public void setNeedBypass(int needBypass) {

	}

	@Override
	public void setNeedDeepGC(boolean needDeepGC) {

	}

	@Override
	public void setRegal(boolean regal) {

	}

	@Override
	public void setSelectionActive(boolean selectionActive) {
		mSelectionActive = selectionActive;
	}

	@Override
	public void setExtraDelayFullRefresh(int extraDelayFullRefresh) {

	}

	@Override
	public void setScreenFullUpdateMethod(int screenFullUpdateMethod) {

	}

	@Override
	public void prepareController(View view, boolean isPartially) {
	}

	@Override
	public void updateController(View view, boolean isPartially) {
	}

	@Override
	public void refreshScreen(View view) {
	}

	@Override
	public EinkUpdateMode getUpdateMode() {
		return EinkUpdateMode.Unspecified;
	}

	@Override
	public int getUpdateInterval() {
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
}
