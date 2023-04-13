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
 *	Includes
 *****************************************************************************/
// Camera dependencies
#include <stdio.h>
#include <stdlib.h>
#include "unistd.h"
#include <pthread.h>
#include <sys/types.h>
#include <assert.h>
#include <string.h>
#include <errno.h>

#include "mm_qcamera_app.h"
#include "mm_qcamera_dbg.h"
#include "mm_qcamera_unit_test.h"
#include "mm_qthread_pool.h"
#include "libyuv.h"
#include <poll.h>

/*****************************************************************************
 *	Camera Global Variable
 *****************************************************************************/
//TODO:
//fps_mode_t fps_mode = FPS_MODE_FIXED;
int zoom_level;
int zoom_max_value;
mm_camera_lib_handle lib_handle;
//mm_camera_test_obj_t my_cam_obj;
mm_camera_app_t my_cam_app;

static PFN_FRAMEPOST_CB s_pfnFrameCB = NULL;
static PFN_FRAMEPOST_CB s_pfnSnapShotCB = NULL;
static PFN_FRAMEPOST_CB s_pfnPreviewCB = NULL;
static PFN_FRAMEPOST_CB s_pfnUploadCB = NULL;
static PFN_FRAMEPOST_CB s_pfnVideoCB = NULL;
static PFN_YUV2APP_CB s_pfnYuv2AppCB = NULL;
/*****************************************************************************
 *	Shared Typedefs & Macros
 *****************************************************************************/
cam_dimension_t i420_stream_dim = {0, 0};
static uint8_t *stream_i420_buf = NULL;
static uint8_t *snapshot_i420_buf = NULL;
static uint8_t *video_nv12_buf = NULL;
static uint8_t *video_i420_buf = NULL;
static uint8_t *upload_i420_buf = NULL;
static uint8_t *upload_nv12_buf = NULL;
static uint8_t *display_i420_buf = NULL;
static uint8_t *display_nv21_buf = NULL;

static int sFd = -1;
static int pFd = -1;
static int uFd = -1;
static int vFd = -1;
static int p_mFd = -1;
static int u_mFd = -1;
static int v_mFd = -1;
static int yuv2app_switch = 0;
static int nKillThread = KILL_THREAD_NO;

void *mm_app_thread_snapshot(void * arg);
void *mm_app_thread_preview(void * arg);
void *mm_app_thread_upload(void * arg);
void *mm_app_thread_video(void * arg);
void mm_app_display_pool_status(CThread_pool_t * pPool);



/*****************************************************************************
 *  Zoom & TakePic
 *****************************************************************************/
int set_zoom(mm_camera_lib_handle *lib_handle, int zoom_action_param, int zoom_target)
{
    //默认代码
    if (zoom_action_param == ZOOM_IN) {
        zoom_level += ZOOM_STEP;
        if (zoom_level > zoom_max_value)
            zoom_level = zoom_max_value;
    } else if (zoom_action_param == ZOOM_OUT) {
        zoom_level -= ZOOM_STEP;
        if (zoom_level < ZOOM_MIN_VALUE)
            zoom_level = ZOOM_MIN_VALUE;
    } else {
        LOGD(" Invalid zoom_action_param value\n");
        return -EINVAL;
    }

    if(zoom_target < 0)
    {
        LOGD("zoom_target < 0, use 0\n");
        zoom_target = 0;
    }
    else if(zoom_target > 91)
    {
        LOGD("zoom_target > max_value, use max_value\n");
        zoom_target = 91;
    }
    zoom_level = zoom_target;
    return mm_camera_lib_send_command(lib_handle,
                                      MM_CAMERA_LIB_ZOOM,
                                      &zoom_level,
                                      NULL);
}

//added by luosiyuan 2018.11.30, use default interface to  zoom
int mm_app_set_zoom(int zoom_target)
{
    set_zoom(&lib_handle, ZOOM_IN, zoom_target);
    return 0;
}

int mm_app_set_effect (int effect)
{
    return mm_camera_lib_send_command(&lib_handle,
                                      MM_CAMERA_LIB_EFFECT,
                                      &effect,
                                      NULL);
}

void mm_app_set_asf (int enable_wnr,int sharpness)
{
    setWNR(&lib_handle.test_obj,enable_wnr);
    setSharpness(&lib_handle.test_obj,sharpness);
}

void mm_app_set_contrast(int value)
{
    setContrast(&lib_handle.test_obj, value);
}

void mm_app_set_saturation(int value)
{
    setSaturation(&lib_handle.test_obj, value);
}

void mm_app_set_sharpness(int value)
{
    setSharpness(&lib_handle.test_obj, value);
}

//added by wanghongbin
void mm_app_set_exposure_metering(int mode)
{
    setExposureMetering(&lib_handle.test_obj, mode);
}

void mm_app_set_tintless(int tintless)
{
    setTintless(&lib_handle.test_obj, tintless);
}

void mm_app_set_antibanding(int antibanding)
{
    setAntibanding(&lib_handle.test_obj, antibanding);
}

