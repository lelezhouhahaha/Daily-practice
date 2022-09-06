
#include "CameraManger.h"


static ACameraManager* mCameraManager = nullptr;
static ACameraManager_AvailabilityCallbacks mServiceCb;
static CameraServiceListener mServiceListener;

bool CreateCameraManger(){
    camera_status_t ret;

    if (!mCameraManager) {
        mCameraManager = ACameraManager_create();
    }
    if (!mCameraManager)
        return false;

    mServiceListener.resetCount();

    mServiceCb.context = &mServiceListener;
    mServiceCb.onCameraAvailable = mServiceListener.onAvailable;
    mServiceCb.onCameraUnavailable = mServiceListener.onUnavailable;

    ret = ACameraManager_registerAvailabilityCallback(mCameraManager, &mServiceCb);
    if (ret != ACAMERA_OK) {
        ALOGE("Register availability callback failed: ret %d", ret);
        ACameraManager_delete(mCameraManager);
        mCameraManager = nullptr;
        return false;
    }
    return true;
}

void DestoryCameraManger(){
    if (!mCameraManager) {
        ACameraManager_unregisterAvailabilityCallback(mCameraManager, &mServiceCb);
        ACameraManager_delete(mCameraManager);
        mCameraManager = nullptr;
    }
    mServiceListener.resetCount();
}

ACameraManager* GetCameraManger(){
    return mCameraManager;
}

