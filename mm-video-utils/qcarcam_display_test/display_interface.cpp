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

#define LOG_TAG "Display_test"
#include <stdint.h>
#include <sys/inotify.h>
#include <sys/poll.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <math.h>
#include <fcntl.h>
#include <utils/misc.h>
#include <signal.h>
#include <time.h>
#include <cutils/properties.h>
#include <androidfw/AssetManager.h>
#include <binder/IPCThreadState.h>
#include <utils/Atomic.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <ui/PixelFormat.h>
#include <ui/Rect.h>
#include <ui/Region.h>
#include <ui/DisplayInfo.h>
#include <gui/ISurfaceComposer.h>
#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>

// TODO: Fix Skia.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
#include <SkBitmap.h>
#include <SkStream.h>
#include <SkImageDecoder.h>
#pragma GCC diagnostic pop

#include <GLES/gl.h>
#include <GLES/glext.h>
#include <EGL/eglext.h>
#include "display_interface.h"
#include <private/regionalization/Environment.h>

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


#define BUTTON_DISPLAY_LAYER    21010
#define CAMERA_DISPLAY_LAYER    21009
#define BLACK_BACKGROUND_LAYER  21007
static const GLint version_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
};

namespace android {

Qcarcam_display::Qcarcam_display(int channel, void *display_buffer, int width, int height,int *stopDisplay)
{
    int rc = 0;
    mChannel = channel;
    //data=display_buffer;
    mWidth = width;
    mHeight = height;
    CDBG("%s: BEGIN - addr=%p, display dimesion: %d x %d\n", __func__,
        display_buffer, mWidth, mHeight);
    mSession = new SurfaceComposerClient();
    stopDisplayShow = NULL;
    stopDisplayShow = stopDisplay;
    rc = init_display_surface(mWidth, mHeight);
    if(rc!=0)
        goto init_failed;
    usleep(1000);
    start_surface_config();
    do{
        if(display_buffer != NULL)
           start_show(display_buffer);
        else
           usleep(100 * 1000);
        checkExit(stopDisplayShow);
    }while (!exitPending());
    start_surface_deconfig();
    usleep(1000);
    deinit_display_surface();
init_failed:
    return;

}

Qcarcam_display::~Qcarcam_display()
{
}

void Qcarcam_display::onFirstRef()
{
/*
    status_t err = mSession->linkToComposerDeath(this);
    CDBG_IF(err, "linkToComposerDeath failed (%s) ", strerror(-err));
    if (err == NO_ERROR) {
        run("Qcarcam_display", PRIORITY_DISPLAY);
    }
*/
}

void Qcarcam_display::binderDied(const wp<IBinder>&)
{
    // woah, surfaceflinger died!
    ALOGD("SurfaceFlinger died, exiting...");

    // calling requestExit() is not enough here because the Surface code
    // might be blocked on a condition variable that will never be updated.
    kill( getpid(), SIGKILL );
    requestExit();
}

sp<SurfaceComposerClient> Qcarcam_display::session() const
{
    return mSession;
}

status_t Qcarcam_display::readyToRun()
{
/*
    int ret = init_display_surface(mWidth, mHeight);
    if(ret < 0)
        return -1;
*/
    return NO_ERROR;
}

bool Qcarcam_display::threadLoop()
{
/*
    start_surface_config();
    char prop[PROPERTY_VALUE_MAX];
    int display_blank_prop ;

    do{
        property_get("persist.meigcam.display.blank", prop, "0");
        display_blank_prop = atoi(prop);
        if(data != NULL)
            start_show(data);
        else
            usleep(100 * 1000);

        checkExit(stopDisplayShow);
    }while (!exitPending());

    start_surface_deconfig();
    deinit_display_surface();
*/
    return true;
}


void Qcarcam_display::checkExit(int *stopDisplayShow)
{
    if(*stopDisplayShow == true)
        requestExit();
}

int Qcarcam_display::init_display_surface(int dinfo_w, int dinfo_h)
{
    CDBG("%s Display Init.\n", __FUNCTION__);
    /* No binder was used
    sp<IBinder> dtoken(SurfaceComposerClient::getBuiltInDisplay(
            ISurfaceComposer::eDisplayIdMain));
    DisplayInfo dinfo;
    SurfaceComposerClient::getDisplayInfo(dtoken, &dinfo);
    */
    //dinfo_w = 320;//dinfo.w;
    //dinfo_h = 240;//dinfo.h;
    mFlingerSurfaceControl = session()->createSurface(String8("Qcarcam_display"),
                                                dinfo_w,
                                                dinfo_h,
                                                PIXEL_FORMAT_RGB_565);
    SurfaceComposerClient::openGlobalTransaction();
    mFlingerSurfaceControl->setLayer(CAMERA_DISPLAY_LAYER);	//control->setLayer(0x40000000);
    mFlingerSurfaceControl->setSize(dinfo_w, dinfo_h);
    if(mChannel == 0){
        mFlingerSurfaceControl->setPosition(0, 30);
    }else if(mChannel == 1){
        mFlingerSurfaceControl->setPosition(dinfo_w, 0);
    }else if(mChannel == 2){
        mFlingerSurfaceControl->setPosition(0, dinfo_h);
    }else if(mChannel == 3){
        mFlingerSurfaceControl->setPosition(dinfo_w, dinfo_h);
    }
    //control->setPosition(0, 0);
    SurfaceComposerClient::closeGlobalTransaction();

    sp<ANativeWindow> mFlingerSurface = mFlingerSurfaceControl->getSurface();

    const EGLint attribs[] = {
            EGL_RED_SIZE,   8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE,  8,
            EGL_DEPTH_SIZE, 0,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT, EGL_NONE
    };

    EGLint w = 0, h = 0;
    EGLint numConfigs  = 0;
    EGLConfig   mConfig = NULL;
    //EGLConfig config   = NULL;
    //EGLSurface surface = NULL;
    //EGLContext context = NULL;
    CDBG("meig0903 meigdisplay\n");
    mDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    usleep(10 * 1000);
    if (mDisplay == EGL_NO_DISPLAY) {
        CDBG("meig0826 eglGetDisplay returned EGL_NO_DISPLAY.\n");
        return -1;
    }
    if (eglInitialize(mDisplay, NULL, NULL) == EGL_FALSE || eglGetError() != EGL_SUCCESS) {
        CDBG("meigdisplay eglInitialize failed %d eglGetError = %d\n",EGL_FALSE,eglGetError());
        return -1;
    }
    if(EGL_TRUE !=eglChooseConfig(mDisplay, attribs, &mConfig, 1, &numConfigs))
    {
        CDBG("meigdisplay eglChooseConfig failed!");
        return -1;
    }
    CDBG("mConfig: %p numConfigs: %d", mConfig, numConfigs);
    mSurface = eglCreateWindowSurface(mDisplay, mConfig, mFlingerSurface.get(), NULL);
    if(EGL_NO_SURFACE == mSurface){
        CDBG("meigdisplay eglCreateWindowSurface error");
        return -1;
    }
    usleep(5 * 1000);
    mContext = eglCreateContext(mDisplay, mConfig, EGL_NO_CONTEXT, version_attribs);
    if(EGL_NO_CONTEXT == mContext){
        CDBG("meigdisplay eglCreateContext error");
        return -1;
    }
    eglQuerySurface(mDisplay, mSurface, EGL_WIDTH, &w);
    usleep(5 * 1000);
    eglQuerySurface(mDisplay, mSurface, EGL_HEIGHT, &h);
    usleep(5 * 1000);
    if (eglMakeCurrent(mDisplay, mSurface, mSurface, mContext) == EGL_FALSE){
        CDBG("meigdisplay eglMakeCurrent failed\n");
        return -1;
    }

    //mDisplay = display;
    //mContext = context;
    //mSurface = surface;
    //mConfig = config;
    mWidth = w;
    mHeight = h;
    //mFlingerSurfaceControl = control;
    //mFlingerSurface = s;
    return 0;
}

void Qcarcam_display::deinit_display_surface()
{
    glDisableVertexAttribArray(mPositionHandle);
    glDisableVertexAttribArray(mCoordHandle);

    if (mContext != EGL_NO_CONTEXT) {
        CDBG("MEIGDISPLAY eglDestroyContext \n");
        eglDestroyContext(mDisplay, mContext);
    }
    if (mSurface != EGL_NO_SURFACE) {
        CDBG("MEIGDISPLAY eglDestroySurface \n");
        eglDestroySurface(mDisplay, mSurface);
    }
    if (mDisplay != EGL_NO_DISPLAY) {
        CDBG("MEIGDISPLAY mDisplay clear \n");
        eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglTerminate(mDisplay);
    }
    eglReleaseThread();
    if ( NULL != mFlingerSurfaceControl.get() ) {
        CDBG("MEIGDISPLAY mFlingerSurfaceControl clear \n");
        mFlingerSurfaceControl->clear();
        mFlingerSurfaceControl.clear();
    }
    //glDisableVertexAttribArray(mPositionHandle);
    //glDisableVertexAttribArray(mCoordHandle);
    //data = NULL;
    //mSession = NULL;
    if ( NULL != mSession.get() ) {
        CDBG("MEIGDISPLAY mSession.clear() \n");
        mSession->dispose();
        mSession.clear();
    }
    mFlingerSurfaceControl = NULL;
    stopDisplayShow = NULL;
    mDisplay = NULL;
    mContext = NULL;
    mSurface = NULL;
    mSession = NULL;
}

GLuint Qcarcam_display::loadShader(int iShaderType, const char* source)
{
    GLuint  iShader = glCreateShader(iShaderType);
    GLint compiled = 0;
    if(iShader != 0) {
        glShaderSource(iShader, 1, &source, NULL);
        glCompileShader(iShader);
        glGetShaderiv(iShader, GL_COMPILE_STATUS, &compiled);
        if (compiled == 0) {
            CDBG("meigdisplay Could not compile shader %d", iShaderType);
            glDeleteShader(iShader);
            iShader = 0;
        }
    }
    return iShader;
}

GLuint Qcarcam_display::createProgram()
{
    const char* VERTEX_SHADER =
        "attribute vec4 vPosition;\n" \
        "attribute vec2 a_texCoord;\n" \
        "varying vec2 tc;\n" \
        "void main() {\n" \
        "gl_Position = vPosition;" \
        "tc = a_texCoord;\n" \
        "}\n";
    const char* FRAGMENT_BIND_UV_SHADER =
        "precision mediump float;\n" \
        "uniform sampler2D tex_y;\n" \
        "uniform sampler2D tex_uv;\n" \
        "varying vec2 tc;\n" \
        "void main() {\n" \
        "vec3 yuv;\n" \
        "vec3 rgb;\n" \
        "vec2 tmp = vec2(1.0 - tc.y, 1.0 - tc.x);"
        "yuv.x = texture2D(tex_y, tmp).r;\n" \
        "yuv.y = texture2D(tex_uv, tmp).r - 0.5;\n" \
        "yuv.z = texture2D(tex_uv, tmp).a - 0.5;\n" \
        "rgb = mat3( 1,       1,         1,\n" \
        "        0,     -0.344,  1.772,\n" \
        "        1.402, -0.714,   0) * yuv;\n" \
        "gl_FragColor = vec4(rgb.z, rgb.y, rgb.x, 1);\n" \
        "}\n";
    GLuint iVertexShader = loadShader(GL_VERTEX_SHADER, VERTEX_SHADER);
    GLuint iPixelShader = loadShader(GL_FRAGMENT_SHADER, FRAGMENT_BIND_UV_SHADER);
    GLuint program = glCreateProgram();
    GLint linkStatus = 0;
    CDBG("meigdisplay createProgram iVertexShader = %u, iPixelShader = %u, program = %u",
        iVertexShader, iPixelShader, program);
    if(program != 0) {
        glAttachShader(program, iVertexShader);
        glAttachShader(program, iPixelShader);
        glLinkProgram(program);
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus != GL_TRUE) {
            CDBG("meigdisplay createProgram the program link failed");
            glDeleteProgram(program);
            program = 0;
        }
    }
    glDeleteShader(iVertexShader);
    glDeleteShader(iPixelShader);
    CDBG("meigdisplay createProgram return program = %u", program);
    return program;
}

