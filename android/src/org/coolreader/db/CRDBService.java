package org.coolreader.db;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.coolreader.CoolReader;
import org.coolreader.crengine.*;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.library.AuthorAlias;
import org.coolreader.userdic.UserDicDlg;
import org.coolreader.userdic.UserDicEntry;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class CRDBService extends BaseService {
	public static final Logger log = L.create("db");
	public static final Logger vlog = L.create("db", Log.ASSERT);

	public MainDB getMainDB() {
		return mainDB;
	}

	public CoverDB getCoverDB() {
		return coverDB;
	}

	private MainDB mainDB = new MainDB();
    private CoverDB coverDB = new CoverDB();

	public CRDBService() {
		super("crdb");
	}
	
    @Override
    public void onCreate() {
    	log.i("onCreate()");
		super.onCreate();
    	execTask(new OpenDatabaseTask());
    }

    public void reopenDatabase() {
		execTask(new ReOpenDatabaseTask());
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.i("Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	log.i("onDestroy()");
    	execTask(new CloseDatabaseTask());
		super.onDestroy();
    }

    private File getDatabaseDir() {
		if ((Build.VERSION.SDK_INT >= 23) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.R))  {
			if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				return getFilesDir();
			}
		}
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R))  {
			if (!Environment.isExternalStorageManager()) {
				return getFilesDir();
			}
		}
    	//File storage = Environment.getExternalStorageDirectory();
    	File storage = DeviceInfo.EINK_NOOK ? new File("/media/") : Environment.getExternalStorageDirectory();
    	//File cr3dir = new File(storage, ".cr3");
		File cr3dir = new File(storage, "cr3e");
		boolean dExists = (cr3dir.isDirectory()) && (cr3dir.exists());
		if (!dExists) cr3dir = new File(storage, "KnownReader");
		if (cr3dir.isDirectory())
    		cr3dir.mkdirs();
    	if (!cr3dir.isDirectory() || !cr3dir.canWrite()) {
	    	log.w("Cannot use " + cr3dir + " for writing database, will use data directory instead");
	    	log.w("getFilesDir=" + getFilesDir() + " getDataDirectory=" + Environment.getDataDirectory());
    		cr3dir = getFilesDir(); //Environment.getDataDirectory();
    	}
    	log.i("DB directory: " + cr3dir);
    	return cr3dir;
    }
    
    private class OpenDatabaseTask extends Task {
    	public OpenDatabaseTask() {
    		super("OpenDatabaseTask");
    	}
    	
		@Override
		public void work() {
	    	open();
		}

		private boolean open() {
	    	File dir = getDatabaseDir();
	    	boolean res = mainDB.open(dir);
	    	res = coverDB.open(dir) && res;
	    	if (!res) {
	    		mainDB.close();
	    		coverDB.close();
	    	}
	    	return res;
	    }
	    
    }

    private class CloseDatabaseTask extends Task {
    	public CloseDatabaseTask() {
    		super("CloseDatabaseTask");
    	}

    	@Override
		public void work() {
	    	close();
		}

		private void close() {
			clearCaches();
    		mainDB.close();
    		coverDB.close();
	    }
    }

	private class ReOpenDatabaseTask extends Task {
		public ReOpenDatabaseTask() {
			super("ReOpenDatabaseTask");
		}

		@Override
		public void work() {
			close();
			open();
		}

		private boolean open() {
			File dir = getDatabaseDir();
			boolean res = mainDB.open(dir);
			res = coverDB.open(dir) && res;
			if (!res) {
				mainDB.close();
				coverDB.close();
			}
			return res;
		}

		private void close() {
			clearCaches();
			mainDB.close();
			coverDB.close();
		}
	}

	private FlushDatabaseTask lastFlushTask;
    private class FlushDatabaseTask extends Task {
    	private boolean force;
    	public FlushDatabaseTask(boolean force) {
    		super("FlushDatabaseTask");
    		this.force = force;
    		lastFlushTask = this;
    	}
		@Override
		public void work() {
			long elapsed = Utils.timeInterval(lastFlushTime);
			if (force || (lastFlushTask == this && elapsed > MIN_FLUSH_INTERVAL)) {
		    	mainDB.flush();
		    	coverDB.flush();
		    	if (!force)
		    		lastFlushTime = Utils.timeStamp();
			}
		}
    }
    
    public void clearCaches() {
		mainDB.clearCaches();
		coverDB.clearCaches();
    }

    private static final long MIN_FLUSH_INTERVAL = 30000; // 30 seconds
    private long lastFlushTime;
    
    /**
     * Schedule flush.
     */
    private void flush() {
   		execTask(new FlushDatabaseTask(false), MIN_FLUSH_INTERVAL);
    }

    /**
     * Flush ASAP.
     */
    private void forceFlush() {
   		execTask(new FlushDatabaseTask(true));
    }

    public static class FileInfoCache {
    	private ArrayList<FileInfo> list = new ArrayList<FileInfo>();
    	public void add(FileInfo item) {
    		list.add(item);
    	}
    	public void clear() {
    		list.clear();
    	}
    }

	public interface SearchHistoryLoadingCallback {
		void onSearchHistoryLoaded(ArrayList<String> searches);
	}

	public interface TagsLoadingCallback {
		void onTagsLoaded(ArrayList<BookTag> tags);
	}

	public interface UserDicLoadingCallback {
		void onUserDicLoaded(HashMap<String, UserDicEntry> dic);
	}

	public interface DicSearchHistoryLoadingCallback {
		void onDicSearchHistoryLoaded(List<DicSearchHistoryEntry> dic);
	}

	//=======================================================================================
    // OPDS and Calibre catalogs access code
    //=======================================================================================
    public interface OPDSCatalogsLoadingCallback {
    	void onOPDSCatalogsLoaded(ArrayList<FileInfo> catalogs);
    }
    
	public void saveOPDSCatalog(final Long id, final String url, final String name,
								final String username, final String password,
								final String proxy_addr, final String proxy_port,
								final String proxy_uname, final String proxy_passw,
								final int onion_def_proxy
								) {
		execTask(new Task("saveOPDSCatalog") {
			@Override
			public void work() {
				mainDB.saveOPDSCatalog(id, url, name, username, password,
						proxy_addr, proxy_port,
						proxy_uname, proxy_passw,
				  		onion_def_proxy);
			}
		});
	}

	public void saveCalibreCatalog(final Long id, final String name,
								final boolean isLocal,
								final String localFolder,
								final String remoteFolderYD
	) {
		execTask(new Task("saveCalibreCatalog") {
			@Override
			public void work() {
				mainDB.saveCalibreCatalog(id, name, isLocal,
						localFolder, remoteFolderYD);
			}
		});
	}

	public void updateCalibreCatalog(final String name,
								   final boolean isLocal,
								   final String localFolder,
								   final String remoteFolderYD,
								   final boolean onlyMax
	) {
    	execTask(new Task("saveCalibreCatalog") {
			@Override
			public void work() {
				mainDB.updateCalibreCatalog(name, isLocal,
						localFolder, remoteFolderYD, onlyMax);
			}
		});
	}

	//=======================================================================================
	// StarDict dics access code
	//=======================================================================================
	public void convertStartDictDic(String dicPath, String dicName,
									final Scanner.ScanControl control, final Engine.ProgressControl progress,
									final ObjectCallback callback, final Handler handler) {
		execTask(new Task("convertStartDictDic") {
			@Override
			public void work() {
				String doneS = mainDB.convertStartDictDic(dicPath, dicName, control, progress);
				sendTask(handler, () -> callback.onObjectLoaded(doneS));
			}
		});
	}

	public void findInOfflineDictDic(String searchStr, String langFrom, String langTo,
					 boolean extended, final ObjectCallback callback, final Handler handler) {
		execTask(new Task("findInOfflineDictDic") {
			@Override
			public void work() {
				DicStruct ds = mainDB.findInOfflineDictDic(searchStr, langFrom, langTo, extended);
				sendTask(handler, () -> callback.onObjectLoaded(ds));
			}
		});
	}

	public void closeAllDics(final ObjectCallback callback, final Handler handler) {
		execTask(new Task("closeAllDics") {
			@Override
			public void work() {
				mainDB.closeAllDics();
				sendTask(handler, () -> callback.onObjectLoaded(""));
			}
		});
	}

	//=======================================================================================

	public void saveSearchHistory(final BookInfo book, final String sHist) {
		execTask(new Task("saveSearchHistory") {
			@Override
			public void work() {
				mainDB.saveSearchHistory(book, sHist);
			}
		});
	}

	public void saveUserDic(final UserDicEntry ude, final int action) {
		execTask(new Task("saveUserDic") {
			@Override
			public void work() {
				mainDB.saveUserDic(ude, action);
			}
		});
	}

	public void updateDicSearchHistory(final DicSearchHistoryEntry dshe, final int action) {
		execTask(new Task("updateDicSearchHistory") {
			@Override
			public void work() {
				mainDB.updateDicSearchHistory(dshe, action);
			}
		});
	}

	public void clearSearchHistory(final BookInfo book) {
		execTask(new Task("clearSearchHistory") {
			@Override
			public void work() {
				mainDB.clearSearchHistory(book);
			}
		});
	}
	
	public void updateOPDSCatalog(final String url, final String field, final String value) {
		execTask(new Task("saveOPDSCatalog") {
			@Override
			public void work() {
				mainDB.updateOPDSCatalog(url, field, value);
			}
		});
	}	

	public void loadOPDSCatalogs(final OPDSCatalogsLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadOPDSCatalogs") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<FileInfo>(); 
				mainDB.loadOPDSCatalogs(list);
				sendTask(handler, () -> callback.onOPDSCatalogsLoaded(list));
			}
		});
	}

	public String getDBPath() {
    	return mainDB.mDB.getPath();
	}

	public void loadSearchHistory(final BookInfo book, final SearchHistoryLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadSearchHistory") {
			@Override
			public void work() {
				final ArrayList<String> list = mainDB.loadSearchHistory(book);
				sendTask(handler, () -> callback.onSearchHistoryLoaded(list));
			}
		});
	}

	public void loadUserDic(final UserDicLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadUserDic") {
			@Override
			public void work() {
				final HashMap<String, UserDicEntry> list = mainDB.loadUserDic();
				sendTask(handler, () -> callback.onUserDicLoaded(list));
			}
		});
	}

	public void loadDicSearchHistory(final DicSearchHistoryLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadDicSearchHistory") {
			@Override
			public void work() {
				final List<DicSearchHistoryEntry> list = mainDB.loadDicSearchHistory();
				sendTask(handler, () -> callback.onDicSearchHistoryLoaded(list));
			}
		});
	}

	public void removeOPDSCatalog(final Long id) {
		execTask(new Task("removeOPDSCatalog") {
			@Override
			public void work() {
				mainDB.removeOPDSCatalog(id);
			}
		});
	}

	public void removeCalibreCatalog(final Long id) {
		execTask(new Task("removeCalibreCatalog") {
			@Override
			public void work() {
				mainDB.removeCalibreCatalog(id);
			}
		});
	}

	//=======================================================================================
    // coverpage DB access code
    //=======================================================================================
    public interface CoverpageLoadingCallback {
    	void onCoverpageLoaded(FileInfo fileInfo, byte[] data);
    }

	public void saveBookCoverpage(final FileInfo fileInfo, final byte[] data) {
		if (data == null)
			return;
		execTask(new Task("saveBookCoverpage") {
			@Override
			public void work() {
				coverDB.saveBookCoverpage(fileInfo.getPathName(), data);
			}
		});
		flush();
	}
	
	public void loadBookCoverpage(final FileInfo fileInfo, final CoverpageLoadingCallback callback, final Handler handler) 
	{
		execTask(new Task("loadBookCoverpage") {
			@Override
			public void work() {
				final byte[] data = coverDB.loadBookCoverpage(fileInfo.getPathName());
				sendTask(handler, () -> callback.onCoverpageLoaded(fileInfo, data));
			}
		});
	}
	
	public void deleteCoverpage(final String bookId) {
		execTask(new Task("deleteCoverpage") {
			@Override
			public void work() {
				coverDB.deleteCoverpage(bookId);
			}
		});
		flush();
	}

	//=======================================================================================
    // Item groups access code
    //=======================================================================================
    public interface ItemGroupsLoadingCallback {
    	void onItemGroupsLoaded(FileInfo parent);
    }

    public interface FileInfoLoadingCallback {
		void onFileInfoListLoadBegin(String prefix);
    	void onFileInfoListLoaded(ArrayList<FileInfo> list, String prefix);
    }

    public interface CalendarStatsLoadingCallback {
		void onCalendarStatsListLoaded(ArrayList<CalendarStats> list);
	}

	public interface ObjectCallback {
		void onObjectLoaded(Object o);
	}

	public interface FileInfo1LoadingCallback {
		void onFileInfoListLoadBegin();
		void onFileInfoLoaded(FileInfo fi);
	}
    
    public interface RecentBooksLoadingCallback {
		void onRecentBooksListLoadBegin();
    	void onRecentBooksListLoaded(ArrayList<BookInfo> bookList);
    }
    
    public interface BookInfoLoadingCallback {
    	void onBooksInfoLoaded(BookInfo bookInfo);
    }
    
    public interface BookSearchCallback {
		void onBooksSearchBegin();
    	void onBooksFound(ArrayList<FileInfo> fileList);
    }
    
	public void loadAuthorsList(FileInfo parent, String filterSeries, boolean withAliases, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent); 
		execTask(new Task("loadAuthorsList") {
			@Override
			public void work() {
				mainDB.loadAuthorsList(p, filterSeries, withAliases);
				sendTask(handler, () -> callback.onItemGroupsLoaded(p));
			}
		});
	}

	public void loadCalibreAuthorsList(FileInfo parent, String filterSeries, boolean withAliases,
									   final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent);
		execTask(new Task("loadCalibreAuthorsList") {
			@Override
			public void work() {
				mainDB.loadCalibreAuthorsList(p, filterSeries, withAliases);
				sendTask(handler, () -> callback.onItemGroupsLoaded(p));
			}
		});
	}

