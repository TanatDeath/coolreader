package org.coolreader.crengine;

public class DicSearchHistoryEntry {

    public static int ACTION_SAVE = 0;
    public static int ACTION_CLEAR_ALL = 1;

    public DicSearchHistoryEntry()
    {
    }

    private Long id;
    private String search_text;
    private String text_translate;
    private String search_from_book;
    private String dictionary_used;
    private long create_time = System.currentTimeMillis(); // UTC timestamp
    private long last_access_time = System.currentTimeMillis(); // UTC timestamp
    private String language_from;
    private String language_to;
    private Long seen_count;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSearch_text() {
        return StrUtils.getNonEmptyStr(search_text,true);
    }

    public void setSearch_text(String search_text) {
        this.search_text = search_text;
    }

    public String getText_translate() {
        return StrUtils.getNonEmptyStr(text_translate,true);
    }

    public void setText_translate(String text_translate) {
        this.text_translate = text_translate;
    }

    public String getSearch_from_book() {
        return StrUtils.getNonEmptyStr(search_from_book,true);
    }

    public void setSearch_from_book(String search_from_book) {
        this.search_from_book = search_from_book;
    }

    public String getDictionary_used() {
        return StrUtils.getNonEmptyStr(dictionary_used,true);
    }

    public void setDictionary_used(String dictionary_used) {
        this.dictionary_used = dictionary_used;
    }

    public long getCreate_time() {
        return create_time;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public long getLast_access_time() {
        return last_access_time;
    }

    public void setLast_access_time(long last_access_time) {
        this.last_access_time = last_access_time;
    }

    public String getLanguage_from() {
        return StrUtils.getNonEmptyStr(language_from,true);
    }

    public void setLanguage_from(String language_from) {
        this.language_from = language_from;
    }

    public String getLanguage_to() {
        return StrUtils.getNonEmptyStr(language_to,true);
    }

    public void setLanguage_to(String language_to) {
        this.language_to = language_to;
    }

    public Long getSeen_count() {
        return seen_count;
    }

    public void setSeen_count(Long seen_count) {
        this.seen_count = seen_count;
    }

}
