LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := android.hardware.oem.customizescanservice-service
LOCAL_INIT_RC := android.hardware.oem.customizescanservice-service.rc
LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_PROPRIETARY_MODULE := true
LOCAL_SRC_FILES := \
    CustomizeScanService.cpp \
    service.cpp

LOCAL_SHARED_LIBRARIES := \
    android.hardware.oem.customizescanservice-ndk_platform \
    libbase \
    libbinder_ndk \
    libcutils \
    libhidlbase \
    libhidltransport \
    liblog \
    libutils \
    libhardware \
    libui \
    libcamera2ndk_vendor \
    libcamera_metadata \
    libmediandk \
    libnativewindow \
    libMeigCameraApi

LOCAL_CFLAGS += -D__ANDROID_VNDK__

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
	system/core/include \
    $(TARGET_OUT_HEADERS)/libMeigCameraApi \

include $(BUILD_EXECUTABLE)
