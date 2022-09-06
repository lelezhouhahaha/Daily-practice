#include "CameraNativeManager.h"
#include "linkedQueue.h"
#include "CameraQueue.h"
#include "CameraI2cControl.h"

typedef enum {
    OPEN_CAMERA = 100,
    CLOSE_CAMERA,
    CAPTURE_CAMERA,
    RESUME_CAMERA,
    SUSPEND_CAMERA,
    SET_PARAMETERS,
    MOVEFOCUS_CAMERA,
    EXIT_CAMERA,
}camera_MSG_type;

typedef struct {
    nsecs_t OpenStartTime;
    nsecs_t OpenEndTime;
    nsecs_t CLoseStartTime;
    nsecs_t CLoseEndTime;
    nsecs_t CaptureStartTime;
    nsecs_t CaptureEndTime;
    nsecs_t SuspendStartTime;
    nsecs_t SuspendEndTime;
    nsecs_t ResumeStartTime;
    nsecs_t ResumeEndTime;
}CameraTimeInfo;

typedef struct {
    int32_t width;
    int32_t height;
    int32_t format;
}StreamInfo;

typedef struct {
    camera_parameters_type type;
    int32_t value;
}ParametersInfo;

typedef struct {
    float value;
}MoveFocusInfo;

typedef struct {
    camera_MSG_type msgtype;
    StreamInfo data;
    ParametersInfo parameters;
    MoveFocusInfo  FocusInfo;
}CameraMsgInfo;

typedef struct {
    pthread_t thread_id;
    int thread_exit;
    pthread_mutex_t m_mutex;
    pthread_cond_t  m_cond;
    pthread_cond_t  m_close_cond;
    cam_queue_t cmd_queue;
    int32_t CameraId;
    camera_state_t state;
    CameraTimeInfo m_timeinfo;
    Datacallback cb;
	Exposurecallback exposurecb;
	pthread_mutex_t m_send_msg_mutex;
}CameraThreadCtrl;

typedef struct {
	pthread_t thread_id;
	pthread_mutex_t m_mutex;
	pthread_cond_t  m_cond;
	bool IsCreate;
	bool thread_exit;
}CameraCaptureThreadCtrl;

static CameraHandle *pCameraHande = NULL;
static CameraThreadCtrl g_CameraThreadHande[3];
static CameraCaptureThreadCtrl g_CameraCaptureThreadHande;

void DestoryCameraCaptureThread();
void CreateCameraCaptureThread();

static void SendCameraMsg(int32_t CameraId,CameraMsgInfo *msg,Datacallback cb) {
    CameraThreadCtrl *hande = NULL;
    if (CameraId == 0) {
        hande = &g_CameraThreadHande[0];
    } else if (CameraId == 1) {
        hande = &g_CameraThreadHande[1];
    } else {
        hande = &g_CameraThreadHande[2];
    }

    if (!hande) {
        return ;
    }

    switch (msg->msgtype) {
        case OPEN_CAMERA: {
            hande->m_timeinfo.OpenStartTime = systemTime();
            break;
        }
        case CLOSE_CAMERA: {
            hande->m_timeinfo.CLoseStartTime = systemTime();
            break;
        }
        case RESUME_CAMERA: {
            hande->m_timeinfo.ResumeStartTime = systemTime();
            break;
        }
        case SUSPEND_CAMERA: {
            hande->m_timeinfo.SuspendStartTime = systemTime();
            break;
        }
        case CAPTURE_CAMERA: {
            hande->m_timeinfo.CaptureStartTime = systemTime();
            hande->cb = cb;
            break;
        }
        case SET_PARAMETERS:
        case MOVEFOCUS_CAMERA:
        case EXIT_CAMERA: {
            break;
        }
    }
    //pthread_mutex_lock(&hande->m_send_msg_mutex);
    ALOGI("CameraId %d SendCameraMsg msg type:%d",CameraId,msg->msgtype);
    cam_queue_enq(&hande->cmd_queue, msg);
    ALOGI("CameraId %d SendCameraMsg msg done",CameraId);
   // pthread_mutex_unlock(&hande->m_send_msg_mutex);

    pthread_mutex_lock(&hande->m_mutex);
    pthread_cond_signal(&hande->m_cond);
    pthread_mutex_unlock(&hande->m_mutex);

}

