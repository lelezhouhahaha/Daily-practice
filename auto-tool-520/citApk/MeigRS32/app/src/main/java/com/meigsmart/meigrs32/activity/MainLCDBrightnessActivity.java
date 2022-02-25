package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class MainLCDBrightnessActivity extends BaseActivity implements View.OnClickListener ,
        PromptDialog.OnPromptDialogCallBack,Runnable{
    private MainLCDBrightnessActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private String mFatherName = "";
    @BindView(R.id.values)
    public TextView mValues;
    private PowerManager pm;
    private int curBackground = 50;
    private int background = 50;
    @BindView(R.id.flag)
    public TextView mFlag;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main_lcdbrightness;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);;
        mSuccess.setVisibility(View.GONE);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.run_in_mainlcd_brightness);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.lcd_brightness_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        curBackground = Settings.System.getInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 50);
        background = curBackground;

        //mFlag.setText(R.string.start_tag);
        mHandler.postDelayed(this,10);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:;
                    isStartTest = true;
                    if (MyApplication.RuninTestNAME.equals(mFatherName)){
                        deInit(mFatherName, SUCCESS);
                    }else {
                        deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                    };
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.back:
                if (!mDialog.isShowing())mDialog.show();
                mDialog.setTitle(super.mName);
                break;
            case R.id.success:
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                deInit(mFatherName, SUCCESS);
                break;
            case R.id.fail:
                mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                break;
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

    @Override
    public void run() {
        curBackground+=100;
        if(curBackground>255) {
            curBackground = 50;
        }
        if(curBackground==150){
            mSuccess.setVisibility(View.VISIBLE);
        }
        mFlag.setText(R.string.brightness_title);
        mValues.setText(String.valueOf(curBackground));
        //pm.setBacklightBrightness(curBackground);
        Settings.System.putInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, curBackground);
        mHandler.postDelayed(this,1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(9999);
        //pm.setBacklightBrightness(background);
        Settings.System.putInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, background);
    }

}