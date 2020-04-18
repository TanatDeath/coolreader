package org.coolreader.cloud.yandex;

import org.coolreader.CoolReader;
import org.coolreader.cloud.CloudFileInfo;
import org.coolreader.cloud.CloudSync;
import org.coolreader.crengine.StrUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class YNDListFiles {

    String path = ""; // path listed
    public List<CloudFileInfo> fileList;
    public CoolReader mCoolReader = null;

    public YNDListFiles(CoolReader cr, String json, String fileMark, String sCRC, String sExt) {
        this(cr, json, "", fileMark, sCRC, false, sExt);
    }

    public YNDListFiles(CoolReader cr, String json, String findStr, boolean thisObj) {
        this(cr, json, findStr, "", "", thisObj, "");
    }

    public YNDListFiles(CoolReader cr, String json, String findStr, String fileMark, String sCRC,
                        boolean thisObj, String ext) {
        mCoolReader = cr;
        fileList = new ArrayList<CloudFileInfo>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("path")) path = jsonObject.get("path").toString();
            JSONArray items = null;
            if (jsonObject.has("_embedded")) {
                if (jsonObject.getJSONObject("_embedded").has("items"))
                    items = jsonObject.getJSONObject("_embedded").getJSONArray("items");
            }
            if (jsonObject.has("items"))
                items = jsonObject.getJSONArray("items");

            if ((items != null) || (thisObj)) {
                int i=0;
                int itSize = 1;
                if (!thisObj) itSize = items.length();
                boolean wasInit = false;
                while (i < itSize) {
                    JSONObject jso;
                    if (thisObj) jso = jsonObject; else jso = (JSONObject) items.get(i);
                    CloudFileInfo yf = new CloudFileInfo();
                    if (jso.has("name")) yf.name = jso.get("name").toString(); //2020-03-29_202206_rpos_635942216_36b928e773055c4a.json
                    if (jso.has("path")) yf.path = jso.get("path").toString(); //"disk:/CoolReader/2020-03-29_202206_rpos_635942216_36b928e773055c4a.json"
                    if (jso.has("type")) yf.type = jso.get("type").toString();
                    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    if (jso.has("created")) yf.created = sdf.parse(jso.get("created").toString());
                    if (jso.has("modified")) yf.modified = sdf.parse(jso.get("modified").toString());
                    if ((yf.path.contains("disk:/CoolReader/")) && (yf.path.contains(".json"))) {
                        if (!wasInit) CloudSync.checkFileForDeleteInit();
                        wasInit = true;
                        CloudSync.checkFileForDelete(yf);
                    }
                    if (StrUtils.isEmptyStr(findStr)) {
                        if (!StrUtils.isEmptyStr(fileMark)) {
                            boolean ok = yf.name.contains(fileMark);
                            if (!StrUtils.isEmptyStr(ext)) ok = ok && yf.name.contains("."+ext);
                            if (!StrUtils.isEmptyStr(sCRC)) ok = ok && yf.name.contains(sCRC);
                            if (ok) fileList.add(yf);
                        } else fileList.add(yf);
                    } else {
                       if (yf.name.toUpperCase().matches(".*" + findStr.toUpperCase() + ".*"))
                           fileList.add(yf);
                    }
                    i++;
                }
                if (wasInit) CloudSync.checkFileForDeleteFinish(mCoolReader);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
