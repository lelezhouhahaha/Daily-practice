package com.swfp.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.sunwave.utils.MsgType;
import com.swfp.app.FpContext;
import com.swfp.device.DeviceManager;
import com.swfp.device.MessageCallBack;
import com.swfp.exception.SunwaveConnectException;
import com.swfp.factory.R;
import com.swfp.utils.Utils;

import java.nio.ByteBuffer;

/**
 * Created by zhouj on 2017/6/5.
 */

public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "sw-BaseActivity";

    public static final int RET_CLOSE = -1;

    protected DeviceManager manager;
    protected boolean isConnected = false;
    protected boolean isCoating;
    protected int mIcId = 0;
    protected int image_w = 112;
    protected int image_h = 112;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);

        manager = FpContext.getContext().getDeviceManager();
        apkEnter();
        MessageCallBack callBack = getMessageCallBack();
        if (callBack != null) {
            manager.registerCallBack(callBack);
        }
        try {
            manager.connect();

            isConnected = true;

            if(checkChip() < 0){
                onConnectError();
            }else{
                initIcConfig();
                mIcId = getIcId();
                //initImgWidthAndHeight();
            }
        } catch (SunwaveConnectException e) {
            if (onConnectError() == RET_CLOSE) {
                showDialog();
            }
        }
    }

    protected abstract MessageCallBack getMessageCallBack();

    private void initIcConfig() {
        int[] w = new int[1];
        int[] h = new int[1];
        int err = manager.getICSize(w, h);
        if (err == 0) {
            image_w = w[0];
            image_h = h[0];
            Log.i(TAG, "image_w = " + image_w + " image_h = " + image_h);
        } else {
            Log.e(TAG, "getICSize eror ");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * when connect error
     * @return if return RET_CLOSE(-1) will show a dialog and shutdown application
     */
    protected int onConnectError() {
        return RET_CLOSE;
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.text_connect_error_tips)
                .setPositiveButton(R.string.text_conform, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Process.killProcess(Process.myPid());
                        System.exit(1);
                    }
                })
                .setCancelable(false);
        builder.show();
    }

    protected void beforeDisconnect() {

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnected) {
            manager.reset();
            beforeDisconnect();
            manager.disConnect();
        }
        apkExit();
    }

    protected void getId(){
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        int ret =  manager.sendCmd(MsgType.FP_MSG_GET_ID, 0, buf, len);
        if(ret==0){
            if(len[0]<8){
                int[] arr = Utils.byteArray2IntArray(buf, len[0]);
                String hexId = Integer.toHexString(arr[0]);
                Log.d(TAG,"id = "+hexId);
            }else{
                return;
            }
        }
    }

    protected int getIcId(){
        int icid = 0;
        byte[] buf = new byte[4];
        int[] len = new int[1];
        len[0] = buf.length;
        int ret =  manager.sendCmd(MsgType.FP_MSG_GET_MODEL_ID, 0, buf, len);
        if(ret==0){
            icid = (buf[0] & 0xff) | ((buf[1] << 8) & 0xff00)
                    | ((buf[2] << 24) >>> 8) | (buf[3] << 24);
            String hexId = Integer.toHexString(icid);  //hexId = 8231
            Log.d(TAG,"icid = "+icid+"<--->hex_icid = "+hexId);
        }
        return icid;
    }

    protected void apkEnter(){
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        manager.sendCmd(MsgType.FP_MSG_TEST_FACTORY_APK_ENTER, 0, buf, len);
    }

    protected void apkExit(){
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        manager.sendCmd(MsgType.FP_MSG_TEST_FACTORY_APK_EXIT, 0, buf, len);
    }

    public static final int FP_MSG_TEST_READ_FINGER_UNKNOWN   = 0;
    public static final int FP_MSG_TEST_READ_TOUCH   = 1;
    public static final int FP_MSG_TEST_READ_LEAVE   = 2;
    protected int getFingerStatus(byte[]buf){
        int[] len = new int[1];
        len[0] = buf.length;
        int ret = manager.sendCmd(MsgType.FP_MSG_TEST_READ_FINGER, 0, buf, len); //buf[0] == 1(new,notify) buf[0] != 1(old,no-notify)
        return ret;
    }

    protected int checkChip(){
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        int ret =  manager.sendCmd(MsgType.FP_MSG_TEST_CHECK_CHIP, 0, buf, len);
        //return ret;
        return 1;
    }

    protected Bitmap convert8bitToBmp(byte[] data, int width, int height) {
        byte[] Bits = new byte[data.length * 4];

        int i;
        for (i = 0; i < data.length; i++) {
            Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = data[i];
            Bits[i * 4 + 3] = -1;
        }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmp;
    }

    protected Bitmap mirrorBmp(Bitmap originalBmp) {
        Bitmap newBitmap = Bitmap.createBitmap(originalBmp.getWidth(), originalBmp.getHeight(), originalBmp.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        matrix.postTranslate(originalBmp.getWidth(), 0);
        canvas.drawBitmap(originalBmp, matrix, paint);
        originalBmp.recycle();
        return newBitmap;
    }
}
