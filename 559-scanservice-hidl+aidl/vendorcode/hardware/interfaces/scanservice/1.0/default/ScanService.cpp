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

#define LOG_TAG "ScanServiceHidl"

#include "ScanService.h"

#include <log/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>

namespace vendor {
namespace scan {
namespace hardware {
namespace scanservice {
namespace V1_0 {
namespace implementation {
//sp<IScanServiceCallback> ScanService::mCallback = NULL;
sp<IScanServiceCallback> mCallback = NULL;
int32_t mCameraId0 = 0;
int32_t mCameraId1 = 0;
int32_t mCameraId2 = 0;
//static void OnDataCallBackTestCamera0(AImageReader *reader) {
void OnDataCallBackTestCamera0(AImageReader *reader) {
	ALOGD("%s: Get camera 0 Capture imge ", __func__);
	if(mCallback != NULL) {
 		//mCallback->onNotify(mCameraId0);
		mCallback->onNotifyCamera0Data(mCameraId0);
 	}
}

//static void OnDataCallBackTestCamera1(AImageReader *reader) {
void OnDataCallBackTestCamera1(AImageReader *reader) {
	ALOGD("%s: Get camera 1 Capture imge ", __func__);
	if(mCallback != NULL) {
 		//mCallback->onNotify(mCameraId1);
		mCallback->onNotifyCamera1Data(mCameraId1);
 	}

}

//static void OnDataCallBackTestCamera2(AImageReader *reader) {
void OnDataCallBackTestCamera2(AImageReader *reader) {
	ALOGD("%s: Get camera 2 Capture imge ", __func__);
	if(mCallback != NULL) {
 		//mCallback->onNotify(mCameraId2);
		mCallback->onNotifyCamera2Data(mCameraId2);
 	}
}

void OnExposureCallback(int64_t exposuretime,int32_t iso) {
	ALOGD("CCM camera exprosure callback exposuretime=%ld iso=%d .....",exposuretime,iso);
	if(mCallback != NULL) {
		mCallback->onNotifyExposure(exposuretime, iso);
 	}
}

Return<void> ScanService::open(int32_t cameraId, int32_t width, int32_t height, int32_t format){
	ALOGD("%s: cameraId:[ %d ], width:[ %d ] , height:[ %d ] , format:[ %d ] \n", __func__, cameraId, width, height, format);
	if( pNode == NULL){
		ALOGD("%s: pNode == NULL, and exit!", __func__);
		return Void();
	}
	switch(cameraId){
		case 0:
			mCameraId0 = cameraId;
			break;
		case 1:
			mCameraId1 = cameraId;
			break;
		default:
			mCameraId2 = cameraId;
			break;
	}
	
	pNode->Open(cameraId, width, height, format);
	return Void();
}

Return<void> ScanService::close(int32_t cameraId){
	ALOGD("%s: cameraId:[ %d ] \n", __func__, cameraId);
	if( pNode == NULL){
		ALOGD("%s: pNode == NULL, and exit!", __func__);
		return Void();
	}
	pNode->Close(cameraId);
	return Void();
}

Return<void> ScanService::resume(int32_t cameraId){
	ALOGD("%s: cameraId:[ %d ] \n", __func__, cameraId);
	if( pNode == NULL){
		ALOGD("%s: pNode == NULL, and exit!", __func__);
		return Void();
	}
	pNode->Resume(cameraId);
	return Void();
}

Return<void> ScanService::suspend(int32_t cameraId){
	ALOGD("%s: cameraId:[ %d ] \n", __func__, cameraId);
	if( pNode == NULL){
		ALOGD("%s: pNode == NULL, and exit!", __func__);
		return Void();
	}
	pNode->Suspend(cameraId);
	return Void();
}

Return<void> ScanService::capture(int32_t cameraId){
	ALOGD("%s: cameraId:[ %d ] \n", __func__, cameraId);
	if( pNode == NULL){
		ALOGD("%s: pNode == NULL, and exit!", __func__);
		return Void();
	}
	/*switch(cameraId){
		case 0:
			pNode->Capture(cameraId, OnDataCallBackTestCamera0);
			break;
		case 1:
			pNode->Capture(cameraId, OnDataCallBackTestCamera1);
			break;
		default:
			pNode->Capture(cameraId, OnDataCallBackTestCamera2);
			break;
	}*/
	pNode->CaptureByCustomer(OnDataCallBackTestCamera1, OnDataCallBackTestCamera2, OnExposureCallback, OnDataCallBackTestCamera0);
	return Void();
}

Return<void> ScanService::setParameters(int32_t cameraId,int32_t type,int32_t value){
	ALOGD("%s: cameraId:[ %d ] type:[ %d ] value:[%d]\n", __func__, cameraId, type, value);
	if( pNode == NULL){
		ALOGD("%s: pNode == NULL, and exit!", __func__);
		return Void();
	}
	pNode->SetParameters(cameraId, (camera_parameters_type)type, value);
	return Void();
}

Return<void> ScanService::move_focus(int32_t cameraId,float value){
	ALOGD("%s: cameraId:[ %d ] value:[ %f ]\n", __func__, cameraId, value);
	if( pNode == NULL){
		ALOGD("%s: pNode == NULL, and exit!", __func__);
		return Void();
	}
	pNode->MoveFocus(cameraId, value);
	return Void();
}

Return<void> ScanService::setScanServiceCallback(const sp<IScanServiceCallback>& callback){
	if(mCallback == NULL) {
		ALOGD("setCallback: EXIT! mCallback == NULL");
 	}
	ALOGD("%s: ", __func__);

	mCallback = callback;
	return Void();
}

ScanService::ScanService() {
	ALOGD("%s, start",  __func__);
	CreateCameraInterfaceNode(&pNode);
}

}  // namespace implementation
}  // namespace V1_0
}  // namespace ScanService
}  // namespace hardware
}  // namespace meig
}  // namespace vendor
