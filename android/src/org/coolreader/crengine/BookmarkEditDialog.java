package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.readerview.ReaderView;
import org.coolreader.dic.Dictionaries;
import org.coolreader.dic.TranslationDirectionDialog;
import org.coolreader.options.OptionsDialog;
import org.coolreader.options.SelectionModesOption;
import org.coolreader.userdic.UserDicEntry;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.CRC32;

public class BookmarkEditDialog extends BaseDialog {
	
	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final ReaderView mReaderView;
	private final Bookmark mOriginalBookmark;
	private final Bookmark mBookmark;
	private final boolean mIsNew;
	private final String cbtextFull;
	final TextView lblSetComment;
	final TextView lblTransl1;
	final TextView lblTransl2;
	final TextView lblBookmarkLink;
	BookInfo mBookInfo;
	final EditText commentEdit;
	final EditText posEdit;
	final TextView rb_descr;
	final TableRow tr_descr;
	int mChosenType = 0;
	final ImageButton btnComment;
	final ImageButton btnCorrection;
	final ImageButton btnUserDic;
	final ImageButton btnInternalLink;
	final ImageButton btnCitation;
	final TextView commentLabel;
	final Button btnSendTo1;
	final Button btnSendTo2;
	final Button btnTransl;
	final Button btnTransl2;
	final Button btnColorCheck;
	final Button btnColorChoose;
	final EditText edtContext;
	public static int lastColor = 0;
	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;
	private final ArrayList<Button> colorButtons = new ArrayList<>();

	private boolean bColorCheck = false;

