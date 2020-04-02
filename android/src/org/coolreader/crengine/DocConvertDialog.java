package org.coolreader.crengine;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DocConvertDialog extends BaseDialog {

	public static final Logger log = L.create("dcd");
	private CoolReader mActivity;

	private LayoutInflater mInflater;
	private int mWindowSize;
	private String fileToOpen;

	public DocConvertDialog(CoolReader activity, String fileToOpen)
	{
		super("DocConvertDialog", activity, activity.getString(R.string.doc_convert_needed), false, true);
		this.fileToOpen = fileToOpen;
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		this.mActivity = activity;
		if(getWindow().getAttributes().softInputMode==WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
		    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		}
	}

	@Override
	protected void onCreate() {
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        super.onCreate();
		L.v("DocConvertDialog is created");
	}

	Button btnOpenExisting;
	Button btnConv;
	String sConvFile;
	String sConvPath;

	private void hideExistingFileControls(ViewGroup view) {
		TableLayout tl = (TableLayout) view.findViewById(R.id.table);
		TableRow trowExists1 = (TableRow)view.findViewById(R.id.trow_conv_file_exists1);
		TableRow trowExists2 = (TableRow)view.findViewById(R.id.trow_conv_file_exists2);
		tl.removeView(trowExists1);
		tl.removeView(trowExists2);
	}

	private void setDashedButton(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_dashed_border);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	}

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mInflater = LayoutInflater.from(getContext());
        ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.doc_convert_dialog, null);
        
        TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorIcon,
				 R.attr.colorThemeGray2Contrast
				});
		int colorIcon = a.getColor(0, Color.GRAY);
		int colorGrayC = a.getColor(1, Color.GRAY);
		btnOpenExisting = (Button)view.findViewById(R.id.btn_open_existing);
		setDashedButton(btnOpenExisting);
		btnConv = (Button)view.findViewById(R.id.btn_conv);
		setDashedButton(btnConv);
		FileInfo item = new FileInfo(fileToOpen);
		((TextView)view.findViewById(R.id.conv_doc_path)).setText(fileToOpen);
		final FileInfo downloadDir = Services.getScanner().getDownloadDirectory();
		final String sConvPath = downloadDir.pathname+"/converted/";
		sConvFile = sConvPath + item.filename+".html";
		btnOpenExisting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((CoolReader) activity).loadDocumentExt(sConvFile, "");
				onPositiveButtonClick();
			}
		});
		btnConv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					log.i("Convert odt file");
					ConvertOdtFormat.convertOdtFile(fileToOpen, sConvPath);
					File f = new File(sConvFile);
					if (f.exists()) {
						((CoolReader) activity).loadDocumentExt(sConvFile, "");
						onPositiveButtonClick();
					}
				} catch (Exception e) {
					activity.showToast("exception while converting odt file");
				}
			}
		});
		((TextView)view.findViewById(R.id.conv_path)).setText(sConvFile);
		File f = new File(sConvFile);
		if (!f.exists()) hideExistingFileControls(view);
		a.recycle();
		setView(view);
	}

	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	protected void onOkButtonClick() {
		super.onPositiveButtonClick();
	}

}

