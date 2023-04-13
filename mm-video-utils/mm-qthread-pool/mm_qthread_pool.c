/**
 *  �̳߳�ʵ��
 *
 **/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <pthread.h>
#include <assert.h>
#include "mm_qthread_pool.h"

#include <utils/Log.h>



void * ThreadPoolRoutine(void * arg);

/**
 *  function:       ThreadPoolAddWorkLimit
 *  description:    ���̳߳�Ͷ������,�޿����߳�������
 *  input param:    pthis   �̳߳�ָ��
 *                  process �ص�����
 *                  arg     �ص���������
 *  return Val:     0       �ɹ�
 *                  -1      ʧ��
 */
int
ThreadPoolAddWorkLimit(void * pthis, void * (* process)(void * arg), void * arg)
{
    // int FreeThreadNum = 0;
    // int CurrentPthreadNum = 0;

    CThread_pool_t * pool = (CThread_pool_t *)pthis;

    /*Ϊ��ӵ�������нڵ�����ڴ�*/
    worker_t * newworker  = (worker_t *)malloc(sizeof(worker_t));
    if(NULL == newworker)
        return -1;

    newworker->process  = process;  // �ص�����,���߳�ThreadPoolRoutine()��ִ��
    newworker->arg      = arg;      // �ص���������
    newworker->next     = NULL;

    pthread_mutex_lock(&(pool->queue_lock));

    /*������������нڵ�*/
    worker_t * member = pool->queue_head;   // ָ�����������������
    if(member != NULL) {
        while(member->next != NULL) // �������нڵ�
            member = member->next;  // memberָ�������ƶ�

        member->next = newworker;   // ���뵽��������β��
    } else
        pool->queue_head = newworker; // ���뵽��������ͷ

    assert(pool->queue_head != NULL);
    pool->current_wait_queue_num++; // �ȴ����м�1

    /*���е��߳�= ��ǰ�̳߳ش�ŵ��߳� - ��ǰ�Ѿ�ִ��������ѷ���������߳���Ŀ��*/
    int FreeThreadNum = pool->current_pthread_num - pool->current_pthread_task_num;
    ALOGE("%s:%d FreeThreadNum %d",__func__,__LINE__,FreeThreadNum);
    /*���û�п����߳��ҳ��е�ǰ�߳�������������������߳�*/
    if((0 == FreeThreadNum) && (pool->current_pthread_num < pool->max_thread_num)) {
        ALOGE("%s:%d current_pthread_num %d max_thread_num %d",__func__,__LINE__,FreeThreadNum,pool->current_pthread_num,pool->max_thread_num);
        int CurrentPthreadNum = pool->current_pthread_num;

        /*�����߳�*/
        pool->threadid = (pthread_t *)realloc(pool->threadid,
                                        (CurrentPthreadNum+1) * sizeof(pthread_t));

        pthread_create(&(pool->threadid[CurrentPthreadNum]),
                                              NULL, ThreadPoolRoutine, (void *)pool);
        /*��ǰ�̳߳����߳�������1*/
        pool->current_pthread_num++;

        /*���������߳�����1*/
        pool->current_pthread_task_num++;
        pthread_mutex_unlock(&(pool->queue_lock));

        /*�����źŸ�һ���������������ȴ�״̬���߳�*/
        pthread_cond_signal(&(pool->queue_ready));
        return 0;
    }

    pool->current_pthread_task_num++;
    pthread_mutex_unlock(&(pool->queue_lock));

    /*�����źŸ�һ���������������ȴ�״̬���߳�*/
    pthread_cond_signal(&(pool->queue_ready));
//  usleep(10);  //�����
    return 0;
}

/**
 *  function:       ThreadPoolAddWorkUnlimit
 *  description:    ���̳߳�Ͷ������
 *  input param:    pthis   �̳߳�ָ��
 *                  process �ص�����
 *                  arg     �ص���������
 *  return Valr:    0       �ɹ�
 *                  -1      ʧ��
 */
