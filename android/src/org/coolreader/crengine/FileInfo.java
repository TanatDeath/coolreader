package org.coolreader.crengine;

import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import android.util.Log;
import org.coolreader.R;
import org.coolreader.plugins.OnlineStoreBook;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;

public class FileInfo {

	public final static String RECENT_DIR_TAG = "@recent";
	public final static String SEARCH_RESULT_DIR_TAG = "@searchResults";
	public final static String ROOT_DIR_TAG = "@root";
	public final static String OPDS_LIST_TAG = "@opds";
	public final static String OPDS_DIR_PREFIX = "@opds:";
	public final static String ONLINE_CATALOG_PLUGIN_PREFIX = "@plugin:";
	public final static String AUTHORS_TAG = "@authorsRoot";
	public final static String AUTHOR_GROUP_PREFIX = "@authorGroup:";
	public final static String AUTHOR_PREFIX = "@author:";
	public final static String SERIES_TAG = "@seriesRoot";
	public final static String SERIES_GROUP_PREFIX = "@seriesGroup:";
	public final static String SERIES_PREFIX = "@series:";
	public final static String BOOK_DATE_TAG = "@bookdateRoot";
	public final static String BOOK_DATE_GROUP_PREFIX = "@bookdateGroup:";
	public final static String BOOK_DATE_PREFIX = "@bookdate:";
	public final static String DOC_DATE_TAG = "@docdateRoot";
	public final static String DOC_DATE_GROUP_PREFIX = "@docdateGroup:";
	public final static String DOC_DATE_PREFIX = "@docdate:";
	public final static String PUBL_YEAR_TAG = "@publyearRoot";
	public final static String PUBL_YEAR_GROUP_PREFIX = "@publyearGroup:";
	public final static String PUBL_YEAR_PREFIX = "@publyeardate:";
	public final static String FILE_DATE_TAG = "@filedateRoot";
	public final static String FILE_DATE_GROUP_PREFIX = "@filedateGroup:";
	public final static String FILE_DATE_PREFIX = "@filedate:";
	public final static String RATING_TAG = "@ratingRoot";
	public final static String STATE_TO_READ_TAG = "@stateToReadRoot";
	public final static String STATE_READING_TAG = "@stateReadingRoot";
	public final static String STATE_FINISHED_TAG = "@stateFinishedRoot";
	public final static String TITLE_TAG = "@titlesRoot";
	public final static String TITLE_GROUP_PREFIX = "@titleGroup:";
	public final static String SEARCH_SHORTCUT_TAG = "@search";
	public final static String QSEARCH_SHORTCUT_TAG = "@qsearch";

	public Long id; // db id
	public String title; // book title
	public String authors; // authors, delimited with '|'
	public String series; // series name w/o number
	public int seriesNumber; // number of book inside series
    public int saved_with_ver; // version of database book was saved with
	public boolean need_to_update_ver; // file was saved in old format
	public String genre;
	public String annotation;
	public String srclang;
    private String bookdate;
	public String translator;
	public String docauthor;
	public String docprogram;
	private String docdate;
	public String docsrcurl;
	public String docsrcocr;
	public String docversion;
	public String publname;
	public String publisher;
	public String publcity;
	private String publyear;
	public String publisbn;
    public String publseries; // series name w/o number
    public int publseriesNumber; // number of book inside series
	public long fileCreateTime;
	public long symCount;
	public long wordCount;
	public long bookDateN; //converted to nubmer from string
	public long docDateN;
	public long publYearN;
	public String path; // path to directory where file or archive is located
	public String filename; // file name w/o path for normal file, with optional path for file inside archive 
	public String pathname; // full path+arcname+filename
	public String arcname; // archive file name w/o path
	public String language; // document language
	public String lang_from; // translate from
	public String lang_to; // translate to
	public String username; // username for online catalogs
	public String password; // password for online catalogs
	public DocumentFormat format;
	public int size; // full file size
	public int arcsize; // compressed size
	private long createTime;
	public long lastAccessTime;
	public int flags;
	public boolean isArchive;
	public boolean isDirectory;
	public boolean isListed;
	public boolean isScanned;
	public FileInfo parent; // parent item
	public Object tag; // some additional information
	public boolean askedMarkRead = false; // did we ask to mark book as read
	public boolean askedMarkReading = false; // did we ask to mark book as reading
	public ArrayList<Integer> arrReadBeg = new ArrayList<Integer>();
	private ArrayList<FileInfo> files;// files
	private ArrayList<FileInfo> dirs; // directories
	public boolean isFav; // only for display star in file browser

	// 16 lower bits reserved for document flags
	public static final int DONT_USE_DOCUMENT_STYLES_FLAG = 1;
	public static final int DONT_REFLOW_TXT_FILES_FLAG = 2;
	public static final int USE_DOCUMENT_FONTS_FLAG = 4;
	
	// bits 16..19 - reading state (0..15 max)
	public static final int READING_STATE_SHIFT = 16;
	public static final int READING_STATE_MASK = 0x0F;
	public static final int STATE_NEW = 0;
	public static final int STATE_TO_READ = 1;
	public static final int STATE_READING = 2;
	public static final int STATE_FINISHED = 3;

	// bits 20..23 - rate (0..15 max, 0..5 currently)
	public static final int RATE_SHIFT = 20;
	public static final int RATE_MASK = 0x0F;
	public static final int RATE_VALUE_NOT_RATED = 0;
	public static final int RATE_VALUE_1 = 1;
	public static final int RATE_VALUE_2 = 2;
	public static final int RATE_VALUE_3 = 3;
	public static final int RATE_VALUE_4 = 4;
	public static final int RATE_VALUE_5 = 5;

