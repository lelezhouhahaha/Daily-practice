# ---------------------------------------------------------------------------------
# Android JNI makefile for libmm-qcamera
# ---------------------------------------------------------------------------------
OLD_LOCAL_PATH := $(LOCAL_PATH)
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libmm-qunitcam

#$(warning "Start to $(LOCAL_PATH) Android.mk")
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_C_INCLUDES +=  \
        $(TOP)/frameworks/native/include/media/openmax \
        $(TOP)/hardware/qcom/camera/QCamera2/stack/mm-camera-interface/inc/ \
        $(TOP)/hardware/qcom/camera/QCamera2/stack/common \
        $(TOP)/hardware/qcom/camera/mm-image-codec/qexif \
        $(TOP)/hardware/qcom/camera/mm-image-codec/qomx_core \
        $(TOP)/external/libyuv/files/include \
        $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include \
        $(LOCAL_PATH)/../mm-qthread-pool \
        $(LOCAL_PATH)/inc \
        $(LOCAL_PATH)/ \

LOCAL_C_INCLUDES += $(kernel_includes)

LOCAL_CFLAGS += -DCAMERA_ION_HEAP_ID=ION_IOMMU_HEAP_ID
LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=GRALLOC_USAGE_PRIVATE_UNCACHED #uncached
LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_CAMERA_HEAP_ID
LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=5
# System header file path prefix
LOCAL_CFLAGS += -DSYSTEM_HEADER_PREFIX=sys
LOCAL_CFLAGS += -D_ANDROID_ -DQCAMERA_REDEFINE_LOG

LOCAL_SRC_FILES := \
        src/mm_qcamera_app.c \
        src/mm_qcamera_video.c \
        src/mm_qcamera_snapshot.c \
        src/mm_qcamera_reprocess.c \
        src/mm_qcamera_queue.c \
        mm_qcamera_unit_test.c \

LOCAL_SHARED_LIBRARIES := libcutils libdl libmmcamera_interface libjpeg libmm-qthreadpool
LOCAL_STATIC_LIBRARIES := \
    libyuv_static

LOCAL_32_BIT_ONLY := true
include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(OLD_LOCAL_PATH)