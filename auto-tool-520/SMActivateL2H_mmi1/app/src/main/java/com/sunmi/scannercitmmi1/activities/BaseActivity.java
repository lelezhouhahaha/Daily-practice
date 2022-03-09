package com.sunmi.scannercitmmi1.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;

import com.sunmi.scannercitmmi1.R;

import java.util.HashMap;

/**
 * Created by liumin on 2020/02/11.
 */

abstract class BaseActivity extends Activity {

//    private Vibrator mVibrator;
//    private AudioManager mAudioManager;
//    private HashMap<Integer, Integer> mSoundPoolMap = new HashMap();
//    //private SoundPool mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
//    public static final int SUCCESS_SOUND_ID = 0;

    /**
     * 权限列表
     */
    private final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA
    };

    //多个权限请求Code
    private final int REQUEST_CODE_PERMISSIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        //mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //initPlayVideo();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isToRequestPermissions()) {
            requestPermissions();
        }
    }

    public abstract int getLayout();

    public abstract boolean isToRequestPermissions();

    public void requestPermissionsSuccessCallback() {

    }

    private void requestPermissions() {
        PermissionUtils.checkAndRequestMorePermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSIONS, new PermissionUtils.PermissionRequestSuccessCallBack() {
            @Override
            public void onHasPermission() {
                //权限申请回调
                requestPermissionsSuccessCallback();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS:
                PermissionUtils.onRequestMorePermissionsResult(this, PERMISSIONS, new PermissionUtils.PermissionCheckCallBack() {
                    @Override
                    public void onHasPermission() {
                        requestPermissionsSuccessCallback();
                    }

                    @Override
                    public void onUserHasAlreadyTurnedDown(String... permission) {
                    }

                    @Override
                    public void onUserHasAlreadyTurnedDownAndDontAsk(String... permission) {
                    }
                });
        }
    }

//    /**
//     * 初始化音频参数
//     */
//    private void initPlayVideo() {
//        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        this.mSoundPoolMap.put(Integer.valueOf(SUCCESS_SOUND_ID),
//                Integer.valueOf(this.mSoundPool.load(this, R.raw.beep, 1)));
//    }
//
//
//    private void playSound(int audioType, int rate) {
//        float vol = (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 1.0f)
//                / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        Integer soundId = mSoundPoolMap.get(Integer.valueOf(audioType));
//        if ((this.mSoundPool != null) && (this.mSoundPoolMap != null)
//                && (this.mSoundPoolMap.size() > 0)) {
//            this.mSoundPool.play(soundId, vol, vol, 1, 0, rate);
//        }
//    }
//
//    /**
//     * 播放音频
//     */
//    public void playSound() {
//        playSound(0, 1);
//    }

//    public void playVibrator() {
//        if (mVibrator != null) {
//            mVibrator.vibrate(new long[]{20L, 50L}, -1);
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        if (mSoundPool != null) {
//            mSoundPool.release();
//        }
//        if (mSoundPoolMap != null) {
//            mSoundPoolMap.clear();
//        }
    }

}
