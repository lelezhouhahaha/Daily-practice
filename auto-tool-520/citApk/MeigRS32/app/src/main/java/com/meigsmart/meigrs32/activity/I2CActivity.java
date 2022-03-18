package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

import butterknife.BindView;

public class I2CActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack,Runnable {

    private I2CActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.i2cView)
    public TextView i2cView;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.startButton)
    public Button startButton;
    private String mFatherName = "";
    public String i2cValues = "/sys/devices/platform/soc/4a84000.i2c/i2c-0/0-0058/aw9523_test_i2c";
    private final String I2C_PATH_KEY = "common_cit_i2c_test_path";
    private boolean istest = false;
    private boolean TestTwice = false;

    private final String TAG = I2CActivity.class.getSimpleName();
    private Boolean mCurrentTestResult = false;
    private Runnable mRun = null;
    //rivate Handler mHandler = null;
    private int mConfigTime = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_i2c;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        //mFail.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        startButton.setOnClickListener(this);
        mTitle.setText(R.string.I2CActivity);
        mDialog.setCallBack(this);
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, I2C_PATH_KEY);
        if (!TextUtils.isEmpty(temp))
            i2cValues = temp;
        Log.d("I2CActivity", "i2cValues:" + i2cValues);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mHandler.postDelayed(this,500);

        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
            mRun = new Runnable() {
                @Override
                public void run() {
                    mConfigTime--;
                    LogUtil.d(TAG, "initData mConfigTime:" + mConfigTime);
                    updateFloatView(mContext, mConfigTime);
                    if ((mConfigTime == 0) && (mFatherName.equals(MyApplication.PCBAAutoTestNAME))) {
                        if (mCurrentTestResult) {
                            deInit(mFatherName, SUCCESS);
                        } else {
                            LogUtil.d(TAG, " Test fail!");
                            deInit(mFatherName, FAILURE, " Test fail!");
                        }
                        return;
                    }
                    mHandler.postDelayed(this, 1000);
                }
            };
            mRun.run();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
            mHandler.removeCallbacks(mRun);
        }
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
        if (v == startButton) {
            istest = true;
            startButton.setEnabled(false);
        }
    }

    private String getString(String path) {
        String prop = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            prop = reader.readLine();
            Log.d("I2CActivity", "prop==" + prop);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            Log.e("I2CActivity", "write to file " + path + " abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    istest = false;
                    i2cView.setText(R.string.testtwice);
                    startButton.setEnabled(true);
                    startButton.setText(R.string.start_twice);
                    TestTwice = true;
                    break;
                case 1002:
                    istest = false;
                    i2cView.setText(R.string.gpiocmplete);
                    mSuccess.setVisibility(View.VISIBLE);
                    if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)
                            || mFatherName.equals(MyApplication.MMI1_PreName) || mFatherName.equals(MyApplication.MMI2_PreName) || mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        mCurrentTestResult = true;
                        deInit(mFatherName, SUCCESS);//auto pass pcba
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    public void run() {
        if(istest) {
            String i2cStr = getString(i2cValues);
            int num = Integer.parseInt(i2cStr);
            Log.d("I2CActivity","num=="+num);
            if (num > 0) {
                mHandler.sendEmptyMessageDelayed(1001, 1000);
            }
            if(TestTwice){
                if(num<0){
                    mHandler.sendEmptyMessageDelayed(1002, 1000);
                }
            }
        }
        mHandler.postDelayed(this,500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 11 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
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