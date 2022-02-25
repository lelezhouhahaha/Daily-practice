package com.meigsmart.meigrs32.activity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
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
import com.meigsmart.meigrs32.util.CitTestJni;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import butterknife.BindView;


public class PICCTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private PICCTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @BindView(R.id.content_show)
    public TextView mContentShow;

    public static final int PICC_TEST_MSG = 1001;
    public static final int PICC_TEST_UPDATE_MSG = 1002;
    public static final int TEST_SUCCESS = 1003;
    public static final int TEST_FAIL = 1004;
    public static final int PICC_TEST_HANDLERTHREAD_MSG = 1;

    public static final String PICC_TEST_COMMAND = "castles mifare_test 1";

    private String p_msg,p_command;
    private boolean mTestEnding = false;

    public boolean isMC511 = DataUtil.getDeviceName().equals("MC511");

    private boolean isMC518 = "MC518".equals(DataUtil.getDeviceName());
    private CitTestJni mCitTestJni = null;
    private int mRspStatus = -1;
    private HandlerThread mHanlerThread = null;
    private Handler mPICCTestHandler = null;
    private int TEST_TIMES = 5;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_icctest;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.PICCTestActivity);//待修改

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        Log.d("picctest","init");
        /*Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.readCardModule"); /*com.idtechproducts.device.sdkdemo*/
        /*if (intent != null) {
            Log.d("picctest","start");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);
            startActivityForResult(intent,11);
        }*/
        if(isMC511){
            super.startBlockKeys = Const.isCanBackKey;
            try {
                runShellCommand(PICC_TEST_COMMAND);
            }catch (Exception e) {
                LogUtil.e("START_TEST_COMMAND: " +e.getMessage());
            }
        }else if(isMC518){
            mCitTestJni = new CitTestJni();
            mHanlerThread = new HandlerThread("PICCHandlerThread");
            mHanlerThread.start();
            mPICCTestHandler = new Handler(mHanlerThread.getLooper()){
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what){
                        case PICC_TEST_HANDLERTHREAD_MSG:
                            LogUtil.i("testPicc start TEST_TIMES="+TEST_TIMES);
                            mRspStatus = mCitTestJni.testPicc();
                            TEST_TIMES--;
                            mHandler.removeMessages(PICC_TEST_UPDATE_MSG);
                            mHandler.sendEmptyMessageDelayed(PICC_TEST_UPDATE_MSG,100);
                            LogUtil.i("testPicc end mRspStatus="+mRspStatus);
                            break;
                    }

                }
            };
            mPICCTestHandler.sendEmptyMessage(PICC_TEST_HANDLERTHREAD_MSG);

        }else {
            ComponentName componentName = new ComponentName("com.android.readCardModule", "com.android.readCardModule.PICCTestActivity");
            Intent intent = new Intent();
            intent.setComponent(componentName);
            startActivityForResult(intent, 11);
            mSuccess.setVisibility(View.VISIBLE);
            Log.d("picctest", "end");
        }


    }

    final Runnable test_runnable = new Runnable() {
        public void run() {
            new Thread(new Runnable(){
                public void run()
                {
                    try {
                        String line;
                        Process process = Runtime.getRuntime().exec(p_command);
                        process.waitFor();
                        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        while ((line = input.readLine()) != null) {
                            p_msg = p_msg + line + "\n";
                        }
                        input.close();
                        if(!mTestEnding && PICC_TEST_COMMAND.equals(p_command)){
                            mHandler.sendEmptyMessageDelayed(PICC_TEST_MSG,300);
                        }
                    }
                    catch (Exception e) {
                        Log.d("DebugMsg", e.getMessage());
                    }
                }
            }).start();
        }};

    private void runShellCommand(String command) throws Exception {
        p_command = command;
        p_msg = "";
        mHandler.postDelayed(test_runnable, 800);
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LogUtil.i("p_msg="+p_msg);
            switch (msg.what){

                case PICC_TEST_MSG:
                    if(p_msg.contains("ok")){
                        mContentShow.setText(p_msg);
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        deInit(mFatherName, SUCCESS);
                    }else{
                        try {
                            runShellCommand(PICC_TEST_COMMAND);
                        }catch (Exception e) {
                            LogUtil.e("START_TEST_COMMAND: " +e.getMessage());
                        }
                    }

                    break;
                case PICC_TEST_UPDATE_MSG:
                    String text = "";
                    if(mRspStatus == 0){
                        text = getString(R.string.PICCTestActivity) + ": "+getString(R.string.success)+"\n"+mCitTestJni.getPiccRspText();
                        mContentShow.setText(text);
                        mSuccess.setVisibility(View.VISIBLE);
                        mHandler.sendEmptyMessageDelayed(TEST_SUCCESS,300);
                    }else{
                        if(TEST_TIMES > 0){
                            mPICCTestHandler.removeMessages(PICC_TEST_HANDLERTHREAD_MSG);
                            mPICCTestHandler.sendEmptyMessageDelayed(PICC_TEST_HANDLERTHREAD_MSG,1000);
                        }else {
                            text = getString(R.string.PICCTestActivity) + ": " + getString(R.string.fail);
                            mContentShow.setText(text);
                            mHandler.sendEmptyMessageDelayed(TEST_FAIL,300);
                        }
                    }

                    LogUtil.i("mRspStatus="+mRspStatus+" getIccRspText="+mCitTestJni.getPiccRspText());
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
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPICCTestHandler != null){
            mPICCTestHandler.removeMessages(PICC_TEST_HANDLERTHREAD_MSG);
        }
        if(mHanlerThread != null){
            mHanlerThread.quit();
        }

        mHandler.removeCallbacks(test_runnable);
        mHandler.removeMessages(PICC_TEST_MSG);
        mHandler.removeMessages(PICC_TEST_UPDATE_MSG);
        mHandler.removeMessages(TEST_SUCCESS);
        mHandler.removeMessages(TEST_FAIL);
        mHandler.removeMessages(9999);

    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 11 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }

}
