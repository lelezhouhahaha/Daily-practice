#include "vtest_Core.h"
#include <pthread.h>
#include "vtest_Script.h"
#include "vtest_Debug.h"
#include "vtest_ComDef.h"
#include "vtest_ITestCase.h"
#include "vtest_TestCaseFactory.h"
#include "vtest_Sleeper.h"
#include "vtest_Time.h"
#include "vtest_Mutex.h"

#define VTEST_DEMUXER_LOCATION "/data/misc/media/"
#define MEM_BUF_1440     1024*1024*50
#define MEM_BUF_1080     1024*1024*42
#define MEM_BUF_720_480  1024*1024*30




static inline int32_t Round(float x)
{
  return (int32_t) ((x > 0) ? (x+0.5f) : (x-0.5f));
}

static OMX_STRING GetString(OMX_STRING (*p), int index) {
    return *(p + index);
}

static void GetFileName(void *user) {
    vtest::VideoCore* pThis = (vtest::VideoCore*)user;
    sprintf(pThis->sGlobalVideoProp.sOutRoot,"%s",VTEST_DEMUXER_LOCATION);
    if(pThis->eStreamType == Stream_Local) { //Stream_Local
        sprintf(pThis->sGlobalVideoProp.sOutFileName, "%d_%d_%06d", pThis->nWidth, pThis->nHeight, pThis->nVideoPart);
    } else { //Stream_Upload
        sprintf(pThis->sGlobalVideoProp.sOutFileName, "Upload_%d_%d_%06d", pThis->nWidth, pThis->nHeight, pThis->nVideoPart);
    }
    pThis->nVideoPart++;
    VTEST_MSG_HIGH("denghan Root = %s, Name = %s", pThis->sGlobalVideoProp.sOutRoot, pThis->sGlobalVideoProp.sOutFileName);
    return;
}

