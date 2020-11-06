package org.coolreader.crengine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.litres.LitresConfig;
import org.coolreader.cloud.litres.LitresSearchParams;
import org.coolreader.dic.TranslationDirectionDialog;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import uk.co.deanwild.flowtextview.FlowTextView;

import static org.coolreader.crengine.FileInfo.OPDS_DIR_PREFIX;

public class BookInfoDialog extends BaseDialog {

	public static ProgressDialog progressDlg;

	public final static int BOOK_INFO = 0;
	public final static int OPDS_INFO = 1;
	public final static int OPDS_FINAL_INFO = 2;

	private final CoolReader mCoolReader;
	private final BookInfo mBookInfo;
	private int mActionType;
	private FileInfo mFileInfoCloud;
	boolean isLitres = false;
	boolean isPerson = false;
	private FileBrowser mFileBrowser;
	private FileInfo mCurrDir;
	private final LayoutInflater mInflater;
	private FileInfo mFileInfoSearchDir = null;
	private String mAuthors = "";
	private int mWindowSize;
	private Bitmap mBookCover;
	private String sAuthors = "";
	private String sBookTitle = "";
	private String sFileName = "";
	private boolean mPurchased = false;
	ImageButton btnBack;
	ImageButton btnOpenBook;
	ImageButton btnBookFolderOpen;
	ImageButton btnBookShortcut;
	ImageButton btnBookEdit;
	Button btnMarkToRead;
	TableLayout tlLitresDownl;
	LinearLayout llLitresPurchase;
	Button btnFragment;
	Button btnDownloadLitresBook;
	Button btnDownloadFB2;
	Button btnDownloadFB3;
	public Button btnPurchase;
	ImageButton btnBookDownload;
	String annot2 = "";
	ImageButton btnFindAuthors;
	boolean bMarkToRead;
	TextView tvWC;
	TextView tvSC;
	TextView tvML;
	Button btnCalc;

	private static int DM_FRAGMENT = 1;
	private static int DM_FULL = 2;

	int curDownloadMode = DM_FRAGMENT;

	public BookInfo getmBookInfo() {
		return mBookInfo;
	}

