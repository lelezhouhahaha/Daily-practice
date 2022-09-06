#include "CameraNativeManager.h"
#include "CameraI2cControl.h"

static CameraNativeManager mCameraNativeManager[3];
#if 1
static std::condition_variable mWaitScmCameraCallBackCondition;
static std::mutex mWaitScmCameraCallBackMutex;
//static std::queue<int> datat_queue;
static int scmcallbackcount = 0;

static int WaitScmCameraAllDataCallback() {
	std::unique_lock<std::mutex> lk(mWaitScmCameraCallBackMutex);
	mWaitScmCameraCallBackCondition.wait(lk,[]{return scmcallbackcount == 2;});
	return 0;
}

static void SigngalScmCameraDatatCallbackCount(){
	std::lock_guard<std::mutex> lk(mWaitScmCameraCallBackMutex);
	scmcallbackcount++;
	mWaitScmCameraCallBackCondition.notify_all();
}
#endif

static void onDisconnected(void* obj, ACameraDevice* device) {
        ALOGI("Camera %s is disconnected!", ACameraDevice_getId(device));
        return;
}

static void onError(void* obj, ACameraDevice* device, int errorCode) {
        ALOGE("Camera %s receive error %d!", ACameraDevice_getId(device), errorCode);
        return;
}

static void onClosed(void* obj, ACameraCaptureSession *session) {
        // TODO: might want an API to query cameraId even session is closed?
        ALOGI("Session %p is closed!", session);
}
#define CCM_EXPOSURE_EFFECT_TIME 2
#define SCM_EXPOSURE_EFFECT_TIME 1

static void ImageCallback(AImageReader *reader,CameraNativeManager *Manager) {
    media_status_t ret;
    AImage* image = NULL;
    int32_t format = -1;
    int32_t width = -1, height = -1;
    char dumpFilePath[512];
    int32_t numPlanes;
    int32_t i = 0;
	int32_t frameNum = 0;

    uint8_t *data[3];
    int32_t datalen[3];
    int32_t pixelStride[3];
    int32_t rowStride[3];

    if (!reader || !Manager) {
      return;
    }

    std::lock_guard<std::mutex> lock(Manager->mMutex);
	//frameNum = 0;//GetSensorFrameNum(atoi(Manager->mCameraId));
    ret = AImageReader_acquireNextImage(reader, &image);
    if (ret != AMEDIA_OK) {
        ALOGE("CameraNativeManager::ImageCallback Failed to get image");
        return;
    }

    ret = AImage_getFormat(image, &format);
    if (ret != AMEDIA_OK || format == -1) {
        AImage_delete(image);
        ALOGE("CameraNativeManager:: get format for image %p failed! ret: %d, format %d",image, ret, format);
        return;
    }

    ret = AImage_getWidth(image, &width);
    if (ret != AMEDIA_OK || width <= 0) {
        AImage_delete(image);
        ALOGE("%s: get width for image %p failed! ret: %d, width %d",
                     __FUNCTION__, image, ret, width);
        return;
    }

    ret = AImage_getHeight(image, &height);
    if (ret != AMEDIA_OK || height <= 0) {
        AImage_delete(image);
        ALOGE("%s: get height for image %p failed! ret: %d, width %d",
                     __FUNCTION__, image, ret, height);
        return;
    }

    ret  = AImage_getNumberOfPlanes(image,&numPlanes);

    for (i = 0; i < numPlanes; i++) {
      AImage_getPlaneData(image,i,&data[i],&datalen[i]);
      AImage_getPlaneRowStride(image,i,&rowStride[i]);
      AImage_getPlanePixelStride(image,i,&pixelStride[i]);
#if 0
      ALOGD("%s,+++++ 11111111 Camera %s Get Capture imge Plane[%d] [%dx%d] rowStride[%d] pixelStride[%d] length[%d]\n", __FUNCTION__,Manager->mCameraId,
           i,width,height,
           rowStride[i],
           pixelStride[i],
           datalen[i]);
#endif
    }
	//ALOGI("Get camera %s  frameNum = %d",Manager->mCameraId,frameNum);
	if (strcmp(Manager->mCameraId,"0") && Manager->mFrameNum == 0 ) {
		SigngalScmCameraDatatCallbackCount();
	}
	Manager->mFrameNum++;
    Manager->mBufferCondition.notify_one();
    if (Manager->mIsCapture && Manager->mDataCallbackFun) {
#if 0
		int32_t effect_time;
		bool skip = false;
		bool CanCallBack = false;
		if (atoi(Manager->mCameraId) > 0) {
			effect_time = SCM_EXPOSURE_EFFECT_TIME - 1;
			skip = true;
		} else {
			effect_time = CCM_EXPOSURE_EFFECT_TIME -1;
		}
		if (Manager->mFrameNum == 0xFF) {
			int32_t newfreamNum = 0;
			if (frameNum - newfreamNum > effect_time) {
				CanCallBack = true;
			}
		} else if ((frameNum - Manager->mFrameNum) > effect_time){
			CanCallBack = true;
		}
		skip= true;
#endif
		if (1) {
			Manager->mDataCallbackFun(reader);
			ALOGI("[CameraPerformance] end time of camera %s image data transport count=%d timediff=%ldms....",
		                 Manager->mCameraId, Manager->mCallbackFrameCount++,(systemTime() - Manager->mStartSnapShortTime)/1000000);
			//if (strcmp(Manager->mCameraId,"0")) {
			//	SigngalScmCameraDatatCallbackCount();
			//}
			Manager->mIsCapture = false;		
		}
    }
    AImage_delete(image);
}

