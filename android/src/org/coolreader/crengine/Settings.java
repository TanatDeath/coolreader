package org.coolreader.crengine;

import android.util.Log;

import org.coolreader.R;

import java.util.ArrayList;
import java.util.Locale;

public interface Settings {
    String PROP_PAGE_BACKGROUND_IMAGE       ="background.image";
	String PROP_PAGE_BACKGROUND_IMAGE_SAVE  ="background.image.save";
    String PROP_PAGE_BACKGROUND_IMAGE_DAY   ="background.image.day";
    String PROP_PAGE_BACKGROUND_IMAGE_NIGHT ="background.image.night";
    String PROP_NIGHT_MODE              ="crengine.night.mode";
    String PROP_FONT_COLOR_DAY          ="font.color.day";
    String PROP_BACKGROUND_COLOR_DAY    ="background.color.day";
    String PROP_FONT_COLOR_NIGHT        ="font.color.night";
    String PROP_BACKGROUND_COLOR_NIGHT  ="background.color.night";
    String PROP_FONT_COLOR              ="font.color.default";
    String PROP_BACKGROUND_COLOR        ="background.color.default";
	String PROP_BACKGROUND_COLOR_SAVE   ="background.color.save";
	String PROP_BACKGROUND_COLOR_SAVE_WAS   ="background.color.save.was";
    String PROP_FONT_ANTIALIASING       ="font.antialiasing.mode";
    String PROP_FONT_FACE               ="font.face.default";
    String PROP_FONT_HINTING            ="font.hinting.mode";
	String PROP_FONT_CHAR_SPACE_COMPRESS = "font.char.space.compress";
    String PROP_FONT_GAMMA              ="font.gamma";
    String PROP_FONT_GAMMA_DAY          ="font.gamma.day";
    String PROP_FONT_GAMMA_NIGHT        ="font.gamma.night";
	String PROP_FONT_WEIGHT_EMBOLDEN_OBSOLETED ="font.face.weight.embolden";	// obsoleted
	String PROP_FONT_ITALICIZE          ="font.face.italicize";
	String PROP_FONT_EMBOLDEN_ALG       ="font.face.embolden.alg";
	String PROP_FONT_FINE_EMBOLDEN      ="font.face.fine.embolden";
	String PROP_FONT_BASE_WEIGHT        ="font.face.base.weight";        // replaces PROP_FONT_WEIGHT_EMBOLDEN ("font.face.weight.embolden")
	String PROP_TRIM_INITIAL_PAR_SPACES ="crengine.trim.initial.paragraph.spaces";

	String PROP_TXT_OPTION_PREFORMATTED ="crengine.file.txt.preformatted";
    String PROP_LOG_FILENAME            ="crengine.log.filename";
    String PROP_LOG_LEVEL               ="crengine.log.level";
    String PROP_LOG_AUTOFLUSH           ="crengine.log.autoflush";
    String PROP_FONT_SIZE               ="crengine.font.size";
	String PROP_FONT_SIZE_USER_DIC      ="crengine.font.size.userdic";
	String PROP_FALLBACK_FONT_FACES     ="crengine.font.fallback.faces";
	String PROP_STATUS_FONT_COLOR       ="crengine.page.header.font.color";
    String PROP_STATUS_FONT_COLOR_DAY   ="crengine.page.header.font.color.day";
    String PROP_STATUS_FONT_COLOR_NIGHT ="crengine.page.header.font.color.night";
    String PROP_STATUS_FONT_FACE        ="crengine.page.header.font.face";
    String PROP_STATUS_FONT_SIZE        ="crengine.page.header.font.size";
	String PROP_STATUS_HEADER_IN_SCROLL_MODE
			                            ="crengine.page.header.in.scroll.mode";
    String PROP_STATUS_CHAPTER_MARKS    ="crengine.page.header.chapter.marks";
    String PROP_PAGE_MARGIN_TOP         ="crengine.page.margin.top";
    String PROP_PAGE_MARGIN_BOTTOM      ="crengine.page.margin.bottom";
    String PROP_PAGE_MARGIN_LEFT        ="crengine.page.margin.left";
    String PROP_PAGE_MARGIN_RIGHT       ="crengine.page.margin.right";
	String PROP_GLOBAL_MARGIN       	="crengine.global.margin";
	String PROP_ROUNDED_CORNERS_MARGIN  ="crengine.rounded.corners.margin";
	String PROP_ROUNDED_CORNERS_MARGIN_POS = "crengine.rounded.corners.margin.pos";
	String PROP_ROUNDED_CORNERS_MARGIN_MOD = "crengine.rounded.corners.margin.mod";
	String PROP_ROUNDED_CORNERS_MARGIN_FSCR = "crengine.rounded.corners.margin.fscr";
	String PROP_EXT_FULLSCREEN_MARGIN   ="crengine.ext.fullscreen.margin";
	String PROP_EXT_FULLSCREEN_MOD   ="crengine.ext.fullscreen.mod";
	String PROP_PAGE_VIEW_MODE          ="crengine.page.view.mode"; // pages/scroll
    String PROP_PAGE_VIEW_MODE_AUTOCHANGED = "crengine.page.view.mode.autochanged"; // when tts
	String PROP_PAGE_VIEW_MODE_TTS_DONT_CHANGE = "crengine.page.view.mode.tts.dont.change"; // when tts
	String PROP_PAGE_VIEW_MODE_SEL_DONT_CHANGE = "crengine.page.view.mode.tts.dont.change2"; // when sel toolbar
    String PROP_PAGE_ANIMATION          ="crengine.page.animation";
	String PROP_PAGE_ANIMATION_SPEED    ="crengine.page.animation.speed";
	String PROP_DOUBLE_CLICK_INTERVAL    ="crengine.double.click.interval";
	String PROP_PREVENT_CLICK_INTERVAL    ="crengine.prevent.click.interval";
	String PROP_INTERLINE_SPACE         ="crengine.interline.space";
    String PROP_ROTATE_ANGLE            ="window.rotate.angle";
    String PROP_EMBEDDED_STYLES         ="crengine.doc.embedded.styles.enabled";
	String PROP_EMBEDDED_STYLES_DEF     ="crengine.doc.embedded.styles.def.enabled";
    String PROP_EMBEDDED_FONTS          ="crengine.doc.embedded.fonts.enabled";
    String PROP_DISPLAY_INVERSE         ="crengine.display.inverse";
//    String PROP_DISPLAY_FULL_UPDATE_INTERVAL ="crengine.display.full.update.interval";
//    String PROP_DISPLAY_TURBO_UPDATE_MODE ="crengine.display.turbo.update";

