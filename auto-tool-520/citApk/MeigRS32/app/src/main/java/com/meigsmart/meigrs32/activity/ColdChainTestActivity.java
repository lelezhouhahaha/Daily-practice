package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.SerialPort;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.FileOutputStream;
import java.io.FileReader;

import butterknife.BindView;

public class ColdChainTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    private ColdChainTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.mcu_text_view)
    public TextView mMcuTextView;
    @BindView(R.id.heating_temp)
    public TextView mDevTempTextView;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private String mHeatingfwVersion = "/sys/clHeater/fwVersion";
    private String mHeatingDevTempNode = "/sys/clHeater/devTemp";
    private String mHeatingDevStatusNode = "/sys/clHeater/devStatus";
    private String mHeatingManualTempNode = "/sys/clHeater/manualTemp";
    private String mHeatingPowerDutyNode = "/sys/clHeater/devPowerDuty";
    private String mScannerTypeNode = "/sys/scan_choose/scan_value";

    private String mFwVersion;
    private boolean mMcuPass = false;
    private String mScannerType;

    private  String HEATER_VERSION = "FW Version:";
    private  int HEATER_VERSION_LENGTH_MIN = 16;

    private String mDevTemp;
    private final int BCR_TEMP = 0;
    private final int LCM_TMEP = 1;
    private float[] mFirstTemp = {0,0};
    private float[] mLastTemp = {0,0};
    private float TEMP_THRESHOLD = 2.0f;
    private String SCANNER4850 = "H";

    private String mLcmTestTemp = "e,l,40,40";
    private String mBcrTestTemp = "e,b,40,40";
    private String POWER_DUTY = "100,100";//bcr,lcm
    private String RESET_STATUS_CMD = "R";

    private int HEAER_TEST_TIME = 10; //10s


    private String HEATER_LCM_TEST_TEMP_COMMON_TAG = "common_heater_lcm_test_temp_tag";
    private String HEATER_BCR_TEST_TEMP_COMMON_TAG = "common_heater_bcr_test_temp_tag";
    private String HEATER_TEMP_TEST_THRESHOLD_COMMON_TAG = "common_heater_temp_test_threshold_tag";
    private String HEATER_RESET_STATUS_CMD_COMMON_TAG = "common_heater_reset_status_cmd_tag";
    private String HEATER_POWER_DUTY_COMMON_TAG = "common_heater_power_duty_tag";


    private String HEATER_VERSION_COMMON_TAG = "common_heater_version_tag";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_coldchain;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.ColdChainTestActivity);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mScannerType = readFromFile(mScannerTypeNode);

        String heaterPowerDutyConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_POWER_DUTY_COMMON_TAG);
        if(heaterPowerDutyConfig != null && !heaterPowerDutyConfig.isEmpty()){
            POWER_DUTY = heaterPowerDutyConfig;
        }

        String heaterResetConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_RESET_STATUS_CMD_COMMON_TAG);
        if(heaterResetConfig != null && !heaterResetConfig.isEmpty()){
            RESET_STATUS_CMD = heaterResetConfig;
        }

        writeToFile(mHeatingPowerDutyNode,POWER_DUTY);

        String heaterVersionFromConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_VERSION_COMMON_TAG);
        if(heaterVersionFromConfig != null && !heaterVersionFromConfig.isEmpty()){
            HEATER_VERSION = heaterVersionFromConfig;
        }

        String lcmTestTempFromConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_LCM_TEST_TEMP_COMMON_TAG);
        if(lcmTestTempFromConfig != null && !lcmTestTempFromConfig.isEmpty()){
            mLcmTestTemp = lcmTestTempFromConfig;
        }

        String bcrTestTempFromConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_BCR_TEST_TEMP_COMMON_TAG);
        if(bcrTestTempFromConfig != null && !bcrTestTempFromConfig.isEmpty()){
            mBcrTestTemp = bcrTestTempFromConfig;
        }

        String  tempThresholdFromConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HEATER_TEMP_TEST_THRESHOLD_COMMON_TAG);
        if(tempThresholdFromConfig != null && !tempThresholdFromConfig.isEmpty()){

            try {
                TEMP_THRESHOLD = Float.parseFloat(tempThresholdFromConfig);
            }catch (NumberFormatException e){
                LogUtil.e("TEMP_THRESHOLD parseFloat fail");
            }
        }

        setHeatingTempTextView();
        formatTempValue(true);
        writeToFile(mHeatingManualTempNode,mLcmTestTemp);
        //if(SCANNER4850.equals(mScannerType)) {
            writeToFile(mHeatingManualTempNode, mBcrTestTemp);
        //}
        mHandler.sendEmptyMessageDelayed(1000,1000);

        updateVersionInfo();

    }

    private void formatTempValue(boolean isFirst){

        String bcrTag = "BCR Temp:";
        String lcmTag = "LCM Temp:";
        String lcmTemp = null,bcrTemp = null;
        if(mDevTemp == null || mDevTemp.isEmpty() || !mDevTemp.contains(bcrTag) || !mDevTemp.contains(lcmTag)){
            return;
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
                }

            }
        }

        LogUtil.i("bcrTemp="+bcrTemp +" lcmTemp="+lcmTemp);

        if(isFirst){
            if(bcrTemp != null && !bcrTemp.isEmpty()){
                try {
                    mFirstTemp[BCR_TEMP] = Float.parseFloat(bcrTemp);
                }catch (NumberFormatException e){
                    LogUtil.e("bcrTemp parseFloat fail");
                }
            }
            if(lcmTemp != null && !lcmTemp.isEmpty()){
                try {
                    mFirstTemp[LCM_TMEP] = Float.parseFloat(lcmTemp);
                }catch (NumberFormatException e){
                    LogUtil.e("lcmTemp parseFloat fail");
                }
            }
            LogUtil.i("mFirstTemp[BCR_TEMP]="+mFirstTemp[BCR_TEMP]+"  mFirstTemp[LCM_TMEP]="+mFirstTemp[LCM_TMEP]);
        }else{
            if(bcrTemp != null && !bcrTemp.isEmpty()){
                try {
                    mLastTemp[BCR_TEMP] = Float.parseFloat(bcrTemp);
                }catch (NumberFormatException e){
                    LogUtil.e("bcrTemp parseFloat fail");
                }
            }
            if(lcmTemp != null && !lcmTemp.isEmpty()){
                try {
                    mLastTemp[LCM_TMEP] = Float.parseFloat(lcmTemp);
                }catch (NumberFormatException e){
                    LogUtil.e("lcmTemp parseFloat fail");
                }
            }
            LogUtil.i("mLastTemp[BCR_TEMP]="+mLastTemp[BCR_TEMP]+"  mLastTemp[LCM_TMEP]="+mLastTemp[LCM_TMEP]);
        }
    }
    private void setHeatingTempTextView(){
        mDevTemp = readFromFile(mHeatingDevTempNode);
        LogUtil.i("mDevTemp="+mDevTemp+"  mDevTemp.isEmpty()="+mDevTemp.isEmpty()+"  length="+mDevTemp.length());
        if(mDevTemp != null && !mDevTemp.isEmpty()){
            mDevTempTextView.setText(mDevTemp);
        }else{
            mDevTempTextView.setText(R.string.heating_test_abnormal);
            mHandler.sendEmptyMessageDelayed(1002,100);
        }

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

    private void updateResultText(){
        float bcrHeatingValue = mLastTemp[BCR_TEMP] - mFirstTemp[BCR_TEMP];
        float lcmHeatingValue = mLastTemp[LCM_TMEP] - mFirstTemp[LCM_TMEP];
        String textValue = mDevTemp;
        LogUtil.i("bcrHeatingValue="+bcrHeatingValue+"  lcmHeatingValue="+lcmHeatingValue+
                  "  mLastTemp[BCR_TEMP]="+mLastTemp[BCR_TEMP] +"  mFirstTemp[BCR_TEMP]="+mFirstTemp[BCR_TEMP]+
                "  mLastTemp[LCM_TMEP]="+mLastTemp[LCM_TMEP] +"  mFirstTemp[LCM_TMEP]="+mFirstTemp[LCM_TMEP]);
        if(SCANNER4850.equals(mScannerType)) {
            if (bcrHeatingValue >= TEMP_THRESHOLD) {
                textValue = textValue + "\n BCR:" + getString(R.string.success);
            } else {
                textValue = textValue + "\n BCR:" + getString(R.string.fail);
            }
        }
        if(lcmHeatingValue >= TEMP_THRESHOLD){
            textValue = textValue + "\n LCM:"+  getString(R.string.success);
        }else{
            textValue = textValue + "\n LCM:"+  getString(R.string.fail);
        }
        mDevTempTextView.setText(textValue);
    }

    private boolean isPass(){
        if(SCANNER4850.equals(mScannerType)) {
            if((mLastTemp[BCR_TEMP] - mFirstTemp[BCR_TEMP]) >= TEMP_THRESHOLD &&
                ((mLastTemp[LCM_TMEP] - mFirstTemp[LCM_TMEP]) >= TEMP_THRESHOLD) &&
                mMcuPass){
                return true;
            }
        }else{
            if(((mLastTemp[LCM_TMEP] - mFirstTemp[LCM_TMEP]) >= TEMP_THRESHOLD) &&
                    mMcuPass){
                return true;
            }
        }
        return false;
    }

    private void updateVersionInfo(){
        String mcuText;
        mFwVersion = readFromFile(mHeatingfwVersion);

        LogUtil.i("mFwVersion="+mFwVersion  );

        if(mFwVersion != null && !mFwVersion.isEmpty() && mFwVersion.contains(HEATER_VERSION) && (mFwVersion.length() >= HEATER_VERSION_LENGTH_MIN)){
            mcuText = getString(R.string.cold_chain_mcu_tag) + "  " + getString(R.string.normal);
            mMcuTextView.setText(mcuText);
            mMcuPass = true;
        }else{
            mcuText = getString(R.string.cold_chain_mcu_tag) + "  " + getString(R.string.Abnormal);
            mMcuTextView.setText(mcuText);
            mMcuPass = false;
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
        return value.trim();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1000:
                    setHeatingTempTextView();
                    HEAER_TEST_TIME--;
                    if(HEAER_TEST_TIME > 0){
                        mHandler.sendEmptyMessageDelayed(1000,1000);
                    }else {
                        formatTempValue(false);
                        updateResultText();
                        if (isPass()) {
                            mHandler.sendEmptyMessageDelayed(1001,1000);
                        } else {
                            mHandler.sendEmptyMessageDelayed(1002,1000);
                        }
                    }
                    break;
                case 1001:
                    deInit(mFatherName, SUCCESS);
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
        writeToFile(mHeatingPowerDutyNode,"50,50");
        writeToFile(mHeatingDevStatusNode,RESET_STATUS_CMD);
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }


    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            deInit(mFatherName, result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(mFatherName, result);
        }
    }

}
