package org.coolreader.crengine;

import org.coolreader.utils.StrUtils;

public class BookInfoEntry {
	public String infoTitle;
	public String infoValue;
	public String infoType;

	public BookInfoEntry(String iTitle, String iValue, String iType) {
		infoTitle = StrUtils.getNonEmptyStr(iTitle, true);
		infoValue = StrUtils.getNonEmptyStr(iValue, true);
		infoType = StrUtils.getNonEmptyStr(iType, true);
	}
}