    String PROP_STATUS_LOCATION         ="viewer.status.location";
    String PROP_TOOLBAR_LOCATION        ="viewer.toolbar.location2";
    String PROP_TOOLBAR_HIDE_IN_FULLSCREEN="viewer.toolbar.fullscreen.hide";
    String PROP_TOOLBAR_APPEARANCE      ="viewer.toolbar.appearance";
    String PROP_TOOLBAR_BUTTONS         ="viewer.toolbar.buttons";
	String PROP_READING_MENU_BUTTONS    ="viewer.reading.menu.buttons";
	String PROP_GROUP_BUTTONS			="viewer.group.buttons";
    String PROP_SKIPPED_RES             ="viewer.skipped.resolutions";
	String PROP_APP_RESTORE_SETTINGS    ="app.restore.settings";
	String PROP_DIC_LIST_MULTI          ="app.dic.list.multi";

    String PROP_STATUS_LINE             ="window.status.line";
    String PROP_BOOKMARK_ICONS          ="crengine.bookmarks.icons";
    String PROP_FOOTNOTES               ="crengine.footnotes";
    String PROP_SHOW_TIME               ="window.status.clock";
    String PROP_SHOW_TITLE              ="window.status.title";
    String PROP_SHOW_BATTERY            ="window.status.battery";
    String PROP_SHOW_BATTERY_PERCENT    ="window.status.battery.percent";
    String PROP_SHOW_POS_PERCENT        ="window.status.pos.percent";
    String PROP_SHOW_PAGE_COUNT         ="window.status.pos.page.count";
    String PROP_SHOW_PAGE_NUMBER        ="window.status.pos.page.number";
	String PROP_SHOW_PAGES_TO_CHAPTER   ="window.status.pos.pages.to.chapter";
	String PROP_SHOW_TIME_LEFT   		="window.status.pos.time.left";
	String PROP_SHOW_TIME_LEFT_TO_CHAPTER ="window.status.pos.time.left.to.chapter";
	String PROP_FONT_SHAPING            ="font.shaping.mode";
    String PROP_FONT_KERNING_ENABLED    ="font.kerning.enabled";
    String PROP_FLOATING_PUNCTUATION    ="crengine.style.floating.punctuation.enabled";
    String PROP_LANDSCAPE_PAGES         ="window.landscape.pages";
    //String PROP_HYPHENATION_DICT        ="crengine.hyphenation.dictionary.code"; // non-crengine (old)
	String PROP_HYPHENATION_DICT        = "crengine.hyphenation.directory";

	String PROP_AUTOSAVE_BOOKMARKS      ="crengine.autosave.bookmarks";
	// New textlang typography settings:
	String PROP_TEXTLANG_MAIN_LANG      = "crengine.textlang.main.lang";
	String PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED = "crengine.textlang.embedded.langs.enabled";
	String PROP_TEXTLANG_HYPHENATION_ENABLED    = "crengine.textlang.hyphenation.enabled";
	String PROP_TEXTLANG_HYPH_SOFT_HYPHENS_ONLY = "crengine.textlang.hyphenation.soft.hyphens.only";
	String PROP_TEXTLANG_HYPH_FORCE_ALGORITHMIC = "crengine.textlang.hyphenation.force.algorithmic";

    String PROP_PROFILE_NUMBER          ="crengine.profile.number"; // current settings profile number
	String PROP_PROFILE_NAME            ="crengine.profile.name";
	String PROP_APP_SETTINGS_SHOW_ICONS ="app.settings.show.icons";
    String PROP_APP_KEY_BACKLIGHT_OFF   ="app.key.backlight.disabled";
	String PROP_APP_USE_EINK_FRONTLIGHT   ="app.use.eink.frontlight"; //KR
	String PROP_APP_BACKLIGHT_SWIPE_SENSIVITY   ="app.backlight.swipe.sensivity";

	 // image scaling settings
	 // mode: 0=disabled, 1=integer scaling factors, 2=free scaling
	 // scale: 0=auto based on font size, 1=no zoom, 2=scale up to *2, 3=scale up to *3
    String PROP_IMG_SCALING_ZOOMIN_INLINE_MODE = "crengine.image.scaling.zoomin.inline.mode";
    String PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE = "crengine.image.scaling.zoomin.inline.scale";
    String PROP_IMG_SCALING_ZOOMOUT_INLINE_MODE = "crengine.image.scaling.zoomout.inline.mode";
    String PROP_IMG_SCALING_ZOOMOUT_INLINE_SCALE = "crengine.image.scaling.zoomout.inline.scale";
    String PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE = "crengine.image.scaling.zoomin.block.mode";
    String PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE = "crengine.image.scaling.zoomin.block.scale";
    String PROP_IMG_SCALING_ZOOMOUT_BLOCK_MODE = "crengine.image.scaling.zoomout.block.mode";
    String PROP_IMG_SCALING_ZOOMOUT_BLOCK_SCALE = "crengine.image.scaling.zoomout.block.scale";
	String PROP_IMG_CUSTOM_BACKGROUND = "crengine.image.custom.background";
	String PROP_IMG_CUSTOM_BACKGROUND_COLOR = "crengine.image.custom.background.color";
    
