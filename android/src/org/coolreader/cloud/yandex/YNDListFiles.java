package org.coolreader.cloud.yandex;

import org.coolreader.cloud.CloudFileInfo;
import org.coolreader.crengine.StrUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class YNDListFiles {

    String path = ""; // path listed
    public List<CloudFileInfo> fileList;

    public YNDListFiles(String json, String findStr) {
        fileList = new ArrayList<CloudFileInfo>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("path")) path = jsonObject.get("path").toString();
            JSONArray items = null;
            if (jsonObject.has("_embedded"))
                if (jsonObject.getJSONObject("_embedded").has("items"))
                    items = jsonObject.getJSONObject("_embedded").getJSONArray("items");
            if (jsonObject.has("items"))
                items = jsonObject.getJSONArray("items");

            if (items != null) {
                int i=0;
                while (i<items.length()) {
                    JSONObject jso = (JSONObject) items.get(i);
                    CloudFileInfo yf = new CloudFileInfo();
                    if (jso.has("name")) yf.name = jso.get("name").toString();
                    if (jso.has("path")) yf.path = jso.get("path").toString();
                    if (jso.has("type")) yf.type = jso.get("type").toString();
                    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    if (jso.has("created")) yf.created = sdf.parse(jso.get("created").toString());
                    if (jso.has("modified")) yf.modified = sdf.parse(jso.get("modified").toString());
                    if (StrUtils.isEmptyStr(findStr)) {
                        fileList.add(yf);
                    } else {
                       if (yf.name.toUpperCase().matches(".*" + findStr.toUpperCase() + ".*"))
                           fileList.add(yf);
                    }
                    i++;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