    //bit 24,25 - info type
    private static final int TYPE_SHIFT = 24;
    private static final int TYPE_MASK = 0x03;
    public static final int TYPE_NOT_SET = 0;
    public static final int TYPE_FS_ROOT = 1;
    public static final int TYPE_DOWNLOAD_DIR = 2;

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getFileCreateTime() {
		return fileCreateTime;
	}

	public void setFileCreateTime(long fileCreateTime) {
		this.fileCreateTime = fileCreateTime;
	}

	public String getBookdate() {
		return bookdate;
	}

	public String getDocdate() {
		return docdate;
	}

	public String getPublyear() {
		return publyear;
	}

	/**
	 * Get book reading state.
	 * @return reading state (one of STATE_XXX constants)
	 */
	public int getReadingState() {
        return getBitValue(READING_STATE_SHIFT,READING_STATE_MASK);
    }

	/**
	 * Set new reading state.
	 * @param state is new reading state (one of STATE_XXX constants)
	 */
	public boolean setReadingState(int state) {
        return setBitValue(state, READING_STATE_SHIFT, READING_STATE_MASK);
	}

	/**
	 * Get book reading state.
	 * @return reading state (one of STATE_XXX constants)
	 */
	public int getRate() {
        return getBitValue(RATE_SHIFT, RATE_MASK);
    }

	/**
	 * Set new rate.
	 * @param rate is new rate (one of RATE_XXX constants)
	 */
	public boolean setRate(int rate) {
        return setBitValue(rate, RATE_SHIFT, RATE_MASK);
	}

    /**
   	 * Get FileInfo type.
   	 * @return folder type (one of TYPE_XXX constants)
   	 */
   	public int getType() {
        return getBitValue(TYPE_SHIFT, TYPE_MASK);
    }

   	/**
   	 * Set FileInfo type.
   	 * @param type is new type
   	 */
   	public boolean setType(int type) {
        return setBitValue(type, TYPE_SHIFT, TYPE_MASK);
   	}

	/**
	 * To separate archive name from file name inside archive.
	 */
	public static final String ARC_SEPARATOR = "@/";
	
	public static final int PROFILE_ID_SHIFT = 16;
	public static final int PROFILE_ID_MASK = 0x0F;
	
	
	public void setFlag( int flag, boolean value ) {
		flags = flags & (~flag) | (value? flag : 0);
	}
	
	public boolean getFlag( int flag ) {
		return (flags & flag)!=0;
	}
	
	public int getProfileId() {
        return getBitValue(PROFILE_ID_SHIFT,PROFILE_ID_MASK);
    }

    public void setProfileId(int id) {
        setBitValue(id,PROFILE_ID_SHIFT,PROFILE_ID_MASK);
	}

    private boolean setBitValue(int value, int shift, int mask) {
        int oldFlags = flags;
        flags = (flags & ~(mask << shift))
                | ((value & mask) << shift);
        return flags != oldFlags;
    }

    private int getBitValue(int shift, int mask) {
        return (flags >> shift) & mask;
    }

    public String getTitleOrFileName() {
		if (title != null && title.length() > 0)
			return title;
		if (authors != null && authors.length() > 0)
			return "";
		if (series != null && series.length() > 0)
			return "";
		return filename;
	}
	
	/**
	 * Split archive + file path name by ARC_SEPARATOR
	 * @param pathName is pathname like /arc_file_path@/filepath_inside_arc or /file_path 
	 * @return item[0] is pathname, item[1] is archive name (null if no archive)
	 */
	public static String[] splitArcName( String pathName )
	{
		String[] res = new String[2];
		int arcSeparatorPos = pathName.indexOf(ARC_SEPARATOR);
		if ( arcSeparatorPos>=0 ) {
			// from archive
			res[1] = pathName.substring(0, arcSeparatorPos);
			res[0] = pathName.substring(arcSeparatorPos + ARC_SEPARATOR.length());
		} else {
			res[0] = pathName;
		}
		return res;
	}
	
	public FileInfo( String pathName )
	{
		String[] parts = splitArcName( pathName );
		if ( parts[1]!=null ) {
			// from archive
			isArchive = true;
			arcname = parts[1];
			pathname = parts[0];
			File f = new File(pathname);
			filename = f.getName();
			fileCreateTime = f.lastModified();
			path = f.getPath();
			File arc = new File(arcname);
			if (arc.isFile() && arc.exists()) {
				arcsize = (int)arc.length();
				size = arcsize;
				isArchive = true;
				try {
					//ZipFile zip = new ZipFile(new File(arcname));
					ArrayList<ZipEntry> entries = Services.getEngine().getArchiveItems(arcname);
					//for ( Enumeration<?> e = zip.entries(); e.hasMoreElements(); ) {
					for (ZipEntry entry : entries) {
						String name = entry.getName();
						
						if ( !entry.isDirectory() && pathname.equals(name) ) {
							File itemf = new File(name);
							filename = itemf.getName();
							path = itemf.getPath();
							format = DocumentFormat.byExtension(name);
							//size = (int)entry.getSize();//plotn - guess this is same as compressed...
							arcsize = (int)entry.getCompressedSize();
							createTime = entry.getTime();
							break;
						}
					}
				} catch ( Exception e ) {
					Log.e("cr3", "error while reading contents of " + arcname);
				}
			}
		} else {
			fromFile(new File(pathName));
		}
	}
	
	public String getFileNameToDisplay() {
		boolean isSingleFileArchive = (isArchive && parent!=null && !parent.isArchive && arcname!=null);
		return isSingleFileArchive
			? new File(arcname).getName() : filename;
	}
	
