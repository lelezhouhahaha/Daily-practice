#include <iostream>
#include<string>
#include <vector>
#include "OsdCtl.h"
#include "OsdObj.h"
#include "OsdLog.h"
#include "yuvosd.h"
#include "CLock.cc"
static CLock osdctl_lock;


using namespace std;

OsdCtl *OsdCtl::sInstance = NULL;

OsdCtl *OsdCtl::OSD_Instance() {
    if (!sInstance)
        sInstance = new OsdCtl();
    return sInstance;
}

OsdCtl::OsdCtl(){
       font = NULL;
       font_480p = NULL;
    //    LOGD("OsdCtl Constructor...");
}

OsdCtl::~OsdCtl(){
    //    LOGD("OsdCtl ~ Constructor...");
    //OSD_Release();
}

int OsdCtl::OSD_ReadYuvFile(OsdObj *objYuv, int osd_yuv_480p_flag)
{
    int ret = 0;
    int buf_size = 0;
    char *pYuvBuf = NULL;
    FILE *pYuvHandle = NULL;
    FILE *pYuvHandlebak = NULL;
    char path[100] = "/system/etc/camera/time_str.yuv";
    char path_480p[100] = "/system/etc/camera/time_str_480p.yuv";
#if OSD_DEBUG
    char path_bak[100] = "/system/etc/camera/time_str_bak.yuv";
#endif


    if ( !osd_yuv_480p_flag ) {
        pYuvHandle = fopen(path, "rb");
        if(!pYuvHandle){
            LOGE("open %s failed...", path);
            return -1;
        }

        objYuv->setOsdRes(16, 608);
        buf_size = 16 * 608 * 3 / 2;
        pYuvBuf = (char *)malloc(buf_size);
        ret = fread(pYuvBuf, 1, buf_size, pYuvHandle);
        if(ret != buf_size)
        {
            LOGE("read from yuv file failed...");
            fclose(pYuvHandle);
            return -1;
        }
        objYuv->setOsdYuvBufP(pYuvBuf);
        LOGE("zhou: readyuvfile: osd_yuv_480p_flag = 0");

    } else {

        pYuvHandle = fopen(path_480p, "rb");
        if(!pYuvHandle){
            LOGE("open %s failed...", path_480p);
            return -1;
        }

        objYuv->set480pOsdRes(12, 12*2*19);
        buf_size = 12 * 12 * 2 * 19 * 3 / 2;
        pYuvBuf = (char *)malloc(buf_size);
        ret = fread(pYuvBuf, 1, buf_size, pYuvHandle);
        if(ret != buf_size)
        {
            LOGE("read from yuv file failed...");
            fclose(pYuvHandle);
            return -1;
        }
        objYuv->setOsd480pYuvBufP(pYuvBuf);
        LOGE("zhou: readyuvfile: osd_yuv_480p_flag = 1");
    }


#if OSD_DEBUG
    ret = fwrite(pYuvBuf, 1, buf_size, pYuvHandlebak);
    LOGD("fwrite " << ret << " bytes...");
    fclose(pYuvHandlebak);
#endif
    fclose(pYuvHandle);
    return 0;
}

