/**
 *  �̳߳�ͷ�ļ�
 *
 **/

#ifndef _CTHREADPOOL_H_
#define _CTHREADPOOL_H_

#include <pthread.h>

/*�̳߳ؿ���������߳���*/
#define DEFAULT_MAX_THREAD_NUM      100

/*�̳߳��������Ŀ����̣߳��������߳��ͷŻز���ϵͳ*/
#define DEFAULT_FREE_THREAD_NUM     10

// The AVI_HANDLETYPE structure defines the component handle.  The component
// handle is used to access all of the component's public methods and also
// contains pointers to the component's private data area.  The component
// handle is initialized by the AVI core (with help from the component)
// during the process of loading the component.  After the component is
// successfully loaded, the application can safely access any of the
// component's public functions (although some may return an error because
// the state is inappropriate for the access).
//
// @ingroup comp
//
typedef enum AVI_THREADPOOL {
  KILL_THREAD_NO,
  KILL_THREAD_SNAPSHOT,
  KILL_THREAD_PREVIEW,
  KILL_THREAD_UPLOAD,
  KILL_THREAD_VIDEO,
  KILL_THREAD_JPEG_RESTORE,
  KILL_THREAD_EXIT
} AVI_THREADPOOL;

typedef struct worker_t         worker_t;
typedef struct CThread_pool_t   CThread_pool_t;

/*�̳߳�����ڵ�*/
struct worker_t {
    void * (* process)(void * arg); /*�ص�����*/
    int    paratype;                /*��������(Ԥ��)*/
    void * arg;                     /*�ص���������*/
    struct worker_t * next;         /*������һ������ڵ�*/
};

/*�߳̿�����*/
struct CThread_pool_t {
    pthread_mutex_t queue_lock;     /*������*/
    pthread_cond_t  queue_ready;    /*��������*/

    worker_t * queue_head;          /*����ڵ����� ��������Ͷ�ݵ�����*/
    int shutdown;                   /*�̳߳����ٱ�־ 1-����*/
    pthread_t * threadid;           /*�߳�ID*/

    int max_thread_num;             /*�̳߳ؿ���������߳���*/
    int current_pthread_num;        /*��ǰ�̳߳ش�ŵ��߳�*/
    int current_pthread_task_num;   /*��ǰ�Ѿ�ִ��������ѷ���������߳���Ŀ��*/
    int current_wait_queue_num;     /*��ǰ�ȴ����еĵ�������Ŀ*/
    int free_pthread_num;           /*�̳߳��������Ŀ����߳���/*/

    /**
     *  function:       ThreadPoolAddWorkUnlimit
     *  description:    ���̳߳�Ͷ������
     *  input param:    pthis   �̳߳�ָ��
     *                  process �ص�����
     *                  arg     �ص���������
     *  return Valr:    0       �ɹ�
     *                  -1      ʧ��
     */
    int (* AddWorkUnlimit)(void * pthis, void * (* process)(void * arg), void * arg);

    /**
     *  function:       ThreadPoolAddWorkLimit
     *  description:    ���̳߳�Ͷ������,�޿����߳�������
     *  input param:    pthis   �̳߳�ָ��
     *                  process �ص�����
     *                  arg     �ص���������
     *  return Val:     0       �ɹ�
     *                  -1      ʧ��
     */
    int (* AddWorkLimit)(void * pthis, void * (* process)(void * arg), void * arg);

    /**
     *  function:       ThreadPoolGetThreadMaxNum
     *  description:    ��ȡ�̳߳ؿ����ɵ�����߳���
     *  input param:    pthis   �̳߳�ָ��
     */
    int (* GetThreadMaxNum)(void * pthis);

    /**
     *  function:       ThreadPoolGetCurrentThreadNum
     *  description:    ��ȡ�̳߳ش�ŵ��߳���
     *  input param:    pthis   �̳߳�ָ��
     *  return Val:     �̳߳ش�ŵ��߳���
     */
    int (* GetCurrentThreadNum)(void * pthis);

    /**
     *  function:       ThreadPoolGetCurrentTaskThreadNum
     *  description:    ��ȡ��ǰ����ִ��������Ѿ�����������߳���Ŀ��
     *  input param:    pthis   �̳߳�ָ��
     *  return Val:     ��ǰ����ִ��������Ѿ�����������߳���Ŀ��
     */
    int (* GetCurrentTaskThreadNum)(void * pthis);

    /**
     *  function:       ThreadPoolGetCurrentWaitTaskNum
     *  description:    ��ȡ�̳߳صȴ�����������
     *  input param:    pthis   �̳߳�ָ��
     *  return Val:     �ȴ�����������
     */
    int (* GetCurrentWaitTaskNum)(void * pthis);

    /**
     *  function:       ThreadPoolDestroy
     *  description:    �����̳߳�
     *  input param:    pthis   �̳߳�ָ��
     *  return Val:     0       �ɹ�
     *                  -1      ʧ��
     */
    int (* Destroy)(void * pthis);
};

/*�̳߳ؾ�̬����*/
int pKillThread = KILL_THREAD_NO;
CThread_pool_t *pThreadPool = NULL;
int mm_app_init();


/**
 *  function:       ThreadPoolConstruct
 *  description:    �����̳߳�
 *  input param:    max_num   �̳߳ؿ����ɵ�����߳���
 *                  free_num  �̳߳�������ڵ��������߳�,�������߳��ͷŻز���ϵͳ
 *  return Val:     �̳߳�ָ��
 */
CThread_pool_t * ThreadPoolConstruct(int max_num, int free_num);

/**
 *  function:       ThreadPoolConstructDefault
 *  description:    �����̳߳�,��Ĭ�ϵķ�ʽ��ʼ��,δ�����߳�
 *
 *  return Val:     �̳߳�ָ��
 */
CThread_pool_t * ThreadPoolConstructDefault(void);

#endif  // _CTHREADPOOL_H_
