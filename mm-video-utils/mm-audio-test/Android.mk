#
# Android JNI makefile for avi_audio
#
OLD_LOCAL_PATH := $(LOCAL_PATH)
LOCAL_PATH := $(call my-dir)

# ---------------------------------------------------------------------------------
#             Make the Shared library (avi Audio)
# ---------------------------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libmm-qaudio

$(warning "Start to ----> $(LOCAL_PATH)/Android.mk")
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_C_INCLUDES +=  \
        $(LOCAL_PATH)/../mm-mediacodec/common \
        $(LOCAL_PATH)/../../../../meig/external/ffmpeg-4.0.2


LOCAL_SRC_FILES:= mm_qaudio_test.cpp

LOCAL_SHARED_LIBRARIES := libcutils liblog

LOCAL_32_BIT_ONLY := true
include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(OLD_LOCAL_PATH)