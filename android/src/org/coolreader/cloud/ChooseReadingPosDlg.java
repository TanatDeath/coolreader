package org.coolreader.cloud;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.yandex.YNDListFiles;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

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

public class ChooseReadingPosDlg extends BaseDialog {

	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private ReadingPosListView mList;

	public List<CloudFileInfo> mReadingPosList;

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
			if (mReadingPosList != null) cnt = mReadingPosList.size();
			return cnt;
		}

		public Object getItem(int position) {
			int cnt = 0;
			if (mReadingPosList != null) cnt = mReadingPosList.size();
			if ( position<0 || position>=cnt )
				return null;
			return mReadingPosList.get(position);
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
			if (mReadingPosList!=null) md = mReadingPosList.get(position);
			if ( labelView!=null ) {
				labelView.setText(String.valueOf(position+1));
			}
			String sTitle0 = "";
			String sTitle = "";
			String sAdd = "unknown file name format";
			if ( md!=null ) {
				if ( titleTextView!=null )
					sTitle0 = StrUtils.getNonEmptyStr(md.name,true);
					sTitle = StrUtils.getNonEmptyStr(md.comment,true);
					final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
					final CharSequence sFName0 = dfmt.format("yyyy-MM-dd kk:mm:ss", md.created);
					final String sFName = sFName0.toString();
					sAdd = "";
				    if (sTitle.contains("~from")) {
						int ipos = sTitle.indexOf("~from");
						sAdd = sTitle.substring(ipos+6,sTitle.length()).trim();
						sTitle = sTitle.substring(0,ipos).trim();
					} else {
				    	String[] arrS = sTitle0.split("_"); // 2020-03-30_222303_rpos_635942216_36b928e773055c4a_0.16.json
				    	if (arrS.length>=6) {
				    		sTitle = arrS[5].replace(".json","")+"%";
							sAdd =	arrS[4];
						}
					}
					titleTextView.setText(sTitle + " at "+sFName);
				    if (sAdd.contains(mCoolReader.getAndroid_id())) {
						sAdd = sAdd.replace(mCoolReader.getAndroid_id(), mCoolReader.getString(R.string.rpos_current_device));
					} else {
				    	if (CloudSync.devicesKnown != null)
				    		for (DeviceKnown dev: CloudSync.devicesKnown) {
								if (sAdd.contains(dev.deviceId))
									sAdd = sAdd.replace(dev.deviceId, dev.deviceName);
							}
					}
					addTextView.setText(" "+mCoolReader.getString(R.string.rpos_from)+" "+sAdd);
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
			if (mReadingPosList != null) cnt = mReadingPosList.size();
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
	
	class ReadingPosListView extends BaseListView {

		public ReadingPosListView( Context context, boolean shortcutMode ) {
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
			if ( mReadingPosList==null )
				return true;
			CloudFileInfo m = mReadingPosList.get(position);
			CloudSync.loadFromJsonInfoFile(mCoolReader, CloudSync.CLOUD_SAVE_READING_POS, m.path,false,
					m.name, cloudMode);
			dismiss();
			return true;
		}

	}

	public ChooseReadingPosDlg(CoolReader activity, File[] matchingFiles)
	{
		super("ChooseReadingPosDlg", activity, activity.getResources().getString(R.string.win_title_reading_pos), false, true);
		cloudMode = Settings.CLOUD_SYNC_VARIANT_FILESYSTEM;
		CloudSync.readKnownDevices(activity);
		//mThis = this; // for inner classes
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mReadingPosList = new ArrayList<CloudFileInfo>();
		for (File f: matchingFiles) {
			if (f.getName().endsWith(".json")) {
				String sComment = "";
				File fInfo = new File(f.getPath().replace(".json",".info"));
				if (fInfo.exists()) {
					sComment = Utils.readFileToString(f.getPath().replace(".json",".info"));
				}
				CloudFileInfo yfile = new CloudFileInfo();
				yfile.comment = sComment;
				yfile.name = f.getName();
				yfile.path = f.getPath();
				yfile.created = new Date(f.lastModified());
				yfile.modified = new Date(f.lastModified());
				mReadingPosList.add(yfile);
			}
		}
//		Comparator<CloudFileInfo> compareByDate = new Comparator<CloudFileInfo>() {
//			@Override
//			public int compare(CloudFileInfo o1, CloudFileInfo o2) {
//				return -(o1.created.compareTo(o2.created));
//			}
//		};
//		Collections.sort(mReadingPosList, compareByDate);
		//setPositiveButtonImage(R.drawable.cr3_button_add, R.string.mi_Dict_add);
		View frame = mInflater.inflate(R.layout.conf_list_dialog, null);
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.conf_list);
		mList = new ReadingPosListView(activity, false);
		body.addView(mList);
		setView(frame);
		setFlingHandlers(mList, null, null);
	}

	public ChooseReadingPosDlg(CoolReader activity, YNDListFiles matchingFiles)
	{
		super("ChooseReadingPosDlg", activity, activity.getResources().getString(R.string.win_title_reading_pos), false, true);
		cloudMode = Settings.CLOUD_SYNC_VARIANT_YANDEX;
		//mThis = this; // for inner classes
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mReadingPosList = matchingFiles.fileList;

		Comparator<CloudFileInfo> compareByDate = new Comparator<CloudFileInfo>() {
			@Override
			public int compare(CloudFileInfo o1, CloudFileInfo o2) {
				return -(o1.created.compareTo(o2.created));
			}
		};
		Collections.sort(mReadingPosList, compareByDate);
		//setPositiveButtonImage(R.drawable.cr3_button_add, R.string.mi_Dict_add);
		View frame = mInflater.inflate(R.layout.conf_list_dialog, null);
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.conf_list);
		mList = new ReadingPosListView(activity, false);
		body.addView(mList);
		setView(frame);
		setFlingHandlers(mList, null, null);
	}

	@Override
	protected void onPositiveButtonClick() {
		// add Dict
		ChooseReadingPosDlg.this.dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		ChooseReadingPosDlg.this.dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating ChooseConfFileDlg");
		setTitle(mCoolReader.getResources().getString(R.string.win_title_reading_pos));
		setCancelable(true);
		super.onCreate(savedInstanceState);
		//registerForContextMenu(mList);
	}

}
