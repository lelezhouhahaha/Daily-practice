#
# Android JNI makefile for simplest_ffmpeg_audio_encoder
#

OLD_LOCAL_PATH := $(LOCAL_PATH)
LOCAL_PATH := $(call my-dir)
#-------------------------------------------------------------------
# include Android.mk
#-------------------------------------------------------------------
include $(LOCAL_PATH)/atest-ffm/Android.mk

# ---------------------------------------------------------------------------------
#             Make the Shared library (one-dimensional)
# ---------------------------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libmm-mediacodec

LOCAL_LDLIBS     := -L$(call host-path, $(LOCAL_PATH)) -lm -lc

#$(warning "Start to $(LOCAL_PATH) Android.mk")
avitest-inc      := $(LOCAL_PATH)/
avitest-inc      += $(TOP)/external/libxml2
avitest-inc      += $(TOP)/external/libxml2/include
avitest-inc      += $(TOP)/external/icu/icu4c/source/common
avitest-inc      += $(TOP)/hardware/qcom/media/mm-core/inc
avitest-inc      += $(TOP)/hardware/qcom/media/libstagefrighthw/
avitest-inc      += $(TARGET_OUT_HEADERS)/mm-core/omxcore
avitest-inc      += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include
avitest-inc      += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include/media
avitest-inc      += $(LOCAL_PATH)/vtest-omx/common/inc
avitest-inc      += $(LOCAL_PATH)/vtest-omx/utils/inc
avitest-inc      += $(LOCAL_PATH)/vtest-omx/xmlparser/inc
avitest-inc      += $(LOCAL_PATH)/vtest-omx
avitest-inc      += $(LOCAL_PATH)/atest-ffm
avitest-inc      += $(LOCAL_PATH)/common
avitest-inc      += $(LOCAL_PATH)/../mm-qthread-pool
avitest-inc      += $(LOCAL_PATH)/../../../../meig/external/ffmpeg-4.0.2
LOCAL_C_INCLUDES := $(avitest-inc)

LOCAL_SRC_FILES:= AVI_MediaCodec.cpp

LOCAL_SHARED_LIBRARIES := libcutils liblog libbinder
LOCAL_SHARED_LIBRARIES += libdl libgui libui libutils libqdMetaData libxml2
LOCAL_SHARED_LIBRARIES += libmm-qthreadpool
LOCAL_SHARED_LIBRARIES += libmm-meigvideo
LOCAL_SHARED_LIBRARIES += libmm-meigaudio
LOCAL_SHARED_LIBRARIES += libavutil libavcodec libswresample libavformat libswscale libavfilter

LOCAL_32_BIT_ONLY := true
include $(BUILD_SHARED_LIBRARY)
LOCAL_PATH := $(OLD_LOCAL_PATH)


