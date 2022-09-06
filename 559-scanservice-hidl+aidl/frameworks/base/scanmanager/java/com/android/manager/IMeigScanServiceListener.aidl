package com.android.manager;

oneway interface IMeigScanServiceListener {
	void onScanDataNotify(int deviceId);
	void onScanDataNotifyCamera0(int deviceId);
	void onScanDataNotifyCamera1(int deviceId);
	void onScanDataNotifyCamera2(int deviceId);
	void onScanDataNotifyExposure(long exposuretime,int iso);
}
