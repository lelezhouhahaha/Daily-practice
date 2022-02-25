package com.meigsmart.meigrs32.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import com.meigsmart.meigrs32.log.LogUtil;

import java.io.IOException;

/**
 * Created by chenMeng on 2018/1/30.
 */

public class MusicService extends Service {
    public MediaPlayer mediaPlayer;
    public boolean isPlay = false;

    public MusicService() {
    }

    private MusicService getInstance(boolean isCustom,String customFilePath){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.reset();
        setDataSource(isCustom,customFilePath);
        mediaPlayer.setLooping(false);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isPlay = true;
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                isPlay = true;
                mp.seekTo(0);
                mp.start();
            }
        });
        return MusicService.this;
    }

    private void setDataSource(boolean isCustom,String path){
        try {
            if (isCustom){
                mediaPlayer.setDataSource(path);
            }else {
                AssetFileDescriptor afd = null;
                if(path == null){
                    afd = this.getAssets().openFd("music.mp3");
                }else {
                    String[] str = path.split("/");
                    int index = str.length;
                    String fileName = str[index-1];
                    LogUtil.d("music service", "fileName:<" + fileName + "> path:<" + path + ">");
                    afd = this.getAssets().openFd(fileName);
                }
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            }
            mediaPlayer.prepare();
        } catch (IOException e) {
            stop();
            e.printStackTrace();
        }
    }

    public MyBinder binder = new MyBinder();

    public class MyBinder extends Binder {
        public MusicService getService(boolean isCustom,String customFilePath) {
            return getInstance(isCustom, customFilePath);
        }
    }

    public void stop() {
        isPlay = false;
        if(mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
