package org.coolreader.crengine;
// unused...

import android.os.AsyncTask;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;

import org.coolreader.CoolReader;
import org.coolreader.cloud.dropbox.DBXConfig;

/**
 * Async task for calc book stats
 */
public class StatisticsTask extends AsyncTask<FileInfo, Void, BookInfo> {

    private final ReaderView mReaderView;
    public final Callback mCallback;
    public Exception mException;

    public interface Callback {
        void onComplete(BookInfo bi);
        void onError(Exception e);
    }

    public StatisticsTask(ReaderView rv, Callback callback) {
        mReaderView = rv;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(BookInfo bi) {
        super.onPostExecute(bi);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onComplete(bi);
        }
    }

    @Override
    protected BookInfo doInBackground(FileInfo... params) {

        FileInfo fi = params[0];
        BookInfo bi = new BookInfo(fi);
        try {
            int iPageCnt = 0;
            int iSymCnt = 0;
            int iWordCnt = 0;
            if (mReaderView.getArrAllPages()!=null)
                iPageCnt = mReaderView.getArrAllPages().size();
            else {
                mReaderView.CheckAllPagesLoadVisual();
                iPageCnt = mReaderView.getArrAllPages().size();
            }
            for (int i=0;i<iPageCnt;i++) {
                String sPage = mReaderView.getArrAllPages().get(i);
                if (sPage == null) sPage = "";
                sPage = sPage.replace("\\n", " ");
                sPage = sPage.replace("\\r", " ");
                iSymCnt=iSymCnt + sPage.replaceAll("\\s+"," ").length();
                iWordCnt=iWordCnt + sPage.replaceAll("\\p{Punct}", " ").
                        replaceAll("\\s+"," ").split("\\s").length;
            }
            fi.symCount = iSymCnt;
            fi.wordCount = iWordCnt;
        } catch (Exception e) {
            System.err.println("Error in statistics task: " + e.getMessage());
            mException = e;
        }

        return bi;
    }
}
