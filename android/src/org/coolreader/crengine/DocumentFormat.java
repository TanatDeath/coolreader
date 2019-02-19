package org.coolreader.crengine;

import android.content.Context;

import org.coolreader.R;

public enum DocumentFormat {
	/// lvtinydom.h: source document formats
	//typedef enum {
	NONE("fb2.css", R.raw.fb2, R.drawable.icons8_file, false, false, 0,
			new String[] {},
			new String[] {}),// doc_format_none,
	FB2("fb2.css", R.raw.fb2, R.drawable.icons8_fb2, true, true, 10,
			new String[] {".fb2", ".fb2.zip"},
			new String[] {"application/fb2+zip"}), // doc_format_fb2,
	TXT("txt.css", R.raw.txt, R.drawable.icons8_txt_2, false, false, 3,
			new String[] {".txt", ".tcr", ".pml"},
			new String[] {"text/plain"}), // doc_format_txt,
	RTF("rtf.css", R.raw.rtf, R.drawable.icons8_doc, false, false, 7,
			new String[] {".rtf"},
			new String[] {}), // doc_format_rtf,
	EPUB("epub.css", R.raw.epub, R.drawable.icons8_epub_1, true, true, 9,
			new String[] {".epub"},
			new String[] {"application/epub+zip"}),// doc_format_epub,
	HTML("htm.css", R.raw.htm, R.drawable.icons8_html_filetype_2, false, false, 8,
			new String[] {".htm", ".html", ".shtml", ".xhtml"},
			new String[] {"text/html"}),// doc_format_html,
	TXT_BOOKMARK("fb2.css", R.raw.fb2, R.drawable.icons8_fb2, false, false, 0,
			new String[] {".txt.bmk"},
			new String[] {}), // doc_format_txt_bookmark, // coolreader TXT format bookmark
	CHM("chm.css", R.raw.chm, R.drawable.icons8_html_filetype_2, false, false, 6,
			new String[] {".chm"},
			new String[] {}), //  doc_format_chm,
	DOC("doc.css", R.raw.doc, R.drawable.icons8_doc, false, false, 5,
			new String[] {".doc"},
			new String[] {}), // doc_format_doc,
	PDB("htm.css", R.raw.htm, R.drawable.icons8_mobi, false, true, 4,
			new String[] {".pdb", ".prc", ".mobi", ".azw"},
			new String[] {}),
	DOCX("htm.css", R.raw.htm, R.drawable.icons8_docx, false, false, 5,
			new String[] {".docx"},
			new String[] {}), // doc_format_docx,
	ODT("htm.css", R.raw.htm, R.drawable.icons8_odt, false, false, 6,
			new String[] {".odt",".odp",".ods"},
			new String[] {}), // doc_format_odt,
	; // doc_format_txt/html/...,
    // don't forget update getDocFormatName() when changing this enum
	//} doc_format_t;
	
	public String getCssName()
	{
		return cssFileName;
	}
	
	public int getPriority()
	{
		return priority;
	}
	
	public String[] getExtensions()
	{
		return extensions;
	}
	
	public int getCSSResourceId()
	{
		return cssResourceId;
	}
	
	public int getIconResourceId()
	{
		return iconResourceId;
	}

