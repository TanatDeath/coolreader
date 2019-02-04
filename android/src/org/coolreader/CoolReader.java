// Main Class
package org.coolreader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;

import org.coolreader.Dictionaries.DictionaryException;
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
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.ErrorDialog;
import org.coolreader.crengine.FileBrowser;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.GenreSAXElem;
import org.coolreader.crengine.GoogleDriveTools;
import org.coolreader.crengine.History;
import org.coolreader.crengine.History.BookInfoLoadedCallack;
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
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.TTS;
import org.coolreader.crengine.TTS.OnTTSCreatedListener;
import org.coolreader.crengine.UserDicEntry;
import org.coolreader.crengine.Utils;
import org.coolreader.db.CRDBService;
import org.coolreader.donations.CRDonationService;
import org.koekak.android.ebookdownloader.SonyBookSelector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
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
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.provider.DocumentFile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.provider.Settings.Secure;

import com.google.android.gms.common.util.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CoolReader extends BaseActivity implements SensorEventListener
{
	public static final Logger log = L.create("cr");

	public static int REQUEST_CODE_OPEN_DOCUMENT_TREE = 200001;
	public Uri sdCardUri = null;
	private FileInfo fileToDelete = null;
	
	private ReaderView mReaderView;

    public class ResizeHistory {
        public int X;
        public int Y;
        public long lastSet;
        public int wasX;
        public int wasY;
        public int cnt;
    }

    public ArrayList<ResizeHistory> getResizeHist() {
        return resizeHist;
    }

	public ResizeHistory getNewResizeHistory() {
		ResizeHistory rh = new ResizeHistory();
    	return rh;
	}

	public void setResizeHist(ArrayList<ResizeHistory> resizeHist) {
		this.resizeHist = resizeHist;
	}

	private ArrayList<ResizeHistory> resizeHist = new ArrayList<ResizeHistory>();

	Sensor accelerometer;
	Sensor magnetometer;
	Sensor vectorSensor;
	DeviceOrientation deviceOrientation;
	SensorManager mSensorManager;

	public int sensorPrevRot = -1;
	public int sensorCurRot = -1;

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

	private ReaderViewLayout mReaderFrame;
	private FileBrowser mBrowser;
	private View mBrowserTitleBar;
	private CRToolBar mBrowserToolBar;
	private BrowserViewLayout mBrowserFrame;
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
	private ViewGroup mCurrentFrame;
	private ViewGroup mPreviousFrame;

	private String mOptionAppearance = "0";

	String fileToLoadOnStart = null;
	
	private boolean isFirstStart = true;
	private boolean phoneStateChangeHandlerInstalled = false;
	int initialBatteryState = -1;
	BroadcastReceiver intentReceiver;

	private boolean justCreated = false;

	private boolean dataDirIsRemoved = false;

	private static final int PERM_REQUEST_STORAGE_CODE = 1;
	private static final int PERM_REQUEST_READ_PHONE_STATE_CODE = 2;

	public GoogleDriveTools mGoogleDriveTools = null;

	public String getAndroid_id() {
		return android_id;
	}

	private String android_id = "";

	public String getModel() {
		return mModel;
	}

	private String mModel = "";

	public boolean skipFindInDic = false; // skip find in dic when bookmark toast is shown

	public String optionsFilter = "font"; //filter used for options

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	startServices();
		log.i("CoolReader.onCreate() entered");
		super.onCreate(savedInstanceState);

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
				if ( mReaderView!=null )
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
		// Donations related code
		try {
			
			mDonationService = new CRDonationService(this);
			mDonationService.bind();
    		SharedPreferences pref = getSharedPreferences(DONATIONS_PREF_FILE, 0);
    		try {
    			mTotalDonations = pref.getFloat(DONATIONS_PREF_TOTAL_AMOUNT, 0.0f);
    		} catch (Exception e) {
    			log.e("exception while reading total donations from preferences", e);
    		}
		} catch (VerifyError e) {
			log.e("Exception while trying to initialize billing service for donations");
		}

		N2EpdController.n2MainActivity = this;

		mGoogleDriveTools = new GoogleDriveTools(this);
		android_id = Secure.getString(getApplicationContext().getContentResolver(),
				Secure.ANDROID_ID);

		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			mModel = Strings.capitalize(model);
		} else {
			mModel = Strings.capitalize(manufacturer) + " " + model;
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
        log.i("CoolReader.onCreate() exiting");
    }

	public final static boolean CLOSE_BOOK_ON_STOP = false;
	
    boolean mDestroyed = false;
	@Override
	protected void onDestroy() {

		log.i("CoolReader.onDestroy() entered");
		if (!CLOSE_BOOK_ON_STOP && mReaderView != null)
			mReaderView.close();

		if ( tts!=null ) {
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
		if ( intentReceiver!=null ) {
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
        if ( key.equals(PROP_APP_DICTIONARY) ) {
        	setDict(value);
        } else if ( key.equals(PROP_APP_DICTIONARY_2) ) {
			setDict2(value);
		} else if ( key.equals(PROP_APP_DICT_WORD_CORRECTION) ) {
			setDictWordCorrection(value);
		} else if ( key.equals(PROP_APP_SHOW_USER_DIC_PANEL) ) {
			setShowUserDicPanel(value);
		} else if ( key.equals(PROP_APP_DICT_LONGTAP_CHANGE) ) {
			setDictLongtapChange(value);
		} else if ( key.equals(PROP_TOOLBAR_APPEARANCE) ) {
			setToolbarAppearance(value);
	    } else if (key.equals(PROP_APP_BOOK_SORT_ORDER)) {
        	if (mBrowser != null)
        		mBrowser.setSortOrder(value);
        } else if ( key.equals(PROP_APP_SHOW_COVERPAGES) ) {
        	if (mBrowser != null)
        		mBrowser.setCoverPagesEnabled(flg);
        } else if ( key.equals(PROP_APP_BOOK_PROPERTY_SCAN_ENABLED) ) {
        	Services.getScanner().setDirScanEnabled(flg);
        } else if ( key.equals(PROP_FONT_FACE) ) {
        	if (mBrowser != null)
        		mBrowser.setCoverPageFontFace(value);
        } else if ( key.equals(PROP_APP_COVERPAGE_SIZE) ) {
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
        } else if ( key.equals(PROP_APP_FILE_BROWSER_SIMPLE_MODE) ) {
        	if (mBrowser != null)
        		mBrowser.setSimpleViewMode(flg);
        }
        //
	}
	
	@Override
	public void setFullscreen( boolean fullscreen )
	{
		super.setFullscreen(fullscreen);
		if (mReaderFrame != null)
			mReaderFrame.updateFullscreen(fullscreen);
	}
	
	private String extractFileName( Uri uri )
	{
		if ( uri!=null ) {
			if ( uri.equals(Uri.parse("file:///")) )
				return null;
			else
				return uri.getPath();
		}
		return null;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		log.i("onNewIntent : " + intent);
		if ( mDestroyed ) {
			log.e("engine is already destroyed");
			return;
		}
		processIntent(intent);
	}

	private boolean processIntent(Intent intent) {
		log.d("intent=" + intent);
		if (intent == null)
			return false;
		String fileToOpen = null;
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Uri uri = intent.getData();
			intent.setData(null);
			if (uri != null) {
				fileToOpen = uri.getPath();
//				if (fileToOpen.startsWith("file://"))
//					fileToOpen = fileToOpen.substring("file://".length());
			}
		}
		if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
			if (intent.getType().startsWith("image/")) {
				ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				if (imageUris != null) {
					showToast("Not implemented yet");
				}
			}
		}
		if (Intent.ACTION_SEND.equals(intent.getAction())) {
			if (intent.getType().startsWith("image/")) {
				Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
				if (imageUri != null) {
					PictureCameDialog dlg = new PictureCameDialog(CoolReader.this, imageUri, intent.getType());
					dlg.show();
				}
			} else {
				fileToOpen = intent.getStringExtra(Intent.EXTRA_SUBJECT);
				if (fileToOpen != null) {
					if (fileToOpen.equals(FileInfo.RECENT_DIR_TAG)) {
						this.showRecentBooks();
						return true;
					}
					if (fileToOpen.equals(FileInfo.STATE_READING_TAG)) {
						final FileInfo dir = new FileInfo();
						dir.isDirectory = true;
						dir.pathname = fileToOpen;
						dir.filename = this.getString(R.string.folder_name_books_by_state_reading);
						dir.isListed = true;
						dir.isScanned = true;
						waitForCRDBService(new Runnable() {
							@Override
							public void run() {
								showDirectory(dir);
							}
						});
						return true;
					}
					if (fileToOpen.equals(FileInfo.STATE_TO_READ_TAG)) {
						final FileInfo dir = new FileInfo();
						dir.isDirectory = true;
						dir.pathname = fileToOpen;
						dir.filename = this.getString(R.string.folder_name_books_by_state_to_read);
						dir.isListed = true;
						dir.isScanned = true;
						waitForCRDBService(new Runnable() {
							@Override
							public void run() {
								showDirectory(dir);
							}
						});
						return true;
					}
					if (fileToOpen.equals(FileInfo.STATE_FINISHED_TAG)) {
						final FileInfo dir = new FileInfo();
						dir.isDirectory = true;
						dir.pathname = fileToOpen;
						dir.filename = this.getString(R.string.folder_name_books_by_state_finished);
						dir.isListed = true;
						dir.isScanned = true;
						waitForCRDBService(new Runnable() {
							@Override
							public void run() {
								showDirectory(dir);
							}
						});
						return true;
					}
					if (fileToOpen.equals(FileInfo.SEARCH_SHORTCUT_TAG)) {
						final FileInfo dir = new FileInfo();
						dir.isDirectory = true;
						dir.pathname = fileToOpen;
						dir.filename = this.getString(R.string.dlg_book_search);
						dir.isListed = true;
						dir.isScanned = true;
						waitForCRDBService(new Runnable() {
							@Override
							public void run() {
								showDirectory(dir);
							}
						});
						return true;
					}
				}
				if (StrUtils.isEmptyStr(fileToOpen)) {
					//showToast(intent.getType());
					String sText = intent.getStringExtra(Intent.EXTRA_TEXT);
					if (!StrUtils.isEmptyStr(sText)) {
						if (sText.toLowerCase().startsWith("http"))
							showToast(R.string.warn_http);
						else
							showToast(R.string.warn_empty_file_name);
					}
					return true;
				}
			}
		}

		if (fileToOpen == null && intent.getExtras() != null) {
			log.d("extras=" + intent.getExtras());
			fileToOpen = intent.getExtras().getString(OPEN_FILE_PARAM);
		}
		if (fileToOpen != null) {
			// patch for opening of books from ReLaunch (under Nook Simple Touch) 
			while (fileToOpen.indexOf("%2F") >= 0) {
				fileToOpen = fileToOpen.replace("%2F", "/");
			}
			log.d("FILE_TO_OPEN = " + fileToOpen);
			final String finalFileToOpen = fileToOpen;
			// image handling
			if (
					(fileToOpen.toLowerCase().endsWith(".jpg"))||
							(fileToOpen.toLowerCase().endsWith(".jpeg"))||
							(fileToOpen.toLowerCase().endsWith(".png"))
					) pictureCame(fileToOpen);
			else loadDocument(fileToOpen, new Runnable() {
				@Override
				public void run() {
					BackgroundThread.instance().postGUI(new Runnable() {
						@Override
						public void run() {
							// if document not loaded show error & then root window
							ErrorDialog errDialog = new ErrorDialog(CoolReader.this, CoolReader.this.getString(R.string.error), CoolReader.this.getString(R.string.cant_open_file, finalFileToOpen));
							errDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
								@Override
								public void onDismiss(DialogInterface dialog) {
									showRootWindow();
								}
							});
							errDialog.show();
						}
					}, 500);
				}
			});
			return true;
		} else {
			log.d("No file to open");
			return false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(deviceOrientation.getEventListener());
        mSensorManager.unregisterListener(this);
		if (mReaderView != null)
			mReaderView.onAppPause();
		Services.getCoverpageManager().removeCoverpageReadyListener(mHomeFrame);
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
		log.i("CoolReader.onResume()");
		super.onResume();

		mSensorManager.registerListener(deviceOrientation.getEventListener(), accelerometer, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(deviceOrientation.getEventListener(), magnetometer, SensorManager.SENSOR_DELAY_UI);

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
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		log.i("CoolReader.onSaveInstanceState()");
		super.onSaveInstanceState(outState);
	}

	static final boolean LOAD_LAST_DOCUMENT_ON_START = true; 
	
	@Override
	protected void onStart() {
		log.i("CoolReader.onStart() version=" + getVersion() + ", fileToLoadOnStart=" + fileToLoadOnStart);
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

		if (mHomeFrame == null) {
			waitForCRDBService(new Runnable() {
				@Override
				public void run() {
					Services.getHistory().loadFromDB(getDB(), 200);
					
					mHomeFrame = new CRRootView(CoolReader.this);
					Services.getCoverpageManager().addCoverpageReadyListener(mHomeFrame);
					mHomeFrame.requestFocus();
					
					showRootWindow();
					setSystemUiVisibility();
					
					notifySettingsChanged();
					
					showNotifications();
				}
			});
		}

		if (mUserDic == null) {
			waitForCRDBService(new Runnable() {
				@Override
				public void run() {
					getDB().loadUserDic(new CRDBService.UserDicLoadingCallback() {
							@Override
							public void onUserDicLoaded(HashMap<String, UserDicEntry> list) {
								mUserDic = list;
							}
						});
				}
			});
		}

		if ( isBookOpened() ) {
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
			ErrorDialog dlg = new ErrorDialog(this, getString(R.string.error), getString(R.string.datadir_is_removed, Engine.getExternalSettingsDirName()));
			dlg.show();
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
		if ( CLOSE_BOOK_ON_STOP )
			mReaderView.close();

		
		log.i("CoolReader.onStop() exiting");
	}

	private void requestStoragePermissions() {
		// check or request permission for storage
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int readExtStoragePermissionCheck = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
			int writeExtStoragePermissionCheck = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			ArrayList<String> needPerms = new ArrayList<String>();
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
				log.i("Some permissions DENIED, requesting from user these permissions: " + needPerms.toString());
				// request permission from user
				requestPermissions(needPerms.toArray(templ), PERM_REQUEST_STORAGE_CODE);
			}
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
				requestPermissions(new String[] { Manifest.permission.READ_PHONE_STATE } , PERM_REQUEST_READ_PHONE_STATE_CODE);
			} else {
				log.i("READ_PHONE_STATE permission already granted.");
			}
		}
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		log.i("CoolReader.onRequestPermissionsResult()");
		if (PERM_REQUEST_STORAGE_CODE == requestCode) {		// external storage read & write permissions
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
				rebaseSettings();
				waitForCRDBService(new Runnable() {
					@Override
					public void run() {
						getDBService().setPathCorrector(Engine.getInstance(CoolReader.this).getPathCorrector());
						getDB().reopenDatabase();
						Services.getHistory().loadFromDB(getDB(), 200);
					}
				});
				mHomeFrame.refreshView();
			}
			if (Engine.getExternalSettingsDirName() != null) {
				setExtDataDirCreateTime(new Date());
			} else {
				setExtDataDirCreateTime(null);
			}
		} else if (PERM_REQUEST_READ_PHONE_STATE_CODE == requestCode) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				log.i("read phone state permission is GRANTED, registering phone activity handler...");
				PhoneStateReceiver.setPhoneActivityHandler(new Runnable() {
					@Override
					public void run() {
						if (mReaderView != null) {
							mReaderView.stopTTS();
							mReaderView.save();
						}
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
				if ( buf.length()>0 )
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
				mHomeFrame.refreshOnlineCatalogs();
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
		Properties changedProps = oldProps!=null ? props.diff(oldProps) : props;
		if (mHomeFrame != null) {
			mHomeFrame.refreshOnlineCatalogs();
		}
		if (mReaderFrame != null) {
			mReaderFrame.updateSettings(props);
			if (mReaderView != null)
				mReaderView.updateSettings(props);
		}
        for ( Map.Entry<Object, Object> entry : changedProps.entrySet() ) {
    		String key = (String)entry.getKey();
    		String value = (String)entry.getValue();
    		applyAppSetting( key, value );
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

	private void setCurrentFrame(ViewGroup newFrame) {
        if (mCurrentFrame != newFrame) {
			mPreviousFrame = mCurrentFrame;
			log.i("New current frame: " + newFrame.getClass().toString());
			mCurrentFrame = newFrame;
			setContentView(mCurrentFrame);
			mCurrentFrame.requestFocus();
			if (mCurrentFrame != mReaderFrame)
				releaseBacklightControl();
			if (mCurrentFrame == mHomeFrame) {
				// update recent books
				mHomeFrame.refreshRecentBooks();
				mHomeFrame.refreshFileSystemFolders();
				setLastLocationRoot();
				mCurrentFrame.invalidate();
			}
			if (mCurrentFrame == mBrowserFrame) {
				// update recent books directory
				mBrowser.refreshDirectory(Services.getScanner().getRecentDir(), null);
			}
			onUserActivity();
		}
	}
	
	public void showReader() {
		runInReader(new Runnable() {
			@Override
			public void run() {
				// do nothing
			}
		});
	}
	
	public void showRootWindow() {
		setCurrentFrame(mHomeFrame);
	}
	
	private void runInReader(final Runnable task) {
		waitForCRDBService(new Runnable() {
			@Override
			public void run() {
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
			        mReaderFrame.getToolBar().setOnActionHandler(new OnActionHandler() {
						@Override
						public boolean onActionSelected(ReaderAction item) {
							if (mReaderView != null)
								mReaderView.onAction(item);
							return true;
						}
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
			}
		});
		
	}
	
	public boolean isBrowserCreated() {
		return mBrowserFrame != null;
	}
	
	private void runInBrowser(final Runnable task) {
		waitForCRDBService(new Runnable() {
			@Override
			public void run() {
				if (mBrowserFrame != null) {
					task.run();
					setCurrentFrame(mBrowserFrame);
				} else {
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
					
					task.run();
					setCurrentFrame(mBrowserFrame);

//					if (getIntent() == null)
//						mBrowser.showDirectory(Services.getScanner().getDownloadDirectory(), null);
				}
			}
		});
		
	}
	
	public void showBrowser() {
		runInBrowser(new Runnable() {
			@Override
			public void run() {
				// do nothing, browser is shown
			}
		});
	}
	
	public void showManual() {
		loadDocument("@manual", null);
	}
	
	public static final String OPEN_FILE_PARAM = "FILE_TO_OPEN";
	public void loadDocument(final String item, final Runnable callback)
	{
		runInReader(new Runnable() {
			@Override
			public void run() {
				mReaderView.loadDocument(item, callback);
			}
		});
	}

	public void pictureCame(final String item) {
		PictureCameDialog dlg = new PictureCameDialog(CoolReader.this, item, "");
		dlg.show();
	}

	public void loadDocument( FileInfo item )
	{
		loadDocument(item, null);
	}

	public void loadDocument( FileInfo item, Runnable callback )
	{
		log.d("Activities.loadDocument(" + item.pathname + ")");
		loadDocument(item.getPathName(), callback);
	}

	public void showOpenedBook()
	{
		showReader();
	}
	
	public static final String OPEN_DIR_PARAM = "DIR_TO_OPEN";
	public void showBrowser(final FileInfo dir) {
		runInBrowser(new Runnable() {
			@Override
			public void run() {
				mBrowser.showDirectory(dir, null);
			}
		});
	}
	
	public void showBrowser(final String dir) {
		runInBrowser(new Runnable() {
			@Override
			public void run() {
				mBrowser.showDirectory(Services.getScanner().pathToFileInfo(dir), null);
			}
		});
	}
	
	public void showRecentBooks() {
		log.d("Activities.showRecentBooks() is called");
		runInBrowser(new Runnable() {
			@Override
			public void run() {
				mBrowser.showRecentBooks();
			}
		});
	}

	public void showOnlineCatalogs() {
		log.d("Activities.showOnlineCatalogs() is called");
		runInBrowser(new Runnable() {
			@Override
			public void run() {
				mBrowser.showOPDSRootDirectory();
			}
		});
	}

	public void showDirectory(FileInfo path) {
		log.d("Activities.showDirectory(" + path + ") is called");
		showBrowser(path);
	}

	public void showCatalog(final FileInfo path) {
		log.d("Activities.showCatalog(" + path + ") is called");
		runInBrowser(new Runnable() {
			@Override
			public void run() {
				mBrowser.showDirectory(path, null);
			}
		});
	}

	
	
	public void setBrowserTitle(String title, FileInfo dir) {
		if (mBrowserFrame != null)
			mBrowserFrame.setBrowserTitle(title, dir);
	}
	

	
	// Dictionary support
	
	
	public void findInDictionary( String s ) {
		if ( s!=null && s.length()!=0 ) {
			int start,end;
			
			// Skip over non-letter characters at the beginning and end of the search string
			for (start = 0 ;start<s.length(); start++)
				if (Character.isLetterOrDigit(s.charAt(start)))
 					break;
			for (end=s.length()-1; end>=start; end--)
				if (Character.isLetterOrDigit(s.charAt(end)))
 					break;

			if ( end > start ) {
    			final String pattern = s.substring(start,end+1);

				BackgroundThread.instance().postBackground(new Runnable() {
					@Override
					public void run() {
						BackgroundThread.instance().postGUI(new Runnable() {
							@Override
							public void run() {
								findInDictionaryInternal(pattern);
							}
						}, 100);
					}
				});
			}
		}
	}
	
	private void findInDictionaryInternal(String s) {
		log.d("lookup in dictionary: " + s);
		try {
			mDictionaries.findInDictionary(s);
		} catch (DictionaryException e) {
			showToast(e.getMessage());
		}
	}

	public void showDictionary() {
		findInDictionaryInternal(null);
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

        if (mGoogleDriveTools != null) {
			mGoogleDriveTools.onActivityResult(requestCode, resultCode, intent);
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
						BackgroundThread.instance().postGUI(new Runnable() {
							@Override
							public void run() {
								directoryUpdated(fileToDelete.parent, null);
							}}, 700);
					} // if (file!=null)
				}
			}
		} //if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE)
	}
	
	public void setDict( String id ) {
		mDictionaries.setDict(id);
	}

	public void setDict2( String id ) {
		mDictionaries.setDict2(id);
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
    	BackgroundThread.instance().postBackground(new Runnable() {
			@Override
			public void run() {
		        mDonationService.purchase(itemName, 
	        		new CRDonationService.PurchaseListener() {
						@Override
						public void onPurchaseCompleted(final boolean success, final String productId,
								final float totalDonations) {
							BackgroundThread.instance().postGUI(new Runnable() {
								@Override
								public void run() {
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
								}
							});
						}
				});
			}
    	});
    	return true;
    }
    
	private static String DONATIONS_PREF_FILE = "cr3donations";
	private static String DONATIONS_PREF_TOTAL_AMOUNT = "total";


    // ========================================================================================
    // TTS
	public TTS tts;
	public boolean ttsInitialized;
	public boolean ttsError;
	
	public boolean initTTS(final OnTTSCreatedListener listener) {
		if ( ttsError || !TTS.isFound() ) {
			if ( !ttsError ) {
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
				PhoneStateReceiver.setPhoneActivityHandler(new Runnable() {
					@Override
					public void run() {
						if (mReaderView != null) {
							mReaderView.stopTTS();
							mReaderView.save();
						}
					}
				});
				phoneStateChangeHandlerInstalled = true;
			}
		}

		// here we will try to reinitialize
		if ( ttsInitialized && tts!=null ) {
			BackgroundThread.instance().executeGUI(new Runnable() {
				@Override
				public void run() {
					listener.onCreated(tts);
				}
			});
			return true;
		}

		if ( ttsInitialized && tts!=null ) {
			showToast("TTS initialization is already called");
			return false;
		}
		showToast("Initializing TTS");
    	tts = new TTS(this, new TTS.OnInitListener() {
			@Override
			public void onInit(int status) {
				//tts.shutdown();
				L.i("TTS init status: " + status);
				if ( status==TTS.SUCCESS ) {
					ttsInitialized = true;
					BackgroundThread.instance().executeGUI(new Runnable() {
						@Override
						public void run() {
							listener.onCreated(tts);
						}
					});
				} else {
					ttsError = true;
					BackgroundThread.instance().executeGUI(new Runnable() {
						@Override
						public void run() {
							showToast("Cannot initialize TTS");
						}
					});
				}
			}
		});
		return true;
	}
	

    // ============================================================
	private AudioManager am;
	private int maxVolume;
	public AudioManager getAudioManager() {
		if ( am==null ) {
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
		BackgroundThread.instance().postBackground(new Runnable() {
			public void run() {
				final String[] mFontFaces = Engine.getFontFaceList();
				final String[] mFontFacesFiles = Engine.getFontFaceAndFileNameList();
				BackgroundThread.instance().executeGUI(new Runnable() {
					public void run() {
						OptionsDialog dlg = new OptionsDialog(CoolReader.this, mReaderView, mFontFaces, mFontFacesFiles, mode);
						dlg.show();
					}
				});
			}
		});
	}

	public void showOptionsDialogExt(final OptionsDialog.Mode mode, final String selectOption)
	{
		BackgroundThread.instance().postBackground(new Runnable() {
			public void run() {
				final String[] mFontFaces = Engine.getFontFaceList();
				final String[] mFontFacesFiles = Engine.getFontFaceAndFileNameList();
				BackgroundThread.instance().executeGUI(new Runnable() {
					public void run() {
						OptionsDialog dlg = new OptionsDialog(CoolReader.this, mReaderView, mFontFaces, mFontFacesFiles, mode);
						dlg.selectedOption = selectOption;
						dlg.show();
					}
				});
			}
		});
	}
	
	public void updateCurrentPositionStatus(FileInfo book, Bookmark position, PositionProperties props) {
		mReaderFrame.getStatusBar().updateCurrentPositionStatus(book, position, props);
		mReaderFrame.getUserDicPanel().updateCurrentPositionStatus(book, position, props);
		//showToast("sd "+props.pageNumber);
		//showToast("sdc "+props.pageCount);
		if ((!book.askedMarkRead)&&(book.getReadingState()!=FileInfo.STATE_FINISHED) && (props.pageNumber+1==props.pageCount)) {
			book.askedMarkRead = true;
			final FileInfo book1=book;
			askConfirmation(R.string.mark_book_as_read, new Runnable() {
				@Override
				public void run() {
					Services.getHistory().getOrCreateBookInfo(getDB(), book1, new History.BookInfoLoadedCallack() {
						@Override
						public void onBookInfoLoaded(BookInfo bookInfo) {
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
						}
					});
				}
			});
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
			if (book.arrReadBeg.size()>pagesCnt) {
				book.askedMarkReading = true;
				final FileInfo book1=book;
				askConfirmation(R.string.mark_book_as_reading, new Runnable() {
					@Override
					public void run() {
						Services.getHistory().getOrCreateBookInfo(getDB(), book1, new History.BookInfoLoadedCallack() {
							@Override
							public void onBookInfoLoaded(BookInfo bookInfo) {
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
									showBrowser(!isBrowserCreated() ? getReaderView().getOpenedFileInfo() : null);
							}
						});
					}
				},  new Runnable() {
					@Override
					public void run() {
						if (openBrowser)
							showBrowser(!isBrowserCreated() ? getReaderView().getOpenedFileInfo() : null);
					}
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
		BackgroundThread.instance().executeGUI(new Runnable() {
			@Override
			public void run() {
				BookmarksDlg dlg = new BookmarksDlg(CoolReader.this, mReaderView, bOnlyChoose, obj);
				dlg.show();
			}
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
		askConfirmation(R.string.win_title_confirm_book_delete, new Runnable() {
			@Override
			public void run() {
				closeBookIfOpened(item);
				FileInfo file = Services.getScanner().findFileInTree(item);
				if (file == null)
					file = item;
				final FileInfo finalFile = file;
 				if (file.deleteFile()) {
					waitForCRDBService(new Runnable() {
						@Override
						public void run() {
						Services.getHistory().removeBookInfo(getDB(), finalFile, true, true);
					BackgroundThread.instance().postGUI(new Runnable() {
								@Override
								public void run() {
							directoryUpdated(item.parent, null);
						}}, 700);
					}
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
						showToast("Could not delete file, if the file is on External SD, choose ExtSD root in the dialog");
						if (file.getFile() != null) {
							fileToDelete = file;
							Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
							startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);
						}
					} else {
						waitForCRDBService(new Runnable() {
							@Override
							public void run() {
								Services.getHistory().removeBookInfo(getDB(), finalFile, true, true);
								BackgroundThread.instance().postGUI(new Runnable() {
									@Override
									public void run() {
										directoryUpdated(item.parent, null);
									}}, 700);
							}
						});
					}
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
		askConfirmation(R.string.win_title_confirm_history_record_delete, new Runnable() {
			@Override
			public void run() {
				waitForCRDBService(new Runnable() {
					@Override
					public void run() {
						Services.getHistory().removeBookInfo(getDB(), item, true, false);
						directoryUpdated(Services.getScanner().createRecentRoot());
					}
				});
			}
		});
	}
	
	public void askDeleteCatalog(final FileInfo item)
	{
		askConfirmation(R.string.win_title_confirm_catalog_delete, new Runnable() {
			@Override
			public void run() {
				if (item != null && item.isOPDSDir()) {
					waitForCRDBService(new Runnable() {
						@Override
						public void run() {
							getDB().removeOPDSCatalog(item.id);
							directoryUpdated(Services.getScanner().createRecentRoot());
						}
					});
				}
			}
		});
	}
	
	public void saveSetting(String name, String value) {
		if (mReaderView != null)
			mReaderView.saveSetting(name, value);
	}
	
	public void editBookInfo(final FileInfo currDirectory, final FileInfo item) {
		waitForCRDBService(new Runnable() {
			@Override
			public void run() {
				Services.getHistory().getOrCreateBookInfo(getDB(), item, new BookInfoLoadedCallack() {
					@Override
					public void onBookInfoLoaded(BookInfo bookInfo) {
						if (bookInfo == null)
							bookInfo = new BookInfo(item);
						BookInfoEditDialog dlg = new BookInfoEditDialog(CoolReader.this, currDirectory, bookInfo,
								currDirectory.isRecentDir());
						dlg.show();
					}
				});
			}
		});
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
		final ArrayList<String> itemsAll = new ArrayList<String>();
		itemsAll.add("section=section.file");
		itemsAll.add("file.format=" + item.format.name());
		itemsAll.add("file.name=" + item.pathname);
		itemsAll.add("section=section.book");
		if (!StrUtils.isEmptyStr(item.authors))
			itemsAll.add("book.authors="+item.getAuthors());
		itemsAll.add("book.title="+item.title);
		BookInfoDialog dlg = new BookInfoDialog(this, itemsAll, null, item.filename,
				BookInfoDialog.OPDS_INFO, item, fb, currDir);
		dlg.show();
	}

	public void showBookInfo(BookInfo setBI, final int actionType, final FileInfo currDir) {
		final ArrayList<String> itemsAll = new ArrayList<String>();
		final ArrayList<String> itemsSys = new ArrayList<String>();
		final ArrayList<String> itemsFile = new ArrayList<String>();
		final ArrayList<String> itemsPos = new ArrayList<String>();
		final ArrayList<String> itemsBook = new ArrayList<String>();
		itemsSys.add("section=section.system");
		itemsSys.add("system.version=Cool Reader " + getVersion());
		if (getReaderView()!=null)
			itemsSys.add("system.battery=" + getReaderView().getmBatteryState() + "%");
		itemsSys.add("system.time=" + Utils.formatTime(this, System.currentTimeMillis()));
		if ((getReaderView()!=null)&&(getReaderView().getLastsetWidth()!=0)&&(getReaderView().getLastsetHeight()!=0))
			itemsSys.add("system.resolution="+
					"last requested ("+
					Utils.formatTime(this,getReaderView().getRequestedResTime())+"): "+
						getReaderView().getRequestedWidth()+" x "+getReaderView().getRequestedHeight()+
					"; last set ("+Utils.formatTime(this,getReaderView().getLastsetResTime())+"): "+
					+getReaderView().getLastsetWidth()+" x "+getReaderView().getLastsetHeight());
		itemsSys.add("system.device_model=" + DeviceInfo.MANUFACTURER + " / "+DeviceInfo.MODEL+" / "+
				DeviceInfo.DEVICE+ " / "+DeviceInfo.PRODUCT + " / " + DeviceInfo.BRAND);
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
		itemsSys.add("system.device_flags=" + sDevFlags);

		final BookInfo bi = setBI;
		if ( bi!=null ) {
			FileInfo fi = bi.getFileInfo();
			itemsFile.add("section=section.file");
			String fname = new File(fi.pathname).getName();
			itemsFile.add("file.name=" + fname);
			if ( new File(fi.pathname).getParent()!=null )
				itemsFile.add("file.path=" + new File(fi.pathname).getParent());
			itemsFile.add("file.size=" + StrUtils.readableFileSize(fi.size));
			if ( fi.arcname!=null ) {
				itemsFile.add("file.arcname=" + new File(fi.arcname).getName());
				if ( new File(fi.arcname).getParent()!=null )
					itemsFile.add("file.arcpath=" + new File(fi.arcname).getParent());
				itemsFile.add("file.arcsize=" + StrUtils.readableFileSize(fi.arcsize));
			}
			itemsFile.add("file.format=" + fi.format.name());
			if (!StrUtils.isEmptyStr(fi.opdsLink)) {
				itemsBook.add("file.opds_link=" + fi.opdsLink);
			}
		}
		execute( new Task() {
			Bookmark bm;
			@Override
			public void work() {
				if (getReaderView()!=null)
					if (getReaderView().getDoc()!=null)
						bm =  getReaderView().getDoc().getCurrentPageBookmark();
				if ( bm!=null ) {
					PositionProperties prop = getReaderView().getDoc().getPositionProps(bm.getStartPos());
					itemsPos.add("section=section.position");
					if ( prop.pageMode!=0 ) {
						itemsPos.add("position.page=" + (prop.pageNumber+1) + " / " + prop.pageCount);
					}
					int percent = (int)(10000 * (long)prop.y / prop.fullHeight);
					itemsPos.add("position.percent=" + (percent/100) + "." + (percent%100) + "%" );
					String chapter = bm.getTitleText();
					if ( chapter!=null && chapter.length()>100 )
						chapter = chapter.substring(0, 100) + "...";
					itemsPos.add("position.chapter=" + chapter);
				}
			}
			public void done() {
				FileInfo fi = bi.getFileInfo();
				if ((fi!=null)&&(mReaderView!=null))
					if ((fi.symCount==0)||(fi.wordCount==0)) {
						int iPageCnt = 0;
						int iSymCnt = 0;
						int iWordCnt = 0;
						if (mReaderView.getArrAllPages()!=null)
							iPageCnt = mReaderView.getArrAllPages().size();
						else {
							mReaderView.CheckAllPagesLoadVisual();
							iPageCnt = mReaderView.getArrAllPages().size();
						}
						for (int i=0;i<iPageCnt;i++) {
							String sPage = mReaderView.getArrAllPages().get(i);
							if (sPage == null) sPage = "";
							sPage = sPage.replace("\\n", " ");
							sPage = sPage.replace("\\r", " ");
							iSymCnt=iSymCnt + sPage.replaceAll("\\s+"," ").length();
							iWordCnt=iWordCnt + sPage.replaceAll("\\p{Punct}", " ").
									replaceAll("\\s+"," ").split("\\s").length;
						}
						fi.symCount = iSymCnt;
						fi.wordCount = iWordCnt;
						BookInfo bi = new BookInfo(fi);
						getDB().saveBookInfo(bi);
						getDB().flush();
					}
				itemsBook.add("section=section.book");
				if ( fi.getAuthors()!=null || fi.title!=null || fi.series!=null) {
					if (!StrUtils.isEmptyStr(fi.getAuthors())) itemsBook.add("book.authors=" + fi.getAuthors().
							replaceAll("\\|","; "));
					if (!StrUtils.isEmptyStr(fi.title)) itemsBook.add("book.title=" + fi.title);
					if ( fi.series!=null ) {
						String s = fi.series;
						if ( fi.seriesNumber>0 )
							s = s + " #" + fi.seriesNumber;
						itemsBook.add("book.series=" + s);
					}
				}
				if (!StrUtils.isEmptyStr(fi.getBookdate())) itemsBook.add("book.date=" + fi.getBookdate());
				if (!StrUtils.isEmptyStr(fi.language)) {
					itemsBook.add("book.language=" + fi.language);
				}
				String genreText = "";
				if (!StrUtils.isEmptyStr(fi.genre)) {
					// lets try to get out genre name
					GenreSAXElem ge = null;
					String genreDescr = "";
					if (!StrUtils.isEmptyStr(fi.genre)) {
						String [] arrGenre = fi.genre.split("\\|");
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
						itemsBook.add("book.genre=" + genreText);
				}
				String annot = "";
				if (!StrUtils.isEmptyStr(fi.annotation)) {
					annot = fi.annotation;
				}
				if (!StrUtils.isEmptyStr(fi.srclang)) {
					itemsBook.add("book.srclang=" + fi.srclang);
				}
				if (!StrUtils.isEmptyStr(fi.translator)) {
					itemsBook.add("book.translator=" + fi.translator);
				}
				if (fi.symCount!=0) {
					itemsBook.add("book.symcount=" + fi.symCount);
				}
				if (fi.symCount!=0) {
					itemsBook.add("book.wordcount=" + fi.wordCount);
				}
				itemsBook.add("section=section.book_document");
				if (!StrUtils.isEmptyStr(fi.docauthor)) {
					itemsBook.add("book.docauthor=" + fi.docauthor);
				}
				if (!StrUtils.isEmptyStr(fi.docprogram)) {
					itemsBook.add("book.docprogram=" + fi.docprogram);
				}
				if (!StrUtils.isEmptyStr(fi.getDocdate())) {
					itemsBook.add("book.docdate=" + fi.getDocdate());
				}
				if (!StrUtils.isEmptyStr(fi.docsrcurl)) {
					itemsBook.add("book.docsrcurl=" + fi.docsrcurl);
				}
				if (!StrUtils.isEmptyStr(fi.docsrcocr)) {
					itemsBook.add("book.docsrcocr=" + fi.docsrcocr);
				}
				if (!StrUtils.isEmptyStr(fi.docversion)) {
					itemsBook.add("book.docversion=" + fi.docversion);
				}
				itemsBook.add("section=section.book_publisher");
				if (!StrUtils.isEmptyStr(fi.publname)) {
					itemsBook.add("book.publname=" + fi.publname);
				}
				if (!StrUtils.isEmptyStr(fi.publisher)) {
					itemsBook.add("book.publisher=" + fi.publisher);
				}
				if (!StrUtils.isEmptyStr(fi.publcity)) {
					itemsBook.add("book.publcity=" + fi.publcity);
				}
				if (!StrUtils.isEmptyStr(fi.getPublyear())) {
					itemsBook.add("book.publyear=" + fi.getPublyear());
				}
				if (!StrUtils.isEmptyStr(fi.publisbn)) {
					itemsBook.add("book.publisbn=" + fi.publisbn);
				}
				if ( fi.publseries!=null ) {
					String s = fi.publseries;
					if ( fi.publseriesNumber>0 )
						s = s + " #" + fi.publseriesNumber;
					itemsBook.add("book.publseries=" + s);
				}
				itemsBook.add("section=section.book_translation");
				String lfrom = "[empty]";
				if (!StrUtils.isEmptyStr(fi.lang_from)) lfrom = fi.lang_from;
				String lto = "[empty]";
				if (!StrUtils.isEmptyStr(fi.lang_to)) lto = fi.lang_to;
				itemsBook.add("book.translation=" + lfrom + " -> " + lto);
				if (itemsPos.size()==1) itemsPos.clear();
				if (itemsBook.size()==1) itemsBook.clear();
				if (itemsFile.size()==1) itemsFile.clear();
				if (itemsSys.size()==1) itemsSys.clear();
				boolean bSection = true;
				for (String s: itemsPos) {
					if ((bSection)&&(s.startsWith("section="))&&(itemsAll.size()>0))
						itemsAll.remove(itemsAll.size()-1);
					bSection=s.startsWith("section=");
					itemsAll.add(s);
				}
				for (String s: itemsBook) {
					if ((bSection)&&(s.startsWith("section="))&&(itemsAll.size()>0))
						itemsAll.remove(itemsAll.size()-1);
					bSection=s.startsWith("section=");
					itemsAll.add(s);
				}
				for (String s: itemsFile) {
					if ((bSection)&&(s.startsWith("section="))&&(itemsAll.size()>0))
						itemsAll.remove(itemsAll.size()-1);
					bSection=s.startsWith("section=");
					itemsAll.add(s);
				}
				for (String s: itemsSys) {
					if ((bSection)&&(s.startsWith("section="))&&(itemsAll.size()>0))
						itemsAll.remove(itemsAll.size()-1);
					bSection=s.startsWith("section=");
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
			opds.filename = "New Catalog";
			opds.isListed = true;
			opds.isScanned = true;
			opds.parent = Services.getScanner().getOPDSRoot();
		}
		OPDSCatalogEditDialog dlg = new OPDSCatalogEditDialog(CoolReader.this, opds, new Runnable() {
			@Override
			public void run() {
				refreshOPDSRootDirectory(true);
			}
		});
		dlg.show();
	}

	public void refreshOPDSRootDirectory(boolean showInBrowser) {
		if (mBrowser != null)
			mBrowser.refreshOPDSRootDirectory(showInBrowser);
		if (mHomeFrame != null)
			mHomeFrame.refreshOnlineCatalogs();
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
		showNotice(R.string.note1_reader_menu, new Runnable() {
			@Override
			public void run() {
				setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_SHORT_SIDE), false);
			}
		}, new Runnable() {
			@Override
			public void run() {
				setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_NONE), false);
			}
		});
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
			loadDocument(location, new Runnable() {
				@Override
				public void run() {
					BackgroundThread.instance().postGUI(new Runnable() {
						@Override
						public void run() {
							// if document not loaded show error & then root window
							ErrorDialog errDialog = new ErrorDialog(CoolReader.this, "Error", "Can't open file!");
							errDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
								@Override
								public void onDismiss(DialogInterface dialog) {
									showRootWindow();
								}
							});
							errDialog.show();
						}
					}, 1000);
				}
			});
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
			loadDocument(bi.getFileInfo());
	}

	public void readResizeHistory()
	{
        log.d("Reading rh.json");
		String rh = "";
		try {
			final File fJson = new File(getSettingsFile(0).getParent() + "/rh.json");
			BufferedReader reader = new BufferedReader(
					new FileReader(fJson));
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			String ls = System.getProperty("line.separator");
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
			reader.close();
			rh = stringBuilder.toString();
			setResizeHist(new ArrayList<ResizeHistory>(StrUtils.stringToArray(rh, ResizeHistory[].class)));
		} catch (Exception e) {
		}
	}

	public void saveResizeHistory()
	{
		log.d("Starting save rh.json");
		try {
			final File fJson = new File(getSettingsFile(0).getParent() + "/rh.json");

			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			final String prettyJson = gson.toJson(getResizeHist());

			BufferedWriter bw = null;
			FileWriter fw = null;
			char[] bytesArray = new char[1000];
			int bytesRead = 1000;
			try {
				fw = new FileWriter(fJson);
				bw = new BufferedWriter(fw);
				bw.write(prettyJson);
				bw.close();
				fw.close();
			} catch (Exception e) {
			}
		} catch (Exception e) {
		}
	}

	public void saveCurPosFile(String json)
	{
		log.d("Starting save cur_pos.json");
		try {
			final File fJson = new File(getSettingsFile(0).getParent() + "/cur_pos.json");
			BufferedWriter bw = null;
			FileWriter fw = null;
			char[] bytesArray = new char[1000];
			int bytesRead = 1000;
			try {
				fw = new FileWriter(fJson);
				bw = new BufferedWriter(fw);
				bw.write(json);
				bw.close();
				fw.close();
			} catch (Exception e) {
			}
		} catch (Exception e) {
		}
	}

	public Bookmark readCurPosFile()
	{
		log.d("Reading cur_pos.json");
		String cur_pos = "";
		try {
			final File fJson = new File(getSettingsFile(0).getParent() + "/cur_pos.json");
			if (fJson.exists()) {
				BufferedReader reader = new BufferedReader(
						new FileReader(fJson));
				StringBuilder stringBuilder = new StringBuilder();
				String line = null;
				String ls = System.getProperty("line.separator");
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
					stringBuilder.append(ls);
				}
				reader.close();
				cur_pos = stringBuilder.toString();
				return new Gson().fromJson(cur_pos, Bookmark.class);
			} else return null;
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
			String shTitle = (StrUtils.isEmptyStr(item.title)?item.filename.trim():item.title.trim());
			return addShortCut(this, shortcutIntent, icon, shTitle);
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
				intent.setPackage("org.coolreader.mod.plotn");
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
				intent.setPackage("org.coolreader.mod.plotn");
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
				intent.setPackage("org.coolreader.mod.plotn");
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
				intent.setPackage("org.coolreader.mod.plotn");
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
				intent.setPackage("org.coolreader.mod.plotn");
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

}

