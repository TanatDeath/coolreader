package org.coolreader.dic;

public class WikiArticle {
	String title = "";
	long pageId = 0;
	String snippet = "";

	public WikiArticle(String title, long pageId, String snippet) {
		this.title = title;
		this.pageId = pageId;
		this.snippet = snippet;
	}
}
