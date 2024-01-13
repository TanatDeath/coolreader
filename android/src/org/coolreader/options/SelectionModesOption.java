package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.readerview.ReaderView;
import org.coolreader.crengine.Settings;

public class SelectionModesOption extends SubmenuOption {

	static int[] mSelectionAction = new int[] {
			ReaderView.SELECTION_ACTION_SAME_AS_COMMON,
			ReaderView.SELECTION_ACTION_TOOLBAR,
			ReaderView.SELECTION_ACTION_COPY,
			ReaderView.SELECTION_ACTION_DICTIONARY,
			ReaderView.SELECTION_ACTION_BOOKMARK,
			ReaderView.SELECTION_ACTION_BOOKMARK_QUICK,
			ReaderView.SELECTION_ACTION_FIND,
			ReaderView.SELECTION_ACTION_SEARCH_WEB,
			ReaderView.SELECTION_ACTION_SEND_TO,
			ReaderView.SELECTION_ACTION_USER_DIC,
			ReaderView.SELECTION_ACTION_CITATION,
			ReaderView.SELECTION_ACTION_DICTIONARY_LIST,
			ReaderView.SELECTION_ACTION_DICTIONARY_1,
			ReaderView.SELECTION_ACTION_DICTIONARY_2,
			ReaderView.SELECTION_ACTION_DICTIONARY_3,
			ReaderView.SELECTION_ACTION_DICTIONARY_4,
			ReaderView.SELECTION_ACTION_DICTIONARY_5,
			ReaderView.SELECTION_ACTION_DICTIONARY_6,
			ReaderView.SELECTION_ACTION_DICTIONARY_7,
			ReaderView.SELECTION_ACTION_DICTIONARY_8,
			ReaderView.SELECTION_ACTION_DICTIONARY_9,
			ReaderView.SELECTION_ACTION_DICTIONARY_10,
			ReaderView.SELECTION_ACTION_COMBO,
			ReaderView.SELECTION_ACTION_SUPER_COMBO,
			ReaderView.SELECTION_ACTION_TOOLBAR_SHORT,
			ReaderView.SELECTION_ACTION_COPY_WITH_PUNCT,
			ReaderView.SELECTION_ACTION_SPEAK_SELECTION
	};

	public static int getSelectionActionTitle(int v) {
		for (int i = 0; i < mSelectionAction.length; i++)
			if (v == mSelectionAction[i]) return mSelectionActionTitles[i];
		return 0;
	}

	static int[] mSelectionActionTitles = new int[] {
			R.string.options_selection_action_same_as_common,
			R.string.options_selection_action_toolbar,
			R.string.options_selection_action_copy,
			R.string.options_selection_action_dictionary,
			R.string.options_selection_action_bookmark,
			R.string.options_selection_action_bookmark_quick,
			R.string.mi_search,
			R.string.mi_search_web,
			R.string.options_selection_action_mail,
			R.string.mi_user_dic,
			R.string.mi_citation,
			R.string.options_selection_action_dictionary_list,
			R.string.options_selection_action_dictionary_1,
			R.string.options_selection_action_dictionary_2,
			R.string.options_selection_action_dictionary_3,
			R.string.options_selection_action_dictionary_4,
			R.string.options_selection_action_dictionary_5,
			R.string.options_selection_action_dictionary_6,
			R.string.options_selection_action_dictionary_7,
			R.string.options_selection_action_dictionary_8,
			R.string.options_selection_action_dictionary_9,
			R.string.options_selection_action_dictionary_10,
			R.string.online_combo,
			R.string.online_super_combo,
			R.string.options_selection_action_toolbar_short,
			R.string.options_selection_action_copy_with_punct,
			R.string.speak_selection
	};

