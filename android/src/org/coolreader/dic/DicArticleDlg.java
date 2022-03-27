package org.coolreader.dic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.ProgressDialog;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.dic.struct.Lemma;
import org.coolreader.dic.struct.TranslLine;
import org.coolreader.utils.Utils;

import java.util.HashMap;

public class DicArticleDlg extends BaseDialog {
	CoolReader mCoolReader;
	private LayoutInflater mInflater;
	View mDialogView;
	ExtDicList mExtDicList = null;
	private DicStruct mDicStruct = null;
	private String mFindText = null;
	private DicToastView.Toast mToast;
	private TranslLine mTranslLine;

	public static ProgressDialog progressDlg;

	public static final Logger log = L.create("dad");

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	@Override
	protected void onPositiveButtonClick()
	{
		DicToastView.hideToast(mCoolReader);
		cancel();
	}

	@Override
	protected void onNegativeButtonClick()
	{
	   cancel();
	}

	public final static int ITEM_POSITION=0;

	public DicArticleDlg(CoolReader coolReader, DicToastView.Toast toast, int colorIconL, int position, TranslLine tl)
	{
		super(coolReader, coolReader.getResources().getString(R.string.dic_article_dlg), true, true);
        setCancelable(true);
		this.mCoolReader = coolReader;
		mTranslLine = tl;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mCoolReader, isEInk);
		mInflater = LayoutInflater.from(getContext());
        mDialogView = mInflater.inflate(R.layout.dic_article_dialog, null);
		mDicStruct = new DicStruct();
		mToast = toast;
		Lemma l = toast.dicStruct.getLemmaByNum(position);
		if (l != null)
			mDicStruct.lemmas.add(l);
		mDicStruct.elementsL = toast.dicStruct.elementsL;
		mDicStruct.elementsR = toast.dicStruct.elementsR;
		ViewGroup body = mDialogView.findViewById(R.id.article_list);

		if (tl == null) {
			mExtDicList = new ExtDicList(mActivity, toast, mDicStruct, null, null, colorIconL);
			body.addView(mExtDicList);
		} else {
			View v = mInflater.inflate(R.layout.dic_article_webview, null);
			WebView wv = v.findViewById(R.id.dic_article_wview);
			wv.loadData(tl.transTextHTML, "text/html", "utf8");
			body.addView(v);
		}

		mCoolReader.tintViewIcons(mDialogView);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setView(mDialogView);
	}
}
