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
using namespace android;

#ifndef __QCARCAM_DISPLAY_MAIN_H__
#define __QCARCAM_DISPLAY_MAIN_H__
/*
typedef struct {
    void *data;
    int width;
    int height;
} mm_display_app_buf_t;
*/

extern "C" int start_display_show_data(void *display_buf);
extern "C" int stop_display_show_data();

#endif // __QCARCAM_DISPLAY_MAIN_H__
