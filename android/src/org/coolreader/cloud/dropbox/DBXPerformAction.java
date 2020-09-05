package org.coolreader.cloud.dropbox;

import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.users.FullAccount;

import org.coolreader.CoolReader;
import org.coolreader.cloud.CloudAction;
import org.coolreader.crengine.StrUtils;

import java.io.IOException;
import java.util.List;

public class DBXPerformAction {

    public Exception mException;
    public List<CloudAction> mActionList;
    public CloudAction mCurAction = null;
    public Callback mCallback;
    public CoolReader mCoolReader;

    public interface Callback {
        void onComplete(DBXPerformAction a, String res, Object o);
        void onError(DBXPerformAction a, String res, Exception e);
    }

    public DBXPerformAction(CoolReader cr, List<CloudAction> actionList, Callback cb) {
        mActionList = actionList;
        mCallback = cb;
        mCoolReader = cr;
    }

    public String DoNextAction() {
        try {
            if (mActionList.size()>0)  {
                mCurAction = mActionList.get(0);
                mActionList.remove(0);
                if (mCurAction.action == CloudAction.DBX_GET_CURRENT_ACCOUNT) DbxGetCurrentAccount();
                if (mCurAction.action == CloudAction.DBX_LIST_FOLDER) DbxListFolder(mCurAction);
                if (mCurAction.action == CloudAction.DBX_LIST_FOLDER_THEN_OPEN_DLG) DbxListFolder(mCurAction);
                if (mCurAction.action == CloudAction.DBX_LIST_FOLDER_IN_DLG) DbxListFolder(mCurAction);
                if (mCurAction.action == CloudAction.DBX_DOWNLOAD_FILE) DbxDownloadFile(mCoolReader, mCurAction);
            } else Log.i("DBX", "End of cloud operation");
          //  return mDbxClient.users().getCurrentAccount();
        } catch (Exception e) {
            mException = e;
            return e.getMessage();
        }
        return "";
    }

    public void DbxListFolder(final CloudAction ca) throws DbxException, IOException {
        String folder = ca.param;
        String findStr = ca.param2;
        System.out.println("DBX: folder = " + folder);
        if (StrUtils.isEmptyStr(folder))
            if (!DBXConfig.init(mCoolReader)) return;
        new DBXListFolderTask(DBXConfig.mDbxClient, new DBXListFolderTask.Callback() {
            @Override
            public void onDataLoaded(List<com.dropbox.core.v2.files.Metadata> result, String folder, String findStr) {
                mCallback.onComplete(DBXPerformAction.this,"ListFolderResult", result);
            }

            @Override
            public void onError(Exception e) {
                mCallback.onError(DBXPerformAction.this, e.getMessage(), e);
            }
        }).execute(folder, findStr);
    }

    public void DbxDownloadFile(CoolReader cr, final CloudAction ca) throws DbxException {
        String sfile = ca.param;
        System.out.println("DBX: file = " + sfile);
        new DBXDownloadFileTask(cr, DBXConfig.mDbxClient, new DBXDownloadFileTask.Callback() {
            @Override
            public void onDataLoaded(List<com.dropbox.core.v2.files.Metadata> result, String folder) {
                mCallback.onComplete(DBXPerformAction.this,"DownloadFile", result);
            }

            @Override
            public void onError(Exception e) {
                mCallback.onError(DBXPerformAction.this, e.getMessage(), e);
            }
        }).execute(ca);
    }

    public void DbxGetCurrentAccount() throws DbxException, IOException {
        if (!DBXConfig.init(mCoolReader)) return;
        new DBXGetCurrentAccountTask(DBXConfig.mDbxClient, new DBXGetCurrentAccountTask.Callback() {
            @Override
            public void onComplete(FullAccount result) {
                mCallback.onComplete(DBXPerformAction.this,"FullAccount", result);
            }

            @Override
            public void onError(Exception e) {
                mCallback.onError(DBXPerformAction.this, e.getMessage(), e);
            }
        }).execute();
    }

}

