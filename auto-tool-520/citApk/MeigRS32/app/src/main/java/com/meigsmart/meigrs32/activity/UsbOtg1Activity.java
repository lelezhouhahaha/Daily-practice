package com.meigsmart.meigrs32.activity;

import android.app.AppGlobals;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
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
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import butterknife.BindView;

public class UsbOtg1Activity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private UsbOtg1Activity mContext;
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
    private InputManager mInputManager;
    private boolean mUsbOtgConfigUdisk;
    private String mUdiskTotalSpace= "";
    private String mUdiskUsedSpace="";
    private int mConfigTime = 0;
    private StorageManager mStorageManager;
    private String commonConfigUsbOtgMouse = "common_usb_otg_udisk_config_bool";
    private boolean mAccessMouseFlag = false;
    private boolean mGoOutMouseFlag = false;

    private static final String Bot_connect_change_state_flag = "common_bot_connect_state_flag";
    private static final String Bot_connect_state = "common_bot_connect_state_path";
    private boolean botflag = true;
    private String botstate = "";

    private boolean isSLB786 = DataUtil.getDeviceName().equals("SLB786");
    private static final String USB_OTG_FLAG_PATH = "/sys/bus/usb/drivers/hub/3-0:1.0";

    private boolean isUsbOtgPathExist(){
        File usbOtgPath = new File(USB_OTG_FLAG_PATH);
        Log.i("bot usb otg","usbOtgPath.exists()="+usbOtgPath.exists());
        return usbOtgPath.exists();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_usb_otg;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_usb_otg);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mMouseArea.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                int what = event.getAction();
                //if it is not usb ota , return it
                if(isSLB786 && !isUsbOtgPathExist()){
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
                if(mAccessMouseFlag && mGoOutMouseFlag && !mUsbOtgConfigUdisk) {
                    botflag = changestateflag();
                    if (botflag) {
                        mSuccess.setVisibility(View.VISIBLE);
                        if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            deInit(mFatherName, SUCCESS);//auto pass pcba
                        }
                    }
                }
                return false;
            }
        });

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mStorageManager = mContext.getSystemService(StorageManager.class);

        String tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, commonConfigUsbOtgMouse);
        LogUtil.d("tmpStr:" + tmpStr);
        if(tmpStr.length() != 0) {
            mUsbOtgConfigUdisk = tmpStr.equals("true");
        }else {
            mUsbOtgConfigUdisk = getResources().getBoolean(R.bool.usb_otg_default_config_udisk);
        }
        LogUtil.d("mUsbOtgConfigUdisk:" + mUsbOtgConfigUdisk);

        if( mUsbOtgConfigUdisk ) {
            mMouseArea.setVisibility(View.INVISIBLE);
            mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize) + mUdiskUsedSpace + "\n\n" + getResources().getString(R.string.uDiskTotalSpaceSize) +mUdiskTotalSpace);
        }else{
            //mShow.setVisibility(View.INVISIBLE);
            String valueStr = String.format(getResources().getString(R.string.mouseReminder), getResources().getString(R.string.mouseArea));
            mShow.setText(valueStr);
        }

        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            LogUtil.d("onVolumeStateChanged");
            //if it is not usb ota , return it
            if(isSLB786 && !isUsbOtgPathExist()){
                return ;
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
        if(isSLB786 && !isUsbOtgPathExist()){
            //nothing;
        }else {
            getDiskInfo();
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
                        botflag = changestateflag();
                        if (botflag) {
                            mSuccess.setVisibility(View.VISIBLE);
                            if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                                deInit(mFatherName, SUCCESS);//auto pass pcba
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
        if(botstate_now.contains("1") && change_flag){
            flag = false;
        }
        return flag;
    }
}
