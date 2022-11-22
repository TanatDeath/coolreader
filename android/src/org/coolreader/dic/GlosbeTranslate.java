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
import org.coolreader.dic.struct.ExampleLine;
import org.coolreader.dic.struct.Lemma;
import org.coolreader.dic.struct.TranslLine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;

import okhttp3.HttpUrl;

public class GlosbeTranslate {

	public static final Logger log = L.create("cr3dict_glosbe");

	public static HttpUrl.Builder getSearchUrl(String query, String src, String dst) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.GLOSBE_ONLINE
				.replace("{src_lang}", src)
				.replace("{dst_lang}", dst)
		).newBuilder()
				.addEncodedPathSegment(query);
		return urlBuilder;
	}

	public String glosbeGetDefLangCode(String langCode) {
		String lc = StrUtils.getNonEmptyStr(langCode, true).toLowerCase();
		if (lc.length() > 2) return lc.substring(0,2);
		return lc;
	}

	public void glosbeTranslate(CoolReader cr, String s, boolean fullScreen,
								String langf, String lang, Dictionaries.DictInfo curDict, View view,
								Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium),
						DicToastView.IS_GLOSBE, "", curDict, fullScreen);
				return;
			}
			if ((StrUtils.isEmptyStr(lang)) || (StrUtils.isEmptyStr(langf))) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err),
								cr.getString(R.string.translate_lang_not_set) + ": ["
										+ langf + "] -> [" + lang + "]",
								DicToastView.IS_GLOSBE, "", curDict, fullScreen), 100));
				return;
			}
		}
		HttpUrl.Builder urlBuilder = getSearchUrl(s, langf, lang);
		BackgroundThread.instance().postBackground(() -> {
			try {
				if (llc == null) {
					URL url = new URL(urlBuilder.toString());
					Document doc = Jsoup.parse(url, 5000);

					DicStruct dsl = new DicStruct();
					dsl.srcText = s;

					Elements phraseTs = doc.select("div.phrase__translation__section");

					for (Element phraseT: phraseTs) {
						Lemma le = new Lemma();
						Elements uls = phraseT.select("ul.translations__list");
						for (Element ul: uls) {
							Elements lis = ul.select("li");
							for (Element li: lis) {
								Elements h3s = li.select("h3.translation");
								String deText = "";
								for (Element h3 : h3s) {
									deText = StrUtils.getNonEmptyStr(h3.text(),true);
								}
								Elements spans = li.select("span.phrase__summary__field");
								String deType = "";
								for (Element span : spans) {
									deType = deType + ", " + StrUtils.getNonEmptyStr(span.text(), true);
								}
								if (deType.startsWith(", ")) deType = deType.substring(2);
								if (!StrUtils.isEmptyStr(deText)) {
									DictEntry de = new DictEntry();
									de.dictLinkText = deText;
									de.tagType = deType;
									le.dictEntries.add(de);
								}
								Elements ps = li.select("p.translation__definition");
								TranslLine lastTL = null;
								for (Element p : ps) {
									TranslLine tl = new TranslLine();
									String langT = "";
									Elements spansLang = p.select("span.translation__definition__language");
									for (Element sl: spansLang) langT = sl.text();
									String def = p.text();
									if (def.startsWith(langT + " ")) def = def.substring(langT.length()).trim();
									tl.transText = def;
									if (!StrUtils.isEmptyStr(langT)) tl.transText = tl.transText + ", " + langT;
									lastTL = tl;
									le.translLines.add(tl);
								}
								if (lastTL != null) {
									Elements divExs = li.select("div.translation__example");
									for (Element divEx : divExs) {
										ExampleLine el = new ExampleLine(StrUtils.getNonEmptyStr(divEx.text(), true));
										lastTL.exampleLines.add(el);
									}
								}
							}
						}
						if ((le.translLines.size()>0) || (le.dictEntries.size()>0)) dsl.lemmas.add(le);
					}
					Elements examples = doc.select("div#examples");
					for (Element example: examples) {
						Elements exLs = example.select("div[class~=w-1.*pr-2.*]");
						for (Element exL: exLs) {
							dsl.elementsL.add(StrUtils.getNonEmptyStr(exL.text(), true));
						}
						Elements exRs = example.select("div[class~=w-1.*pl-2.*]");
						for (Element exR: exRs) {
							dsl.elementsR.add(StrUtils.getNonEmptyStr(exR.text(), true));
						}
					}

					final String sTitle = cr.getString(R.string.not_found);
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null) {
									if (dsl.getCount() == 0) {
										cr.showDicToast(s, sTitle, DicToastView.IS_GLOSBE,
												urlBuilder.build().url().toString(), curDict, fullScreen);
									} else {
										Dictionaries.saveToDicSearchHistory(cr, s, dsl.getFirstTranslation(), curDict, dsl);
										cr.showDicToastExt(s, sTitle, DicToastView.IS_GLOSBE,
												urlBuilder.build().url().toString(), curDict, dsl, fullScreen);
									}
								} else {
									dcb.done(dsl.getFirstTranslation(), Dictionaries.dslStructToString(dsl));
									if (dcb.showDicToast()) {
										if (dsl.getCount() == 0) {
											cr.showDicToast(s, sTitle, DicToastView.IS_GLOSBE,
													urlBuilder.build().url().toString(), curDict, fullScreen);
										} else {
											cr.showDicToastExt(s, sTitle, DicToastView.IS_GLOSBE,
													urlBuilder.build().url().toString(), curDict, dsl, fullScreen);
										}
									}
									if (dcb.saveToHist())
										if (dsl.getCount() == 0) {
											Dictionaries.saveToDicSearchHistory(cr, s, dsl.getFirstTranslation(), curDict, dsl);
										}
								}
							}, 100));
				} else {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.not_implemented),
								DicToastView.IS_GLOSBE, "", curDict, fullScreen);
					}, 100));
				}
			} catch (Exception e) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					log.e(cr.getString(R.string.error)+": "+
							e.getClass().getSimpleName()+" "+e.getMessage());
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.error)+": "+
									e.getClass().getSimpleName()+" "+e.getMessage(),
							DicToastView.IS_GLOSBE, "", curDict, fullScreen);
				}, 100));
			}
		});
	};

}
