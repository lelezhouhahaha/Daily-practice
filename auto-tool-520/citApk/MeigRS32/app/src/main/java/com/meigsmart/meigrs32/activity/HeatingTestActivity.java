package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
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
import android.text.TextUtils;
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
import java.io.FileReader;
import java.io.FileWriter;

import butterknife.BindView;

public class HeatingTestActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private HeatingTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    @BindView(R.id.heating_temp)
    public TextView mDevTempTextView;

    private int mConfigTime = 0;
   // private Runnable mRun;

    private String mHeatingManualTempNode = "/sys/clHeater/manualTemp";
    private String mHeatingDevTempNode = "/sys/clHeater/devTemp";
    private String mHeatingDevStatusNode = "/sys/clHeater/devStatus";
    private String mHeatingPowerDutyNode = "/sys/clHeater/devPowerDuty";
    private String mScannerTypeNode = "/sys/scan_choose/scan_value";


    private String mDevTemp;
    private String mScannerType;


    private int HEAER_TEST_TIME = 360; //360s

    private String mLcmTestTemp = "e,l,40,40";
    private String mBcrTestTemp = "e,b,40,40";
    private String POWER_DUTY = "100,100";//bcr,lcm
    private String RESET_STATUS_CMD = "R";
    private String SCANNER4850 = "H";
    private final float PASS_TEMP_MIN = 34.5f;
    private final float PASS_TEMP_MAX = 37.5f;
    private final int TEMP_OVER_PROTECT_FLAG = 0x0020;

    private String HEATER_LCM_TEST_TEMP_COMMON_TAG = "common_heater_lcm_test_temp_tag";
    private String HEATER_BCR_TEST_TEMP_COMMON_TAG = "common_heater_bcr_test_temp_tag";
    private String HEATER_TEST_TIME_COMMON_TAG = "common_heater_test_time_tag";
    private String HEATER_RESET_STATUS_CMD_COMMON_TAG = "common_heater_reset_status_cmd_tag";
    private String HEATER_POWER_DUTY_COMMON_TAG = "common_heater_power_duty_tag";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_heating;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.HeatingTestActivity);
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

        mScannerType = readFromFile(mScannerTypeNode);

        LogUtil.i("mScannerType="+mScannerType + "  mScannerType.length="+mScannerType.length());

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        String heaterPowerDutyConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_POWER_DUTY_COMMON_TAG);
        if(heaterPowerDutyConfig != null && !heaterPowerDutyConfig.isEmpty()){
            POWER_DUTY = heaterPowerDutyConfig;
        }

        String heaterResetConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_RESET_STATUS_CMD_COMMON_TAG);
        if(heaterResetConfig != null && !heaterResetConfig.isEmpty()){
            RESET_STATUS_CMD = heaterResetConfig;
        }

        String lcmTestTempFromConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_LCM_TEST_TEMP_COMMON_TAG);
        if(lcmTestTempFromConfig != null && !lcmTestTempFromConfig.isEmpty()){
            mLcmTestTemp = lcmTestTempFromConfig;
        }

        String bcrTestTempFromConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_BCR_TEST_TEMP_COMMON_TAG);
        if(bcrTestTempFromConfig != null && !bcrTestTempFromConfig.isEmpty()){
            mBcrTestTemp = bcrTestTempFromConfig;
        }

        String testTime = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_TEST_TIME_COMMON_TAG);
        if(testTime != null && !testTime.isEmpty()){
            try {
                HEAER_TEST_TIME = Integer.parseInt(testTime);
            }catch (Exception e){
                LogUtil.i("HEAER_TEST_TIME init fail");
            }
        }


        writeToFile(mHeatingPowerDutyNode,POWER_DUTY);
        setHeatingTempTextView();
        writeToFile(mHeatingManualTempNode,mLcmTestTemp);
        //if(SCANNER4850.equals(mScannerType)) {
            writeToFile(mHeatingManualTempNode, mBcrTestTemp);
        //}
        mHandler.sendEmptyMessageDelayed(1000,2000);

       /* mRun = new Runnable() {
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
        mRun.run();*/

    }

    private boolean isPass(){
        String bcrTag = "BCR Temp:";
        String lcmTag = "LCM Temp:";
        String dustTag = "Dust Temp:";
        String lcmTemp = null;
        String bcrTemp = null;
        String dustTemp = null;
        float lcmT = 0;
        float bcrT = 0;
        float dust = 0;
        if(mDevTemp == null || mDevTemp.isEmpty() || !mDevTemp.contains(bcrTag) || !mDevTemp.contains(lcmTag) || !mDevTemp.contains(dustTag)){
            return false;
        }

        String s[]= mDevTemp.split("\n");
        LogUtil.i("s.length="+s.length);
        for(int i=0; i < s.length; i++){
            LogUtil.i("s[i]="+s[i]);
            if(!TextUtils.isEmpty(s[i])){
                if(s[i].contains(bcrTag)){
                    bcrTemp = s[i].substring(bcrTag.length());
                }else if(s[i].contains(lcmTag)){
                    lcmTemp = s[i].substring(lcmTag.length());
                }else if(s[i].contains(dustTag)){
                    dustTemp = s[i].substring(dustTag.length());
                }

            }
        }

        LogUtil.i(" bcrTemp="+bcrTemp +" lcmTemp="+lcmTemp + "  dustTemp="+dustTemp);

        if(lcmTemp != null && !lcmTemp.isEmpty()){
            try {
                lcmT = Float.parseFloat(lcmTemp);
            }catch (NumberFormatException e){
                LogUtil.e("lcmTemp parseFloat fail");
            }
        }

        if(dustTemp != null && !dustTemp.isEmpty()){
            try {
                dust = Float.parseFloat(dustTemp);
            }catch (NumberFormatException e){
                LogUtil.e("dustTemp parseFloat fail");
            }
        }

        String devStatus = readFromFile(mHeatingDevStatusNode);
        LogUtil.i("devStatus="+devStatus );
        int devStatusValue = 0 ;
        try {
            if(!TextUtils.isEmpty(devStatus)) {

                devStatusValue = Integer.parseInt(devStatus.substring(devStatus.indexOf("0x")+2),16);
                LogUtil.e("devStatusValue = "+devStatusValue +"  TEMP_OVER_PROTECT_FLAG="+TEMP_OVER_PROTECT_FLAG);
            }
        }catch (NumberFormatException e){
            LogUtil.e("devStatusValue parseInt fail");
        }
        if(SCANNER4850.equals(mScannerType)) {
            if(bcrTemp != null && !bcrTemp.isEmpty()){
                try {
                     bcrT = Float.parseFloat(bcrTemp);
                }catch (NumberFormatException e){
                    LogUtil.e("bcrTemp parseFloat fail");
                }
            }

            LogUtil.i("bcrT="+bcrT+"  lcmT="+lcmT +"  dust="+dust);
            if(bcrT >= PASS_TEMP_MIN && bcrT <= PASS_TEMP_MAX && dust >= PASS_TEMP_MIN && dust <= PASS_TEMP_MAX){
                if((lcmT >= PASS_TEMP_MIN && lcmT <= PASS_TEMP_MAX) || ((devStatusValue & TEMP_OVER_PROTECT_FLAG) > 0 && lcmT> PASS_TEMP_MAX)) {
                    return true;
                }
            }
        }else{
            LogUtil.i("lcmT="+lcmT +"  dust="+dust);
            if( dust >= PASS_TEMP_MIN && dust <= PASS_TEMP_MAX){
                if((lcmT >= PASS_TEMP_MIN && lcmT <= PASS_TEMP_MAX) || ((devStatusValue & TEMP_OVER_PROTECT_FLAG) > 0 && lcmT> PASS_TEMP_MAX)) {
                    return true;
                }
            }
        }

        return false;
    }



    private void setHeatingTempTextView(){
        mDevTemp = readFromFile(mHeatingDevTempNode);
        LogUtil.i("mDevTemp="+mDevTemp+"  mDevTemp.isEmpty()="+mDevTemp.isEmpty()+"  length="+mDevTemp.length());
        if(mDevTemp != null && !mDevTemp.isEmpty()){
            mDevTempTextView.setText(mDevTemp);
        }else{
            mDevTempTextView.setText(R.string.heating_test_abnormal);
            mHandler.sendEmptyMessageDelayed(1002,1000);
        }

    }

    private String readFromFile(String node){
        String value = "";
        try {
            char[] buffer = new char[1024];

            FileReader fileReader = new FileReader(node);
            int len = fileReader.read(buffer, 0, buffer.length);
            String data = new String(buffer, 0, len);
            value = data;

            fileReader.close();
        } catch (Exception e) {
            LogUtil.e("Get node : " + node + "fail.");
            LogUtil.e("e : "+e.toString());
        }
        LogUtil.i("value="+value+"  "+value.length());
        return value.trim();
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            LogUtil.d(" heating path:< " + path + ">.");
            LogUtil.d(" heating value:< " + value + ">.");
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e( "write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1000:
                    setHeatingTempTextView();

                    HEAER_TEST_TIME -= 2;
                    if(HEAER_TEST_TIME > 0){
                        mHandler.sendEmptyMessageDelayed(1000,2000);
                    }else {
                        if (isPass()) {
                            deInit(mFatherName, SUCCESS);
                        } else {
                            deInit(mFatherName, FAILURE);
                        }
                    }
                    break;
                case 1001:
                    if (mFatherName.equals(MyApplication.RuninTestNAME) ){
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
        //mHandler.removeCallbacks(mRun);
        //writeToFile(mHeatingPowerDutyNode,"50,50");
        writeToFile(mHeatingDevStatusNode,RESET_STATUS_CMD);
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
