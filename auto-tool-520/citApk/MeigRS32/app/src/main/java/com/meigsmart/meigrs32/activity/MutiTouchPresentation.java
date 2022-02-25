package com.meigsmart.meigrs32.activity;

import java.util.ArrayList;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;


public class MutiTouchPresentation extends Presentation implements OnTouchListener {

	private DisplayMetrics mDisplayMetrics;
	private MainHandler mHandler;
	private Context mContext;
	private ArrayList<PointF> mPointFList = new ArrayList<PointF>();
	private int mRectWidth;
	private int mRectHeight;
	private MuiltImageView mMuiltImageView;
	private boolean mTestSuccess;
	private boolean onTouch = false;
	private boolean touchNumBool;
	
	public MutiTouchPresentation(Context outerContext, Display display) {
		super(outerContext, display);
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        mRectWidth = dm.widthPixels;
        mRectHeight = dm.heightPixels;
		touchNumBool = false;
        mHandler = new MainHandler();
        mMuiltImageView = new MuiltImageView(getContext(), mHandler);
        setContentView(R.layout.mutitouch_presentation);
        LinearLayout layout = (LinearLayout) findViewById(R.id.root_view);
        mMuiltImageView.setOnTouchListener(this);
        mMuiltImageView.setMinimumHeight(mRectHeight);  
        mMuiltImageView.setMinimumWidth(mRectWidth);
        mMuiltImageView.invalidate();  
        layout.addView(mMuiltImageView);
	}
	
	public boolean ismTestSuccess() {
		return mTestSuccess;
	}

	public void setmTestSuccess(boolean mTestSuccess) {
		this.mTestSuccess = mTestSuccess;
	}

	private class MainHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1 && touchNumBool) {
				setmTestSuccess(true);
				dismiss();
			}
		}
	}
	
	private class MuiltImageView extends View {
		private static final float RADIUS = 30f;
		private Handler mHandler;
		private boolean mPass = false;
		private int mWidth, mHeight;
		private String touchPoint;

		public MuiltImageView(Context context, Handler handler){
			super(context);
			mHandler = handler;
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
									mRectWidth, mPointFList.get(j).y,
									linerLaint);
							canvas.drawLine(mPointFList.get(j).x, 0,
									mPointFList.get(j).x, mRectHeight,
									linerLaint);
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
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			mPointFList.clear();
		}

		if (event.getPointerCount() > 0) {
			if (event.getPointerCount() >= 10) {
					touchNumBool = true;
				}
			
			for (int j = 0; j < event.getPointerCount(); j++) {
				PointF piont = new PointF();
				piont.set(event.getX(j), event.getY(j));
				mPointFList.add(piont);
			}
			onTouch = true;
		}

		if (event.getAction() == MotionEvent.ACTION_UP/* && mPass */) {
			mHandler.sendEmptyMessage(1);
			mPointFList.clear();
		}
		mMuiltImageView.invalidate();
		return true;
	}

}
