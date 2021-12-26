package org.coolreader.crengine;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.readerview.ReaderView;

import java.util.ArrayList;
import java.util.HashMap;

public class GotoPageDialog extends BaseDialog {

	HashMap<Integer, Integer> themeColors = null;
	boolean isEInk;

	public interface GotoPageHandler {
		boolean validate(String s, boolean isPercent) throws Exception;
		void onOk(String s, boolean isPercent) throws Exception;
		void onOkPage(String s) throws Exception;
		void onCancel();
	};

	private GotoPageHandler handler;
	private EditText input;
	private EditText inputPerc;
	private boolean inTextChange;
	private boolean inSeekChange;
	private TextWatcher watcher;
	private TextWatcher watcherPerc;
	private SeekBar seekBar;
	int minValue;
	int maxValue;
	int mColorIconL =Color.GRAY;
	private int mPageCount = 0;
	private BookPagesList mList;

	public final static int ITEM_POSITION=0;

	public ReaderView getReaderView() {
		return mReaderView;
	}
	private ReaderView mReaderView;
	private LayoutInflater mInflater;
	private ArrayList<String[]> arrFound = null;
	String mFindText = "";

	class BookPagesAdapter extends BaseAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			if (arrFound == null)
				return mPageCount;
			else return arrFound.size();
		}

		public Object getItem(int position) {
			if (arrFound == null) {
				if (position < 0 || position >= mPageCount)
					return null;
				if (mReaderView.getArrAllPages()==null) return null;
				return mReaderView.getArrAllPages().get(position);
			} else {
				if (position < 0 || position >= arrFound.size())
					return null;
				return arrFound.get(position);
			}

		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.page_item;
			view = mInflater.inflate(res, null);
			TextView labelView = (TextView)view.findViewById(R.id.page_item_shortcut);
			TextView titleTextView = (TextView)view.findViewById(R.id.page_item_title);
			String sPos = "0";
			String s = "";
			if (arrFound==null) {
				sPos = String.valueOf(position+1);
				s = (String) getItem(position);
			} else {
				sPos = ((String[]) getItem(position))[0];
				s = ((String[]) getItem(position))[1];
			}
			if ( labelView!=null ) {
				labelView.setText(sPos);
			}
			if ( s!=null ) {
				if ( titleTextView!=null ) {
					titleTextView.setText(s);
					if (!StrUtils.isEmptyStr(mFindText))
						Utils.setHighLightedText(titleTextView,mFindText, mColorIconL);
				}
			} else {
				if ( titleTextView!=null )
					titleTextView.setText("");
			}
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mPageCount==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class BookPagesList extends BaseListView {

		public BookPagesList(Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			if (mReaderView.getArrAllPages()==null) mPageCount=0;
				else mPageCount = mReaderView.getArrAllPages().size();
			setAdapter(new BookPagesAdapter());
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
				//openContextMenu(DictList.this);
				return true;
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			String value = "1";
			if (arrFound == null)
				value = String.valueOf(position+1);
			else {
				TextView labelView = view.findViewById(R.id.page_item_shortcut);
				if (labelView != null) value = String.valueOf(Integer.valueOf(labelView.getText().toString().trim())+1);
			}
			try {
				if ( handler.validate(value, false) )
					handler.onOkPage(value);
				else
					handler.onCancel();
			} catch ( Exception e ) {
				handler.onCancel();
			}
			cancel();
			return true;
		}


	}
	
	public GotoPageDialog(BaseActivity activity, final String title, final String prompt, boolean isNumberEdit, int minValue, int maxValue, int currentValue, final GotoPageHandler handler )
	{
		super("GotoPageDialog", activity, title, true, false);
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors((CoolReader) activity, isEInk);
		this.arrFound = null;
		this.handler = handler;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.mReaderView = ((CoolReader)activity).getReaderView();
		this.mReaderView.CheckAllPagesLoadVisual();
        this.mInflater = LayoutInflater.from(getContext());
        ViewGroup layout = (ViewGroup)mInflater.inflate(R.layout.goto_page_dlg, null);
        watcher = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
										  int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
									  int count) {
				if (inTextChange) return;
				int val = 0;
				try {
					val = Integer.parseInt(s.toString());
				} catch (Exception e) {
					val = 0;
				}
				mList.setSelection(val-1);
				int perc = (val * 100)/maxValue;
				inTextChange = true;
				inputPerc.setText("" + perc);
				inTextChange = false;
				inSeekChange = true;
				seekBar.setProgress(val);
				inSeekChange = false;
			}
		};
		input = layout.findViewById(R.id.input_field);
		watcherPerc = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
										  int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
									  int count) {
				if (inTextChange) return;
				int val = 0;
				try {
					val = Integer.parseInt(s.toString());
				} catch (Exception e) {
					val = 0;
				}
				int pageNumber = maxValue * val / 100;
				mList.setSelection(pageNumber-1);
				inTextChange = true;
				input.setText("" + pageNumber);
				inTextChange = false;
				inSeekChange = true;
				seekBar.setProgress(pageNumber);
				inSeekChange = false;
			}
		};
		inputPerc = layout.findViewById(R.id.input_field_perc);
		inputPerc.addTextChangedListener(watcherPerc);
		input.addTextChangedListener(watcher);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
        TextView promptView = layout.findViewById(R.id.lbl_prompt);
        if (promptView != null) {
        	promptView.setText(prompt);
        }
		ViewGroup body = layout.findViewById(R.id.pages_list);
		mList = new GotoPageDialog.BookPagesList(activity, false);
		body.addView(mList);

        seekBar = layout.findViewById(R.id.goto_position_seek_bar);
        if (seekBar != null) {
        	seekBar.setMax(maxValue - minValue);
        	seekBar.setProgress(currentValue);
        	seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if (inSeekChange) return;
					if (fromUser) {
						String value = String.valueOf(progress + GotoPageDialog.this.minValue);
						try {
							if (handler.validate(value, false)) {
								inTextChange = true;
								input.setText(value);
								mList.setSelection(Integer.parseInt(value)-1);
								int perc = (Integer.parseInt(value) * 100)/maxValue;
								inputPerc.setText("" + perc);
								inTextChange = false;
							}
						} catch (Exception e) {
							// ignore
						}
					}
				}
			});
        }
        //input = new EditText(getContext());
        if ( isNumberEdit )
        	input.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
