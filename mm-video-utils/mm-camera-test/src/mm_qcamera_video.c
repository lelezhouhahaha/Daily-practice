/*
Copyright (c) 2012-2014, 2016, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

// Camera dependencies
#include "mm_qcamera_app.h"
#include "mm_qcamera_dbg.h"

extern cam_dimension_t i420_stream_dim;

static void mm_app_metadata_notify_cb(mm_camera_super_buf_t *bufs,
                                     void *user_data)
{
  uint32_t i = 0;
  mm_camera_channel_t *channel = NULL;
  mm_camera_stream_t *p_stream = NULL;
  mm_camera_test_obj_t *pme = (mm_camera_test_obj_t *)user_data;
  mm_camera_buf_def_t *frame;
  metadata_buffer_t *pMetadata;

  if (NULL == bufs || NULL == user_data) {
      LOGE("bufs or user_data are not valid ");
      return;
  }
  frame = bufs->bufs[0];

  /* find channel */
  for (i = 0; i < MM_CHANNEL_TYPE_MAX; i++) {
      if (pme->channels[i].ch_id == bufs->ch_id) {
          channel = &pme->channels[i];
          break;
      }
  }

  if (NULL == channel) {
      LOGE("Channel object is NULL ");
      return;
  }

  /* find preview stream */
  for (i = 0; i < channel->num_streams; i++) {
      if (channel->streams[i].s_config.stream_info->stream_type == CAM_STREAM_TYPE_METADATA) {
          p_stream = &channel->streams[i];
          break;
      }
  }

  if (NULL == p_stream) {
      LOGE("cannot find metadata stream");
      return;
  }

  /* find preview frame */
  for (i = 0; i < bufs->num_bufs; i++) {
      if (bufs->bufs[i]->stream_id == p_stream->s_id) {
          frame = bufs->bufs[i];
          break;
      }
  }
#if 0
  if (pme->metadata == NULL) {
    /* The app will free the meta data, we don't need to bother here */
    pme->metadata = malloc(sizeof(metadata_buffer_t));
    if (NULL == pme->metadata) {
        LOGE("Canot allocate metadata memory\n");
        return;
    }
  }
  memcpy(pme->metadata, frame->buffer, sizeof(metadata_buffer_t));
#endif
  pMetadata = (metadata_buffer_t *)frame->buffer;
  IF_META_AVAILABLE(uint32_t, afState, CAM_INTF_META_AF_STATE, pMetadata) {
    if ((cam_af_state_t)(*afState) == CAM_AF_STATE_FOCUSED_LOCKED ||
            (cam_af_state_t)(*afState) == CAM_AF_STATE_NOT_FOCUSED_LOCKED) {
        LOGE("AutoFocus Done Call Back Received\n");
        mm_camera_app_done();
    } else if ((cam_af_state_t)(*afState) == CAM_AF_STATE_NOT_FOCUSED_LOCKED) {
        LOGE("AutoFocus failed\n");
        mm_camera_app_done();
    }
  }

  if (pme->user_metadata_cb) {
      LOGD("[DBG] %s, user defined own metadata cb. calling it...");
      pme->user_metadata_cb(frame);
  }

  if (MM_CAMERA_OK != pme->cam->ops->qbuf(bufs->camera_handle,
                                          bufs->ch_id,
                                          frame)) {
      LOGE("Failed in Preview Qbuf\n");
  }
  mm_app_cache_ops((mm_camera_app_meminfo_t *)frame->mem_info,
                   ION_IOC_INV_CACHES);
}


