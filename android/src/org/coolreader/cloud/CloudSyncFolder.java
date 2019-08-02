package org.coolreader.cloud;

import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.StrUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class CloudSyncFolder {

    private static final String TAG = "CloudSyncFolder";

    public static void saveSettingsFile(CoolReader cr, boolean bQuiet) {
        int iSettClount = 1;
        ArrayList<File> arrSett = new ArrayList<File>();
        File fSett = cr.getSettingsFile(0);
        while (fSett.exists()) {
            arrSett.add(fSett);
            fSett = cr.getSettingsFile(iSettClount);
            iSettClount++;
        }
        ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CloudSyncDirs, true);
        String sDir = "";
        if (tDirs.size() > 0) sDir = tDirs.get(0);
        if (!StrUtils.isEmptyStr(sDir))
            if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
        if (!StrUtils.isEmptyStr(sDir)) {
            Log.d(TAG, "Starting save cr3.ini files to drive...");
            final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
            final CharSequence sFName0 = dfmt.format("yyyy-MM-dd_kkmmss", new java.util.Date());
            for (File fS : arrSett) {
                final String sFName = sFName0.toString() + "_" + fS.getName() + "_" + cr.getAndroid_id();
                boolean bWasErr = false;
                try {
                    FileInputStream fin=new FileInputStream(fS);
                    byte[] buffer = new byte[fin.available()];
                    fin.read(buffer, 0, buffer.length);
                    File f = new File(sDir + sFName);
                    if (f.exists()) f.delete();
                    File file = new File(sDir + sFName);
                    OutputStream fOut = new FileOutputStream(file);
                    fOut.write(buffer, 0, buffer.length);
                    fOut.flush();
                    fOut.close();
                } catch (Exception e) {
                    bWasErr = true;
                    if (!bQuiet) cr.showToast(cr.getString(R.string.cloud_error)+": Error saving file ("+e.getClass().getSimpleName()+")");
                }
            } //for (File fS : arrSett) {
        }
    }
}
