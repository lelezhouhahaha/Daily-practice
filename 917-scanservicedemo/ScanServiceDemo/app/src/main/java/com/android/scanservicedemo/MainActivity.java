package com.android.scanservicedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Button mOpenButton;
    Button mCloseButton;
    Button mResumeButton;
    Button mSuspendButton;
    Button mCaptureButton;
    //Button mDeviceTypeButton;
    //Button mFreeBufferButton;
    Button mI2cWriteButton;
    Button mI2cReadButton;
    //Button mFlashLampButton;
    //Button mSwitchSizeButton;

    Spinner mDeviceTypeSpinner;
    Spinner mFreeBufferSpinner;
    Spinner mFlashLampSpinner;
    Spinner mSwitchSizeSpinner;
    String mDeviceTypeSpinnerContent;
    String mFreeBufferSpinnerContent;
    String mFlashLampSpinnerContent;
    String mSwitchSizeSpinnerContent;

    TextView mSerialTextView;
    TextView mDeviceFwVersionTextView;
    TextView mI2cReadResultTextView;

    LinearLayout mFlashLampLinearlayout;
    LinearLayout mSwitchSizeLinearlayout;
    LinearLayout mSetFocusLinearlayout;

    public static final String TAG = "ScanServiceDemo";
    char mDeviceTypeValue = '3';


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mOpenButton = (Button) findViewById(R.id.cam_open);
        mOpenButton.setOnClickListener(this);
        mCloseButton = (Button) findViewById(R.id.cam_close);
        mCloseButton.setOnClickListener(this);
        mResumeButton = (Button) findViewById(R.id.cam_resume);
        mResumeButton.setOnClickListener(this);
        mSuspendButton = (Button) findViewById(R.id.cam_suspend);
        mSuspendButton.setOnClickListener(this);
        mCaptureButton = (Button) findViewById(R.id.cam_capture);
        mCaptureButton.setOnClickListener(this);
        mI2cWriteButton = (Button) findViewById(R.id.i2cWrite);
        mI2cWriteButton.setOnClickListener(this);
        mI2cReadButton = (Button) findViewById(R.id.i2cRead);
        mI2cReadButton.setOnClickListener(this);
        mOpenButton = (Button) findViewById(R.id.cam_open);
        mOpenButton.setOnClickListener(this);

        mFlashLampLinearlayout = (LinearLayout) findViewById(R.id.linearlayout_flashlamp);
        mSwitchSizeLinearlayout = (LinearLayout) findViewById(R.id.linearlayout_switchsize);
        mSetFocusLinearlayout = (LinearLayout) findViewById(R.id.linearlayout_setfocus);

        mDeviceTypeSpinner = (Spinner) findViewById(R.id.deviceType);
        mFreeBufferSpinner = (Spinner) findViewById(R.id.freeBuffer);
        mFlashLampSpinner = (Spinner) findViewById(R.id.flashLamp);
        mSwitchSizeSpinner = (Spinner) findViewById(R.id.cam_switch_size);

        mDeviceTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mDeviceTypeSpinnerContent = MainActivity.this.getResources().getStringArray(R.array.deviceTypeItem)[position];
                if("ccm".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = '0';
                }else if("scm1".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = '1';
                }else if("scm2".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = '2';
                }
                updateDisplay();
                Log.d(TAG,"zll mDeviceTypeSpinner mDeviceTypeValue:" + mDeviceTypeValue);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mFreeBufferSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFreeBufferSpinnerContent = MainActivity.this.getResources().getStringArray(R.array.freeBufferItem)[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mFlashLampSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFlashLampSpinnerContent = MainActivity.this.getResources().getStringArray(R.array.flashLampItem)[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSwitchSizeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSwitchSizeSpinnerContent = MainActivity.this.getResources().getStringArray(R.array.switchSizeItem)[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSerialTextView = (TextView) findViewById(R.id.serial);
        mDeviceFwVersionTextView = (TextView) findViewById(R.id.fwVersion);
        mI2cReadResultTextView = (TextView) findViewById(R.id.i2c_read_result);

    }


    public void updateDisplay() {
        String deviceType = String.valueOf(mDeviceTypeValue);
        Log.d(TAG, "zll deviceType:" + deviceType);
        if("0".equals(deviceType)){
            Log.d(TAG, "zll updateDisplay 0");
            mFlashLampLinearlayout.setVisibility(View.VISIBLE);
            mSwitchSizeLinearlayout.setVisibility(View.VISIBLE);
            mSetFocusLinearlayout.setVisibility(View.VISIBLE);
        }else{
            Log.d(TAG, "zll updateDisplay else");
            mFlashLampLinearlayout.setVisibility(View.INVISIBLE);
            mSwitchSizeLinearlayout.setVisibility(View.INVISIBLE);
            mSetFocusLinearlayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
    }
}