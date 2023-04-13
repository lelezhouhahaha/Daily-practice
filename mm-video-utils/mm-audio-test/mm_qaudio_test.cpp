/*
 * Copyright (c) 2001 Fabrice Bellard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * @file
 * audio encoding with libavcodec API example.
 *
 * @example encode_audio.c
 */


#include "mm_qaudio_test.h"


extern "C" {
static int gCount1=0;
} // end of extern C


/*===========================================================================
 * FUNCTION   : QAudioTest
 *
 * DESCRIPTION: default constructor of QAudioTest
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
QAudioTest::QAudioTest()
{
    initializeAudio();
}

/*===========================================================================
 * FUNCTION   : ~QAudioTest
 *
 * DESCRIPTION: deconstructor of QAudioTest
 *
 * PARAMETERS : none
 *
 * RETURN     : None
 *==========================================================================*/
QAudioTest::~QAudioTest()
{

}

void QAudioTest::start()
{
    start_t = clock();
}

void QAudioTest::stop()
{

}

int64_t QAudioTest::readData(char *data, int64_t maxlen)
{
    Q_UNUSED(data)
    Q_UNUSED(maxlen)

    return 0;
}

int64_t QAudioTest::writeData(const void* data, int64_t len)
{
    this->notify->notify_cb(data, len, 0);
    return 0;
}


int64_t QAudioTest::bytesAvailable() const
{
    return -1;
}

void QAudioTest::refreshBuffer()
{

}

void  QAudioTest::initializeAudio()
{

}

void QAudioTest::createAudio()
{

}

void QAudioTest::deviceChanged(int index)
{
    Q_UNUSED(index)
    initializeAudio();
}

void QAudioTest::volumeChanged(int value)
{
    Q_UNUSED(value)
}

int QAudioTest::registerCallback(ACallbacks* callbacks)
{
    Q_UNUSED(callbacks)
    /* Decorate with locks */
    this->notify = callbacks;
    return 0;
}