package org.coolreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.coolreader.crengine.StrUtils;

public class ProcessTextAct extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String selectedText = "";
		if (getIntent() != null) {
			if (getIntent().getAction() == Intent.ACTION_PROCESS_TEXT) {
				selectedText = StrUtils.getNonEmptyStr(getIntent().getStringExtra(Intent.EXTRA_PROCESS_TEXT),true);
			}
		}
		setContentView(R.layout.activity_process_text);
		Intent intent = new Intent(this, CoolReader.class);
		intent.putExtra("PROCESS_TEXT", selectedText);
		startActivity(intent);
		setResult(Activity.RESULT_OK);
	}
}