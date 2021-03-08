package org.coolreader.crengine;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.view.MotionEvent;
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
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.CloudSync;
import org.coolreader.db.CRDBService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ScanLibraryDialog extends BaseDialog {

	public static ProgressDialog progressDlg;

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final TableLayout tlScanLibrary;
	private final LinearLayout llScanLibrary;
	private final Button mBtnScanWholeDevice;
	private final LinearLayout mLlScanInternal;
	private final Button mBtnScanInternal;
	private final LinearLayout mLlScanExternal;
	private final Button mBtnScanExternal;
	private final LinearLayout mLlScanBooks;
	private final Button mBtnScanBooks;
	private final LinearLayout mLlScanFav;
	private final Button mBtnScanFav;
	private final LinearLayout mLlScanDownl;
	private final Button mBtnScanDownl;
	private final Button mBtnDoScan;
	private final Button mLibraryMaintenance;
	private boolean bScanWholeDevice = false;
	private boolean bScanInternal = false;
	private boolean bScanExternal = false;
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

	public boolean needInterrupt = false;
	public boolean finished = false;

	private void buttonPressed(Button btn) {
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast});
		colorGrayC = a.getColor(0, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		mCoolReader.tintViewIcons(mBtnScanWholeDevice, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnScanInternal, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnScanExternal, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnScanBooks, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnScanFav, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnScanDownl, PorterDuff.Mode.CLEAR,true);

		if (btn == mBtnScanWholeDevice) {
			if (bScanWholeDevice) {
				bScanWholeDevice = false;
				bScanInternal = false;
				bScanExternal = false;
				bScanBooks = true;
				bScanFav = false;
				bScanDownl = false;
			} else {
				bScanWholeDevice = true;
				bScanInternal = true;
				bScanExternal = true;
				bScanBooks = true;
				bScanFav = true;
				bScanDownl = true;
			}
		}

		if (btn == mBtnScanInternal) {
			if (bScanInternal) {
				bScanWholeDevice = false;
				bScanInternal = false;
			} else {
				bScanInternal = true;
				bScanBooks = true;
				bScanDownl = true;
				bScanWholeDevice = bScanInternal & bScanExternal;
				bScanBooks = bScanBooks || bScanInternal;
				bScanFav = bScanFav || (bScanInternal & bScanExternal);
				bScanDownl = bScanDownl || bScanInternal;
			}
		}

		if (btn == mBtnScanExternal) {
			if (bScanExternal) {
				bScanWholeDevice = false;
				bScanExternal = false;
			} else {
				bScanExternal = true;
				bScanWholeDevice = bScanInternal & bScanExternal;
				bScanFav = bScanFav || (bScanInternal & bScanExternal);
				bScanDownl = bScanDownl || bScanInternal;
			}
		}

		if (btn == mBtnScanBooks) {
			if (bScanBooks) {
				bScanBooks = false;
				bScanInternal = false;
				bScanWholeDevice = false;
			} else {
				bScanBooks = true;
				bScanWholeDevice = bScanInternal & bScanExternal;
				bScanFav = bScanFav || (bScanInternal & bScanExternal);
				bScanDownl = bScanDownl || bScanInternal;
			}
		}

		if (btn == mBtnScanFav) {
			if (bScanFav) {
				bScanFav = false;
				bScanInternal = false;
				bScanWholeDevice = false;
				bScanExternal = false;
			} else {
				bScanFav = true;
			}
		}

		if (btn == mBtnScanDownl) {
			if (bScanDownl) {
				bScanDownl = false;
				bScanInternal = false;
				bScanWholeDevice = false;
			} else {
				bScanDownl = true;
				bScanWholeDevice = bScanInternal & bScanExternal;
			}
		}

		mBtnScanWholeDevice.setBackgroundColor(colorGrayCT);
		mBtnScanInternal.setBackgroundColor(colorGrayCT);
		mBtnScanExternal.setBackgroundColor(colorGrayCT);
		mBtnScanBooks.setBackgroundColor(colorGrayCT);
		mBtnScanFav.setBackgroundColor(colorGrayCT);
		mBtnScanDownl.setBackgroundColor(colorGrayCT);

		if (bScanWholeDevice) {
			mBtnScanWholeDevice.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnScanWholeDevice,true);
		}
		if (bScanInternal) {
			mBtnScanInternal.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnScanInternal,true);
		}
		if (bScanExternal) {
			mBtnScanExternal.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnScanExternal,true);
		}
		if (bScanBooks) {
			mBtnScanBooks.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnScanBooks,true);
		}
		if (bScanFav) {
			mBtnScanFav.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnScanFav,true);
		}
		if (bScanDownl) {
			mBtnScanDownl.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnScanDownl,true);
		}
	}

	private void scanFromStack() {
		if ((toScan.size() == 0) || (needInterrupt)) {
			mCoolReader.getDB().getLibraryStats(o -> {
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
					mCoolReader.showToast(mCoolReader.getString(R.string.db_maint_finished_cnt) + " " +
							mCoolReader.getString(R.string.cnt_entries) + " " + entries + "; " +
							mCoolReader.getString(R.string.cnt_authors) + " " + authors + "; " +
							mCoolReader.getString(R.string.cnt_series) + " " + series + "; " +
							mCoolReader.getString(R.string.cnt_genres) + " " + genres
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
			BackgroundThread.instance().postGUI(() -> {
				File f = new File(s);
				if (f.exists() && f.isDirectory()) {
					String pathText = s;
					String[] arrLab = pathText.split("/");
					if (arrLab.length>2) pathText="../"+arrLab[arrLab.length-2]+"/"+arrLab[arrLab.length-1];
					Log.i("SCANDLG", "scanDirectoryRecursive started (all)");
					final Scanner.ScanControl control = new Scanner.ScanControl();
//					final ProgressDialog dlg = ProgressDialog.show(mCoolReader,
//							pathText + " - " + mCoolReader.getString(R.string.dlg_scan_title),
//							mCoolReader.getString(R.string.dlg_scan_message),
//							true, true, dialog -> {
//								Log.i("SCANDLG", "scanDirectoryRecursive started (all) : stop handler");
//								control.stop();
//								BackgroundThread.instance().postGUI(() -> {
//									mCoolReader.showToast(R.string.cancelled_op);
//								});
//							});
					Services.getScanner().scanDirectory(mCoolReader.getDB(), new FileInfo(s), () -> {
						Log.i("SCANDLG","scanDirectoryRecursive (all) : finish handler");
//						if (dlg.isShowing())
//							dlg.dismiss();
						scanFromStack();
					}, false, control, true);
					if (windowCenterPopup != null)
						if (windowCenterPopup.isShowing()) {
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
							//mScanText.setText(mScanText.getText().toString().replace(s + "\n", ""));
						}
				} else scanFromStack();
			});
		}
	}

	private void addTextToLL(String txt, LinearLayout ll) {
		Button pathButton = new Button(mCoolReader);
		pathButton.setText(txt);
		Properties props = new Properties(mCoolReader.settings());
		int newTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
		pathButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
		//pathButton.setHeight(pathButton.getHeight()-4);
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
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
			if (fi.getType() == FileInfo.TYPE_FS_ROOT) {
				if (StrUtils.getNonEmptyStr(fi.title, true).toUpperCase().contains("EXT SD")) {
					addTextToLL(fi.pathname, mLlScanExternal);
				} else {
					addTextToLL(fi.pathname, mLlScanInternal);
				}
			} else {
				if (fi.getType() == FileInfo.TYPE_DOWNLOAD_DIR) {
					addTextToLL(fi.pathname, mLlScanBooks);
				} else
					addTextToLL(fi.pathname, mLlScanFav);
			}
		}
		addTextToLL(Environment.getExternalStorageDirectory().toString()+ File.separator + Environment.DIRECTORY_DOWNLOADS, mLlScanDownl);
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
		boolean isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		HashMap<Integer, Integer> themeColors = Utils.getThemeColors(mCoolReader, isEInk);
		BackgroundThread.instance().executeGUI(() -> {
			int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
			int colorGray = themeColors.get(R.attr.colorThemeGray2);
			int colorIcon = themeColors.get(R.attr.colorIcon);
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
			Display d = mCoolReader.getWindowManager().getDefaultDisplay();
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
				if (mBtnInterrupt != null) {
					mBtnInterrupt.setBackgroundColor(colorGray);
					mCoolReader.tintViewIcons(toast_ll, true);
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
			if (mCoolReader.getReaderView() != null)
				if (mCoolReader.getReaderView().getSurface() != null) {
					sv.setMaxHeight(mCoolReader.getReaderView().getSurface().getHeight() * 3 / 4);
				}
			windowCenterPopup.showAtLocation(mDialog, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
		});
	}

	public ScanLibraryDialog(CoolReader activity)
	{
		super("ScanLibraryDialog", activity, activity.getString( R.string.scan_library), true, false);
		mCoolReader = activity;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGrayC = a.getColor(1, Color.GRAY);
		a.recycle();
		setTitle(mCoolReader.getString(R.string.scan_library));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.scan_library_dialog, null);
		mDialog = (ViewGroup) view;
		llScanLibrary = view.findViewById(R.id.ll_scan_library_dlg);
		tlScanLibrary = view.findViewById(R.id.tl_scan_library_dlg);
		mBtnScanWholeDevice = view.findViewById(R.id.btn_scan_whole_device);
		mLlScanInternal = view.findViewById(R.id.ll_scan_internal);
		mBtnScanInternal = view.findViewById(R.id.btn_scan_internal);
		mLlScanExternal = view.findViewById(R.id.ll_scan_external);
		mBtnScanExternal = view.findViewById(R.id.btn_scan_external);
		mLlScanBooks = view.findViewById(R.id.ll_scan_books);
		mBtnScanBooks = view.findViewById(R.id.btn_scan_books);
		mLlScanFav = view.findViewById(R.id.ll_scan_fav);
		mBtnScanFav = view.findViewById(R.id.btn_scan_fav);
		mLlScanDownl = view.findViewById(R.id.ll_scan_downl);
		mBtnScanDownl = view.findViewById(R.id.btn_scan_downl);
		mBtnDoScan = view.findViewById(R.id.btn_do_scan);
		mBtnScanWholeDevice.setBackgroundColor(colorGrayC);
		mBtnScanInternal.setBackgroundColor(colorGrayC);
		mBtnScanExternal.setBackgroundColor(colorGrayC);
		mBtnScanBooks.setBackgroundColor(colorGrayC);
		mBtnScanFav.setBackgroundColor(colorGrayC);
		mBtnScanDownl.setBackgroundColor(colorGrayC);
		mBtnDoScan.setBackgroundColor(colorGrayC);
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		Drawable img3 = img.getConstantState().newDrawable().mutate();
		Drawable img4 = img.getConstantState().newDrawable().mutate();
		Drawable img5 = img.getConstantState().newDrawable().mutate();
		Drawable img6 = img.getConstantState().newDrawable().mutate();
		mBtnScanWholeDevice.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		mBtnScanInternal.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		mBtnScanExternal.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
		mBtnScanBooks.setCompoundDrawablesWithIntrinsicBounds(img4, null, null, null);
		mBtnScanFav.setCompoundDrawablesWithIntrinsicBounds(img5, null, null, null);
		mBtnScanDownl.setCompoundDrawablesWithIntrinsicBounds(img6, null, null, null);
		mBtnScanWholeDevice.setOnClickListener(v -> {
			buttonPressed(mBtnScanWholeDevice);
		});
		mBtnScanInternal.setOnClickListener(v -> {
			buttonPressed(mBtnScanInternal);
		});
		mBtnScanExternal.setOnClickListener(v -> {
			buttonPressed(mBtnScanExternal);
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
		buttonPressed(mBtnScanBooks);
		mLibraryMaintenance = view.findViewById(R.id.btn_library_maintenance_button);
		mLibraryMaintenance.setBackgroundColor(colorGrayC);

		mBtnDoScan.setOnClickListener(v -> {
			mBtnDoScan.setText(R.string.do_scan_starting);
			BackgroundThread.instance().postGUI(() -> {
				mBtnDoScan.setText(R.string.do_scan);
			}, 5000);
			ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
			for (FileInfo fi: folders) {
				if (fi.getType() == FileInfo.TYPE_FS_ROOT) {
					if (StrUtils.getNonEmptyStr(fi.title, true).toUpperCase().contains("EXT SD")) {
						if (bScanExternal) toScan.add(fi.pathname + "/");
					} else {
						if (bScanInternal) toScan.add(fi.pathname + "/");
					}
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
				mCoolReader.showToast(R.string.library_maint_list_is_empty);
				return;
			}
			updateToScanFolders();
			finished = false;
			needInterrupt = false;
			showFoldersPopup();
			mCoolReader.getDB().getLibraryStats(o -> {
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
					if (StrUtils.getNonEmptyStr(fi.title, true).toUpperCase().contains("EXT SD")) {
						if (bScanExternal) toRemove.add(fi.pathname + "/");
					} else {
						if (bScanInternal) toRemove.add(fi.pathname + "/");
					}
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
				mCoolReader.showToast(R.string.library_maint_list_is_empty);
				return;
			}
			if (bScanWholeDevice) {
				mCoolReader.showToast(R.string.whole_device_cannot_proceed);
				return;
			}
			mCoolReader.askConfirmation(R.string.are_you_sure, () -> {
				progressDlg = ProgressDialog.show(mCoolReader,
						mCoolReader.getString(R.string.long_op),
						mCoolReader.getString(R.string.long_op),
						true, true, null);
				mCoolReader.getDB().deleteBookEntries(toRemove, o -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					if (o != null) {
						Long cnt = (Long) o;
						BackgroundThread.instance().postGUI(() -> {
							if (cnt == 0L) {
								mCoolReader.showToast(mCoolReader.getString(R.string.db_maint_finished_cnt) + " " + cnt);
							} else {
								mCoolReader.showToast(mCoolReader.getString(R.string.db_maint_finished_cnt) + " " + cnt + ". " +
										mCoolReader.getString(R.string.library_maint_done_need_restart));
								mCoolReader.askConfirmation(mCoolReader.getString(R.string.proceeded) + ": " + cnt + ". " +
										mCoolReader.getString(R.string.restart_app), () -> {
									mCoolReader.finish();
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

		mBtnMaintRemoveOrphans = view2.findViewById(R.id.btn_maint_remove_orphans);
		mBtnMaintRemoveOrphans.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnMaintRemoveOrphans);
		mBtnMaintRemoveOrphans.setOnClickListener(v -> {
			mCoolReader.askConfirmation(R.string.are_you_sure, () -> {
				progressDlg = ProgressDialog.show(mCoolReader,
						mCoolReader.getString(R.string.long_op),
						mCoolReader.getString(R.string.long_op),
						true, true, null);
				mCoolReader.getDB().deleteOrphanEntries(o -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					if (o != null) {
						Long cnt = (Long) o;
						BackgroundThread.instance().postGUI(() -> {
							if (cnt == 0L) {
								mCoolReader.showToast(mCoolReader.getString(R.string.db_maint_finished_cnt) + " " + cnt);
							} else {
								mCoolReader.showToast(mCoolReader.getString(R.string.db_maint_finished_cnt) + " " + cnt + ". " +
										mCoolReader.getString(R.string.library_maint_done_need_restart));
								mCoolReader.askConfirmation(mCoolReader.getString(R.string.proceeded) + ": " + cnt + ". " +
										mCoolReader.getString(R.string.restart_app), () -> {
									mCoolReader.finish();
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
		mBtnMaintRemoveCloud.setOnClickListener(v -> {
			mCoolReader.askConfirmation(R.string.are_you_sure, () -> {
				progressDlg = ProgressDialog.show(mCoolReader,
						mCoolReader.getString(R.string.long_op),
						mCoolReader.getString(R.string.long_op),
						true, true, null);
				mCoolReader.getDB().deleteCloudEntries(o -> {
					if (progressDlg != null)
						if (progressDlg.isShowing()) progressDlg.dismiss();
					if (o != null) {
						Long cnt = (Long) o;
						BackgroundThread.instance().postGUI(() -> {
							if (cnt == 0L) {
								mCoolReader.showToast(mCoolReader.getString(R.string.db_maint_finished_cnt) + " " + cnt);
							} else {
								mCoolReader.showToast(mCoolReader.getString(R.string.db_maint_finished_cnt) + " " + cnt + ". " +
										mCoolReader.getString(R.string.library_maint_done_need_restart));
								mCoolReader.askConfirmation(mCoolReader.getString(R.string.proceeded) + ": " + cnt + ". " +
										mCoolReader.getString(R.string.restart_app), () -> {
									mCoolReader.finish();
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
			Utils.hideView(mLibraryMaintenance);
		});

		addFoldersNames();

		setView( view );
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
