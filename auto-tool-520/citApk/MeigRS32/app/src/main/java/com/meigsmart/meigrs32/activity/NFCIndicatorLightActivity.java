package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
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
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;


import butterknife.BindView;

public class NFCIndicatorLightActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack,Runnable{
    private NFCIndicatorLightActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    @BindView(R.id.content_show)
    public TextView textView;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private String TAG = "NFCIndicatorLightActivity";
    private boolean led_auto_show_flag = false;
    private Handler mDelayHandler= null;
    private Runnable mDelayRunnable= null;
    private int DELAY_TIME = 1000;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_led;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.NFCIndicatorLight_Test);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mHandler.sendEmptyMessage(1001);
        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        if (!MyApplication.getInstance().isConnectPaySDK()) {
            MyApplication.getInstance().bindPaySDKService();
            ToastUtil.showCenterShort(getString(R.string.connect_loading));
            return;
        }
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
    void clearAllTestLed(){
        ledStatus(AidlConstantsV2.LedLight.RED_LIGHT, 1);
        ledStatus(AidlConstantsV2.LedLight.BLUE_LIGHT, 1);
        ledStatus(AidlConstantsV2.LedLight.YELLOW_LIGHT, 1);
        ledStatus(AidlConstantsV2.LedLight.GREEN_LIGHT, 1);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    ledStatus(AidlConstantsV2.LedLight.BLUE_LIGHT, 0);
                    textView.setText(R.string.Blue);
                    mHandler.sendEmptyMessageDelayed(1002,DELAY_TIME);
                    break;
                case 1002:
                    ledStatus(AidlConstantsV2.LedLight.YELLOW_LIGHT, 0);
                    textView.setText(R.string.Yellow);
                    mHandler.sendEmptyMessageDelayed(1003,DELAY_TIME);
                    break;
                case 1003:
                    ledStatus(AidlConstantsV2.LedLight.GREEN_LIGHT, 0);
                    textView.setText(R.string.Green);
                    mHandler.sendEmptyMessageDelayed(1004,DELAY_TIME);
                    break;
                case 1004:
                    ledStatus(AidlConstantsV2.LedLight.RED_LIGHT, 0);
                    textView.setText(R.string.Red);
                    mHandler.sendEmptyMessageDelayed(1005,DELAY_TIME);
                    break;
                case 1005:
                    textView.setText(R.string.nfc_led);
                    mSuccess.setVisibility(View.VISIBLE);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            led_auto_show_flag = true;
            clearAllTestLed();
            if(mDelayHandler != null){
                mDelayHandler.removeCallbacks(mDelayRunnable);
                mDelayHandler = null;
            }
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            led_auto_show_flag = true;
            clearAllTestLed();

            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }else {
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

    /**
     * operate LED light status
     *
     * @param ledIndex  The led index,1~4,1-Red，2-Green，3-Yellow，4-Blue
     * @param ledStatus LED Status，0-ON, 1-OFF
     */
    private void ledStatus(int ledIndex, int ledStatus) {
        try {
            int result = MyApplication.getInstance().basicOptV2.ledStatusOnDevice(ledIndex, ledStatus);
            LogUtil.w("xcode","result:" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
