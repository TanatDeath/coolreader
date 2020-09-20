// Main Class
package org.coolreader;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.coolreader.cloud.CloudSync;
import org.coolreader.cloud.litres.LitresCredentialsDialog;
import org.coolreader.cloud.yandex.YndCloudSettings;
import org.coolreader.crengine.AskSomeValuesDialog;
import org.coolreader.crengine.BookInfoEntry;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.OPDSUtil;
import org.coolreader.crengine.ReadingStatRes;
import org.coolreader.dic.TranslationDirectionDialog;
import org.coolreader.dic.Dictionaries.DictionaryException;
import org.coolreader.cloud.dropbox.DBXConfig;
import org.coolreader.cloud.dropbox.DBXFinishAuthorization;
import org.coolreader.cloud.dropbox.DBXInputTokenDialog;
import org.coolreader.cloud.yandex.YNDConfig;
import org.coolreader.cloud.yandex.YNDInputTokenDialog;
import org.coolreader.crengine.AboutDialog;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.BookInfoDialog;
import org.coolreader.crengine.BookInfoEditDialog;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.BookmarksDlg;
import org.coolreader.crengine.BrowserViewLayout;
import org.coolreader.crengine.CRRootView;
import org.coolreader.crengine.CRToolBar;
import org.coolreader.crengine.CRToolBar.OnActionHandler;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DeviceOrientation;
import org.coolreader.crengine.DocConvertDialog;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.ErrorDialog;
import org.coolreader.crengine.ExternalDocCameDialog;
import org.coolreader.crengine.FileBrowser;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.GenreSAXElem;
import org.coolreader.crengine.InterfaceTheme;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.N2EpdController;
import org.coolreader.crengine.OPDSCatalogEditDialog;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.PictureCameDialog;
import org.coolreader.crengine.PictureReceived;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.ReaderViewLayout;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.donations.CRDonationService;
import org.coolreader.sync2.OnSyncStatusListener;
import org.coolreader.sync2.Synchronizer;
import org.coolreader.sync2.googledrive.GoogleDriveRemoteAccess;
import org.coolreader.crengine.UserDicEntry;
import org.coolreader.crengine.Utils;
import org.coolreader.db.BaseDB;
import org.coolreader.db.CRDBService;
import org.coolreader.db.MainDB;
import org.coolreader.geo.GeoLastData;
import org.coolreader.geo.LocationTracker;
import org.coolreader.geo.ProviderLocationTracker;
import org.coolreader.eink.sony.android.ebookdownloader.SonyBookSelector;
import org.coolreader.tts.TTS;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.provider.Settings.Secure;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CoolReader extends BaseActivity implements SensorEventListener
{
	public static final Logger log = L.create("cr");

	public Uri sdCardUri = null;
	private FileInfo fileToDelete = null;

	public DBXInputTokenDialog dbxInputTokenDialog = null;
	public YNDInputTokenDialog yndInputTokenDialog = null;
	public LitresCredentialsDialog litresCredentialsDialog = null;

	private ReaderView mReaderView;

    public class ResizeHistory {
        public int X;
        public int Y;
        public long lastSet;
        public int wasX;
        public int wasY;
        public int cnt;
    }

    public GeoLastData geoLastData = new GeoLastData(this);

    public ArrayList<ResizeHistory> getResizeHist() {
        return resizeHist;
    }

    public static String BOOK_READING_STATE_NO_STATE = "[no_state]";
	public static String BOOK_READING_STATE_TO_READ = "[to read]";
	public static String BOOK_READING_STATE_READING = "[reading]";
	public static String BOOK_READING_STATE_FINISHED = "[finished]";
	public static String READ_ALOUD = "Read aloud";

//	public ArrayList<String> getProfileNames() {
//		return profileNames;
//	}

	public ResizeHistory getNewResizeHistory() {
		ResizeHistory rh = new ResizeHistory();
    	return rh;
	}

	public void setResizeHist(ArrayList<ResizeHistory> resizeHist) {
		this.resizeHist = resizeHist;
	}

//	public void setProfileNames(ArrayList<String> profileNames) {
//		this.profileNames = profileNames;
//	}

	private ArrayList<ResizeHistory> resizeHist = new ArrayList<ResizeHistory>();
//	private ArrayList<String> profileNames = new ArrayList<String>();

	Sensor accelerometer;
	Sensor magnetometer;
	Sensor vectorSensor;
	DeviceOrientation deviceOrientation;
	SensorManager mSensorManager;

	@Override
	public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
			return;
		int ornt = deviceOrientation.getOrientation();
		if (sensorPrevRot==-1) sensorPrevRot = ornt;
		if (sensorCurRot==-1) sensorCurRot = ornt;
        if (sensorCurRot!=ornt) {
            sensorPrevRot = sensorCurRot;
			sensorCurRot = ornt;
			if (mReaderView!=null) mReaderView.resized();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public ReaderViewLayout getmReaderFrame() {
		return mReaderFrame;
	}

	public ReaderViewLayout mReaderFrame;
	private FileBrowser mBrowser;
	private View mBrowserTitleBar;
	private CRToolBar mBrowserToolBar;
	public BrowserViewLayout mBrowserFrame;
	public CRRootView mHomeFrame;
	private Engine mEngine;
	public PictureReceived picReceived = null;

	public HashMap<String, UserDicEntry> getmUserDic() {
		return mUserDic;
	}

	private HashMap<String, UserDicEntry> mUserDic;

	public HashMap<String, BaseDialog> getmBaseDialog() {
		return mBaseDialog;
	}

	private HashMap<String, BaseDialog> mBaseDialog = new HashMap<String, BaseDialog>();

	public boolean ismDictWordCorrrection() {
        return mDictWordCorrrection;
    }

    private boolean mDictWordCorrrection = false;

	public boolean ismShowUserDicPanel() {
		return mShowUserDicPanel;
	}

	private boolean mShowUserDicPanel = false;

	public boolean ismDictLongtapChange() {
		return mDictLongtapChange;
	}

	private boolean mDictLongtapChange = false;

	//View startupView;
	//CRDB mDB;
	public ViewGroup mCurrentFrame;
	public ViewGroup mPreviousFrame;

	private BookInfo mBookInfoToSync;
	private boolean mSyncGoogleDriveEnabled = false;
	private boolean mCloudSyncAskConfirmations = true;
	private boolean mSyncGoogleDriveEnabledSettings = false;
	private boolean mSyncGoogleDriveEnabledBookmarks = false;
	private boolean mSyncGoogleDriveEnabledCurrentBooks = false;
	private int mSyncGoogleDriveAutoSavePeriod = 0;
	private Synchronizer mGoogleDriveSync;
	private Timer mGoogleDriveAutoSaveTimer = null;
	// can be add more synchronizers
	private boolean mSuppressSettingsCopyToCloud;

	private String mOptionAppearance = "0";

	private String fileToLoadOnStart = null;
	private String mFileToOpenFromExt = null;

	private boolean isFirstStart = true;
	private int settingsCanBeMigratedLastInd = -1;
	private int reserveSettingsLastInd = -1;
	private int currentSettingsLastInd = -1;
	private boolean phoneStateChangeHandlerInstalled = false;
	private int initialBatteryState = -1;
	private BroadcastReceiver intentReceiver;

	private boolean justCreated = false;
	private boolean activityPaused = false;

	private boolean dataDirIsRemoved = false;

	private static final int REQUEST_CODE_STORAGE_PERM = 1;
	private static final int REQUEST_CODE_READ_PHONE_STATE_PERM = 2;
	private static final int REQUEST_CODE_GOOGLE_DRIVE_SIGN_IN = 3;
	private static final int REQUEST_CODE_OPEN_DOCUMENT_TREE = 200001;
	private static final int REQUEST_CODE_LOCATION_PERMISSION = 200002;


	public String getAndroid_id() {
		return android_id;
	}

	private String android_id = "";

	public String getModel() {
		return mModel;
	}

	private String mModel = "";

	public boolean skipFindInDic = false; // skip find in dic when bookmark toast is shown

	public String optionsFilter = ""; //filter used for options

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	startServices();
		log.i("CoolReader.onCreate() entered");
		super.onCreate(savedInstanceState);

		//AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); -- к сожалению не работает ((

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		if(mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size()!=0){
			Sensor s = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
			mSensorManager.registerListener(this,s, SensorManager.SENSOR_DELAY_NORMAL);
		}
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		deviceOrientation = new DeviceOrientation();

		// Can request only one set of permissions at a time
		// Then request all permission at a time.
		requestStoragePermissions();

		// apply settings
    	onSettingsChanged(settings(), null);

		isFirstStart = true;
		justCreated = true;

		mEngine = Engine.getInstance(this);

		//requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
		//==========================================
    	// Battery state listener
		intentReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				int level = intent.getIntExtra("level", 0);
				if (mReaderView != null)
					mReaderView.setBatteryState(level);
				else
					initialBatteryState = level;
			}
			
		};
		registerReceiver(intentReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		if (initialBatteryState >= 0 && mReaderView != null)
			mReaderView.setBatteryState(initialBatteryState);

		//==========================================
		//plotn - possibly this code hungs the start of application with no internet
		// Donations related code
//		try {
//
//			mDonationService = new CRDonationService(this);
//			mDonationService.bind();
//    		SharedPreferences pref = getSharedPreferences(DONATIONS_PREF_FILE, 0);
//    		try {
//    			mTotalDonations = pref.getFloat(DONATIONS_PREF_TOTAL_AMOUNT, 0.0f);
//    		} catch (Exception e) {
//    			log.e("exception while reading total donations from preferences", e);
//    		}
//		} catch (VerifyError e) {
//			log.e("Exception while trying to initialize billing service for donations");
//		}

		N2EpdController.n2MainActivity = this;

		android_id = Secure.getString(getApplicationContext().getContentResolver(),
				Secure.ANDROID_ID);

		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			mModel = model.toUpperCase();
		} else {
			mModel = manufacturer.toUpperCase() + " " + model;
		}

		showRootWindow();
		if (null != Engine.getExternalSettingsDirName()) {
			// if external data directory created or already exist.
			if (!Engine.DATADIR_IS_EXIST_AT_START && getExtDataDirCreateTime() > 0) {
				dataDirIsRemoved = true;
				log.e("DataDir removed by other application!");
			}
		}
		createDynShortcuts();
		createGeoListener();
		GenreSAXElem.mActivity = CoolReader.this;
		try {
			if (GenreSAXElem.elemList.size() == 0)
				GenreSAXElem.initGenreList();
		} catch (Exception e) {
			log.e("exception while init genre list", e);
		}
        log.i("CoolReader.onCreate() exiting");
    }

	public final static boolean CLOSE_BOOK_ON_STOP = false;
	
    boolean mDestroyed = false;
	@Override
	protected void onDestroy() {

		log.i("CoolReader.onDestroy() entered");
		if (!CLOSE_BOOK_ON_STOP && mReaderView != null)
			mReaderView.close();

		if (tts != null) {
			tts.shutdown();
			tts = null;
			ttsInitialized = false;
			ttsError = false;
		}
		
		
		if (mHomeFrame != null)
			mHomeFrame.onClose();
		mDestroyed = true;
		
		//if ( mReaderView!=null )
		//	mReaderView.close();
		
		//if ( mHistory!=null && mDB!=null ) {
			//history.saveToDB();
		//}

		
//		if ( BackgroundThread.instance()!=null ) {
//			BackgroundThread.instance().quit();
//		}
			
		//mEngine = null;
		if (intentReceiver != null) {
			unregisterReceiver(intentReceiver);
			intentReceiver = null;
		}
		
		//===========================
		// Donations support code
		if (mDonationService != null)
			mDonationService.unbind();

		if (mReaderView != null) {
			mReaderView.destroy();
		}
		mReaderView = null;
		
		log.i("CoolReader.onDestroy() exiting");
		super.onDestroy();

		Services.stopServices();
	}
	
	public ReaderView getReaderView() {
		return mReaderView;
	}

	@Override
	public void applyAppSetting( String key, String value )
	{
		super.applyAppSetting(key, value);
		boolean flg = "1".equals(value);
        if (key.equals(PROP_APP_DICTIONARY)) {
        	setDict(value);
        } else if (key.equals(PROP_APP_DICTIONARY_2)) {
			setDict2(value);
		} else if (key.equals(PROP_APP_DICTIONARY_3)) {
			setDict3(value);
		} else if (key.equals(PROP_APP_DICTIONARY_4)) {
			setDict4(value);
		} else if (key.equals(PROP_APP_DICTIONARY_5)) {
			setDict5(value);
		} else if (key.equals(PROP_APP_DICTIONARY_6)) {
			setDict6(value);
		} else if (key.equals(PROP_APP_DICTIONARY_7)) {
			setDict7(value);
		} else if (key.equals(PROP_APP_DICT_WORD_CORRECTION)) {
			setDictWordCorrection(value);
		} else if (key.equals(PROP_APP_SHOW_USER_DIC_PANEL)) {
			setShowUserDicPanel(value);
		} else if (key.equals(PROP_APP_DICT_LONGTAP_CHANGE)) {
			setDictLongtapChange(value);
		} else if (key.equals(PROP_TOOLBAR_APPEARANCE)) {
			setToolbarAppearance(value);
	    } else if (key.equals(PROP_APP_BOOK_SORT_ORDER)) {
        	if (mBrowser != null)
        		mBrowser.setSortOrder(value);
        } else if (key.equals(PROP_APP_SHOW_COVERPAGES)) {
        	if (mBrowser != null)
        		mBrowser.setCoverPagesEnabled(flg);
        } else if (key.equals(PROP_APP_BOOK_PROPERTY_SCAN_ENABLED)) {
        	Services.getScanner().setDirScanEnabled(flg);
        } else if (key.equals(PROP_FONT_FACE)) {
        	if (mBrowser != null)
        		mBrowser.setCoverPageFontFace(value);
        } else if (key.equals(PROP_APP_COVERPAGE_SIZE)) {
        	int n = 0;
        	try {
        		n = Integer.parseInt(value);
        	} catch (NumberFormatException e) {
        		// ignore
        	}
        	if (n < 0)
        		n = 0;
        	else if (n > 2)
        		n = 2;
        	if (mBrowser != null)
        		mBrowser.setCoverPageSizeOption(n);
        } else if (key.equals(PROP_APP_FILE_BROWSER_SIMPLE_MODE)) {
        	if (mBrowser != null)
        		mBrowser.setSimpleViewMode(flg);
        } else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_ENABLED)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveEnabled = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_CONFIRMATIONS)) {
			mCloudSyncAskConfirmations = flg;
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_SETTINGS)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveEnabledSettings = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_BOOKMARKS)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveEnabledBookmarks = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveEnabledCurrentBooks = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_AUTOSAVEPERIOD)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				int n = 0;
				try {
					n = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					// ignore
				}
				if (n < 0)
					n = 0;
				else if (n > 30)
					n = 30;
				mSyncGoogleDriveAutoSavePeriod = n;
				updateGoogleDriveSynchronizer();
			}
		}
        //
	}

	private void buildGoogleDriveSynchronizer() {
		if (null != mGoogleDriveSync)
			return;
		// build synchronizer instance
		// DeviceInfo.getSDKLevel() not applicable here -> compile error about Android API compatibility
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			GoogleDriveRemoteAccess googleDriveRemoteAccess = new GoogleDriveRemoteAccess(this, 30);
			mGoogleDriveSync = new Synchronizer(this, googleDriveRemoteAccess, getString(R.string.app_name), REQUEST_CODE_GOOGLE_DRIVE_SIGN_IN);
			mGoogleDriveSync.setOnSyncStatusListener(new OnSyncStatusListener() {
				@Override
				public void onSyncStarted(Synchronizer.SyncDirection direction, boolean forced) {
					if (Synchronizer.SyncDirection.SyncFrom == direction) {
						log.d("Starting synchronization from Google Drive");
					} else if (Synchronizer.SyncDirection.SyncTo == direction) {
						log.d("Starting synchronization to Google Drive");
					}
					if (forced || Synchronizer.SyncDirection.SyncFrom == direction) {
						// Show sync indicator only for 'Sync From' operation
						// or if this sync operation stated manually by menu action.
						if (null != mReaderView) {
							mReaderView.showCloudSyncProgress(100);
						}
					}
				}

				@Override
				public void OnSyncProgress(Synchronizer.SyncDirection direction, int current, int total, boolean forced) {
					log.v("sync progress: current=" + current + "; total=" + total);
					if (forced || Synchronizer.SyncDirection.SyncFrom == direction) {
						// Show sync indicator only for 'Sync From' operation
						// or this sync operation stated manually by menu action.
						if (null != mReaderView) {
							int total_ = total;
							if (current > total_)
								total_ = current;
							mReaderView.showCloudSyncProgress(10000 * current / total_);
						}
					}
				}

				@Override
				public void onSyncCompleted(Synchronizer.SyncDirection direction, boolean forced) {
					if (Synchronizer.SyncDirection.SyncFrom == direction) {
						log.d("Google Drive SyncFrom successfully completed");
					} else if (Synchronizer.SyncDirection.SyncTo == direction) {
						log.d("Google Drive SyncTo successfully completed");
					}
					if (forced || Synchronizer.SyncDirection.SyncFrom == direction) {
						showToast(R.string.googledrive_sync_completed);
						// Hide sync indicator
						if (null != mReaderView)
							mReaderView.hideSyncProgress();
					}
				}

				@Override
				public void onSyncError(Synchronizer.SyncDirection direction, String errorString) {
					// Hide sync indicator
					if (null != mReaderView)
						mReaderView.hideSyncProgress();
					if (null != errorString)
						showToast(R.string.googledrive_sync_failed_with, errorString);
					else
						showToast(R.string.googledrive_sync_failed);
				}

				@Override
				public void onAborted(Synchronizer.SyncDirection direction) {
					// Hide sync indicator
					if (null != mReaderView)
						mReaderView.hideSyncProgress();
					showToast(R.string.googledrive_sync_aborted);
				}

				@Override
				public void onSettingsLoaded(Properties settings, boolean forced) {
					// Apply downloaded (filtered) settings
					mSuppressSettingsCopyToCloud = true;
					mergeSettings(settings, true);
				}

				@Override
				public void onBookmarksLoaded(BookInfo bookInfo, boolean forced) {
					waitForCRDBService(() -> {
						// TODO: ask the user whether to import new bookmarks.
						Services.getHistory().updateBookInfo(bookInfo);
						getDB().saveBookInfo(bookInfo);
						if (null != mReaderView) {
							BookInfo currentBook = mReaderView.getBookInfo();
							if (null != currentBook) {
								FileInfo currentFileInfo = currentBook.getFileInfo();
								if (null != currentFileInfo) {
									if (currentFileInfo.baseEquals((bookInfo.getFileInfo()))) {
										// if the book indicated by the bookInfo is currently open.
										Bookmark lastPos = bookInfo.getLastPosition();
										if (null != lastPos) {
											if (forced || !mCloudSyncAskConfirmations) {
												mReaderView.goToBookmark(lastPos);
											} else {
												int currentPos = currentBook.getLastPosition().getPercent();
												if (Math.abs(currentPos - lastPos.getPercent()) > 10) {		// 0.1%
													askQuestion(R.string.cloud_synchronization_, R.string.sync_confirmation_new_reading_position,
															() -> mReaderView.goToBookmark(lastPos), null);
												}
											}
										}
									}
								}
							}
						}
					});
				}

				@Override
				public void onCurrentBookInfoLoaded(FileInfo fileInfo, boolean forced) {
					FileInfo current = null;
					if (null != mReaderView) {
						BookInfo bookInfo = mReaderView.getBookInfo();
						if (null != bookInfo)
							current = bookInfo.getFileInfo();
					}
					if (!fileInfo.baseEquals(current)) {
						if (forced || !mCloudSyncAskConfirmations) {
							loadDocument(fileInfo, false);
						} else {
							String shortBookInfo = "";
							if (null != fileInfo.authors && !fileInfo.authors.isEmpty())
								shortBookInfo = "\"" + fileInfo.authors + ", ";
							else
								shortBookInfo = "\"";
							shortBookInfo += fileInfo.title + "\"";
							String question = getString(R.string.sync_confirmation_other_book, shortBookInfo);
							askQuestion(getString(R.string.cloud_synchronization_), question, () -> loadDocument(fileInfo, false), null);
						}
					}
				}

			});
		}
	}

	private void updateGoogleDriveSynchronizer() {
		// DeviceInfo.getSDKLevel() not applicable here -> lint error about Android API compatibility
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mSyncGoogleDriveEnabled) {
				if (null == mGoogleDriveSync) {
					log.d("Google Drive sync is enabled.");
					buildGoogleDriveSynchronizer();
				}
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.SETTINGS, mSyncGoogleDriveEnabledSettings);
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.BOOKMARKS, mSyncGoogleDriveEnabledBookmarks);
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.CURRENTBOOKINFO, mSyncGoogleDriveEnabledCurrentBooks);
				if (null != mGoogleDriveAutoSaveTimer) {
					mGoogleDriveAutoSaveTimer.cancel();
					mGoogleDriveAutoSaveTimer = null;
				}
				if (mSyncGoogleDriveAutoSavePeriod > 0) {
					mGoogleDriveAutoSaveTimer = new Timer();
					mGoogleDriveAutoSaveTimer.schedule(new TimerTask() {
						@Override
						public void run() {
							if (!activityPaused && null != mGoogleDriveSync) {
								mGoogleDriveSync.startSyncTo(false, true, false);
							}
						}
					}, mSyncGoogleDriveAutoSavePeriod * 60000, mSyncGoogleDriveAutoSavePeriod * 60000);
				}
			} else {
				if (null != mGoogleDriveAutoSaveTimer) {
					mGoogleDriveAutoSaveTimer.cancel();
					mGoogleDriveAutoSaveTimer = null;
				}
				if (null != mGoogleDriveSync) {
					log.d("Google Drive sync is disabled.");
					// ask user: cleanup & sign out
					askConfirmation(R.string.googledrive_disabled_cleanup_question,
							() -> {
									if (null != mGoogleDriveSync) {
										mGoogleDriveSync.abort(() -> {
											if (null != mGoogleDriveSync) {
												mGoogleDriveSync.cleanupAndSignOut();
												mGoogleDriveSync = null;
											}
										});
									}
								},
							() -> {
									if (null != mGoogleDriveSync) {
										mGoogleDriveSync.abort(() -> {
											if (null != mGoogleDriveSync) {
												mGoogleDriveSync.signOut();
												mGoogleDriveSync = null;
											}
										});
									}
								}
					);
				}
			}
		}
	}

	public void forceSyncToGoogleDrive() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (null == mGoogleDriveSync)
				buildGoogleDriveSynchronizer();
			mGoogleDriveSync.startSyncTo(true, false, true);
		}
	}

	public void forceSyncFromGoogleDrive() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (null == mGoogleDriveSync)
				buildGoogleDriveSynchronizer();
			mGoogleDriveSync.startSyncFrom(true, false, true);
		}
	}

	public BookInfo getBookInfoToSync() {
		return mBookInfoToSync;
	}

	@Override
	public void setFullscreen( boolean fullscreen )
	{
		super.setFullscreen(fullscreen);
		if (mReaderFrame != null)
			mReaderFrame.updateFullscreen(fullscreen);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		log.i("onNewIntent : " + intent);
		if (mDestroyed) {
			log.e("engine is already destroyed");
			return;
		}
		processIntent(intent);
	}

	public void loadDocumentExt(String fileToOpen, String fileLink) {
		final String finalFileToOpen = fileToOpen;
		loadDocument(fileToOpen, fileLink, null, () -> BackgroundThread.instance().postGUI(() -> {
			// if document not loaded show error & then root window
			ErrorDialog errDialog = new ErrorDialog(CoolReader.this,
					CoolReader.this.getString(R.string.error), CoolReader.this.getString(R.string.cant_open_file, finalFileToOpen));
			errDialog.setOnDismissListener(dialog -> showRootWindow());
			errDialog.show();
		}, 500), true);
	}

	public void loadDocumentExt(final FileInfo fi) {
		loadDocument(fi, null, () -> BackgroundThread.instance().postGUI(() -> {
			// if document not loaded show error & then root window
			ErrorDialog errDialog = new ErrorDialog(CoolReader.this, CoolReader.this.getString(R.string.error),
					CoolReader.this.getString(R.string.cant_open_file, fi.pathname));
			errDialog.setOnDismissListener(dialog -> showRootWindow());
			errDialog.show();
		}, 500), true);
	}

	public void loadDocumentFromStreamExt(final InputStream is, final String path) {
		loadDocumentFromStream(is, path, null, () -> BackgroundThread.instance().postGUI(() -> {
			// if document not loaded show error & then root window
			ErrorDialog errDialog = new ErrorDialog(CoolReader.this, CoolReader.this.getString(R.string.error),
					CoolReader.this.getString(R.string.cant_open_file, path));
			errDialog.setOnDismissListener(dialog -> showRootWindow());
			errDialog.show();
		}, 500));
	}

	private boolean checkIntentIsYandex(String sUri) {
		if (sUri.contains(YNDConfig.YND_REDIRECT_URL)) {
			String token = "";
			if (sUri.contains("#access_token=")) {
				token = sUri.substring(sUri.indexOf("#access_token="));
				if (token.contains("&")) token=token.split("\\&")[0];
				token=token.replace("#access_token=","");
			}
			if (!StrUtils.isEmptyStr(token)) {
				YNDConfig.didLogin = true;
				CoolReader.this.showToast(R.string.ynd_auth_finished_ok);
				if (yndInputTokenDialog!=null)
					if (yndInputTokenDialog.isShowing()) {
						yndInputTokenDialog.tokenEdit.setText(token);
						yndInputTokenDialog.saveYNDToken();
						yndInputTokenDialog.onPositiveButtonClick();
					}
			}
			return true;
		}
		return false;
	}

	private boolean checkIntentIsDropbox(Uri uri, String sUri) {
		if (sUri.contains(DBXConfig.DBX_REDIRECT_URL)) {
			final String code = uri.getQueryParameter("code");
			//showToast("code = "+code);
			if (!StrUtils.isEmptyStr(code)) {
				new DBXFinishAuthorization(this, uri, new DBXFinishAuthorization.Callback() {
					@Override
					public void onComplete(boolean result, String accessToken) {
						DBXConfig.didLogin = result;
						CoolReader.this.showToast(R.string.dbx_auth_finished_ok);
						if (dbxInputTokenDialog!=null)
							if (dbxInputTokenDialog.isShowing()) {
								dbxInputTokenDialog.saveDBXToken(accessToken);
								dbxInputTokenDialog.onPositiveButtonClick();
							}
					}

					@Override
					public void onError(Exception e) {
						DBXConfig.didLogin = false;
						CoolReader.this.showToast(getString(R.string.dbx_auth_finished_error) + ": " + e.getMessage());
					}
				}).execute(code);
			}
			return true;
		}
		return false;
	}

	private void processIntentMultipleImages(ArrayList<Uri> imageUris) {
		showToast("Not implemented yet");
	}

	private void processIntentContent(String stype, Object obj) {
		if (stype.startsWith("image/")) {
			PictureCameDialog dlg = new PictureCameDialog(CoolReader.this, obj, stype, "");
			dlg.show();
		} else {
			ExternalDocCameDialog dlg = new ExternalDocCameDialog(CoolReader.this, stype, obj);
			dlg.show();
		}
	}

