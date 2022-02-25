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
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;


public class LCDBrightnessPresentation extends Presentation{

	private Context mContext;
    private int TIME_VALUES = 1000;
    private int currPosition = 0;
    public TextView mFlag;
    private int mConfigResult;
    private int mConfigTime = 0;
	private Button mSuccess;
	private Button mFail;
	private TextView mValues;

	private int curBackground = 50;
	private int background = 50;
	private PowerManager pm;
	private boolean mIsSuccess;
	private Runnable mRunnable;

	public LCDBrightnessPresentation(Context outerContext, Display display) {
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
        setContentView(R.layout.activity_lcd_brightness);
		mValues = (TextView) findViewById(R.id.values);
		mFlag = (TextView) findViewById(R.id.flag);
		mSuccess = (Button) findViewById(R.id.success);
		mFail = (Button) findViewById(R.id.fail);
		
		
		pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
		background = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 50);

		mSuccess.setVisibility(View.GONE);
		mFail.setVisibility(View.GONE);

		mRunnable = new Runnable() {
			@Override
			public void run() {
				curBackground+=100;
				if(curBackground>255) {
					curBackground = 50;
				}
				if(curBackground <= 50){
					setmIsSuccess(true);
				}
				mValues.setText(String.valueOf(curBackground));
				Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, curBackground);
			}
		};
	}

	public void setBrightness(){
		if(mRunnable != null){
			mRunnable.run();
		}
	}
	
	public boolean ismIsSuccess() {
		return mIsSuccess;
	}

	public void setmIsSuccess(boolean mIsSuccess) {
		this.mIsSuccess = mIsSuccess;
	}
}
