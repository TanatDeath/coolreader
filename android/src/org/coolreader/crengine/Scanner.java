package org.coolreader.crengine;

import android.util.Log;
import org.coolreader.R;
import org.coolreader.db.CRDBService;
import org.coolreader.db.MainDB;
import org.coolreader.plugins.OnlineStorePluginManager;
import org.coolreader.plugins.OnlineStoreWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

public class Scanner extends FileInfoChangeSource {
	
	public static final Logger log = L.create("sc");
	
	HashMap<String, FileInfo> mFileList = new HashMap<>();
//	ArrayList<FileInfo> mFilesForParsing = new ArrayList<FileInfo>();
	FileInfo mRoot;
	
	boolean mHideEmptyDirs = true;
	
	public void setHideEmptyDirs( boolean flgHide ) {
		mHideEmptyDirs = flgHide;
	}

	private boolean dirScanEnabled = true;
	public boolean getDirScanEnabled()
	{
		return dirScanEnabled;
	}
	
	public void setDirScanEnabled(boolean dirScanEnabled)
	{
		this.dirScanEnabled = dirScanEnabled;
	}
	
	private FileInfo scanZip(FileInfo zip)
	{
		try {
			File zf = new File(zip.pathname);
			long arcsize = zf.length();
			//ZipFile file = new ZipFile(zf);
			ArrayList<ZipEntry> entries = engine.getArchiveItems(zip.pathname);
			ArrayList<FileInfo> items = new ArrayList<FileInfo>();
			//for ( Enumeration<?> e = file.entries(); e.hasMoreElements(); ) {
			for (ZipEntry entry : entries) {
				if (entry.isDirectory())
					continue;
				String name = entry.getName();
				FileInfo item = new FileInfo();
				item.format = DocumentFormat.byExtension(name);
				if (item.format==null)
					continue;
				if (item.format == DocumentFormat.NONE)
					continue;
				File f = new File(name);
				item.setFilename(f.getName());
				item.path = f.getPath();
				item.pathname = entry.getName();
				item.size = entry.getSize();
				item.setCreateTime(zf.lastModified()); // it looks strange whilst if fileinfo.create used zip entry date...
				item.setFileCreateTime(zf.lastModified());
				item.arcname = zip.pathname;
				//item.arcsize = entry.getCompressedSize();
				item.arcsize = zip.size;
				item.isArchive = true;
				items.add(item);
			}
			if (items.size() == 0) {
				L.i("Supported files not found in " + zip.pathname);
				return null;
			} else if (items.size() == 1) {
				// single supported file in archive
				FileInfo item = items.get(0);
				item.isArchive = true;
				item.isDirectory = false;
				return item;
			} else {
				zip.isArchive = true;
				zip.isDirectory = true;
				zip.isListed = true;
				for ( FileInfo item : items ) {
					item.parent = zip;
					zip.addFile(item);
				}
				return zip;
			}
		} catch ( Exception e ) {
			L.e("IOException while opening " + zip.pathname + " " + e.getMessage());
		}
		return null;
	}

	public boolean listDirectory(FileInfo baseDir)
	{
		return listDirectory(baseDir, true, true);
	}

