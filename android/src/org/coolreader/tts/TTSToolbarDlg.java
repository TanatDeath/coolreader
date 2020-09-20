package org.coolreader.tts;

import java.util.HashMap;
import java.util.Locale;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.CustomLog;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderCommand;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.Selection;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.Utils;
import org.coolreader.crengine.ViewMode;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import org.coolreader.tts.TTS;
import org.coolreader.tts.TTSControlBinder;
import org.coolreader.tts.TTSControlService;
import org.coolreader.tts.TTSControlServiceAccessor;

import com.s_trace.motion_watchdog.HandlerThread;
import com.s_trace.motion_watchdog.MotionWatchdogHandler;

public class TTSToolbarDlg implements TTS.OnUtteranceCompletedListener {
	public static final Logger log = L.create("ttssrv");

	public PopupWindow mWindow;
	public View mAnchor;
	public CoolReader mCoolReader;
	String mLogFileRoot = "";
	int mForceTTSKoef = 0;
	ReaderView mReaderView;
	String mBookTitle;
	public View mPanel;
	TTS mTTS;
	TTSControlServiceAccessor mTTSControl;
	int mCurPage = -1;
	String mCurPageText;
	String mCurPageTextPrev;
	String mCurPageTextPrev2;
	String mCurPageTextNext;
	String mCurPageTextNext2;
	private static NotificationChannel channel;
	ImageButton playPauseButton;
	ImageButton playPauseButtonEll;
	ImageView ivVolDown;
	ImageView ivVolUp;
	ImageView ivFreqDown;
	ImageView ivFreqUp;
	SeekBar sbSpeed;
	SeekBar sbVolume;
	TextView lblMotionWd;
	private HandlerThread mMotionWatchdog;
	private HandlerThread mTimerHandler;
	private static String CHANNEL_ID = "CoolReader_channel";
	private static int MIN_FORCE_TTS_START_TIME = 5000;

	private static final String TAG = TTSToolbarDlg.class.getSimpleName();
	// For TTS notification:
	private static final int TTS_NOTIFICATION_ID = 0; // Use same ID for all TTS notifications
	private static final int COVER_HEIGHT = 256;
	private static final int COVER_WIDTH = 256;
	private FileInfo mFileInfo = null;
	private Bitmap mBookCover = Bitmap.createBitmap(COVER_WIDTH, COVER_HEIGHT, Bitmap.Config.RGB_565);
	private NotificationManager mNotificationManager;
	private MediaSession mMediaSession;

