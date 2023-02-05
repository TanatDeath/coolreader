package org.coolreader.dic;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Handler;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseListView;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.dic.struct.DictEntry;
import org.coolreader.dic.struct.ExampleLine;
import org.coolreader.dic.struct.LinePair;
import org.coolreader.dic.struct.SuggestionLine;
import org.coolreader.dic.struct.TranslLine;
import org.coolreader.userdic.UserDicEditDialog;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.util.ArrayList;

public class ExtDicList extends BaseListView {

	private DicStruct dicStruct;
	private UserDicEditDialog userDicEditDialog;
	private String findText;
	private CoolReader mCoolReader;
	private Handler mHandler;
	private Runnable mHandleDismiss;
	public static int mColorIconL = Color.GRAY;
	private boolean mFullMode;

	static class ExtDicAdapter extends BaseAdapter {

		private boolean mArticleMode;
		private DicStruct dicStruct;
		private CoolReader mCoolReader;
		private ExtDicList mDicList;

		public ExtDicAdapter(CoolReader coolReader, ExtDicList dicList, DicToastView.Toast toast, DicStruct ds, boolean articleMode) {
			mInflater = (LayoutInflater) toast.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			dicStruct = toast.dicStruct;
			if (ds != null) dicStruct = ds;
			mCoolReader = coolReader;
			curToast = toast;
			mArticleMode = articleMode;
			mDicList = dicList;
		}

		LayoutInflater mInflater;
		DicToastView.Toast curToast;

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			if (curToast == null)
				return 0;
			else if (dicStruct == null)
				return 0;
			else
				return dicStruct.getCount();
		}

