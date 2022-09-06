


#include <sys/resource.h>
#include <utils/Log.h>
#include <utils/threads.h>
#include <cutils/properties.h>
#include <utils/Log.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <pthread.h>
#include <string.h>
#include <errno.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <poll.h>
#include <dlfcn.h>
#include <math.h>
#include <sys/types.h> 
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/time.h>
#include <time.h>
#include <errno.h>
#include <utils/Timers.h>
#include <condition_variable>
#include <map>
#include <mutex>

#include "../Camera.h"
#include "../CameraLedControl.h"
#include "../CameraSofQueue.h"

static int32_t testCount = 100;

static int32_t frameNumCamera0 = 0;
static int32_t frameNumCamera1 = 0;
static int32_t frameNumCamera2 = 0;

static pthread_mutex_t m_camera_0_mutex;
static pthread_cond_t  m_camera_0_cond;

static pthread_mutex_t m_camera_1_mutex;
static pthread_cond_t  m_camera_1_cond;

static pthread_mutex_t m_camera_2_mutex;
static pthread_cond_t  m_camera_2_cond;

static CameraContrlNode *pNode = NULL;

static pthread_t thread_id;
static pthread_t thread2_id;

static bool startCapture = false;

static bool TestDualEnable = false;
static bool isCanCapture = false;
static bool IsCaptureAllCameraOnce = false;

#define IMAGE_FORMAT_JPEG           0x100
#define IMAGE_FORMAT_YUV_420_888    0x23
#define IMAGE_FORMAT_RAW10          0x25


static std::condition_variable mWaitAllCameraCallBackCondition;
static std::mutex mWaiAllCameraCallBackMutex;
//static std::queue<int> datat_queue;
static int callbackcount = 0;

static int WaitAllCameraAllDataCallback() {
	std::unique_lock<std::mutex> lk(mWaiAllCameraCallBackMutex);
	mWaitAllCameraCallBackCondition.wait(lk,[]{return callbackcount == 3;});
	return 0;
}

static void SigngalAllCameraDatatCallbackCount(){
	std::lock_guard<std::mutex> lk(mWaiAllCameraCallBackMutex);
	callbackcount++;
	mWaitAllCameraCallBackCondition.notify_all();
}

static void OnDataCallBackTestCamera0(AImageReader *reader) {
	 ALOGI("CCM camera data callback frameNum=%d.....",frameNumCamera0++);
	 SigngalAllCameraDatatCallbackCount();
	 //pthread_mutex_lock(&m_camera_0_mutex);
	 //pthread_cond_signal(&m_camera_0_cond);
	 //pthread_mutex_unlock(&m_camera_0_mutex);
}

static void OnDataCallBackTestCamera1(AImageReader *reader) {
	 ALOGI("SCM1 camera 1 data callback frameNum=%d.....",frameNumCamera1++);
	 SigngalAllCameraDatatCallbackCount();
	// pthread_mutex_lock(&m_camera_1_mutex);
	// pthread_cond_signal(&m_camera_1_cond);
	// pthread_mutex_unlock(&m_camera_1_mutex);
}

static void OnDataCallBackTestCamera2(AImageReader *reader) {
	 ALOGI("SCM2 camera 2 data callback frameNum=%d.....",frameNumCamera2++);
	 SigngalAllCameraDatatCallbackCount();
	//pthread_mutex_lock(&m_camera_2_mutex);
	 //pthread_cond_signal(&m_camera_2_cond);
	 //pthread_mutex_unlock(&m_camera_2_mutex);
}

static void OnExposureCallback(int64_t exposuretime,int32_t iso) {
	ALOGI("CCM camera exprosure callback exposuretime=%ld iso=%d .....",exposuretime,iso);
}

