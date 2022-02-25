package com.meigsmart.meigrs32.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import butterknife.BindView;

public class USB3TestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{

    private USB3TestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.usbvalue)
    public TextView Usbvalue;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    public String usbvalue ="/sys/devices/soc/7000000.ssusb/host_speed";
    private final String USB_PATH_KEY = "common_cit_usb3_test_path";
    private final String LOG_TAG = "USB3TestActivity";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_usb_test;
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
        mTitle.setText(R.string.usb_3_test);
        mDialog.setCallBack(this);
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, USB_PATH_KEY);
        if (!TextUtils.isEmpty(temp))
            usbvalue = temp;
        Log.d(LOG_TAG,"usbvalue:"+usbvalue);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        getString(usbvalue);
    }

    @Override
    protected void onDestroy() {

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

    private  void getString(String path) {
        String prop = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            prop = reader.readLine();
            Log.d(LOG_TAG,"prop"+prop);
            Usbvalue.setText(prop);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(prop.equals("super-speed")){
            mSuccess.setVisibility(View.VISIBLE);
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