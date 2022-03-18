package com.meigsmart.meigrs32.activity;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
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
import java.lang.ref.WeakReference;

import butterknife.BindView;

public class PCBAAutoDeputyMicActiviy extends BaseActivity implements PromptDialog.OnPromptDialogCallBack {
    private PCBAAutoDeputyMicActiviy mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.mic)
    public TextView mMIC;
    @BindView(R.id.flag)
    public TextView mFlag;


    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private Handler mHandler;

    private boolean isPlay = true;
    private AudioManager mAudioManager;
    private AudioRecord mRecord = null;
    private AudioTrack mTrack = null;
    private static final String TAG = PCBAAutoDeputyMicActiviy.class.getSimpleName();
    private final int STREAM_TYPE = AudioManager.MODE_NORMAL;
    private final int SAMPLE_RATE = 8000;
    private final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_STEREO;
    private final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_STEREO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    //add by maohaojie on 2019.03.18 for bug 782 497
    public static final String TEST_SECOND_LOOPBACKTEST_MAX_CONFIG_ = "common_second_loopbacktest_max_config";
    private int maxMIC_config = 0;
    private static final String CIT_MIC_MODE = "persist.sys.cit.micmode";
    private String mic_mode = "0";
    private static final String CURRENT_MIC_MODE = "2";
    private int maxMIC = 0;
    private String MIC_TEST_PERSIST_CONFIG_KEY = "common_mic_test_persist_config_string";
    private String MIC_TEST_PERSIST_CONFIG = "common_mic_test_persist_config";
    private String mMicMode = "";
    private boolean propSet = false;
    private boolean isStop = false;
    private int mBufferSize = 0;
    private int mCurrentSystemVolume = 0;
    private int mMode = 0;
    //private RecordRun mRecordRun = null;

    private HandlerThread mHandlerThread = null;
    private MyAudioHandler mAudioHandler = null;

    public  static class MyAudioHandler extends Handler {

        WeakReference<Activity> reference;

        public MyAudioHandler(Looper looper, Activity activity) {
            super(looper, null);
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PCBAAutoDeputyMicActiviy activity = (PCBAAutoDeputyMicActiviy) reference.get();
            switch (msg.what) {
                case 1000:
                    LogUtil.d(TAG,"zll 1000");
                    activity.mMode = activity.mAudioManager.getMode();
                    activity.mAudioManager.setSpeakerphoneOn(true);
                    boolean isheadseton = activity.mAudioManager.isWiredHeadsetOn();

                    //activity.mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                    activity.mAudioManager.setMode(activity.STREAM_TYPE);
                    //int maxVolume = activity.mAudioManager.getStreamMaxVolume(activity.STREAM_TYPE);
                    activity.mAudioManager.setStreamVolume(activity.STREAM_TYPE, 15, 0);
                        //run();
                    if(null != activity.mHandler)
                        activity.mAudioHandler.sendEmptyMessage(1001);
                    break;
                case 1001:
                    LogUtil.d(TAG,"zll 1001");
                    activity.doRecordAndPlay();
                    break;
            }
        }

    }

    @Override
    protected int getLayoutId() {
		LogUtil.d(TAG, "getLayoutId");
		return R.layout.activity_pcba_auto_deputy_mic;
    }

    @Override
    protected void initData() {
        LogUtil.d(TAG, "initData start");
        mContext = this;
        mMicMode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, MIC_TEST_PERSIST_CONFIG_KEY);
		propSet = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, MIC_TEST_PERSIST_CONFIG).equals("true");
        if(mMicMode.isEmpty())
            mMicMode = CIT_MIC_MODE;
        LogUtil.d(TAG, "mMicMode:" + mMicMode);
	    if(!propSet){
            SystemProperties.set(mMicMode, CURRENT_MIC_MODE);  //set main mic mode
	    }
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        mFail.setVisibility(View.GONE);
        mTitle.setText(R.string.pcba_audio_secondrecord);

        mConfigTime = mConfigTime * 60;
        LogUtil.d(TAG, "mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mMIC.setVisibility(View.VISIBLE);
        mMIC.setText(String.format((getResources().getString(R.string.record_mic)),0));

        mConfigResult = getResources().getInteger(R.integer.record_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime = getResources().getInteger(R.integer.pcba_auto_test_default_time)*2;
        }else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mCurrentSystemVolume = mAudioManager.getStreamVolume(STREAM_TYPE);
        LogUtil.d(TAG, "mCurrentSystemVolume:" + mCurrentSystemVolume);
        //mAudioManager.setStreamVolume(STREAM_TYPE, 15, 0);
        mHandler = new MyHandler(mContext);
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
		        LogUtil.d(TAG, "initData mConfigTime:" + mConfigTime);
                updateFloatView(mContext,mConfigTime);
                if ( (mConfigTime == 0) && mFatherName.equals(MyApplication.PCBAAutoTestNAME) ){
                    deInit(mFatherName, NOTEST);
                    return;
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        if(mHandlerThread==null){
            mHandlerThread = new HandlerThread("AudioLoopbackService");
            mHandlerThread.start();

            mAudioHandler = new MyAudioHandler(mHandlerThread.getLooper(), this);

            mAudioHandler.sendEmptyMessageDelayed(1000,0);
        }
		LogUtil.d(TAG, "initData end");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy");
        isStop = true;
        if(!propSet){
            SystemProperties.set(mMicMode, mic_mode);
        }
        if(mCurrentSystemVolume != 0){
            mAudioManager.setStreamVolume(STREAM_TYPE, mCurrentSystemVolume, 0);
        }
        mHandler.removeCallbacksAndMessages(null);
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

    private static class MyHandler extends Handler {
        WeakReference<Activity> reference;

        public MyHandler(Activity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PCBAAutoDeputyMicActiviy activity = (PCBAAutoDeputyMicActiviy) reference.get();
            switch (msg.what) {
                case 1001:
                    int ratio = (int)msg.obj;;
                    LogUtil.d(TAG, "ratio:"+ratio);
                    int db = 0;// 分贝
                    if (ratio > 1)
                        db = (int) (20 * Math.log10(ratio));
                    LogUtil.d(TAG, "db:" + db);
                    activity.mMIC.setText(String.format((activity.getResources().getString(R.string.record_mic)),db));
                    break;
                case 1002:{
                    int mode = activity.mAudioManager.getMode();
                    activity.mAudioManager.setSpeakerphoneOn(true);
                    activity.mAudioManager.setMode(AudioManager.MODE_NORMAL);
                    int valume = activity.mAudioManager.getStreamVolume(AudioManager.MODE_NORMAL);
                    activity.mAudioManager.setStreamVolume(AudioManager.MODE_NORMAL, 15, 0);
                    try {
                        if (activity.mRecord.getRecordingState() == AudioRecord.STATE_INITIALIZED
                                && activity.mRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                            activity.mRecord.startRecording();
                        }
                        activity.mTrack.setStereoVolume(1.0f, 1.0f);
                        activity.mTrack.play();
                        activity.mHandler.sendEmptyMessage(1003);
                    } catch (Exception e) {
                        //e.printStackTrace();
                        activity.sendErrorMsgDelayed(activity.mHandler, e.getMessage());
                        if(activity.mCurrentSystemVolume != 0){
                            activity.mAudioManager.setStreamVolume(activity.STREAM_TYPE, activity.mCurrentSystemVolume, 0);
                        }
                    }
                }
                    break;
                case 1003:
                    activity.doRecordAndPlay();
                    break;
            }
        }
    };

    private void doRecordAndPlay(){
        mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        int size = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);

        if (size > mBufferSize) {
            mBufferSize = size;
        }

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, mBufferSize);
        AudioTrack track = new AudioTrack(STREAM_TYPE, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, size, AudioTrack.MODE_STREAM);
        mRecord = record;
        mTrack = track;
        //int mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        //int size = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
        try {
            if (mRecord.getRecordingState() == AudioRecord.STATE_INITIALIZED
                    && mRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                mRecord.startRecording();
            }
            mTrack.setStereoVolume(1.0f, 1.0f);
            mTrack.play();
        short[] buff = new short[mBufferSize / (Short.SIZE / 8)];
        while (!isStop) {
            int length = mRecord.read(buff, 0, buff.length);
            LogUtil.d(TAG, "run length:" + length);
            if (length < 0) {
                return;
            }
            int maxAmplitude = 0;
            for (int i = length - 1; i >= 0; i--) {
                if (buff[i] > maxAmplitude) {
                    maxAmplitude = buff[i];
                }
            }
            maxAmplitude = maxAmplitude / 5;
            Message msg = new Message();
            msg.obj = maxAmplitude;
            msg.what = 1001;
            mHandler.sendMessage(msg);
            /*int db = (int) (20 * Math.log10(maxAmplitude));
            LogUtil.d(TAG, "db:" + db);
            mMIC.setText(String.format((getResources().getString(R.string.record_mic)),db));*/
        }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mTrack != null) {
                mTrack.stop();
                mTrack.release();
            }
            if (mRecord != null) {
                mRecord.stop();
                mRecord.release();
            }
            mAudioManager.setStreamVolume(STREAM_TYPE, mCurrentSystemVolume, 0);
            mAudioManager.setMode(mMode);
        }
    }

}
