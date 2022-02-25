package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class MutiTouchTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {

    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.multi_touch_info)
    public TextView mMultiTouchInfo;
    @BindView(R.id.multi_touch_button)
    public Button mMultiTouchBtn;

    private String mFatherName = "";
    private MutiTouchTestActivity mContext;

    private MuiltImageView imgView;
    private DisplayMetrics mDisplayMetrics;
    private ArrayList<PointF> mPointFList = new ArrayList<PointF>();
    private boolean touchNumBool;
    private int height = 0;
    private int mConfigTime = 0;
    /*jicong.wang add for bug P_RK95LTE_E-43 start {@*/
    private static final int DEFAULT_POINT_COUNTS = 10;
    private int mPointCounts;
    private static final String COMMON_MUTITOUCH_POINT_COUNTS = "common_mutitouch_point_counts_config";
    /*jicong.wang add for bug P_RK95LTE_E-43 end @}*/
    private String projectName = "";
    private boolean isMT537_version =false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        touchNumBool = false;

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        isMT537_version = "MT537".equals(projectName);
        mTitle.setText(getResources().getString(R.string.MutiTouchTestActivity));
        mMultiTouchBtn.setOnClickListener(this);
        mFail.setOnClickListener(this);

        mDisplayMetrics = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(mDisplayMetrics);
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        if(isMT537_version) {
            mMultiTouchBtn.setVisibility(View.GONE);
            mMultiTouchInfo.setVisibility(View.GONE);
        imgView = new MuiltImageView(this, mDisplayMetrics.widthPixels,
                mDisplayMetrics.heightPixels);
        setContentView(imgView);
		showMutiTouchTitle();
        }else {
            mMultiTouchInfo.setText(String.format(getResources().getString(R.string.multiTouchInfo), getResources().getString(R.string.next), mPointCounts));
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_muti_touch_test;
    }

    private void showMutiTouchTitle(){
        Toast mToast = Toast.makeText(mContext, getResources().getString(R.string.MutiTouchTestActivity), Toast.LENGTH_LONG);
        mToast.setGravity(Gravity.CENTER_VERTICAL|Gravity.TOP, 0, 0);
        mToast.show();
    }

    @Override
    protected void initData() {
        //
        /*jicong.wang add for bug P_RK95LTE_E-43 start {@*/
        String point_counts = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, COMMON_MUTITOUCH_POINT_COUNTS);
        if(TextUtils.isEmpty(point_counts)){
            mPointCounts = DEFAULT_POINT_COUNTS;
        } else {
            mPointCounts = Integer.valueOf(point_counts);
        }
        /*jicong.wang add for bug P_RK95LTE_E-43 end @}*/
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
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
        if(v == mMultiTouchBtn){
            mMultiTouchBtn.setVisibility(View.GONE);
            mMultiTouchInfo.setVisibility(View.GONE);
            imgView = new MuiltImageView(this, mDisplayMetrics.widthPixels,
                    mDisplayMetrics.heightPixels);
            setContentView(imgView);
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

    public void isSuccess() {
        Toast.makeText(mContext, getResources().getString(R.string.success), Toast.LENGTH_SHORT)
                .show();
    }

    private class MuiltImageView extends View {
        private static final float RADIUS = 30f;
        private boolean onTouch = false;
        private int mWidth, mHeight;
        private String touchPoint;
        public MuiltImageView(Context context, int width, int height) {
            super(context);
            mWidth = width;
            mHeight = height;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (mPointFList != null) {
                if (onTouch) {
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setTextSize(30);
                    paint.setColor(Color.RED);
                    paint.setStrokeWidth(4);

                    Paint linerLaint = new Paint();
                    linerLaint.setAntiAlias(true);
                    linerLaint.setColor(Color.GREEN);

                    Paint textPaint = new Paint();
                    textPaint.setAntiAlias(true);
                    textPaint.setTextSize(30);

                    for (int j = 0; j < mPointFList.size(); j++) {
                        if (mPointFList.get(j) != null) {
                            canvas.drawPoint(mPointFList.get(j).x,
                                    mPointFList.get(j).y, paint);
                            canvas.drawCircle(mPointFList.get(j).x,
                                    mPointFList.get(j).y, RADIUS, paint);
                            canvas.drawLine(0, mPointFList.get(j).y,
                                    mDisplayMetrics.widthPixels,
                                    mPointFList.get(j).y, linerLaint);
                            canvas.drawLine(mPointFList.get(j).x, 0,
                                    mPointFList.get(j).x,
                                    mDisplayMetrics.heightPixels, linerLaint);
                            touchPoint ="("
                                    + String.valueOf((int)mPointFList.get(j).x)
                                    + ","
                                    + String.valueOf((int)mPointFList.get(j).y)
                                    + ")";
                            canvas.drawText(touchPoint, mPointFList.get(j).x,
                                    mPointFList.get(j).y, textPaint);
                        }
                    }
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mPointFList.clear();
            }

            if (event.getPointerCount() > 0) {
                /*jicong.wang modify for bug P_RK95LTE_E-43 start {@*/
                if (event.getPointerCount() >= mPointCounts) {
                    touchNumBool = true;
                }
                /*jicong.wang modify for bug P_RK95LTE_E-43 end @}*/

                for (int j = 0; j < event.getPointerCount(); j++) {
                    PointF piont = new PointF();
                    piont.set(event.getX(j), event.getY(j));
                    mPointFList.add(piont);
                }
                onTouch = true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP/* && mPass */) {
                if (touchNumBool) {
                    isSuccess();
                    deInit(mFatherName, SUCCESS);
                }
                mPointFList.clear();
            }
            invalidate();
            return true;
        }
    }
}
