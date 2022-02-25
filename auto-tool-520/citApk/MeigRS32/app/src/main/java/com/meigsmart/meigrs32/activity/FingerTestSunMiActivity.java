package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class FingerTestSunMiActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private FingerTestSunMiActivity mContext;
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
    private String projectName = "";

    private int mConfigTime = 0;
    private Runnable mRun;

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
        mSuccess.setVisibility(View.GONE);
        mFail.setOnClickListener(this);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        addData(mFatherName, super.mName);

        mTitle.setText(R.string.ScanActivityFarFocus);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

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
                    mHandler.sendEmptyMessage(1002);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
            mPackageName = "cn.com.aratek.demo";
            mClassName = "cn.com.aratek.demo.FingerprintDemo";

        ComponentName componentName = new ComponentName(mPackageName, mClassName);
        Intent intent = new Intent();
        intent.setComponent(componentName);

        intent.putExtra("fatherName", mFatherName);
        intent.putExtra("name", super.mName);
        intent.putExtra("requestCode", 1000);
        startActivityForResult(intent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

            String result = data.getStringExtra("results");
            if (!result.equals("")) {
                mSuccess.setVisibility(View.VISIBLE);
                //SystemProperties.set("persist.custmized.fingerprint_version",result);
                SystemProperties.set(OdmCustomedProp.getFingerprintVersionProp(),result);
                deInit(mFatherName, SUCCESS);
            } else {
                //SystemProperties.set("persist.custmized.fingerprint_version",result);
                SystemProperties.set(OdmCustomedProp.getFingerprintVersionProp(),result);
                deInit(mFatherName, FAILURE);
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
