/* Copyright (c) 2013, 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

/*****************************************************************************
 *    Includes
 *****************************************************************************/
#include <stdlib.h>
#include <utils/Log.h>
#include <time.h>
#include <sys/types.h>
#include <dirent.h>
#include <string.h>
#include <stdbool.h>
#include <cutils/properties.h>
#include "vtest_Core.h"

#include "mm_qaudio_test.h"
#include "mm_qcamera_unit_test.h"
#include "mm_qcamera_osd.h"
#include "mm_qcamera_scale.h"
#include "display_main.h"
#include "AVI_MediaCodec.h"
#include "meig_cam_hw.h"
#include "avi_android_api.h"

/*****************************************************************************
 *    Dedinfe
 *****************************************************************************/
#define MEIGLOG_TAG "HALMeigCam"
//#define DUMP_SNAPSHOT_IN_FILE
#undef  CDBG
#define CDBG(fmt, args...) ALOGE(fmt, ##args)

#define SNAPSHOT_WIDTH          2560//2592
#define SNAPSHOT_HEIGHT         1440//1458
#define VIDEO_DST_WIDTH         1280
#define VIDEO_DST_HEIGHT        720
#define DISPLAY_WIDTH           320
#define DISPLAY_HEIGHT          180
#define OPEN_WIDTH_960          960
#define OPEN_HEIGHT_540         540


//#define MEIGCAM_SETCOLORMODE


/*****************************************************************************
 *    Shared Typedefs & Macros
 *****************************************************************************/
bool gps_update_flag;
bool start_vidrecord_flag;              //for video snapshot
char device_id[100] = "序列号：";
char station_id[100] = "单位编号：";
char police_id_and_name[100] = "警员编号：";
static char gps_info [77];
char video_file_name[50];
char video_file_path[100];
int32_t  video_strt_num;
int32_t  video_resolution;              //for osd position for different video resolution
bool video_format = false;
int32_t  upload_video_resolution;
int32_t  video_split_length;
takephoto_info_t *global_photo_info;
struct meigcam_hw_device_t *hwdev = NULL;
struct timeval time_begin;
long long time_prerecord_begin;
static pthread_mutex_t mutex;      //Define lock

QAudioTest m_audioTest;
AVIMediaCodec *pMediaCodec =NULL;
AVIMediaCodec *uMediaCodec =NULL;
void *osd_ctl = NULL;

/*****************************************************************************
 *    Camera Global Variable
 *****************************************************************************/
static cam_dimension_t stream_dim = {0, 0};
static cam_dimension_t preview_dim = {0, 0};
static cam_dimension_t upload_dim = {0, 0};
static cam_dimension_t video_dim = {0, 0};
static snapshot_info_t snapshot_info = {0, 0, 0, 0};
//static mm_display_app_buf_t app_buf = {NULL, 0, 0};

static cam_dimension_t video_res[] =
{   {1920, 1080},
    {1280, 720},
#ifdef OPEN_RES2
    {960,  540},
#else
    {848,  480},
#endif
    {2304,  1296}
};

/*****************************************************************************
 *    Common hardware methods
 *****************************************************************************/
static int meigcam_device_open(const hw_module_t* module, const char* name,
                hw_device_t** device);

static struct hw_module_methods_t meigcam_module_methods = {
    .open = meigcam_device_open
};

static int get_station_device_police_id(char *watermark_base_info);

static hw_module_t meig_cam_common =
{
    .tag                    = HARDWARE_MODULE_TAG,
    .module_api_version     = 0,
    .hal_api_version        = HARDWARE_HAL_API_VERSION,
    .id                     = MEIGCAM_HARDWARE_MODULE_ID,
    .name                   = "Meig Cam Hal Module",
    .author                 = "Meig",
    .methods                = &meigcam_module_methods,
    .dso                    = NULL,
    .reserved               = {0}
};

static int meigcam_mkdirs(const char *sPathName)
{
    char   DirName[256];
    strcpy(DirName,   sPathName);
    int   i,len   =   strlen(DirName);
    if(DirName[len-1]!='/')
        strcat(DirName,   "/");
    len   =   strlen(DirName);
    for(i=1;   i<len;   i++){
        if(DirName[i]=='/'){
            DirName[i]   =   0;
            if(access(DirName,NULL)!=0){
                if(mkdir(DirName,0755)==-1){
                    VTEST_MSG_HIGH("mkdir error");
                    return   -1;
                }
            }
        DirName[i]   =   '/';
        }
    }
    return 0;
}

/*****************************************************************************
 *    The MeigCam Module
 *****************************************************************************/
meigcam_module_t HAL_MODULE_INFO_SYM = {
    .common = meig_cam_common,
};

static inline int32_t Round(float x)
{
  return (int32_t) ((x > 0) ? (x+0.5f) : (x-0.5f));
}

void dump_yuv(char *name,int frameid, char *ext, uint8_t *buffer, int width,int height)
{
       char file_name[64];
       snprintf(file_name, sizeof(file_name),QCAMERA_DUMP_FRM_LOCATION"%s_%04d.%s",name, frameid, ext);
       FILE *file = fopen(file_name,"wb+");
       fwrite(buffer,sizeof(uint8_t),(width*height*3) >> 1,file);
       fclose(file);
}

void get_resolution(int res, cam_dimension_t *dim) {
    if(dim != NULL && res < sizeof(video_res)/sizeof(cam_dimension_t)) {
        dim->width = video_res[res].width;
        dim->height = video_res[res].height;
        VTEST_MSG_HIGH("set resolution to width = %d, height = %d", dim->width, dim->height);
    } else {
        VTEST_MSG_ERROR("Error parameters res = %d!", res);
    }
    return;
}

/*===========================================================================
 * videorecord_time_count_unit: For videoname callback time counting
 *                              录像名回调的时间计数器
 * time_begin_record：  video record start time
 * time_end_record:     video record start time
 * time_diff_vidrecord: video record time cost
 *
 * vidrecord_flag = false  --->>  clear all the variables about time counting
 *                                used in vtest_stopvideo or meigcam_close
 *                                停止录像给所有局部变量和静态变量清零
 * return: count_value     --->>  the number of the video split
                                  返回值为分段录像的视频段数
 *===========================================================================*/
