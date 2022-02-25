package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.Html;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;



import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;

import static com.meigsmart.meigrs32.util.DataUtil.initConfig;

public class PreGSensorActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack {

    private PreGSensorActivity mContext;
    private SensorManager mSensorManager;
    private int mConfigTime = 0;
    private String mFatherName = "";
    private int mConfigResult;
    private float initial = 0;
    private float uninitial = 0;
    //private SensorEventListener listener = null;
    private SensorManager manager = null;
    private Sensor sensor = null;
    private float mCurrentValue[];
    private float mCmpValue[];
    private  float mDifferenceValue[];
    private  int mTestFlag = 0;
    private  int mCalibrationFlag = 0;
    private Boolean mSimulationMode = false;
    private float mCmpDifferenceValue = (float)0.0;
    private int mTimeInterval = 0;
    private boolean mXstat = false;
    private boolean mYstat = false;
    private boolean mZstat = false;
    private String mGSensorCalibrationPackageNameKey = "common_sensor_calibration_Gsensor_packagename";
    private String mGSensorCalibrationPackageName = "";
    private String mGSensorCalibrationClassNameKey = "common_sensor_calibration_Gsensor_classname";
    private String mGSensorCalibrationClassName = "";

    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.show)
    public TextView mShow;
    @BindView(R.id.infoX)
    public TextView mInfoX;
    @BindView(R.id.infoY)
    public TextView mInfoY;
    @BindView(R.id.infoZ)
    public TextView mInfoZ;
    @BindView(R.id.calibration)
    public Button mCalibration;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_pre_g_sensor;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_pre_g_sensor);
        mDialog.setCallBack(this);
        mCalibration.setOnClickListener(this);
        mCurrentValue = new float[3];
        mCmpValue = new float[3];
        mDifferenceValue = new float[3];
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mConfigResult = getResources().getInteger(R.integer.g_sensor_default_config_standard_result);

        mCmpValue[0] = Float.parseFloat(getResources().getString(R.string.g_sensor_calibration_default_config_compare_value_x));
        mCmpValue[1] = Float.parseFloat(getResources().getString(R.string.g_sensor_calibration_default_config_compare_value_y));
        mCmpValue[2] = Float.parseFloat(getResources().getString(R.string.g_sensor_calibration_default_config_compare_value_z));
        mSimulationMode = getResources().getBoolean(R.bool.g_sensor_calibration_default_config_simulation_mode);
        mCmpDifferenceValue =Float.parseFloat(getResources().getString(R.string.g_sensor_calibration_default_config_difference_value));
        mTimeInterval = Integer.parseInt(getResources().getString(R.string.g_sensor_calibration_default_config_time_interval));

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mGSensorCalibrationPackageName = initConfig(Const.SENSORTEST_CONFIG_XML_PATH, mGSensorCalibrationPackageNameKey);
        if(mGSensorCalibrationPackageName.isEmpty())
            mGSensorCalibrationPackageName = "com.android.calisensors";

        mGSensorCalibrationClassName = initConfig(Const.SENSORTEST_CONFIG_XML_PATH, mGSensorCalibrationClassNameKey);
        if(mGSensorCalibrationClassName.isEmpty())
            mGSensorCalibrationClassName = "com.android.calisensors.activity.GSensorCalibrationActivity";
        LogUtil.d("citapk mGSensorCalibrationPackageName:" + mGSensorCalibrationPackageName + " mGSensorCalibrationClassName:" + mGSensorCalibrationClassName);
        // mHandler.sendEmptyMessageDelayed(1001, 1000);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert manager != null;
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(FileUtil.parserXMLTag(Const.SENSORTEST_CONFIG_XML_PATH, "g_sensor_calibration_use_3rd_app").equals("true")) {
            ComponentName componentName = new ComponentName(mGSensorCalibrationPackageName, mGSensorCalibrationClassName);
            Intent intent = new Intent();
            intent.setComponent(componentName);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (manager != null)
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }

    @Override
    protected void onPause() {
        if (manager!=null)manager.unregisterListener(listener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (manager != null) {
            manager.unregisterListener(listener);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }

        if(v == mCalibration){
            mCalibrationFlag = -1;
            mCalibration.setVisibility(View.INVISIBLE);
            mShow.setText(getResources().getString(R.string.g_sensor_calibration_instructions_xyz));
            LogUtil.d("citApk v == mCalibration");
            Message msg = mHandler.obtainMessage();
            msg.what = 1003;
            mHandler.sendMessage(msg);
            mHandler.sendEmptyMessageDelayed(1010, mTimeInterval);
        }

    }

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                    break;
                case 1003:  //set Sensor ID to persist
                    //SystemProperties.set(getResources().getString(R.string.g_sensor_calibration_default_config), String.valueOf(0));
                    break;
                case 1010:
                    LogUtil.d("citApk G sensor mCmpDifferenceValue:" + mCmpDifferenceValue);
                    LogUtil.d("citApk G sensor Math.abs(mCurrentValue[SensorManager.DATA_X]):" + Math.abs(mCurrentValue[SensorManager.DATA_X]));
                    LogUtil.d("citApk G sensor Math.abs(mCurrentValue[SensorManager.DATA_Y]):" + Math.abs(mCurrentValue[SensorManager.DATA_Y]));
                    LogUtil.d("citApk G sensor Math.abs(mCurrentValue[SensorManager.DATA_Z]):" + Math.abs(mCurrentValue[SensorManager.DATA_Z]));
                     if(Math.abs(Math.abs(mCurrentValue[SensorManager.DATA_X]) - (float)9.8) < mCmpDifferenceValue){
                         mXstat = true;
                     }
                    if(Math.abs(Math.abs(mCurrentValue[SensorManager.DATA_Y]) - (float)9.8) < mCmpDifferenceValue){
                        mYstat = true;
                    }
                    if(Math.abs(Math.abs(mCurrentValue[SensorManager.DATA_Z]) - (float)9.8) < mCmpDifferenceValue){
                        mZstat = true;
                    }
                    if( (mXstat && mYstat) || (mXstat && mZstat) || (mYstat && mZstat) ){
                        mHandler.sendEmptyMessageDelayed(1001, Const.DELAY_TIME);
                        LogUtil.d("citApk G sensor pass.");
                    }
                    mHandler.sendEmptyMessageDelayed(1010, mTimeInterval);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    /*private boolean getCalibrationParameter(float act_x, float act_y, float x, float y, float a[]) {
        if ((x == y) && (act_x == act_y)) {
            a[0] = 1;
            a[1] = 0;
            return true;
        }
        if (x == y) {
            LogUtil.d("x == y getCalibrationParameter act_x: " + act_x + " act_y: " + act_y + " x: " + x + " y: " + y);
            return false;
        }
        LogUtil.d("getCalibrationParameter act_x: " + act_x + " act_y: " + act_y + " x: " + x + " y: " + y);
        //act_x  = x * a[0] + a[1];
        //act_y = y * a[0] + a[1];
        a[0] = (act_x - act_y) / (x - y);
        a[1] = (act_y * x - act_x * y) / (x - y);
        LogUtil.d("getCalibrationParameter a[0]: " + a[0] + " a[1]: " + a[1]);
        return true;
    }*/

    @Override
    public void onResultListener(int result) {

    }

    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mCurrentValue[SensorManager.DATA_X] = event.values[SensorManager.DATA_X];
            mCurrentValue[SensorManager.DATA_Y] = event.values[SensorManager.DATA_Y];
            mCurrentValue[SensorManager.DATA_Z] = event.values[SensorManager.DATA_Z];
            if(mSimulationMode) {
                LogUtil.d("citApk g-sensor mCurrentValue[SensorManager.DATA_X]:" + mCurrentValue[SensorManager.DATA_X]);
                LogUtil.d("citApk g-sensor mCurrentValue[SensorManager.DATA_Y]:" + mCurrentValue[SensorManager.DATA_Y]);
                LogUtil.d("citApk g-sensor mCurrentValue[SensorManager.DATA_Z]:" + mCurrentValue[SensorManager.DATA_Z]);
                if (mCalibrationFlag == 0) {
                    mDifferenceValue[SensorManager.DATA_X] = mCmpValue[SensorManager.DATA_X] - mCurrentValue[SensorManager.DATA_X];
                    mDifferenceValue[SensorManager.DATA_Y] = mCmpValue[SensorManager.DATA_Y] - mCurrentValue[SensorManager.DATA_Y];
                    mDifferenceValue[SensorManager.DATA_Z] = mCmpValue[SensorManager.DATA_Z] - mCurrentValue[SensorManager.DATA_Z];
                    mCalibrationFlag = 1;
                    LogUtil.d("citApk g-sensor 1 mDifferenceValue[SensorManager.DATA_X]:" + mDifferenceValue[SensorManager.DATA_X]);
                    LogUtil.d("citApk g-sensor 1 mDifferenceValue[SensorManager.DATA_Y]:" + mDifferenceValue[SensorManager.DATA_Y]);
                    LogUtil.d("citApk g-sensor 1 mDifferenceValue[SensorManager.DATA_Z]:" + mDifferenceValue[SensorManager.DATA_Z]);
                }

                if (mCalibrationFlag == -1) {
                    mCurrentValue[SensorManager.DATA_X] = mCurrentValue[SensorManager.DATA_X] + mDifferenceValue[SensorManager.DATA_X];
                    mCurrentValue[SensorManager.DATA_Y] = mCurrentValue[SensorManager.DATA_Y] + mDifferenceValue[SensorManager.DATA_Y];
                    mCurrentValue[SensorManager.DATA_Z] = mCurrentValue[SensorManager.DATA_Z] + mDifferenceValue[SensorManager.DATA_Z];
                    LogUtil.d("citApk g-sensor -1 mCurrentValue[SensorManager.DATA_X]:" + mCurrentValue[SensorManager.DATA_X]);
                    LogUtil.d("citApk g-sensor -1 mCurrentValue[SensorManager.DATA_Y]:" + mCurrentValue[SensorManager.DATA_Y]);
                    LogUtil.d("citApk g-sensor -1 mCurrentValue[SensorManager.DATA_Z]:" + mCurrentValue[SensorManager.DATA_Z]);
                }
            }
            LogUtil.d("citApk mCurrentValule[SensorManager.DATA_X] :" + mCurrentValue[SensorManager.DATA_X] );
            LogUtil.d("citApk mCurrentValule[SensorManager.DATA_Y] :" +  mCurrentValue[SensorManager.DATA_Y] );
            LogUtil.d("citApk mCurrentValule[SensorManager.DATA_Z] :" +  mCurrentValue[SensorManager.DATA_Z] );
            mInfoX.setText(getResources().getString(R.string.g_sensor_calibration_x) + Float.toString(mCurrentValue[SensorManager.DATA_X]));
            mInfoY.setText(getResources().getString(R.string.g_sensor_calibration_y) + Float.toString(mCurrentValue[SensorManager.DATA_Y]));
            mInfoZ.setText(getResources().getString(R.string.g_sensor_calibration_z) + Float.toString(mCurrentValue[SensorManager.DATA_Z]));
        }
        @Override
        public void onAccuracyChanged(Sensor s, int accuracy) {
        }
    };
}
