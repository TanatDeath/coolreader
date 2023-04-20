package org.coolreader.crengine;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.layouts.FlowLayout;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.util.ArrayList;

public class TagsEditDialog extends BaseDialog {

	public interface TagsEditDialogCloseCallback {
		void onOk();
		void onCancel();
	}

	private final LayoutInflater mInflater;
	private final FileInfo mItem;
	private final ImageButton mAddTag;
	private final EditText mTagText;
	private final View mView;
	private final TextView mLblTagAction;
	private final TextView mLblTagComment;
	private final ImageButton mBtnClearAction;
	private final boolean mSelectionEnabled;
	private final TagsEditDialogCloseCallback mCallback;
	boolean changed = false;
	int mNewTextSize;
	ArrayList<BookTag> mBookTagsList;
	FlowLayout mFlTags;
	private Properties props;
	private BookTag tagToRename = null;
	private ArrayList<BookTag> bookTags = new ArrayList<>();
	private ArrayList<BookTag> bookTagsWas;

	private void addTagButton(BookTag bookTag, String savedTag) {
		//LinearLayout dicButton = new LinearLayout(mActivity);
		View buttonView = mInflater.inflate(R.layout.tag_flow_item, null);
		LinearLayout dicTag = buttonView.findViewById(R.id.tag_flow_item_body);
		String txt = bookTag.name;
		if (bookTag.bookCnt > 0) // not hier! show real qty
			txt = txt + " (" + bookTag.bookCnt + ")";
		TextView tvText = buttonView.findViewById(R.id.tag_flow_item_text);
		tvText.setText(txt);
		dicTag.setBackgroundColor(CoverpageManager.randomColor((bookTag.name).hashCode()));
		dicTag.setPadding(5, 5, 5, 5);
		if (bookTagsWas != null)
			for (BookTag bt: bookTagsWas) {
				if (bt.name.equals(bookTag.name)) {
					bookTag.isSelected = bt.isSelected;
					break;
				}
			}
		if (!StrUtils.isEmptyStr(savedTag))
			if ((mItem != null) && (bookTag.name.equals(savedTag))) bookTag.isSelected = true;
		if (bookTag.isSelected) {
			Utils.setDashedView(dicTag);
			tvText.setPaintFlags(tvText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			tvText.setTypeface(null, Typeface.BOLD);
		}
		final BookTag thisTag = bookTag;
		bookTags.add(thisTag);
		dicTag.setOnClickListener(v -> {
			thisTag.isSelected = !thisTag.isSelected;
			if (thisTag.isSelected) {
				Utils.setDashedView(dicTag);
				tvText.setPaintFlags(tvText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				tvText.setTypeface(null, Typeface.BOLD);
			} else {
				dicTag.setBackgroundColor(CoverpageManager.randomColor((bookTag.name).hashCode()));
				tvText.setPaintFlags(tvText.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
				tvText.setTypeface(null, Typeface.NORMAL);
			}
		});
		dicTag.setOnLongClickListener(v -> {
			mActivity.askConfirmation(R.string.rename_tag, () -> {
				mLblTagAction.setText(mActivity.getString(R.string.dlg_button_rename_tag) + ": " + thisTag.name);
				mBtnClearAction.setVisibility(View.VISIBLE);
				tagToRename = thisTag;
				mTagText.setText(thisTag.name);
			});
			return true;
		});
		ImageView btnDel = buttonView.findViewById(R.id.tag_flow_value_del);
		btnDel.setOnClickListener(v -> {
			mActivity.askConfirmation(R.string.delete_tag, () -> {
				mActivity.getDB().deleteTag(thisTag.name, o -> {
					mTagText.setText("");
					mLblTagAction.setText(R.string.dlg_button_add_tag);
					mBtnClearAction.setVisibility(View.INVISIBLE);
					tagToRename = null;
					refreshButtons("");
					changed = true;
				});
			});
		});
		TextView tv = new TextView(mActivity);
		tv.setText(" ");
		tv.setPadding(5, 0, 0, 0);
		tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
		tv.setTextColor(mActivity.getTextColor(colorIcon));
		mActivity.tintViewIcons(dicTag);
		mFlTags.addView(dicTag);
		mFlTags.addView(tv);
	}

	private void refreshButtons(String savedTag) {
		mFlTags.removeAllViews();
		bookTagsWas = bookTags;
		bookTags = new ArrayList<>();
		mActivity.waitForCRDBService(() ->
			mActivity.getDB().loadTags(mItem, tags -> {
				mBookTagsList = tags;
				for (BookTag bookTag: mBookTagsList) {
					//mActivity.showClassicToast(bookTag.name);
					addTagButton(bookTag, savedTag);
				}
			}));
	}

	public TagsEditDialog(CoolReader activity, FileInfo item, boolean selectionEnabled,
						  TagsEditDialogCloseCallback callback) {
		super(activity, item == null ?
				activity.getString(R.string.tags_edit_dialog): activity.getString(R.string.add_book_tags),
				item != null,false);
		mCallback = callback;
		props = new Properties(mActivity.settings());
		mNewTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
		mItem = item;
		mSelectionEnabled = selectionEnabled;
		mInflater = LayoutInflater.from(getContext());
		mView = mInflater.inflate(R.layout.tags_edit_dialog, null);
		mAddTag = mView.findViewById(R.id.tags_dlg_add_tag);
		mTagText = mView.findViewById(R.id.tag_text);
		mLblTagAction = mView.findViewById(R.id.lbl_tag_action);
		mLblTagComment = mView.findViewById(R.id.lbl_tag_comment);
		if (item == null) Utils.hideView(mLblTagComment);
		mBtnClearAction = mView.findViewById(R.id.tags_dlg_clear_action);
		mBtnClearAction.setVisibility(View.INVISIBLE);
		mBtnClearAction.setOnClickListener(v -> {
			mLblTagAction.setText(R.string.dlg_button_add_tag);
			mBtnClearAction.setVisibility(View.INVISIBLE);
			tagToRename = null;
			mTagText.setText("");
		});
		mAddTag.setOnClickListener(v -> {
			String tagText = StrUtils.getNonEmptyStr(mTagText.getText().toString(), true);
			if (StrUtils.isEmptyStr(tagText)) {
				mActivity.showToast(R.string.value_is_empty);
			} else {
				String oldTag = "";
				if (tagToRename != null) oldTag = tagToRename.name;
				mActivity.getDB().saveTag(tagText, oldTag, o -> {
					mTagText.setText("");
					mLblTagAction.setText(R.string.dlg_button_add_tag);
					mBtnClearAction.setVisibility(View.INVISIBLE);
					tagToRename = null;
					refreshButtons(tagText);
					changed = true;
				});
			}
		});
		setView(mView);
		mFlTags = mView.findViewById(R.id.tagsFlowList);
		refreshButtons("");
	}

	@Override
	protected void onPositiveButtonClick() {
		if (mItem != null) {
			mActivity.getDB().addTagsToBook(bookTags, mItem, o -> {
				if ((Boolean) o) {
					super.onPositiveButtonClick();
					if (mCallback != null) mCallback.onOk();
				}
				else
					mActivity.showClassicToast(mActivity.getString(R.string.cannot_add_book_tags));
			});
		} else {
			super.onPositiveButtonClick();
			if (mCallback != null) mCallback.onOk();
		}
		//if (changed) mActivity.getDB().clearCaches //TODO?
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
		if (mCallback != null) mCallback.onCancel();
	}

}
