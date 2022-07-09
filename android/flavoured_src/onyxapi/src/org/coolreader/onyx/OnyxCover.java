package org.coolreader.onyx;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.DisplayMetrics;

import com.onyx.android.sdk.utils.BitmapUtils;

import org.coolreader.CoolReader;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.Services;
import org.coolreader.utils.Utils;

import java.io.File;

public class OnyxCover {

	public static void setCoverPage(CoolReader mActivity, FileInfo item) {
		DisplayMetrics outMetrics = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		int mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		int w1 = mWindowSize * 4 / 10;
		int h1 = w1 * 4 / 3;
		Bitmap bmp = Bitmap.createBitmap(w1, h1, Bitmap.Config.RGB_565);
		Services.getCoverpageManager().drawCoverpageFor(mActivity.getDB(), item, bmp, false,
				(file, bitmap) -> {
					boolean success = false;
					String targetDir = "/data/local/assets/images";
					String targetPathString = targetDir + File.separator + "standby-1.png";//file name format: standby-{num}.png, num starts from 1
					Point p = Utils.getScreenResolution(mActivity.getApplicationContext());
					int h = p.y;
					int w = p.x;
					if (bitmap.getWidth() != h || bitmap.getHeight() != w) {
						bitmap = Bitmap.createScaledBitmap(bitmap, h, w, true);
					}
					try {
						success = BitmapUtils.saveBitmapToFile(bitmap, targetDir, targetPathString, true);
					} catch (Exception e) {
						// do nothing
					}
					if (!success) {
						try {
							success = BitmapUtils.savePngToFile(bitmap, targetDir, targetPathString, true);
						} catch (Exception e) {
							// do nothing
						}
					}
					//send broadcast
					if (success) {
						Intent intent = new Intent("update_standby_pic");
						mActivity.sendBroadcast(intent);
						mActivity.showToast("ok");
					}
				});
	}

}
