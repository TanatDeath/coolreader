package org.coolreader.dic;

import android.view.View;
import android.widget.Toast;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.utils.StrUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class DeeplTranslate {

	public static int unauthCntDeepl = 0;
	public static boolean deeplAuthenticated = false;

	public void deeplAuthThenTranslate(CoolReader cr, String s, boolean fullScreen, String langf, String lang, Dictionaries.DictInfo curDict,
										View view, CoolReader.DictionaryCallback dcb) {
		deeplAuthenticated = false;
		cr.readDeeplCloudSettings();
		HttpUrl.Builder urlBuilder;
		if (StrUtils.getNonEmptyStr(cr.deeplCloudSettings.deeplToken,false).endsWith(":fx"))
			urlBuilder = HttpUrl.parse(Dictionaries.DEEPL_DIC_ONLINE_FREE+"/usage").newBuilder().addQueryParameter("auth_key", cr.deeplCloudSettings.deeplToken);
		else
			urlBuilder = HttpUrl.parse(Dictionaries.DEEPL_DIC_ONLINE+"/usage").newBuilder().addQueryParameter("auth_key", cr.deeplCloudSettings.deeplToken);
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.url(url)
				.build();
		Call call = Dictionaries.client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				if ((response.code() == 403) || (response.code() == 456)) {
					String sErr = "";
					if (response.code() == 403) sErr = cr.getString(R.string.deepl_unauth);
					if (response.code() == 456) sErr = cr.getString(R.string.deepl_quoata);
					String finalSErr = sErr;
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									cr.showDicToast(cr.getString(R.string.dict_err), finalSErr, DicToastView.IS_DEEPL,
											"", fullScreen);
								else {
									if (dcb.showDicToast())
										cr.showDicToast(cr.getString(R.string.dict_err), finalSErr, DicToastView.IS_DEEPL,
												"", fullScreen);
									dcb.fail(null, finalSErr);
								}
							}, 100));
					return;
				}
				if (response.code() != 200) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									cr.showDicToast(cr.getString(R.string.dict_err),
											cr.getString(R.string.http_error) + " " + response.code(), DicToastView.IS_DEEPL,
											"", fullScreen);
								else {
									if (dcb.showDicToast())
										cr.showDicToast(cr.getString(R.string.dict_err),
												cr.getString(R.string.http_error) + " " + response.code(), DicToastView.IS_DEEPL,
												"", fullScreen);
									dcb.fail(null, cr.getString(R.string.http_error) + " " + response.code());
								}
							}, 100));
					return;
				}
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					deeplAuthenticated = true;
					deeplTranslate(cr, s, fullScreen,
							deeplGetDefLangCode(langf, true), deeplGetDefLangCode(lang, true),
							curDict, view, dcb);
				}, 100));
			}
			public void onFailure(Call call, IOException e) {
				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() -> {
							if (dcb == null)
								cr.showDicToast(cr.getString(R.string.dict_err),
										e.getMessage(), DicToastView.IS_DEEPL, "", fullScreen);
							else {
								if (dcb.showDicToast())
									cr.showDicToast(cr.getString(R.string.dict_err),
											e.getMessage(), DicToastView.IS_DEEPL, "", fullScreen);
								dcb.fail(e, e.getMessage());
							}
						}, 100));
			}
		});
	};

	public String deeplGetDefLangCode(String langCode, boolean isSrc) {
		if (isSrc) {
			if (StrUtils.getNonEmptyStr(langCode,true).toLowerCase().startsWith("en-")) return "en";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("nl-")) return "nl";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("fr-")) return "fr";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("de-")) return "de";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("it-")) return "it";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("es-")) return "es";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("pt-")) return "pt";
		} else {
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().equalsIgnoreCase("en")) return "en-us";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("nl-")) return "nl";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("fr-")) return "fr";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("de-")) return "de";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("it-")) return "it";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().equalsIgnoreCase("pt")) return "pt-pt";
			if (StrUtils.getNonEmptyStr(langCode, true).toLowerCase().startsWith("es-")) return "es";
		}
		return StrUtils.getNonEmptyStr(langCode, true).toUpperCase();
	}

	public void deeplTranslate(CoolReader cr, String s, boolean fullScreen, String langf, String lang,
								Dictionaries.DictInfo curDict, View view, CoolReader.DictionaryCallback dcb) {
		if (!FlavourConstants.PREMIUM_FEATURES) {
			cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium),
					DicToastView.IS_DEEPL, "", fullScreen);
			return;
		}
		if ((StrUtils.isEmptyStr(langf))||(StrUtils.isEmptyStr(lang))) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.translate_lang_not_set)+": ["
									+langf+"] -> ["+lang + "]",
							DicToastView.IS_DEEPL, "", fullScreen)
					, 100));
			return;
		}
		if (
				(!(
						lang.equalsIgnoreCase("DE")||
								lang.equalsIgnoreCase("EN-GB")||
								lang.equalsIgnoreCase("EN-US")||
								lang.equalsIgnoreCase("EN")||
								lang.equalsIgnoreCase("FR")||
								lang.equalsIgnoreCase("IT")||
								lang.equalsIgnoreCase("JA")||
								lang.equalsIgnoreCase("ES")||
								lang.equalsIgnoreCase("NL")||
								lang.equalsIgnoreCase("PL")||
								lang.equalsIgnoreCase("PT-PT")||
								lang.equalsIgnoreCase("PT-BR")||
								lang.equalsIgnoreCase("PT")||
								lang.equalsIgnoreCase("RU")||
								lang.equalsIgnoreCase("ZH")
				))
		) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showDicToast(cr.getString(R.string.dict_err),
							cr.getString(R.string.translate_lang_not_found)+": ["
									+langf + "] -> ["+lang + "]",
							DicToastView.IS_DEEPL, "", fullScreen), 100));
			return;
		}
		HttpUrl.Builder urlBuilder;
		if (StrUtils.getNonEmptyStr(cr.deeplCloudSettings.deeplToken,false).endsWith(":fx"))
			urlBuilder = HttpUrl.parse(Dictionaries.DEEPL_DIC_ONLINE_FREE+"/translate").newBuilder();
		else
			urlBuilder = HttpUrl.parse(Dictionaries.DEEPL_DIC_ONLINE+"/translate").newBuilder();
		urlBuilder.addQueryParameter("auth_key", cr.deeplCloudSettings.deeplToken);
		urlBuilder.addQueryParameter("text", s);
		urlBuilder.addQueryParameter("target_lang", lang);
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.url(url)
				.build();
		Call call = Dictionaries.client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				if ((response.code() == 403) || (response.code() == 456)) {
					String sErr = "";
					if (response.code() == 403) sErr = cr.getString(R.string.deepl_unauth);
					if (response.code() == 456) sErr = cr.getString(R.string.deepl_quoata);
					String finalSErr = sErr;
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									cr.showDicToast(cr.getString(R.string.dict_err),
											finalSErr,
											DicToastView.IS_DEEPL, "", fullScreen);
								else {
									if (dcb.showDicToast()) cr.showDicToast(cr.getString(R.string.dict_err),
											finalSErr,
											DicToastView.IS_DEEPL, "", fullScreen);
									dcb.fail(null, finalSErr);
								}
							}, 100));
					deeplAuthenticated = false;
					return;
				}
				if (response.code() != 200) {
					BackgroundThread.instance().postBackground(() ->
							BackgroundThread.instance().postGUI(() -> {
								if (dcb == null)
									cr.showDicToast(cr.getString(R.string.dict_err),
										cr.getString(R.string.http_error) + " " + response.code(),
										DicToastView.IS_DEEPL, "", fullScreen);
								else {
									if (dcb.showDicToast())
										cr.showDicToast(cr.getString(R.string.dict_err),
												cr.getString(R.string.http_error) + " " + response.code(),
												DicToastView.IS_DEEPL, "", fullScreen);
									dcb.fail(null, cr.getString(R.string.http_error) + " " + response.code());
								}
							}, 100));
					deeplAuthenticated = false;
					return;
				}
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					try {
						JSONObject jso = new JSONObject(sBody);
						if (jso.has("translations")) {
							String sDic = "";
							JSONArray jsoT = jso.getJSONArray("translations");
							if (jsoT.length() > 0) {
								if (jsoT.getJSONObject(0).has("detected_source_language"))
									sDic = "Deepl.com, " + cr.getString(R.string.translated_from) + " " + jsoT.getJSONObject(0).getString("detected_source_language");
								if (jsoT.getJSONObject(0).has("text")) {
									String sTrans = jsoT.getJSONObject(0).getString("text");
									if (StrUtils.isEmptyStr(sTrans)) sTrans = cr.getString(R.string.not_found);
									if (dcb == null) {
										cr.showDicToast(s, sTrans, Toast.LENGTH_LONG, view, DicToastView.IS_DEEPL, sDic, fullScreen);
										if (!sTrans.equals(cr.getString(R.string.not_found)))
											Dictionaries.saveToDicSearchHistory(cr, s, sTrans, curDict, "");
									} else {
										dcb.done(sTrans, "");
										if (dcb.showDicToast()) {
											cr.showDicToast(s, sTrans, Toast.LENGTH_LONG, view, DicToastView.IS_DEEPL, sDic, fullScreen);
										}
										if (dcb.saveToHist()) {
											if (!sTrans.equals(cr.getString(R.string.not_found)))
												Dictionaries.saveToDicSearchHistory(cr, s, sTrans, curDict, "");
										}
									}
								} else {
									boolean bShowToast = dcb == null;
									if (!bShowToast) bShowToast = dcb.showDicToast();
									if (bShowToast) {
										cr.showDicToast(s, sBody, DicToastView.IS_DEEPL, "", fullScreen);
									} else dcb.fail(null, sBody);
								}
							} else {
								boolean bShowToast = dcb == null;
								if (!bShowToast) bShowToast = dcb.showDicToast();
								if (bShowToast) {
									cr.showDicToast(s, sBody, DicToastView.IS_DEEPL, "", fullScreen);
								} else dcb.fail(null, sBody);
							}
						} else {
							boolean bShowToast = dcb == null;
							if (!bShowToast) bShowToast = dcb.showDicToast();
							if (bShowToast) {
								cr.showDicToast(s, sBody, DicToastView.IS_DEEPL, "", fullScreen);
							} else dcb.fail(null, sBody);
						}
					} catch (Exception e) {
						boolean bShowToast = dcb == null;
						if (!bShowToast) bShowToast = dcb.showDicToast();
						if (bShowToast) {
							cr.showDicToast(s, sBody, DicToastView.IS_DEEPL, "", fullScreen);
						} else dcb.fail(e, e.getMessage());
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				deeplAuthenticated = false;
				if (unauthCntDeepl == 0) {
					unauthCntDeepl++;
					deeplAuthThenTranslate(cr, s, fullScreen, langf, lang, curDict, view, dcb);
				} else {
					if (dcb == null)
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
							cr.showDicToast(cr.getString(R.string.dict_err), e.getMessage(),
								DicToastView.IS_DEEPL, "", fullScreen)
						));
					else {
						dcb.fail(e, e.getMessage());
						if (dcb.showDicToast())
							BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
								cr.showDicToast(cr.getString(R.string.dict_err), e.getMessage(),
									DicToastView.IS_DEEPL, "", fullScreen)
							));
					}
					unauthCntDeepl = 0;
				}
			}
		});
	};

}