static void OnCamera0ImageCallback(void* ctx, AImageReader* reader)
{
    media_status_t ret;
    int32_t format = -1;
    AImage* image = nullptr;
    CameraNativeManager* thiz = reinterpret_cast<CameraNativeManager*>(ctx);

    if (ctx == nullptr) {
        return;
    }

    if (reader == nullptr) {
        return;
    }
    ImageCallback(reader,thiz);
}

static void OnCamera1ImageCallback(void* ctx, AImageReader* reader)
{
    media_status_t ret;
    int32_t format = -1;
    AImage* image = nullptr;
    CameraNativeManager* thiz = reinterpret_cast<CameraNativeManager*>(ctx);

    if (ctx == nullptr) {
        return;
    }

    if (reader == nullptr) {
        return;
    }
    ImageCallback(reader,thiz);
}

static void OnCamera2ImageCallback(void* ctx, AImageReader* reader)
{
    media_status_t ret;
    int32_t format = -1;
    AImage* image = nullptr;
    CameraNativeManager* thiz = reinterpret_cast<CameraNativeManager*>(ctx);

    if (ctx == nullptr) {
        return;
    }

    if (reader == nullptr) {
        return;
    }
    ImageCallback(reader,thiz);
}

static CameraNativeManager *GetCameraNativeManager(int32_t CameraId)
{
    if (CameraId == 0) {
        return &mCameraNativeManager[0];
    } else if (CameraId == 1) {
        return &mCameraNativeManager[1];
    } else if (CameraId == 2) {
        return &mCameraNativeManager[2];
    }
    return NULL;
}

camera_status_t openCamera(int32_t CameraId) {
    camera_status_t ret;

    CameraNativeManager *Manager = NULL;

    Manager = GetCameraNativeManager(CameraId);

    if (!Manager) {
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }
	Manager->mCallbackFrameCount = 0;
	Manager->mCameraWorking = false;

    if (Manager->mDevice) {
        ALOGE("Camera %d Cannot open camera before closing previously open one",CameraId);
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }

    Manager->mCameraManager = GetCameraManger();
    if (!Manager->mCameraManager) {
        ALOGE("Camera %d Cannot open camera GetCameraManger failed",CameraId);
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }

    ret = ACameraManager_getCameraIdList(Manager->mCameraManager, &Manager->mCameraIdList);
    if (ret != ACAMERA_OK) {
        ALOGE("Get camera id list failed: ret %d", ret);
        return ret;
    }

    Manager->mCameraId = Manager->mCameraIdList->cameraIds[CameraId];
    if (Manager->mCameraId == nullptr) {
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }

    Manager->mDeviceCb.context = NULL;
    Manager->mDeviceCb.onDisconnected = onDisconnected;
    Manager->mDeviceCb.onError = onError;

    ret = ACameraManager_openCamera(Manager->mCameraManager, Manager->mCameraId, &Manager->mDeviceCb, &Manager->mDevice);
    if (ret != ACAMERA_OK || Manager->mDevice == nullptr) {
        ALOGE("Failed to open camera %s, ret=%d, mDevice=%p.", Manager->mCameraId,ret, Manager->mDevice);
        return ret;
    }
    Manager->mIsCapture = false;
	Manager->mCameraWorking = true;
    return ret;
}

