package org.coolreader.crengine;

import android.annotation.TargetApi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import at.stefl.commons.lwxml.writer.LWXMLStreamWriter;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.odf.LocatedOpenDocumentFile;
import at.stefl.opendocument.java.odf.OpenDocument;
import at.stefl.opendocument.java.odf.OpenDocumentPresentation;
import at.stefl.opendocument.java.odf.OpenDocumentSpreadsheet;
import at.stefl.opendocument.java.odf.OpenDocumentText;
import at.stefl.opendocument.java.translator.document.DocumentTranslator;
import at.stefl.opendocument.java.translator.document.PresentationTranslator;
import at.stefl.opendocument.java.translator.document.SpreadsheetTranslator;
import at.stefl.opendocument.java.translator.document.TextTranslator;
import at.stefl.opendocument.java.translator.settings.ImageStoreMode;
import at.stefl.opendocument.java.translator.settings.TranslationSettings;
import at.stefl.opendocument.java.util.DefaultFileCache;
import at.stefl.opendocument.java.util.FileCache;

public class ConvertOdtFormat {

	@TargetApi(19)
	public static String convertOdtFile(String sFile, String sPath) throws
			java.io.IOException {

		File f = new File(sFile);
		File directory = new File(sPath);
		if(!directory.exists()){
			directory.mkdir();
		}
		String sNewName = sPath + f.getName()+".html";
		String IMAGE_DIR_NAME = sPath + f.getName()+"_images";
		String baseURL = f.getName()+"_images/";

		File folder = new File(IMAGE_DIR_NAME);
		if (!folder.exists())
			folder.mkdir();

		File htmlFile = new File(sNewName);
		LWXMLWriter out = new LWXMLStreamWriter(new OutputStreamWriter(
				new FileOutputStream(htmlFile), Charset.forName("UTF-8")));

		//FileCache cache = new DefaultFileCache(this.getExternalCacheDir()+"/tempOdt");
		FileCache cache = new DefaultFileCache(IMAGE_DIR_NAME);

		TranslationSettings settings = new TranslationSettings();
		settings.setCache(cache);
		settings.setImageStoreMode(ImageStoreMode.CACHE);
		settings.setBackTranslateable(true);
		LocatedOpenDocumentFile documentFile = null;
		File cachedFile = new File(sFile);
		documentFile = new LocatedOpenDocumentFile(cachedFile);
		OpenDocument document = documentFile.getAsDocument();
		DocumentTranslator translator;
		if (document instanceof OpenDocumentText) {
			translator = new TextTranslator();
		} else if (document instanceof OpenDocumentSpreadsheet) {
			translator = new SpreadsheetTranslator();
		} else if (document instanceof OpenDocumentPresentation) {
			translator = new PresentationTranslator();
		} else {
			throw new IllegalArgumentException();
		}
		long start = System.nanoTime();
		translator.translate(document, out, settings);
		//src="file:/storage/emulated/0/Books/converted/test_odt.odt_images/100002010000078000000438557A3697D0567655.png"/>
		long end = System.nanoTime();
		//System.out.println((end - start) / 1000000000d);
		//System.out.println(translator.isCurrentOutputTruncated());
		out.close();
		//replace links with relative path
		BufferedReader br = new BufferedReader(new FileReader(sNewName));
		BufferedWriter bw = new BufferedWriter(new FileWriter(sNewName+"_"));
		String line = br.readLine();
		String sName = StrUtils.stripExtension(f.getName());
		while (line != null) {
			line=line.replace("file:"+sPath,"");
			line=line.replace("<title>odf2html</title>","<title>"+sName+"</title>");
			bw.write(line);
			bw.write(System.lineSeparator());
			line = br.readLine();
		}
		bw.close();
		br.close();
		File file = new File(sNewName);
		if (file.exists()) file.delete();
		File file2 = new File(sNewName+"_");
		file2.renameTo(file);
		return sNewName;
	}
}
