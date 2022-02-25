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

public class CradleUartAdcTestActivity extends CradleTestActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    protected CradleUartAdcTestActivity mContext;

    private boolean dc_adc_status = false;
    private boolean uart1_status = false;
    private boolean uart2_status = false;
    private SerialPort mSerialPort1, mSerialPort2;
    private int baud = 115200;
    protected String TAG =  "CradleUardAdcTestActivity";
    private String resultString;
    private Runnable mRun;
    private Thread mThread;
    private final int UART_TIMEOUT = 10000;
    private int mUartTime = 0;

    @Override
    protected void initData() {
        getDefaultConfigInfo();
        // mFail.setVisibility(View.GONE);
        mTitle.setText(R.string.CradleUartAdcTestActivity);
        
        mDCADC.setVisibility(View.VISIBLE);
        mUsbSubUart.setVisibility(View.VISIBLE);
        mGpio0Gpio1Uart.setVisibility(View.VISIBLE);

        mSerialPort1 =new SerialPort();
        mSerialPort2 =new SerialPort();

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mSerialPort1.uhf_test(usb_sub_uart_node, baud, "uart test");
                mSerialPort2.uhf_test(gpio0_gpio1_uart_node, baud, "uart test");
            }
        });
        mThread.start();

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

        mHandler.sendEmptyMessageDelayed(1007, 1000);
    }

    boolean isSuccess(){
         return dc_adc_status && uart1_status && uart2_status;
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
                    mUartTime += 1000;

                    if(mSerialPort1.isStatus()){
                        uart1_status = true;
                        resultString = getResources().getString(R.string.normal);
                    } else if (mUartTime > UART_TIMEOUT) {
                        resultString = getResources().getString(R.string.Abnormal);
                    } else {
                        resultString = getResources().getString(R.string.please_wait);
                    }
                    mUsbSubUart.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_usb_sub_uart_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));

                    if(mSerialPort2.isStatus()){
                        uart2_status = true;
                        resultString = getResources().getString(R.string.normal);
                    } else if (mUartTime > UART_TIMEOUT) {
                        resultString = getResources().getString(R.string.Abnormal);
                    } else {
                        resultString = getResources().getString(R.string.please_wait);
                    }
                    mGpio0Gpio1Uart.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_gpio0_gpio1_uart_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));
                    if (isSuccess()) {
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                            mHandler.sendEmptyMessageDelayed(1001, 1000);
                        }
                    } else if (mUartTime <= UART_TIMEOUT) {
                        mHandler.sendEmptyMessageDelayed(1005, 1000);
                    } else {
                        if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                            mHandler.sendEmptyMessageDelayed(1002, 1000);
                        }
                    }
                    break;
                case 1006:
                    break;
                case 1007:
                    String value = DataUtil.readLineFromFile(dc_adc_node);
                    int val = 0;
                    if (!TextUtils.isEmpty(value))
                        val = Integer.parseInt(value.trim());
                    if (val >= 1200000 && val <= 1600000) {
                        dc_adc_status = true;
                        resultString = getResources().getString(R.string.normal);
                    } else {
                        dc_adc_status = false;
                        resultString = getResources().getString(R.string.Abnormal);
                    }
                    resultString = resultString + " " + value;
                    mDCADC.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_dc_adc_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));
                    mHandler.sendEmptyMessageDelayed(1005, 500);
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
    public void startAction() {
        super.startAction();
        writeToFile(interface_visible_node, INTERFACE_CRADLE_UART);
        writeToFile(uhf_en_node, "0");
//        writeToFile(dc_pwm_node, "1");
    }
    @Override
    public void finishAction() {
//        writeToFile(dc_pwm_node, "0");
        writeToFile(interface_invisible_node, INTERFACE_CRADLE_UART);
        super.finishAction();
    }

}