void Qcarcam_display::destroyCameraProgram()
{
    if(mYTexture != 0) {
        glDeleteTextures(1, &mYTexture);
        mYTexture = 0;
    }
    if(mUVTexture != 0) {
        glDeleteTextures(1, &mUVTexture);
        mUVTexture = 0;
    }
    glDeleteProgram(mProgram);
    mVideoWidth = 0;
    mVideoHeight = 0;
    mProgram = 0;
}

void Qcarcam_display::initCameraProgram()
{

    static float VERTICE_BUFFER[] = { -1.0f, 1.0f,0.0f, -1.0f, -1.0f,0.0f, 1.0f, 1.0f,0.0f, 1.0f, -1.0f,0.0f,};
    static float COORD_BUFFER[] = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f,};
    mProgram = createProgram();
    glUseProgram(mProgram);
    mPositionHandle = (GLuint)glGetAttribLocation(mProgram, "vPosition");
    glEnableVertexAttribArray(mPositionHandle);
    glVertexAttribPointer(mPositionHandle, 3, GL_FLOAT, GL_FALSE, 12, VERTICE_BUFFER);

    mCoordHandle = (GLuint)glGetAttribLocation(mProgram, "a_texCoord");
    glEnableVertexAttribArray(mCoordHandle);
    glVertexAttribPointer(mCoordHandle, 2, GL_FLOAT, GL_FALSE, 8, COORD_BUFFER);

    mYHandle = glGetUniformLocation(mProgram, "tex_y");
    glUniform1i(mYHandle, 0);
    mUVHandle = glGetUniformLocation(mProgram, "tex_uv");
    glUniform1i(mUVHandle, 1);

    glGenTextures(1, &mYTexture);
    glGenTextures(1, &mUVTexture);

    glBindTexture(GL_TEXTURE_2D, mYTexture);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, mVideoWidth, mVideoHeight, 0,
                GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);

    glBindTexture(GL_TEXTURE_2D, mUVTexture);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE_ALPHA, mVideoWidth / 2, mVideoHeight / 2, 0,
                GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, NULL);

}

