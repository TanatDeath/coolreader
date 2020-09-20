package org.coolreader.crengine;

import android.app.SearchManager;
import android.util.Log;

import org.coolreader.R;

import java.util.Locale;

public interface Settings {
    public static final String PROP_PAGE_BACKGROUND_IMAGE       ="background.image";
    public static final String PROP_PAGE_BACKGROUND_IMAGE_DAY   ="background.image.day";
    public static final String PROP_PAGE_BACKGROUND_IMAGE_NIGHT ="background.image.night";
    public static final String PROP_NIGHT_MODE              ="crengine.night.mode";
    public static final String PROP_FONT_COLOR_DAY          ="font.color.day";
    public static final String PROP_BACKGROUND_COLOR_DAY    ="background.color.day";
    public static final String PROP_FONT_COLOR_NIGHT        ="font.color.night";
    public static final String PROP_BACKGROUND_COLOR_NIGHT  ="background.color.night";
    public static final String PROP_FONT_COLOR              ="font.color.default";
    public static final String PROP_BACKGROUND_COLOR        ="background.color.default";
    public static final String PROP_FONT_ANTIALIASING       ="font.antialiasing.mode";
    public static final String PROP_FONT_FACE               ="font.face.default";
    public static final String PROP_FONT_HINTING            ="font.hinting.mode";
    public static final String PROP_FONT_GAMMA              ="font.gamma";
    public static final String PROP_FONT_GAMMA_DAY          ="font.gamma.day";
    public static final String PROP_FONT_GAMMA_NIGHT        ="font.gamma.night";
    public static final String PROP_FONT_WEIGHT_EMBOLDEN    ="font.face.weight.embolden";
    public static final String PROP_TXT_OPTION_PREFORMATTED ="crengine.file.txt.preformatted";
    public static final String PROP_LOG_FILENAME            ="crengine.log.filename";
    public static final String PROP_LOG_LEVEL               ="crengine.log.level";
    public static final String PROP_LOG_AUTOFLUSH           ="crengine.log.autoflush";
    public static final String PROP_FONT_SIZE               ="crengine.font.size";
    public static final String PROP_FALLBACK_FONT_FACE      ="crengine.font.fallback.face";
	public static final String PROP_FALLBACK_FONT_FACES     ="crengine.font.fallback.faces";
	public static final String PROP_STATUS_FONT_COLOR       ="crengine.page.header.font.color";
    public static final String PROP_STATUS_FONT_COLOR_DAY   ="crengine.page.header.font.color.day";
    public static final String PROP_STATUS_FONT_COLOR_NIGHT ="crengine.page.header.font.color.night";
    public static final String PROP_STATUS_FONT_FACE        ="crengine.page.header.font.face";
    public static final String PROP_STATUS_FONT_SIZE        ="crengine.page.header.font.size";
    public static final String PROP_STATUS_CHAPTER_MARKS    ="crengine.page.header.chapter.marks";
    public static final String PROP_PAGE_MARGIN_TOP         ="crengine.page.margin.top";
    public static final String PROP_PAGE_MARGIN_BOTTOM      ="crengine.page.margin.bottom";
    public static final String PROP_PAGE_MARGIN_LEFT        ="crengine.page.margin.left";
    public static final String PROP_PAGE_MARGIN_RIGHT       ="crengine.page.margin.right";
	public static final String PROP_GLOBAL_MARGIN       	="crengine.global.margin";
	public static final String PROP_ROUNDED_CORNERS_MARGIN  ="crengine.rounded.corners.margin";
	public static final String PROP_ROUNDED_CORNERS_MARGIN_POS = "crengine.rounded.corners.margin.pos";
	public static final String PROP_ROUNDED_CORNERS_MARGIN_MOD = "crengine.rounded.corners.margin.mod";
	public static final String PROP_ROUNDED_CORNERS_MARGIN_FSCR = "crengine.rounded.corners.margin.fscr";
	public static final String PROP_EXT_FULLSCREEN_MARGIN   ="crengine.ext.fullscreen.margin";
	public static final String PROP_EXT_FULLSCREEN_MOD   ="crengine.ext.fullscreen.mod";
	public static final String PROP_PAGE_VIEW_MODE          ="crengine.page.view.mode"; // pages/scroll
    public static final String PROP_PAGE_VIEW_MODE_AUTOCHANGED = "crengine.page.view.mode.autochanged"; // when tts
    public static final String PROP_PAGE_ANIMATION          ="crengine.page.animation";
	public static final String PROP_PAGE_ANIMATION_SPEED    ="crengine.page.animation.speed";
	public static final String PROP_INTERLINE_SPACE         ="crengine.interline.space";
    public static final String PROP_ROTATE_ANGLE            ="window.rotate.angle";
    public static final String PROP_EMBEDDED_STYLES         ="crengine.doc.embedded.styles.enabled";
    public static final String PROP_EMBEDDED_FONTS          ="crengine.doc.embedded.fonts.enabled";
    public static final String PROP_DISPLAY_INVERSE         ="crengine.display.inverse";
//    public static final String PROP_DISPLAY_FULL_UPDATE_INTERVAL ="crengine.display.full.update.interval";
//    public static final String PROP_DISPLAY_TURBO_UPDATE_MODE ="crengine.display.turbo.update";

