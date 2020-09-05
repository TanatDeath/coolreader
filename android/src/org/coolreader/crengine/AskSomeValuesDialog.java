package org.coolreader.crengine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;

public class AskSomeValuesDialog extends BaseDialog {

	public interface ValuesEnteredCallback {
		public void done(ArrayList<String> results);
	}

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private FileInfo fInfo;
	private ArrayList<TextView> textViews = new ArrayList<TextView>();
	private ArrayList<EditText> editTexts = new ArrayList<EditText>();
	public final ValuesEnteredCallback callback;

	public AskSomeValuesDialog(CoolReader activity, String sTitle, String sSomeText,
							   ArrayList<String[]> askValues, ValuesEnteredCallback callback)
	{
		super("AskSomeValuesDialog", activity, sTitle, true, false);
		mCoolReader = activity;
		setTitle(sTitle);
		this.callback = callback;
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.ask_some_values_dialog, null);
		TextView someText = (TextView) view.findViewById(R.id.some_text);
		someText.setText(sSomeText);
		textViews.clear();
		editTexts.clear();
		for (int i = 1; i<10; i++) {
			if (i == 1) {
				TableRow tr = (TableRow) view.findViewById(R.id.some_value_tr1);
				TextView tv = (TextView) view.findViewById(R.id.some_value_label1);
				EditText et = (EditText) view.findViewById(R.id.some_value_edit1);
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
			if (i == 2) {
				TableRow tr = (TableRow) view.findViewById(R.id.some_value_tr2);
				TextView tv = (TextView) view.findViewById(R.id.some_value_label2);
				EditText et = (EditText) view.findViewById(R.id.some_value_edit2);
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
		}
		setView( view );
	}

	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
		ArrayList<String> res = new ArrayList<String>();
		for (int i = 1; i<10; i++) {
			if (i == 1) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit1);
				if (et != null) res.add(et.getText().toString());
			}
			if (i == 2) {
				EditText et = (EditText) view.findViewById(R.id.some_value_edit2);
				if (et != null) res.add(et.getText().toString());
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
