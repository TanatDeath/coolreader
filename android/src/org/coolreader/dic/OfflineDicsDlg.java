package org.coolreader.dic;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BaseListView;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.ProgressDialog;
import org.coolreader.crengine.Scanner;
import org.coolreader.crengine.SomeButtonsToolbarDlg;
import org.coolreader.utils.FileUtils;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;
import org.dict.zip.DictZipInputStream;
import org.dict.zip.RandomAccessInputStream;
import org.dict.zip.RandomAccessOutputStream;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.github.eb4j.dsl.DslDictionary;

public class OfflineDicsDlg extends BaseDialog {
	CoolReader mCoolReader;
	private LayoutInflater mInflater;
	View mDialogView;
	ArrayList<OfflineDicInfo> mDics;
	private DictList mList;
	Scanner.ScanControl mScanControl;

	public static ProgressDialog progressDlg;

	public static final Logger log = L.create("odd");

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	@Override
	protected void onPositiveButtonClick()
	{
		log.d("Starting save dic_conf.json");
		try {
			final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
			final String prettyJson = gson.toJson(mDics);
			ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.OfflineDicsDirs, true);
			if (tDirs.size()>0) {
				for (String dir : tDirs) {
					Utils.saveStringToFileSafe(prettyJson, dir + "/dic_conf.json");
				}
			}
		} catch (Exception e) {
		}
		cancel();
	}

	@Override
	protected void onNegativeButtonClick()
	{
	   cancel();
	}

	public final static int ITEM_POSITION=0;

	public void setCheckedOption(ImageView v, boolean checked) {
		if (checked) {
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_checked_checkbox);
			v.setImageDrawable(d);
			v.setTag("1");
			mActivity.tintViewIconsForce(v);
		} else {
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_unchecked_checkbox);
			v.setImageDrawable(d);
			v.setTag("0");
			mActivity.tintViewIcons(v, PorterDuff.Mode.CLEAR,true);
		}
	}

	class DictListAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return mDics.size();
		}

		public Object getItem(int position) {
			if (position < 0 || position >= mDics.size())
				return null;
			return mDics.get(position);
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

		private void paintFormatButtons() {
//			int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
//			int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
//			int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
//			int colorGrayE=Color.argb(100,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
//			mCoolReader.tintViewIcons(btnPage2, PorterDuff.Mode.CLEAR,true);
//			mCoolReader.tintViewIcons(btnBook2, PorterDuff.Mode.CLEAR,true);
//			mCoolReader.tintViewIcons(btnAll2, PorterDuff.Mode.CLEAR,true);
//			if (getCheckedFromTag(btnPage2.getTag())) {
//				btnPage2.setBackgroundColor(isEInk? colorGrayE: colorGrayCT2);
//				mCoolReader.tintViewIcons(btnPage2,true);
//				if (isEInk) Utils.setSolidButtonEink(btnPage2);
//			} else btnPage2.setBackgroundColor(isEInk? Color.WHITE: colorGrayCT);
//			if (getCheckedFromTag(btnBook2.getTag())) {
//				btnBook2.setBackgroundColor(isEInk? colorGrayE: colorGrayCT2);
//				mCoolReader.tintViewIcons(btnBook2,true);
//				if (isEInk) Utils.setSolidButtonEink(btnBook2);
//			} else btnBook2.setBackgroundColor(isEInk? Color.WHITE:colorGrayCT);
//			if (getCheckedFromTag(btnAll2.getTag())) {
//				btnAll2.setBackgroundColor(isEInk? colorGrayE: colorGrayCT2);
//				mCoolReader.tintViewIcons(btnAll2,true);
//				if (isEInk) Utils.setSolidButtonEink(btnAll2);
//			} else btnAll2.setBackgroundColor(isEInk? Color.WHITE: colorGrayCT);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.offline_dict_item;
			view = mInflater.inflate(res, null);
			ImageView valueView = view.findViewById(R.id.option_value_cb);
			TextView tvTitle = view.findViewById(R.id.dict_item_title);
			TextView tvFilepath = view.findViewById(R.id.dict_item_filepath);
			TextView tvDesc = view.findViewById(R.id.dict_item_desc);
			TextView tvVersion = view.findViewById(R.id.dict_item_version);
			Button btnConvert = view.findViewById(R.id.btn_convert);
			Button btnAsText = view.findViewById(R.id.btn_text);
			Button btnAsHtml = view.findViewById(R.id.btn_html);
			EditText langFrom = view.findViewById(R.id.lang_from);
			langFrom.setPadding(10, 5, 10, 20);
			EditText langTo = view.findViewById(R.id.lang_to);
			langTo.setPadding(10, 5, 10, 20);
			//TextView tvConverted = view.findViewById(R.id.tv_converted);
			OfflineDicInfo sdi = (OfflineDicInfo) getItem(position);
			setCheckedOption(valueView, sdi.dicEnabled);
			valueView.setOnClickListener(v -> {
				sdi.dicEnabled = !sdi.dicEnabled;
				setCheckedOption(valueView, sdi.dicEnabled);
			});
			langFrom.setText(sdi.langFrom);
			langTo.setText(sdi.langTo);
			ImageView btnUp = view.findViewById(R.id.btn_up);
			ImageView btnDown = view.findViewById(R.id.btn_down);
			btnUp.setOnClickListener(v -> {
				int idx = mDics.indexOf(sdi);
				if (idx > 0) {
					mDics.remove(sdi);
					mDics.add(idx - 1, sdi);
					((DictListAdapter) mList.adapter).notifyDataSetChanged();
					mList.adapter = new DictListAdapter();
					mList.setAdapter(mList.adapter);
				}
			});
			btnDown.setOnClickListener(v -> {
				int idx = mDics.indexOf(sdi);
				if (idx < mDics.size() - 1) {
					mDics.remove(sdi);
					mDics.add(idx + 1, sdi);
					((DictListAdapter) mList.adapter).notifyDataSetChanged();
					mList.adapter = new DictListAdapter();
					mList.setAdapter(mList.adapter);
				}
			});
			tvTitle.setText(sdi.dicName);
			tvFilepath.setText(sdi.dicNameWOExt);
			String wcnt = mActivity.getString(R.string.word_count) + ": " + sdi.wordCount;
			if (sdi.wordCount == 0) wcnt = "";
			if (StrUtils.isEmptyStr(sdi.dicVersion))
				tvVersion.setText(wcnt);
			else
				tvVersion.setText(sdi.dicVersion + "; " + wcnt);
			tvDesc.setText(sdi.dicDescription);
			btnConvert.setText(R.string.convert_dic);
			int colorGray = themeColors.get(R.attr.colorThemeGray2);
			btnConvert.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
			if (isEInk) Utils.setSolidButtonEink(btnConvert);
			btnConvert.setPadding(10, 20, 10, 20);
			//tvConverted.setPadding(10, 20, 10, 20);
			mActivity.tintViewIcons(view);
			btnConvert.setOnClickListener(v -> {
				// StarDict
				if ((sdi.dbExists) || (sdi.dictExists) || (sdi.dictDzExists)) {
					if (sdi.dbExists) {
						mActivity.askConfirmation(R.string.dict_was_converted, () -> {
							File dicDBFile = new File(sdi.dicPath + "/" + sdi.dicName + ".db");
							File dicDBFileJ = new File(sdi.dicPath + "/" + sdi.dicName + ".db-journal");
							boolean deleted = true;
							if (dicDBFile.exists()) deleted = deleted && dicDBFile.delete();
							if (dicDBFileJ.exists()) deleted = deleted && dicDBFileJ.delete();
							if (deleted)
								convertStarDictDic(sdi, btnConvert);
							else {
								mActivity.showToast(mActivity.getString(R.string.dict_was_not_deleted));
								mDics = fillOfflineDics();
								mList = new DictList(mActivity, false);
								mList.setAdapter(mList.adapter);
							}
						});
					} else
						convertStarDictDic(sdi, btnConvert);
				}
				// Lingvo DSL
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					if ((sdi.dslExists) || (sdi.dslDzExists)) {
						if (sdi.idxExists) {
							mActivity.askConfirmation(R.string.dict_was_converted, () -> {
								File dicIdxFile = new File(sdi.dicPath + "/" + sdi.dicNameWOExt + ".idx");
								boolean deleted = true;
								if (dicIdxFile.exists()) deleted = deleted && dicIdxFile.delete();
								if (deleted)
									convertLingvoDslDic(sdi, btnConvert);
								else {
									mActivity.showToast(mActivity.getString(R.string.dict_was_not_deleted));
									mDics = fillOfflineDics();
									mList = new DictList(mActivity, false);
									mList.setAdapter(mList.adapter);
								}
							});
						} else
							convertLingvoDslDic(sdi, btnConvert);
					}

			});
			TextWatcher watcher = new TextWatcher() {

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
					sdi.langFrom = StrUtils.getNonEmptyStr(langFrom.getText().toString(),true);
					sdi.langTo = StrUtils.getNonEmptyStr(langTo.getText().toString(),true);
				}
			};
			if (langFrom != null)
				langFrom.addTextChangedListener(watcher);
			if (langTo != null)
				langTo.addTextChangedListener(watcher);
			if ((sdi.dbExists) || (sdi.idxExists && (sdi.dslDzExists || sdi.dslExists))) {
				btnConvert.setText(R.string.converted_dic);
				btnConvert.setPaintFlags(btnConvert.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			} else {
				btnConvert.setPaintFlags(btnConvert.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
			}
			int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
			int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
			int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
			int colorGrayE=Color.argb(100,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
			mCoolReader.tintViewIcons(btnAsHtml, PorterDuff.Mode.CLEAR,true);
			mCoolReader.tintViewIcons(btnAsText, PorterDuff.Mode.CLEAR,true);
			Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
			Drawable img1 = img.getConstantState().newDrawable().mutate();
			Drawable img2 = img.getConstantState().newDrawable().mutate();
			btnAsText.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
			btnAsHtml.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
			btnAsText.setBackgroundColor(isEInk? Color.WHITE: colorGrayCT);
			btnAsText.setPadding(15, 10, 15, 10);
			mCoolReader.tintViewIcons(btnAsText,true);
			if (isEInk) Utils.setSolidButtonEink(btnAsText);
			btnAsHtml.setBackgroundColor(isEInk? Color.WHITE: colorGrayCT);
			btnAsHtml.setPadding(15, 10, 15, 10);
			mCoolReader.tintViewIcons(btnAsHtml,true);
			if (isEInk) Utils.setSolidButtonEink(btnAsHtml);
			if (StrUtils.getNonEmptyStr(sdi.displayFormat, true).equals("html")) {
				btnAsHtml.setBackgroundColor(isEInk? colorGrayE: colorGrayCT2);
				mCoolReader.tintViewIcons(btnAsHtml,true);
				if (isEInk) Utils.setSolidButtonEink(btnAsHtml);
				mCoolReader.tintViewIcons(btnAsText, PorterDuff.Mode.CLEAR,true);
			} else {
				btnAsText.setBackgroundColor(isEInk? colorGrayE: colorGrayCT2);
				mCoolReader.tintViewIcons(btnAsText,true);
				if (isEInk) Utils.setSolidButtonEink(btnAsText);
				mCoolReader.tintViewIcons(btnAsHtml, PorterDuff.Mode.CLEAR,true);
			}
			btnAsText.setOnClickListener(v -> {
				sdi.displayFormat = "text";
				btnAsText.setBackgroundColor(isEInk? colorGrayE: colorGrayCT2);
				btnAsHtml.setBackgroundColor(isEInk? Color.WHITE: colorGrayCT);
				mCoolReader.tintViewIcons(btnAsText,true);
				if (isEInk) Utils.setSolidButtonEink(btnAsText);
				mCoolReader.tintViewIcons(btnAsHtml, PorterDuff.Mode.CLEAR,true);
			});
			btnAsHtml.setOnClickListener(v -> {
				sdi.displayFormat = "html";
				btnAsHtml.setBackgroundColor(isEInk? colorGrayE: colorGrayCT2);
				btnAsText.setBackgroundColor(isEInk? Color.WHITE: colorGrayCT);
				mCoolReader.tintViewIcons(btnAsHtml,true);
				if (isEInk) Utils.setSolidButtonEink(btnAsHtml);
				mCoolReader.tintViewIcons(btnAsText, PorterDuff.Mode.CLEAR,true);
			});
			btnAsText.setVisibility(View.INVISIBLE);
			btnAsHtml.setVisibility(View.INVISIBLE);
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mDics.size() == 0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	private void convertStarDictDic(OfflineDicInfo sdi, Button btnConvert) {
		if ((!sdi.dictExists) && (sdi.dictDzExists)) {
			progressDlg = ProgressDialog.show(mCoolReader,
					mCoolReader.getString(R.string.unpack_dic),
					mCoolReader.getString(R.string.unpack_dic),
					true, false, null);
			BackgroundThread.instance().postGUI(() -> {
				String s = doUnzip(sdi);
				if (progressDlg.isShowing()) progressDlg.dismiss();
				sdi.fillMarks();
				convertDicInternal(sdi, btnConvert, true);
			});
		} else convertDicInternal(sdi, btnConvert, false);
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void convertLingvoDslDic(OfflineDicInfo sdi, Button btnConvert) {
		ArrayList<String> sButtons = new ArrayList<>();
		sButtons.add(mActivity.getString(R.string.ok));
		progressDlg = ProgressDialog.show(mCoolReader,
			mCoolReader.getString(R.string.dic_index_create),
			mCoolReader.getString(R.string.dic_index_create),
			true, false, null);
		BackgroundThread.instance().postGUI(() -> {
			DslDictionary dslDictionary = null;
			try {
				if (sdi.dslDzExists)
					dslDictionary = DslDictionary.loadDictionary(
							Paths.get(sdi.dicPath + "/" + sdi.dicNameWOExt + ".dsl.dz"),
							Paths.get(sdi.dicPath + "/" + sdi.dicNameWOExt + ".idx")
					);
				else
					dslDictionary = DslDictionary.loadDictionary(
							Paths.get(sdi.dicPath + "/" + sdi.dicNameWOExt + ".dsl"),
							Paths.get(sdi.dicPath + "/" + sdi.dicNameWOExt + ".idx")
					);
				btnConvert.setText(R.string.converted_dic);
				sButtons.add(0,"*" + mActivity.getString(R.string.success_ext));
				SomeButtonsToolbarDlg.showDialog(mCoolReader, btnConvert, 0, true,
						"", sButtons, null, (o22, btnPressed) -> {});
				btnConvert.setPaintFlags(btnConvert.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			} catch (Exception e) {
				BackgroundThread.instance().postGUI(() -> {
					sButtons.add(0,"*" + mActivity.getString(R.string.unhandled_error) + ": " +
							e.getMessage());
					SomeButtonsToolbarDlg.showDialog(mCoolReader, btnConvert, 0, true,
							"", sButtons, null, (o22, btnPressed) -> {});
					mActivity.showToast(mActivity.getString(R.string.unhandled_error));
					log.e("Lingvo_DSL index create error: " + e.getMessage());
					File dicIdxFile = new File(sdi.dicPath + "/" + sdi.dicNameWOExt + ".idx");
					if (dicIdxFile.exists()) dicIdxFile.delete();
				});
			}
			if (progressDlg != null)
				if (progressDlg.isShowing()) progressDlg.dismiss();
			sdi.fillMarks();
			mDics = fillOfflineDics();
			mList = new DictList(mActivity, false);
		}, 500);
	}

	private void convertDicInternal(OfflineDicInfo sdi, Button btnConvert, boolean tempCreated) {
		ArrayList<String> sButtons = new ArrayList<>();
		sButtons.add(mActivity.getString(R.string.ok));
		this.mScanControl = new Scanner.ScanControl();
		Engine.ProgressControl progress = mActivity.mEngine.createProgress(R.string.dic_converting, mScanControl);
		mActivity.getDB().convertStartDictDic(sdi.dicPath, sdi.dicNameWOExt, mScanControl, progress, o -> {
			if (progress != null) progress.hide();
			if (o != null) {
				String resS = (String) o;
				BackgroundThread.instance().postGUI(() -> {
					if ("".equals(resS)) {
						mActivity.showToast(mActivity.getString(R.string.success_ext));
						btnConvert.setText(R.string.converted_dic);
						sButtons.add(0,"*" + mActivity.getString(R.string.success_ext));
						btnConvert.setPaintFlags(btnConvert.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
						SomeButtonsToolbarDlg.showDialog(mCoolReader, btnConvert, 0, true,
								"", sButtons, null, (o22, btnPressed) -> {});
					}
					else {
						//mActivity.getString(R.string.error) - need to covert messages
						if ("[dic_not_exists]".equals(resS)) {
							mActivity.showToast(mActivity.getString(R.string.dict_not_exists));
							sButtons.add(0,"*" + mActivity.getString(R.string.dict_not_exists));
							btnConvert.setPaintFlags(btnConvert.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
							SomeButtonsToolbarDlg.showDialog(mCoolReader, btnConvert, 0, true,
									"", sButtons, null, (o22, btnPressed) -> {});
							return;
						}
						if ("[incomplete]".equals(resS)) {
							mActivity.showToast(mActivity.getString(R.string.conversion_incomplete));
							sButtons.add(0,"*" + mActivity.getString(R.string.conversion_incomplete));
							btnConvert.setPaintFlags(btnConvert.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
							SomeButtonsToolbarDlg.showDialog(mCoolReader, btnConvert, 0, true,
									"", sButtons, null, (o22, btnPressed) -> {});
							return;
						}
						sButtons.add(0,"*" + mActivity.getString(R.string.error)+ ": " + resS);
						btnConvert.setPaintFlags(btnConvert.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
						SomeButtonsToolbarDlg.showDialog(mCoolReader, btnConvert, 0, true,
								"", sButtons, null, (o22, btnPressed) -> {});
						mActivity.showToast(mActivity.getString(R.string.error)+ ": " +
								resS);
					}
					if (tempCreated) {
						File dictFile = new File(sdi.dicPath + "/" + sdi.dicNameWOExt + ".dict");
						if (!dictFile.delete()) {
							mActivity.showToast(R.string.cannot_delete_temp_dic_file);
						}
					}
				});
			}
			sdi.fillMarks();
			mDics = fillOfflineDics();
			mList = new DictList(mActivity, false);
		});
	}

	class DictList extends BaseListView {

		public void refresh()
		{
			invalidate();
		}

		public ListAdapter adapter;

		public DictList(Context context, boolean shortcutMode) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			adapter = new DictListAdapter();
			setAdapter(adapter);
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> true);
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			return false;
		}
	}

	private static final int BUF_LEN = 5000;

	public static String doUnzip(OfflineDicInfo odi) {
		try {
			File dzFile = new File(odi.dicPath + "/" + odi.dicNameWOExt + ".dict.dz");
			try (DictZipInputStream din = new DictZipInputStream(new RandomAccessInputStream(new
					RandomAccessFile(dzFile, "r")));
				 OutputStream unzipOut = new RandomAccessOutputStream(odi.dicPath + "/" + odi.dicNameWOExt + ".dict", "rw")) {
				byte[] buf = new byte[BUF_LEN];
				din.seek(0);
				int len;
				while ((len = din.read(buf, 0, BUF_LEN)) > 0) {
					unzipOut.write(buf, 0, len);
				}
			}
		} catch (Exception e) {
			return e.getMessage();
		}
		return "";
	}

	public static ArrayList<OfflineDicInfo> fillOfflineDics() {
		ArrayList<OfflineDicInfo> dicsConf = new ArrayList<>();
		ArrayList<String> tDirs1 = Engine.getDataDirsExt(Engine.DataDirType.OfflineDicsDirs, true);
		if (tDirs1.size()>0) {
			String sdicsConf = Utils.readFileToStringOrEmpty(tDirs1.get(0) + "/dic_conf.json");
			if (!StrUtils.isEmptyStr(sdicsConf))
				try {
					dicsConf = new ArrayList<>(StrUtils.stringToArray(sdicsConf, OfflineDicInfo[].class));
				} catch (Exception e) {
				}
		}
		ArrayList<OfflineDicInfo> dics = new ArrayList<>();
		ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.OfflineDicsDirs, true);
		for (String dir: tDirs) {
			ArrayList<File> files = FileUtils.searchFiles(new File(dir),
					".*\\.dict|.*\\.db|.*\\.dict.dz|.*\\.dsl|.*\\.dsl.dz");
			for (File f: files) {
				String dicNameWOExt = Utils.getFileNameWOExtension(f);
				String fileExt = Utils.getFileExtension(f);
				if (StrUtils.getNonEmptyStr(fileExt,true).equals("dz")) {
					String fileExt2 = Utils.getFileExtension(dicNameWOExt);
					if (StrUtils.getNonEmptyStr(fileExt2,true).equals("dict")) {
						fileExt = "dict.dz";
						dicNameWOExt = Utils.getFileNameWOExtension(dicNameWOExt);
					}
					if (StrUtils.getNonEmptyStr(fileExt2,true).equals("dsl")) {
						fileExt = "dsl.dz";
						dicNameWOExt = Utils.getFileNameWOExtension(dicNameWOExt);
					}
				}
				String dicPath =f.getParent();
				boolean dictExists = new File(dicPath + "/" + dicNameWOExt + ".dict").exists();
				boolean dictDzExists = new File(dicPath + "/" + dicNameWOExt + ".dict.dz").exists();
				boolean dslExists = new File(dicPath + "/" + dicNameWOExt + ".dsl").exists();
				boolean dslDzExists = new File(dicPath + "/" + dicNameWOExt + ".dsl.dz").exists();
				boolean starDictExists = (dictExists || dictDzExists);
				boolean isStarDictFile = starDictExists && (fileExt.equals("dict") || fileExt.equals("dict.dz"));
				boolean onlyDB = fileExt.equals("db") && (!starDictExists);
				boolean isLingvoDSL = (dslExists || dslDzExists) && (fileExt.equals("dsl") || fileExt.equals("dsl.dz"));
				// if this file is not db or it is db, but no src file exists (dict or dictDz)
				if (isStarDictFile || onlyDB) {
					OfflineDicInfo sdi = new OfflineDicInfo();
					sdi.dicNameWOExt = dicNameWOExt;
					sdi.dicName = dicNameWOExt;
					sdi.dicPath = dicPath;
					sdi.fillMarks();
					File ifo = new File(sdi.dicPath + "/" + sdi.dicNameWOExt + ".ifo");
					sdi.dicEnabled = sdi.dbExists;
					if (sdi.ifoExists) {
						List<String> allLines = FileUtils.readLinesFromFile(ifo.getAbsolutePath());
						for (String s : allLines) {
							if (s.indexOf("=") > 0) {
								String s1 = s.substring(0, s.indexOf("="));
								String s2 = s.substring(s.indexOf("=") + 1, s.length());
								if (s1.equals("bookname")) sdi.dicName = s2;
								if (s1.equals("date")) sdi.dicDate = StrUtils.parseDate(s2);
								if (s1.equals("description")) sdi.dicDescription = s2;
								if (s1.equals("version")) sdi.dicVersion = s2;
								try {
									if (s1.equals("wordcount"))
										sdi.wordCount = Integer.parseInt(s2);
								} catch (Exception e) {
									sdi.wordCount = 0;
								}
							}
						}
					}
					dics.add(sdi);
				}
				// Lingvo DSL
				if (isLingvoDSL) {
					OfflineDicInfo sdi = new OfflineDicInfo();
					sdi.dicNameWOExt = dicNameWOExt;
					sdi.dicName = dicNameWOExt;
					sdi.dicPath = dicPath;
					sdi.fillMarks();
					File ann = new File(sdi.dicPath + "/" + sdi.dicNameWOExt + ".ann");
					sdi.dicEnabled = sdi.idxExists;
					if (sdi.annExists) {
						List<String> allLines = FileUtils.readLinesFromFile(ann.getAbsolutePath());
						for (String s : allLines) {
							if (!StrUtils.isEmptyStr(s)) {
								if (StrUtils.isEmptyStr(sdi.dicDescription))
									sdi.dicDescription = s;
								else
									sdi.dicDescription += "\n" + s;
							}
						}
					}
					dics.add(sdi);
				}
			}
		}
		ArrayList<OfflineDicInfo> resDics = new ArrayList<>();
		for (OfflineDicInfo odi: dicsConf) {
			for (OfflineDicInfo odi2: dics) {
				if (odi.dicPath.equals(odi2.dicPath)) {
					odi2.dicEnabled = odi.dicEnabled;
					if ((!odi2.dslDzExists) && (!odi2.dslExists)) {
						if (!odi2.dbExists) odi2.dicEnabled = false;
					} else {
						if (!odi2.idxExists) odi2.dicEnabled = false;
					}
					odi2.langFrom = odi.langFrom;
					odi2.langTo = odi.langTo;
					odi2.displayFormat = odi.displayFormat;
					resDics.add(odi2);
					break;
				}
			}
		}
		for (OfflineDicInfo odi: dics) {
			boolean found = false;
			for (OfflineDicInfo odi2: resDics) {
				if (odi.dicPath.equals(odi2.dicPath)) {
					found = true;
					break;
				}
			}
			if (!found) resDics.add(odi);
		}
		boolean doScan = true;
		while (doScan) {
			doScan = false;
			for (OfflineDicInfo odi : resDics) {
				odi.fillMarks();
				if ((!odi.dbExists) && (!odi.dictExists) && (!odi.dictDzExists)
						&& (!odi.dslExists) && (!odi.dslDzExists)) {
					resDics.remove(odi);
					doScan = true;
					break;
				}
			}
		}
		return resDics;
	}

	public OfflineDicsDlg(CoolReader coolReader)
	{
		super(coolReader, coolReader.getResources().getString(R.string.offline_dics), true, true);
		Log.i("ASDF", "OfflineDicsDlg: " + this.getClass().getName());
        setCancelable(true);
		this.mCoolReader = coolReader;
		this.mScanControl = new Scanner.ScanControl();
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(mCoolReader, isEInk);
		mDics = fillOfflineDics();
        mInflater = LayoutInflater.from(getContext());
        mDialogView = mInflater.inflate(R.layout.offline_dicts_dialog, null);

		ViewGroup body = mDialogView.findViewById(R.id.dict_list);
		mList = new DictList(mActivity, false);
		body.addView(mList);
		mCoolReader.tintViewIcons(mDialogView);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setView(mDialogView);
	}
}