static void mm_app_snapshot_notify_cb(mm_camera_super_buf_t *bufs,
                                      void *user_data)
{

    int rc = 0;
    uint32_t i = 0;
    mm_camera_test_obj_t *pme = (mm_camera_test_obj_t *)user_data;
    mm_camera_channel_t *channel = NULL;
    mm_camera_stream_t *p_stream = NULL;
    mm_camera_stream_t *m_stream = NULL;
    mm_camera_buf_def_t *p_frame = NULL;
    mm_camera_buf_def_t *m_frame = NULL;
    mm_camera_channel_t *p_channel = NULL;
    mm_camera_stream_t *md_stream = NULL;
    mm_camera_buf_def_t *md_frame = NULL;

    /* find channel */
    for (i = 0; i < MM_CHANNEL_TYPE_MAX; i++) {
        if (pme->channels[i].ch_id == bufs->ch_id) {
            channel = &pme->channels[i];
            break;
        }
    }
    if (NULL == channel) {
        LOGE("Wrong channel id (%d)",  bufs->ch_id);
        rc = -1;
        goto error;
    }

    /* find snapshot stream */
    for (i = 0; i < channel->num_streams; i++) {
        if (channel->streams[i].s_config.stream_info->stream_type == CAM_STREAM_TYPE_SNAPSHOT) {
            m_stream = &channel->streams[i];
            break;
        }
    }
    if (NULL == m_stream) {
        LOGE("cannot find snapshot stream");
        rc = -1;
        goto error;
    }

    /* find snapshot frame */
    for (i = 0; i < bufs->num_bufs; i++) {
        if (bufs->bufs[i]->stream_id == m_stream->s_id) {
            m_frame = bufs->bufs[i];
            break;
        }
    }
    if (NULL == m_frame) {
        LOGE("main frame is NULL");
        rc = -1;
        goto error;
    }

    //mm_app_dump_frame(m_frame, "main", "yuv", m_frame->frame_idx);

    /* add huangfusheng 2019-01-06 ---> cb stream */
    /*
    if (pme->user_snapshot_cb) {
        LOGI("[DBG] %s, user defined own storage video cb. calling it...",__func__);
        pme->user_snapshot_cb(m_frame);
    }
    */
    /* find postview stream */
    for (i = 0; i < channel->num_streams; i++) {
        if (channel->streams[i].s_config.stream_info->stream_type == CAM_STREAM_TYPE_POSTVIEW) {
            p_stream = &channel->streams[i];
            break;
        }
    }
    if (NULL != p_stream) {
        /* find preview frame */
        for (i = 0; i < bufs->num_bufs; i++) {
            if (bufs->bufs[i]->stream_id == p_stream->s_id) {
                p_frame = bufs->bufs[i];
                break;
            }
        }
        if (NULL != p_frame) {
            mm_app_dump_frame(p_frame, "postview", "yuv", p_frame->frame_idx);
        }
    }

    p_channel = mm_app_get_channel_by_type(pme, MM_CHANNEL_TYPE_PREVIEW);
    for (i = 0; i < p_channel->num_streams; i++) {
        LOGI("p_channel->streams[%d].s_config.stream_info->stream_type:%d, s_id:%d", i, p_channel->streams[i].s_config.stream_info->stream_type, p_channel->streams[i].s_id);
        if (p_channel->streams[i].s_config.stream_info->stream_type == CAM_STREAM_TYPE_METADATA) {
            md_stream = &p_channel->streams[i];
            LOGE("get md s_id:%d", md_stream->s_id);
        }
    }
    if(NULL != md_stream) {
      /* find metadata frame */
      for (i = 0; i < bufs->num_bufs; i++) {
          LOGI("bufs->bufs[%d]->stream_id:%d, md_stream->s_id:%d", i, bufs->bufs[i]->stream_id, md_stream->s_id);
          if (bufs->bufs[i]->stream_id == md_stream->s_id) {
              md_frame = bufs->bufs[i];
              break;
          }
      }
      if (!md_frame) {
          LOGE("md_frame is null\n");
      }
    } else {
        LOGE("md stream is NULL");
    }

    if (NULL != md_frame) {
        rc = mm_app_do_reprocess(pme,
                m_frame,
                md_frame->buf_idx,
                bufs,
                md_stream);

        if (MM_CAMERA_OK != rc ) {
            LOGE("reprocess failed rc = %d",  rc);
        }
    } else {
        LOGE("md_frame is null\n");
    }

    mm_app_cache_ops((mm_camera_app_meminfo_t *)m_frame->mem_info,
                     ION_IOC_CLEAN_INV_CACHES);
#ifdef ENABLE_JPEG_ENDODE
    pme->jpeg_buf.buf.buffer = (uint8_t *)malloc(m_frame->frame_len);
    if ( NULL == pme->jpeg_buf.buf.buffer ) {
        LOGE("error allocating jpeg output buffer");
        goto error;
    }

    pme->jpeg_buf.buf.frame_len = m_frame->frame_len;
    /* create a new jpeg encoding session */
    rc = createEncodingSession(pme, m_stream, m_frame);
    if (0 != rc) {
        LOGE("error creating jpeg session");
        free(pme->jpeg_buf.buf.buffer);
        goto error;
    }

    /* start jpeg encoding job */
    rc = encodeData(pme, bufs, m_stream);
    if (0 != rc) {
        LOGE("error creating jpeg session");
        free(pme->jpeg_buf.buf.buffer);
        goto error;
    }
#endif
error:
    /* buf done rcvd frames in error case */
    if ( 0 != rc ) {
        for (i=0; i<bufs->num_bufs; i++) {
            if (MM_CAMERA_OK != pme->cam->ops->qbuf(bufs->camera_handle,
                                                    bufs->ch_id,
                                                    bufs->bufs[i])) {
                LOGE("Failed in Qbuf\n");
            }
            mm_app_cache_ops((mm_camera_app_meminfo_t *)bufs->bufs[i]->mem_info,
                             ION_IOC_INV_CACHES);
        }
    }

    LOGD(" END\n");
}

