
#include <sys/resource.h>
#include <utils/Log.h>
#include <utils/threads.h>
#include <cutils/properties.h>
#include <utils/Log.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <pthread.h>
#include <string.h>
#include <errno.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <time.h>
#include <poll.h>
#include <dlfcn.h>
#include <math.h>
#include <sys/types.h> 
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>

#include "../CameraInter.h"

#define IMAGE_FORMAT_JPEG           0x100
#define IMAGE_FORMAT_YUV_420_888    0x23
#define IMAGE_FORMAT_YUV_NV21       0x20


#define TEST_WIDTH  1280
#define TEST_HEIGHT 720


void DumpBuffer(char *patch, uint8_t *y,uint8_t *u,uint8_t *v,int32_t ylen,int32_t ulen,int32_t vlen) {
  FILE* file = fopen(patch,"w+");
  if (file != nullptr) {
    fwrite(y, sizeof(uint8_t), ylen, file);
    //fseek(file,0,SEEK_END);
    //fwrite(u, sizeof(uint8_t), ulen, file);
    //fseek(file,0,SEEK_END);
    //fwrite(v, sizeof(uint8_t), vlen, file);
    fclose(file);
  }
}

static void OnDataCallBack(AImageReader *reader){
#if 0
    media_status_t ret;
    AImage* image = NULL;
    int32_t format = -1;
    int32_t width = -1, height = -1;
    char dumpFilePath[512];
    int32_t numPlanes;
    int32_t i = 0;

    uint8_t *data[3];
    int32_t datalen[3];
    int32_t pixelStride[3];
    int32_t rowStride[3];

    if (!reader) {
      return;
    }

    ret = AImageReader_acquireNextImage(reader, &image);
    if (ret != AMEDIA_OK) {
        ALOGE("CameraNativeManager::ImageCallback Failed to get image");
        return;
    }

    ret = AImage_getFormat(image, &format);
    if (ret != AMEDIA_OK || format == -1) {
        AImage_delete(image);
        ALOGE("CameraNativeManager:: get format for image %p failed! ret: %d, format %d",image, ret, format);
        return;
    }

    ret = AImage_getWidth(image, &width);
    if (ret != AMEDIA_OK || width <= 0) {
        AImage_delete(image);
        ALOGE("%s: get width for image %p failed! ret: %d, width %d",
                     __FUNCTION__, image, ret, width);
        return;
    }

    ret = AImage_getHeight(image, &height);
    if (ret != AMEDIA_OK || height <= 0) {
        AImage_delete(image);
        ALOGE("%s: get height for image %p failed! ret: %d, width %d",
                     __FUNCTION__, image, ret, height);
        return;
    }

    ret  = AImage_getNumberOfPlanes(image,&numPlanes);

    for (i = 0; i < numPlanes; i++) {
      AImage_getPlaneData(image,i,&data[i],&datalen[i]);
      AImage_getPlaneRowStride(image,i,&rowStride[i]);
      AImage_getPlanePixelStride(image,i,&pixelStride[i]);
      ALOGI("%s,+++++ 11111111 Get Capture imge Plane[%d] [%dx%d] rowStride[%d] pixelStride[%d] length[%d]\n", __FUNCTION__,i,width,height,
           rowStride[i],
           pixelStride[i],
           datalen[i]);

    }
    //ALOGI("%s,+++++ 11111111 Get Capture imge [%dx%d] Planes:%d\n", __FUNCTION__,width,height,numPlanes);

    sprintf(dumpFilePath, "%s/%dx%d.yuv", "/sdcard", width, height);

    if(format == IMAGE_FORMAT_YUV_420_888) {
        //DumpBuffer(dumpFilePath,data[i],data_u,data_v,ylen,ulen,vlen);
    }

    AImage_delete(image);
#endif
  ALOGI("xxxxxxxxxxxxxxx Get Capture imge.....");
}

int main(int argc, char* argv[])
{
    bool ret = true;
    int i;
    int32_t CameraId = 0;
    int32_t witdth = TEST_WIDTH;
    int32_t height = TEST_HEIGHT;
    int32_t format = IMAGE_FORMAT_YUV_420_888;

    CameraHandle *pHandle = NULL;

    printf("[cameraid] [w] [h] [foramt]\n");

    if (argv[1]) {
        if(!strcmp(argv[1],"0")) {
          CameraId = 0;
        } else if(!strcmp(argv[1],"1")) {
          CameraId = 1;
        } else if(!strcmp(argv[1],"2")) {
          CameraId = 2;
        } else {
          printf("invalid cameraid\n");
          return 0;
        }
    }

    if (argv[2]) {
      witdth = atoi(argv[2]);
    }

    if (argv[3]) {
      height = atoi(argv[3]);
    }

    if (argv[4]) {
      if(!strcmp(argv[4],"YUV")) {
        format = IMAGE_FORMAT_YUV_420_888;
      } else if (!strcmp(argv[4],"JPEG")){
        format = IMAGE_FORMAT_JPEG;
      } else {
        printf("invalid foramt\n");
        return  0;
      }
    }

    printf("start test cameraid=%d size[%dx%d] format:%d\n", CameraId,witdth,height,format);

    if (!CreateCameraHandle(&pHandle)) {
        printf("CreateCameraHandle failed\n");
        return 0;
    }

    ret = pHandle->RegisterCameraServers();
    if (!ret) {
       printf("RegisterCameraServers failed\n");
       goto exit;
    }

    ret = pHandle->Open(CameraId,witdth,height,format);
    if (!ret) {
       printf("Open failed\n");
       goto open_failed;
    }

    do {
        char c;
        printf("e:exit  a:stop   b:start  s:switchsize\n");
        c = getchar();
        if (c == 'a') {
          pHandle->stopCapture(CameraId);
        } else if (c == 'b') {
          pHandle->SnapShort(CameraId,OnDataCallBack,0);
        } else if (c == 's') {
        }else if(c == 'e') {
            break;
        }
    }while(1);

open_failed:
    if (pHandle) {
        pHandle->Close(CameraId);
    }

exit:
   if (pHandle)
       pHandle->DestoryCameraServers();

   DestoryCameraHandle(pHandle);
   return 0;

}