//	private boolean checkIntentIsBitmap(String fFName) {
//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inJustDecodeBounds = true;
//		Bitmap bitmap = BitmapFactory.decodeFile(fFName, options);
//		if (options.outWidth != -1 && options.outHeight != -1) {
//			File f = new File(fFName);
//			File f2 = new File(fFName.replace(".html", ""));
//			f.renameTo(f2);
//			BackgroundThread.instance().postBackground(new Runnable() {
//				@Override
//				public void run() {
//					BackgroundThread.instance().postGUI(new Runnable() {
//						@Override
//						public void run() {
//							PictureCameDialog dlg = new PictureCameDialog(CoolReader.this,
//									fFName.replace(".html", ""), "", "");
//							dlg.show();
//						}
//					}, 500);
//				}
//			});
//			return true;
//		}
//		return false;
//	}

	private boolean checkIsShortcuts(String fileToOpen) {
		if (fileToOpen.equals(FileInfo.RECENT_DIR_TAG)) {
			this.showRecentBooks();
			return true;
		}
		if (fileToOpen.equals(FileInfo.STATE_READING_TAG)) {
			final FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = fileToOpen;
			dir.setFilename(this.getString(R.string.folder_name_books_by_state_reading));
			dir.isListed = true;
			dir.isScanned = true;
			waitForCRDBService(() -> showDirectory(dir, ""));
			return true;
		}
		if (fileToOpen.equals(FileInfo.STATE_TO_READ_TAG)) {
			final FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = fileToOpen;
			dir.setFilename(this.getString(R.string.folder_name_books_by_state_to_read));
			dir.isListed = true;
			dir.isScanned = true;
			waitForCRDBService(() -> showDirectory(dir, ""));
			return true;
		}
		if (fileToOpen.equals(FileInfo.STATE_FINISHED_TAG)) {
			final FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = fileToOpen;
			dir.setFilename( this.getString(R.string.folder_name_books_by_state_finished));
			dir.isListed = true;
			dir.isScanned = true;
			waitForCRDBService(() -> showDirectory(dir, ""));
			return true;
		}
		if (fileToOpen.equals(FileInfo.SEARCH_SHORTCUT_TAG)) {
			final FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = fileToOpen;
			dir.setFilename(this.getString(R.string.dlg_book_search));
			dir.isListed = true;
			dir.isScanned = true;
			waitForCRDBService(() -> showDirectory(dir, ""));
			return true;
		}
		return false;
	}

	private boolean checkOpenDocumentFormat(String fileToOpen) {
		if (
				//(fileToOpen.toUpperCase().endsWith(".ODT"))||
				(fileToOpen.toUpperCase().endsWith(".ODS"))||
				(fileToOpen.toUpperCase().endsWith(".ODP"))
		) {
			DocConvertDialog dlgConv = new DocConvertDialog(this, fileToOpen);
			dlgConv.show();
			return true;
		}
		return false;
	}

	private boolean checkPictureExtension(String fileToOpen) {
		if (
				(fileToOpen.toLowerCase().endsWith(".jpg"))||
				(fileToOpen.toLowerCase().endsWith(".jpeg"))||
				(fileToOpen.toLowerCase().endsWith(".png"))
		) {
			pictureCame(fileToOpen);
			return true;
		}
		return false;
	}

	public boolean processIntent(final Intent intent) {
		log.d("intent=" + intent);
		if (intent == null)
			return false;
		String fileToOpen = null;
		mFileToOpenFromExt = null;
		Uri uri = null;
		String intentAction = StrUtils.getNonEmptyStr(intent.getAction(),false);
		String processText = intent.getStringExtra("PROCESS_TEXT");
		if (!StrUtils.isEmptyStr(processText)) {
			boolean allOk = false;
			if (getReaderView() != null)
				if (getReaderView().getBookInfo() != null)
					if (getReaderView().lastSelection!=null)
						if (!getReaderView().lastSelection.isEmpty()) {
							allOk = true;
							getReaderView().showNewBookmarkDialog(getReaderView().lastSelection, Bookmark.TYPE_USER_DIC, processText);
				}
			if (!allOk) showToast(R.string.no_selection);
			return true;
		}
		if (Intent.ACTION_MAIN.equals(intentAction)) intentAction = Intent.ACTION_VIEW; //hack for Onyx
		if (Intent.ACTION_VIEW.equals(intentAction)) {
			uri = intent.getData();
			String stype = StrUtils.getNonEmptyStr(intent.getType(),false);
			if (uri != null) uri.getLastPathSegment();
			intent.setData(null);
			String sUri = "";
			if (uri != null) sUri = StrUtils.getNonEmptyStr(uri.toString(),false);
			if (checkIntentIsYandex(sUri)) return true;
			if (checkIntentIsDropbox(uri, sUri)) return true;
			if (uri != null) fileToOpen = filePathFromUri(uri);
			if ((sUri.startsWith("content")) && (StrUtils.isEmptyStr(fileToOpen))) {
				processIntentContent(stype, uri);
				return true;
			}
		}
		if (Intent.ACTION_SEND_MULTIPLE.equals(intentAction)) {
			if (StrUtils.getNonEmptyStr(intent.getType(),false).startsWith("image/")) {
				ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				if (imageUris != null) processIntentMultipleImages(imageUris);
			}
			return true;
		}
		if (Intent.ACTION_SEND.equals(intentAction)) {
			String sText = StrUtils.getNonEmptyStr(intent.getStringExtra(Intent.EXTRA_TEXT),false);
			String stype = StrUtils.getNonEmptyStr(intent.getType(),false);
			Uri imageUri1 = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			if (sText.toLowerCase().startsWith("http")) {
				//processIntentHttp(sText);
				if (!BaseActivity.PREMIUM_FEATURES) {
					showToast(R.string.only_in_premium);
					return true;
				}
				processIntentContent("", sText);
				return true;
			}
			if (StrUtils.getNonEmptyStr(intent.getType(),false).startsWith("image/")) {
				Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
				if (imageUri != null) {
					PictureCameDialog dlg = new PictureCameDialog(CoolReader.this, imageUri, intent.getType(), "");
					dlg.show();
					return true;
				}
			} else {
				fileToOpen = StrUtils.getNonEmptyStr(intent.getStringExtra(Intent.EXTRA_SUBJECT),false);
				if (!StrUtils.isEmptyStr(fileToOpen)) {
					if (checkIsShortcuts(fileToOpen)) return true;
					String sText1 = StrUtils.getNonEmptyStr(intent.getStringExtra(Intent.EXTRA_TEXT),false);
					if (StrUtils.isEmptyStr(sText1)) showToast(R.string.warn_empty_file_name);
				}
				return true;
			}
		}
		if (StrUtils.isEmptyStr(fileToOpen) && intent.getExtras() != null) {
			log.d("extras=" + intent.getExtras());
			fileToOpen = StrUtils.getNonEmptyStr(intent.getExtras().getString(OPEN_FILE_PARAM),false);
		}
		if (!StrUtils.isEmptyStr(fileToOpen)) {
			mFileToOpenFromExt = fileToOpen;
			log.d("FILE_TO_OPEN = " + fileToOpen);
			if (checkOpenDocumentFormat(fileToOpen)) return true;
			if (checkPictureExtension(fileToOpen)) return true;
			loadDocumentExt(fileToOpen, "");
			return true;
		} else if (null != uri) {
			log.d("URI_TO_OPEN = " + uri);
			final String uriString = uri.toString();
			mFileToOpenFromExt = uriString;
			loadDocumentFromUriExt(uri, uriString);
			return true;
		} else {
			log.d("No file to open");
			return false;
		}
	}

	private String filePathFromUri(Uri uri) {
		if (null == uri)
			return null;
		String filePath = null;
		String scheme = uri.getScheme();
		String host = uri.getHost();
		if ("file".equals(scheme)) {
			filePath = uri.getPath();
			// patch for opening of books from ReLaunch (under Nook Simple Touch)
			if (null != filePath) {
				if (filePath.contains("%2F"))
					filePath = filePath.replace("%2F", "/");
			}
		} else if (("content".equals(scheme))||("coolreader".equals(scheme))||("knownreader".equals(scheme))) {
			if (uri.getEncodedPath().contains("%00"))
				filePath = uri.getEncodedPath();
			else
				filePath = uri.getPath();
			if (null != filePath) {
				// parse uri from system filemanager
				if (filePath.contains("%00")) {
					// splitter between archive file name and inner file.
					filePath = filePath.replace("%00", "@/");
					filePath = Uri.decode(filePath);
				}
				if ("com.android.externalstorage.documents".equals(host)) {
					// application "Files" by Google, package="com.android.externalstorage.documents"
					if (filePath.matches("^/document/.*:.*$")) {
						// decode special uri form: /document/primary:<somebody>
						//                          /document/XXXX-XXXX:<somebody>
						String shortcut = filePath.replaceFirst("^/document/(.*):.*$", "$1");
						String mountRoot = Engine.getMountRootByShortcut(shortcut);
						if (mountRoot != null) {
							filePath = filePath.replaceFirst("^/document/.*:(.*)$", mountRoot + "/$1");
						}
					}
				} else if ("com.google.android.apps.nbu.files.provider".equals(host)) {
					// application "Files" by Google, package="com.google.android.apps.nbu.files"
					if (filePath.startsWith("/1////")) {
						// skip "/1///"
						filePath = filePath.substring(5);
						filePath = Uri.decode(filePath);
					} else if (filePath.startsWith("/1/file:///")) {
						// skip "/1/file://"
						filePath = filePath.substring(10);
						filePath = Uri.decode(filePath);
					}
				} else {
					// Try some common conversions...
					if (filePath.startsWith("/file%3A%2F%2F")) {
						filePath = filePath.substring(14);
						filePath = Uri.decode(filePath);
						if (filePath.contains("%20")) {
							filePath = filePath.replace("%20", " ");
						}
					}
				}
			}
		}
		if (null != filePath) {
			File file;
			int pos = filePath.indexOf("@/");
			if (pos > 0)
				file = new File(filePath.substring(0, pos));
			else
				file = new File(filePath);
			if (!file.exists())
				filePath = null;

		}
		return filePath;
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(deviceOrientation.getEventListener());
        mSensorManager.unregisterListener(this);
		geoLastData.gps.stop(); geoLastData.netw.stop();
		if (mReaderView != null) {
			// save book info to "sync to" as in the actual sync operation the readerView is no longer available
			BookInfo bookInfo = mReaderView.getBookInfo();
			if (null != bookInfo && null != bookInfo.getFileInfo()) {
				// make copy
				mBookInfoToSync = new BookInfo(bookInfo);
			}
			mReaderView.onAppPause();
		}
		Services.getCoverpageManager().removeCoverpageReadyListener(mHomeFrame);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mSyncGoogleDriveEnabled && mGoogleDriveSync != null && !mGoogleDriveSync.isBusy()) {
				mGoogleDriveSync.startSyncTo(false, true, false);
			}
		}
		activityPaused = true;
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		log.i("CoolReader.onPostCreate()");
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onPostResume() {
		log.i("CoolReader.onPostResume()");
		super.onPostResume();
	}

