package org.coolreader.cloud.litres;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class CreateJsons {

	public static final String LITRES_ADDR = "https://catalit.litres.ru/catalitv2";

	//public static final String DATE_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ssXXX"; // таймзона без двоеточия
	public static final String DATE_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // с двоеточием, но один хрен ((

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

	public static String getCurrentTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_1);
		dateFormat.setTimeZone(TimeZone.getDefault());
		Date today = Calendar.getInstance().getTime();
		return dateFormat.format(today);
	}

	public static JSONObject w_create_sid(String appId, String secret) throws JSONException, NoSuchAlgorithmException {
		JSONObject json = new JSONObject();
		json.put("app", appId);
		String sTime = getCurrentTime();
		json.put("time", sTime);
		String timeAndSecretSha = getSHA256(sTime + secret);
		json.put("sha", timeAndSecretSha);
		JSONArray jsa = new JSONArray();
		JSONObject jsreq = new JSONObject();
		jsreq.put("func", "w_create_sid");
		String  uniqueID = UUID.randomUUID().toString();
		jsreq.put("id", uniqueID);
		JSONObject jsparams = new JSONObject();
		jsparams.put("login","Anonymous");
		jsparams.put("pwd","0");
		jsparams.put("sid","0");
		jsreq.put("param", jsparams);
		jsa.put(jsreq);
		json.put("requests", jsa);
		return json;
	}

}
