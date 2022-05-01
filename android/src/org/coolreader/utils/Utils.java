package org.coolreader.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FileInfo.SortOrder;
import org.coolreader.crengine.FileInfoOperationListener;
import org.coolreader.crengine.L;
import org.coolreader.crengine.OPDSUtil;
import org.coolreader.crengine.Scanner;
import org.coolreader.crengine.Selection;
import org.coolreader.crengine.Services;
import org.coolreader.db.BaseDB;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

public class Utils {

	private static HttpURLConnection connection;

	public static final int CONNECT_TIMEOUT = 60000;
	public static final int READ_TIMEOUT = 60000;

	public static long timeStamp() {
		return android.os.SystemClock.uptimeMillis();
	}
	
	public static long timeInterval(long startTime) {
		return android.os.SystemClock.uptimeMillis() - startTime;
	}

	//NB: this method does not converts &qout; &amp; etc.
	public static String cleanupHtmlTags2(String src) {
		StringBuilder buf = new StringBuilder();
		boolean insideTag = false;
		for (char ch : src.toCharArray()) {
			if (ch=='<') {
				insideTag = true;
			} else if (ch=='>') {
				insideTag = false;
				buf.append(' ');
			} else if (!insideTag) {
				buf.append(ch);
			}
		}
		return buf.toString();
	}

	public static String cleanupHtmlTags(String src) {
		return android.text.Html.fromHtml(src).toString();
	}
	
	public static String authorNameFileAs(String name) {
		if (name == null || name.length() == 0)
			return name;
		String name1 = name;
		String name2 = "";
		if (name.contains("(")) {
			name1 = name.substring(0,name.indexOf("(")).trim();
			name2 = name.substring(name.indexOf("(")).trim();
		}
		int lastSpace = name1.lastIndexOf(' ');
		if (lastSpace >= 0 && lastSpace < name1.length() - 1)
			return name1.substring(lastSpace + 1) + " " + name1.substring(0, lastSpace)+" "+name2;
		return name1.trim()+" "+name2.trim();
	}

	public static boolean moveFile(File oldPlace, File newPlace) {
		return moveFile(oldPlace, newPlace, true);
	}

	public static boolean copyFile(File oldPlace, File newPlace) {
		return moveFile(oldPlace, newPlace, false);
	}

	public static long copyStreamContent(OutputStream os, InputStream is) throws IOException {
		long totalSize = 0;
		byte[] buf = new byte[0x10000];
		for (;;) {
			int bytesRead = is.read(buf);
			if ( bytesRead<=0 )
				break;
			totalSize += bytesRead;
			os.write(buf, 0, bytesRead);
		}
		return totalSize;
	}
	
	private static boolean moveFile(File oldPlace, File newPlace, boolean removeOld) {
		boolean removeNewFile = true;
		Log.i("cr3", "Moving file " + oldPlace.getAbsolutePath() + " to " + newPlace.getAbsolutePath());
		if ( !oldPlace.exists() ) {
			Log.i("cr3", "File " + oldPlace.getAbsolutePath() + " does not exist!");
			return false;
		}
		try {
			if (!newPlace.createNewFile())
				return false; // cannot create file
			try (FileOutputStream os = new FileOutputStream(newPlace);
				 FileInputStream is = new FileInputStream(oldPlace)) {
				copyStreamContent(os, is);
				removeNewFile = false;
				oldPlace.delete();
				return true;
			}
		} catch ( IOException e ) {
			return false;
		} finally {
			if ( removeNewFile )
				newPlace.delete();
		}
	}
	
	public static boolean restoreFromBackup(BaseDB db, File f) {
		File backup = new File(f.getAbsolutePath() + ".good.bak.2");

		if (f.exists()) {
			db.mDeletedDBFile = f.getPath();
			f.delete();
		}
		if (backup.exists()) {
			if (backup.renameTo(f)) {
				db.mBackupRestoredDBFile = backup.getPath();
				db.mBackupRestoredToDBFile = f.getPath();
				return true;
			}
		}
		return false;
	}
	
	public static void backupFile(File f) {
		if (!f.exists())
			return;
		File backup = getBackupFileName(f, true);
		L.i("Creating backup of file " + f + " as " + backup);
		if (Utils.copyFile(f, backup)) {
			L.w("copying of DB has been failed");
		}
		f.renameTo(backup);
	}
	
	public static void moveCorruptedFileToBackup(BaseDB db, File f) {
		if (!f.exists())
			return;
		Log.e("cr3", "Moving corrupted file " + f + " to backup.");
		File backup = getBackupFileName(f, false);
		db.mOriginalDBFile = f.getPath();
		f.renameTo(backup);
		db.mBackedDBFile = backup.getPath();
	}
	
	private final static int MAX_BACKUP_FILES = 5;
	private static File getBackupFileName(File f, boolean isGoodBackup) {
		File f2 = null;
		String prefix = f.getAbsolutePath() + (isGoodBackup ? ".good.bak." : ".corrupted.bak.");
		for (int i=MAX_BACKUP_FILES - 1; i > 2; i--) {
			File to = new File(prefix + i); 
			File from = new File(prefix + (i-1));
			if (to.exists())
				to.delete();
			if (from.exists()) {
				if (!from.renameTo(to))
					Log.e("cr3", "Cannot rename DB file " + from + " to " + to);
			}
		}
		f2 = new File(prefix + 2);
		if (f2.exists())
			if (!f2.delete())
				Log.e("cr3", "Cannot remove DB file " + f2);
		return f2;
	}

