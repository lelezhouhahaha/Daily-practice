#include <stdio.h>
#include <jni.h>
#include <string.h>
#include <stdlib.h>  
#include <stdio.h>  
#include <jni.h>  
#include <assert.h>  
  
#include <termios.h>  
#include <unistd.h>  
#include <sys/types.h>  
#include <sys/stat.h>  
#include <fcntl.h>  
#include <string.h>  
#include <android/log.h>  
#include <errno.h>
 

#include "com_meigsmart_meigrs32_util_SerialPort.h"
static const char *TAG = "serial_port";  
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)  
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)  
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)  

#define PORT_LENGHT 256


/*static JNINativeMethod gMethods[] = {  

};*/
  
/* 
 * 为某一个类注册本地方法 
 */  
/*static int registerNativeMethods(JNIEnv* env, const char* className,  
        JNINativeMethod* gMethods, int numMethods) {  
    jclass clazz;  
    clazz = env->FindClass(className);  
    if (clazz == NULL) {  
        return JNI_FALSE;  
    }  
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {  
        return JNI_FALSE;  
    }  
  
    return JNI_TRUE;  
}*/
  
/* 
 * 为所有类注册本地方法 
 */  
/*static int registerNatives(JNIEnv* env) {  
    const char* kClassName = "com/example/serialutil/SerialActivity"; //指定要注册的类  
    return registerNativeMethods(env, kClassName, gMethods,  
            sizeof(gMethods) / sizeof(gMethods[0]));  
}*/ 
  
/* 
 * System.loadLibrary("lib")时调用 
 * 如果成功返回JNI版本, 失败返回-1 
 */  
/*JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {  
    JNIEnv* env = NULL;  
    jint result = -1; */ 
  
	/*
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {  
        return -1;  
    }  
    assert(env != NULL);  
  
    if (!registerNatives(env)) { //注册  
        return -1;  
    }  
	*/
    //成功  
	//result=JNI_VERSION_1_6;
    //result = JNI_VERSION_1_4;  
  
   // return result;  
//}



struct PortInfo{
    int busy;
    char name[32];
    int handle;
};
#define MAX_PORTS 4
struct PortInfo ports[MAX_PORTS];

long GetBaudRate_K(long baudRate)
{
    long BaudR;
    switch(baudRate)
    {
    case 115200:
        BaudR=B115200;
        break;
    case 57600:
        BaudR=B57600;
        break;
    case 38400:
        BaudR=B38400;
        break;
    case 9600:
        BaudR=B9600;
        break;
    default:
        BaudR=B0;
    }
    return BaudR;
}

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

int OpenComConfig(int port,
                  const char deviceName[],
                  long baudRate/*,
                  int parity,
                  int dataBits,
                  int stopBits,
                  int iqSize,
                  int oqSize*/)
{
    struct termios newtio;
    //long BaudR;

    ports[port].busy = 1;
    strcpy(ports[port].name,deviceName);
	LOGD("name '%s'\n", ports[port].name);
    if ((ports[port].handle = open(deviceName, O_RDWR)) == -1)
    {
        perror("open");
        assert(0);
    }


    newtio.c_cflag = CS8 | CLOCAL | CREAD ;
    newtio.c_iflag = IGNPAR;
    newtio.c_oflag = 0;
    newtio.c_lflag = 0;
    newtio.c_cc[VINTR]    = 0;
    newtio.c_cc[VQUIT]    = 0;
    newtio.c_cc[VERASE]   = 0;
    newtio.c_cc[VKILL]    = 0;
    newtio.c_cc[VEOF]     = 4;
    newtio.c_cc[VTIME]    = 0;
    newtio.c_cc[VMIN]     = 1;
    newtio.c_cc[VSWTC]    = 0;
    newtio.c_cc[VSTART]   = 0;
    newtio.c_cc[VSTOP]    = 0;
    newtio.c_cc[VSUSP]    = 0;
    newtio.c_cc[VEOL]     = 0;
    newtio.c_cc[VREPRINT] = 0;
    newtio.c_cc[VDISCARD] = 0;
    newtio.c_cc[VWERASE]  = 0;
    newtio.c_cc[VLNEXT]   = 0;
    newtio.c_cc[VEOL2]    = 0;
    cfsetospeed(&newtio, GetBaudRate_K(baudRate));
    cfsetispeed(&newtio, GetBaudRate_K(baudRate));
    tcsetattr(ports[port].handle, TCSANOW, &newtio);
    return 0;
}

