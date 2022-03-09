LOCAL_PATH:=$(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:=libmeigpsam-jni
LOCAL_SRC_FILES := SerialPort.cpp
LOCAL_LDLIBS += -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE:=libdiag-jni
LOCAL_SRC_FILES := DiagJniInterface.cpp
LOCAL_CFLAGS += -Wno-unused-variable \
                -Wno-unused-parameter
LOCAL_LDLIBS += -llog 
LOCAL_SHARED_LIBRARIES := libdiag_system libutils libcutils
LOCAL_HEADER_LIBRARIES := vendor_common_inc
include $(BUILD_SHARED_LIBRARY)  