package org.coolreader.dic;

import android.view.View;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.FlavourConstants;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.dic.DicToastView;
import org.coolreader.dic.Dictionaries;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.TreeMap;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class YandexTranslate {

	public static String sYandexIAM = "";
	public static String sYandexIAMexpiresAt = "";
	public static int unauthCntY = 0;

	public static final Logger log = L.create("cr3dict_ynd");

	public String yndGetDefLangCode(String langCode) {
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("en-us")) return "en";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("az-az")) return "az";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("nl-nl")) return "nl";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("fr-fr")) return "fr";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("de-de")) return "de";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("it-it")) return "it";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("ms-my")) return "ms";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("pt-pt")) return "pt";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("es-es")) return "es";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("uz-uz")) return "uz";
		if (StrUtils.getNonEmptyStr(langCode,true).equalsIgnoreCase("sv-se")) return "sv";
		return langCode;
	}

	public void yandexTranslate(CoolReader cr, String s, String langf, String lang,
								Dictionaries.DictInfo curDict, View view, Dictionaries.LangListCallback llc,
								CoolReader.DictionaryCallback dcb) {
		if (llc == null) {
			if (!FlavourConstants.PREMIUM_FEATURES) {
				cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.only_in_premium), DicToastView.IS_YANDEX, "");
				return;
			}
			if (StrUtils.isEmptyStr(lang)) {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err),
								cr.getString(R.string.translate_lang_not_set) + ": ["
										+ langf + "] -> [" + lang + "]", DicToastView.IS_YANDEX, ""), 100));
				return;
			}
		}
		if ((StrUtils.isEmptyStr(sYandexIAM))) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.cloud_need_authorization),
							DicToastView.IS_YANDEX, ""), 100));
			cr.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_DICTIONARY_TITLE);
			return;
		}
		if (StrUtils.isEmptyStr(cr.yndCloudSettings.folderId)) cr.readYndCloudSettings();
		if (StrUtils.isEmptyStr(cr.yndCloudSettings.folderId)) {
			BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
					cr.showDicToast(cr.getString(R.string.dict_err), cr.getString(R.string.cloud_need_authorization),
							DicToastView.IS_YANDEX, ""), 100));
			cr.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_DICTIONARY_TITLE);
			return;
		}
		HttpUrl.Builder urlBuilder;
		if (llc == null)
			urlBuilder = HttpUrl.parse(Dictionaries.YND_DIC_ONLINE_2).newBuilder();
		else
			urlBuilder = HttpUrl.parse(Dictionaries.YND_DIC_GETLANGS_2).newBuilder();
		String url = urlBuilder.build().toString();
		JSONObject json = new JSONObject();
		try {
			json.put("folder_id", cr.yndCloudSettings.folderId);
			if (llc == null) {
				json.put("texts", s);
				if (!StrUtils.isEmptyStr(langf))
					if (!langf.equals("#"))
						json.put("sourceLanguageCode", langf);
				json.put("targetLanguageCode", lang);
			}
		} catch (JSONException e) {
			log.e("yandexAuthThenTranslate json error", e);
		}
		RequestBody body = RequestBody.create(
				MediaType.parse("application/json"), json.toString());
		Request request = new Request.Builder()
				.header("Authorization","Bearer "+sYandexIAM)
				.post(body)
				.url(url)
				.build();
		Call call = Dictionaries.client.newCall(request);
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					if (llc == null) {
						try {
							String sText = "";
							String sLang = "";
							JSONObject jso = new JSONObject(sBody);
							if (jso.has("translations")) {
								JSONArray jsoa = jso.getJSONArray("translations");
								if (jsoa.length() > 0) {
									JSONObject jso2 = (JSONObject) jsoa.get(0);
									if (jso2.has("text")) sText = jso2.getString("text");
									if (jso2.has("detectedLanguageCode"))
										sLang = jso2.getString("detectedLanguageCode");
								}
								if (dcb == null) {
									cr.showDicToast(s, sText, DicToastView.IS_YANDEX, sLang);
									Dictionaries.saveToDicSearchHistory(cr, s, sText, curDict);
								} else {
									dcb.done(sText);
									if (dcb.showDicToast()) {
										cr.showDicToast(s, sText, DicToastView.IS_YANDEX, sLang);
									}
									if (dcb.saveToHist()) {
										Dictionaries.saveToDicSearchHistory(cr, s, sText, curDict);
									}
								}
							} else {
								if (dcb == null)
									cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
								else {
									dcb.fail(null, sBody);
									if (dcb.showDicToast())
										cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
								}
							}
						} catch (Exception e) {
							if (dcb == null)
								cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
							else {
								dcb.fail(e, e.getMessage());
								if (dcb.showDicToast())
									cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
							}
						}
					} else {
						TreeMap<String, String> langs = new TreeMap<>();
						try {
							String sLang = "";
							String sName = "";
							JSONObject jso = new JSONObject(sBody);
							if (jso.has("languages")) {
								JSONArray jsoa = jso.getJSONArray("languages");
								for (int i = 0; i < jsoa.length(); i++) {
									JSONObject jso2 = (JSONObject) jsoa.get(i);
									if (jso2.has("code")) sLang = jso2.getString("code");
									if (jso2.has("name")) sName = jso2.getString("name");
									if (!StrUtils.isEmptyStr(sLang)) {
										langs.put(sLang, sName);
									}
								}
								llc.click(langs);
							} else cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
						} catch (Exception e) {
							cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
						}
					}
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				sYandexIAM = "";
				if (unauthCntY == 0) {
					unauthCntY++;
					yandexAuthThenTranslate(cr, s, langf, lang, curDict, view, llc, dcb);
				} else {
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() ->
						cr.showDicToast(cr.getString(R.string.dict_err), e.getMessage(),
							DicToastView.IS_YANDEX, "")
					));
					unauthCntY = 0;
				}
			}
		});
	};

	public void yandexAuthThenTranslate(CoolReader cr, String s, String langf, String lang,
										Dictionaries.DictInfo curDict, View view, Dictionaries.LangListCallback llc,
										CoolReader.DictionaryCallback dcb)  {
		cr.readYndCloudSettings();
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.YND_DIC_GET_TOKEN).newBuilder();
		String url = urlBuilder.build().toString();
		JSONObject json = new JSONObject();
		try {
			json.put("yandexPassportOauthToken", cr.yndCloudSettings.oauthToken);
		} catch (JSONException e) {
			log.e("yandexAuthThenTranslate json error", e);
		}
		RequestBody body = RequestBody.create(
				MediaType.parse("application/json"), json.toString());
		Request request = new Request.Builder()
				.post(body)
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
						if (jso.has("iamToken")) sYandexIAM = jso.getString("iamToken");
						if (jso.has("expiresAt")) sYandexIAMexpiresAt = jso.getString("expiresAt");
						yandexTranslate(cr, s, yndGetDefLangCode(langf), yndGetDefLangCode(lang), curDict ,view, llc, dcb);
					} catch (Exception e) {
						if (dcb == null)
							cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
						else {
							if (dcb.showDicToast())
								cr.showDicToast(s, sBody, DicToastView.IS_YANDEX, "");
							dcb.fail(e, e.getMessage());
						}
					}
				}, 100));
			}
			public void onFailure(Call call, IOException e) {
				BackgroundThread.instance().postBackground(() ->
						BackgroundThread.instance().postGUI(() ->
										cr.showDicToast(cr.getString(R.string.dict_err), e.getMessage(),
												DicToastView.IS_YANDEX, "")
								, 100));
			}
		});
	}

}