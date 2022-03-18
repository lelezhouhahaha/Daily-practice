package com.meigsmart.meigrs32.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.log.LogUtil;

public class AudioLoopbackService extends Service {
    public static final String ACTION_VOLUME_UPDATED = "com.meigsmart.function.service.VOLUME_UPDATED";

    public static final String OLD_VOLUME = "old_volume";

    private final int STREAM_TYPE = AudioManager.STREAM_VOICE_CALL;
    private final int SAMPLE_RATE = 44100;
    private final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_STEREO;
    private final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_STEREO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioManager mAudioManager;
    private int mBufferSize;
    private boolean isStop = false;
    public final static String ACTION_STOP = "com.meigsmart.function.service.stop";

    private int mOldVolume;

    //added by Yar for speakerloop begin
    private boolean isEarPhone = false;
    private String mSoundChannel = "";
    //added by Yar for speakerloop end
    public void startLoopbackThread() {
        if(mHandlerThread==null){
            mHandlerThread = new HandlerThread("AudioLoopbackService");
            mHandlerThread.start();

            mHandler = new MyHandler(mHandlerThread.getLooper());

            mHandler.sendEmptyMessageDelayed(1000,0);
           // m_recordAudioThread = new AudioLoopbackThread(mOldVolume);
           // m_recordAudioThread.start();
        }
    }

    public void stopLoopbackThread() {
        isStop = true;
        if(mHandler !=null){
            mHandler.removeMessages(1000);
            mHandler.removeMessages(1001);
            mHandler = null;
        }
        if( mHandlerThread != null ) {
            mHandlerThread.quit();
            mHandlerThread = null;
           // m_recordAudioThread.end();
           // m_recordAudioThread = null;
        }
    }

    @Override
    public void onCreate() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        stopLoopbackThread();
        mAudioManager.setStreamVolume(STREAM_TYPE, mOldVolume, 0);
        super.onDestroy();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try{
            if (ACTION_STOP.equals(intent.getAction())) {
                stopLoopbackThread();
                return super.onStartCommand(intent, flags, startId);
            }

            mOldVolume = intent.getIntExtra(OLD_VOLUME, 0);
            isEarPhone = intent.getBooleanExtra("isEarPhone", false);
            mSoundChannel = intent.getStringExtra("soundchannel");
            LogUtil.d("zll", "isEarPhone:" + isEarPhone + " mSoundChannel:" +mSoundChannel);
        }catch (Exception e){
            e.printStackTrace();
        }
        startLoopbackThread();

        return super.onStartCommand(intent, flags, startId);
    }


//    private AudioLoopbackThread m_recordAudioThread = null;
    private AudioRecord mRecord = null;
    private AudioTrack mTrack = null;

    private HandlerThread mHandlerThread = null;
    private MyHandler mHandler = null;
