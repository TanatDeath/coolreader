package org.coolreader.dic.struct;
import org.coolreader.utils.StrUtils;

import java.util.ArrayList;
import java.util.List;

public class DicStruct {
	public String srcText;
	public List<Lemma> lemmas = new ArrayList<>();
	public List<SuggestionLine> suggs = new ArrayList<>();
	public List<String> elementsL = new ArrayList<>();
	public List<String> elementsR = new ArrayList<>();

	public int getCount() {
		int cnt = 0;
		for (Lemma lemma: lemmas) {
			cnt += lemma.dictEntries.size();
			cnt += lemma.translLines.size();
			for (TranslLine tl: lemma.translLines) {
				if (!StrUtils.isEmptyStr(tl.transGroup)) cnt += 1;
				cnt += tl.exampleLines.size();
			}
		}
		cnt += suggs.size();
		cnt += Math.max(elementsL.size(), elementsR.size());
		return cnt;
	}

	public Lemma getLemmaByNum(int num) {
		int cnt = 0;
		Lemma l = null;
		for (Lemma lemma: lemmas) {
			l = lemma;
			for (DictEntry de: lemma.dictEntries) {
				if (num == cnt) return lemma;
				cnt++;
			}
			for (TranslLine tl: lemma.translLines) {
				if (!StrUtils.isEmptyStr(tl.transGroup)) {
					if (num == cnt) return lemma;
					cnt++;
				}
				if (num == cnt) return lemma;
				cnt++;
				for (ExampleLine el: tl.exampleLines) {
					if (num == cnt) return lemma;
					cnt++;
				}
			}
		}
		return l;
	}

	public Object getByNum(int num) {
		int cnt = 0;
		for (Lemma lemma: lemmas) {
			for (DictEntry de: lemma.dictEntries) {
				if (num == cnt) return de;
				cnt++;
			}
			for (TranslLine tl: lemma.translLines) {
				if (!StrUtils.isEmptyStr(tl.transGroup)) {
					if (num == cnt) return "~" + tl.transGroup;
					cnt++;
				}
				if (num == cnt) return tl;
				cnt++;
				for (ExampleLine el: tl.exampleLines) {
					if (num == cnt) return el;
					cnt++;
				}
			}
		}
		for (SuggestionLine sugg: suggs) {
			if (num == cnt) return sugg;
			cnt++;
		}
		for (int i = 0; i < Math.max(elementsL.size(), elementsR.size()); i++) {
			if (num == cnt) {
				String leftPart = null;
				String rightPart = null;
				if (elementsL.size() > i) leftPart = elementsL.get(i);
				if (elementsR.size() > i) rightPart = elementsR.get(i);
				if ((leftPart != null) && (rightPart != null)) {
					LinePair pair = new LinePair(leftPart, rightPart);
					return pair;
				}
				if (leftPart != null) {
					LinePair pair = new LinePair(leftPart, "");
					return pair;
				}
				if (rightPart != null) {
					LinePair pair = new LinePair("", rightPart);
					return pair;
				}
				return new LinePair("", "");
			}
			cnt++;
		}
		return cnt;
	}

	public String getFirstTranslation() {
		if (lemmas.size() == 0) return "";
		if (lemmas.get(0).dictEntries.size() == 0) return "";
		String text = StrUtils.getNonEmptyStr(lemmas.get(0).dictEntries.get(0).dictLinkText, true);
		if (!StrUtils.isEmptyStr(lemmas.get(0).dictEntries.get(0).tagType))
			text = text + "; " + lemmas.get(0).dictEntries.get(0).tagType.trim();
		if (!StrUtils.isEmptyStr(lemmas.get(0).dictEntries.get(0).tagWordType))
			text = text + "; " + lemmas.get(0).dictEntries.get(0).tagWordType.trim();
		if (lemmas.get(0).translLines.size() > 0) {
			text = text + " -> " + lemmas.get(0).translLines.get(0).transText;
			if (!StrUtils.isEmptyStr(lemmas.get(0).translLines.get(0).transType))
				text = text + "; " + lemmas.get(0).translLines.get(0).transType.trim();
		}
		return text;
	}

	public String getTranslation(int num) {
		int cnt = 0;
		DictEntry thisDE = null;
		TranslLine thisTL = null;
		boolean needStop = false;
		for (Lemma lemma: lemmas) {
			for (DictEntry de: lemma.dictEntries) {
				if (!needStop) thisDE = de;
				if (num == cnt) needStop = true;
				cnt++;
			}
			for (TranslLine tl: lemma.translLines) {
				if ((!needStop) || (thisTL == null)) thisTL = tl;
				if (!StrUtils.isEmptyStr(tl.transGroup)) {
					if (num == cnt) needStop = true;
					cnt++;
				}
				if (num == cnt) needStop = true;
				cnt++;
				for (ExampleLine el: tl.exampleLines) {
					if (num == cnt) needStop = true;
					cnt++;
				}
			}
		}
		if (!needStop) return getFirstTranslation();
		if (thisDE == null) return getFirstTranslation();
		String text = StrUtils.getNonEmptyStr(thisDE.dictLinkText, true);
		if (!StrUtils.isEmptyStr(thisDE.tagType))
			text = text + "; " + thisDE.tagType.trim();
		if (!StrUtils.isEmptyStr(thisDE.tagWordType))
			text = text + "; " + thisDE.tagWordType.trim();
		if (thisTL != null) {
			text = text + " -> " + thisTL.transText;
			if (!StrUtils.isEmptyStr(thisTL.transType))
				text = text + "; " + thisTL.transType.trim();
		}
		return text;
	}

	public String getAsJSON() {
		return ""; //TODO: to develop
	}

}
