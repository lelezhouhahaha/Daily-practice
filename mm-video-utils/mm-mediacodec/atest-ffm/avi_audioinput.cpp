/*
 * Copyright (c) 2001 Fabrice Bellard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * @file
 * audio encoding with libavcodec API example.
 *
 * @example encode_audio.c
 */

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
#include <libavcodec/avcodec.h>
#include <libavutil/channel_layout.h>
#include <libavutil/common.h>
#include <libavutil/frame.h>
#include <libavutil/samplefmt.h>
#include <libswresample/swresample.h>
#ifdef __cplusplus
};
#endif
#endif
#include <cutils/properties.h>
#include "avi_android_api.h"
#include "avi_audioinput.h"

#define WATCHDOG_PULLING_TIME 200 //200ms
#define WATCHDOG_MAX_NUMBER   10


static const uint8_t PCMEnv_SoundmaTable[1024] = {
  5,   0,   12,  0,   13,  0,   7,   0,   250, 255, 253, 255, 255, 255, 253, 255,
  251, 255, 241, 255, 238, 255, 238, 255, 242, 255, 230, 255, 229, 255, 226, 255,
  231, 255, 229, 255, 226, 255, 229, 255, 234, 255, 245, 255, 235, 255, 238, 255,
  244, 255, 246, 255, 246, 255, 251, 255, 254, 255, 249, 255, 1,   0,   254, 255,
  3,   0,   7,   0,   7,   0,   7,   0,   5,   0,   4,   0,   10,  0,   3,   0  ,
  0,   0,   3,   0,   255, 255, 250, 255, 240, 255, 245, 255, 231, 255, 234, 255,
  232, 255, 233, 255, 215, 255, 220, 255, 218, 255, 216, 255, 213, 255, 211, 255,
  210, 255, 212, 255, 214, 255, 216, 255, 217, 255, 218, 255, 229, 255, 232, 255,
  234, 255, 233, 255, 240, 255, 239, 255, 247, 255, 251, 255, 246, 255, 248, 255,
  254, 255, 247, 255, 251, 255, 254, 255, 255, 255, 251, 255, 251, 255, 243, 255,
  247, 255, 244, 255, 245, 255, 241, 255, 234, 255, 231, 255, 228, 255, 225, 255,
  215, 255, 217, 255, 212, 255, 211, 255, 211, 255, 210, 255, 214, 255, 210, 255,
  203, 255, 219, 255, 217, 255, 218, 255, 225, 255, 228, 255, 236, 255, 233, 255,
  244, 255, 246, 255, 244, 255, 254, 255, 255, 255, 253, 255, 254, 255, 5,   0  ,
  7,   0,   10,  0,   4,   0,   254, 255, 3,   0,   4,   0,   4,   0,   254, 255,
  252, 255, 248, 255, 249, 255, 240, 255, 243, 255, 241, 255, 237, 255, 243, 255,
  236, 255, 233, 255, 234, 255, 235, 255, 237, 255, 232, 255, 244, 255, 249, 255,
  2,   0,   6,   0,   9,   0,   13,  0,   13,  0,   24,  0,   25,  0,   28,  0  ,
  22,  0,   16,  0,   20,  0,   23,  0,   27,  0,   26,  0,   20,  0,   18,  0  ,
  16,  0,   19,  0,   11,  0,   8,   0,   8,   0,   7,   0,   1,   0,   3,   0  ,
  1,   0,   254, 255, 253, 255, 250, 255, 253, 255, 249, 255, 251, 255, 254, 255,
  6,   0,   13,  0,   17,  0,   23,  0,   22,  0,   23,  0,   29,  0,   40,  0  ,
  36,  0,   41,  0,   44,  0,   47,  0,   44,  0,   43,  0,   47,  0,   42,  0  ,
  47,  0,   44,  0,   38,  0,   34,  0,   25,  0,   28,  0,   16,  0,   17,  0  ,
  13,  0,   5,   0,   7,   0,   2,   0,   0,   0,   255, 255, 255, 255, 255, 255,
  2,   0,   253, 255, 251, 255, 7,   0,   7,   0,   16,  0,   21,  0,   24,  0  ,
  28,  0,   25,  0,   31,  0,   36,  0,   37,  0,   42,  0,   34,  0,   43,  0  ,
  40,  0,   38,  0,   42,  0,   30,  0,   27,  0,   30,  0,   26,  0,   20,  0  ,
  18,  0,   11,  0,   9,   0,   8,   0,   2,   0,   254, 255, 250, 255, 247, 255,
  250, 255, 247, 255, 249, 255, 250, 255, 250, 255, 251, 255, 249, 255, 252, 255,
  1,   0,   9,   0,   9,   0,   12,  0,   8,   0,   9,   0,   20,  0,   12,  0  ,
  20,  0,   24,  0,   21,  0,   19,  0,   22,  0,   23,  0,   14,  0,   13,  0  ,
  19,  0,   12,  0,   11,  0,   3,   0,   254, 255, 248, 255, 245, 255, 242, 255,
  238, 255, 237, 255, 242, 255, 238, 255, 233, 255, 238, 255, 239, 255, 244, 255,
  240, 255, 245, 255, 243, 255, 250, 255, 250, 255, 2,   0,   5,   0,   9,   0  ,
  8,   0,   10,  0,   14,  0,   14,  0,   14,  0,   21,  0,   20,  0,   8,   0  ,
  9,   0,   10,  0,   15,  0,   12,  0,   8,   0,   3,   0,   1,   0,   6,   0  ,
  251, 255, 245, 255, 241, 255, 233, 255, 234, 255, 237, 255, 239, 255, 231, 255,
  234, 255, 240, 255, 242, 255, 236, 255, 238, 255, 239, 255, 247, 255, 241, 255,
  250, 255, 254, 255, 1,   0,   5,   0,   9,   0,   2,   0,   10,  0,   10,  0  ,
  6,   0,   9,   0,   10,  0,   8,   0,   7,   0,   6,   0,   3,   0,   3,   0  ,
  251, 255, 251, 255, 249, 255, 247, 255, 240, 255, 236, 255, 237, 255, 223, 255,
  224, 255, 228, 255, 229, 255, 234, 255, 230, 255, 232, 255, 231, 255, 228, 255,
  235, 255, 238, 255, 244, 255, 249, 255, 246, 255, 246, 255, 249, 255, 248, 255,
  248, 255, 255, 255, 3,   0,   246, 255, 8,   0,   2,   0,   255, 255, 249, 255,
  251, 255, 0,   0,   248, 255, 242, 255, 248, 255, 250, 255, 239, 255, 239, 255,
  241, 255, 239, 255, 235, 255, 239, 255, 237, 255, 242, 255, 238, 255, 241, 255,
  235, 255, 237, 255, 237, 255, 235, 255, 248, 255, 238, 255, 238, 255, 244, 255,
  253, 255, 243, 255, 244, 255, 243, 255, 241, 255, 247, 255, 251, 255, 254, 255,
  2,   0,   253, 255, 249, 255, 249, 255, 254, 255, 5,   0,   1,   0,   255, 255,
  251, 255, 251, 255, 250, 255, 250, 255, 251, 255, 243, 255, 240, 255, 246, 255,
  244, 255, 241, 255, 243, 255, 246, 255, 240, 255, 241, 255, 243, 255, 247, 255,
  247, 255, 248, 255, 248, 255, 246, 255, 251, 255, 246, 255, 249, 255, 245, 255,
  254, 255, 254, 255, 251, 255, 250, 255, 255, 255, 251, 255, 254, 255, 2,   0  ,
  249, 255, 255, 255, 248, 255, 2,   0,   253, 255, 253, 255, 252, 255, 254, 255,
  251, 255, 248, 255, 253, 255, 253, 255, 250, 255, 249, 255, 247, 255, 250, 255,
  255, 255, 252, 255, 250, 255, 247, 255, 251, 255, 1,   0,   3,   0,   4,   0  ,
  4,   0,   252, 255, 2,   0,   2,   0,   6,   0,   6,   0,   11,  0,   13,  0  ,
  5,   0,   3,   0,   3,   0,   5,   0,   1,   0,   1,   0,   255, 255, 255, 255,
  250, 255, 253, 255, 250, 255, 253, 255, 2,   0,   1,   0,   3,   0,   255, 255,
  250, 255, 252, 255, 251, 255, 253, 255, 0,   0,   5,   0,   4,   0,   5,   0  ,
  4,   0,   3,   0,   4,   0,   7,   0,   3,   0,   3,   0,   3,   0,   2,   0  ,
  5,   0,   1,   0,   6,   0,   5,   0,   6,   0,   8,   0,   9,   0,   1,   0  ,
  4,   0,   9,   0,   11,  0,   14,  0,   12,  0,   3,   0,   5,   0,   5,   0
};


