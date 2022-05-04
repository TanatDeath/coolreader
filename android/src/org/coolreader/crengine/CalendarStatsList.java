package org.coolreader.crengine;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CRDBService;
import org.coolreader.db.CalendarStats;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CalendarStatsList extends BaseListView {

	private CoolReader mCoolReader;
	List<CalendarStats> mCalendarStatsList;
	private BaseDialog mDlg;

	static class CalendarStatsAdapter extends BaseAdapter {

		HashMap<Integer, Integer> themeColors;
		int colorIcon = Color.GRAY;
		boolean isEInk = false;

		public CalendarStatsAdapter(CoolReader cr, BaseDialog dlg, List<CalendarStats> cs) {
			isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
			themeColors = Utils.getThemeColors(cr, isEInk);
			colorIcon = themeColors.get(R.attr.colorIcon);
			mInflater = (LayoutInflater) dlg.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mCalendarStats = cs;
		}

		LayoutInflater mInflater;
		List<CalendarStats> mCalendarStats;

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return mCalendarStats.size();
		}

		public Object getItem(int position) {
			return mCalendarStats.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public final static int ITEM_POSITION=0;

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.calendar_stats_entry;
			view = mInflater.inflate(res, null);
			TextView labelDate = view.findViewById(R.id.tv_calendar_stats_date);
			if (!mCalendarStats.get(position).sameDate) {
				long val = mCalendarStats.get(position).readDate;
				java.util.Date date = new java.util.Date(val);
				SimpleDateFormat df2 = new SimpleDateFormat("yyyy.MM.dd");
				String dateText = df2.format(date);
				labelDate.setText(dateText);
			}

			String bookTitle = mCalendarStats.get(position).bookTitle;

			if (StrUtils.isEmptyStr(bookTitle))
				labelDate.setTextColor(Color.argb(100,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon)));
			else
				labelDate.setTextColor(colorIcon);
			TextView labelTime = view.findViewById(R.id.tv_calendar_stats_time);
			int minutes = (int) ((mCalendarStats.get(position).timeSpentSec) / 60);
			int hours = (int) ((mCalendarStats.get(position).timeSpentSec) / 60 / 60);
			if (hours>0) {
				minutes = minutes - (hours * 60);
				labelTime.setText("" + hours + "h "+minutes+"m");
			} else {
				if (minutes == 0)
					labelTime.setText("");
				else
					labelTime.setText("" + minutes + "m");
			}
			TextView labelBook = view.findViewById(R.id.tv_calendar_stats_book);
			labelBook.setText(mCalendarStats.get(position).bookTitle);
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mCalendarStats.size() == 0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}

	}

	public CalendarStatsList(CoolReader coolReader, BaseDialog dlg, List<CalendarStats> cs) {
		super(coolReader, true);
		this.mCoolReader = coolReader;
		this.mDlg = dlg;
		setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		setLongClickable(true);
		setAdapter(new CalendarStatsAdapter(coolReader, dlg, cs));
		mCalendarStatsList = cs;
		setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
			//openContextMenu(DictList.this);
			return true;
		});
	}

	@Override
	public boolean performItemClick(View view, int position, long id) {
		long bookId = mCalendarStatsList.get(position).bookId;
		if (bookId > 0) {
			mCoolReader.waitForCRDBService(() -> mCoolReader.getDB().loadFileInfoById(bookId,
				new CRDBService.FileInfo1LoadingCallback() {
					@Override
					public void onFileInfoLoaded(FileInfo fileInfo) {
						mDlg.dismiss();
						mCoolReader.loadDocument(fileInfo, true);
					}

					@Override
					public void onFileInfoListLoadBegin() {

					}
				}
			));
		}
		return true;
	}
}
