package org.coolreader.crengine;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.litres.LitresSearchParams;
import org.coolreader.plugins.OnlineStoreBook;
import org.coolreader.readerview.ReadingStat;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

public class FileInfo implements Parcelable {

	public final static String RECENT_DIR_TAG = "@recent";
	public final static String SEARCH_RESULT_DIR_TAG = "@searchResults";
	public final static String ROOT_DIR_TAG = "@root";
	public final static String OPDS_LIST_TAG = "@opds";
	public final static String OPDS_DIR_PREFIX = "@opds:";
	public static final String OTG_DIR_PREFIX = "@otg:";
	public final static String CALIBRE_DIR_PREFIX = "@calibre:";
	public final static String LOADING_STUB_PREFIX = "@loadingstub";
	public final static String NOT_FOUND_STUB_PREFIX = "@notfoundstub";
	public final static String ONLINE_CATALOG_PLUGIN_PREFIX = "@plugin:";
	//calibre
	public final static String CALIBRE_LIST_TAG = "@calibre";
	public final static String CALIBRE_BOOKS_PREFIX = "@calibreBooks:";
	//  CR implementation
//	public final static String GENRES_TAG = "@genresRoot";
//	public final static String GENRES_GROUP_PREFIX = "@genresGroup:";
//	public final static String GENRES_PREFIX = "@genre:";
	public final static String LITRES_TAG = "@litresRoot";
	public final static String AUTHORS_TAG = "@authorsRoot";
	public final static String AUTHOR_GROUP_PREFIX = "@authorGroup:";
	public final static String CALIBRE_AUTHOR_GROUP_PREFIX = "@calibreAuthorGroup:";
	public final static String AUTHOR_PREFIX = "@author:";
	public final static String CALIBRE_PREFIX = "@calibre";
	public final static String CALIBRE_AUTHOR_PREFIX = "@calibreAuthor:";
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
	public final static String GENRE_TAG = "@genreRoot";
	public final static String TAG_TAG = "@tagRoot";
	public final static String GENRE_GROUP_PREFIX = "@genreGroup:";
	public final static String TAG_GROUP_PREFIX = "@tagGroup:";
	public final static String GENRE_PREFIX = "@genre:";
	public final static String TAG_PREFIX = "@tag:";
	public final static String RATING_TAG = "@ratingRoot";
	public final static String STATE_TAG = "@stateRoot";
	public final static String STATE_TO_READ_TAG = "@stateToReadRoot";
	public final static String STATE_READING_TAG = "@stateReadingRoot";
	public final static String STATE_FINISHED_TAG = "@stateFinishedRoot";
	public final static String TITLE_TAG = "@titlesRoot";
	public final static String TITLE_TAG_LEVEL = "@titlesLevel";
	public final static String TITLE_GROUP_PREFIX = "@titleGroup:";
	public final static String SEARCH_SHORTCUT_TAG = "@search";
	public final static String ROOT_WINDOW_TAG = "@rootwindow";
	public final static String QSEARCH_SHORTCUT_TAG = "@qsearch";
	public final static String RESCAN_LIBRARY_TAG = "@rescan";
	public final static String CALC_LIBRARY_STATS_TAG = "@calcstats";
	public final static String LITRES_GENRE_TAG = "@litresGenreRoot";
	public final static String LITRES_PERSON_TAG = "@litresPersonRoot";
	public final static String LITRES_PREFIX = "@litres";
	public final static String LITRES_GENRE_GROUP_PREFIX = "@litresGenreGroup:";
	public final static String LITRES_PERSON_GROUP_PREFIX = "@litresPersonGroup:";
	public final static String LITRES_GENRE_PREFIX = "@litresGenre:";
	public final static String LITRES_BOOKS_TAG = "@litresBooksRoot";
	public final static String LITRES_BOOKS_GROUP_PREFIX = "@litresBooksGroup:";
	public final static String LITRES_BOOKS_PREFIX = "@litresBooks:";
	public final static String LITRES_PERSONS_PREFIX = "@litresPersons:";
	public final static String LITRES_PERSONS_GROUP_PREFIX = "@litresPersonsGroup:";
	public final static String LITRES_SEQUENCE_TAG = "@litresSequenceRoot";
	public final static String LITRES_SEQUENCE_PREFIX = "@litresSequence:";
	public final static String LITRES_SEQUENCE_GROUP_PREFIX = "@litresSequenceGroup:";
	public final static String LITRES_COLLECTION_TAG = "@litresCollectionRoot";
	public final static String LITRES_COLLECTION_PREFIX = "@litresCollection:";
	public final static String LITRES_COLLECTION_GROUP_PREFIX = "@litresCollectionGroup:";

	public final static String[] sortTags = {AUTHORS_TAG, AUTHOR_GROUP_PREFIX,
			CALIBRE_AUTHOR_GROUP_PREFIX, AUTHOR_PREFIX, CALIBRE_PREFIX, CALIBRE_AUTHOR_PREFIX,
			SERIES_TAG, SERIES_GROUP_PREFIX, SERIES_PREFIX, BOOK_DATE_TAG, BOOK_DATE_GROUP_PREFIX,
			BOOK_DATE_PREFIX, DOC_DATE_TAG, DOC_DATE_GROUP_PREFIX, DOC_DATE_PREFIX,
		 	PUBL_YEAR_TAG, PUBL_YEAR_GROUP_PREFIX, PUBL_YEAR_PREFIX,
			FILE_DATE_TAG, FILE_DATE_GROUP_PREFIX, FILE_DATE_PREFIX,
			GENRE_TAG, TAG_TAG, GENRE_GROUP_PREFIX, TAG_GROUP_PREFIX,
			GENRE_PREFIX, TAG_PREFIX, RATING_TAG, STATE_TAG,
			STATE_TO_READ_TAG, STATE_READING_TAG, STATE_FINISHED_TAG,
			TITLE_TAG, TITLE_TAG_LEVEL, TITLE_GROUP_PREFIX, SEARCH_SHORTCUT_TAG,
			ROOT_WINDOW_TAG, QSEARCH_SHORTCUT_TAG
	};


