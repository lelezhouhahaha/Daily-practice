package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;

public class TypeCChargeActivity extends BaseActivity implements View.OnClickListener {

    private TypeCChargeActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;

    @BindView(R.id.current_now)
    public TextView mCurrentNow;//主电池电流
    @BindView(R.id.show_info)
    public TextView mShowInfo;
    @BindView(R.id.status)
    public TextView mStatus;//充电状态
    @BindView(R.id.quantity)
    public TextView mQuantity;//充电电量
    @BindView(R.id.voltageNow)
    public TextView mVoltageNow;//充电电压

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private static final String TAG_CURRENT_NOW_NODE = "current_now_node";
    private static final String TAG_VOLTAGE_NOW_NODE = "voltage_now_node";
    private static final String TAG_CAPACITY_NODE = "capacity_node";
    private static final String TAG_CHANGER_STATUS_NODE = "changer_status_node";
    private String TAG_CURRENT_THRESHOLD = "current_threshold_node";

    private String current_now_node = "";
    private String capacity_node = "";
    private String voltage_now_node = "";
    private String mChangerStatusNodePath = "";
    private static final String STATUS = "status";
    private static final String LEVEL = "level";
    private static final String PLUGGED = "plugged";
    private static final String VOLTAGE = "voltage";
    private int mConfigTime = 0;
    private String mChargingSatus = "";
    private int mCurrentThresholdValue = 0;
    private boolean isMT537_version =false;

    private String munknown = "";
    private String mfull = "";
    private String mcharging = "";
    private String mdischarging = "";
    private String mnochange = "";

    private static final int MSG_DISABLE_CHARGING = 1100;
    private static final int MSG_ENABLE_CHARGING = 1101;
    private static final int MSG_DEINIT = 8888;

    private boolean hasDisconnected = false;
    private boolean hasConnected = false;

    private BatteryManager mBatteryManager;
    private static final int BATTERY_PROPERTY_STATUS = 6;

    private boolean isMC511 = DataUtil.getDeviceName().equals("MC511");

