ifndef PLATFORM_ENV
##########value can be (TEE REE ANDROID8_1 ANDROID8_0 ANDROIDO_FW ANDROID_P ANDROID_Q)##########
#TEE          android5.0、android6.0与android7.0的指纹环境 
#ANDROID8_1   android8.1的hidl接口
#ANDROID8_0   android8.0的hidl接口
#ANDROIDO_FW  androidO的移植fingerprintmanager接口
#ANDROID_P    android9的hidl接口
#ANDROID_Q    android10的hidl接口
############################################################################
PLATFORM_ENV := ANDROID_Q
##########################################
endif
include $(call all-subdir-makefiles)
