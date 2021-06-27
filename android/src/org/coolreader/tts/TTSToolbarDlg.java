package org.coolreader.tts;

import android.annotation.SuppressLint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import android.media.AudioManager;
import org.coolreader.crengine.CustomLog;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderCommand;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.RepeatOnTouchListener;
import org.coolreader.crengine.Selection;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.SomeButtonsToolbarDlg;
import org.coolreader.crengine.Utils;
import org.coolreader.crengine.ViewMode;

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
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import com.s_trace.motion_watchdog.HandlerThread;
import com.s_trace.motion_watchdog.MotionWatchdogHandler;

public class TTSToolbarDlg implements Settings {
	public static final Logger log = L.create("ttssrv");

	private static final String CR3_UTTERANCE_ID = "cr3UtteranceId";
	private static final int MAX_CONTINUOUS_ERRORS = 3;

	private final PopupWindow mWindow;
	private final CoolReader mCoolReader;
	private String mLogFileRoot = "";
	private int mForceTTSKoef = 0;
	private final ReaderView mReaderView;
	private String mBookTitle;
	public View mPanel;
	private TextToSpeech mTTS;
	private TTSControlServiceAccessor mTTSControl;
	private int mCurPage = -1;
	private String mCurPageText;
	private String mCurPageTextPrev;
	private String mCurPageTextPrev2;
	private String mCurPageTextNext;
	private String mCurPageTextNext2;
	private static NotificationChannel channel;
	private ImageButton mPlayPauseButton;
	private ImageButton mPlayPauseButtonEll;
	private TextView mVolumeTextView;
	private TextView mSpeedTextView;
	private ImageView ivVolDown;
	private ImageView ivVolUp;
	private ImageView ivFreqDown;
	private ImageView ivFreqUp;
	private SeekBar mSbSpeed;
	private SeekBar mSbVolume;
	private TextView lblMotionWd;
	private HandlerThread mMotionWatchdog;
	private HandlerThread mTimerHandler;
	private static String CHANNEL_ID = "KnownReader_channel";
	private static int MIN_FORCE_TTS_START_TIME = 5000;
	private int mContinuousErrors = 0;
	private Runnable mOnCloseListener;
	private boolean mClosed;
	private Selection mCurrentSelection;
	private boolean isSpeaking;
	private long startTTSTime;
	private Runnable mOnStopRunnable;
	private int mMotionTimeout;
	private boolean mAutoSetDocLang;
	private String mForcedLanguage;
	private String mForcedVoice;
	private int mTTSSpeedPercent = 50;		// 50% (normal)

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	private static final String TAG = TTSToolbarDlg.class.getSimpleName();
	// For TTS notification:
	private static final int TTS_NOTIFICATION_ID = 0; // Use same ID for all TTS notifications
	private static final int COVER_HEIGHT = 256;
	private static final int COVER_WIDTH = 256;
	private FileInfo mFileInfo = null;
	public Bitmap mBookCover = Bitmap.createBitmap(COVER_WIDTH, COVER_HEIGHT, Bitmap.Config.RGB_565);

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
				moveSelection(dcmdSelectSentence);
			});
		} else moveSelection(dcmdSelectSentence);
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
					if (mCurrentSelection != null) {
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
                            say(mCurrentSelection);
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

		}

		private void handleInterrupt() {
			Log.i(TAG, "handleInterrupt()");
			removeMessages(TimerHandler.MSG_TIMER);
			mHandlerThread.quitSafely();
		}
	}

	static public TTSToolbarDlg showDialog( CoolReader coolReader, ReaderView readerView, TextToSpeech tts)
	{
		TTSToolbarDlg dlg = new TTSToolbarDlg(coolReader, readerView, tts);
		log.d("popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		return dlg;
	}

	public void handleTick(int timeLeft, int currentVolume) {
		BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
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
		}, 200));
	}
	
	public void setOnCloseListener(Runnable handler) {
		mOnCloseListener = handler;
	}

	public void stopAndClose() {
		CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
				"stop tts (direct)");
		if (mClosed)
			return;
		isSpeaking = false;
		mClosed = true;
		BackgroundThread.instance().executeGUI(() -> {
			stop();
			mCoolReader.unregisterReceiver(mTTSControlButtonReceiver);
			if (null != mTTSControl)
				mTTSControl.unbind();
			Intent intent = new Intent(mCoolReader, TTSControlService.class);
			mCoolReader.stopService(intent);
			restoreReaderMode();
			mReaderView.clearSelection();
			if (mOnCloseListener != null)
				mOnCloseListener.run();
			if ( mWindow.isShowing() )
				mWindow.dismiss();
			mReaderView.save();
		});
		runInTTSControlService(tts -> tts.notifyStopMediaSession());
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
				if ((isSpeaking) && (curTime - startTTSTime > 300000)) { // 5 min
					if (mReaderView.getBookInfo().getFileInfo().getReadingState()!=FileInfo.STATE_READING) {
						mReaderView.getBookInfo().getFileInfo().askedMarkReading = true;
						final FileInfo book1=mReaderView.getBookInfo().getFileInfo();
						Services.getHistory().getOrCreateBookInfo(mCoolReader.getDB(), book1, bookInfo -> {
							book1.setReadingState(FileInfo.STATE_READING);
							BookInfo bi = new BookInfo(book1);
							mCoolReader.getDB().saveBookInfo(bi);
							mCoolReader.getDB().flush();
							mCoolReader.showToast(R.string.book_marked_reading);
							if (bookInfo.getFileInfo() != null) {
								bookInfo.getFileInfo().setReadingState(FileInfo.STATE_READING);
								if (bookInfo.getFileInfo().parent != null)
									mCoolReader.directoryUpdated(bookInfo.getFileInfo().parent, bookInfo.getFileInfo());
							}
							BookInfo bi2 = Services.getHistory().getBookInfo(book1);
							if (bi2 != null)
								bi2.getFileInfo().setFileProperties(book1);
						});
					}
				}
				mCurrentSelection = selection;
				if ( isSpeaking ) {
					if (isAlwaysStop) {
						mTTS.stop();
						CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
								"stop tts between reading of sentences");
					}
					say(mCurrentSelection);
				}
			}
			
			@Override
			public void onFail() {
				log.e("fail()");
				stop();
			}
		});
	}
	
	private void say( Selection selection ) {
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Bundle bundle = new Bundle();
			bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
			mTTS.speak(selection.text, TextToSpeech.QUEUE_ADD, bundle, CR3_UTTERANCE_ID);
		} else {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, CR3_UTTERANCE_ID);
			mTTS.speak(selection.text, TextToSpeech.QUEUE_ADD, params);
		}
		final String finalClearText = clearText;
		runInTTSControlService(tts -> tts.notifyPlay(mBookTitle, finalClearText));
	}
	
	private void start() {
		if (mCurrentSelection == null)
			return;
		startMotionWatchdog();
		isSpeaking = true;
		startTTSTime = System.currentTimeMillis();
		say(mCurrentSelection);
	}

	private void startMotionWatchdog(){
		String TAG = "MotionWatchdog";
		log.d("startMotionWatchdog() enter");
		if (mMotionTimeout == 0) {
			Log.d(TAG, "startMotionWatchdog() early exit - timeout is 0");
			return;
		}
		mMotionWatchdog = new HandlerThread("MotionWatchdog");
		mMotionWatchdog.start();
		new MotionWatchdogHandler(this, mCoolReader, mMotionWatchdog, mMotionTimeout);
		Log.d(TAG, "startMotionWatchdog() exit");
	}

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
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		ColorDrawable c = new ColorDrawable(colorGrayC);
		String sTranspButtons = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_TTS_TOOLBAR_TRANSP_BUTTONS, "0");
		if (!sTranspButtons.equals("0")) c.setAlpha(130);
		else c.setAlpha(255);

		if ( isSpeaking ) {
			mPlayPauseButton.setImageResource(
					Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play, R.drawable.ic_media_play)
					//R.drawable.ic_media_play
			);
			mPlayPauseButtonEll.setImageResource(
					Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play_ell, R.drawable.icons8_play_ell)
					//R.drawable.ic_media_play
			);
			mPlayPauseButton.setBackgroundDrawable(c);
			mPlayPauseButtonEll.setBackgroundDrawable(c);
			runInTTSControlService(tts -> tts.notifyPause(mBookTitle));
			stop();
		} else {
			if (null != mCurrentSelection) {
				mPlayPauseButton.setImageResource(
						Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_pause, R.drawable.ic_media_pause)
						//R.drawable.ic_media_pause
				);
				mPlayPauseButtonEll.setImageResource(
						Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_pause, R.drawable.ic_media_pause)
						//R.drawable.ic_media_pause
				);
				mPlayPauseButton.setBackgroundDrawable(c);
				mPlayPauseButtonEll.setBackgroundDrawable(c);
				runInTTSControlService(tts -> tts.notifyPlay(mBookTitle, mCurrentSelection.text));
				start();
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
		colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);

		ColorDrawable c = new ColorDrawable(colorGrayC);
		String sTranspButtons = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_TTS_TOOLBAR_TRANSP_BUTTONS, "0");
		if (!sTranspButtons.equals("0")) c.setAlpha(130);
		else c.setAlpha(255);

		if (mPanel!=null) {
			mPanel.findViewById(R.id.tts_play_pause).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_play_pause_ell).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_back).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_forward).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_stop).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_options).setBackgroundDrawable(c);
		}
	}

	public void changeTTS(TextToSpeech tts) {
		pause();
		mTTS = tts;
		setupTTSVoice();
		setupTTSHandlers();
	}

	/**
	 * Convert speech speed percentage to speech rate value.
	 * @param percent speech rate percentage
	 * @return speech rate value
	 *
	 * 0%  - 0.30
	 * 10% - 0.44
	 * 20% - 0.58
	 * 30% - 0.72
	 * 40% - 0.86
	 * 50% - 1.00
	 * 60% - 1.50
	 * 70% - 2.00
	 * 80% - 2.50
	 * 90% - 3.00
	 * 100%- 3.50
	 */
	private float speechRateFromPercent(int percent) {
		float rate;
		if ( percent < 50 )
			rate = 0.3f + 0.7f * percent / 50f;
		else
			rate = 1.0f + 2.5f * (percent - 50) / 50f;
		return rate;
	}

	public void setAppSettings(Properties newSettings, Properties oldSettings) {
		log.v("setAppSettings()");
		BackgroundThread.ensureGUI();
		if (oldSettings == null)
			oldSettings = new Properties();
		int oldTTSSpeed = mTTSSpeedPercent;
		Properties changedSettings = newSettings.diff(oldSettings);
		for (Map.Entry<Object, Object> entry : changedSettings.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			processAppSetting(key, value);
		}
		// Apply settings
		setupTTSVoice();
		if (oldTTSSpeed != mTTSSpeedPercent) {
			mTTS.setSpeechRate(speechRateFromPercent(mTTSSpeedPercent));
			mSbSpeed.setProgress(mTTSSpeedPercent);
		}
	}

	private void processAppSetting(String key, String value) {
		boolean flg = "1".equals(value);
		switch (key) {
			case PROP_APP_MOTION_TIMEOUT:
				mMotionTimeout = Utils.parseInt(value, 0, 0, 100);
				mMotionTimeout = mMotionTimeout * 60 * 1000; // Convert minutes to msecs
				break;
			case PROP_APP_TTS_SPEED:
				mTTSSpeedPercent = Utils.parseInt(value, 50, 0, 100);
				break;
			case PROP_APP_TTS_ENGINE:
				// handled in CoolReader
				break;
			case PROP_APP_TTS_USE_DOC_LANG:
				mAutoSetDocLang = flg;
				break;
			case PROP_APP_TTS_FORCE_LANGUAGE:
				mForcedLanguage = value;
				break;
			case PROP_APP_TTS_VOICE:
				mForcedVoice = value;
				break;
			case PROP_APP_OPTIONS_TTS_TOOLBAR_TRANSP_BUTTONS:
				int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
				ColorDrawable c = new ColorDrawable(colorGrayC);
				int val1 = Utils.parseInt(value, 0);
				if (val1 != 0) c.setAlpha(130);
					else c.setAlpha(255);
				mPanel.findViewById(R.id.tts_play_pause).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_play_pause_ell).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_back).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_forward).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_stop).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_options).setBackgroundDrawable(c);
				ivVolDown.setBackgroundDrawable(c);
				ivVolUp.setBackgroundDrawable(c);
				ivFreqDown.setBackgroundDrawable(c);
				ivFreqUp.setBackgroundDrawable(c);
				break;
			case PROP_APP_OPTIONS_TTS_TOOLBAR_BACKGROUND:
				int val = Utils.parseInt(value, 0, 0, 2);
				int colorGray = themeColors.get(R.attr.colorThemeGray2);
				if (val == 0) mPanel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
				if (val == 1) mPanel.setBackgroundColor(Color.argb(0, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
				if (val == 2) mPanel.setBackgroundColor(Color.argb(255, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
				break;
		}
	}

	private void setupTTSVoice() {
		if (mAutoSetDocLang) {
			// set language for TTS based on book's language
			log.d("Setting language according book's language");
			Locale locale = null;
			BookInfo bookInfo = mReaderView.getBookInfo();
			if (null != bookInfo) {
				FileInfo fileInfo = bookInfo.getFileInfo();
				if (null != fileInfo) {
					log.d("book language is \"" + fileInfo.language + "\"");
					if (null != fileInfo.language && fileInfo.language.length() > 0) {
						locale = new Locale(fileInfo.language);
					}
				}
			}
			if (null != locale) {
				log.d("trying to set TTS language to \"" + locale.getDisplayLanguage() + "\"");
				mTTS.setLanguage(locale);
			} else {
				log.e("Failed to detect book's language, using system default!");
			}
		} else {
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				// Update voices list
				Set<Voice> voices;
				if (null != mTTS)
					voices = mTTS.getVoices();
				else
					voices = null;
				// Filter voices for given language
				log.d("Trying to find voice for language \"" + mForcedLanguage + "\"");
				Voice sel_voice = null;
				if (null != voices && null != mForcedLanguage && mForcedLanguage.length() > 0) {
					ArrayList<Voice> acceptable_voices = new ArrayList<>();
					for (Voice voice : voices) {
						Locale locale = voice.getLocale();
						if (mForcedLanguage.toLowerCase().equals(locale.toString().toLowerCase())) {
							acceptable_voices.add(voice);
						}
					}
					if (acceptable_voices.size() > 0) {
						// Select one specific voice
						boolean found = false;
						for (Voice voice : acceptable_voices) {
							if (voice.getName().equals(mForcedVoice))
							{
								sel_voice = voice;
								found = true;
								break;
							}
						}
						if (found) {
							log.d("Voice \"" + mForcedVoice + "\" is found");
						} else {
							sel_voice = acceptable_voices.get(0);
							log.e("Voice \"" + mForcedVoice + "\" NOT found, using \"" + sel_voice.getName() + "\"");
						}
					}
				}
				if (sel_voice != null) {
					log.d("Setting voice: " + sel_voice.getName());
					mTTS.setVoice(sel_voice);
				} else {
					log.e("Failed to find voice for language \"" + mForcedLanguage + "\"!");
				}
			}
		}

	}

	private void setupTTSHandlers() {
		if (null != mTTS) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
				mTTS.setOnUtteranceCompletedListener(utteranceId -> {
					if (null != mOnStopRunnable) {
						mOnStopRunnable.run();
						mOnStopRunnable = null;
					} else {
						if ( isSpeaking )
							moveSelection( ReaderCommand.DCMD_SELECT_NEXT_SENTENCE );
					}
				});
			} else {
				mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
					@Override
					public void onStart(String utteranceId) {
						// nothing...
					}

					@Override
					public void onDone(String utteranceId) {
						if (null != mOnStopRunnable) {
							mOnStopRunnable.run();
							mOnStopRunnable = null;
						} else {
							if ( isSpeaking )
								moveSelection( ReaderCommand.DCMD_SELECT_NEXT_SENTENCE );
						}
						mContinuousErrors = 0;
					}

					@Override
					public void onError(String utteranceId) {
						log.e("TTS error");
						mContinuousErrors++;
						if (mContinuousErrors > MAX_CONTINUOUS_ERRORS) {
							BackgroundThread.instance().executeGUI(() -> {
								toggleStartStop();
								mCoolReader.showToast(R.string.tts_failed);
							});
						} else {
							if (null != mOnStopRunnable) {
								mOnStopRunnable.run();
								mOnStopRunnable = null;
							} else {
								if ( isSpeaking )
									moveSelection( ReaderCommand.DCMD_SELECT_NEXT_SENTENCE );
							}
						}
					}

					// API 21
					@Override
					public void onError(String utteranceId, int errorCode) {
						log.e("TTS error, code=" + errorCode);
						mContinuousErrors++;
						if (mContinuousErrors > MAX_CONTINUOUS_ERRORS) {
							BackgroundThread.instance().executeGUI(() -> {
								toggleStartStop();
								mCoolReader.showToast(R.string.tts_failed);
							});
						} else {
							if (null != mOnStopRunnable) {
								mOnStopRunnable.run();
								mOnStopRunnable = null;
							} else {
								if ( isSpeaking )
									moveSelection( ReaderCommand.DCMD_SELECT_NEXT_SENTENCE );
							}
						}
					}

					// API 23
					@Override
					public void onStop(String utteranceId, boolean interrupted) {
						if (null != mOnStopRunnable) {
							mOnStopRunnable.run();
							mOnStopRunnable = null;
						}
					}

					// API 24
					public void onAudioAvailable(String utteranceId, byte[] audio) {
						// nothing...
					}

					// API 24
					public void onBeginSynthesis(String utteranceId,
												 int sampleRateInHz,
												 int audioFormat,
												 int channelCount) {
						// nothing...
					}
				});
			}
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	public TTSToolbarDlg(CoolReader coolReader, ReaderView readerView, TextToSpeech tts) {
		mCoolReader = coolReader;
        mLogFileRoot = mCoolReader.getSettingsFileF(0).getParent() + "/";
        mForceTTSKoef = readerView.getSettings().getInt(Settings.PROP_APP_TTS_FORCE_KOEF, 0);
        mReaderView = readerView;
		View anchor = readerView.getSurface();
		mTTS = tts;
		themeColors = Utils.getThemeColors(coolReader, isEInk);
		setupTTSHandlers();
		Context context = anchor.getContext();

		mTimerHandler = new HandlerThread("TimerHandler");
		mTimerHandler.start();
		new TimerHandler(this, mCoolReader, mTimerHandler, 5000);

		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGray = themeColors.get(R.attr.colorThemeGray2);

		ColorDrawable c = new ColorDrawable(colorGrayC);
		String sTranspButtons = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_TTS_TOOLBAR_TRANSP_BUTTONS, "0");
		if (!sTranspButtons.equals("0")) c.setAlpha(130);
		else c.setAlpha(255);

		mPanel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.tts_toolbar, null));
		lblMotionWd = mPanel.findViewById(R.id.lbl_motion_wd);
		lblMotionWd.setText(R.string.wd_sett);
		lblMotionWd.setPaintFlags(lblMotionWd.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		lblMotionWd.setOnClickListener(v -> mCoolReader.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_TTS_TITLE, mTTS));
		mPlayPauseButton = mPanel.findViewById(R.id.tts_play_pause);
		mPlayPauseButton.setImageResource(
				Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play, R.drawable.ic_media_play)
				//R.drawable.ic_media_play
		);
		mPlayPauseButton.setBackgroundDrawable(c);
		mPlayPauseButtonEll = mPanel.findViewById(R.id.tts_play_pause_ell);
		mPlayPauseButtonEll.setImageResource(
				Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play_ell, R.drawable.icons8_play_ell)
				//R.drawable.ic_media_play
		);
		mPlayPauseButtonEll.setBackgroundDrawable(c);
		mPanel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ImageButton backButton = mPanel.findViewById(R.id.tts_back);
		ImageButton forwardButton = mPanel.findViewById(R.id.tts_forward);
		ImageButton stopButton = mPanel.findViewById(R.id.tts_stop);

		mWindow = new PopupWindow(anchor.getContext());
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mPlayPauseButton.setBackgroundDrawable(c);
		mPlayPauseButton.setOnClickListener(v -> toggleStartStopExt(false));
		mPlayPauseButtonEll.setOnClickListener(v -> toggleStartStopExt(true));
		mPlayPauseButton.setOnLongClickListener(v -> {
			mCoolReader.tts = null;
			mCoolReader.ttsInitialized = false;
			mCoolReader.showToast("Re-initializing TTS");
			return true;
		});
		mPlayPauseButtonEll.setOnLongClickListener(v -> {
			mCoolReader.tts = null;
			mCoolReader.ttsInitialized = false;
			mCoolReader.showToast("Re-initializing TTS");
			return true;
		});
		backButton.setBackgroundDrawable(c);
		backButton.setOnClickListener(v -> jumpToSentence( ReaderCommand.DCMD_SELECT_PREV_SENTENCE ));
		forwardButton.setBackgroundDrawable(c);
		forwardButton.setOnClickListener(v -> jumpToSentence( ReaderCommand.DCMD_SELECT_NEXT_SENTENCE ));
		stopButton.setBackgroundDrawable(c);
		stopButton.setOnClickListener(v -> stopAndClose());
		ImageButton optionsButton = mPanel.findViewById(R.id.tts_options);
		optionsButton.setBackgroundDrawable(c);
		optionsButton.setOnClickListener(v -> mCoolReader.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_TTS_TITLE, mTTS));
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
					int p = mSbVolume.getProgress() - 5;
					if ( p<0 )
						p = 0;
					mSbVolume.setProgress(p);
					return true;
				}
				case KeyEvent.KEYCODE_VOLUME_UP:
					int p = mSbVolume.getProgress() + 5;
					if ( p>100 )
						p = 100;
					mSbVolume.setProgress(p);
					return true;
				}
				if ( keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
			}
			return false;
		});

		mWindow.setOnDismissListener(() -> {
			mBookCover = Bitmap.createBitmap(COVER_WIDTH, COVER_HEIGHT, Bitmap.Config.RGB_565);
			if (!mClosed)
				stopAndClose();
		});

		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		String sBkg = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_TTS_TOOLBAR_BACKGROUND, "0");
		if (sBkg.equals("0")) mPanel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
		if (sBkg.equals("1")) mPanel.setBackgroundColor(Color.argb(0, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
		if (sBkg.equals("2")) mPanel.setBackgroundColor(Color.argb(255, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
		mWindow.setContentView(mPanel);


		int [] location = new int[2];
		anchor.getLocationOnScreen(location);

		mWindow.showAtLocation(anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], location[1] + anchor.getHeight() - mPanel.getHeight());

		setReaderMode();

		// setup speed && volume seek bars
		int volume = mCoolReader.getVolume();
		mVolumeTextView = mPanel.findViewById(R.id.tts_lbl_volume);
		mVolumeTextView.setText(String.format(Locale.getDefault(), "%s (%d%%)", context.getString(R.string.tts_volume), volume));
		mSpeedTextView = mPanel.findViewById(R.id.tts_lbl_speed);
		mSpeedTextView.setText(String.format(Locale.getDefault(), "%s (x%.2f)", context.getString(R.string.tts_rate), speechRateFromPercent(50)));

		mSbSpeed = mPanel.findViewById(R.id.tts_sb_speed);
		mSbVolume = mPanel.findViewById(R.id.tts_sb_volume);

		ivVolDown = mPanel.findViewById(R.id.btn_vol_down);
		ivVolUp = mPanel.findViewById(R.id.btn_vol_up);
		ivFreqDown = mPanel.findViewById(R.id.btn_freq_down);
		ivFreqUp = mPanel.findViewById(R.id.btn_freq_up);
		ivVolDown.setBackgroundDrawable(c);
		ivVolUp.setBackgroundDrawable(c);
		ivFreqDown.setBackgroundDrawable(c);
		ivFreqUp.setBackgroundDrawable(c);

		mSbSpeed.setMax(100);
		mSbSpeed.setProgress(50);
		mSbVolume.setMax(100);
		mSbVolume.setProgress(volume);

		ivFreqDown.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> {
			mSbSpeed.setProgress(mSbSpeed.getProgress() - 1);
			mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mTTSSpeedPercent), true);
		}));
		ivFreqUp.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> {
			mSbSpeed.setProgress(mSbSpeed.getProgress() + 1);
			mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mTTSSpeedPercent), true);
		}));

		mSbSpeed.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mTTSSpeedPercent = progress;
				mTTS.setSpeechRate(speechRateFromPercent(mTTSSpeedPercent));
				mSpeedTextView.setText(String.format(Locale.getDefault(), "%s (x%.2f)", context.getString(R.string.tts_rate), speechRateFromPercent(progress)));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mTTSSpeedPercent), true);
			}
		});

		ivVolDown.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> mSbVolume.setProgress(mSbVolume.getProgress() - 1)));
		ivVolUp.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> mSbVolume.setProgress(mSbVolume.getProgress() + 1)));

		mSbVolume.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mCoolReader.setVolume(progress);
				mVolumeTextView.setText(String.format(Locale.getDefault(), "%s (%d%%)", context.getString(R.string.tts_volume), progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		mCoolReader.tintViewIcons(mPanel);
		//mPanel.requestFocus();
		BookInfo bookInfo = mReaderView.getBookInfo();
		if (null != bookInfo) {
			FileInfo fileInfo = bookInfo.getFileInfo();
			if (null != fileInfo) {
				mBookTitle = fileInfo.title;
				// set language for TTS based on book's language
				Services.getCoverpageManager().drawCoverpageFor(mCoolReader.getDB(), fileInfo, mBookCover,
						(file, bitmap) -> {
							mBookCover = bitmap;
							runInTTSControlService(tts1 -> tts1.notifyStartMediaSession(bookInfo, bitmap));
						}
				);
			}
		}
		if (null == mBookTitle)
			mBookTitle = "";
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
		mPanel.requestFocus();
	}
}
