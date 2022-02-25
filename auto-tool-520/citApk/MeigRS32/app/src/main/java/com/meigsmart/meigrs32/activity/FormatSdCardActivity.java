package com.meigsmart.meigrs32.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.log.LogUtil;

import java.util.List;

import butterknife.BindView;

public class FormatSdCardActivity extends BaseActivity {
	@BindView(R.id.start_test)
	public Button start;
	@BindView(R.id.close_test)
	public Button close;
	@BindView(R.id.title)
	public TextView mTitle;
	@BindView(R.id.back)
	public LinearLayout mBack;
	AlertDialog mAlertDialog = null;

	PowerManager pm;
	private Context context;
	private WakeLock wakeLock;
	Handler handler = new Handler();
	Runnable runnable;
	TextView mSaveResult;
	private boolean formated = false;

	@Override
	protected int getLayoutId() {
		return R.layout.electric_test;
	}

	@Override
	protected void initData() {
		mBack.setVisibility(View.GONE);
		start.setVisibility(View.GONE);
		close.setVisibility(View.GONE);
		context = this;
		if(getSDStorageState()){
			new FormatSD().execute();
		}else{
			createNoSdDialog();
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	private class FormatSD extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			createDialog();
		}

		@Override
		protected Void doInBackground(Void... params) {

			try {

				StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
				List<VolumeInfo> volumes = mStorageManager.getVolumes();
				for (VolumeInfo volInfo : volumes)
				{
					DiskInfo diskInfo = volInfo.getDisk();
					if (diskInfo != null && diskInfo.isSd()) {
						mStorageManager.partitionPublic(diskInfo.getId());
						formated = true;
					}
				}

			} catch (Exception e) {

			}

			return null;

		}

		@Override
		protected void onPostExecute(Void e) {

			LogUtil.d("SaveResult -- >onPostExecute");
			updateDialg();

		}

	}

	private void updateDialg(){
		mAlertDialog.setCancelable(true);
		ProgressBar mProgressBar = (ProgressBar)mAlertDialog.findViewById(R.id.save_loading);
		TextView mMessageView = (TextView) mAlertDialog.findViewById(R.id.result_textView);

		mProgressBar.setVisibility(View.GONE);
		mMessageView.setVisibility(View.GONE);
		if(formated){
			mSaveResult.setText(R.string.format_sdcard_finish);
			mSaveResult.setTextColor(Color.GREEN);
		}else{
			mSaveResult.setText(R.string.sd_state_flag);
			mSaveResult.setTextColor(Color.RED);
		}

	}

	private void createDialog(){
		View view = View.inflate(getApplicationContext(), R.layout.dialog_result, null);
		mAlertDialog = new AlertDialog.Builder(context)
				.setView(view)
				.create();
		mAlertDialog.setCancelable(false);
		mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});
		mAlertDialog.show();
		mSaveResult = (TextView) mAlertDialog.findViewById(R.id.save_result);
		mSaveResult.setText(R.string.format_sdcard);
	}

	private void createNoSdDialog(){
		mAlertDialog = new AlertDialog.Builder(context)
				.setMessage(R.string.sd_state_flag)
				.create();
		mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});
		mAlertDialog.show();
	}

	public boolean getSDStorageState() {
		try {
			StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
			List<VolumeInfo> volumes = mStorageManager.getVolumes();
			for (VolumeInfo volInfo : volumes)
			{
				DiskInfo diskInfo = volInfo.getDisk();
				if (diskInfo != null && diskInfo.isSd()) {
					return true;
				}
			}
		} catch (Exception e) {

		}

		return false;
	}

}
