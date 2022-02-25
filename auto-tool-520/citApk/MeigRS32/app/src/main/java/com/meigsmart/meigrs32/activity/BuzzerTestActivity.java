package com.meigsmart.meigrs32.activity;

import android.os.Handler;
import android.os.HandlerThread;
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


public class BuzzerTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private BuzzerTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.retest)
    public Button mRetestBtn;

    private String mFatherName = "";


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @BindView(R.id.content_show)
    public TextView mContentShow;

    public static final int BUZZER_TEST_MSG = 1001;
    public static final int BUZZER_TEST_UPDATE_MSG = 1002;
    public static final String BUZZER_TEST_COMMAND = "castles buzzer_test 5";

    private String p_msg,p_command;

    private boolean isMC518 = "MC518".equals(DataUtil.getDeviceName());
    private CitTestJni mCitTestJni = null;
    private int mRspStatus = -1;
    private int mTestCount = 2;
    public static final int BUZZER_TEST_HANDLERTHREAD_MSG = 1;
    private HandlerThread mHanlerThread = null;
    private Handler mBuzzerTestHandler = null;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_buzzertest;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.BuzzerTestActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mContentShow.setText(R.string.buzzer_test_content);
        if(isMC518){
            mCitTestJni = new CitTestJni();
            mHanlerThread = new HandlerThread("BuzzerHandlerThread");
            mHanlerThread.start();
            mBuzzerTestHandler = new Handler(mHanlerThread.getLooper()){
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what){
                        case BUZZER_TEST_HANDLERTHREAD_MSG:
                            LogUtil.i("testBeeper start mTestCount="+mTestCount);
                            mRspStatus = mCitTestJni.testBeeper();
                            mTestCount--;
                            if( mTestCount <= 0){
                                mHandler.removeMessages(BUZZER_TEST_UPDATE_MSG);
                                mHandler.sendEmptyMessageDelayed(BUZZER_TEST_UPDATE_MSG,100);
                            }else{
                                mBuzzerTestHandler.sendEmptyMessageDelayed(BUZZER_TEST_HANDLERTHREAD_MSG,500);
                            }

                            LogUtil.i("testBeeper end mRspStatus="+mRspStatus);
                            break;
                    }
                }
            };
            mRetestBtn.setVisibility(View.VISIBLE);
            mRetestBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContentShow.setText(R.string.runing_test);
                    mFail.setEnabled(false);
                    mSuccess.setEnabled(false);
                    mRetestBtn.setEnabled(false);
                    mTestCount = 2;
                    mBuzzerTestHandler.sendEmptyMessage(BUZZER_TEST_HANDLERTHREAD_MSG);
                }
            });
            mContentShow.setText(R.string.runing_test);
            mFail.setEnabled(false);
            mSuccess.setEnabled(false);
            mRetestBtn.setEnabled(false);
            mBuzzerTestHandler.sendEmptyMessage(BUZZER_TEST_HANDLERTHREAD_MSG);

        }else {
            try {
                runShellCommand(BUZZER_TEST_COMMAND);
            } catch (Exception e) {
                LogUtil.e("BUZZER_TEST_COMMAND: " + e.getMessage());
            }
            mSuccess.setVisibility(View.VISIBLE);
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
                        if(BUZZER_TEST_COMMAND.equals(p_command)){
                            mHandler.sendEmptyMessageDelayed(BUZZER_TEST_MSG,100);
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
        mHandler.postDelayed(test_runnable, 300);
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            LogUtil.i("p_msg="+p_msg);
            switch (msg.what){

                case BUZZER_TEST_MSG:
                    //nothing
                    break;
                case BUZZER_TEST_UPDATE_MSG:
                    mContentShow.setText(R.string.buzzer_test_content);
                    mSuccess.setVisibility(View.VISIBLE);
                    mFail.setEnabled(true);
                    mSuccess.setEnabled(true);
                    mRetestBtn.setEnabled(true);
                    LogUtil.i("mRspStatus="+mRspStatus);
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
        if(mBuzzerTestHandler != null){
            mBuzzerTestHandler.removeMessages(BUZZER_TEST_HANDLERTHREAD_MSG);
        }
        if(mHanlerThread != null){
            mHanlerThread.quit();
        }
        mHandler.removeMessages(BUZZER_TEST_MSG);
        mHandler.removeMessages(BUZZER_TEST_UPDATE_MSG);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }

        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            mHandler.sendEmptyMessageDelayed(BUZZER_TEST_MSG,100);
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
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


}