/*===========================================================================
 * FUNCTION   : ping_pong_data
 *
 * DESCRIPTION: Double buffered interface for audio data
 *
 * PARAMETERS :
 *   @arg : Structure pointer
 *
 * RETURN     : 0  -- success
 *              none-zero failure code
 *==========================================================================*/
void *ping_pong_data(void * data)
{
    AVIAudioInput *pThis = (AVIAudioInput*)data;
    pThis->pcm2aacProcess(pThis->context);

    return NULL;
}

/*===========================================================================
 * FUNCTION   : async_watchdog_thread
 *
 * DESCRIPTION: Watchdog thread of audio data synchronization program
 *
 * PARAMETERS :
 *   @arg : Structure pointer
 *
 * RETURN     : 0  -- success
 *              none-zero failure code
 *==========================================================================*/
void *async_watchdog_thread(void * data)
{
    AVIAudioInput *pThis = (AVIAudioInput*)data;

    //How to determine whether this is the stopRecord or the completion of the segmentation.
    while((pThis->nKillThread != KILL_THREAD_EXIT)| pThis->audio_enable)
    {
        pThis->watchdog++;
        //Delay 200ms
        usleep(MAX_SLEEP_TIME*WATCHDOG_PULLING_TIME);

        //If the watchdog time has been greater than 2S, it is forced to send fake audio data.
        if(pThis->watchdog > WATCHDOG_MAX_NUMBER)
        {
            int count = (pThis->watchdog*WATCHDOG_PULLING_TIME) / (pThis->audio_len/pThis->duration_1ms);
            av_log(NULL, AV_LOG_VERBOSE,"%s:%d watchdog=<%d> Audio PCM timed out times is [%d].\n",
                    __func__,__LINE__,pThis->watchdog,count);
            for(int i=0;i<count;i++)
                pThis->writeData(PCMEnv_SoundmaTable, sizeof(PCMEnv_SoundmaTable));
        }
    }
    av_log(NULL, AV_LOG_VERBOSE,
        "%s:%d Thread EXIT.\n",
        __func__, __LINE__);
    pThis->watchdogThread = false;
    return NULL;
}



