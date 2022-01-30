package org.coolreader.dic.wiki;

import android.view.View;
import android.widget.Toast;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.ProgressDialog;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;
import org.coolreader.dic.DicToastView;
import org.coolreader.dic.Dictionaries;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class WikiSearch {

	public HttpUrl.Builder wikiUrlBuilder(CoolReader cr, String s, String link, int curAction, int listSkipCount, int prevAction) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(link).newBuilder();
		if (curAction == WIKI_FIND_TITLE) {
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "extracts");
			urlBuilder.addQueryParameter("titles", s);
			urlBuilder.addQueryParameter("exintro", "1");
			urlBuilder.addQueryParameter("explaintext", "1");
		}
		if (curAction == WIKI_FIND_TITLE_FULL) {
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "extracts");
			urlBuilder.addQueryParameter("titles", s);
			urlBuilder.addQueryParameter("explaintext", "1");
		}
		if (curAction == WIKI_FIND_LIST) {
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("list", "search");
			urlBuilder.addQueryParameter("srsearch", s);
			urlBuilder.addQueryParameter("srwhat", "text");
			if (listSkipCount > 0) {
				urlBuilder.addQueryParameter("sroffset", "" + listSkipCount);
			}
		}
		if (curAction == WIKI_SHOW_PAGE_ID) {
			String ss = s;
			if (ss.contains("~")) ss=s.split("~")[0];
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "extracts");
			urlBuilder.addQueryParameter("pageids", ss);
			urlBuilder.addQueryParameter("explaintext", "1");
			urlBuilder.addQueryParameter("exintro", "1");
		}
		if (curAction == WIKI_SHOW_PAGE_FULL_ID) {
			String ss = s;
			if (ss.contains("~")) ss=s.split("~")[0];
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "extracts");
			urlBuilder.addQueryParameter("pageids", ss);
			urlBuilder.addQueryParameter("explaintext", "1");
		}
		if ((curAction == WIKI_FIND_PIC_INFO)
				&& ((prevAction == WIKI_FIND_TITLE)||(prevAction == WIKI_FIND_TITLE_FULL))) {
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "pageimages");
			urlBuilder.addQueryParameter("titles", s);
			int i = 200;
			if (cr.getReaderView() != null) {
				if (cr.getReaderView().getSurface() != null)
					i = Math.min(cr.getReaderView().getSurface().getWidth(), cr.getReaderView().getSurface().getHeight()) / 3;
			} else
				i = Math.min(cr.mHomeFrame.getWidth(), cr.mHomeFrame.getHeight()) / 3;
			urlBuilder.addQueryParameter("pithumbsize", String.valueOf(i));
		}
		if ((curAction == WIKI_FIND_PIC_INFO)
				&& ((prevAction == WIKI_SHOW_PAGE_ID)||(prevAction == WIKI_SHOW_PAGE_FULL_ID))) {
			String ss = s;
			if (ss.contains("~")) ss=s.split("~")[0];
			urlBuilder.addQueryParameter("action", "query");
			urlBuilder.addQueryParameter("format", "xml");
			urlBuilder.addQueryParameter("prop", "pageimages");
			urlBuilder.addQueryParameter("pageids", ss);
			int i = 200;
			if (cr.getReaderView() != null) {
				if (cr.getReaderView().getSurface() != null)
					i = Math.min(cr.getReaderView().getSurface().getWidth(), cr.getReaderView().getSurface().getHeight()) / 5 * 2;
			} else
				i = Math.min(cr.mHomeFrame.getWidth(), cr.mHomeFrame.getHeight()) / 5 * 2;
			urlBuilder.addQueryParameter("pithumbsize", String.valueOf(i));
		}
		return urlBuilder;
	}

	public static int WIKI_FIND_TITLE = 1;
	public static int WIKI_FIND_LIST = 2;
	public static int WIKI_SHOW_PAGE_ID = 3;
	public static int WIKI_SHOW_PAGE_FULL_ID = 4;
	public static int WIKI_FIND_TITLE_FULL = 5;
	public static int WIKI_FIND_PIC_INFO = 6;

	private String wikiTitleText = "";
	private String wikiLink = "";

	public void wikiTranslate(CoolReader cr, Dictionaries.DictInfo curDict, View view, String s, String link, String link2,
							  int curAction, boolean useFirstLink, CoolReader.DictionaryCallback dcb) {
		wikiTranslate(cr, curDict, view, s, link, link2, curAction, 0, useFirstLink, 0, "", dcb);
	}

	public void wikiTranslate(CoolReader cr, Dictionaries.DictInfo curDict, View view, String s, String link, String link2,
							  int curAction, boolean useFirstLink, int prevAction, String articleText, CoolReader.DictionaryCallback dcb) {
		wikiTranslate(cr, curDict, view, s, link, link2, curAction, 0, useFirstLink, prevAction, articleText, dcb);
	}

	public void wikiTranslate(CoolReader cr, Dictionaries.DictInfo curDict, View view, String s, String link, String link2,
							  int curAction, int listSkipCount, boolean useFirstLink,
							  int prevAction, String articleText, CoolReader.DictionaryCallback dcb) {
		if (StrUtils.isEmptyStr(link)) return;
		boolean saveHist = false;
		if (cr.getReaderView() != null)
			saveHist = cr.getReaderView().getSettings().getBool(Settings.PROP_CLOUD_WIKI_SAVE_HISTORY,false);
		final String sLinkF = link;
		final String sLinkF2 = link2;
		String sLink = link + "/w/api.php";
		String sLink2 = link2 + "/w/api.php";
		HttpUrl.Builder urlBuilder = wikiUrlBuilder(cr, s, useFirstLink ? sLink : sLink2, curAction,
				listSkipCount, prevAction);
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.url(url)
				.build();
		Call call = Dictionaries.client.newCall(request);
		final CoolReader crf2 = cr;
		final Dictionaries.DictInfo curDictF2 = curDict;
		if ((curAction == WIKI_FIND_TITLE_FULL) || (curAction == WIKI_SHOW_PAGE_FULL_ID)) {
			Dictionaries.progressDlg = ProgressDialog.show(cr,
					cr.getString(R.string.network_op),
					cr.getString(R.string.network_op),
					true, false, null);
		}
		boolean finalSaveHist = saveHist;
		boolean finalSaveHist1 = saveHist;
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				Document docJsoup = Jsoup.parse(sBody, sLinkF);
				BackgroundThread.instance().postGUI(() -> {
					if (Dictionaries.progressDlg != null)
						if (Dictionaries.progressDlg.isShowing()) Dictionaries.progressDlg.dismiss();
				});
				wikiTitleText = "";
				if ((curAction == WIKI_FIND_TITLE)
						|| (curAction == WIKI_SHOW_PAGE_ID)
						|| (curAction == WIKI_SHOW_PAGE_FULL_ID)
						|| (curAction == WIKI_FIND_TITLE_FULL)
				) {
					Elements results = docJsoup.select("api > query > pages > page > extract");
					if (results.size() > 0) wikiTitleText = results.text(); else
						wikiTitleText = Utils.cleanupHtmlTags(sBody);
				}
				if (StrUtils.isEmptyStr(wikiTitleText))
					wikiTitleText = crf2.getString(R.string.not_found);
				final String sTranslF = wikiTitleText;
				// if found article
				if ((!StrUtils.isEmptyStr(wikiTitleText)) &&
						(curAction == WIKI_FIND_TITLE) || (curAction == WIKI_FIND_TITLE_FULL)
				) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						wikiLink = ((curAction == WIKI_FIND_TITLE) || (curAction == WIKI_FIND_TITLE_FULL)) ? sLinkF : sLinkF2;
//						crf2.showDicToastWiki(s, sTranslF, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
//								curDict, link, link2, curAction, useFirstLink);
						if (finalSaveHist) Dictionaries.saveToDicSearchHistory(cr, s, sTranslF, curDict);
						wikiTranslate(cr, curDict, view, s, link, link2,
								WIKI_FIND_PIC_INFO, useFirstLink, curAction, sTranslF, dcb);
					}, 100));
					return;
				}
				// not found in first link - try in 2nd
				if ((StrUtils.isEmptyStr(wikiTitleText)) && ((curAction == WIKI_FIND_TITLE) || (curAction == WIKI_FIND_TITLE_FULL)) &&
						(!StrUtils.isEmptyStr(sLink2)) && (useFirstLink)) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						wikiLink = link2;
						wikiTranslate(cr, curDict, view, s, link, link2,
								(curAction == WIKI_FIND_TITLE) ? WIKI_FIND_TITLE : WIKI_FIND_TITLE_FULL, false, dcb);
					}, 100));
					return;
				}
				// not found - try to show list
				if ((StrUtils.isEmptyStr(wikiTitleText)) &&
						((curAction == WIKI_FIND_TITLE) || (curAction == WIKI_FIND_TITLE_FULL))
				) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
							wikiTranslate(cr, curDict, view, s, link, link2, WIKI_FIND_LIST, true, dcb), 100));
					return;
				}
				if (curAction == WIKI_FIND_LIST) {
					Elements results = docJsoup.select("api > query > search > p");
					ArrayList<WikiArticle> arrWA = new ArrayList<>();
					for (Element el: results) {
						WikiArticle wa = new WikiArticle(el.attr("title"), Long.valueOf(el.attr("pageid")), el.attr("snippet"));
						arrWA.add(wa);
					}
					if (arrWA.size() > 0) {
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
								crf2.showWikiListToast(s, sTranslF, view, DicToastView.IS_WIKI, wikiLink,
										arrWA, curDict, link, link2, curAction, listSkipCount, useFirstLink), 100));
						return;
					} else {
						if ((curAction == WIKI_FIND_LIST) && (useFirstLink)) {
							BackgroundThread.instance().postBackground(
									() -> BackgroundThread.instance().postGUI(() ->
											wikiTranslate(cr, curDict, view, s, link, link2, WIKI_FIND_LIST, false, dcb), 100)
							);
							return;
						} else
							BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(
									() -> crf2.showToast(cr.getString(R.string.not_found)), 100)
							);
					}
				}
				if ((!StrUtils.isEmptyStr(wikiTitleText)) &&
						(
								(curAction == WIKI_SHOW_PAGE_ID) ||
										(curAction == WIKI_SHOW_PAGE_FULL_ID)
						)
				) {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						wikiLink = useFirstLink ? sLinkF : sLinkF2;
						//crf2.showDicToastWiki(s, sTranslF, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
						//		curDict, link, link2, curAction, useFirstLink);
						if (s.contains("~")) {
							if (finalSaveHist1) Dictionaries.saveToDicSearchHistory(cr, s.split("~")[1], sTranslF, curDict);
						}
						wikiTranslate(cr, curDict, view, s, link, link2,
								WIKI_FIND_PIC_INFO, useFirstLink, curAction, sTranslF, dcb);
					}, 100));
					return;
				}
				if ((curAction == WIKI_FIND_PIC_INFO)
				) {
					Elements results = docJsoup.select("api > query > pages > page > thumbnail");
					if (results.size() > 0) {
						Element el = results.get(0);
						String addr = el.attr("source");
						// showing with pic
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
							if (dcb == null)
								crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
										curDict, link, link2, prevAction, useFirstLink, addr);
							else {
								dcb.done(articleText);
								if (dcb.showDicToast()) {
									crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
											curDict, link, link2, prevAction, useFirstLink, addr);
								}
							}
						}, 100));
					} else {
						// showing with no pic
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
							if (dcb == null)
								crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
										curDict, link, link2, prevAction, useFirstLink, "");
							else {
								dcb.done(articleText);
								if (dcb.showDicToast()) {
									crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
											curDict, link, link2, prevAction, useFirstLink, "");
								}
							}
						}, 100));
					};
				}
			}

			public void onFailure(Call call, IOException e) {
				// showing with no pic
				BackgroundThread.instance().postGUI(() -> {
					if (Dictionaries.progressDlg != null)
						if (Dictionaries.progressDlg.isShowing()) Dictionaries.progressDlg.dismiss();
				});
				if (curAction == WIKI_FIND_PIC_INFO)
					// showing with no pic
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
						if (dcb == null)
							crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
									curDict, link, link2, prevAction, useFirstLink, "");
						else {
							dcb.done(articleText);
							if (dcb.showDicToast())
								crf2.showDicToastWiki(s, articleText, Toast.LENGTH_LONG, view, DicToastView.IS_WIKI, wikiLink,
										curDict, link, link2, prevAction, useFirstLink, "");
						}
					}, 100));
				else
					// nothing to show - article did not load
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
							crf2.showToast(e.getMessage()), 100)
					);
			}
		});
	}

}