static void *startRecord(void *user) {

    OMX_ERRORTYPE result = OMX_ErrorNone;
    vtest::VideoCore* pThis = (vtest::VideoCore*)user;
    vtest::ITestCase *pVidTest = NULL;
    char sTemp[MAX_STR_LEN];
    VTEST_MSG_HIGH("...");

    pThis->pMutex->Lock();
    VTEST_MSG_HIGH("get Lock.");
    pThis->pMutex->UnLock();
    for(; pThis->eState == State_Recording || pThis->ePreState == State_Recording; ) {
        pVidTest = vtest::TestCaseFactory::AllocTest(pThis->sSessionInfo.SessionType);
        if (pVidTest == NULL) {
            VTEST_MSG_ERROR("Unable to alloc video test: %s", pThis->sSessionInfo.SessionType);
            return pThis;
        }

        if(pThis->eCurMode == Mode_Rec) {
            GetFileName(user);
            sprintf(sTemp,"%s",pThis->sGlobalVideoProp.sOutRoot);
            sprintf(pThis->sGlobalVideoProp.sOutRoot,"%s",VTEST_DEMUXER_LOCATION);
            if(!strcmp(pThis->PreFileName, pThis->sGlobalVideoProp.sOutFileName))
                strlcpy(pThis->sGlobalVideoProp.sOutFileName, "PreToRec", vtest::VTEST_MAX_STRING);
        }
        VTEST_MSG_HIGH("PreOutRoot %s, PreFileName = %s, sOutRoot = %s, sOutFileName = %s, sTemp = %s.",
                        pThis->PreOutRoot, pThis->PreFileName, pThis->sGlobalVideoProp.sOutRoot, pThis->sGlobalVideoProp.sOutFileName, sTemp);
        if(pThis->pfnAviOnCB != NULL) {
            if(pThis->eCurMode == Mode_Pre) {
                pThis->pfnAviOnCB(pThis->PreOutRoot, pThis->PreFileName, (pThis->eCodingType == VIDEO_CodingAVC ? 0 : 1));
            } else {
                pThis->pfnAviOnCB(sTemp, pThis->sGlobalVideoProp.sOutFileName, (pThis->eCodingType == VIDEO_CodingAVC ? 0 : 1));
            }
        }
        if(pThis->pfnAviSaveCB != NULL) { //alloc memory only local video start
            if(pThis->nHeight == 1440){
               pThis->video_buf = (char *) malloc(MEM_BUF_1440);
               VTEST_MSG_HIGH("video_buf size MEM_BUF_1440 %d",MEM_BUF_1440);
            }
            if(pThis->nHeight == 1080){
               pThis->video_buf = (char *) malloc(MEM_BUF_1080);
               VTEST_MSG_HIGH("video_buf size MEM_BUF_1080 %d",MEM_BUF_1080);
            }
            if(pThis->nHeight == 720 || pThis->nHeight == 480){
               pThis->video_buf = (char *) malloc(MEM_BUF_720_480);
               VTEST_MSG_HIGH("video_buf size MEM_BUF_720_480 %d",MEM_BUF_720_480);
            }
            if(!pThis->video_buf){
               VTEST_MSG_HIGH("video_buf malloc error!\n");
               return pThis;
            }
            pThis->memcpy_offset = 0;
            VTEST_MSG_HIGH("nWidth %d nHeight %d video_buf %p memcpy_offset %d",
               pThis->nWidth,
               pThis->nHeight,
               pThis->video_buf,
               pThis->memcpy_offset);
        }
        if(pVidTest != NULL) {
            VTEST_MSG_HIGH("Running vid test %s", pThis->sSessionInfo.SessionType);
            result = pVidTest->Start(&pThis->sGlobalVideoProp, &pThis->sSessionInfo);
            if (result != OMX_ErrorNone) {
                VTEST_MSG_ERROR("Error starting vid test");
                return pThis;
            } else {
                result = pVidTest->Finish();
                if (result != OMX_ErrorNone) {
                    VTEST_MSG_ERROR("Vid Test failed");
                    return pThis;
                } else {
                    VTEST_MSG_HIGH("Vid Test pass");
                }
            }
        }
        if(pThis->eState == State_Stopping) {
            pThis->nRealFrameRate = Round(pThis->nFrameNum*10*1000.0/pThis->nRunTimeMillis);
            VTEST_MSG_HIGH("Last video framerate:%d *10 * 1000 / %lld = %d!", pThis->nFrameNum, pThis->nRunTimeMillis, pThis->nRealFrameRate);
        }
        //snprintf(sTemp, sizeof(sTemp), "%.1f#%s", pThis->nRealFrameRate/10.0, pThis->sGlobalVideoProp.sOutRoot);
        if(pThis->pfnAviSaveCB != NULL) {
            if(pThis->eLastMode == Mode_Pre) {
                VTEST_MSG_HIGH("nPreLength = %d", pThis->nPreLength);
                VTEST_MSG_HIGH("PreOutRoot = %s, PreFileName = %s", pThis->PreOutRoot, pThis->PreFileName);
                usleep(200*1000);
                pThis->pfnMemBufCB(pThis->video_buf, pThis->memcpy_offset, (pThis->nRealFrameRate/10.0), (pThis->eCodingType == VIDEO_CodingAVC ? 0 : 1));
                pThis->pfnAviSaveCB(pThis->PreOutRoot, pThis->PreFileName, (pThis->nRealFrameRate/10.0), (pThis->eCodingType == VIDEO_CodingAVC ? 0 : 1));
            } else {
                VTEST_MSG_HIGH("nRealFrameRate = %.2f", (pThis->nRealFrameRate/10.0));
                pThis->pfnMemBufCB(pThis->video_buf, pThis->memcpy_offset, (pThis->nRealFrameRate/10.0), (pThis->eCodingType == VIDEO_CodingAVC ? 0 : 1));
                pThis->pfnAviSaveCB(sTemp, pThis->sGlobalVideoProp.sOutFileName, (pThis->nRealFrameRate/10.0), (pThis->eCodingType == VIDEO_CodingAVC ? 0 : 1));
                //pThis->pfnAviSaveCB(sTemp, pThis->sGlobalVideoProp.sOutFileName, (pThis->eCodingType == VIDEO_CodingAVC ? 0 : 1));
            }
        }

        if(pThis->eState == State_Stopping)
            pThis->eState = State_Stopped;
        if(pThis->ePreState == State_Stopping)
            pThis->ePreState = State_Stopped;

        pThis->eLastMode = pThis->eCurMode;
        pThis->eCurMode = Mode_Rec;
        VTEST_MSG_HIGH("CurMode = %s, LastMode = %s", GetString(ModeString, pThis->eCurMode), GetString(ModeString, pThis->eLastMode));
        VTEST_MSG_HIGH("PreState = %s, State = %s", GetString(StateString, pThis->ePreState), GetString(StateString, pThis->eState));

        vtest::TestCaseFactory::DestroyTest(pVidTest);
    }

    return pThis;
}

