package com.elotouch.library;

/**
 * System private API for test.
 *
 * {@hide}
 */
interface IEloManager {
	boolean getTriggerStatus();
	boolean setTriggerOn();
	boolean setTriggerOff();
	boolean setCameraAsBarcodeReader();
    boolean setModeToGpio(int gpioNum, int gpioMode);
	boolean setIlluminationLightOn();
	boolean setIlluminationLightOff();
	boolean setRedLedOn();
	boolean setRedLedOff();
	boolean setGreenLedOn();
	boolean setGreenLedOff();
	boolean setBlueLedOn();
	boolean setBlueLedOff();
	boolean setKeyMapping(int originkey, int newkey);
	boolean changePoweronPassword(String password);
	int getBatteryTimeRemaining();
	int getBatteryPercentage();
	int getBatteryHealthStatus();
	long getBatteryCycleCountUsed();
	long getBatteryCycleCountRemaining();
	float getBatteryVoltage();
	int getBatteryCapacity();
	int getBatteryInteralTamperature();
	String getBatterySerialNum();
	boolean lockHomeKey();
	boolean unlockHomeKey();
	boolean lockPowerKey();
	boolean unlockPowerKey();
	boolean setPrimaryScreenDensity(int density);
	int getPrimaryScreenDensity();
	boolean setSecondaryScreenMode(int mode);
	int getSecondaryScreenMode();
	boolean checkDebuggable();
	boolean setDebuggaleEnable();
	boolean setDebuggaleDisable();
}
