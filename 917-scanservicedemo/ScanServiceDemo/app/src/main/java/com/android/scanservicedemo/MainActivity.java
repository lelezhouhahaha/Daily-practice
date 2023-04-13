package com.android.scanservicedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Button mOpenButton;
    Button mCloseButton;
    Button mResumeButton;
    Button mSuspendButton;
    Button mCaptureButton;
    Button mI2cWriteButton;
    Button mI2cReadButton;
    Button mMcuI2cWriteButton;
    Button mSetFocusButton;

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

    EditText mRegAddrEditText;
    EditText mRegDataEditText;
    EditText mSetFocusEditText;

    LinearLayout mFlashLampLinearlayout;
    LinearLayout mSwitchSizeLinearlayout;
    LinearLayout mSetFocusLinearlayout;

    public static final String TAG = "ScanServiceDemo";
    String mDeviceTypeValue = "3";
    int mFreeBufferValue = 0;


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
        mMcuI2cWriteButton = (Button) findViewById(R.id.mcuI2cWrite);
        mMcuI2cWriteButton.setOnClickListener(this);
        mSetFocusButton = (Button) findViewById(R.id.setFocus);
        mSetFocusButton.setOnClickListener(this);


        mFlashLampLinearlayout = (LinearLayout) findViewById(R.id.linearlayout_flashlamp);
        mSwitchSizeLinearlayout = (LinearLayout) findViewById(R.id.linearlayout_switchsize);
        mSetFocusLinearlayout = (LinearLayout) findViewById(R.id.linearlayout_setfocus);

        mRegAddrEditText = (EditText) findViewById(R.id.regAddrEdit);
        mRegDataEditText = (EditText) findViewById(R.id.regDataEdit);
        mSetFocusEditText = (EditText) findViewById(R.id.cam_set_focus);

        mDeviceTypeSpinner = (Spinner) findViewById(R.id.deviceType);
        mFreeBufferSpinner = (Spinner) findViewById(R.id.freeBuffer);
        mFlashLampSpinner = (Spinner) findViewById(R.id.flashLamp);
        mSwitchSizeSpinner = (Spinner) findViewById(R.id.cam_switch_size);

        mDeviceTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mDeviceTypeSpinnerContent = MainActivity.this.getResources().getStringArray(R.array.deviceTypeItem)[position];
                if("ccm".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = "0";
                }else if("scm1".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = "1";
                }else if("scm2".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = "2";
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
                mFreeBufferValue = Integer.valueOf(mFreeBufferSpinnerContent);
                Log.d(TAG, "zll mFreeBufferValue:" + mFreeBufferValue);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mFlashLampSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFlashLampSpinnerContent = MainActivity.this.getResources().getStringArray(R.array.flashLampItem)[position];
                int mFlashLampValue = 0;
                if("disable".equals(mFlashLampSpinnerContent)){
                    mFlashLampValue = 0;
                }else if("weak".equals(mFlashLampSpinnerContent)){
                    mFlashLampValue = 1;
                }else if("strong".equals(mFlashLampSpinnerContent)){
                    mFlashLampValue = 2;
                }
                //
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSwitchSizeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSwitchSizeSpinnerContent = MainActivity.this.getResources().getStringArray(R.array.switchSizeItem)[position];
                int mSwitchSizeValue = 0;
                if("1M".equals(mFlashLampSpinnerContent)){
                    mSwitchSizeValue = 0;
                }else if("16M".equals(mFlashLampSpinnerContent)){
                    mSwitchSizeValue = 1;
                }

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
        Log.d(TAG, "zll mDeviceTypeValue:" + mDeviceTypeValue);
        if("0".equals(mDeviceTypeValue)){
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

    public String hexStr2Str(String hexStr) {
        String vi = "0123456789ABC DEF".trim();
        char[] array = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int temp;
        for (int i = 0; i < bytes.length; i++) {
            char c = array[2 * i];
            temp = vi.indexOf(c) * 16;
            c = array[2 * i + 1];
            temp += vi.indexOf(c);
            bytes[i] = (byte) (temp & 0xFF);
        }
        return new String(bytes);
    }

    public static byte[] hexStr2Bytes(String hexStr) {
        if (TextUtils.isEmpty(hexStr)) {
            return new byte[0];
        }
        int length = hexStr.length() / 2;
        char[] chars = hexStr.toCharArray();
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = (byte) (char2Byte(chars[i * 2]) << 4 | char2Byte(chars[i * 2 + 1]));
        }
        return b;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static int char2Byte(char c) {
        if (c >= 'a') {
            return (c - 'a' + 10) & 0x0f;
        }
        if (c >= 'A') {
            return (c - 'A' + 10) & 0x0f;
        }
        return (c - '0') & 0x0f;
    }

    public void writeMcuI2cConfig(){
        String mRegAddrValue = mRegAddrEditText.getText().toString();
        String mRegDataValue = mRegDataEditText.getText().toString();
        Log.d(TAG, "zll mRegAddrValue:" + mRegAddrValue);
        Log.d(TAG, "zll mRegDataValue:" + mRegDataValue);
        byte mbyte[] = hexStr2Bytes("12");
        Log.d(TAG, "zll mByte.length:" + mbyte.length);
        for(int i = 0; i < mbyte.length; i++){
            Log.d(TAG, "zll mByte[" + i + "]:<" + mbyte[i] + ">." );
        }
        if(mRegAddrValue.isEmpty() || mRegDataValue.isEmpty()){
            //toast
            return;
        }

        if(mRegAddrValue.contains("0x") || mRegAddrValue.contains("0X")){
            mRegAddrValue = mRegAddrValue.substring(2);
            Log.d(TAG, "zll mRegAddrValue:" + mRegAddrValue + " mRegAddrValue.length():" + mRegAddrValue.length()/2);
        }

        if(mRegDataValue.contains("0x") || mRegDataValue.contains("0X")){
            mRegDataValue = mRegDataValue.substring(2);
            Log.d(TAG, "zll mRegDataValue:" + mRegDataValue + " mRegDataValue.length():" + mRegDataValue.length()/2);
        }

        byte regAddrBytes[] = hexStr2Bytes(mRegAddrValue);
        byte mRegDataBytes[] = hexStr2Bytes(mRegDataValue);

        for(int i = 0; i < regAddrBytes.length; i++){
            Log.d(TAG, "zll mRegDataBytes[" + i + "]:<" + regAddrBytes[i] + ">." );
        }

        for(int i = 0; i < mRegDataBytes.length; i++){
            Log.d(TAG, "zll mRegDataBytes[" + i + "]:<" + mRegDataBytes[i] + ">." );
        }

        int regAddrValueLength = regAddrBytes.length;
        int regDataValueLength = mRegDataBytes.length;
        if( (regAddrValueLength > 4 ) || ( regDataValueLength > 4 ) ){
            //toast
            return;
        }
        int delay = 0;
        int mask = 0;
        int dataLength = 1;
        if("0".equals(mDeviceTypeValue)){
            //ccm interface
        }else if("1".equals(mDeviceTypeValue)){
            //scm1 interface
        }else if("2".equals(mDeviceTypeValue)){
            //scm2 interface
        }else {
            //scm1, scm2 interface
        }
        //int mRegAddrIntValue = Integer.decode(mRegAddrValue);
        //int mRegDataIntValue = Integer.decode(mRegDataValue);



        //if(mRegAddrIntValue)
       /* Log.d(TAG, "zll mRegAddrIntValue:" + mRegAddrIntValue);
        Log.d(TAG, "zll mRegDataIntValue:" + mRegDataIntValue);

        Log.d(TAG, "zll mRegAddrIntValue:" + (mRegAddrIntValue>>8));
        Log.d(TAG, "zll mRegDataIntValue:" + (mRegDataIntValue>>8));
        Log.d(TAG, "zll Integer.toBinaryString(mRegDataIntValue):" + Integer.toBinaryString(mRegDataIntValue));
        Log.d(TAG, "zll Integer.toBinaryString(mRegDataIntValue):" + Integer.toBinaryString(mRegDataIntValue));*/



    }

    public void writeI2cConfig(){
        String mRegAddrValue = mRegAddrEditText.getText().toString();
        String mRegDataValue = mRegDataEditText.getText().toString();
        Log.d(TAG, "zll mRegAddrValue:" + mRegAddrValue);
        Log.d(TAG, "zll mRegDataValue:" + mRegDataValue);
        byte mbyte[] = hexStr2Bytes("12");
        Log.d(TAG, "zll mByte.length:" + mbyte.length);
        for(int i = 0; i < mbyte.length; i++){
            Log.d(TAG, "zll mByte[" + i + "]:<" + mbyte[i] + ">." );
        }
        if(mRegAddrValue.isEmpty() || mRegDataValue.isEmpty()){
            //toast
            return;
        }

        if(mRegAddrValue.contains("0x") || mRegAddrValue.contains("0X")){
            mRegAddrValue = mRegAddrValue.substring(2);
            Log.d(TAG, "zll mRegAddrValue:" + mRegAddrValue + " mRegAddrValue.length():" + mRegAddrValue.length()/2);
        }

        if(mRegDataValue.contains("0x") || mRegDataValue.contains("0X")){
            mRegDataValue = mRegDataValue.substring(2);
            Log.d(TAG, "zll mRegDataValue:" + mRegDataValue + " mRegDataValue.length():" + mRegDataValue.length()/2);
        }

        byte regAddrBytes[] = hexStr2Bytes(mRegAddrValue);
        byte mRegDataBytes[] = hexStr2Bytes(mRegDataValue);

        for(int i = 0; i < regAddrBytes.length; i++){
            Log.d(TAG, "zll mRegDataBytes[" + i + "]:<" + regAddrBytes[i] + ">." );
        }

        for(int i = 0; i < mRegDataBytes.length; i++){
            Log.d(TAG, "zll mRegDataBytes[" + i + "]:<" + mRegDataBytes[i] + ">." );
        }

        int regAddrValueLength = regAddrBytes.length;
        int regDataValueLength = mRegDataBytes.length;
        if( (regAddrValueLength > 4 ) || ( regDataValueLength > 4 ) ){
            //toast
            return;
        }
        int delay = 0;
        int mask = 0;
        int dataLength = 1;
        if("0".equals(mDeviceTypeValue)){
            //ccm interface
        }else if("1".equals(mDeviceTypeValue)){
            //scm1 interface
        }else if("2".equals(mDeviceTypeValue)){
            //scm2 interface
        }else {
            //scm1, scm2 interface
        }
        //int mRegAddrIntValue = Integer.decode(mRegAddrValue);
        //int mRegDataIntValue = Integer.decode(mRegDataValue);



        //if(mRegAddrIntValue)
       /* Log.d(TAG, "zll mRegAddrIntValue:" + mRegAddrIntValue);
        Log.d(TAG, "zll mRegDataIntValue:" + mRegDataIntValue);

        Log.d(TAG, "zll mRegAddrIntValue:" + (mRegAddrIntValue>>8));
        Log.d(TAG, "zll mRegDataIntValue:" + (mRegDataIntValue>>8));
        Log.d(TAG, "zll Integer.toBinaryString(mRegDataIntValue):" + Integer.toBinaryString(mRegDataIntValue));
        Log.d(TAG, "zll Integer.toBinaryString(mRegDataIntValue):" + Integer.toBinaryString(mRegDataIntValue));*/



    }

    public void readI2cConfig() {

        //reg_addr
        String mRegAddrValue = mRegAddrEditText.getText().toString();
        if(mRegAddrValue.isEmpty()){
            //toast
            return;
        }

        if(mRegAddrValue.contains("0x") || mRegAddrValue.contains("0X")){
            mRegAddrValue = mRegAddrValue.substring(2);
            Log.d(TAG, "zll mRegAddrValue:" + mRegAddrValue + " mRegAddrValue.length():" + mRegAddrValue.length()/2);
        }

        byte regAddrBytes[] = hexStr2Bytes(mRegAddrValue);

        for(int i = 0; i < regAddrBytes.length; i++){
            Log.d(TAG, "zll mRegDataBytes[" + i + "]:<" + regAddrBytes[i] + ">." );
        }
        int regAddrValueLength = regAddrBytes.length;
        int regDataValueLength = 0;
        if( (regAddrValueLength > 4 ) || ( regDataValueLength > 4 ) ){
            //toast
            return;
        }
        int delay = 0;
        int mask = 0;
        int dataLength = 1;

        if("0".equals(mDeviceTypeValue)){
            //ccm interface
        }else if("1".equals(mDeviceTypeValue)){
            //scm1 interface
        }else if("2".equals(mDeviceTypeValue)){
            //scm2 interface
        }else {
            //scm1, scm2 interface
        }

        int mI2cReadResultValue = 0;
        String mI2cReadDisplay = "I2c Read Result:" + mI2cReadResultValue;
        mI2cReadResultTextView.setText(mI2cReadDisplay);

    }

    public void setFocus(){
        String mSetFocusStr = mSetFocusEditText.getText().toString();
        int mSetFocusvalue = Integer.decode(mSetFocusStr);
        Log.d(TAG, "zll setFocus mSetFocusvalue:" + mSetFocusvalue);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.cam_open:
                break;
            case R.id.cam_close:
                break;
            case R.id.cam_resume:
                break;
            case R.id.cam_suspend:
                break;
            case R.id.cam_capture:
                break;
            case R.id.i2cWrite:
                writeI2cConfig();
                break;
            case R.id.i2cRead:
                readI2cConfig();
                break;
            case R.id.mcuI2cWrite:
                writeMcuI2cConfig();
                break;
            case R.id.setFocus:
                break;
        }
    }
}