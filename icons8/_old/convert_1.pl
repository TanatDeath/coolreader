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
        'icons8_cancel.svg' => 'icons8_cancel_small.png'
);

my %ic_actions_list=(
        'icons8_yandex_logo.svg' => 'icons8_yandex_logo.png'
);

my %ic_menu_list=(
	'icons8_send_by_email.svg' => 'icons8_send_by_email.png'

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
