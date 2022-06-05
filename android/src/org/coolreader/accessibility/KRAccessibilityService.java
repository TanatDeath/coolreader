package org.coolreader.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

public class KRAccessibilityService extends AccessibilityService {

	private static final String TAG = KRAccessibilityService.class.getSimpleName();

	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	public void onCreate() {
		Log.d(TAG, "KRA: - onCreate");
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
		Log.d(TAG, "KRA:onAccessibilityEvent");
		Log.d(TAG, "KRA:onAccessibilityEvent " + accessibilityEvent.getEventType());
		Log.d(TAG, "KRA:onAccessibilityEvent " + accessibilityEvent.getAction());
		Log.d(TAG, "KRA:onAccessibilityEvent " + accessibilityEvent.getAction());
	}

	@Override
	public void onInterrupt() {
		Log.d(TAG, "KRA:onInterrupt");
	}

	@Override
	protected boolean onGesture(int gestureId) {
		Log.d(TAG, "KRA: onGesture " + gestureId);
		return super.onGesture(gestureId);
	}

	@Override
	protected boolean onKeyEvent(KeyEvent event) {
		Log.d(TAG, "KRA:onKeyEvent " + event.getKeyCode());
		return super.onKeyEvent(event);
	}

	@Override
	public void onDestroy() {
		Toast.makeText(getApplicationContext(), "KRA:onDestroy" , Toast.LENGTH_SHORT).show();
		super.onDestroy();
	}



	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		Log.d(TAG, "KRA:onServiceConnected - KRA");

		FingerprintManager manager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

		if (manager.isHardwareDetected())
			Log.d(TAG, "KRA:isHardwareDetected");
		if (manager.hasEnrolledFingerprints())
			Log.d(TAG, "KRA:hasEnrolledFingerprints");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			FingerprintGestureController gestureController = getFingerprintGestureController();

			Toast.makeText(getApplicationContext(), "KRA:Is available: " + gestureController.isGestureDetectionAvailable(), Toast.LENGTH_LONG).show();
			Log.e(TAG, "KRA:Is available: " + gestureController.isGestureDetectionAvailable() );

			FingerprintGestureController.FingerprintGestureCallback callback = new
					FingerprintGestureController.FingerprintGestureCallback() {
						@Override
						public void onGestureDetectionAvailabilityChanged(boolean available) {
							super.onGestureDetectionAvailabilityChanged(available);
							Toast.makeText(getApplicationContext(), "KRA:Gesture available change to: " + available, Toast.LENGTH_SHORT).show();
							Log.d(TAG, "KRA:onGestureDetectionAvailabilityChanged " + available);
						}

						@Override
						public void onGestureDetected(int gesture) {
							super.onGestureDetected(gesture);
							Toast.makeText(getApplicationContext(), "KRA:Gesture: " + gesture, Toast.LENGTH_SHORT).show();
							Log.d(TAG, "KRA:onGestureDetected " + gesture);
						}
					};

			gestureController.registerFingerprintGestureCallback(callback, new Handler());
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "KRA:onUnbind " );
		return super.onUnbind(intent);
	}


}