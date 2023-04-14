package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.dic.DictsDlg;
import org.coolreader.readerview.ReaderView;
import org.coolreader.dic.Dictionaries;
import org.coolreader.R;
import org.coolreader.dic.TranslationDirectionDialog;
import org.coolreader.options.OptionsDialog;
import org.coolreader.options.SelectionModesOption;
import org.coolreader.userdic.UserDicEntry;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class SelectionToolbarDlg {
	PopupWindow mWindow;
	View mAnchor;
	boolean mIsShort;
	boolean mIsHorz;
	CoolReader mCoolReader;
	ReaderView mReaderView;
	View mPanel;
	public static Selection stSel;
	Selection selection;
	public static boolean isVisibleNow = false;
	boolean showAtTop;
	int colorGray;
	int colorGrayC;
	int colorIcon;
	ColorDrawable colorButtons;
	String sTranspButtons;
	int alphaVal;
	int [] location = new int[2];
	int popupY;
	boolean isInvisible;
	//buttons
	ImageButton btnVisible;
	ImageButton btnGoback;
	ImageButton btnSelectionBookmark;
	ImageButton btnSelectionCopy;
	ImageButton btnSelectionDict;
	ImageButton btnSelectionEmail;
	ImageButton btnSelectionFind;
	ImageButton btnSelectionCite;
	ImageButton btnSelectionMore;

	static public void showDialog(CoolReader coolReader, ReaderView readerView, final Selection selection)
	{
		if (coolReader.getReaderView() != null)
			if (coolReader.getReaderView().currentImageViewer != null) {
				coolReader.getReaderView().clearSelection();
				return;
			}
		SelectionToolbarDlg dlg = new SelectionToolbarDlg(coolReader, readerView, selection, false);
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
	}

	static public void showDialogShort(CoolReader coolReader, ReaderView readerView, final Selection selection)
	{
		if (coolReader.getReaderView() != null)
			if (coolReader.getReaderView().currentImageViewer != null) {
				coolReader.getReaderView().clearSelection();
				return;
			}
		SelectionToolbarDlg dlg = new SelectionToolbarDlg(coolReader, readerView, selection, true);
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
	}

	private boolean pageModeSet = false;
	private boolean changedPageMode;
	private void setReaderMode()
	{
		if (pageModeSet)
			return;
		if ("1".equals(mReaderView.getSetting(ReaderView.PROP_PAGE_VIEW_MODE_SEL_DONT_CHANGE))) return;
		//if (DeviceInfo.EINK_SCREEN) { return; } // switching to scroll view doesn't work well on eink screens
		
		String oldViewSetting = mReaderView.getSetting(ReaderView.PROP_PAGE_VIEW_MODE);
		if ("1".equals(oldViewSetting)) {
			changedPageMode = true;
			mReaderView.setViewModeNonPermanent(ViewMode.SCROLL);
			if (showAtTop) {
				showAtTop = false;
				BackgroundThread.instance().postGUI(() -> {
					placeLayouts();
				}, 500);
			}
		}
		pageModeSet = true;
	}
	
	private void restoreReaderMode()
	{
		if ( changedPageMode ) {
			mReaderView.setViewModeNonPermanent(ViewMode.PAGES);
		}
	}
	
	private void changeSelectionBound(boolean start, int delta) {
		L.d("changeSelectionBound(" + (start?"start":"end") + ", " + delta + ")");
		setReaderMode();
		ReaderCommand cmd = start ? ReaderCommand.DCMD_SELECT_MOVE_LEFT_BOUND_BY_WORDS : ReaderCommand.DCMD_SELECT_MOVE_RIGHT_BOUND_BY_WORDS;
		mReaderView.moveSelection(cmd, delta, new ReaderView.MoveSelectionCallback() {
			
			@Override
			public void onNewSelection(Selection selection) {
				Log.d("cr3", "onNewSelection: " + selection.text);
				SelectionToolbarDlg.this.selection = selection;
				stSel = selection;
			}
			
			@Override
			public void onFail() {
				Log.d("cr3", "fail()");
				//currentSelection = null;
			}
		}, true);
	}
	
	private final static int SELECTION_CONTROL_STEP = 10;
	private final static int SELECTION_SMALL_STEP = 1;
	private final static int SELECTION_NEXT_SENTENCE_STEP = 999;

	private class BoundControlListener implements OnSeekBarChangeListener {

		public BoundControlListener(SeekBar sb, boolean start) {
			this.start = start;
			this.sb = sb;
			sb.setOnSeekBarChangeListener(this);
		}
		final boolean start;
		final SeekBar sb;
		int lastProgress = 50;
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			sb.setProgress(50);
			lastProgress = 50;
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			sb.setProgress(50);
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (!fromUser)
				return;
			int diff = (progress - lastProgress) / SELECTION_CONTROL_STEP * SELECTION_CONTROL_STEP;
			if (diff!=0) {
				lastProgress += diff;
				changeSelectionBound(start, diff/SELECTION_CONTROL_STEP);
			}
		}
	};
	
	private void closeDialog(boolean clearSelection) {
		if (clearSelection)
			mReaderView.clearSelection();
		restoreReaderMode();
		isVisibleNow = false;
		mReaderView.toggleScreenUpdateModeMode(false);
		mCoolReader.updateUserDicWords();
		mWindow.dismiss();
	}

	LinearLayout llTopLine;
	LinearLayout llMiddleContents;
	LinearLayout llBottomLine;
	LinearLayout llSliderBottom;
	LinearLayout llSliderTop;
	LinearLayout llRecentDics;
	LinearLayout svRecentDics;
	LinearLayout llAddButtons;
	LinearLayout llButtonsRow;
	LinearLayout llButtonsRow2;

	public boolean addButtonEnabled;
	private Properties props;

	public static void saveUserDic(boolean isCitation, String text, Selection selection,
								   CoolReader mActivity, ReaderView mReaderView, String dslStruct) {
		UserDicEntry ude = new UserDicEntry();
		ude.setId(0L);
		ude.setDic_word(selection.text);
		if (isCitation)
			ude.setDic_word_translate(BookmarkEditDialog.getContextText(mActivity, selection.text));
		else
			ude.setDic_word_translate(text);
		ude.setDslStruct(dslStruct);
		final String sBookFName = mReaderView.mBookInfo.getFileInfo().getFilename();
		CRC32 crc = new CRC32();
		crc.update(sBookFName.getBytes());
		ude.setDic_from_book(String.valueOf(crc.getValue()));
		ude.setCreate_time(System.currentTimeMillis());
		ude.setLast_access_time(System.currentTimeMillis());
		ude.setLanguage(mReaderView.mBookInfo.getFileInfo().language);
		ude.setShortContext(BookmarkEditDialog.getContextText(mActivity, text));
		ude.setFullContext(BookmarkEditDialog.getFullContextText(mActivity));
		ude.setIsCustomColor(0);
		ude.setCustomColor(Utils.colorToHex(0));
		ude.setSeen_count(0L);
		ude.setIs_citation(isCitation? 1 : 0);
		mActivity.getDB().saveUserDic(ude, UserDicEntry.ACTION_NEW);
		mActivity.getmUserDic().put(ude.getIs_citation()+ude.getDic_word(), ude);
		BackgroundThread.instance().postGUI(() -> {
			if (isCitation)
				mActivity.showToast(R.string.citation_created);
			else
				mActivity.showToast(R.string.user_dic_entry_created);
			mActivity.updateUserDicWords();
		}, 1000);
	}

	public static void sendTo1(String text, Selection selection, CoolReader mActivity) {
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		String text1 = BookmarkEditDialog.getSendToText1(mActivity,
				selection.text,
				BookmarkEditDialog.getContextText(mActivity, selection.text));
		String text2 = BookmarkEditDialog.getSendToText2(mActivity,
				selection.text,
				text,
				BookmarkEditDialog.getContextText(mActivity, selection.text));
		emailIntent.setType("text/plain");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, text1);
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text2);
		mActivity.startActivity(Intent.createChooser(emailIntent, null));
	}

	public static void sendTo2(String text, Selection selection, CoolReader mActivity) {
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		int selAction = -1;
		if (mActivity.getReaderView() != null) selAction = mActivity.getReaderView().mBookmarkActionSendTo;
		if (selAction == -1) {
			sendTo1(text, selection, mActivity);
			return;
		}
		int titleSendTo =  SelectionModesOption.getSelectionActionTitle(selAction);
		Dictionaries.DictInfo curDict = OptionsDialog.getDicValue(mActivity.getString(titleSendTo), mActivity.settings(), mActivity);
		if (curDict == null) {
			sendTo1(text, selection, mActivity);
			return;
		}
		String text1 = BookmarkEditDialog.getSendToText1(mActivity,
			selection.text,
			BookmarkEditDialog.getContextText(mActivity, selection.text));
		String text2 = BookmarkEditDialog.getSendToText2(mActivity,
				selection.text,
				text,
				BookmarkEditDialog.getContextText(mActivity, selection.text));
		emailIntent.setType("text/plain");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, text1);
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text2);
		for (ResolveInfo resolveInfo : mActivity.getPackageManager().queryIntentActivities(emailIntent, 0)) {
			if( resolveInfo.activityInfo.packageName.contains(curDict.packageName)){
				emailIntent.setComponent(new ComponentName(
						resolveInfo.activityInfo.packageName,
						resolveInfo.activityInfo.name));
				try
				{
					mActivity.startActivity(emailIntent);
				} catch ( ActivityNotFoundException e ) {
					sendTo1(text, selection, mActivity);
					return;
				}
			}

		}
	}

	public void toggleAddButtons(boolean dontChange) {
		if (llAddButtons == null) return;
		if (!dontChange) {
			addButtonEnabled = !addButtonEnabled;
			if (props != null) {
				mCoolReader.getReaderView().skipFallbackWarning = true;
				props.setProperty(Settings.PROP_APP_OPTIONS_EXT_SELECTION_TOOLBAR, addButtonEnabled ? "1" : "0");
				mCoolReader.getReaderView().skipFallbackWarning = true;
				mCoolReader.setSettings(props, -1, true);
			}
		}
		if (addButtonEnabled || mIsHorz) {
			if (llButtonsRow2 != null) llButtonsRow2.removeAllViews();
			if (llButtonsRow2 != null) llButtonsRow2.addView(llAddButtons);
			ColorDrawable c = new ColorDrawable(colorGrayC);
			String sTranspButtons = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_TRANSP_BUTTONS, "0");
			if (!sTranspButtons.equals("0")) c.setAlpha(130);
				else c.setAlpha(255);
			llAddButtons.findViewById(R.id.selection_bookmark).setBackground(c);
			if (mCoolReader.isEInk) Utils.setBtnBackground(llAddButtons.findViewById(R.id.selection_bookmark), null, mCoolReader.isEInk);
			llAddButtons.findViewById(R.id.selection_bookmark).setOnClickListener(v -> {
				mReaderView.showNewBookmarkDialog(selection, Bookmark.TYPE_COMMENT, "");
				closeDialog(true);
			});
			llAddButtons.findViewById(R.id.selection_bookmark).setOnLongClickListener(v -> {
				BookmarksDlg dlg = new BookmarksDlg(mCoolReader, mReaderView, null, false, null);
				dlg.show();
				closeDialog(true);
				return true;
			});
			llAddButtons.findViewById(R.id.btn_correction).setBackground(c);
			if (mCoolReader.isEInk) Utils.setBtnBackground(llAddButtons.findViewById(R.id.btn_correction), null, mCoolReader.isEInk);
			llAddButtons.findViewById(R.id.btn_correction).setOnClickListener(v -> {
				mReaderView.showNewBookmarkDialog(selection, Bookmark.TYPE_CORRECTION, selection.text);
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			});
			llAddButtons.findViewById(R.id.btn_user_dic).setBackground(c);
			if (mCoolReader.isEInk) Utils.setBtnBackground(llAddButtons.findViewById(R.id.btn_user_dic), null, mCoolReader.isEInk);
			llAddButtons.findViewById(R.id.btn_user_dic).setOnClickListener(v -> {
				mReaderView.showNewBookmarkDialog(selection, Bookmark.TYPE_USER_DIC, "");
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			});
			llAddButtons.findViewById(R.id.btn_web_search).setBackground(c);
			if (mCoolReader.isEInk) Utils.setBtnBackground(llAddButtons.findViewById(R.id.btn_web_search), null, mCoolReader.isEInk);
			llAddButtons.findViewById(R.id.btn_web_search).setOnClickListener(v -> {
				final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
				emailIntent.putExtra(SearchManager.QUERY, selection.text.trim());
				try {
					mCoolReader.startActivity(emailIntent);
				} catch (Exception e) {
					mCoolReader.showToast(mCoolReader.getString(R.string.intent_error)+": "+e.getMessage());
				}
				closeDialog(true);
			});
			llAddButtons.findViewById(R.id.btn_dic_list).setBackground(c);
			if (mCoolReader.isEInk) Utils.setBtnBackground(llAddButtons.findViewById(R.id.btn_dic_list), null, mCoolReader.isEInk);
			llAddButtons.findViewById(R.id.btn_dic_list).setOnClickListener(v -> {
				DictsDlg dlg = new DictsDlg(mCoolReader, mReaderView, selection.text, null, false);
				dlg.show();
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			});
			llAddButtons.findViewById(R.id.btn_combo).setBackground(c);
			if (mCoolReader.isEInk) Utils.setBtnBackground(llAddButtons.findViewById(R.id.btn_combo), null, mCoolReader.isEInk);
			llAddButtons.findViewById(R.id.btn_combo).setOnClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mCoolReader.showToast(R.string.only_in_premium);
					return;
				}
				Dictionaries.DictInfo di = mCoolReader.getCurOrFirstOnlineDic();
				if (di != null) {
					if (!StrUtils.isEmptyStr(selection.text)) {
						mCoolReader.mDictionaries.setAdHocDict(di);
						mCoolReader.findInDictionary(selection.text, false, null,
								new CoolReader.DictionaryCallback() {

							@Override
							public boolean showDicToast() {
								return true;
							}

							@Override
							public boolean saveToHist() {
								return false;
							}

							@Override
							public void done(String result, String dslStruct) {
								saveUserDic(false, result, selection, mCoolReader, mReaderView, dslStruct);
								closeDialog(true);
							}

							@Override
							public void fail(Exception e, String msg) {
								mCoolReader.showToast(msg);
							}
						});
					}
				}
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			});
			llAddButtons.findViewById(R.id.btn_super_combo).setBackground(c);
			if (mCoolReader.isEInk) Utils.setBtnBackground((ImageButton) llAddButtons.findViewById(R.id.btn_super_combo), null, mCoolReader.isEInk);
			llAddButtons.findViewById(R.id.btn_super_combo).setOnClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mCoolReader.showToast(R.string.only_in_premium);
					return;
				}
				Dictionaries.DictInfo di = mCoolReader.getCurOrFirstOnlineDic();
				if (di != null) {
					if (!StrUtils.isEmptyStr(selection.text)) {
						mCoolReader.mDictionaries.setAdHocDict(di);
						mCoolReader.findInDictionary(selection.text, false,null,
								new CoolReader.DictionaryCallback() {

							@Override
							public boolean showDicToast() {
								return false;
							}

							@Override
							public boolean saveToHist() {
								return false;
							}

							@Override
							public void done(String result, String dslStruct) {
								saveUserDic(false, result, selection, mCoolReader, mReaderView, dslStruct);
								sendTo2(result, selection, mCoolReader);
								closeDialog(true);
							}

							@Override
							public void fail(Exception e, String msg) {
								mCoolReader.showToast(msg);
							}
						});
					}
				}
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			});
		} else {
			if (llButtonsRow2 != null)
				llButtonsRow2.removeAllViews();
		}
		mCoolReader.tintViewIcons(mPanel);
	}

	private boolean isShowAtTop() {
		if (mIsShort) return false;
		mAnchor.getLocationOnScreen(location);
		int selectionTop = 0;
		int selectionBottom = 0;
		if (mReaderView.lastSelection != null) {
			selectionTop = mReaderView.lastSelection.startY;
			selectionBottom = mReaderView.lastSelection.endY;
		}

		if (selectionBottom<selectionTop) {
			int dummy = selectionBottom;
			selectionBottom = selectionTop;
			selectionTop = dummy;
		}
		popupY = location[1] + mAnchor.getHeight() - mPanel.getHeight();

		if (!pageModeSet) // do not show toolbar at top due to switch to scroll mode - selection is moved upper
			if (
					(selectionTop > (mReaderView.getSurface().getHeight() / 2)) &&
							(selectionBottom > (mReaderView.getSurface().getHeight() / 2))
			)
				return true;
		return false;
	}

	private void setVisibile() {
		if (!isInvisible) return;
		isInvisible = false;
		if (llRecentDics != null) llRecentDics.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
		if (svRecentDics != null) svRecentDics.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
		if (llAddButtons != null) llAddButtons.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
		if (llButtonsRow != null) llButtonsRow.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
		if (llButtonsRow2 != null) llButtonsRow2.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
		if (llSliderTop != null) llSliderTop.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
		if (llSliderBottom != null) llSliderBottom.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
	}

	private void placeLayouts() {
		String sSliders = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_SLIDERS, "0");
		String sDicsPanel = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_RECENT_DICS, "0");
		boolean slidersVisible = sSliders.equals("1");
		if (!slidersVisible) slidersVisible = sSliders.equals("0") && (!mIsHorz);
		llTopLine.setOnClickListener(v -> setVisibile());
		llBottomLine.setOnClickListener(v -> setVisibile());
		if (showAtTop) {
			llTopLine.removeAllViews();
			if (!mIsShort)
				if (slidersVisible) llTopLine.addView(llSliderTop);
			if (sDicsPanel.equals("0")) llTopLine.addView(llRecentDics);
			if (sDicsPanel.equals("1")) llTopLine.addView(svRecentDics);
			if (!mIsShort) llTopLine.addView(llButtonsRow);
			llBottomLine.removeAllViews();
			if (!mIsShort)
				if (slidersVisible) llBottomLine.addView(llSliderBottom);

		} else {
			llTopLine.removeAllViews();
			if (!mIsShort)
				if (slidersVisible) llTopLine.addView(llSliderTop);
			llBottomLine.removeAllViews();
			if (sDicsPanel.equals("0")) llBottomLine.addView(llRecentDics);
			if (sDicsPanel.equals("1")) llBottomLine.addView(svRecentDics);
			if (!mIsShort) llBottomLine.addView(llButtonsRow);
			if (!mIsShort)
				if (slidersVisible) llBottomLine.addView(llSliderBottom);
		}
		int colorFill = colorGray;
		if (mCoolReader.isEInk) colorFill = Color.WHITE;
		if (llSliderTop != null)
			if (!mIsShort) llSliderTop.setBackgroundColor(Color.argb(alphaVal, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));
		if (llSliderBottom != null)
			if (!mIsShort) llSliderBottom.setBackgroundColor(Color.argb(alphaVal, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));
		if (llRecentDics != null)
			llRecentDics.setBackgroundColor(Color.argb(alphaVal, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));
		if (svRecentDics != null)
			svRecentDics.setBackgroundColor(Color.argb(alphaVal, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));
		if (!mIsShort) llButtonsRow.setBackgroundColor(Color.argb(alphaVal, Color.red(colorFill),Color.green(colorFill),Color.blue(colorFill)));

		String sExt = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_EXT_SELECTION_TOOLBAR, "0");
		addButtonEnabled = StrUtils.getNonEmptyStr(sExt, true).equals("1");
		if (((addButtonEnabled) && (!mIsShort)) || (mIsHorz)) toggleAddButtons(true);
		initRecentDics();
		// set up buttons
		btnVisible = llMiddleContents.findViewById(R.id.btn_visible);
		btnVisible.setOnClickListener(v -> {
			isInvisible = !isInvisible;
			if (llRecentDics != null) llRecentDics.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (svRecentDics != null) svRecentDics.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llAddButtons != null) llAddButtons.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llButtonsRow != null) llButtonsRow.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llButtonsRow2 != null) llButtonsRow2.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llSliderTop != null) llSliderTop.setVisibility(View.VISIBLE);
			if (llSliderBottom != null) llSliderBottom.setVisibility(View.VISIBLE);
		});
		btnVisible.setOnLongClickListener(v -> {
			isInvisible = !isInvisible;
			if (llRecentDics != null) llRecentDics.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (svRecentDics != null) svRecentDics.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llAddButtons != null) llAddButtons.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llButtonsRow != null) llButtonsRow.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llButtonsRow2 != null) llButtonsRow2.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llSliderTop != null) llSliderTop.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			if (llSliderBottom != null) llSliderBottom.setVisibility(isInvisible? View.INVISIBLE: View.VISIBLE);
			return true;
		});
		btnGoback = llMiddleContents.findViewById(R.id.btn_goback);
		btnGoback.setOnClickListener(v -> {
			closeDialog(true);
		});
		ColorDrawable colorButtons130 = new ColorDrawable(colorGrayC);
		colorButtons130.setAlpha(130);
		btnVisible.setBackground(colorButtons130);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnVisible, null, mCoolReader.isEInk);
		btnGoback.setBackground(colorButtons130);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnGoback, null, mCoolReader.isEInk);
		btnSelectionBookmark = llButtonsRow.findViewById(R.id.btn_quick_bookmark);
		if (btnSelectionBookmark != null)
			btnSelectionBookmark.setBackground(colorButtons);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnSelectionBookmark, null, mCoolReader.isEInk);
		btnSelectionCopy = llButtonsRow.findViewById(R.id.selection_copy);
		if (btnSelectionCopy != null)
			btnSelectionCopy.setBackground(colorButtons);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnSelectionCopy, null, mCoolReader.isEInk);
		btnSelectionDict = llButtonsRow.findViewById(R.id.selection_dict);
		if (btnSelectionDict != null)
			btnSelectionDict.setBackground(colorButtons);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnSelectionDict, null, mCoolReader.isEInk);
		btnSelectionEmail = llButtonsRow.findViewById(R.id.selection_email);
		if (btnSelectionEmail != null)
			btnSelectionEmail.setBackground(colorButtons);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnSelectionEmail, null, mCoolReader.isEInk);
		btnSelectionFind = llButtonsRow.findViewById(R.id.selection_find);
		if (btnSelectionFind != null)
			btnSelectionFind.setBackground(colorButtons);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnSelectionFind, null, mCoolReader.isEInk);
		btnSelectionCite = llButtonsRow.findViewById(R.id.selection_cite);
		if (btnSelectionCite != null)
			btnSelectionCite.setBackground(colorButtons);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnSelectionCite, null, mCoolReader.isEInk);
		btnSelectionMore = llButtonsRow.findViewById(R.id.selection_more);
		if (btnSelectionMore != null)
			btnSelectionMore.setBackground(null);
		if (mCoolReader.isEInk) Utils.setBtnBackground(btnSelectionMore, null, mCoolReader.isEInk);
		// set up events
		setupEvents();
	}

	private void setupEvents() {
		if (btnSelectionBookmark != null) {
			btnSelectionBookmark.setOnClickListener(v -> {
				Bookmark bmk = new Bookmark();
				bmk.setType(Bookmark.TYPE_COMMENT);
				bmk.setPosText(selection.text);
				bmk.setStartPos(selection.startPos);
				bmk.setEndPos(selection.endPos);
				bmk.setPercent(selection.percent);
				bmk.setTitleText(selection.chapter);
				bmk.setIsCustomColor(0);
				bmk.setCustomColor(Utils.colorToHex(0));
				bmk.setShortContext(BookmarkEditDialog.getContextText(mCoolReader, selection.text));
				bmk.setFullContext(BookmarkEditDialog.getFullContextText(mCoolReader));
				mReaderView.addBookmark(bmk);
				closeDialog(true);
			});

			btnSelectionBookmark.setOnLongClickListener(v -> {
				mReaderView.showNewBookmarkDialog(selection, Bookmark.TYPE_COMMENT, "");
				closeDialog(true);
				return true;
			});
		}
		if (btnSelectionCopy != null)
			btnSelectionCopy.setOnClickListener(v -> {
				mReaderView.copyToClipboard(selection.text);
				closeDialog(true);
			});
		if (btnSelectionDict != null) {
			btnSelectionDict.setOnClickListener(v -> {
				//PositionProperties currpos = mReaderView.getDoc().getPositionProps(null);
				//Log.e("CURPOS", currpos.pageText);
				if (mCoolReader.ismDictLongtapChange()) {
					DictsDlg dlg = new DictsDlg(mCoolReader, mReaderView, selection.text, null, false);
					dlg.show();
					closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
				} else {
					mCoolReader.findInDictionary(selection.text, false, null);
					closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
				}
			});

			btnSelectionDict.setOnLongClickListener(v -> {
				if (!mCoolReader.ismDictLongtapChange()) {
					DictsDlg dlg = new DictsDlg(mCoolReader, mReaderView, selection.text, null, false);
					dlg.show();
					closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
				} else {
					mCoolReader.findInDictionary(selection.text, false, null);
					closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
				}
				return true;
			});

		}
		if (btnSelectionEmail != null)
			btnSelectionEmail.setOnClickListener(v -> {
				mReaderView.sendQuotationInEmail(selection);
				closeDialog(true);
			});
		if (btnSelectionFind != null) {
			btnSelectionFind.setOnClickListener(v -> {
				mReaderView.showSearchDialog(selection.text.trim());
				closeDialog(true);
			});

			btnSelectionFind.setOnLongClickListener(v -> {
				final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
				emailIntent.putExtra(SearchManager.QUERY, selection.text.trim());
				mCoolReader.startActivity(emailIntent);
				closeDialog(true);
				return true;
			});
		}
		if (btnSelectionCite != null)
			btnSelectionCite.setOnClickListener(v -> {
				saveUserDic(true, "", selection, mCoolReader, mReaderView, "");
				closeDialog(true);
			});
		if (llSliderTop != null) new BoundControlListener(llSliderTop.findViewById(R.id.selection_bound_control_t), true);
		if (llSliderBottom != null) new BoundControlListener(llSliderBottom.findViewById(R.id.selection_bound_control_b), false);
		RepeatOnTouchListener lsnrStartSmallStepPlus = new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(true, SELECTION_SMALL_STEP));
		if (llSliderTop != null) llSliderTop.findViewById(R.id.btn_next_t).setOnTouchListener(lsnrStartSmallStepPlus);
		llMiddleContents.findViewById(R.id.middle_contents_top_h3).setOnTouchListener(lsnrStartSmallStepPlus);
		if ((!mCoolReader.isEInk) && (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0))
			llMiddleContents.findViewById(R.id.middle_contents_top_h3).setBackgroundResource(R.drawable.view_bg_dashed_border_transp);
		if (llSliderTop != null)
			if (mCoolReader.isEInk) Utils.setBtnBackground(llSliderTop.findViewById(R.id.btn_next_t), null, mCoolReader.isEInk);
		RepeatOnTouchListener lsnrStartSmallStepMinus = new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(true, -SELECTION_SMALL_STEP));
		if (llSliderTop != null) llSliderTop.findViewById(R.id.btn_prev_t).setOnTouchListener(lsnrStartSmallStepMinus);
		llMiddleContents.findViewById(R.id.middle_contents_top_h2).setOnTouchListener(lsnrStartSmallStepMinus);
		if ((!mCoolReader.isEInk) && (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0))
			llMiddleContents.findViewById(R.id.middle_contents_top_h2).setBackgroundResource(R.drawable.view_bg_dashed_border_transp);
		if (llSliderTop != null)
			if (mCoolReader.isEInk) Utils.setBtnBackground(llSliderTop.findViewById(R.id.btn_prev_t), null, mCoolReader.isEInk);
		RepeatOnTouchListener lsnrStartSentenceStepPlus = new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(true, SELECTION_NEXT_SENTENCE_STEP));
		if (llSliderTop != null) llSliderTop.findViewById(R.id.btn_next_sent_t).setOnTouchListener(lsnrStartSentenceStepPlus);
		llMiddleContents.findViewById(R.id.middle_contents_top_h4).setOnTouchListener(lsnrStartSentenceStepPlus);
		if (llSliderTop != null)
			if (mCoolReader.isEInk) Utils.setBtnBackground(llSliderTop.findViewById(R.id.btn_next_sent_t), null, mCoolReader.isEInk);
		RepeatOnTouchListener lsnrStartSentenceStepMinus = new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(true, -SELECTION_NEXT_SENTENCE_STEP));
		if (llSliderTop != null) llSliderTop.findViewById(R.id.btn_prev_sent_t).setOnTouchListener(lsnrStartSentenceStepMinus);
		llMiddleContents.findViewById(R.id.middle_contents_top_h1).setOnTouchListener(lsnrStartSentenceStepMinus);
		if (llSliderTop != null)
			if (mCoolReader.isEInk) Utils.setBtnBackground(llSliderTop.findViewById(R.id.btn_prev_sent_t), null, mCoolReader.isEInk);
		RepeatOnTouchListener lsnrFinishSmallStepPlus = new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(false, SELECTION_SMALL_STEP));
		if (llSliderBottom != null) llSliderBottom.findViewById(R.id.btn_next_b).setOnTouchListener(lsnrFinishSmallStepPlus);
		llMiddleContents.findViewById(R.id.middle_contents_bottom_h3).setOnTouchListener(lsnrFinishSmallStepPlus);
		if ((!mCoolReader.isEInk) && (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0))
			llMiddleContents.findViewById(R.id.middle_contents_bottom_h3).setBackgroundResource(R.drawable.view_bg_dashed_border_transp);
		if (llSliderBottom != null)
			if (mCoolReader.isEInk) Utils.setBtnBackground(llSliderBottom.findViewById(R.id.btn_next_b), null, mCoolReader.isEInk);
		RepeatOnTouchListener lsnrFinishSmallStepMinus = new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(false, -SELECTION_SMALL_STEP));
		if (llSliderBottom != null) llSliderBottom.findViewById(R.id.btn_prev_b).setOnTouchListener(lsnrFinishSmallStepMinus);
		llMiddleContents.findViewById(R.id.middle_contents_bottom_h2).setOnTouchListener(lsnrFinishSmallStepMinus);
		if ((!mCoolReader.isEInk) && (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0))
			llMiddleContents.findViewById(R.id.middle_contents_bottom_h2).setBackgroundResource(R.drawable.view_bg_dashed_border_transp);
		if (llSliderBottom != null)
			if (mCoolReader.isEInk) Utils.setBtnBackground(llSliderBottom.findViewById(R.id.btn_prev_b), null, mCoolReader.isEInk);
		RepeatOnTouchListener lsnrFinishSentenceStepPlus = new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(false, SELECTION_NEXT_SENTENCE_STEP));
		if (llSliderBottom != null)
			llSliderBottom.findViewById(R.id.btn_next_sent_b).setOnTouchListener(lsnrFinishSentenceStepPlus);
		llMiddleContents.findViewById(R.id.middle_contents_bottom_h4).setOnTouchListener(lsnrFinishSentenceStepPlus);
		if (llSliderBottom != null)
			if (mCoolReader.isEInk) Utils.setBtnBackground(llSliderBottom.findViewById(R.id.btn_next_sent_b), null, mCoolReader.isEInk);
		RepeatOnTouchListener lsnrFinishSentenceStepMinus = new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(false, -SELECTION_NEXT_SENTENCE_STEP));
		if (llSliderBottom != null) llSliderBottom.findViewById(R.id.btn_prev_sent_b).setOnTouchListener(lsnrFinishSentenceStepMinus);
		llMiddleContents.findViewById(R.id.middle_contents_bottom_h1).setOnTouchListener(lsnrFinishSentenceStepMinus);
		if (llSliderBottom != null)
			if (mCoolReader.isEInk) Utils.setBtnBackground(llSliderBottom.findViewById(R.id.btn_prev_sent_b), null, mCoolReader.isEInk);
		if (btnSelectionMore != null)
			btnSelectionMore.setOnClickListener(v -> toggleAddButtons(false));
	}

	private void initRecentDics() {
		if ((llRecentDics == null) && (svRecentDics == null)) return;
		String sDicsPanel = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_RECENT_DICS, "0");
		ViewGroup flRecentDics;
		if (sDicsPanel.equals("1"))
			flRecentDics = svRecentDics.findViewById(R.id.ll_dic_buttons);
		else
			flRecentDics = llRecentDics.findViewById(R.id.fl_recent_dics);
		int newTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
		int iCntRecent = 0;
		if (flRecentDics!=null) {
			flRecentDics.removeAllViews();
			for (final Dictionaries.DictInfo di : mCoolReader.mDictionaries.diRecent) {
				if (!di.equals(mCoolReader.mDictionaries.getCurDictionary())) {
					iCntRecent++;
				}
			}
		}
		if (iCntRecent == 0) iCntRecent++;
		if (flRecentDics!=null) {
			List<Dictionaries.DictInfo> diAllDicts = new ArrayList<>();
			for (final Dictionaries.DictInfo di: mCoolReader.mDictionaries.diRecent) {
				diAllDicts.add(di);
			}
			for (final Dictionaries.DictInfo di: mCoolReader.mDictionaries.getAddDicts()) {
				diAllDicts.add(di);
			}
			List<Dictionaries.DictInfo> dicts = Dictionaries.getDictList(mCoolReader);
			for (Dictionaries.DictInfo dict : dicts) {
				boolean bUseDic = mReaderView.getSettings().getBool(Settings.PROP_DIC_LIST_MULTI+"."+dict.id,false);
				if (bUseDic) {
					boolean bWas = false;
					for (Dictionaries.DictInfo di: diAllDicts) {
						if (di.id.equals(dict.id)) {
							bWas = true;
							break;
						}
					}
					if (!bWas) diAllDicts.add(dict);
				}
			}
			ArrayList<String> added = new ArrayList<>();
			// add lang pos
			String sFrom = mReaderView.mBookInfo.getFileInfo().lang_from;
			String sTo = mReaderView.mBookInfo.getFileInfo().lang_to;
			String sFromTo = mCoolReader.getString(R.string.book_info_section_book_translation_langs);
			if ((!StrUtils.isEmptyStr(sFrom)) || (!StrUtils.isEmptyStr(sTo)))
				sFromTo = StrUtils.getNonEmptyStr(sFrom, true) + " -> " +
						StrUtils.getNonEmptyStr(sTo, true);
			Button dicButton = new Button(mCoolReader);
			dicButton.setText(sFromTo);
			dicButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
			//dicButton.setHeight(dicButton.getHeight()-4);
			dicButton.setTextColor(mCoolReader.getTextColor(colorIcon));
			if (!sTranspButtons.equals("0")) dicButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
			else dicButton.setBackgroundColor(Color.argb(255, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
			if (sDicsPanel.equals("1"))
				dicButton.setPadding(5, 20, 5, 20);
			else
				dicButton.setPadding(10, 20, 10, 20);
			//dicButton.setBackground(null);
			LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			llp.setMargins(8, 4, 4, 4);
			dicButton.setLayoutParams(llp);
			//dicButton.setMaxWidth((mReaderView.getRequestedWidth() - 20) / iCntRecent); // This is not needed anymore - since we use FlowLayout
			dicButton.setMaxLines(1);
			dicButton.setEllipsize(TextUtils.TruncateAt.END);
			flRecentDics.addView(dicButton);
			Button finalDicButton = dicButton;
			if (mCoolReader.isEInk) Utils.setBtnBackground(dicButton, null, mCoolReader.isEInk);
			dicButton.setOnClickListener(v -> {
				mCoolReader.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL, false,
						mReaderView.getSurface(), mReaderView.getBookInfo().getFileInfo().parent,
						mReaderView.getBookInfo().getFileInfo(),
						StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_from, true),
						StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_to, true),
						"", null, TranslationDirectionDialog.FOR_COMMON, s -> {
							finalDicButton.setText(s);
						});
			});
			TextView tv = new TextView(mCoolReader);
			tv.setText(" ");
			if (sDicsPanel.equals("1"))
				tv.setPadding(1, 10, 1, 10);
			else
				tv.setPadding(10, 10, 10, 10);
			tv.setLayoutParams(llp);
			tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
			tv.setTextColor(mCoolReader.getTextColor(colorIcon));
			flRecentDics.addView(tv);
			//\
			for (final Dictionaries.DictInfo di: diAllDicts) {
				if (!added.contains(di.id)) {
					added.add(di.id);
					dicButton = new Button(mCoolReader);
					String sAdd = di.getAddText(mCoolReader);
					String sName = di.shortName;
					if (StrUtils.isEmptyStr(sName)) sName = di.name;
					if (StrUtils.isEmptyStr(sAdd))
						dicButton.setText(sName);
					else
						dicButton.setText(sName + ": " + sAdd);
					dicButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
					//dicButton.setHeight(dicButton.getHeight()-4);
					dicButton.setTextColor(mCoolReader.getTextColor(colorIcon));
					if (!sTranspButtons.equals("0")) dicButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
					else dicButton.setBackgroundColor(Color.argb(255, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
					if (sDicsPanel.equals("1"))
						dicButton.setPadding(5, 20, 5, 20);
					else
						dicButton.setPadding(10, 20, 10, 20);
					//dicButton.setBackground(null);
					llp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					llp.setMargins(8, 4, 4, 8);
					dicButton.setLayoutParams(llp);
					//dicButton.setMaxWidth((mReaderView.getRequestedWidth() - 20) / iCntRecent); // This is not needed anymore - since we use FlowLayout
					dicButton.setMaxLines(1);
					dicButton.setEllipsize(TextUtils.TruncateAt.END);
					tv = new TextView(mCoolReader);
					tv.setText(" ");
					if (sDicsPanel.equals("1"))
						tv.setPadding(1, 10, 1, 10);
					else
						tv.setPadding(10, 10, 10, 10);
					tv.setLayoutParams(llp);
					tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
					tv.setTextColor(mCoolReader.getTextColor(colorIcon));
					flRecentDics.addView(dicButton);
					flRecentDics.addView(tv);
					dicButton.setOnClickListener(v -> {
						mCoolReader.mDictionaries.setAdHocDict(di);
						String sSText = selection.text;
						mCoolReader.findInDictionary(sSText, false,null);
						if (!mReaderView.getSettings().getBool(mReaderView.PROP_APP_SELECTION_PERSIST, false))
							mReaderView.clearSelection();
						closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
					});
					dicButton.setOnLongClickListener(v -> {
						mCoolReader.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_ONLY_CHOOSE_QUICK, false,
								mReaderView.getSurface(), mReaderView.getBookInfo().getFileInfo().parent,
								mReaderView.getBookInfo().getFileInfo(),
								StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_from, true),
								StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_to, true),
								"", null, TranslationDirectionDialog.FOR_COMMON, s -> {
									mCoolReader.mDictionaries.setAdHocDict(di);
									String sSText = selection.text;
									mCoolReader.findInDictionary(sSText, false, null);
									if (!mReaderView.getSettings().getBool(mReaderView.PROP_APP_SELECTION_PERSIST, false))
										mReaderView.clearSelection();
									closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
								});
						return true;
					});
					if (mCoolReader.isEInk) Utils.setBtnBackground(dicButton, null, mCoolReader.isEInk);
				}
			}
		}
	}

	public SelectionToolbarDlg(CoolReader coolReader, ReaderView readerView, Selection sel, boolean isShort)
	{
		this.selection = sel;
		stSel = selection;
		mCoolReader = coolReader;
		mIsHorz = Dips.isHorizontalAndWide();
		props = new Properties(mCoolReader.settings());
		mReaderView = readerView;
		mAnchor = readerView.getSurface();
		mIsShort = isShort;

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar, null));
		llAddButtons = (LinearLayout) (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar_add, null));
		llSliderBottom = (LinearLayout) (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar_slider_bottom, null));
		llSliderTop = (LinearLayout) (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar_slider_top, null));
		llRecentDics = (LinearLayout) (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar_recent_dics, null));
		svRecentDics = (LinearLayout) (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.recent_dic_panel_scroll, null));

		if (mIsHorz) {
			llButtonsRow = (LinearLayout) (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar_wide_buttons_row, null));
			llButtonsRow2 = null;
			llAddButtons = llButtonsRow;
		} else {
			llButtonsRow = (LinearLayout) (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar_buttons_row, null));
			llButtonsRow2 = llButtonsRow.findViewById(R.id.ll_second_buttons_row);
		}

		llTopLine = panel.findViewById(R.id.top_line);
		llTopLine.setOnClickListener(v -> {
		});
		llMiddleContents = panel.findViewById(R.id.middle_contents);
		llMiddleContents.setOnClickListener(v -> {
			closeDialog(true);
		});
		llBottomLine = panel.findViewById(R.id.bottom_line);
		llBottomLine.setOnClickListener(v -> {
		});

		panel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		mWindow = new PopupWindow(mAnchor.getContext());

		mWindow.setTouchInterceptor((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
				closeDialog(true);
				return true;
			}
			return false;
		});

		colorButtons = new ColorDrawable(colorGrayC);
		sTranspButtons = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_TRANSP_BUTTONS, "0");
		if (!sTranspButtons.equals("0")) colorButtons.setAlpha(130);
			else colorButtons.setAlpha(255);
		mPanel = panel;

		panel.setBackgroundColor(Color.argb(0, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));

		String sBkg = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_BACKGROUND, "0");

		alphaVal = 0;
		if (sBkg.equals("0")) alphaVal = 170;
		if (sBkg.equals("2")) alphaVal = 255;

		// determine top or bottom aligned toolbar
		showAtTop = isShowAtTop();
		// place layouts
		placeLayouts();
		// finally show the window
		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		mWindow.setContentView(panel);

		mPanel.setFocusable(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_BACK:
					closeDialog(true);
					return true;
				}
			}
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
			return false;
		});

		mWindow.setOnDismissListener(() -> {
			restoreReaderMode();
			isVisibleNow = false;
			mReaderView.toggleScreenUpdateModeMode(false);
			if (mCoolReader.getmReaderFrame() != null)
				mCoolReader.updateUserDicWords();
			else
				mReaderView.clearSelection();
		});

		mWindow.setBackgroundDrawable(new BitmapDrawable());

		mReaderView.toggleScreenUpdateModeMode(true);

		if (showAtTop)
			mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], 0);
		else
			mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);


		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.FILL_PARENT);

		if (showAtTop)
			mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], 0);
		else
			mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);

		isVisibleNow = true;
		mCoolReader.tintViewIcons(mPanel);
	}

}
