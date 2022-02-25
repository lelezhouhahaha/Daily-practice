package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import butterknife.BindView;

public class ScanActivityNearFocus extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private ScanActivityNearFocus mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    private  String mPackageName = "";
    private String mClassName = "";
    private String defaultCompareValue = "";
    private String projectName = "";

    private int mConfigTime = 0;
    private Runnable mRun;
    private static final String KEY_NEARFOCUS_SCAN = "common_ScanActivityNearFocus_default_compare_value";
    private String HONEYWELL_4603_NEAR_COMPARE_VALUE = "N4603_NEAR";
    private String HONEYWELL_6703_NEAR_COMPARE_VALUE = "N6703SR_NEAR";
    private String SDL_4770_NEAR_COMPARE_VALUE = "SE4770_NEAR";
	private boolean mIsHoneywell4603ScanType = false;
    private boolean mIsHoneywell6703ScanType = false;
    private boolean mIsSdl4770ScanType = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_startsingle;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setOnClickListener(this);
        //mSuccess.setVisibility(View.VISIBLE);
        mSuccess.setVisibility(View.GONE);
        mFail.setOnClickListener(this);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        addData(mFatherName, super.mName);

        mTitle.setText(R.string.ScanActivityNearFocus);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        LogUtil.d("citapk projectName:" + projectName);
		if(projectName.contains("MC520") || projectName.contains("MC520_GMS")){
            String scanType = DataUtil.readLineFromFile("/dev/sunmi/scan/scan_head_type");
            LogUtil.d("citapk scanType:" + scanType);
            if (!scanType.isEmpty()){
                if(scanType.contains("HONEYWELL4603")){
                    mIsHoneywell4603ScanType = true;
                }else if(scanType.contains("HONEYWELL6703")){
                    mIsHoneywell6703ScanType = true;
                }else if(scanType.contains("ZABRA4770")){
                    mIsSdl4770ScanType = true;
                }
            }
        }

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        defaultCompareValue = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, KEY_NEARFOCUS_SCAN);
        if( (defaultCompareValue == null) || defaultCompareValue.isEmpty()){
            defaultCompareValue = "123456789";
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1002);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
       // mHandler.sendEmptyMessage(1003);//delay 1s to open scan activity
        startScanActivity();
    }
    void startScanActivity(){
        if("MT537".equals(projectName)){
            mPackageName = "com.dawn.newlandscan";
            mClassName = "com.dawn.newlandscan.MainActivity";
        }else{
           //if(SystemProperties.get("ro.product.model").contains("L2s")) {
            if(mIsHoneywell4603ScanType || mIsHoneywell6703ScanType){
                mPackageName = "com.example.oemscandemo";
                mClassName = "com.example.oemscandemo.MainActivity";
            } else if(mIsSdl4770ScanType){
                mPackageName = "com.zebra.sdl";
                mClassName = "com.zebra.sdl.SDLguiActivity";
            } else {
                mPackageName = "com.zebra.sdl";
                mClassName = "com.zebra.sdl.SDLguiActivity";
            }
            LogUtil.d("citapk L2s mPackageName:" + mPackageName);
            LogUtil.d("citapk L2s mClassName:" + mClassName);
            //}
        }

        ComponentName componentName = new ComponentName(mPackageName, mClassName);
        Intent intent = new Intent();
        intent.setComponent(componentName);

        intent.putExtra("fatherName", mFatherName);
        intent.putExtra("name", super.mName);
        intent.putExtra("requestCode", 1000);
        //<!-- modify for bug 25564 by huangqian,add for ScanActivityNearFocus start.
        intent.putExtra("title",mTitle.getText().toString());
        // modify for bug 25564 by huangqian,add for ScanActivityNearFocus end. -->
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            intent.putExtra("ScanStartType", "auto");
            LogUtil.d("citapk L2S  ScanStartType: auto");
        }
        startActivityForResult(intent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d(" citapk ScanActivityNearFocus resultCode: <" + resultCode + ">.");
        LogUtil.d(" citapk ScanActivityNearFocus requestCode: <" + requestCode + ">.");
        if(!"MT537".equals(projectName)) {
        if(data != null) {
            if(mFatherName.equals(MyApplication.RuninTestNAME)) {
                mSuccess.setVisibility(View.VISIBLE);
                deInit(mFatherName, SUCCESS);
            }else {
                LogUtil.d(" onActivityResult defaultCompareValue:" + defaultCompareValue);
                String result = data.getStringExtra("results");
                LogUtil.d(" onActivityResult result:" + result);
                /*if (result.contains(defaultCompareValue)) {
                    mSuccess.setVisibility(View.VISIBLE);
                    deInit(mFatherName, SUCCESS);
                } else {
                    deInit(mFatherName, FAILURE);
                }*/
                if(mIsHoneywell4603ScanType){
                    if(result.contains(HONEYWELL_4603_NEAR_COMPARE_VALUE)){
                        mHandler.sendEmptyMessage(1001);
                    } else {
                        mHandler.sendEmptyMessage(1002);
                    }
                }else if(mIsHoneywell6703ScanType){
                    if(result.contains(HONEYWELL_6703_NEAR_COMPARE_VALUE)){
                        mHandler.sendEmptyMessage(1001);
                    } else {
                        mHandler.sendEmptyMessage(1002);
                    }
                }else if(mIsSdl4770ScanType){
                    if(result.contains(SDL_4770_NEAR_COMPARE_VALUE)){
                        mHandler.sendEmptyMessage(1001);
                    } else {
                        mHandler.sendEmptyMessage(1002);
                    }
                }
            }
        }
        }else{
            if(data!=null) {
                String result = data.getStringExtra("results");
                LogUtil.d(" onActivityResult result:" + result);
                if (result.contains("123456789")) {
                    mSuccess.setVisibility(View.VISIBLE);
                    deInit(mFatherName, SUCCESS);
                } else {
                    deInit(mFatherName, FAILURE);
                }
            }else{
                deInit(mFatherName, FAILURE);
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mSuccess.setVisibility(View.VISIBLE);
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1002:
                    if(mFatherName.equals(MyApplication.RuninTestNAME)) {
                        mSuccess.setVisibility(View.VISIBLE);
                        deInit(mFatherName, SUCCESS);
                    }else {
                        deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    }
                    break;
                case 1003:
                    startScanActivity();
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
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