	/**
	 * Adds dir and file children to directory FileInfo item.
	 * @param baseDir is directory to list files and dirs for
	 * @return true if successful.
	 */
	public boolean listDirectory(FileInfo baseDir, boolean onlySupportedFormats, boolean scanzip)
	{
		Set<String> knownItems = null;
		if (baseDir.isListed) {
			knownItems = new HashSet<String>();
			for ( int i=baseDir.itemCount()-1; i>=0; i-- ) {
				FileInfo item = baseDir.getItem(i);
				if (!item.exists()) {
					// remove item from list
					baseDir.removeChild(item);
				} else {
					knownItems.add(item.getBasePath());
				}
			}
		}
		try {
			File dir = new File(baseDir.pathname);
			//File[] items = dir.listFiles();
			// To resolve unhandled exception
			// 'JNI DETECTED ERROR IN APPLICATION: input is not valid Modified UTF-8: illegal continuation byte 0'
			// that can be produced by invalid filename (broken sdcard, etc)
			// or 'JNI WARNING: input is not valid Modified UTF-8: illegal start byte 0xf0'
			// that can be generated if 4-byte UTF-8 sequence found in the filename,
			// we implement own directory listing method instead of File.listFiles().
			// TODO: replace other occurrences of the method File.listFiles().
			File[] items = Engine.listFiles(dir);
			// process normal files
			if (items != null) {
				for ( File f : items ) {
					// check whether file is a link
					if (Engine.isLink(f.getAbsolutePath()) != null) {
						log.w("skipping " + f + " because it's a link");
						continue;
					}
					if (!f.isDirectory()) {
						// regular file
						if (f.getName().startsWith("."))
							continue; // treat files beginning with '.' as hidden
						if (f.getName().equalsIgnoreCase("LOST.DIR"))
							continue; // system directory
						String pathName = f.getAbsolutePath();
						if (knownItems != null && knownItems.contains(pathName))
							continue;
						if (engine.isRootsMountPoint(pathName)) {
							// skip mount root
							continue;
						}
						boolean isZip = pathName.toLowerCase().endsWith(".zip") && (!pathName.toLowerCase().endsWith("fb3.zip"));
						FileInfo item = mFileList.get(pathName);
						boolean isNew = false;
						if (item == null) {
							item = new FileInfo(f);
							if (scanzip && isZip) {
								item = scanZip(item);
								if (item == null)
									continue;
								if (item.isDirectory) {
									// many supported files in ZIP
									item.parent = baseDir;
									baseDir.addDir(item);
									for ( int i=0; i<item.fileCount(); i++ ) {
										FileInfo file = item.getFile(i);
										mFileList.put(file.getPathName(), file);
									}
								} else {
									item.parent = baseDir;
									baseDir.addFile(item);
									mFileList.put(pathName, item);
								}
								continue;
							}
							isNew = true;
						}
						boolean formatOk = item.format!=null;
						if (formatOk) formatOk = item.format != DocumentFormat.NONE;
						if (!onlySupportedFormats || formatOk) {
							item.parent = baseDir;
							baseDir.addFile(item);
							if (isNew)
								mFileList.put(pathName, item);
						}
					}
				}
				// process directories 
				for ( File f : items ) {
					if (f.isDirectory()) {
						if (f.getName().startsWith("."))
							continue; // treat dirs beginning with '.' as hidden
						FileInfo item = new FileInfo( f );
						if (knownItems != null && knownItems.contains(item.getPathName()))
							continue;
						item.parent = baseDir;
						baseDir.addDir(item);					
					}
				}
			}
			baseDir.isListed = true;
			return true;
		} catch ( Exception e ) {
			L.e("Exception while listing directory " + baseDir.pathname, e);
			baseDir.isListed = true;
			return false;
		}
	}
	
	public static class ScanControl {
		volatile private boolean stopped = false;
		public boolean isStopped() {
			return stopped;
		}
		public void stop() {
			stopped = true;
		}
	}

	public interface ScanCompleteListener {
		void onComplete(ScanControl scanControl);
	}

	/**
	 * Call this method (in GUI thread) to update views if directory content is changed outside.
	 * @param dir is directory with changed content
	 */
	public void onDirectoryContentChanged(FileInfo dir) {
		log.v("onDirectoryContentChanged(" + dir.getPathName() + ")");
		onChange(dir, false);
	}
	
