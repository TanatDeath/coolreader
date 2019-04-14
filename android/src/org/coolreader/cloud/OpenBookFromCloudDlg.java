package org.coolreader.cloud;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.StrUtils;

import java.sql.Struct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OpenBookFromCloudDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private OpenBookFromGDList mGDList;
	private OpenBookFromDBXList mDBXList;

	public boolean mWholeSearch = false;
	private TextView lblFolder;
	public EditText edtFind;
	private RadioButton btnWhole;
	private RadioButton btnFolder;
	private ImageButton btnFind;
	private ImageButton btnRoot;
	private ImageButton btnUp;
	private RadioGroup rgScope;

	public boolean mFoundWhole = false;

	public final static int ITEM_POSITION=0;

	public com.google.android.gms.drive.DriveId getGDDriveToGo() {
		if (mGDList == null) return null;
		return mGDList.driveToGoTo;
	}

	public void setGDBooksList(com.google.android.gms.drive.MetadataBuffer bl) {
		if (mGDList != null) mGDList.mBooksList = bl;
	}

	public void setDBXLfrList(List<com.dropbox.core.v2.files.Metadata> lfr) {
		if (mDBXList != null) mDBXList.mLFR = lfr;
	}

	public void listUpdated() {
		if (mGDList != null) mGDList.listUpdated();
		if (mDBXList != null) mDBXList.listUpdated();
	}

	class OpenBookFromGDAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			int cnt = 0;
			if (mGDList.mBooksList != null) cnt = mGDList.mBooksList.getCount();
			return cnt;
		}

		public Object getItem(int position) {
			int cnt = 0;
			int pos = position;
			if (mGDList.mBooksList != null) cnt = mGDList.mBooksList.getCount();
			if ( pos<0 || pos>=cnt )
				return null;
			return mGDList.mBooksList.get(pos);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			View view1;
			View view2;
			int res = R.layout.open_book_from_gd_item;
			int res1 = R.layout.open_book_from_gd_item1;
			int res2 = R.layout.open_book_from_gd_item2;
			view = mInflater.inflate(res, null);
			TextView fileView = (TextView)view.findViewById(R.id.file_name);
			TextView descrView = (TextView)view.findViewById(R.id.file_descr);
			TextView timeView = (TextView)view.findViewById(R.id.file_time);
			view1 = mInflater.inflate(res1, null);
			TextView fileView1 = (TextView)view1.findViewById(R.id.file_name);
			view2 = mInflater.inflate(res2, null);
			TextView fileView2 = (TextView)view2.findViewById(R.id.file_name);
			TextView timeView2 = (TextView)view2.findViewById(R.id.file_time);
			com.google.android.gms.drive.Metadata md = null;
			String sFile = "";
			if (mGDList.mBooksList != null) md = mGDList.mBooksList.get(position);
			String sTitle = "";
			String sFTime = "";
			if ( md!=null ) {
				sFile = md.getTitle();
				if (sFile == null) sFile ="";
				if (md.getMimeType().equals("application/vnd.google-apps.folder")) sFile = "["+sFile+"]";
						sTitle = md.getDescription();
				if (sTitle == null) sTitle ="";
				final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
				final CharSequence sFName0 = dfmt.format("yyyy-MM-dd kk:mm:ss", md.getCreatedDate());
				sFTime = sFName0.toString();
				if (sFTime == null) sFTime ="";
				if ((sFTime.isEmpty())&&(sTitle.isEmpty())) {
					fileView1.setText(sFile);
				}
				if ((!sFTime.isEmpty())&&(sTitle.isEmpty())) {
					fileView2.setText(sFile);
					timeView2.setText(sFTime);
				}
				if ((!sFTime.isEmpty())&&(!sTitle.isEmpty())) {
					fileView.setText(sFile);
					descrView.setText(StrUtils.textShrinkLines(sTitle, true));
					timeView.setText(sFTime);
				}
			} else {
				fileView1.setText(sFile);
				descrView.setText("");
				timeView.setText("");
			}
			if ((sFTime.isEmpty())&&(sTitle.isEmpty())) return view1;
			if ((!sFTime.isEmpty())&&(sTitle.isEmpty())) return view2;
			if ((!sFTime.isEmpty())&&(!sTitle.isEmpty())) return view;
			return null;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			int cnt = 0;
			if (mGDList.mBooksList != null) cnt = mGDList.mBooksList.getCount();
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

	class OpenBookFromDBXAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			int cnt = 0;
			if (mDBXList.mLFR != null) cnt = mDBXList.mLFR.size();
			return cnt;
		}

		public Object getItem(int position) {
			int cnt = 0;
			int pos = position;
			if (mDBXList.mLFR != null) cnt = mDBXList.mLFR.size();
			if ( pos<0 || pos>=cnt )
				return null;
			return mDBXList.mLFR.get(pos);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			View view1;
			View view2;
			int res = R.layout.open_book_from_gd_item;
			int res1 = R.layout.open_book_from_gd_item1;
			int res2 = R.layout.open_book_from_gd_item2;
			view = mInflater.inflate(res, null);
			TextView fileView = (TextView)view.findViewById(R.id.file_name);
			TextView descrView = (TextView)view.findViewById(R.id.file_descr);
			TextView timeView = (TextView)view.findViewById(R.id.file_time);
			view1 = mInflater.inflate(res1, null);
			TextView fileView1 = (TextView)view1.findViewById(R.id.file_name);
			view2 = mInflater.inflate(res2, null);
			TextView fileView2 = (TextView)view2.findViewById(R.id.file_name);
			TextView timeView2 = (TextView)view2.findViewById(R.id.file_time);
			com.dropbox.core.v2.files.Metadata md = null;
			String sFile = "";
			if (mDBXList.mLFR != null) md = mDBXList.mLFR.get(position);
			String sTitle = "";
			String sFTime = "";
			if ( md!=null ) {
				Date dt = null;
				sFile = StrUtils.getNonEmptyStr(md.getName(),true);
				if (md instanceof com.dropbox.core.v2.files.FileMetadata) {
					com.dropbox.core.v2.files.FileMetadata fmd = (com.dropbox.core.v2.files.FileMetadata) md;
					if (fmd.getMediaInfo()!=null)
						sTitle = fmd.getMediaInfo().toString();
					dt = fmd.getServerModified();
				}
				if (md instanceof com.dropbox.core.v2.files.FolderMetadata) {
					com.dropbox.core.v2.files.FolderMetadata fmd = (com.dropbox.core.v2.files.FolderMetadata) md;
					sFile = "["+sFile+"]";
				}
				if (sTitle == null) sTitle ="";
				final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
				CharSequence sFName0 = "";
				if ( dt != null) sFName0 = dfmt.format("yyyy-MM-dd kk:mm:ss", dt);
				sFTime = sFName0.toString();
				if (sFTime == null) sFTime ="";
				if ((sFTime.isEmpty())&&(sTitle.isEmpty())) {
					fileView1.setText(sFile);
				}
				if ((!sFTime.isEmpty())&&(sTitle.isEmpty())) {
					fileView2.setText(sFile);
					timeView2.setText(sFTime);
				}
				if ((!sFTime.isEmpty())&&(!sTitle.isEmpty())) {
					fileView.setText(sFile);
					descrView.setText(StrUtils.textShrinkLines(sTitle, true));
					timeView.setText(sFTime);
				}
			} else {
				fileView1.setText(sFile);
				descrView.setText("");
				timeView.setText("");
			}
			if ((sFTime.isEmpty())&&(sTitle.isEmpty())) return view1;
			if ((!sFTime.isEmpty())&&(sTitle.isEmpty())) return view2;
			if ((!sFTime.isEmpty())&&(!sTitle.isEmpty())) return view;
			return null;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			int cnt = 0;
			if (mDBXList.mLFR != null) cnt = mDBXList.mLFR.size();
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

	class OpenBookFromGDList extends BaseListView {

		public com.google.android.gms.drive.MetadataBuffer mBooksList;
		public ArrayList<com.google.android.gms.drive.DriveId> mDriveIdList;
		public ArrayList<String> mDriveStrList;
		public ArrayList<com.google.android.gms.drive.DriveId> mDriveIdList1;
		public ArrayList<String> mDriveStrList1;
		public com.google.android.gms.drive.DriveId driveToGoTo;

		public boolean ismWholeSearchW() {
			if (mDriveIdList.size()<=1) return true;
			return mFoundWhole;
		}

		private String getFolderText() {
			String res = "";
			String sRoot = "root";
			if (mFoundWhole) sRoot = "search";
			for (String didmd: mDriveStrList) {
				res = res + "\\" + didmd.toString();
				if (res.equals("\\"+sRoot)) res = "";
				if (res.equals("\\root")) res = "";
			}
			if (mDriveStrList.size()<=1) res=sRoot;
			return res;
		}

		public void listUpdated() {
			updateAdapter(new OpenBookFromGDAdapter());
			lblFolder.setText(getFolderText());
		}

		private ListAdapter mAdapter;
		private OpenBookFromCloudDlg uDDlg;

		public OpenBookFromGDList( Context context, OpenBookFromCloudDlg udd) {
			super(context, true);
			uDDlg = udd;
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					openContextMenu(OpenBookFromGDList.this);
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			com.google.android.gms.drive.Metadata md = null;
			if (mBooksList != null) md = mBooksList.get(position);
			if (md != null)
				if (md.getMimeType().equals("application/vnd.google-apps.folder")) {
					btnFolder.setChecked(true);
					driveToGoTo = md.getDriveId();
					mDriveIdList.add(md.getDriveId());
					mDriveStrList.add(md.getTitle());
					mBooksList = null;
					listUpdated();
					mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_FOLDER_CONTENTS, OpenBookFromCloudDlg.this);
				} else {
					mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_BOOK_FILE, md);
					dismiss();
				}
			return true;
		}

		public void updateAdapter( OpenBookFromGDAdapter adapter ) {
			mAdapter = adapter;
			setAdapter(mAdapter);
		}

	}

	class OpenBookFromDBXList extends BaseListView {

		public ArrayList<String> mDriveStrList;
		public ArrayList<String> mDriveStrList1;
		public List<com.dropbox.core.v2.files.Metadata> mLFR;

		public boolean ismWholeSearchW() {
			return false;
//            if (mLFR == null) return false;
//			if (mLFR.size()<=1) return true;
//			return mFoundWhole;
		}

		private String getFolderText() {
			String res = "";
			String sRoot = "root";
			if (mFoundWhole) sRoot = "search";
			for (String didmd: mDriveStrList) {
				res = res + "\\" + didmd.toString();
				if (res.equals("\\"+sRoot)) res = "";
				if (res.equals("\\root")) res = "";
			}
			//if (mDriveStrList.size()<=1) res=sRoot;
			return res;
		}

		public void listUpdated() {
			updateAdapter(new OpenBookFromDBXAdapter());
			lblFolder.setText(getFolderText());
		}

		private ListAdapter mAdapter;
		private OpenBookFromCloudDlg uDDlg;

		public OpenBookFromDBXList( Context context, OpenBookFromCloudDlg udd) {
			super(context, true);
			uDDlg = udd;
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
											   int position, long arg3) {
					openContextMenu(OpenBookFromDBXList.this);
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			com.dropbox.core.v2.files.Metadata md = null;
			if (mLFR != null) md = mLFR.get(position);
			if (md != null)
				if (md instanceof com.dropbox.core.v2.files.FolderMetadata) {
					btnFolder.setChecked(true);
					mDriveStrList.add(md.getName());
					mLFR = null;
					listUpdated();
					String sFolder=getFolderText();
					CloudAction.dbxLoadFolderContents(mCoolReader, OpenBookFromCloudDlg.this, sFolder, "");
				} else {
					String sFolder=getFolderText();
					CloudAction.dbxDownloadFile(mCoolReader, OpenBookFromCloudDlg.this, sFolder, md);
					dismiss();
				}
			return true;
		}

		public void updateAdapter( OpenBookFromDBXAdapter adapter ) {
			mAdapter = adapter;
			setAdapter(mAdapter);
		}

	}

	private View initCommon(final CoolReader activity) {
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;

		View frame = mInflater.inflate(R.layout.open_book_from_cloud_dialog, null);
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.book_list);
		btnWhole = (RadioButton)frame.findViewById(R.id.rb_whole);
		btnWhole.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					mWholeSearch = true;
				}
			}
		});
		btnFolder = (RadioButton)frame.findViewById(R.id.rb_folder);
		btnFolder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					mWholeSearch = false;
				}
			}
		});
		btnFolder.setChecked(true);
		btnFind = (ImageButton)frame.findViewById(R.id.do_find);
		btnRoot = (ImageButton)frame.findViewById(R.id.goto_root);
		btnUp = (ImageButton)frame.findViewById(R.id.goto_up);
		lblFolder = (TextView)frame.findViewById(R.id.lbl_folder_text);
		rgScope = (RadioGroup) frame.findViewById(R.id.rg_scope);

		edtFind = (EditText)frame.findViewById(R.id.find_text);
		edtFind.setText("");
		return frame;
	}

	private BaseListView initForGD(com.google.android.gms.drive.MetadataBuffer mdb,
						   ArrayList<com.google.android.gms.drive.DriveId> adid,
						   ArrayList<String> adidmd) {
		mGDList = new OpenBookFromGDList(mCoolReader, this);
		mGDList.mBooksList = mdb;
		mGDList.mDriveIdList = adid;
		mGDList.mDriveStrList = adidmd;

		mGDList.mDriveIdList1 = new ArrayList<com.google.android.gms.drive.DriveId>(adid);
		mGDList.mDriveStrList1 = new ArrayList<String>(adidmd);

		mGDList.setAdapter(new OpenBookFromGDAdapter());

		btnFind.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (btnWhole.isChecked()) {
					mGDList.mDriveIdList.clear();
					mGDList.mDriveStrList.clear();
					mGDList.mDriveIdList.add(mGDList.mDriveIdList1.get(0));
					mGDList.mDriveStrList.add("search");
				}
				mGDList.mBooksList = null;
				mGDList.listUpdated();
				mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_FOLDER_CONTENTS, OpenBookFromCloudDlg.this);
			}
		});

		btnRoot.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				mGDList.mDriveIdList = new ArrayList<com.google.android.gms.drive.DriveId>(mGDList.mDriveIdList1);
				mGDList.mDriveStrList = new ArrayList<String>(mGDList.mDriveStrList1);
				mGDList.driveToGoTo = mGDList.mDriveIdList.get(mGDList.mDriveIdList.size()-1);
				btnFolder.setChecked(true);
				mWholeSearch = false;
				mFoundWhole = false;
				mGDList.mBooksList = null;
				edtFind.setText("");
				mGDList.listUpdated();
				mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_FOLDER_CONTENTS, OpenBookFromCloudDlg.this);
			}
		});

		btnUp.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (mGDList.mDriveIdList.size()>0) {
					btnFolder.setChecked(true);
					mGDList.driveToGoTo = mGDList.mDriveIdList.get(mGDList.mDriveIdList.size() - 2);
					mGDList.mDriveIdList.remove(mGDList.mDriveIdList.size()-1);
					mGDList.mDriveStrList.remove(mGDList.mDriveStrList.size()-1);
					mGDList.mBooksList = null;
					listUpdated();
					mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_FOLDER_CONTENTS, OpenBookFromCloudDlg.this);
				}
			}
		});

		lblFolder.setText(mGDList.getFolderText());
		mGDList.driveToGoTo = mGDList.mDriveIdList.get(mGDList.mDriveIdList.size()-1);
		return mGDList;
	}

	private BaseListView initForDBX(List<com.dropbox.core.v2.files.Metadata> lfr) {
		mDBXList = new OpenBookFromDBXList(mCoolReader, this);
		mDBXList.mLFR = lfr;
		mDBXList.mDriveStrList = new ArrayList<String>();
		mDBXList.mDriveStrList1 = new ArrayList<String>();
		mDBXList.setAdapter(new OpenBookFromDBXAdapter());

		System.out.println("initForDBX");

		btnFind.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				btnFolder.setChecked(true);
				mDBXList.mDriveStrList.clear();
				mDBXList.mLFR = null;
				listUpdated();
				CloudAction.dbxLoadFolderContents(mCoolReader, OpenBookFromCloudDlg.this, "", edtFind.getText().toString());
			}
		});

		btnRoot.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				btnFolder.setChecked(true);
				mDBXList.mDriveStrList.clear();
				mDBXList.mLFR = null;
				listUpdated();
				CloudAction.dbxLoadFolderContents(mCoolReader, OpenBookFromCloudDlg.this, "", "");
			}
		});

		btnUp.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (mDBXList.mDriveStrList.size()>0) {
					btnFolder.setChecked(true);
					mDBXList.mDriveStrList.remove(mDBXList.mDriveStrList.size()-1);
					mDBXList.mLFR = null;
					listUpdated();
					String sFolder=mDBXList.getFolderText();
					CloudAction.dbxLoadFolderContents(mCoolReader, OpenBookFromCloudDlg.this, sFolder, "");
				}
			}
		});

		lblFolder.setText(mDBXList.getFolderText());
		rgScope.removeView(btnWhole);
		rgScope.removeView(btnFolder);
		return mDBXList;
	}

	public OpenBookFromCloudDlg(final CoolReader activity,
								com.google.android.gms.drive.MetadataBuffer mdb,
								ArrayList<com.google.android.gms.drive.DriveId> adid,
                                ArrayList<String> adidmd)
	{
		super("OpenBookFromCloudDlg", activity, activity.getResources().getString(R.string.win_title_open_book_from_cloud)
				+" - Google One", false, true);
		View v = initCommon(activity);
		BaseListView blv = initForGD(mdb, adid, adidmd);
		ViewGroup body = (ViewGroup)v.findViewById(R.id.book_list);
		body.addView(blv);
		setView(v);
		setFlingHandlers(blv, null, null);
		btnFind.requestFocus();
	}

	public OpenBookFromCloudDlg(final CoolReader activity,
								List<com.dropbox.core.v2.files.Metadata> lfr)
	{
		super("OpenBookFromCloudDlg", activity, activity.getResources().getString(R.string.win_title_open_book_from_cloud)
				+" - DropBox", false, true);
		View v = initCommon(activity);
		BaseListView blv = initForDBX(lfr);
		ViewGroup body = (ViewGroup)v.findViewById(R.id.book_list);
		body.addView(blv);
		setView(v);
		setFlingHandlers(blv, null, null);
		btnFind.requestFocus();
	}

	@Override
	protected void onPositiveButtonClick() {
		// add Dict
		OpenBookFromCloudDlg.this.dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		OpenBookFromCloudDlg.this.dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating OpenBookFromCloudDlg");
		setTitle(mCoolReader.getResources().getString(R.string.win_title_open_book_from_cloud));
		setCancelable(true);
		super.onCreate(savedInstanceState);
	}

}
