package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
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

import butterknife.BindView;

public class ScanActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack,Runnable{

    private static final String TAG = "ScanActivity";
    private ScanActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.editText)
    public EditText mEditText;
    @BindView(R.id.scanButton)
    public Button mScanButton;
    @BindView(R.id.scanStatus)
    public TextView mScanStatus;

    private String mFatherName = "";

    private int currPosition = 0;
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private int TIME_VALUES = 1000;

    private boolean isSLB786 = false;
    private String TAG_DEVICE_NAME = "common_device_name_test";

    private String TAG_MC510 = "common_device_name_test";
    private boolean is_MC510 = false;

    private String TAG_MC501 = "common_device_name_test";
    private boolean isMC501 = false;
    private String TAG_DMR_SCAN_SWITCH = "common_dmr_scan_switch_tag";
    private String TAG_DMR_PTT = "common_dmr_ptt_tag";
    private String mDmrScanSwitchNode = "/sys/devices/platform/soc/soc:meig-dmr-adc/meig-dmr-adc/dmr_scan_switch";
    private String mDmrPttNode = "/sys/devices/platform/soc/soc:meig-dmr-adc/meig-dmr-adc/dmr_ptt";

    private String scanStr = null;

    private Boolean isSLB783 = false;

    private boolean isMC901 = false;
    private static final int REQUEST_CODE_SCAN = 0x0000;
    private static final String DECODED_CONTENT_KEY = "codedContent";
    private static final String DECODED_STATE_KEY = "codedState";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_scan;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_scan);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        isSLB786 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_DEVICE_NAME).equals("SLB786");
        is_MC510 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_MC510).equals("MC510");
        isMC501 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_MC501).equals("MC501");

        isSLB783 = DataUtil.getDeviceName().equals("SLB783");
        isMC901 = DataUtil.getDeviceName().equals("MC901");

        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        if(isSLB786){
            mScanButton.setVisibility(View.VISIBLE);
            mScanStatus.setVisibility(View.VISIBLE);
            mScanButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                        Intent intent = new Intent("com.scan.onStartScan");
                        sendBroadcast(intent);
                    }
                    if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                        Intent intent = new Intent("com.scan.onEndScan");
                        sendBroadcast(intent);
                    }
                    return false;
                }
            });
        }

        if (isMC501){//isMC501
            String scanSwitchNodeFromConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_DMR_SCAN_SWITCH);
            String pttNodeFromConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_DMR_PTT);
            if(!scanSwitchNodeFromConfig.isEmpty()){
                mDmrScanSwitchNode = scanSwitchNodeFromConfig;
            }
            if(!pttNodeFromConfig.isEmpty()){
                mDmrPttNode = pttNodeFromConfig;
            }
            DataUtil.readLineFromFile(mDmrScanSwitchNode);
            writeToFile(mDmrScanSwitchNode,"10");
            writeToFile(mDmrPttNode,"1");

            mScanButton.setText(R.string.ScanSingleActivity);
            mScanButton.setVisibility(View.VISIBLE);
            mScanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityToScan();
                }
            });

            startActivityToScan();

        }else if(isMC901){
            mScanButton.setText(R.string.ScanSingleActivity);
            mScanButton.setVisibility(View.VISIBLE);
            mScanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityToScan();
                }
            });

            startActivityToScan();
        }

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
        //mEditText.setInputType(InputType.TYPE_NULL);
        mEditText.setShowSoftInputOnFocus(false);

        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        mEditText.addTextChangedListener(watcher);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return (event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            }
        });
        mFail.setFocusable(false);//add by wangxing for SLB783 P_RK95_E-717
    }

    private void startActivityToScan(){
        Intent intent = new Intent();
        ComponentName componentName;
        if(isMC901) {
            componentName = new ComponentName("com.ubx.scandemo", "com.ubx.scandemo.ScanDemoActivity");
            intent.setComponent(componentName);

            startActivityForResult(intent, REQUEST_CODE_SCAN);
        }else if(isMC501){
            componentName = new ComponentName("com.example.scantest", "com.example.scantest.MainActivity");
            intent.setComponent(componentName);
            Bundle bundle = new Bundle();
            bundle.putString("scanTestD", "ok");
            intent.putExtras(bundle);
            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (resultCode) {
            case 1210:
                Intent intent1 = getIntent();
                Bundle bundle =intent.getExtras();
                scanStr = bundle.getString("buffer").trim();
                Log.d(TAG, "onActivityResult: buffer:"+scanStr);
                //mHandler.sendEmptyMessageAtTime(1003, 100);
                mEditText.setText(scanStr);
                Log.d(TAG, "onActivityResult: setText"+scanStr);
                Log.d(TAG, "onActivityResult: buffer:"+scanStr);
                break;
            default:
                break;
        }

        //mc901
        if(intent != null && requestCode == REQUEST_CODE_SCAN ) {
            String content = intent.getStringExtra(DECODED_CONTENT_KEY);
            boolean state = intent.getBooleanExtra(DECODED_STATE_KEY , false);
            if(state) {
                mEditText.setText(content);
            }
            Log.d(TAG, " content:"+content+"  state:"+state);
        }
    }

    TextWatcher watcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            LogUtil.d("citapk beforeTextChanged s:<" + s + "> .");
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            LogUtil.d("citapk onTextChanged s:<" + s + "> .");
        }

        @Override
        public void afterTextChanged(Editable s) {
            LogUtil.d("citapk afterTextChanged mEditText:<" + mEditText.getText().toString().trim() + "> .");
            if(mEditText.getText().toString().trim().length() > 0 ) {
                  LogUtil.d("citapk afterTextChanged .");
                mSuccess.setVisibility(View.VISIBLE);
				mHandler.sendEmptyMessageDelayed(1002, 1000);
            }
         }
    };

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.d("write to file " + path + "abnormal.");
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
                case 1001:
                    break;
				case 1002:
                    if((mFatherName.equals(MyApplication.PCBANAME))||(mFatherName.equals(MyApplication.PreNAME))){
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        deInit(mFatherName, SUCCESS);//auto pass pcba & pre
                    }
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
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
		mHandler.removeMessages(1002);
		mHandler.removeMessages(1003);
        if(isMC501){
            writeToFile(mDmrScanSwitchNode,"11");
        }

    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess ){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
            return true;
        } else   if (keyCode == 131 /*KeyEvent.KEYCODE_SCAN */) {
            LogUtil.d("press scan key");
            String editstring = mEditText.getText().toString().trim();
            if(!editstring.isEmpty()){
                mEditText.setText("");
            }
            return true;
        }
        if(is_MC510 && event.getScanCode()==111){
            Log.d("mc510scan","success");
            String mstring = mEditText.getText().toString().trim();
            if(!mstring.isEmpty()){
                mEditText.setText("");
            }
            return true;
        }
        LogUtil.i("event.getScanCode()="+event.getScanCode());
        if(isSLB783 && (event.getScanCode() == 765 || event.getScanCode() == 766 || event.getScanCode() == 528)){
            String editTextString = mEditText.getText().toString().trim();
            if(!editTextString.isEmpty()){
                mEditText.setText("");
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

    @Override
    public void run() {

    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(is786 && requestCode == 11 && data != null && data.getIntExtra("result",SUCCESS)==SUCCESS){
            mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        
    }*/

}

