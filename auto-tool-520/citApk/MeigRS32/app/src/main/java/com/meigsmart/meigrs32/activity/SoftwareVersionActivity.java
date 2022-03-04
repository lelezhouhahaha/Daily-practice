package com.meigsmart.meigrs32.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.DrmInitData;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.CitTestJni;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import cn.hsa.ctp.device.sdk.SEService.SEServiceInterface;


public class SoftwareVersionActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private SoftwareVersionActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    private View mVersion;
    @BindView(R.id.device_id)
    public TextView mDeviceId;
    @BindView(R.id.IMEI0_id)
    public TextView mIMEI_0;
    @BindView(R.id.IMEI1_id)
    public TextView mIMEI_1;
    @BindView(R.id.IMEI_id)
    public TextView mIMEI;
    @BindView(R.id.MEID_id)
    public TextView mMEID;
    @BindView(R.id.pcba_sn)
    public TextView mPcbaSn;
    @BindView(R.id.btSn)
    public TextView mBtSn;
    @BindView(R.id.wifiMac)
    public TextView mWifiMac;
    @BindView(R.id.hardware_version)
    public TextView mHardwareVersion;
    @BindView(R.id.software_version)
    public TextView mSoftwareVersion;
    @BindView(R.id.custom_software_version)
    public TextView mCustomSoftwareVersion;
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
    @BindView(R.id.TV_battery_voltage)
    public TextView mTVBatteryVoltage;
    @BindView(R.id.TV_battery_capacity)
    public TextView mTVBatteryCapacity;
    @BindView(R.id.sw_tag)
    public TextView mSwTag;
    @BindView(R.id.se_version)
    public TextView mSeVersion;
    @BindView(R.id.nfc_version)
    public TextView nfc_version;
    @BindView(R.id.fingerprint_version)
    public TextView fingerprint_version;
	@BindView(R.id.battery_version)
    public TextView battery_version;
    @BindView(R.id.battery_chip_version)
    public TextView battery_chip_version;
    @BindView(R.id.model_version)
    public TextView model_version;
    @BindView(R.id.info_camera_type)
    public  TextView cameraTypeInfo;
    @BindView(R.id.info_firmware)
    public  TextView mFirmware;
    @BindView(R.id.info_firmware2)
    public  TextView mFirmware2;
    @BindView(R.id.Version_release)
    public TextView release_version;
    @BindView(R.id.SunMi_os_version)
    public TextView mSunMi_os_version;
    @BindView(R.id.boot_version)
    public TextView mBoot_version;
    @BindView(R.id.cfg_file_version)
    public TextView mCfg_file_version;
    @BindView(R.id.gpu_version)
    public TextView mGpu_version;
    @BindView(R.id.psam_version)
    public TextView psam_version;

    private StorageManager mStorageManager;
    private String Version_Release = null;
    String wifi_getMacFromFile ="02:00:00:00:00:00";

    int i;
    private int mConfigTime = 0;
    private String mDeviceIdPersist = "";
    private boolean mDeviceIdFlag = false;
    private boolean imeiflag = false;
    private String pcbashowmode = "";
    private String mSoftwareVersionKey = "common_software_version_config_persist";
    private String mDeviceIdConfigStr = "common_software_version_device_id_config_persist";
    private String mDeviceIdFlagConfigStr = "common_software_version_device_id_config_bool";
    private String mTpHwVersionPathConfigStr = "common_software_version_tp_hardware_config_path";
    private String mTpHwVersionPathConfigStr2 = "common_software_version_tp_hardware_config_path2";
    private String mNeedSn = "common_software_version_need_sn";
    //add by maohaojie on 2019.07.11
    private String mTpHwVersionContainConfigStr = "common_software_version_tp_hardware_config_contain";
    private String imeishownumber = "common_imei_show_tag_value";
    String tp_fw_contain="";
    private String mTpHwVersionFlagConfigStr = "common_software_version_tp_hardware_config_bool";
    private String getmTpHwVersionCmpStrConfigStr = "common_software_version_tp_hardware_config_compare_value";
    private boolean mTpHwVersionFlag = false;
    private String mHardwareVersionKey = "common_software_version_hardware_config_bool";
    private String mPcbasnshowmode = "common_pcbasn_show_mode";
    private String mCustomPcbasn = "common_pcbasn_show_custom";
    private boolean mHardwareVersionValue = true;
    private String imei_1;
    private TelephonyManager telMgr = null;
    private String mBatteryVoltagePath = "";
    private String mBatteryCapacityPath = "";
    private String mBatteryVoltageKey = "primary_battery_status_node";
    private String mBatteryVoltageDefaultPath = "/sys/class/power_supply/battery/voltage_now";
    private String mBatteryCapacityKey = "capacity_node";
    private String mBatteryCapacityDefaultPath = "/sys/class/power_supply/battery/capacity";
    private String mhwpath = "sys/devices/platform/soc/soc:meig-hwversion/hwversion";
    private String HARDWARE_COMMON_PATH="common_hardware_version_path";
    private String HARDWARE_COMMON_VALUE="common_hardware_version_path_value";
    public String system_meid = null;
    private boolean isMC526_version =false;
    private String mc526_module = null;
    String imei_0=null;
    int sdkVersion = android.os.Build.VERSION.SDK_INT;
    String meid0 = "";
    String meid1 = "";
    String meid = "";
    private boolean isMC501 = false;
    private boolean noNeedSn =false;
    private String TAG_MC501 = "common_device_name_test";
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    private String WIFI_mt535 = "common_device_wifi_only";

    private boolean isMC518 = "MC518".equals(DataUtil.getDeviceName());
    private boolean isSLM500S = "slm500s".equals(DataUtil.getDeviceName());
    private boolean isMT535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
    private boolean wifi_bluetooth = false;
    private boolean firmware_version = true;
    private boolean firmware_version2 = true;
    private CitTestJni mCitTestJni = null;
    private int mRspStatus = -1;
    String idDevice = "";
    String snPcba = "";
    String strTmp = "";
    String hardware_version ="";
    String availRamSize ="";
    String ramSize ="";
    String romSize ="";
    String cpuName ="";
    String version = "";
    String hardware="";

    private final String COMMONCONFIG_SIMINFO = "common_config_sim_info_display_enable";
    private String nfc_vs = "";
    private String nfc_version_path = "common_nfc_version_path";
    private String fingerprint_version_path = "common_SoftwareVersionActivity_fingerprint_version_path";
    private String battery_version_path = "common_SoftwareVersionActivity_battery_version_path";
    private String battery_chip_version_path = "common_SoftwareVersionActivity_battery_chip_version_path";
    private String displaySecurityModuleInfoPath = "common_SoftwareVersionActivity_Security_Module_Info_config_bool";
    private final String mLcdInfoPathKeyword = "common_SoftwareVersionActivity_lcd_version_path";
    private final String TAG = SoftwareVersionActivity.class.getSimpleName();
    private final int HANDLER_SIM_DISPLAY = 1002;
    private final int HANDLER_SOFTWAREVERSION_TEST_RESULT = 1005;
    private final int HANDLER_SOFTWAREVERSION_TEST_RESULT_FAIL = 1006;

    private boolean isMT537_version =false;
    private boolean isMT520_version =false;
    private boolean isMC520_GMS_version = false;
    private boolean mSimCardDisplaySuccessFlag = false;
    private boolean mSoftwareInformationSuccessFlag = false;

    private boolean WIFI_BUILD = false;
    private boolean WIFI_BUILD1 = false;
    private boolean SIM_HIDE_DISPLAY = false;
    private boolean isWifiOnly = false;

    private boolean isCameraTypeFlag = false;
    String SunMi_firmwareVersion ="";
    String SunMi_bootVersion = "";
    String SunMi_cfg_file_version = "";
    String gpu_version = "";
    private String projectName = "";
    String tp_fw_version = "";
    private String batteryChipVersionStr = "";
    private String batteryVersionStr = "";
    private String mLcdInfoStr = "";
    private String mLcdFwVersionStr = "";
    private String mProjectName = "";
    private String strTmpSn = "";
    private String pcbaSn = "";
    private SEServiceInterface mISEService = null;
    String mISEService_version ="";
    private boolean isBindService = false;
    private boolean doule_psam =false;

    private Runnable mRun;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_software_version;
    }

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.pcba_software_version);
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        //mSuccess.setVisibility(View.VISIBLE);
        mFail.setOnClickListener(this);
        mFail.setVisibility(View.VISIBLE);
        mDialog.setCallBack(this);

        if (SystemProperties.get("ro.product.name", "").contains("Hera51")) {
            mIMEI_0.setVisibility(View.GONE);
            mIMEI_1.setVisibility(View.GONE);
        } else {
            mIMEI.setVisibility(View.GONE);
        }
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        isMT537_version = "MT537".equals(projectName);
        isMT520_version = "MC520".equals(projectName);
        isMC520_GMS_version = "MC520_GMS".equals(projectName);
        mProjectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        if(isMT537_version) {
                    Intent intent = new Intent();
                    intent.setPackage("cn.hsa.ctp.device.sdk.SEService");
                    intent.setAction("cn.hsa.ctp.device.sdk.SEService.start");
                    bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
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
        if(isMT535){
            fingerprint_version.setVisibility(View.GONE);
            mSeVersion.setVisibility(View.GONE);
        }
        String wifi_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.TEST_RIL_STATE);
        String wifi_path1 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WIFI_mt535);
        if(((null != wifi_path) && !wifi_path.isEmpty())||((null != wifi_path1) && !wifi_path1.isEmpty())){
            WIFI_BUILD = FileUtil.readFromFile(wifi_path).contains("1");
            LogUtil.e("zhangshuo", "wifi_path1:" + FileUtil.readFromFile(wifi_path1));
            WIFI_BUILD1 = FileUtil.readFromFile(wifi_path1).contains("0");
        }


        String needsn = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mNeedSn);
        if((null != needsn) && !needsn.isEmpty()){
            noNeedSn = needsn.equals("false");
        }
        SIM_HIDE_DISPLAY = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, COMMONCONFIG_SIMINFO).contains("false");
        isWifiOnly =SystemProperties.get("ro.build.meig.feature.wifi","").equals("true") || SystemProperties.get("ro.radio.noril", "").equals("true")
                || WIFI_BUILD || WIFI_BUILD1;


        if (SIM_HIDE_DISPLAY || isWifiOnly) {
            mIMEI.setVisibility(View.GONE);
            mIMEI_0.setVisibility(View.GONE);
            mIMEI_1.setVisibility(View.GONE);
            mMEID.setVisibility(View.GONE);
        }

        String imei_show = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH,imeishownumber );
        if (imei_show.equals("0")){
            mIMEI_1.setVisibility(View.GONE);
            imeiflag = true;
        }


        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        isMC526_version = SystemProperties.get("forge.cubic.ap.version").contains("MC526");
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                LogUtil.d(TAG, " mConfigTime:" + mConfigTime);
                updateFloatView(mContext, mConfigTime);
                if (mConfigTime == 0 ||
                        mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)) {
                    LogUtil.d(TAG, " finish current test");
                    if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                        mHandler.sendEmptyMessage(HANDLER_SOFTWAREVERSION_TEST_RESULT);
                        return;
                    }
                    return;
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        mBatteryVoltagePath = DataUtil.initConfig(Const.CIT_NODE_CONFIG_PATH, mBatteryVoltageKey);
        if (TextUtils.isEmpty(mBatteryVoltagePath))
            mBatteryVoltagePath = mBatteryVoltageDefaultPath;

        mBatteryCapacityPath = DataUtil.initConfig(Const.CIT_NODE_CONFIG_PATH, mBatteryCapacityKey);
        if (TextUtils.isEmpty(mBatteryCapacityPath))
            mBatteryCapacityPath = mBatteryCapacityDefaultPath;


        mDeviceIdPersist = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mDeviceIdConfigStr);
        strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mDeviceIdFlagConfigStr);
        if ((strTmp != null) && !strTmp.isEmpty())
            mDeviceIdFlag = strTmp.equals("true");
        else mDeviceIdFlag = true;
        if (mDeviceIdPersist == null || mDeviceIdPersist.isEmpty())
            mDeviceIdPersist = getResources().getString(R.string.version_default_config_device_id_persist);

        strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mHardwareVersionKey);
        if ((strTmp != null) && !strTmp.isEmpty())
            mHardwareVersionValue = strTmp.equals("true");
        else mHardwareVersionValue = true;

        LogUtil.d("mDeviceIdPersist:" + mDeviceIdPersist);
        LogUtil.d("mDeviceIdFlag:" + mDeviceIdFlag);
        LogUtil.d("mHardwareVersionValue:" + mHardwareVersionValue);
        mDialog.setSuccess();
        hardware = SystemProperties.get(getResources().getString(R.string.version_default_config_hardware_version_persist), "");
        if (hardware.equals("")) {
          String  hardware_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HARDWARE_COMMON_PATH);
         if(hardware_path!=null&&!hardware_path.isEmpty()&!hardware_path.equals("")){
             hardware=getSubBatteryStatus(hardware_path);
          }else{
               hardware = readFromFile(mhwpath);
         }
        }
        String hardware_value = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HARDWARE_COMMON_VALUE);
        if(null!=hardware_value && !hardware_value.isEmpty()){
            try {
                int value_hw = Integer.parseInt(readFromFile(hardware_value).trim());
                if(value_hw < 2000){
                    hardware = getResources().getString(R.string.Asia);
                }else{
                    hardware = getResources().getString(R.string.American);
                }
                if(SystemProperties.get("ro.boot.yyversion").equals("yes")){
                    hardware += "/YY";
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if (mDeviceIdFlag) {
            idDevice = SystemProperties.get(mDeviceIdPersist, getString(R.string.empty));
            mDeviceId.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_device_id) +
                                    "&nbsp;" + "<font color='#0000FF'>" + idDevice + "</font>"
                    ));
        } else mDeviceId.setVisibility(View.GONE);

        snPcba = getPcbaSn();
        mPcbaSn.setText(
                Html.fromHtml(
                        getResources().getString(R.string.version_pcba_sn) +
                                "&nbsp;" + "<font color='#0000FF'>" + snPcba + "</font>"
                ));

        //add for bug 20558 by zhaohairuo @2018-10-24 start
        if ((mFatherName.equals(MyApplication.PCBANAME)) || (mFatherName.equals(MyApplication.PCBASignalNAME))) {
            mDeviceId.setVisibility(View.GONE);
            mDeviceIdFlag = false;
        }
        //add for bug 20558 by zhaohairuo @2018-10-24 end
        //add for bug 23329 by zhaohairuo @2019-01-21 start
        if ((mFatherName.equals(MyApplication.PreSignalNAME)) || (mFatherName.equals(MyApplication.PreNAME))) {
            mDeviceId.setVisibility(View.GONE);
            mDeviceIdFlag = false;
        }
        //add for bug 23329 by zhaohairuo @2019-01-21 end
        if (mFatherName.equals(MyApplication.InformationCheckName) || isMT535||isMT537_version) {
            mBtSn.setVisibility(View.VISIBLE);
            mWifiMac.setVisibility(View.VISIBLE);
            String bthaddress = getBluetooth();
            String macid = getMacID();
            if(bthaddress != null && !bthaddress.isEmpty() && macid != null && !macid.isEmpty()){
            wifi_bluetooth = true;}
            mBtSn.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.bluetooth_address) +
                                    "&nbsp;" + "<font color='#0000FF'>" + getBluetooth() + "</font>"
                    ));
            if(!isMT537_version){
            mWifiMac.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.wifi_mac_address) +
                                    "&nbsp;" + "<font color='#0000FF'>" + getMacID() + "</font>"
                    ));
            }else{
                if(getMacFromFile()!=null) {
                    wifi_getMacFromFile = getMacFromFile().toUpperCase();
                }
                    mWifiMac.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.wifi_mac_address) +
                                            "&nbsp;" + "<font color='#0000FF'>" + wifi_getMacFromFile + "</font>"
                            ));
                new GetSunmiVersion().execute();
            }
            if(isMT535){
                new GetSunmiVersion().execute();
            }
        }
        isMC501 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_MC501).equals("MC501");
        if (mHardwareVersionValue && !isMC501) {
            mHardwareVersion.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_hardware_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + hardware + "</font>"
                    ));
        } else mHardwareVersion.setVisibility(View.GONE);
        if(isMT535){
            String devicenode = readFromFile("/sys/devices/platform/soc/soc:meig-hwversion/wifi_version");
            Log.d("GM","devicenode："+devicenode);
            if(devicenode.contains("0")){
                hardware_version="标准";
            }else if(devicenode.contains("1")){
                hardware_version="欧洲";
            }else if(devicenode.contains("2")){
                hardware_version="拉美";
            }
            Log.d("GM","hardware_version："+hardware_version);
            if (mHardwareVersionValue) {
                mHardwareVersion.setText(
                        Html.fromHtml(
                                getResources().getString(R.string.version_hardware_version) +
                                        "&nbsp;" + "<font color='#0000FF'>" + hardware +"(" + hardware_version +")"+"</font>"
                        ));
            } else mHardwareVersion.setVisibility(View.GONE);
        }

        if("MC520_GMS".equals(mProjectName)) {
            String mSoftwareVersionConfigPersistValue = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mSoftwareVersionKey);
            if(!mSoftwareVersionConfigPersistValue.isEmpty()){
                version = SystemProperties.get(mSoftwareVersionConfigPersistValue, "");
            }
        }else {
            version = SystemProperties.get(
                    getResources().getString(R.string.version_default_config_software_version_persist), "");
            Log.d("demo_so", version + "");
            if (version.equals("")) {
                Log.d("demo_so", "yes_2");
                version = SystemProperties.get(
                        getResources().getString(R.string.version_default_config_software_version_meig), "");
                if (version.equals("")) {
                    version = SystemProperties.get("meig.ap.sw.version", "");
                }
                Log.d("demo_so", "yes_1");
            }
        }
        mSoftwareVersion.setText(
                Html.fromHtml(
                        getResources().getString(R.string.version_software_version) +
                                "&nbsp;" + "<font color='#0000FF'>" + version + "</font>"
                ));
        String custom_version = SystemProperties.get(
                getResources().getString(R.string.version_default_config_custom_software_version_persist), "");
        if (!isMT535) {
        mCustomSoftwareVersion.setText(
                Html.fromHtml(
                        getResources().getString(R.string.custom_version_software_version) +
                                "&nbsp;" + "<font color='#0000FF'>" + custom_version + "</font>"
                )
        );
        } else{ mCustomSoftwareVersion.setVisibility(View.GONE);}
        // add by maohaojie on 2020.05.18 for MC509
        if(isMT520_version || isMC520_GMS_version){
            FileUtil.writeToFile("/sys/meige_nfc/nfc_version_second_read", "1");
        }
        String nfcVersionPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, nfc_version_path);
        if (nfcVersionPath != null && !nfcVersionPath.isEmpty() & !nfcVersionPath.equals("")) {
            nfc_vs = getSubBatteryStatus(nfcVersionPath);
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
		
		String batteryVersionPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, battery_version_path);
        LogUtil.d("batteryVersionPath:" +batteryVersionPath);
        if(!batteryVersionPath.isEmpty() && batteryVersionPath != null){
            batteryVersionStr = DataUtil.readLineFromFile(batteryVersionPath);
            //SystemProperties.set("persist.custmized.battery_version", batteryVersionStr);
            SystemProperties.set(OdmCustomedProp.getBatteryVersionProp(), batteryVersionStr);
            battery_version.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.battery_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + batteryVersionStr + "</font>"
                    )
            );
        }

        String batteryChipVersionPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, battery_chip_version_path);
        LogUtil.d("batteryChipVersionPath:" +batteryChipVersionPath);
        if(!batteryChipVersionPath.isEmpty() && batteryChipVersionPath != null){
            batteryChipVersionStr = DataUtil.readLineFromFile(batteryChipVersionPath);
            //SystemProperties.set("persist.custmized.battery_version", batteryChipVersionStr);
            SystemProperties.set(OdmCustomedProp.getBatteryVersionProp(), batteryChipVersionStr);
            battery_chip_version.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.battery_chip_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + batteryChipVersionStr + "</font>"
                    )
            );
        }

        if(isMT537_version){
            mMEID.setVisibility(View.GONE);
            gpu_version = "IMG G8322";
            mGpu_version.setVisibility(View.VISIBLE);
            mCfg_file_version.setVisibility(View.VISIBLE);
            mBoot_version.setVisibility(View.VISIBLE);
            mSunMi_os_version.setVisibility(View.VISIBLE);
            battery_version.setVisibility(View.VISIBLE);
            battery_chip_version.setVisibility(View.VISIBLE);
            //SystemProperties.set("persist.custmized.softwareversion",version);
            SystemProperties.set(OdmCustomedProp.getSoftwareVersionProp(),version);
            String battery_version = FileUtil.readFromFile("sys/class/power_supply/battery/sunmi_battery_fw_number");
            String face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
            if((face_select!=null)&&(!face_select.equals(""))&&(face_select.length()==8)) {
                if (face_select.substring(2, 3).equals("1")) {
                    String camera_version2= SystemProperties.get("vendor.cam.sensor.front","");
                    String camera_version3= SystemProperties.get("vendor.cam.sensor.front_aux","");
                    String camera_version1= SystemProperties.get("vendor.cam.sensor.front_aux2","");
                    String camera_version=camera_version1+","+camera_version2+","+camera_version3;
                    //SystemProperties.set("persist.custmized.camera_type",camera_version);
                    SystemProperties.set(OdmCustomedProp.getCameraTypeProp(),camera_version);
                }else{
                    String camera_version1= SystemProperties.get("vendor.cam.sensor.back","");
                    String camera_version2= SystemProperties.get("vendor.cam.sensor.front","");
                    String camera_version3= SystemProperties.get("vendor.cam.sensor.front_aux","");
                    String camera_version4= SystemProperties.get("vendor.cam.sensor.scan","");
                    String camera_version=camera_version1+","+camera_version2+","+camera_version3+","+camera_version4;
                    //SystemProperties.set("persist.custmized.camera_type",camera_version);
                    SystemProperties.set(OdmCustomedProp.getCameraTypeProp(),camera_version);
                }
                }
            //SystemProperties.set("persist.custmized.battery_version",battery_version);
            SystemProperties.set(OdmCustomedProp.getBatteryVersionProp(),battery_version);
            Version_Release =SystemProperties.get("ro.build.version.release");
            release_version.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.release_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + Version_Release + "</font>"
                    )
            );
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
        }else {
            release_version.setVisibility(View.GONE);
        }
        //add by maohaojie on 2020.12.25 for task 9214
        if(isMC526_version){
            mc526_module= SystemProperties.get("gsm.version.baseband");
            model_version.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.model_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + mc526_module + "</font>"
                    )
            );
        }

        cpuName = DataUtil.getCpuName();
        mCpuname.setText(
                Html.fromHtml(
                        getResources().getString(R.string.version_cpu_name) +
                                "&nbsp;" + "<font color='#0000FF'>" + cpuName + "</font>"
                )
        );

        telMgr = TelephonyManager.from(mContext);
        Log.d("MM0604", "sdkVersion:" + sdkVersion);
        if(sdkVersion > 25) {
            meid0 = GetPhoneInfo(0);
            Log.d("MM0604", "meid0:" + meid0);
            meid1 = GetPhoneInfo(1);
            Log.d("MM0604", "meid1:" + meid1);
        }else {
            meid0 = telMgr.getDeviceId(0);
            meid1 = telMgr.getDeviceId(1);
            Log.d("MM0604", "sdkVersion <= 25 meid  0  :" + meid0);
            Log.d("MM0604", "sdkVersion <= 25 meid  1  :" + meid1);
        }

        if ((TextUtils.isEmpty(meid0)|| meid0.equals("0")
                || meid0.startsWith("000000"))&&!TextUtils.isEmpty(meid1) && !meid1.equals("0")
                && !meid1.startsWith("000000")){
            imei_0 = telMgr.getImei(1);
        }else{
            imei_0 = telMgr.getImei(0);
        }
        mIMEI_0.setText(
                Html.fromHtml(
                        getResources().getString(R.string.IMEI_0) +
                                "&nbsp;" + "<font color='#0000FF'>" + imei_0 + "</font>"
                )
        );
        mIMEI.setText(
                Html.fromHtml(
                        getResources().getString(R.string.IMEI) +
                                "&nbsp;" + "<font color='#0000FF'>" + telMgr.getImei(0) + "</font>"
                )
        );
        if(!SIM_HIDE_DISPLAY && !isWifiOnly) {
            if (telMgr.getPhoneCount() == 2) {
                if ((TextUtils.isEmpty(meid0) || meid0.equals("0")
                        || meid0.startsWith("000000")) && !TextUtils.isEmpty(meid1) && !meid1.equals("0")
                        && !meid1.startsWith("000000")) {
                    imei_1 = telMgr.getImei(0);
                } else {
                    imei_1 = telMgr.getImei(1);
                }
                mIMEI_1.setText(
                        Html.fromHtml(
                                getResources().getString(R.string.IMEI_1) +
                                        "&nbsp;" + "<font color='#0000FF'>" + imei_1 + "</font>"
                        )
                );
            } else if (telMgr.getPhoneCount() == 1) {
                mIMEI_0.setVisibility(View.GONE);
                mIMEI_1.setVisibility(View.GONE);
                mIMEI.setVisibility(View.VISIBLE);
            }
        }
        //add by maohaojie on 2019.04.11 for bug 642 start
        if ((TextUtils.isEmpty(meid0)|| meid0.equals("0")
                || meid0.startsWith("000000"))&&!TextUtils.isEmpty(meid1) && !meid1.equals("0")
                && !meid1.startsWith("000000")) {
            meid = meid1;
        } else {
            meid = meid0;
        }

        mMEID.setText(
                Html.fromHtml(
                        getResources().getString(R.string.IMEID) +
                                "&nbsp;" + "<font color='#0000FF'>" + meid + "</font>"
                )
        );
        //add by maohaojie on 2019.04.11 for bug 642 end

        if(isMC518){
            mMEID.setVisibility(View.GONE);
            
            mCitTestJni = new CitTestJni();
            mRspStatus = mCitTestJni.testSeVersion();
            String seVersion = "";
            if(mRspStatus == 0){
                seVersion = mCitTestJni.getSeVersion();
            }
            mSeVersion.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_SE_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + seVersion + "</font>"
                    )
            );
        }else{
            /*meig:jicong.wang add for bug 12141 start {@*/
            if(isSLM500S){
                mMEID.setVisibility(View.GONE);
            }
            /*meig:jicong.wang add for bug 12141 end @}*/
            if(isMT535){
                mMEID.setVisibility(View.GONE);
            }
            mSeVersion.setVisibility(View.GONE);
        }

        String tp_fw_cmp_str = "";
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
                //SystemProperties.set("persist.custmized.tp_version", tp_fw_version);
            SystemProperties.set(OdmCustomedProp.getTpVersionProp(), tp_fw_version);

        } else mTpFwVersion.setVisibility(View.GONE);

        String mLcdInfoPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mLcdInfoPathKeyword);
        if(!mLcdInfoPath.isEmpty()){
            String mLcdInfoAll =  FileUtil.readFile(mLcdInfoPath);
            String[] mLcdInfoArrays=mLcdInfoAll.split("\n");
            for(int i = 0; i< mLcdInfoArrays.length; i++) {
                if(mLcdInfoArrays[i].contains("Lcd_Driver_Ic :")){
                    mLcdFwVersionStr = FileUtil.replaceBlank(mLcdInfoArrays[i].substring(mLcdInfoArrays[i].lastIndexOf(":") + 1) );
                }else if(mLcdInfoArrays[i].contains("Lcd_Manufacturer :")){
                    mLcdInfoStr = FileUtil.replaceBlank(mLcdInfoArrays[i].substring(mLcdInfoArrays[i].lastIndexOf(":") + 1) );
                }
            }
            Log.d(TAG, "mLcdInfoStr:<" + mLcdInfoStr + ">");
            if(!mLcdInfoStr.isEmpty()){
                mLcdSupplier.setVisibility(View.VISIBLE);
                mLcdSupplier.setText(
                        Html.fromHtml(
                                getResources().getString(R.string.version_default_config_software_version_lcd_info) +
                                        "&nbsp;" + "<font color='#0000FF'>" + mLcdInfoStr + "</font>"
                        )
                );
            }
            Log.d(TAG, "mLcdFwVersionStr:<" + mLcdFwVersionStr + ">.");
            if(!mLcdFwVersionStr.isEmpty()){
                mLcdFwVersion.setVisibility(View.VISIBLE);
                mLcdFwVersion.setText(
                        Html.fromHtml(
                                getResources().getString(R.string.version_default_config_software_version_lcd_fw_version) +
                                        "&nbsp;" + "<font color='#0000FF'>" + mLcdFwVersionStr + "</font>"
                        )
                );
            }
        }

        availRamSize = DataUtil.getAvailMemory(mContext);
        ramSize = DataUtil.getTotalMemory(mContext, getResources().getString(R.string.version_default_config_software_version_ram_size_path));
        mTVRamSize.setText(
                Html.fromHtml(
                        getResources().getString(R.string.version_default_config_software_version_ram_size) +
                                "&nbsp;" + "<font color='#0000FF'>" + availRamSize + "/" + ramSize + "</font>"
                )
        );

        //String romSize = DataUtil.getRomSpace(mContext);
        mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        romSize = Formatter.formatFileSize(mContext, mStorageManager.getPrimaryStorageSize());
        mTVRomSize.setText(
                Html.fromHtml(
                        getResources().getString(R.string.version_default_config_software_version_rom_size) +
                                "&nbsp;" + "<font color='#0000FF'>" + romSize + "</font>"
                )
        );
        boolean isnopmi = (SystemProperties.get("ro.boot.pmi_mode", "").equals("nopmi"));
        if (mFatherName.equals(MyApplication.InformationCheckName) && !isnopmi) {
            Float vol = Float.valueOf(DataUtil.getBatteryVoltage(mBatteryVoltagePath));
            vol = vol / 1000;
            BigDecimal b   =   new   BigDecimal(vol);
            float   vol_f1   =   b.setScale(2,   BigDecimal.ROUND_HALF_UP).floatValue();
            mTVBatteryVoltage.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_default_config_software_version_battery_voltage ) +
                                    "&nbsp;" + "<font color='#0000FF'>" + vol_f1 + "V" + "</font>"
                    )
            );

            Float batteryCapacityValue = Float.valueOf(0);
            String batteryCapacityStr = DataUtil.readLineFromFile(mBatteryCapacityPath);
            if (!TextUtils.isEmpty(batteryCapacityStr))
                batteryCapacityValue = Float.valueOf(batteryCapacityStr);
            mTVBatteryCapacity.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_default_config_software_version_battery_capacity) +
                                    "&nbsp;" + "<font color='#0000FF'>" + batteryCapacityValue + "%" + "</font>"
                    )
            );
        }

        String sw_tag = "";
        if(isMC520_GMS_version) {
            sw_tag = SystemProperties.get("persist.vendor.meig.sw.tag.version", "");
        }else {
            sw_tag = SystemProperties.get(getResources().getString(R.string.version_default_config_software_version_tag), "");
        }
        if((sw_tag != null) && (!sw_tag.isEmpty())){
            mSwTag.setVisibility(View.VISIBLE);
            mSwTag.setText(Html.fromHtml(
                    getResources().getString(R.string.version_default_config_software_version_sw_version) +
                            "&nbsp;" + "<font color='#0000FF'>" + sw_tag + "</font>"));
        }
        String cameraTypeInfoStr = DataUtil.readLineFromFile("/sys/kernel/cam_dev_num/cam_model");
        if(isMT535){
            cameraTypeInfoStr = DataUtil.readLineFromFile("/sys/kernel/camera_type/camera_value");
        }
        LogUtil.d("cameraTypeInfoStr:" + cameraTypeInfoStr);
        if(!cameraTypeInfoStr.isEmpty()){
            cameraTypeInfo.setVisibility(View.VISIBLE);
            if(!cameraTypeInfoStr.contains("none")){
                isCameraTypeFlag = true;
            }
            if(!isMT535){
                //SystemProperties.set("persist.custmized.camera_type", cameraTypeInfoStr);
                SystemProperties.set(OdmCustomedProp.getCameraTypeProp(), cameraTypeInfoStr);
            }
            cameraTypeInfo.setText(Html.fromHtml(
                    getResources().getString(R.string.version_camera_type) +
                            "&nbsp;" + "<font color='#0000FF'>" + cameraTypeInfoStr + "</font>"));
        }

        if (isMT535){
            if(MyApplication.getInstance().basicOptV2!=null) {
                //String firmware = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.FIRMWARE_VERSION);
                //String firmwareFlag = "persist.custmized.sp_1902";
                String firmwareFlag = OdmCustomedProp.getSp1902Prop();
                SystemProperties.set(firmwareFlag, SunMi_firmwareVersion);
                LogUtil.e("zhangshuo", "firmware:" + SunMi_firmwareVersion);
                if (SunMi_firmwareVersion == null ||  SunMi_firmwareVersion.isEmpty()) {
                    firmware_version = false;
                }
                mFirmware.setVisibility(View.VISIBLE);
                mFirmware.setText(Html.fromHtml(
                        getResources().getString(R.string.version_firmware) +
                                "&nbsp;" + "<font color='#0000FF'>" + SunMi_firmwareVersion + "</font>"));
            }
            /*try {
                if(MyApplication.getInstance().basicOptV2!=null) {
                    String firmware2 = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.MSR2_FW_VER);
                    String firmware2Flag = "persist.custmized.sp_1902s";
                    SystemProperties.set(firmware2Flag, firmware2);
                    LogUtil.e("zhangshuo", "firmware2:" + firmware2);
                    if (firmware2 == null || firmware2.isEmpty()) {
                        firmware_version2 = false;
                    }
                    mFirmware2.setVisibility(View.VISIBLE);
                    mFirmware2.setText(Html.fromHtml(
                            getResources().getString(R.string.version_firmware2) +
                                    "&nbsp;" + "<font color='#0000FF'>" + firmware2 + "</font>"));
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }*/

        }

        if(isMT520_version|| isMC520_GMS_version){
            fingerprint_version.setVisibility(View.VISIBLE);
            nfc_version.setVisibility(View.VISIBLE);
            battery_version.setVisibility(View.VISIBLE);
            battery_chip_version.setVisibility(View.VISIBLE);
        }

        //add by maohaojie on 2019.07.25 for wifi only Version
        boolean isWifi = SystemProperties.get("ro.radio.noril", "").equals("true") || SystemProperties.get("ro.radio.noril", "").equals("yes");

       if(isWifiOnly || SIM_HIDE_DISPLAY) {
           if(isMT535){
           if (isMT535 && (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PCBASignalNAME)
                   || mFatherName.equals(MyApplication.MMI1_PreName) || mFatherName.equals(MyApplication.MMI1_PreSignalName))) {
               if ((((pcbaSn != null) && (!pcbaSn.isEmpty()) && (pcbaSn.length() > 4) && (!pcbaSn.equals("unknown"))
               ) || noNeedSn) &&
                       ((hardware != null) && (!hardware.isEmpty())) &&
                       ((version != null) && (!version.isEmpty())) &&
 /*                      ((custom_version != null) && (!custom_version.isEmpty())) && */
                       ((cpuName != null) && (!cpuName.isEmpty())) &&
                       ((availRamSize != null) && (!availRamSize.isEmpty())) &&
                       ((romSize != null) && (!romSize.isEmpty()))
                       && firmware_version /* && wifi_bluetooth */){
                   mSoftwareInformationSuccessFlag = true;
                   mSimCardDisplaySuccessFlag = true;
                   mHandler.sendEmptyMessageDelayed(1001,1300);
               }
           }

           if(isMT535 && (mFatherName.equals(MyApplication.MMI2_PreName) || mFatherName.equals(MyApplication.MMI2_PreSignalName))){
               if((((pcbaSn != null) && (!pcbaSn.isEmpty()) && (pcbaSn.length()>4) && (!pcbaSn.equals("unknown"))
                       && (!strTmpSn.equals("unknown")) && (strTmpSn != null) && (!strTmpSn.isEmpty()) && (strTmpSn.length()>4))||noNeedSn) &&
                       ((hardware != null) && (!hardware.isEmpty()))&&
                       ((version != null) && (!version.isEmpty())) &&
                       /* ((custom_version != null) && (!custom_version.isEmpty())) && */
                       ((cpuName != null) && (!cpuName.isEmpty())) &&
                       ((availRamSize != null) && (!availRamSize.isEmpty())) &&
                       ((romSize != null) && (!romSize.isEmpty()))
                       && firmware_version  && wifi_bluetooth ){
                   mSoftwareInformationSuccessFlag = true;
                   mSimCardDisplaySuccessFlag = true;
                   mHandler.sendEmptyMessageDelayed(1001,1300);
               }
           }
           }else {
               if (((!mDeviceIdFlag) || ((idDevice != null) && (!idDevice.isEmpty()))) &&
                       (((snPcba != null) && (!snPcba.isEmpty())) || noNeedSn) &&
                       ((!mHardwareVersionValue) || ((hardware != null) && (!hardware.isEmpty()))) &&
                       ((version != null) && (!version.isEmpty())) &&
                       ((custom_version != null) && (!custom_version.isEmpty())) &&
                       ((cpuName != null) && (!cpuName.isEmpty())) &&
                       ((!mTpHwVersionFlag) || ((tp_fw_version != null) && (!tp_fw_version.isEmpty()))) &&
                       ((availRamSize != null) && (!availRamSize.isEmpty())) &&
                       ((sw_tag != null) && (!sw_tag.isEmpty())) &&
                       ((romSize != null) && (!romSize.isEmpty())) &&
                       isCameraTypeFlag) {
                   mSoftwareInformationSuccessFlag = true;
                   mSimCardDisplaySuccessFlag = true;
                   mSuccess.setVisibility(View.VISIBLE);
               }
           }

        }else{
           if(isMT535) {
               if (isMT535 && (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PCBASignalNAME)
                       || mFatherName.equals(MyApplication.MMI1_PreName) || mFatherName.equals(MyApplication.MMI1_PreSignalName))) {
                   if ((((pcbaSn != null) && (!pcbaSn.isEmpty()) && (pcbaSn.length() > 4) && (!pcbaSn.equals("unknown"))
                   ) || noNeedSn) &&
                           ((hardware != null) && (!hardware.isEmpty())) &&
                           ((version != null) && (!version.isEmpty())) &&
                           /*                  ((custom_version != null) && (!custom_version.isEmpty())) && */
                           ((cpuName != null) && (!cpuName.isEmpty())) &&
                           ((availRamSize != null) && (!availRamSize.isEmpty())) &&
                           ((romSize != null) && (!romSize.isEmpty()))
                           && firmware_version && firmware_version2 /* && wifi_bluetooth */) {
                       mSoftwareInformationSuccessFlag = true;
/*                   if (( ( (imei_0 == null) || (imei_0.isEmpty()) )||
                           ( (telMgr.getPhoneCount() == 2) && ( (imei_1 == null) || (imei_1.isEmpty()) ) ) ) && (mFatherName.equals(MyApplication.MMI2_PreName) || mFatherName.equals(MyApplication.MMI2_PreSignalName))){
                       LogUtil.d("citapk return; imei0:<" + imei_0 + "> imei1:<" + imei_1 + "> telMgr.getPhoneCount():<" + telMgr.getPhoneCount() + ">.");
                       return;
                   } */
                   mSimCardDisplaySuccessFlag = true; //535 MMI1 and PCBA not check imei and meid.
                   mHandler.sendEmptyMessageDelayed(1001, 1300);
               }
           }

           if(isMT535 && (mFatherName.equals(MyApplication.MMI2_PreName) || mFatherName.equals(MyApplication.MMI2_PreSignalName))){
               if((((pcbaSn != null) && (!pcbaSn.isEmpty()) && (pcbaSn.length()>4) && (!pcbaSn.equals("unknown"))
                       && (!strTmpSn.equals("unknown")) && (strTmpSn != null) && (!strTmpSn.isEmpty()) && (strTmpSn.length()>4))||noNeedSn) &&
                       ((hardware != null) && (!hardware.isEmpty()))&&
                       ((version != null) && (!version.isEmpty())) &&
                       /* ((custom_version != null) && (!custom_version.isEmpty())) && */
                       ((cpuName != null) && (!cpuName.isEmpty())) &&
                       ((availRamSize != null) && (!availRamSize.isEmpty())) &&
                       ((romSize != null) && (!romSize.isEmpty()))
                       && firmware_version && firmware_version2  && wifi_bluetooth ){
                   mSoftwareInformationSuccessFlag = true;
                   if (( ( (imei_0 == null) || (imei_0.isEmpty()) || imei_0.length()<2)||
                           ( (telMgr.getPhoneCount() == 2) && ( (imei_1 == null) || (imei_1.isEmpty()) ||imei_1.length()<2) ) ) && (mFatherName.equals(MyApplication.MMI2_PreName) || mFatherName.equals(MyApplication.MMI2_PreSignalName))){
                       LogUtil.d("citapk return; imei0:<" + imei_0 + "> imei1:<" + imei_1 + "> telMgr.getPhoneCount():<" + telMgr.getPhoneCount() + ">.");
                       mSimCardDisplaySuccessFlag = false;
                       mHandler.sendEmptyMessageDelayed(HANDLER_SIM_DISPLAY, 2000);
                       return;
                   }
                   mSimCardDisplaySuccessFlag = true;
                   mHandler.sendEmptyMessageDelayed(1001, 1300);
               }
           }

           }else {
               if (((!mDeviceIdFlag) || ((idDevice != null) && (!idDevice.isEmpty()))) &&
                       (((snPcba != null) && (!snPcba.isEmpty())) || noNeedSn) &&
                       (isMC501 || (!mHardwareVersionValue) || ((hardware != null) && (!hardware.isEmpty()))) &&
                       ((version != null) && (!version.isEmpty())) &&
                       ((custom_version != null) && (!custom_version.isEmpty())) &&
                       ((cpuName != null) && (!cpuName.isEmpty())) &&
                       ((!mTpHwVersionFlag) || ((tp_fw_version != null) && (!tp_fw_version.isEmpty()))) &&
                       ((availRamSize != null) && (!availRamSize.isEmpty())) &&
                       ((sw_tag != null) && (!sw_tag.isEmpty())) &&
                       ((romSize != null) && firmware_version && firmware_version2 && (!romSize.isEmpty())) &&
                       /*((meid != null) && (!meid.isEmpty()) && !meid.startsWith("000000")) &&*/ isCameraTypeFlag) {
                   mSoftwareInformationSuccessFlag = true;
                   if (((imei_0 == null) || (imei_0.isEmpty())) ||
                           ((telMgr.getPhoneCount() == 2) && ((imei_1 == null) || (imei_1.isEmpty())))) {
                       LogUtil.d("citapk return; imei0:<" + imei_0 + "> imei1:<" + imei_1 + "> telMgr.getPhoneCount():<" + telMgr.getPhoneCount() + ">.");
                       mSimCardDisplaySuccessFlag = false;
                       mHandler.sendEmptyMessageDelayed(HANDLER_SIM_DISPLAY, 2000);
                       return;
                   }
                   if((meid == null) || (meid.isEmpty()) || meid.startsWith("000000")){
                       LogUtil.d(TAG, "citapk meid is empty");
                       mSimCardDisplaySuccessFlag = false;
                       mHandler.sendEmptyMessageDelayed(HANDLER_SIM_DISPLAY, 2000);
                       return;
                   }
                   mSimCardDisplaySuccessFlag = true;
                   mSuccess.setVisibility(View.VISIBLE);
               }
           }
       }
        
       /*if(!mSimCardDisplaySuccessFlag){
           mHandler.sendEmptyMessageDelayed(HANDLER_SIM_DISPLAY, 2000);
       }*/

    }

    private void showSimInfo(){
        if(telMgr == null)
            telMgr = TelephonyManager.from(mContext);
        Log.d("MM0604", "sdkVersion:" + sdkVersion);
        if(sdkVersion > 25) {
            meid0 = GetPhoneInfo(0);
            Log.d("MM0604", "meid0:" + meid0);
            meid1 = GetPhoneInfo(1);
            Log.d("MM0604", "meid1:" + meid1);
        }else {
            meid0 = telMgr.getDeviceId(0);
            meid1 = telMgr.getDeviceId(1);
            Log.d("MM0604", "sdkVersion <= 25 meid  0  :" + meid0);
            Log.d("MM0604", "sdkVersion <= 25 meid  1  :" + meid1);
        }

        if ((TextUtils.isEmpty(meid0)|| meid0.equals("0")
                || meid0.startsWith("000000"))&&!TextUtils.isEmpty(meid1) && !meid1.equals("0")
                && !meid1.startsWith("000000")){
            imei_0 = telMgr.getImei(1);
        }else{
            imei_0 = telMgr.getImei(0);
        }
        mIMEI_0.setText(
                Html.fromHtml(
                        getResources().getString(R.string.IMEI_0) +
                                "&nbsp;" + "<font color='#0000FF'>" + imei_0 + "</font>"
                )
        );
        mIMEI.setText(
                Html.fromHtml(
                        getResources().getString(R.string.IMEI) +
                                "&nbsp;" + "<font color='#0000FF'>" + telMgr.getImei(0) + "</font>"
                )
        );
        if(!SIM_HIDE_DISPLAY && !isWifiOnly) {
            if (telMgr.getPhoneCount() == 2) {
                if ((TextUtils.isEmpty(meid0) || meid0.equals("0")
                        || meid0.startsWith("000000")) && !TextUtils.isEmpty(meid1) && !meid1.equals("0")
                        && !meid1.startsWith("000000")) {
                    imei_1 = telMgr.getImei(0);
                } else {
                    imei_1 = telMgr.getImei(1);
                }
                mIMEI_1.setText(
                        Html.fromHtml(
                                getResources().getString(R.string.IMEI_1) +
                                        "&nbsp;" + "<font color='#0000FF'>" + imei_1 + "</font>"
                        )
                );
            } else if (telMgr.getPhoneCount() == 1) {
                mIMEI_0.setVisibility(View.GONE);
                mIMEI_1.setVisibility(View.GONE);
                mIMEI.setVisibility(View.VISIBLE);
            }
        }
        //add by maohaojie on 2019.04.11 for bug 642 start
        if ((TextUtils.isEmpty(meid0)|| meid0.equals("0")
                || meid0.startsWith("000000"))&&!TextUtils.isEmpty(meid1) && !meid1.equals("0")
                && !meid1.startsWith("000000")) {
            meid = meid1;
        } else {
            meid = meid0;
        }

        mMEID.setText(
                Html.fromHtml(
                        getResources().getString(R.string.IMEID) +
                                "&nbsp;" + "<font color='#0000FF'>" + meid + "</font>"
                )
        );
        if ( ( (imei_0 == null) || (imei_0.isEmpty()) )||
                ( (telMgr.getPhoneCount() == 2) && ( (imei_1 == null) || (imei_1.isEmpty()) ) ) ){
            LogUtil.d("citapk return; imei0:<" + imei_0 + "> imei1:<" + imei_1 + "> telMgr.getPhoneCount():<" + telMgr.getPhoneCount() + ">.");
            mSimCardDisplaySuccessFlag = false;
            return;
        }
        if((meid == null) || (meid.isEmpty()) || meid.startsWith("000000")){
            LogUtil.d(TAG, "showSimInfo citapk meid is empty");
            mSimCardDisplaySuccessFlag = false;
            return;
        }
        mSimCardDisplaySuccessFlag = true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mSuccess.setVisibility(View.VISIBLE);
                    if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME))
                        deInit(mFatherName, SUCCESS);
                    break;
                case HANDLER_SOFTWAREVERSION_TEST_RESULT_FAIL:
                    String reason = (String) msg.obj;
                    deInit(mFatherName, FAILURE, reason);
                    break;
                case HANDLER_SIM_DISPLAY:
                    showSimInfo();
                    LogUtil.d(TAG, "1 mSimCardDisplaySuccessFlag:" + mSimCardDisplaySuccessFlag);
                    LogUtil.d(TAG, "1 mSoftwareInformationSuccessFlag:" + mSoftwareInformationSuccessFlag);
                    if(!mSimCardDisplaySuccessFlag){
                        mHandler.sendEmptyMessageDelayed(HANDLER_SIM_DISPLAY, 2000);
                    }else{
                        mHandler.sendEmptyMessage(HANDLER_SOFTWAREVERSION_TEST_RESULT);
                    }
                    break;
                case 1003:
                    Log.d(TAG,"mISEService_version:"+mISEService_version);
                    SystemProperties.set(OdmCustomedProp.getPsamVersionProp(),mISEService_version);
                    psam_version.setText(Html.fromHtml(
                            getResources().getString(R.string.psam_version) +
                                    "&nbsp;" + "<font color='#0000FF'>" + mISEService_version + "</font>"
                    ));
                    psam_version.setVisibility(View.VISIBLE);
                        if((getBluetooth()!=null)&&(getMacFromFile()!= null)) {
                            if ((((snPcba != null) && (!snPcba.isEmpty())) || noNeedSn) &&
                                    ((hardware != null) && (!hardware.isEmpty())) &&
                                    ((version != null) && (!version.isEmpty())) &&
                                    ((cpuName != null) && (!cpuName.isEmpty())) &&
                                    ((availRamSize != null) && (!availRamSize.isEmpty())) &&
                                    ((romSize != null) && (!romSize.isEmpty()))
                                    && (SunMi_firmwareVersion != null && !SunMi_firmwareVersion.isEmpty()) /*&& wifi_bluetooth*/
                                    && (SunMi_bootVersion != null && !SunMi_bootVersion.isEmpty())
                                    && (SunMi_cfg_file_version != null && !SunMi_cfg_file_version.isEmpty())
                                    && (gpu_version != null && !gpu_version.isEmpty())
                                    && (!batteryChipVersionStr.isEmpty())
                                    && (!batteryVersionStr.isEmpty())
                                    && (!mLcdFwVersionStr.isEmpty())
                                    && (!mLcdInfoStr.isEmpty())
                                    && (!mISEService_version.isEmpty()&&(mISEService_version!=null)&&(!mISEService_version.equals("")))
                                    && (getMacFromFile().toUpperCase()).startsWith("74:F7:F6")
                                    && getBluetooth().startsWith("74:F7:F6")) {
                                SystemProperties.set(OdmCustomedProp.getSunmifirmwareVersionProp(), SunMi_firmwareVersion);
                                mSoftwareInformationSuccessFlag = true;
                                if (((imei_0 == null) || (imei_0.isEmpty())) ||
                                        ((telMgr.getPhoneCount() == 2) && ((imei_1 == null) || (imei_1.isEmpty())))) {
                                    LogUtil.d("citapk return; imei0:<" + imei_0 + "> imei1:<" + imei_1 + "> telMgr.getPhoneCount():<" + telMgr.getPhoneCount() + ">.");
                                    mSimCardDisplaySuccessFlag = false;
                                    mHandler.sendEmptyMessageDelayed(HANDLER_SIM_DISPLAY, 2000);
                                    return;
                                }
                                mSimCardDisplaySuccessFlag = true;
                                mSuccess.setVisibility(View.VISIBLE);
                            }
                        }
                    break;
                case HANDLER_SOFTWAREVERSION_TEST_RESULT:
                    if(mSimCardDisplaySuccessFlag && mSoftwareInformationSuccessFlag) {
                        if (isMT535 || mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
                            mHandler.sendEmptyMessage(1001);
                        } else {
                            mSuccess.setVisibility(View.VISIBLE);
                        }
                    }else{
                        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                            Message msg1 = mHandler.obtainMessage();
                            msg1.what = HANDLER_SOFTWAREVERSION_TEST_RESULT_FAIL;
                            msg1.obj = "Empty content of software test item";
                            mHandler.sendMessage(msg1);
                            //mHandler.sendEmptyMessage(HANDLER_SOFTWAREVERSION_TEST_RESULT_FAIL);
                        }
                        LogUtil.d(TAG, "mSimCardDisplaySuccessFlag:" + mSimCardDisplaySuccessFlag);
                        LogUtil.d(TAG, "mSoftwareInformationSuccessFlag:" + mSoftwareInformationSuccessFlag);
                    }
                    break;
            }
        }
    };


    @Override
    protected void onDestroy(){
        super.onDestroy();
		mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(HANDLER_SIM_DISPLAY);
        mHandler.removeMessages(HANDLER_SOFTWAREVERSION_TEST_RESULT);
        mHandler.removeMessages(HANDLER_SOFTWAREVERSION_TEST_RESULT_FAIL);
        mHandler.removeMessages(1003);
        if(isMT537_version&&isBindService) {
            unbindService(serviceConnection);
        }
    }

    private class GetSunmiVersion extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
                try{
                    if(MyApplication.getInstance().basicOptV2!=null) {
                        SunMi_firmwareVersion = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.FIRMWARE_VERSION);
                        SunMi_bootVersion = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.BootVersion);
                        SunMi_cfg_file_version = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.CFG_FILE_VERSION);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
        }
    }


    private String getPcbaSn() {
//        String strTmp = "";
        String snPcba = "";
//        String pcbaSn = "";
        String custom_pcbasn = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mCustomPcbasn);
        if(null!=custom_pcbasn && !custom_pcbasn.isEmpty()){
            pcbaSn = SystemProperties.get(custom_pcbasn, "");
        }else{
            pcbaSn = SystemProperties.get(getResources().getString(R.string.version_default_config_pcba_sn), getString(R.string.empty));
            if((null == pcbaSn || pcbaSn.isEmpty())&&(isMT535)){
                pcbaSn = SystemProperties.get("ro.boot.pcbserialno");
            }
        }
        //strTmp = SystemProperties.get(getResources().getString(R.string.version_default_config_serial_no), getString(R.string.empty));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            // Todo Don't forget to ask the permission
            strTmpSn = Build.getSerial();
        }
        else
        {
            strTmpSn = Build.SERIAL;
        }
        LogUtil.d("serialno:" + strTmpSn);
        if ((strTmpSn != null) && !strTmpSn.isEmpty()) {
            LogUtil.d("persist radio no:" + pcbaSn);
            if ((pcbaSn != null) && !pcbaSn.isEmpty())
                snPcba = strTmpSn + "/" + pcbaSn;
            else snPcba = strTmpSn;
        } else {
            if ((pcbaSn != null) && !pcbaSn.isEmpty())
                snPcba = pcbaSn;
            else snPcba = strTmpSn;
        }
        pcbashowmode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mPcbasnshowmode);
        Log.d("pcbasnmode:", pcbashowmode + "");
        if (pcbashowmode.equals("0")) {
            snPcba = pcbaSn;
        }
        return snPcba;
    }

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

    private String getBluetoothAddress() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Field field = bluetoothAdapter.getClass().getDeclaredField("mService");
            // 参数值为true，禁用访问控制检查
            field.setAccessible(true);
            Object bluetoothManagerService = field.get(bluetoothAdapter);
            if (bluetoothManagerService == null) {
                return null;
            }
            Method method = bluetoothManagerService.getClass().getMethod("getAddress");
            Object address = method.invoke(bluetoothManagerService);
            if (address != null && address instanceof String) {
                return (String) address;
            } else {
                return null;
            }

        } catch (IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getBluetooth(){
        String blueToothMAC = null;
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                    .getDefaultAdapter();
            blueToothMAC = bluetoothAdapter.getAddress();
        } catch (Exception e) {
        }
        return blueToothMAC;

    }

    public String getwifiMac() {
        String macSerial = "";
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return macSerial;
    }

    public String getMacID() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        LogUtil.d("zhangshuo","macAddress="+macAddress);
        return macAddress;

    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }
    
    private static final String MACID_FILE_PATH = "/mnt/vendor/wifimac.txt";
    private String getMacFromFile() {
        File file = new File(MACID_FILE_PATH);
        BufferedReader reader = null;
        String macAddress = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                macAddress = line;
                break;
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG , "Mac file not exist", e);
        } catch (Exception e) {
            Log.w(TAG , "get mac from file caught exception", e);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                Log.w(TAG, "reader close exception");
            }
        }
        return macAddress;
    }

    //add by maohaojie on 2019.04.11 for bug 642 start
    public String GetPhoneInfo(int slotId) {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Class clazz = telephonyManager.getClass();
        Method getMeid = null;
        try {
            getMeid = clazz.getDeclaredMethod("getMeid", int.class);//(int slotId)
            system_meid = (String) getMeid.invoke(telephonyManager, slotId); //sim_1
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Throwable t = e.getTargetException();
            t.printStackTrace();
            Log.e("MM0410", "e:" + t.toString());
        }
        return system_meid;
    }
    //add by maohaojie on 2019.04.11 for bug 642 start


    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            //mSuccess.setTextColor(getResources().getColor(R.color.green_1));
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            //mFail.setTextColor(getResources().getColor(R.color.red_800));
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try{
                mISEService = SEServiceInterface.Stub.asInterface(service);
                mISEService_version=mISEService.getVersion();
                mHandler.sendEmptyMessage(1003);
                isBindService =true;
            }catch (Exception e){
                Log.d(TAG,"mISEService_version Exception:"+e.toString());
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

    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            deInit(mFatherName, result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(mFatherName, result);
        }
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

    private String getSubBatteryStatus(String node_path) {

        String Sub_batterStatus = node_path;
        try {
            FileReader file = new FileReader(node_path);
            char[] buffer = new char[1024];
            int len = file.read(buffer, 0, 1024);
            Sub_batterStatus = new String(buffer, 0, len);
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Sub_batterStatus.trim();
    }

}
