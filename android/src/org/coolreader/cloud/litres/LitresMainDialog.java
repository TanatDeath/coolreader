package org.coolreader.cloud.litres;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.FileInfo;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

public class LitresMainDialog extends BaseDialog {

	public static int VT_SEARCH_BOOKS = 1;
	public static int VT_SEARCH_PERSONS = 2;
	public static int VT_SEARCH_PERSONS_EXT = 3;
	public static int VT_SEARCH_COLLECTIONS = 4;
	public static int VT_SEARCH_GENRES = 5;
	public static int VT_SEARCH_SEQUENCES = 6;

	public static int SM_CONTAINS = 1;
	public static int SM_STARTSWITH = 2;
	public static int SM_STRICT = 3;

	public static int PM_ONLY_AUTHORS = 1;
	public static int PM_ANY_PERSON = 2;

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	final TableLayout tlLitresMain;
	final TableRow trLitresAuth;
	final Button mBtnAuth;
	final public Button mBtnProfile;
	final Button mBtnAddCredit;
	final TableRow trLitresProfile;
	final Button mBtnMyBooks;
	final TableRow trLitresMyBooks;
	final TableRow trLitresSearch1;
	final Button mBtnSearchText;
	Drawable mImgSearchCollapsed;
	Drawable mImgSearchExpanded;
	final TableRow trLitresSearch2;
	public final EditText mSearchText;
	final TableRow trLitresSearch3;
	final Button mBtnContains;
	final Button mBtnStartsWith;
	final Button mBtnStrict;
	final TableRow trLitresSearchBtn;
	final Button mBtnSearch;
	final Button mBtnSearch2;
	final TableRow trLitresSearchBtnGenres;
	final Button mBtnSearchBtnGenres;
	final Button mBtnSearchBtnGenresAll;
	final TableRow trLitresSearchCollections;
	final Button mBtnSearchCollections;
	final TableRow trLitresSearchGenres;
	final Button mBtnSearchGenres;
	final TableRow trLitresSearchPersons;
	final TableRow trLitresSearchPersonsFioLabels;
	final TableRow trLitresSearchPersonsFio;
	final TableRow trLitresSearchPersonsMod;
	final Button mBtnSearchPersonsModOnlyAuthors;
	final Button mBtnSearchPersonsModAnyPerson;
	public final EditText mSearchLastName;
	public final EditText mSearchFirstName;
	public final EditText mSearchMiddleName;
	final Button mBtnSearchPersons;
	final TableRow trLitresSearchPersonsExt;
	final Button mBtnSearchPersonsExt;
	final TableRow trLitresSearchSequences;
	final Button mBtnSearchSequences;
	Drawable mImgSearchCollectionsCollapsed;
	Drawable mImgSearchCollectionsExpanded;
	Drawable mImgSearchGenresCollapsed;
	Drawable mImgSearchGenresExpanded;
	Drawable mImgSearchPersonsCollapsed;
	Drawable mImgSearchPersonsExpanded;
	Drawable mImgSearchPersonsExtCollapsed;
	Drawable mImgSearchPersonsExtExpanded;
	Drawable mImgSearchSequencesCollapsed;
	Drawable mImgSearchSequencesExpanded;
	private int curViewType = VT_SEARCH_BOOKS;
	private int curFindMode = SM_CONTAINS;
	private int curPersonMode = PM_ONLY_AUTHORS;

	public void updateBalance() {
		if (LitresConfig.litresAccountInfo == null) return;
		if (mBtnProfile == null) return;
		if (LitresConfig.litresAccountInfo.needRefresh) return;
		if (!StrUtils.isEmptyStr(LitresConfig.litresAccountInfo.moneyDetails.get("real_money"))) {
			mBtnProfile.setText(LitresConfig.litresAccountInfo.moneyDetails.get("real_money") + " " + LitresConfig.currency + " (bonus " +
					LitresConfig.litresAccountInfo.moneyDetails.get("bonus") + " " + LitresConfig.currency + ")"
			);
		}
	}