static void* camera_listen_handle(void* pContext) {
    CameraThreadCtrl *hande = (CameraThreadCtrl *)pContext;
    while (1) {
        CameraMsgInfo *msg = NULL;
        bool ret;
        if (hande->thread_exit) {
            return NULL;
        }
		//pthread_mutex_lock(&hande->m_send_msg_mutex);

        pthread_mutex_lock(&hande->m_mutex);
        ALOGI("Camera %d wait msg",hande->CameraId);
        pthread_cond_wait(&hande->m_cond,&hande->m_mutex);
        pthread_mutex_unlock(&hande->m_mutex);

        if (hande->thread_exit) {
            ALOGI("Camera %d camera_listen_handle exit ",hande->CameraId);
            return NULL;
        }
        ALOGI("Camera %d deQueue msg",hande->CameraId);
        msg = (CameraMsgInfo*)cam_queue_deq(&hande->cmd_queue);
        if (!msg) {
            continue;
        }

        ALOGI("Camera %d get deQueue msg type:%d",hande->CameraId,msg->msgtype);

        switch (msg->msgtype) {
            case OPEN_CAMERA: {
                int32_t width = msg->data.width;
                int32_t height = msg->data.height;
                int32_t format = msg->data.format;
                ret = pCameraHande->Open(hande->CameraId,width,height,format);
                if (!ret) {
                    free(msg);
                    msg = (CameraMsgInfo*)cam_queue_deq(&hande->cmd_queue);
                    return NULL;
                }
                CameraSensorI2CCfg(hande->CameraId,CAMERA_STREAMOFF_CMD);
                hande->m_timeinfo.OpenEndTime = systemTime();
                ALOGI("[CameraPerformance] Camera %d Open Take time = %llums",hande->CameraId,(hande->m_timeinfo.OpenEndTime - hande->m_timeinfo.OpenStartTime) / 1000000);
                hande->state = CAMERA_LOW_POWER;
                break;
            }
            case CLOSE_CAMERA: {
                if (hande->state > CAMERA_POWER_OFF) {
                    //CameraSensorI2CCfg(hande->CameraId,CAMERA_STREAMON_CMD);
                    ret = pCameraHande->Close(hande->CameraId);
                    hande->state = CAMERA_POWER_OFF;
                    hande->m_timeinfo.CLoseEndTime = systemTime();
                    ALOGI("[CameraPerformance] Camera %d Close Take time = %llums",hande->CameraId,(hande->m_timeinfo.CLoseEndTime - hande->m_timeinfo.CLoseStartTime) / 1000000);
                    pthread_mutex_lock(&hande->m_mutex);
                    pthread_cond_signal(&hande->m_close_cond);
                    pthread_mutex_unlock(&hande->m_mutex);
                }
                break;
            }
            case CAPTURE_CAMERA: {
                if (hande->state == CAMERA_READY) {
                    if (pCameraHande && hande->cb) {
                        hande->m_timeinfo.CaptureEndTime = systemTime();
                        pCameraHande->SnapShort(hande->CameraId,hande->cb,0);
                        ALOGI("[CameraPerformance] Camera %d Capture Take time = %llums",hande->CameraId,(hande->m_timeinfo.CaptureEndTime - hande->m_timeinfo.CaptureStartTime) / 1000000);
                    }
                }
                break;
            }
            case RESUME_CAMERA: {
                if (hande->state == CAMERA_LOW_POWER) {
                    CameraSensorI2CCfg(hande->CameraId,CAMERA_STREAMON_CMD);
                    ret = pCameraHande->Capture(hande->CameraId);
                    hande->m_timeinfo.ResumeEndTime = systemTime();
                    ALOGI("[CameraPerformance] Camera %d Resume Take time = %llums",hande->CameraId,(hande->m_timeinfo.ResumeEndTime - hande->m_timeinfo.ResumeStartTime) / 1000000);
                    hande->state = CAMERA_READY;
                }
                break;
            }
            case SUSPEND_CAMERA:{
                if (hande->state == CAMERA_READY) {
                    ret =  pCameraHande->stopCapture(hande->CameraId);
                    CameraSensorI2CCfg(hande->CameraId,CAMERA_STREAMOFF_CMD);
                    hande->m_timeinfo.SuspendEndTime = systemTime();
                    ALOGI("[CameraPerformance] Camera %d Suspend Take time = %llums",hande->CameraId,(hande->m_timeinfo.SuspendEndTime - hande->m_timeinfo.SuspendStartTime) / 1000000);
                    hande->state = CAMERA_LOW_POWER;
                }
                break;
            }
            case MOVEFOCUS_CAMERA : {
                if (hande->state == CAMERA_READY) {
                    float value = msg->FocusInfo.value;
                    ret =  pCameraHande->SetCameraFocusDistance(hande->CameraId,value);
                }
            	break;
            }
            case SET_PARAMETERS: {
                if (hande->state == CAMERA_READY) {
                    camera_parameters_type type = msg->parameters.type;
                    int32_t value = msg->parameters.value;
                    ret =  pCameraHande->SetCameraParameters(hande->CameraId,type,value,hande->exposurecb);
                }
            	break;
            }
            case EXIT_CAMERA: {
                ALOGI("camera_listen_handle exit Done");
                hande->thread_exit = 1;
                free(msg);
                msg = (CameraMsgInfo*)cam_queue_deq(&hande->cmd_queue);
                return NULL;
            }
        }
        free(msg);
        msg = (CameraMsgInfo*)cam_queue_deq(&hande->cmd_queue);
        ALOGI("Camera %d camera_listen_handle Done",hande->CameraId);
		//pthread_mutex_unlock(&hande->m_send_msg_mutex);
    }
    return NULL;
}

