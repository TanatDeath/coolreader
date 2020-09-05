package org.coolreader.crengine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
	ImageView bmImage;

	public DownloadImageTask(ImageView bmImage) {
		this.bmImage = bmImage;
	}

	protected Bitmap doInBackground(String... urls) {
		String urldisplay = urls[0];
		Bitmap mIcon11 = null;
		Bitmap resizedBitmap = null;
		try {
			String redir_url = Utils.getUrlLoc(new java.net.URL(urldisplay));
			InputStream in = null;
			if (StrUtils.isEmptyStr(redir_url))
				in = new java.net.URL(urldisplay).openStream();
			else
				in = new java.net.URL(redir_url).openStream();
			mIcon11 = BitmapFactory.decodeStream(in);
			final int maxSize = 200;
			int outWidth;
			int outHeight;
			if (mIcon11 != null) {
				int inWidth = mIcon11.getWidth();
				int inHeight = mIcon11.getHeight();
				if (inWidth > inHeight) {
					outWidth = maxSize;
					outHeight = (inHeight * maxSize) / inWidth;
				} else {
					outHeight = maxSize;
					outWidth = (inWidth * maxSize) / inHeight;
				}
				resizedBitmap = Bitmap.createScaledBitmap(mIcon11, outWidth, outHeight, false);
			}
		} catch (Exception e) {
			Log.e("Error", e.getMessage());
			e.printStackTrace();
		}
		return resizedBitmap;
	}

	protected void onPostExecute(Bitmap result) {
		if (result != null) bmImage.setImageBitmap(result);
	}
}