int OpenCom(int portNo, const char deviceName[],long baudRate)
{
    return OpenComConfig(portNo, deviceName, baudRate/*, 1, 8, 1, 0, 0*/);
}

int CloseCom(int portNo)
{
    if (ports[portNo].busy)
    {
        close(ports[portNo].handle);
        ports[portNo].busy = 0;
        return 0;
    }
    else
    {
        return -1;
    }
}

int ComRd(int portNo, char buf[], int maxCnt,int Timeout)
{
    int actualRead = 0;
    fd_set rfds;
    struct timeval tv;
    int retval; 

    if (!ports[portNo].busy)
    {
        assert(0);
    }

    FD_ZERO(&rfds);
    FD_SET(ports[portNo].handle, &rfds);
	retval = FD_ISSET(ports[portNo].handle, &rfds);
    tv.tv_sec = Timeout/1000;
    tv.tv_usec = (Timeout%1000)*1000;
    retval = select(ports[portNo].handle+1, &rfds, NULL, NULL, &tv);
	LOGD(" retval %d\n", retval);
    if (retval)
    {
		if(FD_ISSET(ports[portNo].handle, &rfds)){
			actualRead = read(ports[portNo].handle, buf, maxCnt);
			LOGD(" actualRead %d buf[0]:%2x\n", actualRead, buf[0]);
		}
    }

    return actualRead;
}

int ComWrt(int portNo, const char *buf, int maxCnt)
{
    int written;

    if (!ports[portNo].busy)
    {
        assert(0);
    }
    
    written = write(ports[portNo].handle, buf, maxCnt);
    return written;
}


JNIEXPORT void JNICALL Java_com_meigsmart_meigrs32_util_SerialPort_uhf_1test
  (JNIEnv * env, jobject obj, jstring jport, jint jbaud, jstring reqArray){
    int ret, nWritten, nRead;
    char err[50];
	int portno = 1;
	char *port = NULL;
	char *req = NULL;
	char array[PORT_LENGHT];
	
	port = Jstring2CStr(env, jport);
	req = Jstring2CStr(env, reqArray);
	ret=OpenCom(portno,port,jbaud);
	LOGD("port = %s baud = %d \n",port,jbaud);
    if(ret==-1)
    {
        LOGE(err,"The %s open error.",port);
		return;
	}
	usleep(1000*20);
	nWritten=ComWrt(portno, req, strlen(req));
	LOGD("SerialPort [%s] has send %d chars req:[%s]!\n", port,nWritten, req);
	fflush(stdout);
	usleep(1000*1000);
	memset(array, 0, PORT_LENGHT);
	nRead=ComRd(portno,array,PORT_LENGHT,1000);//modify for cit uart test
	LOGD("SerialPort [%s] has read %d chars req:[%s] nRead:[%d]!\n", port,PORT_LENGHT, array, nRead);
	for(int i=0;i<PORT_LENGHT;i++){
		LOGD("mmm@@ array = %d\n",array[i]);
	}
	if(nRead>0){
		LOGD("@@@@ array = %s\n",&array[0]);
		if(strcmp(req, array) == 0 ){
		jclass java_class = env->FindClass("com/meigsmart/meigrs32/util/SerialPort");
		if(java_class == 0){
			LOGI("SerialPort class not found");
		}else{
			LOGI("SerialPort class found");
			jmethodID method = env->GetMethodID(java_class, "setStatusTwice", "(Z)V"); //modify for cit uart test
			if(method == 0){
				LOGI("setStatusTwice method not found");
			}else{
				LOGI("callback setStatusTwice method");
				env->CallVoidMethod( obj, method,true);
			}
		}
	}
	}else
		LOGD("Timeout\n");
	free(req);
	free(port);
	req = NULL;
	port = NULL;
	
	CloseCom(portno);
}

