package org.coolreader.crengine;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CalendarStats;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class ReadCalendarDlg extends BaseDialog {
	CoolReader mCoolReader;
	private LayoutInflater mInflater;
	View mDialogView;
	public ViewGroup mBody;
	TextView tvCalc;
	CalendarStatsList mCalendarStatsList = null;
	public java.util.Date curDate;
	public java.util.Date curDateEnd;
	public java.util.Date curDateEndForCalc; // plus day - for accurate calc
	//86400000 = 1 day
	public long periodSeconds = 0;

	public static final Logger log = L.create("rcd");

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	@Override
	protected void onPositiveButtonClick() {
		cancel();
	}

	@Override
	protected void onNegativeButtonClick() {
		cancel();
	}

	public final static int ITEM_POSITION=0;

	public ReadCalendarDlg(CoolReader coolReader) {
		super(coolReader, coolReader.getResources().getString(R.string.read_calendar_dlg), true, true);
		whenCreate(coolReader);
	}

	public static Handler mHandler = new Handler();

	public Runnable handleOk = () -> {
		onPositiveButtonClick();
	};

	private void showDateSpan(String sdate01, String sdate) {
		calcPeriodSeconds();
		mActivity.waitForCRDBService(() -> mActivity.getDB().getCalendarEntries(
				StrUtils.parseDateLong(sdate01), StrUtils.parseDateLong(sdate), list -> {
					long date = 0;
					long seconds = 0;
					for (CalendarStats cs :list) {
						cs.sameDate = true;
						if (cs.readDate != date) cs.sameDate = false;
						date = cs.readDate;
						seconds = seconds + cs.timeSpentSec;
					}
					int minutes = (int) (seconds / 60);
					int hours = (int) (seconds / 60 / 60);
					String hm = "";
					if (hours>0) {
						minutes = minutes - (hours * 60);
						hm = hours + "h "+minutes+"m";
					} else {
						if (minutes != 0)
							hm = minutes + "m";
					}
					if (seconds == 0) tvCalc.setText("");
					else
						tvCalc.setText(mCoolReader.getString(R.string.month_read_percent) + " " +
							hm + " (" +
							(String.format("%1$.2f%%",Double.valueOf(seconds) *
							Double.valueOf(100) / Double.valueOf(periodSeconds))) + ")");
					mCalendarStatsList = new CalendarStatsList(mActivity, this, list);
					mBody.addView(mCalendarStatsList);
				}
			)
		);
	}

	private void calcPeriodSeconds() {
		long lCurDate = StrUtils.parseDateLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(curDate));
		long lCurDateE = StrUtils.parseDateLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(curDateEndForCalc));
		if (lCurDate == lCurDateE)
			lCurDateE = lCurDateE + 86400000 - 1;
		periodSeconds = lCurDateE - lCurDate;
		periodSeconds = periodSeconds / 1000;
	}

	private void whenCreate(CoolReader coolReader) {
		setCancelable(true);
		this.mCoolReader = coolReader;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mCoolReader, isEInk);
		mInflater = LayoutInflater.from(getContext());
		mDialogView = mInflater.inflate(R.layout.calendar_stats_dialog, null);
		mBody = mDialogView.findViewById(R.id.calendar_stats_list);
		Button btnPrev = mDialogView.findViewById(R.id.btn_calendar_stats_prev);
		Button btnNext = mDialogView.findViewById(R.id.btn_calendar_stats_next);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT = Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		btnPrev.setBackgroundColor(colorGrayC);
		btnNext.setBackgroundColor(colorGrayC);
		if (isEInk) Utils.setSolidButtonEink(btnPrev);
		if (isEInk) Utils.setSolidButtonEink(btnNext);
		TextView tv = mDialogView.findViewById(R.id.tv_calendar_stats_month);
		tvCalc = mDialogView.findViewById(R.id.tv_calendar_stats_calc);
		String sdate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new java.util.Date());
		String sdate01 = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new java.util.Date());
		String sdateM = new SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(new java.util.Date());
		Calendar c = Calendar.getInstance();   // this takes current date
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		curDate = c.getTime();
		curDateEnd = new java.util.Date();
		curDateEndForCalc = curDateEnd;
		tv.setText(sdateM);
		sdate01 = sdate01.substring(0,6) + "01";
		showDateSpan(sdate01, sdate);
		btnPrev.setOnClickListener(v -> {
			curDate = Utils.getPreviousMonth(curDate);
			curDateEnd = Utils.getLastDayOfMonth(curDate);
			curDateEndForCalc = Utils.getNextMonth(curDate);
			String ssdate01 = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(curDate);
			String ssdate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(curDateEnd);
			String ssdateM = new SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(curDate);
			tv.setText(ssdateM);
			mBody.removeView(mCalendarStatsList);
			showDateSpan(ssdate01, ssdate);
		});
		btnNext.setOnClickListener(v -> {
			java.util.Date ndate = Utils.getNextMonth(curDate);
			if (ndate.after(new java.util.Date())) return;
			curDate = ndate;
			String ssdate01 = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(curDate);
			java.util.Date ddate = new java.util.Date();
			curDateEnd = ddate.before(Utils.getLastDayOfMonth(curDate))? ddate: Utils.getLastDayOfMonth(curDate);
			curDateEndForCalc = ddate.before(Utils.getLastDayOfMonth(curDate))? ddate: Utils.getNextMonth(curDate);
			String ssdate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(
					curDateEnd);
			String ssdateM = new SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(curDate);
			tv.setText(ssdateM);
			mBody.removeView(mCalendarStatsList);
			showDateSpan(ssdate01, ssdate);
		});
		mCoolReader.tintViewIcons(mDialogView);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setView(mDialogView);
	}
}
