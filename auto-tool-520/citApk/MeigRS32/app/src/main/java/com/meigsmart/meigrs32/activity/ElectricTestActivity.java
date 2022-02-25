package com.meigsmart.meigrs32.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.log.LogUtil;

import java.util.List;

import butterknife.BindView;

public class ElectricTestActivity extends BaseActivity implements View.OnClickListener {
	@BindView(R.id.start_test)
	public Button start;
	@BindView(R.id.close_test)
	public Button close;
	@BindView(R.id.title)
	public TextView mTitle;
	@BindView(R.id.back)
	public LinearLayout mBack;

	PowerManager pm;
	private Context context;
	private WakeLock wakeLock;
	Handler handler = new Handler();
	Runnable runnable;

	private NfcAdapter mDefaultAdapter;
	private boolean mLastNfcStatus = false;

	private static final String ELECTRIC_TEST_ACTION = "meig.intent.action.ELECTRIC_TEST";

	@Override
	protected int getLayoutId() {
		return R.layout.electric_test;
	}

	@Override
	protected void initData() {
		mTitle.setText("Electric Test");
		mBack.setVisibility(View.VISIBLE);
		start.setOnClickListener(this);
		close.setOnClickListener(this);
		mBack.setOnClickListener(this);
		context = this;

		mDefaultAdapter = NfcAdapter.getDefaultAdapter(context);

		try
		{
			if(android.provider.Settings.System.getInt(getContentResolver(),android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE) == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
			{
				android.provider.Settings.System.putInt(getContentResolver(),
						android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
						android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			}
		}
		catch (SettingNotFoundException e)
		{
			e.printStackTrace();
		}
		pm = (PowerManager)getSystemService(Context.POWER_SERVICE);

	}

	private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
	     @Override
	     public void onReceive(final Context context, final Intent intent) {
	         final String action = intent.getAction();
			 System.out.println("tfs---->"+action);
	         if (Intent.ACTION_SCREEN_OFF.equals(action)) {
	        	 //pm.setBacklightBrightness(curBackground);
	        	 if(getAirplaneMode(context)){
	        		 setAirplaneModeOn(context, false);
	        	 }
	         }  
	     }  
	 }; 
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			
		case R.id.start_test:
			disableNfc();
            setElectricBroadcast(true);
			setAirplaneModeOn(context, true);
			try {
				killAll();
				pm.goToSleep(SystemClock.uptimeMillis());
			}catch (Exception e){
				e.printStackTrace();
			}
			break;
		case R.id.close_test:
			restoreNfc();
            setElectricBroadcast(false);
			setAirplaneModeOn(context, false);
			finish();
			break;
		case R.id.back:
			finish();
			break;
		}
	}


	private void setElectricBroadcast(boolean start){
	    Intent intent = new Intent(ELECTRIC_TEST_ACTION);
	    intent.putExtra("electric_test",start);
	    sendBroadcast(intent);
	}

	private void disableNfc(){
		try{
			if(mDefaultAdapter!=null){
				if(mDefaultAdapter.isEnabled()){
					mLastNfcStatus = true;
					mDefaultAdapter.disable();
				}
			}
		}catch (Exception e){
			LogUtil.d("disableNfc()");
		}
	}

	private void restoreNfc(){
        try{
            if(mDefaultAdapter != null){
            	if(mLastNfcStatus && !mDefaultAdapter.isEnabled()){
                    mDefaultAdapter.enable();
                }
            }
        }catch (Exception e){
			LogUtil.d("restorenfc()");
		}
	}

	public static void setAirplaneModeOn(Context context, boolean enabling) {
	    Settings.Global.putInt(context.getContentResolver(),
	                         Settings.System.AIRPLANE_MODE_ON,enabling ? 1 : 0);
	    Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
	    intent.putExtra("state", enabling);
	    context.sendBroadcast(intent);
	}
	
	public static boolean getAirplaneMode(Context context){
	    int isAirplaneMode = Settings.Global.getInt(context.getContentResolver(),
	                          Settings.System.AIRPLANE_MODE_ON, 0) ;
	    return (isAirplaneMode == 1)?true:false;  
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}

	public void killAll() {

		ActivityManager activityManager = (ActivityManager)
				getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> appProcessInfos = activityManager
				.getRunningAppProcesses();

		for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfos) {
			if (appProcessInfo.processName.contains("com.android.system")
					|| appProcessInfo.pid == android.os.Process.myPid())
				continue;
			String[] pkNameList = appProcessInfo.pkgList;
			for (int i = 0; i < pkNameList.length; i++) {
				String pkName = pkNameList[i];
				activityManager.killBackgroundProcesses(pkName);
			}
		}
	}

}
