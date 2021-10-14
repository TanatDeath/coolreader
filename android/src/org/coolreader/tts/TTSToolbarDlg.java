package org.coolreader.tts;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
import org.coolreader.crengine.RepeatOnTouchListener;
import org.coolreader.crengine.Selection;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.Utils;
import org.coolreader.crengine.ViewMode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.s_trace.motion_watchdog.HandlerThread;
import com.s_trace.motion_watchdog.MotionWatchdogHandler;

public class TTSToolbarDlg implements Settings {
	public static final Logger log = L.create("ttsdlg");

	public static final int MEDIA_COVER_WIDTH = 300;
	public static final int MEDIA_COVER_HEIGHT = 400;

	private final PopupWindow mWindow;
	private final CoolReader mCoolReader;
	private final ReaderView mReaderView;
	public View mPanel;
	private final TTSControlServiceAccessor mTTSControl;
	private int mCurPage = -1;
	private String mCurPageText;
	private String mCurPageTextPrev;
	private String mCurPageTextPrev2;
	private String mCurPageTextNext;
	private String mCurPageTextNext2;
	private final ImageButton mPlayPauseButton;
	private final TextView mVolumeTextView;
	private final TextView mSpeedTextView;
	private final ImageView btnDecVolume;
	private final ImageView btnIncVolume;
	private final ImageView btnDecSpeed;
	private final ImageView btnIncSpeed;
	private final SeekBar mSbSpeed;
	private final SeekBar mSbVolume;
	private TextView lblMotionWd;
	private TextView lblLang;
	private HandlerThread mMotionWatchdog;
	private Runnable mOnCloseListener;
	private boolean mClosed;
	private Selection mCurrentSelection;
	private boolean isSpeaking;
	private long startTTSTime;
	private int mMotionTimeout;
	private boolean mAutoSetDocLang;
	private String mBookAuthors;
	private String mBookTitle;
	private Bitmap mBookCover;
	private String mBookLanguage;
	private String mForcedVoice;
	private String mCurrentLanguage;
	private String mCurrentVoiceName;
	private boolean mGoogleTTSAbbreviationWorkaround;
	private int mTTSSpeedPercent = 50;		// 50% (normal)
	private Button mLoadSpeed1;
	private Button mLoadSpeed2;
	private Button mLoadSpeed3;
	private Button mLoadVol1;
	private Button mLoadVol2;
	private Button mLoadVol3;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	private String mLogFileRoot = "";

	private static final String TAG = TTSToolbarDlg.class.getSimpleName();

