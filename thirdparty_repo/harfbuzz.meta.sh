
# Metadata for deploy script

PN="harfbuzz"
#PV="4.2.1"
PV="2.8.2"
# package revision: when patchset is changed (but not version), increase it
# when version changed, reset to "1".
REV="1"
SRCFILE="${PN}-${PV}.tar.xz"
SHA512=""

URL="https://github.com/harfbuzz/harfbuzz/releases/download/${PV}/${SRCFILE}"

SOURCESDIR="${PN}-${PV}"

PATCHES=