camera_status_t createImageReader(int32_t CameraId,int32_t width,int32_t height,int32_t format) {
    media_status_t ret;

    CameraNativeManager *Manager = NULL;

    Manager = GetCameraNativeManager(CameraId);

    if (!Manager) {
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }

    ret = AImageReader_newWithUsage(width,height,format,AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN,8,&Manager->mImgReader);
    if (ret != AMEDIA_OK || Manager->mImgReader == nullptr) {
        ALOGE("Failed to create new AImageReader, ret=%d, mImgReader=%p", ret, Manager->mImgReader);
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }

    Manager->mReaderAvailableCb.context = (void *)Manager;

    if (!strcmp(Manager->mCameraId,"0")){
        Manager->mReaderAvailableCb.onImageAvailable = OnCamera0ImageCallback;
    } else if(!strcmp(Manager->mCameraId,"1")) {
        Manager->mReaderAvailableCb.onImageAvailable = OnCamera1ImageCallback;
    } else {
        Manager->mReaderAvailableCb.onImageAvailable = OnCamera2ImageCallback;
    }

    ret = AImageReader_setImageListener(Manager->mImgReader, &Manager->mReaderAvailableCb);
    if (ret != AMEDIA_OK) {
        ALOGE("Failed to set image listener");
        goto failed;
    }

    ret = AImageReader_getWindowNativeHandle(Manager->mImgReader, &Manager->mImgReaderAnw);
    if (ret != AMEDIA_OK) {
        ALOGE("Failed to get image reader native window");
        goto failed;
    }
    return ACAMERA_OK;

failed:
    if (Manager->mImgReader) {
        AImageReader_delete(Manager->mImgReader);
        Manager->mImgReader = nullptr;
        Manager->mImgReaderAnw = nullptr;
    }
    return ACAMERA_ERROR_INVALID_PARAMETER;
}

camera_status_t createCaptureRequest(int32_t CameraId) {
    camera_status_t ret;
    CameraNativeManager *Manager = NULL;

    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }

    ret = ACameraDevice_createCaptureRequest(Manager->mDevice, TEMPLATE_PREVIEW, &Manager->mCaptureRequest);
    if (ret != ACAMERA_OK) {
       ALOGE("Camera %s create preview request failed. ret %d",Manager->mCameraId, ret);
    }
    return ret;
}


