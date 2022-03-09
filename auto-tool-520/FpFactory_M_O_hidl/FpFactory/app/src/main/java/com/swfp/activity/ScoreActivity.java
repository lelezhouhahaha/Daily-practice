package com.swfp.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sunwave.utils.MsgType;
import com.swfp.device.MessageCallBack;
import com.swfp.factory.R;
import com.swfp.utils.MessageType;
import com.swfp.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScoreActivity extends BaseActivity {
    protected static final String TAG = "sw-ScoreActivity";

    private ImageView mIvFingerPrint = null;

    private TextView mTvTips, mTvShowCalibration, mTvScoreQuality, mTvScoreArea;
    private Vibrator mVibrator;
    private final long[] mVbPattern = {10, 50};
    private TextView mTvSensitivity;
    private TextView mTvAgc;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_score);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mTvTips = (TextView) findViewById(R.id.text_identify_user);
        mTvTips.setText(getResources().getString(R.string.fingerdown));
        mTvShowCalibration = (TextView) findViewById(R.id.tv_show_cali);
        mTvScoreQuality = (TextView) findViewById(R.id.tv_score_quality);
        mTvScoreArea = (TextView) findViewById(R.id.tv_score_area);
        mTvSensitivity = (TextView) findViewById(R.id.tv_sensitivity);
        mTvAgc = (TextView) findViewById(R.id.tv_agc);
    }


    @Override
    protected MessageCallBack getMessageCallBack() {
        return new MessageCallBack() {
            @Override
            public void handMessage(int what, int arg1, int arg2) {
                Log.d(TAG, "main msg what = " + what + " arg1 = " + arg1 + " arg2 = " + arg2);
                mHandler.obtainMessage(what, arg1, arg2).sendToTarget();
            }
        };
    }

    private void initImageView() {
        mIvFingerPrint = (ImageView) findViewById(R.id.myview);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int scale = dm.widthPixels / image_h < dm.heightPixels / image_w ? dm.widthPixels / image_h : dm.heightPixels / image_w;

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mIvFingerPrint.getLayoutParams();
        layoutParams.width = image_h * scale * 2 / 3;
        layoutParams.height = image_w * scale * 2 / 3;
        mIvFingerPrint.setLayoutParams(layoutParams);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MessageType.FP_MSG_FINGER:
                    // wait touch
                    if (msg.arg1 == MessageType.FP_MSG_FINGER_WAIT_TOUCH) {
                        Log.v(TAG, "Fp:wait touch 1");
                        mTvTips.setText(getResources().getString(R.string.fingerdown));
                    } else if (msg.arg1 == MessageType.FP_MSG_FINGER_TOUCH) {

                    } else if (msg.arg1 == MessageType.FP_MSG_FINGER_LEAVE) {
                        // next bmp
                        mTvTips.setText(getResources().getString(R.string.fingerdown));
                        // 启动新的图像扫描
                        SystemClock.sleep(300);
                        manager.scanImage();
                    }
                    break;
                case MessageType.FP_MSG_TEST:
                    if (msg.arg1 == MessageType.FP_MSG_TEST_VALID_PIXNUM_AND_QUALITY) {
                        int quality = msg.arg2 & 0xffff;
                        int areaPrecent = 100 * (msg.arg2 >> 16 & 0xffff) / (image_w * image_h);
                        Log.d(TAG, "image pixnum:" + (msg.arg2 >> 16 & 0xffff) + " areaPrecent:" + areaPrecent + " image quality:" + (msg.arg2 & 0xffff));
                        mTvScoreQuality.setText(String.valueOf(quality));
                        mTvScoreArea.setText(String.valueOf(areaPrecent));
                    } else if (msg.arg1 == MessageType.FP_MSG_TEST_IMG_QUALITY) {
                        mVibrator.vibrate(mVbPattern, -1);
                        Log.v(TAG, "Fp:is touch");
                        mTvTips.setText(getResources().getString(R.string.fingerup));
                        int quality = msg.arg2 & 0xff;
                        int agc = (msg.arg2 & 0xff00) >> 8;
                        Log.d(TAG, "image quality:" + quality +" agc:"+agc);
                        if (quality > getResources().getInteger(R.integer.threshold_qulity) && agc==0) {
                            mTvAgc.setText("");
                        } else {
                            mTvAgc.setTextColor(Color.RED);
                            mTvAgc.setText(getResources().getString(R.string.test_agc_fail));
                        }
                        byte[] fingerBuf = new byte[image_w * image_h];
                        int err = manager.readImage(fingerBuf, image_w, image_h);
                        if (err == 0) {
//                            byte[] fings = Utils.translateImageCode(fingerBuf, image_w, image_h);
//                            Bitmap bitmap = BitmapFactory.decodeByteArray(fings, 0, fings.length);
//                            mIvFingerPrint.setImageBitmap(Utils.rotateBitmap(bitmap, 270));
                            Bitmap bitmap = convert8bitToBmp(fingerBuf,image_w,image_h);
                            mIvFingerPrint.setImageBitmap(Utils.rotateBitmap(mirrorBmp(bitmap), 90));
                        } else {
                            mIvFingerPrint.setImageBitmap(null);
                        }
                        waitFingerLeave();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        mVibrator.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isConnected) {

            byte[] buf = new byte[10];
            int[] len = new int[1];
            len[0] = 1;
            int ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_CALIBRATION_STATUS, 0, buf, len);
            if (ret == 0) {
                String text = buf[0] == 1 ? getResources().getString(R.string.yes) : getResources().getString(R.string.no);
                int color = buf[0] == 1 ? Color.BLUE : Color.RED;
                mTvShowCalibration.setText(text);
                mTvShowCalibration.setTextColor(color);
            }

            getSensitivity();

            initImageView();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    manager.scanImage();
                }
            }, 400);
        }
    }

    public void waitFingerLeave() {
        try {
            if (manager != null) {
                manager.waitLeave();
            }
        } catch (final Exception e) {
            Log.e(TAG, "waitFingerLeave error!");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //add by yxf for sunwave
    /**
     * 获取灵敏度
     */
    private void getSensitivity(){
        Log.d(TAG,"getSensitivity");
        String sensitivity = null;
        byte[] buf = new byte[2];
        int[] len = new int[1];
        len[0] = buf.length;
        int ret =  manager.sendCmd(MsgType.FP_MSG_TEST_CMD_SENSITIVITY, 0, buf, len);
        if(ret==0){
            Log.d(TAG,"buf[0] = "+buf[0]+" buf[1] = "+buf[1]);
            String r = Integer.toHexString(buf[0]);
            String c = Integer.toHexString(buf[1]);
            Log.d(TAG,"buf[0]Hex = "+ r+" buf[1]Hex = "+c);
            if("0".equals(r)){
                if(c.length()==1){
                    sensitivity = "0x0"+c;
                }else{
                    sensitivity = "0x"+c;
                }
            }else{
                if(r.length()==1){
                    r = "0x0"+r;
                }else{
                    r = "0x"+r;
                }

                if(c.length()==1){
                    c = "0x0"+c;
                }else{
                    c = "0x"+c;
                }
                sensitivity = "r = "+r+", c = "+c;
            }
        }else{
            sensitivity = null;
        }
        mTvSensitivity.setText(getResources().getString(R.string.sensitivity)+sensitivity);
    }
    //add by yxf end

}
