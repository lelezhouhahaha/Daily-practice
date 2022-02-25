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

public class GpioSunmiActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack,Runnable {

    private GpioSunmiActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.gpiovalue)
    public TextView Gpiovalue;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    public String gpioOutput = "/sys/devices/virtual/sunmi_uhf/uhf/apint";
    public String gpioIntput = "/sys/devices/virtual/sunmi_uhf/uhf/multi";
    private final String GPIO_PATH_OUTPUT = "common_cit_gpio_output_path";
    private final String GPIO_PATH_INTPUT = "common_cit_gpio_intput_path";
    private boolean testone = true;
    private boolean testtwo = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_gpio_sunmi;
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
        mTitle.setText(R.string.GpioSunmiActivity);
        mDialog.setCallBack(this);
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO_PATH_OUTPUT);
        if (!TextUtils.isEmpty(temp))
            gpioOutput = temp;
        String temp1 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO_PATH_INTPUT);
        if (!TextUtils.isEmpty(temp1))
            gpioIntput = temp1;
        Log.d("GpioSunmiActivity", "gpioOutput:" + gpioOutput);
        Log.d("GpioSunmiActivity", "gpioIntput:" + gpioIntput);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mHandler.postDelayed(this,100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        writeToFile(gpioOutput, "0");
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
    }

    private String getString(String path) {
        String prop = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            prop = reader.readLine();
            Log.d("GpioSunmiActivity", "prop==" + prop);
            // Gpiovalue.setText(prop);
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
            Log.e("GpioSunmiActivity", "write to file " + path + " abnormal.");
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
                case 1000:
                    Log.d("GpioSunmiActivity","gpioIn11==="+getString(gpioIntput));
                    if(getString(gpioIntput).equals("1")){
                        testone = false;
                        testtwo = true;
                    }
                    break;
                case 1001:
                    Log.d("GpioSunmiActivity","gpioIn22==="+getString(gpioIntput));
                    if(getString(gpioIntput).equals("0")){
                        testtwo = false;
                        mHandler.sendEmptyMessageDelayed(1002,1000);
                    }
                    break;
                case 1002:
                    Gpiovalue.setText(R.string.gpiocmplete);
                    mSuccess.setVisibility(View.VISIBLE);
                    if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)
                            || mFatherName.equals(MyApplication.MMI1_PreName) || mFatherName.equals(MyApplication.MMI2_PreName)) {
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
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
        if(testone) {
            boolean inputValue = writeToFile(gpioIntput, "input");
            if (inputValue) {
                boolean GpioResult = writeToFile(gpioOutput, "1");
                if (GpioResult) {
                    mHandler.sendEmptyMessageDelayed(1000, 500);
                }
            }
        }
        if(testtwo) {
            boolean inputValue = writeToFile(gpioIntput, "input");
            if (inputValue) {
                boolean GpioResult = writeToFile(gpioOutput, "0");
                if (GpioResult) {
                    mHandler.sendEmptyMessageDelayed(1001, 500);
                }
            }
        }

            mHandler.postDelayed(this, 500);

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