//    private class AudioLoopbackThread extends Thread {
//        private AudioRecord mRecord;
//        private AudioTrack mTrack;
//        private int mOldVolume;
//        private boolean isStop = false;
//        public void end() {
//            isStop = true;
//            try {
//                this.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        };
//        public AudioLoopbackThread(int oldVolume) {
//            super();
//
//            mOldVolume = oldVolume;
//            mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
//            int size = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
//
//            if (size > mBufferSize) {
//                mBufferSize = size;
//            }
//
//            mRecord = new AudioRecord(AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, mBufferSize);
//            mTrack = new AudioTrack(STREAM_TYPE, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, size, AudioTrack.MODE_STREAM);
//        }
//
//        public void run() {
//            int mode = mAudioManager.getMode();
//            mAudioManager.setSpeakerphoneOn(false);
//            boolean isheadseton = mAudioManager.isWiredHeadsetOn();
//            if(isheadseton){
//                mAudioManager.setMode(AudioManager.MODE_IN_CALL);
//                mAudioManager.setStreamVolume(STREAM_TYPE, 6, 0);
//                try {
//                    if (mRecord.getRecordingState() == AudioRecord.STATE_INITIALIZED
//                            && mRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
//                        mRecord.startRecording();
//                    }
//                    mTrack.play();
//                    short[] buff = new short[mBufferSize / (Short.SIZE / 8)];
//                    while (!isStop) {
//                        int length = mRecord.read(buff, 0, buff.length);
//                        if (length < 0) {
//                            break;
//                        }
//
//                        int maxAmplitude = 0;
//
//                        for (int i = length - 1; i >= 0; i--) {
//                            if (buff[i] > maxAmplitude) {
//                                maxAmplitude = buff[i];
//                            }
//                        }
//
//                        length = mTrack.write(buff, 0, length);
//                        maxAmplitude = maxAmplitude / 5;
//                        if(MyApplication.getInstance().volumeViewListener!=null){
//                            MyApplication.getInstance().volumeViewListener.volumeChange(maxAmplitude);
//                        }
//
//                        if (length < 0) {
//                            break;
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    if(mTrack!=null) {
//                        mTrack.stop();
//                        mTrack.release();
//                    }
//                    if(mRecord!=null) {
//                        mRecord.stop();
//                        mRecord.release();
//                    }
//                    mAudioManager.setStreamVolume(STREAM_TYPE, mOldVolume, 0);
//                    mAudioManager.setMode(mode);
//                }
//            }
//
//        }
//    }

    private short[] buff;
    private int mode;

    class MyHandler extends Handler {

        public MyHandler(Looper looper) {
            super(looper, null);


        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1000:
                    LogUtil.d("zll","zll 1000");
                    mode = mAudioManager.getMode();
                    mAudioManager.setSpeakerphoneOn(!isEarPhone);//modified by Yar
                    boolean isheadseton = mAudioManager.isWiredHeadsetOn();
                    if (isheadseton || isEarPhone) {//modified by Yar
                        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                        int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_TYPE);
                        mAudioManager.setStreamVolume(STREAM_TYPE, 15, 0);
                        //run();
                        if(null != mHandler)
                        mHandler.sendEmptyMessage(1001);
                    }
                    break;
                case 1001:
                        run();
                    break;
            }
        }

    }
    public void run() {

        try {
            mBufferSize = 6 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
            int size = 6 * AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);

            /*if (size > mBufferSize) {
                mBufferSize = size;
            }*/
            mRecord = new AudioRecord(AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, mBufferSize);
            mTrack = new AudioTrack(STREAM_TYPE, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, size, AudioTrack.MODE_STREAM);//modified by Yar

            if (mRecord.getRecordingState() == AudioRecord.STATE_INITIALIZED
                    && mRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                mRecord.startRecording();
            }

            mTrack.play();
			//mTrack.setStereoVolume(0.0f, 1.0f);
            if(isEarPhone && (mSoundChannel != null) && (!mSoundChannel.isEmpty())) {
                Boolean left = mSoundChannel.equals("left");
                Boolean right = mSoundChannel.equals("right");
                LogUtil.d("zll", "set left or right channel left:" + left + " right:" + right);
                //mTrack.setStereoVolume(1.0f, 0.0f);
                mTrack.setStereoVolume(left?1.0f:0.0f, right?1.0f:0.0f);
            }else {
                mTrack.setStereoVolume(1.0f, 1.0f);
                LogUtil.d("zll", "set left and right channel");
            }

            buff = new short[mBufferSize / 2];
            playrun();
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
            mAudioManager.setStreamVolume(STREAM_TYPE, mOldVolume, 0);
            mAudioManager.setMode(mode);
            mTrack = null;
            mRecord = null;
        }


    }

    private void playrun(){
        while (!isStop) {
            int length = mRecord.read(buff, 0, buff.length);
            if (length < 0) {
                return;
            }
            length = mTrack.write(buff, 0, length);

            int maxAmplitude = 0;
            for (int i = length - 1; i >= 0; i--) {
                if (buff[i] > maxAmplitude) {
                    maxAmplitude = buff[i];
                }
            }
            maxAmplitude = maxAmplitude / 5;
            if (MyApplication.getInstance().volumeViewListener != null) {
                MyApplication.getInstance().volumeViewListener.volumeChange(maxAmplitude);
            }

            if (length < 0) {
                return;
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