static void CreateCameraThread() {
    int i = 0;
    CameraThreadCtrl *hande = NULL;
    for(i = 0; i < 3; i++) {
        pthread_condattr_t cond_attr;
        hande = &g_CameraThreadHande[i];
        hande->CameraId = i;
        hande->thread_exit = 0;
        hande->state = CAMERA_INVAILABLE;

        pthread_condattr_init(&cond_attr);
        pthread_condattr_setclock(&cond_attr, CLOCK_MONOTONIC);
        pthread_mutex_init(&hande->m_mutex, 0);
		pthread_mutex_init(&hande->m_send_msg_mutex,0);
        pthread_cond_init(&hande->m_cond, &cond_attr);
        pthread_cond_init(&hande->m_close_cond, &cond_attr);
        pthread_condattr_destroy(&cond_attr);

        pthread_create(&hande->thread_id, NULL, camera_listen_handle, hande);
        cam_queue_init(&hande->cmd_queue);
    }
}

static void SendExitCameraThread(int32_t CameraId) {
    CameraMsgInfo *msg = (CameraMsgInfo *)malloc(sizeof(CameraMsgInfo));
    msg->msgtype = EXIT_CAMERA;
    SendCameraMsg(CameraId,msg,NULL);
}

static void DestoryCameraThread () {
     int i = 0;
     CameraThreadCtrl *hande = NULL;

     for(i = 0; i < 3; i++) {
        hande = &g_CameraThreadHande[i];
        if (hande->state >= CAMERA_LOW_POWER) {
            ALOGI("wait camera %d close ",hande->CameraId);
            pthread_mutex_lock(&hande->m_mutex);
            pthread_cond_wait(&hande->m_close_cond,&hande->m_mutex);
            ALOGI("wait camera %d close done",hande->CameraId);
            pthread_mutex_unlock(&hande->m_mutex);
        }
        usleep(1000*100);
        pthread_mutex_lock(&hande->m_mutex);
        hande->thread_exit = 1;
        pthread_cond_signal(&hande->m_cond);
        pthread_mutex_unlock(&hande->m_mutex);

        SendExitCameraThread(hande->CameraId);

        ALOGI("wait camera %d thread exit",hande->CameraId);
        pthread_join(hande->thread_id, NULL);
        hande->thread_exit = 1;
        ALOGI("wait camera %d thread exit done",hande->CameraId);
        pthread_cond_destroy(&hande->m_cond);
        pthread_cond_destroy(&hande->m_close_cond);
        pthread_mutex_destroy(&hande->m_mutex);
		pthread_mutex_destroy(&hande->m_send_msg_mutex);
        hande->state = CAMERA_INVAILABLE;
        ALOGI("destoryQueue camera %d ",hande->CameraId);
        cam_queue_deinit(&hande->cmd_queue);
        ALOGI("destoryQueue camera %d done",hande->CameraId);
        ALOGI("DestoryCameraThread camera %d done",hande->CameraId);
     }
}

static bool Initialize() {
    bool ret = true;

    if (!OpenSensorI2cDevice()) {
        ALOGE("OpenSensorI2cDevice failed");
        //return false;
    }

    CreateCameraThread();
	CreateCameraCaptureThread();

    if (!CreateCameraHandle(&pCameraHande)) {
        DestoryCameraThread();
        return false;
    }
    ret = pCameraHande->RegisterCameraServers();
    if (!ret) {
       goto exit; 
    }

    return ret;

exit:
    DestoryCameraThread();
    DestoryCameraHandle(pCameraHande);
    return false;
}

static void DeInitialize() {
    CloseSensorI2cDevice();
	
	DestoryCameraCaptureThread();
    DestoryCameraThread();

    if (pCameraHande) {
        pCameraHande->DestoryCameraServers();
    }

   DestoryCameraHandle(pCameraHande);
}


