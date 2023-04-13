#include <iostream>
#include <vector>
#include <string>
#include "mm_qcamera_scale.h"
extern "C" {
    #include "../mm-qthread-pool/mm_qthread_pool.h"
}
#include "ScaleModule.h"
//#include "libyuv/convert_from.h"
#include <cutils/properties.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <dirent.h>
#include <sys/stat.h>

//-------------------------------------------------------------------
//  Add libavutil/common.h by huangfusheng 2020-5-22
//-------------------------------------------------------------------
#ifdef __cplusplus
extern "C"
{
#endif
#include <libavutil/common.h>
#ifdef __cplusplus
};
#endif

#undef  CDBG
#define CDBG(fmt, ...) av_log(NULL, AV_LOG_VERBOSE, "SCALE_MODULE %s::%d " fmt, __FUNCTION__, __LINE__, ## __VA_ARGS__)

#define TEST_LOCATION "/data/misc/camera/"
#undef  PHOTO_SCALE_DEBUG
//#define  PHOTO_SCALE_DEBUG

using namespace std;
typedef unsigned int uint_32;
#define  IMG_WIDTH      2560//2592
#define  IMG_HEIGHT     1440//1458

#define  SCALEWIDTH     5760//5248
#define  SCALEHEIGHT    3240//3968

//default snapshot size
#define  DEFAULT_WIDTH       2624//2592
#define  DEFAULT_HEIGHT      1476//1458
//5M
#define  A_WIDTH             3072
#define  A_HEIGHT            1728
//9M
#define  B_WIDTH             4096
#define  B_HEIGHT            2304
//12M
#define  C_WIDTH             4608
#define  C_HEIGHT            2592
//16M
#define  D_WIDTH             5376//5248
#define  D_HEIGHT            3024//3008
//20M
#define  E_WIDTH             6016//5824
#define  E_HEIGHT            3384//3267
//32M
#define  F_WIDTH             7680
#define  F_HEIGHT            4320
//36M
#define  G_WIDTH             8000
#define  G_HEIGHT            4500
//40M
#define  H_WIDTH             8512
#define  H_HEIGHT            4800

/*****************************************************************************
 * photo Global Variable
 * snapshot_scale_dim: snapshot width and height
 * liveshot_scale_dim: snapshot width and height
 * brust_pho_count   : count photo in brust mode 连拍模式计数器
 * pho_over_alarm    : check bit for JPEG coding 照片合成完毕校验位
 * brust_num         : brust num restore         连拍数
 * scaleModule       : photo operation object    拍照对象(含插值和存储动作)
 *****************************************************************************/
static photo_size_info snapshot_scale_dim = {NULL ,0, 0};
static photo_size_info liveshot_scale_dim = {NULL ,0, 0};
static int brust_pho_count;
char pho_over_alarm[10];
int brust_num;

ScaleModule *scaleModule;

/*****************************************************************************
 * declaration for functions
 *****************************************************************************/
int liveshot2JPEG( void *arg);
void *snapshot2JPEG(void * arg);
void mm_scale_display_pool_status(CThread_pool_t * pPool);

// for thread pool purpose
//static CThread_pool_t *s_pThreadPool = NULL;
static int nKillThread = KILL_THREAD_NO;

// initialize a photo_scale_info struct array with 3 members
photo_scale_info *scale_info_ptr = NULL;


static void NV212I420(char* yuv420sp, char* yuv420, int width, int height) {
    int i = 0, j = 0;
    int framesize = width * height;

    if (yuv420sp == NULL || yuv420 == NULL)
        return;
    memcpy(yuv420, yuv420sp, framesize);
    i = 0;
    for (j = 0; j < framesize / 2; j += 2) {
        yuv420[i + framesize * 5 / 4] = yuv420sp[j + framesize];
        i++;
    }
    i = 0;
    for (j = 1; j < framesize / 2; j += 2) {
        yuv420[i + framesize] = yuv420sp[j + framesize];
        i++;
    }
}

static void I4202NV21(char* yuv420sp, char* yuv420, int width, int height) {
    int i = 0, j = 0;
    int framesize = width * height;

    if (yuv420sp == NULL || yuv420 == NULL)
        return;
    memcpy(yuv420sp, yuv420, framesize);
    i = 0;
    for (j = 0; j < framesize / 2; j += 2) {
        yuv420sp[j + framesize] = yuv420[i + framesize * 5 / 4];
        i++;
    }
    i = 0;
    for (j = 1; j < framesize / 2; j += 2) {
        yuv420sp[j + framesize] = yuv420[i + framesize];
        i++;
    }
}

