package org.coolreader.crengine;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TableLayout;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.HashMap;

public class SaveDocDialog extends BaseDialog {

	private CoolReader mActivity;
	private int mWindowSize;
	private LayoutInflater mInflater;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	int colorIcon;
	int colorGrayC;
	int colorGrayCT;
	int colorGrayCT2;


	public SaveDocDialog(CoolReader activity)
	{
		super("SaveDocDialog", activity, activity.getString(R.string.external_doc_came), false, true);
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		this.mActivity = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mActivity, isEInk);
		colorIcon = themeColors.get(R.attr.colorIcon);
		colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		colorGrayCT= Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		if(getWindow().getAttributes().softInputMode== WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		}
	}

	@Override
	protected void onCreate() {
		setCancelable(true);
		setCanceledOnTouchOutside(true);

		super.onCreate();
		L.v("SaveDocDialog is created");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mInflater = LayoutInflater.from(getContext());
		ViewGroup view = (ViewGroup) mInflater.inflate(R.layout.save_doc_dialog, null);
		TableLayout mainTl = view.findViewById(R.id.table);
		View viewFolder = mInflater.inflate(R.layout.save_doc_folder_row, null);
		Button booksFolder = viewFolder.findViewById(R.id.btn_folder);
		booksFolder.setBackgroundColor(colorGrayCT);
		mainTl.addView(viewFolder);

		setView(view);
	}
}
