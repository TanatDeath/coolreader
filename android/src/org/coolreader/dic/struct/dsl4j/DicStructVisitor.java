package org.coolreader.dic.struct.dsl4j;

import org.coolreader.dic.struct.ExampleLine;
import org.coolreader.dic.struct.Lemma;
import org.coolreader.dic.struct.TranslLine;
import org.coolreader.utils.StrUtils;

import java.util.ArrayList;
import java.util.List;

import io.github.eb4j.dsl.DslArticle;
import io.github.eb4j.dsl.visitor.DslVisitor;

/**
 * See: http://lingvo.helpmax.net/en/troubleshooting/dsl-compiler/dsl-card-structure/
 */
public class DicStructVisitor extends DslVisitor<List<Lemma>> {
	protected List<Lemma> lemmas;
	private Lemma curLemma;
	private TranslLine curTranslLine;
	private ExampleLine curEx;
	private boolean inTrn = false;
	private boolean wasTrn = false;
	private boolean inEx = false;
	private boolean wasEx = false;
	private boolean needClear = false;

	private void doClear() {
		lemmas = new ArrayList<>();
		curLemma = new Lemma();
		lemmas.add(curLemma);
		needClear = false;
	}

	/**
	 * Constructor.
	 */
	public DicStructVisitor() {
		doClear();
	}


	/**
	 * Visit a tag.
	 *
	 * @param tag to visit.
	 */
	@Override
	public void visit(final DslArticle.Tag tag) {
		if (needClear) doClear();
		if (StrUtils.getNonEmptyStr(tag.getTagName(),true).equals("trn")) {
			inTrn = true;
			wasTrn = true;
			curTranslLine = new TranslLine();
			curLemma.translLines.add(curTranslLine);
		}
		if ((StrUtils.getNonEmptyStr(tag.getTagName(),true).equals("ex")) && (inTrn)) {
			inEx = true;
			wasEx = true;
			curEx = new ExampleLine("");
			curTranslLine.exampleLines.add(curEx);
		}
	}

	/**
	 * Visit a text.
	 *
	 * @param t Text object to visit.
	 */
	@Override
	public void visit(final DslArticle.Text t) {
		if (needClear) doClear();
		if (inTrn) {
			if (inEx)
				curEx.line = curEx.line + t;
			else {
				if ((wasEx) && (!StrUtils.isEmptyStr(curTranslLine.transText))) {
					wasEx = false;
					curTranslLine = new TranslLine();
					curTranslLine.transText = curTranslLine.transText + t;
					curLemma.translLines.add(curTranslLine);
				} else
					curTranslLine.transText = curTranslLine.transText + t;
			}
		} else {
			if ((wasTrn) && (!StrUtils.isEmptyStr(curLemma.lemmaText))) {
				wasTrn = false;
				curLemma = new Lemma();
				curLemma.lemmaText = curLemma.lemmaText + t;
				lemmas.add(curLemma);
			} else
				curLemma.lemmaText = curLemma.lemmaText + t;
		}
	}

	/**
	 * Visit an Attribute.
	 *
	 * @param a Attribute object to visit.
	 */
	@Override
	public void visit(final DslArticle.Attribute a) {
	}

	/**
	 * Visit a NewLine.
	 *
	 * @param n newline object to visit.
	 */
	@Override
	public void visit(final DslArticle.Newline n) {
		if (needClear) doClear();
		if (inTrn) {
			if (inEx) {
				if (!StrUtils.getNonEmptyStr(curEx.line, false).endsWith("\n"))
					curEx.line = curEx.line + "\n";
			} else if (!StrUtils.getNonEmptyStr(curTranslLine.transText, false).endsWith("\n"))
				curTranslLine.transText = curTranslLine.transText + "\n";
		}
		else {
			if (!StrUtils.getNonEmptyStr(curLemma.lemmaText, false).endsWith("\n"))
				curLemma.lemmaText = curLemma.lemmaText + "\n";
		}
	}

	/**
	 * Visit an EndTag.
	 *
	 * @param endTag to visit.
	 */
	@Override
	public void visit(final DslArticle.EndTag endTag) {
		if (needClear) doClear();
		if (StrUtils.getNonEmptyStr(endTag.getTagName(),true).equals("trn"))
			inTrn = false;
		if (StrUtils.getNonEmptyStr(endTag.getTagName(),true).equals("ex") && (inTrn))
			inEx = false;
	}

	@Override
	public void finish() {
	}

	/**
	 * Return result.
	 *
	 * @return result.
	 */
	@Override
	public List<Lemma> getObject() {
		needClear = true;
		for (Lemma le: lemmas) {
			boolean hasEmpty = true;
			while (hasEmpty) {
				hasEmpty = false;
				for (TranslLine tl: le.translLines) {
					if (StrUtils.isEmptyStr(tl.transText)) {
						le.translLines.remove(tl);
						hasEmpty = true;
						break;
					}
				}
			}
		}
		return lemmas;
	}
}