int OsdCtl::OSD_ReadBmpFile(OsdObj *objBmp, int osd_bmp_480p_flag)
{
    uint_32 ret;
    uint_32 x_res;
    uint_32 y_res;
    uint_32 buf_size;
    int i = 0;
    FILE *pBmpHandle = NULL;

#if OSD_DEBUG
    FILE *pBmpHandlebak = NULL;
    FILE *pYuvHandle = NULL;
    char *YUV_obj = NULL;
#endif

    char *pBmpBuf = NULL;
    unsigned char *rgbData = NULL;
    unsigned char *Y = NULL;
    unsigned char *U = NULL;
    unsigned char *V = NULL;
    int width = 0;
    int height = 0;

    char path[450] = "/data/misc/camera/";
    strcat(path, objBmp->getOsdName().c_str());
    snprintf(path + strlen(path), 5, ".bmp");

    if ( !osd_bmp_480p_flag ) {
        objBmp->getOsdRes(&x_res, &y_res);
        LOGE("zhou: not 480p bmp x_res = %d, y_res = %d", x_res, y_res);
    } else {
        objBmp->get480pOsdRes(&x_res, &y_res);
        LOGE("zhou: 480p bmp x_res = %d, y_res = %d", x_res, y_res);
    }

    buf_size = x_res * y_res * 3 + 14 + 40;
    pBmpBuf = (char *)malloc(buf_size);
	if (pBmpBuf == NULL) {
		LOGE("[xuhao] %s: pBmpBuf malloc buffer failed. \n", __func__);
		return -1;
	}

    //open read to buffer
    pBmpHandle = fopen(path, "rb");
    if(!pBmpHandle){
        LOGE("open %s failed...", path);
        return -1;
    }

#if OSD_DEBUG
    char path_bmp[100] = "/data/misc/camera/";
    char path_yuv[100] = "/data/misc/camera/";
    strcat(path_bmp, objBmp->getOsdName().c_str());
    strcat(path_yuv, objBmp->getOsdName().c_str());
    snprintf(path_bmp + strlen(path_bmp), 9, "_bak.bmp");
    snprintf(path_yuv + strlen(path_bmp), 9, "_bak.yuv");
    pBmpHandlebak = fopen(path_bmp, "wb");
    pYuvHandle = fopen(path_yuv, "wb");
#endif

    ret = fread(pBmpBuf, 1, buf_size, pBmpHandle);
    if(ret != buf_size)
    {
        //LOGE("read from bmp file failed...");
        return -2;
    }
    //LOGE("fread " << ret << " bytes...");
    objBmp->setOsdBmpBufP(NULL);

#if OSD_DEBUG
    ret = fwrite(objBmp->getOsdBmpBufP(), 1, buf_size, pBmpHandlebak);
    LOGE("fwrite " << ret << " bytes...");
    fclose(pBmpHandlebak);
#endif

    fclose(pBmpHandle);

    rgbData = (unsigned char *) malloc(x_res * x_res * 3);
	if (rgbData == NULL) {
		LOGE("[xuhao] %s: rgbData malloc buffer failed \n", __func__);
		return -1;
	}
    memset(rgbData, 0, x_res * x_res *3);
    Y = (unsigned char * )malloc(x_res * x_res);
    U = (unsigned char * )malloc(x_res * x_res / 4);
    V = (unsigned char * )malloc(x_res * x_res / 4);
	if (Y == NULL || U == NULL || V == NULL) {
		LOGE("[xuhao] %s: Y/U/V malloc buffer failed \n", __func__);
		return -1;
	}

    char *YUV = NULL;
    char *YUV_480p = NULL;
    if ( !osd_bmp_480p_flag ) {
        YUV = (char * )malloc(x_res * x_res * 3 / 2);
    } else {
        YUV_480p = (char * )malloc(x_res * x_res * 3 / 2);
    }


    BITMAPFILEHEADER file_header;
    BITMAPINFOHEADER info_header;
    memcpy(&file_header, pBmpBuf, sizeof(BITMAPFILEHEADER));
    memcpy(&info_header, pBmpBuf + sizeof(BITMAPFILEHEADER), sizeof(BITMAPINFOHEADER));

#if OSD_DEBUG
    LOGD("BITMAPFILEHEADER size: " << sizeof(BITMAPFILEHEADER));
    LOGD("BITMAPINFOHEADER size: " << sizeof(BITMAPINFOHEADER));
    LOGD("bfSize: " << file_header.bfSize);
    LOGD("bfOffBits: " << file_header.bfOffBits);
    LOGD("biSize: " << info_header.biSize);
    LOGD("biWidth: " << info_header.biWidth);
    LOGD("biHeight: " << info_header.biHeight);
    LOGD("biBitCout: " << info_header.biBitCount);
#endif
    ReadRGBFromBuf(pBmpBuf, file_header, info_header, rgbData);

    /*LOGD("rgbData...Begin");
      for(i=0; i<100; i++)
      {
        LOGD("%x ", *(rgbData+i));
      }
      LOGD("rgbData...End");*/

    if (((info_header.biWidth * info_header.biBitCount / 8) % 4) == 0)
        width = info_header.biWidth;
    else
        width = (info_header.biWidth * info_header.biBitCount + 31) / 32 * 4;

    if ((info_header.biHeight % 2) == 0)
        height = info_header.biHeight;
    else
        height = info_header.biHeight + 1;

    //LOGD("width: " << width << " height: " << height);
    ret = RGB2YUV(width, height, rgbData, Y, U, V);
    if(ret)
    {
        //LOGE("read from bmp file failed...");
        return -3;
    }

    char *pTemp = NULL;
    if ( !osd_bmp_480p_flag ) {
        pTemp = YUV;
    } else {
        pTemp = YUV_480p;
    }
    memcpy(pTemp, Y, width * height);
    pTemp += width * height;
    memcpy(pTemp, U, width * height / 4);
    pTemp += width * height / 4;
    memcpy(pTemp, V, width * height / 4);
    //FILE *fp_osd_yuv;
    if ( !osd_bmp_480p_flag ) {
        objBmp->setOsdYuvBufP(YUV);
        //fp_osd_yuv = fopen("data/misc/camera/zhou_osd_not480.yuv", "wb+");
        //fwrite(YUV, sizeof(int), (width * height * 3) >> 1, fp_osd_yuv);
        LOGE("zhou: setOsdYuvBufP(YUV): osd_bmp_480p_flag = 0");
        //LOGE("zhou: OsdYuvBufP from bmp = %p", YUV);
    } else {
        objBmp->setOsd480pYuvBufP(YUV_480p);
        //fp_osd_yuv = fopen("data/misc/camera/zhou_osd_480.yuv", "wb+");
        //fwrite(YUV_480p, sizeof(int), (width * height * 3) >> 1, fp_osd_yuv);
        LOGE("zhou: setOsdYuvBufP(YUV): osd_bmp_480p_flag = 1");
        //LOGE("zhou: OsdYuvBufP from bmp = %p", YUV_480p);
    }



#if OSD_DEBUG
    YUV_obj = objBmp->getOsdYuvBufP();
    ret = fwrite(YUV_obj, 1, width * height * 3 / 2, pYuvHandle);
    LOGD("Write " << ret << "Bytes to YUV file...");
    fclose(pYuvHandle);
#endif
    if (Y) {
        free(Y);
	    Y = NULL;
	}
	if (U) {
        free(U);
	    U = NULL;
	}
	if (V) {
        free(V);
	    V = NULL;
	}
	if (pBmpBuf) {
	    LOGE("meig0624 osd ");
	    free(pBmpBuf);
	    pBmpBuf = NULL;
	}
    //delete the bmp file
    remove(path);

    return 0;
}

