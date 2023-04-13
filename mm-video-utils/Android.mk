#
# Android meigcam HAL makefile for meigcam.default.so
#

LOCAL_PATH:= $(call my-dir)

#-------------------------------------------------------------------
# meigcam.default
#-------------------------------------------------------------------
include $(CLEAR_VARS)
ifeq ($(call is-board-platform-in-list, $(MSM_VIDC_TARGET_LIST)),true)

# Forcing O0 flag to remove all compiler optimizations as a workaround for
# stack corruptions seen on 64 bit.
vtest-def := \
    -g -O0 \
    -D_POSIX_SOURCE -DPOSIX_C_SOURCE=199506L \
    -D_XOPEN_SOURCE=600 \
    -D_ANDROID_ \
    -D_ANDROID_ -DQCAMERA_REDEFINE_LOG \
    -DSYSTEM_HEADER_PREFIX=sys \
    -DCAMERA_ION_HEAP_ID=ION_IOMMU_HEAP_ID \
    -Wno-deprecated-declarations \
    -Wno-mismatched-tags \
    -fno-stack-protector \
    -DAMSS_VERSION=$(AMSS_VERSION) \
    $(mmcamera_debug_defines) \
    $(mmcamera_debug_cflags) \
    $(USE_SERVER_TREE) \
    $(barcode-def)


vtest-inc        := $(TARGET_OUT_HEADERS)/mm-core/omxcore
vtest-inc        += $(TOP)/external/connectivity/stlport/stlport/
vtest-inc        += $(TOP)/system/core/include/system/
vtest-inc        += $(TOP)/system/media/camera/include
vtest-inc        += $(TOP)/frameworks/wilhelm/include
vtest-inc        += $(TOP)/frameworks/native/include/gui/
vtest-inc        += $(TOP)/frameworks/native/include/ui/
vtest-inc        += $(TOP)/frameworks/native/include/
vtest-inc        += $(TOP)/vendor/qcom/proprietary/common/inc
vtest-inc        += $(TOP)/vendor/meig/external/ffmpeg-4.0.2
vtest-inc        += $(TOP)/hardware/qcom/media/mm-core/inc
vtest-inc        += $(TOP)/hardware/qcom/media/libstagefrighthw/
vtest-inc        += $(TOP)/hardware/qcom/camera/QCamera2/stack/common
vtest-inc        += $(TOP)/hardware/qcom/camera/mm-image-codec/qexif
vtest-inc        += $(TOP)/hardware/qcom/camera/mm-image-codec/qomx_core
vtest-inc        += $(TOP)/hardware/qcom/camera/QCamera2/stack/mm-camera-interface/inc/
vtest-inc        += $(TOP)/hardware/qcom/display/libgralloc/
vtest-inc        += $(TOP)/hardware/qcom/display/libqdutils/
vtest-inc        += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include
vtest-inc        += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include/media
vtest-inc        += $(TOP)/external/libxml2 $(TOP)/external/libxml2/include $(TOP)/external/icu/icu4c/source/common
vtest-inc        += $(TOP)/external/libyuv/files/include
vtest-inc        += $(TOP)/external/skia/include/core/
vtest-inc        += $(LOCAL_PATH)/mm-mediacodec/vtest-omx/common/inc
vtest-inc        += $(LOCAL_PATH)/mm-mediacodec/vtest-omx/utils/inc
vtest-inc        += $(LOCAL_PATH)/mm-mediacodec/vtest-omx/xmlparser/inc
vtest-inc        += $(LOCAL_PATH)/mm-audio-test
vtest-inc        += $(LOCAL_PATH)/mm-camera-test/inc
vtest-inc        += $(LOCAL_PATH)/mm-camera-test
vtest-inc        += $(LOCAL_PATH)/mm-osd2-scale/osd/inc
vtest-inc        += $(LOCAL_PATH)/mm-osd2-scale/osd/inc/sdl2
vtest-inc        += $(LOCAL_PATH)/mm-osd2-scale/osd/inc/sdl2_ttf
vtest-inc        += $(LOCAL_PATH)/mm-osd2-scale/scale/inc
vtest-inc        += $(LOCAL_PATH)/mm-osd2-scale/scale/inc/libjpeg
vtest-inc        += $(LOCAL_PATH)/mm-qthread-pool
vtest-inc        += $(LOCAL_PATH)/qcarcam_display_test
vtest-inc        += $(LOCAL_PATH)/mm-mediacodec
vtest-inc        += $(LOCAL_PATH)/mm-mediacodec/atest-ffm
vtest-inc        += $(LOCAL_PATH)/mm-mediacodec/common
vtest-inc        += $(LOCAL_PATH)/mm-mediacodec/vtest-omx
vtest-inc        += $(LOCAL_PATH)/

endif
LOCAL_C_INCLUDES        := $(vtest-inc)
#-------------------------------------------------------------------
#            Add by huangfusheng 2020-5-22
#-------------------------------------------------------------------
vtest-inc        += $(LOCAL_PATH)/../../../../../meig/external/ffmpeg-4.0.2
LOCAL_SHARED_LIBRARIES += libavutil


LOCAL_SHARED_LIBRARIES  += libbinder libcutils libmmcamera_interface
LOCAL_SHARED_LIBRARIES  += libdl libgui libui libutils libqdMetaData libxml2 liblog libjpeg
LOCAL_SHARED_LIBRARIES  += libmm-qthreadpool
LOCAL_SHARED_LIBRARIES  += libmm-mediacodec
LOCAL_SHARED_LIBRARIES  += libmm-meigaudio
LOCAL_SHARED_LIBRARIES  += libmm-meigvideo
LOCAL_SHARED_LIBRARIES  += libmm-qosd libmm-qscale
LOCAL_SHARED_LIBRARIES  += libmm-qdisplay
LOCAL_SHARED_LIBRARIES  += libmm-qunitcam
LOCAL_SHARED_LIBRARIES  += libmm-qaudio

LOCAL_STATIC_LIBRARIES := \
    libyuv_static

LOCAL_SRC_FILES        := \
        meig_cam_hw.cpp


LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_MODULE            := meigcam.default
LOCAL_LDLIBS            := -L$(call host-path, $(LOCAL_PATH)) -lm -lc
LOCAL_32_BIT_ONLY       := true
LOCAL_MODULE_TAGS       := optional
LOCAL_CFLAGS            += $(vtest-def)
ifeq ($(strip $(TARGET_USES_ION)),true)
LOCAL_CFLAGS += -DUSE_ION
endif
LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=GRALLOC_USAGE_PRIVATE_UNCACHED #uncached
LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_CAMERA_HEAP_ID
LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=5
LOCAL_CFLAGS += -DAVFFMPEG_AAC_SUPPORT

include $(BUILD_SHARED_LIBRARY)

#-------------------------------------------------------------------
# include Android.mk
#-------------------------------------------------------------------
include $(LOCAL_PATH)/mm-qthread-pool/Android.mk
include $(LOCAL_PATH)/mm-mediacodec/vtest-omx/testconfig/Android.mk
include $(LOCAL_PATH)/qcarcam_display_test/Android.mk
include $(LOCAL_PATH)/mm-osd2-scale/Android.mk
include $(LOCAL_PATH)/mm-osd2-scale/config/Android.mk
include $(LOCAL_PATH)/mm-audio-test/Android.mk
include $(LOCAL_PATH)/mm-camera-test/Android.mk
include $(LOCAL_PATH)/mm-mediacodec/vtest-omx/Android.mk
include $(LOCAL_PATH)/mm-mediacodec/Android.mk