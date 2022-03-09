package com.sunmi.scannercitmmi1.utils;

import android.util.Log;

import java.util.concurrent.TimeoutException;

public class NetUtils {
    private static final String TAG = "NetUtils";
    public static final String SUNMI_SERVER_IP = "www.sunmi.com"; //use sunmi server address convert to ip address.

    public static boolean isNetworkOnline(String ip) {
        boolean isConnected = false;
        try {
            if (ip != null) {
                Process p = Runtime.getRuntime().exec("ping -c 3 -w 10 " + ip);//ping3次
                Log.d(TAG,"Process ping start");
                ProcessWorker mProcessWorker = new ProcessWorker(p);
                mProcessWorker.start();
                try{
                    mProcessWorker.join(10000L);
                if (mProcessWorker.exit != null) {
                    Log.d(TAG, "Process ping end ");
                    isConnected = mProcessWorker.exit == 0;
                } else {
                    Log.d(TAG, "Process ping end timeout ");
                    isConnected = false;
                    mProcessWorker.interrupt();
                    Thread.currentThread().interrupt();

                }
               } catch (InterruptedException ex) {
                    mProcessWorker.interrupt();
                    Thread.currentThread().interrupt();
                    throw ex;
                } finally {
                    Log.d(TAG, "Process destroy end ");
                    p.destroy();
                }
            }

           /* int status = p.waitFor();
                if (status == 0) {
                    isConnected = true;
                } else {
                    isConnected = false;
                }
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "judgeTheConnect: e = " + e.toString());
            isConnected = false;
        }
        return isConnected;
    }

    private static class ProcessWorker extends Thread {
            private final Process process;
            private Integer exit;

            private ProcessWorker(Process process) {
                this.process = process;
            }

            public void run() {
                try {
                    exit = process.waitFor();
                } catch (InterruptedException ignore) {
                    return;
                }
            }
        }

}
