package com.sunmi.scannercitmmi1.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sunmi.scan.DecoderLibrary;
import com.sunmi.scan.Config;
import com.sunmi.scan.ImageScanner;
import com.sunmi.scannercitmmi1.R;
import com.sunmi.scannercitmmi1.utils.Constants;
import com.sunmi.scannercitmmi1.utils.LicenseFileUtils;
import com.sunmi.scannercitmmi1.utils.NetUtils;

import java.util.ArrayList;
import java.util.HashMap;

import static com.sunmi.scannercitmmi1.utils.Constants.SUNMI_CIT_TEST;

public class ScanActivity extends BaseActivity implements View.OnClickListener, DecoderLibrary.SunmiDecoderCallBack,
        DecoderLibrary.ScanTimeoutCallBack {

    private static final String TAG = "do_scan_test";

    private SoundPool mSoundPool = null;
    private Vibrator mVibrator;
    private AudioManager mAudioManager;
    private HashMap<Integer, Integer> mSoundPoolMap = new HashMap();
    public static final int SUCCESS_SOUND_ID = 0;

    TextView tv_barcodeResult;
    Button btn_operation;
    TextView tv_scanner_soft_version;
    TextView tv_decode_library_version;
    TextView tv_scan_type;
    TextView tvScanTitle;

    //解码库对象
    private DecoderLibrary mDecodeLibrary;
    private Handler mHandler = new Handler();

    private int decodeSuccessCount = 0;
    private boolean mScanDeinitFlag = false;
    private boolean isDeinit = false;
    private ScanReleaseTask mScanReleaseTask = null;
    private boolean TEST_START = false;

    DecoderLibrary.CameraType cameraId = DecoderLibrary.CameraType.Camera0;//cameraID选择

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initViews();
        initSoundpool();
        initPlayVideo();
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        String mName = getIntent().getStringExtra("name");
        if(mName != null){
            mScanDeinitFlag = true;
            Log.d(TAG, "onCreate mScanDeinitFlag: " + mScanDeinitFlag);
            tvScanTitle.setText(mName+"");
        }
        //解码库对象
        mDecodeLibrary = DecoderLibrary.sharedObject(this);

        String ScanStartType= getIntent().getStringExtra("ScanStartType");
        if((ScanStartType != null) && ( ScanStartType.equals("auto") || "pcbaautotest".equals(ScanStartType)) ){
            Log.d(TAG, "start auto scan");
            btnState();
        }
    }

    private void initSoundpool() {
        if (mSoundPool != null) {
            mSoundPool.release();
        }
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    }

    private void initViews() {
        tvScanTitle = (TextView)super.findViewById(R.id.tvScanTitle);
        tv_barcodeResult = (TextView)super.findViewById(R.id.tv_barcodeResult);
        tv_scanner_soft_version = (TextView)super.findViewById(R.id.tv_scanner_soft_version);
        tv_decode_library_version = (TextView)super.findViewById(R.id.tv_decode_library_version);
        tv_scan_type = (TextView)super.findViewById(R.id.scanType);
        btn_operation = (Button)super.findViewById(R.id.btn_operation);
        btn_operation.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        //@TODO 调用super.onResume 父类方法,会执行requestPermissionsSuccessCallback()方法,初始化配置,开启扫描
        super.onResume();
    }

    /**
     * 请求权限成功回调
     */
    @Override
    public void requestPermissionsSuccessCallback() {
        initConfig();
        initDecodeLibrary();
        initDecodeVersion();
    }

    private void initDecodeLibrary() {
        mDecodeLibrary.setCameraType(cameraId)//设置cameraID
                .setSunmiCallback(this) //设置解码结果回调
                .setScanTimeoutCallback(this)
                .startCameraPreview(); //设置扫码模式
    }

    @Override
    public void onBackPressed() {
        showBackDialog();
    }

    private void showBackDialog(){
        String message = getResources().getString(R.string.scaning_back);
        AlertDialog mAlertDialog = new AlertDialog.Builder(ScanActivity.this)
                .setMessage(message)
                .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        mAlertDialog.show();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_operation:
                Log.d(TAG, "onClick: btnState() ");
                btn_operation.setEnabled(false);
                btn_operation.setClickable(false);
                tv_barcodeResult.setTextColor(Color.BLACK);
                btnState();
                Log.d(TAG, "onClick: btnState() end");
                break;
        }
    }

    private void btnState() {
        TEST_START = true;
        mDecodeLibrary.startDecoding();
        btn_operation.setText(getResources().getText(R.string.scan));
    }

    private void initConfig() {
        Config.setNumParameter(45, 0);

        int index = mDecodeLibrary.getCameras() - 1;
        if (index < 0) {
            index = 0;
        }
        Log.d(TAG, "initConfig: mDecodeLibrary.getCameras() = " + mDecodeLibrary.getCameras());
        String cameraId = (String) getResources().getStringArray(R.array.cameraId)[index];
        settingCamera(cameraId);
    }

    @Override
    public void receivedDecodedData(final ArrayList<String> resultList, final int decodeTime, final int decodeAllTime, final int decodePicCount) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //声音
                playSound();
                playVibrator();

                decodeSuccessCount++;
                noImageDisplay(resultList, decodeSuccessCount, decodeTime, decodeAllTime, decodePicCount);
            }
        });
    }

    @Override
    public void scanTimeoutCallBack() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuffer sb = new StringBuffer();
                sb.append(getString(R.string.decodeFailed));
                tv_barcodeResult.setTextColor(Color.RED);
                tv_barcodeResult.setText(sb.toString());

                noDisplayOnScanning(true);
                /*if(mScanDeinitFlag) {
                    Intent intent = new Intent();
                    intent.putExtra("results", "scan:timeout fail");
                    setResult(1111, intent);
                    deinit();
                }else{*/
                    btn_operation.setClickable(true);
                    btn_operation.setEnabled(true);
               // }
            }
        });
    }

    private void noDisplayOnScanning(boolean timeout) {
        mDecodeLibrary.stopDecoding();
    }

    private void noImageDisplay(ArrayList<String> resultList, int count, int decodeTime, int decodeAllTime, int decodePicCount) {
        StringBuffer sb = new StringBuffer();
        String decodeResult = "";
        if (resultList.size() == 2) {
            sb.append(getString(R.string.symbologyType) + " " + resultList.get(0) + "\r\n");
            sb.append(getString(R.string.decodeResult) + " " + resultList.get(1) + "\r\n");
            decodeResult = resultList.get(1);
        } else {
            sb.append(resultList.toString() + "\r\n");
            decodeResult = resultList.toString();
        }
        Log.d(TAG, "noImageDisplay: sb.toString " + sb.toString());
        /*if (SUNMI_CIT_TEST.equals(decodeResult)) {
            tv_barcodeResult.setTextColor(Color.GREEN);
            tv_barcodeResult.setText("PASS");
            if(mScanDeinitFlag) {
                Intent intent = new Intent();
                intent.putExtra("results", "scan:true");
                setResult(1111, intent);
                finish();
            }
        } else {
            tv_barcodeResult.setTextColor(Color.RED);
            tv_barcodeResult.setText("FAILED");
            if(mScanDeinitFlag) {
                Intent intent = new Intent();
                intent.putExtra("results", "scan:fail");
                setResult(1111, intent);
                finish();
            }
        }*/
        tv_barcodeResult.setText(decodeResult.toString());

        noDisplayOnScanning(false);

        if(mScanDeinitFlag) {
            Intent intent = new Intent();
            intent.putExtra("results", decodeResult.toString());
            setResult(1111, intent);
            deinit();
        }else{
            btn_operation.setClickable(true);
            btn_operation.setEnabled(true);
        }


    }

    /**
     * 开始扫描
     */
    void startScanning() {
        mDecodeLibrary.startCameraPreview()//开启摄像头预览
                .startDecoding();//开始解码
    }

    /**
     * 停止扫描
     */
    void stopScanning() {
        mDecodeLibrary.stopDecoding() //停止解码
                .stopCameraPreview()//停止摄像头预览
                .closeCamera();//释放相机
    }

    private void deinit(){
        isDeinit = true;
        if(null != mScanReleaseTask) mScanReleaseTask=null;
        mScanReleaseTask = new ScanReleaseTask();
        mScanReleaseTask.execute();
    }

    class ScanReleaseTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d(TAG, "ScanReleaseTask start ");
            stopScanning();
            if (mDecodeLibrary != null) {
                mDecodeLibrary.closeSharedObject();
                mDecodeLibrary = null;
            }
            if (mSoundPool != null) {
                mSoundPool.release();
                mSoundPool = null;
            }
            if (mSoundPoolMap != null) {
                mSoundPoolMap.clear();
                mSoundPoolMap = null;
            }
            TEST_START = false;
            SystemClock.sleep(1000);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            Log.d(TAG, "ScanReleaseTask end ");

            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //停止扫描,释放相机
        Log.d(TAG, "onPause");
        if(!isDeinit)stopScanning();
       // btn_operation.setText(getResources().getString(R.string.scan));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop start");
        mHandler.removeCallbacksAndMessages(null);
        if(!isDeinit) {
            Log.d(TAG, "release not isDeinit start");
            if (mDecodeLibrary != null) {
                mDecodeLibrary.closeSharedObject();
                mDecodeLibrary = null;
            }
            if (mSoundPool != null) {
                mSoundPool.release();
            }
            if (mSoundPoolMap != null) {
                mSoundPoolMap.clear();
            }
            Log.d(TAG, "release not isDeinit end");
        }
        if(null != mScanReleaseTask) {
            mScanReleaseTask.cancel(true);
            mScanReleaseTask = null;
        }
        Log.d(TAG, "onStop end");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy start");
        super.onDestroy();
        Log.d(TAG, "onDestroy end");
    }

    private void settingCamera(CharSequence text) {
        if (getResources().getStringArray(R.array.cameraId)[0].equals(text)) {
            //Camera ID 0
            cameraId = DecoderLibrary.CameraType.Camera0;
        } else if (getResources().getStringArray(R.array.cameraId)[1].equals(text)) {
            //Camera ID 1
            cameraId = DecoderLibrary.CameraType.Camera1;
        } else if (getResources().getStringArray(R.array.cameraId)[2].equals(text)) {
            //Camera ID 2
            cameraId = DecoderLibrary.CameraType.Camera2;
        }
    }

    /**
     * 是否申请权限
     *
     * @return
     */
    @Override
    public boolean isToRequestPermissions() {
        return true;
    }

    @Override
    public int getLayout() {
        return R.layout.activity_scan;
    }

    public static String getVersionName(Context context) throws Exception {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        String version = packInfo.versionName;
        return version;
    }

    private void initDecodeVersion() {
        String versionName = getResources().getString(R.string.decode_version_info_unknow);
        try {
            versionName = getVersionName(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "initDecodeVersion: versionName = " + versionName);
        tv_scanner_soft_version.setText(versionName);
        String scanType = LicenseFileUtils.readScanDeviceInfoFromFile(Constants.SCAN_DEVICE_POINT);
        Log.d(TAG, "ss1100 scan type is " + scanType);
        tv_scan_type.setText("scanType:"+scanType);

        //For decode version not support, first hidden decode version info.
        String decodeVersion = ImageScanner.getLibsunmiscanVersion();
        Log.d(TAG, "initDecodeVersion: decodeVersion = " + decodeVersion);
        tv_decode_library_version.setText(decodeVersion);

    }

    /**
     * 初始化音频参数
     */
    private void initPlayVideo() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        this.mSoundPoolMap.put(Integer.valueOf(SUCCESS_SOUND_ID),
                Integer.valueOf(this.mSoundPool.load(this, R.raw.beep, 1)));
    }


    private void playSound(int audioType, int rate) {
        float vol = (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 1.0f)
                / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Integer soundId = mSoundPoolMap.get(Integer.valueOf(audioType));
        if ((this.mSoundPool != null) && (this.mSoundPoolMap != null)
                && (this.mSoundPoolMap.size() > 0)) {
            this.mSoundPool.play(soundId, vol, vol, 1, 0, rate);
        }
    }

    /**
     * 播放音频
     */
    public void playSound() {
        playSound(0, 1);
    }

    public void playVibrator() {
        if (mVibrator != null) {
            mVibrator.vibrate(new long[]{20L, 50L}, -1);
        }
    }


}