	/**
	 * For all files in directory, retrieve metadata from DB or scan and save into DB.
	 * Call in GUI thread only!
	 * @param baseDir is directory with files to lookup/scan; file items will be updated with info from file metadata or DB
	 * @param readyCallback is Runable to call when operation is finished or stopped (will be called in GUI thread)
	 * @param control allows to stop long operation
	 */
	private void scanDirectoryFiles(final CRDBService.LocalBinder db, final FileInfo baseDir, final ScanControl control, final Engine.ProgressControl progress, final Runnable readyCallback) {
		// GUI thread
		BackgroundThread.ensureGUI();
		log.d("scanDirectoryFiles(" + baseDir.getPathName() + ") ");
		
		// store list of files to scan
		ArrayList<String> pathNames = new ArrayList<>();
		for (int i=0; i < baseDir.fileCount(); i++) {
			pathNames.add(baseDir.getFile(i).getPathName());
		}

		if (pathNames.size() == 0) {
			readyCallback.run();
			return;
		}

		// list all subdirectories
		for (int i=0; i < baseDir.dirCount(); i++) {
			if (control.isStopped())
				break;
			FileInfo dir = baseDir.getDir(i);
			if (!dir.isListed)
				listDirectory(dir);
		}

		// load book infos for files
		db.loadFileInfos(pathNames, control, progress, new CRDBService.FileInfoLoadingCallback() {

			@Override
			public void onFileInfoListLoadBegin(String prefix) {

			}

			@Override
			public void onFileInfoListLoaded(ArrayList<FileInfo> list, String prefix) {
				log.v("onFileInfoListLoaded");
				// GUI thread
				final ArrayList<FileInfo> filesForParsing = new ArrayList<>();
				final ArrayList<FileInfo> filesForCRC32Update = new ArrayList<>();
				Map<String, FileInfo> mapOfFilesFoundInDb = new HashMap<>();
				for (FileInfo f : list)
					mapOfFilesFoundInDb.put(f.getPathName(), f);
				for (int i=0; i<baseDir.fileCount(); i++) {
					FileInfo item = baseDir.getFile(i);
					FileInfo fromDB = mapOfFilesFoundInDb.get(item.getPathName());
					// check the relevance of data in the database
					if (fromDB != null) {
						if (fromDB.crc32 == 0 || fromDB.size != item.size || fromDB.arcsize != item.arcsize ) {
							// to force rescan and update data in DB
							log.v("The found entry in the database is outdated (crc32=0), need to rescan " + fromDB.toString());
							fromDB = null;
						}
						if (null != fromDB && DocumentFormat.FB2 == fromDB.format && null == fromDB.genres) {
							// to force rescan and update data in DB
							log.v("The found entry in the database is outdated (genres=null), need to rescan " + fromDB.toString());
							fromDB = null;
						}
					} else {
						// not found in DB
						// for new files set latest DOM level and max block rendering flags
						item.domVersion = Engine.DOM_VERSION_CURRENT;
						item.blockRenderingFlags = Engine.BLOCK_RENDERING_FLAGS_WEB;
					}
					boolean isOldVer = true;
					if (fromDB != null) {
						if (fromDB.saved_with_ver == MainDB.DB_VERSION) isOldVer = false;
						// use DB value
						baseDir.setFile(i, fromDB);
						if (isOldVer) {
							FileInfo fi = new FileInfo(fromDB);
							fi.need_to_update_ver = true;
							filesForParsing.add(fi);
						}
					} else {
						// not found in DB
						if (item.format != null && item.format != DocumentFormat.NONE && item.format.canParseProperties()) {
							filesForParsing.add(new FileInfo(item));
						} else {
							filesForCRC32Update.add(new FileInfo(item));
						}
					}
					//plotn -remove
					//filesForParsing.add(new FileInfo(item));
				}
//                if (filesForCRC32Update.size() > 0) {
//					db.saveFileInfos(filesForCRC32Update);
//				}
				if ((filesForParsing.size() == 0 && filesForCRC32Update.size() == 0) || control.isStopped()) {
                	readyCallback.run();
					return;
				}
				// update CRC32 in Background thread
				BackgroundThread.instance().postBackground(() -> {
					// Background thread
					final ArrayList<FileInfo> filesForSave1 = new ArrayList<>();
					try {
						int count1 = filesForParsing.size();
						int count2 = filesForCRC32Update.size();
						int count = count1 + count2;
						for ( int i=0; i<count1; i++ ) {
							if (control.isStopped())
								break;
							progress.setProgress((i + count) * 10000 / (2*count));
							FileInfo item = filesForParsing.get(i);
							engine.scanBookProperties(item);
							filesForSave1.add(item);
						}
						for ( int i=0; i<count2; i++ ) {
							if (control.isStopped())
								break;
							progress.setProgress((i + count) * 10000 / (2*count));
							FileInfo item = filesForCRC32Update.get(i);
							Engine.updateFileCRC32(item);
							filesForSave1.add(item);
						}
					} catch (Exception e) {
						L.e("Exception while scanning", e);
					}
					if (progress != null) progress.hide();
					// jump to GUI thread
					BackgroundThread.instance().postGUI(() -> {
						// GUI thread
						try {
							if (filesForSave1.size() > 0) {
								db.saveFileInfos(filesForSave1);
							}
							for (FileInfo file : filesForSave1)
								baseDir.setFile(file);
						} catch (Exception e ) {
							L.e("Exception while scanning", e);
						}
						// call finish handler
						readyCallback.run();
					});
				});
			}
		});
	}
	
	/**
	 * Scan single directory for dir and file properties in background thread.
	 * @param db database instance to fetch/update metadata
	 * @param baseDir is directory to scan
	 * @param readyListener is called on completion
	 * @param recursiveScan is true to scan subdirectories recursively, false to scan current directory only
	 * @param scanControl is to stop long scanning
	 */
	public void scanDirectory(final CRDBService.LocalBinder db, final FileInfo baseDir,
				final Runnable initialUpdateCallback, final ScanCompleteListener readyListener, final boolean recursiveScan, final ScanControl scanControl) {
		scanDirectory(db, baseDir, initialUpdateCallback, readyListener, recursiveScan, scanControl, false);
	}

