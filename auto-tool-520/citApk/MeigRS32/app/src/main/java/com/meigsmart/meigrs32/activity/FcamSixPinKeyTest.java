package com.meigsmart.meigrs32.activity;
/*write for MC510 test*/

import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class FcamSixPinKeyTest extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private FcamSixPinKeyTest mContext;
    @BindView(com.meigsmart.meigrs32.R.id.title)
    public TextView mTitle;
    @BindView(com.meigsmart.meigrs32.R.id.back)
    public LinearLayout mBack;
    @BindView(com.meigsmart.meigrs32.R.id.sixpin_key1)
    public Button key1;
    @BindView(com.meigsmart.meigrs32.R.id.sixpin_key2)
    public Button key2;
    @BindView(com.meigsmart.meigrs32.R.id.sixpin_key3)
    public Button key3;
    private String mFatherName = "";
    private boolean flag_six_key1 = false;
    private boolean flag_six_key2 = false;
    private boolean flag_six_key3 = false;


    @BindView(com.meigsmart.meigrs32.R.id.success)
    public Button mSuccess;
    @BindView(com.meigsmart.meigrs32.R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return com.meigsmart.meigrs32.R.layout.activity_fcspkey;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(com.meigsmart.meigrs32.R.string.FcamSixPinKeyTest);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);


    }

    private void refreshresult(){
        if(flag_six_key1 && flag_six_key2 &&flag_six_key3 ){
            Log.d("sixpinkey","allpass");
            deInit(mFatherName, SUCCESS);
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try{
            int mscancode = event.getScanCode();
            if(mscancode == 523){
                flag_six_key1 = true;
                key1.setBackgroundResource(com.meigsmart.meigrs32.R.drawable.keytest_bg_pressed);
            }else if(mscancode == 190){
                flag_six_key2 = true;
                key2.setBackgroundResource(com.meigsmart.meigrs32.R.drawable.keytest_bg_pressed);
            }else if(mscancode == 191){
                flag_six_key3 = true;
                key3.setBackgroundResource(com.meigsmart.meigrs32.R.drawable.keytest_bg_pressed);
            }
            Log.d("sixpinkey","mscancode:"+ mscancode);
            refreshresult();
        }catch (Exception e){
            Log.e("sixpinkey","fail:"+e);
        }
        return true;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 11 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }

}
