package com.meigsmart.meigrs32.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
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


public class PSAMTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private PSAMTestActivity mContext;
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

    public static final int PSAM1_TEST_MSG = 1001;
    public static final int PSAM2_TEST_MSG = 1002;
    public static final int PSAM_TEST_UPDATE_MSG = 1003;
    public static final int TEST_SUCCESS = 2001;
    public static final int TEST_FAILED = 2002;
    public static final String PSAM1_TEST_COMMAND = "castles icc_test sam1";
    public static final String PSAM2_TEST_COMMAND = "castles icc_test sam2";
    public static final String PSAM_SLOT_ID_KEY = "common_psam_slot_ids";

    private String p_msg,p_command;
    private boolean mPSAM1Result = false;
    private boolean mPSAM2Result = false;
    private String mPSAM1ResultString = "";
    private String mPSAM2ResultString = "";

    private boolean isMC518 = "MC518".equals(DataUtil.getDeviceName());
    private CitTestJni mCitTestJni = null;
    private int mRspStatus = -1;
    public static final int PSAM_TEST_HANDLERTHREAD_MSG = 1;
    private HandlerThread mHanlerThread = null;
    private Handler mPsamTestHandler = null;
    private int TEST_TIMES = 5;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_psamtest;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.PSAMTestActivity);//待修改

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        if(isMC518) {
            mCitTestJni = new CitTestJni();
            mHanlerThread = new HandlerThread("PsamHandlerThread");
            mHanlerThread.start();
            mPsamTestHandler = new Handler(mHanlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case PSAM_TEST_HANDLERTHREAD_MSG:
                            LogUtil.i("testPsam start TEST_TIMES=" + TEST_TIMES);
                            String tmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, PSAM_SLOT_ID_KEY);
                            int slotId = 2;
                            if (!TextUtils.isEmpty(tmp)) {
                                slotId = Integer.valueOf(tmp);
                            }
                            LogUtil.i("testPsam slotId=" + slotId);
                            mRspStatus = mCitTestJni.testPsam(slotId);
                            TEST_TIMES--;

                            if (mRspStatus == 0) {
                                mHandler.removeMessages(PSAM_TEST_UPDATE_MSG);
                                mHandler.sendEmptyMessageDelayed(PSAM_TEST_UPDATE_MSG, 100);
                            } else {
                                if (TEST_TIMES > 0) {
                                    mPsamTestHandler.removeMessages(PSAM_TEST_HANDLERTHREAD_MSG);
                                    mPsamTestHandler.sendEmptyMessageDelayed(PSAM_TEST_HANDLERTHREAD_MSG, 500);
                                } else {
                                    mHandler.removeMessages(PSAM_TEST_UPDATE_MSG);
                                    mHandler.sendEmptyMessageDelayed(PSAM_TEST_UPDATE_MSG, 100);
                                }
                            }
                            LogUtil.i("testPsam end mRspStatus=" + mRspStatus);
                            break;
                    }

                }
            };
            mContentShow.setText(R.string.runing_test);
            mFail.setEnabled(false);
            mSuccess.setEnabled(false);
            mPsamTestHandler.sendEmptyMessage(PSAM_TEST_HANDLERTHREAD_MSG);
        }else {
            mFail.setVisibility(View.GONE);
            try {
                runShellCommand(PSAM1_TEST_COMMAND);
            } catch (Exception e) {
                LogUtil.e("PSAM1_TEST_COMMAND: " + e.getMessage());
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
                        if(PSAM1_TEST_COMMAND.equals(p_command)){
                            mContentShow.setText(p_msg);
                            mPSAM1ResultString = p_msg;
                            mHandler.sendEmptyMessageDelayed(PSAM1_TEST_MSG,100);
                        }else if(PSAM2_TEST_COMMAND.equals(p_command)){
                            mPSAM2ResultString = p_msg;
                            mContentShow.setText(mPSAM1ResultString+mPSAM2ResultString);
                            mHandler.sendEmptyMessageDelayed(PSAM2_TEST_MSG,100);
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

                case PSAM1_TEST_MSG:
                    if(p_msg.contains("ok")){
                        mPSAM1Result = true;
                    }

                    try {
                        runShellCommand(PSAM2_TEST_COMMAND);
                    }catch (Exception e) {
                        LogUtil.e("PSAM2_TEST_COMMAND: " +e.getMessage());
                    }
                    break;

                case PSAM2_TEST_MSG:
                    if(p_msg.contains("ok")){
                        mPSAM2Result = true;
                    }
                    if(mPSAM1Result && mPSAM2Result) {
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        deInit(mFatherName, SUCCESS);
                    }else{
                        mFail.setVisibility(View.VISIBLE);
                        mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                        deInit(mFatherName, FAILURE);
                    }

                    break;
                case PSAM_TEST_UPDATE_MSG:
                    String text = "";
                    mFail.setEnabled(true);
                    mSuccess.setEnabled(true);
                    if(mRspStatus == 0){
                        text = getString(R.string.PSAMTestActivity) + ": "+getString(R.string.success)+"\n"+mCitTestJni.getPsamRspText();
                        mContentShow.setText(text);
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        mHandler.sendEmptyMessageDelayed(TEST_SUCCESS, 1000);
                    }else{
                        text = getString(R.string.PSAMTestActivity) + ": "+getString(R.string.fail);
                        mContentShow.setText(text);
                        mFail.setVisibility(View.VISIBLE);
                        mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                        mHandler.sendEmptyMessageDelayed(TEST_FAILED, 1000);
                    }

                    LogUtil.i("mRspStatus="+mRspStatus+" getPsamRspText="+mCitTestJni.getPsamRspText());
                    break;
                case TEST_SUCCESS:
                    deInit(mFatherName, SUCCESS);
                    break;
                case TEST_FAILED:
                    deInit(mFatherName, FAILURE);
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
        mHandler.removeMessages(PSAM1_TEST_MSG);
        mHandler.removeMessages(PSAM2_TEST_MSG);
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
