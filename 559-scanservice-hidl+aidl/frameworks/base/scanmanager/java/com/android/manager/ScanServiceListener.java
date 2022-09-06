package com.android.manager;

public abstract class ScanServiceListener {
	public void onScanDataNotify(int deviceId){
	}
	public void onScanDataNotifyCamera0(int deviceId){
	}
	public void onScanDataNotifyCamera1(int deviceId){
	}
	public void onScanDataNotifyCamera2(int deviceId){
	}
	public void onScanDataNotifyExposure(long exposuretime,int iso){
	}
}
