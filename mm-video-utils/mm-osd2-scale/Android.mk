#
# Android meigcam HAL makefile for libhidapi/libSDL2/libSDL2_ttf
#
OLD_LOCAL_PATH := $(LOCAL_PATH)
LOCAL_PATH := $(call my-dir)

#-------------------------------------------------------------------
# libhidapi
#-------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_CPPFLAGS += -std=c++11
$(info =======$(LOCAL_PATH)=========)
LOCAL_SRC_FILES := sdl2/src/hidapi/android/hid.cpp

LOCAL_MODULE := libhidapi
LOCAL_SHARED_LIBRARIES := liblog
LOCAL_32_BIT_ONLY := true
include $(BUILD_SHARED_LIBRARY)


#-------------------------------------------------------------------
# libSDL2
#-------------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE := libSDL2
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES := $(LOCAL_PATH)/sdl2/include

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES)

LOCAL_SRC_FILES := \
        $(subst $(LOCAL_PATH)/,, \
        $(wildcard $(LOCAL_PATH)/sdl2/src/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/audio/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/audio/android/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/audio/dummy/*.c) \
        $(LOCAL_PATH)/sdl2/src/atomic/SDL_atomic.c.arm \
        $(LOCAL_PATH)/sdl2/src/atomic/SDL_spinlock.c.arm \
        $(wildcard $(LOCAL_PATH)/sdl2/src/core/android/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/cpuinfo/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/dynapi/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/events/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/file/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/haptic/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/haptic/android/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/joystick/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/joystick/android/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/joystick/hidapi/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/loadso/dlopen/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/power/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/power/android/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/filesystem/android/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/sensor/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/sensor/android/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/render/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/render/*/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/stdlib/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/thread/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/thread/pthread/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/timer/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/timer/unix/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/video/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/video/android/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/video/yuv2rgb/*.c) \
        $(wildcard $(LOCAL_PATH)/sdl2/src/test/*.c))

LOCAL_SHARED_LIBRARIES := libhidapi

LOCAL_32_BIT_ONLY := true
LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES
LOCAL_SHARED_LIBRARIES += libdl \
          libGLESv1_CM \
          libGLESv2 \
          liblog \
          libandroid

include $(BUILD_SHARED_LIBRARY)


#-------------------------------------------------------------------
# libSDL2_ttf
#-------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := libSDL2_ttf

SDL2_LIBRARY_PATH := SDL2_ttf/external/sdl2
FREETYPE_LIBRARY_PATH := SDL2_ttf/external/freetype-2.4.12

LOCAL_C_INCLUDES := $(LOCAL_PATH)/SDL2_ttf

LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SDL2_LIBRARY_PATH)/include
LOCAL_SRC_FILES := SDL2_ttf/SDL_ttf.c \

ifneq ($(FREETYPE_LIBRARY_PATH),)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(FREETYPE_LIBRARY_PATH)/include
LOCAL_CFLAGS += -DFT2_BUILD_LIBRARY
LOCAL_SRC_FILES += \
        $(FREETYPE_LIBRARY_PATH)/src/autofit/autofit.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftbase.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftbbox.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftbdf.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftbitmap.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftcid.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftdebug.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftfstype.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftgasp.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftglyph.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftgxval.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftinit.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftlcdfil.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftmm.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftotval.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftpatent.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftpfr.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftstroke.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftsynth.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftsystem.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/fttype1.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftwinfnt.c \
        $(FREETYPE_LIBRARY_PATH)/src/base/ftxf86.c \
        $(FREETYPE_LIBRARY_PATH)/src/bdf/bdf.c \
        $(FREETYPE_LIBRARY_PATH)/src/bzip2/ftbzip2.c \
        $(FREETYPE_LIBRARY_PATH)/src/cache/ftcache.c \
        $(FREETYPE_LIBRARY_PATH)/src/cff/cff.c \
        $(FREETYPE_LIBRARY_PATH)/src/cid/type1cid.c \
        $(FREETYPE_LIBRARY_PATH)/src/gzip/ftgzip.c \
        $(FREETYPE_LIBRARY_PATH)/src/lzw/ftlzw.c \
        $(FREETYPE_LIBRARY_PATH)/src/pcf/pcf.c \
        $(FREETYPE_LIBRARY_PATH)/src/pfr/pfr.c \
        $(FREETYPE_LIBRARY_PATH)/src/psaux/psaux.c \
        $(FREETYPE_LIBRARY_PATH)/src/pshinter/pshinter.c \
        $(FREETYPE_LIBRARY_PATH)/src/psnames/psmodule.c \
        $(FREETYPE_LIBRARY_PATH)/src/raster/raster.c \
        $(FREETYPE_LIBRARY_PATH)/src/sfnt/sfnt.c \
        $(FREETYPE_LIBRARY_PATH)/src/smooth/smooth.c \
        $(FREETYPE_LIBRARY_PATH)/src/truetype/truetype.c \
        $(FREETYPE_LIBRARY_PATH)/src/type1/type1.c \
        $(FREETYPE_LIBRARY_PATH)/src/type42/type42.c \
        $(FREETYPE_LIBRARY_PATH)/src/winfonts/winfnt.c
endif

LOCAL_SHARED_LIBRARIES := libSDL2
LOCAL_EXPORT_C_INCLUDES += $(LOCAL_C_INCLUDES)
LOCAL_32_BIT_ONLY := true

include $(BUILD_SHARED_LIBRARY)


#-------------------------------------------------------------------
# libmm-qosd
#-------------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE := libmm-qosd
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += $(LOCAL_PATH)/osd/inc
LOCAL_C_INCLUDES += $(LOCAL_PATH)/osd/inc/sdl2
LOCAL_C_INCLUDES += $(LOCAL_PATH)/osd/inc/sdl2_ttf
LOCAL_C_INCLUDES += $(TOP)/vendor/meig/MeiGService/src/common

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES)

LOCAL_SRC_FILES := \
        osd/src/mm_qcamera_osd.cpp \
        osd/src/OsdCtl.cpp \
        osd/src/OsdObj.cpp \
        osd/src/bmp2yuv.cpp \
        osd/src/ttf2bmp.c \
        osd/src/yuvosd.c

LOCAL_SHARED_LIBRARIES := libdl libgui libui libutils libqdMetaData libxml2 liblog libSDL2_ttf libSDL2 libhidapi libjpeg

LOCAL_32_BIT_ONLY := true
LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES

include $(BUILD_SHARED_LIBRARY)

#-------------------------------------------------------------------
# libmm-qscale
#-------------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE := libmm-qscale
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../mm-qthread-pool
LOCAL_C_INCLUDES += $(LOCAL_PATH)/scale/inc
LOCAL_C_INCLUDES += $(LOCAL_PATH)/scale/inc/libjpeg
LOCAL_C_INCLUDES += $(TOP)/external/libyuv/files/include

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES)

LOCAL_SRC_FILES := \
       scale/src/mm_qcamera_scale.cpp \
       scale/src/ScaleModule.cpp

LOCAL_SHARED_LIBRARIES := libmm-qthreadpool libdl libgui libui libutils libqdMetaData libxml2 liblog libSDL2_ttf libSDL2 libhidapi libjpeg libcutils
LOCAL_STATIC_LIBRARIES := \
    libyuv_static
#-------------------------------------------------------------------
#            Add by huangfusheng 2020-5-22
#-------------------------------------------------------------------
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../../meig/external/ffmpeg-4.0.2
LOCAL_SHARED_LIBRARIES += libavutil

LOCAL_32_BIT_ONLY := true
LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES

include $(BUILD_SHARED_LIBRARY)
LOCAL_PATH := $(OLD_LOCAL_PATH)
