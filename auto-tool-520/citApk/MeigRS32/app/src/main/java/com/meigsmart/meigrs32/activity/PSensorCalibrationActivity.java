package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IVibratorService;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
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
import java.io.InputStream;

import butterknife.BindView;

import static com.meigsmart.meigrs32.util.DataUtil.initConfig;

public class PSensorCalibrationActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{

    private PSensorCalibrationActivity mContext;
    private SensorManager mSensorManager;
    private Sensor mSensorProximity;

    private String mFatherName = "";
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean maxValue = false;
    private boolean minValue = false;

    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.show)
    public TextView mShow;
    @BindView(R.id.value)
    public TextView mValue;
    @BindView(R.id.calibration)
    public Button mCalibration;
    @BindView(R.id.test)
    public Button mTest;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private float mCurrentValue = (float) 0.0;
    private float mCmpValue = (float) 0.0;
    private  float mDifferenceValue = (float)0.0;
    private  int mTestFlag = 0;
    private  int mCalibrationFlag = 0;
    private Boolean mSimulationMode = false;
    private float mCmpDifferenceValue = (float)0.0;
    private String mPSensorCalibrationPackageNameKey = "common_sensor_calibration_Psensor_packagename";
    private String mPSensorCalibrationPackageName = "";
    private String mPSensorCalibrationClassNameKey = "common_sensor_calibration_Psensor_classname";
    private String mPSensorCalibrationClassName = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_p_sensor_calibration;
    }

    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.pcba_p_sensor_calibration);

        mFail.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mCalibration.setOnClickListener(this);
        mTest.setOnClickListener(this);
        mCmpValue = Float.parseFloat(getResources().getString(R.string.p_sensor_calibration_default_config_compare_distance_1));
        mSimulationMode = getResources().getBoolean(R.bool.p_sensor_calibration_default_config_simulation_mode);
        mCmpDifferenceValue =Float.parseFloat(getResources().getString(R.string.p_sensor_calibration_default_config_difference_value));
        LogUtil.d("citApk mCmpValue:" + mCmpValue);
        LogUtil.d("citApk mSimulationMode:" + mSimulationMode);
        LogUtil.d("citApk mCmpDifferenceValue:" + mCmpDifferenceValue);


        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mPSensorCalibrationPackageName = initConfig(Const.SENSORTEST_CONFIG_XML_PATH, mPSensorCalibrationPackageNameKey);
        if(mPSensorCalibrationPackageName.isEmpty())
            mPSensorCalibrationPackageName = "com.android.calisensors";

        mPSensorCalibrationClassName = initConfig(Const.SENSORTEST_CONFIG_XML_PATH, mPSensorCalibrationClassNameKey);
        if(mPSensorCalibrationClassName.isEmpty())
            mPSensorCalibrationClassName = "com.android.calisensors.activity.PSensorCalibrationActivity";
        LogUtil.d("citapk mPSensorCalibrationPackageName:" + mPSensorCalibrationPackageName + " mPSensorCalibrationClassName:" + mPSensorCalibrationClassName);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (mSensorProximity == null) {
            sendErrorMsg(mHandler,"PSensor is null!");
            return;
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (mConfigTime == 0 ||
                        mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)) {
                    mHandler.sendEmptyMessage(1002);
                }
                if (maxValue && minValue) deInit(mFatherName, SUCCESS);
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        if(FileUtil.parserXMLTag(Const.SENSORTEST_CONFIG_XML_PATH, "p_sensor_calibration_use_3rd_app").equals("true")) {
            ComponentName componentName = new ComponentName(mPSensorCalibrationPackageName, mPSensorCalibrationClassName);
            Intent intent = new Intent();
            intent.setComponent(componentName);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }

    private SensorEventListener mProximityListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_PROXIMITY) {
                return;
            }

            mCurrentValue = (float)event.values[0];
            LogUtil.d("citApk 0-1 mCurrentValue:" + mCurrentValue);
            if(mSimulationMode) {
                if (mCalibrationFlag == 0) {
                    mDifferenceValue = mCmpValue - mCurrentValue;
                    mCalibrationFlag = 1;
                    LogUtil.d("citApk 1 mDifferenceValue:" + mDifferenceValue);
                }

                if ((mCalibrationFlag == -1) && (mCurrentValue > mCmpValue)) {
                    mCurrentValue = mCurrentValue + mDifferenceValue;
                    LogUtil.d("citApk -1 mCurrentValue:" + mCurrentValue);
                }
            }
            Message msg = mHandler.obtainMessage();
            msg.what = 1010;
            msg.obj = mCurrentValue;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    /*private boolean getCalibrationParameter(float act_x, float act_y, float x, float y, float a[]){
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
    }*/

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
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
                case 1003:
                    //SystemProperties.set(getResources().getString(R.string.p_sensor_calibration_default_config), String.valueOf(10)));
                    break;
                case 1005:
                    mShow.setText(getResources().getString(R.string.pcba_p_sensor_calibration_instructions_keep_off));
                    mHandler.sendEmptyMessageDelayed(1006, Const.DELAY_TIME * 2);
                    break;
                case 1006:
                    mShow.setText(getResources().getString(R.string.pcba_p_sensor_calibration_instructions_move));
                    mCalibration.setVisibility(View.INVISIBLE);
                    mTest.setVisibility(View.VISIBLE);
                    break;
                case 1010:
                    float f = (float) msg.obj;
                    mValue.setText(String.valueOf(f));
                    if(mTestFlag == -1) {
                        LogUtil.d("citApk f:" + f);
                        LogUtil.d("citApk mCmpValue:" + mCmpValue);
                        LogUtil.d("citApk mCmpDifferenceValue:" + mCmpDifferenceValue);
                        LogUtil.d("citApk Math.abs(mCurrentValue - mCmpValue):" + Math.abs(mCurrentValue - mCmpValue));
                        if ((f >= mCmpValue) &&(Math.abs(mCurrentValue - mCmpValue) < mCmpDifferenceValue) ){
                            maxValue = true;
                            LogUtil.d("citApk maxValue == true;");
                        }
                        if (f == 0) minValue = true;

                        if((maxValue == true) && (minValue == true)) {
                            mHandler.sendEmptyMessageDelayed(1001, Const.DELAY_TIME);
                            LogUtil.d("citApk (maxValue == true) && (minValue == true)");
                        }
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        if (mSensorManager != null)
            mSensorManager.registerListener(mProximityListener, mSensorProximity, 2);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) mSensorManager.unregisterListener(mProximityListener);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
        mHandler.removeMessages(1010);
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
        if(v == mCalibration){
            mCalibrationFlag = -1;
            Message msg = mHandler.obtainMessage();
            msg.what = 1003;
            mHandler.sendMessage(msg);
            mHandler.sendEmptyMessageDelayed(1005, Const.DELAY_TIME);
        }

        if(v == mTest){
            mValue.setVisibility(View.VISIBLE);
            mTestFlag = -1;
            mHandler.sendEmptyMessageDelayed(1005, Const.DELAY_TIME *2);
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
