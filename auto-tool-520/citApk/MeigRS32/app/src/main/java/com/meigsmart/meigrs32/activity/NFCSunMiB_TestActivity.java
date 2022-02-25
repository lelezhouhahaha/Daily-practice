package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidl.bean.CardInfo;
import com.sunmi.pay.hardware.aidl.readcard.ReadCardCallback;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.bean.ApduRecvV2;
import com.sunmi.pay.hardware.aidlv2.bean.ApduSendV2;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import butterknife.BindView;

public class NFCSunMiB_TestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{

    private NFCSunMiB_TestActivity mContext;
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
    private String TAG = "NFCSunMiB_TestActivity";
    Timer timer;
    private Nfc_Test mNfc_Test = null;

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
        mUuid1.setText(getString(R.string.nfc_b0));
        mUuid2.setText(getString(R.string.nfc_a4));
        new Nfc_Test().execute();
        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if(str1){
                                    mUuid1.setText("result:0B00");
                                }
                                if(str2){
                                    mUuid2.setText("result:0A04");
                                }
                                    mUuid1.addTextChangedListener(textWatcher);

                            }
                        });
                    }
                },500,500);
            }
        });
    }

    TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // TODO Auto-generated method stub
            if(str1&&str2){
                if(mUuid1.getText().toString().equals("result:0B00")&&
                        mUuid2.getText().toString().equals("result:0A04")){
                    mSuccess.setVisibility(View.VISIBLE);
                    deInit(mFatherName, SUCCESS);
                }

            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
        cancelCheckCard();
        if (mNfc_Test != null && !mNfc_Test.isCancelled() && mNfc_Test.getStatus() == AsyncTask.Status.RUNNING) {
            mNfc_Test.cancel(true);
            mNfc_Test = null;
        }
        if(timer!=null){
            timer.cancel();
        }
        super.onDestroy();
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
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    private void testCard() {
        try {
            int cardType = AidlConstantsV2.CardType.NFC.getValue();
            MyApplication.getInstance().readCardOpt.checkCard(cardType, new ReadCardCallback.Stub() {

                @Override
                public void onCardDetected(CardInfo cardInfo) {
                    Log.i(TAG, "check succ");
                    mHandler.sendEmptyMessage(1000);
                }

                @Override
                public void onError(int code, String message) {
                }

                @Override
                public void onStartCheckCard() {
                }

            }, 60);
        } catch (RemoteException e) {
            e.printStackTrace();

        }
    }

    class Nfc_Test extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                int cardType = AidlConstantsV2.CardType.NFC.getValue();
                MyApplication.getInstance().readCardOpt.checkCard(cardType, new ReadCardCallback.Stub() {

                    @Override
                    public void onCardDetected(CardInfo cardInfo) {
                        Log.i(TAG, "check succ");
                        mHandler.sendEmptyMessage(1000);
                    }

                    @Override
                    public void onError(int code, String message) {
                    }

                    @Override
                    public void onStartCheckCard() {
                    }

                }, 60);
            } catch (RemoteException e) {
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1000:
                    sendApduToCheck();
                    moveCard();
                    break;
                case 1001:
                    testCard();
                    break;
                case 1002:
                  //  mUuid1.setText("result:0B00");
                    break;
                case 1003:
                  //  mUuid2.setText("result:0A04");
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
                        Log.i(TAG,"apduCommand failed,code:" + code);
                    } else{
                        Log.i(TAG, "return outdata :" + FileUtil.bytes2HexString(Arrays.copyOf(recv.outData, recv.outlen)) + "\n");
                        Log.i(TAG, "return value :" + code + "\n");
                        Log.i(TAG, "SWA :" + Integer.toHexString((recv.swa & 0xff)) + "  " + "SWB :" + Integer.toHexString((recv.swb & 0xff)) + "\n");
                        Log.i(TAG, "out[1]=" + recv.outData[1] + "out[0]="+ recv.outData[0]+ "\n");
                        String str = FileUtil.bytes2HexString(Arrays.copyOf(recv.outData, recv.outlen));
                        if("0B00".equals(str)){
                            str1 = true;
                            mHandler.sendEmptyMessage(1002);
                        }
                        if("0A04".equals(str)){
                            str2 = true;
                            mHandler.sendEmptyMessage(1003);
                        }
                        if(str1 && str2){
                            //mHandler.sendEmptyMessage(1004);
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
                while (result != 1) {
                    try {
                        result = MyApplication.getInstance().readCardOpt.getCardExistStatus(AidlConstants.CardType.NFC.getValue());//卡片不存在 =1
                        if (result == 1) {
                            int cardType = AidlConstants.CardType.NFC.getValue();
                            result_R = MyApplication.getInstance().readCardOpt.cardOff(cardType); //off carrier
                            if (result_R == 0) {
                                mHandler.sendEmptyMessage(1001);
                            }
                        }

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
            MyApplication.getInstance().readCardOpt.cardOff(AidlConstantsV2.CardType.NFC.getValue());
            MyApplication.getInstance().readCardOpt.cancelCheckCard();
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