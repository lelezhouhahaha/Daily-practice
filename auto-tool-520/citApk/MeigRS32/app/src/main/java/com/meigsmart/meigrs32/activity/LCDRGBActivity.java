package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
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
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class LCDRGBActivity extends BaseActivity implements View.OnClickListener ,
        PromptDialog.OnPromptDialogCallBack ,Runnable{
    private LCDRGBActivity mContext;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    //@BindView(R.id.title)
    //public TextView mTitle;
    //@BindView(R.id.back)
    //public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    private int[] ids = {
            Color.parseColor("#FF0000"),Color.parseColor("#00FF00"),Color.parseColor("#0000FF"),
            Color.parseColor("#888888"),Color.parseColor("#000000"),Color.parseColor("#FFFFFF"),
    };
    private int TIME_VALUES = 1000;
    private int currPosition = 0;
    @BindView(R.id.flag)
    public TextView mFlag;
    private int mConfigResult;
    private int mConfigTime = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_lcd_rgb;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        /*
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_lcd_rgb);*/

        mSuccess.setVisibility(View.GONE);
        mFail.setVisibility(View.GONE);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.lcd_rgb_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mHandler.postDelayed(this,100);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    if (mConfigResult>=1){
                        deInit(mFatherName, SUCCESS);
                    }else {
                        deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
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
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        /*if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }*/

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
        mFlag.setVisibility(View.GONE);
        mLayout.setBackgroundColor(ids[currPosition]);
        currPosition++;
        if (currPosition == 6){
            currPosition = 0;
            mSuccess.setVisibility(View.VISIBLE);
            mFail.setVisibility(View.VISIBLE);
        }
        mConfigTime--;
        updateFloatView(mContext,mConfigTime);
        if (mConfigTime == 0 ||
                mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)) {
            mHandler.sendEmptyMessage(1001);
        }
        mHandler.postDelayed(this,TIME_VALUES);
    }
}
