package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Bundle;
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
import com.meigsmart.meigrs32.util.ByteUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.wrapper.CheckCardCallbackV2Wrapper;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidl.AidlErrorCode;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;

import java.util.Locale;

import butterknife.BindView;

import static com.meigsmart.meigrs32.util.FileUtil.null2String;

public class MagEncActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{

    private MagEncActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.info)
    public TextView info;
    @BindView(R.id.count1)
    public Button count1;
    @BindView(R.id.count2)
    public Button count2;
    @BindView(R.id.count3)
    public Button count3;
    @BindView(R.id.track1)
    public TextView track11;
    @BindView(R.id.track2)
    public TextView track22;
    @BindView(R.id.track3)
    public TextView track33;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    private String mFatherName = "";
    private final String TAG = "MagEncActivity";
    private final Handler handler = new Handler();
    private int mTotalTime;
    private int mSuccessTime;
    private int mFailTime;
    private int mConfigTime = 0;
    private Runnable mRun;
    private int errcode;
    private boolean isMT537_version =false;
    private String projectName = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_mag_enc;
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
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mTitle.setText(super.mName);
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
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        isMT537_version = "MT537".equals(projectName);
        checkCard();
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (((mConfigTime == 0) && (mFatherName.equals(MyApplication.RuninTestNAME)))||(mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        cancelCheckCard();
        mHandler.removeMessages(1001);
        super.onDestroy();
    }

    /** start check card */
    private void checkCard() {
        try {
            MyApplication.getInstance().readCardOptV2.checkCard(AidlConstantsV2.CardType.MAGNETIC.getValue(), mCheckCardCallback, 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final CheckCardCallbackV2 mCheckCardCallback = new CheckCardCallbackV2Wrapper() {
        /**
         * Find magnetic card
         *
         * @param info return data，contain the following keys:
         *             <br/>cardType: card type (int)
         *             <br/>TRACK1: track 1 data (String)
         *             <br/>TRACK2: track 2 data (String)
         *             <br/>TRACK3: track 3 data (String)
         *             <br/>track1ErrorCode: track 1 error code (int)
         *             <br/>track2ErrorCode: track 2 error code (int)
         *             <br/>track3ErrorCode: track 3 error code (int)
         *             <br/> track error code is one of the following values:
         *             <ul>
         *             <li>0 - No error</li>
         *             <li>-1 - Track has no data</li>
         *             <li>-2 - Track parity check error</li>
         *             <li>-3 - Track LRC check error</li>
         *             </ul>
         */
        @Override
        public void findMagCard(Bundle info) throws RemoteException {
//            LogUtil.e(TAG, "findMagCard,bundle:" + Utility.bundle2String(info));
            handleResult(info);
        }

        @Override
        public void findICCard(String atr) throws RemoteException {
            LogUtil.e(TAG, "findICCard,atr:" + atr);
        }

        @Override
        public void findRFCard(String uuid) throws RemoteException {
            LogUtil.e(TAG, "findRFCard,uuid:" + uuid);
        }

        @Override
        public void onError(int code, String message) throws RemoteException {
            errcode = code;
            String error = "onError:" + message + " -- " + code;
            LogUtil.e(TAG, error);
 //           ToastUtil.showBottomShort(error);
            handleResult(null);
        }
    };

    private void handleResult(Bundle bundle) {
        if (isFinishing()) {
            return;
        }
        handler.post(() -> {
            if(errcode == AidlErrorCode.READ_CARD_TIMEOUT.getCode()){
                handler.postDelayed(this::checkCard, 500);
            }
            if (bundle == null) {
//                showResult(false, "", "", "");
                return;
            }
            String track1 = FileUtil.null2String(bundle.getString("TRACK1"));
            String track2 = FileUtil.null2String(bundle.getString("TRACK2"));
            String track3 = FileUtil.null2String(bundle.getString("TRACK3"));
            //磁道错误码：0-无错误，-1-磁道无数据，-2-奇偶校验错，-3-LRC校验错
            int code1 = bundle.getInt("track1ErrorCode");
            int code2 = bundle.getInt("track2ErrorCode");
            int code3 = bundle.getInt("track3ErrorCode");
            LogUtil.e(TAG, String.format(Locale.getDefault(),
                    "track1ErrorCode:%d,track1:%s\ntrack2ErrorCode:%d,track2:%s\ntrack3ErrorCode:%d,track3:%s",
                    code1, track1, code2, track2, code3, track3));
            if ((code1 != 0 && code1 != -1) || (code2 != 0 && code2 != -1) || (code3 != 0 && code3 != -1)) {
                showResult(false, track1, track2, track3);
            } else if (code1 == 0 && code2 == 0 && code3 == 0){
                showResult(true, track1, track2, track3);
            }
            // 继续检卡
            if(errcode != AidlErrorCode.READ_CARD_TIMEOUT.getCode()) {
                if (!isFinishing()) {
                    handler.postDelayed(this::checkCard, 500);
                }
            }
        });
    }

    private void showResult(boolean success, String track1, String track2, String track3) {
        mTotalTime += 1;
        if (success) {
            mSuccessTime += 1;
        } else {
            mFailTime += 1;
        }
        track11.setText("track1:"+track1);
        track22.setText("track2:"+track2);
        track33.setText("track3:"+track3);
        
        String temp = getString(R.string.card_total) + " " + mTotalTime;
        count1.setText(temp);
        temp = getString(R.string.card_success) + " " + mSuccessTime;
        count2.setText(temp);
        temp = getString(R.string.card_fail) + " " + mFailTime;
        count3.setText(temp);
        if(!isMT537_version){
        if(mSuccessTime>=2 && track1 != null &&  track2 != null &&  track3 != null){
            mSuccess.setVisibility(View.VISIBLE);
            deInit(mFatherName, SUCCESS);
        }
        }else{
                if(mSuccessTime>=2){
                    mSuccess.setVisibility(View.VISIBLE);
                    deInit(mFatherName, SUCCESS);
                }
                if(mTotalTime>=3&&mFailTime>=2){
                    deInit(mFatherName, FAILURE);
                }
            }
        
    }

    private void cancelCheckCard() {
        try {
            MyApplication.getInstance().readCardOptV2.cancelCheckCard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if (mFatherName.equals(MyApplication.RuninTestNAME)) {
                        deInit(mFatherName, SUCCESS);
                    } else {
                        deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                    }
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
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