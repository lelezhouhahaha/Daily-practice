package com.android.services;

import android.content.Context;
import android.util.Log;
import com.elotouch.library.IEloManager;

public class EloService extends IEloManager.Stub {
    private final Context mContext;
	public final String TAG = "EloService";

    public EloService(Context context) {
        mContext = context;
    }

	@Override
	public boolean getTriggerStatus(){
        Log.d(TAG, " getTriggerStatus");
		return true;
    }

	@Override
	public boolean setTriggerOn(){
        Log.d(TAG, " setTriggerOn");
		return true;
    }

	@Override
	public boolean setTriggerOff(){
        Log.d(TAG, " setTriggerOff");
		return true;
    }

	@Override
	public boolean setCameraAsBarcodeReader(){
        Log.d(TAG, " setCameraAsBarcodeReader");
		return true;
    }

	@Override
    public boolean setModeToGpio(int gpioNum, int gpioMode) {
        Log.d(TAG, " setModeToGpio gpioNum:" + gpioNum + " gpioMode:" + gpioMode);
		return true;
    }

	@Override
	public boolean setIlluminationLightOn(){
        Log.d(TAG, " setIlluminationLightOn");
		return true;
    }

	@Override
	public boolean setIlluminationLightOff(){
        Log.d(TAG, " setIlluminationLightOff");
		return true;
    }

	@Override
	public boolean setRedLedOn(){
        Log.d(TAG, " setRedLedOn");
		return true;
    }

	@Override
	public boolean setRedLedOff(){
        Log.d(TAG, " setRedLedOff");
		return true;
    }

	@Override
	public boolean setGreenLedOn(){
        Log.d(TAG, " setGreenLedOn");
		return true;
    }

	@Override
	public boolean setGreenLedOff(){
        Log.d(TAG, " setGreenLedOff");
		return true;
    }

	@Override
	public boolean setBlueLedOn(){
        Log.d(TAG, " setBlueLedOn");
		return true;
    }

	@Override
	public boolean setBlueLedOff(){
        Log.d(TAG, " setBlueLedOff");
		return true;
    }

	@Override
	public boolean setKeyMapping(int originkey, int newkey){
        Log.d(TAG, " setKeyMapping originkey:" + originkey + " newkey:" + newkey);
		return true;
    }

	@Override
	public boolean changePoweronPassword(String password){
        Log.d(TAG, " changePoweronPassword password:" + password);
		return true;
    }

	@Override
	public int getBatteryTimeRemaining(){
        Log.d(TAG, " getBatteryTimeRemaining");
		return 0;
    }

	@Override
	public int getBatteryPercentage(){
        Log.d(TAG, " getBatteryPercentage");
		return 0;
    }

	@Override
	public int getBatteryHealthStatus(){
        Log.d(TAG, " getBatteryHealthStatus");
		return 0;
    }

	@Override
	public long getBatteryCycleCountUsed(){
        Log.d(TAG, " getBatteryCycleCountUsed");
		return 0;
    }

	@Override
	public long getBatteryCycleCountRemaining(){
        Log.d(TAG, " getBatteryCycleCountRemaining");
		return 0;
    }

	@Override
	public float getBatteryVoltage(){
        Log.d(TAG, " getBatteryVoltage");
		return 0.0f;
    }

	@Override
	public int getBatteryCapacity(){
        Log.d(TAG, " getBatteryCapacity");
		return 0;
    }

	@Override
	public int getBatteryInteralTamperature(){
        Log.d(TAG, " getBatteryInteralTamperature");
		return 0;
    }

	@Override
	public String getBatterySerialNum(){
        Log.d(TAG, " getBatterySerialNum");
		return "";
    }

	@Override
	public boolean lockHomeKey(){
        Log.d(TAG, " lockHomeKey");
		return true;
    }

	@Override
	public boolean unlockHomeKey(){
        Log.d(TAG, " unlockHomeKey");
		return true;
    }

	@Override
	public boolean lockPowerKey(){
        Log.d(TAG, " lockPowerKey");
		return true;
    }

	@Override
	public boolean unlockPowerKey(){
        Log.d(TAG, " unlockPowerKey");
		return true;
    }

	@Override
	public boolean setPrimaryScreenDensity(int density){
        Log.d(TAG, " setPrimaryScreenDensity density: " + density);
		return true;
    }

	@Override
	public int getPrimaryScreenDensity(){
        Log.d(TAG, " getPrimaryScreenDensity");
		return 0;
    }

	@Override
	public boolean setSecondaryScreenMode(int mode){
        Log.d(TAG, " setSecondaryScreenMode mode:" + mode);
		return true;
    }

	@Override
	public int getSecondaryScreenMode(){
        Log.d(TAG, " getSecondaryScreenMode");
		return 0;
    }

	@Override
	public boolean checkDebuggable(){
        Log.d(TAG, " checkDebuggable");
		return true;
    }

	@Override
	public boolean setDebuggaleEnable(){
        Log.d(TAG, " setDebuggaleEnable");
		return true;
    }

	@Override
	public boolean setDebuggaleDisable(){
        Log.d(TAG, " setDebuggaleDisable");
		return true;
    }
 }
