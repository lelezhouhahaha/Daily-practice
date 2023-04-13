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

/** AVI_Mediacodec.c - AVI_Mediacodec IL version 1.0.0
 *  The AVI_Mediacodec header file contains the definitions used to define
 *  the public interface of a component.  This header file is intended to
 *  be used by both the application and the component.
 */

#include "vtest_Time.h"
#include "AVI_MediaCodec.h"


/*
FIX: H.264 in some container format (FLV, MP4, MKV etc.) need
"h264_mp4toannexb" bitstream filter (BSF)
  *Add SPS,PPS in front of IDR frame
  *Add start code ("0,0,0,1") in front of NALU
H.264 in some container (MPEG2TS) don't need this BSF.
*/
//'1': Use H.264 Bitstream Filter
#define USE_H264BSF             0

/*
FIX:AAC in some container format (FLV, MP4, MKV etc.) need
"aac_adtstoasc" bitstream filter (BSF)
*/
//'1': Use AAC Bitstream Filter
#define USE_AACBSF              0
#define USE_MOOV_BOX            0
#define STREAM_FRAME_RATE       30     /* 30 images/s */
#define STREAM_FRAME_PTS        3000
#define STREAM_AUDIO_PTS        1024
#define STREAM_FRAME_CAL        6
#define STREAM_AUDIO_CAL        4

extern "C" {
pthread_mutex_t mutex;      //Define lock
AVOutputFormat *ofmt = NULL;
AVFormatContext *ofmt_ctx = NULL;
static int16_t calibATS = 0;
static int16_t calibVTS = 0;
static float video_duration = 1.0/30;
static float audio_duration = ((1024.0/(16 * 2 * 1))*2.0)/1000;
const char * const split = "#";
} // end of extern C

struct av_packet {
    int64_t rpts;
    int64_t rdts;
};
struct av_packet audio_pkt = {0, 0};
struct av_packet video_pkt = {0, 0};

struct buffer_data {
    uint8_t *ptr;   /// Corresponding position pointer in the file
    size_t size;    /// size left in the buffer ---> File current pointer to the end
};

char *video_buf = NULL;
int video_size = NULL;
int buf_flag = NULL;

static int strempty(char *token)
{
    if (token != NULL) {           /// Check if the string is empty
        if (strlen(token) == 0) {   // it is empty string
            av_log(NULL, AV_LOG_DEBUG,
                "%s:%d it is empty string, token length.\n",__func__,__LINE__);
            return -1;
        }else{                     // it is non empty string
            av_log(NULL, AV_LOG_DEBUG,
                "%s:%d it is non empty string, token length.\n",__func__,__LINE__);
        }
    }else{                         // it is non empty string
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d [#] not equal to empty, token length.\n",__func__,__LINE__);
    }
    return 0;
}

//Focus, custom buffer data should be defined here outside
static int read_packet(void *opaque, uint8_t *buf, int buf_size)
{
    struct buffer_data *bd = (struct buffer_data *)opaque;
    buf_size = FFMIN(buf_size, bd->size);

    if (!buf_size)
        return AVERROR_EOF;
    av_log(NULL, AV_LOG_DEBUG,"ptr:%p size:%zu\n", bd->ptr, bd->size);

    /* copy internal buffer data to buf */
    memcpy(buf, bd->ptr, buf_size);
    bd->ptr  += buf_size;
    bd->size -= buf_size;

    return buf_size;
}

static void log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt, const char *tag)
{
    AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;

    av_log(NULL, AV_LOG_DEBUG,
    "%s: pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d\n",
       tag,
       av_ts2str(pkt->pts), av_ts2timestr(pkt->pts, time_base),
       av_ts2str(pkt->dts), av_ts2timestr(pkt->dts, time_base),
       av_ts2str(pkt->duration), av_ts2timestr(pkt->duration, time_base),
       pkt->stream_index);
}

static int metadata_creation_time(AVDictionary **metadata)
{
    char timeBuf[128];
    time_t current_time;
    struct tm * timeinfo;

    memset(timeBuf, 0, sizeof(timeBuf));
    time (&current_time);
    timeinfo = localtime (&current_time);
    timeBuf[0] = '\0';
    if (timeinfo) {
        if (!strftime(timeBuf, sizeof(timeBuf), "%Y-%m-%d %H:%M:%S", timeinfo))
            return AVERROR_EXTERNAL;
        //av_strlcatf(timeBuf, sizeof(timeBuf), ".%06dZ", (int)(timestamp % 1000000));
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d timestamp=[%s].\n", __func__,__LINE__,timeBuf);
        return av_dict_set(metadata, "creation_time", timeBuf, 0);
    } else {
        return AVERROR_EXTERNAL;
    }
}

/*===========================================================================
 * FUNCTION   : sync_demuxer
 *
 * DESCRIPTION: Function to open older hal version implementation
 *
 * PARAMETERS :
 *   @arg : ptr to struct storing camera hardware device info
 *
 * RETURN     : 0  -- success
 *              none-zero failure code
 *==========================================================================*/

