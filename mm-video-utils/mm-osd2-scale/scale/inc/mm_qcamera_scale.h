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

#ifndef __MM_QCAMERA_SCALE_H__
#define __MM_QCAMERA_SCALE_H__

#ifdef __cplusplus
extern "C" {
#endif

//int mm_scale_init();                          // used for thread

/*********************************************************************
 * scale_for_snapshot --> find snapshot scaled resolution
 * scale_for_liveshot --> find liveshot resolution
 * mm_scale_start
 * photo_scale_info   --> prepared for JPEG coding
 *********************************************************************/
 typedef struct {
    void *buffer;
    char *file_name;
    char notify;
    uint16_t width;
    uint16_t height;
    uint16_t brust_number;
} photo_scale_info;
int scale_for_snapshot(int width, int height, int multiple, int burst_number);
int scale_for_liveshot(int video_resolution);
int mm_scale_start(void *pFrameBuffer, int width, int height, int photo_start_num,
                                    const char *photo_save_path,
                                    const char *photo_serial_num,
                                    const char *photo_time_stamp,
                                    bool vidsnap_flag);


/*********************************************************************
 * photo_size_info used for transfer back photo info
 *
 * liveshot_init/liveshot_deinit used for liveshot initialize
 *********************************************************************/
 typedef struct {
    uint8_t *buffer_ptr;
    uint16_t width;
    uint16_t height;
} photo_size_info;
photo_size_info *liveshot_init(uint8_t *liveshot_cb_buffer, int snapshot_buf_width, int snapshot_buf_height);
int liveshot_deinit(uint8_t *scaled_liveshot_buf);

//void scale_deinit();

#ifdef __cplusplus
}
#endif

#endif /* __MM_QCAMERA_SCALE_H__ */


