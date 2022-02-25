package com.meigsmart.meigrs32.cpuservice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.meigsmart.meigrs32.application.MyApplication;


/**
 * Created by chenMeng on 2018/2/1.
 */

public class CpuService1 extends Service {
    private CpuInfo mCpuInfo;
    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;

    @Override
    public void onCreate() {
        HandlerThread localHandlerThread = new HandlerThread("CpuService1");
        localHandlerThread.start();
        this.mServiceLooper = localHandlerThread.getLooper();
        this.mServiceHandler = new ServiceHandler(this.mServiceLooper);
        this.mCpuInfo = new CpuInfo(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message localMessage = this.mServiceHandler.obtainMessage();
        localMessage.obj = intent;
        this.mServiceHandler.sendMessage(localMessage);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mServiceLooper.quit();
        MyApplication.getInstance().getSingleThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        });
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper paramLooper) {
            super();
        }

        public void handleMessage(Message paramMessage) {
            Intent intent = (Intent) paramMessage.obj;
            if (intent.getBooleanExtra("finish", false)) {
                CpuService1.this.stopSelf();
                CpuService1.this.mCpuInfo.hide();
                return;
            }
            if (intent.getBooleanExtra("update", false)) {
                String str = intent.getStringExtra("level");
                CpuService1.this.mCpuInfo.setText(str);
                return;
            }
            CpuService1.this.mCpuInfo.show();
        }
    }

}
