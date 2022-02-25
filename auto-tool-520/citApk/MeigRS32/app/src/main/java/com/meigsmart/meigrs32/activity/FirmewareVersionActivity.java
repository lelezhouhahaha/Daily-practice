package com.meigsmart.meigrs32.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.support.annotation.RequiresApi;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.CitTestJni;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.IATUtils;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidl.AidlConstants;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import woyou.aidlservice.jiuiv5.IWoyouService;


public class FirmewareVersionActivity extends BaseActivity {
    @BindView(R.id.title)
    public TextView mTitle;
    //private String mFatherName = "";
    @BindView(R.id.TV_cpuname)
    public TextView mCpuname;
    @BindView(R.id.TP_fw_version)
    public TextView mTpFwVersion;
    @BindView(R.id.lcd_info)
    public TextView mLcdSupplier;
    @BindView(R.id.lcd_fw_version)
    public TextView mLcdFwVersion;
    @BindView(R.id.TV_RAM_size)
    public TextView mTVRamSize;
    @BindView(R.id.TV_ROM_size)
    public TextView mTVRomSize;
    @BindView(R.id.TV_Wan_Module_Firmeware_Version)
    public TextView mTVWanModuleFirmewareVersion;
    @BindView(R.id.sw_tag)
    public TextView mSwTag;
    @BindView(R.id.nfc_version)
    public TextView nfc_version;
    @BindView(R.id.fingerprint_version)
    public TextView fingerprint_version;
    @BindView(R.id.info_camera_type)
    public TextView cameraTypeInfo;
    @BindView(R.id.info_scan_type)
    public TextView mScanTypeInfo;
    @BindView(R.id.info_printer_type)
    public TextView mPrinterVersion;
    @BindView(R.id.SunMi_os_version)
    public TextView mSunMi_os_version;
    @BindView(R.id.boot_version)
    public TextView mBoot_version;
    @BindView(R.id.cfg_file_version)
    public TextView mCfg_file_version;
    @BindView(R.id.gpu_version)
    public TextView mGpu_version;
    @BindView(R.id.battery_version)
    public TextView mBattery_version;
    @BindView(R.id.battery_chip_version)
    public TextView mBattery_chip_version;


    private FirmewareVersionActivity mContext;
    private int mConfigTime = 0;
    private String tp_fw_contain = "";
    private String mLcdInfoStr = "";
    private String mLcdFwVersionStr = "";
    private boolean mTpHwVersionFlag = false;
    private boolean isCameraTypeFlag = false;
    private boolean mIsMT537 = false;
    private StorageManager mStorageManager;
    private IWoyouService mWoyouService = null;
    private final int HANDLER_FLUSH_DISPLAY = 1000;
    private static final String TAG = FirmewareVersionActivity.class.getSimpleName();
    private final String nfc_version_path = "common_nfc_version_path";
    private final String displaySecurityModuleInfoPath = "common_SoftwareVersionActivity_Security_Module_Info_config_bool";
    private final String getmTpHwVersionCmpStrConfigStr = "common_software_version_tp_hardware_config_compare_value";
    private final String mTpHwVersionPathConfigStr = "common_software_version_tp_hardware_config_path";
    private final String mTpHwVersionContainConfigStr = "common_software_version_tp_hardware_config_contain";
    private final String mTpHwVersionPathConfigStr2 = "common_software_version_tp_hardware_config_path2";
    private final String mTpHwVersionFlagConfigStr = "common_software_version_tp_hardware_config_bool";
    private final String fingerprint_version_path = "common_SoftwareVersionActivity_fingerprint_version_path";
    private final String mBattery_version_path = "common_SoftwareVersionActivity_battery_version_path";
    private final String mBattery_chip_version_path = "common_SoftwareVersionActivity_battery_chip_version_path";
    private final String mLcdInfoPathKeyword = "common_SoftwareVersionActivity_lcd_version_path";
    private final String ATCMMD_GET_WAN_MODULE_FIRMEWARE_VERSION = "AT+CGMR";
    private final String GET_WAN_MODULE_KEYWORD= "BASE  Version:";
    private final String SCAN_TYPE_PATH = "/dev/sunmi/scan/scan_head_type";


