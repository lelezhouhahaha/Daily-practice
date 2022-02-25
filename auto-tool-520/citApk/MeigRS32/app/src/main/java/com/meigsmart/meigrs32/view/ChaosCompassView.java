package com.meigsmart.meigrs32.view;

import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.meigsmart.meigrs32.R;

public class ChaosCompassView extends View{
    private Canvas mCanvas;
    private Context mContext;
    private int width;
    private int mCenterX;
    private int mCenterY;
    private int mOutSideRadius;
    private int mCircumRadius;
    private int mTextHeight;
    private Paint mDarkRedPaint;
    private Paint mDeepGrayPaint;
    private Paint mOutSideCircumPaint;
    private Paint mLightGrayPaint;
    private Paint mTextPaint;
    private Paint mCircumPaint;
    private Rect mTextRect;
    private Path mOutsideTriangle;
    private Path mCircumTriangle;

    private Paint mNorthPaint;
    private Paint mOthersPaint;
    private Rect  mPositionRect;
    private Paint mSamllDegreePaint;
    private Rect mSencondRect;
    private Rect mThirdRect;
    private Rect mCenterTextRect;

    private Paint mCenterPaint;

    private Shader mInnerShader;
    private Paint mInnerPaint;

    private ValueAnimator mValueAnimator;
    private float mCameraRotateX;
    private float mCameraRotateY;
    private float mMaxCameraRotate = 10;

    private float mCameraTranslateX;
    private float mCameraTranslateY;
    private float mMaxCameraTranslate;
    private Matrix mCameraMatrix;
    private Camera mCamera;

    private float val=0f;
    private float valCompare;
    private Paint mAnglePaint;

    private String text="N";

    public float getVal() {
        return val;
    }

    public void setVal(float val) {
        this.val = val;
        invalidate();
    }

    public ChaosCompassView(Context context) {
        this(context,null);
    }

    public ChaosCompassView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public void reset(){
        mOutsideTriangle.reset();
        mCircumTriangle.reset();
    }

    public ChaosCompassView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;

        mDarkRedPaint = new Paint();
        mDarkRedPaint.setStyle(Paint.Style.STROKE);
        mDarkRedPaint.setAntiAlias(true);
        mDarkRedPaint.setColor(context.getResources().getColor(R.color.darkRed));


        mDeepGrayPaint = new Paint();
        mDeepGrayPaint.setStyle(Paint.Style.STROKE);
        mDeepGrayPaint.setAntiAlias(true);
        mDeepGrayPaint.setColor(context.getResources().getColor(R.color.deepGray));


        mLightGrayPaint = new Paint();
        mLightGrayPaint.setStyle(Paint.Style.FILL);
        mLightGrayPaint.setAntiAlias(true);
        mLightGrayPaint.setColor(context.getResources().getColor(R.color.lightGray));

        mTextPaint = new Paint();
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(60);
        mTextPaint.setColor(context.getResources().getColor(R.color.black));

        mCircumPaint = new Paint();
        mCircumPaint.setStyle(Paint.Style.FILL);
        mCircumPaint.setAntiAlias(true);
        mCircumPaint.setColor(context.getResources().getColor(R.color.red));

        mOutSideCircumPaint = new Paint();
        mOutSideCircumPaint.setStyle(Paint.Style.FILL);
        mOutSideCircumPaint.setAntiAlias(true);
        mOutSideCircumPaint.setColor(context.getResources().getColor(R.color.lightGray));

        mTextRect = new Rect();
        mOutsideTriangle = new Path();
        mCircumTriangle = new Path();

        mNorthPaint = new Paint();
        mNorthPaint.setStyle(Paint.Style.STROKE);
        mNorthPaint.setAntiAlias(true);
        mNorthPaint.setTextSize(40);
        mNorthPaint.setColor(context.getResources().getColor(R.color.red));

        mOthersPaint = new Paint();
        mOthersPaint.setStyle(Paint.Style.STROKE);
        mOthersPaint.setAntiAlias(true);
        mOthersPaint.setTextSize(40);
        mOthersPaint.setColor(context.getResources().getColor(R.color.white));

        mPositionRect = new Rect();
        mCenterTextRect = new Rect();

        mCenterPaint = new Paint();
        mCenterPaint.setStyle(Paint.Style.STROKE);
        mCenterPaint.setAntiAlias(true);
        mCenterPaint.setTextSize(60);
        mCenterPaint.setColor(context.getResources().getColor(R.color.white));

        mSamllDegreePaint = new Paint();
        mSamllDegreePaint.setStyle(Paint.Style.STROKE);
        mSamllDegreePaint.setAntiAlias(true);
        mSamllDegreePaint.setTextSize(30);
        mSamllDegreePaint.setColor(context.getResources().getColor(R.color.lightGray));

