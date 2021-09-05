package org.coolreader.dic;

import android.view.View;
import android.widget.Toast;

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.StrUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LingvoTranslate {
	public static String sLingvoToken = "";
	public static int unauthCnt = 0;

	public String lingvoGetDefLangCode(String langCode) {
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("en")) return "en-us";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("az")) return "az-az";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("nl")) return "nl-nl";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("fr")) return "fr-fr";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("de")) return "de-de";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("it")) return "it-it";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("ms")) return "ms-my";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("pt")) return "pt-pt";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("es")) return "es-es";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("uz")) return "uz-uz";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("sv")) return "sv-se";
		return langCode;
	}

	public void lingvoTranslate(CoolReader cr, String s, String langf, String lang, boolean extended,
								 Dictionaries.DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		if (!FlavourConstants.PREMIUM_FEATURES) {
			cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium), DicToastView.IS_LINGVO, "");
			return;
		}
		if ((StrUtils.isEmptyStr(langf))||(StrUtils.isEmptyStr(lang))) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.translate_lang_not_set)+": ["
									+langf+"] -> ["+lang + "]",
							DicToastView.IS_LINGVO, "")
					, 100));
			return;
		}
		int ilangf = 0;
		int ilang = 0;
		try {
			ilangf = Integer.valueOf(langf);
		} catch (Exception e) {
			ilangf = 0;
		}
		try {
			ilang = Integer.valueOf(lang);
		} catch (Exception e) {
			ilang = 0;
		}
		if ((ilangf == 0) || (ilang == 0)) {
			for (String lc: Dictionaries.langCodes) {
				if (lc.split("~").length == 6) {
					String slang = lc.split("~")[2];
					if (StrUtils.isEmptyStr(slang)) slang = lc.split("~")[1];
					if (ilangf == 0) {
						try {
							if (langf.toUpperCase().equals(slang.toUpperCase()))
								ilangf = Integer.valueOf(lc.split("~")[3]);
						} catch (Exception e) {

						}
					}
					if (ilang == 0) {
						try {
							if (lang.toUpperCase().equals(slang.toUpperCase()))
								ilang = Integer.valueOf(lc.split("~")[3]);
						} catch (Exception e) {

						}
					}
				}
			}
			for (String lc: Dictionaries.langCodes) {
				if (lc.split("~").length == 6) {
					String slang = lc.split("~")[1];
					if (ilangf == 0) {
						try {
							if (langf.toUpperCase().equals(slang.toUpperCase()))
								ilangf = Integer.valueOf(lc.split("~")[3]);
						} catch (Exception e) {

						}
					}
					if (ilang == 0) {
						try {
							if (lang.toUpperCase().equals(slang.toUpperCase()))
								ilang = Integer.valueOf(lc.split("~")[3]);
						} catch (Exception e) {

						}
					}
				}
			}
		}
		final int ilangfF = ilangf;
		final int ilangF = ilang;
		if ((ilangf == 0) || (ilang == 0)) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.translate_lang_not_found)+": ["
									+langf+ " {" + ilangfF + "}" + "] -> ["+lang + " {" + ilangF + "}]",
							DicToastView.IS_LINGVO, ""), 100));
			return;
		}
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.LINGVO_DIC_ONLINE+"/v1/Minicard").newBuilder();
		urlBuilder.addQueryParameter("text", s);
		urlBuilder.addQueryParameter("srcLang", String.valueOf(ilangf));
		urlBuilder.addQueryParameter("dstLang", String.valueOf(ilang));
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.header("Authorization","Bearer "+sLingvoToken)
				.url(url)
				.build();
		Call call = Dictionaries.client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					try {
						JSONObject jso = new JSONObject(sBody);
						if (jso.has("Translation")) {
							JSONObject jsoT = jso.getJSONObject("Translation");
							if (jsoT.has("Translation")) {
								String sHeading = jsoT.getString("Heading");
								String sTrans = jsoT.getString("Translation");
								String sDic = "";
								if (jsoT.has("DictionaryName"))
									sDic = jsoT.getString("DictionaryName");
								if (!StrUtils.isEmptyStr(sTrans)) {
									if (!StrUtils.isEmptyStr(sHeading))
										sTrans = sHeading.replace(":", " ") + ": " + sTrans;
									if (!extended) {
										if (dcb == null) {
											cr.showDicToast(s, sTrans, Toast.LENGTH_LONG, view, DicToastView.IS_LINGVO, sDic);
											Dictionaries.saveToDicSearchHistory(cr, s, sTrans, curDict);
										} else {
											dcb.done(sTrans);
											if (dcb.showDicToast()) {
												cr.showDicToast(s, sTrans, Toast.LENGTH_LONG, view, DicToastView.IS_LINGVO, sDic);
											}
											if (dcb.saveToHist()) {
												Dictionaries.saveToDicSearchHistory(cr, s, sTrans, curDict);
											}
										}
									}
									else lingvoExtended(cr, s, ilangfF, ilangF, sTrans, false, sDic, curDict, view, dcb);
								}
							} else {
								if (dcb != null) dcb.fail(null, sBody);
							}
						}
					} catch (Exception e) {
						boolean bShowToast = dcb == null;
						if (!bShowToast) bShowToast = dcb.showDicToast();
						if (bShowToast) {
							cr.showDicToast(s, sBody, DicToastView.IS_LINGVO, "");
							if ((sBody.contains("for direction")) && (sBody.contains("not found")))
								BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
									if (cr.getReaderView().mBookInfo != null) {
										FileInfo fi = cr.getReaderView().mBookInfo.getFileInfo();
										FileInfo dfi = fi.parent;
										if (dfi == null) {
											dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
										}
										if (dfi != null) {
											cr.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL, null, dfi, fi, langf, lang, s, null,
													TranslationDirectionDialog.FOR_LINGVO, null);
										}
									}
									;
								}, 1000));
						} else dcb.fail(e, e.getMessage());
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				sLingvoToken = "";
				if (unauthCnt == 0) {
					unauthCnt++;
					lingvoAuthThenTranslate(cr, s, langf, lang, extended, curDict, view, dcb);
				} else {
					if (dcb == null)
						cr.showDicToast(cr.getString(R.string.dict_err), e.getMessage(), DicToastView.IS_LINGVO, "");
					else {
						dcb.fail(e, e.getMessage());
						if (dcb.showDicToast())
							cr.showDicToast(cr.getString(R.string.dict_err), e.getMessage(), DicToastView.IS_LINGVO, "");
					}
					unauthCnt = 0;
				}
			}
		});
	}

	private void lingvoExtended(CoolReader cr, String s, int ilangf, int ilang,
								String sTrans, boolean isYnd, String sDic, Dictionaries.DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.LINGVO_DIC_ONLINE+"/v1/Translation").newBuilder();
		urlBuilder.addQueryParameter("text", s);
		urlBuilder.addQueryParameter("srcLang", String.valueOf(ilangf));
		urlBuilder.addQueryParameter("dstLang", String.valueOf(ilang));
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.header("Authorization","Bearer "+sLingvoToken)
				.url(url)
				.build();
		Call call = Dictionaries.client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					try {
						String sAdd = "";
						JSONArray jsa = new JSONArray(sBody);
						JSONObject jso = null;
						if (jsa.length()>0)
							jso = jsa.getJSONObject(0);
						if (jso.has("Body")) {
							JSONArray jsa2 = jso.getJSONArray("Body");
							if (jsa2.length()>0) {
								jso = jsa2.getJSONObject(0);
								JSONArray jsa3 = jso.getJSONArray("Markup");
								for (int i=0; i<jsa3.length(); i++) {
									jso = jsa3.getJSONObject(i);
									if ((jso.has("Node"))&&(jso.has("Text"))) {
										if (StrUtils.getNonEmptyStr(jso.getString("Node"), true).equals("Transcription")) {
											sAdd = " ["+StrUtils.getNonEmptyStr(jso.getString("Text"), true)+"]";
											if (dcb == null) {
												cr.showDicToast(s, sTrans + sAdd, Toast.LENGTH_LONG, view, DicToastView.IS_LINGVO, sDic);
												Dictionaries.saveToDicSearchHistory(cr, s, sTrans + sAdd, curDict);
											} else {
												dcb.done(sTrans + sAdd);
												if (dcb.showDicToast()) {
													cr.showDicToast(s, sTrans + sAdd, Toast.LENGTH_LONG, view, DicToastView.IS_LINGVO, sDic);
												}
												if (dcb.saveToHist()) {
													Dictionaries.saveToDicSearchHistory(cr, s, sTrans + sAdd, curDict);
												}
											}
											return;
										}
									}
								}
							}
						}
						if (dcb == null) {
							cr.showDicToast(s, sTrans + sAdd, DicToastView.IS_LINGVO, sDic);
							Dictionaries.saveToDicSearchHistory(cr, s, sTrans + sAdd, curDict);
						} else  {
							dcb.done(sTrans + sAdd);
							if (dcb.showDicToast()) {
								cr.showDicToast(s, sTrans + sAdd, DicToastView.IS_LINGVO, sDic);
							}
							if (dcb.saveToHist()) {
								Dictionaries.saveToDicSearchHistory(cr, s, sTrans + sAdd, curDict);
							}
						}
					} catch (Exception e) {
						if (dcb == null) {
							cr.showDicToast(s, sTrans, DicToastView.IS_LINGVO, sDic);
							Dictionaries.saveToDicSearchHistory(cr, s, sTrans, curDict);
						} else {
							dcb.done(sTrans);
							if (dcb.showDicToast()) {
								cr.showDicToast(s, sTrans, DicToastView.IS_LINGVO, sDic);
							}
							if (dcb.saveToHist()) {
								Dictionaries.saveToDicSearchHistory(cr, s, sTrans, curDict);
							}
						}
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				if (dcb == null)
					cr.showDicToast(s, sTrans, DicToastView.IS_LINGVO, sDic);
				else {
					dcb.done(sTrans);
					if (dcb.showDicToast())
						cr.showDicToast(s, sTrans, DicToastView.IS_LINGVO, sDic);
				}
			}
		});
	}

	public void lingvoAuthThenTranslate(CoolReader cr, String s, String langf,
						String lang, boolean extended, Dictionaries.DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.LINGVO_DIC_ONLINE+"/v1.1/authenticate").newBuilder();
		String url = urlBuilder.build().toString();
		final CoolReader crf2 = cr;
		crf2.readLingvoCloudSettings();
		String token = BuildConfig.LINGVO;
		if (!StrUtils.isEmptyStr(crf2.lingvoCloudSettings.lingvoToken)) token = crf2.lingvoCloudSettings.lingvoToken;
		RequestBody body = RequestBody.create(
				MediaType.parse("text/plain"), "");
		Request request = new Request.Builder()
				.header("Authorization","Basic " + token)
				.post(body)
				.url(url)
				.build();
		Call call = Dictionaries.client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				if (StrUtils.getNonEmptyStr(response.message(), true).equals("Unauthorized")) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									crf2.showDicToast(cr.getString(R.string.dict_err),
											crf2.getString(R.string.lingvo_unauth), DicToastView.IS_LINGVO, "");
								else {
									if (dcb.showDicToast()) crf2.showDicToast(cr.getString(R.string.dict_err),
											crf2.getString(R.string.lingvo_unauth), DicToastView.IS_LINGVO, "");
									dcb.fail(null, crf2.getString(R.string.lingvo_unauth));
								}
							}, 100));
					return;
				}
				if (response.code() != 200) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									crf2.showDicToast(cr.getString(R.string.dict_err),
									crf2.getString(R.string.http_error) + " " + response.code(),
											DicToastView.IS_LINGVO, "");
								else {
									if (dcb.showDicToast())
										crf2.showDicToast(cr.getString(R.string.dict_err),
											crf2.getString(R.string.http_error) + " " + response.code(), DicToastView.IS_LINGVO, "");
									dcb.fail(null, crf2.getString(R.string.http_error) + " " + response.code());
								}
							}, 100));
					return;
				}
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					sLingvoToken = sBody;
					lingvoTranslate(cr, s, lingvoGetDefLangCode(langf), lingvoGetDefLangCode(lang), extended, curDict, view, dcb);
				}, 100));
			}
			public void onFailure(Call call, IOException e) {
				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> {
							if (dcb == null)
								crf2.showDicToast(cr.getString(R.string.dict_err),
									e.getMessage(), DicToastView.IS_LINGVO, "");
							else {
								if (dcb.showDicToast())
									crf2.showDicToast(cr.getString(R.string.dict_err),
										e.getMessage(), DicToastView.IS_LINGVO, "");
								dcb.fail(e, e.getMessage());
							}
						}, 100));
			}
		});
	};

}
