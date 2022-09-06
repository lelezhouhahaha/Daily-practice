/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef ANDROID_HARDWARE_SCANSERVICE_V1_0_SCANSERVICE_H
#define ANDROID_HARDWARE_SCANSERVICE_V1_0_SCANSERVICE_H

#include <vendor/scan/hardware/scanservice/1.0/IScanService.h>
#include <vendor/scan/hardware/scanservice/1.0/IScanServiceCallback.h>
//#include <vendor/meig/hardware/scanservice/1.0/types.h>
#include <libMeigNativeCamera/Camera.h>

namespace vendor {
namespace scan {
namespace hardware {
namespace scanservice {
namespace V1_0 {
namespace implementation {

using ::vendor::scan::hardware::scanservice::V1_0::IScanService;
using ::vendor::scan::hardware::scanservice::V1_0::IScanServiceCallback;
//using ::vendor::scan::hardware::scanservice::V1_0::types;
using ::android::hardware::hidl_array;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_string;
using ::android::hardware::hidl_vec;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::sp;

struct ScanService : public IScanService {
    //Return<int32_t> oemkeys_remap_set(const hidl_string& key_name, const hidl_string& new_key_name) override;
    //Return<int32_t> oemkeys_wakeup_set(const hidl_string& key_name,  int32_t wakeable) override;
    //Return<int32_t> oemkeys_tp_wakeup(int32_t wakeable) override;
	ScanService();
	Return<void> open(int32_t cameraId, int32_t width, int32_t height, int32_t format) override;
	Return<void> close(int32_t cameraId) override;
    Return<void> resume(int32_t cameraId) override;
    Return<void> suspend(int32_t cameraId) override;
    Return<void> capture(int32_t cameraId) override;
    Return<void> setParameters(int32_t cameraId, int32_t type, int32_t value) override;
    Return<void> move_focus(int32_t cameraId,float value) override;
	Return<void> setScanServiceCallback(const sp<IScanServiceCallback>& callback) override;
	CameraContrlNode *pNode = NULL;
};

}  // namespace implementation
}  // namespace V1_0
}  // namespace oemkeys
}  // namespace hardware
}  // namespace elo
}  // namespace vendor

#endif  // ANDROID_HARDWARE_OEMKEYS_V1_0_OEMKEYS_H
