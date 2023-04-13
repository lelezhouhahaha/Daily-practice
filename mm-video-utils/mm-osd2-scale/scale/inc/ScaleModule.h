//
// Created by Administrator on 2018/10/7 0007.
//

#ifndef SCALEMODULE_SCALEMODULE_H
#define SCALEMODULE_SCALEMODULE_H
#include <libyuv.h>
#include <string>
#include <android/log.h>

#define TAG "SCALE_MODULE"

enum YUV_FORMAT {
    NV12 = 0x0001,
    NV21 = 0x0002,
    I420 = 0x0003
};

class FrameObject {
private:
    YUV_FORMAT yuv_format;
public:
    uint32 f_width;
    uint32 f_height;
    uint8 *f_buffer;

    FrameObject(uint32 f_width, uint32 f_height,uint8 *f_buffer,
                YUV_FORMAT yuvformat):f_width(0),f_height(0),f_buffer(nullptr),yuv_format(NV21) {
        this->yuv_format = yuvformat;
        this->f_width = f_width;
        this->f_height = f_height;
        this->f_buffer = f_buffer;
    }

    YUV_FORMAT getFormat() {
        return yuv_format;
    }

    uint32 getFrameWidth() {
        return this->f_width;
    }

    uint32 getFrameHeight() {
        return this->f_height;
    }

    uint8 *getFrameBuffer() {
        return this->f_buffer;
    }
};

class ScaleModule {
private:
    //uint8 *i420_buf = nullptr;
    ScaleModule() {
        printf("Create ScaleModule!\n");
        //__android_log_print(ANDROID_LOG_INFO,TAG,"ScaleModule");
    };
    ~ScaleModule() {
        printf("~ScaleModule!\n");
        //__android_log_print(ANDROID_LOG_INFO,TAG,"~ScaleModule");
        if(i420_buf != nullptr) {
            __android_log_print(ANDROID_LOG_INFO,TAG,"Free i420 buffer");
            free(i420_buf);
            i420_buf = nullptr;
        }
    };

    static ScaleModule *_instance;

public:
    static ScaleModule *Instance();
    uint8 *i420_buf = nullptr;

    bool initBuffer(uint32 dstw,uint32 dsth) {
        i420_buf = (uint8 *)malloc((dstw * dsth * 3) >> 1);
        if(i420_buf == nullptr) {
            return false;
        } else {
            return true;
        }
    }

    void releaseBuffer() {
        if(i420_buf != nullptr) {
            printf("release i420 buffer!\n");
            free(i420_buf);
            i420_buf = nullptr;
        }
    }

    bool yuv_scale(uint8 *src_buffer, int src_w, int src_h, uint8 *dst_buffer, int dst_w, int dst_h, libyuv::FilterModeEnum pfmode);
    bool write_jpeg(uint8 *dst_buffer, int dst_w, int dst_h, const char *filename);

};
#endif //SCALEMODULE_SCALEMODULE_H
