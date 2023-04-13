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

// Camera dependencies
#include "mm_qcamera_app.h"
#include "mm_qcamera_dbg.h"

/* This callback is received once the complete JPEG encoding is done */
static void jpeg_encode_cb(jpeg_job_status_t status,
                           uint32_t client_hdl,
                           uint32_t jobId,
                           mm_jpeg_output_t *p_buf,
                           void *userData)
{
    uint32_t i = 0;
    mm_camera_test_obj_t *pme = NULL;
    LOGD(" BEGIN\n");

    pme = (mm_camera_test_obj_t *)userData;
    if (pme->jpeg_hdl != client_hdl ||
        jobId != pme->current_job_id ||
        !pme->current_job_frames) {
        LOGE(" NULL current job frames or not matching job ID (%d, %d)",
                    jobId, pme->current_job_id);
        return;
    }

    /* dump jpeg img */
    LOGE(" job %d, status=%d",  jobId, status);
    if (status == JPEG_JOB_STATUS_DONE && p_buf != NULL) {
		LOGE(" huangfusheng 001\n");
        mm_app_dump_jpeg_frame(p_buf->buf_vaddr, p_buf->buf_filled_len, "jpeg", "jpg", jobId);
    }

    /* buf done current encoding frames */
    pme->current_job_id = 0;
    for (i = 0; i < pme->current_job_frames->num_bufs; i++) {
        if (MM_CAMERA_OK != pme->cam->ops->qbuf(pme->current_job_frames->camera_handle,
                                                pme->current_job_frames->ch_id,
                                                pme->current_job_frames->bufs[i])) {
            LOGE(" Failed in Qbuf\n");
        }
        mm_app_cache_ops((mm_camera_app_meminfo_t *) pme->current_job_frames->bufs[i]->mem_info,
                         ION_IOC_INV_CACHES);
    }

    free(pme->jpeg_buf.buf.buffer);
    free(pme->current_job_frames);
    pme->current_job_frames = NULL;

    /* signal snapshot is done */
    mm_camera_app_done();
}

int encodeData(mm_camera_test_obj_t *test_obj, mm_camera_super_buf_t* recvd_frame,
               mm_camera_stream_t *m_stream)
{

    int rc = -MM_CAMERA_E_GENERAL;
    mm_jpeg_job_t job;

    /* remember current frames being encoded */
    test_obj->current_job_frames =
        (mm_camera_super_buf_t *)malloc(sizeof(mm_camera_super_buf_t));
    if (!test_obj->current_job_frames) {
        LOGE(" No memory for current_job_frames");
        return rc;
    }
    *(test_obj->current_job_frames) = *recvd_frame;

    memset(&job, 0, sizeof(job));
    job.job_type = JPEG_JOB_TYPE_ENCODE;
    job.encode_job.session_id = test_obj->current_jpeg_sess_id;

    // TODO: Rotation should be set according to
    //       sensor&device orientation
    job.encode_job.rotation = 0;

    /* fill in main src img encode param */
    job.encode_job.main_dim.src_dim = m_stream->s_config.stream_info->dim;
    job.encode_job.main_dim.dst_dim = m_stream->s_config.stream_info->dim;
    job.encode_job.src_index = 0;

    job.encode_job.thumb_dim.src_dim = m_stream->s_config.stream_info->dim;
    job.encode_job.thumb_dim.dst_dim.width = DEFAULT_PREVIEW_WIDTH;
    job.encode_job.thumb_dim.dst_dim.height = DEFAULT_PREVIEW_HEIGHT;

    /* fill in sink img param */
    job.encode_job.dst_index = 0;

    if (test_obj->metadata != NULL) {
        job.encode_job.p_metadata = test_obj->metadata;
    } else {
        LOGE(" Metadata null, not set for jpeg encoding");
    }

    rc = test_obj->jpeg_ops.start_job(&job, &test_obj->current_job_id);
    if ( 0 != rc ) {
        free(test_obj->current_job_frames);
        test_obj->current_job_frames = NULL;
    }

    return rc;
}

int createEncodingSession(mm_camera_test_obj_t *test_obj,
                          mm_camera_stream_t *m_stream,
                          mm_camera_buf_def_t *m_frame)
{
    mm_jpeg_encode_params_t encode_param;

    memset(&encode_param, 0, sizeof(mm_jpeg_encode_params_t));
    encode_param.jpeg_cb = jpeg_encode_cb;
    encode_param.userdata = (void*)test_obj;
    encode_param.encode_thumbnail = 0;
    encode_param.quality = 85;
    encode_param.color_format = MM_JPEG_COLOR_FORMAT_YCRCBLP_H2V2;
    encode_param.thumb_color_format = MM_JPEG_COLOR_FORMAT_YCRCBLP_H2V2;

    /* fill in main src img encode param */
    encode_param.num_src_bufs = 1;
    encode_param.src_main_buf[0].index = 0;
    encode_param.src_main_buf[0].buf_size = m_frame->frame_len;
    encode_param.src_main_buf[0].buf_vaddr = (uint8_t *)m_frame->buffer;
    encode_param.src_main_buf[0].fd = m_frame->fd;
    encode_param.src_main_buf[0].format = MM_JPEG_FMT_YUV;
    encode_param.src_main_buf[0].offset = m_stream->offset;

    /* fill in sink img param */
    encode_param.num_dst_bufs = 1;
    encode_param.dest_buf[0].index = 0;
    encode_param.dest_buf[0].buf_size = test_obj->jpeg_buf.buf.frame_len;
    encode_param.dest_buf[0].buf_vaddr = (uint8_t *)test_obj->jpeg_buf.buf.buffer;
    encode_param.dest_buf[0].fd = test_obj->jpeg_buf.buf.fd;
    encode_param.dest_buf[0].format = MM_JPEG_FMT_YUV;

    /* main dimension */
    encode_param.main_dim.src_dim = m_stream->s_config.stream_info->dim;
    encode_param.main_dim.dst_dim = m_stream->s_config.stream_info->dim;

    return test_obj->jpeg_ops.create_session(test_obj->jpeg_hdl,
                                             &encode_param,
                                             &test_obj->current_jpeg_sess_id);
}