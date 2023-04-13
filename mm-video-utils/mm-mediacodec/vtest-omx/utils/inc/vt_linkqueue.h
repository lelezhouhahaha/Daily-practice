/*-------------------------------------------------------------------
Copyright (c) 2013 Qualcomm Technologies, Inc. All Rights Reserved.
Qualcomm Technologies Proprietary and Confidential

Copyright (c) 2010 The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of The Linux Foundation nor
      the names of its contributors may be used to endorse or promote
      products derived from this software without specific prior written
      permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--------------------------------------------------------------------------*/

#ifndef LINKQUEUE_H
#define LINKQUEUE_H

#ifdef __cplusplus
extern "C" {
#endif
/*========================================================================

                     INCLUDE FILES FOR MODULE

==========================================================================*/
#include <stdio.h>
#include <malloc.h>
#include "OMX_Core.h"

typedef unsigned char ElemType;

typedef struct Node
{
    ElemType* data;
    int data_size;
    OMX_TICKS time;
    struct Node *next;
}LQNode;

typedef struct
{
    LQNode *front;
    LQNode *rear;
    int length;
    int delt_length;
    OMX_TICKS delt_time;
    int time_threshold;
    OMX_BOOL is_full;
}LinkQueue;

int init_linkqueue(void** handle);

int destroy_linkqueue(void* handle);

int is_empity_queue(void* handle);

int get_linkqueue_size(void* handle);

int get_delt_length(void* handle);

int clean_delt_length(void* handle);

OMX_TICKS get_delt_time(void* handle);

int clean_delt_time(void* handle);

int set_time_threshold(void* handle, int time);

int set_state(void* handle);

int en_linkqueue(void* handle,LQNode *node);

int de_linkqueue(void* handle,LQNode **node);

int free_node(LQNode *node);

#ifdef __cplusplus
}
#endif

#endif // #ifndef LINKQUEUE_H
