
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := cr3engine-3-2-X

# Generate CREngine blob with statically linked libjpeg, libpng, freetype, harfbuzz, fribidi, libunibreak, zstd, utf8proc, antiword, chmlib

CRFLAGS := -DLINUX=1 -D_LINUX=1 \
     -DFT2_BUILD_LIBRARY=1 -DFT_CONFIG_MODULES_H=\<android/config/ftmodule.h\> -DFT_CONFIG_OPTIONS_H=\<android/config/ftoption.h\> \
     -DDOC_DATA_COMPRESSION_LEVEL=1 -DDOC_BUFFER_SIZE=0x1000000 \
     -DENABLE_CACHE_FILE_CONTENTS_VALIDATION=1 \
     -DLDOM_USE_OWN_MEM_MAN=0 \
     -DCR3_ANTIWORD_PATCH=1 -DENABLE_ANTIWORD=1 \
     -DLVLONG_FILE_SUPPORT=1 \
     -DMAX_IMAGE_SCALE_MUL=2 \
     -DUSE_NANOSVG=1 \
     -DBUNDLED_FRIBIDI=1 \
     -DKO_LIBUNIBREAK_PATCH=1 \
     -DUSE_UTF8PROC=1

CR3_ROOT := $(LOCAL_PATH)/../..

include $(CR3_ROOT)/thirdparty_repo/repo_srcdirs.mk

LOCAL_C_INCLUDES := \
    $(CR3_ROOT)/crengine/include \
    $(CR3_ROOT)/thirdparty/$(REPO_LIBPNG_SRCDIR) \
    $(CR3_ROOT)/thirdparty/$(REPO_FREETYPE_SRCDIR)/include \
    $(CR3_ROOT)/thirdparty/$(REPO_FREETYPE_SRCDIR) \
    $(CR3_ROOT)/thirdparty/$(REPO_HARFBUZZ_SRCDIR)/src \
    $(CR3_ROOT)/thirdparty/$(REPO_LIBJPEG_SRCDIR) \
    $(CR3_ROOT)/thirdparty_unman/antiword \
    $(CR3_ROOT)/thirdparty_unman/chmlib/src \
    $(CR3_ROOT)/thirdparty_unman/nanosvg/src \
    $(CR3_ROOT)/thirdparty/$(REPO_FRIBIDI_SRCDIR)/lib \
    $(CR3_ROOT)/thirdparty/$(REPO_LIBUNIBREAK_SRCDIR)/src \
    $(CR3_ROOT)/thirdparty/$(REPO_ZSTD_SRCDIR)/lib \
    $(CR3_ROOT)/thirdparty/$(REPO_UTF8PROC_SRCDIR) \
    $(CR3_ROOT)/android/app/thirdparty_libs/freetype \
    $(CR3_ROOT)/android/app/thirdparty_libs/fribidi/lib \
    $(CR3_ROOT)/android/app/thirdparty_libs/libpng/lib


LOCAL_CFLAGS += $(CRFLAGS)

LOCAL_CFLAGS += -Wall -Wno-unused-variable -Wno-sign-compare -Wno-write-strings -Wno-main -Wno-unused-function

LOCAL_CFLAGS += -funwind-tables -Wl,--no-merge-exidx-entries

LOCAL_CFLAGS += -g -O1 -fexceptions -flto

