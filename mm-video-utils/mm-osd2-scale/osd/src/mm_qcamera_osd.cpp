#include <iostream>
#include <vector>
#include <string>
#include "mm_qcamera_osd.h"
#include "OsdObj.h"
#include "OsdCtl.h"
#include "OsdLog.h"
#include "CLock.cc"

using namespace std;
typedef unsigned int uint_32;

OsdObj obj1(0, 0, 32, 0,  1, "device_id", "111111111 222222222 333333333 444444444 555555555 ", false);
OsdObj obj2(0, 0, 32, 0,  2, "police_id_and_name", "111111111 222222222 333333333 444444444 555555555 ", false);
OsdObj obj3(0, 0, 32, 0,  3, "station_id", "111111111 222222222 333333333 444444444 555555555 ", false);
OsdObj obj4(0, 50, 32, 1,  4, "time", "2010-12-34 56:78:90", false);
OsdObj obj5(0, 100, 32, 1,  5, "longitude_latitude", "E:000.00000 N:000.00000", false);
//OsdCtl *ctl;  //OSD控制器指针
OsdObj *osdO1; //OSD对象指针
OsdObj *osdO2;
OsdObj *osdO3;
OsdObj *osdO4;
OsdObj *osdO5;
static CLock osd_lock;

static void NV212I420(char* yuv420sp, char* yuv420, int width, int height) {
    int i = 0, j = 0;
    int framesize = width * height;

    if (yuv420sp == NULL || yuv420 == NULL)
        return;
    memcpy(yuv420, yuv420sp, framesize);
    i = 0;
    for (j = 0; j < framesize / 2; j += 2) {
        yuv420[i + framesize * 5 / 4] = yuv420sp[j + framesize];
        i++;
    }
    i = 0;
    for (j = 1; j < framesize / 2; j += 2) {
        yuv420[i + framesize] = yuv420sp[j + framesize];
        i++;
    }
}


static void I4202NV21(char* yuv420sp, char* yuv420, int width, int height) {
    int i = 0, j = 0;
    int framesize = width * height;

    if (yuv420sp == NULL || yuv420 == NULL)
        return;
    memcpy(yuv420sp, yuv420, framesize);
    i = 0;
    for (j = 0; j < framesize / 2; j += 2) {
        yuv420sp[j + framesize] = yuv420[i + framesize * 5 / 4];
        i++;
    }
    i = 0;
    for (j = 1; j < framesize / 2; j += 2) {
        yuv420sp[j + framesize] = yuv420[i + framesize];
        i++;
    }
}


static void NV122I420(char* yuv420sp, char* yuv420, int width, int height) {
    int i = 0, j = 0;
    int framesize = width * height;

    if (yuv420sp == NULL || yuv420 == NULL)
        return;
    memcpy(yuv420, yuv420sp, framesize);
    i = 0;
    for (j = 1; j < framesize / 2; j += 2) {
        yuv420[i + framesize * 5 / 4] = yuv420sp[j + framesize];
        i++;
    }
    i = 0;
    for (j = 0; j < framesize / 2; j += 2) {
        yuv420[i + framesize] = yuv420sp[j + framesize];
        i++;
    }
}


static void I4202NV12(char* yuv420sp, char* yuv420, int width, int height) {
    int i = 0, j = 0;
    int framesize = width * height;

    if (yuv420sp == NULL || yuv420 == NULL)
        return;
    memcpy(yuv420sp, yuv420, framesize);
    i = 0;
    for (j = 1; j < framesize / 2; j += 2) {
        yuv420sp[j + framesize] = yuv420[i + framesize * 5 / 4];
        i++;
    }
    i = 0;
    for (j = 0; j < framesize / 2; j += 2) {
        yuv420sp[j + framesize] = yuv420[i + framesize];
        i++;
    }
}


