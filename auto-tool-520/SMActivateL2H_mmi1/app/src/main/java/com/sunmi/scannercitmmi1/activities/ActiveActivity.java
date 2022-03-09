package com.sunmi.scannercitmmi1.activities;

import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.sunmi.activation.ScanActivation;
import com.sunmi.scannercitmmi1.R;
import com.sunmi.scannercitmmi1.utils.Constants;
import com.sunmi.scannercitmmi1.utils.LicenseFileUtils;
import com.sunmi.scannercitmmi1.utils.NetUtils;

import static com.sunmi.scannercitmmi1.utils.Constants.LICENSE_PERSIST_ROOT_PATH;
import static com.sunmi.scannercitmmi1.utils.Constants.LICENSE_FILE;

public class ActiveActivity extends BaseActivity implements View.OnClickListener {

    private Button mBtnActiveDecode;
    private Button mBtnCheckActive;
    private TextView mTvSActiveInfo;
    private TextView tvCheckResult;
    private TextView tvSActiveState;
    private TextView tvSActiveTitle;
	private static final String ROOT_PATH = LICENSE_PERSIST_ROOT_PATH;

    public static final String TAG = "active_decode";

    private ScanActivation mScanSActivation = new ScanActivation();
    private static final int MSG_ACTIVE_EVENT = 0x1234;
    private static final int MSG_CHECK_ACTIVE = 0x2345;
    private static final int MSG_FINISH = 0x1111;
    private boolean mDeinitFlag = false;
    private boolean networkOnline = false;
    private boolean smResult = false;
    private final int ACTIVE = 1;
    private final int CHECK = 2;
    private int smRet = 0;
    private String sVersion = "";
    private String checkResult = "";