static void NV122I420(char* yuv420sp, char* yuv420, int width, int height) {
    int i = 0, j = 0;
    int framesize = width * height;

    if (yuv420sp == NULL || yuv420 == NULL)
        return;
    memcpy(yuv420, yuv420sp, framesize);
    i = 0;
    for (j = 1; j < framesize / 2; j += 2) {
        yuv420[i + framesize * 5 / 4] = yuv420sp[j + framesize];
        i++;
    }
    i = 0;
    for (j = 0; j < framesize / 2; j += 2) {
        yuv420[i + framesize] = yuv420sp[j + framesize];
        i++;
    }
}

static void I4202NV12(char* yuv420sp, char* yuv420, int width, int height) {
    int i = 0, j = 0;
    int framesize = width * height;

    if (yuv420sp == NULL || yuv420 == NULL)
        return;
    memcpy(yuv420sp, yuv420, framesize);
    i = 0;
    for (j = 1; j < framesize / 2; j += 2) {
        yuv420sp[j + framesize] = yuv420[i + framesize * 5 / 4];
        i++;
    }
    i = 0;
    for (j = 0; j < framesize / 2; j += 2) {
        yuv420sp[j + framesize] = yuv420[i + framesize];
        i++;
    }
}

void dump_yuv(char *name,int frameid, char *ext, uint8_t *buffer, int width,int height)
{
       char file_name[64];
       snprintf(file_name, sizeof(file_name),"/data/misc/camera/%s_%04d.%s",name, frameid, ext);
       FILE *file = fopen(file_name,"wb+");
       fwrite(buffer,sizeof(uint8_t),(width*height*3) >> 1,file);
       fclose(file);
}


static int meigcam_photo_mkdirs(const char *sPathName)
{
  char   DirName[256];
  strcpy(DirName,   sPathName);
  int   i,len   =   strlen(DirName);
  if(DirName[len-1]!='/')
  strcat(DirName,   "/");
  len   =   strlen(DirName);
  for(i=1;   i<len;   i++){
   if(DirName[i]=='/'){
    DirName[i]   =   0;
    if(access(DirName,NULL)!=0){
     if(mkdir(DirName,0755)==-1){
      CDBG("MEIGLOG: mkdir error");
      return   -1;
     }
    }
    DirName[i]   =   '/';
   }
  }
  return   0;
}


char *check_file_name(const char *photo_save_path,
                      const char *photo_serial_num,
                      char *photo_time,
                      int photo_start_num)
{
    char *filename;
    char photoOutFileRoot[77] = "";
    char strs_in_stor[4][10] = {{'.'}, {".."}, {"self"}, {"emulated"}};
    int rc;
    //srand((unsigned)time(0));
    //snprintf(filename, 64, "/data/misc/camera/data_%d.jpeg",rand());


    filename = (char *)malloc(200);
	if (filename == NULL) {
	    CDBG("[xuhao] check_file_name malloc buffer failed \n");
		return NULL;
	}
    /* End number */
    char endNum[] = "00000";
    int num = photo_start_num;
    sprintf(endNum, "%05d", num);
    CDBG("MEIGLOG: endNum = %s\n", endNum);

    /* make photo restore dir */
    struct dirent *all_dir;
    DIR *dirptr = NULL;
    dirptr = opendir("/storage/");
    while (all_dir = readdir(dirptr)){
        // find external SD card
        if ((strcmp(all_dir -> d_name, strs_in_stor[0])) &&
            (strcmp(all_dir -> d_name, strs_in_stor[1])) &&
            (strcmp(all_dir -> d_name, strs_in_stor[2])) &&
            (strcmp(all_dir -> d_name, strs_in_stor[3]))) {
                sprintf(photoOutFileRoot, "%s", photo_save_path);

                if (((access(photoOutFileRoot, F_OK)) == 0)){
                    /* path already exist */
                    CDBG("MEIGLOG: now the outFileRoot is :%s\n", photoOutFileRoot);
                }
                else {
                    /* SD card has no path && make it. */
                    meigcam_photo_mkdirs(photoOutFileRoot);
                    CDBG("MEIGLOG: outFileRoot is :%s \n", photoOutFileRoot);
                }
                break;
            }
        // No SD card, use default path
        else {
            sprintf(photoOutFileRoot, "/sdcard%s", photo_save_path);

            if (((access(photoOutFileRoot, F_OK)) == 0)){
                /* path already exist */
                CDBG("MEIGLOG: now the outFileRoot is :%s\n", photoOutFileRoot);
            }
            else {
                /* no path && make it */
                meigcam_photo_mkdirs(photoOutFileRoot);
                CDBG("MEIGLOG: the outFileRoot is :%s\n", photoOutFileRoot);
            }
        }
    }

    /* complete filename */
    rc = snprintf(filename, 200, "%s%s_%s_%s.JPG", (char *)photoOutFileRoot, (char *)photo_serial_num, photo_time, endNum);
    CDBG("MEIGLOG: filename = %s\n", filename);
    if(rc < 0)
    {
        CDBG("MEIGLOG: check_file_name: snprintf failed, rc = %d\n", rc);
    }
    closedir(dirptr);
    return filename;
}