int videorecord_time_count_unit(int32_t video_split_length, bool vidrecord_flag)
{
    static int repeat_count_num = 0;
    static long long time_begin_record;

    if ( !vidrecord_flag ) {
        //录像结束，静态变量清零
        repeat_count_num  = 0;
        time_begin_record = 0;
        //video record over, clear static variables
        //录像结束，全局变量清零
        time_prerecord_begin = 0;
        //time_prerecord_end   = 0;
        //video record over, clear global variables
        VTEST_MSG_HIGH("MEGLOG: Clear All!!!!");

        return 0;

    } else {
        //录像开始 ---->> video on <<------

        //---------->> acquire time now <<----------
        //==============获取当前时间======================
        long long time_tmp, time_span;
        struct timeval time;
        if (gettimeofday(&time, NULL) == 0) {
            time_tmp = ((long long) time.tv_sec * 1000);
        }
        //==============获取当前时间======================

        if ( !repeat_count_num ) {

            //录像开始后仅进入一次，time_begin_record获得一个值后不再赋值
            //================获得录像开始时间===================
            if (time_prerecord_begin) {
                //包含预录时间
                //------>> include prerecord <<------
                time_span = (time_tmp - time_prerecord_begin);
                time_span = (time_span > 30 * 1000) ? 30 * 1000 : time_span;
                VTEST_MSG_LOW("MEGLOG: time_span = %lld", time_span);
                time_begin_record = time_tmp - time_span;
            } else {
                //不包含预录时间
                //------>> NOT include prerecord <<------
                time_begin_record = time_tmp;
            }
            VTEST_MSG_HIGH("MEGLOG: time_begin_record = %lld", time_begin_record);
            VTEST_MSG_HIGH("MEGLOG: aaaaaaaaaaaaaaaaaaaaa");
            //================获得录像开始时间===================
            repeat_count_num++;
        } else {

            //录像开始后，每次调用仅有time_end_record该变量的值在变化
            //------>> time difference from record begin <<------
            long long time_diff_vidrecord, time_end_record;
            time_end_record = time_tmp;
            VTEST_MSG_LOW("MEGLOG: time_end_record = %lld", time_end_record);
            time_diff_vidrecord = time_end_record - time_begin_record;
            VTEST_MSG_LOW("onRecordVideoName: time_diff_vidrecord = %lld, video_split_length = %d", time_diff_vidrecord, video_split_length);

            //根据时间跨度计算第几段，例如600s分段时间，录像到800s，返回值为1表示第二段
            //------>> counting the number of the video split <<------
            int count_value;
            count_value = Round((time_diff_vidrecord/1000.0) / video_split_length);
            VTEST_MSG_HIGH("onRecordVideoName: count_value = %lf", (time_diff_vidrecord/1000.0)/video_split_length );
            VTEST_MSG_HIGH("onRecordVideoName: time_diff_vidrecord = %d", count_value);
            VTEST_MSG_HIGH("MEGLOG: bbbbbbbbbbbbbbbbbbbbbbbbb");
            return count_value;
        }

        return 0;
    }

}

/*===========================================================================
 * FUNCTION   : meigcam_frame_cb
 *
 * DESCRIPTION: add osd to video stream
 *
 * PARAMETERS :
 *   @pFrameHeapBase   : buffer pointer
 *   @frame_len        :
 *
 * RETURN     : Null
 *
 *==========================================================================*/
void meigcam_frame_cb(void *pFrameHeapBase, size_t frame_len, uint32_t frame_idx)
{
    /* update gps osd info */
    /*if (gps_update_flag)
    {
        VTEST_MSG_LOW("enter into gps update");
        update_gps_osd(osd_ctl, gps_info);
        gps_update_flag = false;
    }*/

    /* add osd info to video stream */
    //osd_start(osd_ctl, pFrameHeapBase, stream_dim.width, stream_dim.height, video_resolution);

    return;
}

/*===========================================================================
 * FUNCTION   : meigcam_snapshot_cb
 *
 * DESCRIPTION: enqueue buffer back to Android
 *
 * PARAMETERS :
 *   @my_obj       : channel object
 *   @buf          : buf ptr to be enqueued
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void meigcam_snapshot_cb(void *pFrameHeapBase, size_t frame_len, uint32_t frame_idx)
{
    int rc;
    /* update gpsinfo */
    if (gps_update_flag) {
        VTEST_MSG_LOW("enter into gps update");
        update_gps_osd(osd_ctl, gps_info);
        gps_update_flag = false;
    }

    /* choose liveshot or snapshot */
    if (snapshot_info.vidsnap_flag) {
        /*start liveshot */
        photo_size_info *liveshot_info_t;
        liveshot_info_t = liveshot_init((uint8_t *)pFrameHeapBase, SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT);

        /* Add osd */
        osd_start(osd_ctl, liveshot_info_t->buffer_ptr, liveshot_info_t->width, liveshot_info_t->height, video_resolution);
        /* write into JPEG */
        rc = mm_scale_start(liveshot_info_t->buffer_ptr,
                liveshot_info_t->width,
                liveshot_info_t->height,
                global_photo_info->photo_start_num,
                global_photo_info->photo_save_path,
                global_photo_info->photo_serial_num,
                global_photo_info->photo_time_stamp,
                snapshot_info.vidsnap_flag);

        /* free buffer */
        liveshot_deinit(liveshot_info_t->buffer_ptr);
        if (rc) {
            VTEST_MSG_ERROR("write JPEG error!");
        }
    } else {
        /* Normal snapshot procedure */
        /* add osd */
        osd_start(osd_ctl, pFrameHeapBase,
                                    SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT, video_resolution);

        rc = mm_scale_start(pFrameHeapBase,
                    SNAPSHOT_WIDTH,
                    SNAPSHOT_HEIGHT,
                    global_photo_info->photo_start_num + frame_idx,
                    global_photo_info->photo_save_path,
                    global_photo_info->photo_serial_num,
                    global_photo_info->photo_time_stamp,
                    snapshot_info.vidsnap_flag);

        if (rc) {
            VTEST_MSG_ERROR("write JPEG error!");
        }

#ifdef DUMP_SNAPSHOT_IN_FILE
    dump_yuv("snapshot", frame_idx, "yuv",(uint8_t *)pFrameHeapBase, SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT);
#endif
    }


    return;
}