    private WorkTask ActiviteTask = null;
    private WorkTask CheckTask = null;
    private boolean TEST_START = false;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == MSG_ACTIVE_EVENT) {
                TEST_START = true;
                checkResult += "\n"+getString(R.string.activeing_state);
                tvSActiveState.setText(checkResult);
                if(null != ActiviteTask)ActiviteTask = null;
                ActiviteTask = new WorkTask(ACTIVE);
                ActiviteTask.execute(ACTIVE);
            } else if (message.what == MSG_CHECK_ACTIVE) {
                if(null!=CheckTask)CheckTask=null;
                CheckTask = new WorkTask(CHECK);
                CheckTask.execute(CHECK);

            } else if(message.what == MSG_FINISH){
                finish();
            }
            return false;
        }
    });

    class WorkTask extends AsyncTask{
        int type;

        WorkTask(int type){
            this.type = type;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            if(type == ACTIVE) {
                Log.d(TAG,"active start");
                LicenseFileUtils.initFileDirs(LICENSE_PERSIST_ROOT_PATH);
                networkOnline = NetUtils.isNetworkOnline(NetUtils.SUNMI_SERVER_IP);
                if (networkOnline){
                    doSAlgorithmActive();
                }
            }else if(type == CHECK) {
                Log.d(TAG,"check start");
                LicenseFileUtils.initFileDirs(LICENSE_PERSIST_ROOT_PATH);
                int smRet = mScanSActivation.isActivated();
                smResult = smRet >= 1;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            TEST_START = false;
            if(type == ACTIVE){
                updateResultUI();
                Log.d(TAG,"active end networkOnline: "+networkOnline);
            }else if(type == CHECK){
                handleCheckResult();
                Log.d(TAG,"check end smResult: "+smResult);
            }

            super.onPostExecute(o);

        }
    }

    private void handleActiveResult(){
        if (networkOnline){
            doSAlgorithmActive();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvSActiveInfo.setTextColor(Color.RED);
                    mTvSActiveInfo.setText("Connect useful internet first");
                    mBtnActiveDecode.setEnabled(true);
                    mBtnCheckActive.setEnabled(true);
                    mBtnActiveDecode.setClickable(true);
                    mBtnCheckActive.setClickable(true);
                    Toast.makeText(ActiveActivity.this, "Connect useful internet first", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleCheckResult(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (smResult) {
                    checkResult += "\n"+getString(R.string.active_result_success);
                    tvSActiveState.setText(checkResult);
                    //tvCheckResult.setTextColor(Color.GREEN);
                    if (mDeinitFlag) {
                        Intent intent = new Intent();
                        intent.putExtra("results", "active decode:true");
                        setResult(1111, intent);
                        mHandler.sendEmptyMessageDelayed(MSG_FINISH,1000);
                    }
                } else {
                    checkResult += "\n"+getString(R.string.active_result_fail)+"\n"+getString(R.string.activeing_reset);
                    tvSActiveState.setText(checkResult);
                    mBtnActiveDecode.setEnabled(true);
                    mBtnActiveDecode.setClickable(true);
                    //tvCheckResult.setTextColor(Color.RED);
                }


                /*mBtnActiveDecode.setEnabled(true);
                mBtnCheckActive.setEnabled(true);
                mBtnCheckActive.setClickable(true);
                mBtnActiveDecode.setClickable(true);*/
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTvSActiveInfo = (TextView) super.findViewById(R.id.tvSActiveResult);
        tvCheckResult = (TextView) super.findViewById(R.id.tvCheckResult);
        tvSActiveState = (TextView) super.findViewById(R.id.tvSActiveState);
        tvSActiveTitle = (TextView) super.findViewById(R.id.tvSActiveTitle);


        mBtnActiveDecode = (Button) super.findViewById(R.id.btnActiveDecode);
        mBtnActiveDecode.setOnClickListener(this);

        mBtnCheckActive = (Button) super.findViewById(R.id.btnCheckActive);
        mBtnCheckActive.setOnClickListener(this);

        String scanType = LicenseFileUtils.readScanDeviceInfoFromFile(Constants.SCAN_DEVICE_POINT);
        if (!TextUtils.isEmpty(scanType) && scanType.contains("SS110")) {
            Log.d(TAG, "onCreate: is SS1100 scan.");
            mBtnActiveDecode.setVisibility(View.VISIBLE);
            mBtnCheckActive.setVisibility(View.VISIBLE);
        } else {
            mBtnActiveDecode.setVisibility(View.GONE);
            mBtnCheckActive.setVisibility(View.GONE);
        }

        String mName = getIntent().getStringExtra("name");
        if(mName != null){
            mDeinitFlag = true;
            Log.d(TAG, "onCreate mDeinitFlag: " + mDeinitFlag);
            tvSActiveTitle.setText(mName+"");
        }
        String ScanStartType= getIntent().getStringExtra("ScanStartType");
        Log.d(TAG, "onCreate ScanStartType: " + ScanStartType);
        if((ScanStartType != null) && ( "auto".equals(ScanStartType) || "pcbaautotest".equals(ScanStartType))){
            Log.d(TAG, "onCreate send MSG_ACTIVE_EVENT");
            mTvSActiveInfo.setTextColor(Color.GRAY);
            mTvSActiveInfo.setText("Activating...");
            mBtnActiveDecode.setEnabled(false);
            mBtnCheckActive.setEnabled(false);
            mBtnActiveDecode.setClickable(false);
            mBtnCheckActive.setClickable(false);
            mHandler.sendEmptyMessageDelayed(MSG_ACTIVE_EVENT, 1000);
        }
        mBtnCheckActive.setVisibility(View.GONE);
        mBtnActiveDecode.setEnabled(false);
        mBtnActiveDecode.setClickable(false);

        mHandler.sendEmptyMessage(MSG_CHECK_ACTIVE);
        checkResult = getString(R.string.activeing_state);
        tvSActiveState.setText(checkResult);
    }

    @Override
    public int getLayout() {
        return R.layout.activity_active;
    }

    @Override
    public boolean isToRequestPermissions() {
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnActiveDecode:
                mTvSActiveInfo.setTextColor(Color.GRAY);
                mTvSActiveInfo.setText("Activating...");
                mBtnActiveDecode.setEnabled(false);
                mBtnCheckActive.setEnabled(false);
                mBtnCheckActive.setClickable(false);
                mBtnActiveDecode.setClickable(false);
                mHandler.sendEmptyMessage(MSG_ACTIVE_EVENT);
                break;
            case R.id.btnCheckActive:
                mBtnActiveDecode.setEnabled(false);
                mBtnCheckActive.setEnabled(false);
                mBtnActiveDecode.setClickable(false);
                mBtnCheckActive.setClickable(false);
                mHandler.sendEmptyMessage(MSG_CHECK_ACTIVE);
                break;
        }
    }

    private void doGrantSPermission() {
        LicenseFileUtils.grantPermission(LICENSE_PERSIST_ROOT_PATH + LICENSE_FILE, false);
    }

    @Override
    public void onBackPressed() {
        if(TEST_START)return;
        super.onBackPressed();
    }

    private void doSAlgorithmActive() {
        LicenseFileUtils.deleteLicenseFile(LICENSE_PERSIST_ROOT_PATH, LICENSE_FILE);
        smRet = mScanSActivation.licenseActivation();
        Log.d(TAG, "doSAlgorithmActive: smRet = " + smRet);
        sVersion = mScanSActivation.getVersion();

        doGrantSPermission();
    }

    private void updateResultUI(){
        if (networkOnline) {
            String smActiveInfo = "";
            if (smRet >= 1) {
                smActiveInfo = "SAlgo success and V is " + sVersion;
                Log.d(TAG, smActiveInfo);
                mTvSActiveInfo.setTextColor(Color.GREEN);
                checkResult += "\n"+getString(R.string.active_result_success);
                if (mDeinitFlag) {
                    Intent intent = new Intent();
                    intent.putExtra("results", "active decode:true");
                    setResult(1111, intent);
                    mHandler.sendEmptyMessageDelayed(MSG_FINISH,1000);
                }
            } else {
                checkResult += "\n"+getString(R.string.active_result_fail)+": "+smRet+"\n"+getString(R.string.activeing_reset);
                smActiveInfo = "SAlgo failed and V is " + sVersion;
                Log.d(TAG, smActiveInfo);
                mTvSActiveInfo.setTextColor(Color.RED);
//                if (mDeinitFlag) {
//                    Intent intent = new Intent();
//                    intent.putExtra("results", "active decode:fail");
//                    setResult(1111, intent);
//                    mHandler.sendEmptyMessageDelayed(MSG_FINISH,1000);
//                }
            }
            tvSActiveState.setText(checkResult);
            mTvSActiveInfo.setText(smActiveInfo);
            mBtnActiveDecode.setEnabled(true);
            mBtnCheckActive.setEnabled(true);
            mBtnActiveDecode.setClickable(true);
            mBtnCheckActive.setClickable(true);
        }else{
            mTvSActiveInfo.setTextColor(Color.RED);
            mTvSActiveInfo.setText("Connect useful internet first");
            mBtnActiveDecode.setEnabled(true);
            mBtnCheckActive.setEnabled(true);
            mBtnActiveDecode.setClickable(true);
            mBtnCheckActive.setClickable(true);
            Toast.makeText(ActiveActivity.this, "Connect useful internet first", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy start");
        super.onDestroy();
        Log.d(TAG, "onDestroy end");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop start");
        if(null!=ActiviteTask) {
            ActiviteTask.cancel(true);
            ActiviteTask = null;
        }
        if(null!=CheckTask) {
            CheckTask.cancel(true);
            CheckTask = null;
        }
        Log.d(TAG, "onStop end");
    }
}
