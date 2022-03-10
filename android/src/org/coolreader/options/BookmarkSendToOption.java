package org.coolreader.options;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;
import org.coolreader.readerview.ReaderView;

public class BookmarkSendToOption extends SubmenuOption {

	static int[] mBookmarkSendToActionModTitles = new int[] {
			R.string.options_bookmark_action_send_to_mod1,
			R.string.options_bookmark_action_send_to_mod2,
			R.string.options_bookmark_action_send_to_mod3,
			R.string.options_bookmark_action_send_to_mod4,
			R.string.options_bookmark_action_send_to_mod5,
			R.string.options_bookmark_action_send_to_mod6,
			R.string.options_bookmark_action_send_to_mod7,
			R.string.options_bookmark_action_send_to_mod8
	};

	int[] mBookmarkSendToActionModAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	static int[] mBookmarkSendToActionTitles = new int[] {
			R.string.action_none,
			R.string.options_selection_action_dictionary_1,
			R.string.options_selection_action_dictionary_2,
			R.string.options_selection_action_dictionary_3,
			R.string.options_selection_action_dictionary_4,
			R.string.options_selection_action_dictionary_5,
			R.string.options_selection_action_dictionary_6,
			R.string.options_selection_action_dictionary_7,
			R.string.options_selection_action_dictionary_8,
			R.string.options_selection_action_dictionary_9,
			R.string.options_selection_action_dictionary_10
	};

	int[] mBookmarkSendToActionAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	static int[] mBookmarkSendToAction = new int[] {
			ReaderView.SEND_TO_ACTION_NONE,
			ReaderView.SELECTION_ACTION_DICTIONARY_1,
			ReaderView.SELECTION_ACTION_DICTIONARY_2,
			ReaderView.SELECTION_ACTION_DICTIONARY_3,
			ReaderView.SELECTION_ACTION_DICTIONARY_4,
			ReaderView.SELECTION_ACTION_DICTIONARY_5,
			ReaderView.SELECTION_ACTION_DICTIONARY_6,
			ReaderView.SELECTION_ACTION_DICTIONARY_7,
			ReaderView.SELECTION_ACTION_DICTIONARY_8,
			ReaderView.SELECTION_ACTION_DICTIONARY_9,
			ReaderView.SELECTION_ACTION_DICTIONARY_10

	};

	static int[] mBookmarkSendToActionMod = new int[] {
			0,
			1,
			2,
			3,
			4,
			5,
			6,
			7
	};

	final BaseActivity mActivity;
	final OptionsDialog mOptionsDialog;

	public BookmarkSendToOption(BaseActivity activity, OptionsDialog od, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_RARE_TITLE, addInfo, filter);
		mActivity = activity;
		mOptionsDialog = od;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("BookmarkSendToOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mOptionsDialog.getContext(), this);

		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_bookmark_action_send_to),
				Settings.PROP_APP_BOOKMARK_ACTION_SEND_TO, mActivity.getString(R.string.options_bookmark_action_send_to_add_info), this.lastFilteredValue).
				add(mBookmarkSendToAction, mBookmarkSendToActionTitles, mBookmarkSendToActionAddInfos).setDefaultValue("-1").
				setIconIdByAttr(R.attr.attr_icons8_send_to_action, R.drawable.icons8_send_to_action));
		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_bookmark_action_send_to_mod),
				Settings.PROP_APP_BOOKMARK_ACTION_SEND_TO_MOD, mActivity.getString(R.string.options_bookmark_action_send_to_mod_add_info), this.lastFilteredValue).
				add(mBookmarkSendToActionMod, mBookmarkSendToActionModTitles, mBookmarkSendToActionModAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_send_to_action_more, R.drawable.icons8_send_to_action_more));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_bookmark_action_send_to), Settings.PROP_APP_BOOKMARK_ACTION_SEND_TO,
				mActivity.getString(R.string.options_bookmark_action_send_to_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_bookmark_action_send_to_mod), Settings.PROP_APP_BOOKMARK_ACTION_SEND_TO_MOD,
				mActivity.getString(R.string.options_bookmark_action_send_to_mod_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
