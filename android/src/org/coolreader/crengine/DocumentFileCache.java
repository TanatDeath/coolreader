package org.coolreader.crengine;

import android.app.Activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class DocumentFileCache {
	public static final Logger log = L.create("dfc");

	String mBasePath = null;

	public DocumentFileCache(Activity activity) {
		File fd = activity.getCacheDir();
		File dir = new File(fd, "bookCache");
		if (dir.isDirectory() || dir.mkdirs()) {
			mBasePath = dir.getAbsolutePath();
		} else {
			log.e("Failed to obtain private app cache directory!");
		}
	}

	public final String getBasePath() {
		return mBasePath;
	}

	public BookInfo saveStream(FileInfo fileInfo, InputStream inputStream) {
		if (null == mBasePath) {
			log.e("Attempt to save stream while private app cache directory uninitialized!");
			return null;
		}
		BookInfo bookInfo = null;
		String extension;
		long codebase;
		if (0 != fileInfo.crc32)
			codebase = fileInfo.crc32;
		else
			codebase = android.os.SystemClock.uptimeMillis();
		if (null != fileInfo.format)
			extension = fileInfo.format.getExtensions()[0];
		else
			extension = ".fb2";
		if (fileInfo.isArchive) {
			// No info about archive type
			extension += ".pack";
		}
		String filename = Long.valueOf(codebase).toString() + extension;
		try {
			File file = new File(mBasePath, filename);
			FileOutputStream outputStream = new FileOutputStream(file);
			inputStream.reset();
			long size = Utils.copyStreamContent(outputStream, inputStream);
			outputStream.close();
			if (size > 0) {
				FileInfo newFileInfo = new FileInfo(fileInfo);
				// Set new path & name
				if (fileInfo.isArchive) {
					newFileInfo.arcname = file.getAbsolutePath();
					newFileInfo.arcsize = size;
				} else {
					newFileInfo.setFilename(file.getName());
					newFileInfo.path = file.getParent();
					newFileInfo.pathname = file.getAbsolutePath();
					newFileInfo.setCreateTime(file.lastModified());
					newFileInfo.size = size;
				}
				bookInfo = new BookInfo(newFileInfo);
			}
		} catch (Exception e) {
			log.e("Exception while saving stream: " + e.getMessage());
		}
		return bookInfo;
	}
}
