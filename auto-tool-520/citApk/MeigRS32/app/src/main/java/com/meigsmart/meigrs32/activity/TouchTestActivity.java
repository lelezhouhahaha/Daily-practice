package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View.OnTouchListener;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class TouchTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack,OnTouchListener {

    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private TouchTestActivity mContext;
    private String mFatherName = "";

    private int OFFSET = 0;
    private int mRectWidth;
    private int mRectHeight;
    private int mCount = 1;
    private boolean mIsHorizontal = false;
    private boolean mIsDrawrectEnd = false;
    private boolean mIsDrawSlashInit = false;
    private boolean mIsSlash_1 = false;
    private boolean mIsSlash_2 = false;
    private List<PointEntity> mDrawPointList = new ArrayList<PointEntity>();
    private List<RectEntity> mDrawRectList = new ArrayList<RectEntity>();
    private Paint mPaint = null;
    private CustomerView mCustomerView;
    private Path path_1;
    private Path path_2;
    private int height = 0;
    private int mConfigTime = 0;
    //modify by gongming for BUG 14668;
    private int distance_Middle = 54;
    private double Precision_ishorizontal;
    private double Precision_isslash;
    private final String TAG = TouchTestActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        DisplayMetrics dm;
        if(isMT535_version){
            dm = new DisplayMetrics();
            mContext.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        }else{
            dm = getApplicationContext().getResources().getDisplayMetrics();
        }

        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height = mContext.getResources().getDimensionPixelSize(resourceId);
        }

        mRectWidth = dm.widthPixels;
        mRectHeight = dm.heightPixels;//+ height
        String offsetvalue = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, "common_touch_screen_draw_off_set_value_int");
        LogUtil.d("citapk onCreate offsetvalue:" +offsetvalue);
        if(!offsetvalue.isEmpty())
            OFFSET = Integer.parseInt(offsetvalue);
        else OFFSET = 150;

        LogUtil.d("citapk onCreate OFFSET:" +OFFSET);
        setContentView(mCustomerView = new CustomerView(this));
        mCustomerView.setOnTouchListener(this);
        mFail.setVisibility(View.VISIBLE);
		 //modify by gongming for BUG 14595 and 14596;
        RectEntity rect = new RectEntity(0, 0, OFFSET, mRectHeight);
        mDrawRectList.add(rect);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setARGB(255, 255, 0, 0);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(4);
        String ishorizontal_value = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, "common_touch_screen_draw_off_set_distance_double_IsHorizontal");
        String isslash_value = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, "common_touch_screen_draw_off_set_distance_double_IsSlash");
        Log.d("GM","citapk onCreate distance_value:" +ishorizontal_value);
        if(!ishorizontal_value.isEmpty())
            Precision_ishorizontal = Double.parseDouble(ishorizontal_value);
        else Precision_ishorizontal = 0.95;
        if(!isslash_value.isEmpty())
            Precision_isslash = Double.parseDouble(isslash_value);
        else Precision_isslash = 0.93;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_touch_test;
    }

    @Override
    protected void initData() {
        //
    }

    @Override
    public boolean onTouch(View view, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mFail.setVisibility(View.GONE);
                initPoint((int) e.getX(), (int) e.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                initPoint((int) e.getX(), (int) e.getY());
                for (int j = 0; j < mDrawPointList.size(); j++) {
                    PointEntity pe = mDrawPointList.get(j);
                    if (!mIsSlash_1 && !mIsSlash_2) {
                        if (IsCollision(pe.px, pe.py)) {
                            mDrawPointList.clear();
                        }
                    }
                }
                isSuccess(mCount - 1);
                if (mIsSlash_1 || mIsSlash_2) {
                    isSlashSuccess();
                }
                break;
            case MotionEvent.ACTION_UP:
                if ((mCount > 4) && !mIsDrawSlashInit) {
                    mDrawRectList.clear();
                    initSlash();
                    mIsDrawSlashInit = true;
                    mIsSlash_1 = true;
                    Log.d(TAG, "MotionEvent.ACTION_UP initSlash");
                }
                mDrawPointList.clear();
                break;
        }
        mCustomerView.invalidate();
        return true;
    }

    public void initPoint(int px, int py) {
        PointEntity pe = new PointEntity(px, py);
        mDrawPointList.add(pe);
    }

    public void initSlash() {
        path_1 = new Path();
		 //modify by gongming for BUG 14595 and 14596;
        OFFSET = OFFSET;
        path_1.moveTo(OFFSET, 0);
        path_1.lineTo(mRectWidth, mRectHeight - OFFSET);
        path_1.moveTo(mRectWidth - OFFSET, mRectHeight);
        path_1.lineTo(0, OFFSET);

        path_2 = new Path();
        path_2.moveTo((mRectWidth - OFFSET), 0);
        path_2.lineTo(0, mRectHeight - OFFSET);
        path_2.moveTo(OFFSET, mRectHeight);
        path_2.lineTo(mRectWidth, OFFSET);
    }

    private boolean IsCollision(int dx, int dy) {
        try {
            if (mCount > 4) {
                return true;
            }
            RectEntity rect = mDrawRectList.get(mCount - 1);
            if (rect.rx < dx && rect.rWidth > dx && rect.ry < dy
                    && rect.rHeight > dy) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return true;
    }
    //modify by gongming for BUG 14668;
    private boolean mIsSlashCollision(int rx,int ry){
        try {
            if (mCount < 4) {
                return true;
            }
            if (mIsSlash_1) {
                if(Math.abs((-mRectHeight*rx+mRectWidth*ry)/Math.sqrt(mRectHeight*mRectHeight+mRectWidth*mRectWidth))>distance_Middle){
                    return true;
                }
                else {
                    return false;
                }
            }
            else if (mIsSlash_2) {

                if (Math.abs((-mRectHeight * rx - mRectWidth * ry + mRectWidth * mRectHeight) / Math.sqrt(mRectHeight * mRectHeight + mRectWidth * mRectWidth)) > distance_Middle) {
                    return true;
                } else {
                    return false;
                }
        }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return true;
    }

    private boolean isSlashSuccess() {
        PointEntity p0 = mDrawPointList.get(0);
        PointEntity pN = mDrawPointList.get(mDrawPointList.size() - 1);
        //modify by gongming for BUG 14668;
        if(mIsSlash_1 || mIsSlash_2 ){
            if(mIsSlashCollision(pN.px, pN.py)){
            
                mDrawPointList.clear();
            }}

        if (mIsSlash_1 && (Math.abs(pN.px - p0.px) >= mRectWidth * Precision_isslash) && (Math.abs(pN.py - p0.py) >= mRectHeight * Precision_isslash)) {
            mIsSlash_1 = false;
            mIsSlash_2 = true;
            mDrawPointList.clear();
        } else if (mIsSlash_2 && (Math.abs(pN.px - p0.px) >= mRectWidth * Precision_isslash) && (Math.abs(pN.py - p0.py) >= mRectHeight * Precision_isslash)) {
            mIsSlash_2 = false;
            Toast.makeText(mContext, getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
            mSuccess.setVisibility(View.VISIBLE);
            mFail.setVisibility(View.VISIBLE);
            deInit(mFatherName, SUCCESS);
            mDialog.setSuccess();
            return true;
        }
        return false;
    }

    private boolean isSuccess(int count) {
        if (mCount == 1 || mCount == 3) {
            mIsHorizontal = false;
        } else if (mCount == 2 || mCount == 4) {
            mIsHorizontal = true;
        } else if (mCount > 4) {
            return true;
        }
        if (mDrawPointList.size() > 0) {
            PointEntity p0 = mDrawPointList.get(0);
            PointEntity pN = mDrawPointList.get(mDrawPointList.size() - 1);
            RectEntity re = mDrawRectList.get(count);
            if (mIsHorizontal) {
                if (Math.abs(pN.px - p0.px) >= mRectWidth * Precision_ishorizontal) {
                    re.rPaint.setARGB(255, 0, 255, 0);
                    mCount++;
                    if (mCount == 3) {
                        mDrawRectList.add(new RectEntity((mRectWidth - OFFSET),
                                0, mRectWidth, mRectHeight));
                        mDrawPointList.clear();
                    }
                } else {
                    re.rPaint.setARGB(255, 255, 0, 0);
                }
            } else {
                if (Math.abs(pN.py - p0.py) >= mRectHeight * Precision_ishorizontal) {
                    re.rPaint.setARGB(255, 0, 255, 0);
                    mCount++;
                    if (mCount == 2) {
                        mDrawRectList
                                .add(new RectEntity(0, (mRectHeight - OFFSET),
                                        mRectWidth, mRectHeight));
                        mDrawPointList.clear();
                    } else if (mCount == 4) {
                        mDrawRectList.add(new RectEntity(0, 0, mRectWidth,
                                OFFSET));
                        mDrawPointList.clear();
                    }
                } else {
                    re.rPaint.setARGB(255, 255, 0, 0);
                }
            }
        }
        return false;
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
            for (int i = 0; i < mDrawRectList.size(); i++) {
                RectEntity re = mDrawRectList.get(i);
                canvas.drawRect(re.rx, re.ry, re.rWidth, re.rHeight, re.rPaint);
            }

            if (mIsSlash_1) {
                canvas.drawPath(path_1, mPaint);
            }
            if (mIsSlash_2) {
                canvas.drawPath(path_2, mPaint);
            }

            Point lastPoint = new Point(0, 0);
            for (int j = 0; j < mDrawPointList.size(); j++) {
                PointEntity pe = mDrawPointList.get(j);
                if (j == 0) {
                    canvas.drawPoint((int) pe.px, (int) pe.py, pe.pPaint);
                } else {
                    canvas.drawLine(lastPoint.x, lastPoint.y, pe.px, pe.py,
                            pe.pPaint/* mPaint */);
                }
                lastPoint.x = pe.px;
                lastPoint.y = pe.py;
            }
        }
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

    public class PointEntity {
        private Paint pPaint = null;
        int px, py = 0;

        PointEntity(int x, int y) {
            px = x;
            py = y;
            pPaint = new Paint();
            pPaint.setAntiAlias(true);
            pPaint.setARGB(255, 255, 0, 0);
            pPaint.setStyle(Paint.Style.STROKE);
            pPaint.setStrokeWidth(4);
        }
    }

    public class RectEntity {
        private Paint rPaint = null;
        int rx, ry, rWidth, rHeight = 0;

        RectEntity(int _rx, int _ry, int _rWidth, int _rHeight) {
            rx = _rx;
            ry = _ry;
            rWidth = _rWidth;
            rHeight = _rHeight;
            rPaint = new Paint();
            rPaint.setAntiAlias(true);
            rPaint.setARGB(255, 255, 0, 0);
            rPaint.setStyle(Paint.Style.STROKE);
            rPaint.setStrokeWidth(3);
        }
    }
}