//	private boolean restarted = false;
	@Override
	protected void onRestart() {
		log.i("CoolReader.onRestart()");
		//restarted = true;
		super.onRestart();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		log.i("CoolReader.onRestoreInstanceState()");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		if (null == mFileToOpenFromExt)
			log.i("CoolReader.onResume()");
		else
			log.i("CoolReader.onResume(), mFileToOpenFromExt=" + mFileToOpenFromExt);
		super.onResume();

		mSensorManager.registerListener(deviceOrientation.getEventListener(), accelerometer, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(deviceOrientation.getEventListener(), magnetometer, SensorManager.SENSOR_DELAY_UI);

		int iGeo = settings().getInt(Settings.PROP_APP_GEO, 0);
		if (iGeo>1) {
			geoLastData.gps.start(geoLastData.geoListener);
			geoLastData.netw.start(geoLastData.netwListener);
		}

        if(mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size()!=0){
            Sensor s = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            mSensorManager.registerListener(this,s, SensorManager.SENSOR_DELAY_NORMAL);
        }

		//Properties props = SettingsManager.instance(this).get();
		
		if (mReaderView != null)
			mReaderView.onAppResume();
		
		if (DeviceInfo.isEinkScreen(getScreenForceEink())) {
            if (DeviceInfo.EINK_SONY) {
                SharedPreferences pref = getSharedPreferences(PREF_FILE, 0);
                String res = pref.getString(PREF_LAST_BOOK, null);
                if( res != null && res.length() > 0 ) {
                    SonyBookSelector selector = new SonyBookSelector(this);
                    long l = selector.getContentId(res);
                    if(l != 0) {
                       selector.setReadingTime(l);
                       selector.requestBookSelection(l);
                    }
                }
            }
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mSyncGoogleDriveEnabled && mGoogleDriveSync != null && !mGoogleDriveSync.isBusy()) {
				// when the program starts, the local settings file is already updated, so the local file is always newer than the remote one
				// Therefore, the synchronization mode is quiet, i.e. without comparing modification times and without prompting the user for action.
				// If the file is opened from an external file manager, we must disable the "currently reading book" sync operation with google drive.
				if (null == mFileToOpenFromExt)
					mGoogleDriveSync.startSyncFrom(true, true, false);
				else
					mGoogleDriveSync.startSyncFromOnly(true, Synchronizer.SyncTarget.SETTINGS, Synchronizer.SyncTarget.BOOKMARKS);
			}
		}
		if (getReaderView()!=null) {
			if (getReaderView().ttsToolbar != null)
				getReaderView().ttsToolbar.repaintButtons();
			BackgroundThread.instance().postGUI(() -> {
				log.i("Load last rpos from CLOUD");
				int iSyncVariant3 = settings().getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant3 != 0) {
					if (mCurrentFrame == mReaderFrame)
						CloudSync.loadFromJsonInfoFileList(CoolReader.this,
								CloudSync.CLOUD_SAVE_READING_POS, true, iSyncVariant3 == 1, true, true);
				}
			}, 5000);
		}
		activityPaused = false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		log.i("CoolReader.onSaveInstanceState()");
		super.onSaveInstanceState(outState);
	}

	static final boolean LOAD_LAST_DOCUMENT_ON_START = true;

	private void checkIfWeCanMigrateSettings() {
		int i=0;
		if (!getSettingsFileExtExists("cr3e",0)) {
			if (getSettingsFileExtExists(".cr3", 0))
				if (getSettingsFileExt(".cr3", 0).isFile()) {
					settingsCanBeMigratedLastInd = 0;
					i++;
					boolean more = true;
					while (more) {
						if (!(getSettingsFileExtExists(".cr3", i)))
							more = false;
						else
							settingsCanBeMigratedLastInd = i;
						i++;
					}
			}
		} else {
			currentSettingsLastInd = 0;
			boolean more = true;
			while (more) {
				if (!getSettingsFileExtExists("cr3e",i))
					more = false;
				else
					currentSettingsLastInd = i;
				i++;
			}
		}
		if (getSettingsFileExtExists("[DEFAULT]",0)) {
			reserveSettingsLastInd = 0;
			boolean more = true;
			while (more) {
				if (!getSettingsFileExtExists("[DEFAULT]",i))
					more = false;
				else
					reserveSettingsLastInd = i;
				i++;
			}
		}
	}

	public void backupSettings() {
		if (currentSettingsLastInd >= 0) {
			for (int i = 0; i<=currentSettingsLastInd; i++) {
				File fromFile = getSettingsFileExt("cr3e",i);
				File toFile = getSettingsFileExt("[DEFAULT]",i);
				if ((fromFile != null) && (toFile != null))
					if ((fromFile.exists()) && (fromFile.isFile())) {
						boolean bEq = false;
						boolean bDestExists = false;
						if ((toFile.exists()) && (toFile.isFile())) {
							bDestExists = true;
							if (fromFile.getAbsolutePath().equals(toFile.getAbsolutePath()))
								bEq = true;
						}
						if (!bEq) {
							boolean bOk = false;
							if (bDestExists) bOk = toFile.delete();
							if (bOk) Utils.copyFile(fromFile, toFile);
						}
					}
			}
		}
	}
	
	@Override
	protected void onStart() {
		log.i("KnownReader.onStart() version=" + getVersion() + ", fileToLoadOnStart=" + fileToLoadOnStart);
		super.onStart();

		//		BackgroundThread.instance().postGUI(new Runnable() {
//			public void run() {
//				// fixing font settings
//				Properties settings = mReaderView.getSettings();
//				if (SettingsManager.instance(CoolReader.this).fixFontSettings(settings)) {
//					log.i("Missing font settings were fixed");
//					mBrowser.setCoverPageFontFace(settings.getProperty(ReaderView.PROP_FONT_FACE, DeviceInfo.DEF_FONT_FACE));
//					mReaderView.setSettings(settings, null);
//				}
//			}
//		});

		waitForCRDBService(() -> {
			BaseDB db = Services.getHistory().getMainDB(getDB());
			BaseDB cdb = Services.getHistory().getCoverDB(getDB());
			String sMesg ="";
			if (db !=null) {
				if (!StrUtils.isEmptyStr(db.mOriginalDBFile)) {
					sMesg = sMesg + "MainDB:" + getString(R.string.db_action_corrupted) + ". \n";
					sMesg = sMesg + "MainDB:" + getString(R.string.db_action_performed_ren) +
							db.mOriginalDBFile + " -> " + db.mBackedDBFile + " \n";
				}
				if ((!StrUtils.isEmptyStr(db.mDeletedDBFile))||(!StrUtils.isEmptyStr(db.mBackupRestoredDBFile))) {
					sMesg = sMesg + "MainDB:" + getString(R.string.db_action_restored) + ". \n";
					if (!StrUtils.isEmptyStr(db.mDeletedDBFile))
						sMesg = sMesg+"MainDB:"+ getString(R.string.db_action_performed_del)+" "+
							db.mDeletedDBFile+" \n";
					if (!StrUtils.isEmptyStr(db.mBackupRestoredDBFile))
						sMesg = sMesg + "MainDB:" + getString(R.string.db_action_performed_ren)+" " +
								db.mBackupRestoredDBFile + " -> " + db.mBackupRestoredToDBFile + " \n";
				}
			}
			if (cdb !=null) {
				if (!StrUtils.isEmptyStr(cdb.mOriginalDBFile)) {
					sMesg = sMesg + "CoverDB:" + getString(R.string.db_action_corrupted) + ". \n";
					sMesg = sMesg + "CoverDB:" + getString(R.string.db_action_performed_ren)+" " +
							cdb.mOriginalDBFile + " -> " + cdb.mBackedDBFile + " \n";
				}
				if ((!StrUtils.isEmptyStr(cdb.mDeletedDBFile))||(!StrUtils.isEmptyStr(cdb.mBackupRestoredDBFile))) {
					sMesg = sMesg + "CoverDB:" + getString(R.string.db_action_restored) + ". \n";
					if (!StrUtils.isEmptyStr(cdb.mDeletedDBFile))
						sMesg = sMesg+"CoverDB:"+ getString(R.string.db_action_performed_del)+" "+
								cdb.mDeletedDBFile+" \n";
					if (!StrUtils.isEmptyStr(cdb.mBackupRestoredDBFile))
						sMesg = sMesg + "CoverDB:" + getString(R.string.db_action_performed_ren)+" " +
								cdb.mBackupRestoredDBFile + " -> " + cdb.mBackupRestoredToDBFile + " \n";
				}
			}
			if (!StrUtils.isEmptyStr(sMesg)) {
				notificationDB(sMesg);
				if (db !=null) {
					db.mOriginalDBFile = "";
					db.mBackedDBFile = "";
					db.mDeletedDBFile = "";
					db.mBackupRestoredDBFile ="";
				}
				if (cdb !=null) {
					cdb.mOriginalDBFile = "";
					cdb.mBackedDBFile = "";
					cdb.mDeletedDBFile = "";
					cdb.mBackupRestoredDBFile ="";
				}
			}
		});

		if (mHomeFrame == null) {
			waitForCRDBService(() -> {
				Services.getHistory().loadFromDB(getDB(), 200);

				mHomeFrame = new CRRootView(CoolReader.this);
				Services.getCoverpageManager().addCoverpageReadyListener(mHomeFrame);
				mHomeFrame.requestFocus();

				showRootWindow();
				setSystemUiVisibility();

				notifySettingsChanged();

				showNotifications();
			});
		}

		if (mUserDic == null) {
			waitForCRDBService(() -> getDB().loadUserDic(new CRDBService.UserDicLoadingCallback() {
					@Override
					public void onUserDicLoaded(HashMap<String, UserDicEntry> list) {
						mUserDic = list;
					}
				}));
		}

		if (isBookOpened()) {
			showOpenedBook();
			return;
		}
		
		if (!isFirstStart)
			return;
		isFirstStart = false;
		
		if (justCreated) {
			justCreated = false;
			if (!processIntent(getIntent()))
				showLastLocation();
		}
		if (dataDirIsRemoved) {
			// show message
			showNotice(getString(R.string.datadir_is_removed, Engine.getExternalSettingsDirName()), () -> {
				if (settingsCanBeMigratedLastInd>=0) {
					showNotice(R.string.note1_can_be_migrated,
							() -> {},
							() -> {});
				}
			}, null);
			// this could cause to show book not in correct dimrnions - till it was resized (by sensor etc.) - so I replaced by notice
			//ErrorDialog dlg = new ErrorDialog(this, getString(R.string.error), getString(R.string.datadir_is_removed, Engine.getExternalSettingsDirName()));
			//dlg.show();
		} else {
			if (settingsCanBeMigratedLastInd >= 0) {
				showNotice(R.string.note1_can_be_migrated,
						() -> {},
						() -> {});
			}
		}
		if (Engine.getExternalSettingsDirName() != null) {
			setExtDataDirCreateTime(new Date());
		} else {
			setExtDataDirCreateTime(null);
		}
		stopped = false;

		log.i("CoolReader.onStart() exiting");
	}
	
 

	private boolean stopped = false;
	@Override
	protected void onStop() {
		log.i("CoolReader.onStop() entering");
		// Donations support code
		super.onStop();
		stopped = true;
		// will close book at onDestroy()
		if (CLOSE_BOOK_ON_STOP)
			mReaderView.close();

		
		log.i("CoolReader.onStop() exiting");
	}

	private void requestStoragePermissions() {
		// check or request permission for storage
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int readExtStoragePermissionCheck = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
			int writeExtStoragePermissionCheck = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			ArrayList<String> needPerms = new ArrayList<>();
			if (PackageManager.PERMISSION_GRANTED != readExtStoragePermissionCheck) {
				needPerms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			} else {
				log.i("READ_EXTERNAL_STORAGE permission already granted.");
			}
			if (PackageManager.PERMISSION_GRANTED != writeExtStoragePermissionCheck) {
				needPerms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			} else {
				log.i("WRITE_EXTERNAL_STORAGE permission already granted.");
			}
			if (!needPerms.isEmpty()) {
				// TODO: Show an explanation to the user
				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				String[] templ = new String[0];
				log.i("Some permissions DENIED, resting from user these permissions: " + needPerms.toString());
				// request permission from user
				requestPermissions(needPerms.toArray(templ), REQUEST_CODE_STORAGE_PERM);
			} else {
				checkIfWeCanMigrateSettings();
				backupSettings();
			}
		} else {
			checkIfWeCanMigrateSettings();
			backupSettings();
		}
	}

	private void requestReadPhoneStatePermissions() {
		// check or request permission to read phone state
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int phoneStatePermissionCheck = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
			if (PackageManager.PERMISSION_GRANTED != phoneStatePermissionCheck) {
				log.i("READ_PHONE_STATE permission DENIED, requesting from user");
				// TODO: Show an explanation to the user
				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				// request permission from user
				requestPermissions(new String[] { Manifest.permission.READ_PHONE_STATE } , REQUEST_CODE_READ_PHONE_STATE_PERM);
			} else {
				log.i("READ_PHONE_STATE permission already granted.");
			}
		}
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		log.i("CoolReader.onRequestPermissionsResult()");
		if (REQUEST_CODE_STORAGE_PERM == requestCode) {		// external storage read & write permissions
			int ext_sd_perm_count = 0;
			//boolean read_phone_state_granted = false;
			for (int i = 0; i < permissions.length; i++) {
				if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
					log.i("Permission " + permissions[i] + " GRANTED");
				else
					log.i("Permission " + permissions[i] + " DENIED");
				if (permissions[i].compareTo(Manifest.permission.READ_EXTERNAL_STORAGE) == 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED)
					ext_sd_perm_count++;
				else if (permissions[i].compareTo(Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED)
					ext_sd_perm_count++;
			}
			if (2 == ext_sd_perm_count) {
				log.i("read&write to storage permissions GRANTED, adding sd card mount point...");
				Services.refreshServices(this);
				checkIfWeCanMigrateSettings();
				rebaseSettings();
				if (settingsCanBeMigratedLastInd>=0) {
					showNotice(R.string.note1_can_be_migrated,
							() -> {},
							() -> {});
				}
				waitForCRDBService(() -> {
					getDBService().setPathCorrector(Engine.getInstance(CoolReader.this).getPathCorrector());
					getDB().reopenDatabase();
					Services.getHistory().loadFromDB(getDB(), 200);
				});
				mHomeFrame.refreshView();
			}
			if (Engine.getExternalSettingsDirName() != null) {
				setExtDataDirCreateTime(new Date());
			} else {
				setExtDataDirCreateTime(null);
			}
		} else if (REQUEST_CODE_READ_PHONE_STATE_PERM == requestCode) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				log.i("read phone state permission is GRANTED, registering phone activity handler...");
				PhoneStateReceiver.setPhoneActivityHandler(() -> {
					if (mReaderView != null) {
						mReaderView.stopTTS();
						mReaderView.save();
					}
				});
				phoneStateChangeHandlerInstalled = true;
			} else {
				log.i("Read phone state permission is DENIED!");
			}
		}
	}

	private static Debug.MemoryInfo info = new Debug.MemoryInfo();
	private static Field[] infoFields = Debug.MemoryInfo.class.getFields();
	private static String dumpFields( Field[] fields, Object obj) {
		StringBuilder buf = new StringBuilder();
		try {
			for ( Field f : fields ) {
				if (buf.length() > 0)
					buf.append(", ");
				buf.append(f.getName());
				buf.append("=");
				buf.append(f.get(obj));
			}
		} catch ( Exception e ) {
			
		}
		return buf.toString();
	}
	public static void dumpHeapAllocation() {
		Debug.getMemoryInfo(info);
		log.d("nativeHeapAlloc=" + Debug.getNativeHeapAllocatedSize() + ", nativeHeapSize=" + Debug.getNativeHeapSize() + ", info: " + dumpFields(infoFields, info));
	}
	

	
	@Override
	public void setCurrentTheme(InterfaceTheme theme) {
		super.setCurrentTheme(theme);
		if (mHomeFrame != null)
			mHomeFrame.onThemeChange(theme);
		if (mBrowser != null)
			mBrowser.onThemeChanged();
		if (mBrowserFrame != null)
			mBrowserFrame.onThemeChanged(theme);
		//getWindow().setBackgroundDrawable(theme.getActionBarBackgroundDrawableBrowser());
	}

	public void directoryUpdated(FileInfo dir, FileInfo selected) {
		if (dir!=null) {
			if (dir.isOPDSRoot())
				mHomeFrame.refreshOnlineCatalogs(true);
			else if (dir.isRecentDir())
				mHomeFrame.refreshRecentBooks();
			if (mBrowser != null)
				mBrowser.refreshDirectory(dir, selected);
		}
	}
	public void directoryUpdated(FileInfo dir) {
		directoryUpdated(dir, null);
	}

	@Override
	public void onSettingsChanged(Properties props, Properties oldProps) {
		Properties changedProps = oldProps != null ? props.diff(oldProps) : props;
		if (mHomeFrame != null) {
			mHomeFrame.refreshOnlineCatalogs(true);
		}
		if (mReaderFrame != null) {
			mReaderFrame.updateSettings(props);
			if (mReaderView != null)
				mReaderView.updateSettings(props);
		}
        for ( Map.Entry<Object, Object> entry : changedProps.entrySet() ) {
    		String key = (String)entry.getKey();
    		final String value = (String)entry.getValue();
    		applyAppSetting( key, value );
    		if (key.equals(PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE)) {
				waitForCRDBService(() -> {
					BaseDB db = Services.getHistory().getMainDB(getDB());
					((MainDB) db).iMaxGroupSize = Integer.valueOf(value);
				});
			}
        }
		BOOK_READING_STATE_NO_STATE = "["+getString(R.string.book_state_none)+"]";
		BOOK_READING_STATE_TO_READ = "["+getString(R.string.book_state_toread)+"]";
		BOOK_READING_STATE_READING = "["+getString(R.string.book_state_reading)+"]";
		BOOK_READING_STATE_FINISHED = "["+getString(R.string.book_state_finished)+"]";
		READ_ALOUD = getString(R.string.read_aloud);

		// Show/Hide soft navbar after OptionDialog is closed.
		applyFullscreen(getWindow());
		if (changedProps.size() > 0) {
			// After all, sync to the cloud with delay
			BackgroundThread.instance().postGUI(() -> {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					if (mSyncGoogleDriveEnabled && mSyncGoogleDriveEnabledSettings && null != mGoogleDriveSync) {
						if (mSuppressSettingsCopyToCloud) {
							// Immediately after downloading settings from Google Drive
							// prevent uploading settings file
							mSuppressSettingsCopyToCloud = false;
						} else if (!mGoogleDriveSync.isBusy()) {
							// After setting changed in OptionsDialog
							log.d("Some settings is changed, uploading to cloud...");
							mGoogleDriveSync.startSyncToOnly(false, Synchronizer.SyncTarget.SETTINGS);
						}
					}
				}
			}, 1000);
		}
	}

    protected boolean allowLowBrightness() {
    	// override to force higher brightness in non-reading mode (to avoid black screen on some devices when brightness level set to small value)
    	return mCurrentFrame == mReaderFrame;
    }
    

	public ViewGroup getPreviousFrame() {
		return mPreviousFrame;
	}
	
	public boolean isPreviousFrameHome() {
		return mPreviousFrame != null && mPreviousFrame == mHomeFrame;
	}

	@SuppressWarnings("unchecked")
	private void setCurrentFrame(ViewGroup newFrame) {
		if (newFrame == mReaderFrame)
			setCutoutMode(this.iCutoutMode);
		else
			setCutoutModeRaw(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT);
		if (mCurrentFrame != newFrame) {
			mPreviousFrame = mCurrentFrame;
			log.i("New current frame: " + newFrame.getClass().toString());
			if (mCurrentFrame == mBrowserFrame) {
				FileBrowser.mListPosCacheOld = (HashMap<String, Integer>) FileBrowser.mListPosCache.clone();
			}
			mCurrentFrame = newFrame;
			setContentView(mCurrentFrame);
			mCurrentFrame.requestFocus();
			if (mCurrentFrame != mReaderFrame)
				releaseBacklightControl();
			if (mCurrentFrame == mHomeFrame) {
				// update recent books
				mHomeFrame.refreshRecentBooks();
				mHomeFrame.refreshFileSystemFolders(true);
				if (mHomeFrame.needRefreshOnlineCatalogs) refreshOPDSRootDirectory(false);
				setLastLocationRoot();
				mCurrentFrame.invalidate();
			}
			if (mCurrentFrame == mBrowserFrame) {
				// update recent books directory
				mBrowser.refreshDirectory(Services.getScanner().getRecentDir(), null);
				mBrowser.scrollToLastPos();
			}
			onUserActivity();
		}
	}
	
	public void showReader() {
		runInReader(() -> {
			// do nothing
		});
	}
	
	public void showRootWindow() {
		if ((mCurrentFrame != mReaderFrame) || (mReaderFrame == null)) {
			setCurrentFrame(mHomeFrame);
		} else {
			ArrayList<String[]> vl = new ArrayList<String[]>();
			final AskSomeValuesDialog dlgA = new AskSomeValuesDialog(
					this,
					"",
					"",
					vl, results -> {
			});
			dlgA.show();
			BackgroundThread.instance().postGUI(() -> {
				if (dlgA != null)
					if (dlgA.isShowing()) {
						dlgA.dismiss();
					}
				setCurrentFrame(mHomeFrame);
			}, 200);
		}
	}
	
	private void runInReader(final Runnable task) {
		waitForCRDBService(() -> {
			if (mReaderFrame != null) {
				task.run();
				setCurrentFrame(mReaderFrame);
				if (mReaderView != null && mReaderView.getSurface() != null) {
					mReaderView.getSurface().setFocusable(true);
					mReaderView.getSurface().setFocusableInTouchMode(true);
					mReaderView.getSurface().requestFocus();
				} else {
					log.w("runInReader: mReaderView or mReaderView.getSurface() is null");
				}
			} else {
				mReaderView = new ReaderView(CoolReader.this, mEngine, settings());
				mReaderFrame = new ReaderViewLayout(CoolReader.this, mReaderView);
				mReaderFrame.getToolBar().setOnActionHandler(item -> {
					if (mReaderView != null)
						mReaderView.onAction(item);
					return true;
				});
				task.run();
				setCurrentFrame(mReaderFrame);
				if (mReaderView.getSurface() != null) {
					mReaderView.getSurface().setFocusable(true);
					mReaderView.getSurface().setFocusableInTouchMode(true);
					mReaderView.getSurface().requestFocus();
				}
				if (initialBatteryState >= 0)
					mReaderView.setBatteryState(initialBatteryState);
			}
		});
		
	}
	
	public boolean isBrowserCreated() {
		return mBrowserFrame != null;
	}
	
	private void runInBrowser(final Runnable task, final boolean dontShowBrowser) {
		waitForCRDBService(() -> {
			if (mBrowserFrame == null) {
				mBrowser = new FileBrowser(CoolReader.this, Services.getEngine(), Services.getScanner(), Services.getHistory());
				mBrowser.setCoverPagesEnabled(settings().getBool(ReaderView.PROP_APP_SHOW_COVERPAGES, true));
				mBrowser.setCoverPageFontFace(settings().getProperty(ReaderView.PROP_FONT_FACE, DeviceInfo.DEF_FONT_FACE));
				mBrowser.setCoverPageSizeOption(settings().getInt(ReaderView.PROP_APP_COVERPAGE_SIZE, 1));
				mBrowser.setSortOrder(settings().getProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER));
				mBrowser.setSimpleViewMode(settings().getBool(ReaderView.PROP_APP_FILE_BROWSER_SIMPLE_MODE, false));
				mBrowser.init();

				LayoutInflater inflater = LayoutInflater.from(CoolReader.this);// activity.getLayoutInflater();

				mBrowserTitleBar = inflater.inflate(R.layout.browser_status_bar, null);
				setBrowserTitle("Cool Reader browser window", null);


				mBrowserToolBar = new CRToolBar(CoolReader.this, ReaderAction.createList(
						ReaderAction.FILE_BROWSER_UP,
						ReaderAction.CURRENT_BOOK,
						ReaderAction.OPTIONS,
						ReaderAction.FILE_BROWSER_ROOT,
						ReaderAction.RECENT_BOOKS,
						ReaderAction.CURRENT_BOOK_DIRECTORY,
						ReaderAction.OPDS_CATALOGS,
						ReaderAction.SEARCH,
						ReaderAction.SCAN_DIRECTORY_RECURSIVE,
						ReaderAction.FILE_BROWSER_SORT_ORDER,
						ReaderAction.EXIT
						), false, false, true, true);
				mBrowserToolBar.setBackgroundResource(R.drawable.ui_status_background_browser_dark);
				mBrowserToolBar.setOnActionHandler(new OnActionHandler() {
					@Override
					public boolean onActionSelected(ReaderAction item) {
						switch (item.cmd) {
						case DCMD_EXIT:
							//
							finish();
							break;
						case DCMD_FILE_BROWSER_ROOT:
							showRootWindow();
							break;
						case DCMD_FILE_BROWSER_UP:
							mBrowser.showParentDirectory();
							break;
						case DCMD_OPDS_CATALOGS:
							mBrowser.showOPDSRootDirectory();
							break;
						case DCMD_RECENT_BOOKS_LIST:
							mBrowser.showRecentBooks();
							break;
						case DCMD_SEARCH:
							mBrowser.showFindBookDialog(false, "", null);
							break;
						case DCMD_CURRENT_BOOK:
							showCurrentBook();
							break;
						case DCMD_OPTIONS_DIALOG:
							showBrowserOptionsDialog();
							break;
						case DCMD_SCAN_DIRECTORY_RECURSIVE:
							mBrowser.scanCurrentDirectoryRecursive();
							break;
						case DCMD_FILE_BROWSER_SORT_ORDER:
							mBrowser.showSortOrderMenu();
							break;
						default:
							// do nothing
							break;
						}
						return false;
					}
				});
				mBrowserFrame = new BrowserViewLayout(CoolReader.this, mBrowser, mBrowserToolBar, mBrowserTitleBar);
				//					if (getIntent() == null)
//						mBrowser.showDirectory(Services.getScanner().getDownloadDirectory(), null);
			}
			task.run();
			if (!dontShowBrowser) setCurrentFrame(mBrowserFrame);
		});
		
	}
	
	public void showBrowser() {
		runInBrowser(() -> {
			// do nothing, browser is shown
		}, false);
	}
	
	public void showManual() {
		loadDocument("@manual", "", null, null, false);
	}
	
	public static final String OPEN_FILE_PARAM = "FILE_TO_OPEN";
	public void loadDocument(final String item, final String fileLink, final Runnable doneCallback, final Runnable errorCallback, final boolean forceSync)
	{
		runInReader(() -> mReaderView.loadDocument(item, fileLink, forceSync ? () -> {
			if (null != doneCallback)
				doneCallback.run();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				// Save last opened document on cloud
				if (mSyncGoogleDriveEnabled && mSyncGoogleDriveEnabledCurrentBooks && null != mGoogleDriveSync && !mGoogleDriveSync.isBusy())
					mGoogleDriveSync.startSyncToOnly(false, Synchronizer.SyncTarget.CURRENTBOOKINFO);
			}
		} : doneCallback, errorCallback));
	}

	public void pictureCame(final String item) {
		PictureCameDialog dlg = new PictureCameDialog(CoolReader.this, item, "", "");
		dlg.show();
	}

	public void loadDocumentFromUri(Uri uri, Runnable doneCallback, Runnable errorCallback) {
		runInReader(() -> {
			ContentResolver contentResolver = getContentResolver();
			try {
				InputStream inputStream = contentResolver.openInputStream(uri);
				// Don't save the last opened document from the stream in the cloud, since we still cannot open it later in this program.
				mReaderView.loadDocumentFromStream(inputStream, uri.getPath(), doneCallback, errorCallback);
			} catch (Exception e) {
				errorCallback.run();
			}
		});
	}

	public void loadDocumentFromUriExt(Uri uri, String uriString) {
		loadDocumentFromUri(uri, null, () -> BackgroundThread.instance().postGUI(() -> {
			// if document not loaded show error & then root window
			ErrorDialog errDialog = new ErrorDialog(CoolReader.this, CoolReader.this.getString(R.string.error),
					CoolReader.this.getString(R.string.cant_open_file, uriString));
			errDialog.setOnDismissListener(dialog -> showRootWindow());
			errDialog.show();
		}, 500));
	}

	public void loadDocumentFromStream(final InputStream is, final String path, Runnable doneCallback, Runnable errorCallback) {
		runInReader(() -> {
			try {
				mReaderView.loadDocumentFromStream(is, path, doneCallback, errorCallback);
			} catch (Exception e) {
				if (errorCallback != null) errorCallback.run();
			}
		});
	}

	public void loadDocument(FileInfo item, boolean forceSync) {
		loadDocument(item, null, null, forceSync);
	}

	public void loadDocument(FileInfo item, Runnable doneCallback, Runnable errorCallback, boolean forceSync) {
		log.d("Activities.loadDocument(" + item.pathname + ")");
		loadDocument(item.getPathName(), "", doneCallback, errorCallback, forceSync);
	}

	public void showOpenedBook()
	{
		showReader();
	}
	
	public static final String OPEN_DIR_PARAM = "DIR_TO_OPEN";
	public void showBrowser(final FileInfo dir, String addFilter) {
		String pathname = "";
		if (dir != null) pathname = dir.pathname;
		runInBrowser(() -> mBrowser.showDirectory(dir, null, addFilter), FileInfo.RESCAN_LIBRARY_TAG.equals(pathname));
	}
	
	public void showBrowser(final String dir) {
		runInBrowser(() -> mBrowser.showDirectory(Services.getScanner().pathToFileInfo(dir), null, ""), FileInfo.RESCAN_LIBRARY_TAG.equals(dir));
	}
	
	public void showRecentBooks() {
		log.d("Activities.showRecentBooks() is called");
		runInBrowser(() -> mBrowser.showRecentBooks(), false);
	}

	public void showOnlineCatalogs() {
		log.d("Activities.showOnlineCatalogs() is called");
		runInBrowser(() -> mBrowser.showOPDSRootDirectory(), false);
	}

	public void showDirectory(FileInfo path, String addFilter) {
		log.d("Activities.showDirectory(" + path + ") is called");
		showBrowser(path, addFilter);
	}

	public void showCatalog(final FileInfo path, String addFilter) {
		log.d("Activities.showCatalog(" + path + ") is called");
		runInBrowser(() -> mBrowser.showDirectory(path, null, addFilter), false);
	}

	
	
	public void setBrowserTitle(String title, FileInfo dir) {
		if (mBrowserFrame != null)
			mBrowserFrame.setBrowserTitle(title, dir);
	}
	

	
	// Dictionary support
	
	
	public void findInDictionary(String s, View view) {
		if (s != null && s.length() != 0) {
			int start,end;
			
			// Skip over non-letter characters at the beginning and end of the search string
			for (start = 0 ;start<s.length(); start++)
				if (Character.isLetterOrDigit(s.charAt(start)))
 					break;
			for (end = s.length()-1; end >= start; end--)
				if (Character.isLetterOrDigit(s.charAt(end)))
 					break;

			if (end > start) {
    			final String pattern = s.substring(start,end+1);

				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> findInDictionaryInternal(pattern, view), 100));
			}
		}
	}
	
	private void findInDictionaryInternal(String s, View view) {
		log.d("lookup in dictionary: " + s);
		try {
			mDictionaries.findInDictionary(s, view);
		} catch (DictionaryException e) {
			showToast(e.getMessage());
		}
	}

	public void showDictionary() {
		findInDictionaryInternal(null, null);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		try {
			mDictionaries.onActivityResult(requestCode, resultCode, intent);
		} catch (DictionaryException e) {
			showToast(e.getMessage());
		}
    	if (mDonationService != null) {
    		mDonationService.onActivityResult(requestCode, resultCode, intent);
    	}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (null != mGoogleDriveSync) {
				mGoogleDriveSync.onActivityResultHandler(requestCode, resultCode, intent);
			}
		}

        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE) {
			if (resultCode == Activity.RESULT_OK) {
				if (fileToDelete!=null) {
					File file = fileToDelete.getFile();
					if (file!=null) {
						DocumentFile documentFile = DocumentFile.fromTreeUri(this, intent.getData());
						int res = fileToDelete.deleteFileDocTree(this,intent.getData());
						if (res == -1) {
							showToast("Could not find file, make sure the root of ExtSD was selected");
							return;
						}
						if (res == 0) {
							showToast("Could not delete file");
							return;
						}
						sdCardUri = intent.getData();
						Services.getHistory().removeBookInfo(getDB(), fileToDelete, true, true);
						BackgroundThread.instance().postGUI(() -> directoryUpdated(fileToDelete.parent, null), 700);
					} // if (file!=null)
				}
			}
		} //if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE)
	}
	
	public void setDict( String id ) {
		mDictionaries.setDict(id, this);
	}

	public void setDict2( String id ) {
		mDictionaries.setDict2(id, this);
	}

	public void setDict3( String id ) {
		mDictionaries.setDict3(id, this);
	}

	public void setDict4( String id ) {
		mDictionaries.setDict4(id, this);
	}

	public void setDict5( String id ) {
		mDictionaries.setDict5(id, this);
	}

	public void setDict6( String id ) {
		mDictionaries.setDict6(id, this);
	}

	public void setDict7( String id ) {
		mDictionaries.setDict7(id, this);
	}

	public void setDictWordCorrection (String id) {
		mDictWordCorrrection = false;
		if (id.equals("1"))
			mDictWordCorrrection = true;
	}

    public void setShowUserDicPanel (String id) {
        mShowUserDicPanel = false;
        if (id.equals("1"))
			mShowUserDicPanel = true;
    }

	public void setDictLongtapChange (String id) {
		mDictLongtapChange = false;
		if (id.equals("1"))
			mDictLongtapChange = true;
	}

	public void setToolbarAppearance( String id ) {
		mOptionAppearance = id;
	}

	public String getToolbarAppearance() {
		return mOptionAppearance;
	}

	public void showAboutDialog() {
		AboutDialog dlg = new AboutDialog(this);
		dlg.show();
	}
	
	
    private CRDonationService mDonationService = null;
    private DonationListener mDonationListener = null;
    private double mTotalDonations = 0;
    
    public CRDonationService getDonationService() {
    	return mDonationService;
    }

    public boolean isDonationSupported() {
    	if (mDonationService == null) return false;
    	return mDonationService.isBillingSupported();
    }

    public void setDonationListener(DonationListener listener) {
    	mDonationListener = listener;
    }

    public static interface DonationListener {
    	void onDonationTotalChanged(double total);
    }
    
    public double getTotalDonations() {
    	return mTotalDonations;
    }

    public boolean makeDonation(final double amount) {
		final String itemName = "donation" + (amount >= 1 ? String.valueOf((int)amount) : String.valueOf(amount));
    	log.i("makeDonation is called, itemName=" + itemName);
    	if (!mDonationService.isBillingSupported())
    		return false;
    	BackgroundThread.instance().postBackground(() -> mDonationService.purchase(itemName,
				(success, productId, totalDonations) -> BackgroundThread.instance().postGUI(() -> {
					try {
						if (success) {
							log.i("Donation purchased: " + productId + ", total amount: " + mTotalDonations);
							mTotalDonations += amount;
							SharedPreferences pref = getSharedPreferences(DONATIONS_PREF_FILE, 0);
							pref.edit().putString(DONATIONS_PREF_TOTAL_AMOUNT, String.valueOf(mTotalDonations)).commit();
						} else {
							showToast("Donation purchase failed");
						}
						if (mDonationListener != null)
							mDonationListener.onDonationTotalChanged(mTotalDonations);
					} catch (Exception e) {
						// ignore
					}
				})));
    	return true;
    }
    
	private static String DONATIONS_PREF_FILE = "cr3donations";
	private static String DONATIONS_PREF_TOTAL_AMOUNT = "total";


    // ========================================================================================
    // TTS
	public TTS tts;
	public boolean ttsInitialized;
	public boolean ttsError;
	
	public boolean initTTS(final TTS.OnTTSCreatedListener listener) {
		if (ttsError || !TTS.isFound()) {
			if (!ttsError) {
				ttsError = true;
				showToast("TTS is not available");
			}
			return false;
		}
		if (!phoneStateChangeHandlerInstalled) {
			boolean readPhoneStateIsAvailable;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				readPhoneStateIsAvailable = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
			} else
				readPhoneStateIsAvailable = true;
			if (!readPhoneStateIsAvailable) {
				// assumed Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				requestReadPhoneStatePermissions();
			} else {
				// On Android API less than 23 phone read state permission is granted
				// after application install (permission requested while application installing).
				log.i("read phone state permission already GRANTED, registering phone activity handler...");
				PhoneStateReceiver.setPhoneActivityHandler(() -> {
					if (mReaderView != null) {
						mReaderView.stopTTS();
						mReaderView.save();
					}
				});
				phoneStateChangeHandlerInstalled = true;
			}
		}

		// here we will try to reinitialize
		if (ttsInitialized && tts != null) {
			BackgroundThread.instance().executeGUI(() -> listener.onCreated(tts));
			return true;
		}

		if (ttsInitialized && tts != null) {
			showToast("TTS initialization is already called");
			return false;
		}
		showToast("Initializing TTS");
    	tts = new TTS(this, status -> {
			//tts.shutdown();
			L.i("TTS init status: " + status);
			if (status == TTS.SUCCESS) {
				ttsInitialized = true;
				BackgroundThread.instance().executeGUI(() -> listener.onCreated(tts));
				// will play silence
				if (Build.VERSION.SDK_INT >= 24) {
					MediaPlayer mp = new MediaPlayer();
					try {
						final FileDescriptor fd = getResources().openRawResourceFd(R.raw.silence).getFileDescriptor();
						mp.setDataSource(fd);
						mp.prepareAsync();
						mp.start();
						mp.setOnCompletionListener(mp1 -> {
							L.i("silence completed");
						});

						L.i("silence");
					} catch (IOException e) {
						L.e("silence error: "+e.getMessage());
					}
				}
			} else {
				ttsError = true;
				BackgroundThread.instance().executeGUI(() -> showToast("Cannot initialize TTS"));
			}
		});
		return true;
	}
	

    // ============================================================
	private AudioManager am;
	private int maxVolume;
	public AudioManager getAudioManager() {
		if (am == null) {
			am = (AudioManager)getSystemService(AUDIO_SERVICE);
			maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		}
		return am;
	}
	
	public int getVolume() {
		AudioManager am = getAudioManager();
		if (am!=null) {
			return am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / maxVolume;
		}
		return 0;
	}
	
	public void setVolume( int volume ) {
		AudioManager am = getAudioManager();
		if (am!=null) {
			am.setStreamVolume(AudioManager.STREAM_MUSIC, volume * maxVolume / 100, 0);
		}
	}
	
	public void showOptionsDialog(final OptionsDialog.Mode mode)
	{
		if (mode == OptionsDialog.Mode.BROWSER) {
			optionsFilter = "";
			showOptionsDialogExt(OptionsDialog.Mode.READER, PROP_FILEBROWSER_TITLE);
		} else
			BackgroundThread.instance().postBackground(() -> {
				final String[] mFontFaces = Engine.getFontFaceList();
				final String[] mFontFacesFiles = Engine.getFontFaceAndFileNameList();
				BackgroundThread.instance().executeGUI(() -> {
					OptionsDialog.toastShowCnt++;
					if (OptionsDialog.toastShowCnt < 5) showToast(getString(R.string.settings_info));
					OptionsDialog dlg = new OptionsDialog(CoolReader.this, mReaderView, mFontFaces, mFontFacesFiles, mode);
					dlg.show();
				});
			});
	}

	public void showOptionsDialogTab(final OptionsDialog.Mode mode, int tab)
	{
		if (mode == OptionsDialog.Mode.BROWSER) {
			optionsFilter = "";
			showOptionsDialogExt(OptionsDialog.Mode.READER, PROP_FILEBROWSER_TITLE);
		} else
			BackgroundThread.instance().postBackground(() -> {
				final String[] mFontFaces = Engine.getFontFaceList();
				final String[] mFontFacesFiles = Engine.getFontFaceAndFileNameList();
				BackgroundThread.instance().executeGUI(() -> {
					OptionsDialog dlg = new OptionsDialog(CoolReader.this, mReaderView, mFontFaces, mFontFacesFiles, mode);
					dlg.selectedTab = tab;
					dlg.show();
				});
			});
	}

	public void showOptionsDialogExt(final OptionsDialog.Mode mode, final String selectOption)
	{
		BackgroundThread.instance().postBackground(() -> {
			final String[] mFontFaces = Engine.getFontFaceList();
			final String[] mFontFacesFiles = Engine.getFontFaceAndFileNameList();
			BackgroundThread.instance().executeGUI(() -> {
				if ((!selectOption.equals(PROP_FILEBROWSER_TITLE))&&(selectOption == null)) {
					OptionsDialog.toastShowCnt++;
					if (OptionsDialog.toastShowCnt < 5) showToast(getString(R.string.settings_info));
				}
				OptionsDialog dlg = new OptionsDialog(CoolReader.this, mReaderView, mFontFaces, mFontFacesFiles, mode);
				dlg.selectedOption = selectOption;
				dlg.show();
			});
		});
	}
	
	public void updateCurrentPositionStatus(FileInfo book, Bookmark position, PositionProperties props) {
		mReaderFrame.getStatusBar().updateCurrentPositionStatus(book, position, props);
		mReaderFrame.getUserDicPanel().updateCurrentPositionStatus(book, position, props);
		//showToast("sd "+props.pageNumber);
		//showToast("sdc "+props.pageCount);
		boolean bDontAsk = settings().getBool(Settings.PROP_APP_HIDE_STATE_DIALOGS, false);
		if (
			(!bDontAsk) &&
			((!book.askedMarkRead)&&(book.getReadingState()!=FileInfo.STATE_FINISHED) && (props.pageNumber+1==props.pageCount))
		) {
			book.askedMarkRead = true;
			final FileInfo book1=book;
			askConfirmation(R.string.mark_book_as_read, () -> Services.getHistory().getOrCreateBookInfo(getDB(), book1, bookInfo -> {
				book1.setReadingState(FileInfo.STATE_FINISHED);
				BookInfo bi = new BookInfo(book1);
				getDB().saveBookInfo(bi);
				getDB().flush();
				if (bookInfo.getFileInfo()!=null) {
					bookInfo.getFileInfo().setReadingState(FileInfo.STATE_FINISHED);
					if (bookInfo.getFileInfo().parent!=null)
						directoryUpdated(bookInfo.getFileInfo().parent, bookInfo.getFileInfo());
				}
				BookInfo bi2 = Services.getHistory().getBookInfo(book1);
				if (bi2 != null)
					bi2.getFileInfo().setFileProperties(book1);
			}));
		}
		checkAskReading(book, props,13, false);
	}

	public boolean willBeCheckAskReading(FileInfo book, PositionProperties props, int pagesCnt) {
		if ((!book.askedMarkReading) && (book.getReadingState() != FileInfo.STATE_READING) && (props.pageNumber + 1 < 15)) {
			if (!book.arrReadBeg.contains(props.pageNumber)) book.arrReadBeg.add(props.pageNumber);
			if (book.arrReadBeg.size() > pagesCnt) {
				return true;
			}
		}
		return false;
	}

	public void checkAskReading(FileInfo book, PositionProperties props, int pagesCnt, final boolean openBrowser) {
		if ((!book.askedMarkReading)&&(book.getReadingState()!=FileInfo.STATE_READING) && (props.pageNumber+1<15)) {
			if (!book.arrReadBeg.contains(props.pageNumber)) book.arrReadBeg.add(props.pageNumber);
			boolean bDontAsk = settings().getBool(Settings.PROP_APP_HIDE_STATE_DIALOGS, false);
			if ((book.arrReadBeg.size()>pagesCnt) && (!bDontAsk)) {
				book.askedMarkReading = true;
				final FileInfo book1=book;
				askConfirmation(R.string.mark_book_as_reading, () -> Services.getHistory().getOrCreateBookInfo(getDB(), book1, bookInfo -> {
					book1.setReadingState(FileInfo.STATE_READING);
					BookInfo bi = new BookInfo(book1);
					getDB().saveBookInfo(bi);
					getDB().flush();
					if (bookInfo.getFileInfo()!=null) {
						bookInfo.getFileInfo().setReadingState(FileInfo.STATE_READING);
						if (bookInfo.getFileInfo().parent!=null)
							directoryUpdated(bookInfo.getFileInfo().parent, bookInfo.getFileInfo());
					}
					BookInfo bi2 = Services.getHistory().getBookInfo(book1);
					if (bi2 != null)
						bi2.getFileInfo().setFileProperties(book1);
					if (openBrowser)
						showBrowser(!isBrowserCreated() ? getReaderView().getOpenedFileInfo() : null, "");
				}), () -> {
					if (openBrowser)
						showBrowser(!isBrowserCreated() ? getReaderView().getOpenedFileInfo() : null, "");
				});
			}
		}
	}


		@Override
    protected void setDimmingAlpha(int dimmingAlpha) {
		if (mReaderView != null)
			mReaderView.setDimmingAlpha(dimmingAlpha);
    }

	public void showReaderMenu() {
		//
		if (mReaderFrame != null) {
			mReaderFrame.showMenu();
		}
	}
	
	public void sendBookFragment(BookInfo bookInfo, String text) {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
    	emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, bookInfo.getFileInfo().getAuthors() + " " + bookInfo.getFileInfo().getTitle());
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
		startActivity(Intent.createChooser(emailIntent, null));	
	}

	public void showBookmarksDialog(final boolean bOnlyChoose, final Object obj)
	{
		BackgroundThread.instance().executeGUI(() -> {
			BookmarksDlg dlg = new BookmarksDlg(CoolReader.this, mReaderView, bOnlyChoose, obj);
			dlg.show();
		});
	}
	
	public void openURL(String url) {
		try {
			Intent i = new Intent(Intent.ACTION_VIEW);  
			i.setData(Uri.parse(url));  
			startActivity(i);
		} catch (Exception e) {
			log.e("Exception " + e + " while trying to open URL " + url);
			showToast("Cannot open URL " + url);
		}
	}

	
	
	public boolean isBookOpened() {
		if (mReaderView == null)
			return false;
		return mReaderView.isBookLoaded();
	}

	public void closeBookIfOpened(FileInfo book) {
		if (mReaderView == null)
			return;
		mReaderView.closeIfOpened(book);
	}

	public void askDeleteBook(final FileInfo item)
	{
		askConfirmation(R.string.win_title_confirm_book_delete, () -> {
			closeBookIfOpened(item);
			FileInfo file = Services.getScanner().findFileInTree(item);
			if (file == null)
				file = item;
			final FileInfo finalFile = file;
			 if (file.deleteFile()) {
				waitForCRDBService(() -> {
					Services.getHistory().removeBookInfo(getDB(), finalFile, true, true);
					BackgroundThread.instance().postGUI(() -> directoryUpdated(item.parent, null), 700);
			});
			} else {
				boolean bSucceed = false;
				int res = 0;
				if (sdCardUri!=null) {
					res = file.deleteFileDocTree(CoolReader.this, sdCardUri);
					bSucceed = res == 1;
				}
				if (!bSucceed) {
					fileToDelete = null;
					showToast(R.string.choose_root_sd);
					if (file.getFile() != null) {
						fileToDelete = file;
						Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
						startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);
					}
				} else {
					waitForCRDBService(() -> {
						Services.getHistory().removeBookInfo(getDB(), finalFile, true, true);
						BackgroundThread.instance().postGUI(() -> directoryUpdated(item.parent, null), 700);
					});
				}
			}
		});
	}

	public void DeleteRecursive(File dir, FileInfo fi)
	{
		if (dir.isDirectory())
		{
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++)
			{
				File temp = new File(dir, children[i]);
				if (temp.isDirectory())
				{
					DeleteRecursive(temp, null);
				}
				else
				{
					FileInfo item = new FileInfo(temp);
					closeBookIfOpened(item);
					FileInfo file = Services.getScanner().findFileInTree(item);
					if (file == null)
						file = item;
					if (file.deleteFile()) {
						Services.getHistory().removeBookInfo(getDB(), file, true, true);
					}
//					boolean b = temp.delete();
//					if (b == false)
//					{
//						Log.d("DeleteRecursive", "DELETE FAIL");
//					}
				}
			}

		}
		dir.delete();
		if (fi!=null) {
			if (fi.parent != null)
				directoryUpdated(fi.parent);
			else directoryUpdated(fi);
		}
	}
	
	public void askDeleteRecent(final FileInfo item)
	{
		askConfirmation(R.string.win_title_confirm_history_record_delete, () -> waitForCRDBService(() -> {
			Services.getHistory().removeBookInfo(getDB(), item, true, false);
			directoryUpdated(Services.getScanner().createRecentRoot());
		}));
	}
	
	public void askDeleteCatalog(final FileInfo item)
	{
		askConfirmation(R.string.win_title_confirm_catalog_delete, () -> {
			if (item != null && item.isOPDSDir()) {
				waitForCRDBService(() -> {
					getDB().removeOPDSCatalog(item.id);
					directoryUpdated(Services.getScanner().createOPDSRoot());
				});
			}
		});
	}
	
	public void saveSetting(String name, String value) {
		if (mReaderView != null)
			mReaderView.saveSetting(name, value);
	}
	
	public void editBookInfo(final FileInfo currDirectory, final FileInfo item) {
		waitForCRDBService(() -> Services.getHistory().getOrCreateBookInfo(getDB(), item, bookInfo -> {
			if (bookInfo == null)
				bookInfo = new BookInfo(item);
			BookInfoEditDialog dlg = new BookInfoEditDialog(CoolReader.this, currDirectory, bookInfo,
					currDirectory.isRecentDir());
			dlg.show();
		}));
	}

	public void editBookTransl(final FileInfo currDirectory, final FileInfo item,
							   final String lang_from, final String lang_to, final String search_text,
							   BookInfoEditDialog bied, int iType) {
		waitForCRDBService(() -> Services.getHistory().getOrCreateBookInfo(getDB(), item, bookInfo -> {
			if (bookInfo == null)
				bookInfo = new BookInfo(item);
			ArrayList<String[]> vl = new ArrayList<>();
			String[] arrS1 = {getString(R.string.lang_from), "", lang_from};
			vl.add(arrS1);
			String[] arrS2 =  {getString(R.string.lang_to), "", lang_to};
			vl.add(arrS2);
			BookInfo bookInfoF = bookInfo;
			TranslationDirectionDialog dlgA = new TranslationDirectionDialog(
					CoolReader.this,
					getString(R.string.specify_translation_dir),
					getString(R.string.specify_translation_dir_full),
					iType,
					vl, results -> {
						if (results != null) {
							FileInfo file = bookInfoF.getFileInfo();
							if (results.size() >= 1) {
								if (bied != null) bied.edLangFrom.setText(results.get(0));
								else file.lang_from = results.get(0);
							}
							if (results.size() >= 2) {
								if (bied != null) bied.edLangTo.setText(results.get(1));
								else file.lang_to = results.get(1);
							}
							if (bied == null) {
								getDB().saveBookInfo(bookInfoF);
								getDB().flush();
								if (getReaderView() != null) {
									if (getReaderView().getBookInfo() != null) {
										BookInfo book = getReaderView().getBookInfo();
										book.getFileInfo().lang_from = file.lang_from;
										book.getFileInfo().lang_to = file.lang_to;
									}
								}
								BookInfo bi = Services.getHistory().getBookInfo(file);
								if (bi != null)
									bi.getFileInfo().setFileProperties(file);
								if (currDirectory != null) {
									currDirectory.setFile(file);
									directoryUpdated(currDirectory, file);
								}
								if (!StrUtils.isEmptyStr(search_text))
									findInDictionary(search_text, null);
							}
						}
					});
			dlgA.show();
		}));
	}

	private abstract class Task implements Engine.EngineTask {

		public void done() {
			// override to do something useful
		}

		public void fail(Exception e) {
			// do nothing, just log exception
			// override to do custom action
			log.e("Task " + this.getClass().getSimpleName() + " is failed with exception " + e.getMessage(), e);
		}
	}

	private void execute( Engine.EngineTask task )
	{
		mEngine.execute(task);
	}

	public void showOPDSBookInfo(final FileInfo item, final FileBrowser fb, FileInfo currDir) {
		final ArrayList<BookInfoEntry> itemsAll = new ArrayList<BookInfoEntry>();
		itemsAll.add(new BookInfoEntry("section","section.file","section"));
		String sFormat = "";
		if (item.format != null)
			if (item.format != DocumentFormat.NONE)
				sFormat = item.format.name();
		itemsAll.add(new BookInfoEntry("file.format",sFormat,"text"));
		itemsAll.add(new BookInfoEntry("file.name",item.pathname,"text"));
		itemsAll.add(new BookInfoEntry("section","section.book","section"));
		if (!StrUtils.isEmptyStr(item.authors))
			itemsAll.add(new BookInfoEntry("book.authors", item.getAuthors(),"text"));
		itemsAll.add(new BookInfoEntry("book.title", item.title,"text"));
		if (item.tag != null)
			if (item.tag instanceof OPDSUtil.EntryInfo) {
				OPDSUtil.EntryInfo ei = (OPDSUtil.EntryInfo) item.tag;
				if (ei.categories != null)
					for (String cat:  ei.categories) {
						itemsAll.add(new BookInfoEntry(getString(R.string.category),cat,"text"));
						//li.type
					}
				if (ei.links != null)
					for (OPDSUtil.LinkInfo li:  ei.links) {
						itemsAll.add(new BookInfoEntry((StrUtils.isEmptyStr(li.title)?getString(R.string.link):li.title),
								li.href, "link:"+StrUtils.getNonEmptyStr(li.type,true)));
					}
				if (ei.otherElements != null) {
					for (Map.Entry<String, String> entry : ei.otherElements.entrySet()) {
						String key = (String) entry.getKey();
						final String value = (String) entry.getValue();
						itemsAll.add(new BookInfoEntry(key, value, "text"));
					}
				}
			}
		BookInfoDialog dlg = new BookInfoDialog(this, itemsAll, null, item.getFilename(),
				BookInfoDialog.OPDS_INFO, item, fb, currDir);
		dlg.show();
	}

	public String getReadingTimeLeft(FileInfo fi, boolean defString) {
		String sLeft = getString(R.string.not_enough_stat_data);
		if (!defString) sLeft = "";
		try {
			ReadingStatRes sres = getReaderView().getBookInfo().getFileInfo().calcStats();
			double speedKoef = sres.val;
			int pagesLeft;
			double msecLeft;
			double msecFivePages;
			if (speedKoef > 0.000001) {
				PositionProperties currpos = getReaderView().getDoc().getPositionProps(null, true);
				if (fi.symCount>0) {
					pagesLeft = getReaderView().getDoc().getPageCount() - currpos.pageNumber;
					double msecAllPages;
					msecAllPages = speedKoef * (double) fi.symCount;
					msecFivePages = msecAllPages / ((double) getReaderView().getDoc().getPageCount()) * 5.0;
					msecLeft = (((double) pagesLeft) / 5.0) * msecFivePages;
					sLeft = " ";
					int minutes = (int) ((msecLeft / 1000) / 60);
					int hours = (int) ((msecLeft / 1000) / 60 / 60);
					if (hours>0) {
						minutes = minutes - (hours * 60);
						if (!defString)
							sLeft = sLeft + hours + "h "+minutes+"m";
						else
							sLeft = sLeft + hours + "h "+minutes+"min (calc count: "+ sres.cnt +")";
					} else {
						if (!defString)
							sLeft = sLeft + minutes + "m";
						else
							sLeft = sLeft + minutes + "min (calc count: "+ sres.cnt +")";
					}
				} else {
					if (currpos.pageNumber > 4) {
						int fivePages = StrUtils.getNonEmptyStr(getReaderView().getDoc().getPageText(false, currpos.pageNumber - 1), true).length() +
								StrUtils.getNonEmptyStr(getReaderView().getDoc().getPageText(false, currpos.pageNumber - 2), true).length() +
								StrUtils.getNonEmptyStr(getReaderView().getDoc().getPageText(false, currpos.pageNumber - 3), true).length() +
								StrUtils.getNonEmptyStr(getReaderView().getDoc().getPageText(false, currpos.pageNumber - 4), true).length() +
								StrUtils.getNonEmptyStr(getReaderView().getDoc().getPageText(false, currpos.pageNumber - 5), true).length();
						msecFivePages = speedKoef * (double) fivePages;
						pagesLeft = getReaderView().getDoc().getPageCount() - currpos.pageNumber;
						msecLeft = (((double) pagesLeft) / 5.0) * msecFivePages;
						sLeft = " ";
						int minutes = (int) ((msecLeft / 1000) / 60);
						int hours = (int) ((msecLeft / 1000) / 60 / 60);
						if (hours>0) {
							minutes = minutes - (hours * 60);
							if (!defString)
								sLeft = sLeft + hours + "h "+minutes + "m";
							else
								sLeft = sLeft + hours + "h "+minutes + "min (calc count: "+ sres.cnt +")";
						} else {
							if (!defString)
								sLeft = sLeft + minutes + "m";
							else
								sLeft = sLeft + minutes + "min (calc count: "+ sres.cnt +")";
						}
					}
				}
				;
			}
		} catch (Exception e) {
			log.e("Min left error");
		}
		return sLeft;
	}

	public void showBookInfo(BookInfo setBI, final int actionType, final FileInfo currDir,  final FileInfo origEntry) {
		final ArrayList<BookInfoEntry> itemsAll = new ArrayList<BookInfoEntry>();
		final ArrayList<BookInfoEntry> itemsSys = new ArrayList<BookInfoEntry>();
		final ArrayList<BookInfoEntry> itemsFile = new ArrayList<BookInfoEntry>();
		final ArrayList<BookInfoEntry> itemsPos = new ArrayList<BookInfoEntry>();
		final ArrayList<BookInfoEntry> itemsBook = new ArrayList<BookInfoEntry>();
		itemsSys.add(new BookInfoEntry("section","section.system","section"));
		itemsSys.add(new BookInfoEntry("system.version","KnownReader " + getVersion(),"text"));
		if (getReaderView()!=null)
			itemsSys.add(new BookInfoEntry("system.battery",getReaderView().getBatteryState() + "%","text"));
		itemsSys.add(new BookInfoEntry("system.time",Utils.formatTime(this, System.currentTimeMillis()),"text"));
		if ((getReaderView()!=null)&&(getReaderView().getLastsetWidth()!=0)&&(getReaderView().getLastsetHeight()!=0))
			itemsSys.add(new BookInfoEntry("system.resolution","last requested ("+
							Utils.formatTime(this,getReaderView().getRequestedResTime())+"): "+
							getReaderView().getRequestedWidth()+" x "+getReaderView().getRequestedHeight()+
							"; last set ("+Utils.formatTime(this,getReaderView().getLastsetResTime())+"): "+
							+getReaderView().getLastsetWidth()+" x "+getReaderView().getLastsetHeight(),"text"));
		itemsSys.add(new BookInfoEntry("system.device_model",DeviceInfo.MANUFACTURER + " / "+DeviceInfo.MODEL+" / "+
				DeviceInfo.DEVICE+ " / "+DeviceInfo.PRODUCT + " / " + DeviceInfo.BRAND,"text"));
		String sDevFlags = "";
		if (DeviceInfo.AMOLED_SCREEN) sDevFlags = sDevFlags + ", AMOLED screen";
		if (DeviceInfo.isEinkScreen(false)) sDevFlags = sDevFlags + ", EINK screen";
		if (DeviceInfo.EINK_NOOK) sDevFlags = sDevFlags + ", EINK Nook";
		if (DeviceInfo.EINK_NOOK_120) sDevFlags = sDevFlags + ", EINK Nook 120";
		if (DeviceInfo.EINK_SONY) sDevFlags = sDevFlags + ", EINK Sony";
		if (DeviceInfo.EINK_ONYX) sDevFlags = sDevFlags + ", EINK Onyx";
		if (DeviceInfo.EINK_DNS) sDevFlags = sDevFlags + ", EINK Dns";
		if (DeviceInfo.EINK_TOLINO) sDevFlags = sDevFlags + ", EINK Tolino";
		if (DeviceInfo.ONYX_BUTTONS_LONG_PRESS_NOT_AVAILABLE) sDevFlags = sDevFlags + ", Onyx buttons long press unavailable";
		if (DeviceInfo.POCKETBOOK) sDevFlags = sDevFlags + ", Pocketbook";
		if (DeviceInfo.EINK_SCREEN_UPDATE_MODES_SUPPORTED) sDevFlags = sDevFlags + ", EINK screen update modes supported";
		if (DeviceInfo.NAVIGATE_LEFTRIGHT) sDevFlags = sDevFlags + ", navigate left right";
		if (DeviceInfo.REVERT_LANDSCAPE_VOLUME_KEYS) sDevFlags = sDevFlags + ", revert landscape volume keys";
		if (!DeviceInfo.SCREEN_CAN_CONTROL_BRIGHTNESS) sDevFlags = sDevFlags + ", screen brightness cannot be controlled by swipe";
		if (!sDevFlags.equals("")) sDevFlags=sDevFlags.substring(2);
		itemsSys.add(new BookInfoEntry("system.device_flags",sDevFlags,"text"));

		final BookInfo bi = setBI;
		if (bi != null) {
			FileInfo fi = bi.getFileInfo();
			itemsFile.add(new BookInfoEntry("section","section.file","section"));
			String fname = new File(fi.pathname).getName();
			itemsFile.add(new BookInfoEntry("file.name",fname,"text"));
			if (new File(fi.pathname).getParent() != null) {
				File f = new File(fi.pathname);
				itemsFile.add(new BookInfoEntry("file.path", f==null ? "" : f.getParent(), "text"));
			}
			itemsFile.add(new BookInfoEntry("file.size",StrUtils.readableFileSize(fi.size),"text"));
			if (fi.arcname != null) {
				File f = new File(fi.arcname);
				itemsFile.add(new BookInfoEntry("file.arcname",f==null ? "" : f.getName(),"text"));
				if (new File(fi.arcname).getParent() != null) {
					File f2 = new File(fi.arcname);
					itemsFile.add(new BookInfoEntry("file.arcpath", f2==null ? "" : f2.getParent(),"text"));
				}
				itemsFile.add(new BookInfoEntry("file.arcsize",StrUtils.readableFileSize(fi.arcsize),"text"));
			}
			String sFormat = "";
			if (fi.format != null)
				if (fi.format != DocumentFormat.NONE)
					sFormat = fi.format.name();
			itemsFile.add(new BookInfoEntry("file.format", sFormat,"text"));
			if (!StrUtils.isEmptyStr(fi.opdsLink)) {
				itemsBook.add(new BookInfoEntry("file.opds_link",fi.opdsLink,"text"));
			}
		}
		execute( new Task() {
			Bookmark bm;
			@Override
			public void work() {
				if (getReaderView()!=null)
					if (getReaderView().getDoc()!=null)
						bm =  getReaderView().getDoc().getCurrentPageBookmark();
				if (bm != null) {
					PositionProperties prop = getReaderView().getDoc().getPositionProps(bm.getStartPos(), true);
					itemsPos.add(new BookInfoEntry("section","section.position","section"));
					if (prop.pageMode != 0) {
						itemsPos.add(new BookInfoEntry("position.page","" + (prop.pageNumber+1) + " / " + prop.pageCount,"text"));
					}
					int percent = (int)(10000 * (long)prop.y / prop.fullHeight);
					itemsPos.add(new BookInfoEntry("position.percent","" + (percent/100) + "." + (percent%100) + "%","text"));
					String chapter = bm.getTitleText();
					if (chapter != null && chapter.length() > 100)
						chapter = chapter.substring(0, 100) + "...";
					itemsPos.add(new BookInfoEntry("position.chapter",chapter,"text"));
				}
			}
			public void done() {
				FileInfo fi = bi.getFileInfo();
				itemsBook.add(new BookInfoEntry("section","section.book","section"));
				if (fi.getAuthors() != null || fi.title != null || fi.series != null || fi.publseries != null) {
					if (!StrUtils.isEmptyStr(fi.getAuthors())) {
						itemsBook.add(new BookInfoEntry("book.authors",
								fi.getAuthors().replaceAll("\\|", "; "), "text"));
						String[] list = fi.getAuthors().split("\\|");
						for (String s: list) {
							itemsBook.add(new BookInfoEntry(s, getString(R.string.mi_folder_authors_series), "author_series:"+s));
							itemsBook.add(new BookInfoEntry(s, getString(R.string.mi_folder_authors_books), "author_books:"+s));
						}
					}
					if (!StrUtils.isEmptyStr(fi.series)) {
						itemsBook.add(new BookInfoEntry(fi.series, getString(R.string.mi_folder_series_authors), "series_authors:"+fi.series));
						itemsBook.add(new BookInfoEntry(fi.series, getString(R.string.mi_folder_series_books), "series_books:"+fi.series));
					}
					if (
					    (!StrUtils.isEmptyStr(fi.publseries)) &&
						(!StrUtils.getNonEmptyStr(fi.publseries,true).equals(StrUtils.getNonEmptyStr(fi.series,true)))
					) {
						itemsBook.add(new BookInfoEntry(fi.publseries, getString(R.string.mi_folder_series_authors), "series_authors:"+fi.publseries));
						itemsBook.add(new BookInfoEntry(fi.publseries, getString(R.string.mi_folder_series_books), "series_books:"+fi.publseries));
					}
					if (!StrUtils.isEmptyStr(fi.title)) itemsBook.add(new BookInfoEntry("book.title",fi.title,"text"));
					if (fi.series != null) {
						String s = fi.series;
						if (fi.seriesNumber > 0)
							s = s + " #" + fi.seriesNumber;
						itemsBook.add(new BookInfoEntry("book.series",s,"text"));
					}
				}
				if (!StrUtils.isEmptyStr(fi.getBookdate())) itemsBook.add(new BookInfoEntry("book.date",fi.getBookdate(),"text"));
				if (!StrUtils.isEmptyStr(fi.language)) {
					itemsBook.add(new BookInfoEntry("book.language",fi.language,"text"));
				}
				String genreText = "";
				String genreR = fi.genre_list;
				if (StrUtils.isEmptyStr(genreR)) genreR = fi.genre;
				if (!StrUtils.isEmptyStr(genreR)) {
					// lets try to get out genre name
					GenreSAXElem ge = null;
					String genreDescr = "";
					if (!StrUtils.isEmptyStr(genreR)) {
						String [] arrGenre = genreR.split("\\|");
						for (String genre: arrGenre) {
							if (!StrUtils.isEmptyStr(genre)) {
								String lang = CoolReader.this.getCurrentLanguage();
								if (lang.length() > 2) lang = lang.substring(0, 2);

								GenreSAXElem.mActivity = CoolReader.this;
								try {
									if (GenreSAXElem.elemList.size() == 0)
										GenreSAXElem.initGenreList();
								} catch (Exception e) {
									log.e("exception while init genre list", e);
								}
								genreDescr = "";
								ge = GenreSAXElem.getGenreDescr(lang, genre);
								String[] ge2 = null;
								if (ge != null) {
									if (ge.hshAttrs != null) {
										genreDescr = ge.hshAttrs.get("detailed");
										if (StrUtils.isEmptyStr(genreDescr))
											genreDescr = ge.hshAttrs.get("genre-title");
										if (StrUtils.isEmptyStr(genreDescr))
											genreDescr = ge.hshAttrs.get("title");
									}
								} else {
									ge2 = GenreSAXElem.elemList2.get(genre);
									if (ge2!=null) {
										if (lang.toUpperCase().equals("RU")) genreDescr = ge2[0];
										else genreDescr = ge2[1];
									}
								}
								String addGenreText = "";
								if ((!StrUtils.isEmptyStr(genre)) && (!StrUtils.isEmptyStr(genreDescr)))
									addGenreText = genreDescr+" ("+genre+")";
								if ((!StrUtils.isEmptyStr(genre)) && (StrUtils.isEmptyStr(genreDescr)))
									addGenreText = genre;
								if (StrUtils.isEmptyStr(genreText)) genreText = addGenreText;
								else genreText = genreText + "; "+addGenreText;
							}
						}
					}
					if (!StrUtils.isEmptyStr(genreText))
						itemsBook.add(new BookInfoEntry("book.genre", genreText,"text"));
				}
				String annot = "";
				if (!StrUtils.isEmptyStr(fi.annotation)) {
					annot = fi.annotation;
				}
				if (!StrUtils.isEmptyStr(fi.srclang)) {
					itemsBook.add(new BookInfoEntry("book.srclang", fi.srclang,"text"));
				}
				if (!StrUtils.isEmptyStr(fi.translator)) {
					itemsBook.add(new BookInfoEntry("book.translator", fi.translator,"text"));
				}
				itemsBook.add(new BookInfoEntry("book.symcount", ""+fi.symCount,"text"));
				itemsBook.add(new BookInfoEntry("book.wordcount", ""+fi.wordCount,"text"));
				String sLeft = getReadingTimeLeft(fi,true);
				itemsBook.add(new BookInfoEntry("book.minleft", sLeft,"text"));
				itemsBook.add(new BookInfoEntry("section", "section.book_document","section"));
				if (!StrUtils.isEmptyStr(fi.docauthor)) {
					itemsBook.add(new BookInfoEntry("book.docauthor", fi.docauthor,"text"));
				}
				if (!StrUtils.isEmptyStr(fi.docprogram)) {
					itemsBook.add(new BookInfoEntry("book.docprogram", fi.docprogram,"text"));
				}
				if (!StrUtils.isEmptyStr(fi.getDocdate())) {
					itemsBook.add(new BookInfoEntry("book.docdate", fi.getDocdate(),"text"));
				}
				//new BookInfoEntry("", ,"text"));
				if (!StrUtils.isEmptyStr(fi.docsrcurl)) {
					itemsBook.add(new BookInfoEntry("book.docsrcurl", fi.docsrcurl,"text"));
				}
				if (!StrUtils.isEmptyStr(fi.docsrcocr)) {
					itemsBook.add(new BookInfoEntry("book.docsrcocr", fi.docsrcocr,"text"));
				}
				if (!StrUtils.isEmptyStr(fi.docversion)) {
					itemsBook.add(new BookInfoEntry("book.docversion", fi.docversion,"text"));
				}
				itemsBook.add(new BookInfoEntry("section", "section.book_publisher","section"));
				if (!StrUtils.isEmptyStr(fi.publname)) {
					itemsBook.add(new BookInfoEntry("book.publname", fi.publname,"text"));
				}
				if (!StrUtils.isEmptyStr(fi.publisher)) {
					itemsBook.add(new BookInfoEntry("book.publisher", fi.publisher,"text"));
				}
				if (!StrUtils.isEmptyStr(fi.publcity)) {
					itemsBook.add(new BookInfoEntry("book.publcity", fi.publcity,"text"));
				}
				if (!StrUtils.isEmptyStr(fi.getPublyear())) {
					itemsBook.add(new BookInfoEntry("book.publyear", fi.getPublyear(),"text"));
				}
				if (!StrUtils.isEmptyStr(fi.publisbn)) {
					itemsBook.add(new BookInfoEntry("book.publisbn", fi.publisbn,"text"));
				}
				if (fi.publseries != null) {
					String s = fi.publseries;
					if (fi.publseriesNumber > 0)
						s = s + " #" + fi.publseriesNumber;
					itemsBook.add(new BookInfoEntry("book.publseries", s,"text"));
				}
				itemsBook.add(new BookInfoEntry("section", "section.book_translation","section"));
				String lfrom = "[empty]";
				if (!StrUtils.isEmptyStr(fi.lang_from)) lfrom = fi.lang_from;
				String lto = "[empty]";
				if (!StrUtils.isEmptyStr(fi.lang_to)) lto = fi.lang_to;
				itemsBook.add(new BookInfoEntry("book.translation", lfrom + " -> " + lto,"text"));
				if (itemsPos.size()==1) itemsPos.clear();
				if (itemsBook.size()==1) itemsBook.clear();
				if (itemsFile.size()==1) itemsFile.clear();
				if (itemsSys.size()==1) itemsSys.clear();
				boolean bSection = true;
				for (BookInfoEntry s: itemsPos) {
					if ((bSection)&&(s.infoType.equals("section"))&&(itemsAll.size()>0))
						itemsAll.remove(itemsAll.size()-1);
					bSection=s.infoType.equals("section");
					itemsAll.add(s);
				}
				for (BookInfoEntry s: itemsBook) {
					if ((bSection)&&(s.infoType.equals("section"))&&(itemsAll.size()>0))
						itemsAll.remove(itemsAll.size()-1);
					bSection=s.infoType.equals("section");
					itemsAll.add(s);
				}
				for (BookInfoEntry s: itemsFile) {
					if ((bSection)&&(s.infoType.equals("section"))&&(itemsAll.size()>0))
						itemsAll.remove(itemsAll.size()-1);
					bSection=s.infoType.equals("section");
					itemsAll.add(s);
				}
				for (BookInfoEntry s: itemsSys) {
					if ((bSection)&&(s.infoType.equals("section"))&&(itemsAll.size()>0))
						itemsAll.remove(itemsAll.size()-1);
					bSection=s.infoType.equals("section");
					itemsAll.add(s);
				}
				BookInfoDialog dlg = new BookInfoDialog(CoolReader.this, itemsAll, bi, annot,
						actionType, null, null, currDir);
				//PictureCameDialog dlg = new PictureCameDialog(CoolReader.this, null, "image/jpeg");
				dlg.show();
			}
		});
	}
	
	public void editOPDSCatalog(FileInfo opds) {
		if (opds==null) {
			opds = new FileInfo();
			opds.isDirectory = true;
			opds.pathname = FileInfo.OPDS_DIR_PREFIX + "http://";
			opds.setFilename("New Catalog");
			opds.isListed = true;
			opds.isScanned = true;
			opds.parent = Services.getScanner().getOPDSRoot();
		}
		OPDSCatalogEditDialog dlg = new OPDSCatalogEditDialog(CoolReader.this, opds,
				() -> refreshOPDSRootDirectory(true));
		dlg.show();
	}

	public void refreshOPDSRootDirectory(boolean showInBrowser) {
		if (mBrowser != null)
			mBrowser.refreshOPDSRootDirectory(showInBrowser);
		if (mHomeFrame != null)
			mHomeFrame.refreshOnlineCatalogs(true);
	}

	public void setNeedRefreshOPDS() {
		if (mHomeFrame != null)
			mHomeFrame.needRefreshOnlineCatalogs = true;
	}

    private SharedPreferences mPreferences;
    private final static String BOOK_LOCATION_PREFIX = "@book:";
    private final static String DIRECTORY_LOCATION_PREFIX = "@dir:";
    
    private SharedPreferences getPrefs() {
    	if (mPreferences == null)
    		mPreferences = getSharedPreferences(PREF_FILE, 0);
    	return mPreferences;
    }
	
    public void setLastBook(String path) {
    	setLastLocation(BOOK_LOCATION_PREFIX + path);
    }
    
    public void setLastDirectory(String path) {
    	setLastLocation(DIRECTORY_LOCATION_PREFIX + path);
    }
    
    public void setLastLocationRoot() {
    	setLastLocation(FileInfo.ROOT_DIR_TAG);
    }
    
	/**
	 * Store last location - to resume after program restart.
	 * @param location is file name, directory, or special folder tag
	 */
	public void setLastLocation(String location) {
		try {
			String oldLocation = getPrefs().getString(PREF_LAST_LOCATION, null);
			if (oldLocation != null && oldLocation.equals(location))
				return; // not changed
	        SharedPreferences.Editor editor = getPrefs().edit();
	        editor.putString(PREF_LAST_LOCATION, location);
	        editor.commit();
		} catch (Exception e) {
			// ignore
		}
	}
	
	int CURRENT_NOTIFICATOIN_VERSION = 1;
	public void setLastNotificationId(int notificationId) {
		try {
	        SharedPreferences.Editor editor = getPrefs().edit();
	        editor.putInt(PREF_LAST_NOTIFICATION, notificationId);
	        editor.commit();
		} catch (Exception e) {
			// ignore
		}
	}
	
	public int getLastNotificationId() {
        int res = getPrefs().getInt(PREF_LAST_NOTIFICATION, 0);
        log.i("getLastNotification() = " + res);
        return res;
	}
	
	
	public void showNotifications() {
		int lastNoticeId = getLastNotificationId();
		if (lastNoticeId >= CURRENT_NOTIFICATOIN_VERSION)
			return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.HONEYCOMB)
			if (lastNoticeId <= 1)
				notification1();
		setLastNotificationId(CURRENT_NOTIFICATOIN_VERSION);
	}
	
	public void notification1()
	{
		if (hasHardwareMenuKey())
			return; // don't show notice if hard key present
		setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_SHORT_SIDE), false);
		setSetting(PROP_TOOLBAR_APPEARANCE, String.valueOf(8), true);
		//return; // KnownReader - decided to remove
