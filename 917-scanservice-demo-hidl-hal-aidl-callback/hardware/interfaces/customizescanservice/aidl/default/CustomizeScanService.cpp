#define LOG_TAG "CustomizeScanService"
#include <android-base/file.h>
#include <android-base/stringprintf.h>
#include <android-base/strings.h>
#include <log/log.h>
#include <utils/Log.h>
#include <iostream>
#include <fstream>
#include "CustomizeScanService.h"
static std::shared_ptr<ICustomizeScanServiceCallback> mCallback = nullptr;
namespace aidl::android::hardware::oem::customizescanservice {
static int mFrameScm1Fd = 0;
static long mFrameScm1BufferSize = 0;
static int mFrameScm2Fd = 0;
static long mFrameScm2BufferSize = 0;
static int mFrameCcmFd = 0;
static long mFrameCcmBufferSize = 0;

static ndk::ScopedFileDescriptor shared_scm1fd(dup(1));
static ndk::ScopedFileDescriptor shared_scm2fd(dup(1));
static ndk::ScopedFileDescriptor shared_ccmfd(dup(1));
static ndk::ScopedFileDescriptor shared_quadscmfd(dup(1));

#define DUMP_TO_FILE(filename, p_addr, len) ({ \
  size_t rc = 0; \
  FILE *fp = fopen(filename, "w+"); \
  if (fp) { \
    rc = fwrite(p_addr, 1, len, fp); \
    fclose(fp); \
  } else { \
    printf("cannot dump image\n"); \
  } \
})

void OnDataCallBackFdScm1(int32_t width,int32_t height,int32_t stride,size_t size,int FrameBufferFd,int idx) {
	ALOGD("%s: Get camera 1 Capture imge FrameBufferFd:[%d] size:[%lu] idx:[%d]", __func__, FrameBufferFd, size, idx);
	char dumpFilePath[512];
	//static int SCM1_FrameCount = 0;
	mFrameScm1Fd = dup(FrameBufferFd);
	shared_scm1fd.set(mFrameScm1Fd);
	//mFrameScm1BufferSize = size;
	/*void *FrameBufferVirtualAddr = NULL;
	FrameBufferVirtualAddr = mmap(NULL, mFrameScm1BufferSize, (PROT_READ | PROT_WRITE), MAP_SHARED, mFrameScm1Fd, 0);
    sprintf(dumpFilePath,"/data/dump_camera_1_w_%d_h_%d_stride_%d_count_%d.%s",width,height,stride,SCM1_FrameCount++,"raw");
	if(FrameBufferVirtualAddr != MAP_FAILED){
		ALOGD("%s: Get camera 1 Capture imge FrameBufferVirtualAddr:[0x%x]", __func__, FrameBufferVirtualAddr);
	}
	else
	{
	ALOGD("%s: Map failed, FD=%d", __func__, shared_scm1fd.get());
	}
	DUMP_TO_FILE(dumpFilePath,FrameBufferVirtualAddr, mFrameScm1BufferSize);
	munmap(FrameBufferVirtualAddr,mFrameScm1BufferSize);*/
	if (mCallback != nullptr){
		mCallback->OnDataCallBackFdScm1(width, height, stride, size, &shared_scm1fd, idx);
	}
}

