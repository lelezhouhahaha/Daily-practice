// FIXME: your file license if you have one

#define LOG_TAG "ScanServiceHidl"
#include "ScanService.h"
#include <log/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>

#include <iostream>
#include <map>
#include <sys/mman.h>
#include <android/binder_process.h>

using aidl::android::hardware::oem::customizescanservice::ICustomizeScanService;
using aidl::android::hardware::oem::customizescanservice::ICustomizeScanServiceCallback;
using aidl::android::hardware::oem::customizescanservice::Reg_value;
using aidl::android::hardware::oem::customizescanservice::Reg_data;
using namespace ndk;
using namespace aidl::android::hardware::oem::customizescanservice;
using std::vector;

std::shared_ptr<ICustomizeScanService> mService;
namespace aidl::android::hardware::oem::customizescanservice{

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


ndk::ScopedAStatus CustomizeScanServiceCallback::OnDataCallBackFdScm1(int32_t width,int32_t height,int32_t stride,int64_t size,ndk::ScopedFileDescriptor* shared_fd,int32_t idx){
	void *FrameBufferVirtualAddr = NULL;
	char dumpFilePath[512];
	static int SCM1_FrameCount = 0;
	int FrameBufferFd = dup(shared_fd->get());
	ALOGD("%s: width:[%d] height:[%d] stride:[%d] size:[%lu] FrameBufferFd:[%d] idx:[%d]", __func__, width, height, stride, size, FrameBufferFd, idx);
	FrameBufferVirtualAddr = mmap(NULL, size, (PROT_READ | PROT_WRITE), MAP_SHARED, FrameBufferFd, 0);
	sprintf(dumpFilePath,"/data/dump_camera_callback_1_w_%d_h_%d_stride_%d_count_%d.%s",width,height,stride,SCM1_FrameCount++,"raw");
	ALOGD("%s: dumpFilePath:[%s]", __func__, dumpFilePath);
	DUMP_TO_FILE(dumpFilePath,FrameBufferVirtualAddr, size);
	munmap(FrameBufferVirtualAddr,size);
	FrameBufferVirtualAddr=NULL;
	close(FrameBufferFd);
	if(mService != NULL){
		int ret = 0;
		mService->customize_cam_scm1_return_buffer(idx, &ret);
		ALOGD("%s: ret:[%d]", __func__, ret);
	}
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanServiceCallback::OnDataCallBackFdScm2(int32_t width,int32_t height,int32_t stride,int64_t size,ndk::ScopedFileDescriptor* shared_fd,int32_t idx){
	void *FrameBufferVirtualAddr = NULL;
	char dumpFilePath[512];
	static int SCM2_FrameCount = 0;
	int FrameBufferFd = dup(shared_fd->get());
	ALOGD("%s: width:[%d] height:[%d] stride:[%d] size:[%lu] FrameBufferFd:[%d] idx:[%d]", __func__, width, height, stride, size, FrameBufferFd, idx);
	FrameBufferVirtualAddr = mmap(NULL, size, (PROT_READ | PROT_WRITE), MAP_SHARED, FrameBufferFd, 0);
	sprintf(dumpFilePath,"/data/dump_camera_callback_2_w_%d_h_%d_stride_%d_count_%d.%s",width,height,stride,SCM2_FrameCount++,"raw");
	ALOGD("%s: dumpFilePath:[%s]", __func__, dumpFilePath);
	DUMP_TO_FILE(dumpFilePath,FrameBufferVirtualAddr, size);
	munmap(FrameBufferVirtualAddr,size);
	FrameBufferVirtualAddr=NULL;
	close(FrameBufferFd);
	if(mService != NULL){
		int ret = 0;
		mService->customize_cam_scm2_return_buffer(idx, &ret);
		ALOGD("%s: ret:[%d]", __func__, ret);
	}
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanServiceCallback::OnDataCallBackFdCcm(int32_t width,int32_t height,int32_t stride,int64_t size,ndk::ScopedFileDescriptor* shared_fd,int32_t idx){
	void *FrameBufferVirtualAddr = NULL;
	char dumpFilePath[512];
	static int CCM_FrameCount = 0;
	int FrameBufferFd = dup(shared_fd->get());
	ALOGD("%s: width:[%d] height:[%d] stride:[%d] size:[%lu] FrameBufferFd:[%d] idx:[%d]", __func__, width, height, stride, size, FrameBufferFd, idx);
	FrameBufferVirtualAddr = mmap(NULL, size, (PROT_READ | PROT_WRITE), MAP_SHARED, FrameBufferFd, 0);
	sprintf(dumpFilePath,"/data/dump_camera_callback_0_w_%d_h_%d_stride_%d_count_%d.%s",width,height,stride,CCM_FrameCount++,"raw");
	ALOGD("%s: dumpFilePath:[%s]", __func__, dumpFilePath);
	DUMP_TO_FILE(dumpFilePath,FrameBufferVirtualAddr, size);
	munmap(FrameBufferVirtualAddr,size);
	FrameBufferVirtualAddr=NULL;
	close(FrameBufferFd);
	if(mService != NULL){
		int ret = 0;
		mService->customize_cam_ccm_return_buffer(idx, &ret);
		ALOGD("%s: ret:[%d]", __func__, ret);
	}
	return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus CustomizeScanServiceCallback::OnDataCallBackFdQuadScm(int32_t width,int32_t height,int32_t stride,int64_t size,ndk::ScopedFileDescriptor* shared_fd,int32_t idx){
	void *FrameBufferVirtualAddr = NULL;
	char dumpFilePath[512];
	static int Quad_SCM_FrameCount = 0;
	int FrameBufferFd = dup(shared_fd->get());
	ALOGD("%s: width:[%d] height:[%d] stride:[%d] size:[%lu] FrameBufferFd:[%d] idx:[%d]", __func__, width, height, stride, size, FrameBufferFd, idx);
	FrameBufferVirtualAddr = mmap(NULL, size, (PROT_READ | PROT_WRITE), MAP_SHARED, FrameBufferFd, 0);
	sprintf(dumpFilePath,"/data/dump_camera_callback_quad_w_%d_h_%d_stride_%d_count_%d.%s",width,height,stride,Quad_SCM_FrameCount++,"raw");
	ALOGD("%s: dumpFilePath:[%s]", __func__, dumpFilePath);
	DUMP_TO_FILE(dumpFilePath,FrameBufferVirtualAddr, size);
	munmap(FrameBufferVirtualAddr,size);
	FrameBufferVirtualAddr=NULL;
	close(FrameBufferFd);
	if(mService != NULL){
		int ret = 0;
		mService->customize_quad_cam_scm_return_buffer(idx, &ret);
		ALOGD("%s: ret:[%d]", __func__, ret);
	}
	return ndk::ScopedAStatus::ok();
}

std::shared_ptr<ICustomizeScanServiceCallback> getCustomizeScanServiceCallback() {
	ALOGD("%s: 1", __func__);
	ABinderProcess_setThreadPoolMaxThreadCount(0);
	ABinderProcess_startThreadPool();
	std::shared_ptr<CustomizeScanServiceCallback> mScanservicecallback = ndk::SharedRefBase::make<CustomizeScanServiceCallback>();
	ALOGD("%s: 2", __func__);
	SpAIBinder binder = SpAIBinder(mScanservicecallback->asBinder());
	ALOGD("%s: 3", __func__);
    return ICustomizeScanServiceCallback::fromBinder(binder);
  }
};//aidl::android::hardware::oem::customizescanservice

namespace vendor {
namespace scan {
namespace hardware {
namespace scanservice {
namespace V1_0 {
namespace implementation {

std::shared_ptr<ICustomizeScanServiceCallback> mServiceCallback;

std::shared_ptr<ICustomizeScanService> getCustomizeScanService() {
	const std::string instance = std::string() + ICustomizeScanService::descriptor + "/default";
	ALOGD("%s: instance.c_str():[%s]", __func__, instance.c_str());
    SpAIBinder binder = SpAIBinder(AServiceManager_getService(instance.c_str()));
    return ICustomizeScanService::fromBinder(binder);
  }
  
int initCustomizeScanService(){
	if(mService == NULL){
		ALOGD("%s: mService == NULL", __func__);
		mService = getCustomizeScanService();
	}
	if(mService == NULL){
		ALOGD("%s: 1 mService == NULL", __func__);
		return -1;
	}else {
		ALOGD("%s: 2 mService != NULL", __func__);
	}
	
	return 0;
}
  
 

Return<int32_t> ScanService::cam_open() {
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}

	if(mServiceCallback == NULL){
		mServiceCallback = getCustomizeScanServiceCallback();
	}
	
	if(mServiceCallback == NULL){
		ALOGD("%s: get scan callback service failed.");
		return -1;
	}
	
	ALOGD("%s: 1 customize_cam_open start", __func__);
	mService->customize_cam_open(&ret);
	ALOGD("%s: 1 customize_cam_open end ret:[%d]", __func__, ret);
	if(mServiceCallback != NULL){
		ALOGD("%s: set callback start", __func__);
		mService->customize_setCallback(mServiceCallback);
		ALOGD("%s: set callback end", __func__);
	}
    return ret;
}

Return<int32_t> ScanService::cam_close() {
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	
	ALOGD("%s: 1 customize_cam_close start", __func__);
	mService->customize_cam_close(&ret);
	ALOGD("%s: 1 customize_cam_close end ret:[%d]", __func__, ret);
	
	return ret;
}

Return<int32_t> ScanService::cam_suspend() {
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	
	ALOGD("%s: 1 customize_cam_suspend start", __func__);
	mService->customize_cam_suspend(&ret);
	ALOGD("%s: 1 customize_cam_suspend end ret:[%d]", __func__, ret);
	return ret;
}

Return<int32_t> ScanService::cam_resume() {
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	
	ALOGD("%s: 1 customize_cam_resume start", __func__);
	mService->customize_cam_resume(&ret);
	ALOGD("%s: 1 customize_cam_resume end ret:[%d]", __func__, ret);
	return ret;
}

Return<int32_t> ScanService::cam_scm_capture(uint8_t target_cam) {
	ALOGD("%s: target_cam:[%u]", __func__, target_cam);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_scm_capture((int8_t)target_cam, &ret);
	return ret;
}

Return<int32_t> ScanService::cam_ccm_capture() {
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_ccm_capture(1, &ret);
	return ret;
}

Return<int32_t> ScanService::cam_scm_mcu_i2c_write(const hidl_vec<reg_array>& SendData, uint32_t length) {
	for(auto data : SendData){
    	ALOGD("%s: reg_addr:[%u] reg_data:[%u] delay:[%u] data_mask:[%u] length:[%u]", __func__, data.reg_addr, data.reg_data, data.delay, data.data_mask, length);
	}
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	Reg_data aidlSendData;
	aidlSendData.addr_type = 1;
	aidlSendData.data_type = 1;
	vector<Reg_value> aidlData;
	for(auto data : SendData){
		Reg_value item;
		item.reg_addr = data.reg_addr;
		item.reg_data = data.reg_data;
		item.delay = data.delay;
		item.data_mask = data.data_mask;
		aidlData.push_back(item);
	}
	aidlSendData.regValueArray = aidlData;
	mService->customize_cam_scm_mcu_i2c_write(aidlSendData, (int32_t)length, (int *)&ret);
	return ret;
}

Return<int32_t> ScanService::cam_scm_i2c_write(uint8_t slaveAddress, const hidl_vec<reg_array>& SendData, uint32_t length) {
	ALOGD("%s: slaveAddress:[%u]", __func__, slaveAddress);
	for(auto data : SendData){
    	ALOGD("%s: slaveAddress[%u]  reg_addr:[%u] reg_data:[%u] delay:[%u] data_mask:[%u] length:[%u]", __func__, slaveAddress, data.reg_addr, data.reg_data, data.delay, data.data_mask, length);
	}
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	Reg_data aidlSendData;
	aidlSendData.addr_type = 2;
	aidlSendData.data_type = 1;
	vector<Reg_value> aidlData;
	for(auto data : SendData){
		Reg_value item;
		item.reg_addr = data.reg_addr;
		item.reg_data = data.reg_data;
		item.delay = data.delay;
		item.data_mask = data.data_mask;
		aidlData.push_back(item);
	}
	aidlSendData.regValueArray = aidlData;
	mService->customize_cam_scm_i2c_write((int8_t)slaveAddress, aidlSendData, (int32_t)length, (int *)&ret);
	return ret;
}

Return<uint32_t> ScanService::cam_scm_i2c_read(uint8_t slaveAddress, uint32_t SendData, uint32_t AddrType, uint32_t DataType) {
	ALOGD("%s: slaveAddress:[%u] SendData:[%u] AddrType:[%u] DataType:[%u] ", __func__, slaveAddress, SendData, AddrType, DataType);
	uint32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_scm_i2c_read((int8_t)slaveAddress, (int)SendData, (int)AddrType, (int)DataType, (int *)&ret);
	ALOGD("%s: ret:[%u]", __func__, ret);
	return ret;
}

Return<int32_t> ScanService::cam_ccm_i2c_write(const hidl_vec<reg_array>& SendData, uint32_t length) {
	for(auto data : SendData){
    	ALOGD("%s: reg_addr:[%u] reg_data:[%u] delay:[%u] data_mask:[%u] length:[%u]", __func__, data.reg_addr, data.reg_data, data.delay, data.data_mask, length);
	}
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	Reg_data aidlSendData;
	aidlSendData.addr_type = 2;
	aidlSendData.data_type = 2;
	vector<Reg_value> aidlData;
	for(auto data : SendData){
		Reg_value item;
		item.reg_addr = data.reg_addr;
		item.reg_data = data.reg_data;
		item.delay = data.delay;
		item.data_mask = data.data_mask;
		aidlData.push_back(item);
	}
	aidlSendData.regValueArray = aidlData;
	mService->customize_cam_ccm_i2c_write(aidlSendData, (int32_t)length, (int *)&ret);
	return ret;
}

Return<uint32_t> ScanService::cam_ccm_i2c_read(uint32_t SendData, uint32_t AddrType, uint32_t DataType) {
	ALOGD("%s:  SendData:[%u] AddrType:[%u] DataType:[%u] ", __func__, SendData, AddrType, DataType);
	uint32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_ccm_i2c_read((int)SendData, (int)AddrType, (int)DataType, (int *)&ret);
	ALOGD("%s: ret:[%u]", __func__, ret);
	return ret;
}

Return<int32_t> ScanService::cam_ccm_return_buffer(int32_t idx) {
	ALOGD("%s: idx:[%d]", __func__, idx);
	int32_t ret = 0;
	
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_ccm_return_buffer(idx, &ret);
	return ret;
}

Return<int32_t> ScanService::cam_scm1_return_buffer(int32_t idx) {
	ALOGD("%s: idx:[%d]", __func__, idx);
	int32_t ret = 0;

	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_scm1_return_buffer(idx, &ret);
	return ret;
}

Return<int32_t> ScanService::cam_scm2_return_buffer(int32_t idx) {
	ALOGD("%s: idx:[%d]", __func__, idx);
	int32_t ret = 0;

	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_scm2_return_buffer(idx, &ret);
	return ret;
}

Return<void> ScanService::cam_ccm_serial_number_read(cam_ccm_serial_number_read_cb _hidl_cb) {
	ALOGD("%s:", __func__);
	std::string aidl_return = "";
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return Void();
	}
	mService->customize_cam_ccm_serial_number_read(&aidl_return);
	ALOGD("%s: aidl_return.c_str:[%s]", __func__, aidl_return.c_str());
	hidl_string result(aidl_return);
    _hidl_cb(result);
    return Void();
}

Return<void> ScanService::cam_scm_serial_number_read(cam_scm_serial_number_read_cb _hidl_cb) {
	ALOGD("%s:", __func__);
	std::string aidl_return = "";
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return Void();
	}
	mService->customize_cam_scm_serial_number_read(&aidl_return);
	ALOGD("%s: aidl_return.c_str:[%s]", __func__, aidl_return.c_str());
	hidl_string result(aidl_return);
    _hidl_cb(result);
    return Void();
}

Return<void> ScanService::cam_scm_fw_version_read(cam_scm_fw_version_read_cb _hidl_cb) {
	ALOGD("%s:", __func__);
	std::string aidl_return = "";
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return Void();
	}
	mService->customize_cam_scm_fw_version_read(&aidl_return);
	ALOGD("%s: aidl_return.c_str:[%s]", __func__, aidl_return.c_str());
	hidl_string result(aidl_return);
    _hidl_cb(result);
    return Void();
}

Return<int32_t> ScanService::cam_ccm_move_Focus(int32_t distancemm) {
	int32_t ret = 0;
	ALOGD("%s: distancemm:[%d]", __func__, distancemm);
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_ccm_move_Focus(distancemm, &ret);
	return ret;
}

Return<int32_t> ScanService::cam_ccm_flash(int32_t flash_status, int32_t timeout) {
	int32_t ret = 0;
	ALOGD("%s: flash_status:[%d] timeout:[%d]", __func__, flash_status, timeout);
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_ccm_flash(flash_status, timeout, &ret);
	return ret;
}

Return<int32_t> ScanService::cam_ccm_switch_size(int32_t type) {
	int32_t ret = 0;
	ALOGD("%s: type:[%d]", __func__, type);
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_ccm_switch_size(type, &ret);
	return ret;
}

Return<int32_t> ScanService::cam_aim_sus(uint8_t gpio_signal){
	int32_t ret = 0;
	ALOGD("%s: gpio_signal:[%d]", __func__, gpio_signal);
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_aim_sus(gpio_signal, &ret);
	return ret;
}

Return<int32_t> ScanService::cam_wake(uint8_t gpio_signal){
	int32_t ret = 0;
	ALOGD("%s: gpio_signal:[%d]", __func__, gpio_signal);
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_cam_wake(gpio_signal, &ret);
	return ret;
}

Return<int32_t> ScanService::non_volatail_param_write(const hidl_string& param){
	int32_t ret = 0;
	ALOGD("%s: param:[%s]", __func__, param.c_str());
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_non_volatail_param_write(param, &ret);
	return ret;
}

Return<void> ScanService::non_volatail_param_read(non_volatail_param_read_cb _hidl_cb){
	ALOGD("%s:", __func__);
	std::string aidl_return = "";
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return Void();
	}
	mService->customize_non_volatail_param_read(&aidl_return);
	ALOGD("%s: aidl_return.c_str:[%s]", __func__, aidl_return.c_str());
	hidl_string result(aidl_return);
    _hidl_cb(result);
    return Void();
}

Return<int32_t> ScanService::quad_cam_open(){
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	if(mServiceCallback == NULL){
		mServiceCallback = getCustomizeScanServiceCallback();
	}
	
	ALOGD("%s: 1 customize_quad_cam_open start", __func__);
	mService->customize_quad_cam_open(&ret);
	ALOGD("%s: 1 customize_quad_cam_open end ret:[%d]", __func__, ret);
	if(mServiceCallback != NULL){
		ALOGD("%s: set callback start", __func__);
		mService->customize_setCallback(mServiceCallback);
		ALOGD("%s: set callback end", __func__);
	}else return -1;
    return ret;
}

Return<int32_t> ScanService::quad_cam_close(){
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	
	ALOGD("%s: 1 customize_quad_cam_close start", __func__);
	mService->customize_quad_cam_close(&ret);
	ALOGD("%s: 1 customize_quad_cam_close end ret:[%d]", __func__, ret);
    return ret;
}

Return<int32_t> ScanService::quad_cam_suspend(){
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	
	ALOGD("%s: 1 customize_quad_cam_suspend start", __func__);
	mService->customize_quad_cam_suspend(&ret);
	ALOGD("%s: 1 customize_quad_cam_suspend end ret:[%d]", __func__, ret);
    return ret;
}

Return<int32_t> ScanService::quad_cam_resume(){
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	
	ALOGD("%s: 1 customize_quad_cam_resume start", __func__);
	mService->customize_quad_cam_resume(&ret);
	ALOGD("%s: 1 customize_quad_cam_resume end ret:[%d]", __func__, ret);
    return ret;
}

Return<int32_t> ScanService::quad_cam_scm_capture(){
	ALOGD("%s:", __func__);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	
	ALOGD("%s: 1 customize_quad_cam_capture start", __func__);
	mService->customize_quad_cam_capture(&ret);
	ALOGD("%s: 1 customize_quad_cam_capture end ret:[%d]", __func__, ret);
    return ret;
}

Return<int32_t> ScanService::quad_cam_scm_i2c_write(uint8_t slaveAddress, uint32_t AddrType,uint32_t DataType, const hidl_vec<reg_array>& SendData,uint32_t length){
	ALOGD("%s: slaveAddress:[%u]", __func__, slaveAddress);
	for(auto data : SendData){
    	ALOGD("%s: slaveAddress[%u]  reg_addr:[%u] reg_data:[%u] delay:[%u] data_mask:[%u] length:[%u]", __func__, slaveAddress, data.reg_addr, data.reg_data, data.delay, data.data_mask, length);
	}
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	Reg_data aidlSendData;
	aidlSendData.addr_type = AddrType;
	aidlSendData.data_type = DataType;
	vector<Reg_value> aidlData;
	for(auto data : SendData){
		Reg_value item;
		item.reg_addr = data.reg_addr;
		item.reg_data = data.reg_data;
		item.delay = data.delay;
		item.data_mask = data.data_mask;
		aidlData.push_back(item);
	}
	aidlSendData.regValueArray = aidlData;
	mService->customize_quad_cam_scm_i2c_write((int8_t)slaveAddress, aidlSendData, (int32_t)length, (int *)&ret);
	return ret;
}

Return<uint32_t> ScanService::quad_cam_scm_i2c_read(uint8_t slaveAddress,uint32_t SendData,uint32_t AddrType,uint32_t DataType){
	ALOGD("%s:  SendData:[%u] AddrType:[%u] DataType:[%u] ", __func__, SendData, AddrType, DataType);
	uint32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_quad_cam_scm_i2c_read((int8_t)slaveAddress, (int)SendData, (int)AddrType, (int)DataType, (int *)&ret);
	ALOGD("%s: ret:[%u]", __func__, ret);
	return ret;
}

Return<int32_t> ScanService::quad_cam_scm_return_buffer(int32_t idx){
	ALOGD("%s: idx:[%d]", __func__, idx);
	int32_t ret = 0;
	if(initCustomizeScanService() == -1){
		ALOGD("%s: init Customize Scan Service FAILED.", __func__);
		return -1;
	}
	mService->customize_quad_cam_scm_return_buffer(idx, &ret);
	return ret;
}

ScanService::ScanService() {
	ALOGD("%s, start",  __func__);
	mService = NULL;
	mServiceCallback = NULL;
}

ScanService::~ScanService(){
	ALOGD("%s, destroy",  __func__);
	mService = NULL;
	mServiceCallback = NULL;
}

}  // namespace implementation
}  // namespace V1_0
}  // namespace ScanService
}  // namespace hardware
}  // namespace meig
}  // namespace vendor
