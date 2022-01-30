package org.coolreader.userdic;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DicSearchHistoryEntry;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.util.HashMap;

public class UserDicEditDialog extends BaseDialog {

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final EditText dicWord;
	private final EditText dicWordTranslate;
	private final EditText udLang;
	private final TextView udSeenCount;
	private final TextView udCreateTime;
	private final TextView udLastAccessTime;
	private final EditText udContext;
	private final EditText udFullContext;

	private final Button btnIsUd;
	private final Button btnIsCite;
	private final UserDicDlg udDlg;

	private final boolean isDshe;
	private final int isCite;
	private final String word;
	private final String word_translate;

	private final UserDicEntry mUde;

	boolean isEInk;
	HashMap<Integer, Integer> themeColors;

	private boolean getCheckedFromTag(Object o) {
		if (o == null) return false;
		if (!(o instanceof String)) return false;
		if (o.equals("1")) return true;
		return false;
	}

	private void setCheckedTag(Button b) {
		if (b == null) return;
		btnIsUd.setTag("0");
		btnIsCite.setTag("0");
		b.setTag("1");
	}

	private void paintScopeButtons() {
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mCoolReader.tintViewIcons(btnIsUd, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(btnIsCite, PorterDuff.Mode.CLEAR,true);
		if (getCheckedFromTag(btnIsUd.getTag())) {
			btnIsUd.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnIsUd,true);
		} else btnIsUd.setBackgroundColor(colorGrayCT);
		if (getCheckedFromTag(btnIsCite.getTag())) {
			btnIsCite.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnIsCite,true);
		} else btnIsCite.setBackgroundColor(colorGrayCT);
	}

	public UserDicEditDialog(CoolReader activity, UserDicEntry ude, UserDicDlg udd) {
		super("UserDicEditDialog", activity, activity.getString(R.string.ud_title), true,
				false);
		setThirdButtonImage(
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_minus, R.drawable.icons8_minus)
				, R.string.mi_bookmark_delete);
		mUde = ude;
		udDlg = udd;
		isDshe = ude.getThisIsDSHE();
		isCite = ude.getIs_citation();
		word = ude.getDic_word();
		word_translate = StrUtils.getNonEmptyStr(ude.getDic_word_translate(), true);
		mCoolReader = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(activity, isEInk);
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.userdic_edit_dialog, null);
		dicWord = view.findViewById(R.id.dic_word);
		dicWord.setText(ude.getDic_word());
		dicWordTranslate = view.findViewById(R.id.dic_word_translate);
		dicWordTranslate.setText(ude.getDic_word_translate());
		udLang = view.findViewById(R.id.ud_lang);
		udLang.setText(ude.getLanguage());
		btnIsUd = view.findViewById(R.id.btn_is_ud);
		btnIsUd.setOnClickListener(v -> { setCheckedTag(btnIsUd); paintScopeButtons(); });
		btnIsCite = view.findViewById(R.id.btn_is_cite);
		btnIsCite.setOnClickListener(v -> { setCheckedTag(btnIsCite); paintScopeButtons(); });
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		if (ude.getIs_citation() == 1)
			btnIsCite.setTag("1");
		else
			btnIsUd.setTag("1");
		btnIsUd.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		btnIsCite.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		udSeenCount = view.findViewById(R.id.ud_seen_count);
		udSeenCount.setText(ude.getSeen_count() + "");
		udCreateTime = view.findViewById(R.id.ud_create_time);
		udCreateTime.setText(Utils.formatDate2(mCoolReader, ude.getCreate_time()) + " " +
				Utils.formatTime(mCoolReader, ude.getCreate_time()));
		udLastAccessTime = view.findViewById(R.id.ud_last_access_time);
		udLastAccessTime.setText(Utils.formatDate2(mCoolReader, ude.getLast_access_time()) + " " +
				Utils.formatTime(mCoolReader, ude.getLast_access_time()));
		udContext = view.findViewById(R.id.ud_context);
		udContext.setText(ude.getShortContext());
		udFullContext = view.findViewById(R.id.ud_full_context);
		udFullContext.setText(ude.getFullContext());
		paintScopeButtons();
		setView(view);
	}

	@Override
	protected void onPositiveButtonClick() {
		save();
		super.onPositiveButtonClick();
	}
	
	private void save() {
		mUde.setDic_word(dicWord.getText().toString().trim());
		mUde.setDic_word_translate(dicWordTranslate.getText().toString().trim());
		mUde.setLanguage(udLang.getText().toString().trim());
		mUde.setThisIsDSHE(false);
		mUde.setIs_citation(getCheckedFromTag(btnIsCite.getTag())? 1: 0);
		mUde.setShortContext(udContext.getText().toString().trim());
		mUde.setFullContext(udFullContext.getText().toString().trim());
		String wasKey = isCite + StrUtils.getNonEmptyStr(word, false);
		String nowKey = mUde.getIs_citation() + mUde.getDic_word();
		// It was dic search history entry, but now will be User Dic entry - delete old one, save new one
		if (isDshe) {
			DicSearchHistoryEntry dshe = new DicSearchHistoryEntry();
			dshe.setSearch_text(word);
			mCoolReader.getDB().updateDicSearchHistory(dshe, DicSearchHistoryEntry.ACTION_DELETE, mCoolReader);
			mCoolReader.getDB().saveUserDic(mUde, UserDicEntry.ACTION_NEW);
			if (!wasKey.equals(nowKey))
				mCoolReader.getmUserDic().remove(wasKey);
			mCoolReader.getmUserDic().put(nowKey, mUde);
		} else { // we'll update user dic entry
			if (!wasKey.equals(nowKey)) {
				mCoolReader.getmUserDic().remove(wasKey);
				UserDicEntry delUde = new UserDicEntry();
				delUde.setDic_word(word);
				delUde.setDic_word_translate(word_translate);
				delUde.setIs_citation(isCite);
				mCoolReader.getDB().saveUserDic(delUde, UserDicEntry.ACTION_DELETE);
			}
			mCoolReader.getDB().saveUserDic(mUde, UserDicEntry.ACTION_NEW);
			mActivity.getmUserDic().put(mUde.getIs_citation()+mUde.getDic_word(), mUde);
		}
		mCoolReader.updateUserDicWords();
		if (udDlg != null) {
			udDlg.listUpdated();
			udDlg.checkedCallback(null);
		}
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	@Override
	protected void onThirdButtonClick() {
		mCoolReader.askConfirmation(R.string.win_title_confirm_ude_delete, (Runnable) () -> {
			if (mUde.getThisIsDSHE()) {
				DicSearchHistoryEntry dshe = new DicSearchHistoryEntry();
				dshe.setSearch_text(mUde.getDic_word());
				dshe.setText_translate(mUde.getDic_word_translate());
				mCoolReader.getDB().updateDicSearchHistory(dshe, DicSearchHistoryEntry.ACTION_DELETE, mCoolReader);
			}
			mCoolReader.getDB().saveUserDic(mUde, UserDicEntry.ACTION_DELETE);
			mCoolReader.getmUserDic().remove(mUde.getIs_citation()+mUde.getDic_word());
			mCoolReader.updateUserDicWords();
			if (udDlg != null) udDlg.listUpdated();
		});
		super.onThirdButtonClick();
	}

	
}
