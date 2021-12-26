package org.coolreader.readerview;

import android.graphics.Bitmap;

import org.coolreader.crengine.ImageInfo;
import org.coolreader.crengine.PositionProperties;

public class BitmapInfo {

	final BitmapFactory mFactory;
	public BitmapInfo(BitmapFactory factory) {
		mFactory = factory;
	}

	Bitmap bitmap;
	PositionProperties position;
	ImageInfo imageInfo;
	void recycle()
	{
		mFactory.release(bitmap);
		bitmap = null;
		position = null;
		imageInfo = null;
	}
	boolean isReleased() {
		return bitmap == null;
	}
	@Override
	public String toString() {
		return "BitmapInfo [position=" + position + "]";
	}

}


