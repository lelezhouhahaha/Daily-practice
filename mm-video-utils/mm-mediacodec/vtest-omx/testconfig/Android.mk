OLD_LOCAL_PATH := $(LOCAL_PATH)
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE:= MasterConfg.xml
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_SRC_FILES := MasterConfg.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/etc/camera/
LOCAL_MODULE_OWNER := qti
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE:= SampleEncodeTest.xml
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_SRC_FILES := SampleEncodeTest.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/etc/camera/
LOCAL_MODULE_OWNER := qti
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE:= SampleDecodeTest.xml
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_SRC_FILES := SampleDecodeTest.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/etc/camera/
LOCAL_MODULE_OWNER := qti
include $(BUILD_PREBUILT)

LOCAL_PATH := $(OLD_LOCAL_PATH)
