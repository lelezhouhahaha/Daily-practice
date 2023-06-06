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
#define LOG_TAG "android.hardware.oem.customizescanservice-service"

#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <utils/Log.h>
#include "CustomizeScanService.h"
using ::aidl::android::hardware::oem::customizescanservice::CustomizeScanService;

int main(int /* argc */, char* /* argv */ []) {

	ABinderProcess_setThreadPoolMaxThreadCount(0);
    std::shared_ptr<CustomizeScanService> mCustomizeScanService = ndk::SharedRefBase::make<CustomizeScanService>();

    const std::string instance = std::string() + CustomizeScanService::descriptor + "/default";
	ALOGD(" customizescanservice instance:[%s]", instance.c_str());
    binder_status_t status = AServiceManager_addService(mCustomizeScanService->asBinder().get(), instance.c_str());
	if(status != STATUS_OK){
		ALOGD(" Failed to register mCustomizeScanService service");
	}
    CHECK(status == STATUS_OK);

    ABinderProcess_joinThreadPool();
    return EXIT_FAILURE;  // should not reached
	
}
