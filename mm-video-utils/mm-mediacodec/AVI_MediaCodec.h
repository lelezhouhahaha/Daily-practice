/*
 * Copyright (c) 2019 The Meig Group Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject
 * to the following conditions:
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

/** AVI_MediaCodec.h - AVI_Mediacodec IL version 1.0.0
 *  The AVI_Mediacodec header file contains the definitions used to define
 *  the public interface of a component.  This header file is intended to
 *  be used by both the application and the component.
 */

#ifndef __AVI_MEDIACODEC_H
#define __AVI_MEDIACODEC_H


#define __STDC_CONSTANT_MACROS

#ifdef _WIN32
//Windows
extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
};
#else
//Linux...
#ifdef __cplusplus
extern "C"
{
#endif
#include <libavutil/avstring.h>
#include <libavutil/channel_layout.h>
#include <libavutil/common.h>
#include <libavutil/frame.h>
#include <libavutil/samplefmt.h>
#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avio.h>
#include <libavutil/file.h>
#ifdef __cplusplus
};
#endif
#endif
/* Each AVI header must include all required header files to allow the
 *  header to compile without errors.  The includes below are required
 *  for this header file to compile successfully
 */

#include <algorithm>	//added from computing.net tip
#include <sys/types.h>
#include <dirent.h>
#include <vector>
#include <string>
#include <stdio.h>
#include <stdlib.h>   // exit
#include <fcntl.h>    // O_WRONLY
#include <time.h>     // time
#include <errno.h>
#include <sys/stat.h>
#include <utils/Log.h>
#include <fstream>
#include <iostream>
#ifdef __cplusplus 
extern "C" {
#endif
#include "mm_qthread_pool.h"
#ifdef __cplusplus 
}
#endif
#include "vtest_Core.h"
#include "avi_audioinput.h"


/*****************************************************************************
 *	Dedinfe
 *****************************************************************************/
#define MEIGLOG_TAG "HALMeigCam"

#define AVI_MEDIACODEC_TEMPFILE


/*****************************************************************************
 *	Shared Typedefs & Macros
 *****************************************************************************/


/*****************************************************************************
 *	Camera Global Variable
 *****************************************************************************/

/** @Callback function type */
typedef void (*PFN_AVIPOST_CB)(
        char *pDir,
        char *pFileName,
        int m_startFormat);
typedef void (*AVI_Videofilename_cb)(
        char* videofilename_cb);

/** @AVI callback structure. */
typedef struct {
    /** set to sizeof(BarCallbacks) */
    size_t      size;
    PFN_DIR_CB vtest_swvenc_diag;
    PFN_UPLOAD_CB vtest_swvenc_send;
    PFN_AVI_ON_CB venc_notify_start;
    PFN_AVI_SAVE_CB venc_notify_done;
    AVI_Videofilename_cb venc_filename_cb;
} AVICallbacks;



/** @ingroup comp */
//typedef enum AVI_PARAMETER {
//    AVI_SWMENC_STATUS,       /**< Media recording saves the transmitted signal. */
//    AVI_SWMENC_DIAG,         /**< Media recording saves the transmitted signal. */
//    AVI_SWVENC_FORMAT,       /**< Video recording saves the transmitted signal. */
//    AVI_SWVENC_FRAME_RATE,   /**< Video recording saves the transmitted signal. */
//    AVI_SWVENC_CODE_RATE,    /**< Video recording saves the transmitted signal. */
//    AVI_SWVENC_RESOLUTION,   /**< Video recording saves the transmitted signal. */
//    AVI_SWVENC_PFRAME,       /**< Video recording saves the transmitted signal. */
//    AVI_SWVENC_SECTION_TIME, /**< Video recording saves the transmitted signal. */
//}AVI_PARAMETER;


class AVIMediaCodec
{
public:
    AVIMediaCodec();
    virtual ~AVIMediaCodec();

    void initializeMedia();
    int get_audio_frame();
    int get_video_start(AVIOutputProperty *property);
    int get_video_done(AVIOutputProperty *property);
    int mediaDemuxer(AVIOutputProperty *property);
    int registerCallback(AVICallbacks* callbacks);
    int startRecord(struct record_info *record_info);
    int stopRecord(void);
    int startPreRecord(struct record_info *record_info);
    int stopPreRecord(void);
    int64_t pushData(const void* data, int64_t len);
    int64_t pushData(const void* data, int64_t len, uint32_t frame_idx);
    AVICallbacks* notify;

private:
    void displayPoolStatus(
            CThread_pool_t * pPool);
    int getMediadir(std::string dir, std::vector<std::string> &files);

public:
    AVIAudioInput *m_audioInput;
    //Create video codec
    vtest::VideoCore *pVideoCore;
    int nKillThread;
    struct record_info record;
    bool m_record;
    bool m_preRecord;
    bool m_abandon;
    AVIOutputProperty syncProperty;
    AVIOutputProperty demuxerProperty;
    AACData *aacData;

private:
    //CThread_pool_t *m_threadPool;
    char sVideoStartProp[MAX_STRING_LEN];
    char sVideoDoneProp[MAX_STRING_LEN];
    char sAudioFrameProp[MAX_STRING_LEN];
};

#endif
/* File EOF */
