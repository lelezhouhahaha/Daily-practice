package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class SecondMutiTouchTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack{
	private SecondMutiTouchTestActivity mContext;
	@BindView(R.id.title)
	public TextView mTitle;
	@BindView(R.id.back)
	public LinearLayout mBack;
	@BindView(R.id.success)
	public Button mSuccess;
	@BindView(R.id.fail)
	public Button mFail;
	@BindView(R.id.second_muti_touch_msg)
	public TextView mSecondMutitouchMsg;
	private String mFatherName = "";

	private DisplayManager mDisplayManager;
	private Display[] displays = null;
	private MutiTouchPresentation mMutiTouchPresentation;

	private int mConfigTime = 0;
	private Runnable mRun;

	@Override
	protected int getLayoutId() {
		return R.layout.activity_second_mutitouch;
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
		mTitle.setText(R.string.SecondMutiTouchTestActivity);

		mDialog.setCallBack(this);
		mFatherName = getIntent().getStringExtra("fatherName");
		super.mName = getIntent().getStringExtra("name");
		addData(mFatherName, super.mName);

		mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
		displays = mDisplayManager
				.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);

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

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		hidePresentation();
		mHandler.removeCallbacks(mRun);
		super.onDestroy();
	}
	
	private void showDialog() {
		AlertDialog.Builder dialog = 
	            new AlertDialog.Builder(SecondMutiTouchTestActivity.this);
		dialog.setMessage(R.string.dialog_msg);
		dialog.setPositiveButton(R.string.str_yes,
	            new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	if (displays.length > 0) {
	            		mSecondMutitouchMsg.setText(R.string.second_touch_msg);
	        			showPresentation(displays[0]);
	        		}
	            }
	        });
		dialog.setNegativeButton(R.string.str_no,
	            new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
					mSuccess.setVisibility(View.VISIBLE);
	            }
	        });
		dialog.setCancelable(false);	
		dialog.show();
	}

	private void hidePresentation() {
		if (mMutiTouchPresentation != null && mMutiTouchPresentation.isShowing()) {
			mMutiTouchPresentation.dismiss();
		}
	}

	private void showPresentation(Display display) {

		mMutiTouchPresentation = new MutiTouchPresentation(this, display);
		mMutiTouchPresentation.show();
		mMutiTouchPresentation.setOnDismissListener(mOnDismissListener);

	}

	private final DialogInterface.OnDismissListener mOnDismissListener = new DialogInterface.OnDismissListener() {
		@Override
		public void onDismiss(DialogInterface dialog) {
			MutiTouchPresentation presentation = (MutiTouchPresentation) dialog;
			int displayId = presentation.getDisplay().getDisplayId();
			if (mMutiTouchPresentation.ismTestSuccess()) {
				mSuccess.setVisibility(View.VISIBLE);
				deInit(mFatherName, SUCCESS);
			}
		}
	};

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