//  CR implementation
//	public void loadGenresList(FileInfo parent, boolean showEmptyGenres, final ItemGroupsLoadingCallback callback, final Handler handler) {
//		final FileInfo p = new FileInfo(parent);
//		execTask(new Task("loadGenresList") {
//			@Override
//			public void work() {
//				mainDB.loadGenresList(p, showEmptyGenres);
//				sendTask(handler, () -> callback.onItemGroupsLoaded(p));
//			}
//		});
//	}

	//KR implementation
	public void loadGenresList(FileInfo parent, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent);
		execTask(new Task("loadGenresList") {
			@Override
			public void work() {
				mainDB.loadGenresList(p);
				sendTask(handler, () -> callback.onItemGroupsLoaded(p));
			}
		});
	}

	public void loadSeriesList(FileInfo parent, String filterAuthor, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent); 
		execTask(new Task("loadSeriesList") {
			@Override
			public void work() {
				mainDB.loadSeriesList(p, filterAuthor);
				sendTask(handler, () -> callback.onItemGroupsLoaded(p));
			}
		});
	}

    public void loadByDateList(FileInfo parent, final String field, final ItemGroupsLoadingCallback callback, final Handler handler) {
        final FileInfo p = new FileInfo(parent);
        execTask(new Task("loadByDateList") {
            @Override
            public void work() {
                mainDB.loadByDateList(p, field);
                sendTask(handler, () -> callback.onItemGroupsLoaded(p));
            }
        });
    }
	
	public void loadTitleList(FileInfo parent, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent); 
		execTask(new Task("loadTitleList") {
			@Override
			public void work() {
				mainDB.loadTitleList(p);
				sendTask(handler, () -> callback.onItemGroupsLoaded(p));
			}
		});
	}

	public void findAuthorBooks(final long authorId, final String addFilter, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findAuthorBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<>();
				mainDB.findAuthorBooks(list, authorId, addFilter);
				sendTask(handler, () -> callback.onFileInfoListLoaded(list, FileInfo.AUTHOR_GROUP_PREFIX));
			}
		});
	}

	public void findCalibreAuthorBooks(final long authorId, final String addFilter, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findCalibreAuthorBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<>();
				mainDB.findCalibreAuthorBooks(list, authorId, addFilter);
				sendTask(handler, () -> callback.onFileInfoListLoaded(list, FileInfo.CALIBRE_AUTHOR_GROUP_PREFIX));
			}
		});
	}

	//  CR implementation
