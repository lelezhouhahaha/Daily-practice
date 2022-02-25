package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
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
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.CitTestJni;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.util.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import butterknife.BindView;


public class MSRTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private MSRTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.retest)
    public Button mRetestBtn;
    private String mFatherName = "";
    private String msr_path = "/dev/ttyS2";
    private SerialPort mSerialPort;
    private int size;
    private int mConfigtime = 30;//test time
    private Runnable mRun;
    private ScanTask scanTask = null;


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @BindView(R.id.content_show)
    public TextView mContentShow;

    public static final int MSR_TEST_MSG = 1008;
    public static final int MSR_TEST_UPDATE_MSG = 1009;
    public static final int TEST_SUCCESS = 1010;
    public static final int TEST_FAIL = 1011;
    public static final String MSR_TEST_COMMAND = "castles msr_test 1";

    private String p_msg, p_command;
    private boolean mTestEnding = false;

    public boolean isMC511 = DataUtil.getDeviceName().equals("MC511");

    private boolean isMC518 = "MC518".equals(DataUtil.getDeviceName());
    private CitTestJni mCitTestJni = null;
    private int mRspStatus = -1;
    public static final int MSR_TEST_HANDLERTHREAD_MSG = 1;
    private HandlerThread mHanlerThread = null;
    private Handler mMsrTestHandler = null;
    // private int TEST_TIMES = 10;
    private boolean ApkExist = false;
    private boolean Test_result = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_msrtest;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.MSRTestActivity);//待修改
        String strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.TEST_DIALOG);
        if((null != strTmp) && !strTmp.isEmpty() && strTmp.contains("true")) {
            showDialog();
        }else{
            scanTask = new ScanTask();
            scanTask.execute();
        }

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mContentShow.setText(R.string.runing_init);

        Log.d("MSRTestActivity", "init");
        //Intent intent = getPackageManager().getLaunchIntentForPackage("com.idtechproducts.device.sdkdemo");
        /*Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.readCardModule");
        if (intent != null) {
            Log.d("msrtest","start");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);
            startActivityForResult(intent,11);
        }*/

    }

    private void showDialog() {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(MSRTestActivity.this);
        dialog.setMessage(R.string.msr_dialog_msg);
        dialog.setPositiveButton(R.string.str_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scanTask = new ScanTask();
                        scanTask.execute();
                    }
                });
        dialog.setNegativeButton(R.string.str_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }

    class ScanTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            if (isMC511) {
                startBlockKeys = Const.isCanBackKey;
                // mFail.setVisibility(View.GONE);
                try {
                    runShellCommand(MSR_TEST_COMMAND);
                } catch (Exception e) {
                    LogUtil.e("START_TEST_COMMAND: " + e.getMessage());
                }
            } else if (isMC518) {
                mCitTestJni = new CitTestJni();
                mHanlerThread = new HandlerThread("MSRHandlerThread");
                mHanlerThread.start();
                mMsrTestHandler = new Handler(mHanlerThread.getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case MSR_TEST_HANDLERTHREAD_MSG:
                                //LogUtil.i("testMSR start TEST_TIMES="+TEST_TIMES);
                                mRspStatus = mCitTestJni.testMsr();
                                //TEST_TIMES--;
                                mHandler.removeMessages(MSR_TEST_UPDATE_MSG);
                                mHandler.sendEmptyMessageDelayed(MSR_TEST_UPDATE_MSG, 100);
                                LogUtil.i("testMSR end mRspStatus=" + mRspStatus);
                                break;
                        }
                    }
                };
                mMsrTestHandler.sendEmptyMessage(MSR_TEST_HANDLERTHREAD_MSG);
            } else {
                if (checkApkExist("com.android.readCardModule")) {
                    ComponentName componentName = new ComponentName("com.android.readCardModule", "com.android.readCardModule.MSRTestActivity");
                    Intent intent = new Intent();
                    intent.setComponent(componentName);
                    startActivityForResult(intent, 11);
                    ApkExist = true;
                    Log.d("MSRTestActivity", "end");
                } else {
                    mSerialPort = new SerialPort();
                    int ret = mSerialPort.openSerial(1, msr_path, 9600);
                    if (ret != 0)
                        Log.d("MSRTestActivity", " openSerail fail. ret:" + ret);
                    size = mSerialPort.readSerial(1);
                    Log.d("MSRTestActivity", "size_0:" + size);
                    Test_result = true;
                    mRun = new Runnable() {
                        @Override
                        public void run() {
                            mConfigtime--;
                            if (mSerialPort != null) {
                                size = mSerialPort.readSerial(1);
                            }
                            if (size > 0) {
                                mHandler.sendEmptyMessage(1001);
                                mContentShow.setText(R.string.msr_success);
                            } else if (size == -1) {
                                mContentShow.setText(R.string.msr_checkfail);
                            }

//                    if ((mConfigtime < 0) ){
//                        mHandler.sendEmptyMessage(1111);
//                    }
                        }
                    };
                        mHandler.sendEmptyMessage(1002);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (isMC518) {
                mRetestBtn.setVisibility(View.VISIBLE);
                mRetestBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mContentShow.setText(R.string.runing_test);
                        mFail.setEnabled(false);
                        mSuccess.setEnabled(false);
                        mRetestBtn.setEnabled(false);
                        mMsrTestHandler = new Handler(mHanlerThread.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                switch (msg.what) {
                                    case MSR_TEST_HANDLERTHREAD_MSG:
                                        //LogUtil.i("testMSR start TEST_TIMES="+TEST_TIMES);
                                        mRspStatus = mCitTestJni.testMsr();
                                        //TEST_TIMES--;
                                        mHandler.removeMessages(MSR_TEST_UPDATE_MSG);
                                        mHandler.sendEmptyMessageDelayed(MSR_TEST_UPDATE_MSG, 100);
                                        LogUtil.i("testMSR end mRspStatus=" + mRspStatus);
                                        break;
                                }
                            }
                        };
                        mMsrTestHandler.sendEmptyMessage(MSR_TEST_HANDLERTHREAD_MSG);
                    }
                });
                mContentShow.setText(R.string.runing_test);
                mFail.setEnabled(false);
                mSuccess.setEnabled(false);
                mRetestBtn.setEnabled(false);
                mMsrTestHandler.sendEmptyMessage(MSR_TEST_HANDLERTHREAD_MSG);
            }
            if (ApkExist) {
                mSuccess.setVisibility(View.VISIBLE);
            }
            if (Test_result) {
                mContentShow.setText(R.string.msr_test);
            }
        }
    }


    final Runnable test_runnable = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        String line;
                        Process process = Runtime.getRuntime().exec(p_command);
                        process.waitFor();
                        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        while ((line = input.readLine()) != null) {
                            p_msg = p_msg + line + "\n";
                        }
                        input.close();
                        Log.i("MSRTestActivity", "p_msg=" + p_msg);
                        if (!mTestEnding && MSR_TEST_COMMAND.equals(p_command)) {
                            mHandler.sendEmptyMessageDelayed(MSR_TEST_MSG, 300);
                        }
                    } catch (Exception e) {
                        Log.d("MSRTestActivity", e.getMessage());
                    }
                }
            }).start();
        }
    };

    private void runShellCommand(String command) throws Exception {
        p_command = command;
        p_msg = "";
        mHandler.postDelayed(test_runnable, 800);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LogUtil.i("p_msg=" + p_msg);
            switch (msg.what) {
                case MSR_TEST_MSG:
                    if (p_msg.contains("ok")) {
                        mContentShow.setText(p_msg);
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        deInit(mFatherName, SUCCESS);
                    } else {
                        try {
                            runShellCommand(MSR_TEST_COMMAND);
                        } catch (Exception e) {
                            LogUtil.e("START_TEST_COMMAND: " + e.getMessage());
                        }
                    }
                    break;
                case MSR_TEST_UPDATE_MSG:
                    String text = "";
                    mFail.setEnabled(true);
                    mSuccess.setEnabled(true);
                    mRetestBtn.setEnabled(true);
                    if (mRspStatus == 0) {
                        text = getString(R.string.MSRTestActivity) + ": " + getString(R.string.success) + "\n" + mCitTestJni.getMsrRspText();
                        mContentShow.setText(text);
                        mSuccess.setVisibility(View.VISIBLE);
                        mHandler.sendEmptyMessageDelayed(TEST_SUCCESS, 300);
                    } else {
                        text = getString(R.string.MSRTestActivity) + ": " + getString(R.string.fail);
                        mContentShow.setText(text);
                        // mHandler.sendEmptyMessageDelayed(TEST_FAIL,300);

                    }

                    LogUtil.i("mRspStatus=" + mRspStatus + " getMsrRspText=" + mCitTestJni.getMsrRspText());
                    break;
                case TEST_SUCCESS:
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                    deInit(mFatherName, SUCCESS);
                    break;
                case TEST_FAIL:
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.red_800));
                    deInit(mFatherName, FAILURE);
                    break;
                case 9999:
                    mTestEnding = true;
                    if (mSerialPort != null) {
                        mSerialPort.closeSerial(1);
                        mSerialPort = null;
                    }
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
                case 1001:
                    if (mSerialPort != null) {
                        mSerialPort.closeSerial(1);
                        mSerialPort = null;
                    }
                    mSuccess.setVisibility(View.VISIBLE);
                    break;
                case 1002:
                    mHandler.sendEmptyMessageDelayed(1002, 2000);
                    mRun.run();
                    break;
                case 1111:
                    if (mSerialPort != null) {
                        mSerialPort.closeSerial(1);
                        mSerialPort = null;
                    }
                    deInit(mFatherName, FAILURE);
                    break;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSerialPort != null) {
            mSerialPort.closeSerial(1);
            mSerialPort = null;
        }

        if (mMsrTestHandler != null) {
            mMsrTestHandler.removeMessages(MSR_TEST_HANDLERTHREAD_MSG);
        }

        if (mHanlerThread != null) {
            mHanlerThread.quit();
        }

        if (mHandler != null) {
            mHandler.removeMessages(MSR_TEST_MSG);
            mHandler.removeMessages(MSR_TEST_UPDATE_MSG);
            mHandler.removeMessages(TEST_SUCCESS);
            mHandler.removeMessages(TEST_FAIL);
            mHandler.removeMessages(9999);
            mHandler.removeMessages(1002);
            mHandler.removeMessages(1001);
            mHandler.removeMessages(1111);
            mHandler.removeCallbacks(test_runnable);
        }

    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            mTestEnding = true;
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            if (mSerialPort != null) {
                mSerialPort.closeSerial(1);
                mSerialPort = null;
            }
            mTestEnding = true;
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            mTestEnding = true;
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            mTestEnding = true;
            deInit(mFatherName, result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            mTestEnding = true;
            deInit(mFatherName, result);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 11 && data != null) {
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }

    public boolean checkApkExist(String packageName) {
        if (packageName == null || "".equals(packageName))
            return false;
        try {
            ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static {
        System.loadLibrary("meigpsam-jni");
    }
}
