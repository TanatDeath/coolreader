package org.coolreader.cloud.dropbox;

import android.net.Uri;
import android.os.AsyncTask;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;

import org.coolreader.CoolReader;
import org.coolreader.cloud.CloudAction;
import org.coolreader.crengine.BackgroundThread;

import java.util.HashMap;

/**
 * Async task for finishing auth
 */
public class DBXFinishAuthorization extends AsyncTask<String, Void, Boolean> {

    public final CoolReader mCoolReader;
    public final Callback mCallback;
    public final Uri mUri;
    public Exception mException;
    public String access_token = "";

    public interface Callback {
        void onComplete(boolean result, String accessToken);
        void onError(Exception e);
    }

    public DBXFinishAuthorization(CoolReader cr, Uri uri, Callback callback) {
        mCoolReader = cr;
        mCallback = callback;
        mUri = uri;
    }

    @Override
    protected void onPostExecute(Boolean ok) {
        super.onPostExecute(ok);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onComplete(ok, access_token);
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {

        try {
            DbxAuthFinish authFinish;
            if (mUri!=null) {
                HashMap<String, String[]> p = new HashMap<String, String[]>();
                p.put("code",new String[]{mUri.getQueryParameter("code")});
                p.put("state",new String[]{mUri.getQueryParameter("state")});
                authFinish = DBXConfig.webAuth.finishFromRedirect(DBXConfig.DBX_REDIRECT_URL,
                        DBXConfig.sessionStore, p);
                access_token = authFinish.getAccessToken();
            }
            else {
                access_token = params[0];
                //authFinish = DBXConfig.webAuth.finishFromCode(params[0]);
//                HashMap<String, String[]> p = new HashMap<String, String[]>();
//                p.put("code",new String[]{params[0]});
//                authFinish = DBXConfig.webAuth.finishFromRedirect(DBXConfig.DBX_REDIRECT_URL,
//                        DBXConfig.sessionStore, p);
            }
            DBXConfig.mDbxClient = new DbxClientV2(DBXConfig.mDbxRequestConfig, access_token);
            DBXConfig.didLogin = true;
            BackgroundThread.instance().postGUI(new Runnable() {
                @Override
                public void run() {
                    CloudAction.dbxOpenBookDialog(mCoolReader);
                }
            }, 500);
        } catch (Exception e) {
            System.err.println("Error in DbxWebAuth.authorize: " + e.getMessage());
            mException = e;
        }

        return true;
    }
}