    String PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT = "crengine.style.space.condensing.percent";
	String PROP_FORMAT_UNUSED_SPACE_THRESHOLD_PERCENT = "crengine.style.unused.space.threshold.percent";
	String PROP_FORMAT_MAX_ADDED_LETTER_SPACING_PERCENT = "crengine.style.max.added.letter.spacing.percent";
	String PROP_FORMAT_SPACE_WIDTH_SCALE_PERCENT = "crengine.style.space.width.scale.percent";

	// default is 96 (1 css px = 1 screen px)
	// use 0 for old crengine behaviour (no support for absolute units and 1css px = 1 screen px)
	String PROP_RENDER_DPI              = "crengine.render.dpi";
	//String PROP_RENDER_SCALE_FONT_WITH_DPI = "crengine.render.scale.font.with.dpi";
	String PROP_RENDER_BLOCK_RENDERING_FLAGS = "crengine.render.block.rendering.flags";
	String PROP_REQUESTED_DOM_VERSION   = "crengine.render.requested_dom_version";

	String PROP_MIN_FILE_SIZE_TO_CACHE  ="crengine.cache.filesize.min";
    String PROP_FORCED_MIN_FILE_SIZE_TO_CACHE  ="crengine.cache.forced.filesize.min";
    String PROP_PROGRESS_SHOW_FIRST_PAGE="crengine.progress.show.first.page";

    String PROP_CONTROLS_ENABLE_VOLUME_KEYS ="app.controls.volume.keys.enabled";
    
    String PROP_APP_FULLSCREEN          ="app.fullscreen";
	String PROP_APP_TITLEBAR_NEW = "app.titlebar.new";
    String PROP_APP_BOOK_PROPERTY_SCAN_ENABLED ="app.browser.fileprops.scan.enabled";
    String PROP_APP_SHOW_COVERPAGES     ="app.browser.coverpages";
    String PROP_APP_COVERPAGE_SIZE     ="app.browser.coverpage.size"; // 0==small, 2==BIG
    String PROP_APP_SCREEN_ORIENTATION  ="app.screen.orientation";
    String PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION  ="app.screen.orientation.popup.duration";
    String PROP_APP_SCREEN_BACKLIGHT    ="app.screen.backlight";
	String PROP_APP_SCREEN_WARM_BACKLIGHT    ="app.screen.warm.backlight"; //CR!
	String PROP_APP_SCREEN_BACKLIGHT1    ="app.screen.backlight1";
	String PROP_APP_SCREEN_WARM_BACKLIGHT1    ="app.screen.warm.backlight1";
	String PROP_APP_SCREEN_BACKLIGHT2    ="app.screen.backlight2";
	String PROP_APP_SCREEN_WARM_BACKLIGHT2    ="app.screen.warm.backlight2";
	String PROP_APP_SCREEN_BACKLIGHT3    ="app.screen.backlight3";
	String PROP_APP_SCREEN_WARM_BACKLIGHT3    ="app.screen.warm.backlight3";
	//decided to remove and do like in CR
	//String PROP_APP_SCREEN_GET_BACKLIGHT_FROM_SYSTEM = "app.screen.get.backlight.from.system";
	String PROP_APP_SCREEN_BACKLIGHT_FIX_DELTA    = "app.screen.backlight.fix.delta";
	String PROP_APP_MOTION_TIMEOUT    ="app.motion.timeout";
    String PROP_APP_SCREEN_BACKLIGHT_DAY   ="app.screen.backlight.day";
    String PROP_APP_SCREEN_BACKLIGHT_NIGHT ="app.screen.backlight.night";
    String PROP_APP_DOUBLE_TAP_SELECTION     ="app.controls.doubletap.selection";
	String PROP_APP_BOUNCE_TAP_INTERVAL   ="app.controls.bounce.interval";
    String PROP_APP_TAP_ZONE_ACTIONS_TAP     ="app.tapzone.action.tap";
	String PROP_APP_TAP_ZONE_NON_SENS_LEFT     ="app.tapzone.non.sens.left";
	String PROP_APP_TAP_ZONE_NON_SENS_RIGHT     ="app.tapzone.non.sens.right";
	String PROP_APP_DEVICE_TURN = "app.device.turn";

