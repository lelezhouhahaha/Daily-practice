package com.meigsmart.meigrs32.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.util.DiagCommand;

public class MyDiagAutoTestClient{
    private final String TAG = MyDiagAutoTestClient.class.getSimpleName();
    public final static int SERVICEID = 0x0001; //server
    public final static int ACK_SERVICEID = 0X0002; //ack_server
    public final static int ACTIVITYID = 0X0003; //client
    public final static int ACK_ACTIVITYID = 0X0004; //ack_client
    public final static int SAY_HELLO = 0x0005; //server only for handshark
    public final static int ACK_SAY_HELLO = 0X0006; //client only for handshark


    //服务端传来的Messenger
    private Messenger mServerMessenger;
    private Handler mClientHandler;
    private Messenger mClientMessenger;

    public MyDiagAutoTestClient(Context mContext, Handler mHandler){
        //mClientHandler = new MyHandler(this);
        mClientHandler = mHandler;
        mClientMessenger = new Messenger(mClientHandler);
        bindService(mContext);
    }

    public void Destroy(Context mContext){
        mContext.unbindService(serviceConnection);
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServerMessenger = new Messenger(service);
            Log.d(TAG, "zll onServiceConnected");
            doSayHelloToService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServerMessenger = null;
            Log.e(TAG, "连接Service失败");
        }
    };

    private void bindService(Context mContext) {
        Intent intent = new Intent("com.intent.action.meig.autotest"); // 参数为服务端的Service的action的name参数的值
        //intent.setPackage("com.meigsmart.meigrs32.service"); // 参数为服务端的包名
        //intent.setPackage("com.cs.myaidltestservice");
        intent.setPackage("com.meigsmart.meigrs32");
        Log.d(TAG, "zll bindServie");
        mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);// 绑定Service
    }

    public void doSendResultMessage(int msgId, int mDiagId, int mResult, String mData, int mDataSize){
        Message message = Message.obtain();
        message.arg1 = msgId;
        message.replyTo = mClientMessenger;
        Bundle bundle = new Bundle();
        bundle.putInt(DiagCommand.FTM_SUBCMD_CMD_KEY, mDiagId);
        bundle.putInt(DiagCommand.FTM_SUBCMD_RESULT_KEY, mResult);
        bundle.putString(DiagCommand.FTM_SUBCMD_DATA_KEY, mData);
        bundle.putInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY, mDataSize);
        message.setData(bundle);
        try {
            //注意，这里把数据从服务器发出了
            //msg.replyTo.send(message);
            mServerMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void doSendMessage(int msgId, int mDiagId, String mData, int mDataSize){
        Message message = Message.obtain();
        message.arg1 = msgId;
        message.replyTo = mClientMessenger;
        Bundle bundle = new Bundle();
        bundle.putInt(DiagCommand.FTM_SUBCMD_CMD_KEY, mDiagId);
        bundle.putString(DiagCommand.FTM_SUBCMD_DATA_KEY, mData);
        bundle.putInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY, mDataSize);
        message.setData(bundle);
        try {
            //注意，这里把数据从服务器发出了
            //msg.replyTo.send(message);
            mServerMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void doSendLocalMessage(int msgId, int mDiagId, String mData, int mDataSize){
        Message msg = mClientHandler.obtainMessage();
        msg.arg1 = msgId;
        msg.replyTo = mServerMessenger;
        Bundle bundle = new Bundle();
        bundle.putInt(DiagCommand.FTM_SUBCMD_CMD_KEY, mDiagId);
        bundle.putString(DiagCommand.FTM_SUBCMD_DATA_KEY, mData);
        bundle.putInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY, mDataSize);
        msg.setData(bundle);
        mClientHandler.sendMessage(msg);
    }

    public void doSayHelloToService(){
        Message message = Message.obtain();
        message.arg1 = SAY_HELLO;
        //注意这里，把`Activity`的`Messenger`赋值给了`message`中，当然可能你已经发现这个就是`Service`中我们调用的`msg.replyTo`了。
        message.replyTo = mClientMessenger;

        Bundle bundle = new Bundle();
        bundle.putString("content", "say Hello!\n我就是Activity传过来的字符串");
        message.setData(bundle);
        try {
            //消息从客户端发出
            mServerMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}