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

#include "avi_queuec.h"
//Linux...
#ifdef __cplusplus
extern "C"
{
#endif
#include <libavutil/common.h>
#ifdef __cplusplus
};
#endif
#include "avi_types.h"

/** Queue: A linear table that allows only inserts at one end of the table (rear) and deletes at the other end (front of the team)
  * Insert operation is referred to as enqueue. Deletion operation is referred to as dequeue. Queue has first-in-first-out characteristics.
  */

/*=====Scheduling and dequeue of the queue========
 *
 *  Departure ----------------- Entering the team
 *           <--- a1,a2,a3,...,an <---
 *          -----------------
 *
 *================================================*/

//Create queue queueCapacity - queue capacity
status initQueue(queue *PQueue,int queueCapacity)
{
    //Allocate memory to array pointers
    PQueue->pBase = (QElemType *)malloc(sizeof(QElemType)*queueCapacity);
    if(!PQueue->pBase)
    {
        av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d Failed to allocate memory to array pointer.",__func__,__LINE__);
        return Q_ERROR;
    }

    //When starting the creation, the head index is 0.
    PQueue->front = 0;
    //When starting the creation, the tail index is 0.
    PQueue->rear = 0;
    PQueue->maxSize = queueCapacity;

    return Q_OK;
}

//Destroy queue
void destroyQueue(queue *PQueue)
{
    //Release the memory pointed to by the queue array pointer
    free(PQueue);
    //The queue array pointer is redirected to NULL, avoiding becoming a wild pointer
    PQueue = NULL;
}

//Empty queue
void clearQueue(queue *PQueue)
{
    //Team head index cleared
    PQueue->front = 0;
    //End of the team index is cleared
    PQueue->rear = 0;
}

//Determine if the queue is empty
status isEmpityQueue(queue *PQueue)
{
    //Team head == team tail, the description is empty
    if( PQueue->front == PQueue->rear )
        return Q_TRUE;

    return Q_FALSE;
}

/*
  * In the circular queue, the conditions of "team full" and "team empty" may be the same, both front==rear,
  * In this case, it is impossible to distinguish whether it is "team full" or "team empty".
  * There are 3 possible ways to deal with this problem:
  * (1) There is another flag to distinguish whether it is "team full" or "team empty". (ie check whether the team is full / "team empty" before enrolling/departing)
  * (2) Set a counter, even a pointer can be omitted.
  * (3) use one element space less, that is, the head of the team head is used as the "team full" mark when the next position of the team tail pointer.
  * The "team full" condition is: (PQueue->rear+1)%MAX_SIZE == PQueue->front.
  * [The third treatment method is used here]
  */
//Determine if the queue is full
status isFullQueue(queue *PQueue)
{
    //Queue full
    if( (PQueue->rear+1)%PQueue->maxSize == PQueue->front )
        return Q_TRUE;

    return Q_FALSE;
}

//Get queue length
int getQueueLen(queue *PQueue)
{
    //Under normal circumstances,
    //the queue length is the difference between the head and the head of the team.
    //but if the first and last pointers cross the maximum capacity, %.
    return (PQueue->rear - PQueue->front + PQueue->maxSize)%PQueue->maxSize;
}

//New element enrollment [first in, first out principle: insert at the end of the team] element - to insert elements
status enQueue(queue *PQueue,QElemType element)
{
    if(isFullQueue(PQueue)==Q_TRUE)
    {
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d The queue is full and no more elements can be inserted!",__func__,__LINE__);
        return Q_FALSE;
    }

    //Add new elements to the queue
    PQueue->pBase[PQueue->rear] = element;
    //Give rear a new suitable value
    PQueue->rear = (PQueue->rear+1) % PQueue->maxSize;

    return Q_TRUE;
}

//New elements are dequeued while saving the elements of the team [first in, first out principle: deleted at the head of the team]
status deQueue(queue *PQueue,QElemType *pElement)
{
    //Return false if the queue is empty
    if(isEmpityQueue(PQueue)==Q_TRUE)
    {
        av_log(NULL, AV_LOG_DEBUG,
            "%s:%d The queue is empty, the team failed!",__func__,__LINE__);
        return Q_FALSE;
    }

    //First in, first out
    *pElement = PQueue->pBase[PQueue->front];
    //Move to the next position
    PQueue->front = (PQueue->front+1) % PQueue->maxSize;

    return Q_TRUE;
}

//Traversing the queue
void queueTraverse(queue *PQueue)
{
    //Traversing from scratch
    int i = PQueue->front;
    av_log(NULL, AV_LOG_VERBOSE,
        "%s:%d PQueue pBase address <%p> ",__func__,__LINE__,PQueue->pBase);

    av_log(NULL, AV_LOG_VERBOSE,
        "%s:%d Traversing the queue:<%d> ",__func__,__LINE__,i);
    //If you don't reach the rear position, loop
    while(i != PQueue->rear)
    {
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d pkt_number--->%d \n",
                __func__,__LINE__, PQueue->pBase[i].pkt_number);
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d pkt_size--->%lld \n",
                __func__,__LINE__, PQueue->pBase[i].pkt_size);
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d pkt_buf--->0x%2x \n",
                __func__,__LINE__, PQueue->pBase[i].pkt_buf[0]);
        av_log(NULL, AV_LOG_VERBOSE,"%s:%d --->%lld seconds.\n",
                __func__,__LINE__, PQueue->pBase[i].pts);
        //Move to the next position
        i = (i+1) % PQueue->maxSize;
    }
    av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d X.\n",__func__,__LINE__);
}

//Save 30 buffers of audio.
status queuePingPong(queue *PQueue, char *fileName)
{
    FILE *pcmFile;
    char pcm_filename[MAX_STRING_LEN]="";
    //Traversing from scratch
    int i = PQueue->front;

    sprintf(pcm_filename, "%s%s%s", AVI_DEMUXER_LOCATION, fileName, ".pcm");
    pcmFile = fopen(pcm_filename,"wb");
    if (!pcmFile) {
        pcmFile = NULL;
        av_log(NULL, AV_LOG_ERROR,
            "%s:%d Could not open %s\n", __func__,__LINE__, pcm_filename);
        return Q_FALSE;
    }
    av_log(NULL, AV_LOG_VERBOSE,
        "%s:%d Traversing the queue:<%d> ",__func__,__LINE__,i);
    //If you don't reach the rear position, loop
    while(i != PQueue->rear)
    {
        fwrite(PQueue->pBase[i].pkt_buf, 1, PQueue->pBase[i].pkt_size, pcmFile);
        av_log(NULL, AV_LOG_DEBUG,"%s:%d Traversing the queue:<%d> pkt_size--->%lld \n",
                __func__,__LINE__, i, PQueue->pBase[i].pkt_size);
        //Move to the next position
        i = (i+1) % PQueue->maxSize;
    }
    av_log(NULL, AV_LOG_VERBOSE,
            "%s:%d X.\n",__func__,__LINE__);
    clearQueue(PQueue);
    fclose (pcmFile);
    return Q_TRUE;
}