	String PROP_APP_HARDWARE_KEYS = "app.hardware.keys";
	String PROP_APP_DEVICE_TURN_ENABLE     ="app.device.turn.enable";
	String PROP_APP_KEY_ACTIONS_PRESS     ="app.key.action.press";
    String PROP_APP_TRACKBALL_DISABLED    ="app.trackball.disabled";
    String PROP_APP_SCREEN_BACKLIGHT_LOCK    ="app.screen.backlight.lock.enabled";
    String PROP_APP_TAP_ZONE_HILIGHT     ="app.tapzone.hilight";
	String PROP_APP_TURN_PAGE     ="app.turn.page";
	String PROP_APP_FLICK_BACKLIGHT_CONTROL = "app.screen.backlight.control.flick";
	//String PROP_APP_FLICK_WARMLIGHT_CONTROL = "app.screen.warmlight.control.flick"; //CR!
	String PROP_APP_BOOK_SORT_ORDER = "app.browser.sort.order";
	String PROP_APP_TRANSLATE_DIR = "app.translate.dir";
    String PROP_APP_DICTIONARY = "app.dictionary.current";
    String PROP_APP_DICTIONARY_2 = "app.dictionary2.current";
	String PROP_APP_DICTIONARY_3 = "app.dictionary3.current";
	String PROP_APP_DICTIONARY_4 = "app.dictionary4.current";
	String PROP_APP_DICTIONARY_5 = "app.dictionary5.current";
	String PROP_APP_DICTIONARY_6 = "app.dictionary6.current";
	String PROP_APP_DICTIONARY_7 = "app.dictionary7.current";
	String PROP_APP_DICTIONARY_8 = "app.dictionary8.current";
	String PROP_APP_DICTIONARY_9 = "app.dictionary9.current";
	String PROP_APP_DICTIONARY_10 = "app.dictionary10.current";
	String PROP_APP_DICT_TYPE_SELECTED0 = "app.dict.type.selected0";
	String PROP_APP_DICT_TYPE_SELECTED1 = "app.dict.type.selected1";
	String PROP_APP_DICT_TYPE_SELECTED2 = "app.dict.type.selected2";
    String PROP_APP_DICT_WORD_CORRECTION = "app.dictionary.word.correction";
	String PROP_APP_DICT_DONT_SAVE_IF_MORE = "app.dictionary.dont.save.if.more";
	String PROP_APP_DICT_AUTO_SPEAK = "app.dictionary.auto.speak";
	String PROP_INSPECTOR_MODE_NO_DIC_HISTORY = "app.inspector.mode.no.dic.history";
    String PROP_APP_SHOW_USER_DIC_PANEL = "app.dictionary.show.user.dic.panel";
	String PROP_APP_SHOW_USER_DIC_CONTENT = "app.dictionary.user.dic.content";
	String PROP_APP_QUICK_TRANSLATION_DIRS = "app.quick.translation.dirs";
	String PROP_APP_ONLINE_OFFLINE_DICS = "app.quick.translation.online.offline.dics";
	String PROP_APP_OFFLINE_DICS = "app.offline.dics";
	String PROP_APP_DICT_LONGTAP_CHANGE = "app.dictionary.longtap.change";
    String PROP_APP_SELECTION_ACTION = "app.selection.action";
    String PROP_APP_SELECTION_ACTION_LONG = "app.selection.action.long";
	String PROP_APP_MULTI_SELECTION_ACTION = "app.multiselection.action";
	String PROP_APP_BOOKMARK_ACTION_SEND_TO = "app.bookmark.action.send.to";
	String PROP_APP_BOOKMARK_ACTION_SEND_TO_MOD = "app.bookmark.action.send.to.mod";
	String PROP_APP_SELECTION2_ACTION = "app.selection2.action";
	String PROP_APP_SELECTION2_ACTION_LONG = "app.selection2.action.long";
	String PROP_APP_MULTI_SELECTION2_ACTION = "app.multiselection2.action";
	String PROP_APP_SELECTION3_ACTION = "app.selection3.action";
	String PROP_APP_SELECTION3_ACTION_LONG = "app.selection3.action.long";
	String PROP_APP_MULTI_SELECTION3_ACTION = "app.multiselection3.action";
	String PROP_APP_SELECTION_PERSIST = "app.selection.persist";
	String PROP_APP_MIGRATE_SETTINGS = "app.migrate.settings";
	String PROP_CLOUD_SYNC_VARIANT = "app.cloud.sync.variant";
	String PROP_CLOUD_YND_HOME_FOLDER = "app.cloud.ynd.home.folder";
	String PROP_CLOUD_YND_SETTINGS = "app.cloud.ynd.settings";
	String PROP_CLOUD_DBX_SETTINGS = "app.cloud.dbx.settings";
	String PROP_CLOUD_WIKI1_ADDR = "app.cloud.wiki1";
	String PROP_CLOUD_WIKI2_ADDR = "app.cloud.wiki2";
	String PROP_CLOUD_WIKI_SAVE_HISTORY = "app.cloud.wiki.save.history";
	String PROP_CLOUD_YND_TRANSLATE_OPTIONS = "app.cloud.ynd.translate.options";
	String PROP_CLOUD_LINGVO_OPTIONS = "app.cloud.lingvo.options";
	String PROP_CLOUD_DEEPL_OPTIONS = "app.cloud.deepl.options";
	String PROP_CLOUD_LITRES_SETTINGS = "app.cloud.litres.settings";
	String PROP_CLOUD_LITRES_DISABLED = "app.cloud.litres.disabled";
	String PROP_SAVE_POS_TO_CLOUD_TIMEOUT = "app.autosave.reading.pos.timeout";
    String PROP_SAVE_POS_TIMEOUT = "app.autosave.reading.pos.timeout.1";
    String PROP_SAVE_POS_SPEAK_TIMEOUT = "app.autosave.reading.pos.timeout.2";
    String PROP_APP_DOWNLOADED_SET_ADD_MARKS = "app.downloaded.set.add.marks";
    String PROP_APP_TTS_FORCE_KOEF = "app.tts.force.koef";
	String PROP_APP_CLOUD_POS_DATE_SORT = "app.cloudpos.date.sort";
	String PROP_APP_CLOUD_POS_HIDE_CURRENT_DEV = "app.cloudpos.hide.current.dev";
	String PROP_APP_ROOT_VIEW_FS_SECTION_HIDE = "app.rootview.fs_section.hide";
	String PROP_APP_ROOT_VIEW_LIB_SECTION_HIDE = "app.rootview.lib_section.hide";
	String PROP_APP_ROOT_VIEW_OPDS_SECTION_HIDE = "app.rootview.opds_section.hide";
	String PROP_APP_ROOT_VIEW_OPDS_SECTION_SORT_AZ = "app.rootview.opds_section.sort.az";
	String PROP_APP_ROOT_VIEW_CURRENT_BOOK_SECTION_POS = "app.rootview.current_book_section.pos";
	String PROP_APP_ROOT_VIEW_RECENT_SECTION_POS = "app.rootview.recent_section.pos";
	String PROP_APP_ROOT_VIEW_FS_SECTION_POS = "app.rootview.fs.pos";
	String PROP_APP_ROOT_VIEW_LIBRARY_SECTION_POS = "app.rootview.library_section.pos";
	String PROP_APP_ROOT_VIEW_OPDS_SECTION_POS = "app.rootview.opds_section.pos";

