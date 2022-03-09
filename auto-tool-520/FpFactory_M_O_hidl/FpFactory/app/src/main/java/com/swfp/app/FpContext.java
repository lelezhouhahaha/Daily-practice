package com.swfp.app;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.swfp.device.DeviceManager;

//import android.os.SystemProperties;

/**
 * Created by zhouj on 2017/4/5.
 */

public class FpContext extends Application {

    private static final String TAG = "sw-FpContext";
    private static final String DM_CLASS = "DeviceManager";
    private static FpContext instance;
    private DeviceManager manager;

    @Override
    public void onCreate() {
        super.onCreate();
        String clazz = getMetaData(DM_CLASS);
        Log.i(TAG, "DeviceManager impl class: " + clazz);
        try {
            if("com.swfp.device.FwDeviceManagerImpl".equals(clazz)){
                manager = (DeviceManager)Class.forName(clazz).getConstructor(Application.class).newInstance(this);
            }else if("com.swfp.device.HidlDeviceManagerImpl".equals(clazz)){
                //TODO 开启服务
                /*setSwfpService(true);
                SystemClock.sleep(500);
                if(!isSwfpServiceStart()){
                    setSwfpService(true);
                    SystemClock.sleep(500);
                }*/
                manager = (DeviceManager) Class.forName(clazz).newInstance();
            }else{
                manager = (DeviceManager) Class.forName(clazz).newInstance();
            }
        } catch (InstantiationException|IllegalAccessException|ClassNotFoundException e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError("DeviceManager impl newInstance error");
        } catch (Exception e) {
            e.printStackTrace();
            //SunwaveFingerprintServiceNotFonudException
            Log.e(TAG, "com.sunwavecorp.fingerprint.ISunwaveFingerprintService can not be touched, may be not add service or permission is not enough");
            throw new RuntimeException(e);
        }

        instance = this;
    }

    @Override
    public void onTerminate() {
        // 程序终止的时候执行
        Log.i(TAG, "onTerminate");
        super.onTerminate();
        //setSwfpService(false);
    }

    @Override
    public void onTrimMemory(int level) {
        // 程序在内存清理的时候执行
        Log.i(TAG, "onTrimMemory");
        super.onTrimMemory(level);
        //setSwfpService(false);
    }

    public static FpContext getContext() {
        return instance;
    }

    public DeviceManager getDeviceManager() {
        return manager;
    }

    private String getMetaData(String key) {
        ApplicationInfo appInfo = null;
        String val = null;
        try {
            appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (appInfo.metaData != null) {
            val = appInfo.metaData.getString(key);
        }
        return val;
    }

   /* private void setSwfpService(boolean enable) {
        SystemProperties.set("persist.sys.swfingerprint", enable ? "1" : "0");
    }

    private boolean isSwfpServiceStart() {
        return "1".equals(SystemProperties.get("persist.sys.swfingerprint", "0"));
    }*/

}
