//
// Created by Administrator on 2018/10/7 0007.
//
#include "ScaleModule.h"
#include "libjpeg/jpeglib.h"
#include <android/log.h>
#include <jni.h>
#include <utils/Log.h>
#include <sys/stat.h>  // change file R/W permission
#define TAG "LIBYUV"

/***********************************
I420: YYYYYYYY UU VV    =>YUV420P
YV12: YYYYYYYY VV UU    =>YUV420P
NV12: YYYYYYYY UVUV     =>YUV420SP
NV21: YYYYYYYY VUVU     =>YUV420SP
***********************************/

ScaleModule *ScaleModule::_instance = nullptr;

ScaleModule *ScaleModule::Instance() {
    if(nullptr == _instance) {
        _instance = new ScaleModule();
    }
    return _instance;
}

bool ScaleModule::yuv_scale(uint8 *src_buffer, int src_w, int src_h,
                            uint8 *dst_buffer, int dst_w, int dst_h, libyuv::FilterModeEnum pfmode) {
    /* remove to avoid new frameobject
     * uint32 psrc_w = src_obj->getFrameWidth();
     * uint32 psrc_h = src_obj->getFrameHeight();
     * uint32 pdst_w = dst_obj->getFrameWidth();
     * uint32 pdst_h = dst_obj->getFrameHeight();
     *
     * const uint8 *psrc_buf = src_obj->getFrameBuffer();
     * uint8 *pdst_buf = dst_obj->getFrameBuffer();
     * ALOGE();
     */
    YUV_FORMAT yformat = YUV_FORMAT::I420;//src_obj->getFormat();

    if(i420_buf == nullptr && yformat != YUV_FORMAT::I420) {
        printf("Please init buffer first!!!");
        return false;
    }

    uint32 psrc_w = src_w;
    uint32 psrc_h = src_h;
    uint32 pdst_w = dst_w;
    uint32 pdst_h = dst_h;

    const uint8 *psrc_buf = src_buffer;
    uint8 *pdst_buf = dst_buffer;

#if DEBUG
    const char nv21file[20] = "/sdcard/nv21.nv21";
    const char i420file[20] = "/sdcard/i420.i420";
    FILE *f21 = fopen(nv21file,"wb+");
    FILE *f420 = fopen(i420file,"wb+");
    fwrite(psrc_buf,sizeof(uint8),(psrc_w*psrc_h*3) >> 1,f21);
#endif
    if(yformat == YUV_FORMAT::NV21) {
        libyuv::NV21ToI420(&psrc_buf[0],                           psrc_w,
                           &psrc_buf[psrc_w * psrc_h],             psrc_w,
                           &i420_buf[0],                          psrc_w,
                           &i420_buf[psrc_w * psrc_h],            psrc_w >> 1,
                           &i420_buf[(psrc_w * psrc_h * 5) >> 2], psrc_w >> 1,
                           psrc_w, psrc_h);
    } else if(yformat == YUV_FORMAT::NV12){
        libyuv::NV12ToI420(&psrc_buf[0],                           psrc_w,
                           &psrc_buf[psrc_w * psrc_h],             psrc_w,
                           &i420_buf[0],                          psrc_w,
                           &i420_buf[psrc_w * psrc_h],            psrc_w >> 1,
                           &i420_buf[(psrc_w * psrc_h * 5) >> 2], psrc_w >> 1,
                           psrc_w, psrc_h);
    } else if(yformat == YUV_FORMAT::I420){
        libyuv::I420Scale(&psrc_buf[0],                          psrc_w,
                          &psrc_buf[psrc_w * psrc_h],            psrc_w >> 1,
                          &psrc_buf[(psrc_w * psrc_h * 5) >> 2], psrc_w >> 1,
                          psrc_w, psrc_h,
                          &pdst_buf[0],                          pdst_w,
                          &pdst_buf[pdst_w * pdst_h],            pdst_w >> 1,
                          &pdst_buf[(pdst_w * pdst_h * 5) >> 2], pdst_w >> 1,
                          pdst_w, pdst_h,
                          pfmode);
        return true;
    }
#if DEBUG
    fwrite(i420_buf1,sizeof(uint8),(psrc_w*psrc_h*3) >> 1,f420);
    fclose(f420);
    fclose(f21);
#endif
    libyuv::I420Scale(&i420_buf[0],                          psrc_w,
                      &i420_buf[psrc_w * psrc_h],            psrc_w >> 1,
                      &i420_buf[(psrc_w * psrc_h * 5) >> 2], psrc_w >> 1,
                      psrc_w, psrc_h,
                      &pdst_buf[0],                          pdst_w,
                      &pdst_buf[pdst_w * pdst_h],            pdst_w >> 1,
                      &pdst_buf[(pdst_w * pdst_h * 5) >> 2], pdst_w >> 1,
                      pdst_w, pdst_h,
                      pfmode);
    return true;
}

bool ScaleModule::write_jpeg(uint8 *dst_buffer, int dst_w, int dst_h,const char *filename)
{
    FILE *fJpg;
    /* remove to avoid new frameobject
     * int width = src_obj->getFrameWidth();
     * int height = src_obj->getFrameHeight();
     * unsigned char *pYUVBuffer = src_obj->getFrameBuffer();
     */
    int width = dst_w;
    int height = dst_h;
    unsigned char *pYUVBuffer = dst_buffer;

    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;
    JSAMPROW row_pointer[1];
    int row_stride;
    int i = 0, j = 0;
    unsigned char yuvbuf[width * 3];
    unsigned char *pY, *pU, *pV;
    int ulen;

    ulen = width * height / 4;

    if(pYUVBuffer == NULL){
        return false;
    }

    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_compress(&cinfo);
    fJpg = fopen(filename, "wb");
    //int err = fchmod((int)fJpg, 600);  change file R/W permission
    if(fJpg == NULL){
        jpeg_destroy_compress(&cinfo);
        return false;
    }

    jpeg_stdio_dest(&cinfo, fJpg);
    cinfo.image_width = width;
    cinfo.image_height = height;
    cinfo.input_components = 3;
    cinfo.in_color_space = JCS_YCbCr;
    cinfo.dct_method = JDCT_ISLOW;
    jpeg_set_defaults(&cinfo);


    jpeg_set_quality(&cinfo, 99, TRUE);

    jpeg_start_compress(&cinfo, TRUE);
    row_stride = cinfo.image_width * 3;

    pY = pYUVBuffer;
    pU = pYUVBuffer + width*height;
    pV = pYUVBuffer + width*height + ulen;
    j = 0;
    while (cinfo.next_scanline < cinfo.image_height) {
        /*yyyy...uu..vv*/
        if(j % 2 == 1){
            pU = pYUVBuffer + width*height + width / 2 * (j / 2);
            pV = pYUVBuffer + width*height * 5 / 4 + width / 2 *(j / 2);

        }
        for(i = 0; i < width; i += 2){
            yuvbuf[i*3] = *pY++;
            yuvbuf[i*3 + 1] = *pU;
            yuvbuf[i*3 + 2] = *pV;

            yuvbuf[i*3 + 3] = *pY++;
            yuvbuf[i*3 + 4] = *pU++;
            yuvbuf[i*3 + 5] = *pV++;
        }

        row_pointer[0] = yuvbuf;
        (void) jpeg_write_scanlines(&cinfo, row_pointer, 1);
        j++;

    }
    jpeg_finish_compress(&cinfo);
    jpeg_destroy_compress(&cinfo);
    fclose(fJpg);
    //err = fchmod((int)fJpg, 777);  change file R/W permission
    return true;
}