	private void fromFile( File f )
	{
		fileCreateTime = f.lastModified();
		if ( !f.isDirectory() ) {
			DocumentFormat fmt = DocumentFormat.byExtension(f.getName());
			filename = f.getName();
			path = f.getParent();
			pathname = f.getAbsolutePath();
			format = fmt;
			createTime = f.lastModified();
			size = (int)f.length();
		} else {
			filename = f.getName();
			path = f.getParent();
			pathname = f.getAbsolutePath();
			isDirectory = true;
		}
	}
	
	public FileInfo( File f )
	{
		fromFile(f);
		need_to_update_ver = false;
	}
	
	public FileInfo()
	{
		need_to_update_ver = false;
	}

	/// doesn't copy parent and children
	public FileInfo(FileInfo v)
	{
		assign(v);
		need_to_update_ver = false;
	}

	public void assign(FileInfo v)
	{
		title = v.title;
		authors = v.authors;
		series = v.series;
		seriesNumber = v.seriesNumber;
		path = v.path;
		filename = v.filename;
		pathname = v.pathname;
		arcname = v.arcname;
		format = v.format;
		flags = v.flags;
		size = v.size;
		arcsize = v.arcsize;
		isArchive = v.isArchive;
		isDirectory = v.isDirectory;
		createTime = v.createTime;
		lastAccessTime = v.lastAccessTime;
		language = v.language;
		lang_from = v.lang_from;
		lang_to = v.lang_to;
		username = v.username;
		password = v.password;
		id = v.id;
		saved_with_ver = v.saved_with_ver;
		genre = v.genre;
		annotation = v.annotation;
		srclang = v.srclang;
		setBookdate(v.bookdate);
		translator = v.translator;
		docauthor = v.docauthor;
		docprogram = v.docprogram;
		setDocdate(v.docdate);
		docsrcurl = v.docsrcurl;
		docsrcocr = v.docsrcocr;
		docversion = v.docversion;
		publname = v.publname;
		publisher = v.publisher;
		publcity = v.publcity;
		setPublyear(v.publyear);
		publisbn = v.publisbn;
        publseries = v.publseries;
        publseriesNumber = v.publseriesNumber;
        fileCreateTime = v.fileCreateTime;
    }
	
	/**
	 * @return archive file path and name, null if this object is neither archive nor a file inside archive
	 */
	public String getArchiveName()
	{
		return arcname;
	}
	
	/**
	 * @return file name inside archive, null if this object is not a file inside archive
	 */
	public String getArchiveItemName()
	{
		if ( isArchive && !isDirectory && pathname!=null )
			return pathname;
		return null;
	}
	
	public boolean isRecentDir()
	{
		return RECENT_DIR_TAG.equals(pathname);
	}
	
	public boolean isSearchDir()
	{
		return SEARCH_RESULT_DIR_TAG.equals(pathname);
	}
	
	public boolean isRootDir()
	{
		return ROOT_DIR_TAG.equals(pathname);
	}
	
	public boolean isSpecialDir()
	{
		return pathname!=null && pathname.startsWith("@");
	}
	
	public boolean isOnlineCatalogPluginDir()
	{
		return pathname!=null && pathname.startsWith(ONLINE_CATALOG_PLUGIN_PREFIX);
	}
	
	public boolean isOnlineCatalogPluginBook()
	{
		return !isDirectory && pathname != null && pathname.startsWith(ONLINE_CATALOG_PLUGIN_PREFIX) && getOnlineStoreBookInfo() != null;
	}
	
	public boolean isOPDSDir()
	{
		return pathname!=null && pathname.startsWith(OPDS_DIR_PREFIX) && (getOPDSEntryInfo() == null || getOPDSEntryInfo().getBestAcquisitionLink() == null);
	}
	
	public boolean isOPDSBook()
	{
		return pathname!=null && pathname.startsWith(OPDS_DIR_PREFIX) && getOPDSEntryInfo() != null && getOPDSEntryInfo().getBestAcquisitionLink() != null;
	}
	
	private OPDSUtil.EntryInfo getOPDSEntryInfo() {
		if (tag !=null && tag instanceof OPDSUtil.EntryInfo)
			return (OPDSUtil.EntryInfo)tag;
		return null;
	}
	
	public OnlineStoreBook getOnlineStoreBookInfo() {
		if (tag !=null && tag instanceof OnlineStoreBook)
			return (OnlineStoreBook)tag;
		return null;
	}
	
	public boolean isOPDSRoot()
	{
		return OPDS_LIST_TAG.equals(pathname);
	}
	
	public boolean isSearchShortcut()
	{
		return SEARCH_SHORTCUT_TAG.equals(pathname);
	}

	public boolean isQSearchShortcut()
	{
		return QSEARCH_SHORTCUT_TAG.equals(pathname);
	}
	
	public boolean isBooksByAuthorRoot()
	{
		return AUTHORS_TAG.equals(pathname);
	}
	
	public boolean isBooksBySeriesRoot()
	{
		return SERIES_TAG.equals(pathname);
	}

	public boolean isBooksByBookdateRoot()
	{
		return BOOK_DATE_TAG.equals(pathname);
	}

	public boolean isBooksByDocdateRoot()
	{
		return DOC_DATE_TAG.equals(pathname);
	}

	public boolean isBooksByPublyearRoot()
	{
		return PUBL_YEAR_TAG.equals(pathname);
	}

	public boolean isBooksByFiledateRoot()
	{
		return FILE_DATE_TAG.equals(pathname);
	}
	
	public boolean isBooksByRatingRoot()
	{
		return RATING_TAG.equals(pathname);
	}
	
	public boolean isBooksByStateToReadRoot()
	{
		return STATE_TO_READ_TAG.equals(pathname);
	}
	