/*===========================================================================
 * FUNCTION   : AVIAudioInput
 *
 * DESCRIPTION: default constructor of AVIAudioInput
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
AVIAudioInput::AVIAudioInput()
{
    fifoFd = -1;
    //Create a queue object
    PQueue = (queue *)malloc(sizeof(queue));
    if(!PQueue->pBase)
    {
        av_log(NULL, AV_LOG_ERROR,
            "%s::Failed to allocate memory to queue object.\n",__func__);
    }
    //Call the function that initializes the queue
    initQueue(PQueue,MAX_SIZE);
    pthread_mutex_init(&mutex, NULL); //Initialize the lock mutex

    element.sample_rate = getSampleRate(0);
    element.format      = getFormat(0);
    element.channels    = getChannels(0);

    audio_len = 1024;
    audio_time_base = 0;

    audio_enable = false;
    bytes_delete = false;
    watchdogThread = false;
    memset(audio_data, 0x00, AUDIO_NUM_DATA_POINTERS);
    memset(fileName, 0, sizeof(fileName));
    memset(filePath, 0, sizeof(filePath));

    inData = (PcmData *)malloc(sizeof(PcmData));
    outData = (AACData *)malloc(sizeof(AACData));
    outData1 = (AACData *)malloc(sizeof(AACData));
    memset(inData, 0, sizeof(PcmData));
    memset(outData, 0, sizeof(AACData));
    memset(outData1, 0, sizeof(AACData));


    context = (Pcm2aacContext*)malloc(sizeof(Pcm2aacContext));
    if(context == NULL) {
        av_log(NULL, AV_LOG_ERROR,
            "%s:%d malloc context error\n", __func__,__LINE__);
        //return AVERROR(ENOMEM);
    }
    context->in_format = AV_SAMPLE_FMT_S16;       //Initialization bit depth
    context->sample_rate = 16000;                 //Initial sample rate
    context->channel_layout = AV_CH_LAYOUT_MONO;  //Initialization number of channels
    //context->channel_layout = AV_CH_LAYOUT_STEREO;

    //1ms data length is 16 * 16 * 1 = 16 * 2byte
    duration_1ms = (element.sample_rate/1000) * (element.format/8) * element.channels;

    //30s total has 16 * 2byte * 1000 * 30 Byte data
    inData->maxlen = duration_1ms*MAX_SIZE*30;
    //read pcm file to memory * 2
    inData->data = (uint8_t*)av_mallocz(inData->maxlen * 2);
    //output aac file to memory
    outData->data = (uint8_t *)av_mallocz(inData->maxlen);
    //audio aac file to memory
    outData1->data = (uint8_t*)av_mallocz(inData->maxlen);
    outData1->maxlen = 0;

    //api
    initializeAudioInput(context);
}

/*===========================================================================
 * FUNCTION   : ~AVIAudioInput
 *
 * DESCRIPTION: deconstructor of AVIAudioInput
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
AVIAudioInput::~AVIAudioInput()
{
    close(fifoFd);
    fifoFd = -1;
    free(PQueue);
    PQueue = NULL;
    //m_threadPool = NULL;
    pthread_mutex_destroy(&mutex);	//Destroy lock

    av_free(inData->data);
    av_free(outData->data);
    av_free(outData1->data);

    free(inData);
    free(outData);
    free(outData1);
    free(context);

    av_frame_free(&in_frame);
    av_packet_free(&pkt);
    /* flush the encoder */
    avcodec_free_context(&codec_ctx);
}

