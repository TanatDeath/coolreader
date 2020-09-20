package org.coolreader.library;

import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.Engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.CRC32;

public class AuthorAlias {

	public static Long FILE_SIZE;
	public static CRC32 FILE_CRC;

	public Long origId;
	public String origText;
	public Long aliasId;
	public String aliasText;

	public static ArrayList<AuthorAlias> AUTHOR_ALIASES = new ArrayList<>();

	public static void initAliasesList(CoolReader activity) throws FileNotFoundException {
		AUTHOR_ALIASES.clear();
		File[] dataDirs = Engine.getDataDirectories(null, false, true);
		File existingFile = null;
		for (File dir : dataDirs) {
			File f = new File(dir, "aliases.utf8.txt");
			if (f.exists() && f.isFile()) {
				existingFile = f;
				break;
			}
		}
		InputStream targetStream;
		if (existingFile != null)
			targetStream = new FileInputStream(existingFile);
		else
			targetStream = activity.getResources().openRawResource(R.raw.aliases_utf8);
		if (targetStream != null) {
			try {
				//FILE_SIZE = targetStream.available();
				BufferedReader reader = new BufferedReader(new InputStreamReader(targetStream));
				String str = "";
				while ((str = reader.readLine()) != null) {
					if (str.contains("=")) {
						AuthorAlias al = new AuthorAlias();
						al.origText=str.split("=")[0];
						al.aliasText=str.split("=")[1];
						AUTHOR_ALIASES.add(al);
					}
				};
				targetStream.close();
			} catch (Exception e) {
				activity.showToast("Could not parse genres from file: genres_rus.txt. " +
						e.getClass().getSimpleName() + " " + e.getMessage());
			}
		}
	};
}
