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

#ifndef __MM_VTEST_CORE_H__
#define __MM_VTEST_CORE_H__

#include <stdint.h>
#include <queue>
#include "vtest_XmlComdef.h"
#include "vtest_XmlParser.h"
#include "vtest_SignalQueue.h"
#include "vtest_LinkQueue.h"
#include "vtest_Debug.h"

#define LOCAL_FRAME_QUEUE_SIZE   30
#define UPLOAD_FRAME_QUEUE_SIZE   10

typedef enum VideoCodingType {
    VIDEO_CodingAVC,    /**< H.264/AVC */
    VIDEO_CodingHEVC,   /**< ITU H.265/HEVC */
    VIDEO_CodingH263,   /**< H.263 */
    VIDEO_CodingMPEG4,  /**< MPEG-4 */
    VIDEO_CodingVP8,    /**< Google VP8, formerly known as On2 VP8 */
    VIDEO_CodingMax
} VideoCodingType;

typedef enum VideoCodecProfileType {
    VIDEO_MPEG4ProfileSimple,
    VIDEO_MPEG4ProfileAdvancedSimple,
    VIDEO_H263ProfileBaseline,
    VIDEO_AVCProfileBaseline,
    VIDEO_AVCProfileHigh,
    VIDEO_AVCProfileMain,
    VIDEO_AVCProfileConstrainedBaseline,
    VIDEO_AVCProfileConstrainedHigh,
    VIDEO_VP8ProfileMain,
    VIDEO_VP8ProfileVersion0,
    VIDEO_VP8ProfileVersion1,
    VIDEO_HEVCProfileMain,
    VIDEO_HEVCProfileMain10,
}VideoCodecProfileType;

typedef enum VideoStates {
    State_Stopped,
    State_Recording,
    State_Stopping,
}VideoStates;

typedef enum VideoModes {
    Mode_Unknow,
    Mode_Rec,
    Mode_Pre,
}VideoModes;

typedef enum StreamType {
    Stream_Local,
    Stream_Upload,
}StreamType;

static OMX_STRING StateString[] =
{
    (OMX_STRING)"Stopped",
    (OMX_STRING)"Recording",
    (OMX_STRING)"Stopping"
};

static OMX_STRING ModeString[] =
{
    (OMX_STRING)"Unknow",
    (OMX_STRING)"Rec",
    (OMX_STRING)"Pre"
};

enum SourceFormat {
    Format_NV21,
    Format_NV12,
};

typedef void (*PFN_DIR_CB)(char *pDir, char *pFileName);	// event-flags callback proto
typedef void (*PFN_UPLOAD_CB)(char *pBuffer, uint32_t nByte);//
typedef void (*PFN_AVI_ON_CB)(char *pDir, char *pFileName, int videoFormat);
typedef void (*PFN_AVI_SAVE_CB)(char *pDir, char *pFileName, float videoRate, int videoFormat);
typedef void (*PFN_AVI_MEM_BUF_CB)(char *buf, int buf_size, float videoRate, int videoFormat);


namespace vtest {

/**
 * @brief Class for configuring video encoder and video encoder test cases
 */
class VideoCore {

public:
    /**
     * @brief Constructor
     */
    VideoCore();

    /**
     * @brief Destructor
     */
    ~VideoCore();


    void SetResolution(OMX_U32 width, OMX_U32 height);
    void SetBitrate(OMX_U32 bitrate);
    void SetFramerate(OMX_U32 frameRate);
    void SetPFrame(OMX_U32 pFrames);
    void SetPartTimes(OMX_U32 times);
    void SetPreTimes(OMX_U32 times);
    void SetSourceFormat(SourceFormat format);
    void SetCodingType(VideoCodingType codingType, VideoCodecProfileType profileType);
    void SetStreamType(StreamType type);
    int Init();
    int PreStart();
    int PreStop();
    int Start();
    int Stop();
    int Write();
    int Push(void *frameData);
    int RegisterDirCallback(PFN_DIR_CB pfnEventCb);
    int RegisterUploadCallback(PFN_UPLOAD_CB pfnUploadCb);
    int RegisterAviOnCallback(PFN_AVI_ON_CB pfnAviOnCb);
    int RegisterAviSaveCallback(PFN_AVI_SAVE_CB pfnAviSaveCb);
    int RegisterAviMemBufCallback(PFN_AVI_MEM_BUF_CB pfnMemBufCB);

private:
    OMX_BOOL FrameFilter();
public:
    OMX_S32 nWidth;
    OMX_S32 nHeight;
    OMX_S32 nFrameSize;
    OMX_S32 nBitrate;   //Kbit
    OMX_S32 nFrameRate;
    OMX_S32 nPFrames;
    OMX_S32 nPartTimes;//second
    OMX_S32 nPreTimes;//second
    OMX_BOOL bUpload;
    OMX_BOOL bTimeout;
    OMX_TICKS nStartTime;
    OMX_TICKS nEndTime;
    OMX_TICKS nRunTimeMillis;
    OMX_S32 nFrameNum;
    OMX_S32 nRealFrameRate;
    OMX_S32 nVideoPart;
    VideoStates eState;
    VideoStates ePreState;
    VideoModes eCurMode;
    VideoModes eLastMode;
    SourceFormat eSourceFormat;
    VideoCodingType eCodingType;
    VideoCodecProfileType eProfileType;
    StreamType eStreamType;
    XmlParser *pXmlParser;
    VideoStaticProperties sGlobalVideoProp;
    VideoSessionInfo sSessionInfo;
    SignalQueue *pSignalQueue;
    Mutex *pMutex;
    Mutex *pQueueMutex;
    LinkQueue *pLinkQueue;
    std::queue<OMX_TICKS> *pTimeQueue;
    OMX_BOOL bPreFile;
    OMX_BOOL bPreFull;
    char sFileHead[MAX_STR_LEN];
    char PreOutRoot[MAX_STR_LEN];
    char PreFileName[MAX_STR_LEN];
    OMX_S32 sHeadSize;
    OMX_S32 nPreLength;
    char* video_buf;
    int memcpy_offset;

    /**
     * @brief  callback
     */
    PFN_UPLOAD_CB pfnUploadCB;
    PFN_AVI_ON_CB pfnAviOnCB;
    PFN_AVI_SAVE_CB pfnAviSaveCB;
    PFN_AVI_MEM_BUF_CB pfnMemBufCB;

private:
    OMX_TICKS nFrameInterval;
    OMX_TICKS nTime;
};

} // namespace vtest

#endif /* __MM_VTEST_CORE_H__ */









