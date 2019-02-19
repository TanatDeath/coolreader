package org.coolreader.crengine;

import android.os.AsyncTask;
import android.util.Log;

import org.coolreader.CoolReader;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class SimpleDownload extends AsyncTask<String, Void, String> {

    public CoolReader mCoolReader;
    public String mLink;
    public String mFName;

    protected String doInBackground(String... urls) {

        String responseStr = null;
        String sLink = urls[0];
        mLink = sLink;
        String sFName = urls[1];
        mFName = sFName;
        try {
            for (String url : urls) {
                FileOutputStream fos = new FileOutputStream(sFName);
                BufferedOutputStream out = new BufferedOutputStream(fos);

                URL u = new URL(sLink);
                InputStream is = u.openStream();

                DataInputStream in = new DataInputStream(is);
                byte[] buffer = new byte[8192];
                int len = 0;
                while ((len = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
                fos.getFD().sync();
                mCoolReader.pictureCame(sFName);
            }
        } catch (FileNotFoundException e) {
            Log.e("cr3", "file download FileNotFoundException: "+e.getMessage());
        } catch (MalformedURLException e) {
            Log.e("cr3", "file download MalformedURLException: " + e.getMessage());
        } catch (IOException e) {
            Log.e("cr3", "file download IOException: " + e.getMessage());
        }
        return responseStr;
    }

    protected void onPostExecute(String result) {
        mCoolReader.showToast("done!! "+mFName);
        //tv.setText(result);
    }
}
