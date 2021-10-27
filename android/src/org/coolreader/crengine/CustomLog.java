package org.coolreader.crengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomLog {
    final static DateFormat df = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss.SSS");

    public static void doLog(String sRootName, String sFileName, String sLogData) {
        if (StrUtils.isEmptyStr(sRootName)) return;
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(sRootName + sFileName,true));
            String sDate = df.format(new Date())+" ";
            writer.println(sDate+sLogData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (writer!=null) writer.close();
    }
}
