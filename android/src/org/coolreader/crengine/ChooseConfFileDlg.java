package org.coolreader.crengine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudFileInfo;
import org.coolreader.cloud.CloudSyncFolder;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ChooseConfFileDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private ConfFileList mList;

	public ArrayList<CloudFileInfo> mSettingsList;
	public ArrayList<CloudFileInfo> mSettingsFile;

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
			String sAdd = "unknown file name format";
			if ( md!=null ) {
				if ( titleTextView!=null )
					sTitle = md.name.toLowerCase();
				    titleTextView.setText(sTitle);
				    if (sTitle.contains(mCoolReader.getAndroid_id())) sAdd = " (current)";
					addTextView.setText(md.comment+sAdd);
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
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					//openContextMenu(DictList.this);
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			if ( mSettingsList==null )
				return true;
			CloudFileInfo m = mSettingsList.get(position);
			//mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_SETTINGS, m);
			if (m != null) {
				ArrayList<CloudFileInfo> afi = new ArrayList<CloudFileInfo>();
				String fname = m.name;
				for (CloudFileInfo fi : mSettingsFile) {
					String findStr = fname.replace("_cr3_ini_","_cr3.ini.*").replace(".info","");
					if (fi.name.matches(".*"+findStr+".*")) afi.add(fi);
				}
				CloudSyncFolder.restoreSettingsFiles(mCoolReader,m, afi,false);
				dismiss();
			}
			return true;
		}
		
		
	}

	public ChooseConfFileDlg(CoolReader activity, File[] matchingFilesInfo, File[] matchingFiles)
	{
		super("ChooseConfFileDlg", activity, activity.getResources().getString(R.string.win_title_conf_file), false, true);
		//mThis = this; // for inner classes
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
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.conf_list);
		mList = new ConfFileList(activity, false);
		body.addView(mList);
		setView(frame);
		setFlingHandlers(mList, null, null);
	}

	@Override
	protected void onPositiveButtonClick() {
		// add Dict
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
		//registerForContextMenu(mList);
	}

}