int write_packet1(const char *in_filename_v, AACData *aacData, const char *out_filename, int frame_rate, bool flags, int count)
{
    int ret, i;
    AVPacket pkt;
    int m = 0, n = 0;
    struct av_packet apkt = {0, 0};
    struct av_packet vpkt = {0, 0};
    static int videoindex_v=-1,videoindex_out=-1;
    static int audioindex_a=-1,audioindex_out=-1;
    int frame_index=0;
    int64_t cur_pts_v=0,cur_pts_a=0;

    //Input AVFormatContext and Output AVFormatContext
    AVFormatContext *ifmt_ctx_v = NULL;
    AVIOContext *avio_ctx_v = NULL;
    uint8_t *buffer_v = NULL, *avio_ctx_buffer_v = NULL;
    size_t buffer_size_v, avio_ctx_buffer_size_v = 4096;
    struct buffer_data bd_v = { 0 };

    AVFormatContext *ifmt_ctx_a = NULL;
    AVIOContext *avio_ctx_a = NULL;
    uint8_t *avio_ctx_buffer_a = NULL;
    size_t avio_ctx_buffer_size_a = 4096;
    struct buffer_data bd_a = { 0 };

    ///---------------------------------------------------------------
    ///---------------------------------------------------------------
    ///-- Video data stream input ------------------------------------
    ///---------------------------------------------------------------
    ///---------------------------------------------------------------
#if 1
    /* slurp file content into buffer */
/*
    ret = av_file_map(in_filename_v, &buffer_v, &buffer_size_v, 0, NULL);
    if (ret < 0)
        goto end;
*/
    /* fill opaque structure used by the AVIOContext read callback */
    bd_v.ptr  = (uint8_t *)in_filename_v;
    bd_v.size = video_size;

    if (!(ifmt_ctx_v = avformat_alloc_context())) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    avio_ctx_buffer_v = (uint8_t*)av_malloc(avio_ctx_buffer_size_v);
    if (!avio_ctx_buffer_v) {
        ret = AVERROR(ENOMEM);
        goto end;
    }
    avio_ctx_v = avio_alloc_context(avio_ctx_buffer_v, avio_ctx_buffer_size_v,
                                  0, &bd_v, &read_packet, NULL, NULL);
    if (!avio_ctx_v) {
        ret = AVERROR(ENOMEM);
        goto end;
    }
    ifmt_ctx_v->pb = avio_ctx_v;

    //Input
    if ((ret = avformat_open_input(&ifmt_ctx_v, NULL, NULL, NULL)) < 0) {
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d Could not open input video file.\n", __func__,__LINE__);
        goto end;
    }
    if ((ret = avformat_find_stream_info(ifmt_ctx_v, 0)) < 0) {
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d Failed to retrieve input stream information\n", __func__,__LINE__);
        goto end;
    }
#endif

    ///---------------------------------------------------------------
    ///---------------------------------------------------------------
    ///-- Audio data stream input ------------------------------------
    ///---------------------------------------------------------------
    ///---------------------------------------------------------------
#if 1
    /* slurp file content into buffer */
    //ret = av_file_map(input_filename, &buffer, &buffer_size, 0, NULL);
    //if (ret < 0)
    //    goto end;

    /* fill opaque structure used by the AVIOContext read callback */
    bd_a.ptr  = aacData->data;
    bd_a.size = aacData->maxlen;

    if (!(ifmt_ctx_a = avformat_alloc_context())) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    avio_ctx_buffer_a = (uint8_t*)av_malloc(avio_ctx_buffer_size_a);
    if (!avio_ctx_buffer_a) {
        ret = AVERROR(ENOMEM);
        goto end;
    }
    avio_ctx_a = avio_alloc_context(avio_ctx_buffer_a, avio_ctx_buffer_size_a,
                                  0, &bd_a, &read_packet, NULL, NULL);
    if (!avio_ctx_a) {
        ret = AVERROR(ENOMEM);
        goto end;
    }
    ifmt_ctx_a->pb = avio_ctx_a;

    if ((ret = avformat_open_input(&ifmt_ctx_a, NULL, NULL, NULL)) < 0) {
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d Could not open input audio file.\n", __func__,__LINE__);
        goto end;
    }
    if ((ret = avformat_find_stream_info(ifmt_ctx_a, 0)) < 0) {
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d Failed to retrieve input stream information\n", __func__,__LINE__);
        goto end;
    }
#endif
    //av_log(NULL, AV_LOG_DEBUG,
           // "%s:%d ===========Input Information==========\n", __func__,__LINE__);
    //av_dump_format(ifmt_ctx_v, 0, in_filename_v, 0);
    for (i = 0; i < (int)ifmt_ctx_v->nb_streams; i++) {
        int fps = 0;
        AVStream *in_stream = ifmt_ctx_v->streams[i];

        /* timebase: This is the fundamental unit of time (in seconds) in terms
         * of which frame timestamps are represented. For fixed-fps content,
         * timebase should be 1/framerate and timestamp increments should be
         * identical to 1. */
        in_stream->time_base = (AVRational){ 1, frame_rate };
        in_stream->avg_frame_rate.num = frame_rate;
        in_stream->avg_frame_rate.den = 1;
        in_stream->r_frame_rate.num = frame_rate;
        in_stream->r_frame_rate.den = 1;

        fps = (int)av_q2d(in_stream->avg_frame_rate);
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d fps = %d, width = %d, height = %d, codecid = %d, format = %d\n",
             __func__,__LINE__,fps,
             in_stream->codecpar->width,
             in_stream->codecpar->height,
             in_stream->codecpar->codec_id,
             in_stream->codecpar->format);
    }
    //av_dump_format(ifmt_ctx_a, 0, in_filename_a, 0);
    //av_log(NULL, AV_LOG_DEBUG,
    //        "%s:%d ======================================\n\n\n", __func__,__LINE__);

    ///---------------------------------------------------------------
    ///---------------------------------------------------------------
    ///-- MP4 data stream output -------------------------------------
    ///---------------------------------------------------------------
    ///---------------------------------------------------------------
    /// The packaged mp4 is merged into the same output mp4 file
    /// so the output mp4 file does not need to be initialized every time.
    if(ofmt_ctx==NULL)
    {
        //Output
        avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
        if (!ofmt_ctx) {
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d Could not create output context\n", __func__,__LINE__);
            ret = AVERROR_UNKNOWN;
            goto end;
        }
        ofmt = ofmt_ctx->oformat;

        for (i = 0; i < (int)ifmt_ctx_v->nb_streams; i++) {
            int fps = 0;
            AVStream *out_stream;
            AVStream *in_stream = ifmt_ctx_v->streams[i];
            AVCodecParameters *in_codecpar = in_stream->codecpar;

            //Create output AVStream according to input AVStream
            if (in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                out_stream = avformat_new_stream(ofmt_ctx, NULL);
                videoindex_v = i;
                if (!out_stream) {
                    av_log(NULL, AV_LOG_VERBOSE,
                        "%s:%d Failed allocating output stream\n", __func__,__LINE__);
                    ret = AVERROR_UNKNOWN;
                    goto end;
                }
                videoindex_out=out_stream->index;
                //Copy the settings of AVCodecContext
                if (avcodec_parameters_copy(out_stream->codecpar, in_codecpar) < 0) {
                    av_log(NULL, AV_LOG_VERBOSE,
                        "%s:%d Failed to copy context from input to output stream codec context\n", __func__,__LINE__);
                    goto end;
                }
                out_stream->codecpar->codec_tag = 0;
                //if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
                //    out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
                break;
            }
        }

        av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d videoindex_out = %d \n", __func__,__LINE__,videoindex_out);

        for (i = 0; i < (int)ifmt_ctx_a->nb_streams; i++) {
            AVStream *out_stream;
            AVStream *in_stream = ifmt_ctx_a->streams[i];
            AVCodecParameters *in_codecpar = in_stream->codecpar;

            //Create output AVStream according to input AVStream
            if(in_codecpar->codec_type==AVMEDIA_TYPE_AUDIO){
                out_stream = avformat_new_stream(ofmt_ctx, NULL);
                audioindex_a = i;
                if (!out_stream) {
                    av_log(NULL, AV_LOG_VERBOSE,
                        "%s:%d Failed allocating output stream\n", __func__,__LINE__);
                    ret = AVERROR_UNKNOWN;
                    goto end;
                }
                audioindex_out=out_stream->index;
                //Copy the settings of AVCodecContext
                if (avcodec_parameters_copy(out_stream->codecpar, in_codecpar) < 0) {
                    av_log(NULL, AV_LOG_VERBOSE,
                        "%s:%d Failed to copy context from input to output stream codec context\n", __func__,__LINE__);
                    goto end;
                }
                out_stream->codecpar->codec_tag = 0;
                //if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
                //    out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

                break;
            }
        }

        av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d audioindex_out = %d \n", __func__,__LINE__,audioindex_out);
        /// The packaged mp4 is merged into the same output mp4 file
        /// so the output mp4 file does not need to be initialized every time.
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d ==========Output Information==========\n", __func__,__LINE__);
        av_dump_format(ofmt_ctx, 0, out_filename, 1);
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d ======================================\n\n\n", __func__,__LINE__);
        //Open output file
        if (!(ofmt->flags & AVFMT_NOFILE)) {
            if (avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE) < 0) {
                av_log(NULL, AV_LOG_VERBOSE,
                    "%s:%d Could not open output file '%s'", __func__,__LINE__, out_filename);
                goto end;
            }
        }

#if USE_MOOV_BOX
        AVDictionary *dict = NULL;
        metadata_creation_time(&ofmt_ctx->metadata);

        // Set moov pre-option (no such setting in source)
        av_dict_set(&dict, "movflags","rtphint+faststart",0);

        if (av_dict_count(dict) > 0) {
            av_log(NULL, AV_LOG_VERBOSE,"%s:%d Using muxer settings:",__func__, __LINE__);
            AVDictionaryEntry *entry = NULL;
            while ((entry = av_dict_get(dict, "", entry,
                AV_DICT_IGNORE_SUFFIX)))
                av_log(NULL, AV_LOG_VERBOSE,"\n\t%s=%s",entry->key, entry->value);
        }

        //Write file header
        if (avformat_write_header(ofmt_ctx, &dict) < 0) {
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d Error occurred when opening output file\n", __func__,__LINE__);
            av_dict_free(&dict);
            goto end;
        }
        av_dict_free(&dict);