	public static File getReplacementFile(File f) {
		if (!f.exists())
			return f;
		String name = f.getName();
		// get extension
		String ext = DocumentFormat.getSupportedExtension(name);
		if (null == ext) {
			// unsupported file format
			int pos = name.lastIndexOf('.');
			if (pos > 0 && pos < name.length() - 1)
				ext = name.substring(pos + 1);
		}
		int number = 1;
		boolean found = false;
		File file = null;
		String basename;
		if (null != ext)
			basename = name.substring(0, name.length() - ext.length() - 1);
		else
			basename = name;
		while (number < 100) {
			if (null != ext)
				file = new File(f.getParent(), basename + " (" + number + ")." + ext);
			else
				file = new File(f.getParent(), basename + " (" + number + ")");
			if (!file.exists()) {
				found = true;
				break;
			}
			number++;
		}
		return found ? file : null;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static DocumentFile getDocumentFile(FileInfo fi, Context context, Uri sdCardUri) {
		DocumentFile docFile = null;
		String filePath;
		if (fi.isArchive && fi.arcname != null)
			filePath = fi.arcname;
		else
			filePath = fi.pathname;
		if (null != filePath) {
			File f = new File(filePath);
			filePath = f.getAbsolutePath();
			docFile = DocumentFile.fromTreeUri(context, sdCardUri);
			if (null != docFile) {
				String[] parts = filePath.split("\\/");
				for (int i = 3; i < parts.length; i++) {
					docFile = docFile.findFile(parts[i]);
					if (null == docFile)
						break;
				}
			}
		}
		return docFile;
	}

	public static boolean deleteFolder(FileInfo folder, FileInfoOperationListener bookDeleteCallback, FileInfoOperationListener readyCallback) {
		boolean res = deleteFolder_impl(new FileInfo(folder), bookDeleteCallback);
		readyCallback.onStatus(folder, res ? 0 : -1);
		return res;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static boolean deleteFolderDocTree(FileInfo folder, Context context, Uri sdCardUri, FileInfoOperationListener bookDeleteCallback, FileInfoOperationListener readyCallback) {
		boolean res = deleteFolderDocTree_impl(new FileInfo(folder), context, sdCardUri, bookDeleteCallback);
		readyCallback.onStatus(folder, res ? 0 : -1);
		return res;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static boolean deleteFolderDocTree_impl(FileInfo folder, Context context, Uri sdCardUri, FileInfoOperationListener bookDeleteCallback) {
		boolean res = true;
		Scanner scanner = Services.getScanner();
		scanner.listDirectory(folder, false, false);
		DocumentFile documentFile;
		int i;
		for (i = 0; i < folder.dirCount(); i++) {
			res = deleteFolderDocTree_impl(folder.getDir(i), context, sdCardUri, bookDeleteCallback);
			if (!res)
				break;
		}
		if (res) {
			for (i = 0; i < folder.fileCount(); i++) {
				FileInfo fi = folder.getFile(i);
				documentFile = getDocumentFile(fi, context, sdCardUri);
				if (null != documentFile) {
					res = documentFile.delete();
					bookDeleteCallback.onStatus(fi, res ? 0 : -1);
					if (!res) {
						break;
					}
				} else {
					bookDeleteCallback.onStatus(fi, -2);
					break;
				}
			}
		}
		if (res) {
			documentFile = getDocumentFile(folder, context, sdCardUri);
			if (null != documentFile) {
				res = documentFile.delete();
			}
		}
		return res;
	}

	private static boolean deleteFolder_impl(FileInfo folder, FileInfoOperationListener bookDeleteCallback) {
		boolean res = true;
		Scanner scanner = Services.getScanner();
		scanner.listDirectory(folder, false, false);
		int i;
		// delete recursively all child folders
		for (i = 0; i < folder.dirCount(); i++) {
			res = deleteFolder_impl(folder.getDir(i), bookDeleteCallback);
			if (!res)
				break;
		}
		if (res) {
			// if successfully -> delete files
			while (folder.fileCount() > 0) {
				FileInfo fi = folder.getFile(0);
				res = fi.deleteFile();
				bookDeleteCallback.onStatus(fi, res ? 0 : -1);
				if (!res) {
					break;
				}
			}
		}
		if (res) {
			// and if all previous successfully -> delete this folder.
			File fld = new File(folder.pathname);
			res = fld.delete();
		}
		return res;
	}

	private final static String LATIN_C0 =
		// 0xC0 .. 0xFF
		  "aaaaaaaceeeeiiiidnoooooxouuuuyps" 
		+ "aaaaaaaceeeeiiiidnoooooxouuuuypy";
	
	private static char convertCharCaseForSearch(char ch) {
		if (ch >= 'A' && ch <= 'Z')
			return (char)(ch - 'A' + 'a');
		if ( ch>=0xC0 && ch<=0xFF )
			return LATIN_C0.charAt(ch - 0xC0);
    	if ( ch>=0x410 && ch<=0x42F )
    		return (char)(ch + 0x20);
    	if ( ch>=0x390 && ch<=0x3aF )
    		return (char)(ch + 0x20);
    	if ( (ch >> 8)==0x1F ) { // greek
	        int n = ch & 255;
	        if (n<0x70) {
	            return (char)(ch & (~8));
	        } else if (n<0x80) {
	
	        } else if (n<0xF0) {
	            return (char)(ch & (~8));
	        }
	    }
		return ch;
	}
	
	public static boolean matchPatternInternal(String text, String pattern) {
		if (pattern == null)
			return true;
		if (text == null)
			return false;
		int textlen = text.length();
		int patternlen = pattern.length();
		if (textlen < patternlen)
			return false;
		for (int i=0; i <= textlen - patternlen; i++) {
			if (i > 0 && text.charAt(i-1) != ' ')
				continue; // match only beginning of words
			boolean eq = true;
			for (int j=0; j<patternlen; j++) {
				if (convertCharCaseForSearch(text.charAt(i + j)) != convertCharCaseForSearch(pattern.charAt(j))) {
					eq = false;
					break;
				}
			}
			if (eq)
				return true;
		}
		return false;
	}

	public static boolean matchPattern(String text, String pattern) {
		return (matchPatternInternal(text,pattern)) || (matchPatternInternal(
				StrUtils.getNonEmptyStr(text,true).toLowerCase(),
				StrUtils.getNonEmptyStr(pattern,true).toLowerCase()));
	}
	
	public static String[] splitByWhitespace(String str) {
		ArrayList<String> list = new ArrayList<String>();
		StringBuilder buf = new StringBuilder();
		for (int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			if (ch == ' ' || ch == '\t') {
				if (buf.length() > 0) {
					list.add(buf.toString());
					buf = new StringBuilder();
				}
			} else {
				buf.append(ch);
			}
		}
		if (buf.length() > 0)
			list.add(buf.toString());
		return list.toArray(new String[list.size()]);
	}

	public static boolean eq(String s1, String s2) {
		if (s1 == null) {
			return s2 == null;
		}
		return s1.equals(s2);
	}
	
	public static String formatAuthors( String authors ) {
		if ( authors==null || authors.length()==0 )
			return null;
		String[] list = authors.split("\\|");
		StringBuilder buf = new StringBuilder(authors.length());
		for ( String a : list ) {
			if ( buf.length()>0 )
				buf.append("; ");
			buf.append(Utils.authorNameFileAs(a).trim());
//			String[] items = a.split(" ");
//			if ( items.length==3 && items[1]!=null && items[1].length()>=1 )
//				buf.append(items[0] + " " + items[1].charAt(0) + ". " + items[2]);
//			else
//				buf.append(a);
		}
		return buf.toString();
	}

	public static String formatAuthorsNormalNames( String authors ) {
		if ( authors==null || authors.length()==0 )
			return null;
		String[] list = authors.split("\\|");
		StringBuilder buf = new StringBuilder(authors.length());
		for (String a : list) {
			if (buf.length() > 0)
				buf.append(", ");
			buf.append(a);
//			String[] items = a.split(" ");
//			if ( items.length==3 && items[1]!=null && items[1].length()>=1 )
//				buf.append(items[0] + " " + items[1].charAt(0) + ". " + items[2]);
//			else
//				buf.append(a);
		}
		return buf.toString();
	}

	public static String ntrim(String str) {
		if (str == null)
			return null;
		str = str.trim();
		if (str.length() == 0)
			return null;
		return str;
	}

	public static String concatWs(String str1, String str2, String ws) {
		if (empty(str1)) {
			if (empty(str2))
				return "";
			return str2;
		}
		if (empty(str2))
			return str1;
		return str1 + ws + str2;
	}
	
	public static boolean empty(String str) {
		if (str == null || str.length() == 0)
			return true;
		if (str.trim().length() == 0)
			return true;
		return false;
		
	}
	
	public static Drawable solidColorDrawable(final int color) {
		GradientDrawable d = new GradientDrawable();
		d.setColor(color);
		return d;
//		RectShape s = new RectShape();
//		
//		d.setShape(s);
//		return new Drawable() {
//			@Override
//			public void setColorFilter(ColorFilter cf) {
//			}
//			
//			@Override
//			public void setAlpha(int alpha) {
//			}
//			
//			@Override
//			public int getOpacity() {
//				return 0;
//			}
//			
//			@Override
//			public void draw(Canvas canvas) {
//				canvas.drawColor(0xFF000000 | color);
//			}
//		};
	}

	public static String formatSeries(String name, int number)
	{
		if (name == null || name.length() == 0)
			return null;
		if (number > 0)
			return "#" + number + " " + StrUtils.getNonEmptyStr(name, true).replace("|", "; ");
		else
			return StrUtils.getNonEmptyStr(name, true).replace("|", "; ");
	}
	
	public static String formatPercent(int percent)
	{
		if (percent <= 0)
			return null;
		return String.valueOf(percent / 100) + "." + String.valueOf(percent / 10 % 10) + "%";
	}

	public static String formatTime(Activity activity, long timeStamp)
	{
		if (timeStamp < 5000 * 60 * 60 * 24 * 1000)
			return "";
		TimeZone tz = java.util.TimeZone.getDefault();
		Calendar c = Calendar.getInstance(tz);
		c.setTimeInMillis(timeStamp);
		return DateFormat.getTimeFormat(activity.getApplicationContext()).format(c.getTime());
	}

	public static String formatDateFixed(long timeStamp)
	{
		//if ( timeStamp<5000*60*60*24*1000 )
		//	return "";
		TimeZone tz = java.util.TimeZone.getDefault();
		Calendar c = Calendar.getInstance(tz);
		c.setTimeInMillis(timeStamp);
		int m = c.get(Calendar.MONTH)+1;
		String mm = m < 10 ? "0"+ m : ""+m;
		String dd = c.get(Calendar.DAY_OF_MONTH) < 10 ? "0"+c.get(Calendar.DAY_OF_MONTH) : ""+c.get(Calendar.DAY_OF_MONTH);
		return c.get(Calendar.YEAR)+"-"+mm+"-"+dd;
	}
	
	public static String formatDate(Activity activity, long timeStamp)
	{
		if (timeStamp < 5000 * 60 * 60 * 24 * 1000)
			return "";
		TimeZone tz = java.util.TimeZone.getDefault();
		Calendar now = Calendar.getInstance(tz);
		Calendar c = Calendar.getInstance(tz);
		c.setTimeInMillis(timeStamp);
		if (c.get(Calendar.YEAR) < 1980)
			return "";
		if (c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
				&& c.get(Calendar.MONTH)==now.get(Calendar.MONTH)
				&& c.get(Calendar.DAY_OF_MONTH)==now.get(Calendar.DAY_OF_MONTH)) {
			return formatTime(activity, timeStamp);
		} else {
			return DateFormat.getDateFormat(activity.getApplicationContext()).format(c.getTime());
		}
	}

    public static String formatDate2(Activity activity, long timeStamp)
    {
        if (timeStamp < 5000 * 60 * 60 * 24 * 1000)
            return "";
        TimeZone tz = java.util.TimeZone.getDefault();
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(timeStamp);
        if ( c.get(Calendar.YEAR)<1980 )
            return "";
        return DateFormat.getDateFormat(activity.getApplicationContext()).format(c.getTime());
    }
	
//	static private ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>(); 
//	static private ThreadLocal<SimpleDateFormat> timeFormatThreadLocal = new ThreadLocal<SimpleDateFormat>();
//	static private SimpleDateFormat dateFormat() {
//		if (dateFormatThreadLocal.get() == null)
//			dateFormatThreadLocal.set(new SimpleDateFormat("dd.MM.yy", Locale.getDefault()));
//		return dateFormatThreadLocal.get();
//	}
//	
//	static private SimpleDateFormat timeFormat() {
//		if (timeFormatThreadLocal.get() == null)
//			timeFormatThreadLocal.set(new SimpleDateFormat("HH:mm", Locale.getDefault()));
//		return timeFormatThreadLocal.get();
//	}
	
	public static String formatTimeElapsed( long timeElapsed )
	{
		int hours = (int) timeElapsed/(60*60*1000);
		int min = (int) timeElapsed % (60*60*1000) / (60*1000);
//		int sec = (int) timeElapsed % (60*60*1000) % (60 * 1000) / 1000;
//		return String.format("%02d:%02d:%02d", hours, min, sec);
		return String.format("%d:%02d", hours, min);
	}
	public static String formatSize( int size )
	{
		if ( size==0 )
			return "";
		if ( size<10000 )
			return String.valueOf(size);
		else if ( size<1000000 )
			return String.valueOf(size/1000) + "K";
		else if ( size<10000000 )
			return String.valueOf(size/1000000) + "." + String.valueOf(size%1000000/100000) + "M";
		else
			return String.valueOf(size/1000000) + "M";
	}
	public static String formatSize( long size )
	{
		if ( size==0 )
			return "";
		if ( size<10000 )
			return String.valueOf(size);
		else if ( size<1000000 )
			return String.valueOf(size/1000) + "K";
		else if ( size<10000000 )		// < 10M
			return String.valueOf(size/1000000) + "." + String.valueOf(size%1000000/100000) + "M";
		else if ( size<1000000000 )		// < 1G
			return String.valueOf(size/1000000) + "M";
		else if ( size<10000000000L )	// < 10G
			return String.valueOf(size/1000000000L) + "." + String.valueOf(size%1000000000L/100000000L) + "G";
		else
			return String.valueOf(size/1000000000L) + "G";
	}

	public static String formatFileInfo(Activity activity, FileInfo item) {
		int prof = item.getProfileId();
		String sProf = " {p"+prof+"}";
		if (prof == 0) sProf = "";
		boolean bEmptyF = item.format==null;
		if (!bEmptyF) bEmptyF = item.format == DocumentFormat.NONE;
		return formatSize(item.size) + " " + (bEmptyF ? "" : item.format.name().toLowerCase()) + " " +
				formatDate(activity, item.getCreateTime()); //+sProf; // decided to unlink
	}

	public static String formatLastPosition(Activity activity, Bookmark pos) {
		if ( pos!=null && pos.getPercent() > 0 && pos.getTimeStamp() > 0) {
			//return formatPercent(pos.getPercent()) + " " + formatDate(activity, pos.getTimeStamp());
			return formatPercent(pos.getPercent()) + " " + formatDate(activity, pos.getTimeStamp()) + " (" + formatTimeElapsed(pos.getTimeElapsed()) + ")";
		} else {
			return "";
		}
	}

	public static String formatReadingState(Activity activity, FileInfo item) {
		String state = "";
		if (item.getRate() > 0 && item.getRate() <= 5) {
			String[] stars = new String[] {
					"*",
					"**",
					"***",
					"****",
					"*****",
			};
			state = state + stars[item.getRate() - 1] + " ";
		}
		if (item.getReadingState() > 0) {
			String stateName = "";
			int n = item.getReadingState();
			if (n == FileInfo.STATE_READING)
				stateName = activity.getString(R.string.book_state_reading);
			else if (n == FileInfo.STATE_TO_READ)
				stateName = activity.getString(R.string.book_state_toread);
			else if (n == FileInfo.STATE_FINISHED)
				stateName = activity.getString(R.string.book_state_finished);
			state = state + "[" + stateName + "] ";
		}
		return state;
	}
	
	public static void drawFrame(Canvas canvas, Rect rect, Paint paint) {
		canvas.drawRect(new Rect(rect.left, rect.top, rect.right, rect.top + 1), paint);
		canvas.drawRect(new Rect(rect.left, rect.top + 1, rect.left + 1, rect.bottom), paint);
		canvas.drawRect(new Rect(rect.right - 1, rect.top + 1, rect.right, rect.bottom), paint);
		canvas.drawRect(new Rect(rect.left + 1, rect.bottom - 1, rect.right - 1, rect.bottom), paint);
	}

	public static void drawFrame2(Canvas canvas, Rect rect, Paint paint, int thick, float textSize, String sText) {
		for (int i = 1; i<=thick; i++) {
			int marg = 2;
			int l = rect.left+marg;
			int r = rect.right-marg;
			int t = rect.top+marg;
			int b = rect.bottom-marg;
			canvas.drawRect(new Rect(l, t, r, t + thick), paint);
			canvas.drawRect(new Rect(l, t + thick, l + thick, b), paint);
			canvas.drawRect(new Rect(r - thick, t + thick, r, b), paint);
			canvas.drawRect(new Rect(l + thick, b - thick, r - thick, b), paint);
			canvas.drawText("test text", 0, 0, paint);
			paint.setTextSize(textSize);
			canvas.drawText(sText, 20, b - textSize + 10, paint);
		}
	}
	public static void drawFrame3(Canvas canvas, Rect rect, Paint paint, int thick, float textSize, String sText) {
		for (int i = 1; i<=thick; i++) {
			int marg = 2;
			int l = rect.left+marg;
			int r = rect.right-marg;
			int t = rect.top+marg;
			int b = rect.bottom-marg;
			int div10w = (r - l) / 10;
			int div10h = (b - t) / 10;
			for (int ii=0; ii < div10w; ii++) {
				canvas.drawRect(new Rect(l + (ii * 20), t, l + (ii * 20) + 10, t + thick), paint);
				canvas.drawRect(new Rect(l + (ii * 20), b - thick, l + (ii * 20) + 10, b), paint);
			}
			for (int ii=0; ii < div10h; ii++) {
				canvas.drawRect(new Rect(l, t  + (ii * 20), l + thick, t + (ii * 20) + 10), paint);
				canvas.drawRect(new Rect(r - thick, t + (ii * 20), r, t + (ii * 20) + 10), paint);
			}
			paint.setTextSize(textSize);
			canvas.drawText(sText, 20, b - textSize + 10, paint);
		}
	}
	
	public static Paint createSolidPaint(int color) {
		Paint res = new Paint();
		res.setStyle(Paint.Style.FILL);
		res.setColor(color);
		return res;
	}

	public static float colorLuminance(int color) {
		float r = ((float)Color.red(color))/255f;
		float g = ((float)Color.green(color))/255f;
		float b = ((float)Color.blue(color))/255f;
		float res = 0.2126f*r + 0.7152f*g + 0.0722f*b;
		if (res > 1.f)		// for case of rounding error
			res = 1.f;
		return res;
	}

	/**
	 * Returns a darker (or lighter) color.
	 * If the factor is greater than 100, this functions returns a darker color.
	 * Setting factor to 300 returns a color that has one-third the brightness.
	 * If the factor is less than 100, the return color is lighter, but we recommend using the lighter() function for this purpose.
	 * If the factor is 0 or negative, the return value is unspecified.
	 * @param color input color value.
	 * @param factor color change factor, 100 - no change.
	 * @return new color value.
	 */
	public static int darkerColor(int color, int factor) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		if (factor != 0)
			hsv[2] /= ((float)factor)/100f;
		return Color.HSVToColor(hsv);
	}

	/**
	 * Returns a lighter (or darker) color, but does not change this object.
	 * If the factor is greater than 100, this functions returns a lighter color.
	 * Setting factor to 150 returns a color that is 50% brighter.
	 * If the factor is less than 100, the return color is darker, but we recommend using the darker() function for this purpose.
	 * If the factor is 0 or negative, the return value is unspecified.
	 * @param color input color value.
	 * @param factor color change factor, 100 - no change.
	 * @return new color value.
	 */
	public static int lighterColor(int color, int factor) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		if (hsv[2] < 0.001f)		// 0
			hsv[2] = 0.1f;
		hsv[2] *= ((float)factor)/100f;
		return Color.HSVToColor(hsv);
	}

	/**
	 * Compares two strings - with numbers sorted by value.
	 * @param st1
	 * @param st2
	 * @return
	 */
	public static int cmp( String st1, String st2 )
	{
		if ( st1==null && st2==null )
			return 0;
		if ( st1==null )
			return -1;
		if ( st2==null )
			return 1;

		String str1 = st1;
		if (str1.endsWith("...")) str1 = str1.substring(0,str1.length()-3);
		String str2 = st2;
		if (str2.endsWith("...")) str2 = str2.substring(0,str2.length()-3);

		str1 = str1.toLowerCase();
		str2 = str2.toLowerCase();
		int p1 = 0;
		int p2 = 0;
		for ( ;; ) {
			if ( p1>=str1.length() ) {
				if ( p2>=str2.length() )
					return 0;
				return 1;
			}
			if ( p2>=str2.length() )
				return -1;
			char ch1 = str1.charAt(p1);
			char ch2 = str2.charAt(p2);
			if ( ch1>='0' && ch1<='9' && ch2>='0' && ch2<='9' ) {
				int n1 = 0;
				int n2 = 0;
				while ( ch1>='0' && ch1<='9' ) {
					p1++;
					n1 = n1 * 10 + (ch1-'0');
					if ( p1>=str1.length() )
						break;
					ch1 = str1.charAt(p1);
				}
				while ( ch2>='0' && ch2<='9' ) {
					p2++;
					n2 = n2 * 10 + (ch2-'0');
					if ( p2>=str2.length() )
						break;
					ch2 = str2.charAt(p2);
				}
				int c = SortOrder.cmp(n1, n2);
				if ( c!=0 )
					return c;
			} else {
				if ( ch1<ch2 )
					return -1;
				if ( ch1>ch2 )
					return 1;
				p1++;
				p2++;
			}
		}
	}

	public static String transcribeFileName( String fileName ) {
		StringBuilder buf = new StringBuilder(fileName.length());
		for ( char ch : fileName.toCharArray() ) {
			boolean found = false;
			if ( ((ch>='a' && ch<='z') || (ch>='A' && ch<='Z') || (ch>='0' && ch<='9') || ch=='-' || ch=='_' || ch=='(' || ch==')')) {
				buf.append(ch);
				continue;
			}
			for ( OPDSUtil.SubstTable t : Utils.substTables ) {
				if ( t.isInRange(ch) ) {
					buf.append(t.get(ch));
					found = true;
				}
			}
			if ( found )
				continue;
			buf.append("_");
		}
		return buf.toString();
	}

	final static OPDSUtil.SubstTable[] substTables = {
		new OPDSUtil.SubstTable(0x430, new String[]{"a", "b", "v", "g", "d", "e", "zh", "z", "i", "j", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f", "h", "c", "ch", "sh", "sch", "'", "y", "i", "e", "yu", "ya"}),
		new OPDSUtil.SubstTable(0x410, new String[]{"A", "B", "V", "G", "D", "E", "Zh", "Z", "I", "J", "K", "L", "M", "N", "O", "P", "R", "S", "T", "U", "F", "H", "C", "Ch", "Sh", "Sch", "'", "Y", "I", "E", "Yu", "Ya"}),
	};

	public static String transcribeWithLimit(String str, int maxLen) {
		str = transcribeFileName(str);
		if (str.length() > maxLen)
			str = str.substring(0, maxLen);
		return str;
	}

	// to support API LEVEL 3: View.setContentDescription() has been added only since API LEVEL 4
	public static void setContentDescription(View view, CharSequence text) {
		Method m;
		try {
			m = view.getClass().getMethod("setContentDescription", CharSequence.class);
			m.invoke(view, text);
		} catch (NoSuchMethodException e) {
			// Ignore
		} catch (IllegalArgumentException e) {
			// Ignore
		} catch (IllegalAccessException e) {
			// Ignore
		} catch (InvocationTargetException e) {
			// Ignore
		}
	}

	public static int resolveResourceIdByAttr(Context ctx, int attrId, int fallbackResId) {
		int resId;
		TypedArray a = ctx.getTheme().obtainStyledAttributes(new int[] { attrId });
		resId = a.getResourceId(0, 0);
		a.recycle();
		if (0 == resId)
			resId = fallbackResId;
		return resId;
	}

	public static String readFileToStringOrEmpty(String filePath) {
		File f = new File(filePath);
		if (!f.exists()) return "";
		return readFileToString(filePath);
	}

	public static String readFileToString(String filePath)
	{
		StringBuilder contentBuilder = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null)
			{
				contentBuilder.append(sCurrentLine).append("\n");
			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return contentBuilder.toString();
	}

	public static ArrayList<String> readFileToArrayList(String filePath)
	{
		ArrayList<String> resS= new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null)
			{
				resS.add(sCurrentLine);
			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return resS;
	}

	public static void copyFile(String fromPath, String toPath) throws IOException
	{
		File f = new File(fromPath);
		FileInputStream is = new FileInputStream(f);
		byte[] bytesArray = new byte[1000];
		int bytesRead = 1000;
		OutputStream os = new FileOutputStream(toPath);
		while (bytesRead != -1) {
			bytesRead = is.read(bytesArray, 0, 1000);
			if (bytesRead != -1) os.write(bytesArray,0,bytesRead);
		}
		os.close();
		is.close();
	}

	public static void saveStreamToFile(InputStream is, String toPath) throws IOException
	{
		byte[] bytesArray = new byte[1000];
		int bytesRead = 1000;
		OutputStream os = new FileOutputStream(toPath);
		while (bytesRead != -1) {
			bytesRead = is.read(bytesArray, 0, 1000);
			if (bytesRead != -1) os.write(bytesArray,0,bytesRead);
		}
		os.close();
		is.close();
	}

	public static void saveStreamToFileDontClose(InputStream is, String toPath) throws IOException
	{
		byte[] bytesArray = new byte[1000];
		int bytesRead = 1000;
		OutputStream os = new FileOutputStream(toPath);
		while (bytesRead != -1) {
			bytesRead = is.read(bytesArray, 0, 1000);
			if (bytesRead != -1) os.write(bytesArray,0,bytesRead);
		}
		os.close();
		//is.close();
	}

	public static void saveStringToFileSafe(String data, String toPath)
	{
		try {
			final File fJson = new File(toPath);
			BufferedWriter bw = null;
			FileWriter fw = null;
			char[] bytesArray = new char[1000];
			int bytesRead = 1000;
			try {
				fw = new FileWriter(fJson);
				bw = new BufferedWriter(fw);
				bw.write(data);
				bw.close();
				fw.close();
			} catch (Exception e) {
			}
		} catch (Exception e) {
		}
	}

	public static ByteArrayOutputStream inputStreamToBaos(InputStream is) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = is.read(buffer)) > -1 ) {
			baos.write(buffer, 0, len);
		}
		baos.flush();
		return baos;
	}

