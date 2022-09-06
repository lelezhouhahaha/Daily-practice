

#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <sched.h>
#include <sys/resource.h>
#include <utils/Thread.h>
#include <sys/time.h>
#include <time.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <signal.h>
#include <fcntl.h>
#include <sched.h>
#include <dlfcn.h>
#include <cutils/properties.h>
#include <algorithm>
#include <chrono>
#include <inttypes.h>
#include <functional>
#include <android/log.h>
#include <utils/Log.h>

#include "CameraI2cControl.h"

#define CAMERA_HARDWARE_DEVICE_NAME "/dev/camera_i2c_control"

#define SENSOR_I2C_TYPE_BYTE   1
#define SENSOR_I2C_TYPE_SHORT  2

#define SENSOR_STREAM_ON_STATUS  0xaa
#define SENSOR_STREAM_OFF_STATUS 0xbb

typedef struct {
	uint16_t RegAddr;
	uint16_t RegData;
	uint8_t  RegType;
	uint8_t  DataType;
	uint16_t Delay;
}I2CRegInfo;

typedef struct {
	int32_t CameraId;
	uint16_t sensor_id;
	uint8_t index;
	I2CRegInfo SteamOnReg;
	I2CRegInfo SteamOffReg;
}CameraSensorI2cInfo;

//static SensorExposureTimeInfo_t SensorExposureTimeInfo[3];

static CameraSensorI2cInfo CameraSensorI2CRegInfo[] = {
	{
	    .CameraId = 0,
	    .sensor_id = 0x30c6,
	    .index = 0,
	    .SteamOnReg = {
	    	.RegAddr = 0x0100,
	    	.RegData = 0x01,
	    	.RegType = SENSOR_I2C_TYPE_SHORT,
	    	.DataType = SENSOR_I2C_TYPE_BYTE,
	    	.Delay = 0,
	    },
	    .SteamOffReg = {
	    	.RegAddr = 0x0100,
	    	.RegData = 0x0,
	    	.RegType = SENSOR_I2C_TYPE_SHORT,
	    	.DataType = SENSOR_I2C_TYPE_BYTE,
	    	.Delay = 0,
	    },
    },
	{
	    .CameraId = 1,
	    .index = 2,
	    .sensor_id = 0x9281,
	    .SteamOnReg = {
	    	.RegAddr = 0x0100,
	    	.RegData = 0x1,
	    	.RegType = SENSOR_I2C_TYPE_SHORT,
	    	.DataType = SENSOR_I2C_TYPE_BYTE,
	    	.Delay = 0,
	    },
	    .SteamOffReg = {
	    	.RegAddr = 0x0100,
	    	.RegData = 0x0,
	    	.RegType = SENSOR_I2C_TYPE_SHORT,
	    	.DataType = SENSOR_I2C_TYPE_BYTE,
	    	.Delay = 0,
	    },
    },
	{
		.CameraId = 2,
		.index = 1,
		.sensor_id = 0x9281,
		.SteamOnReg = {
			.RegAddr = 0x0100,
			.RegData = 0x1,
			.RegType = SENSOR_I2C_TYPE_SHORT,
			.DataType = SENSOR_I2C_TYPE_BYTE,
			.Delay = 0,
		},
		.SteamOffReg = {
			.RegAddr = 0x0100,
			.RegData = 0x0,
			.RegType = SENSOR_I2C_TYPE_SHORT,
			.DataType = SENSOR_I2C_TYPE_BYTE,
			.Delay = 0,
		},
    },
};

static int mSensorI2CFd = -1;

static CameraSensorI2cInfo *GetCameraSensorI2CInfo(int32_t CameraId) {
	int i = 0;
	int size = sizeof(CameraSensorI2CRegInfo)/sizeof(CameraSensorI2CRegInfo[0]);

	for (i = 0; i < size; i++) {
		if (CameraSensorI2CRegInfo[i].CameraId == CameraId) {
			return &CameraSensorI2CRegInfo[i];
		}
	}
	return NULL;
}

