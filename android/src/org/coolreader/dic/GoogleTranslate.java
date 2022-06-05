package org.coolreader.dic;

import android.view.View;
import android.widget.Toast;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.utils.StrUtils;
import org.json.JSONArray;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class GoogleTranslate {

	public static final Logger log = L.create("cr3dict_google");

	public String googleGetDefLangCode(String langCode) {
		String lc = StrUtils.getNonEmptyStr(langCode, true).toLowerCase();
		if (lc.equals("*")) return "auto";
		if (lc.equals("ceb")) return lc;
		if (lc.equals("zh_TW")) return lc;
		if (lc.equals("zh-TW")) return lc;
		if (lc.equals("haw")) return lc;
		if (lc.equals("hmn")) return lc;
		if (lc.length() > 2) return lc.substring(0,2);
		return lc;
	}

	public void googleTranslate(CoolReader cr, String s, boolean fullScreen, String langf, String lang,
								boolean extended,
								Dictionaries.DictInfo curDict, View view,
								Dictionaries.LangListCallback llc, CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium),
						DicToastView.IS_GOOGLE, "", fullScreen);
				return;
			}
			if ((StrUtils.isEmptyStr(lang)) || (StrUtils.isEmptyStr(langf))) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err),
								cr.getString(R.string.translate_lang_not_set) + ": ["
										+ langf + "] -> [" + lang + "]",
								DicToastView.IS_GOOGLE, "", fullScreen), 100));
				return;
			}
		}
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.GOOGLE_DIC_ONLINE).newBuilder()
				.addQueryParameter("client", "gtx") // -- (using "t" raises 403 Forbidden)
				.addQueryParameter("ie", "UTF-8") //input encoding
				.addQueryParameter("oe", "UTF-8") //output encoding
				.addQueryParameter("sl", langf) //source language (we need to specify "auto" to detect language)
				.addQueryParameter("tl", lang) //target language
				.addQueryParameter("hl", lang) //?
				.addQueryParameter("otf", "1") //?
				.addQueryParameter("ssel", "0") //?
				.addQueryParameter("tsel", "0") //?
				// what we want in result
				.addQueryParameter("dt", "t") //translation of source text
				.addQueryParameter("dt", "at") //alternate translations
				// Next options only give additional results when text is a single word
				.addQueryParameter("dt", "bd") //dictionary (articles, reverse translations, etc)
				.addQueryParameter("dt", "ex") //examples
				.addQueryParameter("dt", "ld") //?
				.addQueryParameter("dt", "md") //definitions of source text
				.addQueryParameter("dt", "qca") //?
				.addQueryParameter("dt", "rw") //"see also" list
				.addQueryParameter("dt", "rm") //transcription / transliteration of source and translated texts
				.addQueryParameter("dt", "ss") //synonyms of source text, if it's one word
				.addQueryParameter("q", s);
		String url = urlBuilder.build().toString();
		log.i("translate url: " + url);
		Request request = new Request.Builder()
				.url(url)
				.build();
		Call call = Dictionaries.client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					try {
						String sTrans = "";
						JSONArray jsa = new JSONArray(sBody);
						if (jsa.length()>0) {
							JSONArray jsa2 = jsa.getJSONArray(0);
							if (jsa2.length()>0) {
								JSONArray jsa3 = jsa2.getJSONArray(0);
								if (jsa3.length()>0) {
									sTrans = jsa3.getString(0);
								}
							}
						}
						String sTransAdd = "";
						if (extended) {
							try {
								if (jsa.length() > 1) {
									if (jsa.isNull(1)) {
										if (jsa.length() > 5) {
											JSONArray jsa22 = jsa.getJSONArray(5);
											if (jsa22.length() > 0) {
												JSONArray jsa3 = jsa22.getJSONArray(0);
												if (jsa3.length() > 2) {
													JSONArray jsa4 = jsa3.getJSONArray(2);
													for (int i = 0; i < jsa4.length(); i++) {
														JSONArray jsa5 = jsa4.getJSONArray(i);
														if (jsa5.length() > 0) {
															if (!jsa5.getString(0).equals(sTrans))
																sTransAdd = sTransAdd + ", " + jsa5.getString(0);
														}
													}
												}
											}
										}
									} else {
										JSONArray jsa22 = jsa.getJSONArray(1);
										if (jsa22.length() > 0) {
											JSONArray jsa3 = jsa22.getJSONArray(0);
											if (jsa3.length() > 1) {
												JSONArray jsa4 = jsa3.getJSONArray(1);
												for (int i = 0; i < jsa4.length(); i++) {
													if (!jsa4.getString(i).equals(sTrans))
														sTransAdd = sTransAdd + ", " + jsa4.getString(i);
												}
											}
										}
									}
								}
							} catch (Exception e) {
								boolean bShowToast = dcb == null;
								if (!bShowToast) bShowToast = dcb.showDicToast();
								if (dcb != null) dcb.fail(null, sBody);
								if (bShowToast)
									BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
										cr.showDicToast(cr.getString(R.string.dict_err), sBody, DicToastView.IS_GOOGLE,
												"", fullScreen)
									));
								log.e(e.getMessage());
								return;
								// do nothing
							}
						}
						if (sTransAdd.startsWith(", ")) sTransAdd = sTransAdd.substring(2);
						if (!StrUtils.isEmptyStr(sTrans)) {
							if (!StrUtils.isEmptyStr(sTransAdd)) sTrans = sTrans + " (" + sTransAdd + ")";
							if (dcb == null) {
								String finalSTrans = sTrans;
								BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
									cr.showDicToast(s, finalSTrans, Toast.LENGTH_LONG, view, DicToastView.IS_GOOGLE,
											"Google translate", fullScreen)
								));
								Dictionaries.saveToDicSearchHistory(cr, s, sTrans, curDict, "");
							} else {
								dcb.done(sTrans, "");
								if (dcb.showDicToast()) {
									String finalSTrans1 = sTrans;
									BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
										cr.showDicToast(s, finalSTrans1, Toast.LENGTH_LONG, view, DicToastView.IS_GOOGLE,
												"Google translate", fullScreen)
									));
								}
								if (dcb.saveToHist()) {
									Dictionaries.saveToDicSearchHistory(cr, s, sTrans, curDict, "");
								}
							}
						} else {
							boolean bShowToast = dcb == null;
							if (!bShowToast) bShowToast = dcb.showDicToast();
							if (dcb != null) dcb.fail(null, sBody);
							if (bShowToast)
								BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
										cr.showDicToast(cr.getString(R.string.dict_err), sBody, DicToastView.IS_GOOGLE,
												"", fullScreen)
								));
						}
					} catch (Exception e) {
						boolean bShowToast = dcb == null;
						if (!bShowToast) bShowToast = dcb.showDicToast();
						if (bShowToast) {
							BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
								cr.showDicToast(s, sBody, DicToastView.IS_GOOGLE,
										"Google translate", fullScreen)
							));
						} else dcb.fail(e, e.getMessage());
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				if (dcb == null)
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err), e.getMessage(), DicToastView.IS_GOOGLE,
								"", fullScreen)
					));
				else {
					dcb.fail(e, e.getMessage());
					if (dcb.showDicToast())
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
							cr.showDicToast(cr.getString(R.string.dict_err), e.getMessage(), DicToastView.IS_GOOGLE,
									"", fullScreen)
						));
				}
			}
		});
	};

}