	String PROP_APP_ROOT_VIEW_FS_SECTION_TYPE = "app.rootview.fs.type";
	String PROP_APP_ROOT_VIEW_LIBRARY_SECTION_TYPE = "app.rootview.library_section.type";
	String PROP_APP_ROOT_VIEW_OPDS_SECTION_TYPE = "app.rootview.opds_section.type";
	String PROP_APP_OPTIONS_PAGE_SELECTED = "app.options.page.selected";
	String PROP_APP_OPTIONS_EXT_SELECTION_TOOLBAR = "app.options.ext.selection.toolbar";
	String PROP_APP_OPTIONS_SELECTION_TOOLBAR_BACKGROUND = "app.options.selection.toolbar.background";
	String PROP_APP_OPTIONS_SELECTION_TOOLBAR_TRANSP_BUTTONS = "app.options.ext.selection.toolbar.transp.buttons";
	String PROP_APP_OPTIONS_SELECTION_TOOLBAR_SLIDERS = "app.options.ext.selection.toolbar.sliders";
	String PROP_APP_OPTIONS_SELECTION_TOOLBAR_RECENT_DICS = "app.options.ext.selection.toolbar.recent.dics";
	String PROP_APP_OPTIONS_TTS_TOOLBAR_BACKGROUND = "app.options.tts.toolbar.background";
	String PROP_APP_OPTIONS_TTS_TOOLBAR_TRANSP_BUTTONS = "app.options.ext.tts.toolbar.transp.buttons";
	String PROP_APP_INIT = "app.init";
	String PROP_APP_SHOW_FILES_DIR = "app.show.files.dir";
	String PROP_APP_SHOW_FILES_NO_MARK = "app.show.files.no.mark";
	String PROP_APP_SHOW_FILES_TO_READ = "app.show.files.to.read";
	String PROP_APP_SHOW_FILES_READING = "app.show.files.reading";
	String PROP_APP_SHOW_FILES_FINISHED = "app.show.files.finished";
	String PROP_APP_SAVE_DOC_EXT_CONTROLS_SHOW = "app.save.doc.ext.controls.show";

	String PROP_APP_TTS_SPEED_1 = "app.tts.speed.1";
	String PROP_APP_TTS_SPEED_2 = "app.tts.speed.2";
	String PROP_APP_TTS_SPEED_3 = "app.tts.speed.3";
	String PROP_APP_TTS_VOL_1 = "app.tts.vol.1";
	String PROP_APP_TTS_VOL_2 = "app.tts.vol.2";
	String PROP_APP_TTS_VOL_3 = "app.tts.vol.3";

	String PROP_APP_HIDE_STATE_DIALOGS = "app.hide.state.dialogs";
	String PROP_APP_HIDE_CSS_WARNING = "app.hide.state.warning";
	String PROP_APP_DISABLE_SAFE_MODE = "app.disable.safe.mode";
	String PROP_APP_USE_SIMPLE_FONT_SELECT_DIALOG = "app.use.simple.font.select.dialog";

	String PROP_APP_HIGHLIGHT_BOOKMARKS = "crengine.highlight.bookmarks";
	String PROP_APP_HIGHLIGHT_USER_DIC = "crengine.highlight.user.dic";
	String PROP_HIGHLIGHT_SELECTION_COLOR = "crengine.highlight.selection.color";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT = "crengine.highlight.bookmarks.color.comment";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION = "crengine.highlight.bookmarks.color.correction";
    String PROP_APP_HIGHLIGHT_BOOKMARKS_DAY = "crengine.highlight.bookmarks.day";
    String PROP_HIGHLIGHT_SELECTION_COLOR_DAY = "crengine.highlight.selection.color.day";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_DAY = "crengine.highlight.bookmarks.color.comment.day";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_DAY = "crengine.highlight.bookmarks.color.correction.day";
    String PROP_APP_HIGHLIGHT_BOOKMARKS_NIGHT = "crengine.highlight.bookmarks.night";
    String PROP_HIGHLIGHT_SELECTION_COLOR_NIGHT = "crengine.highlight.selection.color.night";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_NIGHT = "crengine.highlight.bookmarks.color.comment.night";
    String PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_NIGHT = "crengine.highlight.bookmarks.color.correction.night";
	String PROP_APP_ICONS_IS_CUSTOM_COLOR ="app.settings.show.icons.is.custom.color";
	String PROP_APP_ICONS_CUSTOM_COLOR ="app.settings.show.icons.custom.color";
	String PROP_APP_ICONS_CUSTOM_COLOR_DAY ="app.settings.show.icons.custom.color.day";
	String PROP_APP_ICONS_CUSTOM_COLOR_NIGHT ="app.settings.show.icons.custom.color.night";

	String PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS = "app.browser.hide.empty.folders";
	String PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES = "app.browser.hide.empty.genres";
	String PROP_APP_FILE_BROWSER_ITEM_TYPE = "app.browser.item.type";

	String PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE = "app.browser.max.group.size";
	String PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_AUTHOR = "app.browser.max.group.size.author";
	String PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_SERIES = "app.browser.max.group.size.series";
	String PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_GENRES = "app.browser.max.group.size.genres";
	String PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_TAGS = "app.browser.max.group.size.tags";
	String PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE_DATES = "app.browser.max.group.size.dates";
	String PROP_APP_FILE_BROWSER_TAP_ACTION = "app.browser.tap.action";
	String PROP_APP_FILE_BROWSER_LONGTAP_ACTION = "app.browser.longtap.action";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON = "app.browser.sec.group.common";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR = "app.browser.sec.group.author";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES = "app.browser.sec.group.series";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES = "app.browser.sec.group.genres";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_TAGS = "app.browser.sec.group.tags";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_SEARCH = "app.browser.sec.group.search";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_DATES = "app.browser.sec.group.dates";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_RATING = "app.browser.sec.group.rating";
	String PROP_APP_FILE_BROWSER_SEC_GROUP_STATE = "app.browser.sec.group.state";
	String PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_LOAD = "app.browser.authors.aliases.load";
	String PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_ENABLED = "app.browser.authors.aliases.enabled";
	String PROP_APP_EXT_DOC_CAME_TIMEOUT = "app.ext.doc.came.timeout";
	String PROP_APP_FILE_BROWSER_ZIP_SCAN = "app.browser.zip.scan";

	String PROP_APP_CLOUD_SAVE_FOLDER_NAMING = "app.cloud.save.folder.naming";
	String PROP_APP_FILE_BROWSER_SHOW_HIDDEN_DIRS = "app.browser.show.hidden.dirs";

