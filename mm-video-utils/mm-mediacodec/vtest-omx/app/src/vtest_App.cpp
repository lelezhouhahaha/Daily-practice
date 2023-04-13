/*-------------------------------------------------------------------
Copyright (c) 2013-2014, 2016 Qualcomm Technologies, Inc.
All Rights Reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.

Copyright (c) 2010 The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of The Linux Foundation nor
      the names of its contributors may be used to endorse or promote
      products derived from this software without specific prior written
      permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--------------------------------------------------------------------------*/

#include <stdlib.h>
#include "vtest_Script.h"
#include "vtest_Debug.h"
#include "vtest_ComDef.h"
#include "vtest_XmlComdef.h"
#include "vtest_XmlParser.h"
#include "vtest_ITestCase.h"
#include "vtest_TestCaseFactory.h"
#include "vtest_Sleeper.h"
#include "mm_qcamera_unit_test.h"
#include "mm_qcamera_app.h"

OMX_ERRORTYPE RunTest(vtest::VideoStaticProperties* pGlobalVideoProp, vtest::VideoSessionInfo* pSessionInfo, vtest::VideoSessionInfo* pVidSessionInfo) {

    OMX_ERRORTYPE result = OMX_ErrorNone;
    static OMX_S32 testNum = 0;
    testNum++;
	vtest::ITestCase *pVidTest = NULL;
    vtest::ITestCase *pTest =
        vtest::TestCaseFactory::AllocTest(pSessionInfo->SessionType);
    if (pTest == NULL) {
        VTEST_MSG_CONSOLE("Unable to alloc test: %s", pSessionInfo->SessionType);
        return OMX_ErrorInsufficientResources;
    }
	if(pVidSessionInfo != NULL) {
		pVidTest = vtest::TestCaseFactory::AllocTest(pVidSessionInfo->SessionType);
		if (pVidTest == NULL) {
			VTEST_MSG_CONSOLE("Unable to alloc video test: %s", pVidSessionInfo->SessionType);
			return OMX_ErrorInsufficientResources;
		}
	}
    VTEST_MSG_CONSOLE("Running test %s", pSessionInfo->SessionType);
    result = pTest->Start(testNum,OMX_FALSE,pGlobalVideoProp, pSessionInfo);
    if (result != OMX_ErrorNone) {
        VTEST_MSG_CONSOLE("Error starting test");
    } else {
		if(pVidSessionInfo != NULL) {
			result = pVidTest->Start(testNum,OMX_TRUE,pGlobalVideoProp, pVidSessionInfo);
			if (result != OMX_ErrorNone) {
				VTEST_MSG_CONSOLE("Error starting vid test");
			} else {
				result = pVidTest->Finish();
				if (result != OMX_ErrorNone) {
					VTEST_MSG_CONSOLE("Vid Test failed");
				} else {
					VTEST_MSG_CONSOLE("Vid Test pass");
				}
			}
		}
        result = pTest->Finish();
        if (result != OMX_ErrorNone) {
            VTEST_MSG_CONSOLE("Test failed");
            if(pGlobalVideoProp->fResult) {
                fprintf(pGlobalVideoProp->fResult, "\nVTEST-OMX %s, %s, FAIL\n", OMX_VTEST_VERSION, pSessionInfo->TestId);
            } else {
                VTEST_MSG_HIGH("Result file not found");
            }
        } else {
            VTEST_MSG_CONSOLE("Test passed");
            if(!strcmp(pSessionInfo->SessionType,"DECODE")) {
                if(pGlobalVideoProp->fResult) {
                    fprintf(pGlobalVideoProp->fResult, "\nVTEST-OMX %s, %s, PASS\n", OMX_VTEST_VERSION, pSessionInfo->TestId);
                } else {
                    VTEST_MSG_HIGH("Result file not found");
                }
            } else if(!strcmp(pSessionInfo->SessionType,"ENCODE")) {
                if(pGlobalVideoProp->fResult) {
                    fprintf(pGlobalVideoProp->fResult, "\nVTEST-OMX %s, %s, EPV Pending \n", OMX_VTEST_VERSION, pSessionInfo->TestId);
                } else {
                    VTEST_MSG_HIGH("Result file not found");
                }
            } else {
                if(pGlobalVideoProp->fResult) {
                    fprintf(pGlobalVideoProp->fResult, "\nVTEST-OMX %s, %s, UNSUPPORTED TEST CASE \n", OMX_VTEST_VERSION, pSessionInfo->TestId);
                } else {
                    VTEST_MSG_HIGH("Result file not found");
                }
            }
        }
    }
    vtest::TestCaseFactory::DestroyTest(pTest);
    return result;
}


