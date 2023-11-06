package org.coolreader.options;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FontsPangramms;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Scanner;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;
import org.coolreader.layouts.FlowLayout;
import org.coolreader.readerview.ReaderView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class FontSelectOption extends ListOption
{

	protected interface FontScanCompleted {
		void onComplete(ArrayList<OptionsDialog.Three> list, boolean canceled);
	}

	public static String[] mFontFaces;
	public static String[] mFontFacesFiles;

	ArrayList<String> faces = new ArrayList<>();
	ArrayList<String> faceValues = new ArrayList<>();

	final ReaderView mReaderView;
	String langTag;
	String langDescr;

	private void asyncFilterFontsByLanguage(String langTag, FontScanCompleted onComplete) {
		BackgroundThread.ensureGUI();
		final Scanner.ScanControl control = new Scanner.ScanControl();
		final Engine.ProgressControl progress = Services.getEngine().createProgress(R.string.scanning_font_files, control);
		final ArrayList<OptionsDialog.Three> filtered = new ArrayList<>();
		BackgroundThread.instance().postBackground(() -> {
			int i = 0;
			for (OptionsDialog.Three three : list) {
				if (control.isStopped())
					break;
				String faceName = three.value;
				String addInfo = three.addInfo;
				Engine.font_lang_compat status = Engine.checkFontLanguageCompatibility(faceName, langTag);
				switch (status) {
					case font_lang_compat_full:
					case font_lang_compat_partial:
						filtered.add(new OptionsDialog.Three(faceName, faceName, addInfo));
						break;
					default:
						break;
				}
				i++;
				progress.setProgress(10000*i/list.size(), "");
			}
			if (onComplete != null)
				onComplete.onComplete(filtered, control.isStopped());
			else {
				if (!control.isStopped())
					BackgroundThread.instance().executeGUI(() -> {
						listUpdated(filtered);
					});
			}
			progress.hide();
		});
	}

	@Override
	protected boolean addToQuickFilters(ViewGroup qfView) {
		FlowLayout langView = new FlowLayout(mActivity);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		boolean bWas = false;
		if (!StrUtils.isEmptyStr(langTag)) {
			bWas = true;
			Button qfButton = new Button(mActivity);
			qfButton.setText(langDescr);
			int newTextSize = mActivity.settings().getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
			qfButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
			int colorIcon = themeColors.get(R.attr.colorIcon);
			int colorGray = themeColors.get(R.attr.colorThemeGray2);
			qfButton.setTextColor(mActivity.getTextColor(colorIcon));
			qfButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
			qfButton.setPadding(10, 20, 10, 20);
			LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			llp.setMargins(8, 4, 4, 4);
			qfButton.setLayoutParams(llp);
			qfButton.setMaxLines(1);
			qfButton.setEllipsize(TextUtils.TruncateAt.END);
			langView.addView(qfButton);
			qfButton.setOnClickListener(v -> asyncFilterFontsByLanguage(langTag, null));
			if (isEInk) Utils.setSolidButtonEink(qfButton);
			TextView tv = new TextView(mActivity);
			tv.setText(" ");
			tv.setPadding(10, 10, 10, 10);
			tv.setLayoutParams(llp);
			tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
			tv.setTextColor(mActivity.getTextColor(colorIcon));
			langView.addView(tv);
		}
		String quickDirs = mActivity.settings().getProperty(Settings.PROP_APP_QUICK_TRANSLATION_DIRS);
		boolean isEmptyQuick = StrUtils.isEmptyStr(StrUtils.getNonEmptyStr(quickDirs,true).replace(";",""));
		if (!isEmptyQuick) {
			String[] sQuickDirsArr = quickDirs.split(";");
			ArrayList<String> sButtons = new ArrayList<>();
			for (String s : sQuickDirsArr)
				if (s.contains("=")) {
					String s1 = s.split("=")[0];
					String s2 = s.split("=")[0];
					if (!s1.equals(langTag))
						if (!sButtons.contains(s1)) sButtons.add(s1);
					if (!s2.equals(langTag))
						if (!sButtons.contains(s2)) sButtons.add(s2);
				}
			Collections.sort(sButtons, (o1, o2) -> o1.compareToIgnoreCase(o2));
			for (String s : sButtons) {
				bWas = true;
				Button qfButton = new Button(mActivity);
				qfButton.setText(Engine.getHumanReadableLocaleName(s));
				int newTextSize = mActivity.settings().getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
				qfButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
				int colorIcon = themeColors.get(R.attr.colorIcon);
				int colorGray = themeColors.get(R.attr.colorThemeGray2);
				qfButton.setTextColor(mActivity.getTextColor(colorIcon));
				qfButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
				qfButton.setPadding(10, 20, 10, 20);
				LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				llp.setMargins(8, 4, 4, 4);
				qfButton.setLayoutParams(llp);
				qfButton.setMaxLines(1);
				qfButton.setEllipsize(TextUtils.TruncateAt.END);
				langView.addView(qfButton);
				qfButton.setOnClickListener(v -> asyncFilterFontsByLanguage(s, null));
				TextView tv = new TextView(mActivity);
				tv.setText(" ");
				tv.setPadding(10, 10, 10, 10);
				tv.setLayoutParams(llp);
				tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
				tv.setTextColor(mActivity.getTextColor(colorIcon));
				langView.addView(tv);
			}
		}
		if (bWas) qfView.addView(langView);
		return true;
	}

	public FontSelectOption(OptionOwner owner, String label, String property, String addInfo, boolean bForCSS, String filter)
	{
		super(owner, label, property, addInfo, filter);
		mReaderView = ((CoolReader) mActivity).getmReaderView();
		langTag = null;
		langDescr = null;
		if (mReaderView != null) {
			BookInfo bookInfo = mReaderView.getBookInfo();
			if (null != bookInfo) {
				FileInfo fileInfo = bookInfo.getFileInfo();
				if (null != fileInfo) {
					langTag = fileInfo.language;
					langDescr = Engine.getHumanReadableLocaleName(langTag);
				}
			}
		}
		HashMap<String,ArrayList<String>> fontFiles = new HashMap<String,ArrayList<String>>();
		if (bForCSS) {
			faces.add("-");
			faceValues.add("");
			faces.add(mActivity.getString(R.string.options_css_font_face_sans_serif));
			faceValues.add("font-family: sans-serif");
			faces.add(mActivity.getString(R.string.options_css_font_face_serif));
			faceValues.add("font-family: serif");
			faces.add(mActivity.getString(R.string.options_css_font_face_monospace));
			faceValues.add("font-family: \"Courier New\", \"Courier\", monospace");
		};
		usefulLinks.put("Google Fonts", "https://fonts.google.com/");
		usefulLinks.put("Paratype PT fonts", "http://rus.paratype.ru/pt-sans-pt-serif");
		for (String face : mFontFacesFiles) {
			String sFace = face;
			String sFile = "";
			if (face.contains("~")) {
				sFace = face.split("~")[0];
				sFile = face.split("~")[1];
			}
			if ((!StrUtils.isEmptyStr(sFace))&&(!StrUtils.isEmptyStr(sFile))) {
				if (sFile.contains("/")) {
					String s = sFile.substring(0,sFile.lastIndexOf("/"));
					if (!quickFilters.contains(s)) quickFilters.add(s);
				} else
				if (sFile.contains("\\")) {
					String s = sFile.substring(0,sFile.lastIndexOf("\\"));
					if (!quickFilters.contains(s)) quickFilters.add(s);
				}
				if (fontFiles.get(sFace) == null) {
					ArrayList<String> alFiles = new ArrayList<String>();
					alFiles.add(sFile);
					fontFiles.put(sFace,alFiles);
				} else {
					ArrayList<String> alFiles = fontFiles.get(sFace);
					alFiles.add(sFile);
					fontFiles.put(sFace,alFiles);
				}
			}
			if (!faces.contains(sFace)) {
				faces.add(sFace);
				faceValues.add((bForCSS ? "font-family: " : "") + sFace);
			}
		}
		int i;
		for (i = 0; i < faces.size(); i++) {
			int iFilesExists = -1;
			String addI = "";
			if (fontFiles.get(faces.get(i)) != null) {
				iFilesExists = 0;
				for (String s: fontFiles.get(faces.get(i))) {
					File f = new File(s);
					if (f.exists()) {
						iFilesExists++;
						addI = addI + "~" + s;
					}
				}
			}
			if (!StrUtils.isEmptyStr(addI)) addI = addI.substring(1);
			if (iFilesExists != 0) add(faceValues.get(i), faces.get(i), addI);
		}
		if (faces.size()>0) setDefaultValue(faceValues.get(0));
	}

	protected void closed() {

	}

	protected int getItemLayoutId() {
		return R.layout.option_value_fonts;
	}

	private void addItem(TableLayout table, OptionsDialog.Three item, final String addInfo, final String testPhrase) {
		TableRow tableRow = (TableRow)mInflater.inflate(R.layout.font_option_item, null);
		ImageView btnOptionAddInfo = null;
		btnOptionAddInfo = (ImageView)tableRow.findViewById(R.id.btn_option_add_info1);
		if ((btnOptionAddInfo!=null)&&(StrUtils.isEmptyStr(addInfo))) btnOptionAddInfo.setVisibility(View.INVISIBLE);
		else
			btnOptionAddInfo.setImageDrawable(
					mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
							R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
		mActivity.tintViewIcons(btnOptionAddInfo);
		if (btnOptionAddInfo != null) {
			btnOptionAddInfo.setOnClickListener(v -> mActivity.showToast(addInfo, Toast.LENGTH_LONG, v, true, 0));
		}
		TextView txt1 = (TextView)tableRow.findViewById(R.id.option_value_add_text1);
		txt1.setText(testPhrase);
		Typeface tf = null;
		try {
			if (StrUtils.isEmptyStr(addInfo))
				tf = Typeface.create(item.value.replaceAll("font-family: ", ""), Typeface.NORMAL);
			else
				tf = Typeface.createFromFile(addInfo);
			txt1.setTypeface(tf);
		} catch (Exception e) {

		}
		mActivity.tintViewIcons(tableRow,false);
		table.addView(tableRow);
	}

	protected void updateItemContents(final View layout, final OptionsDialog.Three item, final ListView listView, final int position ) {
		super.updateItemContents(layout, item, listView, position);
		ImageView cb = layout.findViewById(R.id.option_value_check);
		ImageView iv = layout.findViewById(R.id.option_value_del);
		if (cb.getTag().equals("1")) iv.setVisibility(View.INVISIBLE);
		else {
			String[] arrFonts = item.addInfo.split("~");
			int iFilesCnt = 0;
			for (String s: arrFonts) {
				if ((!s.startsWith("/system"))&&(!StrUtils.isEmptyStr(s))) {
					File f = new File(s);
					if (f.exists()) iFilesCnt++;
				}
			}
			if (iFilesCnt == 0) iv.setVisibility(View.INVISIBLE);
		};
		iv.setOnClickListener(v -> {
			if (cb.getTag().equals("1")) {
				return;
			}
			String[] arrFonts = item.addInfo.split("~");
			int iFilesCnt = 0;
			for (String s: arrFonts) {
				if ((!s.startsWith("/system"))&&(!StrUtils.isEmptyStr(s))) {
					File f = new File(s);
					if (f.exists()) iFilesCnt++;
				}
			}
			if (iFilesCnt>0) {
				final int iFilesCntF = iFilesCnt;
				mActivity.askConfirmation(R.string.delete_font, () -> {
					int iFilesDeleted = 0;
					for (String s: arrFonts) {
						if (!s.startsWith("/system")) {
							File f = new File(s);
							if (f.exists())
								if (f.delete()) iFilesDeleted++;
						}
					}
					if (iFilesDeleted != iFilesCntF) {
						mActivity.showToast(mActivity.getString(R.string.fonts_deleted_partial,
								String.valueOf(iFilesDeleted),String.valueOf(iFilesCntF)));
					} else {
						mActivity.showToast(R.string.fonts_deleted_full);
						OptionsDialog.Three forRemove = null;
						for (OptionsDialog.Three t: list)
							if (t.label.equals(item.label)) forRemove = t;
						if (forRemove != null) list.remove(forRemove);
						forRemove = null;
						for (OptionsDialog.Three t: listFiltered)
							if (t.label.equals(item.label)) forRemove = t;
						if (forRemove != null) listFiltered.remove(forRemove);
						listUpdated("");
					}
				});
			} else {
				mActivity.showToast(R.string.non_system_fonts_not_found);
			}
		});
		mActivity.tintViewIcons(iv,true);
		TableLayout table = (TableLayout) layout.findViewById(R.id.table_add_text1);
		if (table != null) table.removeAllViews();
		boolean simple =  StrUtils.getNonEmptyStr(mProperties.getProperty(Settings.PROP_APP_USE_SIMPLE_FONT_SELECT_DIALOG), false).equals("1");
		if (!simple) {
			String[] sAdd = {""};
			if (!StrUtils.isEmptyStr(item.addInfo)) sAdd = item.addInfo.split("~");
			String sAdd1 = "";
			String sAdd2 = "";
			String sAdd3 = "";
			if (sAdd.length > 0) sAdd1 = sAdd[0];
			if (sAdd.length > 1) sAdd2 = sAdd[1];
			if (sAdd.length > 2) sAdd3 = sAdd[2];
			String s = mActivity.getCurrentLanguage();
			if (s.toUpperCase().startsWith("RU")) {
				int r1 = (int) (Math.random() * ((FontsPangramms.RUSSIAN.length - 1) + 1));
				int r2 = (int) (Math.random() * ((FontsPangramms.RUSSIAN.length - 1) + 1));
				int r3 = (int) (Math.random() * ((FontsPangramms.RUSSIAN.length - 1) + 1));
				addItem(table, item, sAdd1, FontsPangramms.RUSSIAN[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.RUSSIAN[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.RUSSIAN[r3]);
			} else if (s.toUpperCase().startsWith("DE")) {
				int r1 = (int) (Math.random() * ((FontsPangramms.GERMAN.length - 1) + 1));
				int r2 = (int) (Math.random() * ((FontsPangramms.GERMAN.length - 1) + 1));
				int r3 = (int) (Math.random() * ((FontsPangramms.GERMAN.length - 1) + 1));
				addItem(table, item, sAdd1, FontsPangramms.GERMAN[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.GERMAN[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.GERMAN[r3]);
			} else if (s.toUpperCase().startsWith("EN")) {
				int r1 = (int) (Math.random() * ((FontsPangramms.ENGLISH.length - 1) + 1));
				int r2 = (int) (Math.random() * ((FontsPangramms.ENGLISH.length - 1) + 1));
				int r3 = (int) (Math.random() * ((FontsPangramms.ENGLISH.length - 1) + 1));
				addItem(table, item, sAdd1, FontsPangramms.ENGLISH[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.ENGLISH[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.ENGLISH[r3]);
			} else if (s.toUpperCase().startsWith("ES")) {
				int r1 = (int) (Math.random() * ((FontsPangramms.SPAIN.length - 1) + 1));
				int r2 = (int) (Math.random() * ((FontsPangramms.SPAIN.length - 1) + 1));
				int r3 = (int) (Math.random() * ((FontsPangramms.SPAIN.length - 1) + 1));
				addItem(table, item, sAdd1, FontsPangramms.SPAIN[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.SPAIN[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.SPAIN[r3]);
			} else if (s.toUpperCase().startsWith("NL")) {
				int r1 = (int) (Math.random() * ((FontsPangramms.DUTCH.length - 1) + 1));
				int r2 = (int) (Math.random() * ((FontsPangramms.DUTCH.length - 1) + 1));
				int r3 = (int) (Math.random() * ((FontsPangramms.DUTCH.length - 1) + 1));
				addItem(table, item, sAdd1, FontsPangramms.DUTCH[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.DUTCH[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.DUTCH[r3]);
			} else if (s.toUpperCase().startsWith("CS")) {
				int r1 = (int) (Math.random() * ((FontsPangramms.CH.length - 1) + 1));
				int r2 = (int) (Math.random() * ((FontsPangramms.CH.length - 1) + 1));
				int r3 = (int) (Math.random() * ((FontsPangramms.CH.length - 1) + 1));
				addItem(table, item, sAdd1, FontsPangramms.CH[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.CH[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.CH[r3]);
			} else if (s.toUpperCase().startsWith("FR")) {
				int r1 = (int) (Math.random() * ((FontsPangramms.FRENCH.length - 1) + 1));
				int r2 = (int) (Math.random() * ((FontsPangramms.FRENCH.length - 1) + 1));
				int r3 = (int) (Math.random() * ((FontsPangramms.FRENCH.length - 1) + 1));
				addItem(table, item, sAdd1, FontsPangramms.FRENCH[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.FRENCH[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.FRENCH[r3]);
			} else {
				addItem(table, item, sAdd1, mActivity.getString(R.string.font_test_phrase));
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, mActivity.getString(R.string.font_test_phrase2));
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, mActivity.getString(R.string.font_test_phrase3));
			}
		} else {
			Utils.removeView(table);
		}
	}
}

