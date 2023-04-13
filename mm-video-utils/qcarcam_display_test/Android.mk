OLD_LOCAL_PATH := $(LOCAL_PATH)
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES -DEGL_EGLEXT_PROTOTYPES

LOCAL_CFLAGS += -Wall -Werror -Wunused -Wunreachable-code

LOCAL_SRC_FILES:=\
    display_main.cpp \
    display_interface.cpp \

LOCAL_C_INCLUDES += \
    $(LOCAL_PATH)/../ \
    external/tinyalsa/include \
    frameworks/wilhelm/include \
    $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include

LOCAL_SHARED_LIBRARIES := \
        libcutils \
        liblog \
        libandroidfw \
        libutils \
        libbinder \
        libui \
        libskia \
        libEGL \
        libGLESv1_CM \
        libGLESv2 \
        libgui \
        libOpenSLES \
        libregionalization

#-------------------------------------------------------------------
#            Add by huangfusheng 2020-5-22
#-------------------------------------------------------------------
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../../meig/external/ffmpeg-4.0.2
LOCAL_SHARED_LIBRARIES += libavutil

LOCAL_MODULE:= libmm-qdisplay
LOCAL_MODULE_TAGS := optional

LOCAL_32_BIT_ONLY := true

include $(BUILD_SHARED_LIBRARY)
LOCAL_PATH := $(OLD_LOCAL_PATH)