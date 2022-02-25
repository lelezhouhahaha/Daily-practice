package com.meigsmart.meigrs32.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class VolumeView extends View {
    private static final float minAngle = (float) (Math.PI / 10);
    private static final float maxAngle = (float) (Math.PI - minAngle);
    private static final float mScale = (maxAngle - minAngle) / Short.MAX_VALUE;
    private static final float PIVOT_RADIUS = 3.5f;
    private static final float PIVOT_Y_OFFSET = 10;
    private static final float SHADOW_OFFSET = 2.0f;

    private Paint mPaint;
    private Paint mShadow;
    private int mCurrentVolume;

    public VolumeView(Context context) {
        super(context);
        init();
    }

    public VolumeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VolumeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);
        mShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShadow.setColor(Color.argb(60, 0, 0, 0));
    }

    public void setVolume(int volume) {
        volume *= 2;
        if (volume < mCurrentVolume) {
            mCurrentVolume = (mCurrentVolume * 3 + volume) / 4;
        } else if (volume > Short.MAX_VALUE) {
            mCurrentVolume = Short.MAX_VALUE;
        } else {
            mCurrentVolume = volume;
        }
        invalidate();
    }

    public void clearVolume(){
        mCurrentVolume = 0;
        invalidate();
    }

    public double getVolume() {
        return mCurrentVolume;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float angle = minAngle + mCurrentVolume * mScale;
        float height = getHeight();
        float rx = getWidth() / 2;
        float ry = height - PIVOT_RADIUS - PIVOT_Y_OFFSET;

        float length = height * 7 / 10;
        float x0 = (float) (rx - length * Math.cos(angle));
        float y0 = (float) (ry - length * Math.sin(angle));

        canvas.drawLine(x0 + SHADOW_OFFSET, y0 + SHADOW_OFFSET, rx + SHADOW_OFFSET, ry + SHADOW_OFFSET, mShadow);
        canvas.drawCircle(rx + SHADOW_OFFSET, ry + SHADOW_OFFSET, PIVOT_RADIUS, mShadow);
        canvas.drawLine(x0, y0, rx, ry, mPaint);
        canvas.drawCircle(rx, ry, PIVOT_RADIUS, mPaint);
    }
}
