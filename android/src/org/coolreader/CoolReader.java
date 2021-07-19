// Main Class
package org.coolreader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.CloudFileInfo;
import org.coolreader.cloud.CloudSync;
import org.coolreader.cloud.deepl.DeeplCloudSettings;
import org.coolreader.cloud.lingvo.LingvoCloudSettings;
import org.coolreader.cloud.litres.LitresCloudSettings;
import org.coolreader.cloud.litres.LitresCredentialsDialog;
import org.coolreader.cloud.yandex.YndCloudSettings;
import org.coolreader.crengine.AskSomeValuesDialog;
import org.coolreader.crengine.BookInfoEntry;
import org.coolreader.crengine.CalibreCatalogEditDialog;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.InputDialog;
import org.coolreader.crengine.OPDSUtil;
import org.coolreader.crengine.ReadingStatRes;
import org.coolreader.crengine.SomeButtonsToolbarDlg;
import org.coolreader.dic.Dictionaries;
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
import org.coolreader.crengine.FileInfoOperationListener;
import org.coolreader.crengine.GenreSAXElem;
import org.coolreader.crengine.InterfaceTheme;
import org.coolreader.crengine.L;
import org.coolreader.crengine.LogcatSaver;
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
import org.coolreader.eink.sony.android.ebookdownloader.SonyBookSelector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
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
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.speech.tts.TextToSpeech;
import org.coolreader.tts.OnTTSCreatedListener;
import org.coolreader.tts.TTSToolbarDlg;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.provider.Settings.Secure;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;

public class CoolReader extends BaseActivity implements SensorEventListener
{
	public static final Logger log = L.create("cr");

	public Uri sdCardUri = null;

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

    //move to flavor
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

	private ArrayList<ResizeHistory> resizeHist = new ArrayList<>();

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