	int[] mSelectionActionAddInfos = new int[] {
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
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.online_combo_add_info,
			R.string.online_super_combo_add_info,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mMultiSelectionAction = new int[] {
			ReaderView.SELECTION_ACTION_SAME_AS_COMMON,
			ReaderView.SELECTION_ACTION_TOOLBAR,
			ReaderView.SELECTION_ACTION_COPY,
			ReaderView.SELECTION_ACTION_DICTIONARY,
			ReaderView.SELECTION_ACTION_BOOKMARK,
			ReaderView.SELECTION_ACTION_BOOKMARK_QUICK,
			ReaderView.SELECTION_ACTION_FIND,
			ReaderView.SELECTION_ACTION_SEARCH_WEB,
			ReaderView.SELECTION_ACTION_SEND_TO,
			ReaderView.SELECTION_ACTION_USER_DIC,
			ReaderView.SELECTION_ACTION_CITATION,
			ReaderView.SELECTION_ACTION_DICTIONARY_LIST,
			ReaderView.SELECTION_ACTION_DICTIONARY_1,
			ReaderView.SELECTION_ACTION_DICTIONARY_2,
			ReaderView.SELECTION_ACTION_DICTIONARY_3,
			ReaderView.SELECTION_ACTION_DICTIONARY_4,
			ReaderView.SELECTION_ACTION_DICTIONARY_5,
			ReaderView.SELECTION_ACTION_DICTIONARY_6,
			ReaderView.SELECTION_ACTION_DICTIONARY_7,
			ReaderView.SELECTION_ACTION_DICTIONARY_8,
			ReaderView.SELECTION_ACTION_DICTIONARY_9,
			ReaderView.SELECTION_ACTION_DICTIONARY_10,
			ReaderView.SELECTION_ACTION_COMBO,
			ReaderView.SELECTION_ACTION_SUPER_COMBO,
			ReaderView.SELECTION_ACTION_TOOLBAR_SHORT,
			ReaderView.SELECTION_ACTION_COPY_WITH_PUNCT,
			ReaderView.SELECTION_ACTION_SPEAK_SELECTION
	};
	int[] mMultiSelectionActionTitles = new int[] {
			R.string.options_selection_action_same_as_common,
			R.string.options_selection_action_toolbar,
			R.string.options_selection_action_copy,
			R.string.options_selection_action_dictionary,
			R.string.options_selection_action_bookmark,
			R.string.options_selection_action_bookmark_quick,
			R.string.mi_search,
			R.string.mi_search_web,
			R.string.options_selection_action_mail,
			R.string.mi_user_dic,
			R.string.mi_citation,
			R.string.options_selection_action_dictionary_list,
			R.string.options_selection_action_dictionary_1,
			R.string.options_selection_action_dictionary_2,
			R.string.options_selection_action_dictionary_3,
			R.string.options_selection_action_dictionary_4,
			R.string.options_selection_action_dictionary_5,
			R.string.options_selection_action_dictionary_6,
			R.string.options_selection_action_dictionary_7,
			R.string.options_selection_action_dictionary_8,
			R.string.options_selection_action_dictionary_9,
			R.string.options_selection_action_dictionary_10,
			R.string.online_combo,
			R.string.online_super_combo,
			R.string.options_selection_action_toolbar_short,
			R.string.options_selection_action_copy_with_punct,
			R.string.speak_selection
	};

	int[] mMultiSelectionActionAddInfo = new int[] {
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
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.online_combo_add_info,
			R.string.online_super_combo_add_info,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	final BaseActivity mActivity;
	final Context mContext;

	public SelectionModesOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_SELECTION_MODES_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("SelectionModesOption", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_selection_action), Settings.PROP_APP_SELECTION_ACTION,
				mActivity.getString(R.string.options_selection_action_add_info), this.lastFilteredValue).addSkip1(mSelectionAction, mSelectionActionTitles, mSelectionActionAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection1, R.drawable.icons8_document_selection1));
		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_multi_selection_action), Settings.PROP_APP_MULTI_SELECTION_ACTION,
				mActivity.getString(R.string.options_multi_selection_action_add_info), this.lastFilteredValue).addSkip1(mMultiSelectionAction, mMultiSelectionActionTitles, mMultiSelectionActionAddInfo).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection2, R.drawable.icons8_document_selection2));
		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_selection_action_long),
				Settings.PROP_APP_SELECTION_ACTION_LONG, mActivity.getString(R.string.options_selection_action_long_add_info), this.lastFilteredValue).addSkip1(mSelectionAction, mSelectionActionTitles, mSelectionActionAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection1_long, R.drawable.icons8_document_selection1_long));
		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_selection2_action), Settings.PROP_APP_SELECTION2_ACTION,
				mActivity.getString(R.string.options_selection_action_add_info), this.lastFilteredValue).add(mSelectionAction, mSelectionActionTitles, mSelectionActionAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection1, R.drawable.icons8_document_selection1));
		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_multi_selection2_action), Settings.PROP_APP_MULTI_SELECTION2_ACTION,
				mActivity.getString(R.string.options_multi_selection_action_add_info), this.lastFilteredValue).add(mMultiSelectionAction, mMultiSelectionActionTitles, mMultiSelectionActionAddInfo).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection2, R.drawable.icons8_document_selection2));
		// plotn: this action cannot be used in special modes, so - comment it out
