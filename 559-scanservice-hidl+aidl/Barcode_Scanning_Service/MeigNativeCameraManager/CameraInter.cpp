#include "CameraNativeManager.h"

//static CameraNativeManager mCameraNativeManager[3];

static bool RegisterCameraServers()
{
    ALOGI("RegisterCameraServers ++++++");
    return CreateCameraManger();
    
}

static void DestoryCameraServers()
{
    ALOGI("DestoryCameraManger ++++++");
    DestoryCameraManger();
}

static bool Open(int32_t CameraId,int32_t width,int32_t height,int32_t format) {
    camera_status_t ret;
    char value[PROPERTY_VALUE_MAX] = {0};
    nsecs_t strat_time = systemTime();

    ALOGI("Camera %d Open ++++++",CameraId);

    ret = openCamera(CameraId);

    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d OpenCamera failed. ret %d",CameraId, ret);
        return false;
    }

    ret = createImageReader(CameraId,width,height,format);

    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d createImageReader failed. ret %d",CameraId, ret);
        return false;
    }
    ret = createCaptureRequest(CameraId);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d createCaptureRequest failed. ret %d",CameraId, ret);
        return false;
    }

    ret = createSession(CameraId);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d createSession failed. ret %d",CameraId, ret);
        return false;
    }

    property_get("persist.vendor.camera.capture.repeating", value, "1");
    if (atoi(value) == 1) {
        ALOGI("Camera %d Capture repeating",CameraId);
        ret = startCapture(CameraId,true);
    } else {
        ALOGI("Camera %d Capture no repeating",CameraId);
        ret = startCapture(CameraId,false);
    }

    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d startCapture failed. ret %d",CameraId, ret);
        return false;
    }

    WaitImage(CameraId,5);

    ret = stopNativeCapture(CameraId);
    ALOGI("Camera %d Open take time=%llums-----",CameraId,(systemTime() - strat_time)/1000000);
    return true;
}

static bool Close(int32_t CameraId)
{
    camera_status_t ret;
    nsecs_t strat_time = systemTime();
    ALOGI("Camera %d Close ++++++",CameraId);

    stopNativeCapture(CameraId);
    destorySession(CameraId);
    destoryImageReader(CameraId);

    ret = closeCamera(CameraId);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d Close failed. ret %d",CameraId, ret);
        return false;
    }
    ALOGI("Camera %d Close take time=%llums-----",CameraId,(systemTime() - strat_time)/1000000);
    return true;
}
/*
static bool ConfigureStreams(int32_t CameraId,int32_t width,int32_t height,int32_t format)
{
    camera_status_t ret;
    CameraNativeManager *ndkCamera = nullptr;
    nsecs_t strat_time = systemTime();
    ALOGI("Camera %d ConfigureStreams size:[%dx%d] format:%d++++++",CameraId,width,height,format);
    if (CameraId == 0) {
        ndkCamera = &mCameraNativeManager[0];
    } else if (CameraId == 1) {
        ndkCamera = &mCameraNativeManager[1];
    } else {
        ndkCamera = &mCameraNativeManager[2];
    }

    ret = ndkCamera->createImageReader(width,height,format);

    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d createImageReader failed. ret %d",CameraId, ret);
        return false;
    }
    ret = ndkCamera->createCaptureRequest();
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d createCaptureRequest failed. ret %d",CameraId, ret);
        return false;
    }
    ret = ndkCamera->createSession();
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d createSession failed. ret %d",CameraId, ret);
        return false;
    }
    ALOGI("Camera %d ConfigureStreams take time=%llums-----",CameraId,(systemTime() - strat_time)/1000000);
    return true;
}

static bool DeleteStreams(int32_t CameraId)
{
    camera_status_t ret;
    CameraNativeManager *ndkCamera = nullptr;
    nsecs_t strat_time = systemTime();
    ALOGI("Camera %d DeleteStreams ++++++",CameraId);
    if (CameraId == 0) {
        ndkCamera = &mCameraNativeManager[0];
    } else if (CameraId == 1) {
        ndkCamera = &mCameraNativeManager[1];
    } else {
        ndkCamera = &mCameraNativeManager[2];
    }
    ndkCamera->destorySession();
    ndkCamera->destoryImageReader();
    ALOGI("Camera %d DeleteStreams take time=%llums-----",CameraId,(systemTime() - strat_time)/1000000);
    return true;
}

static bool setCallBack(int32_t CameraId,Datacallback cb){
    CameraNativeManager *ndkCamera = nullptr;
    nsecs_t strat_time = systemTime();

    ALOGI("Camera %d setCallBack ++++++",CameraId);
    if (CameraId == 0) {
        ndkCamera = &mCameraNativeManager[0];
    } else if (CameraId == 1) {
        ndkCamera = &mCameraNativeManager[1];
    } else {
        ndkCamera = &mCameraNativeManager[2];
    }
    ndkCamera->SetCallback(cb);
    ALOGI("Camera %d setCallBack take time=%llums------",CameraId,(systemTime() - strat_time)/1000000);
    return true;
}
*/

