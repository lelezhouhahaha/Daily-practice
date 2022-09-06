package com.android.server;

import android.content.Context;
import android.util.Log;
import com.android.manager.IScanManager;
import com.android.manager.IMeigScanServiceListener;
import android.os.RemoteException;
import java.util.List;
import java.util.ArrayList;
import android.os.IBinder;
import vendor.scan.hardware.scanservice.V1_0.IScanService;
import vendor.scan.hardware.scanservice.V1_0.IScanServiceCallback;

public class ScanService extends IScanManager.Stub {
    private final Context mContext;
	public final String TAG = "ScanService";
	private IScanService mIScanService;
	
	final private ArrayList<ScanServiceBinderListener> mListeners =
            new ArrayList<ScanServiceBinderListener>();
	
    private final class ScanServiceBinderListener implements IBinder.DeathRecipient {
        final IMeigScanServiceListener mListener;
	
        ScanServiceBinderListener(IMeigScanServiceListener listener) {
            mListener = listener;
        }
	
        public void binderDied() {
            Log.d(TAG, "An IMeigScanServiceListener has died!");
            synchronized (mListeners) {
                mListeners.remove(this);
                mListener.asBinder().unlinkToDeath(this, 0);
            }
        }
    }
	
	private IScanServiceCallback.Stub mScanServiceCallback = new IScanServiceCallback.Stub() {
		@Override
		public void onNotify(int deviceId)throws RemoteException{
			Log.d(TAG, "IScanServiceCallback.Stub onNotify deviceId:" + deviceId);
			synchronized (mListeners) {
				for (int i = mListeners.size() - 1; i >= 0; i--) {
				   ScanServiceBinderListener bl = mListeners.get(i);
				   try {
					   bl.mListener.onScanDataNotify(deviceId);
				   } catch (RemoteException rex) {
					   Log.e(TAG, "Listener dead");
					   mListeners.remove(i);
				   } catch (Exception ex) {
					   Log.e(TAG, "Listener failed", ex);
				   }
			   }
			}
		}
		
		@Override
		public void onNotifyCamera0Data(int deviceId){
			Log.d(TAG, "IScanServiceCallback.Stub onNotifyCamera0Data deviceId:" + deviceId);
			synchronized (mListeners) {
				for (int i = mListeners.size() - 1; i >= 0; i--) {
				   ScanServiceBinderListener bl = mListeners.get(i);
				   try {
					   bl.mListener.onScanDataNotifyCamera0(deviceId);
				   } catch (RemoteException rex) {
					   Log.e(TAG, "Listener dead");
					   mListeners.remove(i);
				   } catch (Exception ex) {
					   Log.e(TAG, "Listener failed", ex);
				   }
			   }
			}
		}
		
		@Override
		public void onNotifyCamera1Data(int deviceId){
			Log.d(TAG, "IScanServiceCallback.Stub onNotifyCamera1Data deviceId:" + deviceId);
			synchronized (mListeners) {
				for (int i = mListeners.size() - 1; i >= 0; i--) {
				   ScanServiceBinderListener bl = mListeners.get(i);
				   try {
					   bl.mListener.onScanDataNotifyCamera1(deviceId);
				   } catch (RemoteException rex) {
					   Log.e(TAG, "Listener dead");
					   mListeners.remove(i);
				   } catch (Exception ex) {
					   Log.e(TAG, "Listener failed", ex);
				   }
			   }
			}
		}
		
		@Override
		public void onNotifyCamera2Data(int deviceId){
			Log.d(TAG, "IScanServiceCallback.Stub onNotifyCamera2Data deviceId:" + deviceId);
			synchronized (mListeners) {
				for (int i = mListeners.size() - 1; i >= 0; i--) {
				   ScanServiceBinderListener bl = mListeners.get(i);
				   try {
					   bl.mListener.onScanDataNotifyCamera2(deviceId);
				   } catch (RemoteException rex) {
					   Log.e(TAG, "Listener dead");
					   mListeners.remove(i);
				   } catch (Exception ex) {
					   Log.e(TAG, "Listener failed", ex);
				   }
			   }
			}
		}
		
		@Override
		public void onNotifyExposure(long exposuretime,int iso){
			Log.d(TAG, "IScanServiceCallback.Stub onNotifyExposure exposuretime:" + exposuretime + " iso:" + iso);
			synchronized (mListeners) {
				for (int i = mListeners.size() - 1; i >= 0; i--) {
				   ScanServiceBinderListener bl = mListeners.get(i);
				   try {
					   bl.mListener.onScanDataNotifyExposure(exposuretime, iso);
				   } catch (RemoteException rex) {
					   Log.e(TAG, "Listener dead");
					   mListeners.remove(i);
				   } catch (Exception ex) {
					   Log.e(TAG, "Listener failed", ex);
				   }
			   }
			}
		}
	};

    public ScanService(Context context) {
        mContext = context;
		try {
			Log.d(TAG, "zll ScanService");
            mIScanService = IScanService.getService();
			mIScanService.setScanServiceCallback(mScanServiceCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
            mIScanService = null;
        }
    }
	
	@Override
	public void Open(int cameraId, int width, int height, int format){
		Log.d(TAG, "Open");
		try {
			Log.d(TAG, "zll Open");
            mIScanService.open(cameraId, width, height, format);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
        }
	}
	
	@Override
	public void Close(int cameraId){
		Log.d(TAG, "Close");
		try {
			Log.d(TAG, "zll Close");
            mIScanService.close(cameraId);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
        }
	}
	
	@Override
	public void Resume(int cameraId){
		Log.d(TAG, "Resume");
		try {
			Log.d(TAG, "zll Resume");
            mIScanService.resume(cameraId);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
        }
	}
	
	@Override
	public void Suspend(int cameraId){
		Log.d(TAG, "Suspend");
		try {
			Log.d(TAG, "zll Suspend");
            mIScanService.suspend(cameraId);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
        }
	}
	
	@Override
	public void Capture(int cameraId){
		Log.d(TAG, "Capture");
		try {
			Log.d(TAG, "zll Capture");
            mIScanService.capture(cameraId);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
        }
	}
	
	@Override
	public void SetParameters(int cameraId,int type,int value){
		Log.d(TAG, "SetParameters");
		try {
			Log.d(TAG, "zll SetParameters");
            mIScanService.setParameters(cameraId, type, value);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
        }
	}
	
	@Override
	public void MoveFocus(int cameraId,float value){
		Log.d(TAG, "MoveFocus");
		try {
			Log.d(TAG, "zll MoveFocus");
            mIScanService.move_focus(cameraId, value);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
        }
	}
	
	@Override
	public void registerListener(IMeigScanServiceListener listener){
		Log.d(TAG, "registerListener");
		synchronized (mListeners) {
            ScanServiceBinderListener bl = new ScanServiceBinderListener(listener);
            try {
				Log.d(TAG, "registerListener 1");
                listener.asBinder().linkToDeath(bl, 0);
                mListeners.add(bl);
            } catch (RemoteException rex) {
                Log.e(TAG, "Failed to link to listener death");
            }
        }
	}
	
	@Override
	public void unregisterListener(IMeigScanServiceListener listener){
		Log.d(TAG, "unregisterListener");
		synchronized (mListeners) {
			Log.d(TAG, "unregisterListener 1");
            for (ScanServiceBinderListener bl : mListeners) {
                if (bl.mListener == listener) {
					Log.d(TAG, "unregisterListener 2");
                    mListeners.remove(mListeners.indexOf(bl));
                    listener.asBinder().unlinkToDeath(bl, 0);
                    return;
                }
            }
        }
	}
 }