void Qcarcam_display::buildTexture(const unsigned char* y, const unsigned char* uv, int width, int height)
{
    bool videoSizeChanged = (width != mVideoWidth || height != mVideoHeight);
    if(videoSizeChanged) {
        mVideoHeight = height;
        mVideoWidth = width;
    }
    if(videoSizeChanged) {
        //glDeleteTextures(1, &mYTexture);
        //glGenTextures(1, &mYTexture);
        CDBG("meigdisplay %s mYTexture = %u mUVTexture = %u" , __FUNCTION__, mYTexture,mUVTexture);
    }
#if 0
    glBindTexture(GL_TEXTURE_2D, mYTexture);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, mVideoWidth, mVideoHeight, 0,
                GL_LUMINANCE, GL_UNSIGNED_BYTE, y);

    if(videoSizeChanged) {
        //glDeleteTextures(1, &mUVTexture);
        //glGenTextures(1, &mUVTexture);
        CDBG("%s mUVTexture = %u", __FUNCTION__, mUVTexture);
    }
    glBindTexture(GL_TEXTURE_2D, mUVTexture);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE_ALPHA, mVideoWidth / 2, mVideoHeight / 2, 0,
                GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, uv);
    //glFlush();
#endif
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mYTexture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, mVideoWidth, mVideoHeight, 0,
                GL_LUMINANCE, GL_UNSIGNED_BYTE, y);


    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, mUVTexture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE_ALPHA, mVideoWidth / 2, mVideoHeight / 2, 0,
                GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, uv);
    glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    eglSwapBuffers(mDisplay, mSurface);
    if(videoSizeChanged) {
       CDBG("meigdisplay %s mYTexture = %u mUVTexture = %u display success" , __FUNCTION__, mYTexture,mUVTexture);
    }

}

