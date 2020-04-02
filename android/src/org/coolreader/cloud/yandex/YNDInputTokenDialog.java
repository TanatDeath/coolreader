package org.coolreader.cloud.yandex;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.StrUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class YNDInputTokenDialog extends BaseDialog {

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	public final EditText tokenEdit;
	final Button tokenDelete;
	//final Button authManual;
	final Button authAuto;

	private String extractToken(String sUri) {
		if (sUri.contains(YNDConfig.YND_REDIRECT_URL)) {
			String token = "";
			if (sUri.contains("#access_token=")) {
				token = sUri.substring(sUri.indexOf("#access_token="));
				if (token.contains("&")) token = token.split("\\&")[0];
				token = token.replace("#access_token=", "");
				return token;
			}
		}
		return sUri;
	}

	public void saveYNDToken() {
		if (!StrUtils.isEmptyStr(tokenEdit.getText().toString())) {
			Log.i("YND","Starting save ynd.token");
			try {
				final File fYND = new File(mCoolReader.getSettingsFile(0).getParent() + "/ynd.token");
				BufferedWriter bw = null;
				FileWriter fw = null;
				char[] bytesArray = new char[1000];
				int bytesRead = 1000;
				try {
					fw = new FileWriter(fYND);
					bw = new BufferedWriter(fw);
					bw.write(extractToken(tokenEdit.getText().toString()));
					bw.close();
					fw.close();
				} catch (Exception e) {
				}
			} catch (Exception e) {
			}
			YNDConfig.didLogin = true;
			mCoolReader.showToast(R.string.ynd_auth_finished_ok);
			BackgroundThread.instance().postGUI(new Runnable() {
				@Override
				public void run() {
					CloudAction.yndOpenBookDialog(mCoolReader);
				}
			}, 500);
		}
	}

	public YNDInputTokenDialog(CoolReader activity)
	{
		super("YNDInputTokenDialog", activity, activity.getString( R.string.ynd_auth), true, false);
		mCoolReader = activity;
		setTitle(mCoolReader.getString( R.string.ynd_auth));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.ynd_input_token, null);
		tokenEdit = (EditText)view.findViewById(R.id.ynd_input_token_text);
		tokenDelete = (Button) view.findViewById(R.id.ynd_btn_delete_token);
		//authManual = (Button) view.findViewById(R.id.ynd_btn_auth_manual);
		authAuto = (Button) view.findViewById(R.id.ynd_btn_auth_auto_ynd);
		ImageButton tokenOk =view.findViewById(R.id.ynd_input_token_ok_btn);
		tokenOk.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				saveYNDToken();
				dismiss();
			}
		});
		TextView tokenDeleteTxt = (TextView) view.findViewById(R.id.ynd_txt_delete_token);
        final File fYND = new File(mCoolReader.getSettingsFile(0).getParent() + "/ynd.token");
        if (!fYND.exists()) {
        	tokenDelete.setVisibility(View.INVISIBLE);
			tokenDeleteTxt.setVisibility(View.INVISIBLE);
		}
		else {
			tokenDelete.setVisibility(View.VISIBLE);
			tokenDeleteTxt.setVisibility(View.VISIBLE);
		}
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGrayC = a.getColor(1, Color.GRAY);
		a.recycle();
		tokenDelete.setBackgroundColor(colorGrayC);

		tokenDelete.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
        	if (fYND.exists())
            	if (fYND.delete()) {
                	mCoolReader.showToast(R.string.success + ": " + fYND.getAbsolutePath() + " [deleted]");
                } else {
                	mCoolReader.showToast(R.string.unsuccess + ": " + fYND.getAbsolutePath() + " [was not deleted]");
                }
            }
        });

		authAuto.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				String authorizeUrl = Uri.parse(YNDConfig.YND_AUTH_URL)
						.buildUpon()
						.appendQueryParameter("response_type", "token")
						.appendQueryParameter("client_id", YNDConfig.YND_ID)
						.appendQueryParameter("device_id", mCoolReader.getAndroid_id())
						.appendQueryParameter("device_name", mCoolReader.getModel())
						.appendQueryParameter("redirect_url", YNDConfig.YND_REDIRECT_URL)
						.build().toString();
				try {
					Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
					mCoolReader.startActivity(myIntent);
				} catch (ActivityNotFoundException e) {
					mCoolReader.showToast(R.string.dbx_no_browser);
					e.printStackTrace();
				}
			}
		});

		authAuto.setBackgroundColor(colorGrayC);
		setView( view );
	}

	@Override
	public void onPositiveButtonClick() {
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}
}
