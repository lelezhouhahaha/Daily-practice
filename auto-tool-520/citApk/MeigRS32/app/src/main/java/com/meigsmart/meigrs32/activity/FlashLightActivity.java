package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.SurfaceView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import android.view.SurfaceHolder.Callback;

import butterknife.BindView;

public class FlashLightActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private FlashLightActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.start)
    public Button mStart;
    @BindView(R.id.stop)
    public Button mStop;
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean isEnable = true;;

    @BindView(R.id.surfaceView)
    public SurfaceView mSurfaceView = null;
    private SurfaceHolder mHolder = null;
    private Camera mCamera = null;
    private int mCameraId = 0;
    /**
     * preview width
     */
    private static final int PREVIEW_WIDTH = 640;
    /**
     * preview height
     */
    private static final int PREVIEW_HEIGHT = 480;
    /**
     * picture width
     */
    private static final int PICTURE_WIDTH = 640;
    /**
     * picture height
     */
    private static final int PICTURE_HEIGHT = 480;
    /**
     * preview width
     */
    private static final int PREVIEW_WIDTH_BACK = 1280;
    /**
     * preview height
     */
    private static final int PREVIEW_HEIGHT_BACK = 720;
    /**
     * picture width
     */
    private static final int PICTURE_WIDTH_BACK = 1280;
    /**
     * picture height
     */
    private static final int PICTURE_HEIGHT_BACK = 720;
    @BindView(R.id.flashlight)
    public TextView flashlight;
    @BindView(R.id.ImageView)
    public ImageView imageView;
    @BindView(R.id.success)
    public  Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;

    private boolean isCanOpen = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_flash_light;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.pcba_flashlight);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mSuccess.setVisibility(View.GONE);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        LogUtil.d(mName);

        mConfigResult = getResources().getInteger(R.integer.rear_camera_auto_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
            mStart.setVisibility(View.INVISIBLE);
            mStop.setVisibility(View.INVISIBLE);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        if (super.mName.equals(getResources().getString(R.string.pcba_rear_camera))){
            mCameraId = 0;
        }

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new SurfaceHolder.Callback() {

            public void surfaceDestroyed(SurfaceHolder holder) {

            }

            public void surfaceCreated(SurfaceHolder holder) {
                startCamera(mCameraId);
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {

            }
        });
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        flashlight.setOnClickListener(this);
        //if (!isCameraFlashEnable()) {
        flashlight.setVisibility(View.GONE);
        //}
        System.out.println(Camera.getNumberOfCameras());
        imageView.setVisibility(View.GONE);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if(mConfigTime <= 0) {
                    mConfigTime = 1;
                    //mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if (isCanOpen){
                        deInit(mFatherName, SUCCESS);
                    }else {
                        deInit(mFatherName, FAILURE,"camera is not open");
                    }
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }

    private void startCamera(int CameraId) {
        Camera.Parameters parameters = null;
        if (mCamera != null)
            return;
        try {
            mCamera = Camera.open(CameraId);
        } catch (RuntimeException e) {
            LogUtil.d("startCamera failed");
            e.printStackTrace();
            mCamera = null;
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
        if (mCamera != null) {
            parameters = mCamera.getParameters();
            parameters.set("orientation", "portrait");
            if (CameraId == 0) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCamera.setParameters(parameters);
            try {
                mCamera.setPreviewDisplay(mHolder);
                LogUtil.d("start preview");
                isCanOpen =  true;
                mSuccess.setVisibility(View.VISIBLE);
                mCamera.startPreview();
                //mDialog.setSuccess();
            } catch (Exception e) {
                mCamera.release();
                mCamera = null;
                sendErrorMsgDelayed(mHandler,e.getMessage());
            }
        }
    }


    private void stopCamera() {
        try {
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void onStop(View view) {
        if(isEnable) {
            isEnable = false;
            stopCamera();
            mStart.setVisibility(View.VISIBLE);
            mStop.setVisibility(View.INVISIBLE);
        }
    }
    public void onStart(View view) {
        if(!isEnable) {
            isEnable = true;
            startCamera(0);
            mStart.setVisibility(View.INVISIBLE);
            mStop.setVisibility(View.VISIBLE);
        }
    }

}