//		showNotice(R.string.note1_reader_menu, new Runnable() {
//			@Override
//			public void run() {
//				setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_SHORT_SIDE), false);
//			}
//		}, new Runnable() {
//			@Override
//			public void run() {
//				setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_NONE), false);
//			}
//		});
	}

	public void notificationDB(String sMesg)
	{
		showNotice(sMesg, () -> {

		}, null);
	}
	
	/**
	 * Get last stored location.
	 * @return
	 */
	private String getLastLocation() {
        String res = getPrefs().getString(PREF_LAST_LOCATION, null);
        if (res == null) {
    		// import last book value from previous releases 
        	res = getPrefs().getString(PREF_LAST_BOOK, null);
        	if (res != null) {
        		res = BOOK_LOCATION_PREFIX + res;
        		try {
        			getPrefs().edit().remove(PREF_LAST_BOOK).commit();
        		} catch (Exception e) {
        			// ignore
        		}
        	}
        }
        log.i("getLastLocation() = " + res);
        return res;
	}
	
	/**
	 * Open location - book, root view, folder...
	 */
	public void showLastLocation() {
		String location = getLastLocation();
		if (location == null)
			location = FileInfo.ROOT_DIR_TAG;
		if (location.startsWith(BOOK_LOCATION_PREFIX)) {
			location = location.substring(BOOK_LOCATION_PREFIX.length());
			final String floc = location;
			loadDocument(location, "", null, () -> BackgroundThread.instance().postGUI(() -> {
				// if document not loaded show error & then root window
				ErrorDialog errDialog = new ErrorDialog(CoolReader.this, "Error",
					"Can't open file: "+floc);
				errDialog.setOnDismissListener(dialog -> showRootWindow());
				errDialog.show();
			}, 1000), false);
			return;
		}
		if (location.startsWith(DIRECTORY_LOCATION_PREFIX)) {
			location = location.substring(DIRECTORY_LOCATION_PREFIX.length());
			showBrowser(location);
			return;
		}
		if (location.equals(FileInfo.RECENT_DIR_TAG)) {
			showBrowser(location);
			return;
		}
		// TODO: support other locations as well
		showRootWindow();
	}

	public void setExtDataDirCreateTime(Date d) {
		try {
			SharedPreferences.Editor editor = getPrefs().edit();
			editor.putLong(PREF_EXT_DATADIR_CREATETIME, (null != d) ? d.getTime() : 0);
			editor.commit();
		} catch (Exception e) {
			// ignore
		}
	}

	public long getExtDataDirCreateTime() {
		long res = getPrefs().getLong(PREF_EXT_DATADIR_CREATETIME, 0);
		log.i("getExtDataDirCreateTime() = " + res);
		return res;
	}

	public void showCurrentBook() {
		BookInfo bi = Services.getHistory().getLastBook();
		if (bi != null)
			loadDocument(bi.getFileInfo(), false);
	}

	public void readResizeHistory()
	{
        log.d("Reading rh.json");
		String rh = Utils.readFileToString(getSettingsFileF(0).getParent() + "/rh.json");
		try {
			setResizeHist(new ArrayList<>(StrUtils.stringToArray(rh, ResizeHistory[].class)));
		} catch (Exception e) {
		}
	}

	public void saveResizeHistory()
	{
		log.d("Starting save rh.json");
		try {
			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			final String prettyJson = gson.toJson(getResizeHist());
			Utils.saveStringToFileSafe(prettyJson,getSettingsFileF(0).getParent() + "/rh.json");
		} catch (Exception e) {
		}
	}

