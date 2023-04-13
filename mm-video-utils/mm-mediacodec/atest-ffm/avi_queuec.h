/*
 * Copyright (c) 2019 The Meig Group Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject
 * to the following conditions:
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#ifndef MYQUEUEC_H
#define MYQUEUEC_H

#include <stdio.h>
#include <malloc.h>


/*****************************************************************************
 *  Queue:
 *  Only the insertion operation is allowed at one end of the table (rear),
 *  and the linear table insertion operation at the other end
 *  (the front of the team) is referred to as the enqueue deletion operation.
 *  The dequeue queue is referred to as the first-in-first-out queue.
 *  Characteristics
 *****************************************************************************/
// buffer_split_length (unit: second)
#define BUFFER_SPLIT_LENGTH     30
#define AUDIO_NUM_DATA_POINTERS 1024
#define MAX_SIZE                2048

/*=====Scheduling and dequeue of the queue========
 *
 *  Departure ----------------- Entering the team
 *  <--------- a1,a2,a3,...,an <----------
 *  ---------------------------------------------
 *
 *================================================*/

typedef enum
{
    Q_OK=0,   //Correct
    Q_ERROR=1,//Error
    Q_TRUE=2, //True
    Q_FALSE=3 //False
}status;

/** @AVI callback structure. */
typedef struct {

    /**
     * Audio buffer number
     */
    int pkt_number;

    /**
     * size of the corresponding packet containing the compressed
     * frame.
     * It is set to a negative value if unknown.
     * - encoding: unused
     * - decoding: set by libavcodec, read by user.
     */
    int64_t pkt_size;
    /**
     * AVBuffer references backing the data for this frame. If all elements of
     * this array are NULL, then this frame is not reference counted. This array
     * must be filled contiguously -- if buf[i] is non-NULL then buf[j] must
     * also be non-NULL for all j < i.
     *
     * There may be at most one AVBuffer per data plane, so for video this array
     * always contains all the references. For planar audio with more than
     * AUDIO_NUM_DATA_POINTERS channels, there may be more buffers than can fit in
     * this array. Then the extra AVBufferRef pointers are stored in the
     * extended_buf array.
     */
    char pkt_buf[AUDIO_NUM_DATA_POINTERS];
    /**
     * Sample rate of the audio data.
     */
    int sample_rate;
    /**
     * number of audio channels, only used for audio.
     * - encoding: unused
     * - decoding: Read by user.
     */
    int channels;
    /**
     * format of the frame, -1 if unknown or unset
     * Values correspond to enum AVPixelFormat for video frames,
     * enum AVSampleFormat for audio)
     */
    int format;
    /**
     * Presentation timestamp in time_base units (time when frame should be shown to user).
     */
    int64_t pts;
} QElemType;     //Macro defines the data type of the queue


/*===============================================
 *
 * First, the use of array storage queues is
 * called static sequential queues.
 * Second, the use of dynamically allocated
 * pointers called dynamic sequential queues
 * [here is the dynamic sequence queue]
 *
 *===============================================*/
typedef struct
{
    QElemType *pBase; //Array points to dynamically allocated memory
    int front;       //Head index
    int rear;        //Team tail index
    int maxSize;     //Current allocated maximum capacity
}queue;

//Create an empty queue queueCapacity - queue capacity
status initQueue(queue *PQueue,int queueCapacity);
//Destroy queue
void destroyQueue(queue *PQueue);
//Empty queue
void clearQueue(queue *PQueue);
//Determine if the queue is empty
status isEmpityQueue(queue *PQueue);
//Determine if the queue is full
status isFullQueue(queue *PQueue);
//Get queue length
int getQueueLen(queue *PQueue);
//New element enrollment
//[first in, first out principle: insert at the end of the team]
//element - to insert elements
status enQueue(queue *PQueue,QElemType element);
//New elements are dequeued while saving the elements of the team
//[first in, first out principle: deleted at the head of the team]
status deQueue(queue *PQueue,QElemType *pElement);
//Traversing the queue
void queueTraverse(queue *PQueue);
//Save audio data
status queuePingPong(queue *PQueue, char *fileName);

#endif // MYQUEUEC_H