	private void paintColorCheckButton() {
		int colorGrayC;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast});
		colorGrayC = a.getColor(0, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mCoolReader.tintViewIcons(btnColorCheck, PorterDuff.Mode.CLEAR,true);
		if (bColorCheck) {
			btnColorCheck.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnColorCheck, true);
			btnColorCheck.setPaintFlags(btnColorCheck.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		} else {
			btnColorCheck.setBackgroundColor(colorGrayCT);
			btnColorCheck.setPaintFlags( btnColorCheck.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
		}
	}

	private void selectBmkColor(boolean doSelect) {
		int colorGray;
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon, R.attr.colorIconL});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		a.recycle();
		int colorGrayCT = Color.argb(128, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC));
		if (doSelect) {
			btnColorChoose.setBackgroundColor(bColorCheck ? lastColor : colorGrayCT);
			ColorPickerDialog dlg = new ColorPickerDialog(mCoolReader, color -> {
				lastColor = color;
				paintColorCheckButton();
				btnColorChoose.setBackgroundColor(bColorCheck ? lastColor : colorGrayCT);
			}, lastColor, "");
			dlg.show();
		} else {
			btnColorChoose.setBackgroundColor(bColorCheck ? lastColor : colorGrayCT);
		}
	}

	@Override
	protected void onPositiveButtonClick() {
		if (
			 (mBookmark.getType() == Bookmark.TYPE_USER_DIC) ||
			 (mBookmark.getType() == Bookmark.TYPE_CITATION)
		   ) {
			UserDicEntry ude = new UserDicEntry();
			ude.setId(0L);
			ude.setDic_word(posEdit.getText().toString());
			ude.setDic_word_translate(commentEdit.getText().toString());
			final String sBookFName = mBookInfo.getFileInfo().getFilename();
			CRC32 crc = new CRC32();
			crc.update(sBookFName.getBytes());
			ude.setDic_from_book(String.valueOf(crc.getValue()));
			ude.setCreate_time(System.currentTimeMillis());
			ude.setLast_access_time(System.currentTimeMillis());
			ude.setLanguage(mBookInfo.getFileInfo().language);
			ude.setShortContext(edtContext.getText().toString());
			ude.setFullContext(getFullContextText(mCoolReader));
			ude.setIsCustomColor(bColorCheck ? 1 : 0);
			ude.setCustomColor(Utils.colorToHex(lastColor));
			ude.setSeen_count(0L);
			ude.setIs_citation(0);
			if (mBookmark.getType() == Bookmark.TYPE_CITATION) ude.setIs_citation(1);
			activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_NEW);
			if (activity instanceof CoolReader) {
				((CoolReader) activity).getmUserDic().put(ude.getIs_citation()+ude.getDic_word(), ude);
				BackgroundThread.instance().postGUI(() -> ((CoolReader) activity).getmReaderFrame().getUserDicPanel().updateUserDicWords(), 1000);
			}
		} else {
			if (mIsNew) {
				mBookmark.setCommentText(commentEdit.getText().toString());
				mBookmark.setShortContext(edtContext.getText().toString());
				mBookmark.setFullContext(getFullContextText(mCoolReader));
				mBookmark.setIsCustomColor(bColorCheck ? 1 : 0);
				mBookmark.setCustomColor(Utils.colorToHex(lastColor));
				if (mBookmark.getType() == Bookmark.TYPE_INTERNAL_LINK)
					mBookmark.setType(Bookmark.TYPE_COMMENT);
				mReaderView.addBookmark(mBookmark);
			} else {
				boolean changed = mOriginalBookmark.setCommentText(commentEdit.getText().toString());
				boolean changed2 = mOriginalBookmark.setIsCustomColor(bColorCheck ? 1 : 0);
				boolean changed3 = mOriginalBookmark.setCustomColor(Utils.colorToHex(lastColor));
				boolean changed4 = mOriginalBookmark.setShortContext(edtContext.getText().toString());
				if (changed || changed2 || changed3 || changed4)
					{
					mOriginalBookmark.setTimeStamp(System.currentTimeMillis());
					mReaderView.updateBookmark(mOriginalBookmark);
				}
			}
		}
		super.onPositiveButtonClick();
	}

	public void BookmarkChooseCallback(Bookmark bm) {
		if (bm!=null) {
			String s1 = bm.getTitleText();
			String s2 = bm.getPosText();
			String s3 = bm.getCommentText();
			lblBookmarkLink.setText("");
			if (!StrUtils.isEmptyStr(s1)) lblBookmarkLink.setText(activity.getString(R.string.dlg_bookmark_link) +": " +s1);
			else if (!StrUtils.isEmptyStr(s2)) lblBookmarkLink.setText(activity.getString(R.string.dlg_bookmark_link) +": " +s2);
			else if (!StrUtils.isEmptyStr(s3)) lblBookmarkLink.setText(activity.getString(R.string.dlg_bookmark_link) +": " + s3);
			mBookmark.setLinkPos(bm.getStartPos());
		} else {
			mBookmark.setLinkPos("");
			lblBookmarkLink.setText("");
		}
	}

	private void setChecked(ImageButton btn) {
		rb_descr.setText(btn.getContentDescription()+" ");
		if (btn.getContentDescription().equals(activity.getString(R.string.dlg_bookmark_type_comment))) {
			mChosenType = Bookmark.TYPE_COMMENT;
			commentLabel.setText(R.string.dlg_bookmark_edit_comment);
		}
		if (btn.getContentDescription().equals(activity.getString(R.string.dlg_bookmark_type_correction))) {
			mChosenType = Bookmark.TYPE_CORRECTION;
			commentLabel.setText(R.string.dlg_bookmark_edit_correction);
		}
		if (btn.getContentDescription().equals(activity.getString(R.string.dlg_bookmark_internal_link))) {
			mChosenType = Bookmark.TYPE_INTERNAL_LINK;
			commentLabel.setText(R.string.dlg_bookmark_edit_comment);
		}
		if (btn.getContentDescription().equals(activity.getString(R.string.dlg_bookmark_user_dic))) {
			mChosenType = Bookmark.TYPE_USER_DIC;
			commentLabel.setText(R.string.dlg_bookmark_edit_translation);
		}
		if (btn.getContentDescription().equals(activity.getString(R.string.dlg_bookmark_citation))) {
			mChosenType = Bookmark.TYPE_CITATION;
			commentLabel.setText(R.string.dlg_bookmark_edit_comment);
		}
		int colorGray;
		int colorGrayC;
		int colorIcon;
		int colorIconL;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon, R.attr.colorIconL});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(100,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		rb_descr.setBackgroundColor(colorGrayCT);
		//tr_descr.setBackgroundColor(colorGrayC);
		btnComment.setBackgroundColor(colorGrayCT);
		btnCorrection.setBackgroundColor(colorGrayCT);
		btnUserDic.setBackgroundColor(colorGrayCT);
		btnInternalLink.setBackgroundColor(colorGrayCT);
		btnCitation.setBackgroundColor(colorGrayCT);
		//lblSetComment.setTextColor(colorGrayCT2);
		lblTransl1.setTextColor(colorGrayCT2);
		lblTransl2.setTextColor(colorGrayCT2);
		lblBookmarkLink.setTextColor(activity.getTextColor(colorIcon));
		btnSendTo1.setBackgroundColor(colorGrayCT);
		btnSendTo2.setBackgroundColor(colorGrayCT);
		btnTransl.setBackgroundColor(colorGrayCT);
		btnTransl2.setBackgroundColor(colorGrayCT);
		paintColorCheckButton();
		btnColorChoose.setBackgroundColor(bColorCheck ? lastColor : colorGrayCT);
		btn.setBackgroundColor(colorGray);
	}

	private boolean getChecked(ImageButton btn) {
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_type_comment))) {
			return mChosenType == Bookmark.TYPE_COMMENT;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_type_correction))) {
			return mChosenType == Bookmark.TYPE_CORRECTION;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_internal_link))) {
			return mChosenType == Bookmark.TYPE_INTERNAL_LINK;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_user_dic))) {
			return mChosenType == Bookmark.TYPE_USER_DIC;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_citation))) {
			return mChosenType == Bookmark.TYPE_CITATION;
		}
		return false;
	}

	private Dictionaries.DictInfo currentDic = null;

	private void setupTranslButton() {
		Dictionaries.DictInfo di = mCoolReader.getCurOrFirstOnlineDic();
		if (di != null) {
			btnTransl.setText(di.name);
			btnTransl2.setText(di.name);
			currentDic = di;
			return;
		}
		Utils.hideView(btnTransl);
		Utils.hideView(btnTransl2);
	}

	public static String getFullContextText(CoolReader cr) {
		if (cr != null)
			if (cr.getReaderView() != null) {
				return StrUtils.getNonEmptyStr(
						cr.getReaderView().getPageTextFromEngine(
								cr.getReaderView().getDoc().getCurPage()), true);
			}
		return "";
	}

	public static String getContextText(CoolReader cr, String text) {
		int curPage = cr.getReaderView().getDoc().getCurPage();
		String sPageText = StrUtils.getNonEmptyStr(cr.getReaderView().getPageTextFromEngine(curPage), true);
		String sBmkText = StrUtils.getNonEmptyStr(text, true);
		String sContextText = "";
		if (sPageText.contains(sBmkText)) {
			int iPos = sPageText.indexOf(sBmkText);
			int iPosBeg = iPos;
			while (iPosBeg > 0) {
				iPosBeg--;
				if (sPageText.startsWith(".", iPosBeg) ||
						sPageText.startsWith("!", iPosBeg) ||
						sPageText.startsWith("?", iPosBeg) ||
						sPageText.startsWith("\u2026", iPosBeg)
				) break;
			}
			int iPosEnd = iPos + sBmkText.length();
			while (iPosEnd < sPageText.length()-1) {
				iPosEnd++;
				if (sPageText.startsWith(".", iPosEnd) ||
						sPageText.startsWith("!", iPosEnd) ||
						sPageText.startsWith("?", iPosEnd) ||
						sPageText.startsWith("\u2026", iPosEnd)
				) break;
			}
			sContextText = StrUtils.getNonEmptyStr((sPageText + ".").substring(iPosBeg + 1, iPosEnd + 1),true);
		}
		if (StrUtils.isEmptyStr(sContextText)) sContextText = sPageText;
		return sContextText;
		//return sPageText;
//		return mBookmark.getPosText();
	}

	public static String getSendToText1(CoolReader cr, String defText, String context) {
		String text = StrUtils.textShrinkLines(StrUtils.getNonEmptyStr(defText, true), true);
		int mod = cr.settings().getInt(Settings.PROP_APP_BOOKMARK_ACTION_SEND_TO_MOD, 0);
		if ((mod > 0) && (StrUtils.getNonEmptyStr(context, false).contains(StrUtils.getNonEmptyStr(defText, false)))) { // need context, instead of text
			String newContext = StrUtils.getNonEmptyStr(context, false);
			if (mod == 2) newContext = newContext.replaceAll(StrUtils.getNonEmptyStr(defText, false), "<b>"+defText+"</b>");
			if (mod == 3) newContext = newContext.replaceAll(StrUtils.getNonEmptyStr(defText, false), "<i>"+defText+"</i>");
			if (mod == 4) newContext = newContext.replaceAll(StrUtils.getNonEmptyStr(defText, false), "<u>"+defText+"</u>");
			if (mod == 5) newContext = defText + "<br>" + newContext.replaceAll(StrUtils.getNonEmptyStr(defText, false), "<b>"+defText+"</b>");
			if (mod == 6) newContext = defText + "<br>" + newContext.replaceAll(StrUtils.getNonEmptyStr(defText, false), "<i>"+defText+"</i>");
			if (mod == 7) newContext = defText + "<br>" +newContext.replaceAll(StrUtils.getNonEmptyStr(defText, false), "<u>"+defText+"</u>");
			return StrUtils.getNonEmptyStr(newContext, true);
		}
		return StrUtils.getNonEmptyStr(text, true);
	}

	public static String getSendToText2(CoolReader cr, String defText1, String defText2, String context) {
		String text2 = StrUtils.textShrinkLines(StrUtils.getNonEmptyStr(defText2, true), true);
		int mod = cr.settings().getInt(Settings.PROP_APP_BOOKMARK_ACTION_SEND_TO_MOD, 0);
		if ((mod > 0) && (StrUtils.getNonEmptyStr(context, false).contains(StrUtils.getNonEmptyStr(defText1, false)))) {
			if (StrUtils.isEmptyStr(text2)) { // empty comment and context - we'll use full page text
				int curPage = cr.getReaderView().getDoc().getCurPage();
				String sPageText = StrUtils.getNonEmptyStr(cr.getReaderView().getPageTextFromEngine(curPage), true);
				if (mod == 2) sPageText = sPageText.replaceAll(StrUtils.getNonEmptyStr(defText1, false), "<b>"+defText1+"</b>");
				if (mod == 3) sPageText = sPageText.replaceAll(StrUtils.getNonEmptyStr(defText1, false), "<i>"+defText1+"</i>");
				if (mod == 4) sPageText = sPageText.replaceAll(StrUtils.getNonEmptyStr(defText1, false), "<u>"+defText1+"</u>");
				if (mod == 5) sPageText = defText1 + "<br>" + sPageText.replaceAll(StrUtils.getNonEmptyStr(defText1, false), "<b>"+defText1+"</b>");
				if (mod == 6) sPageText = defText1 + "<br>" + sPageText.replaceAll(StrUtils.getNonEmptyStr(defText1, false), "<i>"+defText1+"</i>");
				if (mod == 7) sPageText = defText1 + "<br>" + sPageText.replaceAll(StrUtils.getNonEmptyStr(defText1, false), "<u>"+defText1+"</u>");
				text2 = StrUtils.textShrinkLines(StrUtils.getNonEmptyStr(sPageText, true), true);
			}
		} else {
			if (StrUtils.isEmptyStr(text2)) { // empty comment and no context - we'll use context
				text2 = StrUtils.textShrinkLines(StrUtils.getNonEmptyStr(context, true), true);
				//text2 = StrUtils.textShrinkLines(StrUtils.getNonEmptyStr(getContextText(cr, context), true), true);
			}
		}
		return StrUtils.getNonEmptyStr(text2, true);
	}

	private void doTranslate(boolean isPosText, boolean withLangSel, View anchor) {
		if (!FlavourConstants.PREMIUM_FEATURES) {
			mCoolReader.showToast(R.string.only_in_premium);
			return;
		}
		String posText = posEdit.getText().toString();
		if (!isPosText) posText = edtContext.getText().toString();
		if ((currentDic != null) && (!StrUtils.isEmptyStr(posText))) {
			mCoolReader.mDictionaries.setAdHocDict(currentDic);

			if (withLangSel) {
				String finalPosText = posText;
				mCoolReader.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_ONLY_CHOOSE_QUICK,
						anchor, mReaderView.getBookInfo().getFileInfo().parent,
						mBookInfo.getFileInfo(),
						StrUtils.getNonEmptyStr(mBookInfo.getFileInfo().lang_from, true),
						StrUtils.getNonEmptyStr(mBookInfo.getFileInfo().lang_to, true),
						"", null, TranslationDirectionDialog.FOR_COMMON, s -> {
							//mCoolReader.showToast(s);
							mCoolReader.findInDictionary(finalPosText, null, new CoolReader.DictionaryCallback() {

								@Override
								public boolean showDicToast() {
									return false;
								}

								@Override
								public boolean saveToHist() {
									return true;
								}

								@Override
								public void done(String result) {
									commentEdit.setText(result);
								}

								@Override
								public void fail(Exception e, String msg) {
									mCoolReader.showToast(msg);
								}
							});
						});
			} else {
				mCoolReader.findInDictionary(posText, null, new CoolReader.DictionaryCallback() {

					@Override
					public boolean showDicToast() {
						return false;
					}

					@Override
					public boolean saveToHist() {
						return true;
					}

					@Override
					public void done(String result) {
						commentEdit.setText(result);
					}

					@Override
					public void fail(Exception e, String msg) {
						mCoolReader.showToast(msg);
					}
				});
			}
		}
	}

	public BookmarkEditDialog(final CoolReader activity, ReaderView readerView, Bookmark bookmark, boolean isNew, int chosenType, String commentText)
	{
		super("BookmarkEditDialog", activity, "", true, false);
		mCoolReader = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mCoolReader, isEInk);
		mReaderView = readerView;
		mIsNew = isNew;
		mOriginalBookmark = bookmark;
		this.mBookInfo = mReaderView.getBookInfo();
		//if ( !isNew )
		mBookmark = new Bookmark(bookmark);
		//else
		//	mBookmark = bookmark;
		if (!isNew) {
			setThirdButtonImage(
					Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_minus, R.drawable.icons8_minus)
					, R.string.mi_bookmark_delete);
		}
		boolean isComment = bookmark.getType()==Bookmark.TYPE_COMMENT;
		boolean isCorrection = bookmark.getType()==Bookmark.TYPE_CORRECTION;
		boolean isUserDic = bookmark.getType()==Bookmark.TYPE_USER_DIC;
		boolean isInternalLink = bookmark.getType()==Bookmark.TYPE_INTERNAL_LINK;
		boolean isCitation = bookmark.getType()==Bookmark.TYPE_CITATION;
		setTitle(mCoolReader.getString( mIsNew ? R.string.dlg_bookmark_create : R.string.dlg_bookmark_edit));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.bookmark_edit_dialog, null);
		Button btnColor1 = view.findViewById(R.id.btn_color_1);
		if (btnColor1 != null) colorButtons.add(btnColor1);
		Button btnColor2 = view.findViewById(R.id.btn_color_2);
		if (btnColor2 != null) colorButtons.add(btnColor2);
		Button btnColor3 = view.findViewById(R.id.btn_color_3);
		if (btnColor3 != null) colorButtons.add(btnColor3);
		Button btnColor4 = view.findViewById(R.id.btn_color_4);
		if (btnColor4 != null) colorButtons.add(btnColor4);
		Button btnColor5 = view.findViewById(R.id.btn_color_5);
		if (btnColor5 != null) colorButtons.add(btnColor5);
		Button btnColor6 = view.findViewById(R.id.btn_color_6);
		if (btnColor6 != null) colorButtons.add(btnColor6);
		Button btnColor7 = view.findViewById(R.id.btn_color_7);
		if (btnColor7 != null) colorButtons.add(btnColor7);
		Button btnColor8 = view.findViewById(R.id.btn_color_8);
		if (btnColor8 != null) colorButtons.add(btnColor8);
		Button btnColor9 = view.findViewById(R.id.btn_color_9);
		if (btnColor9 != null) colorButtons.add(btnColor9);
		Button btnColor10 = view.findViewById(R.id.btn_color_10);
		if (btnColor10 != null) colorButtons.add(btnColor10);
		Button btnColor11 = view.findViewById(R.id.btn_color_11);
		if (btnColor11 != null) colorButtons.add(btnColor11);
		Button btnColor12 = view.findViewById(R.id.btn_color_12);
		if (btnColor12 != null) colorButtons.add(btnColor12);
		Button btnColor13 = view.findViewById(R.id.btn_color_13);
		if (btnColor13 != null) colorButtons.add(btnColor13);
		Button btnColor14 = view.findViewById(R.id.btn_color_14);
		if (btnColor14 != null) colorButtons.add(btnColor14);
		for (int i=0; i<colorButtons.size(); i++) {
			Button btn = colorButtons.get(i);
			if (i == 0) btn.setHint("#ff0000"); //RED
			if (i == 1) btn.setHint("#008000"); //GREEN
			if (i == 2) btn.setHint("#0000ff"); //BLUE
			if (i == 3) btn.setHint("#FFFF00"); //YELLOW
			if (i == 4) btn.setHint("#800000"); //MAROON
			if (i == 5) btn.setHint("#00ff00"); //LIME
			if (i == 6) btn.setHint("#000080"); //NAVY
			if (i == 7) btn.setHint("#800080"); //PURPLE
			if (i == 8) btn.setHint("#808000"); //OLIVE
			if (i == 9) btn.setHint("#00ffff"); //AQUA
			if (i == 10) btn.setHint("#ff00ff"); //fuchsia
			if (i == 11) btn.setHint("#008080"); //teal
			if (i == 12) btn.setHint("#808080"); //gray
			if (i == 13) btn.setHint("#c0c0c0"); //silver
			btn.setBackgroundColor(Color.parseColor(btn.getHint().toString()));
			btn.setOnClickListener(v -> {
				bColorCheck = true;
				lastColor = Color.parseColor(btn.getHint().toString());
				selectBmkColor(false);
				paintColorCheckButton();
			});
		}
		btnComment = view.findViewById(R.id.rb_comment);
		btnCorrection = view.findViewById(R.id.rb_correction);
		btnUserDic = view.findViewById(R.id.rb_user_dic);
		btnInternalLink = view.findViewById(R.id.rb_internal_link);
		btnCitation = view.findViewById(R.id.rb_citation);
		ImageButton btnFake = view.findViewById(R.id.btn_fake);
		rb_descr = view.findViewById(R.id.lbl_rb_descr);
		tr_descr = view.findViewById(R.id.tr_rb_descr);
		final TextView posLabel = view.findViewById(R.id.lbl_position);
		commentLabel = view.findViewById(R.id.lbl_comment_text);
		btnSendTo1 = view.findViewById(R.id.btn_sent_to1);
		btnSendTo2 = view.findViewById(R.id.btn_sent_to2);
		btnTransl = view.findViewById(R.id.btn_transl);
		btnTransl2 = view.findViewById(R.id.btn_transl2);
		setupTranslButton();
		btnColorCheck = view.findViewById(R.id.btn_color_check);
		BackgroundThread.instance().postGUI(() -> paintColorCheckButton(), 200);
		btnColorCheck.setOnClickListener(v -> {
			bColorCheck = !bColorCheck;
			selectBmkColor(bColorCheck);
			paintColorCheckButton();
		});
		btnColorChoose = view.findViewById(R.id.btn_color_choose);
		btnColorChoose.setOnClickListener(v -> {
			selectBmkColor(bColorCheck);
		});
		edtContext = view.findViewById(R.id.context_text);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		((TextView) view.findViewById(R.id.lbl_rb_descr)).setTextColor(Color.argb(170, Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon)));
		
		final CoolReader cr = activity;

		commentLabel.setOnClickListener(v -> {
			if (getChecked(btnUserDic))
				cr.showToast(R.string.dlg_bookmark_info);
		});

		posEdit = view.findViewById(R.id.position_text);
		final KeyListener keyList;
		keyList = posEdit.getKeyListener();
		posEdit.setKeyListener(null);

		final ImageButton btnSetComment = view.findViewById(R.id.base_dlg_btn_add);
		lblSetComment = view.findViewById(R.id.lbl_comment_from_cb);
		lblTransl1 = view.findViewById(R.id.lbl_transl_sel);
		lblTransl2 = view.findViewById(R.id.lbl_context_transl);
		lblBookmarkLink = view.findViewById(R.id.lbl_bookmark_link);
		String sL = mBookmark.getLinkPos();
		if (StrUtils.isEmptyStr(sL)) lblBookmarkLink.setText(""); else
			lblBookmarkLink.setText(activity.getString(R.string.dlg_bookmark_link) +": " +sL);
		if (!StrUtils.isEmptyStr(sL)) {
			for (Bookmark b: mBookInfo.getAllBookmarks()) {
				if (b.getStartPos().equals(sL)) {
					BookmarkChooseCallback(b);
					break;
				}
			}
		}
		String sClpb = "<empty>";
		try {
			android.text.ClipboardManager cm = mCoolReader.getClipboardmanager();
			sClpb = cm.getText().toString();
		} catch (Exception e) {

		}
		String cbtext = StrUtils.textShrinkLines(sClpb,true);
		if (cbtext.length()>100) {
			cbtext = cbtext.substring(0, 100)+"...";
		}
		if (cbtext.trim().length()==0) {
			cbtext = "<empty>";
		}
		cbtextFull = StrUtils.textShrinkLines(sClpb.trim(),false);
		lblSetComment.setText("");
		if (!cbtext.trim().equals(""))
			lblSetComment.setText(activity.getString(R.string.clipb_contents) +" "+cbtext.trim());
		if (!cbtext.trim().equals("")) {
			setAddButtonImage(
					Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_ok_from_clipboard, R.drawable.icons8_ok_from_clipboard),
					//R.drawable.icons8_ok_from_clipboard,
					R.string.set_comment_from_cb_capt);
		}
		lblSetComment.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!StrUtils.isEmptyStr(cbtextFull))
					commentEdit.setText(cbtextFull);
			}
		});
		lblBookmarkLink.setOnClickListener(v -> {
			if ((mIsNew)&&(mBookmark.getType()==Bookmark.TYPE_INTERNAL_LINK))
				activity.showBookmarksDialog(true, BookmarkEditDialog.this);
		});
		commentEdit = view.findViewById(R.id.comment_edit);
		int colorIcon128 = Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		commentEdit.setHintTextColor(colorIcon128);
		String postext = mBookmark.getPercent()/100 + "%";
		if ( mBookmark.getTitleText()!=null )
			postext = postext + "  " + mBookmark.getTitleText();
		String s4 = Utils.formatDateFixed(mBookmark.getTimeStamp()) + " " + Utils.formatTime(activity, mBookmark.getTimeStamp());
		posLabel.setText(postext + " (" + s4 + ")");
		if (isComment) commentLabel.setText(R.string.dlg_bookmark_edit_comment);
		if (isCorrection) commentLabel.setText(R.string.dlg_bookmark_edit_correction);
		if (isUserDic) commentLabel.setText(R.string.dlg_bookmark_edit_translation);
		if (isInternalLink) commentLabel.setText(R.string.dlg_bookmark_edit_comment);
		if (isCitation) commentLabel.setText(R.string.dlg_bookmark_edit_comment);

		posEdit.setText(mBookmark.getPosText());
		commentEdit.setText(bookmark.getCommentText());
		edtContext.setText(bookmark.getShortContext());
		bColorCheck = bookmark.isCustomColor == 1;
		int color = 0;
		if (bColorCheck) {
			try {
				color = Color.parseColor("#" + bookmark.getCustomColor().replace("#", ""));
			} catch (Exception e) {
				bColorCheck = false;
			}
		}
		lastColor = color;
		if (bColorCheck) selectBmkColor(false);
		String sCapt = "";
		if ( isNew ) {
			if (isComment) setChecked(btnComment);
			if (!isComment) setChecked(btnCorrection);
			btnComment.setOnClickListener(v -> {
				mBookmark.setType(Bookmark.TYPE_COMMENT);
				commentLabel.setText(R.string.dlg_bookmark_edit_comment); // : R.string.dlg_bookmark_edit_correction
				posEdit.setKeyListener(null);
				posEdit.setText(mBookmark.getPosText());
				setChecked(btnComment);
			});
			btnCorrection.setOnClickListener(v -> {
				mBookmark.setType(Bookmark.TYPE_CORRECTION);
				commentLabel.setText(R.string.dlg_bookmark_edit_correction);
				String oldText = commentEdit.getText().toString();
				if ( oldText==null || oldText.length()==0 )
					commentEdit.setText(mBookmark.getPosText());
				posEdit.setKeyListener(null);
				posEdit.setText(mBookmark.getPosText());
				setChecked(btnCorrection);
			});
			btnInternalLink.setOnClickListener(v -> {
				mBookmark.setType(Bookmark.TYPE_INTERNAL_LINK);
				commentLabel.setText(R.string.dlg_bookmark_edit_comment);
				posEdit.setKeyListener(null);
				posEdit.setText(mBookmark.getPosText());
				setChecked(btnInternalLink);
				if (mIsNew)
					activity.showBookmarksDialog(true, BookmarkEditDialog.this);
			});
			btnUserDic.setOnClickListener(v -> {
				if ((!FlavourConstants.PRO_FEATURES)&&(!FlavourConstants.PREMIUM_FEATURES)) {
					mCoolReader.showToast(R.string.only_in_pro);
					return;
				}
				mBookmark.setType(Bookmark.TYPE_USER_DIC);
				commentLabel.setText(R.string.dlg_bookmark_edit_translation);
				posEdit.setKeyListener(keyList);
				posEdit.setText(StrUtils.updateText(mBookmark.getPosText(),true));
				setChecked(btnUserDic);
			});
			btnCitation.setOnClickListener(v -> {
				if ((!FlavourConstants.PRO_FEATURES)&&(!FlavourConstants.PREMIUM_FEATURES)) {
					mCoolReader.showToast(R.string.only_in_pro);
					return;
				}
				mBookmark.setType(Bookmark.TYPE_CITATION);
				commentLabel.setText(R.string.dlg_bookmark_edit_comment); // : R.string.dlg_bookmark_edit_correction
				if ((commentEdit.getText().toString().equals("")) ||
						(commentEdit.getText().toString().equals(posEdit.getText().toString())))
					commentEdit.setText(getContextText(mCoolReader, mBookmark.getPosText()));
				posEdit.setKeyListener(keyList);
				posEdit.setText(mBookmark.getPosText());
				setChecked(btnCitation);
			});
			if (!StrUtils.isEmptyStr(commentText)) {
				String cText = StrUtils.getNonEmptyStr(commentText, true);
				if (cText.startsWith("{{lingvo}}")) {
					cText = cText.replace("{{lingvo}}", "");
					if (cText.contains(":")) {
						sCapt = cText.split(":")[0];
						posEdit.setText(StrUtils.updateText(sCapt, true));
						cText = cText.replace(sCapt + ":", "");
					}
				} else {
					if (cText.startsWith(mBookmark.getPosText() + ":"))
						cText = cText.substring((mBookmark.getPosText() + ":").length());
				}
				commentEdit.setText(cText.trim());
			}
			edtContext.setText(getContextText(mCoolReader, mBookmark.getPosText()));
		} else {
			btnComment.setClickable(false);
			btnCorrection.setClickable(false);
			btnInternalLink.setClickable(false);
			btnUserDic.setClickable(false);
			btnCitation.setClickable(false);
		}
		if (chosenType==Bookmark.TYPE_COMMENT) {
			setChecked(btnComment);
			mBookmark.setType(Bookmark.TYPE_COMMENT);
		}
		if (chosenType==Bookmark.TYPE_CORRECTION) {
			setChecked(btnCorrection);
			mBookmark.setType(Bookmark.TYPE_CORRECTION);
		}
		if (chosenType==Bookmark.TYPE_INTERNAL_LINK) {
			setChecked(btnInternalLink);
			mBookmark.setType(Bookmark.TYPE_INTERNAL_LINK);
		}
		if (chosenType==Bookmark.TYPE_USER_DIC) {
			setChecked(btnUserDic);
			mBookmark.setType(Bookmark.TYPE_USER_DIC);
			if (StrUtils.isEmptyStr(sCapt))
				posEdit.setText(StrUtils.updateText(mBookmark.getPosText(),true));
			posEdit.setKeyListener(keyList);
		}
		if (chosenType==Bookmark.TYPE_CITATION) {
			setChecked(btnCitation);
			mBookmark.setType(Bookmark.TYPE_CITATION);
			posEdit.setKeyListener(keyList);
		}
		btnSendTo1.setOnClickListener(v -> {
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.setType("text/plain");
			String text1 = getSendToText1(cr, posEdit.getText().toString(),
					edtContext.getText().toString());
			String text2 = getSendToText2(cr, posEdit.getText().toString(),
					commentEdit.getText().toString(),
					edtContext.getText().toString());
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, text1);
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text2);
		 	mCoolReader.startActivity(Intent.createChooser(emailIntent, null));
		 	onPositiveButtonClick();
		});
		btnSendTo2.setOnClickListener(v -> {
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			int selAction = -1;
			if (mCoolReader.getReaderView() != null) selAction = mCoolReader.getReaderView().mBookmarkActionSendTo;
			if (selAction == -1) {
				mCoolReader.showToast("Dictionary is not installed. ");
				return;
			}
			int titleSendTo =  SelectionModesOption.getSelectionActionTitle(selAction);
			Dictionaries.DictInfo curDict = OptionsDialog.getDicValue(mCoolReader.getString(titleSendTo), mCoolReader.settings(), mCoolReader);
			if (curDict == null) {
				mCoolReader.showToast("Dictionary is not installed. ");
				return;
			}
			emailIntent.setType("text/plain");
			String text1 = getSendToText1(cr, posEdit.getText().toString(),
					edtContext.getText().toString());
			String text2 = getSendToText2(cr, posEdit.getText().toString(),
					commentEdit.getText().toString(),
					edtContext.getText().toString());
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, text1);
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text2);
			for (ResolveInfo resolveInfo : mCoolReader.getPackageManager().queryIntentActivities(emailIntent, 0)) {
				if( resolveInfo.activityInfo.packageName.contains(curDict.packageName)){
					emailIntent.setComponent(new ComponentName(
							resolveInfo.activityInfo.packageName,
							resolveInfo.activityInfo.name));
					try
					{
						mCoolReader.startActivity(emailIntent);
						onPositiveButtonClick();
					} catch ( ActivityNotFoundException e ) {
						mCoolReader.showToast("Dictionary \"" + curDict.name + "\" is not installed. "+e.getMessage());
					}
				}

			}
		});
		btnTransl.setOnClickListener(v -> {
			doTranslate(true, false, btnTransl);
		});
		btnTransl.setOnLongClickListener(v -> {
			doTranslate(true, true, btnTransl);
			return true;
		});
		btnTransl2.setOnClickListener(v -> {
			doTranslate(false, false, btnTransl2);
		});
		btnTransl2.setOnLongClickListener(v -> {
			doTranslate(false, true, btnTransl2);
			return true;
		});
		int selAction = -1;
		if (mCoolReader.getReaderView() != null) selAction = mCoolReader.getReaderView().mBookmarkActionSendTo;
		if (selAction == -1) Utils.hideView(btnSendTo2);
		else {
			int titleSendTo =  SelectionModesOption.getSelectionActionTitle(selAction);
			String sText = "[undefined]";
			Dictionaries.DictInfo curDict = OptionsDialog.getDicValue(mCoolReader.getString(titleSendTo), mCoolReader.settings(), mCoolReader);
			if (curDict != null) sText = curDict.name;
			//if (titleSendTo != 0) sText = OptionsDialog.updDicValue(mCoolReader.getString(titleSendTo), mCoolReader.settings(), mCoolReader, true);
			btnSendTo2.setText(sText);
			if (sText.endsWith("NONE")) Utils.hideView(btnSendTo2);
		}
		setView(view);
		btnFake.requestFocus();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	@Override
	protected void onThirdButtonClick() {
		mCoolReader.askConfirmation(R.string.win_title_confirm_bookmark_delete, () -> {
			mReaderView.removeBookmark(mBookmark);
			onNegativeButtonClick();
		});
	}

	@Override
	protected void onAddButtonClick() {
		commentEdit.setText(cbtextFull);
		onPositiveButtonClick();
	}

}