package com.swfp.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sunwave.utils.MsgType;
import com.swfp.device.MessageCallBack;
import com.swfp.device.ReeDeviceManagerImpl;
import com.swfp.factory.R;
import com.swfp.utils.MessageType;
import com.swfp.utils.Utils;
import com.swfp.view.ItemView;

import java.util.ArrayList;
import java.util.List;

public class DetectActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "sw-DetectActivity";

    private static final float MAX_COE = 7.84186e-5f;
    private static final float MIN_COE = 9.63669e-7f;
    private static final float MAX_CV = 1e-10f;
    private static final float MIN_CV = -1e-10f;
    private static final int EDGE_ERROR_THR = 200;

    public static final int MSG_SHOW_COUNT_DOWN = 100;
    public static final int MSG_SHOW_COUNT_DOWN_ARG_ONE = 101;
    public static final int MSG_START_COUNT_DOWN = 995;
    public static final int MSG_CANCEL_COUNT_DOWN = 996;
    public static final int MSG_COUNT_DOWN = 997;
    private static final int MSG_TEST_RESULT = 998;
    public static final int MSG_NEXT_TASK = 999;
    public static final int MSG_SPI = 1000;
    public static final int MSG_IRQ = 1001;
    public static final int MSG_PIXEL = 1002;
    public static final int MSG_CALIBRATION = 1003;
    public static final int MSG_PASS = 1;
    public static final int MSG_FAIL = 0;
    public static final int MSG_NOFUN = -1;
    public static final int WAIT_DELAY_TIME = 5*1000;
    private int wait_delay_time_order3 = WAIT_DELAY_TIME;
    public static final int WAIT_DELAY_LEAVE_AND_TOUCH_TIME = 3000;

    private ProgressDialog mProgressDialog;
    private Toast mToast;

    private ItemView itvSpi, itvIrq, itvPixel, itvCalibration, itvQuality, itvEnroll, itvAuth, itvSpeed, itvLeaveAndTouch;
    private TextView mTvTips, mTvCountDown;
    private Handler mTaskHandler;
    private List<Runnable> mListRuns;
    private Runnable mCancelRun, mCancleRunSecondFpTouch, mCancelLeaveRun, mCountDownRun;
    private CountDownRunnable mCountDownRunable;

    private int mFid = -1;
    private boolean isEnrolling = false;
    private int checkFingerStatusNum = 0;
    private int mAuthFailedCount = 0;
    private static final int MAX_AUTH_FAILED = 3;
    private boolean isEnrollOver = false;
    private boolean isTestFailed = false;
    private boolean isFirstTouch = false, isSecondTouch = false;
    private boolean isTestItvSpiFailed= false;
    private boolean isTestItvIrqFailed= false;
    private boolean isTestItvPixelFailed= false;
    private String g_Activity_Start_Type = "";
    private Button mBtStartTest;

    private int[] piexls;
    private int[] blocks;
    private int thresholdPixel, thresholdBlock;

    private boolean isCalibration = false;
    private float kTotalValue = 0.0f,kAverageValue = 0.0f;
    private float max, min;
    private float[] fk ;
    private short[] flashdata;
    private int e240Count = 0;
    private boolean need_touch_finger = true;
    private boolean result_by_broadcast = false;
    private boolean is_override_xml = false;
    /**
     * 第四项无限等待，不取消
     */
    private boolean need_delay_no_limit_order3 = false;
    private boolean isTimeout;

    private Handler initHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,"FpFactory Version:"+getPackVersion());
            Intent it = getIntent();
            if(it != null){
                need_touch_finger = it.getBooleanExtra("need_touch_finger",true);
                result_by_broadcast = it.getBooleanExtra("result_by_broadcast",false);
                wait_delay_time_order3 = it.getIntExtra("press_finger_delay",WAIT_DELAY_TIME);
                need_delay_no_limit_order3 = it.getBooleanExtra("need_delay_no_limit",false);
                is_override_xml = it.getBooleanExtra("is_override_xml",false);
            }
            initView();
            HandlerThread mHandlerThread = new HandlerThread("task");
            mHandlerThread.start();
            mTaskHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_NEXT_TASK:
                            Log.d(TAG, "mListRuns.size():" + mListRuns.size());
                            if (mListRuns != null && mListRuns.size() > 0) {
                                mTaskHandler.post(mListRuns.remove(0));
                            } else {
                                mHandler.obtainMessage(MSG_TEST_RESULT).sendToTarget();
                            }
                            break;
                        case MSG_COUNT_DOWN:
                            if(msg.arg1 == MSG_START_COUNT_DOWN){
                                mCountDownRunable = new CountDownRunnable(msg.arg2);
                                post(mCountDownRunable);
                            }
                            break;
                        case MSG_CANCEL_COUNT_DOWN:
                            if(mCountDownRunable != null){
                                removeCallbacks(mCountDownRunable);
                                mCountDownRunable = null;
                            }
                            break;
                        default:
                            break;
                    }
                }
            };
            if(isConnected){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initPiexlAndBlock();

                        if(mIcId == 0x8271 || mIcId == 0x8281 || mIcId == 0x8273 || mIcId == 0x8233 || mIcId == 0x8283 || mIcId == 0x8263 || mIcId == 0x8393 ){
                            byte[] buf = new byte[8];
                            getFingerStatus(buf);
                            if(buf[0] != 1){ //无回调
                                doNextJob();
                            }
                        }else{
                            doNextJob();
                        }
                    }
                },0);
            }

            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeDark);
        setContentView(R.layout.activity_detect);
        g_Activity_Start_Type = getIntent().getStringExtra("StartType");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initHandler.sendEmptyMessage(0);
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

    private void initView() {
        itvSpi = (ItemView) findViewById(R.id.itv_spi);
        itvIrq = (ItemView) findViewById(R.id.itv_irq);
        itvPixel = (ItemView) findViewById(R.id.itv_pixel);
        itvCalibration = (ItemView) findViewById(R.id.itv_calibration);
        itvQuality = (ItemView) findViewById(R.id.itv_quality);
        itvSpeed = (ItemView) findViewById(R.id.itv_speed);
        itvEnroll = (ItemView) findViewById(R.id.itv_enroll);
        itvAuth = (ItemView) findViewById(R.id.itv_auth);
        itvLeaveAndTouch = (ItemView) findViewById(R.id.itv_leave_and_touch);
        mTvTips = (TextView) findViewById(R.id.tv_tips);
        if("pcbaautotest".equals(g_Activity_Start_Type)){
            itvQuality.setVisibility(View.GONE);
            mTvTips.setVisibility(View.GONE);
        }

        mTvCountDown = (TextView) findViewById(R.id.tv_count_down);
        mBtStartTest = (Button) findViewById(R.id.bt_start_test);
        mBtStartTest.setVisibility(View.GONE);
        mBtStartTest.setOnClickListener(this);

        mListRuns = new ArrayList<>();
        if (getResources().getBoolean(R.bool.is_test_spi)) {
            itvSpi.setVisibility(View.VISIBLE);
            mListRuns.add(mRunSpi);
        }
        if (getResources().getBoolean(R.bool.is_test_irq)) {
            itvIrq.setVisibility(View.VISIBLE);
            mListRuns.add(mRunIrq);
        }
        if (getResources().getBoolean(R.bool.is_test_pixel)) {
            itvPixel.setVisibility(View.VISIBLE);
            mListRuns.add(mRunPixel);
        }
        if (getResources().getBoolean(R.bool.is_test_calibration)) {
            itvCalibration.setVisibility(View.VISIBLE);
            mListRuns.add(mRunCalibration);
        }
        if (getResources().getBoolean(R.bool.is_test_leave_and_touch)) {
            itvLeaveAndTouch.setVisibility(View.VISIBLE);
            mListRuns.add(mRunFirstFpTouch);
        }
        if (getResources().getBoolean(R.bool.is_test_quality) && need_touch_finger) {
            if(!"pcbaautotest".equals(g_Activity_Start_Type)) {
                itvQuality.setVisibility(View.VISIBLE);
                mListRuns.add(mRunQuality);
            }
        }

        if (manager instanceof ReeDeviceManagerImpl) {
            //REE独有测试项

        } else {
            //TEE独有测试项
            if (getResources().getBoolean(R.bool.is_test_speed)) {
                itvSpeed.setVisibility(View.VISIBLE);
                mListRuns.add(mRunSpeed);
            }
            if (getResources().getBoolean(R.bool.is_test_enroll_and_auth)) {
                mListRuns.add(mRunEnroll);
                itvEnroll.setVisibility(View.VISIBLE);
                itvAuth.setVisibility(View.VISIBLE);
                mListRuns.add(mRunAuth);
            }
        }
    }

    @Override
    protected int onConnectError() {
        isTestFailed = true;

        itvSpi.setResult(ItemView.State.FAIL);
        itvIrq.setResult(ItemView.State.FAIL);
        itvPixel.setResult(ItemView.State.FAIL);
        itvCalibration.setResult(ItemView.State.FAIL);
        itvLeaveAndTouch.setResult(ItemView.State.FAIL);
        itvQuality.setResult(ItemView.State.FAIL);
        itvEnroll.setResult(ItemView.State.FAIL);
        itvAuth.setResult(ItemView.State.FAIL);
        itvSpeed.setResult(ItemView.State.FAIL);

        mHandler.obtainMessage(MSG_TEST_RESULT).sendToTarget();

        return 0;
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
    }

    @Override
    protected void beforeDisconnect() {
        if (isConnected) {
            sleep(50);
            manager.sendCmd(MessageType.FP_MSG_TEST_CMD_START_TEST, 0);
            sleep(20);
            cancelDelayRun();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause");
        super.onPause();
//        finish();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setResultForCit(boolean isTestFailed){
        Intent intent = new Intent();
        int result = isTestFailed ? 1 : 2;
        Log.d(TAG, "setResultForCit result:" + result);
        intent.putExtra("results", result);
        setResult(1000,intent);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,"msg.what = "+msg.what+" msg.arg1 = "+msg.arg1+" msg.arg2="+msg.arg2);
            switch (msg.what) {
                case MSG_SHOW_COUNT_DOWN:
                    if(msg.arg1 == MSG_SHOW_COUNT_DOWN_ARG_ONE){
                        mTvCountDown.setText(String.format(getResources().getString(R.string.title_count_down), String.valueOf (msg.arg2)));
                    }
                    break;
                case MSG_TEST_RESULT:
                    Intent intent = new Intent();
                    Log.d(TAG,"fpFactory Version2 "+getPackVersion());
                    intent.putExtra("timeout", isTimeout ? 1 : 0);
                    if(is_override_xml){
                        if(result_by_broadcast){
                            Log.d(TAG,"sendBoradCast test_result "+(isTestFailed ?"failed" : "success"));
                            Log.d(TAG,"action -"+getResources().getString(R.string.action_test_result)+"-");
                            Log.d(TAG,"action --------"+getResources().getString(R.string.action_test_result)+"---------");
                            intent.putExtra("test_result", isTestFailed ? "failed" : "success");
                            intent.setAction(getResources().getString(R.string.action_test_result));
                            DetectActivity.this.sendBroadcast(intent);
                            setResultForCit(isTestFailed);
                        }else{
                            Log.d(TAG,"resultCode "+(isTestFailed ? 999 : 0));
                            //intent.putExtra("value", isTestFailed ? 1 : 0);
                            setResultForCit(isTestFailed);
                        }
                    }else{
                        if(getResources().getBoolean(R.bool.is_return_result_by_broadcast)){
                            //兼容以前的配置
                            Log.d(TAG,"sendBoradCast test_result "+(isTestFailed ? "failed" : "success"));
                            Log.d(TAG,"action -"+getResources().getString(R.string.action_test_result)+"-");
                            Log.d(TAG,"action -1"+getResources().getString(R.string.action_test_result)+"1-");
                            intent.putExtra("test_result", isTestFailed ? "failed" : "success");
                            intent.setAction(getResources().getString(R.string.action_test_result));
                            DetectActivity.this.sendBroadcast(intent);
                            setResultForCit(isTestFailed);
                        }else{
                            Log.d(TAG,"resultCode "+(isTestFailed ? 999 : 0));
                            //intent.putExtra("value", isTestFailed ? 1 : 0);
                            setResultForCit(isTestFailed);
                        }
                    }
//before
//                    Intent intent = new Intent();
//                    intent.putExtra("test_result", isTestFailed ? "failed" : "success");
//                    intent.setAction(getResources().getString(R.string.action_test_result));
//                    DetectActivity.this.sendBroadcast(intent);
//                    intent.putExtra("value", isTestFailed ? 1 : 0);
//                    if(isTestFailed){
//                        setResult(999,intent);
//                    }else{
//                        setResult(0,intent);
//                    }

                    if (mToast != null) {
                        mToast.cancel();
                        mToast = null;
                    }
                    mToast = Toast.makeText(DetectActivity.this, isTestFailed ? "failed" : "success", Toast.LENGTH_LONG);
                    mToast.show();
                    //测试完成需要finish的可以在这添加代码
                    finish();
                    break;
                case MSG_SPI:
                    if (msg.arg1 == MSG_PASS) {
                        itvSpi.setResult(ItemView.State.PASS);
                    } else {
                        itvSpi.setResult(ItemView.State.FAIL);
                        isTestFailed = true;
                    }
                    doNextJob();
                    break;
                case MSG_IRQ:
                    if (msg.arg1 == MSG_PASS) {
                        itvIrq.setResult(ItemView.State.PASS);
                    } else {
                        itvIrq.setResult(ItemView.State.FAIL);
                        isTestFailed = true;
                    }
                    doNextJob();
                    break;
                case MSG_PIXEL:
                    if (msg.arg1 == MSG_PASS) {
                        itvPixel.setResult(ItemView.State.PASS);
                    } else {
                        itvPixel.setResult(ItemView.State.FAIL);
                        isTestFailed = true;
                    }
                    doNextJob();
                    break;
                case MSG_CALIBRATION:
                    if (msg.arg1 == MSG_PASS) {
                        itvCalibration.setResult(ItemView.State.PASS);
                    } else {
                        itvCalibration.setResult(ItemView.State.FAIL);
                        isTestFailed = true;
                    }
                    doNextJob();
                    break;
                case MessageType.FP_MSG_FINGER:
                    if (msg.arg1 == MsgType.FP_MSG_TEST_CMD_FP_LEAVE) {
                        if (mCancelLeaveRun != null) {
                            mHandler.removeCallbacks(mCancelLeaveRun);
                            mCancelLeaveRun = null;
                        }
                        mTaskHandler.obtainMessage(MSG_CANCEL_COUNT_DOWN).sendToTarget();
                        byte[] buf = new byte[4];
                        int[] len = new int[1];
                        setLeaveAndTouchBuf(buf, len);
                        isSecondTouch = true;
                        mCancleRunSecondFpTouch = new Runnable() {
                            @Override
                            public void run() {
                                itvLeaveAndTouch.setResult(ItemView.State.FAIL);
                                isTestFailed = true;
                                doNextJob();
                            }
                        };
                        mCountDownRun = new Runnable() {
                            @Override
                            public void run() {
                                mTvCountDown.setVisibility(View.VISIBLE);
                                mTaskHandler.obtainMessage(MSG_COUNT_DOWN,MSG_START_COUNT_DOWN,WAIT_DELAY_LEAVE_AND_TOUCH_TIME/1000).sendToTarget();
                            }
                        };
                        mHandler.post(mCountDownRun);
                        mHandler.postDelayed(mCancleRunSecondFpTouch, WAIT_DELAY_LEAVE_AND_TOUCH_TIME);
                        int ret = manager.sendCmd(MsgType.FP_MSG_TEST_CMD_FP_TOUCH, 0, buf, len);
                        mTvTips.setText(R.string.text_tips_touch);
                        if(ret == -1){
                            if (mCancleRunSecondFpTouch != null) {
                                mHandler.removeCallbacks(mCancleRunSecondFpTouch);
                                mCancleRunSecondFpTouch = null;
                            }
                            mTaskHandler.obtainMessage(MSG_CANCEL_COUNT_DOWN).sendToTarget();
                            itvLeaveAndTouch.setResult(ItemView.State.NOFUN);
                            doNextJob();
                        }
                    }else if (msg.arg1 == MsgType.FP_MSG_TEST_CMD_FP_TOUCH) {
                        if(isFirstTouch){
                            isFirstTouch = false;
                            if (mRunFpLeave != null) {
                                mHandler.removeCallbacks(mRunFpLeave);
                                mRunFpLeave = null;
                            }
                            mTaskHandler.obtainMessage(MSG_CANCEL_COUNT_DOWN).sendToTarget();
                            itvLeaveAndTouch.setResult(ItemView.State.FAIL);
                            isTestFailed = true;
                            doNextJob();
                        }
                        if(isSecondTouch){
                            if (mCancleRunSecondFpTouch != null) {
                                mHandler.removeCallbacks(mCancleRunSecondFpTouch);
                                mCancleRunSecondFpTouch = null;
                            }
                            mTaskHandler.obtainMessage(MSG_CANCEL_COUNT_DOWN).sendToTarget();
                            itvLeaveAndTouch.setResult(ItemView.State.PASS);
                            doNextJob();
                        }
                    }
                    break;
                case MessageType.FP_MSG_TEST:
                    if (msg.arg1 == MessageType.FP_MSG_TEST_IMG_QUALITY) {
                        cancelDelayRun();
                        int quality = msg.arg2 & 0xff;
                        int agc = (msg.arg2 & 0xff00) >> 8;
                        Log.d(TAG, "image quality:" + quality +" agc:"+agc);
                        if (quality > getResources().getInteger(R.integer.threshold_qulity) && agc==0) {
                            itvQuality.setResult(ItemView.State.PASS);
                        } else {
                            itvQuality.setResult(ItemView.State.FAIL);
                            isTestFailed = true;
                        }
                        mTvTips.setText("");
                        doNextJob();
                    } else if (msg.arg1 == MessageType.FP_MSG_TEST_CMD_PIXEL) {
                        if(msg.arg2 < 0){ // -108 is OK. New HAL will not return -108.
                            mHandler.obtainMessage(MSG_PIXEL, MSG_NOFUN, 0).sendToTarget();
                        }else{
                            int piexl = msg.arg2 & 0xff;
                            int block = (msg.arg2 >> 8) & 0xff;
                            Log.d(TAG, "bad point:" + piexl + "  bad block: " + block);
                            if (msg.arg2 == 0xffff) {
                                mHandler.obtainMessage(MSG_PIXEL, MSG_NOFUN, 0).sendToTarget();
                            } else if (piexl > thresholdPixel || block > thresholdBlock) {
                                mHandler.obtainMessage(MSG_PIXEL, MSG_FAIL, 0).sendToTarget();
                            } else {
                                mHandler.obtainMessage(MSG_PIXEL, MSG_PASS, 0).sendToTarget();
                            }
                        }
                    } else if (msg.arg1 == MessageType.FP_MSG_TEST_SPEED) {
                        cancelDelayRun();
                        Log.d(TAG, "speed:" + msg.arg2);
                        if (msg.arg2 <= getResources().getInteger(R.integer.threshold_speed)) {
                            itvSpeed.setResult(ItemView.State.PASS);
                        } else {
                            itvSpeed.setResult(ItemView.State.FAIL);
                            isTestFailed = true;
                        }

                        doNextJob();
                    }else if (msg.arg1 == MsgType.FP_MSG_TEST_READ_FINGER){
                        Log.d(TAG,"FP_MSG_TEST_READ_FINGER status = "+msg.arg2);
                        if(msg.arg2 == FP_MSG_TEST_READ_LEAVE){ //finger leave
                            if (isConnected) {
                                doNextJob();
                            }
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
                case MessageType.FP_MSG_TEST_RESULT_ENROLL:
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.setProgress(100 - msg.arg2);
                    }
                    if (msg.arg2 == 0) {
                        closeDialog();
                        itvEnroll.setResult(ItemView.State.PASS);
                        isEnrolling = false;
                        mFid = msg.arg1;

                        doNextJob();
                    }
                    break;
                case MessageType.FP_MSG_TEST_RESULT_ON_AUTHENTICATED:
                    if (mAuthFailedCount >= MAX_AUTH_FAILED) {
                        manager.reset();
                        return;
                    }
                    if (msg.arg1 == 1) {
                        cancelDelayRun();
                        isEnrollOver = true;
                        itvAuth.setResult(ItemView.State.PASS);
                        mTvTips.setText("");
                        manager.reset();
                        if (mFid > 0) {
                            removeFinger(mFid);
                            mFid = -1;
                        }
                        doNextJob();
                    } else {
                        mAuthFailedCount++;
                        if (mToast != null) {
                            mToast.cancel();
                            mToast = null;
                        }
                        mToast = Toast.makeText(DetectActivity.this, (mAuthFailedCount >= MAX_AUTH_FAILED) ? R.string.text_tips_auth_failed : R.string.text_tips_retry, Toast.LENGTH_LONG);
                        mToast.show();
                        if (mAuthFailedCount >= MAX_AUTH_FAILED) {
                            cancelDelayRun();
                            isEnrollOver = true;
                            itvAuth.setResult(ItemView.State.FAIL);
                            isTestFailed = true;
                            mTvTips.setText("");
                            manager.reset();
                            doNextJob();
                        }
                    }
                    break;
                case MessageType.FP_MSG_TEST_RESULT_ON_ERROR:
                    if (isEnrolling) {
                        cancelDelayRun();
                        closeDialog();
                        isEnrolling = false;
                        itvEnroll.setResult(ItemView.State.FAIL);
                        itvAuth.setResult(ItemView.State.FAIL);
                        isTestFailed = true;
                        mTvTips.setText("");
                        manager.sendCmd(MessageType.FP_MSG_TEST_CMD_START_TEST, 0);
                        doNextJob();
                    }
                    break;
                case MessageType.FP_MSG_TEST_RESULT_ON_REMOVED:
                    if (!isEnrollOver) {
                        manager.sendCmd(MessageType.FP_MSG_TEST_CMD_START_ENROLL, 0);
                        mTvTips.setText("");
                        isEnrolling = true;
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

    private void doNextJob() {
        mTaskHandler.obtainMessage(MSG_NEXT_TASK).sendToTarget();
    }

    private void cancelDelayRun() {
        if (mCancelRun != null) {
            mHandler.removeCallbacks(mCancelRun);
            mCancelRun = null;
        }
    }

    /**
     * 0--->coating
     * 1--->lens
     * 2--->coating-8271
     * 3--->coating-8281
     * 4--->coating-8273
     */
    private void initPiexlAndBlock(){
        if(getResources().getBoolean(R.bool.is_use_default_piexl_and_block)){
            piexls = getResources().getIntArray(R.array.use_default_piexl);
            blocks = getResources().getIntArray(R.array.use_default_block);
        }else{
            piexls = getResources().getIntArray(R.array.use_other_piexl);
            blocks = getResources().getIntArray(R.array.use_other_block);
        }
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        int ret =  manager.sendCmd(MsgType.FP_MSG_GET_COATING_FLAG, 0, buf, len);
        if(ret==0){
            isCoating = buf[0] == 1;
            if(isCoating){
                switch (mIcId){
                    case 0x8271:
                        thresholdPixel = piexls[2];
                        thresholdBlock = blocks[2];
                        break;
                    case 0x8281:
                        thresholdPixel = piexls[3];
                        thresholdBlock = blocks[3];
                        break;
                    case 0x8273:
                        thresholdPixel = piexls[4];
                        thresholdBlock = blocks[4];
                        break;
                    case 0x8233:
                        thresholdPixel = piexls[5];
                        thresholdBlock = blocks[5];
                        break;
                    case 0x8263:
                        thresholdPixel = piexls[6];
                        thresholdBlock = blocks[6];
                        break;
                    case 0x8393:
                        thresholdPixel = piexls[7];
                        thresholdBlock = blocks[7];
                        break;
                    default:
                        thresholdPixel = piexls[0];
                        thresholdBlock = blocks[0];
                        break;
                }
            }else{
                thresholdPixel = piexls[1];
                thresholdBlock = blocks[1];
            }
        }else{
            thresholdPixel = getResources().getInteger(R.integer.threshold_pixel);
            thresholdBlock = getResources().getInteger(R.integer.threshold_block);
        }
        Log.d(TAG,"icid = "+Integer.toHexString(mIcId)+" ,thresholdPixel = "+thresholdPixel+" ,thresholdBlock = "+thresholdBlock+" ,isCoating = "+isCoating);
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

    /********************************************************/

    /**
     * order 0
     */
    private Runnable mRunSpi = new Runnable() {
        @Override
        public void run() {
            byte[] buf = new byte[8];
            int[] len = new int[1];
            len[0] = buf.length;
            int ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_SPI_RDWR, 0, buf, len); //spi: 147
            Log.d(TAG,"SPI result:ret "+ret +" buf[0] "+buf[0]);
            if (ret == 0 && buf[0] == 1) {

                mHandler.obtainMessage(MSG_SPI, MSG_PASS, 0).sendToTarget();
            } else {
                mHandler.obtainMessage(MSG_SPI, MSG_FAIL, 0).sendToTarget();
            }

        }
    };

    /**
     * order 1
     */
    private Runnable mRunIrq = new Runnable() {
        @Override
        public void run() {
            byte[] buf = new byte[8];
            int[] len = new int[1];
            len[0] = buf.length;
            int ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_IRQ, 0, buf, len); //irq: 148
            if (ret == 0 && buf[0] == 1) {
                mHandler.obtainMessage(MSG_IRQ, MSG_PASS, 0).sendToTarget();
            } else {
                mHandler.obtainMessage(MSG_IRQ, MSG_FAIL, 0).sendToTarget();
            }
        }
    };

    /**
     * order 2
     */
    private Runnable mRunPixel = new Runnable() {
        @Override
        public void run() {
            manager.sendCmd(MessageType.FP_MSG_TEST_CMD_PIXEL, 0); //pixel: 149
        }
    };

    /**
     * order 3
     */
    private Runnable mRunQuality = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getResources().getBoolean(R.bool.is_test_leave_and_touch)) {
                        mTvCountDown.setVisibility(View.GONE);
                    }
                    mTvTips.setText(R.string.text_tips_touch);
                }
            });
            manager.sendCmd(MessageType.FP_MSG_TEST_IMG_QUALITY, 0);

            mCancelRun = new Runnable() {
                @Override
                public void run() {
                    manager.reset();
                    itvQuality.setResult(ItemView.State.FAIL);
                    isTestFailed = true;
                    mTvTips.setText("");
                    isTimeout = true;
                    doNextJob();
                }
            };
            if(!need_delay_no_limit_order3){
                mHandler.postDelayed(mCancelRun, wait_delay_time_order3);
            }
        }
    };

    /**
     * order 4
     */
    private Runnable mRunSpeed = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvTips.setText(R.string.text_tips_touch);
                }
            });
            manager.sendCmd(MessageType.FP_MSG_TEST_SPEED, 0);
            mCancelRun = new Runnable() {
                @Override
                public void run() {
                    manager.reset();
                    itvSpeed.setResult(ItemView.State.FAIL);
                    isTestFailed = true;
                    mTvTips.setText("");

                    doNextJob();
                }
            };
            mHandler.postDelayed(mCancelRun, WAIT_DELAY_TIME);
        }
    };

    /**
     * order 5
     */
    private Runnable mRunEnroll = new Runnable() {
        @Override
        public void run() {
            createDialog(getString(R.string.title_test_input), getString(R.string.text_input_fingerprint));
            manager.sendCmd(MessageType.FP_MSG_TEST_CMD_START_TEST, 1);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    removeFinger(0);
                }
            }, 400);
        }
    };

    /**
     * order 6
     */
    private Runnable mRunAuth = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvTips.setText(R.string.text_tips_touch);
                }
            });
            manager.sendCmd(MessageType.FP_MSG_TEST_CMD_START_AUTH, 0);

            mCancelRun = new Runnable() {
                @Override
                public void run() {

                    manager.reset();
                    itvAuth.setResult(ItemView.State.FAIL);
                    isTestFailed = true;
                    mTvTips.setText("");

                    doNextJob();
                }
            };
            mHandler.postDelayed(mCancelRun, WAIT_DELAY_TIME);
        }
    };

    /**
     * order 7
     */
    private Runnable mRunCalibration = new Runnable() {
        @Override
        public void run() {
            //resultFromJni();
            //resultFromJava();
            resultFromJava2();
        }
    };


    /**
     * 手指离开和触摸检测
     */
    private Runnable mRunFirstFpTouch = new Runnable() {

        @Override
        public void run() {
            byte[] buf = new byte[4];
            int[] len = new int[1];
            setLeaveAndTouchBuf(buf, len);
            isFirstTouch = true;
            mCountDownRun = new Runnable() {
                @Override
                public void run() {
                    mTvCountDown.setVisibility(View.VISIBLE);
                    mTaskHandler.obtainMessage(MSG_COUNT_DOWN,MSG_START_COUNT_DOWN,WAIT_DELAY_LEAVE_AND_TOUCH_TIME/1000).sendToTarget();
                }
            };
            mHandler.postDelayed(mRunFpLeave, WAIT_DELAY_LEAVE_AND_TOUCH_TIME);
            mHandler.post(mCountDownRun);
            int ret = manager.sendCmd(MsgType.FP_MSG_TEST_CMD_FP_TOUCH, 0, buf, len);
            if(ret == -1){
                if (mRunFpLeave != null) {
                    mHandler.removeCallbacks(mRunFpLeave);
                    mRunFpLeave = null;
                }
                mTaskHandler.obtainMessage(MSG_CANCEL_COUNT_DOWN).sendToTarget();
                itvLeaveAndTouch.setResult(ItemView.State.NOFUN);
                doNextJob();
            }
        }
    };

    private Runnable mRunFpLeave = new Runnable() {
        @Override
        public void run() {
            isFirstTouch = false;
            byte[] buf = new byte[4];
            int[] len = new int[1];
            setLeaveAndTouchBuf(buf, len);
            mCancelLeaveRun = new Runnable() {
                @Override
                public void run() {
                    itvLeaveAndTouch.setResult(ItemView.State.FAIL);
                    isTestFailed = true;
                    doNextJob();
                }
            };
            mCountDownRun = new Runnable() {
                @Override
                public void run() {
                    mTvCountDown.setVisibility(View.VISIBLE);
                    mTaskHandler.obtainMessage(MSG_COUNT_DOWN,MSG_START_COUNT_DOWN,WAIT_DELAY_LEAVE_AND_TOUCH_TIME/1000).sendToTarget();
                }
            };
            mHandler.postDelayed(mCancelLeaveRun, WAIT_DELAY_LEAVE_AND_TOUCH_TIME);
            mHandler.post(mCountDownRun);
            int ret = manager.sendCmd(MsgType.FP_MSG_TEST_CMD_FP_LEAVE, 0, buf, len);
            if(ret == -1){
                if (mCancelLeaveRun != null) {
                    mHandler.removeCallbacks(mCancelLeaveRun);
                    mCancelLeaveRun = null;
                }
                mTaskHandler.obtainMessage(MSG_CANCEL_COUNT_DOWN).sendToTarget();
                itvLeaveAndTouch.setResult(ItemView.State.NOFUN);
                doNextJob();
            }
        }
    };

    private void setLeaveAndTouchBuf(byte[] buf, int[] len) {
        buf[0] = -62; //0xc2
        buf[1] = 9; //0x09
        buf[2] = -58; //0xc6
        buf[3] = 8; //0x08
        len[0] = buf.length;
    }

    class CountDownRunnable implements Runnable{
        int count = 0;
        public CountDownRunnable(int count){
            this.count = count;
        }
        @Override
        public void run() {
            while(count != 0){
                mHandler.obtainMessage(MSG_SHOW_COUNT_DOWN,MSG_SHOW_COUNT_DOWN_ARG_ONE,count).sendToTarget();
                SystemClock.sleep(1000);
                count--;
            }
        }
    }

    /********************* TEE ************************/
    /**
     * 移除指纹模板
     *
     * @param fingerId（0： 删除Group=1000的所有指纹模板）
     */
    public void removeFinger(int fingerId) {
        int groupId = 1000;
        byte[] buf = new byte[8];
        buf[0] = (byte) (fingerId & 0xff);
        buf[1] = (byte) (fingerId >> 8 & 0xff);
        buf[2] = (byte) (fingerId >> 16 & 0xff);
        buf[3] = (byte) (fingerId >> 24 & 0xff);
        buf[4] = (byte) (groupId & 0xff);
        buf[5] = (byte) (groupId >> 8 & 0xff);
        buf[6] = (byte) (groupId >> 16 & 0xff);
        buf[7] = (byte) (groupId >> 24 & 0xff);
        int[] len = new int[1];
        len[0] = buf.length;
        manager.sendCmd(MessageType.FP_MSG_TEST_CMD_REMOVE_FINGER, 0, buf, len);
    }

    private void closeDialog() {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mProgressDialog = null;
        }
    }

    private void createDialog(String title, String summary) {
        closeDialog();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(summary);
//        mProgressDialog.setIcon(R.drawable.ic_launcher);
        mProgressDialog.setProgress(0);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_start_test:
                if (isConnected) {
                    doNextJob();
                }
                v.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    private void resultFromJni(){
        byte[] buf = new byte[10];
        int[] len = new int[1];
        len[0] = 1;
        int ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_CALIBRATION_STATUS, 0, buf, len);
        if (ret == 0) {
            isCalibration = buf[0] == 1;
        }

        if (isCalibration) {
            mHandler.obtainMessage(MSG_CALIBRATION, MSG_PASS, 0).sendToTarget();
        } else {
            mHandler.obtainMessage(MSG_CALIBRATION, MSG_FAIL, 0).sendToTarget();
        }
    }

    private void resultFromJava(){
        byte[] buf = new byte[10];
        int[] len = new int[1];
        len[0] = 1;
        int ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_CALIBRATION_STATUS, 0, buf, len);
        if (ret == 0) {
            isCalibration = buf[0] == 1;
        }

        if (isCalibration) {
            byte[] buffer = new byte[image_w * image_h * 4];
            int[] length = new int[1];
            length[0] = buffer.length;
            ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_CALIBRATION_KVALUE, 0, buffer, length);
            fk = Utils.byteArray2floatArray(buffer, length[0] / 4);
            max = fk[0];
            min = fk[0];
            for (int i = 1; i < fk.length; i++) {
                kTotalValue += fk[i];
                if (max < fk[i]) {
                    max = fk[i];
                }
                if (min > fk[i]) {
                    min = fk[i];
                }
            }
            kAverageValue = kTotalValue/fk.length;
        }

        if(!isCalibration ||
                max > (1024.5f*MAX_COE) || max < (512*MIN_COE) ||
                min > (512*MAX_COE) || min < (-0.5f*MIN_COE) ||
                kAverageValue > (800*MAX_COE) || kAverageValue < (200*MIN_COE)){
            mHandler.obtainMessage(MSG_CALIBRATION, MSG_FAIL, 0).sendToTarget();
        }else{
            flashdata = new short[image_w * image_h];
            for (int i = 0; i < image_w * image_h; i++){
                flashdata[i] = (short) (((fk[i] - min) / (max -min)) * 255);
            }

            for (int c = 1; c < 8; ++c) {
                for(int r = 1; r < image_h-1; ++r){
                    if(flashdata[r*image_w+c] > 240){
                        e240Count++;
                    }
                }
            }

            for (int c = image_w-8; c < image_w-1; ++c) {
                for(int r = 1; r < image_h-1; ++r){
                    if(flashdata[r*image_w+c] > 240){
                        e240Count++;
                    }
                }
            }
            Toast.makeText(DetectActivity.this,"e240Count="+e240Count,Toast.LENGTH_SHORT).show();

            if(e240Count > EDGE_ERROR_THR){
                mHandler.obtainMessage(MSG_CALIBRATION, MSG_FAIL, 0).sendToTarget();
            }else{
                mHandler.obtainMessage(MSG_CALIBRATION, MSG_PASS, 0).sendToTarget();
            }
        }
    }

    private void resultFromJava2(){
        byte[] buf = new byte[10];
        int[] len = new int[1];
        len[0] = 1;
        int ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_CALIBRATION_STATUS, 0, buf, len);
        if (ret == 0) {
            isCalibration = buf[0] == 1;
        }

        if (isCalibration) {
            byte[] buffer = new byte[image_w * image_h * 4];
            int[] length = new int[1];
            length[0] = buffer.length;
            ret = manager.sendCmd(MessageType.FP_MSG_TEST_CMD_CALIBRATION_KVALUE, 0, buffer, length);
            fk = Utils.byteArray2floatArray(buffer, length[0] / 4);
            max = fk[0];
            min = fk[0];
            for (int i = 1; i < fk.length; i++) {
                kTotalValue += fk[i];
                if (max < fk[i]) {
                    max = fk[i];
                }
                if (min > fk[i]) {
                    min = fk[i];
                }
            }
            kAverageValue = kTotalValue/fk.length;
        }

        if(!isCalibration ||
                max > (1024.5f*MAX_COE) || max < (512*MIN_COE) ||
                min > (512*MAX_COE) || min < (-0.5f*MIN_COE) ||
                kAverageValue > (800*MAX_COE) || kAverageValue < (200*MIN_COE)){
            if(max < MAX_CV && min > MIN_CV){
                mHandler.obtainMessage(MSG_CALIBRATION, MSG_PASS, 0).sendToTarget();
            }else{
                mHandler.obtainMessage(MSG_CALIBRATION, MSG_FAIL, 0).sendToTarget();
            }
        }else{
            mHandler.obtainMessage(MSG_CALIBRATION, MSG_PASS, 0).sendToTarget();
        }
    }

    public String getPackVersion() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return getResources().getString(R.string.unknow_version);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG,"onKeyDown"+keyCode);
        if(keyCode == KeyEvent.KEYCODE_BACK){
            return true;
        }else{
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }
}
