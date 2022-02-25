package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;

import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
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
import com.meigsmart.meigrs32.camera.CameraPreview_Front;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;


public class FrontCameraAutoActivity2 extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private FrontCameraAutoActivity2 mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
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
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.camera_preview)
    public FrameLayout mFrameLayoutPreview;

    private String mFatherName = "";
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    public CameraPreview_Front mCameraPreview = null;
    private int mCameraId = 0;

    private boolean isCanOpen = false;
    private int mFrontCameraRotationAngle = 0;
    private int mRearCameraRotationAngle = 0;
    private String mFrontCameraRotationAngleConfig = "common_front_camera_rotation_angle";
    private String mRearCameraRotationAngleConfig = "common_rear_camera_rotation_angle";
    private boolean isAutofocusEnable = true;
    private String mRearCameraAutofocusKey = "common_rear_camera_autofocus_bool";
    private final String TAG = FrontCameraAutoActivity.class.getSimpleName();
    private final String Broadcast  ="com.intent.action.meig.takephone";

    @Override
    protected int getLayoutId() {
        boolean isMC520 = "MC520".equals(DataUtil.getDeviceName());
        if(isMC520){
            return R.layout.activity_front_camera_auto_mc520;
        }else {
            return R.layout.activity_front_camera_auto;
        }
    }

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
        if(mFatherName.equals(MyApplication.RuninTestNAME)){
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
            takepicture.setVisibility(View.GONE);
            previewButton.setVisibility(View.GONE);
        }
        switchButton.setVisibility(View.GONE);
        previewButton.setVisibility(View.GONE);
        takepicture.setVisibility(View.GONE);
        registerReceiver(mBroadcastReceiver,new IntentFilter(Broadcast));
        Intent intent = new Intent(Intent.ACTION_MAIN);
        //知道要跳转应用的包名、类名
        ComponentName componentName = new ComponentName("org.codeaurora.snapcam", "com.android.camera.CameraLauncher");
        intent.setComponent(componentName);
        startActivity(intent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        String action;
        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            LogUtil.d("action:" + action);
           if(Broadcast.equals(action)){
               mSuccess.setVisibility(View.VISIBLE);
           }
        }
    };
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
                    if (mCameraPreview != null && mCameraPreview.isPreviewing) {
                        boolean takePhoto = mCameraPreview.takePicture();
                        Log.d(TAG, "recevie 1003 takePicture:" + takePhoto);
                        if(takePhoto) {
                            mHandler.sendEmptyMessage(1001);
                        }else {
                            setTestFailReason(getResources().getString(R.string.fail_reason_camera_take_picture));
                        }
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
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
