package com.sunmi.scannercitmmi1.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sunmi.scannercitmmi1.R;
import com.sunmi.scannercitmmi1.utils.Constants;
import com.sunmi.scannercitmmi1.utils.LicenseFileUtils;

public class MainActivity extends BaseActivity implements OnClickListener{

    public static final String TAG = "MainActivity";

    private Button btnGotoActive;
    private Button btnGotoScan;
    private TextView tvEmptyInfo;
    private LinearLayout llScan;

    @Override
    public int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    public boolean isToRequestPermissions() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();

        String scanType = LicenseFileUtils.readScanDeviceInfoFromFile(Constants.SCAN_DEVICE_POINT);
        if (!TextUtils.isEmpty(scanType) && scanType.contains("SS110")) {
            Log.d(TAG, "onCreate: is SS1100 scan.");
            tvEmptyInfo.setVisibility(View.GONE);
            llScan.setVisibility(View.VISIBLE);
        } else {
            llScan.setVisibility(View.GONE);
            tvEmptyInfo.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        tvEmptyInfo = (TextView) super.findViewById(R.id.tvEmptyInfo);
        llScan = (LinearLayout) super.findViewById(R.id.llScan);

        btnGotoActive = (Button)super.findViewById(R.id.btnGotoActive);
        btnGotoActive.setOnClickListener(this);

        btnGotoScan = (Button)super.findViewById(R.id.btnGotoScan);
        btnGotoScan.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnGotoActive:
                gotoStartActivity(ActiveActivity.class);
                break;
            case R.id.btnGotoScan:
                gotoStartActivity(ScanActivity.class);
        }
    }

    private void gotoStartActivity(Class<?> cls) {
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName(this, cls);
        intent.setComponent(componentName);
        startActivity(intent);
    }
}
