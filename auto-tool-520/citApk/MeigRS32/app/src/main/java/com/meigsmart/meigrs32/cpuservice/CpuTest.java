package com.meigsmart.meigrs32.cpuservice;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.io.RandomAccessFile;

/**
 * Created by chenMeng on 2018/4/8.
 */
public class CpuTest {
    private Context mContext;
    private Handler mHandler;
    private Boolean mIsRuning = true;

    public CpuTest(Context paramContext, Handler mHandler) {
        this.mContext = paramContext;
        this.mHandler = mHandler;
    }

    private void calculateCpuUsage() {
        new Thread(new Runnable() {
            public void run() {
                /*float f = CpuTest.this.readUsage();
                String str = "Cpu Usage : " + f + "%";
                CpuTest.this.updateCpuInfoText(str);*/
                if (mHandler!=null)mHandler.sendEmptyMessageDelayed(1001, 1000L);
            }
        }, "CalCpu").start();
    }

    private float readUsage() {
        try {
            RandomAccessFile localObject1 = new RandomAccessFile("/proc/stat", "r");
            String[] localObject2 = localObject1.readLine().split(" ");
            long l1 = Long.parseLong(localObject2[5]);
            long l2 = Long.parseLong(localObject2[2]) + Long.parseLong(localObject2[3]) + Long.parseLong(localObject2[4]) + Long.parseLong(localObject2[6]) + Long.parseLong(localObject2[7]) + Long.parseLong(localObject2[8]);
            try {
                Thread.sleep(100L);
                localObject1.seek(0L);
                String str = localObject1.readLine();
                localObject1.close();
                String[] str1 = str.split(" ");
                long l3 = Long.parseLong(str1[5]);
                long l4 = Long.parseLong(str1[2]) + Long.parseLong(str1[3]) + Long.parseLong(str1[4]) + Long.parseLong(str1[6]) + Long.parseLong(str1[7]) + Long.parseLong(str1[8]);
                return (float) (Long.valueOf(100L * (l4 - l2) / (l4 + l3 - (l2 + l1))).longValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0.0F;
        } catch (Exception localException1) {
            localException1.printStackTrace();
        }
        return 0.0F;
    }

    private void updateCpuInfoText(String paramString) {
        Intent localIntent = new Intent(this.mContext, CpuService1.class);
        localIntent.putExtra("update", true);
        localIntent.putExtra("level", paramString);
        this.mContext.startService(localIntent);
    }

    private void useCpu() {
        new Thread(new Runnable() {
            public void run() {
                int i = 500000000;
                for (; ; ) {
                    if(mIsRuning == false){
                        break;
                    }
                    if (i == 1) {
                        if (mHandler!=null)mHandler.sendEmptyMessageDelayed(1002, 500L);
                        return;
                    }
                    i -= 1;
                }
            }
        }, "UseCpu").start();
    }

    public void start() {
        mIsRuning = true;
        this.mContext.startService(new Intent(this.mContext, CpuService1.class));
        CPUTestThread localCPUTestThread = new CPUTestThread();
        int i = 0;
        while (i < 24) {
            new Thread(localCPUTestThread).start();
            i += 1;
        }
        calculateCpuUsage();
        useCpu();
    }

    public void stop() {
        Intent localIntent = new Intent(this.mContext, CpuService1.class);
        localIntent.putExtra("finish", true);
        mContext.startService(localIntent);
        mIsRuning = false;
    }

    class CPUTestThread implements Runnable {

        public void run() {
            for (; ; ) {
                if(mIsRuning == false){
                    break;
                }
                long l = System.currentTimeMillis();
                while (System.currentTimeMillis() - l <= 10) {
                }
                l = 10;
                try {
                    Thread.sleep(l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
