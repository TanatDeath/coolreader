package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.dic.Dictionaries;
import org.coolreader.R;
import org.coolreader.dic.TranslationDirectionDialog;
import org.coolreader.layouts.FlowLayout;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.os.Build;
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
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.CRC32;

public class SelectionToolbarDlg {
	PopupWindow mWindow;
	View mAnchor;
	CoolReader mCoolReader;
	ReaderView mReaderView;
	View mPanel;
	public static Selection stSel;
	Selection selection;
	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;
	public static boolean isVisibleNow = false;

	static public void showDialog(CoolReader coolReader, ReaderView readerView, final Selection selection)
	{
		SelectionToolbarDlg dlg = new SelectionToolbarDlg(coolReader, readerView, selection);
		//dlg.mWindow.update(dlg.mAnchor, width, height)
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.showAtLocation(readerView, Gravity.LEFT|Gravity.TOP, readerView.getLeft()+50, readerView.getTop()+50);
		//dlg.showAsDropDown(readerView);
		//dlg.update();
	}

	private boolean pageModeSet = false;
	private boolean changedPageMode;
	private void setReaderMode()
	{
		if (pageModeSet)
			return;
		//if (DeviceInfo.EINK_SCREEN) { return; } // switching to scroll view doesn't work well on eink screens
		
		String oldViewSetting = mReaderView.getSetting( ReaderView.PROP_PAGE_VIEW_MODE );
		if ( "1".equals(oldViewSetting) ) {
			changedPageMode = true;
			mReaderView.setViewModeNonPermanent(ViewMode.SCROLL);
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
		mReaderView.toggleScreenUpdateModeMode();
		mWindow.dismiss();
	}

	LinearLayout llAddButtonsParent = null;
	LinearLayout llAddButtons = null;

	public boolean addButtonEnabled = false;
	private Properties props;

	public static void saveUserDic(boolean isCitation, String text, Selection selection, CoolReader mCoolReader, ReaderView mReaderView) {
		UserDicEntry ude = new UserDicEntry();
		ude.setId(0L);
		ude.setDic_word(selection.text);
		if (isCitation)
			ude.setDic_word_translate(BookmarkEditDialog.getContextText(mCoolReader, selection.text));
		else
			ude.setDic_word_translate(text);
		final String sBookFName = mReaderView.mBookInfo.getFileInfo().getFilename();
		CRC32 crc = new CRC32();
		crc.update(sBookFName.getBytes());
		ude.setDic_from_book(String.valueOf(crc.getValue()));
		ude.setCreate_time(System.currentTimeMillis());
		ude.setLast_access_time(System.currentTimeMillis());
		ude.setLanguage(mReaderView.mBookInfo.getFileInfo().language);
		ude.setShortContext(BookmarkEditDialog.getContextText(mCoolReader, text));
		ude.setFullContext(BookmarkEditDialog.getFullContextText(mCoolReader));
		ude.setIsCustomColor(0);
		ude.setCustomColor(Utils.colorToHex(0));
		ude.setSeen_count(0L);
		ude.setIs_citation(isCitation? 1 : 0);
		mCoolReader.getDB().saveUserDic(ude, UserDicEntry.ACTION_NEW);
		mCoolReader.getmUserDic().put(ude.getIs_citation()+ude.getDic_word(), ude);
		BackgroundThread.instance().postGUI(() -> {
			if (isCitation)
				mCoolReader.showToast(R.string.citation_created);
			else
				mCoolReader.showToast(R.string.user_dic_entry_created);
			mCoolReader.getmReaderFrame().getUserDicPanel().updateUserDicWords();
		}, 1000);
	}

	public static void sendTo1(String text, Selection selection, CoolReader mCoolReader) {
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		String text1 = BookmarkEditDialog.getSendToText1(mCoolReader,
				selection.text,
				BookmarkEditDialog.getContextText(mCoolReader, selection.text));
		String text2 = BookmarkEditDialog.getSendToText2(mCoolReader,
				selection.text,
				text,
				BookmarkEditDialog.getContextText(mCoolReader, selection.text));
		emailIntent.setType("text/plain");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, text1);
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text2);
		mCoolReader.startActivity(Intent.createChooser(emailIntent, null));
	}

	public static void sendTo2(String text, Selection selection, CoolReader mCoolReader) {
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		int selAction = -1;
		if (mCoolReader.getReaderView() != null) selAction = mCoolReader.getReaderView().mBookmarkActionSendTo;
		if (selAction == -1) {
			sendTo1(text, selection, mCoolReader);
			return;
		}
		int titleSendTo =  OptionsDialog.getSelectionActionTitle(selAction);
		Dictionaries.DictInfo curDict = OptionsDialog.getDicValue(mCoolReader.getString(titleSendTo), mCoolReader.settings(), mCoolReader);
		if (curDict == null) {
			sendTo1(text, selection, mCoolReader);
			return;
		}
		String text1 = BookmarkEditDialog.getSendToText1(mCoolReader,
			selection.text,
			BookmarkEditDialog.getContextText(mCoolReader, selection.text));
		String text2 = BookmarkEditDialog.getSendToText2(mCoolReader,
				selection.text,
				text,
				BookmarkEditDialog.getContextText(mCoolReader, selection.text));
		emailIntent.setType("text/plain");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, text1);
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text2);
		for (ResolveInfo resolveInfo : mCoolReader.getPackageManager().queryIntentActivities(emailIntent, 0)) {
			if( resolveInfo.activityInfo.packageName.contains(curDict.packageName)){
				emailIntent.setComponent(new ComponentName(
						resolveInfo.activityInfo.packageName,
						resolveInfo.activityInfo.name));
				try
				{
					mCoolReader.startActivity(emailIntent);
				} catch ( ActivityNotFoundException e ) {
					sendTo1(text, selection, mCoolReader);
					return;
				}
			}

		}
	}

	public void toggleAddButtons(boolean dontChange) {
		if (llAddButtonsParent == null) return;
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
		if (addButtonEnabled) {
			llAddButtonsParent.removeAllViews();
			llAddButtonsParent.addView(llAddButtons);
			int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
			ColorDrawable c = new ColorDrawable(colorGrayC);
			String sTranspButtons = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_TRANSP_BUTTONS, "0");
			if (!sTranspButtons.equals("0")) c.setAlpha(130);
				else c.setAlpha(255);
			llAddButtons.findViewById(R.id.btn_quick_bookmark).setBackgroundDrawable(c);
			llAddButtons.findViewById(R.id.btn_quick_bookmark).setOnClickListener(v -> {
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
			llAddButtons.findViewById(R.id.btn_cite).setBackgroundDrawable(c);
			llAddButtons.findViewById(R.id.btn_cite).setOnClickListener(v -> {
				saveUserDic(true, "", selection, mCoolReader, mReaderView);
				closeDialog(true);
			});
			llAddButtons.findViewById(R.id.btn_user_dic).setBackgroundDrawable(c);
			llAddButtons.findViewById(R.id.btn_user_dic).setOnClickListener(v -> {
				mReaderView.showNewBookmarkDialog(selection, Bookmark.TYPE_USER_DIC, "");
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			});
			llAddButtons.findViewById(R.id.btn_web_search).setBackgroundDrawable(c);
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
			llAddButtons.findViewById(R.id.btn_dic_list).setBackgroundDrawable(c);
			llAddButtons.findViewById(R.id.btn_dic_list).setOnClickListener(v -> {
				DictsDlg dlg = new DictsDlg(mCoolReader, mReaderView, selection.text, null);
				dlg.show();
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			});
			llAddButtons.findViewById(R.id.btn_combo).setBackgroundDrawable(c);
			llAddButtons.findViewById(R.id.btn_combo).setOnClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mCoolReader.showToast(R.string.only_in_premium);
					return;
				}
				Dictionaries.DictInfo di = mCoolReader.getCurOrFirstOnlineDic();
				if (di != null) {
					if (!StrUtils.isEmptyStr(selection.text)) {
						mCoolReader.mDictionaries.setAdHocDict(di);
						mCoolReader.findInDictionary(selection.text, null, new CoolReader.DictionaryCallback() {

							@Override
							public boolean showDicToast() {
								return true;
							}

							@Override
							public boolean saveToHist() {
								return false;
							}

							@Override
							public void done(String result) {
								saveUserDic(false, result, selection, mCoolReader, mReaderView);
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
			llAddButtons.findViewById(R.id.btn_super_combo).setBackgroundDrawable(c);
			llAddButtons.findViewById(R.id.btn_super_combo).setOnClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mCoolReader.showToast(R.string.only_in_premium);
					return;
				}
				Dictionaries.DictInfo di = mCoolReader.getCurOrFirstOnlineDic();
				if (di != null) {
					if (!StrUtils.isEmptyStr(selection.text)) {
						mCoolReader.mDictionaries.setAdHocDict(di);
						mCoolReader.findInDictionary(selection.text, null, new CoolReader.DictionaryCallback() {

							@Override
							public boolean showDicToast() {
								return false;
							}

							@Override
							public boolean saveToHist() {
								return false;
							}

							@Override
							public void done(String result) {
								saveUserDic(false, result, selection, mCoolReader, mReaderView);
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
			llAddButtonsParent.removeAllViews();
		}
		mCoolReader.tintViewIcons(mPanel);
	}

	public SelectionToolbarDlg(CoolReader coolReader, ReaderView readerView, Selection sel )
	{
		this.selection = sel;
		stSel = selection;
		mCoolReader = coolReader;
		props = new Properties(mCoolReader.settings());
		mReaderView = readerView;
		mAnchor = readerView.getSurface();
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(coolReader, isEInk);

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar, null));
		llAddButtons = (LinearLayout) (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar_add, null));

		panel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		mWindow = new PopupWindow( mAnchor.getContext() );

		mWindow.setTouchInterceptor((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
				closeDialog(true);
				return true;
			}
			return false;
		});
		//super(panel);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGray = themeColors.get(R.attr.colorThemeGray2);
		int colorIcon = themeColors.get(R.attr.colorIcon);

		ColorDrawable c = new ColorDrawable(colorGrayC);
		String sTranspButtons = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_TRANSP_BUTTONS, "0");
		if (!sTranspButtons.equals("0")) c.setAlpha(130);
			else c.setAlpha(255);
		mPanel = panel;
		llAddButtonsParent = panel.findViewById(R.id.ll_sel_add);

		String sBkg = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_BACKGROUND, "0");
		if (sBkg.equals("0")) panel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
		if (sBkg.equals("1")) panel.setBackgroundColor(Color.argb(0, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
		if (sBkg.equals("2")) panel.setBackgroundColor(Color.argb(255, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
//		if (isEInk)
//			panel.setBackgroundColor(Color.argb(200, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC)));
//		else
//			panel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));

		//mPanel.findViewById(R.id.selection_copy).setBackgroundColor(colorGrayC);
		mPanel.findViewById(R.id.selection_copy).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_copy).setOnClickListener(v -> {
			mReaderView.copyToClipboard(selection.text);
			closeDialog(true);
		});

		String sExt = mCoolReader.settings().getProperty(Settings.PROP_APP_OPTIONS_EXT_SELECTION_TOOLBAR, "0");
		addButtonEnabled = StrUtils.getNonEmptyStr(sExt, true).equals("1");

		if (addButtonEnabled) toggleAddButtons(true);
		//recent dics
		FlowLayout llRecentDics = mPanel.findViewById(R.id.recentDics);
		//llRecentDics.setOrientation(LinearLayout.VERTICAL);
		int newTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
		int iCntRecent = 0;
		if (llRecentDics!=null) {
			for (final Dictionaries.DictInfo di : mCoolReader.mDictionaries.diRecent) {
				if (!di.equals(mCoolReader.mDictionaries.getCurDictionary())) {
					iCntRecent++;
				}
			}
		}
		if (iCntRecent == 0) iCntRecent++;
		if (llRecentDics!=null) {
			List<Dictionaries.DictInfo> diAllDicts = new ArrayList<>();
			for (final Dictionaries.DictInfo di: mCoolReader.mDictionaries.diRecent) {
				diAllDicts.add(di);
			}
			for (final Dictionaries.DictInfo di: mCoolReader.mDictionaries.getAddDicts()) {
				diAllDicts.add(di);
			}
			ArrayList<String> added = new ArrayList<>();
			// add lang pos
			String sFrom = mReaderView.mBookInfo.getFileInfo().lang_from;
			String sTo = mReaderView.mBookInfo.getFileInfo().lang_to;
			if ((!StrUtils.isEmptyStr(sFrom)) || (!StrUtils.isEmptyStr(sTo))) {
				String sFromTo = StrUtils.getNonEmptyStr(sFrom, true) + " -> " +
						StrUtils.getNonEmptyStr(sTo, true);
				Button dicButton = new Button(mCoolReader);
				dicButton.setText(sFromTo);
				dicButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
				//dicButton.setHeight(dicButton.getHeight()-4);
				dicButton.setTextColor(colorIcon);
				if (!sTranspButtons.equals("0")) dicButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
				else dicButton.setBackgroundColor(Color.argb(255, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
				dicButton.setPadding(10, 10, 10, 10);
				//dicButton.setBackground(null);
				LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				llp.setMargins(8, 4, 4, 8);
				dicButton.setLayoutParams(llp);
				//dicButton.setMaxWidth((mReaderView.getRequestedWidth() - 20) / iCntRecent); // This is not needed anymore - since we use FlowLayout
				dicButton.setMaxLines(1);
				dicButton.setEllipsize(TextUtils.TruncateAt.END);
				llRecentDics.addView(dicButton);
				dicButton.setOnClickListener(v -> {
					mCoolReader.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_NORMAL,
							mReaderView.getSurface(), mReaderView.getBookInfo().getFileInfo().parent,
							mReaderView.getBookInfo().getFileInfo(),
							StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_from, true),
							StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_to, true),
							"", null, TranslationDirectionDialog.FOR_COMMON, s -> {
								dicButton.setText(s);
							});
				});
				TextView tv = new TextView(mCoolReader);
				tv.setText(" ");
				tv.setPadding(10, 10, 10, 10);
				tv.setLayoutParams(llp);
				tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
				tv.setTextColor(colorIcon);
				llRecentDics.addView(tv);
			}
			//\
			for (final Dictionaries.DictInfo di: diAllDicts) {
				if (!added.contains(di.id)) {
					added.add(di.id);
					Button dicButton = new Button(mCoolReader);
					String sAdd = di.getAddText(mCoolReader);
					if (StrUtils.isEmptyStr(sAdd))
						dicButton.setText(di.name);
					else
						dicButton.setText(di.name + ": " + sAdd);
					dicButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
					//dicButton.setHeight(dicButton.getHeight()-4);
					dicButton.setTextColor(colorIcon);
					if (!sTranspButtons.equals("0")) dicButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
						else dicButton.setBackgroundColor(Color.argb(255, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
					dicButton.setPadding(10, 10, 10, 10);
					//dicButton.setBackground(null);
					LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					llp.setMargins(8, 4, 4, 8);
					dicButton.setLayoutParams(llp);
					//dicButton.setMaxWidth((mReaderView.getRequestedWidth() - 20) / iCntRecent); // This is not needed anymore - since we use FlowLayout
					dicButton.setMaxLines(1);
					dicButton.setEllipsize(TextUtils.TruncateAt.END);
					TextView tv = new TextView(mCoolReader);
					tv.setText(" ");
					tv.setPadding(10, 10, 10, 10);
					tv.setLayoutParams(llp);
					tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
					tv.setTextColor(colorIcon);
					llRecentDics.addView(dicButton);
					llRecentDics.addView(tv);
					dicButton.setOnClickListener(v -> {
						mCoolReader.mDictionaries.setAdHocDict(di);
						String sSText = selection.text;
						mCoolReader.findInDictionary(sSText, null);
						if (!mReaderView.getSettings().getBool(mReaderView.PROP_APP_SELECTION_PERSIST, false))
							mReaderView.clearSelection();
						closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
					});
					dicButton.setOnLongClickListener(v -> {
						mCoolReader.editBookTransl(CoolReader.EDIT_BOOK_TRANSL_ONLY_CHOOSE_QUICK,
								mReaderView.getSurface(), mReaderView.getBookInfo().getFileInfo().parent,
								mReaderView.getBookInfo().getFileInfo(),
								StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_from, true),
								StrUtils.getNonEmptyStr(mReaderView.mBookInfo.getFileInfo().lang_to, true),
								"", null, TranslationDirectionDialog.FOR_COMMON, s -> {
									//mCoolReader.showToast(s);
									mCoolReader.mDictionaries.setAdHocDict(di);
									String sSText = selection.text;
									mCoolReader.findInDictionary(sSText, null);
									if (!mReaderView.getSettings().getBool(mReaderView.PROP_APP_SELECTION_PERSIST, false))
										mReaderView.clearSelection();
									closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
								});
						return true;
					});
				}
			}
		}

		mPanel.findViewById(R.id.selection_dict).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_dict).setOnClickListener(v -> {
			//PositionProperties currpos = mReaderView.getDoc().getPositionProps(null);
			//Log.e("CURPOS", currpos.pageText);
			if (mCoolReader.ismDictLongtapChange()) {
				DictsDlg dlg = new DictsDlg(mCoolReader, mReaderView, selection.text, null);
				dlg.show();
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			} else {
				mCoolReader.findInDictionary( selection.text , null);
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			}
		});

		mPanel.findViewById(R.id.selection_dict).setOnLongClickListener(v -> {
			if (!mCoolReader.ismDictLongtapChange()) {
				DictsDlg dlg = new DictsDlg(mCoolReader, mReaderView, selection.text, null);
				dlg.show();
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			} else {
				mCoolReader.findInDictionary( selection.text , null);
				closeDialog(!mReaderView.getSettings().getBool(ReaderView.PROP_APP_SELECTION_PERSIST, false));
			}
			return true;
		});

		mPanel.findViewById(R.id.selection_bookmark).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_bookmark).setOnClickListener(v -> {
			mReaderView.showNewBookmarkDialog(selection,Bookmark.TYPE_COMMENT, "");
			closeDialog(true);
		});

		mPanel.findViewById(R.id.selection_bookmark).setOnLongClickListener(v -> {
			BookmarksDlg dlg = new BookmarksDlg(mCoolReader, mReaderView, false, null);
			dlg.show();
			closeDialog(true);
			return true;
		});

		mPanel.findViewById(R.id.selection_email).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_email).setOnClickListener(v -> {
			mReaderView.sendQuotationInEmail(selection);
			closeDialog(true);
		});
		mPanel.findViewById(R.id.selection_find).setOnClickListener(v -> {
			mReaderView.showSearchDialog(selection.text.trim());
			closeDialog(true);
		});

		mPanel.findViewById(R.id.selection_find).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_find).setOnLongClickListener(v -> {
			final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
			emailIntent.putExtra(SearchManager.QUERY, selection.text.trim());
			mCoolReader.startActivity(emailIntent);
			closeDialog(true);
			return true;
		});
		mPanel.findViewById(R.id.selection_cancel).setBackgroundDrawable(c);
		mPanel.findViewById(R.id.selection_cancel).setOnClickListener(v -> closeDialog(true));
		new BoundControlListener(mPanel.findViewById(R.id.selection_left_bound_control), true);
		new BoundControlListener(mPanel.findViewById(R.id.selection_right_bound_control), false);
		mPanel.findViewById(R.id.selection_more).setBackgroundDrawable(null);

		mPanel.findViewById(R.id.btn_next1).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(true, SELECTION_SMALL_STEP)));
		mPanel.findViewById(R.id.btn_next2).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(false, SELECTION_SMALL_STEP)));
		mPanel.findViewById(R.id.btn_prev1).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(true, -SELECTION_SMALL_STEP)));
		mPanel.findViewById(R.id.btn_prev2).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(false, -SELECTION_SMALL_STEP)));
		mPanel.findViewById(R.id.btn_next_sent1).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(true, SELECTION_NEXT_SENTENCE_STEP)));
		mPanel.findViewById(R.id.btn_next_sent2).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(false, SELECTION_NEXT_SENTENCE_STEP)));
		mPanel.findViewById(R.id.btn_prev_sent1).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(true, -SELECTION_NEXT_SENTENCE_STEP)));
		mPanel.findViewById(R.id.btn_prev_sent2).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> changeSelectionBound(false, -SELECTION_NEXT_SENTENCE_STEP)));
		mPanel.findViewById(R.id.selection_more).setOnClickListener(v -> toggleAddButtons(false));

		mPanel.setFocusable(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if ( event.getAction()==KeyEvent.ACTION_UP ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_BACK:
					closeDialog(true);
					return true;
//					case KeyEvent.KEYCODE_DPAD_LEFT:
//					case KeyEvent.KEYCODE_DPAD_UP:
//						//mReaderView.findNext(pattern, true, caseInsensitive);
//						return true;
//					case KeyEvent.KEYCODE_DPAD_RIGHT:
//					case KeyEvent.KEYCODE_DPAD_DOWN:
//					*	//mReaderView.findNext(pattern, false, caseInsensitive);
//						return true;
				}
			} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
					switch ( keyCode ) {
//						case KeyEvent.KEYCODE_BACK:
//						case KeyEvent.KEYCODE_DPAD_LEFT:
//						case KeyEvent.KEYCODE_DPAD_UP:
//						case KeyEvent.KEYCODE_DPAD_RIGHT:
//						case KeyEvent.KEYCODE_DPAD_DOWN:
//							return true;
					}
				}
			if ( keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
			return false;
		});

		mWindow.setOnDismissListener(() -> {
			restoreReaderMode();
			isVisibleNow = false;
			mReaderView.toggleScreenUpdateModeMode();
			mReaderView.clearSelection();
		});

		mWindow.setBackgroundDrawable(new BitmapDrawable());

		// plotn - I think this is not needed
