package org.coolreader.dic;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.Settings;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TranslationDirectionDialog extends BaseDialog {

	public interface ValuesEnteredCallback {
		public void done(ArrayList<String> results);
	}

	public static final Logger log = L.create("transldir");

	public static int FOR_COMMON = 0;
	public static int FOR_YND = 1;
	public static int FOR_LINGVO = 2;

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private FileInfo fInfo;
	private ArrayList<TextView> textViews = new ArrayList<>();
	private ArrayList<EditText> editTexts = new ArrayList<>();
	public final ValuesEnteredCallback callback;
	private TranslList mList;
	private int listType = 0; // 1 - ynd; 2 - lingvo
	public static OkHttpClient client = new OkHttpClient();

	ArrayList<String[]> mTransl = new ArrayList<>();
	ArrayList<String[]> mTranslFiltered = new ArrayList<>();
	TreeMap<String, String> yndLangs = new TreeMap<>();

	private void doFilterList(String filter) {
		if (listType > 0) {
			mTranslFiltered.clear();
			for (String[] arrS: mTransl) {
				boolean found = false;
				for (String s: arrS) {
					if (!StrUtils.isEmptyStr(filter))
						if (s.toUpperCase().contains(filter.toUpperCase())) found = true;
					if (!StrUtils.isEmptyStr(editTexts.get(0).getText().toString()))
						if (s.toUpperCase().contains(editTexts.get(0).getText().toString().toUpperCase())) found = true;
					if (!StrUtils.isEmptyStr(editTexts.get(1).getText().toString()))
						if (s.toUpperCase().contains(editTexts.get(1).getText().toString().toUpperCase())) found = true;
				}
				if ((StrUtils.isEmptyStr(editTexts.get(0).getText().toString()))&&
					(StrUtils.isEmptyStr(editTexts.get(1).getText().toString()))&&
					(StrUtils.isEmptyStr(filter))) found = true;
				if (found) mTranslFiltered.add(arrS);
			}
			TranslListAdapter tla = new TranslListAdapter();
			mList.setAdapter(tla);
			tla.notifyDataSetChanged();
		}
	}

	class TranslListAdapter extends BaseAdapter {

		public final static int ITEM_POSITION=0;

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return mTranslFiltered.size();
		}

		public Object getItem(int position) {
			if ( position<0 || position>=mTranslFiltered.size() )
				return null;
			return mTranslFiltered.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (listType == 2) {
				int res = R.layout.transl_item_lingvo;
				view = mInflater.inflate(res, null);
				String[] arrS = mTranslFiltered.get(position);
				if (arrS != null) {
					if (arrS.length > 0) {
						TextView tv_locale = (TextView) view.findViewById(R.id.transl_item_locale);
						tv_locale.setText(arrS[0]);
					}
					if (arrS.length > 1) {
						TextView tv_langcode = (TextView) view.findViewById(R.id.transl_item_langcode);
						tv_langcode.setText(arrS[1]);
					}
					if (arrS.length > 2) {
						TextView tv_lcid_str = (TextView) view.findViewById(R.id.transl_item_lcid_str);
						tv_lcid_str.setText(arrS[2]);
					}
					if (arrS.length > 3) {
						TextView tv_lcid_dec = (TextView) view.findViewById(R.id.transl_item_lcid_dec);
						tv_lcid_dec.setText(arrS[3]);
					}
					if (arrS.length > 4) {
						TextView tv_lcid_hex = (TextView) view.findViewById(R.id.transl_item_lcid_hex);
						tv_lcid_hex.setText(arrS[4]);
					}
					if (arrS.length > 5) {
						TextView tv_lcid_cp = (TextView) view.findViewById(R.id.transl_item_lcid_cp);
						tv_lcid_cp.setText(arrS[5]);
					}
				}
				TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
						{R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
				int colorGrayC = a.getColor(0, Color.GRAY);
				int colorIcon = a.getColor(1, Color.GRAY);
				a.recycle();
				int colorGrayCT = Color.argb(30, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC));
				int colorGrayCT2 = Color.argb(200, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC));
				Button btnFrom = view.findViewById(R.id.transl_item_lanf_from);
				btnFrom.setBackgroundColor(colorGrayCT2);
				final String[] sArrS = arrS;
				btnFrom.setOnClickListener(v -> {
					String s = "";
					if (sArrS.length > 2) s = sArrS[2];
					if (StrUtils.isEmptyStr(s)) {
						if (sArrS.length > 3) s = sArrS[3];
					}
					if (!StrUtils.isEmptyStr(s)) editTexts.get(0).setText(sArrS[2]);
				});
				btnFrom.setTextColor(colorIcon);
				Button btnTo = view.findViewById(R.id.transl_item_lanf_to);
				btnTo.setBackgroundColor(colorGrayCT2);
				btnTo.setTextColor(colorIcon);
				btnTo.setOnClickListener(v -> {
					String s = "";
					if (sArrS.length > 2) s = sArrS[2];
					if (StrUtils.isEmptyStr(s)) {
						if (sArrS.length > 3) s = sArrS[3];
					}
					if (!StrUtils.isEmptyStr(s)) editTexts.get(1).setText(sArrS[2]);
				});
			} else {
				int res = R.layout.transl_item_ynd;
				view = mInflater.inflate(res, null);
				String[] arrS = mTranslFiltered.get(position);
				if (arrS != null) {
					if (arrS[0].contains("-")) {
						String[] arrS2 = arrS[0].split("-");
						String sText = StrUtils.getNonEmptyStr(arrS2[0], true);
						TextView tv_from = (TextView) view.findViewById(R.id.transl_item_from_lang);
						if (yndLangs.get(sText) != null) {
							sText = sText + ": " + yndLangs.get(sText);
						}
						tv_from.setText(sText);
					}
				}
				if (arrS != null) {
					if (arrS[0].contains("-")) {
						String[] arrS2 = arrS[0].split("-");
						String sText = StrUtils.getNonEmptyStr(arrS2[1], true);
						TextView tv_from = (TextView) view.findViewById(R.id.transl_item_to_lang);
						if (yndLangs.get(sText) != null) {
							sText = sText + ": " + yndLangs.get(sText);
						}
						tv_from.setText(sText);
					}
				}
				TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
						{R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
				int colorGrayC = a.getColor(0, Color.GRAY);
				int colorIcon = a.getColor(1, Color.GRAY);
				a.recycle();
				int colorGrayCT = Color.argb(30, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC));
				int colorGrayCT2 = Color.argb(200, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC));
				Button btnFrom = view.findViewById(R.id.transl_item_lanf_select);
				btnFrom.setBackgroundColor(colorGrayCT2);
				if (arrS != null) {
					if (arrS[0].contains("-")) {
						btnFrom.setOnClickListener(v -> {
							String[] arrS2 = arrS[0].split("-");
							String s = "";
							if (arrS2.length > 0) s = arrS2[0];
							if (!StrUtils.isEmptyStr(s)) editTexts.get(0).setText(arrS2[0]);
							s = "";
							if (arrS2.length > 0) s = arrS2[1];
							if (!StrUtils.isEmptyStr(s)) editTexts.get(1).setText(arrS2[1]);
						});
					}
				}
			}
			//TextView titleTextView = (TextView)view.findViewById(R.id.dict_item_title);
