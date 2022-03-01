package com.meigsmart.meigrs32.config;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Xml;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.activity.AudioActivity;
import com.meigsmart.meigrs32.activity.BatteryChargePCBAActivity;
import com.meigsmart.meigrs32.activity.DcJackChargeActivity;
import com.meigsmart.meigrs32.activity.BatterySwitchActivity;
import com.meigsmart.meigrs32.activity.BluetoothActivity;
import com.meigsmart.meigrs32.activity.CalibrationActivity;
import com.meigsmart.meigrs32.activity.ChargerActivity;
import com.meigsmart.meigrs32.activity.CpuActivity;
import com.meigsmart.meigrs32.activity.EComPassActivity;
import com.meigsmart.meigrs32.activity.EarPhoneActivity;
import com.meigsmart.meigrs32.activity.FlashLightActivity;
import com.meigsmart.meigrs32.activity.FmActivity;
import com.meigsmart.meigrs32.activity.GSensorActivity;
import com.meigsmart.meigrs32.activity.GpsActivity;
import com.meigsmart.meigrs32.activity.GyroMeterActivity;
import com.meigsmart.meigrs32.activity.GyroMeterCalibrationActivity;
import com.meigsmart.meigrs32.activity.InformationCheckActivity;
import com.meigsmart.meigrs32.activity.KeyboardActivity;
import com.meigsmart.meigrs32.activity.LCDBrightnessActivity;
import com.meigsmart.meigrs32.activity.LCDRGBActivity;
import com.meigsmart.meigrs32.activity.LCDWaterRippleActivity;
import com.meigsmart.meigrs32.activity.LEDActivity;
import com.meigsmart.meigrs32.activity.LSensorActivity;
import com.meigsmart.meigrs32.activity.LSensorCalibrationActivity;
import com.meigsmart.meigrs32.activity.MemoryEmmcActivity;
import com.meigsmart.meigrs32.activity.MemoryRamActivity;
import com.meigsmart.meigrs32.activity.NFCActivity;
import com.meigsmart.meigrs32.activity.PCBAActivity;
import com.meigsmart.meigrs32.activity.PCBASignalActivity;
import com.meigsmart.meigrs32.activity.PSAMActivity;
import com.meigsmart.meigrs32.activity.PSensorActivity;
import com.meigsmart.meigrs32.activity.PSensorCalibrationActivity;
import com.meigsmart.meigrs32.activity.PreFunctionActivity;
import com.meigsmart.meigrs32.activity.PreFunctionSignalActivity;
import com.meigsmart.meigrs32.activity.PreGSensorActivity;
import com.meigsmart.meigrs32.activity.RearCameraAutoActivity;
import com.meigsmart.meigrs32.activity.FrontCameraAutoActivity;
import com.meigsmart.meigrs32.activity.RecActivity;
import com.meigsmart.meigrs32.activity.ReceiverOrMicActivity;
import com.meigsmart.meigrs32.activity.RecordActivity;
import com.meigsmart.meigrs32.activity.RtcActivity;
import com.meigsmart.meigrs32.activity.RunInActivity;
import com.meigsmart.meigrs32.activity.SIMActivity;
import com.meigsmart.meigrs32.activity.ScanActivity;
import com.meigsmart.meigrs32.activity.SecondPhoneLoopBackTestActivity;
import com.meigsmart.meigrs32.activity.SimCallActivity;
import com.meigsmart.meigrs32.activity.SoftwareVersionActivity;
import com.meigsmart.meigrs32.activity.SpeakerActivity;
import com.meigsmart.meigrs32.activity.StartCustomerActivity;
import com.meigsmart.meigrs32.activity.StorageCardActivity;
import com.meigsmart.meigrs32.activity.ThreeDActivity;
import com.meigsmart.meigrs32.activity.UsbOtgActivity;
import com.meigsmart.meigrs32.activity.VibratorActivity;
import com.meigsmart.meigrs32.activity.VideoActivity;
import com.meigsmart.meigrs32.activity.WifiActivity;
import com.meigsmart.meigrs32.activity.SecondaryBatteryTest;
import com.meigsmart.meigrs32.activity.TouchTestActivity;
import com.meigsmart.meigrs32.activity.MutiTouchTestActivity;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chenMeng on 2018/4/24.
 */