JNIEXPORT void JNICALL Java_com_meigsmart_meigrs32_util_SerialPort_pin_1test
  (JNIEnv * env, jobject obj, jstring jport, jint jbaud, jstring reqArray){
    int ret, nWritten, nRead;
    char err[50];
	int portno = 1;
	char *port = NULL;
	char *req = NULL;
	char array[PORT_LENGHT];
	
	port = Jstring2CStr(env, jport);
	req = Jstring2CStr(env, reqArray);
	ret=OpenCom(portno,port,jbaud);
	LOGD("pin test port = %s baud = %d \n",port,jbaud);
    if(ret==-1)
    {
        LOGE(err,"The %s open error.",port);
		return;
	}
	usleep(1000*20);
	nWritten=ComWrt(portno, req, strlen(req));
	LOGD("SerialPort [%s] has send %d chars req:[%s]!\n", port,nWritten, req);
	fflush(stdout);
	usleep(1000*1000);
	memset(array, 0, PORT_LENGHT);
	nRead=ComRd(portno,array,PORT_LENGHT,2000);//modify for cit uart test
	LOGD("SerialPort [%s] has read %d chars req:[%s] nRead:[%d]!\n", port,PORT_LENGHT, array, nRead);
	for(int i=0;i<PORT_LENGHT;i++){
		LOGD("mmm@@ array = %d\n",array[i]);
	}
	if(nRead>0){
		LOGD("@@@@ array = %s\n",&array[0]);
		jclass java_class = env->FindClass("com/meigsmart/meigrs32/util/SerialPort");
		if(java_class == 0){
			LOGI("SerialPort class not found");
		}else{
			LOGI("SerialPort class found");
			jmethodID method = env->GetMethodID(java_class, "setStatus", "(Z)V");
			if(method == 0){
				LOGI("setStatus method not found");
			}else{
				LOGI("callback setStatus method");
				env->CallVoidMethod( obj, method,true);
			}
		}
	}else
		LOGD("Timeout\n");
	free(req);
	free(port);
	req = NULL;
	port = NULL;
	
	CloseCom(portno);
}