static void mm_app_preview_notify_cb(mm_camera_super_buf_t *bufs,
                                     void *user_data)
{
    uint32_t i = 0;
    mm_camera_channel_t *channel = NULL;
    mm_camera_stream_t *p_stream = NULL;
    mm_camera_buf_def_t *frame = NULL;
    mm_camera_test_obj_t *pme = (mm_camera_test_obj_t *)user_data;

    if (NULL == bufs || NULL == user_data) {
        LOGE("bufs or user_data are not valid ");
        return;
    }

    frame = bufs->bufs[0];

    /* find channel */
    for (i = 0; i < MM_CHANNEL_TYPE_MAX; i++) {
        if (pme->channels[i].ch_id == bufs->ch_id) {
            channel = &pme->channels[i];
            break;
        }
    }
    if (NULL == channel) {
        LOGE("Channel object is NULL ");
        return;
    }
    /* find preview stream */
    for (i = 0; i < channel->num_streams; i++) {
        if (channel->streams[i].s_config.stream_info->stream_type == CAM_STREAM_TYPE_PREVIEW) {
            p_stream = &channel->streams[i];
            break;
        }
    }

    if (NULL == p_stream) {
        LOGE("cannot find preview stream");
        return;
    }

    /* find preview frame */
    for (i = 0; i < bufs->num_bufs; i++) {
        if (bufs->bufs[i]->stream_id == p_stream->s_id) {
            frame = bufs->bufs[i];
            break;
        }
    }

    if ( 0 < pme->fb_fd ) {
        //mm_app_overlay_display(pme, frame->fd);
    }
#ifdef DUMP_PRV_IN_FILE
    {
        char file_name[64];
        snprintf(file_name, sizeof(file_name), "P_C%d", pme->cam->camera_handle);
        mm_app_dump_frame(frame, file_name, "yuv", frame->frame_idx);
    }
#endif
/*
    if (pme->user_preview_cb) {
        LOGE("[DBG] %s, user defined own preview cb. calling it...",__func__);
        pme->user_preview_cb(frame);
    }*/
    /* add huangfusheng 2019-01-06 ---> cb stream */
    if (pme->user_storage_video_cb) {
        LOGD("[DBG] %s, user defined own storage video cb. calling it...",__func__);
        pme->user_storage_video_cb(frame);
    }

    if (MM_CAMERA_OK != pme->cam->ops->qbuf(bufs->camera_handle,
                bufs->ch_id,
                frame)) {
        LOGE("Failed in Preview Qbuf\n");
    }
    mm_app_cache_ops((mm_camera_app_meminfo_t *)frame->mem_info,
            ION_IOC_INV_CACHES);

    LOGD(" END\n");
}


static void mm_app_video_notify_cb(mm_camera_super_buf_t *bufs,
                                   void *user_data)
{
    char file_name[64];
    mm_camera_buf_def_t *frame = bufs->bufs[0];
    mm_camera_test_obj_t *pme = (mm_camera_test_obj_t *)user_data;

    LOGD("BEGIN - length=%zu, frame idx = %d\n",
          frame->frame_len, frame->frame_idx);
#ifdef DUMP_PRV_IN_FILE
    snprintf(file_name, sizeof(file_name), "V_C%d", pme->cam->camera_handle);
    mm_app_dump_frame(frame, file_name, "yuv", frame->frame_idx);
#endif

    if (MM_CAMERA_OK != pme->cam->ops->qbuf(bufs->camera_handle,
                                            bufs->ch_id,
                                            frame)) {
        LOGE("Failed in Preview Qbuf\n");
    }
    mm_app_cache_ops((mm_camera_app_meminfo_t *)frame->mem_info,
                     ION_IOC_INV_CACHES);

    LOGD("END\n");
}

mm_camera_stream_t * mm_app_add_metadata_stream(mm_camera_test_obj_t *test_obj,
                                               mm_camera_channel_t *channel,
                                               mm_camera_buf_notify_t stream_cb,
                                               void *userdata,
                                               uint8_t num_bufs)
{
    int rc = MM_CAMERA_OK;
    mm_camera_stream_t *stream = NULL;
    cam_capability_t *cam_cap = (cam_capability_t *)(test_obj->cap_buf.buf.buffer);
    stream = mm_app_add_stream(test_obj, channel);
    if (NULL == stream) {
        printf("add stream failed\n");
        return NULL;
    }

    stream->s_config.mem_vtbl.get_bufs = mm_app_stream_initbuf;
    stream->s_config.mem_vtbl.put_bufs = mm_app_stream_deinitbuf;
    stream->s_config.mem_vtbl.clean_invalidate_buf =
      mm_app_stream_clean_invalidate_buf;
    stream->s_config.mem_vtbl.invalidate_buf = mm_app_stream_invalidate_buf;
    stream->s_config.mem_vtbl.user_data = (void *)stream;
    stream->s_config.stream_cb = stream_cb;
    stream->s_config.stream_cb_sync = NULL;
    stream->s_config.userdata = userdata;
    stream->num_of_bufs = num_bufs;

    stream->s_config.stream_info = (cam_stream_info_t *)stream->s_info_buf.buf.buffer;
    memset(stream->s_config.stream_info, 0, sizeof(cam_stream_info_t));
    stream->s_config.stream_info->stream_type = CAM_STREAM_TYPE_METADATA;
    stream->s_config.stream_info->streaming_mode = CAM_STREAMING_MODE_CONTINUOUS;
    stream->s_config.stream_info->fmt = DEFAULT_PREVIEW_FORMAT;
    stream->s_config.stream_info->dim.width = sizeof(metadata_buffer_t);
    stream->s_config.stream_info->dim.height = 1;
    stream->s_config.padding_info = cam_cap->padding_info;

    rc = mm_app_config_stream(test_obj, channel, stream, &stream->s_config);
    if (MM_CAMERA_OK != rc) {
        printf("config preview stream err=%d\n",  rc);
        return NULL;
    }

    return stream;
}

