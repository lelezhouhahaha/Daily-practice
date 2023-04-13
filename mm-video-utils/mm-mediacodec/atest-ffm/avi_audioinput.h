/*----------------------------------------------------------------------
Copyright (c) 2017-2018 Qualcomm Technologies, Inc. All Rights Reserved.
Qualcomm Technologies Proprietary and Confidential
------------------------------------------------------------------------*/

#ifndef _AVI_AUDIOINPUT_H
#define _AVI_AUDIOINPUT_H

#include <stdio.h>
#include <stdlib.h>   // exit
#include <fcntl.h>    // O_WRONLY
#include <sys/stat.h>
#include <time.h>     // time
#include <errno.h>
#include <utils/Log.h>
#include "avi_queuec.h"
#include "avi_types.h"
#ifdef __cplusplus
extern "C" {
#endif
#include "mm_qthread_pool.h"
#ifdef __cplusplus
}
#endif

/***********************************************************************
 *	Dedinfe
 ***********************************************************************/
#define FFMLOG_TAG "FFMAudio"
#define POLLING_MAX 100


/***********************************************************************
 *	Shared Typedefs & Macros
 ***********************************************************************/
typedef struct AVSampleFmt {
    int fmt;
    char* str;
} sample_fmt_entries;

typedef struct Pcm2aacContext {
    AVCodec *codec;
    AVCodecContext *c;
    AVPacket *pkt;
    AVFrame *frame;
    int buffer_size;
    int sample_rate;
    int channel_layout;
    int in_format; //S16, S16P...
} Pcm2aacContext;

typedef struct PcmData {
    uint8_t *data;
    int64_t maxlen;
} PcmData;

typedef struct AACData {
    uint8_t *data;
    int64_t maxlen;
} AACData;


static const int sample_rate[] =
{
  8000,
  16000,
  24000,
  44100,
  48000
};

static const sample_fmt_entries sample_fmt[] = {
  { 16, "s16le" },
  /* must come after s16le in this list */
  { 16, "u16le" },
  { 32, "s32le" },
  { 32, "u32le" }
};

static const int channel_layout[] =
{
  1, //mono
  2, //stereo
  3,
  4
};

/*****************************************************************************
 *	Camera Global Variable
 *****************************************************************************/
extern "C" void *AVI_AudioInput(void * arg);


// The AVI_HANDLETYPE structure defines the component handle.  The component
// handle is used to access all of the component's public methods and also
// contains pointers to the component's private data area.  The component
// handle is initialized by the AVI core (with help from the component)
// during the process of loading the component.  After the component is
// successfully loaded, the application can safely access any of the
// component's public functions (although some may return an error because
// the state is inappropriate for the access).
//
// @ingroup comp
//

class AVIAudioInput
{
public:
    AVIAudioInput();
    virtual ~AVIAudioInput();

    int start(struct record_info *record_info,bool deleted);
    int stop(AVIOutputProperty *property,bool deleted);
    int64_t readData(char *data, int64_t maxlen);
    int64_t writeData(const void *data, int64_t len);
    int bytesAvailable() const;
    void clearBuffer();
    int refreshBuffer(char *fileName);
    int bytesDelete(bool deleted);
    int pcm2aacProcess(Pcm2aacContext *context);

private:
    int initializeAudioInput(Pcm2aacContext *context);
    int getSampleRate(int res);
    int getFormat(int res);
    int getChannels(int res);
    int checkSampleFmt(const AVCodec *codec, enum AVSampleFormat sample_fmt);
    int initAACHeader(Pcm2aacContext *context);
    int writeAACHeader(uint8_t *outBuffer, AVPacket* pkt);
    int encodeAudioFrame(AVCodecContext *ctx, AVFrame *frame, AVPacket *pkt, AACData *outData);
    int transSamplerate2freqIdx(int sample_rate);


public:
    QElemType element;
    queue *PQueue;
    char fileName[MAX_STRING_LEN];
    char filePath[MAX_STRING_LEN];
    PcmData *inData;
    AACData *outData;
    AACData *outData1;
    Pcm2aacContext *context;
    int64_t audio_len;
    char audio_data[AUDIO_NUM_DATA_POINTERS];
    int64_t duration_1ms;
    int watchdog;
    int nKillThread;
    bool audio_enable;
    bool watchdogThread;

private:
    //CThread_pool_t *m_threadPool;
    int64_t audio_time_base;
    bool bytes_delete;
    int fifoFd;
    pthread_mutex_t mutex;		//Define lock

    const AVCodec *codec;
    AVCodecContext *codec_ctx;
    AVFrame *in_frame;
    AVPacket *pkt;
    char aac_adts_header[7] = {0};
    int chanCfg = 1;
};


#endif // #ifndef _AVI_AUDIOINPUT_H