//		if (!DeviceInfo.EINK_SCREEN) {
//			// transparent
//			mWindow.setBackgroundDrawable(new BitmapDrawable());
//		}
//		else {
//			// white background with rectangle
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//				mWindow.setBackgroundDrawable(mCoolReader.getDrawable(R.drawable.btn_default_normal_hc_light));
//			else
//				mWindow.setBackgroundDrawable(mCoolReader.getResources().getDrawable(R.drawable.btn_default_normal_hc_light));
//		}
		//mWindow.setAnimationStyle(android.R.style.Animation_Toast);
		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
//		setWidth(panel.getWidth());
//		setHeight(panel.getHeight());
		
		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		mWindow.setContentView(panel);
		
		
		int [] location = new int[2];
		mAnchor.getLocationOnScreen(location);
		//mWindow.update(location[0], location[1], mPanel.getWidth(), mPanel.getHeight() );
		//mWindow.setWidth(mPanel.getWidth());
		//mWindow.setHeight(mPanel.getHeight());
		int selectionTop = 0;
		int selectionBottom = 0;
		if (mReaderView.lastSelection != null) {
			selectionTop = mReaderView.lastSelection.startY;
			selectionBottom = mReaderView.lastSelection.endY;
		}
		int panelHeight = mPanel.getHeight();
		int surfaceHeight = mReaderView.getSurface().getHeight();

		if (selectionBottom<selectionTop) {
			int dummy = selectionBottom;
			selectionBottom = selectionTop;
			selectionTop = dummy;
		}
		int popupY = location[1] + mAnchor.getHeight() - mPanel.getHeight();

		if (
				(selectionTop > (mReaderView.getSurface().getHeight() / 2)) &&
				(selectionBottom > (mReaderView.getSurface().getHeight() / 2))
						/*&& -- for some reasons, mPanel Height = 0 here :(((
						((selectionTop > (surfaceHeight - panelHeight)) ||
						 (selectionBottom > (surfaceHeight - panelHeight)))*/
		)
			mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], 0);
		else
			mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
