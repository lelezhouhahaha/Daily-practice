package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.ChaosCompassView;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class EComPassPCBAActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private EComPassPCBAActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.compassView)
    public ChaosCompassView mCompass;

    private SensorManager mSensorManager;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    private boolean isPass;
    private float[] mAccelerometerValues = null;
    private float[] mMagneticFieldValues  = null;
    private float mRotation = (float) 0.00;
    private boolean mAccFlag = false;
    private boolean mMagFlag = false;

    private boolean mTestSuccessFlag = false;

    private String TAG_MC510 = "common_device_name_test";
    private boolean is_MC510 = false;
    String EComPassActivity_auto_judgment_key = "common_EComPassActivity_auto_judgment_bool";
    boolean B_EComPassActivity_auto_judgment = true;
    Sensor defaultSensor = null;
    Sensor defaultSensorAcc = null;
    private final String TAG = EComPassPCBAActivity.class.getSimpleName();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_e_compass_pcba;
    }
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    @Override
    protected void initData() {
        mContext = this;
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, EComPassActivity_auto_judgment_key);
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.run_in_e_compass);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mAccelerometerValues = new float[3];
        mMagneticFieldValues = new float[3];

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mConfigResult = getResources().getInteger(R.integer.e_compass_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) /*&& mFatherName.equals(MyApplication.RuninTestNAME)*/) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    LogUtil.d("EComPassActivity mConfigTime:<" + mConfigTime + "> and sendEmptyMessage 1002.");
                    if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                        mHandler.sendEmptyMessage(1002);
                    }else mHandler.sendEmptyMessage(1001);
                    mConfigTime = 1;
                    return;
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        //mHandler.sendEmptyMessageDelayed(1001, 5000);
    }

    @Override
    protected void onResume(){
        super.onResume();
        initSensor();
    }

    private void initSensor() {
        if (mSensorManager != null){
            if (Build.VERSION.SDK_INT >= 29) {
                defaultSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                defaultSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                LogUtil.d("defaultSensor = TYPE_MAGNETIC_FIELD");
            } else {
                defaultSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
                LogUtil.d("defaultSensor = TYPE_ORIENTATION");
            }

            if (defaultSensor == null) {
                sendErrorMsg(mHandler, "defaultSensor is null");
                return;
            }
            mSensorManager.registerListener(mSensorEventListener, defaultSensor, SensorManager.SENSOR_DELAY_UI);
            if ((Build.VERSION.SDK_INT >= 29) && (defaultSensorAcc != null))
                mSensorManager.registerListener(mSensorEventListener, defaultSensorAcc, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if(mFatherName.equals(MyApplication.RuninTestNAME)){
                        deInit(mFatherName, SUCCESS);
                    }else {
                        mCompass.setVisibility(View.VISIBLE);
                    }
                    break;
                case 1002:
                    if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                        if(mTestSuccessFlag)
                            deInit(mFatherName, SUCCESS);
                        else deInit(mFatherName, FAILURE, getTestFailReason());
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LogUtil.d("EComPassActivity", "onConfigurationChanged ");
        mCompass.reset();
        super.onConfigurationChanged(newConfig);
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (Build.VERSION.SDK_INT >= 29){
                if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                    mMagneticFieldValues = event.values;
                    mMagFlag = true;
                }else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                        mAccelerometerValues = event.values;
                        mAccFlag = true;
                }
                if(mMagFlag && mAccFlag) {
                    float value = onCalculateOrientation();
                    LogUtil.d(TAG, " value:<" + value + ">.");
                    if(mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PCBASignalNAME) || mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                        if(value != 0) {
                            mTestSuccessFlag  = true;
                            mSuccess.setVisibility(View.VISIBLE);
                            if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                                mHandler.sendEmptyMessage(1002);
                            }
                        }
                    }else {
                        if (value > 150 && value < 230) {
                            mTestSuccessFlag = true;
                            mSuccess.setVisibility(View.VISIBLE);
                        }
                    }
                    mCompass.setVal(value);
                    mMagFlag = false;
                    mAccFlag = false;
                }
            }else {
                mCompass.setVal(event.values[0]);
                LogUtil.d("Build.VERSION.SDK_INT <= 29 value:<" + event.values[0] + ">.");
                if(mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PCBASignalNAME) || mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                    if(event.values[0] != 0) {
                        mTestSuccessFlag = true;
                        mSuccess.setVisibility(View.VISIBLE);
                        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
                            mHandler.sendEmptyMessage(1002);
                        }
                    }
                }else {
                    if (event.values[0] > 150 && event.values[0] < 230) {
                        mTestSuccessFlag = true;
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    LogUtil.d("accuracy:" + accuracy);
        }

        private float onCalculateOrientation() {
            float[] values = new float[3];
            float[] R = new float[9];

            SensorManager.getRotationMatrix(R, null, mAccelerometerValues, mMagneticFieldValues);
            SensorManager.getOrientation(R, values);

            values[0] = (float) Math.toDegrees(values[0]);
            float orientation = values[0];

            if(orientation<0){
                orientation = 360+orientation;
            }
            orientation += mRotation;
            if (orientation>360){
                orientation -= 360;
            }
            LogUtil.d(" orientation:<" +values[0] + ">.");
            return orientation;
        }
    };

    @Override
    public void onClick(View v) {
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
    protected void onDestroy() {
        mHandler.removeCallbacks(mRun);
	if((mSensorEventListener != null)&&(mSensorManager!=null)){
            mSensorManager.unregisterListener(mSensorEventListener);
            Log.d("ecom_demo","exit");
        }else{
            Log.d("ecom_demo","null_sensor");
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
        super.onDestroy();
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