	public boolean isBooksByStateReadingRoot()
	{
		return STATE_READING_TAG.equals(pathname);
	}
	
	public boolean isBooksByStateFinishedRoot()
	{
		return STATE_FINISHED_TAG.equals(pathname);
	}
	
	public boolean isBooksByTitleRoot()
	{
		return TITLE_TAG.equals(pathname);
	}
	
	public boolean isBooksByAuthorDir()
	{
		return pathname!=null && pathname.startsWith(AUTHOR_PREFIX);
	}
	
	public boolean isBooksBySeriesDir()
	{
		return pathname!=null && pathname.startsWith(SERIES_PREFIX);
	}

	public boolean isBooksByBookdateDir()
	{
		return pathname!=null && pathname.startsWith(BOOK_DATE_PREFIX);
	}

	public boolean isBooksByDocdateDir()
	{
		return pathname!=null && pathname.startsWith(DOC_DATE_PREFIX);
	}

	public boolean isBooksByPublyearDir()
	{
		return pathname!=null && pathname.startsWith(PUBL_YEAR_PREFIX);
	}

	public boolean isBooksByFiledateDir()
	{
		return pathname!=null && pathname.startsWith(FILE_DATE_PREFIX);
	}
	
	public long getAuthorId()
	{
		if (!isBooksByAuthorDir())
			return 0;
		return id;
	}
	
	public long getSeriesId()
	{
		if (!isBooksBySeriesDir())
			return 0;
		return id;
	}

	public long getBookdateId()
	{
		if (!isBooksByBookdateDir())
			return 0;
		return id;
	}
	public long getDocdateId()
	{
		if (!isBooksByDocdateDir())
			return 0;
		return id;
	}

	public long getPublyearId()
	{
		if (!isBooksByPublyearDir())
			return 0;
		return id;
	}

	public long getFiledateId()
	{
		if (!isBooksByFiledateDir())
			return 0;
		return id;
	}
	
	public boolean isHidden()
	{
		return pathname.startsWith(".");
	}
	
	public String getOPDSUrl()
	{
		if ( !pathname.startsWith(OPDS_DIR_PREFIX) )
			return null;
		return pathname.substring(OPDS_DIR_PREFIX.length());
	}
	
	public String getOnlineCatalogPluginPackage()
	{
		if ( !pathname.startsWith(ONLINE_CATALOG_PLUGIN_PREFIX) )
			return null;
		String s = pathname.substring(ONLINE_CATALOG_PLUGIN_PREFIX.length());
		int p = s.indexOf(":");
		if (p < 0)
			return s;
		else
			return s.substring(0, p);
	}
	
	public String getOnlineCatalogPluginPath()
	{
		if ( !pathname.startsWith(ONLINE_CATALOG_PLUGIN_PREFIX) )
			return null;
		String s = pathname.substring(ONLINE_CATALOG_PLUGIN_PREFIX.length());
		int p = s.indexOf(":");
		if (p < 0)
			return null;
		else
			return s.substring(p + 1);
	}
	
	public String getOnlineCatalogPluginId()
	{
		String s = getOnlineCatalogPluginPath();
		if (s == null)
			return null;
		int p = s.indexOf("=");
		if (p < 0)
			return null;
		else
			return s.substring(p + 1);
	}
	
	/**
	 * Get absolute path to file.
	 * For plain files, returns /abs_path_to_file/filename.ext
	 * For archives, returns /abs_path_to_archive/arc_file_name.zip@/filename_inside_archive.ext
	 * @return full path + filename
	 */
	public String getPathName()
	{
		if ( arcname!=null )
			return arcname + ARC_SEPARATOR + pathname;
		return pathname;
	}

	public String getBasePath()
	{
		if ( arcname!=null )
			return arcname;
		return pathname;
	}

	public int dirCount()
	{
		return dirs!=null ? dirs.size() : 0;
	}

	public int fileCount()
	{
		return files!=null ? files.size() : 0;
	}

	public int itemCount()
	{
		return dirCount() + fileCount();
	}

	public void addDir( FileInfo dir )
	{
		if ( dirs==null )
			dirs = new ArrayList<FileInfo>();
		dirs.add(dir);
		if (dir.parent == null)
			dir.parent = this;
	}
	public void addFile( FileInfo file )
	{
		if ( files==null )
			files = new ArrayList<FileInfo>();
		files.add(file);
	}
	public void addItems( Collection<FileInfo> items )
	{
		for ( FileInfo item : items ) {
			if ( item.isDirectory )
				addDir(item);
			else
				addFile(item);
			item.parent = this;
		}
	}
	public void replaceItems( Collection<FileInfo> items )
	{
		files = null;
		dirs = null;
		addItems( items );
	}
	public boolean isEmpty()
	{
		return fileCount()==0 && dirCount()==0;
	}
	public FileInfo getItem( int index )
	{
		if ( index<0 )
			throw new IndexOutOfBoundsException();
		if ( index<dirCount())
			return dirs.get(index);
		index -= dirCount();
		if ( index<fileCount())
			return files.get(index);
		Log.e("cr3", "Index out of bounds " + index + " at FileInfo.getItem() : returning 0");
		//throw new IndexOutOfBoundsException();
		return null;
	}
	public FileInfo findItemByPathName( String pathName )
	{
		if ( dirs!=null )
			for ( FileInfo dir : dirs )
				if ( pathName.equals(dir.getPathName() ))
					return dir;
		if ( files!=null )
			for ( FileInfo file : files ) {
				if ( pathName.equals(file.getPathName() ))
					return file;
				if ( file.getPathName().startsWith(pathName+"@/" ))
					return file;
			}
		return null;
	}

