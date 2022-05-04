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
        'icons8_visible.svg' => 'icons8_visible.png',
        'icons8_tear_off_calendar.svg' => 'icons8_tear_off_calendar.png'
);

my %ic_actions_list=(
        'icons8_hide.svg' => 'icons8_hide.png',
        'icons8_brightness_down.svg' => 'icons8_brightness_down.png',
        'icons8_brightness_up.svg' => 'icons8_brightness_up.png',
        'icons8_brightness_warm_down.svg' => 'icons8_brightness_warm_down.png',
        'icons8_brightness_warm_up.svg' => 'icons8_brightness_warm_up.png',
        'icons8_offline_dics1.svg' => 'icons8_offline_dics1.png',
        'icons8_offline_dics2.svg' => 'icons8_offline_dics2.png',
        'icons8_google_web_search.svg' => 'icons8_google_web_search.png',
        'icons8_bookmarks_export.svg' => 'icons8_bookmarks_export.png',
        'icons8_save_ell.svg' => 'icons8_save_ell.png'
);

my %ic_menu_list=(
        'icons8_hide.svg_' => 'icons8_hide.png_'
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