photo_size_info *liveshot_init(uint8_t *snapshot_cb_buffer, int snapshot_buf_width, int snapshot_buf_height) {

    /* intialize photo_size_info struct ptr */
    photo_size_info *liveshot_info_t;
    liveshot_info_t = &liveshot_scale_dim;

    /* intialize scaled buffer */
    uint8_t *scaled_liveshot_i420_buf = NULL;
    if(scaled_liveshot_i420_buf == NULL) {
        scaled_liveshot_i420_buf = (uint8_t *)malloc((liveshot_scale_dim.width * liveshot_scale_dim.height * 3) >> 1);
        //CDBG("MEGLOG: SNAPSHOT_WIDTH = %d, SNAPSHOT_HEIGHT = %d", liveshot_scale_dim.width, liveshot_scale_dim.height);
    }

    /* I420 scale from picture size to video-snapshot size */
    libyuv::I420Scale(&snapshot_cb_buffer[0], snapshot_buf_width,
                    &snapshot_cb_buffer[ snapshot_buf_width * snapshot_buf_height], snapshot_buf_width >> 1,
                    &snapshot_cb_buffer[(snapshot_buf_width * snapshot_buf_height * 5) >> 2], snapshot_buf_width >> 1,
                    snapshot_buf_width, snapshot_buf_height,
                    &scaled_liveshot_i420_buf[0], liveshot_scale_dim.width,
                    &scaled_liveshot_i420_buf[ liveshot_scale_dim.width * liveshot_scale_dim.height], liveshot_scale_dim.width >> 1,
                    &scaled_liveshot_i420_buf[(liveshot_scale_dim.width * liveshot_scale_dim.height * 5) >> 2], liveshot_scale_dim.width >> 1,
                    liveshot_scale_dim.width, liveshot_scale_dim.height,
                    libyuv::FilterModeEnum::kFilterLinear);

    /* release liveshot original buffer from callback*/
    if(snapshot_cb_buffer != NULL) {
        free(snapshot_cb_buffer);
        snapshot_cb_buffer = NULL;
    }

    /* fill liveshot_info_t */
    liveshot_info_t->buffer_ptr = scaled_liveshot_i420_buf;
    //CDBG("MEGLOG: liveshot_info->buffer_ptr = %p", liveshot_info_t->buffer_ptr);

    /* return ptr */
    return liveshot_info_t;
}

int liveshot_deinit(uint8_t *scaled_liveshot_buf) {

    /* release liveshot buffer */
    if(scaled_liveshot_buf != NULL) {
        free(scaled_liveshot_buf);
        scaled_liveshot_buf = NULL;
        //CDBG("MEGLOG: free scaled_liveshot_buf successfully");
    }

    return 0;
}

int scale_for_liveshot(int video_resolution) {

    switch (video_resolution) {
        case 1:
        {
            liveshot_scale_dim.width  = 1280;
            liveshot_scale_dim.height = 720;
        }
        break;

        case 2:
        {
            liveshot_scale_dim.width  = 848;
            liveshot_scale_dim.height = 480;
        }
        break;

        case 3:
        {
            liveshot_scale_dim.width  = 2304;
            liveshot_scale_dim.height = 1296;
        }
        break;

        default:
        {
            liveshot_scale_dim.width  = 1920;
            liveshot_scale_dim.height = 1080;
        }
    }
    CDBG("MEGLOG: scale_width = %d", liveshot_scale_dim.width);
    CDBG("MEGLOG: scale_height = %d", liveshot_scale_dim.height);

    return 0;
}