	public static boolean eq(String s1, String s2) {
		if (s1 == null)
			return s2 == null;
		return s1.equals(s2);
	}
	
	public boolean pathNameEquals(FileInfo item) {
		return isDirectory == item.isDirectory && eq(arcname, item.arcname) && eq(pathname, item.pathname);
	}
	
	public boolean hasItem(FileInfo item) {
		return getItemIndex(item) >= 0;
	}
	
	public int getItemIndex( FileInfo item )
	{
		if ( item==null )
			return -1;
		for ( int i=0; i<dirCount(); i++ ) {
			if ( item.pathNameEquals(getDir(i)) )
				return i;
		}
		for ( int i=0; i<fileCount(); i++ ) {
			if (item.pathNameEquals(getFile(i)))
				return i + dirCount();
		}
		return -1;
	}

	public int getFileIndex( FileInfo item )
	{
		if ( item==null )
			return -1;
		for ( int i=0; i<fileCount(); i++ ) {
			if (item.pathNameEquals(getFile(i)))
				return i;
		}
		return -1;
	}

	public FileInfo getDir( int index )
	{
		if ( index<0 )
			throw new IndexOutOfBoundsException();
		if ( index<dirCount())
			return dirs.get(index);
		throw new IndexOutOfBoundsException();
	}

	public FileInfo getFile( int index )
	{
		if ( index<0 )
			throw new IndexOutOfBoundsException();
		if ( index<fileCount())
			return files.get(index);
		throw new IndexOutOfBoundsException();
	}

	public boolean setFileProperties(FileInfo file)
	{
		boolean modified = false;
		modified = setTitle(file.getTitle()) || modified;
		modified = setAuthors(file.getAuthors()) || modified;
		modified = setSeriesName(file.getSeriesName()) || modified;
		modified = setSeriesNumber(file.getSeriesNumber()) || modified;
		modified = setReadingState(file.getReadingState()) || modified;
		modified = setRate(file.getRate()) || modified;
		return modified;
	}

    public void setFile(int index, FileInfo file)
    {
        if ( index<0 )
			throw new IndexOutOfBoundsException();
		if (index < fileCount()) {
			files.set(index, file);
			file.parent = this;
			return;
		}
		throw new IndexOutOfBoundsException();
    }
	
	public void setFile(FileInfo file)
	{
		int index = getFileIndex(file);
		if ( index<0 )
			return;
		setFile(index, file);
	}

	public void setItems(FileInfo copyFrom)
	{
		clear();
		for (int i=0; i<copyFrom.fileCount(); i++) {
			addFile(copyFrom.getFile(i));
			copyFrom.getFile(i).parent = this;
		}
		for (int i=0; i<copyFrom.dirCount(); i++) {
			addDir(copyFrom.getDir(i));
			copyFrom.getDir(i).parent = this;
		}
	}

	public void setItems(Collection<FileInfo> list)
	{
		clear();
		if (list == null)
			return;
		for (FileInfo item : list) {
			if (item.isDirectory)
				addDir(item);
			else
				addFile(item);
			item.parent = this;
		}
	}

	public void removeEmptyDirs()
	{
		if ( parent==null || pathname.startsWith("@") || !isListed || dirs==null )
			return;
		for ( int i=dirCount()-1; i>=0; i-- ) {
			FileInfo dir = getDir(i);
			if ( dir.isListed && dir.dirCount() == 0 && dir.fileCount() == 0)
				dirs.remove(i);
		}
	}
	
	public void removeChild( FileInfo item )
	{
		if ( item.isSpecialDir() )
			return;
		if ( files!=null ) {
			int n = files.indexOf(item);
			if ( n>=0 && n<files.size() ) {
				files.remove(n);
				return;
			}
		}
		if ( dirs!=null ) {
			int n = dirs.indexOf(item);
			if ( n>=0 && n<dirs.size() ) {
				dirs.remove(n);
			}
		}
	}
	
	public boolean deleteFile()
	{
		if ( isArchive ) {
			if ( isDirectory )
				return false;
			File f = new File(arcname);
			if ( f.exists() && !f.isDirectory() ) {
				if ( !f.delete() )
					return false;
				if ( parent!=null ) {
					if ( parent.isArchive ) {
						// remove all files belonging to this archive
					} else {
						parent.removeChild(this);
					}
				}
				return true;
			}
		}
		if ( isDirectory )
			return false;
		if ( !fileExists() )
			return false;
		File f = new File(pathname);
		if ( f.delete() ) {
			if ( parent!=null ) {
				parent.removeChild(this);
			}
			return true;
		}
		return false;
	}

	public int deleteFileDocTree(Context context, Uri sdCardUri) {
		File file = this.getFile();
		if (file!=null) {
			DocumentFile documentFile = DocumentFile.fromTreeUri(context, sdCardUri);
			String[] parts = file.getPath().split("\\/");
			for (int i = 3; i < parts.length; i++) {
				if (documentFile != null) {
					documentFile = documentFile.findFile(parts[i]);
				}
			}
			if (documentFile != null) {
				if (documentFile.delete()) return 1;
			} else {
				return -1;
			}
		}
		return 0;
	}

	public boolean fileExists()
	{
		if (isDirectory)
			return false;
		if ( isArchive ) {
			if ( arcname!=null )
				return new File(arcname).exists();
			return false;
		}
		return new File(pathname).exists();
	}

	public File getFile()
	{
		if (isDirectory)
			return null;
		if ( isArchive ) {
			if ( arcname!=null )
				return new File(arcname);
			return null;
		}
		return new File(pathname);
	}

	public long fileLastModified()
	{
		if (isDirectory)
			return 0;
		if ( isArchive ) {
			if ( arcname!=null )
				return new File(arcname).lastModified();
			return 0;
		}
		return new File(pathname).lastModified();
	}
	
