package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.SpeakerListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.service.MusicService;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindArray;
import butterknife.BindView;

public class SpeakerActivity extends BaseActivity implements View.OnClickListener,
        PromptDialog.OnPromptDialogCallBack ,SpeakerListAdapter.OnSpeakerSound{
    private SpeakerActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.layout)
    public RelativeLayout mLayout;
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindArray(R.array.speaker_list)
    public String[] mSoundList;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private AudioManager mManager;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mCustomFileName ;
    private MediaPlayer mediaPlayer;
    private SpeakerListAdapter mAdapter;
    private int randomInt;
    private int randomIntsSecond;
    private boolean SuccessClick = false;
    private boolean SuccessClick2 = false;
    private boolean ManualTestFinish = false;
    private ArrayList<String> songArrayList;
    private int songIndex = 0;
    private int mConfigTime = 0;
    private String projectName = "";

    private AudioManager mAudioManger = null;
    private Intent intentMusic = null;
    private MusicService musicService = null;
    private int mCurrentVolume = 0;
    private final String TAG = SpeakerActivity.class.getSimpleName();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_speaker;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        mFail.setVisibility(View.GONE);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_audio_speaker);
        Random random=new Random();
        randomInt=0;
        do{
            randomIntsSecond=random.nextInt(10);
        } while (randomIntsSecond==randomInt);
        LogUtil.d("citapk randomInt:" + randomInt);
        LogUtil.d("citapk randomIntsSecond:" + randomIntsSecond);
        isCustomPath = getResources().getBoolean(R.bool.speaker_default_config_is_use_custom_path);
        mCustomPath = getResources().getString(R.string.speaker_default_config_custom_path);
        mCustomFileName = getResources().getString(R.string.speaker_default_config_custom_file_name);
        LogUtil.d("isCustomPath:"+isCustomPath+" mCustomPath:"+mCustomPath);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        mRecyclerView.setLayoutManager(new GridLayoutManager(this,3));
        mAdapter = new SpeakerListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mHandler.sendEmptyMessageDelayed(1001,10);

        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        if(projectName.contains("MC520") && (mFatherName.equals(MyApplication.PreNAME) || mFatherName.equals(MyApplication.PreSignalNAME))){
            mAudioManger = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mFlag.setVisibility(View.GONE);
                    mLayout.setVisibility(View.VISIBLE);
                    initSoundList(mSoundList);
                    init(isCustomPath);
                    break;
                case 1002:
                    deInit(mFatherName, SUCCESS);
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

    private void init(boolean isCustom){
        mManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new CompletionListener());
        mManager.setMicrophoneMute(false);
        mManager.setSpeakerphoneOn(true);//使用扬声器外放，即使已经插入耳机
        /*setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
        mManager.setMode(AudioManager.STREAM_MUSIC);
        mManager.setStreamVolume(AudioManager.STREAM_MUSIC, 15, 0);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);*/
         setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
        mManager.setMode(AudioManager.MODE_NORMAL);
        mManager.setStreamVolume(AudioManager.MODE_NORMAL, 15, 0);
        mediaPlayer.setAudioStreamType(AudioManager.MODE_NORMAL);
        songArrayList = new ArrayList<String>();
        songArrayList.add(randomInt+".wav");
        songArrayList.add(randomIntsSecond+".wav");
        songplay();
        //mediaPlayer.reset();
        //setDataSource(isCustom);
        //mediaPlayer.stop();
        /*mediaPlayer.setLooping(false);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.seekTo(0);
                mp.start();
            }
        });*/
    }
    private final class CompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            if(projectName.contains("MC520") && (mFatherName.equals(MyApplication.PreNAME) || mFatherName.equals(MyApplication.PreSignalNAME))) {
                if (!ManualTestFinish) {
                    nextsong();
                }
            }else{
                nextsong();
            }
        }

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
        try {
            mediaPlayer.reset();
            AssetFileDescriptor afd = this.getAssets().openFd(songArrayList.get(songIndex));
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            if("MT535".equals(projectName)) {
                mediaPlayer.setVolume(0.5f, 0.5f);
            }else{
                mediaPlayer.setVolume(1.0f, 1.0f);
            }
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
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
    protected void onDestroy() {
        super.onDestroy();
        stopAudioService();
        if (mediaPlayer!=null){
            mManager.setMode(AudioManager.MODE_NORMAL);
            mManager.setSpeakerphoneOn(false);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    @Override
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

    @Override
    public void onSpeakerItemListener(int pos) {
        if(ManualTestFinish)
            return;

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

        if(projectName.contains("MC520") && (mFatherName.equals(MyApplication.PreNAME) || mFatherName.equals(MyApplication.PreSignalNAME))){
            if(SuccessClick && SuccessClick2) {
                ManualTestFinish = true;
                mSuccess.setVisibility(View.VISIBLE);
                mFail.setVisibility(View.VISIBLE);
                //mHandler.sendEmptyMessage(1002);
                //start audio service
                startAudioService();
            }else if(pos !=  randomInt && pos !=  randomIntsSecond) {
                ManualTestFinish = true;
                mFail.setVisibility(View.VISIBLE);
                mHandler.sendEmptyMessage(1003);
            }
        }else{
            if (!mFatherName.equals(MyApplication.PreSignalNAME) && !mFatherName.equals(MyApplication.PCBASignalNAME)){
                if(SuccessClick && SuccessClick2) {
                    mSuccess.setVisibility(View.VISIBLE);
                    mHandler.sendEmptyMessage(1002);
                }
            }else {
                if(pos !=  randomInt && pos !=  randomIntsSecond) {
                    ManualTestFinish = true;
                    mFail.setVisibility(View.VISIBLE);
                }else if(SuccessClick && SuccessClick2) {
                    ManualTestFinish = true;
                    mSuccess.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void startAudioService(){
        mCurrentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVoluem = mAudioManger.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        LogUtil.d(TAG, "citapk maxVoluem:" + maxVoluem);
        mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, maxVoluem, 0);
        intentMusic = new Intent(this, MusicService.class);
        startService(intentMusic);
        bindAudioService();
    }

    private void bindAudioService(){
        bindService(intentMusic, serviceConnection, BIND_AUTO_CREATE);
    }
    private void unbindAudioService(){
        if (musicService!=null){
            musicService.stop();
            unbindService(serviceConnection);
        }
    }

    private void stopAudioService(){
        if(mCurrentVolume != 0) {
            mAudioManger.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
            int currentVolume = mAudioManger.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        unbindAudioService();
        if (intentMusic!=null) {
            stopService(intentMusic);
        }
        musicService = null;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.d(TAG, "onServiceConnected");
            musicService = ((MusicService.MyBinder) (service)).getService(false/*isAudioCustomPath*/,/*mAudioCustomFilePath*/"speaker.mp3");
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.d(TAG, "onServiceDisconnected");
            musicService = null;
        }
    };
}
