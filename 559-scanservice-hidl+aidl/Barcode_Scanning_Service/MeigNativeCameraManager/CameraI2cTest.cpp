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

#include "CameraI2cControl.h"


int main(int argc, char* argv[]) {
	int exposuretime[3] = {-1};
	int gain[3] = {-1};
	int i = 0;

	OpenSensorI2cDevice();

	if (argv[1]) {
		exposuretime[0] = atoi(argv[1]);
	}
	if (argv[2]) {
		exposuretime[1] = atoi(argv[2]);
	}
	if (argv[3]) {
		exposuretime[2] = atoi(argv[3]);
	}

	if (argv[4]) {
		gain[0] = atoi(argv[4]);
	}
	if (argv[5]) {
		gain[1] = atoi(argv[5]);
	}
	if (argv[6]) {
		gain[2] = atoi(argv[6]);
	}

	for (i = 0;i < 3; i++) {
		printf("Test camera %d exposuretime:%d gain=%d\n",i,exposuretime[i],gain[i]);
	}

	do {
		char c;
		printf("0:Set Camera0 stream on\n");
		printf("1:Set Camera0 stream off\n");
		printf("2:Set Camera0 exposure\n");
		printf("3:Set Camera0 gain\n");
		printf("4:Set Camera1 stream on\n");
		printf("5:Set Camera1 stream off\n");
		printf("6:Set Camera1 exposure\n");
		printf("7:Set Camera1 gain\n");
		printf("8:Set Camera2 stream on\n");
		printf("9:Set Camera2 stream off\n");
		printf("a:Set Camera2 exposure\n");
		printf("b:Set Camera2 gain\n");
		printf("c:Check Camera I2C(Read chipID)\n");
		printf("d:Get camera0 frame number\n");
		printf("f:Get camera1 frame number\n");
		printf("g:Get camera2 frame number\n");
		printf("e:exit\n");
		c = getchar();
		//usleep(1000*1000);
		if (c == '0') {
			CameraSensorI2CCfg(0,CAMERA_STREAMON_CMD);
		} else if (c == '1') {
			CameraSensorI2CCfg(0,CAMERA_STREAMOFF_CMD);
		} else if (c == '2') {
			if (exposuretime[0] != -1) {
				SetSensorExposureTime(0,exposuretime[0]);
			}
		} else if (c == '3') {
			if (gain[0] != -1) {
				SetSensorGain(0,gain[0]);
			}
		} else if (c == '4') {
			CameraSensorI2CCfg(1,CAMERA_STREAMON_CMD);
		} else if (c == '5') {
			CameraSensorI2CCfg(1,CAMERA_STREAMOFF_CMD);
		} else if (c ==  '6') {
			if (exposuretime[1] != -1) {
				SetSensorExposureTime(1,exposuretime[1]);
			}
		} else if (c == '7') {
			if (gain[1] != -1) {
				SetSensorGain(1,gain[1]);
			}
		} else if (c == '8') {
			CameraSensorI2CCfg(2,CAMERA_STREAMON_CMD);
		} else if (c == '9') {
			CameraSensorI2CCfg(2,CAMERA_STREAMOFF_CMD);
		} else if (c == 'a') {
			if (exposuretime[2] != -1) {
				SetSensorExposureTime(2,exposuretime[2]);
			}
		} else if (c == 'b') {
			if (gain[2] != -1) {
				SetSensorGain(2,gain[2]);
			}
		} else if (c == 'c') {
			SensorI2cCheckTest(0);
			SensorI2cCheckTest(1);
			SensorI2cCheckTest(2);
		} else if (c == 'd') {
			GetSensorFrameNum(0);
		} else if (c == 'f') {
			GetSensorFrameNum(1);
		} else if (c == 'g') {
			GetSensorFrameNum(2);
		} else if (c == 'e') {
			break;
		}
	}while(1);
    printf("Exit\n");
	CloseSensorI2cDevice();
	printf("Exit done\n");
	return 0;
}