bool OpenSensorI2cDevice(void){
	int fd = -1;
	//int i = 0;
	char value[PROPERTY_VALUE_MAX] = {0};

	if (mSensorI2CFd > 0) {
		return true;
	}
	//for (i = 0; i < 3; i++) {
	//	memset(&SensorExposureTimeInfo[i],0,sizeof(SensorExposureTimeInfo_t))l;
	//}

    property_get("persist.vendor.camera.stream.onoff.enable", value, "0");
    if (atoi(value) == 0) {
    	mSensorI2CFd = -1;
    	ALOGI("I2C send sendor stream on/off disalble ");
    	return true;
    }

	fd = open(CAMERA_HARDWARE_DEVICE_NAME,O_RDWR | O_NONBLOCK);
	if (fd < 0) {
		ALOGI("open devide %s failed",CAMERA_HARDWARE_DEVICE_NAME);
		return false;
	}
	mSensorI2CFd = fd;
	return true;
}

void CloseSensorI2cDevice(void) {
	if (mSensorI2CFd > 0 ) {
		close(mSensorI2CFd);
		mSensorI2CFd = -1;
	}
}
/*
write buffer:
buf[0]:sensor_id_h
buf[1]:sensor_id_l
buf[2]:reg_addr_h
buf[3]:reg_addr_l
buf[4]:addr_type
buf[5]:reg_data_h
buf[6]:reg_data_l
buf[7]:data_type
buf[8]:stream on/off status
buf[9]:index

read buffer:
buf[0]:sensor_id_h
buf[1]:sensor_id_l
buf[2]:reg_addr_h
buf[3]:reg_addr_l
buf[4]:addr_type
buf[5]:data_type
buf[6]:index
buf[7]:reg_data_h
buf[8]:reg_data_l
*/
int CameraSensorI2CCfg(int32_t CameraId,int cmd) {
	int ret;
	CameraSensorI2cInfo *I2cInfo = NULL;
	char value[PROPERTY_VALUE_MAX] = {0};
	char buffer[20] = {0};

	if (mSensorI2CFd < 0) {
		return -1;
	}
	I2cInfo = GetCameraSensorI2CInfo(CameraId);

    if (!I2cInfo) {
    	ALOGE("not foud cameraid %d i2c info",CameraId);
    	return -1;
    }

    property_get("persist.vendor.camera.stream.onoff.enable", value, "0");
    if (atoi(value) == 0) {
    	ALOGI("cameraid %d I2C send sendor stream on/off disalble ",CameraId);
    	return 0;
    }

    //if (CameraId == 1) {
    //	return -1;
   // }

	switch (cmd) {
		case CAMERA_STREAMON_CMD:{
			ALOGI("cameraid %d I2C send sendor stream on ",CameraId);
		    /*sensor id*/
			buffer[0] = (I2cInfo->sensor_id >> 8) & 0xFF;
			buffer[1] = I2cInfo->sensor_id & 0xFF;

			/*reg addr*/
			buffer[2] = (I2cInfo->SteamOnReg.RegAddr >> 8) & 0xFF;
			buffer[3] = (I2cInfo->SteamOnReg.RegAddr) & 0xFF;
			buffer[4] = (I2cInfo->SteamOnReg.RegType) & 0xFF;

			/*data*/
			buffer[5] = (I2cInfo->SteamOnReg.RegData >> 8) & 0xFF;
			buffer[6] = (I2cInfo->SteamOnReg.RegData) & 0xFF;
			buffer[7] = (I2cInfo->SteamOnReg.DataType) & 0xFF;
			buffer[8] = SENSOR_STREAM_ON_STATUS;
			buffer[9] = I2cInfo->index;
			ret = write(mSensorI2CFd,buffer,10);
			break;
		}
		case CAMERA_STREAMOFF_CMD:{
			ALOGI("cameraid %d I2C send sendor stream off ",CameraId);
		    /*sensor id*/
			buffer[0] = (I2cInfo->sensor_id >> 8) & 0xFF;
			buffer[1] = I2cInfo->sensor_id & 0xFF;

			/*reg addr*/
			buffer[2] = (I2cInfo->SteamOffReg.RegAddr >> 8) & 0xFF;
			buffer[3] = (I2cInfo->SteamOffReg.RegAddr) & 0xFF;
			buffer[4] = (I2cInfo->SteamOffReg.RegType) & 0xFF;

			/*data*/
			buffer[5] = (I2cInfo->SteamOffReg.RegData >> 8) & 0xFF;
			buffer[6] = (I2cInfo->SteamOffReg.RegData) & 0xFF;
			buffer[7] = (I2cInfo->SteamOffReg.DataType) & 0xFF;
			buffer[8] = SENSOR_STREAM_OFF_STATUS;
			buffer[9] = I2cInfo->index;
			ret = write(mSensorI2CFd,buffer,10);
			break;
		}
	}
	if (ret < 0) {
		ALOGE("cameraid %d I2C send sendor cmd:%d failed",CameraId,cmd);
	}
	return ret;
}
/*
1280x800 120fps
SYS_CLK 80MHZ
HTS = 0x02d0
Tline = HTS/SYS_CLK = 0x02d0/80 = 9us
LineCout = ExposureTime/Tline
*/
#define OV9281_HTS    0x02d0
#define OV9281_SCLK   80
#define OV9281_EXPOSURE_REG_BASE  0x3502
#define OV9281_EXPOSURE_REG_CNT 2
#define OV9281_GAIN_REG_BASE 0x3509
#define OV9281_GAIN_REG_CNT   1

