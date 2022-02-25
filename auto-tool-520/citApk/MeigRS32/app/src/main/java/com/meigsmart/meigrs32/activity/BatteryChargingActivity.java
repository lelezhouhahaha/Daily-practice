package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.service.MusicService;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;

import static com.meigsmart.meigrs32.util.DataUtil.initConfig;
import static com.meigsmart.meigrs32.util.DataUtil.readLineFromFile;

public class BatteryChargingActivity extends BaseActivity {

    private BatteryChargingActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;

    @BindView(R.id.status)
    public TextView mStatus;//充电状态
    @BindView(R.id.quantity)
    public TextView mQuantity;//充电电量


    private static final String TAG_CAPACITY_NODE = "capacity_node";
    private static final String TAG_CHANGER_STATUS_NODE = "changer_status_node";

    //Charging current on Qualcomm platform is negative, SPRD platform is positive
    private static final String TAG_COMMON_CURRENT_ABSOLUTE_VALUE_ENABLE = "common_current_absolute_value_enable";
    private String mCurrentAbsoluteEnabled = "1";
    private Boolean mOperationChargingAction = false;
    private Boolean mOperationCharging2Action = false;
    private Boolean mOperationChargingCompleteAction = false;
    private Boolean mOperationDischargingAction = false;
    private int mBatteryStatus = 0;
    private Boolean mUsbInsertStatus = false;

    private String capacity_node = "";
    private String mChangerStatusNodePath = "";
    private static final String STATUS = "status";
    //int Quantity = 0;

    private int mConfigTime = 0;
    private int mCurrentThresholdValue = 0;
    private int charingStatus = 0;
    private static final int MSG_DEINIT = 8888;
    private final String TAG_CHARGING_TYPE_NODE = "dc_charging_type_node";
    private Runnable mRun;
    private String projectName = "";
    private int minVoltage = 78;

    private AudioManager mAudioManger = null;
    private Intent intentMusic = null;
    private MusicService musicService = null;
    private int mCurrentVolume = 0;
    private static boolean isFirstEnter = true;
    private int mCurrentCapacityValue = 0;
    private static final String PLUGGED = "plugged";
    private Boolean mUsbPluggedChangedStatus = false;
    private int mUsbPluggedChangedSave = 0;
    private final String TAG = BatteryChargingActivity.class.getSimpleName();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_battery_charging;
    }
