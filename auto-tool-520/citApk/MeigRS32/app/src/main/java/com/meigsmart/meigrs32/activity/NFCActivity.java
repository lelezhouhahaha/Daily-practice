package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class NFCActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private NFCActivity mContext;
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

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean mLastNfcStatus = true;//add by wangxing for taskid 2611

    private static IntentFilter[] NFC_FILTERS;
    private static String[][] NFC_TECHLISTS;

    private boolean isPass;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_nfc;
    }

    @Override
    protected void initData() {
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
    }

    private void showDialog() {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(NFCActivity.this);
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
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
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
                case 1000:
                    mFlag.setVisibility(View.GONE);
                    mLayout.setVisibility(View.VISIBLE);
                    isStartTest = true;
                    break;
                case 1001:
                    if (isPass || mFatherName.equals(MyApplication.RuninTestNAME) ){//modify by wangxing for bug P_RK95_E-706 run in log show pass
                        deInit(mFatherName, SUCCESS);
                    }else {
                        deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                    }
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE);
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
        try{
            if(mDefaultAdapter!=null){
                //add by wangxing for taskid 2611
                if(!mLastNfcStatus){
                    mDefaultAdapter.disable();
                }
                mDefaultAdapter.disableForegroundDispatch(this);//关闭前台发布系统
            }
        }catch (Exception e){

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mDefaultAdapter!=null){
            mDefaultAdapter.disableForegroundDispatch(this);//关闭前台发布系统
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mDefaultAdapter!=null) {
            mDefaultAdapter.enableForegroundDispatch(this, pendingIntent, NFC_FILTERS, NFC_TECHLISTS);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        String[] techList=mTag.getTechList();
        if(mTag!=null){
            LogUtil.w(techList.toString());
            if(mFatherName.equals(MyApplication.PreSignalNAME)||mFatherName.equals(MyApplication.PCBASignalNAME)) {
                mSuccess.setVisibility(View.VISIBLE);
            }else {
                isPass = true;
                mHandler.sendEmptyMessage(1001);
            }
        }
    }

    private void initNFC() {
        mDefaultAdapter = NfcAdapter.getDefaultAdapter(mContext);
        //add by wangxing for taskid 2611
        try{
            if(!mDefaultAdapter.isEnabled()){
                mLastNfcStatus = false;
                mDefaultAdapter.enable();
            }
        }catch (Exception e){

        }

        NFC_TECHLISTS = new String[][]{{IsoDep.class.getName()}, {NfcA.class.getName()}, {NfcB.class.getName()}, {NfcF.class.getName()}, {NfcV.class.getName()},
                {Ndef.class.getName()}, {NdefFormatable.class.getName()}, {MifareClassic.class.getName()}, {MifareUltralight.class.getName()}, {NfcBarcode.class.getName()}};
        try {
            NFC_FILTERS =
                    new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED, "*/*") ,
                            new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED,"*/*"),
                            new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED,"*/*")};
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        pendingIntent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
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