static void *writeFile(void *user) {

    int result = 0;
    vtest::VideoCore* pThis = (vtest::VideoCore*)user;
    LQNode *node = NULL;
    //FILE *f = NULL;
    //char newFileName[MAX_STR_LEN] = "data/misc/media/";
    VTEST_MSG_HIGH("...");
/*
    if(pThis->eCodingType == VIDEO_CodingAVC)
        f = fopen("data/misc/media/prerecord.264", "wb");
    else
        f = fopen("data/misc/media/prerecord.265", "wb");

    if (f == NULL) {
        VTEST_MSG_ERROR("Unable to open file");
        return NULL;
    }
    result = (int)fwrite(pThis->sFileHead, 1, pThis->sHeadSize, (FILE *)f);
*/
    memcpy(pThis->video_buf + pThis->memcpy_offset, pThis->sFileHead, pThis->sHeadSize);
    pThis->memcpy_offset = pThis->memcpy_offset+pThis->sHeadSize;
    for(; pThis->pLinkQueue->GetSize() > 0; ) {
        pThis->pLinkQueue->Dequeue(&node);
        if(node) {
            memcpy(pThis->video_buf + pThis->memcpy_offset, node->data, node->data_size);

            pThis->memcpy_offset = pThis->memcpy_offset+node->data_size;
            //result = (int)fwrite(node->data, 1, node->data_size, (FILE *)f);
            pThis->pLinkQueue->FreeNode(node);
        }
    }
/*
    fclose((FILE *)f);

    strlcat((OMX_STRING)newFileName, (OMX_STRING)pThis->PreFileName, vtest::VTEST_MAX_STRING);
    if(pThis->eCodingType == VIDEO_CodingAVC) {
        strlcat((OMX_STRING)newFileName, ".264", vtest::VTEST_MAX_STRING);
        VTEST_MSG_HIGH("New File name %s", newFileName);
        rename("data/misc/media/prerecord.264", newFileName);
    } else {
        strlcat((OMX_STRING)newFileName, ".265", vtest::VTEST_MAX_STRING);
        VTEST_MSG_HIGH("New File name %s", newFileName);
        rename("data/misc/media/prerecord.265", newFileName);
    }
*/
    if(pThis->pTimeQueue != NULL) {
        delete pThis->pTimeQueue;
        pThis->pTimeQueue = NULL;
    }
    if(pThis->pLinkQueue != NULL) {
        delete pThis->pLinkQueue;
        pThis->pLinkQueue = NULL;
    }
    return pThis;
}

