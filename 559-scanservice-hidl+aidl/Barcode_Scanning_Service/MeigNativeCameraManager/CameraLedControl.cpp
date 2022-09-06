
#include <log/log.h>

#include <chrono>
#include <cinttypes>
#include <condition_variable>
#include <map>
#include <mutex>
#include <string>
#include <vector>
#include <unistd.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <set>
#include <cutils/properties.h>
#include <time.h>
#include <pthread.h>
#include <sys/time.h>
#include <time.h>
#include <errno.h>
#include <utils/Timers.h>
#include <android/log.h>

#include "CameraLedControl.h"

#define LASERAIMING_LED_FILE  "/sys/devices/platform/soc/soc:meig-scanlight/scanlight_fir"
#define AIM_SUS_FILE          "/sys/devices/platform/soc/soc:meig-scanlight/scanlight_scd"
#define LIM_OUT_FILE          "/sys/devices/platform/soc/soc:meig-scanlight/scanlight_trd"

typedef struct {
	int32_t LaserAiming_status;
	int32_t AIM_SUS_status;
	int32_t ILM_OUT_status;
	std::mutex mMutex;
}SanLightCtr_t;

static SanLightCtr_t SanLightCtrlInfo;

void SanLightInit(void){
	SanLightCtrlInfo.LaserAiming_status = LASERAIMING_OFF;
	SanLightCtrlInfo.AIM_SUS_status = AIM_SUS_LOW;
	SanLightCtrlInfo.ILM_OUT_status = LIM_OUT_OFF;
}

void Set_LaserAiming(bool on) {
	int fd = -1;
	std::lock_guard<std::mutex> lock(SanLightCtrlInfo.mMutex);
	ALOGI("Set_LaserAiming %s",(on == true)?"on":"off");
	fd = open(LASERAIMING_LED_FILE,O_RDWR | O_NONBLOCK);
	if (fd < 0) {
		return ;
	}

	if (on) {
		write(fd,"1",1);
		SanLightCtrlInfo.LaserAiming_status = LASERAIMING_0N;
	} else {
		write(fd,"0",1);
		SanLightCtrlInfo.LaserAiming_status = LASERAIMING_OFF;
	}
	close(fd);
	ALOGI("Set_LaserAiming %s done",(on == true)?"on":"off");
}

void Set_AIM_SUS(bool high) {
	int fd = -1;
	std::lock_guard<std::mutex> lock(SanLightCtrlInfo.mMutex);
	ALOGI("Set_AIM_SUS %s",(high == true)?"high":"low");
	fd = open(AIM_SUS_FILE,O_RDWR | O_NONBLOCK);
	if (fd < 0) {
		return ;
	}
	if (high) {
		write(fd,"1",1);
		SanLightCtrlInfo.AIM_SUS_status = AIM_SUS_HIGH;
	} else {
		write(fd,"0",1);
		SanLightCtrlInfo.AIM_SUS_status = AIM_SUS_LOW;
	}
	close(fd);
	ALOGI("Set_AIM_SUS %s done",(high == true)?"high":"low");
}

void Set_ILM_OUT(bool on) {
	int fd = -1;
	std::lock_guard<std::mutex> lock(SanLightCtrlInfo.mMutex);
	ALOGI("Set_ILM_OUT %s",(on == true)?"on":"off");
	fd = open(LIM_OUT_FILE,O_RDWR | O_NONBLOCK);
	if (fd < 0) {
		return ;
	}
	if (on) {
		write(fd,"1",1);
		SanLightCtrlInfo.ILM_OUT_status = LIM_OUT_ON;
	} else {
		write(fd,"0",1);
		SanLightCtrlInfo.ILM_OUT_status = LIM_OUT_OFF;
	}
	close(fd);
	ALOGI("Set_ILM_OUT %s done",(on == true)?"on":"off");
}