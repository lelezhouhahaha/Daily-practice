package com.meigsmart.meigrs32.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.meigsmart.meigrs32.R;

public class PressureView extends View{
    Bitmap bmpPresDial = null;
    Bitmap bmpPresPointer = null;
    Bitmap bmpPresScrew = null;
    private int dialWidth;
    private Context mContext = null;
    private Paint mPaint = null;
    private float mPressureNumber = 1010.0F;
    private int pointerHeight;
    private int pointerWidth;
    private float rate;
    private int screwHeight;
    private int screwWidth;
    private int viewCenterX = 0;
    private int viewCenterY = 0;
    private float viewHeight;
    private float viewWidth;

    public PressureView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    private float calcRoateDegree(float paramFloat) {
        float f = paramFloat - 1000.0F;
        if (f > 50.0F)
            f = 51.0F;
        if (f < -50.0F)
            f = -51.0F;
        return (int) (2.9D * f);

    }

    public void destroy() {
        if ((this.bmpPresDial != null) && (!this.bmpPresDial.isRecycled()))
            this.bmpPresDial.recycle();
        if ((this.bmpPresPointer != null)
                && (!this.bmpPresPointer.isRecycled()))
            this.bmpPresPointer.recycle();
        if ((this.bmpPresScrew != null) && (!this.bmpPresScrew.isRecycled()))
            this.bmpPresScrew.recycle();
    }

    protected void onDraw(Canvas paramCanvas) {
        super.onDraw(paramCanvas);
        paramCanvas.drawBitmap(this.bmpPresDial, this.viewCenterX - 0.5F
                        * this.dialWidth, this.viewCenterY - 0.5F * this.dialWidth,
                null);
        paramCanvas.rotate(calcRoateDegree(this.mPressureNumber),
                this.viewCenterX, this.viewCenterY);
        paramCanvas.drawBitmap(this.bmpPresPointer, this.viewCenterX - 0.5F
                        * this.pointerWidth, this.viewCenterY - this.pointerHeight,
                null);
        paramCanvas.drawBitmap(this.bmpPresScrew, this.viewCenterX - 0.5F
                        * this.screwWidth, this.viewCenterY - 0.5F * this.screwHeight,
                null);
    }

    protected void onSizeChanged(int paramInt1, int paramInt2, int paramInt3,
                                 int paramInt4) {
        super.onSizeChanged(paramInt1, paramInt2, paramInt3, paramInt4);
        this.viewWidth = paramInt1;
        this.viewHeight = paramInt2;
        float f1 = paramInt1 / 472.0F;
        float f2 = paramInt2 / 472.0F;

        rate = f1 < f2 ? f1 : f2;

        this.viewCenterX = ((int) (0.5F * this.viewWidth));
        this.viewCenterY = ((int) (0.5F * this.viewHeight));
        this.dialWidth = ((int) (472.0F * this.rate));
        this.screwWidth = ((int) (42.0F * this.rate));
        this.screwHeight = ((int) (41.0F * this.rate));
        this.pointerWidth = ((int) (41.0F * this.rate));
        this.pointerHeight = ((int) (171.0F * this.rate));
        this.bmpPresDial = BitmapFactory.decodeResource(getResources(),
                R.drawable.pressure_dial);
        this.bmpPresDial = Bitmap.createScaledBitmap(this.bmpPresDial,
                this.dialWidth, this.dialWidth, true);
        this.bmpPresScrew = BitmapFactory.decodeResource(getResources(),
                R.drawable.pressure_screw);
        this.bmpPresScrew = Bitmap.createScaledBitmap(this.bmpPresScrew,
                this.screwWidth, this.screwHeight, true);
        this.bmpPresPointer = BitmapFactory.decodeResource(getResources(),
                R.drawable.pressure_pointer);
        this.bmpPresPointer = Bitmap.createScaledBitmap(this.bmpPresPointer,
                this.pointerWidth, this.pointerHeight, true);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);

    }

    public void setData(float paramFloat) {
        this.mPressureNumber = ((int) paramFloat);
        postInvalidate();
    }
}
