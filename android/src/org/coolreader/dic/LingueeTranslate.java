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
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;

public class LingueeTranslate {

	public static final Logger log = L.create("cr3dict_linguee");

	static String[] LANGUAGE_CODE = {
			"bg",
			"cs",
			"da",
			"de",
			"el",
			"en",
			"es",
			"et",
			"fi",
			"fr",
			"hu",
			"it",
			"ja",
			"lt",
			"lv",
			"mt",
			"nl",
			"pl",
			"pt",
			"ro",
			"ru",
			"sk",
			"sl",
			"sv",
			"zh"
	};

	public static Map<String, String> LANGUAGES;
	static {
		LANGUAGES = new HashMap<>();
		LANGUAGES.put("bg", "bulgarian");
		LANGUAGES.put("cs", "czech");
		LANGUAGES.put("da", "danish");
		LANGUAGES.put("de", "german");
		LANGUAGES.put("el", "greek");
		LANGUAGES.put("en", "english");
		LANGUAGES.put("es", "spanish");
		LANGUAGES.put("et", "estonian");
		LANGUAGES.put("fi", "finnish");
		LANGUAGES.put("fr", "french");
		LANGUAGES.put("hu", "hungarian");
		LANGUAGES.put("it", "italian");
		LANGUAGES.put("ja", "japanese");
		LANGUAGES.put("lt", "lithuanian");
		LANGUAGES.put("lv", "latvian");
		LANGUAGES.put("mt", "maltese");
		LANGUAGES.put("nl", "dutch");
		LANGUAGES.put("pl", "polish");
		LANGUAGES.put("pt", "portuguese");
		LANGUAGES.put("ro", "romanian");
		LANGUAGES.put("ru", "russian");
		LANGUAGES.put("sk", "slovak");
		LANGUAGES.put("sl", "slovene");
		LANGUAGES.put("sv", "swedish");
		LANGUAGES.put("zh", "chinese");
	}

