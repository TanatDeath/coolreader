package org.coolreader.dic;

import android.database.sqlite.SQLiteDatabase;

import java.util.Date;

public class OfflineInfo {

	public String dicVersion;
	public String dicPath; // no trailing backslash
	public String dicNameWOExt;
	public String dicName;
	public Date dicDate;
	public int wordCount;
	public String dicDescription;
	public boolean dbExists;
	public boolean ifoExists;
	public boolean dictExists;

	public SQLiteDatabase db = null;

	public String getDBFullPath() {
		return dicPath + "/" + dicNameWOExt + ".db";
	}

}
