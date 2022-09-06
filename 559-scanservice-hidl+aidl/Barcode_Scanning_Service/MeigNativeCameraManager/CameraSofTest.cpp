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

#include "CameraSofQueue.h"


int main(int argc, char* argv[]) {
	OpenCameraSofDevice();
	getchar();
	CloseCameraSofDevice();
	printf("Exit done\n");
	return 0;
}