#else
        //Write file header
        if (avformat_write_header(ofmt_ctx, NULL) < 0) {
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d Error occurred when opening output file\n", __func__,__LINE__);
            goto end;
        }
#endif
        //FIX
#if USE_H264BSF
        AVBitStreamFilterContext* h264bsfc =  av_bitstream_filter_init("h264_mp4toannexb");
#endif
#if USE_AACBSF
        AVBitStreamFilterContext* aacbsfc =  av_bitstream_filter_init("aac_adtstoasc");
#endif
    }

    while (1)
    {
        AVFormatContext *ifmt_ctx;
        int stream_index=0;
        AVStream *in_stream, *out_stream;

        //Get an AVPacket
        if(av_compare_ts(cur_pts_v, ifmt_ctx_v->streams[videoindex_v]->time_base,
                         cur_pts_a, ifmt_ctx_a->streams[audioindex_a]->time_base) <= 0) {
            ifmt_ctx=ifmt_ctx_v;
            stream_index=videoindex_out;

            if(av_read_frame(ifmt_ctx, &pkt) >= 0){
                do{
                    in_stream  = ifmt_ctx->streams[pkt.stream_index];
                    out_stream = ofmt_ctx->streams[stream_index];

                    if(pkt.stream_index==videoindex_v){
                        //FIX: No PTS (Example: Raw H.264)
                        //Simple Write PTS
                        if(pkt.pts==AV_NOPTS_VALUE){
                            //Write PTS
                            AVRational time_base1=in_stream->time_base;
                            //Duration between 2 frames (us)
                            int64_t calc_duration=(double)AV_TIME_BASE/av_q2d(in_stream->r_frame_rate);
                            //Parameters
                            pkt.pts=(double)(frame_index*calc_duration)/(double)(av_q2d(time_base1)*AV_TIME_BASE) + 0.5;
                            pkt.dts=pkt.pts;
                            pkt.duration=(double)calc_duration/(double)(av_q2d(time_base1)*AV_TIME_BASE);
                            frame_index++;
                            av_log(NULL, AV_LOG_DEBUG,
                                "%s:%d Write 1 Packet. frame_index:%d\tsize:%5d\tpts:%lld\n", __func__,__LINE__,frame_index,pkt.size,pkt.pts);
                        }
                        cur_pts_v=pkt.pts;
                        break;
                    }

                }while(av_read_frame(ifmt_ctx, &pkt) >= 0);
            }else{
                int64_t vvTS = vpkt.rpts - count * (BUFFER_SPLIT_LENGTH * STREAM_FRAME_PTS)/video_duration;
                calibVTS = vvTS/(BUFFER_SPLIT_LENGTH/video_duration);
                video_pkt.rpts = vpkt.rpts;
                video_pkt.rdts = vpkt.rdts;
                av_log(NULL, AV_LOG_DEBUG,
                    "%s::%d ####### calibVTS=%d.\n",__func__, __LINE__,calibVTS);

                int64_t aaTS = apkt.rpts - count * (BUFFER_SPLIT_LENGTH * STREAM_AUDIO_PTS)/audio_duration;
                calibATS = aaTS/(BUFFER_SPLIT_LENGTH/audio_duration);
                audio_pkt.rpts = apkt.rpts;
                audio_pkt.rdts = apkt.rdts;
                av_log(NULL, AV_LOG_DEBUG,
                    "%s::%d ####### calibATS=%d.\n",__func__, __LINE__,calibATS);
                if(pkt.buf != NULL)
                {
                    av_log(NULL, AV_LOG_ERROR,
                        "%s::%d ####### pkt.buf %p \n",__func__, __LINE__,pkt.buf);
                    av_packet_unref(&pkt);
                }
                if(flags==false)
                    goto end;
                else
                    break;
            }
        } else {
            ifmt_ctx=ifmt_ctx_a;
            stream_index=audioindex_out;
            if(av_read_frame(ifmt_ctx, &pkt) >= 0){
                do{
                    in_stream  = ifmt_ctx->streams[pkt.stream_index];
                    out_stream = ofmt_ctx->streams[stream_index];

                    if(pkt.stream_index==audioindex_a){
                        //FIX: No PTS
                        //Simple Write PTS
                        if(pkt.pts==AV_NOPTS_VALUE){
                            //Write PTS
                            AVRational time_base1=in_stream->time_base;
                            //Duration between 2 frames (us)
                            int64_t calc_duration=(double)AV_TIME_BASE/av_q2d(in_stream->r_frame_rate);
                            //Parameters
                            pkt.pts=(double)(frame_index*calc_duration)/(double)(av_q2d(time_base1)*AV_TIME_BASE);
                            pkt.dts=pkt.pts;
                            pkt.duration=(double)calc_duration/(double)(av_q2d(time_base1)*AV_TIME_BASE);
                            frame_index++;
                        }
                        cur_pts_a=pkt.pts;
                        break;
                    }
                }while(av_read_frame(ifmt_ctx, &pkt) >= 0);
            }else{
                int64_t vvTS = vpkt.rpts - count * (BUFFER_SPLIT_LENGTH * STREAM_FRAME_PTS)/video_duration;
                calibVTS = vvTS/(BUFFER_SPLIT_LENGTH/video_duration);
                video_pkt.rpts = vpkt.rpts;
                video_pkt.rdts = vpkt.rdts;
                av_log(NULL, AV_LOG_DEBUG,
                    "%s::%d ####### calibVTS=%d.\n",__func__, __LINE__,calibVTS);

                int64_t aaTS = apkt.rpts - count * (BUFFER_SPLIT_LENGTH * STREAM_AUDIO_PTS)/audio_duration;
                calibATS = aaTS/(BUFFER_SPLIT_LENGTH/audio_duration);
                audio_pkt.rpts = apkt.rpts;
                audio_pkt.rdts = apkt.rdts;
                av_log(NULL, AV_LOG_DEBUG,
                    "%s::%d ####### calibATS=%d.\n",__func__, __LINE__,calibATS);
                if(pkt.buf != NULL)
                {
                    av_log(NULL, AV_LOG_ERROR,
                        "%s::%d ####### calibATS=%d. pkt.buf %p\n",__func__, __LINE__,calibATS,pkt.buf);
                    av_packet_unref(&pkt);
                }
                if(flags==false)
                    goto end;
                else
                    break;
            }

        }

        //FIX:Bitstream Filter
#if USE_H264BSF
        av_bitstream_filter_filter(h264bsfc, in_stream->codec, NULL, &pkt.data, &pkt.size, pkt.data, pkt.size, 0);
#endif
#if USE_AACBSF
        av_bitstream_filter_filter(aacbsfc, out_stream->codec, NULL, &pkt.data, &pkt.size, pkt.data, pkt.size, 0);
#endif

        //Convert PTS/DTS
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base, (AVRounding)(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base, (AVRounding)(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;
        pkt.stream_index=stream_index;
        if(count>AVI_DIVNEM_FIRST)
        {
            if(pkt.stream_index == videoindex_out)
            {
                m++;
                if(flags==false) {
                    pkt.pts = pkt.pts + video_pkt.rpts + STREAM_FRAME_PTS - m*calibVTS;
                    pkt.dts = pkt.dts + video_pkt.rdts + STREAM_FRAME_PTS - m*calibVTS;
                } else {
                    pkt.pts = pkt.pts + video_pkt.rpts + STREAM_FRAME_PTS + STREAM_FRAME_CAL*m;
                    pkt.dts = pkt.dts + video_pkt.rdts + STREAM_FRAME_PTS + STREAM_FRAME_CAL*m;
                }
                av_log(NULL, AV_LOG_DEBUG,
                        "%s:%d ####### video_pkt.rpts = [%d].\n",__func__, __LINE__,(STREAM_FRAME_PTS - m*calibVTS));
            }
            else if(pkt.stream_index == audioindex_out)
            {
                n++;
                if(flags==false)
                {
                    pkt.pts = pkt.pts + audio_pkt.rpts + STREAM_AUDIO_PTS - n*calibATS;
                    pkt.dts = pkt.dts + audio_pkt.rdts + STREAM_AUDIO_PTS - n*calibATS;
                }else{
                    pkt.pts = pkt.pts + audio_pkt.rpts + STREAM_AUDIO_PTS + STREAM_AUDIO_CAL*n;
                    pkt.dts = pkt.dts + audio_pkt.rdts + STREAM_AUDIO_PTS + STREAM_AUDIO_CAL*n;
                }
                av_log(NULL, AV_LOG_DEBUG,
                        "%s::%d ####### audio_pkt.rpts = [%d].\n",__func__, __LINE__,(STREAM_AUDIO_PTS - n*calibATS));
            }
        }
        if(pkt.stream_index == videoindex_out)
        {
            vpkt.rpts = pkt.pts;
            vpkt.rdts = pkt.dts;
        }
        else if(pkt.stream_index == audioindex_out)
        {

            apkt.rpts = pkt.pts;
            apkt.rdts = pkt.dts;
        }
        log_packet(ofmt_ctx, &pkt, "out");
        //av_log(NULL, AV_LOG_VERBOSE,
        //    "%s:%d Write 1 Packet. size:%5d\tpts:%lld\n", __func__,__LINE__,pkt.size,pkt.pts);
        //Write
        if (av_interleaved_write_frame(ofmt_ctx, &pkt) < 0) {
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d Error muxing packet\n", __func__,__LINE__);
            break;
        }
        if(pkt.buf != NULL)
        {
            av_log(NULL, AV_LOG_ERROR,
                "%s:%d muxing packet pkt.buf %p\n", __func__,__LINE__,pkt.buf);
            av_packet_unref(&pkt);
        }

    }
    //Write file trailer
    av_write_trailer(ofmt_ctx);

#if USE_H264BSF
    av_bitstream_filter_close(h264bsfc);
#endif
#if USE_AACBSF
    av_bitstream_filter_close(aacbsfc);
#endif

end:
    av_log(NULL, AV_LOG_DEBUG,
        "%s::%d Number of times to loop the output: %d.\n",__func__, __LINE__,count);
    avformat_close_input(&ifmt_ctx_v);
    /* note: the internal buffer could have changed, and be != avio_ctx_buffer */
    if (avio_ctx_v) {
        av_freep(&avio_ctx_v->buffer);
        av_freep(&avio_ctx_v);
    }
    //< !-- modify for tastk 13872, crash when stop record
    //av_file_unmap(buffer_v, buffer_size_v);
    //modify for tastk 13872, crash when stop record --! >
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR,
            "%s::%d Video Error occurred: %s\n",__func__, __LINE__, av_err2str(ret));
        return AVERROR(ENOMEM);
    }

    avformat_close_input(&ifmt_ctx_a);
    /* note: the internal buffer could have changed, and be != avio_ctx_buffer */
    if (avio_ctx_a) {
        av_freep(&avio_ctx_a->buffer);
        av_freep(&avio_ctx_a);
    }
    //av_file_unmap(aacData->data, aacData->maxlen);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR,
            "%s::%d Audio Error occurred: %s\n",__func__, __LINE__, av_err2str(ret));
        return AVERROR(ENOMEM);
    }

    if(flags==false)
        return STREAM_FRAME_RATE;
    av_log(NULL, AV_LOG_VERBOSE,
        "%s::%d MP4 file sync succeeded.\n",__func__, __LINE__);

    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_close(ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);
    ofmt_ctx = NULL;
    if (ret < 0 && ret != AVERROR_EOF) {
        av_log(NULL, AV_LOG_VERBOSE, "%s:%d Error occurred: %s.\n",
              __func__, __LINE__, av_err2str(ret));
        return AVERROR(ENOMEM);
    }
    return 0;
}


