package org.coolreader.crengine;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CRDBService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

public class UserDicDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private UserDicList mList;
	private int openPage = 0;
	final TextView rb_descr;
	final TableRow tr_descr;
	final ImageButton btnUserDic;
	final ImageButton btnCitation;
	final ImageButton btnDicSearchHistory;
	final RadioButton btnPage;
	final RadioButton btnBook;
	final RadioButton btnAll;
	final ImageButton searchButton;
	final EditText selEdit;

	private ArrayList<UserDicEntry> mUserDic = new ArrayList<UserDicEntry>();
	private ArrayList<DicSearchHistoryEntry> mDicSearchHistory = new ArrayList<DicSearchHistoryEntry>();

	public final static int ITEM_POSITION=0;

	private void listUpdated() {
		mList.updateAdapter(new UserDicAdapter());
	}

	class UserDicAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			int cnt = 0;
			if ((openPage==0)||(openPage==1))
				if (mUserDic != null) cnt = mUserDic.size();
			if ((openPage==2))
				if (mDicSearchHistory != null) cnt = mDicSearchHistory.size();
			return cnt;
		}

		public Object getItem(int position) {
			int cnt = 0;
			if ((openPage==0)||(openPage==1)) {
				if (mUserDic != null) cnt = mUserDic.size();
				if (position < 0 || position >= cnt)
					return null;
				return mUserDic.get(position);
			}
			if ((openPage==2)) {
				if (mDicSearchHistory != null) cnt = mDicSearchHistory.size();
				if (position < 0 || position >= cnt)
					return null;
				return mDicSearchHistory.get(position);
			}
			return null;
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.userdic_item;
			view = mInflater.inflate(res, null);
			TextView wordView = (TextView)view.findViewById(R.id.userdic_word);
			TextView wordTranslateView = (TextView)view.findViewById(R.id.userdic_word_translate);
			if ((openPage==0)||(openPage==1)) {
				UserDicEntry ude = null;
				if (mUserDic != null) ude = mUserDic.get(position);
				if (wordView != null) {
					wordView.setText(String.valueOf(position + 1));
				}
				String sTitle = "";
				if (ude != null) {
					String word = StrUtils.textShrinkLines(ude.getDic_word(), true);
					if (!StrUtils.isEmptyStr(ude.getLanguage()))
						word = word + " [" + ude.getLanguage() + "]";
					if (wordView != null)
						wordView.setText(word);
					wordTranslateView.setText(StrUtils.textShrinkLines(ude.getDic_word_translate(), true));
				} else {
					if (wordView != null)
						wordView.setText("");
					wordTranslateView.setText("");
				}
			}
			if ((openPage==2)) {
				DicSearchHistoryEntry dshe = null;
				if (mDicSearchHistory != null) dshe = mDicSearchHistory.get(position);
				if (wordView != null) {
					wordView.setText(String.valueOf(position + 1));
				}
				String sTitle = "";
				if (dshe != null) {
					String word = StrUtils.textShrinkLines(dshe.getSearch_text(), true);
					String sLangFrom = dshe.getLanguage_from();
					String sLangTo = dshe.getLanguage_to();
					String sLang = "";
					if ((!StrUtils.isEmptyStr(sLangFrom))||(!StrUtils.isEmptyStr(sLangTo))) {
						if (StrUtils.isEmptyStr(sLangFrom)) sLangFrom = "any";
						if (StrUtils.isEmptyStr(sLangTo)) sLangTo = "any";
						sLang = "[" +sLangFrom+" > "+sLangTo + "] ";
					}
					if (wordView != null)
						wordView.setText(word);
					sLang = sLang + StrUtils.textShrinkLines(dshe.getText_translate(), true);
					if (!sLang.trim().equals("")) sLang = sLang +"; ";
							wordTranslateView.setText(sLang+
							mCoolReader.getString(R.string.txt_seen)+ " "+String.valueOf(dshe.getSeen_count()));
				} else {
					if (wordView != null)
						wordView.setText("");
					wordTranslateView.setText("");
				}
			}
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			int cnt = 0;
			if ((openPage==0)||(openPage==1)) {
				if (mUserDic != null) cnt = mUserDic.size();
			}
			if ((openPage==2)) {
				if (mDicSearchHistory != null) cnt = mDicSearchHistory.size();
			}
			return cnt==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class UserDicList extends BaseListView {

		private ListAdapter mAdapter;
		private UserDicDlg uDDlg;

		public UserDicList( Context context, UserDicDlg udd) {
			super(context, true);
			uDDlg = udd;
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new UserDicAdapter());
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					if ((openPage==0)||(openPage==1)) {
						if (mUserDic == null)
							return true;
						final UserDicEntry ude = mUserDic.get(position);
						final UserDicDlg thisDlg = uDDlg;
						mCoolReader.askConfirmation(R.string.win_title_confirm_ude_delete, new Runnable() {
							@Override
							public void run() {
								activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_DELETE);
								mCoolReader.getmUserDic().remove(ude.getIs_citation() + ude.getDic_word());
								mUserDic.remove(ude);
								if (thisDlg != null)
									thisDlg.listUpdated();
								mCoolReader.getmReaderFrame().getUserDicPanel().updateUserDicWords();
							}
						});
					}
					if ((openPage==2)) {
						if (mCoolReader.getReaderView()==null) return false;
						final DicSearchHistoryEntry dshe = mDicSearchHistory.get(position);
						DictsDlg dlg = new DictsDlg(mCoolReader, mCoolReader.getReaderView(), dshe.getSearch_text());
						dlg.show();
					}
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			if (mCoolReader.getReaderView()==null) return false;
			if (openPage==0) {
				openContextMenu(UserDicList.this);
				DictsDlg dlg = new DictsDlg(mCoolReader, mCoolReader.getReaderView(), mUserDic.get(position).getDic_word());
				dlg.show();
				dismiss();
			}
			if (openPage==1) {
				mCoolReader.getReaderView().copyToClipboard(
					mUserDic.get(position).getDic_word()+" \n"+
						mUserDic.get(position).getDic_word_translate()
				);
			}
			if (openPage==2) {
				mCoolReader.findInDictionary( mDicSearchHistory.get(position).getSearch_text());
			}
			return true;
		}

		public void updateAdapter( UserDicAdapter adapter ) {
			mAdapter = adapter;
			setAdapter(mAdapter);
		}

	}

	private void setChecked(ImageButton btn) {
		rb_descr.setText(btn.getContentDescription()+" ");
		btnPage.setEnabled(true);
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_user_dic))) {
			openPage = 0;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_citation))) {
			if (openPage == 0) {
				if (btnPage.isChecked()) {
					btnBook.setChecked(true);
				}
			}
			btnPage.setEnabled(false);
			openPage = 1;
		}

		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_button_dic_search_hist))) {
			if (openPage == 0) {
				if (btnPage.isChecked()) {
					btnBook.setChecked(true);
				}
			}
			btnPage.setEnabled(false);
			openPage = 2;
		}
		int colorGray;
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
        int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
        rb_descr.setBackgroundColor(colorGrayCT);
		//tr_descr.setBackgroundColor(colorGrayC);
		btnUserDic.setBackgroundColor(colorGrayCT);
		btnCitation.setBackgroundColor(colorGrayCT);
		btnDicSearchHistory.setBackgroundColor(colorGrayCT);
		btn.setBackgroundColor(colorGray);
	}

	private boolean getChecked(ImageButton btn) {
		if (btn.getContentDescription().equals(R.string.dlg_bookmark_user_dic)) {
			return openPage == 0;
		}
		if (btn.getContentDescription().equals(R.string.dlg_bookmark_citation)) {
			return openPage == 1;
		}
		if (btn.getContentDescription().equals(R.string.dlg_button_dic_search_hist)) {
			return openPage == 2;
		}
		return false;
	}

	private void checkedCallback(RadioButton btn) {
		boolean bPageC = btnPage.isChecked();
		boolean bBookC = btnBook.isChecked();
		boolean bAllC = btnAll.isChecked();
		if (btn!=null) {
			bPageC = (btn.equals(btnPage));
			bBookC = (btn.equals(btnBook));
			bAllC = (btn.equals(btnAll));
		}
		if (bPageC) {
			if ((openPage==0)||(openPage==1)) {
				mUserDic.clear();
				for (UserDicEntry ude : mCoolReader.getmReaderFrame().getUserDicPanel().getArrUdeWords()) {
					if (ude.getIs_citation() == openPage)
						if (
								(selEdit.getText().toString().trim().equals("")) ||
										(
												(ude.getDic_word().toLowerCase().contains(selEdit.getText().toString().toLowerCase().trim())) ||
														(ude.getDic_word_translate().toLowerCase().contains(selEdit.getText().toString().toLowerCase().trim()))
										)
								)
							mUserDic.add(ude);
				}
			}
		}
		if (bBookC) {
			final String sBookFName = mCoolReader.getReaderView().getBookInfo().getFileInfo().filename;
			CRC32 crc = new CRC32();
			crc.update(sBookFName.getBytes());
			String sCRC = String.valueOf(crc.getValue());
			if ((openPage==0)||(openPage==1)) updUserDic(sCRC);
			if ((openPage==2)) updDicSearchHistory(sCRC);

		}
		if (bAllC) {
			if ((openPage==0)||(openPage==1)) updUserDic("");
			if ((openPage==2)) updDicSearchHistory("");
		}
	}

	public UserDicDlg(final CoolReader activity, final int openPage)
	{
		super("UserDicDlg", activity, activity.getResources().getString(R.string.win_title_user_dic), false, true);
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mUserDic.clear();
		for (UserDicEntry ude: activity.getmReaderFrame().getUserDicPanel().getArrUdeWords()) {
			if (ude.getIs_citation()==openPage)
				mUserDic.add(ude);
			//mCoolReader.showToast(ude.getDic_word()+" "+ude.getIs_citation());
		}
		View frame = mInflater.inflate(R.layout.userdic_list_dialog, null);
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.userdic_list);
		mList = new UserDicList(activity, this);
		btnPage = (RadioButton)frame.findViewById(R.id.rb_page);
		btnBook = (RadioButton)frame.findViewById(R.id.rb_book);
		btnAll = (RadioButton)frame.findViewById(R.id.rb_userdic_all);
		btnUserDic = (ImageButton)frame.findViewById(R.id.rb_user_dic);
		btnCitation = (ImageButton)frame.findViewById(R.id.rb_citation);
		btnDicSearchHistory = (ImageButton)frame.findViewById(R.id.rb_dic_search_history);
		ImageButton btnFake = (ImageButton)frame.findViewById(R.id.btn_fake);
		rb_descr = (TextView)frame.findViewById(R.id.lbl_rb_descr);
		tr_descr = (TableRow)frame.findViewById(R.id.tr_rb_descr);
		if (openPage==0) setChecked(btnUserDic);
		if (openPage==1) setChecked(btnCitation);
		if (openPage==2) setChecked(btnDicSearchHistory);
		body.addView(mList);
		setView(frame);
		searchButton = (ImageButton) frame.findViewById(R.id.btn_search);
		selEdit = (EditText) frame.findViewById(R.id.search_text);
		selEdit.clearFocus();
		btnFake.requestFocus();
		setFlingHandlers(mList, null, null);

		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				checkedCallback(null);
			}
		});

		btnPage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					checkedCallback(btnPage);
				}
			}
		});
		btnBook.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					checkedCallback(btnBook);
				}
			}
		});
		btnAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					checkedCallback(btnAll);
				}
			}
		});

		btnUserDic.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setChecked(btnUserDic);
				checkedCallback(null);
			}
		});

		btnCitation.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setChecked(btnCitation);
				checkedCallback(null);
			}
		});
		btnDicSearchHistory.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setChecked(btnDicSearchHistory);
				checkedCallback(null);
			}
		});
		searchButton.requestFocus();
	}

	private void updUserDic(String sCRC) {
		mUserDic.clear();
		Iterator it = mCoolReader.getmUserDic().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			UserDicEntry ude = (UserDicEntry)pair.getValue();
			if (((ude.getDic_from_book().equals(sCRC))||(sCRC.equals(""))) &&
				(openPage==ude.getIs_citation()))
				if (
						(selEdit.getText().toString().trim().equals("")) ||
								(
									(ude.getDic_word().toLowerCase().contains(selEdit.getText().toString().toLowerCase().trim())) ||
									(ude.getDic_word_translate().toLowerCase().contains(selEdit.getText().toString().toLowerCase().trim()))
								)
						)
					mUserDic.add(ude);
			//mCoolReader.showToast(ude.getDic_word()+" "+ude.getIs_citation());
		}
		Collections.sort(mUserDic, new Comparator<UserDicEntry>() {
			@Override
			public int compare(UserDicEntry lhs, UserDicEntry rhs) {
				// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
				return lhs.getDic_word().compareToIgnoreCase(rhs.getDic_word());
			}
		});
		listUpdated();
	}

	private void updDicSearchHistory(final String sCRC) {
		mDicSearchHistory.clear();
		if (activity instanceof CoolReader) {
			CoolReader cr = (CoolReader)activity;
			cr.getDB().loadDicSearchHistory(new CRDBService.DicSearchHistoryLoadingCallback() {
				@Override
				public void onDicSearchHistoryLoaded(List<DicSearchHistoryEntry> list) {
					ArrayList<DicSearchHistoryEntry> list1 = (ArrayList<DicSearchHistoryEntry>) list;
					for (DicSearchHistoryEntry dshe: list1) {
						if ((dshe.getSearch_from_book().equals(sCRC))||(sCRC.equals("")))
							if (
									(selEdit.getText().toString().trim().equals("")) ||
											(
													(dshe.getSearch_text().toLowerCase().contains(selEdit.getText().toString().toLowerCase().trim())) ||
															(dshe.getText_translate().toLowerCase().contains(selEdit.getText().toString().toLowerCase().trim()))
											)
									)
								mDicSearchHistory.add(dshe);
					};
					listUpdated();
				}
			});
		}
	}

	@Override
	protected void onPositiveButtonClick() {
		// add Dict
		UserDicDlg.this.dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		UserDicDlg.this.dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating UserDicDlg");
		setTitle(mCoolReader.getResources().getString(R.string.win_title_user_dic));
		setCancelable(true);
		super.onCreate(savedInstanceState);
		//registerForContextMenu(mList);
	}

}
