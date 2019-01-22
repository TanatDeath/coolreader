package org.coolreader.crengine;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class GenreSAXElem {

    public static BaseActivity mActivity;
    public static ArrayList<GenreSAXElem> elemList = new ArrayList<GenreSAXElem>();
    public static HashMap<String, String[]> elemList2 = new HashMap<String, String[]>();

    public String qName;
    public GenreSAXElem parentLink;
    public HashMap<String,String> hshAttrs;

    public GenreSAXElem(String qName, GenreSAXElem parentLink, HashMap<String,String> hshAttrs) {
        this.qName = qName;
        this.parentLink = parentLink;
        this.hshAttrs = hshAttrs;
        elemList.add(this);
    }

    public static GenreSAXElem getGenreDescr(String lang, String genre) {
        GenreSAXElem found = null;
        for (GenreSAXElem g: elemList) {
            if (g.hshAttrs!=null) {
                if (g.hshAttrs.get("value")!=null)
                    if ((g.hshAttrs.get("value").equalsIgnoreCase(genre))&&
                            (
                                    (g.qName.equalsIgnoreCase("genre"))||
                                            (g.qName.equalsIgnoreCase("subgenre"))||
                                            (g.qName.equalsIgnoreCase("genre-alt"))
                            )
                            ) {
                        found = g;
                        break;
                    }
            }
        }
        GenreSAXElem foundDescr = null;
        GenreSAXElem foundDescrEn = null;
        if (found!=null) {
            for (GenreSAXElem g: elemList) {
                if (g.hshAttrs!=null) {
                    if (
                            (g.parentLink==found) &&
                                    (
                                            (g.qName.equalsIgnoreCase("root-descr"))||
                                                    (g.qName.equalsIgnoreCase("genre-descr"))
                                    )
                            ) {
                        if (g.hshAttrs.get("lang")!=null) {
                            if (g.hshAttrs.get("lang").equalsIgnoreCase("en")) foundDescrEn = g;
                            if (g.hshAttrs.get("lang").equalsIgnoreCase(lang)) foundDescr = g;
                        }
                    }
                }
            }
            if ((foundDescrEn==null)&&(foundDescr==null)&&(found.parentLink!=null)) {
                for (GenreSAXElem g: elemList) {
                    if (g.hshAttrs!=null) {
                        if (
                                (g.parentLink==found.parentLink) &&
                                        (
                                                (g.qName.equalsIgnoreCase("root-descr"))||
                                                (g.qName.equalsIgnoreCase("genre-descr"))
                                        )
                                ) {
                            if (g.hshAttrs.get("lang")!=null) {
                                if (g.hshAttrs.get("lang").equalsIgnoreCase("en")) foundDescrEn = g;
                                if (g.hshAttrs.get("lang").equalsIgnoreCase(lang)) foundDescr = g;
                            }
                        }
                    }
                }
            }
        }
        if (foundDescr!=null) return foundDescr;
        return foundDescr;
    }

    public static void splitS(String str) {
        if (str.contains(" ")) {
            String sKey = str.split(" ")[0];
            if (!StrUtils.isEmptyStr(sKey)) {
                String sVal = str.substring(sKey.length()).trim();
                String sValAdd = "";
                if (!StrUtils.isEmptyStr(sVal)) {
                    if (sVal.contains("|")) {
                        String sVal2 = sVal.split("\\|")[0];
                        sValAdd = sVal.substring(sVal2.length()+1);
                        if (!StrUtils.isEmptyStr(sVal2)) sVal = sVal2;
                    }
                }
                String[] sV = {sVal, sValAdd};
                elemList2.put(sKey, sV);
            }
        }
    }

    public static void initGenreList() throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser = factory.newSAXParser();
        DefaultHandler handler = new DefaultHandler() {
            GenreSAXElem curElem = new GenreSAXElem("root", null, null);

            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                GenreSAXElem oldElem = curElem;
                HashMap<String, String> hshAttrs = new HashMap<String, String>();
                for (int i = 0; i < attributes.getLength(); i++) {
                    hshAttrs.put(attributes.getQName(i), attributes.getValue(i));
                }
                curElem = new GenreSAXElem(qName, oldElem, hshAttrs);
            }//end of startElement method

            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (curElem != null)
                    curElem = curElem.parentLink;
            }

            public void characters(char ch[], int start, int length) throws SAXException {

            }
            ; //end of DefaultHandler object
        };
        File[] dataDirs = Engine.getDataDirectories(null, false, true);
        File existingFile = null;
        File existingFile2 = null;
        for ( File dir : dataDirs ) {
            File f = new File(dir, "fb2genres_utf8_wo_bom.xml");
            if ( f.exists() && f.isFile() ) {
                existingFile = f;
                break;
            }
        }
        for ( File dir : dataDirs ) {
            File f = new File(dir, "genres_rus.txt");
            if ( f.exists() && f.isFile() ) {
                existingFile = f;
                break;
            }
        }
        InputStream targetStreamF = null;
        InputStream targetStream = null;
        if (existingFile!=null) targetStreamF = new FileInputStream(existingFile);
        targetStream = mActivity.getResources().openRawResource(R.raw.fb2genres_utf8_wo_bom);
        InputStream targetStreamF2 = null;
        InputStream targetStream2 = null;
        if (existingFile2!=null) targetStreamF2 = new FileInputStream(existingFile2);
        targetStream2 = mActivity.getResources().openRawResource(R.raw.genres_rus);
        if (targetStreamF!=null) {
            try {
                saxParser.parse(targetStreamF, handler);
                targetStreamF.close();
            } catch (Exception e) {
                mActivity.showToast("Could not parse genres from file: fb2genres_utf8_wo_bom.xml. "+e.getMessage());
                try {
                    saxParser.parse(targetStream, handler);
                } catch (Exception e1) {
                    mActivity.showToast("Could not parse genres from resource: fb2genres_utf8_wo_bom.xml. "+e1.getMessage());
                }
            }
        } else {
            try {
                saxParser.parse(targetStream, handler);
                targetStream.close();
            } catch (Exception e1) {
                mActivity.showToast("Could not parse genres from resource: fb2genres_utf8_wo_bom.xml. "+e1.getMessage());
            }
        }
        if (targetStreamF2!=null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(targetStreamF2));
                String str = "";
                while ((str = reader.readLine()) != null) splitS(str);
                targetStreamF2.close();
            } catch (Exception e) {
                mActivity.showToast("Could not parse genres from file: genres_rus.txt. "+
                        e.getClass().getSimpleName()+" "+e.getMessage());
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(targetStream2));
                    String str = "";
                    while ((str = reader.readLine()) != null) splitS(str);
                    targetStream2.close();
                } catch (Exception e1) {
                    mActivity.showToast("Could not parse genres from resource: genres_rus.txt. "+
                            e1.getClass().getSimpleName()+" "+e1.getMessage());
                }
            }
        } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(targetStream2));
                String str = "";
                while ((str = reader.readLine()) != null) splitS(str);
                targetStream2.close();
            } catch (Exception e1) {
                mActivity.showToast("Could not parse genres from resource: genres_rus.txt. "+
                        e1.getClass().getSimpleName()+" "+e1.getMessage());
            }
        }
    }
}
