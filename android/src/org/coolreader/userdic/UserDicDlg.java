package org.coolreader.userdic;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DicSearchHistoryEntry;
import org.coolreader.crengine.DictsDlg;
import org.coolreader.crengine.StrUtils;
import org.coolreader.crengine.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
	final Button btnPage2;
	final Button btnBook2;
	final Button btnAll2;
	final ImageButton searchButton;
	final EditText selEdit;
	HashMap<Integer, Integer> themeColors;
	boolean isEInk = false;

	private ArrayList<UserDicEntry> mUserDic = new ArrayList<>();
	public static ArrayList<DicSearchHistoryEntry> mDicSearchHistoryAll = new ArrayList<>();
	private ArrayList<DicSearchHistoryEntry> mDicSearchHistory = new ArrayList<>();

	public final static int ITEM_POSITION=0;

	void listUpdated() {
		mList.updateAdapter(new UserDicAdapter());
	}

	private void paintScopeButtons() {
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mCoolReader.tintViewIcons(btnPage2, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(btnBook2, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(btnAll2, PorterDuff.Mode.CLEAR,true);
		if (getCheckedFromTag(btnPage2.getTag())) {
			btnPage2.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnPage2,true);
		} else btnPage2.setBackgroundColor(colorGrayCT);
		if (getCheckedFromTag(btnBook2.getTag())) {
			btnBook2.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnBook2,true);
		} else btnBook2.setBackgroundColor(colorGrayCT);
		if (getCheckedFromTag(btnAll2.getTag())) {
			btnAll2.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnAll2,true);
		} else btnAll2.setBackgroundColor(colorGrayCT);
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
			TextView wordView = view.findViewById(R.id.userdic_word);
			TextView wordTranslateView = view.findViewById(R.id.userdic_word_translate);
			ImageView userdicDel = view.findViewById(R.id.userdic_value_del);
			userdicDel.setOnClickListener(v -> {
				if ((openPage==0)||(openPage==1)) {
					if (mUserDic == null) return;
					final UserDicEntry ude = mUserDic.get(position);
					mCoolReader.askConfirmation(R.string.win_title_confirm_ude_delete, () -> {
						if (ude.getThisIsDSHE()) {
							DicSearchHistoryEntry dshe = new DicSearchHistoryEntry();
							dshe.setSearch_text(ude.getDic_word());
							mCoolReader.getDB().updateDicSearchHistory(dshe, DicSearchHistoryEntry.ACTION_DELETE, mCoolReader);
						} else
							mCoolReader.getDB().saveUserDic(ude, UserDicEntry.ACTION_DELETE);
						mCoolReader.getmUserDic().remove(ude.getIs_citation() + ude.getDic_word());
						mUserDic.remove(ude);
						listUpdated();
						mCoolReader.updateUserDicWords();
					});
				}
				if (openPage==2) {
					final DicSearchHistoryEntry dshe = mDicSearchHistory.get(position);
					mCoolReader.askConfirmation(R.string.win_title_confirm_ude_delete, () -> {
						mCoolReader.getDB().updateDicSearchHistory(dshe, DicSearchHistoryEntry.ACTION_DELETE, mCoolReader);
						mDicSearchHistory.remove(dshe);
						listUpdated();
						mCoolReader.updateUserDicWords();
					});
				}
			});
			mCoolReader.tintViewIcons(view,true);
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
					if (wordView != null) {
						wordView.setText(word);
						if (ude.getThisIsDSHE()) wordView.setTypeface(null, Typeface.BOLD_ITALIC);
						else wordView.setTypeface(null, Typeface.BOLD);
					}
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
						if (StrUtils.isEmptyStr(sLangFrom)) sLangFrom = mCoolReader.getString(R.string.txt_any);
						if (StrUtils.isEmptyStr(sLangTo)) sLangTo = mCoolReader.getString(R.string.txt_any);
						sLang = "[" +sLangFrom+" > "+sLangTo + "] ";
					}
					if (wordView != null)
						wordView.setText(word);
					sLang = sLang + StrUtils.textShrinkLines(dshe.getText_translate(), true);
					if (!sLang.trim().equals("")) sLang = sLang +"; ";
							wordTranslateView.setText(sLang+
							mCoolReader.getString(R.string.txt_seen)+ " "+ dshe.getSeen_count());
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
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
				if (openPage==0) {
					if (mUserDic == null)
						return true;
					final UserDicEntry ude = mUserDic.get(position);
					UserDicEditDialog uded = new UserDicEditDialog(mCoolReader, ude, uDDlg);
					uded.show();
				}
				if ((openPage==1)) {
					if (mCoolReader.getReaderView()==null) return false;
					final UserDicEntry ude = mUserDic.get(position);
					UserDicEditDialog uded = new UserDicEditDialog(mCoolReader, ude, uDDlg);
					uded.show();
				}
				if ((openPage==2)) {
					if (mCoolReader.getReaderView()==null) return false;
					final DicSearchHistoryEntry dshe = mDicSearchHistory.get(position);
					UserDicEntry ude = new UserDicEntry();
					ude.setDic_word(dshe.getSearch_text());
					ude.setDic_word_translate(dshe.getText_translate());
					ude.setCreate_time(dshe.getCreate_time());
					ude.setLast_access_time(dshe.getLast_access_time());
					ude.setDic_from_book(dshe.getSearch_from_book());
					ude.setLanguage(dshe.getLanguage_from());
					ude.setSeen_count(dshe.getSeen_count());
					ude.setThisIsDSHE(true);
					UserDicEditDialog uded = new UserDicEditDialog(mCoolReader, ude, uDDlg);
					uded.show();
				}
				return true;
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			if (mCoolReader.getReaderView()==null) return false;
			if (openPage==0) {
				openContextMenu(UserDicList.this);
				DictsDlg dlg = new DictsDlg(mCoolReader, mCoolReader.getReaderView(), mUserDic.get(position).getDic_word(), null, false);
				dlg.show();
				dismiss();
			}
			if (openPage==1) {
				mCoolReader.getReaderView().copyToClipboardAndToast(
					mUserDic.get(position).getDic_word()+" \n"+
						mUserDic.get(position).getDic_word_translate()
				);
			}
			if (openPage==2) {
				mCoolReader.findInDictionary( mDicSearchHistory.get(position).getSearch_text() , view);
			}
			return true;
		}

		public void updateAdapter( UserDicAdapter adapter ) {
			mAdapter = adapter;
			setAdapter(mAdapter);
		}

	}

	private boolean getCheckedFromTag(Object o) {
		if (o == null) return false;
		if (!(o instanceof String)) return false;
		if (o.equals("1")) return true;
		return false;
	}

	private void setCheckedTag(Button b) {
		if (b == null) return;
		btnPage2.setTag("0");
		btnBook2.setTag("0");
		btnAll2.setTag("0");
		b.setTag("1");
	}

	private void setChecked(ImageButton btn) {
		int colorIcon = themeColors.get(R.attr.colorIcon);
		int colorIconL = themeColors.get(R.attr.colorIconL);
		int colorGray = themeColors.get(R.attr.colorThemeGray2);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		rb_descr.setText(btn.getContentDescription()+" ");
		btnPage2.setEnabled(true);
		btnPage2.setTextColor(activity.getTextColor(colorIcon));
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_user_dic))) {
			openPage = 0;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_citation))) {
			if (openPage == 0) {
				if (getCheckedFromTag(btnPage2.getTag())) {
					setCheckedTag(btnBook2);
				}
			}
			btnPage2.setEnabled(false);
			btnPage2.setTextColor(colorIconL);
			openPage = 1;
		}

		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_button_dic_search_hist))) {
			if (openPage == 0) {
				if (getCheckedFromTag(btnPage2.getTag())) {
					setCheckedTag(btnBook2);
				}
			}
			btnPage2.setEnabled(false);
			btnPage2.setTextColor(colorIconL);
			openPage = 2;
		}
		int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		rb_descr.setBackgroundColor(colorGrayCT);
		//tr_descr.setBackgroundColor(colorGrayC);
		btnUserDic.setBackgroundColor(colorGrayCT);
		btnCitation.setBackgroundColor(colorGrayCT);
		btnDicSearchHistory.setBackgroundColor(colorGrayCT);
		btn.setBackgroundColor(colorGray);
		paintScopeButtons();
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

	void checkedCallback(Button btn) {
		boolean bPageC = getCheckedFromTag(btnPage2.getTag());
		boolean bBookC = getCheckedFromTag(btnBook2.getTag());
		boolean bAllC = getCheckedFromTag(btnAll2.getTag());
		if (btn!=null) {
			bPageC = (btn.equals(btnPage2));
			bBookC = (btn.equals(btnBook2));
			bAllC = (btn.equals(btnAll2));
			setCheckedTag(btn);
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
				listUpdated();
			}
		}
		if (bBookC) {
			final String sBookFName = mCoolReader.getReaderView().getBookInfo().getFileInfo().getFilename();
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
		paintScopeButtons();
	}

	public UserDicDlg(final CoolReader activity, final int openPage)
	{
		super("UserDicDlg", activity, activity.getResources().getString(R.string.win_title_user_dic), false, true);
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(activity, isEInk);
		mUserDic.clear();
		for (UserDicEntry ude: activity.getmReaderFrame().getUserDicPanel().getArrUdeWords()) {
			if (ude.getIs_citation()==openPage)
				mUserDic.add(ude);
			//mCoolReader.showToast(ude.getDic_word()+" "+ude.getIs_citation());
		}
		View frame = mInflater.inflate(R.layout.userdic_list_dialog, null);
		ViewGroup body = frame.findViewById(R.id.userdic_list);
		mList = new UserDicList(activity, this);
		btnPage2 = frame.findViewById(R.id.rb_page2);
		btnBook2 = frame.findViewById(R.id.rb_book2);
		btnAll2 = frame.findViewById(R.id.rb_userdic_all2);
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		Drawable img3 = img.getConstantState().newDrawable().mutate();
		btnPage2.setTag("1");
		btnPage2.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		btnBook2.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		btnAll2.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
		btnUserDic = frame.findViewById(R.id.rb_user_dic);
		btnCitation = frame.findViewById(R.id.rb_citation);
		btnDicSearchHistory = frame.findViewById(R.id.rb_dic_search_history);
		ImageButton btnFake = frame.findViewById(R.id.btn_fake);
		rb_descr = frame.findViewById(R.id.lbl_rb_descr);
		tr_descr = frame.findViewById(R.id.tr_rb_descr);
		if (openPage==0) setChecked(btnUserDic);
		if (openPage==1) setChecked(btnCitation);
		if (openPage==2) setChecked(btnDicSearchHistory);
		body.addView(mList);
		setView(frame);
		searchButton = frame.findViewById(R.id.btn_search);
		selEdit = frame.findViewById(R.id.search_text);
		selEdit.clearFocus();
		btnFake.requestFocus();
		setFlingHandlers(mList, null, null);

		btnPage2.setOnClickListener(v -> checkedCallback(btnPage2));
		btnBook2.setOnClickListener(v -> checkedCallback(btnBook2));
		btnAll2.setOnClickListener(v -> checkedCallback(btnAll2));

		searchButton.setOnClickListener(v -> checkedCallback(null));

		btnUserDic.setOnClickListener(v -> {
			setChecked(btnUserDic);
			checkedCallback(null);
		});

		btnCitation.setOnClickListener(v -> {
			setChecked(btnCitation);
			checkedCallback(null);
		});
		btnDicSearchHistory.setOnClickListener(v -> {
			setChecked(btnDicSearchHistory);
			checkedCallback(null);
		});
		checkedCallback(null);
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
		}
		Collections.sort(mUserDic, (lhs, rhs) -> {
			// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
			// here we need alphabetical sort!!! - not for page
			return lhs.getDic_word().compareToIgnoreCase(rhs.getDic_word());
			//if (lhs.getLast_access_time() > rhs.getLast_access_time()) return -1;
			//if (lhs.getLast_access_time() < rhs.getLast_access_time()) return 1;
			//return 0;
		});
		listUpdated();
	}

	private void updDicSearchHistory(final String sCRC) {
		mDicSearchHistory.clear();
		if (mCoolReader instanceof CoolReader) {
			CoolReader cr = mCoolReader;
			cr.getDB().loadDicSearchHistory(list -> {
				ArrayList<DicSearchHistoryEntry> list1 = (ArrayList<DicSearchHistoryEntry>) list;
				for (DicSearchHistoryEntry dshe: list1) {
					if ((dshe.getSearch_from_book().equals(sCRC)) || (sCRC.equals("")))
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
			});
		}
	}

	public static void updDicSearchHistoryAll(BaseActivity act) {
		mDicSearchHistoryAll.clear();
		if (act instanceof CoolReader) {
			CoolReader cr = (CoolReader)act;
			cr.getDB().loadDicSearchHistory(list -> {
				ArrayList<DicSearchHistoryEntry> list1 = (ArrayList<DicSearchHistoryEntry>) list;
				for (DicSearchHistoryEntry dshe: list1) {
					mDicSearchHistoryAll.add(dshe);
				};
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
