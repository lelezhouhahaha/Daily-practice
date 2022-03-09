LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_DEX_PREOPT := false

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res

LOCAL_SRC_FILES := $(call all-java-files-under, java) \
    $(call all-Iaidl-files-under, java)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += swfpsdk

ifeq ($(strip $(PLATFORM_ENV)), REE)
    LOCAL_PACKAGE_NAME := FpFactoryM
    LOCAL_MANIFEST_FILE := ex_manifest_ree/AndroidManifest.xml
else ifeq ($(strip $(PLATFORM_ENV)), TEE)
    LOCAL_PACKAGE_NAME := FpFactoryM_TEE
    LOCAL_MANIFEST_FILE := AndroidManifest.xml
else ifeq ($(strip $(PLATFORM_ENV)), ANDROID8_1)
    LOCAL_PACKAGE_NAME := FpFactoryO_hidl_8_1
    LOCAL_MANIFEST_FILE := ex_manifest_o/AndroidManifest.xml
    LOCAL_SRC_FILES += $(call all-java-files-under, android8_1_src)
    LOCAL_STATIC_JAVA_LIBRARIES += android.hidl.base-V1.0-java
else ifeq ($(strip $(PLATFORM_ENV)), ANDROID8_0)
    LOCAL_PACKAGE_NAME := FpFactoryO_hidl_8_0
    LOCAL_MANIFEST_FILE := ex_manifest_o/AndroidManifest.xml
    LOCAL_SRC_FILES += $(call all-java-files-under, android8_0_src)
    LOCAL_STATIC_JAVA_LIBRARIES += android.hidl.base-V1.0-java
else ifeq ($(strip $(PLATFORM_ENV)), ANDROIDO_FW)
    LOCAL_PACKAGE_NAME := FpFactoryO_fw
    LOCAL_MANIFEST_FILE := ex_manifest_o_fw/AndroidManifest.xml
    LOCAL_SRC_FILES += $(call all-java-files-under, android_o_fw_src)
else ifeq ($(strip $(PLATFORM_ENV)), ANDROID_P)
    LOCAL_PACKAGE_NAME := FpFactory_P_hidl
    LOCAL_MANIFEST_FILE := ex_manifest_o/AndroidManifest.xml
    LOCAL_PRIVATE_PLATFORM_APIS := true
    LOCAL_SRC_FILES += $(call all-java-files-under, android9_src)
    LOCAL_STATIC_JAVA_LIBRARIES += android.hidl.base-V1.0-java
else ifeq ($(strip $(PLATFORM_ENV)), ANDROID_Q)
    LOCAL_PACKAGE_NAME := FpFactory_Q_hidl
    LOCAL_MANIFEST_FILE := ex_manifest_o/AndroidManifest.xml
    LOCAL_PRIVATE_PLATFORM_APIS := true
    LOCAL_SRC_FILES += $(call all-java-files-under, android10_src)
    LOCAL_STATIC_JAVA_LIBRARIES += android.hidl.base-V1.0-java
else
    $(error unknown PLATFORM_ENV: $(PLATFORM_ENV))
endif

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := swfpsdk:libs/com.swfp.manager.jar
include $(BUILD_MULTI_PREBUILT)