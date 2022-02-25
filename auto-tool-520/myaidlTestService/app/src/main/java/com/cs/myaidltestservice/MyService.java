package com.cs.myaidltestservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;


public class MyService extends Service {


    public final static String TAG = "MyService";
    Messenger mClientMessage = null;
    SerialPort mSerialPort = new SerialPort();

    public final static int SERVICEID = 0x0001; //server
    public final static int ACK_SERVICEID = 0X0002; //ack_server
    public final static int ACTIVITYID = 0X0003; //client
    public final static int ACK_ACTIVITYID = 0X0004; //ack_client
    public final static int SAY_HELLO = 0x0005; //server only for handshark
    public final static int ACK_SAY_HELLO = 0X0006; //client only for handshark

    private MyHandler mServerHandler = new MyHandler();
    private final Messenger mServerMessenger = new Messenger(mServerHandler);
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1) {
                case SERVICEID:
                    Log.d(TAG, "zll SERVICEID start");
                    String mCmmdContent = (String) msg.getData().get("content");
                    Log.d(TAG, "zll SERVICEID mCmmdContent:[" + mCmmdContent +"].");
                    doSendMessage(msg, ACTIVITYID, mCmmdContent);
                    break;
                case ACK_SERVICEID:
                    String mCmmdResultContent = (String) msg.getData().get("content");
                    Log.d(TAG, "zll ACK_SERVICEID mCmmdResultContent:" + mCmmdResultContent);
                    break;
                case SAY_HELLO:
                    Log.d(TAG, "客服端传来的消息===>>>>>>");
                    String str = (String) msg.getData().get("content");
                    Log.d(TAG, str);
                    if (str.contains("say Hello!")) {
                        mClientMessage = (Messenger) msg.replyTo;
                    }
                    doSendMessage(msg, ACK_SAY_HELLO, "ACK");
                    break;
            }
        }
    }

    private void doSendMessage(Message msg, int cmdId, String cmdData){
        Message message = Message.obtain();
        message.arg1 = cmdId;
        message.replyTo = mServerMessenger;
        Bundle bundle = new Bundle();
        bundle.putString("content", cmdData);
        message.setData(bundle);
        try {
            //注意，这里把数据从服务器发出了
            msg.replyTo.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void doSendLocalMessage(int cmdId, String cmdData){
        Message msg = mServerHandler.obtainMessage();
        //msg.what = SERVICEID;
        msg.arg1 = cmdId;
        msg.replyTo = mClientMessage;
        Bundle bundle = new Bundle();
        bundle.putString("content", cmdData);
        msg.setData(bundle);
        //msg.obj = "AT+SOFTWAREINFO";
        mServerHandler.sendMessage(msg);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServerMessenger.getBinder();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mSerialPort.setLoopFlag(false);
    }

    public class SerialPort {
        private Boolean mLoopFlag =  true;
        SerialPort(){
            Log.d(TAG, "SerialPort send msg");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mLoopFlag) {
                        try {
                            Thread.sleep(3000);
                            Log.d(TAG, "send msg");
                            doSendLocalMessage(SERVICEID, "AT+SOFTWAREINFO");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        public void setLoopFlag(Boolean enable){
            mLoopFlag = enable;
        }
    }
}