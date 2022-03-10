// StarDict to SQLLite loader was taken from https://github.com/tuanna-hsp/stardict-to-sqlite

package org.coolreader.db.StarDict;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.coolreader.crengine.Engine;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.Scanner;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class StartDictDB {

	public static final Logger log = L.create("sddb", Log.WARN);

	public SQLiteDatabase currentDB;
	private StardictManager sdManager;

	public static void flushAndTransaction(SQLiteDatabase db) {
		boolean bNeedStart = false;
		if (db != null && db.inTransaction()) {
			db.setTransactionSuccessful();
			bNeedStart = true;
		} else {
		}
		if (db.inTransaction()) db.endTransaction();
		if (bNeedStart) db.beginTransaction();
	}

	public boolean createDatabase(String dicPath) {
		currentDB = null;
		currentDB = SQLiteDatabase.openOrCreateDatabase(dicPath, null);
		currentDB.rawQuery("PRAGMA synchronous=3", null);
		currentDB.rawQuery("PRAGMA journal_mode = DELETE", null);
		String sql = "DROP TABLE IF EXISTS main";
		currentDB.execSQL(sql);
		sql = "CREATE TABLE main (" +
			"id         INTEGER PRIMARY KEY AUTOINCREMENT," +
			"word       VARCHAR NOT NULL," +
			"word_orig  VARCHAR NOT NULL," +
			"meaning    VARCHAR NOT NULL)";
		currentDB.execSQL(sql);
		sql = "CREATE INDEX word ON main (word ASC)";
		currentDB.execSQL(sql);
		sql = "DROP TABLE IF EXISTS syn";
		currentDB.execSQL(sql);
		sql = "CREATE TABLE syn (" +
			"synonym      VARCHAR NOT NULL," +
			"synonym_orig VARCHAR NOT NULL," +
			"word_id      INTEGER)";
		currentDB.execSQL(sql);
		sql = "CREATE INDEX synonym ON syn (synonym ASC)";
		currentDB.execSQL(sql);
		currentDB.close();
		return true;
	}

	private static String escapeSqlString(String input) {
		// Only deal with single quote.
		if (input.lastIndexOf('\'') == -1)
			return input;

		char[] chars = input.toCharArray();
		// Allocate new char array with additional space for
		// escape characters.
		char[] newChars = new char[chars.length * 3 / 2];

		int j = 0;
		for (int i = 0; i < chars.length; i++, j++) {
			if (chars[i] == '\'') {
				newChars[j] = '\'';
				newChars[++j] = '\'';
			} else
				newChars[j] = chars[i];
		}

		return new String(newChars, 0, j);
	}

	public void insertToMainTable(String word, String definition) {
		String insertSQL = "INSERT INTO main (word, meaning) VALUES ('"+ escapeSqlString(word.toUpperCase()) +"','"+
				escapeSqlString( definition)+"');";
		currentDB.execSQL(insertSQL);
	}

	public void insertToMainTableArr(HashMap<String, String> vals) {
		int i = 0;
		String insertSQL = "";
		for (Map.Entry<String, String> entry : vals.entrySet()) {
			i++;
			String key = entry.getKey();
			String value = entry.getValue();
			if (i == 1) insertSQL = "INSERT INTO main (word, word_orig, meaning) select '"+
					escapeSqlString(key.toUpperCase()) +"' as word,'"+
					escapeSqlString(key) +"' as word_orig,'" + escapeSqlString(value)+"' as meaning";
			else insertSQL += " union all select '"+
				escapeSqlString(key.toUpperCase()) +"','"+
					escapeSqlString(key) +"','" + escapeSqlString(value)+"'";
		}
		insertSQL += ";";
		if (i > 0) currentDB.execSQL(insertSQL);
	}

	public void insertToSynTable(String synonym, int wordIndex) {
		// Need to add 1 to synIndex before inserting, because by default the start value
		// of SQLite autoincrement is 1, whereas stardict synonym indexes start at 0
		String sql = "INSERT INTO syn (synonym, synonym_orig, word_id) VALUES ('"+
				escapeSqlString(synonym.toUpperCase()) +"','"+
				escapeSqlString(synonym) +"',"+
				(wordIndex + 1) +");";
		currentDB.execSQL(sql);
	}

	public boolean loadDic(String dicPath, String dicName,
			final Scanner.ScanControl control, final Engine.ProgressControl progress) {
		sdManager = new StardictManager();
		sdManager.setDictFilesLocation(dicPath, dicName);
		currentDB = SQLiteDatabase.openOrCreateDatabase(dicPath + "/" + dicName + ".db", null);
		int count = 0;
		int totalWords = sdManager.getSynWordCount() + sdManager.getWordCount();
		log.i("Processing .idx and .dict files...");
		boolean stopped = false;
		HashMap<String, String> mapWords = new HashMap<>();
		while (sdManager.nextWordAvailable()) {
			count++;
			if (control.isStopped()) {
				stopped = true;
				break;
			}
			StardictManager.StardictWord word = sdManager.nextStardictWord();
			mapWords.put(word.getWord(), word.getDefinition());
			if (mapWords.size() % 20 == 0) {
				insertToMainTableArr(mapWords);
				mapWords.clear();
			}
			if (count % 100 == 0)
				progress.setProgress(9999, "" + count);
		}
		insertToMainTableArr(mapWords);
		if (!stopped)
			if (sdManager.hasSynFile()) {
				log.i("Processing .syn file...\n");
				while (sdManager.nextSynWordAvailable()) {
					count++;
					if (control.isStopped()) {
						stopped = true;
						break;
					}
					StardictManager.SynWord word = sdManager.nextSynWord();
					insertToSynTable(word.getWord(), word.getSynIndex());
					if (count % 100 == 0)
						progress.setProgress(9999, "" + count);

				}
		} else if (sdManager.getSynWordCount() > 0)
			log.i("Missing synonym file!\n");
		if (currentDB.inTransaction()) currentDB.setTransactionSuccessful();
		if (currentDB.inTransaction()) currentDB.endTransaction();
		currentDB.close();
		log.i("Writing data to disk...\n");
		log.i("All finished.");
		progress.setProgress(10000, ""); // hide
		return !stopped;
	}

}