static bool Capture(int32_t CameraId)
{
    camera_status_t ret;
    char value[PROPERTY_VALUE_MAX] = {0};
    nsecs_t strat_time = systemTime();

    ALOGI("Camera %d Capture ++++++",CameraId);

    property_get("persist.vendor.camera.capture.repeating", value, "1");
    if (atoi(value) == 1) {
        ALOGI("Camera %d Capture repeating",CameraId);
        ret = startCapture(CameraId,true);
    } else {
        ALOGI("Camera %d Capture no repeating",CameraId);
        ret = startCapture(CameraId,false);
    }

    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d startCapture failed. ret %d",CameraId, ret);
        return false;
    }
    ALOGI("Camera %d Capture take time=%llums------",CameraId,(systemTime() - strat_time)/1000000);
	//if (CameraId == 0) {
		//WaitImage(CameraId,5);
	//
    return true;
}

static bool stopCapture(int32_t CameraId)
{
    camera_status_t ret;
    nsecs_t strat_time = systemTime();
    ALOGI("Camera %d stopCapture ++++++",CameraId);

    ret = stopNativeCapture(CameraId);
    if (ret != ACAMERA_OK) {
        ALOGE("Camera %d stopCapture failed. ret %d",CameraId, ret);
        return false;
    }
    ALOGI("Camera %d stopCapture take time=%llums------",CameraId,(systemTime() - strat_time)/1000000);
    return true;
}

static bool SnapShort(int32_t CameraId,Datacallback cb,int32_t frameNum){
    nsecs_t strat_time = systemTime();

    ALOGI("Camera %d SnapShort ++++++",CameraId);

    SnapShortStart(CameraId,cb,strat_time,frameNum);
    ALOGI("Camera %d SnapShort take time=%llums------",CameraId,(systemTime() - strat_time)/1000000);
    return true;
}

static bool SetCameraParameters(int32_t CameraId,camera_parameters_type type,int32_t value,Exposurecallback cb) {
    camera_status_t ret;
    if (type == SET_BRIGHTNESS) {
        ret = SetCameraBrightness(CameraId,value,cb);
        if (ret != ACAMERA_OK) {
            return false;
        }
    } else if (type == SET_FALSHLIGHT) {
        if (value == FLASHLIGHT_ON) {
            SetCameraFlashLight(CameraId,true);
        } else {
            SetCameraFlashLight(CameraId,false);
        }
    }

    return true;
}

static bool SetCameraFocusDistance(int32_t CameraId,float value) {
    camera_status_t ret = CameraMoveFocus(CameraId,value);
    if (ret != ACAMERA_OK) {
        return false;
    }
    return true;
}
static void WaitSCM1SCM2FristFrame() {
	WaitSCM1SCM2FristFrameDone();
}

static void WaitCCMFristFrame() {
	WaitCCMFristFrameDone();
}

bool CreateCameraHandle(CameraHandle **pHande)
{
    CameraHandle *pTempCtrl = NULL;
    ALOGI("CreateCameraHandle ++++++");
    pTempCtrl = (CameraHandle *)malloc(sizeof(CameraHandle));
    if (pTempCtrl == NULL) {
        return false;
    }

    pTempCtrl->RegisterCameraServers = RegisterCameraServers;
    pTempCtrl->DestoryCameraServers = DestoryCameraServers;
    pTempCtrl->Open = Open;
    pTempCtrl->Close = Close;
   // pTempCtrl->ConfigureStreams = ConfigureStreams;
  //  pTempCtrl->DeleteStreams = DeleteStreams;
    pTempCtrl->Capture = Capture;
    pTempCtrl->stopCapture = stopCapture;
   // pTempCtrl->setCallBack = setCallBack;
    pTempCtrl->SnapShort = SnapShort;
    pTempCtrl->SetCameraParameters = SetCameraParameters;
    pTempCtrl->SetCameraFocusDistance = SetCameraFocusDistance;
	pTempCtrl->WaitSCM1SCM2FristFrame = WaitSCM1SCM2FristFrame;
	pTempCtrl->WaitCCMFristFrame = WaitCCMFristFrame;

    *pHande = pTempCtrl;

    ALOGI("CreateCameraHandle ------");
    return true;
}

bool DestoryCameraHandle(CameraHandle *pHande)
{
    ALOGI("DestoryCameraHandle ++++++");
    if (pHande) {
        free(pHande);
        pHande = NULL;
    }
    ALOGI("DestoryCameraHandle ------");
    return true;
}