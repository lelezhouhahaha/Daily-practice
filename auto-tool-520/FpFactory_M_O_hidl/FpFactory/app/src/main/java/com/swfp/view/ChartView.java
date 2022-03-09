package com.swfp.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.swfp.factory.R;

/**
 * Created by yxf on 2017/12/15 0015.
 */

public class ChartView extends View {
    private int mWidth;
    private int mHeight;
    private Paint mRowLinePaint; //x轴画笔
    private Paint mColLinePaint;  //y轴画笔
    private Paint mArrowPaint; //箭头画笔
    private Paint mTextPaint; //文本画笔
    private Paint mVerticalTextPaint; //转折点文本画笔
    private Paint mFoldLinePaint; //折线画笔
    private Paint mVerticalLinePaint; //折线上每个点到x轴之间的线的画笔

    private int mPaddingLeft = 100; //距左侧边距
    private int mPaddingTop = 100; //距上侧边距
    private int mPaddingRight = 30; //距右侧边距
    private int mPaddingBottom = 100; //距底侧边距
    private int mXpointOffset = 10; //x轴每个坐标点竖线长度
    private int mYpointOffset = 10; //y轴每个坐标点横线长度
    private int mxTextOffset = 20; //x轴每个竖线长度
    private int myTextOffset = 20; //y轴每个横线长度
    private int mDrawXTextOffset = 5; //画文本x轴偏移量
    private int mDrawYTextOffset = 7; //画文本y轴偏移量
    private int mArrowOffset = 30; //箭头偏移量

    private static final float DEFAULT_ROW_AND_COL_WIDTH = 2.0f; //默认横轴和数轴线宽
    private static final float DEFAULT_ARROW_WIDTH = 2.0f; //默认箭头线宽
    private static final float DEFAULT_TEXT_WIDTH = 0.7f; //默认文本画笔线宽
    private float textSize = 20.0f; //文本字体大小
    private float verticaltTextSize = 30.0f; //转折点文本字体大小
    private int mXoffset; //x轴每个坐标点之间的偏移量
    private int mYoffset; //Y轴每个坐标点之间的偏移量

    private int mRowColor; //x轴颜色
    private int mColColor; //y轴颜色
    private int mArrowColor; //箭头颜色
    private int mTextColor; //文本颜色
    private int mVerticalTextColor; //转折点文本颜色
    private int mFoldLineColor; //折线颜色
    private int mVerticalLineColor; //折线上每个点到x轴之间的线的颜色
    private float mTextSize; //文本字体大小
    private float mVerticalTextSize; //转折点文本字体大小

    private byte[] datas;
    private int datasLen = 16;
    private int mBreakPoint = 0;