int AVIAudioInput::getSampleRate(int res)
{
    int sampleRate = 16000;    //Initial sample rate

    if(res < ARRAY_SIZE(sample_rate)) {
        sampleRate = sample_rate[res];
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d Set sampleRate = %d.\n", __func__,__LINE__,sampleRate);
    } else {
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d Error parameters res = %d!\n", __func__,__LINE__, res);
    }
    return sampleRate;
}

int AVIAudioInput::getFormat(int res)
{
    int fmt = 1;    //Initialization bit depth
    char sampleFmt[MAX_SIZE] = {};

    if(res < ARRAY_SIZE(sample_fmt)) {
        sprintf(sampleFmt,"%s",sample_fmt[res].str);
        fmt = sample_fmt[res].fmt;
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d Set sampleFmt = %s.\n", __func__,__LINE__,sampleFmt);
    } else {
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d Error parameters res = %d!\n", __func__,__LINE__, res);
    }
    return fmt;
}

int AVIAudioInput::getChannels(int res)
{
    int channel = 1;    //Initialization number of channels

    if(res < ARRAY_SIZE(channel_layout)) {
        channel = channel_layout[res];
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d Set channel = %d.\n", __func__,__LINE__,channel);
    } else {
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d Error parameters res = %d!\n", __func__,__LINE__, res);
    }
    return channel;
}


int AVIAudioInput::start(struct record_info *record_info,bool deleted)
{
    Q_UNUSED(deleted)
    audio_time_base = 0;

    audio_enable = true;
    bytes_delete = false;
    //Packets number is 30s total has (16 * 2Byte * 1000 * 30)/1024 = 937.5
    element.pkt_number = 0;
    element.sample_rate = getSampleRate(record_info->audio_sample_rate);
    element.format = getFormat(record_info->audio_channel_num);
    element.channels = getChannels(record_info->audio_sample_type);

    //1ms data length is 16 * 16 * 1 = 16 * 2byte
    duration_1ms = (element.sample_rate/1000) * (element.format/8) * element.channels;
    av_log(NULL, AV_LOG_VERBOSE,
        "%s:%d duration_1ms = [ %lld ] EEE.\n",__func__,__LINE__, duration_1ms);
    return 0;
}

int AVIAudioInput::stop(AVIOutputProperty *property,bool deleted)
{
    int retry;
    int64_t duration = audio_len/duration_1ms;

    audio_enable = false;
    bytes_delete = false;
    if(!deleted)
    {
        for (retry = 0; retry < 10; retry++) {
            if(audio_time_base < BUFFER_SPLIT_LENGTH*1000) {
                usleep(MAX_SLEEP_TIME*10);//10ms
                audio_time_base += duration;
                element.pkt_number++;
                element.pkt_size = audio_len;
                /* copy internal buffer data to buf */
                memcpy(element.pkt_buf, audio_data, audio_len);
                element.pts  = audio_time_base;
                //Calling in the column function.
                enQueue(PQueue, element);
                continue;
            } else {
                av_log(NULL, AV_LOG_DEBUG,
                    "%s:%d Audio duration is Normal.\n",__func__,__LINE__);
                break;
            }
        }
    }

    sprintf(filePath, "%s", property->m_donePath);
    sprintf(fileName, "%s", property->m_doneName);
    // Save audio after stop, save audio after stop.
    av_log(NULL, AV_LOG_DEBUG,
        "%s:%d filePath=%s fileName=%s EEE.\n",__func__,__LINE__,filePath,fileName);
    av_log(NULL, AV_LOG_VERBOSE,
        "%s:%d pkt_number = [%d] Audio PCM Data Timestamp <%lld>.\n",
        __func__, __LINE__, element.pkt_number, audio_time_base);
    refreshBuffer(property->m_doneName);
    return 0;
}

int AVIAudioInput::bytesDelete(bool deleted)
{
    bytes_delete = deleted;
    return 0;
}

int AVIAudioInput::bytesAvailable() const
{
    return getQueueLen(PQueue);
}

