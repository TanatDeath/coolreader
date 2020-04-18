package org.coolreader.cloud.yandex;

import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.cloud.CloudAction;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
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
                if (mCurAction.action == CloudAction.YND_CHECK_CR_FOLDER) YndCheckCrFolder(mCurAction);
                if (mCurAction.action == CloudAction.YND_CREATE_CR_FOLDER) YndCreateCrFolder(mCurAction);
                if (mCurAction.action == CloudAction.YND_SAVE_TO_FILE_GET_LINK) YndSaveToFileGetLink(mCurAction);
                if (mCurAction.action == CloudAction.YND_DELETE_FILE_ASYNC) YndDeleteFileAsync(mCurAction);
                if (mCurAction.action == CloudAction.YND_SAVE_STRING_TO_FILE) YndSaveStringToFile(mCurAction);
                if (mCurAction.action == CloudAction.YND_LIST_JSON_FILES) YndListJsonFiles(mCurAction);
                if (mCurAction.action == CloudAction.YND_LIST_JSON_FILES_LASTPOS) YndListJsonFiles(mCurAction);
                if (mCurAction.action == CloudAction.YND_DOWNLOAD_FILE_TO_STRING) YndDownloadFileToString(mCurAction);
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
        Log.i("CLOUD","YND: folder = " + folder);
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
                YNDListFiles lf = new YNDListFiles(mCoolReader, sBody, findStr, false);
                mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_LIST_FOLDER_RESULT, lf);
            }

            public void onFailure(Call call, IOException e) {
                mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
            }
        });
    }

    public void YndCheckCrFolder(final CloudAction ca) throws IOException {
        String folder = "/CoolReader";
        Log.i("CLOUD", "YND: folder = " + folder);
        if (!YNDConfig.init(mCoolReader)) return;
        HttpUrl.Builder urlBuilder = null;
        urlBuilder = HttpUrl.parse(YNDConfig.YND_DISK_URL).newBuilder();
        urlBuilder.addQueryParameter("path", folder);
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
                //Log.i("CLOUD", sBody);
                YNDListFiles lf = new YNDListFiles(mCoolReader, sBody, "", true);
                Log.i("CLOUD", "YND: found = " + lf.fileList.size());
                mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_CHECK_CR_FOLDER, lf);
            }

            public void onFailure(Call call, IOException e) {
                mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
            }
        });
    }

    public void YndCreateCrFolder(final CloudAction ca) throws IOException {
        String folder = "/CoolReader";
        Log.i("CLOUD", "YND: create folder = " + folder);
        if (StrUtils.getNonEmptyStr(ca.param2, true).equals("skip")) {
            mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_CREATE_CR_FOLDER, null);
        } else {
            HttpUrl.Builder urlBuilder = null;
            urlBuilder = HttpUrl.parse(YNDConfig.YND_DISK_URL).newBuilder();
            urlBuilder.addQueryParameter("path", folder);
            String url = urlBuilder.build().toString();
            MediaType mt = MediaType.parse("plain/text; charset=utf-8");
            RequestBody body = RequestBody.create(mt, "");
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Authorization", "OAuth " + YNDConfig.yndToken)
                    .build();
            Call call = YNDConfig.client.newCall(request);
            call.enqueue(new okhttp3.Callback() {
                public void onResponse(Call call, Response response)
                        throws IOException {
                    String sBody = response.body().string();
                    //Log.i("CLOUD", sBody);
                    mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_CREATE_CR_FOLDER, null);
                }

                public void onFailure(Call call, IOException e) {
                    Log.i("CLOUD Error", e.getMessage());
                    mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
                }
            });
        }
    }

    public void YndSaveToFileGetLink(final CloudAction ca) throws IOException {
        String folder = "/CoolReader";
        String sFileName = ca.param;
        Log.i("CLOUD", "YND: save string to file = " + sFileName);
        HttpUrl.Builder urlBuilder = null;
        urlBuilder = HttpUrl.parse(YNDConfig.YND_DISK_UPLOAD_URL).newBuilder();
        urlBuilder.addQueryParameter("path", folder + "/" + sFileName);
        String url = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "OAuth " + YNDConfig.yndToken)
                .build();
        Call call = YNDConfig.client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                String href = "";
                try {
                    JSONObject jsonObject = new JSONObject(sBody);
                    if (jsonObject.has("href")) href = jsonObject.get("href").toString();
                } catch (Exception e) {

                }
                Log.i("CLOUD", sBody);
                mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_SAVE_TO_FILE_GET_LINK, href);
            }

            public void onFailure(Call call, IOException e) {
                Log.i("CLOUD Error", e.getMessage());
                mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
            }
        });
    }

    public void YndDeleteFileAsync(final CloudAction ca) throws IOException {
        String sFilePath = ca.param;
        Log.i("CLOUD", "YND: delete file async = " + sFilePath);
        HttpUrl.Builder urlBuilder = null;
        urlBuilder = Objects.requireNonNull(HttpUrl.parse(YNDConfig.YND_DISK_URL)).newBuilder();
        urlBuilder.addQueryParameter("path", sFilePath)
                .addQueryParameter("force_async", "true")
                .addQueryParameter("permanently", "true");
        String url = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "OAuth " + YNDConfig.yndToken)
                .build();
        Call call = YNDConfig.client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                String href = "";
                try {
                    JSONObject jsonObject = new JSONObject(sBody);
                    if (jsonObject.has("href")) href = jsonObject.get("href").toString();
                } catch (Exception e) {

                }
                Log.i("CLOUD", sBody);
                mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_DELETE_FILE_ASYNC, href);
            }

            public void onFailure(Call call, IOException e) {
                Log.i("CLOUD Error", e.getMessage());
                mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
            }
        });
    }

    public void YndSaveStringToFile(final CloudAction ca) throws IOException {
        String folder = "/CoolReader";
        String sContent = ca.param;
        String sHref = ca.param2;
        Log.i("CLOUD", "YND: save string to file = " + sHref);
        HttpUrl.Builder urlBuilder = null;
        urlBuilder = HttpUrl.parse(sHref).newBuilder();
        MediaType mt = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mt, sContent);
        String url = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("Authorization", "OAuth " + YNDConfig.yndToken)
                .build();
        Call call = YNDConfig.client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                Log.i("CLOUD", sBody);
                mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_SAVE_STRING_TO_FILE, null);
            }

            public void onFailure(Call call, IOException e) {
                Log.i("CLOUD Error", e.getMessage());
                mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
            }
        });
    }

    public void YndGetDownloadLink(final CloudAction ca) {
        String sfile = ca.param;
        Log.i("CLOUD","YND: file = " + sfile);
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
                mCallback.onComplete(YNDPerformAction.this,CloudAction.CLOUD_COMPLETE_GET_DOWNLOAD_LINK, null);
            }

            public void onFailure(Call call, IOException e) {
                mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
            }
        });
    }

    public void YndDownloadFile(final CloudAction ca) {
        String sfile = ca.param;
        String sJSON = ca.param2;
        Log.i("CLOUD","YND: file = " + sfile);
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
                    mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_DOWNLOAD_FILE, null);
                }

                public void onFailure(Call call, IOException e) {
                    mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
                }
            });
        }
    }

    public void YndDownloadFileToString(final CloudAction ca) {
        String sJSON = ca.param2;
        String href = "";
        try {
            JSONObject jsonObject = new JSONObject(sJSON);
            if (jsonObject.has("href")) href = jsonObject.get("href").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("CLOUD","YND: download file to string from = " + href);
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
                    StringBuilder textBuilder = new StringBuilder();
                    try (Reader reader = new BufferedReader(
                            new InputStreamReader(is, "UTF-8"))) {
                        int c = 0;
                        while ((c = reader.read()) != -1) {
                            textBuilder.append((char) c);
                        }
                    }
                    is.close();
                    mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_DOWNLOAD_FILE_TO_STRING, textBuilder.toString());
                }

                public void onFailure(Call call, IOException e) {
                    mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
                }
            });
        }
    }

    public void YndListJsonFiles(final CloudAction ca) throws IOException {
        final String fileMark = ca.param;
        final String bookCRC = ca.bookCRC;
        final String existsMark = ca.param2;
        if (!StrUtils.getNonEmptyStr(existsMark,true).equals("skip")) {
            YNDListFiles lf = new YNDListFiles(mCoolReader, "{}", "", false);
            mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_LIST_FOLDER_RESULT, lf);
        } else {
            Log.i("CLOUD", "YND: list JSON files, mark = " + fileMark + ", crc = " + bookCRC);
            HttpUrl.Builder urlBuilder = null;
            urlBuilder = HttpUrl.parse(YNDConfig.YND_DISK_URL).newBuilder();
            urlBuilder.addQueryParameter("path", "/CoolReader/");
            urlBuilder.addQueryParameter("limit", String.valueOf(YNDConfig.YND_ITEMS_LIMIT));
            String url = urlBuilder.build().toString();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "OAuth " + YNDConfig.yndToken)
                    .build();
            Call call = YNDConfig.client.newCall(request);
            call.enqueue(new okhttp3.Callback() {
                public void onResponse(Call call, Response response)
                        throws IOException {
                    String sBody = response.body().string();
                    //Log.i("CLOUD", sBody);
                    String sExt = "json";
                    if (fileMark.equals("_settings_")) sExt = "txt";
                    YNDListFiles lf = new YNDListFiles(mCoolReader, sBody, fileMark, bookCRC, sExt);
                    //Log.i("CLOUD", "size: " + lf.fileList.size());
                    mCallback.onComplete(YNDPerformAction.this, CloudAction.CLOUD_COMPLETE_LIST_JSON_FILES, lf);
                }

                public void onFailure(Call call, IOException e) {
                    mCallback.onError(YNDPerformAction.this, e.getMessage(), e);
                }
            });
        }
    }

}