namespace vtest {

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
VideoCore::VideoCore()
    : nWidth(0),
      nHeight(0),
      nFrameSize(0),
      nBitrate(0),
      nFrameRate(30),
      nPFrames(30),
      nPartTimes(0),
      nPreTimes(0),
      bUpload(OMX_FALSE),
      bTimeout(OMX_FALSE),
      nStartTime(0),
      nEndTime(0),
      nRunTimeMillis(0),
      nFrameNum(0),
      nRealFrameRate(0),
      nVideoPart(0),
      eState(State_Stopped),
      ePreState(State_Stopped),
      eCurMode(Mode_Unknow),
      eLastMode(Mode_Unknow),
      eSourceFormat(Format_NV12),
      eCodingType(VIDEO_CodingHEVC),
      eProfileType(VIDEO_HEVCProfileMain),
      eStreamType(Stream_Local),
      pXmlParser(NULL),
      pSignalQueue(NULL),
      pMutex(new Mutex()),
      pQueueMutex(new Mutex()),
      pLinkQueue(NULL),
      pTimeQueue(NULL),
      bPreFile(OMX_FALSE),
      PreOutRoot("OutRoot/"),
      PreFileName("prefile"),
      pfnUploadCB(NULL),
      pfnAviOnCB(NULL),
      pfnAviSaveCB(NULL),
      nFrameInterval(0),
      nTime(0) {

    VTEST_MSG_HIGH("VideoCore: created...");
}

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
VideoCore::~VideoCore() {

    VTEST_MSG_HIGH("start");
    if (pXmlParser != NULL) {
        delete pXmlParser;
        pXmlParser = NULL;
    }
    if (pSignalQueue != NULL) {
        delete pSignalQueue;
        pSignalQueue = NULL;
    }
    if (pLinkQueue != NULL) {
        delete pLinkQueue;
        pLinkQueue = NULL;
    }
    if (pfnUploadCB != NULL) {
        pfnUploadCB = NULL;
    }
    if (pfnAviOnCB != NULL) {
        pfnAviOnCB = NULL;
    }
    if (pfnAviSaveCB != NULL) {
        pfnAviSaveCB = NULL;
    }
    if (pMutex != NULL) {
        delete pMutex;
        pMutex = NULL;
    }
    if (pQueueMutex != NULL) {
        delete pQueueMutex;
        pQueueMutex = NULL;
    }
    VTEST_MSG_HIGH("done");
}

int VideoCore::RegisterUploadCallback(PFN_UPLOAD_CB pfnUploadCb) {
    if(pfnUploadCb != NULL) {
        bUpload = OMX_TRUE;
        pfnUploadCB = pfnUploadCb;
        VTEST_MSG_HIGH("Enable upload and Register upload callback!");
    }
    return 0;
}

int VideoCore::RegisterAviOnCallback(PFN_AVI_ON_CB pAviOnCb) {
    if(pAviOnCb != NULL) {
        pfnAviOnCB = pAviOnCb;
        VTEST_MSG_HIGH("Register avi stream on callback!");
    }
    return 0;
}

int VideoCore::RegisterAviSaveCallback(PFN_AVI_SAVE_CB pAviSaveCb) {
    if(pAviSaveCb != NULL) {
        pfnAviSaveCB = pAviSaveCb;
        VTEST_MSG_HIGH("Register avi stream save callback!");
    }
    return 0;
}

int VideoCore::RegisterAviMemBufCallback(PFN_AVI_MEM_BUF_CB pAviMemBufCb) {
    if(pAviMemBufCb != NULL) {
        pfnMemBufCB = pAviMemBufCb;
        VTEST_MSG_HIGH("Register pfnMemBufCB callback!");
    }
    return 0;
}


void VideoCore::SetResolution(OMX_U32 width, OMX_U32 height) {
    if(ePreState == State_Stopped && eState == State_Stopped) {
        nWidth = width;
        nHeight = height;
        nFrameSize = (nWidth*nHeight*3) >> 1;
        if (pSignalQueue != NULL) {
            delete pSignalQueue;
        }
        if (eStreamType == Stream_Local) { //Stream_Local
            pSignalQueue = new SignalQueue(LOCAL_FRAME_QUEUE_SIZE, nFrameSize);
        } else { //Stream_Upload
            pSignalQueue = new SignalQueue(UPLOAD_FRAME_QUEUE_SIZE, nFrameSize);
        }
        VTEST_MSG_HIGH("nWidth = %d, nHeight = %d!", nWidth, nHeight);
    } else {
        VTEST_MSG_HIGH("Can not set res when prerecording or recording!");
    }
}

void VideoCore::SetBitrate(OMX_U32 bitrate) {
    nBitrate = bitrate * 1000;//Kb -->> bit
    VTEST_MSG_HIGH("nBitrate = %d bit!", nBitrate);
}

void VideoCore::SetFramerate(OMX_U32 frameRate) {
    if(ePreState == State_Recording) {
        VTEST_MSG_HIGH("Can not set framerate when pre recording!");
        return;
    }
    if(frameRate > 0) {
        nStartTime = Time::GetTimeMicrosec();
        nFrameInterval = (OMX_TICKS)(1*1000*1000/frameRate);
        nTime = nStartTime - nFrameInterval;
        nFrameNum = 0;
        nFrameRate = frameRate;
        VTEST_MSG_HIGH("nFrameRate = %d!", nFrameRate);
        VTEST_MSG_HIGH("nFrameInterval = %lld us!", nFrameInterval);
    } else {
        VTEST_MSG_HIGH("Error parameters. frameRate must > 0.");
    }
}

void VideoCore::SetPFrame(OMX_U32 pFrames) {
    nPFrames = pFrames;
    VTEST_MSG_HIGH("nPFrames = %d!", nPFrames);
}

void VideoCore::SetPartTimes(OMX_U32 times) {
    nPartTimes = times * 1000;  // convert to millis
    VTEST_MSG_HIGH("nPartTimes = %d ms!", nPartTimes);
}

void VideoCore::SetPreTimes(OMX_U32 times) {
    nPreTimes = times * 1000;  // convert to millis
    VTEST_MSG_HIGH("nPreTimes = %d ms!", nPreTimes);
}

void VideoCore::SetSourceFormat(SourceFormat format) {
    eSourceFormat = format;
    VTEST_MSG_HIGH("set eSourceFormat to %s!", eSourceFormat?"NV12":"NV21");
}

void VideoCore::SetCodingType(VideoCodingType codingType,
        VideoCodecProfileType profileType) {
    eCodingType = codingType;
    eProfileType = profileType;
}

void VideoCore::SetStreamType(StreamType type) {
    eStreamType = type;
    VTEST_MSG_HIGH("set Stream type to %s!", eStreamType?"Stream_Upload":"Stream_Local");
}

int VideoCore::Init() {
    int result = 0;
    char masterXmlLocation[MAX_STR_LEN];
    char xmlpath[MAX_STR_LEN] = "system/etc/camera";
    char xmlname[MAX_STR_LEN] = "system/etc/camera/SampleEncodeTest.xml";
    VTEST_MSG_HIGH("Entering Init\n");

    pXmlParser = new vtest::XmlParser;
    //Master XML Parsing
    if (0 != pXmlParser->ResolveMasterXmlPath(xmlpath, masterXmlLocation)) {
        VTEST_MSG_ERROR("Input %s is neither a valid path nor a valid filename\n", xmlpath);
        return -1;
    }

    if (0 != pXmlParser->ProcessMasterConfig(&sGlobalVideoProp)) {
        VTEST_MSG_ERROR("Error while processing MasterXml\n");
        return -1;
    }

    sGlobalVideoProp.pVideoCore = (void*)this;

    //Session XML Parsing and Running
    memset((char*)&sSessionInfo, 0, sizeof(vtest::VideoSessionInfo));
    if (0 != pXmlParser->ParseSessionXml((OMX_STRING)xmlname, &sGlobalVideoProp, &sSessionInfo)) {
        VTEST_MSG_ERROR("Error while processing SessionXml and starting test\n");
        return -1;
    }

    return result;
}

int VideoCore::PreStart() {
    int result = 0;
    nPartTimes = 0;
    nPFrames = 5;//Force I frame spacing to 0 when prerecord
    pMutex->Lock();
    if(ePreState == State_Stopped && eState == State_Stopped) {
        pthread_t thread;
        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, 1);
        int err = pthread_create(&thread, &attr, startRecord, (void *)this);
        if(err != 0){
            VTEST_MSG_ERROR("can't create startRecord: %s\n", strerror(err));
        } else {
            bPreFile = OMX_TRUE;
            pLinkQueue = new LinkQueue();
            pLinkQueue->SetTimeThreshold(nPreTimes); //set time threshold, fixed to 30000 ms at default
            pTimeQueue = new std::queue<OMX_TICKS>;
            ePreState = State_Recording;
            nStartTime = Time::GetTimeMicrosec();
            eLastMode = eCurMode;
            eCurMode = Mode_Pre;
            bTimeout = OMX_FALSE;
            VTEST_MSG_HIGH("Set video state to Recording, start time = %lld.", nStartTime);
        }
    } else {
        VTEST_MSG_ERROR("VideoCore is not prepare to prerecord!");
    }
    pMutex->UnLock();
    VTEST_MSG_HIGH("CurMode = %s, LastMode = %s", GetString(ModeString, eCurMode), GetString(ModeString, eLastMode));
    VTEST_MSG_HIGH("PreState = %s, State = %s", GetString(StateString, ePreState), GetString(StateString, eState));
    return result;
}

