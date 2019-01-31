package org.coolreader.crengine;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import uk.co.deanwild.flowtextview.FlowTextView;

public class BookInfoDialog extends BaseDialog {

	public final static int BOOK_INFO = 0;
	public final static int OPDS_INFO = 1;
	public final static int OPDS_FINAL_INFO = 2;

	private final BaseActivity mCoolReader;
	private final BookInfo mBookInfo;
	private int mActionType;
	private FileInfo mFileInfoOPDS;
	private FileBrowser mFileBrowser;
	private final LayoutInflater mInflater;
	private int mWindowSize;
	private Bitmap mBookCover;
	private String sAuthors = "";
	private String sBookTitle = "";
	private String sFileName = "";
	ImageButton btnBack;
	ImageButton btnOpenBook;
	ImageButton btnBookFolderOpen;
	ImageButton btnBookShortcut;
	ImageButton btnBookEdit;
	Button btnMarkToRead;
	ImageButton btnBookDownload;
	boolean bMarkToRead;

	private Map<String, Integer> mLabelMap;
	private void fillMap() {
		mLabelMap = new HashMap<String, Integer>();
		mLabelMap.put("section.system", R.string.book_info_section_system);
		mLabelMap.put("system.version", R.string.book_info_system_version);
		mLabelMap.put("system.battery", R.string.book_info_system_battery);
		mLabelMap.put("system.time", R.string.book_info_system_time);
		mLabelMap.put("system.resolution", R.string.book_info_system_resolution);
		mLabelMap.put("section.file", R.string.book_info_section_file_properties);
		mLabelMap.put("file.name", R.string.book_info_file_name);
		mLabelMap.put("file.path", R.string.book_info_file_path);
		mLabelMap.put("file.arcname", R.string.book_info_file_arcname);
		mLabelMap.put("file.arcpath", R.string.book_info_file_arcpath);
		mLabelMap.put("file.arcsize", R.string.book_info_file_arcsize);
		mLabelMap.put("file.size", R.string.book_info_file_size);
		mLabelMap.put("file.format", R.string.book_info_file_format);
		mLabelMap.put("section.position", R.string.book_info_section_current_position);
		mLabelMap.put("position.percent", R.string.book_info_position_percent);
		mLabelMap.put("position.page", R.string.book_info_position_page);
		mLabelMap.put("position.chapter", R.string.book_info_position_chapter);
		mLabelMap.put("section.book", R.string.book_info_section_book_properties);
		mLabelMap.put("book.authors", R.string.book_info_book_authors);
		mLabelMap.put("book.title", R.string.book_info_book_title);
		mLabelMap.put("book.date", R.string.book_info_book_date);
		mLabelMap.put("book.series", R.string.book_info_book_series_name);
		mLabelMap.put("book.language", R.string.book_info_book_language);
		mLabelMap.put("book.genre", R.string.book_info_book_genre);
		mLabelMap.put("book.srclang", R.string.book_info_book_srclang);
		mLabelMap.put("book.translator", R.string.book_info_book_translator);
		mLabelMap.put("book.symcount", R.string.book_info_book_symcount);
		mLabelMap.put("book.wordcount", R.string.book_info_book_wordcount);
		mLabelMap.put("section.book_document", R.string.book_info_section_book_document);
		mLabelMap.put("book.docauthor", R.string.book_info_book_docauthor);
		mLabelMap.put("book.docprogram", R.string.book_info_book_docprogram);
		mLabelMap.put("book.docdate", R.string.book_info_book_docdate);
		mLabelMap.put("book.docsrcurl", R.string.book_info_book_docsrcurl);
		mLabelMap.put("book.docsrcocr", R.string.book_info_book_docsrcocr);
		mLabelMap.put("book.docversion", R.string.book_info_book_docversion);
		mLabelMap.put("section.book_publisher", R.string.book_info_section_book_publisher);
		mLabelMap.put("book.publname", R.string.book_info_book_publname);
		mLabelMap.put("book.publisher", R.string.book_info_book_publisher);
		mLabelMap.put("book.publcity", R.string.book_info_book_publcity);
		mLabelMap.put("book.publyear", R.string.book_info_book_publyear);
		mLabelMap.put("book.publisbn", R.string.book_info_book_publisbn);
		mLabelMap.put("book.publseries", R.string.book_info_book_publseries_name);
		mLabelMap.put("book.translation", R.string.book_info_book_translation);
		mLabelMap.put("section.book_translation", R.string.book_info_section_book_translation);
		mLabelMap.put("system.device_model", R.string.book_info_system_device_model);
		mLabelMap.put("system.device_flags", R.string.book_info_system_device_flags);
	}
	