int
ThreadPoolAddWorkUnlimit(void * pthis, void * (* process)(void * arg), void * arg)
{
    // int FreeThreadNum = 0;
    // int CurrentPthreadNum = 0;

    CThread_pool_t * pool = (CThread_pool_t *)pthis;

    /*����������нڵ�����ڴ�*/
    worker_t * newworker = (worker_t *)malloc(sizeof(worker_t));
    if(NULL == newworker)
        return -1;

    newworker->process  = process;  // �ص�����
    newworker->arg      = arg;      // �ص���������
    newworker->next     = NULL;

    pthread_mutex_lock(&(pool->queue_lock));

    /*�½ڵ������������������*/
    worker_t * member = pool->queue_head;
    if(member != NULL) {
        while(member->next != NULL)
            member = member->next;

        member->next = newworker;       // �����������β��
    } else
        pool->queue_head = newworker;   // ���뵽ͷ(Ҳ���ǵ�һ���ڵ�,֮ǰ����û�нڵ�)

    assert(pool->queue_head != NULL);
    pool->current_wait_queue_num++;     // ��ǰ�ȴ����еĵ�������Ŀ+1

    int FreeThreadNum = pool->current_pthread_num - pool->current_pthread_task_num;
    /*ֻ�ж��Ƿ�û�п����߳�*/
    if(0 == FreeThreadNum) {
        int CurrentPthreadNum = pool->current_pthread_num;
        pool->threadid = (pthread_t *)realloc(pool->threadid,
                                           (CurrentPthreadNum+1)*sizeof(pthread_t));
        pthread_create(&(pool->threadid[CurrentPthreadNum]),NULL,
                                        ThreadPoolRoutine, (void *)pool);
        pool->current_pthread_num++;
        if(pool->current_pthread_num > pool->max_thread_num)
            pool->max_thread_num = pool->current_pthread_num;

        pool->current_pthread_task_num++;
        pthread_mutex_unlock(&(pool->queue_lock));
        pthread_cond_signal(&(pool->queue_ready));
        return 0;
    }

    pool->current_pthread_task_num++;
    pthread_mutex_unlock(&(pool->queue_lock));
    pthread_cond_signal(&(pool->queue_ready));
//  usleep(10);
    return 0;
}

/**
 *  function:       ThreadPoolGetThreadMaxNum
 *  description:    ��ȡ�̳߳ؿ����ɵ�����߳���
 *  input param:    pthis   �̳߳�ָ��
 *  return val:     �̳߳ؿ����ɵ�����߳���
 */
int
ThreadPoolGetThreadMaxNum(void * pthis)
{
    int num = 0;
    CThread_pool_t * pool = (CThread_pool_t *)pthis;

    pthread_mutex_lock(&(pool->queue_lock));
    num = pool->max_thread_num;
    pthread_mutex_unlock(&(pool->queue_lock));

    return num;
}

/**
 *  function:       ThreadPoolGetCurrentThreadNum
 *  description:    ��ȡ�̳߳ش�ŵ��߳���
 *  input param:    pthis   �̳߳�ָ��
 *  return Val:     �̳߳ش�ŵ��߳���
 */
int
ThreadPoolGetCurrentThreadNum(void * pthis)
{
    int num = 0;
    CThread_pool_t * pool = (CThread_pool_t *)pthis;

    pthread_mutex_lock(&(pool->queue_lock));
    num = pool->current_pthread_num;
    pthread_mutex_unlock(&(pool->queue_lock));

    return num;
}

/**
 *  function:       ThreadPoolGetCurrentTaskThreadNum
 *  description:    ��ȡ��ǰ����ִ��������Ѿ�����������߳���Ŀ��
 *  input param:    pthis   �̳߳�ָ��
 *  return Val:     ��ǰ����ִ��������Ѿ�����������߳���Ŀ��
 */
int
ThreadPoolGetCurrentTaskThreadNum(void * pthis)
{
    int num = 0;
    CThread_pool_t * pool = (CThread_pool_t *)pthis;

    pthread_mutex_lock(&(pool->queue_lock));
    num = pool->current_pthread_task_num;
    pthread_mutex_unlock(&(pool->queue_lock));

    return num;
}

/**
 *  function:       ThreadPoolGetCurrentWaitTaskNum
 *  description:    ��ȡ�̳߳صȴ�����������
 *  input param:    pthis   �̳߳�ָ��
 *  return Val:     �ȴ�����������
 */
int
ThreadPoolGetCurrentWaitTaskNum(void * pthis)
{
    int num = 0;
    CThread_pool_t * pool = (CThread_pool_t *)pthis;

    pthread_mutex_lock(&(pool->queue_lock));
    num = pool->current_wait_queue_num;
    pthread_mutex_unlock(&(pool->queue_lock));

    return num;
}

