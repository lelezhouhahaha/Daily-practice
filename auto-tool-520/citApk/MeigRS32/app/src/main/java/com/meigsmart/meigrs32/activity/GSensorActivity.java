package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.BindViews;

public class GSensorActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private GSensorActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.display)
    public LinearLayout mDisplay;
    private String mFatherName = "";
    @BindView(R.id.up)
    public TextView mUp;
    @BindView(R.id.down)
    public TextView mDown;
    @BindView(R.id.left)
    public TextView mLeft;
    @BindView(R.id.right)
    public TextView mRight;
    @BindViews({R.id.sesor_x,R.id.sesor_y,R.id.sesor_z})
    public List<TextView> mTextViewListSensor;
    @BindView(R.id.flag)
    public TextView mFlag;

    private Sensor sensor = null;
    private float[] mValues;

    private boolean upok = false;
    private boolean downok = false;
    private boolean leftok = false;
    private boolean rightok = false;
    private boolean alltok = false;
    private boolean center1ok = false;
    private boolean center2ok = false;

    private SensorManager manager = null;
    private static final int DATA_X = 0;
    private static final int DATA_Y = 1;
    private static final int DATA_Z = 2;
    private  float x_fir=0;
    private float y_fir=0;
    private float z_fir=0;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    String GSensorActivity_auto_judgment_key = "common_GSensorActivity_auto_judgment_bool";
    boolean B_GSensorActivity_auto_judgment = true;
    private boolean is_mt535 = false;
    private String TAG_mt535 = "common_keyboard_test_bool_config";


    @Override
    protected int getLayoutId() {
        return R.layout.activity_g_sensor;
    }
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_g_sensor);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.g_sensor_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
            mUp.setVisibility(View.GONE);
            mDown.setVisibility(View.GONE);
            mLeft.setVisibility(View.GONE);
            mRight.setVisibility(View.GONE);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GSensorActivity_auto_judgment_key);
        if(temp != null && !temp.isEmpty() && temp.equals("false")){
            B_GSensorActivity_auto_judgment = false;
            LogUtil.d("citapk GSensorActivity B_GSensorActivity_auto_judgment:false");
        }else LogUtil.d("citapk else GSensorActivity B_GSensorActivity_auto_judgment:" + B_GSensorActivity_auto_judgment);

        if (!MyApplication.PCBANAME.equals(mFatherName) && !MyApplication.PCBASignalNAME.equals(mFatherName) ) {
            mDisplay.setVisibility(View.VISIBLE);
        }

        //add for mt535
        is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
        LogUtil.d("GSensorActivity","is_mt535="+is_mt535);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert manager != null;
        if(is_mt535){
            sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }else {
            sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        }
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) /*&& mFatherName.equals(MyApplication.RuninTestNAME)*/) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                    return;
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    //int showId = 0;
                    mTextViewListSensor.get(0).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_x) + mValues[DATA_X] + "</font>"));
                    mTextViewListSensor.get(1).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_y) + mValues[DATA_Y] + "</font>"));
                    mTextViewListSensor.get(2).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_z) + mValues[DATA_Z] + "</font>"));
                    if ((!(MyApplication.PCBANAME.equals(mFatherName) || MyApplication.PCBASignalNAME.equals(mFatherName)))&& !is_mt535 && (!mFatherName.equals(MyApplication.PCBAAutoTestNAME)) ) {
                        if (mValues[DATA_Y] >= -10.0 && mValues[DATA_Y] < -7.0f) {
                            mUp.setBackgroundResource(R.drawable.arrow_up);
                            upok = true;
                        } else if (mValues[DATA_Y] >= 7.0f && mValues[DATA_Y] < 10.0) {
                            mDown.setBackgroundResource(R.drawable.arrow_down);
                            downok = true;
                        }

                    if (mValues[DATA_X] >= 7.0f && mValues[DATA_X] <10.0) {
                        mLeft.setBackgroundResource(R.drawable.arrow_left);
                        leftok = true;
                    } else if (mValues[DATA_X] >= -10.0 && mValues[DATA_X] <-7.0f) {
                        mRight.setBackgroundResource(R.drawable.arrow_right);
                        rightok = true;
                    }

                    if (mValues[DATA_Z] >= 7.0f && mValues[DATA_Z] <10.0) {
                        //mCenter1.setBackgroundColor(0xff32CD32);
                        center1ok = true;
                    } else if (mValues[DATA_Z] >= -10.0 && mValues[DATA_Z] <-7.0f) {
                        //mCenter2.setBackgroundColor(0xff32CD32);
                        center2ok = true;
                    }

                        if (upok && downok && rightok && leftok /*&& center1ok && center2ok*/) {
                            alltok = true;
                        }
                        mHandler.sendEmptyMessage(1002);
                    }else{
                        mUp.setVisibility(View.GONE);
                        mDown.setVisibility(View.GONE);
                        mLeft.setVisibility(View.GONE);
                        mRight.setVisibility(View.GONE);
                        if( (x_fir != 0 && x_fir != mValues[DATA_X]) && (y_fir != 0 && y_fir != mValues[DATA_Y]) && (z_fir != 0 && z_fir != mValues[DATA_Z])) {
                            alltok = true;
                            mHandler.sendEmptyMessage(1002);
                        }
                        x_fir = mValues[DATA_X];
                        y_fir = mValues[DATA_Y];
                        z_fir = mValues[DATA_Z];
                    }
                    break;
                case 1002:
                    if( ( !mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME) && B_GSensorActivity_auto_judgment ) || mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
                        if (alltok || mFatherName.equals(MyApplication.RuninTestNAME)){//modify by wangxing for bug P_RK95_E-706 run in log show pass

                            cleanAndUnregister();
                            deInit(mFatherName, SUCCESS);
                        }else {
                            cleanAndUnregister();
                            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                        }
                    }else {
                        if (alltok) {
                            mSuccess.setVisibility(View.VISIBLE);
                        }
                    }

                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    private SensorEventListener listener=new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor s, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            mValues = event.values;
            mHandler.sendEmptyMessage(1001);
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d("citapk onDestroy");
        cleanAndUnregister();
    }


    private void cleanAndUnregister(){
        if(manager != null && listener != null){
            LogUtil.d("citapk cleanAndUnregister ");
            manager.unregisterListener(listener);
        }
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
    }

    @Override
    protected void onResume() {
        if(manager != null && listener != null && sensor != null)
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        LogUtil.d("citapk onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.d("citapk onPause");
        if(manager != null && listener != null)
            manager.unregisterListener(listener);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            if(manager != null && listener != null)
                manager.unregisterListener(listener);
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            if(manager != null && listener != null)
                manager.unregisterListener(listener);
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
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
