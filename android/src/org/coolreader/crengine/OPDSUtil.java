package org.coolreader.crengine;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.Engine.DelayedProgress;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@SuppressLint("SimpleDateFormat")
public class OPDSUtil {

	public static final boolean EXTENDED_LOG = false; // set to false for production
    public static final int CONNECT_TIMEOUT = 60000;
    public static final int READ_TIMEOUT = 240000; // for very slow OPDS'ses
	/*
<?xml version="1.0" encoding="utf-8"?>
<feed xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" xmlns:relevance="http://a9.com/-/opensearch/extensions/relevance/1.0/" 
xmlns="http://www.w3.org/2005/Atom" 
xml:base="http://lib.ololo.cc/opds/">
<id>http://lib.ololo.cc/opds/</id>
<updated>2011-05-31T10:28:22+04:00</updated>
<title>OPDS: lib.ololo.cc</title>
<subtitle>Librusec mirror.</subtitle>
<author>
  <name>ololo team</name>
  <uri>http://lib.ololo.cc</uri><email>libololo@gmail.com</email>
</author>
<icon>http://lib.ololo.cc/book.png</icon>
<link rel="self" title="This Page" type="application/atom+xml" href="/opds/"/>
<link rel="alternate" type="text/html" title="HTML Page" href="/"/>
<entry>
   <updated>2011-05-31T10:28:22+04:00</updated>
   <id>http://lib.ololo.cc/opds/asearch/</id>
   <title>sample type</title>
   <content type="text">sample content</content>
   <link type="application/atom+xml" href="http://lib.ololo.cc/opds/asearch/"/>
</entry>
</feed>
	 */
	/**
	 * Callback interface for OPDS.
	 */
	public interface DownloadCallback {
		/**
		 * Some entries are downloaded.
		 * @param doc is document
		 * @param entries is list of entries to add
		 */
		public boolean onEntries(DocInfo doc, Collection<EntryInfo> entries);
		/**
		 * All entries are downloaded.
		 * @param doc is document
		 * @param entries is list of entries to add
		 */
		public boolean onFinish(DocInfo doc, Collection<EntryInfo> entries);
		/**
		 * Before download: request filename to save as.
		 */
		public File onDownloadStart(String type, String initial_url, String url);
		/**
		 * Download progress
		 */
		public void onDownloadProgress(String type, String url, int percent);
		/**
		 * Book is downloaded.
		 */
		public void onDownloadEnd(String type, String initial_url, String url, File file);
		/**
		 * Error occured
		 */
		public void onError(String message);
	}
	
	public static class DocInfo {
		public String id;
		public long updated;
		public String title;
		public String subtitle;
		public String icon;
		public String language;
		public LinkInfo selfLink;
		public LinkInfo alternateLink;
		public LinkInfo nextLink;
		public LinkInfo searchLink;
	}
	
	public static String dirPath(String filePath) {
		int pos = filePath.lastIndexOf("/");
		if (pos < 0)
			return filePath;
		return filePath.substring(0, pos+1);
	}
	
	public static class LinkInfo {
		public String href;
		public String rel;
		public String title;
		public String type;
		public LinkInfo(URL baseURL, Attributes attributes) {
			rel = attributes.getValue("rel");
			type = attributes.getValue("type");
			title = attributes.getValue("title");
			href = convertHref(baseURL, attributes.getValue("href"));
		}
		public static String convertHref(URL baseURL, String href) {
			if (href == null)
				return href;
			String port = "";
			if (baseURL.getPort() != 80 && baseURL.getPort() > 0)
				port = ":" + baseURL.getPort();
			String hostPort = baseURL.getHost() + port;
			if (href.startsWith("//"))
				return baseURL.getProtocol() + ":" + href;
			if (href.startsWith("/"))
				return baseURL.getProtocol() + "://" + hostPort + href;
			if (!href.startsWith("http://") && !href.startsWith("https://")) {
				return baseURL.getProtocol() + "://" + hostPort + dirPath(baseURL.getPath()) + "/" + href;
			}
			return href;
		}
		public boolean isValid() {
			return href!=null && href.length()!=0;
		}
		public int getPriority() {
			if (type == null)
				return 0;
			DocumentFormat df = DocumentFormat.byMimeType(type);
			if (rel != null && rel.indexOf("acquisition") < 0 && df != DocumentFormat.FB2 && df != DocumentFormat.EPUB
					&& df != DocumentFormat.RTF && df != DocumentFormat.DOC)
				return 0;
			return df != null ? df.getPriority() : 0;
		}
		@Override
		public String toString() {
			return "[ rel=" + rel + ", type=" + type
					+ ", title=" + title + ", href=" + href + "]";
		}
		
	}
	
	public static class AuthorInfo {
		public String name;
		public String uri;
	}
	
