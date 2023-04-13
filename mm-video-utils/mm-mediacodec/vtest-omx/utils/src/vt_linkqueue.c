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

/*========================================================================
  Include Files
 =========================================================================*/
#include "vt_linkqueue.h"
#include "vt_debug.h"

int init_linkqueue(void ** handle)
{
    int result = 0;
    if (handle)
    {
        LinkQueue* pQHead = (LinkQueue*) malloc(sizeof(LinkQueue));
        if (!pQHead)
            return 1;

        *handle = (void*) pQHead;
        pQHead->front = pQHead->rear = (LQNode*)malloc(sizeof(LQNode));
        pQHead->length = 0;
        pQHead->delt_length = 0;
        pQHead->delt_time = 0;
        pQHead->is_full = OMX_FALSE;
        if(!pQHead->front)
        {
            VTEST_MSG_ERROR("pQHead->front malloc error!");
            return 1;
        }

        pQHead->front->data = NULL;
        pQHead->front->data_size = 0;
        pQHead->front->time = 0;
        pQHead->front->next = NULL;
    }
    else
   {
        VTEST_MSG_ERROR("invalid handle");
        result = 1;
   }

    return result;
}

int destroy_linkqueue(void* handle)
{
    int result = 0;
    LQNode* p = NULL;
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        while(pQHead->front)
        {
            p = pQHead->front->next;
            VTEST_MSG_LOW("node addr: %p , data addr: %p, size: %d, timestamp : %lld, queue size: %d.", pQHead->front, pQHead->front->data, pQHead->front->data_size, pQHead->front->time, pQHead->length);
            free(pQHead->front->data);
            free(pQHead->front);
            pQHead->front = p;
            pQHead->length --;
        }
        pQHead->rear = NULL;
        free(pQHead);
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        result = 1;
    }
   return result;
}

int is_empity_queue(void* handle)
{
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        if(pQHead->front == pQHead->rear)
        {
            VTEST_MSG_ERROR("LinkQueue is blank.");
            return 1;
        }
        else
            return 0;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }

}

int get_linkqueue_size(void* handle)
{
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        VTEST_MSG_LOW("LinkQueue length = %d.", pQHead->length);
        return pQHead->length;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
}

int get_delt_length(void* handle)
{
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        VTEST_MSG_LOW("LinkQueue delt length = %d.", pQHead->delt_length);
        return pQHead->delt_length;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
}

int clean_delt_length(void* handle)
{
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        pQHead->delt_length = 0;
        return 0;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
}

OMX_TICKS get_delt_time(void* handle)
{
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        VTEST_MSG_LOW("LinkQueue delt time = %lld.", pQHead->delt_time);
        return pQHead->delt_time;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
}

int clean_delt_time(void* handle)
{
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        pQHead->delt_time = 0;
        return 0;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
}

int set_time_threshold(void* handle, int time)
{
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        VTEST_MSG_LOW("LinkQueue time threshold = %d.", time);
        pQHead->time_threshold = time;
        return 0;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
}

int set_state(void* handle)
{
    if(handle)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        VTEST_MSG_HIGH("LinkQueue is full.");
        pQHead->is_full = OMX_TRUE;
        return 0;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
}

int en_linkqueue(void* handle,LQNode *source)
{
    if(handle && source)
    {
        int i ;
        LinkQueue* pQHead = (LinkQueue*) handle;
        LQNode  *temp = (LQNode*)malloc(sizeof(LQNode));
        if(!temp)
        {
            VTEST_MSG_ERROR("temp malloc error!\n");
            return 1;
        }
        pQHead->length ++;
        temp->data = (ElemType*)malloc(sizeof(ElemType)*source->data_size);
        memcpy(temp->data, source->data, source->data_size);
        temp->data_size = source->data_size;
        temp->time = source->time;
        temp->next = NULL;

        pQHead->rear->next = temp;
        pQHead->rear = temp;
        VTEST_MSG_LOW("node addr: %p , data addr: %p, size: %d, timestamp : %lld, queue size: %d.", temp, temp->data, temp->data_size, temp->time, pQHead->length);
        if((source->time - pQHead->front->next->time)/1000 > pQHead->time_threshold && !pQHead->is_full) {
            for(i = 0; i < 6; ++i) {
                de_linkqueue(handle, &temp);
                pQHead->delt_time += (temp->next->time - temp->time);
                VTEST_MSG_LOW("OVERTIME delt time: %lld .", pQHead->delt_time);
                pQHead->delt_length++;
                VTEST_MSG_LOW("OVERTIME node addr: %p , data addr: %p, size: %d, queue size: %d.", temp, temp->data, temp->data_size, pQHead->length);
                VTEST_MSG_LOW("OVERTIME front time: %lld , rear time: %lld.", temp->time, source->time);
                free_node(temp);
            }
        }
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
    return 0;
}

int de_linkqueue(void* handle,LQNode **node)
{
    if(handle && node)
    {
        LinkQueue* pQHead = (LinkQueue*) handle;
        if(is_empity_queue(pQHead)==1)
        {
            VTEST_MSG_ERROR("queue is NULL!\n");
            return 1;
        }
        *node = pQHead->front->next;

        if(pQHead->front->next == pQHead->rear)
            pQHead->rear = pQHead->front;

        pQHead->front->next = (*node)->next;
        VTEST_MSG_LOW("node addr: %p , data addr: %p, size: %d, queue size: %d.", *node, (*node)->data, (*node)->data_size, pQHead->length);

        pQHead->length --;
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
    return 0;
}

int free_node(LQNode *node)
{
    if(node)
    {
        if(node->data)
            free(node->data);
        node->next = NULL;
        free(node);
        VTEST_MSG_LOW("node addr: %p , data addr: %p.", node, node->data);
    }
    else
    {
        VTEST_MSG_ERROR("invalid handle");
        return 1;
    }
    return 0;
}