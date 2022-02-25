package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.KeyEvent;
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
import com.meigsmart.meigrs32.util.SerialPort;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.FileOutputStream;

import butterknife.BindView;

public class CradleUartTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    protected CradleUartTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    protected String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;

    // CradleUartTestActivity
    @BindView(R.id.cradle_uart)
    public TextView mCradleUart;


    //protected String CRADLE_UART_ENABLE_NODE_KEY = "common_cradle_uart_enble_node";
    protected String CRADLE_UART_NODE_KEY = "common_cradle_uart_node";

    //protected String cradle_uart_enable_node = "/sys/kernel/debug/msm_serial_hs/loopback.0";
    protected String cradle_uart_node = "/dev/ttyMSM1";

    private SerialPort mSerialPort1, mSerialPort2;
    private int baud = 115200;
    protected String TAG =  "CradleUartTestActivity";
    private String resultString;
    protected int mConfigTime = 0;
    private Runnable mRun;
    private Thread mThread;
    private final int UART_TIMEOUT = 10000;
    private int mUartTime = 0;
    private boolean uart1_status = false;


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cradle_uart;
    }

    protected void getDefaultConfigInfo() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        //String customCradelUartEnableNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CRADLE_UART_ENABLE_NODE_KEY);
        //if(customCradelUartEnableNode != null && !customCradelUartEnableNode.isEmpty()){
        //    cradle_uart_enable_node = customCradelUartEnableNode;
        //}

        String customCradleUartNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CRADLE_UART_NODE_KEY);
        if(customCradleUartNode != null && !customCradleUartNode.isEmpty()){
            cradle_uart_node = customCradleUartNode;
        }
    }

    @Override
    protected void initData() {
        getDefaultConfigInfo();
       // mFail.setVisibility(View.GONE);
        mTitle.setText(R.string.CradleUartTestActivity);

        mCradleUart.setVisibility(View.VISIBLE);


        //writeToFile(cradle_uart_enable_node,"1");
        mSerialPort1 =new SerialPort();

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mSerialPort1.uhf_test(cradle_uart_node, baud, "uart test");
            }
        });
        mThread.start();

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
        mHandler.sendEmptyMessageDelayed(1004, 1000);
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            LogUtil.d(" write path:< " + path + ">.");
            LogUtil.d(" write value:< " + value + ">.");
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e(TAG, "write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean isSuccess(){
        return uart1_status ;
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
                case 1004:
                    mUartTime += 1000;

                    if(mSerialPort1.isStatus()){
                        uart1_status = true;
                        resultString = getResources().getString(R.string.normal);
                    } else if (mUartTime > UART_TIMEOUT) {
                        resultString = getResources().getString(R.string.Abnormal);
                    } else {
                        resultString = getResources().getString(R.string.please_wait);
                    }
                    mCradleUart.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_uart_test_tag) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));


                    if (isSuccess()) {
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                            mHandler.sendEmptyMessageDelayed(1001, 1000);
                        }
                    } else if (mUartTime <= UART_TIMEOUT) {
                        mHandler.sendEmptyMessageDelayed(1004, 1000);
                    } else {
                        mHandler.sendEmptyMessageDelayed(1002, 1000);
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
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1004);
        mHandler.removeMessages(9999);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtil.d(" onKeyDown keycode: <" + keyCode + ">.");
        int scanCode = event.getScanCode();
        LogUtil.d(" onKeyDown scanCode: <" + scanCode + ">.");
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtil.d(" onKeyUp keycode: <" + keyCode + ">.");
        int scanCode = event.getScanCode();
        LogUtil.d(" onKeyUp scanCode: <" + scanCode + ">.");
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