    @Override
    protected int getLayoutId() {
        return R.layout.activity_firmware_version;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.pcba_firmware_version);
        super.startBlockKeys = !Const.isCanBackKey;

        if (!MyApplication.getInstance().isConnectPaySDK()) {
            MyApplication.getInstance().bindPaySDKService();
            ToastUtil.showCenterShort(getString(R.string.connect_loading));
        }
        PrinterServiceBinding();
        mIsMT537 = "MT537".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK));
        mHandler.sendEmptyMessageDelayed(HANDLER_FLUSH_DISPLAY, 1000);
    }

    public   void showFirmwareInfo(){
        String displaySecurityModuleInfoStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, displaySecurityModuleInfoPath);
        Boolean displaySecurityModuleInfoBool = true;
        if(!displaySecurityModuleInfoStr.equals("")){
            displaySecurityModuleInfoBool = Boolean.getBoolean(displaySecurityModuleInfoStr);
        }
        if(!displaySecurityModuleInfoBool){
            mSunMi_os_version.setVisibility(View.GONE);
            mBoot_version.setVisibility(View.GONE);
            mCfg_file_version.setVisibility(View.GONE);
            mGpu_version.setVisibility(View.GONE);
        }

        /*String wifi_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.TEST_RIL_STATE);
        String wifi_path1 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WIFI_mt535);
        if(((null != wifi_path) && !wifi_path.isEmpty())||((null != wifi_path1) && !wifi_path1.isEmpty())){
            WIFI_BUILD = FileUtil.readFromFile(wifi_path).contains("1");
            LogUtil.e(TAG, "wifi_path1:" + FileUtil.readFromFile(wifi_path1));
            WIFI_BUILD1 = FileUtil.readFromFile(wifi_path1).contains("0");
        }*/

        String SunMi_firmwareVersion = "";
        String SunMi_bootVersion = "";
        String SunMi_cfg_file_version = "";
        String gpu_version = "";
        try{
            if(MyApplication.getInstance().basicOptV2!=null) {
                SunMi_firmwareVersion = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.FIRMWARE_VERSION);
                SunMi_bootVersion = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.BootVersion);
                SunMi_cfg_file_version = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.CFG_FILE_VERSION);
            }
            gpu_version = "IMG G8322";
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mSunMi_os_version.setText(
                Html.fromHtml(
                        getResources().getString(R.string.SunMi_firmwareVersion) +
                                "&nbsp;" + "<font color='#0000FF'>" + SunMi_firmwareVersion + "</font>"
                ));
        mBoot_version.setText(
                Html.fromHtml(
                        getResources().getString(R.string.SP_boot_version) +
                                "&nbsp;" + "<font color='#0000FF'>" + SunMi_bootVersion + "</font>"
                ));
        mCfg_file_version.setText(
                Html.fromHtml(
                        getResources().getString(R.string.Config_file_version) +
                                "&nbsp;" + "<font color='#0000FF'>" + SunMi_cfg_file_version + "</font>"
                ));
        mGpu_version.setText(
                Html.fromHtml(
                        getResources().getString(R.string.gpu_version) +
                                "&nbsp;" + "<font color='#0000FF'>" + gpu_version + "</font>"
                ));

        String nfcVersionPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, nfc_version_path);
        String nfc_vs = "";
        if (nfcVersionPath != null && !nfcVersionPath.isEmpty() & !nfcVersionPath.equals("")) {
            nfc_vs = readFromFile(nfcVersionPath);
            //SystemProperties.set("persist.custmized.nfc_version", nfc_vs);
            SystemProperties.set(OdmCustomedProp.getNfcVersionProp(), nfc_vs);
            nfc_version.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_nfc) +
                                    "&nbsp;" + "<font color='#0000FF'>" + nfc_vs + "</font>"
                    )
            );
        } else {
            nfc_version.setVisibility(View.GONE);
        }

        String fingerprintPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, fingerprint_version_path);
        LogUtil.d("fingerprintPath:" +fingerprintPath);
        String fingerprintVersionStr = "";
        if(!fingerprintPath.isEmpty() && fingerprintPath != null){
            fingerprintVersionStr = getFileVersion(fingerprintPath, "firmware");
            //SystemProperties.set("persist.custmized.fingerprint_version", fingerprintVersionStr);
            SystemProperties.set(OdmCustomedProp.getFingerprintVersionProp(), fingerprintVersionStr);
            fingerprint_version.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.fingerprint_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + fingerprintVersionStr + "</font>"
                    )
            );
        }

        String cpuName = DataUtil.getCpuName();
        mCpuname.setText(
                Html.fromHtml(
                        getResources().getString(R.string.version_cpu_name) +
                                "&nbsp;" + "<font color='#0000FF'>" + cpuName + "</font>"
                )
        );

        String tp_fw_version = "";
        String tp_fw_cmp_str = "";
        String strTmp = "";

        tp_fw_cmp_str = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, getmTpHwVersionCmpStrConfigStr);
        if ((tp_fw_cmp_str == null) && (tp_fw_cmp_str.isEmpty()))
            tp_fw_cmp_str = "Firmware Configuration Version";
        LogUtil.d("tp_fw_cmp_str:" + tp_fw_cmp_str);

        strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mTpHwVersionPathConfigStr);
        tp_fw_contain=DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH,mTpHwVersionContainConfigStr);
        if ((strTmp != null) && !strTmp.isEmpty())
            tp_fw_version = getFileVersion(strTmp, tp_fw_cmp_str);
        if(null==tp_fw_version || tp_fw_version.equals("")){
            strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mTpHwVersionPathConfigStr2);
            if ((strTmp != null) && !strTmp.isEmpty())
                tp_fw_version = getFileVersion(strTmp, tp_fw_cmp_str);
        }
        strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mTpHwVersionFlagConfigStr);
        if ((strTmp != null) && !strTmp.isEmpty()) {
            mTpHwVersionFlag = strTmp.equals("true");
        } else mTpHwVersionFlag = true;
        LogUtil.d("mTpFwVersionFlag str:" + strTmp);
        LogUtil.d("tp_fw_version str:" + tp_fw_version);
        LogUtil.d("mTpHwVersionFlag:" + mTpHwVersionFlag);
        if (mTpHwVersionFlag) {
            mTpFwVersion.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_default_config_software_version_tp_fw_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + tp_fw_version + "</font>"
                    )
            );
        } else mTpFwVersion.setVisibility(View.GONE);

        String availRamSize = DataUtil.getAvailMemory(mContext);
        String ramSize = DataUtil.getTotalMemory(mContext, getResources().getString(R.string.version_default_config_software_version_ram_size_path));
        mTVRamSize.setText(
                Html.fromHtml(
                        getResources().getString(R.string.version_default_config_software_version_ram_size) +
                                "&nbsp;" + "<font color='#0000FF'>" + availRamSize + "/" + ramSize + "</font>"
                )
        );

        //String romSize = DataUtil.getRomSpace(mContext);
        mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        String romSize = Formatter.formatFileSize(mContext, mStorageManager.getPrimaryStorageSize());
        mTVRomSize.setText(
                Html.fromHtml(
                        getResources().getString(R.string.version_default_config_software_version_rom_size) +
                                "&nbsp;" + "<font color='#0000FF'>" + romSize + "</font>"
                )
        );
        boolean isnopmi = (SystemProperties.get("ro.boot.pmi_mode", "").equals("nopmi"));

        String mWanModuleFirmewareVerson = getModemVersionInfo(GET_WAN_MODULE_KEYWORD);
        mTVWanModuleFirmewareVersion.setText(Html.fromHtml(
                getResources().getString(R.string.version_modem_version) +
                        "&nbsp;" + "<font color='#0000FF'>" + mWanModuleFirmewareVerson + "</font>"));

        String sw_tag = SystemProperties.get(getResources().getString(R.string.version_default_config_software_version_tag), "");
        if((sw_tag != null) && (!sw_tag.isEmpty())){
            mSwTag.setVisibility(View.VISIBLE);
            mSwTag.setText(Html.fromHtml(
                    getResources().getString(R.string.version_default_config_software_version_sw_version) +
                            "&nbsp;" + "<font color='#0000FF'>" + sw_tag + "</font>"));
        }

        if(mIsMT537){
            String mCameraInfo = getCameraInfo();
            cameraTypeInfo.setText(Html.fromHtml(
                    getResources().getString(R.string.version_camera_type) +
                            "&nbsp;" + "<font color='#0000FF'>" + mCameraInfo + "</font>"));
        }else {
            String cameraTypeInfoStr = DataUtil.readLineFromFile("/sys/kernel/cam_dev_num/cam_model");
            LogUtil.d("cameraTypeInfoStr:" + cameraTypeInfoStr);
            if (!cameraTypeInfoStr.isEmpty()) {
                cameraTypeInfo.setVisibility(View.VISIBLE);
                if (!cameraTypeInfoStr.contains("none")) {
                    isCameraTypeFlag = true;
                }
                //SystemProperties.set("persist.custmized.camera_type", cameraTypeInfoStr);
                SystemProperties.set(OdmCustomedProp.getCameraTypeProp(), cameraTypeInfoStr);
                cameraTypeInfo.setText(Html.fromHtml(
                        getResources().getString(R.string.version_camera_type) +
                                "&nbsp;" + "<font color='#0000FF'>" + cameraTypeInfoStr + "</font>"));
            }
        }

        String mScanTypeInfoStr = DataUtil.readLineFromFile(SCAN_TYPE_PATH);
        Log.d(TAG, "mScanTypeInfoStr:<" + mScanTypeInfoStr + ">.");
        mScanTypeInfo.setText(Html.fromHtml(
                getResources().getString(R.string.version_scan_type) +
                        "&nbsp;" + "<font color='#0000FF'>" + mScanTypeInfoStr + "</font>"));

        String mPrinterTypeVersion = getPrinterFirmewareInfo();
        Log.d(TAG, "mPrinterTypeVersion:<" + mPrinterTypeVersion + ">.");
        mPrinterVersion.setText(Html.fromHtml(
                getResources().getString(R.string.version_printer_version) +
                        "&nbsp;" + "<font color='#0000FF'>" + mPrinterTypeVersion + "</font>"));

        String mBatteryVersion = getContentFromCommonConfigPath(mBattery_version_path);
        if(!mBatteryVersion.isEmpty()){
            mBattery_version.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.battery_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + mBatteryVersion + "</font>"
                    )
            );
        }

        String mBatteryChipVersion = getContentFromCommonConfigPath(mBattery_chip_version_path);
        if(!mBatteryChipVersion.isEmpty()){
            mBattery_chip_version.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.battery_chip_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + mBatteryChipVersion + "</font>"
                    )
            );
        }

        getLcdVersionInfo();
        Log.d(TAG, "mLcdInfoStr:<" + mLcdInfoStr + ">");
        if(!mLcdInfoStr.isEmpty()){
            mLcdSupplier.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_default_config_software_version_lcd_info) +
                                    "&nbsp;" + "<font color='#0000FF'>" + mLcdInfoStr + "</font>"
                    )
            );
        }
        Log.d(TAG, "mLcdFwVersionStr:<" + mLcdFwVersionStr + ">.");
        if(!mLcdFwVersionStr.isEmpty()){
            mLcdFwVersion.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_default_config_software_version_lcd_fw_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + mLcdFwVersionStr + "</font>"
                    )
            );
        }
    }

    private ServiceConnection mConnService = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(FirmewareVersionActivity.this, "service disconnected", Toast.LENGTH_LONG).show();
            Log.d(TAG,"server disconnect");
            //setButtonEnable(false);
            mWoyouService = null;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            PrinterServiceBinding();
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mWoyouService = IWoyouService.Stub.asInterface(service);
            try {
                mWoyouService.printerInit(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            getPrinterFirmewareInfo();
        }
    };

    private String getContentFromCommonConfigPath(String mFilePath){
        String mPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mFilePath);
        LogUtil.d("mPath:" +mPath);
        String mContent = "";
        if(!mPath.isEmpty() && mPath != null){
            mContent = DataUtil.readLineFromFile(mPath);
        }
        return mContent;
    }

    private void PrinterServiceBinding(){
        Intent intent=new Intent();
        intent.setPackage("woyou.aidlservice.jiuiv5");
        intent.setAction("woyou.aidlservice.jiuiv5.IWoyouService");
        startService(intent);
        bindService(intent, mConnService,Context.BIND_AUTO_CREATE);
    }

    private String getPrinterFirmewareInfo(){
        try {
            String mPrinterVersion = mWoyouService.getPrinterVersion();
            Log.d(TAG, "mPrinterVersion:<" + mPrinterVersion + ">.");
            return mPrinterVersion;
        } catch (RemoteException e) {
            Log.d(TAG, "====>error:"+e.toString());
            e.printStackTrace();
        }
        return "";
    }

    private String getModemVersionInfo(String keyword){
        String mWanModuleFirmewareVerson = IATUtils.sendAtCmd(ATCMMD_GET_WAN_MODULE_FIRMEWARE_VERSION);
        String[] mWanModuleInfoArrays=mWanModuleFirmewareVerson.split("\n");
        for(int i = 0; i< mWanModuleInfoArrays.length; i++){
            if(mWanModuleInfoArrays[i].contains(keyword)){
                String mWanModuleInfo = FileUtil.replaceBlank(mWanModuleInfoArrays[i].substring(mWanModuleInfoArrays[i].lastIndexOf(":") + 1) );
                Log.d(TAG, "mWanModuleInfo:" + mWanModuleInfo);
                return mWanModuleInfo;
            }
        }
        return "";
    }

    private void getLcdVersionInfo() {
        String mLcdInfoPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mLcdInfoPathKeyword);
            if(!mLcdInfoPath.isEmpty()){
            String mLcdInfoAll = FileUtil.readFile(mLcdInfoPath);
            String[] mLcdInfoArrays = mLcdInfoAll.split("\n");
            for (int i = 0; i < mLcdInfoArrays.length; i++) {
                if (mLcdInfoArrays[i].contains("Lcd_Driver_Ic :")) {
                    mLcdFwVersionStr = FileUtil.replaceBlank(mLcdInfoArrays[i].substring(mLcdInfoArrays[i].lastIndexOf(":") + 1));
                } else if (mLcdInfoArrays[i].contains("Lcd_Manufacturer :")) {
                    mLcdInfoStr = FileUtil.replaceBlank(mLcdInfoArrays[i].substring(mLcdInfoArrays[i].lastIndexOf(":") + 1));
                }
            }
        }
    }

    private String getCameraInfo(){
        String face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
        String camera_version = "";
        if((face_select!=null)&&(!face_select.equals(""))&&(face_select.length()==8)) {
            if (face_select.substring(2, 3).equals("1")) {
                String camera_version2= SystemProperties.get("vendor.cam.sensor.front","");
                String camera_version3= SystemProperties.get("vendor.cam.sensor.front_aux","");
                String camera_version1= SystemProperties.get("vendor.cam.sensor.front_aux2","");
                camera_version=camera_version1+","+camera_version2+","+camera_version3;

            }else{
                String camera_version1= SystemProperties.get("vendor.cam.sensor.back","");
                String camera_version2= SystemProperties.get("vendor.cam.sensor.front","");
                String camera_version3= SystemProperties.get("vendor.cam.sensor.front_aux","");
                String camera_version4= SystemProperties.get("vendor.cam.sensor.scan","");
                camera_version=camera_version1+","+camera_version2+","+camera_version3+","+camera_version4;
            }
        }
        return camera_version;
    }

    @Override
    protected void onDestroy(){
        mHandler.removeMessages(HANDLER_FLUSH_DISPLAY);
        super.onDestroy();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_FLUSH_DISPLAY:
                    Log.d(TAG, "HANDLER_FLUSH_DISPLAY start.");
                    if (MyApplication.getInstance().basicOptV2!=null) {
                        showFirmwareInfo();
                    }else {
                        mHandler.sendEmptyMessageDelayed(HANDLER_FLUSH_DISPLAY, 1000);
                    }
                    break;
            }
        }
    };

    private String getFileVersion(String path, String substr) {
        String data = "";
        FileInputStream file = null;
        BufferedReader reader = null;
        try {
            file = new FileInputStream(path);
            reader = new BufferedReader(new InputStreamReader(file));
            while ((data = reader.readLine()) != null) {
                if (data.contains(substr)) {
                    if (file != null) {
                        file.close();
                        file = null;
                    }
                    return data.substring(data.lastIndexOf(":") + 1);
                }else if(tp_fw_contain.equals("false")){
                    return data;
                }
            }
            if (file != null) {
                file.close();
                file = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String readFromFile(String path) {
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
