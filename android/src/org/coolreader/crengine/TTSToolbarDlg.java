package org.coolreader.crengine;

import java.util.HashMap;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.s_trace.motion_watchdog.HandlerThread;
import com.s_trace.motion_watchdog.MotionWatchdogHandler;

import static android.media.session.MediaSession.*;

public class TTSToolbarDlg implements TTS.OnUtteranceCompletedListener {
	PopupWindow mWindow;
	View mAnchor;
	CoolReader mCoolReader;
	String mLogFileRoot = "";
	int mForceTTSKoef = 0;
	ReaderView mReaderView;
	View mPanel;
	TTS mTTS;
	private static NotificationChannel channel;
	ImageButton playPauseButton;
	ImageButton playPauseButtonEll;
	ImageView ivVolDown;
	ImageView ivVolUp;
	ImageView ivFreqDown;
	ImageView ivFreqUp;
	SeekBar sbSpeed;
	SeekBar sbVolume;
	private HandlerThread mMotionWatchdog;
	private HandlerThread mTimerHandler;
	private static String CHANNEL_ID = "CoolReader_channel";
	private static int MIN_FORCE_TTS_START_TIME = 5000;

	private static final String TAG = TTSToolbarDlg.class.getSimpleName();
	// For TTS notification:
	private static final String ACTION_TTS_PREV = "org.coolreader.crengine.tts.prev";
	private static final String ACTION_TTS_PLAY = "org.coolreader.crengine.tts.play";
	private static final String ACTION_TTS_PAUSE = "org.coolreader.crengine.tts.pause";
	private static final String ACTION_TTS_NEXT = "org.coolreader.crengine.tts.next";
	private static final String ACTION_TTS_STOP = "org.coolreader.crengine.tts.stop";
	private static final int TTS_NOTIFICATION_ID = 0; // Use same ID for all TTS notifications
	private static final int COVER_HEIGHT = 256;
	private static final int COVER_WIDTH = 256;
	private FileInfo mFileInfo = null;
	private Bitmap mBookCover = Bitmap.createBitmap(COVER_WIDTH, COVER_HEIGHT, Bitmap.Config.RGB_565);
	private NotificationManager mNotificationManager;
	private MediaSession mMediaSession;
	private BroadcastReceiver mTtsControlReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "mTtsControlReceiver onReceive: got " + intent);
			if (action == null) {
				Log.w(TAG, "mTtsControlReceiver onReceive: got intent with null action!");
				return;
			}
			switch (action) {
				case ACTION_TTS_PREV:
					jumpToSentence(ReaderCommand.DCMD_SELECT_PREV_SENTENCE);
					break;
				case ACTION_TTS_PLAY:
					if (!isSpeaking) {
						toggleStartStopExt(false);
					}
					break;
				case ACTION_TTS_PAUSE:
					if (isSpeaking) {
						toggleStartStopExt(false);
					}
					break;
				case ACTION_TTS_NEXT:
					jumpToSentence(ReaderCommand.DCMD_SELECT_NEXT_SENTENCE);
					break;
				case ACTION_TTS_STOP:
					stopAndClose();
					break;
				default:
					Log.w(TAG, "mTtsControlReceiver onReceive: got unexpected " + intent);
					break;
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

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void displayNotification(boolean isSpeaking, String sText) {
		createNotificationChannel();
		mNotificationManager = (NotificationManager)
				mCoolReader.getSystemService(Context.NOTIFICATION_SERVICE);
		if (mNotificationManager == null) {
			Log.i(TAG, "displayNotification: can't get NotificationService");
			return;
		}
		Notification.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder = new Notification.Builder(mCoolReader, CHANNEL_ID);
		} else {
			builder = new Notification.Builder(mCoolReader);
		}
		// Show controls on lock screen even when user hides sensitive content.
		builder.setVisibility(Notification.VISIBILITY_PUBLIC);
		builder.setSmallIcon(R.drawable.cr3_logo);
		// Add media control buttons that invoke intents in your media service
		PendingIntent prevPendingIntent = PendingIntent.getBroadcast(mCoolReader, 0,
				new Intent(ACTION_TTS_PREV), 0);
		PendingIntent playPendingIntent = PendingIntent.getBroadcast(mCoolReader, 1,
				new Intent(ACTION_TTS_PLAY), 0);
		PendingIntent pausePendingIntent = PendingIntent.getBroadcast(mCoolReader, 1,
				new Intent(ACTION_TTS_PAUSE), 0);
		PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mCoolReader, 2,
				new Intent(ACTION_TTS_NEXT), 0);
		PendingIntent stopPendingIntent = PendingIntent.getBroadcast(mCoolReader, 3,
				new Intent(ACTION_TTS_STOP), 0);
		builder.addAction(R.drawable.ic_media_rew, "Previous", prevPendingIntent); // #0
		if (isSpeaking) {
			builder.addAction(R.drawable.ic_media_pause, "Pause", pausePendingIntent); // #1
		} else {
			builder.addAction(R.drawable.ic_media_play, "Play", playPendingIntent); // #1
		}
		builder.addAction(R.drawable.ic_media_ff, "Next", nextPendingIntent);  // #2
		builder.addAction(R.drawable.ic_media_stop, "Stop", stopPendingIntent);  // #3
		// Apply the media style template
		builder.setStyle(new Notification.MediaStyle()
				.setShowActionsInCompactView(1 /* #1: play or pause button */)
				.setMediaSession(mMediaSession.getSessionToken())
		);
		if (StrUtils.isEmptyStr(sText)) {
			builder.setContentTitle(mFileInfo.getTitle());
			builder.setContentText(mFileInfo.getAuthors());
		} else {
			builder.setContentTitle(mFileInfo.getTitle() + " - " + mFileInfo.getAuthors());
			builder.setContentText(sText);
		}
		builder.setLargeIcon(mBookCover);
		mNotificationManager.notify(TTS_NOTIFICATION_ID, builder.build());
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void startMediaSession() {
		final BookInfo bookInfo = mReaderView.getBookInfo();
		if (bookInfo == null) {
			Log.w(TAG, "startMediaSession: mReaderView.getBookInfo() returned null!");
			return;
		}
		mMediaSession = new MediaSession(mCoolReader, "TTS");
		mMediaSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS|FLAG_HANDLES_TRANSPORT_CONTROLS);
		mMediaSession.setCallback(new Callback() {
			@Override
			public void onPlay() {
				Log.i(TAG, "onPlay");
				mCoolReader.sendBroadcast(new Intent(ACTION_TTS_PLAY));
			}
			@Override
			public void onPause() {
				Log.i(TAG, "onPause");
				mCoolReader.sendBroadcast(new Intent(ACTION_TTS_PAUSE));
			}
			@Override
			public void onSkipToNext() {
				Log.i(TAG, "onSkipToNext");
				mCoolReader.sendBroadcast(new Intent(ACTION_TTS_NEXT));
			}
			@Override
			public void onSkipToPrevious() {
				Log.i(TAG, "onSkipToPrevious");
				mCoolReader.sendBroadcast(new Intent(ACTION_TTS_PREV));
			}
			@Override
			public void onStop() {
				Log.i(TAG, "onStop");
				mCoolReader.sendBroadcast(new Intent(ACTION_TTS_STOP));
			}
		});
		mFileInfo = bookInfo.getFileInfo();
		Services.getCoverpageManager().drawCoverpageFor(mCoolReader.getDB(), mFileInfo, mBookCover,
				new CoverpageManager.CoverpageBitmapReadyListener() {
					@Override
					public void onCoverpageReady(CoverpageManager.ImageItem file, Bitmap bitmap) {
						mBookCover = bitmap;
						displayNotification(isSpeaking, "");
						MediaMetadata metadata = new MediaMetadata.Builder()
								.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
								.putString(MediaMetadata.METADATA_KEY_TITLE, mFileInfo.getTitle())
								.putString(MediaMetadata.METADATA_KEY_ARTIST, mFileInfo.getAuthors())
								.build();
						mMediaSession.setMetadata(metadata);
						setMediaSessionPlaybackState(PlaybackState.STATE_STOPPED);
						mMediaSession.setActive(true);
					}
				}
		);
	}
	private void jumpToSentence(ReaderCommand dcmdSelectSentence) {
		if (isSpeaking) {
			isSpeaking = false;
			mTTS.stop();
			isSpeaking = true;
		}
		moveSelection(dcmdSelectSentence);
	}
	private void registerTtsControlReceiver() {
		IntentFilter intentFilter = new IntentFilter(ACTION_TTS_PREV);
		intentFilter.addAction(ACTION_TTS_PLAY);
		intentFilter.addAction(ACTION_TTS_PAUSE);
		intentFilter.addAction(ACTION_TTS_NEXT);
		intentFilter.addAction(ACTION_TTS_STOP);
		mCoolReader.registerReceiver(mTtsControlReceiver, intentFilter);
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
			double avgWordTimeSpan = 0;
			if (dWordCount>0) {
				avgWordTimeSpan = ((double)lTimeSpan) / dWordCount;
			}
			double avgWordTimeSpanThis = 0;
			if (dWordCountThis>0) {
				avgWordTimeSpanThis = ((double)curTimeSpan) / dWordCountThis;
			}
			if ((avgWordTimeSpan>0) && (mForceTTSKoef>0)) {
				boolean needForce = isSpeaking && (iSentenceCount > 20);
				boolean needForceBase = needForce;
				// if there is enough time spent
				if (needForce)
					needForce = needForce && (avgWordTimeSpanThis > avgWordTimeSpan * mForceTTSKoef);
				// end min time has reached
				if (needForce)
					needForce = needForce && (curTimeSpan > MIN_FORCE_TTS_START_TIME);
				// if current sentence is empty - just wait min time
				if (avgWordTimeSpanThis < 0.1)
					needForce = needForceBase && (curTimeSpan > MIN_FORCE_TTS_START_TIME);
				if (needForce) {
                    CustomLog.doLog(mLogFileRoot + (isAlwaysStop ? "log_tts_type1.log" : "log_tts_type0.log"),
                            "possibly tts unexpectedly stopped...");
                    mCoolReader.showToast("Trying to force restart TTS");
					iSentenceCount = 0;
					dWordCount = 0.0f;
					lTimeSpan = 0L;
					if (currentSelection != null) {
                        if (isSpeaking) {
                            if (isAlwaysStop) {
                                mTTS.stop();
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
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.showAtLocation(readerView, Gravity.LEFT|Gravity.TOP, readerView.getLeft()+50, readerView.getTop()+50);
		//dlg.showAsDropDown(readerView);
		//dlg.update();
		return dlg;
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
		BackgroundThread.instance().executeGUI(new Runnable() {
			@Override
			public void run() {
				stop();
				restoreReaderMode();
				mReaderView.clearSelection();
				if (onCloseListener != null)
					onCloseListener.run();
				if ( mWindow.isShowing() )
					mWindow.dismiss();
				mReaderView.save();
			}
		});
		mCoolReader.unregisterReceiver(mTtsControlReceiver);
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
	private void setReaderMode()
	{
		String oldViewSetting = mReaderView.getSetting( ReaderView.PROP_PAGE_VIEW_MODE );
		if ( "1".equals(oldViewSetting) ) {
			changedPageMode = true;
			mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE, "0");
			mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE_AUTOCHANGED, "1");
		}
		moveSelection( ReaderCommand.DCMD_SELECT_FIRST_SENTENCE );
	}
	
	private void restoreReaderMode()
	{
		if ( changedPageMode ) {
			mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE, "1");
			mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE_AUTOCHANGED, "0");
		}
	}
	
	private Selection currentSelection;
	
	private void moveSelection( ReaderCommand cmd )
	{
		mReaderView.moveSelection(cmd, 0, new ReaderView.MoveSelectionCallback() {
			
			@Override
			public void onNewSelection(Selection selection) {
				Log.d("cr3", "onNewSelection: " + selection.text);
				curTime = System.currentTimeMillis();
				if (curTime - lastSaveCurPosTime > mReaderView.getDefSavePositionInterval()) {
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
				Log.d("cr3", "fail()");
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
		displayNotification(isSpeaking,selection.text);
		mTTS.speak(selection.text, TTS.QUEUE_ADD, params);
	}
	
	private void start() {
		if ( currentSelection==null )
			return;
		startMotionWatchdog();
		isSpeaking = true;
		say( currentSelection );
	}

	private void startMotionWatchdog(){
		Log.d(TAG, "startMotionWatchdog() enter");

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
	private boolean isAlwaysStop;
	private int iSentenceCount;
	private double dWordCount;
	private double dWordCountThis;
	private long lTimeSpan;
	private long curTime = System.currentTimeMillis();
	private long lastSaveSpeakTime = System.currentTimeMillis();
	private long lastSaveCurPosTime = System.currentTimeMillis();

	private void stop() {
		isSpeaking = false;
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
			start();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			displayNotification(isSpeaking, "");
		}
	}
	
	@Override
	public void onUtteranceCompleted(String utteranceId) {
		Log.d("cr3", "onUtteranceCompleted " + utteranceId);
		CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
				isSpeaking?"utterance complete, while is speaking":"utterance complete, while NOT is speaking");
		if ( isSpeaking ) {
			if ((iSentenceCount<50) && (dWordCountThis>5)) {
				dWordCount = dWordCount + dWordCountThis;
				iSentenceCount = iSentenceCount + 1;
				lTimeSpan = lTimeSpan + System.currentTimeMillis() - lastSaveSpeakTime;
			}
			moveSelection(ReaderCommand.DCMD_SELECT_NEXT_SENTENCE);
		}
	}

	public TTSToolbarDlg( CoolReader coolReader, ReaderView readerView, TTS tts )
	{
		mCoolReader = coolReader;
        mLogFileRoot = mCoolReader.getSettingsFile(0).getParent() + "/";
        mForceTTSKoef = readerView.getSettings().getInt(Settings.PROP_APP_TTS_FORCE_KOEF, 0);
        mReaderView = readerView;
		mAnchor = readerView.getSurface();
		mTTS = tts;
		mTTS.setOnUtteranceCompletedListener(this);

		mTimerHandler = new HandlerThread("TimerHandler");
		mTimerHandler.start();
		new TimerHandler(this, mCoolReader, mTimerHandler, 5000);

		registerTtsControlReceiver();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			startMediaSession();
		}

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
		playPauseButton = (ImageButton)panel.findViewById(R.id.tts_play_pause);
		playPauseButton.setImageResource(
				Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play, R.drawable.ic_media_play)
				//R.drawable.ic_media_play
		);
		playPauseButton.setBackgroundDrawable(c);
		playPauseButtonEll = (ImageButton)panel.findViewById(R.id.tts_play_pause_ell);
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
		mPanel.findViewById(R.id.tts_play_pause).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleStartStopExt(false);
			}
		});
		mPanel.findViewById(R.id.tts_play_pause_ell).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleStartStopExt(true);
			}
		});
		mPanel.findViewById(R.id.tts_play_pause).setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				mCoolReader.tts = null;
				mCoolReader.ttsInitialized = false;
				mCoolReader.showToast("Re-initializing TTS");
				return true;
			}
		});
		mPanel.findViewById(R.id.tts_play_pause_ell).setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				mCoolReader.tts = null;
				mCoolReader.ttsInitialized = false;
				mCoolReader.showToast("Re-initializing TTS");
				return true;
			}
		});
		mPanel.findViewById(R.id.tts_back).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.tts_back).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				jumpToSentence( ReaderCommand.DCMD_SELECT_PREV_SENTENCE );
			}
		});
		mPanel.findViewById(R.id.tts_forward).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.tts_forward).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				jumpToSentence( ReaderCommand.DCMD_SELECT_NEXT_SENTENCE );
			}
		});
		mPanel.findViewById(R.id.tts_stop).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.tts_stop).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopAndClose();
			}
		});
		mPanel.setFocusable(true);
		mPanel.setEnabled(true);
		mPanel.setOnKeyListener( new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
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
			}
			
		});

		mWindow.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				if ( !closed )
					stopAndClose();
			}
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
		sbSpeed = (SeekBar)mPanel.findViewById(R.id.tts_sb_speed);
		sbVolume = (SeekBar)mPanel.findViewById(R.id.tts_sb_volume);

		ivVolDown = (ImageView)mPanel.findViewById(R.id.btn_vol_down);
		ivVolUp = (ImageView)mPanel.findViewById(R.id.btn_vol_up);
		ivFreqDown = (ImageView)mPanel.findViewById(R.id.btn_freq_down);
		ivFreqUp = (ImageView)mPanel.findViewById(R.id.btn_freq_up);
		ivVolDown.setBackgroundDrawable(c);
		ivVolUp.setBackgroundDrawable(c);
		ivFreqDown.setBackgroundDrawable(c);
		ivFreqUp.setBackgroundDrawable(c);

		sbSpeed.setMax(100);
		sbSpeed.setProgress(50);
		sbVolume.setMax(100);
		sbVolume.setProgress(mCoolReader.getVolume());

		ivFreqDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int progress = sbSpeed.getProgress();
				if (progress>10) progress=progress-10; else progress = 0;
				sbSpeed.setProgress(progress);
				float rate = 1.0f;
				if ( progress<50 )
					rate = 0.3f + 0.7f * progress / 50f;
				else
					rate = 1.0f + 2.5f * (progress-50) / 50f;
				mTTS.setSpeechRate(rate);
			}
		});
		ivFreqUp.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int progress = sbSpeed.getProgress();
				if (progress<100) progress=progress+10; else progress = 100;
				sbSpeed.setProgress(progress);
				float rate = 1.0f;
				if ( progress<50 )
					rate = 0.3f + 0.7f * progress / 50f;
				else
					rate = 1.0f + 2.5f * (progress-50) / 50f;
				mTTS.setSpeechRate(rate);
			}
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

		ivVolDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int progress = sbVolume.getProgress();
				if (progress>10) progress=progress-10; else progress = 0;
				sbVolume.setProgress(progress);
				mCoolReader.setVolume(progress);
			}
		});
		ivVolUp.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int progress = sbVolume.getProgress();
				if (progress<100) progress=progress+10; else progress = 100;
				sbVolume.setProgress(progress);
				mCoolReader.setVolume(progress);
			}
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
	}
	
}
