package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class SARSensorActivity extends BaseActivity implements View.OnClickListener ,  PromptDialog.OnPromptDialogCallBack{

    private SARSensorActivity mContext;
    private SensorManager mSensorManager;
    private Sensor mSensorSAR;

    private String mFatherName = "";
    private int mConfigTime = 0;
    private Runnable mRun;
    private float mSARValue = 0.0f;
    private float mSARLeaveValue = 1.0f;
    private float mSARCoverValue = 0.0f;
    private boolean mSARLeaveFlag = false;
    private boolean mSARCoverFlag = false;
    private int countnum = 0;

    @BindView(R.id.values)
    public TextView mValues;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    //add for bug 11904 by gongming @2021-05-18
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sar_sensor;
    }

    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.SARSensorActivity);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorSAR = mSensorManager.getDefaultSensor(33171005);
        //add for bug 11904 by gongming @2021-05-20
        if(is_mt535){
            mSensorSAR = mSensorManager.getDefaultSensor(33171015);
        }
        if (mSensorSAR == null) {
            sendErrorMsg(mHandler,"SAR Sensor is null!");
            return;
        }
		mValues.setText(String.valueOf(mSARValue));

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if ((mConfigTime == 0) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    private SensorEventListener mSARListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {

            String sensorType = event.sensor.getStringType();
            //LogUtil.d("mSARListener sensorType: " + sensorType);
            if (!sensorType.equals("qti.sensor.sar")) {
                LogUtil.d("mSARListener sensorType: " + sensorType);
                return;
            }
            //LogUtil.d("mSARListener event.values[0]: " + event.values[0]);
            //LogUtil.d("mSARListener event.values[1]: " + event.values[1]);
            //LogUtil.d("mSARListener event.values[2]: " + event.values[2]);
            if(mSARValue != event.values[0]) {
                Message msg = mHandler.obtainMessage();
                msg.what = 1001;
                msg.obj = event.values[0];
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            LogUtil.d("mSARListener accuracy: " + accuracy);
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    countnum++;
                    mSARValue = (float)msg.obj;
                    mValues.setText(String.valueOf(mSARValue));
                    if(mSARValue == mSARLeaveValue){
                        mSARLeaveFlag = true;
                    }
                    if(mSARLeaveFlag && (mSARValue == mSARCoverValue)){
                        mSARCoverFlag = true;
                    }
                    LogUtil.d("citapk countnum:" + countnum + " mFatherName:" + mFatherName);
                    if(countnum >= 2) {
                        mHandler.sendEmptyMessage(1002);
                    }
                    break;
                case 1002:
                    if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                        if ((mSARLeaveFlag && mSARCoverFlag) || mFatherName.equals(MyApplication.RuninTestNAME)) {//modify by wangxing for bug P_RK95_E-706 run in log show pass
                            deInit(mFatherName, SUCCESS);
                        } else {
                            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                        }
                    }else{
                        if (mSARLeaveFlag && mSARCoverFlag) mSuccess.setVisibility(View.VISIBLE);
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
            mSensorManager.registerListener(mSARListener, mSensorSAR, 2);
    }
    @Override
    protected void onPause(){
        if (mSensorManager != null) mSensorManager.unregisterListener(mSARListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) mSensorManager.unregisterListener(mSARListener);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
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
}
