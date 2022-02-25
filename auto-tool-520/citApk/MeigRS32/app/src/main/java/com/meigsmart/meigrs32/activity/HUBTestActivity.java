package com.meigsmart.meigrs32.activity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.StorageVolume;
import android.os.storage.DiskInfo;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.hardware.usb.UsbDevice;
import android.os.SystemClock;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.util.DataUtil;

import android.util.Log;

import butterknife.BindView;

public class HUBTestActivity extends BaseActivity implements InputDeviceListener,View.OnClickListener, PromptDialog.OnPromptDialogCallBack {

	private HUBTestActivity mContext;
	@BindView(R.id.title)
	public TextView mTitle;
	@BindView(R.id.back)
	public LinearLayout mBack;
	@BindView(R.id.success)
	public Button mSuccess;
	@BindView(R.id.fail)
	public Button mFail;
	@BindView(R.id.HUB_msg)
	public TextView mHUBmsg;
	private String mFatherName = "";
	
	private boolean showButton = false;

	private int mUdiskNum = 0;
	private int mUsbNum = 5;
	private InputManager mInputManager = null;
	private int mExternalUsbDeviceNum = 0;
	private List<String> mExternalDeviceVendorIds =null;
	private final String CONFIG_USB_NUM = "common_cit_hub_num";

	private int mConfigTime = 0;
	private Runnable mRun;

	@Override
	protected int getLayoutId() {
		return R.layout.activity_hub_test;
	}

	@Override
	protected void initData() {
		mContext = this;
		super.startBlockKeys = Const.isCanBackKey;
		mBack.setVisibility(View.GONE);
		mSuccess.setVisibility(View.GONE);
		//mFail.setVisibility(View.GONE);
		mBack.setOnClickListener(this);
		mSuccess.setOnClickListener(this);
		mFail.setOnClickListener(this);
		mTitle.setText(R.string.HUBTestActivity);

		mDialog.setCallBack(this);
		mFatherName = getIntent().getStringExtra("fatherName");
		super.mName = getIntent().getStringExtra("name");
		addData(mFatherName, super.mName);


		mInputManager = (InputManager) getSystemService(INPUT_SERVICE);
		mInputManager.registerInputDeviceListener(this, null);
		mExternalDeviceVendorIds = new ArrayList<String>();


		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addDataScheme("file");
		mContext.registerReceiver(mMountedReceiver, filter);

		String Number = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CONFIG_USB_NUM);
		if (Number != null && !Number.isEmpty()){
            mUsbNum = Integer.parseInt(Number);
        }

