package org.coolreader.crengine;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;
import java.util.HashMap;

public class AskSomeValuesDialog extends BaseDialog {

	public interface ValuesEnteredCallback {
		void done(ArrayList<String> results);
	}

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private FileInfo fInfo;
	private ArrayList<TextView> textViews = new ArrayList<TextView>();
	private ArrayList<EditText> editTexts = new ArrayList<EditText>();
	public final ValuesEnteredCallback callback;
	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors = null;

	public AskSomeValuesDialog(CoolReader activity, String sTitle, String sSomeText,
							   ArrayList<String[]> askValues, ValuesEnteredCallback callback)
	{
		super("AskSomeValuesDialog", activity, sTitle, true, false);
		mCoolReader = activity;
		setTitle(sTitle);
		this.callback = callback;
		mInflater = LayoutInflater.from(getContext());
		themeColors = Utils.getThemeColors((CoolReader) activity, isEInk);
		View view = mInflater.inflate(R.layout.ask_some_values_dialog, null);
		TextView someText = (TextView) view.findViewById(R.id.some_text);
		someText.setText(sSomeText);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT= Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorIcon = themeColors.get(R.attr.colorIcon);
		int colorIcon128 = Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		textViews.clear();
		editTexts.clear();
		for (int i = 1; i<=10; i++) {
			TableRow tr = null;
			TextView tv = null;
			EditText et = null;
			if (i == 1) {
				tr = view.findViewById(R.id.some_value_tr1);
				tv = view.findViewById(R.id.some_value_label1);
				et = view.findViewById(R.id.some_value_edit1);
			}
			if (i == 2) {
				tr = view.findViewById(R.id.some_value_tr2);
				tv = view.findViewById(R.id.some_value_label2);
				et = view.findViewById(R.id.some_value_edit2);
			}
			if (i == 3) {
				tr = view.findViewById(R.id.some_value_tr3);
				tv = view.findViewById(R.id.some_value_label3);
				et = view.findViewById(R.id.some_value_edit3);
			}
			if (i == 4) {
				tr = view.findViewById(R.id.some_value_tr4);
				tv = view.findViewById(R.id.some_value_label4);
				et = view.findViewById(R.id.some_value_edit4);
			}
			if (i == 5) {
				tr = view.findViewById(R.id.some_value_tr5);
				tv = view.findViewById(R.id.some_value_label5);
				et = view.findViewById(R.id.some_value_edit5);
			}
			if (i == 6) {
				tr = view.findViewById(R.id.some_value_tr6);
				tv = view.findViewById(R.id.some_value_label6);
				et = view.findViewById(R.id.some_value_edit6);
			}
			if (i == 7) {
				tr = view.findViewById(R.id.some_value_tr7);
				tv = view.findViewById(R.id.some_value_label7);
				et = view.findViewById(R.id.some_value_edit7);
			}
			if (i == 8) {
				tr = view.findViewById(R.id.some_value_tr8);
				tv = view.findViewById(R.id.some_value_label8);
				et = view.findViewById(R.id.some_value_edit8);
			}
			if (i == 9) {
				tr = view.findViewById(R.id.some_value_tr9);
				tv = view.findViewById(R.id.some_value_label9);
				et = view.findViewById(R.id.some_value_edit9);
			}
			if (i == 10) {
				tr = view.findViewById(R.id.some_value_tr10);
				tv = view.findViewById(R.id.some_value_label10);
				et = view.findViewById(R.id.some_value_edit10);
			}
		//	et.setBackgroundColor(colorGrayCT);
			et.setTextColor(colorIcon);
			et.setHintTextColor(colorIcon128);
			if (tr != null) {
				textViews.add(tv);
				editTexts.add(et);
				if (askValues.size() >= i) {
					tv.setText(askValues.get(i-1)[0]);
					et.setHint(askValues.get(i-1)[1]);
					if (!StrUtils.isEmptyStr(askValues.get(i-1)[2])) et.setText(askValues.get(i-1)[2]);
				} else {
					((ViewGroup) tr.getParent()).removeView(tr);
				}
			}
		}
		setView(view);
	}

	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
		ArrayList<String> res = new ArrayList<String>();
		for (int i = 1; i<10; i++) {
			if (i == 1) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit1);
				if (et != null) res.add(et.getText().toString());
					else res.add("");
			}
			if (i == 2) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit2);
				if (et != null) res.add(et.getText().toString());
					else res.add("");
			}
			if (i == 3) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit3);
				if (et != null) res.add(et.getText().toString());
				else res.add("");
			}
			if (i == 4) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit4);
				if (et != null) res.add(et.getText().toString());
				else res.add("");
			}
			if (i == 5) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit5);
				if (et != null) res.add(et.getText().toString());
				else res.add("");
			}
			if (i == 6) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit6);
				if (et != null) res.add(et.getText().toString());
				else res.add("");
			}
			if (i == 7) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit7);
				if (et != null) res.add(et.getText().toString());
				else res.add("");
			}
			if (i == 8) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit8);
				if (et != null) res.add(et.getText().toString());
				else res.add("");
			}
			if (i == 9) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit9);
				if (et != null) res.add(et.getText().toString());
				else res.add("");
			}
			if (i == 10) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit10);
				if (et != null) res.add(et.getText().toString());
				else res.add("");
			}
		}
		if (callback != null) callback.done(res);
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
		if (callback != null) callback.done(null);
	}
}
