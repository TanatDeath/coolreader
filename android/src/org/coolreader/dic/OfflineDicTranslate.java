package org.coolreader.dic;

import android.view.View;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.dic.DicToastView;
import org.coolreader.dic.Dictionaries;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.utils.StrUtils;

public class OfflineDicTranslate {

	public static final Logger log = L.create("cr3dict_stardict");

	public String starDictGetDefLangCode(String langCode) {
		String lc = StrUtils.getNonEmptyStr(langCode, true).toLowerCase();
		if (lc.length() > 2) return lc.substring(0,2);
		return lc;
	}

	public void starDictTranslate(CoolReader cr, String s, String langf, String lang, Dictionaries.DictInfo curDict, View view,
								 Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium),
						DicToastView.IS_OFFLINE, "Offline dic");
				return;
			}
		}
		cr.getDB().findInStarDictDic(s, o -> {
			if (o != null) {
				final String sTitle = cr.getString(R.string.not_found);
				BackgroundThread.instance().postBackground(() ->
					BackgroundThread.instance().postGUI(() -> {
						DicStruct dsl = (DicStruct) o;
						if (dcb == null) {
							if (dsl.getCount() == 0) {
								cr.showDicToast(s, sTitle, DicToastView.IS_OFFLINE, "Offline dic");
							} else {
								Dictionaries.saveToDicSearchHistory(cr, s, dsl.getFirstTranslation(), curDict);
								cr.showDicToastExt(s, sTitle, DicToastView.IS_OFFLINE, "Offline dic", curDict, dsl);
							}
						} else {
							dcb.done(dsl.getFirstTranslation());
							if (dcb.showDicToast()) {
								if (dsl.getCount() == 0) {
									cr.showDicToast(s, sTitle, DicToastView.IS_OFFLINE, "Offline dic");
								} else {
									cr.showDicToastExt(s, sTitle, DicToastView.IS_OFFLINE, "Offline dic", curDict, dsl);
								}
							}
							if (dcb.saveToHist())
								if (dsl.getCount() == 0) {
									Dictionaries.saveToDicSearchHistory(cr, s, dsl.getFirstTranslation(), curDict);
								}
						}
					}, 100));
			}
		});
	};

}
