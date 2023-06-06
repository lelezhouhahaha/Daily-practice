package com.android.scanservicedemo;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import vendor.scan.hardware.scanservice.V1_0.reg_array;
import vendor.scan.hardware.scanservice.V1_0.IScanService;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener{
    Context mContext;
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
    Spinner mRegDataLengthSpinner;

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
    String mDeviceTypeValue = "0";
    int mFreeBufferValue = 0;
    int mRegDataLengthValue = 1;
    private IScanService mScanService;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_BACK:
                Log.d(TAG, "zll KEYCODE_BACK");
                break;
                //return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_camera);
		try {
            Log.d(TAG, "ScanService");
            mScanService = IScanService.getService();
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
            mScanService = null;
        }
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
        mRegDataLengthSpinner = (Spinner) findViewById(R.id.regDataLength);

        mDeviceTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mDeviceTypeSpinnerContent = CameraActivity.this.getResources().getStringArray(R.array.deviceTypeItem)[position];
                if("ccm".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = "0";
                }else if("scm1".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = "1";
                }else if("scm2".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = "2";
                }else if("scm1/scm2".equals(mDeviceTypeSpinnerContent)){
                    mDeviceTypeValue = "3";
                }
                updateDisplay();
                Log.d(TAG," mDeviceTypeSpinner mDeviceTypeValue:" + mDeviceTypeValue);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mFreeBufferSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFreeBufferSpinnerContent = CameraActivity.this.getResources().getStringArray(R.array.freeBufferItem)[position];
                mFreeBufferValue = Integer.valueOf(mFreeBufferSpinnerContent);
                Log.d(TAG, " mFreeBufferValue:" + mFreeBufferValue);
                try{
					if("ccm".equals(mDeviceTypeSpinnerContent)){
						mScanService.cam_ccm_return_buffer(mFreeBufferValue);
					}else if("scm1".equals(mDeviceTypeSpinnerContent)){
						mScanService.cam_scm1_return_buffer(mFreeBufferValue);
					}else if("scm2".equals(mDeviceTypeSpinnerContent)){
						mScanService.cam_scm2_return_buffer(mFreeBufferValue);
					}
                    //mScanService.cam_ccm_return_buffer(mFreeBufferValue);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception cam_ccm_return_buffer: " + e);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mFlashLampSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFlashLampSpinnerContent = CameraActivity.this.getResources().getStringArray(R.array.flashLampItem)[position];
                int mFlashLampValue = 0;
                if("disable".equals(mFlashLampSpinnerContent)){
                    mFlashLampValue = 0;
                }else if("weak".equals(mFlashLampSpinnerContent)){
                    mFlashLampValue = 1;
                }else if("strong".equals(mFlashLampSpinnerContent)){
                    mFlashLampValue = 2;
                }
				int timeout= 0;
                Log.d(TAG, " mFlashLampValue:" + mFlashLampValue);
                try {
                    mScanService.cam_ccm_flash(mFlashLampValue, timeout);
                }catch (RemoteException e) {
                    Log.e(TAG, "Exception cam_ccm_flash: " + e);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSwitchSizeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSwitchSizeSpinnerContent = CameraActivity.this.getResources().getStringArray(R.array.switchSizeItem)[position];
                Log.d(TAG, " mSwitchSizeSpinnerContent:" + mSwitchSizeSpinnerContent);
                int mSwitchSizeValue = 0;
                if("1M".equals(mSwitchSizeSpinnerContent)){
                    mSwitchSizeValue = 0;
                }else if("13M".equals(mSwitchSizeSpinnerContent)){
                    mSwitchSizeValue = 1;
                }
                Log.d(TAG, " mSwitchSizeValue:" + mSwitchSizeValue);
                try{
				    mScanService.cam_ccm_switch_size(mSwitchSizeValue);
                }catch (RemoteException e) {
                    Log.e(TAG, "Exception cam_ccm_switch_size: " + e);
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mRegDataLengthSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String mRegDataLenghtSpinnerContent = CameraActivity.this.getResources().getStringArray(R.array.regDataLength)[position];
                Log.d(TAG," mRegDataLengthSpinner mRegDataLenghtSpinnerContent:" + mRegDataLenghtSpinnerContent);
                mRegDataLengthValue = Integer.decode(mRegDataLenghtSpinnerContent);
                Log.d(TAG," mRegDataLengthSpinner mRegDataLengthValue:" + mRegDataLengthValue);
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
        Log.d(TAG, " mDeviceTypeValue:" + mDeviceTypeValue);
        if("0".equals(mDeviceTypeValue)){
            Log.d(TAG, " updateDisplay 0");
            mFlashLampLinearlayout.setVisibility(View.VISIBLE);
            mSwitchSizeLinearlayout.setVisibility(View.VISIBLE);
            mSetFocusLinearlayout.setVisibility(View.VISIBLE);
        }else{
            Log.d(TAG, " updateDisplay else");
            mFlashLampLinearlayout.setVisibility(View.GONE);
            mSwitchSizeLinearlayout.setVisibility(View.GONE);
            mSetFocusLinearlayout.setVisibility(View.GONE);
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


    public reg_array createRegSettingConfig(String regAddr, String regData){
        String mRegAddrValue = regAddr;//mRegAddrEditText.getText().toString();
        String mRegDataValue = regData;//mRegDataEditText.getText().toString();
        if(mRegAddrValue.isEmpty()){
            //toast
            Toast.makeText(mContext, mContext.getString(R.string.reg_addr_not_input_message), Toast.LENGTH_LONG).show();
            return null;
        }
        if(mRegDataValue.isEmpty()){
            //toast
            Toast.makeText(mContext, mContext.getString(R.string.reg_data_not_input_message), Toast.LENGTH_LONG).show();
            return null;
        }
        int mRegAddInt = Integer.decode(mRegAddrValue);
        int mRegDataInt = Integer.decode(mRegDataValue);

        if(mRegAddrValue.contains("0x") || mRegAddrValue.contains("0X")){
            mRegAddrValue = mRegAddrValue.substring(2);
            Log.d(TAG, " mRegAddrValue:" + mRegAddrValue + " mRegAddrValue.length():" + mRegAddrValue.length()/2);
        }

        if(mRegDataValue.contains("0x") || mRegDataValue.contains("0X")){
            mRegDataValue = mRegDataValue.substring(2);
            Log.d(TAG, " mRegDataValue:" + mRegDataValue + " mRegDataValue.length():" + mRegDataValue.length()/2);
        }
        byte regAddrBytes[] = hexStr2Bytes(mRegAddrValue);
        byte mRegDataBytes[] = hexStr2Bytes(mRegDataValue);

        for(int i = 0; i < regAddrBytes.length; i++){
            Log.d(TAG, " mRegDataBytes[" + i + "]:<" + regAddrBytes[i] + ">." );
        }

        for(int i = 0; i < mRegDataBytes.length; i++){
            Log.d(TAG, " mRegDataBytes[" + i + "]:<" + mRegDataBytes[i] + ">." );
        }

        int regAddrValueLength = regAddrBytes.length;
        int regDataValueLength = mRegDataBytes.length;
        if( (regAddrValueLength > 4 ) || ( regDataValueLength > 4 ) ){
            //toast
            Toast.makeText(mContext, mContext.getString(R.string.mcu_i2c_write_addr_length_messag), Toast.LENGTH_LONG).show();
            return null;
        }
        int delay = 0;
        int mask = 0;
        reg_array mRegArray = new reg_array();
        mRegArray.reg_addr = mRegAddInt;
        mRegArray.reg_data = mRegDataInt;
        mRegArray.delay = delay;
        mRegArray.data_mask = mask;
        return mRegArray;
    }

    public void writeMcuI2cConfig(){
        if("3".equals(mDeviceTypeValue)){
            Toast.makeText(mContext, mContext.getString(R.string.device_type_not_match_messag), Toast.LENGTH_LONG).show();
            return;
        }
	ArrayList<reg_array> mSettingArray= new ArrayList();
        reg_array mI2cReg = createRegSettingConfig(mRegAddrEditText.getText().toString(), mRegDataEditText.getText().toString());
        mSettingArray.add(mI2cReg);
        reg_array mI2cReg1 = createRegSettingConfig("0x0B", "0x04");
        mSettingArray.add(mI2cReg1);

        try{
            mScanService.cam_scm_mcu_i2c_write( mSettingArray, mSettingArray.size());
        }catch (RemoteException e) {
            Log.e(TAG, "Exception cam_scm_mcu_i2c_write: " + e);
        }
    }

    public void writeI2cConfig(){
        if("3".equals(mDeviceTypeValue)){
            Toast.makeText(mContext, mContext.getString(R.string.device_type_not_match_messag), Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<reg_array> mSettingArray= new ArrayList();
        reg_array mI2cReg = createRegSettingConfig(mRegAddrEditText.getText().toString(), mRegDataEditText.getText().toString());
        mSettingArray.add(mI2cReg);

        byte scmSlaveAddr = 0;
        if("0".equals(mDeviceTypeValue)){
            //ccm interface
            reg_array mI2cReg1 = createRegSettingConfig("0x0200", "0x0001");
            mSettingArray.add(mI2cReg1);
            try{
                mScanService.cam_ccm_i2c_write( mSettingArray, mSettingArray.size());
            }catch (RemoteException e) {
                Log.e(TAG, "Exception cam_ccm_i2c_write: " + e);
            }
        }else if("1".equals(mDeviceTypeValue)){
            //scm1 interface
            reg_array mI2cReg1 = createRegSettingConfig("0x3501", "0x06");
            mSettingArray.add(mI2cReg1);
            scmSlaveAddr = (byte) 0xc0;
            try{
                mScanService.cam_scm_i2c_write(scmSlaveAddr, mSettingArray, mSettingArray.size());
            }catch (RemoteException e) {
                Log.e(TAG, "Exception cam_scm_i2c_write: " + e);
            }
        }else if("2".equals(mDeviceTypeValue)){
            //scm2 interface
            reg_array mI2cReg1 = createRegSettingConfig("0x3501", "0x06");
            mSettingArray.add(mI2cReg1);
            scmSlaveAddr = (byte)0x20;
            try{
                mScanService.cam_scm_i2c_write(scmSlaveAddr, mSettingArray, mSettingArray.size());
            }catch (RemoteException e) {
                Log.e(TAG, "Exception cam_scm_i2c_write: " + e);
            }
        }
    }

    public void readI2cConfig() {
        if("3".equals(mDeviceTypeValue)){
            Toast.makeText(mContext, mContext.getString(R.string.device_type_not_match_messag), Toast.LENGTH_LONG).show();
            return;
        }
        if(mRegDataLengthValue == 0){
            Toast.makeText(mContext, mContext.getString(R.string.reg_data_length_not_select_message), Toast.LENGTH_LONG).show();
            return;
        }
        //reg_addr
        String mRegAddrValue = mRegAddrEditText.getText().toString();
        Log.d(TAG, " readI2cConfig mRegAddrValue:" + mRegAddrValue);
        Log.d(TAG, " readI2cConfig mContext.getString(R.string.reg_addr_not_input_message):" + mContext.getString(R.string.reg_addr_not_input_message));
        if(mRegAddrValue.isEmpty()){
            //toast
            Toast.makeText(mContext, mContext.getString(R.string.reg_addr_not_input_message), Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, " readI2cConfig 1");
        int mRegAddInt = Integer.decode(mRegAddrValue);

        if(mRegAddrValue.contains("0x") || mRegAddrValue.contains("0X")){
            mRegAddrValue = mRegAddrValue.substring(2);
            Log.d(TAG, " mRegAddrValue:" + mRegAddrValue + " mRegAddrValue.length():" + mRegAddrValue.length()/2);
        }
        byte regAddrBytes[] = hexStr2Bytes(mRegAddrValue);

        for(int i = 0; i < regAddrBytes.length; i++){
            Log.d(TAG, " mRegDataBytes[" + i + "]:<" + regAddrBytes[i] + ">." );
        }
        int regAddrValueLength = regAddrBytes.length;
        int regDataValueLength = 0;
        if( regAddrValueLength > 4 ){
            //toast
            Toast.makeText(mContext, mContext.getString(R.string.reg_addr_length_too_long_message), Toast.LENGTH_LONG).show();
            return;
        }

        int mI2cReadResultValue = 0;
        String mSerialNumber = "";
        String mScmFwversion = "";
        byte scmSlaveAddr = 0;
        if("0".equals(mDeviceTypeValue)){
            //ccm interface
            try{
                mI2cReadResultValue = mScanService.cam_ccm_i2c_read(mRegAddInt, regAddrValueLength, mRegDataLengthValue);
            }catch (RemoteException e) {
                Log.e(TAG, "Exception cam_ccm_i2c_read: " + e);
            }
            try{
                mSerialNumber = mScanService.cam_ccm_serial_number_read();
            }catch (RemoteException e) {
                Log.e(TAG, "Exception cam_ccm_serial_number_read: " + e);
            }
        }else if("1".equals(mDeviceTypeValue)){
            //scm1 interface
            scmSlaveAddr = (byte) 0xc0;
            try{
                mI2cReadResultValue = mScanService.cam_scm_i2c_read(scmSlaveAddr, mRegAddInt, regAddrValueLength, mRegDataLengthValue);
            }catch (RemoteException e) {
                Log.e(TAG, "Exception cam_scm_i2c_read: " + e);
            }
        }else if("2".equals(mDeviceTypeValue)){
            //scm2 interface
            scmSlaveAddr = (byte)0x20;
            try{
                mI2cReadResultValue = mScanService.cam_scm_i2c_read(scmSlaveAddr, mRegAddInt, regAddrValueLength, mRegDataLengthValue);
            }catch (RemoteException e) {
                Log.e(TAG, "Exception cam_scm_i2c_read: " + e);
            }
        }

        if( ( ("1".equals(mDeviceTypeValue)) || ("2".equals(mDeviceTypeValue)))) {
            try {
                mSerialNumber = mScanService.cam_scm_serial_number_read();
            } catch (RemoteException e) {
                Log.e(TAG, "Exception cam_scm_serial_number_read: " + e);
            }
            try {
                mScmFwversion = mScanService.cam_scm_fw_version_read();
            } catch (RemoteException e) {
                Log.e(TAG, "Exception cam_scm_fw_version_read: " + e);
            }
        }

        Log.d(TAG, " mI2cReadResultValue:" + mI2cReadResultValue);
        Log.d(TAG, " mSerialNumber:" + mSerialNumber);
        Log.d(TAG, " mScmFwversion:" + mScmFwversion);

        String mI2cReadDisplay = "I2c Read Result:" + mI2cReadResultValue;
        mI2cReadResultTextView.setText(mI2cReadDisplay);
        mSerialTextView.setText("Serial Number:" + mSerialNumber);
        mDeviceFwVersionTextView.setText("device Fwversion:" + mScmFwversion);

    }

    public void setFocus(){
        String mSetFocusStr = mSetFocusEditText.getText().toString();
        if(mSetFocusStr.isEmpty()){
            Toast.makeText(mContext, mContext.getString(R.string.set_focus_is_empty_message), Toast.LENGTH_LONG).show();
            return;
        }
        int mSetFocusvalue = Integer.decode(mSetFocusStr);
        Log.d(TAG, " setFocus mSetFocusvalue:" + mSetFocusvalue);
        if( (mSetFocusvalue < 0) || ( mSetFocusvalue > 5000 )){
            Toast.makeText(mContext, mContext.getString(R.string.set_focus_message), Toast.LENGTH_LONG).show();
            return;
        }
        try{
			int  ret = 0;
			String mReadContent = "";
            mScanService.cam_ccm_move_Focus(mSetFocusvalue);
			
			ret = mScanService.non_volatail_param_write("testwriteparam");
			Log.d(TAG, "setFocus non_volatail_param_write ret:" + ret);
			mReadContent = mScanService.non_volatail_param_read();
			Log.d(TAG, "setFocus non_volatail_param_read mReadContent:" + mReadContent);
        }catch (RemoteException e) {
            Log.e(TAG, "Exception cam_ccm_move_Focus: " + e);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.cam_open:
				try {
					Log.d(TAG, " ScanService cam_open");
					mScanService.cam_open();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception cam_open: " + e);
				}
                break;
            case R.id.cam_close:
				try {
					Log.d(TAG, " ScanService cam_close");
					mScanService.cam_close();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception cam_close: " + e);
				}
                break;
            case R.id.cam_resume:
				try {
					Log.d(TAG, " ScanService cam_resume");
					mScanService.cam_resume();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception cam_resume: " + e);
				}
                break;
            case R.id.cam_suspend:
				try {
					Log.d(TAG, " ScanService cam_suspend");
					mScanService.cam_suspend();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception cam_suspend: " + e);
				}
                break;
            case R.id.cam_capture:
				try {
					if("0".equals(mDeviceTypeValue)){
						Log.d(TAG, " ScanService cam_ccm_capture ");
						mScanService.cam_ccm_capture();
					}else {
						Log.d(TAG, " ScanService cam_scm_capture ");
						mScanService.cam_scm_capture(Byte.decode(mDeviceTypeValue));
					}
				} catch (RemoteException e) {
					Log.e(TAG, "Exception cam_ccm_capture: " + e);
				}
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
                setFocus();
                break;
        }
    }
}