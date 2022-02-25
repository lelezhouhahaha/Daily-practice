package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.camera.CameraPreview_Back;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;


public class RearCameraCloseShotActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private RearCameraCloseShotActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    @BindView(R.id.camera_preview)
    public FrameLayout mFrameLayoutPreview;
    public CameraPreview_Back mCameraPreview = null;
    private int mCameraId = 0;

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
    private String projectName = "";

    private boolean isCanOpen = false;
    private int mFrontCameraRotationAngle = 0;
    private int mRearCameraRotationAngle = 0;
    private String mFrontCameraRotationAngleConfig = "common_front_camera_rotation_angle";
    private String mRearCameraRotationAngleConfig = "common_rear_camera_rotation_angle";
    private boolean isAutofocusEnable = true;
    private String mRearCameraAutofocusKey = "common_rear_camera_autofocus_bool";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_rear_camera_closeshot;
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
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        LogUtil.d(mName);
        if("MT537".equals(projectName)) {
            if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PCBASignalNAME)) {
                SystemProperties.set("persist.sys.face_select", "false");
            } else {
                SystemProperties.set("persist.sys.face_select", "true");
            }
        }

        mConfigResult = getResources().getInteger(R.integer.rear_camera_auto_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        String tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mFrontCameraRotationAngleConfig);
        if (tmpStr.length() != 0) {
            mFrontCameraRotationAngle = Integer.parseInt(tmpStr);
        } else
            mFrontCameraRotationAngle = getResources().getInteger(R.integer.front_camera_auto_deault_config_roration_angle);
        LogUtil.d("mFrontCameraRotationAngle:" + mFrontCameraRotationAngle);

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
        LogUtil.d("isAutofocusEnable:" + isAutofocusEnable);
        mTitle.setText(super.mName);
        
        switchButton.setOnClickListener(this);
        System.out.println(Camera.getNumberOfCameras());
        switchButton.setVisibility(View.GONE);
        takepicture.setOnClickListener(this);
        imageView.setVisibility(View.GONE);
        previewButton.setOnClickListener(this);
        previewButton.setEnabled(false);
        initCamera();
        if (mCameraPreview.getCameraInstance() == null) {
            takepicture.setEnabled(false);
            Toast toast = Toast.makeText(RearCameraCloseShotActivity.this, R.string.camera_msg_no, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else {
            takepicture.setEnabled(true);
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        //add by wangqinghao zd4338
        //mHandler.sendEmptyMessageDelayed(1100, Const.DELAY_TIME);

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if (isCanOpen || mFatherName.equals(MyApplication.RuninTestNAME)) {//modify by wangxing for bug P_RK95_E-706 run in log show pass


                        deInit(mFatherName, SUCCESS);
                    } else {
                        deInit(mFatherName, FAILURE, "camera is not open");
                    }
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    break;
                case 1100:
                    mCameraPreview.autoFocus();
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };


    private void initCamera() {
        if (mCameraPreview != null) {
            mFrameLayoutPreview.removeView(mCameraPreview);
            mCameraPreview.destroy();
            mCameraPreview = null;
        }
        mCameraPreview = new CameraPreview_Back(this);
        mFrameLayoutPreview.addView(mCameraPreview);

    }


    /*@Override
    protected void onPause() {
        super.onPause();
        mCameraPreview = null;
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1100);
        mHandler.removeMessages(9999);
        if (mCameraPreview != null) {
            mFrameLayoutPreview.removeView(mCameraPreview);
            mCameraPreview.destroy();
            mCameraPreview = null;
        }
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
        if (v == switchButton) {
            mFrameLayoutPreview.removeAllViews();
            mFrameLayoutPreview.addView(mCameraPreview);
        }
        if (v == previewButton) {
            takepicture.setEnabled(true);
            previewButton.setEnabled(false);
            switchButton.setEnabled(true);
            mFrameLayoutPreview.setVisibility(View.VISIBLE);
            initCamera();
            mHandler.sendEmptyMessage(1100);
            imageView.setVisibility(View.GONE);
        }
        if (v == takepicture) {
            if (mCameraPreview != null && mCameraPreview.isPreviewing) {//hejianfeng modif for zendao12318
                boolean takePhoto = mCameraPreview.takePicture();
                if (takePhoto) {
                    takepicture.setEnabled(false);
                    previewButton.setEnabled(true);
                    mSuccess.setVisibility(View.VISIBLE);
                    mTextViewPointInfo.setVisibility(View.GONE);
                    mTextViewPointInfo.setText("");
                } else {
                    Toast toast = Toast.makeText(RearCameraCloseShotActivity.this, R.string.camera_msg_error, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }

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
