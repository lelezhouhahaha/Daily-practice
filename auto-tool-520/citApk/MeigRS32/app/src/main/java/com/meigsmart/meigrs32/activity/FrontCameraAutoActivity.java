package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.hardware.Camera;

import android.os.AsyncTask;
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


public class FrontCameraAutoActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private FrontCameraAutoActivity mContext;
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

	private boolean isCameraRelease = false;

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

        mConfigResult = getResources().getInteger(R.integer.rear_camera_auto_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
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
        LogUtil.d("isAutofocusEnable:" + isAutofocusEnable);
        if (super.mName.equals(getResources().getString(R.string.pcba_rear_camera))) {
            mTitle.setText(R.string.pcba_rear_camera);
            mCameraId = 0;
        } else if (super.mName.equals(getResources().getString(R.string.pcba_camera))) {
            mTitle.setText(R.string.pcba_camera);
            mCameraId = 1;
        }

        switchButton.setOnClickListener(this);
        //System.out.println(Camera.getNumberOfCameras());
        switchButton.setVisibility(View.GONE);
        takepicture.setOnClickListener(this);
        imageView.setVisibility(View.GONE);
        previewButton.setOnClickListener(this);
        previewButton.setEnabled(false);
        initCamera();
        if (!mCameraPreview.isCameraAvalible()) {
            takepicture.setEnabled(false);
            setTestFailReason(getResources().getString(R.string.camera_msg_no));
            Toast toast = Toast.makeText(FrontCameraAutoActivity.this, R.string.camera_msg_no, Toast.LENGTH_LONG);
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
                if ((mConfigTime == 0) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                    return;
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

		if(mFatherName.equals(MyApplication.RuninTestNAME)|| mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
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
                    if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
                        if(mCameraPreview.getPreviewYDataStatus()){
                            Log.d(TAG, "zll 1001 preview data normal!");
                            new CameraReleaseTask(0).execute();
                        }else{
                            Log.d(TAG, "zll 1001 preview data abnormal!");
                            setTestFailReason(TAG + "preview picture abnormal!");
                            new CameraReleaseTask(1).execute();
                        }
                        break;
                    }
                    if (isCanOpen || mFatherName.equals(MyApplication.RuninTestNAME)) {
                        new CameraReleaseTask(0).execute();
                        //deInit(mFatherName, SUCCESS);
                    } else {
                        new CameraReleaseTask(2).execute();
                        //deInit(mFatherName, FAILURE, "camera is not open");
                    }
                    break;
                case 1002:
                    new CameraReleaseTask(3).execute();
                    //deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    break;
                case 1003:
                    /*if (mCameraPreview != null && mCameraPreview.isPreviewing) {
                        boolean takePhoto = mCameraPreview.takePicture();
                        Log.d(TAG, "recevie 1003 takePicture:" + takePhoto);
                        if(takePhoto) {
                            mHandler.sendEmptyMessage(1001);
                        }else {
                            setTestFailReason(getResources().getString(R.string.fail_reason_camera_take_picture));
                        }
                    }*/
                    doTakePictrue();
                    break;
                case 9999:
                    new CameraReleaseTask(4).execute();
                    //deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };


    private void initCamera() {
        if (mCameraPreview != null) {
            Log.d(TAG, "citapk initCamera mCameraPreview != null start.");
            mFrameLayoutPreview.removeView(mCameraPreview);
            mCameraPreview.destroy();
            Log.d(TAG, "citapk initCamera mCameraPreview != null end.");
            mCameraPreview = null;
        }
        Log.d(TAG, "citapk initCamera new CameraPreview_Front start.");
        mCameraPreview = new CameraPreview_Front(this);
        Log.d(TAG, "citapk initCamera new CameraPreview_Front end");
        mFrameLayoutPreview.addView(mCameraPreview);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mCameraPreview && !isCameraRelease ) {
            mFrameLayoutPreview.removeView(mCameraPreview);
            mCameraPreview.destroy();
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
            //deInit(mFatherName, SUCCESS);
            new CameraReleaseTask(0).execute();
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            //deInit(mFatherName, FAILURE, getTestFailReason());
            new CameraReleaseTask(1).execute();
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
            //initCamera();
            mCameraPreview.start();
            imageView.setVisibility(View.GONE);
        }
        if (v == takepicture) {
            /*if (mCameraPreview != null && mCameraPreview.isPreviewing) {//hejianfeng modif for zendao12318
                boolean takePhoto = mCameraPreview.takePicture();
                if (takePhoto) {
                    takepicture.setEnabled(false);
                    previewButton.setEnabled(true);
                    mSuccess.setVisibility(View.VISIBLE);
                    mTextViewPointInfo.setVisibility(View.GONE);
                    mTextViewPointInfo.setText("");
                } else {
                    Toast toast = Toast.makeText(FrontCameraAutoActivity.this, R.string.camera_msg_error, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                }
            }*/
            doTakePictrue();
        }
    }

    private void doTakePictrue(){
        if (mCameraPreview != null && mCameraPreview.isPreviewing) {//hejianfeng modif for zendao12318
            boolean takePhoto = mCameraPreview.takePicture();
            if (takePhoto) {
                takepicture.setEnabled(false);
                previewButton.setEnabled(true);
                mSuccess.setVisibility(View.VISIBLE);
                mTextViewPointInfo.setVisibility(View.GONE);
                mTextViewPointInfo.setText("");
                if(mFatherName.equals(MyApplication.PCBAAutoTestNAME) || mFatherName.equals(MyApplication.RuninTestNAME)){
                    mHandler.sendEmptyMessage(1001);
                }
            } else {
                if(mFatherName.equals(MyApplication.PCBAAutoTestNAME) || mFatherName.equals(MyApplication.RuninTestNAME)){
                    setTestFailReason(getResources().getString(R.string.fail_reason_camera_take_picture));
                    new CameraReleaseTask(1).execute();
                }else {
                    Toast toast = Toast.makeText(FrontCameraAutoActivity.this, R.string.camera_msg_error, Toast.LENGTH_LONG);
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

    class CameraReleaseTask extends AsyncTask {
        private int type;
        CameraReleaseTask(int type){
            this.type = type;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mCameraPreview != null) {
                Log.d(TAG, "citapk onDestroy destroy start.");
                mFrameLayoutPreview.removeView(mCameraPreview);
                takepicture.setVisibility(View.INVISIBLE);
                previewButton.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            if (mCameraPreview != null) {
                Log.d("MMCAM","release start");
                mCameraPreview.destroy();
                Log.d(TAG, "citapk onDestroy destroy end.");
                mCameraPreview = null;
                isCameraRelease = true;
                Log.d("MMCAM","release end");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if(type == 0){
                deInit(mFatherName, SUCCESS);
            }else if(type == 1){
                deInit(mFatherName, FAILURE, getTestFailReason());
            }else if(type == 2){
                deInit(mFatherName, FAILURE, "camera is not open");
            }else if(type == 3){
                deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
            }else if(type == 4){
                deInit(mFatherName, FAILURE, "9999");
            }
        }
    }

}
