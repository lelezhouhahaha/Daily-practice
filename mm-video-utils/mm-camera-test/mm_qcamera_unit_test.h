/* Copyright (c) 2012-2016, The Linux Foundation. All rights reserved.
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

#ifndef __MM_QCAMERA_UNIT_TEST_H__
#define __MM_QCAMERA_UNIT_TEST_H__

#include "mm_qcamera_app.h"

/*==========================================================================
 * Defines
 *===========================================================================*/
#define ZOOM_STEP             2
#define ZOOM_MIN_VALUE        0
#define MAX_CFIFO_LEN          256
int if_dropped_frame = 0;

const char *const snapshot_fifo = "/data/misc/camera/fifo_snapshot";
const char *const preview_fifo  = "/data/misc/camera/fifo_preview";
const char *const upload_fifo   = "/data/misc/camera/fifo_upload";
const char *const video_fifo    = "/data/misc/camera/fifo_video";

typedef enum {
  ZOOM_IN,
  ZOOM_OUT,
} Zoom_direction;

/**
 * Photo information structure.
 */
typedef struct {
  int width;
  int height;
  int photo_num;
  bool vidsnap_flag;
} snapshot_info_t ;


#ifdef __cplusplus
extern "C" {
#endif
typedef void (*PFN_FRAMEPOST_CB)(void *pFrameHeapBase, size_t frame_len, uint32_t frame_idx);
typedef void (*PFN_YUV2APP_CB)(void *pYuv2AppBuf, int yuv_w, int yuv_h);
int mm_app_RegisterFrameCallback(PFN_FRAMEPOST_CB pfnEventCB);
int mm_app_RegisterSnapShotCallback(PFN_FRAMEPOST_CB pfnEventCB);
int mm_app_RegisterPreviewCallback(PFN_FRAMEPOST_CB pfnEventCB);
int mm_app_RegisterUploadCallback(PFN_FRAMEPOST_CB pfnEventCB);
int mm_app_RegisterVideoCallback(PFN_FRAMEPOST_CB pfnEventCB);
int mm_app_RegisterYUV2AppCallback(PFN_YUV2APP_CB pYuv2AppCB);
void *mm_app_tc_start_snapshot(snapshot_info_t *snapshot);
void *mm_app_tc_start_preview(cam_dimension_t *dim);
void *mm_app_tc_start_upload(cam_dimension_t *dim);
void *mm_app_tc_start_video(cam_dimension_t *dim);
int mm_app_tc_stop_snapshot();
int mm_app_tc_stop_preview();
int mm_app_tc_stop_upload();
int mm_app_tc_stop_video();
int mm_app_tc_start_stream(cam_dimension_t *dim);
int mm_app_tc_stop_stream();
void mm_app_tc_start_yuv2app(int start_yuv);
void mm_app_tc_stop_yuv2app(int stop_yuv);
int mm_app_set_zoom(int zoom_target);
int mm_app_set_WNR(uint8_t enable);
int mm_app_set_effect(int effect);
void mm_app_set_asf(int enable_wnr,int sharpness);
int mm_app_tc_open(int cameraID);
int mm_app_tc_close();
int mm_app_init();
#ifdef __cplusplus
}
#endif

#endif /* __MM_QCAMERA_UNIT_TEST_H__ */
