package org.coolreader.cloud.dropbox;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.SearchMatch;
import com.dropbox.core.v2.files.SearchResult;

import org.coolreader.crengine.StrUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Async task to list items in a folder
 * https://www.programcreek.com/java-api-examples/?api=com.dropbox.core.v2.files.FileMetadata
 */
class DBXListFolderTask extends AsyncTask<String, Void, List<Metadata>> {

    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private String mFolder;
    private String mFindStr;

    public interface Callback {
        void onDataLoaded(List<Metadata> result, String folder, String findStr);

        void onError(Exception e);
    }

    public DBXListFolderTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(List<Metadata> result) {
        super.onPostExecute(result);

        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDataLoaded(result, mFolder, mFindStr);
        }
    }

    @Override
    protected List<Metadata> doInBackground(String... params) {
        try {
            mFolder = params[0];
            mFindStr = params[1];
            if (mFolder.equals("root")) mFolder = "";
            mFolder = mFolder.replace("\\", "/");
            System.out.println("DBX: mFolder = " + mFolder);
            if ((StrUtils.isEmptyStr(mFolder))&&(StrUtils.isEmptyStr(mFindStr))) {
                return mDbxClient.files().listFolder("").getEntries();
            } else {
                ListFolderResult result = null;
                SearchResult sres = null;
                if (StrUtils.isEmptyStr(mFindStr)) {
                    result = mDbxClient.files().listFolderBuilder(mFolder).start();
                    ArrayList<Metadata> al = new ArrayList<Metadata>();
                    for (Metadata md: result.getEntries()) al.add(md);
                    if (result.getHasMore()) {
                        result = mDbxClient.files().listFolderContinue(result.getCursor());
                        for (Metadata md: result.getEntries()) al.add(md);
                    }
                    System.out.println("DBX: mFolder = " + mFolder);
                    return result.getEntries();
                } else {
                    sres = mDbxClient.files().search(mFolder, mFindStr);
                    System.out.println("DBX: mFolder = " + mFolder+"; mFindStr = " + mFindStr);
                    ArrayList<Metadata> al = new ArrayList<Metadata>();
                    for (SearchMatch sm: sres.getMatches()) al.add(sm.getMetadata());
                    return al;
                }
//                if(result.getHasMore()){
//                    result = mDbxClient.files().listFolderContinue(result.getCursor());
//                }

            }
        } catch (DbxException e) {
            mException = e;
        }
        return null;
    }
}