/*
 * Class:     com_meigsmart_meigrs32_util_SerialPort
 * Method:    test
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_meigsmart_meigrs32_util_SerialPort_test(JNIEnv * env, jobject obj, jstring jport, jint jbaud, jintArray reqArray, jintArray respArray){
    int portno=0;
    int ret,nWritten,nRead;
    char err[50];
	char req[PORT_LENGHT];
	char resp[PORT_LENGHT];
	char array[PORT_LENGHT];
	char *port;
	jint *arr_req;
	jint *arr_resp;
	jint req_length =0;
	jint resp_length =0;
	
	port = Jstring2CStr(env, jport);
	
	arr_req = env->GetIntArrayElements(reqArray,NULL);
	req_length = env->GetArrayLength(reqArray);
	for(int i = 0; i < req_length; i++){
		req[i] = (char)arr_req[i];
	}
	
	arr_resp = env->GetIntArrayElements(respArray,NULL);
	resp_length = env->GetArrayLength(respArray);
	for(int i = 0; i < resp_length; i++){
		resp[i] = (char)arr_resp[i];
	}
	
	ret=OpenCom(portno,port,jbaud);
	LOGD(" port = %s baud = %d \n",port,jbaud);
    if(ret==-1)
    {
        LOGE(err,"The %s open error.",port);
        perror(err);
        exit(1);
    }

    //-------------------------------------------------------->
    //-------------------------------------------------------->
	usleep(1000*20);
	char cmdData[5];
	char cmdPowerOnData[6];
	char cmdPowerOffData[5];
	char respData[128];
	memset(cmdData, 0, sizeof(cmdData));
	memset(cmdPowerOnData, 0, sizeof(cmdPowerOnData));
	memset(cmdPowerOffData, 0, sizeof(cmdPowerOffData));
	memset(respData, 0, sizeof(respData));

	//send get card slot data
	cmdData[0] = (char)0xAA;
	cmdData[1] = (char)0x00;
	cmdData[2] = (char)0x02;
	cmdData[3] = (char)0x11;  //card slot 1
	cmdData[4] = (char)0xB9;
	for(int i = 0; i < 5; i++){
		LOGD(" cmdData[%d] = %02x \n",i,cmdData[i]);
	}
	memset(array, 0, PORT_LENGHT);
	nWritten=ComWrt(portno, cmdData, 5);

	int cardSlotStatus = 0;
	int cardActivitionStatus = 0;
	int readCount = 100;

		while(readCount){
			nRead = ComRd(portno, array, PORT_LENGHT, 10000);
			LOGD(" nRead:%d array[0]:0x%02x array[4]:0x%02x	array[5]:0x%02x\\n", nRead, array[0], array[4], array[5]);
			if(nRead>0 && (array[0] == 0xAA) && (array[4] == 0x00)&& ((array[5] == 0x00) || (array[5] == 0x01)) ){
				cardSlotStatus = 1;
				break;
			}else{
				readCount--;
			}
		}
	
	for(int i = 0; i < nRead; i++){
		LOGD(" array[%d] = %02x \n",i,array[i]);
	}
	if(cardSlotStatus == 1){
		cmdPowerOnData[0] = (char)0xAA;
		cmdPowerOnData[1] = (char)0x00;
		cmdPowerOnData[2] = (char)0x03;
		cmdPowerOnData[3] = (char)0x21;
		cmdPowerOnData[4] = (char)0x04;
		cmdPowerOnData[5] = (char)0x8C;
		for(int i = 0; i < 6; i++){
			LOGD(" cmdPowerOnData[%d] = %02X \n",i,cmdPowerOnData[i]);
		}
		nWritten=ComWrt(portno, cmdPowerOnData, 6);
		LOGD(" send power on data %d chars!\n",nWritten);
		readCount = 100;
		while(readCount){
			nRead = ComRd(portno, array, PORT_LENGHT, 10000);
			LOGD(" nRead:%d array[0]:0x%02x\n", nRead, array[0]);
			if(nRead>0 && (array[0] == 0xAA) && (array[4] == 0x00)){
				cardActivitionStatus = 1;
				break;
			}else{
				readCount--;
			}
		}
	}else LOGD("get card slot status fail.");

	
	for(int i = 0; i < nRead; i++){
		LOGD(" array[%d] = %02x \n",i,array[i]);
	}

	if( (cardSlotStatus == 1) && (cardActivitionStatus == 1) ){
		jclass java_class = env->FindClass("com/meigsmart/meigrs32/util/SerialPort");
		if(java_class == 0){
			LOGI(" SerialPort class not found");
		}else{
			LOGI(" SerialPort class found");
			jmethodID method = env->GetMethodID(java_class, "setStatus", "(Z)V");
			if(method == 0){
				LOGI(" setStatus method not found");
			}else{
				LOGI(" callback setStatus method");
				env->CallVoidMethod( obj, method,true);
			}
		}
	}
	
	//send power off data
	if(cardActivitionStatus == 1){
	cmdPowerOffData[0] = (char)0xAA;
	cmdPowerOffData[1] = (char)0x00;
	cmdPowerOffData[2] = (char)0x02;
	cmdPowerOffData[3] = (char)0x31;
	cmdPowerOffData[4] = (char)0x99;
	for(int i = 0; i < sizeof(cmdPowerOffData); i++){
		LOGD(" cmdPowerOffData[%d] = %02x \n",i,cmdPowerOffData[i]);
	}
	nWritten=ComWrt(portno, cmdPowerOffData, 5);
	LOGD(" send power off data %d chars!\n",nWritten);
	fflush(stdout);
	}else LOGD("card not activition.");
	usleep(1000*1000);

    if((CloseCom(portno)==-1))
    {
        LOGD("Close com");
        exit(1);
    }

    LOGD("Exit now.\n");
}