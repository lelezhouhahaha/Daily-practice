package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.SerialPort;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class RS232TestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    private RS232TestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    @BindView(R.id.tty_test)
    public TextView mRs232Msg;
    private final int REQUEST_RJ11 = 0;
    private final int BAUDRATE = 115200;

    private String PATH_NODE = "/dev/ttyHSL1";
    public  final String TAG_RS232_PATH_NODE = "common_RS232TestActivity_cit_rs232_node";
    public  final String TAG_RS232_PATH_BAUDRATE = "common_RS232TestActivity_cit_rs232_baudrate";
    private Thread mThread;
    private Runnable mRunnable;
    private SerialPort mSerialPort;
    private int baudrate = 0;
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
    @Override
    protected int getLayoutId() {
        return R.layout.activity_rs232_test;
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
        mTitle.setText(R.string.RS232TestActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);


        String path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_RS232_PATH_NODE);
        if(path != null && !path.isEmpty()){
            PATH_NODE = path;
        }

        String baudrateStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_RS232_PATH_BAUDRATE);
        if(baudrateStr != null && !baudrateStr.isEmpty()){
            baudrate = Integer.parseInt(baudrateStr);
        }

        LogUtil.d("PATH_NODE: "+PATH_NODE + " baudrate:" + baudrate);
        mSerialPort =new SerialPort();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                mSerialPort.serialLoopTest(PATH_NODE,baudrate, "pin test");
                if(mSerialPort.isStatus()){
                    try {
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    mHandler.sendEmptyMessage(1001);
                }else{
                    mHandler.sendEmptyMessage(1000);
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
                    mRs232Msg.setText(R.string.success);
                    mSuccess.setVisibility(View.VISIBLE);
                    if(is_mt535){
                        mSuccess.performClick();
                    }
                    break;
                case 1000:
                    mRs232Msg.setText(R.string.fail);
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if(mHandler != null) {
            mHandler.removeMessages(1000);
            mHandler.removeMessages(1001);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
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
        if (REQUEST_RJ11 == requestCode && resultCode == 0) {
            mSuccess.setVisibility(View.VISIBLE);
            if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                deInit(mFatherName, SUCCESS);//auto pass pcba
            }
        } else {
            if(requestCode == 11 && data != null)
                deInit(mFatherName, data.getIntExtra("result", FAILURE));

        }

    }

    static {
        System.loadLibrary("meigpsam-jni");
    }
}