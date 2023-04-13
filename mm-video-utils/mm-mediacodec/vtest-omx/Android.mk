#-------------------------------------------------------------------
# Android JNI makefile for libmm-meigvideo
#-------------------------------------------------------------------
OLD_LOCAL_PATH := $(LOCAL_PATH)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(call is-board-platform-in-list, $(MSM_VIDC_TARGET_LIST)),true)

# Forcing O0 flag to remove all compiler optimizations as a workaround for
# stack corruptions seen on 64 bit.
vtest-def := \
    -g -O0 -Werror\
    -D_POSIX_SOURCE -DPOSIX_C_SOURCE=199506L \
    -D_XOPEN_SOURCE=600 \
    -D_ANDROID_ \
    -D_ANDROID_ -DQCAMERA_REDEFINE_LOG \
    -DSYSTEM_HEADER_PREFIX=sys \
    -DCAMERA_ION_HEAP_ID=ION_IOMMU_HEAP_ID \
    -Wno-deprecated-declarations \
    -Wno-mismatched-tags \
    -fno-stack-protector \
    -DAMSS_VERSION=$(AMSS_VERSION) \
    $(mmcamera_debug_defines) \
    $(mmcamera_debug_cflags) \
    $(USE_SERVER_TREE) \
    $(barcode-def)

vtest-inc        := $(TARGET_OUT_HEADERS)/mm-core/omxcore
vtest-inc        += $(LOCAL_PATH)/common/inc
vtest-inc        += $(LOCAL_PATH)/utils/inc
vtest-inc        += $(TOP)/vendor/qcom/proprietary/common/inc
vtest-inc        += $(TOP)/frameworks/native/include/media/hardware
vtest-inc        += $(TOP)/frameworks/native/include/media/openmax
vtest-inc        += $(TOP)/hardware/qcom/media/mm-video-v4l2/vidc
vtest-inc        += $(TOP)/hardware/qcom/media/mm-core/inc
vtest-inc        += $(TOP)/hardware/qcom/media/libstagefrighthw/
vtest-inc        += $(TOP)/hardware/qcom/camera/QCamera2/stack/common
vtest-inc        += $(TOP)/hardware/qcom/camera/mm-image-codec/qexif
vtest-inc        += $(TOP)/hardware/qcom/camera/mm-image-codec/qomx_core
vtest-inc        += $(TOP)/hardware/qcom/camera/QCamera2/stack/mm-camera-interface/inc/
vtest-inc        += $(TOP)/frameworks/native/include/gui/
vtest-inc        += $(TOP)/frameworks/native/include/ui/
vtest-inc        += $(TOP)/frameworks/native/include/
vtest-inc        += $(TOP)/external/connectivity/stlport/stlport/
vtest-inc        += $(TOP)/hardware/qcom/display/libgralloc/
vtest-inc        += $(TOP)/hardware/qcom/display/libqdutils/
vtest-inc        += $(TOP)/system/core/include/system/
vtest-inc        += $(LOCAL_PATH)/xmlparser/inc

#-------------------------------------------------------------------
#            Add by huangfusheng 2020-5-22
#-------------------------------------------------------------------
vtest-inc        += $(LOCAL_PATH)/../../../../../meig/external/ffmpeg-4.0.2
LOCAL_SHARED_LIBRARIES += libavutil


CP_COPY_PATH := $(TOP)/vendor/qcom/proprietary/securemsm/sampleclient/
ifeq ($(shell if test -f $(CP_COPY_PATH)/content_protection_copy.h; then echo true; else echo false; fi;),true)
$(warning SECURE_COPY_ENABLED)
vtest-def        += -DSECURE_COPY_ENABLED
vtest-inc        += $(CP_COPY_PATH)
vtest-inc        += $(TOP)/vendor/qcom/proprietary/securemsm/QSEEComAPI/
endif

ifeq ($(call is-board-platform-in-list, $(MASTER_SIDE_CP_TARGET_LIST)),true)
$(warning MASTER_SIDE_CP_ENABLED)
vtest-def        += -DMASTER_SIDE_CP
endif

ifeq ($(TARGET_USES_MEDIA_EXTENSIONS),true)
$(warning SUPPORT_CONFIG_INTRA_REFRESH)
vtest-def        += -DSUPPORT_CONFIG_INTRA_REFRESH
endif

LOCAL_MODULE            := libmm-meigvideo
LOCAL_32_BIT_ONLY := true
LOCAL_MODULE_TAGS       := optional
LOCAL_CFLAGS            := $(vtest-def)
ifeq ($(strip $(TARGET_USES_ION)),true)
LOCAL_CFLAGS += -DUSE_ION
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm8974)
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_IOMMU_HEAP_ID
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=0
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=9
else ifeq ($(filter $(TARGET_BOARD_PLATFORM), apq8084 msm8084),$(TARGET_BOARD_PLATFORM))
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_IOMMU_HEAP_ID
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=0
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=9
else ifeq ($(TARGET_BOARD_PLATFORM),msm8994)
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_IOMMU_HEAP_ID
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=0
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=9
else ifeq ($(TARGET_BOARD_PLATFORM),msm8916 msm8952 msm8937 msm8953)
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_IOMMU_HEAP_ID
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=0
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=9
else ifeq ($(TARGET_BOARD_PLATFORM),msm8226)
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_IOMMU_HEAP_ID
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=0
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=9
else ifeq ($(TARGET_BOARD_PLATFORM),msm8610)
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_IOMMU_HEAP_ID
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=0
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=9
else ifeq ($(TARGET_BOARD_PLATFORM),msm8960)
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_IOMMU_HEAP_ID
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=0
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=5
else ifneq (,$(filter msm8660,$(TARGET_BOARD_PLATFORM)))
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_IOMMU_HEAP_ID # EBI
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=0
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=5
else
        LOCAL_CFLAGS += -DCAMERA_GRALLOC_CACHING_ID=GRALLOC_USAGE_PRIVATE_UNCACHED #uncached
        LOCAL_CFLAGS += -DCAMERA_ION_FALLBACK_HEAP_ID=ION_CAMERA_HEAP_ID
        LOCAL_CFLAGS += -DNUM_RECORDING_BUFFERS=5
