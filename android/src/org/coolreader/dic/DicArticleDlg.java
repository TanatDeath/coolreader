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
	public ViewGroup mBody;
	ExtDicList mExtDicList = null;
	private DicStruct mDicStruct = null;
	private String mFindText = null;
	private DicToastView.Toast mToast;
	private TranslLine mTranslLine;
	public boolean positiveClicked = false;
	public boolean negativeClicked = false;

	public static ProgressDialog progressDlg;

	public static final Logger log = L.create("dad");

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	@Override
	protected void onPositiveButtonClick()
	{
		positiveClicked = true;
		DicArticleDlg dad = null;
		boolean dadExists = mActivity.getmBaseDialog().get("DicArticleDlg") != null;
		if (dadExists) dad = (DicArticleDlg) mActivity.getmBaseDialog().get("DicArticleDlg");
		boolean isDetailed = (dlgName.equals("DicArticleDlgDetailed"));
		// when we show detailed article from toast - allow close both toast and dialog
		if ((isDetailed) && (!dadExists)) {
			DicToastView.hideToast(mCoolReader);
			cancel();
		}
		// when we show detailed article from arcticle view - simple close dialog
		if ((isDetailed) && (dadExists)) {
			DicToastView.hideToast(mCoolReader);
			dad.positiveClicked = true;
			dad.cancel();
			cancel();
		}
		// when we show article list - allow close both toast and dialog
		if (!isDetailed) {
			DicToastView.hideToast(mCoolReader);
			cancel();
		}
	}

	@Override
	protected void onNegativeButtonClick()
	{
		if (!positiveClicked) {
			boolean dadExists = mActivity.getmBaseDialog().get("DicArticleDlg") != null;
			boolean isDetailed = (dlgName.equals("DicArticleDlgDetailed"));
			if (isDetailed) {
				cancel();
			}
			// when we show article list - allow close both toast and dialog
			if (!isDetailed) {
				if (!negativeClicked)
					DicToastView.hideToast(mCoolReader);
				cancel();
			}
		} else cancel();
		negativeClicked = true;
	}

	public final static int ITEM_POSITION=0;

	public DicArticleDlg(String dlgName, CoolReader coolReader, DicToastView.Toast toast, int position)
	{
		super(dlgName, coolReader, coolReader.getResources().getString(R.string.dic_article_dlg), true, true);
		whenCreate(dlgName, coolReader, toast, position, null, null);
	}

	public DicArticleDlg(String dlgName, CoolReader coolReader, DicToastView.Toast toast, int position, TranslLine tl)
	{
		super(dlgName, coolReader, coolReader.getResources().getString(R.string.dic_article_dlg), true, true);
		whenCreate(dlgName, coolReader, toast, position,tl, null);
	}

	public DicArticleDlg(String dlgName, CoolReader coolReader, DicToastView.Toast toast, int position, View dicContent) {
		super(dlgName, coolReader, coolReader.getResources().getString(R.string.dic_article_dlg2), true, true);
		whenCreate(dlgName, coolReader, toast, position,null, dicContent);
	}

	private void whenCreate(String dlgName, CoolReader coolReader, DicToastView.Toast toast, int position,
					   TranslLine tl, View dicContent) {
		setCancelable(true);
		this.mCoolReader = coolReader;
		mTranslLine = tl;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mCoolReader, isEInk);
		mInflater = LayoutInflater.from(getContext());
		mDialogView = mInflater.inflate(R.layout.dic_article_dialog, null);
		if (position == -1) {
			mDicStruct = toast.dicStruct;
		} else {
			mDicStruct = new DicStruct();
			mToast = toast;
			Lemma l = toast.dicStruct.getLemmaByNum(position);
			if (l != null)
				mDicStruct.lemmas.add(l);
			mDicStruct.elementsL = toast.dicStruct.elementsL;
			mDicStruct.elementsR = toast.dicStruct.elementsR;
		}

		mBody = mDialogView.findViewById(R.id.article_list);
		if (dicContent != null) {
			mBody.addView(dicContent);
		} else {
			if (tl == null) {
				mExtDicList = new ExtDicList(mActivity, toast, mDicStruct, null, null,
						position >= 0);
				mBody.addView(mExtDicList);
			} else {
				View v = mInflater.inflate(R.layout.dic_article_webview, null);
				WebView wv = v.findViewById(R.id.dic_article_wview);
				wv.loadData(tl.transTextHTML, "text/html", "utf8");
				mBody.addView(v);
			}
		}
		mCoolReader.tintViewIcons(mDialogView);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setView(mDialogView);
	}
}
