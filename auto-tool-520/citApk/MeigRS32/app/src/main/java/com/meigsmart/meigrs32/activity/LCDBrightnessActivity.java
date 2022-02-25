package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
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
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

import butterknife.BindView;

public class LCDBrightnessActivity extends BaseActivity implements View.OnClickListener ,
        PromptDialog.OnPromptDialogCallBack,Runnable{
    private LCDBrightnessActivity mContext;
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
    private int resetBrightness = 50;
    private int background = 50;
    private int default_brightness =50;
    @BindView(R.id.flag)
    public TextView mFlag;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private final String SYSTEM_KEY = SCREEN_BRIGHTNESS_MODE;
    private final int DEFAULT_VALUE = SCREEN_BRIGHTNESS_MODE_MANUAL;
    private int mCurrentScreenBrightnessMode = 0;
    private final String TAG = LCDBrightnessActivity.class.getSimpleName();
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    private boolean isMT535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");

    @Override
    protected int getLayoutId() {
        return R.layout.activity_lcd_brightness;
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
        mTitle.setText(R.string.run_in_lcd_brightness);

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
        mCurrentScreenBrightnessMode = Settings.System.getInt(mContext.getContentResolver(), SYSTEM_KEY, DEFAULT_VALUE);
        Log.d(TAG, "mCurrentScreenBrightnessMode:" + mCurrentScreenBrightnessMode);
        Log.d(TAG, "DEFAULT_VALUE:" + DEFAULT_VALUE);
        Log.d(TAG, "SCREEN_BRIGHTNESS_MODE_AUTOMATIC:" + SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        if(mCurrentScreenBrightnessMode != DEFAULT_VALUE){
            Settings.System.putInt(mContext.getContentResolver(), SYSTEM_KEY, DEFAULT_VALUE);
        }

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        resetBrightness = Settings.System.getInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 50);
        background = resetBrightness;
        default_brightness =resetBrightness;
        LogUtil.d("LCDBrightnessActivity","curBackground: "+resetBrightness);

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

        if(isMT535 && mFatherName.equals(MyApplication.RuninTestNAME)){
            mFail.setVisibility(View.GONE);
        }
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
                    }
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                Settings.System.putInt(getApplicationContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, resetBrightness);
            }
        }).start();
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
        background+=100;
        if(background>255) {
            background = 50;
        }
        if(background==150){
            mSuccess.setVisibility(View.VISIBLE);
        }
        mFlag.setText(R.string.brightness_title);
        mValues.setText(String.valueOf(background));
        //pm.setBacklightBrightness(curBackground);
        Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, background);
        mHandler.postDelayed(this,1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCurrentScreenBrightnessMode != DEFAULT_VALUE){
            Settings.System.putInt(mContext.getContentResolver(), SYSTEM_KEY, mCurrentScreenBrightnessMode);
        }
        Log.d(TAG, "SYSTEM_KEY:" +  Settings.System.getInt(mContext.getContentResolver(), SYSTEM_KEY, DEFAULT_VALUE));
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(9999);
        //pm.setBacklightBrightness(background);
        Settings.System.putInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, default_brightness);
    }

}