    String PROP_APP_SCREEN_UPDATE_MODE = "app.screen.update.mode";
    String PROP_APP_SCREEN_UPDATE_INTERVAL = "app.screen.update.interval";
    String PROP_APP_SCREEN_BLACKPAGE_INTERVAL = "app.screen.blackpage.interval";
    String PROP_APP_SCREEN_BLACKPAGE_DURATION = "app.screen.blackpage.duration";
    String PROP_APP_SCREEN_FORCE_EINK = "app.screen.force.eink";


    String PROP_APP_SECONDARY_TAP_ACTION_TYPE = "app.touch.secondary.action.type";
    String PROP_APP_GESTURE_PAGE_FLIPPING = "app.touch.gesture.page.flipping";
	String PROP_APP_GESTURE_PAGE_FLIPPING_NEW = "app.touch.gesture.page.flipping.new";
	String PROP_APP_GESTURE_PAGE_FLIPPING_SENSIVITY = "app.touch.gesture.page.flipping.sensivity";
	String PROP_APP_GESTURE_PAGE_FLIPPING_PAGE_COUNT = "app.touch.gesture.page.flipping.page.count";
	String PROP_APP_DISABLE_TWO_POINTER_GESTURES = "app.touch.gesture.twopointer.disable";

    String PROP_APP_VIEW_AUTOSCROLL_SPEED  ="app.view.autoscroll.speed";
	String PROP_APP_VIEW_AUTOSCROLL_SIMPLE_SPEED  ="app.view.autoscroll.simple.speed";
    String PROP_APP_VIEW_AUTOSCROLL_TYPE  ="app.view.autoscroll.type";
	String PROP_APP_VIEW_AUTOSCROLL_SHOW_SPEED  ="app.view.autoscroll.show.speed";
	String PROP_APP_VIEW_AUTOSCROLL_SHOW_PROGRESS  ="app.view.autoscroll.show.progress";

    String PROP_APP_THEME = "app.ui.theme";
    String PROP_APP_THEME_DAY  = "app.ui.theme.day";
    String PROP_APP_THEME_NIGHT = "app.ui.theme.night";
	String PROP_APP_FONT_SCALE = "app.ui.font.scale";
	String PROP_APP_START_BEHAVIOUR = "app.start.behaviour";

    String PROP_APP_LOCALE = "app.locale.name";
    
    String PROP_APP_STARTUP_ACTION = "app.startup.action";

    String PROP_APP_PLUGIN_ENABLED = "app.plugin.enabled.litres";

    String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_ENABLED = "app.cloudsync.googledrive.enabled";
    String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_SETTINGS = "app.cloudsync.googledrive.settings";
    String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_BOOKMARKS = "app.cloudsync.googledrive.bookmarks";
	String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_INFO = "app.cloudsync.googledrive.currentbook";
	String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_BODY = "app.cloudsync.googledrive.currentbook.body";
	String PROP_APP_CLOUDSYNC_GOOGLEDRIVE_AUTOSAVEPERIOD = "app.cloudsync.googledrive.autosaveperiod";
	String PROP_APP_CLOUDSYNC_CONFIRMATIONS = "app.cloudsync.confirmations";
	String PROP_APP_CLOUDSYNC_DATA_KEEPALIVE = "app.cloudsync.bookmarks.keepalive";		// days

	String PROP_APP_TTS_SPEED = "app.tts.speed";
	String PROP_APP_TTS_ENGINE = "app.tts.engine";
	String PROP_APP_TTS_USE_DOC_LANG = "app.tts.use.doc.lang";		// The TTS language is set according to the language of the book.
	String PROP_APP_TTS_FORCE_LANGUAGE = "app.tts.force.lang";		// Force use specified language
	String PROP_APP_TTS_VOICE = "app.tts.voice";
	String PROP_APP_TTS_GOOGLE_END_OF_SENTENCE_ABBR = "app.tts.google.end-of-sentence-abbreviation.workaround";	// Use a workaround to disable processing of abbreviations at the end of a sentence when using "Google Speech Services"

	String PROP_APP_TTS_USE_AUDIOBOOK = "app.tts.use.audiobook"; //if *.wordtiming file exists for ebook
	String PROP_APP_TTS_SENTENCE_PAUSE = "app.tts.sentence.pause";

	String PROP_APP_TTS_AUTO_START = "app.tts.auto.start";
	String PROP_APP_VIEW_ANIM_DURATION ="app.view.anim.duration";

	String PROP_APP_GEO = "app.geo.locations";
	String PROP_TOOLBAR_TITLE = "window.toolbar.title";
	String PROP_CLOUD_TITLE = "window.cloud.title";
	String PROP_DICTIONARY_TITLE = "window.dictionary.title";
	String PROP_FILEBROWSER_TITLE = "window.filebrowser.title";
	String PROP_FILEBROWSER_SEC_GROUP = "window.filebrowser.sec.group";
	String PROP_RARE_TITLE = "window.rare.title";

	String PROP_ROOT_SCREEN_TITLE = "window.root.screen.title";
	String PROP_TTS_TITLE = "window.tts.title";
	String PROP_BACKLIGHT_TITLE = "window.backlight.title";
	String PROP_SELECTION_MODES_TITLE = "window.selection.modes.title";
	String PROP_PAGEANDORIENTATION_TITLE = "window.pageandorientation.modes.title";
	String PROP_FONTTWEAKS_TITLE = "window.font.tweaks.title";
	String PROP_EINKSCREENUPDATE_TITLE = "window.eink.screen.update.title";
	String PROP_HYPH_REND_TITLE = "window.hyph.rend.title";
	String PROP_SPACING_TITLE = "window.spacing.title";
	String PROP_PAGECOLORS_TITLE = "window.pagecolors.title";
	String PROP_PAGEMARGINS_TITLE = "window.pagemargins.title";
	String PROP_PAGE_FLIP_TITLE = "window.page.flip.title";
	String PROP_ADD_DIC_TITLE = "window.add.dic.title";

