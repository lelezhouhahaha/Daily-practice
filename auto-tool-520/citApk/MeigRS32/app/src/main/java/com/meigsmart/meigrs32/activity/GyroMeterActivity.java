package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.widget.Button;
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

public class GyroMeterActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private GyroMeterActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindViews({R.id.gyro_x,R.id.gyro_y,R.id.gyro_z})
    public List<TextView> mGyroList;
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private SensorManager sensorManager;//管理器对象
    private Sensor gyroSensor;//传感器对象

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean isFinished = false;
    private float timestamp = 0;
    private float angle[] = new float[3];
    private float value[] = new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_gyro_meter;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.run_in_gyro_meter);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.gyro_meter_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mHandler.sendEmptyMessageDelayed(1001,getResources().getInteger(R.integer.start_delay_time));

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if( ( mConfigTime == 0 ) && ( !mFatherName.isEmpty() ) && mFatherName.equals(MyApplication.PCBAAutoTestNAME) ){
                    mHandler.sendEmptyMessage(1002);
                    return;
                }
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                }
                mHandler.postDelayed(this, 1000);
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
        if(gyroSensor==null){
            sendErrorMsg(mHandler,"gyro-meter sensor is no supper");
            return;
        }else{
            sensorManager.registerListener(sensoreventlistener, gyroSensor, 2*1000*1000/*SensorManager.SENSOR_DELAY_NORMAL*/);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    mFlag.setVisibility(View.GONE);
                    mGyroList.get(0).setText(Html.fromHtml(getResources().getString(R.string.gyro_x_angle)+"&nbsp;"+Float.toString(0)));
                    mGyroList.get(1).setText(Html.fromHtml(getResources().getString(R.string.gyro_y_angle)+"&nbsp;"+Float.toString(0)));
                    mGyroList.get(2).setText(Html.fromHtml(getResources().getString(R.string.gyro_z_angle)+"&nbsp;"+Float.toString(0)));
                    break;
                case 1002:
                    if( (!mFatherName.isEmpty()) && mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                        if(isFinished){
                            deInit(mFatherName, SUCCESS);
                        }else deInit(mFatherName, FAILURE, "sensor unreported value!");
                        break;
                    }
                    if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                        deInit(mFatherName, SUCCESS);
                    }else {
                        if(isFinished)
                            mSuccess.setVisibility(View.VISIBLE);
                    }
                    break;
                case 2:
                    float[] f = (float[]) msg.obj;
                    mGyroList.get(0).setText(Html.fromHtml(getResources().getString(R.string.gyro_x_angle)+"&nbsp;"+Float.toString(f[0])));
                    mGyroList.get(1).setText(Html.fromHtml(getResources().getString(R.string.gyro_y_angle)+"&nbsp;"+Float.toString(f[1])));
                    mGyroList.get(2).setText(Html.fromHtml(getResources().getString(R.string.gyro_z_angle)+"&nbsp;"+Float.toString(f[2])));
                    if((f[0] != 0.0 && f[1] != 0.0 && f[2] != 0.0) && !isFinished){
                        isFinished = true;
                        mHandler.sendEmptyMessage(1002);
                    }
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
            if (timestamp!= 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;

                angle[0] = event.values[0] * dT;
                angle[1] = event.values[1] * dT;
                angle[2] = event.values[2] * dT;
                value[0] = (float) Math.toDegrees(angle[0]);
                value[1] = (float) Math.toDegrees(angle[1]);
                value[2] = (float) Math.toDegrees(angle[2]);

                if((float)Math.abs(value[0]) < 0.5f)
                    value[0] = (float)0.0;
                if((float)Math.abs(value[1]) < 0.5f)
                    value[1] = (float)0.0;
                if((float)Math.abs(value[2]) < 0.5f)
                    value[2] = (float)0.0;
            }
            timestamp = event.timestamp;
            Message msg = mHandler.obtainMessage();
            msg.what = 2;
            msg.obj = value;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        if (sensorManager!=null)sensorManager.unregisterListener(sensoreventlistener);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(2);
        mHandler.removeMessages(9999);
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