public class Const {
    public static boolean isCanBackKey = true;
    public static final String RESULT_SUCCESS = "Success";
    public static final String RESULT_FAILURE = "Failure";
    public static final String RESULT_NOTEST = "NOTEST";
    public static final String RESULT_UNKNOWN = "unknown";
    public static final String RESULT_TIMEOUT = "TIMEOUT";

    public static final String KEYTEST_CONFIG_XML_PATH = "/system/etc/meig/cit_keytest_config.xml";
    public static final String SIXPIN_CONFIG_XML_PATH = "/system/etc/meig/cit_keytest_config_one.xml";
    public static final String TENPIN_CONFIG_XML_PATH = "/system/etc/meig/cit_keytest_config_two.xml";
    public static final String KEYTEST_DEPUTY_CONFIG_XML_PATH = "/system/etc/meig/cit_deputy_keytest_config.xml";
    public static final String LEDTEST_CONFIG_XML_PATH = "/system/etc/meig/cit_ledtest_config.xml";
    public static final String CIT_CONFIG_PATH_DEFAULT = "/system/etc/meig/cit_config.xml";
	public static final String CIT_CONFIG_PATH_NOPMI =  "/system/etc/meig/cit_config_nopmi.xml";
    public static final String CIT_NODE_CONFIG_PATH = "/system/etc/meig/cit_node_config.xml";
    public static final String CIT_COMMON_CONFIG_PATH = "/system/etc/meig/cit_common_config.xml";
    public static final String CIT_CUSTOMER_CONFIG_PATH = "/system/etc/meig/cit_customer_config.xml";
    public static final String SENSORTEST_CONFIG_XML_PATH = "/system/etc/meig/cit_sensor_config.xml";
    public static final String PCBA_AUTO_TEST_CONFIG_XML_PATH = "/system/etc/meig/cit_pcba_auto_test_config.xml";
    public static final int CONFIG_FUNCTION = 0;
    public static final int CONFIG_PCBA = 1;
    public static final int CONFIG_PRE_FUNCTION = 2;
    public static final int CONFIG_RUNIN = 3;
    public static final int CONFIG_PCBA_SIGNAL = 4;
    public static final int CONFIG_PRE_FUNCTION_SIGNAL = 5;
    public static final int CONFIG_INFORMATION_CHECK = 6;
    public static final int CONFIG_CALIBRATION = 7;
    public static final int CONFIG_POWER_BOARD = 8;
    public static final int CONFIG_POWER_BOARD_SINGLE = 9;
    public static final int CONFIG_SMT_KEYPAD = 10;
    public static final int CONFIG_SMT_KEYPAD_SINGLE = 11;
    public static final int CONFIG_KEYPAD_POWER_BOARD = 12;
    public static final int CONFIG_SCAN_SINGLE = 13;
    public static final int CONFIG_LAMP_PLATE = 14;
    public static final int CONFIG_LAMP_PLATE_SIGNAL = 15;
    public static final int CONFIG_SUNMI_SIM_SIGNAL = 16;
    public static final int CONFIG_SUNMI_3D_STRUCTURED_SIGNAL = 17;
    public static final int CONFIG_MMI1_PRE = 18;
    public static final int CONFIG_MMI1_PRE_SIGNAL = 19;
    public static final int CONFIG_MMI2_PRE = 20;
    public static final int CONFIG_MMI2_PRE_SIGNAL = 21;
    private static final String TAG_CONFIG_FUNCTION = "fcuntion_config";
    private static final String TAG_CONFIG_PCBA = "pcba_config";
    private static final String TAG_CONFIG_PCBA_SIGNAL = "pcba_signal_config";
    private static final String TAG_CONFIG_FUNCTION_SIGNAL = "pre_function_signal_config";
    private static final String TAG_CONFIG_PRE_FUNCTION = "pre_function_config";
    private static final String TAG_CONFIG_RUNIN = "runin_config";
    private static final String TAG_CONFIG_CALIBRATION= "calibration_config";
    private static final String TAG_CONFIG_INFORMATION = "information_config";
    private static final String TAG_CONFIG_POWER_BOARD = "power_board_config";
    private static final String TAG_CONFIG_SMT_KEYPAD = "smt_keypad_config";
    private static final String TAG_CONFIG_POWER_BOARD_SINGLE = "power_board_single_config";
    private static final String TAG_CONFIG_SMT_KEYPAD_SINGLE = "smt_keypad_single_config";
    private static final String TAG_CONFIG_KEYPAD_POWER_BOARD = "keypad_power_board_config";
    private static final String TAG_CONFIG_SCAN_SINGLE = "scan_single_config";
    private static final String TAG_CONFIG_LAMP_PLATE = "lamp_plate_config";
    private static final String TAG_CONFIG_LAMP_PLATE_SIGNAL = "lamp_plate_signal_config";
    private static final String TAG_CONFIG_SUNMI_SIM_SIGNAL = "sunmi_sim_config";
    private static final String TAG_CONFIG_SUNMI_3D_STRUCTURED_SIGNAL = "sunmi_3d_structured_config";
    private static final String TAG_CONFIG_MMI1_PRE_SIGNAL = "mmi1_pre_signal_config";
    private static final String TAG_CONFIG_MMI1_PRE = "mmi1_pre_config";
    private static final String TAG_CONFIG_MMI2_PRE_SIGNAL = "mmi2_pre_signal_config";
    private static final String TAG_CONFIG_MMI2_PRE = "mmi2_pre_config";
    public static final int DELAY_TIME = 1000;

