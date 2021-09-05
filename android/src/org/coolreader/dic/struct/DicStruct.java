package org.coolreader.dic.struct;
import org.coolreader.crengine.StrUtils;

import java.util.ArrayList;
import java.util.List;

public class DicStruct {
	public List<Lemma> lemmas = new ArrayList<>();
	public List<String> elementsL = new ArrayList<>();
	public List<String> elementsR = new ArrayList<>();

	public int getCount() {
		int cnt = 0;
		for (Lemma lemma: lemmas) {
			cnt += lemma.dictEntry.size();
			cnt += lemma.translLine.size();
			for (TranslLine tl: lemma.translLine) {
				if (!StrUtils.isEmptyStr(tl.transGroup)) cnt += 1;
				cnt += tl.exampleLines.size();
			}
		}
		cnt += Math.max(elementsL.size(), elementsR.size());
		return cnt;
	}

	public Object getByNum(int num) {
		int cnt = 0;
		for (Lemma lemma: lemmas) {
			for (DictEntry de: lemma.dictEntry) {
				if (num == cnt) return de;
				cnt++;
			}
			for (TranslLine tl: lemma.translLine) {
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
		cnt += Math.max(elementsL.size(), elementsR.size());
		return cnt;
	}

	public String getFirstTranslation() {
		if (lemmas.size() == 0) return "";
		if (lemmas.get(0).dictEntry.size() == 0) return "";
		String text = StrUtils.getNonEmptyStr(lemmas.get(0).dictEntry.get(0).dictLinkText, true);
		if (!StrUtils.isEmptyStr(lemmas.get(0).dictEntry.get(0).tagType))
			text = text + "; " + lemmas.get(0).dictEntry.get(0).tagType.trim();
		if (!StrUtils.isEmptyStr(lemmas.get(0).dictEntry.get(0).tagWordType))
			text = text + "; " + lemmas.get(0).dictEntry.get(0).tagWordType.trim();
		if (lemmas.get(0).translLine.size() > 0) {
			text = text + " -> " + lemmas.get(0).translLine.get(0).transText;
			if (!StrUtils.isEmptyStr(lemmas.get(0).translLine.get(0).transType))
				text = text + "; " + lemmas.get(0).translLine.get(0).transType.trim();
		}
		return text;
	}

	public String getTranslation(int num) {
		int cnt = 0;
		DictEntry thisDE = null;
		TranslLine thisTL = null;
		boolean needStop = false;
		for (Lemma lemma: lemmas) {
			for (DictEntry de: lemma.dictEntry) {
				if (!needStop) thisDE = de;
				if (num == cnt) needStop = true;
				cnt++;
			}
			for (TranslLine tl: lemma.translLine) {
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

}