void cropYUV(uint8_t *src_, int width, int height,
             uint8_t *dst_, int dst_width, int dst_height,
             int left, int top)
{
    //裁剪的区域大小不对
    if (left + dst_width > width || top + dst_height > height) {
        return;
    }

    //left和top必须为偶数，否则显示会有问题
    if (left % 2 != 0 || top % 2 != 0) {
        return;
    }

    int src_length = width*height*3/2;
    uint8_t *src_i420_data = src_;
    uint8_t *dst_i420_data = dst_;


    int dst_i420_y_size = dst_width * dst_height;
    int dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);

    uint8_t *dst_i420_y_data = dst_i420_data;
    uint8_t *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    uint8_t *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;

    ConvertToI420((const uint8 *) src_i420_data, src_length,
                  (uint8 *) dst_i420_y_data, dst_width,
                  (uint8 *) dst_i420_u_data, dst_width >> 1,
                  (uint8 *) dst_i420_v_data, dst_width >> 1,
                  left, top,
                  width, height,
                  dst_width, dst_height,
                  kRotate0, FOURCC_I420);

    return;
}


/*===========================================================================
 * FUNCTION   : mm_app_stream_cb
 *
 * DESCRIPTION: enqueue buffer back to video
 *
 * PARAMETERS :
 *   @my_obj       : channel object
 *   @buf          : buf ptr to be enqueued
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void mm_app_stream_cb(mm_camera_buf_def_t *frame)
{
    int n;
    char file_name[64];

    if(pFd < 0)
    {
        pFd = open(preview_fifo, O_WRONLY | O_NONBLOCK);
        if(pFd < 0)
            LOGD("Open Preview FIFO Failed!\n");
    }
    if(uFd < 0)
    {
        uFd = open(upload_fifo, O_WRONLY | O_NONBLOCK);
        if(uFd < 0)
            LOGD("Open UpLoad FIFO Failed!\n");
    }
    if(vFd < 0)
    {
        vFd = open(video_fifo, O_WRONLY | O_NONBLOCK);
        if(vFd < 0)
            LOGD("Open Video FIFO Failed!\n");
    }
    LOGD("File_name =%s\n", file_name);
    //LOGE("zhazhao yuv2app_switch =%d\n", yuv2app_switch);
    if(yuv2app_switch)
       (*s_pfnYuv2AppCB)(frame->buffer, i420_stream_dim.width, i420_stream_dim.height);
    if(stream_i420_buf != NULL)
        NV21ToI420(frame->buffer, i420_stream_dim.width,
                (uint8_t *)frame->buffer + i420_stream_dim.width * i420_stream_dim.height, i420_stream_dim.width,
                &stream_i420_buf[0], i420_stream_dim.width,
                &stream_i420_buf[i420_stream_dim.width * i420_stream_dim.height], i420_stream_dim.width >> 1,
                &stream_i420_buf[(i420_stream_dim.width * i420_stream_dim.height * 5) >> 2], i420_stream_dim.width >> 1,
                i420_stream_dim.width, i420_stream_dim.height);

    //n = snprintf(file_name, sizeof(file_name), "P_C%d_%04d", 256,frame->frame_idx);
    //(*s_pfnFrameCB)((void *)stream_i420_buf, (i420_stream_dim.width*i420_stream_dim.height*3)/2, frame->frame_idx);
    n = snprintf(file_name, sizeof(file_name), "%04d", frame->frame_idx);
    if(write(pFd, file_name, n+1) < 0)
        LOGD("Write Preview FIFO Failed\n");
    if(write(uFd, file_name, n+1) < 0)
        LOGD("Write Uploade FIFO Failed\n");
    if(write(vFd, file_name, n+1) < 0)
        LOGD("Write Video FIFO Failed\n");

#ifdef DUMP_PRV_IN_FILE
    mm_app_dump_frame(frame, file_name, "yuv", frame->frame_idx);
#endif
    LOGD("XXX\n");
    return;
}

/*===========================================================================
 * FUNCTION   : mm_app_snapshot_cb
 *
 * DESCRIPTION: enqueue buffer back to video
 *
 * PARAMETERS :
 *   @my_obj       : channel object
 *   @buf          : buf ptr to be enqueued
 *
 * RETURN     : int32_t type of status
 *              0  -- success
 *              -1 -- failure
 *==========================================================================*/
