package org.coolreader.crengine;

import android.content.ContentResolver;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CRDBService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;

public class PictureCameDialog extends BaseDialog implements Settings {
	private final BaseActivity mActivity;
	private ViewGroup mView;
	private LinearLayout mRecentBooksScroll;
	private final LayoutInflater mInflater;
	private int mWindowSize;
	private int coverWidth;
	private int coverHeight;
	private BookInfo currentBook;
	private CoverpageManager mCoverpageManager;
	private PictureReceived picReceived = null;
	private boolean bThisIsTexture = false;
	private Button ibPicTexture;
	private Button ibPicBackground;


	public static final Logger log = L.create("cr");

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
		dismiss();
	}

	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
		dismiss();
	}

	@Override
	protected void onThirdButtonClick() {
		super.onThirdButtonClick();
		dismiss();
	}

	public static boolean isFileIsPicture(String fileName) {
		boolean bIsImage = false;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			Bitmap bitmap = BitmapFactory.decodeFile(fileName, options);
			if (options.outWidth != -1 && options.outHeight != -1) {
				bIsImage = true;
			}
		} catch (Exception e) {

		}
		return bIsImage;
	}

	public String getPicExtByMimetype(String smime) {
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().equals("image/gif")) return ".gif";
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().equals("image/jpeg")) return ".jpeg";
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().equals("image/pjpeg")) return ".jpeg";
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().equals("image/png")) return ".png";
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().startsWith("image/svg")) return ".svg";
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().equals("image/tiff")) return ".tiff";
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().equals("image/vnd.microsoft.icon")) return ".ico";
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().equals("image/vnd.wap.wbmp")) return ".wbmp";
		if (StrUtils.getNonEmptyStr(smime,true).toLowerCase().equals("image/webp")) return ".webp";
		return ".pic";
	}

	private String getAvailFName(String path, String fname) {
		String fname1 = fname;
		String fname1ext = "";
		if (fname1.contains(".")) {
			fname1ext = fname1.substring(fname1.lastIndexOf(".")+1);
			fname1 = fname1.substring(0,fname1.lastIndexOf("."));
		}
		int i=0;
		String fname2 = fname1;
		File f = new File(path+fname2+"."+fname1ext);
		while (f.exists()) {
			i++;
			fname2 = fname1 + "-" + String.valueOf(i);
			f = new File(path+fname2+"."+fname1ext);
		}
		return fname2+"."+fname1ext;
	}

	private void switchTexture(boolean isTexture) {
		bThisIsTexture = isTexture;
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGray = a.getColor(0, Color.GRAY);
		int colorGrayC = a.getColor(1, Color.GRAY);
		int colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		if (ibPicTexture!=null) {
			ibPicTexture.setBackgroundColor(colorGrayCT);
			this.mActivity.tintViewIcons(ibPicTexture, PorterDuff.Mode.CLEAR,true);
		}
		if (ibPicBackground!=null) {
			ibPicBackground.setBackgroundColor(colorGrayCT);
			this.mActivity.tintViewIcons(ibPicBackground, PorterDuff.Mode.CLEAR,true);
		}
		if ((ibPicTexture!=null)&&(bThisIsTexture)) {
			ibPicTexture.setBackgroundColor(colorGrayCT2);
			this.mActivity.tintViewIcons(ibPicTexture, true);
		}
		if ((ibPicBackground!=null)&&(!bThisIsTexture)) {
			ibPicBackground.setBackgroundColor(colorGrayCT2);
			this.mActivity.tintViewIcons(ibPicBackground, true);
		}
	}

	private void proceedTexture(final boolean needToActivate) {
		PictureReceived pic = ((CoolReader) this.mActivity).picReceived;
		if (pic.bmpReceived!=null) {
			String filename = "texture";
			if ((pic.uri!=null)||(!StrUtils.isEmptyStr(pic.fileName))) {
				String fname = "";
				if (!StrUtils.isEmptyStr(pic.fileName)) fname=pic.fileName;
				if ((pic.uri!=null) && (StrUtils.isEmptyStr(fname))) fname=pic.uri.getPath();
				while (fname.contains("/")) {
					fname=fname.split("\\/")[fname.split("\\/").length-1];
				}
				while (fname.contains(":")) {
					fname=fname.split("\\:")[fname.split("\\:").length-1];
				}
				while (fname.contains("\\")) {
					fname=fname.split("\\\\")[fname.split("\\\\").length-1];
				}
				filename=fname;
				if (!(
						(filename.toLowerCase().endsWith(".jpg"))||
								(filename.toLowerCase().endsWith(".jpeg"))||
								(filename.toLowerCase().endsWith(".png"))
						)) {
					if (StrUtils.getNonEmptyStr(pic.mimeType,true).contains("jpeg")) filename = filename + ".jpg";
					if (StrUtils.getNonEmptyStr(pic.mimeType,true).contains("png")) filename = filename + ".png";
				}

			}
			String sDir = "";
			if (bThisIsTexture) {
				ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.TexturesDirs, true);
				if (tDirs.size()>0) sDir=tDirs.get(0);
			} else {
				ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.BackgroundsDirs, true);
				if (tDirs.size()>0) sDir=tDirs.get(0);
			}
			if (!StrUtils.isEmptyStr(sDir))
				if ((!sDir.endsWith("/"))&&(!sDir.endsWith("\\"))) sDir = sDir + "/";
			filename = getAvailFName(sDir,filename);
			final String fname1 = filename;
			final String sDir1 = sDir;
			BackgroundThread.instance().executeGUI((Runnable) () -> {
				final InputDialog dlg = new InputDialog(mActivity, title,
						((CoolReader) this.mActivity).getString(R.string.pic_filename)+" "+
						sDir1.replace("/storage/","/s/").replace("/emulated/","/e/")+":"
						, fname1,
						new InputDialog.InputHandler() {
							@Override
							public boolean validateNoCancel(String s) {
								File f = new File(sDir1+s);
								if (f.exists()) {
									((CoolReader) PictureCameDialog.this.mActivity).showToast(R.string.pic_file_exists);
									return false;
								}
								return true;
							}
							@Override
							public boolean validate(String s) {
								return true;
							}
							@Override
							public void onOk(String s) {
								try {
									String s1 = s;
									if (!((s.toLowerCase().endsWith(".png")) ||
										(s.toLowerCase().endsWith(".jpg")) ||
										(s.toLowerCase().endsWith(".jpeg")))) s1 = s+".png";
									File file = new File(sDir1 + s1);
									OutputStream fOut = new FileOutputStream(file);
									if ((s.toLowerCase().endsWith(".jpg")) ||
											(s.toLowerCase().endsWith(".jpeg")))
										((CoolReader) PictureCameDialog.this.mActivity).picReceived.bmpReceived.
											compress(Bitmap.CompressFormat.JPEG,100,fOut);
									else ((CoolReader) PictureCameDialog.this.mActivity).picReceived.bmpReceived.
											compress(Bitmap.CompressFormat.PNG,100,fOut);
									fOut.flush();
									fOut.close();
									if (needToActivate) {
File file1 = new File(sDir1 + s1);
if (file1.exists()) {
BackgroundTextureInfo item = BackgroundTextureInfo.fromFile(file1
.getAbsolutePath());
											Properties props = new Properties(mActivity.settings());
											props.setProperty(PROP_PAGE_BACKGROUND_IMAGE,item.id);
mActivity.setSettings(props, -1, true);
}
}
									dismiss();
								} catch (Exception e) {
									((CoolReader) PictureCameDialog.this.mActivity).showToast( ((CoolReader) PictureCameDialog.this.mActivity).getString(R.string.pic_problem)+" "+
											e.getMessage());
								}
							}
							@Override
							public void onCancel() {
							}
						});
				dlg.show();
			});
		} else {
			((CoolReader) this.mActivity).showToast(R.string.pic_problem);
		}
	}

	private String queryName(ContentResolver resolver, Uri uri) {
		Cursor returnCursor =
				resolver.query(uri, null, null, null, null);
		assert returnCursor != null;
		int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
		returnCursor.moveToFirst();
		String name = returnCursor.getString(nameIndex);
		returnCursor.close();
		return name;
	}

	public PictureCameDialog(final BaseActivity activity, Object obj, String objMime, String suggestedName)
	{
		super("PictureCameDialog", activity, "", true, false);
		if (obj instanceof BookInfo) {
			picReceived = ((CoolReader) activity).picReceived;
			picReceived.book = (BookInfo)obj;
		} else
			picReceived = null;
		if (picReceived == null) picReceived = new PictureReceived();
		if (!StrUtils.isEmptyStr(objMime)) picReceived.mimeType = StrUtils.getNonEmptyStr(objMime,true);
		if (obj!=null) {
			if (obj instanceof Uri) {
				picReceived.uri = (Uri) obj;
				picReceived.fileName = StrUtils.getNonEmptyStr(queryName(activity.getApplicationContext().getContentResolver(),(Uri)obj),true);
				if (StrUtils.isEmptyStr(picReceived.fileName)) picReceived.fileName = "texture"+getPicExtByMimetype(picReceived.mimeType);
				try {
					picReceived.bmpReceived = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), (Uri)obj);
				} catch (Exception e) {
					picReceived.bmpReceived = null;
					log.e("failed to get image from mediastore", e);
				}
			}
			if (obj instanceof String) {
				try {
					picReceived.fileName = (String) obj;
					picReceived.bmpReceived = BitmapFactory.decodeFile((String)obj);
				} catch (Exception e) {
					picReceived.bmpReceived = null;
					log.e("failed to get image from file", e);
				}
			}
			if (obj instanceof InputStream) {
				try {
					// TODO: filename from web URL
					picReceived.fileName = suggestedName+getPicExtByMimetype(picReceived.mimeType);
					picReceived.bmpReceived = BitmapFactory.decodeStream((InputStream) obj);
				} catch (Exception e) {
					picReceived.bmpReceived = null;
					log.e("failed to get image from file", e);
				}
			}
		}
		mActivity = activity;
		((CoolReader)mActivity).picReceived = picReceived;
		setNegativeButtonImage(0,0);
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		setTitle(mActivity.getString(R.string.dlg_picture_came));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.picture_came_dialog, null);
		LinearLayout llMain = (LinearLayout)view.findViewById(R.id.ll_main);
		this.mCoverpageManager = Services.getCoverpageManager();
		this.mCoverpageManager.setmCoolReader((CoolReader) mActivity);
		int screenHeight = mActivity.getWindowManager().getDefaultDisplay().getHeight();
		int screenWidth = mActivity.getWindowManager().getDefaultDisplay().getWidth();
		int h = screenHeight / 4;
		int w = screenWidth / 4;
		if (h > w)
			h = w;
		w = h * 3 / 4;
		coverWidth = w;
		coverHeight = h;
		final ImageView imageCame = (ImageView)view.findViewById(R.id.image_came);
		if (((CoolReader)activity).picReceived!=null)
			if (((CoolReader)activity).picReceived.bmpReceived!=null)
				imageCame.setImageBitmap(((CoolReader)activity).picReceived.bmpReceived);

		ibPicTexture=(Button)view.findViewById(R.id.ib_texture);
		ibPicTexture.setOnClickListener(v -> switchTexture(true));
		Drawable imgB = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = imgB.getConstantState().newDrawable().mutate();
		if (ibPicTexture!=null) ibPicTexture.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		activity.tintViewIcons(ibPicTexture, PorterDuff.Mode.CLEAR,true);
		ibPicBackground= view.findViewById(R.id.ib_background);
		ibPicBackground.setOnClickListener(v -> switchTexture(false));
		Drawable img2 = imgB.getConstantState().newDrawable().mutate();
		if (ibPicBackground!=null) ibPicBackground.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		activity.tintViewIcons(ibPicBackground, PorterDuff.Mode.CLEAR,true);
		switchTexture(true);
		BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> switchTexture(true), 200));
		final Button ibPicCopy= view.findViewById(R.id.ib_copy);
		ibPicCopy.setOnClickListener(v -> proceedTexture(false));
		final Button ibPicCopyAct= view.findViewById(R.id.ib_copy_activate);
		ibPicCopyAct.setOnClickListener(v -> proceedTexture(true));
		final Button ibPicRememberForLater= view.findViewById(R.id.ib_save_for_later);
		int resId = Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_texture, R.drawable.icons8_texture);
		Drawable img = getContext().getResources().getDrawable( R.drawable.icons8_texture);
		mActivity.tintViewIcons(img,true);
		ibPicRememberForLater.setOnClickListener(v -> {
			activity.showToast(R.string.pic_image_was_remembered);
			dismiss();
		});
		int colorGray;
		int colorGrayC;
		int colorIcon;
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		ibPicCopy.setBackgroundColor(colorGrayCT);
		ibPicCopy.setTextColor(activity.getTextColor(colorIcon));
		Utils.setDashedButton(ibPicCopy);
		ibPicCopyAct.setBackgroundColor(colorGrayCT);
		ibPicCopyAct.setTextColor(activity.getTextColor(colorIcon));
		Utils.setDashedButton(ibPicCopyAct);
		ibPicTexture.setTextColor(activity.getTextColor(colorIcon));
		ibPicBackground.setTextColor(activity.getTextColor(colorIcon));
		ibPicRememberForLater.setBackgroundColor(colorGrayCT);
		ibPicRememberForLater.setTextColor(activity.getTextColor(colorIcon));
		Utils.setDashedButton(ibPicRememberForLater);
		imageCame.setMinimumHeight(h);
		imageCame.setMaxHeight(h);
		imageCame.setMinimumWidth(w);
		imageCame.setMaxWidth(w);
		createViews(llMain);
		setView( view );
	}

	private void updateDelimiterTheme(int viewId) {
		View view = mView.findViewById(viewId);
		InterfaceTheme theme = mActivity.getCurrentTheme();
		view.setBackgroundResource(theme.getRootDelimiterResourceId());
		view.setMinimumHeight(theme.getRootDelimiterHeight());
		view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, theme.getRootDelimiterHeight()));
	}

	private void setBookInfoItem(ViewGroup baseView, int viewId, String value) {
		TextView view = (TextView)baseView.findViewById(viewId);
		if (view != null) {
			if (value != null && value.length() > 0) {
				view.setText(value);
			} else {
				view.setText("");
			}
		}
	}

	private void setBookPicture(FileInfo item) {
		final String sBookFName = item.getFilename();
		CRC32 crc = new CRC32();
		crc.update(sBookFName.getBytes());
		final String sFName = String.valueOf(crc.getValue()) + "_cover.png";
		String sDir = "";
		ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CustomCoversDirs, true);
		if (tDirs.size()>0) sDir=tDirs.get(0);
		if (!StrUtils.isEmptyStr(sDir))
			if ((!sDir.endsWith("/"))&&(!sDir.endsWith("\\"))) sDir = sDir + "/";
		if (!StrUtils.isEmptyStr(sDir)) {
			try {
				File f = new File(sDir + sFName);
				if (f.exists()) f.delete();
				File file = new File(sDir + sFName);
				OutputStream fOut = new FileOutputStream(file);
				((CoolReader) this.mActivity).picReceived.bmpReceived.compress(Bitmap.CompressFormat.PNG, 100, fOut);
				fOut.flush();
				fOut.close();
			} catch (Exception e) {
			((CoolReader) this.mActivity).showToast( ((CoolReader) this.mActivity).getString(R.string.pic_problem)+" "+
					e.getMessage());
			}
		}
	}

	public void deleteBookPicture(FileInfo item) {
		final String sBookFName = item.getFilename();
		CRC32 crc = new CRC32();
		crc.update(sBookFName.getBytes());
		final String sFName = String.valueOf(crc.getValue()) + "_cover.png";
		String sDir = "";
		ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.CustomCoversDirs, true);
		if (tDirs.size() > 0) sDir = tDirs.get(0);
		if (!StrUtils.isEmptyStr(sDir))
			if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
		if (!StrUtils.isEmptyStr(sDir)) {
			try {
				final File f = new File(sDir + sFName);
				if (f.exists())
					this.mActivity.askConfirmation(R.string.pic_delete_cover, (Runnable) () -> f.delete());
				else this.mActivity.showToast( ((CoolReader) this.mActivity).getString(R.string.pic_no_custom_cover));
			} catch (Exception e) {
				this.mActivity.showToast( ((CoolReader) this.mActivity).getString(R.string.pic_problem)+" "+
					e.getMessage());
			}
		}
	}

	private void updateCurrentBook(BookInfo book) {
		currentBook = book;

		// set current book cover page
		ImageView cover = mView.findViewById(R.id.book_cover);
		if (currentBook != null) {
			final FileInfo item = currentBook.getFileInfo();
			cover.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item, coverWidth, coverHeight));
			cover.setMinimumHeight(coverHeight);
			cover.setMinimumWidth(coverWidth);
			cover.setMaxHeight(coverHeight);
			cover.setMaxWidth(coverWidth);
			cover.setTag(new CoverpageManager.ImageItem(item, coverWidth, coverHeight));
			cover.setOnClickListener(v -> {
				setBookPicture(item);
				dismiss();
			});
			cover.setOnLongClickListener(v -> {
				deleteBookPicture(item);
				return true;
			});
			setBookInfoItem(mView, R.id.lbl_book_author, Utils.formatAuthors(item.getAuthors()));
			setBookInfoItem(mView, R.id.lbl_book_title, currentBook.getFileInfo().title);
			setBookInfoItem(mView, R.id.lbl_book_series, Utils.formatSeries(item.series, item.seriesNumber));
			String state = Utils.formatReadingState(mActivity, item);
			int colorBlue;
			int colorGreen;
			int colorGray;
			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorThemeBlue,
							R.attr.colorThemeGreen,
							R.attr.colorThemeGray,
							R.attr.colorIcon});
			colorBlue = a.getColor(0, Color.BLUE);
			colorGreen = a.getColor(1, Color.GREEN);
			colorGray = a.getColor(2, Color.GRAY);
			colorIcon = a.getColor(3, Color.GRAY);
			a.recycle();
			TextView tvInfo = (TextView)mView.findViewById(R.id.lbl_book_info1);
			tvInfo.setTag("notint");
			int n = item.getReadingState();
			if (n == FileInfo.STATE_READING)
				tvInfo.setTextColor(colorGreen);
			else if (n == FileInfo.STATE_TO_READ)
				tvInfo.setTextColor(colorBlue);
			else if (n == FileInfo.STATE_FINISHED)
				tvInfo.setTextColor(colorGray);
			setBookInfoItem(mView, R.id.lbl_book_info1, state);
			state =  " " + Utils.formatFileInfo(mActivity, item) + " ";
			if (Services.getHistory() != null)
				state = state + " " + Utils.formatLastPosition(mActivity, Services.getHistory().getLastPos(item));
			setBookInfoItem(mView, R.id.lbl_book_info, state);
		} else {
			log.w("No current book in history");
			cover.setImageDrawable(null);
			cover.setMinimumHeight(0);
			cover.setMinimumWidth(0);
			cover.setMaxHeight(0);
			cover.setMaxWidth(0);

			setBookInfoItem(mView, R.id.lbl_book_author, "");
			setBookInfoItem(mView, R.id.lbl_book_title, "No last book"); // TODO: i18n
			setBookInfoItem(mView, R.id.lbl_book_series, "");
		}
	}

	private final static int MAX_RECENT_BOOKS = 12;

	private void updateRecentBooks(ArrayList<BookInfo> books) {
		int colorBlue;
		int colorGreen;
		int colorGray;
		int colorIcon;
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeBlue,
						R.attr.colorThemeGreen,
						R.attr.colorThemeGray,
						R.attr.colorIcon});
		colorBlue = a.getColor(0, Color.BLUE);
		colorGreen = a.getColor(1, Color.GREEN);
		colorGray = a.getColor(2, Color.GRAY);
		colorIcon = a.getColor(3, Color.GRAY);
		a.recycle();

		ArrayList<FileInfo> files = new ArrayList<FileInfo>();
		int iBeg = 1;
		if (picReceived.book != null) iBeg = 0;
		for (int i = iBeg; i <= MAX_RECENT_BOOKS && i < books.size(); i++)
			files.add(books.get(i).getFileInfo());
		if (books.size() > MAX_RECENT_BOOKS && Services.getScanner() != null)
			files.add(Services.getScanner().createRecentRoot());
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		mRecentBooksScroll.removeAllViews();
		for (final FileInfo item : files) {
			final View view = inflater.inflate(R.layout.root_item_recent_book, null);
			ImageView cover = (ImageView)view.findViewById(R.id.book_cover);
			TextView label = (TextView)view.findViewById(R.id.book_name);
			cover.setMinimumHeight(coverHeight);
			cover.setMaxHeight(coverHeight);
			cover.setMaxWidth(coverWidth);
			if (item.isRecentDir()) {
				cover.setImageResource(
						Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_button_next_drawable, R.drawable.cr3_button_next)
						//R.drawable.cr3_button_next
				);
				if (label != null) {
					label.setText("More...");
					label.setTextColor(this.mActivity.getTextColor(colorIcon));
				}
				view.setOnClickListener(v -> ((CoolReader)mActivity).showRecentBooks());
			} else {
				cover.setMinimumWidth(coverWidth);
				cover.setTag(new CoverpageManager.ImageItem(item, coverWidth, coverHeight));
				cover.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item, coverWidth, coverHeight));
				if (label != null) {
					String title = item.title;
					String authors = Utils.formatAuthors(item.getAuthors());
					String s = item.getFileNameToDisplay();
					if (!Utils.empty(title) && !Utils.empty(authors))
						s = title + " - " + authors;
					else if (!Utils.empty(title))
						s = title;
					else if (!Utils.empty(authors))
						s = authors;
					label.setText(s != null ? s : "");
					label.setTextColor(this.mActivity.getTextColor(colorIcon));
					int n = item.getReadingState();
					if (n == FileInfo.STATE_READING)
						label.setTextColor(colorGreen);
					else if (n == FileInfo.STATE_TO_READ)
						label.setTextColor(colorBlue);
					else if (n == FileInfo.STATE_FINISHED)
						label.setTextColor(colorGray);
					label.setMaxWidth(coverWidth);
				}
				view.setOnClickListener(v -> {
					setBookPicture(item);
					dismiss();
				});
				view.setOnLongClickListener(v -> {
					deleteBookPicture(item);
					return true;
				});
			}
			mRecentBooksScroll.addView(view);
		}
		mRecentBooksScroll.invalidate();
	}

	public void refreshRecentBooks() {
		this.mActivity.waitForCRDBService(() -> BackgroundThread.instance().postGUI(() -> {
			if (Services.getHistory() != null && mActivity.getDB() != null) {
				Services.getHistory().getOrLoadRecentBooks(mActivity.getDB(), new CRDBService.RecentBooksLoadingCallback() {

					@Override
					public void onRecentBooksListLoadBegin() {

					}

					@Override
					public void onRecentBooksListLoaded(ArrayList<BookInfo> bookList) {
						if (PictureCameDialog.this.picReceived.book == null)
							updateCurrentBook(bookList != null && bookList.size() > 0 ? bookList.get(0) : null);
						updateRecentBooks(bookList);
					}
				});
			}
		}));
	}

	private void createViews(ViewGroup view1) {
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		View view = inflater.inflate(R.layout.cur_book_and_recent, null);
		mView = (ViewGroup)view;

		updateDelimiterTheme(R.id.delimiter1);
		updateDelimiterTheme(R.id.delimiter2);

		mRecentBooksScroll = (LinearLayout)mView.findViewById(R.id.scroll_recent_books);

		if (picReceived.book!=null)
			updateCurrentBook(picReceived.book);
		else
			updateCurrentBook(Services.getHistory().getLastBook());

		mView.findViewById(R.id.current_book).setOnClickListener(v -> {
			if (currentBook != null) {
			//	mActivity.loadDocument(currentBook.getFileInfo());
			}

		});

		mView.findViewById(R.id.current_book).setOnLongClickListener(v -> {
			//if (currentBook != null)
			//	mActivity.editBookInfo(Services.getScanner().createRecentRoot(), currentBook.getFileInfo());
			return true;
		});

		refreshRecentBooks();
		view1.addView(mView);
	}

}
