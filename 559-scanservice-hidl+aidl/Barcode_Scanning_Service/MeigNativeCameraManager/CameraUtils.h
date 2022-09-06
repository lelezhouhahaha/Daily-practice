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
#include "camera/NdkCameraError.h"
#include "camera/NdkCameraManager.h"
#include "camera/NdkCameraMetadata.h"
#include "camera/NdkCameraDevice.h"
#include "camera/NdkCameraCaptureSession.h"
#include "media/NdkImage.h"
#include "media/NdkImageReader.h"

#include "CameraInter.h"

#define LOG_TAG "MeigNativeCamera"