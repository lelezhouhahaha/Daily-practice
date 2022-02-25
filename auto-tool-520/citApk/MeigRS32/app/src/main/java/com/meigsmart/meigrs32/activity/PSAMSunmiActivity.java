package com.meigsmart.meigrs32.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
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
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.wrapper.CheckCardCallbackV2Wrapper;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;
import cn.hsa.ctp.device.sdk.SEService.SEServiceInterface;

import butterknife.BindView;

public class PSAMSunmiActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{

    private PSAMSunmiActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.card1)
    public TextView samcard1;
    @BindView(R.id.card2)
    public TextView samcard2;
    @BindView(R.id.card3)
    public TextView samcard3;
    @BindView(R.id.info)
    public TextView info;
    @BindView(R.id.rdo_sam1)
    public Button rdo_sam1;
    @BindView(R.id.rdo_sam2)
    public Button rdo_sam2;
    @BindView(R.id.rdo_sam3)
    public Button rdo_sam3;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    private int cardType = AidlConstants.CardType.SAM1.getValue();
    private String TAG =  "PSAMSunmiActivity";
    public boolean sam1 = false;
    public boolean sam2 = false;
    public boolean sam3 = false;
    private boolean isMT537_version =false;
    private String projectName = "";
    private String WIFI_mt535 = "common_device_wifi_only";
    private boolean WIFI_BUILD = false;
    private boolean ERO_BUILD = false;
    private SEServiceInterface mISEService = null;
    String mISEService_version ="";
    @Override
    protected int getLayoutId() {
        return R.layout.activity_sam;
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
        rdo_sam1.setOnClickListener(this);
        rdo_sam2.setOnClickListener(this);
        rdo_sam3.setOnClickListener(this);
        mTitle.setText(R.string.sam_test);
        mDialog.setCallBack(this);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        isMT537_version = "MT537".equals(projectName);
        String wifi_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WIFI_mt535);
        if(((null != wifi_path) && !wifi_path.isEmpty())){
            WIFI_BUILD = FileUtil.readFromFile(wifi_path).contains("0");
            ERO_BUILD = FileUtil.readFromFile(wifi_path).contains("1");
        }
        if(WIFI_BUILD||ERO_BUILD){
            samcard3.setVisibility(View.GONE);
            rdo_sam3.setVisibility(View.GONE);
        }
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if (!MyApplication.getInstance().isConnectPaySDK()) {
            MyApplication.getInstance().bindPaySDKService();
            ToastUtil.showCenterShort(getString(R.string.connect_loading));
            return;
        }
        if(isMT537_version){
            rdo_sam1.setVisibility(View.GONE);
            rdo_sam2.setVisibility(View.GONE);
            samcard3.setVisibility(View.GONE);
            rdo_sam3.setVisibility(View.GONE);
            info.setText(R.string.auto_card_info);
            mHandler.sendEmptyMessage(1002);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try{
                mISEService = SEServiceInterface.Stub.asInterface(service);
                mISEService_version=mISEService.getVersion();
                Log.d(TAG,"mISEService_version :"+mISEService_version);
            }catch (Exception e){
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Intent intent = new Intent();
            intent.setPackage("cn.hsa.ctp.device.sdk.SEService");
            intent.setAction("cn.hsa.ctp.device.sdk.SEService.start");
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
    };
    @Override
    protected void onDestroy() {
        cancelCheckCard();
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        try {
            if (isMT537_version) {
                        Intent intent = new Intent();
                        intent.setPackage("cn.hsa.ctp.device.sdk.SEService");
                        intent.setAction("cn.hsa.ctp.device.sdk.SEService.start");
                        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
            }
        }catch (Exception e){

        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
        if (v == rdo_sam1) {
            cardType = AidlConstants.CardType.PSAM0.getValue();
            checkCard();
            sam1 = true;
        }
        if (v == rdo_sam2) {
            cardType = AidlConstants.CardType.SAM1.getValue();
            checkCard();
            sam2 = true;
        }
        if (v == rdo_sam3) {
            cardType = AidlConstants.CardType.SAM2.getValue();
            checkCard();
            sam3 = true;
        }
    }

    /** Check card */
    private void checkCard() {
        try {
            MyApplication.getInstance().readCardOptV2.checkCard(cardType, mCheckCardCallback, 20);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final CheckCardCallbackV2 mCheckCardCallback = new CheckCardCallbackV2Wrapper() {

        @Override
        public void findMagCard(Bundle bundle) throws RemoteException {
            LogUtil.d(TAG, "findMagCard:track1");
        }

        @Override
        public void findICCard(String atr) throws RemoteException {
            LogUtil.d(TAG, "findICCard:" + atr);
            Message msg = mHandler.obtainMessage();
            msg.what = 1001;
            msg.obj = atr;
            mHandler.sendMessage(msg);
        }

        @Override
        public void findRFCard(String uuid) throws RemoteException {
            LogUtil.d(TAG, "findRFCard:" + uuid);
        }

        @Override
        public void onError(int code, String message) throws RemoteException {
            String error = "CheckCard error,code:" + code + ",msg:" + message;
            LogUtil.d(TAG, error);
 //           ToastUtil.showBottomShort(error);
        }
    };

    private void cancelCheckCard() {
        try {
            MyApplication.getInstance().readCardOptV2.cardOff(cardType);
            MyApplication.getInstance().readCardOptV2.cancelCheckCard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String card1 = null;
    String card2 = null;
    String card3 = null;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    if(cardType == AidlConstants.CardType.PSAM0.getValue())
                    {
                        String atrr = (String) msg.obj;
                        samcard1.setText("card1 info:"+atrr);
                        card1 = samcard1.getText().toString();
                    }
                    if(cardType == AidlConstants.CardType.SAM1.getValue())
                    {
                        String atrr = (String) msg.obj;
                        samcard2.setText("card2 info:"+atrr);
                        card2 = samcard2.getText().toString();
                    }
                    if(cardType == AidlConstants.CardType.SAM2.getValue())
                    {
                        String atrr = (String) msg.obj;
                        samcard3.setText("card3 info:"+atrr);
                        card3 = samcard3.getText().toString();
                    }
                    if(WIFI_BUILD||ERO_BUILD){
                        if (sam1 && sam2 && card1 != null && card2 != null ) {
                            mSuccess.setVisibility(View.VISIBLE);
                            deInit(mFatherName, SUCCESS);
                        }
                    }
                    if(!isMT537_version) {
                        if (card1 != null && card2 != null && card3 != null && sam1 && sam2 && sam3) {
                            LogUtil.d(TAG, "Card1:" + card1 + ",card2:" + card2 + ",card3:" + card3);
                            mSuccess.setVisibility(View.VISIBLE);
                            deInit(mFatherName, SUCCESS);
                        }
                    }else{
                        if (card1 != null && card2 != null && sam1 && sam2) {
                            LogUtil.d(TAG, "Card1:" + card1 + ",card2:" + card2);
                            mSuccess.setVisibility(View.VISIBLE);
                            deInit(mFatherName, SUCCESS);
                        }
                    }
                    break;
                case 1002:
                    cardType = AidlConstants.CardType.PSAM0.getValue();
                    checkCard();
                    sam1 = true;
                    mHandler.sendEmptyMessageDelayed(1003,500);
                    break;
                case 1003:
                    cardType = AidlConstants.CardType.SAM1.getValue();
                    checkCard();
                    sam2 = true;
                    break;
            }
        }
    };

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