void *sync_demuxer(void * data)
{
    int rc;
    int retry;
    bool flags;
    char aacFileName[MAX_STRING_LEN]="";
    char vFileName[MAX_STRING_LEN]="";
    static char aviDemuxerName[MAX_FIFO_LEN]={0};
    static char aviDemuxerNameCB[MAX_FIFO_LEN]={0};
    AVIOutputProperty property;


    AVIMediaCodec *pThis = (AVIMediaCodec*)data;
    memcpy(&property, &pThis->demuxerProperty, sizeof(AVIOutputProperty));

    av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d m_doneName=[%s]\n", __func__,__LINE__, property.m_doneName);
    av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d m_donePath=[%s]\n", __func__,__LINE__, property.m_donePath);
    /// ---> Notification Audio thread or video thread starts recording or recording video operation
    for (retry = 0; retry < POLLING_MAX; retry++) {
        rc = strempty(property.m_doneName);
        if (rc < 0) {
            usleep(MAX_SLEEP_TIME*10);
            continue;
        } else {
            av_log(NULL, AV_LOG_DEBUG,
                "%s:%d %s file name is valid!\n",
                __func__,__LINE__,property.m_doneName);
            break;
        }
    }

    pthread_mutex_lock(&mutex); //Lock
    /// Audio Name Check
    sprintf(aacFileName, "%s%s%s",AVI_DEMUXER_LOCATION, property.m_doneName,".aac");
    for (retry = 0; retry < POLLING_MAX*10; retry++) {
        rc = access(aacFileName, F_OK);
        if (rc < 0) {
            usleep(MAX_SLEEP_TIME*10);
            continue;
        } else {
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d %s File EXISIT!\n",
                __func__,__LINE__,aacFileName);
            break;
        }
    }
    pThis->aacData->maxlen = pThis->m_audioInput->outData1->maxlen;
    memcpy(pThis->aacData->data,
        pThis->m_audioInput->outData1->data,
        pThis->m_audioInput->outData1->maxlen);
    av_log(NULL, AV_LOG_VERBOSE,"%s:%d outData data addr <%p>, outData maxlen <%lld>.\n",
            __func__, __LINE__, pThis->m_audioInput->outData->data, pThis->m_audioInput->outData1->maxlen);
    pThis->m_audioInput->outData1->maxlen = 0;
#if 0
    /// H265/H264 Check
    if(property.m_startFormat == VIDEO_COMPRESSION_FORMAT_H265)
        sprintf(vFileName, "%s%s%s",AVI_DEMUXER_LOCATION, property.m_doneName, ".265");
    else
        sprintf(vFileName, "%s%s%s",AVI_DEMUXER_LOCATION, property.m_doneName, ".264");

    for (retry = 0; retry < POLLING_MAX*10; retry++) {
        rc = access(vFileName, F_OK);
        if (rc < 0) {
            usleep(MAX_SLEEP_TIME*10);//10ms
            continue;
        } else {
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d %s File EXISIT!\n",
                __func__,__LINE__,vFileName);
            break;
        }
    }
#endif
    if(!property.aviStatus)
        flags=true;
    else if(property.dCount==property.m_sLen)
        flags=true;
    else
        flags=false;

    if(property.dCount==AVI_DIVNEM_FIRST){
        if(!strcmp(property.m_startName,property.m_doneName)){
            sprintf(aviDemuxerName, "%s%s%s",property.m_donePath,property.m_doneName,".MP4");
            sprintf(aviDemuxerNameCB, "%s", property.m_doneName);
        } else {
            sprintf(aviDemuxerName, "%s%s%s",property.m_startPath,property.m_startName,".MP4");
            sprintf(aviDemuxerNameCB, "%s", property.m_startName);
        }
    }

    //av_log(NULL, AV_LOG_VERBOSE, "%s:%d test whether file read and writte [%s].\n",__func__,__LINE__, aviDemuxerName);
