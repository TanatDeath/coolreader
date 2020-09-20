package org.coolreader.cloud.dropbox;

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

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxWebAuth;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.StrUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class DBXInputTokenDialog extends BaseDialog {

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	public final EditText tokenEdit;
	final Button tokenDelete;
	final Button authManual;
	final Button authAuto;

	public static final class SimpleSessionStore implements DbxSessionStore {
		private String token;

		public SimpleSessionStore() {
			this.token = null;
		}

		@Override
		public String get() {
			return token;
		}

		@Override
		public void set(String value) {
			this.token = value;
		}

		@Override
		public void clear() {
			this.token = null;
		}
	}

	public void saveDBXToken(String token) {
		if (!StrUtils.isEmptyStr(token)) {
			Log.i("DBX","Starting save dbx.token");
			try {
				final File fDBX = new File(mCoolReader.getSettingsFileF(0).getParent() + "/dbx.token");
				BufferedWriter bw = null;
				FileWriter fw = null;
				char[] bytesArray = new char[1000];
				int bytesRead = 1000;
				try {
					fw = new FileWriter(fDBX);
					bw = new BufferedWriter(fw);
					bw.write(token);
					bw.close();
					fw.close();
				} catch (Exception e) {
				}
			} catch (Exception e) {
			}
		}
	}

	public DBXInputTokenDialog(CoolReader activity)
	{
		super("DBXInputTokenDialog", activity, activity.getString( R.string.dbx_auth), true, false);
		mCoolReader = activity;
		setTitle(mCoolReader.getString( R.string.dbx_auth));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.dbx_input_token, null);
		tokenEdit = (EditText)view.findViewById(R.id.input_token_text);
		tokenDelete = (Button) view.findViewById(R.id.btn_delete_token);
		authManual = (Button) view.findViewById(R.id.btn_auth_manual);
		authAuto = (Button) view.findViewById(R.id.btn_auth_auto);
		ImageButton tokenOk =view.findViewById(R.id.input_token_ok_btn);
		tokenOk.setOnClickListener(v -> {
			if (!StrUtils.isEmptyStr(tokenEdit.getText().toString())) {
				String sToken = tokenEdit.getText().toString();
				if (sToken.contains(DBXConfig.DBX_REDIRECT_URL)) {
					try {
						Uri uri = Uri.parse(sToken);
						final String code = uri.getQueryParameter("code");
						//showToast("code = "+code);
						if (!StrUtils.isEmptyStr(code)) {
							new DBXFinishAuthorization(mCoolReader, uri, new DBXFinishAuthorization.Callback() {
								@Override
								public void onComplete(boolean result, String accessToken) {
									DBXConfig.didLogin = result;
									mCoolReader.showToast(R.string.dbx_auth_finished_ok);
									saveDBXToken(accessToken);
									dismiss();
								}

								@Override
								public void onError(Exception e) {
									DBXConfig.didLogin = false;
									mCoolReader.showToast(mCoolReader.getString(R.string.dbx_auth_finished_error) + ": " + e.getMessage());
								}
							}).execute(code);
						}
					} catch (Exception e) {
						mCoolReader.showToast(mCoolReader.getString(R.string.dbx_auth_finished_error) +
								": cannot parse URL - " + e.getMessage());
					}
				} else {
					new DBXFinishAuthorization(mCoolReader, null, new DBXFinishAuthorization.Callback() {
						@Override
						public void onComplete(boolean result, String accessToken) {
							DBXConfig.didLogin = result;
							mCoolReader.showToast(R.string.dbx_auth_finished_ok);
							saveDBXToken(accessToken);
							dismiss();
						}

						@Override
						public void onError(Exception e) {
							DBXConfig.didLogin = false;
							mCoolReader.showToast(mCoolReader.getString(R.string.dbx_auth_finished_error) + ": " + e.getMessage());
						}
					}).execute(tokenEdit.getText().toString());
				}
			}
		});
		TextView tokenDeleteTxt = (TextView) view.findViewById(R.id.txt_delete_token);
        final File fDBX = new File(mCoolReader.getSettingsFileF(0).getParent() + "/dbx.token");
        if (!fDBX.exists()) {
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

		tokenDelete.setOnClickListener(v -> {
			if (fDBX.exists())
				if (fDBX.delete()) {
					mCoolReader.showToast(R.string.success + ": " + fDBX.getAbsolutePath() + " [deleted]");
				} else {
					mCoolReader.showToast(R.string.unsuccess + ": " + fDBX.getAbsolutePath() + " [was not deleted]");
				}
		});

		authAuto.setOnClickListener(v -> {
			DBXConfig.mDbxRequestConfig = new DbxRequestConfig("CoolReaderExperience");
			DbxAppInfo appInfo = new DbxAppInfo(DBXConfig.DBX_KEY, DBXConfig.DBX_SECRET);
			DBXConfig.webAuth = new DbxWebAuth(DBXConfig.mDbxRequestConfig, appInfo);
			DBXConfig.sessionStore = new SimpleSessionStore();
			String key = "dropbox-auth-csrf-token";
			DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
					.withRedirectUri(DBXConfig.DBX_REDIRECT_URL, DBXConfig.sessionStore)
					.build();

			String authorizeUrl = DBXConfig.webAuth.authorize(webAuthRequest);
			//String code = new BufferedReader(new InputStreamReader(System.in)).readLine();
			//if (code == null) {
			try {
				Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
				mCoolReader.startActivity(myIntent);
			} catch (ActivityNotFoundException e) {
				mCoolReader.showToast(R.string.dbx_no_browser);
				e.printStackTrace();
			}
		});

		authAuto.setBackgroundColor(colorGrayC);

		authManual.setOnClickListener(v -> {
			DBXConfig.mDbxRequestConfig = new DbxRequestConfig("CoolReaderExperience");
			DbxAppInfo appInfo = new DbxAppInfo(DBXConfig.DBX_KEY, DBXConfig.DBX_SECRET);
			DBXConfig.webAuth = new DbxWebAuth(DBXConfig.mDbxRequestConfig, appInfo);
			DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
					.withNoRedirect()
					.build();

			String authorizeUrl = DBXConfig.webAuth.authorize(webAuthRequest);
			//String code = new BufferedReader(new InputStreamReader(System.in)).readLine();
			//if (code == null) {
			try {
				Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
				mCoolReader.startActivity(myIntent);
			} catch (ActivityNotFoundException e) {
				mCoolReader.showToast(R.string.dbx_no_browser);
				e.printStackTrace();
			}
		});

		authManual.setBackgroundColor(colorGrayC);
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