        mSencondRect = new Rect();
        mThirdRect = new Rect();

        mInnerPaint = new Paint();
        mInnerPaint.setStyle(Paint.Style.FILL);
        mInnerPaint.setAntiAlias(true);

        mAnglePaint = new Paint();
        mAnglePaint.setStyle(Paint.Style.STROKE);
        mAnglePaint.setAntiAlias(true);
        mAnglePaint.setColor(context.getResources().getColor(R.color.red));

        mCameraMatrix = new Matrix();
        mCamera = new Camera();

    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mCanvas = canvas;
        set3DMetrix();
        drawText();
        drawCompassOutSide();
        drawCompassCircum();
        drawInnerCricle();
        drawCompassDegreeScale();
        drawCenterText();
    }



    /**
     * 设置camera相关
     */
    private void set3DMetrix() {
        mCameraMatrix.reset();
        mCamera.save();
        mCamera.rotateX(mCameraRotateX);
        mCamera.rotateY(mCameraRotateY);
        mCamera.getMatrix(mCameraMatrix);
        mCamera.restore();
        mCameraMatrix.preTranslate(-getWidth()/2,-getHeight()/2);
        mCameraMatrix.postTranslate(getWidth()/2,getHeight()/2);
        mCanvas.concat(mCameraMatrix);
    }

    private void drawInnerCricle() {

        mInnerShader = new RadialGradient(width/2,mOutSideRadius+mTextHeight,mCircumRadius-40, Color.parseColor("#323232"),
                Color.parseColor("#000000"),Shader.TileMode.CLAMP);
        mInnerPaint.setShader(mInnerShader);
        mCanvas.drawCircle(width/2,mOutSideRadius+mTextHeight,mCircumRadius-40,mInnerPaint);

    }

    private void drawCenterText() {
        String centerText=String.valueOf((int) val+"°");
        mCenterPaint.getTextBounds(centerText,0,centerText.length(),mCenterTextRect);
        int centerTextWidth = mCenterTextRect.width();
        int centerTextHeight = mCenterTextRect.height();
        mCanvas.drawText(centerText,width/2-centerTextWidth/2,mTextHeight+mOutSideRadius+centerTextHeight/5,mCenterPaint);

    }

    private void drawCompassDegreeScale() {
        mCanvas.save();
        mNorthPaint.getTextBounds("N",0,1,mPositionRect);
        int mPositionTextWidth = mPositionRect.width();
        int mPositionTextHeight = mPositionRect.height();
        mNorthPaint.getTextBounds("W",0,1,mPositionRect);
        int mWPositionTextWidth = mPositionRect.width();
        int mWPositionTextHeight = mPositionRect.height();
        mSamllDegreePaint.getTextBounds("30",0,1,mSencondRect);
        int mSencondTextWidth = mSencondRect.width();
        int mSencondTextHeight = mSencondRect.height();
        mSamllDegreePaint.getTextBounds("30",0,1,mThirdRect);
        int mThirdTextWidth = mThirdRect.width();
        int mThirdTextHeight = mThirdRect.height();

        mCanvas.rotate(-val,width/2,mOutSideRadius+mTextHeight);


        for (int i = 0; i < 240; i++) {

            if (i==0||i==60||i==120||i==180){
                mCanvas.drawLine(getWidth() / 2, mTextHeight+mOutSideRadius-mCircumRadius+10,
                        getWidth() / 2,  mTextHeight+mOutSideRadius-mCircumRadius+30, mDeepGrayPaint);
            }else{
                mCanvas.drawLine(getWidth() / 2, mTextHeight+mOutSideRadius-mCircumRadius+10,
                        getWidth() / 2,  mTextHeight+mOutSideRadius-mCircumRadius+30, mLightGrayPaint);
            }
            if (i==0){
                mCanvas.drawText("N", this.width /2-mPositionTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mPositionTextHeight,mNorthPaint);
            }else if (i==60){
                mCanvas.drawText("E", this.width /2-mPositionTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mPositionTextHeight,mOthersPaint);
            }else if (i==120){
                mCanvas.drawText("S", this.width /2-mPositionTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mPositionTextHeight,mOthersPaint);
            }else if (i==180){
                mCanvas.drawText("W", this.width /2-mWPositionTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mWPositionTextHeight,mOthersPaint);
            }else if (i==20){
                mCanvas.drawText("30", this.width /2-mSencondTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mSencondTextHeight,mSamllDegreePaint);
            }else if (i==40){
                mCanvas.drawText("60", this.width /2-mSencondTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mSencondTextHeight,mSamllDegreePaint);
            }else if (i==80){
                mCanvas.drawText("120", this.width /2-mThirdTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mThirdTextHeight,mSamllDegreePaint);
            }else if (i==100){
                mCanvas.drawText("150", this.width /2-mThirdTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mThirdTextHeight,mSamllDegreePaint);
            }else if (i==140){
                mCanvas.drawText("210", this.width /2-mThirdTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mThirdTextHeight,mSamllDegreePaint);
            }else if (i==160){
                mCanvas.drawText("240", this.width /2-mThirdTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mThirdTextHeight,mSamllDegreePaint);
            }else if (i==200){
                mCanvas.drawText("300", this.width /2-mThirdTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mThirdTextHeight,mSamllDegreePaint);
            }else if (i==220){
                mCanvas.drawText("330", this.width /2-mThirdTextWidth/2,mTextHeight+mOutSideRadius-mCircumRadius+40+mThirdTextHeight,mSamllDegreePaint);
            }
            mCanvas.rotate(1.5f, mCenterX, mOutSideRadius+mTextHeight);
        }
        mCanvas.restore();

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void drawCompassCircum() {
        mCanvas.save();
        int mTriangleHeight=(mOutSideRadius-mCircumRadius)/2;

        mCanvas.rotate(-val,width/2,mOutSideRadius+mTextHeight);
        mCircumTriangle.moveTo(width/2,mTriangleHeight+mTextHeight);
        float mTriangleSide = (float) ((mTriangleHeight/(Math.sqrt(3)))*2);
        mCircumTriangle.lineTo(width/2-mTriangleSide/2,mTextHeight+mTriangleHeight*2);
        mCircumTriangle.lineTo(width/2+mTriangleSide/2,mTextHeight+mTriangleHeight*2);
        mCircumTriangle.close();
        mCanvas.drawPath(mCircumTriangle,mCircumPaint);
        mCanvas.drawArc(width/2-mCircumRadius,mTextHeight+mOutSideRadius-mCircumRadius,
                width/2+mCircumRadius,mTextHeight+mOutSideRadius+mCircumRadius,-85,350,false,mDeepGrayPaint);
        mAnglePaint.setStrokeWidth(5f);
        if (val<=180){
            valCompare = val;
            mCanvas.drawArc(width/2-mCircumRadius,mTextHeight+mOutSideRadius-mCircumRadius,
                    width/2+mCircumRadius,mTextHeight+mOutSideRadius+mCircumRadius,-85,valCompare,false,mAnglePaint);
        }else{
            valCompare = 360-val;
            mCanvas.drawArc(width/2-mCircumRadius,mTextHeight+mOutSideRadius-mCircumRadius,
                    width/2+mCircumRadius,mTextHeight+mOutSideRadius+mCircumRadius,-95,-valCompare,false,mAnglePaint);
        }

        mCanvas.restore();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void drawCompassOutSide() {
        mCanvas.save();
        int mTriangleHeight=40;
        mOutsideTriangle.moveTo(width/2,mTextHeight-mTriangleHeight);
        float mTriangleSide = 46.18f;
        mOutsideTriangle.lineTo(width/2-mTriangleSide/2,mTextHeight);
        mOutsideTriangle.lineTo(width/2+mTriangleSide/2,mTextHeight);
        mOutsideTriangle.close();
        mCanvas.drawPath(mOutsideTriangle,mOutSideCircumPaint);

        mDarkRedPaint.setStrokeWidth((float) 5);
        mLightGrayPaint.setStrokeWidth((float)5);
        mDeepGrayPaint.setStrokeWidth((float)3);
        mLightGrayPaint.setStyle(Paint.Style.STROKE);
        mCanvas.drawArc(width/2-mOutSideRadius,mTextHeight,width/2+mOutSideRadius,mTextHeight+mOutSideRadius*2,-80,120,false,mLightGrayPaint);
        mCanvas.drawArc(width/2-mOutSideRadius,mTextHeight,width/2+mOutSideRadius,mTextHeight+mOutSideRadius*2,40,20,false,mDeepGrayPaint);
        mCanvas.drawArc(width/2-mOutSideRadius,mTextHeight,width/2+mOutSideRadius,mTextHeight+mOutSideRadius*2,-100,-20,false,mLightGrayPaint);
        mCanvas.drawArc(width/2-mOutSideRadius,mTextHeight,width/2+mOutSideRadius,mTextHeight+mOutSideRadius*2,-120,-120,false,mDarkRedPaint);
        mCanvas.restore();
    }

    private void drawText() {
        if (val<=15||val>=345){
            text = "North";
        }else if (val>15&&val<=75){
            text= "Northeast";
        }else if (val>75&&val<=105){
            text= "East";
        }else if (val>105&&val<=165){
            text="Southeast";
        }else if (val>165&&val<=195){
            text = "South";
        }else if (val>195&&val<=255){
            text = "Southwest";
        }else if (val>255&&val<=285){
            text = "West";
        }else if (val>285&&val<345){
            text="Northwest";
        }

        mTextPaint.getTextBounds(text,0,text.length(),mTextRect);
        int mTextWidth = mTextRect.width();
        mCanvas.drawText(text,width/2-mTextWidth/2,mTextHeight/8*5,mTextPaint);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        width = Math.min(widthSize, heightSize);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            width = heightSize;
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            width = widthSize;
        }
        mTextHeight = width/3;
        mCenterX = width/2;
        mCenterY = width/2+mTextHeight;
        mOutSideRadius = width*3/8;
        mCircumRadius = mOutSideRadius*4/5;
        mMaxCameraTranslate = 0.02f*mOutSideRadius;
        setMeasuredDimension(width, width+width/3 );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (mValueAnimator!=null&&mValueAnimator.isRunning()){
                    mValueAnimator.cancel();
                }
                getCameraRotate(event);
                getCameraTranslate(event);
                break;
            case MotionEvent.ACTION_MOVE:
                getCameraRotate(event);
                getCameraTranslate(event);
                break;
            case MotionEvent.ACTION_UP:
                startRestore();
                break;
        }
        return true;

    }

    private void startRestore() {
        final String cameraRotateXName = "cameraRotateX";
        final String cameraRotateYName = "cameraRotateY";
        final String canvasTranslateXName = "canvasTranslateX";
        final String canvasTranslateYName = "canvasTranslateY";
        PropertyValuesHolder cameraRotateXHolder =
                PropertyValuesHolder.ofFloat(cameraRotateXName, mCameraRotateX, 0);
        PropertyValuesHolder cameraRotateYHolder =
                PropertyValuesHolder.ofFloat(cameraRotateYName, mCameraRotateY, 0);
        PropertyValuesHolder canvasTranslateXHolder =
                PropertyValuesHolder.ofFloat(canvasTranslateXName, mCameraTranslateX, 0);
        PropertyValuesHolder canvasTranslateYHolder =
                PropertyValuesHolder.ofFloat(canvasTranslateYName, mCameraTranslateY, 0);
        mValueAnimator = ValueAnimator.ofPropertyValuesHolder(cameraRotateXHolder,
                cameraRotateYHolder, canvasTranslateXHolder, canvasTranslateYHolder);
        mValueAnimator.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float f = 0.571429f;
                return (float) (Math.pow(2, -2 * input) * Math.sin((input - f / 4) * (2 * Math.PI) / f) + 1);
            }
        });
        mValueAnimator.setDuration(1000);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCameraRotateX = (float) animation.getAnimatedValue(cameraRotateXName);
                mCameraRotateY = (float) animation.getAnimatedValue(cameraRotateYName);
                mCameraTranslateX = (float) animation.getAnimatedValue(canvasTranslateXName);
                mCameraTranslateX = (float) animation.getAnimatedValue(canvasTranslateYName);
            }
        });
        mValueAnimator.start();
    }

    private void getCameraTranslate(MotionEvent event) {
        float translateX = (event.getX() - getWidth() / 2);
        float translateY = (event.getY() - getHeight()/2);
        float[] percentArr = getPercent(translateX, translateY);
        mCameraTranslateX = percentArr[0] * mMaxCameraTranslate;
        mCameraTranslateY = percentArr[1] * mMaxCameraTranslate;
    }

    private void getCameraRotate(MotionEvent event) {
        float mRotateX = -(event.getY()-(getHeight())/2);
        float mRotateY = (event.getX()-getWidth()/2);
        float[] percentArr = getPercent(mRotateX,mRotateY);
        mCameraRotateX = percentArr[0]*mMaxCameraRotate;
        mCameraRotateY = percentArr[1]*mMaxCameraRotate;
    }

    private float[] getPercent(float mCameraRotateX, float mCameraRotateY) {
        float[] percentArr = new float[2];
        float percentX = mCameraRotateX/width;
        float percentY = mCameraRotateY/width;
        if (percentX > 1) {
            percentX = 1;
        } else if (percentX < -1) {
            percentX = -1;
        }
        if (percentY > 1) {
            percentY = 1;
        } else if (percentY < -1) {
            percentY = -1;
        }
        percentArr[0] = percentX;
        percentArr[1] = percentY;
        return percentArr;
    }



}
