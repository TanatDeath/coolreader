package org.coolreader.cloud.litres;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.History;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import okhttp3.HttpUrl;

import static org.coolreader.crengine.Utils.cleanupHtmlTags;

public class LitresJsons {

	public static final String LITRES_ADDR = "https://catalit.litres.ru/catalitv2";
	public static final String LITRES_ADDR_DOWNLOAD = "https://catalit.litres.ru/pages/catalit_download_book";

	public static final String DATE_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ssXXX"; // таймзона без двоеточия
	//public static final String DATE_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // с двоеточием, но один хрен ((
	public static final String DATE_FORMAT_2 = "yyyy-MM-dd'T'HH:mm:ss";
	public static final int HOUR = 1000 * 60 * 60;

	private static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if(hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	private static String getSHA256(String s) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] encodedhash = digest.digest(s.getBytes());
		return bytesToHex(encodedhash);
	}

	public static String getCurrentTimeOld() { // better, but dont works on old androids
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_1);
		dateFormat.setTimeZone(TimeZone.getDefault());
		Date today = Calendar.getInstance().getTime();
		return dateFormat.format(today);
	}

	public static String getCurrentTime() { // think later about 30m timezone shift
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_2);
		dateFormat.setTimeZone(TimeZone.getDefault());
		Date today = Calendar.getInstance().getTime();
		String sTime = dateFormat.format(today);
		TimeZone tz = TimeZone.getDefault();
		int offs = tz.getRawOffset() / HOUR;
		String soffs = ((offs < 0) ? "-" : "+") + String.format("%02d", Math.abs(offs));
		sTime = sTime + soffs + ":00";
		//sTime = "2020-09-23T18:32:56+00:00";
		return sTime;
	}


	public static JSONObject w_create_sid(String uuid, String appId, String secret,
										  String login, String passw) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("func", "w_create_sid");
		jsreq.put("id", uuid);
		JSONObject jsparams = new JSONObject();
		jsparams.put("login",login);
		jsparams.put("pwd",passw);
		jsparams.put("sid","0");
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static JSONObject w_actualize_sid(String uuid, String appId, String secret, CoolReader cr) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		json.put("sid", cr.litresCloudSettings.sessionId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("func", "w_actualize_sid");
		jsreq.put("id", uuid);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static boolean parse_w_create_sid(String uuid, CoolReader cr, String jsonBody) throws JSONException {
		boolean res = false;
		JSONObject json = new JSONObject(jsonBody);
		if (json.has(uuid)) {
			JSONObject json2 = (JSONObject) json.get(uuid);
			if (json2.has("sid")) cr.litresCloudSettings.sessionId = json2.getString("sid");
			if (json2.has("success")) res = json2.getBoolean("success");
			if (json2.has("country")) LitresConfig.country = json2.getString("country");
			if (json2.has("currency")) LitresConfig.currency = json2.getString("currency");
			if (json2.has("region")) LitresConfig.region = json2.getString("region");
			if (json2.has("city")) LitresConfig.city = json2.getString("city");
			if (res) LitresConfig.didLogin = true;
			LitresConfig.whenLogin = System.currentTimeMillis();
		}
		return res;
	}

	public static LitresError parse_error(String uuid, String jsonBody) throws JSONException {
		LitresError le = new LitresError("", "");
		JSONObject json = new JSONObject(jsonBody);
		if (json.has(uuid)) {
			JSONObject json2 = (JSONObject) json.get(uuid);
			if (json2.has("error_code")) le.errorCode = json2.getString("error_code");
			if (json2.has("error_message")) le.errorText = json2.getString("error_message");
		}
		return le;
	}

	public static JSONObject r_profile(String uuid, String appId, String secret,
										   String sid) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		JSONArray jsa2 = new JSONArray();
		jsa2.put("money_details");
		jsa2.put("mail");
		jsa2.put("login");
		jsa2.put("first_name");
		jsa2.put("middle_name");
		jsa2.put("last_name");
		jsa2.put("nickname");
		jsa2.put("phone");
		jsa2.put("birth_date");
		jsa2.put("gender");
		jsa2.put("reviews_cnt");
		jsa2.put("quotes_cnt");
		jsa2.put("biblio");
		jsa2.put("subscr");
		jsa2.put("socnet");
		jsa2.put("is_megafone_user");
		jsa2.put("subscr_profit");
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("func", "r_profile");
		jsreq.put("id", uuid);
		JSONObject jsfields = new JSONObject();
		jsfields.put("fields", jsa2);
		jsreq.put("param", jsfields);
		jsa.put(jsreq);
		json.put("requests", jsa);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		json.put("mobile_app", 1);
		//json.put("app", appId);
		json.put("sid", sid);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		return json;
	}

	public static JSONObject r_genres_list(String uuid, String appId, String secret,
										  String sid) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("func", "r_genres_list");
		jsreq.put("id", uuid);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static void parse_genre_sub(FileInfo item, JSONArray jsa) throws JSONException {
		for (int i = 0; i < jsa.length(); i++) {
			JSONObject jso = (JSONObject) jsa.get(i);
			String name = "undefined";
			String id = "0";
			if (jso.has("name")) name = jso.getString("name");
			if (jso.has("id")) id = jso.getString("id");
			FileInfo item1 = new FileInfo();
			item1.isDirectory = true;
			if (jso.has("sub"))
				item1.pathname = FileInfo.LITRES_GENRE_GROUP_PREFIX + id;
			else
				item1.pathname = FileInfo.LITRES_GENRE_PREFIX + id;
			item1.setFilename(name);
			item1.isListed = true;
			item1.isScanned = true;
			item1.id = Long.parseLong(id);
			item1.tag = "";
			item1.parent = item;
			String top_arts = "";
			if (jso.has("top_arts")) {
				JSONArray jsonaP = (JSONArray) jso.get("top_arts");
				for (int ii = 0; ii < jsonaP.length(); ii++) {
					JSONObject jsoP = (JSONObject) jsonaP.get(ii);
					if (jsoP.has("name")) {
						top_arts = top_arts + "|" +jsoP.getString("name");
					}
				}
			}
			if (top_arts.length()>0) item.top_arts = top_arts.substring(1);
			item.addDir(item1);
			if (jso.has("sub")) parse_genre_sub(item1, (JSONArray) jso.get("sub"));
		}
	}

	public static ArrayList<FileInfo> parse_r_genres_list(String uuid, CoolReader cr, String jsonBody, CloudAction ca) throws JSONException {
		ArrayList<FileInfo> list = new ArrayList<>();
		if (ca.lsp != null)
			if (ca.lsp.beginIndex > 0) {
				FileInfo item = new FileInfo();
				item.isDirectory = true;
				item.pathname = FileInfo.LITRES_GENRE_GROUP_PREFIX + "prevpage";
				item.setFilename(cr.getString(R.string.prev_page));
				item.isListed = true;
				item.isScanned = true;
				item.id = -1L;
				item.tag = "";
				item.lsp = ca.lsp.copy();
				item.lsp.prevPage();
				list.add(item);
			}
		boolean bFirst = true;
		JSONObject json = new JSONObject(jsonBody);
		if (json.has(uuid)) {
			JSONObject json2 = (JSONObject) json.get(uuid);
			if (json2.has("genres")) {
				JSONArray jsona = (JSONArray) json2.get("genres");
				for (int i = 0; i < jsona.length(); i++) {
					if (bFirst) {
						bFirst = false;
						if (ca.lsp != null) {
							if (ca.lsp.count == jsona.length()) {
								FileInfo item = new FileInfo();
								item.isDirectory = true;
								item.pathname = FileInfo.LITRES_GENRE_GROUP_PREFIX + "nextpage";
								item.setFilename(cr.getString(R.string.next_page));
								item.isListed = true;
								item.isScanned = true;
								item.id = -2L;
								item.tag = "";
								item.lsp = ca.lsp.copy();
								item.lsp.nextPage();
								list.add(item);
							}
						}
					}
					JSONObject jso = (JSONObject) jsona.get(i);
					String name = "undefined";
					String id = "0";
					if (jso.has("name")) name = jso.getString("name");
					if (jso.has("id")) id = jso.getString("id");
					FileInfo item = new FileInfo();
					item.isDirectory = true;
					if (jso.has("sub"))
						item.pathname = FileInfo.LITRES_GENRE_GROUP_PREFIX + id;
					else
						item.pathname = FileInfo.LITRES_GENRE_PREFIX + id;
					item.setFilename(name);
					item.isListed = true;
					item.isScanned = true;
					item.id = Long.parseLong(id);
					item.tag = "";
					String top_arts = "";
					if (jso.has("top_arts")) {
						JSONArray jsonaP = (JSONArray) jso.get("top_arts");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("name")) {
								top_arts = top_arts + "|" +jsoP.getString("name");
							}
						}
					}
					if (top_arts.length()>0) item.top_arts = top_arts.substring(1);
					list.add(item);
					if (jso.has("sub")) parse_genre_sub(item, (JSONArray) jso.get("sub"));
				}
			}
		}
		return list;
	}

	public static ArrayList<FileInfo> parse_r_collection_list(String uuid, CoolReader cr, String jsonBody, CloudAction ca) throws JSONException {
		ArrayList<FileInfo> list = new ArrayList<>();
		if (ca.lsp != null)
			if (ca.lsp.beginIndex > 0) {
				FileInfo item = new FileInfo();
				item.isDirectory = true;
				item.pathname = FileInfo.LITRES_COLLECTION_GROUP_PREFIX + "prevpage";
				item.setFilename(cr.getString(R.string.prev_page));
				item.isListed = true;
				item.isScanned = true;
				item.id = -1L;
				item.tag = "";
				item.lsp = ca.lsp.copy();
				item.lsp.prevPage();
				list.add(item);
			}
		boolean bFirst = true;
		JSONObject json = new JSONObject(jsonBody);
		if (json.has(uuid)) {
			JSONObject json2 = (JSONObject) json.get(uuid);
			if (json2.has("collections")) {
				JSONArray jsona = (JSONArray) json2.get("collections");
				for (int i = 0; i < jsona.length(); i++) {
					if (bFirst) {
						bFirst = false;
						if (ca.lsp != null) {
							if (ca.lsp.count == jsona.length()) {
								FileInfo item = new FileInfo();
								item.isDirectory = true;
								item.pathname = FileInfo.LITRES_COLLECTION_GROUP_PREFIX + "nextpage";
								item.setFilename(cr.getString(R.string.next_page));
								item.isListed = true;
								item.isScanned = true;
								item.id = -2L;
								item.tag = "";
								item.lsp = ca.lsp.copy();
								item.lsp.nextPage();
								list.add(item);
							}
						}
					}
					JSONObject jso = (JSONObject) jsona.get(i);
					String name = "undefined";
					String id = "0";
					if (jso.has("category_name")) name = jso.getString("category_name");
					if (jso.has("id")) id = jso.getString("id");
					FileInfo item = new FileInfo();
					item.isDirectory = true;
					item.pathname = FileInfo.LITRES_COLLECTION_PREFIX + id;
					item.setFilename(name);
					item.isListed = true;
					item.isScanned = true;
					item.id = Long.parseLong(id);
					item.tag = "";
					String top_arts = "";
					if (jso.has("top_arts")) {
						JSONArray jsonaP = (JSONArray) jso.get("top_arts");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("name")) {
								top_arts = top_arts + "|" +jsoP.getString("name");
							}
						}
					}
					String description_html = "";
					if (jso.has("description_html")) description_html = cleanupHtmlTags(jso.getString("description_html"));
					item.annotation = description_html;
					if (top_arts.length()>0) item.top_arts = top_arts.substring(1);
					if (jso.has("arts_n")) item.arts_n = jso.getInt("arts_n");
					list.add(item);
				}
			}
		}
		return list;
	}

	public static ArrayList<FileInfo> parse_r_sequence_list(String uuid, CoolReader cr, String jsonBody, CloudAction ca) throws JSONException {
		ArrayList<FileInfo> list = new ArrayList<>();
		if (ca.lsp != null)
			if (ca.lsp.beginIndex > 0) {
				FileInfo item = new FileInfo();
				item.isDirectory = true;
				item.pathname = FileInfo.LITRES_SEQUENCE_GROUP_PREFIX + "prevpage";
				item.setFilename(cr.getString(R.string.prev_page));
				item.isListed = true;
				item.isScanned = true;
				item.id = -1L;
				item.tag = "";
				item.lsp = ca.lsp.copy();
				item.lsp.prevPage();
				list.add(item);
			}
		boolean bFirst = true;
		JSONObject json = new JSONObject(jsonBody);
		if (json.has(uuid)) {
			JSONObject json2 = (JSONObject) json.get(uuid);
			if (json2.has("sequences")) {
				JSONArray jsona = (JSONArray) json2.get("sequences");
				for (int i = 0; i < jsona.length(); i++) {
					if (bFirst) {
						bFirst = false;
						if (ca.lsp != null) {
							if (ca.lsp.count == jsona.length()) {
								FileInfo item = new FileInfo();
								item.isDirectory = true;
								item.pathname = FileInfo.LITRES_SEQUENCE_GROUP_PREFIX + "nextpage";
								item.setFilename(cr.getString(R.string.next_page));
								item.isListed = true;
								item.isScanned = true;
								item.id = -2L;
								item.tag = "";
								item.lsp = ca.lsp.copy();
								item.lsp.nextPage();
								list.add(item);
							}
						}
					}
					JSONObject jso = (JSONObject) jsona.get(i);
					String name = "undefined";
					String id = "0";
					if (jso.has("name")) name = jso.getString("name");
					if (jso.has("id")) id = jso.getString("id");
					FileInfo item = new FileInfo();
					item.isDirectory = true;
					item.pathname = FileInfo.LITRES_SEQUENCE_PREFIX + id;
					item.setFilename(name);
					item.isListed = true;
					item.isScanned = true;
					item.id = Long.parseLong(id);
					item.tag = "";
					String top_arts = "";
					if (jso.has("top_arts")) {
						JSONArray jsonaP = (JSONArray) jso.get("top_arts");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("name")) {
								top_arts = top_arts + "|" +jsoP.getString("name");
							}
						}
					}
					if (top_arts.length()>0) item.top_arts = top_arts.substring(1);
					String top_genres = "";
					if (jso.has("top_genres")) {
						JSONArray jsonaP = (JSONArray) jso.get("top_genres");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("name")) {
								top_genres = top_genres + "|" +jsoP.getString("name");
							}
						}
					}
					if (top_genres.length()>0) item.top_genres = top_genres.substring(1);
					if (jso.has("arts_n")) item.arts_n = jso.getInt("arts_n");
					list.add(item);
				}
			}
		}
		return list;
	}

	public static JSONObject r_search_arts(String uuid, String appId, String secret,
										   String sid, CoolReader cr, CloudAction ca) throws JSONException, NoSuchAlgorithmException {
		String newOrPop = "new";
		if (ca.lsp.newOrPop > 0) newOrPop = "pop";
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("id", uuid);
		JSONObject jsparams = new JSONObject();
		if (ca.lsp.groupId > 0) {
			if (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS_BY_GENRE) {
				jsreq.put("func", "r_genre_arts_" + newOrPop);
				jsparams.put("genre", ca.lsp.groupId);
			}
			if (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS_BY_COLLECTION) {
				jsreq.put("func", "r_collection_arts_" + newOrPop);
				jsparams.put("collection", ca.lsp.groupId);
			}
			if (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS_BY_SEQUENCE) {
				jsreq.put("func", "r_sequence_arts_" + newOrPop);
				jsparams.put("sequence", ca.lsp.groupId);
			}
			if (ca.lsp.searchType == LitresSearchParams.SEARCH_TYPE_ARTS_BY_PERSON) {
				jsreq.put("func", "r_person_arts_" + newOrPop);
				jsparams.put("person", ca.lsp.groupId);
//				if (ca.lsp.searchModifier != LitresMainDialog.PM_ONLY_AUTHORS) {
//					JSONArray jsaPersons = new JSONArray();
//					jsaPersons.put(0); //Автор текста
//					jsaPersons.put(1); //Переводчик
//					jsaPersons.put(2); //Агент
//					jsaPersons.put(3); //Художник
//					jsaPersons.put(4); //Составитель
//					jsaPersons.put(5); //Пересказчик
//					jsaPersons.put(6); //Чтец
//					jsaPersons.put(7); //Исполнитель
//					jsaPersons.put(8); //Производитель
//					jsaPersons.put(9); //Редактор
//					jsaPersons.put(10); //Актер
//					jsaPersons.put(11); //Режиссер
//					jsaPersons.put(15); //Продюсер
//					jsaPersons.put(19); //Композитор
//					jsaPersons.put(23); //Звукорежиссер
//					jsaPersons.put(27); //Сценарист
//					jsparams.put("type", jsaPersons);
//				}
			}
		}
		else {
			jsreq.put("func", "r_search_arts");
			jsparams.put("q", ca.lsp.searchString);
		}
		JSONArray jsa2 = new JSONArray();
		jsa2.put(ca.lsp.beginIndex);
		jsa2.put(ca.lsp.count);
		jsparams.put("limit", jsa2);
		jsparams.put("anno", "1");
		if (ca.lsp.searchModifier == LitresMainDialog.SM_STARTSWITH)
			jsparams.put("strict", "start");
		else
			if (ca.lsp.searchModifier == LitresMainDialog.SM_STRICT)
				jsparams.put("strict", "exact");
			else
				jsparams.put("strict", "no");
		jsparams.put("currency", LitresConfig.currency);
		jsparams.put("atype", "1");
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static JSONObject r_my_arts(String uuid, String appId, String secret,
										   String sid, CoolReader cr, CloudAction ca) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("func", "r_my_arts_all");
		jsreq.put("id", uuid);
		JSONObject jsparams = new JSONObject();
		JSONArray jsa2 = new JSONArray();
		jsa2.put(ca.lsp.beginIndex);
		jsa2.put(ca.lsp.count);
		jsparams.put("limit", jsa2);
		jsparams.put("anno", "1");
		jsparams.put("atype", "1");
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static ArrayList<FileInfo> parse_r_search_arts(String uuid, CoolReader cr, CloudAction ca, String jsonBody) throws JSONException, MalformedURLException {
		ArrayList<FileInfo> list = new ArrayList<>();
		if (ca.lsp != null)
			if (ca.lsp.beginIndex > 0) {
				FileInfo item = new FileInfo();
				item.isDirectory = true;
				item.pathname = FileInfo.LITRES_BOOKS_GROUP_PREFIX + "prevpage";
				item.setFilename(cr.getString(R.string.prev_page));
				item.isListed = true;
				item.isScanned = true;
				item.id = -1L;
				item.tag = "";
				item.lsp = ca.lsp.copy();
				item.lsp.prevPage();
				list.add(item);
			}
		boolean bFirst = true;
		JSONObject json = new JSONObject(jsonBody);
		if (json.has(uuid)) {
			JSONObject json2 = (JSONObject) json.get(uuid);
			JSONArray jsona = null;
			if (json2.has("arts"))
				jsona = (JSONArray) json2.get("arts");
			if (jsona == null) {
				if (json2.has("genre_arts")) {
					JSONObject jsono2 = (JSONObject) json2.get("genre_arts");
					if (jsono2.has("arts"))
						jsona = (JSONArray) json2.get("arts");
				}
				if (json2.has("collection_arts_pop")) {
					JSONObject jsono2 = (JSONObject) json2.get("collection_arts_pop");
					if (jsono2.has("arts"))
						jsona = (JSONArray) json2.get("arts");
				}
				if (json2.has("collection_arts_new")) {
					JSONObject jsono2 = (JSONObject) json2.get("collection_arts_new");
					if (jsono2.has("arts"))
						jsona = (JSONArray) json2.get("arts");
				}
				if (json2.has("person_arts_pop")) {
					JSONObject jsono2 = (JSONObject) json2.get("person_arts_pop");
					if (jsono2.has("arts"))
						jsona = (JSONArray) json2.get("arts");
				}
				if (json2.has("person_arts_new")) {
					JSONObject jsono2 = (JSONObject) json2.get("person_arts_new");
					if (jsono2.has("arts"))
						jsona = (JSONArray) json2.get("arts");
				}
			}
			if (jsona != null) {
				for (int i = 0; i < jsona.length(); i++) {
					if (bFirst) {
						bFirst = false;
						if (ca.lsp != null) {
							if (ca.lsp.count == jsona.length()) {
								FileInfo item = new FileInfo();
								item.isDirectory = true;
								item.pathname = FileInfo.LITRES_BOOKS_GROUP_PREFIX + "nextpage";
								item.setFilename(cr.getString(R.string.next_page));
								item.isListed = true;
								item.isScanned = true;
								item.id = -2L;
								item.tag = "";
								item.lsp = ca.lsp.copy();
								item.lsp.nextPage();
								list.add(item);
							}
						}
					}
					JSONObject jso = (JSONObject) jsona.get(i);
					String title = "undefined";
					String id = "0";
					if (jso.has("title")) title = jso.getString("title");
					if (jso.has("id")) id = jso.getString("id");
					FileInfo item = new FileInfo();
					item.isDirectory = false;
					item.pathname = FileInfo.LITRES_BOOKS_PREFIX + id;
					item.setFilename(title);
					item.setTitle(title);
					String authors = "";
					if (jso.has("persons")) {
						JSONArray jsonaP = (JSONArray) jso.get("persons");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("type")) {
								if (jsoP.getString("type").equals("0")) {
									if (jsoP.has("full_name")) {
										authors = authors + "|" +jsoP.getString("full_name");
									}
								}
							}
						}
					}
					if (authors.length()>0) item.setAuthors(authors.substring(1));
					String genres = "";
					if (jso.has("genres")) {
						JSONArray jsonaP = (JSONArray) jso.get("genres");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("name")) {
								genres = genres + "|" +jsoP.getString("name");
							}
						}
					}
					if (genres.length()>0) item.setGenres(genres.substring(1));
					String sequences = "";
					if (jso.has("sequences")) {
						JSONArray jsonaP = (JSONArray) jso.get("sequences");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("name")) {
								sequences = sequences + "|" +jsoP.getString("name");
							}
						}
					}
					if (sequences.length()>0) item.setSeriesName(sequences.substring(1));
					if (jso.has("annotation")) item.annotation =  cleanupHtmlTags(jso.getString("annotation"));
					if (jso.has("cover")) item.cover_href =  jso.getString("cover");
					item.cover_href2 = "";
					if (id.length()>1) {
						String ss = id.substring(id.length() - 2, id.length() - 1);
						item.cover_href2 = "https://cv" + ss + ".litres.ru/pub/c/cover/" + id + ".jpg";
						item.fragment_href = "https://cv" + ss + ".litres.ru/pub/t/" + id;
					}
					String redir_url = Utils.getUrlLoc(new java.net.URL(LitresJsons.LITRES_ADDR_DOWNLOAD));
					if (StrUtils.isEmptyStr(redir_url)) redir_url = LitresJsons.LITRES_ADDR_DOWNLOAD;
					HttpUrl.Builder urlBuilder = HttpUrl.parse(redir_url).newBuilder();
					urlBuilder.addQueryParameter("sid", cr.litresCloudSettings.sessionId);
					urlBuilder.addQueryParameter("art", "" + id);
					String url = urlBuilder.build().toString();
					item.full_href = url;
					item.full_href_wo_sid = url.replace(cr.litresCloudSettings.sessionId, "[sid]");

					if (jso.has("publisher")) item.publisher =  jso.getString("publisher");
					if (jso.has("isbn")) item.publisbn =  jso.getString("isbn");
					if (jso.has("year")) item.setPublyear(jso.getString("year"));
					if (jso.has("year_written")) item.setBookdate(jso.getString("year_written"));
					if (jso.has("final_price")) item.finalPrice = jso.getDouble("final_price");
					if (jso.has("free")) item.free =  jso.getInt("free");
					if (jso.has("available")) item.available =  jso.getInt("available");
					if (jso.has("available_date")) item.setAvaildate(jso.getString("available_date"));
					if (jso.has("type")) item.type = jso.getInt("type");
					if (jso.has("lvl")) item.lvl = jso.getInt("lvl");
					if (jso.has("lang")) item.language = jso.getString("lang");
					if (jso.has("minage")) item.minage = jso.getString("minage");
					if (jso.has("subtitle")) item.minage = jso.getString("subtitle");
					item.lsp = ca.lsp.copy();

					item.id = Long.parseLong(id);
					item.tag = "";
					Services.getHistory().getFileInfoByOPDSLink(cr.getDB(), item.fragment_href + "|" + item.full_href_wo_sid, true,
							new History.FileInfo1LoadedCallback() {

								@Override
								public void onFileInfoLoadBegin() {

								}

								@Override
								public void onFileInfoLoaded(final FileInfo fileInfo) {
									if (fileInfo!=null) {
										if (fileInfo.exists()) {
											item.pathnameR = fileInfo.pathname;
											item.arcnameR = fileInfo.arcname;
											item.pathR = fileInfo.path;
											item.opdsLinkR = item.fragment_href;
											item.opdsLinkRfull = item.full_href_wo_sid;
										}
									}
								}
							}
					);
					list.add(item);
				}
			}
		}
		return list;
	}

	public static JSONObject w_buy_arts(String uuid, String appId, String secret,
										   String sid, CoolReader cr, CloudAction ca) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("func", "w_buy_art");
		jsreq.put("id", uuid);
		JSONObject jsparams = new JSONObject();
		JSONArray jsa2 = new JSONArray();
		jsa2.put(ca.param);
		jsparams.put("arts", jsa2);
		jsparams.put("lfrom", appId);
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static LitresPurchaseInfo parse_w_buy_arts(String uuid, CoolReader cr, String jsonBody) throws JSONException {
		LitresPurchaseInfo lpi = new LitresPurchaseInfo();
		JSONObject json = new JSONObject(jsonBody);
		if (json.has(uuid)) {
			JSONObject json2 = (JSONObject) json.get(uuid);
			if (json2.has("success")) lpi.success = json2.getBoolean("success");
			if (json2.has("error_message")) lpi.errorMessage = json2.getString("error_message");
			if (json2.has("error_code")) lpi.errorCode = json2.getInt("error_code");
			if (json2.has("baskets")) {
				JSONObject json3 = (JSONObject) json2.get("baskets");
				Iterator keys = json3.keys();
				while(keys.hasNext()) {
					String key = (String) keys.next();
					String value = json3.getString(key);
					lpi.baskets.put(key, value);
				}
			}
		}
		return lpi;
	}

	public static JSONObject r_search_genres(String uuid, String appId, String secret,
										   String sid, CoolReader cr, CloudAction ca) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("id", uuid);
		JSONObject jsparams = new JSONObject();
		jsreq.put("func", "r_search_genres");
		jsparams.put("q", ca.lsp.searchString);
		jsparams.put("tags", "1");
		JSONArray jsa2 = new JSONArray();
		jsa2.put(ca.lsp.beginIndex);
		jsa2.put(ca.lsp.count);
		jsparams.put("limit", jsa2);
		if (ca.lsp.searchModifier == LitresMainDialog.SM_STARTSWITH)
			jsparams.put("strict", "start");
		else
		if (ca.lsp.searchModifier == LitresMainDialog.SM_STRICT)
			jsparams.put("strict", "exact");
		else
			jsparams.put("strict", "no");
		jsparams.put("atype", "1");
		jsparams.put("top_n_arts", "3");
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static JSONObject r_search_collections(String uuid, String appId, String secret,
											 String sid, CoolReader cr, CloudAction ca) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("id", uuid);
		JSONObject jsparams = new JSONObject();
		jsreq.put("func", "r_search_collections");
		jsparams.put("q", ca.lsp.searchString);
		JSONArray jsa2 = new JSONArray();
		jsa2.put(ca.lsp.beginIndex);
		jsa2.put(ca.lsp.count);
		jsparams.put("limit", jsa2);
		if (ca.lsp.searchModifier == LitresMainDialog.SM_STARTSWITH)
			jsparams.put("strict", "start");
		else
		if (ca.lsp.searchModifier == LitresMainDialog.SM_STRICT)
			jsparams.put("strict", "exact");
		else
			jsparams.put("strict", "no");
		jsparams.put("descr", "1");
		jsparams.put("top_n_arts", "3");
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static JSONObject r_search_sequences(String uuid, String appId, String secret,
												  String sid, CoolReader cr, CloudAction ca) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("id", uuid);
		JSONObject jsparams = new JSONObject();
		jsreq.put("func", "r_search_sequences");
		jsparams.put("q", ca.lsp.searchString);
		JSONArray jsa2 = new JSONArray();
		jsa2.put(ca.lsp.beginIndex);
		jsa2.put(ca.lsp.count);
		jsparams.put("limit", jsa2);
		if (ca.lsp.searchModifier == LitresMainDialog.SM_STARTSWITH)
			jsparams.put("strict", "start");
		else
		if (ca.lsp.searchModifier == LitresMainDialog.SM_STRICT)
			jsparams.put("strict", "exact");
		else
			jsparams.put("strict", "no");
		jsparams.put("descr", "1");
		jsparams.put("top_n_arts", "3");
		jsparams.put("top_n_genres", "3");
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static JSONObject r_search_persons(String uuid, String appId, String secret,
											 String sid, CoolReader cr, CloudAction ca) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("id", uuid);
		JSONObject jsparams = new JSONObject();
		jsreq.put("func", "r_search_persons");
		if (!StrUtils.isEmptyStr(ca.lsp.lastName)) {
			jsparams.put("last", ca.lsp.lastName);
			if (!StrUtils.isEmptyStr(ca.lsp.firstName)) jsparams.put("first", ca.lsp.firstName);
			if (!StrUtils.isEmptyStr(ca.lsp.middleName)) jsparams.put("middle", ca.lsp.middleName);
		} else
			jsparams.put("q", ca.lsp.searchString);
		JSONArray jsa2 = new JSONArray();
		jsa2.put(ca.lsp.beginIndex);
		jsa2.put(ca.lsp.count);
		jsparams.put("limit", jsa2);
		if (ca.lsp.searchModifier == LitresMainDialog.PM_ANY_PERSON) {
			JSONArray jsa3 = new JSONArray();
			for (int i = 0; i < 58; i++) {
				jsa3.put(i);
			}
			jsparams.put("type", jsa2);
		}
		jsparams.put("descr", "1");
		jsparams.put("top_n_arts", "3");
		jsparams.put("top_n_genres", "3");
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static JSONObject catalit_download_book(String uuid, String appId, String secret,
										String sid, CoolReader cr, CloudAction ca) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		json.put("sid", sid);
		json.put("art", ca.param);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("func", "catalit_download_book1");
		jsreq.put("id", uuid);
		//JSONObject jsparams = new JSONObject();
		//jsparams.put("art", ca.param);
		//jsparams.put("sid", sid);
		//jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

	public static ArrayList<FileInfo> parse_r_search_persons(String uuid, CoolReader cr, CloudAction ca, String jsonBody) throws JSONException {
		ArrayList<FileInfo> list = new ArrayList<>();
		if (ca.lsp != null)
			if (ca.lsp.beginIndex > 0) {
				FileInfo item = new FileInfo();
				item.isDirectory = true;
				item.pathname = FileInfo.LITRES_PERSONS_GROUP_PREFIX + "prevpage";
				item.setFilename(cr.getString(R.string.prev_page));
				item.isListed = true;
				item.isScanned = true;
				item.id = -1L;
				item.tag = "";
				item.lsp = ca.lsp.copy();
				item.lsp.prevPage();
				list.add(item);
			}
		boolean bFirst = true;
		JSONObject json = new JSONObject(jsonBody);
		if (json.has(uuid)) {
			JSONObject json2 = (JSONObject) json.get(uuid);
			JSONArray jsona = null;
			if (json2.has("persons"))
				jsona = (JSONArray) json2.get("persons");
			if (jsona != null) {
				for (int i = 0; i < jsona.length(); i++) {
					if (bFirst) {
						bFirst = false;
						if (ca.lsp != null) {
							if (ca.lsp.count == jsona.length()) {
								FileInfo item = new FileInfo();
								item.isDirectory = true;
								item.pathname = FileInfo.LITRES_PERSONS_GROUP_PREFIX + "nextpage";
								item.setFilename(cr.getString(R.string.next_page));
								item.isListed = true;
								item.isScanned = true;
								item.id = -2L;
								item.tag = "";
								item.lsp = ca.lsp.copy();
								item.lsp.nextPage();
								list.add(item);
							}
						}
					}
					JSONObject jso = (JSONObject) jsona.get(i);
					String fullName = "undefined";
					String id = "0";
					if (jso.has("full_name")) fullName = jso.getString("full_name");
					if (jso.has("id")) id = jso.getString("id");
					FileInfo item = new FileInfo();
					item.isDirectory = true;
					item.pathname = FileInfo.LITRES_PERSONS_PREFIX + id;
					item.isListed = true;
					item.isScanned = true;
					item.setFilename(fullName);
					item.setTitle(fullName);
					String description_html = "";
					if (jso.has("description_html")) description_html = cleanupHtmlTags(jso.getString("description_html"));
					item.annotation = description_html;
					String top_arts = "";
					if (jso.has("top_arts")) {
						JSONArray jsonaP = (JSONArray) jso.get("top_arts");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("name")) {
								top_arts = top_arts + "|" +jsoP.getString("name");
							}
						}
					}
					if (top_arts.length()>0) item.top_arts = top_arts.substring(1);
					String top_genres = "";
					if (jso.has("top_genres")) {
						JSONArray jsonaP = (JSONArray) jso.get("top_genres");
						for (int ii = 0; ii < jsonaP.length(); ii++) {
							JSONObject jsoP = (JSONObject) jsonaP.get(ii);
							if (jsoP.has("name")) {
								top_genres = top_genres + "|" +jsoP.getString("name");
							}
						}
					}
					if (top_genres.length()>0) item.top_genres = top_genres.substring(1);
					if (jso.has("img")) item.cover_href =  jso.getString("img");
					if (jso.has("lvl")) item.lvl = jso.getInt("lvl");
					if (jso.has("arts_n")) item.arts_n = jso.getInt("arts_n");
					if (jso.has("type")) item.type = jso.getInt("type");
					item.lsp = ca.lsp.copy();
					item.id = Long.parseLong(id);
					item.tag = "";
					list.add(item);
				}
			}
		}
		return list;
	}

}
