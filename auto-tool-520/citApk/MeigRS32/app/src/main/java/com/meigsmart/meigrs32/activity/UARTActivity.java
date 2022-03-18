package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
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
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.SerialPort;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

import static com.meigsmart.meigrs32.util.DataUtil.initConfig;
import static com.meigsmart.meigrs32.util.DataUtil.readLineFromFile;

public class UARTActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private UARTActivity mContext;
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
    @BindView(R.id.startButton)
    public Button startButton;
    private final int REQUEST_RJ11 = 0;

    private String PATH_NODE = "/dev/ttyMSM0";
    public  final String TAG_UART_PATH_NODE = "common_cit_uart_node";
    public  final String TAG_UART_PATH_BAUDRATE = "common_cit_uart_baudrate";
    private String TAG_POGOPIN_OTG_TYPE_NODE = "dc_pogopin_otg_type_node";
    private SerialPort mSerialPort;
    private boolean uartvalue = true;
    private int baudrate = 115200;
    private final String TAG = UARTActivity.class.getSimpleName();
    private Boolean mCurrentTestResult = false;
    private Runnable mRun = null;
    //rivate Handler mHandler = null;
    private int mConfigTime = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_uart;
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
        startButton.setOnClickListener(this);
        mTitle.setText(R.string.UARTActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);


        String path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_UART_PATH_NODE);
        if(path != null && !path.isEmpty()){
            PATH_NODE = path;
        }

        String baudrateStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_UART_PATH_BAUDRATE);
        if(baudrateStr != null && !baudrateStr.isEmpty()){
            baudrate = Integer.parseInt(baudrateStr);
        }
        Log.d("UARTActivity","PATH_NODE: "+PATH_NODE + " baudrate:" + baudrate);
        mSerialPort =new SerialPort();

        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
            mRun = new Runnable() {
                @Override
                public void run() {
                    mConfigTime--;
                    LogUtil.d(TAG, "initData mConfigTime:" + mConfigTime);
                    updateFloatView(mContext, mConfigTime);
                    if ((mConfigTime == 0) && (mFatherName.equals(MyApplication.PCBAAutoTestNAME))) {
                        if (mCurrentTestResult) {
                            deInit(mFatherName, SUCCESS);
                        } else {
                            LogUtil.d(TAG, " Test fail!");
                            deInit(mFatherName, FAILURE, " Test fail!");
                        }
                        return;
                    }
                    mHandler.postDelayed(this, 1000);
                }
            };
            mRun.run();
        }

    }

    private boolean isPogopinOtg(){
        boolean mRet = false;
        String type_node = initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_POGOPIN_OTG_TYPE_NODE);
        Log.d("UARTActivity", "type_node:" + type_node);
        if ((type_node != null) && !type_node.isEmpty()) {
            String type = readLineFromFile(type_node);
            int mUsbTypeValue = Integer.valueOf(type);
            Log.d("UARTActivity", "type:" + type +  " mUsbTypeValue:" + mUsbTypeValue);
            if((mUsbTypeValue >= 800000) &&  (mUsbTypeValue <= 1100000))
                mRet = true;
        }
        Log.d("UARTActivity", "mRet:" + mRet);
        return mRet;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1000:
                //    startButton.setEnabled(true);
                    startButton.setText(R.string.start_test);
                    mRs232Msg.setText(R.string.fail);
                    break;
                case 1001:
                    uartvalue = false;
                    startButton.setEnabled(true);
                    startButton.setText(R.string.start_twice);
                    mRs232Msg.setText(R.string.testtwice);
                    break;
                case 1002:
                    mRs232Msg.setText(R.string.gpiocmplete);
                    mSuccess.setVisibility(View.VISIBLE);
                    if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)
                            || mFatherName.equals(MyApplication.MMI1_PreName) || mFatherName.equals(MyApplication.MMI2_PreName) || mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        mCurrentTestResult = true;
                        deInit(mFatherName, SUCCESS);//auto pass pcba
                    }
                    break;
                case 1003:
                    startButton.setEnabled(true);
                    mRs232Msg.setText(R.string.uart_button);
                    startButton.setText(R.string.start_twice);
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mHandler != null) {
            if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                mHandler.removeCallbacks(mRun);
            }
            mHandler.removeMessages(1000);
            mHandler.removeMessages(1001);
            mHandler.removeMessages(1002);
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
        if (v == startButton) {
            startButton.setEnabled(false);
            if(uartvalue) {
                new UARTTask(0).execute();
            }else {
                new UARTTask(1).execute();
            }
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
            if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME) || mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                mCurrentTestResult = true;
                deInit(mFatherName, SUCCESS);//auto pass pcba
            }
        } else {
            if(requestCode == 11 && data != null)
                deInit(mFatherName, data.getIntExtra("result", FAILURE));

        }

    }

    class UARTTask extends AsyncTask {
        private int type;
        UARTTask(int type){
            this.type = type;
        }
        @Override
        protected Object doInBackground(Object[] objects) {
            if(type == 0) {
                if (!isPogopinOtg()) {
                    Log.d("UARTActivity","test one node false");
                    mHandler.sendEmptyMessage(1000);
                    return null;
                } else {
                    mSerialPort.setStatus(false);
                    mSerialPort.pin_test(PATH_NODE, baudrate, "pin test");
                    if (mSerialPort.isStatus()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("UARTActivity","test one complete");
                        mHandler.sendEmptyMessage(1001);
                    } else {
                        Log.d("UARTActivity","test one status false");
                        mHandler.sendEmptyMessage(1000);
                    }
                }
            }
            if(type == 1) {
                if (!isPogopinOtg()) {
                    mSerialPort.setStatusTwice(false);
                    mSerialPort.uhf_test(PATH_NODE, baudrate, "pin test");
                    if (mSerialPort.isStatusTwice()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("UARTActivity","test two status false");
                        mHandler.sendEmptyMessage(1003);
                    } else {
                        Log.d("UARTActivity","test complete");
                        mHandler.sendEmptyMessage(1002);
                    }
                }else{
                    mSerialPort.setStatusTwice(false);
                    mSerialPort.uhf_test(PATH_NODE, baudrate, "pin test");
                    if (mSerialPort.isStatusTwice()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("UARTActivity","test two status and node false");
                        mHandler.sendEmptyMessage(1003);
                    } else {
                        Log.d("UARTActivity","test two node false");
                        mHandler.sendEmptyMessage(1003);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

        }
    }

    static {
        System.loadLibrary("meigpsam-jni");
    }
}