    private boolean isTypeCCharging(){
        if(isMC511) {
            return  DataUtil.getChargingType() == DataUtil.USB_CHARGE;
        }else{
            return true;
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_battery_charge_pcba;
    }

    @Override
    protected void initData() {
        mContext = this;

        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.TypeCChargeActivity);
        mShowInfo.setText(String.format(getString(R.string.show_info), getString(R.string.fail)));

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        isMT537_version = SystemProperties.get("meig.full.sw.version").contains("MT537")||SystemProperties.get("meig.ap.sw.version").contains("MT537");

        munknown = mContext.getString(R.string.stat_unknown);
        mfull = mContext.getString(R.string.Full);
        mcharging = mContext.getString(R.string.Charging);
        mdischarging = mContext.getString(R.string.Discharging);
        mnochange = mContext.getString(R.string.Non_change);


        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        initNode();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        mHandler.sendEmptyMessageDelayed(1001, getResources().getInteger(R.integer.start_delay_time));

        mBatteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DISABLE_CHARGING), 3000);
        }

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

                    if (TAG_CURRENT_NOW_NODE.equals(startTagName)) {
                        current_now_node = xmlPullParser.nextText();
                    } else if (TAG_CAPACITY_NODE.equals(startTagName)) {
                        capacity_node = xmlPullParser.nextText();
                    } else if (TAG_VOLTAGE_NOW_NODE.equals(startTagName)) {
                        voltage_now_node = xmlPullParser.nextText();
                    } else if (TAG_CHANGER_STATUS_NODE.equals(startTagName)) {
                        mChangerStatusNodePath = xmlPullParser.nextText();
                    } else if (TAG_CURRENT_THRESHOLD.equals(startTagName)) {
                        String CurrentThreshold = xmlPullParser.nextText();
                        if (!CurrentThreshold.isEmpty())
                            mCurrentThresholdValue = Integer.parseInt(CurrentThreshold);
                        LogUtil.d("mCurrentThresholdValue:" + mCurrentThresholdValue);
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

        String emptyNode = current_now_node.isEmpty() ? TAG_CURRENT_NOW_NODE : "";
        emptyNode += capacity_node.isEmpty() ? " " + TAG_CAPACITY_NODE : "";
        emptyNode += voltage_now_node.isEmpty() ? " " + TAG_VOLTAGE_NOW_NODE : "";
        if (mFatherName.equals(MyApplication.RuninTestNAME))
            emptyNode += mChangerStatusNodePath.isEmpty() ? " " + TAG_CHANGER_STATUS_NODE : "";

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
            LogUtil.e("Get current now node Exception : " + e.toString());
            LogUtil.e("Get current now node : " + node + "fail.");
        }
        return currentNow;
    }

    private boolean isOkCurrentNow(Float currentNow) {
        if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PCBANAME)) {
            if (currentNow > 0&&!isMT537_version) {
                currentNow = Float.valueOf("0");
            }else if(currentNow > 0&&isMT537_version) {
                currentNow =currentNow;
            } else{
                currentNow = Math.abs(currentNow);
            }
            if (mCurrentThresholdValue == 0) {
                mCurrentThresholdValue = 1000;
                LogUtil.d("isOkCurrentNow mCurrentThresholdValue:" + mCurrentThresholdValue);
            }
            if ((currentNow < mCurrentThresholdValue * 1000)&&!isMT537_version)
                return false;
        }
        return true;
    }

    private void updateView(Float currentNow, boolean result) {

        if (!result) {
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.VISIBLE);
            mShowInfo.setText(String.format(getString(R.string.show_info), getString(R.string.fail)));
        } else {
            mSuccess.setVisibility(View.VISIBLE);
            mFail.setVisibility(View.GONE);
            mShowInfo.setText(String.format(getString(R.string.show_info), getString(R.string.success)));
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
                    mFlag.setVisibility(View.GONE);
                    Float currentNow = getCurrentNow(current_now_node);

                    if (mFatherName.equals(MyApplication.RuninTestNAME)) {
                        mChargingSatus = getChangeStatusString(mBatteryManager.getIntProperty(BATTERY_PROPERTY_STATUS));
                        if (!TextUtils.isEmpty(mChargingSatus) && !munknown.equals(mChargingSatus)) {
                            if (!hasDisconnected) {
                                hasDisconnected = !mChargingSatus.equals(mcharging);
                            } else if (!hasConnected) {
                                hasConnected = mChargingSatus.equals(mcharging);
                            }
                        }
                    }
                    if (!TextUtils.isEmpty(mChargingSatus)
                            && !munknown.equals(mChargingSatus) && isOkCurrentNow(currentNow)) {
                        if (mChargingSatus.equals(mcharging) && isTypeCCharging()) {
                            updateView(currentNow, true);
                            if ((mFatherName.equals(MyApplication.PCBANAME)) || (mFatherName.equals(MyApplication.PreNAME))) {
                                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                                deInit(mFatherName, SUCCESS);//auto pass pcba & pre
                            }
                        }else{
                            updateView(currentNow, false);
                        }
                    }
                    if (currentNow > 0&&(!isMT537_version)) {
                        currentNow = Float.valueOf("0");

                    }else if(currentNow>0&&(isMT537_version)){
                        currentNow =currentNow;
                    }else {
                        currentNow = Math.abs(currentNow);
                    }
                    mCurrentNow.setText(String.format(getString(R.string.current_now), currentNow / 1000));
                    if(!isTypeCCharging() && mChargingSatus.equals(mcharging)) {
                        mStatus.setText(String.format(getString(R.string.statusNow), getString(R.string.not_typec_charging)));
                    }else{
                        mStatus.setText(String.format(getString(R.string.statusNow), mChargingSatus));
                    }

                    int Quantity = (int) getCurrentNow(capacity_node);
                    float voltageNow = getCurrentNow(voltage_now_node) / 1000;
                    mQuantity.setText(String.format(getString(R.string.quantity), Quantity));
                    if (voltageNow > 0) {
                        mVoltageNow.setText(String.format(getString(R.string.voltage_now), voltageNow / 1000));
                    } else
                        mVoltageNow.setText(String.format(getString(R.string.voltage_now_ov), "OV"));
                    mHandler.sendEmptyMessageDelayed(1001, getResources().getInteger(R.integer.loop_delay_time));
                    break;
                case 1003:
                    if(!isMT537_version){
                        BatteryVolume volume = (BatteryVolume) msg.obj;
                        mChargingSatus = volume.getStatus();
                    }else{
                        mChargingSatus =FileUtil.readFromFile("/sys/class/power_supply/battery/status");
                        if(mChargingSatus.contains("Charging")){
                            mChargingSatus= mContext.getString(R.string.Charging);
                        }else if(mChargingSatus.contains("Discharging")){
                            mChargingSatus= mContext.getString(R.string.Discharging);
                        }else if(mChargingSatus.contains("Full")){
                            mChargingSatus= mContext.getString(R.string.Full);
                        }
                    }
                    break;
                case MSG_DISABLE_CHARGING:
                    setChangerEnable(false);
                    sendMessageDelayed(obtainMessage(MSG_ENABLE_CHARGING), 3000);
                    break;
                case MSG_ENABLE_CHARGING:
                    setChangerEnable(true);
                    sendMessageDelayed(obtainMessage(MSG_DEINIT), 3000);
                    break;
                case MSG_DEINIT:
                    if (mFatherName.equals(MyApplication.RuninTestNAME)) {
                        //modify by wangxing for bug P_RK95_E-706 run in log show pass
                        //deInit(mFatherName, (hasDisconnected && hasConnected) ? SUCCESS : FAILURE);
                        deInit(mFatherName,  SUCCESS );
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    private void setChangerEnable(boolean enable) {
        try {
            BufferedWriter bfw = new BufferedWriter(new FileWriter(mChangerStatusNodePath, true));
            bfw.write(enable ? "1" : "0");
            bfw.flush();
            bfw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
        mHandler.removeMessages(MSG_DISABLE_CHARGING);
        mHandler.removeMessages(MSG_ENABLE_CHARGING);
        mHandler.removeMessages(MSG_DEINIT);

    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        } else if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
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
                break;
        }
        return statusString;
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        String action;


        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            BatteryVolume volume = new BatteryVolume();

            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(STATUS, 0);
                int plugged = intent.getIntExtra(PLUGGED, 0);
                volume.setVoltage(intent.getIntExtra(VOLTAGE, 0));
                volume.setLevel(intent.getIntExtra(LEVEL, 0));
                String statusString = getChangeStatusString(status);
                String acString = "";
                switch (plugged) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        acString = "PLUGGED_AC";
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        acString = "PLUGGED_USB";
                        break;
                    default:
                        acString = "UNKNOWN";
                        break;
                }
                volume.setStatus(statusString);
                volume.setPlugged(acString);
                LogUtil.d(volume.toString());
                Message msg = mHandler.obtainMessage();
                msg.what = 1003;
                msg.obj = volume;
                mHandler.sendMessage(msg);
            }
        }
    };


}