	public ReaderView getmReaderView() {
		return mReaderView;
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
	public Long mLastDialogClosed = 0L;

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

	private boolean mSyncGoogleDriveEnabled = false;
	private boolean mSyncGoogleDriveEnabledPrev = false;
	private boolean mCloudSyncAskConfirmations = true;
	private boolean mSyncGoogleDriveEnabledSettings = false;
	private boolean mSyncGoogleDriveEnabledBookmarks = false;
	private boolean mSyncGoogleDriveEnabledCurrentBookInfo = false;
	private boolean mSyncGoogleDriveEnabledCurrentBookBody = false;
	private int mCloudSyncBookmarksKeepAlive = 14;
	private int mSyncGoogleDriveAutoSavePeriod = 0;
	private int mSyncGoogleDriveErrorsCount = 0;
	private Synchronizer mGoogleDriveSync;
	private Timer mGoogleDriveAutoSaveTimer = null;
	// can be add more synchronizers
	private boolean mSuppressSettingsCopyToCloud;

	public boolean mAppUseEinkFrontlight = false;

	private String mOptionAppearance = "0";

	private String fileToLoadOnStart = null;
	private String mFileToOpenFromExt = null;

	private int mOpenDocumentTreeCommand = ODT_CMD_NO_SPEC;
	private FileInfo mOpenDocumentTreeArg = null;

	private boolean isFirstStart = true;
	private int settingsCanBeMigratedLastInd = -1;
	public int settingsMayBeMigratedLastInd = -1;
	private int reserveSettingsLastInd = -1;
	private int currentSettingsLastInd = -1;
	private boolean phoneStateChangeHandlerInstalled = false;
	private int initialBatteryState = -1;
	private BroadcastReceiver intentReceiver;

	private boolean justCreated = false;
	private boolean activityIsRunning = false;

	private boolean dataDirIsRemoved = false;

	private static final int REQUEST_CODE_STORAGE_PERM = 1;
	private static final int REQUEST_CODE_READ_PHONE_STATE_PERM = 2;
	private static final int REQUEST_CODE_GOOGLE_DRIVE_SIGN_IN = 3;
	private static final int REQUEST_CODE_OPEN_DOCUMENT_TREE = 11;
	public static final int REQUEST_CODE_CHOOSE_DIR = 10012;

	public CalibreCatalogEditDialog cced = null; // for callback

	// open document tree activity commands
	private static final int ODT_CMD_NO_SPEC = -1;
	private static final int ODT_CMD_DEL_FILE = 1;
	private static final int ODT_CMD_DEL_FOLDER = 2;
	private static final int ODT_CMD_SAVE_LOGCAT = 3;

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

		isFirstStart = true;
		justCreated = true;
		activityIsRunning = false;

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

		mEngine = Engine.getInstance(this);

		//requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
		//==========================================
    	// Battery state listener
		intentReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				// Are we charging / charged?
				int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
						status == BatteryManager.BATTERY_STATUS_FULL;

				// How are we charging?
				int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
				boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
				boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
				boolean wirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;

				int level = intent.getIntExtra("level", 0);

				if (usbCharge) level += 10000; else
					if (acCharge) level += 20000; else
						if (wirelessCharge) level += 30000;

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
		// working with ssl - needed for nook
//		try {
//			ProviderInstaller.installIfNeeded(getApplicationContext());
//			SSLContext sslContext;
//			sslContext = SSLContext.getInstance("TLSv1.2");
//			sslContext.init(null, null, null);
//			sslContext.createSSLEngine();
//		} catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException
//				| NoSuchAlgorithmException | KeyManagementException e) {
//			log.e("CoolReader.onCreate() sslContext error: " + e.getMessage());
//		}
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
			if (mBrowser != null)
				mBrowser.setCoverPageSizeOption(Utils.parseInt(value, 0, 0, 2));
        } else if (key.equals(PROP_APP_FILE_BROWSER_SIMPLE_MODE)) {
        	if (mBrowser != null)
        		mBrowser.setSimpleViewMode(flg);
        } else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_ENABLED)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveEnabledPrev = mSyncGoogleDriveEnabled;
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
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_INFO)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveEnabledCurrentBookInfo = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_BODY)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveEnabledCurrentBookBody = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_AUTOSAVEPERIOD)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveAutoSavePeriod = Utils.parseInt(value, 0, 0, 30);
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_DATA_KEEPALIVE)) {
			mCloudSyncBookmarksKeepAlive = Utils.parseInt(value, 14, 0, 365);
			updateGoogleDriveSynchronizer();
		} else if (key.equals(PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS)) {
			// already in super method:
			// Services.getScanner().setHideEmptyDirs(flg);
			// Here only refresh the file browser
			if (null != mBrowser) {
				mBrowser.showLastDirectory();
			}
		} else if (key.equals(PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES)) {
			if (null != mBrowser) {
				mBrowser.setHideEmptyGenres(flg);
			}
		} else if (key.equals(PROP_APP_USE_EINK_FRONTLIGHT)) { //KR
			mAppUseEinkFrontlight = StrUtils.getNonEmptyStr(value,true).equals("1");
		} else if (key.equals(PROP_APP_TTS_ENGINE)) {
			ttsEnginePackage = value;
			if (null != mReaderView && mReaderView.isTTSActive() && null != tts) {
				// Stop current TTS process & create new
				mReaderView.stopTTS();
				if (tts != null) {
					// Cleanup previous TTS
					tts.shutdown();
					tts = null;
					ttsInitialized = false;
					ttsError = false;
				}
				initTTS(tts -> {
					TTSToolbarDlg dlg = mReaderView.getTTSToolbar();
					dlg.changeTTS(tts);
				});
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
				public void onSyncStarted(Synchronizer.SyncDirection direction, boolean showProgress, boolean interactively) {
					if (Synchronizer.SyncDirection.SyncFrom == direction) {
						log.d("Starting synchronization from Google Drive");
					} else if (Synchronizer.SyncDirection.SyncTo == direction) {
						log.d("Starting synchronization to Google Drive");
					}
					if (null != mReaderView) {
						if (showProgress) {
							mReaderView.showCloudSyncProgress(100);
						}
					}
				}

				@Override
				public void OnSyncProgress(Synchronizer.SyncDirection direction, boolean showProgress, int current, int total, boolean interactively) {
					log.v("sync progress: current=" + current + "; total=" + total);
					if (null != mReaderView) {
						if (showProgress) {
							int total_ = total;
							if (current > total_)
								total_ = current;
							mReaderView.showCloudSyncProgress(10000 * current / total_);
						}
					}
				}

				@Override
				public void onSyncCompleted(Synchronizer.SyncDirection direction, boolean showProgress, boolean interactively) {
					if (Synchronizer.SyncDirection.SyncFrom == direction) {
						log.d("Google Drive SyncFrom successfully completed");
					} else if (Synchronizer.SyncDirection.SyncTo == direction) {
						log.d("Google Drive SyncTo successfully completed");
					}
					if (interactively)
						showToast(R.string.googledrive_sync_completed);
					if (showProgress) {
						if (null != mReaderView) {
							// Hide sync indicator
							mReaderView.hideCloudSyncProgress();
						}
					}
					if (mSyncGoogleDriveEnabled)
						mSyncGoogleDriveErrorsCount = 0;
				}

				@Override
				public void onSyncError(Synchronizer.SyncDirection direction, String errorString) {
					// Hide sync indicator
					if (null != mReaderView) {
						mReaderView.hideCloudSyncProgress();
					}
					if (null != errorString)
						showToast(R.string.googledrive_sync_failed_with, errorString);
					else
						showToast(R.string.googledrive_sync_failed);
					if (mSyncGoogleDriveEnabled) {
						mSyncGoogleDriveErrorsCount++;
						if (mSyncGoogleDriveErrorsCount >= 3) {
							showToast(R.string.googledrive_sync_failed_disabled);
							log.e("More than 3 sync failures in a row, auto sync disabled.");
							mSyncGoogleDriveEnabled = false;
						}
					}
				}

				@Override
				public void onAborted(Synchronizer.SyncDirection direction) {
					// Hide sync indicator
					if (null != mReaderView) {
						mReaderView.hideCloudSyncProgress();
					}
					showToast(R.string.googledrive_sync_aborted);
				}

				@Override
				public void onSettingsLoaded(Properties settings, boolean interactively) {
					// Apply downloaded (filtered) settings
					mSuppressSettingsCopyToCloud = true;
					mergeSettings(settings, true);
				}

				@Override
				public void onBookmarksLoaded(BookInfo bookInfo, boolean interactively) {
					waitForCRDBService(() -> {
						// TODO: ask the user whether to import new bookmarks.
						BookInfo currentBook = null;
						int currentPos = -1;
						if (null != mReaderView) {
							currentBook = mReaderView.getBookInfo();
							if (null != currentBook)
								currentPos = currentBook.getLastPosition().getPercent();
						}
						Services.getHistory().updateBookInfo(bookInfo);
						getDB().saveBookInfo(bookInfo);
						if (null != currentBook) {
							FileInfo currentFileInfo = currentBook.getFileInfo();
							if (null != currentFileInfo) {
								if (currentFileInfo.baseEquals((bookInfo.getFileInfo()))) {
									// if the book indicated by the bookInfo is currently open.
									Bookmark lastPos = bookInfo.getLastPosition();
									if (null != lastPos) {
										if (!interactively) {
											mReaderView.goToBookmark(lastPos);
										} else {
											if (Math.abs(currentPos - lastPos.getPercent()) > 10) {		// 0.1%
												askQuestion(R.string.cloud_synchronization_from_, R.string.sync_confirmation_new_reading_position,
														() -> mReaderView.goToBookmark(lastPos), null);
											}
										}
									}
								}
							}
						}
					});
				}

				@Override
				public void onCurrentBookInfoLoaded(FileInfo fileInfo, boolean interactively) {
					FileInfo current = null;
					if (null != mReaderView) {
						BookInfo bookInfo = mReaderView.getBookInfo();
						if (null != bookInfo)
							current = bookInfo.getFileInfo();
					}
					if (!fileInfo.baseEquals(current)) {
						if (!interactively) {
							loadDocument(fileInfo, false);
						} else {
							String shortBookInfo = "";
							if (null != fileInfo.authors && !fileInfo.authors.isEmpty())
								shortBookInfo = "\"" + fileInfo.authors + ", ";
							else
								shortBookInfo = "\"";
							shortBookInfo += fileInfo.title + "\"";
							String question = getString(R.string.sync_confirmation_other_book, shortBookInfo);
							askQuestion(getString(R.string.cloud_synchronization_from_), question, () -> loadDocument(fileInfo, false), null);
						}
					}
				}

				@Override
				public void onFileNotFound(FileInfo fileInfo) {
					if (null == fileInfo)
						return;
					String docInfo = "Unknown";
					if (null != fileInfo.title && !fileInfo.authors.isEmpty())
						docInfo = fileInfo.title;
					if (null != fileInfo.authors && !fileInfo.authors.isEmpty())
						docInfo = fileInfo.authors + ", " + docInfo;
					if (null != fileInfo.getFilename() && !fileInfo.getFilename().isEmpty())
						docInfo += " (" + fileInfo.getFilename() + ")";
					showToast(R.string.sync_info_no_such_document, docInfo);
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
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.CURRENTBOOKINFO, mSyncGoogleDriveEnabledCurrentBookInfo);
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.CURRENTBOOKBODY, mSyncGoogleDriveEnabledCurrentBookBody);
				mGoogleDriveSync.setBookmarksKeepAlive(mCloudSyncBookmarksKeepAlive);
				if (null != mGoogleDriveAutoSaveTimer) {
					mGoogleDriveAutoSaveTimer.cancel();
					mGoogleDriveAutoSaveTimer = null;
				}
				if (mSyncGoogleDriveAutoSavePeriod > 0) {
					mGoogleDriveAutoSaveTimer = new Timer();
					mGoogleDriveAutoSaveTimer.schedule(new TimerTask() {
						@Override
						public void run() {
							if (activityIsRunning && null != mGoogleDriveSync) {
								mGoogleDriveSync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS);
							}
						}
					}, mSyncGoogleDriveAutoSavePeriod * 60000, mSyncGoogleDriveAutoSavePeriod * 60000);
				}
			} else {
				if (null != mGoogleDriveAutoSaveTimer) {
					mGoogleDriveAutoSaveTimer.cancel();
					mGoogleDriveAutoSaveTimer = null;
				}
				if (mSyncGoogleDriveEnabledPrev && null != mGoogleDriveSync) {
					log.d("Google Drive autosync is disabled.");
					if (false) {
						// TODO: Don't remove authorization on Google Account here, move this into OptionsDialog
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
	}

	public void forceSyncToGoogleDrive() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (null == mGoogleDriveSync)
				buildGoogleDriveSynchronizer();
			mGoogleDriveSync.setBookmarksKeepAlive(mCloudSyncBookmarksKeepAlive);
			mGoogleDriveSync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_FORCE | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | Synchronizer.SYNC_FLAG_ASK_CHANGED);
		}
	}

	public void forceSyncFromGoogleDrive() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (null == mGoogleDriveSync)
				buildGoogleDriveSynchronizer();
			mGoogleDriveSync.setBookmarksKeepAlive(mCloudSyncBookmarksKeepAlive);
			mGoogleDriveSync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_FORCE | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | Synchronizer.SYNC_FLAG_ASK_CHANGED);
		}
	}

	private BookInfo getCurrentBookInfo() {
		BookInfo bookInfo = null;
		if (mReaderView != null) {
			bookInfo = mReaderView.getBookInfo();
			if (null != bookInfo && null == bookInfo.getFileInfo()) {
				// nullify if fileInfo is null
				bookInfo = null;
			}
		}
		return bookInfo;
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

	public void processIntentContent(String stype, Object obj) {
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
			if ((sUri.startsWith("http")) && (StrUtils.isEmptyStr(fileToOpen))) {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					showToast(R.string.only_in_premium);
					return true;
				}
				processIntentContent("", sUri);
				return true;
			}
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
				if (!FlavourConstants.PREMIUM_FEATURES) {
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
			// TODO: calculate fingerprint for uri and find fileInfo in DB
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
		activityIsRunning = false;
		mSensorManager.unregisterListener(deviceOrientation.getEventListener());
        mSensorManager.unregisterListener(this);
		geoLastData.gpsStop(); geoLastData.netwStop();
		if (mReaderView != null) {
			mReaderView.onAppPause();
		}
		if (mBrowser != null) {
			mBrowser.stopCurrentScan();
		}
		Services.getCoverpageManager().removeCoverpageReadyListener(mHomeFrame);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mSyncGoogleDriveEnabled && mGoogleDriveSync != null && !mGoogleDriveSync.isBusy()) {
				mGoogleDriveSync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS);
			}
		}
		super.onPause();
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
			geoLastData.gpsStart();
			geoLastData.netwStart();
		}

        if(mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size()!=0){
            Sensor s = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            mSensorManager.registerListener(this,s, SensorManager.SENSOR_DELAY_NORMAL);
        }

		//Properties props = SettingsManager.instance(this).get();
		
		if (mReaderView != null) {
			mReaderView.onAppResume();
//			if (mCurrentFrame == mReaderFrame)
//				setCutoutMode(this.iCutoutMode);
		}
		
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
			if (mSyncGoogleDriveEnabled && mGoogleDriveSync != null) {
				// when the program starts, the local settings file is already updated, so the local file is always newer than the remote one
				// Therefore, the synchronization mode is quiet, i.e. without comparing modification times and without prompting the user for action.
				// If the file is opened from an external file manager, we must disable the "currently reading book" sync operation with google drive.
				if (!mGoogleDriveSync.isBusy()) {
					if (null == mFileToOpenFromExt)
						mGoogleDriveSync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mCloudSyncAskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0) );
					else
						mGoogleDriveSync.startSyncFromOnly(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mCloudSyncAskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0), Synchronizer.SyncTarget.SETTINGS, Synchronizer.SyncTarget.BOOKMARKS);
				} else {
					log.d("Synchronizer is busy!");
				}
			}
		}
		activityIsRunning = true;

		if (getReaderView()!=null) {
			if (getReaderView().ttsToolbar != null)
				getReaderView().ttsToolbar.repaintButtons();
			BackgroundThread.instance().postGUI(() -> {
				log.i("Load last rpos from CLOUD");
				int iSyncVariant3 = settings().getInt(PROP_CLOUD_SYNC_VARIANT, 0);
				if (iSyncVariant3 != 0) {
					if (mCurrentFrame == mReaderFrame)
						CloudSync.loadFromJsonInfoFileList(CoolReader.this,
								CloudSync.CLOUD_SAVE_READING_POS, true, iSyncVariant3 == 1, CloudAction.FINDING_LAST_POS, true);
				}
			}, 5000);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		log.i("CoolReader.onSaveInstanceState()");
		super.onSaveInstanceState(outState);
	}

	static final boolean LOAD_LAST_DOCUMENT_ON_START = true;

	private void checkIfWeCanMigrateSettings() {
		int i=0;
		// move files to safe place if exists in old
		String fpath1 = getSettingsFileF(0).getParent() + "/ynd_cloud_settings.json";
		String fpath2 = getSettingsFileF(0).getParent() + "/dbx.token";
		String fpath3 = getSettingsFileF(0).getParent() + "/ynd.token";
		String nfpath1 = getSettingsFileExt("[DEFAULT]",0).getParent() + "/ynd_cloud_settings.json";
		String nfpath2 = getSettingsFileExt("[DEFAULT]",0).getParent() + "/dbx.token";
		String nfpath3 = getSettingsFileExt("[DEFAULT]",0).getParent() + "/ynd.token";
		File f1 = new File(fpath1); File nf1 = new File(nfpath1);
		if ((f1.exists()) && (!nf1.exists()) && (!fpath1.equals(nfpath1)))
			if (f1.isFile()) {
				if (Utils.copyFile(f1, nf1)) f1.delete();
			}
		File f2 = new File(fpath2); File nf2 = new File(nfpath2);
		if ((f2.exists()) && (!nf2.exists()) && (!fpath2.equals(nfpath2)))
			if (f2.isFile()) {
				if (Utils.copyFile(f2, nf2)) f2.delete();
			}
		File f3 = new File(fpath3); File nf3 = new File(nfpath3);
		if ((f3.exists()) && (!nf3.exists()) && (!fpath3.equals(nfpath3)))
			if (f3.isFile()) {
				if (Utils.copyFile(f3, nf3)) f3.delete();
			}
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
			if (getSettingsFileExtExists(".cr3", 0))
				if (getSettingsFileExt(".cr3", 0).isFile()) {
					settingsMayBeMigratedLastInd = 0;
					i++;
					more = true;
					while (more) {
						if (!(getSettingsFileExtExists(".cr3", i)))
							more = false;
						else
							settingsMayBeMigratedLastInd = i;
						i++;
					}
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
			waitForCRDBService(() -> getDB().loadUserDic(list -> mUserDic = list));
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
							() -> {
								ArrayList<CloudFileInfo> afi = new ArrayList<>();
								for (int i = 0; i <= settingsMayBeMigratedLastInd; i++) {
									if (getSettingsFileExtExists(".cr3", i))
										if (getSettingsFileExt(".cr3", i).isFile()) {
											CloudFileInfo cfi = new CloudFileInfo();
											cfi.name = getSettingsFileExt(".cr3", i).getName();
											cfi.path = getSettingsFileExt(".cr3", i).getPath();
											afi.add(cfi);
										}
								}
								CloudSync.restoreSettingsFiles(this, null, afi, false);
							},
							() -> {});
				}
			}, null);
			// this could cause to show book not in correct dimenions - till it was resized (by sensor etc.) - so I replaced by notice
			//ErrorDialog dlg = new ErrorDialog(this, getString(R.string.error), getString(R.string.datadir_is_removed, Engine.getExternalSettingsDirName()));
			//dlg.show();
		} else {
			if (settingsCanBeMigratedLastInd >= 0) {
				showNotice(R.string.note1_can_be_migrated,
						() -> {
							ArrayList<CloudFileInfo> afi = new ArrayList<>();
							for (int i = 0; i <= settingsMayBeMigratedLastInd; i++) {
								if (getSettingsFileExtExists(".cr3", i))
									if (getSettingsFileExt(".cr3", i).isFile()) {
										CloudFileInfo cfi = new CloudFileInfo();
										cfi.name = getSettingsFileExt(".cr3", i).getName();
										cfi.path = getSettingsFileExt(".cr3", i).getPath();
										afi.add(cfi);
									}
							}
							CloudSync.restoreSettingsFiles(this, null, afi, false);
						},
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
        // after reading of all settings (only once) - we'll check should we set backlight or not
//		if (initialBacklight != -1) {
//				BackgroundThread.instance().postGUI(() -> BackgroundThread.instance()
//					.postBackground(() -> BackgroundThread.instance()
//							.postGUI(() -> {
//								setScreenBacklightLevel(initialBacklight);
//								initialBacklight = -1;
//							})), 100);
//		}
//		if (initialWarmBacklight != -1)
//				BackgroundThread.instance().postGUI(() -> BackgroundThread.instance()
//						.postBackground(() -> BackgroundThread.instance()
//								.postGUI(() -> {
//									setScreenWarmBacklightLevel(initialWarmBacklight);
//									initialWarmBacklight = -1;
//								})), 200);
		BOOK_READING_STATE_NO_STATE = "["+getString(R.string.book_state_none)+"]";
		BOOK_READING_STATE_TO_READ = "["+getString(R.string.book_state_toread)+"]";
		BOOK_READING_STATE_READING = "["+getString(R.string.book_state_reading)+"]";
		BOOK_READING_STATE_FINISHED = "["+getString(R.string.book_state_finished)+"]";
		READ_ALOUD = getString(R.string.read_aloud);

		// Show/Hide soft navbar after OptionDialog is closed.
		applyFullscreen(getWindow());
		validateSettings();
		if (!justCreated) {
			// Only after onStart()!
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				if (mSyncGoogleDriveEnabled && !mSyncGoogleDriveEnabledPrev && null != mGoogleDriveSync) {
					// if cloud sync has just been enabled in options dialog
					mGoogleDriveSync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mCloudSyncAskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0) );
					mSyncGoogleDriveEnabledPrev = mSyncGoogleDriveEnabled;
					return;
				}
				if (changedProps.size() > 0) {
					// After options dialog is closed, sync new settings to the cloud with delay
					BackgroundThread.instance().postGUI(() -> {
						if (mSyncGoogleDriveEnabled && mSyncGoogleDriveEnabledSettings && null != mGoogleDriveSync) {
							if (mSuppressSettingsCopyToCloud) {
								// Immediately after downloading settings from Google Drive
								// prevent uploading settings file
								mSuppressSettingsCopyToCloud = false;
							} else if (!mGoogleDriveSync.isBusy()) {
								// After setting changed in OptionsDialog
								log.d("Some settings is changed, uploading to cloud...");
								mGoogleDriveSync.startSyncToOnly(null, Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS, Synchronizer.SyncTarget.SETTINGS);
							}
						}
					}, 500);
				}
			}
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
			} else {
				if (null != mBrowser)
					mBrowser.stopCurrentScan();
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
		if (null != mBrowser)
			mBrowser.stopCurrentScan();
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
		if (activityIsRunning) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				// Save bookmarks and current reading position on the cloud
				if (mSyncGoogleDriveEnabled && null != mGoogleDriveSync && !mGoogleDriveSync.isBusy()) {
					mGoogleDriveSync.startSyncToOnly(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY, Synchronizer.SyncTarget.BOOKMARKS);
				}
			}
		}
	}
	
	private void runInReader(final Runnable task) {
		if (null != mBrowser)
			mBrowser.stopCurrentScan();
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
				mBrowser = new FileBrowser(CoolReader.this, Services.getEngine(), Services.getScanner(), Services.getHistory(),
						settings().getBool(PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES, false));
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
						ReaderAction.SAVE_LOGCAT,
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
						case DCMD_SAVE_LOGCAT:
							createLogcatFile();
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
				if (mSyncGoogleDriveEnabled && null != mGoogleDriveSync && !mGoogleDriveSync.isBusy()) {
					ArrayList<Synchronizer.SyncTarget> targets = new ArrayList<Synchronizer.SyncTarget>();
					if (mSyncGoogleDriveEnabledCurrentBookInfo)
						targets.add(Synchronizer.SyncTarget.CURRENTBOOKINFO);
					if (mSyncGoogleDriveEnabledCurrentBookBody)
						targets.add(Synchronizer.SyncTarget.CURRENTBOOKBODY);
					if (!targets.isEmpty())
						mGoogleDriveSync.startSyncToOnly(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_SHOW_SIGN_IN, targets.toArray(new Synchronizer.SyncTarget[0]));
				}
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

	/**
	 * When current book is opened, switch to previous book.
	 *
	 * @param errorCallback
	 */
	public void loadPreviousDocument(Runnable errorCallback) {
		BookInfo bi = Services.getHistory().getPreviousBook();
		if (bi != null && bi.getFileInfo() != null) {
			log.i("loadPreviousDocument() is called, prevBookName = " + bi.getFileInfo().getPathName());
			loadDocument(bi.getFileInfo(), null, errorCallback, true);
			return;
		}
		errorCallback.run();
	}

	public void showOpenedBook()
	{
		showReader();
	}
	
	public static final String OPEN_DIR_PARAM = "DIR_TO_OPEN";
	public void showBrowser(final FileInfo dir, String addFilter) {
		String pathname = "";
		if (dir != null) pathname = dir.pathname;
		runInBrowser(() -> mBrowser.showDirectory(dir, null, addFilter, null), FileInfo.RESCAN_LIBRARY_TAG.equals(pathname));
	}
	
	public void showBrowser(final String dir) {
		runInBrowser(() -> mBrowser.showDirectory(Services.getScanner().pathToFileInfo(dir), null, "", null), FileInfo.RESCAN_LIBRARY_TAG.equals(dir));
	}

	public void showBrowser(final String dir, String addFilter) {
		runInBrowser(() -> mBrowser.showDirectory(Services.getScanner().pathToFileInfo(dir), null, addFilter, null), FileInfo.RESCAN_LIBRARY_TAG.equals(dir));
	}

	public void showBrowser(final String dir, Object params) {
		runInBrowser(() -> mBrowser.showDirectory(Services.getScanner().pathToFileInfo(dir), null, "", params), FileInfo.RESCAN_LIBRARY_TAG.equals(dir));
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
		runInBrowser(() -> mBrowser.showDirectory(path, null, addFilter, null), false);
	}

	public void setBrowserTitle(String title, FileInfo dir) {
		if (mBrowserFrame != null)
			mBrowserFrame.setBrowserTitle(title, dir);
	}

	public void setBrowserProgressStatus(boolean enable) {
		if (mBrowserFrame != null)
			mBrowserFrame.setBrowserProgressStatus(enable);
	}

	public void setBrowserBottomBar(boolean isLitres) {
		if (mBrowserFrame != null)
			mBrowserFrame.setBrowserBottomBar(isLitres);
	}
	
	// Dictionary support

	public interface DictionaryCallback {
		boolean showDicToast();
		boolean saveToHist();
		void done(String result);
		void fail(Exception e, String msg);
	}

	public void findInDictionary(String s, View view) {
		findInDictionary(s, view, null);
	}

	public void findInDictionary(String s, View view, DictionaryCallback dcb) {
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
						BackgroundThread.instance().postGUI(() -> findInDictionaryInternal(pattern, view, dcb), 100));
			}
		}
	}
	
	private void findInDictionaryInternal(String s, View view, DictionaryCallback dcb) {
		log.d("lookup in dictionary: " + s);
		try {
			mDictionaries.findInDictionary(s, view, dcb);
		} catch (DictionaryException e) {
			if (e.getMessage().contains("is not installed")) {
				optionsFilter = "";
				showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_DICTIONARY_TITLE);
			}
			showToast(e.getMessage());
		}
	}

	public Dictionaries.DictInfo getCurOrFirstOnlineDic() {
		Dictionaries.DictInfo di1 = mDictionaries.getCurDict();
		if (di1 != null)
			if (di1.isOnline()) return di1;
		for (final Dictionaries.DictInfo di: mDictionaries.getAddDicts()) {
			if (di.isOnline()) return di;
		}
		return null;
	}

	public void showDictionary() {
		findInDictionaryInternal(null, null, null);
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
		if (requestCode == REQUEST_CODE_GOOGLE_DRIVE_SIGN_IN) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				if (null != mGoogleDriveSync) {
					mGoogleDriveSync.onActivityResultHandler(requestCode, resultCode, intent);
				}
			}
		} else if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				if (resultCode == Activity.RESULT_OK) {
					switch (mOpenDocumentTreeCommand) {
						case ODT_CMD_DEL_FILE:
							if (mOpenDocumentTreeArg != null && !mOpenDocumentTreeArg.isDirectory) {
								Uri sdCardUri = intent.getData();
								DocumentFile documentFile = null;
								if (null != sdCardUri)
									documentFile = Utils.getDocumentFile(mOpenDocumentTreeArg, this, sdCardUri);
								if (null != documentFile) {
									if (documentFile.delete()) {
										Services.getHistory().removeBookInfo(getDB(), mOpenDocumentTreeArg, true, true);
										final FileInfo dirToUpdate = mOpenDocumentTreeArg.parent;
										if (null != dirToUpdate)
											BackgroundThread.instance().postGUI(() -> directoryUpdated(dirToUpdate), 700);
										updateExtSDURI(mOpenDocumentTreeArg, sdCardUri);
									} else {
										showToast(R.string.could_not_delete_file, mOpenDocumentTreeArg);
									}
								} else {
									showToast(R.string.could_not_delete_on_sd);
								}
							}
							break;
						case ODT_CMD_DEL_FOLDER:
							if (mOpenDocumentTreeArg != null && mOpenDocumentTreeArg.isDirectory) {
								Uri sdCardUri = intent.getData();
								DocumentFile documentFile = null;
								if (null != sdCardUri)
									documentFile = Utils.getDocumentFile(mOpenDocumentTreeArg, this, sdCardUri);
								if (null != documentFile) {
									if (documentFile.exists()) {
										updateExtSDURI(mOpenDocumentTreeArg, sdCardUri);
										deleteFolder(mOpenDocumentTreeArg);
									}
								} else {
									showToast(R.string.could_not_delete_on_sd);
								}
							}
							break;
						case ODT_CMD_SAVE_LOGCAT:
							if (mOpenDocumentTreeArg != null) {
								Uri uri = intent.getData();
								if (null != uri) {
									DocumentFile docFolder = DocumentFile.fromTreeUri(this, uri);
									if (null != docFolder) {
										DocumentFile file = docFolder.createFile("text/x-log", mOpenDocumentTreeArg.getFilename());
										if (null != file) {
											try {
												OutputStream ostream = getContentResolver().openOutputStream(file.getUri());
												if (null != ostream) {
													saveLogcat(file.getName(), ostream);
													ostream.close();
												} else {
													log.e("logcat: failed to open stream!");
												}
											} catch (Exception e) {
												log.e("logcat: " + e);
											}
										} else {
											log.e("logcat: can't create file!");
										}
									}
								} else {
									log.d("logcat creation canceled by user");
								}
							}
							break;


					}
					mOpenDocumentTreeArg = null;

				}
			}
		} //if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE)
	 	  else if (requestCode == REQUEST_CODE_CHOOSE_DIR) {
	 	  	try {
	 	  		if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
					cced.edtLocalFolder.setText(intent.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
				}
			} finally {
	 	  		// do nothing
			}
		}
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
        if (!id.equals("0"))
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
	public TextToSpeech tts;
	public boolean ttsInitialized;
	private boolean ttsError;
	private String ttsEnginePackage;
	private Timer initTTSTimer;

	private final static long INIT_TTS_TIMEOUT = 10000;		// 10 sec.

	public boolean initTTS(final OnTTSCreatedListener listener) {
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
		showToast("Initializing TTS");
		TextToSpeech.OnInitListener onInitListener = status -> {
			//tts.shutdown();
			if (initTTSTimer != null) initTTSTimer.cancel();
			initTTSTimer = null;
			L.i("TTS init status: " + status);
			if (status == TextToSpeech.SUCCESS) {
				ttsInitialized = true;
				BackgroundThread.instance().executeGUI(() -> listener.onCreated(tts));
			} else {
				ttsError = true;
				BackgroundThread.instance().executeGUI(() -> showToast("Cannot initialize TTS"));
			}
		};
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH && null != ttsEnginePackage && ttsEnginePackage.length() > 0)
			tts = new TextToSpeech(this, onInitListener, ttsEnginePackage);
		else
			tts = new TextToSpeech(this, onInitListener);
		initTTSTimer = new Timer();
		initTTSTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// TTS engine init hangs, remove it from settings
				log.e("TTS engine \"" + ttsEnginePackage + "\" init failure, disabling!");
				BackgroundThread.instance().executeGUI(() -> showToast(R.string.tts_init_failure, ttsEnginePackage));
				setSetting(PROP_APP_TTS_ENGINE, "", false);
				ttsEnginePackage = "";
				try {
					mReaderView.getTTSToolbar().stopAndClose();
				} catch (Exception ignored) {}
				initTTSTimer.cancel();
				initTTSTimer = null;
			}
		}, INIT_TTS_TIMEOUT);
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
					//if (OptionsDialog.toastShowCnt < 5) showToast(getString(R.string.settings_info));
					OptionsDialog dlg = new OptionsDialog(CoolReader.this, mode, mReaderView, mFontFaces, mFontFacesFiles, null);
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
					OptionsDialog dlg = new OptionsDialog(CoolReader.this, mode, mReaderView, mFontFaces, mFontFacesFiles, null);
					dlg.selectedTab = tab;
					dlg.show();
				});
			});
	}

	public void showOptionsDialogExt(final OptionsDialog.Mode mode, final String selectOption)
	{
		showOptionsDialogExt(mode, selectOption, null);
	}

	public void showOptionsDialogExt(final OptionsDialog.Mode mode, final String selectOption, TextToSpeech tts)
	{
		BackgroundThread.instance().postBackground(() -> {
			final String[] mFontFaces = Engine.getFontFaceList();
			final String[] mFontFacesFiles = Engine.getFontFaceAndFileNameList();
			BackgroundThread.instance().executeGUI(() -> {
				if ((!selectOption.equals(PROP_FILEBROWSER_TITLE))&&(selectOption == null)) {
					OptionsDialog.toastShowCnt++;
					//if (OptionsDialog.toastShowCnt < 5) showToast(getString(R.string.settings_info));
				}
				OptionsDialog dlg = new OptionsDialog(CoolReader.this, mode, mReaderView, mFontFaces, mFontFacesFiles, tts);
				dlg.selectedOption = selectOption;
				dlg.show();
			});
		});
	}

	public void AskBookStars(FileInfo book) {
		BackgroundThread.instance().postGUI(() -> {
			ArrayList<String> sButtons = new ArrayList<String>();
			sButtons.add("*" + getString(R.string.book_info_rating));
			sButtons.add(getString(R.string.mi_book_rate_5));
			sButtons.add(getString(R.string.mi_book_rate_4));
			sButtons.add(getString(R.string.mi_book_rate_3));
			sButtons.add(getString(R.string.mi_book_rate_2));
			sButtons.add(getString(R.string.mi_book_rate_1));
			sButtons.add(getString(R.string.mi_book_rate_0));
			sButtons.add(getString(R.string.str_cancel));
			SomeButtonsToolbarDlg.showDialog(this, getReaderView().getSurface(), 10, true,
					"",
				sButtons, null, (o22, btnPressed) -> {
					if (btnPressed.equals(getString(R.string.mi_book_rate_5))) {
						setBookRate(book, 5);
					}
					if (btnPressed.equals(getString(R.string.mi_book_rate_4))) {
						setBookRate(book, 4);
					}
					if (btnPressed.equals(getString(R.string.mi_book_rate_3))) {
						setBookRate(book, 3);
					}
					if (btnPressed.equals(getString(R.string.mi_book_rate_2))) {
						setBookRate(book, 2);
					}
					if (btnPressed.equals(getString(R.string.mi_book_rate_1))) {
						setBookRate(book, 1);
					}
					if (btnPressed.equals(getString(R.string.mi_book_rate_0))) {
						setBookRate(book, 0);
					}
				});
		}, 200);
	}

	public void updateCurrentPositionStatus(FileInfo book, Bookmark position, PositionProperties props) {
		if (getReaderView() == null) return;
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
			int iSyncVariant2 = settings().getInt(PROP_CLOUD_SYNC_VARIANT, 0);
			final FileInfo book1=book;
			BackgroundThread.instance().postGUI(() -> {
				ArrayList<String> sButtons = new ArrayList<String>();
				sButtons.add("*" + getString(R.string.mark_book_as_read));
				if (iSyncVariant2 == 2) sButtons.add(getString(R.string.str_yes_and_del_pos)); // only for yandex
				sButtons.add(getString(R.string.str_yes));
				sButtons.add(getString(R.string.str_no));
				SomeButtonsToolbarDlg.showDialog(this, getReaderView().getSurface(), 10, true,
						"",
					sButtons, null, (o22, btnPressed) -> {
							if (btnPressed.equals(getString(R.string.str_yes))) {
								setBookStateFinished(book1);
								AskBookStars(book1);
							}
							if (btnPressed.equals(getString(R.string.str_yes_and_del_pos))) {
								setBookStateFinished(book1);
								AskBookStars(book1);
								CloudSync.loadFromJsonInfoFileList(this,
										CloudSync.CLOUD_SAVE_READING_POS, false, iSyncVariant2 == 1, CloudAction.DELETE_FILES, false);
							}
					});
			}, 200);
		}
		checkAskReading(book, props,13, false);
	}

	private void setBookStateFinished(FileInfo book1) {
		Services.getHistory().getOrCreateBookInfo(getDB(), book1, bookInfo -> {
			book1.setReadingState(FileInfo.STATE_FINISHED);
			BookInfo bi = new BookInfo(book1);
			getDB().saveBookInfo(bi);
			getDB().flush();
			if (bookInfo.getFileInfo() != null) {
				bookInfo.getFileInfo().setReadingState(FileInfo.STATE_FINISHED);
				if (bookInfo.getFileInfo().parent != null)
					directoryUpdated(bookInfo.getFileInfo().parent, bookInfo.getFileInfo());
			}
			BookInfo bi2 = Services.getHistory().getBookInfo(book1);
			if (bi2 != null)
				bi2.getFileInfo().setFileProperties(book1);
		});
	}

	private void setBookRate(FileInfo book1, int rate) {
		Services.getHistory().getOrCreateBookInfo(getDB(), book1, bookInfo -> {
			book1.setRate(rate);
			BookInfo bi = new BookInfo(book1);
			getDB().saveBookInfo(bi);
			getDB().flush();
			if (bookInfo.getFileInfo() != null) {
				bookInfo.getFileInfo().setRate(rate);
				if (bookInfo.getFileInfo().parent != null)
					directoryUpdated(bookInfo.getFileInfo().parent, bookInfo.getFileInfo());
			}
			BookInfo bi2 = Services.getHistory().getBookInfo(book1);
			if (bi2 != null)
				bi2.getFileInfo().setFileProperties(book1);
		});
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
				BackgroundThread.instance().postGUI(() -> {
					ArrayList<String> sButtons = new ArrayList<String>();
					sButtons.add("*" + getString(R.string.mark_book_as_reading));
					sButtons.add(getString(R.string.str_yes));
					sButtons.add(getString(R.string.str_no));
					SomeButtonsToolbarDlg.showDialog(this, getReaderView().getSurface(), 10, true,
							"",
							sButtons, null, (o22, btnPressed) -> {
								if (btnPressed.equals(getString(R.string.str_yes))) {
									Services.getHistory().getOrCreateBookInfo(getDB(), book1, bookInfo -> {
										book1.setReadingState(FileInfo.STATE_READING);
										BookInfo bi = new BookInfo(book1);
										getDB().saveBookInfo(bi);
										getDB().flush();
										if (bookInfo.getFileInfo() != null) {
											bookInfo.getFileInfo().setReadingState(FileInfo.STATE_READING);
											if (bookInfo.getFileInfo().parent != null)
												directoryUpdated(bookInfo.getFileInfo().parent, bookInfo.getFileInfo());
										}
										BookInfo bi2 = Services.getHistory().getBookInfo(book1);
										if (bi2 != null)
											bi2.getFileInfo().setFileProperties(book1);
										if (openBrowser)
											showBrowser(!isBrowserCreated() ? getReaderView().getOpenedFileInfo() : null, "");
									});
								} else {
									if (openBrowser)
										showBrowser(!isBrowserCreated() ? getReaderView().getOpenedFileInfo() : null, "");
								}
							});
				}, 200);
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
	
	public void sendBookFragment(BookInfo bookInfo, String chapter, String text) {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
    	emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, (bookInfo.getFileInfo().getAuthors() + " " + bookInfo.getFileInfo().getTitle()
		 + " " + chapter).trim());
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
	
	public void openURL(String url, boolean withAsking) {
		if (!withAsking) {
			try {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
			} catch (Exception e) {
				log.e("Exception " + e + " while trying to open URL " + url);
				showToast("Cannot open URL " + url);
			}
		} else {
			ArrayList<String> sButtons = new ArrayList<>();
			sButtons.add("*" + getString(R.string.open_url, url));
			sButtons.add(getString(R.string.open_url_browser));
			sButtons.add(getString(R.string.open_url_kr));
			sButtons.add(getString(R.string.open_url_cancel));
			View anch = mCurrentFrame;
			if (mCurrentFrame == mReaderFrame) anch = getReaderView().getSurface();
			SomeButtonsToolbarDlg.showDialog(this, anch, 10, true,
					"",
					sButtons, null, (o22, btnPressed) -> {
						if (btnPressed.equals(getString(R.string.open_url_browser))) {
							try {
								Intent i = new Intent(Intent.ACTION_VIEW);
								i.setData(Uri.parse(url));
								startActivity(i);
							} catch (Exception e) {
								log.e("Exception " + e + " while trying to open URL " + url);
								showToast("Cannot open URL " + url);
							}
						}
						if (btnPressed.equals(getString(R.string.open_url_kr))) {
							processIntentContent("", url);
							return;
						}
					});
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

	public void askDeleteBook(final FileInfo item) {
		askConfirmation(R.string.win_title_confirm_book_delete, () -> {
			closeBookIfOpened(item);
			FileInfo file = Services.getScanner().findFileInTree(item);
			if (file == null)
				file = item;
			final FileInfo finalFile = file;
			if (file.deleteFile()) {
				waitForCRDBService(() -> {
					Services.getHistory().removeBookInfo(getDB(), finalFile, true, true);
					BackgroundThread.instance().postGUI(() -> directoryUpdated(finalFile.parent, null), 700);
				});
			} else {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					DocumentFile documentFile = null;
					Uri sdCardUri = getExtSDURIByFileInfo(file);
					if (sdCardUri != null)
						documentFile = Utils.getDocumentFile(file, this, sdCardUri);
					if (null != documentFile) {
						if (documentFile.delete()) {
							waitForCRDBService(() -> {
								Services.getHistory().removeBookInfo(getDB(), finalFile, true, true);
								BackgroundThread.instance().postGUI(() -> directoryUpdated(finalFile.parent), 700);
							});
						} else {
							showToast(R.string.could_not_delete_file, file);
						}
					} else {
						showToast(R.string.choose_root_sd);
						mOpenDocumentTreeArg = file;
						mOpenDocumentTreeCommand = ODT_CMD_DEL_FILE;
						Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
						startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);
					}
				} else {
					showToast(R.string.could_not_delete_file, file);
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

	public void askDeleteCalibreCatalog(final FileInfo item)
	{
		askConfirmation(R.string.win_title_confirm_catalog_delete, () -> {
			if (item != null && item.isCalibreRoot()) {
				waitForCRDBService(() -> {
					getDB().removeCalibreCatalog(item.id);
					directoryUpdated(Services.getScanner().createCalibreRoot());
					refreshOPDSRootDirectory(true);
				});
			}
		});
	}

	int mFolderDeleteRetryCount = 0;
	public void askDeleteFolder(final FileInfo item) {
		askConfirmation(R.string.win_title_confirm_folder_delete, () -> {
			mFolderDeleteRetryCount = 0;
			deleteFolder(item);
		});
	}

	private void deleteFolder(final FileInfo item) {
		if (mFolderDeleteRetryCount > 3)
			return;
		if (item != null && item.isDirectory && !item.isOPDSDir() && !item.isOnlineCatalogPluginDir()) {
			FileInfoOperationListener bookDeleteCallback = (fileInfo, errorStatus) -> {
				if (0 == errorStatus && null != fileInfo.format) {
					BackgroundThread.instance().executeGUI(() -> {
						waitForCRDBService(() -> Services.getHistory().removeBookInfo(getDB(), fileInfo, true, true));
					});
				}
			};
			BackgroundThread.instance().postBackground(() -> Utils.deleteFolder(item, bookDeleteCallback, (fileInfo, errorStatus) -> {
				if (0 == errorStatus) {
					BackgroundThread.instance().executeGUI(() -> directoryUpdated(fileInfo.parent));
				} else {
					// Can't be deleted using standard Java I/O,
					// Try DocumentFile interface...
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						Uri sdCardUri = getExtSDURIByFileInfo(item);
						if (null != sdCardUri) {
							Utils.deleteFolderDocTree(item, this, sdCardUri, bookDeleteCallback, (fileInfo2, errorStatus2) -> {
								BackgroundThread.instance().executeGUI(() -> {
									if (0 == errorStatus2) {
										directoryUpdated(fileInfo2.parent);
									} else {
										showToast(R.string.choose_root_sd);
										mFolderDeleteRetryCount++;
										mOpenDocumentTreeCommand = ODT_CMD_DEL_FOLDER;
										mOpenDocumentTreeArg = item;
										Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
										startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);
									}
								});
							});
						} else {
							BackgroundThread.instance().executeGUI(() -> {
								showToast(R.string.choose_root_sd);
								mFolderDeleteRetryCount++;
								mOpenDocumentTreeCommand = ODT_CMD_DEL_FOLDER;
								mOpenDocumentTreeArg = item;
								Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
								startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);
							});
						}
					}
				}
			}));
		}
	}

	public void createLogcatFile() {
		final SimpleDateFormat format = new SimpleDateFormat("'cr3-'yyyy-MM-dd_HH_mm_ss'.log'", Locale.US);
		FileInfo dir = Services.getScanner().getSharedDownloadDirectory();
		if (null == dir) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				log.d("logcat: no access to download directory, opening document tree...");
				askConfirmation(R.string.confirmation_select_folder_for_log, () -> {
					mOpenDocumentTreeCommand = ODT_CMD_SAVE_LOGCAT;
					mOpenDocumentTreeArg = new FileInfo(format.format(new Date()));
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);
				});
			} else {
				log.e("Can't create logcat file: no access to download directory!");
			}
		} else {
			try {
				File outputFile = new File(dir.pathname, format.format(new Date()));
				FileOutputStream ostream = new FileOutputStream(outputFile);
				saveLogcat(outputFile.getCanonicalPath(), ostream);
			} catch (Exception e) {
				log.e("createLogcatFile: " + e);
			}
		}
	}

	private void saveLogcat(String fileName, OutputStream ostream) {
		Date since = getLastLogcatDate();
		Date now = new Date();
		if (LogcatSaver.saveLogcat(since, ostream)) {
			setLastLogcatDate(now);
			log.i("logcat saved to file " + fileName);
			//showToast("Logcat saved to " + fileName);
			BackgroundThread.instance().postGUI(() -> {
				ArrayList<String> sButtons = new ArrayList<>();
				sButtons.add("*" + getString(R.string.notice_log_saved_to_, fileName));
				sButtons.add(getString(R.string.str_yes));
				sButtons.add(getString(R.string.str_no));
				View anch = mCurrentFrame;
				if (mCurrentFrame == mReaderFrame) anch = getReaderView().getSurface();
				//if (mCurrentFrame == mHomeFrame) anch = mHomeFrame.;
				SomeButtonsToolbarDlg.showDialog(this, anch, 10, true,
						"",
						sButtons, null, (o22, btnPressed) -> {
							if (btnPressed.equals(getString(R.string.str_yes))) {
								File f = new File(fileName);
								Uri path = FileProvider.getUriForFile(this, FlavourConstants.FILE_PROVIDER_NAME, f);
								Intent emailIntent = new Intent(Intent.ACTION_SEND);
								// set the type to 'email'
								emailIntent.setType("vnd.android.cursor.dir/email");
								// the attachment
								emailIntent.putExtra(Intent.EXTRA_STREAM, path);
								// the mail subject
								emailIntent.putExtra(Intent.EXTRA_SUBJECT, "KnownReader logcat file");
								startActivity(Intent.createChooser(emailIntent, getString(R.string.send_logcat)+ "..."));
							}
						});
			}, 200);
		} else {
			log.e("Failed to save logcat to " + fileName);
			showToast("Failed to save logcat to " + fileName);
		}
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

	public void editBookTransl(final boolean noQuick,
							   final View anchor,
							   final FileInfo currDirectory, final FileInfo item,
							   final String lang_from, final String lang_to, final String search_text,
							   BookInfoEditDialog bied, int iType) {
		if (!noQuick) {
			String sQuickDirs = settings().getProperty(Settings.PROP_APP_QUICK_TRANSLATION_DIRS);
			if (!StrUtils.isEmptyStr(sQuickDirs)) {
				String[] sQuickDirsArr = sQuickDirs.split(";");
				int iCnt = 0;
				ArrayList<String> sButtons = new ArrayList<>();
				sButtons.add(getString(R.string.all_languages));
				for (String s: sQuickDirsArr)
					if (s.contains("=")) {
						sButtons.add(s.replace("=", " -> "));
						iCnt++;
					}
				if (iCnt > 0) {
					View anch = anchor;
					if (anch == null) {
						anch = mCurrentFrame;
						if (mCurrentFrame == mReaderFrame) anch = getReaderView().getSurface();
					}
					SomeButtonsToolbarDlg.showDialog(this, anch, 0, true,
							getString(R.string.select_transl_dir),
							sButtons, null, (o22, btnPressed) -> {
								if (btnPressed.equals(getString(R.string.all_languages))) {
									editBookTransl(true,
										anchor, currDirectory, item, lang_from, lang_to, search_text, bied, iType);
									return;
								}
								if (btnPressed.contains("->")) {
									String[] pair = btnPressed.replace(" -> ", "=").split("=");
									String l_from = pair[0];
									String l_to = pair[1];
									waitForCRDBService(() -> Services.getHistory().getOrCreateBookInfo(getDB(), item, bookInfo -> {
											if (bookInfo == null) bookInfo = new BookInfo(item);
											BookInfo bookInfoF = bookInfo;
											FileInfo file = bookInfoF.getFileInfo();
											if (bied != null) bied.edLangFrom.setText(l_from);
												else file.lang_from = l_from;
											if (bied != null) bied.edLangTo.setText(l_to);
												else file.lang_to = l_to;
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
													findInDictionary(search_text, null, null);
											}
										}
									));
								}
							});
					return;
				}
			}
		}
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
									findInDictionary(search_text, null, null);
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

	public void showCloudItemInfo(final FileInfo item, final FileBrowser fb, FileInfo currDir) {
		final ArrayList<BookInfoEntry> itemsAll = new ArrayList<>();
		itemsAll.add(new BookInfoEntry("section","section.file","section"));
		String sFormat = "";
		if (item.format != null)
			if (item.format != DocumentFormat.NONE)
				sFormat = item.format.name();
		itemsAll.add(new BookInfoEntry("file.format",sFormat,"text"));
		itemsAll.add(new BookInfoEntry("file.name",item.pathname,"text"));
		itemsAll.add(new BookInfoEntry("section","section.book","section"));
		if (!StrUtils.isEmptyStr(item.authors))
			itemsAll.add(new BookInfoEntry("book.authors", StrUtils.getNonEmptyStr(item.getAuthors(), true).replace("|", "; "),"text"));
		itemsAll.add(new BookInfoEntry("book.title", item.title,"text"));
		if (!StrUtils.isEmptyStr(item.genre))
			itemsAll.add(new BookInfoEntry("book.genre", item.genre.replace("|", "; "),"text"));
		if (!StrUtils.isEmptyStr(item.getSeriesName()))
			itemsAll.add(new BookInfoEntry("book.series", item.getSeriesName().replace("|", "; "),"text"));
		bookInfoAddPublisher(item, itemsAll);
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
						String key = entry.getKey();
						final String value = entry.getValue();
						itemsAll.add(new BookInfoEntry(key, value, "text"));
					}
				}
			}
		String annot = item.getFilename();
		if (item.isLitresBook() || item.isLitresSpecialDir())
			if (!StrUtils.isEmptyStr(item.annotation)) annot = item.annotation;
		BookInfoDialog dlg = new BookInfoDialog(this, itemsAll, null, annot,
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

	public void bookInfoAddPublisher(FileInfo fi, ArrayList<BookInfoEntry> itemsBook) {
		boolean isEmpty = true;
		if (!StrUtils.isEmptyStr(fi.publname)) isEmpty = false;
		if (!StrUtils.isEmptyStr(fi.publisher)) isEmpty = false;
		if (!StrUtils.isEmptyStr(fi.publcity)) isEmpty = false;
		if (!StrUtils.isEmptyStr(fi.getPublyear())) isEmpty = false;
		if (!StrUtils.isEmptyStr(fi.publisbn))  isEmpty = false;
		if (fi.publseries != null) {
			String s = fi.publseries;
			if (fi.publseriesNumber > 0) isEmpty = false;
		}
		if (isEmpty) return;
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
			itemsSys.add(new BookInfoEntry("system.battery",getReaderView().getBatteryStateText(),"text"));
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
				//CR's implementation
//				if (fi.format == DocumentFormat.FB2) {
//					if (fi.genres != null && fi.genres.length() > 0) {
//						items.add("book.genres=" + fi.genres);
//					}
//				}
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
				bookInfoAddPublisher(fi, itemsBook);
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
		if (opds == null) {
			opds = new FileInfo();
			opds.isDirectory = true;
			opds.pathname = FileInfo.OPDS_DIR_PREFIX + "http://";
			opds.setFilename("New Catalog");
			opds.isListed = true;
			opds.isScanned = true;
			opds.parent = Services.getScanner().getOPDSRoot();
		}
		if (StrUtils.getNonEmptyStr(opds.pathname, false).startsWith("@calibre")) {
			CalibreCatalogEditDialog dlg = new CalibreCatalogEditDialog(CoolReader.this, opds,
					() -> refreshOPDSRootDirectory(true));
			dlg.show();
		} else {
			OPDSCatalogEditDialog dlg = new OPDSCatalogEditDialog(CoolReader.this, opds,
					() -> refreshOPDSRootDirectory(true));
			dlg.show();
		}
	}

	public void editCalibreCatalog(FileInfo fi) {
		if (fi == null) {
			fi = new FileInfo();
			fi.isDirectory = true;
			fi.pathname = FileInfo.OPDS_DIR_PREFIX + "http://";
			fi.setFilename("New Calibre Catalog");
			fi.isListed = true;
			fi.isScanned = true;
			fi.parent = Services.getScanner().getOPDSRoot();
		}
		CalibreCatalogEditDialog dlg = new CalibreCatalogEditDialog(CoolReader.this, fi,
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

	private static final int NOTIFICATION_READER_MENU_MASK = 0x01;
	private static final int NOTIFICATION_LOGCAT_MASK = 0x02;
	private static final int NOTIFICATION_MASK_ALL = NOTIFICATION_READER_MENU_MASK |
			NOTIFICATION_LOGCAT_MASK;

	public void setLastNotificationMask(int notificationId) {
		try {
			SharedPreferences.Editor editor = getPrefs().edit();
			editor.putInt(PREF_LAST_NOTIFICATION_MASK, notificationId);
			editor.commit();
		} catch (Exception e) {
			// ignore
		}
	}

	public int getLastNotificationMask() {
		int res = getPrefs().getInt(PREF_LAST_NOTIFICATION_MASK, 0);
		log.i("getLastNotification() = " + res);
		return res;
	}

	public void showNotifications() {
		int lastNoticeMask = getLastNotificationMask();
		if ((lastNoticeMask & NOTIFICATION_MASK_ALL) == NOTIFICATION_MASK_ALL)
			return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.HONEYCOMB) {
			if ((lastNoticeMask & NOTIFICATION_READER_MENU_MASK) == 0) {
				notification1();
				return;
			}
		}
		if ((lastNoticeMask & NOTIFICATION_LOGCAT_MASK) == 0) {
			notification2();
		}
	}
	
	public void notification1()
	{
		if (hasHardwareMenuKey())
			return; // don't show notice if hard key present
		setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_SHORT_SIDE), false);
		setSetting(PROP_TOOLBAR_APPEARANCE, String.valueOf(8), true);
		//return; // KnownReader - decided to remove
		setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_READER_MENU_MASK);
		showNotifications();

