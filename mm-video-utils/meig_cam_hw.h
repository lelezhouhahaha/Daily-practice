/*
 * Copyright (C) Motorola Solutions, Inc. - http://www.motorolasolutions.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_INCLUDE_BARCODE_H
#define ANDROID_INCLUDE_BARCODE_H


#include <ctype.h>
#include <unistd.h>
#include <malloc.h>
#include <stdint.h>
#include <sys/cdefs.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/time.h>
#include <sys/select.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <errno.h>
#include <assert.h>
#include <pthread.h>
#include <signal.h>
#include <semaphore.h>
#include <utils/Log.h>
#include <hardware/hardware.h>
#include <cutils/properties.h>

__BEGIN_DECLS


////////////////////////////////////////////////////////////////////////////////
// Definitions
////////////////////////////////////////////////////////////////////////////////

#define MAX_TEST_COUNT             50000
#define MAX_MSG_LENGTH             65525
#define MAX_SSIDATA_LENGTH         256
#define SCANNER_SYNC_TIMEOUT       300   // in msecs
#define SCANNER_TEST_TIMEOUT       10000 // in msecs

//for snapshot
#define TAKEPHOTO_FROM_P 1
#define TAKEPHOTO_FROM_S 0

/**
 * This value must equal with cameraID of scan engine,
 * 1 for front camera, 0 for rear camera.
 */

#define SCANNER                        1
#define MEIGCAM_HARDWARE_MODULE_ID     "meigcam"

#define HAL_VIDEO_RESOLUTION_1920_1080 0
#define HAL_VIDEO_RESOLUTION_1280_720  1
#define HAL_VIDEO_RESOLUTION_720_480   2

#define HAL_VIDEO_FRAME_RATE_MIN       1
#define HAL_VIDEO_FRAME_RATE_MAX       30

#define HAL_VIDEO_PRERECORD_LENGTH_MIN 0
#define HAL_VIDEO_PRERECORD_LENGTH_MAX 30

#define HAL_VIDEO_SPLIT_LENGTH_MIN     0
#define HAL_VIDEO_SPLIT_LENGTH_MAX     1080

#define HAL_VIDEO_FORMAT_H264          0
#define HAL_VIDEO_FORMAT_H265          1

#define HAL_VIDEO_BIT_RATE_MIN         64
#define HAL_VIDEO_BIT_RATE_MAX         20000

#define HAL_VIDEO_COLOR_MODE_NORMAL      0
#define HAL_VIDEO_COLOR_MODE_BLACK_WHITE 1

////////////////////////////////////////////////////////////////////////////////
// Prototypes
////////////////////////////////////////////////////////////////////////////////
// uMsgType posted by BARCODE
// msgType in notifyCallback and dataCallback functions



/** State information for each device instance */
struct meigcam_context_t {
    char *buffer;
    int filelen;
};

struct meigcam_yuv2app_t {
    void *yuv2app_buffer;
    int yuv2app_width;
    int yuv2app_height;
};

struct private_info {
    int cam_id;
    int video_resolution;
    int video_frame_rate;
    int video_format;
    int video_bit_rate;
    int video_color_mode;
    int video_mode;
    int upload_video_resolution;
    int upload_video_frame_rate;
    int upload_video_format;
    int upload_video_bit_rate;
    int video_prerecord_length;
    int video_split_length;
    const char *watermark_info;
};

/**
 * Photo information structure.
 */
typedef struct {
    int photo_resolution;
    int photo_multi_shot_num;
    int photo_start_num;
    char photo_save_path[256];
    char photo_serial_num[256];
    char photo_time_stamp[256];
} takephoto_info_t ;

/**
 *Callback function type 
 */
typedef int (*meigcam_notify_t)(struct meigcam_context_t msg, void *user);
typedef void (*meigcam_videofilename_cb)(char* videofilename_cb, void *user);
typedef void (*meigcam_yuv2app_cb)(struct meigcam_yuv2app_t yuv2app, void *user);

/**
 * Callback utility for acquiring a wakelock.
 * This can be used to prevent the CPU from suspending while handling FLP events.
 */
