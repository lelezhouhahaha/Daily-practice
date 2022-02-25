package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import butterknife.BindView;

public class SunMiRecordActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private SunMiRecordActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.start)
    public Button mStart;
    @BindView(R.id.stop)
    public Button mStop;
    @BindView(R.id.play)
    public Button mPlay;
    @BindView(R.id.record_status)
    public TextView mRecord_status;


    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private int mConfigResult;
    private int mConfigTime = 0;

    private MediaRecorder mMediaRecorder = null;

    private static final String CIT_MIC_MODE = "persist.sys.cit.micmode";
    private static final String CURRENT_MIC_MODE = "1";
    private String MIC_TEST_PERSIST_CONFIG_KEY = "common_mic_test_persist_config_string";
    private String MIC_TEST_PERSIST_CONFIG = "common_mic_test_persist_config";
    private String mMicMode = "";
    private boolean propSet = false;
    private File audioFile;
    private boolean isRecording = false;
    private MediaPlayer mediaPlayer;
    private AudioManager mAudioManager;
    private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private static final String TAG = "SunMiRecordActivity";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_record_sunmi;
    }

    @Override
    protected void initData() {
        mContext = this;
        mMicMode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, MIC_TEST_PERSIST_CONFIG_KEY);
	propSet = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, MIC_TEST_PERSIST_CONFIG).equals("true");
        if(mMicMode.isEmpty())
            mMicMode = CIT_MIC_MODE;
        LogUtil.d("SecondPhoneLoopBackTestActivity mMicMode:" + mMicMode);
	if(!propSet){
            SystemProperties.set(mMicMode, CURRENT_MIC_MODE);  //set main mic mode
	}
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mPlay.setOnClickListener(this);
        mTitle.setText(R.string.pcba_audio_record);

        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(STREAM_TYPE, 15, 0);
        mMediaRecorder = new MediaRecorder();
        mStop.setEnabled(false);
        mPlay.setEnabled(false);
        mConfigResult = getResources().getInteger(R.integer.record_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    StartRecording();
                    mRecord_status.setText(R.string.isRecording);
                    mStart.setEnabled(false);
                    mStop.setEnabled(true);
                    mPlay.setEnabled(false);
                    break;
                case 1002:
                    StopRecording();
                    mRecord_status.setText(R.string.isRecorded);
                    mStop.setEnabled(false);
                    mStart.setEnabled(true);
                    mPlay.setEnabled(true);
                    break;
                case 1003:
                    mPlay.setEnabled(false);
                    mStart.setEnabled(false);
                    mStop.setEnabled(false);
                    mRecord_status.setText(R.string.isPlayingRecord);
                    PlayRecord();
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back:
                if (!mDialog.isShowing()) mDialog.show();
                mDialog.setTitle(super.mName);
                break;
            case R.id.start:
                mHandler.sendEmptyMessage(1001);
                break;
            case R.id.stop:
                mHandler.sendEmptyMessage(1002);
                break;
            case R.id.play:
                mHandler.sendEmptyMessage(1003);
                break;
            case R.id.success:
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                deInit(mFatherName, SUCCESS);
                break;
            case R.id.fail:
                mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                break;
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

    public void StartRecording() {
        try {
            isRecording = true;
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder
                    .setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mMediaRecorder
                    .setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            audioFile = File.createTempFile("record_", ".wav");
            mMediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (Exception e) {
            Log.d(TAG, "StartRecording Exception:"+e.toString());
        }
    }

    public void StopRecording() {
        if (audioFile != null) {
            mMediaRecorder.stop();
        }

    }

    public void PlayRecord() {
        try {
            isRecording = false;
            if (audioFile != null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mediaPlayer.release();
                        mStart.setEnabled(true);
                        mPlay.setEnabled(true);
                        mRecord_status.setText(R.string.record_status);
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                });
            }
        } catch (IOException e) {
        }
    }

}