	public Long id; // db id
	public String title; // book title
	public String authors; // authors, delimited with '|'
	public String authorsLFM; // authors, delimited with '|'
	public String authorext; // full_name (or first-middle-last)~firstName~middleName~lastName~nickName~homePage~email
	public String series; // series name w/o number
	public int seriesNumber; // number of book inside series
	public String genres; // genre codes, delimited with '|', CR implementation, not KR's!
	public int saved_with_ver; // version of database book was saved with
	public boolean need_to_update_ver; // file was saved in old format
	public String genre; // Genre list written directly
	public String genre_list; // Genre list from genre table
	public String tags; // Tag list written directly
	public String tag_list; // Tag list from tags table
	public String genre_tmp; // Genre - for grouping in order not to calculate many times
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
	public String path; // path to directory where file is located
	private String filename; // file name w/o path for normal file, with optional path for file inside archive
	public String pathname; // full path+filename
	public String arcname; // archive file name with path
	public String language; // document language
	public String description;	// book description, CR implementation (KR use annotation instead)
	public String name_crc32; // crc32 of filename
	public String lang_from; // translate from
	public String lang_to; // translate to
	public String opdsLink;
	public String pathR; // path form opds-assigned entry
	public String pathnameR; // pathname form opds-assigned entry
	public String arcnameR; // arcname form opds-assigned entry
	public String opdsLinkR; // link form opds-assigned entry
	public String opdsLinkRfull; // For litres
	public String username; // username for online catalogs
	public String password; // password for online catalogs
	public String proxy_addr;
	public String proxy_port;
	public String proxy_uname;
	public String proxy_passw;
	public int onion_def_proxy;
	public String remote_folder; // calibre related
	public int cat_type; // calibre related
	// Litres related
	public String cover_href;
	public String cover_href2;
	public String fragment_href;
	public String full_href;
	public String full_href_wo_sid;
	public Double finalPrice = 0D;
	public int free = 0;
	public String availDate;
	public long availDateN;
	public int available = 0;
	public int type = 0;
	public int lvl = 0;
	public int arts_n = 0;
	public LitresSearchParams lsp;
	public String top_arts;
	public String top_genres;
	public String format_chosen;
	public String minage;
	public String subtitle;

	public DocumentFormat format;
	public long size; // full file size
	public long arcsize; // compressed size
	private long createTime;
	public long lastAccessTime;
	public int flags;
	public boolean isArchive;
	public boolean isDirectory;
	public boolean isListed;
	public boolean isScanned;
	public long crc32;
	public int domVersion;
	public int blockRenderingFlags;
	public FileInfo parent; // parent item
	public Object tag; // some additional information
	public boolean askedMarkRead = false; // did we ask to mark book as read
	public boolean askedMarkReading = false; // did we ask to mark book as reading
	public int askedShownStylesInfo = 0; // how much time we've shown styles notification when switched in night mode
	public Long book_downloaded = 0L;
	public int was_error = 0;

	public DocumentFile documentFile; // For OTG, etc
	//public String usbDeviceName; // USB OTG Device
	//public UsbDevice usbDevice; // USB OTG Device

//	public String getUsbDeviceLabel() {
//		if (usbDevice != null) {
//			String serial = "";
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//				try {
//					serial = usbDevice.getSerialNumber();
//				} catch (SecurityException ifPermissionDenied) {
//					// May happen when device is running Android 10 or above.
//				}
//				if (!StrUtils.isEmptyStr(serial)) return "OTG:" + serial;
//				return "OTG:" + usbDeviceName;
//			}
//		}
//		return "";
//	}

	public ArrayList<Integer> arrReadBeg = new ArrayList<Integer>();
	public ArrayList<FileInfo> files;// files
	public ArrayList<FileInfo> dirs; // directories
	public ArrayList<FileInfo> getElements() {
		ArrayList<FileInfo> res = new ArrayList<FileInfo>();
		if (dirs!=null) for (FileInfo d: dirs) res.add(d);
		if (files!=null) for (FileInfo f: files) res.add(f);
		return res;
	}; //files + directories
	public boolean isFav; // only for display star in file browser
	public ArrayList<OPDSUtil.LinkInfo> links = new ArrayList<>(); // for OPDS entries

	//Statistics
	public long lastTimeSaved = 0;
	public int lastPageSet = 0;
	public ArrayList<ReadingStat> stats = new ArrayList<>(); // reading stat

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

	// bits 26..29 - profile id (0..15 max)
	public static final int PROFILE_ID_SHIFT = 26;
	public static final int PROFILE_ID_MASK = 0x0F;

	// NB: CR implementation (of genres)
	// bitmask for field 'tag' when obtained genres list as special folders
	public static final int GENRE_DATA_INCCHILD_MASK = 0x80000000;
	public static final int GENRE_DATA_BOOKCOUNT_MASK = 0x00FFFFFF;

	public static final Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
		@Override
		public FileInfo createFromParcel(Parcel in) {
			return new FileInfo(in);
		}

