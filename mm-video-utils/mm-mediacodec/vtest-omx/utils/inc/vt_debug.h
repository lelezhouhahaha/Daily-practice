/*-------------------------------------------------------------------
Copyright (c) 2013-2015 Qualcomm Technologies, Inc. All Rights Reserved.
Qualcomm Technologies Proprietary and Confidential
--------------------------------------------------------------------*/

#ifndef _VT_DEBUG_H
#define _VT_DEBUG_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <cutils/properties.h>
#include <libavutil/common.h>


// undefine this to print results to the display
#define _ANDROID_LOG_

// define these to print the low/medium logs
#define _ANDROID_LOG_DEBUG
#define _ANDROID_LOG_DEBUG_LOW

#ifdef _ANDROID_LOG_
#include <utils/Log.h>
#include <utils/threads.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#undef LOG_NDEBUG
#undef LOG_TAG
#define LOG_NDEBUG 0
#define LOG_TAG "VTEST"

#define DEBUG_HIGH 1
#define DEBUG_MED 2
#define DEBUG_LOW 4

static int vtest_getLoglevel(void){
    char prop[PROPERTY_VALUE_MAX];
    int i = 0;
    int vtest_debug_flag ;
    property_get("persist.meigcam.loglevel.debug", prop, "56");
    vtest_debug_flag = atoi(prop);

    return vtest_debug_flag;
}

//-------------------------------------------------------------------
//            Add by huangfusheng 2020-5-22
//-------------------------------------------------------------------
#define VTEST_MSG_HIGH(fmt, ...)    av_log(NULL, AV_LOG_VERBOSE, "VT_HIGH %s::%d " fmt"\n", __FUNCTION__, __LINE__, ## __VA_ARGS__)
#define VTEST_MSG_PROFILE(fmt, ...) av_log(NULL, AV_LOG_WARNING, "VT_PROFILE %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)
#define VTEST_MSG_ERROR(fmt, ...)   av_log(NULL, AV_LOG_ERROR, "VT_ERROR %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)
#define VTEST_MSG_FATAL(fmt, ...)   av_log(NULL, AV_LOG_FATAL, "VT_ERROR %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)
///#define VTEST_MSG_HIGH(fmt, ...) ALOGE("VT_HIGH %s::%d " fmt, __FUNCTION__, __LINE__,  ##__VA_ARGS__)
///#define VTEST_MSG_PROFILE(fmt, ...) ALOGE("VT_PROFILE %s::%d " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
///#define VTEST_MSG_ERROR(fmt, ...) ALOGE("VT_ERROR %s::%d " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
///#define VTEST_MSG_FATAL(fmt, ...) ALOGE("VT_ERROR %s::%d " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)

#ifdef _ANDROID_LOG_DEBUG
///#define VTEST_MSG_MEDIUM(fmt, ...) ALOGD_IF(vtest_getLoglevel() & DEBUG_MED, "VT_MED %s::%d " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define VTEST_MSG_MEDIUM(fmt, ...) av_log(NULL, vtest_getLoglevel() & 0x3F, "VT_MED %s::%d " fmt"\n", __FUNCTION__, __LINE__, ##__VA_ARGS__)
#else
#define VTEST_MSG_MEDIUM(fmt, ...)
#endif

#ifdef _ANDROID_LOG_DEBUG_LOW
///#define VTEST_MSG_LOW(fmt, ...) ALOGD_IF(vtest_getLoglevel() & DEBUG_LOW, "VT_LOW %s::%d " fmt, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define VTEST_MSG_LOW(fmt, ...)    av_log(NULL, vtest_getLoglevel() & 0x3F, "VT_LOW %s::%d " fmt"\n", __FUNCTION__, __LINE__, ##__VA_ARGS__)
#else
#define VTEST_MSG_LOW(fmt, ...)
#endif

#else

#define VTEST_MSG_HIGH(fmt, ...) fprintf(stderr, "VT_HIGH %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)
#define VTEST_MSG_PROFILE(fmt, ...) fprintf(stderr, "VT_PROFILE %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)
#define VTEST_MSG_ERROR(fmt, ...) fprintf(stderr, "VT_ERROR %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)
#define VTEST_MSG_FATAL(fmt, ...) fprintf(stderr, "VT_ERROR %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)

#ifdef  VTEST_MSG_LOG_DEBUG
#define VTEST_MSG_LOW(fmt, ...) fprintf(stderr, "VT_LOW %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)
#define VTEST_MSG_MEDIUM(fmt, ...) fprintf(stderr, "VT_MED %s::%d " fmt"\n",__FUNCTION__, __LINE__,## __VA_ARGS__)
#else
#define VTEST_MSG_LOW(fmt, ...)
#define VTEST_MSG_MEDIUM(fmt, ...)
#endif

#endif //#ifdef _ANDROID_LOG_

#endif // #ifndef _VT_DEBUG_H