	public void scanDirectory(final CRDBService.LocalBinder db, final FileInfo baseDir,
							  final Runnable initialUpdateCallback, final ScanCompleteListener readyListener, final boolean recursiveScan, final ScanControl scanControl,
							  boolean hideProgress) {
		// Call in GUI thread only!
		BackgroundThread.ensureGUI();

		log.d("scanDirectory(" + baseDir.getPathName() + ") " + (recursiveScan ? "recursive" : ""));

		listDirectory(baseDir, true, false);
		if (null != initialUpdateCallback)
			initialUpdateCallback.run();
		listSubtreeBg(baseDir, mHideEmptyDirs ? Integer.MAX_VALUE : 2, scanControl, () -> {
			if ( (!getDirScanEnabled() || baseDir.isScanned) && !recursiveScan || scanControl.isStopped() ) {
				readyListener.onComplete(scanControl);
				return;
			}
			Engine.ProgressControl progress = engine.createProgress(recursiveScan ? 0 : R.string.progress_scanning, scanControl);
			scanDirectoryFiles(db, baseDir, scanControl, hideProgress ? null : progress, () -> {
				// GUI thread
				onDirectoryContentChanged(baseDir);
				try {
					if (scanControl.isStopped()) {
						// scan is stopped
						readyListener.onComplete(scanControl);
					} else {
						baseDir.isScanned = true;

						if ( recursiveScan ) {
							if (scanControl.isStopped()) {
								// scan is stopped
								readyListener.onComplete(scanControl);
								return;
							}
							// make list of subdirectories to scan
							final ArrayList<FileInfo> dirsToScan = new ArrayList<>();
							for ( int i=baseDir.dirCount()-1; i>=0; i-- ) {
								File dir = new File(baseDir.getDir(i).getPathName());
								if (!engine.getPathCorrector().isRecursivePath(dir))
									dirsToScan.add(baseDir.getDir(i));
							}
							final ScanCompleteListener dirIterator = new ScanCompleteListener() {
								@Override
								public void onComplete(ScanControl scanControl) {
									// process next directory from list
									if (dirsToScan.size() == 0 || scanControl.isStopped()) {
										readyListener.onComplete(scanControl);
										return;
									}
									final FileInfo dir = dirsToScan.get(0);
									dirsToScan.remove(0);
									final ScanCompleteListener listener = this;
									BackgroundThread.instance().postGUI(() -> scanDirectory(db, dir, null, listener, true, scanControl));
								}
							};
							dirIterator.onComplete(scanControl);
						} else {
							readyListener.onComplete(scanControl);
						}
					}
				} catch (Exception e) {
					// treat as finished
					readyListener.onComplete(scanControl);
				}
			});
		});
	}
	
	private boolean addRoot( String pathname, int resourceId, boolean listIt) {
		return addRoot( pathname, mActivity.getResources().getString(resourceId), listIt);
	}

	private FileInfo findRoot(String pathname) {
		String normalized = engine.getPathCorrector().normalizeIfPossible(pathname);
		for (int i = 0; i<mRoot.dirCount(); i++) {
			FileInfo dir = mRoot.getDir(i);
			if (normalized.equals(engine.getPathCorrector().normalizeIfPossible(dir.getPathName())))
				return dir;
		}
		return null;
	}

	private boolean addRoot( String pathname, String filename, boolean listIt) {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = pathname;
		dir.setFilename(filename);
		dir.title = filename;
		if (findRoot(pathname) != null) {
			log.w("skipping duplicate root " + pathname);
			return false; // exclude duplicates
		}
		if (listIt) {
			log.i("Checking FS root " + pathname);
			if (!dir.isReadableDirectory()) { // isWritableDirectory
				log.w("Skipping " + pathname + " - it's not a readable directory");
				return false;
			}
			if (!listDirectory(dir, true, false)) {
				log.w("Skipping " + pathname + " - listing failed");
				return false;
			}
			log.i("Adding FS root: " + pathname + "  " + filename);
		}
		mRoot.addDir(dir);
		dir.parent = mRoot;
		if (!listIt) {
			dir.isListed = true;
			dir.isScanned = true;
		}
		return true;
	}
	