/*===========================================================================
 * FUNCTION   : meigcam_preview_cb
 *
 * DESCRIPTION: enqueue buffer back to Android
 *
 * PARAMETERS :
 *   @my_obj       : channel object
 *   @buf          : buf ptr to be enqueued
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void meigcam_preview_cb(void *pFrameHeapBase, size_t frame_len, uint32_t frame_idx)
{
#ifdef DUMP_PRV_IN_FILE
    dump_yuv("display", frame_idx,"nv21",(uint8_t *)pFrameHeapBase,DISPLAY_WIDTH,DISPLAY_HEIGHT);
#endif
    VTEST_MSG_LOW("BEGIN - addr=%p, length=%zu, frame idx = %d\n",
          pFrameHeapBase, frame_len, frame_idx);

    return;
}

/*===========================================================================
 * FUNCTION   : meigcam_upload_stream_cb
 *
 * DESCRIPTION: enqueue buffer back to Android
 *
 * PARAMETERS :
 *   @my_obj       : channel object
 *   @buf          : buf ptr to be enqueued
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void meigcam_upload_stream_cb(void *pFrameHeapBase, size_t frame_len, uint32_t frame_idx)
{
    int rc = 0;

    /* update gps osd info */
    if (gps_update_flag)
    {
        VTEST_MSG_LOW("enter into gps update");
        update_gps_osd(osd_ctl, gps_info);
        gps_update_flag = false;
    }

    /* add osd info to video stream */
    osd_start_upload(osd_ctl, pFrameHeapBase, upload_dim.width, upload_dim.height, upload_video_resolution);

#ifdef DUMP_PRV_IN_FILE
    dump_yuv("V720P", frame_idx, "nv12",(uint8_t *)pFrameHeapBase,VIDEO_DST_WIDTH,VIDEO_DST_HEIGHT);
#endif
    if(uMediaCodec) {
        rc = uMediaCodec->pVideoCore->Push(pFrameHeapBase);
        if(rc) {
            VTEST_MSG_LOW("Push frame failed!");
        } else {
            VTEST_MSG_LOW("Push frame successed.");
        }
    }
    VTEST_MSG_LOW("BEGIN - addr=%p, length=%zu, frame idx = %d\n",
          pFrameHeapBase, frame_len, frame_idx);

    return;
}
/*===========================================================================
 * FUNCTION   : meigcam_video_cb
 *
 * DESCRIPTION: enqueue buffer back to Android
 *
 * PARAMETERS :
 *   @my_obj       : channel object
 *   @buf          : buf ptr to be enqueued
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void meigcam_local_stream_cb(void *pFrameHeapBase, size_t frame_len, uint32_t frame_idx)
{
    int32_t rc = 0;

    /* update gps osd info */
    if (gps_update_flag)
    {
        VTEST_MSG_LOW("enter into gps update");
        update_gps_osd(osd_ctl, gps_info);
        gps_update_flag = false;
    }

    /* add osd info to video stream */
    pthread_mutex_lock(&mutex); //Lock
    if (start_vidrecord_flag)
        osd_start(osd_ctl, pFrameHeapBase, video_dim.width, video_dim.height, video_resolution);

#ifdef DUMP_PRV_IN_FILE
    dump_yuv("V1080P",frame_idx,"nv12",(uint8_t *)pFrameHeapBase,DEFAULT_PREVIEW_WIDTH,DEFAULT_PREVIEW_HEIGHT);
#endif
    if(pMediaCodec) {
        rc = pMediaCodec->pVideoCore->Push(pFrameHeapBase);
        pthread_mutex_unlock(&mutex); //Lock
        if(rc) {
            VTEST_MSG_LOW("Push frame failed!");
        } else {
            VTEST_MSG_LOW("Push frame successed.");
        }
    }
    VTEST_MSG_LOW("BEGIN - addr=%p, length=%zu, frame idx = %d\n",
          pFrameHeapBase, frame_len, frame_idx);

    return;
}

 /*===========================================================================
 * FUNCTION   : meigcam_yuv_cb
 *
 * DESCRIPTION: enqueue buffer back to app
 *
 * PARAMETERS :
 *   @my_obj       : channel object
 *   @buf          : buf ptr to be enqueued
 *
 *==========================================================================*/
void meigcam_yuv_cb(void *yuv2app_buf, int yuv2app_width, int yuv2app_height)
{

    struct meigcam_yuv2app_t yuv2app_t;
    yuv2app_t.yuv2app_buffer = yuv2app_buf;
    yuv2app_t.yuv2app_width  = yuv2app_width;
    yuv2app_t.yuv2app_height = yuv2app_height;
    //VTEST_MSG_ERROR("zhazhao: hwdev %p yuv2app_buffer = %p yuv2app_width %d yuv2app_height %d", hwdev,yuv2app_t.yuv2app_buffer,yuv2app_t.yuv2app_width,yuv2app_t.yuv2app_height);
    if(hwdev != NULL && hwdev->user != NULL)
        hwdev->notify->yuv2app_cb(yuv2app_t, hwdev->user);

}

