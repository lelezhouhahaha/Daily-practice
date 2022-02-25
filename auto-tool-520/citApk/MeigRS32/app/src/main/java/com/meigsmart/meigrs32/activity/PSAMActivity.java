package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.ByteUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.util.SerialPort;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import butterknife.BindView;


public class PSAMActivity extends BaseActivity implements View.OnClickListener{
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    @BindView(R.id.psamChange)
    public  TextView mPsamChange;
    private PSAMActivity mContext;
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
    boolean cardSlotStatus = false;
    boolean cardActivitionStatus = false;
    int NUMBER_R = 20;
    int NUMBER_W = 3;
    int LOOPNUM = 5;
    private int getSlotNum = 0;
    private int powerOnNum = 0;
    private boolean loopeProcessStatus = true;  //false: loop must be stop   true: loop can continue
    private final String PSAM_IS_CLOSE_PRINTK_FUNCTION_BOOL = "common_PSAMActivity_close_printk_bool";
    private final String PRINTK_PATH = "/proc/sys/kernel/printk";
    private final String OPEN_PRINTK_VALUE = "4 6 1 7";
    private final String CLOSE_PRINTK_VALUE = "0 0 0 0";
    private String mPsamIsClosePrintk = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_psam;
    }

    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.pcba_psam);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mFail.setVisibility(View.INVISIBLE);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime =10;
        }
        mValues.setText(String.format(getString(R.string.psam_test), mConfigTime));
        xmlParse();
        mPsamIsClosePrintk = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, PSAM_IS_CLOSE_PRINTK_FUNCTION_BOOL);
        if(!mPsamIsClosePrintk.isEmpty() && mPsamIsClosePrintk.equals("true")){
            FileUtil.writeToFile(PRINTK_PATH, OPEN_PRINTK_VALUE);
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (mConfigTime == 0) {
                    loopeProcessStatus = false;
                    if(mFatherName.equals(MyApplication.PCBASignalNAME)||mFatherName.equals(MyApplication.PreSignalNAME)) {
                        if (cardSlotStatus && cardActivitionStatus) {
                            mSuccess.setVisibility(View.VISIBLE);
                            mValues.setText(getString(R.string.psam_test_normal));
                            mFail.setVisibility(View.VISIBLE);
                        } else {
                            mValues.setText(getString(R.string.psam_test_next));
                            mFail.setVisibility(View.VISIBLE);
                        }
                    }else {
                        if(cardSlotStatus && cardActivitionStatus){
                            deInit(mFatherName, SUCCESS);
                        }else   deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    }
                    mConfigTime = 1;
                   //mHandler.sendEmptyMessage(1002);
                }
                LogUtil._d("check mSerialPort status. " + mConfigTime);
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        mHandler.sendEmptyMessage(1000);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1000: {
                    if(!loopeProcessStatus){
                        getSlotNum = 0;
                        mHandler.sendEmptyMessage(1002);
                        break;
                    }
                    getPsamCardSlotStatus(device_name);
                    getSlotNum++;
                    if(cardSlotStatus || (getSlotNum >= LOOPNUM)) {
                        getSlotNum = 0;
                        //loopeProcessStatus = false;
                        mHandler.sendEmptyMessage(1001);
                    }else{
                        mHandler.sendEmptyMessage(1000);
                    }
                    break;
                }
                case 1001:
                    {
                        if(!loopeProcessStatus){
                            mHandler.sendEmptyMessage(1002);
                            powerOnNum = 0;
                            break;
                        }
                        if (cardSlotStatus) {
                            getPsamCardPowerOnStatus(device_name);
                            powerOnNum++;

                            if (cardActivitionStatus || (powerOnNum >= LOOPNUM)) {
                                powerOnNum = 0;
                                mHandler.sendEmptyMessage(1002);
                            } else {
                                mHandler.sendEmptyMessage(1001);
                            }
                        } else {
                            powerOnNum = 0;
                            mHandler.sendEmptyMessage(1002);
                        }
                        break;
                    }
                case 1002:
                    if(mFatherName.equals(MyApplication.PCBASignalNAME)||mFatherName.equals(MyApplication.PreSignalNAME)){
                        if (cardSlotStatus && cardActivitionStatus) {
                            String psamFirmWareVersion = getPsamVersion("/dev/spidev0.0");
                            LogUtil._d("psamFirmWareVersion:" +psamFirmWareVersion);
                            //SystemProperties.set("persist.custmized.psam_version", psamFirmWareVersion);
                            SystemProperties.set(OdmCustomedProp.getPsamVersionProp(), psamFirmWareVersion);
                            mSuccess.setVisibility(View.VISIBLE);
                            mValues.setText(getString(R.string.psam_test_normal));
                            mFail.setVisibility(View.VISIBLE);
                        } else {
                            mValues.setText(getString(R.string.psam_test_next));
                            mFail.setVisibility(View.VISIBLE);
                        }
                    }else {
                        if(cardSlotStatus && cardActivitionStatus){
                            String psamFirmWareVersion = getPsamVersion("/dev/spidev0.0");
                            LogUtil._d(" psamFirmWareVersion:" +psamFirmWareVersion);
                            //SystemProperties.set("persist.custmized.psam_version", psamFirmWareVersion);
                            SystemProperties.set(OdmCustomedProp.getPsamVersionProp(), psamFirmWareVersion);
                            deInit(mFatherName, SUCCESS);
                        }else   deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    private void xmlParse(){
        File configFile = new File(config_file);
        if(!configFile.exists()){
            LogUtil._d("psam test file not exists");
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
                LogUtil._d("parser xml");
                String nodeName = xmlPullParser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    if("dev".equals(xmlPullParser.getName())){
                        device_name = xmlPullParser.nextText();
                        LogUtil._d("psam test device name = " + device_name);
                    }else if("baud".equals(xmlPullParser.getName())){
                        baud = Integer.parseInt(xmlPullParser.nextText());
                        LogUtil._d("psam test baud = " + baud);
                    }else if("input".equals(xmlPullParser.getName())){
                        String value = xmlPullParser.nextText();
                        String[] tmp = value.split(",");
                        input_value = new int[tmp.length];
                        for(int i = 0; i < tmp.length; i++){
                            input_value[i] = Integer.parseInt(tmp[i]);
                            LogUtil._d("psam test input_value i = " + i + " value = " + input_value[i]);
                        }
                    }else if("output".equals(xmlPullParser.getName())){
                        String value = xmlPullParser.nextText();
                        String[] tmp = value.split(",");
                        output_value = new int[tmp.length];
                        for(int i = 0; i < tmp.length; i++){
                            output_value[i] = Integer.parseInt(tmp[i]);
                            LogUtil._d("psam test out_value i = " + i + " value = " + output_value[i]);
                        }
                    }else if ("psamPowerSupplypath".equals(xmlPullParser.getName())) {
                        mPowerSupplyPath = xmlPullParser.nextText();
                    }
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String sendSerialCmmd(String devPath, String cmmd){
        RandomAccessFile PsamRaf=null;
        File file=null;
        int loopCountR = NUMBER_R;
        int loopCountW = NUMBER_W;

        try {
            file=new File(devPath);
            PsamRaf=new RandomAccessFile(file,"rw");
            byte[] recv=new byte[260];
            int hasRead=0;


            while((loopCountW>0) && loopeProcessStatus) {
                byte[] cmmdData = ByteUtil.hexStr2Bytes(cmmd);
                for(int i = 0; i < cmmdData.length; i++)
                    LogUtil._d( " send[" + i + "]:<" + cmmdData[i] + ">.");
                PsamRaf.write(cmmdData);
                loopCountR = NUMBER_R;

                while(loopCountR>0 && loopeProcessStatus) {
                    //Thread.sleep(10);
                    hasRead = PsamRaf.read(recv);
                    LogUtil._d(" hasRead:" + hasRead +  "  loopCount:" + loopCountR);
                    if (hasRead > 0) {
                        byte[] valid = Arrays.copyOf(recv, hasRead);
                        //byte[] outData = Arrays.copyOf(valid, 12);
                        String outDataStr = ByteUtil.bytes2HexStr(valid);
                        LogUtil._d(" valid:" + valid);
                        LogUtil._d(" outDataStr:" + outDataStr);
                        if(outDataStr.contains("AA")) {
                            PsamRaf.close();
                            return outDataStr;
                        }
                    }
                    loopCountR--;
                    //System.out.print(new String(recv,0,hasRead));
                }
                loopCountW--;
            }
            PsamRaf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getPsamVersion(String devPath){
        String cmmdGetPsamVersion = "AA0002DE76";

        String getPsamVersionRecvStr = sendSerialCmmd(devPath, cmmdGetPsamVersion);
        LogUtil._d(" getPsamVersionRecvStr:" + getPsamVersionRecvStr);
        if(getPsamVersionRecvStr.equals("")){
            return "";
        }
        LogUtil._d(" getPsamVersion getPsamVersionRecvStr.substring(0,2):" + getPsamVersionRecvStr.substring(0,2));
        LogUtil._d( " getPsamVersion getPsamVersionRecvStr.substring(8,10):" + getPsamVersionRecvStr.substring(8,10));

        if ( getPsamVersionRecvStr.substring(0,2).equals("AA") && getPsamVersionRecvStr.substring(8,10).equals("00")  ) {
            String recvDataLen = getPsamVersionRecvStr.substring(4,6);
            LogUtil._d( " getPsamVersion recvDataLen:" + recvDataLen);
            int len = 0;
            try {
                len = Integer.parseInt(recvDataLen);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            LogUtil._d( " getPsamVersion len:" + len);
            String recvDataVersionStr = getPsamVersionRecvStr.substring(10,len*2+2*2);
            LogUtil._d( " getPsamVersion recvDataVersionStr:" + recvDataVersionStr);
            String DataVersionStr = ByteUtil.hexStr2Str(recvDataVersionStr);
            LogUtil._d(" DataVersionStr:" + DataVersionStr);
            return DataVersionStr;
        }

        return "";
    }

    private void getPsamCardSlotStatus(String devPath) {
        String cmmdGetPsamCardSlotStatus = "AA000211B9";

        String getPsamCardSlotStatusStr = sendSerialCmmd(devPath, cmmdGetPsamCardSlotStatus);
        LogUtil._d(" getPsamCardSlotStatusStr:" + getPsamCardSlotStatusStr);

        if(getPsamCardSlotStatusStr.equals("")){
            return;
        }

        if (getPsamCardSlotStatusStr.contains("AA0004110000") || getPsamCardSlotStatusStr.contains("AA0004110001")) {
            cardSlotStatus = true;
            LogUtil._d("get card Slot Status suceess");
        }

       // mHandler.sendEmptyMessage(1001);
    }

    private void getPsamCardPowerOnStatus(String devPath) {
        String cmmdGetPsamCardPowerOnStatus = "AA0003210880";

        String getPsamCardPowerOnStatusStr = sendSerialCmmd(devPath, cmmdGetPsamCardPowerOnStatus);
        LogUtil._d(" getPsamCardPowerOnStatusStr:" + getPsamCardPowerOnStatusStr);
        if(getPsamCardPowerOnStatusStr.equals("")){
            return;
        }

        if (!getPsamCardPowerOnStatusStr.contains("AA00042100018E") && !getPsamCardPowerOnStatusStr.contains("AA0003210189")) {
            cardActivitionStatus = true;
            LogUtil._d("card power on suceess");
        }
    }

    private void getPsamCardPowerOffStatus(String devPath) {
        String cmmdGetPsamCardPowerOffStatus = "AA00023199";

        String getPsamCardPowerOffStatusStr = sendSerialCmmd(devPath, cmmdGetPsamCardPowerOffStatus);
        LogUtil._d(" getPsamCardPowerOnStatusStr:" + getPsamCardPowerOffStatusStr);
        if(getPsamCardPowerOffStatusStr.equals("")){
            return;
        }
        if (getPsamCardPowerOffStatusStr.contains("AA00") && getPsamCardPowerOffStatusStr.contains("3100")) {
            LogUtil._d("card power off suceess");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!mPsamIsClosePrintk.isEmpty() && mPsamIsClosePrintk.equals("true")){
            FileUtil.writeToFile(PRINTK_PATH, CLOSE_PRINTK_VALUE);
        }
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
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