	public static String getUrlLoc(URL url) {
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("User-Agent", "KnownReader (Android)");
			connection.setInstanceFollowRedirects(true);
			connection.setUseCaches(false);
			connection.setAllowUserInteraction(false);
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			connection.setDoInput(true);
			int response = -1;
			response = connection.getResponseCode();
			L.i("opds: " + connection.getResponseMessage());
			if (response == 301 || response == 302 || response == 307 || response == 303) {
				// redirects
				String redirect = connection.getHeaderField("Location");
				return redirect;
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	public static String getFileExtension(String fileName) {
		if (StrUtils.isEmptyStr(fileName)) {
			return "";
		} else {
			int index = fileName.lastIndexOf('.');
			return index >= 0 ? fileName.substring(index + 1).toLowerCase() : "";
		}
	}

	public static String getFileNameWOExtension(String fileName) {
		if (StrUtils.isEmptyStr(fileName)) {
			return "";
		} else {
			int index = fileName.lastIndexOf('.');
			return index >= 0 ? fileName.substring(0, index) : "";
		}
	}

	public static String getFileExtension(File file) {
		return getFileExtension(file.getName());
	}

	public static String getFileNameWOExtension(File file) {
		return getFileNameWOExtension(file.getName());
	}

	public static void hideView(View l) {
		if (l == null) return;
		try {
			((ViewGroup) l.getParent()).removeView(l);
		} catch (Exception e) {
			//do nothing
		}
	}

	public static void setDashedButton(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_dashed_border);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	}

	public static void setSolidButton(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_solid_border);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	}

