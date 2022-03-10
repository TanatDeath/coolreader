package org.coolreader.utils;

public class StorageDirectory {

	public static final int STORAGE_INTERNAL = 0;
	public static final int STORAGE_SD_CARD = 1;
	public static final int ROOT = 2;
	public static final int NOT_KNOWN = 3;
	public static final int STORAGE_USB_CARD = 4;

	public String mPath;
	public String mName;
	public int mDirType;

	public StorageDirectory(String path, String name, int dirType) {
		this.mPath = path;
		this.mName = name;
		this.mDirType = dirType;
	}

}