//		showNotice(R.string.note1_reader_menu,
//				R.string.dlg_button_yes, () -> {
//					setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_SHORT_SIDE), false);
//					setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_READER_MENU_MASK);
//					showNotifications();
//				},
//				R.string.dlg_button_no, () -> {
//					setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_NONE), false);
//					setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_READER_MENU_MASK);
//					showNotifications();
//				}
//		);
	}

	//KR - still thinking this is not needed and makes interface ugly
//	public void notification1_CR() {
//		if (hasHardwareMenuKey())
//			return; // don't show notice if hard key present
//		showNotice(R.string.note1_reader_menu,
//				() -> {
//					setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_SHORT_SIDE), false);
//					setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_READER_MENU_MASK);
//					showNotifications();
//				},
//				() -> {
//					setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_NONE), false);
//					setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_READER_MENU_MASK);
//					showNotifications();
//				}
//		);
//	}

	public void notification2() {
//		showNotice(R.string.note2_logcat,
//				() -> {
					setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_LOGCAT_MASK);
//					showNotifications();
//				}
//		);
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

	private boolean updateExtSDURI(FileInfo fi, Uri extSDUri) {
		String prefKey = null;
		String filePath = null;
		if (fi.isArchive && fi.arcname != null) {
			filePath = fi.arcname;
		} else
			filePath = fi.pathname;
		if (null != filePath) {
			File f = new File(filePath);
			filePath = f.getAbsolutePath();
			String[] parts = filePath.split("\\/");
			if (parts.length >= 3) {
				// For example,
				// parts[0] = ""
				// parts[1] = "storage"
				// parts[2] = "1501-3F19"
				// then prefKey = "/storage/1501-3F19"
				prefKey = "uri_for_/" + parts[1] + "/" + parts[2];
			}
		}
		if (null != prefKey) {
			SharedPreferences prefs = getPrefs();
			return prefs.edit().putString(prefKey, extSDUri.toString()).commit();
		}
		return false;
	}

	private Uri getExtSDURIByFileInfo(FileInfo fi) {
		Uri uri = null;
		String prefKey = null;
		String filePath = null;
		if (fi.isArchive && fi.arcname != null) {
			filePath = fi.arcname;
		} else
			filePath = fi.pathname;
		if (null != filePath) {
			File f = new File(filePath);
			filePath = f.getAbsolutePath();
			String[] parts = filePath.split("\\/");
			if (parts.length >= 3) {
				prefKey = "uri_for_/" + parts[1] + "/" + parts[2];
			}
		}
		if (null != prefKey) {
			SharedPreferences prefs = getPrefs();
			String strUri = prefs.getString(prefKey, null);
			if (null != strUri)
				uri = Uri.parse(strUri);
		}
		return uri;
	}

	private Date getLastLogcatDate() {
		long dateMillis = getPrefs().getLong(PREF_LAST_LOGCAT, 0);
		return new Date(dateMillis);
	}

	private void setLastLogcatDate(Date date) {
		SharedPreferences.Editor editor = getPrefs().edit();
		editor.putLong(PREF_LAST_LOGCAT, date.getTime());
		editor.commit();
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
		Utils.saveStringToFileSafe(json,getSettingsFileExt("[DEFAULT]",0).getParent() + "/ynd_cloud_settings.json");
	}

	public YndCloudSettings readYndCloudSettings()
	{
		log.d("Reading ynd_cloud_settings.json");
		String s = Utils.readFileToString(getSettingsFileExt("[DEFAULT]",0).getParent() + "/ynd_cloud_settings.json");
		try {
			final File fJson = new File(getSettingsFileExt("[DEFAULT]",0).getParent() + "/ynd_cloud_settings.json");
			if (!fJson.exists()) return null;
			yndCloudSettings = new Gson().fromJson(s, YndCloudSettings.class);
			return yndCloudSettings;
		} catch (Exception e) {
		}
		return null;
	}

	public LingvoCloudSettings lingvoCloudSettings = new LingvoCloudSettings();

	public void saveLingvoCloudSettings(String json)
	{
		log.d("Starting save lingvo_cloud_settings.json");
		Utils.saveStringToFileSafe(json,getSettingsFileExt("[DEFAULT]",0).getParent() + "/lingvo_cloud_settings.json");
	}

	public LingvoCloudSettings readLingvoCloudSettings()
	{
		log.d("Reading lingvo_cloud_settings.json");
		String s = Utils.readFileToString(getSettingsFileExt("[DEFAULT]",0).getParent() + "/lingvo_cloud_settings.json");
		try {
			final File fJson = new File(getSettingsFileExt("[DEFAULT]",0).getParent() + "/lingvo_cloud_settings.json");
			if (!fJson.exists()) return null;
			lingvoCloudSettings = new Gson().fromJson(s, LingvoCloudSettings.class);
			return lingvoCloudSettings;
		} catch (Exception e) {
		}
		return null;
	}

	public DeeplCloudSettings deeplCloudSettings = new DeeplCloudSettings();

	public void saveDeeplCloudSettings(String json)
	{
		log.d("Starting save deepl_cloud_settings.json");
		Utils.saveStringToFileSafe(json,getSettingsFileExt("[DEFAULT]",0).getParent() + "/deepl_cloud_settings.json");
	}

	public DeeplCloudSettings readDeeplCloudSettings()
	{
		log.d("Reading deepl_cloud_settings.json");
		String s = Utils.readFileToString(getSettingsFileExt("[DEFAULT]",0).getParent() + "/deepl_cloud_settings.json");
		try {
			final File fJson = new File(getSettingsFileExt("[DEFAULT]",0).getParent() + "/deepl_cloud_settings.json");
			if (!fJson.exists()) return null;
			deeplCloudSettings = new Gson().fromJson(s, DeeplCloudSettings.class);
			return deeplCloudSettings;
		} catch (Exception e) {
		}
		return null;
	}

	public LitresCloudSettings litresCloudSettings = new LitresCloudSettings();

	public void saveLitresCloudSettings(String json)
	{
		log.d("Starting save litres_cloud_settings.json");
		Utils.saveStringToFileSafe(json,getSettingsFileExt("[DEFAULT]",0).getParent() + "/litres_cloud_settings.json");
	}

	public LitresCloudSettings readLitresCloudSettings()
	{
		log.d("Reading litres_cloud_settings.json");
		String s = Utils.readFileToString(getSettingsFileExt("[DEFAULT]",0).getParent() + "/litres_cloud_settings.json");
		try {
			final File fJson = new File(getSettingsFileExt("[DEFAULT]",0).getParent() + "/litres_cloud_settings.json");
			if (!fJson.exists()) return null;
			litresCloudSettings = new Gson().fromJson(s, LitresCloudSettings.class);
			return litresCloudSettings;
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
				intent.setPackage(FlavourConstants.MAIN_CLASS_NAME);
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
				intent.setPackage(FlavourConstants.MAIN_CLASS_NAME);
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
				intent.setPackage(FlavourConstants.MAIN_CLASS_NAME);
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
				intent.setPackage(FlavourConstants.MAIN_CLASS_NAME);
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
				intent.setPackage(FlavourConstants.MAIN_CLASS_NAME);
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
		geoLastData.createGeoListener(settings());
	}

	public boolean checkLocationPermission() {
		return geoLastData.checkLocationPermission(this);
	}

	public boolean isNetworkAvailable() {
		ConnectivityManager manager =
				(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		boolean isAvailable = false;
		if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
			// Network is present and connected
			isAvailable = true;
		}
		return isAvailable;
	}

//	public boolean initSDCV() {
//		File f = new File(getSettingsFileF(0).getParent() + "/sdcv");
//		if (!f.exists()) {
//			InputStream targetStream = getResources().openRawResource(R.raw.sdcv);
//			if (targetStream != null) {
//				try {
//					Utils.saveStreamToFile(targetStream, getSettingsFileF(0).getParent() + "/sdcv");
//				} catch (Exception e) {
//					return false;
//				}
//				return true;
//			}
//		}
//		return false;
//	}

	public void showInputDialog(final String title, final String prompt, final boolean isNumberEdit, final int minValue, final int maxValue, final int lastValue, final InputDialog.InputHandler handler) {
		BackgroundThread.instance().executeGUI(() -> {
			final InputDialog dlg = new InputDialog(this, title, prompt, isNumberEdit, minValue, maxValue, lastValue, handler);
			dlg.show();
		});
	}

	public void showFilterDialog() {
		showInputDialog(this.getString(R.string.mi_filter_option), this.getString(R.string.mi_filter_option),
				false,
				1, 2, 3,
				new InputDialog.InputHandler() {
					@Override
					public boolean validate(String s) {
						return true;
					}
					@Override
					public boolean validateNoCancel(String s) {
						return true;
					}
					@Override
					public void onOk(String s) {
						optionsFilter = s;
						showOptionsDialog(OptionsDialog.Mode.READER);
					}
					@Override
					public void onCancel() {
					}
				});
	}

}