    public static final String PROP_STATUS_LOCATION         ="viewer.status.location";
    public static final String PROP_TOOLBAR_LOCATION        ="viewer.toolbar.location2";
    public static final String PROP_TOOLBAR_HIDE_IN_FULLSCREEN="viewer.toolbar.fullscreen.hide";
    public static final String PROP_TOOLBAR_APPEARANCE="viewer.toolbar.appearance";
    public static final String PROP_TOOLBAR_BUTTONS     ="viewer.toolbar.buttons";
    public static final String PROP_SKIPPED_RES     ="viewer.skipped.resolutions";

    public static final String PROP_STATUS_LINE             ="window.status.line";
    public static final String PROP_BOOKMARK_ICONS          ="crengine.bookmarks.icons";
    public static final String PROP_FOOTNOTES               ="crengine.footnotes";
    public static final String PROP_SHOW_TIME               ="window.status.clock";
    public static final String PROP_SHOW_TITLE              ="window.status.title";
    public static final String PROP_SHOW_BATTERY            ="window.status.battery";
    public static final String PROP_SHOW_BATTERY_PERCENT    ="window.status.battery.percent";
    public static final String PROP_SHOW_POS_PERCENT        ="window.status.pos.percent";
    public static final String PROP_SHOW_PAGE_COUNT         ="window.status.pos.page.count";
    public static final String PROP_SHOW_PAGE_NUMBER        ="window.status.pos.page.number";
	public static final String PROP_SHOW_PAGES_TO_CHAPTER   ="window.status.pos.pages.to.chapter";
	public static final String PROP_SHOW_TIME_LEFT   		="window.status.pos.time.left";
	public static final String PROP_FONT_SHAPING            ="font.shaping.mode";
    public static final String PROP_FONT_KERNING_ENABLED    ="font.kerning.enabled";
    public static final String PROP_FLOATING_PUNCTUATION    ="crengine.style.floating.punctuation.enabled";
    public static final String PROP_LANDSCAPE_PAGES         ="window.landscape.pages";
    //public static final String PROP_HYPHENATION_DICT        ="crengine.hyphenation.dictionary.code"; // non-crengine (old)
	public static final String PROP_HYPHENATION_DICT        = "crengine.hyphenation.directory";

	public static final String PROP_AUTOSAVE_BOOKMARKS      ="crengine.autosave.bookmarks";
	// New textlang typography settings:
	public static final String PROP_TEXTLANG_MAIN_LANG      = "crengine.textlang.main.lang";
	public static final String PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED = "crengine.textlang.embedded.langs.enabled";
	public static final String PROP_TEXTLANG_HYPHENATION_ENABLED    = "crengine.textlang.hyphenation.enabled";
	public static final String PROP_TEXTLANG_HYPH_SOFT_HYPHENS_ONLY = "crengine.textlang.hyphenation.soft.hyphens.only";
	public static final String PROP_TEXTLANG_HYPH_FORCE_ALGORITHMIC = "crengine.textlang.hyphenation.force.algorithmic";

