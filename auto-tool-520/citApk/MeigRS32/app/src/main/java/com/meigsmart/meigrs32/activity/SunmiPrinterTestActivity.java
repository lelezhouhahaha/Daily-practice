package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;
import woyou.aidlservice.jiuiv5.IWoyouService;

public class SunmiPrinterTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{

	private String TAG = "PrinterTest";

	private SunmiPrinterTestActivity mContext;
	@BindView(R.id.title)
	public TextView mTitle;
	@BindView(R.id.back)
	public LinearLayout mBack;
	@BindView(R.id.success)
	public Button mSuccess;
	@BindView(R.id.fail)
	public Button mFail;
	@BindView(R.id.text_msg)
	public TextView mMsg;
	@BindView(R.id.btn_test)
	public Button mBtnPrint;
	private String mFatherName = "";

	private int mConfigTime = 0;
	private Runnable mRun;

	private IWoyouService mWoyouService = null;
	private boolean showSuccessButton = false;
	
	private ServiceConnection mConnService = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
				Toast.makeText(SunmiPrinterTestActivity.this, "service disconnected", Toast.LENGTH_LONG).show();
				Log.d(TAG,"server disconnect");
				//setButtonEnable(false);
				mWoyouService = null;
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Binding();
			}
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mWoyouService = IWoyouService.Stub.asInterface(service);
				try {
					mWoyouService.printerInit(null);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				Log.d("Printer_zhr","printerinit");
				
				refreshState();
			}
		};
		
	private void refreshState() {
		String msg = "";
		try {
			String serviceVersion = mWoyouService.getServiceVersion();
			Log.d("Printer_zhr","serviceVersion:"+serviceVersion);
			Log.d("Printer_zhr","mWoyouService.getPrinterVersion():"+mWoyouService.getPrinterVersion());
			msg = msg+"Service Version:  "+serviceVersion+"\n";
			msg = msg+"Printer Version:  "+mWoyouService.getPrinterVersion()+"\n";
			mMsg.setText(msg);
		} catch (RemoteException e) {
			Log.d(TAG, "====>error:"+e.toString());
			e.printStackTrace();
		}
	}
	
	private void showDialog() {
		AlertDialog.Builder dialog = 
	            new AlertDialog.Builder(SunmiPrinterTestActivity.this);
		dialog.setMessage(R.string.do_printerTest);
		dialog.setPositiveButton(R.string.str_yes,
	            new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	mBtnPrint.setEnabled(true);
	            	Binding();
					mBtnPrint.setOnClickListener(new OnClickListener() {
	        			
	        			@Override
	        			public void onClick(View v) {
	        				try {

	        					Log.d("Printer_zhr","printerSelfChecking");
								refreshState();
	        					mWoyouService.printerSelfChecking(null);
	        					if (!showSuccessButton) {
									mSuccess.setVisibility(View.VISIBLE);
	        						showSuccessButton = true;
	        					}
	        					
	        				} catch (RemoteException e) {
	        					Log.d(TAG, "====>print error:e=>"+e.toString());
	        					e.printStackTrace();
	        				}
	        			}
	        		});
	            }
	        });
		dialog.setNegativeButton(R.string.str_no, 
	            new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	mBtnPrint.setEnabled(false);
	            	if (!showSuccessButton) {
						mSuccess.setVisibility(View.VISIBLE);
						showSuccessButton = true;
					}
	            }
	        });
		dialog.setCancelable(false);	
		dialog.show();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.activity_print_test;
	}

	@Override
	protected void initData() {
		mContext = this;
		super.startBlockKeys = Const.isCanBackKey;
		mBack.setVisibility(View.GONE);
		mSuccess.setVisibility(View.GONE);
		//mFail.setVisibility(View.GONE);
		mBack.setOnClickListener(this);
		mSuccess.setOnClickListener(this);
		mFail.setOnClickListener(this);
		mTitle.setText(R.string.SunmiPrinterTestActivity);

		mDialog.setCallBack(this);
		mFatherName = getIntent().getStringExtra("fatherName");
		super.mName = getIntent().getStringExtra("name");
		addData(mFatherName, super.mName);

		if(mFatherName.equals(MyApplication.RuninTestNAME)) {
			mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
		} else {
			mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
		}

		mBtnPrint = (Button)findViewById(R.id.btn_test);
		mMsg = (TextView)findViewById(R.id.text_msg);

		mHandler.sendEmptyMessage(1001);
		mRun = new Runnable() {
			@Override
			public void run() {
				mConfigTime--;
				updateFloatView(mContext,mConfigTime);
				if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
					mHandler.sendEmptyMessage(1111);
				}
				mHandler.postDelayed(this, 1000);
			}
		};
		mRun.run();
	}


	private void Binding(){
		Intent intent=new Intent();
		intent.setPackage("woyou.aidlservice.jiuiv5");
		intent.setAction("woyou.aidlservice.jiuiv5.IWoyouService");
		startService(intent);
		bindService(intent, mConnService,Context.BIND_AUTO_CREATE);
	}



	@Override
	protected void onDestroy() {
		try {
			unbindService(mConnService);
		} catch (Exception e) {
			// TODO: handle exception
		}
		mHandler.removeCallbacks(mRun);
		super.onDestroy();
	}


	@Override
	public void onClick(View v) {
		if (v == mBack){
			if (!mDialog.isShowing())mDialog.show();
			mDialog.setTitle(super.mName);
		}
		if (v == mSuccess){
			mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
			deInit(mFatherName, SUCCESS);
		}
		if (v == mFail){
			mFail.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.red_800));
			deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
		}
	}

	@Override
	public void onResultListener(int result) {
		if (result == 0){
			deInit(mFatherName, result,Const.RESULT_NOTEST);
		}else if (result == 1){
			deInit(mFatherName, result,Const.RESULT_UNKNOWN);
		}else if (result == 2){
			deInit(mFatherName, result);
		}
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what){
				case 1001:
					showDialog();
					break;
				case 1111:
					deInit(mFatherName, FAILURE);
					break;
				case 9999:
					deInit(mFatherName, FAILURE,msg.obj.toString());
					break;
			}
		}
	};
}