    public static final String PERSIST_PATH = "/persist";
    public static final String AUTO_TEST_RESULT_DIR = "/persist/CITResults";
    public static final String PCBA_AUTO_RESULT_FILE = "pcba_auto_result.json";
    public static final String PCBA_AUTO_RESULT_CIT2_FILE = "pcba_auto_result_cit2.json";
    public static final String PRE_FUNCTION_AUTO_RESULT_FILE = "pre_function_auto_result.json";
    public static final String RUN_IN_AUTO_RESULT_FILE = "run_in_auto_result.json";
    public static final String POWER_BOARD_AUTO_RESULT_FILE = "power_board_auto_result.json";
    public static final String SMT_KEYPAD_AUTO_RESULT_FILE = "smt_keypad_auto_result.json";
    public static final String KEYPAD_POWER_BOARD_AUTO_RESULT_FILE = "keypad_power_board_auto_result.json";
    public static final String LAMP_PLATE_AUTO_RESULT_FILE = "lamp_plate_auto_result.json";
    public static final String SCAN_SINGLE_RESULT_FILE = "scan_result.json";
    public static final String LOG_PATH_DIR = "common_cit_log_save_path_dir";
    public static final String LOG_PATH_DIR_AT = "common_cit_log_save_path_dir_at";
    public static final String TEST_RESULT_DIR = "common_cit_log_save_path_file";
    public static final String TEST_RIL_STATE = "common_4g_status_path";
    public static final String MMI1_FUNCTION_AUTO_RESULT_FILE = "mmi1_function_auto_result.json";
    public static final String MMI2_FUNCTION_AUTO_RESULT_FILE = "mmi2_function_auto_result.json";

    public static final int TYPE_LOG_PATH_DIR = 0;
    public static final int TYPE_LOG_PATH_FILE = 1;

    public  static final int REQUEST_CODE_FOR_COMMON = 1000;
    public  static final int REQUEST_CODE_FOR_SCAN = 1000;

    /*jicong.wang modify for task 9812 start {@*/
    public static final String CIT_CONFIG_HW010_PATH_DEFAULT = "/system/etc/meig/cit_config_hw010.xml";
    public static final String CIT_CONFIG_PATH =
            //index 0
            CustomConfig.isSLB761X()?
            (CustomConfig.readFromFile(CustomConfig.HW_PATH).contains("010")?CIT_CONFIG_HW010_PATH_DEFAULT
            :CIT_CONFIG_PATH_DEFAULT)
            //default
            :(SystemProperties.get("ro.boot.pmi_mode", "").equals("nopmi") ? CIT_CONFIG_PATH_NOPMI
            : CIT_CONFIG_PATH_DEFAULT);
    /*jicong.wang modify for task 9812 end @}*/

    public static Class[] functionList = {
            PCBASignalActivity.class,
            PCBAActivity.class,
            RunInActivity.class,
            PreFunctionActivity.class,
            PreFunctionSignalActivity.class,
            Class.class,
            InformationCheckActivity.class,
            CalibrationActivity.class,
            Class.class,
            Class.class
    };