    public static final String PROP_PROFILE_NUMBER          ="crengine.profile.number"; // current settings profile number
	public static final String PROP_PROFILE_NAME            ="crengine.profile.name";
	public static final String PROP_APP_SETTINGS_SHOW_ICONS ="app.settings.show.icons";
    public static final String PROP_APP_ICONS_IS_CUSTOM_COLOR ="app.settings.show.icons.is.custom.color";
    public static final String PROP_APP_ICONS_CUSTOM_COLOR ="app.settings.show.icons.custom.color";
    public static final String PROP_APP_KEY_BACKLIGHT_OFF   ="app.key.backlight.disabled";

	 // image scaling settings
	 // mode: 0=disabled, 1=integer scaling factors, 2=free scaling
	 // scale: 0=auto based on font size, 1=no zoom, 2=scale up to *2, 3=scale up to *3
    public static final String PROP_IMG_SCALING_ZOOMIN_INLINE_MODE = "crengine.image.scaling.zoomin.inline.mode";
    public static final String PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE = "crengine.image.scaling.zoomin.inline.scale";
    public static final String PROP_IMG_SCALING_ZOOMOUT_INLINE_MODE = "crengine.image.scaling.zoomout.inline.mode";
    public static final String PROP_IMG_SCALING_ZOOMOUT_INLINE_SCALE = "crengine.image.scaling.zoomout.inline.scale";
    public static final String PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE = "crengine.image.scaling.zoomin.block.mode";
    public static final String PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE = "crengine.image.scaling.zoomin.block.scale";
    public static final String PROP_IMG_SCALING_ZOOMOUT_BLOCK_MODE = "crengine.image.scaling.zoomout.block.mode";
    public static final String PROP_IMG_SCALING_ZOOMOUT_BLOCK_SCALE = "crengine.image.scaling.zoomout.block.scale";
    
    public static final String PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT = "crengine.style.space.condensing.percent";
	public static final String PROP_FORMAT_UNUSED_SPACE_THRESHOLD_PERCENT = "crengine.style.unused.space.threshold.percent";
	public static final String PROP_FORMAT_MAX_ADDED_LETTER_SPACING_PERCENT = "crengine.style.max.added.letter.spacing.percent";
	public static final String PROP_FORMAT_SPACE_WIDTH_SCALE_PERCENT = "crengine.style.space.width.scale.percent";

	// default is 96 (1 css px = 1 screen px)
	// use 0 for old crengine behaviour (no support for absolute units and 1css px = 1 screen px)
	public static final String PROP_RENDER_DPI              = "crengine.render.dpi";
	//public static final String PROP_RENDER_SCALE_FONT_WITH_DPI = "crengine.render.scale.font.with.dpi";
	public static final String PROP_RENDER_BLOCK_RENDERING_FLAGS = "crengine.render.block.rendering.flags";
	public static final String PROP_REQUESTED_DOM_VERSION   = "crengine.render.requested_dom_version";

	public static final String PROP_MIN_FILE_SIZE_TO_CACHE  ="crengine.cache.filesize.min";
    public static final String PROP_FORCED_MIN_FILE_SIZE_TO_CACHE  ="crengine.cache.forced.filesize.min";
    public static final String PROP_PROGRESS_SHOW_FIRST_PAGE="crengine.progress.show.first.page";

    public static final String PROP_CONTROLS_ENABLE_VOLUME_KEYS ="app.controls.volume.keys.enabled";
    