void mm_app_snapshot_cb(mm_camera_buf_def_t *frame)
{
    int n;
    char file_name[64];

    if(sFd < 0)
    {
        sFd = open(snapshot_fifo, O_WRONLY | O_NONBLOCK);
        if(sFd < 0)
            LOGD("Open Preview FIFO Failed!\n");
    }

    LOGI("File_name =%s\n", file_name);

    if(snapshot_i420_buf != NULL)
        NV21ToI420(frame->buffer, REARCAM_SNAPSHOT_WIDTH,
                (uint8_t *)frame->buffer + REARCAM_SNAPSHOT_WIDTH * REARCAM_SNAPSHOT_HEIGHT, REARCAM_SNAPSHOT_WIDTH,
                &snapshot_i420_buf[0], REARCAM_SNAPSHOT_WIDTH,
                &snapshot_i420_buf[REARCAM_SNAPSHOT_WIDTH * REARCAM_SNAPSHOT_HEIGHT], REARCAM_SNAPSHOT_WIDTH >> 1,
                &snapshot_i420_buf[(REARCAM_SNAPSHOT_WIDTH * REARCAM_SNAPSHOT_HEIGHT * 5) >> 2], REARCAM_SNAPSHOT_WIDTH >> 1,
                REARCAM_SNAPSHOT_WIDTH, REARCAM_SNAPSHOT_HEIGHT);


    //n = snprintf(file_name, sizeof(file_name), "SSSS_C%d_%04d", 256,frame->frame_idx);
    n = snprintf(file_name, sizeof(file_name), "%04d", frame->frame_idx);
    if(write(sFd, file_name, n+1) < 0)
        LOGD("Write Preview FIFO Failed\n");

#ifdef DUMP_PRV_IN_FILE
    mm_app_dump_frame(frame, file_name, "yuv", frame->frame_idx);
#endif
    LOGI("XXX\n");
    return;
}

/*****************************************************************************
 *	Camera Open etc..
 *****************************************************************************/
int mm_app_tc_open(int cameraID)
{
    int rc = MM_CAMERA_OK;

    LOGI("mm_app_tc_open EEE\n");

    //memset(&my_cam_obj, 0, sizeof(mm_camera_test_obj_t));

    rc = mm_camera_lib_open(&lib_handle, cameraID);
    if (rc != MM_CAMERA_OK) {
        LOGE("mm_camera_lib_open() err=%d\n",  rc);
        return -1;
    }
    mm_app_set_asf(1, 12);
    mm_app_set_contrast(5);
    mm_app_set_saturation(5);
    mm_app_set_sharpness(2);
    mm_app_set_exposure_metering(CAM_AEC_MODE_FRAME_AVERAGE);
    mm_app_set_tintless(1);
    mm_app_set_antibanding(CAM_ANTIBANDING_MODE_50HZ);
    LOGI("mm_app_tc_open XXX\n");

    return rc;
}

int  mm_app_tc_close()
{
    int rc = MM_CAMERA_OK;

    LOGI("mm_app_tc_close EEE\n");
    nKillThread = KILL_THREAD_PREVIEW;
    usleep(1000);
    nKillThread = KILL_THREAD_UPLOAD;
    usleep(1000);
    nKillThread = KILL_THREAD_VIDEO;
    usleep(1000);
    //pThreadPool->Destroy((void*)pThreadPool);
    close(sFd);
    close(pFd);
    close(uFd);
    close(vFd);
    close(p_mFd);
    close(u_mFd);
    close(v_mFd);
    sFd = pFd = uFd = vFd = -1;
    p_mFd = u_mFd = v_mFd = -1;
    //必须先执行mm_app_stop_record_preview
    //才能执行mm_app_close
    LOGI("Close Thread--->mm_app_close\n");
    rc = mm_app_close(&lib_handle.test_obj);
    if (rc != MM_CAMERA_OK) {
        LOGE("mm_app_close() err=%d\n", rc);
        return rc;
    }

    LOGI("mm_app_tc_close XXX\n");
    return rc;
}

int mm_app_tc_start_stream(cam_dimension_t *dim)
{
    int rc = MM_CAMERA_OK;

    i420_stream_dim.width  = dim->width;
    i420_stream_dim.height = dim->height;
    if(stream_i420_buf == NULL)
    {
      stream_i420_buf =
        (uint8_t *)malloc((dim->width * dim->width * 3) >> 1);
    }
    if(snapshot_i420_buf == NULL)
    {
      snapshot_i420_buf =
        (uint8_t *)malloc((REARCAM_SNAPSHOT_WIDTH * REARCAM_SNAPSHOT_WIDTH * 3) >> 1);
    }

    LOGI("Stream dimesion: %d x %d\n",  i420_stream_dim.width, i420_stream_dim.height);
    rc = mm_camera_lib_set_stream_usercb(&lib_handle, mm_app_stream_cb);
    if(rc < 0)
    {
        LOGE("mm_app_tc_RegisterVideoUsercb fail, rc = %d\n",rc);
        return rc;
    }
    rc = mm_camera_lib_set_snapshot_usercb(&lib_handle, mm_app_snapshot_cb);
    if(rc < 0)
    {
        LOGE("mm_app_tc_RegisterSnapshotUsercb fail, rc = %d\n",rc);
        return rc;
    }
    LOGI("\n mm_app_tc_start_stream E\n");
    rc = mm_app_start_record_preview(&lib_handle.test_obj);
    if (rc != MM_CAMERA_OK) {
        LOGE(" mm_app_start_record_preview() err=%d\n", rc);
        return rc;
    }
    LOGI("\n mm_app_tc_start_stream X\n");

    return MM_CAMERA_OK;
}