camera_status_t createSession(int32_t CameraId) {
    camera_status_t ret;

    CameraNativeManager *Manager = NULL;

    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }

    ret = ACaptureSessionOutputContainer_create(&Manager->mOutputs);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %s Create capture session output container failed. ret %d",Manager->mCameraId, ret);
        return ret;
    }

    ret = ACaptureSessionOutput_create(Manager->mImgReaderAnw, &Manager->mImgReaderOutput);
    if (ret != ACAMERA_OK || Manager->mImgReaderOutput == nullptr) {
        ALOGE("Camera %s Session image reader output create fail! ret %d",Manager->mCameraId,ret);
        if (ret == ACAMERA_OK) {
            ret = ACAMERA_ERROR_UNKNOWN; // ret OK but output is null
        }
        return ret;
    }

    ret = ACaptureSessionOutputContainer_add(Manager->mOutputs, Manager->mImgReaderOutput);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %s Session image reader output add failed! ret %d",Manager->mCameraId,ret);
        return ret;
    }

    Manager->mSessionCb.context = NULL;
    Manager->mSessionCb.onClosed = onClosed;
    //Manager->mSessionCb.onReady = onReady;
    //Manager->mSessionCb.onActive = onActive; 

    ret = ACameraDevice_createCaptureSession(Manager->mDevice, Manager->mOutputs, &Manager->mSessionCb, &Manager->mSession);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %s Session image reader create Session failed! ret %d", Manager->mCameraId,ret);
        return ret;
    }

    ret = ACameraOutputTarget_create(Manager->mImgReaderAnw, &Manager->mReqImgReaderOutput);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %s create request reader output target failed. ret %d",
              Manager->mCameraId, ret);
        return ret;
    }

    ret = ACaptureRequest_addTarget(Manager->mCaptureRequest, Manager->mReqImgReaderOutput);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %s create request reader request output failefailed. ret %d",
              Manager->mCameraId, ret);
        return ret;
    }
    return ret;
}

 static void onCaptureCompleted(void* obj, ACameraCaptureSession* /*session*/,
              ACaptureRequest* request, const ACameraMetadata* result) {
	camera_status_t ret = ACAMERA_OK;
	if ((obj == nullptr) || (result == nullptr)) {
		return;
	}
	CameraNativeManager* thiz = reinterpret_cast<CameraNativeManager*>(obj);
	//std::lock_guard<std::mutex> lock(thiz->mMutex);
	ACameraMetadata_const_entry entry;
	int32_t iso;
	int64_t exposuretime;
	int32_t aevalue;
	ret = ACameraMetadata_getConstEntry(result, ACAMERA_CONTROL_AE_EXPOSURE_COMPENSATION, &entry);
	if (entry.count > 0) {
		aevalue = entry.data.i32[0];
		//ALOGI("ACAMERA_CONTROL_AE_EXPOSURE_COMPENSATION=%d",entry.data.i32[0]);
	}
	ret = ACameraMetadata_getConstEntry(result, ACAMERA_SENSOR_EXPOSURE_TIME, &entry);
	if (entry.count > 0) {
		exposuretime = entry.data.i64[0];
		//ALOGI("onCaptureCompleted ACAMERA_SENSOR_EXPOSURE_TIME=%ld",entry.data.i64[0]);
	}
	ret = ACameraMetadata_getConstEntry(result, ACAMERA_SENSOR_SENSITIVITY, &entry);
	if (entry.count > 0) {
		iso = entry.data.i32[0];
		//ALOGI("onCaptureCompleted ACAMERA_SENSOR_SENSITIVITY=%d",entry.data.i32[0]);
	}
	if (thiz->mIsSetBrightness && thiz->mExposurecallbackFun && thiz->mBrightnessLevel == aevalue) {
		ALOGI("[CameraPerformance] start time of ccm exposure callback iso=%d exposuretime=%ld timediff=%ldms.....",iso,exposuretime,(systemTime() - thiz->mSetBrightnessStartTime)/1000000);
		thiz->mExposurecallbackFun(exposuretime,iso);
		thiz->mIsSetBrightness = false;
	}
}

camera_status_t startCapture(int32_t CameraId,bool repeating) {
    int seqId;
    camera_status_t ret = ACAMERA_OK;

    CameraNativeManager *Manager = NULL;

    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }

    if (!Manager->mCaptureRequest || !Manager->mSession) {
        ALOGE("cannot take picture: session %p, Capture request %p",
                    Manager->mSession, Manager->mCaptureRequest);
        return ACAMERA_ERROR_UNKNOWN;
    }
	if (CameraId == 0) {
	    Manager->mResultCb.context = (void *)Manager;
	    //mResultCb.onCaptureProgressed = 
	    Manager->mResultCb.onCaptureCompleted = onCaptureCompleted;
    }
	if (CameraId == 0) {
		uint8_t value =  ACAMERA_CONTROL_AE_MODE_OFF ;
		int64_t time = 5000000;
		ACaptureRequest_setEntry_u8(Manager->mCaptureRequest,ACAMERA_CONTROL_AE_MODE,1,&value);
		ACaptureRequest_setEntry_i64(Manager->mCaptureRequest,ACAMERA_SENSOR_EXPOSURE_TIME,1,&time);
	}

    if (repeating) {
		if (CameraId == 0) {
            ret = ACameraCaptureSession_setRepeatingRequest(
                  Manager->mSession, &Manager->mResultCb, 1, &Manager->mCaptureRequest, &seqId);
		} else {
            ret = ACameraCaptureSession_setRepeatingRequest(
                  Manager->mSession, nullptr, 1, &Manager->mCaptureRequest, &seqId);
		}
    } else {
       ret = ACameraCaptureSession_capture(
                  Manager->mSession, nullptr, 1, &Manager->mCaptureRequest, &seqId);
    }

    Manager->mIsRepeating = repeating;
    //WaitImage(5);
    return ret;
}
camera_status_t stopNativeCapture(int32_t CameraId) {
    camera_status_t ret = ACAMERA_OK;
    CameraNativeManager *Manager = NULL;

    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }
    if (Manager->mSession) {
        //ACameraCaptureSession_abortCaptures(Manager->mSession);
        ret = ACameraCaptureSession_stopRepeating(Manager->mSession);
    }
    return ret;
}