int VideoCore::PreStop() {
    int result = 0;

    if(ePreState == State_Recording) {
        ePreState = State_Stopping;
        VTEST_MSG_HIGH("PreState = %s, State = %s", GetString(StateString, ePreState), GetString(StateString, eState));
        do {
            VTEST_MSG_HIGH("Sleeping... and wait encoder to exit.");
            vtest::Sleeper::Sleep(50);
        }while(ePreState != State_Stopped);

        eCurMode = Mode_Unknow;
        eLastMode = Mode_Unknow;
        bPreFile = OMX_FALSE;
        nStartTime = 0;
        if(pTimeQueue != NULL) {
            delete pTimeQueue;
            pTimeQueue = NULL;
        }
        if(pLinkQueue != NULL) {
            delete pLinkQueue;
            pLinkQueue = NULL;
        }
        pQueueMutex->Lock();
        if (pSignalQueue != NULL) {
            delete pSignalQueue;
            pSignalQueue = NULL;
        }
        pQueueMutex->UnLock();
    } else {
        VTEST_MSG_ERROR("VideoCore is not prepare to stop!");
        result = -1;
    }
    VTEST_MSG_HIGH("CurMode = %s, LastMode = %s", GetString(ModeString, eCurMode), GetString(ModeString, eLastMode));
    VTEST_MSG_HIGH("PreState = %s, State = %s", GetString(StateString, ePreState), GetString(StateString, eState));
    return result;
}

