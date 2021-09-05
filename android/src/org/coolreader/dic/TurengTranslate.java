package org.coolreader.dic;

import android.view.View;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.StrUtils;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.dic.struct.DictEntry;
import org.coolreader.dic.struct.Lemma;
import org.coolreader.dic.struct.TranslLine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.HttpUrl;

public class TurengTranslate {

	public String getTurengLangPair(String langf, String lang) {
		if ((StrUtils.getNonEmptyStr(langf,true).toLowerCase().startsWith("tr")) ||
			(StrUtils.getNonEmptyStr(lang,true).toLowerCase().startsWith("tr")))
				return "turkish-english";
		if ((StrUtils.getNonEmptyStr(langf,true).toLowerCase().startsWith("de")) ||
				(StrUtils.getNonEmptyStr(lang,true).toLowerCase().startsWith("de")))
				return "german-english";
		if ((StrUtils.getNonEmptyStr(langf,true).toLowerCase().startsWith("es")) ||
				(StrUtils.getNonEmptyStr(lang,true).toLowerCase().startsWith("es")))
			return "spanish-english";
		if ((StrUtils.getNonEmptyStr(langf,true).toLowerCase().startsWith("fr")) ||
				(StrUtils.getNonEmptyStr(lang,true).toLowerCase().startsWith("fr")))
			return "french-english";
		if (StrUtils.getNonEmptyStr(langf,true).toLowerCase().startsWith("en"))
			return "english-synonym";
		return "";
	}

	public static final Logger log = L.create("cr3dict_tureng");
	public void turengTranslate(CoolReader cr, String s, String langf, String lang, Dictionaries.DictInfo curDict, View view,
								Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		String langPair = getTurengLangPair(langf, lang);
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium), DicToastView.IS_TURENG, "");
				return;
			}
			if (StrUtils.isEmptyStr(langPair)) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err),
								cr.getString(R.string.translate_lang_not_set) + ": ["
										+ langf + "] -> [" + lang + "]",
								DicToastView.IS_TURENG, "")
						, 100));
				return;
			}
		}
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.TURENG_ONLINE.
				replace("{langpair}", langPair)).newBuilder().addEncodedPathSegment(s);
		BackgroundThread.instance().postBackground(() -> {
			try {
				if (llc == null) {
					String sTitle = "";
					DicStruct dsl = new DicStruct();
					Lemma lemma = null;
					Document docJsoup = Jsoup.parse(urlBuilder.build().url(), 180000); // three minutes
					Elements resTables = docJsoup.select("table#englishResultsTable");
					for (Element tb: resTables) {
						Elements trs = tb.select("tr");
						for (Element tr: trs) {
							Elements tds = tr.select("td");
							int i = 0;
							String categ = "";
							String lang1 = "";
							String val1 = "";
							String lang2 = "";
							String val2 = "";
							for (Element td: tds) {
								if (i == 1) categ = td.text();
								if (i == 2) {
									lang1 = td.attr("lang");
									val1 = td.text();
								}
								if (i == 3) {
									lang2 = td.attr("lang");
									val2 = td.text();
								}
								i++;
							}
							if ((!StrUtils.isEmptyStr(val1)) &&
								(!StrUtils.isEmptyStr(val2))) {
								lemma = new Lemma();
								DictEntry de = new DictEntry();
								de.dictLinkText = StrUtils.getNonEmptyStr(val1, true);
								de.tagType = StrUtils.getNonEmptyStr(lang1, true);
								lemma.dictEntry.add(de);
								TranslLine tl = new TranslLine();
								tl.transText = StrUtils.getNonEmptyStr(val2, true);
								tl.transGroup = categ;
								tl.transType = lang2;
								lemma.translLine.add(tl);
								dsl.lemmas.add(lemma);
								sTitle = sTitle + "; " + val1 +
										(StrUtils.isEmptyStr(lang1)?"": ("(" + lang1 + ") = "))
										+ val2 +
										(StrUtils.isEmptyStr(lang2)?"": ("(" + lang2 + ") = "));
							}
						}
					}
					if (StrUtils.getNonEmptyStr(sTitle, true).startsWith("; "))
						sTitle = sTitle.substring(2);
					final String finalSTitle = sTitle;
					BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> {
							if (dcb == null) {
								if (!StrUtils.isEmptyStr(finalSTitle))
									Dictionaries.saveToDicSearchHistory(cr, s, finalSTitle, curDict);
								if (dsl.lemmas.size() > 0) {
									cr.showDicToastExt(s, s, DicToastView.IS_TURENG, urlBuilder.build().url().toString(), curDict, dsl);
								} else
									cr.showDicToast(s, cr.getString(R.string.not_found), DicToastView.IS_TURENG, urlBuilder.build().url().toString());
							} else {
								dcb.done(s);
								if (dcb.showDicToast()) {
									cr.showDicToast(s, finalSTitle, DicToastView.IS_TURENG, urlBuilder.build().url().toString());
								}
								if (dcb.saveToHist())
									if (!StrUtils.isEmptyStr(finalSTitle)) {
										Dictionaries.saveToDicSearchHistory(cr, s, finalSTitle, curDict);
									}
							}
						}, 100));
				} else {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.not_implemented),
								DicToastView.IS_TURENG, "");
					}, 100));
				}
			} catch (Exception e) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					log.e(cr.getString(R.string.error)+": "+
							e.getClass().getSimpleName()+" "+e.getMessage());
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.error)+": "+
									e.getClass().getSimpleName()+" "+e.getMessage(),
							DicToastView.IS_TURENG, "");
				}, 100));
			}
		});
	};

}