//	public void findGenresBooks(final String genreCode, boolean showEmptyGenres, final FileInfoLoadingCallback callback, final Handler handler) {
//		execTask(new Task("findGenresBooks") {
//			@Override
//			public void work() {
//				final ArrayList<FileInfo> list = mainDB.findByGenre(genreCode, showEmptyGenres);
//				sendTask(handler, () -> callback.onFileInfoListLoaded(list));
//			}
//		});
//	}

	// KR implementation
	public void findGenreBooks(final long genreId, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findGenreBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<>();
				mainDB.findGenreBooks(list, genreId);
				sendTask(handler, () -> callback.onFileInfoListLoaded(list, FileInfo.GENRE_GROUP_PREFIX));
			}
		});
	}
	
	public void findSeriesBooks(final long seriesId, final String addFilter, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findSeriesBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<>();
				mainDB.findSeriesBooks(list, seriesId, addFilter);
				sendTask(handler, () -> callback.onFileInfoListLoaded(list, FileInfo.SERIES_GROUP_PREFIX));
			}
		});
	}

	public void findByDateBooks(final long bookdateId, final String field, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findByDateBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<>();
				mainDB.findByDateBooks(list, field, bookdateId);
				sendTask(handler, () -> {
					String fld = "";
					if (field.equals("book_date_n")) fld = FileInfo.BOOK_DATE_GROUP_PREFIX;
					if (field.equals("doc_date_n")) fld = FileInfo.DOC_DATE_GROUP_PREFIX;
					if (field.equals("publ_year_n")) fld = FileInfo.PUBL_YEAR_GROUP_PREFIX;
					if (field.equals("file_create_time")) fld = FileInfo.FILE_DATE_GROUP_PREFIX;
					callback.onFileInfoListLoaded(list, fld);
				});
			}
		});
	}

	public void findBooksByRating(final int minRate, final int maxRate, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findBooksByRating") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<>();
				mainDB.findBooksByRating(list, minRate, maxRate);
				sendTask(handler, () -> callback.onFileInfoListLoaded(list, FileInfo.RATING_TAG));
			}
		});
	}

	public void findBooksByState(final int state, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findBooksByState") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<>();
				mainDB.findBooksByState(list, state);
				sendTask(handler, () -> callback.onFileInfoListLoaded(list, FileInfo.STATE_TAG));
			}
		});
	}

	public void loadRecentBooks(final int maxCount, final RecentBooksLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadRecentBooks") {
			@Override
			public void work() {
				final ArrayList<BookInfo> list = mainDB.loadRecentBooks(maxCount);
				sendTask(handler, () -> callback.onRecentBooksListLoaded(list));
			}
		});
	}
	
	public void sync(final Runnable callback, final Handler handler) {
		execTask(new Task("sync") {
			@Override
			public void work() {
				sendTask(handler, () -> callback.run());
			}
		});
	}
	
	public void findByPatterns(final int maxCount, final String authors, final String title, final String series, final String filename, final BookSearchCallback callback, final Handler handler) {
		execTask(new Task("findByPatterns") {
			@Override
			public void work() {
				callback.onBooksSearchBegin();
				final ArrayList<FileInfo> list = mainDB.findByPatterns(maxCount, authors, title, series, filename);
				sendTask(handler, () -> callback.onBooksFound(list));
			}
		});
	}

	public void findByFingerprints(final int maxCount, Collection<String> fingerprints, final BookSearchCallback callback, final Handler handler) {
		execTask(new Task("findByFingerprint") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = mainDB.findByFingerprints(maxCount, fingerprints);
				sendTask(handler, () -> callback.onBooksFound(list));
			}
		});
	}

	private ArrayList<FileInfo> deepCopyFileInfos(final Collection<FileInfo> src) {
		final ArrayList<FileInfo> list = new ArrayList<>(src.size());
		for (FileInfo fi : src)
			list.add(new FileInfo(fi));
		return list;
	}
	
	public void saveFileInfos(final Collection<FileInfo> list) {
		execTask(new Task("saveFileInfos") {
			@Override
			public void work() {
				mainDB.saveFileInfos(list);
			}
		});
		flush();
	}

	public void loadBookInfo(final FileInfo fileInfo, final BookInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadBookInfo") {
			@Override
			public void work() {
				final BookInfo bookInfo = mainDB.loadBookInfo(fileInfo);
				sendTask(handler, () -> callback.onBooksInfoLoaded(bookInfo));
			}
		});
	}

	public void loadFileInfoByOPDSLink(final String opdsLink, final boolean isLitres,
									   final FileInfo1LoadingCallback callback, final Handler handler) {
		execTask(new Task("loadFileInfoByOPDSLink") {
			@Override
			public void work() {
				if (StrUtils.isEmptyStr(opdsLink)) {
					sendTask(handler, () -> callback.onFileInfoLoaded(null));
				} else {
					final FileInfo fileInfo = mainDB.loadFileInfoByOPDSLink(opdsLink, isLitres);
					sendTask(handler, () -> callback.onFileInfoLoaded(fileInfo));
				}
			}
		});
	}

	public void loadFileInfoById(final long id,
									   final FileInfo1LoadingCallback callback, final Handler handler) {
		execTask(new Task("loadFileInfoById") {
			@Override
			public void work() {
				final FileInfo fileInfo = mainDB.loadFileInfoById(id);
				sendTask(handler, () -> callback.onFileInfoLoaded(fileInfo));
			}
		});
	}

	public void loadFileInfos(final ArrayList<String> pathNames, final Scanner.ScanControl control, final Engine.ProgressControl progress, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadFileInfos") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = mainDB.loadFileInfos(pathNames, control, progress);
				sendTask(handler, () -> callback.onFileInfoListLoaded(list, ""));
			}
		});
	}
	
	public void saveBookInfo(final BookInfo bookInfo) {
		execTask(new Task("saveBookInfo") {
			@Override
			public void work() {
				mainDB.saveBookInfo(bookInfo);
			}
		});
		flush();
	}
	
	public void deleteBook(final FileInfo fileInfo)	{
		execTask(new Task("deleteBook") {
			@Override
			public void work() {
				mainDB.deleteBook(fileInfo);
				coverDB.deleteCoverpage(fileInfo.getPathName());
			}
		});
		flush();
	}
	
	public void deleteBookmark(final Bookmark bm) {
		execTask(new Task("deleteBookmark") {
			@Override
			public void work() {
				mainDB.deleteBookmark(bm);
			}
		});
		flush();
	}
	
	public void setPathCorrector(final MountPathCorrector corrector) {
		execTask(new Task("setPathCorrector") {
			@Override
			public void work() {
				mainDB.setPathCorrector(corrector);
			}
		});
	}

	public void deleteRecentPosition(final FileInfo fileInfo) {
		execTask(new Task("deleteRecentPosition") {
			@Override
			public void work() {
				mainDB.deleteRecentPosition(fileInfo);
			}
		});
		flush();
	}

    //=======================================================================================
    // Favorite folders access code
    //=======================================================================================
    public void createFavoriteFolder(final FileInfo folder) {
   		execTask(new Task("createFavoriteFolder") {
   			@Override
   			public void work() {
   				mainDB.createFavoritesFolder(folder);
   			}
   		});
        flush();
   	}

    public void loadFavoriteFolders(final FileInfoLoadingCallback callback, final Handler handler) {
   		execTask(new Task("loadFavoriteFolders") {
            @Override
            public void work() {
                final ArrayList<FileInfo> favorites = mainDB.loadFavoriteFolders();
                sendTask(handler, () -> callback.onFileInfoListLoaded(favorites, ""));
            }
        });
   	}

    public void updateFavoriteFolder(final FileInfo folder) {
   		execTask(new Task("updateFavoriteFolder") {
   			@Override
   			public void work() {
   				mainDB.updateFavoriteFolder(folder);
   			}
   		});
        flush();
   	}

    public void deleteFavoriteFolder(final FileInfo folder) {
   		execTask(new Task("deleteFavoriteFolder") {
   			@Override
   			public void work() {
   				mainDB.deleteFavoriteFolder(folder);
   			}
   		});
        flush();
   	}

	//=======================================================================================
	// Author aliases access code
	//=======================================================================================

	public interface AuthorsAliasesLoadingCallback {
		void onAuthorsAliasesLoaded(int cnt);
		void onAuthorsAliasesLoadProgress(int percent);
	}

	public void saveAuthorsAliasesInfo(final ArrayList<AuthorAlias> list, final AuthorsAliasesLoadingCallback callback, final Handler handler) {
		execTask(new Task("saveAuthorsAliasesInfo") {
			@Override
			public void work() {
				mainDB.deleteAuthorsAliasesInfo();
				int i = 0;
				int wasperc = 0;
				if (list.size() > 0) {
					for (AuthorAlias al : list) {
						i++;
						final int perc = (int) i * 100 / list.size();
						if (wasperc != perc)
							sendTask(handler, () -> callback.onAuthorsAliasesLoadProgress(perc));
						mainDB.saveAuthorAliasInfo(al);
					}
				} else {
					sendTask(handler, () -> callback.onAuthorsAliasesLoaded(0));
				}
				mainDB.createUniqueAliasesList();
				mainDB.flushAndTransaction();
				final int iF = i;
				sendTask(handler, () -> callback.onAuthorsAliasesLoaded(iF));
			}
		});
		flush();
	}

	//=======================================================================================
	// Library maintenance access code
	//=======================================================================================

	public void deleteBookEntries(final ArrayList<String> toRemove, final ObjectCallback callback, final Handler handler) {
		execTask(new Task("deleteBookEntries") {
			@Override
			public void work() {
				Long cnt = mainDB.deleteBookEntries(toRemove);
				sendTask(handler, () -> callback.onObjectLoaded(cnt));
			}
		});
		flush();
	}

	public void deleteOrphanEntries(final ObjectCallback callback, final Handler handler) {
		execTask(new Task("deleteOrphanEntries") {
			@Override
			public void work() {
				Long cnt = mainDB.deleteOrphanEntries();
				sendTask(handler, () -> callback.onObjectLoaded(cnt));
			}
		});
		flush();
	}

	public void deleteCloudEntries(final ObjectCallback callback, final Handler handler) {
		execTask(new Task("deleteCloudEntries") {
			@Override
			public void work() {
				Long cnt = mainDB.deleteCloudEntries();
				sendTask(handler, () -> callback.onObjectLoaded(cnt));
			}
		});
		flush();
	}

	public void getLibraryStats(final ObjectCallback callback, final Handler handler) {
		execTask(new Task("getLibraryStats") {
			@Override
			public void work() {
				LibraryStats ls = mainDB.getLibraryStats();
				sendTask(handler, () -> callback.onObjectLoaded(ls));
			}
		});
		flush();
	}

	public void getLibraryCategStats(final ObjectCallback callback, final Handler handler) {
		execTask(new Task("getLibraryCategStats") {
			@Override
			public void work() {
				boolean b = mainDB.getLibraryCategStats();
				sendTask(handler, () -> callback.onObjectLoaded(b));
			}
		});
		flush();
	}

	public void moveBookToFolder(final FileInfo bookFile, final String toFolder, final boolean alreadyMoved,
								 ObjectCallback callback, final Handler handler) {
		execTask(new Task("moveBookToFolder") {
			@Override
			public void work() {
				boolean b = mainDB.moveBookToFolder(bookFile, toFolder, alreadyMoved);
				if (b)
					b = coverDB.changeCoverPath(bookFile, toFolder);
				boolean finalB = b;
				sendTask(handler, () -> callback.onObjectLoaded(finalB));
			}
		});
		flush();
	}

	public void getBookFlags(final FileInfo bookFile,
								 ObjectCallback callback, final Handler handler) {
		execTask(new Task("getBookFlags") {
			@Override
			public void work() {
				int flags = mainDB.getBookFlags(bookFile);
				sendTask(handler, () -> callback.onObjectLoaded(flags));
			}
		});
		flush();
	}

	//=======================================================================================
	// calendar DB access code
	//=======================================================================================

	public void updateCalendarEntry(final Long book_fk,  final Long read_date, final Long time_spent_sec) {
		execTask(new Task("updateCalendarEntry") {
			@Override
			public void work() {
				mainDB.updateCalendarEntry(book_fk,  read_date, time_spent_sec);
			}
		});
	}

	public void getCalendarEntries(final long fromDate, final long toDate,
								   final CalendarStatsLoadingCallback callback, final Handler handler)
	{
		execTask(new Task("getCalendarEntries") {
			@Override
			public void work() {
				ArrayList<CalendarStats> list = mainDB.getCalendarEntries(fromDate, toDate);
				sendTask(handler, () -> callback.onCalendarStatsListLoaded(list));
			}
		});
	}

	public void getBookLastCalendarEntries(final Long book_fk, final long cnt,
										   final CalendarStatsLoadingCallback callback,
										   final Handler handler)
	{
		execTask(new Task("getBookLastCalendarEntries") {
			@Override
			public void work() {
				ArrayList<CalendarStats> list = mainDB.getBookLastCalendarEntries(book_fk, cnt);
				sendTask(handler, () -> callback.onCalendarStatsListLoaded(list));
			}
		});
	}

	// tags access code

	public void saveTag(final String tag, final String oldTag, final ObjectCallback callback, final Handler handler) {
		execTask(new Task("saveTag") {
			@Override
			public void work() {
				Long tagId = mainDB.getTagId(tag, tag, oldTag);
				sendTask(handler, () -> callback.onObjectLoaded(tagId));
			}
		});
	}

	public void deleteTag(final String tag, final ObjectCallback callback, final Handler handler) {
		execTask(new Task("deleteTag") {
			@Override
			public void work() {
				Long tagId = mainDB.deleteTag(tag);
				sendTask(handler, () -> callback.onObjectLoaded(tagId));
			}
		});
	}

	public void loadTags(final FileInfo fi, final TagsLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadTags") {
			@Override
			public void work() {
				mainDB.loadTagsListF(fi);
				ArrayList<BookTag> bookTagsList = mainDB.loadTagsListF(fi);
				sendTask(handler, () -> callback.onTagsLoaded(bookTagsList));
			}
		});
	}

	public void loadTagsList(FileInfo parent, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent);
		execTask(new Task("loadTagsList") {
			@Override
			public void work() {
				mainDB.loadTagsList(p);
				sendTask(handler, () -> callback.onItemGroupsLoaded(p));
			}
		});
	}

	public void findTagBooks(final long tagId, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findTagBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<>();
				mainDB.findTagBooks(list, tagId);
				sendTask(handler, () -> callback.onFileInfoListLoaded(list, FileInfo.TAG_GROUP_PREFIX));
			}
		});
	}

	public void addTagsToBook(List<BookTag> bookTags, FileInfo fi, final ObjectCallback callback, final Handler handler) {
		execTask(new Task("loadTags") {
			@Override
			public void work() {
				Boolean b = mainDB.addTagsToBook(bookTags, fi);
				sendTask(handler, () -> callback.onObjectLoaded(b));
			}
		});
	}

	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     * Provides interface for asynchronous operations with database.
     */
    public class LocalBinder extends Binder {
        public CRDBService getService() {
            return CRDBService.this;
        }
        
    	public void saveBookCoverpage(final FileInfo fileInfo, byte[] data) {
    		getService().saveBookCoverpage(fileInfo, data);
    	}
    	
    	public void loadBookCoverpage(final FileInfo fileInfo, final CoverpageLoadingCallback callback) {
    		getService().loadBookCoverpage(new FileInfo(fileInfo), callback, new Handler());
    	}
    	
    	public void loadOPDSCatalogs(final OPDSCatalogsLoadingCallback callback) {
    		getService().loadOPDSCatalogs(callback, new Handler());
    	}

		public String getDBPath() {
			return getService().getDBPath();
		}

    	public void saveOPDSCatalog(final Long id, final String url, final String name,
									final String username, final String password,
									final String proxy_addr, final String proxy_port,
									final String proxy_uname, final String proxy_passw,
									final int onion_def_proxy
									) {
    		getService().saveOPDSCatalog(id, url, name, username, password,
					proxy_addr,proxy_port,
					proxy_uname, proxy_passw,
					onion_def_proxy);
    	}

    	public void updateOPDSCatalog(final String url, final String field, final String value) {
    		getService().updateOPDSCatalog(url, field, value);
    	}

    	public void removeOPDSCatalog(final Long id) {
    		getService().removeOPDSCatalog(id);
    	}

		public void saveCalibreCatalog(final Long id, final String name, final boolean isLocal,
									final String localFolder,
									final String remoteFolderYD
		) {
			getService().saveCalibreCatalog(id, name, isLocal, localFolder, remoteFolderYD);
		}

		public void updateCalibreCatalog(final String name, final boolean isLocal,
										 final String localFolder,
										 final String remoteFolderYD,
										 final boolean onlyMax) {
			getService().updateCalibreCatalog(name, isLocal, localFolder, remoteFolderYD, onlyMax);
		}

		public void removeCalibreCatalog(final Long id) {
			getService().removeCalibreCatalog(id);
		}

		public void loadAuthorsList(FileInfo parent, String filterSeries, boolean withAliases,
									final ItemGroupsLoadingCallback callback) {
    		getService().loadAuthorsList(parent, filterSeries, withAliases, callback, new Handler());
    	}

		public void loadCalibreAuthorsList(FileInfo parent, String filterSeries, boolean withAliases,
										   final ItemGroupsLoadingCallback callback) {
			getService().loadCalibreAuthorsList(parent, filterSeries, withAliases, callback, new Handler());
		}

// 		CR implementation
//		public void loadGenresList(FileInfo parent, boolean showEmptyGenres, final ItemGroupsLoadingCallback callback) {
//			getService().loadGenresList(parent, showEmptyGenres, callback, new Handler());
//		}

		// KR implementation
		public void loadGenresList(FileInfo parent, final ItemGroupsLoadingCallback callback) {
			getService().loadGenresList(parent, callback, new Handler());
		}

    	public void loadSeriesList(FileInfo parent, String filterAuthor, final ItemGroupsLoadingCallback callback) {
    		getService().loadSeriesList(parent, filterAuthor, callback, new Handler());
    	}

        public void loadByDateList(FileInfo parent, final String field, final ItemGroupsLoadingCallback callback) {
            getService().loadByDateList(parent, field, callback, new Handler());
        }
    	
    	public void loadTitleList(FileInfo parent, final ItemGroupsLoadingCallback callback) {
    		getService().loadTitleList(parent, callback, new Handler());
    	}

    	public void loadAuthorBooks(long authorId, String addFilter, FileInfoLoadingCallback callback) {
    		getService().findAuthorBooks(authorId, addFilter, callback, new Handler());
    	}

		public void loadCalibreAuthorBooks(long authorId, String addFilter, FileInfoLoadingCallback callback) {
			getService().findCalibreAuthorBooks(authorId, addFilter, callback, new Handler());
		}

		// CR implementation
//		public void loadGenresBooks(String genreCode, boolean showEmptyGenres, FileInfoLoadingCallback callback) {
//			getService().findGenresBooks(genreCode, showEmptyGenres, callback, new Handler());
//		}

		// KR implementation
		public void loadGenreBooks(long genreId, FileInfoLoadingCallback callback) {
			getService().findGenreBooks(genreId, callback, new Handler());
		}
    	
    	public void loadSeriesBooks(long seriesId, String addFilter, FileInfoLoadingCallback callback) {
    		getService().findSeriesBooks(seriesId, addFilter, callback, new Handler());
    	}

		public void loadByDateBooks(long bookdateId, final String field, FileInfoLoadingCallback callback) {
			getService().findByDateBooks(bookdateId, field, callback, new Handler());
		}

		public void loadSearchHistory(BookInfo book, SearchHistoryLoadingCallback callback) {
			getService().loadSearchHistory(book, callback, new Handler());
		}

		public void loadUserDic(UserDicLoadingCallback callback) {
			getService().loadUserDic(callback, new Handler());
		}

		public void loadDicSearchHistory(DicSearchHistoryLoadingCallback callback) {
			getService().loadDicSearchHistory(callback, new Handler());
		}

    	public void loadBooksByRating(int minRating, int maxRating, FileInfoLoadingCallback callback) {
    		getService().findBooksByRating(minRating, maxRating, callback, new Handler());
    	}

    	public void loadBooksByState(int state, FileInfoLoadingCallback callback) {
    		getService().findBooksByState(state, callback, new Handler());
    	}

    	public void loadRecentBooks(final int maxCount, final RecentBooksLoadingCallback callback) {
    		getService().loadRecentBooks(maxCount, callback, new Handler());
    	}

    	public void sync(final Runnable callback) {
    		getService().sync(callback, new Handler());
    	}

    	public void saveFileInfos(final Collection<FileInfo> list) {
    		getService().saveFileInfos(deepCopyFileInfos(list));
    	}

    	public void findByFingerprints(final int maxCount, Collection<String> fingerprints, final BookSearchCallback callback) {
    		getService().findByFingerprints(maxCount, fingerprints, callback, new Handler());
    	}

		public void findByPatterns(final int maxCount, final String authors, final String title, final String series, final String filename, final BookSearchCallback callback) {
			getService().findByPatterns(maxCount, authors, title, series, filename, callback, new Handler());
		}

		public void loadFileInfos(final ArrayList<String> pathNames, final Scanner.ScanControl control, final Engine.ProgressControl progress, final FileInfoLoadingCallback callback) {
			getService().loadFileInfos(pathNames, control, progress, callback, new Handler());
		}

		public void deleteBook(final FileInfo fileInfo)	{
    		getService().deleteBook(new FileInfo(fileInfo));
    	}

    	public void saveBookInfo(final BookInfo bookInfo) {
    		getService().saveBookInfo(new BookInfo(bookInfo));
    	}

		public void saveSearchHistory(final BookInfo book, String sHist) {
			getService().saveSearchHistory(new BookInfo(book), sHist);
		}

		public void saveUserDic(final UserDicEntry ude, final int action) {
			getService().saveUserDic(ude, action);
		}

		public void updateDicSearchHistory(final DicSearchHistoryEntry dshe, final int action, CoolReader act) {
			int idx = -1;
			if (action == DicSearchHistoryEntry.ACTION_CLEAR_ALL) UserDicDlg.mDicSearchHistoryAll.clear();
			if (UserDicDlg.mDicSearchHistoryAll.size() > 0)
				for (int i = 0; i < UserDicDlg.mDicSearchHistoryAll.size(); i++) {
					if (StrUtils.getNonEmptyStr(UserDicDlg.mDicSearchHistoryAll.get(i).getSearch_text(), true).toLowerCase().equals(
							StrUtils.getNonEmptyStr(dshe.getSearch_text(),true).toLowerCase())) {
						idx = i;
						break;
					}
				}
			if (idx >= 0) {
				UserDicDlg.mDicSearchHistoryAll.remove(idx);
				//act.showToast("deleted:" + StrUtils.getNonEmptyStr(dshe.getSearch_text(),true));
			}
        	if (action == DicSearchHistoryEntry.ACTION_SAVE) {
				UserDicDlg.mDicSearchHistoryAll.add(0, dshe);
			}
			act.updateUserDicWords();
			getService().updateDicSearchHistory(dshe, action);
		}

		public void clearSearchHistory(final BookInfo book) {
			getService().clearSearchHistory(new BookInfo(book));
		}

    	public void deleteRecentPosition(final FileInfo fileInfo)	{
    		getService().deleteRecentPosition(new FileInfo(fileInfo));
    	}
    	
    	public void deleteBookmark(final Bookmark bm) {
    		getService().deleteBookmark(new Bookmark(bm));
    	}

    	public void loadBookInfo(final FileInfo fileInfo, final BookInfoLoadingCallback callback) {
    		getService().loadBookInfo(new FileInfo(fileInfo), callback, new Handler());
    	}

		public void loadFileInfoByOPDSLink(final String opdsLink, final boolean isLitres, final FileInfo1LoadingCallback callback) {
			getService().loadFileInfoByOPDSLink(opdsLink, isLitres, callback, new Handler());
		}

		public void loadFileInfoById(final long id, final FileInfo1LoadingCallback callback) {
			getService().loadFileInfoById(id, callback, new Handler());
		}

		public void createFavoriteFolder(final FileInfo folder) {
            getService().createFavoriteFolder(folder);
       	}

        public void loadFavoriteFolders(FileInfoLoadingCallback callback) {
            getService().loadFavoriteFolders(callback, new Handler());
       	}

        public void updateFavoriteFolder(final FileInfo folder) {
            getService().updateFavoriteFolder(folder);
       	}

        public void deleteFavoriteFolder(final FileInfo folder) {
            getService().deleteFavoriteFolder(folder);
       	}


    	public void setPathCorrector(MountPathCorrector corrector) {
    		getService().setPathCorrector(corrector);
    	}
    	
    	public void flush() {
    		getService().forceFlush();
    	}

    	public void reopenDatabase() {
        	getService().reopenDatabase();
		}

		public void saveAuthorsAliasesInfo(final ArrayList<AuthorAlias> list, final AuthorsAliasesLoadingCallback callback) {
			getService().saveAuthorsAliasesInfo(list, callback, new Handler());
		}

		public void deleteBookEntries(final ArrayList<String> toRemove, ObjectCallback callback) {
			getService().deleteBookEntries(toRemove, callback, new Handler());
		}

		public void deleteOrphanEntries(ObjectCallback callback) {
			getService().deleteOrphanEntries(callback, new Handler());
		}

		public void deleteCloudEntries(ObjectCallback callback) {
			getService().deleteCloudEntries(callback, new Handler());
		}

		public void getLibraryStats(ObjectCallback callback) {
			getService().getLibraryStats(callback, new Handler());
		}

		public void getLibraryCategStats(ObjectCallback callback) {
			getService().getLibraryCategStats(callback, new Handler());
		}

		public void moveBookToFolder(final FileInfo bookFile, final String toFolder, final boolean alreadyMoved,
									 ObjectCallback callback) {
			getService().moveBookToFolder(bookFile, toFolder, alreadyMoved, callback, new Handler());
		}

		public void getBookFlags(final FileInfo bookFile, ObjectCallback callback) {
			getService().getBookFlags(bookFile, callback, new Handler());
		}

		public void loadFileInfos(ArrayList<String> pathNames, FileInfoLoadingCallback fileInfoLoadingCallback) {
		}

		public void convertStartDictDic(String dicPath, String dicName,
									final Scanner.ScanControl control, final Engine.ProgressControl progress,
									final ObjectCallback callback) {
			getService().convertStartDictDic(dicPath, dicName, control, progress, callback, new Handler());
		}

		public void findInOfflineDictDic(String searchStr, String langFrom, String langTo,
										boolean extended, final ObjectCallback callback) {
			getService().findInOfflineDictDic(searchStr, langFrom, langTo, extended, callback, new Handler());
		}

		public void closeAllDics(final ObjectCallback callback) {
			getService().closeAllDics(callback, new Handler());
		}

		public void getCalendarEntries(final long fromDate, final long toDate,
									   final CalendarStatsLoadingCallback callback) {
			getService().getCalendarEntries(fromDate, toDate, callback, new Handler());
		}

		public void getBookLastCalendarEntries(final Long book_fk, final long cnt,
									   final CalendarStatsLoadingCallback callback) {
			getService().getBookLastCalendarEntries(book_fk, cnt, callback, new Handler());
		}

		public void updateCalendarEntry(final Long book_fk,  final Long read_date, final Long time_spent_sec) {
			getService().updateCalendarEntry(book_fk,  read_date, time_spent_sec);
		}

		// Tags access code

		public void saveTag(String tag, String oldTag, final ObjectCallback callback) {
			getService().saveTag(tag, oldTag, callback, new Handler());
		}

		public void deleteTag(String tag, final ObjectCallback callback) {
			getService().deleteTag(tag, callback, new Handler());
		}

		public void loadTags(FileInfo fi, TagsLoadingCallback callback) {
			getService().loadTags(fi, callback, new Handler());
		}

		public void addTagsToBook(List<BookTag> bookTags, FileInfo fi, final ObjectCallback callback) {
			getService().addTagsToBook(bookTags, fi, callback, new Handler());
		}

		public void loadTagsList(FileInfo parent, final ItemGroupsLoadingCallback callback) {
			getService().loadTagsList(parent, callback, new Handler());
		}

		public void loadTagBooks(long genreId, FileInfoLoadingCallback callback) {
			getService().findTagBooks(genreId, callback, new Handler());
		}

	}

    @Override
    public IBinder onBind(Intent intent) {
	    log.i("onBind(): " + intent);
        return mBinder;
    }

    @Override
    public void onRebind (Intent intent) {
        log.i("onRebind(): " + intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        log.i("onUnbind(): intent=" + intent);
        return true;
    }

    private final IBinder mBinder = new LocalBinder();
    
}
