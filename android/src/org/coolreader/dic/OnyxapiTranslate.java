package org.coolreader.dic;

import android.view.View;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.onyx.OnyxTranslate;
import org.coolreader.utils.StrUtils;

public class OnyxapiTranslate {

	CoolReader.DictionaryCallback mDcb;
	boolean mFullScreen;
	String mKeyword = "";
	Dictionaries.DictInfo mCurDict;
	OnyxTranslate onyxTranslate;

	public static final Logger log = L.create("cr3dict_onyxapi");

	public void onyxapiTranslate(CoolReader cr, String s, boolean fullScreen,
									String langf, String lang, Dictionaries.DictInfo curDict, View view,
									boolean extended,
									Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		mDcb = dcb;
		mFullScreen = fullScreen;
		mKeyword = s;
		mCurDict = curDict;
		if (onyxTranslate == null) onyxTranslate = new OnyxTranslate();
		onyxTranslate.doKeywordQueryJob(cr, s);
	};

	public void onyxapiTranslateFinish(CoolReader cr, DicStruct dsl) {
		final String sTitle = cr.getString(R.string.not_found);
		BackgroundThread.instance().postBackground(() ->
				BackgroundThread.instance().postGUI(() -> {
					if (mDcb == null) {
						boolean isEmpty = dsl.getCount() == 0;
						if (!isEmpty) {
							if (dsl.lemmas.size() == 1)
								if (StrUtils.isEmptyStr(dsl.lemmas.get(0).lemmaText))
									isEmpty = true;
						}
						if (isEmpty) {
							cr.showDicToast(mKeyword, sTitle, DicToastView.IS_ONYXAPI, "OnyxAPI dic", mCurDict, mFullScreen);
						} else {
							Dictionaries.saveToDicSearchHistory(cr, mKeyword, dsl.getFirstTranslation(), mCurDict, dsl);
							cr.showDicToastExt(mKeyword, sTitle, DicToastView.IS_ONYXAPI, "OnyxAPI dic",
									mCurDict, dsl, mFullScreen);
						}
					} else {
						mDcb.done(dsl.getFirstTranslation(), Dictionaries.dslStructToString(dsl));
						if (mDcb.showDicToast()) {
							if (dsl.getCount() == 0) {
								cr.showDicToast(mKeyword, sTitle, DicToastView.IS_ONYXAPI, "Offline dic", mCurDict, mFullScreen);
							} else {
								cr.showDicToastExt(mKeyword, sTitle, DicToastView.IS_ONYXAPI, "Offline dic",
										mCurDict, dsl, mFullScreen);
							}
						}
						if (mDcb.saveToHist())
							if (dsl.getCount() == 0) {
								Dictionaries.saveToDicSearchHistory(cr, mKeyword, dsl.getFirstTranslation(), mCurDict, dsl);
							}
					}
				}, 100));
	}

}
