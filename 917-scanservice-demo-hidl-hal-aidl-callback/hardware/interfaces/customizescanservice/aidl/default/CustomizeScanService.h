#ifndef _CUSTOMIZE_SCAN_SERVICE_SERVICE_H_
#define _CUSTOMIZE_SCAN_SERVICE_SERVICE_H_
#include <aidl/android/hardware/oem/customizescanservice/BnCustomizeScanService.h>
#include <aidl/android/hardware/oem/customizescanservice/BnCustomizeScanServiceCallback.h>
#include <libMeigCameraApi/CameraApi.h>
#include <iostream>
#include <map>
#include <sys/mman.h>
#include <utils/StrongPointer.h>
using ::std::vector;
using aidl::android::hardware::oem::customizescanservice::ICustomizeScanServiceCallback;

namespace aidl::android::hardware::oem::customizescanservice {

class CustomizeScanService : public BnCustomizeScanService {
  public:
    CustomizeScanService();
	~CustomizeScanService();
	ndk::ScopedAStatus customize_setCallback(const std::shared_ptr<ICustomizeScanServiceCallback>& callback) override;
	ndk::ScopedAStatus customize_cam_open(int* ret) override;
	ndk::ScopedAStatus customize_cam_close(int* ret) override;
	ndk::ScopedAStatus customize_cam_suspend(int* ret) override;
	ndk::ScopedAStatus customize_cam_resume(int* ret) override;
	ndk::ScopedAStatus customize_cam_scm_capture(int8_t target_cam, int* ret) override;
	ndk::ScopedAStatus customize_cam_ccm_capture(int8_t aimer_suspend, int* ret) override;
	ndk::ScopedAStatus customize_cam_scm_mcu_i2c_write(const Reg_data& SendData, int length, int* ret) override;
	ndk::ScopedAStatus customize_cam_scm_i2c_write(int8_t slaveAddress, const Reg_data& SendData, int length, int* ret) override;
	ndk::ScopedAStatus customize_cam_ccm_i2c_write(const Reg_data& SendData, int length, int* ret) override;
	ndk::ScopedAStatus customize_cam_scm_i2c_read(int8_t slaveAddress, int SendData, int AddrType, int DataType, int* ret) override;
	ndk::ScopedAStatus customize_cam_ccm_i2c_read(int SendData, int AddrType, int DataType, int* ret) override;
	ndk::ScopedAStatus customize_cam_ccm_return_buffer(int idx, int* ret) override;
	ndk::ScopedAStatus customize_cam_scm1_return_buffer(int idx, int* ret) override;
	ndk::ScopedAStatus customize_cam_scm2_return_buffer(int idx, int* ret) override;
	ndk::ScopedAStatus customize_cam_ccm_serial_number_read(std::string* ccm_serial_number_buffer) override;
	ndk::ScopedAStatus customize_cam_scm_serial_number_read(std::string* scm_serial_number_buffer) override;
	ndk::ScopedAStatus customize_cam_scm_fw_version_read(std::string* scm_fw_version_buffer) override;
	ndk::ScopedAStatus customize_cam_ccm_move_Focus(int distancemm, int* ret) override;
	ndk::ScopedAStatus customize_cam_ccm_flash(int flash_status, int timeout, int* ret) override;
	ndk::ScopedAStatus customize_cam_ccm_switch_size(int type, int* ret) override;
	ndk::ScopedAStatus customize_cam_aim_sus(int8_t gpio_signal, int* ret) override;
	ndk::ScopedAStatus customize_cam_wake(int8_t gpio_signal, int* ret) override;
	ndk::ScopedAStatus customize_non_volatail_param_write(const std::string& param, int* ret) override;
	ndk::ScopedAStatus customize_non_volatail_param_read(std::string* param) override;
	ndk::ScopedAStatus customize_quad_cam_open(int* ret) override;
	ndk::ScopedAStatus customize_quad_cam_close(int* ret) override;
	ndk::ScopedAStatus customize_quad_cam_suspend(int* ret) override;
	ndk::ScopedAStatus customize_quad_cam_resume(int* ret) override;
	ndk::ScopedAStatus customize_quad_cam_capture(int* ret) override;
	ndk::ScopedAStatus customize_quad_cam_scm_i2c_write(int8_t slaveAddress,const Reg_data& SendData,int length, int* ret) override;
	ndk::ScopedAStatus customize_quad_cam_scm_i2c_read(int8_t slaveAddress,int SendData,int AddrType,int DataType, int* ret) override;
	ndk::ScopedAStatus customize_quad_cam_scm_return_buffer(int idx, int* ret) override;
};
} // namespace aidl::android::hardware::customizescanservice
#endif  // _CUSTOMIZE_SCAN_SERVICE_SERVICE_H_