int mm_app_tc_stop_stream()
{
    int rc = MM_CAMERA_OK;

    LOGI("\n mm_app_tc_stop_stream E\n");
    rc = mm_app_stop_record_preview(&lib_handle.test_obj);
    if (rc != MM_CAMERA_OK) {
        LOGE(" mm_app_stop_record_preview() err=%d\n", rc);
        return rc;
    }
    if(stream_i420_buf != NULL)
    {
        free(stream_i420_buf);
        stream_i420_buf = NULL;
    }
    if(snapshot_i420_buf != NULL)
    {
        free(snapshot_i420_buf);
        snapshot_i420_buf = NULL;
    }

    LOGI("\n mm_app_tc_stop_stream X\n");
    return MM_CAMERA_OK;
}

void *mm_app_tc_start_snapshot(snapshot_info_t *snapshot)
{
    LOGE(" E\n");
    nKillThread = KILL_THREAD_NO;
    LOGI("SnapShot[%d] dimesion: %d x %d\n",
        snapshot->photo_num, snapshot->width, snapshot->height);

    mm_app_display_pool_status(pThreadPool);
    if(pThreadPool != NULL)
        pThreadPool->AddWorkLimit((void *)pThreadPool, mm_app_thread_snapshot, (void *)snapshot);
    else
        return NULL;
    /**
     * 没加延迟发现连续投递任务时pthread_cond_wait()会收不到信号pthread_cond_signal() !!
     * 因为AddWorkUnlimit()进去后调用pthread_mutex_lock()把互斥锁锁上,导致pthread_cond_wait()
     * 收不到信号!!也可在AddWorkUnlimit()里面加个延迟,一般情况可能也遇不到这个问题
     */
    usleep(10);
    LOGE(" X\n");
    return (void *)snapshot_i420_buf;
}

void *mm_app_tc_start_preview(cam_dimension_t *dim)
{
    LOGE(" E\n");
    nKillThread = KILL_THREAD_NO;
    LOGI("Preview dimesion: %d x %d\n",  dim->width, dim->height);
    if(display_i420_buf == NULL)
      display_i420_buf = (uint8_t *)malloc((dim->width * dim->width * 3) >> 1);
    if(display_nv21_buf == NULL)
      display_nv21_buf = (uint8_t *)malloc((dim->width * dim->width * 3) >> 1);

    mm_app_display_pool_status(pThreadPool);
    if(pThreadPool != NULL)
        pThreadPool->AddWorkLimit((void *)pThreadPool, mm_app_thread_preview, (void *)dim);
    else
        return NULL;
    /**
     * 没加延迟发现连续投递任务时pthread_cond_wait()会收不到信号pthread_cond_signal() !!
     * 因为AddWorkUnlimit()进去后调用pthread_mutex_lock()把互斥锁锁上,导致pthread_cond_wait()
     * 收不到信号!!也可在AddWorkUnlimit()里面加个延迟,一般情况可能也遇不到这个问题
     */
    usleep(10);
    LOGE(" X\n");
    return (void *)display_nv21_buf;
}

void *mm_app_tc_start_upload(cam_dimension_t *dim)
{
    LOGE(" E\n");
    nKillThread = KILL_THREAD_NO;
    LOGI("Upload dimesion: %d x %d\n",  dim->width, dim->height);
    if(upload_i420_buf == NULL)
        upload_i420_buf = (uint8_t *)malloc((dim->width * dim->width * 3) >> 1);
    if(upload_nv12_buf == NULL)
        upload_nv12_buf = (uint8_t *)malloc((dim->width * dim->width * 3) >> 1);

    mm_app_display_pool_status(pThreadPool);
    if(pThreadPool != NULL)
        pThreadPool->AddWorkLimit((void *)pThreadPool, mm_app_thread_upload, (void *)dim);
    else
        return NULL;
    /**
     * 没加延迟发现连续投递任务时pthread_cond_wait()会收不到信号pthread_cond_signal() !!
     * 因为AddWorkUnlimit()进去后调用pthread_mutex_lock()把互斥锁锁上,导致pthread_cond_wait()
     * 收不到信号!!也可在AddWorkUnlimit()里面加个延迟,一般情况可能也遇不到这个问题
     */
    usleep(10);
    LOGE(" X\n");
    return (void *)upload_nv12_buf;
}

void *mm_app_tc_start_video(cam_dimension_t *dim)
{
    LOGE(" E\n");
    nKillThread = KILL_THREAD_NO;
    LOGI("Video dimesion: %d x %d\n",  dim->width, dim->height);
    if(video_nv12_buf == NULL)
      video_nv12_buf = (uint8_t *)malloc((dim->width * dim->width * 3) >> 1);
    if(video_i420_buf == NULL)
      video_i420_buf = (uint8_t *)malloc((dim->width * dim->width * 3) >> 1);

    mm_app_display_pool_status(pThreadPool);
    if(pThreadPool != NULL)
        pThreadPool->AddWorkLimit((void *)pThreadPool, mm_app_thread_video, (void *)dim);
    else
        return NULL;
    /**
     * 没加延迟发现连续投递任务时pthread_cond_wait()会收不到信号pthread_cond_signal() !!
     * 因为AddWorkUnlimit()进去后调用pthread_mutex_lock()把互斥锁锁上,导致pthread_cond_wait()
     * 收不到信号!!也可在AddWorkUnlimit()里面加个延迟,一般情况可能也遇不到这个问题
     */
    usleep(10);
    LOGE(" X\n");
    return (void *)video_nv12_buf;
}

