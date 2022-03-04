package com.meigsmart.meigrs32.application;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;

import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.db.FunctionDao;
import com.meigsmart.meigrs32.db.FunctionDao_New;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.sunmi.pay.hardware.aidl.readcard.ReadCardOpt;
import com.sunmi.pay.hardware.aidlv2.emv.EMVOptV2;
import com.sunmi.pay.hardware.aidlv2.etc.ETCOptV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadOptV2;
import com.sunmi.pay.hardware.aidlv2.print.PrinterOptV2;
import com.sunmi.pay.hardware.aidlv2.readcard.ReadCardOptV2;
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2;
import com.sunmi.pay.hardware.aidlv2.system.BasicOptV2;
import com.sunmi.pay.hardware.aidlv2.tax.TaxOptV2;
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.hsa.ctp.device.sdk.SEService.SEServiceInterface;
import sunmi.paylib.SunmiPayKernel;

/**
 * Created by chenMeng on 2018/4/23.
 */
public class MyApplication extends Application {
    private static MyApplication instance;// application对象
    public FunctionDao mDb;
    public FunctionDao_New mDb_new;
    public static String RuninTestNAME = "";
    public static String PCBASignalNAME = "";
    public static String PCBANAME = "";
    public static String PCBAAutoTestNAME = "";
    public static String PreSignalNAME = "";
    public static String PreNAME = "";
    public static String MMI1_PreName = "";
    public static String MMI1_PreSignalName = "";
    public static String MMI2_PreName = "";
    public static String MMI2_PreSignalName = "";
    public static String InformationCheckName = "";
    public static String CustomerFatherName = "";
    public BasicOptV2 basicOptV2;           // 获取基础操作模块
    public ReadCardOptV2 readCardOptV2;     // 获取读卡模块
    public ReadCardOpt readCardOpt;
    public PinPadOptV2 pinPadOptV2;         // 获取PinPad操作模块
    public SecurityOptV2 securityOptV2;     // 获取安全操作模块
    public EMVOptV2 emvOptV2;               // 获取EMV操作模块
    public TaxOptV2 taxOptV2;               // 获取税控操作模块
    public ETCOptV2 etcOptV2;               // 获取ETC操作模块
    public PrinterOptV2 printerOptV2;       // 获取打印操作模块
    private boolean connectPaySDK;//是否已连接PaySDK
    public static SunmiPrinterService sunmiPrinterService;
    private SEServiceInterface mISEService = null;
    String mISEService_version ="";
    private boolean isMT537_version =false;
    private String projectName = "";

    @Override
    public void onCreate() {
        super.onCreate();
        if(SystemProperties.get("persist.sys.db_name_cit").equals("cit2_test")){
            mDb_new = new FunctionDao_New(getApplicationContext());
        }else{
            mDb = new FunctionDao(getApplicationContext());
        }
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        isMT537_version = "MT537".equals(projectName);
        String paysdk = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PAY_SDK_ENABLE);
        if("true".equals(paysdk)) {
            bindPaySDKService();
            bindPrintService();
        }
        if(isMT537_version) {
                    Intent intent = new Intent();
                    intent.setPackage("cn.hsa.ctp.device.sdk.SEService");
                    intent.setAction("cn.hsa.ctp.device.sdk.SEService.start");
                    bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
        setdefaultLanguage(instance);
    }

    public void setdefaultLanguage(Context context) {
        Configuration config = context.getResources().getConfiguration();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        config.locale = Locale.CHINESE;
        context.getResources().updateConfiguration(config, metrics);
    }
    public static MyApplication getInstance(){
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
    }
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try{
                mISEService = SEServiceInterface.Stub.asInterface(service);
                mISEService_version=mISEService.getVersion();
                Log.d("MyApplication","mISEService_version:"+mISEService_version);
            }catch (Exception e){
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Intent intent = new Intent();
            intent.setPackage("cn.hsa.ctp.device.sdk.SEService");
            intent.setAction("cn.hsa.ctp.device.sdk.SEService.start");
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
    };

    public boolean isConnectPaySDK() {
        return connectPaySDK;
    }

    /** bind PaySDK service */
    public void bindPaySDKService() {
        final SunmiPayKernel payKernel = SunmiPayKernel.getInstance();
        payKernel.initPaySDK(this, new SunmiPayKernel.ConnectCallback() {
            @Override
            public void onConnectPaySDK() {
                LogUtil.e( "onConnectPaySDK...");
                emvOptV2 = payKernel.mEMVOptV2;
                basicOptV2 = payKernel.mBasicOptV2;
                pinPadOptV2 = payKernel.mPinPadOptV2;
                readCardOptV2 = payKernel.mReadCardOptV2;
                securityOptV2 = payKernel.mSecurityOptV2;
                taxOptV2 = payKernel.mTaxOptV2;
                etcOptV2 = payKernel.mETCOptV2;
                printerOptV2 = payKernel.mPrinterOptV2;
                readCardOpt = payKernel.mReadCardOpt;
                connectPaySDK = true;
            }

            @Override
            public void onDisconnectPaySDK() {
                LogUtil.e( "onConnectPaySDK...");
                connectPaySDK = false;
                emvOptV2 = null;
                basicOptV2 = null;
                pinPadOptV2 = null;
                readCardOptV2 = null;
                securityOptV2 = null;
                taxOptV2 = null;
                etcOptV2 = null;
                printerOptV2 = null;
            }
        });
    }

    /**
     * 获取版本号
     *
     * @return 当前应用的版本号
     */
    public String getVersionName() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** bind printer service */
    private void bindPrintService() {
        try {
            InnerPrinterManager.getInstance().bindService(this, new InnerPrinterCallback() {
                @Override
                protected void onConnected(SunmiPrinterService service) {
                    sunmiPrinterService = service;
                }

                @Override
                protected void onDisconnected() {
                    sunmiPrinterService = null;
                }
            });
        } catch (InnerPrinterException e) {
            e.printStackTrace();
        }
    }

    /**
     * get App versionCode
     * @return
     */
    public String getVersionCode(){
        PackageManager packageManager=getApplicationContext().getPackageManager();
        PackageInfo packageInfo;
        String versionCode="";
        try {
            packageInfo=packageManager.getPackageInfo(getApplicationContext().getPackageName(),0);
            versionCode=packageInfo.versionCode+"";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    //hejianfeng add start
    private ExecutorService singleThreadPool=Executors.newSingleThreadExecutor();//hejianfeng add for zendao 10260
    public ExecutorService getSingleThreadPool(){//同步线程调用
        return singleThreadPool;
    }
    public VolumeViewListener volumeViewListener;
    public void setVolumeViewListener(VolumeViewListener viewListener){
        volumeViewListener=viewListener;
    }
    public interface VolumeViewListener{
        void volumeChange(int volume);
    }
    //hejianfeng add end
}
