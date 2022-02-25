package com.meigsmart.meigrs32.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.meigsmart.meigrs32.log.LogUtil;

import java.util.Iterator;

/**
 * 定位服务
 * Created by chenMeng on 2017/7/13.
 */
public class GpsService extends Service{
    private static final String TAG = "GpsService";
    private LocationManager lm;
    private Messenger messenger = new Messenger(new MessengerHandler());
    private Location mLocation;
    //自定义简易计时器
    private TimeCount timeCount;

    public static final int GPS_FROM_CLIENT = 1212;
    public static final int GPS_FROM_SERVER = 1213;

    private int currNumber = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
        stopForeground(true);
   }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onCreate() {
        lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d("onStartCommand   GPS");
        //为获取地理位置信息时设置查询条件
        String bestProvider = lm.getBestProvider(getCriteria(), true);
        //获取位置信息
        //如果不设置查询要求，getLastKnownLocation方法传人的参数为LocationManager.GPS_PROVIDER
        if (bestProvider!=null){
//            Location location = lm.getLastKnownLocation(bestProvider);
//            updateView(location);
        }
        //监听状态
        lm.addGpsStatusListener(listener);
//        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
        startTimeCount();
        return START_STICKY;
    }

    /**
     * 关闭定位服务
     */
    private void close() {
        timeCount.cancel();
        if (lm != null) {
            lm.removeUpdates(gpsListener);
            lm.removeGpsStatusListener(listener);
            lm = null;
        }

        if (gpsListener != null) {
            gpsListener = null;
        }
    }

    /**
     * 自定义handler
     */
    private class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GPS_FROM_CLIENT:
                    try {
                        Messenger messenger = msg.replyTo;
                        Message replyMsg = Message.obtain(null, GPS_FROM_SERVER);
                        Bundle bundle = new Bundle();
                        bundle.putInt("count",currNumber);
                        if (mLocation==null){
                            bundle.putDouble("lat", 0.0);
                            bundle.putDouble("lng", 0.0);

                        }else{
                            bundle.putDouble("lat", mLocation.getLatitude());
                            bundle.putDouble("lng", mLocation.getLongitude());
                        }

                        replyMsg.setData(bundle);
                        messenger.send(replyMsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void startTimeCount() {
        timeCount = new TimeCount(1000, 1000);
        timeCount.start();
    }

    private class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            timeCount.cancel();
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
            startTimeCount();
        }
    }


    /**
     * 实时更新文本内容
     *
     * @param location
     */
    private void updateView(Location location){
        if(location!=null){
            mLocation = location;
        }else{
            mLocation = null;
        }
    }

    private LocationListener gpsListener = new LocationListener() {

        /**
         * 位置信息变化时触发
         */
        public void onLocationChanged(Location location) {

            updateView(location);
            LogUtil.i("result", "时间："+location.getTime());
            LogUtil.i("result", "经度："+location.getLongitude());
            LogUtil.i("result", "纬度："+location.getLatitude());
            LogUtil.i("result", "海拔："+location.getAltitude());
        }

        /**
         * GPS状态变化时触发
         */
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                //GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    LogUtil.i("result", "当前GPS状态为可见状态");
                    break;
                //GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    LogUtil.i("result", "当前GPS状态为服务区外状态");
                    break;
                //GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    LogUtil.i("result", "当前GPS状态为暂停服务状态");
                    break;
            }
        }

        /**
         * GPS开启时触发
         */
        public void onProviderEnabled(String provider) {
            Location location=lm.getLastKnownLocation(provider);
            updateView(location);
        }

        /**
         * GPS禁用时触发
         */
        public void onProviderDisabled(String provider) {
            updateView(null);
        }

    };

    //状态监听
    GpsStatus.Listener listener = new GpsStatus.Listener() {

        public void onGpsStatusChanged(int event) {

            switch (event) {
                //第一次定位
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    LogUtil.i(TAG, "第一次定位");
                    break;
                //卫星状态改变
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
//                    LogUtil.i(TAG, "卫星状态改变");
                    //获取当前状态
                    GpsStatus gpsStatus=lm.getGpsStatus(null);
                    //获取卫星颗数的默认最大值
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    //创建一个迭代器保存所有卫星
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                    int count = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite s = iters.next();
                        count++;
                    }
                    System.out.println("搜索到："+count+"颗卫星");
                    currNumber = count;
                    break;
                //定位启动
                case GpsStatus.GPS_EVENT_STARTED:
                    LogUtil.i(TAG, "定位启动");
                    break;
                //定位结束
                case GpsStatus.GPS_EVENT_STOPPED:
                    LogUtil.i(TAG, "定位结束");
                    break;
            }
        }
    };

    /**
     * 返回查询条件
     * @return
     */
    private Criteria getCriteria(){
        Criteria criteria=new Criteria();
        //设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        //设置是否要求速度
        criteria.setSpeedRequired(false);
        // 设置是否允许运营商收费
        criteria.setCostAllowed(false);
        //设置是否需要方位信息
        criteria.setBearingRequired(false);
        //设置是否需要海拔信息
        criteria.setAltitudeRequired(false);
        // 设置对电源的需求
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }

}