int VideoCore::Start() {
    int result = 0;
    pMutex->Lock();
    if(eState == State_Stopped && ePreState == State_Stopped) {
        pthread_t thread;
        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, 1);
        int err = pthread_create(&thread, &attr, startRecord, (void *)this);
        if(err != 0){
            VTEST_MSG_ERROR("can't create startRecord: %s\n", strerror(err));
        } else {
            VTEST_MSG_HIGH("create startRecord\n");
        }
    }

    eState = State_Recording;
    if(eCurMode == Mode_Pre) {
        GetFileName((void *)this);
        ePreState = State_Stopped;
        pLinkQueue->SetState();
    }
    eLastMode = eCurMode;
    eCurMode = Mode_Rec;
    bTimeout = OMX_FALSE;
    pMutex->UnLock();
    VTEST_MSG_HIGH("CurMode = %s, LastMode = %s", GetString(ModeString, eCurMode), GetString(ModeString, eLastMode));
    VTEST_MSG_HIGH("PreState = %s, State = %s", GetString(StateString, ePreState), GetString(StateString, eState));
    VTEST_MSG_HIGH("start time = %lld.", nStartTime);
    return result;
}

int VideoCore::Stop() {
    int result = 0;

    if(eState == State_Recording) {
        eState = State_Stopping;
        VTEST_MSG_HIGH("PreState = %s, State = %s", GetString(StateString, ePreState), GetString(StateString, eState));
        do {
            VTEST_MSG_HIGH("Sleeping... and wait encoder to exit.");
            vtest::Sleeper::Sleep(50);
        }while(eState != State_Stopped);
        eCurMode = Mode_Unknow;
        eLastMode = Mode_Unknow;
        pQueueMutex->Lock();
        if (pSignalQueue != NULL) {
            delete pSignalQueue;
            pSignalQueue = NULL;
        }
        pQueueMutex->UnLock();
        nStartTime = 0;
    } else {
        VTEST_MSG_ERROR("VideoCore is not prepare to stop!");
        result = -1;
    }
    VTEST_MSG_HIGH("CurMode = %s, LastMode = %s", GetString(ModeString, eCurMode), GetString(ModeString, eLastMode));
    VTEST_MSG_HIGH("PreState = %s, State = %s", GetString(StateString, ePreState), GetString(StateString, eState));
    return result;
}

