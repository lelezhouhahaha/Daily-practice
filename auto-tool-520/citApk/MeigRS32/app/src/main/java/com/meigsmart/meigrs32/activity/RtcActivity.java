package com.meigsmart.meigrs32.activity;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.util.UUID;

import butterknife.BindView;

public class RtcActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack  {
    private RtcActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.rtc_text_status)
    public TextView mStatusText;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    private String mFatherName = "";
    private final int START_TEST_DELAY_TIME = 1000;
    private final int TEST_SLEEP_TIME = 2000;
    private int mCurrentDelayTime = 0;
    private final int MSG_TIMER = 0;
    private final int MSG_START_RTC = 1;
    private final int MSG_TEST_OVER = 2;
    private RtcHandler mHandler = new RtcHandler();
    private PowerManager mPowerManager = null;
    private KeyguardManager.KeyguardLock mKeyguardLock = null;
    private AlarmManager mAlarmManager = null;
    private PendingIntent mAlarmPendingIntent = null;
    private final String WAKEUP_FROM_RTC = "wake_up_frome_rtc";
    private boolean isWakeByRTC = false;
    private boolean isTestStarted = false;
    private boolean isFinishTest = false;
    private int mConfigTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        mAlarmManager=(AlarmManager)getSystemService(ALARM_SERVICE);
        mKeyguardLock = keyguardManager.newKeyguardLock(getClass().getName());
        mKeyguardLock.disableKeyguard();
        Intent alarmIntent=new Intent(this,getClass());
        alarmIntent.putExtra(WAKEUP_FROM_RTC,true);
        mAlarmPendingIntent= PendingIntent.getActivity(this, UUID.randomUUID().hashCode(),alarmIntent,PendingIntent.FLAG_UPDATE_CURRENT);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(isFinishTest){
            return;
        }
        isWakeByRTC = true;
        isFinishTest = true;
        mPowerManager.wakeUp(SystemClock.uptimeMillis());
        mStatusText.setText("PASS");
        Message msg = new Message();
        msg.what = MSG_TEST_OVER;
        msg.arg1 = 2;
        mHandler.sendMessageDelayed(msg,2000);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_rtc;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_rtc_test);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        mStatusText.setText(getLeftTime()+"");
        mHandler.sendEmptyMessage(MSG_TIMER);
    }


    private class RtcHandler extends android.os.Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_TIMER:
                    if(getLeftTime()>0&&!isTestStarted){
                        mStatusText.setText(getLeftTime()+"");
                        mHandler.sendEmptyMessageDelayed(MSG_TIMER,getResources().getInteger(R.integer.start_delay_time));
                    }else {
                        mHandler.sendEmptyMessage(MSG_START_RTC);
                        return;
                    }
                    if(mCurrentDelayTime<=9000)
                        mCurrentDelayTime+=getResources().getInteger(R.integer.start_delay_time);
                    break;
                case MSG_START_RTC:
                    startRtcTest();
                    break;
                case MSG_TEST_OVER:
                    deInit(mFatherName, msg.arg1);
            }
            super.handleMessage(msg);
        }
    }

    private void startRtcTest() {
        isTestStarted = true;
        mStatusText.setText(R.string.pcba_rtc_starting);
        LogUtil.w("====>start rtc test...");
        mAlarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+TEST_SLEEP_TIME,mAlarmPendingIntent);
        mPowerManager.goToSleep(SystemClock.uptimeMillis());
    }



    private int getLeftTime(){
        return (START_TEST_DELAY_TIME-mCurrentDelayTime)/1000;
    }




    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            //mSuccess.setTextColor(getResources().getColor(R.color.green_1));
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            //mFail.setTextColor(getResources().getColor(R.color.red_800));
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            mAlarmManager.cancel(mAlarmPendingIntent);
            mKeyguardLock.reenableKeyguard();
        }catch (Exception e){

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isTestStarted){
            if(!isWakeByRTC&&!isFinishTest){
                isFinishTest = true;
                mStatusText.setText("FAIL");
                Message msg = new Message();
                msg.what = MSG_TEST_OVER;
                msg.arg1 = 1;
                mHandler.sendMessageDelayed(msg,2000);
            }
        }
    }
}
