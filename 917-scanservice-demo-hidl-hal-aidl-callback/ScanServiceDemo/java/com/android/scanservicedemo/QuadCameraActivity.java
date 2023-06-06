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


public class QuadCameraActivity extends AppCompatActivity implements View.OnClickListener{
    Context mContext;
    Button mQuadOpenButton;
    Button mQuadCloseButton;
    Button mQuadResumeButton;
    Button mQuadSuspendButton;
    Button mQuadCaptureButton;
    Button mQuadI2cWriteButton;
    Button mQuadI2cReadButton;
    Spinner mQuadFreeBufferSpinner;
    Spinner mQuadRegDataLengthSpinner;

    String mQuadFreeBufferSpinnerContent;

    TextView mQuadI2cReadResultTextView;

    EditText mQuadRegAddrEditText;
    EditText mQuadRegDataEditText;

    public static final String TAG = "ScanServiceDemo";
    int mFreeBufferValue = 0;
    int mQuadRegDataLengthValue = 1;
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
        setContentView(R.layout.activity_quad_camera);
		try {
            Log.d(TAG, "ScanService");
            mScanService = IScanService.getService();
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IScanService service: " + e);
            mScanService = null;
        }
        mQuadOpenButton = (Button) findViewById(R.id.quad_cam_open);
        mQuadOpenButton.setOnClickListener(this);
        mQuadCloseButton = (Button) findViewById(R.id.quad_cam_close);
        mQuadCloseButton.setOnClickListener(this);
        mQuadResumeButton = (Button) findViewById(R.id.quad_cam_resume);
        mQuadResumeButton.setOnClickListener(this);
        mQuadSuspendButton = (Button) findViewById(R.id.quad_cam_suspend);
        mQuadSuspendButton.setOnClickListener(this);
        mQuadCaptureButton = (Button) findViewById(R.id.quad_cam_capture);
        mQuadCaptureButton.setOnClickListener(this);
        mQuadI2cWriteButton = (Button) findViewById(R.id.quad_i2cWrite);
        mQuadI2cWriteButton.setOnClickListener(this);
        mQuadI2cReadButton = (Button) findViewById(R.id.quad_i2cRead);
        mQuadI2cReadButton.setOnClickListener(this);

        mQuadRegAddrEditText = (EditText) findViewById(R.id.quad_regAddrEdit);
        mQuadRegDataEditText = (EditText) findViewById(R.id.quad_regDataEdit);

        mQuadFreeBufferSpinner = (Spinner) findViewById(R.id.quad_freeBuffer);
        mQuadRegDataLengthSpinner = (Spinner) findViewById(R.id.quad_regDataLength);

		mQuadI2cReadResultTextView = (TextView) findViewById(R.id.quad_i2c_read_result);

        mQuadFreeBufferSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mQuadFreeBufferSpinnerContent = QuadCameraActivity.this.getResources().getStringArray(R.array.freeBufferItem)[position];
                mFreeBufferValue = Integer.valueOf(mQuadFreeBufferSpinnerContent);
                Log.d(TAG, " mFreeBufferValue:" + mFreeBufferValue);
                try{
                    mScanService.quad_cam_scm_return_buffer(mFreeBufferValue);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception cam_ccm_return_buffer: " + e);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mQuadRegDataLengthSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String mRegDataLenghtSpinnerContent = QuadCameraActivity.this.getResources().getStringArray(R.array.regDataLength)[position];
                Log.d(TAG," mRegDataLengthSpinner mRegDataLenghtSpinnerContent:" + mRegDataLenghtSpinnerContent);
                mQuadRegDataLengthValue = Integer.decode(mRegDataLenghtSpinnerContent);
                Log.d(TAG," mRegDataLengthSpinner mRegDataLengthValue:" + mQuadRegDataLengthValue);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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


    public void writeI2cConfig(){
        ArrayList<reg_array> mSettingArray= new ArrayList();
        reg_array mI2cReg = createRegSettingConfig(mQuadRegAddrEditText.getText().toString(), mQuadRegDataEditText.getText().toString());
        mSettingArray.add(mI2cReg);

        byte quadScmSlaveAddr = 0;
        //quad scm interface
        reg_array mI2cReg1 = createRegSettingConfig("0x00", "0x06");
        mSettingArray.add(mI2cReg1);
        quadScmSlaveAddr = (byte)0x52;
        try{
        	mScanService.quad_cam_scm_i2c_write(quadScmSlaveAddr, 1, 1, mSettingArray, mSettingArray.size());
        }catch (RemoteException e) {
        	Log.e(TAG, "Exception cam_scm_i2c_write: " + e);
        }
		
		ArrayList<reg_array> mSettingArray1= new ArrayList();
        reg_array mI2cReg2 = createRegSettingConfig("0x01", "0x25");
        mSettingArray1.add(mI2cReg2);
        quadScmSlaveAddr = (byte)0x67;
        try{
        	mScanService.quad_cam_scm_i2c_write(quadScmSlaveAddr, 1, 1, mSettingArray1, mSettingArray1.size());
        }catch (RemoteException e) {
        	Log.e(TAG, "Exception cam_scm_i2c_write: " + e);
        }
		
		ArrayList<reg_array> mSettingArray2= new ArrayList();
        reg_array mI2cReg3 = createRegSettingConfig("0x3501", "0x06");
        mSettingArray2.add(mI2cReg3);
        quadScmSlaveAddr = (byte)0xc0;
        try{
        	mScanService.quad_cam_scm_i2c_write(quadScmSlaveAddr, 2, 1, mSettingArray2, mSettingArray2.size());
        }catch (RemoteException e) {
        	Log.e(TAG, "Exception cam_scm_i2c_write: " + e);
        }
    }

    public void readI2cConfig() {
        if(mQuadRegDataLengthValue == 0){
            Toast.makeText(mContext, mContext.getString(R.string.reg_data_length_not_select_message), Toast.LENGTH_LONG).show();
            return;
        }
        //reg_addr
        String mRegAddrValue = mQuadRegAddrEditText.getText().toString();
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
        byte scmSlaveAddr = 0;

            //scm2 interface
            scmSlaveAddr = (byte)0x52;
            try{
                mI2cReadResultValue = mScanService.quad_cam_scm_i2c_read(scmSlaveAddr, mRegAddInt, regAddrValueLength, mQuadRegDataLengthValue);
            }catch (RemoteException e) {
                Log.e(TAG, "Exception cam_scm_i2c_read: " + e);
            }

        Log.d(TAG, " mI2cReadResultValue:" + mI2cReadResultValue);

        String mI2cReadDisplay = "I2c Read Result:" + mI2cReadResultValue;
        mQuadI2cReadResultTextView.setText(mI2cReadDisplay);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.quad_cam_open:
				try {
					Log.d(TAG, " ScanService quad_cam_open");
					mScanService.quad_cam_open();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception quad_cam_open: " + e);
				}
                break;
            case R.id.quad_cam_close:
				try {
					Log.d(TAG, " ScanService quad_cam_close");
					mScanService.quad_cam_close();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception quad_cam_close: " + e);
				}
                break;
            case R.id.quad_cam_resume:
				try {
					Log.d(TAG, " ScanService quad_cam_resume");
					mScanService.quad_cam_resume();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception quad_cam_resume: " + e);
				}
                break;
            case R.id.quad_cam_suspend:
				try {
					Log.d(TAG, " ScanService quad_cam_suspend");
					mScanService.quad_cam_suspend();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception quad_cam_suspend: " + e);
				}
                break;
            case R.id.quad_cam_capture:
				try {
						Log.d(TAG, " ScanService quad_cam_ccm_capture ");
						mScanService.quad_cam_scm_capture();
				} catch (RemoteException e) {
					Log.e(TAG, "Exception quad_cam_ccm_capture: " + e);
				}
                break;
            case R.id.quad_i2cWrite:
                writeI2cConfig();
                break;
            case R.id.quad_i2cRead:
                readI2cConfig();
                break;
        }
    }
}