    public static final String PROP_APP_FULLSCREEN          ="app.fullscreen";
	public static final String PROP_APP_TITLEBAR_NEW = "app.titlebar.new";
    public static final String PROP_APP_BOOK_PROPERTY_SCAN_ENABLED ="app.browser.fileprops.scan.enabled";
    public static final String PROP_APP_SHOW_COVERPAGES     ="app.browser.coverpages";
    public static final String PROP_APP_COVERPAGE_SIZE     ="app.browser.coverpage.size"; // 0==small, 2==BIG
    public static final String PROP_APP_SCREEN_ORIENTATION  ="app.screen.orientation";
    public static final String PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION  ="app.screen.orientation.popup.duration";
    public static final String PROP_APP_SCREEN_BACKLIGHT    ="app.screen.backlight";
    public static final String PROP_APP_MOTION_TIMEOUT    ="app.motion.timeout";
    public static final String PROP_APP_SCREEN_BACKLIGHT_DAY   ="app.screen.backlight.day";
    public static final String PROP_APP_SCREEN_BACKLIGHT_NIGHT ="app.screen.backlight.night";
    public static final String PROP_APP_DOUBLE_TAP_SELECTION     ="app.controls.doubletap.selection";
    public static final String PROP_APP_TAP_ZONE_ACTIONS_TAP     ="app.tapzone.action.tap";
    public static final String PROP_APP_KEY_ACTIONS_PRESS     ="app.key.action.press";
    public static final String PROP_APP_TRACKBALL_DISABLED    ="app.trackball.disabled";
    public static final String PROP_APP_SCREEN_BACKLIGHT_LOCK    ="app.screen.backlight.lock.enabled";
    public static final String PROP_APP_TAP_ZONE_HILIGHT     ="app.tapzone.hilight";
    public static final String PROP_APP_FLICK_BACKLIGHT_CONTROL = "app.screen.backlight.control.flick";
    public static final String PROP_APP_BOOK_SORT_ORDER = "app.browser.sort.order";
    public static final String PROP_APP_DICTIONARY = "app.dictionary.current";
    public static final String PROP_APP_DICTIONARY_2 = "app.dictionary2.current";
	public static final String PROP_APP_DICTIONARY_3 = "app.dictionary3.current";
	public static final String PROP_APP_DICTIONARY_4 = "app.dictionary4.current";
	public static final String PROP_APP_DICTIONARY_5 = "app.dictionary5.current";
	public static final String PROP_APP_DICTIONARY_6 = "app.dictionary6.current";
	public static final String PROP_APP_DICTIONARY_7 = "app.dictionary7.current";
    public static final String PROP_APP_DICT_WORD_CORRECTION = "app.dictionary.word.correction";
    public static final String PROP_APP_SHOW_USER_DIC_PANEL = "app.dictionary.show.user.dic.panel";
    public static final String PROP_APP_DICT_LONGTAP_CHANGE = "app.dictionary.longtap.change";
    public static final String PROP_APP_SELECTION_ACTION = "app.selection.action";
    public static final String PROP_APP_SELECTION_ACTION_LONG = "app.selection.action.long";
    public static final String PROP_APP_MULTI_SELECTION_ACTION = "app.multiselection.action";
	public static final String PROP_APP_SELECTION2_ACTION = "app.selection2.action";
	public static final String PROP_APP_SELECTION2_ACTION_LONG = "app.selection2.action.long";
	public static final String PROP_APP_MULTI_SELECTION2_ACTION = "app.multiselection2.action";
	public static final String PROP_APP_SELECTION3_ACTION = "app.selection3.action";
	public static final String PROP_APP_SELECTION3_ACTION_LONG = "app.selection3.action.long";
	public static final String PROP_APP_MULTI_SELECTION3_ACTION = "app.multiselection3.action";
	public static final String PROP_APP_SELECTION_PERSIST = "app.selection.persist";
	public static final String PROP_CLOUD_SYNC_VARIANT = "app.cloud.sync.variant";
	public static final String PROP_CLOUD_YND_HOME_FOLDER = "app.cloud.ynd.home.folder";
	public static final String PROP_CLOUD_YND_SETTINGS = "app.cloud.ynd.settings";
	public static final String PROP_CLOUD_DBX_SETTINGS = "app.cloud.dbx.settings";
	public static final String PROP_CLOUD_WIKI1_ADDR = "app.cloud.wiki1";
	public static final String PROP_CLOUD_WIKI2_ADDR = "app.cloud.wiki2";
	public static final String PROP_CLOUD_WIKI_SAVE_HISTORY = "app.cloud.wiki.save.history";
	public static final String PROP_CLOUD_YND_TRANSLATE_OPTIONS = "app.cloud.ynd.translate.options";
	public static final String PROP_CLOUD_LITRES_SETTINGS = "app.cloud.litres.settings";
	public static final String PROP_SAVE_POS_TO_CLOUD_TIMEOUT = "app.autosave.reading.pos.timeout";
    public static final String PROP_SAVE_POS_TIMEOUT = "app.autosave.reading.pos.timeout.1";
    public static final String PROP_SAVE_POS_SPEAK_TIMEOUT = "app.autosave.reading.pos.timeout.2";
    public static final String PROP_APP_MARK_DOWNLOADED_TO_READ = "app.mark.downloaded.toread";
    public static final String PROP_APP_TTS_FORCE_KOEF = "app.tts.force.koef";
	public static final String PROP_APP_CLOUD_POS_DATE_SORT = "app.cloudpos.date.sort";
	public static final String PROP_APP_CLOUD_POS_HIDE_CURRENT_DEV = "app.cloudpos.hide.current.dev";
	public static final String PROP_APP_ROOT_VIEW_FS_SECTION_HIDE = "app.rootview.fs_section.hide";
	public static final String PROP_APP_ROOT_VIEW_LIB_SECTION_HIDE = "app.rootview.lib_section.hide";
	public static final String PROP_APP_ROOT_VIEW_OPDS_SECTION_HIDE = "app.rootview.opds_section.hide";

