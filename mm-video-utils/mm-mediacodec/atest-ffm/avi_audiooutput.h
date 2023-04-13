/*-------------------------------------------------------------------
Copyright (c) 2017-2018 Qualcomm Technologies, Inc. All Rights Reserved.
Qualcomm Technologies Proprietary and Confidential
--------------------------------------------------------------------*/

#ifndef _AVI_AUDIOTRACK_H
#define _AVI_AUDIOTRACK_H

#include <stdio.h>
#include <stdlib.h>   // exit
#include <fcntl.h>    // O_WRONLY
#include <sys/stat.h>
#include <time.h>     // time
#include <errno.h>
#include <utils/Log.h>

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
#ifdef __cplusplus
};
#endif
#endif
#include "avi_types.h"


/*****************************************************************************
 *	Dedinfe
 *****************************************************************************/
#define FFMLOG_TAG "FFMAudio"



/*****************************************************************************
 *	Shared Typedefs & Macros
 *****************************************************************************/
const char *const audio_output = "/data/misc/camera/fifotrack";

/*****************************************************************************
 *	Camera Global Variable
 *****************************************************************************/
extern "C" void *AVI_AudioOutput(void * arg);


namespace android {
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

class AVIAudioOutput
{
public:
    AVIAudioOutput();
    virtual ~AVIAudioOutput();

    void start(struct record_info *record_info);
    void stop();

private:
    void initializeAudioOutput();

public:
    int audFd;

private:
    int rStatus;
};
}; /*namespace android*/

#endif // #ifndef _AVI_AUDIOTRACK_H
