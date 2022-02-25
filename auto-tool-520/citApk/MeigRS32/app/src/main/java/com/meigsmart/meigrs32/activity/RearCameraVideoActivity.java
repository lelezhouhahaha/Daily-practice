package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;


public class RearCameraVideoActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private RearCameraVideoActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.camera_info)
    public TextView mCameraInfo;
    @BindView(R.id.camera_vedio)
    public FrameLayout mFrameLayoutPreview;
    @BindView(R.id.btn_start)
    public Button mBtnStart;
    @BindView(R.id.btn_finish)
    public Button mBtnFinish;
    @BindView(R.id.TextureView)
    public TextureView mTextureview;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private String mFatherName = "";
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    private boolean isCanOpen = false;
    private int mFrontCameraRotationAngle = 0;
    private int mRearCameraRotationAngle = 0;
    private String mFrontCameraRotationAngleConfig = "common_front_camera_rotation_angle";
    private String mRearCameraRotationAngleConfig = "common_rear_camera_rotation_angle";
    private boolean isAutofocusEnable = true;
    private String mRearCameraAutofocusKey = "common_rear_camera_autofocus_bool";
    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private Camera.Size mSelectSize;//记录当前选择的分辨率
    private boolean isRecorder = false;//用于判断当前是否在录制视频
    private boolean isTestSuccess = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_camera_video;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mBtnStart.setVisibility(View.VISIBLE);
        mBtnFinish.setVisibility(View.GONE);
        mTitle.setText(R.string.RearCameraVideoActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        LogUtil.d(mName);

        mBtnStart.setOnClickListener(this);
        mBtnFinish.setOnClickListener(this);
        initTextureViewListener();
        initMediaRecorder();

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

    }

    /**
     * 初始化TextureView监听
     */
    private void initTextureViewListener(){
        mTextureview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { //Textureview初始化启用回调
                initCamera();
                initCameraConfig();
                try {
                    mCamera.setPreviewTexture(surface);
                    mCamera.startPreview();
                    mHandler.sendEmptyMessage(1100);//启动对焦
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(mFatherName.equals(MyApplication.RuninTestNAME) && !RuninConfig.isOverTotalRuninTime(mContext)){
                    mBtnStart.setVisibility(View.INVISIBLE);
                    mBtnFinish.setVisibility(View.INVISIBLE);
                    //mFail.setVisibility(View.GONE);
                    startRecorder();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    /**
     * 初始化MediaRecorder
     */
    private void initMediaRecorder(){
        mMediaRecorder = new MediaRecorder();
    }

    /**
     * 选择摄像头
     * @param isFacing true=前摄像头 false=后摄像头
     * @return 摄像id
     */
    private Integer selectCamera(boolean isFacing){
        int cameraCount = Camera.getNumberOfCameras();
//        CameraInfo.CAMERA_FACING_BACK 后摄像头
//        CameraInfo.CAMERA_FACING_FRONT  前摄像头
        int facing = isFacing ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        LogUtil.d("selectCamera: cameraCount="+cameraCount);
        if (cameraCount == 0){
            LogUtil.d("selectCamera: The device does not have a camera ");
            return null;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i=0; i < cameraCount; i++){
            Camera.getCameraInfo(i,info);
            LogUtil.d("selectCamera: facing="+facing);
            //LogUtil.d("selectCamera: info.facing="+info.facing);
            if (info.facing == facing){
                LogUtil.d("selectCamera: i="+i);
                return i;
            }

        }
        return null;

    }

    /**
     * 初始化相机
     */
    private void initCamera(){
        mCamera = Camera.open(selectCamera(false));
        mSelectSize = selectPreviewSize(mCamera.getParameters());


    }

    /**
     * 初始化相机配置
     */
    private void initCameraConfig(){
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);//关闭闪光灯
        parameters.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO); //对焦设置为自动
        parameters.setPreviewSize(mSelectSize.width,mSelectSize.height);//设置预览尺寸
        parameters.setPictureSize(mSelectSize.width,mSelectSize.height);//设置图片尺寸  就拿预览尺寸作为图片尺寸,其实他们基本上是一样的
        parameters.set("orientation", "portrait");//相片方向
        //parameters.set("rotation", 90); //相片镜头角度转90度（默认摄像头是横拍）
        mCamera.setParameters(parameters);//添加参数
        mCamera.setDisplayOrientation(mRearCameraRotationAngle);//设置显示方向

    }

    /**
     * 计算获取预览尺寸
     * @param parameters
     * @return
     */
    private Camera.Size selectPreviewSize(Camera.Parameters parameters){
        List<Camera.Size> previewSizeList =  parameters.getSupportedPreviewSizes();
        if (previewSizeList.size() == 0){
            LogUtil.d("selectPreviewSize: previewSizeList size is 0" );
            return null;

        }

        Camera.Size currentSelectSize = null;
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int deviceWidth =displayMetrics.widthPixels;
        int deviceHeight = displayMetrics.heightPixels;
        LogUtil.d("selectPreviewSize: Rear deviceWidth:" + deviceWidth + " deviceHeight： " + deviceHeight);
        for (int i = 1; i < 41 ; i++){
            for(Camera.Size itemSize : previewSizeList){
//                Log.e(TAG, "selectPreviewSize: itemSize 宽="+itemSize.width+"高"+itemSize.height);
                if (itemSize.height > (deviceHeight - i*5) && itemSize.height < (deviceHeight + i*5)){
                    if (currentSelectSize != null){ //如果之前已经找到一个匹配的宽度
                        if (Math.abs(deviceWidth-itemSize.width) < Math.abs(deviceWidth - currentSelectSize.width)){ //求绝对值算出最接近设备高度的尺寸
                            currentSelectSize = itemSize;
                            continue;
                        }
                    }else {
                        currentSelectSize = itemSize;
                    }

                }
            }
        }
        LogUtil.d("selectPreviewSize: Rear 当前选择的尺寸 宽="+currentSelectSize.width+"高"+currentSelectSize.height);
        return currentSelectSize;
    }

    /**
     * 配置MedioRecorder
     */
    private void configMedioRecorder(){
        File saveRecorderFile = new File(getExternalCacheDir(),"CameraRecorder.mp4");
        if (saveRecorderFile.exists()){
            saveRecorderFile.delete();
        }
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);//设置音频源
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);//设置视频源
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);//设置音频输出格式
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);//设置音频编码格式
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);//设置视频编码格式
        mMediaRecorder.setVideoSize(mSelectSize.width,mSelectSize.height);//设置视频分辨率
        mMediaRecorder.setVideoEncodingBitRate(8*1920*1080);//设置视频的比特率
        mMediaRecorder.setVideoFrameRate(60);//设置视频的帧率
        mMediaRecorder.setOrientationHint(90);//设置视频的角度
        mMediaRecorder.setMaxDuration(60*1000);//设置最大录制时间
        Surface surface = new Surface(mTextureview.getSurfaceTexture());
        mMediaRecorder.setPreviewDisplay(surface);//设置预览
        mMediaRecorder.setOutputFile(saveRecorderFile.getAbsolutePath());//设置文件保存路径
        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() { //录制异常监听
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                try {
                    mCamera.setPreviewTexture(mTextureview.getSurfaceTexture());
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });


    }

    /**
     * 开启录制视频
     */
    private void startRecorder(){
        if (!isRecorder) {//如果不在录制视频
            mCamera.stopPreview();//暂停相机预览
            configMedioRecorder();//再次配置MedioRecorder
            try {
                mMediaRecorder.prepare();//准备录制
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMediaRecorder.start();//开始录制
            mCameraInfo.setText(R.string.start_record);
            isRecorder = true;
        }

    }

    /**
     * 停止录制视频
     */
    private void stopRecorder(){
        if (isRecorder){ //如果在录制视频
            mMediaRecorder.stop();//暂停录制
            mMediaRecorder.reset();//重置,将MediaRecorder调整为空闲状态
            mCameraInfo.setText(R.string.stop_record);
            isRecorder = false;
            mSuccess.setVisibility(View.VISIBLE);
            isTestSuccess = true;
            try {
                mCamera.setPreviewTexture(mTextureview.getSurfaceTexture());//重新设置预览SurfaceTexture
                mCamera.startPreview(); //重新开启相机预览
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    stopRecorder();
                    if (isTestSuccess) {
                        deInit(mFatherName, SUCCESS);
                    } else {
                        deInit(mFatherName, FAILURE, "camera Test fail");
                    }
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    break;
                case 1100:
                    mCamera.autoFocus(new Camera.AutoFocusCallback() { //自动对焦
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {

                        }
                    });
                    mHandler.sendEmptyMessageDelayed(1100,2*1000);//2秒之后在对焦一次,一直重复自动对焦
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaRecorder != null){
            stopRecorder();
            if (isRecorder) {
                mMediaRecorder.stop();
            }
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

        }
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1100);
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
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }

        if(v == mBtnStart) {
            mBtnStart.setVisibility(View.GONE);
            mBtnFinish.setVisibility(View.VISIBLE);
            startRecorder();
        }

        if( v == mBtnFinish ) {
            mBtnFinish.setVisibility(View.GONE);
            mBtnStart.setVisibility(View.VISIBLE);
            stopRecorder();
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