	BroadcastReceiver mTTSControlButtonReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			log.d("received action: " + action);
			if (null != action) {
				switch (action) {
					case TTSControlService.TTS_CONTROL_ACTION_PLAY_PAUSE:
						toggleStartStopExt(false);
						break;
					case TTSControlService.TTS_CONTROL_ACTION_NEXT:
						jumpToSentence(ReaderCommand.DCMD_SELECT_NEXT_SENTENCE);
						break;
					case TTSControlService.TTS_CONTROL_ACTION_PREV:
						jumpToSentence(ReaderCommand.DCMD_SELECT_PREV_SENTENCE);
						break;
					case TTSControlService.TTS_CONTROL_ACTION_DONE:
						stopAndClose();
						break;
				}
			}
		}
	};

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void setMediaSessionPlaybackState(int state) {
		if (mMediaSession == null) {
			return;
		}
		PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
				.setActions(PlaybackState.ACTION_PLAY
						| PlaybackState.ACTION_STOP
						| PlaybackState.ACTION_PAUSE
						| PlaybackState.ACTION_PLAY_PAUSE
						| PlaybackState.ACTION_SKIP_TO_NEXT
						| PlaybackState.ACTION_SKIP_TO_PREVIOUS)
				.setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1);
		mMediaSession.setPlaybackState(stateBuilder.build());
	}

	public NotificationChannel createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (channel == null) {
				NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "CoolReader", NotificationManager.IMPORTANCE_LOW);
				channel.enableLights(false);
				channel.enableVibration(false);
				NotificationManager notificationManager = mCoolReader.getSystemService(NotificationManager.class);
				notificationManager.createNotificationChannel(channel);
			}
		}
		return channel;
	}

	private void jumpToSentence(ReaderCommand dcmdSelectSentence) {
		if ( isSpeaking ) {
			stop(() -> {
				isSpeaking = true;
				moveSelection( dcmdSelectSentence );
			});
		} else moveSelection( dcmdSelectSentence );
	}

	class TimerHandler extends Handler {
		private static final int MSG_TIMER  = 0;
		private final CoolReader mCoolReader;
		private final TTSToolbarDlg mTTSToolbarDlg;
		private HandlerThread mHandlerThread;
		private final int mTimeout;
		private boolean mIsStopping;
		private boolean mIsStopped;

		public TimerHandler(TTSToolbarDlg ttsToolbarDlg, CoolReader coolReader,
									 com.s_trace.motion_watchdog.HandlerThread handlerThread, int timeout) {
			mHandlerThread = handlerThread;
			mCoolReader = coolReader;
			mTTSToolbarDlg = ttsToolbarDlg;
			mTimeout = timeout;
			Message message = Message.obtain();
			message.what = MSG_TIMER;
			sendMessage(message);
		}

		@Override
		public void handleMessage(Message msg) {
			if (mHandlerThread.isInterrupted()) {
				Log.i(TAG, "handleMessage: mHandlerThread.isInterrupted() for msg=" + msg);
				handleInterrupt();
				return;
			}
			CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
					"timer event - every 5 sec");

			long curTimeSpan = System.currentTimeMillis() - lastSaveSpeakTime;
			long curTimeSpanRestart = System.currentTimeMillis() - lastRestartTime;
			double avgWordTimeSpan = 0;
			if (dWordCount>0) {
				avgWordTimeSpan = ((double)lTimeSpan) / dWordCount;
			}
			double thisPhraseExpectedTime = avgWordTimeSpan * dWordCountThis;
			double avgWordTimeSpanThis = 0;
			if (dWordCountThis>0) {
				avgWordTimeSpanThis = ((double)curTimeSpan) / dWordCountThis;
			}
			if ((avgWordTimeSpan>0) && (mForceTTSKoef>0)) {
				//mCoolReader.showToast("checking force "+curTimeSpan);
				boolean needForce = isSpeaking && (iSentenceCount > 20);
				boolean needForceBase = needForce;
				// if there is enough time spent
				if (mForceTTSKoef > 9) {
					if (needForce)
						needForce = needForce &&
								curTimeSpan > thisPhraseExpectedTime + (mForceTTSKoef * 1000);
				} else {
					if (needForce)
						needForce = needForce &&
								curTimeSpan > thisPhraseExpectedTime + (mForceTTSKoef * avgWordTimeSpan * 3);
				}
				//		(avgWordTimeSpanThis > avgWordTimeSpan * ((double)mForceTTSKoef));
				// end min time has reached
				if (needForce)
					needForce = needForce && (curTimeSpan > MIN_FORCE_TTS_START_TIME);
				// if current sentence is empty - just wait min time
				//if (avgWordTimeSpanThis < 0.1)
				//	needForce = needForceBase && (curTimeSpan > MIN_FORCE_TTS_START_TIME);
				//if (mForceTTSKoef>9)
				//	needForce = (curTimeSpan>(mForceTTSKoef * 1000));
				if (needForce) {
                    CustomLog.doLog(mLogFileRoot + (isAlwaysStop ? "log_tts_type1.log" : "log_tts_type0.log"),
                            "possibly tts unexpectedly stopped...");
                    mCoolReader.showToast("Trying to force restart TTS");
					iSentenceCount = 0;
					dWordCount = 0.0f;
					lTimeSpan = 0L;
					if (currentSelection != null) {
                        if (isSpeaking) {
                            if (
                            	 (isAlwaysStop)&&
							       (
								      (curTimeSpanRestart > MIN_FORCE_TTS_START_TIME)||
										  (lastRestartTime==0)
							       )
							   )
                            {
								lastRestartTime = System.currentTimeMillis();
                                //mTTS.stop();
								mCoolReader.tts = null;
								mCoolReader.ttsInitialized = false;

								CustomLog.doLog(mLogFileRoot + (isAlwaysStop ? "log_tts_type1.log" : "log_tts_type0.log"),
                                        "stop tts between reading of sentences");
                            }
                            say(currentSelection);
                        }
                    } else {
                        moveSelection(ReaderCommand.DCMD_SELECT_NEXT_SENTENCE);
                    }
                }
			}
			CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
					"timer event, avgWordTimeSpan = "+avgWordTimeSpan+", avgWordTimeSpanThis = "+avgWordTimeSpanThis+
					", dWordCountThis = "+dWordCountThis+", curTimeSpan = "+curTimeSpan);

			Log.d(TAG, "handleMessage: msg=" + msg);
			if (mCoolReader == null || mIsStopped) {
				return;
			}
			switch (msg.what) {
				case MSG_TIMER:
					mIsStopping = false;
					this.removeMessages(MSG_TIMER);

					Message message = Message.obtain();
					message.what = MSG_TIMER;
					this.sendMessageDelayed(message, mTimeout);
					break;
			}
		}

		private void handleStop() {
			if (mHandlerThread.isInterrupted()) {
				Log.i(TAG, "handleStop: mHandlerThread.isInterrupted()");
				handleInterrupt();
				return;
			}

			Message message = Message.obtain();
			message.what = MSG_TIMER;
			this.sendMessageDelayed(message, mTimeout);
			return;

//			Log.i(TAG, "Final stop");
//			mIsStopped = true;
//			mIsStopping = false;
//			try {
//				Thread.sleep(2000);
//			} catch (InterruptedException e) {
//				handleInterrupt();
//				return;
//			}
//			handleInterrupt();
//			mHandlerThread.interrupt();
		}

		private void handleInterrupt() {
			Log.i(TAG, "handleInterrupt()");
			removeMessages(TimerHandler.MSG_TIMER);
			mHandlerThread.quitSafely();
		}
	}

	static public TTSToolbarDlg showDialog( CoolReader coolReader, ReaderView readerView, TTS tts)
	{
		TTSToolbarDlg dlg = new TTSToolbarDlg(coolReader, readerView, tts);
		//dlg.mWindow.update(dlg.mAnchor, width, height)
		log.d("popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.showAtLocation(readerView, Gravity.LEFT|Gravity.TOP, readerView.getLeft()+50, readerView.getTop()+50);
		//dlg.showAsDropDown(readerView);
		//dlg.update();
		return dlg;
	}

	public void handleTick(int timeLeft, int currentVolume) {
		BackgroundThread.instance().postBackground(new Runnable() {
			@Override
			public void run() {
				BackgroundThread.instance().postGUI(new Runnable() {
					@Override
					public void run() {
						if (lblMotionWd != null) {
							if (timeLeft>0) {
								String left = String.valueOf(timeLeft/60000);
								if (timeLeft<60000) left = "< 1";
								lblMotionWd.setText(mCoolReader.getString(R.string.wd_time_left, left));
								return;
							}
							if (currentVolume == 0) {
								mCoolReader.showToast(R.string.wd_still);
								return;
							}
							lblMotionWd.setText(mCoolReader.getString(R.string.wd_decrease_vol, String.valueOf(currentVolume), String.valueOf(currentVolume-1)));
						}
					}
				}, 200);
			}
		});
	}
	
	private Runnable onCloseListener;
	public void setOnCloseListener(Runnable handler) {
		onCloseListener = handler;
	}

	private boolean closed; 
	public void stopAndClose() {
		CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
				"stop tts (direct)");
		if (closed)
			return;
		isSpeaking = false;
		closed = true;
		BackgroundThread.instance().executeGUI(() -> {
			stop();
			mCoolReader.unregisterReceiver(mTTSControlButtonReceiver);
			if (null != mTTSControl)
				mTTSControl.unbind();
			Intent intent = new Intent(mCoolReader, TTSControlService.class);
			mCoolReader.stopService(intent);
			restoreReaderMode();
			mReaderView.clearSelection();
			if (onCloseListener != null)
				onCloseListener.run();
			if ( mWindow.isShowing() )
				mWindow.dismiss();
			mReaderView.save();
		});
		if (mNotificationManager != null) {
			mNotificationManager.cancel(TTS_NOTIFICATION_ID);
		}
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mMediaSession.setActive(false);
				mMediaSession.release();
			}
		} catch (Throwable ignored) {
		}
	}
	
	private boolean changedPageMode;

	// plotn: NB!!! same fix as we did, but in other way. Check if it works
	private void setReaderMode()
	{
		String oldViewSetting = mReaderView.getSetting( ReaderView.PROP_PAGE_VIEW_MODE );
		if ( "1".equals(oldViewSetting) ) {
			changedPageMode = true;
			//mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE, "0");
			//mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE_AUTOCHANGED, "1");
			mReaderView.setViewModeNonPermanent(ViewMode.SCROLL);
		}
		moveSelection( ReaderCommand.DCMD_SELECT_FIRST_SENTENCE );
	}
	
	private void restoreReaderMode()
	{
		if ( changedPageMode ) {
			//mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE, "1");
			//mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE_AUTOCHANGED, "0");
			mReaderView.setViewModeNonPermanent(ViewMode.PAGES);
		}
	}
	
	private Selection currentSelection;
	
	private void moveSelection( ReaderCommand cmd )
	{
		mReaderView.moveSelection(cmd, 0, new ReaderView.MoveSelectionCallback() {
			
			@Override
			public void onNewSelection(Selection selection) {
				log.d("onNewSelection: " + selection.text);
				curTime = System.currentTimeMillis();
				long interv = mReaderView.getDefSavePositionIntervalSpeak();
				if (interv == 0)
                    interv = mReaderView.getDefSavePositionInterval();
				if (curTime - lastSaveCurPosTime > interv) {
					CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
							"it is time to save position");
					lastSaveCurPosTime = curTime;
					try {
						final Bookmark bmk = mReaderView.getCurrentPositionBookmark();
						if (bmk != null) mReaderView.savePositionBookmark(bmk);
					} catch (Exception e) {
						L.e("couldn't save current position");
					}
				}
				currentSelection = selection;
				if ( isSpeaking ) {
					if (isAlwaysStop) {
						mTTS.stop();
						CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
								"stop tts between reading of sentences");
					}
					say(currentSelection);
				}
			}
			
			@Override
			public void onFail() {
				log.e("fail()");
				stop();
				//currentSelection = null;
			}
		});
	}
	
	private void say( Selection selection ) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TTS.KEY_PARAM_UTTERANCE_ID, "cr3UtteranceId");
		CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
				"speaking: "+selection.text);
		dWordCountThis = 0.0f;
		for (String sWord: selection.text.split(" ")) {
			if (sWord.length()<4) dWordCountThis = dWordCountThis + 0.8d;
				else if (sWord.length()<7) dWordCountThis = dWordCountThis + 1d;
			else dWordCountThis = dWordCountThis + 1.2d;
			if (sWord.replaceAll("\\p{Punct}", "").length()==0) dWordCountThis = 0;
		}
		lastSaveSpeakTime = System.currentTimeMillis();
		// text clearance
		String clearText = selection.text;
		if ((mCoolReader.getmReaderFrame()!=null) && (mCoolReader.getReaderView() != null))
			if ((mCoolReader.getmReaderFrame().getUserDicPanel()!=null)&&
					(mCoolReader.getReaderView().getDoc() !=null)) {
				int curPage = mCoolReader.getReaderView().getDoc().getCurPage();
				if (mCurPage != curPage) {
					//mCoolReader.showToast("asdf "+mCurPage);
					mCurPageTextPrev2 = mCoolReader.getmReaderFrame().getUserDicPanel().getCurPageText(-2,true);
					mCurPageTextPrev = mCoolReader.getmReaderFrame().getUserDicPanel().getCurPageText(-1,true);
					mCurPageText = mCoolReader.getmReaderFrame().getUserDicPanel().getCurPageText(0,true);
					mCurPageTextNext = mCoolReader.getmReaderFrame().getUserDicPanel().getCurPageText(1,true);
					mCurPageTextNext2 = mCoolReader.getmReaderFrame().getUserDicPanel().getCurPageText(2,true);
					mCurPage = curPage;
				}
				if ((mCurPageText.contains("["+clearText+"]")) || (mCurPageText.contains("{"+clearText+"}")))
					clearText = "";
				if ((mCurPageTextPrev.contains("["+clearText+"]")) || (mCurPageTextPrev.contains("{"+clearText+"}")))
					clearText = "";
				if ((mCurPageTextNext.contains("["+clearText+"]")) || (mCurPageTextNext.contains("{"+clearText+"}")))
					clearText = "";
				if ((mCurPageTextPrev2.contains("["+clearText+"]")) || (mCurPageTextPrev2.contains("{"+clearText+"}")))
					clearText = "";
				if ((mCurPageTextNext2.contains("["+clearText+"]")) || (mCurPageTextNext2.contains("{"+clearText+"}")))
					clearText = "";
			}
		// this won't work but I let it...
		clearText = clearText.replaceAll("\\[\\d+\\]", "");
		clearText = clearText.replaceAll("\\{\\d+\\}", "");
		//mCoolReader.showToast(mCurPageText);
		//mCoolReader.showToast(clearText);
		//\
		mTTS.speak(clearText, TTS.QUEUE_ADD, params);
		runInTTSControlService(tts -> tts.notifyPlay(mBookTitle, currentSelection.text));
	}
	
	private void start() {
		if ( currentSelection==null )
			return;
		startMotionWatchdog();
		isSpeaking = true;
		say( currentSelection );
	}

	private void startMotionWatchdog(){
		String TAG = "MotionWatchdog";
		log.d("startMotionWatchdog() enter");

		Properties settings = mReaderView.getSettings();
		int timeout = settings.getInt(ReaderView.PROP_APP_MOTION_TIMEOUT, 0);
		if (timeout == 0) {
			Log.d(TAG, "startMotionWatchdog() early exit - timeout is 0");
			return;
		}
		timeout = timeout * 60 * 1000; // Convert minutes to msecs
		mMotionWatchdog = new HandlerThread("MotionWatchdog");
		mMotionWatchdog.start();
		new MotionWatchdogHandler(this, mCoolReader, mMotionWatchdog, timeout);
		Log.d(TAG, "startMotionWatchdog() exit");
	}
	
	private boolean isSpeaking;
	private Runnable mOnStopRunnable;
	private boolean isAlwaysStop;
	private int iSentenceCount;
	private double dWordCount;
	private double dWordCountThis;
	private long lTimeSpan;
	private long curTime = System.currentTimeMillis();
	private long lastSaveSpeakTime = System.currentTimeMillis();
	private long lastRestartTime = 0;
	private long lastSaveCurPosTime = System.currentTimeMillis();

	private void stop() {
		stop(null);
	}

	private void stop(Runnable runnable) {
		isSpeaking = false;
		mOnStopRunnable = runnable;
		if ( mTTS.isSpeaking() ) {
			mTTS.stop();
		}
		if (mMotionWatchdog != null) {
			mMotionWatchdog.interrupt();
		}
	}

	public void pause() {
		if (isSpeaking)
			toggleStartStopExt(false);
	}

	private void toggleStartStopExt(boolean b) {
		isAlwaysStop = b;
		toggleStartStop();
	}

	private void toggleStartStop() {

		iSentenceCount = 0;
		dWordCount = 0.0f;
		lTimeSpan = 0L;

		CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
				isSpeaking?"stop tts":"start tts");
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2});
		colorGrayC = a.getColor(0, Color.GRAY);
		a.recycle();

		ColorDrawable c = new ColorDrawable(colorGrayC);
		c.setAlpha(130);

		if ( isSpeaking ) {
			playPauseButton.setImageResource(
					Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play, R.drawable.ic_media_play)
					//R.drawable.ic_media_play
			);
			playPauseButtonEll.setImageResource(
					Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play_ell, R.drawable.icons8_play_ell)
					//R.drawable.ic_media_play
			);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				setMediaSessionPlaybackState(PlaybackState.STATE_STOPPED);
			}
			playPauseButton.setBackgroundDrawable(c);
			playPauseButtonEll.setBackgroundDrawable(c);
			runInTTSControlService(tts -> tts.notifyPause(mBookTitle));
			stop();
		} else {
			playPauseButton.setImageResource(
					Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_pause, R.drawable.ic_media_pause)
					//R.drawable.ic_media_pause
			);
			playPauseButtonEll.setImageResource(
					Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_pause, R.drawable.ic_media_pause)
					//R.drawable.ic_media_pause
			);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				setMediaSessionPlaybackState(PlaybackState.STATE_PLAYING);
			}
			playPauseButton.setBackgroundDrawable(c);
			playPauseButtonEll.setBackgroundDrawable(c);
			runInTTSControlService(tts -> tts.notifyPlay(mBookTitle, currentSelection.text));
			start();
		}
	}
	
	@Override
	public void onUtteranceCompleted(String utteranceId) {
		Log.d("cr3", "onUtteranceCompleted " + utteranceId);
		CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
				isSpeaking?"utterance complete, while is speaking":"utterance complete, while NOT is speaking");
		if (null != mOnStopRunnable) {
			mOnStopRunnable.run();
			mOnStopRunnable = null;
		} else {
			if (isSpeaking) {
				if ((iSentenceCount < 50) && (dWordCountThis > 5)) {
					dWordCount = dWordCount + dWordCountThis;
					iSentenceCount = iSentenceCount + 1;
					lTimeSpan = lTimeSpan + System.currentTimeMillis() - lastSaveSpeakTime;
				}
				moveSelection(ReaderCommand.DCMD_SELECT_NEXT_SENTENCE);
			}
		}
	}

	private void runInTTSControlService(TTSControlBinder.Callback callback) {
		if (null == mTTSControl) {
			mTTSControl = new TTSControlServiceAccessor(mCoolReader);
		}
		mTTSControl.bind(callback);
	}

	public void repaintButtons()
	{
		int colorGrayC;
		int colorGray;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorGray = a.getColor(1, Color.GRAY);
		a.recycle();

		ColorDrawable c = new ColorDrawable(colorGrayC);
		c.setAlpha(130);
		if (mPanel!=null) {
			mPanel.findViewById(R.id.tts_play_pause).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_play_pause_ell).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_back).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_forward).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_stop).setBackgroundDrawable(c);
		}
	}

	public TTSToolbarDlg( CoolReader coolReader, ReaderView readerView, TTS tts )
	{
		mCoolReader = coolReader;
        mLogFileRoot = mCoolReader.getSettingsFileF(0).getParent() + "/";
        mForceTTSKoef = readerView.getSettings().getInt(Settings.PROP_APP_TTS_FORCE_KOEF, 0);
        mReaderView = readerView;
		mAnchor = readerView.getSurface();
		mTTS = tts;
		mTTS.setOnUtteranceCompletedListener(this);

		mTimerHandler = new HandlerThread("TimerHandler");
		mTimerHandler.start();
		new TimerHandler(this, mCoolReader, mTimerHandler, 5000);

		int colorGrayC;
		int colorGray;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorGray = a.getColor(1, Color.GRAY);
		a.recycle();

		ColorDrawable c = new ColorDrawable(colorGrayC);
		c.setAlpha(130);

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.tts_toolbar, null));
		lblMotionWd = (TextView) panel.findViewById(R.id.lbl_motion_wd);
		lblMotionWd.setText(R.string.wd_sett);
		lblMotionWd.setPaintFlags(lblMotionWd.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		lblMotionWd.setOnClickListener(v -> mCoolReader.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_APP_MOTION_TIMEOUT));
		playPauseButton = panel.findViewById(R.id.tts_play_pause);
		playPauseButton.setImageResource(
				Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play, R.drawable.ic_media_play)
				//R.drawable.ic_media_play
		);
		playPauseButton.setBackgroundDrawable(c);
		playPauseButtonEll = panel.findViewById(R.id.tts_play_pause_ell);
		playPauseButtonEll.setImageResource(
				Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play_ell, R.drawable.icons8_play_ell)
				//R.drawable.ic_media_play
		);
		playPauseButtonEll.setBackgroundDrawable(c);
		panel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		mWindow = new PopupWindow( mAnchor.getContext() );
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mPanel = panel;
		mPanel.findViewById(R.id.tts_play_pause).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.tts_play_pause).setOnClickListener(v -> toggleStartStopExt(false));
		mPanel.findViewById(R.id.tts_play_pause_ell).setOnClickListener(v -> toggleStartStopExt(true));
		mPanel.findViewById(R.id.tts_play_pause).setOnLongClickListener(v -> {
			mCoolReader.tts = null;
			mCoolReader.ttsInitialized = false;
			mCoolReader.showToast("Re-initializing TTS");
			return true;
		});
		mPanel.findViewById(R.id.tts_play_pause_ell).setOnLongClickListener(v -> {
			mCoolReader.tts = null;
			mCoolReader.ttsInitialized = false;
			mCoolReader.showToast("Re-initializing TTS");
			return true;
		});
		mPanel.findViewById(R.id.tts_back).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.tts_back).setOnClickListener(v -> jumpToSentence( ReaderCommand.DCMD_SELECT_PREV_SENTENCE ));
		mPanel.findViewById(R.id.tts_forward).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.tts_forward).setOnClickListener(v -> jumpToSentence( ReaderCommand.DCMD_SELECT_NEXT_SENTENCE ));
		mPanel.findViewById(R.id.tts_stop).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.tts_stop).setOnClickListener(v -> stopAndClose());
		mPanel.setFocusable(true);
		mPanel.setEnabled(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if ( event.getAction()==KeyEvent.ACTION_UP ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_VOLUME_DOWN:
				case KeyEvent.KEYCODE_VOLUME_UP:
					return true;
				case KeyEvent.KEYCODE_BACK:
					stopAndClose();
					return true;
				}
			} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_VOLUME_DOWN: {
					int p = sbVolume.getProgress() - 5;
					if ( p<0 )
						p = 0;
					sbVolume.setProgress(p);
					return true;
				}
				case KeyEvent.KEYCODE_VOLUME_UP:
					int p = sbVolume.getProgress() + 5;
					if ( p>100 )
						p = 100;
					sbVolume.setProgress(p);
					return true;
				}
				if ( keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
			}
			return false;
		});

		mWindow.setOnDismissListener(() -> {
			if ( !closed )
				stopAndClose();
		});
		
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		panel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
		mWindow.setContentView(panel);
		
		
		int [] location = new int[2];
		mAnchor.getLocationOnScreen(location);

		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], location[1] + mAnchor.getHeight() - mPanel.getHeight());

		setReaderMode();

		// setup speed && volume seek bars
		sbSpeed = mPanel.findViewById(R.id.tts_sb_speed);
		sbVolume = mPanel.findViewById(R.id.tts_sb_volume);

		ivVolDown = mPanel.findViewById(R.id.btn_vol_down);
		ivVolUp = mPanel.findViewById(R.id.btn_vol_up);
		ivFreqDown = mPanel.findViewById(R.id.btn_freq_down);
		ivFreqUp = mPanel.findViewById(R.id.btn_freq_up);
		ivVolDown.setBackgroundDrawable(c);
		ivVolUp.setBackgroundDrawable(c);
		ivFreqDown.setBackgroundDrawable(c);
		ivFreqUp.setBackgroundDrawable(c);

		sbSpeed.setMax(100);
		sbSpeed.setProgress(50);
		sbVolume.setMax(100);
		sbVolume.setProgress(mCoolReader.getVolume());

		ivFreqDown.setOnClickListener(v -> {
			int progress = sbSpeed.getProgress();
			if (progress>10) progress=progress-10; else progress = 0;
			sbSpeed.setProgress(progress);
			float rate = 1.0f;
			if ( progress<50 )
				rate = 0.3f + 0.7f * progress / 50f;
			else
				rate = 1.0f + 2.5f * (progress-50) / 50f;
			mTTS.setSpeechRate(rate);
		});
		ivFreqUp.setOnClickListener(v -> {
			int progress = sbSpeed.getProgress();
			if (progress<100) progress=progress+10; else progress = 100;
			sbSpeed.setProgress(progress);
			float rate = 1.0f;
			if ( progress<50 )
				rate = 0.3f + 0.7f * progress / 50f;
			else
				rate = 1.0f + 2.5f * (progress-50) / 50f;
			mTTS.setSpeechRate(rate);
		});

		sbSpeed.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				float rate = 1.0f;
				if ( progress<50 )
					rate = 0.3f + 0.7f * progress / 50f;
				else
					rate = 1.0f + 2.5f * (progress-50) / 50f;
				mTTS.setSpeechRate(rate);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		ivVolDown.setOnClickListener(v -> {
			int progress = sbVolume.getProgress();
			if (progress>10) progress=progress-10; else progress = 0;
			sbVolume.setProgress(progress);
			mCoolReader.setVolume(progress);
		});
		ivVolUp.setOnClickListener(v -> {
			int progress = sbVolume.getProgress();
			if (progress<100) progress=progress+10; else progress = 100;
			sbVolume.setProgress(progress);
			mCoolReader.setVolume(progress);
		});

		sbVolume.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mCoolReader.setVolume(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		mCoolReader.tintViewIcons(mPanel);
		mPanel.requestFocus();
		BookInfo bookInfo = mReaderView.getBookInfo();
		if (null != bookInfo) {
			FileInfo fileInfo = bookInfo.getFileInfo();
			if (null != fileInfo) {
				mBookTitle = fileInfo.title;
				// set language for TTS based on book's language
				log.d("book language is \"" + fileInfo.language + "\"");
				if (null != fileInfo.language && fileInfo.language.length() > 0) {
					Locale locale = new Locale(fileInfo.language);
					log.d("trying to set TTS language to \"" + locale.getDisplayLanguage() + "\"");
					mTTS.setLanguage(locale);
				}
			}
		}

		// Start the foreground service to make this app also foreground,
		// even if the main activity is in the background.
		// https://developer.android.com/about/versions/oreo/background#services
		Intent intent = new Intent(coolReader, TTSControlService.class);
		Bundle data = new Bundle();
		data.putString("bookTitle", mBookTitle);
		intent.putExtras(data);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			coolReader.startForegroundService(intent);
		else
			coolReader.startService(intent);
		IntentFilter filter = new IntentFilter();
		filter.addAction(TTSControlService.TTS_CONTROL_ACTION_PLAY_PAUSE);
		filter.addAction(TTSControlService.TTS_CONTROL_ACTION_NEXT);
		filter.addAction(TTSControlService.TTS_CONTROL_ACTION_PREV);
		filter.addAction(TTSControlService.TTS_CONTROL_ACTION_DONE);
		mCoolReader.registerReceiver(mTTSControlButtonReceiver, filter);
	}
	
}
