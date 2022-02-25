package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.SerialPort;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import butterknife.BindView;

public class PSAMActivity2 extends BaseActivity implements View.OnClickListener{
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    @BindView(R.id.psamChange)
    public  TextView mPsamChange;
    private PSAMActivity2 mContext;
    private String mFatherName = "";
    private int mConfigTime = 0;
    private Runnable mRun;
    private SerialPort mSerialPort;
    private String device_name = "";
    private int baud = 0;
    private int[] input_value ;
    private int[] output_value ;
    private int check;
    private String config_file = "/system/etc/meig/cit_psam.xml";
    private Thread mThread;
    @BindView(R.id.psamTime)
    public TextView mValues;
    private String mPCBAStatus = "";
    //private static final String mPsamPath = "/sys/class/leds/power-SAM/brightness";
    private String mPowerSupplyPath = "";
    @Override
    protected int getLayoutId() {
        return R.layout.activity_psam;
    }

    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.pcba_psam_2);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mFail.setVisibility(View.INVISIBLE);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mValues.setText(getString(R.string.psam_test));
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            //mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
            mConfigTime =5;
        }

        xmlParse();
        LogUtil.d("NAME mPowerSupplyPath.length():" + mPowerSupplyPath.length());
        //if(mPowerSupplyPath.length() == 0) {
            mPowerSupplyPath = "/sys/class/leds/power-SAM2/brightness";
        //}
        LogUtil.d("NAME mPowerSupplyPath:" + mPowerSupplyPath);
        writeToFile(mPowerSupplyPath, "1");
        //registerReceiver(mBroadcastReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        LogUtil.d("set to 1: " + mPowerSupplyPath);

        mSerialPort = new SerialPort();


        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (mConfigTime == 0) {
                    if(mFatherName.equals(MyApplication.PCBASignalNAME)||mFatherName.equals(MyApplication.PreSignalNAME)){
                        if (mSerialPort.isStatus()) {
                            mSuccess.setVisibility(View.VISIBLE);
                        } else {
                            mValues.setText(getString(R.string.psam2_test_next));
                            mFail.setVisibility(View.VISIBLE);
                        }
                    }else {
                        mHandler.sendEmptyMessage(1002);
                    }
                }
                LogUtil.d("check mSerialPort status. " + mConfigTime);
                if (mSerialPort.isStatus())deInit(mFatherName, SUCCESS);
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        mHandler.sendEmptyMessageDelayed(1001,1000);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    String unchage="0";
                    /*if (mPCBAStatus.equals(unchage)){
                        mPsamChange.setText(getResources().getString(R.string.PSAM_now)+getResources().getString(R.string.Non_PSAM));
                    }else {
                        mPsamChange.setText(getResources().getString(R.string.PSAM_now) +getResources().getString(R.string.PSAM_change));
                    }*/

                    LogUtil.d("start to test ");
                    mThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mSerialPort.test(device_name, baud, input_value,output_value);
                        }
                    });
                    mThread.start();
                    break;
                case 1002:
                    if (mSerialPort.isStatus()) {
                        deInit(mFatherName, SUCCESS);
                    } else {
                        deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };
    public static String ByteToString(byte[] bytes)
    {

        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i <bytes.length ; i++) {
            if (bytes[i]!=0){
                strBuilder.append((char)bytes[i]);
            }else {
                break;
            }

        }
        return strBuilder.toString();
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e("write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*private String getBatteryVoltage() {
        FileInputStream file = null;
        String PSAMValue = "";
        try {
            file = new FileInputStream(mPsamPath);
            byte[] buffer = new byte[file.available()];
            file.read(buffer);

            file.close();
            PSAMValue = new String(ByteToString(buffer));
            if (file != null) {
                file.close();
                file = null;
            }
        } catch (Exception e) {
            try {
                if (file != null) {
                    file.close();
                    file = null;
                }
            } catch (IOException io) {
                LogUtil.e("getBatteryElectronic fail");
            }
        }
        return PSAMValue;
    }
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        String action;
        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            mPCBAStatus=getBatteryVoltage().trim();
            Message msg = mHandler.obtainMessage();
            msg.what = 1001;
            msg.obj = mPCBAStatus;
            mHandler.sendMessage(msg);
        }
    };*/
    private void xmlParse(){
        File configFile = new File(config_file);
        if(!configFile.exists()){
            LogUtil.d("psam test file not exists");
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return;
        }

        try {
            InputStream inputStream = new FileInputStream(config_file);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "utf-8");
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                LogUtil.d("parser xml");
                String nodeName = xmlPullParser.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        if("dev".equals(xmlPullParser.getName())){
                            device_name ="/dev/ttyHSL2";//xmlPullParser.nextText();
                            LogUtil.d("psam test device name = " + device_name);
                        }else if("baud".equals(xmlPullParser.getName())){
                            baud = Integer.parseInt(xmlPullParser.nextText());
                            LogUtil.d("psam test baud = " + baud);
                        }else if("input".equals(xmlPullParser.getName())){
                            String value = xmlPullParser.nextText();
                            String[] tmp = value.split(",");
                            input_value = new int[tmp.length];
                            for(int i = 0; i < tmp.length; i++){
                                input_value[i] = Integer.parseInt(tmp[i]);
                                LogUtil.d("psam test input_value i = " + i + " value = " + input_value[i]);
                            }
                        }else if("output".equals(xmlPullParser.getName())){
                            String value = xmlPullParser.nextText();
                            String[] tmp = value.split(",");
                            output_value = new int[tmp.length];
                            for(int i = 0; i < tmp.length; i++){
                                output_value[i] = Integer.parseInt(tmp[i]);
                                LogUtil.d("psam test out_value i = " + i + " value = " + output_value[i]);
                            }
                        }else if ("psamPowerSupplypath".equals(xmlPullParser.getName())) {
                            mPowerSupplyPath = xmlPullParser.nextText();
                        }
                        /*else if("check".equals(xmlPullParser.getName())){
 check = Integer.parseInt(xmlPullParser.nextText());
 LogUtil.d("psam test check = " + check);
 }*/
                    }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d("set to 0: " + mPowerSupplyPath);
        writeToFile(mPowerSupplyPath, "0");
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
    }

    static {
        System.loadLibrary("meigpsam-jni");
    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }
}
