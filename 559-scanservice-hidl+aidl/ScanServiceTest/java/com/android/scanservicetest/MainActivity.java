package com.android.scanservicetest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import vendor.scan.hardware.scanservice.V1_0.IScanService;
import vendor.scan.hardware.scanservice.V1_0.IScanServiceCallback;

//import com.android.ScanManager;

public class MainActivity extends AppCompatActivity {

    public static final String SCAN_SERVICE = "scan";
    private final String TAG = "ScanServiceTest";
    private IScanService mScanService;
   private IScanServiceCallback.Stub mScanServiceCallback = new IScanServiceCallback.Stub() {
       @Override
       public void onNotify(int deviceId)throws RemoteException {
           Log.d(TAG, "IScanServiceCallback.Stub onNotify deviceId:" + deviceId);
       }
       @Override
       public void onNotifyCamera0Data(int deviceId)throws RemoteException{
           Log.d(TAG, "IScanServiceCallback.Stub onNotifyCamera0Data deviceId:" + deviceId);
       }

       @Override
       public void onNotifyCamera1Data(int deviceId)throws RemoteException{
           Log.d(TAG, "IScanServiceCallback.Stub onNotifyCamera1Data deviceId:" + deviceId);
       }

       @Override
       public void onNotifyCamera2Data(int deviceId)throws RemoteException{
           Log.d(TAG, "IScanServiceCallback.Stub onNotifyCamera2Data deviceId:" + deviceId);
       }

       @Override
       public void onNotifyExposure(long exposuretime,int iso)throws RemoteException{
           Log.d(TAG, "IScanServiceCallback.Stub onNotifyExposure exposuretime:" + exposuretime + " iso:" + iso);
       }
   };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*ScanManager mScanManager = (ScanManager)getSystemService(SCAN_SERVICE);
        mScanManager.Open(0, 1280, 800, 37);
        mScanManager.Resume(0);
        mScanManager.Suspend(0);
        mScanManager.SetParameters(0, 12, 1);
        mScanManager.Capture(0);
        mScanManager.MoveFocus(0, 12);
        mScanManager.Close(0);*/
        //mScanService = IScanService.getService();
        try {
            Log.d(TAG, "zll ScanService");
            mScanService = IScanService.getService();
            mScanService.setScanServiceCallback(mScanServiceCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
            mScanService = null;
        }

        try {
            mScanService.open(0, 1280, 800, 35);
			mScanService.open(1, 1280, 800, 37);
			mScanService.open(2, 1280, 800, 37);
            mScanService.resume(0);
			mScanService.resume(1);
			mScanService.resume(2);
            //mScanService.suspend(0);
            mScanService.capture(0);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception operation IScanService service: " + e);
            //mScanService = null;
        }
    }
}