/*===========================================================================
 * FUNCTION   : vtest_swvenc_send
 *
 * DESCRIPTION: send callback
 *
 * PARAMETERS :
 *   @buffer       : buffer addr
 *   @bytes        : buffer length
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void vtest_swvenc_send(char *buffer, uint32_t bytes)
{
    struct meigcam_context_t bContext;
    bContext.buffer = (char *)buffer;
    bContext.filelen = bytes;
    VTEST_MSG_LOW("buffer = %p, bytes = %d\n", buffer, bytes);
    if(hwdev != NULL && hwdev->user != NULL)
        hwdev->notify->notify_cb(bContext,hwdev->user);

    return;
}

/*===========================================================================
 * FUNCTION   : vtest_swvenc_diag
 *
 * DESCRIPTION: video name integration
 *
 * PARAMETERS :
 *   @pDir        : video restore path
 *   @pFileName   : video name string
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void vtest_swvenc_diag(char* pDir, char* pFileName)
{
    int32_t rc = -1;
    char strs_in_stor[4][10] = {{'.'}, {".."}, {"self"}, {"emulated"}};


    /* find SD card*/
    struct dirent *all_dir;
    DIR *dirptr = NULL;
    dirptr = opendir("/storage/");
    while (all_dir = readdir(dirptr)){
        if ((strcmp(all_dir -> d_name, strs_in_stor[0])) &&
            (strcmp(all_dir -> d_name, strs_in_stor[1])) &&
            (strcmp(all_dir -> d_name, strs_in_stor[2])) &&
            (strcmp(all_dir -> d_name, strs_in_stor[3]))) {
                /* external SD card */
                sprintf(pDir, "%s", video_file_path);
                if (((access(pDir, F_OK)) == 0)){
                    /* path already exist */
                }
                else {
                    /* no path && make it*/
                    meigcam_mkdirs(pDir);
                }
                break;
            }
        else {
            /* no SD card */
            sprintf(pDir, "/sdcard%s", video_file_path);
            if (((access(pDir, F_OK)) == 0)){
                /* path already exist */
            }
            else {
                /* no path && make it*/
                meigcam_mkdirs(pDir);
            }
        }
    }

    /* get video_split_count number to video name suffix*/
    int32_t video_split_count_tmp, video_strt_num_tmp;
    video_split_count_tmp = videorecord_time_count_unit(video_split_length, true);
    video_strt_num_tmp    = video_strt_num + video_split_count_tmp;
    VTEST_MSG_HIGH("MEGLOG: video_strt_num = %d, video_split_count_tmp = %d, video_strt_num_tmp = %d", \
                    video_strt_num, video_split_count_tmp, video_strt_num_tmp);

    /* get video section */
    char video_part[] = "22222";
    sprintf(video_part, "_%05d", video_strt_num_tmp);

    /* get time part section */
    char time_part[]  = "0000000_11112233445566000";
    char        hms_part_now[] = "000000";
    static char hms_part_ori[] = "000000";
    static int  hms_val_ori = 0;
    int         hms_val_now = 0;
    int         hms_val_tmp;
    time_t now;
    struct tm* t_tm;

    time(&now);
    t_tm = localtime(&now);
    sprintf(hms_part_now, "%02d%02d%02d", t_tm->tm_hour, t_tm->tm_min, t_tm->tm_sec);
    hms_val_now = t_tm->tm_hour * 60 * 60 + t_tm->tm_min * 60 + t_tm->tm_sec;
    hms_val_tmp = hms_val_now - hms_val_ori;
    VTEST_MSG_LOW("zhou: hms_val_tmp = %d", hms_val_tmp);


    if ( (hms_val_tmp <= 1) && (hms_val_tmp >= 0) ) {
        sprintf(time_part, "%s_%4d%02d%02d%s000", video_file_name, t_tm->tm_year + 1900, t_tm->tm_mon + 1, t_tm->tm_mday, hms_part_ori);
        VTEST_MSG_LOW("zhou: hms_part_ori = \"%s\"", hms_part_ori);
    } else {
        sprintf(time_part, "%s_%4d%02d%02d%s000", video_file_name, t_tm->tm_year + 1900, t_tm->tm_mon + 1, t_tm->tm_mday, hms_part_now);
        VTEST_MSG_ERROR("zhou: hms_part_now = \"%s\"", hms_part_now);
        strcpy(hms_part_ori, hms_part_now);
        hms_val_ori = hms_val_now;
        VTEST_MSG_LOW("zhou: hms_part_ori = \"%s\", hms_val_ori = %d", hms_part_ori, hms_val_ori);
    }

    VTEST_MSG_HIGH("year = %d, mon = %d, day = %d.", t_tm->tm_year, t_tm->tm_mon + 1, t_tm->tm_mday);

    /* get outFileString */
    if(video_format) {
        sprintf(pFileName, "%s%s_H", time_part, video_part);
    } else {
        sprintf(pFileName, "%s%s", time_part, video_part);
    }

    VTEST_MSG_LOW("************************************");
    VTEST_MSG_LOW("MEGLOG: pDir = %s, pFileName = %s", pDir, pFileName);
    VTEST_MSG_LOW("************************************");

    if(closedir(dirptr)) {
        VTEST_MSG_ERROR("close dir failed");
    } else {
        VTEST_MSG_LOW("close dir successed");
    }
    return;
}

/*===========================================================================
 * FUNCTION   : vtest_swvenc_name
 *
 * DESCRIPTION: send video name
 *
 * PARAMETERS :
 *   @name    : video name
 *
 * RETURN     : void
 *==========================================================================*/
void vtest_swvenc_name(char *filename)
{
    VTEST_MSG_HIGH("filename = %s.\n", filename);
    if(hwdev != NULL && hwdev->user != NULL)
        hwdev->notify->filename_cb(filename,hwdev->user);

    return;
}

AVICallbacks sAVICallbacks = {
    sizeof(AVICallbacks),
    vtest_swvenc_diag,
    vtest_swvenc_send,
    NULL,
    NULL,
    vtest_swvenc_name
};