    /*public static Class[] commonList = {
            SoftwareVersionActivity.class, 			//软件版本测试
            RtcActivity.class,						//Rtc测试
            GpsActivity.class,						//Gps测试
            BluetoothActivity.class,				//蓝牙测试
            WifiActivity.class,						//wifi测试
            SIMActivity.class,						//Sim卡测试
            SimCallActivity.class,					//SIM卡打电话测试
            PSAMActivity.class,						//PSAM卡测试
            StorageCardActivity.class,				//T卡测试
            GSensorActivity.class,					//重力传感器测试
            PreGSensorActivity.class,				//重力传感器校准测试
            EComPassActivity.class,					//指南针传感器测试
            GyroMeterActivity.class,				//陀螺仪传感器测试
            LSensorActivity.class,					//光感传感器测试
            LSensorCalibrationActivity.class,		//光感传感器校准测试
            PSensorActivity.class,					//近距离传感器测试
            PSensorCalibrationActivity.class,		//近距离传感器校准测试
            BatteryChargePCBAActivity.class,		//主电池充电测试
            DcJackChargeActivity.class,				//底座充电测试
            SecondaryBatteryTest.class,				//副电池测试
            BatterySwitchActivity.class,			//电池切换测试
            FrontCameraAutoActivity.class,			//前置摄像头测试
            RearCameraAutoActivity.class,			//后置摄像头测试
            FlashLightActivity.class,				//相机闪光灯测试
            RecordActivity.class,					//主MIC测试
            SecondPhoneLoopBackTestActivity.class,	//副MIC测试
            EarPhoneActivity.class,					//耳机测试测试
            SpeakerActivity.class,					//喇叭测试
            RecActivity.class,						//听筒测试
            FmActivity.class,						//收音机测试
            KeyboardActivity.class,					//按键测试
            LCDBrightnessActivity.class,			//LCD背光测试
            LCDRGBActivity.class,					//LCD屏测试
            LCDWaterRippleActivity.class,			//水波纹测试
            TouchTestActivity.class,				//触屏测试
            MutiTouchTestActivity.class,			//多触点测试
            LEDActivity.class,						//LED灯测试
            VibratorActivity.class,					//震动马达测试
            ScanActivity.class,						//扫码测试
            NFCActivity.class,						//NFC测试
            UsbOtgActivity.class,					//usb otg测试
            VideoActivity.class,					//视频播放测试
            AudioActivity.class,					//音频播放测试
            CpuActivity.class,						//cpu测试
            MemoryRamActivity.class,				//RAM测试
            MemoryEmmcActivity.class,				//ROM测试
            ThreeDActivity.class,					//3D效果测试
            StartCustomerActivity.class,            //客户自定义工具
    };*/

    /*public static int[] getConfig(Context context,int type){
        int[] enableConfig ;
        Class[] classArray ;
        switch (type){
            case CONFIG_FUNCTION:
                classArray = functionList;
                break;
            default:
                classArray = commonList;
                break;
        }
        enableConfig = new int[classArray.length];
        for (int i=0;i<classArray.length;i++)
            enableConfig[i] = 1;

        List<String> tempConfig = Const.getXmlConfig(context,type);
        if(tempConfig!=null&&tempConfig.size()>0){
            for (int i=0;i<classArray.length;i++){
                int index = tempConfig.indexOf(classArray[i].getSimpleName());
                if(index>=0){
                    enableConfig[i] = 1;
                }else {
                    enableConfig[i] = 0;
                }
            }
            return enableConfig;
        }else {
            for (int i=0;i<classArray.length;i++)
                enableConfig[i] = 1;
            return  enableConfig;
        }
    }*/