int main(int argc, char *argv[]) {
    OMX_ERRORTYPE result = OMX_ErrorNone;
    vtest::XmlParser *pXmlParser = NULL;
    vtest::VideoStaticProperties sGlobalVideoProp;
    vtest::VideoSessionInfo sSessionInfo[MAX_NUM_SESSIONS];
    vtest::VideoSessionInfo *pSessionInfo = NULL;
    vtest::VideoSessionInfo *pVidSessionInfo = NULL;
    vtest::VideoSessionInfo *pBaseSessionInfo = NULL;
    char resultFile[MAX_STR_LEN];
    char masterXmlLocation[MAX_STR_LEN];
	int videoPart;
	int sessionIndex = 0, cameraId = 0, resolution = 0, fps = 0, compressionFormat = 0, times = 5;
	char outFileName[MAX_STR_LEN] = "", outFileRoot[MAX_STR_LEN] = "/data/misc/camera/";
    VTEST_MSG_CONSOLE("Entering TestApp\n");
    OMX_Init();

    if (argc < 3) {
        VTEST_MSG_CONSOLE("At least two arg is necessary .\n Usage: %s <MasterConfg.xml path> <input.xml>\n", argv[0]);
        return OMX_ErrorBadParameter;
    }

    pXmlParser = new vtest::XmlParser;

    //Master XML Parsing
    if (OMX_ErrorNone != pXmlParser->ResolveMasterXmlPath(argv[1], masterXmlLocation)) {
        VTEST_MSG_CONSOLE("Error: Input %s is neither a valid path nor a valid filename\n", argv[1]);
        return OMX_ErrorUndefined;
    }


    if (OMX_ErrorNone != pXmlParser->ProcessMasterConfig(&sGlobalVideoProp)) {
        VTEST_MSG_CONSOLE("Error while processing MasterXml\n");
        return OMX_ErrorUndefined;
    }


    //Also open the Results.Csv file
    SNPRINTF(resultFile, MAX_STR_LEN, "%s/Results.csv", masterXmlLocation);
    sGlobalVideoProp.fResult = fopen(resultFile, "a+");

    if (!sGlobalVideoProp.fResult) {
        VTEST_MSG_CONSOLE("Results.Csv file opening failed");
        return OMX_ErrorUndefined;
    }
	//arg seq: MasterConfg.xml path, input.xml, cameraId, resolution, fps, compressionFormat, time, outFileName, outFileRoot
	VTEST_MSG_CONSOLE("argc = %d", argc);
	switch (argc) {
		case 10:
			strlcpy(outFileRoot, (OMX_STRING)argv[9], MAX_STR_LEN);
		case 9:
			strlcpy(outFileName, (OMX_STRING)argv[8], MAX_STR_LEN);
		case 8:
			times = atoi((OMX_STRING)argv[7]);
			if(times <= 0)
				times = 1;
		case 7:
			if(!strcmp((OMX_STRING)argv[6],"AVC"))
				compressionFormat = 1;
		case 6:
			if(!strcmp((OMX_STRING)argv[5],"60"))
				fps = 1;
		case 5:
			if(!strcmp((OMX_STRING)argv[4],"720p")) {
				resolution = 1;
			}else if(!strcmp((OMX_STRING)argv[4],"480p")) {
				resolution = 3;
			}
		case 4:
			cameraId = atoi((OMX_STRING)argv[3]);
			if(cameraId > 1){
				VTEST_MSG_CONSOLE("camera ID must <= 1.");
			}
			break;
		default:
			VTEST_MSG_CONSOLE("argc <= 3, will use default args.");
	}
	strlcpy(sGlobalVideoProp.sOutRoot, outFileRoot, MAX_STR_LEN);
	strlcpy(sGlobalVideoProp.sOutFileName, outFileName, MAX_STR_LEN);
	sGlobalVideoProp.nTimes = times;
	sessionIndex = resolution + fps + compressionFormat * 5;//1080p30, 720p30, 720p60, 480p30, 480p60
	VTEST_MSG_CONSOLE("use args: cameraId = %d, sessionIndex = %d, times = %d, outFileName = %s, outFileRoot = %s.", cameraId, sessionIndex, times, outFileName, outFileRoot);

    VTEST_MSG_CONSOLE("Processed MasterXml. Starting parsing sessionXml:%s\n", (OMX_STRING)argv[2]);

    //Session XML Parsing and Running
    memset((char*)&sSessionInfo[0], 0, MAX_NUM_SESSIONS * sizeof(vtest::VideoSessionInfo));
    if (OMX_ErrorNone != pXmlParser->ParseSessionXml((OMX_STRING)argv[2], &sGlobalVideoProp, &sSessionInfo[0])) {
        VTEST_MSG_CONSOLE("Error while processing SessionXml and starting test\n");
        return OMX_ErrorUndefined;
    }

    pSessionInfo = pBaseSessionInfo = &sSessionInfo[sessionIndex];
	pVidSessionInfo = &sSessionInfo[sessionIndex];//for video stream session
	
	VTEST_MSG_CONSOLE("dh Start to open camera 0...");
	mm_app_tc_open(cameraId);
	initFrameQueue(OMX_FALSE);//init preview queue
	initFrameQueue(OMX_TRUE);//init video queue
	VTEST_MSG_CONSOLE("dh Start to preview...");
	mm_app_tc_start_preview();

    //while (pSessionInfo->bActiveSession == OMX_TRUE) {
		for(videoPart = 0; videoPart < 2; videoPart++) {
			if(OMX_ErrorNone != RunTest(&sGlobalVideoProp,pSessionInfo,pVidSessionInfo)) {
				VTEST_MSG_CONSOLE("Failed Processing Session: %s\n", pSessionInfo->SessionType);
			} else {
				VTEST_MSG_CONSOLE("Completed Processing Session: %s\n", pSessionInfo->SessionType);
			}
		}

        //pSessionInfo++;
        //if(pSessionInfo >= (pBaseSessionInfo + MAX_NUM_SESSIONS )) {
            //VTEST_MSG_CONSOLE("Exceeded the number of sessions\n");
            //break;
        //}
    //}
	//vtest::Sleeper::Sleep(1000);
	VTEST_MSG_CONSOLE("dh Stop to preview...");
	mm_app_tc_stop_preview();
	VTEST_MSG_CONSOLE("dh close camera...");
	mm_app_tc_close();

    if (sGlobalVideoProp.fResult) {
        fclose(sGlobalVideoProp.fResult);
        sGlobalVideoProp.fResult = NULL;
    }

    if(pXmlParser) {
        delete pXmlParser;
        pXmlParser = NULL;
    }

    OMX_Deinit();
    VTEST_MSG_CONSOLE("Exiting TestApp\n");
    return result;
}