	public int getIconResourceIdThemed(Context ctx)
	{
		if (iconResourceId==R.drawable.icons8_file)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_file, R.drawable.icons8_file);
		if (iconResourceId==R.drawable.icons8_fb2)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_fb2, R.drawable.icons8_fb2);
		if (iconResourceId==R.drawable.icons8_txt_2)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_txt_2, R.drawable.icons8_txt_2);
		if (iconResourceId==R.drawable.icons8_doc)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_doc, R.drawable.icons8_doc);
		if (iconResourceId==R.drawable.icons8_epub_1)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_epub_1, R.drawable.icons8_epub_1);
		if (iconResourceId==R.drawable.icons8_html_filetype_2)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_html_filetype_2, R.drawable.icons8_html_filetype_2);
		if (iconResourceId==R.drawable.icons8_mobi)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_mobi, R.drawable.icons8_mobi);
		if (iconResourceId==R.drawable.icons8_docx)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_docx, R.drawable.icons8_docx);
		if (iconResourceId==R.drawable.icons8_odt)
			return Utils.resolveResourceIdByAttr(ctx, R.attr.attr_icons8_odt, R.drawable.icons8_odt);
		return iconResourceId;
	}
	
	public String[] getMimeFormats()
	{
		return mimeFormats;
	}
	
	public String getMimeFormat()
	{
		return mimeFormats.length>0 ? mimeFormats[0] : null;
	}
	
	public boolean canParseProperties()
	{
		return canParseProperties;
	}
	
	public boolean canParseCoverpages()
	{
		return canParseCoverpages;
	}
	
	public boolean needCoverPageCaching()
	{
		return this == FB2;
	}
	
	public static DocumentFormat byId( int i )
	{
		if ( i>=0 && i<DocumentFormat.values().length )
			return values()[i];
		return null;
	}
	
	public boolean matchExtension( String filename )
	{
		for ( String ext : extensions )
			if ( filename.endsWith(ext) )
				return true;
		return false;
	}
	
	public boolean matchMimeType( String type )
	{
		for ( String s : mimeFormats ) {
			if ( type.equals(s) || type.startsWith(s+";"))
				return true;
		}
		return false;
	}
	
	public static DocumentFormat byExtension( String filename )
	{
		String s = filename.toLowerCase();
		for ( int i=0; i<DocumentFormat.values().length; i++ )
			if ( values()[i].matchExtension(s))
				return values()[i];
		return null;
	}
	
	public static DocumentFormat byMimeType( String format )
	{
		if ( format==null )
			return null;
		String s = format.toLowerCase();
		for ( int i=0; i<DocumentFormat.values().length; i++ )
			if ( values()[i].matchMimeType(s))
				return values()[i];
		return null;
	}

	public static String extByMimeType( String stype )
	{
		String sExt = "";
		if (stype.equals("text/plain")) sExt = "txt";
		if (stype.equals("text/plain")) sExt = "txt";
		if (stype.equals("application/x-fictionbook+xml")) sExt = "fb2";
		if (stype.equals("application/x-fictionbook")) sExt = "fb2";
		if (stype.equals("application/x-fb2")) sExt = "fb2";
		if (stype.equals("application/fb2")) sExt = "fb2";
		if (stype.equals("application/fb2.zip")) sExt = "fb2.zip";
		if (stype.equals("application/zip")) sExt = "zip";
		if (stype.equals("text/fb2+xml")) sExt = "fb2";
		if (stype.equals("application/epub+zip")) sExt = "epub.zip";
		if (stype.equals("application/epub")) sExt = "epub";
		if (stype.equals("application/xhtml+xml")) sExt = "html";
		if (stype.equals("application/vnd.ms-htmlhelp")) sExt = "chm";
		if (stype.equals("application/x-chm")) sExt = "chm";
		if (stype.equals("application/x-cdisplay")) sExt = "cbz";
		if (stype.equals("application/x-cbz")) sExt = "cbz";
		if (stype.equals("application/x-cbr")) sExt = "cbr";
		if (stype.equals("application/x-cbt")) sExt = "cbt";
		if (stype.equals("application/rtf")) sExt = "rtf";
		if (stype.equals("application/rtf+zip")) sExt = "rtf.zip";
		if (stype.equals("application/x-rtf")) sExt = "rtf";
		if (stype.equals("text/richtext")) sExt = "rtf";
		if (stype.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) sExt = "docx";
		if (stype.equals("application/msword")) sExt = "doc";
		if (stype.equals("application/doc")) sExt = "doc";
		if (stype.equals("application/vnd.msword")) sExt = "doc";
		if (stype.equals("application/vnd.ms-word")) sExt = "doc";
		if (stype.equals("application/winword")) sExt = "doc";
		if (stype.equals("application/word")) sExt = "doc";
		if (stype.equals("application/x-msw6")) sExt = "doc";
		if (stype.equals("application/x-msword")) sExt = "doc";
		if (stype.equals("application/x-mobipocket-ebook")) sExt = "mobi";
		if (stype.equals("application/x-pdb-plucker-ebook")) sExt = "mobi";
		if (stype.equals("application/x-pdb-textread-ebook")) sExt = "mobi";
		if (stype.equals("application/x-pdb-peanutpress-ebook")) sExt = "mobi";
		if (stype.equals("application/x-pdb-ztxt-ebook")) sExt = "mobi";
		if (stype.equals("application/vnd.palm")) sExt = "mobi";
		if (stype.equals("application/x-pdb-plucker-ebook")) sExt = "mobi";
		if (stype.equals("image/jpeg")) sExt = "jpg";
		if (stype.equals("image/png")) sExt = "png";
		if (stype.equals("image/*")) sExt = "png";
		if (stype.equals("application/x-pilot-prc")) sExt = "azw";
		return sExt;
	}
	
	private DocumentFormat( String cssFileName, int cssResourceId, int iconResourceId, boolean canParseProperties, boolean canParseCoverpages, int priority, String extensions[], String mimeFormats[] )
	{
		this.cssFileName = cssFileName;
		this.cssResourceId = cssResourceId;
		this.iconResourceId = iconResourceId;
		this.extensions = extensions;
		this.canParseProperties = canParseProperties;
		this.canParseCoverpages = canParseCoverpages;
		this.mimeFormats = mimeFormats;
		this.priority = priority;
	}
	final private String cssFileName;
	final private int cssResourceId;
	final private int iconResourceId;
	final private String[] extensions;
	final boolean canParseProperties;
	final boolean canParseCoverpages;
	final private String[] mimeFormats;
	final private int priority;
}