    public static  List<String> getXmlConfig(Context context, int configFunction) {
        List<String> config = new ArrayList<String>();
        File configXml=null;
        configXml = new File(Const.CIT_CONFIG_PATH);
        if(!configXml.exists()){
            LogUtil.e("xcode","====>configXml "+Const.CIT_CONFIG_PATH +"not found");
            return null;
        }

        String readTag = "";
        String readValues = "";
        switch (configFunction) {
            case CONFIG_FUNCTION:
                readTag = TAG_CONFIG_FUNCTION;
                break;
            case CONFIG_PCBA:
                readTag = TAG_CONFIG_PCBA;
                break;
            case CONFIG_PRE_FUNCTION:
                readTag = TAG_CONFIG_PRE_FUNCTION;
                break;
            case CONFIG_PCBA_SIGNAL:
                readTag = TAG_CONFIG_PCBA_SIGNAL;
                break;
            case CONFIG_PRE_FUNCTION_SIGNAL:
                readTag = TAG_CONFIG_FUNCTION_SIGNAL;
                break;
			case CONFIG_RUNIN:
                readTag = TAG_CONFIG_RUNIN;
                break;
            case CONFIG_CALIBRATION:
                readTag = TAG_CONFIG_CALIBRATION;
                break;
            case CONFIG_INFORMATION_CHECK:
                readTag = TAG_CONFIG_INFORMATION;
                break;
            case CONFIG_POWER_BOARD:
                readTag = TAG_CONFIG_POWER_BOARD;
                break;
            case CONFIG_SMT_KEYPAD:
                readTag = TAG_CONFIG_SMT_KEYPAD;
                break;
            case CONFIG_POWER_BOARD_SINGLE:
                readTag = TAG_CONFIG_POWER_BOARD_SINGLE;
                break;
            case CONFIG_SMT_KEYPAD_SINGLE:
                readTag = TAG_CONFIG_SMT_KEYPAD_SINGLE;
                break;
            case CONFIG_KEYPAD_POWER_BOARD:
                readTag = TAG_CONFIG_KEYPAD_POWER_BOARD;
                break;
            case CONFIG_SCAN_SINGLE:
                readTag = TAG_CONFIG_SCAN_SINGLE;
                break;
            case CONFIG_LAMP_PLATE:
                readTag = TAG_CONFIG_LAMP_PLATE;
                break;
            case CONFIG_LAMP_PLATE_SIGNAL:
                readTag = TAG_CONFIG_LAMP_PLATE_SIGNAL;
                break;
            case CONFIG_SUNMI_SIM_SIGNAL:
                readTag = TAG_CONFIG_SUNMI_SIM_SIGNAL;
                break;
            case CONFIG_SUNMI_3D_STRUCTURED_SIGNAL:
                readTag = TAG_CONFIG_SUNMI_3D_STRUCTURED_SIGNAL;
                break;
            case CONFIG_MMI1_PRE_SIGNAL:
                readTag = TAG_CONFIG_MMI1_PRE_SIGNAL;
                break;
            case CONFIG_MMI1_PRE:
                readTag = TAG_CONFIG_MMI1_PRE;
                break;
            case CONFIG_MMI2_PRE_SIGNAL:
                readTag = TAG_CONFIG_MMI2_PRE_SIGNAL;
                break;
            case CONFIG_MMI2_PRE:
                readTag = TAG_CONFIG_MMI2_PRE;
                break;
            default:
                return null;
        }
        InputStream inputStream=null;
        try {
            inputStream = new FileInputStream(Const.CIT_CONFIG_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if(readTag.equals(startTagName)){
                            readValues = xmlPullParser.nextText();
                            if(readValues==null){
                                return null;
                            }else {
                                readValues = readValues.replace("\r","");
                                readValues = readValues.replace("\n","");
                                readValues = readValues.replace("\t","");
                                readValues = readValues.replace(" ","");
                                LogUtil.e("====>readValues = "+readValues);
                                config = Arrays.asList(readValues.split(","));
                                LogUtil.e("=====>array value is " + config.toString());
                                return config;
                            }
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        }  catch (Exception e) {
            LogUtil.e(context.getClass().getName(), "====>error = " + e.toString());
            e.printStackTrace();
        }
        return null;
    }
    public static String getLogPath(int config){
        String path = "";
        switch (config){
            case Const.TYPE_LOG_PATH_DIR:
                path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.LOG_PATH_DIR);
                if(path==null || path.isEmpty()){
                    path = Const.PERSIST_PATH;
                }
                break;
            case Const.TYPE_LOG_PATH_FILE:
                path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.TEST_RESULT_DIR);
                if(path==null || path.isEmpty()){
                    path = Const.AUTO_TEST_RESULT_DIR;
                }
                break;
            }
        return path;

    }

}
