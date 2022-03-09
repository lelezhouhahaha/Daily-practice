package com.meigsmart.meigrs32.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.client.MyDiagAutoTestClient;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.DiagCommand;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class PCBAAutoTouchTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack,OnTouchListener {

    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private PCBAAutoTouchTestActivity mContext;
    private Runnable mRun;
    private MyHandler mHandler;
    private CustomerView mCustomerView;

    private int mConfigTime = 0;

    private String mFatherName = "";
    private final String TAG = PCBAAutoTouchTestActivity.class.getSimpleName();
    public static final int HANDLER_TEST_REAULT = 1100;
    public static  int HANDLER_TEST_REAULT_SUCCESS = 1101;
    public static  int HANDLER_TEST_REAULT_FAIL = 1102;

    private Boolean mTouchTestResult = false;
    private boolean mTouchDownTestResult = false;
    private boolean mTouchUpTestResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mFail.setVisibility(View.VISIBLE);

        setContentView(mCustomerView = new CustomerView(this));
        mCustomerView.setOnTouchListener(this);

        mHandler = new MyHandler(mContext);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);

                if(( mConfigTime == 0 ) && ( mFatherName.equals(MyApplication.PCBAAutoTestNAME) )){
                    mHandler.sendEmptyMessage(HANDLER_TEST_REAULT);
                    return;
                }

                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(HANDLER_TEST_REAULT);
                    return;
                }
               mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        showMutiTouchTitle();  

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_touch_test;
    }

    @Override
    protected void initData() {
        /*mContext = this;
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = getResources().getInteger(R.integer.pcba_auto_test_default_time);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mHandler = new MyHandler(mContext);*/
    }

    @Override
    public boolean onTouch(View view, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mFail.setVisibility(View.GONE);
                LogUtil.d(TAG, "MotionEvent.ACTION_DOWN e.getX():" + e.getX()  +  " e.getY():"+ e.getY());
                if( (e.getX() != 0) && (e.getY() != 0)){
                    mTouchDownTestResult = true;

                }
                if(mTouchDownTestResult && mTouchUpTestResult){
                    mTouchTestResult = true;
                    mHandler.sendEmptyMessage(HANDLER_TEST_REAULT);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                LogUtil.d(TAG, "MotionEvent.ACTION_MOVE ");
                break;
            case MotionEvent.ACTION_UP:
                LogUtil.d(TAG, "MotionEvent.ACTION_UP e.getX():" + e.getX()  +  " e.getY():"+ e.getY());
                if( (e.getX() != 0) && (e.getY() != 0)){
                    mTouchUpTestResult = true;
                }
                if(mTouchDownTestResult && mTouchUpTestResult){
                    mTouchTestResult = true;
                    mHandler.sendEmptyMessage(HANDLER_TEST_REAULT);
                }
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }

    private void showMutiTouchTitle(){
        Toast mToast = Toast.makeText(mContext, getResources().getString(R.string.PCBAAutoTouchTestActivity), Toast.LENGTH_LONG);
        mToast.setGravity(Gravity.CENTER_VERTICAL|Gravity.TOP, 0, 0);
        mToast.show();
    }

    private static class MyHandler extends Handler {
        WeakReference<Activity> reference;
        public MyHandler(Activity activity) {
            reference = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PCBAAutoTouchTestActivity activity = (PCBAAutoTouchTestActivity) reference.get();
            switch (msg.what) {
                case HANDLER_TEST_REAULT:
                    if(activity.mTouchTestResult){
                        Toast.makeText(activity.mContext, "success", Toast.LENGTH_SHORT).show();
                        activity.deInit(activity.mFatherName, activity.SUCCESS);
                    }else activity.deInit(activity.mFatherName, activity.FAILURE, "Touch not receive event.");
                    break;
                default:
                    LogUtil.d(activity.TAG, "operation abnormal!");
                    break;
            }
        }
    }

    public class CustomerView extends View {

        public CustomerView(Context context) {
            super(context);
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xFF0000);
        }
    }
}
