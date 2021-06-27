package org.coolreader.crengine;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class UserDicPanel extends LinearLayout implements Settings {
		private CoolReader activity;
		private LinearLayout content;
		private TextView lblWordFound;
		private TextView lblStar;
		private ArrayList<TextView> arrLblWords = new ArrayList<TextView>();

	public ArrayList<UserDicEntry> getArrUdeWords() {
		return arrUdeWords;
	}

	private ArrayList<UserDicEntry> arrUdeWords = new ArrayList<UserDicEntry>();
		private TextView lblWord;
		private int textSize = 14;
		private int color = 0;
		private int wc = 0;
		private boolean fullscreen;
		private boolean nightMode;

		FileInfo book;
		Bookmark position;
		PositionProperties props;

		public void updateFullscreen(boolean fullscreen) {
			if (this.fullscreen == fullscreen)
				return;
			this.fullscreen = fullscreen;
			requestLayout();
		}

		public boolean updateSettings(Properties props) {
			int newTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
			boolean needRelayout = (textSize != newTextSize);
			this.textSize = newTextSize;
			nightMode = props.getBool(PROP_NIGHT_MODE, false);
			this.color = props.getColor(Settings.PROP_STATUS_FONT_COLOR, 0);
			lblWordFound.setTextColor(0xFF000000 | color);
			Typeface tf = activity.getReaderFont();
			if (tf != null) {
				lblWordFound.setTypeface(tf);
				lblStar.setTypeface(tf);
			}
			int fontSize = activity.settings().getInt(Settings.PROP_FONT_SIZE_USER_DIC, 0);
			if (fontSize != 0) textSize = fontSize;
			for (TextView tv: arrLblWords) {
				tv.setTextColor(0xFF000000 | color);
				tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
				if (tf != null) tv.setTypeface(tf);
			}
			lblWordFound.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			lblStar.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			if (needRelayout) {
				CoolReader.log.d("changing user dic layout");
				lblWordFound.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				for (TextView tv: arrLblWords) {
					tv.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				}
				content.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				content.forceLayout();
				forceLayout();
			}
			invalidate();
			return needRelayout;
		}

		public UserDicPanel(CoolReader context) {
			super(context);
			this.activity = context;
			setOrientation(VERTICAL);
			
			this.color = context.settings().getColor(Settings.PROP_STATUS_FONT_COLOR, 0);
			
			LayoutInflater inflater = LayoutInflater.from(activity);
			int showUD = activity.settings().getInt(Settings.PROP_APP_SHOW_USER_DIC_PANEL, 0);
			if (showUD == 2)
				content = (LinearLayout)inflater.inflate(R.layout.user_dic_panel_scroll, null);
			else
				content = (LinearLayout)inflater.inflate(R.layout.user_dic_panel, null);
			lblWordFound = content.findViewById(R.id.word_found);
			lblStar = content.findViewById(R.id.tview_saving);
			lblStar.setText("#");
            lblStar.setTextColor(0xFF000000 | color);
            lblStar.setOnClickListener(v -> {
				if (activity.getReaderView()!=null)
					activity.getReaderView().scheduleSaveCurrentPositionBookmark(1);
					activity.showToast(activity.getString(R.string.pos_saved));
			});
			arrLblWords.clear();
			arrUdeWords.clear();
			lblWord = content.findViewById(R.id.word1);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word2);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word3);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word4);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word5);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word6);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word7);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word8);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word9);
			arrLblWords.add(lblWord);
			lblWord = content.findViewById(R.id.word10);
			arrLblWords.add(lblWord);

			lblWordFound.setText("");
			lblWordFound.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			lblWordFound.setTextColor(0xFF000000 | color);
            lblWordFound.setPaintFlags(lblWordFound.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

			lblWordFound.setOnClickListener(v -> {
				UserDicDlg dlg = new UserDicDlg(activity,0);
				dlg.show();
			});

			int i = 0;

			for (TextView tv: arrLblWords) {
				i++;
				tv.setText(String.valueOf(""));
				tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
				tv.setTextColor(0xFF000000 | color);
				tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				tv.setOnClickListener(v -> {
						if (v instanceof TextView) {
							String sWord = ((TextView) v).getText().toString();
							for (UserDicEntry ude: arrUdeWords) {
								String sKey = ude.getDic_word();
								try {
									String[] arrKey = sKey.split("~");
									sKey = arrKey[0];
									sKey = sKey.replace("|", "");
								} catch (Exception e) {

								}
								if (sKey.equals(sWord)) {
									activity.showSToast("*"+StrUtils.updateText(ude.getDic_word_translate(),true), sWord);
									activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_UPDATE_CNT);
									break;
								}
							}
						}
					});
				tv.setOnLongClickListener(v -> {
					if (v instanceof TextView) {
						String sWord = ((TextView) v).getText().toString();
						for (final UserDicEntry ude: arrUdeWords) {
							if (ude.getDic_word().equals(sWord)) {
								activity.askConfirmation(R.string.win_title_confirm_ude_delete, (Runnable) () -> {
									if (ude.getThisIsDSHE()) {
										DicSearchHistoryEntry dshe = new DicSearchHistoryEntry();
										dshe.setSearch_text(ude.getDic_word());
										activity.getDB().updateDicSearchHistory(dshe, DicSearchHistoryEntry.ACTION_DELETE, activity);
									}
									activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_DELETE);
									activity.getmUserDic().remove(ude.getIs_citation()+ude.getDic_word());
									activity.getmReaderFrame().getUserDicPanel().updateUserDicWords();
								});
								break;
							}
						}
					}
					return true;
				});
			}

			addView(content);
			onThemeChanged(context.getCurrentTheme());
			updateSettings(context.settings());
		}

		public void onThemeChanged(InterfaceTheme theme) {
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			content.measure(widthMeasureSpec, heightMeasureSpec);
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
		
		private static boolean empty(String s) {
			return s == null || s.length() == 0;
		}
		
		private static void append(StringBuilder buf, String text, String delimiter) {
			if (!Utils.empty(text)) {
				if (buf.length() != 0 && !empty(delimiter)) {
					buf.append(delimiter);
				}
				buf.append(text);
			}
		}
		
		public void updateCurrentPositionStatus(FileInfo book, Bookmark position, PositionProperties props) {
			this.book = book != null ? new FileInfo(book) : null;
			this.position = position != null ? new Bookmark(position) : null;
			this.props = props != null ? new PositionProperties(props) : null;
			BackgroundThread.instance().postGUI(new Runnable() {
				@Override
				public void run() {
					updateUserDicWords();
				}
			}, 500);
		}

		public void updateSavingMark(final String mark) {
			BackgroundThread.instance().postGUI(new Runnable() {
				@Override
				public void run() {
					lblStar.setTextColor(0xFF000000 | color);
					lblStar.setText(mark);
				}
			}, 500);
			BackgroundThread.instance().postGUI(new Runnable() {
				@Override
				public void run() {
					lblStar.setText("#");
                    lblStar.setTextColor(0xFF000000 | color);
				}
			}, 3000);
		}

		public String getCurPageText() {
			return getCurPageText(0, true);
		}

		public String getCurPageText(int offset, boolean toLower) {
			int curPage = activity.getReaderView().getDoc().getCurPage() + offset;
			//activity.getReaderView().CheckAllPagesLoad();
			String sPrevPage = "";
			String sPageText = "";
			sPageText = activity.getReaderView().getPageTextFromEngine(curPage);
			if (curPage > 0)
				sPrevPage = activity.getReaderView().getPageTextFromEngine(curPage - 1);
//			if ((activity.getReaderView().getArrAllPages().size()>curPage)&&(curPage>=0)) {
//				sPageText = activity.getReaderView().getArrAllPages().get(curPage);
//				if (curPage > 0)
//					sPrevPage = activity.getReaderView().getArrAllPages().get(curPage - 1);
//			}
			if (sPageText==null) sPageText = "";
			if (sPrevPage==null) sPrevPage = "";
			int iLen = 300;
			if (sPageText.length()<iLen) iLen = sPageText.length();
			if (sPrevPage.length()<iLen) iLen = sPrevPage.length();
			String sCurPage = sPageText;
			if (toLower) {
				sCurPage = sCurPage.toLowerCase();
				sPrevPage = sPrevPage.toLowerCase();
			}
			// check the text rafting
			boolean bRafting = false;
			for (int i = iLen - 1; i > 0; i--) {
				String s1 = sPrevPage.substring(sPrevPage.length()-i, sPrevPage.length()).trim();
				String s2 = sPageText.substring(0,i).trim();
				if (sPrevPage.substring(sPrevPage.length()-i, sPrevPage.length()).trim().equals(
						sPageText.substring(0,i).trim()
				)) bRafting = true;
			}
			if (!bRafting) {
				int cnt = 50;
				try {
					if (sPrevPage.length() > 10) {
						if (!Character.isWhitespace(sPrevPage.charAt(sPrevPage.length() - 1))) {
							for (int i = sPrevPage.length() - 1; i > 0; i--) {
								cnt++;
								if (Character.isWhitespace(sPrevPage.charAt(i))) {
									sCurPage = sPrevPage.substring(i, sPrevPage.length()) + sCurPage;
									break;
								}
								if (cnt > 100) break;
							}
						}
					}
				} catch (Exception e) {

				}
			}
			return sCurPage;
		}

		public void updateUserDicWords() {
			//PositionProperties currpos = activity.getReaderView().getDoc().getPositionProps(null);
			this.wc = 0;
			this.arrUdeWords.clear();
			String sCurPage = getCurPageText();
			Iterator it = activity.getmUserDic().entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				String sKey = pair.getKey().toString().toLowerCase();
				String sType = sKey.substring(0,1);
				sKey = sKey.substring(1);
				if ((!StrUtils.isEmptyStr(sKey))&&(!sType.equals("1"))) {
					try {
						String[] arrKey = sKey.split("~");
						boolean bWas = false;
						for (String sK : arrKey) {
							//CoolReader.log.i(sK);
							String sK2 = sK.replaceAll("-", " ");
							String sK3 = sK.replaceAll("-", "");
							String[] arrK = sK.split("\\|");
							String[] arrK2 = sK2.split("\\|");
							String[] arrK3 = sK3.split("\\|");
							if ((!bWas)&&
								  (sCurPage.contains(arrK[0]) || sCurPage.contains(arrK2[0]) || sCurPage.contains(arrK3[0]))) {
								bWas = true;
								this.wc = this.wc + 1;
								UserDicEntry ude = (UserDicEntry) pair.getValue();
								this.arrUdeWords.add(ude);
							}
						}
					} catch (Exception e) {
						Log.e("UDP", e.getMessage());
					}
				}
			}
			String sContent = activity.settings().getProperty(Settings.PROP_APP_SHOW_USER_DIC_CONTENT, "0");
			if (sContent.equals("0"))
				// check recent translates
				for (DicSearchHistoryEntry dshe: UserDicDlg.mDicSearchHistoryAll) {
					String sKey = dshe.getSearch_text().toLowerCase();
					String sValue = dshe.getText_translate().toLowerCase();
					if ((!StrUtils.isEmptyStr(sKey))&&(!StrUtils.isEmptyStr(sValue))) {
						try {
							String[] arrKey = sKey.split("~");
							boolean bWas = false;
							for (String sK : arrKey) {
								//CoolReader.log.i(sK);
								String sK2 = sK.replaceAll("-", " ");
								String sK3 = sK.replaceAll("-", "");
								String[] arrK = sK.split("\\|");
								String[] arrK2 = sK2.split("\\|");
								String[] arrK3 = sK3.split("\\|");
								if ((!bWas)&&
										(sCurPage.contains(arrK[0]) || sCurPage.contains(arrK2[0]) || sCurPage.contains(arrK3[0]))) {
									bWas = true;
									boolean bDouble = false;
									for (int i = 0; i < this.arrUdeWords.size(); i++) {
										if (StrUtils.getNonEmptyStr(this.arrUdeWords.get(i).getDic_word(),true).
												equals(StrUtils.getNonEmptyStr(sKey,true))) {
											bDouble = true;
											break;
										}
									}
									boolean tooMuch = sKey.split("\\s+").length>3;
									if ((!bDouble)&&(!tooMuch)) {
										this.wc = this.wc + 1;
										UserDicEntry ude = new UserDicEntry();
										ude.setDic_word(sKey);
										ude.setDic_word_translate(sValue);
										ude.setCreate_time(dshe.getCreate_time());
										ude.setLast_access_time(dshe.getLast_access_time());
										ude.setDic_from_book(dshe.getSearch_from_book());
										ude.setLanguage(dshe.getLanguage_from());
										ude.setThisIsDSHE(true);
										this.arrUdeWords.add(ude);
									}
								}
							}
						} catch (Exception e) {

						}
					}
				}
			Collections.sort(arrUdeWords, (lhs, rhs) -> {
				// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
				//return lhs.getDic_word().compareToIgnoreCase(rhs.getDic_word());
				//lets by time
				if (lhs.getLast_access_time() > rhs.getLast_access_time()) return -1;
				if (lhs.getLast_access_time() < rhs.getLast_access_time()) return 1;
				return 0;
			});
			updateViews();
			int showUD = activity.settings().getInt(Settings.PROP_APP_SHOW_USER_DIC_PANEL, 0);
			if ((!arrUdeWords.isEmpty()) && (showUD > 0)) {
				String wrds = "";
				for (int i = 0; i < arrUdeWords.size(); i++) {
					wrds = wrds + (((i == 0) ? "": "~" ) + arrUdeWords.get(i).getDic_word());
				}
				if (activity.getmReaderView() != null) {
					if (activity.getmReaderView().flgHighlightUserDic) {
						activity.getmReaderView().clearSelection();
						activity.getmReaderView().findText("{{curPage}}" + wrds, false, true, true);
					}
				}
			}
		}

		private void updateViews() {
			String sWC = activity.getString(R.string.wc) +": "+String.valueOf(this.wc);
			if (this.wc == 0) sWC="";
			String sAll = this.lblWordFound.getText().toString();
			for (TextView tv: arrLblWords) {
				sAll=sAll+tv.getText().toString();
			}
			boolean updated = false;
			if (!lblWordFound.getText().equals(sWC)) {
				this.lblWordFound.setText(sWC);
			}
			int i=0;
			for (TextView tv: arrLblWords) {
				tv.setText("");
			}
			for (UserDicEntry ude: arrUdeWords) {
				i++;
				if (i<10) {
					arrLblWords.get(i).setText("_");
					try {
						String sKey = ude.getDic_word();
						String[] arrKey = sKey.split("~");
						sKey = arrKey[0];
						arrLblWords.get(i).setText(sKey.replace("|", ""));
						Typeface tf = activity.getReaderFont();
						if (ude.getThisIsDSHE())
							arrLblWords.get(i).setTypeface(tf, Typeface.ITALIC);
						else
							arrLblWords.get(i).setTypeface(tf);
					} catch (Exception e) {

					}
				}
			}
			String sAll2 = this.lblWordFound.getText().toString();
			for (TextView tv: arrLblWords) {
				sAll2=sAll2+tv.getText().toString();
			}
			if (!sAll.equals(sAll2)) updated=true;
			if (updated && isShown()) {
				CoolReader.log.d("changing user dic layout");
				measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				forceLayout();
			}
		}
	
	}