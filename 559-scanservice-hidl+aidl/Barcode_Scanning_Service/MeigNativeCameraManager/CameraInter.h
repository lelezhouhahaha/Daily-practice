
#include <stdatomic.h>
#include <pthread.h>
#include "media/NdkImage.h"
#include "media/NdkImageReader.h"
#include "Camera.h"
//typedef void(*Datacallback)(AImageReader *reader);

typedef struct CameraHandle_t {
    bool (*RegisterCameraServers)(void);
    void (*DestoryCameraServers)(void);
    bool (*Open)(int32_t CameraId,int32_t width,int32_t height,int32_t format);
    bool (*Close)(int32_t CameraId);
   // bool (*ConfigureStreams)(int32_t CameraId,int32_t width,int32_t height,int32_t format);
    //bool (*DeleteStreams)(int32_t CameraId);
    bool (*Capture)(int32_t CameraId);
    bool (*stopCapture)(int32_t CameraId);
    //bool (*setCallBack)(int32_t CameraId,Datacallback cb);
    bool (*SnapShort)(int32_t CameraId,Datacallback cb,int32_t frameNum);
    bool (*SetCameraParameters)(int32_t CameraId,camera_parameters_type type,int32_t value,Exposurecallback cb);
    bool (*SetCameraFocusDistance)(int32_t CameraId,float value);
	void (*WaitSCM1SCM2FristFrame)(void);
	void (*WaitCCMFristFrame)(void);
}CameraHandle;

bool CreateCameraHandle(CameraHandle **pHande);
bool DestoryCameraHandle(CameraHandle *pHande);