	/**
	 * @return true if item (file, directory, or archive) exists
	 */
	public boolean exists()
	{
		if ( isArchive ) {
			if ( arcname==null )
				return false;
			File f = new File(arcname);
			return f.exists();
		}
		File f = new File(pathname);
		return f.exists();
	}
	
	/**
	 * @return true if item is a directory, which exists and can be written to
	 */
	public boolean isWritableDirectory()
	{
		if (!isDirectory || isArchive || isSpecialDir())
			return false;
		File f = new File(pathname);
		boolean isDir = f.isDirectory();
		boolean canWr = f.canWrite();
//		if (!canWr) {
//			File testFile = new File(f, "cr3test.tmp");
//			try {
//				OutputStream os = new FileOutputStream(testFile, false);
//				os.close();
//				testFile.delete();
//				canWr = true;
//			} catch (FileNotFoundException e) {
//				L.e("cannot write " + testFile, e);
//			} catch (IOException e) {
//				L.e("cannot write " + testFile, e);
//			}
//		}
		return isDir && canWr;
	}
	
	/**
	 * @return true if item is a directory, which exists and can be written to
	 */
	public boolean isReadableDirectory()
	{
		if (!isDirectory || isArchive || isSpecialDir())
			return false;
		File f = new File(pathname);
		boolean isDir = f.isDirectory();
		boolean canRd = f.canRead();
		return isDir && canRd;
	}
	
	public String getAuthors() {
		if (authors!=null) {
			String[] list = authors.split("\\|");
			ArrayList<String> arrS = new ArrayList<String>();
			for (String s : list) {
				s = s.replaceAll("\\s+", " ").trim();
				if (!arrS.contains(s)) arrS.add(s);
			}
			String resS = "";
			for (String s : arrS) {
				resS = resS + "|" + s;
			}
			if (resS.length() > 0) resS = resS.substring(1);
			return resS;
		}
		return null;
	}
	
	public boolean setAuthors(String authors) {
		if (eq(this.authors, authors))
			return false;
		if (authors!=null) {
			String[] list = authors.split("\\|");
			ArrayList<String> arrS = new ArrayList<String>();
			for (String s : list) {
				s = s.replaceAll("\\s+", " ").trim();
				if (!arrS.contains(s)) arrS.add(s);
			}
			String resS = "";
			for (String s : arrS) {
				resS = resS + "|" + s;
			}
			if (resS.length() > 0) resS = resS.substring(1);
			this.authors = resS;
		} else this.authors = null;
		return true;
	}
	
	public String getTitle() {
		return title;
	}
	
	public boolean setTitle(String title) {
		if (eq(this.title, title))
			return false;
		this.title = title;
		return true;
	}
	
	public String getSeriesName() {
		return series;
	}
	
	public boolean setSeriesName(String series) {
		if (eq(this.series, series))
			return false;
		this.series = series;
		return true;
	}

	public boolean setLangFrom(String lang) {
		if (eq(this.lang_from, lang))
			return false;
		this.lang_from = lang;
		return true;
	}

	public boolean setLangTo(String lang) {
		if (eq(this.lang_to, lang))
			return false;
		this.lang_to = lang;
		return true;
	}
	
	public boolean setSeriesNumber(int seriesNumber) {
		if (this.seriesNumber == seriesNumber)
			return false;
		this.seriesNumber = seriesNumber;
		return true;
	}

	public boolean setGenre(String genre) {
		if (eq(this.genre, genre))
			return false;
		this.genre = genre;
		return true;
	}

	public boolean setAnnotation(String annotation) {
		if (eq(this.annotation, annotation))
			return false;
		this.annotation = annotation;
		return true;
	}

	public boolean setSrclang(String srclang) {
		if (eq(this.srclang, srclang))
			return false;
		this.srclang = srclang;
		return true;
	}

	public boolean setBookdate(String bookdate) {
		if (eq(this.bookdate, bookdate))
			return false;
		this.bookdate = bookdate;
		this.bookDateN = StrUtils.parseDateLong(bookdate);
		return true;
	}

	public boolean setTranslator(String translator) {
		if (eq(this.translator, translator))
			return false;
		this.translator = translator;
		return true;
	}

	public boolean setDocauthor(String docauthor) {
		if (eq(this.docauthor, docauthor))
			return false;
		this.docauthor = docauthor;
		return true;
	}

	public boolean setDocprogram(String docprogram) {
		if (eq(this.docprogram, docprogram))
			return false;
		this.docprogram = docprogram;
		return true;
	}

	public boolean setDocdate(String docdate) {
		if (eq(this.docdate, docdate))
			return false;
		this.docdate = docdate;
		this.docDateN = StrUtils.parseDateLong(docdate);
		return true;
	}

	public boolean setDocsrcurl(String docsrcurl) {
		if (eq(this.docsrcurl, docsrcurl))
			return false;
		this.docsrcurl = docsrcurl;
		return true;
	}

	public boolean setDocsrcocr(String docsrcocr) {
		if (eq(this.docsrcocr, docsrcocr))
			return false;
		this.docsrcocr = docsrcocr;
		return true;
	}

	public boolean setDocversion(String docversion) {
		if (eq(this.docversion, docversion))
			return false;
		this.docversion = docversion;
		return true;
	}

	public boolean setPublname(String publname) {
		if (eq(this.publname, publname))
			return false;
		this.publname = publname;
		return true;
	}

	public boolean setPublisher(String publisher) {
		if (eq(this.publisher, publisher))
			return false;
		this.publisher = publisher;
		return true;
	}