int mm_app_tc_stop_snapshot()
{
    LOGI(" E\n");
    do {
        usleep(1000*100);
    }while (nKillThread != KILL_THREAD_SNAPSHOT);
    return MM_CAMERA_OK;
}


int mm_app_tc_stop_preview()
{
    LOGI(" E\n");
    nKillThread = KILL_THREAD_PREVIEW;
    usleep(100);
    if(display_i420_buf != NULL)
    {
        free(display_i420_buf);
        display_i420_buf = NULL;
    }
    if(display_nv21_buf != NULL)
    {
        free(display_nv21_buf);
        display_nv21_buf = NULL;
    }
    return MM_CAMERA_OK;
}

int mm_app_tc_stop_upload()
{
    LOGI(" E\n");
    nKillThread = KILL_THREAD_UPLOAD;
    usleep(100);

    if(upload_i420_buf != NULL)
    {
        free(upload_i420_buf);
        upload_i420_buf = NULL;
    }
    if(upload_nv12_buf != NULL)
    {
        free(upload_nv12_buf);
        upload_nv12_buf = NULL;
    }
    return MM_CAMERA_OK;
}

int mm_app_tc_stop_video()
{
    LOGI(" E\n");
    nKillThread = KILL_THREAD_VIDEO;
    usleep(100);
    if(video_nv12_buf != NULL)
    {
        free(video_nv12_buf);
        video_nv12_buf = NULL;
    }
    if(video_i420_buf != NULL)
    {
        free(video_i420_buf);
        video_i420_buf = NULL;
    }
    return MM_CAMERA_OK;
}

void mm_app_tc_start_yuv2app(int start_yuv)
{
    yuv2app_switch = start_yuv;
    LOGE("mm_qcamera mm_app_tc_stop_yuv2app yuv2app_switch %d start_yuv %d\n",yuv2app_switch,start_yuv);
}
void mm_app_tc_stop_yuv2app(int stop_yuv)
{
    yuv2app_switch = stop_yuv;
    LOGE("mm_qcamera mm_app_tc_stop_yuv2app yuv2app_switch %d stop_yuv %d\n",yuv2app_switch,stop_yuv);

}

/*****************************************************************************
 *	Snapshot & Preview & Video & Upload
 *****************************************************************************/
 void NV21ToNV12(uint8_t* nv21,uint8_t* nv12,int width,int height)
{
    int framesize = width*height;
    int i = 0,j = 0;
    if(nv21 == NULL || nv12 == NULL)
        return;
    memcpy(nv12,nv21,framesize);
    for(i = 0; i < framesize; i++){
        nv12[i] = nv21[i];
    }
    for (j = 0; j < framesize/2; j+=2)
    {
        nv12[framesize + j-1] = nv21[j+framesize];
    }
    for (j = 0; j < framesize/2; j+=2)
    {
        nv12[framesize + j] = nv21[j+framesize-1];
    }
}

int mm_app_RegisterFrameCallback(PFN_FRAMEPOST_CB pfnEventCB)
{
    s_pfnFrameCB = pfnEventCB;
    return 0;
}
int mm_app_RegisterSnapShotCallback(PFN_FRAMEPOST_CB pfnEventCB)
{
    s_pfnSnapShotCB = pfnEventCB;
    return 0;
}
int mm_app_RegisterPreviewCallback(PFN_FRAMEPOST_CB pfnEventCB)
{
    s_pfnPreviewCB = pfnEventCB;
    return 0;
}
int mm_app_RegisterUploadCallback(PFN_FRAMEPOST_CB pfnEventCB)
{
    s_pfnUploadCB = pfnEventCB;
    return 0;
}
int mm_app_RegisterVideoCallback(PFN_FRAMEPOST_CB pfnEventCB)
{
    s_pfnVideoCB = pfnEventCB;
    return 0;
}
int mm_app_RegisterYUV2AppCallback(PFN_YUV2APP_CB pYuv2AppCB)
{
    s_pfnYuv2AppCB = pYuv2AppCB;
    return 0;
}

