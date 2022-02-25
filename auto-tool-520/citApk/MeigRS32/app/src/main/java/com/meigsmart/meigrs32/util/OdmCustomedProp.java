package com.meigsmart.meigrs32.util;

import android.app.Application;

import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.util.DataUtil;

public class OdmCustomedProp {
    private static String gCitPCBAResultProp;
    private static String gCitCit1ResultProp;
    private static String gCitCit2ResultProp;
    private static String gCitRuninResultProp;
    private static String gCitNfcVersionProp;
    private static String gCitFingerprintVersionProp;
    private static String gCitBatteryVersionProp;
    private static String gCitTpVersionProp;
    private static String gCitPsamVersionProp;
    private static String gCitScanVersionProp;
    private static String gCitCameraTypeProp;
    private static String gCitSunmifirmwareVersionProp;
    private static String gCitSp1902Prop;
    private static String gCitFaceSelectProp;
    private static String gCitSnProp;
    private static String gCitModelNameProp;
    private static String gCitPressureNumberProp;
    private static String gCitSoftwareVersionProp;

    public static void init(){
        String mProjectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        boolean isMC520 = "MC520_GMS".equals(mProjectName);
        if( isMC520 ){
            gCitPCBAResultProp = "persist.vendor.custmized.pcba_result";
            gCitCit1ResultProp = "persist.vendor.custmized.cit1_result";
            gCitCit2ResultProp = "persist.vendor.custmized.cit2_result";
            gCitRuninResultProp = "persist.vendor.custmized.runin_result";
            gCitNfcVersionProp = "persist.vendor.custmized.nfc_version";
            gCitFingerprintVersionProp = "persist.vendor.custmized.fingerprint_version";
            gCitBatteryVersionProp = "persist.vendor.custmized.battery_version";
            gCitTpVersionProp = "persist.vendor.custmized.tp_version";
            gCitPsamVersionProp = "persist.vendor.custmized.psam_version";
            gCitScanVersionProp = "persist.vendor.custmized.scan_version";
            gCitCameraTypeProp = "persist.vendor.custmized.camera_type";
            gCitSnProp = "persist.vendor.custmized.sn";
            gCitModelNameProp = "persist.vendor.custmized.model_name";
            gCitPressureNumberProp = "persist.vendor.custmized.pressurenumber";
        }else {
            gCitPCBAResultProp = "persist.custmized.pcba_result";
            gCitCit1ResultProp = "persist.custmized.cit1_result";
            gCitCit2ResultProp = "persist.custmized.cit2_result";
            gCitRuninResultProp = "persist.custmized.runin_result";
            gCitNfcVersionProp = "persist.custmized.nfc_version";
            gCitFingerprintVersionProp = "persist.custmized.fingerprint_version";
            gCitBatteryVersionProp = "persist.custmized.battery_version";
            gCitTpVersionProp = "persist.custmized.tp_version";
            gCitPsamVersionProp = "persist.custmized.psam_version";
            gCitScanVersionProp = "persist.custmized.scan_version";
            gCitCameraTypeProp = "persist.custmized.camera_type";
            gCitSnProp = "persist.custmized.sn";
            gCitModelNameProp = "persist.custmized.model_name";
            gCitPressureNumberProp = "persist.custmized.pressurenumber";
            gCitSunmifirmwareVersionProp = "persist.custmized.sunmifirmware_version";
            gCitSp1902Prop = "persist.custmized.sp_1902";
            gCitFaceSelectProp = "persist.custmized.face_select";
            gCitSoftwareVersionProp = "persist.custmized.softwareversion";
        }

    }

    public static String getPCBAResultProp(){
        return gCitPCBAResultProp;
    }

    public static String getCit1ResultProp(){
        return gCitCit1ResultProp;
    }

    public static  String getCit2ResultProp(){
        return gCitCit2ResultProp;
    }

    public static String getRuninResultProp(){
        return gCitRuninResultProp;
    }

    public static String getNfcVersionProp(){
        return gCitNfcVersionProp;
    }

    public static String getFingerprintVersionProp(){
        return gCitFingerprintVersionProp;
    }

    public static String getBatteryVersionProp(){
        return gCitBatteryVersionProp;
    }

    public static String getTpVersionProp(){
        return gCitTpVersionProp;
    }

    public static String getPsamVersionProp(){
        return gCitPsamVersionProp;
    }

    public static String getScanVersionProp(){
        return gCitScanVersionProp;
    }

    public static String getCameraTypeProp(){
        return gCitCameraTypeProp;
    }

    public static String getSunmifirmwareVersionProp(){
        return gCitSunmifirmwareVersionProp;
    }

    public static String getSp1902Prop(){
        return gCitSp1902Prop;
    }

    public static String getFaceSelectProp(){
        return gCitFaceSelectProp;
    }

    public static String getSnProp(){
        return gCitSnProp;
    }

    public static String getModelNameProp(){
        return gCitModelNameProp;
    }

    public static String getPressureNumberProp(){
        return gCitPressureNumberProp;
    }


    public static String getSoftwareVersionProp(){
        return gCitSoftwareVersionProp;
    }
}