	public FileInfo pathToFileInfo(String path) {
		if (path == null || path.length() == 0)
			return null;
		if (FileInfo.OPDS_LIST_TAG.equals(path))
			return createOPDSRoot();
		else if (FileInfo.RESCAN_LIBRARY_TAG.equals(path))
			return createRescanRoot();
		else if (FileInfo.SEARCH_SHORTCUT_TAG.equals(path))
			return createSearchRoot();
		else if (FileInfo.RECENT_DIR_TAG.equals(path))
			return getRecentDir();
		//CR genres implementation
		//else if (FileInfo.GENRES_TAG.equals(path))
		//	return createGenresRoot();
		else if (FileInfo.AUTHORS_TAG.equals(path))
			return createAuthorsRoot();
		else if (FileInfo.GENRE_TAG.equals(path))
			return createGenreRoot();
		else if (FileInfo.TITLE_TAG.equals(path))
			return createTitleRoot();
		else if (FileInfo.SERIES_TAG.equals(path))
			return createSeriesRoot();
		else if (FileInfo.BOOK_DATE_TAG.equals(path))
			return createBookdateRoot();
		else if (FileInfo.DOC_DATE_TAG.equals(path))
			return createDocdateRoot();
		else if (FileInfo.PUBL_YEAR_TAG.equals(path))
			return createPublyearRoot();
		else if (FileInfo.FILE_DATE_TAG.equals(path))
			return createFiledateRoot();
		else if (FileInfo.RATING_TAG.equals(path))
			return createBooksByRatingRoot();
		else if (FileInfo.STATE_READING_TAG.equals(path))
			return createBooksByStateReadingRoot();
		else if (FileInfo.STATE_TO_READ_TAG.equals(path))
			return createBooksByStateToReadRoot();
		else if (FileInfo.STATE_FINISHED_TAG.equals(path))
			return createBooksByStateFinishedRoot();
		else if (FileInfo.LITRES_TAG.equals(path))
			return createBooksByLitresRoot();
		else if (FileInfo.LITRES_GENRE_TAG.equals(path))
			return createBooksByLitresGenreRoot();
		else if (FileInfo.LITRES_COLLECTION_TAG.equals(path))
			return createBooksByLitresCollectionRoot();
		else if (FileInfo.LITRES_SEQUENCE_TAG.equals(path))
			return createBooksByLitresSequenceRoot();
		else if (FileInfo.LITRES_PERSON_TAG.equals(path))
			return createBooksByLitresPersonRoot();
		else if (path.startsWith(FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX)) {
			OnlineStoreWrapper w = OnlineStorePluginManager.getPlugin(mActivity, path);
			if (w != null)
				return w.createRootDirectory();
			return null;
		} else if (path.startsWith(FileInfo.OPDS_DIR_PREFIX))
			return createOPDSDir(path);
		else
			return new FileInfo(path);
	}
	
	public FileInfo createOPDSRoot() {
		final FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.OPDS_LIST_TAG;
		dir.setFilename(mActivity.getString(R.string.mi_book_opds_root));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createCalibreRoot() {
		final FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.CALIBRE_LIST_TAG;
		dir.setFilename(mActivity.getString(R.string.mi_book_calibre_root));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public static FileInfo createOnlineLibraryPluginItem(String packageName, String label) {
		final FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		if (packageName.startsWith(FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX))
			dir.pathname = packageName;
		else
			dir.pathname = FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX + packageName;
		dir.setFilename(label);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public static FileInfo createLitresItem(String label) {
		final FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.LITRES_TAG;
		dir.setFilename(label);
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	private void addRoot(FileInfo dir) {
		dir.parent = mRoot;
		mRoot.addDir(dir);
	}

	public FileInfo createRecentRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.RECENT_DIR_TAG;
		dir.setFilename(mActivity.getString(R.string.dir_recent_books));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addOPDSRoot() {
		addRoot(createOPDSRoot());
	}
	
	public FileInfo createSearchRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.SEARCH_SHORTCUT_TAG;
		dir.setFilename(mActivity.getString(R.string.mi_book_search));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addSearchRoot() {
		addRoot(createSearchRoot());
	}

//  CR genres implementation
//	public FileInfo createGenresRoot() {
//		FileInfo dir = new FileInfo();
//		dir.isDirectory = true;
//		dir.pathname = FileInfo.GENRES_TAG;
//		dir.filename = mActivity.getString(R.string.folder_name_books_by_genre);
//		dir.isListed = true;
//		dir.isScanned = true;
//		return dir;
//	}

	public FileInfo createRescanRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.RESCAN_LIBRARY_TAG;
		dir.setFilename(mActivity.getString(R.string.mi_book_rescan));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	private void addRescanRoot() {
		addRoot(createRescanRoot());
	}

	public FileInfo createAuthorsRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.AUTHORS_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_author));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createGenreRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.GENRE_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_genre));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addAuthorsRoot() {
		addRoot(createAuthorsRoot());
	}
	
	public FileInfo createOPDSDir(String path) {
		FileInfo opds = mRoot.findItemByPathName(FileInfo.OPDS_LIST_TAG);
		if (opds == null)
			return null;
		return opds.findItemByPathName(path);
	}
	
