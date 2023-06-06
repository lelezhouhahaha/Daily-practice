// FIXME: your file license if you have one

#pragma once
#ifndef ANDROID_HARDWARE_OEMSERVICE_V1_0_OEMSERVICE_H
#define ANDROID_HARDWARE_OEMSERVICE_V1_0_OEMSERVICE_H

#include <vendor/oem/hardware/customizeoemservice/1.0/ICustomizeOemService.h>
#include <hidl/MQDescriptor.h>
#include <hidl/Status.h>

namespace vendor::oem::hardware::customizeoemservice::V1_0::implementation {

using ::android::hardware::hidl_array;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_string;
using ::android::hardware::hidl_vec;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::sp;

struct CustomizeOemService : public ICustomizeOemService {
	CustomizeOemService();
	~CustomizeOemService();
    Return<int32_t> customize_SetProvisionStatus(uint32_t enable) override;
    Return<int32_t> customize_GetProvisionStatus() override;

    // Methods from ::android::hidl::base::V1_0::IBase follow.

};

}  // namespace vendor::oem::hardware::customizeoemservice::V1_0::implementation
#endif  // ANDROID_HARDWARE_SCANSERVICE_V1_0_SCANSERVICE_H
