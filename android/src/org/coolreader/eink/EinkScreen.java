package org.coolreader.eink;

import android.content.Context;
import android.view.View;

import java.util.List;

public interface EinkScreen {

	enum EinkUpdateMode {
		Unspecified(-1),
		Regal(3),				// also known as 'SNOW Field', in onyx - boolean, can be applied to any mode
		Normal(0),			// old name CMODE_CLEAR, Onyx = Notmal
		FastQuality(1),		// old name CMODE_ONESHOT, Onyx = Fast_Quality
		Active(2),			// old name CMODE_ACTIVE
		FastA2(4),			// Fast 'A2' mode, Onyx = Fast
		FastX(5),				// Onyx X Mode, Onyx = Fast_X
		;

		public static EinkUpdateMode byCode(int code) {
			for (EinkUpdateMode mode : EinkUpdateMode.values()) {
				if (mode.code == code)
					return mode;
			}
			return EinkUpdateMode.Unspecified;
		}

		EinkUpdateMode(int code) {
			this.code = code;
		}

		public final int code;
	}

	void setupController(EinkUpdateMode mode, int updateInterval, View view, boolean noRegal);

	void setNeedBypass(int needBypass);

	void setDeepUpdateInterval(int deepUpdateInterval);

	void setRegal(boolean regal);

	void setSelectionActive(boolean selectionActive);

	void setExtraDelayFullRefresh(int extraDelayFullRefresh);

	void setScreenFullUpdateMethod(int screenFullUpdateMethod);

	void prepareController(View view, boolean isPartially);

	void updateController(View view, boolean isPartially);

	void refreshScreen(View view);

	EinkUpdateMode getUpdateMode();

	int getUpdateInterval();

	int getDeepUpdateInterval();

	int getFrontLightValue(Context context);

	boolean setFrontLightValue(Context context, int value);

	int getWarmLightValue(Context context);

	boolean setWarmLightValue(Context context, int value);

	List<Integer> getFrontLightLevels(Context context);

	List<Integer> getWarmLightLevels(Context context);

	boolean isAppOptimizationEnabled();
}
