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
        'icons8_cancel.svg' => 'icons8_cancel_small.png',
        'icons8_ask_question.svg' => 'icons8_ask_question.png',
        'icons8_back_small.svg' => 'icons8_back_small.png',
        'icons8_forward_small.svg' => 'icons8_forward_small.png',
        'icons8_drop_down_no_frame.svg' => 'icons8_drop_down_no_frame_small.png',
        'icons8_drop_up_no_frame.svg' => 'icons8_drop_up_no_frame_small.png',
        'icons8_scroll_down.svg' => 'icons8_scroll_down_small.png',
        'icons8_scroll_up.svg' => 'icons8_scroll_up_small.png',
        'icons8_freq_down.svg' => 'icons8_freq_down.png',
        'icons8_freq_up.svg' => 'icons8_freq_up.png',
        'icons8_rate_down.svg' => 'icons8_rate_down.png',
        'icons8_rate_up.svg' => 'icons8_rate_up.png',
        'icons8_volume_down.svg' => 'icons8_volume_down.png',
        'icons8_volume_up.svg' => 'icons8_volume_up.png',
        'icons8_star.svg' => 'icons8_fav_star.png',
        'icons8_star_filled.svg' => 'icons8_fav_star_filled.png'
);

my %ic_actions_list=(
        'icons8_play_ell.svg' => 'icons8_play_ell.png'
);

my %ic_menu_list=(
	'icons8_css.svg' => 'icons8_css.png',
        'icons8_page.svg' => 'icons8_page.png',
        'icons8_one_finger.svg' => 'icons8_one_finger.png',
        'icons8_settings.svg' => 'icons8_settings.png',
        'icons8_cursor.svg' => 'icons8_cursor.png',
        'icons8_type_filled.svg' => 'icons8_type_filled.png',
        'icons8_book.svg' => 'icons8_book_2.png',
        'icons8_happy.svg' => 'icons8_happy.png',
        'icons8_link.svg' => 'icons8_link.png',
        'icons8_folder.svg' => 'icons8_folder_2.png',
        'icons8_star.svg' => 'icons8_star.png'

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

sub makeTemp {
	$src_file = $_[0];
	$resfile = "${TEMP_DIR}/drk_${src_file}";
	open(IN, '<'.$src_file) or die $src_file.':'.$!;
	open(OUT, '>'.$resfile) or die $resfile.':'.$!;
	while(<IN>)
	{
	    $_ =~ s/808080/202020/g;
	    print OUT $_;
	}
	close(IN);
	close(OUT);	

	$resfile = "${TEMP_DIR}/lgt_${src_file}";
	open(IN, '<'.$src_file) or die $src_file.':'.$!;
	open(OUT, '>'.$resfile) or die $resfile.':'.$!;
	while(<IN>)
	{
	    $_ =~ s/808080/EEEEEE/g;
	    print OUT $_;
	}
	close(IN);
	close(OUT);	

	$resfile = "${TEMP_DIR}/wdrk_${src_file}";
	open(IN, '<'.$src_file) or die $src_file.':'.$!;
	open(OUT, '>'.$resfile) or die $resfile.':'.$!;
	while(<IN>)
	{
	    $_ =~ s/808080/65441a/g;
	    print OUT $_;
	}
	close(IN);
	close(OUT);	

	$resfile = "${TEMP_DIR}/wlgt_${src_file}";
	open(IN, '<'.$src_file) or die $src_file.':'.$!;
	open(OUT, '>'.$resfile) or die $resfile.':'.$!;
	while(<IN>)
	{
	    $_ =~ s/808080/efdbc2/g;
	    print OUT $_;
	}
	close(IN);
	close(OUT);
}

# smaller icons
while (($srcfile, $dstfile) = each(%ic_smaller_list))
{
	makeTemp($srcfile);
	while (($dpi, $size) = each(%ic_smaller_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			#$resfile = "${folder}/${dstfile}";
			#$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			#print "$cmd\n";
			#$ret = system($cmd);
			#print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/drk_${src_file}";
			$resfile = "${folder}/drk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/lgt_${src_file}";
			$resfile = "${folder}/lgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wdrk_${src_file}";
			$resfile = "${folder}/wdrk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wlgt_${src_file}";
			$resfile = "${folder}/wlgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# action icons
while (($srcfile, $dstfile) = each(%ic_actions_list))
{
	makeTemp($srcfile);
	while (($dpi, $size) = each(%ic_actions_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			#$resfile = "${folder}/${dstfile}";
			#$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			#print "$cmd\n";
			#$ret = system($cmd);
			#print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/drk_${src_file}";
			$resfile = "${folder}/drk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/lgt_${src_file}";
			$resfile = "${folder}/lgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wdrk_${src_file}";
			$resfile = "${folder}/wdrk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wlgt_${src_file}";
			$resfile = "${folder}/wlgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# menu icons
while (($srcfile, $dstfile) = each(%ic_menu_list))
{
	makeTemp($srcfile);
	while (($dpi, $size) = each(%ic_menu_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			#$resfile = "${folder}/${dstfile}";
			#$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			#print "$cmd\n";
			#$ret = system($cmd);
			#print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/drk_${src_file}";
			$resfile = "${folder}/drk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/lgt_${src_file}";
			$resfile = "${folder}/lgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wdrk_${src_file}";
			$resfile = "${folder}/wdrk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wlgt_${src_file}";
			$resfile = "${folder}/wlgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# launcher icons
while (($srcfile, $dstfile) = each(%ic_launcher_list))
{
	makeTemp($srcfile);
	while (($dpi, $size) = each(%ic_launcher_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			#$resfile = "${folder}/${dstfile}";
			#$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			#print "$cmd\n";
			#$ret = system($cmd);
			#print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/drk_${src_file}";
			$resfile = "${folder}/drk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/lgt_${src_file}";
			$resfile = "${folder}/lgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wdrk_${src_file}";
			$resfile = "${folder}/wdrk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wlgt_${src_file}";
			$resfile = "${folder}/wlgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# bigicons
while (($srcfile, $dstfile) = each(%ic_bigicons_list))
{
	makeTemp($srcfile);
	while (($dpi, $size) = each(%ic_bigicons_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			#$resfile = "${folder}/${dstfile}";
			#$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			#print "$cmd\n";
			#$ret = system($cmd);
			#print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/drk_${src_file}";
			$resfile = "${folder}/drk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/lgt_${src_file}";
			$resfile = "${folder}/lgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wdrk_${src_file}";
			$resfile = "${folder}/wdrk_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;

			$srcfile_2 = "${TEMP_DIR}/wlgt_${src_file}";
			$resfile = "${folder}/wlgt_${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile_2}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}
