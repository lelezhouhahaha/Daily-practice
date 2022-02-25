package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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

public class PogoPin2ChargeActivity extends BaseActivity implements View.OnClickListener {

    private PogoPin2ChargeActivity mContext;
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
    private static final String TAG_PRESENT_NODE = "present_node";

    //Charging current on Qualcomm platform is negative, SPRD platform is positive
    private static final String TAG_COMMON_CURRENT_ABSOLUTE_VALUE_ENABLE = "common_current_absolute_value_enable";
    private String mCurrentAbsoluteEnabled = "1";

    private String current_now_node = "";
    private String capacity_node = "";
    private String voltage_now_node = "";
    private String mChangerStatusNodePath = "";
    private static final String STATUS = "status";

    private int mConfigTime = 0;
    private int mCurrentThresholdValue = 0;
    private int charingStatus = 0;
    private static final int MSG_DEINIT = 8888;

    private String POGOPIN_CHARGE_STATUS_NODE = "/sys/bus/platform/drivers/musb-sprd/20200000.usb/vbus_status";
    private String POGOPIN_USB_TYPE_PATH = "dc_charging_type_node";


    private boolean isPogopinCharge(){
        String usbTypePath = "";
        boolean isPogoPinCharge = true;
        usbTypePath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, POGOPIN_USB_TYPE_PATH);
        if(usbTypePath != null && !usbTypePath.isEmpty()){
            String usbType = DataUtil.readLineFromFile(usbTypePath);
            LogUtil.d("citapk PogoPin2ChargeActivity usbType:" + usbType);
            isPogoPinCharge = usbType.equals("1");
            LogUtil.d("citapk PogoPin2ChargeActivity isPogopinCharge:" + isPogoPinCharge + " usbTypePath:" + usbTypePath);
        }
        LogUtil.d("citapk PogoPin2ChargeActivity isPogopinCharge:" + isPogoPinCharge);
        return isPogoPinCharge;//"pogo".equals(readNodeValue(POGOPIN_CHARGE_STATUS_NODE));
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
        mTitle.setText(R.string.Pogopin2ChargeActivity);
        mShowInfo.setText(String.format(getString(R.string.show_info), getString(R.string.fail)));

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
            LogUtil.e("Get current now node : " + node + "fail.");
        }

        return currentNow;
    }

    private boolean isOkCurrentNow(Float currentNow) {
        if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PCBANAME)) {
            /*if(!"0".equals(mCurrentAbsoluteEnabled)) {
                if (currentNow > 0)
                    currentNow = Float.valueOf("0");
                else currentNow = Math.abs(currentNow);
            }*/
            if (mCurrentThresholdValue == 0) {
                mCurrentThresholdValue = 1000;
                LogUtil.d("isOkCurrentNow mCurrentThresholdValue:" + mCurrentThresholdValue);
            }
            if (currentNow < mCurrentThresholdValue * 1000)
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
                    int Quantity = (int) getCurrentNow(capacity_node);
                    float voltageNow = getCurrentNow(voltage_now_node) / 1000;

					if(!"0".equals(mCurrentAbsoluteEnabled)) {
                        if (currentNow > 0)
                            currentNow = Float.valueOf("0");
                        else currentNow = Math.abs(currentNow);
                    }
                    mCurrentNow.setText(String.format(getString(R.string.current_now), currentNow / 1000));
                    mQuantity.setText(String.format(getString(R.string.quantity), Quantity));
                    if (voltageNow > 0) {
                        mVoltageNow.setText(String.format(getString(R.string.voltage_now), voltageNow / 1000));
                    } else
                        mVoltageNow.setText(String.format(getString(R.string.voltage_now_ov), "OV"));


                    if ( isPogopinCharge() ){
						LogUtil.d(" charingStatus:" + charingStatus);
						LogUtil.d("isOkCurrentNow(currentNow):" + isOkCurrentNow(currentNow));
						if((charingStatus == BatteryManager.BATTERY_STATUS_CHARGING) && isOkCurrentNow(currentNow)) {
                            updateView(currentNow, true);
                            if ((mFatherName.equals(MyApplication.PCBANAME)) || (mFatherName.equals(MyApplication.PreNAME))) {
                                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                                deInit(mFatherName, SUCCESS);//auto pass pcba & pre
                            }else {
                            mSuccess.setVisibility(View.VISIBLE);
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            }
                        } else {
                            updateView(currentNow, false);
                        }
                        mStatus.setText(String.format(getString(R.string.statusNow), getChangeStatusString(charingStatus)));
                    }else{
						updateView(currentNow, false);
						mStatus.setText(String.format(getString(R.string.statusNow), getString(R.string.not_pogopin_charging)));
					}
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
        unregisterReceiver(mBroadcastReceiver);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(9999);
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
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                charingStatus = intent.getIntExtra(STATUS, 0);
            }
        }
    };

    private String readNodeValue(String node) {
        String value = "";
        try {
            char[] buffer = new char[1024];

            FileReader fileReader = new FileReader(node);
            int len = fileReader.read(buffer, 0, buffer.length);
            String data = new String(buffer, 0, len);
            value = data;

            fileReader.close();
        } catch (Exception e) {
            LogUtil.e("readNodeValue node : " + node + "fail.");
            LogUtil.e("e1111 : "+e.toString());
        }
        LogUtil.e("readNodeValue node : " + node + "="+value.trim());
        return value.trim();
    }
}