int scale_for_snapshot(int pic_width, int pic_height, int multiple, int brust_number) {

#ifdef PHOTO_SCALE_DEBUG
    multiple = 8; // 2,3,4,5,6,7,8
#endif

    CDBG("MEIGLOG: enter in %s\n ", __func__);
    CDBG("MEIGLOG: pic_width: %d, pic_height: %d, multiple: %d\n", pic_width, pic_height, multiple);

    switch (multiple){
        case 1:
        {
            CDBG("MEIGLOG: choose as multiple = 1\n");
            snapshot_scale_dim.width  = A_WIDTH;
            snapshot_scale_dim.height = A_HEIGHT;
        }
        break;

        case 2:
        {
            CDBG("MEIGLOG: choose as multiple = 2\n");
            snapshot_scale_dim.width  = B_WIDTH;
            snapshot_scale_dim.height = B_HEIGHT;
        }
        break;

        case 3:
        {
            CDBG("MEIGLOG: choose as multiple = 3\n");
            snapshot_scale_dim.width  = C_WIDTH;
            snapshot_scale_dim.height = C_HEIGHT;
        }
        break;

        case 4:
        {
            CDBG("MEIGLOG: choose as multiple = 4\n");
            snapshot_scale_dim.width  = D_WIDTH;
            snapshot_scale_dim.height = D_HEIGHT;
        }
        break;

        case 5:
        {
            CDBG("MEIGLOG: choose as multiple = 5\n");
            snapshot_scale_dim.width  = E_WIDTH;
            snapshot_scale_dim.height = E_HEIGHT;
        }
        break;

        case 6:
        {
            CDBG("MEIGLOG: choose as multiple = 6\n");
            snapshot_scale_dim.width  = F_WIDTH;
            snapshot_scale_dim.height = F_HEIGHT;
        }
        break;

        case 7:
        {
            CDBG("MEIGLOG: choose as multiple = 7\n");
            snapshot_scale_dim.width  = G_WIDTH;
            snapshot_scale_dim.height = G_HEIGHT;
        }
        break;

        case 8:
        {
            CDBG("MEIGLOG: choose as multiple = 8\n");
            snapshot_scale_dim.width  = H_WIDTH;
            snapshot_scale_dim.height = H_HEIGHT;
        }
        break;

        default:
        {
            CDBG("MEIGLOG: multiple is Invalid !!! Use Default\n");
            snapshot_scale_dim.width  = DEFAULT_WIDTH;
            snapshot_scale_dim.height = DEFAULT_HEIGHT;
        }
    }

    if(!(scaleModule = ScaleModule::Instance())) {
        CDBG("MEIGLOG: ScaleModule init error!!!\n");
        return -1;
    } /*else {
        if(scaleModule->initBuffer(pic_width, pic_height)) { //if not I420, we need extra buffer;
            scaled_dst_buffer = (unsigned char *)malloc((scale_width * scale_height * 3) >> 1);
            if(scaled_dst_buffer != nullptr)
                f_dst = new FrameObject(scale_width, scale_height, scaled_dst_buffer, YUV_FORMAT::I420);
        }
    }*/

    /************************************************************
     * brust_num: notify how many photos in this phototake
     * brust_pho_count: clear brust count  连拍计数器归零
     * pho_over_alarm : clear JPEG alarm   连拍合成结束标志位清零
     ************************************************************/
    brust_num = brust_number;
    brust_pho_count = 0;
    pho_over_alarm[0]  = '0';

    return 0;
}


/*void scale_deinit() {
    scaleModule->releaseBuffer();
    free(scaled_dst_buffer);
    scaled_dst_buffer = nullptr;
    delete(f_dst);
    return;
}*/


