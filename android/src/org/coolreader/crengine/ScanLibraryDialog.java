package org.coolreader.crengine;

import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CRDBService;

import java.io.File;
import java.util.ArrayList;

public class ScanLibraryDialog extends BaseDialog {

	public static ProgressDialog progressDlg;

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final TableLayout tlScanLibrary;
	private final LinearLayout llScanLibrary;
	private final Button mBtnScanWholeDevice;
	private final Button mBtnScanInternal;
	private final Button mBtnScanExternal;
	private final Button mBtnScanBooks;
	private final Button mBtnScanFav;
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
		if (toScan.size() == 0) {
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
					final ProgressDialog dlg = ProgressDialog.show(mCoolReader,
							pathText + " - " + mCoolReader.getString(R.string.dlg_scan_title),
							mCoolReader.getString(R.string.dlg_scan_message),
							true, true, dialog -> {
								Log.i("SCANDLG", "scanDirectoryRecursive started (all) : stop handler");
								control.stop();
								BackgroundThread.instance().postGUI(() -> {
									mCoolReader.showToast(R.string.cancelled_op);
								});
							});
					Services.getScanner().scanDirectory(mCoolReader.getDB(), new FileInfo(s), () -> {
						Log.i("SCANDLG","scanDirectoryRecursive (all) : finish handler");
						if (dlg.isShowing())
							dlg.dismiss();
						scanFromStack();
					}, true, control);
				} else scanFromStack();
			});
		}
	}

	public ScanLibraryDialog(CoolReader activity)
	{
		super("ScanLibraryDialog", activity, activity.getString( R.string.litres_main), true, false);
		mCoolReader = activity;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGrayC = a.getColor(1, Color.GRAY);
		a.recycle();
		setTitle(mCoolReader.getString(R.string.scan_library));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.scan_library_dialog, null);
		llScanLibrary = view.findViewById(R.id.ll_scan_library_dlg);
		tlScanLibrary = view.findViewById(R.id.tl_scan_library_dlg);
		mBtnScanWholeDevice = view.findViewById(R.id.btn_scan_whole_device);
		mBtnScanInternal = view.findViewById(R.id.btn_scan_internal);
		mBtnScanExternal = view.findViewById(R.id.btn_scan_external);
		mBtnScanBooks = view.findViewById(R.id.btn_scan_books);
		mBtnScanFav = view.findViewById(R.id.btn_scan_fav);
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