/*
1200 x 800 120fps
Pclk = 480Mhz
linelength  = 4896
Tline = 4896/480 us = 10.2us
LineCout = ExposureTime / Tline
*/
#define S5K3L6_PCLK 480
#define S5K3L6_LINE_LENGTH 4896
#define S5K3L6_EXPOSURE_REG_BASE 0x0203
#define S5K3L6_EXPOSURE_REG_CNT 2
#define S5K3L6_GAIN_REG_BASE  0x0205
#define S5K3L6_GAIN_REG_CNT   2

#define OV9281_CHIPID_REG 0x300a
#define S5K3L6_CHIPID_REG 0x0000
#define OV9281_FRAME_CNT_REG 0x4244 //0x4244 0x303F
#define S5K3L6_FRAME_CNT_REG 0x0005

int SetSensorExposureTime(int cameraId,int32_t time) {
	int ret;
	CameraSensorI2cInfo *I2cInfo = NULL;
	char buffer[20] = {0};
	int32_t exposurelinecout = 0;
	uint8_t value = 0;
	uint16_t reg = 0;
    int32_t count = 0;	
	int32_t i = 0;

	I2cInfo = GetCameraSensorI2CInfo(cameraId);

    if (!I2cInfo) {
    	ALOGE("not foud cameraid %d i2c info",cameraId);
    	return -1;
    }

	if (mSensorI2CFd < 0) {
		return -1;
	}
	//10896
	if (cameraId > 0) {
		int Tline = OV9281_HTS/OV9281_SCLK;
		reg = OV9281_EXPOSURE_REG_BASE;
		count = OV9281_EXPOSURE_REG_CNT;
		exposurelinecout = (time/Tline) * 1000;
		ALOGI("Set camera %d exposuretime=%dms, linecout=0x%x, Tline=%dus",cameraId,time,exposurelinecout,Tline);
	} else {
		int Tline = S5K3L6_LINE_LENGTH/S5K3L6_PCLK;
		reg = S5K3L6_EXPOSURE_REG_BASE;
		count = S5K3L6_EXPOSURE_REG_CNT;
		exposurelinecout = (time/Tline) * 1000;
		ALOGI("Set camera %d exposuretime=%dms, linecout=0x%x, Tline=%dus",cameraId,time,exposurelinecout,Tline);
	}

	for (i = 0; i < count; i++) {
		value = (exposurelinecout >> (8*i)) & 0xFF;
	
		buffer[0] = (I2cInfo->sensor_id >> 8) & 0xFF;
		buffer[1] = I2cInfo->sensor_id & 0xFF;
		buffer[2] = (reg >> 8) & 0xFF;
		buffer[3] = (reg) & 0xFF;
		buffer[4] = (SENSOR_I2C_TYPE_SHORT) & 0xFF;

		buffer[5] = 0x00;
		buffer[6] = (value) & 0xFF;
		buffer[7] = (SENSOR_I2C_TYPE_BYTE) & 0xFF;
		buffer[8] = 0;
		buffer[9] = I2cInfo->index;
		ret = write(mSensorI2CFd,buffer,10);
		reg--;
	}
	
	return ret;
}

