package org.coolreader.db;

import java.io.File;

import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.Utils;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public abstract class BaseDB {

	public static final Logger log = L.create("bdb");
	public static final Logger vlog = L.create("bdb", Log.INFO);

	public String mOriginalDBFile = "";
	public String mBackedDBFile = "";
	public String mDeletedDBFile = "";
	public String mBackupRestoredDBFile = "";
	public String mBackupRestoredToDBFile = "";

	protected SQLiteDatabase mDB;
	private File mFileName;
	public boolean restoredFromBackup;
	private boolean error = false;

	public File getFileName() {
		return mFileName;
	}

	public boolean isOpened() {
		if (mDB != null && !error)
			return true;
		log.w("DB access while not opened");
		return false;
	}

	public boolean open(File dir) {
		error = false;
		File dbFile = new File(dir, dbFileName());
		log.i("opening DB " + dbFile);
		mFileName = dbFile;
		mDB = openDB(dbFile);
		if (mDB == null) {
			return false;
		}
		boolean res = checkSchema();
		if (!res) {
			log.e("Closing DB due error while upgrade of schema: " + dbFile.getAbsolutePath());
			close();
			Utils.moveCorruptedFileToBackup(this, dbFile);
			if (!restoredFromBackup)
				Utils.restoreFromBackup(this, dbFile);
			mDB = openDB(dbFile);
			res = checkSchema();
			if (!res)
				close();
		}
		if (mDB != null) {
			return true;
		}
		return false;
	}

	public boolean close() {
		if (mDB != null) {
			try {
				log.i("Closing database");
				flush();
				clearCaches();
				mDB.close();
				mDB = null;
				return true;
			} catch (SQLiteException e) {
				log.e("Error while closing DB " + mFileName);
			}
			mDB = null;
		}
		return false;
	}

	protected boolean checkSchema() {
		try {
			upgradeSchema();
			return true;
		} catch (SQLiteException e) {
			log.e(e.getMessage());
			return false;
		}
	}

	protected abstract boolean upgradeSchema();

	protected abstract String dbFileName();

	private SQLiteDatabase openDB(File dbFile) {
		restoredFromBackup = false;
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			db.rawQuery("PRAGMA synchronous=3", null);
			db.rawQuery("PRAGMA journal_mode = DELETE", null);
			return db;
		} catch (SQLiteException e) {
			log.e("Error while opening DB " + dbFile.getAbsolutePath() + ". " + e.getMessage());
			Utils.moveCorruptedFileToBackup(this, dbFile);
			restoredFromBackup = Utils.restoreFromBackup(this, dbFile);
			try {
				db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
				db.rawQuery("PRAGMA synchronous=3", null);
				db.rawQuery("PRAGMA journal_mode = DELETE", null);
				return db;
			} catch (SQLiteException ee) {
				log.e("Error while opening DB " + dbFile.getAbsolutePath() + ". " + e.getMessage());
			}
		}
		return null;
	}

	public void execSQLIgnoreErrors(String... sqls) {
		boolean bDML = false;
		for (String sql : sqls) {
			try {
				if (
						(sql.toUpperCase().contains("UPDATE")) ||
								(sql.toUpperCase().contains("INSERT")) ||
								(sql.toUpperCase().contains("DELETE"))
						) bDML = true;
				mDB.execSQL(sql);
			} catch (SQLException e) {
				// ignore
				Log.w("cr3db", "query failed, ignoring: " + sql);
			}
		}
		if (bDML) flushAndTransaction();
	}

	protected void ensureOpened() {
		if (!isOpened())
			throw new DBRuntimeException("DB is not opened");
	}

	public void execSQL(String... sqls) {
		ensureOpened();
		boolean bDML = false;
		for (String sql : sqls) {
			try {
				if (
						(sql.toUpperCase().contains("UPDATE")) ||
								(sql.toUpperCase().contains("INSERT")) ||
								(sql.toUpperCase().contains("DELETE"))
						) bDML = true;
				mDB.execSQL(sql);
			} catch (SQLException e) {
				// ignore
				Log.w("cr3", "query failed: " + sql);
				throw e;
			}
		}
		if (bDML) flushAndTransaction();
	}

	public Long longQuery(String sql) {
		ensureOpened();
		SQLiteStatement stmt = null;
		try {
			stmt = mDB.compileStatement(sql);
			return stmt.simpleQueryForLong();
		} catch (Exception e) {
			// not found or error
			return null;
		} finally {
			if (stmt != null)
				stmt.close();
		}
	}

	public String stringQuery(String sql) {
		ensureOpened();
		SQLiteStatement stmt = null;
		try {
			stmt = mDB.compileStatement(sql);
			return stmt.simpleQueryForString();
		} catch (Exception e) {
			// not found or error
			return null;
		} finally {
			if (stmt != null)
				stmt.close();
		}
	}

	public static String quoteSqlString(String src) {
		if (src == null)
			return "null";
		String s = src.replaceAll("\\'", "\\'\\'");
		return "'" + s + "'";
	}

	public void clearCaches() {
		// override it
	}

	private boolean changed = false;

	/**
	 * Begin transaction, if not yet started, for changes.
	 */
	public void beginChanges() {
		if (!mDB.inTransaction()) {
			vlog.v("starting writable transaction");
			mDB.beginTransaction();
		}
		if (!changed) {
			vlog.v("modify readonly transaction to writable");
			changed = true;
		}
	}

	/**
	 * Begin transaction, if not yet started, for faster reading.
	 */
	public void beginReading() {
		if (mDB != null) {
			if (!mDB.inTransaction()) {
				vlog.v("starting transaction");
				mDB.beginTransaction();
			}
		}
	}

	/**
	 * Rolls back transaction, if writing is not started.
	 */
	public void endReading() {
		if (mDB.inTransaction() && !changed) {
			vlog.v("ending transaction");
			mDB.setTransactionSuccessful();
			mDB.endTransaction();
		}
	}

	/**
	 * Commits or rolls back transaction, if started, and frees DB resources.
	 * Will commit only if beginChanges() has been called. Otherwise will roll back.
	 */
	public void flush() {
		if (mDB != null && mDB.inTransaction()) {
			if (changed) {
				changed = false;
				mDB.setTransactionSuccessful();
				log.i("flush: committing changes");
			} else {
				log.i("flush: rolling back changes");
			}
			mDB.endTransaction();
		}
	}

	public void flushAndTransaction() {
		boolean bNeedStart = false;
		if (mDB != null && mDB.inTransaction()) {
			mDB.setTransactionSuccessful();
			bNeedStart = true;
			log.i("flush: committing changes");
		} else {
			log.i("flush: was not in transaction");
		}
		if (mDB.inTransaction()) mDB.endTransaction();
		if (bNeedStart) mDB.beginTransaction();
	}
}