	static public TTSToolbarDlg showDialog(CoolReader coolReader, ReaderView readerView, TTSControlServiceAccessor ttsacc)
	{
		TTSToolbarDlg dlg = new TTSToolbarDlg(coolReader, readerView, ttsacc);
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
//		CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
//				"stop tts (direct)");
		if (mClosed)
			return;
		isSpeaking = false;
		mClosed = true;
		mTTSControl.bind(ttsbinder -> {
			ttsbinder.stop(result -> {
				BackgroundThread.instance().postGUI(() -> {
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
			});
		});
	}

	private boolean changedPageMode;

	public void pause() {
		mTTSControl.bind(ttsbinder -> {
			ttsbinder.pause(null);
		});
	}

	// plotn: NB!!! same fix as we did, but in other way. Check if it works
	private void setReaderMode()
	{
		String oldViewSetting = mReaderView.getSetting(ReaderView.PROP_PAGE_VIEW_MODE);
		moveSelection(ReaderCommand.DCMD_SELECT_FIRST_SENTENCE, null);
		if (("1".equals(oldViewSetting)) &&
				(!("1".equals(mReaderView.getSetting(ReaderView.PROP_PAGE_VIEW_MODE_TTS_DONT_CHANGE))))) {
			changedPageMode = true;
			//mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE, "0");
			//mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE_AUTOCHANGED, "1");
			mReaderView.setViewModeNonPermanent(ViewMode.SCROLL);
		}
	}
	
	private void restoreReaderMode()
	{
		if ( changedPageMode ) {
			//mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE, "1");
			//mReaderView.setSetting(ReaderView.PROP_PAGE_VIEW_MODE_AUTOCHANGED, "0");
			mReaderView.setViewModeNonPermanent(ViewMode.PAGES);
		}
	}

	/**
	 * Select next or previous sentence. ONLY the selection changes and the specified callback is called!
	 * Not affected to speech synthesis process.
	 * @param cmd move command. DCMD_SELECT_NEXT_SENTENCE, DCMD_SELECT_PREV_SENTENCE, DCMD_SELECT_FIRST_SENTENCE.
	 * @param callback optional completion callback
	 */
	private void moveSelection(ReaderCommand cmd, ReaderView.MoveSelectionCallback callback)
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
//					CustomLog.doLog(mLogFileRoot+(isAlwaysStop?"log_tts_type1.log":"log_tts_type0.log"),
//							"it is time to save position");
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
				if (null != callback)
					callback.onNewSelection(mCurrentSelection);
			}
			
			@Override
			public void onFail() {
				log.e("fail()");
				if (isSpeaking) {
					mTTSControl.bind(ttsbinder ->
							ttsbinder.stop(result ->
									log.e("speech synthesis process stopped!")));
				}
				if (null != callback)
					callback.onFail();
			}
		});
	}

	private String preprocessUtterance(String utterance) {
		String newUtterance = utterance;
		if (mGoogleTTSAbbreviationWorkaround) {
			// Add space before last char if it's dot.
			int len = newUtterance.length();
			if (len > 1) {
				if (newUtterance.charAt(len - 1) == '.') {
					newUtterance = newUtterance.substring(0, len - 1);
					newUtterance += " .";
				}
			}
		}
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
				if ((mCurPageText.contains("["+newUtterance+"]")) || (mCurPageText.contains("{"+newUtterance+"}")))
					newUtterance = "";
				if ((mCurPageTextPrev.contains("["+newUtterance+"]")) || (mCurPageTextPrev.contains("{"+newUtterance+"}")))
					newUtterance = "";
				if ((mCurPageTextNext.contains("["+newUtterance+"]")) || (mCurPageTextNext.contains("{"+newUtterance+"}")))
					newUtterance = "";
				if ((mCurPageTextPrev2.contains("["+newUtterance+"]")) || (mCurPageTextPrev2.contains("{"+newUtterance+"}")))
					newUtterance = "";
				if ((mCurPageTextNext2.contains("["+newUtterance+"]")) || (mCurPageTextNext2.contains("{"+newUtterance+"}")))
					newUtterance = "";
			}
		// this won't work but I let it...
		newUtterance = newUtterance.replaceAll("\\[\\d+\\]", "");
		newUtterance = newUtterance.replaceAll("\\{\\d+\\}", "");
		return newUtterance;
	}

	private void startMotionWatchdog(){
		startTTSTime = System.currentTimeMillis();
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

	private int iSentenceCount;
	private double dWordCount;
	private double dWordCountThis;
	private long lTimeSpan;
	private long curTime = System.currentTimeMillis();
	private long lastSaveSpeakTime = System.currentTimeMillis();
	private long lastRestartTime = 0;
	private long lastSaveCurPosTime = System.currentTimeMillis();

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
			mPanel.findViewById(R.id.tts_back).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_forward).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_stop).setBackgroundDrawable(c);
			mPanel.findViewById(R.id.tts_options).setBackgroundDrawable(c);
		}
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
			mTTSControl.bind(ttsbinder -> {
				ttsbinder.setSpeechRate(speechRateFromPercent(mTTSSpeedPercent), result -> {
					if (result)
						BackgroundThread.instance().postGUI(() -> mSbSpeed.setProgress(mTTSSpeedPercent));
				});
			});
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
			/*case PROP_APP_TTS_FORCE_LANGUAGE: // exists in CR, but is not used
				mForcedLanguage = value;
				break;
			 */
			case PROP_APP_TTS_VOICE:
				mForcedVoice = value;
				break;
			case PROP_APP_TTS_GOOGLE_END_OF_SENTENCE_ABBR:
				mGoogleTTSAbbreviationWorkaround = flg;
			case PROP_APP_OPTIONS_TTS_TOOLBAR_TRANSP_BUTTONS:
				int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
				ColorDrawable c = new ColorDrawable(colorGrayC);
				int val1 = Utils.parseInt(value, 0);
				if (val1 != 0) c.setAlpha(130);
					else c.setAlpha(255);
				mPanel.findViewById(R.id.tts_play_pause).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_back).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_forward).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_stop).setBackgroundDrawable(c);
				mPanel.findViewById(R.id.tts_options).setBackgroundDrawable(c);
				btnDecVolume.setBackgroundDrawable(c);
				btnIncVolume.setBackgroundDrawable(c);
				btnDecSpeed.setBackgroundDrawable(c);
				btnIncSpeed.setBackgroundDrawable(c);
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
		if (lblLang != null)
			lblLang.setText("?lang?");
		if (mAutoSetDocLang) {
			// set language for TTS based on book's language
			if (null != mBookLanguage && mBookLanguage.length() > 0 && !mBookLanguage.equals(mCurrentLanguage)) {
				log.d("trying to set TTS language to \"" + mBookLanguage + "\"");
				mTTSControl.bind(ttsbinder -> {
					ttsbinder.setLanguage(mBookLanguage, result -> {
						mCurrentLanguage = mBookLanguage;
						if (result) {
							log.d("setting TTS language to \"" + mBookLanguage + "\" successful.");
							if (lblLang != null)
								lblLang.setText(mBookLanguage);
						}
						else
							log.d("Failed to set TTS language to \"" + mBookLanguage + "\".");
					});
				});
			} else {
				if ((mCurrentLanguage != null) && (mBookLanguage != null))
					if (mBookLanguage.equals(mCurrentLanguage))
						if (lblLang != null)
							lblLang.setText(mCurrentLanguage);
				log.e("Failed to detect (or not changed) book's language, will be used system default!");
			}
		} else {
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				if (null != mForcedVoice && mForcedVoice.length() > 0 && !mForcedVoice.equals(mCurrentVoiceName)) {
					mTTSControl.bind(ttsbinder -> {
						ttsbinder.setVoice(mForcedVoice, result -> {
							mCurrentVoiceName = mForcedVoice;
							if (result) {
								log.d("Set voice \"" + mForcedVoice + "\" successful");
								if (lblLang != null)
									lblLang.setText(mForcedVoice);
							} else {
								log.e("Failed to set voice \"" + mForcedVoice + "\"!");
							}
						});
					});
				} else {
					if ((mCurrentVoiceName != null) && (mForcedVoice != null))
						if (mForcedVoice.equals(mCurrentVoiceName)) {
							if (lblLang != null)
								lblLang.setText(mCurrentVoiceName);
						}
				}
			}
		}
	}

	private void setupSpeechStatusHandler() {
		mTTSControl.bind(ttsbinder -> {
			ttsbinder.setStatusListener(new OnTTSStatusListener() {
				@Override
				public void onUtteranceStart() {
					isSpeaking = true;
				}

				@Override
				public void onUtteranceDone() {
				}

				@Override
				public void onError(int errorCode) {
					BackgroundThread.instance().postGUI(() -> mCoolReader.showToast(R.string.tts_failed));
				}

				@Override
				public void onStateChanged(TTSControlService.State state) {
					switch (state) {
						case PLAYING:
							isSpeaking = true;
							BackgroundThread.instance().postGUI(() ->
							{
								mPlayPauseButton.setImageResource(Utils.resolveResourceIdByAttr(
										mCoolReader, R.attr.attr_ic_media_pause, R.drawable.ic_media_pause));
								repaintButtons();
							});
							startMotionWatchdog();
							break;
						case PAUSED:
						case STOPPED:
							isSpeaking = false;
							BackgroundThread.instance().postGUI(() ->
							{
								mPlayPauseButton.setImageResource(Utils.resolveResourceIdByAttr(
										mCoolReader, R.attr.attr_ic_media_play, R.drawable.ic_media_play));
								repaintButtons();
							});
							if (mMotionWatchdog != null)
								mMotionWatchdog.interrupt();
							break;
					}
				}

				@Override
				public void onVolumeChanged(int currentVolume, int maxVolume) {
					BackgroundThread.instance().postGUI(() -> {
						CustomLog.doLog(mLogFileRoot, "log_volume.log",
								"Got volumes, current (1): " + currentVolume + ", max = " + maxVolume);
						mSbVolume.setMax(maxVolume);
						mSbVolume.setProgress(currentVolume);
					});
				}

				@Override
				public void onAudioFocusLost() {
				}

				@Override
				public void onAudioFocusRestored() {
				}

				@Override
				public void onCurrentSentenceRequested(TTSControlBinder ttsbinder) {
					if (null != mCurrentSelection) {
						ttsbinder.say(preprocessUtterance(mCurrentSelection.text), null);
					}
				}

				@Override
				public void onNextSentenceRequested(TTSControlBinder ttsbinder) {
					if (isSpeaking) {
						moveSelection(ReaderCommand.DCMD_SELECT_NEXT_SENTENCE, new ReaderView.MoveSelectionCallback() {
							@Override
							public void onNewSelection(Selection selection) {
								ttsbinder.say(preprocessUtterance(selection.text), null);
							}

							@Override
							public void onFail() {
							}
						});
					} else {
						moveSelection(ReaderCommand.DCMD_SELECT_NEXT_SENTENCE, null);
					}
				}

				@Override
				public void onPreviousSentenceRequested(TTSControlBinder ttsbinder) {
					if (isSpeaking) {
						moveSelection(ReaderCommand.DCMD_SELECT_PREV_SENTENCE, new ReaderView.MoveSelectionCallback() {
							@Override
							public void onNewSelection(Selection selection) {
								ttsbinder.say(preprocessUtterance(selection.text), null);
							}

							@Override
							public void onFail() {
							}
						});
					} else {
						moveSelection(ReaderCommand.DCMD_SELECT_PREV_SENTENCE, null);
					}
				}

				@Override
				public void onStopRequested(TTSControlBinder ttsbinder) {
					stopAndClose();
				}
			});
		});
	}

	private void updateSaveButtons() {
		int speed1 = mCoolReader.settings().getInt(PROP_APP_TTS_SPEED_1, -1);
		int speed2 = mCoolReader.settings().getInt(PROP_APP_TTS_SPEED_2, -1);
		int speed3 = mCoolReader.settings().getInt(PROP_APP_TTS_SPEED_3, -1);
		int vol1 = (int) Math.round(mCoolReader.settings().getDouble(PROP_APP_TTS_VOL_1, -1));
		int vol2 = (int) Math.round(mCoolReader.settings().getDouble(PROP_APP_TTS_VOL_2, -1));
		int vol3 = (int) Math.round(mCoolReader.settings().getDouble(PROP_APP_TTS_VOL_3, -1));
		if (speed1 == -1) mLoadSpeed1.setText("?");
			else mLoadSpeed1.setText(String.format(Locale.getDefault(), "%.2f", speechRateFromPercent(speed1)));
		if (speed2 == -1) mLoadSpeed2.setText("?");
			else mLoadSpeed2.setText(String.format(Locale.getDefault(), "%.2f", speechRateFromPercent(speed2)));
		if (speed3 == -1) mLoadSpeed3.setText("?");
			else mLoadSpeed3.setText(String.format(Locale.getDefault(), "%.2f", speechRateFromPercent(speed3)));
		mLoadVol1.setText("?");
		mLoadVol2.setText("?");
		mLoadVol3.setText("?");
		if (mSbVolume != null) {
			int seekbarMax = mSbVolume.getMax();
			if (seekbarMax > 0) {
				if (vol1 != -1)
					mLoadVol1.setText(String.format(Locale.getDefault(), "%d", vol1));
				if (vol2 != -1)
					mLoadVol2.setText(String.format(Locale.getDefault(), "%d", vol2));
				if (vol3 != -1)
					mLoadVol3.setText(String.format(Locale.getDefault(), "%d", vol3));
			}
		}
	}

	private int getVolValue(double vol) {
		if (mSbVolume == null) return (int) Math.round(vol);
		if (mSbVolume.getMax() == 0) return (int) Math.round(vol);
		double vvol = Math.round(vol * ((double) mSbVolume.getMax()) / (double) 100.0);
		return (int) vvol;
	}

	private double getVolPercent(int vol) {
		if (mSbVolume == null) return vol;
		if (mSbVolume.getMax() == 0) return vol;
		double vvol = ((double) vol)  / ((double) mSbVolume.getMax()) * (double) 100.0;
		return vvol;
	}

	@SuppressLint("ClickableViewAccessibility")
	public TTSToolbarDlg(CoolReader coolReader, ReaderView readerView, TTSControlServiceAccessor ttsacc) {
		mCoolReader = coolReader;
        mReaderView = readerView;
		mLogFileRoot = coolReader.getSettingsFileF(0).getParent() + "/";
		mTTSControl = ttsacc;
		View anchor = readerView.getSurface();
		themeColors = Utils.getThemeColors(coolReader, isEInk);
		Context context = anchor.getContext();

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
		lblMotionWd.setOnClickListener(
				v -> mTTSControl.bind(ttsbinder -> {
					mCoolReader.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_TTS_TITLE, ttsbinder);
				})
		);
		lblLang = mPanel.findViewById(R.id.lbl_lang);
		mPlayPauseButton = mPanel.findViewById(R.id.tts_play_pause);
		mPlayPauseButton.setImageResource(
				Utils.resolveResourceIdByAttr(mCoolReader, R.attr.attr_ic_media_play, R.drawable.ic_media_play)
				//R.drawable.ic_media_play
		);
		mPlayPauseButton.setBackgroundDrawable(c);

		ImageButton backButton = mPanel.findViewById(R.id.tts_back);
		ImageButton forwardButton = mPanel.findViewById(R.id.tts_forward);
		ImageButton stopButton = mPanel.findViewById(R.id.tts_stop);

		mWindow = new PopupWindow(anchor.getContext());
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mPlayPauseButton.setBackgroundDrawable(c);
		mPlayPauseButton.setOnClickListener(
				v -> mCoolReader.sendBroadcast(new Intent(TTSControlService.TTS_CONTROL_ACTION_PLAY_PAUSE)));
		backButton.setBackgroundDrawable(c);
		backButton.setOnClickListener(
				v -> mCoolReader.sendBroadcast(new Intent(TTSControlService.TTS_CONTROL_ACTION_PREV)));
		forwardButton.setBackgroundDrawable(c);
		forwardButton.setOnClickListener(
				v -> mCoolReader.sendBroadcast(new Intent(TTSControlService.TTS_CONTROL_ACTION_NEXT)));
		stopButton.setBackgroundDrawable(c);
		stopButton.setOnClickListener(v -> stopAndClose());
		ImageButton optionsButton = mPanel.findViewById(R.id.tts_options);
		optionsButton.setBackgroundDrawable(c);
		optionsButton.setOnClickListener(
		v -> mTTSControl.bind(ttsbinder -> {
			mCoolReader.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_TTS_TITLE, ttsbinder);
		}));

		// setup speed && volume seek bars
		mVolumeTextView = mPanel.findViewById(R.id.tts_lbl_volume);
		mSpeedTextView = mPanel.findViewById(R.id.tts_lbl_speed);
		mSpeedTextView.setText(String.format(Locale.getDefault(), "%s (x%.2f)", context.getString(R.string.tts_rate), speechRateFromPercent(50)));

		mSbSpeed = mPanel.findViewById(R.id.tts_sb_speed);
		mSbVolume = mPanel.findViewById(R.id.tts_sb_volume);

		btnDecVolume = mPanel.findViewById(R.id.btn_vol_down);
		btnIncVolume = mPanel.findViewById(R.id.btn_vol_up);
		btnDecSpeed = mPanel.findViewById(R.id.btn_freq_down);
		btnIncSpeed = mPanel.findViewById(R.id.btn_freq_up);
		btnDecVolume.setBackgroundDrawable(c);
		btnIncVolume.setBackgroundDrawable(c);
		btnDecSpeed.setBackgroundDrawable(c);
		btnIncSpeed.setBackgroundDrawable(c);

		mSbSpeed.setMax(100);
		mSbSpeed.setProgress(50);
		mSbVolume.setMax(100);
		mSbVolume.setProgress(0);

		btnDecSpeed.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> {
			mSbSpeed.setProgress(mSbSpeed.getProgress() - 1);
			mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mSbSpeed.getProgress()), true);
		}));
		btnIncSpeed.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> {
			mSbSpeed.setProgress(mSbSpeed.getProgress() + 1);
			mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mSbSpeed.getProgress()), true);
		}));

		mSbSpeed.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			int mProgress;
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mProgress = progress;
				float rate = speechRateFromPercent(progress);
				mSpeedTextView.setText(String.format(Locale.getDefault(), "%s (x%.2f)", context.getString(R.string.tts_rate), rate));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mProgress), true);
			}
		});

		btnDecVolume.setOnTouchListener(new RepeatOnTouchListener(500, 150, view ->
				mSbVolume.setProgress(mSbVolume.getProgress() - 1)));
		btnIncVolume.setOnTouchListener(new RepeatOnTouchListener(500, 150, view ->
				mSbVolume.setProgress(mSbVolume.getProgress() + 1)));

		mCoolReader.tintViewIcons(mPanel);
		repaintButtons();

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
					int p = mSbVolume.getProgress() - 1;
					if ( p<0 )
						p = 0;
					mSbVolume.setProgress(p);
					return true;
				}
				case KeyEvent.KEYCODE_VOLUME_UP:
					int p = mSbVolume.getProgress() + 1;
					if ( p > mSbVolume.getMax() )
						p = mSbVolume.getMax();
					mSbVolume.setProgress(p);
					return true;
				}
				if ( keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
			}
			return false;
		});

		mLoadVol1 = mPanel.findViewById(R.id.load_vol_1);
		mLoadVol2 = mPanel.findViewById(R.id.load_vol_2);
		mLoadVol3 = mPanel.findViewById(R.id.load_vol_3);
		mLoadSpeed1 = mPanel.findViewById(R.id.load_speed_1);
		mLoadSpeed2 = mPanel.findViewById(R.id.load_speed_2);
		mLoadSpeed3 = mPanel.findViewById(R.id.load_speed_3);
		mLoadSpeed1.setOnClickListener((v) -> {
			int speed0 = mSbSpeed.getProgress();
			int speed = mCoolReader.settings().getInt(PROP_APP_TTS_SPEED_1, -1);
			if (speed != -1) {
				mSbSpeed.setProgress(speed);
				mCoolReader.setSetting(PROP_APP_TTS_SPEED_1, String.valueOf(speed), true);
				mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(speed), true);
			} else {
				mReaderView.skipFallbackWarning = true;
				mCoolReader.settings().setInt(PROP_APP_TTS_SPEED_1, speed0);
				mCoolReader.showToast(R.string.value_saved);
				updateSaveButtons();
			}
		});
		mLoadSpeed1.setOnLongClickListener((v) -> {
			mReaderView.skipFallbackWarning = true;
			int speed0 = mSbSpeed.getProgress();
			mCoolReader.settings().setInt(PROP_APP_TTS_SPEED_1, speed0);
			mCoolReader.showToast(R.string.value_saved);
			updateSaveButtons();
			return true;
		});
		mLoadSpeed2.setOnClickListener((v) -> {
			int speed0 = mSbSpeed.getProgress();
			int speed = mCoolReader.settings().getInt(PROP_APP_TTS_SPEED_2, -1);
			if (speed != -1) {
				mSbSpeed.setProgress(speed);
				mCoolReader.setSetting(PROP_APP_TTS_SPEED_2, String.valueOf(speed), true);
				mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(speed), true);
			} else {
				mReaderView.skipFallbackWarning = true;
				mCoolReader.settings().setInt(PROP_APP_TTS_SPEED_2, speed0);
				mCoolReader.showToast(R.string.value_saved);
				updateSaveButtons();
			}
		});
		mLoadSpeed2.setOnLongClickListener((v) -> {
			int speed0 = mSbSpeed.getProgress();
			mReaderView.skipFallbackWarning = true;
			mCoolReader.settings().setInt(PROP_APP_TTS_SPEED_2, speed0);
			mCoolReader.showToast(R.string.value_saved);
			updateSaveButtons();
			return true;
		});
		mLoadSpeed3.setOnClickListener((v) -> {
			int speed0 = mSbSpeed.getProgress();
			int speed = mCoolReader.settings().getInt(PROP_APP_TTS_SPEED_3, -1);
			if (speed != -1) {
				mSbSpeed.setProgress(speed);
				mCoolReader.setSetting(PROP_APP_TTS_SPEED_3, String.valueOf(speed), true);
				mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(speed), true);
			} else {
				mReaderView.skipFallbackWarning = true;
				mCoolReader.settings().setInt(PROP_APP_TTS_SPEED_3, speed0);
				mCoolReader.showToast(R.string.value_saved);
				updateSaveButtons();
			}
		});
		mLoadSpeed3.setOnLongClickListener((v) -> {
			int speed0 = mSbSpeed.getProgress();
			mReaderView.skipFallbackWarning = true;
			mCoolReader.settings().setInt(PROP_APP_TTS_SPEED_3, speed0);
			mCoolReader.showToast(R.string.value_saved);
			updateSaveButtons();
			return true;
		});
		mLoadSpeed1.setBackgroundDrawable(c);
		mLoadSpeed2.setBackgroundDrawable(c);
		mLoadSpeed3.setBackgroundDrawable(c);
		mLoadVol1.setOnClickListener((v) -> {
			double vol = mCoolReader.settings().getDouble(PROP_APP_TTS_VOL_1, -1);
			if (vol != -1)
				mSbVolume.setProgress(getVolValue(vol));
			else {
				mReaderView.skipFallbackWarning = true;
				mCoolReader.settings().setDouble(PROP_APP_TTS_VOL_1, getVolPercent(mSbVolume.getProgress()));
				CustomLog.doLog(mLogFileRoot, "log_volume.log",
					"Save volume to: " + getVolPercent(mSbVolume.getProgress()) + ", in absolute = " + mSbVolume.getProgress());
				mCoolReader.showToast(R.string.value_saved);
				updateSaveButtons();
			}
		});
		mLoadVol1.setOnLongClickListener((v) -> {
			mReaderView.skipFallbackWarning = true;
			mCoolReader.settings().setDouble(PROP_APP_TTS_VOL_1, getVolPercent(mSbVolume.getProgress()));
			CustomLog.doLog(mLogFileRoot, "log_volume.log",
					"Save volume to: " + getVolPercent(mSbVolume.getProgress()) + ", in absolute = " + mSbVolume.getProgress());
			mCoolReader.showToast(R.string.value_saved);
			updateSaveButtons();
			return true;
		});
		mLoadVol2.setOnClickListener((v) -> {
			double vol = mCoolReader.settings().getDouble(PROP_APP_TTS_VOL_2, -1);
			if (vol != -1)
				mSbVolume.setProgress(getVolValue(vol));
			else {
				mReaderView.skipFallbackWarning = true;
				mCoolReader.settings().setDouble(PROP_APP_TTS_VOL_2, getVolPercent(mSbVolume.getProgress()));
				CustomLog.doLog(mLogFileRoot, "log_volume.log",
						"Save volume to: " + getVolPercent(mSbVolume.getProgress()) + ", in absolute = " + mSbVolume.getProgress());
				mCoolReader.showToast(R.string.value_saved);
				updateSaveButtons();
			}
		});
		mLoadVol2.setOnLongClickListener((v) -> {
			mReaderView.skipFallbackWarning = true;
			mCoolReader.settings().setDouble(PROP_APP_TTS_VOL_2, getVolPercent(mSbVolume.getProgress()));
			CustomLog.doLog(mLogFileRoot, "log_volume.log",
					"Save volume to: " + getVolPercent(mSbVolume.getProgress()) + ", in absolute = " + mSbVolume.getProgress());
			mCoolReader.showToast(R.string.value_saved);
			updateSaveButtons();
			return true;
		});
		mLoadVol3.setOnClickListener((v) -> {
			double vol = mCoolReader.settings().getDouble(PROP_APP_TTS_VOL_3, -1);
			if (vol != -1)
				mSbVolume.setProgress(getVolValue(vol));
			else {
				mReaderView.skipFallbackWarning = true;
				mCoolReader.settings().setDouble(PROP_APP_TTS_VOL_3, getVolPercent(mSbVolume.getProgress()));
				CustomLog.doLog(mLogFileRoot, "log_volume.log",
						"Save volume to: " + getVolPercent(mSbVolume.getProgress()) + ", in absolute = " + mSbVolume.getProgress());
				mCoolReader.showToast(R.string.value_saved);
				updateSaveButtons();
			}
		});
		mLoadVol3.setOnLongClickListener((v) -> {
			mReaderView.skipFallbackWarning = true;
			mCoolReader.settings().setDouble(PROP_APP_TTS_VOL_3, getVolPercent(mSbVolume.getProgress()));
			CustomLog.doLog(mLogFileRoot, "log_volume.log",
					"Save volume to: " + getVolPercent(mSbVolume.getProgress()) + ", in absolute = " + mSbVolume.getProgress());
			mCoolReader.showToast(R.string.value_saved);
			updateSaveButtons();
			return true;
		});
		mLoadVol1.setBackgroundDrawable(c);
		mLoadVol2.setBackgroundDrawable(c);
		mLoadVol3.setBackgroundDrawable(c);

		mPanel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		mWindow.setOnDismissListener(() -> {
			if ( !mClosed)
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

		if (null == mBookTitle)
			mBookTitle = "";
		if (null == mBookAuthors)
			mBookAuthors = "";
		if (null == mBookLanguage) {
			log.e("Failed to detect book's language!");
		}
		// Start the foreground service to make this app also foreground,
		// even if the main activity is in the background.
		// https://developer.android.com/about/versions/oreo/background#services
		Intent intent = new Intent(TTSControlService.TTS_CONTROL_ACTION_PREPARE, Uri.EMPTY, coolReader, TTSControlService.class);
		Bundle data = new Bundle();
		data.putString("bookAuthors", mBookAuthors);
		data.putString("bookTitle", mBookTitle);
		intent.putExtras(data);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			coolReader.startForegroundService(intent);
		else
			coolReader.startService(intent);

		mPanel.requestFocus();

		// All tasks bellow after service start
		// Fetch book's metadata
		BookInfo bookInfo = mReaderView.getBookInfo();
		if (null != bookInfo) {
			FileInfo fileInfo = bookInfo.getFileInfo();
			if (null != fileInfo) {
				mBookAuthors = fileInfo.authors;
				mBookTitle = fileInfo.title;
				mBookLanguage = fileInfo.language;
				mBookCover = Bitmap.createBitmap(MEDIA_COVER_WIDTH, MEDIA_COVER_HEIGHT, Bitmap.Config.RGB_565);
				Services.getCoverpageManager().drawCoverpageFor(mCoolReader.getDB(), fileInfo, mBookCover, true,
						(file, bitmap) -> mTTSControl.bind(ttsbinder -> ttsbinder.setMediaItemInfo(mBookAuthors, mBookTitle, bitmap)));
			}
		}
		// Show volume
		mTTSControl.bind(ttsbinder -> ttsbinder.retrieveVolume((current, max) -> {
			mSbVolume.setMax(max);
			mSbVolume.setProgress(current);
			CustomLog.doLog(mLogFileRoot, "log_volume.log",
					"Got volumes (2), current: " + current + ", max = " + max);
			updateSaveButtons();
		}));
		mSbVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				if (mSbVolume.getMax() < 1)
					return;
				mTTSControl.bind(ttsbinder -> ttsbinder.setVolume(progress));
				if (seekBar.getMax() == 0)
					mVolumeTextView.setText("?");
				else {
					CustomLog.doLog(mLogFileRoot, "log_volume.log",
							"Set volume to: " + progress + ", in percent = " + (100.0 * ((double) progress / (double) seekBar.getMax())));
					mVolumeTextView.setText(String.format(Locale.getDefault(), "%s (%d%%)",
							context.getString(R.string.tts_volume), Math.round(100.0 * ((double) progress / (double) seekBar.getMax()))));
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		// And finally, setup status change handler
		setupSpeechStatusHandler();
	}
}