	private void addItem(TableLayout table, String item) {
		int p = item.indexOf("=");
		if ( p<0 )
			return;
		String name = item.substring(0, p).trim();
		String name1 = name;
		String value = item.substring(p+1).trim();
		if ( name.length()==0 || value.length()==0 )
			return;
		boolean isSection = false;
		if ( "section".equals(name) ) {
			name = "";
			Integer id = mLabelMap.get(value);
			if ( id==null )
				return;
			String section = getContext().getString(id);
			if ( section!=null )
				value = section;
			isSection = true;
		} else {
			Integer id = mLabelMap.get(name);
			String title = id!=null ? getContext().getString(id) : name;
			if ( title!=null )
				name = title;
		}
		TableRow tableRow = (TableRow)mInflater.inflate(isSection ? R.layout.book_info_section : R.layout.book_info_item, null);
		ImageView btnOptionAddInfo = null;
		if (isSection) btnOptionAddInfo = (ImageView)tableRow.findViewById(R.id.btn_book_add_info);
		if ((!item.equals("section=section.book"))||((StrUtils.isEmptyStr(sBookTitle))&&(StrUtils.isEmptyStr(sFileName)))) {
		//if (name.equals("section=section.book")) {
			if (btnOptionAddInfo!=null) btnOptionAddInfo.setVisibility(View.INVISIBLE);
		} else {
			if (btnOptionAddInfo!=null) {
				String sFind = sBookTitle;
				if (StrUtils.isEmptyStr(sBookTitle)) sFind = StrUtils.stripExtension(sFileName);
				if (!StrUtils.isEmptyStr(sAuthors)) sFind = sFind + ", " + sAuthors;
				btnOptionAddInfo.setImageDrawable(
						activity.getResources().getDrawable(Utils.resolveResourceIdByAttr(activity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				final View view1 = view;
				final String sFind1 = sFind;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
							emailIntent.putExtra(SearchManager.QUERY, sFind1.trim());
							mCoolReader.startActivity(emailIntent);
						}
					});
			}
		}
		TextView nameView = (TextView)tableRow.findViewById(R.id.name);
		TextView valueView = (TextView)tableRow.findViewById(R.id.value);
		nameView.setText(name);
		valueView.setText(value);
		valueView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String text = ((TextView) v).getText().toString();
				if ( text!=null && text.length()>0 ) {
					ClipboardManager cm = activity.getClipboardmanager();
					cm.setText(text);
					L.i("Setting clipboard text: " + text);
					activity.showToast(activity.getString(R.string.copied_to_clipboard)+": "+text,v);
				}
			}
		});

		table.addView(tableRow);
	}

	private void paintMarkButton() {
		int colorGrayC;
		int colorBlue;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeBlue});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorBlue = a.getColor(1, Color.BLUE);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		if (bMarkToRead) btnMarkToRead.setBackgroundColor(colorGrayCT2);
		else btnMarkToRead.setBackgroundColor(colorGrayCT);
		btnMarkToRead.setTextColor(colorBlue);
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			Bitmap resizedBitmap = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);

				final int maxSize = 200;
				int outWidth;
				int outHeight;
				int inWidth = mIcon11.getWidth();
				int inHeight = mIcon11.getHeight();
				if(inWidth > inHeight){
					outWidth = maxSize;
					outHeight = (inHeight * maxSize) / inWidth;
				} else {
					outHeight = maxSize;
					outWidth = (inWidth * maxSize) / inHeight;
				}
				resizedBitmap = Bitmap.createScaledBitmap(mIcon11, outWidth, outHeight, false);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return resizedBitmap;
		}

		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
		}
	}
	
	public BookInfoDialog(final BaseActivity activity, Collection<String> items, BookInfo bi, final String annot,
						  int actionType, FileInfo fiOPDS, FileBrowser fb)
	{
		super("BookInfoDialog", activity, null, false, false);
		mCoolReader = activity;
		mBookInfo = bi;
		mActionType = actionType;
		mFileInfoOPDS = fiOPDS;
		mFileBrowser = fb;
		FileInfo file = null;
		if (mBookInfo!=null)
			file = mBookInfo.getFileInfo();
		else file = fiOPDS;
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		//setTitle(mCoolReader.getString(R.string.dlg_book_info));
		fillMap();
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.book_info_dialog, null);
		final ImageView image = (ImageView)view.findViewById(R.id.book_cover);
		image.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CoolReader cr = (CoolReader)mCoolReader;
				if (mBookInfo!=null) {
					FileInfo fi = mBookInfo.getFileInfo();
					cr.loadDocument(fi);
					dismiss();
				} else cr.showToast(R.string.book_info_action_unavailable);
			}
		});
		btnBack = ((ImageButton)view.findViewById(R.id.base_dlg_btn_back));
		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onNegativeButtonClick();
			}
		});

		btnOpenBook = ((ImageButton)view.findViewById(R.id.btn_open_book));
		btnOpenBook.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CoolReader cr = (CoolReader)mCoolReader;
				if (mBookInfo!=null) {
					FileInfo fi = mBookInfo.getFileInfo();
					cr.loadDocument(fi);
					dismiss();
				} else cr.showToast(R.string.book_info_action_unavailable);
			}
		});

		btnBookFolderOpen = ((ImageButton)view.findViewById(R.id.book_folder_open));

			btnBookFolderOpen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CoolReader cr = (CoolReader)mCoolReader;
				cr.showDirectory(mBookInfo.getFileInfo());
				dismiss();
			}
		});

		btnBookShortcut = ((ImageButton)view.findViewById(R.id.book_create_shortcut));

		btnBookShortcut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			CoolReader cr = (CoolReader)mCoolReader;
			if (mBookInfo!=null) {
				FileInfo fi = mBookInfo.getFileInfo();
				cr.createBookShortcut(fi,mBookCover);
			} else cr.showToast(R.string.book_info_action_unavailable);
			}
		});

		btnBookEdit = ((ImageButton)view.findViewById(R.id.book_edit));
		btnBookEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			CoolReader cr = (CoolReader) mCoolReader;
			if (mBookInfo!=null) {
				FileInfo fi = mBookInfo.getFileInfo();
				FileInfo dfi = fi.parent;
				if (dfi == null) {
					dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
				}
				if (dfi != null) {
					cr.editBookInfo(dfi, fi);
					dismiss();
				}
			} else cr.showToast(R.string.book_info_action_unavailable);
			}
		});

		btnMarkToRead = ((Button)view.findViewById(R.id.btn_mark_toread));
		bMarkToRead = activity.settings().getBool(Settings.PROP_APP_MARK_DOWNLOADED_TO_READ, false);

		btnMarkToRead.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			bMarkToRead = !bMarkToRead;
			Properties props = new Properties(activity.settings());
			props.setProperty(Settings.PROP_APP_MARK_DOWNLOADED_TO_READ, bMarkToRead?"1":"0");
			activity.setSettings(props, -1, true);
			paintMarkButton();
			}
		});
		paintMarkButton();
		btnBookDownload = ((ImageButton)view.findViewById(R.id.book_download));
		final String annot2 = annot;
		btnBookDownload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mFileBrowser != null) mFileBrowser.showOPDSDir(mFileInfoOPDS, mFileInfoOPDS, annot2);
				dismiss();
			}
		});
		int w = mWindowSize * 4 / 10;
		int h = w * 4 / 3;
		image.setMinimumHeight(h);
		image.setMaxHeight(h);
		image.setMinimumWidth(w);
		image.setMaxWidth(w);
		Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		if (file != null) {
			if (mFileInfoOPDS!=null) {
				String sTitle = StrUtils.getNonEmptyStr(file.title, true);
				String sAuthors = StrUtils.getNonEmptyStr(file.authors, true).replace("\\|", "\n");
				if (StrUtils.isEmptyStr(sTitle))
					sTitle = StrUtils.stripExtension(file.filename);
				Bitmap bmp2 = Services.getCoverpageManager().getBookCoverWithTitleBitmap(sTitle, sAuthors,
					150, 200);
				image.setImageBitmap(bmp2);
				for (OPDSUtil.LinkInfo link : mFileInfoOPDS.links) {
					if (link.type.contains("image")) {
						new DownloadImageTask(image).execute(link.href);
						break;
					}
				}
			} else {
				Services.getCoverpageManager().drawCoverpageFor(mCoolReader.getDB(), file, bmp, new CoverpageManager.CoverpageBitmapReadyListener() {
					@Override
					public void onCoverpageReady(CoverpageManager.ImageItem file, Bitmap bitmap) {
						mBookCover = bitmap;
						BitmapDrawable drawable = new BitmapDrawable(bitmap);
						image.setImageDrawable(drawable);
					}
				});
			}
		}
		TableLayout table = (TableLayout)view.findViewById(R.id.table);
		FlowTextView txtAnnot = (FlowTextView) view.findViewById(R.id.lbl_annotation);
		txtAnnot.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String text = ((FlowTextView) v).getText().toString();
				if ( text!=null && text.length()>0 ) {
					ClipboardManager cm = activity.getClipboardmanager();
					cm.setText(text);
					L.i("Setting clipboard text: " + text);
					activity.showToast(activity.getString(R.string.copied_to_clipboard)+": "+text,v);
				}
			}
		});
		SpannableString ss = new SpannableString(annot);
		txtAnnot.setText(ss);
		int colorGray;
		int colorGrayC;
		int colorIcon;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.GRAY);
		txtAnnot.setTextColor(colorIcon);
		txtAnnot.setTextSize(txtAnnot.getTextsize()/4f*3f);
		int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		if (actionType == BOOK_INFO) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnBookDownload);
			parent.removeView(btnMarkToRead);
		}
		if (actionType == OPDS_INFO) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnOpenBook);
			parent.removeView(btnBookFolderOpen);
			parent.removeView(btnBookShortcut);
			parent.removeView(btnBookEdit);
		}
		if (actionType == OPDS_FINAL_INFO) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnBookDownload);
			parent.removeView(btnMarkToRead);
		}
		for ( String item : items ) {
			int p = item.indexOf("=");
			if ( p>=0 ) {
				String name = item.substring(0, p).trim();
				String value = item.substring(p + 1).trim();
				if (name.equals("file.name")) sFileName = value;
				if (name.equals("book.authors")) sAuthors = value.replace("|",", ");
				if (name.equals("book.title")) sBookTitle = value;
			}
		}
		for ( String item : items ) {
			addItem(table, item);
		}
		setView( view );
	}

}
