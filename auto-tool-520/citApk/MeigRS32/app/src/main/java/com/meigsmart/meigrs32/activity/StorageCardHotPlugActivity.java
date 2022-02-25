package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import java.lang.reflect.Method;
import java.util.List;

import butterknife.BindView;

public class StorageCardHotPlugActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private StorageCardHotPlugActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.sdState)
    public TextView mSDState;
    @BindView(R.id.info)
    public TextView mInfo;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean mStorageInsertAction = false;
    private boolean mStoragePulloutAction = false;
    private StorageManager mManager;
    private String current_state;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_storage_card_hotplug;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.StorageCardHotPlugActivity);
        mSuccess.setOnClickListener(this);
        //mSuccess.setVisibility(View.VISIBLE);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);

        mConfigResult = getResources().getInteger(R.integer.sim_card_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        mContext.registerReceiver(mSDReceiver, filter);
        if(getSDCardMemory() != null)
            mInfo.setText(R.string.storage_card_hot_plug_after_insert_info);
        else mInfo.setText(R.string.storage_card_hot_plug_insert_info);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mManager.registerListener(mListener);
        mFlag.setVisibility(View.GONE);
        mLayout.setVisibility(View.VISIBLE);
        mHandler.sendEmptyMessage(1001);
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if (getSecondaryStorageState()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(getResources().getString(R.string.sd_card_size_info)).append(" ").append(getSDCardMemory()).append("\n");
                        /*mSDState.setText(getResult(DataUtil.getRomSpace(mContext),DataUtil.getTotalMemory(mContext,
                            getResources().getString(R.string.version_default_config_software_version_ram_size_path))
                            ,getSDCardMemory()));*/
                       mSDState.setText(sb.toString());
                    }else{
                        if(null!=current_state && current_state.equals(Environment.MEDIA_BAD_REMOVAL)){
                            mStoragePulloutAction = true;
                            mInfo.setText(R.string.storage_card_hot_plug_after_pull_out_info);
                            mSDState.setText("");
                            mSDState.setVisibility(View.INVISIBLE);
                        }
                    }
                    if(!mStoragePulloutAction || !mStorageInsertAction)
                        mHandler.sendEmptyMessageDelayed(1001, 1000);
                    else mHandler.sendEmptyMessage(1002);
                    break;
                case 1002:
                    if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                        deInit(mFatherName, SUCCESS);
                    }else
                        mSuccess.setVisibility(View.VISIBLE);
                    mHandler.sendEmptyMessageDelayed(1001, 1000);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mManager.unregisterListener(mListener);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    private String getResult(String rom,String ram,String sd){
        StringBuffer sb = new StringBuffer();
        sb.append("ROM size:").append(" ").append(rom).append("\n");
        sb.append("RAM size:").append(" ").append(ram).append("\n");
        sb.append("Storage SD size:").append(" ").append(sd).append("\n");
        return sb.toString();
    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess){
            //mSuccess.setTextColor(getResources().getColor(R.color.green_1));
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            //mFail.setTextColor(getResources().getColor(R.color.red_800));
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }
    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            deInit(mFatherName, result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(mFatherName, result);
        }
    }

    private final BroadcastReceiver mSDReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("SDCardTest", "Receive " + action);
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action) && getSecondaryStorageState()) {
                mStorageInsertAction = true;
                mInfo.setText(R.string.storage_card_hot_plug_after_insert_info);
                StringBuffer sb = new StringBuffer();
                sb.append(getResources().getString(R.string.sd_card_size_info)).append(" ").append(getSDCardMemory()).append("\n");
                mSDState.setText(sb.toString());
                mSDState.setVisibility(View.VISIBLE);
            }else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                if(current_state==null||current_state.equals(Environment.MEDIA_BAD_REMOVAL) && !getSecondaryStorageState()) {
                    mStoragePulloutAction = true;
                    mInfo.setText(R.string.storage_card_hot_plug_after_pull_out_info);
                    mSDState.setText("");
                    mSDState.setVisibility(View.INVISIBLE);
                }
            }


        }
    };

    private StorageEventListener mListener = new StorageEventListener(){
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            LogUtil.d("SDCardTest:" + newState);
            current_state = newState;
        }
    };

    /**
     * ram
     * @return
     */
    /**
     * sd
     * @return
     */
    public String getSDCardMemory() {
        try{
            //File path = Environment.getExternalStorageDirectory();
            StatFs statFs = new StatFs(getSDPath(mContext));
            long blocksize = statFs.getBlockSize();
            long totalblocks = statFs.getBlockCount();
            long availableblocks = statFs.getAvailableBlocks();
            long totalsize = blocksize * totalblocks;
            long availablesize = availableblocks * blocksize;
            String totalsize_str = Formatter.formatFileSize(this, totalsize);
            String availablesize_strString = Formatter.formatFileSize(this,
                    availablesize);
            return totalsize_str;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    private String getSDPath(Context mcon) {
        String sd = null;
        StorageManager mStorageManager = (StorageManager) mcon
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        List<VolumeInfo> volumeInfos = mStorageManager.getVolumes();
        for(VolumeInfo s:volumeInfos){
            if(null!=s && null!=s.getDisk() && s.getDisk().isSd()){
                sd = s.getPath().toString();
            }
        }
        return sd;
    }

    public boolean getSecondaryStorageState() {
        try {
            StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
            List<DiskInfo> diskInfos = sm.getDisks();
            for(DiskInfo s:diskInfos){
                LogUtil.d("SDCardTest:   " +s.getId());
                if(s.isSd()){
                    return true;
                }
            }
        }catch (Exception e){
            LogUtil.d("SDCardTest:   " +e.toString());
            e.printStackTrace();
        }
        return false;
    }

}
