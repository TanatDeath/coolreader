package org.coolreader.cloud.dropbox;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.v2.DbxClientV2;

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DBXConfig {
    public static final String DBX_KEY = BuildConfig.DBX_KEY; //"put_yours";
    public static final String DBX_SECRET = BuildConfig.DBX_SECRET; //"put_yours";
    public static final String DBX_REDIRECT_URL = "https://github.com/plotn/coolreader/blob/master/dbx_readme.md";
    public static DbxRequestConfig mDbxRequestConfig = null;
    public static DbxClientV2 mDbxClient = null;
    public static DbxWebAuth webAuth = null;
    public static DbxSessionStore sessionStore = null;
    public static boolean didLogin = false;

    public static boolean init(CoolReader cr) throws IOException {
        // Create Dropbox client
        final File fDBX = new File(cr.getSettingsFileExt("[DEFAULT]",0).getParent() + "/dbx.token");
        if (!fDBX.exists()) {
            cr.showToast(R.string.cloud_need_authorization);
            cr.dbxInputTokenDialog = new DBXInputTokenDialog(cr);
            cr.dbxInputTokenDialog.show();
            return false;
        } else {
            //mDbxRequestConfig = DbxRequestConfig.newBuilder("CoolReaderExperience").build();
            if (!didLogin) {
                mDbxRequestConfig = new DbxRequestConfig("CoolReaderExperience");
//                mDbxRequestConfig = DbxRequestConfig.newBuilder("CoolReaderExperience")
//                        .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
//                        .build();
                DbxAppInfo appInfo = new DbxAppInfo(DBXConfig.DBX_KEY, DBXConfig.DBX_SECRET);
                DBXConfig.webAuth = new DbxWebAuth(DBXConfig.mDbxRequestConfig, appInfo);
                DBXConfig.sessionStore = new DBXInputTokenDialog.SimpleSessionStore();
//                DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
//                        .withRedirectUri(DBXConfig.DBX_REDIRECT_URL, DBXConfig.sessionStore)
//                        .build();
                DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                        .withNoRedirect()
                        .build();
                String authorizeUrl = DBXConfig.webAuth.authorize(webAuthRequest);

                BufferedReader reader = new BufferedReader(
                        new FileReader(fDBX));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                String ls = System.getProperty("line.separator");
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(ls);
                }
                reader.close();
                final String token = stringBuilder.toString().trim();
                cr.showCloudToast("Using Dropbox token: [" + token + "]",false);
                final CoolReader crf = cr;
                new DBXFinishAuthorization(cr, null, new DBXFinishAuthorization.Callback() {
                    @Override
                    public void onComplete(boolean result, String accessToken) {
                        crf.showCloudToast(R.string.dbx_auth_finished_ok,false);
                        didLogin = result;
                    }

                    @Override
                    public void onError(Exception e) {
                        DBXConfig.didLogin = false;
                        crf.showCloudToast(crf.getString(R.string.dbx_auth_finished_error) + ": " + e.getMessage(),true);
                    }
                }).execute(token);
                return false;
            }
            return true;
        }
    }
}