	public FileInfo createSeriesRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.SERIES_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_series));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createBookdateRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.BOOK_DATE_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_bookdate));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createDocdateRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.DOC_DATE_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_docdate));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createPublyearRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.PUBL_YEAR_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_publyear));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createFiledateRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.FILE_DATE_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_filedate));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	public FileInfo createBooksByRatingRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.RATING_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_rating));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	public FileInfo createBooksByStateToReadRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.STATE_TO_READ_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_state_to_read));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	public FileInfo createBooksByStateReadingRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.STATE_READING_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_state_reading));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	public FileInfo createBooksByStateFinishedRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.STATE_FINISHED_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_state_finished));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createBooksByLitresRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.LITRES_TAG;
		dir.setFilename("LitRes");
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createBooksByLitresGenreRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.LITRES_GENRE_TAG;
		dir.setFilename("LitRes: " + mActivity.getString(R.string.search_genres));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createBooksByLitresCollectionRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.LITRES_COLLECTION_TAG;
		dir.setFilename("LitRes: " + mActivity.getString(R.string.search_collections));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createBooksByLitresSequenceRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.LITRES_SEQUENCE_TAG;
		dir.setFilename("LitRes: " + mActivity.getString(R.string.search_sequences));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}

	public FileInfo createBooksByLitresPersonRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.LITRES_PERSON_TAG;
		dir.setFilename("LitRes: " + mActivity.getString(R.string.search_persons));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addSeriesRoot() {
		addRoot(createSeriesRoot());
	}
	
	public FileInfo createTitleRoot() {
		FileInfo dir = new FileInfo();
		dir.isDirectory = true;
		dir.pathname = FileInfo.TITLE_TAG;
		dir.setFilename(mActivity.getString(R.string.folder_name_books_by_title));
		dir.isListed = true;
		dir.isScanned = true;
		return dir;
	}
	
	private void addTitleRoot() {
		addRoot(createTitleRoot());
	}
	
	/**
	 * Lists all directories from root to directory of specified file, returns found directory.
	 * @param file
	 * @param root
	 * @return
	 */
	private FileInfo findParentInternal(FileInfo file, FileInfo root)	{
		if (root == null || file == null || root.isRecentDir())
			return null;
		if (!root.isRootDir() &&
				!(file.getPathName().startsWith(root.getPathName()) ||
						root.isOnSDCard() && file.getPathName().toLowerCase().startsWith( root.getPathName().toLowerCase() ) ) )
			return null;
		// to list all directories starting root dir
		if (root.isDirectory && !root.isSpecialDir())
				listDirectory(root);
		for ( int i=0; i<root.dirCount(); i++ ) {
			FileInfo found = findParentInternal( file, root.getDir(i));
			if (found != null)
				return found;
		}
		for ( int i=0; i<root.fileCount(); i++ ) {
			Log.i("ASDF", "findParentInternal, root: " + root.getFilename());
			String s1 = root.getFile(i).getPathName();
			String s2 = file.getPathName();
			Log.i("ASDF", "findParentInternal, s1: " + s1);
			Log.i("ASDF", "findParentInternal, s2: " + s2);
			if (root.getFile(i).getPathName().equals(file.getPathName()) ||
					root.isOnSDCard() && root.getFile(i).getPathName().equalsIgnoreCase(file.getPathName()))
				return root;
			if (root.getFile(i).getPathName().startsWith(file.getPathName() + "@/") ||
					root.isOnSDCard() && root.getFile(i).getPathName().toLowerCase().startsWith(file.getPathName().toLowerCase() + "@/"))
				return root;
		}
		return null;
	}
	
	public final static int MAX_DIR_LIST_TIME = 500; // 0.5 seconds
	
	/**
	 * Lists all directories from root to directory of specified file, returns found directory.
	 * @param file
	 * @param root
	 * @return
	 */
	public FileInfo findParent(FileInfo file, FileInfo root) {
		FileInfo parent = findParentInternal(file, root);
		if (parent == null) {
			autoAddRootForFile(new File(file.pathname));
			parent = findParentInternal(file, root);
			if (parent == null) {
				L.e("Cannot find root directory for file " + file.pathname);
				return null;
			}
		}
		return parent;
	}
	
	public FileInfo findFileInTree(FileInfo f) {
		FileInfo parent = findParent(f, getRoot());
		if (parent == null)
			return null;
		FileInfo item = parent.findItemByPathName(f.getPathName());
		return item;
	}

	/**
	 * List directories in subtree (in background thread), remove empty branches (w/o books).
	 * @param root is directory to start with
	 * @param maxDepth is maximum depth
	 * @param scanControl is to stop long scanning
	 * @param readyCallback ready callback, can be null
	 */
	private void listSubtreeBg(FileInfo root, int maxDepth, ScanControl scanControl, Runnable readyCallback) {
		BackgroundThread.instance().postBackground(() -> {
			// make a copy to scan in background
			FileInfo dir = new FileInfo(root);
			dir.parent = root.parent;
			dir.setItems(root);
			listSubtreeBg_impl(dir, maxDepth, scanControl);
			BackgroundThread.instance().postGUI(() -> {
				// transfer scanned items from background copy to update in GUI
				root.setItems(dir);
				if (null != readyCallback) {
					readyCallback.run();
				}
			});
		});
	}

	private boolean listSubtreeBg_impl(FileInfo dir, int maxDepth, ScanControl scanControl) {
		BackgroundThread.ensureBackground();
		boolean fullDepthScan = true;
		if (maxDepth <= 0 || scanControl.isStopped())
			return false;
		boolean res = listDirectory(dir, true, true);
		if (res) {
			for (int i = dir.dirCount() - 1; i >= -0; i--) {
				res = listSubtreeBg_impl(dir.getDir(i), maxDepth - 1, scanControl);
				if (!res) {
					fullDepthScan = false;
				}
				if (scanControl.isStopped())
					break;
			}
			if (fullDepthScan && mHideEmptyDirs) {
				if (dir.removeEmptyDirs()) {
					BackgroundThread.instance().postGUI(() -> {
						// make a copy to update in GUI
						FileInfo cp = new FileInfo(dir);
						cp.assign(dir);
						cp.parent = dir.parent;
						cp.setItems(dir);
						onDirectoryContentChanged(cp);
					});
				}
			}
		}
		BackgroundThread.instance().postGUI(() -> {
			// make a copy to update in GUI
			FileInfo cp = new FileInfo(dir);
			cp.assign(dir);
			cp.parent = dir.parent;
			cp.setItems(dir);
			onDirectoryContentChanged(cp);
		});
		return res;
	}

	public FileInfo setSearchResults( FileInfo[] results ) {
		FileInfo existingResults = null;
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			FileInfo dir = mRoot.getDir(i);
			if (dir.isSearchDir()) {
				existingResults = dir;
				dir.clear();
				break;
			}
		}
		if (existingResults == null) {
			FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = FileInfo.SEARCH_RESULT_DIR_TAG;
			dir.setFilename(mActivity.getResources().getString(R.string.dir_search_results));
			dir.parent = mRoot;
			dir.isListed = true;
			dir.isScanned = true;
			mRoot.addDir(dir);
			existingResults = dir;
		}
		for ( FileInfo item : results )
			existingResults.addFile(item);
		return existingResults;
	}
	
	public void initRoots(Map<String, String> fsRoots) {
		Log.d("cr3", "Scanner.initRoots(" + fsRoots + ")");
		mRoot.clear();
		// create recent books dir
		addRoot( FileInfo.RECENT_DIR_TAG, R.string.dir_recent_books, false);

		// create system dirs
		for (Map.Entry<String, String> entry : fsRoots.entrySet())
			addRoot( entry.getKey(), entry.getValue(), true);

		// create OPDS dir
		addOPDSRoot();
		
		// create search dir
		addSearchRoot();
		
		// create books by author root
		addAuthorsRoot();
		// create books by series root
		addSeriesRoot();
		// create books by title root
		addTitleRoot();
	}
	
	public boolean autoAddRootForFile( File f ) {
		File p = f.getParentFile();
		while ( p!=null ) {
			if (p.getParentFile() == null || p.getParentFile().getParentFile() == null)
				break;
			p = p.getParentFile();
		}
		if (p != null) {
			L.i("Found possible mount point " + p.getAbsolutePath());
			return addRoot(p.getAbsolutePath(), p.getAbsolutePath(), true);
		}
		return false;
	}
	
