package com.elotouch.library;
import android.util.Log;
import android.os.RemoteException;
import com.elotouch.library.IEloManager;

/** @hide */
public class EloManager {
    private final IEloManager mService;
	/**@hide*/
	public final String TAG = "EloManager";

	/**@hide*/
    public EloManager(IEloManager mService) {
        this.mService = mService;
	}

	/**@hide*/
	public boolean getTriggerStatus(){
		boolean mRet = false;
		try {
            mRet = mService.getTriggerStatus();
            Log.d(TAG, "getTriggerStatus mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**@hide*/
	public boolean setTriggerOn(){
		boolean mRet = false;
		try {
            mRet = mService.setTriggerOn();
            Log.d(TAG, "setTriggerOn mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**@hide*/
	public boolean setTriggerOff(){
		boolean mRet = false;
		try {
            mRet = mService.setTriggerOff();
            Log.d(TAG, "setTriggerOff mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* set camera mode is Barcode reader
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setCameraAsBarcodeReader(){
		boolean mRet = false;
		try {
            mRet = mService.setCameraAsBarcodeReader();
            Log.d(TAG, "setCameraAsBarcodeReader mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}
	/** 
	* set gpio mode api
	* @param gpioNum 
	* @param gpioMode 1:input 0:output
	*@hide
	*/
    public boolean setModeToGpio(int gpioNum, int gpioMode) {
		boolean mRet = false;
        try {
			Log.d(TAG, "setModeToGpio gpioNum:" + gpioNum + " gpioMode:" + gpioMode);
            mRet = mService.setModeToGpio(gpioNum, gpioMode);
            Log.d(TAG, "setModeToGpio mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
    }

	/**
	* turn on Illumination light 
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setIlluminationLightOn(){
		boolean mRet = false;
		try {
            mRet = mService.setIlluminationLightOn();
            Log.d(TAG, "setIlluminationLightOn mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* close Illumination light 
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setIlluminationLightOff(){
		boolean mRet = false;
		try {
            mRet = mService.setIlluminationLightOff();
            Log.d(TAG, "setIlluminationLightOff mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* turn on Red LED 
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setRedLedOn(){
		boolean mRet = false;
		try {
            mRet = mService.setRedLedOn();
            Log.d(TAG, "setRedLedOn mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* turn off Red LED 
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setRedLedOff(){
		boolean mRet = false;
		try {
            mRet = mService.setRedLedOff();
            Log.d(TAG, "setRedLedOff mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* turn on Green LED 
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setGreenLedOn(){
		boolean mRet = false;
		try {
            mRet = mService.setGreenLedOn();
            Log.d(TAG, "setGreenLedOn mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* turn off Green LED 
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setGreenLedOff(){
		boolean mRet = false;
		try {
            mRet = mService.setGreenLedOff();
            Log.d(TAG, "setGreenLedOff mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* turn on Blue LED 
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setBlueLedOn(){
		boolean mRet = false;
		try {
            mRet = mService.setBlueLedOn();
            Log.d(TAG, "setBlueLedOn mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* turn off Blue LED
	*@hide
	*@return true:success flase:fail
	*/
	public boolean setBlueLedOff(){
		boolean mRet = false;
		try {
            mRet = mService.setBlueLedOff();
            Log.d(TAG, "setBlueLedOff mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* set key map
	*@hide
	*@param originkey int
	*@param newkey int
	*@return true:success flase:fail
	*/
	public boolean setKeyMapping(int originkey, int newkey){
		boolean mRet = false;
		try {
			Log.d(TAG, "setKeyMapping originkey:" + originkey + " newkey:" + newkey);
            mRet = mService.setKeyMapping(originkey, newkey);
            Log.d(TAG, "setKeyMapping mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* update starting up password
	*@hide
	*@param password String
	*@return true:success flase:fail
	*/
	public boolean changePoweronPassword(String password){
		boolean mRet = false;
		try {
			Log.d(TAG, "changePoweronPassword password:" + password);
            mRet = mService.changePoweronPassword(password);
            Log.d(TAG, "changePoweronPassword mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery remaining time
	*@hide
	*@return remaining time
	*/
	public int getBatteryTimeRemaining(){
		int mRet = 0;
		try {
            mRet = mService.getBatteryTimeRemaining();
            Log.d(TAG, "getBatteryTimeRemaining mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery percentage
	*@hide
	*@return
	*/
	public int getBatteryPercentage(){
		int mRet = 0;
		try {
            mRet = mService.getBatteryPercentage();
            Log.d(TAG, "getBatteryPercentage mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery health status
	*@hide
	*@return Excellent/Good/Bad
	*/
	public int getBatteryHealthStatus(){
		int mRet = 0;
		try {
            mRet = mService.getBatteryHealthStatus();
            Log.d(TAG, "getBatteryHealthStatus mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery cycle count used
	*@hide
	*@return
	*/
	public long getBatteryCycleCountUsed(){
		long mRet = 0;
		try {
            mRet = mService.getBatteryCycleCountUsed();
            Log.d(TAG, "getBatteryCycleCountUsed mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery cycle count remaining
	*@hide
	*@return
	*/
	public long getBatteryCycleCountRemaining(){
		long mRet = 0;
		try {
            mRet = mService.getBatteryCycleCountRemaining();
            Log.d(TAG, "getBatteryCycleCountRemaining mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery voltage
	*@hide
	*@return
	*/
	public float getBatteryVoltage(){
		float mRet = 0.0f;
		try {
            mRet = mService.getBatteryVoltage();
            Log.d(TAG, "getBatteryVoltage mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery capacity
	*@hide
	*@return
	*/
	public int getBatteryCapacity(){
		int mRet = 0;
		try {
            mRet = mService.getBatteryCapacity();
            Log.d(TAG, "getBatteryCapacity mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery tamperature
	*@hide
	*@return
	*/
	public int getBatteryInteralTamperature(){
		int mRet = 0;
		try {
            mRet = mService.getBatteryInteralTamperature();
            Log.d(TAG, "getBatteryInteralTamperature mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get battery Serial Number
	*@hide
	*@return
	*/
	public String getBatterySerialNum(){
		String mRet = "";
		try {
            mRet = mService.getBatterySerialNum();
            Log.d(TAG, "getBatterySerialNum mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* lock HOME key
	*@hide
	*@return true:success false:fail
	*/
	public boolean lockHomeKey(){
		boolean mRet = false;
		try {
            mRet = mService.lockHomeKey();
            Log.d(TAG, "lockHomeKey mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* unlock HOME key
	*@hide
	*@return true:success false:fail
	*/
	public boolean unlockHomeKey(){
		boolean mRet = false;
		try {
            mRet = mService.unlockHomeKey();
            Log.d(TAG, "unlockHomeKey mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* lock POWER key
	*@hide
	*@return true:success false:fail
	*/
	public boolean lockPowerKey(){
		boolean mRet = false;
		try {
            mRet = mService.lockPowerKey();
            Log.d(TAG, "lockPowerKey mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* unlock POWER key
	*@hide
	*@return true:success false:fail
	*/
	public boolean unlockPowerKey(){
		boolean mRet = false;
		try {
            mRet = mService.unlockPowerKey();
            Log.d(TAG, "unlockPowerButton mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* set primary Display density
	*@hide
	*@return true:success false:fail
	*/
	public boolean setPrimaryScreenDensity(int density){
		boolean mRet = false;
		try {
			Log.d(TAG, "setPrimaryScreenDensity density:" + density);
            mRet = mService.setPrimaryScreenDensity(density);
            Log.d(TAG, "setPrimaryScreenDensity mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get primary Display density
	*@hide
	*@return
	*/
	public int getPrimaryScreenDensity(){
		int mRet = 0;
		try {
            mRet = mService.getPrimaryScreenDensity();
            Log.d(TAG, "getPrimaryScreenDensity mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* set secondary Display mode
	*@hide
	*@return true:success false:fail
	*/
	public boolean setSecondaryScreenMode(int mode){
		boolean mRet = false;
		try {
			Log.d(TAG, "setSecondaryScreenMode mode:" + mode);
            mRet = mService.setSecondaryScreenMode(mode);
            Log.d(TAG, "setSecondaryScreenMode mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* get secondary Display mode
	*@hide
	*@return
	*/
	public int getSecondaryScreenMode(){
		int mRet = 0;
		try {
            mRet = mService.getSecondaryScreenMode();
            Log.d(TAG, "getSecondaryScreenMode mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* check USB is debug mode
	*@hide
	*@return true:debug mode false:not debug mode
	*/
	public boolean checkDebuggable(){
		boolean mRet = false;
		try {
            mRet = mService.checkDebuggable();
            Log.d(TAG, "checkDebuggable mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* set USB is debug mode
	*@hide
	*@return true:success false:fail
	*/
	public boolean setDebuggaleEnable(){
		boolean mRet = false;
		try {
            mRet = mService.setDebuggaleEnable();
            Log.d(TAG, "setDebuggaleEnable mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}

	/**
	* set USB is not debug mode
	*@hide
	*@return true:success false:fail
	*/
	public boolean setDebuggaleDisable(){
		boolean mRet = false;
		try {
            mRet = mService.setDebuggaleDisable();
            Log.d(TAG, "setDebuggaleDisable mRet:" + mRet);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
		return mRet;
	}
}
