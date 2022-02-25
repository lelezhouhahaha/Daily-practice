package com.meigsmart.meigrs32.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;


public class LCDRGBPresentation extends Presentation{

	private Context mContext;
	
	
	private int[] ids = {
            Color.parseColor("#FF0000"),Color.parseColor("#00FF00"),Color.parseColor("#0000FF"),
            Color.parseColor("#888888"),Color.parseColor("#000000"),Color.parseColor("#FFFFFF"),
    };
    private int TIME_VALUES = 1000;
    private int currPosition = 0;
    public TextView mFlag;
    private int mConfigResult;
    private int mConfigTime = 0;
	private LinearLayout mLayout;
	private Button mSuccess;
	private Button mFail;
	private boolean mIsSuccess;
	private Runnable mRunnable;

	public LCDRGBPresentation(Context outerContext, Display display) {
		super(outerContext, display);
		mContext = outerContext;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        setContentView(R.layout.activity_lcd_rgb);
		mLayout = (LinearLayout) findViewById(R.id.layout);
		mFlag = (TextView) findViewById(R.id.flag);
		mSuccess = (Button) findViewById(R.id.success);
		mFail = (Button) findViewById(R.id.fail);
		
		mFlag.setVisibility(View.GONE);
		mSuccess.setVisibility(View.GONE);
		mFail.setVisibility(View.GONE);
		mRunnable = new Runnable() {
			@Override
			public void run() {
				mFlag.setVisibility(View.GONE);
				mLayout.setBackgroundColor(ids[currPosition]);
				currPosition++;
				if(currPosition == 6){
					currPosition = 0;
					setmIsSuccess(true);
					dismiss();
				}
				mHandler.postDelayed(this,TIME_VALUES);
			}
		};
		mRunnable.run();
	}
	


	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

		}
	};
	
	public boolean ismIsSuccess() {
		return mIsSuccess;
	}

	public void setmIsSuccess(boolean mIsSuccess) {
		this.mIsSuccess = mIsSuccess;
	}
}