//	public boolean scan()
//	{
//		L.i("Started scanning");
//		long start = System.currentTimeMillis();
//		mFileList.clear();
//		mFilesForParsing.clear();
//		mRoot.clear();
//		// create recent books dir
//		FileInfo recentDir = new FileInfo();
//		recentDir.isDirectory = true;
//		recentDir.pathname = "@recent";
//		recentDir.filename = "Recent Books";
//		mRoot.addDir(recentDir);
//		recentDir.parent = mRoot;
//		// scan directories
//		lastPercent = -1;
//		lastProgressUpdate = System.currentTimeMillis() - 500;
//		boolean res = scanDirectories( mRoot );
//		// process found files
//		lookupDB();
//		parseBookProperties();
//		updateProgress(9999);
//		L.i("Finished scanning (" + (System.currentTimeMillis()-start)+ " ms)");
//		return res;
//	}
	
	
	public ArrayList<FileInfo> getLibraryItems() {
		ArrayList<FileInfo> result = new ArrayList<FileInfo>();
		result.add(pathToFileInfo(FileInfo.RESCAN_LIBRARY_TAG));
		result.add(pathToFileInfo(FileInfo.SEARCH_SHORTCUT_TAG));
		//result.add(pathToFileInfo(FileInfo.GENRES_TAG)); //CR genres implementation
		result.add(pathToFileInfo(FileInfo.AUTHORS_TAG));
		result.add(pathToFileInfo(FileInfo.TITLE_TAG));
		result.add(pathToFileInfo(FileInfo.SERIES_TAG));
		result.add(pathToFileInfo(FileInfo.RATING_TAG));
		//result.add(pathToFileInfo(FileInfo.STATE_TO_READ_TAG));
		//result.add(pathToFileInfo(FileInfo.STATE_READING_TAG));
		//result.add(pathToFileInfo(FileInfo.STATE_FINISHED_TAG));
		result.add(pathToFileInfo(FileInfo.BOOK_DATE_TAG));
		result.add(pathToFileInfo(FileInfo.DOC_DATE_TAG));
		result.add(pathToFileInfo(FileInfo.PUBL_YEAR_TAG));
		result.add(pathToFileInfo(FileInfo.FILE_DATE_TAG));
		result.add(pathToFileInfo(FileInfo.GENRE_TAG));
		return result;
	}
	
	public FileInfo getDownloadDirectory() {
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			FileInfo item = mRoot.getDir(i);
			if (!item.isWritableDirectory())
				continue;
			if (!item.isSpecialDir() && !item.isArchive) {
				if (!item.isListed)
					listDirectory(item, false, false);
				FileInfo books = item.findItemByPathName(item.pathname + "/Books");
				if (books == null)
					books = item.findItemByPathName(item.pathname + "/books");
				if (books != null && books.exists())
					return books;
				File dir = new File(item.getPathName());
				if (dir.isDirectory()) {
					if (!dir.canWrite())
						Log.w("cr3", "Directory " + dir + " is readonly");
					File f = new File( dir, "Books" );
					if (f.mkdirs() || f.isDirectory()) {
						books = new FileInfo(f);
						books.parent = item;
						item.addDir(books);
						books.isScanned = true;
						books.isListed = true;
						return books;
					}
				}
			}
		}
		File fd = mActivity.getFilesDir();
		File downloadDir = new File(fd, "downloads");
		if (downloadDir.isDirectory() || downloadDir.mkdirs()) {
			Log.d("cr3", "download dir: " + downloadDir);
			FileInfo books = null;
			books = new FileInfo(downloadDir);
			//books.parent = item;
			//item.addDir(books);
			books.isScanned = true;
			books.isListed = true;
			return books;
		}
		try {
			throw new Exception("download directory not found and cannot be created");
		} catch (Exception e) {
			Log.e("cr3", "download directory is not found!!!", e);
		}
		return null;
	}

	public FileInfo getSharedDownloadDirectory() {
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			FileInfo item = mRoot.getDir(i);
			if (!item.isWritableDirectory())
				continue;
			if ( !item.isSpecialDir() && !item.isArchive ) {
				if (!item.isListed)
					listDirectory(item, false, false);
				FileInfo download = item.findItemByPathName(item.pathname + "/Download");
				if (download == null)
					download = item.findItemByPathName(item.pathname + "/download");
				if (download != null && download.exists())
					return download;
			}
		}
		Log.e("cr3", "shared download directory is not found!!!");
		return null;
	}

	public boolean isValidFolder(FileInfo info) {
        File dir = new File( info.pathname );
        return dir.isDirectory();
    }

	public FileInfo getRoot() {
		return mRoot;
	}

	public FileInfo getOPDSRoot() {
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			if (mRoot.getDir(i).isOPDSRoot())
				return mRoot.getDir(i);
		}
		L.w("OPDS root directory not found!");
		return null;
	}
	
	public FileInfo getRecentDir() 
	{
		for ( int i=0; i<mRoot.dirCount(); i++ ) {
			if (mRoot.getDir(i).isRecentDir())
				return mRoot.getDir(i);
		}
		L.w("Recent books directory not found!");
		return null;
	}
	
	public Scanner( BaseActivity coolReader, Engine engine )
	{
		this.engine = engine;
		this.mActivity = coolReader;
		mRoot = new FileInfo();
		mRoot.path = FileInfo.ROOT_DIR_TAG;	
		mRoot.setFilename("File Manager");
		mRoot.pathname = FileInfo.ROOT_DIR_TAG;
		mRoot.isListed = true;
		mRoot.isScanned = true;
		mRoot.isDirectory = true;
	}

	private final Engine engine;
	private final BaseActivity mActivity;
}