    public ChartView(Context context) {
        this(context,null);
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs);
        initPaint();
    }

    /**
     * 从xml文件中获取属性值
     * @param context
     * @param attrs
     */
    private void initFromAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChartView);

        mRowColor = a.getColor(R.styleable.ChartView_row_color, Color.CYAN);
        mColColor = a.getColor(R.styleable.ChartView_col_color, Color.CYAN);
        mArrowColor = a.getColor(R.styleable.ChartView_arrow_color, Color.CYAN);
        mTextColor = a.getColor(R.styleable.ChartView_text_color, Color.BLUE);
        mVerticalTextColor = a.getColor(R.styleable.ChartView_vertical_text_color, Color.MAGENTA);
        mFoldLineColor = a.getColor(R.styleable.ChartView_fold_line_color, Color.RED);
        mVerticalLineColor = a.getColor(R.styleable.ChartView_vertical_line_color, Color.GREEN);

        mTextSize = a.getFloat(R.styleable.ChartView_text_size, textSize);
        mVerticalTextSize = a.getFloat(R.styleable.ChartView_vertical_text_size, verticaltTextSize);

        a.recycle();
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        mRowLinePaint = new Paint();
        mRowLinePaint.setAntiAlias(true);
        mRowLinePaint.setColor(mRowColor);
        mRowLinePaint.setStrokeWidth(DEFAULT_ROW_AND_COL_WIDTH);

        mColLinePaint = new Paint();
        mColLinePaint.setAntiAlias(true);
        mColLinePaint.setColor(mColColor);
        mColLinePaint.setStrokeWidth(DEFAULT_ROW_AND_COL_WIDTH);

        mArrowPaint = new Paint();
        mArrowPaint.setAntiAlias(true);
        mArrowPaint.setStyle(Paint.Style.STROKE);
        mArrowPaint.setColor(mArrowColor);
        mArrowPaint.setStrokeWidth(DEFAULT_ROW_AND_COL_WIDTH);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setStrokeWidth(DEFAULT_TEXT_WIDTH);
        mTextPaint.setTextSize(mTextSize);

        mVerticalTextPaint = new Paint();
        mVerticalTextPaint.setAntiAlias(true);
        mVerticalTextPaint.setStyle(Paint.Style.STROKE);
        mVerticalTextPaint.setColor(mVerticalTextColor);
        mVerticalTextPaint.setStrokeWidth(DEFAULT_TEXT_WIDTH);
        mVerticalTextPaint.setTextSize(mVerticalTextSize);

        mFoldLinePaint = new Paint();
        mFoldLinePaint.setAntiAlias(true);
        mFoldLinePaint.setStyle(Paint.Style.STROKE);
        mFoldLinePaint.setColor(mFoldLineColor);
        mFoldLinePaint.setStrokeWidth(DEFAULT_ROW_AND_COL_WIDTH);

        mVerticalLinePaint = new Paint();
        mVerticalLinePaint.setAntiAlias(true);
        mVerticalLinePaint.setStyle(Paint.Style.STROKE);
        mVerticalLinePaint.setColor(mVerticalLineColor);
        mVerticalLinePaint.setStrokeWidth(DEFAULT_ROW_AND_COL_WIDTH);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawRowLine(canvas);
        drawColLine(canvas);
        if(datas!=null&&datas.length>0){
            drawFoldLine(canvas);
        }
    }

    /**
     * 画X轴坐标
     * @param canvas
     */
    private void drawRowLine(Canvas canvas) {
        canvas.drawLine(mPaddingLeft,mHeight-mPaddingBottom,mWidth-mPaddingRight,mHeight-mPaddingBottom,mRowLinePaint);
        canvas.drawLine(mWidth-mPaddingRight,mHeight-mPaddingBottom,mWidth-mPaddingRight-mArrowOffset,mHeight-mPaddingBottom-mArrowOffset,mArrowPaint);
        canvas.drawLine(mWidth-mPaddingRight,mHeight-mPaddingBottom,mWidth-mPaddingRight-mArrowOffset,mHeight-(mPaddingBottom-mArrowOffset),mArrowPaint);

        for (int i = 0; i < datasLen; i++) {
            canvas.drawLine(mPaddingLeft+(mXoffset*i),mHeight-mPaddingBottom,mPaddingLeft+(mXoffset*i),mHeight-mPaddingBottom+mXpointOffset,mRowLinePaint);
            String hexString = Integer.toHexString(i);
            StringBuilder sb = new StringBuilder();
            if(hexString.length() == 1){
                sb.append("0").append(hexString);
            }else{
                sb.append(hexString);
            }
            String s = sb.toString();
            canvas.drawText(String.valueOf(s.charAt(0)),mPaddingLeft+(mXoffset*i)- mDrawXTextOffset,mHeight-mPaddingBottom+mXpointOffset+ mxTextOffset, mTextPaint);
            canvas.drawText(String.valueOf(s.charAt(1)),mPaddingLeft+(mXoffset*i)- mDrawXTextOffset,mHeight-mPaddingBottom+mXpointOffset+2* mxTextOffset, mTextPaint);
        }
    }

    /**
     * 画Y轴坐标
     * @param canvas
     */
    private void drawColLine(Canvas canvas) {
        canvas.drawLine(mPaddingLeft,mPaddingTop,mPaddingLeft,mHeight-mPaddingBottom,mColLinePaint);
        canvas.drawLine(mPaddingLeft,mPaddingTop,mPaddingLeft-mArrowOffset,mPaddingTop+mArrowOffset,mArrowPaint);
        canvas.drawLine(mPaddingLeft,mPaddingTop,mPaddingLeft+mArrowOffset,mPaddingTop+mArrowOffset,mArrowPaint);

        for (int i = 1; i < 3; i++) {
            canvas.drawLine(mPaddingLeft,mHeight-mPaddingBottom-(mYoffset*i),mPaddingLeft-mYpointOffset,mHeight-mPaddingBottom-(mYoffset*i),mColLinePaint);
            canvas.drawText(String.valueOf(i-1),mPaddingLeft-mYpointOffset- myTextOffset,mHeight-mPaddingBottom-(mYoffset*i)+ mDrawYTextOffset, mTextPaint);
        }
    }

    /**
     * 画折线图
     * @param canvas
     */
    private void drawFoldLine(Canvas canvas) {
        Path path = new Path();
        if(datas[0] == 0){
            //y轴0处坐标点 mPaddingLeft,mHeight-mPaddingBottom-mYoffset
            path.moveTo(mPaddingLeft,mHeight-mPaddingBottom-mYoffset);
        }else if(datas[0] == 1){
            //y轴1处坐标点 mPaddingLeft,mHeight-mPaddingBottom-2*mYoffset
            path.moveTo(mPaddingLeft,mHeight-mPaddingBottom-(2*mYoffset));
        }
        for (int i = 1; i < datas.length; i++) {
            int num = datas[i];
            if(num == 0){
                path.lineTo(mPaddingLeft+i*mXoffset,mHeight-mPaddingBottom-mYoffset);
                canvas.drawLine(mPaddingLeft+i*mXoffset,mHeight-mPaddingBottom-mYoffset,mPaddingLeft+i*mXoffset,mHeight-mPaddingBottom,mVerticalLinePaint);
            }else if(num == 1){
                path.lineTo(mPaddingLeft+i*mXoffset,mHeight-mPaddingBottom-(2*mYoffset));
                canvas.drawLine(mPaddingLeft+i*mXoffset,mHeight-mPaddingBottom-(2*mYoffset),mPaddingLeft+i*mXoffset,mHeight-mPaddingBottom,mVerticalLinePaint);
            }
        }
        String s = Integer.toHexString(mBreakPoint);
        if(s.length() == 1){
            s = "0"+s;
        }
        canvas.drawText("折点为："+s,(mWidth-mPaddingRight)/2-50,mHeight-mPaddingBottom-(2*mYoffset)-10, mVerticalTextPaint);

        canvas.drawPath(path,mFoldLinePaint);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mXoffset = (mWidth-(mPaddingLeft+mPaddingRight))/datasLen;
        mYoffset = (mHeight-(mPaddingTop+mPaddingBottom))/3;

    }

    public void upDataChart(byte[] datas, int mBreakPoint){
        this.datas = datas;
        this.mBreakPoint = mBreakPoint;
        datasLen = datas.length;
        mXoffset = (mWidth-(mPaddingLeft+mPaddingRight))/datasLen;
        invalidate();
    }
}
