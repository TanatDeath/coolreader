package org.coolreader.cloud.dropbox;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;

import org.coolreader.CoolReader;
import org.coolreader.cloud.CloudAction;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Async task to list items in a folder
 */
class DBXDownloadFileTask extends AsyncTask<CloudAction, Void, List<Metadata>> {

    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private CoolReader mCoolReader;
    private String mFile;

    public interface Callback {
        void onDataLoaded(List<Metadata> result, String file);

        void onError(Exception e);
    }

    public DBXDownloadFileTask(CoolReader cr, DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
        mCoolReader = cr;
    }

    @Override
    protected void onPostExecute(List<Metadata> result) {
        super.onPostExecute(result);

        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDataLoaded(result, mFile);
        }
    }

    @Override
    protected List<Metadata> doInBackground(CloudAction... params) {
        // Download the file.
        CloudAction ca = params[0];
        mFile = ca.mFile;
        try {
        String sFName = ca.param.replace("\\", "/");
        OutputStream outputStream = new FileOutputStream(mFile);
        mDbxClient.files().download(ca.mDbxMd.getPathLower())
                    .download(outputStream);
        } catch (DbxException e) {
            mException = e;
        } catch (FileNotFoundException e1) {
            mException = e1;
        } catch (IOException e2) {
            mException = e2;
        }
        return null;
    }
}
