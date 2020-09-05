package org.coolreader.cloud;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
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
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.yandex.YNDListFiles;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OpenBookFromCloudDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private OpenBookFromDBXList mDBXList;
	private OpenBookFromYNDList mYNDList;

	public boolean mWholeSearch = false;
	private TextView lblFolder;
	public EditText edtFind;
	private RadioButton btnWhole;
	private RadioButton btnFolder;
	private ImageButton btnFind;
	private ImageButton btnRoot;
	private ImageButton btnUp;
	private RadioGroup rgScope;
	private Button btnSaveOpenedBook;
	private LinearLayout llSaveBook;
	private TextView tvCurrentBook;
	private FileInfo bookToSave;

	public boolean mFoundWhole = false;

	public final static int ITEM_POSITION=0;

	public void setDBXLfrList(List<com.dropbox.core.v2.files.Metadata> lfr) {
		if (mDBXList != null) mDBXList.mLFR = lfr;
	}

	public void setYNDLfrList(YNDListFiles lfr) {
		if (mYNDList != null) mYNDList.mLFR = lfr;
	}

	public void listUpdated() {
		if (mDBXList != null) mDBXList.listUpdated();
		if (mYNDList != null) mYNDList.listUpdated();
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

	class OpenBookFromYNDAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			int cnt = 0;
			if (mYNDList.mLFR != null) cnt = mYNDList.mLFR.fileList.size();
			return cnt;
		}

		public Object getItem(int position) {
			int cnt = 0;
			int pos = position;
			if (mYNDList.mLFR != null) cnt = mYNDList.mLFR.fileList.size();
			if ( pos<0 || pos>=cnt )
				return null;
			return mYNDList.mLFR.fileList.get(pos);
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
			CloudFileInfo md = null;
			String sFile = "";
			if (mYNDList.mLFR != null) md = mYNDList.mLFR.fileList.get(position);
			String sTitle = "";
			String sFTime = "";
			if ( md!=null ) {
				Date dt = null;
				sFile = StrUtils.getNonEmptyStr(md.name,true);
				sTitle = "";
				if (md.type.equals("dir")) {
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
			if (mYNDList.mLFR != null) cnt = mYNDList.mLFR.fileList.size();
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

	class OpenBookFromDBXList extends BaseListView {

		public ArrayList<String> mDriveStrList;
		public ArrayList<String> mDriveStrList1;
		public List<com.dropbox.core.v2.files.Metadata> mLFR;

		public boolean ismWholeSearchW() {
			return false;
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
					CloudAction.dbxDownloadFile(mCoolReader, OpenBookFromCloudDlg.this, sFolder, md, false);
					dismiss();
				}
			return true;
		}

		public void updateAdapter( OpenBookFromDBXAdapter adapter ) {
			mAdapter = adapter;
			setAdapter(mAdapter);
		}

	}

	class OpenBookFromYNDList extends BaseListView {

		public ArrayList<String> mDriveStrList;
		public ArrayList<String> mDriveStrList1;
		public YNDListFiles mLFR;

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
			updateAdapter(new OpenBookFromYNDAdapter());
			lblFolder.setText(getFolderText());
		}

		private ListAdapter mAdapter;
		private OpenBookFromCloudDlg uDDlg;

		public OpenBookFromYNDList( Context context, OpenBookFromCloudDlg udd) {
			super(context, true);
			uDDlg = udd;
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
											   int position, long arg3) {
					openContextMenu(OpenBookFromYNDList.this);
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			CloudFileInfo md = null;
			if (mLFR != null) md = mLFR.fileList.get(position);
			if (md != null)
				if (md.type.equals("dir")) {
					btnFolder.setChecked(true);
					mDriveStrList.add(md.name);
					mLFR = null;
					listUpdated();
					String sFolder=getFolderText();
					CloudAction.yndLoadFolderContents(mCoolReader, OpenBookFromCloudDlg.this, sFolder, "");
				} else {
					String sFolder=getFolderText();
					CloudAction.yndDownloadFile(mCoolReader, OpenBookFromCloudDlg.this, sFolder, md, false);
					dismiss();
				}
			return true;
		}

		public void updateAdapter( OpenBookFromYNDAdapter adapter ) {
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
		llSaveBook = (LinearLayout)frame.findViewById(R.id.ll_save_book);
		btnSaveOpenedBook = (Button)frame.findViewById(R.id.btn_save_book_to_cloud_ynd);
		tvCurrentBook = (TextView)frame.findViewById(R.id.lbl_save_book);
		if (bookToSave == null)
			((ViewGroup)llSaveBook.getParent()).removeView(llSaveBook);
		else {
			String sFName = "";
			if (!StrUtils.isEmptyStr(bookToSave.arcname)) sFName = bookToSave.arcname;
			else sFName = bookToSave.pathname;
			if (!StrUtils.isEmptyStr(bookToSave.title)) sFName = bookToSave.title +" ("+sFName+")";
			tvCurrentBook.setText(StrUtils.getNonEmptyStr(sFName,true).replace("/storage/emulated", "/s/e"));

		}
		return frame;
	}

	private BaseListView initForDBX(List<com.dropbox.core.v2.files.Metadata> lfr) {
		mDBXList = new OpenBookFromDBXList(mCoolReader, this);
		mDBXList.mLFR = lfr;
		mDBXList.mDriveStrList = new ArrayList<String>();
		mDBXList.mDriveStrList1 = new ArrayList<String>();
		OpenBookFromDBXAdapter dbxa = new OpenBookFromDBXAdapter();
		mDBXList.setAdapter(dbxa);
		dbxa.notifyDataSetChanged();

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
		rgScope.removeView(llSaveBook);
		return mDBXList;
	}

	private BaseListView initForYND(YNDListFiles lfr, String homeFolder) {
		mYNDList = new OpenBookFromYNDList(mCoolReader, this);
		mYNDList.mLFR = lfr;
		mYNDList.mDriveStrList = new ArrayList<String>();
		mYNDList.mDriveStrList1 = new ArrayList<String>();
		OpenBookFromYNDAdapter ynda = new OpenBookFromYNDAdapter();
		mYNDList.setAdapter(ynda);
		ynda.notifyDataSetChanged();

		System.out.println("initForYND");

		if (
			(!StrUtils.isEmptyStr(homeFolder)) &&
			(!StrUtils.getNonEmptyStr(homeFolder,true).equals("/"))
		) for (String s: homeFolder.split("\\/"))
			if (!StrUtils.isEmptyStr(s)) mYNDList.mDriveStrList.add(s);

		btnFind.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				btnFolder.setChecked(true);
				mYNDList.mDriveStrList.clear();
				mYNDList.mLFR = null;
				listUpdated();
				CloudAction.yndLoadFolderContents(mCoolReader, OpenBookFromCloudDlg.this, "", edtFind.getText().toString());
			}
		});

		btnRoot.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				btnFolder.setChecked(true);
				mYNDList.mDriveStrList.clear();
				mYNDList.mLFR = null;
				listUpdated();
				Properties props = new Properties(mCoolReader.settings());
				String homeFolder = props.getProperty(Settings.PROP_CLOUD_YND_HOME_FOLDER, "");
				if (
						(!StrUtils.isEmptyStr(homeFolder)) &&
								(!StrUtils.getNonEmptyStr(homeFolder,true).equals("/"))
				) for (String s: homeFolder.split("\\/"))
					if (!StrUtils.isEmptyStr(s)) mYNDList.mDriveStrList.add(s);
				CloudAction.yndLoadFolderContents(mCoolReader, OpenBookFromCloudDlg.this, homeFolder, "");
			}
		});

		btnRoot.setOnLongClickListener(new Button.OnLongClickListener() {
			public boolean onLongClick(View v) {
				if (mYNDList == null) return false;
				mCoolReader.askConfirmation(mCoolReader.getString(R.string.ynd_set_def), new Runnable() {
					@Override
					public void run() {
						String s = "";
						for (String ss: mYNDList.mDriveStrList) {
							s = s + "/" + ss;
						}
						Properties props = new Properties(mCoolReader.settings());
						props.setProperty(Settings.PROP_CLOUD_YND_HOME_FOLDER, s);
						mCoolReader.setSettings(props, -1, true);
						mCoolReader.showToast(R.string.ok);
					}
				});
				return true;
			}
		});

		btnUp.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (mYNDList.mDriveStrList.size()>0) {
					btnFolder.setChecked(true);
					mYNDList.mDriveStrList.remove(mYNDList.mDriveStrList.size()-1);
					mYNDList.mLFR = null;
					listUpdated();
					String sFolder=mYNDList.getFolderText();
					CloudAction.yndLoadFolderContents(mCoolReader, OpenBookFromCloudDlg.this, sFolder, "");
				}
			}
		});

		lblFolder.setText(mYNDList.getFolderText());
		rgScope.removeView(btnWhole);
		rgScope.removeView(btnFolder);
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGray = a.getColor(0, Color.GRAY);
		int colorGrayC = a.getColor(1, Color.GRAY);
		int colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		btnSaveOpenedBook.setBackgroundColor(colorGrayCT2);
		btnSaveOpenedBook.setTextColor(colorIcon);
		btnSaveOpenedBook.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (bookToSave != null) {
					btnSaveOpenedBook.setVisibility(View.INVISIBLE);
					BackgroundThread.instance().postGUI(new Runnable() {
						@Override
						public void run() {
							btnSaveOpenedBook.setEnabled(true);
							btnSaveOpenedBook.setVisibility(View.VISIBLE);
						}
					}, 5000);
					listUpdated();
					String sFolder = mYNDList.getFolderText();
					CloudAction.yndSaveBookThenLoadFolderContents(mCoolReader, bookToSave, OpenBookFromCloudDlg.this, sFolder, "", false);
				}
			}
		});
		return mYNDList;
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

	public OpenBookFromCloudDlg(final CoolReader activity, YNDListFiles lfr, FileInfo bookToS, String homeFolder)
	{
		super("OpenBookFromCloudDlg", activity, activity.getResources().getString(R.string.win_title_open_book_from_cloud)
				+" - Yandex", false, true);
		bookToSave = bookToS;
		View v = initCommon(activity);
		BaseListView blv = initForYND(lfr, homeFolder);
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