endif
LOCAL_C_INCLUDES        := $(vtest-inc)
LOCAL_C_INCLUDES        += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include
LOCAL_C_INCLUDES        += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include/media
LOCAL_C_INCLUDES        += $(TOP)/external/libxml2 $(TOP)/external/libxml2/include $(TOP)/external/icu/icu4c/source/common
LOCAL_ADDITIONAL_DEPENDENCIES    := $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr
LOCAL_PRELINK_MODULE    := false

LOCAL_SHARED_LIBRARIES  += libOmxCore libOmxVenc libbinder libcutils
LOCAL_SHARED_LIBRARIES  += libdl libgui libui libutils libqdMetaData libxml2 liblog


LOCAL_SRC_FILES         := \
                xmlparser/src/vtest_XmlParser.cpp \
                xmlparser/src/vtest_XmlParserHelper.cpp \
                utils/src/vt_queue.c \
                utils/src/vt_linkqueue.c \
                utils/src/vt_semaphore.c \
                utils/src/vt_signal.c \
                utils/src/vt_file.c \
                utils/src/vt_ion_allocator.c \
                common/src/vtest_Config.cpp \
                common/src/vtest_File.cpp \
                common/src/vtest_Mutex.cpp \
                common/src/vtest_Parser.cpp \
                common/src/vtest_LinkQueue.cpp \
                common/src/vtest_Queue.cpp \
                common/src/vtest_Script.cpp \
                common/src/vtest_Signal.cpp \
                common/src/vtest_SignalQueue.cpp \
                common/src/vtest_Sleeper.cpp \
                common/src/vtest_Thread.cpp \
                common/src/vtest_Time.cpp \
                common/src/vtest_Crypto.cpp \
                common/src/vtest_BufferManager.cpp \
                common/src/vtest_ISource.cpp \
                common/src/vtest_ITestCase.cpp \
                common/src/vtest_IPostProc.cpp \
                common/src/vtest_PostProcFactory.cpp \
                common/src/vtest_TestCaseFactory.cpp \
                common/src/vtest_Decoder.cpp \
                common/src/vtest_DecoderFileSource.cpp \
                common/src/vtest_DecoderFileSink.cpp \
                common/src/vtest_Encoder.cpp \
                common/src/vtest_EncoderFileSink.cpp \
                common/src/vtest_EncoderFileSource.cpp \
                common/src/vtest_NativeWindow.cpp \
                common/src/vtest_MdpSource.cpp \
                common/src/vtest_MdpOverlaySink.cpp \
                common/src/vtest_PostProcSource.cpp \
                common/src/vtest_TestDecode.cpp \
                common/src/vtest_TestEncode.cpp \
                common/src/vtest_TestTranscode.cpp \
                common/src/vtest_TestPostProc.cpp \
                vtest_Core.cpp

ifeq ($(BOARD_HAVE_ADRENO), true)
GPU_PP_PATH := $(TOP)/vendor/qcom/proprietary/gles/adreno200/tools/gpupostprocessing
ifeq ($(shell if test -f $(GPU_PP_PATH)/gpupostprocessing.h; then echo true; else echo false; fi;),true)
vtest-pp-def            += -DGPU_PP_ENABLED
vtest-pp-inc            += $(GPU_PP_PATH)
vtest-pp-inc            += $(TOP)/system/core/include/system/
vtest-pp-src            += common/src/vtest_GpuPostProc.cpp
endif

C2DCC_PP_PATH := $(TOP)/hardware/qcom/media/libc2dcolorconvert
ifeq ($(shell if test -f $(C2DCC_PP_PATH)/C2DColorConverter.h; then echo true; else echo false; fi;),true)
vtest-pp-def            += -DC2DCC_PP_ENABLED
vtest-pp-inc            += $(C2DCC_PP_PATH)
vtest-pp-inc            += $(TARGET_OUT_HEADERS)/qcom/display
vtest-pp-src            += common/src/vtest_C2dCCPostProc.cpp
endif
endif # ($(BOARD_HAVE_ADRENO), true)

MMCC_PP_PATH := $(TOP)/vendor/qcom/proprietary/mm-color-convertor/libmmcolorconvertor/
ifeq ($(shell if test -f $(MMCC_PP_PATH)/MMColorConvert.h; then echo true; else echo false; fi;),true)
vtest-pp-def            += -DMMCC_PP_ENABLED
vtest-pp-inc            += $(MMCC_PP_PATH)
vtest-pp-src            += common/src/vtest_MmCCPostProc.cpp
endif

$(warning $(vtest-pp-def))
LOCAL_CFLAGS            += $(vtest-pp-def)
LOCAL_C_INCLUDES        += $(vtest-pp-inc)
LOCAL_SRC_FILES         += $(vtest-pp-src)

include $(BUILD_SHARED_LIBRARY)
LOCAL_PATH := $(OLD_LOCAL_PATH)