void OnDataCallBackFdScm2(int32_t width,int32_t height,int32_t stride,size_t size,int FrameBufferFd,int idx) {
	ALOGD("%s: Get camera 2 Capture imge FrameBufferFd:[%d] size:[%lu] idx:[%d]", __func__, FrameBufferFd, size, idx);

	char dumpFilePath[512];
	//static int SCM2_FrameCount = 0;
	mFrameScm2Fd = dup(FrameBufferFd);
	shared_scm2fd.set(mFrameScm2Fd);
	//mFrameScm2BufferSize = size;
	/*void *FrameBufferVirtualAddr = NULL;
	FrameBufferVirtualAddr = mmap(NULL, mFrameScm2BufferSize, (PROT_READ | PROT_WRITE), MAP_SHARED, mFrameScm2Fd, 0);
    sprintf(dumpFilePath,"/data/dump_camera_2_w_%d_h_%d_stride_%d_count_%d.%s",width,height,stride,SCM2_FrameCount++,"raw");
	if(FrameBufferVirtualAddr != MAP_FAILED){
		ALOGD("%s: Get camera 2 Capture imge FrameBufferVirtualAddr:[0x%x]", __func__, FrameBufferVirtualAddr);
	}
	else
	{
	ALOGD("%s: Map failed, FD=%d", __func__, shared_scm2fd.get());
	}
	DUMP_TO_FILE(dumpFilePath,FrameBufferVirtualAddr,mFrameScm2BufferSize);
	munmap(FrameBufferVirtualAddr,mFrameScm2BufferSize);*/
	if (mCallback != nullptr){
		mCallback->OnDataCallBackFdScm2(width, height, stride, size, &shared_scm2fd, idx);
	}
}

void OnDataCallBackFdCcm(int32_t width,int32_t height,int32_t stride,size_t size,int FrameBufferFd,int idx) {
	ALOGD("%s: Get camera 0 Capture imge FrameBufferFd:[%d] size:[%lu] idx:[%d]", __func__, FrameBufferFd, size, idx);
	char dumpFilePath[512];
	//static int Ccm_FrameCount = 0;	
	mFrameCcmFd = dup(FrameBufferFd);
	shared_ccmfd.set(mFrameCcmFd);
	//mFrameCcmBufferSize = size;
	/*void *FrameBufferVirtualAddr = NULL;
	FrameBufferVirtualAddr = mmap(NULL, mFrameCcmBufferSize, (PROT_READ | PROT_WRITE), MAP_SHARED, mFrameCcmFd, 0);
    sprintf(dumpFilePath,"/data/dump_camera_0_w_%d_h_%d_stride_%d_count_%d.%s",width,height,stride,Ccm_FrameCount++,"raw");
	if(FrameBufferVirtualAddr != MAP_FAILED){
		ALOGD("%s: Get camera 0 Capture imge FrameBufferVirtualAddr:[0x%x]", __func__, FrameBufferVirtualAddr);
	}
	else
	{
	ALOGD("%s: Map failed, FD=%d", __func__, shared_ccmfd.get());
	}
	DUMP_TO_FILE(dumpFilePath,FrameBufferVirtualAddr,mFrameCcmBufferSize);
	munmap(FrameBufferVirtualAddr,mFrameCcmBufferSize);*/
	if (mCallback != nullptr){
		mCallback->OnDataCallBackFdCcm(width, height, stride, size, &shared_ccmfd, idx);
	}
}

void OnDataCallBackFdQuadScm(int32_t width,int32_t height,int32_t stride,size_t size,int FrameBufferFd,int idx) {
	ALOGD("%s: Get camera quad scm Capture imge ", __func__);
	ALOGD("%s: Get camera quad scm Capture imge FrameBufferFd:[%d] ", __func__, FrameBufferFd);
	ALOGD("%s: Get camera quad scm Capture imge size:[%lu] idx:[%d]", __func__, size, idx);
	char dumpFilePath[512];
	//static int Quad_Scm_FrameCount = 0;	
	int mFrameQuadScmFd = dup(FrameBufferFd);
	shared_quadscmfd.set(mFrameQuadScmFd);
	//mFrameQuadScmBufferSize = size;
	if (mCallback != nullptr){
		mCallback->OnDataCallBackFdQuadScm(width, height, stride, size, &shared_quadscmfd, idx);
	}
}


CustomizeScanService::CustomizeScanService() {
	ALOGD(" CustomizeScanService");
	mCallback = nullptr;
}

CustomizeScanService::~CustomizeScanService(){
	ALOGD(" ~CustomizeScanService");
	mCallback = nullptr;
}

