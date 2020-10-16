package org.coolreader.library;

import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.StrUtils;

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
	public String origTextR;
	public Long aliasId;
	public String aliasText;
	public String aliasTextR;

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
						String orig = StrUtils.getNonEmptyStr(str.split("=")[0],true);
						al.origText=orig;
						String alias = StrUtils.getNonEmptyStr(str.split("=")[1],true);
						al.aliasText=alias;
						if (orig.split(" ").length > 1) {
							String[] aorig = orig.split(" ");
							String origN = "";
							String origL = aorig[0];
							for (int i = 1; i <= aorig.length - 1; i++) {
								origN = origN + " " + aorig[i];
							}
							orig = origN + " " + origL;
						}
						al.origTextR=StrUtils.getNonEmptyStr(orig, true);
						if (alias.split(" ").length > 1) {
							String[] aalias = alias.split(" ");
							String aliasN = "";
							String aliasL = aalias[0];
							for (int i = 1; i <= aalias.length - 1; i++) {
								aliasN = aliasN + " " + aalias[i];
							}
							alias = aliasN + " " + aliasL;
						}
						al.aliasTextR=StrUtils.getNonEmptyStr(alias, true);
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
