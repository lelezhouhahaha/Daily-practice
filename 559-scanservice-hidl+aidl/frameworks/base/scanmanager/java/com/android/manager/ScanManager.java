package com.android.manager;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import android.os.RemoteException;
import com.android.manager.IScanManager;
import com.android.manager.ScanServiceListener;
import android.os.IBinder;

/** @hide */
public class ScanManager {
    private final IScanManager mService;
	/**@hide*/
	public final String TAG = "ScanManager";

	/**@hide*/
    public ScanManager(IScanManager mService) {
        this.mService = mService;
	}
	
	/**@hide*/
	public void Open(int cameraId, int width, int height, int format){
		Log.d(TAG, "Open start cameraId" + cameraId + " width:" + width + " height:" + height + " format:" + format);		
		try {
            mService.Open(cameraId, width, height, format);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
	}
	
	/**@hide*/
	public void Close(int CameraId){
		Log.d(TAG, "Close start CameraId" + CameraId);		
		try {
            mService.Close(CameraId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

	}
	/**@hide*/
	public void Resume(int CameraId){
		Log.d(TAG, "Resume start CameraId" + CameraId);		
		try {
            mService.Resume(CameraId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

	}
	/**@hide*/
	public void Suspend(int CameraId){
		Log.d(TAG, "Suspend start CameraId" + CameraId);		
		try {
            mService.Suspend(CameraId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
	}
	/**@hide*/
	public void Capture(int CameraId){
		Log.d(TAG, "Capture start CameraId" + CameraId);		
		try {
            mService.Capture(CameraId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
	}
	/**@hide*/
	public void SetParameters(int CameraId,int type,int value){
		Log.d(TAG, "SetParameters start CameraId" + CameraId + " type:" + type + " value:" + value);		
		try {
            mService.SetParameters(CameraId, type, value);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
	}
	/**@hide*/
	public void MoveFocus(int CameraId,float value){
		Log.d(TAG, "MoveFocus start CameraId" + CameraId + " value:" + value);		
		try {
            mService.MoveFocus(CameraId, value);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
	}
	
	public void registerListener(ScanServiceListener listener){
		Log.d(TAG, "registerListener start ");		
		try {
			Log.d(TAG, "registerListener 1 ");
			if(mScanBinderListener == null){
				Log.d(TAG, "registerListener 2 ");
				mScanBinderListener = new ScanBinderListener();
				mService.registerListener(mScanBinderListener);
			}
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		
		final int size = mListeners.size();
		Log.d(TAG, "registerListener 2 size:" + size);
		for (int i = 0; i < size; i++) {
			ScanServiceListener l = mListeners.get(i);
			if (l == listener) {
				Log.d(TAG, "registerListener 2 i:" + i);
				Log.d(TAG, "listener: " + listener + ", been registered!!");
				return;
			}
		}
		mListeners.add(listener);
	}
	
	public void unregisterListener(ScanServiceListener listener){
		Log.d(TAG, "unregisterListener start ");
		if (listener == null) {
            Log.e(TAG, "listener == null");
            return;
        }
		synchronized (mListeners) {
            final int size = mListeners.size();
            Log.e(TAG, "unregisterListener, size = " + size + ", listener = " + listener);
            for (int i = 0; i < size; i++) {
                ScanServiceListener l = mListeners.get(i);
                Log.e(TAG, "l = " + l);
                if (l == listener) {
                    Log.i(TAG, "mListeners.remove(i); i = " + i);
                    mListeners.remove(i);
                    break;
                }
            }
			if (mListeners.size() == 0 && mScanBinderListener != null) {
                try {
                    Log.i(TAG, "mService.unregisterListener(mScanBinderListener);");
                    mService.unregisterListener(mScanBinderListener);
					mScanBinderListener = null;
                } catch (RemoteException rex) {
                    Log.e(TAG, "Unregister mBinderListener failed");
                    return;
                }
            }
		}
	}
	
	private ArrayList<ScanServiceListener> mListeners = new ArrayList<ScanServiceListener>();
	
    private ScanBinderListener mScanBinderListener;
	
    private class ScanBinderListener extends IMeigScanServiceListener.Stub {
        public void onScanDataNotify(int deviceId) {
            Log.i(TAG, "onScanDataNotify deviceId: " + deviceId);
	
            final int size = mListeners.size();
            Log.i(TAG, "onScanDataNotify, size: " + size);
            for (int i = 0; i < size; i++) {
                    Log.i(TAG, "mListeners.get(" + i + ").onScanDataNotify(deviceId);");
                mListeners.get(i).onScanDataNotify(deviceId);
            }
        }
		
		public void onScanDataNotifyCamera0(int deviceId) {
            Log.i(TAG, "onScanDataNotify deviceId: " + deviceId);
	
            final int size = mListeners.size();
            Log.i(TAG, "onScanDataNotifyCamera0, size: " + size);
            for (int i = 0; i < size; i++) {
                    Log.i(TAG, "mListeners.get(" + i + ").onScanDataNotifyCamera0(deviceId);");
                mListeners.get(i).onScanDataNotifyCamera0(deviceId);
            }
        }
		
        public void onScanDataNotifyCamera1(int deviceId) {
            Log.i(TAG, "onScanDataNotifyCamera1 deviceId: " + deviceId);
	
            final int size = mListeners.size();
            Log.i(TAG, "onScanDataNotifyCamera1, size: " + size);
            for (int i = 0; i < size; i++) {
                    Log.i(TAG, "mListeners.get(" + i + ").onScanDataNotifyCamera1(deviceId);");
                mListeners.get(i).onScanDataNotifyCamera1(deviceId);
            }
        }
		
        public void onScanDataNotifyCamera2(int deviceId) {
            Log.i(TAG, "onScanDataNotifyCamera2 deviceId: " + deviceId);
	
            final int size = mListeners.size();
            Log.i(TAG, "onScanDataNotifyCamera2, size: " + size);
            for (int i = 0; i < size; i++) {
                    Log.i(TAG, "mListeners.get(" + i + ").onScanDataNotifyCamera2(deviceId);");
                mListeners.get(i).onScanDataNotifyCamera2(deviceId);
            }
        }

        public void onScanDataNotifyExposure(long exposuretime,int iso) {
            Log.i(TAG, "onScanDataNotifyExposure exposuretime: " + exposuretime + " iso:" + iso);
	
            final int size = mListeners.size();
            Log.i(TAG, "onScanDataNotifyExposure, size: " + size);
            for (int i = 0; i < size; i++) {
                    Log.i(TAG, "mListeners.get(" + i + ").onScanDataNotifyExposure(deviceId);");
                mListeners.get(i).onScanDataNotifyExposure(exposuretime, iso);
            }
        }
    }
}