CRENGINE_SRC_FILES := \
    ../../crengine/src/cp_stats.cpp \
    ../../crengine/src/lvstring.cpp \
    ../../crengine/src/lvstring8collection.cpp \
    ../../crengine/src/lvstring32collection.cpp \
    ../../crengine/src/lvstring32hashedcollection.cpp \
    ../../crengine/src/crlog.cpp \
    ../../crengine/src/serialbuf.cpp \
    ../../crengine/src/props.cpp \
    ../../crengine/src/lstridmap.cpp \
    ../../crengine/src/rtfimp.cpp \
    ../../crengine/src/lvmemman.cpp \
    ../../crengine/src/lvstyles.cpp \
    ../../crengine/src/crtxtenc.cpp \
    ../../crengine/src/lvtinydom.cpp \
    ../../crengine/src/chmfmt.cpp \
    ../../crengine/src/epubfmt.cpp \
    ../../crengine/src/pdbfmt.cpp \
    ../../crengine/src/wordfmt.cpp \
    ../../crengine/src/lvopc.cpp \
    ../../crengine/src/docxfmt.cpp \
    ../../crengine/src/fb3fmt.cpp \
    ../../crengine/src/odtfmt.cpp \
    ../../crengine/src/odxutil.cpp \
    ../../crengine/src/lvstsheet.cpp \
    ../../crengine/src/txtselector.cpp \
    ../../crengine/src/crtest.cpp \
    ../../crengine/src/lvbmpbuf.cpp \
    ../../crengine/src/hyphman.cpp \
    ../../crengine/src/crskin.cpp \
    ../../crengine/src/lvdocview.cpp \
    ../../crengine/src/lvpagesplitter.cpp \
    ../../crengine/src/lvtextfm.cpp \
    ../../crengine/src/lvrend.cpp \
    ../../crengine/src/wolutil.cpp \
    ../../crengine/src/crconcurrent.cpp \
    ../../crengine/src/mathml.cpp \
    ../../crengine/src/hist.cpp \
    ../../crengine/src/xxhash.c \
    ../../crengine/src/textlang.cpp \
    ../../crengine/src/lvimg/lvcacheableobject.cpp \
    ../../crengine/src/lvimg/lvimagesource.cpp \
    ../../crengine/src/lvimg/crninepatchdecoder.cpp \
    ../../crengine/src/lvimg/lvjpegimagesource.cpp \
    ../../crengine/src/lvimg/lvpngimagesource.cpp \
    ../../crengine/src/lvimg/lvxpmimagesource.cpp \
    ../../crengine/src/lvimg/lvgifimagesource.cpp \
    ../../crengine/src/lvimg/lvgifframe.cpp \
    ../../crengine/src/lvimg/clzwdecoder.cpp \
    ../../crengine/src/lvimg/lvsvgimagesource.cpp \
    ../../crengine/src/lvimg/lvstretchimgsource.cpp \
    ../../crengine/src/lvimg/lvcolortransformimgsource.cpp \
    ../../crengine/src/lvimg/lvalphatransformimgsource.cpp \
    ../../crengine/src/lvimg/lvunpackedimgsource.cpp \
    ../../crengine/src/lvimg/lvdrawbufimgsource.cpp \
    ../../crengine/src/lvimg/lvimg.cpp \
    ../../crengine/src/lvfont/lvfnt.cpp \
    ../../crengine/src/lvfont/lvembeddedfont.cpp \
    ../../crengine/src/lvfont/lvfont.cpp \
    ../../crengine/src/lvfont/lvfntman.cpp \
    ../../crengine/src/lvfont/lvfontglyphcache.cpp \
    ../../crengine/src/lvfont/lvfontboldtransform.cpp \
    ../../crengine/src/lvfont/lvfontcache.cpp \
    ../../crengine/src/lvfont/lvfontdef.cpp \
    ../../crengine/src/lvfont/lvfreetypeface.cpp \
    ../../crengine/src/lvfont/lvfreetypefontman.cpp \
    ../../crengine/src/lvdrawbuf/lvbasedrawbuf.cpp \
    ../../crengine/src/lvdrawbuf/lvgraydrawbuf.cpp \
    ../../crengine/src/lvdrawbuf/lvcolordrawbuf.cpp \
    ../../crengine/src/lvdrawbuf/lvinkmeasurementdrawbuf.cpp \
    ../../crengine/src/lvdrawbuf/lvimagescaleddrawcallback.cpp \
    ../../crengine/src/lvdrawbuf/lvdrawbuf_utils.cpp \
    ../../crengine/src/lvstream/lvdefstreambuffer.cpp \
    ../../crengine/src/lvstream/lvfilemappedstream.cpp \
    ../../crengine/src/lvstream/lvnamedstream.cpp \
    ../../crengine/src/lvstream/lvfilestream.cpp \
    ../../crengine/src/lvstream/lvdirectorycontainer.cpp \
    ../../crengine/src/lvstream/lvcachedstream.cpp \
    ../../crengine/src/lvstream/lvzipdecodestream.cpp \
    ../../crengine/src/lvstream/lvziparc.cpp \
    ../../crengine/src/lvstream/lvrararc.cpp \
    ../../crengine/src/lvstream/lvmemorystream.cpp \
    ../../crengine/src/lvstream/lvtcrstream.cpp \
    ../../crengine/src/lvstream/lvblockwritestream.cpp \
    ../../crengine/src/lvstream/lvstream.cpp \
    ../../crengine/src/lvstream/lvbase64stream.cpp \
    ../../crengine/src/lvstream/lvstreamutils.cpp \
    ../../crengine/src/lvxml/lvfileparserbase.cpp \
    ../../crengine/src/lvxml/lvtextfilebase.cpp \
    ../../crengine/src/lvxml/lvtextparser.cpp \
    ../../crengine/src/lvxml/lvtextlinequeue.cpp \
    ../../crengine/src/lvxml/pmltextimport.cpp \
    ../../crengine/src/lvxml/lvtextrobustparser.cpp \
    ../../crengine/src/lvxml/lvtextbookmarkparser.cpp \
    ../../crengine/src/lvxml/lvxmlparser.cpp \
    ../../crengine/src/lvxml/lvhtmlparser.cpp \
    ../../crengine/src/lvxml/fb2coverpageparsercallback.cpp \
    ../../crengine/src/lvxml/lvxmlutils.cpp \
    ../../crengine/src/locale_data/fc-lang-data.c \
    ../../crengine/src/locale_data/crlocaledata.cpp
#    ../../crengine/src/cri18n.cpp
#    ../../crengine/src/crgui.cpp \

JNI_SRC_FILES := \
    cr3engine.cpp \
    cr3java.cpp \
    docview.cpp

COFFEECATCH_SRC_FILES := \
    coffeecatch/coffeecatch.c \
    coffeecatch/coffeejni.c

LOCAL_SRC_FILES := \
    $(JNI_SRC_FILES) \
    $(CRENGINE_SRC_FILES)

ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_SRC_FILES += \
    $(COFFEECATCH_SRC_FILES)
endif
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_SRC_FILES += \
    $(COFFEECATCH_SRC_FILES)
endif
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_SRC_FILES += \
    $(COFFEECATCH_SRC_FILES)
endif
ifeq ($(TARGET_ARCH_ABI),mips)
LOCAL_SRC_FILES += \
    $(COFFEECATCH_SRC_FILES)
endif
ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_SRC_FILES += \
    $(COFFEECATCH_SRC_FILES)
endif

LOCAL_STATIC_LIBRARIES := \
    local_png \
    local_jpeg \
    local_freetype \
    local_harfbuzz \
    local_chmlib \
    local_antiword \
    local_fribidi \
    local_libunibreak \
    local_qimagescale \
    local_zstd \
    local_utf8proc

LOCAL_LDLIBS    := -lm -llog -lz -ldl -flto
# 
#LOCAL_LDLIBS    += -Wl,-Map=cr3engine.map
#-ljnigraphics

include $(BUILD_SHARED_LIBRARY)

MY_LOCAL_PATH := $(LOCAL_PATH)

include $(MY_LOCAL_PATH)/../app/thirdparty_libs/Android.mk
