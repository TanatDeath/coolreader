package org.coolreader.db;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.*;
import org.coolreader.crengine.Properties;
import org.coolreader.library.AuthorAlias;

import java.util.*;

public class MainDB extends BaseDB {
	public static final Logger log = L.create("mdb");
	public static final Logger vlog = L.create("mdb", Log.VERBOSE);

	public static String currentLanguage = "EN"; // for genres - sets from BaseActivity

	public static int iMaxGroupSize = 8;

	private boolean pathCorrectionRequired = false;
	public static final int DB_VERSION = 49;
	@Override
	protected boolean upgradeSchema() {
		// When the database is just created, its version is 0.
		int currentVersion = mDB.getVersion();
		// TODO: check database structure consistency regardless of its version.
		if (currentVersion > DB_VERSION) {
			// trying to update the structure of a database that has been modified by some kind of <s>inconsistent</s> fork of the program.
			// upd: do not do anything, because only very old fork's (some kind of the best fork in the world :))
			// db live in the same folder, the modern - in its own
			///log.v("MainDB: incompatible database version found (" + currentVersion + "), forced setting to 26.");
			///currentVersion = 26;
		}

		log.i("DB_VERSION "+DB_VERSION);
		log.i("DB_VERSION INSTALLED "+mDB.getVersion());
		//execSQL("update book set saved_with_ver = 0 ");
		if (mDB.needUpgrade(DB_VERSION) || currentVersion < DB_VERSION) {
			log.i("DB_VERSION NEED UPGRADE");
			execSQL("CREATE TABLE IF NOT EXISTS author (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name VARCHAR NOT NULL COLLATE NOCASE," +
					"book_cnt INTEGER"+
					")");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"author_name_index ON author (name) ");
			execSQL("CREATE TABLE IF NOT EXISTS genre (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"code VARCHAR NOT NULL COLLATE NOCASE, " +
					"name VARCHAR NOT NULL COLLATE NOCASE, " +
					"book_cnt INTEGER "+
					")");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"genre_code_index ON genre (code) ");
			execSQL("CREATE TABLE IF NOT EXISTS genre_transl (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"code VARCHAR NOT NULL COLLATE NOCASE, " +
					"lang VARCHAR NOT NULL COLLATE NOCASE, " +
					"name VARCHAR NOT NULL COLLATE NOCASE" +
					")");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"genre_transl_code_lang_index ON genre_transl (code, lang) ");
			execSQL("CREATE TABLE IF NOT EXISTS series (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name VARCHAR NOT NULL COLLATE NOCASE," +
					"book_cnt INTEGER" +
					")");
			execSQL("CREATE INDEX IF NOT EXISTS " +
			        "series_name_index ON series (name) ");
			execSQL("CREATE TABLE IF NOT EXISTS folder (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name VARCHAR NOT NULL," +
					"book_cnt INTEGER "+
					")");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"folder_name_index ON folder (name) ");
			execSQL("CREATE TABLE IF NOT EXISTS book (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"pathname VARCHAR NOT NULL," +
					"folder_fk INTEGER REFERENCES folder (id)," +
					"filename VARCHAR NOT NULL," +
					"arcname VARCHAR," +
					"title VARCHAR COLLATE NOCASE," +
					"series_fk INTEGER REFERENCES series (id)," +
					"series_number INTEGER," +
					"format INTEGER," +
					"filesize INTEGER," +
					"arcsize INTEGER," +
					"create_time INTEGER," +
					"last_access_time INTEGER, " +
					"flags INTEGER DEFAULT 0, " +
					"language VARCHAR DEFAULT NULL, " +
					"lang_from VARCHAR DEFAULT NULL, " +
					"lang_to VARCHAR DEFAULT NULL, " +
					"saved_with_ver INTEGER DEFAULT 0, " +
					"genre VARCHAR DEFAULT NULL, " +
					"annotation VARCHAR DEFAULT NULL, " +
					"srclang VARCHAR DEFAULT NULL, " +
					"translator VARCHAR DEFAULT NULL, " +
					"docauthor VARCHAR DEFAULT NULL, " +
					"docprogram VARCHAR DEFAULT NULL, " +
					"docdate VARCHAR DEFAULT NULL, " +
					"docsrcurl VARCHAR DEFAULT NULL, " +
					"docsrcocr VARCHAR DEFAULT NULL, " +
					"docversion VARCHAR DEFAULT NULL, " +
					"publname VARCHAR DEFAULT NULL, " +
					"publisher VARCHAR DEFAULT NULL, " +
					"publcity VARCHAR DEFAULT NULL, " +
					"publyear VARCHAR DEFAULT NULL, " +
					"publisbn VARCHAR DEFAULT NULL, " +
                    "bookdate VARCHAR DEFAULT NULL, " +
                    "publseries_fk INTEGER REFERENCES series (id), " +
                    "publseries_number INTEGER, " +
					"file_create_time INTEGER, " +
					"sym_count INTEGER, " +
					"word_count INTEGER, " +
                    "book_date_n INTEGER, " +
					"doc_date_n INTEGER, " +
                    "publ_year_n INTEGER, " +
					"opds_link VARCHAR DEFAULT NULL, " +
					"crc32 INTEGER DEFAULT NULL ," +
					"domVersion INTEGER DEFAULT 0, " +
			        "rendFlags INTEGER DEFAULT 0" +
					")");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"book_folder_index ON book (folder_fk) ");
			execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " +
					"book_pathname_index ON book (pathname) ");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"book_filename_index ON book (filename) ");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"book_title_index ON book (title) ");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"book_last_access_time_index ON book (last_access_time) ");
			execSQL("CREATE INDEX IF NOT EXISTS " +
					"book_title_index ON book (title) ");
			execSQL("CREATE TABLE IF NOT EXISTS book_author (" +
					"book_fk INTEGER NOT NULL REFERENCES book (id)," +
					"author_fk INTEGER NOT NULL REFERENCES author (id)," +
					"PRIMARY KEY (book_fk, author_fk)" +
					")");
			execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " +
					"author_book_index ON book_author (author_fk, book_fk) ");
			execSQL("CREATE TABLE IF NOT EXISTS book_genre (" +
					"book_fk INTEGER NOT NULL REFERENCES book (id)," +
					"genre_fk INTEGER NOT NULL REFERENCES genre (id)," +
					"PRIMARY KEY (book_fk, genre_fk)" +
					")");
			execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " +
					"genre_book_index ON book_genre (genre_fk, book_fk) ");
			execSQL("CREATE TABLE IF NOT EXISTS bookmark (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"book_fk INTEGER NOT NULL REFERENCES book (id)," +
					"type INTEGER NOT NULL DEFAULT 0," +
					"percent INTEGER DEFAULT 0," +
					"shortcut INTEGER DEFAULT 0," +
					"time_stamp INTEGER DEFAULT 0," +
					"start_pos VARCHAR NOT NULL," +
					"end_pos VARCHAR," +
					"title_text VARCHAR," +
					"pos_text VARCHAR," +
					"comment_text VARCHAR, " +
					"time_elapsed INTEGER DEFAULT 0" +
					")");
			execSQL("CREATE INDEX IF NOT EXISTS " +
			"bookmark_book_index ON bookmark (book_fk) ");
			execSQL("CREATE TABLE IF NOT EXISTS book_dates_stats (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"date_field VARCHAR NOT NULL COLLATE NOCASE," +
					"book_date INTEGER, " +
					"book_cnt INTEGER"+
					")");
			execSQL("CREATE TABLE IF NOT EXISTS book_titles_stats (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"text_field VARCHAR NOT NULL COLLATE NOCASE," +
					"stat_level INTEGER," +
					"text_value VARCHAR NOT NULL COLLATE NOCASE," +
					"book_cnt INTEGER"+
					")");
			// ====================================================================
			if (currentVersion < 1)
				execSQLIgnoreErrors("ALTER TABLE bookmark ADD COLUMN shortcut INTEGER DEFAULT 0");
			if (currentVersion < 4)
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN flags INTEGER DEFAULT 0");
			if (currentVersion < 6)
				execSQL("CREATE TABLE IF NOT EXISTS opds_catalog (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name VARCHAR NOT NULL COLLATE NOCASE, " +
                        "url VARCHAR NOT NULL COLLATE NOCASE, " +
                        "last_usage INTEGER DEFAULT 0," +
                        "username VARCHAR DEFAULT NULL, " +
                        "password VARCHAR DEFAULT NULL, " +
						"proxy_addr VARCHAR DEFAULT NULL, " +
						"proxy_port VARCHAR DEFAULT NULL, " +
						"proxy_uname VARCHAR DEFAULT NULL, " +
						"proxy_passw VARCHAR DEFAULT NULL, " +
						"onion_def_proxy INTEGER DEFAULT 1, " +
						"books_downloaded INTEGER DEFAULT 0, " +
						"was_error INTEGER DEFAULT 0" +
                        ")");
			if (currentVersion < 7) {
				addOPDSCatalogs(DEF_OPDS_URLS1);
			}
			if (currentVersion < 13)
			    execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN language VARCHAR DEFAULT NULL");
			if (currentVersion < 14)
				pathCorrectionRequired = true;
			if (currentVersion < 15)
			    execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN last_usage INTEGER DEFAULT 0");
			if (currentVersion < 16)
				execSQLIgnoreErrors("ALTER TABLE bookmark ADD COLUMN time_elapsed INTEGER DEFAULT 0");
			if (currentVersion < 17)
				pathCorrectionRequired = true; // chance to correct paths under Android 4.2
			if (currentVersion < 20)
				removeOPDSCatalogsFromBlackList(); // BLACK LIST enforcement, by LitRes request
            if (currentVersion < 21)
                execSQL("CREATE TABLE IF NOT EXISTS favorite_folders (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "path VARCHAR NOT NULL, " +
                        "position INTEGER NOT NULL default 0, " +
						"filename VARCHAR " +
                        ")");
			if (currentVersion < 23) {
			    execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN username VARCHAR DEFAULT NULL");
			    execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN password VARCHAR DEFAULT NULL");
			}
			if (currentVersion < 24) {
				execSQL("CREATE TABLE IF NOT EXISTS search_history (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"book_fk INTEGER NOT NULL REFERENCES book (id), " +
						"search_text VARCHAR " +
						")");
				execSQL("CREATE INDEX IF NOT EXISTS " +
						"search_history_index ON search_history (book_fk) ");
				execSQL("CREATE TABLE IF NOT EXISTS user_dic (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"dic_word VARCHAR, " +
						"dic_word_translate VARCHAR, " +
						"dic_from_book VARCHAR, " +
						"create_time INTEGER, " +
						"last_access_time INTEGER, " +
						"language VARCHAR DEFAULT NULL, " +
						"seen_count INTEGER, " +
						"is_citation INTEGER DEFAULT 0 " +
						")");
				execSQL("CREATE INDEX IF NOT EXISTS " +
						"user_dic_index ON user_dic (dic_word) ");
				execSQLIgnoreErrors("ALTER TABLE bookmark ADD COLUMN link_pos VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN lang_from VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN lang_to VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE user_dic ADD COLUMN is_citation INTEGER DEFAULT 0");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN saved_with_ver INTEGER DEFAULT 0");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN genre VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN annotation VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN srclang VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN translator VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN docauthor VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN docprogram VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN docdate VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN docsrcurl VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN docsrcocr VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN docversion VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN publname VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN publisher VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN publcity VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN publyear VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN publisbn VARCHAR DEFAULT NULL");
                        //addOPDSCatalogs(DEF_OPDS_URLS3);

				execSQL("CREATE TABLE IF NOT EXISTS dic_search_history (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"search_text VARCHAR, " +
						"text_translate VARCHAR, " +
						"search_from_book VARCHAR, " +
						"dictionary_used VARCHAR, " +
						"create_time INTEGER, " +
						"last_access_time INTEGER, " +
						"language_from VARCHAR DEFAULT NULL, " +
						"language_to VARCHAR DEFAULT NULL, " +
						"seen_count INTEGER "+
						")");
				execSQL("CREATE INDEX IF NOT EXISTS " +
						"dic_search_history_index ON dic_search_history (search_text) ");
				execSQL("CREATE INDEX IF NOT EXISTS " +
						"dic_search_history_index_date ON dic_search_history (last_access_time DESC) ");

			}

			if (currentVersion < 29) {
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN bookdate VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN publseries_fk INTEGER REFERENCES series (id)");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN publseries_number INTEGER");
			}

			if (currentVersion < 31) {
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN file_create_time INTEGER");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN sym_count INTEGER");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN word_count INTEGER");
                execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN book_date_n INTEGER");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN doc_date_n INTEGER");
                execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN publ_year_n INTEGER");

				String sql = READ_FILEINFO_SQL + " WHERE file_create_time is null";
				try (Cursor rs = mDB.rawQuery(sql, null)) {
					if (rs.moveToFirst()) {
						do {
							FileInfo fileInfo = new FileInfo();
							readFileInfoFromCursor(fileInfo, rs);
							if (!fileInfo.fileExists())
								continue;
							long lm = fileInfo.fileLastModified();
							execSQL("UPDATE book SET file_create_time="+ lm +" WHERE id=" + fileInfo.id);
						} while (rs.moveToNext());
					}
				}
			}
			if (currentVersion < 35) {
				execSQLIgnoreErrors("ALTER TABLE favorite_folders ADD COLUMN filename VARCHAR ");
			}
			if (currentVersion < 36)
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN opds_link VARCHAR DEFAULT NULL");
			if (currentVersion < 37) {
				execSQL("CREATE INDEX IF NOT EXISTS " +
						"book_opds_link_index ON book (opds_link) ");
				execSQL("CREATE INDEX IF NOT EXISTS " +
						"book_opds_link_index ON book (opds_link) ");
			}
			if (currentVersion < 38) {
				execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN proxy_addr VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN proxy_port VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN proxy_uname VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN proxy_passw VARCHAR DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN onion_def_proxy INTEGER DEFAULT 1");
			}

			if (currentVersion < 39) {
				addOPDSCatalogs(DEF_OPDS_URLS3);
			}

			if (currentVersion < 41) {
				removeOPDSCatalogsByURLs(OBSOLETE_OPDS_URLS);
				addOPDSCatalogs(DEF_OPDS_URLS3);
				execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN books_downloaded INTEGER DEFAULT 0");
				execSQLIgnoreErrors("ALTER TABLE opds_catalog ADD COLUMN was_error INTEGER DEFAULT 0");
			}
			if (currentVersion < 43) {
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN crc32 INTEGER DEFAULT NULL");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN domVersion INTEGER DEFAULT 0");
				execSQLIgnoreErrors("ALTER TABLE book ADD COLUMN rendFlags INTEGER DEFAULT 0");
				// After adding support for the 'fb3' and 'docx' formats in version 3.2.33,
				// the 'format' field in the 'book' table becomes invalid because the enum DocumentFormat has been changed.
				// So, after reading this field from the database, we must recheck the format by pathname.
				// TODO: check format by mime-type or file contents...
				log.i("Update 'format' field in table 'book'...");
				String sql = "SELECT id, pathname, format FROM book";
				HashMap<Long, Long> formatsMap = new HashMap<>();
				try (Cursor rs = mDB.rawQuery(sql, null)) {
					if (rs.moveToFirst()) {
						do {
							Long id = rs.getLong(0);
							String pathname = rs.getString(1);
							Long old_format = rs.getLong(2);
							if (old_format > 1) {		// skip 'none', 'fb2' - ordinal is not changed
								DocumentFormat new_format = DocumentFormat.byExtension(pathname);
								if (null != new_format && old_format != new_format.ordinal())
									formatsMap.put(id, (long) new_format.ordinal());
							}
						} while (rs.moveToNext());
					}
				} catch (Exception e) {
					Log.e("cr3", "exception while reading format", e);
				}
				// Save new format in table 'book'...
				if (!formatsMap.isEmpty()) {
					int updatedCount = 0;
					mDB.beginTransaction();
					try (SQLiteStatement stmt = mDB.compileStatement("UPDATE book SET format = ? WHERE id = ?")) {
						for (Map.Entry<Long, Long> record : formatsMap.entrySet() ) {
							stmt.clearBindings();
							stmt.bindLong(1, record.getValue());
							stmt.bindLong(2, record.getKey());
							stmt.execute();
							updatedCount++;
						}
						mDB.setTransactionSuccessful();
						vlog.i("Updated " + updatedCount + " records with invalid format.");
					} catch (Exception e) {
						Log.e("cr3", "exception while reading format", e);
					} finally {
						mDB.endTransaction();
					}
				}
			}
			if (currentVersion < 30) {
				// Forced update DOM version from previous latest (20200223) to current (20200824).
				execSQLIgnoreErrors("UPDATE book SET domVersion=20200824 WHERE domVersion=20200223");
			}

			if (currentVersion < 44) {
				execSQLIgnoreErrors("ALTER TABLE author ADD COLUMN book_cnt INTEGER");
				execSQLIgnoreErrors("ALTER TABLE genre ADD COLUMN book_cnt INTEGER");
				execSQLIgnoreErrors("ALTER TABLE series ADD COLUMN book_cnt INTEGER");
				execSQLIgnoreErrors("ALTER TABLE folder ADD COLUMN book_cnt INTEGER");
			}

			if (currentVersion < 45) {
				execSQLIgnoreErrors("update book set title = filename where title IS NULL or title = ''");
			}
			if (currentVersion < 47) {
				execSQLIgnoreErrors("delete from book_dates_stats");
				execSQLIgnoreErrors(
						"insert into book_dates_stats(date_field, book_date, book_cnt) " +
						"select 'book_date_n', case when coalesce(b.book_date_n,0)=0 then 0 else " +
						"cast(strftime('%s',datetime(b.book_date_n/1000, 'unixepoch', 'start of month')) as integer) end, " +
						"count(*) as book_cnt from book b " +
						"group by 2 " +
						"union all " +
						"select 'doc_date_n', case when coalesce(b.doc_date_n,0)=0 then 0 else  " +
						"cast(strftime('%s',datetime(b.doc_date_n/1000, 'unixepoch', 'start of month')) as integer) end, " +
						"count(*) as book_cnt from book b " +
						"group by 2 " +
						"union all " +
						"select 'publ_year_n', case when coalesce(b.publ_year_n,0)=0 then 0 else  " +
						"cast(strftime('%s',datetime(b.publ_year_n/1000, 'unixepoch', 'start of month')) as integer) end, " +
						"count(*) as book_cnt from book b " +
						"group by 2 " +
						"union all " +
						"select 'file_create_time', case when coalesce(b.file_create_time,0)=0 then 0 else  " +
						"cast(strftime('%s',datetime(b.file_create_time/1000, 'unixepoch', 'start of month')) as integer) end, " +
						"count(*) as book_cnt from book b " +
						"group by 2");
				execSQLIgnoreErrors("delete from book_titles_stats");
				execSQLIgnoreErrors(
						"insert into book_titles_stats(text_field, stat_level, text_value, book_cnt) "+
								"select 'book_title' text_field, 0 stat_level, substr(b.title,1,1) text_value, count(*) as book_cnt from book b "+
								"where (not (b.pathname like '@opds%')) and length(b.title)>=1 "+
								"GROUP BY substr(b.title,1,1) "+
								"union all "+
								"select 'book_title' text_field, 1 stat_level, substr(b.title,1,2) text_value, count(*) as book_cnt from book b "+
								"where (not (b.pathname like '@opds%')) and length(b.title)>=2 "+
								"GROUP BY substr(b.title,1,2) "+
								"union all "+
								"select 'book_title' text_field, 2 stat_level, substr(b.title,1,3) text_value, count(*) as book_cnt from book b "+
								"where (not (b.pathname like '@opds%')) and length(b.title)>=3 "+
								"GROUP BY substr(b.title,1,3) ");
			}
			if (currentVersion < 49) {
				execSQL("CREATE TABLE IF NOT EXISTS author_aliases (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT," +
						"orig_text VARCHAR NOT NULL COLLATE NOCASE, " +
						"alias_text VARCHAR NOT NULL COLLATE NOCASE " +
						")");
				execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " +
						"author_aliases_orig_text_index ON author_aliases (orig_text, alias_text) ");
				execSQL("CREATE INDEX IF NOT EXISTS " +
						"author_aliases_alias_text_index ON author_aliases (alias_text) ");
				// Это решил пока не делать
