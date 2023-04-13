#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "OsdLog.h"

void time_OSD_overlap_caption(char *src_yuv, int src_w, int src_h,//源图及宽高
						 char *osd_yuv, int osd_w, int osd_h,//OSD图及宽高
                         int off_x, int off_y)//要叠加的字符串的位置及宽高
{
	int i, j, n_off;
	int size_src = src_w * src_h;
    int size_osd = osd_w * osd_h;
    char *psrc = src_yuv;
    char *posd = osd_yuv;

	if(!src_yuv || !osd_yuv)
		return;

	if(off_x + osd_w > src_w)
	{
		off_x = src_w - osd_w;
	}
	if(off_y + osd_h > src_h)
	{
		off_y = src_h - osd_h;
	}

    for (i = 0; i < osd_h; i++){
		n_off = src_w * (off_y + i) + off_x;
		for(j=0; j < osd_w; j++){
			//LOGD("*(posd + osd_w * i + j):%d\n", *(posd + osd_w * i + j));
			if( (*(posd + osd_w * i + j) != 76) ){
				//LOGE("zhou: i = %d, j = %d", i, j);
				//LOGE("zhou: %d", *(posd + osd_w * i + j));

				if (*(posd + osd_w * i + j) > 215 )
					*(psrc + n_off + j) = 255;
				if (*(posd + osd_w * i + j) < 200 )
					*(psrc + n_off + j) = 20;
				//*(psrc + n_off + j) = *(posd + osd_w * i + j);

			}
		}
	}
}

void _OSD_overlap_caption(char *src_yuv, int src_w, int src_h,//源图及宽高
						 char *osd_yuv, int osd_w, int osd_h,//OSD图及宽高
                         int off_x, int off_y)//要叠加的字符串的位置及宽高
{
	int i, j, n_off;
	int size_src = src_w * src_h;
    int size_osd = osd_w * osd_h;
    char *psrc = src_yuv;
    char *posd = osd_yuv;

	if(!src_yuv || !osd_yuv)
		return;

	if(off_x + osd_w > src_w)
	{
		off_x = src_w - osd_w;
	}
	if(off_y + osd_h > src_h)
	{
		off_y = src_h - osd_h;
	}

    for (i = 0; i < osd_h; i++){
		n_off = src_w * (off_y + i) + off_x;
		for(j=0; j < osd_w; j++){
			//LOGD("*(posd + osd_w * i + j):%d\n", *(posd + osd_w * i + j));
			if( (*(posd + osd_w * i + j) != 56) && (*(posd + osd_w * i + j) != 16) ){

				*(psrc + n_off + j) = *(posd + osd_w * i + j);
				if( j > 0 ) {
				   if(*(posd + osd_w * i + j-1) == 0x38)
					  *(psrc + n_off + j-1)=0x10;
				   if(*(posd + osd_w * i + j+1) == 0x38)
					  *(psrc + n_off + j+1)=0x10;
				   /*
				   if(*(posd + osd_w * (i-1) + j-1) == 0x38)
					  *(psrc + src_w * (off_y + i-1) + j + off_x - 1)=0x10;
				   if(*(posd + osd_w * (i+1) + j+1) == 0x38)
					  *(psrc + src_w * (off_y + i+1) + j + off_x + 1)=0x10;
				   if(*(posd + osd_w * (i+1) + j-1) == 0x38)
					  *(psrc + src_w * (off_y + i+1) + j + off_x - 1)=0x10;
				   if(*(posd + osd_w * (i-1) + j+1) == 0x38)
					  *(psrc + src_w * (off_y + i-1)  + j + off_x + 1)=0x10;
				   */
				}
				if( i > 0 ) {
				   if(*(posd + osd_w * (i-1) + j) == 0x38)
				      *(psrc + src_w * (off_y + i-1) + off_x + j)=0x10;
				   if(*(posd + osd_w * (i+1) + j) == 0x38)
					  *(psrc + src_w * (off_y + i+1) + off_x + j)=0x10;
				}
				//LOGE("zhou: i = %d, j = %d", i, j);
				//LOGE("zhou: %d", *(posd + osd_w * i + j));
			}
		}
	}
}

int OSD_overlap_time_caption(char *src_yuv, int src_w, int src_h,
						 char *osd_yuv, int osd_w, int osd_h,
                         int off_x, int off_y, const char *p_caption)
{
	int i = 0;
	int offset = 0;
	int len = strlen(p_caption);

	if(!src_yuv || !osd_yuv || !p_caption || osd_w > src_w || osd_h > src_h)
		return -1;

	//off_x = src_w - osd_w * len;
	//off_y = src_h - osd_h / 14 - 50;

	//case time string
	osd_h /= 19;//time bmp have 14 index chars

	for(i = 0; i < len; i++){
		if(*(p_caption+i) == 45){//'-'
			offset = 11;
		}else if(*(p_caption+i) == 47){//'/'
			offset = 12;
		}else if(*(p_caption+i) == 58){//':'
			offset = 10;
		}else if(*(p_caption+i) == 32){// ' '
			offset = 13;
		}else if(*(p_caption+i) == 69){// 'E'
			offset = 14;
		}else if(*(p_caption+i) == 78){// 'N'
			offset = 15;
		}else if(*(p_caption+i) == 87){// 'W'
			offset = 16;
		}else if(*(p_caption+i) == 83){// 'S'
			offset = 17;
		}else if(*(p_caption+i) == 46){// '.'
			offset = 18;
		} else {
			offset = *(p_caption+i) - 48;//0-9
		}
		//16 * 32 = 512 512 * 14 = 7168 7168 * 3 / 2 = 10752
		//8 * 16 = 128 128 * 14 = 1792 1792 * 3 / 2 = 2688
		/*_OSD_overlap_caption(src_yuv, src_w, src_h, osd_yuv + 512 * offset, 16, 32, off_x + 16*i, off_y);*/

        if((osd_w*osd_h*offset+osd_w*(osd_h-1)+osd_w-1) > osd_w*osd_h*19*3/2)
        {
            LOGE("meig0531 osd_yuv %p offset %d X*Y (%d,%d)",osd_yuv,offset,osd_w*osd_h*offset+osd_w*(osd_h-1)+osd_w-1,osd_w*osd_h*19*3/2);
            return 0;
        }
		time_OSD_overlap_caption(src_yuv, src_w, src_h, osd_yuv + osd_w * osd_h * offset, osd_w, osd_h, off_x + osd_w * i, off_y);
	}

	return 0;
}
