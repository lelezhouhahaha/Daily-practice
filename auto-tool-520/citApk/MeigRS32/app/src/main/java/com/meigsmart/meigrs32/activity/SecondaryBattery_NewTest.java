package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
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
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import butterknife.BindView;

public class SecondaryBattery_NewTest extends BaseActivity implements View.OnClickListener {

    private SecondaryBattery_NewTest mContext;
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

    private String mFatherName = "";

    private int mConfigResult;
    private int mConfigTime = 0;

    private final static String TAG_BATTERY_SWITCH_NODE = "/sys/devices/platform/soc/soc:gpio_lock/bat_ctl";


    private String switch_battery_node = "/sys/devices/platform/soc/soc:gpio_lock/bat_ctl";
    private String primary_battery_status_node = "/sys/class/power_supply/bms/voltage_now";
    private String mSecondBatteryStatePath = "/sys/class/power_supply/oem_bat/pin_enabled";
    private String sub_battery_charg_status_node = "";
    private String mSecondBatteryState = "";

    private final static String BATTERY_PRIMARY = "0";
    private final static String BATTERY_SUB = "1";

    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_battery_test;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.pcba_second_battery_charge);

        mNext.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        mConfigResult = getResources().getInteger(R.integer.battery_switch_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        addData(mFatherName, super.mName);
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        initNode();
        if (sub_battery_charg_status_node.equals(""))
            sub_battery_charg_status_node = "/sys/class/power_supply/tla2024/sub_battery_chg";

        // add by maohaojie on 2018.12.17 for bug 20503
      //  setAirplaneMode(SecondaryBattery_NewTest.this, true);
        mHandler.sendEmptyMessage(1001);

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

    private void initNode() {
        File nodeFile = new File(Const.CIT_NODE_CONFIG_PATH);
        if (!nodeFile.exists()) {
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.CIT_NODE_CONFIG_PATH));
            finish();
            return;
        }

    }

    private void switchBattery(String battery) {
        FileOutputStream fSwitchBattery = null;
        try {
            fSwitchBattery = new FileOutputStream(switch_battery_node);
            fSwitchBattery.write(battery.getBytes());
            fSwitchBattery.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
                    mMessage.setVisibility(View.VISIBLE);
                    mNext.setVisibility(View.VISIBLE);
                    mBelow.setVisibility(View.VISIBLE);
                    break;
                case 1002:
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
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
        batterySwap("0");
        // add by maohaojie on 2018.12.17 for bug 20503
        //setAirplaneMode(SecondaryBattery_NewTest.this, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                if (mMessage.getText().equals(getString(R.string.message_switch_to_subbattery))) {
                    checkUnSubplug();
                } else if (mMessage.getText().equals(getString(R.string.message_unplug_battery))) {
                    checkUnplug();
                } else if(mMessage.getText().equals(getString(R.string.message_insert_battery))){
                    checkInsert();
                }else{
                    TestSubBtteryElectric();
                }
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

    private void checkUnSubplug() {
        switchBattery(BATTERY_SUB);
     //   PowerManager pm = (PowerManager) (SecondaryBattery_NewTest.this.getSystemService(Context.POWER_SERVICE));
      //  pm.goToSleep(500);
        if(getSubBatteryStatus().equals("1")){
            String message = getString(R.string.message_unplug_battery);
            mMessage.setText(message);
        }

    }

    private void checkUnplug() {

        String message = getString(R.string.message_insert_battery);
        mMessage.setText(message);
    }

    private void checkInsert() {
        switchBattery(BATTERY_PRIMARY);

        String message = getBatteryStatus(primary_battery_status_node)+ "\n\n" + getString(R.string.message_switch_pribattery);
        mMessage.setText(message);
    }

    private void TestSubBtteryElectric() {
        String message =  getString(R.string.message_sub_battery);
        batterySwap("1");
        mMessage.setText(message);

        mNext.setVisibility(View.GONE);
        mSuccess.setVisibility(View.VISIBLE);
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
        if (battery_status_node.equals(primary_battery_status_node))
            str = getString(R.string.primary_battery_voltage, (voltage / 1000) / 1000);
        else {
            str = getString(R.string.sub_battery_voltage, voltage / 1000);
            str += "\n";
            String state = null;
            state = DataUtil.readLineFromFile(sub_battery_charg_status_node);
            if (state.equals("0"))
                mSecondBatteryState = getResources().getString(R.string.Non_change);
            else if (state.equals("1"))
                mSecondBatteryState = getResources().getString(R.string.Charging);
            else mSecondBatteryState = getResources().getString(R.string.Abnormal);
            str += getResources().getString(R.string.battery_second_charge_state);
            str += mSecondBatteryState;
        }

        return String.format(str);
    }

    // add by maohaojie on 2018.11.27
    public static void setScreenBrightness(Activity activity, int value) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = value / 255f;
        activity.getWindow().setAttributes(params);
    }

    // add by maohaojie on 2018.12.17 for bug 20503
    private void setAirplaneMode(Context context, boolean enabling) {
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                enabling ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        context.sendBroadcast(intent);
    }

    private String getSubBatteryStatus() {

        String Sub_batterStatus = TAG_BATTERY_SWITCH_NODE;
        try {
            FileReader file = new FileReader(TAG_BATTERY_SWITCH_NODE);
            char[] buffer = new char[1024];
            int len = file.read(buffer, 0, 1024);
            Sub_batterStatus = new String(buffer, 0, len);
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Sub_batterStatus.trim();
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

}