/****************************************/
/*Camera API*/
static void Open(int32_t CameraId,int32_t width,int32_t height,int32_t format) {
    CameraMsgInfo *msg = (CameraMsgInfo *)malloc(sizeof(CameraMsgInfo));
    msg->msgtype = OPEN_CAMERA;
    msg->data.width = width;
    msg->data.height = height;
    msg->data.format = format;
    SendCameraMsg(CameraId,msg,NULL);
}

static void Close(int32_t CameraId) {
    CameraMsgInfo *msg = (CameraMsgInfo *)malloc(sizeof(CameraMsgInfo));
    msg->msgtype = CLOSE_CAMERA;
    SendCameraMsg(CameraId,msg,NULL);
}

static void Resume(int32_t CameraId) {
    CameraMsgInfo *msg = (CameraMsgInfo *)malloc(sizeof(CameraMsgInfo));
    msg->msgtype = RESUME_CAMERA;
    SendCameraMsg(CameraId,msg,NULL);
}

static void WaitScm1Scm2FristFrameReady(){
	pCameraHande->WaitSCM1SCM2FristFrame();
}

static void WaitCCMFristFrameReady(){
	pCameraHande->WaitCCMFristFrame();
}

static void Suspend(int32_t CameraId) {
    CameraMsgInfo *msg = (CameraMsgInfo *)malloc(sizeof(CameraMsgInfo));
    msg->msgtype = SUSPEND_CAMERA;
    SendCameraMsg(CameraId,msg,NULL);
}

static void Capture(int32_t CameraId,Datacallback cb) {
    CameraMsgInfo *msg = (CameraMsgInfo *)malloc(sizeof(CameraMsgInfo));
    msg->msgtype = CAPTURE_CAMERA;
    SendCameraMsg(CameraId,msg,cb);
}

static void SetParameters(int32_t CameraId,camera_parameters_type type,int32_t value) {
    CameraMsgInfo *msg = (CameraMsgInfo *)malloc(sizeof(CameraMsgInfo));
    msg->msgtype = SET_PARAMETERS;
    msg->parameters.type = type;
    msg->parameters.value = value;
    SendCameraMsg(CameraId,msg,NULL);
}

static void MoveFocus(int32_t CameraId,float value) {
    CameraMsgInfo *msg = (CameraMsgInfo *)malloc(sizeof(CameraMsgInfo));
    msg->msgtype = MOVEFOCUS_CAMERA;
    msg->FocusInfo.value = value;
    SendCameraMsg(CameraId,msg,NULL);
}

static void* camera_capture_listen_handle(void* pContext) {
	int value = BRIGHTNESS_LEVEL_0;
	int scm1_frameNum = 0;
	int scm2_frameNum = 0;
	int ccm_frameNum = 0;

	do {
		if (g_CameraCaptureThreadHande.thread_exit) {
			break;
		}
		pthread_mutex_lock(&g_CameraCaptureThreadHande.m_mutex);
		ALOGI("camera_capture_listen_handle wait ......");
		pthread_cond_wait(&g_CameraCaptureThreadHande.m_cond,&g_CameraCaptureThreadHande.m_mutex);
		pthread_mutex_unlock(&g_CameraCaptureThreadHande.m_mutex);
		
		if (g_CameraCaptureThreadHande.thread_exit) {
			break;
		}
#if 0
		ccm_frameNum = GetSensorFrameNum(0);
		ALOGI("[CameraPerformance] start camera 0 CCM set brightness frameNum=%d start by i2c.....",ccm_frameNum);
		SetSensorExposureTime(0,100);
		SetSensorGain(0,16);
		//SetParameters(0,SET_BRIGHTNESS,BRIGHTNESS_LEVEL_0);
		//SetSensorExposureTime(0,100);
		if (value > BRIGHTNESS_LEVEL_MAX) {
			value = BRIGHTNESS_LEVEL_0;
		}
		//pCameraHande->SetCameraParameters(0,SET_BRIGHTNESS,value,g_CameraThreadHande[0].exposurecb);
		scm1_frameNum = GetSensorFrameNum(1);
		ALOGI("[CameraPerformance] start camera 1 SCM1 set exposuretime frameNum:%d start by i2c.....",scm1_frameNum);
		SetSensorExposureTime(1,100);
		scm2_frameNum = GetSensorFrameNum(2);
		ALOGI("[CameraPerformance] start camera 2 SCM2 set exposuretime frameNum:%d start by i2c.....",scm2_frameNum);
		SetSensorExposureTime(2,100);
#endif
		ALOGI("[CameraPerformance] start Snapshort SCM1.....");
		pCameraHande->SnapShort(1,g_CameraThreadHande[1].cb,scm1_frameNum);
		ALOGI("[CameraPerformance] start Snapshort SCM2.....");
		pCameraHande->SnapShort(2,g_CameraThreadHande[2].cb,scm2_frameNum);
		//wait scm1 & scm2 all callback
		ALOGI("[CameraPerformance] start Snapshort CCM.....");
		pCameraHande->SnapShort(0,g_CameraThreadHande[0].cb,ccm_frameNum);
		//value++;
	} while(!g_CameraCaptureThreadHande.thread_exit);
	ALOGI("xxxxxxxxxxxxxxxxx camera_capture_listen_handle exit done");
	return NULL;
}

