package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
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
import com.meigsmart.meigrs32.adapter.BluetoothListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class BluetoothActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack  {
    private BluetoothActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.flag)
    public TextView mFlag;
    //@BindView(R.id.btSn)
    //public TextView mBtSn;
    @BindView(R.id.bluetooth_address)
    public TextView mbluetooth_address;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.scan)
    public TextView mScan;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDiscoveryReceiver btDiscoveryReceiver;
    private BlueToothStateReceiver btStateReceiver;
    private List<BluetoothDevice> bluetoothDeviceList = new ArrayList<BluetoothDevice>();;
    private BluetoothListAdapter mAdapter;
    private Runnable mRun;

    private int mConfigResult;
    private int mConfigTime = 0;
    private String mFatherName = "";
    private final String TAG = BluetoothActivity.class.getSimpleName();
    //add for bug 11904 by gongming @2021-05-18
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");

    private int mBtScanFailCount = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_bluetooth;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mScan.setOnClickListener(this);
        mTitle.setText(R.string.pcba_bt);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        //add for bug 11904 by gongming @2021-05-18
        if(is_mt535){
            Handler handler=new Handler();
            Runnable runnable=new Runnable(){
                @Override
                public void run() {
                    mbluetooth_address.setText(getResources().getString(R.string.bluetooth_address) +" "+getBluetooth());
                }
            };
            handler.postDelayed(runnable, 1000);
        }else {
            mbluetooth_address.setVisibility(View.GONE);
        }
        //add for bug 11904 by gongming @2021-05-18 end
		if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
        }else{
            mSuccess.setOnClickListener(this);
            mFail.setOnClickListener(this);
        }

        mConfigResult = getResources().getInteger(R.integer.bt_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.bt_d(TAG, "mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        //mHandler.sendEmptyMessageDelayed(1001,getResources().getInteger(R.integer.start_delay_time));

        mAdapter = new BluetoothListAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                mHandler.sendEmptyMessageDelayed(1004,3000);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME))) {
                        mHandler.sendEmptyMessageDelayed(1002,0);
                }else if(mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)){
                    mHandler.sendEmptyMessage(1010);
                }
                /*if (!mFatherName.equals(MyApplication.RuninTestNAME))
                    mHandler.sendEmptyMessageDelayed(1002,3000);
                 */
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        mFlag.setVisibility(View.GONE);
        mLayout.setVisibility(View.VISIBLE);
        init();
        registerAllReceiver();
        btStart();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1002:
                    mScan.setVisibility(View.GONE);
                    mAdapter.setData(bluetoothDeviceList);
                    if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                        if (bluetoothDeviceList.size()>0 ){
                            deInit(mFatherName, SUCCESS);
                        }else {
//                            mScan.setVisibility(View.VISIBLE);
//                            ToastUtil.showBottomShort(getResources().getString(R.string.bluetooth_no_device));
                        setTestFailReason(getResources().getString(R.string.fail_reason_bluetooth_search_result_empty));
                        deInit(mFatherName, FAILURE, getTestFailReason());
                        }
                    }else {
                        if (bluetoothDeviceList.size() > 0) {
                            mSuccess.setVisibility(View.VISIBLE);
                        }else{
                            setTestFailReason(getResources().getString(R.string.fail_reason_bluetooth_search_result_empty));
                        }
                    }
                    break;
                case 1003:
                    mScan.setVisibility(View.VISIBLE);
                    ToastUtil.showBottomShort(getResources().getString(R.string.bluetooth_no_device));
                    break;
                case 1009:
                    deInit(mFatherName, FAILURE, Const.RESULT_TIMEOUT);
                    break;
                case 1010:
                    deInit(mFatherName, SUCCESS);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
                case 1004:
                    mAdapter.setData(bluetoothDeviceList);
                    if (bluetoothDeviceList.size() > 0)
                        mHandler.sendEmptyMessageDelayed(1002,3000);
                    break;
            }
        }
    };

    private void init(){
        mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            sendErrorMsgDelayed(mHandler,"Unable to initialize BluetoothManager.");
            return ;
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            sendErrorMsgDelayed(mHandler,"Unable to obtain a BluetoothListAdapter.");
            return ;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.bt_d(TAG, "onDestroy");
        unregisterAllReceiver();
        btStop();
        mHandler.removeMessages(1001);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
        mHandler.removeMessages(1009);
        mHandler.removeMessages(1010);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, getTestFailReason());

        }
        if (v == mScan){
            btStart();
        }
    }

    private void btStart(){
        switch(mBluetoothAdapter.getState()){
            case BluetoothAdapter.STATE_ON:
            case BluetoothAdapter.STATE_TURNING_ON:
                btStartDiscovery();
                break;
            case BluetoothAdapter.STATE_OFF:
            case BluetoothAdapter.STATE_TURNING_OFF:
                btEnable();
                break;
        }
    }

    private void btStop() {
        LogUtil.bt_d(TAG, "btStop");
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            if(mBluetoothAdapter.isEnabled()){
                mBluetoothAdapter.disable();
            }
        }
    }

    private void btEnable() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        boolean mBtEnableStat = mBluetoothAdapter.enable();
        LogUtil.bt_d(TAG, "btEnable mBtEnableStat:" + mBtEnableStat);
    }

    private void btDisable() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        boolean mBtDisableStat = mBluetoothAdapter.disable();
        LogUtil.bt_d(TAG, "btDisable mBtDisableStat:" + mBtDisableStat);
    }


    private void restartBt(){
        try {
            btStop();
            btEnable();
            btStart();
        }catch (Exception e){
            LogUtil.bt_d(TAG, "restartBt fail: "+e.toString());
        }
    }

    private void btStartDiscovery() {
        LogUtil.bt_d(TAG, "btStartDiscovery");
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        boolean mBtDiscoveryStat = mBluetoothAdapter.startDiscovery();
        LogUtil.bt_d(TAG, "btStartDiscovery mBtDiscoveryStat:" + mBtDiscoveryStat);
		if(!mBtDiscoveryStat){
            restartBt();
		}
    }

    private void unregisterAllReceiver() {
        LogUtil.bt_d(TAG, "unregisterAllReceiver");
        if (btDiscoveryReceiver != null) {
            unregisterReceiver(btDiscoveryReceiver);
            btDiscoveryReceiver = null;
        }
        if (btStateReceiver != null) {
            unregisterReceiver(btStateReceiver);
            btStateReceiver = null;
        }

    }

    private void registerAllReceiver() {
        LogUtil.bt_d(TAG, "registerAllReceiver");
        // register receiver for bt search
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btDiscoveryReceiver = new BluetoothDiscoveryReceiver();
        registerReceiver(btDiscoveryReceiver, intent);
        // register reveiver for bt state change
        btStateReceiver = new BlueToothStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btStateReceiver, filter);
    }

    private class BluetoothDiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.bt_d(TAG, "BluetoothDiscoveryReceiver action : " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(bluetoothDeviceList.contains(device))
                    return;
                LogUtil.bt_d(TAG, "Search bluetooth device: "+device.getName());
                if (device != null) {
                    bluetoothDeviceList.add(device);
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        synchronized (this) {

                            StringBuffer deviceInfo = new StringBuffer();
                            deviceInfo.append("name:");
                            deviceInfo.append(device.getName());
                            deviceInfo.append("\naddress: ");
                            deviceInfo.append(device.getAddress());
                            deviceInfo.append("\n");
                            LogUtil.bt_d(TAG, deviceInfo.toString());
                        }
                    }
                }
                if (bluetoothDeviceList.size() <= 0) {
                    mBtScanFailCount++;
                    LogUtil.bt_d(TAG, "Search bluetooth result is empty! and search fail number:" + mBtScanFailCount);
                    restartBt();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                LogUtil.bt_d(TAG, "Search bluetooth finished !");
                //btStop();
                btStart();
                //mHandler.sendEmptyMessage(1003);
            }

        }

    }

    private class BlueToothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.bt_d(TAG, "BlueToothStateReceiver:"+mBluetoothAdapter.getState());
            switch (mBluetoothAdapter.getState()) {
                case BluetoothAdapter.STATE_ON:
                    btStartDiscovery();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_OFF:
                    btEnable();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    break;
                default:
                    // do nothing
            }
        }
    }

    /**
     * 获取蓝牙地址
     *
     * @return
     */
    private String getBluetoothAddress() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Field field = bluetoothAdapter.getClass().getDeclaredField("mService");
            // 参数值为true，禁用访问控制检查
            field.setAccessible(true);
            Object bluetoothManagerService = field.get(bluetoothAdapter);
            if (bluetoothManagerService == null) {
                Log.d(TAG, "bluetoothManagerService == null");
                return null;
            }
            Method method = bluetoothManagerService.getClass().getMethod("getAddress");
            Object address = method.invoke(bluetoothManagerService);
            if (address != null && address instanceof String) {
                return (String) address;
            } else {
                Log.d(TAG, "address == null or !(address instanceof String)");
                return null;
            }

        } catch (IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
        return null;
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
