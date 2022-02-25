package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioSystem;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.IOException;

import butterknife.BindView;

public class HDMITestActivity extends BaseActivity implements View.OnClickListener ,
        PromptDialog.OnPromptDialogCallBack, Runnable, MediaPlayer.OnCompletionListener,MediaPlayer.OnErrorListener,MediaPlayer.OnInfoListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback{
    private HDMITestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.sf)
    public SurfaceView surfaceView;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;


    private String mFatherName = "";
    private MediaPlayer player;
    private SurfaceHolder holder;
    private AudioManager mAudioManger;
    private boolean isHint ;
    private int mCurrentVolume = 0;
    //private boolean mHDMIConnectionStatus = false;
    private String HDMI_AUDIO_STATE_NODE = "";
    private String HDMI_VIDEO_STATE_NODE = "";
    private String HDMI_AUDIO_STATE_NODE_KEY = "common_hdmi_audio_state_node";
    private String HDMI_VIDEO_STATE_NODE_KEY = "common_hdmi_video_state_node";

    private String mAudioNodeState = "";
    private String mVideoNodeState = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_hdmi;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.HDMITestActivity);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);

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

        HDMI_AUDIO_STATE_NODE = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HDMI_AUDIO_STATE_NODE_KEY);
        HDMI_VIDEO_STATE_NODE = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, HDMI_VIDEO_STATE_NODE_KEY);
        LogUtil.d("citapk hdmi audio node:" + HDMI_AUDIO_STATE_NODE);
        LogUtil.d("citapk hdmi video node:" + HDMI_VIDEO_STATE_NODE);
        if(HDMI_AUDIO_STATE_NODE.isEmpty() || HDMI_VIDEO_STATE_NODE.isEmpty()){
            mHandler.sendEmptyMessageDelayed(1002, 1000);
        }else {
            mHandler.sendEmptyMessageDelayed(1003, 1000);
        }


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
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                    deInit(mFatherName, SUCCESS);
                    break;

                case 1002:
                    if (player!=null){
                        if (player.isPlaying())player.stop();
                        player.release();
                        player = null;
                    }
                    mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                    deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    break;
                case 1003:
                   // mHDMIConnectionStatus = AudioSystem.getDeviceConnectionState(AudioSystem.DEVICE_IN_HDMI,"") == AudioSystem.DEVICE_STATE_AVAILABLE;
                    mAudioNodeState = DataUtil.readLineFromFile(HDMI_AUDIO_STATE_NODE);
                    mVideoNodeState = DataUtil.readLineFromFile(HDMI_VIDEO_STATE_NODE);
                    android.util.Log.i("cit/hdmi",
                            "mAudioNodeState="+mAudioNodeState+"  mVideoNodeState="+mVideoNodeState +"  mVideoNodeState.length()="+mVideoNodeState.length());

                   // android.util.Log.i("cit/hdmi","ConnectionStatus="+AudioSystem.getDeviceConnectionState(AudioSystem.DEVICE_IN_HDMI,"") );

                    if(!mAudioNodeState.isEmpty() && !mVideoNodeState.isEmpty() &&
                            "1".equals(mAudioNodeState) && mVideoNodeState.length() > 1) {
                        LogUtil.d("hdmi state: DEVICE_STATE_AVAILABLE");
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                    mHandler.sendEmptyMessageDelayed(1003, 1000);
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
        if (player!=null){
            if (player.isPlaying())player.stop();
            player.release();
            player = null;
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
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
                mCurrentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVoluem = mAudioManger.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                LogUtil.d("citapk surfaceCreated maxVoluem:" + maxVoluem);
                mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, maxVoluem, 0);
                int currentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
                LogUtil.d("citapk surfaceCreated getCurrentVolume:" + currentVolume);
            } catch (IOException e) {
                e.printStackTrace();
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
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }


    @Override
    public void onClick(View v) {
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void run() {

    }
}
