package org.coolreader.cloud.litres;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.StrUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class LitresCredentialsDialog extends BaseDialog {

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	public final EditText mUsernameEdit;
	public final EditText mPasswordEdit;
	final Button mBtnTestConnect;

	public void saveLitresCreds() {
		if (
			(!StrUtils.isEmptyStr(mUsernameEdit.getText().toString())) &&
			(!StrUtils.isEmptyStr(mPasswordEdit.getText().toString()))
		) {
			mCoolReader.litresCloudSettings.login = StrUtils.getNonEmptyStr(mUsernameEdit.getText().toString(),true);
			mCoolReader.litresCloudSettings.passw = StrUtils.getNonEmptyStr(mPasswordEdit.getText().toString(),true);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			final String prettyJson = gson.toJson(mCoolReader.litresCloudSettings);
			mCoolReader.saveLitresCloudSettings(prettyJson);
			LitresConfig.didLogin = true;
			mCoolReader.showToast(R.string.litres_auth_finished_ok);
			dismiss();
		}
	}

	public LitresCredentialsDialog(CoolReader activity)
	{
		super("LitresCredentialsDialog", activity, activity.getString( R.string.litres_credentials), true, false);
		mCoolReader = activity;
		LitresConfig.init(mCoolReader);
		setTitle(mCoolReader.getString(R.string.litres_credentials));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.litres_credentials, null);
		mUsernameEdit = (EditText)view.findViewById(R.id.lites_username);
		mUsernameEdit.setText(mCoolReader.litresCloudSettings.login);
		mPasswordEdit = (EditText)view.findViewById(R.id.lites_password);
		mPasswordEdit.setText(mCoolReader.litresCloudSettings.passw);
		mBtnTestConnect = (Button) view.findViewById(R.id.btn_test_connect);
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGrayC = a.getColor(1, Color.GRAY);
		a.recycle();

		mBtnTestConnect.setOnClickListener(v -> {
			try {
				if (StrUtils.isEmptyStr(mUsernameEdit.getText().toString()) ||
						StrUtils.isEmptyStr(mPasswordEdit.getText().toString())) {
					mCoolReader.showToast(R.string.litres_empty_creds);
					return;
				}
				String  uniqueID = UUID.randomUUID().toString();
				JSONObject json = LitresJsons.w_create_sid(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET,
						StrUtils.getNonEmptyStr(mUsernameEdit.getText().toString(), true),
						StrUtils.getNonEmptyStr(mPasswordEdit.getText().toString(), true)
						);
				OkHttpClient client = new OkHttpClient();
				HttpUrl.Builder urlBuilder = HttpUrl.parse(LitresJsons.LITRES_ADDR).newBuilder();
				String url = urlBuilder.build().toString();
				RequestBody body = new FormBody.Builder()
						.add("jdata", json.toString())
						.build();
				Request request =
						new Request.Builder()
						.header("Content-type","application/json; charset=utf-8")
						.post(body)
						.url(url)
						.build();
				//final Request copy = request.newBuilder().build();
				//final Buffer buffer = new Buffer();
				//String h = copy.headers().toString();
				//String m = copy.method();
				//copy.body().writeTo(buffer);
				//String s = buffer.readUtf8();
				//activity.showToast(m + "; Headers is: " + h +"; Request is: " + s);
				//Log.i("LitresCredentialsDialog", "req: " + s);
				Call call = client.newCall(request);
				call.enqueue(new okhttp3.Callback() {
					public void onResponse(Call call, Response response)
							throws IOException {
						String sBody = response.body().string();
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
							try {
								if (LitresJsons.parse_w_create_sid(uniqueID, activity, sBody))
									saveLitresCreds();
							} catch (Exception e) {
								activity.showToast(activity.getString(R.string.cloud_error) + ": " + sBody);
							}
						}, 100));
					}
					public void onFailure(Call call, IOException e) {
						BackgroundThread.instance().postBackground(() ->
								BackgroundThread.instance().postGUI(() ->
										activity.showToast(activity.getString(R.string.cloud_error) + ": " + e.getMessage()), 100));
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		mBtnTestConnect.setBackgroundColor(colorGrayC);
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
