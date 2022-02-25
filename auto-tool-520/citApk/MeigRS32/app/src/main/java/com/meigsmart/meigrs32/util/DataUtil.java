package com.meigsmart.meigrs32.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Xml;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.meigsmart.meigrs32.model.PersistResultModel;

import static android.content.Context.ACTIVITY_SERVICE;

public class DataUtil {

    /** 1s==1000ms */
    private final static int TIME_MILLISECONDS = 1000;
    /** 时间中的分、秒最大值均为60 */
    private final static int TIME_NUMBERS = 60;
    /** 时间中的小时最大值 */
    private final static int TIME_HOURSES = 24;

    private final static int MIN_DELAY_TIME= 400;  // 两次点击间隔不能少于400ms
    private static long lastClickTime = 0;

    public static final int USB_CHARGE = 0;
    public static final int DC_CHARGE = 1;
    public static final int CRADLE_CHARGE = 2;
    private final static String TAG_CHARGING_TYPE = "charging_type_node";
    private final static String TAG_DEVICE_NAME = "common_device_name_test";
    public static String getDeviceName(){
        return initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_DEVICE_NAME);
    }

    public static int  getChargingType(){
        int chargingType = 0;
        String typeNode = initConfig(Const.CIT_NODE_CONFIG_PATH, TAG_CHARGING_TYPE);
        if(typeNode != null && !typeNode.isEmpty()){
            try {
                chargingType = Integer.parseInt(readLineFromFile(typeNode).trim());
            }catch (Exception e){
                LogUtil.e("getChargingType fail");
            }
        }
        LogUtil.i("typeNode="+typeNode+"  chargingType="+chargingType);
        return chargingType;
    }

    /**
     * 将总秒数转换为时分秒表达形式
     * @param seconds 任意秒数
     * @return %s小时%s分%s秒
     */
    public static String formatTime(long seconds)
    {
        long hh = seconds / TIME_NUMBERS / TIME_NUMBERS;
        long mm = (seconds - hh * TIME_NUMBERS * TIME_NUMBERS) > 0 ? (seconds - hh * TIME_NUMBERS * TIME_NUMBERS) / TIME_NUMBERS : 0;
        long ss = seconds < TIME_NUMBERS ? seconds : seconds % TIME_NUMBERS;
        return     (hh == 0 ? "" : (hh < 10 ? "0" + hh : hh) + "小时")
                + (mm == 0 ? "" : (mm < 10 ? "0" + mm : mm) + "分")
                + (ss == 0 ? "" : (ss < 10 ? "0" + ss : ss) + "秒");
    }

    public static String formatTime(Context context, long seconds)
    {
        long hh = seconds / TIME_NUMBERS / TIME_NUMBERS;
        long mm = (seconds - hh * TIME_NUMBERS * TIME_NUMBERS) > 0 ? (seconds - hh * TIME_NUMBERS * TIME_NUMBERS) / TIME_NUMBERS : 0;
        long ss = seconds < TIME_NUMBERS ? seconds : seconds % TIME_NUMBERS;
        return     (hh == 0 ? "00:" : (hh < 10 ? "0" + hh : hh) + ":")
                + (mm == 0 ? "00:" : (mm < 10 ? "0" + mm : mm) + ":")
                + (ss == 0 ? "00" : (ss < 10 ? "0" + ss : ss));
    }

    public static String getAvailMemory(Context context){
        // 获取android当前可用内存大小
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        //mi.availMem; 当前系统的可用内存

        return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化
    }

    public static String getTotalMemory(Context context, String path){
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;

        try
        {
            FileReader localFileReader = new FileReader(path);
            BufferedReader localBufferedReader = new BufferedReader(
                    localFileReader, 8192);
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小

            arrayOfString = str2.split("\\s+");
            for (String num : arrayOfString) {
                Log.i(str2, num + "\t");
            }

            initial_memory = Long.valueOf(arrayOfString[1]).longValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
            localBufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        //return Formatter.formatFileSize(context, initial_memory);// Byte转换为KB或者MB，内存大小规格化
        //return initial_memory;
        return Formatter.formatFileSize(context, initial_memory);
    }

    public static String getRomSpace(Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockCount = stat.getBlockCount();
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();

        String totalSize = Formatter.formatFileSize(context, blockCount*blockSize);
        String availableSize = Formatter.formatFileSize(context, blockCount*availableBlocks);

        return totalSize;

    }



    /**
     * 获取CPU型号
     * @return
     */
    public static String getCpuName(){

        String str1 = "/proc/cpuinfo";
        String str2 = "";

        try {
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr);
            while ((str2=localBufferedReader.readLine()) != null) {
                if (str2.contains("Hardware")) {
                    return str2.split(":")[1];
                }
            }
            localBufferedReader.close();
        } catch (IOException e) {
        }
        return null;

    }

    /**
     * 获取config 文件中的配置项内容
     * @return
     */
    public static String initConfig(String path, String tag) {
        File configFile = new File(path);
        String tagValue = "";
        if (!configFile.exists()) {
            LogUtil.d(R.string.config_xml_not_found + path);
            return tagValue;
        }
        try {
            InputStream inputStream = new FileInputStream(path);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");
            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if(tag.equals(startTagName)) {
                            tagValue = xmlPullParser.nextText();
                            LogUtil.d("citapp tagValue:<" + tagValue + ">.");
                            return tagValue;
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return tagValue;
    }

    public static float readCPUUsage() {
        try {
            RandomAccessFile localObject1 = new RandomAccessFile("/proc/stat", "r");
            String[] localObject2 = localObject1.readLine().split(" ");
            long l1 = Long.parseLong(localObject2[5]);
            long l2 = Long.parseLong(localObject2[2]) + Long.parseLong(localObject2[3]) + Long.parseLong(localObject2[4]) + Long.parseLong(localObject2[6]) + Long.parseLong(localObject2[7]) + Long.parseLong(localObject2[8]);
            try {
                Thread.sleep(100L);
                localObject1.seek(0L);
                String str = localObject1.readLine();
                localObject1.close();
                String[] str1 = str.split(" ");
                long l3 = Long.parseLong(str1[5]);
                long l4 = Long.parseLong(str1[2]) + Long.parseLong(str1[3]) + Long.parseLong(str1[4]) + Long.parseLong(str1[6]) + Long.parseLong(str1[7]) + Long.parseLong(str1[8]);
                return (float) (Long.valueOf(100L * (l4 - l2) / (l4 + l3 - (l2 + l1))).longValue());
            } catch (Exception e) {
                e.printStackTrace();
                //sendErrorMsg(mHandler,e.getMessage());
                LogUtil.d("cpu usage statistical anomaly:" + e.getMessage());
            }
            return 0.0F;
        } catch (Exception localException1) {
            localException1.printStackTrace();
            //sendErrorMsg(mHandler,localException1.getMessage());
            LogUtil.d("CPU status file operation exception:" + localException1.getMessage());
        }
        return 0.0F;
    }

    public static String readLineFromFile( String path ){
        String data = "";
        FileInputStream file = null;
        try{
            file = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));
            data = reader.readLine();
            if (file != null) {
                file.close();
                file = null;
            }
        }catch(Exception e){
            try {
                if (file != null) {
                    file.close();
                    file = null;
                }
            } catch (IOException io) {
                LogUtil.e("readLineFromFile operation fail");
            }
        }
        return data;
    }

    public static float getBatteryVoltage(String fileNode) {
        String ret = DataUtil.readLineFromFile(fileNode);
        float batteryVoltage = Float.valueOf(ret);

        return batteryVoltage/1000;
    }

    public static boolean checkActivityStatus(Context context, String cls){
        boolean mark = false;
        String className;
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> appTask = am.getRunningTasks(1);
        if (appTask.size() > 0){
            for(ActivityManager.RunningTaskInfo info :appTask){
                className = info.topActivity.getClassName();
                if(className.equals(cls)){
                    if(info.numRunning > 0){
                        mark = true;
                    }
                    return mark;
                }
            }
        }
        return mark;
    }

    public static boolean isForeground(Context context, String className) {
        if (context == null || TextUtils.isEmpty(className))
            return false;
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        for (ActivityManager.RunningTaskInfo taskInfo : list) {
            if (taskInfo.topActivity.getShortClassName().contains(className)) { // 说明它已经启动了
                return true;
            }
        }
        return false;

    }

    public static boolean isForegroundPackage(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName))
            return false;
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        for (ActivityManager.RunningTaskInfo taskInfo : list) {
            if (taskInfo.topActivity.getPackageName().contains(packageName)) { // 说明它已经启动了
                return true;
            }
        }
        return false;

    }

    public static void stopActivity(Context mContext, String packageName){
        try {
            ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(mActivityManager, packageName);
        }catch (Exception e) {
            LogUtil.e("FmActivity", "operate forceStopPackage amnormal.");
            e.printStackTrace();
        }
    }

    public static boolean isFastClick() {

        boolean flag = true;
        long currentClickTime = System.currentTimeMillis();
        Log.d("DataUtil","currentClickTime:"+currentClickTime);
        Log.d("DataUtil","lastClickTime:"+lastClickTime);
        if ((currentClickTime - lastClickTime) >= MIN_DELAY_TIME) {
            flag = false;
            lastClickTime = currentClickTime;
            return flag;
        }
        lastClickTime = currentClickTime;
        Log.d("DataUtil","flag:"+flag);
        return flag;
    }

    public static List<PersistResultModel> setListOrder(List<String> regulation, List<PersistResultModel> targetList) {
        final List<String> orderRegulation = regulation;
        Collections.sort(targetList, new Comparator<PersistResultModel>() {
            @Override
            public int compare(PersistResultModel o1, PersistResultModel o2) {
                int io1 = orderRegulation.indexOf(o1.getName());
                int io2 = orderRegulation.indexOf(o2.getName());

//                if (io1 != -1) {
//                    io1 = targetList.size() - io1;
//                }
//                if (io2 != -1) {
//                    io2 = targetList.size() - io2;
//                }

                return io1 - io2;
            }
        });
        return targetList;
    }

}
