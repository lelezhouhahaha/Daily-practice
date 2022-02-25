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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.List;

import butterknife.BindView;

public class FingerOtgActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private FingerOtgActivity mContext;
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
    private int mConfigTime = 0;
    private StorageManager mStorageManager;

    private String cradle_5v_node = "/sys/meige/gpio_output/cradle_5v/value";
    private String finger_power_node = "/sys/meige/gpio_output/finger_power/value";


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
        mTitle.setText(R.string.FingerOtgActivity);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);

       // writeToFile(cradle_5v_node, "1");
        writeToFile(finger_power_node, "1");

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

        mMouseArea.setVisibility(View.INVISIBLE);
        mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize) + mUdiskUsedSpace + "\n\n" + getResources().getString(R.string.uDiskTotalSpaceSize) +mUdiskTotalSpace);

        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mSuccess.setVisibility(View.VISIBLE);
        mFail.setVisibility(View.VISIBLE);
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            LogUtil.d(" uhf path:< " + path + ">.");
            LogUtil.d(" uhf value:< " + value + ">.");
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e( "write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            LogUtil.d("onVolumeStateChanged");
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
        getDiskInfo();
        mStorageManager.registerListener(mStorageListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // writeToFile(cradle_5v_node, "0");
        writeToFile(finger_power_node, "0");
    }

    private void getDiskInfo(){
        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
       long totalBytes = 0;
        Context context = AppGlobals.getInitialApplication();
        mUdiskUsedSpace = "";
        mUdiskTotalSpace = "";

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

                    break;
				}
			}
		}
        mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize) + mUdiskUsedSpace + "\n\n" + getResources().getString(R.string.uDiskTotalSpaceSize) +mUdiskTotalSpace);
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



}