void destorySession(int32_t CameraId) {
    CameraNativeManager *Manager = NULL;

    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ;
    }

    if (Manager->mSession != nullptr) {
        ACameraCaptureSession_close(Manager->mSession);
        Manager->mSession = nullptr;
    }

    //if (Manager->mImgReaderOutput && Manager->mOutputs) {
     //   ACaptureSessionOutputContainer_remove(Manager->mOutputs,Manager->mImgReaderOutput);
    //}

    if (Manager->mImgReaderOutput) {
        ACaptureSessionOutput_free(Manager->mImgReaderOutput);
        Manager->mImgReaderOutput = nullptr;
    }

    if (Manager->mOutputs) {
        ACaptureSessionOutputContainer_free(Manager->mOutputs);
        Manager->mOutputs = nullptr;
    }

    if (Manager->mReqImgReaderOutput) {
        ACameraOutputTarget_free(Manager->mReqImgReaderOutput);
        Manager->mReqImgReaderOutput = nullptr;  
    }

    if (Manager->mCaptureRequest) {
        ACaptureRequest_free(Manager->mCaptureRequest);
        Manager->mCaptureRequest = nullptr;
    }
}

void destoryImageReader(int32_t CameraId) {
    CameraNativeManager *Manager = NULL;

    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ;
    }

    if (Manager->mImgReader) {
        AImageReader_delete(Manager->mImgReader);
        Manager->mImgReader = nullptr;
        Manager->mImgReaderAnw = nullptr;
    }
}

camera_status_t closeCamera(int32_t CameraId) {
    camera_status_t ret;
    CameraNativeManager *Manager = NULL;
    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ACAMERA_ERROR_INVALID_PARAMETER;
    }
    if (Manager->mCameraIdList) {
        ACameraManager_deleteCameraIdList(Manager->mCameraIdList);
        Manager->mCameraIdList = nullptr;
    }
    ret = ACameraDevice_close(Manager->mDevice);
    Manager->mDevice = nullptr;
	Manager->mCameraWorking = false;
    return ret;
}

void WaitImage(int32_t CameraId,uint32_t timeoutSec)
{
    CameraNativeManager *Manager = NULL;
    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ;
    }

    std::unique_lock<std::mutex> l(Manager->mMutex);
    auto timeout = std::chrono::system_clock::now() + std::chrono::seconds(timeoutSec);
    if (std::cv_status::no_timeout == Manager->mBufferCondition.wait_until(l, timeout)) {
        return;
    } else {
        ALOGE("Camera %s Wait camera frame timeout",Manager->mCameraId);
    }
}

void WaitSCM1SCM2FristFrameDone() {
	WaitScmCameraAllDataCallback();
}

void WaitCCMFristFrameDone() {
	WaitImage(0,5);
}

void SnapShortStart(int32_t CameraId,Datacallback cb,nsecs_t starttime,int32_t frameNum) {
    CameraNativeManager *Manager = NULL;
    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ;
    }
	//if (CameraId == 0 && GetCameraNativeManager(1)->mCameraWorking && GetCameraNativeManager(2)->mCameraWorking) {
		//ALOGI("[CameraPerformance] wait SCM1 && SCM2 data callback");
		//WaitScmCameraAllDataCallback();
		//scmcallbackcount = 0;
		//starttime = systemTime();
		//ALOGI("[CameraPerformance] SCM1 && SCM2 captute done,start capture CCM camera");
	//}
    std::lock_guard<std::mutex> lock(Manager->mMutex);
	//Manager->mFrameNum = frameNum;
	Manager->mStartSnapShortTime = starttime;//systemTime();
    Manager->mIsCapture = true;
    Manager->mDataCallbackFun = cb;
}
/*
-12:  16666666  191
-9:   20000000  197
-6:   20000000  294
0:    33333333  100
6:    30000000  903
9:    40000000  1088
12:   40000000  1600
*/
camera_status_t SetCameraBrightness(int32_t CameraId,int32_t level,Exposurecallback cb) {
    camera_status_t ret = ACAMERA_OK;
    float brigntnessLevelarry[9] = {-2,-1.5,-1,-0.5,0,0.5,1,1.5,2};
    int32_t levelvalue = 0;
    int seqId;
    CameraNativeManager *Manager = NULL;
	char value[PROPERTY_VALUE_MAX] = {0};
    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ACAMERA_ERROR_UNKNOWN;
    }
	Manager->mSetBrightnessStartTime = systemTime();

    if (level > BRIGHTNESS_LEVEL_MAX || level < BRIGHTNESS_LEVEL_0) {
        return ACAMERA_ERROR_UNKNOWN;
    }

    levelvalue = brigntnessLevelarry[level] * 6;
	sprintf(value,"%d",levelvalue);
	ALOGI("Camera %d SetCameraBrightness level=%d value=%s",CameraId,levelvalue,value);
    Manager->mExposurecallbackFun = cb;
	Manager->mIsSetBrightness = true;
	Manager->mBrightnessLevel = levelvalue;
