package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.Html;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;

public class GyroMeterCalibrationActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private GyroMeterCalibrationActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    //@BindView(R.id.back)
    //public LinearLayout mBack;
    private String mFatherName = "";
    //@BindViews({R.id.gyro_x,R.id.gyro_y,R.id.gyro_z})
    // List<TextView> mGyroList;
    @BindView(R.id.show_x)
    public TextView mShowx;
    @BindView(R.id.ensure_x)
    public Button mEnsurex;
    @BindView(R.id.show_y)
    public TextView mShowy;
    @BindView(R.id.ensure_y)
    public Button mEnsurey;
    @BindView(R.id.show_z)
    public TextView mShowz;
    @BindView(R.id.ensure_z)
    public Button mEnsurez;
    @BindView(R.id.show_xx)
    public TextView mShowxx;
    @BindView(R.id.ensure_xx)
    public Button mEnsurexx;
    @BindView(R.id.show_yy)
    public TextView mShowyy;
    @BindView(R.id.ensure_yy)
    public Button mEnsureyy;
    @BindView(R.id.show_zz)
    public TextView mShowzz;
    @BindView(R.id.ensure_zz)
    public Button mEnsurezz;
    @BindView(R.id.calibration)
    public Button mCalibration;
    @BindView(R.id.show_input_info)
    public TextView mInputInfo;
    @BindView(R.id.input)
    public EditText mInput;
    @BindView(R.id.ensure)
    public Button mEnsure;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private boolean mGyroMeterXAxis = false;
    private boolean mGyroMeterYAxis = false;
    private boolean mGyroMeterZAxis = false;
    private int mTimerDelay = 0;
    private float mSaveValue[];
    private float mCurrentValule[];
    private boolean mGyroMeterDefaultConfig;
    private String mGyroMeterDefaultCompareValue;

    private SensorManager sensorManager;//管理器对象
    private Sensor gyroSensor;//传感器对象

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_gyro_meter_calibration;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        //mBack.setVisibility(View.VISIBLE);
        //mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mEnsurex.setOnClickListener(this);
        mEnsurey.setOnClickListener(this);
        mEnsurez.setOnClickListener(this);
        mEnsurexx.setOnClickListener(this);
        mEnsureyy.setOnClickListener(this);
        mEnsurezz.setOnClickListener(this);
        mCalibration.setOnClickListener(this);
        mEnsure.setOnClickListener(this);
        mGyroMeterDefaultConfig = getResources().getBoolean(R.bool.gyro_meter_calibration_default_config_use_default_value);
        if(mGyroMeterDefaultConfig) {
            controlInvisible();
            mShowx.setVisibility(View.VISIBLE);
            mEnsurex.setVisibility(View.VISIBLE);
            mGyroMeterDefaultCompareValue = getResources().getString(R.string.gyro_meter_calibration_default_config_compare_value);
        }
        mTitle.setText(R.string.run_in_gyro_meter_calibration);
        mTimerDelay = getResources().getInteger(R.integer.gyro_meter_calibration_default_config_time_delay);
        mSaveValue = new float[6];
        mCurrentValule = new float[3];

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.gyro_meter_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        //mHandler.sendEmptyMessageDelayed(1001,2000);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    //mHandler.sendEmptyMessage(1002);
                    mHandler.sendEmptyMessage(1001);
                }
                //mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        init();

    }

    /**
     * 对象的初始化
     */
    private void init(){
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        /*if(gyroSensor==null){
            sendErrorMsg(mHandler,"gyro-meter sensor is no supper");
            return;
        }else{
            sensorManager.registerListener(sensoreventlistener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }*/
    }

    @Override
    protected void onResume() {
        if(gyroSensor==null){
            sendErrorMsg(mHandler,"gyro-meter sensor is no supper");
            return;
        }else{
            sensorManager.registerListener(sensoreventlistener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (sensorManager!=null)sensorManager.unregisterListener(sensoreventlistener);
        super.onPause();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1002:
                    deInit(mFatherName, SUCCESS);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    /**
     * 传感器的监听
     */
    private SensorEventListener sensoreventlistener=new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] value = event.values;
            mCurrentValule[SensorManager.DATA_X] = event.values[SensorManager.DATA_X];
            mCurrentValule[SensorManager.DATA_Y] = event.values[SensorManager.DATA_Y];
            mCurrentValule[SensorManager.DATA_Z] = event.values[SensorManager.DATA_Z];
            /*Message msg = mHandler.obtainMessage();
            msg.what = 2;
            msg.obj = value;
            mHandler.sendMessage(msg);*/
            LogUtil.d("mCurrentValule[0]:" + mCurrentValule[0] + " mCurrentValule[1]:" + mCurrentValule[1] + " mCurrentValule[2]:" + mCurrentValule[2]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        //if (sensorManager!=null)sensorManager.unregisterListener(sensoreventlistener);
        //mHandler.removeMessages(1001);
        //mHandler.removeMessages(1002);
        //mHandler.removeMessages(1003);
        //mHandler.removeMessages(2);
        mHandler.removeMessages(9999);
    }

    private boolean getCalibrationParameter(float act_x, float act_y, float x, float y, float a[]){
        if( (x == y) && ( act_x == act_y ) ) {
            a[0] = 1;
            a[1] = 0;
            return true;
        }
        if( x == y ) {
            LogUtil.d("x == y getCalibrationParameter act_x: " + act_x + " act_y: " + act_y + " x: " + x + " y: " + y);
            return false;
        }
        LogUtil.d("getCalibrationParameter act_x: " + act_x + " act_y: " + act_y + " x: " + x + " y: " + y);
        //act_x  = x * a[0] + a[1];
        //act_y = y * a[0] + a[1];
        a[0] = (act_x - act_y)/(x - y);
        a[1] = (act_y*x - act_x*y)/(x-y);
        LogUtil.d("getCalibrationParameter a[0]: " + a[0] + " a[1]: " + a[1]);
        return true;
    }

    private void controlInvisible(){
        mInputInfo.setVisibility(View.INVISIBLE);
        mInput.setVisibility(View.INVISIBLE);
        mEnsure.setVisibility(View.INVISIBLE);
        mShowx.setVisibility(View.INVISIBLE);
        mEnsurex.setVisibility(View.INVISIBLE);
        mShowy.setVisibility(View.INVISIBLE);
        mEnsurey.setVisibility(View.INVISIBLE);
        mShowz.setVisibility(View.INVISIBLE);
        mEnsurez.setVisibility(View.INVISIBLE);
        mShowxx.setVisibility(View.INVISIBLE);
        mEnsurexx.setVisibility(View.INVISIBLE);
        mShowyy.setVisibility(View.INVISIBLE);
        mEnsureyy.setVisibility(View.INVISIBLE);
        mShowzz.setVisibility(View.INVISIBLE);
        mEnsurezz.setVisibility(View.INVISIBLE);
        mCalibration.setVisibility(View.INVISIBLE);
    }

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

        if( v == mEnsure ){
            LogUtil.d("mEnsure");
            if(!mGyroMeterDefaultConfig)
                mGyroMeterDefaultCompareValue = mInput.getText().toString();
            LogUtil.d("mGyroMeterDefaultCompareValue:" + mGyroMeterDefaultCompareValue);
            InputMethodManager imm = (InputMethodManager) getSystemService(mContext.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            controlInvisible();
            mShowx.setVisibility(View.VISIBLE);
            mEnsurex.setVisibility(View.VISIBLE);
        }

        if( v == mEnsurex ){
            mSaveValue[0] = mCurrentValule[SensorManager.DATA_X];
            LogUtil.d("mSaveValue[0]:" + mSaveValue[0]);
            controlInvisible();
            mShowxx.setVisibility(View.VISIBLE);
            mEnsurexx.setVisibility(View.VISIBLE);
        }

        if( v == mEnsurexx ){
            mSaveValue[1] = mCurrentValule[SensorManager.DATA_X];
            LogUtil.d("mSaveValue[1]:" + mSaveValue[1]);
            controlInvisible();
            mShowy.setVisibility(View.VISIBLE);
            mEnsurey.setVisibility(View.VISIBLE);
        }

        if( v == mEnsurey ){
            mSaveValue[2] = mCurrentValule[SensorManager.DATA_Y];
            LogUtil.d("mSaveValue[2]:" + mSaveValue[2]);
            controlInvisible();
            mShowyy.setVisibility(View.VISIBLE);
            mEnsureyy.setVisibility(View.VISIBLE);
        }

        if( v == mEnsureyy ){
            mSaveValue[3] = mCurrentValule[SensorManager.DATA_Y];
            LogUtil.d("mSaveValue[3]:" + mSaveValue[3]);
            controlInvisible();
            mShowz.setVisibility(View.VISIBLE);
            mEnsurez.setVisibility(View.VISIBLE);
        }

        if( v == mEnsurez ){
            mSaveValue[4] = mCurrentValule[SensorManager.DATA_Z];
            LogUtil.d("mSaveValue[4]:" + mSaveValue[4]);
            controlInvisible();
            mShowzz.setVisibility(View.VISIBLE);
            mEnsurezz.setVisibility(View.VISIBLE);
        }

        if( v == mEnsurezz ){
            mSaveValue[5] = mCurrentValule[SensorManager.DATA_Z];
            LogUtil.d("mSaveValue[5]:" + mSaveValue[5]);
            controlInvisible();
            mCalibration.setVisibility(View.VISIBLE);
        }

        if( v == mCalibration ){
            controlInvisible();
            float parameter[] = new float[6];
            boolean flag = true;
            float positiveValue = Float.parseFloat(mGyroMeterDefaultCompareValue);
            float reverseValue = -Float.parseFloat(mGyroMeterDefaultCompareValue);
            LogUtil.d("getCalibrationParameter positiveValue: " + positiveValue + " reverseValue: " + reverseValue);

            for( int i = 0; i < 3; i++) {
                float temp[] = new float[2];
                boolean result = getCalibrationParameter(positiveValue, reverseValue, mSaveValue[i], mSaveValue[i + 1], temp);
                if( !result ) {
                    ToastUtil.showCenterLong(getString(R.string.p_sensor_calibration_fail));
                    controlInvisible();
                    if(mGyroMeterDefaultConfig) {
                        mShowx.setVisibility(View.VISIBLE);
                        mEnsurex.setVisibility(View.VISIBLE);
                    }else{
                        mInputInfo.setVisibility(View.VISIBLE);
                        mInput.setVisibility(View.VISIBLE);
                        mEnsure.setVisibility(View.VISIBLE);
                    }
                    flag = false;
                    break;
                }else {
                    parameter[i] = temp[0];
                    parameter[i+1] = temp[1];
                }
            }
            if( flag ){
                mSuccess.setVisibility(View.VISIBLE);
                LogUtil.d("getCalibration success parameter[0]: " + parameter[0] + " parameter[1]: " + parameter[1] + " parameter[2]: " + parameter[2] );
                LogUtil.d("getCalibration success parameter[3]: " + parameter[3] + " parameter[4]: " + parameter[4] + " parameter[5]: " + parameter[5]);
                    /*SystemProperties.set(getResources().getString(R.string.gyro_meter_calibration_default_config_persist_param_x_1), String.valueOf(parameter[0]));
                        SystemProperties.set(getResources().getString(R.string.gyro_meter_calibration_default_config_persist_param_x_2), String.valueOf(parameter[1]));
                        SystemProperties.set(getResources().getString(R.string.gyro_meter_calibration_default_config_persist_param_y_1), String.valueOf(parameter[2]));
                        SystemProperties.set(getResources().getString(R.string.gyro_meter_calibration_default_config_persist_param_y_2), String.valueOf(parameter[3]));
                        SystemProperties.set(getResources().getString(R.string.gyro_meter_calibration_default_config_persist_param_z_1), String.valueOf(parameter[4]));
                        SystemProperties.set(getResources().getString(R.string.gyro_meter_calibration_default_config_persist_param_z_2), String.valueOf(parameter[5]));*/
            }
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
