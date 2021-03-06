package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class FlashLight_NewActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private FlashLight_NewActivity mContext;
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
    private boolean isEnable = true;

    @BindView(R.id.surfaceView)
    public SurfaceView mSurfaceView = null;

    @BindView(R.id.flashlight)
    public TextView flashlight;
    @BindView(R.id.ImageView)
    public ImageView imageView;
    @BindView(R.id.success)
    public  Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;

    private boolean isCanOpen = false;
    private  CameraManager mCameraManager;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_flash_light;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.pcba_flashlight);
        mSuccess.setVisibility(View.VISIBLE);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        LogUtil.d(mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
        }else{
            mSuccess.setOnClickListener(this);
            mFail.setOnClickListener(this);
        }

        mConfigResult = getResources().getInteger(R.integer.rear_camera_auto_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
            mStart.setVisibility(View.INVISIBLE);
            mStop.setVisibility(View.INVISIBLE);
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        SetFlashLight(true);
        flashlight.setOnClickListener(this);
        flashlight.setVisibility(View.GONE);
        System.out.println(Camera.getNumberOfCameras());
        imageView.setVisibility(View.GONE);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if( ( mConfigTime == 0 ) && mFatherName.equals(MyApplication.PCBAAutoTestNAME) ){
                    mHandler.sendEmptyMessage(1001);
                    return;
                }
                if(mConfigTime <= 0 || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mConfigTime = 1;
                    mHandler.sendEmptyMessage(1001);
                    return;
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
                    if (isCanOpen || mFatherName.equals(MyApplication.RuninTestNAME)){
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
        SetFlashLight(false);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

   /* @Override
    protected void onPause() {
        super.onPause();
        SetFlashLight(false);
    }*/

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


    public void SetFlashLight(boolean setFlashLight){

        try {
            mCameraManager = (CameraManager) FlashLight_NewActivity.this.getSystemService(Context.CAMERA_SERVICE);
            if(null == getCameraId()){
                Toast.makeText(mContext,"no cameraId available",Toast.LENGTH_SHORT).show();
            }else {
                LogUtil.d("FlashLight_NewActivity getCameraId: " + getCameraId());
                mCameraManager.setTorchMode(getCameraId(), setFlashLight);
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
            Log.d("MM0614","e:"+e.toString());
        }
    }

    private String getCameraId() throws CameraAccessException {
        String[] ids = mCameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable
                    && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }


    public void onStop(View view) {
        if(isEnable) {
            isEnable = false;
            SetFlashLight(false);
            mStart.setVisibility(View.VISIBLE);
            mStop.setVisibility(View.INVISIBLE);
        }
    }
    public void onStart(View view) {
        if(!isEnable) {
            isEnable = true;
            SetFlashLight(true);
            mStart.setVisibility(View.INVISIBLE);
            mStop.setVisibility(View.VISIBLE);
        }
    }

}