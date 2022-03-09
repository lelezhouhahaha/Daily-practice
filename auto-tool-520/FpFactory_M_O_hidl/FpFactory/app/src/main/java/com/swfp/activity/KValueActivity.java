package com.swfp.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.swfp.device.MessageCallBack;
import com.swfp.factory.R;
import com.swfp.utils.MessageType;
import com.swfp.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class KValueActivity extends BaseActivity {
    protected static final String TAG = "sw-KValueActivity";

    private ImageView mIvFingerPrint = null;
    private TextView mTvTips, mTvShowCalibration, mTvPath1, mTvPath2, mTvRange;

    private Vibrator mVibrator;
    private final long[] mVbPattern = {10, 50};
    private boolean isCalibration = false;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kvalue);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mTvTips = (TextView) findViewById(R.id.text_identify_user);
//        mTvTips.setText(getResources().getString(R.string.fingerdown));
        mTvShowCalibration = (TextView) findViewById(R.id.tv_show_cali);
        mTvPath1 = (TextView) findViewById(R.id.tv_path1);
        mTvPath2 = (TextView) findViewById(R.id.tv_path2);
        mTvRange = (TextView) findViewById(R.id.tv_range);
    }

    @Override
    protected MessageCallBack getMessageCallBack() {
        return null;
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

            mTvPath1.setText("");
            mTvPath2.setText("");
            mIvFingerPrint.setImageBitmap(null);

            byte[] buf = new byte[10];
            int[] len = new int[1];
            len[0] = 1;
            int ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_CALIBRATION_STATUS, 0, buf, len);
            if (ret == 0) {
                isCalibration = buf[0] == 1;
                String text = isCalibration ? getResources().getString(R.string.yes) : getResources().getString(R.string.no);
                int color = isCalibration ? Color.BLUE : Color.RED;
                mTvShowCalibration.setText(text);
                mTvShowCalibration.setTextColor(color);
            }

            if (isCalibration) {
                byte[] buffer = new byte[image_w * image_h * 4];
                int[] length = new int[1];
                length[0] = buffer.length;
                ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_CALIBRATION_KVALUE, 0, buffer, length);
                float[] fk = Utils.byteArray2floatArray(buffer, length[0] / 4);
                float max = fk[0], min = fk[0];
                for (int i = 1; i < fk.length; i++) {
                    if (max < fk[i]) {
                        max = fk[i];
                    }
                    if (min > fk[i]) {
                        min = fk[i];
                    }
                }
                mTvRange.setText(String.format(getResources().getString(R.string.text_krange), min, max));
                File file = new File(getSaveDir(), "kvalue.k");
                mTvPath1.setText(file.getAbsolutePath());
                Utils.saveByteArrayToLocal(buffer, length[0], file);
                byte[] fings = kvalueToBmp(fk, length[0] / 4);

//                byte[] bmp = Utils.translateImageCode(fings, image_w, image_h);
//                file = new File(getSaveDir(), "kvalue.bmp");
//                mTvPath2.setText(file.getAbsolutePath());
//                Utils.saveByteArrayToLocal(bmp, bmp.length, file);
//                final Bitmap bitmap = BitmapFactory.decodeByteArray(bmp, 0, bmp.length);
//                mIvFingerPrint.setImageBitmap(Utils.rotateBitmap(bitmap, 270));


                final Bitmap bitmap2 = convert8bitToBmp(fings,image_w,image_h);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] bmp2 = baos.toByteArray();
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                file = new File(getSaveDir(), "kvalue.bmp");
                mTvPath2.setText(file.getAbsolutePath());
                Utils.saveByteArrayToLocal(bmp2, bmp2.length, file);
                mIvFingerPrint.setImageBitmap(Utils.rotateBitmap(mirrorBmp(bitmap2), 90));
            }
        }
    }

    private File getSaveDir() {
        File file = new File(Environment.getExternalStorageDirectory(), "sunwave");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
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

    /**
     * 替换Utils.kvalueToBmp方法的实现
     * @param fkv
     * @param len
     * @return
     */
    private byte[] kvalueToBmp(float[] fkv, int len) {
        byte[] bmp = new byte[len];
        float kmin = fkv[0];
        float kmax = fkv[0];
        int i;
        float karea;

        for(i = 0; i < len; ++i) {
            if (kmin > fkv[i]) {
                kmin = fkv[i];
            }

            if (kmax < fkv[i]) {
                kmax = fkv[i];
            }
        }

        karea = (kmax - kmin) / 255;

        for(i = 0; i < len; ++i) {
            bmp[i] = (byte)((fkv[i] - kmin) / karea);
        }
        return bmp;
    }
}
