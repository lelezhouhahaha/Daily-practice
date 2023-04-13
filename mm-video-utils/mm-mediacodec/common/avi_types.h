/*****************************************************************************
 *	avi_types.h
 *
 * 	$HeadURL:$
 * 	$Revision:$ v1.00
 * 	$Date:$     2019-3-10
 * 	$Author:$   huangfusheng
 *
 * 	General type definitions for the Audio and video test SDK.
 *
 *  Copyright (c) 2019 Meig Solutions, Inc. All Rights Reserved.
 *****************************************************************************/

#ifndef AVI_TYPES_H_
#define AVI_TYPES_H_

/*****************************************************************************
 *	Includes
 *****************************************************************************/
#include <limits.h>		// Platform specific numeric limits


/***********************************************************************
 *	Dedinfe
 ***********************************************************************/
#define AVIFFMPEG_TAG        "AVI_FFMPEG"
#define MAX_STRING_LEN       1024
#define MAX_FIFO_LEN         1024
#define MAX_SLEEP_TIME       1000
#define SYNCERROT            -100
#define AVIO_BUFFER_SIZE     2048*2048*12	//4M*12
#define AVI_DEMUXER_LOCATION "/data/misc/media/"

/*
   Avoid "unused parameter" warnings
*/
#define Q_UNUSED(x) (void)x;

#define ARRAY_SIZE(a) (int)(sizeof(a)/sizeof((a)[0]))
#define	EAVIPERM		 1	/* Operation not permitted */
#define	EAVINOENT		 2	/* No such file or directory */
#define	EAVISRCH		 3	/* No such process */
#define	EAVIINTR		 4	/* Interrupted system call */
#define	EAVIIO			 5	/* I/O error */
#define	EAVINXIO		 6	/* No such device or address */
#define	EAVI2BIG		 7	/* Argument list too long */
#define	EAVINOEXEC		 8	/* Exec format error */
#define	EAVIBADF		 9	/* Bad file number */
#define	EAVICHILD		10	/* No child processes */
#define	EAVIAGAIN		11	/* Try again */
#define	EAVINOMEM		12	/* Out of memory */
#define	EAVIACCES		13	/* Permission denied */
#define	EAVIFAULT		14	/* Bad address */
#define	EAVINOTBLK		15	/* Block device required */
#define	EAVIBUSY		16	/* Device or resource busy */
#define	EAVIEXIST		17	/* File exists */
#define	EAVIXDEV		18	/* Cross-device link */
#define	EAVINODEV		19	/* No such device */
#define	EAVINOTDIR		20	/* Not a directory */
#define	EAVIISDIR		21	/* Is a directory */
#define	EAVIINVAL		22	/* Invalid argument */
#define	EAVINFILE		23	/* File table overflow */
#define	EAVIMFILE		24	/* Too many open files */
#define	EAVINOTTY		25	/* Not a typewriter */
#define	EAVITXTBSY		26	/* Text file busy */
#define	EAVIFBIG		27	/* File too large */
#define	EAVINOSPC		28	/* No space left on device */
#define	EAVISPIPE		29	/* Illegal seek */
#define	EAVIROFS		30	/* Read-only file system */
#define	EAVIMLINK		31	/* Too many links */
#define	EAVIPIPE		32	/* Broken pipe */
#define	EAVIDOM			33	/* Math argument out of domain of func */
#define	EAVIRANGE		34	/* Math result not representable */

/*****************************************************************************
 *	Shared Typedefs & Macros
 *****************************************************************************/
const char *const audio_fifo = "/data/misc/camera/fifo_record";
const char *const vsenc_fifo = "/data/misc/camera/fifo_demuxer1";
const char *const vdenc_fifo = "/data/misc/camera/fifo_demuxer2";


#if (USHRT_MAX >= 0xFFFFFFFFL)
	typedef		signed short int	AVI_I32;
	typedef		unsigned short int	AVI_U32;
#elif (UINT_MAX >= 0xFFFFFFFFL)
	typedef		signed int			AVI_I32;
	typedef		unsigned int		AVI_U32;
#elif (ULONG_MAX >= 0xFFFFFFFFL)
	typedef		signed long int		AVI_I32;
	typedef		unsigned long int	AVI_U32;
#else
#error "No available integral type for AVI_I32, AVI_U32"
#endif

#if (USHRT_MAX >= 0xFFFF)
	typedef		signed short int	AVI_I16;
	typedef		unsigned short int	AVI_U16;
#elif (UINT_MAX >= 0xFFFF)
	typedef		signed int			AVI_I16;
	typedef		unsigned int		AVI_U16;