/*===========================================================================
 * FUNCTION   : atest_swaenc_send
 *
 * DESCRIPTION: send callback
 *
 * PARAMETERS :
 *   @buffer       : buffer addr
 *   @bytes        : buffer length
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void atest_pcm_cb(const void *pFrameHeapBase, size_t frame_len, uint32_t frame_idx)
{
    int64_t rc;

    Q_UNUSED(frame_idx)
    if(pMediaCodec) {
        rc = pMediaCodec->pushData(pFrameHeapBase, frame_len);
        VTEST_MSG_LOW("pFrameHeapBase=[%p] frame_len = %d.\n", pFrameHeapBase, frame_len);
        if(rc) {
            VTEST_MSG_LOW("Push audio pcm failed!");
        } else {
            VTEST_MSG_LOW("Push audio pcm successed.");
        }
    }

    return;
}
static void atest_acquire_wakelock()
{
    VTEST_MSG_LOW("%s: @@@@@@\n", __func__);
}

static void atest_release_wakelock()
{
    VTEST_MSG_LOW("%s: @@@@@@\n", __func__);
}

static int atest_set_threadevent(ThreadEvent event)
{
    //JavaVM* javaVm = AndroidRuntime::getJavaVM();
    Q_UNUSED(event)
    return 0;
}

ACallbacks sACallbacks = {
    sizeof(ACallbacks),
    atest_pcm_cb,
    atest_acquire_wakelock,
    atest_release_wakelock,
    atest_set_threadevent
};
static int meigcam_open(struct record_info *record_info)
{

    int rc = 0;
    int open_rc = 0;
    char aviPath[MAX_FIFO_LEN]={0};

    /* Property used for snapshot */
    char property_ready_str[10] = "ready";
    property_set("photo.complete.flag", property_ready_str);

    /* Flag used for liveshot */
    start_vidrecord_flag = false;
    if(!record_info)
    {
        return -1;
    }
    sprintf(aviPath, "%s", AVI_DEMUXER_LOCATION);
    if (((access(aviPath, F_OK)) == 0)){
        /* path already exist */
    }
    else {
        /* no path && make it*/
        meigcam_mkdirs(aviPath);
    }
    ///Set the size of the data stream
    video_resolution = record_info->video_resolution;
    upload_video_resolution = record_info->video_resolution;
    get_resolution(record_info->video_resolution, &stream_dim);
    if(record_info->video_resolution == HAL_VIDEO_RESOLUTION_720_480) {
      stream_dim.width  = OPEN_WIDTH_960;
      stream_dim.height = OPEN_HEIGHT_540;
    }
    ///ADD OSD.
	//strcpy(record_info->watermark_base_info, "单位-Bhao：1234/Xv.列：09876/员,工：6754u  Hi 美格~!@#$%^&*<>-=+");
    ALOGE("[xuhao] record_info->watermark_base_info = %s \n", record_info->watermark_base_info);
    get_station_device_police_id(record_info->watermark_base_info);
    memcpy(gps_info, record_info->watermark_gps_info, sizeof(gps_info));
    osd_ctl    = osd_init(device_id, police_id_and_name, station_id, gps_info);
    gps_update_flag = false;
    //VTEST_MSG_HIGH("MeigLog: police_num = %s, gps_info = %s", police_num, gps_info);

    ///Create a sound signal.
    m_audioTest.initializeAudio();
    m_audioTest.registerCallback(&sACallbacks);
    VTEST_MSG_HIGH("record_info->cam_frame_rate = %d",record_info->cam_frame_rate);
    property_set("persist.meigcam.25fps", record_info->cam_frame_rate?"1":"0");

    open_rc = mm_app_tc_open(record_info->cam_id);
    if(open_rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_tc_open fail, rc = %d\n", open_rc);
        return open_rc;
    }

    ///Registration callback function.
    rc = mm_app_RegisterPreviewCallback(meigcam_preview_cb);
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_RegisterPreviewCallback fail, rc = %d\n",rc);
        return rc;
    }
    rc = mm_app_RegisterUploadCallback(meigcam_upload_stream_cb);
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_RegisterUploadCallback fail, rc = %d\n",rc);
        return rc;
    }
    rc = mm_app_RegisterVideoCallback(meigcam_local_stream_cb);
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_RegisterVideoCallback fail, rc = %d\n",rc);
        return rc;
    }
    rc = mm_app_RegisterSnapShotCallback(meigcam_snapshot_cb);
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_RegisterSnapShotCallback fail, rc = %d\n",rc);
        return rc;
    }
    rc = mm_app_RegisterFrameCallback(meigcam_frame_cb);
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_RegisterFrameCallback fail, rc = %d\n",rc);
        return rc;
    }
    rc = mm_app_tc_start_stream(&stream_dim);
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_tc_start_stream fail, rc = %d\n",rc);
        return rc;
    }
    rc = mm_app_RegisterYUV2AppCallback(meigcam_yuv_cb);
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_RegisterYUV2AppCallback fail, rc = %d\n",rc);
        return rc;
    }
    hwdev = record_info->hwDevice;
    if(rc < 0)
    {
        VTEST_MSG_ERROR("meigcam_prerecord fail, rc = %d\n", rc);
        return rc;
    }
    VTEST_MSG_HIGH("meigcam_open ok, open_rc = %d\n", open_rc);
    pthread_mutex_init(&mutex, NULL); //Initialize the lock mutex
    return open_rc;
}

static int meigcam_close(struct meigcam_hw_device_t *dev)
{
    int rc;
    start_vidrecord_flag = false;
    videorecord_time_count_unit( video_split_length, start_vidrecord_flag);

    /*osd deinit*/
    if(osd_ctl) {
        osd_deinit(osd_ctl);
		osd_ctl = NULL;
	}

    if(!dev)
    {
        return -1;
    }
/*
    rc = dev->meigcam_stoppreview(dev);
    if(rc < 0)
    {
        VTEST_MSG_ERROR("meigcam_stoppreview fail, rc = %d\n", rc);
        return rc;
    }
*/
    ///先停止数据流然后再关闭camera，同时退出所有线程会不会导致server奔溃
    rc = mm_app_tc_stop_stream();
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_tc_stop_stream fail, rc = %d\n", rc);
        return rc;
    }
    rc = mm_app_tc_close();
    if(rc < 0)
    {
        VTEST_MSG_ERROR("mm_app_tc_close fail, rc = %d\n", rc);
        return rc;
    }
    property_set("persist.meigcam.25fps", "0");
    pthread_mutex_destroy(&mutex);  //Destroy lock
    VTEST_MSG_HIGH("meigcam_close ok, rc = %d\n", rc);
    return rc;
}

static int meigcam_startpreview(struct meigcam_hw_device_t *dev)
{
    int rc = 0;
    void *display_buffer = NULL;
    if(!dev)
    {
        return -1;
    }
    VTEST_MSG_HIGH("dispaly preview EEE.");

    preview_dim.width = DISPLAY_WIDTH;
    preview_dim.height = DISPLAY_HEIGHT;

    ///先打开数据流再打开display
    display_buffer = mm_app_tc_start_preview(&preview_dim);
    if (display_buffer == NULL) {
        VTEST_MSG_ERROR("Display Alloc memory failed!\n");
        return -1;
    }
    usleep(50*1000);
    //app_buf.data   = display_buffer;
    //app_buf.width  = preview_dim.width;
   //app_buf.height = preview_dim.height;
    start_display_show_data(display_buffer);
    VTEST_MSG_HIGH("dispaly preview XXX.");
    return rc;
}