	public void setupView(int iViewType) {
		tlLitresMain.removeAllViews();
		if (!LitresConfig.didLogin)
			tlLitresMain.addView(trLitresAuth);
		if (iViewType == VT_SEARCH_BOOKS) {
			curViewType = VT_SEARCH_BOOKS;
			tlLitresMain.addView(trLitresProfile);
			tlLitresMain.addView(trLitresMyBooks);
			tlLitresMain.addView(trLitresSearch1);
			tlLitresMain.addView(trLitresSearch2);
			tlLitresMain.addView(trLitresSearch3);
			tlLitresMain.addView(trLitresSearchBtn);
			tlLitresMain.addView(trLitresSearchPersons);
			tlLitresMain.addView(trLitresSearchPersonsExt);
			tlLitresMain.addView(trLitresSearchCollections);
			tlLitresMain.addView(trLitresSearchGenres);
			tlLitresMain.addView(trLitresSearchSequences);
			mBtnStartsWith.setEnabled(true);
			mBtnSearchText.setCompoundDrawablesWithIntrinsicBounds(mImgSearchExpanded, null, null, null);
			if (isEInk) Utils.setSolidButtonEink(mBtnSearchText);
			mBtnSearchPersons.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsCollapsed, null, null, null);
			if (isEInk) Utils.setSolidButtonEink(mBtnSearchPersons);
			mBtnSearchPersonsExt.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsExtCollapsed, null, null, null);
			if (isEInk) Utils.setSolidButtonEink(mBtnSearchPersonsExt);
			mBtnSearchCollections.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollectionsCollapsed, null, null, null);
			if (isEInk) Utils.setSolidButtonEink(mBtnSearchCollections);
			mBtnSearchGenres.setCompoundDrawablesWithIntrinsicBounds(mImgSearchGenresCollapsed, null, null, null);
			if (isEInk) Utils.setSolidButtonEink(mBtnSearchGenres);
			mBtnSearchSequences.setCompoundDrawablesWithIntrinsicBounds(mImgSearchSequencesCollapsed, null, null, null);
			if (isEInk) Utils.setSolidButtonEink(mBtnSearchSequences);
			//mSearchText.setText("365 вопросов");
			mSearchText.setText("");
			mSearchText.setHint(R.string.search_books);
			mSearchText.requestFocus();
			setModeChecked(null);
		}
		if (iViewType == VT_SEARCH_COLLECTIONS) {
			curViewType = VT_SEARCH_COLLECTIONS;
			tlLitresMain.addView(trLitresProfile);
			tlLitresMain.addView(trLitresMyBooks);
			tlLitresMain.addView(trLitresSearch1);
			tlLitresMain.addView(trLitresSearchPersons);
			tlLitresMain.addView(trLitresSearchPersonsExt);
			tlLitresMain.addView(trLitresSearchCollections);
			tlLitresMain.addView(trLitresSearch2);
			tlLitresMain.addView(trLitresSearch3);
			tlLitresMain.addView(trLitresSearchBtn);
			tlLitresMain.addView(trLitresSearchGenres);
			tlLitresMain.addView(trLitresSearchSequences);
			mBtnStartsWith.setEnabled(false);
			mBtnSearchText.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollapsed, null, null, null);
			mBtnSearchPersons.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsCollapsed, null, null, null);
			mBtnSearchPersonsExt.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsExtCollapsed, null, null, null);
			mBtnSearchCollections.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollectionsExpanded, null, null, null);
			mBtnSearchGenres.setCompoundDrawablesWithIntrinsicBounds(mImgSearchGenresCollapsed, null, null, null);
			mBtnSearchSequences.setCompoundDrawablesWithIntrinsicBounds(mImgSearchSequencesCollapsed, null, null, null);
			//mSearchText.setText("вой");
			mSearchText.setText("");
			mSearchText.setHint(R.string.search_collections);
			mSearchText.requestFocus();
			setModeChecked(null);
		}
		if (iViewType == VT_SEARCH_GENRES) {
			curViewType = VT_SEARCH_GENRES;
			tlLitresMain.addView(trLitresProfile);
			tlLitresMain.addView(trLitresMyBooks);
			tlLitresMain.addView(trLitresSearch1);
			tlLitresMain.addView(trLitresSearchPersons);
			tlLitresMain.addView(trLitresSearchPersonsExt);
			tlLitresMain.addView(trLitresSearchCollections);
			tlLitresMain.addView(trLitresSearchGenres);
			tlLitresMain.addView(trLitresSearch2);
			tlLitresMain.addView(trLitresSearch3);
			tlLitresMain.addView(trLitresSearchBtnGenres);
			tlLitresMain.addView(trLitresSearchSequences);
			mBtnStartsWith.setEnabled(true);
			mBtnSearchText.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollapsed, null, null, null);
			mBtnSearchPersons.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsCollapsed, null, null, null);
			mBtnSearchPersonsExt.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsExtCollapsed, null, null, null);
			mBtnSearchCollections.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollectionsCollapsed, null, null, null);
			mBtnSearchGenres.setCompoundDrawablesWithIntrinsicBounds(mImgSearchGenresExpanded, null, null, null);
			mBtnSearchSequences.setCompoundDrawablesWithIntrinsicBounds(mImgSearchSequencesCollapsed, null, null, null);
			mSearchText.setText("");
			//mSearchText.setText("Детектив");
			mSearchText.setHint(R.string.search_genres);
			mSearchText.requestFocus();
			setModeChecked(null);
		}
		if (iViewType == VT_SEARCH_SEQUENCES) {
			curViewType = VT_SEARCH_SEQUENCES;
			tlLitresMain.addView(trLitresProfile);
			tlLitresMain.addView(trLitresMyBooks);
			tlLitresMain.addView(trLitresSearch1);
			tlLitresMain.addView(trLitresSearchPersons);
			tlLitresMain.addView(trLitresSearchPersonsExt);
			tlLitresMain.addView(trLitresSearchCollections);
			tlLitresMain.addView(trLitresSearchGenres);
			tlLitresMain.addView(trLitresSearchSequences);
			tlLitresMain.addView(trLitresSearch2);
			tlLitresMain.addView(trLitresSearch3);
			tlLitresMain.addView(trLitresSearchBtn);
			mBtnStartsWith.setEnabled(true);
			mBtnSearchText.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollapsed, null, null, null);
			mBtnSearchPersons.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsCollapsed, null, null, null);
			mBtnSearchPersonsExt.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsExtCollapsed, null, null, null);
			mBtnSearchCollections.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollectionsCollapsed, null, null, null);
			mBtnSearchGenres.setCompoundDrawablesWithIntrinsicBounds(mImgSearchGenresCollapsed, null, null, null);
			mBtnSearchSequences.setCompoundDrawablesWithIntrinsicBounds(mImgSearchSequencesExpanded, null, null, null);
			mSearchText.setText("");
			//mSearchText.setText("вой");
			mSearchText.setHint(R.string.search_sequences);
			mSearchText.requestFocus();
			setModeChecked(null);
		}
		if (iViewType == VT_SEARCH_PERSONS) {
			curViewType = VT_SEARCH_PERSONS;
			tlLitresMain.addView(trLitresProfile);
			tlLitresMain.addView(trLitresMyBooks);
			tlLitresMain.addView(trLitresSearch1);
			tlLitresMain.addView(trLitresSearchPersons);
			tlLitresMain.addView(trLitresSearch2);
			//tlLitresMain.addView(trLitresSearchPersonsMod);
			tlLitresMain.addView(trLitresSearchBtn);
			tlLitresMain.addView(trLitresSearchPersonsExt);
			tlLitresMain.addView(trLitresSearchCollections);
			tlLitresMain.addView(trLitresSearchGenres);
			tlLitresMain.addView(trLitresSearchSequences);
			mBtnStartsWith.setEnabled(true);
			mBtnSearchText.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollapsed, null, null, null);
			mBtnSearchPersons.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsExpanded, null, null, null);
			mBtnSearchPersonsExt.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsExtCollapsed, null, null, null);
			mBtnSearchCollections.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollectionsCollapsed, null, null, null);
			mBtnSearchGenres.setCompoundDrawablesWithIntrinsicBounds(mImgSearchGenresCollapsed, null, null, null);
			mBtnSearchSequences.setCompoundDrawablesWithIntrinsicBounds(mImgSearchSequencesCollapsed, null, null, null);
			mSearchText.setText("");
			//mSearchText.setText("Пушкин");
			mSearchText.setHint(R.string.search_persons);
			mSearchText.requestFocus();
			setModeChecked(null);
		}
		if (iViewType == VT_SEARCH_PERSONS_EXT) {
			curViewType = VT_SEARCH_PERSONS_EXT;
			tlLitresMain.addView(trLitresProfile);
			tlLitresMain.addView(trLitresMyBooks);
			tlLitresMain.addView(trLitresSearch1);
			tlLitresMain.addView(trLitresSearchPersons);
			tlLitresMain.addView(trLitresSearchPersonsExt);
			tlLitresMain.addView(trLitresSearchPersonsFioLabels);
			tlLitresMain.addView(trLitresSearchPersonsFio);
			//tlLitresMain.addView(trLitresSearchPersonsMod); // не работает - разбираться с литресом
			tlLitresMain.addView(trLitresSearchBtn);
			tlLitresMain.addView(trLitresSearchCollections);
			tlLitresMain.addView(trLitresSearchGenres);
			tlLitresMain.addView(trLitresSearchSequences);
			mSearchLastName.setText("");
			//mSearchLastName.setText("Лермонтов");
			mBtnStartsWith.setEnabled(true);
			mBtnSearchText.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollapsed, null, null, null);
			mBtnSearchPersons.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsCollapsed, null, null, null);
			mBtnSearchPersonsExt.setCompoundDrawablesWithIntrinsicBounds(mImgSearchPersonsExtExpanded, null, null, null);
			mBtnSearchCollections.setCompoundDrawablesWithIntrinsicBounds(mImgSearchCollectionsCollapsed, null, null, null);
			mBtnSearchGenres.setCompoundDrawablesWithIntrinsicBounds(mImgSearchGenresCollapsed, null, null, null);
			mBtnSearchSequences.setCompoundDrawablesWithIntrinsicBounds(mImgSearchSequencesCollapsed, null, null, null);
			mSearchLastName.requestFocus();
			setModeChecked(null);
		}
		mCoolReader.tintViewIcons(mBtnSearchText, true);
		mCoolReader.tintViewIcons(mBtnSearchCollections, true);
		mCoolReader.tintViewIcons(mBtnSearchGenres, true);
		mCoolReader.tintViewIcons(mBtnSearchSequences, true);
		mCoolReader.tintViewIcons(mBtnSearchPersons, true);
		mCoolReader.tintViewIcons(mBtnSearchPersonsExt, true);
		updateBalance();
	}

	private void setModeChecked(Button btn) {
		if (btn != null) {
			if (btn == mBtnContains) {
				curFindMode = SM_CONTAINS;
			}
			if (btn == mBtnStartsWith) {
				curFindMode = SM_STARTSWITH;

			}
			if (btn == mBtnStrict) {
				curFindMode = SM_STRICT;
			}
		}
		if ((curFindMode == SM_STARTSWITH) && (curViewType == VT_SEARCH_COLLECTIONS))
			curFindMode = SM_CONTAINS;
		mCoolReader.tintViewIcons(mBtnContains, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnStartsWith, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnStrict, PorterDuff.Mode.CLEAR,true);
		mBtnContains.setBackgroundColor(colorGrayCT);
		mBtnStartsWith.setBackgroundColor(colorGrayCT);
		mBtnStrict.setBackgroundColor(colorGrayCT);
		if (curFindMode == SM_CONTAINS) {
			mBtnContains.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnContains,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnContains);
		}
		if (curFindMode == SM_STARTSWITH) {
			mBtnStartsWith.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnStartsWith,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnStartsWith);
		}
		if (curFindMode == SM_STRICT) {
			mBtnStrict.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnStrict,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnStrict);
		}
	}

	private void setPersonModeChecked(Button btn) {
		if (btn != null) {
			if (btn == mBtnSearchPersonsModOnlyAuthors) {
				curPersonMode = PM_ONLY_AUTHORS;
			}
			if (btn == mBtnSearchPersonsModAnyPerson) {
				curPersonMode = PM_ANY_PERSON;

			}
		}
		mCoolReader.tintViewIcons(mBtnSearchPersonsModOnlyAuthors, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnSearchPersonsModAnyPerson, PorterDuff.Mode.CLEAR,true);
		mBtnSearchPersonsModOnlyAuthors.setBackgroundColor(colorGrayCT);
		mBtnSearchPersonsModAnyPerson.setBackgroundColor(colorGrayCT);
		if (curPersonMode == PM_ONLY_AUTHORS) {
			mBtnSearchPersonsModOnlyAuthors.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnSearchPersonsModOnlyAuthors,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnSearchPersonsModOnlyAuthors);
		}
		if (curPersonMode == PM_ANY_PERSON) {
			mBtnSearchPersonsModAnyPerson.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnSearchPersonsModAnyPerson,true);
			if (isEInk) Utils.setSolidButtonEink(mBtnSearchPersonsModAnyPerson);
		}
	}

	private void doSearch() {
		if (curViewType == VT_SEARCH_BOOKS)
		try {
			if (StrUtils.isEmptyStr(mSearchText.getText().toString())) {
				mCoolReader.showToast(R.string.value_is_empty);
				return;
			}
			LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_ARTS, 0,0, 20,
					StrUtils.getNonEmptyStr(mSearchText.getText().toString(), true), 0, curFindMode);
			mCoolReader.showBrowser(FileInfo.LITRES_TAG, lsp);
			dismiss();
		} catch (Exception e) {

		}
		if (curViewType == VT_SEARCH_GENRES)
			try {
				if (StrUtils.isEmptyStr(mSearchText.getText().toString())) {
					mCoolReader.showToast(R.string.value_is_empty);
					return;
				}
				LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_GENRES, 0,0, 20,
						StrUtils.getNonEmptyStr(mSearchText.getText().toString(), true), 0, curFindMode);
				mCoolReader.showBrowser(FileInfo.LITRES_GENRE_TAG, lsp);
				dismiss();
			} catch (Exception e) {

			}
		if (curViewType == VT_SEARCH_COLLECTIONS)
			try {
				if (StrUtils.isEmptyStr(mSearchText.getText().toString())) {
					mCoolReader.showToast(R.string.value_is_empty);
					return;
				}
				LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_COLLECTIONS, 0,0, 20,
						StrUtils.getNonEmptyStr(mSearchText.getText().toString(), true), 0, curFindMode);
				mCoolReader.showBrowser(FileInfo.LITRES_COLLECTION_TAG, lsp);
				dismiss();
			} catch (Exception e) {

			}
		if (curViewType == VT_SEARCH_SEQUENCES)
			try {
				if (StrUtils.isEmptyStr(mSearchText.getText().toString())) {
					mCoolReader.showToast(R.string.value_is_empty);
					return;
				}
				LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_SEQUENCES, 0,0, 20,
						StrUtils.getNonEmptyStr(mSearchText.getText().toString(), true), 0, curFindMode);
				mCoolReader.showBrowser(FileInfo.LITRES_SEQUENCE_TAG, lsp);
				dismiss();
			} catch (Exception e) {

			}
		if (curViewType == VT_SEARCH_PERSONS)
			try {
				if (StrUtils.isEmptyStr(mSearchText.getText().toString())) {
					mCoolReader.showToast(R.string.value_is_empty);
					return;
				}
				LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_PERSONS, 0,0, 20,
						StrUtils.getNonEmptyStr(mSearchText.getText().toString(), true), 0, curPersonMode);
				mCoolReader.showBrowser(FileInfo.LITRES_PERSON_TAG, lsp);
				dismiss();
			} catch (Exception e) {

			}
		if (curViewType == VT_SEARCH_PERSONS_EXT)
			try {
				if (StrUtils.isEmptyStr(mSearchLastName.getText().toString())) {
					mCoolReader.showToast(R.string.last_is_empty);
					return;
				}
				LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_PERSONS, 0,0, 20,
						"", 0, curPersonMode);
				lsp.lastName = StrUtils.getNonEmptyStr(mSearchLastName.getText().toString(), true);
				lsp.firstName = StrUtils.getNonEmptyStr(mSearchFirstName.getText().toString(), true);
				lsp.middleName = StrUtils.getNonEmptyStr(mSearchMiddleName.getText().toString(), true);
				mCoolReader.showBrowser(FileInfo.LITRES_PERSON_TAG, lsp);
				dismiss();
			} catch (Exception e) {

			}
	}

	public LitresMainDialog(CoolReader activity, LitresSearchParams lastLsp)
	{
		super(activity, activity.getString( R.string.litres_main), true, false);
		mCoolReader = activity;
		LitresConfig.init(mCoolReader);
		setTitle(mCoolReader.getString(R.string.litres_main));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.litres_main_dialog, null);
		tlLitresMain = view.findViewById(R.id.tl_litres_main_dlg);
		View viewAuth = mInflater.inflate(R.layout.litres_main_auth_btn, null);
		trLitresAuth = viewAuth.findViewById(R.id.tr_litres_auth);
		mBtnAuth = viewAuth.findViewById(R.id.btn_auth);
		mBtnAuth.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnAuth);
		if (isEInk) Utils.setDashedButtonEink(mBtnAuth);
		mCoolReader.tintViewIcons(mBtnAuth,true);;
		mBtnAuth.setOnClickListener(v -> {
			mCoolReader.litresCredentialsDialog = new LitresCredentialsDialog(mCoolReader);
			mCoolReader.litresCredentialsDialog.show();
		});
		View viewProfile = mInflater.inflate(R.layout.litres_main_profile, null);
		trLitresProfile = viewProfile.findViewById(R.id.tr_litres_profile);
		mBtnProfile = viewProfile.findViewById(R.id.btn_profile);
		mBtnProfile.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnProfile);
		if (isEInk) Utils.setDashedButtonEink(mBtnProfile);
		mBtnProfile.setOnClickListener(v -> {
			try {
				CloudAction.litresGetProfile(mCoolReader, this);
			} catch (Exception e) {

			}
		});
		mCoolReader.tintViewIcons(mBtnProfile,true);
		mBtnAddCredit = viewProfile.findViewById(R.id.btn_add_credit);
		mBtnAddCredit.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnAddCredit);
		if (isEInk) Utils.setDashedButtonEink(mBtnAddCredit);
		mBtnAddCredit.setOnClickListener(v -> {
			try {
				CloudAction.litresPutMoneyOnAccount(mCoolReader, this);
				if (LitresConfig.litresAccountInfo != null) LitresConfig.litresAccountInfo.needRefresh = true;
			} catch (Exception e) {

			}
		});
		mCoolReader.tintViewIcons(mBtnAddCredit,true);
		View viewMy = mInflater.inflate(R.layout.litres_main_search_my_books, null);
		trLitresMyBooks = viewMy.findViewById(R.id.tr_litres_my_books);
		mBtnMyBooks = viewMy.findViewById(R.id.btn_my_books);
		mBtnMyBooks.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnMyBooks);
		if (isEInk) Utils.setDashedButtonEink(mBtnMyBooks);
		mBtnMyBooks.setOnClickListener(v -> {
			try {
				LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_MY_BOOKS, 0, 0, 20,"", 0, 0);
				mCoolReader.showBrowser(FileInfo.LITRES_TAG, lsp);
				dismiss();
			} catch (Exception e) {

			}
		});
		mCoolReader.tintViewIcons(mBtnMyBooks,true);;
		View viewSearch1 = mInflater.inflate(R.layout.litres_main_search_row1, null);
		trLitresSearch1 = viewSearch1.findViewById(R.id.tr_litres_search1);
		mBtnSearchText = viewSearch1.findViewById(R.id.btn_search_books);
		mBtnSearchText.setBackgroundColor(colorGrayC);
		mBtnSearchText.setOnClickListener(v -> {
			setupView(VT_SEARCH_BOOKS);
		});
		Drawable imgC1 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
		Drawable imgE1 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
		mImgSearchCollapsed = imgC1.getConstantState().newDrawable().mutate();
		mImgSearchExpanded = imgE1.getConstantState().newDrawable().mutate();
		mCoolReader.tintViewIcons(mBtnSearchText,true);;
		View viewSearch2 = mInflater.inflate(R.layout.litres_main_search_row2, null);
		trLitresSearch2 = viewSearch2.findViewById(R.id.tr_litres_search2);
		mSearchText = viewSearch2.findViewById(R.id.litres_search_edit1);
		View viewSearch3 = mInflater.inflate(R.layout.litres_main_search_row3, null);
		trLitresSearch3 = viewSearch3.findViewById(R.id.tr_litres_search3);
		mBtnContains = viewSearch3.findViewById(R.id.btn_contains);
		mBtnContains.setBackgroundColor(colorGrayC);
		mBtnContains.setPadding(1, 3, 1, 3);
		mBtnStartsWith = viewSearch3.findViewById(R.id.btn_startswith);
		mBtnStartsWith.setBackgroundColor(colorGrayC);
		mBtnStartsWith.setPadding(1, 3, 1, 3);
		mBtnStrict = viewSearch3.findViewById(R.id.btn_strict);
		mBtnStrict.setBackgroundColor(colorGrayC);
		mBtnStrict.setPadding(1, 3, 1, 3);
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		Drawable img3 = img.getConstantState().newDrawable().mutate();
		mBtnContains.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		mBtnStartsWith.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		mBtnStrict.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
		setModeChecked(mBtnContains);
		mBtnContains.setOnClickListener(v -> setModeChecked(mBtnContains));
		mBtnStartsWith.setOnClickListener(v -> setModeChecked(mBtnStartsWith));
		mBtnStrict.setOnClickListener(v -> setModeChecked(mBtnStrict));
		View viewSearchBtn = mInflater.inflate(R.layout.litres_main_search_btn, null);
		trLitresSearchBtn = viewSearchBtn.findViewById(R.id.tr_litres_search_btn);
		mBtnSearch = viewSearchBtn.findViewById(R.id.btn_search);
		mBtnSearch.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnSearch);
		if (isEInk) Utils.setDashedButtonEink(mBtnSearch);
		mBtnSearch.setOnClickListener(v -> {
			doSearch();
		});
		View viewSearchBtnGenres = mInflater.inflate(R.layout.litres_main_search_btn_and_genres, null);
		trLitresSearchBtnGenres = viewSearchBtnGenres.findViewById(R.id.tr_litres_search_btn_and_genres);
		mBtnSearchBtnGenres = viewSearchBtnGenres.findViewById(R.id.btn_search);
		mBtnSearchBtnGenres.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnSearchBtnGenres);
		if (isEInk) Utils.setDashedButtonEink(mBtnSearchBtnGenres);
		mBtnSearch2 = viewSearchBtnGenres.findViewById(R.id.btn_search);
		mBtnSearch2.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnSearch2);
		if (isEInk) Utils.setDashedButtonEink(mBtnSearch2);
		mBtnSearch2.setOnClickListener(v -> {
			doSearch();
		});
		mBtnSearchBtnGenresAll = viewSearchBtnGenres.findViewById(R.id.btn_search_all_genres);
		mBtnSearchBtnGenresAll.setBackgroundColor(colorGrayC);
		Utils.setDashedButton(mBtnSearchBtnGenresAll);
		if (isEInk) Utils.setDashedButtonEink(mBtnSearchBtnGenresAll);
		mBtnSearchBtnGenresAll.setOnClickListener(v -> {
			LitresSearchParams lsp = new LitresSearchParams(LitresSearchParams.SEARCH_TYPE_GENRES, 0,0, 20,
					"", 0, 0);
			mCoolReader.showBrowser(FileInfo.LITRES_GENRE_TAG, lsp);
			dismiss();
		});
		View viewSearchCollections = mInflater.inflate(R.layout.litres_main_search_collections, null);
		trLitresSearchCollections = viewSearchCollections.findViewById(R.id.tr_litres_search_collections);
		Drawable imgC2 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
		Drawable imgE2 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
		mImgSearchCollectionsCollapsed = imgC2.getConstantState().newDrawable().mutate();
		mImgSearchCollectionsExpanded = imgE2.getConstantState().newDrawable().mutate();
		mBtnSearchCollections = viewSearchCollections.findViewById(R.id.btn_search_collections);
		mBtnSearchCollections.setBackgroundColor(colorGrayC);
		mBtnSearchCollections.setOnClickListener(v -> {
			setupView(VT_SEARCH_COLLECTIONS);
		});
		mCoolReader.tintViewIcons(mBtnSearchCollections,true);
		View viewSearchGenres = mInflater.inflate(R.layout.litres_main_search_genres, null);
		trLitresSearchGenres = viewSearchGenres.findViewById(R.id.tr_litres_search_genres);
		Drawable imgC3 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
		Drawable imgE3 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
		mImgSearchGenresCollapsed = imgC3.getConstantState().newDrawable().mutate();
		mImgSearchGenresExpanded = imgE3.getConstantState().newDrawable().mutate();
		mBtnSearchGenres = viewSearchGenres.findViewById(R.id.btn_search_genres);
		mBtnSearchGenres.setBackgroundColor(colorGrayC);
		mBtnSearchGenres.setOnClickListener(v -> {
			setupView(VT_SEARCH_GENRES);
		});
		mCoolReader.tintViewIcons(mBtnSearchGenres,true);
		View viewSearchSequences = mInflater.inflate(R.layout.litres_main_search_sequences, null);
		trLitresSearchSequences = viewSearchSequences.findViewById(R.id.tr_litres_search_sequences);
		Drawable imgC4 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
		Drawable imgE4 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
		mImgSearchSequencesCollapsed = imgC4.getConstantState().newDrawable().mutate();
		mImgSearchSequencesExpanded = imgE4.getConstantState().newDrawable().mutate();
		mBtnSearchSequences = viewSearchSequences.findViewById(R.id.btn_search_sequences);
		mBtnSearchSequences.setBackgroundColor(colorGrayC);
		mBtnSearchSequences.setOnClickListener(v -> {
			setupView(VT_SEARCH_SEQUENCES);
		});
		mCoolReader.tintViewIcons(mBtnSearchSequences,true);
		View viewSearchPersons = mInflater.inflate(R.layout.litres_main_search_persons, null);
		trLitresSearchPersons = viewSearchPersons.findViewById(R.id.tr_litres_search_persons);
		Drawable imgC5 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
		Drawable imgE5 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
		mImgSearchPersonsCollapsed = imgC5.getConstantState().newDrawable().mutate();
		mImgSearchPersonsExpanded = imgE5.getConstantState().newDrawable().mutate();
		mBtnSearchPersons = viewSearchPersons.findViewById(R.id.btn_search_persons);
		mBtnSearchPersons.setBackgroundColor(colorGrayC);
		mBtnSearchPersons.setOnClickListener(v -> {
			setupView(VT_SEARCH_PERSONS);
		});
		mCoolReader.tintViewIcons(mBtnSearchPersons,true);
		View viewSearchPersonsExt = mInflater.inflate(R.layout.litres_main_search_persons_ext, null);
		trLitresSearchPersonsExt = viewSearchPersonsExt.findViewById(R.id.tr_litres_search_persons_ext);
		View viewSearchPersonsFioLabels = mInflater.inflate(R.layout.litres_main_search_persons_fio_labels, null);
		trLitresSearchPersonsFioLabels = viewSearchPersonsFioLabels.findViewById(R.id.tr_litres_search_persons_fio_labels);
		View viewSearchPersonsFio = mInflater.inflate(R.layout.litres_main_search_persons_fio, null);
		trLitresSearchPersonsFio = viewSearchPersonsFio.findViewById(R.id.tr_litres_search_persons_fio);
		mSearchLastName = viewSearchPersonsFio.findViewById(R.id.litres_search_person_last);
		mSearchFirstName = viewSearchPersonsFio.findViewById(R.id.litres_search_person_first);
		mSearchMiddleName = viewSearchPersonsFio.findViewById(R.id.litres_search_person_middle);
		View viewSearchPersonsMod = mInflater.inflate(R.layout.litres_main_search_persons_mod, null);
		trLitresSearchPersonsMod = viewSearchPersonsMod.findViewById(R.id.tr_litres_search_persons_mod);
		mBtnSearchPersonsModOnlyAuthors = viewSearchPersonsMod.findViewById(R.id.btn_authors);
		mBtnSearchPersonsModAnyPerson = viewSearchPersonsMod.findViewById(R.id.btn_any);
		mBtnSearchPersonsModOnlyAuthors.setBackgroundColor(colorGrayC);
		mBtnSearchPersonsModOnlyAuthors.setPadding(1, 3, 1, 3);
		mBtnSearchPersonsModAnyPerson.setBackgroundColor(colorGrayC);
		mBtnSearchPersonsModAnyPerson.setPadding(1, 3, 1, 3);
		Drawable imgP = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable imgP1 = imgP.getConstantState().newDrawable().mutate();
		Drawable imgP2 = imgP.getConstantState().newDrawable().mutate();
		mBtnSearchPersonsModOnlyAuthors.setCompoundDrawablesWithIntrinsicBounds(imgP1, null, null, null);
		mBtnSearchPersonsModAnyPerson.setCompoundDrawablesWithIntrinsicBounds(imgP2, null, null, null);
		mBtnSearchPersonsModOnlyAuthors.setOnClickListener(v -> setPersonModeChecked(mBtnSearchPersonsModOnlyAuthors));
		mBtnSearchPersonsModAnyPerson.setOnClickListener(v -> setPersonModeChecked(mBtnSearchPersonsModAnyPerson));
		setPersonModeChecked(mBtnSearchPersonsModOnlyAuthors);
		Drawable imgC6 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
		Drawable imgE6 = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
		mImgSearchPersonsExtCollapsed = imgC6.getConstantState().newDrawable().mutate();
		mImgSearchPersonsExtExpanded = imgE6.getConstantState().newDrawable().mutate();
		mBtnSearchPersonsExt = viewSearchPersonsExt.findViewById(R.id.btn_search_persons_ext);
		mBtnSearchPersonsExt.setBackgroundColor(colorGrayC);
		mBtnSearchPersonsExt.setOnClickListener(v -> {
			setupView(VT_SEARCH_PERSONS_EXT);
		});
		mCoolReader.tintViewIcons(mBtnSearchPersonsExt,true);
		
		setupView(VT_SEARCH_BOOKS);
		setView( view );
	}

	@Override
	public void onPositiveButtonClick() {
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}
}
