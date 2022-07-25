package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.BatteryVolume;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;

public class BatterySwitchActivity extends BaseActivity implements View.OnClickListener {

    private BatterySwitchActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.message)
    public TextView mMessage;
    @BindView(R.id.next)
    public Button mNext;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.below_ll)
    public LinearLayout mBelow;
    @BindView(R.id.message_debug)
    public TextView mMessage_debug;
    @BindView(R.id.message_debug2)
    public TextView mMessage_debug2;

    private String mFatherName = "";

    private int mConfigResult;
    private int mConfigTime = 0;


    private final String TAG = "BatterySwitchActivity";
    private final static String  BATTERY_CURRENT_NODE ="/sys/class/power_supply/battery/current_now";
    private final static String  BATTERY_VOLTAGE_NODE ="/sys/class/power_supply/battery/voltage_now";

    private String switch_battery_node = "";
    private String primary_battery_status_node = "";
    private String sub_battery_status_node = "";
    private String sub_battery_charg_status_node = "";
    private String primary_battery_node = "";
    private String mSecondBatteryState = "";


    private boolean switch_action = false;
    private Boolean mCurrentTestResult = false;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_battery_switch;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.pcba_battery_switch);

        mNext.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        mConfigResult = getResources().getInteger(R.integer.battery_switch_default_config_standard_result);

        addData(mFatherName,super.mName);
        Log.d(TAG,"mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mHandler.sendEmptyMessage(1001);

        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time)*6;
            mRun = new Runnable() {
                @Override
                public void run() {
                    mConfigTime--;
                    LogUtil.d(TAG, "initData mConfigTime:" + mConfigTime);
                    updateFloatView(mContext, mConfigTime);
                    if ((mConfigTime == 0) && (mFatherName.equals(MyApplication.PCBAAutoTestNAME))) {
                        if (mCurrentTestResult) {
                            deInit(mFatherName, SUCCESS);
                        } else {
                            LogUtil.d(TAG, " Test fail!");
                            deInit(mFatherName, FAILURE, " Test fail!");
                        }
                        return;
                    }
                    mHandler.postDelayed(this, 1000);
                }
            };
            mRun.run();
        }

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    isStartTest = true;
                    mNext.setEnabled(false);
                    mFlag.setVisibility(View.GONE);
                    mMessage.setVisibility(View.VISIBLE);
                    mMessage_debug.setVisibility(View.VISIBLE);
                    mMessage_debug2.setVisibility(View.VISIBLE);
                    mBelow.setVisibility(View.VISIBLE);
                    //mMessage.setText(R.string.sunmi_message_switch_battery);
                    if(!("userdebug".equals(SystemProperties.get("ro.build.type")))){
                        mMessage_debug.setText(R.string.sunmi_message_switch_battery);
                    }
                    /*if(isNoChargeSubBattery){
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                        registerReceiver(mBroadcastReceiver, filter);
                        mMessage.setText(getString(R.string.message_switch_battery));
                        mSuccess.setVisibility(View.GONE);
                        mNext.setVisibility(View.VISIBLE);
                    }else {*/
                        mNext.setVisibility(View.VISIBLE);
                        mBelow.setVisibility(View.VISIBLE);
                    //}
                    mHandler.sendEmptyMessage(1002);
                    break;
                case 1002:
                    if(mFatherName.equals(MyApplication.PCBANAME)||mFatherName.equals(MyApplication.PCBASignalNAME)){
                        PcbacheckUnplug();
                    }else {
                        checkUnplug();
                    }
                    mHandler.sendEmptyMessageDelayed(1002,500);
                    break;
                case 1003:
                    mMessage_debug.setVisibility(View.INVISIBLE);
                    mMessage_debug2.setVisibility(View.INVISIBLE);
                    mMessage.setText(getBatteryStatus("/sys/class/power_supply/sub_bat/voltage_now"));
                    mCurrentTestResult = true;
                    mSuccess.setVisibility(View.VISIBLE);
                    mNext.setVisibility(View.INVISIBLE);
                    if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                        if(mCurrentTestResult){
                            deInit(mFatherName, SUCCESS);
                        }else deInit(mFatherName, FAILURE, "Battery switch test fail!");
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
		if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
        	mHandler.removeCallbacks(mRun);
		}
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
        /*if(isNoChargeSubBattery && mBroadcastReceiver!= null){
            unregisterReceiver(mBroadcastReceiver);
        }
		// add by maohaojie on 2018.12.17 for bug 20503
        setAirplaneMode(BatterySwitchActivity.this,false);*/

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                /*if (mMessage.getText().equals(getString(R.string.message_unplug_battery)))
                    checkUnplug();
                else
                    checkInsert();*/
                /*mMessage_debug.setVisibility(View.INVISIBLE);
                mMessage_debug2.setVisibility(View.INVISIBLE);
                mMessage.setText(getBatteryStatus("/sys/class/power_supply/sub_bat/voltage_now"));
                mCurrentTestResult = true;
                mSuccess.setVisibility(View.VISIBLE);
                mNext.setVisibility(View.INVISIBLE);*/
                mHandler.sendEmptyMessage(1003);
                break;
            case R.id.success:
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                deInit(mFatherName, SUCCESS);
                break;
            case R.id.fail:
                mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                break;
        }
    }

    private void checkUnplug() {
        String current = "";
        try {
            int line;
            FileInputStream fis = new FileInputStream(BATTERY_CURRENT_NODE);
            DataInputStream dis = new DataInputStream(fis);
            StringBuffer sb = new StringBuffer();
            byte b[] = new byte[1];
            while ((line = dis.read(b)) != -1) {
                String mData = new String(b, 0, line);
                sb.append(mData);
                current = sb.toString();
            }
            dis.close();
            fis.close();
            Log.d(TAG,"battery current: "+current);
            switch_action = false;
        } catch (Exception e) {
            Log.d(TAG,"battery current Exception: "+current);
            switch_action = true;
            mNext.setEnabled(true);
            mHandler.removeMessages(1002);
            if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                mHandler.sendEmptyMessage(1003);
            }
        }
    }

    private void PcbacheckUnplug() {
        String current = "";
        try {
            int line;
            FileInputStream fis = new FileInputStream(BATTERY_VOLTAGE_NODE);
            DataInputStream dis = new DataInputStream(fis);
            StringBuffer sb = new StringBuffer();
            byte b[] = new byte[1];
            while ((line = dis.read(b)) != -1) {
                String mData = new String(b, 0, line);
                sb.append(mData);
                current = sb.toString();
            }
            dis.close();
            fis.close();
            int newVoltage = Integer.parseInt(current.trim());
            Log.d(TAG,"battery current voltage: "+newVoltage);
            if(newVoltage < 3900000) {
                switch_action = true;
                mNext.setEnabled(true);
                mHandler.removeMessages(1002);
            }
        } catch (Exception e) {
            Log.d(TAG,"battery voltage Exception: "+current+"  "+e.toString());

        }
    }




    private String getBatteryStatus(String battery_status_node) {
        Float voltage = 0f;

        FileReader file = null;
        char[] buffer = new char[1024];
        try {
            file = new FileReader(battery_status_node);
            int len = file.read(buffer, 0, 1024);
            voltage = Float.valueOf((new String(buffer, 0, len)));
            file.close();
        } catch (Exception e) {
            LogUtil.e("Get battery status node : " + battery_status_node + "fail.");
        }

        String str = "";

        str = getString(R.string.sub_battery_voltage, voltage / 1000 / 1000);
        str += "\n";

        return String.format(str);
    }

}
