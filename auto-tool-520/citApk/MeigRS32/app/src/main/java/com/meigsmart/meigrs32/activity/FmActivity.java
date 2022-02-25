package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
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
import java.lang.reflect.Method;

import butterknife.BindView;

public class FmActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack,Runnable{
    private FmActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";

    private int currPosition = 0;
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private int TIME_VALUES = 1000;
    private boolean foolProofFlag = false;
    private String FM_FOOLPROOF_FLAG = "common_cit_fm_test_bool_flag";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_fm;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_fm);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        foolProofFlag = "true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, FM_FOOLPROOF_FLAG));

        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

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

        Intent Cameraintent= new Intent();
        ComponentName cn = new ComponentName("com.caf.fmradio", "com.caf.fmradio.FMRadio");
        //ComponentName cn = new ComponentName(mScanPakageName, mScanClassName);
        Cameraintent.setComponent(cn);
        //add by wangjinfeng for foolproof
        if(foolProofFlag){
            Cameraintent.putExtra("foolProofFlag",true);
            mContext.startActivityForResult(Cameraintent,0);//Check whether the requestcode is consistent in onActivityResult
        }else{
            mSuccess.setVisibility(View.VISIBLE);
            mContext.startActivity(Cameraintent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d("fmActivity", "onActivityResult: requestCode="+requestCode+";resultCode= "+resultCode);
        if(requestCode == 0 && resultCode == RESULT_OK){
            int ListCount = data.getIntExtra("listCount",0);
            LogUtil.d("FmActivity_onActivityResult","get FM station list count= "+ListCount);
            if(ListCount>0){
                mSuccess.setVisibility(View.VISIBLE);
            }
        }
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.d("write to file " + path + "abnormal.");
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
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    private void stopActivity(String packageName){
        try {
            ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(mActivityManager, packageName);
        }catch (Exception e) {
            LogUtil.e("FmActivity", "operate forceStopPackage amnormal.");
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
       /* String packageName = "com.caf.fmradio";
        try {
            ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(mActivityManager, packageName);
        }catch (Exception e) {
            LogUtil.e("FmActivity", "operate forceStopPackage amnormal.");
            e.printStackTrace();
        }*/
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
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
    protected void deInit(String fatherName, int results, String reason){
        stopActivity("com.caf.fmradio");
        super.deInit(fatherName, results, reason);
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