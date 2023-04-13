/*-------------------------------------------------------------------
Copyright (c) 2013-2014 Qualcomm Technologies, Inc. All Rights Reserved.
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

#ifndef _VTEST_SIGNAL_QUEUE_H
#define _VTEST_SIGNAL_QUEUE_H

#include "OMX_Core.h"

namespace vtest {

class Signal;
class Mutex;
class Queue;

/**
 * @brief Signal queue class.
 *
 * Reader will block on Pop until the writer adds an item
 * to the queue via the Push method.
 */
class SignalQueue {

public:
    /**
     * @brief Constructor
     *
     * @param nMaxQueueSize Max number of items in queue (size)
     * @param nMaxDataSize Max data size
     */
    SignalQueue(OMX_S32 nMaxQueueSize, OMX_S32 nMaxDataSize);

    /**
     * @brief Destructor
     */
    ~SignalQueue();

private:
    SignalQueue(); // private default constructor

public:
    /**
     * @brief Pushes an item onto the queue.
     *
     * @param pData Pointer to the data
     * @param nDataSize Size of the data in bytes
     */
    OMX_ERRORTYPE Push(OMX_PTR pData, OMX_S32 nDataSize);

    /**
     * @brief Pops an item from the queue.
     *
     * @param pData Pointer to the data
     * @param nDataSize Size of the data buffer in bytes
     * @param nTimeoutMillis Milliseconds before timeout. Specify 0 for infinite.
     */
    OMX_ERRORTYPE Pop(OMX_PTR pData, OMX_S32 nDataSize, OMX_S32 nTimeoutMillis);

    /**
     * @brief Peeks at the item at the head of the queue
     *
     * Method will not block and nothing will be removed from the queue
     *
     * @param pData Pointer to the data
     * @param nDataSize Size of the data in bytes
     *
     * @return OMX_ErrorNotReady if there is no data
     */
    OMX_ERRORTYPE Peek(OMX_PTR pData, OMX_S32 nDataSize);

    /**
     * @brief Get the size of the queue.
     */
    OMX_S32 GetSize();

    /**
     * @brief Broadcast the signal to get out of Pop(wait)
     */
    OMX_S32 WakeAll();

	/**
     * @brief Set the Queue handle
     */
    OMX_ERRORTYPE SetSignalQueue(OMX_PTR pHandle);

	/**
     * @brief Get the signal handle
     */
	OMX_PTR GetSignalHandle();

private:
    Signal *m_pSignal;
    Mutex *m_pMutex;
    Queue *m_pQueue;
    OMX_BOOL m_bBroadcast;
};

}

#endif // #ifndef _VTEST_SIGNAL_QUEUE_H
