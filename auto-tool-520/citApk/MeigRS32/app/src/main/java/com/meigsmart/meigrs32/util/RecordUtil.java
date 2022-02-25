package com.meigsmart.meigrs32.util;

import android.media.MediaRecorder;
import android.os.Handler;

import com.meigsmart.meigrs32.log.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RecordUtil {

    public static final int MSG_ERROR_AUDIO_RECORD = -4;
    private MediaRecorder mRecorder;
    private String mDirString;
    private String mCurrentFilePathString;
    private Handler handler;
    private boolean isPrepared;// 是否准备好了

    /**
     * 单例化这个类
     */
    private static RecordUtil mInstance;

    private RecordUtil(String dir) {
        mDirString = dir;
    }

    public static RecordUtil getInstance(String dir) {
        if (mInstance == null) {
            synchronized (RecordUtil.class) {
                if (mInstance == null) {
                    mInstance = new RecordUtil(dir);
                }
            }
        }
        return mInstance;

    }

    public void setHandle(Handler handler) {
        this.handler = handler;
    }

    /**
     * 回调函数，准备完毕，准备好后，button才会开始显示录音框
     */
    public interface AudioStageListener {
        void wellPrepared();
    }

    public AudioStageListener mListener;

    public void setOnAudioStageListener(AudioStageListener listener) {
        mListener = listener;
    }

    public void setVocDir(String dir) {
        mDirString = dir;
    }

    public void prepareAudio() {
        try {
            isPrepared = false;

            File dir = new File(mDirString);
            LogUtil.d("md:"+mDirString);
            String fileNameString = generalFileName();
            File file = new File(dir, fileNameString);
            mCurrentFilePathString = file.getAbsolutePath();
            mRecorder = new MediaRecorder();
            // 设置meidaRecorder的音频源是麦克风
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //设置文件的输出格式
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.WAVE);
            mRecorder.setAudioSamplingRate(44100);
            //设置audio的编码格式
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            //设置编码频率
            mRecorder.setAudioEncodingBitRate(96000);
            // 设置输出文件
            mRecorder.setOutputFile(file.getAbsolutePath());
            // 严格遵守google官方api给出的mediaRecorder的状态流程图
            mRecorder.prepare();

            mRecorder.start();

            if (mListener != null) {
                mListener.wellPrepared();
            }
            isPrepared = true;

        } catch (IllegalStateException e) {
            e.printStackTrace();
            if (handler != null) {
                handler.sendEmptyMessage(MSG_ERROR_AUDIO_RECORD);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (handler != null) {
                handler.sendEmptyMessage(MSG_ERROR_AUDIO_RECORD);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (handler != null) {
                handler.sendEmptyMessage(MSG_ERROR_AUDIO_RECORD);
            }
        }

    }

    /**
     * 随机生成文件的名称
     *
     * @return
     */
    private String generalFileName() {
        return String.valueOf(System.currentTimeMillis());
    }

    private int vocAuthority[] = new int[10];
    private int vocNum = 0;
    private boolean check = true;

    // 获得声音的level
    public int getVoiceLevel(int maxLevel) {
        // mRecorder.getMaxAmplitude()这个是音频的振幅范围，值域是0-32767
        if (isPrepared) {
            try {
                int vocLevel = mRecorder.getMaxAmplitude();
                if (check) {
                    if (vocNum >= 10) {
                        Set<Integer> set = new HashSet<Integer>();
                        for (int i = 0; i < vocNum; i++) {
                            set.add(vocAuthority[i]);
                        }
                        if (set.size() == 1) {
                            if (handler != null)
                                handler.sendEmptyMessage(MSG_ERROR_AUDIO_RECORD);
                            vocNum = 0;
                            vocAuthority = null;
                            vocAuthority = new int[10];
                        } else {
                            check = false;
                        }
                    } else {
                        vocAuthority[vocNum] = vocLevel;
                        vocNum++;
                    }
                }
                return maxLevel * vocLevel / 32768 + 1;
            } catch (Exception e) {
                if (handler != null)
                    handler.sendEmptyMessage(MSG_ERROR_AUDIO_RECORD);
            }
        }

        return 1;
    }

    // 释放资源
    public void release() {
        // 严格按照api流程进行
        if (null != mRecorder) {
            isPrepared = false;
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mRecorder = null;
        }
    }

    public void cancel() {
        release();
        if (mCurrentFilePathString != null) {
            File file = new File(mCurrentFilePathString);
            file.delete();
            mCurrentFilePathString = null;
        }

    }

    public String getCurrentFilePath() {
        return mCurrentFilePathString;
    }

}
