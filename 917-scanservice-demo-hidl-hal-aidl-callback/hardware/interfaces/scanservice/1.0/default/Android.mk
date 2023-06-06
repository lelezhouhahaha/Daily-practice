LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

include $(CLEAR_VARS)
LOCAL_MODULE := vendor.scan.hardware.scanservice@1.0-service
LOCAL_INIT_RC := vendor.scan.hardware.scanservice@1.0-service.rc
#LOCAL_VINTF_FRAGMENTS := vendor.scan.hardware.scanservice@1.0-service.xml
LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_PROPRIETARY_MODULE := true
LOCAL_SRC_FILES := \
    ScanService.cpp \
    service.cpp

LOCAL_SHARED_LIBRARIES := \
    vendor.scan.hardware.scanservice@1.0 \
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
    android.hardware.oem.customizescanservice-ndk_platform \
    libnativewindow

LOCAL_CFLAGS += -D__ANDROID_VNDK__

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
	system/core/include \
    $(TARGET_OUT_HEADERS)/libMeigCameraApi \

include $(BUILD_EXECUTABLE)
