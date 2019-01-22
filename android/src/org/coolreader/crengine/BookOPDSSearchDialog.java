package org.coolreader.crengine;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CRDBService;

import java.util.ArrayList;

public class BookOPDSSearchDialog extends BaseDialog {

	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	final EditText termsEdit;
	private FileInfo fInfo;
	public final BookSearchDialog.SearchCallback callback;

	public BookOPDSSearchDialog(CoolReader activity, FileInfo fi, BookSearchDialog.SearchCallback callback)
	{
		super("BookOPDSSearchDialog", activity, activity.getString( R.string.dlg_book_search), true, false);
		mCoolReader = activity;
		fInfo = fi;
		setTitle(mCoolReader.getString( R.string.dlg_book_search));
		this.callback = callback;
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.book_opds_search_dialog, null);
		termsEdit = (EditText)view.findViewById(R.id.search_text_terms);
		setView( view );
	}

	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
		FileInfo fi2 = new FileInfo(fInfo);
		fi2.pathname = fi2.pathname.replace("{searchTerms}", termsEdit.getText());
		FileInfo[] arrFi = {fi2};
		callback.done(arrFi);
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
		callback.done(null);
	}
}