mm_camera_stream_t * mm_app_add_video_preview_stream(mm_camera_test_obj_t *test_obj,
                                               mm_camera_channel_t *channel,
                                               mm_camera_buf_notify_t stream_cb,
                                               void *userdata,
                                               uint8_t num_bufs)
{
    int rc = MM_CAMERA_OK;
    mm_camera_stream_t *stream = NULL;
    cam_capability_t *cam_cap = (cam_capability_t *)(test_obj->cap_buf.buf.buffer);
    cam_dimension_t preview_dim = {0, 0};

    if ((test_obj->preview_resolution.user_input_display_width == 0) ||
           ( test_obj->preview_resolution.user_input_display_height == 0)) {
        preview_dim.width = i420_stream_dim.width;
        preview_dim.height = i420_stream_dim.height;
    } else {
        preview_dim.width = test_obj->preview_resolution.user_input_display_width;
        preview_dim.height = test_obj->preview_resolution.user_input_display_height;
    }
    LOGI("preview dimesion: %d x %d\n",  preview_dim.width, preview_dim.height);

    stream = mm_app_add_stream(test_obj, channel);
    if (NULL == stream) {
        LOGE("add stream failed\n");
        return NULL;
    }
    stream->s_config.mem_vtbl.get_bufs = mm_app_stream_initbuf;
    stream->s_config.mem_vtbl.put_bufs = mm_app_stream_deinitbuf;
    stream->s_config.mem_vtbl.clean_invalidate_buf =
            mm_app_stream_clean_invalidate_buf;
    stream->s_config.mem_vtbl.invalidate_buf = mm_app_stream_invalidate_buf;
    stream->s_config.mem_vtbl.user_data = (void *)stream;
    stream->s_config.stream_cb = stream_cb;
    stream->s_config.stream_cb_sync = NULL;
    stream->s_config.userdata = userdata;
    stream->num_of_bufs = num_bufs;

    stream->s_config.stream_info = (cam_stream_info_t *)stream->s_info_buf.buf.buffer;
    memset(stream->s_config.stream_info, 0, sizeof(cam_stream_info_t));
    stream->s_config.stream_info->stream_type = CAM_STREAM_TYPE_PREVIEW;
    stream->s_config.stream_info->streaming_mode = CAM_STREAMING_MODE_CONTINUOUS;
    stream->s_config.stream_info->fmt = DEFAULT_PREVIEW_FORMAT;

    stream->s_config.stream_info->dim.width = preview_dim.width;
    stream->s_config.stream_info->dim.height = preview_dim.height;

    stream->s_config.padding_info = cam_cap->padding_info;

    stream->s_config.stream_info->pp_config.feature_mask |= CAM_QTI_FEATURE_SW_TNR;
    LOGI("preview stream pp mask:%lld (%x)\n", stream->s_config.stream_info->pp_config.feature_mask, stream->s_config.stream_info->pp_config.feature_mask);

    rc = mm_app_config_stream(test_obj, channel, stream, &stream->s_config);
    if (MM_CAMERA_OK != rc) {
        LOGE("config preview stream err=%d\n",  rc);
        return NULL;
    }

    return stream;
}


