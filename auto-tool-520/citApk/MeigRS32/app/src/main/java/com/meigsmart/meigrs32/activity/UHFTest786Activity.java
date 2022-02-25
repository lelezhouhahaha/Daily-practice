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
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.SerialPort;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.FileOutputStream;

import butterknife.BindView;

public class UHFTest786Activity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
    private UHFTest786Activity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.serial_test)
    public TextView mSerialTestStatus;
    @BindView(R.id.io_test)
    public TextView mIoPortTestStatus;
    @BindView(R.id.spi_test)
    public TextView mSpiTestStatus;

    private String UHF_PATH="dev/ttyHS1";
    private String UHF_SUPPLY_NODE_PATH = "common_uhf_supply_node_path";
    private String UHF_SERIALPORT_ENABLE_NODE_KEY = "common_uhf_serial_enable_node";
    private String UHF_IO_PORT1_NODE_KEY = "common_uhf_io_port1_node";
    private String UHF_IO_PORT2_NODE_KEY = "common_uhf_io_port2_node";
    private String uhf_serial_enable_node = "";
    private String uhf_io_port1_node = "";
    private String uhf_io_port2_node = "";
    private boolean uhf_io_port1_status = false;
    private boolean uhf_io_port2_status = false;
    private boolean serial_port_status = false;
    private SerialPort mSerialPort;
    private int baud = 115200;
    private String TAG =  "UHFTest786Activity";
    private String UHF_TEST_ENBLE_NODE= "/sys/meige/gpio_output/cradle_5v/value";
    private String uhf_en_node = "/sys/meige/gpio_output/uhf_en/value";

    private Thread mThread;
    String ioPortStatus;

    private String interface_visible_node = "/sys/meige/interface/visible";
    private String interface_invisible_node = "/sys/meige/interface/invisible";
    private String spi_result_node = "/sys/bus/spi/devices/spi1.0/spi_cit_test";
    private String INTERFACE_SPI_VALUE = "se";
    private String INTERFACE_IO_VALUE = "uhf";
    private String gpio_r = "/sys/meige/gpio_output/GPIO_R/value";
    private String gpio_s = "/sys/meige/gpio_output/GPIO_S/value";
    private String uhf_en = "/sys/meige/gpio_output/uhf_en/value";
    private boolean spi_status;
    
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_uhf786;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mFail.setVisibility(View.GONE);
        mTitle.setText(R.string.pcba_uhftest);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        ioPortStatus = getResources().getString(R.string.Abnormal);

        writeToFile(interface_visible_node, INTERFACE_IO_VALUE);
        //writeToFile(UHF_TEST_ENBLE_NODE, "1");
        writeToFile(uhf_en_node, "1");
        UHF_PATH = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_SUPPLY_NODE_PATH);
        if(UHF_PATH == null || UHF_PATH.isEmpty()){
            UHF_PATH="dev/ttyHS1";
        }

        uhf_io_port1_node = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_IO_PORT1_NODE_KEY);

        uhf_io_port2_node = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_IO_PORT2_NODE_KEY);

        uhf_serial_enable_node  = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, UHF_SERIALPORT_ENABLE_NODE_KEY);

        if(!uhf_serial_enable_node.isEmpty()){
            writeToFile(uhf_serial_enable_node, "1");
        }

        mSerialPort =new SerialPort();
        if(!uhf_io_port1_node.isEmpty()) {
            writeToFile(uhf_io_port1_node, "1");
        }

        if(!uhf_io_port2_node.isEmpty()){
            writeToFile(uhf_io_port2_node, "1");
        }

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mSerialPort.uhf_test(UHF_PATH,baud, "ufh test");
            }
        });
        mThread.start();

        mHandler.sendEmptyMessageDelayed(1006, 1000);
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            LogUtil.d(" uhf path:< " + path + ">.");
            LogUtil.d(" uhf value:< " + value + ">.");
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e(TAG, "write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
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
                case 1003:
                    if (getResult()) {
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        mSuccess.setVisibility(View.VISIBLE);
                        mHandler.sendEmptyMessageDelayed(1001, 3000);
                    } else {
                        mHandler.sendEmptyMessageDelayed(1002, 3000);
                        mFail.setVisibility(View.VISIBLE);
                    }
                    break;
                case 1005:

                    if(!uhf_io_port1_node.isEmpty()) {
                        writeToFile(uhf_io_port1_node, "1");
                    }
                    if(!uhf_io_port2_node.isEmpty()){
                        writeToFile(uhf_io_port2_node, "1");
                    }

                    if(mSerialPort.isStatus()){
                        serial_port_status = true;
                        LogUtil.d(" serialPort isStatus sucess");
                        String status = getResources().getString(R.string.normal);
                        mSerialTestStatus.setText(Html.fromHtml(
                                getResources().getString(R.string.uhf_serial_test) +
                                        "&nbsp;" + "<font color='#FF0000'>" + status + "</font>"
                        ));
                    }else{
                        // fail
                        LogUtil.d(" serialPort isStatus fail");
                        String status = getResources().getString(R.string.Abnormal);
                        mSerialTestStatus.setText(Html.fromHtml(
                                getResources().getString(R.string.uhf_serial_test) +
                                        "&nbsp;" + "<font color='#FF0000'>" + status + "</font>"
                        ));
                    }
                    if(uhf_io_port1_status && uhf_io_port2_status) {
                        ioPortStatus = getResources().getString(R.string.normal);
                    }
                    mIoPortTestStatus.setText(Html.fromHtml(
                            getResources().getString(R.string.uhf_io_test) +
                                    "&nbsp;" + "<font color='#FF0000'>" + ioPortStatus + "</font>"
                    ));
                    writeToFile(interface_invisible_node, INTERFACE_IO_VALUE);
                    mHandler.sendEmptyMessageDelayed(2001, 500);
                    break;
                case 1006:
                    if(!uhf_io_port1_node.isEmpty()) {
                        writeToFile(uhf_io_port1_node, "0");
                    }

                    if(!uhf_io_port2_node.isEmpty()){
                        writeToFile(uhf_io_port2_node, "0");
                    }
                    mHandler.sendEmptyMessageDelayed(1005, 500);
                    break;
                case 2001:
                    startSpiTest();
                    mHandler.sendEmptyMessageDelayed(2002, 500);
                    break;
                case 2002:
                    String value = DataUtil.readLineFromFile(spi_result_node);
                    LogUtil.d("get spi result :" + value);
                    spi_status = "1".equals(DataUtil.readLineFromFile(spi_result_node));
                    String str = spi_status ? getResources().getString(R.string.normal)
                                            : getResources().getString(R.string.Abnormal);
                        mSpiTestStatus.setText(Html.fromHtml(
                                getResources().getString(R.string.uhf_spi_test) +
                                        "&nbsp;" + "<font color='#FF0000'>" + str + "</font>"));
                    stopSpiTest();
                    mHandler.sendEmptyMessageDelayed(1003, 500);
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

        if(!uhf_serial_enable_node.isEmpty()){
            writeToFile(uhf_serial_enable_node, "0");
        }
        writeToFile(uhf_en_node, "0");
        //writeToFile(UHF_TEST_ENBLE_NODE, "0");
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(9999);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtil.d(" uhf onKeyDown keycode: <" + keyCode + ">.");
        int scanCode = event.getScanCode();
        LogUtil.d(" uhf onKeyDown scanCode: <" + scanCode + ">.");
        if ( scanCode == 250) {
            uhf_io_port1_status = true;
        }
        if ( scanCode == 754) {
            uhf_io_port2_status = true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtil.d(" uhf onKeyUp keycode: <" + keyCode + ">.");
        int scanCode = event.getScanCode();
        LogUtil.d(" uhf onKeyUp scanCode: <" + scanCode + ">.");
        if (keyCode == 250 || scanCode == 250) {
            uhf_io_port1_status = true;
        }
        if (keyCode == 754 || scanCode == 754) {
            uhf_io_port2_status = true;
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

    private boolean getResult() {
        return serial_port_status && uhf_io_port1_status && uhf_io_port2_status && spi_status;
    }

    private void startSpiTest() {
        writeToFile(interface_visible_node, INTERFACE_SPI_VALUE);
        writeToFile(gpio_r, "1");
        writeToFile(gpio_s, "1");
        writeToFile(uhf_en, "1");
    }
    private void stopSpiTest() {
        writeToFile(interface_invisible_node, INTERFACE_SPI_VALUE);
        writeToFile(gpio_r, "0");
        writeToFile(gpio_s, "0");
        writeToFile(uhf_en, "0");
    }
    

}
