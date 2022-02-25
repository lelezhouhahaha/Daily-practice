package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
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

import butterknife.BindView;

public class IRCutModeActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack,Runnable{
    private IRCutModeActivity mContext;
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

    private static final String INFRARED_CUT_NODE_KEY = "infrared_cut_node";
    private String mInfraredCutNodePath;

    public static final int IR_CUT_MODE_COAST = 0;
    public static final int IR_CUT_MODE_REVERSE = 1;
    public static final int IR_CUT_MODE_FORWARD = 2;
    public static final int IR_CUT_MODE_BRADE = 3;

    private static final int MSG_IR_CUT_MODE_COAST = 1002;
    private static final int MSG_IR_CUT_MODE_REVERSE = 1003;
    private static final int MSG_IR_CUT_MODE_FORWARD = 1004;
    private static final int MSG_IR_CUT_MODE_BRADE = 1005;
    private static final int MSG_SHOW_SUCCESS = 8888;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ir_cut_mode;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_ir_cut_mode);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mInfraredCutNodePath = DataUtil.initConfig(Const.CIT_NODE_CONFIG_PATH, INFRARED_CUT_NODE_KEY);

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

        mHandler.sendEmptyMessage(MSG_IR_CUT_MODE_COAST);
        if(!mInfraredCutNodePath.isEmpty())
            mHandler.sendEmptyMessageDelayed(MSG_SHOW_SUCCESS, 6000);
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
                case MSG_IR_CUT_MODE_COAST:
                    setLED(IR_CUT_MODE_COAST);
                    sendEmptyMessageDelayed(MSG_IR_CUT_MODE_REVERSE,500);
                    break;
                case MSG_IR_CUT_MODE_REVERSE:
                    setLED(IR_CUT_MODE_REVERSE);
                    sendEmptyMessageDelayed(MSG_IR_CUT_MODE_FORWARD,500);
                    break;
                case MSG_IR_CUT_MODE_FORWARD:
                    setLED(IR_CUT_MODE_FORWARD);
                    sendEmptyMessageDelayed(MSG_IR_CUT_MODE_BRADE,500);
                    break;
                case MSG_IR_CUT_MODE_BRADE:
                    setLED(IR_CUT_MODE_BRADE);
                    sendEmptyMessageDelayed(MSG_IR_CUT_MODE_COAST,500);
                    break;
                case MSG_SHOW_SUCCESS:
                    mSuccess.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    private void setLED(int cutMode) {
        byte ledData[] = String.valueOf(cutMode).getBytes();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(mInfraredCutNodePath);
            fileOutputStream.write(ledData);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(MSG_IR_CUT_MODE_COAST);
        mHandler.removeMessages(MSG_IR_CUT_MODE_REVERSE);
        mHandler.removeMessages(MSG_IR_CUT_MODE_FORWARD);
        mHandler.removeMessages(MSG_IR_CUT_MODE_BRADE);
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