	public static class EntryInfo {
		public String id;
		public long updated;
		public String title="";
		public String content="";
		public String summary="";
		public LinkInfo link;
		public ArrayList<LinkInfo> links = new ArrayList<LinkInfo>();
		public String icon;
		public ArrayList<String> categories = new ArrayList<String>(); 
		public ArrayList<AuthorInfo> authors = new ArrayList<AuthorInfo>();
		public HashMap<String, String> otherElements = new HashMap<String, String>();
		public LinkInfo getBestAcquisitionLink() {
			LinkInfo best = null;
			int bestPriority = 0; 
			for (LinkInfo link : links) {
				//boolean isAcquisition = link.rel!=null && link.rel.indexOf("acquisition")>=0;
				int priority = link.getPriority();
				if (priority > 0 && priority > bestPriority) {
					if (link.getPriority() > 0 && (best == null || best.getPriority() < link.getPriority())) {
						best = link;
						bestPriority = priority;
					}
				}
			}
			return best;
		}
		public String getAuthors() {
			if (authors.size() == 0)
				return null;
			StringBuilder buf = new StringBuilder(100);
			for (AuthorInfo a : authors) {
				if (buf.length() > 0)
					buf.append(", ");
				buf.append(a.name);
			}
			// plotn - dedup
			String bufS = buf.toString();
			if (bufS!=null) {
				String[] list = bufS.split(",");
				ArrayList<String> arrS = new ArrayList<String>();
				for (String s : list) {
					s = s.replaceAll("\\s+", " ").trim();
					if (!arrS.contains(s)) arrS.add(s);
				}
				String resS = "";
				for (String s : arrS) {
					resS = resS + ", " + s;
				}
				if (resS.length() > 0) resS = resS.substring(1);
				return resS;
			}

			return bufS;
		}
	}
	
	public static class OPDSHandler extends DefaultHandler {
		private URL url;
		private DocInfo docInfo = new DocInfo(); 
		private EntryInfo entryInfo = new EntryInfo(); 
		private ArrayList<EntryInfo> entries = new ArrayList<EntryInfo>(); 
		private Stack<String> elements = new Stack<String>();
		//private Attributes currentAttributes;
		private AuthorInfo authorInfo;
		private boolean insideFeed;
		private boolean insideEntry;
		private boolean insideEntryTitle;
		//private boolean singleEntry;
		private int level = 0;
		//2011-05-31T10:28:22+04:00
		private static SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); 
		private static SimpleDateFormat tsFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		public OPDSHandler(URL url) {
			this.url = url;
		}
		public void setUrl(URL url) {
			this.url = url;
		}
		private long parseTimestamp(String ts) {
			if (ts == null)
				return 0;
			ts = ts.trim();
			try {
				if (ts.length() == "2010-01-10T10:01:10Z".length())
					return tsFormat2.parse(ts).getTime();
				if (ts.length() == "2011-11-11T11:11:11+67:87".length() && ts.lastIndexOf(":") == ts.length()-3) {
					ts = ts.substring(0, ts.length()-3) + ts.substring(0, ts.length()-2);
					return tsFormat.parse(ts).getTime();
				}
				if (ts.length() == "2011-11-11T11:11:11+6787".length()) {
					return tsFormat.parse(ts).getTime();
				}
			} catch (ParseException e) {
			}
			L.e("cannot parse timestamp " + ts);
			return 0;
		}

		boolean isEntryEq(String entry, String cmp) {
			if (StrUtils.getNonEmptyStr(entry,true).equals(StrUtils.getNonEmptyStr(cmp,true)))
					return true;
			// for tags e.g. calibre:language
			if (StrUtils.getNonEmptyStr(entry,true).endsWith(":"+StrUtils.getNonEmptyStr(cmp,true)))
				return true;
			return false;
		}
		
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			super.characters(ch, start, length);
			
