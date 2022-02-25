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
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class StorageCardActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private StorageCardActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.sdState)
    public TextView mSDState;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private StorageManager mManager;
    private String projectName = "";
    float Sdcard_memory ;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_storage_card;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.pcba_storage_card);
        mSuccess.setOnClickListener(this);
        //mSuccess.setVisibility(View.VISIBLE);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

        mConfigResult = getResources().getInteger(R.integer.sim_card_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        //mHandler.sendEmptyMessageDelayed(1001, 100);
        mManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);

        /*long  l = Environment.getDataDirectory().getTotalSpace();
        long s = Environment.getStorageDirectory().getFreeSpace();
        long maxFileSize = Environment.getDataDirectory().getUsableSpace();
        LogUtil.w(Formatter.formatFileSize(getBaseContext(), l));
        LogUtil.w(Formatter.formatFileSize(getBaseContext(), s));
        LogUtil.w(Formatter.formatFileSize(getBaseContext(), maxFileSize));*/

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
        mFlag.setVisibility(View.GONE);
        mLayout.setVisibility(View.VISIBLE);
        mManager.registerListener(mListener);
        mHandler.sendEmptyMessage(1001);
    }

    @Override
    protected void onPause(){
        super.onPause();
        mManager.unregisterListener(mListener);
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
                        if(projectName.contains("MT537")) {
                            if(getSDCardMemory()!=null){
                                Sdcard_memory = Float.parseFloat(getSDCardMemory().substring(0, getSDCardMemory().length() - 3));
                                if (Sdcard_memory > 80 && Sdcard_memory < 130) {
                                    if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME)) {
                                        deInit(mFatherName, SUCCESS);
                                    } else {
                                        mSuccess.setVisibility(View.VISIBLE);
                                    }
                                } else {
                                    sb.append(getResources().getString(R.string.sd_card_size_not_128));
                                }
                                mSDState.setText(sb.toString());
                            }else{
                                mSDState.setText(getResources().getString(R.string.storage_card_hot_plug_insert_info));
                            }
                        }else{
                            if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME)) {
                                deInit(mFatherName, SUCCESS);
                            } else {
                                mSuccess.setVisibility(View.VISIBLE);
                            }
                        mSDState.setText(sb.toString());
                        }
                    }else {
                        mSuccess.setVisibility(View.GONE);
                        mSDState.setText(R.string.sd_state_flag);
                        if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)&&!mFatherName.equals(getResources().getString(R.string.sunmi_subsim_signal))&&
                                !mFatherName.equals(getResources().getString(R.string.mmi_one_test_manual))&&!mFatherName.equals(getResources().getString(R.string.mmi_two_test_manual))) {
                            deInit(mFatherName, FAILURE);
                        }
                    }
                    mHandler.sendEmptyMessageDelayed(1001, 1000);
                    break;
                case 1002:
                    String newState = (String) msg.obj;
                    if (newState.equals(Environment.MEDIA_MOUNTED) && getSecondaryStorageState()){//sd use
                        StringBuffer sb = new StringBuffer();
                        sb.append(getResources().getString(R.string.sd_card_size_info)).append(" ").append(getSDCardMemory()).append("\n");
                        /*mSDState.setText(getResult(DataUtil.getRomSpace(mContext),DataUtil.getTotalMemory(mContext,
                            getResources().getString(R.string.version_default_config_software_version_ram_size_path))
                            ,getSDCardMemory()));*/
                        mSDState.setText(sb.toString());
                        if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                            deInit(mFatherName, SUCCESS);
                        }else
                            mSuccess.setVisibility(View.VISIBLE);
                    }else if (newState.equals(Environment.MEDIA_BAD_REMOVAL)){//remove
                        mSDState.setText(R.string.sd_state_flag);
                        if(!mFatherName.equals(MyApplication.PCBASignalNAME)&&!mFatherName.equals(MyApplication.PreSignalNAME)) {
                            deInit(mFatherName, FAILURE);
                        }
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {

        mManager.unregisterListener(mListener);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
        super.onDestroy();
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

    private StorageEventListener mListener = new StorageEventListener(){
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Message msg = mHandler.obtainMessage();
            msg.what = 1002;
            msg.obj = newState;
            mHandler.sendMessage(msg);
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
    /*private String getStrForBytes(float size){
        String str = "";
        if(size > 1024){
            str = size / 1024 + "G ";
        }else{
            str = size +"M ";
        }
        return str;
    }*/
    public boolean getSecondaryStorageState() {
        try {
            StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
            List<DiskInfo> diskInfos = sm.getDisks();
            List<VolumeInfo> volumeInfos = sm.getVolumes();
            for(DiskInfo s:diskInfos){
                if(s.isSd()){
                    return true;
                }
            }
        }catch (Exception e){
            LogUtil.d("paths:   " +e.toString());
            e.printStackTrace();
        }
        return false;
    }

}
