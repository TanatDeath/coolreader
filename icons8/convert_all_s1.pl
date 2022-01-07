#!/usr/bin/perl -w

$TARGET_DIR = "../android/res/";
$TEMP_DIR = "../icons_temp/";

#                      dpi: 120       160       240        320         480            640
my %ic_smaller_sizes  = (ldpi=>20, mdpi=>24, hdpi=>32, xhdpi=>48, xxhdpi=>64,  xxxhdpi=>96);
my %ic_actions_sizes  = (ldpi=>24, mdpi=>32, hdpi=>48, xhdpi=>64, xxhdpi=>96,  xxxhdpi=>128);
my %ic_menu_sizes     = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);
my %ic_launcher_sizes = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);
my %ic_bigicons_sizes = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);

my %ic_smaller_list=(
        'icons8_minus_minus_small.svg' => 'icons8_minus_minus_small.png',
        'icons8_minus_small.svg' => 'icons8_minus_small.png',
        'icons8_plus_plus_small.svg' => 'icons8_plus_plus_small.png',
        'icons8_plus_small.svg' => 'icons8_plus_small.png',
        'icons8_drop_up_no_frame.svg' => 'icons8_drop_up_no_frame_small.png',
        'icons8_unchecked_checkbox.svg' => 'icons8_unchecked_checkbox.png',
        'icons8_checked_checkbox.svg' => 'icons8_checked_checkbox.png',
        'icons8_check_no_frame.svg' => 'icons8_check_no_frame.png',
        'icons8_delete.svg' => 'icons8_delete.png',
        'icons8_type_filled.svg' => 'icons8_type_filled.png',
        'icons8_css.svg' => 'icons8_css.png',
        'icons8_page.svg' => 'icons8_page.png',
        'icons8_settings.svg' => 'icons8_settings.png',
        'icons8_cursor.svg' => 'icons8_cursor.png',
        'icons8_save_small.svg' => 'icons8_save_small.png',
        'icons8_menu.svg' => 'icons8_menu_small.png',
        'icons8_goback.svg' => 'icons8_goback.png',
        'icons8_down_small.svg' => 'icons8_down_small.png',
        'icons8_up_small.svg' => 'icons8_up_small.png'
);

