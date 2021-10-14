package org.coolreader.libscan;

import android.content.Intent;
import android.os.IBinder;

import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.db.BaseService;

public class LibscanService extends BaseService {

	public static final Logger log = L.create("libscansvc");
	private final LibscanBinder mBinder = new LibscanBinder(this);

	public LibscanService() {
		super("libscan");
	}

	@Override
	public IBinder onBind(Intent intent) {
		log.i("onBind(): " + intent);
		return mBinder;
	}

	@Override
	public void onCreate() {
		log.d("onCreate");
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// do nothing
	}


}