//	public void readProfileNames()
//	{
//		log.d("Reading profileNames.json");
//		String pn = "";
//		try {
//			final File fJson = new File(getSettingsFile(0).getParent() + "/profileNames.json");
//			BufferedReader reader = new BufferedReader(
//					new FileReader(fJson));
//			StringBuilder stringBuilder = new StringBuilder();
//			String line = null;
//			String ls = System.getProperty("line.separator");
//			while ((line = reader.readLine()) != null) {
//				stringBuilder.append(line);
//				stringBuilder.append(ls);
//			}
//			reader.close();
//			pn = stringBuilder.toString();
//			setProfileNames(new ArrayList<String>(StrUtils.stringToArray(pn, String[].class)));
//		} catch (Exception e) {
//		}
//	}

//	public void saveProfileNames()
//	{
//		log.d("Starting save profileNames.json");
//		try {
//			final File fJson = new File(getSettingsFile(0).getParent() + "/profileNames.json");
//
//			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
//			final String prettyJson = gson.toJson(getProfileNames());
//
//			BufferedWriter bw = null;
//			FileWriter fw = null;
//			char[] bytesArray = new char[1000];
//			int bytesRead = 1000;
//			try {
//				fw = new FileWriter(fJson);
//				bw = new BufferedWriter(fw);
//				bw.write(prettyJson);
//				bw.close();
//				fw.close();
//			} catch (Exception e) {
//			}
//		} catch (Exception e) {
//		}
//	}

	public void saveCurPosFile(boolean is0, String json)
	{
		log.d("Starting save cur_pos.json");
		Utils.saveStringToFileSafe(json,getSettingsFileF(0).getParent() + "/cur_pos" +
				(is0? "0":"")+".json");
	}

	public Bookmark readCurPosFile(boolean is0)
	{
		log.d("Reading cur_pos.json");
		String cur_pos = Utils.readFileToString(getSettingsFileF(0).getParent() + "/cur_pos"+(is0? "0":"")+".json");
		try {
			final File fJson = new File(getSettingsFileF(0).getParent() + "/cur_pos"+(is0? "0":"")+".json");
			if (!fJson.exists()) return null;
			return new Gson().fromJson(cur_pos, Bookmark.class);
		} catch (Exception e) {
		}
		return null;
	}

	public YndCloudSettings yndCloudSettings = new YndCloudSettings();

	public void saveYndCloudSettings(String json)
	{
		log.d("Starting save ynd_cloud_settings.json");
		Utils.saveStringToFileSafe(json,getSettingsFileF(0).getParent() + "/ynd_cloud_settings.json");
	}

	public YndCloudSettings readYndCloudSettings()
	{
		log.d("Reading ynd_cloud_settings.json");
		String s = Utils.readFileToString(getSettingsFileF(0).getParent() + "/ynd_cloud_settings.json");
		try {
			final File fJson = new File(getSettingsFileF(0).getParent() + "/ynd_cloud_settings.json");
			if (!fJson.exists()) return null;
			yndCloudSettings = new Gson().fromJson(s, YndCloudSettings.class);
			return yndCloudSettings;
		} catch (Exception e) {
		}
		return null;
	}

	@TargetApi(26)
	public static final boolean addShortCut(Context context, Intent in_shortcut, Icon icon, String title) {
		boolean res = false;

		try {

			ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

			ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "id" + Long.toString(System.currentTimeMillis()))
					.setShortLabel(title)
					.setLongLabel(title)
					.setIcon(icon)
					.setIntent(in_shortcut)
					.build();
			//ArrayList<ShortcutInfo> scList = new ArrayList<ShortcutInfo>();
			//scList.add(shortcut);

			shortcutManager.requestPinShortcut(shortcut, null);
			res = true;
			//res = shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
		} catch (Exception e) {
			e.printStackTrace();
			res = false;
		}
		return res;
	}

	@TargetApi(23)
	public boolean createBookShortcut(FileInfo item, Bitmap bmp) {

		if (DeviceInfo.getSDKLevel() < 26) {
			this.showToast("Shortcut creation cannot be guaranteed on systems below API26 (Android 8.0)");
		}
		final String sPathName = item.pathname;
		final String sPathName2 = item.isArchive && item.arcname != null
				? new File(item.arcname).getPath() : null;
		String sPathNameF = sPathName2 == null ? sPathName : sPathName2;
		Intent shortcutIntent;

		if (DeviceInfo.getSDKLevel() >= 26) {
			shortcutIntent = new Intent(getApplicationContext(), CoolReader.class);
		} else {
			shortcutIntent = new Intent();
			shortcutIntent.setClassName(this, this.getLocalClassName());
		}
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		shortcutIntent.setAction(Intent.ACTION_VIEW);

		if (DeviceInfo.getSDKLevel() >= 19) {
			shortcutIntent.setData(Uri.parse("coolreader://" + sPathNameF));
		} else {
			shortcutIntent.setData(Uri.parse("file://" + sPathNameF));
		}

		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.title);

		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(getApplicationContext(),
							Utils.resolveResourceIdByAttr(this, R.attr.attr_icons8_book, R.drawable.icons8_book)
					//		R.drawable.cr3_browser_book_hc
					));

		if (DeviceInfo.getSDKLevel() >= 26) {
			Icon icon = Icon.createWithResource(getApplicationContext(),
					Utils.resolveResourceIdByAttr(this, R.attr.attr_icons8_book, R.drawable.icons8_book)
					//R.drawable.cr3_browser_book_hc
			);
			if (bmp!=null) icon = Icon.createWithBitmap(bmp);
			String shTitle = (StrUtils.isEmptyStr(item.title)?item.getFilename().trim():item.title.trim());
			boolean res = addShortCut(this, shortcutIntent, icon, shTitle);
			if (res) this.showToast(R.string.shortcut_created);
			return res;
		} else {
			addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
			this.sendBroadcast(addIntent);
			return true;
		}
	}

    @TargetApi(25) 
    public void createDynShortcuts() {
		try {
			if (DeviceInfo.getSDKLevel() >= 25) {
				ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setPackage(MAIN_CLASS_NAME);
				intent.setType("text/plain");
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, FileInfo.RECENT_DIR_TAG);

				ShortcutInfo shortcut1 = new ShortcutInfo.Builder(this, "id_recent")
						.setShortLabel(this.getString(R.string.mi_book_recent_books))
						.setLongLabel(this.getString(R.string.mi_book_recent_books))
						.setIcon(Icon.createWithResource(getApplicationContext(),
								Utils.resolveResourceIdByAttr(this, R.attr.attr_icons8_book, R.drawable.icons8_book)
								//R.drawable.cr3_browser_book_hc
						))
						.setIntent(intent)
						.build();

				intent = new Intent(Intent.ACTION_SEND);
				intent.setPackage(MAIN_CLASS_NAME);
				intent.setType("text/plain");
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, FileInfo.STATE_READING_TAG);

				ShortcutInfo shortcut2 = new ShortcutInfo.Builder(this, "id_reading")
						.setShortLabel(this.getString(R.string.folder_name_books_by_state_reading))
						.setLongLabel(this.getString(R.string.folder_name_books_by_state_reading))
						.setIcon(Icon.createWithResource(getApplicationContext(),
								Utils.resolveResourceIdByAttr(this, R.attr.attr_icons8_book, R.drawable.icons8_book)
								//R.drawable.cr3_browser_book_hc
						))
						.setIntent(intent)
						.build();

				intent = new Intent(Intent.ACTION_SEND);
				intent.setPackage(MAIN_CLASS_NAME);
				intent.setType("text/plain");
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, FileInfo.STATE_TO_READ_TAG);

				ShortcutInfo shortcut3 = new ShortcutInfo.Builder(this, "id_to_read")
						.setShortLabel(this.getString(R.string.folder_name_books_by_state_to_read))
						.setLongLabel(this.getString(R.string.folder_name_books_by_state_to_read))
						.setIcon(Icon.createWithResource(getApplicationContext(),
								//R.drawable.cr3_browser_book_hc
								Utils.resolveResourceIdByAttr(this, R.attr.attr_icons8_book, R.drawable.icons8_book)
						))
						.setIntent(intent)
						.build();

				intent = new Intent(Intent.ACTION_SEND);
				intent.setPackage(MAIN_CLASS_NAME);
				intent.setType("text/plain");
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, FileInfo.STATE_FINISHED_TAG);

				ShortcutInfo shortcut4 = new ShortcutInfo.Builder(this, "id_finished")
						.setShortLabel(this.getString(R.string.folder_name_books_by_state_finished))
						.setLongLabel(this.getString(R.string.folder_name_books_by_state_finished))
						.setIcon(Icon.createWithResource(getApplicationContext(),
								Utils.resolveResourceIdByAttr(this, R.attr.attr_icons8_book, R.drawable.icons8_book)
						//		R.drawable.cr3_browser_book_hc
						))
						.setIntent(intent)
						.build();

				intent = new Intent(Intent.ACTION_SEND);
				intent.setPackage(MAIN_CLASS_NAME);
				intent.setType("text/plain");
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, FileInfo.SEARCH_SHORTCUT_TAG);

				ShortcutInfo shortcut5 = new ShortcutInfo.Builder(this, "id_search")
						.setShortLabel(this.getString(R.string.dlg_book_search))
						.setLongLabel(this.getString(R.string.dlg_book_search))
						.setIcon(Icon.createWithResource(getApplicationContext(),
								//R.drawable.cr3_browser_find_hc
								Utils.resolveResourceIdByAttr(this, R.attr.cr3_viewer_find_drawable, R.drawable.icons8_search)
						))
						.setIntent(intent)
						.build();

				shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut1, shortcut2, shortcut3,
						/*shortcut4,*/ shortcut5));
			}
		} catch (Exception e) {

		}
	}

	public void createGeoListener() {

		geoLastData.gps = new ProviderLocationTracker(this,
				ProviderLocationTracker.ProviderType.GPS);
		geoLastData.geoListener = new LocationTracker.LocationUpdateListener() {
			@Override
			public void onUpdate(Location oldLoc, long oldTime, Location newLoc,
								 long newTime) {
				geoLastData.geoUpdateCoords(oldLoc, oldTime, newLoc, newTime);
			}

		};

		geoLastData.netw = new ProviderLocationTracker(this,
				ProviderLocationTracker.ProviderType.NETWORK);

		geoLastData.netwListener = new LocationTracker.LocationUpdateListener() {
			@Override
			public void onUpdate(Location oldLoc, long oldTime, Location newLoc,
								 long newTime) {
				geoLastData.geoUpdateCoords(oldLoc, oldTime, newLoc, newTime);
			}

		};
		int iGeo = settings().getInt(Settings.PROP_APP_GEO, 0);
		if ((iGeo==2)||(iGeo==4)) geoLastData.loadMetroStations(this);
		if ((iGeo==3)||(iGeo==4)) geoLastData.loadTransportStops(this);
		if (iGeo>1) {
			geoLastData.gps.start(geoLastData.geoListener);
			geoLastData.netw.start(geoLastData.netwListener);
		}
	}

	public boolean checkLocationPermission() {
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.ACCESS_FINE_LOCATION)) {
				new AlertDialog.Builder(this)
						.setTitle(R.string.title_location_permission)
						.setMessage(R.string.text_location_permission)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						ActivityCompat.requestPermissions(CoolReader.this,
								new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
								REQUEST_CODE_LOCATION_PERMISSION);
					}
				}).create().show();
			} else {
				ActivityCompat.requestPermissions(CoolReader.this,
						new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
						REQUEST_CODE_LOCATION_PERMISSION);
			}
			return false;
		} else {
			return true;
		}
	}

}