//			if ( s!=null ) {
//				if ( titleTextView!=null )
//					titleTextView.setText(s);
//			} else {
//				if ( titleTextView!=null )
//					titleTextView.setText("");
//			}
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mTranslFiltered.size()==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class TranslList extends BaseListView {

		public TranslList(Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new TranslationDirectionDialog.TranslListAdapter());
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
				//mEditView.setText(mTransl.get(position));
				return true;
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			//mEditView.setText(mTransl.get(position));
			return true;
		}
	}

	public void yndButtonClick() {
		mTranslFiltered.clear();
		mTransl.clear();
		yndLangs.clear();
		listType = 2;
		if (StrUtils.isEmptyStr(Dictionaries.sYandexIAM))
			mCoolReader.mDictionaries.yandexAuthThenTranslate("", "", "", null, view,
					lst -> {
						yndLangs = lst;
						for (Map.Entry<String, String> entry : yndLangs.entrySet()) {
							mTransl.add(new String[]{entry.getValue(), entry.getKey(), entry.getKey(), entry.getKey(), entry.getKey(), entry.getKey()});
							mTranslFiltered.add(new String[]{entry.getValue(), entry.getKey(), entry.getKey(), entry.getKey(), entry.getKey(), entry.getKey()});
						}
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
							TranslListAdapter tla = new TranslListAdapter();
							mList.setAdapter(tla);
							tla.notifyDataSetChanged();
						}, 100));
					});
		else
			mCoolReader.mDictionaries.yandexTranslate("", "", "", null, view,
					lst -> {
						yndLangs = lst;
						for (Map.Entry<String, String> entry : yndLangs.entrySet()) {
							mTransl.add(new String[]{entry.getValue(), entry.getKey(), entry.getKey(), entry.getKey(), entry.getKey(), entry.getKey()});
							mTranslFiltered.add(new String[]{entry.getValue(), entry.getKey(), entry.getKey(), entry.getKey(), entry.getKey(), entry.getKey()});
						}
						BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
							TranslListAdapter tla = new TranslListAdapter();
							mList.setAdapter(tla);
							tla.notifyDataSetChanged();
						}, 100));
					});
		if (1==1) return;
		HttpUrl.Builder urlBuilder = HttpUrl.parse(Dictionaries.YND_DIC_GETLANGS).newBuilder();
		urlBuilder.addQueryParameter("key", BuildConfig.YND_TRANSLATE);
		urlBuilder.addQueryParameter("ui", "en");
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder()
				.url(url)
				.build();
		Call call = client.newCall(request);
		final CoolReader crf = mCoolReader;
		call.enqueue(new okhttp3.Callback() {
			public void onResponse(Call call, Response response)
					throws IOException {
				String sBody = response.body().string();
				Document docJsoup = Jsoup.parse(sBody, Dictionaries.YND_DIC_GETLANGS);
				Elements dirs = docJsoup.select("Langs > dirs > string");
				Elements langs = docJsoup.select("Langs > langs > item");
				for (Element el: dirs) {
					mTransl.add(new String[]{el.text()});
					mTranslFiltered.add(new String[]{el.text()});
				}
				for (Element el: langs) {
					yndLangs.put(el.attr("key"),el.attr("value"));
				}
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					TranslListAdapter tla = new TranslListAdapter();
					mList.setAdapter(tla);
					tla.notifyDataSetChanged();
				}, 100));
			}

			public void onFailure(Call call, IOException e) {
				crf.showToast(e.getMessage());
			}
		});
	}

	public void lingvoButtonClick() {
		mTranslFiltered.clear();
		mTransl.clear();
		listType = 2;
		try {
			InputStream is = mCoolReader.getResources().openRawResource(R.raw.lang_codes);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String str = "";
			while ((str = reader.readLine()) != null) {
				String[] arrS = str.split("~");
				if (arrS.length>0)
					if (!arrS[0].equals("Locale")) {
						mTransl.add(arrS);
						mTranslFiltered.add(arrS);
					}
			}
			is.close();
		} catch (Exception e) {
			log.e("load lang_codes file", e);
		}
		TranslListAdapter tla = new TranslListAdapter();
		mList.setAdapter(tla);
		tla.notifyDataSetChanged();
	}

	public TranslationDirectionDialog(CoolReader activity, String sTitle, String sSomeText, int iType,
									  ArrayList<String[]> askValues, ValuesEnteredCallback callback)
	{
		super("TranslationDirectionDialog", activity, sTitle, true, false);
		mCoolReader = activity;
		setTitle(sTitle);
		this.callback = callback;
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.translation_directions_dialog, null);
		setAddButtonImage(
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_settings, R.drawable.icons8_settings),
				R.string.dictionary_settings);
		TextView someText = view.findViewById(R.id.some_text);
		someText.setText(sSomeText);
		textViews.clear();
		editTexts.clear();
		for (int i = 1; i<10; i++) {
			if (i == 1) {
				TableRow tr = view.findViewById(R.id.some_value_tr1);
				TextView tv = view.findViewById(R.id.some_value_label1);
				EditText et = view.findViewById(R.id.some_value_edit1);
				if (tr != null) {
					textViews.add(tv);
					editTexts.add(et);
					if (askValues.size() >= i) {
						tv.setText(askValues.get(i-1)[0]);
						et.setHint(askValues.get(i-1)[1]);
						if (!StrUtils.isEmptyStr(askValues.get(i-1)[2])) et.setText(askValues.get(i-1)[2]);
						TextView tv1 = new TextView(mCoolReader);
						tv1.setText(R.string.ynd_autolang);
						((ViewGroup) et.getParent()).addView(tv1);
						TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
								{R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
						int colorGrayC = a.getColor(0, Color.GRAY);
						int colorIcon = a.getColor(1, Color.GRAY);
						a.recycle();
						tv1.setMaxLines(2);
						tv1.setTextColor(colorIcon);
						tv1.setOnClickListener(v -> editTexts.get(0).setText("#"));
						if (mCoolReader.getReaderView() != null)
						  if (mCoolReader.getReaderView().getLastsetWidth()>0)
						    tv1.setMaxWidth(mCoolReader.getReaderView().getLastsetWidth()/2);
					} else {
						((ViewGroup) tr.getParent()).removeView(tr);
					}
				}
				//TextView tv_a = (TextView) view.findViewById(R.id.some_value_label_after1);
				//tv_a.setText(R.string.ynd_autolang);
				et.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

					public void onTextChanged(CharSequence s, int start, int before, int count) {
						doFilterList(s.toString());
					}
				});
			}
			if (i == 2) {
				TableRow tr = (TableRow) view.findViewById(R.id.some_value_tr2);
				TextView tv = (TextView) view.findViewById(R.id.some_value_label2);
				EditText et = (EditText) view.findViewById(R.id.some_value_edit2);
				if (tr != null) {
					textViews.add(tv);
					editTexts.add(et);
					if (askValues.size() >= i) {
						tv.setText(askValues.get(i-1)[0]);
						et.setHint(askValues.get(i-1)[1]);
						if (!StrUtils.isEmptyStr(askValues.get(i-1)[2])) et.setText(askValues.get(i-1)[2]);
					} else {
						((ViewGroup) tr.getParent()).removeView(tr);
					}
				}
				et.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

					public void onTextChanged(CharSequence s, int start, int before, int count) {
						doFilterList(s.toString());
					}
				});
			}
		}
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		colorGrayC = a.getColor(0, Color.GRAY);
		int colorIcon = a.getColor(1, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		TableLayout tl = (TableLayout) view;
		LinearLayout ll = new LinearLayout(mCoolReader);
		LinearLayout.LayoutParams llp1 = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		llp1.setMargins(5, 3, 5, 3);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		Button swButton = new Button(mCoolReader);
		swButton.setLayoutParams(llp1);
		swButton.setText(mCoolReader.getString(R.string.translate_dics_switch));
		swButton.setBackgroundColor(colorGrayCT2);
		swButton.setTextColor(colorIcon);
		ll.addView(swButton);
		swButton.setOnClickListener(v -> {
			String s = editTexts.get(0).getText().toString();
			editTexts.get(0).setText(editTexts.get(1).getText().toString());
			editTexts.get(1).setText(s);
		});
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		llp.setMargins(5, 3, 5, 3);
		Button yndButton = new Button(mCoolReader);
		yndButton.setLayoutParams(llp);
		yndButton.setText(mCoolReader.getString(R.string.ynd_translate_dics_info));
		yndButton.setBackgroundColor(colorGrayCT2);
		yndButton.setTextColor(colorIcon);
		ll.addView(yndButton);
		yndButton.setOnClickListener(v -> yndButtonClick());
		Button lingvoButton = new Button(mCoolReader);
		lingvoButton.setText(mCoolReader.getString(R.string.lingvo_translate_dics_info));
		lingvoButton.setBackgroundColor(colorGrayCT2);
		lingvoButton.setTextColor(colorIcon);
		lingvoButton.setLayoutParams(llp);
		ll.addView(lingvoButton);
		lingvoButton.setOnClickListener(v -> lingvoButtonClick());
		tl.addView(ll);
		mList = new TranslList(activity, false);
		tl.addView(mList);
		setView(view);
		BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
			if (iType == FOR_YND) yndButtonClick();
			if (iType == FOR_LINGVO) lingvoButtonClick();
		}, 500));
	}

	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
		ArrayList<String> res = new ArrayList<>();
		for (int i = 1; i<10; i++) {
			if (i == 1) {
				EditText et = view.findViewById(R.id.some_value_edit1);
				if (et != null) res.add(et.getText().toString());
			}
			if (i == 2) {
				EditText et = view.findViewById(R.id.some_value_edit2);
				if (et != null) res.add(et.getText().toString());
			}
		}
		if (callback != null) callback.done(res);
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
		if (callback != null) callback.done(null);
	}

	@Override
	protected void onAddButtonClick() {
		mCoolReader.optionsFilter = "";
		mCoolReader.showOptionsDialogExt(OptionsDialog.Mode.READER, Settings.PROP_DICTIONARY_TITLE);
		onPositiveButtonClick();
	}
}