	public static final String PROP_APP_HIDE_STATE_DIALOGS = "app.hide.state.dialogs";
	public static final String PROP_APP_HIDE_CSS_WARNING = "app.hide.state.warning";

	public static final String PROP_APP_HIGHLIGHT_BOOKMARKS = "crengine.highlight.bookmarks";
    public static final String PROP_HIGHLIGHT_SELECTION_COLOR = "crengine.highlight.selection.color";
    public static final String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT = "crengine.highlight.bookmarks.color.comment";
    public static final String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION = "crengine.highlight.bookmarks.color.correction";
    public static final String PROP_APP_HIGHLIGHT_BOOKMARKS_DAY = "crengine.highlight.bookmarks.day";
    public static final String PROP_HIGHLIGHT_SELECTION_COLOR_DAY = "crengine.highlight.selection.color.day";
    public static final String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_DAY = "crengine.highlight.bookmarks.color.comment.day";
    public static final String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_DAY = "crengine.highlight.bookmarks.color.correction.day";
    public static final String PROP_APP_HIGHLIGHT_BOOKMARKS_NIGHT = "crengine.highlight.bookmarks.night";
    public static final String PROP_HIGHLIGHT_SELECTION_COLOR_NIGHT = "crengine.highlight.selection.color.night";
    public static final String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_NIGHT = "crengine.highlight.bookmarks.color.comment.night";
    public static final String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_NIGHT = "crengine.highlight.bookmarks.color.correction.night";

    public static final String PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS = "app.browser.hide.empty.folders";
    public static final String PROP_APP_FILE_BROWSER_SIMPLE_MODE = "app.browser.simple.mode";
    public static final String PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE = "app.browser.max.group.size";
	public static final String PROP_APP_FILE_BROWSER_TAP_ACTION = "app.browser.tap.action";
	public static final String PROP_APP_FILE_BROWSER_LONGTAP_ACTION = "app.browser.longtap.action";
	public static final String PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON = "app.browser.sec.group.common";
	public static final String PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR = "app.browser.sec.group.author";
	public static final String PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES = "app.browser.sec.group.series";
	public static final String PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES = "app.browser.sec.group.genres";
	public static final String PROP_APP_FILE_BROWSER_SEC_GROUP_SEARCH = "app.browser.sec.group.search";
	public static final String PROP_APP_FILE_BROWSER_SEC_GROUP_DATES = "app.browser.sec.group.dates";
	public static final String PROP_APP_FILE_BROWSER_SEC_GROUP_RATING = "app.browser.sec.group.rating";
	public static final String PROP_APP_FILE_BROWSER_SEC_GROUP_STATE = "app.browser.sec.group.state";
	public static final String PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_LOAD = "app.browser.authors.aliases.load";
	public static final String PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_ENABLED = "app.browser.authors.aliases.enabled";

    public static final String PROP_APP_SCREEN_UPDATE_MODE = "app.screen.update.mode";
    public static final String PROP_APP_SCREEN_UPDATE_INTERVAL = "app.screen.update.interval";
    public static final String PROP_APP_SCREEN_BLACKPAGE_INTERVAL = "app.screen.blackpage.interval";
    public static final String PROP_APP_SCREEN_BLACKPAGE_DURATION = "app.screen.blackpage.duration";
    public static final String PROP_APP_SCREEN_FORCE_EINK = "app.screen.force.eink";


    public static final String PROP_APP_SECONDARY_TAP_ACTION_TYPE = "app.touch.secondary.action.type";
    public static final String PROP_APP_GESTURE_PAGE_FLIPPING = "app.touch.gesture.page.flipping";
	public static final String PROP_APP_DISABLE_TWO_POINTER_GESTURES = "app.touch.gesture.twopointer.disable";

