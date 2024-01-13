package org.coolreader.crengine;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.coolreader.BuildConfig;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.options.OptionBase;
import org.coolreader.readerview.ReaderView;
import org.coolreader.utils.Utils;

public class ReaderAction {
	final public String id;
	final private int shortNameId;

	public String getShortNameText(Context activity) {
		if (shortNameId != 0)
			return activity.getString(shortNameId);
		if (nameId != 0)
			return activity.getString(nameId);
		if (actionOption != null)
			return "*" + actionOption.label;
		return "[NONE]";
	}

	final private int nameId;

	public String getNameText(Context activity) {
		if (nameId != 0)
			return activity.getString(nameId);
		if (actionOption != null)
			return "*" + actionOption.label;
		return "[NONE]";
	}

	final public int addInfoR;

	public static ReaderAction[] getDefReaderActions() {
		return new ReaderAction[] {
				ReaderAction.GO_BACK,
				ReaderAction.TOC,
				ReaderAction.BOOK_INFO,
				ReaderAction.FONTS_MENU,
				ReaderAction.SEARCH,
				ReaderAction.OPTIONS,
				ReaderAction.BOOKMARKS,
				ReaderAction.FILE_BROWSER_ROOT,
				ReaderAction.TOGGLE_DAY_NIGHT,
				ReaderAction.TOGGLE_SELECTION_MODE,
				ReaderAction.GO_PAGE,
				//ReaderAction.GO_PERCENT,
				ReaderAction.FILE_BROWSER,
				ReaderAction.TTS_PLAY,
				ReaderAction.GO_FORWARD,
				ReaderAction.RECENT_BOOKS,
				ReaderAction.OPEN_PREVIOUS_BOOK,
				ReaderAction.TOGGLE_AUTOSCROLL,
				ReaderAction.ABOUT,
				ReaderAction.HIDE
		};
	}

	public void setMirrorAction(ReaderAction mirrorAction) {
		this.mirrorAction = mirrorAction;
	}

	public ReaderAction getMirrorAction() {
		if (mirrorAction!=null) return mirrorAction;
		ReaderAction[] actions_all = ReaderAction.AVAILABLE_ACTIONS;
		for (ReaderAction a : actions_all)
			if ((a!=this) && (a.mirrorAction == this)) return a;
		return null;
	}

	public ReaderAction mirrorAction = null;

	public OptionBase actionOption = null;

	public int getIconId() {
		return iconId;
	}