int OsdCtl::OSD_Add_Obj(OsdObj *obj){
    int ret = 0;
    int x = 0;
    int y = 0;
    int x_480p = 0;
    int y_480p = 0;
    int osd_yuv_480p_flag = 0;
    int osd_bmp_480p_flag = 0;


    if(obj->isOsdTimeStr()){
        /*****************************************************
         * loading dynamic osd yuv {time + longitude_latitude}
         * osd_yuv_buf          -->>  1280p
         * osd_480p_yuv_buf     -->>  720p + 480p
         * 加载动态水印yuv内容
         *****************************************************/
        osd_yuv_480p_flag = 0;
        ret = OSD_ReadYuvFile(obj, osd_yuv_480p_flag);
        if(ret)
        {
            LOGE("OSD_ReadYuvFile failed...");
            return -1;
        }

        ////////////////////////////////////////////////
        osd_yuv_480p_flag = 1;
        ret = OSD_ReadYuvFile(obj, osd_yuv_480p_flag);
        if(ret)
        {
            LOGE("OSD_Read480pYuvFile failed...");
            return -1;
        }
        ////////////////////////////////////////////////

        v.push_back(obj);
    }else{

        OsdObj *objBmp = obj;

        /*****************************************************
         * loading static osd yuv {Police Num}
         * osd_yuv_buf          -->>  1280p
         * osd_480p_yuv_buf     -->>  720p + 480p
         * 加载动态水印yuv内容
         *****************************************************/
        osd_bmp_480p_flag = 0;
		if (font == NULL)
		{
            obj->setOsdFontSize(32);
            font = SDL_TTF_Init(obj->getOsdFontSize());
            if(!font)
            {
                LOGE("SDL_TTF_Init failed...");
                return -1;
            }
		}

        //ttf convert to bmp
        ret = SDL_TTF_Render(font, (char *)obj->getOsdContentStr().c_str(), (char *)obj->getOsdName().c_str(), &x, &y);
        if(ret)
        {
            LOGE("TTF font rendering to bitmap failed... ret: %d", ret);
            return -2;
        }

        objBmp->setOsdRes(x, y);
        //Read bmp file to memory
        ret = OSD_ReadBmpFile(objBmp, osd_bmp_480p_flag);
        if(ret)
        {
            LOGE("OSD read bmp file failed..., ret: %d", ret);
            return -3;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        osd_bmp_480p_flag = 1;
		if (font_480p == NULL) {
            obj->setOsdFontSize(24);
            font_480p = SDL_TTF_Init(obj->getOsdFontSize());
            if(!font_480p)
            {
                LOGE("SDL_TTF_Init 480p failed...");
                return -1;
            }
		}

        //ttf convert to bmp
        ret = SDL_TTF_Render(font_480p, (char *)obj->getOsdContentStr().c_str(), (char *)obj->getOsdName().c_str(), &x_480p, &y_480p);
        if(ret)
        {
            LOGE("TTF font_480p rendering to 480p bitmap failed... ret: %d", ret);
            return -2;
        }

        objBmp->set480pOsdRes(x_480p, y_480p);
        //Read bmp file to memory
        ret = OSD_ReadBmpFile(objBmp, osd_bmp_480p_flag);
        if(ret)
        {
            LOGE("OSD read 480p bmp file failed..., ret: %d", ret);
            return -3;
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //Add obj to obj list
        v.push_back(obj);

    }
    return 0;
}

void OsdCtl::OSD_Del_Obj(OsdObj *obj){
    //Delete obj form obj list
    for(uint_32 i = 0; i < v.size(); i++){
        if(v[i]->getOsdId() == obj->getOsdId()){
            //        LOGE("VectorSize: %d, Del Obj-ID:%d ", v.size(), obj->getOsdId());
#if 1
            if(v[i]->getOsdBmpBufP() != NULL)
            {
                free(v[i]->getOsdBmpBufP());
                v[i]->setOsdBmpBufP(NULL);
            }
            if(v[i]->getOsdYuvBufP() != NULL)
            {
                free(v[i]->getOsdYuvBufP());
                v[i]->setOsdYuvBufP(NULL);
            }
            if(v[i]->getOsd480pYuvBufP() != NULL)
            {
                free(v[i]->getOsd480pYuvBufP());
                v[i]->setOsd480pYuvBufP(NULL);
            }
            v.erase(v.begin() + i);
#else
            delete v[i];
            v.erase(v.begin() + i);
#endif
        }
    }
}

void OsdCtl::OSD_Show_Vector(){
    for(uint_32 i = 0; i < v.size(); i++){
        LOGD("ID: %d, Name:%s, ContenStr:%s\n", v[i]->getOsdId(), v[i]->getOsdName().c_str(), v[i]->getOsdContentStr().c_str());
    }
}

void OsdCtl::OSD_Update_Obj_Content(OsdObj *obj){
    /*if(!obj->isOsdTimeStr()){
        LOGE("Only support update time str content...");
        return;
    }*/
    //Update obj content
    for(uint_32 i = 0; i < v.size(); i++){
        if(v[i]->getOsdId() == obj->getOsdId()){
            v[i]->setOsdContentStr(obj->getOsdContentStr());
        }
    }
}

void OsdCtl::OSD_Start_Composing(char * pYuv420Frame, int iSrcWidth, int iSrcHeight, int video_resolution){
    //LOGD("OSD Start Composing, there was %d osd objs...", v.size());
    char *pYuvObj = NULL;
    uint_32 iResX, iResY;
    uint_32 iStartX, iStartY;
    uint_32 offset_X, offset_Y, gap;
    CLock::Auto lock( osdctl_lock );

    if(v.size() == 0)
    {
        LOGE("No OSD obj to composing, return...");
        return;
    }

    /******************************************************************************
     * Adjust Osd Position in different video resolution
     * 客户要求： 根据录像分辨率来调整水印位置
     ******************************************************************************/
    switch ( video_resolution ){
        case 1:
        {
            offset_X = 20;
            offset_Y = 12;
            gap      = 30;
        }
        break;

        case 2:
        {
            offset_X = 13;
            offset_Y = 7;
            gap      = 25;
        }
        break;

        default:
        {
            offset_X = 30;
            offset_Y = 16;
            gap      = 50;
        }
    }


    for(uint_32 i = 0; i < v.size(); i++){
        if(v[i]->isOsdHide() != true){

            /*******************************************
             * Get Osd Position
             * Get Osd Resolution
             * 根据录像分辨率来获取水印位置和大小
             *******************************************/
            v[i]->getOsdPosition(&iStartX, &iStartY);
            if ( video_resolution == 2 || video_resolution == 1){
                pYuvObj = v[i]->getOsd480pYuvBufP();
                v[i]->get480pOsdRes(&iResX, &iResY);
            } else {
                pYuvObj = v[i]->getOsdYuvBufP();
                v[i]->getOsdRes(&iResX, &iResY);
            }

            if(pYuvObj == NULL)
            {
                LOGE("Yuv Obj Buffer NULL, Composing failed...");
                return;
            }

            /*****************************************************************************
             * Start Overlap
             * iStartX/iStartY              -->> Start Position
             * OSD_overlap_time_caption     -->> Dynamic Osd {time + longitude_latitude}
             * _OSD_overlap_caption         -->> Static  Osd {Police Num}
             *****************************************************************************/
            //if(v[i]->isOsdTimeStr())
            if ( i == 0)
            {
                iStartX = offset_X;
                iStartY = offset_Y;
                _OSD_overlap_caption(pYuv420Frame, iSrcWidth, iSrcHeight,
                                     pYuvObj, (int)iResX, (int)iResY,//OSD图及宽高
                                     (int)iStartX, (int)iStartY);//要叠加的字符串的位置及宽高
			} else if (i == 1)
			{
			    iStartX = offset_X;
                iStartY = offset_Y + gap;
                _OSD_overlap_caption(pYuv420Frame, iSrcWidth, iSrcHeight,
                                     pYuvObj, (int)iResX, (int)iResY,//OSD图及宽高
                                     (int)iStartX, (int)iStartY);//要叠加的字符串的位置及宽高
		    }
			else if (i == 2)
			{
                iStartX = offset_X;
                iStartY = iSrcHeight - iResY - offset_Y;
                _OSD_overlap_caption(pYuv420Frame, iSrcWidth, iSrcHeight,
                                         pYuvObj, (int)iResX, (int)iResY,//OSD图及宽高
                                         (int)iStartX, (int)iStartY);//要叠加的字符串的位置及宽高
            } else if ( i == 3)
			{
                iStartX = iSrcWidth - iResX * strlen(v[i]->getOsdContentStr().c_str()) - offset_X;
                iStartY = iSrcHeight - iResY/19 - offset_Y;
                OSD_overlap_time_caption(pYuv420Frame, iSrcWidth, iSrcHeight,
                                         pYuvObj, (int)iResX, (int)iResY,//OSD图及宽高
                                         (int)iStartX, (int)iStartY, (char *)v[i]->getOsdContentStr().c_str());//要叠加的字符串的位置及宽高
            } else {
                iStartX = iSrcWidth - iResX * strlen(v[i]->getOsdContentStr().c_str()) - offset_X;
                iStartY = offset_Y;
                OSD_overlap_time_caption(pYuv420Frame, iSrcWidth, iSrcHeight,
                                     pYuvObj, (int)iResX,(int)iResY,//OSD图及宽高
                                     (int)iStartX, (int)iStartY, (char *)v[i]->getOsdContentStr().c_str());//要叠加的字符串的位置及宽高
            }
        }
    }
}

void OsdCtl::OSD_Stop_Composing(){
    for(uint_32 i = 0; i < v.size(); i++){
        if( !(v[i]->isOsdTimeStr()) ) {
            //LOGE("zhou: not isOsdTimeStr, i = %d", i);
            LOGE("SDL_TTF_Deinit(font) 0 finish font %p font_480p %p v.size() = %d v[i]->isOsdTimeStr() %d",font,font_480p,v.size(),v[i]->isOsdTimeStr());
            if(font&&font_480p){
                SDL_TTF_Deinit(font,font_480p);
                font = NULL;
                font_480p = NULL;
                LOGE("SDL_TTF_Deinit(font) 1 finish font %p font_480p %p",font,font_480p);
            }
        }
        if(v[i]->getOsdBmpBufP() != NULL)
        {
            //free(v[i]->getOsdBmpBufP());
            v[i]->setOsdBmpBufP(NULL);
        }
        if(v[i]->getOsdYuvBufP() != NULL)
        {
            free(v[i]->getOsdYuvBufP());
            v[i]->setOsdYuvBufP(NULL);
        }
        if(v[i]->getOsd480pYuvBufP() != NULL)
        {
            free(v[i]->getOsd480pYuvBufP());
            v[i]->setOsd480pYuvBufP(NULL);
        }
    }
    v.clear();
}

int OsdCtl::OSD_Update_Obj(OsdObj *obj){
    int ret = 0;
    int x = 0;
    int y = 0;
    int x_480p = 0;
    int y_480p = 0;
    int osd_yuv_480p_flag = 0;
    int osd_bmp_480p_flag = 0;


    if(obj->isOsdTimeStr()){
        /*****************************************************
         * loading dynamic osd yuv {time + longitude_latitude}
         * osd_yuv_buf          -->>  1280p
         * osd_480p_yuv_buf     -->>  720p + 480p
         * 加载动态水印yuv内容
         *****************************************************/
        osd_yuv_480p_flag = 0;
        ret = OSD_ReadYuvFile(obj, osd_yuv_480p_flag);
        if(ret)
        {
            LOGE("OSD_ReadYuvFile failed...");
            return -1;
        }

        ////////////////////////////////////////////////
        osd_yuv_480p_flag = 1;
        ret = OSD_ReadYuvFile(obj, osd_yuv_480p_flag);
        if(ret)
        {
            LOGE("OSD_Read480pYuvFile failed...");
            return -1;
        }
        ////////////////////////////////////////////////

        //v.push_back(obj);
    }else{

        OsdObj *objBmp = obj;

        /*****************************************************
         * loading static osd yuv {Police Num}
         * osd_yuv_buf          -->>  1280p
         * osd_480p_yuv_buf     -->>  720p + 480p
         * 加载动态水印yuv内容
         *****************************************************/
        osd_bmp_480p_flag = 0;

        //ttf convert to bmp
        ret = SDL_TTF_Render(font, (char *)obj->getOsdContentStr().c_str(), (char *)obj->getOsdName().c_str(), &x, &y);
        if(ret)
        {
            LOGE("TTF font rendering to bitmap failed... ret: %d", ret);
            return -2;
        }

        objBmp->setOsdRes(x, y);
        //Read bmp file to memory
        ret = OSD_ReadBmpFile(objBmp, osd_bmp_480p_flag);
        if(ret)
        {
            LOGE("OSD read bmp file failed..., ret: %d", ret);
            return -3;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        osd_bmp_480p_flag = 1;

        //ttf convert to bmp
        ret = SDL_TTF_Render(font_480p, (char *)obj->getOsdContentStr().c_str(), (char *)obj->getOsdName().c_str(), &x_480p, &y_480p);
        if(ret)
        {
            LOGE("TTF font_480p rendering to 480p bitmap failed... ret: %d", ret);
            return -2;
        }

        objBmp->set480pOsdRes(x_480p, y_480p);
        //Read bmp file to memory
        ret = OSD_ReadBmpFile(objBmp, osd_bmp_480p_flag);
        if(ret)
        {
            LOGE("OSD read 480p bmp file failed..., ret: %d", ret);
            return -3;
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //Add obj to obj list
        //v.push_back(obj);

    }
    return 0;
}
