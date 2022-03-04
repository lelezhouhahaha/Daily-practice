package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.WifiListAdapter;
import com.meigsmart.meigrs32.adapter.WifiListAdapter_5;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;

public class WifiActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private WifiActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    //@BindView(R.id.wifiMac)
    //public TextView mWifiMac;
    @BindView(R.id.wifi_mac_address)
    public TextView mWifiMac;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.scan)
    public TextView mScan;
    @BindView(R.id.recycleView_5)
    public RecyclerView mRecyclerView_5;
    @BindView(R.id.layout_5)
    public LinearLayout mLayout_5;
    @BindView(R.id.scan_5)
    public TextView mScan_5;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private WifiListAdapter mAdapter;
    private WifiListAdapter_5 mAdapter_5;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    private WifiManager wifimanager = null;
    private static final int DELAY_TIME = 10000;
    private WifiEnableReceiver wifiEnableReceiver = null;
    private WifiScanReceiver wifiScanReceiver = null;

    private List<ScanResult> mList = new ArrayList<>();
    private List<ScanResult> mList_5 = new ArrayList<>();

    private boolean wifi_list = false;
    private boolean wifi_list_5 = false;
    private boolean mCheckMacAddress = true;
    private boolean isMT537_version =false;
    private static final String COMMONCONFIG_WIFI_CHECKMAC = "common_wifi_test_check_wifi_mac_config_bool";
    // is support 5GHz band
    private boolean isSupport5G = true;
    //add for bug 11904 by gongming @2021-05-18
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    private final String TAG = WifiActivity.class.getSimpleName();
    boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
    private boolean isTest = true;
    private boolean isScan = false;
    private boolean wifiScanResultNoEmpty = false;
    private int mScanFailCount = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_wifi;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_wifi);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        //add for bug 11904 by gongming @2021-05-18
        if(is_mt535){
            String macid = getMacID();
            mWifiMac.setText(getResources().getString(R.string.wifi_mac_address)+" "+macid);
        }else {
            mWifiMac.setVisibility(View.GONE);
        }
        //add for bug 11904 by gongming @2021-05-18 end
		if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
        }else{
            mSuccess.setOnClickListener(this);
            mFail.setOnClickListener(this);
        }

        mConfigResult = getResources().getInteger(R.integer.wifi_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        isMT537_version = SystemProperties.get("meig.full.sw.version").contains("MT537")||SystemProperties.get("meig.ap.sw.version").contains("MT537");
        LogUtil.wifi_d(TAG, "mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        isSupport5G = wifimanager.is5GHzBandSupported();
        String check_mac_str = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, COMMONCONFIG_WIFI_CHECKMAC);
        if (TextUtils.isEmpty(check_mac_str)){
            mCheckMacAddress = true;
        } else {
            mCheckMacAddress = check_mac_str.contains("true");
        }

        mAdapter = new WifiListAdapter();
        mAdapter_5 = new WifiListAdapter_5();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView_5.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView_5.setAdapter(mAdapter_5);

        //mHandler.sendEmptyMessageDelayed(1005, getResources().getInteger(R.integer.start_delay_time));
        mFlag.setVisibility(View.GONE);
        mLayout.setVisibility(View.VISIBLE);
        if(isSupport5G) {
            mLayout_5.setVisibility(View.VISIBLE);
        }else{
            mLayout_5.setVisibility(View.GONE);
        }

        startWifi();

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if ( mConfigTime == 0 /*&& mFatherName.equals(MyApplication.RuninTestNAME)*/ ) {
                    Log.d(TAG, "test finished.");
                    mHandler.sendEmptyMessage(1001);
                    mConfigTime = 1;
                    return;
                }else if( mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)){
                    mHandler.sendEmptyMessage(1003);
                    return;
                }
                if (isStartTest) mHandler.sendEmptyMessageDelayed(1006, 3000);
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    wifi_list = mList.size() > 0;
                    wifi_list_5 = mList_5.size() > 0;
                    if(wifi_list || (wifi_list_5 || !isSupport5G) || wifiScanResultNoEmpty) {
                        if (mFatherName.equals(MyApplication.PCBASignalNAME) || mFatherName.equals(MyApplication.PreSignalNAME)) {
                            mSuccess.setVisibility(View.VISIBLE);
                        } else {
                            deInit(mFatherName, SUCCESS);
                        }
                    }else {
						setTestFailReason(getResources().getString(R.string.fail_reason_wifi_data_not_search));
                        if (mFatherName.equals(MyApplication.PCBASignalNAME) || mFatherName.equals(MyApplication.PreSignalNAME)) {
                        } else {
                            deInit(mFatherName, FAILURE, getTestFailReason());
                        }
                    }

                    break;
                case 1002:
                    deInit(mFatherName, FAILURE, Const.RESULT_TIMEOUT);
                    break;
                case 1003:
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1005:

                    //mWifiMac.setText(Html.fromHtml(getResources().getString(R.string.wifi_mac_address)+"&nbsp;"));
                    break;
                case 1006:
                    mAdapter.setData(mList);
                    mAdapter_5.setData(mList_5);
                    WifiInfo info = wifimanager.getConnectionInfo();

                    if (info!=null){
                        LogUtil.wifi_d(TAG, "info = "+info.toString());
                    }
                    if(isMT537_version){
                        if ((info != null) && (!TextUtils.isEmpty(info.getMacAddress()) || !mCheckMacAddress)) {
                            //mWifiMac.setText(Html.fromHtml(getResources().getString(R.string.wifi_mac_address)+"&nbsp;"+getMac()));
                            wifi_list = mList.size() > 0;
                            wifi_list_5 = mList_5.size() > 0;
                        } else {
                            sendErrorMsgDelayed(mHandler, "wifi mac address is null");
                        }
                    }else{
                        if ((info != null) && (!TextUtils.isEmpty(info.getMacAddress()) && !"02:00:00:00:00:00".equals(info.getMacAddress()) || !mCheckMacAddress)) {
                            //mWifiMac.setText(Html.fromHtml(getResources().getString(R.string.wifi_mac_address)+"&nbsp;"+getMac()));
                            wifi_list = mList.size() > 0;
                            wifi_list_5 = mList_5.size() > 0;
                        } else {
                            sendErrorMsgDelayed(mHandler, "wifi mac address is null");
                        }
                    }


                    if(wifi_list && (wifi_list_5 || !isSupport5G)){
                        if (mFatherName.equals(MyApplication.PCBASignalNAME) || mFatherName.equals(MyApplication.PreSignalNAME)) {
                            mSuccess.setVisibility(View.VISIBLE);
                        } else {
                            //if(!mFatherName.equals(MyApplication.RuninTestNAME)){
                                deInit(mFatherName, SUCCESS);
                            //}
                        }
                    }

                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
                case 1101:
                    ExecutorService singleThreadPool= Executors.newSingleThreadExecutor();
                    singleThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                LogUtil.wifi_d(TAG, "RUN ......");
                                // wait until other actions finish.
                                boolean mWifiScanStat = wifimanager.startScan();
                                if(!mWifiScanStat){
                                    boolean mSetWifiDisableState = wifimanager.setWifiEnabled(false);
                                    LogUtil.wifi_d(TAG, "StartScanThread mSetWifiDisableState:" + mSetWifiDisableState);
                                    SystemClock.sleep(1000);
                                    boolean mSetWifiEnableState = wifimanager.setWifiEnabled(true);
                                    LogUtil.wifi_d(TAG, "StartScanThread mSetWifiEnableState:" + mSetWifiEnableState);
                                }

                            } catch (Exception e) {
                                // do nothing
                            }
                        }
                    });
                    singleThreadPool.shutdown();
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.wifi_d(TAG, "onDestroy");
        stopWifi();
        isTest = false;
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
        mHandler.removeMessages(9999);
        mHandler.removeMessages(1101);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, getTestFailReason());
        }
    }

    /**
     * 获取mac地址 wifi
     *
     * @return
     */
    /*public String getMac() {
        String macSerial = "";
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str;) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();
                    break;
                }
            }
        } catch (IOException ex) {
            sendErrorMsgDelayed(mHandler,ex.getMessage());
            ex.printStackTrace();
        }
        return macSerial;
    }*/
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

    private void startWifi() {
        int wifiDefaultState = wifimanager.getWifiState();
        LogUtil.wifi_d(TAG, "wifiDefaultState:" + wifiDefaultState);
        switch (wifiDefaultState) {
            case WifiManager.WIFI_STATE_ENABLED:
                scanDevices();
                if(!isScan) {
                    mHandler.sendEmptyMessage(1101);
                    isScan = true;
                }
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                if (!enableWifi())
                    LogUtil.wifi_d(TAG, "wifi open fail");
                break;
            default:
                if (!enableWifi())
                    LogUtil.wifi_d(TAG, "default wifi open fail");
                break;
        }

    }

    private void restartWifi(){
        stopWifi();
        SystemClock.sleep(1000);
        boolean mSetWifiEnableState = wifimanager.setWifiEnabled(true);
        LogUtil.wifi_d(TAG, "restartWifi mSetWifiEnableState:" + mSetWifiEnableState);
        SystemClock.sleep(1000);
        isScan = false;
        startWifi();
    }


    /**
     * Wifi scan receiver class
     *
     * @author
     * @see BroadcastReceiver
     */
    private class WifiScanReceiver extends BroadcastReceiver {
        @SuppressLint("NewApi")
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.wifi_d(TAG, "WifiScanReceiver onReceive ......");
            // get scan result lisg
            List<ScanResult> wifiScanResultList = wifimanager.getScanResults();
            LogUtil.wifi_d(TAG, "WifiScanReceiver onReceive wifiScanResultList:[" + wifiScanResultList.toString() + "].");
            // check result
            if ((wifiScanResultList != null) && (wifiScanResultList.size() > 0)) {
                for (ScanResult r : wifiScanResultList) {
                    if (r.SSID.isEmpty() && (r.SSID.equals("")))
                        continue;
                    if (!mList.contains(r) && (String.valueOf(r.frequency).startsWith("24")) && (!r.SSID.isEmpty()) && (!r.SSID.equals(""))) {
                        mList.add(r);
                    }
                    if (!mList_5.contains(r) && (String.valueOf(r.frequency).startsWith("5")) && (!r.SSID.isEmpty()) && (!r.SSID.equals(""))) {
                        mList_5.add(r);
                    }
                }
                isStartTest = true;
                wifiScanResultNoEmpty = true;
            } else {
                mScanFailCount++;
                LogUtil.wifi_d(TAG, "WifiScanReceiver onReceive wifi scan failure count:" + mScanFailCount + "].");
                restartWifi();
                //sendErrorMsgDelayed(mHandler, "wifi scan failure");
            }
        }
    }

    /**
     * Wifi enabled receiver class
     *
     * @author
     * @see BroadcastReceiver
     */
    private class WifiEnableReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (wifimanager.getWifiState()) {
                case WifiManager.WIFI_STATE_ENABLED:
                    // enabled wifi ok
                    scanDevices();
                    if(!isScan) {
                        mHandler.sendEmptyMessage(1101);
                        isScan = true;
                    }
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                case WifiManager.WIFI_STATE_DISABLING:
                case WifiManager.WIFI_STATE_UNKNOWN:
                case WifiManager.WIFI_STATE_ENABLING:
                default:
                    // do nothing
            }
        }
    }
    public String getMacID() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        LogUtil.wifi_d(TAG, "macAddress="+macAddress);
        return macAddress;

    }
    /**
     * Create wifi state change receiver and set wifi eanbled
     *
     * @return result of setWifiEnabled
     */
    private boolean enableWifi() {
        if (wifiEnableReceiver == null) {
            wifiEnableReceiver = new WifiEnableReceiver();
            IntentFilter filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
            registerReceiver(wifiEnableReceiver, filter);
            LogUtil.wifi_d(TAG, "enableWifi registerReceiver wifiEnableReceiver");
        }
        // return wifi enabled result
        boolean mWifiEnalbeState = wifimanager.setWifiEnabled(true);
        LogUtil.wifi_d(TAG, "enableWifi mWifiEnalbeState:" + mWifiEnalbeState);
        return mWifiEnalbeState;

    }

    /**
     * Create wifi scan result receiver and start scan
     */
    private void scanDevices() {
        if (wifiScanReceiver == null) {
            wifiScanReceiver = new WifiScanReceiver();
            IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(wifiScanReceiver, filter);
            LogUtil.wifi_d(TAG, "scanDevices registerReceiver wifiScanReceiver");
        }
    }

    private void stopWifi() {
        if (wifimanager.isWifiEnabled() && mFatherName.equals(MyApplication.RuninTestNAME)){
            boolean mSetWifiDisableState = wifimanager.setWifiEnabled(false);
            LogUtil.wifi_d(TAG, "stopWifi mSetWifiDisableState:" + mSetWifiDisableState);
        }
        // release wifi enabled receiver
        if (wifiEnableReceiver != null) {
            unregisterReceiver(wifiEnableReceiver);
            wifiEnableReceiver = null;
            LogUtil.wifi_d(TAG, "stopWifi unregisterReceiver wifiEnableReceiver");
        }

        // release wifi scan receiver
        if (wifiScanReceiver != null) {
            unregisterReceiver(wifiScanReceiver);
            wifiScanReceiver = null;
            LogUtil.wifi_d(TAG, "stopWifi unregisterReceiver wifiScanReceiver");
        }

    }

}
