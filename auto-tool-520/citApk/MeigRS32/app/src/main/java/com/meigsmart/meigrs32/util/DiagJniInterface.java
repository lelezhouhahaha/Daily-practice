package com.meigsmart.meigrs32.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DiagJniInterface {

    public static String TAG = "DiagJniInterfaceJava";
    private boolean status = false;
    public static  Handler mHandler = null;

    public boolean isStatus() {
        return status;
    }
    public void setStatus(boolean result) {
        status = result;
    }

    /*private void handlerDiagCmmd(int cmdid){
    Log.d(TAG, "cmdid:" + cmdid);
        if(this.mHandler == null){
            //Log.d("DiagJniInterfaceJava", "mHandler is null");
            Log.d(TAG, "mHandler is null");
            return;
        }
        Bundle bundle = new Bundle();
        String mCmmdId = String.valueOf(cmdid);
        bundle.putString("diag_command", mCmmdId);
        Message msg = Message.obtain();
        msg.setData(bundle);
        msg.what = 10000;
        msg.setTarget(this.mHandler);
        msg.sendToTarget();
    }
*/
    //notice ap to handler diag cmmd
    public void doNoticeApHandlerAutoJudged(int cmdid){
        //Log.d("DiagJniInterfaceJava", "cmdid:" + cmdid);
        Log.d(TAG, "cmdid:" + cmdid);
        if(this.mHandler == null){
            //Log.d("DiagJniInterfaceJava", "mHandler is null");
            Log.d(TAG, "mHandler is null");
            return;
        }
        Bundle bundle = new Bundle();
        String mCmmdId = String.valueOf(cmdid);
        bundle.putString("diag_command", mCmmdId);
        Message msg = Message.obtain();
        msg.setData(bundle);
        msg.what = 10000;
        msg.setTarget(this.mHandler);
        msg.sendToTarget();
    }

    public void doNoticeApHandlerSetResult(int cmdid, String data){
        Log.d(TAG, "cmdid:" + cmdid);
        if(this.mHandler == null){
            //Log.d("DiagJniInterfaceJava", "mHandler is null");
            Log.d(TAG, "mHandler is null");
            return;
        }
        Bundle bundle = new Bundle();
        String mCmmdId = String.valueOf(cmdid);
        bundle.putString("diag_command", mCmmdId);
        bundle.putString("diag_data", data);
        Message msg = Message.obtain();
        msg.setData(bundle);
        msg.what = 10000;
        msg.setTarget(this.mHandler);
        msg.sendToTarget();
    }

    public void setHandler(Handler h) {
        this.mHandler = h;
    }

    public native void Diag_Init();
    public native void Diag_Deinit();
    public native void SendDiagResult(int cmdId, String data, int dataSize);

    static {
        System.loadLibrary("diag-jni");
    }
}
