#include "CameraManger.h"

typedef struct {
  const char* mCameraId;
  ACameraManager* mCameraManager;
  ACameraDevice *mDevice;
  ACameraIdList* mCameraIdList;
  ACaptureRequest *mCaptureRequest;
  AImageReader* mImgReader;
  native_handle_t* mImgReaderAnw;
  ACameraOutputTarget* mReqImgReaderOutput;
  ACaptureSessionOutputContainer* mOutputs;
  ACaptureSessionOutput* mImgReaderOutput;
  ACameraCaptureSession* mSession;
  ACameraDevice_StateCallbacks mDeviceCb;
  AImageReader_ImageListener mReaderAvailableCb;
  ACameraCaptureSession_stateCallbacks mSessionCb;
  ACameraCaptureSession_captureCallbacks mResultCb;
  Datacallback mDataCallbackFun;
  Exposurecallback mExposurecallbackFun;
  std::condition_variable mBufferCondition;
  std::mutex mMutex;
  bool mIsCapture;
  bool mIsRepeating;
  bool mIsSetBrightness;
  int32_t mBrightnessLevel;
  nsecs_t mSetBrightnessStartTime;
  nsecs_t mStartSnapShortTime;
  int32_t mCallbackFrameCount;
  bool mCameraWorking;
  int32_t mFrameNum;
}CameraNativeManager;

camera_status_t openCamera(int32_t CameraId);
camera_status_t createImageReader(int32_t CameraId,int32_t width,int32_t height,int32_t format);
camera_status_t createCaptureRequest(int32_t CameraId);
camera_status_t createSession(int32_t CameraId);
camera_status_t startCapture(int32_t CameraId,bool repeating);
camera_status_t stopNativeCapture(int32_t CameraId);
void destorySession(int32_t CameraId);
void destoryImageReader(int32_t CameraId);
camera_status_t closeCamera(int32_t CameraId);
void SnapShortStart(int32_t CameraId,Datacallback cb,nsecs_t starttime,int32_t frameNum);
void WaitImage(int32_t CameraId,uint32_t timeoutSec);
camera_status_t SetCameraBrightness(int32_t CameraId,int32_t level,Exposurecallback cb);
camera_status_t CameraMoveFocus(int32_t CameraId,float value);
camera_status_t SetCameraFlashLight(int32_t CameraId,bool on);
void WaitSCM1SCM2FristFrameDone();
void WaitCCMFristFrameDone();