    public static final String PROP_APP_VIEW_AUTOSCROLL_SPEED  ="app.view.autoscroll.speed";
    public static final String PROP_APP_VIEW_AUTOSCROLL_TYPE  ="app.view.autoscroll.type";

    public static final String PROP_APP_THEME = "app.ui.theme";
    public static final String PROP_APP_THEME_DAY  = "app.ui.theme.day";
    public static final String PROP_APP_THEME_NIGHT = "app.ui.theme.night";

    public static final String PROP_APP_LOCALE = "app.locale.name";
    
    public static final String PROP_APP_STARTUP_ACTION = "app.startup.action";

    public static final String PROP_APP_PLUGIN_ENABLED = "app.plugin.enabled.litres";

    String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_ENABLED = "app.cloudsync.googledrive.enabled";
    String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_SETTINGS = "app.cloudsync.googledrive.settings";
    String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_BOOKMARKS = "app.cloudsync.googledrive.bookmarks";
    String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK = "app.cloudsync.googledrive.currentbook";
    String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_AUTOSAVEPERIOD = "app.cloudsync.googledrive.autosaveperiod";
    String PROP_APP_CLOUDSYNC_CONFIRMATIONS = "app.cloudsync.confirmations";

	public static final String PROP_APP_GEO = "app.geo.locations";
	public static final String PROP_TOOLBAR_TITLE = "window.toolbar.title";
	public static final String PROP_CLOUD_TITLE = "window.cloud.title";
	public static final String PROP_DICTIONARY_TITLE = "window.dictionary.title";
	public static final String PROP_FILEBROWSER_TITLE = "window.filebrowser.title";
	public static final String PROP_RARE_TITLE = "window.rare.title";
	public static final String PROP_TTS_TITLE = "window.tts.title";
	public static final String PROP_BACKLIGHT_TITLE = "window.backlight.title";
	public static final String PROP_SELECTION_MODES_TITLE = "window.selection.modes.title";
	public static final String PROP_PAGEANDORIENTATION_TITLE = "window.pageandorientation.modes.title";
	public static final String PROP_FONTTWEAKS_TITLE = "window.font.tweaks.title";
	public static final String PROP_SPACING_TITLE = "window.spacing.title";
	public static final String PROP_PAGECOLORS_TITLE = "window.pagecolors.title";
	public static final String PROP_PAGEMARGINS_TITLE = "window.pagemargins.title";

	// available options for PROP_APP_SELECTION_ACTION setting
	public static final int SELECTION_ACTION_SAME_AS_COMMON = -1;
    public static final int SELECTION_ACTION_TOOLBAR = 0;
    public static final int SELECTION_ACTION_COPY = 1;
    public static final int SELECTION_ACTION_DICTIONARY = 2;
    public static final int SELECTION_ACTION_BOOKMARK = 3;
    public static final int SELECTION_ACTION_FIND = 4;
    public static final int SELECTION_ACTION_DICTIONARY_1 = 5;
    public static final int SELECTION_ACTION_DICTIONARY_2 = 6;
    public static final int SELECTION_ACTION_SEARCH_WEB = 7;
    public static final int SELECTION_ACTION_SEND_TO = 8;
    public static final int SELECTION_ACTION_USER_DIC = 9;
    public static final int SELECTION_ACTION_CITATION = 10;
    public static final int SELECTION_ACTION_DICTIONARY_LIST = 11;
	public static final int SELECTION_ACTION_DICTIONARY_3 = 12;
	public static final int SELECTION_ACTION_DICTIONARY_4 = 13;
	public static final int SELECTION_ACTION_DICTIONARY_5 = 14;
	public static final int SELECTION_ACTION_DICTIONARY_6 = 15;
	public static final int SELECTION_ACTION_DICTIONARY_7 = 16;

	// available options for PROP_APP_SECONDARY_TAP_ACTION_TYPE setting
    public static final int TAP_ACTION_TYPE_LONGPRESS = 0;
    public static final int TAP_ACTION_TYPE_DOUBLE = 1;
    public static final int TAP_ACTION_TYPE_SHORT = 2;

