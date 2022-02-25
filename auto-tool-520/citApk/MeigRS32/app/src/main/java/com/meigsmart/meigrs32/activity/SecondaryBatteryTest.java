package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
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
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;

public class SecondaryBatteryTest extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private static final String TAG = "SecondaryBatteryTest";
    private static final String BATTERY_V = "/sys/class/power_supply/battery/sub_voltage_now";
    private static final String BATTERY_CHARGE_STATE = "/sys/class/power_supply/tla2024/sub_battery_chg";
    private static final String TAG_CONFIG = "second_battery_config";
    private static final String TAG_NODE = "sub_battery_status_node";
    private static final String TAG_SECOND_CHARGE_STATE = "sub_battery_charging_node";
    private static final String ENABLE = "1";
    private static final String DISABLE = "0";
    private static final String SECOND_BATTERY_NODE= "second_battery_node";
    private static final String SECOND_STATUS_NODE= "second_status_node";
    private String CIT_BATTERY_PATH = "/sys/class/power_supply/battery";
    private String CIT_BATTERY_STATUS = "/sys/class/power_supply/tla2024/sub_battery_chg";
    float secondbatteryVoltage = 0;

    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.secondary_battery_voltage)
    public TextView mSecondBatteryVoltage;
    @BindView(R.id.secondary_battery_charge_state)
    public TextView mSecondBatteryChargeState;
    @BindView(R.id.secondary_battery_state)
    public TextView mSecondBatteryState;

    @BindView(R.id.below_ll)
    public LinearLayout mBelow;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private SecondaryBatteryTest mContext;
    private int mConfigResult;
    private int mConfigTime = 0;
    private String nodeName = null;
    private String mSecondBatteryStatePath = null;
    private String mSecondBatteryChargeStateStr = null;
    private Runnable mRun;
    private float SbVotage1 = 0;
    private float SbVotage2 = 0;
    private String FORCED_CHARGE_CONFIG_NODE = "common_second_battery_forced_charge_config_bool";
    private boolean mForcedChargeConfig = true;
    private String WAIT_TIME_NODE = "common_second_battery_wait_time_config_int";
    private String SECOND_MODE = "common_second_battery_voltage_change_pass";
    private int mWaitTimeValue = 10;
    private String mValue = "";
    private int mCountWaitTime = 0;
    private boolean mChargingStatus = false;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_secondary_battery_test;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mFail.setVisibility(View.VISIBLE);
        mTitle.setText(R.string.pcba_second_battery_charge);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.second_battery_charge_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        parseXmlFile();

        mValue = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, FORCED_CHARGE_CONFIG_NODE);
        if(mValue != null && !mValue.isEmpty())
            mForcedChargeConfig = mValue.equals("true");
        mValue = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WAIT_TIME_NODE);
        LogUtil.d(" 1 mForcedChargeConfig:" + mForcedChargeConfig);
        if(mValue != null && !mValue.isEmpty())
            mWaitTimeValue = Integer.parseInt(mValue);
                // add  by maohaojie on 2019.03.28 for bug 644
        LogUtil.d(" 1 mWaitTimeValue:" + mWaitTimeValue);
        CIT_BATTERY_PATH = DataUtil.initConfig(Const.CIT_NODE_CONFIG_PATH, SECOND_BATTERY_NODE);
        if(CIT_BATTERY_PATH==null||CIT_BATTERY_PATH.isEmpty()){
            nodeName = BATTERY_V;
        }else{
            nodeName = CIT_BATTERY_PATH;
        }
        LogUtil.d(" 1 nodeName:" + nodeName);

        CIT_BATTERY_STATUS = DataUtil.initConfig(Const.CIT_NODE_CONFIG_PATH, SECOND_STATUS_NODE);
        if(CIT_BATTERY_STATUS==null||CIT_BATTERY_STATUS.isEmpty()){
            mSecondBatteryStatePath = BATTERY_CHARGE_STATE;
        }else{
            mSecondBatteryStatePath = CIT_BATTERY_STATUS;
        }
        LogUtil.d(" 1 mSecondBatteryStatePath:" + mSecondBatteryStatePath);
        String state = getSubBatteryStatus();
        LogUtil.d(" 1 state:" + state);
        String batteryState;
        if(mForcedChargeConfig)
            batterySwap(DISABLE);
        if (getBatteryVoltage(nodeName) <= 2.0) {
            mSecondBatteryChargeStateStr = getResources().getString(R.string.Non_change);
            mSecondBatteryVoltage.setVisibility(View.GONE);
            mSecondBatteryChargeState.setVisibility(View.GONE);
            batteryState = getResources().getString(R.string.NoExist);
        }else{
            SbVotage1 = getBatteryVoltage(nodeName);
            batteryState = getResources().getString(R.string.normal);
            if(mForcedChargeConfig) {
                batterySwap(ENABLE);
                mSecondBatteryChargeStateStr = getResources().getString(R.string.Charging);
                // add by maohaojie on 2018.11.27
                if (state.equals("1")) {
                    setScreenBrightness(mContext, 0);
                }
            }else {
                mSecondBatteryChargeState.setVisibility(View.GONE);
            }
            mHandler.sendEmptyMessageDelayed(1005, 1000);
        }
        mSecondBatteryVoltage.setText(
                Html.fromHtml(
                        getResources().getString(R.string.battery_second_voltage) +"<br/>"+
                                "&nbsp;" + "<font color='#FF0000'>" + getBatteryVoltage(nodeName) + " V" + "</font>"
                ));

        mSecondBatteryChargeState.setText(
                Html.fromHtml(
                        getResources().getString(R.string.battery_second_charge_state) +"<br/>"+
                                "&nbsp;" + "<font color='#FF0000'>" + mSecondBatteryChargeStateStr + "</font>")
        );
        mSecondBatteryState.setText(
                Html.fromHtml(
                        getResources().getString(R.string.battery_second_state) +"<br/>"+
                                "&nbsp;" + "<font color='#FF0000'>"+ batteryState + "</font>")
        );
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                   mHandler.sendEmptyMessage(1002);
                }
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
                    mSuccess.setVisibility(View.VISIBLE);
                    if(mForcedChargeConfig)
                        batterySwap(DISABLE);
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1002:
                    if(mForcedChargeConfig)
                        batterySwap(DISABLE);
                    deInit(mFatherName, FAILURE);
                    break;
                case 1003:
                    mSuccess.setVisibility(View.VISIBLE);
                    break;
                case 1005:
                    LogUtil.d(" mCountWaitTime:" + mCountWaitTime);
                    LogUtil.d(" mWaitTimeValue:" + mWaitTimeValue);
                    if(mCountWaitTime >= 0) {
                        mCountWaitTime++;
                        mHandler.sendEmptyMessage(1006);
                    }
                    if(mCountWaitTime >= mWaitTimeValue) {
                        mHandler.sendEmptyMessage(1006);
                        mHandler.sendEmptyMessageDelayed(1007, 1000);
                    }else mHandler.sendEmptyMessageDelayed(1005, 1000);
                    break;
                case 1006:
                    float batteryVoltage = getBatteryVoltage(nodeName);
                    LogUtil.d(" batteryVoltage:" + batteryVoltage);
                    String batteryState = getResources().getString(R.string.normal);
                    if ((batteryVoltage < 3.0) || (batteryVoltage > 5.0)) {
                        mSecondBatteryVoltage.setVisibility(View.GONE);
                        mSecondBatteryChargeState.setVisibility(View.GONE);
                        batteryState = getResources().getString(R.string.Abnormal);
                    }
                    String state = getSubBatteryStatus();

                    if(CIT_BATTERY_STATUS==null||CIT_BATTERY_STATUS.isEmpty()){
                        if (state.equals("1")) {
                            mSecondBatteryChargeStateStr = getResources().getString(R.string.Charging);
                            mChargingStatus = true;
                        } else {
                            mSecondBatteryChargeStateStr = getResources().getString(R.string.Non_change);
                        }
                    }else{
                        if (state.equals("Not charging")) {
                            mSecondBatteryChargeStateStr = getResources().getString(R.string.Non_change);
                        }else{
                            mSecondBatteryChargeStateStr = getResources().getString(R.string.Charging);
                            mChargingStatus = true;
                        }
                        //mSuccess.setVisibility(View.VISIBLE);
                    }

                    mSecondBatteryVoltage.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_second_voltage) +"<br/>"+
                                            "&nbsp;" + "<font color='#FF0000'>" + batteryVoltage + " V" + "</font>"
                            ));

                    mSecondBatteryChargeState.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_second_charge_state) +"<br/>"+
                                            "&nbsp;" + "<font color='#FF0000'>" + mSecondBatteryChargeStateStr + "</font>")
                    );
                    mSecondBatteryState.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_second_state) +"<br/>"+
                                            "&nbsp;" + "<font color='#FF0000'>" + batteryState + "</font>")
                    );
                    break;
                case 1007:
                    float CurrentBatteryVoltage = getBatteryVoltage(nodeName);
                    LogUtil.d(" 1007 CurrentBatteryVoltage:" + CurrentBatteryVoltage);
                    if ((CurrentBatteryVoltage > 3.0) && (CurrentBatteryVoltage < 5.0)) {
                        // modified by zhaohairuo @ 2019-01-16 for bug23228 start
                        if(SbVotage2 < CurrentBatteryVoltage)
                            SbVotage2 = CurrentBatteryVoltage;
                        String second_mode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, SECOND_MODE);
                        boolean second_voltage_pass = (second_mode.contains("true") && (SbVotage2 != SbVotage1))||(SbVotage2 > SbVotage1);
                        if ((second_voltage_pass) && (mChargingStatus || !mForcedChargeConfig))
                            mHandler.sendEmptyMessage(1001);
                    } else {
                        mSuccess.setVisibility(View.GONE);
                        mFail.setVisibility(View.VISIBLE);
                    }
                    break;
                default:
                    break;

            }
        }
    };


    private float getBatteryVoltage(String fileNode) {
        char[] buffer = new char[1024];

        float batteryVoltage = 0;
        //double time = 0.787;
        FileReader file = null;
        try {
            file = new FileReader(fileNode);
            int len = file.read(buffer, 0, 1024);
            batteryVoltage = Float.valueOf((new String(buffer, 0, len)));
            //batteryVoltage = (float)((double)batteryVoltage / time);
            if (file != null) {
                file.close();
                file = null;
            }
        } catch (Exception e) {
            try {
                if (file != null) {
                    file.close();
                    file = null;
                }
            } catch (IOException io) {
                LogUtil.e("getBatteryElectronic fail");
            }
        }
        if((batteryVoltage / 1000)>1000){

            secondbatteryVoltage=batteryVoltage / 1000000;
        }else{
            secondbatteryVoltage=batteryVoltage/1000;
        }

        return secondbatteryVoltage;
    }

    private void parseXmlFile() {
        File configFile = new File(Const.CIT_NODE_CONFIG_PATH);
        if (!configFile.exists()) {
            LogUtil.e(TAG, " xml not found");
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.KEYTEST_CONFIG_XML_PATH));
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(Const.CIT_NODE_CONFIG_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if (TAG_NODE.equals(startTagName)) {
                            nodeName = xmlPullParser.nextText();
                            if (nodeName != null) {
                                LogUtil.e(TAG, "nodeName = " + nodeName);
                            }
                        } else if (TAG_SECOND_CHARGE_STATE.equals(startTagName)) {
                            mSecondBatteryStatePath = xmlPullParser.nextText();
                            if (mSecondBatteryStatePath != null) {
                                LogUtil.e(TAG, "mSecondBatteryStatePath = " + mSecondBatteryStatePath);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String endTagName = xmlPullParser.getName();
                        if (TAG_CONFIG.equals(endTagName)) {
                            break;
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "====>error = " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
        mHandler.removeMessages(1007);
        mHandler.removeMessages(9999);
        if(mForcedChargeConfig)
            batterySwap(DISABLE);
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
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

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

    // add by maohaojie on 2018.11.27
    public static void setScreenBrightness(Activity activity, int value) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = value / 255f;
        activity.getWindow().setAttributes(params);
    }

    private void batterySwap(String battery) {
        try {
            FileOutputStream fSwitchBattery = new FileOutputStream(mSecondBatteryStatePath);
            fSwitchBattery.write(battery.getBytes());
            fSwitchBattery.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getSubBatteryStatus() {

        String Sub_batterStatus = mSecondBatteryStatePath;
        try {
            FileReader file = new FileReader(mSecondBatteryStatePath);
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