int mm_scale_start(void *pFrameBuffer, int width, int height, int photo_start_num,
                                    const char *photo_save_path,
                                    const char *photo_serial_num,
                                    const char *photo_time_stamp,
                                    bool vidsnap_flag)
{
    //Get Time
    char *filename;
    char photo_time[20];
    sprintf(photo_time, "%s000", photo_time_stamp);
    filename = check_file_name(photo_save_path, photo_serial_num, photo_time, photo_start_num);

    /******************************************************************************
     *brust_pho_count连拍计数器
     *连拍第一张取ASCII字符‘1’=00110001；第二张取‘2’=00110010；第三张取‘4’=00110100
     *三次连拍按“位与”操作，字符为00110111才可判断照片合成完毕
     ******************************************************************************/
    char snapshot_notify;
    brust_pho_count++;
    switch (brust_pho_count) {
        case 1:
        {
            snapshot_notify = 0x31;
        }
        break;

        case 2:
        {
            snapshot_notify = 0x32;
        }
        break;

        case 3:
        {
            snapshot_notify = 0x34;
        }
        break;
    }

    /******************************************************************************
     * Define ptr of structure --> photo_scale_info, 
     * Structure prepared to deliver into thread --> snapshot2JPEG or liveshot2JPEG
     * brust_number   : numbers of photo
     * file_name      : photo name
     * buffer         : photo content
     * width + height : photo size
     * snapshot_notify: photo index in brust mode
     ******************************************************************************/
    ////////////////////////////////////////////////////
    scale_info_ptr = (photo_scale_info *)malloc( sizeof(photo_scale_info) );//&photo_scale_info_t;

    scale_info_ptr->brust_number = brust_num;
    scale_info_ptr->file_name    = filename;
    scale_info_ptr->buffer       = pFrameBuffer;
    scale_info_ptr->width  = width;
    scale_info_ptr->height = height;
    scale_info_ptr->notify = snapshot_notify;
    //dump_yuv("snapshot", 7771, "yuv",(uint8_t *)photo_scale_info_t.buffer, width, height);
    ///////////////////////////////////////////////////////////////////////////////////////


    /******************************************************************
     * vidsnap_flag lead to snapshot or liveshot
     * vidsnap_flag 判断是正常拍照还是摄中拍
     ******************************************************************/
    if (vidsnap_flag) {
        /* start liveshot write JPEG */
        if ( liveshot2JPEG((void *)scale_info_ptr) ) {
            CDBG("MEIGLOG: liveshot write JPEG error!!! liveshot stop");
            return -1;
        }

    } else {
        /* Normal snapshot and scale */
        nKillThread = KILL_THREAD_NO;
        mm_scale_display_pool_status(pThreadPool);

        if(pThreadPool != NULL) {
            pThreadPool->AddWorkLimit((void *)pThreadPool, snapshot2JPEG, (void *)scale_info_ptr);
        } else
            return -1;
    }

    return 0;
}

/* get videonapshot buffer and write into JPEG */
int liveshot2JPEG( void *arg )
{
    /****************************************
     * restore photo_scale_info structure ptr
     ****************************************/
    photo_scale_info *photo_info_t = NULL;
    photo_info_t = (photo_scale_info *)arg;


    /**********************************************
     * back-up the information in photo_scale_info
     **********************************************/
    char *filename;
    filename = (char *)malloc(200);
    if (filename == NULL) {
        CDBG("[xuhao] liveshot2JPEG malloc buffer failed \n");
        return -1;
    }
    memcpy( filename, photo_info_t->file_name, 200);


    /*****************************************************************
     * write_jpeg
     * 合成照片
     *****************************************************************/
    if(!scaleModule->write_jpeg((uint8 *)photo_info_t->buffer, photo_info_t->width, photo_info_t->height, filename)) {
        CDBG("MEIGLOG: can't write jpeg file %s!\n", filename);
        free(filename);
        return -1;
    }

    /******************************
     * property_set JPEG finish
     *****************************/
    char property_ready_str[10] = "ready";
    property_set("photo.complete.flag", property_ready_str);


    /******************************
     * free all memory and return
     *****************************/
    if (filename != NULL) {
        free(filename);
        filename = nullptr;
    }

    if (scale_info_ptr != NULL) {
        free(scale_info_ptr);
        scale_info_ptr = nullptr;
    }

    return 0;
}