int VideoCore::Write() {
    int result = 0;

    pthread_t thread;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, 1);
    int err = pthread_create(&thread, &attr, writeFile, (void *)this);
    if(err != 0){
        VTEST_MSG_ERROR("can't create startRecord: %s\n", strerror(err));
    } else {
        VTEST_MSG_HIGH("Start to writefile.");
    }
    return result;
}

OMX_BOOL VideoCore::FrameFilter() {
    OMX_BOOL flag = OMX_FALSE;
    VTEST_MSG_LOW("E");
    if((nEndTime - nTime) >= nFrameInterval) {
        nTime += nFrameInterval;
        flag = OMX_TRUE;
    } else {
        flag = OMX_FALSE;
    }
    VTEST_MSG_LOW("%s", flag?"selected":"ignored");
    VTEST_MSG_LOW("X");
    return flag;
}

int VideoCore::Push(void *frameData) {
    int result = 0;
    if(!pSignalQueue || !frameData) {
        VTEST_MSG_ERROR("pSignalQueue or frameData are invalid!");
        result = -1;
    } else {
        if(ePreState != State_Stopped || eState != State_Stopped) {
            nEndTime = Time::GetTimeMicrosec();
            if(FrameFilter()) {
                if(pLinkQueue != NULL && pLinkQueue->GetDeltTime()) {
                    nStartTime += pLinkQueue->GetDeltTime();
                    pLinkQueue->CleanDeltTime();
                    nFrameNum -= pLinkQueue->GetDeltLength();
                    pLinkQueue->CleanDeltLength();
                    VTEST_MSG_HIGH("nStartTime time %lld !", nStartTime);
                }
                nFrameNum ++;
                if(pTimeQueue != NULL && bPreFile) {
                    pTimeQueue->push(nEndTime);
                    VTEST_MSG_LOW("Push time %lld !", nEndTime);
                }
                nRunTimeMillis = (nEndTime - nStartTime) / 1000;   // convert to millis
                if(nPartTimes && (nRunTimeMillis > nPartTimes) && (eState != State_Stopping)) {
                    nStartTime = nStartTime + nPartTimes*1000; //convert to us
                    bTimeout = OMX_TRUE;
                    nRealFrameRate = Round(nFrameNum*10*1000.0/nRunTimeMillis);
                    VTEST_MSG_HIGH("Time out ! framerate:%d *10 * 1000 / %lld = %d!", nFrameNum, nRunTimeMillis, nRealFrameRate);
                    nFrameNum = 0;
                }
                pQueueMutex->Lock();
                if(NULL != pSignalQueue) {
                    result = pSignalQueue->Push(frameData, nFrameSize);
                } else {
                    VTEST_MSG_ERROR("pSignalQueue is invalid!");
                }
                pQueueMutex->UnLock();
                if(result != 0) {
                    VTEST_MSG_ERROR("%s: Push frame data failed!", eStreamType?"Stream_Upload":"Stream_Local");
                } else {
                    VTEST_MSG_LOW("Push frame data successed!");
                }
            } else {
                VTEST_MSG_LOW("This frame do not push!");
            }
        } else {
            VTEST_MSG_HIGH("VideoCore is not prepare to record, ignore this frame!");
        }
    }
    return result;
}
}
