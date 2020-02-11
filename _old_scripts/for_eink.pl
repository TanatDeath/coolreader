#!/usr/bin/perl -w

$TARGET_DIR = "../android/res/";

my $cmd;
my $ret;

my %ic_drawable=(
        'drawable-hdpi' => 'hdpi',
        'drawable-ldpi' => 'ldpi',
        'drawable-mdpi' => 'mdpi',
        'drawable-xhdpi' => 'xhdpi',
        'drawable-xxhdpi' => 'xxhdpi',
        'drawable-xxxhdpi' => 'xxxhdpi'
);

while (($dstdir,$dstd) = each(%ic_drawable))
{
  $cmd = "/bin/cp -rf ${TARGET_DIR}${dstdir}/* ./res/${dstdir}";
  print "$cmd\n";
  $ret = system($cmd);
  $cmd = "rm ${TARGET_DIR}${dstdir}/lgt_*";
  print "$cmd\n";
  $ret = system($cmd);
  $cmd = "rm ${TARGET_DIR}${dstdir}/wdrk_*";
  print "$cmd\n";
  $ret = system($cmd);
  $cmd = "rm ${TARGET_DIR}${dstdir}/wlgt_*";
  print "$cmd\n";
  $ret = system($cmd);
  $cmd = "/bin/cp -rf ${TARGET_DIR}values-v4/styles.xml ./res/values-v4/styles_bk.xml";
  print "$cmd\n";
  $ret = system($cmd);
}

$src_file = "${TARGET_DIR}values-v4/styles.xml";
$resfile = "./res/values-v4/styles_eink.xml";
open(IN, '<'.$src_file) or die $src_file.':'.$!;
open(OUT, '>'.$resfile) or die $resfile.':'.$!;
while(<IN>)
{
  $_ =~ s/@drawable\/wdrk_/@drawable\/drk_/g;
  $_ =~ s/@drawable\/wlgt_/@drawable\/drk_/g;
  $_ =~ s/@drawable\/lgt_/@drawable\/drk_/g;
  print OUT $_;
}
close(IN);
close(OUT);
$cmd = "/bin/cp -rf ./res/values-v4/styles_eink.xml ${TARGET_DIR}values-v4/styles.xml";
print "$cmd\n";
$ret = system($cmd);