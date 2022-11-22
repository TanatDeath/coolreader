package org.coolreader.dic;

import android.view.View;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.dic.struct.DictEntry;
import org.coolreader.dic.struct.ExampleLine;
import org.coolreader.dic.struct.Lemma;
import org.coolreader.dic.struct.SuggestionLine;
import org.coolreader.dic.struct.TranslLine;
import org.coolreader.utils.StrUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;

public class ReversoTranslate {

	public static final Logger log = L.create("cr3dict_reverso");

	static String[] LANGUAGE_CODE = {
			"ar",
			"de",
			"es",
			"fr",
			"he",
			"it",
			"ja",
			"nl",
			"pl",
			"pt",
			"ro",
			"ru",
			"tr",
			"zh",
			"en",
			"ua"
	};

	public static Map<String, String> LANGUAGES;
	static {
		LANGUAGES = new HashMap<>();
		LANGUAGES.put("ar", "arabic");
		LANGUAGES.put("de", "german");
		LANGUAGES.put("es", "spanish");
		LANGUAGES.put("fr", "french");
		LANGUAGES.put("he", "hebrew");
		LANGUAGES.put("it", "italian");
		LANGUAGES.put("ja", "japanese");
		LANGUAGES.put("nl", "dutch");
		LANGUAGES.put("pl", "polish");
		LANGUAGES.put("pt", "portuguese");
		LANGUAGES.put("ro", "romanian");
		LANGUAGES.put("ru", "russian");
		LANGUAGES.put("tr", "turkish");
		LANGUAGES.put("zh", "chinese");
		LANGUAGES.put("en", "english");
		LANGUAGES.put("ua", "ukrainian");
	}

	public static boolean lastFullScreen;
	public static String lastLangf;
	public static String lastLang;
	public static Dictionaries.DictInfo lastCurDict;
	public static View lastView;
	public static Dictionaries.LangListCallback lastLlc;
	public static CoolReader.DictionaryCallback lastDcb;

	public static HttpUrl.Builder getSearchUrl(String query, String src, String dst) {
		String srcLangName = LANGUAGES.get(src);
		if (srcLangName == null) srcLangName = StrUtils.getNonEmptyStr(src, true);
		String dstLangName = LANGUAGES.get(dst);
		if (dstLangName == null) dstLangName = StrUtils.getNonEmptyStr(dst, true);
		String q = StrUtils.getNonEmptyStr(query, true);
		String fm = "";
		if (q.contains("#")) {
			fm = q.split("#")[1];
			q = q.split("#")[0];
		}
		HttpUrl.Builder urlBuilder = HttpUrl.parse(
				Dictionaries.REVERSO_DIC_ONLINE
						.replace("{src_lang_name}", srcLangName)
						.replace("{dst_lang_name}", dstLangName)
		).newBuilder();
		if (StrUtils.isEmptyStr(fm))
			urlBuilder = urlBuilder.addPathSegment(q);
		else
			urlBuilder = urlBuilder.addPathSegment(q).fragment(fm);
		return urlBuilder;
	}

	public String reversoGetDefLangCode(String langCode) {
		String lc = StrUtils.getNonEmptyStr(langCode, true).toLowerCase();
		if (lc.length() > 2) return lc.substring(0,2);
		return lc;
	}

	public void reversoTranslateAsLast(CoolReader cr, String s, String topicHref) {
		try {
			if (lastLangf == null)
				lastLangf = cr.getReaderView().getBookInfo().getFileInfo().lang_from;
			if (lastLang == null)
				lastLang = cr.getReaderView().getBookInfo().getFileInfo().lang_to;
			if (lastCurDict == null) lastCurDict = Dictionaries.findById("Reveso context (online)",
					cr);
		} catch (Exception e) {

		}
		reversoTranslate(cr, s, lastFullScreen,
		lastLangf, lastLang, lastCurDict, lastView,
		topicHref,
		lastLlc, lastDcb);
	}