/*
    FILE *fout;
    rc = access(aviDemuxerName, F_OK);
    if (rc < 0) {
        fout = fopen(aviDemuxerName,"wb");
        if (!fout) {
            av_log(NULL, AV_LOG_ERROR, "%s:%d Could not open %s\n",
                      __func__,__LINE__, aviDemuxerName);
            return NULL;
        }
        fwrite(aviDemuxerName, sizeof(aviDemuxerName), 1, fout);
    } else {
        fout = fopen(aviDemuxerName,"rb");
        if (!fout) {
            av_log(NULL, AV_LOG_ERROR, "%s:%d Could not open %s\n",
                     __func__,__LINE__, aviDemuxerName);
            return NULL;
        }
    }
    av_log(NULL, AV_LOG_VERBOSE, "%s:%d AVI Demuxer Name is %s\n",
            __func__,__LINE__,aviDemuxerName);
    fclose(fout);
*/

    write_packet1(video_buf, pThis->aacData, aviDemuxerName, (int)(property.m_doneFrameRate + 0.5), flags, property.dCount);
    av_log(NULL, AV_LOG_VERBOSE, "%s:%d FrameRated=[ %f ], Count=[ %d ], flags=[ %d ] aviDemuxerName=[ %s ].\n",
        __func__,__LINE__,property.m_doneFrameRate, property.dCount, flags, aviDemuxerName);

    ///Delete temporary files
    usleep(MAX_SLEEP_TIME*10);
#ifdef AVI_MEDIACODEC_TEMPFILE
    if(remove(aacFileName)==0)
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d Deleted AAC File Successfully.\n", __func__,__LINE__);
    else
        av_log(NULL, AV_LOG_ERROR,
            "%s:%d Deleted AAC File Failed.\n", __func__,__LINE__);
/*
    if(remove(vFileName)==0)
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d Deleted Video File Successfully.\n", __func__,__LINE__);
    else
        av_log(NULL, AV_LOG_ERROR,
            "%s:%d Deleted Video File Failed.\n", __func__,__LINE__);
*/
#endif // #ifdef AVI_MEDIACODEC_TEMPFILE

    //Zhazhao's video file name callback
    if(video_buf !=NULL)
    {
	  free(video_buf);
	  video_buf = NULL;
	  buf_flag = 7;
    }
    if (pThis->notify && (flags==true)) {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d [DBG] Video End FileName callback.\n",__func__, __LINE__);
        (*pThis->notify->venc_filename_cb)(aviDemuxerNameCB);
    }
    pthread_mutex_unlock(&mutex); //Unlock
    av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d EXIT.\n", __func__,__LINE__);
    return NULL;
}


/*===========================================================================
 * FUNCTION   : venc_process
 *
 * DESCRIPTION: Attempt to turn on or off the torch mode of the flash unit.
 *
 * PARAMETERS :
 *   @data        : Indicates whether to turn the flash on or off
 *
 * RETURN     : 0  -- success
 *              none-zero failure code
 *==========================================================================*/
void *sync_process1(void *data)
{
    int value;
    int retry = 0;
    //int m_aFd = -1;
    OMX_TICKS start_t;
    OMX_TICKS finish_t;
    OMX_TICKS durationPrevious = 0;
    OMX_TICKS timeDiff = 0;
    //char buff[MAX_STRING_LEN];

    AVIMediaCodec *pThis = (AVIMediaCodec*)data;
    start_t = vtest::Time::GetTimeMicrosec();

    //How to determine whether this is the stopRecord or the completion of the segmentation.
    while(pThis->nKillThread != KILL_THREAD_EXIT)
    {
        int sLen = pThis->record.video_split_length/BUFFER_SPLIT_LENGTH;
        pThis->syncProperty.dCount = 0;
        pThis->syncProperty.m_sLen = sLen;
        pThis->syncProperty.aviStatus = true;
        //Zhazhao's video file name callback
        if (pThis->notify && !pThis->m_preRecord) {
            (*pThis->notify->vtest_swvenc_diag)(pThis->syncProperty.m_startPath,pThis->syncProperty.m_startName);
            (*pThis->notify->venc_filename_cb)(pThis->syncProperty.m_startName);
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d [DBG] Video Start FileName callback = [%s].\n",__func__, __LINE__,pThis->syncProperty.m_startName);
        }

        for (int i = 0; i < sLen; ++i) {
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d Video Split Length [%d] Start Loop [ %d ].\n",__func__, __LINE__,sLen,i);
            /// ---> Start Audio && Video
            /// ---> Notification Audio thread or video thread starts recording
            //pThis->get_video_start(&pThis->syncProperty);
            pThis->m_audioInput->start(&pThis->record, false);

            //The pre-recorded operation must be performed in this for loop.
            //If the recording starts after 30s, continue the for loop.
            //If the pre-recorded 30s, change 30s to 29s, then delete the first audio to continue recording.
            for (OMX_TICKS durationCurrent = 0; durationCurrent < (BUFFER_SPLIT_LENGTH*1000); ) {
                // select the stream to encode
                // Audio encoding to get the beat of the audio
                value = pThis->get_audio_frame();
                // Video encoding, get video beats
                ///const uchar *rgb = vt->getRGB();

                finish_t = vtest::Time::GetTimeMicrosec();
                durationCurrent = ((finish_t - start_t) / 1000) - i*(BUFFER_SPLIT_LENGTH*1000) - (BUFFER_SPLIT_LENGTH*1000)*sLen*retry;   // convert to millis
                av_log(NULL, AV_LOG_DEBUG,
                    "%s:%d Time difference [ %lld ].\n",__func__, __LINE__, durationCurrent);

                if(pThis->m_record)
                {
                    av_log(NULL, AV_LOG_DEBUG,
                        "%s:%d m_record true.\n",__func__,__LINE__);
                    continue;
                }
                if(pThis->m_preRecord)
                {
                    bool rc = ((durationCurrent + value) > (BUFFER_SPLIT_LENGTH*1000))? true : false;
                    if (rc) {
                        pThis->m_audioInput->bytesDelete(true);
                        av_log(NULL, AV_LOG_DEBUG,
                            "%s:%d Pre-recorded time difference is greater than 30s = [ %lld ].\n",__func__,__LINE__,durationCurrent);
                        durationCurrent = (BUFFER_SPLIT_LENGTH*1000) - value;
                        start_t = vtest::Time::GetTimeMicrosec() - (BUFFER_SPLIT_LENGTH*1000)*1000;
                    } else {
                        av_log(NULL, AV_LOG_DEBUG,
                            "%s:%d Pre-recorded time = [ %lld ].\n",__func__,__LINE__,durationCurrent);
                    }
                    continue;
                }
                if(pThis->m_abandon)
                {
                    pThis->get_video_done(&pThis->syncProperty);
                    sprintf(pThis->syncProperty.m_donePath,"%s","");
                    sprintf(pThis->syncProperty.m_doneName,"%s","");
                    pThis->m_audioInput->stop(&pThis->syncProperty,true);//End the Video
                    //Zhazhao's video file name callback
                    if(video_buf !=NULL) {
                        av_log(NULL, AV_LOG_VERBOSE,
                        "[Debug]%s:%d free video_buf %p when stop prerecord.\n",__func__, __LINE__, video_buf);
	                   free(video_buf);
	                   video_buf = NULL;
	                   buf_flag = 7;
                    }
                    av_log(NULL, AV_LOG_VERBOSE,
                        "%s:%d Discard Prerecorded Files.\n",__func__,__LINE__);
                    return ((void *)0);
                }
                goto end;
            }

            ///---->Segmental completion.
            pThis->syncProperty.dCount++;
            av_log(NULL, AV_LOG_DEBUG,
                "%s:%d Record Retry[%d]=%d.\n",__func__,__LINE__, retry, pThis->syncProperty.dCount);
            // Stop Audio && Stop Video
            // This function is also blocked
            pThis->get_video_done(&pThis->syncProperty);
            pThis->m_audioInput->stop(&pThis->syncProperty,false);
            if(pThis->m_record || pThis->m_preRecord)
                pThis->mediaDemuxer(&pThis->syncProperty);
            else
                goto enddemux;
        }
        retry++;
    }

end:
    pThis->syncProperty.dCount++;
    pThis->get_video_done(&pThis->syncProperty);
    pThis->m_audioInput->stop(&pThis->syncProperty,true); //End the Video
enddemux:
    pThis->syncProperty.aviStatus = false;
    pThis->mediaDemuxer(&pThis->syncProperty);
    return ((void *)0);
}


