package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
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
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;

import butterknife.BindView;

public class NFCandKeyBoardLightActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private NFCandKeyBoardLightActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.light)
    public TextView lights;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private int DELAY_TIME = 1000;
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    private boolean isMT535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");

    @Override
    protected int getLayoutId() {
        return R.layout.activity_light_keyboard;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mTitle.setText(super.mName);
        lights.setText(getString(R.string.light_nfc_keyboard));
        if (!MyApplication.getInstance().isConnectPaySDK()) {
            MyApplication.getInstance().bindPaySDKService();
            ToastUtil.showCenterShort(getString(R.string.connect_loading));
            return;
        }
        if(mFatherName==null){
            LogUtil.d("mFatherName==null finish()");
            finish();
            return;
        }
        mConfigResult = getResources().getInteger(R.integer.nfc_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mHandler.sendEmptyMessageDelayed(1000,DELAY_TIME);
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    deInit(mFatherName, SUCCESS);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        if(isMT535 && mFatherName.equals(MyApplication.RuninTestNAME)){
            mFail.setVisibility(View.GONE);
        }
    }

    private void ledStatus(int ledIndex, int ledStatus) {
        try {
            int result = MyApplication.getInstance().basicOptV2.ledStatusOnDevice(ledIndex, ledStatus);
            LogUtil.w("xcode","result:" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1000:
                    //mLayout.setVisibility(View.VISIBLE);
                    isStartTest = true;
                    ledStatus(AidlConstantsV2.LedLight.WHITE_LIGHT, 0);
                    mHandler.sendEmptyMessageDelayed(1001,DELAY_TIME);
                    break;
                case 1001:
                    ledStatus(AidlConstantsV2.LedLight.WHITE_LIGHT, 1);
                    mHandler.sendEmptyMessageDelayed(1000,DELAY_TIME);
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
        ledStatus(AidlConstantsV2.LedLight.WHITE_LIGHT, 1);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(9999);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