typedef void (*meigcam_acquire_wakelock)();

/**
 * Callback utility for releasing the FLP wakelock.
 */
typedef void (*meigcam_release_wakelock)();


/**
 * Callback for associating a thread that can call into the Java framework code.
 * This must be used to initialize any threads that report events up to the framework.
 * Return value:
 *      FLP_RESULT_SUCCESS on success.
 *      FLP_RESULT_ERROR if the association failed in the current thread.
 */
typedef int (*meigcam_set_thread_event)();


/** SCANNER callback structure. */
typedef struct {
    /** set to sizeof(MeigcamCallbacks) */
    size_t      size;
    meigcam_notify_t notify_cb;
    meigcam_videofilename_cb filename_cb;
    meigcam_yuv2app_cb yuv2app_cb;
} MeigcamCallbacks;


/**
 * Every hardware module must have a data structure named HAL_MODULE_INFO_SYM
 * and the fields of this data structure must begin with hw_module_t
 * followed by module specific information.
 */
typedef struct meigcam_module {
    struct hw_module_t common;
} meigcam_module_t;

struct preview_info {
    int preview_width;
    int preview_height;
};



struct meigcam_hw_device_t {
    struct hw_device_t common;
    void *user;
    int (*meigcam_open)(struct record_info *record_info);

    int (*meigcam_close)(struct meigcam_hw_device_t *);

    int (*meigcam_startpreview)(struct meigcam_hw_device_t *);

    int (*meigcam_stoppreview)(struct meigcam_hw_device_t *);

    int (*meigcam_startvideo)(struct record_info *record_info);

    int (*meigcam_stopvideo)(struct meigcam_hw_device_t *);

    int (*meigcam_startprerecord)(struct record_info *record_info);

    int (*meigcam_stopprerecord)(struct meigcam_hw_device_t *);

    int (*meigcam_startvideorecord)(struct record_info *record_info);

    int (*meigcam_stopvideorecord)(struct meigcam_hw_device_t *);

    int (*meigcam_startuploadrecord)(struct record_info *record_info);

    int (*meigcam_stopuploadrecord)(struct meigcam_hw_device_t *);

    int (*meigcam_startyuv2app)(struct meigcam_hw_device_t *,int startyuv);

    int (*meigcam_stopyuv2app)(struct meigcam_hw_device_t *,int stopyuv);

    int (*meigcam_setuploadbitrate)(struct meigcam_hw_device_t * dev, int bitrate);

    int (*meigcam_setuploadPframe)(struct meigcam_hw_device_t * dev, int pframe);

    int (*meigcam_setupvideoframerate)(struct meigcam_hw_device_t * dev, int framerate);

    int (*meigcam_takephoto)(struct meigcam_hw_device_t *, takephoto_info_t *);

    int (*meigcam_getgpsinfo)(struct meigcam_hw_device_t * dev, char *watermark_gps_info);

    int (*meigcam_setcolormode)(struct meigcam_hw_device_t * dev, int color_mode);

    int (*meigcam_writeaudio)(struct meigcam_hw_device_t * dev, const void *data, int count);

    int (*meigcam_setnotify)(struct meigcam_hw_device_t *dev,
                        MeigcamCallbacks* callbacks, void *user);

    int (*meigcam_setzoom)(struct meigcam_hw_device_t *dev, int zoom_target);

    int (*meigcam_set_WNR)(struct meigcam_hw_device_t *dev, uint8_t enable);

    //void *filename_user;
    //int (*meigcam_notify_name)(struct meigcam_hw_device_t *dev, meigcam_videofilename_cb videofilename_cb, void* user);
    /**
     * Client provided callback function to receive notifications.
     * Do not set by hand, use the function above instead.
     *
     * @param dev from open
     *
     * @return 0 if successful
     */
    MeigcamCallbacks* notify;

	int (*meigcam_getbaseinfo)(struct meigcam_hw_device_t * dev, char *watermark_base_info);
};
//typedef struct barcode_hw_device barcode_hw_device_t;



/** convenience API for opening and closing a device */
__END_DECLS

#endif /* #ifdef ANDROID_INCLUDE_BARCODE_H */
