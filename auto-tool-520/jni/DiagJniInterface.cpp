
#define TAG "mmiDiagJNIInterface"
#include "msg.h"
#include "diag_lsm.h"
#include "diagpkt.h"
#include "diagcmd.h"

#include <string.h>
#include <jni.h>
#include <dlfcn.h>
#include <dirent.h>
#include <errno.h>
#include <sys/stat.h>
#include <android/log.h>
#include <sys/types.h>   
#include <stdlib.h>
#include <semaphore.h>
 

#include "com_meigsmart_meigrs32_util_DiagJniInterface.h" 
#define LOGI_LOG(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)  
#define LOGD_LOG(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)  
#define LOGE_LOG(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

const char *classPath = "com/meigsmart/meigrs32/util/DiagJniInterface";


static JavaVM *jvm;
static jobject gDiagJNIInterfaceObject;
static sem_t g_sem_diag_cmmd;
int mDiagCmmdId = 0;
int mDiagCmmdResult = 0;
char *data = NULL;
const int FTM_AP_C = 52;
const int FAIL = 1;
const int SUCCESS = 2;
const int NOTEST = 0;

#ifdef __cplusplus
  extern "C" {
#endif

PACKED void *CreatResponsePacket(int diagCmmdId, int diagCmmdResult, const char *rsp_data){
	PACKED void *rsp_pkt = NULL;
 
    /* Allocate the same length as the request. */
    rsp_pkt = (ftm_cmd_response *) diagpkt_subsys_alloc(DIAG_SUBSYS_FTM, FTM_AP_C, sizeof(ftm_cmd_response));
 
    if(rsp_pkt != NULL) {
        LOGD_LOG("%s: diagpkt_subsys_alloc succeeded", __FUNCTION__);
    } else {
        LOGD_LOG("%s: diagpkt_subsys_alloc failed", __FUNCTION__);
        return NULL;
    }
 
	((ftm_cmd_response *)rsp_pkt)->sftm_header.cmd_code = (uint8_t)75;
	((ftm_cmd_response *)rsp_pkt)->sftm_header.sub_sys_id = (uint8_t)DIAG_SUBSYS_FTM;
	((ftm_cmd_response *)rsp_pkt)->sftm_header.sub_sys_cmd_code = (uint16_t)FTM_AP_C;
	((ftm_cmd_response *)rsp_pkt)->sftm_header.ftm_cmd_id = (uint16_t)diagCmmdId;
	((ftm_cmd_response *)rsp_pkt)->sftm_header.ftm_data_len = (uint16_t)0;
	((ftm_cmd_response *)rsp_pkt)->sftm_header.ftm_rsp_pkt_size = (uint16_t)0;
	((ftm_cmd_response *)rsp_pkt)->ftm_cmd_resp_result = (uint8_t)diagCmmdResult;
	
	if( ( diagCmmdResult != FAIL ) || ( rsp_data == NULL ) ){
		((ftm_cmd_response *)rsp_pkt)->size = 0;
		memset(((ftm_cmd_response *)rsp_pkt)->Data, 0, PACK_SIZE+4);
		LOGD_LOG("%s: 1 diagCmmdResult:[%d] or rsp_data == NULL", __FUNCTION__, diagCmmdResult);
		return (PACKED void *)rsp_pkt;
	}
	
	if(rsp_data != NULL){
		uint16_t mSize = strlen(rsp_data)+1;
		LOGD_LOG("ftm_read_file: mSize=[%d]", mSize);
		LOGD_LOG("ftm_read_file: rsp_data=[%s]", rsp_data);
		((ftm_cmd_response *)rsp_pkt)->size = mSize > PACK_SIZE? PACK_SIZE:mSize;
		LOGD_LOG("ftm_read_file: iSize=%d", ((ftm_cmd_response *)rsp_pkt)->size);
		
		memset(((ftm_cmd_response *)rsp_pkt)->Data, 0, PACK_SIZE+4);
		memcpy(((ftm_cmd_response *)rsp_pkt)->Data, rsp_data, ((ftm_cmd_response *)rsp_pkt)->size);
	}
	return (PACKED void *)rsp_pkt;
}

PACKED void *cmmdHandlerAutoJudged(int mDiagCmmdId){
	JNIEnv *env;
	bool threadAttached = false;
	if(jvm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK){
		if(jvm->AttachCurrentThread(&env, NULL) != JNI_OK) {
			LOGD_LOG("cmmdHandlerAutoJudged: AttachCurrentThread error");
			return CreatResponsePacket(mDiagCmmdId, 1, "AttachCurrentThread error");
		}
		threadAttached = true;
	}
	LOGD_LOG("cmmdHandlerAutoJudged: 1");
	jclass java_class = env->GetObjectClass(gDiagJNIInterfaceObject);
	if(!java_class){
		if(threadAttached)
            jvm->DetachCurrentThread();
        LOGD_LOG("cmmdHandlerAutoJudged: GetObjectClass error");
		return CreatResponsePacket(mDiagCmmdId, 1, "GetObjectClass error");
	}
	LOGD_LOG("cmmdHandlerAutoJudged: 2");
	jmethodID method = env->GetStaticMethodID(java_class, "doNoticeApHandlerAutoJudged", "(I)V");
	if(!method) {
      if(threadAttached)
          jvm->DetachCurrentThread();
      LOGD_LOG("cmmdHandlerAutoJudged: GetStaticMethodID error");
	  return CreatResponsePacket(mDiagCmmdId, 1, "GetObjectClass error");
    }
	LOGD_LOG("cmmdHandlerAutoJudged: 3");
	/* Finally call the callback */
    jint ret = env->CallStaticIntMethod(java_class, method, mDiagCmmdId);
	LOGD_LOG("cmmdHandlerAutoJudged ret:%d", ret);
    if(threadAttached)
        jvm->DetachCurrentThread();
	LOGD_LOG("cmmdHandlerAutoJudged end");
	return NULL;
}

PACKED void *cmmdHandlerSetResult(int mCmdId, uint8_t *data){
	JNIEnv *env;
	LOGD_LOG("cmmdHandlerSetResult: mCmdId:%d", mCmdId);
	//ftm_cmd_response *cmd = (ftm_cmd_response *)req_pkt;
	bool threadAttached = false;
	if(jvm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK){
		if(jvm->AttachCurrentThread(&env, NULL) != JNI_OK) {
			LOGD_LOG("cmmdHandlerSetResult: AttachCurrentThread error");
			return CreatResponsePacket(mCmdId, 1, "AttachCurrentThread error");
		}
		threadAttached = true;
	}
	LOGD_LOG("cmmdHandlerSetResult: 1");
	jclass java_class = env->GetObjectClass(gDiagJNIInterfaceObject);
	if(!java_class){
		if(threadAttached)
            jvm->DetachCurrentThread();
        LOGD_LOG("cmmdHandlerSetResult: GetObjectClass error");
		return CreatResponsePacket(mCmdId, 1, "GetObjectClass error");
	}
	LOGD_LOG("cmmdHandlerSetResult: 2");
	jmethodID method = env->GetStaticMethodID(java_class, "doNoticeApHandlerSetResult", "(ILjava/lang/String;)V");
	if(!method) {
      if(threadAttached)
          jvm->DetachCurrentThread();
      LOGD_LOG("cmmdHandlerSetResult: GetStaticMethodID error");
	  return CreatResponsePacket(mCmdId, 1, "GetObjectClass error");
    }
	LOGD_LOG("cmmdHandlerSetResult: 3");
	jstring jdata = env->NewStringUTF((char *)data);
	/* Finally call the callback */
    jint ret = env->CallStaticIntMethod(java_class, method, mCmdId, jdata);
	LOGD_LOG("cmmdHandlerSetResult ret:%d", ret);
    if(threadAttached)
        jvm->DetachCurrentThread();
	LOGD_LOG("cmmdHandlerSetResult end");
	return NULL;
}

jboolean getToolStartStatus(){
	
	JNIEnv *env;
	jboolean mToolStartStatus = false;
	bool threadAttached = false;
	if(jvm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK){
		if(jvm->AttachCurrentThread(&env, NULL) != JNI_OK) {
			LOGD_LOG("getToolStartStatus: AttachCurrentThread error");
			return mToolStartStatus;
		}
		threadAttached = true;
	}
	LOGD_LOG("getToolStartStatus: 1");
	jclass java_class = env->GetObjectClass(gDiagJNIInterfaceObject);
	if(!java_class){
		if(threadAttached)
            jvm->DetachCurrentThread();
        LOGD_LOG("getToolStartStatus: GetObjectClass error");
		return mToolStartStatus;
	}
	LOGD_LOG("getToolStartStatus: 2");
	jmethodID method = env->GetStaticMethodID(java_class, "getToolStartStatus", "()Z");
	if(!method) {
      if(threadAttached)
          jvm->DetachCurrentThread();
      LOGD_LOG("getToolStartStatus: GetStaticMethodID error");
	  return mToolStartStatus;
    }
	LOGD_LOG("getToolStartStatus: 3");
	/* Finally call the callback */
    mToolStartStatus = env->CallStaticBooleanMethod(java_class, method);
	LOGD_LOG("getToolStartStatus mToolStartStatus:%s", mToolStartStatus?"true":"false");
    if(threadAttached)
        jvm->DetachCurrentThread();
	LOGD_LOG("getToolStartStatus end");
	return mToolStartStatus;
}

//PACKED void *ftm_ap_dispatch((void *)req_pkt, uint16_t pkt_len) {
void * ftm_ap_dispatch(void *req_pkt, uint16 pkt_len) {
    LOGD_LOG("ftm_ap_dispatch called!");
	ftm_header *pheader = (ftm_header *) req_pkt;
    uint16_t iCmd = pheader->ftm_cmd_id;
	LOGD_LOG("ftm_ap_dispatch called! iCmd:%d", iCmd);
	LOGD_LOG("ftm_ap_dispatch called! pkt_len:%d", pkt_len);
	PACKED void *ptr_ret = NULL;
	jboolean mToolStartStatus = false;
	
	mToolStartStatus = getToolStartStatus();
	LOGD_LOG("ftm_ap_dispatch mToolStartStatus:%s", mToolStartStatus?"true":"false");
	
	if((iCmd > (FTM_SUBCMD_END + FTM_SUBCMD_BASE) ) && !mToolStartStatus){
		LOGD_LOG("ftm_ap_dispatch  PCBA Auto Test tool is not start, Please start PCBA Auto Test Tool first!");
		return CreatResponsePacket(iCmd, 1, "Please start PCBA Auto Test Tool first!");
	}
	
	if( ( (iCmd >= (FTM_SUBCMD_START + FTM_SUBCMD_BASE) ) && ( iCmd < (FTM_SUBCMD_HEADSET + FTM_SUBCMD_BASE)) ) ||
	( (iCmd >= FTM_SUBCMD_QUERY_BASE ) && ( iCmd < (FTM_SUBCMD_MAX + FTM_SUBCMD_QUERY_BASE)) )){
		ptr_ret = cmmdHandlerAutoJudged(iCmd);
	}else{
		ptr_ret = (PACKED void *)cmmdHandlerSetResult(iCmd, ((ftm_cmd_response *)req_pkt)->Data);
	}
	
	if(ptr_ret != NULL){
		LOGD_LOG("ftm_ap_dispatch called fail!");
	}
	LOGD_LOG("ftm_ap_dispatch called! sem_wait start");
	
	sem_wait(&g_sem_diag_cmmd);
	LOGD_LOG("ftm_ap_dispatch called! sem_wait end iCmd:[%d], mDiagCmmdResult:[%d]", iCmd, mDiagCmmdResult);

	return CreatResponsePacket(iCmd, mDiagCmmdResult, (mDiagCmmdResult == 2)?NULL:data);
}

static const diagpkt_user_table_entry_type ftm_mmi_diag_func_table[] = {
        {FTM_AP_C, FTM_AP_C, ftm_ap_dispatch},
};


char* Jstring2CStr(JNIEnv* env, jstring jstr)
{
	 char* rtn = NULL;
	 jclass clsstring = env->FindClass("java/lang/String");
	 jstring strencode = env->NewStringUTF("GB2312");
	 jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
	 jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr,mid,strencode);
	 jsize alen = env->GetArrayLength(barr);
	 jbyte* ba = env->GetByteArrayElements(barr,JNI_FALSE);
	 if(alen > 0)
	 {
	  rtn   =   (char*)malloc(alen+1);         //new   char[alen+1];
	  memcpy(rtn,ba,alen);
	  rtn[alen]=0;
	 }
	 env->ReleaseByteArrayElements(barr,ba,0);
	 return rtn;
}
  