/*===========================================================================
 * FUNCTION   : venc_notify_start
 *
 * DESCRIPTION: static function to query number of cameras detected
 *
 * PARAMETERS : none
 *
 * RETURN     : number of cameras detected
 *==========================================================================*/
void venc_notify_start(char *audioPath, char *audioFileName, int m_startFormat)
{
    int n;
    int ret;
    static int m_sFd=-1;
    char eState[MAX_FIFO_LEN]={0};
    //const char * const split = "\t";

    ///Create a video FIFO pipeline
    if (access(vsenc_fifo, F_OK) == -1)
    {
        // The pipeline file does not exist
        // Create a named pipe
        ret = mkfifo(vsenc_fifo, 0666);
        if (ret != 0)
        {
            av_log(NULL, AV_LOG_ERROR,
                "%s:%d Could not create fifo %s.\n",__func__, __LINE__, vsenc_fifo);
            return;
        }
    }
    if(m_sFd < 0)
    {
        // Open a vsenc FIFO with read and write
        m_sFd = open(vsenc_fifo, O_WRONLY | O_NONBLOCK);
        if(m_sFd < 0)
        {
            close(m_sFd);
            //av_log(NULL, AV_LOG_ERROR,
            //    "%s:%d Open vsenc FIFO[%d] Failed!\n",__func__, __LINE__,m_sFd);
            return;
        }
    }

    n = sprintf(eState,"%s#%s#%d", audioPath, audioFileName, m_startFormat);
    av_log(NULL, AV_LOG_VERBOSE,
        "%s:%d eState= [%s].\n",__func__, __LINE__, eState);
    // write to FIFO
    if(write(m_sFd, eState, n+1) < 0)
    {
        av_log(NULL, AV_LOG_ERROR,
            "%s:%d Write vsenc FIFO Failed.\n",__func__,__LINE__);
        close(m_sFd);
        m_sFd=-1;
        return;
    }

    return;
}

void venc_notify_mem_buf(char *buf, int buf_size, float videoRate, int m_startFormat)
{
   while(1)
   {
     if(buf_flag == 0 || buf_flag ==7)
     {
	   av_log(NULL, AV_LOG_ERROR,
		   "%s:%d buf %p buf_size %d videoRate %f m_startFormat %d buf_flag %d.\n",__func__,__LINE__,
				   buf,
				   buf_size,
				   videoRate,
				   m_startFormat,
				   buf_flag);
       buf_flag = 1;
       video_buf = buf;
       video_size = buf_size;
	   break;
     }
     av_log(NULL, AV_LOG_ERROR,
		   "%s:%d usleep 10ms buf %p buf_size %d videoRate %f m_startFormat %d buf_flag %d.\n",__func__,__LINE__,
				   buf,
				   buf_size,
				   videoRate,
				   m_startFormat,
				   buf_flag);
	 usleep(10*1000);
   }
#if 0
   FILE *file = fopen("data/misc/camera/mem_buf.265","wb+");
   fwrite((void *)buf,1,buf_size,file);
   fclose(file);
   free(buf);
   buf = NULL;
#endif
   return;
}


/*===========================================================================
 * FUNCTION   : venc_notify_done
 *
 * DESCRIPTION: static function to set callbacks function to camera module
 *
 * PARAMETERS :
 *   @callbacks : ptr to callback functions
 *
 * RETURN     : NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
void venc_notify_done(char *audioPath, char *audioFileName, float videoRate, int m_startFormat)
{
    int n;
    int retry;
    static int m_dFd=-1;
    char eState[MAX_FIFO_LEN]={0};
    //const char * const split = "\t";

    ///Create a video FIFO pipeline
    if (access(vdenc_fifo, F_OK) == -1)
    {
        // The pipeline file does not exist
        // Create a named pipe
        retry = mkfifo(vdenc_fifo, 0666);
        if (retry != 0)
        {
            av_log(NULL, AV_LOG_ERROR,
                "%s:%d Could not create fifo %s.\n",__func__, __LINE__, vdenc_fifo);
            return;
        }
    }
    if(m_dFd < 0)
    {
        for (retry = 0; retry < 100; retry++) {
            // Open a vdenc FIFO with read and write
            m_dFd = open(vdenc_fifo, O_WRONLY | O_NONBLOCK);
            if(m_dFd < 0)
            {
                usleep(MAX_SLEEP_TIME*10);
                continue;
            } else {
                av_log(NULL, AV_LOG_VERBOSE,
                    "%s:%d vdenc FIFO EXISIT!\n",__func__,__LINE__);
                break;
            }
        }
    }
    n=sprintf(eState,"%s#%s#%d#%f",audioPath,audioFileName,m_startFormat,videoRate);

    // write to FIFO
    if(write(m_dFd, eState, n+1) < 0)
    {
        av_log(NULL, AV_LOG_ERROR,
            "%s:%d Write vdenc FIFO Failed.\n",__func__,__LINE__);
        close(m_dFd);
        m_dFd=-1;
        return;
    }


    av_log(NULL, AV_LOG_VERBOSE,
        "%s:%d eState=[%d] END.\n",__func__, __LINE__, sizeof(eState));
    return;
}


/*===========================================================================
 * FUNCTION   : AVIMediaCodec
 *
 * DESCRIPTION: Create audio codec, video codec, thread pool, etc.
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
AVIMediaCodec::AVIMediaCodec()
{
    m_record = false;
    m_preRecord = false;
    m_abandon = false;
    nKillThread = KILL_THREAD_NO;
    pVideoCore = new vtest::VideoCore;
    pVideoCore->Init();
    m_audioInput = new AVIAudioInput;

    aacData = (AACData *)malloc(sizeof(AACData));
    memset(aacData, 0, sizeof(AACData));

    //1ms PCM data length is 16 * 16 * 1bit = 16 * 2byte
    //30s total has (16 * 2byte * 1000) * 30 Byte PCM data
    aacData->maxlen = 16*2*1000*30;
    //read pcm file to memory
    aacData->data = (uint8_t*)av_mallocz(aacData->maxlen);

    pthread_mutex_init(&mutex, NULL); //Initialize the lock mutex
}


/*===========================================================================
 * FUNCTION   : ~AVIMediaCodec
 *
 * DESCRIPTION: Release audio codec, release video codec
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
AVIMediaCodec::~AVIMediaCodec()
{
    //m_threadPool = NULL;
    if(pVideoCore != NULL)
    {
        delete pVideoCore;
    }
    pthread_mutex_destroy(&mutex);  //Destroy lock
}

void  AVIMediaCodec::initializeMedia()
{
    char filename[MAX_STRING_LEN]="";
    std::string firstfile = "";
    std::string dir = std::string(AVI_DEMUXER_LOCATION);
    std::vector<std::string> files = std::vector<std::string>();

    getMediadir(dir,files);
    /*this for loop prints directory contents*/
    for (unsigned int i = 2;i < files.size();i++) {	//i=0 includes . and ..; i=1 includes ..; i=2 starts on first file
        sprintf(filename, "%s%s", dir.c_str(),files[i].c_str());//print each file in directory
        av_log(NULL, AV_LOG_VERBOSE, "%s:%d The file found is %s.\n",
                __func__, __LINE__, filename);
        if(remove(filename)==0)
            av_log(NULL, AV_LOG_VERBOSE,
                "%s:%d Deleted Successfully.\n", __func__,__LINE__);
        else
            av_log(NULL, AV_LOG_ERROR,
                "%s:%d Deleted Failed.\n", __func__,__LINE__);
    }
}