mm_camera_stream_t * mm_app_add_video_snapshot_stream(mm_camera_test_obj_t *test_obj,
                                                mm_camera_channel_t *channel,
                                                mm_camera_buf_notify_t stream_cb,
                                                void *userdata,
                                                uint8_t num_bufs,
                                                uint8_t num_burst)
{
    int rc = MM_CAMERA_OK;
    mm_camera_stream_t *stream = NULL;
    cam_capability_t *cam_cap = (cam_capability_t *)(test_obj->cap_buf.buf.buffer);

    stream = mm_app_add_stream(test_obj, channel);
    if (NULL == stream) {
        LOGE("add stream failed\n");
        return NULL;
    }

    stream->s_config.mem_vtbl.get_bufs = mm_app_stream_initbuf;
    stream->s_config.mem_vtbl.put_bufs = mm_app_stream_deinitbuf;
    stream->s_config.mem_vtbl.clean_invalidate_buf =
      mm_app_stream_clean_invalidate_buf;
    stream->s_config.mem_vtbl.invalidate_buf = mm_app_stream_invalidate_buf;
    stream->s_config.mem_vtbl.user_data = (void *)stream;
    stream->s_config.stream_cb = stream_cb;
    stream->s_config.stream_cb_sync = NULL;
    stream->s_config.userdata = userdata;
    stream->num_of_bufs = num_bufs;

    stream->s_config.stream_info = (cam_stream_info_t *)stream->s_info_buf.buf.buffer;
    memset(stream->s_config.stream_info, 0, sizeof(cam_stream_info_t));
    stream->s_config.stream_info->stream_type = CAM_STREAM_TYPE_SNAPSHOT;
    if (num_burst == 0) {
        stream->s_config.stream_info->streaming_mode = CAM_STREAMING_MODE_CONTINUOUS;
    } else {
        stream->s_config.stream_info->streaming_mode = CAM_STREAMING_MODE_BURST;
        stream->s_config.stream_info->num_of_burst = num_burst;
    }
    stream->s_config.stream_info->fmt = DEFAULT_SNAPSHOT_FORMAT;
    if ( test_obj->buffer_width == 0 || test_obj->buffer_height == 0 ) {
        stream->s_config.stream_info->dim.width = DEFAULT_SNAPSHOT_WIDTH;
        stream->s_config.stream_info->dim.height = DEFAULT_SNAPSHOT_HEIGHT;
    } else {
        stream->s_config.stream_info->dim.width = DEFAULT_SNAPSHOT_WIDTH;
        stream->s_config.stream_info->dim.height = DEFAULT_SNAPSHOT_HEIGHT;
    }
    stream->s_config.padding_info = cam_cap->padding_info;
    /* Make offset as zero as CPP will not be used  */
    stream->s_config.padding_info.offset_info.offset_x = 0;
    stream->s_config.padding_info.offset_info.offset_y = 0;

    rc = mm_app_config_stream(test_obj, channel, stream, &stream->s_config);
    if (MM_CAMERA_OK != rc) {
        LOGE("config preview stream err=%d\n",  rc);
        return NULL;
    }

    return stream;

}


mm_camera_stream_t * mm_app_add_video_stream(mm_camera_test_obj_t *test_obj,
                                             mm_camera_channel_t *channel,
                                             mm_camera_buf_notify_t stream_cb,
                                             void *userdata,
                                             uint8_t num_bufs)
{
    int rc = MM_CAMERA_OK;
    mm_camera_stream_t *stream = NULL;
    cam_capability_t *cam_cap = (cam_capability_t *)(test_obj->cap_buf.buf.buffer);
/*
    uint32_t analysis_pp_mask = cam_cap->qcom_supported_feature_mask &
                                        (CAM_QCOM_FEATURE_SHARPNESS |
                                         CAM_QCOM_FEATURE_EFFECT |
                                         CAM_QCOM_FEATURE_DENOISE2D);
*/
    uint32_t analysis_pp_mask = cam_cap->qcom_supported_feature_mask & (CAM_QCOM_FEATURE_PP_SUPERSET | CAM_QTI_FEATURE_SW_TNR);
    LOGI("analysis stream pp mask:%x\n",  analysis_pp_mask);

    cam_stream_size_info_t abc_snap ;
    memset (&abc_snap , 0, sizeof (cam_stream_size_info_t));

    abc_snap.num_streams = 3;
    abc_snap.postprocess_mask[2] = analysis_pp_mask;
    abc_snap.stream_sizes[2].width = i420_stream_dim.width;
    abc_snap.stream_sizes[2].height = i420_stream_dim.height;
    abc_snap.type[2] = CAM_STREAM_TYPE_PREVIEW;

    abc_snap.postprocess_mask[1] = analysis_pp_mask;
    abc_snap.stream_sizes[1].width = i420_stream_dim.width;
    abc_snap.stream_sizes[1].height = i420_stream_dim.height;
    abc_snap.type[1] = CAM_STREAM_TYPE_VIDEO;

    abc_snap.postprocess_mask[0] = 0;
    abc_snap.stream_sizes[0].width = DEFAULT_SNAPSHOT_WIDTH;
    abc_snap.stream_sizes[0].height = DEFAULT_SNAPSHOT_HEIGHT;
    abc_snap.type[0] = CAM_STREAM_TYPE_SNAPSHOT;

    abc_snap.buffer_info.min_buffers = 7;
    abc_snap.buffer_info.max_buffers = 7;
    abc_snap.is_type[0] = IS_TYPE_NONE;

    rc = setmetainfoCommand(test_obj, &abc_snap);
    if (rc != MM_CAMERA_OK) {
       LOGE("meta info command snapshot failed\n");
    }

    stream = mm_app_add_stream(test_obj, channel);
    if (NULL == stream) {
        LOGE("add stream failed\n");
        return NULL;
    }

    stream->s_config.mem_vtbl.get_bufs = mm_app_stream_initbuf;
    stream->s_config.mem_vtbl.put_bufs = mm_app_stream_deinitbuf;
    stream->s_config.mem_vtbl.clean_invalidate_buf =
            mm_app_stream_clean_invalidate_buf;
    stream->s_config.mem_vtbl.invalidate_buf = mm_app_stream_invalidate_buf;
    stream->s_config.mem_vtbl.user_data = (void *)stream;
    stream->s_config.stream_cb = stream_cb;
    stream->s_config.stream_cb_sync = NULL;
    stream->s_config.userdata = userdata;
    stream->num_of_bufs = num_bufs;

    stream->s_config.stream_info = (cam_stream_info_t *)stream->s_info_buf.buf.buffer;
    memset(stream->s_config.stream_info, 0, sizeof(cam_stream_info_t));
    stream->s_config.stream_info->stream_type = CAM_STREAM_TYPE_VIDEO;
    stream->s_config.stream_info->streaming_mode = CAM_STREAMING_MODE_CONTINUOUS;
    stream->s_config.stream_info->fmt = DEFAULT_VIDEO_FORMAT;
    stream->s_config.stream_info->dim.width = i420_stream_dim.width;
    stream->s_config.stream_info->dim.height = i420_stream_dim.height;
    stream->s_config.padding_info = cam_cap->padding_info;

    stream->s_config.stream_info->pp_config.feature_mask |= CAM_QTI_FEATURE_SW_TNR;
    LOGI("preview stream pp mask:%lld (%x)\n", stream->s_config.stream_info->pp_config.feature_mask, stream->s_config.stream_info->pp_config.feature_mask);

    rc = mm_app_config_stream(test_obj, channel, stream, &stream->s_config);
    if (MM_CAMERA_OK != rc) {
        LOGE("config preview stream err=%d\n",  rc);
        return NULL;
    }

    return stream;
}