		if(mFatherName.equals(MyApplication.RuninTestNAME)) {
			mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
		} else {
			mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
		}
		mRun = new Runnable() {
			@Override
			public void run() {
				mConfigTime--;
				updateFloatView(mContext,mConfigTime);
				if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
					mHandler.sendEmptyMessage(1111);
				}
				mHandler.postDelayed(this, 1000);
			}
		};
		mRun.run();

		refresh();
		freshButton();
		//showDialog();
	}

	private final BroadcastReceiver mMountedReceiver = new BroadcastReceiver() {
	        @Override  
	        public void onReceive(Context context, Intent intent) {                                   
	            String action = intent.getAction();  
	                        Log.d("HUBTest", "Receive " + action);  
	                        if (Intent.ACTION_MEDIA_MOUNTED.equals(action) || Intent.ACTION_MEDIA_UNMOUNTED .equals(action)) {  
	                        	refresh();
	                    		freshButton();
	                        }  
	                 }  
	         };  
	
	private void freshButton() {
			
			if ( mUdiskNum + mExternalUsbDeviceNum >= mUsbNum) {
				mSuccess.setVisibility(View.VISIBLE);
				mHandler.sendEmptyMessage(1001);
			} 
			else {
				mSuccess.setVisibility(View.GONE);
			}
	}

	private void refresh() {
		mUdiskNum = 0;
		StorageManager mStorageManager = (StorageManager) mContext
				.getSystemService(Context.STORAGE_SERVICE);

		List<VolumeInfo> volumes = mStorageManager.getVolumes();
		VolumeInfo volumeInfo = null;
		for (VolumeInfo volInfo : volumes) {
            DiskInfo diskInfo = volInfo.getDisk();
            if (diskInfo != null && diskInfo.isUsb()) {
            	mUdiskNum ++;
            }

        }

		mHUBmsg.setText(this.getResources().getString(R.string.hub_udisk_num) + mUdiskNum+"\r\n\r\n\r\n"+refreshUsbInputDevices());
		
	}
	
	private void showDialog() {
		AlertDialog.Builder dialog = 
	            new AlertDialog.Builder(HUBTestActivity.this);
		dialog.setMessage(R.string.hub_usb_num);
		dialog.setPositiveButton(R.string.str_yes, 
	            new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	//mUsbNum = 5;
	            	refresh();
	            	freshButton();
	            }
	        });
		dialog.setNegativeButton(R.string.str_no, 
	            new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	//mUsbNum = 5;
	            	refresh();
	            	freshButton();
	            }
	        });
		dialog.setCancelable(false);	
		dialog.show();
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		mInputManager.unregisterInputDeviceListener(this);
		unregisterReceiver(mMountedReceiver);
		if(mHandler != null){
			mHandler.removeMessages(1001);
			mHandler.removeMessages(1111);
			mHandler.removeMessages(9999);
		}
		super.onDestroy();

		
	}

	@Override
	public void onInputDeviceAdded(int deviceId) {
		Log.d("HUBTest", "onInputDeviceAdded " + deviceId);
		refresh();
		freshButton();
		
	}

	@Override
	public void onInputDeviceChanged(int deviceId) {
		// TODO Auto-generated method stub
		Log.d("HUBTest", "onInputDeviceChanged " + deviceId);
		refresh();
		freshButton();
	}

	@Override
	public void onInputDeviceRemoved(int deviceId) {
		// TODO Auto-generated method stub
		Log.d("HUBTest", "onInputDeviceRemoved " + deviceId);
		refresh();
		freshButton();
	}
	
	
	private String refreshUsbInputDevices() {		
		String deviceListString  = "";
		int[] devices =mInputManager.getInputDeviceIds();
		mExternalUsbDeviceNum = 0;
		mExternalDeviceVendorIds.clear();
		for(int i=0;i<devices.length;i++) {
			int deviceId = devices[i];
			InputDevice device = InputDevice.getDevice(deviceId);  
		    if (device != null && !device.isVirtual() && (device.isFullKeyboard() || device.isExternal())) {//
		        //if(device.getName().contains("Mouse") || device.getName().contains("Keyboard")) {
		    	//if(device.getKeyboardType()!=InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC) {
		    	if(!mExternalDeviceVendorIds.contains(device.getVendorId()+"")) {
		    		mExternalDeviceVendorIds.add(device.getVendorId()+"");
		    		deviceListString+="getName:"+device.getName()+"\r\n";	
		    		mExternalUsbDeviceNum+=1;
		    		//deviceListString+="device.getName()=" + device.getName() + " device.getId() " + device.getId() + " getDescriptor " + device.toString();
		        }
		    }
		}
		if(mExternalUsbDeviceNum==0) {
			deviceListString+="empty\r\n";
		}
		String title = getString(R.string.external_usb_devices_list)+"("+mExternalUsbDeviceNum+")\r\n";
		deviceListString = title+deviceListString;
		return deviceListString;
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

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what){
				case 1001:
					if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME)) {
						deInit(mFatherName, SUCCESS);
					}
					break;
				case 1111:
					deInit(mFatherName, FAILURE);
					break;
				case 9999:
					deInit(mFatherName, FAILURE,msg.obj.toString());
					break;
			}
		}
	};
}
