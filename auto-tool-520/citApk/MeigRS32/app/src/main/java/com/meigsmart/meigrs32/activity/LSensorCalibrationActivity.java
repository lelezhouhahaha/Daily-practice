package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Xml;
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
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import butterknife.BindView;

public class LSensorCalibrationActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private LSensorCalibrationActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.show)
    public TextView mShow;
    @BindView(R.id.value)
    public TextView mValue;
    @BindView(R.id.calibration)
    public Button mCalibration;
    @BindView(R.id.test)
    public Button mTest;
    private String mFatherName = "";


    private SensorManager mSensorManager;
    private Sensor mSensorLight;

    private int mConfigResult;
    private int mConfigTime = 0;
    private float mCurrentValue = (float) 0.0;
    private float mCmpValue = (float) 0.0;
    private  float mDifferenceValue = (float)0.0;
    private  int mTestFlag = 0;
    private  int mCalibrationFlag = 0;
    private Boolean mSimulationMode = false;
    private float mCmpDifferenceValue = (float)0.0;
    private int mTimeInterval = 0;

    private boolean isMixValue = false;
    private boolean isMaxValue = false;
    private Runnable mRun;
    private String mLSensorCalibrationPackageNameKey = "common_sensor_calibration_Lsensor_packagename";
    private String mLSensorCalibrationPackageName = "";
    private String mLSensorCalibrationClassNameKey = "common_sensor_calibration_Lsensor_classname";
    private String mLSensorCalibrationClassName = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_l_sensor_calibration;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mCalibration.setOnClickListener(this);
        mTest.setOnClickListener(this);
        mTitle.setText(R.string.run_in_l_sensor_calibration);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        //mConfigResult = getResources().getInteger(R.integer.l_sensor_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mLSensorCalibrationPackageName = initConfig(Const.SENSORTEST_CONFIG_XML_PATH, mLSensorCalibrationPackageNameKey);
        if(mLSensorCalibrationPackageName.isEmpty())
            mLSensorCalibrationPackageName = "com.android.calisensors";

        mLSensorCalibrationClassName = initConfig(Const.SENSORTEST_CONFIG_XML_PATH, mLSensorCalibrationClassNameKey);
        if(mLSensorCalibrationClassName.isEmpty())
            mLSensorCalibrationClassName = "com.android.calisensors.activity.LSensorCalibrationActivity";

        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        LogUtil.d("citapk mLSensorCalibrationPackageName:" + mLSensorCalibrationPackageName + " mLSensorCalibrationClassName:" + mLSensorCalibrationClassName);
        //mCmpValue = Float.parseFloat(getResources().getString(R.string.l_sensor_calibration_default_config_compare_value));
        String tmpStr = initConfig(Const.SENSORTEST_CONFIG_XML_PATH, "l_sensor_calibration_config_success_threshold");
        if(!tmpStr.isEmpty())
            mCmpValue = Float.parseFloat(tmpStr);
        mSimulationMode = getResources().getBoolean(R.bool.l_sensor_calibration_default_config_simulation_mode);
        mCmpDifferenceValue =Float.parseFloat(getResources().getString(R.string.l_sensor_calibration_default_config_difference_value));
        mTimeInterval = Integer.parseInt(getResources().getString(R.string.l_sensor_calibration_default_config_time_interval));
        LogUtil.d("citApk mCmpValue:" + mCmpValue);
        LogUtil.d("citApk mSimulationMode:" + mSimulationMode);
        LogUtil.d("citApk mCmpDifferenceValue:" + mCmpDifferenceValue);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (mSensorLight == null) {
            sendErrorMsg(mHandler,"LSensor is null!");
            return;
        }
        LogUtil.w(mSensorLight.getName() + "  " + mSensorLight.getType() + " " + mSensorLight.toString());
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                }
                if (isMixValue && isMaxValue) deInit(mFatherName, SUCCESS);
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        if(FileUtil.parserXMLTag(Const.SENSORTEST_CONFIG_XML_PATH, "l_sensor_calibration_use_3rd_app").equals("true")) {
            //ComponentName componentName = new ComponentName("com.android.calisensors", "com.android.calisensors.activity.LSensorCalibrationActivity");
            ComponentName componentName = new ComponentName(mLSensorCalibrationPackageName, mLSensorCalibrationClassName);
            Intent intent = new Intent();
            intent.setComponent(componentName);
            startActivityForResult(intent, 1);
        }
    }

    private String initConfig(String path, String tag) {
        File configFile = new File(path);
        String tagValue = "";
        if (!configFile.exists()) {
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.SENSORTEST_CONFIG_XML_PATH));
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return tagValue;
        }
        try {
            InputStream inputStream = new FileInputStream(Const.SENSORTEST_CONFIG_XML_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");
            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if(tag.equals(startTagName)) {
                            tagValue = xmlPullParser.nextText();
                            LogUtil.d("citapp mConfigResult:<" + mConfigResult + ">.");
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return tagValue;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }

    /*private boolean getCalibrationParameter(float act_x, float act_y, float x, float y, float a[]){
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
    }*/

    private SensorEventListener mLightListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_LIGHT) {
                return;
            }
            mCurrentValue = event.values[0];
            if(mSimulationMode) {
                LogUtil.d("citApk l-sensor mCurrentValue:" + mCurrentValue);
                if (mCalibrationFlag == 0) {
                    mDifferenceValue = mCmpValue - mCurrentValue;
                    mCalibrationFlag = 1;
                    LogUtil.d("citApk l-sensor 1 mDifferenceValue:" + mDifferenceValue);
                }

                if ((mCalibrationFlag == -1) && (mCurrentValue > mCmpValue)) {
                    mCurrentValue = mCurrentValue + mDifferenceValue;
                    LogUtil.d("citApk l-sensor -1 mCurrentValue:" + mCurrentValue);
                }
            }
            mValue.setText(String.valueOf(mCurrentValue));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

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
                case 1003:  //set Sensor ID to persist
                    //SystemProperties.set(getResources().getString(R.string.l_sensor_calibration_default_config), String.valueOf(40)));
                    break;
                case 1005:
                    mShow.setText(getResources().getString(R.string.pcba_p_sensor_calibration_instructions_keep_off));
                    mHandler.sendEmptyMessageDelayed(1006, Const.DELAY_TIME*2);
                    break;
                case 1006:
                    mShow.setText(getResources().getString(R.string.pcba_p_sensor_calibration_instructions_move));
                    mTest.setVisibility(View.VISIBLE);
                    if(mTestFlag == -1) {
                        mHandler.sendEmptyMessageDelayed(1010, mTimeInterval);
                        String valueStr = getResources().getString(R.string.pcba_p_sensor_calibration_instructions_move);
                        valueStr += String.format(getResources().getString(R.string.l_sensor_calibration_instructions_end_time), (mTimeInterval/1000));
                        mShow.setText(valueStr);
                    }
                    break;
                case 1010:
                    if(mTestFlag == -1) {
                        LogUtil.d("citApk L sensor mCmpValue:" + mCmpValue);
                        LogUtil.d("citApk L sensor mCmpDifferenceValue:" + mCmpDifferenceValue);
                        LogUtil.d("citApk L sensor Math.abs(mCurrentValue - mCmpValue):" + Math.abs(mCurrentValue - mCmpValue));
                        if((Math.abs(mCurrentValue - mCmpValue) < mCmpDifferenceValue) ){
                            mHandler.sendEmptyMessageDelayed(1001, Const.DELAY_TIME);
                            LogUtil.d("citApk L sensor pass.");
                        }else {
                            ToastUtil.showCenterLong(getResources().getString(R.string.l_sensor_calibration_fail));
                            mHandler.sendEmptyMessage(1002);
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
        super.onResume();
        if (mSensorManager != null)
            mSensorManager.registerListener(mLightListener, mSensorLight, 2);
    }

    @Override
    protected void onPause() {
        if (mSensorManager!=null)mSensorManager.unregisterListener(mLightListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) mSensorManager.unregisterListener(mLightListener);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
        mHandler.removeMessages(1010);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
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

        if(v == mCalibration){
            mCalibrationFlag = -1;
            mCalibration.setVisibility(View.INVISIBLE);
            Message msg = mHandler.obtainMessage();
            msg.what = 1003;
            mHandler.sendMessage(msg);
            mHandler.sendEmptyMessageDelayed(1005, mTimeInterval);
            String valueStr = String.format(getResources().getString(R.string.l_sensor_calibration_instruction_start_time), (mTimeInterval/1000));
            valueStr += getResources().getString(R.string.l_sensor_calibration_instructions_keep_off);
            mShow.setText(valueStr);
        }

        if(v == mTest){
            mTestFlag = -1;
            mValue.setVisibility(View.VISIBLE);
            mHandler.sendEmptyMessageDelayed(1005, Const.DELAY_TIME );
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
