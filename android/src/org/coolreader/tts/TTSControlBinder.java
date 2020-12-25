package org.coolreader.tts;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;

import org.coolreader.crengine.BookInfo;

public class TTSControlBinder extends Binder {

	public interface Callback {
		void run(TTSControlBinder tts);
	}

	private TTSControlService mService;

	public TTSControlBinder(TTSControlService service) {
		mService = service;
	}

	public void notifyPlay(String title, String sentence) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR)
			mService.notifyPlay(title, sentence);
	}

	public void notifyPause(String title) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR)
			mService.notifyPause(title);
	}

	public void notifyStartMediaSession(BookInfo bookInfo, Bitmap bitmap) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			mService.notifyStartMediaSession(bookInfo, bitmap);
	}

	public void notifyStopMediaSession() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			mService.notifyStopMediaSession();
	}

}