//		if ( mWindow.isShowing() )
//			mWindow.update(mAnchor, 50, 50);
		//dlg.mWindow.showAsDropDown(dlg.mAnchor);
		int y = sel.startY;
		if (y > sel.endY)
			y = sel.endY;
		int maxy = mReaderView.getSurface().getHeight() * 4 / 5;
		//plotn - since we have a transparent toolbar - this is not nessesary
//		if (y > maxy) {
//			setReaderMode(); // selection is overlapped by toolbar: set scroll mode and move
//			BackgroundThread.instance().postGUI(new Runnable() {
//				@Override
//				public void run() {
//					//mReaderView.doEngineCommand(ReaderCommand.DCMD_REQUEST_RENDER, 0);
//					BackgroundThread.instance().postBackground(new Runnable() {
//						@Override
//						public void run() {
//							BackgroundThread.instance().postGUI(new Runnable() {
//								@Override
//								public void run() {
//									mReaderView.doEngineCommand(ReaderCommand.DCMD_SCROLL_BY, mReaderView.getSurface().getHeight() / 3);
//									mReaderView.redraw();
//								}
//							});
//						}
//					});
//				}
//			});
//		}
		isVisibleNow = true;
		mReaderView.toggleScreenUpdateModeMode();
		mCoolReader.tintViewIcons(mPanel);
	}

}
