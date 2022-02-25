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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.BatteryVolume;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class BatteryActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack {
    private BatteryActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.chargeLevel)
    public TextView mLevel;
    @BindView(R.id.chargeStatus)
    public TextView mStatus;
    @BindView(R.id.chargeMethod)
    public TextView mMethod;
    @BindView(R.id.charge)
    public TextView mCharge;
    @BindView(R.id.discharge)
    public TextView mDischarge;
    @BindView(R.id.recharge)
    public TextView mRecharge;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean status_charge = false;
    private boolean status_discharge = false;
    private boolean status_recharge = false;

    private static final String STATUS = "status";
    private static final String LEVEL = "level";
    private static final String PLUGGED = "plugged";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_battery;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_battery);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        mConfigResult = getResources().getInteger(R.integer.battery_charge_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        addData(mFatherName,super.mName);
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mHandler.sendEmptyMessageDelayed(1001,2000);
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                }
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
                    mFlag.setVisibility(View.GONE);
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                    registerReceiver(mBroadcastReceiver, filter);
                    break;

                case 1002:
                    deInit(mFatherName, SUCCESS);
                    break;

                case 1003:
                    BatteryVolume volume = (BatteryVolume) msg.obj;
                    mLevel.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_charge_is)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getLevel()+"%"+"</font>"
                            ));
                    mStatus.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_charger_status)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getStatus()+"</font>"
                            ));
                    mMethod.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_charger_method)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getPlugged()+"</font>"
                            ));
                    checkSuccess(volume);
                    break;

                case 9999:
                    break;
            }
        }
    };

    public void checkSuccess(BatteryVolume volume) {
        if (volume.getLevel() == 100 && !status_charge) {
            status_charge = true;
            mCharge.setTextColor(getResources().getColor(R.color.green_1));
        }
        if (status_charge && !status_discharge) {
            if ("DISCHARGING".equals(volume.getStatus())) {
                status_discharge = true;
                mDischarge.setTextColor(getResources().getColor(R.color.green_1));
            }
        }
        if (status_discharge && !status_recharge) {
            if ("CHARGING".equals(volume.getStatus())) {
                status_recharge = true;
                mRecharge.setTextColor(getResources().getColor(R.color.green_1));
            }
        }
        if (status_recharge) {
            //mDialog.setSuccess();
            mHandler.sendEmptyMessage(1002);
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        String action;
        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            BatteryVolume volume = new BatteryVolume();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int status = intent.getIntExtra(STATUS, 0);
                int plugged = intent.getIntExtra(PLUGGED, 0);
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
                Message msg = mHandler.obtainMessage();
                msg.what = 1003;
                msg.obj = volume;
                mHandler.sendMessage(msg);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
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
}
