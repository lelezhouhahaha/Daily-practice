package com.meigsmart.meigrs32.activity;

/*This activity is only written for SLB782(d310)*/

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;



import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;


import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.BatteryVolume;
import com.meigsmart.meigrs32.view.PromptDialog;



import butterknife.BindView;


public class LightActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private LightActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;

    private static final String FILE_RED_LED = "/sys/class/leds/red/brightness";
    private static final String FILE_GREEN_LED = "/sys/class/leds/green/brightness";
    private static final String FILE_BLUE_LED = "/sys/class/leds/blue/brightness";
    private static final String FILE_SCAN_LED = "/sys/class/leds/scan/brightness";
    private static final String FILE_RSCAN_LED = "/sys/class/leds/red_scan/brightness";
    private static final String FILE_GSCAN_LED = "/sys/class/leds/green_scan/brightness";

    private static final int STATE_FLASH_LED_ON = 0;
    private static final int STATE_FLASH_LED_OFF = 1;
    private static final int STATE_SCAN_LED_ON = 2;
    private static final int STATE_SCAN_LED_OFF = 3;
    private static final int STATE_RED_LED_ON = 4;
    private static final int STATE_RED_LED_OFF = 5;
    private static final int STATE_GREEN_LED_ON = 6;
    private static final int STATE_GREEN_LED_OFF = 7;
    private static final int STATE_BLUE_LED_ON = 8;
    private static final int STATE_BLUE_LED_OFF = 9;
    private static final int STATE_RSCAN_LED_ON = 10;
    private static final int STATE_RSCAN_LED_OFF = 11;
    private static final int STATE_GSCAN_LED_ON = 12;
    private static final int STATE_GSCAN_LED_OFF = 13;
    private static final int STATE_LED_COUNT = 14;
    private CameraManager mCameraManager;
    private int mIndex;
    private TextView mColorView;

    private Handler mHandler;
    private Runnable mLedRunnable = new Runnable() {
        @Override
        public void run() {
            testLed();
            //mHandler.postDelayed(mLedRunnable, 1000);
        }
    };


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_light;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mSuccess.setVisibility(View.GONE);
        mTitle.setText(R.string.run_in_led);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
		addData(mFatherName,super.mName);

        //start test
        mColorView = (TextView) findViewById(R.id.color);//
        mHandler = new Handler();

    }

    private void testLed() {
        if (mIndex == STATE_SCAN_LED_ON || mIndex == STATE_SCAN_LED_OFF) {
            //d310*  device ,has no scan led
            mIndex++;
            mHandler.postDelayed(mLedRunnable, 1);
            return;
        }
        switch (mIndex) {
            case STATE_RED_LED_ON:
                setLight(FILE_RED_LED, 255);
                showMessage(R.string.test_light_red);
                mColorView.setBackgroundColor(Color.RED);
                break;
            case STATE_RED_LED_OFF:
                setLight(FILE_RED_LED, 0);
                showMessage(R.string.test_light_red);
                mColorView.setBackgroundColor(Color.BLACK);
                break;
            case STATE_GREEN_LED_ON:
                setLight(FILE_GREEN_LED, 255);
                showMessage(R.string.test_light_green);
                mColorView.setBackgroundColor(Color.GREEN);
                break;
            case STATE_GREEN_LED_OFF:
                setLight(FILE_GREEN_LED, 0);
                showMessage(R.string.test_light_green);
                mColorView.setBackgroundColor(Color.BLACK);
                break;
            case STATE_BLUE_LED_ON:
                setLight(FILE_BLUE_LED, 255);
                showMessage(R.string.test_light_blue);
                mColorView.setBackgroundColor(Color.BLUE);
                break;
            case STATE_BLUE_LED_OFF:
                setLight(FILE_BLUE_LED, 0);
                showMessage(R.string.test_light_blue);
                mColorView.setBackgroundColor(Color.BLACK);
                break;
            case STATE_SCAN_LED_ON:
                setLight(FILE_SCAN_LED, 255);
                showMessage(R.string.test_light_scan);
                mColorView.setBackgroundColor(Color.RED);
                break;
            case STATE_SCAN_LED_OFF:
                setLight(FILE_SCAN_LED, 0);
                showMessage(R.string.test_light_scan);
                mColorView.setBackgroundColor(Color.BLACK);
                break;
            case STATE_RSCAN_LED_ON:
                setLight(FILE_RSCAN_LED, 255);
                showMessage(R.string.test_light_rscan);
                mColorView.setBackgroundColor(Color.RED);
                break;
            case STATE_RSCAN_LED_OFF:
                setLight(FILE_RSCAN_LED, 0);
                showMessage(R.string.test_light_rscan);
                mColorView.setBackgroundColor(Color.BLACK);
                break;
            case STATE_GSCAN_LED_ON:
                setLight(FILE_GSCAN_LED, 255);
                showMessage(R.string.test_light_gscan);
                mColorView.setBackgroundColor(Color.GREEN);
                break;
            case STATE_GSCAN_LED_OFF:
                setLight(FILE_GSCAN_LED, 0);
                showMessage(R.string.test_light_gscan);
                mColorView.setBackgroundColor(Color.BLACK);
                break;
            case STATE_FLASH_LED_ON:
                setFlash(true);
                showMessage(R.string.test_light_flash);
                mColorView.setBackgroundColor(Color.rgb(240, 240, 240));
                break;
            case STATE_FLASH_LED_OFF:
                setFlash(false);
                showMessage(R.string.test_light_flash);
                mColorView.setBackgroundColor(Color.BLACK);
                mSuccess.setVisibility(View.VISIBLE);
                break;
        }
        mIndex++;
        mHandler.postDelayed(mLedRunnable, 1000);
        if (mIndex >= STATE_LED_COUNT)
            mIndex = 0;
    }

    private void showMessage(int message){
        String message_show = mContext.getString(message);
        TextView m = (TextView) findViewById(R.id.ledname);
        m.setText(message_show);

    }

    private void setLight(String lightFileName, int value) {
        File deviceFile = new File(lightFileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(deviceFile);
            OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream, "UTF-8");
            writer.write(Integer.toString(value));
            writer.close();
        } catch (Exception e) {
        }
    }

    private void setFlash(boolean on) {
        try {
            //获取CameraManager
            CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            //获取当前手机所有摄像头设备ID
            String[] ids  = mCameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                //查询该摄像头组件是否包含闪光灯
                Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                /*
                 * 获取相机面对的方向
                 * CameraCharacteristics.LENS_FACING_FRONT 前置摄像头
                 * CameraCharacteristics.LENS_FACING_BACK 后只摄像头
                 * CameraCharacteristics.LENS_FACING_EXTERNAL 外部的摄像头
                 */
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null && flashAvailable
                        && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    //打开或关闭手电筒
                    mCameraManager.setTorchMode(id, on ? true : false);
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        mIndex = 0;
        //testLed();
        mHandler.postDelayed(mLedRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 如果LED打开，则关闭之
        if (mIndex % 2 == 1)
            testLed();
        mHandler.removeCallbacks(mLedRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            deInit(mFatherName, result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(mFatherName, result);
        }
    }
}
