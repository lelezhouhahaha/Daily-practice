LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

include $(CLEAR_VARS)
LOCAL_MODULE := vendor.oem.hardware.customizeoemservice@1.0-impl
LOCAL_INIT_RC := vendor.oem.hardware.customizeoemservice@1.0-impl.rc
#LOCAL_VINTF_FRAGMENTS := vendor.scan.hardware.scanservice@1.0-service.xml
LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_PROPRIETARY_MODULE := true
LOCAL_SRC_FILES := \
    CustomizeOemService.cpp \
    service.cpp

LOCAL_SHARED_LIBRARIES := \
    vendor.oem.hardware.customizeoemservice@1.0 \
    libhidlbase \
    liblog \
    libutils \
    lib_customized_ssign

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
	system/core/include 
include $(BUILD_EXECUTABLE)
