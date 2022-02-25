package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
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

public class CradleTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    protected CradleTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    protected String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;

    // CradleI2CTestActivity
    @BindView(R.id.GPIO9_CRADLE_IO2)
    public TextView mGpio9CradleIo2;
    @BindView(R.id.GPIO64_CRADLE_IO3)
    public TextView mGpio64CradleIo3;
    // CradleUartAdcTestActivity
    @BindView(R.id.usb_sub_uart)
    public TextView mUsbSubUart;
    @BindView(R.id.gpio0_gpio1_uart)
    public TextView mGpio0Gpio1Uart;
    @BindView(R.id.DC_ADC)
    public TextView mDCADC;
    // CradlePinTestActivity
    @BindView(R.id.DC_PWM)
    public TextView mDCPWM;
    @BindView(R.id.VBUS_5V)
    public TextView mVBUS_5V;
    @BindView(R.id.GPIO74_CRADLE_IO1)
    public TextView mGpio74;


    protected String USB_SUB_UART_NODE_KEY = "common_cradle_supply_node";
    protected String GPIO9_CRADLE_IO2_NODE_KEY = "common_gpio9_cradle_io2_node";
    protected String GPIO64_CRADLE_IO3_NODE_KEY = "common_gpio64_cradle_io3_node";
    protected String FPC_GREEN_LED_NODE_KEY = "common_cradle_fpc_green_led_node";
    protected String FPC_RED_LED_NODE_KEY = "common_cradle_fpc_red_led_node";
    protected String DC_PWM_NODE_KEY = "common_cradle_dc_pwm_node";
    protected String DC_ADC_NODE_KEY = "common_cradle_dc_adc_node";
    protected String GPIO0_GPIO1_UART_NODE_KEY = "common_cradle_gpio0_gpio1_uart_node";
    protected String CRADLE_5V_NODE_KEY = "common_cradle_5v_node";
    protected String CRADLE_5V_A_NODE_KEY = "common_cradle_5v_a_node";
    protected String UHF_IO_PORT1_NODE_KEY = "common_uhf_io_port1_node";
    protected String UHF_IO_PORT2_NODE_KEY = "common_uhf_io_port2_node";

    protected String usb_sub_uart_node = "/dev/ttyHS1";
    protected String gpio0_gpio1_uart_node = "/dev/ttyMSM1";
    protected String gpio9_cradle_io2_node = "/sys/class/gpio/gpio9/value";
    protected String gpio64_cradle_io3_node = "/sys/class/gpio/gpio64/value";
    protected String dc_pwm_node = "/sys/meige/gpio_output/dc_pwm/value";
    protected String cradle_5v_node = "/sys/meige/gpio_output/cradle_5v/value";
    protected String cradle_5v_a_node = "/sys/meige/gpio_output/cradle_5v_a/value";
    protected String fpc_green_led_node = "sys/class/leds/green_1/brightness";
    protected String fpc_red_led_node = "sys/class/leds/red_1/brightness";
    protected String dc_adc_node = "/sys/meige/adc/dc_adc/value";
    protected String uhf_io_port1_node = "";
    protected String uhf_io_port2_node = "";

    protected String INTERFACE_CRADLE_UART = "cradle_uart";
    protected String interface_visible_node = "/sys/meige/interface/visible";
    protected String interface_invisible_node = "/sys/meige/interface/invisible";

    protected String uhf_en_node = "/sys/meige/gpio_output/uhf_en/value";

    protected String gpio9_cradle_io2_value = "";
    protected String gpio64_cradle_io3_value = "";

    private boolean gpio9_cradle_io2_status = false;
    private boolean gpio64_cradle_io3_status = false;
    private boolean dc_pwm_status = false;
    private SerialPort mSerialPort1, mSerialPort2;
    private int baud = 115200;
    protected String TAG =  "CradleTestActivity";
    private String resultString;
    protected int mConfigTime = 0;
    private Runnable mRun;
    private Thread mThread;
    private final int UART_TIMEOUT = 10000;


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cradle;
    }

    protected void getDefaultConfigInfo() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        String customGpio0Gpio1UartNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO0_GPIO1_UART_NODE_KEY);
        if(customGpio0Gpio1UartNode != null && !customGpio0Gpio1UartNode.isEmpty()){
            gpio0_gpio1_uart_node = customGpio0Gpio1UartNode;
        }

        String customUsbSubUartNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, USB_SUB_UART_NODE_KEY);
        if(customUsbSubUartNode != null && !customUsbSubUartNode.isEmpty()){
            usb_sub_uart_node = customUsbSubUartNode;
        }

        String customGpio9CradleIo2Node = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO9_CRADLE_IO2_NODE_KEY);
        if(customGpio9CradleIo2Node != null && !customGpio9CradleIo2Node.isEmpty()){
            gpio9_cradle_io2_node = customGpio9CradleIo2Node;
        }

        String customGpio64CradleIo3Node = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO64_CRADLE_IO3_NODE_KEY);
        if(customGpio64CradleIo3Node != null && !customGpio64CradleIo3Node.isEmpty()){
            gpio64_cradle_io3_node = customGpio64CradleIo3Node;
        }

        String customDcPWMNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, DC_PWM_NODE_KEY);
        if(customDcPWMNode != null && !customDcPWMNode.isEmpty()){
            dc_pwm_node = customDcPWMNode;
        }

        String customDcAdcNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, DC_ADC_NODE_KEY);
        if(customDcAdcNode != null && !customDcAdcNode.isEmpty()){
            dc_adc_node = customDcAdcNode;
        }

        String customCradle5vNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CRADLE_5V_NODE_KEY);
        if(customCradle5vNode != null && !customCradle5vNode.isEmpty()){
            cradle_5v_node = customCradle5vNode;
        }

        String customCradle5vANode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CRADLE_5V_A_NODE_KEY);
        if(customCradle5vANode != null && !customCradle5vANode.isEmpty()){
            cradle_5v_a_node = customCradle5vANode;
        }

        String customFcpGreenLedNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, FPC_GREEN_LED_NODE_KEY);
        if(customFcpGreenLedNode != null && !customFcpGreenLedNode.isEmpty()){
            fpc_green_led_node = customFcpGreenLedNode;
        }

        String customFcpRedLedNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, FPC_RED_LED_NODE_KEY);
        if(customFcpRedLedNode != null && !customFcpRedLedNode.isEmpty()){
            fpc_red_led_node = customFcpRedLedNode;
        }

        uhf_io_port1_node = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_IO_PORT1_NODE_KEY);
        uhf_io_port2_node = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_IO_PORT2_NODE_KEY);
        startAction();
    }

    @Override
    protected void initData() {
        getDefaultConfigInfo();
        mFail.setVisibility(View.GONE);
        mTitle.setText(R.string.CradleTestActivity);

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
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                        mHandler.sendEmptyMessage(1001);
                }

                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        writeToFile(dc_pwm_node, "1");
        mHandler.sendEmptyMessageDelayed(1007, 1000);
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            LogUtil.d(" write path:< " + path + ">.");
            LogUtil.d(" write value:< " + value + ">.");
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e(TAG, "write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean isSuccess(){
         if(dc_pwm_status
                 && gpio9_cradle_io2_status
                 && gpio64_cradle_io3_status
                 && mSerialPort1.isStatus()
                 && mSerialPort2.isStatus()){
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
                    if(mSerialPort1.isStatus()){
                        resultString = getResources().getString(R.string.normal);
                    }else{
                        resultString = getResources().getString(R.string.Abnormal);

                    }
                    mUsbSubUart.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_usb_sub_uart_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));

                    if(mSerialPort2.isStatus()){
                        resultString = getResources().getString(R.string.normal);
                    }else{
                        resultString = getResources().getString(R.string.Abnormal);

                    }
                    mGpio0Gpio1Uart.setText(Html.fromHtml(
                            getResources().getString(R.string.cradle_gpio0_gpio1_uart_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));

                    if(isSuccess()){
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        mHandler.sendEmptyMessageDelayed(1001, 1000);
                    }else{
                        mHandler.sendEmptyMessageDelayed(1002, 1000);
                    }

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

                    resultString = getResources().getString(R.string.normal);
                    if(mSerialPort1.isStatus()){
                        mUsbSubUart.setText(Html.fromHtml(
                                getResources().getString(R.string.cradle_usb_sub_uart_test) +
                                        "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));
                    }
                    if(mSerialPort2.isStatus()){
                        mGpio0Gpio1Uart.setText(Html.fromHtml(
                                getResources().getString(R.string.cradle_gpio0_gpio1_uart_test) +
                                        "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));
                    }

                    if(isSuccess()){
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        mHandler.sendEmptyMessageDelayed(1001, 1000);
                    }else{
                        mHandler.sendEmptyMessageDelayed(1004, UART_TIMEOUT);
                    }
                    break;
                case 1006:
                    gpio64_cradle_io3_value = DataUtil.readLineFromFile(gpio64_cradle_io3_node);
                    if(!gpio64_cradle_io3_value.isEmpty() && gpio64_cradle_io3_value.equals("1")){
                        gpio64_cradle_io3_status = true;
                        resultString = getResources().getString(R.string.normal);
                    }else{
                        resultString = getResources().getString(R.string.Abnormal);
                    }
                    mGpio64CradleIo3.setText(Html.fromHtml(
                            getResources().getString(R.string.gpio64_cradle_io3_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));

                    gpio9_cradle_io2_value = DataUtil.readLineFromFile(gpio9_cradle_io2_node);
                    if(!gpio9_cradle_io2_value.isEmpty() && gpio9_cradle_io2_value.equals("1")){
                        gpio9_cradle_io2_status = true;
                        resultString = getResources().getString(R.string.normal);
                    }else{
                        resultString = getResources().getString(R.string.Abnormal);
                    }
                    mGpio9CradleIo2.setText(Html.fromHtml(
                            getResources().getString(R.string.gpio9_cradle_io2_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + resultString + "</font>"));
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
        finishAction();
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

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    public void startAction() {
        writeToFile(cradle_5v_a_node, "1");
        //writeToFile(cradle_5v_node, "1");
        writeToFile(uhf_io_port1_node, "1");
        writeToFile(uhf_io_port2_node, "1");
    }

    public void finishAction() {
        writeToFile(uhf_io_port1_node, "0");
        writeToFile(uhf_io_port2_node, "0");
       // writeToFile(cradle_5v_node, "0");
        writeToFile(cradle_5v_a_node, "0");
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

    static {
        System.loadLibrary("meigpsam-jni");
    }


}
