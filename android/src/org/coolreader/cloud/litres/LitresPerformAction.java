package org.coolreader.cloud.litres;

import android.content.DialogInterface;
import android.util.Log;

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.ProgressDialog;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class LitresPerformAction {

    public Exception mException;
    public List<CloudAction> mActionList;
    public CloudAction mCurAction = null;
    public Callback mCallback;
    public CoolReader mCoolReader;
    public static ProgressDialog progressDlg;

    public interface Callback {
        void onComplete(LitresPerformAction a, String res, Object o);
        void onError(LitresPerformAction a, String res, Exception e);
    }

    public LitresPerformAction(CoolReader cr, List<CloudAction> actionList, Callback cb) {
        mActionList = actionList;
        mCallback = cb;
        mCoolReader = cr;
    }

    public String DoFirstAction() {
        Log.i("Litres", "Begin of cloud operation");
        progressDlg = ProgressDialog.show(mCoolReader,
                mCoolReader.getString(R.string.network_op),
                mCoolReader.getString(R.string.network_op),
                true, false, null);
        return DoNextAction();
    }

    public String DoNextAction() {
        try {
            if (mActionList.size()>0)  {
                mCurAction = mActionList.get(0);
                mActionList.remove(0);
                if (mCurAction.action == CloudAction.LITRES_AUTH) LitresAuth(mCurAction);
                if (mCurAction.action == CloudAction.LITRES_GET_GENRE_LIST) LitresGetGenreList(mCurAction);
                if (mCurAction.action == CloudAction.LITRES_GET_COLLECTION_LIST) LitresGetCollectionList(mCurAction);
                if (mCurAction.action == CloudAction.LITRES_GET_SEQUENCE_LIST) LitresGetSequenceList(mCurAction);
                if (mCurAction.action == CloudAction.LITRES_SEARCH_BOOKS) LitresSearchBooks(mCurAction);
                if (mCurAction.action == CloudAction.LITRES_PURCHASE_BOOK) LitresPurchaseBook(mCurAction);
                if (mCurAction.action == CloudAction.LITRES_DOWNLOAD_BOOK) LitresDownloadBook(mCurAction);
                if (mCurAction.action == CloudAction.LITRES_SEARCH_PERSON_LIST) LitresSearchPersonList(mCurAction);
            } else {
                Log.i("Litres", "End of cloud operation");
                if (progressDlg != null)
                    if (progressDlg.isShowing()) progressDlg.dismiss();
            }
        } catch (Exception e) {
            mException = e;
            if (progressDlg != null)
                if (progressDlg.isShowing()) progressDlg.dismiss();
            return e.getMessage();
        }
        return "";
    }

    public void LitresAuth(final CloudAction ca) throws JSONException, NoSuchAlgorithmException {
        if (
                (StrUtils.isEmptyStr(mCoolReader.litresCloudSettings.login)) ||
                (StrUtils.isEmptyStr(mCoolReader.litresCloudSettings.passw))
        ) mCoolReader.readLitresCloudSettings();
        if (
            (StrUtils.isEmptyStr(mCoolReader.litresCloudSettings.login)) ||
            (StrUtils.isEmptyStr(mCoolReader.litresCloudSettings.passw))
        ) {
            BackgroundThread.instance().postBackground(() ->
                    BackgroundThread.instance().postGUI(() ->
            mCoolReader.showToast(mCoolReader.getString(R.string.cloud_error) + ": " + mCoolReader.getString(R.string.litres_empty_login_or_passw), 100)));
            return;
        }

        String  uniqueID = UUID.randomUUID().toString();
        ca.param2 = uniqueID;

        JSONObject json = LitresJsons.w_create_sid(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET,
                StrUtils.getNonEmptyStr(mCoolReader.litresCloudSettings.login, true),
                StrUtils.getNonEmptyStr(mCoolReader.litresCloudSettings.passw, true)
        );
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(20, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).build();
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
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
                    try {
                        if (LitresJsons.parse_w_create_sid(uniqueID, mCoolReader, sBody))
                            mCallback.onComplete(LitresPerformAction.this,"Auth", null);
                    } catch (Exception e) {
                        mCallback.onError(LitresPerformAction.this, e.getMessage(), e);
                    }
                }, 100));
            }
            public void onFailure(Call call, IOException e) {
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() ->
                                mCallback.onError(LitresPerformAction.this, e.getMessage(), e)));
            }
        });
    }

    public void LitresGetGenreList(final CloudAction ca) throws JSONException, NoSuchAlgorithmException {
        String  uniqueID = UUID.randomUUID().toString();
        ca.param2 = uniqueID;
        JSONObject json = null;
        // whole list
        if (StrUtils.isEmptyStr(ca.lsp.searchString))
            json = LitresJsons.r_genres_list(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId);
        else {
            json = LitresJsons.r_search_genres(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId,
                    mCoolReader, ca);
        }
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(20, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).build();
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
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
                    mCallback.onComplete(LitresPerformAction.this,"GenreList", sBody);
                }, 100));
            }

            public void onFailure(Call call, IOException e) {
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() ->
                                mCallback.onError(LitresPerformAction.this, e.getMessage(), e)));
            }
        });
    }

    public void LitresGetCollectionList(final CloudAction ca) throws JSONException, NoSuchAlgorithmException {
        String  uniqueID = UUID.randomUUID().toString();
        ca.param2 = uniqueID;
        JSONObject json = LitresJsons.r_search_collections(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId,
                    mCoolReader, ca);
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(20, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).build();
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
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
                    mCallback.onComplete(LitresPerformAction.this,"CollectionList", sBody);
                }, 100));
            }

            public void onFailure(Call call, IOException e) {
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() ->
                                mCallback.onError(LitresPerformAction.this, e.getMessage(), e)));
            }
        });
    }

    public void LitresGetSequenceList(final CloudAction ca) throws JSONException, NoSuchAlgorithmException {
        String  uniqueID = UUID.randomUUID().toString();
        ca.param2 = uniqueID;
        JSONObject json = LitresJsons.r_search_sequences(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId,
                mCoolReader, ca);
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(20, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).build();
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
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
                    mCallback.onComplete(LitresPerformAction.this,"SequenceList", sBody);
                }, 100));
            }

            public void onFailure(Call call, IOException e) {
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() ->
                                mCallback.onError(LitresPerformAction.this, e.getMessage(), e)));
            }
        });
    }

    public void LitresSearchPersonList(final CloudAction ca) throws JSONException, NoSuchAlgorithmException {
        String  uniqueID = UUID.randomUUID().toString();
        ca.param2 = uniqueID;
        JSONObject json = LitresJsons.r_search_persons(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId,
                    mCoolReader, ca);
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(20, TimeUnit.SECONDS).
                writeTimeout(100, TimeUnit.SECONDS).
                readTimeout(100, TimeUnit.SECONDS).build();
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
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
                    mCallback.onComplete(LitresPerformAction.this,"LitresSearchPersonList", sBody);
                }, 100));
            }

            public void onFailure(Call call, IOException e) {
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() ->
                                mCallback.onError(LitresPerformAction.this, e.getMessage(), e)));
            }
        });
    }

    public void LitresSearchBooks(final CloudAction ca) throws JSONException, NoSuchAlgorithmException, IOException {
        String  uniqueID = UUID.randomUUID().toString();
        ca.param2 = uniqueID;
        JSONObject json = null;
        if (
            (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS) ||
            (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS_BY_GENRE) ||
            (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS_BY_COLLECTION) ||
            (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS_BY_SEQUENCE) ||
            (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS_BY_PERSON)
        )
            json = LitresJsons.r_search_arts(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId,
                mCoolReader, ca);
        if (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_MY_BOOKS)
            json = LitresJsons.r_my_arts(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId,
                    mCoolReader, ca);
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(20, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).build();
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

//        final Request copy = request.newBuilder().build();
//        final Buffer buffer = new Buffer();
//        String h = copy.headers().toString();
//        String m = copy.method();
//        copy.body().writeTo(buffer);
//        String s = buffer.readUtf8();
//        mCoolReader.showToast(h + m + s);

        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
                    mCallback.onComplete(LitresPerformAction.this,"SearchBooks", sBody);
                }, 100));
            }

            public void onFailure(Call call, IOException e) {
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() ->
                                mCallback.onError(LitresPerformAction.this, e.getMessage(), e)));
            }
        });
    }

    public void LitresPurchaseBook(final CloudAction ca) throws JSONException, NoSuchAlgorithmException {
        String  uniqueID = UUID.randomUUID().toString();
        ca.param2 = uniqueID;
        JSONObject json = LitresJsons.w_buy_arts(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId,
                mCoolReader, ca);
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(20, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).build();
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
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
                    mCallback.onComplete(LitresPerformAction.this,"PurchaseBook", sBody);
                }, 100));
            }

            public void onFailure(Call call, IOException e) {
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() ->
                                mCallback.onError(LitresPerformAction.this, e.getMessage(), e)));
            }
        });
    }

    public void LitresDownloadBook_old(final CloudAction ca) throws JSONException, NoSuchAlgorithmException {
        String  uniqueID = UUID.randomUUID().toString();
        ca.param2 = uniqueID;
        JSONObject json = LitresJsons.catalit_download_book(uniqueID, BuildConfig.LITRES_APP, BuildConfig.LITRES_SECRET, mCoolReader.litresCloudSettings.sessionId,
                mCoolReader, ca);
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(20, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).build();
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
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                String sBody = response.body().string();
                BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
                    mCallback.onComplete(LitresPerformAction.this,"PurchaseBook", sBody);
                }, 100));
            }

            public void onFailure(Call call, IOException e) {
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() ->
                                mCallback.onError(LitresPerformAction.this, e.getMessage(), e)));
            }
        });
    }

    public void LitresDownloadBook(final CloudAction ca) {
        final FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
        final String fName = downloadDir.pathname + Utils.transcribeFileName(ca.fi.getFilename()) + ca.fi.format_chosen;
        final File fPath = new File(downloadDir.pathname);
        if (!fPath.exists()) fPath.mkdir();
        BackgroundThread.instance().postBackground(() -> {
            FileOutputStream fos;
            try {
                InputStream in;
                String redir_url = Utils.getUrlLoc(new java.net.URL(LitresJsons.LITRES_ADDR_DOWNLOAD));
                if (StrUtils.isEmptyStr(redir_url)) redir_url = LitresJsons.LITRES_ADDR_DOWNLOAD;
                HttpUrl.Builder urlBuilder = HttpUrl.parse(redir_url).newBuilder();
                urlBuilder.addQueryParameter("sid", mCoolReader.litresCloudSettings.sessionId);
                urlBuilder.addQueryParameter("art", "" + ca.fi.id);
                urlBuilder.addQueryParameter("type", ca.fi.format_chosen);
                String url = urlBuilder.build().toString();
                in = new java.net.URL(url).openStream();
                final File fBook = new File(fName);
                fos = new FileOutputStream(fBook);
                BufferedOutputStream out = new BufferedOutputStream(fos);
                Utils.copyStreamContent(out, in);
                out.flush();
                fos.getFD().sync();
                BackgroundThread.instance().postBackground(() ->
                        BackgroundThread.instance().postGUI(() -> mCoolReader.
                                loadDocumentExt(fBook.getPath(), url), 500));
            } catch (Exception e) {
                mCoolReader.showToast(mCoolReader.getString(R.string.not_exists_on_server));
            }
        });
    }
}

