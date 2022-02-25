package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import butterknife.BindView;

public class SunMi_LEDActivity extends BaseActivity implements View.OnClickListener
        , PromptDialog.OnPromptDialogCallBack {
    private SunMi_LEDActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    @BindView(R.id.led_root_view)
    public LinearLayout mRootView;
    @BindView(R.id.content_show)
    public TextView textView;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private Handler mDelayHandler = null;
    private Runnable mDelayRunnable = null;
    private String SET_IndicatorLight_ACTION_off = "ACTION_SET_INDICATOR_LIGHT_OFF";
    private String SET_IndicatorLight_ACTION_on = "ACTION_SET_INDICATOR_LIGHT_ON";
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    private boolean isMT535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");

    private int currPosition = 0;
    private int[] ids = {R.string.Blue,R.string.Yellow, R.string.Green, R.string.Red};

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sunmi_led;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.run_in_led);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        Intent intent_led_off = new Intent();
        intent_led_off.setAction(SET_IndicatorLight_ACTION_off);
        intent_led_off.putExtra("color_indicator_one", "blue");
        intent_led_off.putExtra("color_indicator_two", "yellow");
        intent_led_off.putExtra("color_indicator_three", "green");
        intent_led_off.putExtra("color_indicator_four", "red");
        sendBroadcast(intent_led_off);

        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mConfigResult = getResources().getInteger(R.integer.lcd_rgb_default_config_standard_result);
        mRun = new Runnable() {
            @Override
            public void run() {
                textView.setText(getResources().getString(ids[currPosition]));
                currPosition++;
                mHandler.sendEmptyMessage(1002);
                if (currPosition == 4){
                    currPosition = 0;
                    mSuccess.setVisibility(View.VISIBLE);
                    mFail.setVisibility(View.VISIBLE);
                }
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if (mConfigResult >= 1) {
                        deInit(mFatherName, SUCCESS);
                    } else {
                        deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    }
                    break;
                case 1002:
                    if (currPosition==1) {
                        Intent intent_led_on = new Intent();
                        intent_led_on.setAction(SET_IndicatorLight_ACTION_on);
                        intent_led_on.putExtra("color_indicator_one", "blue");
                        sendBroadcast(intent_led_on);
                        Intent intent_led_off = new Intent();
                        intent_led_off.setAction(SET_IndicatorLight_ACTION_off);
                        intent_led_off.putExtra("color_indicator_two", "yellow");
                        intent_led_off.putExtra("color_indicator_three", "green");
                        intent_led_off.putExtra("color_indicator_four", "red");
                        sendBroadcast(intent_led_off);
                    } else if (currPosition==2) {
                        Intent intent_led_off = new Intent();
                        intent_led_off.setAction(SET_IndicatorLight_ACTION_off);
                        intent_led_off.putExtra("color_indicator_one", "blue");
                        intent_led_off.putExtra("color_indicator_three", "green");
                        intent_led_off.putExtra("color_indicator_four", "red");
                        sendBroadcast(intent_led_off);
                        Intent intent_led_on = new Intent();
                        intent_led_on.setAction(SET_IndicatorLight_ACTION_on);
                        intent_led_on.putExtra("color_indicator_two", "yellow");
                        sendBroadcast(intent_led_on);
                    } else if (currPosition==3) {
                        Intent intent_led_on = new Intent();
                        intent_led_on.setAction(SET_IndicatorLight_ACTION_on);
                        intent_led_on.putExtra("color_indicator_three", "green");
                        sendBroadcast(intent_led_on);
                        Intent intent_led_off = new Intent();
                        intent_led_off.setAction(SET_IndicatorLight_ACTION_off);
                        if(isMT535){
                            intent_led_off.putExtra("color_indicator_one", "blue");
                            intent_led_off.putExtra("color_indicator_two", "yellow");
                            intent_led_off.putExtra("color_indicator_four", "red");
                        }
                        String cmdline_redon=readFile("/proc/cmdline");
                        if((cmdline_redon!=null)&&(!cmdline_redon.equals(""))){
                            if(cmdline_redon.contains("hwboard.id=0")){
                                intent_led_off.putExtra("color_indicator_one", "blue");
                                intent_led_off.putExtra("color_indicator_two", "yellow");
                                intent_led_off.putExtra("color_indicator_four", "red");
                            }
                            if(cmdline_redon.contains("hwboard.id=1")){
                                intent_led_off.putExtra("color_indicator_one", "blue");
                                //intent_led_off.putExtra("color_indicator_two", "yellow");
                                intent_led_off.putExtra("color_indicator_four", "red");
                            }
                        }

                        sendBroadcast(intent_led_off);
                    } else if (currPosition==0) {
                        Intent intent_led_on = new Intent();
                        intent_led_on.setAction(SET_IndicatorLight_ACTION_on);
                        intent_led_on.putExtra("color_indicator_four", "red");
                        sendBroadcast(intent_led_on);
                        Intent intent_led_off = new Intent();
                        intent_led_off.setAction(SET_IndicatorLight_ACTION_off);
                        if(isMT535){
                            intent_led_off.putExtra("color_indicator_one", "blue");
                            intent_led_off.putExtra("color_indicator_two", "yellow");
                            intent_led_off.putExtra("color_indicator_three", "green");
                        }
                        String cmdline_redon=readFile("/proc/cmdline");
                        if((cmdline_redon!=null)&&(!cmdline_redon.equals(""))){
                            if(cmdline_redon.contains("hwboard.id=0")){
                                intent_led_off.putExtra("color_indicator_one", "blue");
                                intent_led_off.putExtra("color_indicator_two", "yellow");
                                intent_led_off.putExtra("color_indicator_three", "green");
                            }
                            if(cmdline_redon.contains("hwboard.id=1")){
                                intent_led_off.putExtra("color_indicator_one", "blue");
                                //intent_led_off.putExtra("color_indicator_two", "yellow");
                                intent_led_off.putExtra("color_indicator_three", "green");
                            }
                        }

                        sendBroadcast(intent_led_off);
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDelayHandler != null) {
            mDelayHandler.removeCallbacks(mDelayRunnable);
            mDelayHandler = null;
        }
        Intent intent_led_off = new Intent();
        intent_led_off.setAction(SET_IndicatorLight_ACTION_off);
        intent_led_off.putExtra("color_indicator_one", "blue");
        intent_led_off.putExtra("color_indicator_two", "yellow");
        intent_led_off.putExtra("color_indicator_three", "green");
        intent_led_off.putExtra("color_indicator_four", "red");
        sendBroadcast(intent_led_off);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            if (mDelayHandler != null) {
                mDelayHandler.removeCallbacks(mDelayRunnable);
                mDelayHandler = null;
            }
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            if (mDelayHandler != null) {
                mDelayHandler.removeCallbacks(mDelayRunnable);
                mDelayHandler = null;
            }
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

    public static String readFile(String path) {
        return readFile(path.substring(0, path.lastIndexOf('/')), path.substring(path.lastIndexOf('/')+1));
    }

    public static String readFile(String filePath, String fileName) {
        String result = "";
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream bos = null;
        try {
            File file = new File(filePath, fileName);
            if (!file.exists()) {
                return null;
            }
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int len = bis.read(b);
            while (len != -1) {
                bos.write(b, 0, len);
                len = bis.read(b);
            }
            result = new String(bos.toByteArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null)bos.close();
                if (bis != null)bis.close();
                if (fis != null)fis.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
