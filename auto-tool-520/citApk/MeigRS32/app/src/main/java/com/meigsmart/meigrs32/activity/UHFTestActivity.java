package com.meigsmart.meigrs32.activity;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.SerialPort;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import butterknife.BindView;


public class UHFTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private UHFTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.RxTx)
    public TextView mRxTxStatus;
    @BindView(R.id.powerSupplyStatus)
    public TextView mPowerSupplyStatus;
    @BindView(R.id.eintTriggleStatus)
    public TextView mEintTriggleStatus;
    private String UHF_PATH="dev/ttyHSL3";
    private String UHF_SUPPLY_NODE_PATH = "common_uhf_supply_node_path";
    private String UHF_POWER_SUPPLY_NODE_KEY = "common_uhf_power_supply_node";
    private String UHF_EINT_PIN_NODE_KEY = "common_uhf_eint_pin_path";
    private String uhf_power_supply_node = "";
    private String uhf_eint_pin_path = "";
    private String uhf_power_supply_status = "0";
    private boolean uhf_eint_trigger_status = false;
    private SerialPort mSerialPort;
    private int baud = 115200;
    private int mUhfStatus = -1;
    private String TAG =  "UHFTestActivity";
    String eintTriggleStatus;

    private String TAG_PLATFORM = "common_device_name_test";
    private boolean is_SLB783 = false;
    private String  platform ="";
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_uhf;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        //mBack.setVisibility(View.GONE);
        //mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mFail.setVisibility(View.GONE);
        mTitle.setText(R.string.pcba_uhftest);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        eintTriggleStatus = getResources().getString(R.string.Abnormal);
        platform = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_PLATFORM);
        if ("MC531".equals(platform)) {
         mPowerSupplyStatus.setVisibility(View.GONE);
        mEintTriggleStatus.setVisibility(View.GONE);          
 

        }



        UHF_PATH = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_SUPPLY_NODE_PATH);
        if(UHF_PATH == null || UHF_PATH.isEmpty()){
            UHF_PATH="dev/ttyHSL3";
        }

        uhf_eint_pin_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_EINT_PIN_NODE_KEY);

        uhf_power_supply_node = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_POWER_SUPPLY_NODE_KEY);
        if(uhf_power_supply_node != null && !uhf_power_supply_node.isEmpty()) {
            uhf_power_supply_status = DataUtil.readLineFromFile(uhf_power_supply_node);
            if(uhf_power_supply_node == null || uhf_power_supply_node.isEmpty())
                uhf_power_supply_status = "0";
        }
        String powerSupplyStatusStr = "";

        //add for SLB783
        is_SLB783 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_PLATFORM).equals("SLB783");
        if(is_SLB783) {
            if (uhf_power_supply_node != null && !uhf_power_supply_node.isEmpty() && writeToFile(uhf_power_supply_node, "1")) {
                powerSupplyStatusStr = getResources().getString(R.string.normal);
                uhf_power_supply_status = "1";
            } else {
                powerSupplyStatusStr = getResources().getString(R.string.Abnormal);
                uhf_power_supply_status = "0";
            }
        }else{

            if (uhf_power_supply_status.equals("1")) {
                powerSupplyStatusStr = getResources().getString(R.string.normal);
            } else {
                powerSupplyStatusStr = getResources().getString(R.string.Abnormal);
            }
        }
        mPowerSupplyStatus.setText(Html.fromHtml(
                getResources().getString(R.string.uhf_power_supply_status) +
                        "&nbsp;" + "<font color='#FF0000'>" + powerSupplyStatusStr + "</font>"
        ));
        mSerialPort =new SerialPort();
        mHandler.sendEmptyMessageDelayed(1005, 1000);
        if(!uhf_eint_pin_path.isEmpty()) {
            writeToFile(uhf_eint_pin_path, "0");
        }
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            LogUtil.d(" uhf path:< " + path + ">.");
            LogUtil.d(" uhf value:< " + value + ">.");
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e(TAG, "write to file " + path + "abnormal.");
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
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE);
                    break;
                case 1005:
                    if(!uhf_eint_pin_path.isEmpty()) {
                        writeToFile(uhf_eint_pin_path, "1");
                    }
                    mSerialPort.uhf_test(UHF_PATH,baud, "ufh test");
                    if(mSerialPort.isStatus()){
                        try {
                            Thread.sleep(1000);
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        //mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        //deInit(mFatherName, SUCCESS);
                        String status = getResources().getString(R.string.normal);
                        mRxTxStatus.setText(Html.fromHtml(
                                getResources().getString(R.string.RxTxStatus) +
                                        "&nbsp;" + "<font color='#FF0000'>" + status + "</font>"
                        ));
                    }else{
                        // fail
                        String status = getResources().getString(R.string.Abnormal);
                        mRxTxStatus.setText(Html.fromHtml(
                                getResources().getString(R.string.RxTxStatus) +
                                        "&nbsp;" + "<font color='#FF0000'>" + status + "</font>"
                        ));
                        //deInit(mFatherName, FAILURE);
                    }
                    if(uhf_eint_trigger_status) {
                        eintTriggleStatus = getResources().getString(R.string.normal);
                    }
                    mEintTriggleStatus.setText(Html.fromHtml(
                            getResources().getString(R.string.uhf_eint_triggle_status) +
                                    "&nbsp;" + "<font color='#FF0000'>" + eintTriggleStatus + "</font>"
                    ));
                    mFail.setVisibility(View.VISIBLE);
                   if("MC531".equals(platform)) {
                        if(mSerialPort.isStatus() ) {
                            mSuccess.setVisibility(View.VISIBLE);
                            mHandler.sendEmptyMessageDelayed(1001, 3000);
                        }else mHandler.sendEmptyMessageDelayed(1002, 3000);
                    }else {
                        if(mSerialPort.isStatus() && uhf_power_supply_status.equals("1") && uhf_eint_trigger_status) {
                            mSuccess.setVisibility(View.VISIBLE);
                            mHandler.sendEmptyMessageDelayed(1001, 3000);
                        }else mHandler.sendEmptyMessageDelayed(1002, 3000);
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
        if(!uhf_eint_pin_path.isEmpty()) {
            writeToFile(uhf_eint_pin_path, "1");
        }
        if(is_SLB783 && uhf_power_supply_node != null && !uhf_power_supply_node.isEmpty()) {
            writeToFile(uhf_power_supply_node, "0");
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(9999);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtil.d(" uhf onKeyDown keycode: <" + keyCode + ">.");
        int scanCode = event.getScanCode();
        LogUtil.d(" uhf onKeyDown scanCode: <" + scanCode + ">.");
        if (keyCode == 550 || scanCode == 545) {
                uhf_eint_trigger_status = true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtil.d(" uhf onKeyUp keycode: <" + keyCode + ">.");
        int scanCode = event.getScanCode();
        LogUtil.d(" uhf onKeyUp scanCode: <" + scanCode + ">.");
        if (keyCode == 550 || scanCode == 545) {
            uhf_eint_trigger_status = true;
        }
        return super.onKeyUp(keyCode, event);
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

    static {
        System.loadLibrary("meigpsam-jni");
    }

}