static void* camera_2_capture_thread(void* pContext) {
	int i = 0;
    if (!TestDualEnable || IsCaptureAllCameraOnce) {
    	return NULL;
    }
	while (!startCapture) {
		usleep(20*1000);
	}
	if (!isCanCapture) {
		return NULL;
	}

    if (!TestDualEnable) {
    	return NULL;
    }

	for (i = 0; i < testCount; i++) {
		nsecs_t starttime = systemTime();
		ALOGI("[CameraPerformance] CameraTest camera 2 start capture count:%d",i);
		pNode->Capture(2,OnDataCallBackTestCamera2);
		pthread_mutex_lock(&m_camera_2_mutex);
		pthread_cond_wait(&m_camera_2_cond,&m_camera_2_mutex);
		pthread_mutex_unlock(&m_camera_2_mutex);
		ALOGI("[CameraPerformance] CameraTest camera 2 get capture count:%d frame,timediff=%llums",i,(systemTime() - starttime)/1000000);
	}

	return NULL;
}


static void* camera_1_capture_thread(void* pContext) {
	int i = 0;
    if (!TestDualEnable || IsCaptureAllCameraOnce) {
    	return NULL;
    }

	while (!startCapture) {
		usleep(20*1000);
	}
	if (!isCanCapture) {
		return NULL;
	}

    if (!TestDualEnable) {
    	return NULL;
    }

	for (i = 0; i < testCount; i++) {
		nsecs_t starttime = systemTime();
		ALOGI("[CameraPerformance] CameraTest camera 1 start capture count:%d",i);
		pNode->Capture(1,OnDataCallBackTestCamera1);
		pthread_mutex_lock(&m_camera_1_mutex);
		pthread_cond_wait(&m_camera_1_cond,&m_camera_1_mutex);
		pthread_mutex_unlock(&m_camera_1_mutex);
		ALOGI("[CameraPerformance] CameraTest camera 1 get capture count:%d frame,timediff=%llums",i,(systemTime() - starttime)/1000000);
	}

	return NULL;
}
/*
Test one camera: MeigCameraInterFaceTest cameraid [0,1] witdth height foramt
Camera 0:
MeigCameraInterFaceTest 0 1280 800 YUV
MeigCameraInterFaceTest 0 4160 3120 YUV

camera 1:
MeigCameraInterFaceTest 1 1280 800 YUV


Test two cameras:
MeigCameraInterFaceTest 4 1280 800 YUV
MeigCameraInterFaceTest 4 4160 3120 YUV
*/
/*[4208 3120 2104 1560 ]*/

