package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import butterknife.BindView;

public class RJ12TestActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack{

    private RJ12TestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";

    private RJ12Thread  mThread;
    private TextView mTextstatus;
    private Button mButtonOpen;

    private boolean showButton = false;
    private boolean hasClickOpen = false;
    private boolean mIsOpen = false;

    TextView mTextviewOutput;

    public final String TAG_RJ12_PATH_NODE = "common_cit_rj12_node";
    public final String TAG_RJ12_STATE_NODE = "common_cit_rj12_state";
    public String RJ12_PATH = "/sys/cash_drawer/kickstate";
    public String TRJ12_STATE = "/sys/cash_drawer/kickout";


    @Override
    protected int getLayoutId() {
        return R.layout.activity_rj12_test;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        //mFail.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.run_in_rj12test);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);


        mTextstatus = (TextView)findViewById(R.id.RJ12_status);

        mTextviewOutput = (TextView)findViewById(R.id.RJ12_status_output);
        mTextviewOutput.setText("");

        String rj12_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_RJ12_PATH_NODE);
        if(rj12_path != null && !rj12_path.isEmpty()){
            RJ12_PATH = rj12_path;
        }
        String rj12_state = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_RJ12_STATE_NODE);
        if(rj12_state != null && !rj12_state.isEmpty()){
            TRJ12_STATE = rj12_state;
        }


        mButtonOpen = (Button)findViewById(R.id.btn_open);
        mButtonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!mIsOpen)hasClickOpen = true;
                byte[] pullUpData = {'1'};
                byte[] pullDownData = {'0'};

                boolean ret1 = writeToFile(pullUpData);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                boolean ret2 = writeToFile(pullDownData);
            }
        });
        mThread = new RJ12Thread();
        mThread.start();
    }


    public boolean writeToFile(byte[] date){
        try {
            String str = new String(date);
            FileOutputStream fileOutputStream = new FileOutputStream(RJ12_PATH);
            fileOutputStream.write(date);
            fileOutputStream.close();
            return true;
        } catch (FileNotFoundException i) {
            i.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int readState()
    {
        BufferedReader reader = null;
        String state = "";
        int boxState = 0;
        try {
            reader = new BufferedReader(new FileReader(TRJ12_STATE));
            state = reader.readLine();
            boxState = Integer.parseInt(state);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return boxState;
        }
    }
    private class RJ12Thread extends Thread {

        private boolean stop = false;

        @Override
        public void run() {
            super.run();
            while (!stop) {
                try {
                    mHandler.sendEmptyMessage(222222);
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public void close() {
            stop = true;
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            LogUtil.d("qqqqqqqqq" );
            switch (msg.what) {
                case 222222: {
                    if( readState() == 0 ) {
                        mIsOpen = true;
                        mTextviewOutput.setText(R.string.RJ12_status_open);
                        if (hasClickOpen && !showButton) {
                            LogUtil.d("qianxiang:" + mIsOpen);
                            mSuccess.setVisibility(View.VISIBLE);
                            if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                                deInit(mFatherName, SUCCESS);//auto pass pcba
                            }
                        }


                    }else if(readState() == 1) {
                        mIsOpen = false;
                        mTextviewOutput.setText(R.string.RJ12_status_close);
                        LogUtil.d("qrrrrr"+mIsOpen );
                    }else {
                        mIsOpen = false;
                        mTextviewOutput.setText("error");
                        LogUtil.d("zzzzzzzzz"+mIsOpen );
                    }

                    break;
                }
                default:
                    break;
            }
        }
    };



    @Override
    protected void onDestroy() {

        mThread.close();
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

}
