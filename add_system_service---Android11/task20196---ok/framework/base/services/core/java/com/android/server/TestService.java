package com.android.server;

import android.content.Context;
import android.util.Slog;
import android.mymodule.test.ITestManager;

public class TestService extends ITestManager.Stub {
    private final Context mContext;

    public TestService(Context context) {
        mContext = context;
    }
	
	@Override
    public void testMethod() {
        Slog.i("add_service_test", "TestService testMethod");
    }
 }
