package org.coolreader.options;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.utils.StrUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;

import okhttp3.HttpUrl;

public class WikiOption extends ListOption {

	public static String WIKI_ADRESSES = "https://en.wikipedia.org/wiki/List_of_Wikipedias";

	private Document docJsoup = null;
	private ArrayList<String[]> wikiLangs = new ArrayList<String[]>();
	final BaseActivity mActivity;

	public WikiOption(BaseActivity activity, OptionOwner owner, String title, String prop, String addInfo, String filter) {
		super(owner, title, prop, addInfo, filter);
		mActivity = activity;
		this.updateFilteredMark("");
	}

	public void fillList() {
		if (listView == null) return;
		list.clear();
		listFiltered.clear();
		for(int i=0;i<wikiLangs.size();i++){
			OptionsDialog.Three item = new OptionsDialog.Three(
					wikiLangs.get(i)[1], wikiLangs.get(i)[0], wikiLangs.get(i)[1]);
			list.add(item);
			listFiltered.add(item);
		}
		listAdapter = new BaseAdapter() {

			public boolean areAllItemsEnabled() {
				return true;
			}

			public boolean isEnabled(int position) {
				return true;
			}

			public int getCount() {
				return listFiltered.size();
			}

			public Object getItem(int position) {
				return listFiltered.get(position);
			}

			public long getItemId(int position) {
				return position;
			}

			public int getItemViewType(int position) {
				return 0;
			}

			public View getView(final int position, View convertView,
								ViewGroup parent) {
				ViewGroup layout;
				final OptionsDialog.Three item = listFiltered.get(position);
				if (convertView == null) {
					layout = (ViewGroup)mInflater.inflate(getItemLayoutId(), null);
					//view = new TextView(getContext());
				} else {
					layout = (ViewGroup)convertView;
				}
				updateItemContents( layout, item, listView, position );
				return layout;
			}

			public int getViewTypeCount() {
				return 1;
			}

			public boolean hasStableIds() {
				return true;
			}

			public boolean isEmpty() {
				return listFiltered.size()==0;
			}

			private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

			public void registerDataSetObserver(DataSetObserver observer) {
				observers.add(observer);
			}

			public void unregisterDataSetObserver(DataSetObserver observer) {
				observers.remove(observer);
			}

		};
		int selItem = getSelectedItemIndex();
		if (selItem < 0)
			selItem = 0;
		listView.setAdapter(listAdapter);
		listAdapter.notifyDataSetChanged();
		listView.setSelection(selItem);
	}

	@Override
	public void whenOnSelect(){
		if (wikiLangs.isEmpty())
			BackgroundThread.instance().postBackground(() -> {
				try {
					HttpUrl hurl = HttpUrl.parse(WIKI_ADRESSES);
					if (hurl == null) {
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(
								() -> mActivity.showToast(mActivity.getString(R.string.wiki_list_error) + " - cannot parse link"), 100));
						return;
					}
					final HttpUrl.Builder urlBuilder = hurl.newBuilder();
					final String url = urlBuilder.build().toString();
					docJsoup = Jsoup.parse(urlBuilder.build().url(), 180000); // three minutes
					Elements resultLinks = docJsoup.select(".extiw");
					if (resultLinks.size()>0) {
						ArrayList<String> t = new ArrayList<String>();
						for (Element el: resultLinks) {
							String elHref = StrUtils.getNonEmptyStr(el.attr("href"),false); // "https://ru.wikipedia.org/wiki/"
							String elTitle = StrUtils.getNonEmptyStr(el.attr("title"), true);
							if (!StrUtils.isEmptyStr(elHref)) {
								if (elHref.endsWith("/wiki/")) {
									if (!t.contains(elHref)) {
										wikiLangs.add(new String[]{elTitle.replace(":", ""), elHref.replace("/wiki/","")});
										t.add(elHref);
									}
								}
							}
						}
						Collections.sort(wikiLangs, (lhs, rhs) -> {
							//if (lhs[0].equals("ru")) return -1;
							return lhs[1].compareTo(rhs[1]);
						});
						wikiLangs.add(0, new String[]{"en", "https://en.wikipedia.org"});
						BackgroundThread.instance().postGUI(() -> fillList(), 100);
					}
				} catch (Exception e) {
					docJsoup = null;
					BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(
							() -> mActivity.showToast(mActivity.getString(R.string.wiki_list_error) + " - "+
									e.getClass().getSimpleName()+" "+e.getMessage()), 100));
				}
			});
	}
}