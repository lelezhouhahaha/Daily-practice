

#include <stdatomic.h>
#include <pthread.h>
#include "media/NdkImage.h"
#include "media/NdkImageReader.h"

typedef void(*Datacallback)(AImageReader *reader);
typedef void(*Exposurecallback)(int64_t exposuretime,int32_t iso);

typedef enum {
	CAMERA_INVAILABLE = -1,
    CAMERA_POWER_OFF = 0,
    CAMERA_LOW_POWER,
    CAMERA_READY,
}camera_state_t;

typedef enum {
    SET_BRIGHTNESS = 100,
    SET_FALSHLIGHT,
	SET_SCM_EXPOSURETIME,
}camera_parameters_type;

typedef enum {
	FLASHLIGHT_ON,
	FLASHLIGHT_OFF,
}camera_flashlight_level_t;

typedef enum {
	BRIGHTNESS_LEVEL_0,
	BRIGHTNESS_LEVEL_1,
	BRIGHTNESS_LEVEL_2,
	BRIGHTNESS_LEVEL_3,
	BRIGHTNESS_LEVEL_default,
	BRIGHTNESS_LEVEL_5,
	BRIGHTNESS_LEVEL_6,
	BRIGHTNESS_LEVEL_7,
	BRIGHTNESS_LEVEL_MAX,
}camera_brigntness_level_t;

typedef struct {
	void (*Open)(int32_t CameraId,int32_t width,int32_t height,int32_t format);
	void (*Close)(int32_t CameraId);
	void (*Resume)(int32_t CameraId);
	void (*Suspend)(int32_t CameraId);
	void (*Capture)(int32_t CameraId,Datacallback cb);
	void (*SetParameters)(int32_t CameraId,camera_parameters_type type,int32_t value);
	void (*MoveFocus)(int32_t CameraId,float value);
	void (*CaptureByCustomer)(Datacallback scm1_cb,Datacallback scm2_cb,Exposurecallback CCM_ExposureCb,Datacallback ccm_cb);
	void (*WaitScm1Scm2FristFrameReady)();
	void (*WaitCCMFristFrameReady)();
}CameraContrlNode;

bool CreateCameraInterfaceNode(CameraContrlNode **pNode);
void DestoryCameraInterfaceNode(CameraContrlNode *pNode);
