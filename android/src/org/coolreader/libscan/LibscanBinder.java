package org.coolreader.libscan;

import android.os.Binder;

public class LibscanBinder extends Binder {

	public interface Callback {
		void run(LibscanBinder libscanBinder);
	}

	private final LibscanService mService;

	public LibscanBinder(LibscanService service) {
		mService = service;
	}

}
