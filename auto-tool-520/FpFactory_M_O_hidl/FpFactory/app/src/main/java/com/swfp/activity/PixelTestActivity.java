package com.swfp.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sunwave.utils.MsgType;
import com.swfp.device.MessageCallBack;
import com.swfp.factory.R;
import com.swfp.utils.MessageType;
import com.swfp.utils.Utils;

public class PixelTestActivity extends BaseActivity {
    protected static final String TAG = "sw-PixelTestActivity";

    private int checkFingerStatusNum = 0;
    private ImageView mIvFingerPrint = null;

    private TextView tvUserIdentify;
    private TextView mTvTips;
    private String TEXT_BADPOINT;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pixel);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tvUserIdentify = (TextView) findViewById(R.id.text_identify_user);
        mTvTips = (TextView) findViewById(R.id.tv_tips);
        TEXT_BADPOINT = getResources().getString(R.string.pixel_bad_num);
    }

    @Override
    protected MessageCallBack getMessageCallBack() {
        return new MessageCallBack() {
            @Override
            public void handMessage(int what, int arg1, int arg2) {
                Log.d(TAG, "main msg what = " + what + "(0x" + Integer.toHexString(what) + ")" + " arg1 = "
                        + arg1 + "(0x" + Integer.toHexString(arg1) + ")" + " arg2 = " + arg2);
                mHandler.obtainMessage(what, arg1, arg2).sendToTarget();
            }
        };
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MessageType.FP_MSG_TEST:
                    if (msg.arg1 == MsgType.FP_MSG_TEST_READ_FINGER) {
                        Log.d(TAG,"FP_MSG_TEST_READ_FINGER status = "+msg.arg2);
                        if(msg.arg2 == FP_MSG_TEST_READ_LEAVE){ //finger leave
                            testPixel();
                        }else if(msg.arg2 == FP_MSG_TEST_READ_TOUCH){ //finger touch
                            checkFingerStatusNum++;
                            mHandler.postDelayed(checkFingerStatusRun,1000);
                            if(checkFingerStatusNum >= 3){
                                mHandler.removeCallbacks(checkFingerStatusRun);
                                mTvTips.setText(R.string.text_retest);
                                break;
                            }

                        }else{ //finger unknown

                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private Runnable checkFingerStatusRun = new Runnable() {
        @Override
        public void run() {
            byte[] buf = new byte[8];
            getFingerStatus(buf);
        }
    };

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

    @Override
    protected void onResume() {
        super.onResume();
        if (isConnected) {
            initImageView();
            if(mIcId == 0x8271 || mIcId == 0x8281 || mIcId == 0x8273 || mIcId == 0x8233 || mIcId == 0x8283){
                byte[] buf = new byte[8];
                getFingerStatus(buf);
                if(buf[0] != 1){ //no notify callback
                    testPixel();
                }
            }else{
                testPixel();
            }
        }
    }

    private void testPixel() {
        byte[] buf = new byte[4];
        int len[] = new int[1];
        len[0] = 4;
        int ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_PIXEL_NUM, 0, buf, len);
        if (ret == 0) {
            int arg = Utils.byteArray2Int(buf);
            int piexl = arg & 0xff;
            int block = (arg >> 8) & 0xff;
            Log.i(TAG, "get bad pixel info : " + ret +" arg="+arg+" piexl="+piexl+" block="+block);
            tvUserIdentify.setText(String.format(TEXT_BADPOINT, piexl, block));
        } else {
            tvUserIdentify.setText("");
            Log.e(TAG, "get bad pixel info error: " + ret);
        }

        buf = new byte[image_w * image_h];
        len = new int[1];
        len[0] = image_w * image_h;
        ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_PIXEL_IMG, 0, buf, len);
        if (ret == 0) {
            for (int i = 0; i < len[0]; i++) {
                if (buf[i] == 1) {
                    buf[i] = (byte) 255;
                }
            }
//            byte[] fings = Utils.translateImageCode(buf, image_w, image_h);
//            final Bitmap bitmap = BitmapFactory.decodeByteArray(fings, 0, fings.length);
//            mIvFingerPrint.setImageBitmap(Utils.rotateBitmap(bitmap, 270));
            final Bitmap bitmap = convert8bitToBmp(buf,image_w,image_h);
            mIvFingerPrint.setImageBitmap(Utils.rotateBitmap(mirrorBmp(bitmap), 90));
        } else {
            mIvFingerPrint.setImageBitmap(null);
            Log.e(TAG, "get bad pixel image error: " + ret);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mTvTips.setText(R.string.text_tips_untouch);
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
