package org.coolreader.crengine;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ScanLibraryDialog extends BaseDialog {

	public static ProgressDialog progressDlg;

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	private final CoolReader mActivity;
	private final LayoutInflater mInflater;
	private final TableLayout tlScanLibrary;
	private final LinearLayout llScanLibrary;
	private final Button mBtnScanWholeDevice;
	private final LinearLayout mLlScanCards;
	private final ArrayList<Button> cardButtons = new ArrayList<>();
	private final HashMap<String, Boolean> cardButtonsSelected = new HashMap<>();
	private final LinearLayout mLlScanBooks;
	private final Button mBtnScanBooks;
	private String mBtnScanBooksPath = "";
	private final LinearLayout mLlScanFav;
	private final Button mBtnScanFav;
	private String mBtnScanFavPath = "";
	private final LinearLayout mLlScanDownl;
	private final Button mBtnScanDownl;
	private String mBtnScanDownlPath = "";
	private final Button mBtnDoScan;
	private final Button mLibraryMaintenance;
	private boolean bScanWholeDevice = false;
	private boolean bScanBooks = false;
	private boolean bScanFav = false;
	private boolean bScanDownl = false;
	private final Button mBtnMaintRemove;
	private final Button mBtnMaintRemoveOrphans;
	private final Button mBtnMaintRemoveCloud;
	private final Button mBtnMaintShowStatistics;
	ArrayList<String> toScan = new ArrayList<>();
	LibraryStats cntWas;
	private final ViewGroup mDialog;
	private TextView mScanText;
	private TextView mBooks;
	private TextView mAuthors;
	private TextView mSeries;
	private TextView mGenres;
	private TextView mBooksCnt;
	private TextView mAuthorsCnt;
	private TextView mSeriesCnt;
	private TextView mGenresCnt;
	private Button mBtnInterrupt;
	private TextView mPercent;
	private int initialSize;

	public boolean needInterrupt = false;
	public boolean finished = false;

	private boolean isAllCardSelected() {
		for (Button btnCard: cardButtons)
			if (!cardButtonsSelected.get(btnCard.getContentDescription().toString())) return false;
		return true;
	}

	private boolean isPathOnCards(String path) {
		for (Button btnCard: cardButtons)
			if (cardButtonsSelected.get(btnCard.getContentDescription().toString())) {
				if (path.startsWith(btnCard.getContentDescription().toString())) return true;
			}
		return false;
	}

	private void disableCardsForPath(String path) {
		for (Button btnCard: cardButtons)
			if (cardButtonsSelected.get(btnCard.getContentDescription().toString())) {
				if (path.startsWith(btnCard.getContentDescription().toString()))
					cardButtonsSelected.put(btnCard.getContentDescription().toString(), false);
			}
	}

	private void disableAllCards() {
		for (Button btnCard: cardButtons)
			cardButtonsSelected.put(btnCard.getContentDescription().toString(), false);
	}

	private void buttonPressed(Button btn) {
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		if (isEInk) colorGrayCT = Color.WHITE;
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		mActivity.tintViewIcons(mBtnScanWholeDevice, PorterDuff.Mode.CLEAR,true);
		for (Button btnCard: cardButtons) {
			mActivity.tintViewIcons(btnCard, PorterDuff.Mode.CLEAR,true);
		}
		mActivity.tintViewIcons(mBtnScanBooks, PorterDuff.Mode.CLEAR,true);
		mActivity.tintViewIcons(mBtnScanFav, PorterDuff.Mode.CLEAR,true);
		mActivity.tintViewIcons(mBtnScanDownl, PorterDuff.Mode.CLEAR,true);

		if (btn == mBtnScanWholeDevice) {
			if (bScanWholeDevice) {
				bScanWholeDevice = false;
				for (Button btnCard: cardButtons) {
					cardButtonsSelected.put(btnCard.getContentDescription().toString(), false);
				}
				bScanBooks = true;
				bScanFav = false;
				bScanDownl = false;
			} else {
				bScanWholeDevice = true;
				for (Button btnCard: cardButtons) {
					cardButtonsSelected.put(btnCard.getContentDescription().toString(), true);
				}
				bScanBooks = true;
				bScanFav = true;
				bScanDownl = true;
			}
		}

		for (Button btnCard: cardButtons) {
			if (btn == btnCard) {
				String cardPath = btnCard.getContentDescription().toString();
				boolean curV = cardButtonsSelected.get(cardPath);
				cardButtonsSelected.put(cardPath, !curV);
				if (curV) {
					bScanWholeDevice = false;
				} else {
					bScanBooks = isPathOnCards(mBtnScanBooksPath);
					bScanDownl = isPathOnCards(mBtnScanDownlPath);
					bScanWholeDevice = isAllCardSelected();
					bScanFav = isAllCardSelected();
				}
			}
		}

		if (btn == mBtnScanBooks) {
			if (bScanBooks) {
				bScanBooks = false;
				disableCardsForPath(mBtnScanBooksPath);
				bScanWholeDevice = false;
			} else {
				bScanBooks = true;
				bScanWholeDevice = isAllCardSelected();
				bScanFav = bScanFav || (isAllCardSelected());
				bScanDownl = bScanDownl || isPathOnCards(mBtnScanDownlPath);
			}
		}

		if (btn == mBtnScanFav) {
			if (bScanFav) {
				bScanFav = false;
				disableAllCards();
				bScanWholeDevice = false;
			} else {
				bScanFav = true;
			}
		}

		if (btn == mBtnScanDownl) {
			if (bScanDownl) {
				bScanDownl = false;
				disableCardsForPath(mBtnScanDownlPath);
				bScanWholeDevice = false;
			} else {
				bScanDownl = true;
				bScanWholeDevice = isAllCardSelected();
			}
		}

		mBtnScanWholeDevice.setBackgroundColor(colorGrayCT);
		for (Button btnCard: cardButtons) btnCard.setBackgroundColor(colorGrayCT);
		mBtnScanBooks.setBackgroundColor(colorGrayCT);
		mBtnScanFav.setBackgroundColor(colorGrayCT);
		mBtnScanDownl.setBackgroundColor(colorGrayCT);

		if (bScanWholeDevice) {
			mBtnScanWholeDevice.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(mBtnScanWholeDevice,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnScanWholeDevice);
		}
		for (Button btnCard: cardButtons) {
			if (cardButtonsSelected.get(btnCard.getContentDescription().toString())) {
				btnCard.setBackgroundColor(colorGrayCT2);
				mActivity.tintViewIcons(btnCard,true);
				if (isEInk) Utils.setSolidButtonEink(btnCard);
			}
		}
		if (bScanBooks) {
			mBtnScanBooks.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(mBtnScanBooks,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnScanBooks);
		}
		if (bScanFav) {
			mBtnScanFav.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(mBtnScanFav,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnScanFav);
		}
		if (bScanDownl) {
			mBtnScanDownl.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(mBtnScanDownl,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnScanDownl);
		}
	}

	private void scanFromStack() {
		if ((toScan.size() == 0) || (needInterrupt)) {
			mActivity.getDB().getLibraryStats(o -> {
				if (progressDlg != null)
					if (progressDlg.isShowing()) progressDlg.dismiss();
				if (o != null) {
					LibraryStats cntBecome = (LibraryStats) o;
					String s = "[undefined]";
					Long authors = 0L;
					Long genres = 0L;
					Long entries = 0L;
					Long series = 0L;
					if ((cntWas != null) && (cntBecome != null)) {
						authors = cntBecome.authorsCnt = cntBecome.authorsCnt - cntWas.authorsCnt;
						genres = cntBecome.genresCnt = cntBecome.genresCnt - cntWas.genresCnt;
						entries = cntBecome.entriesCnt = cntBecome.entriesCnt - cntWas.entriesCnt;
						series = cntBecome.seriesCnt = cntBecome.seriesCnt - cntWas.seriesCnt;
					}
					mActivity.showToast(mActivity.getString(R.string.db_maint_finished_cnt) + " " +
							mActivity.getString(R.string.cnt_entries) + " " + entries + "; " +
							mActivity.getString(R.string.cnt_authors) + " " + authors + "; " +
							mActivity.getString(R.string.cnt_series) + " " + series + "; " +
							mActivity.getString(R.string.cnt_genres) + " " + genres
							);
					if (windowCenterPopup != null)
						if (windowCenterPopup.isShowing()) {
							mBooks.setText(R.string.entries);
							mAuthors.setText(R.string.authors);
							mGenres.setText(R.string.genres);
							mSeries.setText(R.string.series);
							mBooksCnt.setText("" + entries);
							mAuthorsCnt.setText("" + authors);
							mGenresCnt.setText("" + genres);
							mSeriesCnt.setText("" + series);
							if (!needInterrupt)
								mScanText.setText("");
							mBtnInterrupt.setText(R.string.close);
						}
					finished = true;
				}
			});
		} else {
			String s = toScan.get(0);
			toScan.remove(0);
			int curSize = toScan.size();
			BackgroundThread.instance().postGUI(() -> {
				File f = new File(s);
				if (f.exists() && f.isDirectory()) {
					String pathText = s;
					String[] arrLab = pathText.split("/");
					if (arrLab.length>2) pathText="../"+arrLab[arrLab.length-2]+"/"+arrLab[arrLab.length-1];
					Log.i("SCANDLG", "scanDirectoryRecursive started (all)");
					final Scanner.ScanControl control = new Scanner.ScanControl();
					Services.getScanner().scanDirectory(mActivity.getDB(), new FileInfo(s), () -> {
					}, (scanControl) -> {
						Log.i("SCANDLG","scanDirectoryRecursive (all) : finish handler");
						scanFromStack();
					}, false, control, true);
					if (windowCenterPopup != null)
						if (windowCenterPopup.isShowing()) {
							int perc = 0;
							try {
								perc = curSize * 100 / initialSize;
								perc = 100 - perc;
							} catch (Exception e) {
								// do nothing
							}
							//int percent =  toScan.size();
							String ss = "";
							int i = 1;
							for (String s1: toScan) {
								i++;
								if (ss.equals("")) ss = s1;
								else ss = ss + "\n" + s1;
								if (i > 100) {
									ss = ss + "\n...";
									break;
								}
							}
							mScanText.setText(ss);
							mPercent.setText(perc + "%");
						}
				} else scanFromStack();
			});
		}
	}

	private void addTextToLL(String txt, LinearLayout ll) {
		Button pathButton = new Button(mActivity);
		pathButton.setText(txt);
		Properties props = new Properties(mActivity.settings());
		int newTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
		pathButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
		//pathButton.setHeight(pathButton.getHeight()-4);
		TypedArray a = this.mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGrayC = a.getColor(1, Color.GRAY);
		int colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorIconCT=Color.argb(100,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		pathButton.setTextColor(colorIconCT);
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		pathButton.setBackgroundColor(colorGrayCT);
		pathButton.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		pathButton.setPadding(6, 6, 6, 6);
		//pathButton.setBackground(null);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		llp.setMargins(8, 4, 4, 8);
		pathButton.setLayoutParams(llp);
		pathButton.setMaxLines(1);
		pathButton.setEllipsize(TextUtils.TruncateAt.END);
		ll.addView(pathButton);
	}

	private void addFoldersNames() {

		ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
		for (FileInfo fi: folders) {
			if (fi.getType() != FileInfo.TYPE_FS_ROOT) {
				if (fi.getType() == FileInfo.TYPE_DOWNLOAD_DIR) {
					addTextToLL(fi.pathname, mLlScanBooks);
					mBtnScanBooksPath = fi.pathname;
				} else{
					addTextToLL(fi.pathname, mLlScanFav);
					mBtnScanFavPath = fi.pathname;
				}
			}
		}
		addTextToLL(Environment.getExternalStorageDirectory().toString()+ File.separator + Environment.DIRECTORY_DOWNLOADS, mLlScanDownl);
		mBtnScanDownlPath = Environment.getExternalStorageDirectory().toString()+ File.separator + Environment.DIRECTORY_DOWNLOADS;
	}

	private void addFoldersToLL(ViewGroup view) {
		ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
		for (FileInfo fi : folders) {
			if (fi.getType() == FileInfo.TYPE_FS_ROOT) {
				Button cardButton = new Button(mActivity);
				if (StrUtils.getNonEmptyStr(fi.title, true).toUpperCase().contains("EXT SD"))
					cardButton.setText(mActivity.getString(R.string.scan_external_sd)+": "+fi.pathname);
				else
					cardButton.setText(mActivity.getString(R.string.scan_internal_storage)+": "+fi.pathname);
				cardButton.setContentDescription(fi.pathname);
				cardButtonsSelected.put(cardButton.getContentDescription().toString(),false);
				int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
				int colorIcon = themeColors.get(R.attr.colorIcon);
				cardButton.setBackgroundColor(colorGrayC);
				Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
				Drawable img1 = img.getConstantState().newDrawable().mutate();
				cardButton.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
				cardButton.setPadding(10, 20, 10, 20);
				LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.FILL_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				llp.setMargins(8, 8, 8, 8);
				cardButton.setLayoutParams(llp);
				cardButton.setTextColor(this.mActivity.getTextColor(colorIcon));
				//dicButton.setMaxWidth((mReaderView.getRequestedWidth() - 20) / iCntRecent); // This is not needed anymore - since we use FlowLayout
				cardButton.setMaxLines(3);
				cardButton.setEllipsize(TextUtils.TruncateAt.END);
				cardButton.setOnClickListener(v -> {
					buttonPressed(cardButton);
				});
				view.addView(cardButton);
				cardButtons.add(cardButton);
			}
		}
	}

	ArrayList<String> toScanTemp;

	private void updateToScanFoldersInternal(File dir) {
		File[] files = dir.listFiles();
		if (files != null)
			for (File file : files) {
				if (file.isDirectory()) {
					toScanTemp.add(file.getAbsolutePath());
					updateToScanFoldersInternal(file);
				}
			}
	}

	private void updateToScanFolders() {
		toScanTemp = new ArrayList<>();
		for (String s: toScan) {
			File f = new File(s);
			if (f.exists())
				if (f.isDirectory()) {
					toScanTemp.add(f.getAbsolutePath());
					updateToScanFoldersInternal(f);
				}
		}
		toScan.clear();
		for (String s: toScanTemp) {
			if (!toScan.contains(s))
				toScan.add(s);
		}
	}

	PopupWindow windowCenterPopup = null;

	public void showFoldersPopup() {
		BackgroundThread.instance().executeGUI(() -> {
			int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
			int colorGray = themeColors.get(R.attr.colorThemeGray2);
			int fontSize = 24;
			windowCenterPopup = new PopupWindow(this.getContext());
			windowCenterPopup.setWidth(WindowManager.LayoutParams.FILL_PARENT);
			windowCenterPopup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
			windowCenterPopup.setTouchable(true);
			windowCenterPopup.setFocusable(false);
			windowCenterPopup.setOutsideTouchable(false);
			windowCenterPopup.setBackgroundDrawable(null);
			LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			windowCenterPopup.setContentView(inflater.inflate(R.layout.scan_library_toast, null, true));
			Display d = mActivity.getWindowManager().getDefaultDisplay();
			DisplayMetrics m = new DisplayMetrics();
			d.getMetrics(m);
			int screenHeight = m.heightPixels;
			int screenWidth = m.widthPixels;
			int [] location = new int[2];
			mDialog.getLocationOnScreen(location);
			int popupY = location[1] + mDialog.getHeight();
			LinearLayout toast_ll = windowCenterPopup.getContentView().findViewById(R.id.dic_toast_ll);
			if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()))
				toast_ll.setBackgroundColor(Color.argb(100, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC)));
			else {
				toast_ll.setBackgroundColor(colorGrayC);
				mBtnInterrupt = windowCenterPopup.getContentView().findViewById(R.id.btn_interrupt);
				mPercent = windowCenterPopup.getContentView().findViewById(R.id.percent_title);
				if (mBtnInterrupt != null) {
					mBtnInterrupt.setBackgroundColor(colorGray);
					mActivity.tintViewIcons(toast_ll, true);
					//toast_btn.setPadding(6, 6, 6, 6);
					mBtnInterrupt.setOnClickListener((v) -> {
						needInterrupt = true;
						mBtnInterrupt.setText(R.string.interrupting);
						if (finished) windowCenterPopup.dismiss();
					});
				}
			}
			int newTextSize = 16;
			LinearLayout upper_row_ll = windowCenterPopup.getContentView().findViewById(R.id.upper_row_ll);
			mScanText = windowCenterPopup.getContentView().findViewById(R.id.scan_text);
			String ss = "";
			int i = 1;
			for (String s: toScan) {
				i++;
				if (ss.equals("")) ss = s;
					else ss = ss + "\n" + s;
				if (i > 100) {
					ss = ss + "\n...";
					break;
				}
			}
			mScanText.setText(ss);
			mBooks = windowCenterPopup.getContentView().findViewById(R.id.books_title);
			mAuthors = windowCenterPopup.getContentView().findViewById(R.id.authors_title);
			mSeries = windowCenterPopup.getContentView().findViewById(R.id.series_title);
			mGenres = windowCenterPopup.getContentView().findViewById(R.id.genres_title);
			mBooksCnt = windowCenterPopup.getContentView().findViewById(R.id.books_cnt);
			mAuthorsCnt = windowCenterPopup.getContentView().findViewById(R.id.authors_cnt);
			mSeriesCnt = windowCenterPopup.getContentView().findViewById(R.id.series_cnt);
			mGenresCnt = windowCenterPopup.getContentView().findViewById(R.id.genres_cnt);
			mBooks.setText("");
			mAuthors.setText("");
			mSeries.setText("");
			mGenres.setText("");
			mBooksCnt.setText("");
			mAuthorsCnt.setText("");
			mSeriesCnt.setText("");
			mGenresCnt.setText("");
			MaxHeightScrollView sv =  windowCenterPopup.getContentView().findViewById(R.id.dic_scrollV);
			sv.setMaxHeight(d.getHeight() * 3 / 4);
			if (mActivity.getReaderView() != null)
				if (mActivity.getReaderView().getSurface() != null) {
					sv.setMaxHeight(mActivity.getReaderView().getSurface().getHeight() * 3 / 4);
				}
			windowCenterPopup.showAtLocation(mDialog, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
		});
	}

	public ScanLibraryDialog(CoolReader activity)
	{
		super(activity, activity.getString( R.string.scan_library), true, false);
		mActivity = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mActivity, isEInk);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		setTitle(mActivity.getString(R.string.scan_library));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.scan_library_dialog, null);
		mDialog = (ViewGroup) view;
		llScanLibrary = view.findViewById(R.id.ll_scan_library_dlg);
		tlScanLibrary = view.findViewById(R.id.tl_scan_library_dlg);
		mBtnScanWholeDevice = view.findViewById(R.id.btn_scan_whole_device);
		mLlScanCards = view.findViewById(R.id.ll_scan_cards);
		addFoldersToLL(mLlScanCards);
		mLlScanBooks = view.findViewById(R.id.ll_scan_books);
		mBtnScanBooks = view.findViewById(R.id.btn_scan_books);
		mLlScanFav = view.findViewById(R.id.ll_scan_fav);
		mBtnScanFav = view.findViewById(R.id.btn_scan_fav);
		mLlScanDownl = view.findViewById(R.id.ll_scan_downl);
		mBtnScanDownl = view.findViewById(R.id.btn_scan_downl);
		mBtnDoScan = view.findViewById(R.id.btn_do_scan);
		mBtnScanWholeDevice.setBackgroundColor(colorGrayC);
		mBtnScanBooks.setBackgroundColor(colorGrayC);
		mBtnScanFav.setBackgroundColor(colorGrayC);
		mBtnScanDownl.setBackgroundColor(colorGrayC);
		mBtnDoScan.setBackgroundColor(colorGrayC);
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img4 = img.getConstantState().newDrawable().mutate();
		Drawable img5 = img.getConstantState().newDrawable().mutate();
		Drawable img6 = img.getConstantState().newDrawable().mutate();
		mBtnScanWholeDevice.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		mBtnScanBooks.setCompoundDrawablesWithIntrinsicBounds(img4, null, null, null);
		mBtnScanFav.setCompoundDrawablesWithIntrinsicBounds(img5, null, null, null);
		mBtnScanDownl.setCompoundDrawablesWithIntrinsicBounds(img6, null, null, null);
		mBtnScanWholeDevice.setOnClickListener(v -> {
			buttonPressed(mBtnScanWholeDevice);
		});
		mBtnScanBooks.setOnClickListener(v -> {
			buttonPressed(mBtnScanBooks);
		});
		mBtnScanFav.setOnClickListener(v -> {
			buttonPressed(mBtnScanFav);
		});
		mBtnScanDownl.setOnClickListener(v -> {
			buttonPressed(mBtnScanDownl);
		});
		Utils.setDashedButton(mBtnDoScan);
		if (isEInk) Utils.setDashedButtonEink(mBtnDoScan);
		buttonPressed(mBtnScanBooks);
		mLibraryMaintenance = view.findViewById(R.id.btn_library_maintenance_button);
		mLibraryMaintenance.setBackgroundColor(colorGrayC);
		if (isEInk) Utils.setSolidButtonEink(mLibraryMaintenance);

		mBtnDoScan.setOnClickListener(v -> {
			mBtnDoScan.setText(R.string.do_scan_starting);
			BackgroundThread.instance().postGUI(() -> {
				mBtnDoScan.setText(R.string.do_scan);
			}, 5000);
			ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
			for (FileInfo fi: folders) {
				if (fi.getType() == FileInfo.TYPE_FS_ROOT) {
					for (Button btnCard: cardButtons)
						if (cardButtonsSelected.get(btnCard.getContentDescription().toString()))
							if (btnCard.getContentDescription().toString().equals(fi.pathname))
								toScan.add(fi.pathname + "/");
				} else {
					if (fi.getType() == FileInfo.TYPE_DOWNLOAD_DIR) {
						if (bScanBooks) toScan.add(fi.pathname + "/");
					} else
					if (bScanFav) toScan.add(fi.pathname + "/");
				}
			}
			if (bScanDownl)
				toScan.add(Environment.getExternalStorageDirectory().toString()+ File.separator + Environment.DIRECTORY_DOWNLOADS + "/");
			if (toScan.isEmpty()) {
				mActivity.showToast(R.string.library_maint_list_is_empty);
				return;
			}
			updateToScanFolders();
			finished = false;
			needInterrupt = false;
			initialSize = toScan.size();
			showFoldersPopup();
			mActivity.getDB().getLibraryStats(o -> {
				if (progressDlg != null)
					if (progressDlg.isShowing()) progressDlg.dismiss();
				if (o != null) {
					cntWas = (LibraryStats) o;
					scanFromStack();
				}
			});
		});

		View view2 = mInflater.inflate(R.layout.scan_library_dialog_m, null);
		TableLayout tlm = view2.findViewById(R.id.tl_scan_library_dlg_m);
		mBtnMaintRemove = view2.findViewById(R.id.btn_maint_remove);
		mBtnMaintRemove.setOnClickListener(v -> {
			ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
			ArrayList<String> toRemove = new ArrayList<>();
			for (FileInfo fi: folders) {
				if (fi.getType() == FileInfo.TYPE_FS_ROOT) {
					for (Button btnCard: cardButtons)
						if (cardButtonsSelected.get(btnCard.getContentDescription().toString()))
							if (btnCard.getContentDescription().toString().equals(fi.pathname))
								toScan.add(fi.pathname + "/");
				} else {
					if (fi.getType() == FileInfo.TYPE_DOWNLOAD_DIR) {
						if (bScanBooks) toRemove.add(fi.pathname + "/");
					} else
						if (bScanFav) toRemove.add(fi.pathname + "/");
				}
			}
			if (bScanDownl)
				toRemove.add(Environment.getExternalStorageDirectory().toString()+ File.separator + Environment.DIRECTORY_DOWNLOADS + "/");
			if (toRemove.isEmpty()) {
				mActivity.showToast(R.string.library_maint_list_is_empty);
				return;
			}
			if (bScanWholeDevice) {
				mActivity.showToast(R.string.whole_device_cannot_proceed);
				return;
			}
			mActivity.askConfirmation(R.string.are_you_sure, () -> {
				progressDlg = ProgressDialog.show(mActivity,
						mActivity.getString(R.string.long_op),
						mActivity.getString(R.string.long_op),
						true, true, null);
				mActivity.getDB().deleteBookEntries(toRemove, o -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					if (o != null) {
						Long cnt = (Long) o;
						BackgroundThread.instance().postGUI(() -> {
							if (cnt == 0L) {
								mActivity.showToast(mActivity.getString(R.string.db_maint_finished_cnt) + " " + cnt);
							} else {
								mActivity.showToast(mActivity.getString(R.string.db_maint_finished_cnt) + " " + cnt + ". " +
										mActivity.getString(R.string.library_maint_done_need_restart));
								mActivity.askConfirmation(mActivity.getString(R.string.proceeded) + ": " + cnt + ". " +
										mActivity.getString(R.string.restart_app), () -> {
									mActivity.finish();
								});
							}
						});
					}
				});
			});
			//for (String s: toRemove) Log.i("SCANDLG", s);
		});
		mBtnMaintRemove.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnMaintRemove);
		if (isEInk) Utils.setDashedButtonEink(mBtnMaintRemove);

		mBtnMaintRemoveOrphans = view2.findViewById(R.id.btn_maint_remove_orphans);
		mBtnMaintRemoveOrphans.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnMaintRemoveOrphans);
		if (isEInk) Utils.setDashedButtonEink(mBtnMaintRemoveOrphans);
		mBtnMaintRemoveOrphans.setOnClickListener(v -> {
			mActivity.askConfirmation(R.string.are_you_sure, () -> {
				progressDlg = ProgressDialog.show(mActivity,
						mActivity.getString(R.string.long_op),
						mActivity.getString(R.string.long_op),
						true, true, null);
				mActivity.getDB().deleteOrphanEntries(o -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					if (o != null) {
						Long cnt = (Long) o;
						BackgroundThread.instance().postGUI(() -> {
							if (cnt == 0L) {
								mActivity.showToast(mActivity.getString(R.string.db_maint_finished_cnt) + " " + cnt);
							} else {
								mActivity.showToast(mActivity.getString(R.string.db_maint_finished_cnt) + " " + cnt + ". " +
										mActivity.getString(R.string.library_maint_done_need_restart));
								mActivity.askConfirmation(mActivity.getString(R.string.proceeded) + ": " + cnt + ". " +
										mActivity.getString(R.string.restart_app), () -> {
									mActivity.finish();
								});
							}
						});
					}
				});
			});
		});

		mBtnMaintRemoveCloud = view2.findViewById(R.id.btn_maint_remove_cloud);
		mBtnMaintRemoveCloud.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnMaintRemoveCloud);
		if (isEInk) Utils.setDashedButtonEink(mBtnMaintRemoveCloud);
		mBtnMaintRemoveCloud.setOnClickListener(v -> {
			mActivity.askConfirmation(R.string.are_you_sure, () -> {
				progressDlg = ProgressDialog.show(mActivity,
						mActivity.getString(R.string.long_op),
						mActivity.getString(R.string.long_op),
						true, true, null);
				mActivity.getDB().deleteCloudEntries(o -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					if (o != null) {
						Long cnt = (Long) o;
						BackgroundThread.instance().postGUI(() -> {
							if (cnt == 0L) {
								mActivity.showToast(mActivity.getString(R.string.db_maint_finished_cnt) + " " + cnt);
							} else {
								mActivity.showToast(mActivity.getString(R.string.db_maint_finished_cnt) + " " + cnt + ". " +
										mActivity.getString(R.string.library_maint_done_need_restart));
								mActivity.askConfirmation(mActivity.getString(R.string.proceeded) + ": " + cnt + ". " +
										mActivity.getString(R.string.restart_app), () -> {
									mActivity.finish();
								});
							}
						});
					}
				});
			});
		});

		mBtnMaintShowStatistics = view2.findViewById(R.id.btn_maint_show_statistics);
		mBtnMaintShowStatistics.setOnClickListener(v -> {

		});

		mBtnMaintShowStatistics.setBackgroundColor(colorGrayC);
		Utils.hideView(mBtnMaintShowStatistics); // temporarily, while not implemented

		mLibraryMaintenance.setOnClickListener(v -> {
			llScanLibrary.addView(tlm);
			mActivity.tintViewIcons(tlm);
			Utils.hideView(mLibraryMaintenance);
		});

		addFoldersNames();
		setView(view);
	}

	@Override
	public void onPositiveButtonClick() {
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}
}