	public boolean setPublcity(String publcity) {
		if (eq(this.publcity, publcity))
			return false;
		this.publcity = publcity;
		return true;
	}

	public boolean setPublyear(String publyear) {
		if (eq(this.publyear, publyear))
			return false;
		this.publyear = publyear;
		this.publYearN = StrUtils.parseDateLong(publyear);
		return true;
	}

	public boolean setPublisbn(String publisbn) {
		if (eq(this.publisbn, publisbn))
			return false;
		this.publisbn = publisbn;
		return true;
	}

	public boolean setPublseries(String publseries) {
		if (eq(this.publseries, publseries))
			return false;
		this.publseries = publseries;
		return true;
	}

	public boolean setPublseriesNumber(int publseriesNumber) {
		if (this.publseriesNumber == publseriesNumber)
			return false;
		this.publseriesNumber = publseriesNumber;
		return true;
	}

	public int getSeriesNumber() {
		return series != null && series.length() > 0 ? seriesNumber : 0;
	}
	
	public String getLanguage() {
		return language;
	}

	public String getLang_from() {
		return lang_from;
	}

	public String getLang_to() {
		return lang_to;
	}

	public void clear()
	{
		dirs = null;
		files = null;
	}
	
	public static enum SortOrder {
		FILENAME(R.string.mi_book_sort_order_filename, new Comparator<FileInfo>() {
			public int compare( FileInfo f1, FileInfo f2 )
			{
				if ( f1==null || f2==null )
					return 0;
				return Utils.cmp(f1.getFileNameToDisplay(), f2.getFileNameToDisplay());
			}
		}),
		FILENAME_DESC(R.string.mi_book_sort_order_filename_desc, new Comparator<FileInfo>() {
			public int compare( FileInfo f1, FileInfo f2 )
			{
				if ( f1==null || f2==null )
					return 0;
				return Utils.cmp(f2.getFileNameToDisplay(), f1.getFileNameToDisplay());
			}
		}),
		TIMESTAMP(R.string.mi_book_sort_order_timestamp, new Comparator<FileInfo>() {
			public int compare( FileInfo f1, FileInfo f2 )
			{
				if ( f1==null || f2==null )
					return 0;
				return firstNz( cmp(f1.createTime, f2.createTime), Utils.cmp(f1.filename, f2.filename) );
			}
		}),
		TIMESTAMP_DESC(R.string.mi_book_sort_order_timestamp_desc, new Comparator<FileInfo>() {
			public int compare( FileInfo f1, FileInfo f2 )
			{
				if ( f1==null || f2==null )
					return 0;
				return firstNz( cmp(f2.createTime, f1.createTime), Utils.cmp(f2.filename, f1.filename) );
			}
		}),
		AUTHOR_TITLE(R.string.mi_book_sort_order_author, new Comparator<FileInfo>() {
			public int compare( FileInfo f1, FileInfo f2 )
			{
				if ( f1==null || f2==null )
					return 0;
				return firstNz(
						cmpNotNullFirst(Utils.formatAuthors(f1.authors), Utils.formatAuthors(f2.authors))
						,cmpNotNullFirst(f1.series, f2.series)
						,cmp(f1.getSeriesNumber(), f2.getSeriesNumber())
						,cmpNotNullFirst(f1.title, f2.title)
						,Utils.cmp(f1.filename, f2.filename) 
						);
			}
		}),
		AUTHOR_TITLE_DESC(R.string.mi_book_sort_order_author_desc, new Comparator<FileInfo>() {
			public int compare( FileInfo f1, FileInfo f2 )
			{
				if ( f1==null || f2==null )
					return 0;
				return firstNz(
						cmpNotNullFirst(Utils.formatAuthors(f2.authors), Utils.formatAuthors(f1.authors))
						,cmpNotNullFirst(f2.series, f1.series)
						,cmp(f2.getSeriesNumber(), f1.getSeriesNumber())
						,cmpNotNullFirst(f2.title, f1.title)
						,Utils.cmp(f2.filename, f1.filename)
				);
			}
		}),
		TITLE_AUTHOR(R.string.mi_book_sort_order_title, new Comparator<FileInfo>() {
			public int compare( FileInfo f1, FileInfo f2 )
			{
				if ( f1==null || f2==null )
					return 0;
				return firstNz(
						cmpNotNullFirst(f1.series, f2.series)
						,cmp(f1.getSeriesNumber(), f2.getSeriesNumber())
						,cmpNotNullFirst(f1.title, f2.title)
						,cmpNotNullFirst(Utils.formatAuthors(f1.authors), Utils.formatAuthors(f2.authors))
						,Utils.cmp(f1.filename, f2.filename) 
						);
			}
		}),
		TITLE_AUTHOR_DESC(R.string.mi_book_sort_order_title_desc, new Comparator<FileInfo>() {
			public int compare( FileInfo f1, FileInfo f2 )
			{
				if ( f1==null || f2==null )
					return 0;
				return firstNz(
						cmpNotNullFirst(f2.series, f1.series)
						,cmp(f2.getSeriesNumber(), f1.getSeriesNumber())
						,cmpNotNullFirst(f2.title, f1.title)
						,cmpNotNullFirst(Utils.formatAuthors(f2.authors), Utils.formatAuthors(f1.authors))
						,Utils.cmp(f2.filename, f1.filename)
				);
			}
		});
		//================================================
		private final Comparator<FileInfo> comparator;
		public final int resourceId;
		private SortOrder( int resourceId, Comparator<FileInfo> comparator )
		{
			this.resourceId = resourceId;
			this.comparator = comparator;
		}

		public final Comparator<FileInfo> getComparator()
		{
			return comparator;
		}
		