/*
	property_set("persist.vendor.camera.aecompenstion", value);
*/
    ret = ACaptureRequest_setEntry_i32(Manager->mCaptureRequest,ACAMERA_CONTROL_AE_EXPOSURE_COMPENSATION,1,&levelvalue);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d SetCameraBrightness failed",CameraId);
    }
    ret = ACameraCaptureSession_setRepeatingRequest(
                  Manager->mSession, nullptr, 1, &Manager->mCaptureRequest, &seqId);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d SetCameraBrightness setRepeatingRequest failed",CameraId);
    }
    return ret;
}

camera_status_t CameraMoveFocus(int32_t CameraId,float value) {
    CameraNativeManager *Manager = NULL;
    camera_status_t ret = ACAMERA_OK;
    int seqId;
    uint8_t mode = ACAMERA_CONTROL_AF_MODE_OFF;

    if (CameraId != 0) {
        ALOGI("Camera %d not support move focus",CameraId);
        return ACAMERA_ERROR_UNKNOWN;
    }
    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ACAMERA_ERROR_UNKNOWN;
    }
    if (value > 10.0) {
        value = 10.0;
    } else if (value < 0.0) {
        value = 0.0;
    }
    ALOGI("Camera %d CameraMoveFocus value=%f",CameraId,value);

    ret = ACaptureRequest_setEntry_u8(Manager->mCaptureRequest,ACAMERA_CONTROL_AF_MODE,1,&mode);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d CameraMoveFocus set af mode failed",CameraId);
    }
    ret = ACaptureRequest_setEntry_float(Manager->mCaptureRequest,ACAMERA_LENS_FOCUS_DISTANCE,1,&value);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d CameraMoveFocus set focus distance failed",CameraId);
    }
    ret = ACameraCaptureSession_setRepeatingRequest(
                  Manager->mSession, nullptr, 1, &Manager->mCaptureRequest, &seqId);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d SetCameraBrightness setRepeatingRequest failed",CameraId);
    }
    return ret;
}

camera_status_t SetCameraFlashLight(int32_t CameraId,bool on) {
    CameraNativeManager *Manager = NULL;
    camera_status_t ret = ACAMERA_OK;
    int seqId;
    uint8_t levelvalue = ACAMERA_FLASH_MODE_OFF;
 
    if (CameraId != 0) {
        ALOGI("Camera %d not support set falshlight",CameraId);
        return ACAMERA_ERROR_UNKNOWN;
    }
    Manager = GetCameraNativeManager(CameraId);
    if (!Manager) {
        return ACAMERA_ERROR_UNKNOWN;
    }
    if (on) {
        levelvalue = ACAMERA_FLASH_MODE_TORCH;
    } else {
        levelvalue = ACAMERA_FLASH_MODE_OFF;
    }


    ALOGI("Camera %d SetCameraFlashLight %s",CameraId,on?"on":"off");

    ret = ACaptureRequest_setEntry_u8(Manager->mCaptureRequest,ACAMERA_FLASH_MODE,1,&levelvalue);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d SetCameraFlashLight failed",CameraId);
    }
    ret = ACameraCaptureSession_setRepeatingRequest(
                  Manager->mSession, nullptr, 1, &Manager->mCaptureRequest, &seqId);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d SetCameraFlashLight setRepeatingRequest failed",CameraId);
    }

    return ret;
}
