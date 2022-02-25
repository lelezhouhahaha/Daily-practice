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
import android.widget.EditText;
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
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.util.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import butterknife.BindView;


public class TpCapacityActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private TpCapacityActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.threshold)
    public EditText mThreshold;
    @BindView(R.id.max_threshold)
    public EditText mMaxThreshold;
    @BindView(R.id.min_threshold)
    public EditText mMinThreshold;
    @BindView(R.id.tp_test_btn)
    public Button mTest;
    @BindView(R.id.tp_result)
    public TextView mResult;
    private String mFatherName = "";
    private int mConfigtime = 30;//test time
    private Runnable mRun;
    private TpTask tpTask = null;


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    public String TP_CAPACITY_PATH = "/proc/ilitek/sensor_test_data";
    public static final String COMMON_TP_CAPACITY_PATH = "common_tp_capacity_path";

    private String open_threshold, allnode_max_threshold,allnode_min_threshold,value,result;
    private boolean mTestEnding = false;



    @Override
    protected int getLayoutId() {
        return R.layout.activity_tp_capacity;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.TpCapacityActivity);
        String TP_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, COMMON_TP_CAPACITY_PATH);
        if((null != TP_path) && !TP_path.isEmpty()){
            TP_CAPACITY_PATH = TP_path;
        }

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mTest.setOnClickListener(this);
    }


    class TpTask extends AsyncTask {
        int type;
        public TpTask(int type){
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(type == 2){
                mResult.setText("测试中...");
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            if(type == 1){
                FileUtil.writeToFile(TP_CAPACITY_PATH,value);
            }else if(type == 2){
                result = FileUtil.readFile(TP_CAPACITY_PATH);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if(type == 1) {
                new TpTask(2).execute();
            }else if(type == 2) {
                mHandler.sendEmptyMessage(1001);
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mResult.setText(result);
                    if(null!=result && result.contains("pass")){
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                    break;

                case 1111:
                    deInit(mFatherName, FAILURE);
                    break;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mHandler != null) {
            mHandler.removeMessages(1001);
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
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
        if(v == mTest){
            open_threshold = mThreshold.getText().toString();
            allnode_max_threshold = mMaxThreshold.getText().toString();
            allnode_min_threshold = mMinThreshold.getText().toString();
            open_threshold = (open_threshold.equals("") || open_threshold.isEmpty())?"400":open_threshold;
            allnode_max_threshold = (allnode_max_threshold.equals("") || allnode_max_threshold.isEmpty())?"4400":allnode_max_threshold;
            allnode_min_threshold = (allnode_min_threshold.equals("") || allnode_min_threshold.isEmpty())?"3900":allnode_min_threshold;
            value = "7,"+open_threshold+",120,120,"+allnode_max_threshold+","+allnode_min_threshold+",140,140,20,3,1,/data/local/tmp";

            tpTask = new TpTask(1);
            tpTask.execute();

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
}
