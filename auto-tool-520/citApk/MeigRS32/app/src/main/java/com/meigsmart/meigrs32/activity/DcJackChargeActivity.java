package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.util.UTF8Decoder;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;

public class DcJackChargeActivity extends BaseActivity implements View.OnClickListener{
    private DcJackChargeActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.charge_off)
    public TextView mChargeOff;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    private String mChangeStatus = "";
    private static String mDcJactChargeNode = "";
    private String mChangerStatusNodePath = "";
    private static final String TAG_DC_JACT_CHARGE_NODE = "dc_jact_charge_node";
    private static final String TAG_CHANGER_STATUS_NODE = "changer_status_node";

    private static final String Bot_connect_change_state_flag = "common_bot_connect_state_flag";
    private static final String Bot_connect_state = "common_bot_connect_state_path";
    private boolean botflag = true;
    private String botstate = "";


    private static final int MSG_DISABLE_CHARGING = 1100;
    private static final int MSG_ENABLE_CHARGING = 1101;
    private static final int MSG_DEINIT = 8888;

    private boolean hasDisconnected = false;
    private boolean hasConnected = false;

    private BatteryManager mBatteryManager;
    private static final int BATTERY_PROPERTY_STATUS = 6;

    private int mConfigTime = 0;
    private Runnable mRun;
    private Boolean mIsRuning = true;

    private String DC_ONLINE_NODE = "/sys/class/power_supply/dc/online";
    private String TAG_DEVICE_NAME = "common_device_name_test";
    private boolean isSLB786 = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_dc_jact_charge;
    }

    @Override
    protected void initData() {
        mContext = this;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        //mSuccess.setVisibility(View.VISIBLE);
        mTitle.setText(R.string.pcba_dc_battery_charge);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        isSLB786 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_DEVICE_NAME).equals("SLB786");

        addData(mFatherName,super.mName);
        initNode();
        registerReceiver(mBroadcastReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mHandler.sendEmptyMessageDelayed(1001,getResources().getInteger(R.integer.start_delay_time));

        mRun = new Runnable() {
            @Override
            public void run() {
                if(mIsRuning) {
                        mConfigTime--;
                        updateFloatView(mContext, mConfigTime);
                        if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext) )) {
                            mHandler.sendEmptyMessage(1003);
                        }
                    mHandler.postDelayed(this, 1000);
                }
            }
        };
        mRun.run();

        mBatteryManager = (BatteryManager)getSystemService(Context.BATTERY_SERVICE);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DISABLE_CHARGING), 3000);
        }
    }

    private void initNode() {

        File nodeFile = new File(Const.CIT_NODE_CONFIG_PATH);
        if(!nodeFile.exists()){
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.CIT_NODE_CONFIG_PATH));
            finish();
            return;
        }
        try{
            InputStream inputStream = new FileInputStream(nodeFile);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT){
                if(type == XmlPullParser.START_TAG){
                    String startTagName = xmlPullParser.getName();

                    if(TAG_DC_JACT_CHARGE_NODE.equals(startTagName)){
                        mDcJactChargeNode = xmlPullParser.nextText();
                    }else if(TAG_CHANGER_STATUS_NODE.equals(startTagName)){
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

        String emptyNode = "";
        emptyNode += mDcJactChargeNode.isEmpty() ? TAG_DC_JACT_CHARGE_NODE : "";
        if(mFatherName.equals(MyApplication.RuninTestNAME))
            emptyNode += mChangerStatusNodePath.isEmpty() ? " " + TAG_CHANGER_STATUS_NODE : "";

        if(!emptyNode.isEmpty()){
            ToastUtil.showCenterLong(getString(R.string.node_not_found, emptyNode, Const.CIT_NODE_CONFIG_PATH));
            finish();
            return;
        }

    }

    public static String ByteToString(byte[] bytes)
    {

        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i <bytes.length ; i++) {
            if (bytes[i]!=0){
                strBuilder.append((char)bytes[i]);
            }else {
                break;
            }

        }
        return strBuilder.toString();
    }
    private String getBatteryStatus() {
        FileInputStream file = null;
        String batteryDC = "";
        try {
            file = new FileInputStream(mDcJactChargeNode);
            byte[] buffer = new byte[file.available()];
            file.read(buffer);

            file.close();
            batteryDC = new String(ByteToString(buffer));
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
        return batteryDC;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    botflag = changestateflag();
                    mChangeStatus=getBatteryStatus().trim();
                    mChargeOff.setText(getResources().getString(R.string.change_now) + mChangeStatus);
                    LogUtil.d("mChangeStatus:" + mChangeStatus +"  isSLB786="+isSLB786);

                    if(isSLB786){
                        String dcOnlineStatus = DataUtil.readLineFromFile(DC_ONLINE_NODE);
                        LogUtil.d("dcOnlineStatus:" + dcOnlineStatus);
                        boolean isDcOnline = "1".equals(dcOnlineStatus);
                        if (mChangeStatus.equals("Full") && isDcOnline) {
                            mChargeOff.setText(getResources().getString(R.string.change_now) + getResources().getString(R.string.Full));
                        } else if (mChangeStatus.equals("Charging") && isDcOnline) {
                            if (botflag) {
                                mSuccess.setVisibility(View.VISIBLE);
                                if ((mFatherName.equals(MyApplication.PCBANAME)) || (mFatherName.equals(MyApplication.PreNAME))) {
                                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                                    deInit(mFatherName, SUCCESS);//auto pass pcba & pre
                                }
                            } else {
                                mSuccess.setVisibility(View.GONE);
                            }
                            mChargeOff.setText(getResources().getString(R.string.change_now) + getResources().getString(R.string.Charging));
                        } else {
                            mSuccess.setVisibility(View.GONE);
                            mChargeOff.setText(getResources().getString(R.string.change_now) + getResources().getString(R.string.Non_change));
                        }
                    }else {
                        if (mChangeStatus.equals("Full")) {
                            mChargeOff.setText(getResources().getString(R.string.change_now) + getResources().getString(R.string.Full));
                        } else if (mChangeStatus.equals("Charging")) {
                            if (botflag) {
                                mSuccess.setVisibility(View.VISIBLE);
                                if ((mFatherName.equals(MyApplication.PCBANAME)) || (mFatherName.equals(MyApplication.PreNAME))) {
                                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                                    deInit(mFatherName, SUCCESS);//auto pass pcba & pre
                                }
                            } else {
                                mSuccess.setVisibility(View.GONE);
                            }
                            mChargeOff.setText(getResources().getString(R.string.change_now) + getResources().getString(R.string.Charging));
                        } else {
                            mSuccess.setVisibility(View.GONE);
                            mChargeOff.setText(getResources().getString(R.string.change_now) + getResources().getString(R.string.Non_change));
                        }
                    }

                    break;
                case 1002:
                    deInit(mFatherName, SUCCESS);
                        break;
                case 1003:
                        deInit(mFatherName, FAILURE, getResources().getString(R.string.fail_timeout));
                    break;
                case MSG_DISABLE_CHARGING:
                    setChangerEnable(false);
                    sendMessage(mHandler.obtainMessage(1001));
                    sendMessageDelayed(obtainMessage(MSG_ENABLE_CHARGING), 3000);
                    break;
                case MSG_ENABLE_CHARGING:
                    hasDisconnected = mBatteryManager.getIntProperty(BATTERY_PROPERTY_STATUS) != BatteryManager.BATTERY_STATUS_CHARGING;
                    setChangerEnable(true);
                    sendMessage(mHandler.obtainMessage(1001));
                    sendMessageDelayed(obtainMessage(MSG_DEINIT), 3000);
                    break;
                case MSG_DEINIT:
                    hasConnected = mBatteryManager.getIntProperty(BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING;
                    if(mFatherName.equals(MyApplication.RuninTestNAME)){
                        deInit(mFatherName, (hasDisconnected && hasConnected) ? SUCCESS : FAILURE);
                    }
            }
        }
    };

    private void setChangerEnable(boolean enable){
        try {
            BufferedWriter bfw = new BufferedWriter(new FileWriter(mChangerStatusNodePath, true));
            bfw.write(enable ? "1" : "0");
            bfw.flush();
            bfw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        String action;
        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            LogUtil.d("action:" + action);
            Message msg = mHandler.obtainMessage();
            msg.what = 1001;
            mHandler.sendMessage(msg);
         }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsRuning = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        mHandler.removeMessages(1001);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(MSG_DISABLE_CHARGING);
        mHandler.removeMessages(MSG_ENABLE_CHARGING);
        mHandler.removeMessages(MSG_DEINIT);
        mIsRuning = false;
    }
    @Override
    public void onClick(View v) {
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }else if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    private String getCurrentnode(String node) {
        String currentNow = "";
        try {
            char[] buffer = new char[1024];

            FileReader fileReader = new FileReader(node);
            int len = fileReader.read(buffer, 0, buffer.length);
            String data = new String(buffer, 0, len);
            currentNow = data;

            fileReader.close();
        } catch (Exception e) {
            LogUtil.e("Get current now node : " + node + "fail.");
            LogUtil.e("e1111 : "+e.toString());
        }
        return currentNow;
    }

    private boolean changestateflag(){
        boolean flag = true;
        String bot_changeflag = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Bot_connect_change_state_flag);
        boolean change_flag = bot_changeflag.equals("true");
        botstate = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Bot_connect_state);
        String botstate_now = getCurrentnode(botstate);
        if(botstate_now.contains("0") && change_flag){
            flag = false;
        }
        return flag;
    }
}