int main(int argc, char* argv[]) {
	int i = 0;
	startCapture = false;
    int32_t CameraId = 0;
    int32_t witdth = 1280;
    int32_t height = 800;
    int32_t witdth0 = 4160;
    int32_t height0 = 3120;
    int32_t format = IMAGE_FORMAT_YUV_420_888;	//CameraContrlNode *pNode = NULL;
	int32_t cam1_format = IMAGE_FORMAT_RAW10;
	int32_t cam2_format = IMAGE_FORMAT_RAW10;
    TestDualEnable = false;
    isCanCapture = false;
    IsCaptureAllCameraOnce = false;
	int count = 0;

    if (argv[1]) {
        if(!strcmp(argv[1],"0")) {
          CameraId = 0;
        } else if(!strcmp(argv[1],"1")) {
          CameraId = 1;
        } else if(!strcmp(argv[1],"2")) {
          CameraId = 2;
        } else if (!strcmp(argv[1],"3")) {
        	CameraId = 3;
        } else if (!strcmp(argv[1],"4")) {
        	CameraId = 4;
        } else if (!strcmp(argv[1],"5")){
			CameraId = 5;
		} else if (!strcmp(argv[1],"6")){
			CameraId = 6;
		}
		else {
        	printf("invalid cameraid\n");
        	return 0;
        }
    }

	if (CameraId == 5) {
	    IsCaptureAllCameraOnce = true;
	}

    if (CameraId > 2) {
		TestDualEnable = true;
    	if (argv[2]) {
    		witdth0 = atoi(argv[2]);
    	}
    	if (argv[3]) {
    		height0 = atoi(argv[3]);
    	}
    } else {
    	if (argv[2]) {
    		witdth0 = atoi(argv[2]);
    	}
    	if (argv[3]) {
    		height0 = atoi(argv[3]);
    	}
    }

    if (argv[4]) {
    	if(!strcmp(argv[4],"YUV")) {
    		format = IMAGE_FORMAT_YUV_420_888;
    	} else if (!strcmp(argv[4],"JPEG")){
    		format = IMAGE_FORMAT_JPEG;
    	} else if (!strcmp(argv[4],"RAW10")){
    		format = IMAGE_FORMAT_RAW10;
    	}
    	else {
    		printf("invalid foramt\n");
    		//return  0;
    	}
    }

    if (argv[5]) {
    	if(!strcmp(argv[5],"YUV")) {
    		cam1_format = IMAGE_FORMAT_YUV_420_888;
    	} else if (!strcmp(argv[5],"JPEG")){
    		cam1_format = IMAGE_FORMAT_JPEG;
    	} else if (!strcmp(argv[5],"RAW10")){
    		cam1_format = IMAGE_FORMAT_RAW10;
    	}
    	else {
    		printf("invalid foramt\n");
    		//return  0;
    	}
    }

    if (argv[6]) {
    	if(!strcmp(argv[6],"YUV")) {
    		cam2_format = IMAGE_FORMAT_YUV_420_888;
    	} else if (!strcmp(argv[6],"JPEG")){
    		cam2_format = IMAGE_FORMAT_JPEG;
    	} else if (!strcmp(argv[6],"RAW10")){
    		cam2_format = IMAGE_FORMAT_RAW10;
    	}
    	else {
    		printf("invalid foramt\n");
    		//return  0;
    	}
    }
/*
    if (format == IMAGE_FORMAT_RAW10) {
    	if (witdth0 > 2104 || height0 > 1560) {
    		witdth0 = 4208;
    		height0 = 3120;
    	} else {
    		witdth0 = 2104;
    		height0 = 1560;
    	}
    }
*/
    printf("test: camera 0:[%dx%d %s]\n",witdth0,height0,(format == IMAGE_FORMAT_RAW10)?"RAW10":"YUV");
    printf("test: camera 1:[%dx%d %s]\n",witdth,height,(cam1_format == IMAGE_FORMAT_RAW10)?"RAW10":"YUV");
    printf("test: camera 2:[%dx%d %s]\n",witdth,height,(cam2_format == IMAGE_FORMAT_RAW10)?"RAW10":"YUV");

	pthread_mutex_init(&m_camera_0_mutex, 0);
    pthread_cond_init(&m_camera_0_cond, NULL);

	pthread_mutex_init(&m_camera_1_mutex, 0);
    pthread_cond_init(&m_camera_1_cond, NULL);

    pthread_mutex_init(&m_camera_2_mutex, 0);
    pthread_cond_init(&m_camera_2_cond, NULL);

    pthread_create(&thread_id, NULL, camera_1_capture_thread, NULL);
    pthread_create(&thread2_id, NULL, camera_2_capture_thread, NULL);

	CreateCameraInterfaceNode(&pNode);
	if (TestDualEnable) {
		pNode->Open(0,witdth0,height0,format);
		usleep(1000*3000);
		pNode->Open(1,witdth,height,cam1_format);
		if (CameraId > 3) {
			pNode->Open(2,witdth,height,cam2_format);
		}
	} else {
		if (CameraId == 0) {
			pNode->Open(CameraId,witdth0,height0,format);
		} else {
			pNode->Open(CameraId,witdth,height,format);
		}
	}
	if (CameraId == 6) {
		usleep(1000*1000);
		OpenCameraSofDevice();
		pNode->Resume(1);
		pNode->Resume(2);
		ALOGI("[CameraPerformance] start time of WaitScm1Scm2FristFrameReady.....");
		pNode->WaitScm1Scm2FristFrameReady();
		ALOGI("[CameraPerformance] end time of WaitScm1Scm2FristFrameReady.....");
		//灭灯
		Set_AIM_SUS(true);
		Set_LaserAiming(false);
		Set_ILM_OUT(false);

		pNode->Resume(0);
		ALOGI("[CameraPerformance] start time of WaitCCMFristFrameReady.....");
		pNode->WaitCCMFristFrameReady();
		ALOGI("[CameraPerformance] end time of WaitCCMFristFrameReady.....");
		//wait ccm frist frame
		WaitCameraSOF();
		frameNumCamera0 = 0;
		frameNumCamera1 = 0;
		frameNumCamera2 = 0;

		startCapture = true;
		//ALOGI("[CameraPerformance] start time of capture API called.....");
		do {
			nsecs_t starttime = systemTime();
			ALOGI("[CameraPerformance] start time of capture API called count=%d.....",count);
			WaitCameraSOF();
			//usleep(1000*5);//假设ccm 曝光结束时间是16ms
			ALOGI("[CameraPerformance] end of time ccm exposure ");
			//亮灯
			ALOGI("[CameraPerformance] set LD and LCM_OUT on");
			Set_AIM_SUS(false);
			Set_LaserAiming(true);
			Set_ILM_OUT(true);
			pNode->CaptureByCustomer(OnDataCallBackTestCamera1,OnDataCallBackTestCamera2,OnExposureCallback,OnDataCallBackTestCamera0);
			//wait ccm call all back
			//pthread_mutex_lock(&m_camera_0_mutex);
			//pthread_cond_wait(&m_camera_0_cond,&m_camera_0_mutex);
			//pthread_mutex_unlock(&m_camera_0_mutex);
			WaitAllCameraAllDataCallback();
			callbackcount = 0;
			//灭灯
			ALOGI("[CameraPerformance] set LD and LCM_OUT off");
			Set_AIM_SUS(true);
			Set_LaserAiming(false);
			Set_ILM_OUT(false);
			ALOGI("[CameraPerformance] end time of capture count:%d,timediff=%llums",count,(systemTime() - starttime)/1000000);
		}while(count++ < 100);

		CloseCameraSofDevice();
		goto exit;
    }
	do {
		char c;
		printf("input s:supend  r:resume   c:capture  a:close  b:open  p:brightness f:flashlight_on q:flashlight_off m:movefocus  n:all_camera_capture_at_same_time   e:exit\n");
		c = getchar();
		if (c == 's') {
			if (TestDualEnable) {
				pNode->Suspend(0);
				pNode->Suspend(1);
				if (CameraId > 3) {
					pNode->Suspend(2);
				}
				isCanCapture = false;
			} else {
				pNode->Suspend(CameraId);
				isCanCapture = false;
			}
		} else if (c == 'r') {
			if (TestDualEnable) {
				pNode->Resume(0);
				pNode->Resume(1);
				if (CameraId > 3) {
					pNode->Resume(2);
				}
				isCanCapture = true;
			} else {
				pNode->Resume(CameraId);
				isCanCapture = true;
			}
		} else if (c == 'c') {
			if (TestDualEnable) {
				pNode->Capture(0,OnDataCallBackTestCamera0);
				pNode->Capture(1,OnDataCallBackTestCamera1);
				if (CameraId > 3) {
					pNode->Capture(2,OnDataCallBackTestCamera2);
				}
			} else {
				pNode->Capture(CameraId,OnDataCallBackTestCamera0);
			}
		} else if (c == 'a') {
			if (TestDualEnable) {
				pNode->Close(0);
				pNode->Close(1);
				if (CameraId > 3) {
					pNode->Close(2);
				}
				isCanCapture = false;
			} else {
				pNode->Close(CameraId);
				isCanCapture = false;
			}
		} else if (c == 'b') {
			if (TestDualEnable) {
				pNode->Open(0,witdth0,height0,format);
				pNode->Open(1,witdth,height,cam1_format);
				if (CameraId > 3) {
					pNode->Open(2,witdth,height,cam2_format);
				}
				isCanCapture = false;
			} else {
				if (CameraId == 0) {
					pNode->Open(CameraId,witdth0,height0,format);
				} else {
					pNode->Open(CameraId,witdth,height,format);
				}
				isCanCapture = false;
			}
		} else if (c == 'p') {
			pNode->SetParameters(CameraId,SET_BRIGHTNESS,BRIGHTNESS_LEVEL_MAX);
		} else if (c == 'f') {
			pNode->SetParameters(CameraId,SET_FALSHLIGHT,FLASHLIGHT_ON);
		} else if (c == 'q') {
			pNode->SetParameters(CameraId,SET_FALSHLIGHT,FLASHLIGHT_OFF);
		} else if (c == 'm') {
			pNode->MoveFocus(CameraId,3.2);
		} else if (c == 'n') {
			pNode->CaptureByCustomer(OnDataCallBackTestCamera1,OnDataCallBackTestCamera2,OnExposureCallback,OnDataCallBackTestCamera0);
		}else if (c == 'e') {
			break;
		}
	}while(1);
    frameNumCamera0 = 0;
    frameNumCamera1 = 0;
    frameNumCamera2 = 0;

    startCapture = true;

    if (!isCanCapture) {
    	goto exit;
    }
	
	if (IsCaptureAllCameraOnce) {
		for (i = 0; i < testCount; i++) {
			nsecs_t starttime = systemTime();
			ALOGI("[CameraPerformance] start time of capture count:%d API called.....",i);
			pNode->CaptureByCustomer(OnDataCallBackTestCamera1,OnDataCallBackTestCamera2,OnExposureCallback,OnDataCallBackTestCamera0);
			pthread_mutex_lock(&m_camera_0_mutex);
    		pthread_cond_wait(&m_camera_0_cond,&m_camera_0_mutex);
    		pthread_mutex_unlock(&m_camera_0_mutex);
			//usleep(1000*2000);
			ALOGI("[CameraPerformance] end time of capture count:%d,timediff=%llums",i,(systemTime() - starttime)/1000000);
		}
		goto exit;
	} else {
		if (TestDualEnable) {
			for (i = 0; i < testCount; i++) {
				nsecs_t starttime = systemTime();
				ALOGI("CameraTest camera 0 start capture count:%d",i);
				pNode->Capture(0,OnDataCallBackTestCamera0);
				pthread_mutex_lock(&m_camera_0_mutex);
				pthread_cond_wait(&m_camera_0_cond,&m_camera_0_mutex);
				pthread_mutex_unlock(&m_camera_0_mutex);
				ALOGI("[CameraPerformance] CameraTest camera 0 get capture count:%d frame,timediff=%llums",i,(systemTime() - starttime)/1000000);
			}
		}else {
			for (i = 0; i < testCount; i++) {
				nsecs_t starttime = systemTime();
				ALOGI("CameraTest camera %d start capture count:%d",CameraId,i);
				pNode->Capture(CameraId,OnDataCallBackTestCamera0);
				pthread_cond_wait(&m_camera_0_cond,&m_camera_0_mutex);
				ALOGI("[CameraPerformance] CameraTest camera %d get capture count:%d frame,timediff=%llums",CameraId,i,(systemTime() - starttime)/1000000);
			}
		}
    }
exit:
	//usleep(1000*1000);
    isCanCapture = false;
	pthread_join(thread_id, NULL);
	pthread_join(thread2_id, NULL);
    pthread_cond_destroy(&m_camera_0_cond);
    pthread_mutex_destroy(&m_camera_0_mutex);
    pthread_cond_destroy(&m_camera_1_cond);
    pthread_mutex_destroy(&m_camera_1_mutex);
    pthread_cond_destroy(&m_camera_2_cond);
    pthread_mutex_destroy(&m_camera_2_mutex);

    if (TestDualEnable) {
    	pNode->Close(0);
    	pNode->Close(1);
    	if (CameraId > 4) {
    		pNode->Close(2);
    	}
    } else {
    	pNode->Close(CameraId);
    }

	DestoryCameraInterfaceNode(pNode);
	return 0;
}