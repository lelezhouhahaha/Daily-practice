# Copyright (C) 2015 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_COPY_HEADERS_TO := libMeigNativeCamera
LOCAL_COPY_HEADERS    := Camera.h

LOCAL_MODULE    := libMeigNativeCamera

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
	CameraServiceListener.cpp \
        CameraManger.cpp \
        CameraNativeManager.cpp \
        CameraInter.cpp \
        linkedQueue.cpp \
        CameraControlInter.cpp \
        CameraI2cControl.cpp \
		CameraLedControl.cpp \
		CameraSofQueue.cpp

LOCAL_C_INCLUDES := \
	system/core/include \

LOCAL_CFLAGS += -Wall -Werror -Wno-unused-parameter
LOCAL_CFLAGS += -Wno-unused-value -Wno-unused-variable -Wno-macro-redefined -Wno-format
LOCAL_CFLAGS += -D__ANDROID_VNDK__

LOCAL_STATIC_LIBRARIES := android.hardware.camera.common@1.0-helper

LOCAL_SHARED_LIBRARIES := liblog \
    libcamera2ndk_vendor \
    libcamera_metadata \
    libmediandk \
    libnativewindow \
    libutils \
    libui \
    libcutils

# NDK build, shared C++ runtime
#LOCAL_SDK_VERSION := current
#LOCAL_NDK_STL_VARIANT := c++_shared
LOCAL_PROPRIETARY_MODULE := true

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE    := MeigNativeCameraTest

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
	test/main.cpp

LOCAL_C_INCLUDES := \
	system/core/include \

LOCAL_CFLAGS += -Wall -Werror -Wno-unused-parameter
LOCAL_CFLAGS += -Wno-unused-value -Wno-unused-variable -Wno-format
LOCAL_CFLAGS += -D__ANDROID_VNDK__

LOCAL_SHARED_LIBRARIES := liblog \
    libMeigNativeCamera \
    libcamera_metadata \
    libmediandk \
    libutils \
    libui \
    libcutils

LOCAL_PROPRIETARY_MODULE := true
LOCAL_CFLAGS += -D__ANDROID_VNDK__

include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)

LOCAL_MODULE    := MeigCameraInterFaceTest

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
	InterfaceTest/main.cpp

LOCAL_C_INCLUDES := \
	system/core/include \

LOCAL_CFLAGS += -Wall -Werror -Wno-unused-parameter 
LOCAL_CFLAGS += -Wno-unused-value -Wno-unused-variable -Wno-format
LOCAL_CFLAGS += -D__ANDROID_VNDK__

LOCAL_SHARED_LIBRARIES := liblog \
    libMeigNativeCamera \
    libcamera_metadata \
    libmediandk \
    libutils \
    libui \
    libcutils

LOCAL_PROPRIETARY_MODULE := true
LOCAL_CFLAGS += -D__ANDROID_VNDK__

include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)

LOCAL_MODULE    := CameraI2cTest

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
	CameraI2cTest.cpp

LOCAL_C_INCLUDES := \
	system/core/include \

LOCAL_CFLAGS += -Wall -Werror -Wno-unused-parameter 
LOCAL_CFLAGS += -Wno-unused-value -Wno-unused-variable -Wno-format
LOCAL_CFLAGS += -D__ANDROID_VNDK__

LOCAL_SHARED_LIBRARIES := liblog \
    libMeigNativeCamera \
    libcamera_metadata \
    libmediandk \
    libutils \
    libui \
    libcutils

LOCAL_PROPRIETARY_MODULE := true
LOCAL_CFLAGS += -D__ANDROID_VNDK__

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)

LOCAL_MODULE    := CameraSOFTest

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
	CameraSofTest.cpp

LOCAL_C_INCLUDES := \
	system/core/include \

LOCAL_CFLAGS += -Wall -Werror -Wno-unused-parameter 
LOCAL_CFLAGS += -Wno-unused-value -Wno-unused-variable -Wno-format
LOCAL_CFLAGS += -D__ANDROID_VNDK__

LOCAL_SHARED_LIBRARIES := liblog \
    libMeigNativeCamera \
    libcamera_metadata \
    libmediandk \
    libutils \
    libui \
    libcutils

LOCAL_PROPRIETARY_MODULE := true
LOCAL_CFLAGS += -D__ANDROID_VNDK__

include $(BUILD_EXECUTABLE)
