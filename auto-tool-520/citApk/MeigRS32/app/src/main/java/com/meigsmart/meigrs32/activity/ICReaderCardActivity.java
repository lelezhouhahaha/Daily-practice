package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Bundle;
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
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.wrapper.CheckCardCallbackV2Wrapper;
import com.sunmi.pay.hardware.aidl.bean.TransData;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;


import butterknife.BindView;

public class ICReaderCardActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{

    private ICReaderCardActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.mAtr)
    public TextView mAtr;
    @BindView(R.id.info)
    public TextView info;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    private final Handler mHandler = new Handler();
    public final String TAG="ICReaderCardActivity";
    private int mConfigTime = 0;
    private Runnable mRun;
    private int code;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ic_card_test;
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
        checkCard();
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (((mConfigTime == 0) && (mFatherName.equals(MyApplication.RuninTestNAME)))||(mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    nHandler.sendEmptyMessage(1001);
                }
                nHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        nHandler.removeMessages(1001);
        cancelCheckCard();
        super.onDestroy();
    }

    private void initTransData() {
        try {
            TransData transData = new TransData();
            transData.amount = "1";
            transData.transType = "00";
            MyApplication.getInstance().readCardOpt.initTransData(transData);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

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
    private void checkCard() {
        try {
            initTransData();
            int cardType = AidlConstantsV2.CardType.IC.getValue();
            MyApplication.getInstance().readCardOptV2.checkCard(cardType, mCheckCardCallback, 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler nHandler = new Handler() {
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
    private final CheckCardCallbackV2 mCheckCardCallback = new CheckCardCallbackV2Wrapper() {

        @Override
        public void findMagCard(Bundle info) throws RemoteException {
        }

        /**
         * Find IC card
         *
         * @param info return data，contain the following keys:
         *             <br/>cardType: card type (int)
         *             <br/>atr: card's ATR (String)
         */
        @Override
        public void findICCardEx(Bundle info) throws RemoteException {
            handleResult(0, info);
        }

        /**
         * Find RF card
         *
         * @param info return data，contain the following keys:
         *             <br/>cardType: card type (int)
         *             <br/>uuid: card's UUID (String)
         *             <br/>ats: card's ATS (String)
         *             <br/>sak: card's SAK, if exist (int) (M1 S50:0x08, M1 S70:0x18, CPU:0x28)
         *             <br/>cardCategory: card's category,'A' or 'B', if exist (int)
         *             <br/>atqa: card's ATQA, if exist (byte[])
         */
        @Override
        public void findRFCardEx(Bundle info) throws RemoteException {
            handleResult(1, info);
        }

        /**
         * Check card error
         *
         * @param info return data，contain the following keys:
         *             <br/>cardType: card type (int)
         *             <br/>code: the error code (String)
         *             <br/>message: the error message (String)
         */
        @Override
        public void onErrorEx(Bundle info) throws RemoteException {
            code = info.getInt("code");
            String msg = info.getString("message");
            String error = "onError:" + msg + " -- " + code;
            LogUtil.e(TAG, error);
//            ToastUtil.showBottomShort(error);
            handleResult(-1, info);
        }
    };
    /**
     * Show check card result
     *
     * @param type 0-find IC, 1-find NFC, <0-check card error
     * @param info The info returned by check card
     */
    private void handleResult(int type, Bundle info) {
        if (isFinishing()) {
            return;
        }
        mHandler.post(() -> {
            if (type == 0) {// find IC
                mAtr.setText("ATR:"+info.getString("atr"));
                String atr = mAtr.getText().toString();
                if(atr != null && !atr.isEmpty()){
                    mSuccess.setVisibility(View.VISIBLE);
                    deInit(mFatherName, SUCCESS);
                }
            }else {//on Error
                if(-30005 != code) {
                    ToastUtil.showBottomShort(getString(R.string.card_check_card_error));
                }
            }
            // 继续检卡
            Log.d(TAG, "isFinishing:"+isFinishing());
            Log.d(TAG, "code:"+code);
            if (!isFinishing()&&(code!=-20001)) {
                mHandler.postDelayed(this::checkCard, 500);
            }
        });
    }

    private void cancelCheckCard() {
        try {
            MyApplication.getInstance().readCardOptV2.cardOff(AidlConstantsV2.CardType.IC.getValue());
            MyApplication.getInstance().readCardOptV2.cancelCheckCard();
        } catch (Exception e) {
            e.printStackTrace();
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