package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.BatteryVolume;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.FileReader;
import java.io.IOException;

import butterknife.BindView;

/**
 * charger 充电器测试
 * Created by chenMeng on 2018/05/14
 */
public class ChargerActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack   {
    private ChargerActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.battery)
    public TextView mBattery;
    @BindView(R.id.secondBattery)
    public TextView mChargingVoltage;
    @BindView(R.id.chargingStatus)
    public TextView mChargingStatus;
    @BindView(R.id.chargingMethod)
    public TextView mChargingMethod;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;


    private static final String BATTERY_ELECTRONIC = "/sys/class/power_supply/bms/current_now";//read
    private static final String BATTERY_V = "/sys/class/power_supply/bms/voltage_ocv";
    private static final String STATUS = "status";
    private static final String LEVEL = "level";
    private static final String PLUGGED = "plugged";
    private static final String VOLTAGE = "voltage";

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_charger;
    }
    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_charger);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mConfigResult = getResources().getInteger(R.integer.charger_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        mHandler.sendEmptyMessageDelayed(1001,2000);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                }
                //if (isStartTest)mHandler.sendEmptyMessage(1003);
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    isStartTest = true;
                    mFlag.setVisibility(View.GONE);
                    break;
                case 1002:
				//modify by gongming for 13840 on 2020/08/28
				deInit(mFatherName, SUCCESS);
                    break;
                case 1003:
                    BatteryVolume volume = (BatteryVolume) msg.obj;
                    mBattery.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_charge_is)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getLevel()+"%"+"</font>"
                            ));

                    mChargingVoltage.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_charger_voltage)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getVoltage()+ " mv"+"</font>"
                            ));
                    mChargingStatus.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_charger_status)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getStatus()+"</font>"
                            ));

                    mChargingMethod.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_charger_method)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getPlugged()+"</font>"
                            ));
                    if (!TextUtils.isEmpty(volume.getPlugged())
                            && !"UNKNOWN".equals(volume.getPlugged())){
                        mSuccess.setVisibility(View.VISIBLE);
                        mDialog.setSuccess();
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
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

    private float getBatteryElectronic() {
        char[] buffer = new char[1024];

        float batteryElectronic = 0;
        FileReader file = null;
        try {
            file = new FileReader(BATTERY_ELECTRONIC);
            int len = file.read(buffer, 0, 1024);
            batteryElectronic = Float.valueOf((new String(buffer, 0, len)));
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
        return batteryElectronic;
    }

    private float getBatteryVoltage() {
        char[] buffer = new char[1024];

        float batteryVoltage = 0;
        FileReader file = null;
        try {
            file = new FileReader(BATTERY_V);
            int len = file.read(buffer, 0, 1024);
            batteryVoltage = Float.valueOf((new String(buffer, 0, len)));
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
        return batteryVoltage;
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
                volume.setLevel(intent.getIntExtra(LEVEL,0));
                String statusString = "";
                String acString = "";
                switch (status) {
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                        statusString = "UNKNOWN";
                        break;

                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        statusString = "CHARGING";
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        statusString = "DISCHARGING";
                        break;

                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        statusString = "NOT_CHARGING";
                        break;

                    case BatteryManager.BATTERY_STATUS_FULL:
                        statusString = "FULL";
                        break;
                    default:
                        break;
                }
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
