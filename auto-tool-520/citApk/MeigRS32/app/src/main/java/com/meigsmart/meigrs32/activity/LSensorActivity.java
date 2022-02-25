package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import butterknife.BindView;



public class LSensorActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private LSensorActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.values)
    public TextView mValues;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.success_min)
    public TextView minSuccessValue;
    @BindView(R.id.success_max)
    public TextView maxSuccessValue;
    @BindView(R.id.l_sensor_disable)
    public TextView mLSensorDisable;
    @BindView(R.id.tags)
    public RelativeLayout mTagsRl;

    private SensorManager mSensorManager;
    private Sensor mSensorLight;

    private int  maxConfigResult ;
    private int  minConfigResult ;
    private int mConfigTime = 0;

    private boolean isMixValue = false;
    private boolean isMaxValue = false;
    private Runnable mRun;
    private String TAG_CONFIG = "sensor_config";
    private String TAG_TEST = "sensor_test";
    private String TAG_L_SENSOR_SUCCESS_MIN_THRESHOLD = "l_sensor_config_success_min_threshold";
    private String TAG_L_SENSOR_SUCCESS_MAX_THRESHOLD = "l_sensor_config_success_max_threshold";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_l_sensor;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.run_in_l_sensor);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        maxConfigResult = getResources().getInteger(R.integer.l_sensor_default_config_standard_max_result);
        minConfigResult = getResources().getInteger(R.integer.l_sensor_default_config_standard_min_result);

        //mConfigResult = getResources().getInteger(R.integer.l_sensor_default_config_standard_result);
        initConfig();
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("maxConfigResult:" + maxConfigResult +"minConfigResult:" + minConfigResult+ " mConfigTime:" + mConfigTime);

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
                updateFloatView(mContext, mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                }
                if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME)) {
                    if (isMixValue && isMaxValue) deInit(mFatherName, SUCCESS);
                } else {
                    if (isMixValue && isMaxValue) mSuccess.setVisibility(View.VISIBLE);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    private void initConfig() {
        File configFile = new File(Const.SENSORTEST_CONFIG_XML_PATH);
        if (!configFile.exists()) {
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.SENSORTEST_CONFIG_XML_PATH));
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return;
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
                        if(TAG_L_SENSOR_SUCCESS_MAX_THRESHOLD.equals(startTagName)) {
                            String str = xmlPullParser.nextText();
                            if(str != null && str != ""){
                                maxConfigResult = Integer.parseInt(str);
                                LogUtil.d("citapp maxConfigResult:<" + maxConfigResult + ">.");
                            }
                        }
                        else if(TAG_L_SENSOR_SUCCESS_MIN_THRESHOLD.equals(startTagName)){
                            String str = xmlPullParser.nextText();
                            if(str != null && str != ""){
                                minConfigResult = Integer.parseInt(str);
                                LogUtil.d("citapp minConfigResult:<" + minConfigResult + ">.");
                            }
                        }
                        break;
                }
                type = xmlPullParser.next();
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
        maxSuccessValue.setText(String.format(getString(R.string.l_sensor_tag2), maxConfigResult));
        minSuccessValue.setText(String.format(getString(R.string.l_sensor_tag1), minConfigResult));
    }



    private SensorEventListener mLightListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_LIGHT) {
                return;
            }
            Message msg = mHandler.obtainMessage();
            msg.what = 1001;
            msg.obj = event.values[0];
            mHandler.sendMessage(msg);
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
                    float f = (float) msg.obj;
                    mValues.setText(String.valueOf(f));
                    if (f < minConfigResult) {
                        isMixValue = true;
                    }
                    if (f > maxConfigResult) {
                        isMaxValue = true;
                    }
                    break;
                case 1002:
                    if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                        if ((isMixValue && isMaxValue) || mFatherName.equals(MyApplication.RuninTestNAME)) {//modify by wangxing for bug P_RK95_E-706 run in log show pass

                            deInit(mFatherName, SUCCESS);
                        } else {
                            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                        }
                    }else{
                        if (isMixValue && isMaxValue) mSuccess.setVisibility(View.VISIBLE);
                    }
                    break;
                case 9999:
//                    deInit(FAILURE,msg.obj.toString());
                    mTagsRl.setVisibility(View.GONE);
                    mLSensorDisable.setVisibility(View.VISIBLE);
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
    protected void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) mSensorManager.unregisterListener(mLightListener);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
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
