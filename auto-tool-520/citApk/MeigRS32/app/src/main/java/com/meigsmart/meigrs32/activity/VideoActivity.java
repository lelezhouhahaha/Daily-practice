package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.activity.BaseActivity;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;

public class VideoActivity extends BaseActivity implements PromptDialog.OnPromptDialogCallBack,MediaPlayer.OnCompletionListener,MediaPlayer.OnErrorListener,MediaPlayer.OnInfoListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnVideoSizeChangedListener,SurfaceHolder.Callback{
    private VideoActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.sf)
    public SurfaceView surfaceView;

    private String mFatherName = "";
    private static final String TAG = VideoActivity.class.getSimpleName();
    private Runnable mRun;
    private MediaPlayer player;
    private SurfaceHolder holder;
    private AudioManager mAudioManger;
    private boolean isHint ;
    private int mCurrentVolume = 0;
    private int mConfigResult;
    private int mConfigTime = 0;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_video;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.run_in_audio);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
        //为了可以播放视频或者使用Camera预览，我们需要指定其Buffer类型
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //下面开始实例化MediaPlayer对象
        player = new MediaPlayer();
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnInfoListener(this);
        player.setOnPreparedListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnVideoSizeChangedListener(this);
        mAudioManger = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManger.setSpeakerphoneOn(true);
        mConfigResult = getResources().getInteger(R.integer.audio_auto_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                   mHandler.sendEmptyMessage(1001);
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
                    if (player!=null){
                        if (player.isPlaying())player.stop();
                        player.release();
                        player = null;
                    }
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
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
        if(mCurrentVolume != 0) {
            LogUtil.d("citapk onDestroy setCurrentVolume:" + mCurrentVolume);
            mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
            int currentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
            LogUtil.d("citapk onDestroy getCurrentVolume:" + currentVolume);
        }
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isHint){
            isHint = false;
            if (player!=null){
                player.start();
            }
        }
    }
    @Override
    public boolean onInfo(MediaPlayer player, int whatInfo, int extra) {
        switch(whatInfo){
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                break;
        }
        return false;
    }
    @Override
    public boolean onError(MediaPlayer player, int whatError, int extra) {
        switch (whatError) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        surfaceView.setLayoutParams(lp);
        player.start();
    }

    @SuppressLint("NewApi")
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if ( player != null ){
            try {
                player.reset();
                player.setDisplay(holder);
                AssetManager assetMg = this.getApplicationContext().getAssets();
                AssetFileDescriptor fileDescriptor = assetMg.openFd("audio.mp4");
                player.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
                player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.prepareAsync();
                player.setLooping(true);
                mCurrentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVoluem = mAudioManger.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                LogUtil.d("citapk surfaceCreated maxVoluem:" + maxVoluem);
                mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, maxVoluem, 0);
                int currentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
                LogUtil.d("citapk surfaceCreated getCurrentVolume:" + currentVolume);
            } catch (IOException e) {
                e.printStackTrace();
                setTestFailReason(getResources().getString(R.string.fail_reason_video_play));
                Log.d(TAG, getTestFailReason());
                Toast.makeText(this,"error",Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }
    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {

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
}
