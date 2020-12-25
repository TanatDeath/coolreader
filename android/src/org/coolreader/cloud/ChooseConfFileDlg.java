package org.coolreader.cloud;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.yandex.YNDListFiles;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class ChooseConfFileDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private ConfFileList mList;
	private Button btnThisDevice;
	private Button btnDeleteAll;
	private Button btnDateSort;
	private Button btnPercentSort;
	private boolean bHideThisDevice = false;
	private boolean bDateSort = true;

	public ArrayList<CloudFileInfo> mSettingsList;
	public ArrayList<CloudFileInfo> mSettingsFile;

	private File[] mMatchingFiles;
	private YNDListFiles mYMatchingFiles;

	public int cloudMode;

	public final static int ITEM_POSITION=0;

	class ConfFileAdapter extends BaseAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			int cnt = 0;
			if (mSettingsList != null) cnt = mSettingsList.size();
			return cnt;
		}

		public Object getItem(int position) {
			int cnt = 0;
			if (mSettingsList != null) cnt = mSettingsList.size();
			if ( position<0 || position>=cnt )
				return null;
			return mSettingsList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public int getViewTypeCount() {
			return 4;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.conf_file_item;
			view = mInflater.inflate(res, null);
			TextView labelView = (TextView)view.findViewById(R.id.conf_file_shortcut);
			TextView titleTextView = (TextView)view.findViewById(R.id.conf_file_title);
			TextView addTextView = (TextView)view.findViewById(R.id.conf_file_pos_text);
		 	CloudFileInfo md = null;
			if (mSettingsList!=null) md = mSettingsList.get(position);
			if ( labelView!=null ) {
				labelView.setText(String.valueOf(position+1));
			}
			String sTitle = "";
			String sAdd = "";
			if ( md!=null ) {
				if ( titleTextView!=null )
					sTitle = StrUtils.getNonEmptyStr(md.name, true).toLowerCase();
					String[] arrS = sTitle.split("_"); // 2020-04-24_192613_settings_04a72e2be696893c_2
					if (arrS.length>=4) {
						sAdd = arrS[4].replace(".txt","") + " profile(s)";
					}
				    titleTextView.setText(sTitle);
				    String sAndrId = mCoolReader.getAndroid_id();
				    if (sTitle.contains(sAndrId)) {
						sAdd = sAdd + ", from current device";
					} else {
						if (CloudSync.devicesKnown != null)
							for (DeviceKnown dev: CloudSync.devicesKnown) {
								if (sTitle.contains(dev.deviceId))
									sAdd = sAdd + ", from " + dev.deviceName;
							}
					}
				    if (sAdd.startsWith(", ")) sAdd = sAdd.substring(2);
				    if (StrUtils.isEmptyStr(md.comment)) addTextView.setText(sAdd);
				    else addTextView.setText(md.comment+", "+sAdd);
			} else {
				if ( titleTextView!=null )
					titleTextView.setText("");
					addTextView.setText("");
			}
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			int cnt = 0;
			if (mSettingsList != null) cnt = mSettingsList.size();
			return cnt==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();
		
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}
	
	class ConfFileList extends BaseListView {

		public ConfFileList( Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new ConfFileAdapter());
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
				return true;
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			if ( mSettingsList==null )
				return true;
			CloudFileInfo m = mSettingsList.get(position);
			if (m != null) {
				if (cloudMode == Settings.CLOUD_SYNC_VARIANT_FILESYSTEM) {
					ArrayList<CloudFileInfo> afi = new ArrayList<CloudFileInfo>();
					String fname = m.name;
					for (CloudFileInfo fi : mSettingsFile) {
						String findStr = fname.replace("_cr3_ini_", "_cr3.ini.*").replace(".info", "");
						if (fi.name.matches(".*" + findStr + ".*")) afi.add(fi);
					}
					CloudSync.restoreSettingsFiles(mCoolReader, m, afi, false);
				}
				if (cloudMode == Settings.CLOUD_SYNC_VARIANT_YANDEX) {
					CloudSync.loadFromJsonInfoFile(mCoolReader, CloudSync.CLOUD_SAVE_SETTINGS, m.path,
							false, m.name, cloudMode);
				}
				dismiss();
			}
			return true;
		}
	}

	private void paintButtons() {
		int colorGrayC;
		int colorIcon;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeBlue, R.attr.colorIcon});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mCoolReader.tintViewIcons(btnThisDevice, PorterDuff.Mode.CLEAR,true);
		if (!bHideThisDevice) {
			btnThisDevice.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnThisDevice, true);
		} else {
			btnThisDevice.setBackgroundColor(colorGrayCT);
		}
		btnThisDevice.setTextColor(colorIcon);
		mCoolReader.tintViewIcons(btnDeleteAll,true);
		btnDeleteAll.setBackgroundColor(colorGrayCT2);
		btnDeleteAll.setTextColor(colorIcon);
	}

	public void setButtonsState() {
		Drawable imgC = getContext().getResources().getDrawable(R.drawable.icons8_check_no_frame);
		Drawable imgC1 = imgC.getConstantState().newDrawable().mutate();
		if (btnThisDevice!=null) {
			btnThisDevice.setCompoundDrawablesWithIntrinsicBounds(imgC1, null, null, null);
			btnThisDevice.setOnClickListener(v -> {
				bHideThisDevice = !bHideThisDevice;
				Properties props = new Properties(mCoolReader.settings());
				props.setProperty(Settings.PROP_APP_CLOUD_POS_HIDE_CURRENT_DEV, bHideThisDevice?"1":"0");
				mCoolReader.setSettings(props, -1, true);
				paintButtons();
				sortAndFilterList();
			});
		}
		bHideThisDevice = mCoolReader.settings().getBool(Settings.PROP_APP_CLOUD_POS_HIDE_CURRENT_DEV, false);
		paintButtons();
	}

	public void sortAndFilterList() {
		mSettingsList = new ArrayList<>();
		if (mYMatchingFiles != null) {
			for (CloudFileInfo cfi: mYMatchingFiles.fileList) {
				if ((!bHideThisDevice) || (!cfi.name.contains(mCoolReader.getAndroid_id())))
					mSettingsList.add(cfi);
			}
		}
		if (mMatchingFiles != null) {
			for (File f: mMatchingFiles) {
				if (f.getName().endsWith(".json")) {
					String sComment = "";
					File fInfo = new File(f.getPath().replace(".json",".info"));
					if (fInfo.exists()) {
						sComment = Utils.readFileToString(f.getPath().replace(".json",".info"));
					}
					if ((!bHideThisDevice) || (!f.getName().contains(mCoolReader.getAndroid_id()))) {
						CloudFileInfo yfile = new CloudFileInfo();
						yfile.comment = sComment;
						yfile.name = f.getName();
						yfile.path = f.getPath();
						yfile.created = new Date(f.lastModified());
						yfile.modified = new Date(f.lastModified());
						mSettingsList.add(yfile);
					}
				}
			}
		}
		ChooseConfFileDlg.ConfFileAdapter cfa = new ChooseConfFileDlg.ConfFileAdapter();
		mList.setAdapter(cfa);
		cfa.notifyDataSetChanged();
	}

	private void initAddButtons(View frame) {
		btnThisDevice = (frame.findViewById(R.id.btn_this_device));
		btnDateSort = (frame.findViewById(R.id.btn_date_sort));
		btnPercentSort = (frame.findViewById(R.id.btn_percent_sort));
		Utils.hideView(btnDateSort);
		Utils.hideView(btnPercentSort);
		btnDeleteAll = (frame.findViewById(R.id.btn_delete_all_pos));
		btnDeleteAll.setOnClickListener(v -> {
			mCoolReader.askConfirmation(R.string.are_you_sure, () -> {
				int iSyncVariant2 = mCoolReader.settings().getInt(Settings.PROP_CLOUD_SYNC_VARIANT, 0);
				CloudSync.loadFromJsonInfoFileList(mCoolReader,
						CloudSync.CLOUD_SAVE_SETTINGS, false, iSyncVariant2 == 1, CloudAction.DELETE_FILES, false);
				dismiss();
			});
		});
		int iSyncVariant2 = mCoolReader.settings().getInt(Settings.PROP_CLOUD_SYNC_VARIANT, 0);
		if (iSyncVariant2 != 2) Utils.hideView(btnDeleteAll);
	}


	public ChooseConfFileDlg(CoolReader activity, File[] matchingFilesInfo, File[] matchingFiles)
	{
		super("ChooseConfFileDlg", activity, activity.getResources().getString(R.string.win_title_conf_file), false, true);
		mMatchingFiles = matchingFiles;
		cloudMode = Settings.CLOUD_SYNC_VARIANT_FILESYSTEM;
		//mThis = this; // for inner classes
		CloudSync.readKnownDevices(activity);
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mSettingsList = new ArrayList<CloudFileInfo>();
		for (File f: matchingFilesInfo) {
			String sComment = "";
			File fInfo = new File(f.getPath());
			if (fInfo.exists()) {
				sComment = Utils.readFileToString(f.getPath());
			}
			CloudFileInfo yfile = new CloudFileInfo();
			yfile.comment = sComment;
			yfile.name = f.getName();
			yfile.path = f.getPath();
			yfile.created = new Date(f.lastModified());
			yfile.modified = new Date(f.lastModified());
			mSettingsList.add(yfile);
		}
		Comparator<CloudFileInfo> compareByDate = new Comparator<CloudFileInfo>() {
			@Override
			public int compare(CloudFileInfo o1, CloudFileInfo o2) {
				return -(o1.created.compareTo(o2.created));
			}
		};
		Collections.sort(mSettingsList, compareByDate);
		mSettingsFile = new ArrayList<CloudFileInfo>();
		for (File f: matchingFiles) {
			CloudFileInfo yfile = new CloudFileInfo();
			yfile.comment = "";
			yfile.name = f.getName();
			yfile.path = f.getPath();
			yfile.created = new Date(f.lastModified());
			yfile.modified = new Date(f.lastModified());
			mSettingsFile.add(yfile);
		}
		//setPositiveButtonImage(R.drawable.cr3_button_add, R.string.mi_Dict_add);
		View frame = mInflater.inflate(R.layout.conf_list_dialog, null);
		initAddButtons(frame);
		btnThisDevice = frame.findViewById(R.id.btn_this_device);
		btnDateSort = frame.findViewById(R.id.btn_date_sort);
		btnPercentSort = frame.findViewById(R.id.btn_percent_sort);
		ViewGroup body = frame.findViewById(R.id.conf_list);
		mCoolReader.tintViewIcons(frame);
		mList = new ConfFileList(activity, false);
		body.addView(mList);
		setView(frame);
		setButtonsState();
		sortAndFilterList();
		paintButtons();
		setFlingHandlers(mList, null, null);
	}

	public ChooseConfFileDlg(CoolReader activity, YNDListFiles matchingFiles)
	{
		super("ChooseConfFileDlg", activity, activity.getResources().getString(R.string.win_title_conf_file), false, true);
		mYMatchingFiles = matchingFiles;
		cloudMode = Settings.CLOUD_SYNC_VARIANT_YANDEX;
		//mThis = this; // for inner classes
		CloudSync.readKnownDevices(activity);
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mSettingsList = new ArrayList<>();
		for (CloudFileInfo f: matchingFiles.fileList) {
			mSettingsList.add(f);
		}
		Comparator<CloudFileInfo> compareByDate = (o1, o2) -> -(o1.created.compareTo(o2.created));
		Collections.sort(mSettingsList, compareByDate);
		mSettingsFile = new ArrayList<>();
		View frame = mInflater.inflate(R.layout.conf_list_dialog, null);
		initAddButtons(frame);
		btnThisDevice = frame.findViewById(R.id.btn_this_device);
		btnDateSort = frame.findViewById(R.id.btn_date_sort);
		btnPercentSort = frame.findViewById(R.id.btn_percent_sort);
		ViewGroup body = frame.findViewById(R.id.conf_list);
		mList = new ConfFileList(activity, false);
		body.addView(mList);
		setView(frame);
		setButtonsState();
		sortAndFilterList();
		paintButtons();
		setFlingHandlers(mList, null, null);
	}

	@Override
	protected void onPositiveButtonClick() {
		ChooseConfFileDlg.this.dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		ChooseConfFileDlg.this.dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating ChooseConfFileDlg");
		setTitle(mCoolReader.getResources().getString(R.string.win_title_conf_file));
		setCancelable(true);
		super.onCreate(savedInstanceState);
	}
}
