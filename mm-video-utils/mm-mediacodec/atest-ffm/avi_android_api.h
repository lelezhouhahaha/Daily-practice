/*----------------------------------------------------------------------
Copyright (c) 2017-2018 Qualcomm Technologies, Inc. All Rights Reserved.
Qualcomm Technologies Proprietary and Confidential
------------------------------------------------------------------------*/
#ifdef __ANDROID_API__
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <android/log.h>
//Linux...
#ifdef __cplusplus
extern "C"
{
#endif
#include <libavutil/common.h>
#ifdef __cplusplus
};
#endif

#define SYS_LOG_TAG "nmplayer"
#define FFMLOG(level, TAG, ...)    ((void)__android_log_vprint(level, TAG, __VA_ARGS__))
#define _DEBUG 0x01
static FILE *fp = NULL;
va_list vll;

static void syslog_print(void *ptr, int level, const char *fmt, va_list vl)
{
    switch(level) {
    case AV_LOG_DEBUG:
        //FFMLOG(ANDROID_LOG_VERBOSE, SYS_LOG_TAG, fmt, vl);
        break;
    case AV_LOG_VERBOSE:
        FFMLOG(ANDROID_LOG_DEBUG, SYS_LOG_TAG, fmt, vl);
        break;
    case AV_LOG_INFO:
        FFMLOG(ANDROID_LOG_INFO, SYS_LOG_TAG, fmt, vl);
        break;
    case AV_LOG_WARNING:
        FFMLOG(ANDROID_LOG_WARN, SYS_LOG_TAG, fmt, vl);
        break;
    case AV_LOG_ERROR:
        FFMLOG(ANDROID_LOG_ERROR, SYS_LOG_TAG, fmt, vl);
        break;
    }
}



#define _DATETIME_SIZE  32


// GetDate - 获取当前系统日期
/**
 *  函数名称：GetDate
 *  功能描述：取当前系统日期
 *
 *  输出参数：char * psDate  - 系统日期，格式为yyymmdd
 *  返回结果：0 -> 成功
 */
int GetDate(char * psDate)
{
    time_t nSeconds;
    struct tm * pTM;

    time(&nSeconds); // 同 nSeconds = time(NULL);
    pTM = localtime(&nSeconds);

    /* 系统日期,格式:YYYMMDD */
    sprintf(psDate,"%04d-%02d-%02d",
            pTM->tm_year + 1900, pTM->tm_mon + 1, pTM->tm_mday);

    return 0;
}

// GetTime  - 获取当前系统时间
/**
 *  函数名称：GetTime
 *  功能描述：取当前系统时间
 *
 *  输出参数：char * psTime -- 系统时间，格式为HHMMSS
 *  返回结果：0 -> 成功
 */
int GetTime(char * psTime)
{
    time_t nSeconds;
    struct tm * pTM;

    time(&nSeconds);
    pTM = localtime(&nSeconds);

    /* 系统时间，格式: HHMMSS */
    sprintf(psTime, "%02d:%02d:%02d",
            pTM->tm_hour, pTM->tm_min, pTM->tm_sec);

    return 0;
}

// GetDateTime - 取当前系统日期和时间
/**
 *  函数名称：GetDateTime
 *  功能描述：取当前系统日期和时间
 *
 *  输出参数：char * psDateTime -- 系统日期时间,格式为yyymmddHHMMSS
 *  返回结果：0 -> 成功
 */
int GetDateTime(char * psDateTime)
{
    time_t nSeconds;
    struct tm * pTM;

    time(&nSeconds);
    pTM = localtime(&nSeconds);

    /* 系统日期和时间,格式: yyyymmddHHMMSS */
    sprintf(psDateTime, " %04d-%02d-%02d %02d:%02d:%02d   ",
            pTM->tm_year + 1900, pTM->tm_mon + 1, pTM->tm_mday,
            pTM->tm_hour, pTM->tm_min, pTM->tm_sec);

    return 0;
}

static void av_log_callback(void* ptr, int level, const char* fmt, va_list vl)
{
#ifdef _DEBUG

  if(level > AV_LOG_VERBOSE)
    return;

  if(!fp)
  {
    FFMLOG(ANDROID_LOG_VERBOSE, SYS_LOG_TAG,"open /sdcard/nmplayer_logcat.txt failed.",vll);
    fp = fopen("/sdcard/nmplayer_logcat.txt","wb");
  }

  if(fp)
  {
      if (level != AV_LOG_INFO)
      {
          char DateTime[_DATETIME_SIZE];
          memset(DateTime, 0, sizeof(DateTime));
          /* 获取系统当前日期时间 */
          GetDateTime(DateTime);
          fwrite(DateTime, strlen(DateTime), 1, fp);
      }
      vfprintf(fp,fmt,vl);
      fflush(fp);
  }
#endif
}

static void syslog_init()
{
    char prop[32];
    property_get("persist.meigcam.loglevel.debugx", prop, "0");
    int loglevel = atoi(prop);
    if(!loglevel)
    {
        FFMLOG(ANDROID_LOG_VERBOSE, SYS_LOG_TAG,"syslog_print.",vll);
        av_log_set_callback(syslog_print);
        return;
    }
	FFMLOG(ANDROID_LOG_VERBOSE, SYS_LOG_TAG,"av_log_callback.",vll);
    av_log_set_callback(av_log_callback);
}

#endif // __ANDROID_API__
