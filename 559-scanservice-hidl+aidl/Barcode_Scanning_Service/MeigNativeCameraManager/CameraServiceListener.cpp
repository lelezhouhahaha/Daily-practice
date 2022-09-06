
#include "CameraServiceListener.h"

void CameraServiceListener::onAvailable(void* obj, const char* cameraId)
{
    ALOGI("%s:: Camera %s onAvailable","CameraServiceListener",cameraId);
    if (obj == nullptr) {
        return;
    }
    CameraServiceListener* thiz = reinterpret_cast<CameraServiceListener*>(obj);
    std::lock_guard<std::mutex> lock(thiz->mMutex);
    thiz->mOnAvailableCount++;
    thiz->mAvailableMap[cameraId] = true;
    return;
}

void CameraServiceListener::onUnavailable(void* obj, const char* cameraId) {
    ALOGI("%s:: Camera %s onUnavailable","CameraServiceListener",cameraId);
    if (obj == nullptr) {
        return;
    }
    CameraServiceListener* thiz = reinterpret_cast<CameraServiceListener*>(obj);
    std::lock_guard<std::mutex> lock(thiz->mMutex);
    thiz->mOnUnavailableCount++;
    thiz->mAvailableMap[cameraId] = false;
    return;
}

int CameraServiceListener::getAvailableCount() {
    std::lock_guard<std::mutex> lock(mMutex);
    return mOnAvailableCount;
}

int CameraServiceListener::getUnavailableCount() {
    std::lock_guard<std::mutex> lock(mMutex);
    return mOnUnavailableCount;
}

bool CameraServiceListener::isAvailable(const char* cameraId) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mAvailableMap.count(cameraId) == 0) {
        return false;
    }
    return mAvailableMap[cameraId];
}

void CameraServiceListener::resetCount() {
    std::lock_guard<std::mutex> lock(mMutex);
    mOnAvailableCount = 0;
    mOnUnavailableCount = 0;
    return;
}

