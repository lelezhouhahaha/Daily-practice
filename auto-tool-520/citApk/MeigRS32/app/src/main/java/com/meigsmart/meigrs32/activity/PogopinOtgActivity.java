package com.meigsmart.meigrs32.activity;

import android.app.Activity;
import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
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
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

import static com.meigsmart.meigrs32.util.DataUtil.initConfig;
import static com.meigsmart.meigrs32.util.DataUtil.readLineFromFile;

public class PogopinOtgActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private PogopinOtgActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.show)
    public TextView mShow;
	@BindView(R.id.mouse_area)
    public Button mMouseArea;
    private String mFatherName = "";
    private String mUdiskTotalSpace= "";
    private String mUdiskUsedSpace="";
    private StorageManager mStorageManager;
    private boolean mAccessMouseFlag = false;
    private boolean mGoOutMouseFlag = false;
    String pogopinStatus = "";
    private String projectName = "";

    private static final String POGOPIN_OTG_STATUS_NODE = "/sys/bus/platform/drivers/musb-sprd/20200000.usb/pogo_otg_status";
    private static final String POGOPIN_OTG_STATUS_NODE_NEW = "/sys/bus/i2c/drivers/bq2588x_charger/4-006b/pogo_id";
    private static final String POGOPIN_OTG_STATUS = "common_pogopin_discheck";
    private final String TAG = PogopinOtgActivity.class.getSimpleName();
    private String TAG_POGOPIN_OTG_TYPE_NODE = "dc_pogopin_otg_type_node";
    String mProjectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
    boolean isMC520 = "MC520".equals(mProjectName);
    boolean isMC520_GMS_version = "MC520_GMS".equals(mProjectName);
    private static final String POGOPIN_OTG_UHF = "/sys/class/sunmi_uhf/uhf/chargeUhf";
    private Boolean mCurrentTestResult = false;
    private Runnable mRun = null;
    //private Handler mHandler = null;
    private int mConfigTime = 0;
    private MyHandler mHandler;

    private boolean isPogopinOtg(){
            /*pogopinStatus = readNodeValue(POGOPIN_OTG_STATUS_NODE);
        String str = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, POGOPIN_OTG_STATUS);
        return "low".equals(pogopinStatus) || str.contains("true");*/
        boolean mRet = false;
        String type_node = initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_POGOPIN_OTG_TYPE_NODE);
        LogUtil.d(TAG, "type_node:" + type_node);
        if ((type_node != null) && !type_node.isEmpty()) {
            String type = readLineFromFile(type_node);
            int mUsbTypeValue = Integer.valueOf(type);
            LogUtil.d(TAG, "type:" + type +  " mUsbTypeValue:" + mUsbTypeValue);
            if((mUsbTypeValue >= 200000) &&  (mUsbTypeValue <= 900000))
                mRet = true;
        }
        LogUtil.d(TAG, "mRet:" + mRet);
        return mRet;
    }

    private boolean isPogopin_Otg(){
            pogopinStatus = readNodeValue(POGOPIN_OTG_STATUS_NODE_NEW);
        Log.d("Meig_pogopinotg"," pogopinStatus:"+pogopinStatus);
        return pogopinStatus.contains("0");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_usb_otg;
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        boolean flag = intent.getBooleanExtra("finish", false);
        LogUtil.d(TAG, "onNewIntent flag:" + flag);
        if(flag) {
            LogUtil.d(TAG, "onNewIntent finish current activity!");
            deInit(mFatherName, NOTEST);
            //mContext.finish();
        }
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.PogopinOtgActivity);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        if(isMC520 || isMC520_GMS_version){
            writeToFile(POGOPIN_OTG_UHF,"1");
        }

        mHandler = new MyHandler(mContext);

        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time)*4;
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            Log.d(TAG, "current test is pcbaautotest 1");
            mMouseArea.setVisibility(View.GONE);
        }else {
            mMouseArea.setOnHoverListener(new View.OnHoverListener() {
                @Override
                public boolean onHover(View v, MotionEvent event) {
                    int what = event.getAction();
                    //if it is not usb ota , return it
                    if (!"MT537".equals(projectName)) {
                        if (!isPogopinOtg()) {
                            return false;
                        }
                    } else {
                        Log.d("Meig_pogopinotg", " Pogopin_Otg:" + isPogopin_Otg());
                        if (!isPogopin_Otg()) {
                            return false;
                        }
                    }

                switch(what){
                    case MotionEvent.ACTION_HOVER_ENTER:
                        Log.d("Meig_pogopinotg"," bottom ACTION_HOVER_ENTER");
                        break;
                    case MotionEvent.ACTION_HOVER_MOVE:
                        mAccessMouseFlag = true;
                        Log.d("Meig_pogopinotg"," bottom ACTION_HOVER_MOVE");
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        mGoOutMouseFlag = true;
                        Log.d("Meig_pogopinotg"," bottom ACTION_HOVER_EXIT");
                        break;
                }
                Log.d("Meig_pogopinotg","mAccessMouseFlag:"+mAccessMouseFlag);
                Log.d("Meig_pogopinotg","mGoOutMouseFlag:"+mGoOutMouseFlag);
                if(mAccessMouseFlag && mGoOutMouseFlag) {
                    mSuccess.setVisibility(View.VISIBLE);
                    if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)
                            || mFatherName.equals(MyApplication.MMI1_PreName) || mFatherName.equals(MyApplication.MMI2_PreName)) {
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        deInit(mFatherName, SUCCESS);//auto pass pcba
                    }
                }
                return false;
            }
        });
        }

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mStorageManager = mContext.getSystemService(StorageManager.class);

        //mShow.setVisibility(View.INVISIBLE);
        String valueStr = String.format(getResources().getString(R.string.mouseReminder), getResources().getString(R.string.mouseArea));
        mShow.setText(valueStr);

        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            Log.d(TAG, "current test is pcbaautotest 2");
            mMouseArea.setVisibility(View.INVISIBLE);
            mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize) + mUdiskUsedSpace + "\n\n" + getResources().getString(R.string.uDiskTotalSpaceSize) +mUdiskTotalSpace);
            mHandler = new MyHandler(mContext);
            mRun = new Runnable() {
                @Override
                public void run() {
                    mConfigTime--;
                    LogUtil.d(TAG, "pcba auto test initData mConfigTime:" + mConfigTime);
                    updateFloatView(mContext, mConfigTime);
                    if ((mConfigTime == 0) && (mFatherName.equals(MyApplication.PCBAAutoTestNAME))) {
                        if (mCurrentTestResult) {
                            deInit(mFatherName, SUCCESS);
                        } else {
                            LogUtil.d(TAG, "Pogopin otg Test fail!");
                            deInit(mFatherName, FAILURE, "Pogopin otg Test fail!");
                        }
                        return;
                    }
                    if(isPogopinOtg()){
                        LogUtil.d(TAG, "Pogopin otg Test fail!");
                        mHandler.sendEmptyMessageDelayed(HANDLER_POGOPIN_OTG, 1000);
                        //getDiskInfo();
                    }
                    mHandler.postDelayed(this, 1000);
                }
            };
            mRun.run();
        }
    }

    private static final int HANDLER_POGOPIN_OTG = 1000;
    private static class MyHandler extends Handler {
        WeakReference<Activity> reference;
        public MyHandler(Activity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PogopinOtgActivity activity = (PogopinOtgActivity) reference.get();
            switch (msg.what) {
                case HANDLER_POGOPIN_OTG:
                    activity.getDiskInfo();
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mHandler.removeCallbacks(mRun);
        }
        if(isMC520 || isMC520_GMS_version){
            writeToFile(POGOPIN_OTG_UHF,"0");
        }
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            LogUtil.d("onVolumeStateChanged");
            //if it is not usb ota , return it
            if(!"MT537".equals(projectName)) {
                if (!isPogopinOtg()) {
                    return ;
                }
            }else{
                if (!isPogopin_Otg()) {
                    return ;
                }
            }
            getDiskInfo();
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            LogUtil.d("onDiskDestroyed");
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if(!"MT537".equals(projectName)) {
            if (isPogopinOtg()) {
                getDiskInfo();
            }
        }else{
            if (isPogopin_Otg()) {
                getDiskInfo();
            }
        }
        mStorageManager.registerListener(mStorageListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    private void getDiskInfo(){
        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
       long totalBytes = 0;
        Context context = AppGlobals.getInitialApplication();
		
		for (VolumeInfo volume : volumes) {
			if (volume.getType() == VolumeInfo.TYPE_PUBLIC){
				 if ((volume.isMountedReadable())&&(!volume.disk.isSd())) {
					File path = volume.getPath();
					if (totalBytes <= 0) {
						totalBytes = path.getTotalSpace();
					}
					long freeBytes = path.getFreeSpace();
					long usedBytes = totalBytes - freeBytes;

                    mUdiskUsedSpace = Formatter.formatFileSize(context, usedBytes);
					mUdiskTotalSpace = Formatter.formatFileSize(context, totalBytes);
                    mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize) + mUdiskUsedSpace + "\n\n" + getResources().getString(R.string.uDiskTotalSpaceSize) +mUdiskTotalSpace);
					if(mUdiskTotalSpace.length() != 0 || mUdiskUsedSpace.length() != 0){
                        mSuccess.setVisibility(View.VISIBLE);
						mCurrentTestResult = true;
                        if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)
                                || mFatherName.equals(MyApplication.MMI1_PreName) || mFatherName.equals(MyApplication.MMI2_PreName)) {
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            deInit(mFatherName, SUCCESS);//auto pass pcba
                        }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
							try {
                            	Thread.sleep(3000);
                        	} catch (InterruptedException e) {
                            	e.printStackTrace();
                        	}
						}
					}

                    break;
				}
			}
		}
        mUdiskUsedSpace = "";
        mUdiskTotalSpace = "";
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            Log.e("Meig_pogopinotg", "write to file " + path + " abnormal.");
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

    private String readNodeValue(String node) {
        String value = "";
        try {
            char[] buffer = new char[1024];

            FileReader fileReader = new FileReader(node);
            int len = fileReader.read(buffer, 0, buffer.length);
            String data = new String(buffer, 0, len);
            value = data;

            fileReader.close();
        } catch (Exception e) {
            LogUtil.e("readNodeValue node : " + node + "fail.");
            LogUtil.e("e1111 : "+e.toString());
        }
        LogUtil.e("readNodeValue node : " + node + "="+value.trim());
        return value.trim();
    }

}
