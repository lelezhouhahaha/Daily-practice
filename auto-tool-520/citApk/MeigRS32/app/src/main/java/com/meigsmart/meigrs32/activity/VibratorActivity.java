package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
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
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class VibratorActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private VibratorActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.start)
    public Button mStart;
    @BindView(R.id.stop)
    public Button mStop;
    @BindView(R.id.vibrator_info)
    public TextView mVibratorInfo;
    private String mFatherName = "";

    private Vibrator mVibrator;
    private int mConfigResult;
    private int mConfigTime;

    private static final long SHORT_TIME = 2000;
    private static final long STOP_TIME = 1000;
    private static final long V_TIME = 12 * 30 * 24 * 60 * 60 * 1000;
    private static final long WAIT_TIME = SHORT_TIME+STOP_TIME;
    private Runnable mRun;
    private int mTimes = 0;
    private boolean mFlag = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_vibrator;
    }
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_vibrator);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.vibrator_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
            mStart.setVisibility(View.INVISIBLE);
            mStop.setVisibility(View.INVISIBLE);
            mFail.setVisibility(View.INVISIBLE);
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mTimes = mConfigTime/2;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //mHandler.sendEmptyMessageDelayed(1001,2000);
		// modify by maohaojie on 2018.11.08 for bug 21522
        mHandler.sendEmptyMessageDelayed(1002, 500);
        mVibratorInfo.setText(String.format(getString(R.string.vibrator_tag_stop), getString(R.string.vibrator_stop)));


        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                /*if(mFatherName.equals(MyApplication.RuninTestNAME)) {
                    //if (mConfigTime == mTimes) {
                    if(!mFlag) {
                        mHandler.sendEmptyMessageDelayed(1002, SHORT_TIME);
                        mFlag = true;
                    }
                    //}
                }*/
                if( ( mConfigTime == 0 ) && mFatherName.equals(MyApplication.PCBAAutoTestNAME) ){
                    mHandler.sendEmptyMessage(1003);
                    return;
                }
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1003);
                    return;
                }

                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    mDialog.setSuccess();
                    mVibrator.vibrate(V_TIME);
                    break;
                case 1002:
                    mVibrator.cancel();
                    mVibrator.vibrate(
                            new long[] {
                                    STOP_TIME, SHORT_TIME
                            }, 0);
                    if(!mFatherName.equals(MyApplication.RuninTestNAME)) {
                        try {
                            Thread.sleep(WAIT_TIME);
                        } catch (InterruptedException e) {
                            return;
                        }
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                    break;
                case 1003:
                    mVibrator.cancel();
                    if (MyApplication.RuninTestNAME.equals(mFatherName)){
                        deInit(mFatherName, SUCCESS);
                    }else {
                        mConfigTime = 60;
                    }
                    if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
                        deInit(mFatherName, SUCCESS); //Just close current test
                    }
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
        mHandler.removeMessages(9999);
        mVibrator.cancel();
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
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }

    public void onStop(View view) {
        mVibrator.cancel();
        mStart.setVisibility(View.VISIBLE);
        mStop.setVisibility(View.GONE);
        mVibratorInfo.setText(String.format(getString(R.string.vibrator_tag_start), getString(R.string.vibrator_start)));
    }
    public void onStart(View view) {
        /*long[] pattern = {1000, 1000, 1000, 2000};
        mVibrator.vibrate(pattern, 1);
        mSuccess.setVisibility(View.VISIBLE);*/
        mStart.setVisibility(View.GONE);
        mStop.setVisibility(View.VISIBLE);
        mVibratorInfo.setText(String.format(getString(R.string.vibrator_tag_stop), getString(R.string.vibrator_stop)));
        Message msg = mHandler.obtainMessage();
        msg.what = 1002;
        mHandler.sendMessage(msg);
    }
}
