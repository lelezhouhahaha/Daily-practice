#include <cutils/properties.h>
#include <sys/resource.h>
#include <utils/Log.h>
#include <utils/threads.h>
#include <cutils/properties.h>
#include <sys/resource.h>
#include <utils/Log.h>
#include <utils/threads.h>
#include <cutils/properties.h>
#include <utils/Log.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <pthread.h>
#include <string.h>
#include <errno.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <time.h>
#include <poll.h>
#include <dlfcn.h>
#include <math.h>
#include <sys/types.h> 
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <linux/videodev2.h>
#define POLL_TIMEOUT 1000

#define SOF_DEV_NAME "dev/video2"
#define V4L_EVENT_CAM_REQ_MGR_EVENT       (0x08000000 + 0)
#define V4L_EVENT_CAM_REQ_MGR_SOF            0

struct cam_req_mgr_error_msg {
  	uint32_t error_type;
  	uint32_t request_id;
  	int32_t device_hdl;
 	int32_t link_hdl;
  	uint64_t resource_size;	
};

struct cam_req_mgr_frame_msg {
 	uint64_t request_id;
  	uint64_t frame_id;
  	uint64_t timestamp;
  	int32_t  link_hdl;
  	uint32_t sof_status;
  	uint32_t frame_id_meta;
  	uint32_t reserved;
};

struct cam_req_mgr_custom_msg {
	uint32_t custom_type;
	uint64_t request_id;
	uint64_t frame_id;
	uint64_t timestamp;
	int32_t  link_hdl;
	uint64_t custom_data;
};

struct cam_req_mgr_message {
 	int32_t session_hdl;
  	int32_t reserved;
 	union {
  		struct cam_req_mgr_error_msg err_msg;
  		struct cam_req_mgr_frame_msg frame_msg;
  		struct cam_req_mgr_custom_msg custom_msg;
  	} u;
};

struct camera_sof_info_t{
	int fd;
	pthread_t thread_id;
	int thread_exit;
	pthread_mutex_t m_mutex;
    pthread_cond_t  m_cond;
};

static struct camera_sof_info_t CameraSofInfo;

static void* camera_sof_listen_thread(void* pContext) {
	do {
		int rc;
		struct pollfd pfds[2];
		if (CameraSofInfo.thread_exit) {
			break;
		}
		pfds[0].events = POLLIN | POLLRDNORM | POLLPRI;
		pfds[0].fd = CameraSofInfo.fd;
		rc = poll(pfds, 1, POLL_TIMEOUT);
		if (CameraSofInfo.thread_exit) {
			break;
		}
		if (rc > 0) {
			if ((pfds[0].revents & POLLPRI) || (pfds[0].revents & POLLRDNORM) || (pfds[0].revents & POLLIN)) {
				struct v4l2_event event;
				ioctl(CameraSofInfo.fd, VIDIOC_DQEVENT, &event);
				if (V4L_EVENT_CAM_REQ_MGR_EVENT == event.type && event.id == V4L_EVENT_CAM_REQ_MGR_SOF) {
					struct cam_req_mgr_message* pMessage = NULL;
				    pMessage = reinterpret_cast<struct cam_req_mgr_message*>(event.u.data);
					//ALOGI("ccm get sof......");
					ALOGI("ccm get sof link_hdl 0x%x request id %lu frame count %lu......",pMessage->u.frame_msg.link_hdl,pMessage->u.frame_msg.request_id,pMessage->u.frame_msg.frame_id);
					pthread_mutex_lock(&CameraSofInfo.m_mutex);
					pthread_cond_signal(&CameraSofInfo.m_cond);
					pthread_mutex_unlock(&CameraSofInfo.m_mutex);
				}
			}
		}
	}while(!CameraSofInfo.thread_exit);
	return NULL;
}

int OpenCameraSofDevice() {
	int rc;
	struct v4l2_event_subscription sub;
	int fd = open(SOF_DEV_NAME,O_RDWR | O_NONBLOCK);
	if (fd < 0) {
		ALOGI("OpenCameraSofDevice failed");
		CameraSofInfo.fd = -1;
		return -1;
	}
	memset(&sub, 0, sizeof(sub));
	sub.id  = V4L_EVENT_CAM_REQ_MGR_SOF;
	sub.type = V4L_EVENT_CAM_REQ_MGR_EVENT;
	rc = ioctl(fd, VIDIOC_SUBSCRIBE_EVENT, &sub);
	//ALOGI("VIDIOC_SUBSCRIBE_EVENT rc=%d",rc);
	pthread_mutex_init(&CameraSofInfo.m_mutex, 0);
    pthread_cond_init(&CameraSofInfo.m_cond, NULL);
	CameraSofInfo.fd = fd;
	CameraSofInfo.thread_exit = 0;
	pthread_create(&CameraSofInfo.thread_id, NULL, camera_sof_listen_thread, NULL);
	return 0;
}

void CloseCameraSofDevice() {
	if (CameraSofInfo.fd > 0) {
		CameraSofInfo.thread_exit = 1;
		pthread_join(CameraSofInfo.thread_id, NULL);
		pthread_cond_destroy(&CameraSofInfo.m_cond);
        pthread_mutex_destroy(&CameraSofInfo.m_mutex);
		close(CameraSofInfo.fd);
		CameraSofInfo.fd = -1;
	}
}

int WaitCameraSOF() {
	pthread_mutex_lock(&CameraSofInfo.m_mutex);
	pthread_cond_wait(&CameraSofInfo.m_cond,&CameraSofInfo.m_mutex);
	pthread_mutex_unlock(&CameraSofInfo.m_mutex);
	return 0;
}