int SetSensorGain(int cameraId,int32_t gain) {
	int ret;
	CameraSensorI2cInfo *I2cInfo = NULL;
	char buffer[20] = {0};
	int32_t value = 0;
	uint16_t reg = 0;
    int32_t count = 0;
	int32_t i = 0;
	I2cInfo = GetCameraSensorI2CInfo(cameraId);

    if (!I2cInfo) {
    	ALOGE("not foud cameraid %d i2c info",cameraId);
    	return -1;
    }

	if (mSensorI2CFd < 0) {
		return -1;
	}

	if (cameraId > 0) {
		reg = OV9281_GAIN_REG_BASE;
		count = OV9281_GAIN_REG_CNT;
		if (gain < 1) {
			gain = 1;
		}
		if (gain > 16) {
			gain = 16;
		}
		value = gain * 16;
		if (value > 0xff) {
			value = 0xff;
		}
	} else {
		reg = S5K3L6_GAIN_REG_BASE;
		count = S5K3L6_GAIN_REG_CNT;
		if (gain < 1) {
			gain = 1;
		}
		if (gain > 16) {
			gain = 16;
		}
		value = gain*32;
	}

	ALOGI("Set camera %d realgain=%dX, regGain=0x%x",cameraId,gain,value);

	for (i = 0; i < count; i++) {
		buffer[0] = (I2cInfo->sensor_id >> 8) & 0xFF;
		buffer[1] = I2cInfo->sensor_id & 0xFF;
		buffer[2] = (reg >> 8) & 0xFF;
		buffer[3] = (reg) & 0xFF;
		buffer[4] = (SENSOR_I2C_TYPE_SHORT) & 0xFF;

		buffer[5] = 0x00;
		buffer[6] = (value >> (8*i)) & 0xFF;
		buffer[7] = (SENSOR_I2C_TYPE_BYTE) & 0xFF;
		buffer[8] = 0;
		buffer[9] = I2cInfo->index;
		ret = write(mSensorI2CFd,buffer,10);
		reg--;
	}
	return ret;
}

int GetSensorFrameNum(int cameraId) {
	int ret;
	CameraSensorI2cInfo *I2cInfo = NULL;
	uint16_t reg = 0;
	int32_t framenumer = 0;
	char buffer[20] = {0};
	I2cInfo = GetCameraSensorI2CInfo(cameraId);

    if (!I2cInfo) {
    	ALOGE("not foud cameraid %d i2c info",cameraId);
    	return -1;
    }

	if (mSensorI2CFd < 0) {
		return -1;
	}
	if (cameraId == 0) {
		reg = S5K3L6_FRAME_CNT_REG;
	} else {
		reg = OV9281_FRAME_CNT_REG;
	}
	buffer[0] = (I2cInfo->sensor_id >> 8) & 0xFF;
	buffer[1] = I2cInfo->sensor_id & 0xFF;
	buffer[2] = (reg >> 8) & 0xFF;
	buffer[3] = (reg) & 0xFF;
	buffer[4] = (SENSOR_I2C_TYPE_SHORT) & 0xFF;
	buffer[5] = (SENSOR_I2C_TYPE_BYTE) & 0xFF;
	buffer[6] = I2cInfo->index;
	ret = read(mSensorI2CFd,buffer,9);
	framenumer = ((buffer[7] << 8 ) & 0xff ) | buffer[8];
	//ALOGI("camera %d Read reg=0x%x, data[0]=0x%x data[1]=0x%x framenumer=%d",cameraId,reg,buffer[7],buffer[8],framenumer);
	return framenumer;
}

int SensorI2cCheckTest(int cameraId) {
	int ret;
	CameraSensorI2cInfo *I2cInfo = NULL;
	uint16_t reg = 0;
	char buffer[20] = {0};
	int32_t chipid = 0;
	I2cInfo = GetCameraSensorI2CInfo(cameraId);

    if (!I2cInfo) {
    	ALOGE("not foud cameraid %d i2c info",cameraId);
    	return -1;
    }

	if (mSensorI2CFd < 0) {
		return -1;
	}
	if (cameraId == 0) {
		reg = S5K3L6_CHIPID_REG;
	} else {
		reg = OV9281_CHIPID_REG;
	}
	buffer[0] = (I2cInfo->sensor_id >> 8) & 0xFF;
	buffer[1] = I2cInfo->sensor_id & 0xFF;
	buffer[2] = (reg >> 8) & 0xFF;
	buffer[3] = (reg) & 0xFF;
	buffer[4] = (SENSOR_I2C_TYPE_SHORT) & 0xFF;
	buffer[5] = (SENSOR_I2C_TYPE_SHORT) & 0xFF;
	buffer[6] = I2cInfo->index;
	ret = read(mSensorI2CFd,buffer,9);
	chipid = ((buffer[7] << 8 ) & 0xff ) | buffer[8];
	ALOGI("camera %d Read reg=0x%x, data[0]=0x%x data[1]=0x%x chipid=0x%x",cameraId,reg,buffer[7],buffer[8],chipid);
	return ret;
}