    // available options for PROP_APP_FLICK_BACKLIGHT_CONTROL setting
    public static final int BACKLIGHT_CONTROL_FLICK_NONE = 0;
    public static final int BACKLIGHT_CONTROL_FLICK_LEFT = 1;
    public static final int BACKLIGHT_CONTROL_FLICK_RIGHT = 2;

    public static final int APP_STARTUP_ACTION_LAST_BOOK = 0;
    public static final int APP_STARTUP_ACTION_ROOT = 1;
    public static final int APP_STARTUP_ACTION_RECENT_BOOKS = 2;
    public static final int APP_STARTUP_ACTION_LAST_BOOK_FOLDER = 3;
    
    public static final int VIEWER_STATUS_NONE = 0;
    public static final int VIEWER_STATUS_TOP = 1;
    public static final int VIEWER_STATUS_BOTTOM = 2;
    public static final int VIEWER_STATUS_PAGE = 3;
	public static final int VIEWER_STATUS_PAGE_2LINES = 4;

	public static final int VIEWER_TOOLBAR_NONE = 0;
    public static final int VIEWER_TOOLBAR_TOP = 1;
    public static final int VIEWER_TOOLBAR_BOTTOM = 2;
    public static final int VIEWER_TOOLBAR_LEFT = 3;
    public static final int VIEWER_TOOLBAR_RIGHT = 4;
    public static final int VIEWER_TOOLBAR_SHORT_SIDE = 5;
    public static final int VIEWER_TOOLBAR_LONG_SIDE = 6;

    public static final int VIEWER_TOOLBAR_100 = 0;
    public static final int VIEWER_TOOLBAR_100_gray = 1;
    public static final int VIEWER_TOOLBAR_100_inv = 2;
    public static final int VIEWER_TOOLBAR_75 = 3;
    public static final int VIEWER_TOOLBAR_75_gray = 4;
    public static final int VIEWER_TOOLBAR_75_inv = 5;
    public static final int VIEWER_TOOLBAR_50 = 6;
    public static final int VIEWER_TOOLBAR_50_gray = 7;
    public static final int VIEWER_TOOLBAR_50_inv = 8;

	public static final int CLOUD_SYNC_VARIANT_DISABLED = 0;
	public static final int CLOUD_SYNC_VARIANT_FILESYSTEM = 1;
	public static final int CLOUD_SYNC_VARIANT_YANDEX = 2;

	public enum Lang {
    	DEFAULT("system", R.string.options_app_locale_system, R.raw.help_template_en),
    	EN("en", R.string.options_app_locale_en, R.raw.help_template_en),
        DE("de", R.string.options_app_locale_de, 0),
    	ES("es", R.string.options_app_locale_es, 0),
    	FR("fr", R.string.options_app_locale_fr, 0),
    	JA("ja", R.string.options_app_locale_ja, 0),
    	RU("ru", R.string.options_app_locale_ru, R.raw.help_template_ru),
    	UK("uk", R.string.options_app_locale_uk, R.raw.help_template_ru),
    	BG("bg", R.string.options_app_locale_bg, 0),
    	BE("be", R.string.options_app_locale_be, 0),
    	SK("sk", R.string.options_app_locale_sk, 0),
    	TR("tr", R.string.options_app_locale_tr, 0),
    	LT("lt", R.string.options_app_locale_lt, 0),
    	IT("it", R.string.options_app_locale_it, 0),
    	HU("hu", R.string.options_app_locale_hu, R.raw.help_template_hu),
    	NL("nl", R.string.options_app_locale_nl, 0),
    	PL("pl", R.string.options_app_locale_pl, 0),
        PT("pt", R.string.options_app_locale_pt, 0),
        PT_BR("pt_BR", R.string.options_app_locale_pt_rbr, 0),
    	CS("cs", R.string.options_app_locale_cs, 0),
    	ZH_CN("zh_CN", R.string.options_app_locale_zh_cn, R.raw.help_template_zh_cn),
    	;
    	
    	public Locale getLocale() {
   			return getLocale(code);
    	}
    	
    	static public Locale getLocale(String code) {
    		if (code.length() == 2)
    			return new Locale(code);
    		if (code.length() == 5)
    			return new Locale(code.substring(0, 2), code.substring(3, 5));
    		return null;
    	}

    	static public String getCode(Locale locale) {
    		String country = locale.getCountry();
    		if (country == null || country.length()==0)
    			return locale.getLanguage();
			return locale.getLanguage() + "_" + country;
    	}
    	
