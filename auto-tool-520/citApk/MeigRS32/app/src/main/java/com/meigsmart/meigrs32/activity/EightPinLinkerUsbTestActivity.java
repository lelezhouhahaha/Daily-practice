package com.meigsmart.meigrs32.activity;

import android.app.AlertDialog;
import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.text.format.Formatter;
import android.util.Log;
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

public class EightPinLinkerUsbTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private EightPinLinkerUsbTestActivity mContext;
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
    boolean ledstate = false;
    boolean usbotg = false;



    @Override
    protected int getLayoutId() {
        return R.layout.activity_eightpin_linker_usb_test;
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
        mTitle.setText(R.string.run_eightpinlinkerusbtest);

        textone = (TextView) findViewById(R.id.usb_msg);
        texttwo = (TextView) findViewById(R.id.mouse_msg);
        textthree= (TextView) findViewById(R.id.equipment_msg);

        textone.setText(R.string.usbi_unconnected);
//        texttwo.setText(R.string.mousei_unconnected);
//        textthree.setText(R.string.equipmenti_unconnected);


        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbDeviceStateFilter);
        getDeviceList();
        showDialog();
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
                textone.setText(R.string.usbi_unconnected);
//                texttwo.setText(R.string.mousei_unconnected);
//                textthree.setText(R.string.equipmenti_unconnected);

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
            String buf=deviceMap.size()+"";
            while (deviceIterator.hasNext()){
                UsbDevice usbDevice = deviceIterator.next();
                int deviceClass = usbDevice.getDeviceClass();
                if(deviceClass==0) {
                    UsbInterface anInterface = usbDevice.getInterface(0);
                    if (anInterface.getInterfaceClass() == 8) {
                        textone.setText(R.string.usbi_connected);

                        usbotg = true;
                        update ();

                        LogUtil.d("Finger device Type",anInterface + "");
                    } else if (anInterface.getInterfaceClass() == 3) {
                        texttwo.setText(R.string.mousei_connected);

                        usbotg = true;
                        update ();

                        LogUtil.d("Finger device Type",anInterface + "");
                    } else {
                        textthree.setText(R.string.equipment_connected);

                        usbotg = true;
                        update ();

                        LogUtil.d("Finger device Type",anInterface + "");
                    }
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

    private void showDialog() {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(EightPinLinkerUsbTestActivity.this);
        dialog.setMessage(R.string.power_led_on);
        dialog.setPositiveButton(R.string.str_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ledstate = true ;
                        update ();

                    }
                });
        dialog.setNegativeButton(R.string.str_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       ledstate  = false;
                        update ();

                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }
    private  void update ()
    {
        if (ledstate && usbotg ){
            mSuccess.setVisibility(View.VISIBLE);
        }
    }
}
