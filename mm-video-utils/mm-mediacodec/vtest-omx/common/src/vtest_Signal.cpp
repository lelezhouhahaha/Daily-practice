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

#include "vtest_Signal.h"
#include "vtest_Debug.h"
#include "vt_signal.h"

namespace vtest {

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
Signal::Signal()
    : m_pSignal(NULL) {

    if (vt_signal_create(&m_pSignal) != 0) {
        VTEST_MSG_ERROR("error creating signal");
    }
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
Signal::~Signal() {

    if (vt_signal_destroy(m_pSignal) != 0) {
        VTEST_MSG_ERROR("error destroying signal");
    }
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_ERRORTYPE Signal::Set() {

    OMX_ERRORTYPE result = OMX_ErrorNone;

    if (vt_signal_set(m_pSignal) != 0) {
        VTEST_MSG_ERROR("error setting signal");
        result = OMX_ErrorUndefined;
    }
    return result;
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_ERRORTYPE Signal::Wait(OMX_S32 nTimeoutMillis) {

    OMX_ERRORTYPE result = OMX_ErrorNone;
	VTEST_MSG_LOW("start to wait for signal");
    int ret = vt_signal_wait(m_pSignal, (int)nTimeoutMillis);

    if (ret == 2) {
        result = OMX_ErrorTimeout;
    } else if (ret != 0) {
        VTEST_MSG_ERROR("error waiting for signal");
        result = OMX_ErrorUndefined;
    }
	VTEST_MSG_LOW("wait for signal end...");
    return result;
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_ERRORTYPE Signal::Broadcast() {

    OMX_ERRORTYPE result = OMX_ErrorNone;

    if (vt_signal_broadcast(m_pSignal) != 0) {
        VTEST_MSG_ERROR("error broadcasting signal");
        result = OMX_ErrorUndefined;
    }
    return result;
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
OMX_PTR Signal::GetSignal() {
	return m_pSignal;
}

} // namespace vtest