		@Override
		public FileInfo[] newArray(int size) {
			return new FileInfo[size];
		}
	};

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

	public void setFlag(int flag, boolean value) {
		flags = flags & (~flag) | (value? flag : 0);
	}
	
	public boolean getFlag( int flag ) {
		return (flags & flag)!=0;
	}

	public int getFlags() {
		return flags;
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

	protected FileInfo(Parcel in) {
		if (in.readByte() == 0) {
			id = null;
		} else {
			id = in.readLong();
		}
		title = in.readString();
		authors = in.readString();
		series = in.readString();
		seriesNumber = in.readInt();
		genres = in.readString();
		path = in.readString();
		filename = in.readString();
		pathname = in.readString();
		arcname = in.readString();
		language = in.readString();
		description = in.readString();
		username = in.readString();
		password = in.readString();
		size = in.readLong();
		arcsize = in.readLong();
		createTime = in.readLong();
		lastAccessTime = in.readLong();
		flags = in.readInt();
		isArchive = in.readByte() != 0;
		isDirectory = in.readByte() != 0;
		isListed = in.readByte() != 0;
		isScanned = in.readByte() != 0;
		crc32 = in.readLong();
		domVersion = in.readInt();
		blockRenderingFlags = in.readInt();
		parent = in.readParcelable(FileInfo.class.getClassLoader());
		files = in.createTypedArrayList(FileInfo.CREATOR);
		dirs = in.createTypedArrayList(FileInfo.CREATOR);
	}

	public FileInfo(String pathName)
	{
		if (pathName.startsWith(FileInfo.OPDS_DIR_PREFIX)) {
			filename = pathName;
			name_crc32 = getFNameCRC(filename);
			path = "";
			pathname = pathName;
			isDirectory = true;
		} else {
			String[] parts = splitArcName( pathName );
			if ( parts[1]!=null ) {
				// from archive
				isArchive = true;
				arcname = parts[1];
				pathname = parts[0];
				File f = new File(pathname);
				filename = f.getName();
				name_crc32 = getFNameCRC(filename);
				path = f.getPath();
				File arc = new File(arcname);
				if (arc.isFile() && arc.exists()) {
					arcsize = arc.length();
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
								name_crc32 = getFNameCRC(filename);
								path = itemf.getPath();
								format = DocumentFormat.byExtension(name);
								size = entry.getSize();
								//arcsize = entry.getCompressedSize();
								createTime = entry.getTime();
								break;
							}
						}
					} catch ( Exception e ) {
						Log.e("cr3", "error while reading contents of " + arcname);
					}
				//if (flags == 0) flags = DONT_USE_DOCUMENT_STYLES_FLAG;
				}
			} else {
				fromFile(new File(pathName));
			}
		}
	}
	
	public String getFileNameToDisplay() {
		boolean isSingleFileArchive = (isArchive && parent!=null && !parent.isArchive && arcname!=null);
		return isSingleFileArchive
			? new File(arcname).getName() : filename;
	}

	public String getFileNameToDisplay2() {
		String arcn = "";
		if ((!StrUtils.isEmptyStr(arcname))&&(!StrUtils.isEmptyStr(filename))) {
			arcn = new File(arcname).getName();
			if (!arcn.equals(filename)) return filename + " [" + arcn.replace(filename, "*") + "]";
			return filename;
		}
		if (!StrUtils.isEmptyStr(filename)) return filename;
		if (!StrUtils.isEmptyStr(arcn)) return arcn;
		return "";
	}

	public static String getFNameCRC(String fname) {
		if (fname == null) return "";
		CRC32 crc = new CRC32();
		crc.update(fname.getBytes());
		return String.valueOf(crc.getValue());
	}
	
	private void fromFile( File f )
	{
		fileCreateTime = f.lastModified();
		if ( !f.isDirectory() ) {
			DocumentFormat fmt = DocumentFormat.byExtension(f.getName());
			filename = f.getName();
			name_crc32 = getFNameCRC(filename);
			path = f.getParent();
			pathname = f.getAbsolutePath();
			format = fmt;
			if (format == null) format = DocumentFormat.NONE;
			createTime = f.lastModified();
			size = f.length();
			domVersion = Engine.DOM_VERSION_CURRENT;
			blockRenderingFlags = Engine.BLOCK_RENDERING_FLAGS_WEB;
			//if (StrUtils.isEmptyStr(title)) title = filename; // Dont work properly, think later
			//if (flags == 0) flags = DONT_USE_DOCUMENT_STYLES_FLAG;
		} else {
			filename = f.getName();
			name_crc32 = getFNameCRC(filename);
			path = f.getParent();
			pathname = f.getAbsolutePath();
			isDirectory = true;
		}
		File parent_ = f.getParentFile();
		if (null != parent_)
			parent = new FileInfo(parent_);
	}
	
	public FileInfo(File f)
	{
		fromFile(f);
		need_to_update_ver = false;
	}
	
	public FileInfo()
	{
		need_to_update_ver = false;
		domVersion = Engine.DOM_VERSION_CURRENT;
		blockRenderingFlags = Engine.BLOCK_RENDERING_FLAGS_WEB;
		fileCreateTime = System.currentTimeMillis();
	}

	/// doesn't copy parent and children
	public FileInfo(FileInfo v)
	{
		assign(v);
		if (fileCreateTime == 0L) fileCreateTime = System.currentTimeMillis();
		need_to_update_ver = false;
	}

	public void assign(FileInfo v)
	{
		if (v == null) return;
		title = v.title;
		authors = v.authors;
		authorext = v.authorext;
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
		genres = v.genres; // CR implementation
		description = v.description; // CR implementation
		name_crc32 = v.name_crc32;
		lang_from = v.lang_from;
		lang_to = v.lang_to;
		username = v.username;
		password = v.password;
		proxy_addr = v.proxy_addr;
		proxy_port = v.proxy_port;
		proxy_uname = v.proxy_uname;
		proxy_passw = v.proxy_passw;
		onion_def_proxy = v.onion_def_proxy;
		crc32 = v.crc32;
		domVersion = v.domVersion;
		blockRenderingFlags = v.blockRenderingFlags;
		id = v.id;
		saved_with_ver = v.saved_with_ver;
		genre = v.genre;
		genre_list = v.genre_list;
		tags = v.tags;
		tag_list = v.tag_list;
		//if (StrUtils.isEmptyStr(genre_list)) genre_list = v.genre;
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
		if (fileCreateTime == 0L) fileCreateTime = System.currentTimeMillis();
        opdsLink = v.opdsLink;
		isArchive = v.isArchive;
        wordCount = v.wordCount;
		symCount = v.symCount;
		links = v.links;
		authorsLFM = v.authorsLFM;
		genre_list = v.genre_list;
		tag_list = v.tag_list;
		crc32 = v.crc32;
		description = v.description;
		domVersion = v.domVersion;
		blockRenderingFlags = v.blockRenderingFlags;
		description = v.description;
		name_crc32 = v.name_crc32;
//		public long bookDateN; //converted to number from string
//		public long docDateN;
//		public long publYearN;
//		public String path; // path to directory where file or archive is located
//		private String filename; // file name w/o path for normal file, with optional path for file inside archive
//		public String pathname; // full path+arcname+filename
//		public String arcname; // archive file name w/o path
//		public String language; // document language
//		public String description;	// book description
//		public String lang_from; // translate from
//		public String lang_to; // translate to
//		public String opdsLink;
//		public String pathR; // path form opds-assigned entry
//		public String pathnameR; // pathname form opds-assigned entry
//		public String arcnameR; // arcname form opds-assigned entry
//		public String opdsLinkR; // link form opds-assigned entry
//		public String username; // username for online catalogs
//		public String password; // password for online catalogs
//		public String proxy_addr;
//		public String proxy_port;
//		public String proxy_uname;
//		public String proxy_passw;
//		public int onion_def_proxy;
//		// Litres related
		cover_href = v.cover_href;
		cover_href2 = v.cover_href2;
		// Content URI related
		documentFile = v.documentFile;
//		public String fragment_href;
//		public Double finalPrice = 0D;
//		public int free = 0;
//		public String availDate;
//		public long availDateN;
//		public int available = 0;
//		public int type = 0;
//		public int lvl = 0;
//		public int arts_n = 0;
//		public LitresSearchParams lsp;
//		public String top_arts;
//		public String top_genres;
//		public String format_chosen;
//		public String minage;
//		public String subtitle;
    }

	public boolean contains(String text)
	{
		String find = StrUtils.getNonEmptyStr(text,true).toUpperCase();
		if (StrUtils.getNonEmptyStr(title, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(authors, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(authorext, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(series, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(String.valueOf(seriesNumber), true).toUpperCase().contains(find)) return true;
		//if (StrUtils.getNonEmptyStr(path, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(filename, true).toUpperCase().contains(find)) return true;
		//if (StrUtils.getNonEmptyStr(pathname, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(arcname, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(genre, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(genre_list, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(tags, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(tag_list, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(annotation, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(translator, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(docauthor, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(docprogram, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(docsrcurl, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(docsrcocr, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(docversion, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(publname, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(publisher, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(publcity, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(publyear, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(publisbn, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(publseries, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(String.valueOf(publseriesNumber), true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(publisbn, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(lang_from, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(lang_to, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(srclang, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(bookdate, true).toUpperCase().contains(find)) return true;
		if (StrUtils.getNonEmptyStr(docdate, true).toUpperCase().contains(find)) return true;
		java.util.Date dateTime=new java.util.Date(createTime);
		android.text.format.DateFormat df = new android.text.format.DateFormat();
		if (df.format("yyyy-MM-dd hh:mm:ss a", dateTime).toString().toUpperCase().contains(find)) return true;
		dateTime=new java.util.Date(fileCreateTime);
		df = new android.text.format.DateFormat();
		if (df.format("yyyy-MM-dd hh:mm:ss a", dateTime).toString().toUpperCase().contains(find)) return true;
		return false;
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
		return pathname!=null &&
				(
					(pathname.startsWith("@") && (!isLitresPrefix()))
					||
					isLitresPagination()
					||
					isLitresSpecialDir()
				);
	}

	public boolean isLitresSpecialDir()
	{
		// Item, that requires additional behaviour on click - person, genre, collection, etc
		return pathname!=null && pathname.startsWith("@")
				&&
				(
					(pathname.startsWith(LITRES_GENRE_PREFIX)) ||
					(pathname.startsWith(LITRES_PERSONS_PREFIX)) ||
					(pathname.startsWith(LITRES_PERSONS_GROUP_PREFIX)) ||
					(pathname.startsWith(LITRES_SEQUENCE_PREFIX)) ||
					(pathname.startsWith(LITRES_SEQUENCE_GROUP_PREFIX)) ||
					(pathname.startsWith(LITRES_COLLECTION_PREFIX)) ||
					(pathname.startsWith(LITRES_COLLECTION_GROUP_PREFIX)) ||
					(pathname.startsWith(LITRES_BOOKS_GROUP_PREFIX)) ||
					(pathname.startsWith(LITRES_GENRE_GROUP_PREFIX))
				);
	}

	public boolean isLoadingStub()
	{
		return pathname!=null && pathname.startsWith(LOADING_STUB_PREFIX);
	}

	public boolean isNotFoundStub()
	{
		return pathname!=null && pathname.startsWith(NOT_FOUND_STUB_PREFIX);
	}
	
	public boolean isOnlineCatalogPluginDir()
	{
		return pathname!=null && pathname.startsWith(ONLINE_CATALOG_PLUGIN_PREFIX);
	}

	public boolean isLitresDir()
	{
		return pathname!=null && pathname.startsWith(LITRES_TAG);
	}

	public boolean isLitresBooksGroupDir()
	{
		return pathname!=null && pathname.startsWith(LITRES_BOOKS_GROUP_PREFIX);
	}

	public boolean isLitresPaginationNextPage()
	{
		return pathname!=null && pathname.startsWith(LITRES_PREFIX) && pathname.contains("nextpage");
	}

	public boolean isLitresPaginationPrevPage()
	{
		return pathname!=null && pathname.startsWith(LITRES_PREFIX) && pathname.contains("prevpage");
	}

	public boolean isLitresPagination() {
		return isLitresPaginationPrevPage() || isLitresPaginationNextPage();
	}

	public boolean isLitresPrefix()
	{
		return pathname!=null && pathname.startsWith(LITRES_PREFIX);
	}

	public static String getDisplayName(CoolReader cr, String s) {
		String ss = s;
		if (ss.equals(FileInfo.AUTHORS_TAG)) ss = cr.getString(R.string.calibre_authors);
		return ss;
	}

	public boolean isCalibrePrefix()
	{
		return pathname!=null && pathname.startsWith(CALIBRE_PREFIX);
	}

	public boolean isOnlineCatalogPluginBook()
	{
		return !isDirectory && pathname != null && pathname.startsWith(ONLINE_CATALOG_PLUGIN_PREFIX) && getOnlineStoreBookInfo() != null;
	}

	public boolean isOTGDir()
	{
		return pathname!=null && pathname.startsWith(OTG_DIR_PREFIX);
	}
	
	public boolean isOPDSDir()
	{
		return pathname!=null && pathname.startsWith(OPDS_DIR_PREFIX) && (getOPDSEntryInfo() == null || getOPDSEntryInfo().getBestAcquisitionLink() == null);
	}

	public boolean isOPDSSearchDir()
	{
		return isOPDSDir() && pathname!=null && pathname.startsWith(OPDS_DIR_PREFIX+"search:");
	}
	
	public boolean isOPDSBook()
	{
		return pathname!=null && pathname.startsWith(OPDS_DIR_PREFIX) && getOPDSEntryInfo() != null && getOPDSEntryInfo().getBestAcquisitionLink() != null;
	}

	public boolean isLitresBook()
	{
		return pathname!=null && pathname.startsWith(LITRES_BOOKS_PREFIX);
	}

	public boolean isCalibreBook()
	{
		return pathname!=null && pathname.startsWith(CALIBRE_BOOKS_PREFIX);
	}

	public boolean isCloudBook() {
		return isOPDSBook() || isLitresBook();
	}

	public boolean isLitresPerson()
	{
		return pathname!=null && pathname.startsWith(LITRES_PERSONS_PREFIX);
	}

	public boolean isLitresSequence()
	{
		return pathname!=null && pathname.startsWith(LITRES_SEQUENCE_PREFIX);
	}

	public boolean isLitresCollection()
	{
		return pathname!=null && pathname.startsWith(LITRES_COLLECTION_PREFIX);
	}

	public OPDSUtil.EntryInfo getOPDSEntryInfo() {
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

	public boolean isCalibreRoot() {
		return pathname.startsWith(CALIBRE_LIST_TAG);
	}

	public boolean isCalibreByAuthors() {
		return isCalibreRoot() && filename.startsWith(AUTHORS_TAG);
	}

	public boolean isSearchShortcut()
	{
		return SEARCH_SHORTCUT_TAG.equals(pathname);
	}

//	CR implementation
//	public boolean isBooksByGenreRoot()
//	{
//		return GENRES_TAG.equals(pathname);
//	}

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

//	CR implementation
//	public boolean isBooksByGenreDir()
//	{
//		return pathname!=null && pathname.startsWith(GENRES_PREFIX);
//	}

	public boolean isBooksByTitleLevel()
	{
		return pathname.startsWith(TITLE_TAG_LEVEL);
	}

	public boolean isBooksByGenreRoot()
	{
		return GENRE_TAG.equals(pathname);
	}

	public boolean isBooksByTagRoot()
	{
		return TAG_TAG.equals(pathname);
	}

	public boolean isBooksByLitresGenreRoot()
	{
		return LITRES_GENRE_TAG.equals(pathname);
	}

	public boolean isBooksByLitresCollectionRoot()
	{
		return LITRES_COLLECTION_TAG.equals(pathname);
	}

	public boolean isBooksByLitresSequenceRoot()
	{
		return LITRES_SEQUENCE_TAG.equals(pathname);
	}

	public boolean isBooksByLitresPersonRoot()
	{
		return LITRES_PERSON_TAG.equals(pathname);
	}

	public boolean isBooksByLitresBooksRoot()
	{
		return LITRES_BOOKS_TAG.equals(pathname);
	}

	public boolean isRescanShortcut()
	{
		return RESCAN_LIBRARY_TAG.equals(pathname);
	}

	public boolean isCalcLibraryStatsShortcut()
	{
		return CALC_LIBRARY_STATS_TAG.equals(pathname);
	}

	public boolean isBooksByAuthorDir()
	{
		return pathname!=null && pathname.startsWith(AUTHOR_PREFIX);
	}

	public boolean isBooksByCalibreAuthorDir()
	{
		return pathname!=null && pathname.startsWith(CALIBRE_AUTHOR_PREFIX);
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

	public boolean isBooksByGenreDir()
	{
		return pathname!=null && pathname.startsWith(GENRE_PREFIX);
	}

	public boolean isBooksByTagDir()
	{
		return pathname!=null && pathname.startsWith(TAG_PREFIX);
	}

	public boolean isBooksByLitresGenreDir()
	{
		return pathname!=null && pathname.startsWith(LITRES_GENRE_PREFIX);
	}

	public boolean isBooksByLitresGenreGroupDir()
	{
		return pathname!=null && pathname.startsWith(LITRES_GENRE_GROUP_PREFIX);
	}

	public boolean isBooksByLitresCollectionDir()
	{
		return pathname!=null && pathname.startsWith(LITRES_COLLECTION_PREFIX);
	}

	public boolean isBooksByLitresSequenceDir()
	{
		return pathname!=null && pathname.startsWith(LITRES_SEQUENCE_PREFIX);
	}

	public boolean isBooksByLitresPersonDir()
	{
		return pathname!=null && pathname.startsWith(LITRES_PERSONS_PREFIX);
	}

	public boolean isBooksByLitresBooksDir()
	{
		return pathname!=null && pathname.startsWith(LITRES_BOOKS_PREFIX);
	}
	
	public boolean isOnSDCard() {
		if (null == parent)
			return false;
		if ( ( ( "SD".equals(filename) && "SD".equals(title)) ||
				("EXT SD".equals(filename) && "EXT SD".equals(title)) ) &&
				isDirectory && !isArchive && 0 == size && 0 == arcsize &&
				ROOT_DIR_TAG.equals(parent.pathname) )
			return true;
		return parent.isOnSDCard();
	}

	// CR implementation
//	public String getGenreCode() {
//		if (pathname.startsWith(GENRES_PREFIX)) {
//			return pathname.substring(GENRES_PREFIX.length());
//		}
//		return "";
//	}

	public long getAuthorId()
	{
		if (!isBooksByAuthorDir() && (!isBooksByCalibreAuthorDir()))
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

	public long getGenreId()
	{
		if (!isBooksByGenreDir())
			return 0;
		return id;
	}

	public long getTagId()
	{
		if (!isBooksByTagDir())
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
		if (!StrUtils.isEmptyStr(arcname))
			return arcname + ARC_SEPARATOR + pathname;
		return pathname;
	}

	public String getBasePath()
	{
		if (!StrUtils.isEmptyStr(arcname))
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

	public int fileCountTotal()
	{
		int fc = files!=null ? files.size() : 0;
		if (dirs != null) {
			for (FileInfo dir: dirs) {
				fc += dir.fileCountTotal();
			}
		}
		return fc;
	}

	public int itemCount()
	{
		return dirCount() + fileCount();
	}

	public void addDir(FileInfo dir)
	{
		if ( dirs==null )
			dirs = new ArrayList<>();
		dirs.add(dir);
		if (dir.parent == null)
			dir.parent = this;
	}

	public void addFile(FileInfo file)
	{
		if (files == null)
			files = new ArrayList<>();
		files.add(file);
	}

	public void addItems(Collection<FileInfo> items)
	{
		for ( FileInfo item : items ) {
			if ( item.isDirectory )
				addDir(item);
			else
				addFile(item);
			item.parent = this;
		}
	}

	public void replaceItems(Collection<FileInfo> items)
	{
		files = null;
		dirs = null;
		addItems( items );
	}

	public boolean updateItem(FileInfo item) {
		if (null != dirs) {
			for (FileInfo dir : dirs) {
				if (dir.pathNameEquals(item)) {
					dir.assign(item);
					dir.setItems(item);
					return true;
				}
			}
		}
		if (null != files) {
			for (FileInfo file : files) {
				if (file.pathNameEquals(item)) {
					file.assign(item);
					return true;
				}
			}
		}
		return false;
	}

	public boolean isEmpty()
	{
		return fileCount()==0 && dirCount()==0;
	}

	public FileInfo getItem(int index)
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
				if ( isOnSDCard() && pathName.compareToIgnoreCase(dir.getPathName()) == 0 || pathName.equals(dir.getPathName()) )
					return dir;
		if ( files!=null )
			for ( FileInfo file : files ) {
				if ( isOnSDCard() && pathName.compareToIgnoreCase(file.getPathName()) == 0 || pathName.equals(file.getPathName()) )
					return file;
				if ( isOnSDCard() && file.getPathName().toLowerCase().startsWith(pathName.toLowerCase()+"@/") || file.getPathName().startsWith(pathName+"@/" ))
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
//		Log.i("cr3", "pathNameEquals1 " + item.arcname);
//		Log.i("cr3", "pathNameEquals2 " + arcname);
//		Log.i("cr3", "pathNameEquals3 " + item.pathname);
//		Log.i("cr3", "pathNameEquals4 " + pathname);
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
		modified = setAuthorsLFM(file.getAuthorsLFM()) || modified;
		modified = setGenres(file.getGenres()) || modified;
		modified = setTags(file.getTags()) || modified;
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
		if (this == copyFrom)
			return;
		clear();
		for (int i=0; i<copyFrom.fileCount(); i++) {
			FileInfo file = copyFrom.getFile(i);
			file.parent = this;
			addFile(file);
		}
		for (int i=0; i<copyFrom.dirCount(); i++) {
			FileInfo dir = copyFrom.getDir(i);
			dir.parent = this;
			addDir(dir);
		}
		isListed = copyFrom.isListed;
		isScanned = copyFrom.isScanned;
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
		isListed = true;
	}

	public boolean removeEmptyDirs()
	{
		if (parent == null || pathname.startsWith("@") || !isListed || dirs == null)
			return false;
		boolean removed = false;
		for ( int i=dirCount()-1; i>=0; i-- ) {
			FileInfo dir = getDir(i);
			if ( dir.isListed && dir.dirCount() == 0 && dir.fileCount() == 0) {
				dirs.remove(i);
				removed = true;
			}
		}
		return removed;
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

	public String getAuthorsLFM() {
		if (authorsLFM != null) {
			String[] list = authorsLFM.split("\\|");
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

	public String getAuthorExt() {
		if (authorext!=null) {
			String[] list = authorext.split("\\|");
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
		if (authors != null) {
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

	public boolean setAuthorsLFM(String authorsLFM) {
		if (eq(this.authorsLFM, authorsLFM))
			return false;
		if (authorsLFM != null) {
			String[] list = authorsLFM.split("\\|");
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
			this.authorsLFM = resS;
		} else this.authorsLFM = null;
		return true;
	}

	public boolean setAuthorext(String authorext) {
		if (eq(this.authorext, authorext))
			return false;
		this.authorext = authorext;
		return true;
	}

	public String getGenres() {
		String genreR = this.genre_list;
		if (StrUtils.isEmptyStr(genreR)) genreR = this.genre;
		if (genreR != null) {
			String[] list = genreR.split("\\|");
			ArrayList<String> arrS = new ArrayList<>();
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

	public boolean setGenres(String genres) {
		String genreR = this.genre_list;
		if (StrUtils.isEmptyStr(genreR)) genreR = this.genre;
		if ((eq(genreR, genres)) && (!StrUtils.isEmptyStr(this.genre_list)))
			return false;
		if (genres!=null) {
			String[] list = genres.split("\\|");
			ArrayList<String> arrS = new ArrayList<>();
			for (String s : list) {
				s = s.replaceAll("\\s+", " ").trim();
				if (!arrS.contains(s)) arrS.add(s);
			}
			String resS = "";
			for (String s : arrS) {
				resS = resS + "|" + s;
			}
			if (resS.length() > 0) resS = resS.substring(1);
			this.genre = resS;
			this.genre_list = resS;
		} else {
			this.genre = null;
			this.genre_list = null;
		}
		return true;
	}

	public String getTags() {
		String tagR = this.tag_list;
		if (StrUtils.isEmptyStr(tagR)) tagR = this.tags;
		if (tagR != null) {
			String[] list = tagR.split("\\|");
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

	public boolean setTags(String tags) {
		String tagR = this.tag_list;
		if (StrUtils.isEmptyStr(tagR)) tagR = this.tags;
		if ((eq(tagR, tags)) && (!StrUtils.isEmptyStr(this.tag_list)))
			return false;
		if (tags != null) {
			String[] list = tags.split("\\|");
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
			this.tags = resS;
			this.tag_list = resS;
		} else {
			this.tags = null;
			this.tag_list = null;
		}
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

	public String getFilename() {
		return filename;
	}

	public boolean setFilename(String filename) {
		if (eq(this.filename, filename))
			return false;
		this.filename = filename;
		this.name_crc32 = getFNameCRC(filename);
		//if (StrUtils.isEmptyStr(this.title)) this.title = filename; // dont work properly, think later
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
		String genreR = this.genre_list;
		if (StrUtils.isEmptyStr(genreR)) genreR = this.genre;
		if (eq(genreR, genre))
			return false;
		this.genre = genre;
		this.genre_list = genre;
		return true;
	}

	public boolean setTag(String tag) {
		String tagR = this.tag_list;
		if (StrUtils.isEmptyStr(tagR)) tagR = this.tags;
		if (eq(tagR, genre))
			return false;
		this.tags = tag;
		this.tag_list = tag;
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

	public boolean setAvaildate(String availDate) {
		if (eq(this.availDate, availDate))
			return false;
		this.availDate = availDate;
		this.availDateN = StrUtils.parseDateLong(availDate);
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

	public boolean setOpdsLink(String opdsLink) {
		if (eq(this.opdsLink, opdsLink))
			return false;
		this.opdsLink = opdsLink;
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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (id == null) {
			dest.writeByte((byte) 0);
		} else {
			dest.writeByte((byte) 1);
			dest.writeLong(id);
		}
		dest.writeString(title);
		dest.writeString(authors);
		dest.writeString(series);
		dest.writeInt(seriesNumber);
		dest.writeString(genres);
		dest.writeString(path);
		dest.writeString(filename);
		dest.writeString(pathname);
		dest.writeString(arcname);
		dest.writeString(language);
		dest.writeString(description);
		dest.writeString(username);
		dest.writeString(password);
		dest.writeLong(size);
		dest.writeLong(arcsize);
		dest.writeLong(createTime);
		dest.writeLong(lastAccessTime);
		dest.writeInt(flags);
		dest.writeByte((byte) (isArchive ? 1 : 0));
		dest.writeByte((byte) (isDirectory ? 1 : 0));
		dest.writeByte((byte) (isListed ? 1 : 0));
		dest.writeByte((byte) (isScanned ? 1 : 0));
		dest.writeLong(crc32);
		dest.writeInt(domVersion);
		dest.writeInt(blockRenderingFlags);
		dest.writeParcelable(parent, flags);
		dest.writeTypedList(files);
		dest.writeTypedList(dirs);
	}
	
	public static enum SortOrder {
		FILENAME(R.string.mi_book_sort_order_filename, (f1, f2) -> {
			if ( f1==null || f2==null )
				return 0;
			return Utils.cmp(f1.getFileNameToDisplay(), f2.getFileNameToDisplay());
		}),
		FILENAME_DESC(R.string.mi_book_sort_order_filename_desc, (f1, f2) -> {
			if ( f1==null || f2==null )
				return 0;
			return Utils.cmp(f2.getFileNameToDisplay(), f1.getFileNameToDisplay());
		}),
		TIMESTAMP(R.string.mi_book_sort_order_timestamp, (f1, f2) -> {
			if ( f1==null || f2==null )
				return 0;
			return firstNz( cmp(f1.createTime, f2.createTime), Utils.cmp(f1.filename, f2.filename) );
		}),
		TIMESTAMP_DESC(R.string.mi_book_sort_order_timestamp_desc, (f1, f2) -> {
			if ( f1==null || f2==null )
				return 0;
			return firstNz( cmp(f2.createTime, f1.createTime), Utils.cmp(f2.filename, f1.filename) );
		}),
		AUTHOR_TITLE(R.string.mi_book_sort_order_author, (f1, f2) -> {
			if ( f1==null || f2==null )
				return 0;
			return firstNz(
					cmpNotNullFirst(Utils.formatAuthors(f1.authors), Utils.formatAuthors(f2.authors))
					,cmpNotNullFirst(f1.series, f2.series)
					,cmp(f1.getSeriesNumber(), f2.getSeriesNumber())
					,cmpNotNullFirst(f1.title, f2.title)
					,Utils.cmp(f1.filename, f2.filename)
					);
		}),
		AUTHOR_TITLE_DESC(R.string.mi_book_sort_order_author_desc, (f1, f2) -> {
			if ( f1==null || f2==null )
				return 0;
			return firstNz(
					cmpNotNullFirst(Utils.formatAuthors(f2.authors), Utils.formatAuthors(f1.authors))
					,cmpNotNullFirst(f2.series, f1.series)
					,cmp(f2.getSeriesNumber(), f1.getSeriesNumber())
					,cmpNotNullFirst(f2.title, f1.title)
					,Utils.cmp(f2.filename, f1.filename)
			);
		}),
		TITLE_AUTHOR(R.string.mi_book_sort_order_title, (f1, f2) -> {
			if ( f1==null || f2==null )
				return 0;
			return firstNz(
					cmpNotNullFirst(f1.series, f2.series)
					,cmp(f1.getSeriesNumber(), f2.getSeriesNumber())
					,cmpNotNullFirst(f1.title, f2.title)
					,cmpNotNullFirst(Utils.formatAuthors(f1.authors), Utils.formatAuthors(f2.authors))
					,Utils.cmp(f1.filename, f2.filename)
					);
		}),
		TITLE_AUTHOR_DESC(R.string.mi_book_sort_order_title_desc, (f1, f2) -> {
			if ( f1==null || f2==null )
				return 0;
			return firstNz(
					cmpNotNullFirst(f2.series, f1.series)
					,cmp(f2.getSeriesNumber(), f1.getSeriesNumber())
					,cmpNotNullFirst(f2.title, f1.title)
					,cmpNotNullFirst(Utils.formatAuthors(f2.authors), Utils.formatAuthors(f1.authors))
					,Utils.cmp(f2.filename, f1.filename)
			);
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
		
		public static int cmp( long n1, long n2 )
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
			if (dirs != null)
				if (dirs.size()>0)
					if (dirs.get(0).isLitresPrefix()) {
						FileInfo nextPageDir = null;
						FileInfo prevPageDir = null;
						for (FileInfo fi : dirs) {
							if (fi.isLitresPaginationNextPage()) nextPageDir = fi;
							if (fi.isLitresPaginationPrevPage()) prevPageDir = fi;
						}
						if (nextPageDir != null) {
							dirs.remove(nextPageDir);
							dirs.add(0, nextPageDir);
						}
						if (prevPageDir != null) {
							dirs.remove(prevPageDir);
							dirs.add(0, prevPageDir);
						}
					}
		}
		if ( files!=null ) {
			ArrayList<FileInfo> newFiles = new ArrayList<>(files);
			Collections.sort( newFiles, SortOrder.getComparator() );
			files = newFiles;
		}
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arcname == null) ? 0 : arcname.hashCode());
		result = prime * result + (int)arcsize;
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
		result = prime * result + (int)size;
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
		if (!StrUtils.euqalsIgnoreNulls(description, other.description, true)) return false;
		if (name_crc32 != other.name_crc32) return false;
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
		if (!StrUtils.euqalsIgnoreNulls(genre_list, other.genre_list, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(tags, other.tags, true)) return false;
		if (!StrUtils.euqalsIgnoreNulls(tag_list, other.tag_list, true)) return false;
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
		if (!StrUtils.euqalsIgnoreNulls(opdsLink, other.opdsLink, true)) return false;
		if (wordCount != other.wordCount) return false;
		if (symCount != other.symCount) return false;
		if (publseriesNumber != other.publseriesNumber) return false;
		if (fileCreateTime != other.fileCreateTime) return false;
		if (seriesNumber != other.seriesNumber) return false;
		if (bookDateN != other.bookDateN) return false;
		if (docDateN != other.docDateN) return false;
		if (publYearN != other.publYearN) return false;
		if (!StrUtils.euqalsIgnoreNulls(opdsLink, other.opdsLink, true)) return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (domVersion != other.domVersion)
			return false;
		if (blockRenderingFlags != other.blockRenderingFlags)
			return false;
		// Base check: fingerprint (for now only crc32)
		return crc32 == other.crc32;
	}

	public boolean baseEquals(FileInfo other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (arcsize != other.arcsize)
			return false;
		if (authors == null) {
			if (other.authors.length() > 0)
				return false;
		} else if ( ! ( authors.equals(other.authors) || (authors.length() == 0 && other.authors == null) ) )
			return false;
		// TODO: potentially filename may not match. Perhaps we need to remove this check.
		if (filename == null) {
			if (other.filename != null)
				return false;
		} else if (!filename.equals(other.filename))
			return false;
		if (format != other.format)
			return false;
		// TODO: potentially isArchive may not match. Perhaps we need to remove this check.
		if (isArchive != other.isArchive)
			return false;
		if (isDirectory != other.isDirectory)
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		// do not compare genres of books, because in the absence of certain genres in the handbook,
		// the 'genres' field obtained from the database will not be equal to the field obtained when parsing the book file.
		/*
		if (!eqGenre(genres, other.genres))
			return false;
		*/
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name_crc32 != other.name_crc32) return false;
		if (series == null) {
			if (other.series != null && other.series.length() != 0)
				return false;
		} else if (!series.equals(other.series) && !(series.length() == 0 && other.series == null))
			return false;
		if (seriesNumber != other.seriesNumber)
			return false;
		if (size != other.size)
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		// Base check: fingerprint (for now only crc32)
		return crc32 == other.crc32;
	}

	// CR implementation
	private static boolean eqGenre(String g1, String g2) {
		if (g1 == null) {
				if (g2 != null && g2.length() != 0)
					return false;
		}
		if (g1.equals(g2))
			return true;
		String[] g1_array = g1.split("\\|");
		String[] g2_array = g2.split("\\|");
		if (g1_array.length == g2_array.length) {
			Arrays.sort(g1_array);
			Arrays.sort(g2_array);
			return Arrays.equals(g1_array, g2_array);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return pathname;
	}
	
	public boolean allowSorting() {
		boolean res = isDirectory && !isRecentDir();
		if ((res) && isSpecialDir()) {
			for (String s: sortTags) {
				if ((pathname.startsWith(s)) || (pathname.equals(s.replace(":", ""))))
					return true;
			}
			return false;
		}
		return res;
//		&& !isRootDir() && !isRecentDir() && !isOPDSDir() && !isBooksBySeriesDir()
//				&& !isBooksByBookdateDir()&& !isBooksByDocdateDir()&& !isBooksByPublyearDir()
//				&& !isBooksByFiledateDir();
	}

	public ReadingStatRes calcStats() {
		int i = 0;
		ReadingStatRes sres = new ReadingStatRes();
		sres.cnt = 0;
		sres.val = 0.0;
		ArrayList<Double> arrValues = new ArrayList<Double>();
		while (i < stats.size() - 5) {
			ReadingStat rs1 = stats.get(i);
			ReadingStat rs2 = stats.get(i+1);
			ReadingStat rs3 = stats.get(i+2);
			ReadingStat rs4 = stats.get(i+3);
			ReadingStat rs5 = stats.get(i+4);
			if (
				(rs1.speedKoef>0) && (rs2.speedKoef>0) && (rs3.speedKoef>0)
					&& (rs4.speedKoef>0) && (rs5.speedKoef>0)
					&& (rs2.pageNumber == rs1.pageNumber + 1)
					&& (rs3.pageNumber == rs2.pageNumber + 1)
					&& (rs4.pageNumber == rs3.pageNumber + 1)
					&& (rs5.pageNumber == rs4.pageNumber + 1)
			) {
				// Нашли последовательное чтение пяти страниц, посчитаем
				double avgValue = (rs1.speedKoef + rs2.speedKoef + rs3.speedKoef + rs4.speedKoef + rs5.speedKoef) / 5;
				double sigma = Math.sqrt((
						(rs1.speedKoef - avgValue) * (rs1.speedKoef - avgValue) +
						(rs2.speedKoef - avgValue) * (rs2.speedKoef - avgValue) +
						(rs3.speedKoef - avgValue) * (rs3.speedKoef - avgValue) +
						(rs4.speedKoef - avgValue) * (rs4.speedKoef - avgValue) +
						(rs5.speedKoef - avgValue) * (rs5.speedKoef - avgValue))/5);
				i = i + 4;
				int cnt = 0;
				double avgValue2 = 0;
				if ((rs1.speedKoef > avgValue - (2.0 * sigma)) &&
				   		(rs1.speedKoef < avgValue + (2.0 * sigma))) {
					cnt ++;
					avgValue2 = avgValue2 + rs1.speedKoef;
				}
				if ((rs2.speedKoef > avgValue - (2.0 * sigma)) &&
						(rs2.speedKoef < avgValue + (2.0 * sigma))) {
					cnt ++;
					avgValue2 = avgValue2 + rs2.speedKoef;
				}
				if ((rs3.speedKoef > avgValue - (2.0 * sigma)) &&
						(rs3.speedKoef < avgValue + (2.0 * sigma))) {
					cnt ++;
					avgValue2 = avgValue2 + rs3.speedKoef;
				}
				if ((rs4.speedKoef > avgValue - (2.0 * sigma)) &&
						(rs4.speedKoef < avgValue + (2.0 * sigma))) {
					cnt ++;
					avgValue2 = avgValue2 + rs4.speedKoef;
				}
				if ((rs5.speedKoef > avgValue - (2.0 * sigma)) &&
						(rs5.speedKoef < avgValue + (2.0 * sigma))) {
					cnt ++;
					avgValue2 = avgValue2 + rs5.speedKoef;
				}
				if (cnt == 5) { // Only good measurements
					avgValue2 = avgValue2 / ((double) cnt);
					arrValues.add(avgValue2);
				}
			}
			i = i + 1;
		}
		if (arrValues.size()>0) {
			int cnt = 0;
			double avgValue = 0;
			for (Double d: arrValues) {
				cnt++;
				avgValue+=d;
			}
			avgValue = avgValue / ((double) cnt);
			Double sigma = 0.0;
			for (Double d: arrValues) {
				sigma += (d - avgValue) * (d - avgValue);
			}
			sigma = Math.sqrt(sigma/ ((double) cnt));
			cnt = 0;
			double avgValueC = 0;
			for (Double d: arrValues) {
				if (((d > avgValue - (2.0 * sigma)) &&
					(d < avgValue + (2.0 * sigma)))
						|| (arrValues.size()<4))  {
				  cnt ++;
				  avgValueC += d;
				}
			}
			sres.cnt = cnt;
			sres.val = avgValueC / ((double) cnt);
			return sres;
		}
		return sres;
	}

}