//				execSQL("CREATE TABLE IF NOT EXISTS settings (" +
//						"id INTEGER PRIMARY KEY AUTOINCREMENT," +
//						"author_aliases_size INTEGER, " +
//						"author_aliases_crc INTEGER " +
//						")");
				// - нужна защита от повторной вставки
				//execSQL("INSERT INTO settings (author_aliases_size, author_aliases_crc) values (0,0)");
			}

			//==============================================================
			// add more updates above this line
				
			// set current version
			mDB.setVersion(DB_VERSION);
		}

		dumpStatistics();
		
		return true;
	}

	private void dumpStatistics() {
		log.i("mainDB: " + longQuery("SELECT count(*) FROM author") + " authors, "
				 + longQuery("SELECT count(*) FROM genre") + " genres, "
				 + longQuery("SELECT count(*) FROM genre_transl") + " genres_transl, "
				 + longQuery("SELECT count(*) FROM series") + " series, "
				 + longQuery("SELECT count(*) FROM book") + " books, "
				 + longQuery("SELECT count(*) FROM bookmark") + " bookmarks, "
				 + longQuery("SELECT count(*) FROM search_history") + " search_historys, "
				 + longQuery("SELECT count(*) FROM user_dic") + " user_dics, "
				 + longQuery("SELECT count(*) FROM dic_search_history") + " dic_search_historys, "
				 + longQuery("SELECT count(*) FROM folder") + " folders, "
				 + longQuery("SELECT count(*) FROM author_aliases") + " author_aliases "
		);
	}

	@Override
	protected String dbFileName() {
		return "cr3db.sqlite";
	}
	
	public void clearCaches() {
		seriesCache.clear();
		authorCache.clear();
		folderCache.clear();
		fileInfoCache.clear();
		genreCache.clear();
	}

	public void flush() {
        super.flush();
        if (seriesStmt != null) {
            seriesStmt.close();
            seriesStmt = null;
        }
        if (folderStmt != null) {
        	folderStmt.close();
        	folderStmt = null;
        }
        if (authorStmt != null) {
            authorStmt.close();
            authorStmt = null;
        }
        if (seriesSelectStmt != null) {
            seriesSelectStmt.close();
            seriesSelectStmt = null;
        }
        if (folderSelectStmt != null) {
        	folderSelectStmt.close();
        	folderSelectStmt = null;
        }
        if (authorSelectStmt != null) {
            authorSelectStmt.close();
            authorSelectStmt = null;
        }
		if (genreSelectStmt != null) {
			genreSelectStmt.close();
			genreSelectStmt = null;
		}
	}
	
	//=======================================================================================
    // OPDS access code
    //=======================================================================================
	private final static String[] DEF_OPDS_URLS1 = {
		// feedbooks.com tested 2020.01
		// offers preview or requires registration
		//"http://www.feedbooks.com/catalog.atom", "Feedbooks",
		// tested 2020.01 - error 500
		"http://bookserver.archive.org/catalog/", "Internet Archive",
		// obsolete link
		//"http://m.gutenberg.org/", "Project Gutenberg",
		//		"http://ebooksearch.webfactional.com/catalog.atom", "eBookSearch",
		//"http://bookserver.revues.org/", "Revues.org",
		//"http://www.legimi.com/opds/root.atom", "Legimi",
		//https://www.ebooksgratuits.com/opds/index.php
		// tested 2020.01
		"http://www.ebooksgratuits.com/opds/", "Ebooks libres et gratuits (fr)",
	};

	private final static String[] OBSOLETE_OPDS_URLS = {
			"http://m.gutenberg.org/", // "Project Gutenberg" old URL
			"http://www.shucang.org/s/index.php", //"ShuCang.org"
			"http://www.legimi.com/opds/root.atom", //"Legimi",
			"http://bookserver.revues.org/", //"Revues.org",
			"http://ebooksearch.webfactional.com/catalog.atom", //
	};

	private final static String[] DEF_OPDS_URLS3 = {
		// o'reilly
		"http://opds.oreilly.com/opds/", "O'Reilly",
		"http://m.gutenberg.org/ebooks.opds/", "Project Gutenberg",
		//"https://api.gitbook.com/opds/catalog.atom", "GitBook",
		"http://srv.manybooks.net/opds/index.php", "ManyBooks",
		//"http://opds.openedition.org/", "OpenEdition (fr)",
		"https://gallica.bnf.fr/opds", "Gallica (fr)",
		"https://www.textos.info/catalogo.atom", "textos.info (es)",
		"https://wolnelektury.pl/opds/", "Wolne Lektury (pl)",
		"http://www.bokselskap.no/wp-content/themes/bokselskap/tekster/opds/root.xml", "Bokselskap (no)",
		"http://asmodei.org/opds", "asmodei.org",
		"http://fb.litlib.net/", "litlib",
	};

	private void addOPDSCatalogs(String[] catalogs) {
		for (int i=0; i<catalogs.length-1; i+=2) {
			String url = catalogs[i];
			String name = catalogs[i+1];
			saveOPDSCatalog(null, url, name, null, null,
					null,null,null,null,1);
		}
	}

	public void removeOPDSCatalogsByURLs(String ... urls) {
		for (String url : urls) {
			execSQLIgnoreErrors("DELETE FROM opds_catalog WHERE url=" + quoteSqlString(url));
		}
	}

	public void removeOPDSCatalogsFromBlackList() {
		if (OPDSConst.BLACK_LIST_MODE != OPDSConst.BLACK_LIST_MODE_FORCE) {
			removeOPDSCatalogsByURLs("http://flibusta.net/opds/");
		} else {
			removeOPDSCatalogsByURLs(OPDSConst.BLACK_LIST);
		}
	}

	public void updateOPDSCatalog(String url, String field, String value) {
		try {
			Long existingIdByUrl = longQuery("SELECT id FROM opds_catalog WHERE url=" + quoteSqlString(url));
			if (existingIdByUrl == null)
				return;
			// update existing
			if (value.toLowerCase().equals("max")) {
				Long fValue = longQuery("SELECT max("+field+") FROM opds_catalog");
				if (fValue == null)
					fValue = 1L;
				else
					fValue = fValue + 1;
				execSQL("UPDATE opds_catalog SET "+field+"=" + fValue + " WHERE id=" + existingIdByUrl);
			} else {
				execSQL("UPDATE opds_catalog SET "+field+"=" + value + " WHERE id=" + existingIdByUrl);
			}
		} catch (Exception e) {
			log.e("exception while updating OPDS catalog item", e);
		}
	}

	
	public boolean saveOPDSCatalog(Long id, String url, String name, String username, String password,
		String proxy_addr, String proxy_port, String proxy_uname, String proxy_passw, int onion_def_proxy) {
		if (!isOpened())
			return false;
		if (url==null || name==null)
			return false;
		url = url.trim();
		name = name.trim();
		if (url.length()==0 || name.length()==0)
			return false;
		try {
			Long existingIdByUrl = longQuery("SELECT id FROM opds_catalog WHERE url=" + quoteSqlString(url));
			Long existingIdByName = longQuery("SELECT id FROM opds_catalog WHERE name=" + quoteSqlString(name));
			if (existingIdByUrl!=null && existingIdByName!=null && !existingIdByName.equals(existingIdByUrl))
				return false; // duplicates detected
			if (id==null) {
				id = existingIdByUrl;
				if (id==null)
					id = existingIdByName;
			}
			if (id==null) {
				// insert new
				log.i("Saving "+name+" OPDS catalog");
				execSQL("INSERT INTO opds_catalog (name, url, username, password, "+
						"proxy_addr, proxy_port, proxy_uname, proxy_passw, onion_def_proxy) VALUES ("+
						quoteSqlString(name)+", "+quoteSqlString(url)+", "+
						quoteSqlString(username)+", "+quoteSqlString(password)+", "+
						quoteSqlString(proxy_addr)+", "+quoteSqlString(proxy_port)+", "+
						quoteSqlString(proxy_uname)+", "+quoteSqlString(proxy_passw)+", "+
						String.valueOf(onion_def_proxy)+
						")");
			} else {
				// update existing
				execSQL("UPDATE opds_catalog SET name="+quoteSqlString(name)+", url="+quoteSqlString(url)+
						", username="+quoteSqlString(username)+", password="+quoteSqlString(password)+
						", proxy_addr="+quoteSqlString(proxy_addr)+", proxy_port="+quoteSqlString(proxy_port)+
						", proxy_uname="+quoteSqlString(proxy_uname)+", proxy_passw="+quoteSqlString(proxy_passw)+
						", onion_def_proxy="+String.valueOf(onion_def_proxy)+
						" WHERE id=" + id);
			}
			updateOPDSCatalog(url, "last_usage", "max");
				
		} catch (Exception e) {
			log.e("exception while saving OPDS catalog item", e);
			return false;
		}
		return true;
	}

	public boolean saveSearchHistory(BookInfo book, String sHist) {
		if (!isOpened())
			return false;
		if (sHist==null)
			return false;
		if (book.getFileInfo().id == null)
			return false; // unknown book id
		sHist = sHist.trim();
		if (sHist.length()==0)
			return false;
		try {
			execSQL("DELETE FROM search_history where book_fk = " + book.getFileInfo().id+
				" and search_text = "+quoteSqlString(sHist));
			execSQL("INSERT INTO search_history (book_fk, search_text) values (" + book.getFileInfo().id+
					", "+quoteSqlString(sHist)+")");
		} catch (Exception e) {
			log.e("exception while saving search history item", e);
			return false;
		}
		return true;
	}

	public boolean saveUserDic(UserDicEntry ude, int action) {
        Log.i("cr3", "saving user dic");
		if (ude==null)
			return false;
        String sWord = ude.getDic_word();
		String sWordTranslate = ude.getDic_word_translate();
		int is_cit = ude.getIs_citation();
		if (!isOpened())
			return false;
		if (sWord==null)
			return false;
		if ((sWordTranslate==null)&&(action == UserDicEntry.ACTION_NEW))
			return false;
		if (ude.getIs_citation()==0)
			sWord = sWord.trim().toLowerCase();
		if (sWord.length()==0)
			return false;
		sWordTranslate = sWordTranslate.trim();
        if ((sWordTranslate.length()==0)&&(action == UserDicEntry.ACTION_NEW))
			return false;
		Cursor rs = null;
		String sW = "";

        try {
			if (action == UserDicEntry.ACTION_NEW) {
				String sql = "SELECT id, dic_word_translate FROM user_dic where dic_word=" + quoteSqlString(sWord) +
						" and coalesce(is_citation,0) = "+is_cit;
				rs = mDB.rawQuery(sql, null);
				if (rs.moveToFirst()) {
					sW = rs.getString(1);
					// need update
					if (!sW.equals(sWordTranslate)) {
						execSQL("UPDATE user_dic SET " +
								" dic_word_translate = " + quoteSqlString(sWordTranslate) + ", " +
								" dic_from_book = " + quoteSqlString(String.valueOf(ude.getDic_from_book())) + ", " +
								" last_access_time = " + System.currentTimeMillis() + ", " +
								" language = " + quoteSqlString(ude.getLanguage()) + ", " +
								" is_citation = " + is_cit +
								" WHERE id = " + rs.getInt(0)
						);
					}
				} else {
					// need insert
					execSQL("INSERT INTO user_dic " +
							"(dic_word, dic_word_translate, dic_from_book, create_time, last_access_time, language, seen_count, is_citation) " +
							"values (" + quoteSqlString(sWord) + ", " +
							quoteSqlString(sWordTranslate) + ", " +
							quoteSqlString(String.valueOf(ude.getDic_from_book())) + ", " +
							System.currentTimeMillis() + ", " +
							System.currentTimeMillis() + ", " +
							quoteSqlString(ude.getLanguage()) + ", " +
							"0, "+
							is_cit +
							")"
					);
				}
			}
			if (action == UserDicEntry.ACTION_DELETE) {
//				Log.i("sql","DELETE FROM user_dic " +
//						" where dic_word = " + quoteSqlString(sWord) + " and coalesce(is_citation,0) = "+is_cit);
				execSQL("DELETE FROM user_dic " +
						" where dic_word = " + quoteSqlString(sWord) + " and coalesce(is_citation,0) = "+is_cit);
			}
			if (action == UserDicEntry.ACTION_UPDATE_CNT) {
				execSQL("UPDATE user_dic SET " +
						" last_access_time = " + System.currentTimeMillis() + ", " +
						" seen_count = seen_count + 1 " +
						" WHERE dic_word = " + quoteSqlString(sWord) + " and coalesce(is_citation,0) = "+is_cit);
			}
		} catch (Exception e) {
			Log.e("cr3", "exception while saving user dic", e);
			return false;
		} finally {
			if (rs != null)
				rs.close();
		}
		return true;
	}

	public boolean updateDicSearchHistory(DicSearchHistoryEntry dshe, int action) {
		Log.i("cr3", "saving dic search history");
		Cursor rs = null;
		try {
			if (action==DicSearchHistoryEntry.ACTION_CLEAR_ALL) {
				if (!isOpened())
					return false;
				execSQL("DELETE FROM dic_search_history");
				return true;
			} if (action==DicSearchHistoryEntry.ACTION_DELETE) {
				if (!isOpened())
					return false;
				if (dshe == null) return false;
				String sSearchText = dshe.getSearch_text();
				if (!isOpened())
					return false;
				if (sSearchText == null)
					return false;
				sSearchText = sSearchText.trim();
				if (sSearchText.length()==0)
					return false;
				String sql = "DELETE FROM dic_search_history where search_text=" + quoteSqlString(sSearchText);
				execSQL(sql);
			} else {
				if (dshe == null) return false;
				String sSearchText = dshe.getSearch_text();
				if (!isOpened())
					return false;
				if (sSearchText == null)
					return false;
				sSearchText = sSearchText.trim();
				if (sSearchText.length()==0)
					return false;
				String sql = "SELECT id FROM dic_search_history where search_text=" + quoteSqlString(sSearchText);
				rs = mDB.rawQuery(sql, null);
				if (rs.moveToFirst()) {
					// need update
					if (StrUtils.isEmptyStr(dshe.getText_translate()))
						execSQL("UPDATE dic_search_history SET " +
										" search_from_book = " + quoteSqlString(String.valueOf(dshe.getSearch_from_book())) + ", " +
										" dictionary_used = " + quoteSqlString(dshe.getDictionary_used()) + ", " +
										" last_access_time = " + System.currentTimeMillis() + ", " +
										" language_from = " + quoteSqlString(dshe.getLanguage_from()) + ", " +
										" language_to = " + quoteSqlString(dshe.getLanguage_to()) + ", " +
										" seen_count = coalesce(seen_count,0) + 1 " +
										" WHERE id = " + rs.getInt(0)
						);
					else
						execSQL("UPDATE dic_search_history SET " +
							" text_translate = " + quoteSqlString(dshe.getText_translate()) + ", " +
							" search_from_book = " + quoteSqlString(String.valueOf(dshe.getSearch_from_book())) + ", " +
							" dictionary_used = " + quoteSqlString(dshe.getDictionary_used()) + ", " +
							" last_access_time = " + System.currentTimeMillis() + ", " +
							" language_from = " + quoteSqlString(dshe.getLanguage_from()) + ", " +
							" language_to = " + quoteSqlString(dshe.getLanguage_to()) + ", " +
							" seen_count = coalesce(seen_count,0) + 1 " +
							" WHERE id = " + rs.getInt(0)
					);
				} else {
					// need insert
					execSQL("INSERT INTO dic_search_history " +
									"(search_text, text_translate, search_from_book, dictionary_used, " +
									"create_time, last_access_time, language_from, language_to, seen_count) " +
									"values (" +
									quoteSqlString(sSearchText) + ", " +
									quoteSqlString(dshe.getText_translate()) + ", " +
									quoteSqlString(dshe.getSearch_from_book()) + ", " +
									quoteSqlString(dshe.getDictionary_used()) + ", " +
									System.currentTimeMillis() + ", " +
									System.currentTimeMillis() + ", " +
									quoteSqlString(dshe.getLanguage_from()) + ", " +
									quoteSqlString(dshe.getLanguage_to()) + ", " +
									"1 "+
									")"
					);
				}
			}
		} catch (Exception e) {
			Log.e("cr3", "exception while saving dic search history", e);
			return false;
		} finally {
			if (rs != null)
				rs.close();
		}
		return true;
	}

	public boolean clearSearchHistory(BookInfo book) {
		if (!isOpened())
			return false;
		if (book.getFileInfo().id == null)
			return false; // unknown book id
		try {
			execSQL("DELETE FROM search_history where book_fk = " + book.getFileInfo().id);
		} catch (Exception e) {
			log.e("exception while clearing search history", e);
			return false;
		}
		return true;
	}

	public ArrayList<String> loadSearchHistory(BookInfo book) {
		log.i("loadSearchHistory()");
		String sql = "SELECT search_text FROM search_history where book_fk=" + book.getFileInfo().id + " ORDER BY id desc";
		ArrayList<String> list = new ArrayList<>();
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				do {
					String sHist = rs.getString(0);
					list.add(sHist);
				} while (rs.moveToNext());
			}
		} catch (Exception e) {
			Log.e("cr3", "exception while loading search history", e);
		}
		return list;
	}

	public HashMap<String, UserDicEntry> loadUserDic() {
		log.i("loadUserDic()");
		HashMap<String, UserDicEntry> hshDic = new HashMap<String, UserDicEntry>();
		if (mDB == null) return hshDic;
		String sql = "SELECT id, dic_word, dic_word_translate, dic_from_book, "+
				" create_time, last_access_time, language, seen_count, coalesce(is_citation,0) as is_cit "+
				" FROM user_dic";
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				do {
					UserDicEntry ude = new UserDicEntry();
					ude.setId(rs.getLong(0));
					ude.setDic_word(rs.getString(1));
					ude.setDic_word_translate(rs.getString(2));
					ude.setDic_from_book(rs.getString(3));
					ude.setCreate_time(rs.getLong(4));
					ude.setLast_access_time(rs.getLong(5));
					ude.setLanguage(rs.getString(6));
					ude.setSeen_count(rs.getLong(7));
					ude.setIs_citation(rs.getInt(8));
					hshDic.put(ude.getIs_citation()+ude.getDic_word(),ude);
				} while (rs.moveToNext());
			}
		} catch (Exception e) {
			Log.e("cr3", "exception while loading user_dic", e);
		}
		return hshDic;
	}

	public List<DicSearchHistoryEntry> loadDicSearchHistory() {
		log.i("loadDicSearchHistory()");
		ArrayList<DicSearchHistoryEntry> arrlDshe = new ArrayList<DicSearchHistoryEntry>();
		String sql =
				"SELECT h.id, h.search_text, h.text_translate, h.search_from_book, h.dictionary_used, " +
						"  h.create_time, h.last_access_time, h.language_from, h.language_to, h.seen_count " +
						"  FROM dic_search_history h where COALESCE(text_translate,'') != '' " +
						" union all " +
						"SELECT h.id, h.search_text, ud.dic_word_translate, ud.dic_from_book, h.dictionary_used, " +
						"  h.create_time, h.last_access_time, h.language_from, h.language_to, h.seen_count " +
						"  FROM dic_search_history h" +
						"  left join user_dic ud on trim(ud.dic_word) = trim(h.search_text) " +
						"  where COALESCE(text_translate,'') = '' " +
						"order by last_access_time DESC "+
						"  LIMIT 5000 ";
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				do {
					DicSearchHistoryEntry dshe = new DicSearchHistoryEntry();
					dshe.setId(rs.getLong(0));
					dshe.setSearch_text(rs.getString(1));
					dshe.setText_translate(rs.getString(2));
					dshe.setSearch_from_book(rs.getString(3));
					dshe.setDictionary_used(rs.getString(4));
					dshe.setCreate_time(rs.getLong(5));
					dshe.setLast_access_time(rs.getLong(6));
					dshe.setLanguage_from(rs.getString(7));
					dshe.setLanguage_to(rs.getString(8));
					dshe.setSeen_count(rs.getLong(9));
					arrlDshe.add(dshe);
				} while (rs.moveToNext());
			}
		} catch (Exception e) {
			Log.e("cr3", "exception while loading dic_search_history", e);
		}
		return arrlDshe;
	}

	public boolean loadOPDSCatalogs(ArrayList<FileInfo> list) {
		log.i("loadOPDSCatalogs()");
		boolean found = false;
		String sql = "SELECT id, name, url, username, password, "+
				" proxy_addr, proxy_port, proxy_uname, proxy_passw, "+
				" onion_def_proxy, books_downloaded, was_error "+
				" FROM opds_catalog ORDER BY coalesce(was_error,0), last_usage DESC, name";
		if (mDB == null) return false;
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				// remove existing entries
				list.clear();
				// read DB
				do {
					Long id = rs.getLong(0);
					String name = rs.getString(1);
					String url = rs.getString(2);
					String username = rs.getString(3);
					String password = rs.getString(4);
					String proxy_addr = rs.getString(5);
					String proxy_port = rs.getString(6);
					String proxy_uname = rs.getString(7);
					String proxy_passw = rs.getString(8);
					int onion_def_proxy = rs.getInt(9);
					Long book_downloaded = rs.getLong(10);
					int was_error = rs.getInt(11);
					FileInfo opds = new FileInfo();
					opds.isDirectory = true;
					opds.pathname = FileInfo.OPDS_DIR_PREFIX + url;
					opds.setFilename(name);
					opds.username = username;
					opds.password = password;
					opds.proxy_addr = proxy_addr;
					opds.proxy_port = proxy_port;
					opds.proxy_uname = proxy_uname;
					opds.proxy_passw = proxy_passw;
					opds.onion_def_proxy = onion_def_proxy;
					opds.isListed = true;
					opds.isScanned = true;
					opds.id = id;
					opds.book_downloaded = book_downloaded;
					opds.was_error = was_error;
					list.add(opds);
					found = true;
				} while (rs.moveToNext());
			}
		} catch (Exception e) {
			Log.e("cr3", "exception while loading list of OPDS catalogs", e);
		}
		return found;
	}
	
	public void removeOPDSCatalog(Long id) {
		log.i("removeOPDSCatalog(" + id + ")");
		execSQLIgnoreErrors("DELETE FROM opds_catalog WHERE id = " + id);
	}

    public ArrayList<FileInfo> loadFavoriteFolders() {
        log.i("loadFavoriteFolders()");
        ArrayList<FileInfo> list = new ArrayList<>();
		String sql = "SELECT id, path, position, filename FROM favorite_folders ORDER BY position, path";
        if (mDB == null) return list;
        try (Cursor rs = mDB.rawQuery(sql, null)) {
            if (rs.moveToFirst()) {
                do {
                    Long id = rs.getLong(0);
                    String path = rs.getString(1);
                    int pos = rs.getInt(2);
					FileInfo favorite = new FileInfo(path);
                    favorite.id = id;
                    favorite.seriesNumber = pos;
                    favorite.setType(FileInfo.TYPE_NOT_SET);
                    if (!StrUtils.isEmptyStr(rs.getString(3)))
                    	favorite.setFilename(rs.getString(3));
                    list.add(favorite);
                } while (rs.moveToNext());
            }
        } catch (Exception e) {
            Log.e("cr3", "exception while loading list of favorite folders", e);
        }
        return list;
    }

    public void deleteFavoriteFolder(FileInfo folder){
        execSQLIgnoreErrors("DELETE FROM favorite_folders WHERE id = "+ folder.id);
    }

    public void updateFavoriteFolder(FileInfo folder){
        try (SQLiteStatement stmt = mDB.compileStatement("UPDATE favorite_folders SET position = ?, path = ?, filename = ? WHERE id = ?")) {
            stmt.bindLong(1, folder.seriesNumber);
            stmt.bindString(2, folder.pathname);
			stmt.bindString(3, folder.getFilename());
            stmt.bindLong(4, folder.id);
            stmt.execute();
        }
    }

    public void createFavoritesFolder(FileInfo folder){
        try (SQLiteStatement stmt = mDB.compileStatement("INSERT INTO favorite_folders (id, path, position, filename) VALUES (NULL, ?, ?, ?)")) {
			stmt.bindString(1, folder.pathname);
			stmt.bindLong(2, folder.seriesNumber);
			String fname = folder.getFilename();
			if ((folder.isOPDSDir()) && (folder.parent != null)) {
				if (folder.parent.parent != null) {
					fname = fname + " | " + folder.parent.getFilename();
					if (folder.parent.parent.parent != null) {
						fname = fname + " | " + folder.parent.parent.getFilename();
					}
				}
			}
			stmt.bindString(3, fname);
            folder.id = stmt.executeInsert();
        }
    }

	//=======================================================================================
    // Bookmarks access code
    //=======================================================================================
	private static final String READ_BOOKMARK_SQL = 
		"SELECT " +
		"id, type, percent, shortcut, time_stamp, " + 
		"start_pos, end_pos, title_text, pos_text, comment_text, time_elapsed, link_pos " +
		"FROM bookmark b ";
	private void readBookmarkFromCursor(Bookmark v, Cursor rs)
	{
		int i=0;
		v.setId(rs.getLong(i++));
		v.setType((int)rs.getLong(i++));
		v.setPercent((int)rs.getLong(i++));
		v.setShortcut((int)rs.getLong(i++));
		v.setTimeStamp(rs.getLong(i++));
		v.setStartPos(rs.getString(i++));
		v.setEndPos(rs.getString(i++));
		v.setTitleText(rs.getString(i++));
		v.setPosText(rs.getString(i++));
		v.setCommentText(rs.getString(i++));
		v.setTimeElapsed(rs.getLong(i++));
		v.setLinkPos(rs.getString(i++));
	}

	public boolean findBy(Bookmark v, String condition) {
		boolean found = false;
		try (Cursor rs = mDB.rawQuery(READ_BOOKMARK_SQL + " WHERE " + condition, null)) {
			if (rs.moveToFirst()) {
				readBookmarkFromCursor(v, rs);
				found = true;
			}
		}
		return found;
	}

	public boolean load(ArrayList<Bookmark> list, String condition)
	{
		boolean found = false;
		try (Cursor rs = mDB.rawQuery(READ_BOOKMARK_SQL + " WHERE " + condition, null)) {
			if (rs.moveToFirst()) {
				do {
					Bookmark v = new Bookmark();
					readBookmarkFromCursor(v, rs);
					list.add(v);
					found = true;
				} while (rs.moveToNext());
			}
		}
		return found;
	}

	public void loadBookmarks(BookInfo book) {
		if (book.getFileInfo().id == null)
			return; // unknown book id
		ArrayList<Bookmark> bookmarks = new ArrayList<>();
		if (load(bookmarks, "book_fk=" + book.getFileInfo().id + " ORDER BY type" )) {
			book.setBookmarks(bookmarks);
		}
	}
	
	//=======================================================================================
    // Item groups access code
    //=======================================================================================
	
	/// add items range to parent dir
	private static void addItems(FileInfo parent, ArrayList<FileInfo> items, int start, int end) {
		for (int i=start; i<end; i++) {
			items.get(i).parent = parent;
			if (items.get(i).isSpecialDir()) {
				parent.addDir(items.get(i));
			} else
				parent.addFile(items.get(i));
		}
	}
	
	public static abstract class ItemGroupExtractor {
		public abstract String getComparisionField(FileInfo item);
		public String getItemFirstLetters(FileInfo item, int level) {
			try {
				String name = getComparisionField(item); //.filename;
				//vlog.i("getItemFirstLetters: "+name);
				int l = name == null ? 0 : Math.min(name.length(), level);
				if (l > 0) {
					String sRet = "error";
					if (name!=null) {
						if (name.length()>=l) sRet = name.substring(0, l).toUpperCase();
							else sRet = name;
					} else sRet="[empty]";
					if (sRet.equals("")) sRet="[empty]";
					if (sRet.length()>level) {
						sRet="_______";
						if (sRet.length()>level) sRet=sRet.substring(0,l);
					}
					return sRet;
				} else {
					return "_";
				}
			} catch (Exception e) {
				vlog.e("getItemFirstLetters error",e);
			}
			String sRet="[error]";
			if (sRet.length()>level) {
				sRet="_______";
				if (sRet.length()>level) sRet=sRet.substring(0,level);
			}
			return sRet;
		}
	}

	public static class ItemGroupFilenameExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			return item.getFilename();
		}
	}

	public static class ItemGroupTitleExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			if (StrUtils.isEmptyStr(item.title)) return item.getFilename();
			return item.title;
		}
	}

	public static class ItemGroupAuthorExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			if (StrUtils.isEmptyStr(item.getAuthors())) return item.getFilename();
			return item.getAuthors();
		}
	}

	public static class ItemGroupSeriesExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			if (StrUtils.isEmptyStr(item.getSeriesName())) return item.getFilename();
			return item.getSeriesName();
		}
	}

	public static class ItemGroupGenresExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			if (StrUtils.isEmptyStr(item.getGenres())) return item.getFilename();
			return item.getGenres();
		}
	}

	public static class ItemGroupBookDateNExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			if ((StrUtils.isEmptyStr(item.getBookdate()))||
				StrUtils.isEmptyStr(Utils.formatDateFixed(item.bookDateN))) return item.getFilename();
			String s = Utils.formatDateFixed(item.bookDateN);
			return s;
		}
	}

	public static class ItemGroupDocDateNExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			if ((StrUtils.isEmptyStr(item.getDocdate()))||
					StrUtils.isEmptyStr(Utils.formatDateFixed(item.docDateN))) return item.getFilename();
			String s = Utils.formatDateFixed(item.docDateN);
			return s;
		}
	}

	public static class ItemGroupPublYearNExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			if ((StrUtils.isEmptyStr(item.getPublyear()))||
					StrUtils.isEmptyStr(Utils.formatDateFixed(item.publYearN))) return item.getFilename();
			String s = Utils.formatDateFixed(item.publYearN);
			return s;
		}
	}

	public static class ItemGroupFileCreateTimeExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			if (StrUtils.isEmptyStr(Utils.formatDateFixed(item.fileCreateTime))) return item.getFilename();
			String s = Utils.formatDateFixed(item.fileCreateTime);
			return s;
		}
	}

	public static class ItemGroupRatingExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			String s = ""+item.getRate();
			return s;
		}
	}

	public static class ItemGroupStateExtractor extends ItemGroupExtractor {
		@Override
		public String getComparisionField(FileInfo item) {
			int state = item.getReadingState();
			String s = CoolReader.BOOK_READING_STATE_NO_STATE;
			if (state == FileInfo.STATE_TO_READ) s = CoolReader.BOOK_READING_STATE_TO_READ;
			if (state == FileInfo.STATE_READING) s = CoolReader.BOOK_READING_STATE_READING;
			if (state == FileInfo.STATE_FINISHED) s = CoolReader.BOOK_READING_STATE_FINISHED;
			return s;
		}
	}
	
	public static FileInfo createItemGroup(String groupPrefix, String groupPrefixTag) {
		FileInfo groupDir = new FileInfo();
		groupDir.isDirectory = true;
		groupDir.pathname = groupPrefixTag + groupPrefix;
		groupDir.setFilename(groupPrefix + "...");
		groupDir.isListed = true;
		groupDir.isScanned = true;
		groupDir.id = 0l;
		return groupDir;
	}
	
	private void sortItems(ArrayList<FileInfo> items, final ItemGroupExtractor extractor, final boolean sortAsc) {
		Collections.sort(items, (lhs, rhs) -> {
			String l = extractor.getComparisionField(lhs) != null ? extractor.getComparisionField(lhs).toUpperCase() : "";
			String r = extractor.getComparisionField(rhs) != null ? extractor.getComparisionField(rhs).toUpperCase() : "";
			if (sortAsc) return l.compareTo(r);
			return r.compareTo(l);
		});
	}

	static int MAX_GROUP_LEVEL = 30;

	public static void addGroupedItems2(FileInfo parent, String filter,
								  ArrayList<FileInfo> items, String groupPrefixTag, final ItemGroupExtractor extractor, int lev) {
		int iMaxItemsCount = iMaxGroupSize;
		log.i("iMaxItemsCount "+iMaxItemsCount);
		if (iMaxItemsCount<8) iMaxItemsCount = 8;
		if (lev >= MAX_GROUP_LEVEL) {
			addItems(parent, items, 0, items.size());
			return;
		}
		// if there are already small amount
		if (items.size()<=iMaxItemsCount) {
			addItems(parent, items, 0, items.size());
			return;
		}
		int curLevel = 0;
		int level = 1; // initial level
		int prevSize = 0;
		if (!StrUtils.isEmptyStr(filter)) level = filter.length();
		boolean br = false;
		HashMap<String, Integer> grouped = null;
		while ((level <= MAX_GROUP_LEVEL) && (!br)) {
			HashMap<String, Integer> groupedCur = new HashMap<String, Integer>();
			for (int i=0; i<items.size(); i++) {
				String firstLetter = extractor.getItemFirstLetters(items.get(i), level);
				if ((firstLetter.toUpperCase().startsWith(filter.toUpperCase())) || (StrUtils.isEmptyStr(filter))) {
					// avoid equality with parent
					//if (!firstLetter.toUpperCase().equals(parent.getFilename().toUpperCase()+"...")) {
					Integer cnt = groupedCur.get(firstLetter);
					if (cnt == null) cnt = 0;
					groupedCur.put(firstLetter, cnt + 1);
					//}
				}
			}
			if ((groupedCur.size()<=iMaxItemsCount) || (curLevel == 0)  || (prevSize == 1)) {
				curLevel = level;
				grouped = groupedCur;
				prevSize = grouped.size();
			}
			else
				br = true;
			level = level + 1;
		}
		if (grouped==null) {
			addItems(parent, items, 0, items.size());
			return;
		}
		if (grouped.size()==1) { // grouping failed :)
			addItems(parent, items, 0, items.size());
			return;
		}
		// we have found maximum allowable group level
		int curSize = grouped.size();
		for (Map.Entry<String, Integer> entry : grouped.entrySet()) {
			String key = (String) entry.getKey();
			Integer value = (Integer) entry.getValue();
			if ((curSize-1+value <= iMaxItemsCount)&&(value>1)) { // this group can be "linearized"
				for (int i=0; i<items.size(); i++) {
					String firstLetter = extractor.getItemFirstLetters(items.get(i), key.length());
					if (firstLetter.toUpperCase().equals(key.toUpperCase())) {
						items.get(i).parent = parent;
						if (items.get(i).isSpecialDir()) {
							parent.addDir(items.get(i));
						} else
							parent.addFile(items.get(i));
					}
				}
				curSize = curSize-1+value;
			} else
			if (value == 1) { // group of one element - adding element
				for (int i=0; i<items.size(); i++) {
					String firstLetter = extractor.getItemFirstLetters(items.get(i), key.length());
					if (firstLetter.toUpperCase().equals(key.toUpperCase())) {
						items.get(i).parent = parent;
						if (items.get(i).isSpecialDir()) {
							parent.addDir(items.get(i));
						} else
							parent.addFile(items.get(i));
					}
				}
			} else { // really add group
				ArrayList<FileInfo> itemsGr = new ArrayList<FileInfo>();
				for (int i=0; i<items.size(); i++) {
					String firstLetter = extractor.getItemFirstLetters(items.get(i), key.length());
					if (firstLetter.toUpperCase().equals(key.toUpperCase())) itemsGr.add(items.get(i));
				}
				FileInfo newGroup = createItemGroup(key, groupPrefixTag);
				newGroup.parent = parent;
				parent.addDir(newGroup);
				addGroupedItems2(newGroup, key, itemsGr, groupPrefixTag, extractor, lev + 1);
			}
		}
	}
	
	private void addGroupedItems(FileInfo parent,
				ArrayList<FileInfo> items, int start, int end, String groupPrefixTag, int level, final ItemGroupExtractor extractor) {
		boolean bNoGroups = true;
		if (((parent.pathname.startsWith(FileInfo.TITLE_TAG_LEVEL))&&(level<5))
				||parent.pathname.startsWith(FileInfo.TITLE_TAG)) bNoGroups = true;
		if (groupPrefixTag.equals(FileInfo.BOOK_DATE_GROUP_PREFIX)) bNoGroups = true;
		if (groupPrefixTag.equals(FileInfo.DOC_DATE_GROUP_PREFIX)) bNoGroups = true;
		if (groupPrefixTag.equals(FileInfo.PUBL_YEAR_GROUP_PREFIX)) bNoGroups = true;
		if (groupPrefixTag.equals(FileInfo.FILE_DATE_GROUP_PREFIX)) bNoGroups = true;
		if (groupPrefixTag.equals(FileInfo.GENRE_GROUP_PREFIX)) bNoGroups = true;
		if (level>30) bNoGroups = true; // to prevent infinite loop when list is bad

		int itemCount = end - start;
		if (itemCount < 1)
			return;
		// for nested level (>1), create base subgroup, otherwise use parent 
		if ((level > 1 && itemCount > 1) && (!bNoGroups)) {
			String baseFirstLetter = extractor.getItemFirstLetters(items.get(start), level - 1);
			FileInfo newGroup = createItemGroup(baseFirstLetter, groupPrefixTag);
			newGroup.parent = parent;
			parent.addDir(newGroup);
			parent = newGroup;
		}
		// check group count
		int topLevelGroupsCount = 0;
		String lastFirstLetter = "";
		for (int i=start; i<end; i++) {
			String firstLetter = extractor.getItemFirstLetters(items.get(i), level);
			if (!firstLetter.equals(lastFirstLetter)) {
				topLevelGroupsCount++;
				lastFirstLetter = firstLetter;
			}
		}

		int iMaxItemsCount = iMaxGroupSize;
		log.i("iMaxItemsCount "+iMaxItemsCount);
		if (iMaxItemsCount<8) iMaxItemsCount = 8;
		if ((itemCount <= topLevelGroupsCount * 11 / 10 || itemCount < iMaxItemsCount)||(bNoGroups)) {
			// small number of items: add as is
			addItems(parent, items, start, end); 
			return;
		}

		// divide items into groups
		for (int i=start; i<end; ) {
			String firstLetter = extractor.getItemFirstLetters(items.get(i), level);
			int groupEnd = i + 1;
			for (; groupEnd < end; groupEnd++) {
				String firstLetter2 = groupEnd < end ? extractor.getItemFirstLetters(items.get(groupEnd), level) : "";
				if (!firstLetter.equals(firstLetter2))
					break;
			}
			// group is i..groupEnd
			addGroupedItems(parent, items, i, groupEnd, groupPrefixTag, level + 1, extractor);
			i = groupEnd;
		}
	}
	
	private boolean loadItemList(ArrayList<FileInfo> list, String sql, String groupPrefixTag, boolean sortAsc) {
		boolean found = false;
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				// read DB
				do {
					long id = rs.getLong(0);
					String name = rs.getString(1);
					if (FileInfo.AUTHOR_PREFIX.equals(groupPrefixTag))
						name = Utils.authorNameFileAs(name);
					int bookCount = rs.getInt(2);
					
					FileInfo item = new FileInfo();
					item.isDirectory = true;
					item.pathname = groupPrefixTag + id;
					item.setFilename(name);
					item.isListed = true;
					item.isScanned = true;
					item.id = id;
					item.tag = bookCount;
					
					list.add(item);
					found = true;
				} while (rs.moveToNext());
			}
		} catch (Exception e) {
			Log.e("cr3", "exception while loading list of authors", e);
		}
		sortItems(list, new ItemGroupFilenameExtractor(), sortAsc);
		return found;
	}
	
	public boolean loadAuthorsList(FileInfo parent, String filterSeries) {
		Log.i("cr3", "loadAuthorsList()");
		beginReading();
		// gather missed stats
		try (Cursor rs = mDB.rawQuery(
				"select a.id, count(*) as cnt " +
						"from author a " +
						"join book_author ba on ba.author_fk = a.id " +
						"join book b on b.id = ba.book_fk and (not (b.pathname like '@opds%')) " +
						"where a.book_cnt is null " +
						"group by a.id", null)) {
			if (rs.moveToFirst()) {
				// read DB
				do {
					execSQLIgnoreErrors("update author set book_cnt = " + rs.getLong(1) + " where id = " + rs.getLong(0));
				} while (rs.moveToNext());
			}
			if (rs != null) rs.close();
		} catch (Exception e) {
			Log.e("cr3", "exception while loading list of authors", e);
		}
		//\
		parent.clear();
		ArrayList<FileInfo> list = new ArrayList<FileInfo>();

		String sql = "";

		if (StrUtils.isEmptyStr(filterSeries))
			sql = "SELECT author.id, author.name, book_cnt as book_count FROM author ORDER BY author.name";
		else {
			String series = "";
			if (filterSeries.startsWith("seriesId:")) series = filterSeries.replace("seriesId:", "");
			else if (filterSeries.startsWith("series:")) {
				String s = filterSeries.replace("series:", "").trim();
				long l = getLongValue("SELECT series.id FROM series where trim(series.name) = " + quoteSqlString(s));
				if (l > 0) series = ""+l;
			}
			if (StrUtils.isEmptyStr(series)) return false;
			sql = "SELECT author.id, author.name, count(*) as book_count FROM author "+
					" JOIN book on book.id = book_author.book_fk and (not (book.pathname like '@opds%')) "+
					"   and (book.series_fk = " + series + " or book.publseries_fk = " + series + ") " +
					" JOIN book_author ON  book_author.author_fk = author.id " +
					" GROUP BY author.name, author.id ORDER BY author.name";
		}
		boolean found = loadItemList(list, sql, FileInfo.AUTHOR_PREFIX, true);
		//
		sortItems(list, new ItemGroupFilenameExtractor(), true);
		// remove duplicate titles
		for (int i=list.size() - 1; i>0; i--) {
			String title = list.get(i).getFilename();
			if (title == null) {
				list.remove(i);
				continue;
			}
			String prevTitle = list.get(i - 1).getFilename();
			if (title.equals(prevTitle))
				list.remove(i);
		}
		//addGroupedItems(parent, list, 0, list.size(), FileInfo.AUTHOR_GROUP_PREFIX, 1, new ItemGroupFilenameExtractor());
		addGroupedItems2(parent, "", list, FileInfo.AUTHOR_GROUP_PREFIX, new ItemGroupFilenameExtractor(),1);
		endReading();
		return found;
	}

	public boolean loadGenresList(FileInfo parent) {
		Log.i("cr3", "loadGenresList()");
		beginReading();
		parent.clear();
		ArrayList<FileInfo> list = new ArrayList<FileInfo>();
		String lang = currentLanguage;
		if (!StrUtils.isEmptyStr(lang)) lang = lang.substring(0,2).toUpperCase();
		// gather missed stats
		try (Cursor rs = mDB.rawQuery(
				"select g.id, count(*) as cnt "+
						"from genre g "+
						"join book_genre bg on bg.genre_fk = g.id "+
						"join book b on b.id = bg.book_fk and (not (b.pathname like '@opds%')) "+
						"where g.book_cnt is null "+
						"group by g.id", null)) {
			if (rs.moveToFirst()) {
				// read DB
				do {
					execSQLIgnoreErrors("update genre set book_cnt = " + rs.getLong(1) + " where id = " + rs.getLong(0));
				} while (rs.moveToNext());
			}
			if (rs != null) rs.close();
		} catch (Exception e) {
			Log.e("cr3", "exception while loading list of genres", e);
		}
		//\
		String gname =
			" trim(coalesce(nullif(coalesce(genre_transl.name,''),''), "+
			"nullif(coalesce(genre.name,''),''), "+
			"nullif(coalesce(genre.code,''),''))) ";
		//String sql = "SELECT genre.id, "+gname+" name, count(*) as book_count FROM genre "+
		//		" LEFT JOIN  genre_transl on genre_transl.code = genre.code and genre_transl.lang = '"+lang+"' " +
		//		" INNER JOIN book on book.id = book_genre.book_fk and (not (book.pathname like '@opds%')) "+
		//		" INNER JOIN book_genre ON book_genre.genre_fk = genre.id GROUP BY "+gname+", genre.id ORDER BY "+gname;
		String sql = "SELECT genre.id, "+gname+" name, book_cnt as book_count FROM genre "+
				" LEFT JOIN genre_transl on genre_transl.code = genre.code and genre_transl.lang = '"+lang+"' " +
				" ORDER BY "+gname;
		boolean found = loadItemList(list, sql, FileInfo.GENRE_PREFIX, true);
		//
		sortItems(list, new ItemGroupFilenameExtractor(), true);
		// remove duplicate titles
		for (int i=list.size() - 1; i>0; i--) {
			String title = list.get(i).getFilename();
			if (title == null) {
				list.remove(i);
				continue;
			}
			String prevTitle = list.get(i - 1).getFilename();
			if (title.equals(prevTitle))
				list.remove(i);
		}
		//addGroupedItems(parent, list, 0, list.size(), FileInfo.GENRE_GROUP_PREFIX, 1, new ItemGroupFilenameExtractor());
		addGroupedItems2(parent, "", list, FileInfo.GENRE_GROUP_PREFIX, new ItemGroupFilenameExtractor(),1);
		endReading();
		return found;
	}

	public long getLongValue(String sql) {
		long res = -1;
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				do {
					res = rs.getLong(0);
				} while (rs.moveToNext());
			}
			if (rs != null) rs.close();
		} catch (Exception e) {
			Log.e("cr3", "exception while getLongValue", e);
		}
		return res;
	};

	public boolean loadSeriesList(FileInfo parent, String filterAuthor) {
		Log.i("cr3", "loadSeriesList()");
		beginReading();
		parent.clear();
		// gather missed stats
		//execSQLIgnoreErrors("update series set book_cnt = null");
		try (Cursor rs = mDB.rawQuery(
				"select s.id, count(*) as cnt " +
						"from series s " +
						"join book b on (s.id = b.series_fk or s.id = b.publseries_fk) " +
						"  and s.book_cnt is null " +
						"where (not (b.pathname like '@opds%')) " +
						"group by s.id ", null)) {
			if (rs.moveToFirst()) {
				// read DB
				do {
					execSQLIgnoreErrors("update series set book_cnt = " + rs.getLong(1) + " where id = " + rs.getLong(0));
				} while (rs.moveToNext());
			}
			if (rs != null) rs.close();
		} catch (Exception e) {
			Log.e("cr3", "exception while loading list of series", e);
		}
		//\
		ArrayList<FileInfo> list = new ArrayList<FileInfo>();
		String sql = "";

		if (StrUtils.isEmptyStr(filterAuthor))
			sql = "SELECT series.id, series.name, book_cnt as book_count FROM series "+
					" ORDER BY series.name";
		else {
			String author = "";
			if (filterAuthor.startsWith("authorId:")) author = filterAuthor.replace("authorId:", "");
			else if (filterAuthor.startsWith("author:")) {
				String s = filterAuthor.replace("author:", "").trim();
				long l = getLongValue("SELECT author.id FROM author where trim(author.name) = " + quoteSqlString(s));
				if (l > 0) author = ""+l;
			}
			if (StrUtils.isEmptyStr(author)) return false;
			sql = "SELECT series.id, series.name, count(*) as book_count FROM series " +
					" JOIN book ON book.series_fk = series.id or book.publseries_fk = series.id " +
					" JOIN book_author ON book_author.book_fk = book.id and book_author.author_fk = " + author +
					" where (not (book.pathname like '@opds%')) GROUP BY series.name, series.id ORDER BY series.name";
		}
		boolean found = loadItemList(list, sql, FileInfo.SERIES_PREFIX, true);
		//
		sortItems(list, new ItemGroupFilenameExtractor(), true);
		// remove duplicate titles
		for (int i=list.size() - 1; i>0; i--) {
			String title = list.get(i).getFilename();
			if (title == null) {
				list.remove(i);
				continue;
			}
			String prevTitle = list.get(i - 1).getFilename();
			if (title.equals(prevTitle))
				list.remove(i);
		}
		//addGroupedItems(parent, list, 0, list.size(), FileInfo.SERIES_GROUP_PREFIX, 1, new ItemGroupFilenameExtractor());
		//addGroupedItems2(parent, list, 0, list.size(), FileInfo.SERIES_GROUP_PREFIX, 1, new ItemGroupFilenameExtractor());
		addGroupedItems2(parent, "", list, FileInfo.SERIES_GROUP_PREFIX, new ItemGroupFilenameExtractor(),1);
		endReading();
		return found;
	}

	public boolean loadByDateList(FileInfo parent, final String field) {
		Log.i("cr3", "loadByDateList()");
		beginReading();
		parent.clear();
		// gather missed stats
		//execSQLIgnoreErrors("update series set book_cnt = null");
		try (Cursor rs = mDB.rawQuery(
				"select bds.id, count(*) as cnt " +
						"from book_dates_stats bds " +
						"join book b on case when coalesce(b.book_date_n,0)=0 then 0 else " +
						"  cast(strftime('%s',datetime(b."+field+"/1000, 'unixepoch', 'start of month')) as integer) end " +
						"  = bds.book_date " +
						"  and bds.date_field ='"+field+"' " +
						"  and bds.book_cnt is null " +
						"where (not (b.pathname like '@opds%')) " +
						"group by bds.id", null)) {
			if (rs.moveToFirst()) {
				// read DB
				do {
					execSQLIgnoreErrors("update book_dates_stats set book_cnt = " + rs.getLong(1) + " where id = " + rs.getLong(0));
				} while (rs.moveToNext());
			}
			if (rs != null) rs.close();
		} catch (Exception e) {
			Log.e("cr3", "exception while loading list of dates", e);
		}
		//\
		ArrayList<FileInfo> list = new ArrayList<FileInfo>();

//		String f1 = "case when coalesce("+field+",0)=0 then 0 else "+
//			"cast(strftime('%s',datetime("+field+"/1000, 'unixepoch', 'start of month')) as integer) end";
//		String f2 = "case when coalesce("+field+",0)=0 then '[empty]' " +
//			" else strftime('%Y-%m', datetime("+field+"/1000, 'unixepoch', 'start of month')) end";
//		String sql = "SELECT "+f1+" as date_n, "+
//			f2 + " as date_s, " +
//			" count(*) as book_count FROM book where (not (book.pathname like '@opds%')) GROUP BY " + f1 + ", " +f2;

		String sql = "select book_date, " +
				"case when book_date=0 then '[empty]' else strftime('%Y-%m', datetime(book_date, 'unixepoch')) end book_date_text, " +
				"  book_cnt from book_dates_stats where date_field = '"+field+"' " +
				"order by 2 desc";

		String prefix = FileInfo.BOOK_DATE_PREFIX;
		String groupPrefix = FileInfo.BOOK_DATE_GROUP_PREFIX;
		if (field.equals("doc_date_n")) {
			prefix = FileInfo.DOC_DATE_PREFIX;
			groupPrefix = FileInfo.DOC_DATE_GROUP_PREFIX;
		}
		if (field.equals("publ_year_n")) {
			prefix = FileInfo.PUBL_YEAR_PREFIX;
			groupPrefix = FileInfo.PUBL_YEAR_GROUP_PREFIX;
		}
		if (field.equals("file_create_time")) {
			prefix = FileInfo.FILE_DATE_PREFIX;
			groupPrefix = FileInfo.FILE_DATE_GROUP_PREFIX;
		}
		boolean found = loadItemList(list, sql, prefix, false);
		// remove duplicate titles
		for (int i=list.size() - 1; i>0; i--) {
			String title = list.get(i).getFilename();
			if (title == null) {
				list.remove(i);
				continue;
			}
			String prevTitle = list.get(i - 1).getFilename();
			if (title.equals(prevTitle))
				list.remove(i);
		}
		//addGroupedItems(parent, list, 0, list.size(), groupPrefix, 1, new ItemGroupFilenameExtractor());
		addGroupedItems2(parent, "", list, groupPrefix, new ItemGroupFilenameExtractor(),1);
		endReading();
		return found;
	}
	
	public boolean loadTitleList(FileInfo parent) {
		Log.i("cr3", "loadTitleList()");
		execSQLIgnoreErrors("UPDATE book SET title=filename WHERE title is null");
		// gather missed stats
		try (Cursor rs = mDB.rawQuery(
				"select bts.stat_level, bts.text_value, count(*) as cnt " +
						"from book b " +
						"join book_titles_stats bts on " +
						"  bts.text_field='book_title' and " +
						"  b.title like bts.text_value || '%' " +
						"  and substr(b.title,1,length(bts.text_value)) = bts.text_value " +
						"  and bts.book_cnt is null " +
						"where (not (b.pathname like '@opds%')) " +
						"group by bts.stat_level, bts.text_value", null);
		) {
			if (rs.moveToFirst()) {
				// read DB
				do {
					execSQLIgnoreErrors("update book_titles_stats set book_cnt = " +
						rs.getLong(2) +
						" where stat_level = " + rs.getInt(0) +
						" and text_value = " + quoteSqlString(rs.getString(1)));
				} while (rs.moveToNext());
			}
			if (rs != null) rs.close();
		} catch (Exception e) {
			Log.e("cr3", "exception while loading list of series", e);
		}
		//\
		parent.clear();
		ArrayList<FileInfo> list = new ArrayList<FileInfo>();
		ArrayList<FileInfo> listFiles = new ArrayList<FileInfo>();
		beginReading();
		int lev = 0;
		if (StrUtils.getNonEmptyStr(parent.pathname,false).startsWith(FileInfo.TITLE_TAG_LEVEL)) {
			lev = Integer.valueOf(StrUtils.getNonEmptyStr(parent.pathname,false).replace(FileInfo.TITLE_TAG_LEVEL+":", ""));
		}
		int iMaxItemsCount = iMaxGroupSize;
		if (iMaxItemsCount<8) iMaxItemsCount = 8;
		//iMaxItemsCount = 2; // for tests
		String sql = "SELECT bts.id, bts.text_value, bts.book_cnt as book_count FROM book_titles_stats bts "+
				" where bts.text_field='book_title' and bts.stat_level = " + lev +
				" and bts.book_cnt > " + iMaxItemsCount;
		if ((lev >= 1) && (lev <= 2)) {
			sql = sql + " and bts.text_value like " +quoteSqlString(parent.getFilename().substring(0,lev)+ "%");
		}
		sql = sql + " ORDER BY bts.text_value";
		boolean found = false;
		boolean found2 = false;
		if (lev > 2) {
			sql = READ_FILEINFO_SQL + " WHERE b.title IS NOT NULL AND b.title != '' and (not (b.pathname like '@opds%')) "+
					" and b.title like "+quoteSqlString(parent.getFilename().substring(0,lev)+ "%")+
					" and substr(b.title,1,length("+quoteSqlString(parent.getFilename().substring(0,lev))+")) = "+
					quoteSqlString(parent.getFilename().substring(0,lev)) +
					" ORDER BY b.title";
			found = findBooks(sql, list);
		} else {
			found = loadItemList(list, sql, FileInfo.TITLE_GROUP_PREFIX, true);
			for (FileInfo fi : list) {
				fi.pathname = FileInfo.TITLE_TAG_LEVEL +":"+ (lev + 1);
				fi.setFilename(fi.getFilename() + "...");
			}
			// Non grouped filenames
			String sCond = "";
			if (lev > 0) sCond = " and b.title like " + quoteSqlString(parent.getFilename().substring(0, lev) + "%") +
					" and substr(b.title,1,length("+quoteSqlString(parent.getFilename().substring(0, lev))+")) = "+
					quoteSqlString(parent.getFilename().substring(0, lev));
			sql = READ_FILEINFO_SQL +
					" join book_titles_stats bts on bts.text_field='book_title' and bts.stat_level = " + lev +
					" and b.title like bts.text_value||'%' " +
					" and substr(b.title,1,length(bts.text_value)) = bts.text_value "+
					" and bts.book_cnt <= " + iMaxItemsCount +
					" WHERE b.title IS NOT NULL AND b.title != '' and (not (b.pathname like '@opds%')) " +
					" and length(b.title)>"+lev+sCond+
					" ORDER BY b.title";
			found2 = findBooks(sql, listFiles);
			if (found2) for (FileInfo fi: listFiles) list.add(fi);
			// Short filenames
			if (lev > 0) {
				sql = READ_FILEINFO_SQL + " WHERE b.title IS NOT NULL AND b.title != '' and (not (b.pathname like '@opds%')) " +
						" and length(b.title)="+lev+" and b.title like " + quoteSqlString(parent.getFilename().substring(0, lev) + "%") +
						" and substr(b.title,1,length(" + quoteSqlString(parent.getFilename().substring(0, lev))+")) = "+
						quoteSqlString(parent.getFilename().substring(0, lev)) +
						" ORDER BY b.title";
				found2 = findBooks(sql, listFiles);
				if (found2) for (FileInfo fi: listFiles) list.add(fi);
			}
		}
		sortItems(list, new ItemGroupTitleExtractor(), true);
		// remove duplicate titles
		for (int i=list.size() - 1; i>0; i--) {
			String title = list.get(i).title;
			if (title == null) {
				list.remove(i);
				continue;
			}
			String prevTitle = list.get(i - 1).title;
			if (title.equals(prevTitle))
				list.remove(i);
		}
		//addGroupedItems(parent, list, 0, list.size(), FileInfo.TITLE_GROUP_PREFIX, 1+lev+1, new ItemGroupTitleExtractor());
		addGroupedItems2(parent, parent.getFilename().substring(0,lev), list, FileInfo.TITLE_GROUP_PREFIX, new ItemGroupTitleExtractor(),1);
		endReading();
		return found;
	}
	
	public boolean findAuthorBooks(ArrayList<FileInfo> list, long authorId, String addFilter)
	{
		if (!isOpened())
			return false;
		long author = authorId;
		if (!StrUtils.isEmptyStr(addFilter)) {
			if (addFilter.startsWith("author:")) {
				String s = addFilter.replace("author:", "").trim();
				long l = getLongValue("SELECT author.id FROM author where trim(author.name) = " + quoteSqlString(s));
				if (l > 0) author = l;
			}
		}
		if (author == 0L) return false;
		String sql = READ_FILEINFO_SQL + " INNER JOIN book_author ON book_author.book_fk = b.id "+
				" WHERE (not (b.pathname like '@opds%')) and book_author.author_fk = " + author + " ORDER BY b.title";
		return findBooks(sql, list);
	}

	public boolean findGenreBooks(ArrayList<FileInfo> list, long genreId)
	{
		if (!isOpened())
			return false;
		String sql = READ_FILEINFO_SQL + " INNER JOIN book_genre ON book_genre.book_fk = b.id "+
				" WHERE (not (b.pathname like '@opds%')) and book_genre.genre_fk = " + genreId + " ORDER BY b.title";
		return findBooks(sql, list);
	}
	
	public boolean findSeriesBooks(ArrayList<FileInfo> list, long seriesId, String addFilter)
	{
		if (!isOpened())
			return false;
		long series = seriesId;
		if (!StrUtils.isEmptyStr(addFilter)) {
			if (addFilter.startsWith("series:")) {
				String s = addFilter.replace("series:", "").trim();
				long l = getLongValue("SELECT series.id FROM series where trim(series.name) = " + quoteSqlString(s));
				if (l > 0) series = l;
			}
		}
		if (series == 0L) return false;
		String sql = READ_FILEINFO_SQL + " INNER JOIN series ON series.id = b.series_fk or series.id = b.publseries_fk "+
				" WHERE (not (b.pathname like '@opds%')) and series.id = " + series + " ORDER BY b.series_number, b.title";
		return findBooks(sql, list);
	}

	public boolean findByDateBooks(ArrayList<FileInfo> list, final String field, long bookdateId)
	{
		if (!isOpened())
			return false;
		String f1 = "case when coalesce("+field+",0)=0 then 0 else "+
				"cast(strftime('%s',datetime("+field+"/1000, 'unixepoch', 'start of month')) as integer) end";
		String sql = READ_FILEINFO_SQL + " WHERE (not (b.pathname like '@opds%')) and "+f1+" = " + bookdateId +
				" ORDER BY b.title";
		vlog.i(sql);
		return findBooks(sql, list);
	}
	
	public boolean findBooksByRating(ArrayList<FileInfo> list, int minRate, int maxRate)
	{
		if (!isOpened())
			return false;
		String sql = READ_FILEINFO_SQL + " WHERE (not (b.pathname like '@opds%')) and "+
				" ((flags>>20)&15) BETWEEN " + minRate + " AND " + maxRate + " ORDER BY ((flags>>20)&15) DESC, b.title LIMIT 1000";
		return findBooks(sql, list);
	}
	
	public boolean findBooksByState(ArrayList<FileInfo> list, int state)
	{
		if (!isOpened())
			return false;
		String sql = READ_FILEINFO_SQL + " WHERE (not (b.pathname like '@opds%')) and "+
				" ((flags>>16)&15) = " + state + " ORDER BY b.title LIMIT 1000";
		return findBooks(sql, list);
	}
	
	private String findAuthors(int maxCount, String authorPattern) {
		StringBuilder buf = new StringBuilder();
		String sql = "SELECT id, name FROM author";
		int count = 0;
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				do {
					long id = rs.getLong(0);
					String name = rs.getString(1);
					if (Utils.matchPattern(name, authorPattern)) {
						if (buf.length() != 0)
							buf.append(",");
						buf.append(id);
						count++;
						if (count >= maxCount)
							break;
					}
				} while (rs.moveToNext());
			}
		}
		return buf.toString();
	}

	private Map<String, String> getGenres() {
		HashMap<String, String> res = new HashMap<String, String>();
		String lang = currentLanguage;
		if (!StrUtils.isEmptyStr(lang)) lang = lang.substring(0,2).toUpperCase();
		String sql = "SELECT genre.id, genre.code, genre.name, genre_transl.name tr_name FROM genre "+
				" LEFT JOIN genre_transl on genre_transl.code = genre.code and genre_transl.lang = '"+lang+"'";
		int count = 0;
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				do {
					res.put(rs.getString(1),
							StrUtils.getNonEmptyStr(rs.getString(2),true)+"|"+
									StrUtils.getNonEmptyStr(rs.getString(3),true));
				} while (rs.moveToNext());
			}
		}
		return res;
	}

	//=======================================================================================
    // Series access code
    //=======================================================================================
	
	private SQLiteStatement seriesStmt;
	private SQLiteStatement seriesSelectStmt;
	private HashMap<String,Long> seriesCache = new HashMap<String,Long>();
	public Long getSeriesId(String seriesName)
	{
		if (seriesName == null || seriesName.trim().length() == 0)
			return null;
		Long id = seriesCache.get(seriesName); 
		if (id != null)
			return id;
		if (seriesSelectStmt == null)
			seriesSelectStmt = mDB.compileStatement("SELECT id FROM series WHERE name=?");
		try {
			seriesSelectStmt.bindString(1, seriesName);
			return seriesSelectStmt.simpleQueryForLong();
		} catch (Exception e) {
			// not found
		}
		if (seriesStmt == null)
			seriesStmt = mDB.compileStatement("INSERT INTO series (id, name) VALUES (NULL,?)");
		seriesStmt.bindString(1, seriesName);
		id = seriesStmt.executeInsert();
		seriesCache.put(seriesName, id);
		return id;
	}
	
	//=======================================================================================
    // Folder access code
    //=======================================================================================
	
	private SQLiteStatement folderStmt;
	private SQLiteStatement folderSelectStmt;
	private HashMap<String,Long> folderCache = new HashMap<String,Long>();
	public Long getFolderId(String folderName)
	{
		if (folderName == null || folderName.trim().length() == 0)
			return null;
		Long id = folderCache.get(folderName); 
		if (id != null)
			return id;
		if (folderSelectStmt == null)
			folderSelectStmt = mDB.compileStatement("SELECT id FROM folder WHERE name=?");
		try {
			folderSelectStmt.bindString(1, folderName);
			return folderSelectStmt.simpleQueryForLong();
		} catch (Exception e) {
			// not found
		}
		if (folderStmt == null)
			folderStmt = mDB.compileStatement("INSERT INTO folder (id, name) VALUES (NULL,?)");
		folderStmt.bindString(1, folderName);
		id = folderStmt.executeInsert();
		folderCache.put(folderName, id);
		return id;
	}
	
	//=======================================================================================
    // Author access code
    //=======================================================================================
	
	private SQLiteStatement authorStmt;
	private SQLiteStatement authorSelectStmt;
	private HashMap<String,Long> authorCache = new HashMap<String,Long>();
	private Long getAuthorId(String authorName) {
		if (authorName == null || authorName.trim().length() == 0)
			return null;
		Long id = authorCache.get(authorName); 
		if (id != null)
			return id;
		if (authorSelectStmt == null)
			authorSelectStmt = mDB.compileStatement("SELECT id FROM author WHERE name=?");
		try {
			authorSelectStmt.bindString(1, authorName);
			return authorSelectStmt.simpleQueryForLong();
		} catch (Exception e) {
			// not found
		}
		if (authorStmt == null)
			authorStmt = mDB.compileStatement("INSERT INTO author (id, name) VALUES (NULL,?)");
		authorStmt.bindString(1, authorName);
		id = authorStmt.executeInsert();
		authorCache.put(authorName, id);
		return id;
	}
	
	private Long[] getAuthorIds(String authorNames) {
		if (authorNames == null || authorNames.trim().length() == 0)
			return null;
		String[] names = authorNames.split("\\|");
		if (names == null || names.length == 0)
			return null;
		ArrayList<Long> ids = new ArrayList<Long>(names.length);
		for (String name : names) {
			Long id = getAuthorId(name);
			if (id != null)
				ids.add(id);
		}
		if (ids.size() > 0)
			return ids.toArray(new Long[ids.size()]);
		return null;
	}
	
	public void saveBookAuthors(Long bookId, Long[] authors) {
		if (authors == null || authors.length == 0)
			return;
		String insertQuery = "INSERT OR IGNORE INTO book_author (book_fk,author_fk) VALUES ";
		for (Long id : authors) {
			String sql = insertQuery + "(" + bookId + "," + id + ")"; 
			//Log.v("cr3", "executing: " + sql);
			mDB.execSQL(sql);
		}
	}

	//=======================================================================================
	// Genre access code
	//=======================================================================================

	private SQLiteStatement genreStmt;
	private SQLiteStatement genreStmtT;
	private SQLiteStatement genreSelectStmt;
	private HashMap<String,Long> genreCache = new HashMap<String,Long>();
	private Long getGenreId(String genreCode, String genreLang) {
		if (genreCode == null || genreCode.trim().length() == 0)
			return null;
		Long id = genreCache.get(genreCode.trim());
		if (id != null)
			return id;
		if (genreSelectStmt == null)
			genreSelectStmt = mDB.compileStatement("SELECT id FROM genre WHERE code=?");
		try {
			genreSelectStmt.bindString(1, genreCode.trim());
			return genreSelectStmt.simpleQueryForLong();
		} catch (Exception e) {
			// not found
		}
		if (genreStmt == null)
			genreStmt = mDB.compileStatement("INSERT INTO genre (id, code, name) VALUES (NULL,?,?)");
		genreStmt.bindString(1, genreCode.trim());
		String[] geEN = GenreSAXElem.getGenreDescrFull("EN",genreCode.trim());
		String[] geRU = GenreSAXElem.getGenreDescrFull("RU",genreCode.trim());

		if (StrUtils.isEmptyStr(geEN[0]))
			genreStmt.bindString(2, genreCode.trim());
		else
			genreStmt.bindString(2, geEN[0]);
		id = genreStmt.executeInsert();
		genreCache.put(genreCode.trim(), id);
		if (genreStmtT == null)
			genreStmtT = mDB.compileStatement("INSERT INTO genre_transl (id, code, lang, name) VALUES (NULL,?,?,?)");

		genreStmtT.bindString(1, genreCode.trim());
		genreStmtT.bindString(2, "EN");
		if (StrUtils.isEmptyStr(geEN[1]))
			genreStmtT.bindString(3, genreCode.trim());
		else
			genreStmtT.bindString(3, geEN[1]);
		Long id2 = genreStmtT.executeInsert();
		genreStmtT.bindString(1, genreCode.trim());
		genreStmtT.bindString(2, "RU");
		if (StrUtils.isEmptyStr(geRU[1]))
			genreStmtT.bindString(3, genreCode.trim());
		else
			genreStmtT.bindString(3, geRU[1]);
		id2 = genreStmtT.executeInsert();
		return id;
	}

	private Long[] getGenreIds(String genreCodes, String genreLang) {
		if (genreCodes == null || genreCodes.trim().length() == 0)
			return null;
		String[] codes = genreCodes.split("\\|");
		if (codes == null || codes.length == 0)
			return null;
		ArrayList<Long> ids = new ArrayList<Long>(codes.length);
		for (String code : codes) {
			if (!StrUtils.isEmptyStr(code)) {
				Long id = getGenreId(code.trim(), genreLang);
				if (id != null)
					ids.add(id);
			}
		}
		if (ids.size() > 0)
			return ids.toArray(new Long[ids.size()]);
		return null;
	}

	public void saveBookGenres(Long bookId, Long[] genres) {
		if (genres == null || genres.length == 0)
			return;
		String insertQuery = "INSERT OR IGNORE INTO book_genre (book_fk,genre_fk) VALUES ";
		for (Long id : genres) {
			String sql = insertQuery + "(" + bookId + "," + id + ")";
			//Log.v("cr3", "executing: " + sql);
			mDB.execSQL(sql);
		}
	}

	private static boolean eq(String s1, String s2)
	{
		if (s1 != null)
			return s1.equals(s2);
		return s2==null;
	}

	private final static int FILE_INFO_CACHE_SIZE = 3000;
	private FileInfoCache fileInfoCache = new FileInfoCache(FILE_INFO_CACHE_SIZE); 
	
	private FileInfo findMovedFileInfo(String path) {
		ArrayList<FileInfo> list = new ArrayList<FileInfo>();
		FileInfo fi = new FileInfo(path);
		if (fi.exists()) {
			if (findAllBy(list, "filename", fi.getFilename())) {
				for (FileInfo item : list) {
					if (item.exists())
						continue;
					if (item.size == fi.size) {
						log.i("Found record for file of the same name and size: treat as moved " + item.getFilename() + " " + item.size);
						// fix and save
						item.pathname = fi.pathname;
						item.arcname = fi.arcname;
						item.arcsize = fi.arcsize;
						item.path = fi.path;
						item.setCreateTime(fi.getCreateTime());
						item.setFileCreateTime(fi.getFileCreateTime());
						save(item);
						fileInfoCache.put(item);
						return item;
					}
				}
			}
		}
		return null;
	}
	
	private FileInfo findFileInfoByPathname(String path, boolean detectMoved)
	{
		FileInfo existing = fileInfoCache.get(path);
		if (existing != null)
			return existing;
		FileInfo fileInfo = new FileInfo(); 
		if (findBy(fileInfo, "pathname", path)) {
			fileInfoCache.put(fileInfo);
			return fileInfo;
		}
		if (!detectMoved)
			return null;
		return findMovedFileInfo(path);
	}

	private FileInfo findFileInfoByOPDSLink(String opdsLink)
	{
		FileInfo existing = fileInfoCache.getByOPDSLink(opdsLink);
		if (existing != null)
			return existing;
		FileInfo fileInfo = new FileInfo();
		if (findBy(fileInfo, "opds_link", opdsLink)) {
			fileInfoCache.put(fileInfo);
			return fileInfo;
		}
		return null;
	}

	private FileInfo findFileInfoById(Long id)
	{
		if (id == null)
			return null;
		FileInfo existing = fileInfoCache.get(id);
		if (existing != null)
			return existing;
		FileInfo fileInfo = new FileInfo(); 
		if (findBy(fileInfo, "b.id", id)) {
			return fileInfo;
		}
		return null;
	}

	private boolean findBy(FileInfo fileInfo, String fieldName, Object fieldValue)
	{
		String condition;
		StringBuilder buf = new StringBuilder(" WHERE ");
		buf.append(fieldName);
		if (fieldValue == null) {
			buf.append(" IS NULL ");
		} else {
			buf.append("=");
			DatabaseUtils.appendValueToSql(buf, fieldValue);
			buf.append(" ");
		}
		condition = buf.toString();
		boolean found = false;
		try (Cursor rs = mDB.rawQuery(READ_FILEINFO_SQL +
				condition, null)) {
			if (rs.moveToFirst()) {
				readFileInfoFromCursor(fileInfo, rs);
				found = true;
			}
		}
		return found;
	}

	private boolean findAllBy(ArrayList<FileInfo> result, String fieldName, Object fieldValue)
	{
		String condition;
		StringBuilder buf = new StringBuilder(" WHERE ");
		buf.append(fieldName);
		if (fieldValue == null) {
			buf.append(" IS NULL ");
		} else {
			buf.append("=");
			DatabaseUtils.appendValueToSql(buf, fieldValue);
			buf.append(" ");
		}
		condition = buf.toString();
		boolean found = false;
		try (Cursor rs = mDB.rawQuery(READ_FILEINFO_SQL +
				condition, null)) {
			if (rs.moveToFirst()) {
				do {
					FileInfo fileInfo = new FileInfo();
					readFileInfoFromCursor(fileInfo, rs);
					result.add(fileInfo);
					found = true;
				} while (rs.moveToNext());
			}
		}
		return found;
	}

	private HashMap<String, Bookmark> loadBookmarks(FileInfo fileInfo) {
		HashMap<String, Bookmark> map = new HashMap<>();
		if (fileInfo.id != null) {
			ArrayList<Bookmark> bookmarks = new ArrayList<>();
			if (load(bookmarks, "book_fk=" + fileInfo.id + " ORDER BY type")) {
				for (Bookmark b : bookmarks) {
					// delete non-unique bookmarks
					String key = b.getUniqueKey();
					if (!map.containsKey(key))
						map.put(key, b);
					else {
						log.w("Removing non-unique bookmark " + b + " for " + fileInfo.getPathName());
						deleteBookmark(b);
					}
				}
			}
		}
		return map;
	}

	private boolean save(Bookmark v, long bookId)
	{
		Log.d("cr3db", "saving bookmark id=" + v.getId() + ", bookId=" + bookId + ", pos=" + v.getStartPos());
		Bookmark oldValue = new Bookmark();
		if (v.getId() != null) {
			// update
			oldValue.setId(v.getId());
			if (findBy(oldValue, "book_fk=" + bookId + " AND id=" + v.getId())) {
				// found, updating
				QueryHelper h = new QueryHelper(v, oldValue, bookId);
				h.update(v.getId());
			} else {
				oldValue = new Bookmark();
				QueryHelper h = new QueryHelper(v, oldValue, bookId);
				v.setId(h.insert());
			}
		} else {
			QueryHelper h = new QueryHelper(v, oldValue, bookId);
			v.setId(h.insert());
		}
		return true;
	}

	public void saveBookInfo(BookInfo bookInfo)	{
		if (!isOpened()) {
			Log.e("cr3db", "cannot save book info : DB is closed");
			return;
		}
		if (bookInfo == null || bookInfo.getFileInfo() == null)
			return;
		
		// save main data
		save(bookInfo.getFileInfo());
		fileInfoCache.put(bookInfo.getFileInfo());
		
		// save bookmarks
		HashMap<String, Bookmark> existingBookmarks = loadBookmarks(bookInfo.getFileInfo());
		int changed = 0;
		int removed = 0;
		int added = 0;
		for (Bookmark bmk : bookInfo.getAllBookmarks()) {
			 Bookmark existing = existingBookmarks.get(bmk.getUniqueKey());
			 if (existing != null) {
				 bmk.setId(existing.getId());
				 if (!bmk.equals(existing)) {
					 Long id = bookInfo.getFileInfo().id;
					 if (id == null) return;
					 save(bmk, id);
					 changed++;
				 }
				 existingBookmarks.remove(bmk.getUniqueKey()); // saved
			 } else {
				 // create new
				 Long id = bookInfo.getFileInfo().id;
				 if (id == null) return;
			 	 save(bmk, id);
			 	 added++;
			 }
		}
		if (existingBookmarks.size() > 0) {
			// remove bookmarks not found in new object
			for (Bookmark bmk : existingBookmarks.values()) {
				deleteBookmark(bmk);
				removed++;
			}
		}
		if (added + changed + removed > 0)
			vlog.i("bookmarks added:" + added + ", updated: " + changed + ", removed:" + removed);
	}

	private void clearBookStats(FileInfo fileInfo,
			boolean authorsChanged, boolean genresChanged, boolean seriesChanged, boolean titleChanged, boolean folderChanged,
			boolean bookDateNChanged, boolean docDateNChanged, boolean publYearNChanged, boolean fileCreateTimeChanged)	{
		if (authorsChanged) {
			Long[] authorIds = getAuthorIds(fileInfo.getAuthors());
			if (authorIds != null)
				for (Long authorId : authorIds) {
					execSQL("UPDATE author set book_cnt = null WHERE id = " + authorId);
				}
		}
		if (genresChanged) {
			Long[] genreIds = getGenreIds(fileInfo.getGenres(), currentLanguage);
			if (genreIds != null)
				for (Long genreId : genreIds) {
					execSQL("UPDATE genre set book_cnt = null WHERE id = " + genreId);
				}
		}
		if (seriesChanged) {
			long s = 0;
			if (getSeriesId(fileInfo.series) != null) s = getSeriesId(fileInfo.series);
			execSQL("UPDATE series set book_cnt = null WHERE id = " + s);
			long s2 = 0;
			if (getSeriesId(fileInfo.publseries) != null) s2 = getSeriesId(fileInfo.publseries);
			if (s != s2)
				execSQL("UPDATE series set book_cnt = null WHERE id = " + s2);
		}
		if (folderChanged) {
			execSQL("UPDATE folder set book_cnt = null WHERE id = " + getFolderId(fileInfo.path));
		}
		if (bookDateNChanged) {
			execSQL("delete from book_dates_stats WHERE date_field = 'book_date_n' and " +
					"book_date = case when " + fileInfo.bookDateN + "=0 then 0 else "+
					"cast(strftime('%s',datetime("+fileInfo.bookDateN+"/1000, 'unixepoch', 'start of month')) as integer) end");
			execSQL("insert into book_dates_stats (date_field, book_date, book_cnt) " +
					"values ('book_date_n', case when " + fileInfo.bookDateN + "=0 then 0 else "+
				"cast(strftime('%s',datetime("+fileInfo.bookDateN+"/1000, 'unixepoch', 'start of month')) as integer) end, null)");
		}
		if (docDateNChanged) {
			execSQL("delete from book_dates_stats WHERE date_field = 'doc_date_n' and " +
					"book_date = case when " + fileInfo.docDateN + "=0 then 0 else "+
					"cast(strftime('%s',datetime("+fileInfo.docDateN+"/1000, 'unixepoch', 'start of month')) as integer) end");
			execSQL("insert into book_dates_stats (date_field, book_date, book_cnt) " +
					"values ('doc_date_n', case when " + fileInfo.docDateN + "=0 then 0 else "+
					"cast(strftime('%s',datetime("+fileInfo.docDateN+"/1000, 'unixepoch', 'start of month')) as integer) end, null)");
		}
		if (publYearNChanged) {
			execSQL("delete from book_dates_stats WHERE date_field = 'publ_year_n' and " +
					"book_date = case when " + fileInfo.publYearN + "=0 then 0 else "+
					"cast(strftime('%s',datetime("+fileInfo.publYearN+"/1000, 'unixepoch', 'start of month')) as integer) end");
			execSQL("insert into book_dates_stats (date_field, book_date, book_cnt) " +
					"values ('publ_year_n', case when " + fileInfo.publYearN + "=0 then 0 else "+
					"cast(strftime('%s',datetime("+fileInfo.publYearN+"/1000, 'unixepoch', 'start of month')) as integer) end, null)");
		}
		if (fileCreateTimeChanged) {
			execSQL("delete from book_dates_stats WHERE date_field = 'file_create_time' and " +
					"book_date = case when " + fileInfo.fileCreateTime + "=0 then 0 else "+
					"cast(strftime('%s',datetime("+fileInfo.fileCreateTime+"/1000, 'unixepoch', 'start of month')) as integer) end");
			execSQL("insert into book_dates_stats (date_field, book_date, book_cnt) " +
					"values ('file_create_time', case when " + fileInfo.fileCreateTime + "=0 then 0 else "+
					"cast(strftime('%s',datetime("+fileInfo.fileCreateTime+"/1000, 'unixepoch', 'start of month')) as integer) end, null)");

		}
		if (titleChanged) {
			for (int i = 0; i < 3; i++) {
				if (StrUtils.getNonEmptyStr(fileInfo.title, false).length() > i) {
					String sText = StrUtils.getNonEmptyStr(fileInfo.title, false);
					if (sText.length()>i) {
						execSQL("delete from book_titles_stats WHERE text_field = 'book_title' and " +
								"stat_level = " + i + " and text_value = " + quoteSqlString(sText.substring(0, i + 1)));
						execSQL("insert into book_titles_stats (text_field, stat_level, text_value, book_cnt) " +
								"values ('book_title', " + i + ", " + quoteSqlString(sText.substring(0, i + 1)) + ", null)");
					}
				}
			}
		}
	}

	private boolean save(FileInfo fileInfo)	{
		boolean authorsChanged = true;
		boolean genresChanged = true;
		boolean seriesChanged = true;
		boolean titleChanged = true;
		boolean folderChanged = true;
		boolean bookDateNChanged = true;
		boolean docDateNChanged = true;
		boolean publYearNChanged = true;
		boolean fileCreateTimeChanged = true;
		try {
			FileInfo oldValue = findFileInfoByPathname(fileInfo.getPathName(), false);
			if (oldValue == null && fileInfo.id != null)
				oldValue = findFileInfoById(fileInfo.id);
			if (oldValue != null && fileInfo.id == null && oldValue.id != null)
				fileInfo.id = oldValue.id;
			if (oldValue != null) {
				// found, updating
				if (!fileInfo.equals(oldValue)) {
					vlog.d("updating file " + fileInfo.getPathName());
					beginChanges();
					QueryHelper h = new QueryHelper(fileInfo, oldValue);
					h.update(fileInfo.id);
				}
				authorsChanged = !eq(fileInfo.getAuthors(), oldValue.getAuthors());
				genresChanged = !eq(fileInfo.getGenres(), oldValue.getGenres());
				seriesChanged = (!eq(fileInfo.series,oldValue.series)) ||
						(!eq(fileInfo.publseries,oldValue.publseries));
				titleChanged = !eq(fileInfo.title,oldValue.title);
				folderChanged = !eq(fileInfo.path,oldValue.path);
				bookDateNChanged = fileInfo.bookDateN != oldValue.bookDateN;
				docDateNChanged = fileInfo.docDateN != oldValue.docDateN;;
				publYearNChanged = fileInfo.publYearN != oldValue.publYearN;
				fileCreateTimeChanged = fileInfo.fileCreateTime != oldValue.fileCreateTime;
				if (!genresChanged) {
					if ((StrUtils.isEmptyStr(fileInfo.genre_list)) &&
							(!StrUtils.isEmptyStr(fileInfo.genre))) genresChanged = true;
					if (!StrUtils.isEmptyStr(fileInfo.genre_list))
						if (!fileInfo.genre_list.equals(fileInfo.genre)) genresChanged = true;
				}
			} else {
				// inserting
				vlog.d("inserting new file " + fileInfo.getPathName());
				beginChanges();
				QueryHelper h = new QueryHelper(fileInfo, new FileInfo());
				fileInfo.id = h.insert();
				authorsChanged = true;
				genresChanged = true;
				seriesChanged = true;
				titleChanged = true;
				folderChanged = true;
				bookDateNChanged = true;
				docDateNChanged = true;
				publYearNChanged = true;
				fileCreateTimeChanged = true;
			}

			clearBookStats(fileInfo, authorsChanged, genresChanged, seriesChanged, titleChanged, folderChanged,
					bookDateNChanged, docDateNChanged, publYearNChanged, fileCreateTimeChanged);
			
			fileInfoCache.put(fileInfo);
			if (fileInfo.id != null) {
				if (authorsChanged) {
					vlog.d("updating authors for file " + fileInfo.getPathName());
					beginChanges();
					Long[] authorIds = getAuthorIds(fileInfo.getAuthors());
					saveBookAuthors(fileInfo.id, authorIds);
				}
				if (genresChanged) {
					vlog.d("updating genres for file " + fileInfo.getPathName());
					beginChanges();
					Long[] genreIds = getGenreIds(fileInfo.getGenres(),currentLanguage);
					saveBookGenres(fileInfo.id, genreIds);
				}
				return true;
			}
			return false;
		} catch (SQLiteException e) {
			log.e("error while writing to DB", e);
			return false;
		}
	}

	public void saveFileInfos(Collection<FileInfo> list)
	{
		Log.v("cr3db", "save BookInfo collection: " + list.size() + " items");
		if (!isOpened()) {
			Log.e("cr3db", "cannot save book info : DB is closed");
			return;
		}
		for (FileInfo fileInfo : list) {
			save(fileInfo);
		}
	}
	
	/**
	 * Load recent books list, with bookmarks
	 * @param maxCount is max number of recent books to get
	 * @return list of loaded books
	 */
	public ArrayList<BookInfo> loadRecentBooks(int maxCount)
	{
		ArrayList<FileInfo> list = new ArrayList<FileInfo>();
		if (!isOpened())
			return null;
		beginReading();
		findRecentBooks(list, maxCount, maxCount*10);
		ArrayList<BookInfo> res = new ArrayList<BookInfo>(list.size());
		for (FileInfo f : list) {
			FileInfo file = fileInfoCache.get(f.getPathName()); // try using cached value instead
			if (file == null) {
				file = f;
				fileInfoCache.put(file);
			}
			BookInfo item = new BookInfo(new FileInfo(file));
			loadBookmarks(item);
			res.add(item);
		}
		endReading();
		return res;
	}

	private boolean findRecentBooks(ArrayList<FileInfo> list, int maxCount, int limit)
	{
		String sql = READ_FILEINFO_SQL + " WHERE last_access_time>0 ORDER BY last_access_time DESC LIMIT " + limit;
		boolean found = false;
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				do {
					FileInfo fileInfo = new FileInfo();
					readFileInfoFromCursor(fileInfo, rs);
					if (!fileInfo.fileExists())
						continue;
					list.add(fileInfo);
					fileInfoCache.put(fileInfo);
					found = true;
					if (list.size() > maxCount)
						break;
				} while (rs.moveToNext());
			}
		}
		return found;
	}
	
	
	//=======================================================================================
    // File info access code
    //=======================================================================================
	
	public class QueryHelper {
		String tableName;
		QueryHelper(String tableName)
		{
			this.tableName = tableName;
		}
		ArrayList<String> fields = new ArrayList<String>(); 
		ArrayList<Object> values = new ArrayList<Object>();
		QueryHelper add(String fieldName, int value, int oldValue )
		{
			if (value != oldValue) {
				fields.add(fieldName);
				values.add(Long.valueOf(value));
			}
			return this;
		}
		QueryHelper add(String fieldName, Long value, Long oldValue )
		{
			if (value != null && (oldValue == null || !oldValue.equals(value))) {
				fields.add(fieldName);
				values.add(value);
			}
			return this;
		}
		QueryHelper add(String fieldName, String value, String oldValue)
		{
			String val = StrUtils.isEmptyStr(value)?"":value;
			String oldVal = StrUtils.isEmptyStr(oldValue)?"":oldValue;
			if (val != null && (oldVal == null || !oldVal.equals(val))) {
				fields.add(fieldName);
				values.add(value);
			}
			return this;
		}
		QueryHelper add(String fieldName, Double value, Double oldValue)
		{
			if (value != null && (oldValue == null || !oldValue.equals(value))) {
				fields.add(fieldName);
				values.add(value);
			}
			return this;
		}
		Long insert()
		{
			if (fields.size() == 0)
				return null;
			beginChanges();
			StringBuilder valueBuf = new StringBuilder();
			try {
				String ignoreOption = ""; //"OR IGNORE ";
				StringBuilder buf = new StringBuilder("INSERT " + ignoreOption + " INTO ");
				buf.append(tableName);
				buf.append(" (id");
				for (String field : fields) {
					buf.append(",");
					buf.append(field);
				}
				buf.append(") VALUES (NULL");
				for (@SuppressWarnings("unused") String field : fields) {
					buf.append(",");
					buf.append("?");
				}
				buf.append(")");
				String sql = buf.toString();
				Log.d("cr3db", "going to execute " + sql);
				Long id;
				try (SQLiteStatement stmt = mDB.compileStatement(sql)) {
					for (int i = 1; i <= values.size(); i++) {
						Object v = values.get(i - 1);
						valueBuf.append(v!=null ? v.toString() : "null");
						valueBuf.append(",");
						if (v == null)
							stmt.bindNull(i);
						else if (v instanceof String)
							stmt.bindString(i, (String) v);
						else if (v instanceof Long)
							stmt.bindLong(i, (Long) v);
						else if (v instanceof Double)
							stmt.bindDouble(i, (Double) v);
					}
					id = stmt.executeInsert();
					Log.d("cr3db", "added book, id=" + id + ", query=" + sql);
				}
				flushAndTransaction();
				return id;
			} catch (Exception e) {
				Log.e("cr3db", "insert failed: " + e.getMessage());
				Log.e("cr3db", "values: " + valueBuf.toString());
				return null;
			}
		}
		boolean update(Long id)
		{
			if (fields.size() == 0)
				return false;
			beginChanges();
			StringBuilder buf = new StringBuilder("UPDATE ");
			buf.append(tableName);
			buf.append(" SET ");
			boolean first = true;
			for (String field : fields) {
				if (!first)
					buf.append(",");
				buf.append(field);
				buf.append("=?");
				first = false;
			}
			buf.append(" WHERE id=" + id );
			vlog.v("executing " + buf);
			for (Object o: values) {
				vlog.v("value:"+o.toString());
			}
			mDB.execSQL(buf.toString(), values.toArray());
			flushAndTransaction();
			return true;
		}
		Long fromFormat(DocumentFormat f)
		{
			if (f == null)
				return null;
			return (long)f.ordinal();
		}
		QueryHelper(FileInfo newValue, FileInfo oldValue)
		{
			this("book");
			if (!newValue.need_to_update_ver) {
				add("pathname", newValue.getPathName(), oldValue.getPathName());
				add("folder_fk", getFolderId(newValue.path), getFolderId(oldValue.path));
				add("filename", newValue.getFilename(), oldValue.getFilename());
				add("arcname", newValue.arcname, oldValue.arcname);
				String sTitle = newValue.title;
				if (StrUtils.isEmptyStr(sTitle)) sTitle = newValue.getFilename();
				add("title", sTitle, oldValue.title);
				add("series_fk", getSeriesId(newValue.series), getSeriesId(oldValue.series));
				add("series_number", (long) newValue.seriesNumber, (long) oldValue.seriesNumber);
				add("format", fromFormat(newValue.format), fromFormat(oldValue.format));
				add("arcsize", (long) newValue.arcsize, (long) oldValue.arcsize);
				add("last_access_time", (long) newValue.lastAccessTime, (long) oldValue.lastAccessTime);
				add("create_time", (long) newValue.getCreateTime(), (long) oldValue.getCreateTime());
				add("flags", (long) newValue.flags, (long) oldValue.flags);
				add("language", newValue.language, oldValue.language);
			}
			add("filesize", (long) newValue.size, (long) oldValue.size);
			add("lang_from", newValue.lang_from, oldValue.lang_from);
			add("lang_to", newValue.lang_to, oldValue.lang_to);
			add("saved_with_ver", (long)MainDB.DB_VERSION, (long)oldValue.saved_with_ver);
			//vlog.v("QueryHelper: saved_with_ver "+(long)MainDB.DB_VERSION+"<"+(long)oldValue.saved_with_ver);
			add("genre", newValue.genre, oldValue.genre);
			add("annotation", newValue.annotation, oldValue.annotation);
			add("srclang", newValue.srclang, oldValue.srclang);
			add("bookdate", newValue.getBookdate(), oldValue.getBookdate());
			add("translator", newValue.translator, oldValue.translator);
			add("docauthor", newValue.docauthor, oldValue.docauthor);
			add("docprogram", newValue.docprogram, oldValue.docprogram);
			add("docdate", newValue.getDocdate(), oldValue.getDocdate());
			add("docsrcurl", newValue.docsrcurl, oldValue.docsrcurl);
			add("docsrcocr", newValue.docsrcocr, oldValue.docsrcocr);
			add("docversion", newValue.docversion, oldValue.docversion);
			add("publname", newValue.publname, oldValue.publname);
			add("publisher", newValue.publisher, oldValue.publisher);
			add("publcity", newValue.publcity, oldValue.publcity);
			add("publyear", newValue.getPublyear(), oldValue.getPublyear());
			add("publisbn", newValue.publisbn, oldValue.publisbn);
			add("publseries_fk", getSeriesId(newValue.publseries), getSeriesId(oldValue.publseries));
			add("publseries_number", (long) newValue.publseriesNumber, (long) oldValue.publseriesNumber);
			add("file_create_time", (long) newValue.getFileCreateTime(), (long) oldValue.getFileCreateTime());
			//vlog.v("FCD1:"+newValue.seriesNumber());
			//vlog.v("FCD2:"+oldValue.getFileCreateTime());
			add("sym_count", (long) newValue.symCount, (long) oldValue.symCount);
			add("word_count", (long) newValue.wordCount, (long) oldValue.wordCount);
			add("book_date_n", (long) newValue.bookDateN, (long) oldValue.bookDateN);
			add("doc_date_n", (long) newValue.docDateN, (long) oldValue.docDateN);
			add("publ_year_n", (long) newValue.publYearN, (long) oldValue.publYearN);
			add("opds_link", newValue.opdsLink, oldValue.opdsLink);
			//plotn: TODO: may perform
			add("crc32", newValue.crc32, oldValue.crc32);
			add("domVersion", newValue.domVersion, oldValue.domVersion);
			add("rendFlags", newValue.blockRenderingFlags, oldValue.blockRenderingFlags);
			if (fields.size() == 0)
				vlog.v("QueryHelper: no fields to update");
		}
		QueryHelper(Bookmark newValue, Bookmark oldValue, long bookId)
		{
			this("bookmark");
			add("book_fk", bookId, oldValue.getId()!=null ? bookId : null);
			add("type", newValue.getType(), oldValue.getType());
			add("percent", newValue.getPercent(), oldValue.getPercent());
			add("shortcut", newValue.getShortcut(), oldValue.getShortcut());
			add("start_pos", newValue.getStartPos(), oldValue.getStartPos());
			add("end_pos", newValue.getEndPos(), oldValue.getEndPos());
			add("title_text", newValue.getTitleText(), oldValue.getTitleText());
			add("pos_text", newValue.getPosText(), oldValue.getPosText());
			add("comment_text", newValue.getCommentText(), oldValue.getCommentText());
			add("time_stamp", newValue.getTimeStamp(), oldValue.getTimeStamp());
			add("time_elapsed", newValue.getTimeElapsed(), oldValue.getTimeElapsed());
			add("link_pos", newValue.getLinkPos(), oldValue.getLinkPos());
		}
	}

	private static final String READ_FILEINFO_FIELDS = 
		"b.id AS id, pathname, " +
		"f.name as path, " +
		"filename, arcname, title, " +
		"(SELECT GROUP_CONCAT(a.name,'|') FROM author a JOIN book_author ba ON a.id=ba.author_fk WHERE ba.book_fk=b.id) as authors, " +
		"s.name as series_name, " +
		"series_number, " +
		"format, filesize, arcsize, " +
		"create_time, last_access_time, flags, language, lang_from, lang_to, "+
		"saved_with_ver, genre, annotation, srclang, bookdate, translator, docauthor, "+
		"docprogram, docdate, docsrcurl, docsrcocr, docversion, publname, publisher, " +
		"publcity,  publyear, publisbn, "+
		"sp.name as publseries_name, " +
		"publseries_number, file_create_time, sym_count, word_count, book_date_n, doc_date_n, publ_year_n, opds_link,  " +
		"(SELECT GROUP_CONCAT(g.code,'|') FROM genre g JOIN book_genre bg ON g.id=bg.genre_fk WHERE bg.book_fk=b.id) as genre_list, " +
		"crc32, domVersion, rendFlags "
		;

	private static final String READ_FILEINFO_SQL =
		"SELECT " +
		READ_FILEINFO_FIELDS +
		"FROM book b " +
		"LEFT JOIN series s ON s.id=b.series_fk " +
		"LEFT JOIN series sp ON sp.id=b.publseries_fk " +
		"LEFT JOIN folder f ON f.id=b.folder_fk ";

	private void readFileInfoFromCursor(FileInfo fileInfo, Cursor rs) {
		int i = 0;
		fileInfo.id = rs.getLong(i++);
		String pathName = rs.getString(i++);
		String[] parts = FileInfo.splitArcName(pathName);
		fileInfo.pathname = parts[0];
		fileInfo.path = rs.getString(i++);
		fileInfo.setFilename(rs.getString(i++));
		fileInfo.arcname = rs.getString(i++);
		fileInfo.title = rs.getString(i++);
		fileInfo.setAuthors(rs.getString(i++));
		fileInfo.series = rs.getString(i++);
		fileInfo.seriesNumber = rs.getInt(i++);
		fileInfo.format = DocumentFormat.byId(rs.getInt(i++));
		fileInfo.size = rs.getInt(i++);
		fileInfo.arcsize = rs.getInt(i++);
		fileInfo.setCreateTime(rs.getLong(i++));
		fileInfo.lastAccessTime = rs.getLong(i++);
		fileInfo.flags = rs.getInt(i++);
	    fileInfo.language = StrUtils.getNonEmptyStr(rs.getString(i++),false).toLowerCase();
		fileInfo.lang_from = rs.getString(i++);
		fileInfo.lang_to = rs.getString(i++);
		fileInfo.saved_with_ver = rs.getInt(i++);
		fileInfo.genre = rs.getString(i++);
		fileInfo.annotation = rs.getString(i++);
		fileInfo.srclang = rs.getString(i++);
		fileInfo.setBookdate(rs.getString(i++));
		fileInfo.translator = rs.getString(i++);
		fileInfo.docauthor = rs.getString(i++);
		fileInfo.docprogram = rs.getString(i++);
		fileInfo.setDocdate(rs.getString(i++));
		fileInfo.docsrcurl = rs.getString(i++);
		fileInfo.docsrcocr = rs.getString(i++);
		fileInfo.docversion = rs.getString(i++);
		fileInfo.publname = rs.getString(i++);
		fileInfo.publisher = rs.getString(i++);
		fileInfo.publcity = rs.getString(i++);
		fileInfo.setPublyear(rs.getString(i++));
		fileInfo.publisbn = rs.getString(i++);
		fileInfo.publseries = rs.getString(i++);
		fileInfo.publseriesNumber = rs.getInt(i++);
		fileInfo.setFileCreateTime(rs.getLong(i++));
		fileInfo.symCount = rs.getLong(i++);
		fileInfo.wordCount = rs.getLong(i++);
		fileInfo.bookDateN = rs.getLong(i++);
		fileInfo.docDateN = rs.getLong(i++);
		fileInfo.publYearN = rs.getLong(i++);
		fileInfo.opdsLink = rs.getString(i++);
		fileInfo.isArchive = fileInfo.arcname!=null;
		fileInfo.genre_list = rs.getString(i++);
		fileInfo.crc32 = rs.getInt(i++);
		fileInfo.domVersion = rs.getInt(i++);
		fileInfo.blockRenderingFlags = rs.getInt(i++);
		fileInfo.isArchive = fileInfo.arcname != null;
	}

	private boolean findBooks(String sql, ArrayList<FileInfo> list) {
		boolean found = false;
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				do {
					FileInfo fileInfo = new FileInfo();
					readFileInfoFromCursor(fileInfo, rs);
					if (!fileInfo.fileExists())
						continue;
					fileInfoCache.put(fileInfo);
					list.add(new FileInfo(fileInfo));
					found = true;
				} while (rs.moveToNext());
			}
		}
		return found;
	}

	private String findSeries(int maxCount, String seriesPattern) {
		StringBuilder buf = new StringBuilder();
		String sql = "SELECT id, name FROM series";
		int count = 0;
		try (Cursor rs = mDB.rawQuery(sql, null))  {
			if (rs.moveToFirst()) {
				do {
					long id = rs.getLong(0);
					String name = rs.getString(1);
					if (Utils.matchPattern(name, seriesPattern)) {
						if (buf.length() != 0)
							buf.append(",");
						buf.append(id);
						count++;
						if (count >= maxCount)
							break;
					}
				} while (rs.moveToNext());
			}
		}
		return buf.toString();
	}

	private boolean matchPatt(String sField, String  searchText) {
		return (Utils.matchPattern(sField, searchText))||
				(StrUtils.getNonEmptyStr(sField, true).toLowerCase().contains(
						StrUtils.getNonEmptyStr(searchText,true).toLowerCase()))||
				(StrUtils.getNonEmptyStr(sField, true).toLowerCase().matches(
						StrUtils.getNonEmptyStr(searchText,true).toLowerCase()))||
				(StrUtils.getNonEmptyStr(sField, true).toLowerCase().matches(
						".*"+StrUtils.getNonEmptyStr(searchText,true).toLowerCase()+".*"))||
				(StrUtils.getNonEmptyStr(sField, true).contains(StrUtils.getNonEmptyStr(searchText,true)))||
				(StrUtils.getNonEmptyStr(sField, true).matches(StrUtils.getNonEmptyStr(searchText,true)))||
				(StrUtils.getNonEmptyStr(sField, true).matches(".*"+StrUtils.getNonEmptyStr(searchText,true)+".*"));
	}
	
	public ArrayList<FileInfo> findByPatterns(int maxCount, String author, String title, String series, String filename)
	{
		Log.i("cr3","findByPatterns fired");
		beginReading();
		boolean bQuickSearch = StrUtils.getNonEmptyStr(title,true).equals("##QUICK_SEARCH##");
		Map<String, String> alGenres = new HashMap<>();
		if (bQuickSearch) {
			alGenres = getGenres();
		}
		String sDefCond = bQuickSearch ? " OR ":" AND ";
		ArrayList<FileInfo> list = new ArrayList<>();
		String searchText =  StrUtils.getNonEmptyStr(author, true);
		String authorSearch =  StrUtils.getNonEmptyStr(bQuickSearch ? author : author, true);
		String titleSearch = StrUtils.getNonEmptyStr(bQuickSearch ? author : title, true);
		String seriesSearch = StrUtils.getNonEmptyStr(bQuickSearch ? author : series, true);
		String filenameSearch = StrUtils.getNonEmptyStr(bQuickSearch ? author : filename, true);
		
		StringBuilder buf = new StringBuilder();
		boolean hasCondition = false;
		if (!StrUtils.isEmptyStr(authorSearch)) {
			String authorIds = findAuthors(maxCount, authorSearch);
			if ((authorIds == null || authorIds.length() == 0) && (!bQuickSearch))
				return list;
			if (!StrUtils.isEmptyStr(authorIds)) {
				if (buf.length() > 0)
					buf.append(sDefCond);
				buf.append(" ( b.id IN (SELECT ba.book_fk FROM book_author ba WHERE ba.author_fk IN (").append(authorIds).append(")) ) ");
				hasCondition = true;
			}
		}
		if (!StrUtils.isEmptyStr(seriesSearch)) {
			String seriesIds = findSeries(maxCount, seriesSearch);
			if ((seriesIds == null || seriesIds.length() == 0) && (!bQuickSearch))
				return list;
			if (!StrUtils.isEmptyStr(seriesIds)) {
				if (buf.length() > 0)
					buf.append(sDefCond);
				buf.append(" ( ");
				buf.append(" (b.series_fk IN (").append(seriesIds).append(")) ");
				buf.append(" OR ");
				buf.append(" (b.publseries_fk IN (").append(seriesIds).append(")) ");
				buf.append(" ) ");
				hasCondition = true;
			}
		}
		if (!StrUtils.isEmptyStr(titleSearch)) {
			hasCondition = true;
		}
		if (!StrUtils.isEmptyStr(filenameSearch)) {
			hasCondition = true;
		}
		if (bQuickSearch) {
			hasCondition = true;
			if (buf.length() > 0)
				buf.append(sDefCond);
			buf.append(" (1=1) ");
		}
		if (!hasCondition) return list;
		String condition = buf.length()==0 ? " WHERE (not (b.pathname like '@opds%'))" :
				" WHERE (" + buf.toString() + ")  and (not (b.pathname like '@opds%'))";
		String sql = READ_FILEINFO_SQL + condition + " ORDER BY file_create_time desc";
		Log.d("cr3", "sql: " + sql );
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				int count = 0;
				do {
					boolean bDidntFindAll = false;
					int matchesCnt = 0;
					if (!StrUtils.isEmptyStr(titleSearch)) {
						if (!Utils.matchPattern(rs.getString(5), titleSearch))
							bDidntFindAll = true;
						else
							matchesCnt++;
					}
					if (!StrUtils.isEmptyStr(filenameSearch)) {
						if (!Utils.matchPattern(rs.getString(3), filenameSearch))
							bDidntFindAll = true;
						else
							matchesCnt++;
					}
					if (bQuickSearch) {
						int i=0;
						for (String s: READ_FILEINFO_FIELDS.split(", ")) {
							String sField = rs.getString(i);
							if (s.contains("book_date_n")||
							   s.contains("doc_date_n")||
							   s.contains("publ_year_n")||
							   s.contains("file_create_time")||
							   s.contains("create_time")||
							   s.contains("last_access_time")
							) {
								final long unixTime = rs.getLong(i);
								java.util.Date dateTime=new java.util.Date((long)unixTime);
								android.text.format.DateFormat df = new android.text.format.DateFormat();
								sField = df.format("yyyy-MM-dd hh:mm:ss a", dateTime).toString();
							}
							if (matchPatt(sField, searchText)) {
								matchesCnt++;
							}
							if ((s.contains("genre_list")) && (!StrUtils.isEmptyStr(sField))) {
								for (String gnr: sField.split("\\|")) {
									if (!StrUtils.isEmptyStr(gnr)) {
										String gnrE = alGenres.get(gnr);
										if (!StrUtils.isEmptyStr(gnrE)) {
											if (matchPatt(gnrE, searchText)) {
												matchesCnt++;
											}
										}
									}
								}
							}
							i++;
						}
					}
					if ((bDidntFindAll)&&(!bQuickSearch)) continue;
					if ((matchesCnt==0)&&(bQuickSearch)) continue;
					FileInfo fi = new FileInfo(); 
					readFileInfoFromCursor(fi, rs);
					list.add(fi);
					fileInfoCache.put(fi);
					count++;
				} while (count < maxCount && rs.moveToNext());
			}
		}
		endReading();
		return list;
	}

	/*
	public ArrayList<FileInfo> findByFingerprint (int maxCount, String filename, int crc32)
	{
		// TODO: replace crc32 with sha512, remove filename as search criteria

		beginReading();
		ArrayList<FileInfo> list = new ArrayList<>();

		String condition = " WHERE b.crc32=" + crc32;
		String sql = READ_FILEINFO_SQL + condition;
		Log.d("cr3", "sql: " + sql );
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				int count = 0;
				do {
					if (filename != null && filename.length() > 0)
						if (!Utils.matchPattern(rs.getString(3), filename))
							continue;
					FileInfo fi = new FileInfo();
					readFileInfoFromCursor(fi, rs);
					list.add(fi);
					fileInfoCache.put(fi);
					count++;
				} while (count < maxCount && rs.moveToNext());
			}
		}
		endReading();
		return list;
	}
	*/

	public ArrayList<FileInfo> loadFileInfos(ArrayList<String> pathNames) {
		ArrayList<FileInfo> list = new ArrayList<FileInfo>();
		if (!isOpened())
			return list;
		try {
			beginReading();
			for (String path : pathNames) {
				FileInfo file = findFileInfoByPathname(path, true);
				if (file != null)
					list.add(new FileInfo(file));
			}
			endReading();
		} catch (Exception e) {
			log.e("Exception while loading books from DB", e);
		}
		return list;
	}

	public void deleteRecentPosition(FileInfo fileInfo) {
		Long bookId = getBookId(fileInfo);
		if (bookId == null)
			return;
		execSQLIgnoreErrors("DELETE FROM bookmark WHERE book_fk=" + bookId + " AND type=0");
		execSQLIgnoreErrors("UPDATE book SET last_access_time=0 WHERE id=" + bookId);
		flushAndTransaction();
	}
	
	public void deleteBookmark(Bookmark bm) {
		if (bm.getId() == null)
			return;
		execSQLIgnoreErrors("DELETE FROM bookmark WHERE id=" + bm.getId());
		flushAndTransaction();
	}

	public BookInfo loadBookInfo(FileInfo fileInfo) {
		if (!isOpened())
			return null;
		try {
			FileInfo cached = fileInfoCache.get(fileInfo.getPathName());
			if (cached != null) {
				BookInfo book = new BookInfo(new FileInfo(cached));
				loadBookmarks(book);
				return book;
			}
			if (loadByPathname(fileInfo)) {
				BookInfo book = new BookInfo(new FileInfo(fileInfo));
				loadBookmarks(book);
				return book;
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}
	
	public FileInfo loadFileInfo(String pathName) {
		if (!isOpened())
			return null;
		try {
			FileInfo cached = fileInfoCache.get(pathName);
			if (cached != null) {
				return new FileInfo(cached);
			}
			FileInfo fileInfo = new FileInfo(pathName);
			if (loadByPathname(fileInfo)) {
				fileInfoCache.put(fileInfo);
				return new FileInfo(fileInfo);
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public FileInfo loadFileInfoByOPDSLink(String opdsLink) {
		if (!isOpened())
			return null;
		try {
			FileInfo cached = fileInfoCache.getByOPDSLink(opdsLink);
			if (cached != null) {
				return new FileInfo(cached);
			}
			FileInfo fileInfo = new FileInfo();
			if (loadByOPDSLink(fileInfo, opdsLink)) {
				fileInfoCache.put(fileInfo);
				return new FileInfo(fileInfo);
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}
	
	private boolean loadByPathname(FileInfo fileInfo) {
		if (findBy(fileInfo, "pathname", fileInfo.getPathName())) {
			fileInfoCache.put(fileInfo);
			return true;
		}
		
		FileInfo moved = findMovedFileInfo(fileInfo.getPathName());
		if (moved != null) {
			fileInfo.assign(moved);
			return true;
		}
		
		return false;
	}

	private boolean loadByOPDSLink(FileInfo fileInfo, String opdsLink) {
		if (findBy(fileInfo, "opds_link", opdsLink)) {
			fileInfoCache.put(fileInfo);
			return true;
		}

		return false;
	}

//	private boolean loadById( FileInfo fileInfo ) {
//		return findBy(fileInfo, "b.id", fileInfo.id);
//	}

	private Long getBookId(FileInfo fileInfo) {
		Long bookId = null;
		if (fileInfo == null)
			return bookId;
		String pathName = fileInfo.getPathName();
		FileInfo cached = fileInfoCache.get(pathName);
		if (cached != null) {
			bookId = cached.id;
		}
		if (bookId == null)
			bookId = fileInfo.id;
		if (bookId == null)
			loadByPathname(fileInfo);
		return bookId;
	}
	public Long deleteBook(FileInfo fileInfo)
	{
		if (fileInfo == null)
			return null;
		Long bookId = getBookId(fileInfo);
		fileInfoCache.remove(fileInfo);
		if (bookId == null)
			return null;
		execSQLIgnoreErrors("DELETE FROM bookmark WHERE book_fk=" + bookId);
		execSQLIgnoreErrors("DELETE FROM book WHERE id=" + bookId);
		return bookId;
	}
	
	public void correctFilePaths() {
		if (mDB == null) return;
		Log.i("cr3", "checking data for path correction");
		beginReading();
		int rowCount = 0;
		Map<String, Long> map = new HashMap<>();
		String sql = "SELECT id, pathname FROM book";
		try (Cursor rs = mDB.rawQuery(sql, null)) {
			if (rs.moveToFirst()) {
				// read DB
				do {
					Long id = rs.getLong(0);
					String pathname = rs.getString(1);
					String corrected = pathCorrector.normalize(pathname);
					if (pathname == null)
						continue;
					rowCount++;
					if (corrected == null) {
						Log.w("cr3", "DB contains unknown path " + pathname);
					} else if (!pathname.equals(corrected)) {
						map.put(pathname, id);
					}
				} while (rs.moveToNext());
			}
		} catch (Exception e) {
			Log.e("cr3", "exception while loading list books to correct paths", e);
		}
		Log.i("cr3", "Total rows: " + rowCount + ", " + (map.size() > 0 ? "need to correct " + map.size() + " items" : "no corrections required"));
		if (map.size() > 0) {
			beginChanges();
			int count = 0;
			for (Map.Entry<String, Long> entry : map.entrySet()) {
				String pathname = entry.getKey();
				String corrected = pathCorrector.normalize(pathname);
				if (corrected != null && !corrected.equals(pathname)) {
					count++;
					execSQLIgnoreErrors("update book set pathname=" + quoteSqlString(corrected) + " WHERE id=" + entry.getValue());
				}
			}
			flush();
			log.i("Finished. Rows corrected: " + count);
		}
	}
	
	private MountPathCorrector pathCorrector;

	public void setPathCorrector(MountPathCorrector corrector) {
		this.pathCorrector = corrector;
		if (pathCorrectionRequired) {
			correctFilePaths();
			pathCorrectionRequired = false;
		}
	}

	public int saveAuthorsAliasesInfo(ArrayList<AuthorAlias> list) {
		if (list == null || list.size() == 0)
			return 0;
		int i = 0;
		execSQL("DELETE FROM author_aliases");
		for (AuthorAlias al: list) {
			execSQLIgnoreErrorsNoFlush("insert into author_aliases (orig_text, alias_text) values (" +
					quoteSqlString(al.origText) + ", " + quoteSqlString(al.aliasText) + ")");
			i++;
		}
		flushAndTransaction();
		return i;
	}

	public void deleteAuthorsAliasesInfo() {
		execSQL("DELETE FROM author_aliases");
	}

	public void saveAuthorAliasInfo(AuthorAlias al) {
		execSQLIgnoreErrorsNoFlush("insert into author_aliases (orig_text, alias_text) values (" +
					quoteSqlString(al.origText) + ", " + quoteSqlString(al.aliasText) + ")");
	}

}
