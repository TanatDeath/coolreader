package org.coolreader.crengine;

import java.io.IOException;
import java.io.InputStream;
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
        InputStream targetStream = mActivity.getResources().openRawResource(R.raw.fb2genres_utf8_wo_bom);
        saxParser.parse(targetStream,handler);
    }
}
