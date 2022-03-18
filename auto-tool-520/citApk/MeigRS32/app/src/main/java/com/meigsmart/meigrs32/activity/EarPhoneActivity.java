package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.meigsmart.meigrs32.service.AudioLoopbackService;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.view.VolumeView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindArray;
import butterknife.BindView;

public class EarPhoneActivity extends BaseActivity implements OnClickListener, PromptDialog.OnPromptDialogCallBack, SpeakerListAdapter.OnSpeakerSound{
    TextView textview;
    private EarPhoneActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.headset_prompt)
    public TextView mheadsetPrompt;
    @BindView(R.id.headset_prompt_un)
    public TextView mheadsetPromptUn;
    @BindView(R.id.headset_prompt_key)
    public TextView mheadsetPromptKey;
    @BindView(R.id.headset_prompt_unkey)
    public TextView mheadsetPromptUnkey;
    @BindView(R.id.headset_pttkey)
    public TextView mheadsetPttKey;
    @BindView(R.id.headset_pttkey_has_pressed)
    public TextView mheadsetPttKeyHasPressed;

    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.volume_size)
    public TextView vl;
    private String projectName = "";

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindArray(R.array.speaker_list)
    public String[] mSoundList;
    @BindView(R.id.headset_tips)
    public TextView mTips;

    @BindView(R.id.earphone_layout)
    public RelativeLayout mearphonelayout;

    private AudioManager mAudioManager;
    private final int  STREAM_TYPE = AudioManager.STREAM_MUSIC;
    @BindView(R.id.volumeView)
    public VolumeView mVolumeView;
    private MediaSession mSession;
    public boolean hasPressButton = false;
    public boolean iseraphone=false;
    private int mConfigTime = 0;
    private Runnable mRun;
    private Boolean mHeadsetHangUpKeyConfig = false;
    private Boolean mCommonKeyConfig = true;
    private String commonConfigHangUpKey = "common_ear_phone_hang_up_key_config";
    private String mCommonConfigKeyShowKey = "common_ear_phone_key_show_bool_config";
    private String mCommonConfigPassKey = "common_ear_phone_pass_volume_config";
    public static final int SLB786_HANGUP_KEY = 85;

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
    private int passVolume =0;
    boolean button_test_completed =false;
    private String mEarPhoneSoundChannel = "";
    private final String TAG = EarPhoneActivity.class.getSimpleName();


    private int mOldVolume;
    //hejianfeng add start
    int volume=0;
    int dbVol=0;
    //hejianfeng add end
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
    @Override
    protected int getLayoutId() {
        if(is_mt535){
            return R.layout.activity_ear1_phone;
        }else{
            return R.layout.activity_ear_phone;
        }

    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_audio_earphone);

        mheadsetPromptKey.setVisibility(View.GONE);
        mheadsetPromptUnkey.setVisibility(View.GONE);
        mTips.setVisibility(View.GONE);
        mearphonelayout.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mEarPhoneSoundChannel = getIntent().getStringExtra("soundchannel");
        }
        addData(mFatherName, super.mName);

        Random random=new Random();
        randomInt=0;
        do{
            randomIntsSecond=random.nextInt(10);
        } while (randomIntsSecond==randomInt);

        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime = getResources().getInteger(R.integer.pcba_auto_test_default_time) * 4;
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mOldVolume = Settings.System.getInt(getContentResolver(), "volume_music_headset", -1);
        mSession = new MediaSession(getApplicationContext(), this.getClass().getName());

        if(mAudioManager.isWiredHeadsetOn()){
            mheadsetPrompt.setVisibility(View.VISIBLE);
            mheadsetPromptUn.setVisibility(View.GONE);
            iseraphone=true;
        }

        mRecyclerView.setLayoutManager(new GridLayoutManager(this,3));
        mAdapter = new SpeakerListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        isCustomPath = getResources().getBoolean(R.bool.speaker_default_config_is_use_custom_path);
        mCustomPath = getResources().getString(R.string.speaker_default_config_custom_path);
        mCustomFileName = getResources().getString(R.string.speaker_default_config_custom_file_name);

        mHandler.sendEmptyMessageDelayed(1001,10);

        String configKeyShow = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mCommonConfigKeyShowKey);
        if(configKeyShow.length() == 0) {
            mCommonKeyConfig = true;
        } else {
            mCommonKeyConfig = configKeyShow.equals("true");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(earphonePluginReceiver, filter);

        if(mCommonKeyConfig){
            String tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, commonConfigHangUpKey);
            LogUtil.d("tmpStr:" + tmpStr);
            if (tmpStr.length() != 0) {
                mHeadsetHangUpKeyConfig = tmpStr.equals("true");
            } else {
                mHeadsetHangUpKeyConfig = getResources().getBoolean(R.bool.ear_phone_default_config_hang_up_key);
            }
            LogUtil.d("mHeadsetHangUpKeyConfig:" + mHeadsetHangUpKeyConfig);
        }
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mCommonConfigPassKey);
        LogUtil.d("tempVol:" + temp);
        if (!TextUtils.isEmpty(temp))
            passVolume = Integer.parseInt(temp);
        if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
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
    }
	@SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1000:
                    if(dbVol > 0) {
                        vl.setVisibility(View.VISIBLE);
                        vl.setText(dbVol + "dB");
                        if(dbVol>passVolume) {
                            isSound = true;
                        }
                    }
                    mVolumeView.setVolume(5*volume);
                    if(!mCommonKeyConfig||!mHeadsetHangUpKeyConfig){
                       mSuccess.setVisibility(View.VISIBLE);
                    }
                    break;
                case 1001:
                    initSoundList(mSoundList);
                    init(isCustomPath);
                    break;
                case 1002:
                    mearphonelayout.setVisibility(View.VISIBLE);
                    mRecyclerView.setVisibility(View.GONE);
                    if(mCommonKeyConfig){
                        if (mHeadsetHangUpKeyConfig) {
                            button_test_completed = true;
                            mheadsetPromptUnkey.setVisibility(View.VISIBLE);
                        }else {
                            mheadsetPttKey.setVisibility(View.VISIBLE);
                        }
                        mheadsetPromptKey.setVisibility(View.GONE);
                    }

                    MyApplication.getInstance().setVolumeViewListener(new MyApplication.VolumeViewListener() {
                        @Override
                        public void volumeChange(int vol) {
                            if(iseraphone){
                                int db = (int) (20 * Math.log10(vol));
                                LogUtil.d("volumeChange vol=" + vol+",db="+db);
                                volume=vol;
                                dbVol=db;
                                mHandler.sendEmptyMessage(1000);
                            }
                        }
                    });
                    if(mAudioManager.isWiredHeadsetOn()){
                        mheadsetPrompt.setVisibility(View.VISIBLE);
                        mTips.setVisibility(View.VISIBLE);
                        mheadsetPromptUn.setVisibility(View.GONE);
                        iseraphone=true;
                        startAudioService();
                    }
                    //deInit(mFatherName, SUCCESS);
                    break;
                case 1003:
                    deInit(mFatherName, FAILURE);
                    break;
                case 1004:
                    if(!mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
                        songplay();
                    }else LogUtil.d(TAG, "1004 mFatherName:" + mFatherName);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,"Wrong choice sound");
                    break;
            }
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtil.d("onKeyUp keyCode:" + keyCode);
        LogUtil.d("onKeyUp event:" + event);
        switch(keyCode){
            case KeyEvent.KEYCODE_HEADSETHOOK: 
            case SLB786_HANGUP_KEY:
                if(mHeadsetHangUpKeyConfig) {
                    mheadsetPromptUnkey.setVisibility(View.GONE);
                    mheadsetPromptKey.setVisibility(View.VISIBLE);
                    if("MT537".equals(projectName)&& button_test_completed) {
                        mSuccess.setVisibility(View.VISIBLE);
                    }
                    hasPressButton = true;
					//add by zhaohairuo for bug 23329 @2019-01-21 start
                    if(SuccessClick && SuccessClick2 && isSound){
                        mSuccess.setVisibility(View.VISIBLE);
                        if ((mFatherName.equals(MyApplication.PCBANAME)) || (mFatherName.equals(MyApplication.PreNAME))) {
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            deInit(mFatherName, SUCCESS);//auto pass pcba & pre
                        }
                        if(is_mt535){
                            mSuccess.performClick();
                        }
                    }
                    //add by zhaohairuo for bug 23329 @2019-01-21 end
                }
                break;
            case KeyEvent.KEYCODE_F11:
                if(!mHeadsetHangUpKeyConfig) {

                    mheadsetPttKey.setVisibility(View.GONE);
                    mheadsetPttKeyHasPressed.setVisibility(View.VISIBLE);
                    hasPressButton = true;
					//add by zhaohairuo for bug 23329 @2019-01-21 start
                    if(SuccessClick && SuccessClick2 && isSound) {
                        mSuccess.setVisibility(View.VISIBLE);
                        if ((mFatherName.equals(MyApplication.PCBANAME)) || (mFatherName.equals(MyApplication.PreNAME))) {
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            deInit(mFatherName, SUCCESS);//auto pass pcba & pre
                        }
                    }
                    //add by zhaohairuo for bug 23329 @2019-01-21 end
                }
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(earphonePluginReceiver);
//        unregisterReceiver(mReceiver);
        stopAudioService();



        if (mediaPlayer!=null){
            mManager.setMode(AudioManager.MODE_RINGTONE);
            mManager.setSpeakerphoneOn(false);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1004);
        mHandler.removeMessages(9999);
        //mAudioManager.setStreamVolume(STREAM_TYPE, mOldVolume, 0);
    }
    //hejianfeng add start for [zendao]8134
    private boolean isRunAudioService=false;
    private void startAudioService(){
        if(!isRunAudioService) {
            Intent intent = new Intent(this, AudioLoopbackService.class);
            intent.putExtra("isEarPhone", true);
            intent.putExtra("soundchannel", mEarPhoneSoundChannel);
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
    private BroadcastReceiver earphonePluginReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent earphoneIntent) {
            if (earphoneIntent == null || earphoneIntent.getAction() == null)
                return;
            if (!earphoneIntent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG))
                return;
            int st = earphoneIntent.getIntExtra("state", 0);
            boolean isWiredHeadsetOn=mAudioManager.isWiredHeadsetOn();
            LogUtil.d("citapk st:" + st+",isWiredHeadsetOn="+isWiredHeadsetOn);
            if(!mCommonKeyConfig) {
                if(isWiredHeadsetOn) {
                    iseraphone = true;
                    if (!(SuccessClick&&SuccessClick2)) {
                        if (mediaPlayer != null) {
                            songplay();
                        } else {
                            init(isCustomPath);
                        }
                    }
                    //mSuccess.setVisibility(View.VISIBLE);
                }
            }
            if (!isWiredHeadsetOn) {
                mheadsetPrompt.setVisibility(View.GONE);
                mheadsetPromptUn.setVisibility(View.VISIBLE);
                vl.setVisibility(View.INVISIBLE);
                if(mCommonKeyConfig) {
                    if(mHeadsetHangUpKeyConfig) {
                        mheadsetPromptUnkey.setVisibility(View.VISIBLE);
                        mheadsetPromptKey.setVisibility(View.GONE);
                    }else{
                        mheadsetPttKey.setVisibility(View.VISIBLE);
                        mheadsetPttKeyHasPressed.setVisibility(View.GONE);
                    }
                }
                iseraphone=false;
                if(isRunAudioService)
                stopAudioService();//hejianfeng add for [zendao]8134
            } else if (isWiredHeadsetOn) {
                mheadsetPrompt.setVisibility(View.VISIBLE);
                mheadsetPromptUn.setVisibility(View.GONE);
                iseraphone=true;
                if(!isRunAudioService && SuccessClick && SuccessClick2)
                startAudioService();//hejianfeng add for [zendao]8134
                if (!(SuccessClick&&SuccessClick2)) {
                    if (mediaPlayer != null) {
                        songplay();
                    } else {
                        init(isCustomPath);
                    }
                }
            }
        }
    };

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
//    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (action.equalsIgnoreCase(AudioLoopbackService.ACTION_VOLUME_UPDATED)) {
//                if(iseraphone){
//                    int vol = intent.getIntExtra("volume", 0);
//                    int db = (int) (20 * Math.log10(vol));
//                    LogUtil.d("mReceiver vol=" + vol+",db="+db);
//                    if(db > 0) {
//                        vl.setVisibility(View.VISIBLE);
//                        vl.setText(db + "dB");
//                    }
//                    mVolumeView.setVolume(vol);
//                }
//            }
//        }
//    };

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
        mManager.setMode(AudioManager.MODE_RINGTONE);
        mediaPlayer.setAudioStreamType(AudioManager.MODE_RINGTONE);
        songArrayList = new ArrayList<String>();
        songArrayList.add(randomInt+".wav");
        songArrayList.add(randomIntsSecond+".wav");
        mHandler.sendEmptyMessageDelayed(1004,600);
    }

    private final class CompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            nextsong();
        }

    }

    private void initSoundList(String[] str){
        if(mFatherName.equals(MyApplication.PCBAAutoTestNAME)){
            LogUtil.d(TAG, "current test can do handler nothing!");
            SuccessClick = true;
            SuccessClick2 = true;
            ManualTestFinish = true;
            mHandler.sendEmptyMessage(1002);
            return;
        }
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
        if(!iseraphone) return;
        try {
            mediaPlayer.reset();
            AssetFileDescriptor afd = this.getAssets().openFd(songArrayList.get(songIndex));
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setVolume(1.0f,1.0f);
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
    public void onSpeakerItemListener(int pos) {
        click++;
        if(ManualTestFinish) {
            return;
        }
        if(click == 2){
            if (mediaPlayer != null) {
                mManager.setMode(AudioManager.MODE_RINGTONE);
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
