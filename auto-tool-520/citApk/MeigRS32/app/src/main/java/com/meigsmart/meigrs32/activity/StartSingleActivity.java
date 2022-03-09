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

public class StartSingleActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private StartSingleActivity mContext;
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
    private String mPackageName = "";
    private String mClassName = "";
    private String mScantestName = "";
    private String projectName = "";

    private final String TAG = StartSingleActivity.class.getSimpleName();
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
        mSuccess.setVisibility(View.VISIBLE);
        mFail.setOnClickListener(this);
        //mTitle.setText(R.string.run_in_l_sensor_calibration);
        if(isMT535_version){
            mSuccess.setVisibility(View.GONE);
        }
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        mPackageName = getIntent().getStringExtra("packageName");
        mClassName = getIntent().getStringExtra("className");
        if(!mClassName.isEmpty()) {
            mScantestName = mClassName;
            String title = getStringFromName(mContext, mClassName);
            if(!title.isEmpty())
                super.mName = title;
            else super.mName = mClassName;
            LogUtil.d("citapk addData");
            addData(mFatherName, super.mName);
            LogUtil.d("citapk addData end.");
            if(mPackageName.contains("com.swfp.factory")){
                mSuccess.setVisibility(View.GONE);
            }
        }

        mTitle.setText(getStringFromName(mContext,mClassName));
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
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

       LogUtil.d("citapk StartSingleActivity mPackageName: <" + mPackageName + ">.");
        LogUtil.d("citapk StartSingleActivity mClassName: <" + mClassName + ">.");
        if(!mPackageName.isEmpty() && ! mClassName.isEmpty()){
            ComponentName componentName = new ComponentName(mPackageName, mClassName);
            Intent intent = new Intent();
            intent.setComponent(componentName);
            LogUtil.d("citapk 1 mFatherName:" + mFatherName);
            LogUtil.d("citapk 1 mClassName:" + super.mName);
            intent.putExtra("fatherName", mFatherName);
            intent.putExtra("name", super.mName);
            intent.putExtra("requestCode", 1000);
            if(mFatherName.equals(MyApplication.RuninTestNAME) && (mPackageName.contains("zebra") || mPackageName.contains("oemscandemo")|| mPackageName.contains("newlandscan"))) {
                intent.putExtra("ScanStartType", "auto");
                LogUtil.d("citapk L2S  ScanStartType: auto");
            }
            if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                intent.putExtra("StartType", "pcbaautotest");
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                LogUtil.d(TAG,"citapk L2S  ScanStartType: pcbaautotest");
            }
            startActivityForResult(intent, 1000);
       }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d("citapk StartSingleActivity resultCode: <" + resultCode + ">.");
        LogUtil.d("citapk StartSingleActivity requestCode: <" + requestCode + ">.");
        LogUtil.d("citapk StartSingleActivity data: <" + data + ">.");

        if(!"MT537".equals(projectName)) {
            if(data != null) {
                int result = data.getIntExtra("results", FAILURE);
                LogUtil.d("citapk StartSingleActivity result: <" + result + ">.");
                if (result == SUCCESS) {
                    mSuccess.setVisibility(View.VISIBLE);
                    deInit(mFatherName, SUCCESS);
                } else {
                    deInit(mFatherName, FAILURE);
                }
		    }else{
                if (resultCode == SUCCESS) {
                deInit(mFatherName, SUCCESS);
                } else if(resultCode == FAILURE){
                deInit(mFatherName, FAILURE);
                }
            }
        }else{
            if (resultCode == SUCCESS) {
                deInit(mFatherName, SUCCESS);
            } else if(resultCode == FAILURE){
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
                    deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
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
