
# Metadata for deploy script

PN="fribidi"
PV="1.0.10"
# package revision: when patchset is changed (but not version), increase it
# when version changed, reset to "1".
REV="1"
SRCFILE="${PN}-${PV}.tar.xz"
SHA512=""

URL="https://github.com/fribidi/fribidi/releases/download/v${PV}/${SRCFILE}"

SOURCESDIR="${PN}-${PV}"

PATCHES="01-cmake-static.patch
		9e78abb7ead0781a6160be2e0d27d212ccd6f438.patch
		a11d8b3942546906b4a74d2061f50e05e4d33f18.patch"
