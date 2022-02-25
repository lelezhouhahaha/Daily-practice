package com.meigsmart.meigrs32.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;


public class TpCapacity_New_Activity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private TpCapacity_New_Activity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.tp_result)
    public TextView mResult;
    private String mFatherName = "";


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private String TP_RESULT_INTENT="com.chsc.semitouchtester.testresult";
    private MyBroadcastReceiver myBroadcastReceiver;

    @Override
    protected int getLayoutId() {
        return R.layout.tp_capacity_new;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mSuccess.setVisibility(View.GONE);
        mTitle.setText(R.string.TpCapacity_New_Activity);
        myBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(myBroadcastReceiver, new IntentFilter(TP_RESULT_INTENT));

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        Intent tp_test =new Intent();
        tp_test.setClassName("com.chsc.semitouchtester", "com.chsc.semitouchtester.activity.MainActivity");
        tp_test.putExtra("autofinish",true);
        startActivityForResult(tp_test,101);

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mResult.setText("TP容值测试成功");
                    mResult.setTextColor(Color.GREEN);
                    mSuccess.setVisibility(View.VISIBLE);
                    break;

                case 1002:
                    mResult.setText("TP容值测试失败");
                    mResult.setTextColor(Color.RED);
                    break;
            }
        }
    };

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            int finalResult= intent.getIntExtra("testResult",-1);
            if(finalResult==0){
                mHandler.sendEmptyMessage(1001);
            }else{
                mHandler.sendEmptyMessage(1002);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mHandler != null) {
            mHandler.removeMessages(1001);
        }
        if (myBroadcastReceiver != null) {
            unregisterReceiver(myBroadcastReceiver);
        }
    }

    @Override
    public void onClick(View v) {

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
}
