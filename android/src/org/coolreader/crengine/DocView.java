package org.coolreader.crengine;


import android.graphics.Bitmap;

import org.coolreader.CoolReader;
import org.coolreader.readerview.ReaderView;

import java.util.List;

public class DocView {

	public static final Logger log = L.create("dv");

	public static final int SWAP_DONE = 0;
	public static final int SWAP_TIMEOUT = 1;
	public static final int SWAP_ERROR = 2;

	public static boolean wasIdleUpdate = false;

	private final Object mutex;

	private Bookmark curPageBookmark;
	private long curPageBmkTime;

	private CoolReader mCoolReader;

	public DocView(Object mutex, CoolReader mCoolReader) {
		log.i("DocView()");
		this.mutex = mutex;
		this.mCoolReader = mCoolReader;
	}
	
	/**
	 * Create native object.
	 */
	public void create() {
		synchronized(mutex) {
			createInternal();
		}
	}

	/**
	 * Destroy native object.
	 */
	public void destroy() {
		synchronized(mutex) {
			destroyInternal();
		}
	}

	/**
	 * Set document callback.
	 * @param readerCallback is callback to set
	 */
	public void setReaderCallback(ReaderCallback readerCallback) {
		this.readerCallback = readerCallback;
	}

	/**
	 * If document uses cache file, swap all unsaved data to it.
	 * @return either SWAP_DONE, SWAP_TIMEOUT, SWAP_ERROR
	 */
	public int swapToCache() {
		
		synchronized(mutex) {
			return swapToCacheInternal();
		}
	}

	/**
	 * Follow link.
	 * @param link
	 * @return
	 */
	public int goLink(String link) {
		synchronized(mutex) {
			return goLinkInternal(link);
		}
	}
	
	/**
	 * Find a link near to specified window coordinates.
	 * @param x
	 * @param y
	 * @param delta
	 * @return
	 */
	public String checkLink(int x, int y, int delta) {
		synchronized(mutex) {
			return checkLinkInternal(x, y, delta);
		}
	}

	/**
	 * Get page text
	 * @param wrapWords
	 * @param pageIndex
	 * @return
	 */
	public String getPageText(boolean wrapWords, int pageIndex) {
		synchronized(mutex) {
			return getPageTextInternal(wrapWords, pageIndex);
		}
	}

	/**
	 * Get page count
	 * @return
	 */
	public int getPageCount() {
		synchronized(mutex) {
			return getPageCountInternal();
		}
	}

	/**
	 * Get visible page count
	 * @return
	 */
	public int getVisiblePageCount() {
		synchronized(mutex) {
			return getVisiblePageCountInternal();
		}
	}

	/**
	 * Get page number
	 * @return
	 */

	public int getCurPage() {
		synchronized(mutex) {
			return getCurPageInternal();
		}
	}
	/**
	 * Set selection range.
	 * @param sel
	 */
	public void updateSelection(Selection sel) {
		synchronized(mutex) {
			updateSelectionInternal(sel);
		}
	}

	/**
	 * Move selection.
	 * @param sel
	 * @param moveCmd
	 * @param params
	 * @return
	 */
	public boolean moveSelection(Selection sel,
			int moveCmd, int params) {
		synchronized(mutex) {
			return moveSelectionInternal(sel, moveCmd, params);
		}
	}

	/**
	 * Send battery state to native object.
	 * @param state
	 */
	public void setBatteryState(int state, int chargingConn, int chargeLevel) {
		synchronized(mutex) {
			setBatteryStateInternal(state, chargingConn, chargeLevel);
		}
	}

	/**
	 * Get current book coverpage data bytes.
	 * @return
	 */
	public byte[] getCoverPageData() {
		synchronized(mutex) {
			return getCoverPageDataInternal();
		}
	}

	/**
	 * Set texture for page background.
	 * @param imageBytes
	 * @param tileFlags
	 */
	public void setPageBackgroundTexture(
			byte[] imageBytes, int tileFlags) {
		synchronized(mutex) {
			setPageBackgroundTextureInternal(imageBytes, tileFlags);
		}
	}

	/**
	 * create empty document with specified message (e.g. to show errors)
	 * @param title
	 * @param message
	 */
	public void createDefaultDocument(String title, String message)
	{
		synchronized(mutex) {
			createDefaultDocumentInternal(title, message);
		}
	}

	/**
	 * Load document from file.
	 * @param fileName
	 * @return
	 */
	public boolean loadDocument(String fileName) {
		synchronized(mutex) {
			return loadDocumentInternal(fileName);
		}
	}

	/**
	 * Load document from memory buffer.
	 * @param buffer document content as byte buffer
	 * @param contentPath Non empty stream name
	 * @return true if operation successful, false otherwise.
	 */
	public boolean loadDocumentFromBuffer(byte[] buffer, String contentPath) {
		synchronized (mutex) {
			return loadDocumentFromMemoryInternal(buffer, contentPath);
		}
	}

	/**
	 * Set time left
	 * @param time_left
	 * @return
	 */
	public boolean setTimeLeft(String time_left) {
		synchronized(mutex) {
			return setTimeLeftInternal(time_left);
		}
	}