mm_camera_channel_t * mm_app_add_video_channel(mm_camera_test_obj_t *test_obj)
{
    mm_camera_channel_t *channel = NULL;
    mm_camera_stream_t *stream = NULL;

    channel = mm_app_add_channel(test_obj,
                                 MM_CHANNEL_TYPE_VIDEO,
                                 NULL,
                                 NULL,
                                 NULL);
    if (NULL == channel) {
        LOGE("add channel failed");
        return NULL;
    }

    stream = mm_app_add_video_stream(test_obj,
                                     channel,
                                     mm_app_video_notify_cb,
                                     (void *)test_obj,
                                     1);
    if (NULL == stream) {
        LOGE("add video stream failed\n");
        mm_app_del_channel(test_obj, channel);
        return NULL;
    }

    return channel;
}


int mm_app_del_stream_channel(mm_camera_test_obj_t *test_obj,
                                mm_camera_channel_t *channel)
{
    int rc = MM_CAMERA_OK;
    mm_camera_stream_t *stream = NULL;
    uint8_t i;
    cam_stream_size_info_t abc ;
    memset (&abc , 0, sizeof (cam_stream_size_info_t));

    if (channel->num_streams <= MAX_STREAM_NUM_IN_BUNDLE) {
        for (i = 0; i < channel->num_streams; i++) {
            stream = &channel->streams[i];
            rc = mm_app_del_stream(test_obj, channel, stream);
            if (MM_CAMERA_OK != rc) {
                printf("del stream(%d) failed rc=%d\n",  i, rc);
            }
        }
    } else {
        printf("num_streams = %d. Should not be more than %d\n",
             channel->num_streams, MAX_STREAM_NUM_IN_BUNDLE);
    }

    rc = setmetainfoCommand(test_obj, &abc);
    if (rc != MM_CAMERA_OK) {
       printf("meta info command failed\n");
    }

    rc = mm_app_del_channel(test_obj, channel);
    if (MM_CAMERA_OK != rc) {
        printf("delete channel failed rc=%d\n",  rc);
    }

    return rc;
}



int mm_app_stop_and_del_channel(mm_camera_test_obj_t *test_obj,
                                mm_camera_channel_t *channel)
{
    int rc = MM_CAMERA_OK;
    mm_camera_stream_t *stream = NULL;
    uint8_t i;
    cam_stream_size_info_t abc ;
    memset (&abc , 0, sizeof (cam_stream_size_info_t));

    rc = mm_app_stop_channel(test_obj, channel);
    if (MM_CAMERA_OK != rc) {
        printf("Stop Preview failed rc=%d\n",  rc);
    }

    if (channel->num_streams <= MAX_STREAM_NUM_IN_BUNDLE) {
        for (i = 0; i < channel->num_streams; i++) {
            stream = &channel->streams[i];
            rc = mm_app_del_stream(test_obj, channel, stream);
            if (MM_CAMERA_OK != rc) {
                printf("del stream(%d) failed rc=%d\n",  i, rc);
            }
        }
    } else {
        printf("num_streams = %d. Should not be more than %d\n",
             channel->num_streams, MAX_STREAM_NUM_IN_BUNDLE);
    }

    rc = setmetainfoCommand(test_obj, &abc);
    if (rc != MM_CAMERA_OK) {
       printf("meta info command failed\n");
    }

    rc = mm_app_del_channel(test_obj, channel);
    if (MM_CAMERA_OK != rc) {
        printf("delete channel failed rc=%d\n",  rc);
    }

    return rc;
}

