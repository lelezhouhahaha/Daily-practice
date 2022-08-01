package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.SpeakerListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.service.AudioLoopbackService;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.view.VolumeView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;

public class PCBAAutoMainTestMICActivity extends BaseActivity implements PromptDialog.OnPromptDialogCallBack, SpeakerListAdapter.OnSpeakerSound{
    //TextView textview;
    private PCBAAutoMainTestMICActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;

    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    //@BindView(R.id.volume_size)
    //public TextView vl;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.mic)
    public TextView mMIC;
    //@BindView(R.id.recycleView)
    //public RecyclerView mRecyclerView;
    //@BindArray(R.array.speaker_list)
    //public String[] mSoundList;
    //@BindView(R.id.speak_tips)
    //public TextView mTips;
    //@BindView(R.id.speakloop_prompt)
    //public TextView mSpeakloopPrompt;

    //@BindView(R.id.speakloop_layout)
    //public RelativeLayout speakloop_layout;

    private AudioManager mAudioManager;
    private final int  STREAM_TYPE = AudioManager.STREAM_MUSIC;
    //@BindView(R.id.volumeView)
    //public VolumeView mVolumeView;
    private MediaSession mSession;
    private int mConfigTime = 0;
    private String mCommonConfigPassKey = "common_ear_phone_pass_volume_config";

    private int randomInt;
    private int randomIntsSecond;
    private SpeakerListAdapter mAdapter;
    private AudioManager mManager;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mCustomFileName ;
    private MediaPlayer mediaPlayer;
    private boolean SuccessClick = false;
    private boolean SuccessClick2 = false;
    private boolean ManualTestFinish = false;
    private ArrayList<String> songArrayList;
    private int songIndex = 0;
    private int click = 0;
    private boolean isSound = false;
    private int passVolume = 50;//default

