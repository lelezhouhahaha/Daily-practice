package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Environment;
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
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import butterknife.BindView;

public class USB3FrontAndbackTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{

    private USB3FrontAndbackTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.usbfvalue)
    public TextView Usbfvalue;
    @BindView(R.id.usbbvalue)
    public TextView UsbBvalue;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    public String usbhostvalue ="/sys/devices/soc/7000000.ssusb/host_speed";
    public String usbvalue ="/sys/devices/soc/7000000.ssusb/host_speed";
    public String usbfbvalue ="/sys/devices/soc/7000000.ssusb/host_speed";
    private final String USB_HOST_PATH_KEY = "common_cit_usb3_test_host_path";
    private final String USB_PATH_KEY = "common_cit_usb3_test_path";
    private final String USB_ForB_PATH_KEY = "common_cit_usb3_test_f_or_b_path";
    private final String LOG_TAG = "USB3ForBTestActivity";
    private boolean front_pass_flag = false;
    private boolean back_pass_flag = false;
    private int mConfigTime = 0;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_usb_fb_test;
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
        mTitle.setText(R.string.usb_3_test);
        mDialog.setCallBack(this);
        Usbfvalue.setText("front:UnTest");
        UsbBvalue.setText("back:UnTest");
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, USB_HOST_PATH_KEY);
        if (!TextUtils.isEmpty(temp))
            usbhostvalue = temp;
        String temp_host = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, USB_PATH_KEY);
        if (!TextUtils.isEmpty(temp_host))
            usbvalue = temp_host;
        String temp_fb = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, USB_ForB_PATH_KEY);
        if (!TextUtils.isEmpty(temp_fb))
            usbfbvalue = temp_fb;
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
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
        mHandler.sendEmptyMessage(1001);
        //getString(usbvalue);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if("1".equals (getString(usbfbvalue)) && (("super-speed").equals(getString(usbhostvalue)) || ("super-speed").equals(getString(usbvalue)))){
                        front_pass_flag = true;
                        Usbfvalue.setText("front:super-speed");
                    }else if("2".equals (getString(usbfbvalue)) && (("super-speed").equals(getString(usbhostvalue)) || ("super-speed").equals(getString(usbvalue)))){
                        back_pass_flag = true;
                        UsbBvalue.setText("back:super-speed");
                    }
                    if(front_pass_flag && back_pass_flag){
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                    mHandler.sendEmptyMessageDelayed(1001, 1000);
                    break;
                case 1002:
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
        super.onDestroy();
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
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    private  String getString(String path) {
        String prop = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            prop = reader.readLine();
            Log.d(LOG_TAG,"getString:"+path+"="+prop);
            //Usbvalue.setText(prop);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
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