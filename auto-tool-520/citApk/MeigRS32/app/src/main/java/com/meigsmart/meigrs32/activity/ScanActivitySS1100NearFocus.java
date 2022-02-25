package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
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
import com.meigsmart.meigrs32.model.WifiConnector;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class ScanActivitySS1100NearFocus extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private ScanActivitySS1100NearFocus mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.scantip)
    public TextView mScanTipInfo;
    //@BindView(R.id.info)
    //public TextView mScanInfo;
    private String mFatherName = "";
    private  String mPackageName = "";
    private String mClassName = "";
    private String defaultCompareValue = "";

    private int mConfigTime = 0;
    private Runnable mRun;
    private static final String KEY_FARFOCUS_SCAN = "common_ScanActivityFarFocus_default_compare_value";
    private final String KEY_WIFI_HOTSPOT = "common_ScanActivitySS1100_wifi_hotspot_value";
    private final String KEY_WIFI_PASSWORD = "common_ScanActivitySS1100_wifi_password_value";
    private String scanType = "";
    private String buildType = "";
    private String gWifiHotspot = "";
    private String gWifiPassword = "";
    private Boolean mActiveDecodeFlag = false;
    private Boolean mScanFlag = false;
    private Boolean mAcitiveDecodeAction = false;
    private Boolean mScanAction = false;
    private Boolean gWifiEnable = false;
    private WifiManager mWifiManager = null;
    private ConnectivityManager mNetworkConnectManager = null;
    private WifiConnector mWifiConnector = null;
    private static final  int HANDLER_START_SCAN_CODE = 1010;
    private static final  int HANDLER_SS1100_RESULT_CODE = 1011;
    private static final  int HANDLER_START_CODE = 1020;
    private static final  int HANDLER_SS1100_RESULT_SUCCESS_CODE = 1015;
    private static final  int HANDLER_SS1100_RESULT_FAIL_CODE = 1016;
    private final String TAG = ScanActivitySS1100NearFocus.class.getSimpleName();
    private String SS1100_NEAR_COMPARE_VALUE = "https://cli.im/text/小";
    private String mProjectName;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_scan_ss1100;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mProjectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)){
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
        }else {
            mSuccess.setOnClickListener(this);
            //mSuccess.setVisibility(View.VISIBLE);
            mSuccess.setVisibility(View.GONE);
            mFail.setOnClickListener(this);
        }
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mScanTipInfo.setVisibility(View.GONE);
        }
        buildType = SystemProperties.get("ro.build.type", "unknown");
        LogUtil.d("PreFunctionActivity buildType:" + buildType);

        mTitle.setText(R.string.ScanActivitySS1100NearFocus);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        defaultCompareValue = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, KEY_FARFOCUS_SCAN);
        if( (defaultCompareValue == null) || defaultCompareValue.isEmpty()){
            defaultCompareValue = "123456789";
        }
        gWifiHotspot = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, KEY_WIFI_HOTSPOT);
        gWifiPassword = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, KEY_WIFI_PASSWORD);
        Log.d(TAG, "gWifiHotspot:<" + gWifiHotspot +">.");
        Log.d(TAG, "gWifiPassword:<" + gWifiPassword +">.");


        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mNetworkConnectManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiConnector = new WifiConnector(mWifiManager);
        if(mFatherName.equals(MyApplication.RuninTestNAME)){
            startActivityForComponentName("com.sunmi.scanaging", "com.sunmi.scanaging.activity.MainActivity");
        }else {
            if (!isNetSystemUsable()) {
                if (gWifiHotspot.equals("")) {
                    Log.d(TAG, "no Wifi Hotspot!");
                } else if (gWifiPassword.equals("")) {
                    Log.d(TAG, "WIFICIPHER_NOPASS connect!");
                    mWifiConnector.connect(gWifiHotspot, null, WifiConnector.WifiCipherType.WIFICIPHER_NOPASS);
                } else {
                    Log.d(TAG, "WIFICIPHER_WPA connect!");
                    mWifiConnector.connect(gWifiHotspot, gWifiPassword, WifiConnector.WifiCipherType.WIFICIPHER_WPA);
                }
            } else {
                gWifiEnable = true;
                mHandler.sendEmptyMessage(HANDLER_START_CODE);
            }
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    //mHandler.sendEmptyMessage(HANDLER_SS1100_RESULT_CODE);
					mHandler.sendEmptyMessage(HANDLER_SS1100_RESULT_SUCCESS_CODE);
                }
                if ((!mFatherName.equals(MyApplication.RuninTestNAME)) && (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) && (isNetSystemUsable()) && (!gWifiEnable)) {
                    gWifiEnable = true;
                    mHandler.sendEmptyMessage(HANDLER_START_CODE);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        Toast.makeText(mContext, getResources().getString(R.string.ss1100Scaninfo), Toast.LENGTH_SHORT)
                .show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d(" citapk ScanActivitySS1100 resultCode: <" + resultCode + ">.");
        LogUtil.d(" citapk ScanActivitySS1100 requestCode: <" + requestCode + ">.");

        if(data == null) {
            LogUtil.d(" onActivityResult data == null");
            return;
        }

        if(mFatherName.equals(MyApplication.RuninTestNAME)){
            mHandler.sendEmptyMessage(HANDLER_SS1100_RESULT_SUCCESS_CODE);
            return;
        }

        LogUtil.d(" onActivityResult defaultCompareValue:" + defaultCompareValue);
        String result = data.getStringExtra("results");
        LogUtil.d(" onActivityResult result:" + result);
        if(result == null){
            LogUtil.d(" onActivityResult result == null");
            return;
        }

        if(mProjectName.equals("MC520_GMS") && !buildType.equals("userdebug")){
            if(result.equals(SS1100_NEAR_COMPARE_VALUE)){//if(result.equals("scan:true")) {
                mScanFlag = true;
                LogUtil.d(" onActivityResult scantype SS1100 active decode sucess");
                //mHandler.sendEmptyMessage(HANDLER_SS1100_RESULT_CODE);
                deInit(mFatherName, SUCCESS);
            }else{
                String fileReason = "Scan fail result:" + result;
                deInit(mFatherName, FAILURE, fileReason);
            }
			return;
        }

        if(result.equals("fail")){
            setTestFailReason(result);
            Log.d(TAG, "fail reason:" + result);
        }

        if(result.equals("active decode:true")) {
            mActiveDecodeFlag = true;
            LogUtil.d(" onActivityResult scantype SS1100 active decode sucess");
            mHandler.sendEmptyMessage(HANDLER_START_SCAN_CODE);
        }else if(result.equals(SS1100_NEAR_COMPARE_VALUE)){//if(result.equals("scan:true")) {
            mScanFlag = true;
            LogUtil.d(" onActivityResult scantype SS1100 active decode sucess");
            mHandler.sendEmptyMessage(HANDLER_SS1100_RESULT_CODE);
        }else{
            mHandler.sendEmptyMessage(HANDLER_SS1100_RESULT_CODE);
        }
        return;
    }

    private boolean isNetSystemUsable() {
        boolean isNetUsable = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                NetworkCapabilities networkCapabilities =
                        mNetworkConnectManager.getNetworkCapabilities(mNetworkConnectManager.getActiveNetwork());
                isNetUsable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY);
                Log.d(TAG, " isNetUsable:" + isNetUsable);
                Log.d(TAG, " networkCapabilities:" + networkCapabilities);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isNetUsable;
    }
	
	 protected void startActivityForComponentName(String packageName, String className){
        LogUtil.d("citapk startActivityForComponentName packageName:" + packageName);
        LogUtil.d("citapk startActivityForComponentName className:" + className);
        ComponentName componentName = new ComponentName(packageName, className);
        Intent intent = new Intent();
        intent.setComponent(componentName);

        intent.putExtra("fatherName", mFatherName);
        intent.putExtra("name", super.mName);
        intent.putExtra("requestCode", 1000);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            intent.putExtra("ScanStartType", "auto");
            LogUtil.d("citapk L2S  ScanStartType: auto");
        }
        startActivityForResult(intent, 1000);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_START_CODE:
                    if(mProjectName.equals("MC520_GMS") && !buildType.equals("userdebug")){
                        startActivityForComponentName("com.sunmi.scannercit", "com.sunmi.scannercit.ScanActivity");
                    }else {
                        if (!mFatherName.equals(MyApplication.RuninTestNAME)) {
                            mScanTipInfo.setVisibility(View.GONE);
                            if (buildType.equals("userdebug")) {
                                startActivityForComponentName("com.sunmi.scannercitmmi1", "com.sunmi.scannercitmmi1.activities.ActiveActivity");
                            } else {
                                startActivityForComponentName("com.sunmi.scannercitmmi2", "com.sunmi.scannercitmmi2.activities.ActiveActivity");
                            }
                        }
                    }
                    break;
				case HANDLER_START_SCAN_CODE:
                    LogUtil.d("handler HANDLER_START_SCAN_CODE");
                    if(buildType.equals("userdebug")) {
                        startActivityForComponentName("com.sunmi.scannercitmmi1", "com.sunmi.scannercitmmi1.activities.ScanActivity");
                    }else{
                        startActivityForComponentName("com.sunmi.scannercitmmi2", "com.sunmi.scannercitmmi2.activities.ScanActivity");
                    }

                    mScanAction = true;
                    break;
                case HANDLER_SS1100_RESULT_CODE:
                    LogUtil.d("handler HANDLER_SS1100_RESULT_CODE");
                    if(mFatherName.equals(MyApplication.RuninTestNAME)) {
                        mSuccess.setVisibility(View.VISIBLE);
                        deInit(mFatherName, SUCCESS);
                        break;
                    }
                    if(mActiveDecodeFlag && mScanFlag){
                        mSuccess.setVisibility(View.VISIBLE);
                        deInit(mFatherName, SUCCESS);
                    }else if(!mActiveDecodeFlag){
                        deInit(mFatherName, FAILURE, "SS1100 active decode fail.");
                    }else if(!mScanFlag){
                        deInit(mFatherName, FAILURE, "SS1100 scan fail.");
                    }
                    break;
                case HANDLER_SS1100_RESULT_SUCCESS_CODE:
                    deInit(mFatherName, SUCCESS);
                    break;
                case HANDLER_SS1100_RESULT_FAIL_CODE:
                    deInit(mFatherName, FAILURE, getTestFailReason());
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(HANDLER_START_CODE);
        mHandler.removeMessages(HANDLER_START_SCAN_CODE);
        mHandler.removeMessages(HANDLER_SS1100_RESULT_CODE);
        mHandler.removeMessages(HANDLER_SS1100_RESULT_SUCCESS_CODE);
        mHandler.removeMessages(HANDLER_SS1100_RESULT_FAIL_CODE);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }

        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }

    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }
}