void Qcarcam_display::checkGlError(const char* op)
{
    int error = glGetError();
    if(error != GL_NO_ERROR) {
        CDBG("[%s] error: 0x%x", op, error);
    }
}

#if 0
void Qcarcam_display::drawFrame()
{
    //static float VERTICE_BUFFER[] = { -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, };
    //static float VERTICE_BUFFER[] = { 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, };
    //static float COORD_BUFFER[] = { 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, };
    //static float COORD_BUFFER[] = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, };
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    //glUseProgram(mProgram);
    glVertexAttribPointer(mPositionHandle, 2, GL_FLOAT, false, 8, VERTICE_BUFFER);
    //glEnableVertexAttribArray(mPositionHandle);
    glVertexAttribPointer(mCoordHandle, 2, GL_FLOAT, false, 8, COORD_BUFFER);
    glEnableVertexAttribArray(mCoordHandle);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, mYTexture);
    //glUniform1i(mYHandle, 0);
    checkGlError("glUniform1i0");
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, mUVTexture);
    glUniform1i(mUVHandle, 1);
    checkGlError("glUniform1i1");
    if(mUVTexture!=2881)
      mUVTexture = 2 ;
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    checkGlError("glDrawArrays");
    //glFlush();
    glDisableVertexAttribArray(mPositionHandle);
    glDisableVertexAttribArray(mCoordHandle);
    eglSwapBuffers(mDisplay, mSurface);
}
#endif
int Qcarcam_display::start_surface_config()
{
    /*
    const GLint version_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };
    eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    CDBG("%s eglMakeCurrent opengl 2.0", __FUNCTION__);
    mContext = eglCreateContext(mDisplay, mConfig, NULL, version_attribs);
    if (eglMakeCurrent(mDisplay, mSurface, mSurface, mContext) == EGL_FALSE) {
        CDBG("%s eglMakeCurrent 2.0 error", __FUNCTION__);
        return -1;
    }*/
    initCameraProgram();
    return 0;
}

