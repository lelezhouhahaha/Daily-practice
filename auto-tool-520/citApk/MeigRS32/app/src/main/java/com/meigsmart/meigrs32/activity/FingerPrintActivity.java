package com.meigsmart.meigrs32.activity;
/*this is for 786 finger print test*/

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
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
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import butterknife.BindView;

public class FingerPrintActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private FingerPrintActivity mContext;
    @BindView(com.meigsmart.meigrs32.R.id.title)
    public TextView mTitle;
    @BindView(com.meigsmart.meigrs32.R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    private boolean is786 = false;//for MC510 test
    private UsbDeviceReceiver mReceiver = null;
    private String device_judge = "common_device_fingertest_origin_judge";
	private final String OTG_STATE_PATH = "sys/devices/platform/soc/soc:meig-gpios/meig-gpios/otg_enable";
    private final String MODE_USUAL = "0";
    private final String MODE_FIN = "1";
    private String TAG_MC510 = "common_device_name_test";
    private boolean is_MC510 = false;
    private String TAG_DEVICE_NAME = "common_device_name_test";


    @BindView(com.meigsmart.meigrs32.R.id.success)
    public Button mSuccess;
    @BindView(com.meigsmart.meigrs32.R.id.fail)
    public Button mFail;

    @BindView(R.id.get_finger)
    public Button mGetImage;
    private ProgressDialog mProgressDlg = null;
    private GetImageThread m_getImageThread = null;
    private int iVID = 0x821B;
    private int iPID = 0x0202;
    Class<?> MXFingerAPI = null;
    private Object fingerAPI = null;

    //add for MC901
    private static final int FP_TEST_REQUEST_CODE = 150;
    private String mDeviceName = DataUtil.getDeviceName();
    private boolean isMC901 = false;

    @Override
    protected int getLayoutId() {
        return com.meigsmart.meigrs32.R.layout.activity_fingerprint;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.FingerPrintActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        //add for MC901
        isMC901 = "MC901".equals(mDeviceName);
        if(isMC901){
            Intent testIntent = new Intent();
            testIntent.putExtra("config_autoexit", true);//控制是否测试完成后自动退出 activity， true/fase
            testIntent.putExtra("config_autotest", true);//控制是否启动 activity 自动执行测试
            testIntent.putExtra("config_autoexit_delay_time", 500);
            testIntent.putExtra("config_supportTouchTest", true);
            testIntent.putExtra("config_showcapturedImg", false);
            testIntent.putExtra("config_savecapturedImg", false);
            testIntent.setClassName("com.fpsensor.fpSensorExtensionSvc2","com.fpsensor.sensortesttool.sensorTestActivity");
            startActivityForResult(testIntent,FP_TEST_REQUEST_CODE);

        }else {
            //add for MC510
            is_MC510 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_MC510).equals("MC510");
            if (is_MC510) {
                writeToFile(OTG_STATE_PATH, MODE_FIN);
            }
            String deviceflag = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, device_judge);
            Log.d("Finger deviceflag", deviceflag + "");
            if (deviceflag.equals("false")) {
                is786 = false;//for MC510 test
            }
            is786 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_DEVICE_NAME).equals("SLB786");

            mHandler.sendEmptyMessageDelayed(1001, 1000);


            if (is786) {
                // begin modified by sijingjing for task 2826: SLB786 move fingerprint test into cit
            /*
            ComponentName componentName = new ComponentName("com.example.scannerfinger", "com.example.scannerfinger.MainActivity");
            Intent intent = new Intent();
            intent.setComponent(componentName);
            startActivityForResult(intent, 11);
            */
                mSuccess.setVisibility(View.VISIBLE);
                try {
                    MXFingerAPI = Class.forName("org.zz.api.MXFingerAPI");
                    Constructor<?> cons = MXFingerAPI.getConstructor(Context.class, int.class, int.class);
                    fingerAPI = cons.newInstance(this, iPID, iVID);
                } catch (Exception e) {
                    e.printStackTrace();
                    deInit(mFatherName, FAILURE);
                    return;
                }
                if (m_getImageThread != null) {
                    m_getImageThread.interrupt();
                    m_getImageThread = null;
                }
                mGetImage.setVisibility(View.VISIBLE);
                mGetImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showProgressDlg(getString(R.string.fingerprint_getImage),
                                getString(R.string.fingerprint_promptPressFinger));
                        m_getImageThread = new GetImageThread();
                        m_getImageThread.start();
                    }
                });
                // end modified by sijingjing for task 2826: SLB786 move fingerprint test into cit
            }
        }
    }

    // begin added by sijingjing for task 2826: SLB786 move fingerprint test into cit
    private void showProgressDlg(String strTitle, String strMsg) {
        mProgressDlg = ProgressDialog.show(FingerPrintActivity.this, strTitle, strMsg, true);
    }
    private void dismissProgressDlg() {
        if (mProgressDlg != null) {
            mProgressDlg.cancel();
            mProgressDlg = null;
        }
    }
    private class GetImageThread extends Thread {
        public void run() {
            try {
                GetImage();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public void GetImage() {
        int ret = 0;
        byte[] bFingerImage = new byte[256 * 360];
        Arrays.fill(bFingerImage, (byte) 0);
        try {
            if (fingerAPI != null) {
                Method m = MXFingerAPI.getDeclaredMethod("mxCaptueFingerprint", byte[].class, int.class, int.class);
                ret = (int)m.invoke(fingerAPI, bFingerImage, 15*1000, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ret == 0) {
            // mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
            // deInit(mFatherName, SUCCESS);
            mHandler.sendEmptyMessage(2001);
        } else {
            mHandler.sendEmptyMessage(2002);
        }
    }
    // end added by sijingjing for task 2826: SLB786 move fingerprint test into cit

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mReceiver = new UsbDeviceReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
                    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                    registerReceiver(mReceiver, filter);
                    if (!is786)
                        DetectUsb();
                    break;
                // begin added by sijingjing for task 2826: SLB786 move fingerprint test into cit
                case 2001:
                    mSuccess.setVisibility(View.VISIBLE);
                    mFail.setVisibility(View.GONE);
                    dismissProgressDlg();
                    break;
                case 2002:
                    mSuccess.setVisibility(View.GONE);
                    mFail.setVisibility(View.VISIBLE);
                    dismissProgressDlg();
                    break;
                // end added by sijingjing for task 2826: SLB786 move fingerprint test into cit
            }
        }
    };

    public void DetectUsb() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()){
            UsbDevice usbDevice = deviceIterator.next();
            int deviceClass = usbDevice.getDeviceClass();
            if(deviceClass==0) {
                UsbInterface anInterface = usbDevice.getInterface(0);//device type
                mSuccess.setVisibility(View.VISIBLE);
                Log.d("Finger device Type",anInterface + "");
            }
        }
    }

    class UsbDeviceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!is786){
                DetectUsb();
            }
        }
    }



    @Override
    protected void onDestroy() {
        // begin added by sijingjing for task 2826: SLB786 move fingerprint test into cit
        if (is786) {
            if (m_getImageThread != null) {
                m_getImageThread.interrupt();
                m_getImageThread = null;
            }
            dismissProgressDlg();
            try {
                if (fingerAPI != null) {
                    Method m = MXFingerAPI.getDeclaredMethod("unRegUsbMonitor");
                    m.invoke(fingerAPI);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // end added by sijingjing for task 2826: SLB786 move fingerprint test into cit
        if (mReceiver != null){
            unregisterReceiver(mReceiver);
        }
		if(is_MC510){
            writeToFile(OTG_STATE_PATH,MODE_USUAL);
        }
        mHandler.removeMessages(1001);
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
        ///add for MC901
        LogUtil.d("resultCode: " + resultCode +" requestCode="+requestCode);
        if(requestCode == FP_TEST_REQUEST_CODE ) {
            if (resultCode == RESULT_OK) {
                mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
                deInit(mFatherName, SUCCESS);
            }else{
                mFail.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.red_800));
                deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
            }
        }


    }

	private boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
