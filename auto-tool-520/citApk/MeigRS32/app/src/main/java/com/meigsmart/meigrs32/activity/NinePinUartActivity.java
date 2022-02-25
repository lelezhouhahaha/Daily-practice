package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.util.SerialPort;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class NinePinUartActivity  extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private NinePinUartActivity mContext;
    @BindView(com.meigsmart.meigrs32.R.id.title)
    public TextView mTitle;
    @BindView(com.meigsmart.meigrs32.R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    private int baud = 115200;


    @BindView(com.meigsmart.meigrs32.R.id.success)
    public Button mSuccess;
    @BindView(com.meigsmart.meigrs32.R.id.fail)
    public Button mFail;

    private SerialPort mSerialPort;
    private String NPUART_PATH = "/dev/ttyMSM0";
    private Thread mThread;
    private Runnable mRunnable;

    @Override
    protected int getLayoutId() {
        return com.meigsmart.meigrs32.R.layout.activity_npuart;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(com.meigsmart.meigrs32.R.string.NinePinUartActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mSerialPort =new SerialPort();

        mRunnable = new Runnable() {
            @Override
            public void run() {
                mSerialPort.pin_test(NPUART_PATH,baud, "pin test");
                Log.d("zhrr","pintest0w0");
                if(mSerialPort.isStatus()){
                    try {
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    mHandler.sendEmptyMessage(1001);
                }
            }
        };
        mThread = new Thread(mRunnable);
        mThread.start();


    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mSuccess.setVisibility(View.VISIBLE);
            }
        }
    };



    @Override
    protected void onDestroy() {
        mHandler.removeMessages(1001);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 11 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }

    static {
        System.loadLibrary("meigpsam-jni");
    }

}