	/**
	 * Set time left to chapter
	 * @param time_left_to_chapter
	 * @return
	 */
	public boolean setTimeLeftToChapter(String time_left_to_chapter) {
		synchronized(mutex) {
			return setTimeLeftToChapterInternal(time_left_to_chapter);
		}
	}

	/**
	 * Get settings from native object.
	 * @return
	 */
	public java.util.Properties getSettings() {
		synchronized(mutex) {
			return getSettingsInternal();
		}
	}

	/**
	 * Apply settings.
	 * @param settings
	 * @return
	 */
	public boolean applySettings(java.util.Properties settings) {
		synchronized(mutex) {
			return applySettingsInternal(settings);
		}
	}

	public java.util.Properties getDocProps() {
		synchronized (mutex) {
			return getDocPropsInternal();
		}
	}

	/**
	 * Set stylesheet for document.
	 * @param stylesheet
	 */
	public void setStylesheet(String stylesheet) {
		synchronized(mutex) {
			setStylesheetInternal(stylesheet);
		}
	}
	
	public void requestRender() {
		doCommand(ReaderCommand.DCMD_REQUEST_RENDER.getNativeId(), 0);
	}

	/**
	 * Change window size.
	 * @param dx
	 * @param dy
	 */
	public void resize(ReaderView readerView, int dx, int dy) {
		wasIdleUpdate = ((dx == 100) && (dy == 100));
		synchronized(mutex) {
			log.d("DocView.resize(" + dx + ", "+ dy + ")");
			resizeInternal(dx, dy);
		}
	}

	/**
	 * Execute command by native object.
	 * @param command
	 * @param param
	 * @return
	 */
	public boolean doCommand(int command, int param) {
		synchronized(mutex) {
			return doCommandInternal(command, param);
		}
	}

	/**
	 * Get current page bookmark info.
	 * @return
	 */
	public Bookmark getCurrentPageBookmark() {
		// make not to call this too many times
		long curTime = System.currentTimeMillis();
		if (((curTime-curPageBmkTime)>2000)||(curPageBmkTime==0)||(curPageBookmark==null)) {
			synchronized (mutex) {
				curPageBookmark = getCurrentPageBookmarkInternal();
				curPageBmkTime = System.currentTimeMillis();
				return curPageBookmark;
			}
		} else return curPageBookmark;
	}
	
	/**
	 * Get current page bookmark info, returning null if document is not yet rendered (to avoid long call).
	 * @return bookmark for current page, null if cannot be determined fast
	 */
	public Bookmark getCurrentPageBookmarkNoRender() {
		if (!isRenderedInternal())
			return null;
		synchronized(mutex) {
			return getCurrentPageBookmarkInternal();
		}
	}

	public List<SentenceInfo> getAllSentences() {
		List<SentenceInfo> sentences;
		synchronized(mutex) {
			sentences = getAllSentencesInternal();
		}
		return sentences;
	}

	/**
	 * Check whether document is formatted/rendered.
	 * @return true if document is rendered, and e.g. retrieving of page image will not cause long activity (formatting etc.)
	 */
	public boolean isRendered() {
		// thread safe
		return isRenderedInternal();
	}

	/**
	 * Move reading position to specified xPath.
	 * @param xPath
	 * @return
	 */
	public boolean goToPosition(String xPath, boolean saveToHistory) {
		synchronized(mutex) {
			return goToPositionInternal(xPath, saveToHistory);
		}
	}

	/**
	 * Get position properties by xPath.
	 * @param xPath
	 * @return
	 */
	public PositionProperties getPositionProps(String xPath, boolean precise) {
		synchronized(mutex) {
			return getPositionPropsInternal(xPath, precise);
		}
	}

	/**
	 * Fill book info fields using metadata from current book. 
	 * @param info
	 * @param updatePath
	 */
	public void updateBookInfo(BookInfo info, boolean updatePath) {
		synchronized(mutex) {
			updateBookInfoInternal(info, updatePath);
		}
	}

	/**
	 * Get TOC tree from current book.
	 * @return
	 */
	public TOCItem getTOC() {
		synchronized(mutex) {
			return getTOCInternal();
		}
	}

	/**
	 * Clear selection.
	 */
	public void clearSelection() {
		synchronized(mutex) {
			clearSelectionInternal();
		}
	}

	/**
	 * Find text in book.
	 * @param pattern
	 * @param origin
	 * @param reverse
	 * @param caseInsensitive
	 * @return
	 */
	public boolean findText(String pattern, int origin,
			int reverse, int caseInsensitive) {
		synchronized(mutex) {
			return findTextInternal(pattern, origin, reverse, caseInsensitive);
		}
	}

	/**
	 * Get current page image.
	 * @param bitmap is buffer to put data to.
	 */
	public void getPageImage(Bitmap bitmap) {
		synchronized(mutex) {
			getPageImageInternal(bitmap, DeviceInfo.isBlackAndWhiteEinkScreen(BaseActivity.getScreenTypeForce()) ? 4 : 32);
		}
	}

