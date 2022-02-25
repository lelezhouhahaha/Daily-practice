package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

//import static android.view.WindowManagerPolicy.WindowManagerFuncs.LID_CLOSED;
//import static android.view.WindowManagerPolicy.WindowManagerFuncs.LID_OPEN;

import java.io.FileOutputStream;
import java.util.List;

import butterknife.BindView;

public class HallActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack,Runnable{
    private HallActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    @BindView(R.id.tv_message)
    public TextView mMessageTv;

    private boolean isCloseSuccessed = false;
    private boolean isOpenSuccessed = false;

    private static final int MSG_TEST_SUCCESS = 8888;

    private static final String HALL_RECEIVER_ACTION = "com.meig.broadcast.hallswitch";
    private static final String HALL_CHECK_KEYEVENT = "common_hal_keyevent_check";
    private String HALL_TEST_CONFIG_KEY = "common_hall_test_defualt_int_config";
    private int mHallTestConfig = 0;
    private int mHallTestkey = 0;
    private SensorManager mSensorManager;
    private Sensor mSensorLight;
    private boolean mHallFirstStat = false;
    private boolean mHallSecondStat = false;
    private int LID_CLOSED = 0;
    private int LID_OPEN = 1;

    private BroadcastReceiver mHallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int hallState = bundle.getInt("hallState");
            LogUtil.d("onRecive hallState:" + hallState);
            if(hallState == LID_CLOSED){
                LogUtil.d("onRecive hallState == LID_CLOSED");
                mMessageTv.setText(R.string.hall_msg_magent_open);
                Toast.makeText(HallActivity.this, R.string.hall_toast_magent_close, Toast.LENGTH_SHORT);
                isCloseSuccessed = true;
                if(isOpenSuccessed)
                    mHandler.sendEmptyMessage(MSG_TEST_SUCCESS);
            }else if(hallState == LID_OPEN){
                LogUtil.d("onRecive hallState == LID_OPEN");
                mMessageTv.setText(R.string.hall_msg_magent_close);
                Toast.makeText(HallActivity.this, R.string.hall_toast_magent_open, Toast.LENGTH_SHORT);
                isOpenSuccessed = true;
                if(isCloseSuccessed)
                    mHandler.sendEmptyMessage(MSG_TEST_SUCCESS);
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_hall;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_hall);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
            addData(mFatherName,super.mName);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        String str = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HALL_TEST_CONFIG_KEY);
        if(!str.isEmpty())
            mHallTestConfig = Integer.parseInt(str);

        String hall_key = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HALL_CHECK_KEYEVENT);
        if(!hall_key.isEmpty())
            mHallTestkey = Integer.parseInt(hall_key.trim());

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        LogUtil.d("citapk mHallTestConfig:" + mHallTestConfig);

        if(mHallTestkey != 0) {

            if (mHallTestConfig == 1) {
                mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
                for (Sensor sensor : allSensors) {
                    LogUtil.d("citapk sensor.getStringType():" + sensor.getStringType());
                    if (sensor.getStringType().equals("com.qti.sensor.hall_effect")) {
                        LogUtil.d("citapk 1:");
                        mSensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
                        break;
                    }
                }
            } else {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(HALL_RECEIVER_ACTION);
                registerReceiver(mHallReceiver, intentFilter);
            }
        }
        LogUtil.d("initData end."+"  mHallTestkey: "+mHallTestkey);
    }

    SensorEventListener listener = new SensorEventListener() {
        @Override  public void onSensorChanged(SensorEvent event) {
            LogUtil.d("citapk event.values[0]:" + event.values[0]);
            int value = Math.round(event.values[0]);
            LogUtil.d("citapk value:" + value);
            if(value == 0) {
                mMessageTv.setText(R.string.hall_msg_magent_close);
                mHallFirstStat = true;
            }else if(value == 1) {
                mMessageTv.setText(R.string.hall_msg_magent_open);
                mHallSecondStat = true;
            }
            mHandler.sendEmptyMessage(1005);
        }
        @Override  public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
                case MSG_TEST_SUCCESS:
                    mMessageTv.setText(R.string.success);
                    if(mFatherName.equals(MyApplication.PCBASignalNAME) || mFatherName.equals(MyApplication.PreSignalNAME)){
                        mSuccess.setVisibility(View.VISIBLE);
                    }else{
                        deInit(mFatherName, SUCCESS);
                    }
                    break;
                case 1005:
                    if(mHallTestConfig == 1){
                        if(mHallFirstStat && mHallSecondStat)
                            mHandler.sendEmptyMessage(MSG_TEST_SUCCESS);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
		mHandler.removeMessages(1005);
		mHandler.removeMessages(MSG_TEST_SUCCESS);

        if(mHallTestConfig == 1 && null != listener){
            mSensorManager.unregisterListener(listener);
        }else if(null != mHallReceiver){
            unregisterReceiver(mHallReceiver);
        }
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
            return true;
        }
        if(mHallTestkey!=0 && event.getScanCode()==mHallTestkey){
            mHandler.sendEmptyMessage(MSG_TEST_SUCCESS);
        }
        return super.onKeyDown(keyCode, event);
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

    @Override
    public void run() {

    }
}