#elif (ULONG_MAX >= 0xFFFF)
	typedef		signed long int		AVI_I16;
	typedef		unsigned long int	AVI_U16;
#else
#error "No available integral type for AVI_I16, AVI_U16"
#endif

#if (UCHAR_MAX == 0xFF)
	typedef		signed char			AVI_I8;
	typedef		unsigned char		AVI_U8;
	typedef		unsigned char		AVI_BOOL;
#else
#error "No available integral type for AVI_I8, AVI_U8, AVI_BOOL"
#endif

#define AVI_FALSE			((AVI_BOOL)0)
#define AVI_TRUE			((AVI_BOOL)(!AVI_FALSE))

typedef void *			AVI_REF;		// Generic reference handle
#define AVI_REF_INVALID		((AVI_REF)(-1))

typedef AVI_REF		AVI_SCANNER_OBJ;


/*****************************************************************************
 *	Shared Enumerations
 *****************************************************************************/
/** @ingroup comp */
typedef enum AVI_DIVNEM {
    AVI_DIVNEM_ZERO  = 0,  /**< The number of splits is initialized to zero */
    AVI_DIVNEM_FIRST = 1,  /**< Initial value of the number of divisions */
    AVI_DIVNEM_MAX   = 100 /**< Maximum number of segments */
}AVI_DIVNEM;

/** @ingroup comp */
typedef enum AVI_COMPRESSION {
    VIDEO_COMPRESSION_FORMAT_H264,  /**< The format supported by video recording by default */
    VIDEO_COMPRESSION_FORMAT_H265,  /**< Video recording compatible format supported */
    VIDEO_MODE_LOCAL_SAVE_UPLDOAD,
    VIDEO_MODE_UPLOAD,
    VIDEO_MODE_LOCAL_SAVE,
    VIDEO_PRIV_STREAM_ON,           /**< The video starts recording the transmitted signal. */
    VIDEO_PRIV_STREAM_OFF,          /**< The signal sent by the video recording is completed. */
    VIDEO_PRIV_STREAM_SAVE,         /**< Video recording saves the transmitted signal. */
    VIDEO_PRIV_STREAM_DEFAULT       /**< Video recording saves the transmitted signal. */
}AVI_COMPRESSION;

/** @ingroup comp */
typedef enum AVI_PARAMETER {
    AVI_SWMENC_STATUS,       /**< Media recording saves the transmitted signal. */
    AVI_SWMENC_DIAG,         /**< Media recording saves the transmitted signal. */
    AVI_SWMENC_PATH,         /**< Media recording saves the transmitted signal. */
    AVI_SWVENC_FORMAT,       /**< Video recording saves the transmitted signal. */
    AVI_SWVENC_FRAME_RATE,   /**< Video recording saves the transmitted signal. */
    AVI_SWVENC_CODE_RATE,    /**< Video recording saves the transmitted signal. */
    AVI_SWVENC_RESOLUTION,   /**< Video recording saves the transmitted signal. */
    AVI_SWVENC_PFRAME,       /**< Video recording saves the transmitted signal. */
    AVI_SWVENC_SECTION_TIME, /**< Video recording saves the transmitted signal. */
    AVI_SWVENC_RECORD_STATUS /**< Video recording saves the transmitted signal. */
}AVI_PARAMETER;

struct record_info {
    struct meigcam_hw_device_t *hwDevice;
    int cam_id;
    //video settings
    int video_resolution;
    int video_frame_rate;//30fps or 60fps
    int cam_frame_rate;//25fps or 30fps
    int video_format;
    int video_bit_rate;//[4M,12M]
    int video_I_frame_space;
    int video_split_length;
    int video_prerecord_length;
    int video_file_start_number;
    int audio_sample_rate;
    int audio_channel_num;
    int audio_sample_type;
    int surfaceview_left;
    int surfaceview_right;
    int surfaceview_top;
    int surfaceview_bottom;
    char video_serial_number[50];
    char video_save_path[100];
	char watermark_base_info[300];
    char watermark_gps_info[200];
};

/** @Media property structure. */
typedef struct {
    int dCount;
    int m_sLen;
    float m_doneFrameRate;
    int m_startFormat;
    char m_doneName[MAX_STRING_LEN];
    char m_donePath[MAX_STRING_LEN];
    char m_startName[MAX_STRING_LEN];
    char m_startPath[MAX_STRING_LEN];
    bool aviStatus;
} AVIOutputProperty;     //Macro defines the data type of the Media save

#endif /* AVI_TYPES_H_ */
