package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.cpuservice.CpuService1;
import com.meigsmart.meigrs32.cpuservice.CpuTest;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;


import butterknife.BindView;

public class CpuActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private CpuActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private CpuTest mCpuTest;
    private String mFatherName = "";

    private int mConfigTime = 0;
    private Runnable mRun;
    private int count = 0;
    private int cpu_use_threshold = 0;
    private String cpu_temperature_path;
    private Boolean mIsRuning = true;
    private String CPU_Temperature_Check_Max_Value_Key = "common_cpu_test_check_max_temperature_int";
    private String CPU_Temperature_Check_Min_Value_Key = "common_cpu_test_check_min_temperature_int";
    private int mCpuMaxValue = 0;
    private int mCpuMinValue = 0;
    private Boolean mCpuTestStatus = false;    //true:success false:fail
    private Boolean mCpuTemperatureStatus = false;   //true: cpu temperature normal        fail:cpu temperature abnormal
    private final String TAG = CpuActivity.class.getSimpleName();
    boolean isCanSuccess =false;
    private boolean isMT537_version =false;
    private String projectName = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cpu;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_cpu);
        mDialog.setCallBack(this);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        isMT537_version = "MT537".equals(projectName);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        Log.d(TAG, " mConfigTime:" + mConfigTime);
        String tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CPU_Temperature_Check_Max_Value_Key);
        if(tmpStr.isEmpty())
            mCpuMaxValue = 75;
        else mCpuMaxValue = Integer.parseInt(tmpStr);

        tmpStr = "";
        tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CPU_Temperature_Check_Min_Value_Key);
        if(tmpStr.isEmpty())
            mCpuMinValue = 50;
        else mCpuMinValue = Integer.parseInt(tmpStr);
        cpu_use_threshold = getResources().getInteger(R.integer.cpu_default_config_threshold);
        if(isMT537_version) {
            cpu_temperature_path = getResources().getString(R.string.cpu_default_config_cpu_temperature_path_new);
        }else {
            cpu_temperature_path = getResources().getString(R.string.cpu_default_config_cpu_temperature_path);
        }
        Log.d("MEIGCpuTest", "cpu mCpuMaxValue:" + mCpuMaxValue);
        Log.d("MEIGCpuTest", "cpu mCpuMinValue:" + mCpuMinValue);
        init();

        mRun = new Runnable() {
            @Override
            public void run() {
                if(mIsRuning) {
                    mConfigTime--;
                    updateFloatView(mContext, mConfigTime);
                    if ((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) {
                        mHandler.sendEmptyMessage(1003);
                    }else if(mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)){
                        mHandler.sendEmptyMessage(1004);
                    }
                    mHandler.postDelayed(this, 1000);
                }
            }
        };
        mRun.run();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsRuning = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCpu();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
        mHandler.removeMessages(9999);
        mIsRuning = false;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    calculateCpuUsage();
                    break;
                case 1002:
                    useCpu();
                    break;
                case 1003:
                    if(mCpuTemperatureStatus){
                        deInit(mFatherName, SUCCESS);
                    }else{
                        Log.d(TAG, "handler 1003 cpu Temperature read abnormal");
                        deInit(mFatherName, FAILURE,getTestFailReason());
                    }
                    /*if(mCpuTestStatus){
                        deInit(mFatherName, SUCCESS);
                    }*/
                    break;
                case 1004:
                    deInit(mFatherName, SUCCESS);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mBack){
            stopCpu();
            mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void init(){
        this.mCpuTest = new CpuTest(this,mHandler);
        this.mCpuTest.start();
    }

    private void stopCpu(){
        if (this.mCpuTest != null) {
            this.mCpuTest.stop();
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
		mHandler.removeMessages(9999);
    }
    int temperature= 0;
    private void calculateCpuUsage() {
        new Thread(new Runnable() {
            public void run() {
                if(mIsRuning) {
                    float f = DataUtil.readCPUUsage();
                    //if (f >= cpu_use_threshold) count++;
                    String str = "Cpu Usage : " + f + "%";
                    updateCpuInfoText(str);
                    Log.d("MEIGCpuTest","f:"+f);
                    Log.d("MEIGCpuTest","cpu_use_threshold:"+cpu_use_threshold);
                    Log.d("MEIGCpuTest","temperature:"+temperature);
                    /*if(!mCpuTestStatus && ( f == 0.0F )){
                        mCpuTemperatureStatus = false;
                    }
                    if (f > cpu_use_threshold) {
                        isCanSuccess =true;*/
                        String temp = DataUtil.readLineFromFile(cpu_temperature_path);
                        if(temp!=null&&!temp.isEmpty()&&!temp.equals("")){
                            temperature  = Integer.parseInt(temp) / 10;
                            if(isMT535_version||isMT537_version){
                                temperature = temperature/100;
                                Log.d("GM","temperature:"+temperature);
                            }
                        }else{
							LogUtil.d(TAG, "read temperature abnormal!");
						}
                        if (temperature > mCpuMaxValue) {
                            Log.d(TAG, Const.RESULT_FAILURE + " cpu usage:" + f + "% cpu temperature:" + temperature);
                            Log.d("MEIGCpuTest","temperature_fail:"+temperature);
                            Log.d("MEIGCpuTest","mCpuMaxValue_fail:"+mCpuMaxValue);
                            mCpuTemperatureStatus = false;
                            setTestFailReason(String.format(getResources().getString(R.string.fail_reason_cputest_cpu_temperature), mCpuMaxValue ));
                        }else {
                            mCpuTemperatureStatus= true;
                        }
                    /*}else{
                        Log.d("MEIGCpuTest","f_fail:"+f);
                        Log.d("MEIGCpuTest","isCanSuccess:"+isCanSuccess);
                        if(!isCanSuccess){
                            setTestFailReason(String.format(getResources().getString(R.string.fail_reason_cputest_cpu_usage), cpu_use_threshold ));
                        }
                    }*/
                    mHandler.sendEmptyMessageDelayed(1001, 1000L);
                }
            }
        }, "CalCpu").start();
    }

    private void updateCpuInfoText(String paramString) {
        Intent localIntent = new Intent(this, CpuService1.class);
        localIntent.putExtra("update", true);
        localIntent.putExtra("level", paramString);
        startService(localIntent);
    }

    private void useCpu() {
        new Thread(new Runnable() {
            public void run() {
                int i = 500000000;
                for (; ; ) {
                    if(mIsRuning == false) {
                        break;
                    }
                    if (i == 1) {
                        mHandler.sendEmptyMessageDelayed(1002, 500L);
                        break;
                    }
                    i -= 1;
                }
            }
        }, "UseCpu").start();
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