int mm_app_start_record_preview(mm_camera_test_obj_t *test_obj)
{
    int rc = MM_CAMERA_OK;
    mm_camera_channel_t *p_ch = NULL;
    mm_camera_channel_t *v_ch = NULL;
    mm_camera_channel_t *s_ch = NULL;
    mm_camera_stream_t *s_preview = NULL;
    mm_camera_stream_t *s_metadata = NULL;
    mm_camera_stream_t *s_main = NULL;
    mm_camera_stream_t *s_video = NULL;
    mm_camera_channel_attr_t attr;
    memset(&attr, 0, sizeof(mm_camera_channel_attr_t));
    attr.notify_mode = MM_CAMERA_SUPER_BUF_NOTIFY_CONTINUOUS;
    attr.look_back = 2;
    attr.post_frame_skip = 0;
    attr.water_mark = 2;
    attr.max_unmatched_frames = 3;


    /* Create Video Channel */
    v_ch = mm_app_add_channel(test_obj,
                              MM_CHANNEL_TYPE_VIDEO,
                              NULL,
                              NULL,
                              NULL);
    if (NULL == v_ch) {
        LOGE("add channel failed");
        return -MM_CAMERA_E_GENERAL;
    }

   /* Add Video Stream */
    s_video = mm_app_add_video_stream(test_obj,
                                      v_ch,
                                      mm_app_video_notify_cb,
                                      (void*)test_obj,
                                      VIDEO_BUF_NUM);

    if (NULL == s_video) {
        LOGE("add video stream failed\n");
        mm_app_del_channel(test_obj, v_ch);
        return rc;
    }

    /* Create Preview Channel */
    p_ch = mm_app_add_channel(test_obj,
                              MM_CHANNEL_TYPE_PREVIEW,
                              NULL,
                              NULL,
                              NULL);
    /* Add Preview stream to Channel */
    if (NULL == p_ch) {
        LOGE("add channel failed");
        return -MM_CAMERA_E_GENERAL;
    }


    s_preview = mm_app_add_video_preview_stream(test_obj,
                                             p_ch,
                                             mm_app_preview_notify_cb,
                                             (void *)test_obj,
                                             PREVIEW_BUF_NUM);
    if (NULL == s_preview) {
        LOGE("add preview stream failed\n");
        mm_app_del_channel(test_obj, p_ch);
        return rc;
    }
    /* Create Snapshot Channel */
    s_ch = mm_app_add_channel(test_obj,
                              MM_CHANNEL_TYPE_SNAPSHOT,
                              &attr,
                              mm_app_snapshot_notify_cb,
                              test_obj);
    if (NULL == s_ch) {
        LOGE("add channel failed");
        return -MM_CAMERA_E_GENERAL;
    }

    /* Add Snapshot Stream */
    s_main = mm_app_add_video_snapshot_stream(test_obj,
                                           s_ch,
                                           NULL,
                                           (void *)test_obj,
                                           1,
                                           1);
    if (NULL == s_main) {
        LOGE("add main snapshot stream failed\n");
        mm_app_del_channel(test_obj, s_ch);
        return rc;
    }


    /* Add Metadata Stream to preview channel */
    s_metadata = mm_app_add_metadata_stream(test_obj,
                                            p_ch,
                                            mm_app_metadata_notify_cb,
                                            (void *)test_obj,
                                            PREVIEW_BUF_NUM);

    if (NULL == s_metadata) {
        LOGE("add metadata stream failed\n");
        mm_app_del_channel(test_obj, p_ch);
        return rc;
    }
    if ( NULL == mm_app_add_reprocess_channel(test_obj, s_main) ) {
        LOGE("Reprocess channel failed to initialize \n");
        mm_app_del_stream(test_obj, v_ch, s_preview);
        mm_app_del_stream(test_obj, p_ch, s_metadata);
        mm_app_del_stream(test_obj, s_ch, s_main);
        mm_app_del_channel(test_obj, v_ch);
        mm_app_del_channel(test_obj, p_ch);
        mm_app_del_channel(test_obj, s_ch);
        return rc;
    }

    /* Start Preview Channel */
    rc = mm_app_start_channel(test_obj, p_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("start preview failed rc=%d\n", rc);
        mm_app_del_channel(test_obj, p_ch);
        mm_app_del_channel(test_obj, v_ch);
        mm_app_del_channel(test_obj, s_ch);
        return rc;
    }
#if 0
    /* Start Video Channel */
    rc = mm_app_start_channel(test_obj, v_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("start preview failed rc=%d\n", rc);
        mm_app_del_channel(test_obj, p_ch);
        mm_app_del_channel(test_obj, v_ch);
        mm_app_del_channel(test_obj, s_ch);
        return rc;
    }
#endif
    return rc;
}

