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

#include "vtest_Debug.h"
#include "vtest_Mutex.h"
#include "vtest_LinkQueue.h"
#include "vt_linkqueue.h"

namespace vtest {

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
LinkQueue::LinkQueue()
    :  m_pHandle(NULL),
     m_pMutex(new Mutex()) {
    VTEST_MSG_HIGH("constructor");
    if (init_linkqueue((void **)&m_pHandle) != 0) {
        VTEST_MSG_ERROR("failed to create queue");
    }
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
LinkQueue::~LinkQueue() {
    VTEST_MSG_HIGH("~destructor");
    if (m_pMutex != NULL) delete m_pMutex;
    if (destroy_linkqueue((void *)m_pHandle) != 0) {
        VTEST_MSG_ERROR("failed to create queue");
    }
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_ERRORTYPE LinkQueue::Enqueue(LQNode *node) {
    VTEST_MSG_LOW("Enqueue");
    OMX_ERRORTYPE result = OMX_ErrorNone;
    // lock mutex
    m_pMutex->Lock();
    if (en_linkqueue((void *)m_pHandle, node) != 0) {
        VTEST_MSG_LOW("failed to push onto queue");
        result = OMX_ErrorUndefined;
    }
    // unlock mutex
    m_pMutex->UnLock();

    return result;
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_ERRORTYPE LinkQueue::Dequeue(LQNode **node) {
    VTEST_MSG_LOW("Dequeue");
    OMX_ERRORTYPE result = OMX_ErrorNone;
    // lock mutex
    m_pMutex->Lock();
    if (de_linkqueue((void *)m_pHandle, node) != 0) {
        VTEST_MSG_LOW("failed to push onto queue");
        result = OMX_ErrorUndefined;
    }
    // unlock mutex
    m_pMutex->UnLock();

    return result;
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_S32 LinkQueue::GetSize() {
    return (OMX_S32)get_linkqueue_size((void *)m_pHandle);
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_S32 LinkQueue::GetDeltLength() {
    return (OMX_S32)get_delt_length((void *)m_pHandle);
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_S32 LinkQueue::CleanDeltLength() {
    return (OMX_S32)clean_delt_length((void *)m_pHandle);
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_TICKS LinkQueue::GetDeltTime() {
    return (OMX_TICKS)get_delt_time((void *)m_pHandle);
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_S32 LinkQueue::CleanDeltTime() {
    return clean_delt_time((void *)m_pHandle);
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_S32 LinkQueue::SetState() {
    return set_state((void *)m_pHandle);
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_ERRORTYPE LinkQueue::SetTimeThreshold(OMX_S32 time) {

	VTEST_MSG_LOW("Set overtime");
    OMX_ERRORTYPE result = OMX_ErrorNone;
	set_time_threshold((void *)m_pHandle, time);

	return result;
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_ERRORTYPE LinkQueue::FreeNode(LQNode *node) {

	VTEST_MSG_LOW("SetLinkQueue");
    OMX_ERRORTYPE result = OMX_ErrorNone;
	free_node(node);

	return result;
}

} // namespace vtest
