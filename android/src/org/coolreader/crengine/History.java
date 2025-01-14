package org.coolreader.crengine;

import java.util.ArrayList;

import org.coolreader.db.BaseDB;
import org.coolreader.db.CRDBService;

import android.util.Log;

public class History extends FileInfoChangeSource {
	private ArrayList<BookInfo> mBooks = new ArrayList<>();
	private FileInfo mRecentBooksFolder;

	public History(Scanner scanner)
	{
		this.mScanner = scanner;
	}
	
	public BookInfo getLastBook()
	{
		if ( mBooks.size()==0 )
			return null;
		return mBooks.get(0);
	}

	public BookInfo getPreviousBook()
	{
		if ( mBooks.size()<2 )
			return null;
		return mBooks.get(1);
	}

	public interface BookInfoLoadedCallback {
		void onBookInfoLoaded(BookInfo bookInfo);
	}

	public interface FileInfo1LoadedCallback {
		void onFileInfoLoaded(FileInfo fileInfo);
		void onFileInfoLoadBegin();
	}
	
	public void getOrCreateBookInfo(final CRDBService.LocalBinder db, final FileInfo file, final BookInfoLoadedCallback callback)
	{
		BookInfo res = getBookInfo(file);
		if (res != null) {
			callback.onBookInfoLoaded(res);
			return;
		}
		db.loadBookInfo(file, bookInfo -> {
			if (bookInfo == null || bookInfo.getFileInfo() == null
					|| bookInfo.getFileInfo().arcsize < 0
					|| bookInfo.getFileInfo().size < 0
					|| bookInfo.getFileInfo().crc32 < 0) {
				bookInfo = new BookInfo(file);
				mBooks.add(0, bookInfo);
			}
			callback.onBookInfoLoaded(bookInfo);
		});
	}

	public void getFileInfoByOPDSLink(final CRDBService.LocalBinder db, final String opdsLink,
									  final boolean isLitres, final FileInfo1LoadedCallback callback)
	{
		db.loadFileInfoByOPDSLink(opdsLink, isLitres, new CRDBService.FileInfo1LoadingCallback() {
			@Override
			public void onFileInfoLoaded(FileInfo fileInfo) {
				callback.onFileInfoLoaded(fileInfo);
			}

			@Override
			public void onFileInfoListLoadBegin() {
				callback.onFileInfoLoadBegin();
			}
		});
	}
	
	public BookInfo getBookInfo( FileInfo file )
	{
		int index = findBookInfo( file );
		if ( index>=0 )
			return mBooks.get(index);
		return null;
	}

	public BookInfo getBookInfo( String pathname )
	{
		int index = findBookInfo( pathname );
		if ( index>=0 )
			return mBooks.get(index);
		return null;
	}
	
	public void removeBookInfo(final CRDBService.LocalBinder db, FileInfo fileInfo, boolean removeRecentAccessFromDB, boolean removeBookFromDB)
	{
		int index = findBookInfo(fileInfo);
		if (index >= 0)
			mBooks.remove(index);
		if ( removeBookFromDB )
			db.deleteBook(fileInfo);
		else if ( removeRecentAccessFromDB )
			db.deleteRecentPosition(fileInfo);
		updateRecentDir();
	}

	public void updateBookInfo(BookInfo bookInfo)
	{
		Log.v("cr3", "History.updateBookInfo() for " + bookInfo.getFileInfo().getPathName());
		bookInfo.updateAccess();
		int index = findBookInfo(bookInfo.getFileInfo());
		if ( index>=0 ) {
			BookInfo info = mBooks.get(index);
			if ( index>0 ) {
				mBooks.remove(index);
				mBooks.add(0, info);
			}
			info.setBookmarks(bookInfo.getAllBookmarks());
		} else {
			mBooks.add(0, bookInfo);
		}
	}

	public void updateBookAccess(BookInfo bookInfo, long timeElapsed)
	{
		Log.v("cr3", "History.updateBookAccess() for " + bookInfo.getFileInfo().getPathName());
		bookInfo.updateAccess();
		bookInfo.updateTimeElapsed(timeElapsed);
		int index = findBookInfo(bookInfo.getFileInfo());
		if ( index>=0 ) {
			BookInfo info = mBooks.get(index);
			if ( index>0 ) {
				mBooks.remove(index);
				mBooks.add(0, info);
			}
			info.setBookmarks(bookInfo.getAllBookmarks());
		} else {
			mBooks.add(0, bookInfo);
		}
		updateRecentDir();
	}

	public int findBookInfo( String pathname )
	{
		for ( int i=0; i<mBooks.size(); i++ )
			if ( pathname.equals(mBooks.get(i).getFileInfo().getPathName()) )
				return i;
		return -1;
	}
	
	public int findBookInfo( FileInfo file )
	{
		for ( int i=0; i<mBooks.size(); i++ )
			if (file.pathNameEquals(mBooks.get(i).getFileInfo()))
				return i;
		return -1;
	}
	
	public Bookmark getLastPos( FileInfo file )
	{
		int index = findBookInfo(file);
		if ( index<0 )
			return null;
		return mBooks.get(index).getLastPosition();
	}
	public void updateRecentDir()
	{
		Log.v("cr3", "History.updateRecentDir()");
		if ( mRecentBooksFolder!=null ) { 
			mRecentBooksFolder.clear();
			for ( BookInfo book : mBooks )
				mRecentBooksFolder.addFile(book.getFileInfo());
			onChange(mRecentBooksFolder, false);
		} else {
			Log.v("cr3", "History.updateRecentDir() : mRecentBooksFolder is null");
		}
	}
	Scanner mScanner;

	
	public void getOrLoadRecentBooks(final CRDBService.LocalBinder db, final CRDBService.RecentBooksLoadingCallback callback) {
		if (mBooks != null && mBooks.size() > 0) {
			callback.onRecentBooksListLoaded(mBooks);
		} else {
			// not yet loaded. Wait until ready: sync with DB thread.
			db.sync(() -> callback.onRecentBooksListLoaded(mBooks));
		}
	}

	public boolean loadFromDB(final CRDBService.LocalBinder db, int maxItems )
	{
		Log.v("cr3", "History.loadFromDB()");
		mRecentBooksFolder = mScanner.getRecentDir();
		db.loadRecentBooks(100, new CRDBService.RecentBooksLoadingCallback() {
			@Override
			public void onRecentBooksListLoaded(ArrayList<BookInfo> bookList) {
				if (bookList != null) {
					mBooks = bookList;
					updateRecentDir();
				}
			}

			@Override
			public void onRecentBooksListLoadBegin() {
			}
		});
		if ( mRecentBooksFolder==null )
			Log.v("cr3", "History.loadFromDB() : mRecentBooksFolder is null");
		return true;
	}

	public BaseDB getMainDB(final CRDBService.LocalBinder db)
	{
		Log.v("cr3", "History.getMainDB()");
		return db.getService().getMainDB();
	}

    public BaseDB getCoverDB(final CRDBService.LocalBinder db)
    {
        Log.v("cr3", "History.getMainDB()");
        return db.getService().getCoverDB();
    }

}
