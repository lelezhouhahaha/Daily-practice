
#include <pthread.h>

#include "CameraList.h"

typedef struct {
    struct cam_list list;
    void *data;
} cam_node_t;

typedef struct {
    cam_node_t head; /* dummy head */
    uint32_t size;
    pthread_mutex_t lock;
} cam_queue_t;

static inline int32_t cam_queue_init(cam_queue_t *queue)
{
    pthread_mutex_init(&queue->lock, NULL);
    cam_list_init(&queue->head.list);
    queue->size = 0;
    return 0;
}

static inline int32_t cam_queue_enq(cam_queue_t *queue, void *data)
{
    cam_node_t *node =
        (cam_node_t *)malloc(sizeof(cam_node_t));
    if (NULL == node) {
        return -1;
    }

    memset(node, 0, sizeof(cam_node_t));
    node->data = data;

    pthread_mutex_lock(&queue->lock);
    cam_list_add_tail_node(&node->list, &queue->head.list);
    queue->size++;
    pthread_mutex_unlock(&queue->lock);

    return 0;
}

static inline void *cam_queue_deq(cam_queue_t *queue)
{
    cam_node_t *node = NULL;
    void *data = NULL;
    struct cam_list *head = NULL;
    struct cam_list *pos = NULL;

    pthread_mutex_lock(&queue->lock);
    head = &queue->head.list;
    pos = head->next;
    if (pos != head) {
        node = member_of(pos, cam_node_t, list);
        cam_list_del_node(&node->list);
        queue->size--;
    }
    pthread_mutex_unlock(&queue->lock);

    if (NULL != node) {
        data = node->data;
        free(node);
    }

    return data;
}

static inline int32_t cam_queue_flush(cam_queue_t *queue)
{
    cam_node_t *node = NULL;
    struct cam_list *head = NULL;
    struct cam_list *pos = NULL;

    pthread_mutex_lock(&queue->lock);

    if (queue->size == 0) {
        pthread_mutex_unlock(&queue->lock);
        return 0;
    }

    head = &queue->head.list;
    pos = head->next;

    while(pos != head) {
        node = member_of(pos, cam_node_t, list);
        pos = pos->next;
        cam_list_del_node(&node->list);
        queue->size--;

        /* TODO later to consider ptr inside data */
        /* for now we only assume there is no ptr inside data
         * so we free data directly */
        if (NULL != node->data) {
            free(node->data);
        }
        free(node);

    }
    queue->size = 0;
    pthread_mutex_unlock(&queue->lock);
    return 0;
}

static inline int32_t cam_queue_deinit(cam_queue_t *queue)
{
    cam_queue_flush(queue);
    pthread_mutex_destroy(&queue->lock);
    return 0;
}