		/**
		 * Same as cmp, but not-null comes first
		 * @param str1
		 * @param str2
		 * @return
		 */
		private static int cmpNotNullFirst( String str1, String str2 )
		{
			if ( str1==null && str2==null )
				return 0;
			if ( str1==null )
				return 1;
			if ( str2==null )
				return -1;
			return Utils.cmp(str1, str2);
		}
		
		static int cmp( long n1, long n2 )
		{
			if ( n1<n2 )
				return -1;
			if ( n1>n2 )
				return 1;
			return 0;
		}
		
		private static int firstNz( int... v)
		{
			for ( int i=0; i<v.length; i++ ) {
				if ( v[i]!=0 )
					return v[i];
			}
			return 0;
		}
		public static SortOrder fromName( String name ) {
			if ( name!=null )
				for ( SortOrder order : values() )
					if ( order.name().equals(name) )
						return order;
			return DEF_SORT_ORDER;
		}
	}
	public final static SortOrder DEF_SORT_ORDER = SortOrder.AUTHOR_TITLE;
		
	public void sort( SortOrder SortOrder )
	{
		if ( dirs!=null ) {
			ArrayList<FileInfo> newDirs = new ArrayList<FileInfo>(dirs);
			Collections.sort( newDirs, SortOrder.getComparator() );
			dirs = newDirs;
		}
		if ( files!=null ) {
			ArrayList<FileInfo> newFiles = new ArrayList<FileInfo>(files);
			Collections.sort( newFiles, SortOrder.getComparator() );
			files = newFiles;
		}
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arcname == null) ? 0 : arcname.hashCode());
		result = prime * result + arcsize;
		result = prime * result + ((authors == null) ? 0 : authors.hashCode());
		result = prime * result + (int) (createTime ^ (createTime >>> 32));
		result = prime * result + ((dirs == null) ? 0 : dirs.hashCode());
		result = prime * result
				+ ((filename == null) ? 0 : filename.hashCode());
		result = prime * result + ((files == null) ? 0 : files.hashCode());
		result = prime * result + flags;
		result = prime * result + ((format == null) ? 0 : format.hashCode());
		result = prime * result + (isArchive ? 1231 : 1237);
		result = prime * result + (isDirectory ? 1231 : 1237);
		result = prime * result + (isListed ? 1231 : 1237);
		result = prime * result + (isScanned ? 1231 : 1237);
		result = prime * result
				+ ((language == null) ? 0 : language.hashCode());
		result = prime * result
				+ ((lang_from == null) ? 0 : lang_from.hashCode());
		result = prime * result
				+ ((lang_to == null) ? 0 : lang_to.hashCode());
		result = prime * result
				+ (int) (lastAccessTime ^ (lastAccessTime >>> 32));
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result
				+ ((pathname == null) ? 0 : pathname.hashCode());
		result = prime * result + ((series == null) ? 0 : series.hashCode());
		result = prime * result + seriesNumber;
		result = prime * result + size;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileInfo other = (FileInfo) obj;
		if (arcname == null) {
			if (other.arcname != null)
				return false;
		} else if (!arcname.equals(other.arcname))
			return false;
		if (arcsize != other.arcsize)
			return false;
		if (authors == null) {
			if (other.authors != null)
				return false;
		} else if (!authors.equals(other.authors))
			return false;
		if (createTime != other.createTime)
			return false;
		if (dirs == null) {
			if (other.dirs != null)
				return false;
		} else if (!dirs.equals(other.dirs))
			return false;
		if (!StrUtils.euqalsIgnoreNulls(filename, other.filename, true)) return false;
		if (files == null) {
			if (other.files != null)
				return false;
		} else if (!files.equals(other.files))
			return false;
		if (flags != other.flags)
			return false;
		if (format != other.format)
			return false;
		if (isArchive != other.isArchive)
			return false;
		if (isDirectory != other.isDirectory)
			return false;
		if (isListed != other.isListed)
			return false;
		if (isScanned != other.isScanned)
			return false;
		if (!StrUtils.euqalsIgnoreNulls(language, other.language, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(lang_from, other.lang_from, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(lang_to, other.lang_to, true)) return false;
		if (lastAccessTime != other.lastAccessTime)
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (!StrUtils.euqalsIgnoreNulls(path, other.path, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(pathname, other.pathname, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(series, other.series, true)) return false;
		if (seriesNumber != other.seriesNumber)
			return false;
		if (size != other.size)
			return false;
		if (!StrUtils.euqalsIgnoreNulls(title, other.title, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(genre, other.genre, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(annotation, other.annotation, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(srclang, other.srclang, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(bookdate, other.bookdate, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(translator, other.translator, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(docauthor, other.docauthor, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(docprogram, other.docprogram, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(docdate, other.docdate, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(docsrcurl, other.docsrcurl, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(docsrcocr, other.docsrcocr, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(docversion, other.docversion, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(publname, other.publname, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(publisher, other.publisher, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(publcity, other.publcity, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(publyear, other.publyear, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(publisbn, other.publisbn, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(publseries, other.publseries, true)) return false;
		if (publseriesNumber != other.publseriesNumber) return false;
		if (fileCreateTime != other.fileCreateTime) return false;
		if (seriesNumber != other.seriesNumber) return false;
		if (bookDateN != other.bookDateN) return false;
		if (docDateN != other.docDateN) return false;
		if (publYearN != other.publYearN) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return pathname;
	}
	
	public boolean allowSorting() {
		return isDirectory && !isRootDir() && !isRecentDir() && !isOPDSDir() && !isBooksBySeriesDir()
				&& !isBooksByBookdateDir()&& !isBooksByDocdateDir()&& !isBooksByPublyearDir()
				&& !isBooksByFiledateDir();
	}
}