static int meigcam_stoppreview(struct meigcam_hw_device_t *dev)
{
    int rc = 0;
    if(!dev)
    {
        return -1;
    }
    ///先关闭display再停止数据流
    stop_display_show_data();
    usleep(1000);
    mm_app_tc_stop_preview();
    VTEST_MSG_HIGH("dispaly preview off.");
    return rc;
}

static int meigcam_takephoto(struct meigcam_hw_device_t *dev, takephoto_info_t *photo_info)
{
    /***********************************************************
     * property_get: get status of the JPEG coding
     * if "ready"  : is able to take photo
     * if "busy"   : is not able to take photo
     ***********************************************************/
    char property_value[10];
    property_get("photo.complete.flag", property_value, "0");

    if ( strcmp(property_value, "ready") ) {
        VTEST_MSG_ERROR("MEGLOG: photo take is not ready !!!!");
        return -1;
    }

    /* start to take a photo */
    int rc = 0;
    void *snapshot_buffer;

    /* inform system starts to take photo */
    char property_busy_str[10] = "busy";
    property_set("photo.complete.flag", property_busy_str);

    if(!dev)
    {
        VTEST_MSG_ERROR("luosiyuan: meigcam_takephoto dev is NULL !!!\n");
        return -1;
    }

    global_photo_info = photo_info;
    snapshot_info.width = SNAPSHOT_WIDTH;
    snapshot_info.height = SNAPSHOT_HEIGHT;
    snapshot_info.photo_num = photo_info->photo_multi_shot_num;
    snapshot_info.vidsnap_flag  = start_vidrecord_flag;

    VTEST_MSG_HIGH("meigcam_takephoto serial_number: %s\n",
        global_photo_info->photo_serial_num);
    VTEST_MSG_HIGH("meigcam_takephoto photo_resolution = %d \n",
        photo_info->photo_resolution);
    VTEST_MSG_HIGH("meigcam_takephoto photo_multi_shot_num = %d \n",
        photo_info->photo_multi_shot_num);


    //Initialize the resolution of the interpolation
    if (snapshot_info.vidsnap_flag) {
        scale_for_liveshot(video_resolution);
    } else {
        scale_for_snapshot(snapshot_info.width, snapshot_info.height, photo_info->photo_resolution, snapshot_info.photo_num);
    }

    //Start snapshot and set the number of consecutive photos
    snapshot_buffer = mm_app_tc_start_snapshot(&snapshot_info);
    if (snapshot_buffer == NULL) {
        VTEST_MSG_ERROR("SnapShot Alloc memory failed!\n");
        return -1;
    }
    rc = mm_app_tc_stop_snapshot();
    //scale_deinit();
    if(if_dropped_frame){
      VTEST_MSG_ERROR("take photo failed  if_dropped_frame %d!\n",if_dropped_frame);
      return -2;
    }
    return rc;
}

static int vtest_startvideo(struct record_info *record_info)
{
    void *video_buffer;
    int rc = 0;
    VTEST_MSG_HIGH(" E !");

    if(!record_info)
    {
        return -1;
    }

    start_vidrecord_flag = true;
    video_resolution     = record_info->video_resolution;
    get_resolution(record_info->video_resolution, &video_dim);
    video_buffer = mm_app_tc_start_video(&video_dim);
    if (video_buffer == NULL)
    {
        VTEST_MSG_ERROR("Video Alloc memory failed!\n");
        return -1;
    }
    if(pMediaCodec != NULL)
    {
        ///Start video
        pMediaCodec->pVideoCore->SetStreamType(Stream_Local);
        pMediaCodec->pVideoCore->SetResolution(video_dim.width, video_dim.height);
        pMediaCodec->pVideoCore->SetBitrate(record_info->video_bit_rate);
        pMediaCodec->pVideoCore->SetFramerate(record_info->video_frame_rate);
        pMediaCodec->pVideoCore->SetSourceFormat(Format_NV12);
        pMediaCodec->pVideoCore->SetCodingType((VideoCodingType)record_info->video_format, ((VideoCodingType)record_info->video_format == VIDEO_CodingAVC ? VIDEO_AVCProfileMain : VIDEO_HEVCProfileMain));
    }
    VTEST_MSG_HIGH(" X !");
    return rc;
}

static int vtest_stopvideo(struct meigcam_hw_device_t * dev)
{
    int rc = 0;
    if(!dev)
    {
        return -1;
    }

    VTEST_MSG_HIGH(" E !");
    start_vidrecord_flag = false;
    video_resolution     = 0;
    videorecord_time_count_unit( video_split_length, start_vidrecord_flag);

    rc = mm_app_tc_stop_video();
    VTEST_MSG_HIGH("X, rc = %d\n", rc);

    return rc;
}

static int meigcam_startprerecord(struct record_info *record_info)
{
    void *video_buffer;
    int rc = 0;
    VTEST_MSG_HIGH(" E !");

    if(!record_info)
    {
        return -1;
    }
    if(pMediaCodec != NULL)
    {
        if (gettimeofday(&time_begin, NULL) == 0) {
            time_prerecord_begin = ((long long) time_begin.tv_sec * 1000);
            VTEST_MSG_LOW("MEGLOG: time_prerecord_begin = %lld", time_prerecord_begin);
        }

        ///Start Pre Media
        rc = pMediaCodec->startPreRecord(record_info);
        if(!start_vidrecord_flag)
            rc = vtest_startvideo(record_info);
        pMediaCodec->pVideoCore->SetPreTimes(30);
        rc = pMediaCodec->pVideoCore->PreStart();
    }
    VTEST_MSG_HIGH(" X !");
    return rc;
}

static int meigcam_stopprerecord(struct meigcam_hw_device_t * dev)
{
    int rc = 0;
    VTEST_MSG_HIGH(" E !");

    if(!dev)
    {
        return -1;
    }
    if(pMediaCodec != NULL)
    {
        ///Stop Pre Media
        rc = pMediaCodec->stopPreRecord();
        if(start_vidrecord_flag)
            rc = pMediaCodec->pVideoCore->PreStop();
        rc = vtest_stopvideo(dev);
    }
    VTEST_MSG_HIGH("X, rc = %d\n", rc);

    return rc;
}

