package org.coolreader.crengine;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.layouts.FlowLayout;
import org.coolreader.utils.FileUtils;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SaveDocDialog extends BaseDialog implements FolderSelectedCallback {

	static class FolderControls {
		Button folderButton;
		TextView folderLabel;
	}

	public static final Logger log = L.create("sdd");

	private CoolReader mActivity;
	private int mWindowSize;
	private LayoutInflater mInflater;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	ArrayList<FolderControls> mFolders = new ArrayList<>();
	FolderControls selectedFolder;
	FolderControls notMoveFolder;

	boolean mIsHTML = true;
	int mChosenState = FileInfo.STATE_NEW;
	private int mChosenRate = 0;

	Button bookStateNew;
	Button bookStateToRead;
	Button bookStateReading;
	Button bookStateFinished;
	int colorIcon;
	int colorGrayC;
	int colorGrayCT;
	int colorGrayCT2;

	TextView tvChosenFolder;

	private ImageButton btnStar1;
	private ImageButton btnStar2;
	private ImageButton btnStar3;
	private ImageButton btnStar4;
	private ImageButton btnStar5;
	private int attrStar;
	private int attrStarFilled;
	private boolean mDoMove = false;
	String mFileDir;
	String mExistingFileName;
	String mSuggestedFileName;
	String mFileExt;
	String mSUri;
	Uri mUri;
	String mSType;
	private EditText edtFileName;
	private EditText edtFileExt;
	boolean addAuthorsFolder = true;
	FolderControls addAuthorsFC;
	private InputStream mIstream = null;
	private InputStream mIstreamTxt = null;

	private boolean bAddControls = false;
	private TableRow trDocAdd1;
	private TableRow trDocAdd2;
	private FlowLayout flDocAdd2;
	private ImageView mIvExpand;

	@Override
	public void folderSelected(String path) {
		if (tvChosenFolder != null) {
			for (FolderControls fc1: mFolders)
				if (fc1.folderLabel == tvChosenFolder) {
					selectedFolder = fc1;
					paintButtons();
					break;
				}
			tvChosenFolder.setText(path);
		}
	}

	public SaveDocDialog(CoolReader activity, boolean doMove, String fileDir, String existingFileName,
						 String suggestedFileName, String fileExt, String sUri, Uri uri, String stype) {
		super(activity, activity.getString(R.string.save_doc_to_library), false, true);
		// fileDir = /storage/emulated/0/Книги/Мэри Стюарт
		// existingFileName = /storage/emulated/0/Книги/Мэри Стюарт/Сага о Короле Артуре.fb2.zip
		// suggestedFileName = KnownReader_Downloaded
		// fileExt = .
		// sUri = /storage/emulated/0/Книги/Мэри Стюарт/Сага о Короле Артуре.fb2.zip
		// stype = ""
		mDoMove = doMove;
		mFileDir = fileDir;
		mExistingFileName = existingFileName;
		mSuggestedFileName = suggestedFileName;
		mFileExt = fileExt;
		mSUri = sUri;
		mUri = uri;
		mSType = stype;
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		this.mActivity = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mActivity, isEInk);
		colorIcon = themeColors.get(R.attr.colorIcon);
		colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		colorGrayCT= Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		if(getWindow().getAttributes().softInputMode== WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		}
	}

	public void setHTMLData(boolean isHTML, InputStream istream, InputStream istreamTxt) {
		mIsHTML = isHTML;
		mIstreamTxt = istreamTxt;
		mIstream = istream;
	}

	@Override
	protected void onCreate() {
		setCancelable(true);
		setCanceledOnTouchOutside(true);

		super.onCreate();
		L.v("SaveDocDialog is created");
	}

	private void paintButtons() {
		int colorGrayC;
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast});
		colorGrayC = a.getColor(0, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		for (FolderControls fc: mFolders) {
			mActivity.tintViewIcons(fc.folderButton, PorterDuff.Mode.CLEAR,true);
			fc.folderButton.setBackgroundColor(colorGrayCT);
			fc.folderLabel.setBackgroundColor(colorGrayCT);
			if (selectedFolder == fc) {
				mActivity.tintViewIcons(fc.folderButton,true);
				fc.folderButton.setBackgroundColor(colorGrayCT2);
				fc.folderLabel.setBackgroundColor(colorGrayCT2);
			}
		}
		if (bookStateNew != null) {
			bookStateNew.setBackgroundColor(colorGrayCT);
			mActivity.tintViewIcons(bookStateNew, PorterDuff.Mode.CLEAR, true);
			if (mChosenState == FileInfo.STATE_NEW) {
				mActivity.tintViewIcons(bookStateNew, true);
				bookStateNew.setBackgroundColor(colorGrayCT2);
			}
		}
		if (bookStateToRead != null) {
			bookStateToRead.setBackgroundColor(colorGrayCT);
			mActivity.tintViewIcons(bookStateToRead, PorterDuff.Mode.CLEAR, true);
			if (mChosenState == FileInfo.STATE_TO_READ) {
				mActivity.tintViewIcons(bookStateToRead, true);
				bookStateToRead.setBackgroundColor(colorGrayCT2);
			}
		}
		if (bookStateReading != null) {
			bookStateReading.setBackgroundColor(colorGrayCT);
			mActivity.tintViewIcons(bookStateReading, PorterDuff.Mode.CLEAR, true);
			if (mChosenState == FileInfo.STATE_READING) {
				mActivity.tintViewIcons(bookStateReading, true);
				bookStateReading.setBackgroundColor(colorGrayCT2);
			}
		}
		if (bookStateFinished != null) {
			bookStateFinished.setBackgroundColor(colorGrayCT);
			mActivity.tintViewIcons(bookStateFinished, PorterDuff.Mode.CLEAR, true);
			if (mChosenState == FileInfo.STATE_FINISHED) {
				mActivity.tintViewIcons(bookStateFinished, true);
				bookStateFinished.setBackgroundColor(colorGrayCT2);
			}
		}
		mActivity.tintViewIcons(addAuthorsFC.folderButton, PorterDuff.Mode.CLEAR,true);
		addAuthorsFC.folderButton.setBackgroundColor(colorGrayCT);
		addAuthorsFC.folderLabel.setBackgroundColor(colorGrayCT);
		if (addAuthorsFolder) {
			addAuthorsFC.folderButton.setBackgroundColor(colorGrayCT2);
			addAuthorsFC.folderLabel.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(addAuthorsFC.folderButton,true);
		}
		setRate(null);
	}

	private void addFolderToTL(TableLayout toTL, String folder, String path, boolean notMove) {
		FolderControls fc = new FolderControls();
		View viewFolder = mInflater.inflate(R.layout.save_doc_folder_row, null);
		Button booksFolder = viewFolder.findViewById(R.id.btn_folder);
		Drawable img1;
		if (folder.equals(mActivity.getString(R.string.add_author_folder))) {
			Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_check_no_frame);
			img1 = img.getConstantState().newDrawable().mutate();
		} else {
			Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
			img1 = img.getConstantState().newDrawable().mutate();
		}
		booksFolder.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		fc.folderButton = booksFolder;
		booksFolder.setText(folder);
		booksFolder.setBackgroundColor(colorGrayCT);
		TextView tvFolder = viewFolder.findViewById(R.id.txt_folder);
		fc.folderLabel = tvFolder;
		tvFolder.setText(path);
		if (folder.equals(mActivity.getString(R.string.do_not_move)))
			notMoveFolder = fc;
		if (folder.equals(mActivity.getString(R.string.scan_books_folder))) {
			selectedFolder = fc;
			if (notMoveFolder != null)
				if (notMove) selectedFolder = notMoveFolder;
		}
		if (folder.equals(mActivity.getString(R.string.add_author_folder))) {
			addAuthorsFC = fc;
			booksFolder.setOnClickListener(v -> {
				addAuthorsFolder = !addAuthorsFolder;
				paintButtons();
			});
		} else
			if (folder.equals(mActivity.getString(R.string.select_folder))) {
				tvChosenFolder = tvFolder;
				booksFolder.setOnClickListener(v -> {
					final Intent chooserIntent = new Intent(
							mActivity,
							DirectoryChooserActivity.class);

					final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
							.newDirectoryName("NewFolder")
							.allowReadOnlyDirectory(true)
							.allowNewDirectoryNameModification(true)
							.build();

					chooserIntent.putExtra(
							DirectoryChooserActivity.EXTRA_CONFIG,
							config);
					mActivity.dirChosenCallback=this;
					mActivity.startActivityForResult(chooserIntent, CoolReader.REQUEST_CODE_CHOOSE_DIR);
				});
			} else {
				booksFolder.setOnClickListener(v -> {
					for (FolderControls fc1: mFolders)
						if (fc1.folderButton == booksFolder) {
							selectedFolder = fc1;
							paintButtons();
							break;
						}
				});
			}
		if (!folder.equals(mActivity.getString(R.string.add_author_folder))) mFolders.add(fc);
		toTL.addView(viewFolder);
	}

	private void addFoldersNames(TableLayout mainTl) {
		ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
		String par = "";
		if (mDoMove) {
			addFolderToTL(mainTl, mActivity.getString(R.string.do_not_move),
					"", false);
		}
		for (FileInfo fi: folders) {
			if (fi.getType() != FileInfo.TYPE_FS_ROOT) {
				if (fi.getType() == FileInfo.TYPE_DOWNLOAD_DIR) {
					boolean selNotMove = mExistingFileName.startsWith(fi.pathname);
					addFolderToTL(mainTl, mActivity.getString(R.string.scan_books_folder), fi.pathname, selNotMove);
				}
			}
		}
		addFolderToTL(mainTl, mActivity.getString(R.string.scan_downloads_folder),
				Environment.getExternalStorageDirectory().toString()+ File.separator + Environment.DIRECTORY_DOWNLOADS, false);
		for (FileInfo fi: folders) {
			if (fi.getType() != FileInfo.TYPE_FS_ROOT) {
				if (fi.getType() != FileInfo.TYPE_DOWNLOAD_DIR) {
					String labText = StrUtils.getNonEmptyStr(fi.pathname, true);
					String[] arrLab = labText.split("/");
					if (arrLab.length>2) labText="../"+arrLab[arrLab.length-2]+"/"+arrLab[arrLab.length-1];
					addFolderToTL(mainTl, labText, fi.pathname, false);
				}
			}
		}
		addFolderToTL(mainTl, mActivity.getString(R.string.select_folder),
				"", false);
		addFolderToTL(mainTl, mActivity.getString(R.string.add_author_folder),
				"", false);
	}

	private void setupFileNameControls(TableLayout mainTl) {
		View viewTitle = mInflater.inflate(R.layout.save_doc_title, null);
		TextView tvTitle = viewTitle.findViewById(R.id.txt_title);
		tvTitle.setText(mActivity.getString(R.string.select_file_name));
		mainTl.addView(viewTitle);
		View viewFname = mInflater.inflate(R.layout.save_doc_select_fname, null);
		edtFileName = viewFname.findViewById(R.id.file_name);
		edtFileExt = viewFname.findViewById(R.id.file_ext);
		String sName = mSuggestedFileName;
		String sExt = mFileExt;
		if (!StrUtils.isEmptyStr(mExistingFileName)) {
			sName = new File(mExistingFileName).getName();
			int i = sName.lastIndexOf('.');
			if (i > 0) {
				sExt = sName.substring(i);
				sName = sName.substring(0,i);
			}
		}
		edtFileName.setText(sName);
		edtFileExt.setText(sExt);
		mainTl.addView(viewFname);
	}

	private void setRate(ImageButton btn) {
		if (btn != null) {
			int mChosenRate1 = 0;
			if (btn == btnStar1) {
				mChosenRate1 = 1;
			}
			if (btn == btnStar2) {
				mChosenRate1 = 2;
			}
			if (btn == btnStar3) {
				mChosenRate1 = 3;
			}
			if (btn == btnStar4) {
				mChosenRate1 = 4;
			}
			if (btn == btnStar5) {
				mChosenRate1 = 5;
			}
			if (mChosenRate1 == mChosenRate) mChosenRate = 0;
			else mChosenRate = mChosenRate1;
		}
		if (btnStar1 != null)
			btnStar1.setImageResource(mChosenRate>=1?attrStarFilled:attrStar);
		if (btnStar2 != null)
			btnStar2.setImageResource(mChosenRate>=2?attrStarFilled:attrStar);
		if (btnStar3 != null)
			btnStar3.setImageResource(mChosenRate>=3?attrStarFilled:attrStar);
		if (btnStar4 != null)
			btnStar4.setImageResource(mChosenRate>=4?attrStarFilled:attrStar);
		if (btnStar5 != null)
			btnStar5.setImageResource(mChosenRate>=5?attrStarFilled:attrStar);
	}

	private void setupStarsButtons() {
		View viewStarsC = mInflater.inflate(R.layout.save_doc_add1_c, null);
		btnStar1 = viewStarsC.findViewById(R.id.book_star1);
		btnStar2 = viewStarsC.findViewById(R.id.book_star2);
		btnStar3 = viewStarsC.findViewById(R.id.book_star3);
		btnStar4 = viewStarsC.findViewById(R.id.book_star4);
		btnStar5 = viewStarsC.findViewById(R.id.book_star5);
		btnStar1.setOnClickListener(v -> setRate(btnStar1));
		btnStar2.setOnClickListener(v -> setRate(btnStar2));
		btnStar3.setOnClickListener(v -> setRate(btnStar3));
		btnStar4.setOnClickListener(v -> setRate(btnStar4));
		btnStar5.setOnClickListener(v -> setRate(btnStar5));
		trDocAdd1.addView(viewStarsC);
		mActivity.tintViewIcons(trDocAdd1);
	}

	private void setupStatesButtons() {
		ViewGroup viewStatesC = (ViewGroup) mInflater.inflate(R.layout.save_doc_add2_c, null);
		bookStateNew = viewStatesC.findViewById(R.id.book_state_new);
		bookStateToRead = viewStatesC.findViewById(R.id.book_state_toread);
		bookStateReading = viewStatesC.findViewById(R.id.book_state_reading);
		bookStateFinished = viewStatesC.findViewById(R.id.book_state_finished);
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		Drawable img3 = img.getConstantState().newDrawable().mutate();
		Drawable img4 = img.getConstantState().newDrawable().mutate();
		bookStateNew.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		bookStateToRead.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		bookStateReading.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
		bookStateFinished.setCompoundDrawablesWithIntrinsicBounds(img4, null, null, null);
		int colorBlue = themeColors.get(R.attr.colorThemeBlue);
		int colorGreen = themeColors.get(R.attr.colorThemeGreen);
		int colorGray = themeColors.get(R.attr.colorThemeGray);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		bookStateNew.setTextColor(colorIcon);
		bookStateNew.setOnClickListener(v -> {
			mChosenState = FileInfo.STATE_NEW;
			paintButtons();
		});
		bookStateToRead.setTextColor(colorBlue);
		bookStateToRead.setOnClickListener(v -> {
			mChosenState = FileInfo.STATE_TO_READ;
			paintButtons();
		});
		bookStateReading.setTextColor(colorGreen);
		bookStateReading.setOnClickListener(v -> {
			mChosenState = FileInfo.STATE_READING;
			paintButtons();
		});
		bookStateFinished.setTextColor(colorGray);
		bookStateFinished.setOnClickListener(v -> {
			mChosenState = FileInfo.STATE_FINISHED;
			paintButtons();
		});
		TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.attr_icons8_rate_star,
						R.attr.attr_icons8_rate_star_filled
				});
		attrStar = a.getResourceId(0, 0);
		attrStarFilled = a.getResourceId(1, 0);
		a.recycle();
		viewStatesC.removeView(bookStateNew);
		flDocAdd2.addView(bookStateNew);
		viewStatesC.removeView(bookStateToRead);
		flDocAdd2.addView(bookStateToRead);
		viewStatesC.removeView(bookStateReading);
		flDocAdd2.addView(bookStateReading);
		viewStatesC.removeView(bookStateFinished);
		flDocAdd2.addView(bookStateFinished);
		//trDocAdd2.addView(viewStatesC);
		mActivity.tintViewIcons(trDocAdd2);
	}

	private void expandClick(ImageView ivExpand) {
		if (bAddControls) {
			trDocAdd1.removeAllViews();
			flDocAdd2.removeAllViews();
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
			ivExpand.setImageDrawable(d);
			mActivity.tintViewIcons(ivExpand);
			setupStarsButtons();
			setupStatesButtons();
			paintButtons();
		} else {
			trDocAdd1.removeAllViews();
			flDocAdd2.removeAllViews();
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
			ivExpand.setImageDrawable(d);
			mActivity.tintViewIcons(ivExpand);
		}
	}

	private void setupAddControls(TableLayout mainTl) {
		View viewTitle = mInflater.inflate(R.layout.save_doc_title_exp, null);
		TextView tvTitle = viewTitle.findViewById(R.id.txt_title);
		mIvExpand = viewTitle.findViewById(R.id.iv_title);
		View.OnClickListener lstnr = v -> {
			bAddControls = !bAddControls;
			expandClick(mIvExpand);
			Properties props = new Properties(mActivity.settings());
			props.setProperty(Settings.PROP_APP_SAVE_DOC_EXT_CONTROLS_SHOW, bAddControls?"1":"0");
			mActivity.setSettings(props, -1, false);
		};
		tvTitle.setOnClickListener(lstnr);
		mIvExpand.setOnClickListener(lstnr);
		tvTitle.setText(mActivity.getString(R.string.additional_marks));
		mainTl.addView(viewTitle);

		View viewStars = mInflater.inflate(R.layout.save_doc_add1, null);
		trDocAdd1 = viewStars.findViewById(R.id.tr_save_doc_add1);
		mainTl.addView(viewStars);
		if (bAddControls) setupStarsButtons();
		View viewStates = mInflater.inflate(R.layout.save_doc_add2, null);
		trDocAdd2 = viewStates.findViewById(R.id.tr_save_doc_add2);
		flDocAdd2 = trDocAdd2.findViewById(R.id.fl_states);
		mainTl.addView(viewStates);
		if (bAddControls) setupStatesButtons();
	}

	private void setupButtons(TableLayout mainTl) {
		View viewButton = mInflater.inflate(R.layout.save_doc_button, null);
		Button btn = viewButton.findViewById(R.id.btn_save_doc);
		if (mDoMove)
			btn.setText(mActivity.getString(R.string.move_file));
		else
			btn.setText(mActivity.getString(R.string.copy_file));
		btn.setOnClickListener(v -> {
			String toFileName = "";
			String toDir = "";
			if (selectedFolder != null) {
				toDir = selectedFolder.folderLabel.getText().toString();
				toFileName = edtFileName.getText().toString() +
						(edtFileExt.getText().toString().startsWith(".") ? edtFileExt.getText().toString() :
							"." + edtFileExt.getText().toString());
			}
			if (mDoMove)
				tryToMoveThenOpen(this, mActivity, mExistingFileName, mFileDir,
					toFileName, toDir);
			else
				saveFile(toFileName, toDir, mUri);
		});
		Utils.setDashedButton(btn);
		btn.setTextColor(mActivity.getTextColor(colorIcon));
		mainTl.addView(viewButton);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInflater = LayoutInflater.from(getContext());
		ViewGroup view = (ViewGroup) mInflater.inflate(R.layout.save_doc_dialog, null);
		TextView tvPath = view.findViewById(R.id.tv_path);
		if (!mDoMove) Utils.hideView(tvPath);
		else tvPath.setText(mExistingFileName);
		TableLayout mainTl = view.findViewById(R.id.table);
		addFoldersNames(mainTl);
		setupFileNameControls(mainTl);
		bAddControls = mActivity.settings().getBool(Settings.PROP_APP_SAVE_DOC_EXT_CONTROLS_SHOW, false);
		setupAddControls(mainTl);
		setupButtons(mainTl);
		getAuthorSubdir(this);
		paintButtons();
		setView(view);
		BackgroundThread.instance().postGUI(() -> {
			paintButtons();
		}, 300);
	}

	public static void getAuthorSubdir(BaseDialog bd) {
		if (!(bd instanceof SaveDocDialog)) return;
		SaveDocDialog sdd = (SaveDocDialog) bd;
		if ((StrUtils.isEmptyStr(sdd.mExistingFileName)) || (StrUtils.isEmptyStr(sdd.mFileDir))) return;
		FileInfo fileOrDir = FileUtils.getFileProps(null,
				new File(sdd.mExistingFileName), new FileInfo(sdd.mFileDir), true);
		if (StrUtils.isEmptyStr(fileOrDir.getAuthors())) Services.getEngine().scanBookProperties(fileOrDir);
		bd.mActivity.waitForCRDBService(() -> {
			bd.mActivity.getDB().getBookFlags(fileOrDir, fl -> {
				String subdir = null;
				if (!StrUtils.isEmptyStr(fileOrDir.getAuthors())) {
					subdir = Utils.transcribeFileName(fileOrDir.getAuthors());
					if (subdir.length() > FileBrowser.MAX_SUBDIR_LEN)
						subdir = subdir.substring(0, FileBrowser.MAX_SUBDIR_LEN);
				} else {
					subdir = "NoAuthor";
				}
				if (sdd.addAuthorsFC != null) sdd.addAuthorsFC.folderLabel.setText(subdir);
				int flags = (int) fl;
				fileOrDir.flags = flags;
				if ((flags != 0) && (bd instanceof SaveDocDialog)) {
					sdd.bAddControls = true;
					sdd.mChosenRate = fileOrDir.getRate();
					sdd.mChosenState = fileOrDir.getReadingState();
					sdd.paintButtons();
					if (sdd.mIvExpand != null)
						sdd.expandClick(sdd.mIvExpand);
				}
			});
		});
	}

	public void saveFile(String toFileName, String toDir, Uri uri) {
		if (!(
			(uri != null) ||
			((mIstream != null) && (mIsHTML)) ||
			((mIstreamTxt != null) && (!mIsHTML))
		)) {
			BackgroundThread.instance().postGUI(() ->
			{
				mActivity.showToast(R.string.cannot_save_book);
			}, 500);
			return;
		}
		FileInfo downloadDir = StrUtils.isEmptyStr(toDir) ?
				Services.getScanner().getDownloadDirectory() : new FileInfo(toDir);
		File resultDir;
		if (addAuthorsFolder) {
			downloadDir = new FileInfo(Services.getScanner().getDownloadDirectory().getPathName() + "/tmp/");
			final File fPath = new File(downloadDir.getPathName());
			if (!fPath.exists()) fPath.mkdir();
			resultDir = new File(downloadDir.getPathName());
		} else
			resultDir = new File(downloadDir.getPathName());
		String sResult = resultDir.getAbsolutePath() + "/" + toFileName;
		File result = new File(sResult);
		if (result.exists()) {
			if (addAuthorsFolder) {
				if (!result.delete()) {
					BackgroundThread.instance().postGUI(() -> {
						mActivity.showToast(R.string.cannot_delete_tmp_book);
					}, 500);
				} else
					doSave(uri, sResult, result, toFileName, toDir);
			} else
				mActivity.askConfirmation(R.string.replace_existing_book, () -> {
					if (result.delete()) {
						doSave(uri, sResult, result, toFileName, toDir);
					} else {
						BackgroundThread.instance().postGUI(() ->
						{
							mActivity.showToast(R.string.cannot_move_book);
						}, 500);
					}
				});
		} else
			doSave(uri, sResult, result, toFileName, toDir);
	}

	private void doSave(Uri uri, String sResult, File result,
						String toFileName, String toDir) {
		BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(result);
				BufferedOutputStream out = new BufferedOutputStream(fos);
				InputStream in = null;
				if (uri != null) in = this.mActivity.getApplicationContext().getContentResolver().openInputStream(uri);
				if ((mIstream != null) && (mIsHTML)) in = mIstream;
				if ((mIstreamTxt != null) && (!mIsHTML)) in = mIstreamTxt;
				Utils.copyStreamContent(out, in);
				out.flush();
				fos.getFD().sync();
				// TODO: work with opendocument in tryToMove...
				if ((mSType.contains("opendocument")) && (!mSType.contains("opendocument.text")) && (!addAuthorsFolder)) {
					DocConvertDialog dlgConv = new DocConvertDialog(mActivity, sResult);
					onPositiveButtonClick();
					dlgConv.show();
					return;
				}
				if (addAuthorsFolder) {
					tryToMoveThenOpen(this, mActivity, result.getAbsolutePath(), result.getParent(),
							toFileName, toDir);
				} else {
					boolean setMarks = ((mChosenRate != 0) || (mChosenState != FileInfo.STATE_NEW));
					if (setMarks)
						doSetMarks(this, mActivity, new File(sResult));
					else {
						onPositiveButtonClick();
						//FileInfo fiResF = new FileInfo(result);
						FileInfo fiResF = FileUtils.getFileProps(null, result, new FileInfo(result.getParent()), true);
						if (StrUtils.isEmptyStr(fiResF.getAuthors())) Services.getEngine().scanBookProperties(fiResF);
						Services.getHistory().getOrCreateBookInfo(mActivity.getDB(), fiResF,
							bookInfo -> {
								BookInfo bif2 = bookInfo;
								if (bif2 == null) bif2 = new BookInfo(fiResF);
								final BookInfo bif = bif2;
								FileInfo dir1 = bif.getFileInfo().parent;
								mActivity.showBookInfo(bif, BookInfoDialog.BOOK_INFO, dir1, null);
							}
						);
					}
				}
			} catch (Exception e) {
				log.e("Error creating file: " + e.getClass().getSimpleName()+": "+e.getLocalizedMessage());
				this.mActivity.showToast("Error creating file: " + e.getClass().getSimpleName()+": "+e.getLocalizedMessage());
			}
		}, 200));
	}

	public static void tryToMoveThenOpen(BaseDialog bd, CoolReader cr,
										 String fromFileName, String fromDir,
										 String toFileName, String toDir) {
		String subdir = "";
		if (bd instanceof SaveDocDialog) {
			SaveDocDialog sdd = (SaveDocDialog) bd;
			if (sdd.addAuthorsFC != null)
				subdir = sdd.addAuthorsFC.folderLabel.getText().toString();
		}
		FileInfo downloadDir = StrUtils.isEmptyStr(toDir) ?
				Services.getScanner().getDownloadDirectory() : new FileInfo(toDir);
		if (StrUtils.isEmptyStr(subdir)) {
			FileInfo fileOrDir = FileUtils.getFileProps(null, new File(fromFileName), new FileInfo(fromDir), true);
			if (StrUtils.isEmptyStr(fileOrDir.getAuthors()))
				Services.getEngine().scanBookProperties(fileOrDir);
			if (!StrUtils.isEmptyStr(fileOrDir.getAuthors())) {
				subdir = Utils.transcribeFileName(fileOrDir.getAuthors());
				if (subdir.length() > FileBrowser.MAX_SUBDIR_LEN)
					subdir = subdir.substring(0, FileBrowser.MAX_SUBDIR_LEN);
			} else {
				subdir = "NoAuthor";
			}
		}
		if (downloadDir == null) {
			BackgroundThread.instance().postGUI(() ->
			{
				cr.showToast(R.string.cannot_move_book);
			}, 500);
		} else {
			boolean setMarks = false;
			SaveDocDialog sdd = null;
			if (bd instanceof SaveDocDialog) {
				sdd = (SaveDocDialog) bd;
				if ((sdd.mChosenRate != 0) || (sdd.mChosenState != FileInfo.STATE_NEW))
					setMarks = true;
			}
			File f = new File(fromFileName);
			String toName = StrUtils.isEmptyStr(toFileName) ? fromFileName : toFileName;
			File resultDir = new File(downloadDir.getPathName());
			boolean addA = true;
			if (bd instanceof SaveDocDialog)
				addA = ((SaveDocDialog) bd).addAuthorsFolder;
			if (addA) {
				resultDir = new File(resultDir, subdir);
				resultDir.mkdirs();
			}
			File result = new File(resultDir.getAbsolutePath() + "/" + toName);
			String fn = f.getName();
			String rdn = resultDir.getAbsolutePath();
			if ((
				(StrUtils.getNonEmptyStr(fn, true).equals(StrUtils.getNonEmptyStr(toName, true)))
				&&
				(StrUtils.getNonEmptyStr(fromDir, true).equals(StrUtils.getNonEmptyStr(rdn, true)))
			) || (StrUtils.isEmptyStr(toDir))) {
				if (setMarks) {
					doSetMarks(sdd, cr, result);
					BackgroundThread.instance().postGUI(() ->
					{
						cr.showToast(R.string.book_is_the_same_marks_set);
					}, 500);
				} else
					BackgroundThread.instance().postGUI(() ->
					{
						cr.showToast(R.string.book_is_the_same);
						FileInfo fiResF = FileUtils.getFileProps(null, result, new FileInfo(result.getParent()), true);
						if (StrUtils.isEmptyStr(fiResF.getAuthors())) Services.getEngine().scanBookProperties(fiResF);
						if (bd != null) bd.onPositiveButtonClick();
						Services.getHistory().getOrCreateBookInfo(cr.getDB(), fiResF,
								bookInfo -> {
									BookInfo bif2 = bookInfo;
									if (bif2 == null) bif2 = new BookInfo(fiResF);
									final BookInfo bif = bif2;
									FileInfo dir1 = bif.getFileInfo().parent;
									cr.showBookInfo(bif, BookInfoDialog.BOOK_INFO, dir1, null);
								}
						);
					}, 500);
			} else {
				boolean finalSetMarks = setMarks;
				if (result.exists()) {
					cr.askConfirmation(R.string.replace_existing_book, () -> {
						if (result.delete()) {
							doRename(bd, cr, downloadDir, f, result, finalSetMarks);
						} else {
							BackgroundThread.instance().postGUI(() ->
							{
								cr.showToast(R.string.cannot_move_book);
							}, 500);
						}
					});
				} else
					doRename(bd, cr, downloadDir, f, result, finalSetMarks);
			}
		}
	}

	private static void doRename(BaseDialog bd, CoolReader cr,
								 FileInfo downloadDir, File f, File result, boolean setMarks) {
		FileInfo fiOldF = FileUtils.getFileProps(null, f, new FileInfo(f.getParent()), true);
		if (StrUtils.isEmptyStr(fiOldF.getAuthors())) Services.getEngine().scanBookProperties(fiOldF);
		cr.waitForCRDBService(() -> {
			cr.getDB().getBookFlags(fiOldF, fl -> {
				int flags = (int) fl;
				fiOldF.flags = flags;
				boolean copied = f.renameTo(result);
				if (!copied)
					copied = Utils.moveFile(f,result);
				if (copied) {
					downloadDir.findItemByPathName(result.getAbsolutePath());
					final File resF = result;
					SaveDocDialog sdd = null;
					if (bd instanceof SaveDocDialog) sdd = (SaveDocDialog) bd;
					if (setMarks) {
						doSetMarks(sdd, cr, result);
					} else {
						FileInfo fiResF = FileUtils.getFileProps(null, resF, new FileInfo(resF.getParent()), true);
						if (StrUtils.isEmptyStr(fiResF.getAuthors())) Services.getEngine().scanBookProperties(fiResF);
						fiResF.flags = flags;
						if (bd != null) bd.onPositiveButtonClick();
						cr.getDB().moveBookToFolder(fiOldF, result.getParent(), true, o -> {
							if ((boolean) o)
								Services.getHistory().getOrCreateBookInfo(cr.getDB(), fiResF,
										bookInfo -> {
											BookInfo bif2 = bookInfo;
											if (bif2 == null) bif2 = new BookInfo(fiResF);
											final BookInfo bif = bif2;
											FileInfo dir1 = bif.getFileInfo().parent;
											bif.getFileInfo().setFlag(flags, false);
											cr.showToast(cr.getString(R.string.book_moved) + ": " + result.getAbsolutePath());
											cr.showBookInfo(bif, BookInfoDialog.BOOK_INFO, dir1, null);
										}
								);
							else
								BackgroundThread.instance().postGUI(() -> {
									cr.showToast(R.string.cannot_move_book);
								}, 500);
						});
					}
				} else {
					BackgroundThread.instance().postGUI(() ->
					{
						cr.showToast(R.string.cannot_move_book);
					}, 500);
				}
			});
		});
	}

	private static void doSetMarks(SaveDocDialog sdd, CoolReader cr, File result) {
		FileInfo item = FileUtils.getFileProps(null, result, new FileInfo(result.getParent()), true);
		if (StrUtils.isEmptyStr(item.getAuthors())) Services.getEngine().scanBookProperties(item);
		SaveDocDialog finalSdd = sdd;
		Services.getHistory().getOrCreateBookInfo(cr.getDB(), item,
			bookInfo -> {
				BookInfo bif2 = bookInfo;
				if (bif2 == null) bif2 = new BookInfo(item);
				final BookInfo bif = bif2;
				bif.getFileInfo().setReadingState(finalSdd.mChosenState);
				if (finalSdd.mChosenRate != 0) {
					bif.getFileInfo().setRate(finalSdd.mChosenRate);
				}
				cr.getDB().saveBookInfo(bif);
				cr.getDB().flush();
				FileInfo dir1 = bif.getFileInfo().parent;
				if (dir1 == null) {
					dir1 = Services.getScanner().findParent(bif.getFileInfo(), dir1);
					bif.getFileInfo().parent = dir1;
				}
				if (dir1 != null) {
					final FileInfo dir2 = dir1;
					bif.getFileInfo().setFileProperties(bif.getFileInfo());
					dir2.setFile(bif.getFileInfo());
					cr.directoryUpdated(dir2, bif.getFileInfo());
				}
				if (sdd != null) sdd.onPositiveButtonClick();
				cr.showBookInfo(bif, BookInfoDialog.BOOK_INFO, new FileInfo(result.getParent()), null);
			}
		);
	}

}
