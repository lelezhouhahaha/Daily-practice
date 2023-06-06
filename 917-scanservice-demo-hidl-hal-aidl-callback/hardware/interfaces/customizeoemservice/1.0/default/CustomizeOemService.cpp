// FIXME: your file license if you have one

#include "CustomizeOemService.h"
#include <log/log.h>
#include <string.h>
#include "lib_customized_ssign.h"

namespace vendor::oem::hardware::customizeoemservice::V1_0::implementation {

CustomizedSSIGN* customized_ssign = NULL;
// Methods from ::vendor::oem::hardware::customizeoemservice::V1_0::ICustomizeOemService follow.
CustomizeOemService::CustomizeOemService() {
	ALOGD("%s, start",  __func__);
	customized_ssign = CustomizedSSIGN::create();
    if( customized_ssign == NULL ) {
        ALOGD("%s, can not create customized_odm\n",  __func__ );
        return;
    }
}

CustomizeOemService::~CustomizeOemService(){
	ALOGD("%s, destroy",  __func__);
}
Return<int32_t> CustomizeOemService::customize_SetProvisionStatus(uint32_t enable) {
	ALOGD("%s: enable:[%u]", __func__, enable);
	int32_t ret = 0;
	uint8_t buf[CustomizedSSIGN::PROVISION_SIZE+4];
    //memset(buf, 0, CustomizedSSIGN::PROVISION_SIZE+4);
	buf[CustomizedSSIGN::PROVISION_SIZE+4] = 0;
	
	if(enable == 1){
//		strcpy((char *)buf, "true");
		buf[0] = 1;
		//ret = customized_ssign->camparam_set("true", sizeof("true"));
	}else {
		buf[0] = 0;
		//ret = customized_ssign->camparam_set("false", sizeof("false"));
	}
	if( customized_ssign == NULL){
		customized_ssign = CustomizedSSIGN::create();
		if( customized_ssign == NULL ) {
			ALOGD("%s, can not create customized_odm\n",  __func__ );
			return 0;
		}
	}
	ret = customized_ssign->provision_set(buf, CustomizedSSIGN::PROVISION_SIZE);
    if( ret < 0 ){
        ALOGD("%s, fail to get camparam, err=%d\n",  __func__, ret );
		return -1;
    }
    else {
        ALOGD("%s, \n",  __func__);
		return 0;
    }
}

Return<int32_t> CustomizeOemService::customize_GetProvisionStatus() {
    // TODO implement
	ALOGD("customize_GetProvisionStatus");
	uint8_t buf[CustomizedSSIGN::PROVISION_SIZE+4];
    //memset(buf, 0, CustomizedSSIGN::PROVISION_SIZE+4);
	buf[CustomizedSSIGN::PROVISION_SIZE+4] = 0;
	int ret = 0;
	uint32_t hidl_return = 0;
	if( customized_ssign == NULL){
		customized_ssign = CustomizedSSIGN::create();
		if( customized_ssign == NULL ) {
			ALOGD("%s, can not create customized_odm\n",  __func__ );
			return 0;
		}
	}
    ret = customized_ssign->provision_get(buf, CustomizedSSIGN::PROVISION_SIZE);
    if( ret < 0 ){
        ALOGD("%s, fail to get camparam, err=%d\n",  __func__, ret );
		return -1;
    }
    else {
        ALOGD("%s, buf:[%u]\n",  __func__, buf[0] );
		if(buf[0] == 1){
			hidl_return = 1;
		}
		ALOGD("%s, 2 buf:[%u]\n",  __func__, buf[0] );
		return hidl_return;
    }
	return hidl_return;
    //return uint32_t {};
}
}  // namespace vendor::oem::hardware::customizeoemservice::implementation