int mm_app_stop_record_preview(mm_camera_test_obj_t *test_obj)
{
    int rc = MM_CAMERA_OK;
    mm_camera_channel_t *p_ch   = NULL;
    mm_camera_channel_t *v_ch   = NULL;
    mm_camera_channel_t *s_ch   = NULL;
    mm_camera_channel_t *rep_ch = NULL;

    p_ch   = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_PREVIEW);
    v_ch   = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_VIDEO);
    s_ch   = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_SNAPSHOT);
    rep_ch = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_REPROCESS);

    rc = mm_app_stop_and_del_channel(test_obj, p_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("Stop Preview failed rc=%d\n", rc);
    }

    rc = mm_app_del_stream_channel(test_obj, v_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("Stop Preview failed rc=%d\n", rc);
    }

    rc = mm_app_del_stream_channel(test_obj, s_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("Stop Preview failed rc=%d\n", rc);
    }

    rc = mm_app_del_stream_channel(test_obj, rep_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("Stop Preview failed rc=%d\n", rc);
    }
    return rc;
}

int mm_app_start_record(mm_camera_test_obj_t *test_obj)
{
    int rc = MM_CAMERA_OK;
    mm_camera_channel_t *v_ch = NULL;

    v_ch = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_VIDEO);

    rc = mm_app_start_channel(test_obj, v_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("start recording failed rc=%d\n", rc);
    }

    return rc;
}

int mm_app_stop_record(mm_camera_test_obj_t *test_obj)
{
    int rc = MM_CAMERA_OK;
    mm_camera_channel_t *v_ch = NULL;

    v_ch = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_VIDEO);

    rc = mm_app_stop_channel(test_obj, v_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("stop recording failed rc=%d\n", rc);
    }

    return rc;
}

int mm_app_start_live_snapshot(mm_camera_test_obj_t *test_obj)
{
    int rc = MM_CAMERA_OK;
    mm_camera_channel_t *s_ch = NULL;
    mm_camera_stream_t *s_metadata = NULL;
    mm_camera_channel_t *p_ch = NULL;
    mm_camera_stream_t *s_preview = NULL;
    s_ch = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_SNAPSHOT);
    p_ch = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_PREVIEW);
    if (NULL != p_ch) {
        for (int i = 0; i < p_ch->num_streams; i++) {
            LOGE("p_ch->streams[%d].s_config.stream_info->stream_type:%d", i, p_ch->streams[i].s_config.stream_info->stream_type);
            if ( CAM_STREAM_TYPE_PREVIEW == p_ch->streams[i].s_config.stream_info->stream_type) {
                s_preview = &p_ch->streams[i];
                LOGE("Get s_preview:%p", s_preview);
            } else if ( CAM_STREAM_TYPE_METADATA == p_ch->streams[i].s_config.stream_info->stream_type) {
              s_metadata = &p_ch->streams[i];
              LOGE("Get s_metadata:%p", s_metadata);
            }
        }
        if (s_metadata) {
            rc = test_obj->cam->ops->link_stream(test_obj->cam->camera_handle, p_ch->ch_id, s_metadata->s_id, s_ch->ch_id);
            if (!rc) {
                LOGE("link metadata fail!!! :%d", rc);
            }
        } else {
            LOGE("get metadata stream NULL(%p)!!!", s_metadata);
        }
    } else {
        LOGE("get preivew channel NULL!!!");
    }

    rc = mm_app_start_channel(test_obj, s_ch);
    rc |= mm_app_start_reprocess(test_obj);
    if (MM_CAMERA_OK != rc) {
        LOGE("start recording failed rc=%d\n", rc);
    }

    return rc;
}

int mm_app_stop_live_snapshot(mm_camera_test_obj_t *test_obj)
{
    int rc = MM_CAMERA_OK;
    mm_camera_channel_t *s_ch = NULL;
    //mm_camera_channel_t *rp_ch = NULL;

    s_ch = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_SNAPSHOT);
    //rp_ch = mm_app_get_channel_by_type(test_obj, MM_CHANNEL_TYPE_REPROCESS);

    rc = mm_app_stop_channel(test_obj, s_ch);
    if (MM_CAMERA_OK != rc) {
        LOGE("stop recording failed rc=%d\n", rc);
    }
    rc |= mm_app_stop_reprocess(test_obj);
    if (MM_CAMERA_OK != rc) {
        LOGE("stop recording failed rc=%d\n", rc);
    }
    return rc;
}
