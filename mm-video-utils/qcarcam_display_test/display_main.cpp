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
#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <cutils/properties.h>
#include <sys/resource.h>
#include <utils/Log.h>
#include <utils/threads.h>
#include "display_interface.h"
#define inputs_num 1
#define DISPLAY_WIDTH_320 320
#define DISPLAY_HEIGHT_180 180
extern "C"{
#include "display_main.h"
}
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

using namespace android;
static int stopDisplayShow=false;
pthread_attr_t attr;
pthread_t thread;

void *display_show_thread(void * display_buf)
{
    stopDisplayShow=false;

    CDBG("%s: BEGIN - addr=%p, display dimesion: %d x %d\n", __func__,
        display_buf, DISPLAY_WIDTH_320, DISPLAY_HEIGHT_180);
    if(inputs_num == 0 || inputs_num > 4){
        CDBG("%s erro set input num Must be greater than 0 and less than 3  \n", __func__);
        return NULL;
    }

    sp<Qcarcam_display> m_Qcarcam_display_0 = new Qcarcam_display(0, display_buf, DISPLAY_WIDTH_320, DISPLAY_HEIGHT_180,&stopDisplayShow);
/*
    m_Qcarcam_display_0->stopDisplayShow=&stopDisplayShow;

    if(inputs_num > 1){
        sp<Qcarcam_display> m_Qcarcam_display_1 = new Qcarcam_display(1, app_buf.data, app_buf.width, app_buf.height);
    }
    
    if(inputs_num > 2){
        sp<Qcarcam_display> m_Qcarcam_display_2 = new Qcarcam_display(2, app_buf.data, app_buf.width, app_buf.height);
    }
    
    if(inputs_num > 3){
        sp<Qcarcam_display> m_Qcarcam_display_3 = new Qcarcam_display(3, app_buf.data, app_buf.width, app_buf.height);
    }
*/
    //IPCThreadState::self()->joinThreadPool();
    //pthread_detach(pthread_self());

    m_Qcarcam_display_0 = NULL;
    return NULL;
}

int stop_display_show_data()
{
    stopDisplayShow=true;
    CDBG("BEGIN - %s\n",
        __func__);
    return 0;
}


// ---------------------------------------------------------------------------------
// Fourth: Thread initialization
// ---------------------------------------------------------------------------------
int start_display_show_data(void *display_buf)
{
    int rc = 0;
    pthread_attr_init (&attr);
    pthread_attr_setdetachstate (&attr, PTHREAD_CREATE_DETACHED);
    rc = pthread_create(&thread, &attr, display_show_thread, display_buf);
    usleep(40*1000);
    pthread_attr_destroy (&attr);

    if(rc != 0)
    {
        CDBG("%s: Create Display data pthread error!\n", __func__);
        return rc;
    }

    CDBG("%s: END -> %d\n",__func__, __LINE__);
    return rc;
}