	String PROP_APP_EINK_ONYX_NEED_BYPASS = "app.eink.onyx.need.bypass";
	String PROP_APP_EINK_ONYX_NEED_DEEPGC = "app.eink.onyx.need.deepgc";
	String PROP_APP_EINK_ONYX_REGAL = "app.eink.onyx.regal";
	String PROP_APP_EINK_ONYX_EXTRA_DELAY_FULL_REFRESH   ="app.eink.onyx.extra.delay.full.refresh";
	String PROP_APP_EINK_ONYX_FULL_SCREEN_UPDATE_METHOD   ="app.eink.onyx.full.screen.update.method";
	String PROP_APP_EINK_ONYX_DONT_UPDATE_LIBRARY   ="app.eink.onyx.dont.update.library";
	String PROP_APP_EINK_ONYX_SWITCH_TO_A2   ="app.eink.onyx.switch.to.a2";

	// available options for PROP_APP_SELECTION_ACTION setting
	int SELECTION_ACTION_SAME_AS_COMMON = -1;
	int SEND_TO_ACTION_NONE = -1;
	int SELECTION_ACTION_TOOLBAR = 0;
    int SELECTION_ACTION_COPY = 1;
    int SELECTION_ACTION_DICTIONARY = 2;
    int SELECTION_ACTION_BOOKMARK = 3;
    int SELECTION_ACTION_FIND = 4;
    int SELECTION_ACTION_DICTIONARY_1 = 5;
    int SELECTION_ACTION_DICTIONARY_2 = 6;
    int SELECTION_ACTION_SEARCH_WEB = 7;
    int SELECTION_ACTION_SEND_TO = 8;
    int SELECTION_ACTION_USER_DIC = 9;
    int SELECTION_ACTION_CITATION = 10;
    int SELECTION_ACTION_DICTIONARY_LIST = 11;
	int SELECTION_ACTION_DICTIONARY_3 = 12;
	int SELECTION_ACTION_DICTIONARY_4 = 13;
	int SELECTION_ACTION_DICTIONARY_5 = 14;
	int SELECTION_ACTION_DICTIONARY_6 = 15;
	int SELECTION_ACTION_DICTIONARY_7 = 16;
	int SELECTION_ACTION_DICTIONARY_8 = 21;
	int SELECTION_ACTION_DICTIONARY_9 = 22;
	int SELECTION_ACTION_DICTIONARY_10 = 23;
	int SELECTION_ACTION_BOOKMARK_QUICK = 17;
	int SELECTION_ACTION_COMBO = 18;
	int SELECTION_ACTION_SUPER_COMBO = 19;
	int SELECTION_ACTION_TOOLBAR_SHORT = 20;
	int SELECTION_ACTION_COPY_WITH_PUNCT = 24;

	int SELECTION_ACTION_SPEAK_SELECTION = 25;

	// available options for PROP_APP_SECONDARY_TAP_ACTION_TYPE setting
    int TAP_ACTION_TYPE_LONGPRESS = 0;
    int TAP_ACTION_TYPE_DOUBLE = 1;
    int TAP_ACTION_TYPE_SHORT = 2;

    // available options for PROP_APP_FLICK_BACKLIGHT_CONTROL setting
    int BACKLIGHT_CONTROL_FLICK_NONE = 0;
    int BACKLIGHT_CONTROL_FLICK_LEFT = 1;
    int BACKLIGHT_CONTROL_FLICK_RIGHT = 2;
	int BACKLIGHT_CONTROL_FLICK_BOTH = 3;
	int BACKLIGHT_CONTROL_FLICK_LEFT_COLD_RIGHT_WARM = 4;
	int BACKLIGHT_CONTROL_FLICK_LEFT_WARM_RIGHT_COLD = 5;
	int BACKLIGHT_CONTROL_FLICK_LEFT_BOTH_RIGHT_WARM = 6;
	int BACKLIGHT_CONTROL_FLICK_RIGHT_BOTH_LEFT_WARM = 7;
	int BACKLIGHT_CONTROL_FLICK_LEFT_BOTH_RIGHT_COLD = 8;
	int BACKLIGHT_CONTROL_FLICK_RIGHT_BOTH_LEFT_COLD = 9;
	int BACKLIGHT_CONTROL_FLICK_BOTH_BOTH = 10;
	int BACKLIGHT_CONTROL_FLICK_LEFT_SYSTEM = 11;
	int BACKLIGHT_CONTROL_FLICK_RIGHT_SYSTEM = 12;
	int BACKLIGHT_CONTROL_FLICK_BOTH_SYSTEM = 13;


	int APP_STARTUP_ACTION_LAST_BOOK = 0;
    int APP_STARTUP_ACTION_ROOT = 1;
    int APP_STARTUP_ACTION_RECENT_BOOKS = 2;
    int APP_STARTUP_ACTION_LAST_BOOK_FOLDER = 3;
    
    int VIEWER_STATUS_NONE = 0;
    int VIEWER_STATUS_TOP = 1;
    int VIEWER_STATUS_BOTTOM = 2;

	int VIEWER_STATUS_PAGE_HEADER = 3;
	int VIEWER_STATUS_PAGE_FOOTER = 4;
    int VIEWER_STATUS_PAGE = 5;
	int VIEWER_STATUS_PAGE_2LINES_HEADER = 6;
	int VIEWER_STATUS_PAGE_2LINES_FOOTER = 7;

	int VIEWER_TOOLBAR_NONE = 0;
    int VIEWER_TOOLBAR_TOP = 1;
    int VIEWER_TOOLBAR_BOTTOM = 2;
    int VIEWER_TOOLBAR_LEFT = 3;
    int VIEWER_TOOLBAR_RIGHT = 4;
    int VIEWER_TOOLBAR_SHORT_SIDE = 5;
    int VIEWER_TOOLBAR_LONG_SIDE = 6;