	public int getIconIdWithDef(BaseActivity activity) {
		if (iconId == 0) {
			if (activity==null)
				return R.drawable.cr3_option_other;
				else return Utils.resolveResourceIdByAttr(activity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
		}
		return iconId;
	}

	public void setupIconView(BaseActivity activity, ImageView icon) {
		if (null == icon)
			return;
		int resId = iconId;
		if (resId != 0) {
			icon.setImageResource(resId);
			icon.setVisibility(View.VISIBLE);
			activity.tintViewIcons(icon,true);
		} else {
			icon.setImageResource(R.drawable.icons8_more);
			icon.setVisibility(View.VISIBLE);
			activity.tintViewIcons(icon,true);
		}
	}

	public int iconId;
	final public ReaderCommand cmd;
	final public int param;
	final public int menuItemId;
	private boolean canRepeat = false;
	private boolean mayAssignOnKey = true;
	private boolean mayAssignOnTap = true;
	private boolean activateWithLongMenuKey = false;
	private ReaderAction setActivateWithLongMenuKey() { this.activateWithLongMenuKey = true; return this; }
	public ReaderAction setIconId(int iconId) { this.iconId = iconId; return this; }
	private ReaderAction setCanRepeat() { canRepeat = true; return this; }
	//private ReaderAction dontAssignOnKey() { mayAssignOnKey=false; return this; }
	private ReaderAction dontAssignOnTap() { mayAssignOnTap = false; return this; }
	public boolean canRepeat() { return canRepeat; }
	public boolean mayAssignOnKey() { return mayAssignOnKey; }
	public boolean mayAssignOnTap() { return mayAssignOnTap; }
	public boolean activateWithLongMenuKey() { return activateWithLongMenuKey; }

	public ReaderAction(String id, int shortNameId, int nameId, ReaderCommand cmd, int param, int menuItemId, ReaderAction mirrorAction, int addInfoR) {
		super();
		this.id = id;
		this.nameId = nameId;
		this.shortNameId = shortNameId;
		this.cmd = cmd;
		this.param = param;
		this.menuItemId = menuItemId;
		this.iconId = 0;
		this.mirrorAction = mirrorAction;
		this.addInfoR = addInfoR;
		this.actionOption = null;
	}

	public ReaderAction(String id, int shortNameId, int nameId, ReaderCommand cmd, int param, int menuItemId, ReaderAction mirrorAction, int addInfoR,
			OptionBase actionOption) {
		super();
		this.id = id;
		this.nameId = nameId;
		this.shortNameId = shortNameId;
		this.cmd = cmd;
		this.param = param;
		this.menuItemId = menuItemId;
		this.iconId = 0;
		this.mirrorAction = mirrorAction;
		this.addInfoR = addInfoR;
		this.actionOption = actionOption;
	}

	public String toString() {
		return id;
	}

	public final static ReaderAction NONE = new ReaderAction("NONE", 0, R.string.action_none, ReaderCommand.DCMD_NONE, 0 , 0,null, R.string.option_add_info_empty_text);
	public final static ReaderAction REPEAT = new ReaderAction("REPEAT", 0, R.string.action_repeat, ReaderCommand.DCMD_REPEAT, 0 , 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_repeat);
	public final static ReaderAction PAGE_DOWN = new ReaderAction("PAGE_DOWN", 0, R.string.action_pagedown, ReaderCommand.DCMD_PAGEDOWN, 1 , 0, null, R.string.option_add_info_empty_text).setCanRepeat().setIconId(R.drawable.icons8_document_down);
	public final static ReaderAction PAGE_DOWN_10 = new ReaderAction("PAGE_DOWN_10", 0, R.string.action_pagedown_10, ReaderCommand.DCMD_PAGEDOWN, 10 ,0, null, R.string.option_add_info_empty_text).setCanRepeat().setIconId(R.drawable.icons8_document_down2);
	public final static ReaderAction PAGE_UP = new ReaderAction("PAGE_UP", 0, R.string.action_pageup, ReaderCommand.DCMD_PAGEUP, 1 , 0, PAGE_DOWN, R.string.option_add_info_empty_text).setCanRepeat().setIconId(R.drawable.icons8_document_up);
	public final static ReaderAction PAGE_UP_10 = new ReaderAction("PAGE_UP_10", 0, R.string.action_pageup_10, ReaderCommand.DCMD_PAGEUP, 10 , 0, PAGE_DOWN_10, R.string.option_add_info_empty_text).setCanRepeat().setIconId(R.drawable.icons8_document_up2);
	public final static ReaderAction ZOOM_IN = new ReaderAction("ZOOM_IN", 0, R.string.mi_font_size_increase, ReaderCommand.DCMD_ZOOM_IN, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_increase_font_2); //,  R.id.cr3_mi_font_size_increase
	public final static ReaderAction ZOOM_OUT = new ReaderAction("ZOOM_OUT", 0, R.string.mi_font_size_decrease, ReaderCommand.DCMD_ZOOM_OUT, 1, 0, ZOOM_IN, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_decrease_font_1); //,  R.id.cr3_mi_font_size_decrease
    public final static ReaderAction FONT_SELECT = new ReaderAction("FONT_SELECT", 0, R.string.mi_font_select, ReaderCommand.DCMD_FONT_SELECT, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_font_face);
    public final static ReaderAction FONT_BOLD = new ReaderAction("FONT_BOLD", 0, R.string.mi_font_bold, ReaderCommand.DCMD_FONT_BOLD, 1, 0, FONT_SELECT, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_bold);
	public final static ReaderAction DOCUMENT_STYLES = new ReaderAction("DOCUMENT_STYLES", R.string.action_toggle_document_styles_short, R.string.action_toggle_document_styles, ReaderCommand.DCMD_TOGGLE_DOCUMENT_STYLES, 0, R.id.cr3_mi_toggle_document_styles, null, R.string.option_add_info_empty_text ).setIconId(R.drawable.icons8_css_2);
	public final static ReaderAction TEXT_AUTOFORMAT = new ReaderAction("TEXT_AUTOFORMAT", R.string.action_toggle_text_autoformat_short, R.string.action_toggle_text_autoformat, ReaderCommand.DCMD_TOGGLE_TEXT_AUTOFORMAT, 0, R.id.cr3_mi_toggle_text_autoformat, null, R.string.option_add_info_empty_text );
	public final static ReaderAction BOOKMARKS = new ReaderAction("BOOKMARKS", 0, R.string.action_bookmarks, ReaderCommand.DCMD_BOOKMARKS, 0, R.id.cr3_mi_bookmarks, null, R.string.option_add_info_empty_text ).setIconId(R.drawable.cr3_button_bookmarks);
	public final static ReaderAction ABOUT = new ReaderAction("ABOUT", 0, R.string.dlg_about, ReaderCommand.DCMD_ABOUT, 0, R.id.cr3_mi_about, null, R.string.option_add_info_empty_text ).setIconId(R.drawable.known_reader_logo);
	public final static ReaderAction USER_MANUAL = new ReaderAction("USER_MANUAL", 0, R.string.mi_goto_manual, ReaderCommand.DCMD_USER_MANUAL, 0, R.id.cr3_mi_user_manual , ABOUT, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_manual_2);
	public final static ReaderAction BOOK_INFO = new ReaderAction("BOOK_INFO", 0, R.string.dlg_book_info, ReaderCommand.DCMD_BOOK_INFO, 0, R.id.cr3_mi_book_info , null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_info);
	public final static ReaderAction TOC = new ReaderAction("TOC", 0, R.string.action_toc, ReaderCommand.DCMD_TOC_DIALOG, 0, R.id.cr3_go_toc, BOOKMARKS, R.string.option_add_info_empty_text ).setIconId(R.drawable.cr3_viewer_toc);
	public final static ReaderAction SEARCH = new ReaderAction("SEARCH", 0, R.string.action_search, ReaderCommand.DCMD_SEARCH, 0, R.id.cr3_mi_search, null, R.string.option_add_info_empty_text ).setIconId(R.drawable.cr3_viewer_find);
	public final static ReaderAction GO_PAGE = new ReaderAction("GO_PAGE", 0, R.string.action_go_page, ReaderCommand.DCMD_GO_PAGE_DIALOG, 0, R.id.cr3_mi_go_page, null, R.string.option_add_info_empty_text ).setIconId(R.drawable.cr3_button_go_page);
	//public final static ReaderAction GO_PERCENT = new ReaderAction("GO_PERCENT", R.string.action_go_percent, ReaderCommand.DCMD_GO_PERCENT_DIALOG, 0, R.id.cr3_mi_go_percent, GO_PAGE, R.string.option_add_info_empty_text ).setIconId(R.drawable.cr3_button_go_percent);
	public final static ReaderAction FIRST_PAGE = new ReaderAction("FIRST_PAGE", 0, R.string.action_go_first_page, ReaderCommand.DCMD_BEGIN, 0 , 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_document_1);
	public final static ReaderAction LAST_PAGE = new ReaderAction("LAST_PAGE", R.string.action_go_last_page_short, R.string.action_go_last_page, ReaderCommand.DCMD_END, 0 , 0, FIRST_PAGE, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_document_z);
	public final static ReaderAction OPTIONS = new ReaderAction("OPTIONS", 0, R.string.action_options, ReaderCommand.DCMD_OPTIONS_DIALOG, 0, R.id.cr3_mi_options , null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_viewer_settings);
	public final static ReaderAction OPTIONS_FILTER = new ReaderAction("OPTIONS_FILTER", 0, R.string.action_options_filtered, ReaderCommand.DCMD_OPTIONS_DIALOG_FILTERED, 0, R.id.cr3_mi_options , OPTIONS, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_settings_search);
	public final static ReaderAction READER_MENU = new ReaderAction("READER_MENU", 0, R.string.action_reader_menu, ReaderCommand.DCMD_READER_MENU, 0 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_menu);
	public final static ReaderAction TOGGLE_DAY_NIGHT = new ReaderAction("TOGGLE_DAY_NIGHT", 0, R.string.action_toggle_day_night, ReaderCommand.DCMD_TOGGLE_DAY_NIGHT_MODE, 0, R.id.cr3_mi_toggle_day_night , null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_option_night);
	public final static ReaderAction RECENT_BOOKS = new ReaderAction("RECENT_BOOKS", 0, R.string.action_recent_books_list, ReaderCommand.DCMD_RECENT_BOOKS_LIST, 0, R.id.book_recent_books, null, R.string.option_add_info_empty_text ).setIconId(R.drawable.cr3_browser_folder_recent);
	public final static ReaderAction OPDS_CATALOGS = new ReaderAction("OPDS_CATALOGS", 0, R.string.mi_book_opds_root, ReaderCommand.DCMD_OPDS_CATALOGS, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_browser_folder_opds);
	public final static ReaderAction FILE_BROWSER_ROOT = new ReaderAction("FILE_BROWSER_ROOT", 0, R.string.mi_book_root, ReaderCommand.DCMD_FILE_BROWSER_ROOT, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_browser_folder_root);
	public final static ReaderAction FILE_BROWSER = new ReaderAction("FILE_BROWSER", 0, R.string.action_file_browser, ReaderCommand.DCMD_FILE_BROWSER, 0, R.id.cr3_mi_open_file ,RECENT_BOOKS, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_browser_folder);
	public final static ReaderAction FILE_BROWSER_UP = new ReaderAction("FILE_BROWSER_UP", 0, R.string.action_go_back, ReaderCommand.DCMD_FILE_BROWSER_UP, 0, 0, FILE_BROWSER_ROOT, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_button_prev);
	public final static ReaderAction CURRENT_BOOK_DIRECTORY = new ReaderAction("DCMD_CURRENT_BOOK_DIRECTORY", 0, R.string.mi_book_recent_goto, ReaderCommand.DCMD_CURRENT_BOOK_DIRECTORY, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_browser_folder_current_book);
	public final static ReaderAction CURRENT_BOOK = new ReaderAction("DCMD_CURRENT_BOOK", 0, R.string.mi_book_back_to_reading, ReaderCommand.DCMD_CURRENT_BOOK, 0, 0, CURRENT_BOOK_DIRECTORY, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_button_book_open);
	public final static ReaderAction FILE_BROWSER_SORT_ORDER = new ReaderAction("FILE_BROWSER_SORT_ORDER", 0, R.string.mi_book_sort_order, ReaderCommand.DCMD_FILE_BROWSER_SORT_ORDER, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_alphabetical_sorting);
	public final static ReaderAction TOGGLE_DICT_ONCE = new ReaderAction("TOGGLE_DICT_ONCE", R.string.toggle_dict_once_short, R.string.toggle_dict_once, ReaderCommand.DCMD_TOGGLE_DICT_ONCE, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_google_translate_2);
	public final static ReaderAction TOGGLE_DICT = new ReaderAction("TOGGLE_DICT", R.string.toggle_dict_short, R.string.toggle_dict, ReaderCommand.DCMD_TOGGLE_DICT, 0, 0, TOGGLE_DICT_ONCE, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_google_translate_switch);
	public final static ReaderAction WHOLE_PAGE_TO_DIC = new ReaderAction("WHOLE_PAGE_TO_DIC", 0, R.string.whole_page_to_dic, ReaderCommand.DCMD_WHOLE_PAGE_TO_DIC, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_page_to_dic);

	public final static ReaderAction FONT_PREVIOUS = new ReaderAction("FONT_PREVIOUS", 0, R.string.mi_font_previous, ReaderCommand.DCMD_FONT_PREVIOUS, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_font_up); //, R.id.cr3_mi_font_previous
	public final static ReaderAction FONT_NEXT = new ReaderAction("FONT_NEXT", 0, R.string.mi_font_next, ReaderCommand.DCMD_FONT_NEXT, 0, 0, FONT_PREVIOUS, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_font_down); //, R.id.cr3_mi_font_next
	public final static ReaderAction TOGGLE_TOUCH_SCREEN_LOCK = new ReaderAction("TOGGLE_TOUCH_SCREEN_LOCK", R.string.action_touch_screen_toggle_lock_short, R.string.action_touch_screen_toggle_lock, ReaderCommand.DCMD_TOGGLE_TOUCH_SCREEN_LOCK, 0 , 0,null, R.string.option_add_info_empty_text).dontAssignOnTap().setIconId(R.drawable.icons8_lock_portrait_2);
	public final static ReaderAction TOGGLE_ORIENTATION = new ReaderAction("TOGGLE_ORIENTATION", R.string.action_toggle_screen_orientation_short, R.string.action_toggle_screen_orientation, ReaderCommand.DCMD_TOGGLE_ORIENTATION, 0 , 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_orientation);
	public final static ReaderAction TOGGLE_FULLSCREEN = new ReaderAction("TOGGLE_FULLSCREEN", R.string.action_toggle_fullscreen_short, R.string.action_toggle_fullscreen, ReaderCommand.DCMD_TOGGLE_FULLSCREEN, 0 , 0, TOGGLE_ORIENTATION, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_fullscreen);
	public final static ReaderAction TOGGLE_SELECTION_MODE = new ReaderAction("TOGGLE_SELECTION_MODE", R.string.action_toggle_selection_mode_short, R.string.action_toggle_selection_mode, ReaderCommand.DCMD_TOGGLE_SELECTION_MODE, 0, R.id.cr3_mi_select_text, null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_option_touch);
	public final static ReaderAction TOGGLE_INSPECTOR_MODE = new ReaderAction("TOGGLE_INSPECTOR_MODE", R.string.action_toggle_inspector_mode_short, R.string.action_toggle_inspector_mode, ReaderCommand.DCMD_TOGGLE_INSPECTOR_MODE, 0, 0, TOGGLE_SELECTION_MODE, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_night_vision);
	public final static ReaderAction HOME_SCREEN = new ReaderAction("HOME_SCREEN", 0, R.string.action_exit_home_screen, ReaderCommand.DCMD_SHOW_HOME_SCREEN, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_touchscreen);
	public final static ReaderAction GO_BACK = new ReaderAction("GO_BACK", 0, R.string.action_go_back,  ReaderCommand.DCMD_LINK_BACK, 0, R.id.cr3_go_back, null, R.string.option_add_info_empty_text ).setIconId(R.drawable.cr3_button_prev);
	public final static ReaderAction GO_FORWARD = new ReaderAction("GO_FORWARD", 0, R.string.action_go_forward, ReaderCommand.DCMD_LINK_FORWARD, 0, R.id.cr3_go_forward, GO_BACK, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_button_next);
	public final static ReaderAction TTS_PLAY = new ReaderAction("TTS_PLAY", 0,  R.string.mi_tts_play, ReaderCommand.DCMD_TTS_PLAY, 0, R.id.cr3_mi_tts_play, null, R.string.option_add_info_empty_text ).setIconId(R.drawable.cr3_button_tts); //.setActivateWithLongMenuKey()

	//TODO: Make corresponding icon
	public final static ReaderAction TTS_STOP = new ReaderAction("TTS_STOP", 0,  R.string.mi_tts_stop, ReaderCommand.DCMD_TTS_STOP, 0, R.id.cr3_mi_tts_stop, TTS_PLAY, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_speaker_stop); //.setActivateWithLongMenuKey()
	public final static ReaderAction TOGGLE_TITLEBAR = new ReaderAction("TOGGLE_TITLEBAR", 0, R.string.action_toggle_titlebar, ReaderCommand.DCMD_TOGGLE_TITLEBAR, 0 , 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_navigation_toolbar_top);
	public final static ReaderAction SHOW_POSITION_INFO_POPUP = new ReaderAction("SHOW_POSITION_INFO_POPUP", 0, R.string.action_show_position_info, ReaderCommand.DCMD_SHOW_POSITION_INFO_POPUP, 0, 0, BOOK_INFO, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_position_info);
	public final static ReaderAction SHOW_DICTIONARY = new ReaderAction("SHOW_DICTIONARY", 0, R.string.action_show_dictionary, ReaderCommand.DCMD_SHOW_DICTIONARY, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_google_translate);
	public final static ReaderAction OPEN_PREVIOUS_BOOK = new ReaderAction("OPEN_PREVIOUS_BOOK", 0, R.string.action_open_last_book, ReaderCommand.DCMD_OPEN_PREVIOUS_BOOK, 0, R.id.cr3_go_previous_book, BOOK_INFO, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_btn_books_swap);
	public final static ReaderAction TOGGLE_AUTOSCROLL = new ReaderAction("TOGGLE_AUTOSCROLL", 0, R.string.action_toggle_autoscroll, ReaderCommand.DCMD_TOGGLE_AUTOSCROLL, 0, R.id.cr3_mi_toggle_autoscroll, null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_button_scroll_go);
	public final static ReaderAction AUTOSCROLL_SPEED_INCREASE = new ReaderAction("AUTOSCROLL_SPEED_INCREASE", 0, R.string.action_autoscroll_speed_increase, ReaderCommand.DCMD_AUTOSCROLL_SPEED_INCREASE, 0, 0, null, R.string.option_add_info_empty_text);
	public final static ReaderAction AUTOSCROLL_SPEED_DECREASE = new ReaderAction("AUTOSCROLL_SPEED_DECREASE", 0, R.string.action_autoscroll_speed_decrease, ReaderCommand.DCMD_AUTOSCROLL_SPEED_DECREASE, 0, 0, AUTOSCROLL_SPEED_INCREASE, R.string.option_add_info_empty_text);
	public final static ReaderAction START_SELECTION = new ReaderAction("START_SELECTION", 0, R.string.action_toggle_selection_mode, ReaderCommand.DCMD_START_SELECTION, 0, 0, TOGGLE_SELECTION_MODE, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_document_selection1);
	public final static ReaderAction SWITCH_PROFILE = new ReaderAction("SWITCH_PROFILE", R.string.action_switch_settings_profile_short, R.string.action_switch_settings_profile, ReaderCommand.DCMD_SWITCH_PROFILE, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_switch_profile);
	public final static ReaderAction SCAN_DIRECTORY_RECURSIVE = new ReaderAction("SCAN_DIRECTORY_RECURSIVE", 0, R.string.mi_book_scan_recursive, ReaderCommand.DCMD_SCAN_DIRECTORY_RECURSIVE, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_folder_scan);
	public final static ReaderAction NEXT_CHAPTER = new ReaderAction("NEXT_CHAPTER", 0, R.string.action_chapter_next, ReaderCommand.DCMD_MOVE_BY_CHAPTER, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_document_down_ch);
	public final static ReaderAction PREV_CHAPTER = new ReaderAction("PREV_CHAPTER", 0, R.string.action_chapter_prev, ReaderCommand.DCMD_MOVE_BY_CHAPTER, -1, 0, NEXT_CHAPTER, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_document_up_ch);
	public final static ReaderAction SAVE_LOGCAT = new ReaderAction("SAVE_LOGCAT", 0, R.string.action_logcat, ReaderCommand.DCMD_SAVE_LOGCAT, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_log);
	public final static ReaderAction EXIT = new ReaderAction("EXIT", 0, R.string.action_exit, ReaderCommand.DCMD_EXIT, 0, R.id.cr3_mi_exit, null , R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_viewer_exit);
	public final static ReaderAction HIDE = new ReaderAction("HIDE", 0, R.string.action_hide, ReaderCommand.DCMD_HIDE, 0, R.id.cr3_mi_hide, EXIT, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_hide);

	public final static ReaderAction SAVE_SETTINGS_TO_CLOUD = new ReaderAction("SAVE_SETTINGS_TO_CLOUD", R.string.save_settings_to_cloud_short, R.string.save_settings_to_cloud, ReaderCommand.DCMD_SAVE_SETTINGS_TO_CLOUD, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_settings_to_gd);
	public final static ReaderAction LOAD_SETTINGS_FROM_CLOUD = new ReaderAction("LOAD_SETTINGS_FROM_CLOUD", R.string.load_settings_from_cloud_short, R.string.load_settings_from_cloud, ReaderCommand.DCMD_LOAD_SETTINGS_FROM_CLOUD, 0, 0, SAVE_SETTINGS_TO_CLOUD, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_settings_from_gd);
	public final static ReaderAction SAVE_READING_POS = new ReaderAction("SAVE_READING_POS", 0, R.string.save_reading_pos_to_cloud, ReaderCommand.DCMD_SAVE_READING_POS, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_position_to_gd);
	public final static ReaderAction LOAD_READING_POS = new ReaderAction("LOAD_READING_POS", R.string.load_reading_pos_from_cloud_short, R.string.load_reading_pos_from_cloud, ReaderCommand.DCMD_LOAD_READING_POS, 0, 0, SAVE_READING_POS, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_position_from_gd);
	public final static ReaderAction SAVE_BOOKMARKS = new ReaderAction("SAVE_BOOKMARKS", R.string.save_bookmarks_to_cloud_short, R.string.save_bookmarks_to_cloud, ReaderCommand.DCMD_SAVE_BOOKMARKS, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_bookmarks_to_gd);
	public final static ReaderAction LOAD_BOOKMARKS = new ReaderAction("LOAD_BOOKMARKS", R.string.load_bookmarks_from_cloud_short, R.string.load_bookmarks_from_cloud, ReaderCommand.DCMD_LOAD_BOOKMARKS, 0, 0, SAVE_BOOKMARKS, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_bookmarks_from_gd);
    public final static ReaderAction CLOUD_MENU = new ReaderAction("CLOUD_MENU", R.string.cloud_menu_short, R.string.cloud_menu, ReaderCommand.DCMD_CLOUD_MENU, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_cloud_storage);
    public final static ReaderAction SAVE_CURRENT_BOOK_TO_CLOUD_YND = new ReaderAction("SAVE_CURRENT_BOOK_TO_CLOUD_YND", R.string.save_current_book_to_cloud_ynd_short, R.string.save_current_book_to_cloud_ynd, ReaderCommand.DCMD_SAVE_CURRENT_BOOK_TO_CLOUD_YND, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_book_to_gd);
	public final static ReaderAction SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL = new ReaderAction("SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL", R.string.save_current_book_to_cloud_email_short, R.string.save_current_book_to_cloud_email, ReaderCommand.DCMD_SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL, 0, 0, SAVE_CURRENT_BOOK_TO_CLOUD_YND, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_send_by_email);
	//public final static ReaderAction OPEN_BOOK_FROM_CLOUD = new ReaderAction("OPEN_BOOK_FROM_GD", R.string.open_book_from_gd, ReaderCommand.DCMD_OPEN_BOOK_FROM_CLOUD, 0, SAVE_CURRENT_BOOK_TO_CLOUD, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_book_from_gd);
	public final static ReaderAction OPEN_BOOK_FROM_CLOUD_YND = new ReaderAction("OPEN_BOOK_FROM_YND", R.string.open_book_from_ynd_short2, R.string.open_book_from_ynd, ReaderCommand.DCMD_OPEN_BOOK_FROM_CLOUD_YND, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_yandex_logo);
	public final static ReaderAction OPEN_BOOK_FROM_CLOUD_DBX = new ReaderAction("OPEN_BOOK_FROM_DBX", R.string.open_book_from_dbx_short2, R.string.open_book_from_dbx, ReaderCommand.DCMD_OPEN_BOOK_FROM_CLOUD_DBX, 0, 0, OPEN_BOOK_FROM_CLOUD_YND, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_dropbox_filled);
	public final static ReaderAction FONTS_MENU = new ReaderAction("FONTS_MENU", 0, R.string.fonts_menu, ReaderCommand.DCMD_FONTS_MENU, 0, 0, CLOUD_MENU, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_type_filled_2);
	public final static ReaderAction SAVE_BOOKMARK_LAST_SEL = new ReaderAction("SAVE_BOOKMARK_LAST_SEL", R.string.save_bookmark_last_sel_short, R.string.save_bookmark_last_sel, ReaderCommand.DCMD_SAVE_BOOKMARK_LAST_SEL, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_bookmark_plus);
	public final static ReaderAction SAVE_BOOKMARK_LAST_SEL_USER_DIC = new ReaderAction("SAVE_BOOKMARK_LAST_SEL_USER_DIC", R.string.save_bookmark_last_sel_user_dic_short, R.string.save_bookmark_last_sel_user_dic, ReaderCommand.DCMD_SAVE_BOOKMARK_LAST_SEL_USER_DIC, 0, 0, SAVE_BOOKMARK_LAST_SEL, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_google_translate_save);
	public final static ReaderAction SAVE_BOOKMARK_QUICK = new ReaderAction("SAVE_BOOKMARK_QUICK", R.string.save_bookmark_quick_short, R.string.save_bookmark_quick, ReaderCommand.DCMD_SAVE_BOOKMARK_QUICK, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_bookmark_plus_q);
	public final static ReaderAction SHOW_USER_DIC = new ReaderAction("SHOW_USER_DIC", R.string.win_title_user_dic_short, R.string.win_title_user_dic, ReaderCommand.DCMD_SHOW_USER_DIC, 0, 0, SHOW_DICTIONARY, R.string.options_app_show_user_dic_panel_add_info).setIconId(R.drawable.icons8_google_translate_user);
	public final static ReaderAction SHOW_CITATIONS = new ReaderAction("SHOW_CITATIONS", 0, R.string.win_title_citations, ReaderCommand.DCMD_SHOW_CITATIONS, 0, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_quote_2);
	public final static ReaderAction TOGGLE_PAGE_VIEW_MODE = new ReaderAction("TOGGLE_PAGE_VIEW_MODE", 0, R.string.options_view_mode,
			ReaderCommand.DCMD_TOGGLE_PAGE_VIEW_MODE, 0, 0, TOGGLE_AUTOSCROLL,
			R.string.options_view_mode_add_info).setIconId(R.drawable.cr3_option_view_mode_scroll);
	public final static ReaderAction CHOOSE_TEXTURE = new ReaderAction("CHOOSE_TEXTURE", R.string.options_choose_texture_short, R.string.options_choose_texture,
			ReaderCommand.DCMD_CHOOSE_TEXTURE, 0, 0, FONT_SELECT,
			R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_texture);

	public final static ReaderAction BACKLIGHT_SET_DEFAULT = new ReaderAction("BACKLIGHT_SET_DEFAULT", 0, R.string.action_backlight_set_default,
			ReaderCommand.DCMD_BACKLIGHT_SET_DEFAULT, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_sun_auto);

	public final static ReaderAction SHOW_SYSTEM_BACKLIGHT_DIALOG = new ReaderAction("SHOW_SYSTEM_BACKLIGHT_DIALOG", R.string.action_show_onyx_backlight_system_dialog_short, R.string.action_show_onyx_backlight_system_dialog,
			ReaderCommand.DCMD_SHOW_SYSTEM_BACKLIGHT_DIALOG, 0, 0, BACKLIGHT_SET_DEFAULT, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_sun);

	public final static ReaderAction SKIM = new ReaderAction("SKIM", R.string.skim_document_short, R.string.skim_document, ReaderCommand.DCMD_SKIM, 0, 0, SEARCH, R.string.skim_document_add_info).setIconId(R.drawable.icons8_skim);
	public final static ReaderAction ONLINE_COMBO = new ReaderAction("ONLINE_COMBO", 0, R.string.online_combo, ReaderCommand.DCMD_ONLINE_COMBO, 0, 0, null, R.string.online_combo_add_info).setIconId(R.drawable.icons8_combo);
	public final static ReaderAction ONLINE_SUPER_COMBO = new ReaderAction("ONLINE_SUPER_COMBO", 0, R.string.online_super_combo, ReaderCommand.DCMD_ONLINE_SUPER_COMBO, 0, 0, ONLINE_COMBO, R.string.online_super_combo_add_info).setIconId(R.drawable.icons8_super_combo);

	public final static ReaderAction SPEAK_SELECTION = new ReaderAction("SPEAK_SELECTION", 0, R.string.speak_selection, ReaderCommand.DCMD_SPEAK_SELECTION, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_button_tts);

	public final static ReaderAction GDRIVE_SYNCTO = new ReaderAction("GDRIVE_SYNCTO", 0, R.string.googledrive_sync_to, ReaderCommand.DCMD_GOOGLEDRIVE_SYNC, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.google_drive);
	public final static ReaderAction GDRIVE_SYNCFROM = new ReaderAction("GDRIVE_SYNCFROM", 0, R.string.googledrive_sync_from, ReaderCommand.DCMD_GOOGLEDRIVE_SYNC, 1, 0, GDRIVE_SYNCTO, R.string.option_add_info_empty_text).setIconId(R.drawable.google_drive);

	public final static ReaderAction ADD_OPDS_CATALOG = new ReaderAction("ADD_OPDS_CATALOG", R.string.add_opds_catalog_short, R.string.add_opds_catalog, ReaderCommand.DCMD_ADD_OPDS_CATALOG, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_opds);
	public final static ReaderAction ADD_REMOVE_LITRES_CATALOG = new ReaderAction("ADD_REMOVE_LITRES_CATALOG", R.string.add_remove_litres_catalog_short, R.string.add_remove_litres_catalog, ReaderCommand.DCMD_ADD_REMOVE_LITRES_CATALOG, 1, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_litres_en_logo_2lines_big);
	public final static ReaderAction ADD_CALIBRE_CATALOG_LOCAL = new ReaderAction("ADD_CALIBRE_CATALOG_LOCAL", R.string.add_calibre_catalog_local_short, R.string.add_calibre_catalog_local, ReaderCommand.DCMD_ADD_CALIBRE_CATALOG_LOCAL, 1, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_calibre);
	public final static ReaderAction ADD_CALIBRE_CATALOG_YD = new ReaderAction("ADD_CALIBRE_CATALOG_YD", R.string.add_calibre_catalog_yd_short, R.string.add_calibre_catalog_yd, ReaderCommand.DCMD_ADD_CALIBRE_CATALOG_YD, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_calibre);

	public final static ReaderAction CALIBRE_SEARCH = new ReaderAction("CALIBRE_SEARCH", 0, R.string.calibre_search, ReaderCommand.DCMD_CALIBRE_SEARCH, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_browser_find);
	public final static ReaderAction CALIBRE_SHOW_AUTHORS = new ReaderAction("CALIBRE_SHOW_AUTHORS", 0, R.string.calibre_authors, ReaderCommand.DCMD_CALIBRE_SHOW_AUTHORS, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_folder_author);
	public final static ReaderAction CALIBRE_SHOW_TITLES = new ReaderAction("CALIBRE_SHOW_TITLES", 0, R.string.calibre_titles, ReaderCommand.DCMD_CALIBRE_SHOW_TITLES, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.cr3_browser_folder_authors);
	public final static ReaderAction CALIBRE_SHOW_SERIES = new ReaderAction("CALIBRE_SHOW_SERIES", 0, R.string.calibre_series, ReaderCommand.DCMD_CALIBRE_SHOW_SERIES, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_folder_hash);
	public final static ReaderAction CALIBRE_SHOW_RATING = new ReaderAction("CALIBRE_SHOW_RATING", 0, R.string.calibre_rating, ReaderCommand.DCMD_CALIBRE_SHOW_RATING, 1, 0,null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_folder_stars);
	public final static ReaderAction CALIBRE_SHOW_PUB_DATES = new ReaderAction("CALIBRE_SHOW_PUB_DATES", 0, R.string.calibre_publish_date, ReaderCommand.DCMD_CALIBRE_SHOW_PUB_DATES, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_folder_year);
	public final static ReaderAction CALIBRE_SHOW_TAGS = new ReaderAction("CALIBRE_SHOW_TAGS", 0, R.string.calibre_tags, ReaderCommand.DCMD_CALIBRE_SHOW_TAGS, 1, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_theatre_mask);
	public final static ReaderAction INIT_APP_DIALOG = new ReaderAction("INIT_APP_DIALOG", 0, R.string.init_app, ReaderCommand.DCMD_INIT_APP_DIALOG, 0, 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_delete_database);
	public final static ReaderAction EXPERIMENAL_FEATURE = new ReaderAction("EXPERIMENAL_FEATURE", R.string.exp_feature_short, R.string.exp_feature, ReaderCommand.DCMD_EXPERIMENTAL_FEATURE, 0, 0,null, R.string.exp_feature_add_info).setIconId(R.drawable.icons8_physics);
	public final static ReaderAction BRIGHTNESS_DOWN = new ReaderAction("BRIGHTNESS_DOWN", 0, R.string.action_brightness_down, ReaderCommand.DCMD_BRIGHTNESS_DOWN, 1 , 0, null, R.string.option_add_info_empty_text).setCanRepeat().setIconId(R.drawable.icons8_brightness_down);
	public final static ReaderAction BRIGHTNESS_DOWN_WARM = new ReaderAction("BRIGHTNESS_DOWN_WARM", 0, R.string.action_brightness_warm_down, ReaderCommand.DCMD_BRIGHTNESS_WARM_DOWN, 1 ,0, null, R.string.option_add_info_empty_text).setCanRepeat().setIconId(R.drawable.icons8_brightness_warm_down);
	public final static ReaderAction BRIGHTNESS_UP = new ReaderAction("BRIGHTNESS_UP", 0, R.string.action_brightness_up, ReaderCommand.DCMD_BRIGHTNESS_UP, 1 , 0, BRIGHTNESS_DOWN, R.string.option_add_info_empty_text).setCanRepeat().setIconId(R.drawable.icons8_brightness_up);
	public final static ReaderAction BRIGHTNESS_UP_WARM = new ReaderAction("BRIGHTNESS_UP_WARM", 0, R.string.action_brightness_warm_up, ReaderCommand.DCMD_BRIGHTNESS_WARM_UP, 1 , 0, BRIGHTNESS_DOWN_WARM, R.string.option_add_info_empty_text).setCanRepeat().setIconId(R.drawable.icons8_brightness_warm_up);
	public final static ReaderAction EINK_ONYX_BACK = new ReaderAction("EINK_ONYX_BACK", 0, R.string.action_eink_onyx_back, ReaderCommand.DCMD_EINK_ONYX_BACK, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_triangle);
	public final static ReaderAction EINK_ONYX_HOME = new ReaderAction("EINK_ONYX_HOME", 0, R.string.action_eink_onyx_home, ReaderCommand.DCMD_EINK_ONYX_HOME, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_circle);
	public final static ReaderAction EINK_ONYX_RECENT = new ReaderAction("EINK_ONYX_RECENT", 0, R.string.action_eink_onyx_recent, ReaderCommand.DCMD_EINK_ONYX_RECENT, 1 , 0, EINK_ONYX_HOME, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_square);
	public final static ReaderAction EINK_ONYX_REPAINT_SCREEN = new ReaderAction("EINK_ONYX_REPAINT_SCREEN", 0, R.string.action_eink_onyx_repaint_screen, ReaderCommand.DCMD_EINK_ONYX_REPAINT_SCREEN, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_refresh_screen);
	public final static ReaderAction EINK_ONYX_SCREENSHOT = new ReaderAction("EINK_ONYX_SCREENSHOT", 0, R.string.action_eink_onyx_screenshot, ReaderCommand.DCMD_EINK_ONYX_SCREENSHOT, 1 , 0, EINK_ONYX_REPAINT_SCREEN, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_screenshot);
	public final static ReaderAction ADD_BOOK_TAGS = new ReaderAction("ADD_BOOK_TAGS", 0, R.string.add_book_tags, ReaderCommand.DCMD_ADD_BOOK_TAGS, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_tag);
	public final static ReaderAction COMMAND_GROUP_1 = new ReaderAction("COMMAND_GROUP_1", 0, R.string.command_group_1, ReaderCommand.DCMD_COMMAND_GROUP_1, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_1);
	public final static ReaderAction COMMAND_GROUP_2 = new ReaderAction("COMMAND_GROUP_2", 0, R.string.command_group_2, ReaderCommand.DCMD_COMMAND_GROUP_2, 1 , 0, COMMAND_GROUP_1, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_2);
	public final static ReaderAction COMMAND_GROUP_3 = new ReaderAction("COMMAND_GROUP_3", 0, R.string.command_group_3, ReaderCommand.DCMD_COMMAND_GROUP_3, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_3);
	public final static ReaderAction COMMAND_GROUP_4 = new ReaderAction("COMMAND_GROUP_4", 0, R.string.command_group_4, ReaderCommand.DCMD_COMMAND_GROUP_4, 1 , 0, COMMAND_GROUP_3, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_4);
	public final static ReaderAction COMMAND_GROUP_5 = new ReaderAction("COMMAND_GROUP_5", 0, R.string.command_group_5, ReaderCommand.DCMD_COMMAND_GROUP_5, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_5);
	public final static ReaderAction COMMAND_GROUP_6 = new ReaderAction("COMMAND_GROUP_6", 0, R.string.command_group_6, ReaderCommand.DCMD_COMMAND_GROUP_6, 1 , 0, COMMAND_GROUP_5, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_6);
	public final static ReaderAction COMMAND_GROUP_7 = new ReaderAction("COMMAND_GROUP_7", 0, R.string.command_group_7, ReaderCommand.DCMD_COMMAND_GROUP_7, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_7);
	public final static ReaderAction COMMAND_GROUP_8 = new ReaderAction("COMMAND_GROUP_8", 0, R.string.command_group_8, ReaderCommand.DCMD_COMMAND_GROUP_8, 1 , 0, COMMAND_GROUP_7, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_8);
	public final static ReaderAction COMMAND_GROUP_9 = new ReaderAction("COMMAND_GROUP_9", 0, R.string.command_group_9, ReaderCommand.DCMD_COMMAND_GROUP_9, 1 , 0, null, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_9);
	public final static ReaderAction COMMAND_GROUP_10 = new ReaderAction("COMMAND_GROUP_10", 0, R.string.command_group_10, ReaderCommand.DCMD_COMMAND_GROUP_10, 1 , 0, COMMAND_GROUP_9, R.string.option_add_info_empty_text).setIconId(R.drawable.icons8_10);

	private final static ReaderAction[] AVAILABLE_ACTIONS;
	public final static HashMap<String, ReaderAction> OPTIONS_ACTIONS = new HashMap<>();
	public static List<ReaderAction> getAvailActions(boolean withOptions) {
		List<ReaderAction> lra = new ArrayList<>();
		for (ReaderAction ra: AVAILABLE_ACTIONS) lra.add(ra);
		if (withOptions) {
			ArrayList<ReaderAction> oal = new ArrayList<>();
			for (Map.Entry<String, ReaderAction> entry : OPTIONS_ACTIONS.entrySet()) oal.add(entry.getValue());
			Collections.sort(oal, (lhs, rhs) -> {
				// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
				return lhs.actionOption.label.compareToIgnoreCase(rhs.actionOption.label);
			});
			for (ReaderAction ra: oal) lra.add(ra);
		}
		return lra;
	}

	public boolean isNone() {
		return cmd == NONE.cmd;
	}
	
	public boolean isRepeat() {
		return cmd == REPEAT.cmd;
	}
	
	public static ReaderAction findById(String id) {
		if (id == null)
			return NONE;
		for (ReaderAction a : getAvailActions(true)) {
			if (id.equals(a.id))
				return a;
		}
		if (id.equals(REPEAT.id))
			return REPEAT;
		return NONE;
	}
	public static ReaderAction findByMenuId( int id ) {
		if (id == 0)
			return NONE;
		for (ReaderAction a : getAvailActions(true)) {
			if (id == a.menuItemId)
				return a;
		}
		return NONE;
	}
	public final static String NORMAL_PROP = ".";
	public final static String LONG_PROP = ".long.";
	public final static String DOUBLECLICK_PROP = ".dbl.";
	
	public final static int NORMAL = 0;
	public final static int LONG = 1;
	public final static int DOUBLE = 2;
	public final static String[] TYPE_PROP_SUBPATH = new String[] {NORMAL_PROP, LONG_PROP, DOUBLECLICK_PROP};

	public static String getTypeString(int type) {
		return TYPE_PROP_SUBPATH[type];
	}
	
	public static String getTapZoneProp(int tapZoneNumber, int type) {
		return ReaderView.PROP_APP_TAP_ZONE_ACTIONS_TAP + getTypeString(type) + tapZoneNumber;
	}
	public static String getKeyProp(int keyCode, int type) {
		return ReaderView.PROP_APP_KEY_ACTIONS_PRESS + getTypeString(type) + keyCode;
	}
	public static ReaderAction findForTap(int tapZoneNumber, Properties settings) {
		String id = settings.getProperty(getTapZoneProp(tapZoneNumber, NORMAL));
		return findById(id);
	}
	public static ReaderAction findForLongTap(int tapZoneNumber, Properties settings) {
		String id = settings.getProperty(getTapZoneProp(tapZoneNumber, LONG));
		return findById(id);
	}
	public static ReaderAction findForDoubleTap(int tapZoneNumber, Properties settings) {
		String id = settings.getProperty(getTapZoneProp(tapZoneNumber, DOUBLE));
		return findById(id);
	}
	public static ReaderAction findForKey(int keyCode, int scanCode, Properties settings) {
		String id = settings.getProperty(getKeyProp(keyCode, NORMAL));
		ReaderAction ra = findById(id);
		if (ra.equals(NONE)) {
			id = settings.getProperty(getKeyProp(-scanCode, NORMAL));
			ra = findById(id);
		}
		return ra;
	}
	public static ReaderAction findForLongKey(int keyCode, int scanCode, Properties settings) {
		String id = settings.getProperty(getKeyProp(keyCode, LONG));
		ReaderAction ra = findById(id);
		if (ra.equals(NONE)) {
			id = settings.getProperty(getKeyProp(-scanCode, LONG));
			ra = findById(id);
		}
		return ra;
	}
	public static ReaderAction findForDoubleKey(int keyCode, int scanCode, Properties settings) {
		String id = settings.getProperty(getKeyProp(keyCode, DOUBLE));
		ReaderAction ra = findById(id);
		if (ra.equals(NONE)) {
			id = settings.getProperty(getKeyProp(-scanCode, DOUBLE));
			ra = findById(id);
		}
		return ra;
	}
	
	public static ArrayList<ReaderAction> createList(ReaderAction ... actions) {
		ArrayList<ReaderAction> list = new ArrayList<ReaderAction>(actions.length);
		for (ReaderAction item : actions)
			list.add(item);
		return list;
	}

	static {
		ReaderAction[] BASE_ACTIONS = new ReaderAction[]{
				NONE,
				PAGE_DOWN,
				PAGE_UP,
				PAGE_DOWN_10,
				PAGE_UP_10,
				FIRST_PAGE,
				LAST_PAGE,
				NEXT_CHAPTER,
				PREV_CHAPTER,
				TOC,
				GO_PAGE,
				//GO_PERCENT,
				BOOKMARKS,
				SEARCH,
				OPTIONS,
				EXIT,
				TOGGLE_DAY_NIGHT,
				RECENT_BOOKS,
				FILE_BROWSER,
				FILE_BROWSER_ROOT,
				CURRENT_BOOK_DIRECTORY,
				READER_MENU,
				TOGGLE_TOUCH_SCREEN_LOCK,
				TOGGLE_SELECTION_MODE,
				TOGGLE_INSPECTOR_MODE,
				TOGGLE_ORIENTATION,
				TOGGLE_FULLSCREEN,
				GO_BACK,
				GO_FORWARD,
				HOME_SCREEN,
				ZOOM_IN,
				ZOOM_OUT,
				FONT_PREVIOUS,
				FONT_NEXT,
				DOCUMENT_STYLES,
				ABOUT,
				BOOK_INFO,
				TTS_PLAY,
				TTS_STOP,
				TOGGLE_TITLEBAR,
				SHOW_POSITION_INFO_POPUP,
				SHOW_DICTIONARY,
				OPEN_PREVIOUS_BOOK,
				TOGGLE_AUTOSCROLL,
				SWITCH_PROFILE,
				TEXT_AUTOFORMAT,
				USER_MANUAL,
//		AUTOSCROLL_SPEED_INCREASE,
//		AUTOSCROLL_SPEED_DECREASE,
				TOGGLE_DICT_ONCE,
				TOGGLE_DICT,
				SAVE_SETTINGS_TO_CLOUD,
				LOAD_SETTINGS_FROM_CLOUD,
				SAVE_READING_POS,
				LOAD_READING_POS,
				SAVE_BOOKMARKS,
				LOAD_BOOKMARKS,
				CLOUD_MENU,
				SAVE_CURRENT_BOOK_TO_CLOUD_YND,
				SAVE_CURRENT_BOOK_TO_CLOUD_EMAIL,
				//OPEN_BOOK_FROM_CLOUD,
				OPEN_BOOK_FROM_CLOUD_YND,
				OPEN_BOOK_FROM_CLOUD_DBX,
				FONTS_MENU,
				SAVE_BOOKMARK_LAST_SEL,
				SAVE_BOOKMARK_LAST_SEL_USER_DIC,
				SAVE_BOOKMARK_QUICK,
				SHOW_USER_DIC,
				SHOW_CITATIONS,
				TOGGLE_PAGE_VIEW_MODE,
				OPTIONS_FILTER,
				FONT_SELECT,
				FONT_BOLD,
				WHOLE_PAGE_TO_DIC,
				CHOOSE_TEXTURE,
				HIDE,
				BACKLIGHT_SET_DEFAULT,
				SKIM,
				ONLINE_COMBO,
				ONLINE_SUPER_COMBO,
				SAVE_LOGCAT,
				EXPERIMENAL_FEATURE,
				BRIGHTNESS_DOWN,
				BRIGHTNESS_UP,
				ADD_BOOK_TAGS,
				COMMAND_GROUP_1,
				COMMAND_GROUP_2,
				COMMAND_GROUP_3,
				COMMAND_GROUP_4,
				COMMAND_GROUP_5,
				COMMAND_GROUP_6,
				COMMAND_GROUP_7,
				COMMAND_GROUP_8,
				COMMAND_GROUP_9,
				COMMAND_GROUP_10,
				SPEAK_SELECTION
				// calibre will only be available from rootview
		};
		if (BuildConfig.GSUITE_AVAILABLE && DeviceInfo.getSDKLevel() >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			int count = BASE_ACTIONS.length;
			ReaderAction[] new_array = new ReaderAction[count + 2];
			System.arraycopy(BASE_ACTIONS, 0, new_array, 0, count);
			new_array[count] = GDRIVE_SYNCTO;
			new_array[count + 1] = GDRIVE_SYNCFROM;
			BASE_ACTIONS = new_array;
		}
		if (DeviceInfo.EINK_HAVE_FRONTLIGHT) {
			// TODO: and may be other eink devices with frontlight...
			if (DeviceInfo.EINK_ONYX) {
				int count = BASE_ACTIONS.length;
				ReaderAction[] new_array = new ReaderAction[count + 1];
				System.arraycopy(BASE_ACTIONS, 0, new_array, 0, count);
				new_array[count] = SHOW_SYSTEM_BACKLIGHT_DIALOG;
				BASE_ACTIONS = new_array;
			}
		}
		if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT) {
			int count = BASE_ACTIONS.length;
			ReaderAction[] new_array = new ReaderAction[count + 2];
			System.arraycopy(BASE_ACTIONS, 0, new_array, 0, count);
			new_array[count] = BRIGHTNESS_DOWN_WARM;
			new_array[count+1] = BRIGHTNESS_UP_WARM;
			BASE_ACTIONS = new_array;
		}
		if (DeviceInfo.EINK_ONYX) {
			int count = BASE_ACTIONS.length;
			ReaderAction[] new_array = new ReaderAction[count + 5];
			System.arraycopy(BASE_ACTIONS, 0, new_array, 0, count);
			new_array[count] = EINK_ONYX_BACK;
			new_array[count+1] = EINK_ONYX_HOME;
			new_array[count+2] = EINK_ONYX_RECENT;
			new_array[count+3] = EINK_ONYX_REPAINT_SCREEN;
			new_array[count+4] = EINK_ONYX_SCREENSHOT;
			BASE_ACTIONS = new_array;
		}
		AVAILABLE_ACTIONS = BASE_ACTIONS;
	}

}