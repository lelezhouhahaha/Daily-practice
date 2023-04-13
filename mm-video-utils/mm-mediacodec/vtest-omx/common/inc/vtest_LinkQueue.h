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

#ifndef _VTEST_LINKQUEUE_H
#define _VTEST_LINKQUEUE_H

#include "vt_linkqueue.h"

namespace vtest {

/**
 * @brief LinkQueue class.
 */
class LinkQueue {
public:

    /**
     * @brief Constructor
     *
     * @param nMaxQueueSize Max number of items in queue (size)
     * @param nMaxDataSize Max data size
     */
    LinkQueue();

    /**
     * @brief Destructor
     */
    ~LinkQueue();

public:

    /**
     * @brief Pushes an item onto the queue.
     *
     * @param pData Pointer to the data
     * @param nDataSize Size of the data in bytes
     */
    OMX_ERRORTYPE Enqueue(LQNode *node);

    /**
     * @brief Pops an item from the queue.
     *
     * @param pData Pointer to the data
     * @param nDataSize Size of the data buffer in bytes
     */
    OMX_ERRORTYPE Dequeue(LQNode **node);

    /**
     * @brief Get the size of the queue.
     */
    OMX_S32 GetSize();

    OMX_S32 GetDeltLength();

    OMX_S32 CleanDeltLength();

    OMX_TICKS GetDeltTime();

    OMX_S32 CleanDeltTime();

    OMX_S32 SetState();
    OMX_ERRORTYPE SetTimeThreshold(OMX_S32 time);
	/**
     * @brief Set the Queue handle
     */
    OMX_ERRORTYPE FreeNode(LQNode *node);

private:
    OMX_PTR m_pHandle;
    Mutex *m_pMutex;
};
}

#endif // #ifndef _VTEST_LINKQUEUE_H