void *mm_app_thread_snapshot(void * arg)
{
    int i = 0;
    int s_mFd = -1;
    size_t frame_len;
    uint32_t frame_idx;
    char buff[MAX_CFIFO_LEN];
    int rc = MM_CAMERA_OK;
    uint8_t *snapshot_crop_buf = NULL;
    if_dropped_frame = 0;
    struct pollfd fds;

    snapshot_info_t snapshot = *(snapshot_info_t *)arg;
    LOGI("Snapshot[%d] dimesion: %d x %d\n",
        snapshot.photo_num, snapshot.width, snapshot.height);
    /* 在for循环中定义
     * remove the definition of snapshot_crop_buf
     * if(snapshot_crop_buf == NULL)
     *{
     *  snapshot_crop_buf =
     *    (uint8_t *)malloc((snapshot.width * snapshot.height * 3) >> 1);
     *}
     */
    frame_len = (snapshot.width*snapshot.height*3)/2;
    //Create a snapshot FIFO pipeline
    if (access(snapshot_fifo, F_OK) == -1) {
        rc = mkfifo(snapshot_fifo, 0666);
        if(rc < 0 && errno!=EEXIST)
        {
            LOGE("Create SnapShot FIFO Failed!\n");
            return NULL;
        }
    }
    LOGI("Thread SnapShot is running !\n");

    for(int count = 0; count < snapshot.photo_num; count++)
    {
        /* 在for循环中定义snapshot_crop_buf，防止照片buf刷的太快导致照片出现绿色的问题 */
        snapshot_crop_buf =
            (uint8_t *)malloc((snapshot.width * snapshot.height * 3) >> 1);


        LOGI("mm_app_takepic start ...\n");
        rc = mm_app_start_live_snapshot(&lib_handle.test_obj);
        if (rc != MM_CAMERA_OK) {
            LOGE("mm_app_start_live_snapshot() cam_idx=%d, err=%d\n",
                        i, rc);
            return NULL;
        }
        if(s_mFd < 0)
        {
            s_mFd = open(snapshot_fifo, O_RDWR);
            fds.fd = s_mFd;
            fds.events = POLLIN;
            fds.revents = 0;
            rc = poll(&fds, 1, 1*800);
            if (rc == -1 || rc == 0) {
                /* Error or timeout */
                LOGI("timeout s_mFd %d ...\n",s_mFd);
                if_dropped_frame = 1;
                goto stop_snapshot;
            }
            else {
                LOGI("else s_mFd %d ...\n",s_mFd);
            }
        }
        if((rc = read(s_mFd, buff, MAX_CFIFO_LEN)) > 0)
        {
            frame_idx = atoi(buff);
            cropYUV(&snapshot_i420_buf[0], REARCAM_SNAPSHOT_WIDTH, REARCAM_SNAPSHOT_HEIGHT,
                    &snapshot_crop_buf[0], snapshot.width, snapshot.height,
                    0, 240);

            if (s_pfnSnapShotCB) {
                LOGI("[DBG] user defined own Snapshot cb [%d]. calling it...\n", frame_idx);
                //(*s_pfnSnapShotCB)((void *)snapshot_crop_buf, frame_len, frame_idx);
                (*s_pfnSnapShotCB)((void *)snapshot_crop_buf, frame_len, count);
            }
            /* mei_cam_hw.cpp中会根据属性photo.complete.flag的状态等待照片合成 */
            //usleep(10000);
        }

        /* wait for jpeg is done */
        //mm_camera_app_wait();
stop_snapshot:
        if(if_dropped_frame){
           char property_ready_str[10] = "ready";
           property_set("photo.complete.flag", property_ready_str);
           free(snapshot_crop_buf);
        }
        LOGI("mm_app_takepic step 1 ...\n");
        rc = mm_app_stop_live_snapshot(&lib_handle.test_obj);
        if (rc != MM_CAMERA_OK) {
            LOGE("mm_app_stop_live_snapshot() cam_idx=%d, err=%d\n",
                        i, rc);
            return NULL;
        }
        LOGI("mm_app_takepic end ...\n");
    }

    /* snapshot_crop_buf will be free after phototaking in “../mm_osd_scale/scale_src/mm_qcamera_scale.cpp”
     * snapshot_crop_buf这个buf在插值完成以后才能释放
     *if(snapshot_crop_buf != NULL)
     *{
     *    free(snapshot_crop_buf);
     *    snapshot_crop_buf = NULL;
     *}
     */
    nKillThread = KILL_THREAD_SNAPSHOT;
    close(s_mFd);  // 关闭FIFO文件
    LOGE("Thread SnapShot is Exit!\n");
    return NULL;
}

