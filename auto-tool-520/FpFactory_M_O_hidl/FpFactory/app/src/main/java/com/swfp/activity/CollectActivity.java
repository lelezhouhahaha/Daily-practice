package com.swfp.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.swfp.device.MessageCallBack;
import com.swfp.factory.R;
import com.swfp.utils.MessageType;
import com.swfp.utils.Utils;

/**
 * Created by zhouj on 2017/4/5.
 */
public class CollectActivity extends BaseActivity {
    protected static final String TAG = "sw-CollectActivity";

    private ImageView mIvFingerPrint = null;

    private TextView mTvTips;
    private Vibrator mVibrator;
    private final long[] mVbPattern = {10, 50};


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_collect);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mTvTips = (TextView) findViewById(R.id.text_identify_user);
        mTvTips.setText(getResources().getString(R.string.fingerdown));
    }

    @Override
    protected MessageCallBack getMessageCallBack() {
        return new MessageCallBack() {
            @Override
            public void handMessage(int what, int arg1, int arg2) {
                Log.v(TAG, "main msg what = " + what + " arg1 = " + arg1 + " arg2 = " + arg2);
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
                        manager.scanImage();
                    }
                    break;
                case MessageType.FP_MSG_TEST:
                    if (msg.arg1 == MessageType.FP_MSG_TEST_IMG_QUALITY) {
                        Log.d(TAG, "image quality:" + msg.arg2);

                        mVibrator.vibrate(mVbPattern, -1);
                        Log.v(TAG, "Fp:is touch");
                        mTvTips.setText(getResources().getString(R.string.fingerup));

                        byte[] fingerBuf = new byte[image_w * image_h];
                        int err = manager.readImage(fingerBuf, image_w, image_h);
                        if (err == 0) {
//                            byte[] fings = Utils.translateImageCode(fingerBuf, image_w, image_h);
//                            final Bitmap bitmap = BitmapFactory.decodeByteArray(fings, 0, fings.length);
//                            mIvFingerPrint.setImageBitmap(Utils.rotateBitmap(bitmap, 270));

                            final Bitmap bitmap = convert8bitToBmp(fingerBuf,image_w,image_h);
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
}
