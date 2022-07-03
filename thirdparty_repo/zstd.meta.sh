
# Metadata for deploy script

PN="zstd"
#PV="1.5.2"
PV="1.5.0"
# package revision: when patchset is changed (but not version), increase it
# when version changed, reset to "1".
REV="1"
SRCFILE="${PN}-${PV}.tar.gz"
SHA512=""

URL="https://github.com/facebook/zstd/releases/download/v${PV}/${SRCFILE}"

SOURCESDIR="${PN}-${PV}"

PATCHES="01-disable-install.patch"
