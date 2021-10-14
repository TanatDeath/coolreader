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
import org.coolreader.dic.struct.ExampleLine;
import org.coolreader.dic.struct.Lemma;
import org.coolreader.dic.struct.TranslLine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.HttpUrl;

public class UrbanTranslate {

	//<div class="def-panel " //doc.select("div.def-panel ")
		//div class="contributor"
		//<div class="meaning">
		//<div class="def-header"> a class="word"
		//<div class="example">

	public static final Logger log = L.create("cr3dict_urban");

	public void urbanTranslate(CoolReader cr, String s, Dictionaries.DictInfo curDict, View view,
								Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium), DicToastView.IS_URBAN, "");
				return;
			}
		}
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.URBAN_ONLINE)
				.newBuilder().addQueryParameter("term", s);
		BackgroundThread.instance().postBackground(() -> {
			try {
				if (llc == null) {
					String sTitle = s;
					DicStruct dsl = new DicStruct();
					Lemma lemma = null;
					Document docJsoup = Jsoup.parse(urlBuilder.build().url(), 180000); // three minutes
					Elements resDivs = docJsoup.select("div.def-panel");
					for (Element resDiv: resDivs) {
						String sWord = "";
						Elements wordsD = resDiv.select("div.def-header");
						for (Element wordD: wordsD) {
							Elements words = resDiv.select("a.word");
							for (Element word: words) {
								if (StrUtils.isEmptyStr(sWord)) sWord = word.text();
									else sWord += ", " + word.text();
							}
						}
						String sMeaning = "";
						Elements meanings = resDiv.select("div.meaning");
						for (Element meaning: meanings) {
							if (StrUtils.isEmptyStr(sMeaning)) sMeaning = meaning.text();
							else sMeaning += ", " + meaning.text();
						}
						sTitle = sTitle + sMeaning;
						String sContributor = "";
						Elements contributors = resDiv.select("div.contributor");
						for (Element contributor: contributors) {
							if (StrUtils.isEmptyStr(sContributor)) sContributor = contributor.text();
							else sContributor += ", " + contributor.text();
						}
						String sExample = "";
						Elements examples = resDiv.select("div.example");
						TranslLine tl = new TranslLine();
						for (Element example: examples) {
							if (StrUtils.isEmptyStr(sExample)) sExample = examples.text();
							else sExample += ", " + examples.text();
							ExampleLine el = new ExampleLine(StrUtils.getNonEmptyStr(examples.text(), true));
							tl.exampleLines.add(el);
						}

						lemma = new Lemma();
						DictEntry de = new DictEntry();
						de.dictLinkText = StrUtils.getNonEmptyStr(sWord, true);
						de.tagType = StrUtils.getNonEmptyStr(sContributor, true);
						lemma.dictEntry.add(de);
						tl.transText = StrUtils.getNonEmptyStr(sMeaning, true);
						lemma.translLine.add(tl);
						dsl.lemmas.add(lemma);
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
									cr.showDicToastExt(s, s, DicToastView.IS_URBAN, urlBuilder.build().url().toString(), curDict, dsl);
								} else
									cr.showDicToast(s, cr.getString(R.string.not_found), DicToastView.IS_URBAN, urlBuilder.build().url().toString());
							} else {
								dcb.done(s);
								if (dcb.showDicToast()) {
									cr.showDicToast(s, finalSTitle, DicToastView.IS_URBAN, urlBuilder.build().url().toString());
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
								DicToastView.IS_URBAN, "");
					}, 100));
				}
			} catch (Exception e) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					log.e(cr.getString(R.string.error)+": "+
							e.getClass().getSimpleName()+" "+e.getMessage());
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.error)+": "+
									e.getClass().getSimpleName()+" "+e.getMessage(),
							DicToastView.IS_URBAN, "");
				}, 100));
			}
		});
	};

}