			String s = new String(ch, start, length);
			s = s.trim();
			if (s.length()==0 || (s.length()==1 && s.charAt(0) == '\n') )
				return; // ignore empty line
			//L.d(tab() + "  {" + s + "}");
			String currentElement = elements.peek();
			if (currentElement == null)
				return;
			if (insideFeed) {
				if (isEntryEq(currentElement, "id")) {
					if (insideEntry)
						entryInfo.id = s;
					else
						docInfo.id = s;
				} else if (isEntryEq(currentElement, "updated")) {
					long ts = parseTimestamp(s);
					if (insideEntry)
						entryInfo.updated = ts;
					else
						docInfo.updated = ts;
				} else if (isEntryEq(currentElement, "title")) {
					if (!insideEntry) {
						docInfo.title = s;
					} else {
						entryInfo.title = entryInfo.title + s;
					}
				} else if (isEntryEq(currentElement, "summary")) {
					if (insideEntry)
						entryInfo.summary = entryInfo.summary + s;
				} else if (isEntryEq(currentElement, "name")) {
					if (authorInfo != null)
						authorInfo.name = s;
				} else if (isEntryEq(currentElement, "uri")) {
					if (authorInfo!=null)
						authorInfo.uri = s;
				} else if (isEntryEq(currentElement, "icon")) {
					if (!insideEntry)
						docInfo.icon = s;
					else
						entryInfo.icon = s;
				} else if (isEntryEq(currentElement, "link")) {
					// rel, type, title, href
//					if (!insideEntry )
//						docInfo.icon = s;
//					else
//						entryInfo.icon = s;
				} else if (isEntryEq(currentElement, "content")) {
					if (insideEntry)
						entryInfo.content = entryInfo.content + s;
				} else if (isEntryEq(currentElement, "subtitle") ) {
					if (!insideEntry)
						docInfo.subtitle = s;
				} else if (isEntryEq(currentElement, "language")) {
					if (!insideEntry) {
						docInfo.language = s;
						entryInfo.otherElements.put(currentElement, s);
					}
				} else if (insideEntryTitle) {
					if (entryInfo.title.length() > 0)
						entryInfo.title = entryInfo.title + " ";
					entryInfo.title = entryInfo.title + s;
				} else if (insideEntry) {
					entryInfo.otherElements.put(currentElement, s);
					//asdf //plotn продолжить обработку других элементов
				}
			}
		}

		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
			if (EXTENDED_LOG) L.d("endDocument: " + entries.size() + " entries parsed");
			if (EXTENDED_LOG) 
				for (EntryInfo entry : entries) {
					L.d("   " + entry.title + " : " + entry.link.toString());
				}
		}

		private String tab() {
			if (level <= 1)
				return "";
			StringBuffer buf = new StringBuffer(level*2);
			for (int i=1; i<level; i++)
				buf.append("  ");
			return buf.toString();
		}
		
		@Override
		public void startElement(String uri, String localName,
				String qName, Attributes attributes)
				throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			if (qName!=null && qName.length()>0)
				localName = qName;
			level++;
			//L.d(tab() + "<" + localName + ">");
			//currentAttributes = attributes;
			elements.push(localName);
			//String currentElement = elements.peek();
			if (!insideFeed && "feed".equals(localName)) {
				insideFeed = true;
			} else if ("entry".equals(localName)) {
				if (!insideFeed) {
					insideFeed = true;
					//singleEntry = true;
				}
				insideEntry = true;
				entryInfo = new EntryInfo();
			} else if ("category".equals(localName)) {
				if (insideEntry) {
					String category = attributes.getValue("label");
					if ( category!=null )
						entryInfo.categories.add(category);
				}
			} else if ("id".equals(localName)) {
				
			} else if ("updated".equals(localName)) {
				
			} else if ("title".equals(localName)) {
				insideEntryTitle = insideEntry;
			} else if ("name".equals(localName)) {

			} else if ("link".equals(localName)) {
				LinkInfo link = new LinkInfo(url, attributes);
				if (link.isValid() && insideFeed) {
					if (EXTENDED_LOG) L.d(tab()+ link);
					if (insideEntry) {
						if (link.type!=null) {
							entryInfo.links.add(link);
							int priority = link.getPriority();
							DocumentFormat df = DocumentFormat.byMimeType(link.type);
							if ((df != null) || (link.type.startsWith("application/atom+xml"))) {
							//if ( link.type.startsWith("application/atom+xml") ) {
								//if (entryInfo.link == null || !entryInfo.link.type.startsWith("application/atom+xml"))
								DocumentFormat df2 = DocumentFormat.byMimeType(link.type);
								if (entryInfo.link == null || (df2 != null) || !entryInfo.link.type.startsWith("application/atom+xml"))
									entryInfo.link = link;
							} else if ( "http://opds-spec.org/cover".equals(link.rel)
									&& StrUtils.getNonEmptyStr(link.type,true).startsWith("image/")) {
								entryInfo.icon = link.href;
							} else if ( "http://opds-spec.org/thumbnail".equals(link.rel)
									&& StrUtils.getNonEmptyStr(link.type,true).startsWith("image/")) {
								if (entryInfo.icon == null)
									entryInfo.icon = link.href;
							} else if (priority>0 && (entryInfo.link==null || entryInfo.link.getPriority()<priority)) {
								entryInfo.link = link;
							}
						}
					} else {
						if ("self".equals(link.rel))
							docInfo.selfLink = link;
						else if ("alternate".equals(link.rel))
							docInfo.alternateLink = link;
						else if ("next".equals(link.rel))
							docInfo.nextLink = link;
						else if ("search".equals(link.rel))
							docInfo.searchLink = link;
					}
				}
			} else if ("author".equals(localName)) {
				authorInfo = new AuthorInfo();
			}
		}
		
		@Override
		public void endElement(String uri, String localName,
				String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (qName!=null && qName.length()>0)
				localName = qName;
			//L.d(tab() + "</" + localName + ">");
			//String currentElement = elements.peek();
			if (insideFeed && "feed".equals(localName)) {
				insideFeed = false;
			} else if ("title".equals(localName)) {
				insideEntryTitle = false;
			} else if ("entry".equals(localName)) {
				if (!insideFeed || !insideEntry)
					throw new SAXException("unexpected element " + localName);
				if (entryInfo.link != null || entryInfo.getBestAcquisitionLink() != null) {
					entries.add(entryInfo);
				}
				insideEntry = false;
				entryInfo = null;
			} else if ("author".equals(localName)) {
				if (insideEntry) {
					if (authorInfo != null && authorInfo.name != null)
						entryInfo.authors.add(authorInfo);
				}
				authorInfo = null;
			} 
			//currentAttributes = null;
			if ( level>0 )
				level--;
		}

		@Override
		public void startDocument() throws SAXException {
			// TODO Auto-generated method stub
			super.startDocument();
		}

	}
	
	public static class DownloadTask {
		final private CoolReader coolReader;
		private String catalogURL;
		private URL initial_url;
		private URL url;
		private String username;
		private String password;
		private String proxy_addr;
		private String proxy_port;
		private String proxy_uname;
		private String proxy_passw;
		private int onion_def_proxy;
		final private String expectedType;
		final private String referer;
		final private String defaultFileName;
		final private DownloadCallback callback;
		private String progressMessage = "Dowloading...";
		private HttpURLConnection connection;
		private DelayedProgress delayedProgress;
		OPDSHandler handler;
		public DownloadTask(String catalogURL,
							CoolReader coolReader, URL url, String defaultFileName, String expectedType, String referer,
							DownloadCallback callback, String username, String password,
							String proxy_addr, String proxy_port,
							String proxy_uname, String proxy_passw,
							int onion_def_proxy
							) {
			this.catalogURL = catalogURL;
			this.url = url;
			this.initial_url = url;
			this.coolReader = coolReader;
			this.callback = callback; 
			this.referer = referer;
			this.expectedType = expectedType;
			this.defaultFileName = defaultFileName;
			this.username = username;
			this.password = password;
			this.proxy_addr = proxy_addr;
			this.proxy_port = proxy_port;
			this.proxy_uname = proxy_uname;
			this.proxy_passw = proxy_passw;
			this.onion_def_proxy = onion_def_proxy;
			L.d("Created DownloadTask for " + url);
		}
		private void setProgressMessage( String url, int totalSize ) {
			progressMessage = coolReader.getString(org.coolreader.R.string.progress_downloading) + " " + url;
			if ( totalSize>0 )
				progressMessage = progressMessage + " (" + totalSize + ")";
		}
		// call in GUI thread only!
		private void hideProgress() {
			if ( progressShown && Services.getEngine() != null)
				Services.getEngine().hideProgress();
			if ( delayedProgress != null ) {
				delayedProgress.cancel();
				delayedProgress.hide();
			}
		}
		private void onError(final String msg) {
			BackgroundThread.instance().executeGUI(() -> {
				hideProgress();
				callback.onError(msg);
			});
		}
		private void parseFeed(InputStream is) throws Exception {
			try {
				if (handler==null)
					handler = new OPDSHandler(url);
				else
					handler.setUrl(url); // download next part
				String[] namespaces = new String[] { 
                        "access", "http://www.bloglines.com/about/specs/fac-1.0",
                        "admin", "http://webns.net/mvcb/",
                        "ag", "http://purl.org/rss/1.0/modules/aggregation/",
                        "annotate", "http://purl.org/rss/1.0/modules/annotate/",
                        "app", "http://www.w3.org/2007/app",
                        "atom", "http://www.w3.org/2005/Atom",
                        "audio", "http://media.tangent.org/rss/1.0/",
                        "blogChannel", "http://backend.userland.com/blogChannelModule",
                        "cc", "http://web.resource.org/cc/",
                        "cf", "http://www.microsoft.com/schemas/rss/core/2005",
                        "company", "http://purl.org/rss/1.0/modules/company",
                        "content", "http://purl.org/rss/1.0/modules/content/",
                        "conversationsNetwork", "http://conversationsnetwork.org/rssNamespace-1.0/",
                        "cp", "http://my.theinfo.org/changed/1.0/rss/",
                        "creativeCommons", "http://backend.userland.com/creativeCommonsRssModule",
                        "dc", "http://purl.org/dc/elements/1.1/",
                        "dcterms", "http://purl.org/dc/terms/",
                        "email", "http://purl.org/rss/1.0/modules/email/",
                        "ev", "http://purl.org/rss/1.0/modules/event/",
                        "feedburner", "http://rssnamespace.org/feedburner/ext/1.0",
                        "fh", "http://purl.org/syndication/history/1.0",
                        "foaf", "http://xmlns.com/foaf/0.1/",
                        "foaf", "http://xmlns.com/foaf/0.1",
                        "geo", "http://www.w3.org/2003/01/geo/wgs84_pos#",
                        "georss", "http://www.georss.org/georss",
                        "geourl", "http://geourl.org/rss/module/",
                        "g", "http://base.google.com/ns/1.0",
                        "gml", "http://www.opengis.net/gml",
                        "icbm", "http://postneo.com/icbm",
                        "image", "http://purl.org/rss/1.0/modules/image/",
                        "indexing", "urn:atom-extension:indexing",
                        "itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd",
                        "kml20", "http://earth.google.com/kml/2.0",
                        "kml21", "http://earth.google.com/kml/2.1",
                        "kml22", "http://www.opengis.net/kml/2.2",
                        "l", "http://purl.org/rss/1.0/modules/link/",
                        "mathml", "http://www.w3.org/1998/Math/MathML",
                        "media", "http://search.yahoo.com/mrss/",
                        "openid", "http://openid.net/xmlns/1.0",
                        "opensearch10", "http://a9.com/-/spec/opensearchrss/1.0/",
                        "opensearch", "http://a9.com/-/spec/opensearch/1.1/",
                        "opml", "http://www.opml.org/spec2",
                        "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                        "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
                        "ref", "http://purl.org/rss/1.0/modules/reference/",
                        "reqv", "http://purl.org/rss/1.0/modules/richequiv/",
                        "rss090", "http://my.netscape.com/rdf/simple/0.9/",
                        "rss091", "http://purl.org/rss/1.0/modules/rss091#",
                        "rss1", "http://purl.org/rss/1.0/",
                        "rss11", "http://purl.org/net/rss1.1#",
                        "search", "http://purl.org/rss/1.0/modules/search/",
                        "slash", "http://purl.org/rss/1.0/modules/slash/",
                        "ss", "http://purl.org/rss/1.0/modules/servicestatus/",
                        "str", "http://hacks.benhammersley.com/rss/streaming/",
                        "sub", "http://purl.org/rss/1.0/modules/subscription/",
                        "svg", "http://www.w3.org/2000/svg",
                        "sx", "http://feedsync.org/2007/feedsync",
                        "sy", "http://purl.org/rss/1.0/modules/syndication/",
                        "taxo", "http://purl.org/rss/1.0/modules/taxonomy/",
                        "thr", "http://purl.org/rss/1.0/modules/threading/",
                        "thr", "http://purl.org/syndication/thread/1.0",
                        "trackback", "http://madskills.com/public/xml/rss/module/trackback/",
                        "wfw", "http://wellformedweb.org/CommentAPI/",
                        "wiki", "http://purl.org/rss/1.0/modules/wiki/",
                        "xhtml", "http://www.w3.org/1999/xhtml",
                        "xlink", "http://www.w3.org/1999/xlink",
                        "xrd", "xri://$xrd*($v*2.0)",
                        "xrds", "xri://$xrds"
				};
				for ( int i=0; i<namespaces.length-1; i+=2 )
					handler.startPrefixMapping(namespaces[i], namespaces[i+1]);
				SAXParserFactory spf = SAXParserFactory.newInstance();
				spf.setValidating(false);
//				spf.setNamespaceAware(true);
//				spf.setFeature("http://xml.org/sax/features/namespaces", false);
				SAXParser sp = spf.newSAXParser();
				//XMLReader xr = sp.getXMLReader();				
				sp.parse(is, handler);
			} catch (SAXException se) {
				L.e("sax error", se);
				throw se;
			} catch (IOException ioe) {
				L.e("sax parse io error", ioe);
				throw ioe;
			}
		}
		
		private File generateFileName(File outDir, String fileName, String type, boolean isZip) {
			DocumentFormat fmt = type!=null ? DocumentFormat.byMimeType(type) : null;
			//DocumentFormat fmtext = fileName!=null ? DocumentFormat.byExtension(fileName) : null;
			if ( fileName==null )
				fileName = "noname";
			String ext = null;
			if ( fileName.lastIndexOf(".")>0 ) {
				ext = fileName.substring(fileName.lastIndexOf(".")+1);
				fileName = fileName.substring(0, fileName.lastIndexOf("."));
			}
			fileName = Utils.transcribeFileName( fileName );
			if ( fmt!=null ) {
				if ( fmt==DocumentFormat.FB2 && isZip )
					ext = ".fb2.zip";
				else
					ext = fmt.getExtensions()[0].substring(1);
			}
			for (int i=0; i<1000; i++ ) {
				String fn = fileName + (i==0 ? "" : "(" + i + ")") + "." + ext; 
				File f = new File(outDir, fn);
				if ( !f.exists() && !f.isDirectory() )
					return f;
			}
			return null;
		}
		private void downloadBook(final String type, final String initial_url, final String url, InputStream is, int contentLength,
								  final String fileName, final String defFileName, final boolean isZip) throws Exception {
			L.d("Download requested: " + type + " " + url + " " + contentLength);
			DocumentFormat fmt = DocumentFormat.byMimeType(type);
			if ( fmt==null ) {
				L.d("Download: unknown type " + type);
				throw new Exception("Unknown file type " + type);
			}
			final File outDir = BackgroundThread.instance().callGUI(() -> callback.onDownloadStart(type, initial_url, url));
			if ( outDir==null ) {
				L.d("Cannot find writable location for downloaded file " + url);
				throw new Exception("Cannot save file " + url);
			}
			boolean needRetry = false;
			boolean fileNotCreated = false;
			// try with file name
			File outFile = generateFileName(outDir, fileName, type, isZip);
			try {
				needRetry = outFile == null;
				if (!needRetry) {
					L.d("Creating file: " + outFile.getAbsolutePath());
					fileNotCreated = (outFile.exists() || !outFile.createNewFile());
				} else
					L.d("Cannot generate file name - we'll try def name");
			} catch (IOException e) {
				String errMes = StrUtils.getNonEmptyStr(e.getMessage().toString(), true);
				needRetry = errMes.contains("too long");
				if (!needRetry) {
					if (fileNotCreated) {
						L.d("Cannot create file " + outFile.getAbsolutePath());
						throw new Exception(coolReader.getString(R.string.cannot_create_file) +
								" " + outFile.getAbsolutePath());
					}
				}
			}
			// try with def file name
			if (needRetry) {
				outFile = generateFileName(outDir, defFileName, type, isZip);
				if (outFile == null) {
					L.d("Cannot generate file name "+defFileName);
					throw new Exception(coolReader.getString(R.string.cannot_generate_file_name) +
							" " + defFileName);
				}
				L.d("Creating file: " + outFile.getAbsolutePath());
				fileNotCreated = (outFile.exists() || !outFile.createNewFile());
				if (fileNotCreated) {
					L.d("Cannot create file " + outFile.getAbsolutePath());
					throw new Exception(coolReader.getString(R.string.cannot_create_file) +
							" " + outFile.getAbsolutePath());
				}
			}
			L.d("Download started: " + outFile.getAbsolutePath());
//			long lastTs = System.currentTimeMillis(); 
//			int lastPercent = -1;
			boolean success = false;
			try (FileOutputStream os = new FileOutputStream(outFile)) {
				byte[] buf = new byte[16384];
				int totalWritten = 0;
				while (totalWritten<contentLength || contentLength==-1) {
					int bytesRead = is.read(buf);
					if ( bytesRead<=0 )
						break;
					os.write(buf, 0, bytesRead);
					totalWritten += bytesRead;
//					final int percent = totalWritten * 100 / contentLength;
//					long ts = System.currentTimeMillis(); 
//					if ( percent!=lastPercent && ts - lastTs > 1500 ) {
//						L.d("Download progress: " + percent + "%");
//						BackgroundThread.instance().postGUI(new Runnable() {
//							@Override
//							public void run() {
//								callback.onDownloadProgress(type, url, percent);
//							}
//						});
//					}
				}
				success = true;
			} finally {
				if ( !success ) {
					if ( outFile.exists() && outFile.isFile() ) {
						L.w("deleting unsuccessully downloaded file " + outFile);
						outFile.delete();
					}
				}
			}
			L.d("Download finished");
			final File outFileF = outFile;
			BackgroundThread.instance().executeGUI(() -> callback.onDownloadEnd(type, initial_url, url, outFileF));
		}
		public static int findSubstring( byte[]buf, String str ) {
			for ( int i=0; i<buf.length-str.length(); i++ ) {
				boolean found = true;
				for ( int j=0; j<str.length(); j++ )
					if ( str.charAt(j)!=buf[i+j] ) {
						found = false;
						break;
					}
				if ( found )
					return i;
			}
			return -1; // not found
		}
		
		public static String encodePassword(String username, String password) {
			return Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
		}

		public void runInternal() {
			connection = null;
			
			boolean itemsLoadedPartially = false;
			boolean loadNext = false;
			HashSet<String> visited = new HashSet<String>();

			do {
				try {
					setProgressMessage( url.toString(), -1 );
					visited.add(url.toString());
					long startTimeStamp = System.currentTimeMillis();
					if (!partialDownloadCompleted) {
						if (delayedProgress != null)
							delayedProgress.cancel();
						delayedProgress = Services.getEngine().showProgressDelayed(0, progressMessage, "", PROGRESS_DELAY_MILLIS);
					}
					URL newURL = url;
					boolean useOrobotProxy = false;
					String host = url.getHost();
					if (host.endsWith(".onion"))
					    useOrobotProxy = true;
					String oldAddress = url.toString();
					if (oldAddress.startsWith("orobot://")) {
					    newURL = new URL("http://" + oldAddress.substring(9)); // skip orobot://
					    useOrobotProxy = true;
					    L.d("Converting url - " + oldAddress + " to " + newURL + " for using ORobot proxy");
					} else if (oldAddress.startsWith("orobots://")) {
					    newURL = new URL("https://" + oldAddress.substring(10)); // skip orobots://
					    useOrobotProxy = true;
					    L.d("Converting url - " + oldAddress + " to " + newURL + " for using ORobot proxy");
					}
					Proxy proxy = null;
                    System.setProperty("http.keepAlive", "false");					
					if ((useOrobotProxy)&&(onion_def_proxy==1)) {
                        // Set-up proxy
                        //System.setProperty("http.proxyHost", "127.0.0.1");
                        //System.setProperty("http.proxyPort", "8118");
					    //L.d("Using ORobot proxy: " + proxy);
					    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8118)); // ORobot proxy running on this device
					    L.d("Using ORobot proxy: " + proxy);
					} else {
                        //System.clearProperty("http.proxyHost");
                        //System.clearProperty("http.proxyPort");
					}
					if ((!StrUtils.isEmptyStr(proxy_addr))&&(!StrUtils.isEmptyStr(proxy_port))) {
						int port = 0;
						try {
							port = Integer.valueOf(proxy_port);
							proxy = new Proxy(Proxy.Type.HTTP,
									new InetSocketAddress(proxy_addr, port)); // ORobot proxy running on this device
							L.d("Using proxy: " + proxy);
						} catch (Exception e) {
							L.e("Wrong proxy port value: " + proxy_port);
						}
					}
				    URLConnection conn = proxy == null ? newURL.openConnection() : newURL.openConnection(proxy);
					if ((!StrUtils.isEmptyStr(proxy_addr))&&(!StrUtils.isEmptyStr(proxy_port))&&
							(!StrUtils.isEmptyStr(proxy_uname))&&(!StrUtils.isEmptyStr(proxy_passw))) {
						conn.setRequestProperty("Proxy-Authorization",
								"Basic " + Base64.encodeToString((proxy_uname + ":" + proxy_passw).getBytes(), Base64.NO_WRAP));
					}
					if ( conn instanceof HttpsURLConnection ) {
						HttpsURLConnection https = (HttpsURLConnection)conn;

	                    // Create a trust manager that does not validate certificate chains
	                    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
	                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                            return null;
	                        }
	                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
	                        }
	                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
	                        }
	                    } };
	                    // Install the all-trusting trust manager
	                    final SSLContext sc = SSLContext.getInstance("SSL");
	                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
	                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	                    
	                	https.setHostnameVerifier((arg0, arg1) -> true);
					}
					if ( !(conn instanceof HttpURLConnection) ) {
						onError("Only HTTP supported");
						return;
					}
					connection = (HttpURLConnection)conn;
		            connection.setRequestProperty("User-Agent", "KnownReader (Android)");
		            if ( referer!=null )
		            	connection.setRequestProperty("Referer", referer);
		            connection.setInstanceFollowRedirects(true);
	                connection.setUseCaches(false);
					if (username != null && username.length() > 0 && password != null && password.length() > 0) {
	                	//connection.setRequestProperty("Authorization", encodePassword(username, password));
						connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
						Authenticator.setDefault(new Authenticator() {
	                	    protected PasswordAuthentication getPasswordAuthentication() {
	                	        return new PasswordAuthentication(username, password.toCharArray());
	                	    }});
					}
					connection.setAllowUserInteraction(false);
		            connection.setConnectTimeout(CONNECT_TIMEOUT);
		            connection.setReadTimeout(READ_TIMEOUT);
		            connection.setDoInput(true);
					String fileName = null;
		            String disp = connection.getHeaderField("Content-Disposition");
		            if (disp != null) {
		            	int p = disp.toLowerCase().indexOf("filename=");
						if (p > 0) {
		            		fileName = disp.substring(p + 9);
		            		if (fileName.startsWith("\"")) {
								fileName = fileName.substring(1);
								if (fileName.contains("\""))
									fileName = fileName.substring(0, fileName.indexOf("\""));
							}
		            	}
		            }
		            //connection.setDoOutput(true);
		            //connection.set
		            
		            int response = -1;
					response = connection.getResponseCode();
					L.i("opds: "+connection.getResponseMessage());
					if (EXTENDED_LOG) L.d("Response: " + response);
					if ( response == 301 || response == 302 || response == 307 || response == 303 ) {
						// redirects
						String redirect = connection.getHeaderField("Location");
						if (null == redirect) {
							onError("Invalid redirect " + response);
							return;
						}
						L.d("continue with next part: " + url);
						url = new URL(redirect);
						if (visited.contains(url.toString())) {
							onError("Duplicate redirect " + url);
							return;
						}
						loadNext = true;
						L.d("Response " + response + ": redirect to " + url);
						continue;
					}
					if ( response != 200 ) {
						onError(url+" - Error " + response + ": " + connection.getResponseMessage());
						coolReader.getDB().updateOPDSCatalog(catalogURL, "was_error", "1");
						Log.i("WASERROR", catalogURL+": 1");
						coolReader.setNeedRefreshOPDS();
						return;
					} else {
						coolReader.getDB().updateOPDSCatalog(catalogURL, "was_error", "0");
						Log.i("WASERROR", catalogURL+": 0");
						coolReader.setNeedRefreshOPDS();
					}
					
					if (cancelled)
						break;
					String contentType = connection.getContentType();
					String contentEncoding = connection.getContentEncoding();
					int contentLen = connection.getContentLength();
					//connection.getC
					if (EXTENDED_LOG) L.d("Entity content length: " + contentLen);
					if (EXTENDED_LOG) L.d("Entity content type: " + contentType);
					if (EXTENDED_LOG) L.d("Entity content encoding: " + contentEncoding);
					setProgressMessage(url.toString(), contentLen);
					InputStream is = connection.getInputStream();
					if (delayedProgress != null)
						delayedProgress.cancel();
					is = new ProgressInputStream(is, startTimeStamp, progressMessage, contentLen, 80);
					final int MAX_CONTENT_LEN_TO_BUFFER = 256*1024;
					boolean isZip = contentType!=null && contentType.equals("application/zip");
					if ( expectedType!=null )
						contentType = expectedType;
					else if ( contentLen>0 && contentLen<MAX_CONTENT_LEN_TO_BUFFER) { // autodetect type
						byte[] buf = new byte[contentLen];
						if ( is.read(buf)!=contentLen ) {
							onError("Wrong content length");
							return;
						}
						is.close();
						is = new ByteArrayInputStream(buf);
						if ( findSubstring(buf, "<?xml version=")>=0 && findSubstring(buf, "<feed")>=0  )
							contentType = "application/atom+xml"; // override type
					}
					if ( contentType.startsWith("application/atom+xml") ) {
						if (EXTENDED_LOG) L.d("Parsing feed");
						parseFeed(is);
						itemsLoadedPartially = true;
						if (handler.docInfo.nextLink!=null && handler.docInfo.nextLink.type.startsWith("application/atom+xml;profile=opds-catalog")) {
							if (handler.entries.size() < MAX_OPDS_ITEMS) {
								url = new URL(handler.docInfo.nextLink.href);
								loadNext = !visited.contains(url.toString());
								L.d("continue with next part: " + url);
							} else {
								L.d("max item count reached: " + handler.entries.size());
								loadNext = false;
							}
						} else {
							loadNext = false;
						}
							
					} else {
						if (fileName == null)
							fileName = defaultFileName;
						L.d("Downloading book: " + contentEncoding);
						downloadBook(contentType, initial_url.toString(), url.toString(), is,
								contentLen, fileName, defaultFileName, isZip);
						hideProgress();
						loadNext = false;
						itemsLoadedPartially = false;
					}
				} catch (Exception e) {
					L.e(coolReader.getString(R.string.uri_error) + " " + url.toString()
							+" "+e.getMessage(), e);
					if ( progressShown )
						Services.getEngine().hideProgress();
					String sErr = e.getMessage();
					if (!StrUtils.isEmptyStr(sErr))
						if (sErr.length()>100) sErr = sErr.substring(0,99);
					onError(coolReader.getString(R.string.catalog_error) + ": "+sErr);
					coolReader.getDB().updateOPDSCatalog(catalogURL, "was_error", "1");
					Log.i("WASERROR", catalogURL+": 1");
					coolReader.setNeedRefreshOPDS();
					break;
				} finally {
					if ( connection!=null )
						try {
							connection.disconnect();
						} catch ( Exception e ) {
							// ignore
						}
				}
			
				partialDownloadCompleted = true; // don't show progress
				
				if (loadNext && !cancelled) {
					// partially loaded
					if ( progressShown )
						Services.getEngine().hideProgress();
					final ArrayList<EntryInfo> entries = new ArrayList<>();
					entries.addAll(handler.entries);
					BackgroundThread.instance().executeGUI(() -> {
						L.d("Parsing is partially. " + handler.entries.size() + " entries found -- updating view");
						if (!callback.onEntries(handler.docInfo, entries))
							cancel();
					});
				}
			} while (loadNext && !cancelled);
			if (delayedProgress != null)
				delayedProgress.cancel();
			hideProgress();
			if (itemsLoadedPartially && !cancelled) {
				BackgroundThread.instance().executeGUI(() -> {
					L.d("Parsing is finished successfully. " + handler.entries.size() + " entries found");
					hideProgress();
					if (!callback.onFinish(handler.docInfo, handler.entries))
						cancel();
				});
			}
		}

		public void run() {
			BackgroundThread.instance().postBackground(() -> {
				try {
					runInternal();
				} catch ( Exception e ) {
					L.e("exception while opening OPDS", e);
				}
			});
		}

		public void cancel() {
			if (!cancelled) {
				L.d("cancelling current download task");
				cancelled = true;
			}
		}

		volatile private boolean cancelled = false;
		
		private boolean progressShown = false;
		
		private boolean partialDownloadCompleted = false;
	
		public class ProgressInputStream extends InputStream {

			private static final int TIMEOUT = 1500;  
			
			private final InputStream sourceStream;
			private final int totalSize;
			private final String progressMessage;
			private long lastUpdate;
			private int lastPercent;
			private int maxPercentToStartShowingProgress;
			private int bytesRead;
			
			public ProgressInputStream( InputStream sourceStream, long startTimeStamp, String progressMessage, int totalSize, int maxPercentToStartShowingProgress ) {
				this.sourceStream = sourceStream;
				this.totalSize = totalSize;
				this.maxPercentToStartShowingProgress = maxPercentToStartShowingProgress * 100;
				this.progressMessage = progressMessage;
				this.lastUpdate = startTimeStamp;
				this.bytesRead = 0;
			}

			private void updateProgress() {
				long ts = System.currentTimeMillis();
				long delay = ts - lastUpdate;
				if ( delay > TIMEOUT ) {
					lastUpdate = ts;
					int percent = 0;
					if ( totalSize>0 ) {
						percent = bytesRead * 100 / totalSize * 100;
					}
					if ( !partialDownloadCompleted && (!progressShown || percent!=lastPercent) && (progressShown || percent<maxPercentToStartShowingProgress || delay > TIMEOUT*2 ) ) {
						Services.getEngine().showProgress(percent, progressMessage, "");
						lastPercent = percent;
						progressShown = true;
					}
				}
					
			}
			
			@Override
			public int read() throws IOException {
				bytesRead++;
				updateProgress();
				return sourceStream.read();
			}

			@Override
			public void close() throws IOException {
				super.close();
			}
		}
		
	}
	private static DownloadTask currentTask;
	public static DownloadTask create(String catalogURL,
			                          CoolReader coolReader, URL uri, String defaultFileName, String expectedType,
									  String referer, DownloadCallback callback, String username, String password,
									  String proxy_addr, String proxy_port,
									  String proxy_uname, String proxy_passw,
									  int onion_def_proxy) {
		if (currentTask != null)
			currentTask.cancel();
		final DownloadTask task = new DownloadTask(catalogURL, coolReader, uri, defaultFileName, expectedType,
				referer, callback, username, password,
				proxy_addr, proxy_port,
				proxy_uname, proxy_passw,
				onion_def_proxy);
		currentTask = task;
		return task;
	}

	public static class SubstTable {
		private final int startChar;
		private final String[] replacements;
		public SubstTable( int startChar, String[] replacements ) {
			this.startChar = startChar;
			this.replacements = replacements;
		}
		public boolean isInRange( char ch ) {
			return ch>=startChar && ch<startChar + replacements.length;
		}

		public String get( char ch ) {
			return (ch>=startChar && ch<startChar + replacements.length) ? replacements[ch - startChar] : "";
		}
	}
	
	public static final int PROGRESS_DELAY_MILLIS = 2000;
	public static final int MAX_OPDS_ITEMS = 1000;
}
