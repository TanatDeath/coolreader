package org.coolreader.libscan;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

public class LibscanServiceAccessor {
	private final static String TAG = "libscansrv";
	private final Activity mActivity;
	private volatile LibscanBinder mServiceBinder;
	private volatile boolean mServiceBound;
	private volatile boolean bindIsCalled;
	private final ArrayList<LibscanBinder.Callback> onConnectCallbacks = new ArrayList<>();
	private final Object mLocker = new Object();

	public interface Callback {
		void run(LibscanServiceAccessor libscanacc);
	}

	public LibscanServiceAccessor(Activity activity) {
		mActivity = activity;
	}

	public void bind(final LibscanBinder.Callback boundCallback) {
		synchronized (this) {
			if (mServiceBinder != null && mServiceBound) {
				Log.v(TAG, "LibscanService is already bound");
				if (boundCallback != null)
					boundCallback.run(mServiceBinder);
				return;
			}
		}
		if (boundCallback != null) {
			synchronized (mLocker) {
				onConnectCallbacks.add(boundCallback);
			}
		}
		if (!bindIsCalled) {
			bindIsCalled = true;
			if (mActivity.bindService(new Intent(mActivity, LibscanService.class), mServiceConnection, Context.BIND_AUTO_CREATE)) {
				mServiceBound = true;
				Log.v(TAG, "binding LibscanService in progress...");
			} else {
				Log.e(TAG, "cannot bind LibscanService");
			}
		}
	}

	public void unbind() {
		Log.v(TAG, "unbinding LibscanService");
		if (mServiceBound) {
			// Detach our existing connection.
			mActivity.unbindService(mServiceConnection);
			mServiceBound = false;
			bindIsCalled = false;
			mServiceBinder = null;
		}
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			synchronized (LibscanServiceAccessor.this) {
				mServiceBinder = ((LibscanBinder) service);
				Log.i(TAG, "connected to LibscanService");
			}
			synchronized (mLocker) {
				if (onConnectCallbacks.size() != 0) {
					// run once
					for (LibscanBinder.Callback callback : onConnectCallbacks)
						callback.run(mServiceBinder);
					onConnectCallbacks.clear();
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			synchronized (LibscanServiceAccessor.this) {
				mServiceBound = false;
				bindIsCalled = false;
				mServiceBinder = null;
			}
			Log.i(TAG, "Connection to the LibscanService has been lost");
		}
	};

}