	public static void setSolidButton1(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_solid_border1);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	}

	public static void setDashedButton1(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_dashed_border1);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	}

	public static void setDashedButtonEink(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_dashed_border_eink);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	}

	public static void setSolidButtonEink(ImageView btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_solid_border_eink);
	}

	public static void setWhiteButtonEink(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundColor(Color.WHITE);
	}

	public static void setWhiteButtonEink(ImageView btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundColor(Color.WHITE);
	}

	public static void setSolidButtonEink(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_solid_border_eink);
		else {
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			btn.setBackgroundColor(Color.WHITE);
		}
	}

	public static void setSolidButtonEink(ImageButton btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_solid_border_eink);
		else
			btn.setBackgroundColor(Color.WHITE);
	}

	public static void setSolidLLEink(LinearLayout v) {
		if (v == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			v.setBackgroundResource(R.drawable.button_bg_solid_border_eink);
		else
			v.setBackgroundColor(Color.WHITE);
	}

	public static void setSolidEditEink(EditText edt) {
		if (edt == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			edt.setBackgroundResource(R.drawable.edit_bg_solid_border_eink);
		else {
			edt.setBackgroundColor(Color.WHITE);
		}
	}

	public static void setBtnBackground(Object o, Drawable d, boolean isEInk) {
		if (o == null) return;
		if (o instanceof Button) {
			Button btn = (Button) o;
			if (!isEInk)
				btn.setBackground(d);
			else
				Utils.setSolidButtonEink(btn);
		}
		if (o instanceof ImageButton) {
			ImageButton btn = (ImageButton) o;
			if (!isEInk)
				btn.setBackground(d);
			else
				Utils.setSolidButtonEink(btn);
		}
	}

	public static int findNearestIndex(List<Integer> list, int value) {
		int res = -1;
		if (list != null && list.size() > 0) {
			int min = Math.abs(list.get(0) - value);
			int index = 0;
			int diff;
			for (int i = 1; i < list.size(); i++) {
				diff = Math.abs(list.get(i) - value);
				if (diff < min) {
					min = diff;
					index = i;
				}
				if (diff == 0)
					break;
			}
			res = index;
		}
		return res;
	}

	public static Integer findNearestValue(List<Integer> list, int value) {
		Integer res = null;
		if (list != null && list.size() > 0) {
			int min = Math.abs(list.get(0) - value);
			int index = 0;
			int diff;
			for (int i = 1; i < list.size(); i++) {
				diff = Math.abs(list.get(i) - value);
				if (diff < min) {
					min = diff;
					index = i;
				}
				if (diff == 0)
					break;
			}
			res = list.get(index);
		}
		return res;
	}

	public static HashMap<Integer, Integer> getThemeColors(CoolReader cr, boolean forEink) {
		HashMap<Integer, Integer> res = new HashMap<>();
		int colorGrayC;
		int colorGray;
		int colorIcon;
		int colorIconL;
		int colorThemeBlue;
		int colorThemeGreen;
		int colorThemeGray;
		int colorThemeGray2;
		TypedArray a = cr.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon,
						R.attr.colorIconL,
						R.attr.colorThemeBlue,
						R.attr.colorThemeGreen,
						R.attr.colorThemeGray,
						R.attr.colorThemeGray2});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorGray = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.BLACK);
		colorIconL = a.getColor(3, Color.GRAY);
		colorThemeBlue = a.getColor(4, Color.BLUE);
		colorThemeGreen = a.getColor(5, Color.GREEN);
		colorThemeGray = a.getColor(6, Color.GRAY);
		colorThemeGray2 = a.getColor(7, Color.GRAY);
		a.recycle();
		if (forEink) {
			res.put(R.attr.colorThemeGray2Contrast, Color.WHITE);
			res.put(R.attr.colorThemeGray2, Color.BLACK);
			res.put(R.attr.colorIcon, Color.BLACK);
			res.put(R.attr.colorIconL, Color.GRAY);
			res.put(R.attr.colorThemeBlue, Color.GRAY);
			res.put(R.attr.colorThemeGreen, Color.GRAY);
			res.put(R.attr.colorThemeGray, Color.GRAY);
			res.put(R.attr.colorThemeGray2, Color.GRAY);
		}
		res.put(R.attr.colorThemeGray2Contrast, colorGrayC);
		res.put(R.attr.colorThemeGray2, colorGray);
		res.put(R.attr.colorIcon, colorIcon);
		res.put(R.attr.colorIconL, colorIconL);
		res.put(R.attr.colorThemeBlue, colorThemeBlue);
		res.put(R.attr.colorThemeGreen, colorThemeGreen);
		res.put(R.attr.colorThemeGray, colorThemeGray);
		return res;
	}

	public static String byteToHex(int n) {
		String s = Integer.toHexString(n & 255);
		if (s.length()<2)
			s = "0" + s;
		return s;
	}

	public static String colorToHex(int n) {
		return ("#" + byteToHex(Color.red(n))
				+ byteToHex(Color.green(n))
				+ byteToHex(Color.blue(n))).toUpperCase();
	}

	public static int parseInt(String str, int defValue) {
		return parseInt(str, defValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public static int parseInt(String str, int defValue, int minValue, int maxValue) {
		int n;
		if (null == str)
			return defValue;
		try {
			n = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			n = defValue;
		}
		if (n < minValue)
			n = minValue;
		else if (n > maxValue)
			n = maxValue;
		return n;
	}

	public static double parseDouble(String str, double defValue) {
		return parseDouble(str, defValue, Double.MIN_VALUE, Double.MAX_VALUE);
	}

	public static double parseDouble(String str, double defValue, double minValue, double maxValue) {
		double n;
		if (null == str)
			return defValue;
		try {
			n = Double.parseDouble(str);
		} catch (NumberFormatException e) {
			n = defValue;
		}
		if (n < minValue)
			n = minValue;
		else if (n > maxValue)
			n = maxValue;
		return n;
	}

	public static void setHighLightedText(TextView tv, String textToHighlight, int color) {
		if (StrUtils.isEmptyStr(textToHighlight)) return;
		String tvt = tv.getText().toString();
		if (StrUtils.isEmptyStr(tvt)) return;
		int ofe = tvt.toUpperCase().indexOf(textToHighlight.toUpperCase(), 0);
		Spannable wordToSpan = new SpannableString(tv.getText());
		for (int ofs = 0; ofs < tvt.length() && ofe != -1; ofs = ofe + 1) {
			ofe = tvt.toUpperCase().indexOf(textToHighlight.toUpperCase(), ofs);
			if (ofe == -1)
				break;
			else {
				// set color here
				wordToSpan.setSpan(new BackgroundColorSpan(color), ofe,
						ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
			}
		}
	}

	public static boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) deleteDirectory(file);
		}
		boolean succ = directoryToBeDeleted.delete();
		return succ;
	}

	public static String getBookInfoToSend(Selection sel) {
		if (sel.chapter != null && sel.chapter.length() > 0) return "\"" + sel.chapter + "\"";
		return "";
	}

	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
	public static void setScrollBarsFadeOff(ListView lv) {
		lv.setScrollBarFadeDuration(0);
	}

	public static byte[] bitmapToPNG(Bitmap bitmap) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}

	public static boolean copyToClipboard(Context context, String text) {
		try {
			android.content.ClipboardManager clipboard =
					(android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip =
					android.content.ClipData.newPlainText("Path copied to clipboard", text);
			clipboard.setPrimaryClip(clip);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static java.util.Date getPreviousMonth(java.util.Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		if (calendar.get(Calendar.MONTH) == Calendar.JANUARY) {
			calendar.set(Calendar.MONTH, Calendar.DECEMBER);
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
		} else {
			calendar.roll(Calendar.MONTH, false);
		}
		return calendar.getTime();
	}

	public static java.util.Date getNextMonth(java.util.Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		if (calendar.get(Calendar.MONTH) == Calendar.DECEMBER) {
			calendar.set(Calendar.MONTH, Calendar.JANUARY);
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		} else {
			calendar.roll(Calendar.MONTH, true);
		}
		return calendar.getTime();
	}

	public static java.util.Date getLastDayOfMonth(java.util.Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int lastDateOfMonthForGivenDate = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		cal.set(Calendar.DATE, lastDateOfMonthForGivenDate);
		return cal.getTime();
	}

}
