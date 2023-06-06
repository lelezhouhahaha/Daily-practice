///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL interface (or parcelable). Do not try to
// edit this file. It looks like you are doing that because you have modified
// an AIDL interface in a backward-incompatible way, e.g., deleting a function
// from an interface or a field from a parcelable and it broke the build. That
// breakage is intended.
//
// You must not make a backward incompatible changes to the AIDL files built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package android.hardware.oem.customizescanservice;
@VintfStability
interface ICustomizeScanService {
  void customize_setCallback(in android.hardware.oem.customizescanservice.ICustomizeScanServiceCallback callback);
  int customize_cam_open();
  int customize_cam_close();
  int customize_cam_suspend();
  int customize_cam_resume();
  int customize_cam_scm_capture(in byte target_cam);
  int customize_cam_ccm_capture(in byte aimer_suspend);
  int customize_cam_scm_mcu_i2c_write(in android.hardware.oem.customizescanservice.Reg_data SendData, in int length);
  int customize_cam_scm_i2c_write(in byte slaveAddress, in android.hardware.oem.customizescanservice.Reg_data SendData, in int length);
  int customize_cam_ccm_i2c_write(in android.hardware.oem.customizescanservice.Reg_data SendData, in int length);
  int customize_cam_scm_i2c_read(in byte slaveAddress, in int SendData, in int AddrType, in int DataType);
  int customize_cam_ccm_i2c_read(in int SendData, in int AddrType, in int DataType);
  int customize_cam_ccm_return_buffer(in int idx);
  int customize_cam_scm1_return_buffer(in int idx);
  int customize_cam_scm2_return_buffer(in int idx);
  @utf8InCpp String customize_cam_ccm_serial_number_read();
  @utf8InCpp String customize_cam_scm_serial_number_read();
  @utf8InCpp String customize_cam_scm_fw_version_read();
  int customize_cam_ccm_move_Focus(in int distancemm);
  int customize_cam_ccm_flash(in int flash_status, in int timeout);
  int customize_cam_ccm_switch_size(in int type);
  int customize_cam_aim_sus(in byte gpio_signal);
  int customize_cam_wake(in byte gpio_signal);
  int customize_non_volatail_param_write(@utf8InCpp String param);
  @utf8InCpp String customize_non_volatail_param_read();
  int customize_quad_cam_open();
  int customize_quad_cam_close();
  int customize_quad_cam_suspend();
  int customize_quad_cam_resume();
  int customize_quad_cam_capture();
  int customize_quad_cam_scm_i2c_write(in byte slaveAddress, in android.hardware.oem.customizescanservice.Reg_data SendData, in int length);
  int customize_quad_cam_scm_i2c_read(in byte slaveAddress, in int SendData, in int AddrType, in int DataType);
  int customize_quad_cam_scm_return_buffer(in int idx);
}