//	        input.getText().setFilters(new InputFilter[] {
//	        	new DigitsKeyListener()        
//	        });
        setView(layout);
	}

	public GotoPageDialog(BaseActivity activity, final String title, final String findtext, final boolean caseSensitive, final GotoPageHandler handler )
	{
		super("GotoPageDialog", activity, title, true, false);
		arrFound = new ArrayList<String[]>();
		mFindText = findtext;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorIconL});
		mColorIconL = a.getColor(0, Color.GRAY);
		a.recycle();
		this.handler = handler;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.mReaderView = ((CoolReader)activity).getReaderView();
		this.mReaderView.CheckAllPagesLoadVisual();
		this.mInflater = LayoutInflater.from(getContext());
		ViewGroup layout = (ViewGroup)mInflater.inflate(R.layout.goto_page_find_dlg, null);
		TextView promptView = (TextView)layout.findViewById(R.id.lbl_find_text_pages);
		if (promptView != null) {
			promptView.setText(activity.getString(R.string.search_results_for)+" "+findtext);
		}
		ViewGroup body = (ViewGroup)layout.findViewById(R.id.find_pages_list);
		mList = new GotoPageDialog.BookPagesList(activity, false);
		body.addView(mList);
		setView(layout);
		int iPageCnt = 0;
		if (mReaderView.getArrAllPages()!=null) iPageCnt = mReaderView.getArrAllPages().size();
		for (int i=0;i<iPageCnt;i++) {
			String sPage = mReaderView.getArrAllPages().get(i);
			if (sPage == null) sPage = "";
			sPage = sPage.replace("\\n", " ");
			sPage = sPage.replace("\\r", " ");
			String[] arrPage = {String.valueOf(i), sPage};
			String sFindText = findtext;
			if (sFindText == null) sFindText = "";
			if (sFindText.equals("")) {
				arrFound.add(arrPage);
			} else {
				if ((sPage.toLowerCase().contains(sFindText.toLowerCase())) && (!caseSensitive)) arrFound.add(arrPage);
				if ((sPage.contains(sFindText)) && (caseSensitive)) arrFound.add(arrPage);
			}
		}
	}

	@Override
	protected void onNegativeButtonClick() {
        cancel();
        handler.onCancel();
	}

	@Override
	protected void onPositiveButtonClick() {
		if (input != null)
			if (input.getText() != null) {
				String value = input.getText().toString().trim();
				try {
					if (handler.validate(value, false)) {
						handler.onOk(value, false);
					} else
						handler.onCancel();
				} catch (Exception e) {
					handler.onCancel();
				}
				cancel();
			}
	}
}
