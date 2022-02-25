package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.camera2.CameraManager;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.camera.Camera2Proxy;
import com.meigsmart.meigrs32.camera.Camera2SurfaceView;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.nio.ByteBuffer;

import butterknife.BindView;


public class ThreeM_CameraAutoActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private ThreeM_CameraAutoActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    @BindView(R.id.camera_view)
    public Camera2SurfaceView mCameraPreview = null;

    @BindView(R.id.preview)
    public Button previewButton;
    @BindView(R.id.btn_retry)
    public Button switchButton;
    @BindView(R.id.take_picture)
    public Button takepicture;
    @BindView(R.id.ImageView)
    public ImageView imageView;
    @BindView(R.id.point_info)
    public TextView mTextViewPointInfo;
    String [] camera_id;

    private boolean isCanOpen = false;
    private int mFrontCameraRotationAngle = 0;
    private int mRearCameraRotationAngle = 0;
    private String mFrontCameraRotationAngleConfig = "common_front_camera_rotation_angle";
    private String mRearCameraRotationAngleConfig = "common_rear_camera_rotation_angle";
    private boolean isAutofocusEnable = true;
    private String mRearCameraAutofocusKey = "common_rear_camera_autofocus_bool";
    private final String TAG = ThreeM_CameraAutoActivity.class.getSimpleName();
    private Camera2Proxy mCameraProxy;
    private CameraManager mCameraManager;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_three3m_camera;
    }

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        LogUtil.d(mName);
        if (MyApplication.RuninTestNAME.equals(mFatherName)) {
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
            takepicture.setVisibility(View.GONE);
            previewButton.setVisibility(View.GONE);
        }

        mConfigResult = getResources().getInteger(R.integer.rear_camera_auto_default_config_standard_result);
        if (MyApplication.RuninTestNAME.equals(mFatherName)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        String tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mFrontCameraRotationAngleConfig);
        if (tmpStr.length() != 0) {
            mFrontCameraRotationAngle = Integer.parseInt(tmpStr);
        }
        mFrontCameraRotationAngle = getResources().getInteger(R.integer.front_camera_auto_deault_config_roration_angle);

        tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mRearCameraRotationAngleConfig);
        if (tmpStr.length() != 0) {
            mRearCameraRotationAngle = Integer.parseInt(tmpStr);
        } else
            mRearCameraRotationAngle = getResources().getInteger(R.integer.rear_camera_auto_deault_config_roration_angle);
        LogUtil.d("mRearCameraRotationAngle:" + mRearCameraRotationAngle);

        tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mRearCameraAutofocusKey);
        if (tmpStr.length() != 0) {
            isAutofocusEnable = Boolean.valueOf(tmpStr);
        } else isAutofocusEnable = true;
        mTitle.setText(R.string.ThreeM_CameraAutoActivity);

        switchButton.setOnClickListener(this);
        System.out.println(Camera.getNumberOfCameras());
        switchButton.setVisibility(View.GONE);
        takepicture.setOnClickListener(this);
        imageView.setVisibility(View.GONE);
        previewButton.setOnClickListener(this);
        previewButton.setEnabled(false);
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            camera_id= mCameraManager.getCameraIdList();
        }catch (Exception e){
        }
        initCamera();
        if (mCameraPreview == null) {
            takepicture.setEnabled(false);
            setTestFailReason(getResources().getString(R.string.camera_msg_no));
            Toast toast = Toast.makeText(ThreeM_CameraAutoActivity.this, R.string.camera_msg_no, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else {
            takepicture.setEnabled(true);
        }
        if(!(camera_id[camera_id.length-1].equals("5"))) {
            takepicture.setEnabled(false);
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if ((mConfigTime == 0) || (MyApplication.RuninTestNAME.equals(mFatherName) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        if (MyApplication.RuninTestNAME.equals(mFatherName)) {
            mHandler.sendEmptyMessageDelayed(1003, 2000);
        }

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if (isCanOpen || mFatherName.equals(MyApplication.RuninTestNAME)) {
                        deInit(mFatherName, SUCCESS);
                    } else {
                        deInit(mFatherName, FAILURE, "camera is not open");
                    }
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    break;
                case 1003:
                    if (mCameraPreview != null) {
                        mCameraProxy.captureStillPicture();
                        Log.d(TAG, "recevie  takePicture:");
                        mHandler.sendEmptyMessage(1001);
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };


    private void initCamera() {
        if (mCameraPreview != null) {
            mCameraProxy = mCameraPreview.getCameraProxy();
        }
        mCameraProxy.startPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraPreview != null) {
            Log.d(TAG, "citapk onDestroy destroy start.");
            mCameraProxy.stopPreview();
            mCameraPreview = null;
        }
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
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
            deInit(mFatherName, FAILURE, getTestFailReason());
        }
        if (v == switchButton) {
        }
        if (v == previewButton) {
            takepicture.setEnabled(true);
            previewButton.setEnabled(false);
            switchButton.setEnabled(true);
            imageView.setVisibility(View.GONE);
            mCameraProxy.startPreview();
        }
        if (v == takepicture) {
            if (mCameraPreview != null) {
                if (mCameraPreview != null) {
                    mCameraProxy.captureStillPicture();
                }
                takepicture.setEnabled(false);
                previewButton.setEnabled(true);
                mSuccess.setVisibility(View.VISIBLE);
                mCameraProxy.setImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                        }
                        image.close();
                    }
                });
                mTextViewPointInfo.setVisibility(View.GONE);
                mTextViewPointInfo.setText("");
            }
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
