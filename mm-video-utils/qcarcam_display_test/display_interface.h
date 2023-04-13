/*
 * Copyright (C) 2007 The Android Open Source Project
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

#ifndef __QCARCAM_DISPLAY_TEST_H__
#define __QCARCAM_DISPLAY_TEST_H__

#include <stdint.h>
#include <sys/types.h>

#include <androidfw/AssetManager.h>
#include <utils/Thread.h>

#include <EGL/egl.h>
#include <GLES/gl.h>
#include <GLES2/gl2.h>

#include <linux/msm_ion.h>
#include <linux/ion.h>
#include <linux/videodev2.h>
//#include "camera_interface.h"
//#include "camera_interface_test.h"

class SkBitmap;

namespace android {

class Surface;
class SurfaceComposerClient;
class SurfaceControl;

// ---------------------------------------------------------------------------

class Qcarcam_display : public Thread, public IBinder::DeathRecipient
{
public:
    //            Qcarcam_display(int channel,Qcarcam_camera* Qcarcam_camera_interface);
    Qcarcam_display(int channel, void *display_buffer, int width, int height,int *stopDisplayShow);
    virtual     ~Qcarcam_display();
    sp<SurfaceComposerClient> session() const;
    int *stopDisplayShow = NULL;


private:
    virtual bool        threadLoop();
    virtual status_t    readyToRun();
    virtual void        onFirstRef();
    virtual void        binderDied(const wp<IBinder>& who);

    int mChannel;
    sp<SurfaceComposerClient>       mSession ;
    //sp<Qcarcam_camera>              Qcarcam_camera_control;
    //void *data = NULL;
    int         mWidth = 0;
    int         mHeight = 0;
    EGLDisplay  mDisplay = NULL;
    EGLContext  mContext = NULL;
    EGLSurface  mSurface = NULL;
    sp<SurfaceControl> mFlingerSurfaceControl;
    //sp<Surface> mFlingerSurface;

    GLuint mProgram = 0;
    //EGLConfig   mConfig;
    GLuint mPositionHandle = 0;
    GLuint mCoordHandle = 0;
    GLuint mYHandle = 0;
    GLuint mUVHandle = 0;
    GLuint mYTexture = 0;
    GLuint mUVTexture = 0;
    int mVideoWidth = 0;
    int mVideoHeight = 0;

    void UYVYtoNV12(unsigned char *inyuv,unsigned char *outyuv,int width,int height);
    void checkGlError(const char* op);

    GLuint createProgram();
    GLuint loadShader(int iShaderType, const char* source);
    void initCameraProgram();
    void destroyCameraProgram();

    int init_display_surface(int dinfo_w,int dinfo_h);
    void deinit_display_surface();

    void start_surface_deconfig();
    int start_surface_config();

    void buildTexture(const unsigned char* y, const unsigned char* uv, int width, int height);
    void drawFrame();
    int start_show(void *in_buffer);
    void checkExit(int *stopDisplayShow);
};
// ---------------------------------------------------------------------------

}; // namespace android

#endif // __QCARCAM_DISPLAY_TEST_H__
