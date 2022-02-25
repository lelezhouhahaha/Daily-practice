package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;


public class BotUSBOTGActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private BotUSBOTGActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.show)
    public TextView mShow;
    @BindView(R.id.mouse_area)
    public Button mMouseArea;
    private String mFatherName = "";

    private UsbDeviceEventReceiver mReceiver = null;
    //private StorageManager mStorageManager;
    private String mUdiskTotalSpace= "";
    private String mUdiskUsedSpace="";
    private boolean mAccessMouseFlag = false;
    private boolean mGoOutMouseFlag = false;
    private StorageManager mStorageManager;

    private static final String Bot_connect_change_state_flag = "common_bot_connect_state_flag";
    private static final String Bot_connect_state = "common_bot_connect_state_path";
    private String CRADLE_5V_NODE_KEY = "common_cradle_5v_node";
    private String CRADLE_5V_A_NODE_KEY = "common_cradle_5v_a_node";
    private boolean botflag = true;
    private String botstate = "";
    private String cradle_5v_node = "/sys/meige/gpio_output/cradle_5v/value";
    private String cradle_5v_a_node = "/sys/meige/gpio_output/cradle_5v_a/value";

    private boolean isSLB786 = false;
    private static final String USB_OTG_FLAG_PATH = "/sys/bus/usb/drivers/hub/3-0:1.0";

    private Thread mThread = null;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;


    private boolean isUsbOtgPathExist(){
        File usbOtgPath = new File(USB_OTG_FLAG_PATH);
        Log.i("bot usb otg","usbOtgPath.exists()="+usbOtgPath.exists());
        return usbOtgPath.exists();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_botusb;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.BotUSBOTGActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        isSLB786 = DataUtil.getDeviceName().equals("SLB786");
        if(isSLB786){
            String customCradle5vNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CRADLE_5V_NODE_KEY);
            if(customCradle5vNode != null && !customCradle5vNode.isEmpty()){
                cradle_5v_node = customCradle5vNode;
            }
            String customCradle5vANode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CRADLE_5V_A_NODE_KEY);
            if(customCradle5vANode != null && !customCradle5vANode.isEmpty()){
                cradle_5v_a_node = customCradle5vANode;
            }
            writeToFile(cradle_5v_a_node, "1");
            //writeToFile(cradle_5v_node, "1");
            mShow.setText(getResources().getString(R.string.readyfortest));
        }

        mReceiver = new UsbDeviceEventReceiver();
        Log.d("msrtest","init");
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);
        showUsbList();
        mStorageManager = mContext.getSystemService(StorageManager.class);
        mMouseArea.setVisibility(View.GONE);
        mMouseArea.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                int what = event.getAction();
                //if it is usb ota but not cradle ota, return it
                if(isSLB786 && isUsbOtgPathExist()){
                    return false;
                }

                switch(what){
                    case MotionEvent.ACTION_HOVER_ENTER:
                        LogUtil.d(" bottom ACTION_HOVER_ENTER");
                        break;
                    case MotionEvent.ACTION_HOVER_MOVE:
                        mAccessMouseFlag = true;
                        LogUtil.d(" bottom ACTION_HOVER_MOVE");
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        mGoOutMouseFlag = true;
                        LogUtil.d(" bottom ACTION_HOVER_EXIT");
                        break;
                }
                if(mAccessMouseFlag && mGoOutMouseFlag ) {
                    botflag = changestateflag();
                    if(botflag){
                        mSuccess.setVisibility(View.VISIBLE);
                        if(mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)){
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            deInit(mFatherName, SUCCESS);//auto pass pcba
                        }
                    }
                }
                return false;
            }
        });
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            LogUtil.d("onVolumeStateChanged");
            if(mThread != null){
                mThread.interrupt();
            }
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    getDiskInfo();
                    mHandler.sendEmptyMessage(1003);
                }
            });
            mThread.start();
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            LogUtil.d("onDiskDestroyed");
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }



    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //if it is usb ota but not cradle ota, return it
            if(isSLB786 && isUsbOtgPathExist()){
                return;
            }
            switch (msg.what) {
                case 1001:
                    mShow.setText(getResources().getString(R.string.readyfortest));
                    mMouseArea.setVisibility(View.GONE);
                    mSuccess.setVisibility(View.GONE);
                    break;
                case 1002:
                    mMouseArea.setVisibility(View.VISIBLE);
                    break;
                case 1003:
                    mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize) + mUdiskUsedSpace + "\n\n" + getResources().getString(R.string.uDiskTotalSpaceSize) +mUdiskTotalSpace);
                    mMouseArea.setVisibility(View.GONE);
                    botflag = changestateflag();
                    if(botflag && !TextUtils.isEmpty(mUdiskTotalSpace)){
                        mSuccess.setVisibility(View.VISIBLE);
                        if(mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)){
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            deInit(mFatherName, SUCCESS);//auto pass pcba
                        }
                    }
                    mUdiskUsedSpace = "";
                    mUdiskTotalSpace = "";
                    break;
                case 9999:
                    mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize));
                    mMouseArea.setVisibility(View.GONE);
                    mSuccess.setVisibility(View.GONE);
                    break;
            }


        }
    };



    private void getDiskInfo(){
        LogUtil.i("getDiskInfo() start ...");
        StorageManager ustorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        final List<VolumeInfo> volumes = ustorageManager.getVolumes();
        long totalBytes = 0;
        Context context = AppGlobals.getInitialApplication();

        for (VolumeInfo volume : volumes) {
            if (volume.getType() == VolumeInfo.TYPE_PUBLIC) {
                if (volume.isMountedReadable()) {
                    try {
                        File path = volume.getPath();
                        if (totalBytes <= 0) {
                            totalBytes = path.getTotalSpace();
                        }
                        long freeBytes = path.getFreeSpace();
                        long usedBytes = totalBytes - freeBytes;

                        mUdiskUsedSpace = Formatter.formatFileSize(context, usedBytes);
                        mUdiskTotalSpace = Formatter.formatFileSize(context, totalBytes);
                        break;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        LogUtil.i("getDiskInfo() end ...");
    }



    @Override
    protected void onDestroy() {
        if (mReceiver != null){
            unregisterReceiver(mReceiver);
        }
        if(mThread != null){
            mThread.interrupt();
        }

        if (isSLB786) {
           // writeToFile(cradle_5v_node, "0");
            writeToFile(cradle_5v_a_node, "0");
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
        super.onDestroy();
    }

    public void showUsbList() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        mHandler.sendEmptyMessage(1001);
        while (deviceIterator.hasNext()){
            UsbDevice usbDevice = deviceIterator.next();
            int deviceClass = usbDevice.getDeviceClass();
            if(deviceClass==0) {
                UsbInterface anInterface = usbDevice.getInterface(0);
                if(anInterface.getInterfaceClass()==8){
                    if(mThread != null){
                        mThread.interrupt();
                    }
                    mThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getDiskInfo();
                            mHandler.sendEmptyMessage(1003);
                        }
                    });
                    mThread.start();
                }else if(anInterface.getInterfaceClass()==3){
                    mHandler.sendEmptyMessage(1002);
                }else{
                    mHandler.sendEmptyMessage(1001);
                }
            }
        }
    }

    class UsbDeviceEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            showUsbList();
        }
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            LogUtil.d(" write path:< " + path + ">.");
            LogUtil.d(" write value:< " + value + ">.");
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e("write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }


    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
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

    private String getCurrentnode(String node) {
        String currentNow = "";
        try {
            char[] buffer = new char[1024];

            FileReader fileReader = new FileReader(node);
            int len = fileReader.read(buffer, 0, buffer.length);
            String data = new String(buffer, 0, len);
            currentNow = data;

            fileReader.close();
        } catch (Exception e) {
            LogUtil.e("Get current now node : " + node + "fail.");
            LogUtil.e("e1111 : "+e.toString());
        }
        return currentNow;
    }

    private boolean changestateflag(){
        boolean flag = true;
        String bot_changeflag = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Bot_connect_change_state_flag);
        boolean change_flag = bot_changeflag.equals("true");
        botstate = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Bot_connect_state);
        String botstate_now = getCurrentnode(botstate);
        if(botstate_now.contains("0") && change_flag){
            flag = false;
        }
        return flag;
    }

}