void Qcarcam_display::UYVYtoNV12(unsigned char *inyuv,unsigned char *outyuv,int width,int height)
{
    int i,j,ysize;
    unsigned char* up = NULL,*yp1 = NULL,*yp2 = NULL,*in1 = NULL,*in2 = NULL;

    ysize = width*height;
    up = outyuv + ysize;

    yp1 = outyuv;
    yp2 = outyuv + width;

    in1 = inyuv;
    in2 = inyuv + 2*width;

    for(i = 0; i < height/2 ; i++){
        if(i){
            yp1 += width;
            yp2 += width;
            in1 += 2*width;
            in2 += 2*width;
        }

        for(j = 0; j < width/2; j++){
            *up++ = *(in2 + 2);//*in1
            *yp1++ = *(in1 + 1);
            *yp1++ = *(in1 + 3);

            *yp2++ = *(in2 + 1);
            *yp2++ = *(in2 + 3);
            *up++ = *in1;//*(in2 + 2)

            in1 += 4;
            in2 += 4;
        }
    }
}

void Qcarcam_display::start_surface_deconfig()
{
    destroyCameraProgram();
    //eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);//init it already has.
    //eglDestroyContext(mDisplay, mContext);
    //mContext = eglCreateContext(mDisplay, mConfig, NULL, NULL);
    //glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    //glClear(GL_COLOR_BUFFER_BIT);
    //eglSwapBuffers(mDisplay, mSurface);
}

int Qcarcam_display::start_show(void *in_buffer)
{
    unsigned char *camera_data = (unsigned char *)in_buffer;

    buildTexture(camera_data,
                camera_data + (mWidth * mHeight),
                mWidth,
                mHeight);
    //drawFrame();
    return 0;
}

}
