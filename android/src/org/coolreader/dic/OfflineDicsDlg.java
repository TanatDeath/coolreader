package org.coolreader.dic;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.Scanner;
import org.coolreader.utils.FileUtils;
import org.coolreader.crengine.ProgressDialog;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OfflineDicsDlg extends BaseDialog {
	CoolReader mCoolReader;
	private LayoutInflater mInflater;
	View mDialogView;
	ArrayList<OfflineInfo> mDics;
	private DictList mList;
	Scanner.ScanControl mScanControl;

	public static ProgressDialog progressDlg;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	@Override
	protected void onPositiveButtonClick()
	{
        cancel();
	}

	@Override
	protected void onNegativeButtonClick()
	{
	   cancel();
	}

	public final static int ITEM_POSITION=0;

	class DictListAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return mDics.size();
		}

		public Object getItem(int position) {
			if (position < 0 || position >= mDics.size())
				return null;
			return mDics.get(position);
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
			int res = R.layout.star_dict_item;
			view = mInflater.inflate(res, null);
			TextView tvTitle = view.findViewById(R.id.dict_item_title);
			TextView tvFilepath = view.findViewById(R.id.dict_item_filepath);
			TextView tvDesc = view.findViewById(R.id.dict_item_desc);
			TextView tvVersion = view.findViewById(R.id.dict_item_version);
			Button btnConvert = view.findViewById(R.id.btn_convert);
			TextView tvConverted = view.findViewById(R.id.tv_converted);
			OfflineInfo sdi = (OfflineInfo) getItem(position);
			tvTitle.setText(sdi.dicName);
			tvFilepath.setText(sdi.dicNameWOExt);
			tvVersion.setText(sdi.dicVersion + "; " + mActivity.getString(R.string.word_count) + ": " + sdi.wordCount);
			tvDesc.setText(sdi.dicDescription);
			btnConvert.setText(R.string.convert_dic);
			int colorGray = themeColors.get(R.attr.colorThemeGray2);
			btnConvert.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
			if (isEInk) Utils.setSolidButtonEink(btnConvert);
			btnConvert.setPadding(10, 20, 10, 20);
			tvConverted.setPadding(10, 20, 10, 20);
			mActivity.tintViewIcons(view);
			btnConvert.setOnClickListener(v -> {
				// TODO: Cancel listener - предлагать удалить недопреобразованное
//				progressDlg = ProgressDialog.show(mActivity,
//						mActivity.getString(R.string.long_op),
//						mActivity.getString(R.string.long_op),
//						true, true, null);
				Engine.ProgressControl progress = mActivity.mEngine.createProgress(R.string.progress_scanning, mScanControl);
				mActivity.getDB().convertStartDictDic(sdi.dicPath, sdi.dicNameWOExt, mScanControl, progress, o -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					if (o != null) {
						BackgroundThread.instance().postGUI(() -> {
							if ("".equals((String) o))
								mActivity.showToast(mActivity.getString(R.string.success_ext));
							else
								//mActivity.getString(R.string.error) - need to covert messages
								mActivity.showToast((String) o);
						});
					}
				});
			});
			File fconverted = new File(sdi.dicPath + "/" + sdi.dicNameWOExt + ".db");
			if (fconverted.exists()) {
				tvConverted.setText(R.string.converted_dic);
				tvConverted.setVisibility(View.VISIBLE);
			} else {
				tvConverted.setVisibility(View.INVISIBLE);
			}
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mDics.size() == 0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class DictList extends BaseListView {

		public DictList(Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new DictListAdapter());
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
//				mEditView.setText(mSearches.get(position));
//				searchPagesClick();
				return true;
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			//mEditView.setText(mSearches.get(position));
			return true;
		}
	}

	public static ArrayList<OfflineInfo> fillOfflineDics() {
		ArrayList<OfflineInfo> dics = new ArrayList<>();
		ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.OfflineDicsDirs, true);
		for (String dir: tDirs) {
			ArrayList<File> files = FileUtils.searchFiles(new File(dir), ".*\\.dict|.*\\.db");
			for (File f: files) {
				String dicNameWOExt = Utils.getFileNameWOExtension(f);
				String fileExt = Utils.getFileExtension(f);
				String dicPath =f.getParent();
				boolean dictExists = new File(dicPath + "/" + dicNameWOExt + ".dict").exists();
				if ((!fileExt.equals("db")) || (!dictExists)) {
					OfflineInfo sdi = new OfflineInfo();
					sdi.dicNameWOExt = dicNameWOExt;
					sdi.dicPath = dicPath;
					sdi.dbExists = new File(sdi.dicPath + "/" + sdi.dicNameWOExt + ".db").exists();
					File ifo = new File(sdi.dicPath + "/" + sdi.dicNameWOExt + ".ifo");
					sdi.ifoExists = ifo.exists();
					sdi.dictExists = dictExists;
					if (sdi.ifoExists) {
						List<String> allLines = FileUtils.readLinesFromFile(ifo.getAbsolutePath());
						for (String s : allLines) {
							if (s.indexOf("=") > 0) {
								String s1 = s.substring(0, s.indexOf("="));
								String s2 = s.substring(s.indexOf("=") + 1, s.length());
								if (s1.equals("bookname")) sdi.dicName = s2;
								if (s1.equals("date")) sdi.dicDate = StrUtils.parseDate(s2);
								if (s1.equals("description")) sdi.dicDescription = s2;
								if (s1.equals("version")) sdi.dicVersion = s2;
								try {
									if (s1.equals("wordcount"))
										sdi.wordCount = Integer.parseInt(s2);
								} catch (Exception e) {
									sdi.wordCount = 0;
								}
							}
						}
					}
					dics.add(sdi);
				}
			}
		}
		return dics;
	}

	public OfflineDicsDlg(CoolReader coolReader)
	{
		super("OfflineDicsDlg", coolReader, coolReader.getResources().getString(R.string.offline_dics), true, false);
        setCancelable(true);
		this.mCoolReader = coolReader;
		this.mScanControl = new Scanner.ScanControl();
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mCoolReader, isEInk);
		mDics = fillOfflineDics();
        mInflater = LayoutInflater.from(getContext());
        mDialogView = mInflater.inflate(R.layout.star_dicts_dialog, null);

		ViewGroup body = mDialogView.findViewById(R.id.dict_list);
		mList = new DictList(mActivity, false);
		body.addView(mList);
		mCoolReader.tintViewIcons(mDialogView);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setView(mDialogView);
	}
}