/**
 *  function:       ThreadPoolDestroy
 *  description:    �����̳߳�
 *  input param:    pthis   �̳߳�ָ��
 *  return Val:     0       �ɹ�
 *                  -1      ʧ��
 */
int
ThreadPoolDestroy(void * pthis)
{
    int i;
    CThread_pool_t * pool = (CThread_pool_t *)pthis;

    if(pool->shutdown)      // ������
        return -1;

    pool->shutdown = 1;     // ���ٱ�־��λ

    /*��������pthread_cond_wait()�ȴ��߳�*/
    pthread_cond_broadcast(&(pool->queue_ready));
    for(i=0; i<pool->current_pthread_num; i++)
        pthread_join(pool->threadid[i], NULL);  // �ȴ������߳�ִ�н���

    free(pool->threadid);   // �ͷ�

    /*���������������*/
    worker_t * head = NULL;
    while(pool->queue_head != NULL) {
        head = pool->queue_head;
        pool->queue_head = pool->queue_head->next;
        free(head);
    }

    /*������*/
    pthread_mutex_destroy(&(pool->queue_lock));
    pthread_cond_destroy(&(pool->queue_ready));

    free(pool);
    pool = NULL;

    return 0;
}

/**
 *  function:       ThreadPoolRoutine
 *  description:    �̳߳������е��߳�
 *  input param:    arg  �̳߳�ָ��
 */
void *
ThreadPoolRoutine(void * arg)
{
    CThread_pool_t * pool = (CThread_pool_t *)arg;

    while(1) {
        /*����,pthread_cond_wait()���û����*/
        pthread_mutex_lock(&(pool->queue_lock));

        /*����û�еȴ�����*/
        while((pool->current_wait_queue_num == 0) && (!pool->shutdown)) {
            /*�����������ȴ������ź�*/
            pthread_cond_wait(&(pool->queue_ready), &(pool->queue_lock));
        }

        if(pool->shutdown) {
            pthread_mutex_unlock(&(pool->queue_lock));
            pthread_exit(NULL);         // �ͷ��߳�
        }

        assert(pool->current_wait_queue_num != 0);
        assert(pool->queue_head != NULL);

        pool->current_wait_queue_num--; // �ȴ������1,׼��ִ������
        worker_t * worker = pool->queue_head;   // ȥ�ȴ���������ڵ�ͷ
        pool->queue_head = worker->next;        // �������
        pthread_mutex_unlock(&(pool->queue_lock));

        (* (worker->process))(worker->arg);      // ִ�лص�����

        pthread_mutex_lock(&(pool->queue_lock));
        pool->current_pthread_task_num--;       // ����ִ�н���
        free(worker);   // �ͷ�������
        worker = NULL;

        if((pool->current_pthread_num - pool->current_pthread_task_num) > pool->free_pthread_num) {
            pthread_mutex_unlock(&(pool->queue_lock));
            break;  // ���̳߳��п����̳߳��� free_pthread_num ���߳��ͷŻز���ϵͳ
        }
        pthread_mutex_unlock(&(pool->queue_lock));
    }

    pool->current_pthread_num--;    // ��ǰ�߳�����1
    pthread_exit(NULL);             // �ͷ��߳�

    return (void *)NULL;
}

/**
 *  function:       ThreadPoolConstruct
 *  description:    �����̳߳�
 *  input param:    max_num   �̳߳ؿ����ɵ�����߳���
 *                  free_num  �̳߳�������ڵ��������߳�,�������߳��ͷŻز���ϵͳ
 *  return Val:     �̳߳�ָ��
 */