	private void setLitresDownloadModeChecked(Button btn) {
		if (btn != null) {
			if (btn == btnFragment) {
				curDownloadMode = DM_FRAGMENT;
			}
			if (btn == btnDownloadLitresBook) {
				curDownloadMode = DM_FULL;
			}
		}
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast});
		colorGrayC = a.getColor(0, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		mCoolReader.tintViewIcons(btnFragment, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(btnDownloadLitresBook, PorterDuff.Mode.CLEAR,true);
		btnFragment.setBackgroundColor(colorGrayCT);
		btnDownloadLitresBook.setBackgroundColor(colorGrayCT);
		if (curDownloadMode == DM_FRAGMENT) {
			btnFragment.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnFragment,true);
		}
		if (curDownloadMode == DM_FULL) {
			btnDownloadLitresBook.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnDownloadLitresBook,true);
		}
	}

	public void LitresSetPurchased(String errorMsg, boolean excep) {
		if (btnPurchase != null) {
			btnPurchase.setEnabled(true);
			btnPurchase.setVisibility(View.VISIBLE);
			if (excep) return; // exception will be shown in toast
			if (StrUtils.isEmptyStr(errorMsg)) {
				btnPurchase.setText(mCoolReader.getString(R.string.online_store_purchase_success));
				setLitresDownloadModeChecked(btnDownloadLitresBook);
				Utils.hideView(btnFragment);
				mPurchased = true;
			} else {
				String s = mCoolReader.getString(R.string.error) + ": " + errorMsg;
				s = StrUtils.getNonEmptyStr(s, true).replace("Error: Error", "Error:");
				s = s.replace("::", ":");
				if (s.contains("already exists")) {
					s = mCoolReader.getString(R.string.already_purchased);
					setLitresDownloadModeChecked(btnDownloadLitresBook);
					Utils.hideView(btnFragment);
					mPurchased = true;
				}
				btnPurchase.setText(s);
			}
		}

	}

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
		mLabelMap.put("file.opds_link", R.string.book_info_opds_link);
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
		mLabelMap.put("book.minleft", R.string.book_info_stats_minutes_left);
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

	protected void updateGlobalMargin(ViewGroup v,
									  boolean left, boolean top, boolean right, boolean bottom) {
		if (v == null) return;
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		int globalMargins = activity.settings().getInt(Settings.PROP_GLOBAL_MARGIN, 0);
		if (globalMargins > 0)
			if (lp instanceof ViewGroup.MarginLayoutParams) {
				if (top) ((ViewGroup.MarginLayoutParams) lp).topMargin = globalMargins;
				if (bottom) ((ViewGroup.MarginLayoutParams) lp).bottomMargin = globalMargins;
				if (left) ((ViewGroup.MarginLayoutParams) lp).leftMargin = globalMargins;
				if (right) ((ViewGroup.MarginLayoutParams) lp).rightMargin = globalMargins;
			}
	}
	
	private void addItem(TableLayout table, BookInfoEntry item) {
		String name = item.infoTitle;
		String name1 = name;
		String value = item.infoValue;
		String typ = item.infoType;
		if (name.length() == 0 || value.length() == 0)
			return;
		boolean isSection = false;
		if ("section".equals(name)) {
			name = "";
			Integer id = mLabelMap.get(value);
			if (id == null)
				return;
			String section = getContext().getString(id);
			if (section != null)
				value = section;
			isSection = true;
		} else {
			Integer id = mLabelMap.get(name);
			String title = id!=null ? getContext().getString(id) : name;
			if (title != null)
				name = title;
		}
		TableRow tableRow = (TableRow)mInflater.inflate(isSection ? R.layout.book_info_section : R.layout.book_info_item, null);
		ImageView btnOptionAddInfo = null;
		if (isSection) btnOptionAddInfo = (ImageView)tableRow.findViewById(R.id.btn_book_add_info);
		if ((!item.infoValue.equals("section.book"))||((StrUtils.isEmptyStr(sBookTitle))&&(StrUtils.isEmptyStr(sFileName)))) {
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
					btnOptionAddInfo.setOnClickListener(v -> {
						final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
						emailIntent.putExtra(SearchManager.QUERY, sFind1.trim());
						mCoolReader.startActivity(emailIntent);
					});
			}
		}
		TextView nameView = (TextView)tableRow.findViewById(R.id.name);
		TextView valueView = (TextView)tableRow.findViewById(R.id.value);
		nameView.setText(name);
		valueView.setText(value);
		valueView.setTag(name);
		if (name.equals(activity.getString(R.string.book_info_book_symcount))) tvSC = valueView;
		if (name.equals(activity.getString(R.string.book_info_book_wordcount))) tvWC = valueView;
		if (name.equals(activity.getString(R.string.book_info_stats_minutes_left))) tvML = valueView;
		ReaderView rv = ((CoolReader) mCoolReader).getReaderView();
		if (
			(name.equals(activity.getString(R.string.book_info_book_symcount))) &&
		   (value.equals("0")) && (rv != null)
		)
			if ((mBookInfo != null) && (rv.mBookInfo != null))
				if (rv.mBookInfo.getFileInfo().getFilename().equals(mBookInfo.getFileInfo().getFilename())) {
					int colorGrayC;
					int colorIcon;
					TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
							{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon, R.attr.colorIconL});
					colorGrayC = a.getColor(0, Color.GRAY);
					colorIcon = a.getColor(2, Color.BLACK);
					a.recycle();
					Button countButton = new Button(mCoolReader);
					btnCalc = countButton;
					countButton.setText(activity.getString(R.string.calc_stats));
					countButton.setTextColor(colorIcon);
					countButton.setBackgroundColor(colorGrayC);
					LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					llp.setMargins(20, 0, 8, 0);
					countButton.setLayoutParams(llp);
					countButton.setMaxLines(3);
					countButton.setEllipsize(TextUtils.TruncateAt.END);
					final ViewGroup vg = (ViewGroup) valueView.getParent();
					vg.addView(countButton);
					countButton.setOnClickListener((View.OnClickListener) v -> {
						int iPageCnt = 0;
						int iSymCnt = 0;
						int iWordCnt = 0;
						ReaderView rv1 = ((CoolReader) mCoolReader).getReaderView();
						if ((rv1 != null)&&(mBookInfo!=null)) {
							if (rv1.getArrAllPages() != null)
								iPageCnt = rv1.getArrAllPages().size();
							else {
								rv1.CheckAllPagesLoadVisual();
								iPageCnt = rv1.getArrAllPages().size();
							}
							for (int i = 0; i < iPageCnt; i++) {
								String sPage = rv1.getArrAllPages().get(i);
								if (sPage == null) sPage = "";
								sPage = sPage.replace("\\n", " ");
								sPage = sPage.replace("\\r", " ");
								iSymCnt = iSymCnt + sPage.replaceAll("\\s+", " ").length();
								iWordCnt = iWordCnt + sPage.replaceAll("\\p{Punct}", " ").
										replaceAll("\\s+", " ").split("\\s").length;
							}
							mBookInfo.getFileInfo().symCount = iSymCnt;
							mBookInfo.getFileInfo().wordCount = iWordCnt;
							BookInfo bi = new BookInfo(mBookInfo.getFileInfo());
							bi.getFileInfo().symCount = iSymCnt;
							bi.getFileInfo().wordCount = iWordCnt;
							((CoolReader) mCoolReader).getDB().saveBookInfo(bi);
							((CoolReader) mCoolReader).getDB().flush();
							vg.removeView(countButton);
							if (tvSC != null) tvSC.setText(""+iSymCnt);
							if (tvWC != null) tvWC.setText(""+iWordCnt);
							ReadingStatRes sres = ((CoolReader) mCoolReader).getReaderView().getBookInfo().getFileInfo().calcStats();
							double speedKoef = sres.val;
							int pagesLeft;
							double msecLeft;
							double msecFivePages;
							PositionProperties currpos = ((CoolReader) mCoolReader).getReaderView().getDoc().getPositionProps(null, true);
							if ((bi.getFileInfo().symCount>0) && (speedKoef > 0.000001)) {
								pagesLeft = ((CoolReader) mCoolReader).getReaderView().getDoc().getPageCount() - currpos.pageNumber;
								double msecAllPages;
								msecAllPages = speedKoef * (double) bi.getFileInfo().symCount;
								msecFivePages = msecAllPages / ((double) ((CoolReader) mCoolReader).getReaderView().getDoc().getPageCount()) * 5.0;
								msecLeft = (((double) pagesLeft) / 5.0) * msecFivePages;
								String sLeft = " ";
								int minutes = (int) ((msecLeft / 1000) / 60);
								int hours = (int) ((msecLeft / 1000) / 60 / 60);
								if (hours>0) {
									minutes = minutes - (hours * 60);
									sLeft = sLeft + hours + "h "+minutes + "min (calc count: "+ sres.cnt +")";
								} else {
									sLeft = sLeft + minutes + "min (calc count: "+ sres.cnt +")";
								}
								if (tvML != null) tvML.setText(sLeft);
							}
						}
					});
		}
		if (
			(name.equals(activity.getString(R.string.book_info_book_translation)))
		) {
			int colorGrayC;
			int colorIcon;
			TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon, R.attr.colorIconL});
			colorGrayC = a.getColor(0, Color.GRAY);
			colorIcon = a.getColor(2, Color.BLACK);
			a.recycle();
			Button translButton = new Button(mCoolReader);
			translButton.setText(activity.getString(R.string.specify_translation_dir));
			translButton.setTextColor(colorIcon);
			translButton.setBackgroundColor(colorGrayC);
			LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			llp.setMargins(20, 0, 8, 0);
			translButton.setLayoutParams(llp);
			translButton.setMaxLines(3);
			translButton.setEllipsize(TextUtils.TruncateAt.END);
			final CoolReader cr = (CoolReader) mCoolReader;
			translButton.setOnClickListener(v -> {
				String lang = StrUtils.getNonEmptyStr(mBookInfo.getFileInfo().lang_to,true);
				String langf = StrUtils.getNonEmptyStr(mBookInfo.getFileInfo().lang_from, true);
				FileInfo fi = mBookInfo.getFileInfo();
				FileInfo dfi = fi.parent;
				if (dfi == null) {
					dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
				}
				if (dfi != null) {
					cr.editBookTransl(dfi, fi, langf, lang, "", null, TranslationDirectionDialog.FOR_COMMON);
				}
				dismiss();
			});
			final ViewGroup vg = (ViewGroup) valueView.getParent();
			vg.addView(translButton);
		}
		if (typ.startsWith("link")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			if (typ.contains("application/atom+xml")) {
				valueView.setOnClickListener((View.OnClickListener) v -> {
					String text = ((TextView) v).getText().toString();
					if (text != null && text.length() > 0)
						if (mFileBrowser != null) {
							FileInfo item1 = new FileInfo();
							item1.pathname = OPDS_DIR_PREFIX + text;
							item1.parent = mCurrDir;
							if (((TextView) v).getTag()!=null) {
								item1.title = ((TextView) v).getTag().toString();
								item1.setFilename(item1.title);
							}
							BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(
									() -> mFileBrowser.showOPDSDir(item1, null, ""), 200));
							dismiss();
						}
					});
			} else {
				Linkify.addLinks(valueView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
				valueView.setLinksClickable(true);
				int colorGrayC;
				int colorIcon;
				TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
						{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon, R.attr.colorIconL});
				colorGrayC = a.getColor(0, Color.GRAY);
				colorIcon = a.getColor(2, Color.BLACK);
				a.recycle();
				valueView.setLinkTextColor(colorIcon);
				valueView.setTextColor(colorIcon);
			}
		} else
		if (typ.startsWith("series_authors")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			valueView.setOnClickListener(v -> {
				String text = typ.replace("series_authors:", "").trim();
				if (!StrUtils.isEmptyStr(text)) {
					FileInfo dir = new FileInfo();
					dir.isDirectory = true;
					dir.pathname = FileInfo.AUTHORS_TAG;
					dir.setFilename(mCoolReader.getString(R.string.folder_name_books_by_author));
					dir.isListed = true;
					dir.isScanned = true;
					((CoolReader)mCoolReader).showDirectory(dir, "series:"+text);
					dismiss();
				}
			});
		} else
		if (typ.startsWith("series_books")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			valueView.setOnClickListener(v -> {
				String text = typ.replace("series_books:", "").trim();
				if (!StrUtils.isEmptyStr(text)) {
					FileInfo dir = new FileInfo();
					dir.isDirectory = true;
					dir.pathname = FileInfo.SERIES_PREFIX;
					dir.setFilename(mCoolReader.getString(R.string.folder_name_books_by_series));
					dir.isListed = true;
					dir.isScanned = true;
					dir.id = 0L;
					((CoolReader)mCoolReader).showDirectory(dir, "series:"+text);
					dismiss();
				}
			});
		} else
		if (typ.startsWith("author_series")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			valueView.setOnClickListener(v -> {
				String text = typ.replace("author_series:", "").trim();
				if (!StrUtils.isEmptyStr(text)) {
					FileInfo dir = new FileInfo();
					dir.isDirectory = true;
					dir.pathname = FileInfo.SERIES_TAG;
					dir.setFilename(mCoolReader.getString(R.string.folder_name_books_by_series));
					dir.isListed = true;
					dir.isScanned = true;
					((CoolReader)mCoolReader).showDirectory(dir, "author:"+text);
					dismiss();
				}
			});
		} else
		if (typ.startsWith("author_books")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			valueView.setOnClickListener(v -> {
				String text = typ.replace("author_books:", "").trim();
				if (!StrUtils.isEmptyStr(text)) {
					FileInfo dir = new FileInfo();
					dir.isDirectory = true;
					dir.pathname = FileInfo.AUTHOR_PREFIX;
					dir.setFilename(mCoolReader.getString(R.string.folder_name_books_by_author));
					dir.isListed = true;
					dir.isScanned = true;
					dir.id = 0L;
					((CoolReader)mCoolReader).showDirectory(dir, "author:"+text);
					dismiss();
				}
			});
		} else
		if (typ.equals("text")) {
			valueView.setOnClickListener(v -> {
				String text = ((TextView) v).getText().toString();
				if (text != null && text.length() > 0) {
					ClipboardManager cm = activity.getClipboardmanager();
					cm.setText(text);
					L.i("Setting clipboard text: " + text);
					activity.showToast(activity.getString(R.string.copied_to_clipboard) + ": " + text, v);
				}
			});
		}
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
		mCoolReader.tintViewIcons(btnMarkToRead, PorterDuff.Mode.CLEAR,true);
		if (bMarkToRead) {
			btnMarkToRead.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnMarkToRead, true);
			btnMarkToRead.setPaintFlags(btnMarkToRead.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		} else {
			btnMarkToRead.setBackgroundColor(colorGrayCT);
			btnMarkToRead.setPaintFlags( btnMarkToRead.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
		}
		/*if (bMarkToRead) setDashedButton(btnMarkToRead);
			else {
				btnMarkToRead.setBackgroundColor(colorGrayCT);
				btnMarkToRead.setPaintFlags( btnMarkToRead.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
			}*/
		btnMarkToRead.setTextColor(colorBlue);
	}

	private void downloadFragment(String format) {
		final FileInfo downloadDir1 = Services.getScanner().getDownloadDirectory();
		final FileInfo downloadDir = new FileInfo(downloadDir1.pathname+"/fragments");
		downloadDir.parent = downloadDir1;
		final String fName = downloadDir1.pathname+"/fragments/" + Utils.transcribeFileName(mFileInfoCloud.getFilename()) + "_fragment" + format;
		final File fPath = new File(downloadDir1.pathname+"/fragments");
		if (!fPath.exists()) fPath.mkdir();
		progressDlg = ProgressDialog.show(mCoolReader,
				mCoolReader.getString(R.string.network_op),
				mCoolReader.getString(R.string.network_op),
				true, false, null);
		BackgroundThread.instance().postBackground(() -> {
			FileOutputStream fos;
			try {
				InputStream in;
				String redir_url = Utils.getUrlLoc(new java.net.URL(mFileInfoCloud.fragment_href + format));
				if (StrUtils.isEmptyStr(redir_url))
					in = new java.net.URL(mFileInfoCloud.fragment_href + format).openStream();
				else
					in = new java.net.URL(redir_url).openStream();
				final File fBook = new File(fName);
				fos = new FileOutputStream(fBook);
				BufferedOutputStream out = new BufferedOutputStream(fos);
				Utils.copyStreamContent(out, in);
				out.flush();
				fos.getFD().sync();
				BackgroundThread.instance().postGUI(() -> {
					onPositiveButtonClick();
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					FileUtils.fileDownloadEndThenOpen("litres", mFileInfoCloud.fragment_href + format, redir_url,
							fBook, mCoolReader, Services.getEngine(), "", Services.getScanner(), downloadDir,
							mFileInfoCloud, mFileInfoCloud.parent, mFileInfoCloud.annotation);
				});
				//BackgroundThread.instance().postBackground(() ->
						//BackgroundThread.instance().postGUI(() -> ((CoolReader) activity).
						//		loadDocumentExt(fBook.getPath(), mFileInfoCloud.fragment_href + format), 500));
			} catch (Exception e) {
				BackgroundThread.instance().postGUI(() -> {
					onPositiveButtonClick();
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					activity.showToast(activity.getString(R.string.not_exists_on_server)+" "+e.getLocalizedMessage());
				});
			}
		});
	}

	public BookInfoDialog(final BaseActivity activity, Collection<BookInfoEntry> items, BookInfo bi, final String annt,
						  int actionType, FileInfo fiOPDS, FileBrowser fb, FileInfo currDir)
	{
		super("BookInfoDialog", activity, null, false, false);
		String annot = annt;
		if (StrUtils.isEmptyStr(annt))
			if (fiOPDS != null) {
				if (!StrUtils.isEmptyStr(fiOPDS.annotation)) annot = fiOPDS.annotation;
				try {
					annot = android.text.Html.fromHtml(fiOPDS.annotation).toString();
				} catch (Exception e) {
					annot = fiOPDS.annotation;
				}
			}
		mCoolReader = (CoolReader) activity;
		mBookInfo = bi;
		mActionType = actionType;
		mFileInfoCloud = fiOPDS;
		if (mFileInfoCloud != null) {
			if ((mFileInfoCloud.isLitresBook()) || (mFileInfoCloud.isLitresSpecialDir()))
				isLitres = true;
			if ((mFileInfoCloud.isLitresPerson()))
				isPerson = true;
		}
		mFileBrowser = fb;
		mCurrDir = currDir;
		FileInfo file = null;
		if (mBookInfo!=null)
			file = mBookInfo.getFileInfo();
		else file = fiOPDS;
		mFileInfoSearchDir = null;
		if (currDir!=null) {
			if (currDir.isOPDSDir())  {
				FileInfo par = currDir;
				while (par.parent != null) par=par.parent;
				for (int i=0; i<par.dirCount(); i++)
					if (par.getDir(i).pathname.startsWith(OPDS_DIR_PREFIX + "search:")) {
						mFileInfoSearchDir = par.getDir(i);
						break;
					}
			}
		}
		for (BookInfoEntry s: items) {
			if (s.infoTitle.equals("book.authors")) {
				mAuthors = s.infoValue;
			}
		}
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		//setTitle(mCoolReader.getString(R.string.dlg_book_info));
		fillMap();
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.book_info_dialog, null);
		final ImageView image = view.findViewById(R.id.book_cover);
		image.setOnClickListener(v -> {
			if ((mActionType == OPDS_INFO) && (isLitres)) return;
			CoolReader cr = (CoolReader)mCoolReader;
			if (mBookInfo!=null) {
				FileInfo fi = mBookInfo.getFileInfo();
				cr.loadDocument(fi, true);
				dismiss();
			} else //cr.showToast(R.string.book_info_action_unavailable);
			{
				cr.showToast(R.string.book_info_action_downloading);
				if (mFileBrowser != null) mFileBrowser.showOPDSDir(mFileInfoCloud, mFileInfoCloud, annot2);
				dismiss();
			}
		});
		btnBack = view.findViewById(R.id.base_dlg_btn_back);
		btnBack.setOnClickListener(v -> onNegativeButtonClick());

		btnOpenBook = ((ImageButton)view.findViewById(R.id.btn_open_book));
		btnOpenBook.setOnClickListener(v -> {
			CoolReader cr = (CoolReader)mCoolReader;
			if (mBookInfo!=null) {
				FileInfo fi = mBookInfo.getFileInfo();
				cr.loadDocument(fi, true);
				dismiss();
			} else {
				//cr.showToast(R.string.book_info_action_unavailable);
				cr.showToast(R.string.book_info_action_downloading);
				if (mFileBrowser != null) mFileBrowser.showOPDSDir(mFileInfoCloud, mFileInfoCloud, annot2);
				dismiss();
			}
		});

		btnBookFolderOpen = view.findViewById(R.id.book_folder_open);

			btnBookFolderOpen.setOnClickListener(v -> {
				CoolReader cr = (CoolReader)mCoolReader;
				if (mBookInfo != null) {
					cr.showDirectory(mBookInfo.getFileInfo(), "");
					dismiss();
				}
			});

		btnBookShortcut = view.findViewById(R.id.book_create_shortcut);

		btnBookShortcut.setOnClickListener(v -> {
		CoolReader cr = (CoolReader)mCoolReader;
		if (mBookInfo!=null) {
			FileInfo fi = mBookInfo.getFileInfo();
			cr.createBookShortcut(fi,mBookCover);
		} else cr.showToast(R.string.book_info_action_unavailable);
		});

		btnBookEdit = view.findViewById(R.id.book_edit);
		btnBookEdit.setOnClickListener(v -> {
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
		});

		ImageButton btnSendByEmail = view.findViewById(R.id.save_to_email);

		btnSendByEmail.setOnClickListener(v -> CloudAction.emailSendBook((CoolReader) mCoolReader, mBookInfo));

		ImageButton btnSendByYnd = view.findViewById(R.id.save_to_ynd);

		btnSendByYnd.setOnClickListener(v -> CloudAction.yndOpenBookDialog((CoolReader) mCoolReader, mBookInfo.getFileInfo(),true));

		ImageButton btnDeleteBook = view.findViewById(R.id.book_delete);
		btnDeleteBook.setOnClickListener(v -> {
			((CoolReader)activity).askDeleteBook(mBookInfo.getFileInfo());
			dismiss();
		});

		ImageButton btnCustomCover = view.findViewById(R.id.book_custom_cover);
		btnCustomCover.setOnClickListener((View.OnClickListener) v -> {
			if (((CoolReader)activity).picReceived!=null) {
				if (((CoolReader)activity).picReceived.bmpReceived!=null) {
					PictureCameDialog dlg = new PictureCameDialog(((CoolReader) activity),
							mBookInfo, "", "");
					dlg.show();
				}
			} else {
				((CoolReader)activity).showToast(R.string.pic_no_pic);
			}
		});

		btnMarkToRead = view.findViewById(R.id.btn_mark_toread);

		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		if (btnMarkToRead!=null) btnMarkToRead.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		bMarkToRead = activity.settings().getBool(Settings.PROP_APP_MARK_DOWNLOADED_TO_READ, false);

		btnMarkToRead.setOnClickListener(v -> {
		bMarkToRead = !bMarkToRead;
		Properties props = new Properties(activity.settings());
		props.setProperty(Settings.PROP_APP_MARK_DOWNLOADED_TO_READ, bMarkToRead?"1":"0");
		activity.setSettings(props, -1, true);
		paintMarkButton();
		});
		paintMarkButton();
		btnBookDownload = view.findViewById(R.id.book_download);
		annot2 = annot;
		btnBookDownload.setOnClickListener(v -> {
			if (mFileBrowser != null) mFileBrowser.showOPDSDir(mFileInfoCloud, mFileInfoCloud, annot2);
			dismiss();
		});
		btnFindAuthors = view.findViewById(R.id.btn_find_authors);
		btnFindAuthors.setOnClickListener(v -> {
			if ((mFileBrowser != null) && (mFileInfoSearchDir!=null))
				mFileBrowser.showFindBookDialog(false, mAuthors, mFileInfoSearchDir);
			dismiss();
		});
		int w = mWindowSize * 4 / 10;
		int h = w * 4 / 3;
		image.setMinimumHeight(h);
		image.setMaxHeight(h);
		image.setMinimumWidth(w);
		image.setMaxWidth(w);
		Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		if (file != null) {
			if (mFileInfoCloud !=null) {
				String sTitle = StrUtils.getNonEmptyStr(file.title, true);
				String sAuthors = StrUtils.getNonEmptyStr(file.authors, true).replace("\\|", "\n");
				if (StrUtils.isEmptyStr(sTitle))
					sTitle = StrUtils.stripExtension(file.getFilename());
				Bitmap bmp2 = Services.getCoverpageManager().getBookCoverWithTitleBitmap(sTitle, sAuthors,
					150, 200);
				image.setImageBitmap(bmp2);
				if ((!StrUtils.isEmptyStr(mFileInfoCloud.cover_href)) ||
					(!StrUtils.isEmptyStr(mFileInfoCloud.cover_href2))) {
					if (!StrUtils.isEmptyStr(mFileInfoCloud.cover_href)) new DownloadImageTask(image).execute(mFileInfoCloud.cover_href);
					if (!StrUtils.isEmptyStr(mFileInfoCloud.cover_href2)) new DownloadImageTask(image).execute(mFileInfoCloud.cover_href2);
				} else
					for (OPDSUtil.LinkInfo link : mFileInfoCloud.links) {
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
		TableLayout table = view.findViewById(R.id.table);
		FlowTextView txtAnnot = view.findViewById(R.id.lbl_annotation);
		txtAnnot.setOnClickListener(v -> {
			String text = ((FlowTextView) v).getText().toString();
			if (text != null && text.length() > 0) {
				ClipboardManager cm = activity.getClipboardmanager();
				cm.setText(text);
				L.i("Setting clipboard text: " + text);
				activity.showToast(activity.getString(R.string.copied_to_clipboard)+": "+text,v);
			}
		});
		File f = activity.getSettingsFileF(activity.getCurrentProfile());
		String sF = f.getAbsolutePath();
		sF = sF.replace("/storage/","/s/").replace("/emulated/","/e/");
		TextView prof = (TextView) view.findViewById(R.id.lbl_profile);
		String sprof = activity.getCurrentProfileName();
		if (!StrUtils.isEmptyStr(sprof)) sprof = sprof + " - ";
		prof.setText(activity.getString(R.string.settings_profile)+": "+sprof + sF);
		String sss = "";
		if ((mActionType == OPDS_INFO) && (!isLitres)) sss = "["+mCoolReader.getString(R.string.book_info_action_download1)+"]\n";
		if (mActionType == OPDS_FINAL_INFO) sss = sss = "["+mCoolReader.getString(R.string.book_info_action_download2)+"]\n";
		SpannableString ss = new SpannableString(sss+annot);
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

		tlLitresDownl =  view.findViewById(R.id.tl_downl_book);
		llLitresPurchase =  view.findViewById(R.id.ll_litres_purchase);

		if (mFileInfoCloud != null) {
			btnFragment = view.findViewById(R.id.btn_fragment);
			btnFragment.setOnClickListener(v -> {
				setLitresDownloadModeChecked(btnFragment);
			});
			btnDownloadLitresBook = view.findViewById(R.id.btn_downl);
			btnDownloadLitresBook.setOnClickListener(v -> {
				setLitresDownloadModeChecked(btnDownloadLitresBook);
			});
			Drawable img0 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
			Drawable img11 = img.getConstantState().newDrawable().mutate();
			Drawable img22 = img.getConstantState().newDrawable().mutate();
			btnFragment.setCompoundDrawablesWithIntrinsicBounds(img11, null, null, null);
			btnDownloadLitresBook.setCompoundDrawablesWithIntrinsicBounds(img22, null, null, null);

			setLitresDownloadModeChecked(null);
			btnDownloadFB2 = view.findViewById(R.id.btn_fb2);
			btnDownloadFB3 = view.findViewById(R.id.btn_fb3);

			btnDownloadFB2.setOnClickListener(v -> {
				btnDownloadFB2.setEnabled(false);
				btnDownloadFB2.setVisibility(View.INVISIBLE);
				BackgroundThread.instance().postGUI(() -> {
					try {
						if (btnDownloadFB2 != null) {
							btnDownloadFB2.setEnabled(true);
							btnDownloadFB2.setVisibility(View.VISIBLE);
						}
					} catch (Exception e) {

					}
				}, 5000);
				if (curDownloadMode == DM_FRAGMENT) {
					downloadFragment(".fb2.zip");
				} else {
					mFileInfoCloud.format_chosen = "fb2.zip";
					CloudAction.litresDownloadBook((CoolReader) mCoolReader, mFileInfoCloud, this);
				}
			});

			btnDownloadFB3.setOnClickListener(v -> {
				btnDownloadFB3.setEnabled(false);
				btnDownloadFB3.setVisibility(View.INVISIBLE);
				BackgroundThread.instance().postGUI(() -> {
					try {
						if (btnDownloadFB3 != null) {
							btnDownloadFB3.setEnabled(true);
							btnDownloadFB3.setVisibility(View.VISIBLE);
						}
					} catch (Exception e) {

					}
				}, 5000);
				if (curDownloadMode == DM_FRAGMENT) {
					downloadFragment(".fb3");
				} else {
					mFileInfoCloud.format_chosen = "fb3";
					CloudAction.litresDownloadBook((CoolReader) mCoolReader, mFileInfoCloud, this);
				}
			});
			int colorGrayCT = Color.argb(128, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC));
			btnDownloadFB2.setBackgroundColor(colorGrayCT);
			btnDownloadFB3.setBackgroundColor(colorGrayCT);
			if (mFileInfoCloud.type != 0) {
				Utils.hideView(tlLitresDownl);
			}
			btnPurchase = view.findViewById(R.id.btn_purchase);
			btnPurchase.setBackgroundColor(colorGrayCT);
			TextView tvLvl = view.findViewById(R.id.lbl_purchase);
			if (mFileInfoCloud.lvl > 0)
				tvLvl.setText(tvLvl.getText() + " (" + mCoolReader.getString(R.string.online_store_book_rating)+": "+ mFileInfoCloud.lvl + ")");
			else
				tvLvl.setText("");
			btnPurchase.setOnClickListener(v -> {
				if (mPurchased) return;
				if (mFileInfoCloud.available != 1) return;
				if (mFileInfoCloud.type != 0) return;
				if (btnPurchase.getText().toString().equals(mCoolReader.getString(R.string.online_store_purchase_success))) return;
				mCoolReader.askConfirmation( mCoolReader.getString(R.string.online_store_confirm_purchase), () -> {
						CloudAction.litresPurchaseBook(mCoolReader, mFileInfoCloud, this);
						btnPurchase.setEnabled(false);
						btnPurchase.setVisibility(View.INVISIBLE);
						BackgroundThread.instance().postGUI(() -> {
							try {
								if (btnPurchase != null) {
									btnPurchase.setEnabled(true);
									btnPurchase.setVisibility(View.VISIBLE);
								}
							} catch (Exception e) {

							}
						}, 20000);
					}
				);
			});

			if (mFileInfoCloud.available == 1)
				btnPurchase.setText(String.format("%.2f", mFileInfoCloud.finalPrice) + " " + LitresConfig.currency);
			else if (mFileInfoCloud.available == 2)
				btnPurchase.setText(mCoolReader.getString(R.string.online_store_book_soon));
				else if (mFileInfoCloud.available == 6)
						btnPurchase.setText(mCoolReader.getString(R.string.online_store_book_soon)+": "+ mFileInfoCloud.availDate);
					else btnPurchase.setText(mCoolReader.getString(R.string.online_store_book_unavailable));
			if (mFileInfoCloud.type == 4) btnPurchase.setText(mCoolReader.getString(R.string.online_store_book_not_supported)+": PDF");
			if (mFileInfoCloud.type == 1) btnPurchase.setText(mCoolReader.getString(R.string.online_store_book_not_supported)+": AudioBook");
			if (mFileInfoCloud.type == 11) btnPurchase.setText(mCoolReader.getString(R.string.online_store_book_not_supported)+": Gardner books");
			if (isLitres)
				if (mFileInfoCloud.lsp.searchType == LitresSearchParams.SEARCH_TYPE_MY_BOOKS) {
					//Utils.hideView(tlLitresDownl);
					Utils.hideView(llLitresPurchase);
				}
		}
		if ((!isLitres) || (isPerson)) {
			try {
				Utils.hideView(btnMarkToRead);
				Utils.hideView(tlLitresDownl);
				Utils.hideView(llLitresPurchase);
			} catch (Exception e) {
				// do nothing
			}
		}

		if ((StrUtils.isEmptyStr(mAuthors))||(mFileInfoSearchDir==null)) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnFindAuthors);
		}
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
			parent.removeView(btnSendByEmail);
			parent.removeView(btnDeleteBook);
			parent.removeView(btnCustomCover);
			parent.removeView(btnSendByYnd);
		}
		if (actionType == OPDS_FINAL_INFO) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnBookDownload);
			parent.removeView(btnMarkToRead);
			parent.removeView(btnFindAuthors);
		}
		if (isLitres) Utils.hideView(btnBookDownload);
		for ( BookInfoEntry item : items ) {
			String name = item.infoTitle;
			String value = item.infoValue;
			if (name.equals("file.name")) sFileName = value;
			if (name.equals("book.authors")) sAuthors = value.replace("|",", ");
			if (name.equals("book.title")) sBookTitle = value;
		}
		for ( BookInfoEntry item : items ) {
			addItem(table, item);
		}
		buttonsLayout = view.findViewById(R.id.base_dlg_button_panel);
		updateGlobalMargin(buttonsLayout, true, true, true, false);
		setView( view );
	}

}