	public static HttpUrl.Builder getSearchUrl(String query, String src, String dst, boolean guessDirection) {
		String srcLangName = LANGUAGES.get(src);
		if (srcLangName == null) srcLangName = StrUtils.getNonEmptyStr(src, true);
		String dstLangName = LANGUAGES.get(dst);
		if (dstLangName == null) dstLangName = StrUtils.getNonEmptyStr(dst, true);
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.LINGUEE_DIC_ONLINE
				.replace("{src_lang_name}", srcLangName)
				.replace("{dst_lang_name}", dstLangName)
		).newBuilder()
				.addQueryParameter("query", query)
				.addQueryParameter("ajax", "1");
		if (!guessDirection) urlBuilder.addQueryParameter("source", src);
		return urlBuilder;
	}

	public static HttpUrl.Builder getAutocompletionsUrl(String query, String src, String dst) {
		String srcLangName = LANGUAGES.get(src);
		String dstLangName = LANGUAGES.get(dst);
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.LINGUEE_DIC_ONLINE
				.replace("{src_lang_name}", srcLangName)
				.replace("{dst_lang_name}", dstLangName)
		).newBuilder()
				.addQueryParameter("qe", query);
		return urlBuilder;
	}

	public String lingueeGetDefLangCode(String langCode) {
		String lc = StrUtils.getNonEmptyStr(langCode, true).toLowerCase();
		if (lc.length() > 2) return lc.substring(0,2);
		return lc;
	}

	public void lingueeTranslate(CoolReader cr, String s, boolean fullScreen,
								 String langf, String lang, Dictionaries.DictInfo curDict, View view,
								 Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium),
						DicToastView.IS_LINGUEE, "", fullScreen);
				return;
			}
			if ((StrUtils.isEmptyStr(lang)) || (StrUtils.isEmptyStr(langf))) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err),
								cr.getString(R.string.translate_lang_not_set) + ": ["
										+ langf + "] -> [" + lang + "]",
								DicToastView.IS_LINGUEE, "", fullScreen), 100));
				return;
			}
		}
		HttpUrl.Builder urlBuilder = getSearchUrl(s, langf, lang, (langf.equals("*") || (langf.equals("auto")))? true: false);
		BackgroundThread.instance().postBackground(() -> {
			try {
				if (llc == null) {
					URL url = new URL(urlBuilder.toString());
					Document doc = Jsoup.parse(url, 5000);

					DicStruct dsl = new DicStruct();

					Elements line_lemma_descs = doc.select("h2.line.lemma_desc");
					for (Element line_lemma_desc : line_lemma_descs) {
						Element par = line_lemma_desc.parent();
						Elements line_lemma_descs_p = par.select("h2.line.lemma_desc");
						Elements lemma_content_p = par.select("div.lemma_content");
						if (line_lemma_descs_p.size() == lemma_content_p.size()) {
							for (int i=0; i<line_lemma_descs_p.size(); i++) {
								Lemma le = new Lemma();
								Element lemma_d = line_lemma_descs_p.get(i);
								Element lemma_content_d = lemma_content_p.get(i);
								Elements tag_lemmas = lemma_d.select("span.tag_lemma");
								for (Element tag_lemma : tag_lemmas) {
									Elements dictLink = tag_lemma.select("a.dictLink");
									String href = "";
									for (Element dl: dictLink) {
										if (href.equals("")) href = dictLink.attr("href");
									}
									DictEntry de = new DictEntry();
									de.dictLinkText = dictLink.text();
									de.dictLinkLink = href;
									Elements tag_wordtype = tag_lemma.select("span.tag_wordtype");
									de.tagWordType = tag_wordtype.text();
									Elements tag_type = tag_lemma.select("span.tag_type");
									de.tagType = tag_type.text();
									le.dictEntries.add(de);
								}
								Elements translation_lines = lemma_content_d.select("div.translation_lines");
								for (Element translation_line : translation_lines) {
									TranslLine translLine = new TranslLine();
									Elements translation = translation_line.select("div.translation");
									Elements translation_descs_h3 = translation.select("h3.translation_desc");
									Elements translation_descs_div = translation.select("div.translation_desc");
									String sTransText = translation_descs_h3.text();
									if (sTransText.equals("")) sTransText = translation_descs_div.text();
									String sTransLink = "";
									String sTransType = "";
									for (Element translation_desc : translation_descs_h3) {
										Elements dictLink = translation_desc.select("a.dictLink");
										for (Element dl: dictLink) {
											if (sTransLink.equals("")) sTransLink = dictLink.attr("href");
										}
										String sT = dictLink.text();
										if (!sT.equals("")) sTransText = sT;
										Elements tagType = translation_desc.select("span.tag_type");
										sT = tagType.text();
										if (!sT.equals("")) sTransType = sT;
									}
									for (Element translation_desc : translation_descs_div) {
										Elements dictLink = translation_desc.select("a.dictLink");
										for (Element dl: dictLink) {
											if (sTransLink.equals("")) sTransLink = dictLink.attr("href");
										}
										String sT = dictLink.text();
										if (!sT.equals("")) sTransText = sT;
										Elements tagType = translation_desc.select("span.tag_type");
										sT = tagType.text();
										if (!sT.equals("")) sTransType = sT;
									}
									translLine.transText = sTransText;
									translLine.transType = sTransType;
									translLine.transLink = sTransLink;
									Elements example_lines = translation.select("div.example.line");
									for (Element example_line : example_lines) {
										translLine.exampleLines.add(new ExampleLine(example_line.text()));
									}
									le.translLines.add(translLine);
								}
								dsl.lemmas.add(le);
							}
						}
					}

					Elements result_table = doc.select("table.result_table");
					for (Element result_table1 : result_table) {
						Elements td_sentences_left = result_table1.select("td.sentence.left");
						Elements td_sentences_right = result_table1.select("td.sentence.right2");
						for (int i=0; i< td_sentences_left.size(); i++) {
							Element td_sentence_left = td_sentences_left.get(i);
							Element td_sentence_right = null;
							if (td_sentences_right.size()>i) td_sentence_right = td_sentences_right.get(i);
							dsl.elementsL.add(td_sentence_left.text());
							if (td_sentence_right != null)
								dsl.elementsR.add(td_sentence_right.text());
						}
					}
					final String sTitle = cr.getString(R.string.not_found);
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null) {
									if (dsl.getCount() == 0) {
										cr.showDicToast(s, sTitle, DicToastView.IS_LINGUEE,
												urlBuilder.build().url().toString(), fullScreen);
									} else {
										Dictionaries.saveToDicSearchHistory(cr, s, dsl.getFirstTranslation(), curDict, dsl);
										cr.showDicToastExt(s, sTitle, DicToastView.IS_LINGUEE,
												urlBuilder.build().url().toString(), curDict, dsl, fullScreen);
									}
								} else {
									dcb.done(dsl.getFirstTranslation(), Dictionaries.dslStructToString(dsl));
									if (dcb.showDicToast()) {
										if (dsl.getCount() == 0) {
											cr.showDicToast(s, sTitle, DicToastView.IS_LINGUEE,
													urlBuilder.build().url().toString(), fullScreen);
										} else {
											cr.showDicToastExt(s, sTitle, DicToastView.IS_LINGUEE,
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
						cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.not_implemented), DicToastView.IS_LINGUEE, "", fullScreen);
					}, 100));
				}
			} catch (Exception e) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					log.e(cr.getString(R.string.error)+": "+
							e.getClass().getSimpleName()+" "+e.getMessage());
					cr.showDicToast(cr.getString(R.string.dict_err), e.getClass().getSimpleName()+" "+e.getMessage(),
							DicToastView.IS_LINGUEE, "", fullScreen);
				}, 100));
			}
		});
	};

}