ndk::ScopedAStatus CustomizeScanService::customize_setCallback(const std::shared_ptr<ICustomizeScanServiceCallback>& callback){
	ALOGD(" customize_setCallback");
	if (callback == nullptr) {
        return ndk::ScopedAStatus::fromExceptionCode(EX_TRANSACTION_FAILED);
	}
	if( mCallback != nullptr){
		ALOGD(" mCallback is exist! and return ok.");
		return ndk::ScopedAStatus::ok();
	}
	mCallback = callback;
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_open(int* ret) {
		*ret = 0;
		ALOGD(" customize_cam_open");
		*ret = tripple_cam_open();
		//tripple_cam_scm_set_callback(OnDataCallBackScm1, OnDataCallBackScm2);
		//tripple_cam_ccm_set_callback(OnDataCallBackCcm);
		tripple_cam_scm_set_callback(OnDataCallBackFdScm1, OnDataCallBackFdScm2);
		tripple_cam_ccm_set_callback(OnDataCallBackFdCcm);
		ALOGD(" customize_cam_open ret:[%d]", *ret);
		return ndk::ScopedAStatus::ok();
	}
	
ndk::ScopedAStatus CustomizeScanService::customize_cam_close(int* ret) {
	*ret = 0;
	ALOGD(" customize_cam_close");
	*ret = tripple_cam_close();
	ALOGD(" customize_cam_close ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_suspend(int* ret) {
	*ret = 0;
	ALOGD(" customize_cam_suspend");
	*ret = tripple_cam_suspend();
	ALOGD(" customize_cam_suspend ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_resume(int* ret) {
	*ret = 0;
	ALOGD(" customize_cam_resume");
	*ret = tripple_cam_resume();
	ALOGD(" customize_cam_resume ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_scm_capture(int8_t target_cam, int* ret){
	ALOGD(" customize_cam_scm_capture:[%d]", target_cam);
	*ret = tripple_cam_scm_capture((uint8_t)target_cam);
	ALOGD(" customize_cam_scm_capture:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

    // Adding return type to method instead of out param int ret since there is only one return value.
ndk::ScopedAStatus CustomizeScanService::customize_cam_ccm_capture(int8_t aimer_suspend, int* ret){
	ALOGD(" customize_cam_ccm_capture:[%d]", aimer_suspend);
	*ret = tripple_cam_ccm_capture((uint8_t)aimer_suspend);
	ALOGD(" customize_cam_ccm_capture:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}



    // Adding return type to method instead of out param int ret since there is only one return value.
ndk::ScopedAStatus CustomizeScanService::customize_cam_scm_mcu_i2c_write(const Reg_data& SendData, int length, int* ret){
	for(auto data : SendData.regValueArray){
    	ALOGD("%s: reg_addr:[%d] reg_data:[%d] delay:[%d] data_mask:[%d] length:[%d]", __func__, data.reg_addr, data.reg_data, data.delay, data.data_mask, length);
	}
	struct i2c_reg_setting i2cRegSettings;
	i2cRegSettings.addr_type = SendData.addr_type;
	i2cRegSettings.data_type = SendData.data_type;
	struct i2c_reg_array i2cRegArray[length];
	int i = 0;
	for(auto data : SendData.regValueArray){
		i2cRegArray[i].reg_addr = data.reg_addr;
		i2cRegArray[i].reg_data = data.reg_data;
		i2cRegArray[i].delay = data.delay;
		i2cRegArray[i].data_mask = data.data_mask;
		i++;
	}
	i2cRegSettings.reg_settings = i2cRegArray;
	*ret = tripple_cam_scm_mcu_i2c_write(&i2cRegSettings, length);
	ALOGD("%s: ret:[%d]", __func__, *ret);
	return ndk::ScopedAStatus::ok();
}

    // Adding return type to method instead of out param int ret since there is only one return value.
ndk::ScopedAStatus CustomizeScanService::customize_cam_scm_i2c_write(int8_t slaveAddress, const Reg_data& SendData, int length, int* ret){
	ALOGD("%s: slaveAddress:[%u]", __func__, (uint8_t)slaveAddress);
	for(auto data : SendData.regValueArray){
    	ALOGD("%s: slaveAddress[%u]  reg_addr:[%u] reg_data:[%u] delay:[%u] data_mask:[%u] length:[%u]", __func__, (uint8_t)slaveAddress, (uint32_t)data.reg_addr, (uint32_t)data.reg_data, (uint32_t)data.delay, (uint32_t)data.data_mask, (uint32_t)length);
	}
	struct i2c_reg_setting i2cRegSettings;
	i2cRegSettings.addr_type = SendData.addr_type;
	i2cRegSettings.data_type = SendData.data_type;
	struct i2c_reg_array i2cRegArray[length];
	int i = 0;
	for(auto data : SendData.regValueArray){
		i2cRegArray[i].reg_addr = data.reg_addr;
		i2cRegArray[i].reg_data = data.reg_data;
		i2cRegArray[i].delay = data.delay;
		i2cRegArray[i].data_mask = data.data_mask;
		i++;
	}
	i2cRegSettings.reg_settings = i2cRegArray;
	*ret = tripple_cam_scm_i2c_write(slaveAddress, &i2cRegSettings, length);
	return ndk::ScopedAStatus::ok();
}
    
    // Adding return type to method instead of out param int ret since there is only one return value.
ndk::ScopedAStatus CustomizeScanService::customize_cam_ccm_i2c_write(const Reg_data& SendData, int length, int* ret){
	for(auto data : SendData.regValueArray){
    	ALOGD("%s: reg_addr:[%u] reg_data:[%u] delay:[%u] data_mask:[%u] length:[%u]", __func__, (int32_t)data.reg_addr, (int32_t)data.reg_data, (int32_t)data.delay, (int32_t)data.data_mask, (int32_t)length);
	}
	struct i2c_reg_setting i2cRegSettings;
	i2cRegSettings.addr_type = SendData.addr_type;
	i2cRegSettings.data_type = SendData.data_type;
	struct i2c_reg_array i2cRegArray[length];
	int i = 0;
	for(auto data : SendData.regValueArray){
		i2cRegArray[i].reg_addr = data.reg_addr;
		i2cRegArray[i].reg_data = data.reg_data;
		i2cRegArray[i].delay = data.delay;
		i2cRegArray[i].data_mask = data.data_mask;
		i++;
	}
	i2cRegSettings.reg_settings = i2cRegArray;
	*ret = tripple_cam_ccm_i2c_write(&i2cRegSettings, length);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_scm_i2c_read(int8_t slaveAddress, int SendData, int AddrType, int DataType, int* ret){
	*ret = 0;
	ALOGD(" customize_cam_scm_i2c_read slaveAddress:[%d] SendData:[%d] AddrType:[%d] DataType:[%d]", slaveAddress, SendData, AddrType, DataType);
	tripple_cam_scm_i2c_read((uint8_t)slaveAddress, (uint32_t)SendData, (uint32_t *)ret, (uint32_t)AddrType, (uint32_t)DataType);
	ALOGD(" customize_cam_scm_i2c_read *ret:[0x%x]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_ccm_i2c_read(int SendData, int AddrType, int DataType, int* ret){
	*ret = 0;
	ALOGD(" customize_cam_scm_i2c_read SendData:[%d]", SendData);
	ALOGD(" customize_cam_scm_i2c_read AddrType:[%d]", AddrType);
	ALOGD(" customize_cam_scm_i2c_read DataType:[%d]", DataType);
	tripple_cam_ccm_i2c_read((uint32_t)SendData, (uint32_t *)ret, (uint32_t)AddrType, (uint32_t)DataType);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_ccm_return_buffer(int idx, int* ret) {
	*ret = 0;
	ALOGD(" customize_cam_ccm_return_buffer idx:[%d]", idx);
	*ret = tripple_cam_ccm_return_buffer((int32_t)idx);
	ALOGD(" customize_cam_ccm_return_buffer ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_scm1_return_buffer(int idx, int* ret) {
	*ret = 0;
	ALOGD(" customize_cam_scm1_return_buffer idx:[%d]", idx);
	*ret = tripple_cam_scm1_return_buffer((int32_t)idx);
	ALOGD(" customize_cam_scm1_return_buffer ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_scm2_return_buffer(int idx, int* ret) {
	*ret = 0;
	ALOGD(" customize_cam_scm2_return_buffer idx:[%d]", idx);
	*ret = tripple_cam_scm2_return_buffer((int32_t)idx);
	ALOGD(" customize_cam_scm2_return_buffer ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_ccm_serial_number_read(std::string* ccm_serial_number_buffer) {
	*ccm_serial_number_buffer = "";
	char buf[256 +4];
	memset(buf, 0, 256 + 4);
	tripple_cam_ccm_serial_number_read(buf);
	*ccm_serial_number_buffer = std::string(buf);
	ALOGD(" customize_cam_ccm_serial_number_read ccm_serial_number_buffer:[%s]", ccm_serial_number_buffer->c_str());
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_scm_serial_number_read(std::string* scm_serial_number_buffer) {
	*scm_serial_number_buffer = "";
	char buf[256 +4];
	memset(buf, 0, 256 + 4);
	tripple_cam_scm_serial_number_read(buf);
	*scm_serial_number_buffer = std::string(buf);
	ALOGD(" customize_cam_scm_serial_number_read scm_serial_number_buffer:[%s]", scm_serial_number_buffer->c_str());
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_scm_fw_version_read(std::string* scm_fw_version_buffer) {
	*scm_fw_version_buffer = "";
	char buf[256 +4];
	memset(buf, 0, 256 + 4);
	tripple_cam_scm_fw_version_read(buf);
	*scm_fw_version_buffer = std::string(buf);
	ALOGD(" customize_cam_scm_fw_version_read scm_fw_version_buffer:[%s]", scm_fw_version_buffer->c_str());
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_ccm_move_Focus(int distancemm, int* ret) {
	*ret = 0;
	ALOGD("zll customize_cam_ccm_move_Focus distancemm:[%d]", distancemm);
	*ret = tripple_cam_ccm_move_Focus((int32_t)distancemm);
	ALOGD(" customize_cam_ccm_move_Focus ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_ccm_flash(int flash_status, int timeout, int* ret) {
	*ret = 0;
	ALOGD(" customize_cam_ccm_flash flash_status:[%d] timeout:[%d]", flash_status, timeout);
	*ret = tripple_cam_ccm_flash((int32_t)flash_status, (int32_t)timeout);
	ALOGD(" customize_cam_ccm_flash ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_ccm_switch_size(int type, int* ret) {
	*ret = 0;
	ALOGD(" customize_cam_ccm_switch_size type:[%d]", type);
	*ret = tripple_cam_ccm_switch_size((int32_t)type);
	ALOGD(" customize_cam_ccm_switch_size ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_aim_sus(int8_t gpio_signal, int* ret){
	*ret = 0;
	ALOGD(" customize_cam_aim_sus gpio_signal:[%d]", gpio_signal);
	*ret = tripple_cam_aim_sus((int8_t)gpio_signal);
	ALOGD(" customize_cam_aim_sus ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_cam_wake(int8_t gpio_signal, int* ret){
	*ret = 0;
	ALOGD(" customize_cam_wake gpio_signal:[%d]", gpio_signal);
	*ret = tripple_cam_wake((int8_t)gpio_signal);
	ALOGD(" customize_cam_wake ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_non_volatail_param_write(const std::string& param, int* ret){
	*ret = 0;
	ALOGD(" customize_non_volatail_param_write param:[%s]", param.c_str());
	char buf[256 +4];
	memset(buf, 0, 256 + 4);
	const char *content = param.c_str();
	int size = 0;
	int length = param.size();
	if(length >= 256)
		size = 256;
	else size = length;
	ALOGD(" customize_non_volatail_param_write size:[%d]", size);
	strncpy(buf, content, size);
	*ret = non_volatail_param_write(buf);
	ALOGD(" customize_non_volatail_param_write ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_non_volatail_param_read(std::string* param){
	*param = "";
	char buf[256 +4];
	memset(buf, 0, 256 + 4);
	non_volatail_param_read(buf);
	*param = std::string(buf);
	ALOGD(" customize_non_volatail_param_read param:[%s]", param->c_str());
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_quad_cam_open(int* ret){
	*ret = 0;
	ALOGD(" customize_quad_cam_open");
	*ret = quad_cam_open();
	ALOGD(" customize_quad_cam_open ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_quad_cam_close(int* ret){
	*ret = 0;
	ALOGD(" customize_quad_cam_close");
	*ret = quad_cam_close();
	ALOGD(" customize_quad_cam_close ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_quad_cam_suspend(int* ret){
	*ret = 0;
	ALOGD(" customize_quad_cam_suspend");
	*ret = quad_cam_suspend();
	ALOGD(" customize_quad_cam_suspend ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}
ndk::ScopedAStatus CustomizeScanService::customize_quad_cam_resume(int* ret){
	*ret = 0;
	ALOGD(" customize_quad_cam_resume");
	*ret = quad_cam_resume();
	ALOGD(" customize_quad_cam_resume ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_quad_cam_capture(int* ret){
	*ret = 0;
	ALOGD(" customize_quad_cam_capture");
	*ret = quad_cam_capture(OnDataCallBackFdQuadScm);
	ALOGD(" customize_quad_cam_capture ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}
ndk::ScopedAStatus CustomizeScanService::customize_quad_cam_scm_i2c_write(int8_t slaveAddress,const Reg_data& SendData,int length, int* ret){
	ALOGD("%s: slaveAddress:[%u]", __func__, (uint8_t)slaveAddress);
	for(auto data : SendData.regValueArray){
    	ALOGD("%s: slaveAddress[%u]  reg_addr:[%u] reg_data:[%u] delay:[%u] data_mask:[%u] length:[%u]", __func__, (uint8_t)slaveAddress, (uint32_t)data.reg_addr, (uint32_t)data.reg_data, (uint32_t)data.delay, (uint32_t)data.data_mask, (uint32_t)length);
	}
	struct i2c_reg_setting i2cRegSettings;
	i2cRegSettings.addr_type = SendData.addr_type;
	i2cRegSettings.data_type = SendData.data_type;
	struct i2c_reg_array i2cRegArray[length];
	int i = 0;
	for(auto data : SendData.regValueArray){
		i2cRegArray[i].reg_addr = data.reg_addr;
		i2cRegArray[i].reg_data = data.reg_data;
		i2cRegArray[i].delay = data.delay;
		i2cRegArray[i].data_mask = data.data_mask;
		i++;
	}
	i2cRegSettings.reg_settings = i2cRegArray;
	*ret = quad_cam_scm_i2c_write(slaveAddress, &i2cRegSettings, length);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_quad_cam_scm_i2c_read(int8_t slaveAddress,int SendData,int AddrType,int DataType, int* ret){
	*ret = 0;
	ALOGD(" customize_quad_cam_scm_i2c_read slaveAddress:[%d] SendData:[%d] AddrType:[%d] DataType:[%d]", slaveAddress, SendData, AddrType, DataType);
	quad_cam_scm_i2c_read((uint8_t)slaveAddress, (uint32_t)SendData, (uint32_t *)ret, (uint32_t)AddrType, (uint32_t)DataType);
	ALOGD(" customize_quad_cam_scm_i2c_read *ret:[0x%x]", *ret);
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanService::customize_quad_cam_scm_return_buffer(int idx, int* ret){
	*ret = 0;
	ALOGD(" customize_quad_cam_scm_return_buffer idx:[%d]", idx);
	*ret = quad_cam_scm_return_buffer((int32_t)idx);
	ALOGD(" customize_quad_cam_scm_return_buffer ret:[%d]", *ret);
	return ndk::ScopedAStatus::ok();
}

} //namespace aidl::android::hardware::oem::customizescanservice
