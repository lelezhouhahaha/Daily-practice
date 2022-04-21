package com.elotouch.library;
import android.util.Log;
import android.os.RemoteException;
import com.elotouch.library.IEloGpioManager;

/** @hide */
public class EloGpioManager {
    private final IEloGpioManager mService;

	/**@hide*/
    public EloGpioManager(IEloGpioManager mService) {
        this.mService = mService;
	}

	/** 
	* set gpio mode api
	*@hide
	*/
    public boolean setModeToGpio(int gpioNum, int gpioMode) {
		boolean mRet = false;
        try {
			Log.d("add_service_test", "EloGpioManager gpioNum:" + gpioNum + " gpioMode:" + gpioMode);
            mRet = mService.setModeToGpio(gpioNum, gpioMode);
            Log.d("add_service_test", "EloGpioManager mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
    }
	
	
}