static int meigcam_startrecord(struct record_info *record_info)
{
    void *video_buffer;
    int rc = 0;
    VTEST_MSG_HIGH(" E !");

    if(!record_info)
    {
        return -1;
    }

    /* video name parameters*/
    video_split_length =    record_info->video_split_length;
    video_strt_num   =      record_info->video_file_start_number;
    strcpy(video_file_name, record_info->video_serial_number);
    strcpy(video_file_path, record_info->video_save_path);
    if(pMediaCodec != NULL)
    {
        if((VideoCodingType)record_info->video_format == VIDEO_CodingHEVC) {
            video_format = true;
        } else {
            video_format = false;
        }
        ///Start audio and video
        rc = pMediaCodec->startRecord(record_info);
        if(!start_vidrecord_flag)
            rc = vtest_startvideo(record_info);
        pMediaCodec->pVideoCore->SetPartTimes(30);
        pMediaCodec->pVideoCore->SetPFrame(30);
        rc = pMediaCodec->pVideoCore->Start();
    }
    VTEST_MSG_HIGH(" X !");
    return rc;
}

static int meigcam_stoprecord(struct meigcam_hw_device_t * dev)
{
    int rc = 0;
    VTEST_MSG_HIGH(" E !");

    if(!dev)
    {
        return -1;
    }
    if(pMediaCodec != NULL)
    {
        ///Stop Media
        rc = pMediaCodec->stopRecord();
        if(start_vidrecord_flag)
            rc = pMediaCodec->pVideoCore->Stop();
        rc = vtest_stopvideo(dev);
    }
    VTEST_MSG_HIGH("X, rc = %d\n", rc);

    return rc;
}

static int meigcam_startuploadrecord(struct record_info *record_info)
{
    void *upload_buffer;
    int rc = 0;
    VTEST_MSG_HIGH(" E !");

    if(!record_info)
    {
        return -1;
    }

    get_resolution(record_info->video_resolution, &upload_dim);
    upload_video_resolution     = record_info->video_resolution;
    upload_buffer = mm_app_tc_start_upload(&upload_dim);
    if (upload_buffer == NULL)
    {
        VTEST_MSG_ERROR("Upload Alloc memory failed!\n");
        return -1;
    }
    if(uMediaCodec != NULL)
    {
        uMediaCodec->pVideoCore->SetStreamType(Stream_Upload);
        uMediaCodec->pVideoCore->SetResolution(upload_dim.width, upload_dim.height);
        uMediaCodec->pVideoCore->SetBitrate(record_info->video_bit_rate);
        uMediaCodec->pVideoCore->SetFramerate(record_info->video_frame_rate);
        uMediaCodec->pVideoCore->SetSourceFormat(Format_NV12);
        uMediaCodec->pVideoCore->SetPFrame(record_info->video_I_frame_space);
        uMediaCodec->pVideoCore->SetCodingType((VideoCodingType)record_info->video_format, ((VideoCodingType)record_info->video_format == VIDEO_CodingAVC ? VIDEO_AVCProfileMain : VIDEO_HEVCProfileMain));
        rc = uMediaCodec->pVideoCore->Start();
    }
    VTEST_MSG_HIGH(" X !");
    return rc;
}

static int meigcam_stopuploadrecord(struct meigcam_hw_device_t * dev)
{
    int rc = 0;
    VTEST_MSG_HIGH(" E !");
    if(!dev)
    {
        return -1;
    }
    if(uMediaCodec != NULL)
    {
        rc = uMediaCodec->pVideoCore->Stop();
    }
    mm_app_tc_stop_upload();
    VTEST_MSG_HIGH("X, rc = %d\n", rc);

    return rc;
}

static int meigcam_startyuv2app(struct meigcam_hw_device_t *dev,int startyuv)
{
    void *display_buffer = NULL;
    if(!dev)
    {
        return -1;
    }
    mm_app_tc_start_yuv2app(startyuv);
    return 0;
}

static int meigcam_stopyuv2app(struct meigcam_hw_device_t *dev,int stopyuv)
{
    if(!dev)
    {
        return -1;
    }
    mm_app_tc_stop_yuv2app(stopyuv);
    return 0;
}

static int meigcam_setuploadbitrate(struct meigcam_hw_device_t * dev, int bitrate)
{
    int rc = 0;
    if (!dev) {
       return -1;
    }
    if(uMediaCodec != NULL)
    {
        uMediaCodec->pVideoCore->SetBitrate((OMX_U32)bitrate);
    }
    return rc;
}

static int meigcam_setuploadPframe(struct meigcam_hw_device_t * dev, int pframe)
{
    int rc = 0;
    if (!dev) {
       return -1;
    }
    if(uMediaCodec != NULL)
    {
        uMediaCodec->pVideoCore->SetPFrame((OMX_U32)pframe);
    }
    return rc;
}

static int meigcam_setupvideoframerate(struct meigcam_hw_device_t * dev, int framerate)
{
    int rc = 0;
    VTEST_MSG_HIGH("TAG: E\n");
    if (!dev) {
       return -1;
    }
    if(uMediaCodec != NULL)
    {
        uMediaCodec->pVideoCore->SetFramerate((OMX_U32)framerate);
    }
    VTEST_MSG_HIGH("TAG: X\n");
    return rc;
}

static int meigcam_getgpsinfo(struct meigcam_hw_device_t * dev, char *watermark_gps_info)
{
    int rc = 0;
    if(!dev)
    {
        return -1;
    }

    strcpy(gps_info, watermark_gps_info);
    gps_update_flag = true;
    VTEST_MSG_HIGH("TAG: %s - %s: meigcam_getgpsinfo gps_info %s ok, rc = %d\n", MEIGLOG_TAG, __func__,gps_info,rc);

    return 0;
}

static int meigcam_getbaseinfo(struct meigcam_hw_device_t * dev, char *watermark_base_info)
{
    int rc = 0;
    if(dev == NULL || watermark_base_info == NULL || start_vidrecord_flag)
    {
        return -1;
    }

    rc = get_station_device_police_id(watermark_base_info);

	if (osd_ctl == NULL)
	{
        return -2;
	}
	update_base_info_osd(osd_ctl, device_id, police_id_and_name, station_id);
	VTEST_MSG_HIGH("[xuhao] meigcam_getbaseinfo ok, rc = %d \n", rc);

    return 0;
}