void *mm_app_thread_preview(void * arg)
{
    int nRead;
    size_t frame_len;
    uint32_t frame_idx;
    char buff[MAX_CFIFO_LEN];

    cam_dimension_t preview_dim = *(cam_dimension_t *)arg;
    LOGI("Preview dimesion: %d x %d\n", preview_dim.width, preview_dim.height);

    frame_len = (preview_dim.width*preview_dim.height*3)/2;
    ///Create preview FIFO pipeline
    nRead = mkfifo(preview_fifo, 0666);
    if(nRead < 0 && errno!=EEXIST)
    {
        LOGE("Create Preview FIFO Failed!\n");
        return NULL;
    }
    LOGI("Thread Preview is running SSSSSS!\n");
    if(p_mFd < 0) {
        p_mFd = open(preview_fifo, O_RDONLY);
        if(p_mFd < 0) // Open a preview FIFO with non-blocking write only
        {
            LOGE("Open Preview FIFO Failed!\n");
            return NULL;
        }
    }
    LOGI("Thread Preview is running EEEEEEE!\n");

    while(nKillThread != KILL_THREAD_PREVIEW)
    {
        if((nRead = read(p_mFd, buff, MAX_CFIFO_LEN)) > 0)
        {
            LOGD("Read Preview message: %s\n", buff);
            frame_idx = atoi(buff);
            I420Scale(&stream_i420_buf[0], i420_stream_dim.width,
                      &stream_i420_buf[i420_stream_dim.width * i420_stream_dim.height], i420_stream_dim.width >> 1,
                      &stream_i420_buf[(i420_stream_dim.width * i420_stream_dim.height * 5) >> 2], i420_stream_dim.width >> 1,
                      i420_stream_dim.width, i420_stream_dim.height,
                      &display_i420_buf[0], preview_dim.width,
                      &display_i420_buf[preview_dim.width * preview_dim.height], preview_dim.width >> 1,
                      &display_i420_buf[(preview_dim.width * preview_dim.height * 5) >> 2], preview_dim.width >> 1,
                      preview_dim.width, preview_dim.height,
                      3); //display
            I420ToNV21(&display_i420_buf[0], preview_dim.width,
                      &display_i420_buf[preview_dim.width * preview_dim.height], preview_dim.width >> 1,
                      &display_i420_buf[(preview_dim.width* preview_dim.height* 5) >> 2], preview_dim.width >> 1,
                      &display_nv21_buf[0], preview_dim.width,
                      &display_nv21_buf[preview_dim.width * preview_dim.height], preview_dim.width,
                      preview_dim.width, preview_dim.height); //display nv21
            if (s_pfnPreviewCB) {
                LOGD("[DBG] user defined own Display cb [%d]. calling it...\n", frame_len);
                //(*s_pfnPreviewCB)((void *)display_nv21_buf, frame_len, frame_idx);
            }
            /*if(yuv2app_switch && s_pfnYuv2AppCB){
               (*s_pfnYuv2AppCB)((void *)display_nv21_buf, preview_dim.width, preview_dim.height);
            }*/
        }
        usleep(10000);
    }
    //close(p_mFd);  // 关闭FIFO文件
    LOGE("Thread Preview is Exit!\n");
    return NULL;
}

void *mm_app_thread_upload(void * arg)
{
    int nRead;
    size_t frame_len;
    uint32_t frame_idx;
    char buff[MAX_CFIFO_LEN];

    cam_dimension_t upload_dim = *(cam_dimension_t *)arg;
    LOGI("Upload dimesion: %d x %d\n", upload_dim.width, upload_dim.height);

    LOGD("Thread Upload is running !\n");
    frame_len = (upload_dim.width*upload_dim.height*3)/2;
    ///Create upload FIFO pipeline
    nRead = mkfifo(upload_fifo, 0666);
    if(nRead < 0 && errno!=EEXIST)
    {
        LOGE("Create Upload FIFO Failed!\n");
        return NULL;
    }

    if(u_mFd < 0) {
        u_mFd = open(upload_fifo, O_RDONLY);
        if(u_mFd < 0) // Open a upload FIFO with non-blocking write only
        {
            LOGE("Open Upload FIFO Failed!\n");
            return NULL;
        }
    }

    while(nKillThread != KILL_THREAD_UPLOAD)
    {
        if((nRead = read(u_mFd, buff, MAX_CFIFO_LEN)) > 0)
        {
            LOGD("Read Upload message: %s\n", buff);
            frame_idx = atoi(buff);
            if((i420_stream_dim.width != upload_dim.width) && (i420_stream_dim.height != upload_dim.height)) {
              I420Scale(&stream_i420_buf[0], i420_stream_dim.width,
                        &stream_i420_buf[i420_stream_dim.width * i420_stream_dim.height], i420_stream_dim.width >> 1,
                        &stream_i420_buf[(i420_stream_dim.width * i420_stream_dim.height * 5) >> 2], i420_stream_dim.width >> 1,
                        i420_stream_dim.width, i420_stream_dim.height,
                        &upload_i420_buf[0], upload_dim.width,
                        &upload_i420_buf[upload_dim.width * upload_dim.height], upload_dim.width >> 1,
                        &upload_i420_buf[(upload_dim.width * upload_dim.height * 5) >> 2], upload_dim.width >> 1,
                        upload_dim.width, upload_dim.height,
                        0);
              I420ToNV12(&upload_i420_buf[0], upload_dim.width,
                         &upload_i420_buf[upload_dim.width * upload_dim.height], upload_dim.width >> 1,
                         &upload_i420_buf[(upload_dim.width* upload_dim.height* 5) >> 2], upload_dim.width >> 1,
                         &upload_nv12_buf[0], upload_dim.width,
                         &upload_nv12_buf[upload_dim.width * upload_dim.height], upload_dim.width,
                         upload_dim.width, upload_dim.height); //720P nv12
            } else {
              I420ToNV12(&stream_i420_buf[0], upload_dim.width,
                         &stream_i420_buf[upload_dim.width * upload_dim.height], upload_dim.width >> 1,
                         &stream_i420_buf[(upload_dim.width* upload_dim.height* 5) >> 2], upload_dim.width >> 1,
                         &upload_nv12_buf[0], upload_dim.width,
                         &upload_nv12_buf[upload_dim.width * upload_dim.height], upload_dim.width,
                         upload_dim.width, upload_dim.height); //720P nv12
            }
            if (s_pfnUploadCB) {
                LOGD("[DBG] user defined own Upload 720p cb [%d]. calling it...\n", frame_len);
                (*s_pfnUploadCB)((void *)upload_nv12_buf, frame_len, frame_idx);
            }
        }
        usleep(10000);
    }
    //close(u_mFd);  // 关闭FIFO文件
    LOGE("Thread Upload is Exit!\n");
    return NULL;
}

