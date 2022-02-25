package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
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
import com.meigsmart.meigrs32.util.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import butterknife.BindView;

public class ScalesActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{

    private ScalesActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    private String scale_path = "/dev/ttyS2";
    private SerialPort mSerialPort;
    private int baud = 9600;


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @BindView(R.id.content_show)
    public TextView mContentShow;
    private boolean mTestEnding = false;
    private Thread mThread;
    private Runnable mRunnable;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_scales;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.ScalesActivity);//待修改

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mSerialPort =new SerialPort();

        mRunnable = new Runnable() {
            @Override
            public void run() {
                mSerialPort.pin_test(scale_path,baud, "scales test");
                if(null!=mSerialPort && mSerialPort.isStatus()){
                    try {
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    mHandler.sendEmptyMessage(1001);
                }
            }
        };
        mContentShow.setText(R.string.scale_fail);
        mThread = new Thread(mRunnable);
        mThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSerialPort != null){
            mSerialPort = null;
        }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    mContentShow.setText(R.string.scale_success);
                    mSuccess.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            mTestEnding = true;
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mTestEnding = true;
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }

    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            mTestEnding = true;
            deInit(mFatherName, result,Const.RESULT_NOTEST);
        }else if (result == 1){
            mTestEnding = true;
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            mTestEnding = true;
            deInit(mFatherName, result);
        }
    }
    static {
        System.loadLibrary("meigpsam-jni");
    }
}