/*===========================================================================
 * FUNCTION   : displayPoolStatus
 *
 * DESCRIPTION: deconstructor of AVIMediaCodec
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
void AVIMediaCodec::displayPoolStatus(CThread_pool_t * pPool)
{
    static int nCount = AVI_DIVNEM_FIRST;

    av_log(NULL, AV_LOG_DEBUG, "****************************\n");
    av_log(NULL, AV_LOG_DEBUG, "nCount = %d\n", nCount++);
    av_log(NULL, AV_LOG_DEBUG, "max_thread_num = %d\n", pPool->GetThreadMaxNum((void *)pPool));
    av_log(NULL, AV_LOG_DEBUG, "current_pthread_num = %d\n", pPool->GetCurrentThreadNum((void *)pPool));
    av_log(NULL, AV_LOG_DEBUG, "current_pthread_task_num = %d\n", pPool->GetCurrentTaskThreadNum((void *)pPool));
    av_log(NULL, AV_LOG_DEBUG, "current_wait_queue_num = %d\n", pPool->GetCurrentWaitTaskNum((void *)pPool));
    av_log(NULL, AV_LOG_DEBUG, "****************************\n");
}

/*function... might want it in some class?*/
int AVIMediaCodec::getMediadir(std::string dir, std::vector<std::string> &files)
{
    DIR *dp;
    struct dirent *dirp;

    if((dp  = opendir(dir.c_str())) == NULL) {
        av_log(NULL, AV_LOG_VERBOSE, "%s:%d Error( %d ) opening %s.\n",
               __func__, __LINE__, errno, dir.c_str());
        return errno;
    }

    while ((dirp = readdir(dp)) != NULL) {
        files.push_back(std::string(dirp->d_name));
    }

    closedir(dp);
    sort (files.begin(), files.end());			//added from computing.net tip

    return 0;
}

/*===========================================================================
 * FUNCTION   : registerCallback
 *
 * DESCRIPTION: deconstructor of AVIMediaCodec
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int AVIMediaCodec::registerCallback(AVICallbacks* callbacks)
{
    if (callbacks == NULL) {
        av_log(NULL, AV_LOG_ERROR, "%s%d: Already set AVICallbacks\n",__func__, __LINE__);
        return -1;
    }
    /* Decorate with locks */
    this->notify = callbacks;
    pVideoCore->RegisterAviOnCallback(venc_notify_start);
    pVideoCore->RegisterAviSaveCallback(venc_notify_done);
    pVideoCore->RegisterAviMemBufCallback(venc_notify_mem_buf);
    return 0;
}

/*===========================================================================
 * FUNCTION   : mediaDemuxer
 *
 * DESCRIPTION: deconstructor of mediaDemuxer
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int AVIMediaCodec::mediaDemuxer(AVIOutputProperty *property)
{
    Q_UNUSED(property)

    memcpy(&demuxerProperty, &syncProperty, sizeof(AVIOutputProperty));
    if(pThreadPool != NULL)
        pThreadPool->AddWorkLimit((void *)pThreadPool, sync_demuxer, (void *)this);
    else
        return -1;
    return 0;
}

/*
 * encode one audio frame and send it to the muxer
 * return 1 when encoding is finished, 0 otherwise
 */
int AVIMediaCodec::get_audio_frame()
{
    int ret;
    static int m_aFd=-1;
    char *token;

    if(m_aFd<0)
    {
        ///Create preview FIFO pipeline
        ret = mkfifo(audio_fifo, 0666);
        if(ret < 0 && errno!=EEXIST)
        {
            av_log(NULL, AV_LOG_ERROR,"%s:%d Create sync audio FIFO Failed!\n",
                __func__, __LINE__);
            return NULL;
        }
        av_log(NULL, AV_LOG_DEBUG,"%s:%d sync thread is running S!\n",
                __func__, __LINE__);
        m_aFd = open(audio_fifo, O_RDONLY);
        if(m_aFd < 0) // Open a preview FIFO with non-blocking write only
        {
            av_log(NULL, AV_LOG_ERROR,"%s:%d Open Preview FIFO Failed!\n",
                __func__, __LINE__);
            return NULL;
        }
    }
    av_log(NULL, AV_LOG_DEBUG,"%s:%d sync thread is running E!\n",
        __func__, __LINE__);
    /* select the stream to encode */
    // Audio encoding to get the beat of the audio
    if((ret = read(m_aFd, sAudioFrameProp, MAX_STRING_LEN)) > 0)
    {
        av_log(NULL, AV_LOG_DEBUG,"%s:%d The recorded data is %s.\n",
			__func__, __LINE__, sAudioFrameProp);
    }
    token = sAudioFrameProp;
    if(strempty(token)<0)
        return -EAVIBADF;
    else
        ret = atoi(token);
    return ret;
}

/*
 * encode one audio frame and send it to the muxer
 * return 1 when encoding is finished, 0 otherwise
 */
int AVIMediaCodec::get_video_start(AVIOutputProperty *property)
{
    int ret;
    static int sFd=-1;
    char *token, *sepstr;


    ///Create a video FIFO pipeline
    if (access(vsenc_fifo, F_OK) == -1)
    {
        // The pipeline file does not exist
        // Create a named pipe
        ret = mkfifo(vsenc_fifo, 0666);
        if (ret != 0)
        {
            av_log(NULL, AV_LOG_ERROR,
                "%s:%d Could not create fifo %s.\n",__func__, __LINE__, vsenc_fifo);
            return -EAVINODEV;
        }
    }
    if(sFd < 0)
    {
        // Open a demuxer FIFO with read and write
        sFd = open(vsenc_fifo, O_RDONLY);
        if(sFd < 0)
        {
            close(sFd);
            av_log(NULL, AV_LOG_ERROR, "%s:%d Open demuxer FIFO[%d] Failed!\n",__func__, __LINE__,sFd);
        }
    }

    /* select the stream to encode */
    // Audio encoding to get the beat of the audio
    if((ret = read(sFd, sVideoStartProp, MAX_STRING_LEN)) > 0)
    {
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d The recorded data is %s.",
            __func__, __LINE__, sVideoStartProp);
    }

    sepstr = sVideoStartProp;
    av_log(NULL, AV_LOG_DEBUG,
            "%s:%d Read demuxer message: [%s]\n",
                __func__,__LINE__,sepstr);

    /// Split the string with the TAB
    token = strsep(&sepstr, split);
    if(strempty(token)<0)
        return -EAVIBADF;
    sprintf(property->m_startPath,"%s",token);
    av_log(NULL, AV_LOG_DEBUG,
        "%s:%d seppage=[ %s ]\n", __func__, __LINE__, property->m_startPath);

    /// Split the string with the TAB
    token = strsep(&sepstr, split);
    if(strempty(token)<0)
        return -EAVIBADF;
    sprintf(property->m_startName,"%s",token);
    av_log(NULL, AV_LOG_DEBUG,
            "%s:%d token=[%s]\n", __func__,__LINE__, property->m_startName);

    token = strsep(&sepstr, split);
    if(strempty(token)<0)
        return -EAVIBADF;
    property->m_startFormat = atoi(token);

    av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d X.\n", __func__,__LINE__);
    return ret;
}


/*
 * encode one audio frame and send it to the muxer
 * return 1 when encoding is finished, 0 otherwise
 */
