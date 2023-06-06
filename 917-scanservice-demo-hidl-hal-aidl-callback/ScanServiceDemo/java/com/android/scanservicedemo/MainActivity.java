package com.android.scanservicedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Context mContext;
    Button mCameraButton;
    Button mQuadCameraButton;

    public static final String TAG = "ScanServiceDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        mCameraButton = (Button) findViewById(R.id.cam);
        mQuadCameraButton = (Button) findViewById(R.id.quad_cam);
        mCameraButton.setOnClickListener(this);
        mQuadCameraButton.setOnClickListener(this);
    }

    void StartAcivity(String packagename, String classname) {
        Log.d(TAG, "zll StartAcivity packagename:" + packagename + " classname:" + classname);
        ComponentName componentName = new ComponentName(packagename, classname);
        Intent intent = new Intent();
        intent.setComponent(componentName);
        startActivityForResult(intent, 1000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.cam:
                StartAcivity("com.android.scanservicedemo", "com.android.scanservicedemo.CameraActivity");
                break;
            case R.id.quad_cam:
                StartAcivity("com.android.scanservicedemo", "com.android.scanservicedemo.QuadCameraActivity");
                break;
        }
    }
}
