package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.log.LogUtil;

import butterknife.BindView;

public class PSensorActivity extends BaseActivity implements View.OnClickListener ,  PromptDialog.OnPromptDialogCallBack{

    private PSensorActivity mContext;
    private SensorManager mSensorManager;
    private Sensor mSensorProximity;

    private String mFatherName = "";
    private int mConfigTime = 0;
    private Runnable mRun;
    /*jicong.wang modify for bug 11652 start {@*/
    private final float DEFAULT_MAX_VALUE = 5.0f;
    private float mFloatMaxValue;
    private String PSENSOR_MAXVALUE_CONFIG_NODE = "common_psensor_maxvalue_config";
    /*jicong.wang modify for bug 11652 end @}*/
    private boolean maxValue = false;
    private boolean minValue = false;
    private final String TAG = PSensorActivity.class.getSimpleName();

    @BindView(R.id.values)
    public TextView mValues;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_p_sensor;
    }

    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.pcba_p_sensor);
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
        mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (mSensorProximity == null) {
            sendErrorMsg(mHandler,"PSensor is null!");
            return;
        }
        /*jicong.wang modify for bug 11652 start {@*/
        String value = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, PSENSOR_MAXVALUE_CONFIG_NODE);
        if(value != null && !value.isEmpty()){
            mFloatMaxValue = Float.parseFloat(value);
        } else {
            mFloatMaxValue = DEFAULT_MAX_VALUE;
        }
        LogUtil.d("mFloatMaxValue:" + mFloatMaxValue);
        /*jicong.wang modify for bug 11652 end @}*/
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                }
                if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                    if (maxValue && minValue) deInit(mFatherName, SUCCESS);
                }else{
                    if( maxValue && minValue) mSuccess.setVisibility(View.VISIBLE);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    private SensorEventListener mProximityListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_PROXIMITY) {
                LogUtil.d(TAG, "event.sensor.getType():" + event.sensor.getType());
                return;
            }
			LogUtil.d(TAG, "event.values[0]:" + event.values[0]);
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
                    LogUtil.d(TAG, " msg.obj:<" + msg.obj + "> f:<" + f + ">.");
                    mValues.setText(String.valueOf(f));
                    /*jicong.wang modify for bug 11652 start {@*/
                    if(f >= mFloatMaxValue) maxValue = true;
                    /*jicong.wang modify for bug 11652 end @}*/
                    if (f == 0) minValue = true;

                    break;
                case 1002:
                    if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                        if ((maxValue && minValue) || mFatherName.equals(MyApplication.RuninTestNAME)) {//modify by wangxing for bug P_RK95_E-706 run in log show pass

                            deInit(mFatherName, SUCCESS);
                        } else {
                            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                        }
                    }else{
                        if (maxValue && minValue) mSuccess.setVisibility(View.VISIBLE);
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
        if (mSensorManager != null) {
            LogUtil.d(TAG, "registerListener mProximityListener");
            mSensorManager.registerListener(mProximityListener, mSensorProximity, 2);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null){
            LogUtil.d(TAG, "unregisterListener mProximityListener");
            mSensorManager.unregisterListener(mProximityListener);
        }
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