static int get_station_device_police_id(char *watermark_base_info)
{
	if (watermark_base_info == NULL)
	{
		return -1;
	}

    char *token, *sepstr = watermark_base_info;
    const char * const split = "/";

	token = strsep(&sepstr, split);
    if (token == NULL)
        return -EAVIBADF;
	sprintf(station_id, "%s", token);
	VTEST_MSG_HIGH("[xuhao] station_id = %s, len = %d \n", station_id, strlen(station_id));

	token = strsep(&sepstr, split);
    if(token == NULL)
        return -EAVIBADF;
    sprintf(device_id, "%s", token);
	VTEST_MSG_HIGH("[xuhao] station_id = %s, device_id = %s len = %d \n", station_id, device_id, strlen(device_id));

    token = strsep(&sepstr, split);
    if(token == NULL)
        return -EAVIBADF;
    sprintf(police_id_and_name, "%s ", token);
	VTEST_MSG_HIGH("[xuhao] station_id = %s, device_id = %s, police_id_and_name = %s len = %d \n", station_id, device_id, police_id_and_name, strlen(police_id_and_name));

    return 0;
}

static int meigcam_setcolormode(struct meigcam_hw_device_t * dev, int color_mode)
{
    int rc = 0;
    if (!dev) {
       return -1;
    }

#ifdef MEIGCAM_SETCOLORMODE
    static int switch_key = 0;
    switch_key = ~switch_key;
    if(switch_key){
      rc = mm_app_set_effect(color_mode);
    } else {
      rc = mm_app_set_effect(color_mode-1);
    }
#else
    rc = mm_app_set_effect(color_mode);
#endif
    return 0;
}


static int meigcam_setzoom(struct meigcam_hw_device_t *dev, int zoom_target)
{
    if (!dev) {
        return -1;
    }
    VTEST_MSG_HIGH("luosiyuan start meigcam_setzoom, zoom_target is %d\n", zoom_target);
    mm_app_set_zoom(zoom_target);
    return 0;
}

static int app_close(struct meigcam_hw_device_t *dev)
{
    // NOP for MDP barcode
    if (dev) {
        free(dev);
    }
    VTEST_MSG_HIGH("\n");
    return 0;
}


static int meigcam_writeaudio(struct meigcam_hw_device_t *dev, const void* data, int count)
{
    int rc = 0;

    if (!dev) {
        return -1;
    }
    m_audioTest.writeData(data, count);
    //rc = pMediaCodec->pushData(data, count);
    VTEST_MSG_LOW("%s:%d rc = %d\n", __func__,__LINE__,rc);
    return 0;
}


static int meigcam_setnotify(struct meigcam_hw_device_t *dev,
                                MeigcamCallbacks* callbacks, void *user)
{
    /* Decorate with locks */
    dev->notify = callbacks;
    dev->user = user;
    VTEST_MSG_HIGH("luosiyuan dev->user is %p\n", dev->user);
    return 0;
}

/* open device handle to one of the barcodes
 *
 * assume barcode service will keep singleton of each barcode
 * so this function will always only be called once per barcode instance
 */

static int meigcam_device_open(const hw_module_t* module, const char* name,
                hw_device_t** device)
{
    uint8_t *data=NULL;

    if (strcmp(name, MEIGCAM_HARDWARE_MODULE_ID)) {
        return -1;
    }
    syslog_init();
    VTEST_MSG_HIGH("MeiGe cam Openning");

    struct meigcam_hw_device_t *dev;
    dev = (meigcam_hw_device_t *)malloc(sizeof(meigcam_hw_device_t));
    if(!dev)
        return -ENOMEM;

    memset(dev, 0, sizeof(*dev));

    /************************************************************************/
    dev->common.tag = HARDWARE_DEVICE_TAG;
    dev->common.version = 1;
    dev->common.module = const_cast<hw_module_t*>(module);
    dev->common.close = (int (*)(struct hw_device_t*))app_close;

    dev->meigcam_open                 =    meigcam_open;
    dev->meigcam_close                =    meigcam_close;
    dev->meigcam_startpreview         =    meigcam_startpreview;
    dev->meigcam_stoppreview          =    meigcam_stoppreview;
    dev->meigcam_takephoto            =    meigcam_takephoto;
    dev->meigcam_startprerecord       =    meigcam_startprerecord;
    dev->meigcam_stopprerecord        =    meigcam_stopprerecord;
    dev->meigcam_startvideorecord     =    meigcam_startrecord;
    dev->meigcam_stopvideorecord      =    meigcam_stoprecord;
    dev->meigcam_startuploadrecord    =    meigcam_startuploadrecord;
    dev->meigcam_stopuploadrecord     =    meigcam_stopuploadrecord;
    dev->meigcam_startyuv2app         =    meigcam_startyuv2app;
    dev->meigcam_stopyuv2app          =    meigcam_stopyuv2app;
    dev->meigcam_setuploadbitrate     =    meigcam_setuploadbitrate;
    dev->meigcam_setuploadPframe      =    meigcam_setuploadPframe;
    dev->meigcam_setupvideoframerate  =    meigcam_setupvideoframerate;
    dev->meigcam_setcolormode         =    meigcam_setcolormode;
    dev->meigcam_writeaudio           =    meigcam_writeaudio;
    dev->meigcam_getgpsinfo           =    meigcam_getgpsinfo;
    dev->meigcam_setnotify            =    meigcam_setnotify;
    dev->meigcam_setzoom              =    meigcam_setzoom;
    dev->notify                       =    NULL;
	dev->meigcam_getbaseinfo          =    meigcam_getbaseinfo;
    VTEST_MSG_HIGH("$$$$$$ The MeigCam is initialized successfully $$$$$$\n");

    *device = (struct hw_device_t*)dev;
#if AVFFMPEG_AAC_SUPPORT
    pMediaCodec = new AVIMediaCodec;
    pMediaCodec->initializeMedia();
    pMediaCodec->registerCallback(&sAVICallbacks);

    uMediaCodec = new AVIMediaCodec;
    uMediaCodec->pVideoCore->RegisterUploadCallback(vtest_swvenc_send);
#endif
    VTEST_MSG_HIGH("$$$$$$ pMediaCodec && uMediaCodec $$$$$$\n");
    mm_app_init();
    //mm_scale_init();
    return 0;
}
