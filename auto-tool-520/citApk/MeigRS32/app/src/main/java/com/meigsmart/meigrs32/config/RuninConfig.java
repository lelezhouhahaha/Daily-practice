package com.meigsmart.meigrs32.config;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RuninConfig {
    public static final String TAG = "RuninConfig";

    public static final String TAG_RUNIN_CONFIG = "runin_config";
    public static final String TAG_TEST_LINE = "test_line";
    public static final String TAG_ACTIVITY_NAME = "ActivityName";
    public static final String TAG_RUNTIME = "runtime";

    public static final String RUNIN_CONFIG_XML_PATH = "/system/etc/meig/cit_runin_config.xml";
    //public static final String RUNIN_CONFIG_XML_PATH = "/sdcard/cit_runin_config.xml";

    private static Map<String, Integer> mRuninConfig = new HashMap<String, Integer>();
    private static long mRuninStartTime = 0;

    public static void setRuninStartTime() {
        mRuninStartTime = System.currentTimeMillis();
    }

    public static long getRuninStartTime(){
        return mRuninStartTime;
    }

    public static boolean isOverTotalRuninTime(Context context) {
        if( 0 >= getLeftTotalRuninTime(context)) {
            return true;
        } else {
            return false;
        }
    }

    public static long getLeftTotalRuninTime(Context context) {
        long totalTime = 1000 * getRunTime(context, "total");
        long currentTime = System.currentTimeMillis();
        return totalTime - (currentTime - mRuninStartTime);
    }

    public static long getTotalRuninTime(Context context) {
        long currentTime = System.currentTimeMillis();
        return currentTime - mRuninStartTime;
    }

    public static int getRunTime(Context context, String name) {
        if(mRuninConfig.get(0) == null && (mRuninConfig.size() ==  1) ) {
            initdata(context);
        }
        return mRuninConfig.getOrDefault(name, context.getResources().getInteger(R.integer.run_in_test_default_time)).intValue();
    }

    public static void putRunTime(String name, int value) {
        mRuninConfig.put(name, value);
    }

    public static void initdata(Context context) {
        File configFile = new File(RUNIN_CONFIG_XML_PATH);
        if(!configFile.exists()){
//            ToastUtil.showCenterLong(context.getString(R.string.config_xml_not_found,RUNIN_CONFIG_XML_PATH));
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(RUNIN_CONFIG_XML_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            String activityName = null;
            int runTime = 0;

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if (TAG_ACTIVITY_NAME.equals(startTagName)) {
                            activityName = xmlPullParser.nextText();
                        } else if (TAG_RUNTIME.equals(startTagName)) {
                            runTime = Integer.parseInt(xmlPullParser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String endTagName = xmlPullParser.getName();
                        if (TAG_RUNIN_CONFIG.equals(endTagName)) {
                            break;
                        }
                        if (TAG_TEST_LINE.equals(endTagName)) {
                            if (activityName != null && runTime != 0) {
                                LogUtil.i(TAG, "map put activityName:" + activityName + " runtime:" + runTime);
                                mRuninConfig.put(activityName, new Integer(runTime));
                            }
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
