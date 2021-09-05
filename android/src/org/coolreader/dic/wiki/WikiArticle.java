package org.coolreader.dic.wiki;

public class WikiArticle {
	public String title = "";
	public long pageId = 0;
	public String snippet = "";

	public WikiArticle(String title, long pageId, String snippet) {
		this.title = title;
		this.pageId = pageId;
		this.snippet = snippet;
	}
}
