package com.meigsmart.meigrs32.config;

import android.util.Log;

import com.meigsmart.meigrs32.util.DataUtil;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CustomConfig {
    private static final String TAG = "CustomConfig";
    private static String TAG_CUSTOM = "common_device_name_test";
    public static String HW_PATH = "sys/devices/platform/soc/soc:meig-hwversion/hwversion";
    public static String TEST_DIALOG = "common_test_dialog_message";
    public static String SUNMI_PAY_SDK_ENABLE = "common_sunmi_paysdk_enable";
    public static String SUNMI_PROJECT_MARK = "common_project_name";

    public static boolean isSLB761X(){
        boolean result = false;
        try {
            result = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_CUSTOM).equals("slb761x");
        } catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static List<String> SLB761XtestItemConfig(List<String> config) {
        String hw_version = readFromFile(HW_PATH);
        List<String> new_config = new ArrayList<>();
        Log.d(TAG,"cit gpio hw_version  = "+hw_version);
        if (hw_version.contains("101")){
            Log.d(TAG,"cit gpio current is 101");
            for (int i=0;i<config.size();i++){
                if (!config.get(i).equals("EarPhoneActivity")){
                    new_config.add(config.get(i));
                }
            }
            Log.d(TAG,"new list value = "+new_config.toString());
        } else {
            new_config = config;
        }
        return new_config;
    }

    public static String readFromFile(String path) {
        String result = "";
        try {
            int line;
            FileInputStream fis = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(fis);
            StringBuffer sb = new StringBuffer();
            byte b[] = new byte[1];
            while ((line = dis.read(b)) != -1) {
                String mData = new String(b, 0, line);
                sb.append(mData);
                result = sb.toString();
            }
            dis.close();
            fis.close();
        } catch (Exception e) {
        }
        return result;
    }
}
