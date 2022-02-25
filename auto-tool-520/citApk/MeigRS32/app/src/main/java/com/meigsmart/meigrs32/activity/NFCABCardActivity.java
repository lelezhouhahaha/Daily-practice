package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcBarcode;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
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
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class NFCABCardActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private NFCABCardActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    private NfcAdapter mDefaultAdapter;
    private PendingIntent pendingIntent;
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    @BindView(R.id.nfc_info)
    public TextView mNFCInfo;
    @BindView(R.id.nfc_info_a0)
    public TextView mNFCInfoA0;
    @BindView(R.id.nfc_info_a4)
    public TextView mNFCInfoA4;
    @BindView(R.id.nfc_info_b0)
    public TextView mNFCInfoB0;
    @BindView(R.id.nfc_info_b4)
    public TextView mNFCInfoB4;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean mLastNfcStatus = true;//add by wangxing for taskid 2611

    private static IntentFilter[] NFC_FILTERS;
    private static String[][] NFC_TECHLISTS;
    private final String TAG = NFCABCardActivity.class.getSimpleName();
    private boolean isPass;
    private boolean mReadCardA1Status = false;
    private boolean mReadCardA2Status = false;
    private boolean mReadCardB1Status = false;
    private boolean mReadCardB2Status = false;
    private boolean mIsPCBATest = false;

    @Override
    protected int getLayoutId() {
		LogUtil.d(TAG, "getLayoutId");
        return R.layout.activity_nfc_ab_card;
    }

    @Override
    protected void initData() {
		LogUtil.d(TAG, "initData start");
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.run_in_nfc);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName==null){
            finish();
            return;
        }

        mIsPCBATest = mFatherName.equals(MyApplication.PCBASignalNAME) || mFatherName.equals(MyApplication.PCBANAME);
        if(mIsPCBATest){
            mNFCInfoA4.setVisibility(View.GONE);
            mNFCInfoB0.setVisibility(View.GONE);
            mNFCInfoB4.setVisibility(View.GONE);
        }
        mConfigResult = getResources().getInteger(R.integer.nfc_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        String strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.TEST_DIALOG);
        if((null != strTmp) && !strTmp.isEmpty() && strTmp.contains("true")) {
            showDialog();
        }else{
            initTest();
        }
		LogUtil.d(TAG, "initData end");
    }

    private void showDialog() {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(NFCABCardActivity.this);
        dialog.setMessage(R.string.nfc_dialog_msg);
        dialog.setPositiveButton(R.string.str_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        initTest();
                        if(mDefaultAdapter!=null) {
                            mDefaultAdapter.enableForegroundDispatch(mContext, pendingIntent, NFC_FILTERS, NFC_TECHLISTS);
                        }
                    }
                });
        dialog.setNegativeButton(R.string.str_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void initTest(){
        mHandler.sendEmptyMessage(1000);

        initNFC();

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if(mConfigTime <= 0 )
                    mConfigTime = 1;
                LogUtil.d(TAG, "initTest mConfigTime:" + mConfigTime);
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    private void showNfcInfo(){
        if(!mReadCardA1Status){
            mNFCInfo.setText(R.string.nfc_a0);
        }else if(!mReadCardA2Status && !mIsPCBATest) {
            mNFCInfo.setText(R.string.nfc_a4);
        }else if(!mReadCardB1Status && !mIsPCBATest) {
            mNFCInfo.setText(R.string.nfc_b0);
        }else if(!mReadCardB2Status && !mIsPCBATest){
            mNFCInfo.setText(R.string.nfc_b4);
        }else {
            mNFCInfo.setText(R.string.nfc_layout_tag);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1000:
                    mFlag.setVisibility(View.GONE);
                    mLayout.setVisibility(View.VISIBLE);
                    isStartTest = true;
                    break;
                case 1001:
                    if(mFatherName.equals(MyApplication.PreSignalNAME)||mFatherName.equals(MyApplication.PCBASignalNAME) && isPass) {
                        mSuccess.setVisibility(View.VISIBLE);
                    }else if (isPass || mFatherName.equals(MyApplication.RuninTestNAME) ){//modify by wangxing for bug P_RK95_E-706 run in log show pass
                        deInit(mFatherName, SUCCESS);
                    }else {
                        deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                    }
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE);
                    break;
                case 1005:
                    LogUtil.d(TAG, "1005 mIsPCBATest:" + mIsPCBATest + "   mReadCardA1Status:" + mReadCardA1Status );
                    if(mIsPCBATest){
                        if(mReadCardA1Status){
                            isPass = true;
                            mHandler.sendEmptyMessage(1001);
                        }
                    }else {
                        if (mReadCardA1Status && mReadCardA2Status && mReadCardB1Status && mReadCardB2Status) {
                            isPass = true;
                            mHandler.sendEmptyMessage(1001);
                        }
                    }
                    break;
                case 1006:
                    String content = (String) msg.obj;
                    LogUtil.d(TAG, "1006 content:" + content);
                    if(content.contains("0a00")){
                        mReadCardA1Status= true;
                        mNFCInfoA0.setText(String.format(getResources().getString(R.string.nfc_info_a0), "0a00"));
                        showNfcInfo();
                    }else if(content.contains("0a04")){
                        mReadCardA2Status= true;
                        mNFCInfoA4.setText(String.format(getResources().getString(R.string.nfc_info_a4), "0a04"));
                        showNfcInfo();
                    }else if(content.contains("0b00")){
                        mReadCardB1Status= true;
                        mNFCInfoB0.setText(String.format(getResources().getString(R.string.nfc_info_b0), "0b00"));
                        showNfcInfo();
                    }else if(content.contains("0b04")){
                        mReadCardB2Status= true;
                        mNFCInfoB4.setText(String.format(getResources().getString(R.string.nfc_info_b4), "0b04"));
                        showNfcInfo();
                    }
                    mHandler.sendEmptyMessage(1005);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
        try{
            if(mDefaultAdapter!=null){
                //add by wangxing for taskid 2611
                LogUtil.d(TAG, "onDestroy mDefaultAdapter!=null");
                if(!mLastNfcStatus){
                    LogUtil.d(TAG, "onDestroy disable");
                    mDefaultAdapter.disable();
                }
                mDefaultAdapter.disableForegroundDispatch(this);//关闭前台发布系统
            }
        }catch (Exception e){
            LogUtil.d(TAG, "onDestroy exception:" + e.getMessage());
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
		LogUtil.d(TAG, "onPause start");
        if(mDefaultAdapter!=null){
		LogUtil.d(TAG, "onPause mDefaultAdapter!=null");
            mDefaultAdapter.disableForegroundDispatch(this);//关闭前台发布系统
        }
		LogUtil.d(TAG, "onPause end");
    }

    @Override
    protected void onResume() {
        super.onResume();
		LogUtil.d(TAG, "onResume start");
        if(mDefaultAdapter!=null) {
            LogUtil.d(TAG, "onResume mDefaultAdapter!=null");
            mDefaultAdapter.enableForegroundDispatch(this, pendingIntent, NFC_FILTERS, NFC_TECHLISTS);
        }
		LogUtil.d(TAG, "onResume end");
    }

    public void getNdefMsg(Intent intent) {
        LogUtil.d(TAG, "getNdefMsg start");
        if (intent == null) {
            LogUtil.d(TAG, "getNdefMsg 1");
            return;
        }
            //return null;

        //nfc卡支持的格式
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String[] temp = tag.getTechList();
        for (String s : temp) {
            Log.i(TAG, "resolveIntent tag: " + s);
        }

        String action = intent.getAction();
        LogUtil.d(TAG, "action:" + action);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Parcelable[] rawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] ndefMessages;

            // 判断是哪种类型的数据 默认为NDEF格式
            if (rawMessage != null) {
                LogUtil.i(TAG, "getNdefMsg: ndef格式 ");
                ndefMessages = new NdefMessage[rawMessage.length];
                LogUtil.i(TAG, "rawMessage.length : " + rawMessage.length );
                for (int i = 0; i < rawMessage.length; i++) {
                    ndefMessages[i] = (NdefMessage) rawMessage[i];
                    LogUtil.i(TAG, "ndefMessages[i] : " + ndefMessages[i] );
                    NdefRecord record = ndefMessages[i].getRecords()[0];
                    String content = ByteUtil.hexStr2Str(ByteUtil.bytes2HexStr(record.getPayload()));
                    LogUtil.i(TAG, "getPayload1 : " + ByteUtil.bytes2HexStr(record.getPayload()) );
                    LogUtil.d(TAG, "getPayload2:[" +ByteUtil.hexStr2Str(ByteUtil.bytes2HexStr(record.getPayload())) + "].");
                    //mHandler.sendEmptyMessage(1006);

                    Message msg = mHandler.obtainMessage();
                    msg.what = 1006;
                    msg.obj = content;
                    mHandler.sendMessage(msg);
                }
            } else {
                //未知类型 (公交卡类型)
                LogUtil.i(TAG, "getNdefMsg: 未知类型");
                //对应的解析操作，在Github上有
            }
            //return ndefMessages;
            LogUtil.d(TAG, "getNdefMsg 2");
            return;
        }
        //return null;
        LogUtil.d(TAG, "getNdefMsg end");
        return;
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
            getNdefMsg(intent);
    }

    private void initNFC() {
        LogUtil.d(TAG, "initNFC start");
        mDefaultAdapter = NfcAdapter.getDefaultAdapter(mContext);
        //add by wangxing for taskid 2611
        try{
            if(!mDefaultAdapter.isEnabled()){
                LogUtil.d(TAG, "initNFC 2");
                mLastNfcStatus = false;
                mDefaultAdapter.enable();
            }
        }catch (Exception e){
            LogUtil.d(TAG, "initNFC exception: " + e.getMessage());
        }

        NFC_TECHLISTS = new String[][]{{IsoDep.class.getName()}, {NfcA.class.getName()}, {NfcB.class.getName()}, {NfcF.class.getName()}, {NfcV.class.getName()},
                {Ndef.class.getName()}, {NdefFormatable.class.getName()}, {MifareClassic.class.getName()}, {MifareUltralight.class.getName()}, {NfcBarcode.class.getName()}};
        try {
            NFC_FILTERS =
                    new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED, "*/*") ,
                            new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED,"*/*"),
                            new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED,"*/*")};
        } catch (IntentFilter.MalformedMimeTypeException e) {
            LogUtil.d(TAG, "initNFC create NFC_FILTERS exception: " + e.getMessage());
        }
        pendingIntent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		LogUtil.d(TAG, "initNFC end");
    }

    @Override
    public void onClick(View v) {
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