void AVIAudioInput::clearBuffer()
{
    clearQueue(PQueue);
    if(!watchdogThread)
    {
        watchdogThread = true;
        pThreadPool->AddWorkLimit((void *)pThreadPool, async_watchdog_thread, (void *)this);
    }
}

int AVIAudioInput::refreshBuffer(char *fileName)
{
    Q_UNUSED(fileName)

    //Traversing from scratch
    int i = PQueue->front;
    //Packets number is 30s total has (16 * 2Byte * 1000 * 30)/1024 = 937.5
    //30s total has 16 * 2byte * 1000 * 30 = 96000 Byte
    inData->maxlen = 0;

    //If you don't reach the rear position, loop
    while(i != PQueue->rear)
    {
        memcpy(inData->data + inData->maxlen,
               PQueue->pBase[i].pkt_buf, PQueue->pBase[i].pkt_size);
        inData->maxlen = inData->maxlen + PQueue->pBase[i].pkt_size;
        //Move to the next position
        i = (i+1) % PQueue->maxSize;
    }
    clearQueue(PQueue);

#if 0
    FILE *pcmFile;
    char pcm_filename[MAX_STRING_LEN]="";

    sprintf(pcm_filename, "%s%s%s", AVI_DEMUXER_LOCATION, fileName, ".pcm");
    pcmFile = fopen(pcm_filename,"wb");
    if (!pcmFile) {
        av_log(NULL, AV_LOG_ERROR,
            "%s:%d Could not open %s\n", __func__,__LINE__, pcm_filename);
        return Q_FALSE;
    }
    fwrite(inData->data, 1, inData->maxlen, pcmFile);
    fclose (pcmFile);
#endif
    if(pThreadPool != NULL)
        pThreadPool->AddWorkLimit((void *)pThreadPool, ping_pong_data, (void *)this);
    else
        return AVERROR(ENOMEM);

    av_log(NULL, AV_LOG_DEBUG,
            "%s:%d X.\n",__func__,__LINE__);
    return Q_TRUE;
}


#define PCM_DATA_IO
#ifdef PCM_DATA_IO
/*===========================================================================
 * FUNCTION   : readData
 *
 * DESCRIPTION: deconstructor of push pcm data
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int64_t AVIAudioInput::readData(char *data, int64_t maxlen)
{
    Q_UNUSED(data)
    Q_UNUSED(maxlen)

    return 0;
}

/*===========================================================================
 * FUNCTION   : writeData
 *
 * DESCRIPTION: deconstructor of push pcm data
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/

int64_t AVIAudioInput::writeData(const void *data, int64_t len)
{
    char file_name[MAX_STRING_LEN]="";
    int64_t duration = len/duration_1ms;

    audio_len = len;
    memcpy(audio_data, data, len);
    av_log(NULL, AV_LOG_DEBUG,
        "%s:%d Audio PCM Data Timestamp <%lld>.\n",
        __func__, __LINE__, audio_time_base);

    if(fifoFd < 0)
    {
        fifoFd = open(audio_fifo, O_WRONLY | O_NONBLOCK);
        if(fifoFd < 0)
            av_log(NULL, AV_LOG_DEBUG,
                "Open Audio FIFO Failed!\n");
    }

    if(nKillThread != KILL_THREAD_EXIT)
    {
        audio_time_base += duration;
        element.pkt_number++;
        element.pkt_size = len;
        /* copy internal buffer data to buf */
        memcpy(element.pkt_buf, data, len);
        element.pts  = audio_time_base;

        //Calling in the column function.
        enQueue(PQueue, element);
    }

    if(bytes_delete)
    {
        int rc = deQueue(PQueue, &element);
        if(rc!=Q_TRUE)
        {
            av_log(NULL, AV_LOG_VERBOSE,
                "Once out of the team, the elements are:%s .\n", element.pkt_buf);
            return -EAVIIO;
        }
        //queueTraverse(PQueue);
    }

    char prop[MAX_STRING_LEN];
    property_get("persist.audio.queue.enabled", prop, "0");
    int isPQueueEnabled = atoi(prop);
    if(isPQueueEnabled)
    {
        property_set("persist.audio.queue.enabled", "0");
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d PQueue Length = %d \n",
                __func__,__LINE__, getQueueLen(PQueue));
        queueTraverse(PQueue);
    }

    int n = snprintf(file_name, sizeof(file_name), "%lld", duration);
    if(write(fifoFd, file_name, n+1) < 0)
        av_log(NULL, AV_LOG_DEBUG,
            "Write Audio FIFO Failed\n");
    watchdog=0;

    return 0;
}
#endif

