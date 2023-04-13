/*-------------------------------------------------------------------
Copyright (c) 2017-2018 Qualcomm Technologies, Inc. All Rights Reserved.
Qualcomm Technologies Proprietary and Confidential
--------------------------------------------------------------------*/

#ifndef _AVI_QAUDIOTEST_H
#define _AVI_QAUDIOTEST_H

#include <stdio.h>
#include <stdlib.h>   // exit
#include <fcntl.h>    // O_WRONLY
#include <sys/stat.h>
#include <time.h>     // time
#include <errno.h>
#include <utils/Log.h>
#include "avi_types.h"


/*****************************************************************************
 *	Dedinfe
 *****************************************************************************/
#define FFMLOG_TAG "FFMAudio"



/*****************************************************************************
 *	Shared Typedefs & Macros
 *****************************************************************************/
/* JVM status parameter */
typedef enum {
    ASSOCIATE_JVM,
    DISASSOCIATE_JVM,
} ThreadEvent;

/* Sound signal callback */
typedef void (*PFN_AUDIOFRAME_CB)(const void *pFrameHeapBase, size_t frame_len, uint32_t frame_idx);

/**
 * Callback utility for acquiring a wakelock.
 * This can be used to prevent the CPU from suspending while handling FLP events.
 */
typedef void (*PFN_ACQUIRE_WAKELOCK)();

/**
 * Callback utility for releasing the FLP wakelock.
 */
typedef void (*PFN_RELEASE_WAKELOCK)();


/**
 * Callback for associating a thread that can call into the Java framework code.
 * This must be used to initialize any threads that report events up to the framework.
 * Return value:
 *      FLP_RESULT_SUCCESS on success.
 *      FLP_RESULT_ERROR if the association failed in the current thread.
 */
typedef int (*PFN_SET_THEAD_EVENT)(ThreadEvent event);

/** @AVI callback structure. */
typedef struct {
    /** set to sizeof(BarCallbacks) */
    size_t      size;
    PFN_AUDIOFRAME_CB notify_cb;
    PFN_ACQUIRE_WAKELOCK acquire_wakelock_cb;
    PFN_RELEASE_WAKELOCK release_wakelock_cb;
    PFN_SET_THEAD_EVENT set_thread_event_cb;
} ACallbacks;


/*****************************************************************************
 *	Camera Global Variable
 *****************************************************************************/
extern "C" void *AVI_QAUDIOTEST(void * arg);


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

class QAudioTest
{
public:
    QAudioTest();
    virtual ~QAudioTest();

    void start();
    void stop();
    void initializeAudio();
    void createAudio();
    int registerCallback(ACallbacks* callbacks);
    int64_t readData(char *data, int64_t maxlen);
    int64_t writeData(const void* data, int64_t len);

private:
    void deviceChanged(int index);
    void volumeChanged(int value);
    int64_t bytesAvailable() const;
    void refreshBuffer();

public:
    int audFd;
    /**
     * Client provided callback function to receive notifications.
     * Do not set by hand, use the function above instead.
     *
     * @param dev from open
     *
     * @return 0 if successful
     */
    ACallbacks* notify;

private:
    int rStatus;
    clock_t start_t;
    clock_t finish_t;
};/*namespace android*/

#endif // #ifndef _AVI_QAUDIOTEST_H
