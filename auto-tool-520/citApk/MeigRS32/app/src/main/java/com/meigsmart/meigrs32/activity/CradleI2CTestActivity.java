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

public class CradleI2CTestActivity extends CradleTestActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    protected CradleI2CTestActivity mContext;
    protected String TAG =  "CradleUardAdcTestActivity";
    private Runnable mRun;

    @Override
    protected void initData() {
        getDefaultConfigInfo();

        // mFail.setVisibility(View.GONE);
        mTitle.setText(R.string.CradleI2CTestActivity);
        mGpio64CradleIo3.setVisibility(View.VISIBLE);
        mGpio9CradleIo2.setVisibility(View.VISIBLE);

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
        writeToFile(fpc_green_led_node, "255");
        writeToFile(fpc_red_led_node, "255");
        mHandler.sendEmptyMessageDelayed(1007, 1000);
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
                    break;
                case 1006:
                    break;
                case 1007:
                    mGpio64CradleIo3.setText(Html.fromHtml(
                            getResources().getString(R.string.gpio64_cradle_io3_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" +
                                    getResources().getString(R.string.is_fpc_led_lights) + "</font>"));
                    mGpio9CradleIo2.setText(Html.fromHtml(
                            getResources().getString(R.string.gpio9_cradle_io2_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" +
                                    getResources().getString(R.string.is_fpc_led_lights) + "</font>"));
                    mSuccess.setVisibility(View.VISIBLE);
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
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
    public void finishAction() {
        writeToFile(fpc_green_led_node, "0");
        writeToFile(fpc_red_led_node, "0");
        writeToFile(dc_pwm_node, "0");
        super.finishAction();
    }

}
