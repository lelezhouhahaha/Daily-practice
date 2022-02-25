package com.meigsmart.meigrs32.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class EightPinLinkerTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    private EightPinLinkerTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    private TextView mRj11Msg;
    private final int REQUEST_RJ11 = 0;
    private boolean hasShowButton = false;
    private final String KEY_PATH = "path";
    private final String PATH = "/dev/ttyHSL1";
    private final String KEY_BAUDRATE = "baudrate";
    private final int BAUDRATE = 115200;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_eightpin_linker_test;


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
        mTitle.setText(R.string.run_in_eightpinlinkertest);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        Intent intent=new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        ComponentName cn=new ComponentName("com.example.serialutil",
                "com.example.serialutil.SerialActivity");
        intent.setComponent(cn);
        intent.putExtra(KEY_PATH, PATH);
        intent.putExtra(KEY_BAUDRATE, BAUDRATE);
        startActivityForResult(intent, REQUEST_RJ11);
    }

    @Override
    protected void onDestroy() {

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_RJ11 == requestCode && resultCode == 0) {
            mSuccess.setVisibility(View.VISIBLE);
            if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                deInit(mFatherName, SUCCESS);//auto pass pcba
            }
        } else {
            if(requestCode == 11 && data != null)
                deInit(mFatherName, data.getIntExtra("result", FAILURE));

        }

    }
}