/*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                    if(Quantity <= 78){
                        return true;
                    }
        }
        return super.onKeyDown(keyCode, event);
    }
 */

    private boolean isUSBConnected(){
        String type_node = initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_CHARGING_TYPE_NODE);
        if ((type_node != null) && !type_node.isEmpty()) {
            String type = readLineFromFile(type_node);
            return type != null && !type.contains("0");
        }
        return false;
    }

    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.BatteryChargingActivity);
        super.startBlockKeys = false;

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        String currentAbsoluteConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_COMMON_CURRENT_ABSOLUTE_VALUE_ENABLE);
        if(currentAbsoluteConfig != null && !currentAbsoluteConfig.isEmpty()){
            mCurrentAbsoluteEnabled = currentAbsoluteConfig;
        }

        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        if("MT537".equals(projectName)){
            minVoltage = 60;
            mAudioManger = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        initNode();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        mHandler.sendEmptyMessageDelayed(1001, getResources().getInteger(R.integer.start_delay_time));
    }

    private void initNode() {

        File nodeFile = new File(Const.CIT_NODE_CONFIG_PATH);
        if (!nodeFile.exists()) {
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.CIT_NODE_CONFIG_PATH));
            finish();
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(nodeFile);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    String startTagName = xmlPullParser.getName();
                    if (TAG_CAPACITY_NODE.equals(startTagName)) {
                        capacity_node = xmlPullParser.nextText();
                    }else if (TAG_CHANGER_STATUS_NODE.equals(startTagName)) {
                        mChangerStatusNodePath = xmlPullParser.nextText();
                    }
                }
                type = xmlPullParser.next();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String emptyNode = capacity_node.isEmpty() ? " " + TAG_CAPACITY_NODE : "";

        if (!emptyNode.isEmpty()) {
            ToastUtil.showCenterLong(getString(R.string.node_not_found, emptyNode, Const.CIT_NODE_CONFIG_PATH));
            finish();
            return;
        }

    }

    private float getCurrentNow(String node) {

        Float currentNow = 0f;

        try {
            char[] buffer = new char[1024];

            FileReader fileReader = new FileReader(node);
            int len = fileReader.read(buffer, 0, buffer.length);
            String data = new String(buffer, 0, len);
            currentNow = Float.valueOf(data);

            fileReader.close();
        } catch (Exception e) {
            LogUtil.e("Get current now node : " + node + "fail.");
        }

        return currentNow;
    }

    private void startAudioService(){
        mCurrentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVoluem = mAudioManger.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        LogUtil.d(TAG, "citapk maxVoluem:" + maxVoluem);
        mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, maxVoluem, 0);
        intentMusic = new Intent(this, MusicService.class);
        startService(intentMusic);
        bindAudioService();
    }

    private void bindAudioService(){
        bindService(intentMusic, serviceConnection, BIND_AUTO_CREATE);
    }
    private void unbindAudioService(){
        if (musicService!=null){
            musicService.stop();
            unbindService(serviceConnection);
        }
    }

    private void stopAudioService(){
        if(mCurrentVolume != 0) {
            mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
            int currentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        unbindAudioService();
        if (intentMusic!=null) {
            stopService(intentMusic);
        }
        musicService = null;
    }

    private void addAudioControl(int level){
            if (level < 75) {
                if (musicService != null) {
                    LogUtil.d(TAG, "1 stopAudioService");
                    stopAudioService();
                }else {
                    LogUtil.d(TAG, "1 else AudioService");
                }
            } else/* if (level >= 80)*/ {
                if (musicService == null || musicService.mediaPlayer == null) {
                    LogUtil.d(TAG, "startAudioService");
                    startAudioService();
                }else {
                    LogUtil.d(TAG, "else AudioService");
                }
            }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.d(TAG, "onServiceConnected");
            musicService = ((MusicService.MyBinder) (service)).getService(false/*isAudioCustomPath*/,/*mAudioCustomFilePath*/null);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.d(TAG, "onServiceDisconnected");
            musicService = null;
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    isStartTest = true;
                    mFlag.setVisibility(View.GONE);
                    //Quantity = (int) getCurrentNow(capacity_node);
                    mQuantity.setText(String.format(getString(R.string.quantity), mCurrentCapacityValue));

                    if( mCurrentCapacityValue > minVoltage && mCurrentCapacityValue < 80 ){
                        mBatteryStatus = 0;
                        mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(0)));   //charging by usb
                        if(projectName.equals("MT537")){
                            if(mCurrentCapacityValue >= 75 && mCurrentCapacityValue < 80){
                                if(!mOperationCharging2Action) {
                                    FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend", "1");
                                    FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled", "0");
                                    mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(3)));   //charging by usb
                                    mOperationChargingAction = false;
                                    mOperationChargingCompleteAction = false;
                                    mOperationCharging2Action = true;
                                    mOperationDischargingAction = false;
                                }
                            }else{
                                if(!mOperationChargingCompleteAction) {
                                    FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend", "0");
                                    FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled", "0");
                                    mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(0)));   //charging by usb
                                    mOperationChargingAction = false;
                                    mOperationChargingCompleteAction = true;
                                    mOperationCharging2Action = false;
                                    mOperationDischargingAction = false;
                                }
                            }
                        }
                        if(!mOperationChargingCompleteAction) {
                            //charging complete   usb  power supply
                            FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend", "0");
                            FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled", "0");
                            //mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(0)));
							mOperationCharging2Action = false;
                            mOperationChargingCompleteAction = true;
                            mOperationChargingAction = false;
                            mOperationDischargingAction = false;
                        }
                    }else if( mCurrentCapacityValue <= minVoltage ) {
                        mBatteryStatus = 1;
                        mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(charingStatus)));   //charging by usb
                        if(!mOperationChargingAction) {
                            //charging      usb power supply for battery
                            FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend", "0");
                            FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled", "1");
                            //mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(2)));
                            mOperationChargingAction = true;
                            mOperationChargingCompleteAction = false;
                            mOperationCharging2Action = false;
                            mOperationDischargingAction = false;
                        }
                    }else if( mCurrentCapacityValue >= 80 ){
                        mBatteryStatus = 2;
                        mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(3)));   //charging by usb
                        if(!mOperationDischargingAction) {
                            //discharge             battery power supply
                            FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend", "1");
                            FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled", "0");
                            //mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(3)));
                            mOperationDischargingAction = true;
                            mOperationChargingCompleteAction = false;
                            mOperationChargingAction = false;
                            mOperationCharging2Action = false;
                        }
                    }
                    LogUtil.d(" writeToFile mBatteryStatus:<" + mBatteryStatus + ">.");
                    LogUtil.d(" writeToFile charingStatus:<" + charingStatus + ">.");
                    LogUtil.d(" writeToFile mOperationDischargingAction:<" + mOperationDischargingAction + ">.");
                    LogUtil.d(" writeToFile mOperationChargingCompleteAction:<" + mOperationChargingCompleteAction + ">.");
                    LogUtil.d(" writeToFile mOperationChargingAction:<" + mOperationChargingAction + ">.");
                    LogUtil.d(" writeToFile FileUtil.readFile(\"sys/class/power_supply/battery/input_suspend\"):<" + FileUtil.readFile("sys/class/power_supply/battery/input_suspend") + ">.");
                    LogUtil.d(" writeToFile FileUtil.readFile(\"sys/class/power_supply/battery/battery_charging_enabled\"):<" + FileUtil.readFile("sys/class/power_supply/battery/battery_charging_enabled") + ">.");

                    mHandler.sendEmptyMessageDelayed(1001, getResources().getInteger(R.integer.loop_delay_time));
                    break;
                case MSG_DEINIT:
                    if (mFatherName.equals(MyApplication.RuninTestNAME)) {
                        deInit(mFatherName,  SUCCESS );
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
	if(projectName.equals("MT537")) {
            addAudioControl(0); //close audio service
        }
        FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend", "0");
        FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled", "1");
        unregisterReceiver(mBroadcastReceiver);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(9999);
        mHandler.removeMessages(MSG_DEINIT);
        finish();
    }

    private String getChangeStatusString(int status) {
        String statusString = "";
        switch (status) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                //statusString = "UNKNOWN";
                statusString = mContext.getString(R.string.stat_unknown);
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                //statusString = "CHARGING";
                statusString = mContext.getString(R.string.Charging);
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                //statusString = "DISCHARGING";
                statusString = mContext.getString(R.string.Discharging);
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                //statusString = "NOT_CHARGING";
                statusString = mContext.getString(R.string.Non_change);
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                //statusString = "FULL";
                statusString = mContext.getString(R.string.Full);
                break;
            default:
                statusString = mContext.getString(R.string.Battery_Charging_Complete);
                break;
        }
        return statusString;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                charingStatus = intent.getIntExtra(STATUS, 0);
                int level = intent.getIntExtra("level", -1);
                mCurrentCapacityValue = level;
                LogUtil.d("citapk charingStatus:" + charingStatus);
                if(projectName.equals("MT537")){
                    addAudioControl(level);
                }
            }
        }
    };
}