/* check that a given sample format is supported by the encoder */
int AVIAudioInput::checkSampleFmt(const AVCodec *codec, enum AVSampleFormat sample_fmt)
{
    const enum AVSampleFormat *p = codec->sample_fmts;

    av_log(NULL, AV_LOG_VERBOSE,"%s:%d need support sample format %s\n",
                __func__, __LINE__, av_get_sample_fmt_name(sample_fmt));
    while (*p != AV_SAMPLE_FMT_NONE) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d Encoder support sample format %s\n",
                __func__, __LINE__, av_get_sample_fmt_name(*p));
        if (*p == sample_fmt)
            return 1;
        p++;
    }
    return 0;
}

int AVIAudioInput::transSamplerate2freqIdx(int sample_rate)
{
    switch(sample_rate) {
        case 96000:
            return 0;
        case 88200:
            return 1;
        case 64000:
            return 2;
        case 48000:
            return 3;
        case 44100:
            return 4;
        case 32000:
            return 5;
        case 24000:
            return 6;
        case 22050:
            return 7;
        case 16000:
            return 8;
        case 12000:
            return 9;
        case 11025:
            return 10;
        case 8000:
            return 11;
        case 7350:
            return 12;
        default:
            return -1;
    }
}



int AVIAudioInput::initializeAudioInput(Pcm2aacContext *context)
{
    AVCodec *codec;
    AVCodecContext *c= NULL;
    AVPacket *pkt;
    AVFrame *frame;
    int ret = 0;

    ret = initAACHeader(context);
    if (ret < 0) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d init aac header error\n",__func__, __LINE__);
        return AVERROR(ENOMEM);
    }

    //codec = avcodec_find_encoder(AV_CODEC_ID_MP2);
    context->codec = avcodec_find_encoder(AV_CODEC_ID_AAC);
    //codec = avcodec_find_encoder(AV_CODEC_ID_MP3);
    if (!context->codec) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d Codec not found\n",__func__, __LINE__);
        return AVERROR(ENOMEM);
    }

    c = avcodec_alloc_context3(context->codec);
    if (!c) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d Could not allocate audio codec context\n",__func__, __LINE__);
        return AVERROR(ENOMEM);
    }

    /* put sample parameters */
    c->bit_rate = 64000;

    /* check that the encoder supports s16 pcm input */
    c->sample_fmt = AV_SAMPLE_FMT_FLTP;
    if (!checkSampleFmt(context->codec, c->sample_fmt)) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d Encoder does not support sample format %s\n",
                __func__, __LINE__,av_get_sample_fmt_name(c->sample_fmt));
        return AVERROR(ENOMEM);
    }

    /* select other audio parameters supported by the encoder */
    c->sample_rate    = context->sample_rate;
    c->channel_layout = context->channel_layout;
    c->channels       = av_get_channel_layout_nb_channels(c->channel_layout);

    av_log(NULL, AV_LOG_VERBOSE,"%s:%d begin avcodec_open2\n",__func__, __LINE__);
    /* open it */
    if (avcodec_open2(c, context->codec, NULL) < 0) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d Could not open codec\n",__func__, __LINE__);
        return AVERROR(ENOMEM);
    }

    /* packet for holding encoded output */
    pkt = av_packet_alloc();
    if (!pkt) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d could not allocate the packet\n",__func__, __LINE__);
        return AVERROR(ENOMEM);
    }
    av_log(NULL, AV_LOG_VERBOSE,"%s:%d av_packet_alloc end\n",__func__, __LINE__);

    /* frame containing input raw audio */
    frame = av_frame_alloc();
    if (!frame) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d Could not allocate audio frame\n",__func__, __LINE__);
        return AVERROR(ENOMEM);
    }
    av_log(NULL, AV_LOG_VERBOSE,"%s:%d av_frame_alloc end\n",__func__, __LINE__);

    frame->nb_samples     = c->frame_size;
    frame->format         = c->sample_fmt;
    frame->channel_layout = c->channel_layout;

    /* allocate the data buffers */
    ret = av_frame_get_buffer(frame, 0);
    if (ret < 0) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d Could not allocate audio data buffers\n",__func__, __LINE__);
        return AVERROR(ENOMEM);
    }

    context->buffer_size = av_samples_get_buffer_size(NULL, c->channels, frame->nb_samples, (enum AVSampleFormat)context->in_format, 0);

    av_log(NULL, AV_LOG_VERBOSE,"%s:%d av_samples_get_buffer_size:%d\n",__func__, __LINE__, context->buffer_size);
    av_log(NULL, AV_LOG_VERBOSE,"%s:%d c->channels:%d\n",__func__, __LINE__, c->channels);
    av_log(NULL, AV_LOG_VERBOSE,"%s:%d frame->nb_samples:%d\n",__func__, __LINE__, frame->nb_samples);
    av_log(NULL, AV_LOG_VERBOSE,"%s:%d frame->format:%d\n",__func__, __LINE__, frame->format);

    context->pkt = pkt;
    context->c = c;
    context->frame = frame;

    av_log(NULL, AV_LOG_VERBOSE,"%s:%d context->c : %p, c=%p\n",__func__, __LINE__, context->c, c);
    return 0;
}