	/**
	 * Check whether point of current document contains image.
	 * If image is found, image becomes current image to be drawn by drawImage(), dstImage fields are set to image dimension.
	 *  
	 * @param x is X coordinate in document window
	 * @param y is Y coordinate in document window
	 * @param dstImage is to place found image dimensions to
	 * @return true if point belongs to image
	 */
	public boolean checkImage(int x, int y, ImageInfo dstImage) {
		synchronized(mutex) {
			return checkImageInternal(x, y, dstImage);
		}
	}

	/**
	 * Check whether point of current document belongs to bookmark.
	 *  
	 * @param x is X coordinate in document window
	 * @param y is Y coordinate in document window
	 * @return bookmark if point belongs to bookmark, null otherwise
	 */
	public Bookmark checkBookmark(int x, int y) {
		synchronized(mutex) {
			Bookmark dstBookmark = new Bookmark();
			if (checkBookmarkInternal(x, y, dstBookmark)) {
				return dstBookmark;
			}
			return null;
		}
	}
	
	
	/**
	 * Draws currently opened image to bitmap.
	 * @param bitmap is destination bitmap
	 * @param imageInfo contains image position and scaling parameters.
	 * @return true if current image is drawn successfully.
	 */
	public boolean drawImage(Bitmap bitmap, ImageInfo imageInfo) {
		synchronized(mutex) {
			return drawImageInternal(bitmap, DeviceInfo.isBlackAndWhiteEinkScreen(BaseActivity.getScreenTypeForce()) ? 4 : 32, imageInfo);
		}
	}

	/**
	 * Close currently opened image, free resources.
	 * @return true if there was opened current image, and it's now closed 
	 */
	public boolean closeImage() {
		synchronized(mutex) {
			return closeImageInternal();
		}
	}
	
	/**
	 * Highlight bookmarks.
	 * Remove highlight using clearSelection().
	 * @params bookmarks is array of bookmarks to highlight 
	 */
	public void hilightBookmarks(Bookmark[] bookmarks) {
		synchronized(mutex) {
			hilightBookmarksInternal(bookmarks);
		}
	}

	public boolean isTimeChanged() {
		synchronized(mutex) {
			return isTimeChangedInternal();
		}
	}
	
	//========================================================================================
	// Native functions
	/* implemented by libcr3engine.so */
	//========================================================================================
	private native void getPageImageInternal(Bitmap bitmap, int bpp);

	private native void createInternal();

	private native void destroyInternal();

	private native void createDefaultDocumentInternal(String title, String message);

	private native boolean loadDocumentInternal(String fileName);

	private native boolean loadDocumentFromMemoryInternal(byte [] buf, String contentPath);

	private native java.util.Properties getSettingsInternal();

	private native boolean applySettingsInternal(
			java.util.Properties settings);

	private native java.util.Properties getDocPropsInternal();

	private native void setStylesheetInternal(String stylesheet);

	private native void resizeInternal(int dx, int dy);

	private native boolean doCommandInternal(int command, int param);

	private native Bookmark getCurrentPageBookmarkInternal();

	private native List<SentenceInfo> getAllSentencesInternal();

	private native boolean goToPositionInternal(String xPath, boolean saveToHistory);

	private native PositionProperties getPositionPropsInternal(String xPath, boolean precise);

	private native void updateBookInfoInternal(BookInfo info, boolean updatePath);

	private native TOCItem getTOCInternal();

	private native void clearSelectionInternal();

	private native boolean findTextInternal(String pattern, int origin,
			int reverse, int caseInsensitive);

	private native void setBatteryStateInternal(int state, int chargingConn, int chargeLevel);

	private native byte[] getCoverPageDataInternal();

	private native void setPageBackgroundTextureInternal(
			byte[] imageBytes, int tileFlags);

	private native void updateSelectionInternal(Selection sel);

	private native boolean moveSelectionInternal(Selection sel,
			int moveCmd, int params);

	private native String checkLinkInternal(int x, int y, int delta);

	private native String getPageTextInternal(boolean wrapWords, int pageIndex);

	private native int getPageCountInternal();

	private native int getVisiblePageCountInternal();

	private native int getCurPageInternal();

	private native boolean checkImageInternal(int x, int y, ImageInfo dstImage);

	private native boolean checkBookmarkInternal(int x, int y, Bookmark dstBookmark);

	private native boolean drawImageInternal(Bitmap bitmap, int bpp, ImageInfo dstImage);

	private native boolean closeImageInternal();

	private native boolean isRenderedInternal();

	private native int goLinkInternal(String link);

	private native void hilightBookmarksInternal(Bookmark[] bookmarks);

	// / returns either SWAP_DONE, SWAP_TIMEOUT or SWAP_ERROR
	private native int swapToCacheInternal();

	private native boolean isTimeChangedInternal();

	private long mNativeObject; // used from JNI

	private ReaderCallback readerCallback;  // used from JNI

	private native boolean setTimeLeftInternal(String time_left);

	private native boolean setTimeLeftToChapterInternal(String time_left_to_chapter);

}