	public void reversoTranslate(CoolReader cr, String s, boolean fullScreen,
			 String langf, String lang, Dictionaries.DictInfo curDict, View view,
			 String sTopicHref,
			 Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb)  {
		lastFullScreen = fullScreen;
		lastLangf = langf;
		lastLang = lang;
		lastCurDict = curDict;
		lastView = view;
		lastLlc = llc;
		lastDcb = dcb;
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium),
						DicToastView.IS_REVERSO, "", curDict, fullScreen);
				return;
			}
			if ((StrUtils.isEmptyStr(lang)) || (StrUtils.isEmptyStr(langf))) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err),
								cr.getString(R.string.translate_lang_not_set) + ": ["
										+ langf + "] -> [" + lang + "]",
								DicToastView.IS_REVERSO, "", curDict, fullScreen), 100));
				return;
			}
		}
		HttpUrl.Builder urlBuilder;
		if (StrUtils.isEmptyStr(sTopicHref))
			urlBuilder = getSearchUrl(s, langf, lang);
		else {
			HttpUrl httpUrl = HttpUrl.parse(Dictionaries.REVERSO_DIC_ONLINE_ROOT + sTopicHref);
			if (httpUrl == null) {
				cr.showToast(R.string.error);
				return;
			}
			urlBuilder = httpUrl.newBuilder();
		}
		BackgroundThread.instance().postBackground(() -> {
			try {
				if (llc == null) {
					URL url = new URL(urlBuilder.toString());
					Document doc = Jsoup.parse(url, 5000);

					DicStruct dsl = new DicStruct();
					dsl.srcText = s;
					Lemma le = new Lemma();
					DictEntry de = new DictEntry();
					de.dictLinkText = "Reverso Context";
					le.dictEntries.add(de);

					Elements translations_content = doc.select("div#translations-content");
					for (Element tr_content: translations_content) {
						// links
						Elements tr_links = tr_content.select("a.translation");
						for (Element tr_l: tr_links) {
							//System.out.println(tr_l.attr("href"));
							// words
							Elements translations = tr_l.select("span.display-term");
							// types
							Elements pos_marks = tr_l.select("div.pos-mark > span");
							for (int i=0; i < translations.size(); i++) {
								Element tr = translations.get(i);
								System.out.println(tr.text());
								TranslLine tl = new TranslLine();
								tl.transText = tr.text();
								tl.transLink = tr_l.attr("href");
								if (!StrUtils.isEmptyStr(tl.transLink))
									tl.canSwitchTo = true; // we can switch to this word
								if (pos_marks.size()>i)
									tl.transType = pos_marks.get(i).attr("title");
								le.translLines.add(tl);
							}
						}
					}
					dsl.lemmas.add(le);
					// Suggestions - note! need remove duplicates
					Elements suggestions_content = doc.select("div.suggestions-content");
					for (Element sugg_c: suggestions_content) {
						Elements suggs = sugg_c.select("div.suggestion");
						for (Element sugg: suggs) {
							Elements suggs_a = sugg.select("a");
							for (Element sugg_a: suggs_a) {
								SuggestionLine suggl = new SuggestionLine();
								suggl.suggText = sugg_a.text();
								suggl.suggLink = sugg_a.attr("href");
								dsl.suggs.add(suggl);
							}
						}
					}
					// examples
					Elements examples_content = doc.select("section#examples-content");
					for (Element examples_c: examples_content) {
						Elements examples = examples_c.select("div.example");
						for (Element example: examples) {
							Elements src = example.select("div.src");
							Elements trg = example.select("div.trg");
							dsl.elementsL.add(src.text());
							dsl.elementsR.add(trg.text());
						}
					}
					final String sTitle = cr.getString(R.string.not_found);
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null) {
									if (dsl.getCount() == 0) {
										cr.showDicToast(s, sTitle, DicToastView.IS_REVERSO,
												urlBuilder.build().url().toString(), curDict, fullScreen);
									} else {
										Dictionaries.saveToDicSearchHistory(cr, s, dsl.getFirstTranslation(), curDict, dsl);
										cr.showDicToastExt(s, sTitle, DicToastView.IS_REVERSO,
												urlBuilder.build().url().toString(), curDict, dsl, fullScreen);
									}
								} else {
									dcb.done(dsl.getFirstTranslation(), Dictionaries.dslStructToString(dsl));
									if (dcb.showDicToast()) {
										if (dsl.getCount() == 0) {
											cr.showDicToast(s, sTitle, DicToastView.IS_REVERSO,
													urlBuilder.build().url().toString(), curDict, fullScreen);
										} else {
											cr.showDicToastExt(s, sTitle, DicToastView.IS_REVERSO,
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
								DicToastView.IS_REVERSO, "", curDict, fullScreen);
					}, 100));
				}
			} catch (Exception e) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					log.e(cr.getString(R.string.error)+": "+
							e.getClass().getSimpleName()+" "+e.getMessage());
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.error)+": "+
									e.getClass().getSimpleName()+" "+e.getMessage(),
							DicToastView.IS_REVERSO, "", curDict, fullScreen);
				}, 100));
			}
		});
	}

}
