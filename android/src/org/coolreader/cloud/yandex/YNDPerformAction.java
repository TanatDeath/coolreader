package org.coolreader.cloud.yandex;

import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.cloud.CloudAction;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class YNDPerformAction {

    public Exception mException;
    public List<CloudAction> mActionList;
    public CloudAction mCurAction = null;
    public Callback mCallback;
    public CoolReader mCoolReader;

    public interface Callback {
        void onError(YNDPerformAction a, String res, Exception e);
        void onComplete(YNDPerformAction a, String res, Object o);
    }

    public YNDPerformAction(CoolReader cr, List<CloudAction> actionList, Callback cb) {
        mActionList = actionList;
        mCallback = cb;
        mCoolReader = cr;
    }

    public String DoNextAction() {
        try {
            if (mActionList.size()>0)  {
                mCurAction = mActionList.get(0);
                mActionList.remove(0);
                if (mCurAction.action == CloudAction.YND_LIST_FOLDER) YndListFolder(mCurAction);
                if (mCurAction.action == CloudAction.YND_LIST_FOLDER_THEN_OPEN_DLG) YndListFolder(mCurAction);
                if (mCurAction.action == CloudAction.YND_LIST_FOLDER_IN_DLG) YndListFolder(mCurAction);
                if (mCurAction.action == CloudAction.YND_GET_DOWNLOAD_LINK) YndGetDownloadLink(mCurAction);
                if (mCurAction.action == CloudAction.YND_DOWNLOAD_FILE) YndDownloadFile(mCurAction);
            } else Log.i("YND", "End of cloud operation");
        } catch (Exception e) {
            mException = e;
            return e.getMessage();
        }
        return "";
    }

    public void YndListFolder(final CloudAction ca) throws IOException {
        String folder = ca.param;
        final String findStr = ca.param2;
        System.out  .println("YND: folder = " + folder);
        if (StrUtils.isEmptyStr(folder))
            if (!YNDConfig.init(mCoolReader)) return;
        if (StrUtils.isEmptyStr(folder)) folder = "/";
        folder = folder.replace("\\","/");
        HttpUrl.Builder urlBuilder = null;
        if (Utils.empty(findStr)) {
            urlBuilder = HttpUrl.parse(YNDConfig.YND_DISK_URL).newBuilder();
            urlBuilder.addQueryParameter("path", folder);
        }
        else urlBuilder = HttpUrl.parse(YNDConfig.YND_DISK_URL_LAST_UPL).newBuilder();
        urlBuilder.addQueryParameter("limit", String.valueOf(YNDConfig.YND_ITEMS_LIMIT));
        String url = urlBuilder.build().toString();
        Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "OAuth "+YNDConfig.yndToken)
                    .build();
        Call call = YNDConfig.client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                YNDListFiles lf = new YNDListFiles(sBody, findStr);
                mCallback.onComplete(YNDPerformAction.this,"ListFolderResult", lf);
            }

            public void onFailure(Call call, IOException e) {
                mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
            }
        });
    }

    public void YndGetDownloadLink(final CloudAction ca) {
        String sfile = ca.param;
        System.out.println("YND: file = " + sfile);
        sfile = sfile.replace("\\","/");
        HttpUrl.Builder urlBuilder = HttpUrl.parse(YNDConfig.YND_DOWNLOAD_URL).newBuilder();
        urlBuilder.addQueryParameter("path", sfile);
        String url = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "OAuth "+YNDConfig.yndToken)
                .build();
        Call call = YNDConfig.client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                ca.param2 = sBody;
                mCallback.onComplete(YNDPerformAction.this,"GetDownloadLink", null);
            }

            public void onFailure(Call call, IOException e) {
                mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
            }
        });
    }

    public void YndDownloadFile(final CloudAction ca) {
        String sfile = ca.param;
        String sJSON = ca.param2;
        System.out.println("YND: file = " + sfile);
        String href = "";
        try {
            JSONObject jsonObject = new JSONObject(sJSON);
            if (jsonObject.has("href")) href = jsonObject.get("href").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        sfile = sfile.replace("\\","/");
        if (!StrUtils.isEmptyStr(href)) {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(href).newBuilder();
            String url = urlBuilder.build().toString();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "OAuth " + YNDConfig.yndToken)
                    .build();
            Call call = YNDConfig.client.newCall(request);
            call.enqueue(new okhttp3.Callback() {
                public void onResponse(Call call, Response response)
                        throws IOException {
                    InputStream is = response.body().byteStream();
                    BufferedInputStream input = new BufferedInputStream(is);
                    OutputStream output = new FileOutputStream(ca.mFile);
                    byte[] data = new byte[1024];
                    long total = 0;
                    int count = 0;
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                    mCallback.onComplete(YNDPerformAction.this, "DownloadFile", null);
                }

                public void onFailure(Call call, IOException e) {
                    mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
                }
            });
        }
    }

}

