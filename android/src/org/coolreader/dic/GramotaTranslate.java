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

import java.net.URL;
import java.util.ArrayList;

import okhttp3.HttpUrl;

public class GramotaTranslate {

	public static final Logger log = L.create("cr3dict_gramota");

	public static HttpUrl.Builder getSearchUrl(String query) {
		String url = Dictionaries.GRAMOTA_RU_ONLINE;
		HttpUrl.Builder urlBuilder = HttpUrl.parse(url
		).newBuilder()
				.addQueryParameter("word", query);
		return urlBuilder;
	}

	public void gramotaTranslate(CoolReader cr, String s, boolean fullScreen,
								 String langf, String lang, Dictionaries.DictInfo curDict, View view,
								 Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium),
						DicToastView.IS_GRAMOTA, "", curDict, fullScreen);
				return;
			}
		}
		HttpUrl.Builder urlBuilder = getSearchUrl(s);
		BackgroundThread.instance().postBackground(() -> {
			try {
				if (llc == null) {
					URL url = new URL(urlBuilder.toString());
					Document doc = Jsoup.parse(url, 5000);

					DicStruct dsl = new DicStruct();
					dsl.srcText = s;

					Elements form = doc.select("form#checkWord");
					Element par = null;
					for (Element f: form) {
						if (par == null) par = f.parent();
					}
					if (par != null) {
						Lemma le = new Lemma();
						DictEntry de = new DictEntry();
						Elements words = par.select("input[name='word']");
						for (Element word: words)
							de.dictLinkText = word.attr("value");
						if (!StrUtils.isEmptyStr(de.dictLinkText)) {
							le.dictEntries.add(de);
							ArrayList<String> h2List = new ArrayList<>();
							Elements h2s = par.select("h2");
							for (Element h2 : h2s)
								h2List.add(h2.text());
							Elements divs = par.select("h2+div");
							ArrayList<String> h2DivList = new ArrayList<>();
							for (Element div : divs)
								h2DivList.add(div.text());
							if (h2List.size() == h2DivList.size()) {
								for (int i = 0; i < h2List.size(); i++) {
									TranslLine tl = new TranslLine();
									tl.transGroup = h2List.get(i);
									tl.transText = h2DivList.get(i);
									le.translLines.add(tl);
								}
								dsl.lemmas.add(le);
							}
						}
					}

					final String sTitle = cr.getString(R.string.not_found);
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null) {
									if (dsl.getCount() == 0) {
										cr.showDicToast(s, sTitle, DicToastView.IS_GRAMOTA,
												urlBuilder.build().url().toString(), curDict, fullScreen);
									} else {
										Dictionaries.saveToDicSearchHistory(cr, s, dsl.getFirstTranslation(), curDict, dsl);
										cr.showDicToastExt(s, sTitle, DicToastView.IS_GRAMOTA,
												urlBuilder.build().url().toString(), curDict, dsl, fullScreen);
									}
								} else {
									dcb.done(dsl.getFirstTranslation(), Dictionaries.dslStructToString(dsl));
									if (dcb.showDicToast()) {
										if (dsl.getCount() == 0) {
											cr.showDicToast(s, sTitle, DicToastView.IS_GRAMOTA,
													urlBuilder.build().url().toString(), curDict, fullScreen);
										} else {
											cr.showDicToastExt(s, sTitle, DicToastView.IS_GRAMOTA,
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
						cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.not_implemented), DicToastView.IS_GRAMOTA, "", curDict, fullScreen);
					}, 100));
				}
			} catch (Exception e) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					log.e(cr.getString(R.string.error)+": "+
							e.getClass().getSimpleName()+" "+e.getMessage());
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.error)+": "+
									e.getClass().getSimpleName()+" "+e.getMessage(),
							DicToastView.IS_GRAMOTA, "", curDict, fullScreen);
				}, 100));
			}
		});
	};
}