void CreateCameraCaptureThread() {
	if (g_CameraCaptureThreadHande.IsCreate == false) {
		pthread_condattr_t cond_attr;
		pthread_condattr_init(&cond_attr);
		pthread_condattr_setclock(&cond_attr, CLOCK_MONOTONIC);

		g_CameraCaptureThreadHande.thread_exit = false;
		pthread_mutex_init(&g_CameraCaptureThreadHande.m_mutex, 0);
		pthread_cond_init(&g_CameraCaptureThreadHande.m_cond, &cond_attr);
		pthread_condattr_destroy(&cond_attr);
		pthread_create(&g_CameraCaptureThreadHande.thread_id, NULL, camera_capture_listen_handle, &g_CameraCaptureThreadHande);
		g_CameraCaptureThreadHande.IsCreate = true;
	}
}

void DestoryCameraCaptureThread() {
	if (g_CameraCaptureThreadHande.IsCreate == true) {
		g_CameraCaptureThreadHande.thread_exit = true;

		pthread_mutex_lock(&g_CameraCaptureThreadHande.m_mutex);
		pthread_cond_signal(&g_CameraCaptureThreadHande.m_cond);
		pthread_mutex_unlock(&g_CameraCaptureThreadHande.m_mutex);
	
		pthread_join(g_CameraCaptureThreadHande.thread_id, NULL);
        pthread_cond_destroy(&g_CameraCaptureThreadHande.m_cond);
        pthread_mutex_destroy(&g_CameraCaptureThreadHande.m_mutex);
		g_CameraCaptureThreadHande.IsCreate = false;
	}
}

static void CaptureByCustomer(Datacallback scm1_cb,Datacallback scm2_cb,Exposurecallback CCM_ExposureCb,Datacallback ccm_cb) {
	//CreateCameraCaptureThread();
	g_CameraThreadHande[0].cb = ccm_cb;
	g_CameraThreadHande[1].cb = scm1_cb;
	g_CameraThreadHande[2].cb = scm2_cb;
	g_CameraThreadHande[0].exposurecb = CCM_ExposureCb;
	g_CameraThreadHande[1].exposurecb = NULL;
	g_CameraThreadHande[2].exposurecb = NULL;
	pthread_mutex_lock(&g_CameraCaptureThreadHande.m_mutex);
	pthread_cond_signal(&g_CameraCaptureThreadHande.m_cond);
	pthread_mutex_unlock(&g_CameraCaptureThreadHande.m_mutex);
}

bool CreateCameraInterfaceNode(CameraContrlNode **pNode) {
	CameraContrlNode *pTempNode = NULL;
	if (!Initialize()) {
		return false;
	}
	pTempNode = (CameraContrlNode *)malloc(sizeof(CameraContrlNode));
	if (!pTempNode) {
	    ALOGE("CreateCameraInfaceNode failed");
	    DeInitialize();
	    return false;
	}
	pTempNode->Open = Open;
	pTempNode->Close = Close;
	pTempNode->Resume = Resume;
	pTempNode->Suspend = Suspend;
	pTempNode->Capture = Capture;
    pTempNode->SetParameters = SetParameters;
    pTempNode->MoveFocus = MoveFocus;
	pTempNode->CaptureByCustomer = CaptureByCustomer;
	pTempNode->WaitScm1Scm2FristFrameReady = WaitScm1Scm2FristFrameReady;
	pTempNode->WaitCCMFristFrameReady = WaitCCMFristFrameReady;
	*pNode = pTempNode;
	return true;
}

void DestoryCameraInterfaceNode(CameraContrlNode *pNode) {
	DeInitialize();
	if (pNode) {
		free(pNode);
		pNode = NULL;
	}
}