package org.coolreader.crengine;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.litres.LitresConfig;
import org.coolreader.cloud.litres.LitresCredentialsDialog;
import org.coolreader.cloud.litres.LitresSearchParams;

public class ScanLibraryDialog extends BaseDialog {

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	final TableLayout tlScanLibrary;
	private final Button mBtnScanWholeDevice;
	private final Button mBtnScanInternal;
	private final Button mBtnScanExternal;
	private final Button mBtnScanBooks;
	private final Button mBtnScanFav;
	private final Button mBtnDoScan;
	private boolean bScanWholeDevice = false;
	private boolean bScanInternal = false;
	private boolean bScanExternal = false;
	private boolean bScanBooks = false;
	private boolean bScanFav = false;

	private void setDashedButton(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_dashed_border);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	}

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

		if (btn == mBtnScanWholeDevice) {
			if (bScanWholeDevice) {
				bScanWholeDevice = false;
				bScanInternal = false;
				bScanExternal = false;
				bScanBooks = true;
				bScanFav = false;
			} else {
				bScanWholeDevice = true;
				bScanInternal = true;
				bScanExternal = true;
				bScanBooks = true;
				bScanFav = true;
			}
		}

		if (btn == mBtnScanInternal) {
			if (bScanInternal) {
				bScanWholeDevice = false;
				bScanInternal = false;
			} else {
				bScanInternal = true;
				bScanBooks = true;
				bScanWholeDevice = bScanInternal & bScanExternal;
				bScanBooks = bScanBooks || bScanInternal;
				bScanFav = bScanFav || (bScanInternal & bScanExternal);
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

		mBtnScanWholeDevice.setBackgroundColor(colorGrayCT);
		mBtnScanInternal.setBackgroundColor(colorGrayCT);
		mBtnScanExternal.setBackgroundColor(colorGrayCT);
		mBtnScanBooks.setBackgroundColor(colorGrayCT);
		mBtnScanFav.setBackgroundColor(colorGrayCT);

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
		tlScanLibrary = view.findViewById(R.id.tl_scan_library_dlg);
		mBtnScanWholeDevice = view.findViewById(R.id.btn_scan_whole_device);
		mBtnScanInternal = view.findViewById(R.id.btn_scan_internal);
		mBtnScanExternal = view.findViewById(R.id.btn_scan_external);
		mBtnScanBooks = view.findViewById(R.id.btn_scan_books);
		mBtnScanFav = view.findViewById(R.id.btn_scan_fav);
		mBtnDoScan = view.findViewById(R.id.btn_do_scan);
		mBtnScanWholeDevice.setBackgroundColor(colorGrayC);
		mBtnScanInternal.setBackgroundColor(colorGrayC);
		mBtnScanExternal.setBackgroundColor(colorGrayC);
		mBtnScanBooks.setBackgroundColor(colorGrayC);
		mBtnScanFav.setBackgroundColor(colorGrayC);
		mBtnDoScan.setBackgroundColor(colorGrayC);
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		Drawable img3 = img.getConstantState().newDrawable().mutate();
		Drawable img4 = img.getConstantState().newDrawable().mutate();
		Drawable img5 = img.getConstantState().newDrawable().mutate();
		mBtnScanWholeDevice.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		mBtnScanInternal.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		mBtnScanExternal.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
		mBtnScanBooks.setCompoundDrawablesWithIntrinsicBounds(img4, null, null, null);
		mBtnScanFav.setCompoundDrawablesWithIntrinsicBounds(img5, null, null, null);
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
		setDashedButton(mBtnDoScan);
		buttonPressed(mBtnScanBooks);
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
