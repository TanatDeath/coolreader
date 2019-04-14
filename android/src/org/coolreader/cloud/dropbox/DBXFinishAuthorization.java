package org.coolreader.cloud.dropbox;

import android.net.Uri;
import android.os.AsyncTask;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;

import org.coolreader.CoolReader;

import java.util.HashMap;

/**
 * Async task for finishing auth
 */
public class DBXFinishAuthorization extends AsyncTask<String, Void, Boolean> {

    public final CoolReader mCoolReader;
    public final Callback mCallback;
    public final Uri mUri;
    public Exception mException;

    public interface Callback {
        void onComplete(boolean result);
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
            mCallback.onComplete(ok);
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
            }
            else
                authFinish = DBXConfig.webAuth.finishFromCode(params[0]);
            DBXConfig.mDbxClient = new DbxClientV2(DBXConfig.mDbxRequestConfig, authFinish.getAccessToken());
            DBXConfig.didLogin = true;
        } catch (Exception e) {
            System.err.println("Error in DbxWebAuth.authorize: " + e.getMessage());
            mException = e;
        }

        return true;
    }
}
