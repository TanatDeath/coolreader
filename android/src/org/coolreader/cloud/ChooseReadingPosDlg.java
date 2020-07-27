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
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
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

public class ChooseReadingPosDlg extends BaseDialog {

	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private ReadingPosListView mList;
	private Button btnThisDevice;
	private Button btnDateSort;
	private Button btnPercentSort;
	private boolean bHideThisDevice = false;
	private boolean bDateSort = true;
	private File[] mMatchingFiles;
	private YNDListFiles mYMatchingFiles;

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
			if (mReadingPosList!=null)
				if (mReadingPosList.size()>position)
					md = mReadingPosList.get(position);
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
					String sAdd2 = "";
				    if (sTitle.contains("~from")) {
						int ipos = sTitle.indexOf("~from");
						sAdd = sTitle.substring(ipos+6,sTitle.length()).trim();
						if (sTitle0.contains(mCoolReader.getAndroid_id())) sAdd = mCoolReader.getString(R.string.rpos_current_device);
						sTitle = sTitle.substring(0,ipos).trim();
						sAdd2 = sFName;
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
					addTextView.setText(sAdd2+" "+mCoolReader.getString(R.string.rpos_from)+" "+sAdd);
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

	private void paintButtons() {
		int colorGrayC;
		int colorBlue;
		int colorIcon;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeBlue, R.attr.colorIcon});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorBlue = a.getColor(1, Color.BLUE);
		colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mCoolReader.tintViewIcons(btnThisDevice, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(btnDateSort, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(btnPercentSort, PorterDuff.Mode.CLEAR,true);
		if (!bHideThisDevice) {
			btnThisDevice.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnThisDevice, true);
		} else {
			btnThisDevice.setBackgroundColor(colorGrayCT);
		}
		if (bDateSort) {
			btnDateSort.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnDateSort, true);
			btnPercentSort.setBackgroundColor(colorGrayCT);
		} else {
			btnPercentSort.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnPercentSort, true);
			btnDateSort.setBackgroundColor(colorGrayCT);
		}
		btnThisDevice.setTextColor(colorIcon);
		btnDateSort.setTextColor(colorIcon);
		btnPercentSort.setTextColor(colorIcon);
	}

	public void sortAndFilterList() {
		mReadingPosList = new ArrayList<CloudFileInfo>();
		if (mYMatchingFiles != null) {
			for (CloudFileInfo cfi: mYMatchingFiles.fileList) {
				if ((!bHideThisDevice) || (!cfi.name.contains(mCoolReader.getAndroid_id())))
					mReadingPosList.add(cfi);
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
						mReadingPosList.add(yfile);
					}
				}
			}
		}
		if (!bDateSort) {
			Comparator<CloudFileInfo> compareByPercent = new Comparator<CloudFileInfo>() {
				@Override
				public int compare(CloudFileInfo o1, CloudFileInfo o2) {
					double d1 = 0.0;
					double d2 = 0.0;
					String[] arrS = o1.name.split("_"); // 2020-03-30_222303_rpos_635942216_36b928e773055c4a_0.16.json
					if (arrS.length >= 6) {
						String s = arrS[5].replace(".json", "");
						try {
							d1 = Double.valueOf(s);
						} catch (Exception e) {

						}
					} else {
						arrS = o1.comment.split("%");
						if (arrS.length>0) {
							try {
								d1 = Double.valueOf(arrS[0]);
							} catch (Exception e) {

							}
						}
					}
					arrS = o2.name.split("_"); // 2020-03-30_222303_rpos_635942216_36b928e773055c4a_0.16.json
					if (arrS.length >= 6) {
						String s = arrS[5].replace(".json", "");
						try {
							d2 = Double.valueOf(s);
						} catch (Exception e) {

						}
					} else {
						arrS = o2.comment.split("%");
						if (arrS.length>0) {
							try {
								d2 = Double.valueOf(arrS[0]);
							} catch (Exception e) {

							}
						}
					}
					if (d1 != d2) return d1 < d2 ? 1 : -1;
					return -(o1.created.compareTo(o2.created));
				}
			};
			Collections.sort(mReadingPosList, compareByPercent);
		} else {
			Comparator<CloudFileInfo> compareByDate = new Comparator<CloudFileInfo>() {
				@Override
				public int compare(CloudFileInfo o1, CloudFileInfo o2) {
					return -(o1.created.compareTo(o2.created));
				}
			};
			Collections.sort(mReadingPosList, compareByDate);
		}
		ConfFileAdapter cfa = new ConfFileAdapter();
		mList.setAdapter(cfa);
		cfa.notifyDataSetChanged();
	}

	public void setButtonsState() {
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		if (btnThisDevice!=null) {
			btnThisDevice.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
			btnThisDevice.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					bHideThisDevice = !bHideThisDevice;
					Properties props = new Properties(mCoolReader.settings());
					props.setProperty(Settings.PROP_APP_CLOUD_POS_HIDE_CURRENT_DEV, bHideThisDevice?"1":"0");
					mCoolReader.setSettings(props, -1, true);
					paintButtons();
					sortAndFilterList();
				}
			});
		}
		bHideThisDevice = mCoolReader.settings().getBool(Settings.PROP_APP_CLOUD_POS_HIDE_CURRENT_DEV, false);
		Drawable img2 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img3 = img2.getConstantState().newDrawable().mutate();
		if (btnDateSort!=null) {
			btnDateSort.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
			btnDateSort.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					bDateSort = true;
					Properties props = new Properties(mCoolReader.settings());
					props.setProperty(Settings.PROP_APP_CLOUD_POS_DATE_SORT, bDateSort?"1":"0");
					mCoolReader.setSettings(props, -1, true);
					paintButtons();
					sortAndFilterList();
				}
			});
		}
		bDateSort  = mCoolReader.settings().getBool(Settings.PROP_APP_CLOUD_POS_DATE_SORT, true);
		Drawable img4 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img5 = img4.getConstantState().newDrawable().mutate();
		if (btnPercentSort!=null) {
			btnPercentSort.setCompoundDrawablesWithIntrinsicBounds(img5, null, null, null);
			btnPercentSort.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					bDateSort = false;
					Properties props = new Properties(mCoolReader.settings());
					props.setProperty(Settings.PROP_APP_CLOUD_POS_DATE_SORT, bDateSort?"1":"0");
					mCoolReader.setSettings(props, -1, true);
					paintButtons();
					sortAndFilterList();
				}
			});
		}
		paintButtons();
	}

	public ChooseReadingPosDlg(CoolReader activity, File[] matchingFiles)
	{
		super("ChooseReadingPosDlg", activity, activity.getResources().getString(R.string.win_title_reading_pos), false, true);
		mMatchingFiles = matchingFiles;
		cloudMode = Settings.CLOUD_SYNC_VARIANT_FILESYSTEM;
		CloudSync.readKnownDevices(activity);
		//mThis = this; // for inner classes
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mReadingPosList = new ArrayList<CloudFileInfo>();
		for (File f: mMatchingFiles) {
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
		View frame = mInflater.inflate(R.layout.conf_list_dialog, null);
		btnThisDevice = ((Button)frame.findViewById(R.id.btn_this_device));
		btnDateSort = ((Button)frame.findViewById(R.id.btn_date_sort));
		btnPercentSort = ((Button)frame.findViewById(R.id.btn_percent_sort));
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.conf_list);
		mList = new ReadingPosListView(activity, false);
		body.addView(mList);
		setView(frame);
		setButtonsState();
		sortAndFilterList();
		setFlingHandlers(mList, null, null);
	}

	public ChooseReadingPosDlg(CoolReader activity, YNDListFiles matchingFiles)
	{
		super("ChooseReadingPosDlg", activity, activity.getResources().getString(R.string.win_title_reading_pos), false, true);
		mYMatchingFiles = matchingFiles;
		cloudMode = Settings.CLOUD_SYNC_VARIANT_YANDEX;
		//mThis = this; // for inner classes
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mReadingPosList = mYMatchingFiles.fileList;
		View frame = mInflater.inflate(R.layout.conf_list_dialog, null);
		btnThisDevice = ((Button)frame.findViewById(R.id.btn_this_device));
		btnDateSort = ((Button)frame.findViewById(R.id.btn_date_sort));
		btnPercentSort = ((Button)frame.findViewById(R.id.btn_percent_sort));
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.conf_list);
		mList = new ReadingPosListView(activity, false);
		body.addView(mList);
		setView(frame);
		setButtonsState();
		sortAndFilterList();
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