int AVIMediaCodec::get_video_done(AVIOutputProperty *property)
{
    int ret;
    static int dFd=-1;
    char *token, *sepstr;

    ///Create a video FIFO pipeline
    if (access(vdenc_fifo, F_OK) == -1)
    {
        // The pipeline file does not exist
        // Create a named pipe
        ret = mkfifo(vdenc_fifo, 0666);
        if (ret != 0)
        {
            av_log(NULL, AV_LOG_ERROR,"%s:%d Could not create fifo %s.\n",
                    __func__, __LINE__, vdenc_fifo);
            return -EAVINODEV;
        }
    }
    if(dFd < 0)
    {
        // Open a demuxer FIFO with read and write
        dFd = open(vdenc_fifo, O_RDONLY);
        if(dFd < 0)
        {
            close(dFd);
            av_log(NULL, AV_LOG_ERROR, "%s:%d Open demuxer FIFO[%d] Failed!\n",
                    __func__, __LINE__,dFd);
        }
    }

    /* select the stream to encode */
    // Audio encoding to get the beat of the audio
    if((ret = read(dFd, sVideoDoneProp, MAX_STRING_LEN)) > 0)
    {
        av_log(NULL, AV_LOG_DEBUG,"%s:%d The recorded data is %s.",
            __func__, __LINE__, sVideoDoneProp);
    }
    sepstr = sVideoDoneProp;
    av_log(NULL, AV_LOG_VERBOSE,"%s:%d Read demuxer message: [%s]\n",
                __func__,__LINE__,sepstr);

    /// Split the string with the TAB
    token = strsep(&sepstr, split);
    if(strempty(token)<0)
        return -EAVIBADF;
    sprintf(property->m_donePath,"%s",token);
    av_log(NULL, AV_LOG_DEBUG,"%s:%d m_donePath=[ %s ]\n",
             __func__, __LINE__, property->m_donePath);

    /// Split the string with the TAB
    token = strsep(&sepstr, split);
    if(strempty(token)<0)
        return -EAVIBADF;
    sprintf(property->m_doneName,"%s",token);
    av_log(NULL, AV_LOG_DEBUG,"%s:%d m_doneName=[%s]\n",
             __func__,__LINE__, property->m_doneName);

    token = strsep(&sepstr, split);
    if(strempty(token)<0)
        return -EAVIBADF;
    property->m_startFormat = atoi(token);

    token = strsep(&sepstr, split);
    if(strempty(token)<0)
        return -EAVIBADF;
    property->m_doneFrameRate = atof(token);
    av_log(NULL, AV_LOG_DEBUG,
            "%s:%d X.\n", __func__,__LINE__);
    return ret;
}



/*===========================================================================
 * FUNCTION   : pushData
 *
 * DESCRIPTION: deconstructor of pushData
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int64_t AVIMediaCodec::pushData(const void* data, int64_t len)
{
    char prop[MAX_STRING_LEN];
    property_get("persist.audio.pcm.enabled", prop, "0");
    int isPCMDisable = atoi(prop);
    if(isPCMDisable)
    {
        return 0;
    }

    m_audioInput->writeData(data, len);
    return 0;
}

/*===========================================================================
 * FUNCTION   : pushData
 *
 * DESCRIPTION: deconstructor of pushData
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int64_t AVIMediaCodec::pushData(const void* data, int64_t len, uint32_t frame_idx)
{
    Q_UNUSED(data)
    Q_UNUSED(len)
    Q_UNUSED(frame_idx)
    return 0;
}

/*===========================================================================
 * FUNCTION   : startRecord
 *
 * DESCRIPTION: startRecord Be sure to send a start signal
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int AVIMediaCodec::startRecord(struct record_info *record_info)
{
    nKillThread = KILL_THREAD_NO;
    memcpy(&record,record_info,sizeof(struct record_info));

    if(!m_record){
        m_abandon = false;
        m_record = true;
        if(m_preRecord){
            m_preRecord = false;
            if (this->notify) {
                (*this->notify->vtest_swvenc_diag)(syncProperty.m_startPath,syncProperty.m_startName);
                (*this->notify->venc_filename_cb)(syncProperty.m_startName);
                av_log(NULL, AV_LOG_VERBOSE,
                    "%s:%d [DBG] Video Start FileName callback = [%s].\n",__func__, __LINE__,syncProperty.m_startName);
            }
        } else {
            if(pThreadPool != NULL)
            {
                m_audioInput->nKillThread = KILL_THREAD_NO;
                m_audioInput->clearBuffer();
                pThreadPool->AddWorkLimit((void *)pThreadPool, sync_process1, (void *)this);
            }
            av_log(NULL, AV_LOG_VERBOSE, "%s:%d AVIMediaCodec Thread Pool Creat.\n",__func__,__LINE__);
        }
    } else {
        av_log(NULL, AV_LOG_VERBOSE, "%s:%d MediaCodec is busy.\n",__func__, __LINE__);
        return -EAVIBUSY;
    }

    usleep(MAX_SLEEP_TIME*10);
    av_log(NULL, AV_LOG_VERBOSE, "%s:%d X.\n",__func__, __LINE__);
    return 0;
}

/*===========================================================================
 * FUNCTION   : stopRecord
 *
 * DESCRIPTION: stopRecord must send a stop signal.
 *              Stop signal is also sent after the completion of 10 segments.
 *              Then send the next start signal again.
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int AVIMediaCodec::stopRecord()
{
    m_abandon = false;
    if(m_record)
        m_record = false;
    else
        return -EAVIBUSY;

    av_log(NULL, AV_LOG_VERBOSE, "%s: E\n",__func__);
    nKillThread = KILL_THREAD_EXIT;
    m_audioInput->nKillThread = KILL_THREAD_EXIT;

    return 0;
}

/*===========================================================================
 * FUNCTION   : startPreRecord
 *
 * DESCRIPTION: StartPreRecord Be sure to send a start signal.
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int AVIMediaCodec::startPreRecord(struct record_info *record_info)
{
    nKillThread = KILL_THREAD_NO;
    memcpy(&record,record_info,sizeof(struct record_info));
    if(!m_record){
        if(!m_preRecord){
            m_abandon = false;
            m_preRecord = true;
            if(pThreadPool != NULL)
            {
                m_audioInput->nKillThread = KILL_THREAD_NO;
                m_audioInput->clearBuffer();
                pThreadPool->AddWorkLimit((void *)pThreadPool, sync_process1, (void *)this);
            }
            av_log(NULL, AV_LOG_VERBOSE, "%s:%d AVIMediaCodec Thread Pool Creat.\n", __func__,__LINE__);
        } else {
            av_log(NULL, AV_LOG_VERBOSE, "%s:%d MediaCodec is busy.\n", __func__,__LINE__);
            return -EAVIBUSY;
        }
    } else {
        av_log(NULL, AV_LOG_VERBOSE, "%s:%d MediaCodec is busy.\n", __func__,__LINE__);
        return -EAVIBUSY;
    }
    usleep(MAX_SLEEP_TIME*10);
    av_log(NULL, AV_LOG_VERBOSE, "%s: X.\n",__func__);
    return 0;
}

/*===========================================================================
 * FUNCTION   : stopPreRecord
 *
 * DESCRIPTION: StopPreRecord must send a stop signal.
 *              Stop signal is also sent after the completion of 10 segments.
 *              Then send the next start signal again.
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
int AVIMediaCodec::stopPreRecord()
{
    if(m_preRecord)
        m_preRecord = false;
    else
        return -EAVIBUSY;
    m_abandon = true;
    av_log(NULL, AV_LOG_VERBOSE, "%s: E.\n",__func__);
    nKillThread = KILL_THREAD_EXIT;
    m_audioInput->nKillThread = KILL_THREAD_EXIT;

    return 0;
}
