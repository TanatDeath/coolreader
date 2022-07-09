package org.coolreader.onyx;

import com.onyx.android.sdk.readerutils.data.BookBean;
import com.onyx.android.sdk.readerutils.data.OnyxLibraryManager;

import org.coolreader.CoolReader;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.FileInfo;

public class OnyxLibrary {

	private static String lastSavedPath = "";
	private static float lastSavedPercent = 0.0f;

	private static BookBean setBookBeanFromFileInfo(BookBean bb, Bookmark bmk, BookInfo bi) {
		FileInfo fi = bi.getFileInfo();
		BookBean book = bb;
		if (book == null)
			book = BookBean.createFromFile(fi.getBasePath());
		else {
			if (book.getNativeAbsolutePath() == null)
				book = BookBean.createFromFile(fi.getBasePath());
		}
		book.setTitle(fi.title);
		book.setName(fi.getFilename());
		book.setPublisher(fi.publisher);
		book.setLanguage(fi.language);
		book.setISBN(fi.publisbn);
		book.setDescription(fi.annotation);
		book.setType(DocumentFormat.nameEndWithExt(fi.getBasePath()));
		float perc = ((float) bmk.getPercent()) / 10000.0f;
		book.setProgress(perc);
		book.setAuthors(fi.getAuthors().split("\\|"));
		//book.setCoverUrl("");
		book.setReadingStatus(BookBean.ReadingStatus.NEW);
		if (fi.getReadingState() == FileInfo.STATE_READING) book.setReadingStatus(BookBean.ReadingStatus.READING);
		if (fi.getReadingState() == FileInfo.STATE_FINISHED) book.setReadingStatus(BookBean.ReadingStatus.FINISHED);
		book.setSize(fi.size);
		//book.setCustomExtra("customExtra");
		//Update when open book
		book.updateLastAccess();
		return book;
	}

	public static boolean saveBookInfo(CoolReader activity, Bookmark bmk, BookInfo bi) {
		boolean res = false;
		float perc = ((float) bmk.getPercent()) / 10000.0f;
		if (
			(Math.abs(lastSavedPercent - perc) > 0.01f) ||
			(!lastSavedPath.equals(bi.getFileInfo().getBasePath()))
		) {
			BookBean bb = OnyxLibraryManager.getInstance().findBookByPath(activity, bi.getFileInfo().getBasePath());
			bb = setBookBeanFromFileInfo(bb, bmk, bi);
			if (bb.hasValidId())
				res = OnyxLibraryManager.getInstance().updateBook(activity, bb);
			else
				res = OnyxLibraryManager.getInstance().insertBook(activity, bb);
			if (res) {
				lastSavedPath = bi.getFileInfo().getBasePath();
				lastSavedPercent = bb.getProgress();
			}
			return res;
		} else return true;
	}

}
