package com.android.elotouchservice;

import android.content.Context;
import android.util.Log;
import com.elotouch.library.IEloGpioManager;

public class EloGpioService extends IEloGpioManager.Stub {
    private final Context mContext;

    public EloGpioService(Context context) {
        mContext = context;
    }
	
	@Override
    public boolean setModeToGpio(int gpioNum, int gpioMode) {
        Log.d("add_service_test", "TestService setModeToGpio gpioNum:" + gpioNum + " gpioMode:" + gpioMode);
		return true;
    }
 }
