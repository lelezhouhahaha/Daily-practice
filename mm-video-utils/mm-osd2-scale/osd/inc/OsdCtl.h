#ifndef _OSD_CTL_H
#define _OSD_CTL_H
#include <vector>
#include <iostream>
#include "OsdObj.h"
#include "SDL.h"
#include "SDL_ttf.h"
#include "ttf2bmp.h"
#include "bmp2yuv.h"

using namespace std;
typedef unsigned int uint_32;

class OsdCtl
{
private:
	static OsdCtl *sInstance;
	vector<OsdObj *> v;
	TTF_Font *font;
	TTF_Font *font_480p;
	int OSD_ReadYuvFile(OsdObj *objYuv, int osd_yuv_480p_flag);
	int OSD_ReadBmpFile(OsdObj *objBmp, int osd_bmp_480p_flag);
	void OSD_Show_Vector();

public:
	static OsdCtl * OSD_Instance();
	int OSD_Add_Obj(OsdObj *obj);
	int OSD_Update_Obj(OsdObj *obj);
	void OSD_Del_Obj(OsdObj *obj);
	void OSD_Update_Obj_Content(OsdObj *obj);
	void OSD_Start_Composing(char * pYuv420Frame, int iSrcWidth, int iSrcHeight, int video_resolution);
	void OSD_Stop_Composing();
	OsdCtl();
	virtual ~OsdCtl();
};
#endif
