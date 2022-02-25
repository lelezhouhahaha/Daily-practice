package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
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
import java.io.FileOutputStream;

import butterknife.BindView;

public class IRBrightnessActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack,Runnable{
    private IRBrightnessActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    private static final String INFRARED_LED_NODE_KEY = "infrared_led_node";
    private String mInfraredLedNodePath;

    private static final String LED_ON = "255";
    private static final String LED_OFF = "0";

    private static final int MSG_SET_LED_ON = 1002;
    private static final int MSG_SET_LED_OFF = 1003;
    private static final int MSG_SHOW_SUCCESS = 8888;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ir_brightness;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_ir_brightness);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mInfraredLedNodePath = DataUtil.initConfig(Const.CIT_NODE_CONFIG_PATH, INFRARED_LED_NODE_KEY);

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

        mHandler.sendEmptyMessage(MSG_SET_LED_ON);
        if(!mInfraredLedNodePath.isEmpty())
            mHandler.sendEmptyMessageDelayed(MSG_SHOW_SUCCESS, 3000);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
                case MSG_SET_LED_ON:
                    setLED(LED_ON);
                    sendEmptyMessageDelayed(MSG_SET_LED_OFF,500);
                    break;
                case MSG_SET_LED_OFF:
                    setLED(LED_OFF);
                    sendEmptyMessageDelayed(MSG_SET_LED_ON,500);
                    break;
                case MSG_SHOW_SUCCESS:
                    mSuccess.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    private void setLED(String ledString) {
        byte ledData[] = ledString.getBytes();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(mInfraredLedNodePath);
            fileOutputStream.write(ledData);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1001);
        setLED(LED_OFF);
        mHandler.removeMessages(MSG_SET_LED_ON);
        mHandler.removeMessages(MSG_SET_LED_OFF);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
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
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

    }
}