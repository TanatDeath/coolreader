package org.coolreader.dic;

import android.database.sqlite.SQLiteDatabase;
import io.github.eb4j.dsl.DslDictionary;

import com.google.gson.annotations.Expose;

import java.io.File;
import java.util.Date;

public class OfflineDicInfo {

	@Expose
	public String dicVersion;
	@Expose
	public String dicPath; // no trailing backslash
	@Expose
	public String dicNameWOExt;
	@Expose
	public String dicName;
	@Expose
	public Date dicDate;
	@Expose
	public int wordCount;
	@Expose
	public String dicDescription;
	public boolean dbExists;
	public boolean ifoExists;
	public boolean dictDzExists;
	public boolean dictExists;
	public boolean dslDzExists;
	public boolean dslExists;
	public boolean annExists;
	public boolean idxExists;
	@Expose
	public boolean dicEnabled = true;
	@Expose
	public String langFrom = "";
	@Expose
	public String langTo = "";
	@Expose
	public String displayFormat = "text";

	public SQLiteDatabase db = null;

	public DslDictionary dslDic = null;

	public String getDBFullPath() {
		return dicPath + "/" + dicNameWOExt + ".db";
	}

	public void fillMarks() {
		dbExists = new File(dicPath + "/" + dicNameWOExt + ".db").exists();
		ifoExists = new File(dicPath + "/" + dicNameWOExt + ".ifo").exists();
		dictExists = new File(dicPath + "/" + dicNameWOExt + ".dict").exists();
		dictDzExists = new File(dicPath + "/" + dicNameWOExt + ".dict.dz").exists();
		idxExists = new File(dicPath + "/" + dicNameWOExt + ".idx").exists();
		annExists = new File(dicPath + "/" + dicNameWOExt + ".ann").exists();
		dslExists = new File(dicPath + "/" + dicNameWOExt + ".dsl").exists();
		dslDzExists = new File(dicPath + "/" + dicNameWOExt + ".dsl.dz").exists();
	}

}
