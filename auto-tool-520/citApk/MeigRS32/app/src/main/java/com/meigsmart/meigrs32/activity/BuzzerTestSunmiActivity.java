package com.meigsmart.meigrs32.activity;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
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
import com.meigsmart.meigrs32.util.CitTestJni;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidlv2.system.BasicOptV2;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import butterknife.BindView;
import sunmi.paylib.SunmiPayKernel;


public class BuzzerTestSunmiActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private BuzzerTestSunmiActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.retest)
    public Button mRetestBtn;
    private String mFatherName = "";
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @BindView(R.id.content_show)
    public TextView mContentShow;

    public static final int BUZZER_TEST_MSG = 1001;
    public static final int BUZZER_TEST_UPDATE_MSG = 1002;
    public static final int BUZZER_TEST_SUCCESS_MSG = 1003;
    public static final int BUZZER_TEST_HANDLERTHREAD_MSG = 1;
    private HandlerThread mHanlerThread = null;
    private Handler mBuzzerTestHandler = null;
    private int mConfigTime = 0;
    private Runnable mRun;
    private final String TAG = "BuzzerTestSunmiActivity";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_buzzertest;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.BuzzerTestSunmiActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        mContentShow.setText(R.string.buzzer_test_content);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        if (!MyApplication.getInstance().isConnectPaySDK()) {
            MyApplication.getInstance().bindPaySDKService();
            ToastUtil.showCenterShort(getString(R.string.connect_loading));
            return;
        }

        Log.d(TAG,"mFatherName: "+mFatherName+" ||| "+MyApplication.RuninTestNAME);
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(BUZZER_TEST_SUCCESS_MSG);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        mHandler.sendEmptyMessageDelayed(BUZZER_TEST_MSG, 1000);
        mRetestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.sendEmptyMessageDelayed(BUZZER_TEST_MSG,100);
            }
        });

    }


    private void buzzer(int time) {
            try {
                int delay = 500;
                MyApplication.getInstance().basicOptV2.buzzerOnDevice(time, 4000, 500, delay);
                mHandler.sendEmptyMessageDelayed(BUZZER_TEST_UPDATE_MSG, 3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case BUZZER_TEST_SUCCESS_MSG:
                    deInit(mFatherName, SUCCESS);
                    break;
                case BUZZER_TEST_MSG:
                    //nothing
 //                   if(isConnectPaySDK()) {
                        buzzer(4);
//                    }
                    break;
                case BUZZER_TEST_UPDATE_MSG:
                    mSuccess.setVisibility(View.VISIBLE);
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
        if(mBuzzerTestHandler != null){
            mBuzzerTestHandler.removeMessages(BUZZER_TEST_HANDLERTHREAD_MSG);
        }
        if(mHanlerThread != null){
            mHanlerThread.quit();
        }
        mHandler.removeMessages(BUZZER_TEST_MSG);
        mHandler.removeMessages(BUZZER_TEST_UPDATE_MSG);
        mHandler.removeMessages(BUZZER_TEST_SUCCESS_MSG);
        mHandler.removeMessages(9999);
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


}