    int VIEWER_TOOLBAR_100 = 0;
    int VIEWER_TOOLBAR_100_gray = 1;
    int VIEWER_TOOLBAR_100_inv = 2;
    int VIEWER_TOOLBAR_75 = 3;
    int VIEWER_TOOLBAR_75_gray = 4;
    int VIEWER_TOOLBAR_75_inv = 5;
    int VIEWER_TOOLBAR_50 = 6;
    int VIEWER_TOOLBAR_50_gray = 7;
    int VIEWER_TOOLBAR_50_inv = 8;

	int CLOUD_SYNC_VARIANT_DISABLED = 0;
	int CLOUD_SYNC_VARIANT_FILESYSTEM = 1;
	int CLOUD_SYNC_VARIANT_YANDEX = 2;

	enum Lang {
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
    	
    	Lang(String code, int nameResId, int helpFileResId) {
    		this.code = code;
    		this.nameId = nameResId;
    		this.helpFileResId = helpFileResId;
    	}
    	public final String code;
    	public final int nameId;
    	public final int helpFileResId;
    };
    
    
	int MAX_PROFILES = 6;

	// settings which depend on profile
	String[] PROFILE_SETTINGS = {
	    "background.*",
	    PROP_NIGHT_MODE,
	    "font.*",
	    "crengine.page.*",
	    PROP_FONT_SIZE,
		PROP_FONT_SIZE_USER_DIC,
	    PROP_FALLBACK_FONT_FACES,
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
		"app.eink.*",
	    PROP_APP_DICTIONARY,
        PROP_APP_DICTIONARY_2,
		PROP_APP_DICTIONARY_3,
		PROP_APP_DICTIONARY_4,
		PROP_APP_DICTIONARY_5,
		PROP_APP_DICTIONARY_6,
		PROP_APP_DICTIONARY_7,
		PROP_APP_DICTIONARY_8,
		PROP_APP_DICTIONARY_9,
		PROP_APP_DICTIONARY_10,
		PROP_APP_SHOW_USER_DIC_PANEL,
		PROP_APP_SHOW_USER_DIC_CONTENT,
        PROP_APP_DICT_WORD_CORRECTION,
		PROP_APP_DICT_DONT_SAVE_IF_MORE,
		PROP_APP_DICT_AUTO_SPEAK,
		PROP_INSPECTOR_MODE_NO_DIC_HISTORY,
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
		PROP_APP_BOOKMARK_ACTION_SEND_TO,
		PROP_APP_BOOKMARK_ACTION_SEND_TO_MOD,
		PROP_APP_SELECTION2_ACTION,
		PROP_APP_SELECTION2_ACTION_LONG,
		PROP_APP_MULTI_SELECTION2_ACTION,
		PROP_APP_SELECTION3_ACTION,
		PROP_APP_SELECTION3_ACTION_LONG,
		PROP_APP_MULTI_SELECTION3_ACTION,
		PROP_APP_SELECTION_PERSIST,
	    PROP_APP_HIGHLIGHT_BOOKMARKS + "*",
		PROP_APP_HIGHLIGHT_USER_DIC,
	    PROP_HIGHLIGHT_SELECTION_COLOR + "*",
	    PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT + "*",
	    PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION + "*",

      "viewer.*",
	    PROP_APP_VIEW_AUTOSCROLL_SPEED,
		PROP_APP_VIEW_AUTOSCROLL_SIMPLE_SPEED,
	    PROP_APP_VIEW_AUTOSCROLL_TYPE,
		PROP_APP_VIEW_AUTOSCROLL_SHOW_SPEED,
		PROP_APP_VIEW_AUTOSCROLL_SHOW_PROGRESS,
	    	    
      "app.key.*",
	    "app.tapzone.*",
	    PROP_APP_DOUBLE_TAP_SELECTION,
	    "app.touch.*",

	    "app.ui.theme*",
        PROP_APP_ICONS_IS_CUSTOM_COLOR,
        PROP_APP_ICONS_CUSTOM_COLOR,
		PROP_GLOBAL_MARGIN,

		PROP_APP_HIDE_STATE_DIALOGS,
	    PROP_APP_HIDE_CSS_WARNING,
		PROP_APP_DISABLE_SAFE_MODE,
		PROP_APP_USE_SIMPLE_FONT_SELECT_DIALOG,

		PROP_APP_USE_EINK_FRONTLIGHT,

		PROP_APP_QUICK_TRANSLATION_DIRS,
		PROP_APP_ONLINE_OFFLINE_DICS,
		PROP_APP_OFFLINE_DICS,

		PROP_APP_TTS_SENTENCE_PAUSE,
		PROP_APP_TTS_AUTO_START

	};

	// settings which will not be saved
	String[] NOTSAVE_SETTINGS = {
		PROP_APP_RESTORE_SETTINGS + "*",
		PROP_APP_MIGRATE_SETTINGS,
		PROP_APP_INIT,
		PROP_CLOUD_YND_SETTINGS,
		PROP_APP_FILE_BROWSER_AUTHOR_ALIASES_LOAD,
		PROP_APP_TRANSLATE_DIR,
		PROP_APP_OFFLINE_DICS,
		PROP_CLOUD_YND_TRANSLATE_OPTIONS,
		PROP_CLOUD_LINGVO_OPTIONS,
		PROP_CLOUD_DEEPL_OPTIONS,
		PROP_CLOUD_YND_SETTINGS,
		PROP_CLOUD_DBX_SETTINGS,
		PROP_CLOUD_LITRES_SETTINGS
	};

	static boolean isSettingBelongToProfile(String settingName) {
		ArrayList<String> props = new ArrayList<String>();
		boolean found = false;
		for (String pattern : Settings.PROFILE_SETTINGS) {
			if (pattern.endsWith("*")) {
				if (settingName.startsWith(pattern.substring(0, pattern.length()-1))) {
					found = true;
					break;
				}
			} else if (pattern.equalsIgnoreCase(settingName)) {
				found = true;
				break;
			} else if (settingName.startsWith("styles.")) {
				found = true;
				break;
			}
		}
		return found;
	}


}