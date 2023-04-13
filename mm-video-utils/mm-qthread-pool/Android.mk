#
# Android JNI makefile for simplest_ffmpeg_audio_encoder
#
OLD_LOCAL_PATH := $(LOCAL_PATH)
LOCAL_PATH := $(call my-dir)

# ---------------------------------------------------------------------------------
#             Make the Shared library (one-dimensional)
# ---------------------------------------------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libmm-qthreadpool

#$(warning "Start to $(LOCAL_PATH) Android.mk")
LOCAL_C_INCLUDES := $(LOCAL_PATH)

LOCAL_SRC_FILES:= mm_qthread_pool.c

LOCAL_SHARED_LIBRARIES := libcutils liblog

LOCAL_32_BIT_ONLY := true
include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(OLD_LOCAL_PATH)