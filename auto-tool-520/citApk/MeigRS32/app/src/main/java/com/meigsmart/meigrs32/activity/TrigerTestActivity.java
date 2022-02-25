package com.meigsmart.meigrs32.activity;

import android.graphics.Color;
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


public class TrigerTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private TrigerTestActivity mContext;
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

    @BindView(R.id.test_color)
    public TextView mTestColor;

    public static final int TRIGER_TEST_MSG = 1001;
    public static final int TEST_SUCCESS = 1002;
    public static final int TEST_FAIL = 1003;
    private boolean isMC518 = "MC518".equals(DataUtil.getDeviceName());
    private CitTestJni mCitTestJni = new CitTestJni();
    private int mRspStatus = -1;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_trigertest;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.TrigerTestActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);


        if(isMC518){
            mHandler.sendEmptyMessageDelayed(TRIGER_TEST_MSG,300);

        }
       //mSuccess.setVisibility(View.VISIBLE);

    }


    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case TRIGER_TEST_MSG:
                    mRspStatus = mCitTestJni.testTriger();

                    LogUtil.i("getTrigerRspText = "+mCitTestJni.getTrigerRspText());
                    String text = "";
                    if(mRspStatus == 0){
                        text = getString(R.string.TrigerTestActivity) + ": "+getString(R.string.success);
                        mContentShow.setText(text);
                        mTestColor.setBackgroundColor(Color.GREEN);
                        mTestColor.setText("0X00000000");
                        mSuccess.setVisibility(View.VISIBLE);
                        mHandler.sendEmptyMessageDelayed(TEST_SUCCESS,300);

                    }else if(mRspStatus == 0x00004804){
                        text = getString(R.string.TrigerTestActivity) + ": "+getString(R.string.success);
                        mContentShow.setText(text);
                        mTestColor.setBackgroundColor(Color.YELLOW);
                        mTestColor.setText("0X"+mCitTestJni.getTrigerRspText());
                        mSuccess.setVisibility(View.VISIBLE);
                        mHandler.sendEmptyMessageDelayed(TEST_SUCCESS,300);
                    }else{
                        text = getString(R.string.TrigerTestActivity) + ": "+getString(R.string.fail);
                        mContentShow.setText(text);
                        mTestColor.setBackgroundColor(Color.RED);
                        mTestColor.setText("0XFF000000");
                        mHandler.sendEmptyMessageDelayed(TEST_FAIL,300);
                    }
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
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(TRIGER_TEST_MSG);
        mHandler.removeMessages(9999);
        mHandler.removeMessages(TEST_SUCCESS);
        mHandler.removeMessages(TEST_FAIL);
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