		public Object getItem(int position) {
			if (curToast == null)
				return 0;
			if (dicStruct == null)
				return 0;
			if (position < 0 || position >= dicStruct.getCount())
				return null;
			return dicStruct.getByNum(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public final static int ITEM_POSITION=0;

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			Object o = getItem(position);
			if (o == null) {
				int res = R.layout.ext_dic_entry;
				view = mInflater.inflate(res, null);
				return view;
			}
			if (o instanceof DictEntry) {
				DictEntry de = (DictEntry) o;
				int res = R.layout.ext_dic_entry;
				view = mInflater.inflate(res, null);
				TextView labelView = view.findViewById(R.id.ext_dic_entry);
				String text = StrUtils.getNonEmptyStr(de.dictLinkText, true);
				if (!StrUtils.isEmptyStr(de.tagType))
					text = text + "; " + de.tagType.trim();
				if (!StrUtils.isEmptyStr(de.tagWordType))
					text = text + "; " + de.tagWordType.trim();
				labelView.setText(text);
				if (mArticleMode) labelView.setMaxLines(999);
				Button btnOpen = view.findViewById(R.id.dic_more);
				btnOpen.setPadding(10, 4, 10, 4);
				btnOpen.setOnClickListener(v -> {
					DicArticleDlg dad = new DicArticleDlg("DicArticleDlgDetailed",
							DicToastView.mActivity, curToast,
							position);
					dad.show();
				});
				Utils.setSolidButton(btnOpen);
				if (DicToastView.isEInk) Utils.setSolidButtonEink(btnOpen);
				if (!StrUtils.isEmptyStr(curToast.sFindText))
					Utils.setHighLightedText(labelView, curToast.sFindText, mColorIconL);
				if  (mArticleMode) Utils.hideView(btnOpen);
				ImageView ivSpeak = view.findViewById(R.id.btn_speak);
				mCoolReader.tintViewIconsForce(ivSpeak);
				String finalText = text;
				ivSpeak.setOnClickListener(view1 -> DicToastView.sayTTS(mCoolReader, finalText));
				return view;
			}

			if (o instanceof String) {
				String s = (String) o;
				if (s.startsWith("~")) {
					int res = R.layout.ext_dic_transl_group;
					view = mInflater.inflate(res, null);
					TextView labelView = view.findViewById(R.id.ext_dic_transl_group);
					String text = StrUtils.getNonEmptyStr(s.substring(1), true);
					labelView.setText(text);
					if (mArticleMode) labelView.setMaxLines(999);
					if (!StrUtils.isEmptyStr(curToast.sFindText))
						Utils.setHighLightedText(labelView, curToast.sFindText, mColorIconL);
					return view;
				}
			}

			if (o instanceof TranslLine) {
				TranslLine tl = (TranslLine) o;
				int res;
				if (tl.canSwitchTo)
					res = R.layout.ext_dic_transl_line_button;
				else
					res = R.layout.ext_dic_transl_line;
				view = mInflater.inflate(res, null);
				TextView labelView = view.findViewById(R.id.ext_dic_transl_line);
				String text = StrUtils.getNonEmptyStr(tl.transText, true);
				if (!StrUtils.isEmptyStr(tl.transType))
					text = text + "; " + tl.transType.trim();
				labelView.setText(text);
				if (tl.canSwitchTo) {
					res = R.layout.ext_dic_transl_line_button;
					Button btnFollow = view.findViewById(R.id.dic_follow);
					btnFollow.setPadding(10, 4, 10, 4);
					String finalText = text;
					btnFollow.setOnClickListener(v -> {
						if ((mDicList.mHandler != null) && (mDicList.mHandleDismiss != null))
							mDicList.mHandler.postDelayed(
									mDicList.mHandleDismiss, 100);
						BackgroundThread.instance().postGUI(() -> BackgroundThread.instance()
							.postBackground(() -> BackgroundThread.instance()
								.postGUI(() -> {
											if (Dictionaries.reversoTranslate == null)
												Dictionaries.reversoTranslate = new ReversoTranslate();
											Dictionaries.reversoTranslate.reversoTranslateAsLast(mCoolReader, finalText,
													tl.transLink);
										}
								)), 300);
						//m.findInDictionary(selection.text, false, null);
					});
					Utils.setSolidButton(btnFollow);
					if (DicToastView.isEInk) Utils.setSolidButtonEink(btnFollow);
				}
				// plotn - something wrong with html visitor in dsl4j
//				labelView.setOnClickListener(v -> {
//					DicArticleDlg dad = new DicArticleDlg(DicToastView.mActivity, curToast, mColorIconL, position, tl);
//					dad.show();
//				});
				if (mArticleMode) labelView.setMaxLines(999);
				if (!StrUtils.isEmptyStr(curToast.sFindText))
					Utils.setHighLightedText(labelView, curToast.sFindText, mColorIconL);
				return view;
			}

			if (o instanceof ExampleLine) {
				ExampleLine el = (ExampleLine) o;
				int res = R.layout.ext_dic_example_line;
				view = mInflater.inflate(res, null);
				TextView labelView = view.findViewById(R.id.ext_dic_example_line);
				ImageView ivSpeak = view.findViewById(R.id.btn_speak);
				mCoolReader.tintViewIconsForce(ivSpeak);
				String text = StrUtils.getNonEmptyStr(el.line, true);
				ivSpeak.setOnClickListener(view1 -> DicToastView.sayTTS(mCoolReader, text));
				labelView.setText(text);
				if (mArticleMode) labelView.setMaxLines(999);
				if (!StrUtils.isEmptyStr(curToast.sFindText))
					Utils.setHighLightedText(labelView, curToast.sFindText, mColorIconL);
				return view;
			}

			if (o instanceof SuggestionLine) {
				SuggestionLine sl = (SuggestionLine) o;
				int res = R.layout.ext_dic_sugg_line;
				view = mInflater.inflate(res, null);
				TextView labelView = view.findViewById(R.id.ext_sugg_line);
				String text = StrUtils.getNonEmptyStr(sl.suggText, true);
				labelView.setText(text);
				if (mArticleMode) labelView.setMaxLines(999);
				Button btnFollow = view.findViewById(R.id.dic_follow);
				btnFollow.setPadding(10, 4, 10, 4);
				btnFollow.setOnClickListener(v -> {
					if ((mDicList.mHandler != null) && (mDicList.mHandleDismiss != null))
						mDicList.mHandler.postDelayed(
								mDicList.mHandleDismiss, 100);
					BackgroundThread.instance().postGUI(() -> BackgroundThread.instance()
						.postBackground(() -> BackgroundThread.instance()
							.postGUI(() ->
								Dictionaries.reversoTranslate.reversoTranslateAsLast(mCoolReader, text,
									sl.suggLink))), 300);
				});
				Utils.setSolidButton(btnFollow);
				if (DicToastView.isEInk) Utils.setSolidButtonEink(btnFollow);
				if (!StrUtils.isEmptyStr(curToast.sFindText))
					Utils.setHighLightedText(labelView, curToast.sFindText, mColorIconL);
				return view;
			}

			if (o instanceof LinePair) {
				LinePair lp = (LinePair) o;
				int res = R.layout.ext_dic_res_pair;
				view = mInflater.inflate(res, null);
				TextView labelView = view.findViewById(R.id.ext_dic_res_pair1);
				ImageView ivSpeak1 = view.findViewById(R.id.btn_speak1);
				mCoolReader.tintViewIconsForce(ivSpeak1);
				String text = StrUtils.getNonEmptyStr(lp.leftPart, true);
				ivSpeak1.setOnClickListener(view1 -> DicToastView.sayTTS(mCoolReader, text));
				labelView.setText(text);
				if (mArticleMode) labelView.setMaxLines(999);
				if (!StrUtils.isEmptyStr(curToast.sFindText))
					Utils.setHighLightedText(labelView, curToast.sFindText, mColorIconL);
				TextView labelView2 = view.findViewById(R.id.ext_dic_res_pair2);
				ImageView ivSpeak2 = view.findViewById(R.id.btn_speak2);
				mCoolReader.tintViewIconsForce(ivSpeak2);
				String text2 = StrUtils.getNonEmptyStr(lp.rightPart, true);
				labelView2.setText(text2);
				ivSpeak2.setOnClickListener(view1 -> DicToastView.sayTTS(mCoolReader, text2));
				if (mArticleMode) labelView.setMaxLines(999);
				if (!StrUtils.isEmptyStr(curToast.sFindText))
					Utils.setHighLightedText(labelView2, curToast.sFindText, mColorIconL);
				return view;
			}

			int res = R.layout.ext_dic_entry;
			view = mInflater.inflate(res, null);
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			if (curToast == null)
				return true;
			else if (curToast.wikiArticles == null)
				return true;
			else return curToast.wikiArticles.wikiArticleList.size() == 0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	public ExtDicList(CoolReader coolReader, DicToastView.Toast t,
					  DicStruct ds, Handler mHandler, Runnable mHandleDismiss,
					  boolean articleMode) {
		super(coolReader, true);
		this.mCoolReader = coolReader;
		this.dicStruct = t.dicStruct;
		Object o = coolReader.getmBaseDialog().get(UserDicEditDialog.class.getName());
		if (o != null)
			this.userDicEditDialog = (UserDicEditDialog) o;
		if (ds != null) this.dicStruct = ds;
		this.findText = t.sFindText;
		this.mHandler = mHandler;
		this.mHandleDismiss = mHandleDismiss;
		this.mFullMode = articleMode;
		setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		setLongClickable(true);
		setAdapter(new ExtDicAdapter(mCoolReader, this, t, ds, articleMode));
		setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
			//openContextMenu(DictList.this);
			return true;
		});
	}

	@Override
	public boolean performItemClick(View view, int position, long id) {
		if (userDicEditDialog == null)
			Dictionaries.saveToDicSearchHistory(mCoolReader, findText,
				dicStruct.getTranslation(position), DicToastView.mListCurDict, dicStruct);
		else
			userDicEditDialog.dicWordTranslate.setText(dicStruct.getTranslation(position));
		if ((mCoolReader.getReaderView() == null) ||
				(mCoolReader.mCurrentFrame != mCoolReader.mReaderFrame)) {
			ClipboardManager cm = mCoolReader.getClipboardmanager();
			String s = StrUtils.getNonEmptyStr(findText + ": " + dicStruct.getTranslation(position), true);
			cm.setText(StrUtils.getNonEmptyStr(s, true));
		}
		if ((mHandler != null) && (mHandleDismiss != null))
			mHandler.postDelayed(mHandleDismiss, 100);
		return true;
	}
}
