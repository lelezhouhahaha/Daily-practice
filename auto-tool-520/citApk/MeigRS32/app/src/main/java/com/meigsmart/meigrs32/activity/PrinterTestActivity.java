package com.meigsmart.meigrs32.activity;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.CitTestJni;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import butterknife.BindView;


public class PrinterTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private PrinterTestActivity mContext;
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

    public static final int PRINTER_TEST_MSG = 1001;
    public static final int PRINTER_TEST_UPDATE_MSG = 1002;
    public static final String PRINTER_TEST_COMMAND = "castles printer_test";

    private String p_msg,p_command;
    private boolean mTestEnding = false;

    private boolean isMC518 = "MC518".equals(DataUtil.getDeviceName());
    private CitTestJni mCitTestJni = null;
    private int mRspStatus = -1;
    private Thread mThread;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_printertest;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.PrinterTestActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        //mFail.setVisibility(View.GONE);
         if(isMC518){
             mCitTestJni = new CitTestJni();
             mThread = new Thread(new Runnable() {
                 @Override
                 public void run() {
                     mRspStatus = mCitTestJni.testPrinter();
                     mHandler.sendEmptyMessageDelayed(PRINTER_TEST_UPDATE_MSG,300);
                     LogUtil.i("testPrinter end mRspStatus="+mRspStatus);
                 }
             });
             mThread.start();
        }else {
             try {
                 runShellCommand(PRINTER_TEST_COMMAND);
             } catch (Exception e) {
                 LogUtil.e("PRINTER_TEST_COMMAND: " + e.getMessage());
             }
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
                        LogUtil.i("p_msg="+p_msg);
                        if(!mTestEnding && PRINTER_TEST_COMMAND.equals(p_command)){
                            mHandler.sendEmptyMessageDelayed(PRINTER_TEST_MSG,300);
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

                case PRINTER_TEST_MSG:
                    if(p_msg.contains("ok")){
                        mContentShow.setText(p_msg);
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        deInit(mFatherName, SUCCESS);
                    }else{
                        try {
                            runShellCommand(PRINTER_TEST_COMMAND);
                        }catch (Exception e) {
                            LogUtil.e("PRINTER_TEST_COMMAND: " +e.getMessage());
                        }
                    }
                    break;
                case PRINTER_TEST_UPDATE_MSG:
                    String text = "";
                    if(mRspStatus == 0){
                        text = getString(R.string.PrinterTestActivity) + ": "+getString(R.string.success);
                        mContentShow.setText(text);
                        mSuccess.setVisibility(View.VISIBLE);
                    }else{
                        text = getString(R.string.PrinterTestActivity) + ": "+getString(R.string.fail);
                        mContentShow.setText(text);
                    }
                    LogUtil.i("mRspStatus="+mRspStatus);
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
        mHandler.removeCallbacks(test_runnable);
        mHandler.removeMessages(PRINTER_TEST_MSG);
        mHandler.removeMessages(PRINTER_TEST_UPDATE_MSG);
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


}
