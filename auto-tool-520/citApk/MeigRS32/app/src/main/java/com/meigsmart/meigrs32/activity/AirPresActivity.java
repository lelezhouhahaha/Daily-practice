package com.meigsmart.meigrs32.activity;


import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.view.HardwareInfoView;
import com.meigsmart.meigrs32.view.PressureView;
import com.meigsmart.meigrs32.config.RuninConfig;

import butterknife.BindView;


public class AirPresActivity extends BaseActivity implements View.OnClickListener{
    private float mPressureNumber = 1010.0F;
    private float moldPressureNumber = 1010.0F;
    private Sensor mSensorPressure;
    private SensorManager mSensorManager = null;
    private TextView tvPresNumber = null;
    private PressureView viewPres = null;
    private Button btnHardinfo = null;
    private boolean isClicked = false;
    private HardwareInfoView mHardInfo = null;
    private AirPresActivity mContext;
    private boolean startAverage = false;

    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    private Handler mHandler;
    private static final  int HANDLER_AirPress_RESULT_SUCCESS_CODE = 1015;
    private static final  int HANDLER_AirPress_RESULT_FAILURE_CODE = 1016;
    private static final  int HANDLER_AirPress_RESULT_UPDATE_CODE = 1017;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_airpress;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.AirPresActivity);
        mHandler = new MyHandler(this);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        mConfigResult = getResources().getInteger(R.integer.record_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
		
		if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
        }else{
            mSuccess.setOnClickListener(this);
            mFail.setOnClickListener(this);
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        this.tvPresNumber = ((TextView) findViewById(R.id.tv_pres_number));
        this.btnHardinfo = ((Button) findViewById(R.id.btn_pres_hardwareinfo));
        this.btnHardinfo.setOnClickListener(new BtnOnClickListener());
        this.isClicked = false;
        this.viewPres = ((PressureView) findViewById(R.id.pres_dialview));
        this.mHardInfo = ((HardwareInfoView) findViewById(R.id.pres_hardinfo));
        this.mHardInfo.setVisibility(View.INVISIBLE);
        this.mSensorManager.registerListener(this.mPressureListener,
                this.mSensorPressure, 2);
        this.mHardInfo.setSensorData(this.mSensorPressure);

        mHandler.sendEmptyMessage(HANDLER_AirPress_RESULT_UPDATE_CODE);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) && !mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME)) {
                    if(mFatherName.equals(MyApplication.RuninTestNAME)) {
                        mHandler.sendEmptyMessage(HANDLER_AirPress_RESULT_SUCCESS_CODE);
                        return;
                    }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                        if(mPressureNumber > 0){
                            mHandler.sendEmptyMessage(HANDLER_AirPress_RESULT_SUCCESS_CODE);
                        }else mHandler.sendEmptyMessage(HANDLER_AirPress_RESULT_FAILURE_CODE);
                        return;
                    }
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }
    private static class MyHandler extends Handler {
        WeakReference<Activity> reference;
        public MyHandler(Activity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            AirPresActivity activity = (AirPresActivity) reference.get();
            switch (msg.what) {
                case HANDLER_AirPress_RESULT_SUCCESS_CODE:
                    activity.deInit(activity.mFatherName, activity.SUCCESS);
                    break;
                case HANDLER_AirPress_RESULT_FAILURE_CODE:
                    activity.deInit(activity.mFatherName, activity.FAILURE, activity.getTestFailReason());
                    break;
                case HANDLER_AirPress_RESULT_UPDATE_CODE:
                    activity.updateUIInfo();
                    sendEmptyMessageDelayed(HANDLER_AirPress_RESULT_UPDATE_CODE,1000);
                    break;
            }
        }
    }

    private SensorEventListener mPressureListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_PRESSURE)
                return;
            mPressureNumber = event.values[0];

            //updateUIInfo();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void updateUIInfo()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if(startAverage){
                    mPressureNumber = (float) ((mPressureNumber * 0.25)+(0.75 * moldPressureNumber));
                }
                startAverage = true;
                moldPressureNumber = mPressureNumber;

                try{
                    SystemProperties.set(OdmCustomedProp.getPressureNumberProp(),""+mPressureNumber);
                }catch (Exception e){}

                if(mPressureNumber > 0){
                    if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                        deInit(mFatherName, SUCCESS);
                    }else{
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                }
                viewPres.setData(mPressureNumber);
                tvPresNumber.setText(String.format("%.2f",mPressureNumber) + "(mBars)");
            }
        });
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
    }

    class BtnOnClickListener
            implements View.OnClickListener
    {

        @Override
        public void onClick(View v) {
            if(!isClicked){
                isClicked = true;
                btnHardinfo.setBackgroundResource(R.drawable.btn_hard_clicked);
                mHardInfo.setVisibility(View.VISIBLE);
            }else{
                isClicked = false;
                btnHardinfo.setBackgroundResource(R.drawable.btn_hard_normal);
                mHardInfo.setVisibility(View.INVISIBLE);
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        mSensorManager.unregisterListener(mPressureListener);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(HANDLER_AirPress_RESULT_SUCCESS_CODE);
        mHandler.removeMessages(HANDLER_AirPress_RESULT_FAILURE_CODE);
        mHandler.removeMessages(HANDLER_AirPress_RESULT_UPDATE_CODE);
        this.viewPres.destroy();
        this.mHardInfo.destroy();

    }

}
