// FIXME: your file license if you have one

#pragma once
#ifndef ANDROID_HARDWARE_SCANSERVICE_V1_0_SCANSERVICE_H
#define ANDROID_HARDWARE_SCANSERVICE_V1_0_SCANSERVICE_H

#include <vendor/scan/hardware/scanservice/1.0/IScanService.h>
#include <aidl/android/hardware/oem/customizescanservice/ICustomizeScanService.h>
#include <aidl/android/hardware/oem/customizescanservice/ICustomizeScanServiceCallback.h>
#include <aidl/android/hardware/oem/customizescanservice/BnCustomizeScanServiceCallback.h>
#include <hidl/MQDescriptor.h>
#include <hidl/Status.h>
#include <android/binder_manager.h>
#include <android/binder_ibinder.h>

//using aidl::android::hardware::oem::customizescanservice::ICustomizeScanServiceCallback;
using aidl::android::hardware::oem::customizescanservice::BnCustomizeScanServiceCallback;

namespace aidl::android::hardware::oem::customizescanservice {
struct CustomizeScanServiceCallback : public BnCustomizeScanServiceCallback {
	ndk::ScopedAStatus OnDataCallBackFdScm1(int32_t width,int32_t height,int32_t stride,int64_t size,ndk::ScopedFileDescriptor* shared_fd,int32_t idx) override;
	ndk::ScopedAStatus OnDataCallBackFdScm2(int32_t width,int32_t height,int32_t stride,int64_t size,ndk::ScopedFileDescriptor* shared_fd,int32_t idx) override;
	ndk::ScopedAStatus OnDataCallBackFdCcm(int32_t width,int32_t height,int32_t stride,int64_t size,ndk::ScopedFileDescriptor* shared_fd,int32_t idx) override;
	ndk::ScopedAStatus OnDataCallBackFdQuadScm(int32_t width,int32_t height,int32_t stride,long size,ndk::ScopedFileDescriptor* shared_fd,int32_t idx) override;
};
};


namespace vendor {
namespace scan {
namespace hardware {
namespace scanservice {
namespace V1_0 {
namespace implementation {

using ::android::hardware::hidl_array;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_string;
using ::android::hardware::hidl_vec;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::sp;


struct ScanService : public IScanService {
    // Methods from ::vendor::scan::hardware::scanservice::V1_0::IScanService follow.
	ScanService();
	~ScanService();
    Return<int32_t> cam_open() override;
    Return<int32_t> cam_close() override;
    Return<int32_t> cam_suspend() override;
    Return<int32_t> cam_resume() override;
    Return<int32_t> cam_scm_capture(uint8_t target_cam) override;
    Return<int32_t> cam_ccm_capture() override;
    Return<int32_t> cam_scm_mcu_i2c_write(const hidl_vec<reg_array>& SendData, uint32_t length) override;
    Return<int32_t> cam_scm_i2c_write(uint8_t slaveAddress, const hidl_vec<reg_array>& SendData, uint32_t length) override;
    Return<uint32_t> cam_scm_i2c_read(uint8_t slaveAddress, uint32_t SendData, uint32_t AddrType, uint32_t DataType) override;
    Return<int32_t> cam_ccm_i2c_write(const hidl_vec<reg_array>& SendData, uint32_t length) override;
    Return<uint32_t> cam_ccm_i2c_read(uint32_t SendData, uint32_t AddrType, uint32_t DataType) override;
    Return<int32_t> cam_ccm_return_buffer(int32_t idx) override;
    Return<int32_t> cam_scm1_return_buffer(int32_t idx) override;
    Return<int32_t> cam_scm2_return_buffer(int32_t idx) override;
    Return<void> cam_ccm_serial_number_read(cam_ccm_serial_number_read_cb _hidl_cb) override;
    Return<void> cam_scm_serial_number_read(cam_scm_serial_number_read_cb _hidl_cb) override;
    Return<void> cam_scm_fw_version_read(cam_scm_fw_version_read_cb _hidl_cb) override;
    Return<int32_t> cam_ccm_move_Focus(int32_t distancemm) override;
    Return<int32_t> cam_ccm_flash(int32_t flash_status, int32_t timeout) override;
    Return<int32_t> cam_ccm_switch_size(int32_t type) override;
	Return<int32_t> cam_aim_sus(uint8_t gpio_signal) override;
	Return<int32_t> cam_wake(uint8_t gpio_signal) override;
	Return<int32_t> non_volatail_param_write(const hidl_string& param) override;
	Return<void> non_volatail_param_read(non_volatail_param_read_cb _hidl_cb) override;
	Return<int32_t> quad_cam_open() override;
	Return<int32_t> quad_cam_close() override;
	Return<int32_t> quad_cam_suspend() override;
	Return<int32_t> quad_cam_resume() override;
	Return<int32_t> quad_cam_scm_capture() override;
	Return<int32_t> quad_cam_scm_i2c_write(uint8_t slaveAddress, uint32_t AddrType,uint32_t DataType, const hidl_vec<reg_array>& SendData,uint32_t length) override;
	Return<uint32_t> quad_cam_scm_i2c_read(uint8_t slaveAddress,uint32_t SendData,uint32_t AddrType,uint32_t DataType) override;
	Return<int32_t> quad_cam_scm_return_buffer(int32_t idx) override;
};

}  // namespace implementation
}  // namespace V1_0
}  // namespace oemkeys
}  // namespace hardware
}  // namespace elo
}  // namespace vendor

#endif  // ANDROID_HARDWARE_SCANSERVICE_V1_0_SCANSERVICE_H
