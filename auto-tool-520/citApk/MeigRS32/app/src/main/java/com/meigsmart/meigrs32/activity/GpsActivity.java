package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import java.util.Iterator;

import butterknife.BindView;

public class GpsActivity extends BaseActivity implements View.OnClickListener{
    private GpsActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public RelativeLayout mLayout;

    @BindView(R.id.status)
    public TextView mStatus;
    @BindView(R.id.satellite_count)
    public TextView mSatelliteCount;
    @BindView(R.id.satellite_info)
    public TextView mSatelliteInfo;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private Runnable mRun;
//    private int mConfigResult;
    private LocationManager mLocationManager = null;

    private static final String PROVIDER = LocationManager.GPS_PROVIDER;
    private final String TAG = GpsActivity.class.getSimpleName();

    /** satellite min count for OK */
    private static  int SATELLITE_COUNT_MIN = 3;
    private static int AVAILABLE_SNR_VALUE = 35;
    private int mConfigTime = 0;
    private String mGpsValue = "common_gps__set_value_int";
    private String mFatherName = "";
    private String projectName = "";
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    private boolean isMT535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");

    @Override
    protected int getLayoutId() {
        return R.layout.activity_gps;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.pcba_gps);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        if("MT537".equals(projectName)){
            if(mFatherName.equals(MyApplication.MMI1_PreName)||mFatherName.equals(MyApplication.MMI1_PreSignalName)
            ||mFatherName.equals(MyApplication.MMI2_PreName)||mFatherName.equals(MyApplication.MMI2_PreSignalName)){
                AVAILABLE_SNR_VALUE = 38;
            }
        }
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
            mSuccess.setOnClickListener(this);
            mFail.setOnClickListener(this);
        }

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // 设置监听器，设置自动更新间隔这里设置1000ms，移动距离：0米。
        mLocationManager.requestLocationUpdates(PROVIDER, 1000, 0, mLocationListener);
        // 设置状态监听回调函数。statusListener是监听的回调函数。
        mLocationManager.addGpsStatusListener(mGpsListener);


        mHandler.sendEmptyMessageDelayed(1001,getResources().getInteger(R.integer.start_delay_time));
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if ((mConfigTime == 0) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                     mHandler.sendEmptyMessage(1111);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    private GpsStatus.Listener mGpsListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                showSatelliteCount();
            }
        }
    };

    /**
     * Get and show satellite count
     */
    @SuppressLint({"MissingPermission"})
    private void showSatelliteCount() {

        int count = 0;

        StringBuilder info = new StringBuilder();

        if (mLocationManager != null) {
            @SuppressLint("MissingPermission") GpsStatus status = mLocationManager.getGpsStatus(null);
            int max = status.getMaxSatellites();
            Iterator<GpsSatellite> iterator = status.getSatellites().iterator();
            String strTmp = "";
            strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mGpsValue);
            if ((strTmp != null) && !strTmp.isEmpty()){
                max = Integer.parseInt( strTmp ) ;
            }

            // get satellite count
            while (iterator.hasNext() && count < max) {

                GpsSatellite gpsSatellite = iterator.next();
                float snr = gpsSatellite.getSnr();
                Log.d(TAG,"AVAILABLE_SNR_VALUE:"+AVAILABLE_SNR_VALUE);
                if(snr <= AVAILABLE_SNR_VALUE){
                    continue;
                }

                count++;
                info.append("id: ");
                info.append(String.valueOf(gpsSatellite.getPrn()));
                info.append("\nsnr: ");
                info.append(String.valueOf(snr));
                info.append("\n\n");
            }
        }

        mSatelliteCount.setText(String.format(getString(R.string.gps_count_num), count));
        mSatelliteInfo.setText(info);
        String strTmp = "";
        strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mGpsValue);

        if ((strTmp != null) && !strTmp.isEmpty()){
            SATELLITE_COUNT_MIN =Integer.parseInt( strTmp ) ;

        } else{
            SATELLITE_COUNT_MIN = 3;
        }

        // satellite count is ok
        if (count >= SATELLITE_COUNT_MIN) {
            if(!mFatherName.equals(MyApplication.RuninTestNAME)) {
                mSuccess.setVisibility(View.VISIBLE);
                mFail.setVisibility(View.GONE);
            }
            mStatus.setText(R.string.success);
            mLocationManager.removeUpdates(mLocationListener);
            if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)||
                    (isMT535 && mFatherName.equals(MyApplication.MMI1_PreName))||
                    (isMT535 && mFatherName.equals(MyApplication.MMI2_PreName))){
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                deInit(mFatherName, SUCCESS);
            }
        }
    }


    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    isStartTest = true;
                    mFlag.setVisibility(View.GONE);
                    mLayout.setVisibility(View.VISIBLE);
                    showSatelliteCount();
                    break;
                case 1111:
                    if(mFatherName.equals(MyApplication.RuninTestNAME)) {
                        deInit(mFatherName, SUCCESS);
                    }else{
                        deInit(mFatherName, FAILURE);
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Settings.Secure.setLocationProviderEnabled(getContentResolver(), PROVIDER, true);

        if(!isGpsEnabled()){
            ToastUtil.showCenterLong(getString(R.string.gps_not_enabled_msg));
        }

    }

    /**
     * Check gps state
     *
     * @return true if enabled
     */
    private boolean isGpsEnabled() {
        if (mLocationManager == null) {
            return false;
        }
        return mLocationManager.isProviderEnabled(PROVIDER);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mLocationManager != null){
            mLocationManager.removeGpsStatusListener(mGpsListener);
        }
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1111);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.success:
                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                deInit(mFatherName, SUCCESS);
                break;
            case R.id.fail:
                mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                break;
        }

    }
}