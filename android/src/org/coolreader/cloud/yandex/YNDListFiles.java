package org.coolreader.cloud.yandex;

import org.coolreader.crengine.StrUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class YNDListFiles {

    public class YNDListFile {
        public String name; //file name
        public Date created; // format: 2017-07-17T18:52:50+00:00
        public Date modified;
        public String path; // format disk:/name
        public String type;
    }

    String path = ""; // path listed
    public List<YNDListFile> fileList;

    public YNDListFiles(String json, String findStr) {
        fileList = new ArrayList<YNDListFile>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("path")) path = jsonObject.get("path").toString();
            if (jsonObject.has("_embedded")) {
                if (jsonObject.getJSONObject("_embedded").has("items")) {
                    JSONArray jsonArray = jsonObject.getJSONObject("_embedded").getJSONArray("items");
                    int i=0;
                    while (i<jsonArray.length()) {
                        JSONObject jso = (JSONObject) jsonArray.get(i);
                        YNDListFile yf = new YNDListFile();
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