void* osd_init(char* device_id, char* police_id_and_name, char* station_id, char *gps_info) {

    //1. Got OsdCtl Instance and Init
    OsdCtl *ctl;
    if(!(ctl = OsdCtl::OSD_Instance())){
        cout << "Got OsdCtl instance NULL..." << endl;
        return NULL;
    }
    //2. Add first OsdObj
    osdO1 = &obj1;
    osdO1->setOsdContentStr(device_id);
    ctl->OSD_Update_Obj_Content(osdO1);
    ctl->OSD_Add_Obj(&obj1);

    //3. Add second OsdObj
    osdO2 = &obj2;
	osdO2->setOsdContentStr(police_id_and_name);
	ctl->OSD_Update_Obj_Content(osdO2);
    ctl->OSD_Add_Obj(&obj2);

    osdO3 = &obj3;
	osdO3->setOsdContentStr(station_id);
	ctl->OSD_Update_Obj_Content(osdO3);
    ctl->OSD_Add_Obj(&obj3);

    osdO4 = &obj4;
    ctl->OSD_Add_Obj(&obj4);

    //4. Add third OsdObj
    osdO5 = &obj5;
    osdO5->setOsdContentStr(gps_info);
    ctl->OSD_Update_Obj_Content(osdO5);
    ctl->OSD_Add_Obj(&obj5);

    return (void *)ctl;
}

int osd_start(void *osd_ctl, void *pFrameBuffer, int image_width, int image_height, int video_resolution)
{
    //Creat local Osd ctl
    CLock::Auto lock( osd_lock );
    OsdCtl *ctl = (OsdCtl *)osd_ctl;

    /*********************************************************************
     * update time str
     * mytime == system time
     *********************************************************************/
    struct timeval tv;
    char mytime[20] = "";
    gettimeofday(&tv, NULL);
    strftime(mytime, sizeof(mytime), "%Y-%m-%d %T", localtime(&tv.tv_sec));
    osdO4->setOsdContentStr(mytime);
    ctl->OSD_Update_Obj_Content(osdO4);

    //OSD_Start_Composing
	ctl->OSD_Start_Composing((char *)pFrameBuffer, image_width, image_height, video_resolution);

    return 0;
}

int osd_start_upload(void *osd_ctl, void *pFrameBuffer, int image_width, int image_height, int upload_video_resolution)
{
    //Creat local Osd ctl
    CLock::Auto lock( osd_lock );
    OsdCtl *ctl = (OsdCtl *)osd_ctl;

    /*********************************************************************
     * update time str
     * mytime == system time
     *********************************************************************/
    struct timeval tv;
    char mytime[20] = "";
    gettimeofday(&tv, NULL);
    strftime(mytime, sizeof(mytime), "%Y-%m-%d %T", localtime(&tv.tv_sec));
    osdO4->setOsdContentStr(mytime);
    ctl->OSD_Update_Obj_Content(osdO4);

    //OSD_Start_Composing
	ctl->OSD_Start_Composing((char *)pFrameBuffer, image_width, image_height, upload_video_resolution);


    return 0;
}

int update_gps_osd(void *osd_ctl, char *gps_info)
{
    //Creat local Osd ctl
    OsdCtl *ctl = (OsdCtl *)osd_ctl;

    /*********************************************************************
     * update gps info str
     * gps_info == updated gps information
     *********************************************************************/
    CLock::Auto lock( osd_lock );
    osdO5->setOsdContentStr(gps_info);
    ctl->OSD_Update_Obj_Content(osdO5);

    return 0;
}

int update_base_info_osd(void *osd_ctl, char* device_id, char* police_id_and_name, char* station_id)
{
    //Creat local Osd ctl
    OsdCtl *ctl = (OsdCtl *)osd_ctl;

	osdO1->setOsdContentStr(device_id);
	ctl->OSD_Update_Obj_Content(osdO1);
	ctl->OSD_Update_Obj(osdO1);

	osdO2->setOsdContentStr(police_id_and_name);
	ctl->OSD_Update_Obj_Content(osdO2);
	ctl->OSD_Update_Obj(osdO2);

	osdO3->setOsdContentStr(station_id);
	ctl->OSD_Update_Obj_Content(osdO3);
	ctl->OSD_Update_Obj(osdO3);

    return 0;
}

void osd_deinit(void *osd_ctl) {
    OsdCtl *ctl = (OsdCtl *)osd_ctl;
    ctl->OSD_Stop_Composing();
    return;
}