    private int mOldVolume;
    //hejianfeng add start
    int volume=0;
    int dbVol=0;
    //hejianfeng add end
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
    private static final String CIT_MIC_MODE = "persist.sys.cit.micmode";
    private String mic_mode = "0";
    private static final String CURRENT_MIC_MODE = "1";
    private int maxMIC = 0;
    private int mCurrentSystemVolume = 0;
    private String MIC_TEST_PERSIST_CONFIG_KEY = "common_mic_test_persist_config_string";
    private String MIC_TEST_PERSIST_CONFIG = "common_mic_test_persist_config";
    private String mMicMode = "";
    private boolean propSet = false;
    private Runnable mRun = null;
    private final String TAG = PCBAAutoMainTestMICActivity.class.getSimpleName();
    @Override
    protected int getLayoutId() {
        return R.layout.activity_pcba_auto_main_mic;
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        boolean flag = intent.getBooleanExtra("finish", false);
        LogUtil.d(TAG, "onNewIntent flag:" + flag);
        if(flag) {
            LogUtil.d(TAG, "onNewIntent finish current activity!");
            mContext.finish();
        }
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        mFail.setVisibility(View.GONE);
        //mBack.setOnClickListener(this);
        //mSuccess.setOnClickListener(this);
        //mFail.setOnClickListener(this);
		mMIC.setVisibility(View.VISIBLE);
        mTitle.setText(R.string.pcba_audio_record);

        //mTips.setVisibility(View.GONE);
        //speakloop_layout.setVisibility(View.GONE);
        //mRecyclerView.setVisibility(View.VISIBLE);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime = getResources().getInteger(R.integer.pcba_auto_test_default_time)*2;
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mOldVolume = Settings.System.getInt(getContentResolver(), "volume_music_headset", -1);
        mSession = new MediaSession(getApplicationContext(), this.getClass().getName());

        /*mRecyclerView.setLayoutManager(new GridLayoutManager(this,3));
        mAdapter = new SpeakerListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);*/

        /*isCustomPath = getResources().getBoolean(R.bool.speaker_default_config_is_use_custom_path);
        mCustomPath = getResources().getString(R.string.speaker_default_config_custom_path);
        mCustomFileName = getResources().getString(R.string.speaker_default_config_custom_file_name);*/

        mHandler.sendEmptyMessageDelayed(1002,10);

        /*String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mCommonConfigPassKey);
        LogUtil.d("tempVol:" + temp);
        if (!TextUtils.isEmpty(temp))
            passVolume = Integer.parseInt(temp);*/

        //mSpeakloopPrompt.setText(R.string.speakloop_prompt_start);
        //mSpeakloopPrompt.setOnClickListener(this);
        //mSpeakloopPrompt.setVisibility(View.GONE);

        mMicMode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, MIC_TEST_PERSIST_CONFIG_KEY);
        propSet = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, MIC_TEST_PERSIST_CONFIG).equals("true");
        if(mMicMode.isEmpty())
            mMicMode = CIT_MIC_MODE;
        LogUtil.d(TAG, "mMicMode:" + mMicMode);
        if(!propSet){
            SystemProperties.set(mMicMode, CURRENT_MIC_MODE);  //set main mic mode
        }

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
    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                /*case 1000:
                    if(dbVol > 0) {
                        vl.setVisibility(View.VISIBLE);
                        vl.setText(dbVol + "dB");
                        if(dbVol>passVolume) {
                            isSound = true;
                        }
                    }
                    mVolumeView.setVolume(5*volume);
                    //android.util.Log.i("Yar", " passVolume = " + passVolume + ", isSound = " + isSound + ", dbVol = " + dbVol);
                    if (isSound) {
                        mSuccess.setVisibility(View.VISIBLE);
                        mSpeakloopPrompt.setTextColor(getResources().getColor(R.color.blue_dan));
                    }
                    break;
                case 1001:
                    initSoundList(mSoundList);
                    init(isCustomPath);
                    break;*/
                case 1000:
                    mMIC.setText(String.format((getResources().getString(R.string.record_mic)),dbVol));
                    break;
                case 1002:
                    //speakloop_layout.setVisibility(View.VISIBLE);
                    //mRecyclerView.setVisibility(View.GONE);
                    //mSpeakloopPrompt.setVisibility(View.VISIBLE);
                    MyApplication.getInstance().setVolumeViewListener(new MyApplication.VolumeViewListener() {
                        @Override
                        public void volumeChange(int vol) {
                            int db = (int) (20 * Math.log10(vol));
                            LogUtil.d(TAG, "volumeChange vol=" + vol+",db="+db);
                            volume=vol;
                            dbVol=db;
                            mHandler.sendEmptyMessage(1000);
                        }
                    });

                    //mTips.setVisibility(View.VISIBLE);
                    //mTips.setText(String.format(getResources().getString(R.string.speak_tips), getResources().getString(R.string.speakloop_prompt_start)));
                    startAudioService();
                    break;
                case 1003:
                    deInit(mFatherName, FAILURE);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,"Wrong choice sound");
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
        //unregisterReceiver(earphonePluginReceiver);
        stopAudioService();

        if (mediaPlayer!=null){
            mManager.setMode(AudioManager.MODE_NORMAL);
            mManager.setSpeakerphoneOn(false);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if(!propSet){
            SystemProperties.set(mMicMode, mic_mode);
        }
        if(mCurrentSystemVolume != 0){
            mAudioManager.setStreamVolume(STREAM_TYPE, mCurrentSystemVolume, 0);
        }
        mHandler.removeMessages(1000);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    private boolean isRunAudioService=false;
    private void startAudioService(){
        if(!isRunAudioService) {
            Intent intent = new Intent(this, AudioLoopbackService.class);
            intent.putExtra("isEarPhone", false);
            intent.putExtra(AudioLoopbackService.OLD_VOLUME, mOldVolume);
            startService(intent);
            isRunAudioService=true;
        }
    }
    private void stopAudioService(){
        if(isRunAudioService) {
            Intent intent = new Intent(this, AudioLoopbackService.class);
            stopService(intent);
            isRunAudioService=false;
        }
    }
    //hejianfeng add end for [zendao]8134
    /*private BroadcastReceiver earphonePluginReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent earphoneIntent) {
            if (earphoneIntent == null || earphoneIntent.getAction() == null)
                return;
            if (!earphoneIntent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG))
                return;
            int st = earphoneIntent.getIntExtra("state", 0);
            boolean isWiredHeadsetOn = mAudioManager.isWiredHeadsetOn();
            LogUtil.d("citapk st:" + st+",isWiredHeadsetOn="+isWiredHeadsetOn);
            android.util.Log.i("Yar", " earphonePluginReceiver isWiredHeadsetOn = " + isWiredHeadsetOn);

            if (!isWiredHeadsetOn) {
                if(isRunAudioService) {
                    stopAudioService();
                }
            } else if (isWiredHeadsetOn) {
                if(!isRunAudioService) {
                    startAudioService();
                }
            }
        }
    };*/

    /*@Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back:
                if (!mDialog.isShowing()) mDialog.show();
                mDialog.setTitle(super.mName);
                break;
            case R.id.success:
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                deInit(mFatherName, SUCCESS);
                break;
            case R.id.fail:
                mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                break;
            case R.id.speakloop_prompt:
                if (isRunAudioService) {
                    mSpeakloopPrompt.setText(R.string.speakloop_prompt_start);
                    stopAudioService();
                } else {
                    mSpeakloopPrompt.setText(R.string.speakloop_prompt_stop);
                    startAudioService();
                }
        }
    }*/

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


    private void init(boolean isCustom){
        mManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new CompletionListener());
        mManager.setSpeakerphoneOn(true);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mManager.setMode(AudioManager.MODE_NORMAL);
        mCurrentSystemVolume = mAudioManager.getStreamVolume(STREAM_TYPE);
        LogUtil.d(TAG, "mCurrentSystemVolume:" + mCurrentSystemVolume);
        //mManager.setStreamVolume(AudioManager.MODE_NORMAL, 6, 0);
        mediaPlayer.setAudioStreamType(AudioManager.MODE_NORMAL);
        songArrayList = new ArrayList<String>();
        songArrayList.add(randomInt+".wav");
        songArrayList.add(randomIntsSecond+".wav");
        songplay();
    }

    private final class CompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            nextsong();
        }

    }

    private void initSoundList(String[] str){
        List<TypeModel> list = new ArrayList<>();
        for (int i=0;i< str.length;i++){
            TypeModel model = new TypeModel();
            model.setName(str[i]);
            model.setType(0);
            list.add(model);
        }
        mAdapter.setData(list);
        mAdapter.notifyDataSetChanged();
    }

    private void nextsong() {

        if (songIndex < songArrayList.size() - 1) {
            songIndex = songIndex + 1;
        }
        else {
            songIndex = 0;
        }
        LogUtil.d("citapk nextsong songIndex:" + songIndex);
        songplay();
    }

    private void songplay() {
        // add by chenzq bug 11065
        try {
            mediaPlayer.reset();
            AssetFileDescriptor afd = this.getAssets().openFd(songArrayList.get(songIndex));
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setVolume(0.5f,0.5f);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void setDataSource(boolean isCustom){
        try {
            if (isCustom){
                if (TextUtils.isEmpty(mCustomPath) && TextUtils.isEmpty(mCustomFileName)){
                    sendErrorMsgDelayed(mHandler,"the custom file path is null");
                    return;
                }
                File file = FileUtil.createRootDirectory(mCustomPath);
                File file1 = FileUtil.mkDir(file);
                File f = new File(file1.getPath(),mCustomFileName);

                mediaPlayer.setDataSource(f.getPath());
            }else {
                AssetFileDescriptor afd = this.getAssets().openFd(randomInt+".wav");
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            }
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
    }

    @Override
    public void onSpeakerItemListener(int pos) {
        click++;
        if(ManualTestFinish) {
            return;
        }
        if(click == 2){
            if (mediaPlayer != null) {
                mManager.setMode(AudioManager.MODE_NORMAL);
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }

        if (mAdapter.getData().get(pos).getType() == 0) {
            mAdapter.getData().get(pos).setType(1);
        } else {
            mAdapter.getData().get(pos).setType(0);
        }
        mAdapter.notifyDataSetChanged();
        if (pos == randomInt) {
            SuccessClick = true;
        } else if (pos == randomIntsSecond) {
            SuccessClick2 = true;
        }else {
            mFail.setVisibility(View.VISIBLE);
            if (!mFatherName.equals(MyApplication.PreSignalNAME) && !mFatherName.equals(MyApplication.PCBASignalNAME))
                mHandler.sendEmptyMessage(9999);
        }

        /*if (!mFatherName.equals(MyApplication.PreSignalNAME) && !mFatherName.equals(MyApplication.PCBASignalNAME)){
            if(SuccessClick && SuccessClick2) {
                mSuccess.setVisibility(View.VISIBLE);
                mHandler.sendEmptyMessage(1002);
            }
        }else {*/
            if(pos !=  randomInt && pos !=  randomIntsSecond) {
                ManualTestFinish = true;
                mFail.setVisibility(View.VISIBLE);
            }else if(SuccessClick && SuccessClick2 ) {
                ManualTestFinish = true;
                mHandler.sendEmptyMessage(1002);
            }
        //}
    }
}