    	static public Lang byCode(String code) {
    		for (Lang lang : values())
    			if (lang.code.equals(code))
    				return lang;
    		if (code.length() > 2) {
    			code = code.substring(0, 2);
        		for (Lang lang : values())
        			if (lang.code.equals(code))
        				return lang;
    		}
    		Log.w("cr3", "language not found by code " + code);
    		return DEFAULT;
    	}
    	
    	private Lang(String code, int nameResId, int helpFileResId) {
    		this.code = code;
    		this.nameId = nameResId;
    		this.helpFileResId = helpFileResId;
    	}
    	public final String code;
    	public final int nameId;
    	public final int helpFileResId;
    };
    
    
	public final static int MAX_PROFILES = 6;

	// settings which depend on profile
	public final static String[] PROFILE_SETTINGS = {
	    "background.*",
	    PROP_NIGHT_MODE,
	    "font.*",
	    "crengine.page.*",
	    PROP_FONT_SIZE,
	    PROP_FALLBACK_FONT_FACE,
	    PROP_INTERLINE_SPACE,
	    PROP_STATUS_LINE,
	    PROP_FOOTNOTES,
	    "window.status.*",
	    PROP_FLOATING_PUNCTUATION,
	    PROP_LANDSCAPE_PAGES,
	    PROP_HYPHENATION_DICT,

	    "crengine.image.*",
	    PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT,
		PROP_FORMAT_SPACE_WIDTH_SCALE_PERCENT,
		PROP_FORMAT_UNUSED_SPACE_THRESHOLD_PERCENT,
		PROP_FORMAT_MAX_ADDED_LETTER_SPACING_PERCENT,
		PROP_APP_FULLSCREEN,
	    "app.screen.*",
	    PROP_APP_DICTIONARY,
        PROP_APP_DICTIONARY_2,
		PROP_APP_DICTIONARY_3,
		PROP_APP_DICTIONARY_4,
		PROP_APP_DICTIONARY_5,
		PROP_APP_DICTIONARY_6,
		PROP_APP_DICTIONARY_7,
		PROP_APP_SHOW_USER_DIC_PANEL,
        PROP_APP_DICT_WORD_CORRECTION,
        PROP_APP_SHOW_USER_DIC_PANEL,
        PROP_APP_DICT_LONGTAP_CHANGE,
		PROP_CLOUD_SYNC_VARIANT,
		PROP_CLOUD_WIKI1_ADDR,
		PROP_CLOUD_WIKI2_ADDR,
		PROP_CLOUD_WIKI_SAVE_HISTORY,
		PROP_CLOUD_YND_HOME_FOLDER,
        PROP_SAVE_POS_TO_CLOUD_TIMEOUT,
	    PROP_APP_SELECTION_ACTION,
        PROP_APP_SELECTION_ACTION_LONG,
        PROP_APP_MULTI_SELECTION_ACTION,
		PROP_APP_SELECTION2_ACTION,
		PROP_APP_SELECTION2_ACTION_LONG,
		PROP_APP_MULTI_SELECTION2_ACTION,
		PROP_APP_SELECTION3_ACTION,
		PROP_APP_SELECTION3_ACTION_LONG,
		PROP_APP_MULTI_SELECTION3_ACTION,
		PROP_APP_SELECTION_PERSIST,
	    PROP_APP_HIGHLIGHT_BOOKMARKS + "*",
	    PROP_HIGHLIGHT_SELECTION_COLOR + "*",
	    PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT + "*",
	    PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION + "*",

      "viewer.*",
	    PROP_APP_VIEW_AUTOSCROLL_SPEED,
	    PROP_APP_VIEW_AUTOSCROLL_TYPE,
	    	    
      "app.key.*",
	    "app.tapzone.*",
	    PROP_APP_DOUBLE_TAP_SELECTION,
	    "app.touch.*",

	    "app.ui.theme*",
        PROP_APP_ICONS_IS_CUSTOM_COLOR,
        PROP_APP_ICONS_CUSTOM_COLOR,
		PROP_GLOBAL_MARGIN,

		PROP_APP_HIDE_STATE_DIALOGS,
	    PROP_APP_HIDE_CSS_WARNING

};


}