CThread_pool_t *
ThreadPoolConstruct(int max_num, int free_num)
{
    int i = 0;

    CThread_pool_t * pool = (CThread_pool_t *)malloc(sizeof(CThread_pool_t));
    if(NULL == pool)
        return NULL;

    memset(pool, 0, sizeof(CThread_pool_t));

    /*��ʼ��������*/
    pthread_mutex_init(&(pool->queue_lock), NULL);
    /*��ʼ����������*/
    pthread_cond_init(&(pool->queue_ready), NULL);

    pool->queue_head                = NULL;
    pool->max_thread_num            = max_num; // �̳߳ؿ����ɵ�����߳���
    pool->current_wait_queue_num    = 0;
    pool->current_pthread_task_num  = 0;
    pool->shutdown                  = 0;
    pool->current_pthread_num       = 0;
    pool->free_pthread_num          = free_num; // �̳߳���������������߳�
    pool->threadid                  = NULL;
    pool->threadid                  = (pthread_t *)malloc(max_num*sizeof(pthread_t));
    /*�ú���ָ�븳ֵ*/
    pool->AddWorkUnlimit            = ThreadPoolAddWorkUnlimit;
    pool->AddWorkLimit              = ThreadPoolAddWorkLimit;
    pool->Destroy                   = ThreadPoolDestroy;
    pool->GetThreadMaxNum           = ThreadPoolGetThreadMaxNum;
    pool->GetCurrentThreadNum       = ThreadPoolGetCurrentThreadNum;
    pool->GetCurrentTaskThreadNum   = ThreadPoolGetCurrentTaskThreadNum;
    pool->GetCurrentWaitTaskNum     = ThreadPoolGetCurrentWaitTaskNum;

    for(i=0; i<max_num; i++) {
        pool->current_pthread_num++;    // ��ǰ���е��߳���
        /*�����߳�*/
        pthread_create(&(pool->threadid[i]), NULL, ThreadPoolRoutine, (void *)pool);
        usleep(1000);
    }

    return pool;
}

/**
 *  function:       ThreadPoolConstructDefault
 *  description:    �����̳߳�,��Ĭ�ϵķ�ʽ��ʼ��,δ�����߳�
 *
 *  return Val:     �̳߳�ָ��
 */
CThread_pool_t *
ThreadPoolConstructDefault(void)
{
    CThread_pool_t * pool = (CThread_pool_t *)malloc(sizeof(CThread_pool_t));
    if(NULL == pool)
        return NULL;

    memset(pool, 0, sizeof(CThread_pool_t));

    pthread_mutex_init(&(pool->queue_lock), NULL);
    pthread_cond_init(&(pool->queue_ready), NULL);

    pool->queue_head                = NULL;
    pool->max_thread_num            = DEFAULT_MAX_THREAD_NUM; // Ĭ��ֵ
    pool->current_wait_queue_num    = 0;
    pool->current_pthread_task_num  = 0;
    pool->shutdown                  = 0;
    pool->current_pthread_num       = 0;
    pool->free_pthread_num          = DEFAULT_FREE_THREAD_NUM; // Ĭ��ֵ
    pool->threadid                  = NULL;
    /*�ú���ָ�븳ֵ*/
    pool->AddWorkUnlimit            = ThreadPoolAddWorkUnlimit;
    pool->AddWorkLimit              = ThreadPoolAddWorkLimit;
    pool->Destroy                   = ThreadPoolDestroy;
    pool->GetThreadMaxNum           = ThreadPoolGetThreadMaxNum;
    pool->GetCurrentThreadNum       = ThreadPoolGetCurrentThreadNum;
    pool->GetCurrentTaskThreadNum   = ThreadPoolGetCurrentTaskThreadNum;
    pool->GetCurrentWaitTaskNum     = ThreadPoolGetCurrentWaitTaskNum;

    return pool;
}

void *mm_app_thread(void * arg)
{
    ALOGE("Stream back thread[%p] is Run!\n",arg);
    while(pKillThread != KILL_THREAD_EXIT)
        usleep(10000000);
    ALOGE("Stream back thread is Exit!\n");
    pThreadPool->Destroy((void*)pThreadPool);
    return NULL;
}

int mm_app_init()
{

    if(pThreadPool == NULL)
    {
      ALOGI("Thread Pool Creat\n");
      pThreadPool = ThreadPoolConstruct(30, 12);
    }
    /**����AddWorkLimit()�滻��ִ�е�Ч��*/
    /*
     * û���ӳٷ�������Ͷ������ʱpthread_cond_wait()���ղ����ź�pthread_cond_signal() !!
     * ��ΪAddWorkUnlimit()��ȥ�����pthread_mutex_lock()�ѻ���������,����pthread_cond_wait()
     * �ղ����ź�!!Ҳ����AddWorkUnlimit()����Ӹ��ӳ�,һ���������Ҳ�������������
     */
    pThreadPool->AddWorkLimit((void *)pThreadPool, (void *)mm_app_thread, (void *)NULL);

    return 0;
}