//			listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_selection2_action_long),
//					Settings.PROP_APP_SELECTION2_ACTION_LONG, mActivity.getString(R.string.options_selection_action_long_add_info), this.lastFilteredValue).add(mSelectionAction, mSelectionActionTitles, mSelectionActionAddInfos).setDefaultValue("0").
//					setIconIdByAttr(R.attr.attr_icons8_document_selection1_long, R.drawable.icons8_document_selection1_long));
		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_selection3_action), Settings.PROP_APP_SELECTION3_ACTION,
				mActivity.getString(R.string.options_selection_action_add_info), this.lastFilteredValue).add(mSelectionAction, mSelectionActionTitles, mSelectionActionAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection1, R.drawable.icons8_document_selection1));
		listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_multi_selection3_action), Settings.PROP_APP_MULTI_SELECTION3_ACTION,
				mActivity.getString(R.string.options_multi_selection_action_add_info), this.lastFilteredValue).add(mMultiSelectionAction, mMultiSelectionActionTitles, mMultiSelectionActionAddInfo).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection2, R.drawable.icons8_document_selection2));
//			listView.add(new ListOptionAction(mOwner, mActivity.getString(R.string.options_selection3_action_long),
//					Settings.PROP_APP_SELECTION3_ACTION_LONG, mActivity.getString(R.string.options_selection_action_long_add_info), this.lastFilteredValue).add(mSelectionAction, mSelectionActionTitles, mSelectionActionAddInfos).setDefaultValue("0").
//					setIconIdByAttr(R.attr.attr_icons8_document_selection1_long, R.drawable.icons8_document_selection1_long));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_selection_action), Settings.PROP_APP_SELECTION_ACTION,
				mActivity.getString(R.string.options_selection_action_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_multi_selection_action), Settings.PROP_APP_MULTI_SELECTION_ACTION,
				mActivity.getString(R.string.options_multi_selection_action_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_selection_action_long), Settings.PROP_APP_SELECTION_ACTION_LONG,
				mActivity.getString(R.string.options_selection_action_long_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_selection2_action), Settings.PROP_APP_SELECTION2_ACTION,
				mActivity.getString(R.string.options_selection_action_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_multi_selection2_action), Settings.PROP_APP_MULTI_SELECTION2_ACTION,
				mActivity.getString(R.string.options_multi_selection_action_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_selection2_action_long), Settings.PROP_APP_SELECTION2_ACTION_LONG,
				mActivity.getString(R.string.options_selection_action_long_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_selection3_action), Settings.PROP_APP_SELECTION3_ACTION,
				mActivity.getString(R.string.options_selection_action_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_multi_selection3_action), Settings.PROP_APP_MULTI_SELECTION3_ACTION,
				mActivity.getString(R.string.options_multi_selection_action_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_selection3_action_long), Settings.PROP_APP_SELECTION3_ACTION_LONG,
				mActivity.getString(R.string.options_selection_action_long_add_info));
		for (int i: mSelectionActionTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mSelectionActionAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}
