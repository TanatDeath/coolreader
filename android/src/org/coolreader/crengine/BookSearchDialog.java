package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.HashMap;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CRDBService;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class BookSearchDialog extends BaseDialog {
	
	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	final EditText authorEdit;
	final EditText titleEdit;
	final EditText seriesEdit;
	final EditText filenameEdit;
	final TextView statusText;
	public final SearchCallback callback;
	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;

	private int searchTaskId = 0;
	private boolean searchActive = false;
	private boolean closing = false;
	
	public BookSearchDialog(CoolReader activity, SearchCallback callback)
	{
		super("BookSearchDialog", activity, activity.getString( R.string.dlg_book_search), true, false);
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(activity, isEInk);
		mCoolReader = activity;
		this.callback = callback;
		setTitle(mCoolReader.getString( R.string.dlg_book_search));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.book_search_dialog, null);
		authorEdit = view.findViewById(R.id.search_text_author);
		titleEdit = view.findViewById(R.id.search_text_title);
		seriesEdit = view.findViewById(R.id.search_text_series);
		filenameEdit = view.findViewById(R.id.search_text_filename);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		int colorIcon128 = Color.argb(128,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon));
		authorEdit.setHintTextColor(colorIcon128);
		titleEdit.setHintTextColor(colorIcon128);
		seriesEdit.setHintTextColor(colorIcon128);
		filenameEdit.setHintTextColor(colorIcon128);

		statusText = view.findViewById(R.id.search_status);
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
				postSearchTask();
			}
			
		}; 
		authorEdit.addTextChangedListener(watcher);
		seriesEdit.addTextChangedListener(watcher);
		titleEdit.addTextChangedListener(watcher);
		filenameEdit.addTextChangedListener(watcher);
		setView( view );
	}

	private void postSearchTask() {
		if ( closing )
			return;
		final int mySearchTaskId = ++searchTaskId;
		BackgroundThread.instance().postGUI(() -> {
			if ( searchTaskId == mySearchTaskId ) {
				if ( searchActive )
					return;
				searchActive = true;
				find( new SearchCallback() {

					@Override
					public void start() {

					}

					@Override
					public void done(FileInfo[] results) {
						searchActive = false;
						statusText.setText(mCoolReader.getString(R.string.dlg_book_search_found) + " " + results.length);
						if ( searchTaskId != mySearchTaskId ) {
							postSearchTask();
						}
					}
				});
			}
		}, 3000);
	}
	
	public interface SearchCallback {
		void start();
		void done(FileInfo[] results);
	}

//	private static String addWildcard( String s, boolean before, boolean after ) {
//		if ( s==null || s.length()==0 )
//			return s;
//		if ( before )
//			s = "%" + s;
//		if ( after )
//			s = s + "%";
//		return s;
//	}
	
	private final static int MAX_RESULTS = 50; 
	protected void find( final SearchCallback cb ) {
		final String author = authorEdit.getText().toString().trim();
		final String series = seriesEdit.getText().toString().trim();
		final String title = titleEdit.getText().toString().trim();
		final String filename = filenameEdit.getText().toString().trim();
		if (mCoolReader == null || mCoolReader.getDB() == null)
			return;
		mCoolReader.getDB().findByPatterns(MAX_RESULTS, author, title, series, filename, new CRDBService.BookSearchCallback() {

			@Override
			public void onBooksSearchBegin() {
				cb.start();
			}

			@Override
			public void onBooksFound(ArrayList<FileInfo> fileList) {
				cb.done(fileList.toArray(new FileInfo[fileList.size()]));
			}
		});
	}

	protected void qfind( final SearchCallback cb, String sText ) {
		if (mCoolReader == null || mCoolReader.getDB() == null)
			return;
		mCoolReader.getDB().findByPatterns(MAX_RESULTS, sText, "##QUICK_SEARCH##",
				"", "", new CRDBService.BookSearchCallback() {

			@Override
			public void onBooksSearchBegin() {
				cb.start();
			}

			@Override
			public void onBooksFound(ArrayList<FileInfo> fileList) {
				cb.done(fileList.toArray(new FileInfo[fileList.size()]));
			}
		});
	}
	
	@Override
	protected void onPositiveButtonClick() {
		searchTaskId++;
		closing = true;
		super.onPositiveButtonClick();
		find( callback );
	}

	@Override
	protected void onNegativeButtonClick() {
		searchTaskId++;
		closing = true;
		super.onNegativeButtonClick();
		callback.done(null);
	}
}
