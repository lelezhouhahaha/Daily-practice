package com.meigsmart.meigrs32.activity;

import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.MotionEvent;
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
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;

public class UsbTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private UsbTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
//    @BindView(R.id.loglog)
//    public TextView Textfive;
    private String mFatherName = "";
    private USBFinder mUsbFinder;
    TextView textone;
    TextView texttwo;
    TextView textthree;
    TextView textfour;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_usb_test2;
    }
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        //mFail.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_usb_test);

        textone = (TextView) findViewById(R.id.usb_msg);
        texttwo = (TextView) findViewById(R.id.mouse_msg);
//        textthree= (TextView) findViewById(R.id.camera_msg);
//        textfour = (TextView) findViewById(R.id.megnetic_msg);

        textone.setText(R.string.usb_unconnected);
//        texttwo.setText(R.string.mouse_unconnected);
//        textthree.setText(R.string.camera_unconnected);
//        textfour.setText(R.string.megnetic_unconnected);


        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbDeviceStateFilter);
        getDeviceList();
    }
    private void getDeviceList(){
        UsbManager usbManager= (UsbManager) getSystemService(Context.USB_SERVICE);
        mUsbFinder= new USBFinder(usbManager);
        findDeviceList();
    }
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
               // Textfive.setText(intent.getAction());
                LogUtil.d("拔出usb了");
                textone.setText(R.string.usb_unconnected);
//                texttwo.setText(R.string.mouse_unconnected);
//                textthree.setText(R.string.camera_unconnected);
//                textfour.setText(R.string.megnetic_unconnected);
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    LogUtil.d("设备的ProductId值为：" + device.getProductId());
                    LogUtil.d("设备的VendorId值为：" + device.getVendorId());
                }
                getDeviceList();
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                LogUtil.d( "插入usb了");
                getDeviceList();
            }
        }
    };

    public class USBFinder {
        private UsbManager mUsbManager;
        public USBFinder(UsbManager usbManager) {
            this.mUsbManager = usbManager;
        }
        public HashMap<String, UsbDevice> findUsbMap() {
            HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
            LogUtil.d("happy:"+mUsbManager.getDeviceList());
            Iterator<UsbDevice> deviceIterator = deviceMap.values().iterator();
            String buf=deviceMap.size()+"\n";

            while (deviceIterator.hasNext()){
                UsbDevice usbDevice = deviceIterator.next();
                int deviceClass = usbDevice.getDeviceClass();
                    UsbInterface anInterface = usbDevice.getInterface(0);
                    if (anInterface.getInterfaceClass() == 8) {
//                        textone.setText(R.string.usb_connected);
//                        texttwo.setText(R.string.mouse_connected);
//                        textthree.setText(R.string.camera_connected);
//                        textfour.setText(R.string.megnetic_connected);
                        String buf1="";
                        for(int i=1;i<=deviceMap.size();i++) {
                            buf1=buf1+getString(R.string.usb_connected) +
                                        "：" + i+"\n";
                        }
                        textone.setText(buf1);
                    }

//                    else if (anInterface.getInterfaceClass() == 3){
//
//                    }
//                    else if(anInterface.getInterfaceClass() == 14){
//
//                    }
//                    else if(anInterface.getInterfaceClass() != 255){
//
//                    }
            }
            if(deviceMap.size()>= 5){
                mSuccess.setVisibility(View.VISIBLE);
                if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                    deInit(mFatherName, SUCCESS);//auto pass pcba
                }
            }

//            for(Map.Entry<String, UsbDevice> entry: deviceMap.entrySet())
//            {
//                LogUtil.d("Key: "+ entry.getKey()+ " Value: "+((UsbDevice)entry.getValue()).getInterface(0).toString());
//               buf=buf+"Key: "+ entry.getKey()+ " Value: "+((UsbDevice)entry.getValue()).getInterface(0).toString()+"\n";
//            }

            return deviceMap;
        }


        public UsbDevice findUsbDevice(int vendorId, int productId) {
            HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
            Iterator iterator = deviceMap.values().iterator();
            UsbDevice localUsbDevice;
            while (iterator.hasNext()) {
                localUsbDevice = (UsbDevice) iterator.next();
                if (localUsbDevice.getVendorId() == vendorId || localUsbDevice.getProductId() == productId) {
                    return localUsbDevice;
                }
            }
            return null;
        }
    }
    public HashMap<String, UsbDevice> findDeviceList() {
        LogUtil.d("happyhappy ---1");
        return mUsbFinder.findUsbMap();
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

        if(requestCode == 11 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }
}
