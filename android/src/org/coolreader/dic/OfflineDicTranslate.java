package org.coolreader.dic;

import android.view.View;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.utils.StrUtils;

import java.util.ArrayList;

public class OfflineDicTranslate {

	public static final Logger log = L.create("cr3dict_offlinedict");

	ArrayList<OfflineDicInfo> offlineDictInfoList = null;

	public String offlineDictGetDefLangCode(String langCode) {
		String lc = StrUtils.getNonEmptyStr(langCode, true).toLowerCase();
		if (lc.length() > 2) return lc.substring(0,2);
		return lc;
	}

//	public void findInOfflineDict1Dic(DicStruct dsl, OfflineDicInfo sdil, String searchStr) {
//		if (((sdil.dslExists) || (sdil.dslDzExists)) && (sdil.dslDic != null)) {
//			try {
//				PlainDslVisitor plainDslVisitor = new PlainDslVisitor();
//				//HtmlDslVisitor htmlDslVisitor = new HtmlDslVisitor();
//				DslResult dslResult = sdil.dslDic.lookupPredictive(searchStr);
//				for (Map.Entry<String, String> entry : dslResult.getEntries(plainDslVisitor)) {
//					Lemma le = new Lemma();
//					DictEntry de = new DictEntry();
//					de.dictLinkText = entry.getKey();
//					le.dictEntry.add(de);
//					TranslLine translLine = new TranslLine();
//					translLine.transText = entry.getValue();
//					translLine.transGroup = sdil.dicName;
//					le.translLine.add(translLine);
//					dsl.lemmas.add(le);
//				}
//			} catch (Exception e) {
//				log.w("Dictionary lookup failed: " + sdil.dicPath + "\n" + e.getMessage());
//			}
//		}
//	}
//
//	Long lastMod = 0L;
//
//	public DicStruct findInOfflineDictDic(String searchStr, String langFrom, String langTo) {
//		DicStruct dsl = new DicStruct();
//		ArrayList<String> tDirs1 = Engine.getDataDirsExt(Engine.DataDirType.OfflineDicsDirs, true);
//		if (tDirs1.size()>0) {
//			File f = new File(tDirs1.get(0) + "/dic_conf.json");
//			if (!f.exists()) return dsl;
//			long newLastMod = f.lastModified();
//			if (newLastMod != lastMod) {
//				String sdicsConf = Utils.readFileToStringOrEmpty(tDirs1.get(0) + "/dic_conf.json");
//				if (!StrUtils.isEmptyStr(sdicsConf))
//					try {
//						offlineDictInfoList = new ArrayList<>(StrUtils.stringToArray(sdicsConf, OfflineDicInfo[].class));
//						for (OfflineDicInfo odi : offlineDictInfoList) odi.fillMarks();
//					} catch (Exception e) {
//					}
//				lastMod = newLastMod;
//			}
//		}
//
//		if (offlineDictInfoList != null)
//			for (OfflineDicInfo sdil: offlineDictInfoList) {
//				String langFromD = StrUtils.getNonEmptyStr(sdil.langFrom.toUpperCase(), true);
//				String langToD = StrUtils.getNonEmptyStr(sdil.langTo.toUpperCase(), true);
//				if (langFromD.length() > 2) langFromD = langFromD.substring(0,2);
//				if (langToD.length() > 2) langToD = langToD.substring(0,2);
//				String langFromS = StrUtils.getNonEmptyStr(langFrom.toUpperCase(), true);
//				String langToS = StrUtils.getNonEmptyStr(langTo.toUpperCase(), true);
//				if (langFromS.length() > 2) langFromS = langFromS.substring(0,2);
//				if (langToS.length() > 2) langToS = langToS.substring(0,2);
//				boolean proceed = langFromD.equals(langFromS) || StrUtils.isEmptyStr(langFromD) || StrUtils.isEmptyStr(langFromS);
//				proceed = proceed && (langToD.equals(langToS) || StrUtils.isEmptyStr(langToD) || StrUtils.isEmptyStr(langToS));
//				if ((sdil.dicEnabled) && (proceed)) {
//					if ((sdil.dslExists) || (sdil.dslDzExists)) {
//						if (sdil.dslDic == null) {
//							try {
//								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//									if (sdil.dslDzExists)
//										sdil.dslDic = DslDictionary.loadDictionary(
//												Paths.get(sdil.dicPath + "/" + sdil.dicNameWOExt + ".dsl.dz"),
//												Paths.get(sdil.dicPath + "/" + sdil.dicNameWOExt + ".idx")
//										);
//									else
//										sdil.dslDic = DslDictionary.loadDictionary(
//												Paths.get(sdil.dicPath + "/" + sdil.dicNameWOExt + ".dsl"),
//												Paths.get(sdil.dicPath + "/" + sdil.dicNameWOExt + ".idx")
//										);
//								}
//							} catch (Exception e) {
//								log.e("Lingvo_DSL index create error: " + e.getMessage());
//							}
//						}
//						if (sdil.dslDic != null) findInOfflineDict1Dic(dsl, sdil, searchStr);
//					}
//				}
//			}
//		return dsl;
//	}

	public void offlineDicTranslate(CoolReader cr, String s, String langf, String lang, Dictionaries.DictInfo curDict, View view,
						boolean extended, Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium),
						DicToastView.IS_OFFLINE, "Offline dic");
				return;
			}
		}
		//DicStruct lingvoDSL = findInOfflineDictDic(s, langf, lang);
		//log.i(lingvoDSL.getFirstTranslation());
		cr.getDB().findInOfflineDictDic(s, langf, lang, extended, o -> {
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