int AVIAudioInput::initAACHeader(Pcm2aacContext *context)
{
    int profile = 2;   //AAC LC
    int freqIdx = transSamplerate2freqIdx(context->sample_rate);
    if (freqIdx < 0) {
        return AVERROR(ENOMEM);
    }
    aac_adts_header[0] = (char)0xFF;      // 11111111     = syncword
    aac_adts_header[1] = (char)0xF1;      // 1111 1 00 1  = syncword MPEG-2 Layer CRC
    aac_adts_header[2] = (char)(((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
    aac_adts_header[6] = (char)0xFC;

    return 0;
}

int AVIAudioInput::writeAACHeader(uint8_t *outBuffer, AVPacket* pkt)
{
    aac_adts_header[3] = (char)(((chanCfg & 3) << 6) + ((7 + pkt->size) >> 11));
    aac_adts_header[4] = (char)(((7 + pkt->size) & 0x7FF) >> 3);
    aac_adts_header[5] = (char)((((7 + pkt->size) & 7) << 5) + 0x1F);

    memcpy(outBuffer, aac_adts_header, 7);

    return 0;
}

int AVIAudioInput::encodeAudioFrame(AVCodecContext *ctx, AVFrame *frame, AVPacket *avpkt, AACData *outData)
{
    int ret;
    int count = 0;
    int64_t resize = 0;
    int64_t oldsize = 0;
    int64_t offset = 0;

    /* send the frame for encoding */
    ret = avcodec_send_frame(ctx, frame);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR,"%s:%d Error sending the frame to the encoder\n",__func__, __LINE__);
        return ret;
    }
    oldsize = outData->maxlen;
    av_log(NULL, AV_LOG_DEBUG,"%s:%d encode begin, offset=%lld, oldsize=%lld\n",
        __func__, __LINE__, offset, oldsize);

    /* read all the available output packets (in general there may be any
     * number of them */
    while (ret >= 0) {
        ret = avcodec_receive_packet(ctx, avpkt);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
            return ret;
        else if (ret < 0) {
            av_log(NULL, AV_LOG_ERROR,"%s:%d Error encoding audio frame\n",__func__, __LINE__);
            return ret;
        } else {
            av_log(NULL, AV_LOG_DEBUG,"%s:%d continue receive\n",__func__, __LINE__);
        }

        offset = offset + oldsize;
        resize = (7 + avpkt->size) + oldsize;

        av_log(NULL, AV_LOG_DEBUG,"%s:%d offset:%lld, avpkt->size:%d, new resize: %lld\n",
                __func__, __LINE__, offset, avpkt->size, resize);
        av_log(NULL, AV_LOG_DEBUG,"%s:%d realloc newsize: %lld\n",
                __func__, __LINE__, resize + offset);

        //realloc function causes memory leak
        //outData->data = (uint8_t *)realloc(outData->data, resize + offset);
        //memset(outData->data, 0, resize + offset);
        writeAACHeader(outData->data + offset, avpkt);

        av_log(NULL, AV_LOG_DEBUG,"%s:%d avpkt->data=%s\n",
                __func__, __LINE__, avpkt->data);
        memcpy(outData->data + offset + 7, avpkt->data, avpkt->size);

        outData->maxlen += 7 + avpkt->size;
        oldsize = resize;

        //test
        //fwrite(avpkt->data, 1, avpkt->size, fpTemp);

        av_packet_unref(avpkt);
        av_log(NULL, AV_LOG_DEBUG,"%s:%d encode end, offset=%lld, oldsize=%lld, outData->maxlen=%lld\n",
            __func__, __LINE__, offset, oldsize, outData->maxlen);

    }

    av_log(NULL, AV_LOG_VERBOSE,"%s:%d X.\n",__func__, __LINE__);
    return ret;
}




int AVIAudioInput::pcm2aacProcess(Pcm2aacContext *context)
{
    int buffer_size;
    int count = 0;
    int ret = 0;
    int finishFlag = 1;
    FILE *fout;
    char aacname[MAX_STRING_LEN]="";
    AVCodec *codec;
    AVCodecContext *c= NULL;
    AVPacket *pkt;
    AVFrame *frame;
    PcmData *pcmData;

    pkt= context->pkt;
    codec = context->codec;
    c = context->c;
    frame = context->frame;
    buffer_size = context->buffer_size;

    pthread_mutex_lock(&mutex); //Lock
    pcmData = (PcmData *)malloc(sizeof(PcmData));
    pcmData->data = (uint8_t*)av_mallocz(inData->maxlen);
    pcmData->maxlen = inData->maxlen;
    memcpy(pcmData->data, inData->data, inData->maxlen);
    sprintf(aacname, "%s%s%s", AVI_DEMUXER_LOCATION, fileName, ".aac");
    //output aac file to memory
    outData->maxlen = 0;
    memset(outData->data, 0, inData->maxlen);


    av_log(NULL, AV_LOG_VERBOSE,"%s:%d PCM data maxlen = %lld.\n",
            __func__,__LINE__, pcmData->maxlen);
    av_log(NULL, AV_LOG_DEBUG,"%s:%d av_samples_get_buffer_size:%d\n",
            __func__, __LINE__, context->buffer_size);
    av_log(NULL, AV_LOG_DEBUG,"%s:%d c->channels:%d\n",
            __func__, __LINE__, c->channels);
    av_log(NULL, AV_LOG_DEBUG,"%s:%d frame->nb_samples:%d\n",
            __func__, __LINE__, frame->nb_samples);
    av_log(NULL, AV_LOG_DEBUG,"%s:%d frame->format:%d\n",
            __func__, __LINE__, frame->format);


    //Allocate temporary memory space for s16 pcm input
    uint8_t * readBuf = (uint8_t*) av_mallocz(buffer_size);

    while(finishFlag) {
        /* make sure the frame is writable -- makes a copy if the encoder
         * kept a reference internally */
        ret = av_frame_make_writable(frame);
        if (ret < 0)
            return -1;

        if ((count + 1) * buffer_size <= pcmData->maxlen) {
            memcpy(readBuf, pcmData->data + count * buffer_size, buffer_size);
            //av_log(NULL, AV_LOG_DEBUG,"%s:%d read data size:%d\n",
            //        __func__, __LINE__, buffer_size);
        } else {
            memcpy(readBuf, pcmData->data + count * buffer_size, pcmData->maxlen - count * buffer_size);
            //av_log(NULL, AV_LOG_VERBOSE,"%s:%d end read data size:%lld\n",
            //        __func__, __LINE__, inData->maxlen - count * buffer_size);
            finishFlag = 0;
        }

        // Format conversion S16-> FLTP
        SwrContext *swrContext = swr_alloc_set_opts(NULL, frame->channel_layout, AV_SAMPLE_FMT_FLTP, c->sample_rate,
            frame->channel_layout, (enum AVSampleFormat)context->in_format, c->sample_rate, 0, NULL);
        swr_init(swrContext);
        swr_convert(swrContext, frame->data, frame->nb_samples,
            (const uint8_t **)&readBuf, frame->nb_samples);
        swr_free(&swrContext);

        encodeAudioFrame(c, frame, pkt, outData);

        count++;
    }


    outData1->maxlen = outData->maxlen;
    memcpy(outData1->data,outData->data,outData->maxlen);
    av_log(NULL, AV_LOG_DEBUG,"%s:%d outData data addr <%p>, outData maxlen <%lld>.\n",
            __func__, __LINE__, outData->data, outData->maxlen);

    /* flush the encoder */
    //encodeAudioFrame(c, NULL, pkt, outData);
    fout = fopen(aacname,"wb");
    if (!fout) {
        av_log(NULL, AV_LOG_ERROR,
            "%s:%d Could not open %s\n", __func__,__LINE__, aacname);
        return AVERROR(ENOENT);
    }
    fwrite(outData->data, 1, 1, fout);
    fclose(fout);
    /// ---> Notification Audio thread or video thread starts recording or recording video operation
    for (ret = 0; ret < POLLING_MAX; ret++) {
        if (0 != outData1->maxlen) {
            usleep(MAX_SLEEP_TIME*10);//10ms
            continue;
        } else {
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d outData1 maxlen = %lld\n",
                __func__,__LINE__,outData1->maxlen);
            break;
        }
    }


    /* Release PCM data */
    av_free(readBuf);
    av_free(pcmData->data);
    free(pcmData);

    pthread_mutex_unlock(&mutex); //Unlock
    av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d count = %d.\n",__func__,__LINE__, count);

    return Q_TRUE;
}

