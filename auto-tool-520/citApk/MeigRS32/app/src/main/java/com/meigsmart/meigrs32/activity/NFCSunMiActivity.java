package com.meigsmart.meigrs32.activity;

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
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.wrapper.CheckCardCallbackV2Wrapper;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidl.bean.CardInfo;
import com.sunmi.pay.hardware.aidl.bean.TransData;
import com.sunmi.pay.hardware.aidl.readcard.ReadCardCallback;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.bean.ApduRecvV2;
import com.sunmi.pay.hardware.aidlv2.bean.ApduSendV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;


import java.util.Arrays;

import butterknife.BindView;

public class NFCSunMiActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{

    private NFCSunMiActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.mUuid1)
    public TextView mUuid1;
    @BindView(R.id.mUuid2)
    public TextView mUuid2;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    private boolean str1 = false;
    private boolean str2 = false;
    private String TAG = "NFCSunMiActivity";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_nfc_sunmi;
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
        mUuid1.setText(getString(R.string.nfc_a0));
        mUuid2.setText(getString(R.string.nfc_b4));
        if (!MyApplication.getInstance().isConnectPaySDK()) {
            Log.i("MM0927","NFCSunMiActivity isConnectPaySDK0000");
            MyApplication.getInstance().bindPaySDKService();
            ToastUtil.showCenterShort(getString(R.string.connect_loading));
            return;
        }
        checkCard();
        initTransData();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
        cancelCheckCard();
        super.onDestroy();
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
            int cardType = AidlConstantsV2.CardType.NFC.getValue();
            MyApplication.getInstance().readCardOptV2.checkCard(cardType, mCheckCardCallback, 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            int code = info.getInt("code");
            String msg = info.getString("message");
            String error = "onError:" + msg + " -- " + code;
            LogUtil.e(TAG, error);
//            ToastUtil.showCenterShort(error);
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
            if (type == 1) {// find NFC
                mHandler.sendEmptyMessage(1000);
            }else {//on Error
                ToastUtil.showBottomLong(getString(R.string.card_check_card_error));
            }
            // 继续检卡
 /*           if (!isFinishing()) {
                mHandler.postDelayed(this::checkCard, 500);
            } */
        });
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

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1000:
                    Log.i("MM0927","NFCSunMiActivity 1000");
                    sendApduToCheck();
                    moveCard();
                    break;
                case 1001:
                    Log.i("MM0927","NFCSunMiActivity 1001");
                    checkCard();
                    initTransData();
                    break;
                case 1002:
                    mUuid1.setText("result:0A00");
                    break;
                case 1003:
                    mUuid2.setText("result:0B04");
                    break;
                case 1004:
                    mSuccess.setVisibility(View.VISIBLE);
                    deInit(mFatherName, SUCCESS);
                    break;
            }
        }
    };

    private void sendApduToCheck() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ApduSendV2 send = new ApduSendV2();
                ApduRecvV2 recv = new ApduRecvV2();
                send.command = FileUtil.hexString2Bytes("00b00000");
                send.lc =0;
                send.le =2;

                try {
                    int code = MyApplication.getInstance().readCardOptV2.apduCommand(AidlConstantsV2.CardType.NFC.getValue(), send, recv);
                    if (code < 0) {
                        Log.i("MM0927","NFCSunMiActivity apduCommand failed,code:" + code);
                    } else{
                            Log.i(TAG, "return outdata :" + FileUtil.bytes2HexString(Arrays.copyOf(recv.outData, recv.outlen)) + "\n");
                            Log.i(TAG, "return value :" + code + "\n");
                            Log.i(TAG, "SWA :" + Integer.toHexString((recv.swa & 0xff)) + "  " + "SWB :" + Integer.toHexString((recv.swb & 0xff)) + "\n");
                            Log.i(TAG, "out[1]=" + recv.outData[1] + "out[0]="+ recv.outData[0]+ "\n");
                            String str = FileUtil.bytes2HexString(Arrays.copyOf(recv.outData, recv.outlen));
                            if("0A00".equals(str)){
                                str1 = true;
                                mHandler.sendEmptyMessage(1002);
                            }
                            if("0B04".equals(str)){
                                str2 = true;
                                mHandler.sendEmptyMessage(1003);
                            }
                            if(str1 && str2){
                                mHandler.sendEmptyMessage(1004);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        thread.start();

    }

    private void moveCard() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int result_R= -1;
                int result = -1;
                while (result_R != 0) {
                    try {
  //                      result = MyApplication.getInstance().readCardOptV2.getCardExistStatus(AidlConstants.CardType.NFC.getValue());//卡片不存在 =1
  //                      if (result == 1) {
                            int cardType = AidlConstants.CardType.NFC.getValue();
                            result_R = MyApplication.getInstance().readCardOptV2.cardOff(cardType); //off carrier
                            if (result_R == 0) {
                                mHandler.sendEmptyMessage(1001);
                            }
  //                      }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();

    }

    private void cancelCheckCard() {
        try {
            MyApplication.getInstance().readCardOptV2.cardOff(AidlConstantsV2.CardType.NFC.getValue());
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