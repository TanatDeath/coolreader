package org.coolreader.userdic;

import org.coolreader.utils.StrUtils;

public class UserDicEntry {

    public static int ACTION_NEW = 0;
    public static int ACTION_UPDATE_CNT = 1;
    public static int ACTION_DELETE = 2;

    public UserDicEntry()
    {
    }

    private Long id;
    private String dic_word;
    private String dic_word_translate;
    private String dic_from_book;
    private long create_time = System.currentTimeMillis(); // UTC timestamp
    private long last_access_time = System.currentTimeMillis(); // UTC timestamp
    private String language;
    private boolean thisIsDSHE = false;
    private Long seen_count;
    private int is_citation;
    public int isCustomColor;
    public String customColor;
    public String shortContext;
    private String dslStruct;

    public String fullContext;

    public int getIs_citation() {
        return is_citation;
    }

    public void setIs_citation(int is_citation) {
        this.is_citation = is_citation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDic_word() {
        return StrUtils.getNonEmptyStr(dic_word, false);
    }

    public void setDic_word(String dic_word) {
        this.dic_word = dic_word;
    }

    public String getDic_word_translate() {
        return dic_word_translate;
    }

    public void setDic_word_translate(String dic_word_translate) {
        this.dic_word_translate = dic_word_translate;
    }

    public String getDslStruct() {
        return dslStruct;
    }

    public void setDslStruct(String dslStruct) {
        this.dslStruct = dslStruct;
    }

    public String getDic_from_book() {
        return dic_from_book;
    }

    public void setDic_from_book(String dic_from_book) {
        this.dic_from_book = dic_from_book;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setThisIsDSHE(boolean b) {
        this.thisIsDSHE = b;
    }

    public boolean getThisIsDSHE() {
        return thisIsDSHE;
    }

    public Long getSeen_count() {
        if (seen_count == null) return 0L;
        return seen_count;
    }

    public void setSeen_count(Long seen_count) {
        if (seen_count == null)
            this.seen_count = 0L;
        else
            this.seen_count = seen_count;
    }

    public int getIsCustomColor() {
        return isCustomColor;
    }

    public void setIsCustomColor(int isCustomColor) {
        this.isCustomColor = isCustomColor;
    }

    public String getCustomColor() {
        return customColor;
    }

    public void setCustomColor(String customColor) {
        this.customColor = customColor;
    }

    public String getShortContext() {
        return shortContext;
    }

    public void setShortContext(String shortContext) {
        this.shortContext = shortContext;
    }

    public String getFullContext() {
        return fullContext;
    }

    public void setFullContext(String fullContext) {
        this.fullContext = fullContext;
    }

}
