package org.coolreader.cloud.litres;

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

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.yandex.YNDConfig;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.StrUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
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

//	public void saveYNDToken() {
//		if (!StrUtils.isEmptyStr(tokenEdit.getText().toString())) {
//			Log.i("YND","Starting save ynd.token");
//			try {
//				final File fYND = new File(mCoolReader.getSettingsFileF(0).getParent() + "/ynd.token");
//				BufferedWriter bw = null;
//				FileWriter fw = null;
//				char[] bytesArray = new char[1000];
//				int bytesRead = 1000;
//				try {
//					fw = new FileWriter(fYND);
//					bw = new BufferedWriter(fw);
//					bw.write(extractToken(tokenEdit.getText().toString()));
//					bw.close();
//					fw.close();
//				} catch (Exception e) {
//				}
//			} catch (Exception e) {
//			}
//			YNDConfig.didLogin = true;
//			mCoolReader.showToast(R.string.ynd_auth_finished_ok);
//			BackgroundThread.instance().postGUI(() -> CloudAction.yndOpenBookDialog(mCoolReader, null,true), 500);
//		}
//	}

	public LitresCredentialsDialog(CoolReader activity)
	{
		super("LitresCredentialsDialog", activity, activity.getString( R.string.litres_credentials), true, false);
		mCoolReader = activity;
		setTitle(mCoolReader.getString(R.string.litres_credentials));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.litres_credentials, null);
		mUsernameEdit = (EditText)view.findViewById(R.id.lites_username);
		mPasswordEdit = (EditText)view.findViewById(R.id.lites_password);
		mBtnTestConnect = (Button) view.findViewById(R.id.btn_test_connect);
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGrayC = a.getColor(1, Color.GRAY);
		a.recycle();

		mBtnTestConnect.setOnClickListener(v -> {
			try {
				JSONObject json = CreateJsons.w_create_sid(BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET);
				OkHttpClient client = new OkHttpClient();
				HttpUrl.Builder urlBuilder = HttpUrl.parse(CreateJsons.LITRES_ADDR).newBuilder();
				String url = urlBuilder.build().toString();
				RequestBody body = RequestBody.create(
						MediaType.parse("application/x-www-form-urlencoded"), "jdata="+json.toString());
				Request request =
						new Request.Builder()
						.header("Content-type","application/json; charset=utf-8")
						.post(body)
						.url(url)
						.build();
				final Request copy = request.newBuilder().build();
				final Buffer buffer = new Buffer();
				String h = copy.headers().toString();
				String m = copy.method();
				copy.body().writeTo(buffer);
				String s = buffer.readUtf8();
				activity.showToast(m + "; Headers is: " + h +"; Request is: " + s);
				Log.i("LitresCredentialsDialog", "req: " + s);
				Call call = client.newCall(request);
				call.enqueue(new okhttp3.Callback() {
					public void onResponse(Call call, Response response)
							throws IOException {
						String sBody = response.body().string();
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
							activity.showToast(sBody);
						}, 100));
					}
					public void onFailure(Call call, IOException e) {
						BackgroundThread.instance().postBackground(() ->
								BackgroundThread.instance().postGUI(() -> activity.showToast(e.getMessage()), 100));
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
