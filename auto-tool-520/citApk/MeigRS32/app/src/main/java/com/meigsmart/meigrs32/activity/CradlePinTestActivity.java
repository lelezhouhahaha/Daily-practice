package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.SerialPort;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.FileOutputStream;

import butterknife.BindView;

public class CradlePinTestActivity extends CradleTestActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    protected CradlePinTestActivity mContext;

    private boolean dc_pwm_status = false;
    protected String TAG =  "CradlePinTestActivity";
    private String resultString;
    private Runnable mRun;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cradle;
    }

    @Override
    protected void initData() {
        getDefaultConfigInfo();
//        mFail.setVisibility(View.GONE);
        mTitle.setText(R.string.CradlePinTestActivity);

        mDCPWM.setVisibility(View.VISIBLE);
        mVBUS_5V.setVisibility(View.VISIBLE);
        mGpio74.setVisibility(View.VISIBLE);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (mConfigTime == 0 ||
                        mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)) {
                        //mHandler.sendEmptyMessage(1001);
                }

                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        writeToFile(dc_pwm_node, "1");
        mHandler.sendEmptyMessageDelayed(1007, 1000);
    }

    boolean isSuccess(){
         if(dc_pwm_status){
             return true;
         }else{
             return false;
         }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE);
                    break;
                case 1004:
                    break;
                case 1005:
                    if(dc_pwm_status){
                        resultString = getResources().getString(R.string.normal);
                    }else{
                        resultString = getResources().getString(R.string.Abnormal);
                    }
                    mDCPWM.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_DC_PWM_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));
                    mGpio74.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_GPIO74_CRADLE_IO1_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));
                    mVBUS_5V.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_VBUS_5V_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));
                    resultString = getResources().getString(R.string.normal);
                    writeToFile(dc_pwm_node, "0");
                    if(isSuccess()){
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        if(mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)){
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            mHandler.sendEmptyMessageDelayed(1001, 500);
                        }
                    }else{
                        if(mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)){
                            mHandler.sendEmptyMessageDelayed(1002, 500);
                        }
                    }
                    break;
                case 1006:
                    writeToFile(dc_pwm_node, "1");
                    mHandler.sendEmptyMessageDelayed(1005, 2000);
                    break;
                case 1007:
                    writeToFile(dc_pwm_node, "0");
                    mHandler.sendEmptyMessageDelayed(1006, 500);
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
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1004);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
        mHandler.removeMessages(1007);
        mHandler.removeMessages(9999);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtil.d(" onKeyDown keycode: <" + keyCode + ">.");
        int scanCode = event.getScanCode();
        LogUtil.d(" onKeyDown scanCode: <" + scanCode + ">.");
        if ( scanCode == 755) {
            dc_pwm_status = true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtil.d(" onKeyUp keycode: <" + keyCode + ">.");
        int scanCode = event.getScanCode();
        LogUtil.d(" onKeyUp scanCode: <" + scanCode + ">.");
        if ( scanCode == 755) {
            dc_pwm_status = true;
        }
        return super.onKeyUp(keyCode, event);
    }

}
