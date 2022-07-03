package org.coolreader.dic;

import android.view.View;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.utils.StrUtils;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.dic.struct.DictEntry;
import org.coolreader.dic.struct.Lemma;
import org.coolreader.dic.struct.TranslLine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.HttpUrl;

public class DictCCTranslate {

	public String dictccGetDefLangCode(String langCode) {
		String lc = StrUtils.getNonEmptyStr(langCode, true).toLowerCase();
		if (lc.length() > 2) return lc.substring(0,2);
		return lc;
	}

	public static final Logger log = L.create("cr3dict_dictcc");

	public void dictCCTranslate(CoolReader cr, String s, boolean fullScreen, String langf, String lang, Dictionaries.DictInfo curDict, View view,
								Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium), DicToastView.IS_DICTCC,
						"", curDict, fullScreen);
				return;
			}
			if ((StrUtils.isEmptyStr(lang)) || (StrUtils.isEmptyStr(langf))) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err),
								cr.getString(R.string.translate_lang_not_set) + ": ["
										+ langf + "] -> [" + lang + "]",
								DicToastView.IS_DICTCC, "", curDict, fullScreen)
						, 100));
				return;
			}
		}
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.DICTCC_DIC_ONLINE.
				replace("{langpair}", (langf+lang).toLowerCase())).newBuilder().addQueryParameter("s", s);
		BackgroundThread.instance().postBackground(() -> {
			try {
				if (llc == null) {
					String sTitle = "";
					DicStruct dsl = new DicStruct();
					Lemma lemma = null;
					Document docJsoup = Jsoup.parse(urlBuilder.build().url(), 180000); // three minutes
					if (StrUtils.getNonEmptyStr(docJsoup.text(), true).contains("language pair is not supported")) {
						sTitle = cr.getString(R.string.language_pair_not_supported) + " (" + langf + " -> " + lang + ")";
					} else {
						//Elements resultWords = docJsoup.select("td.td7nl > a, var");
						Elements resultTrs = docJsoup.select("tr");
						for (Element el : resultTrs) {
							Elements resultTds = el.select("td.td7nl");
							int i = 0;
							for (Element el1 : resultTds) {
								Elements texts = el1.select("a, var");
								String sText = "";
								for (Element t : texts) {
									sTitle = sTitle + t.text() + " ";
									sText = sText + t.text() + " ";
								}
								if (i % 2 == 0) {
									sTitle = sTitle + " -> ";
									lemma = new Lemma();
									DictEntry de = new DictEntry();
									de.dictLinkText = StrUtils.getNonEmptyStr(sText, true);
									lemma.dictEntries.add(de);
								} else {
									sTitle = sTitle + "; ";
									TranslLine tl = new TranslLine();
									sText = StrUtils.getNonEmptyStr(sText, true);
									if (sText.startsWith(";")) sText = sText.substring(1);
									sText = StrUtils.getNonEmptyStr(sText, true);
									tl.transText = StrUtils.getNonEmptyStr(sText, true);
									lemma.translLines.add(tl);
									dsl.lemmas.add(lemma);
								}
								i++;
							}

						}
					}
					String finalSTitle0 = sTitle;
					if (StrUtils.isEmptyStr(sTitle)) sTitle = cr.getString(R.string.not_found);
					String finalSTitle = sTitle;
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null) {
									if (!StrUtils.isEmptyStr(finalSTitle0))
										Dictionaries.saveToDicSearchHistory(cr, s, finalSTitle, curDict, dsl);
									if (dsl.lemmas.size() > 0) {
										cr.showDicToastExt(s, finalSTitle, DicToastView.IS_DICTCC,
												urlBuilder.build().url().toString(), curDict, dsl, fullScreen);
									} else
										cr.showDicToast(s, finalSTitle, DicToastView.IS_DICTCC,
												urlBuilder.build().url().toString(), curDict, fullScreen);
								} else {
									dcb.done(finalSTitle, Dictionaries.dslStructToString(dsl));
									if (dcb.showDicToast()) {
										cr.showDicToast(s, finalSTitle,
												DicToastView.IS_DICTCC, urlBuilder.build().url().toString(), curDict, fullScreen);
									}
									if (dcb.saveToHist())
										if (!StrUtils.isEmptyStr(finalSTitle0)) {
											Dictionaries.saveToDicSearchHistory(cr, s, finalSTitle, curDict, dsl);
										}
								}
							}, 100));
				} else {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.not_implemented),
								DicToastView.IS_DICTCC, "", curDict, fullScreen);
					}, 100));
				}
			} catch (Exception e) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					log.e(cr.getString(R.string.error)+": "+
							e.getClass().getSimpleName()+" "+e.getMessage());
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.error)+": "+
									e.getClass().getSimpleName()+" "+e.getMessage(),
							DicToastView.IS_DICTCC, "", curDict, fullScreen);
				}, 100));
			}
		});
	};

}
