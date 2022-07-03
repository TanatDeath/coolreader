package org.coolreader.onyx;

import android.os.AsyncTask;

import com.onyx.android.sdk.data.DictionaryQuery;
import com.onyx.android.sdk.readerutils.data.BookBean;
import com.onyx.android.sdk.readerutils.data.OnyxLibraryManager;
import com.onyx.android.sdk.utils.DictionaryUtil;

import org.coolreader.CoolReader;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.FileInfo;
import org.coolreader.dic.Dictionaries;
import org.coolreader.dic.OnyxapiTranslate;
import org.coolreader.dic.struct.DicStruct;
import org.coolreader.dic.struct.DictEntry;
import org.coolreader.dic.struct.Lemma;
import org.coolreader.dic.struct.TranslLine;
import org.coolreader.utils.Utils;

import java.util.List;

public class OnyxTranslate {

	public static boolean ONYX_API_TRANSLATE_AVAIL = true;

	public void doKeywordQueryJob(CoolReader cr, final String keyword) {
		new AsyncTask<Void, Void, DictionaryQuery>() {
			@Override
			protected DictionaryQuery doInBackground(Void... params) {
				return DictionaryUtil.queryKeyWord(cr, keyword);
			}

			@Override
			protected void onPostExecute(DictionaryQuery dictionaryQuery) {
				DicStruct dsl = new DicStruct();
				super.onPostExecute(dictionaryQuery);
				if (dictionaryQuery.getState() == DictionaryQuery.DICT_STATE_QUERY_SUCCESSFUL) {
					List<DictionaryQuery.Dictionary> list = dictionaryQuery.getList();
					if (list == null || list.size() <= 0) {
						return;
					}
					for (DictionaryQuery.Dictionary dictionary : list) {
						Lemma le = new Lemma();
						DictEntry de = new DictEntry();
						de.dictLinkText = dictionary.getDictName();
						le.dictEntries.add(de);
						TranslLine tl = new TranslLine();
						tl.transGroup = "";
						String s = Utils.cleanupHtmlTags(dictionary.getExplanation());
						for (int i = 0; i < 10; i++)
							s = s.replace("\n\n", "\n");
						tl.transText = s;
						le.translLines.add(tl);
						dsl.lemmas.add(le);
					}
				}
				Dictionaries.onyxapiTranslate.onyxapiTranslateFinish(cr, dsl);
			}
		}.execute();
	}
}
