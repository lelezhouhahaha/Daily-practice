package com.meigsmart.meigrs32.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.util.AttributeSet;
import android.view.View;
import com.meigsmart.meigrs32.R;

public class HardwareInfoView extends View{
    private Bitmap bgBmp = null;
    private Context mContext;
    private Paint mPaintBg = null;
    private Paint mPaintTextLeft = null;
    private Paint mPaintTextRight = null;
    private Sensor mSensor;
    public String maxi_c = "";
    public float maximumrange;
    public String name = "";
    public String name_c = "";
    public float power;
    public String power_c = "";
    public String resolu_c = "";
    public float resolution;
    private int textsize = 0;
    public String vendor = "";
    public String vendor_c = "";
    public int version;
    public String version_c = "";
    private float viewHeight;
    private float viewWidth;

    public HardwareInfoView(Context paramContext, AttributeSet paramAttributeSet)
    {
        super(paramContext, paramAttributeSet);
        this.mContext = paramContext;
        this.name_c = this.mContext.getResources().getString(R.string.sensor_attr_name);
        this.vendor_c = this.mContext.getResources().getString(R.string.sensor_attr_vendor);
        this.version_c = this.mContext.getResources().getString(R.string.sensor_attr_version);
        this.power_c = this.mContext.getResources().getString(R.string.sensor_attr_power);
        this.maxi_c = this.mContext.getResources().getString(R.string.sensor_attr_maximumrange);
        this.resolu_c = this.mContext.getResources().getString(R.string.sensor_attr_resolution);
        this.mPaintTextLeft = new Paint();
        this.mPaintTextLeft.setStrokeWidth(2.0F);
        this.mPaintTextLeft.setAntiAlias(true);
        this.mPaintTextLeft.setColor(getResources().getColor(R.color.hardinfo_text_left));
        this.mPaintTextLeft.setTextAlign(Paint.Align.LEFT);
        this.mPaintTextRight = new Paint();
        this.mPaintTextRight.setStrokeWidth(2.0F);
        this.mPaintTextRight.setAntiAlias(true);
        this.mPaintTextRight.setColor(getResources().getColor(R.color.hardinfo_text_right));
        this.mPaintTextRight.setTextAlign(Paint.Align.RIGHT);
        this.bgBmp = BitmapFactory.decodeResource(this.mContext.getResources(), R.drawable.background_hardinfoware);
        this.mPaintBg = new Paint();
        this.mPaintBg.setAntiAlias(true);
    }

    public void destroy()
    {
        if ((this.bgBmp != null) && (!this.bgBmp.isRecycled()))
            this.bgBmp.recycle();
    }

    protected void onDraw(Canvas paramCanvas)
    {
        super.onDraw(paramCanvas);
        paramCanvas.drawBitmap(this.bgBmp, 0.0F, 0.0F, this.mPaintBg);
        if(this.name_c != "")
            paramCanvas.drawText(this.name_c, 20.0F, 2 * this.textsize, this.mPaintTextLeft);
        if(this.name != "")
            paramCanvas.drawText(this.name, this.viewWidth - 20.0F, 2 * this.textsize, this.mPaintTextRight);
        if(this.vendor_c != "")
            paramCanvas.drawText(this.vendor_c, 20.0F, 4 * this.textsize, this.mPaintTextLeft);
        if(this.vendor != "")
            paramCanvas.drawText(this.vendor, this.viewWidth - 20.0F, 4 * this.textsize, this.mPaintTextRight);
        if(this.version_c != "")
            paramCanvas.drawText(this.version_c, 20.0F, 6 * this.textsize, this.mPaintTextLeft);
        paramCanvas.drawText(""+this.version, this.viewWidth - 20.0F, 6 * this.textsize, this.mPaintTextRight);
        if(this.power_c != "")
            paramCanvas.drawText(this.power_c, 20.0F, 8 * this.textsize, this.mPaintTextLeft);
        paramCanvas.drawText(""+this.power, this.viewWidth - 20.0F, 8 * this.textsize, this.mPaintTextRight);
        if(this.maxi_c != "")
            paramCanvas.drawText(this.maxi_c, 20.0F, 10 * this.textsize, this.mPaintTextLeft);
        paramCanvas.drawText(""+this.maximumrange, this.viewWidth - 20.0F, 10 * this.textsize, this.mPaintTextRight);
        if(this.resolu_c != "")
            paramCanvas.drawText(this.resolu_c, 20.0F, 12 * this.textsize, this.mPaintTextLeft);
        paramCanvas.drawText(""+this.resolution, this.viewWidth - 20.0F, 12 * this.textsize, this.mPaintTextRight);
    }

    protected void onSizeChanged(int paramInt1, int paramInt2, int paramInt3, int paramInt4)
    {
        super.onSizeChanged(paramInt1, paramInt2, paramInt3, paramInt4);
        this.viewHeight = paramInt2;
        this.viewWidth = paramInt1;
        if (this.viewHeight > 0 && this.viewWidth > 0) {
            this.bgBmp = Bitmap.createScaledBitmap(this.bgBmp, paramInt1, paramInt2, true);
            this.textsize = ((int)(this.viewHeight / 13.0F));
            this.mPaintTextLeft.setTextSize(this.textsize);
            this.mPaintTextRight.setTextSize(this.textsize);
        }
    }

    public void setSensorData(Sensor paramSensor)
    {
        this.mSensor = paramSensor;
        if (this.mSensor != null)
        {
            this.name = this.mSensor.getName();
            this.vendor = this.mSensor.getVendor();
            this.version = this.mSensor.getVersion();
            this.power = this.mSensor.getPower();
            this.maximumrange = this.mSensor.getMaximumRange();
        }
	    /*for (this.resolution = this.mSensor.getResolution(); ; this.resolution = 0.0F)
	    {
	      invalidate();
	      return;
	      this.name = "...";
	      this.vendor = "...";
	      this.version = 0;
	      this.power = 0.0F;
	      this.maximumrange = 0.0F;
	    }*/
    }

    public String toStr()
    {
        return new StringBuilder(String.valueOf(new StringBuilder(String.valueOf(new StringBuilder(String.valueOf(new StringBuilder(String.valueOf(new StringBuilder(String.valueOf("")).append(this.name_c).append(":\t").append(this.name).append("\n").toString())).append(this.vendor_c).append(":\t").append(this.vendor).append("\n").toString())).append(this.version_c).append(":\t").append(this.version).append("\n").toString())).append(this.power_c).append(":\t").append(this.power).append("\n").toString())).append(this.maxi_c).append(":\t").append(this.maximumrange).append("\n").toString() + this.resolu_c + ":\t" + this.resolution;
    }
}