void *snapshot2JPEG(void *arg)
{
    /****************************************
     * keep photo_scale_info structure ptr
     ****************************************/
    photo_scale_info *scale_info_t = NULL;
    scale_info_t = (photo_scale_info *)arg;


    /**********************************************
     * back-up the information in photo_scale_info
     **********************************************/
    char *filename;
    filename = (char *)malloc(200);
    if (filename == NULL) {
        CDBG("[xuhao] snapshot2JPEG malloc buffer failed \n");
        return NULL;
    }
	if (scale_info_t->file_name == NULL) {
		CDBG("[xuhao] scale_info_t->file_name is NULL \n");
        return NULL;
	}
    memcpy( filename, scale_info_t->file_name, 200);
	free(scale_info_t->file_name);

    int snapshot_notify = scale_info_t->notify;
    int brust_number = scale_info_t->brust_number;


    /*****************************************************************
     * yuv_scale
     * 插值
     *****************************************************************/
    unsigned char *scaled_dst_buffer = (unsigned char *)malloc((snapshot_scale_dim.width * snapshot_scale_dim.height * 3) >> 1);
    if(scaled_dst_buffer == nullptr){
        CDBG("MEIGLOG: scaled_dst_buffer allocate error!!!\n");
        return NULL;
    }
    /* yuv scale and release buffer */
    scaleModule->yuv_scale((uint8 *)scale_info_t->buffer, IMG_WIDTH, IMG_HEIGHT,
                           scaled_dst_buffer, snapshot_scale_dim.width, snapshot_scale_dim.height, libyuv::FilterModeEnum::kFilterBox);
    if(scale_info_t->buffer != NULL) {
        free(scale_info_t->buffer);
        scale_info_t->buffer = NULL;
    }


    /*****************************************************************
     * write_jpeg
     * 合成照片
     *****************************************************************/
    if(!scaleModule->write_jpeg((uint8 *)scaled_dst_buffer, snapshot_scale_dim.width, snapshot_scale_dim.height, filename)) {
        CDBG("MEIGLOG: can't write jpeg file %s!\n", filename);
        free(filename);
    }


    /*****************************************************************
     * modify property after finishing JPEG
     * JPEG合成结束后，写属性提示上层已完成JPEG编码
     * photo_wanted_str：根据连拍张数判断
     * 连拍一张： 期望“字符与”结果为：00110001 --> 0x31
     * 连拍三张： 期望“字符与”结果为：00110111 --> 0x37
     *****************************************************************/
    pho_over_alarm[0] |= snapshot_notify;
    char photo_wanted_str;
    switch (brust_number) {
        case 1:
        {
            photo_wanted_str = 0x31;
        }
        break;

        case 3:
        {
            photo_wanted_str = 0x37;
        }
        break;
    }

    if ( photo_wanted_str == pho_over_alarm[0] ) {
        char property_ready_str[10] = "ready";
        property_set("photo.complete.flag", property_ready_str);
    }


    /********************************************
     * free all memory and return
     ********************************************/
    if (filename != NULL) {
        free(filename);
        filename = nullptr;
    }

    if (scale_info_ptr != NULL) {
        free(scale_info_ptr);
        scale_info_ptr = nullptr;
    }

    if (scaled_dst_buffer != NULL) {
        free(scaled_dst_buffer);
        scaled_dst_buffer = nullptr;
    }

    nKillThread = KILL_THREAD_JPEG_RESTORE;
    return NULL;
}


void mm_scale_display_pool_status(CThread_pool_t * pPool)
{
    static int nCount = 1;

    CDBG("****************************\n");
    CDBG("nCount = %d\n", nCount++);
    CDBG("max_thread_num = %d\n", pPool->GetThreadMaxNum((void *)pPool));
    CDBG("current_pthread_num = %d\n", pPool->GetCurrentThreadNum((void *)pPool));
    CDBG("current_pthread_task_num = %d\n", pPool->GetCurrentTaskThreadNum((void *)pPool));
    CDBG("current_wait_queue_num = %d\n", pPool->GetCurrentWaitTaskNum((void *)pPool));
    CDBG("****************************\n");
}

#if 0
void *mm_scale_thread(void * arg)
{
    CDBG("MEIGLOG: Stream back thread[%p] is Run!\n",arg);
    while(nKillThread != KILL_THREAD_EXIT)
        usleep(10000000);
    CDBG("Stream back thread is Exit!\n");
    s_pThreadPool->Destroy((void*)s_pThreadPool);
    return NULL;
}

int mm_scale_init()
{
    if(s_pThreadPool == NULL)
    {
      CDBG("MEIGLOG: Thread Pool Creat\n");
      s_pThreadPool = ThreadPoolConstruct(15, 1);
      mm_scale_display_pool_status(s_pThreadPool);
    }
    /**可用AddWorkLimit()替换看执行的效果*/
    s_pThreadPool->AddWorkLimit((void *)s_pThreadPool, mm_scale_thread, (void *)NULL);

    mm_scale_display_pool_status(s_pThreadPool);
    return 0;
}
#endif

//   FILE *fp_snapshot;
//   fp_snapshot = fopen("data/misc/camera/snapshot_777.yuv", "wb+");
//   fwrite(pFrameBuffer, sizeof(uint8), (width * height * 3) >> 1, fp_snapshot);

