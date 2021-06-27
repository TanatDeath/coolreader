package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.coolreader.CoolReader;
import org.coolreader.dic.Dictionaries;
import org.coolreader.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class DictsDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private ReaderView mReaderView;
	private View  mCallerView;
	private LayoutInflater mInflater;
	private DictList mList;
	private String mSearchText;
	private EditText selEdit;

	List<Dictionaries.DictInfo> dictInfoList;

	public void fillDictListInfo() {
		dictInfoList = new ArrayList<>();
		for (final Dictionaries.DictInfo di: mCoolReader.mDictionaries.diRecent) {
			dictInfoList.add(di);
		}
		for (final Dictionaries.DictInfo di: mCoolReader.mDictionaries.getAddDicts()) {
			if (!dictInfoList.contains(di)) dictInfoList.add(di);
		}
		for (final Dictionaries.DictInfo di: Dictionaries.getDictListExt(mCoolReader,true)) {
			if (!dictInfoList.contains(di)) dictInfoList.add(di);
		}
	}

	//DictsDlg mThis;

	public final static int ITEM_POSITION=0;

	class DictListAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			if (dictInfoList == null) fillDictListInfo();
			return dictInfoList.size();
		}

		public Object getItem(int position) {
			if (dictInfoList == null) fillDictListInfo();
			if ( position<0 || position>=dictInfoList.size() )
				return null;
			return dictInfoList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
		
		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public int getViewTypeCount() {
			return 4;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.dict_item;
			view = mInflater.inflate(res, null);
			TextView labelView = (TextView)view.findViewById(R.id.dict_item_shortcut);
			TextView titleTextView = (TextView)view.findViewById(R.id.dict_item_title);
			ImageView ivIcon = (ImageView)view.findViewById(R.id.dict_icon);

			Dictionaries.DictInfo b = (Dictionaries.DictInfo)getItem(position);
			if ( labelView!=null ) {
				labelView.setText(String.valueOf(position+1));
			}
			if ( b!=null ) {
				if ( titleTextView!=null ) {
					String sAdd = b.getAddText(mCoolReader);
					if (StrUtils.isEmptyStr(sAdd))
						titleTextView.setText(b.name);
					else
						titleTextView.setText(b.name + ": " + sAdd);
				}
				if (b.dicIcon!=0)
					ivIcon.setImageDrawable(mCoolReader.getResources().getDrawable(b.dicIcon));
				if (b.icon != null) {
					Drawable fakeIcon = mCoolReader.getResources().getDrawable(R.drawable.lingvo);
					final Bitmap bmp = Bitmap.createBitmap(b.icon.getIntrinsicWidth(), b.icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
					final Canvas canvas = new Canvas(bmp);
					b.icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
					b.icon.draw(canvas);
					Bitmap resizedBitmap = Bitmap.createScaledBitmap(
							bmp, fakeIcon.getIntrinsicWidth(), fakeIcon.getIntrinsicHeight(), false);
					ivIcon.setImageBitmap(resizedBitmap);
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
			return Dictionaries.getDictList(mCoolReader).size()==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();
		
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}
	
	class DictList extends BaseListView {

		public DictList(Context context) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new DictListAdapter());
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
				openContextMenu(DictList.this);
				return true;
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			if (dictInfoList == null) fillDictListInfo();
			mCoolReader.mDictionaries.setAdHocDict(dictInfoList.get(position));
			String sSText = mSearchText.trim();
			if (selEdit!=null) sSText = selEdit.getText().toString().trim();
			mCoolReader.findInDictionary(sSText, mCallerView);
			if (!mReaderView.getSettings().getBool(mReaderView.PROP_APP_SELECTION_PERSIST, false))
				mReaderView.clearSelection();
			dismiss();
			return true;
		}
		
		
	}

	public DictsDlg( CoolReader activity, ReaderView readerView, String search_text, View view )
	{
		super("DictsDlg", activity, activity.getResources().getString(R.string.win_title_dicts), true, true);
		mInflater = LayoutInflater.from(getContext());
		mSearchText = StrUtils.updateText(search_text.trim(),false);
		mCoolReader = activity;
		mReaderView = readerView;
		mCallerView = view;
		View frame = mInflater.inflate(R.layout.dict_dialog, null);
		ImageButton btnMinus1 = frame.findViewById(R.id.dict_dlg_minus1_btn);
		ImageButton btnMinus2 = frame.findViewById(R.id.dict_dlg_minus2_btn);
		selEdit = (EditText)frame.findViewById(R.id.selection_text);
		selEdit.setText(mSearchText);
		setPositiveButtonImage(0,0);
		setThirdButtonImage(
				//R.drawable.icons8_search_history,
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_search_history, R.drawable.icons8_search_history),
				R.string.dlg_button_search_hist);
		setAddButtonImage(
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_me_smb, R.drawable.icons8_me_smb),
				//R.drawable.icons8_me_smb,
				R.string.dlg_button_pronoun_replace);
		btnMinus1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				String s = StrUtils.replacePuncts(selEdit.getText().toString(),true);
				String res = "";
				String [] arrS = s.split(" ");
				boolean bFirst = true;
				for (String ss: arrS) {
					String repl = StrUtils.replacePuncts(ss.trim().toLowerCase(), false);
					if (!repl.trim().equals("")) {
						if (bFirst) {
							bFirst = false;
						} else {
							res=res.trim()+" "+ss.trim();
						}
					}
				}
				selEdit.setText(res.trim());
			}
		});
		btnMinus2.setOnClickListener(v -> {
			String s = StrUtils.replacePuncts(selEdit.getText().toString(),true);
			String res = "";
			String [] arrS = s.split(" ");
			List<String> list = Arrays.asList(arrS);
			Collections.reverse(list);
			arrS = (String[]) list.toArray();
			boolean bFirst = true;
			for (String ss: arrS) {
				String repl = StrUtils.replacePuncts(ss.trim().toLowerCase(),false);
				if (!repl.trim().equals("")) {
					if (bFirst) {
						bFirst = false;
					} else {
						res=ss.trim()+" "+res.trim();
					}
				}
			}
			selEdit.setText(res.trim());
		});
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.dict_list);
		mList = new DictList(activity);
		body.addView(mList);
		setView(frame);
		selEdit.clearFocus();
		btnMinus2.requestFocus();
		setFlingHandlers(mList, null, null);
	}

	@Override
	protected void onPositiveButtonClick() {
		// add Dict
		DictsDlg.this.dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		DictsDlg.this.dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating DictsDlg");
		//setTitle(mCoolReader.getResources().getString(R.string.win_title_Dicts));
		setCancelable(true);
		super.onCreate(savedInstanceState);
		registerForContextMenu(mList);
	}

	@Override
	protected void onAddButtonClick() {
		String s = StrUtils.updateDictSelText(selEdit.getText().toString());
		selEdit.setText(s.trim());
	}

	protected void onThirdButtonClick() {
		UserDicDlg dlg = new UserDicDlg(mCoolReader,2);
		dlg.show();
	}

}