my %ic_actions_list=(
        'icons8_hide.svg' => 'icons8_hide.png',
        'icons8_library.svg' => 'icons8_library.png',
        'icons8_bookmark_plus_q.svg' => 'icons8_bookmark_plus_q.png',
        'icons8_two_fingers.svg' => 'icons8_two_fingers.png',
        'icons8_select_all.svg' => 'icons8_select_all.png',
        'cr3_option_text_multilang.svg' => 'cr3_option_text_multilang.png',
        'icons8_skim.svg' => 'icons8_skim.png',
        'icons8_combo.svg' => 'icons8_combo.png',
        'icons8_super_combo.svg' => 'icons8_super_combo.png',
        'icons8_web_search.svg' => 'icons8_web_search.png',
        'icons8_night_vision.svg' => 'icons8_night_vision.png',
        'icons8_sun_auto.svg' => 'icons8_sun_auto.png',
        'icons8_sun_warm.svg' => 'icons8_sun_warm.png',
        'icons8_sun_cold.svg' => 'icons8_sun_cold.png',
        'icons8_page_animation_speed.svg' => 'icons8_page_animation_speed.png',
        'icons8_page_animation.svg' => 'icons8_page_animation.png',
        'icons8_opds.svg' => 'icons8_opds.png',
        'icons8_calibre.svg' => 'icons8_calibre.png',
        'litres_en_logo_2lines.svg' => 'icons8_litres_en_logo_2lines_big.png',
        'onyx_dictionary.svg' => 'onyx_dictionary.png',
        'icons8_document_lang.svg' => 'icons8_document_lang.png',
        'icons8_log.svg' => 'icons8_log.png',
        'icons8_no_dialogs.svg' => 'icons8_no_dialogs.png',
        'icons8_no_safe_mode.svg' => 'icons8_no_safe_mode.png',
        'icons8_no_questions.svg' => 'icons8_no_questions.png',
        'icons8_scroll.svg' => 'icons8_scroll.png',
        'icons8_toggle_page_scroll.svg' => 'icons8_toggle_page_scroll.png',
        'icons8_toolbar_background.svg' => 'icons8_toolbar_background.png',
        'icons8_transp_buttons.svg' => 'icons8_transp_buttons.png',
        'icons8_tts_engine.svg' => 'icons8_tts_engine.png',
        'icons8_tts_lang.svg' => 'icons8_tts_lang.png',
        'icons8_two_fingers.svg' => 'icons8_two_fingers.png',
        'icons8_voice.svg' => 'icons8_voice.png',
        'icons8_system_lang.svg' => 'icons8_system_lang.png',
        'icons8_alligator.svg' => 'icons8_alligator.png',
        'icons8_anon.svg' => 'icons8_anon.png',
        'icons8_anon_load.svg' => 'icons8_anon_load.png',
        'icons8_backlight_swipe_sensitivity.svg' => 'icons8_backlight_swipe_sensitivity.png',
        'icons8_disable_trackball.svg' => 'icons8_disable_trackball.png',
        'icons8_document_4pages.svg' => 'icons8_document_4pages.png',
        'icons8_double_click_tap.svg' => 'icons8_double_click_tap.png',
        'icons8_double_click_tap_interval.svg' => 'icons8_double_click_tap_interval.png',
        'icons8_ext_toolbar.svg' => 'icons8_ext_toolbar.png',
        'icons8_font_select_dialog.svg' => 'icons8_font_select_dialog.png',
        'icons8_gesture_sensivity.svg' => 'icons8_gesture_sensivity.png',
        'icons8_highlight_tap_zone.svg' => 'icons8_highlight_tap_zone.png',
        'icons8_l_h.svg' => 'icons8_l_h.png',
        'icons8_ligature.svg' => 'icons8_ligature.png',
        'icons8_moving_sensor.svg' => 'icons8_moving_sensor.png',
        'icons8_physics.svg' => 'icons8_physics.png',
        'icons8_position_to_disk_interval.svg' => 'icons8_position_to_disk_interval.png',
        'icons8_position_to_gd_interval.svg' => 'icons8_position_to_gd_interval.png',
        'icons8_prevent_accidental_tap_interval.svg' => 'icons8_prevent_accidental_tap_interval.png',
        'icons8_quick_transl_dir.svg' => 'icons8_quick_transl_dir.png',
        'icons8_resolution.svg' => 'icons8_resolution.png',
        'icons8_send_to_action.svg' => 'icons8_send_to_action.png',
        'icons8_send_to_action_more.svg' => 'icons8_send_to_action_more.png',
        'icons8_speaker_koef.svg' => 'icons8_speaker_koef.png',
        'icons8_user_dic_panel.svg' => 'icons8_user_dic_panel.png',
        'icons8_group.svg' => 'icons8_group.png',
        'icons8_group2.svg' => 'icons8_group2.png',
        'icons8_airplane_mode_on.svg' => 'icons8_airplane_mode_on.png',
        'icons8_delete_database.svg' => 'icons8_delete_database.png',
        'icons8_book_scan.svg' => 'icons8_book_scan.png',
        'icons8_book_long_tap.svg' => 'icons8_book_long_tap.png',
        'icons8_book_tap.svg' => 'icons8_book_tap.png',
        'icons8_bookmark_simple_fast_add.svg' => 'icons8_bookmark_simple_fast_add.png',
        'icons8_bookmark_simple_add.svg' => 'icons8_bookmark_simple_add.png',
        'icons8_help.svg' => 'icons8_help.png',
        'icons8_dic_list.svg' => 'icons8_dic_list.png',
        'icons8_db_stats.svg' => 'icons8_db_stats.png',
        'icons8_eink_snow.svg' => 'icons8_eink_snow.png',
        'icons8_eink_sett.svg' => 'icons8_eink_sett.png',
        'icons8_font_scale.svg' => 'icons8_font_scale.png',
        'icons8_play_lock.svg' => 'icons8_play_lock.png'
);

my %ic_menu_list=(
        'icons8_css.svg_' => 'icons8_css.png_'
);

my %ic_launcher_list=(
);

my %ic_bigicons_list=(
);

my ($srcfile, $dstfile);
my ($dpi, $size);
my $folder;
my $resfile;
my $cmd;
my $ret;

# smaller icons
while (($srcfile, $dstfile) = each(%ic_smaller_list))
{
	while (($dpi, $size) = each(%ic_smaller_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# action icons
while (($srcfile, $dstfile) = each(%ic_actions_list))
{
	while (($dpi, $size) = each(%ic_actions_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# menu icons
while (($srcfile, $dstfile) = each(%ic_menu_list))
{
	while (($dpi, $size) = each(%ic_menu_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

		}
	}
}

# launcher icons
while (($srcfile, $dstfile) = each(%ic_launcher_list))
{
	while (($dpi, $size) = each(%ic_launcher_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

		}
	}
}

# bigicons
while (($srcfile, $dstfile) = each(%ic_bigicons_list))
{
	while (($dpi, $size) = each(%ic_bigicons_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z --export-filename ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

		}
	}
}