void *mm_app_thread_video(void * arg)
{
    int nRead;
    size_t frame_len;
    uint32_t frame_idx;
    char buff[MAX_CFIFO_LEN];

    cam_dimension_t video_dim = *(cam_dimension_t *)arg;
    LOGI("Video dimesion: %d x %d\n", video_dim.width, video_dim.height);

    frame_len = (video_dim.width*video_dim.height*3)/2;
    ///Create a video FIFO pipeline
    nRead = mkfifo(video_fifo, 0666);
    if(nRead < 0 && errno!=EEXIST)
    {
        LOGE("Create Video FIFO Failed!\n");
        return NULL;
    }

    if(v_mFd < 0) {
        v_mFd = open(video_fifo, O_RDONLY);
        if(v_mFd < 0) // Open an video FIFO with non-blocking write only
        {
            LOGE("Open Video FIFO Failed!\n");
            return NULL;
        }
    }

    while(nKillThread != KILL_THREAD_VIDEO)
    {
        if((nRead = read(v_mFd, buff, MAX_CFIFO_LEN)) > 0)
        {
            //LOGI("Read Video message: %s\n", buff);
            frame_idx = atoi(buff);
            //NV21ToNV12(frame->buffer,video_nv12_buf,i420_stream_dim.width,i420_stream_dim.height);
            if((i420_stream_dim.width != video_dim.width) && (i420_stream_dim.height != video_dim.height)) {
              I420Scale(&stream_i420_buf[0], i420_stream_dim.width,
                        &stream_i420_buf[i420_stream_dim.width * i420_stream_dim.height], i420_stream_dim.width >> 1,
                        &stream_i420_buf[(i420_stream_dim.width * i420_stream_dim.height * 5) >> 2], i420_stream_dim.width >> 1,
                        i420_stream_dim.width, i420_stream_dim.height,
                        &video_i420_buf[0], video_dim.width,
                        &video_i420_buf[video_dim.width * video_dim.height], video_dim.width >> 1,
                        &video_i420_buf[(video_dim.width * video_dim.height * 5) >> 2], video_dim.width >> 1,
                        video_dim.width, video_dim.height,
                        0);

              I420ToNV12(&video_i420_buf[0], video_dim.width,
                         &video_i420_buf[video_dim.width * video_dim.height], video_dim.width >> 1,
                         &video_i420_buf[(video_dim.width* video_dim.height* 5) >> 2], video_dim.width >> 1,
                         &video_nv12_buf[0], video_dim.width,
                         &video_nv12_buf[video_dim.width * video_dim.height], video_dim.width,
                         video_dim.width, video_dim.height); //1080P nv12
            } else {
              I420ToNV12(&stream_i420_buf[0], video_dim.width,
                         &stream_i420_buf[video_dim.width * video_dim.height], video_dim.width >> 1,
                         &stream_i420_buf[(video_dim.width* video_dim.height* 5) >> 2], video_dim.width >> 1,
                         &video_nv12_buf[0], video_dim.width,
                         &video_nv12_buf[video_dim.width * video_dim.height], video_dim.width,
                         video_dim.width, video_dim.height); //1080P nv12
            }
            if (s_pfnVideoCB) {
                //LOGI("[DBG] user defined own 1080p cb [%d]. calling it...\n", frame_len);
                //(*s_pfnFrameCB)((void *)video_nv12_buf, frame_len);
                (*s_pfnVideoCB)((void *)video_nv12_buf, frame_len, frame_idx);
            }
        }
        usleep(10000);
    }
    //close(v_mFd);  // 关闭FIFO文件
    LOGE("Thread Video is Exit!\n");
    return NULL;
}



void mm_app_display_pool_status(CThread_pool_t * pPool)
{
    static int nCount = 1;

    LOGE("****************************\n");
    LOGE("nCount = %d\n", nCount++);
    LOGE("max_thread_num = %d\n", pPool->GetThreadMaxNum((void *)pPool));
    LOGE("current_pthread_num = %d\n", pPool->GetCurrentThreadNum((void *)pPool));
    LOGE("current_pthread_task_num = %d\n", pPool->GetCurrentTaskThreadNum((void *)pPool));
    LOGE("current_wait_queue_num = %d\n", pPool->GetCurrentWaitTaskNum((void *)pPool));
    LOGE("****************************\n");
}

