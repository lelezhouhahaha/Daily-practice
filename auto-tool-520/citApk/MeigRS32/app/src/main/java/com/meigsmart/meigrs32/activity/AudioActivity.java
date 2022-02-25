package com.meigsmart.meigrs32.activity;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.service.MusicService;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.text.SimpleDateFormat;

import butterknife.BindView;

public class AudioActivity extends BaseActivity implements View.OnClickListener ,
        PromptDialog.OnPromptDialogCallBack, Runnable{
    private AudioActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.image)
    public ImageView mImg;
    @BindView(R.id.musicTime)
    public TextView mCurrTime;
    @BindView(R.id.musicTotal)
    public TextView mTotalTime;
    @BindView(R.id.musicSeekBar)
    public SeekBar mSb;

    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");
    private MusicService musicService;
    public Handler handler = new Handler();
    private ObjectAnimator animator;
    private Intent intentMusic;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mCustomFileName ;

    private String mCustomFilePath ;
    private int mCurrentVolume = 0;
    private AudioManager mAudioManger;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_audio;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_audio);

        isCustomPath = getResources().getBoolean(R.bool.audio_default_config_is_user_custom_path);
        mCustomPath = getResources().getString(R.string.audio_default_config_custom_path);
        mCustomFileName = getResources().getString(R.string.audio_default_config_custom_file_name);
        mCustomFilePath = getCustomFilePath();

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        addData(mFatherName,super.mName);
        mAudioManger = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        bindServiceConnection();

        mConfigResult = getResources().getInteger(R.integer.audio_auto_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult +
                " mConfigTime:" + mConfigTime+
                " mCustomPath:" + mCustomPath+
                " mCustomFileName:"+mCustomFileName+
                " isCustomPath:"+isCustomPath);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (mConfigTime == 0 ||
                        mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    private String getCustomFilePath(){
        if (isCustomPath){
            if (!TextUtils.isEmpty(mCustomPath) && !TextUtils.isEmpty(mCustomFileName)){
                File file = FileUtil.createRootDirectory(mCustomPath);
                File file1 = FileUtil.mkDir(file);
                File f = new File(file1.getPath(),mCustomFileName);
                if (f.exists()){
                    return f.getPath();
                }else{
                    ToastUtil.showBottomShort("the file is not exists");
                    sendErrorMsgDelayed(mHandler,"the file is not exists");
                }
            }else {
                ToastUtil.showBottomShort("the file path is not null");
                sendErrorMsgDelayed(mHandler,"the file path is not null");
            }
        }
        return null;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    deInit(mFatherName, SUCCESS);
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
    protected void onDestroy() {
        handler.removeCallbacks(this);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
        if(mCurrentVolume != 0) {
            mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
            LogUtil.d("citapk onDestroy mCurrentVolume:" + mCurrentVolume);
            int currentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
            LogUtil.d("citapk onDestroy currentVolume:" + currentVolume);
        }
        if (musicService!=null)musicService.stop();
        if (intentMusic!=null)stopService(intentMusic);
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void bindServiceConnection() {
        mCurrentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVoluem = mAudioManger.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        LogUtil.d("citapk maxVoluem:" + maxVoluem);
        mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, maxVoluem, 0);
        intentMusic = new Intent(this, MusicService.class);
        startService(intentMusic);
        bindService(intentMusic, serviceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MyBinder) (service)).getService(isCustomPath,mCustomFilePath);
            handler.post(AudioActivity.this);
            mTotalTime.setText(time.format(musicService.mediaPlayer.getDuration()));
            rotationImg();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

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

    @Override
    public void run() {
        if (musicService.mediaPlayer != null && musicService.mediaPlayer.isPlaying()){
            mCurrTime.setText(time.format(musicService.mediaPlayer.getCurrentPosition()));
            mSb.setProgress(musicService.mediaPlayer.getCurrentPosition());
            mSb.setMax(musicService.mediaPlayer.getDuration());
            mTotalTime.setText(time.format(musicService.mediaPlayer.getDuration()));
            handler.postDelayed(this, 200);
        }else {
            handler.removeCallbacks(this);
            /*unbindService(serviceConnection);
            if (intentMusic!=null)stopService(intentMusic);
             */
            deInit(mFatherName, SUCCESS);
        }
    }

    private void rotationImg(){
        animator = ObjectAnimator.ofFloat(mImg, "rotation", 0f, 360.0f);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
        animator.start();
    }
}