/* 
 * System.loadLibrary("lib")时调用 
 * 如果成功返回JNI版本, 失败返回-1 
 */  
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv *env = NULL;
	char * mTest = (char *)reserved;
	if(reserved == NULL){
		LOGD_LOG("JNI_OnLoad reserved == NULL");
	}

    jvm = vm;
	LOGD_LOG("Start to process ftm 1\n");
    if(vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
		LOGD_LOG("Start to process ftm 2\n");
        return JNI_ERR;
    }

	jclass diagJNIInterface = env->FindClass(classPath);
	jmethodID diagJNIInit = env->GetMethodID(diagJNIInterface, "<init>", "()V");
	jobject diagJNIObject = env->NewObject(diagJNIInterface, diagJNIInit);

	gDiagJNIInterfaceObject = env->NewGlobalRef(diagJNIObject);

    return JNI_VERSION_1_4;
}

/*
 * Class:     com_meigsmart_meigrs32_util_DiagJniInterface
 * Method:    Diag_Init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_meigsmart_meigrs32_util_DiagJniInterface_Diag_1Init
  (JNIEnv *jnv, jobject jobj){
	  /* Register for diag packets */
    bool bInit_Success = false;
	LOGD_LOG("Start to process ftm 3\n");
	sem_init(&g_sem_diag_cmmd, 0, 0);

    bInit_Success = Diag_LSM_Init(NULL);
	LOGD_LOG("Start to process ftm 4 %s\n", bInit_Success?"true":"false");

    if(!bInit_Success) {
		LOGD_LOG("Start to process ftm 5\n");
        return;
    }

	LOGD_LOG("Start to process ftm 6 Diag_LSM_Init call succeeded\n");
    DIAGPKT_DISPATCH_TABLE_REGISTER(DIAG_SUBSYS_FTM, ftm_mmi_diag_func_table);
	LOGD_LOG("Start to process ftm register\n");
  }
  
  /*
 * Class:     com_meigsmart_meigrs32_util_DiagJniInterface
 * Method:    Diag_Deinit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_meigsmart_meigrs32_util_DiagJniInterface_Diag_1Deinit
  (JNIEnv *jenv, jobject jobj){
	bool bDeinit_status = Diag_LSM_DeInit();
	if(!bDeinit_status){
		LOGD_LOG("Diag_LSM_DeInit fail.");
	}else LOGD_LOG("Diag_LSM_DeInit success.");
	
	sem_close(&g_sem_diag_cmmd);
	
  }
  
  
  /*
 * Class:     com_meigsmart_meigrs32_util_DiagJniInterface
 * Method:    SendDiagResult
 * Signature: (ILjava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_com_meigsmart_meigrs32_util_DiagJniInterface_SendDiagResult
  (JNIEnv *jenv, jobject jobj, jint jcmdId, jint jcmdresult, jstring jdata, jint jdatasize){
	
	ftm_cmd_response *req_pkt = NULL;
	if(jdata != NULL){
		data = Jstring2CStr(jenv, jdata);
	}else{
		data = NULL;
	}
	mDiagCmmdResult = jcmdresult;
	mDiagCmmdId = jcmdId;
	
	sem_post(&g_sem_diag_cmmd);
  }
#ifdef __cplusplus
}
#endif


