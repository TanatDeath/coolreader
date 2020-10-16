package org.coolreader.cloud.litres;

public class LitresSearchParams {

	public static int SEARCH_TYPE_ARTS = 1;
	public static int SEARCH_TYPE_MY_BOOKS = 2;
	public static int SEARCH_TYPE_ARTS_BY_GENRE = 3;
	public static int SEARCH_TYPE_GENRES = 4;
	public static int SEARCH_TYPE_PERSONS = 5;
	public static int SEARCH_TYPE_COLLECTIONS = 6;
	public static int SEARCH_TYPE_SEQUENCES = 7;
	public static int SEARCH_TYPE_ARTS_BY_COLLECTION = 8;
	public static int SEARCH_TYPE_ARTS_BY_SEQUENCE = 9;
	public static int SEARCH_TYPE_ARTS_BY_PERSON = 10;

	public int searchType;
	public int newOrPop;
	public int beginIndex;
	public int count;
	public String searchString;
	public String lastName;
	public String firstName;
	public String middleName;
	public int groupId;
	public int searchModifier;

	public LitresSearchParams(int searchType, int newOrPop, int beginIndex, int count, String searchString, int groupId, int searchModifier) {
		this.searchType = searchType;
		this.newOrPop = newOrPop;
		this.beginIndex = beginIndex;
		this.count = count;
		this.searchString = searchString;
		this.groupId = groupId;
		this.searchModifier = searchModifier;
	}

	public LitresSearchParams copy() {
		return new LitresSearchParams(searchType, newOrPop, beginIndex, count, searchString, groupId, searchModifier);
	}

	public void nextPage() {
		beginIndex = beginIndex + count;
	}

	public void prevPage() {
		beginIndex = beginIndex - count;
		if (beginIndex < 0) beginIndex = 0;
	}

	public boolean allowNewOrPop() {
		if (
		     (searchType == SEARCH_TYPE_ARTS)
		  || (searchType == SEARCH_TYPE_ARTS_BY_GENRE)
		  || (searchType == SEARCH_TYPE_ARTS_BY_COLLECTION)
		  || (searchType == SEARCH_TYPE_ARTS_BY_SEQUENCE)
		  || (searchType == SEARCH_TYPE_ARTS_BY_PERSON)
		) return true;
		return false;
	}
}
