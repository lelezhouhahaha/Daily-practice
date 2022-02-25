package com.cs.myaidltestservice;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.cs.myaidltestservice.MyService;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();
    public final static int SERVICEID = 0x0001; //server
    public final static int ACK_SERVICEID = 0X0002; //ack_server
    public final static int ACTIVITYID = 0X0003; //client
    public final static int ACK_ACTIVITYID = 0X0004; //ack_client
    public final static int SAY_HELLO = 0x0005; //server only for handshark
    public final static int ACK_SAY_HELLO = 0X0006; //client only for handshark

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClientHandler = new MyHandler(this);
        mClientMessenger = new Messenger(mClientHandler);
        setContentView(R.layout.activity_main);
        bindService();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(serviceConnection);
    }

    //服务端传来的Messenger
    private Messenger mServerMessenger;

    private MyHandler mClientHandler;
    private Messenger mClientMessenger;

    private static class MyHandler extends Handler {
        WeakReference<Activity> reference;
        public MyHandler(Activity activity) {
            reference = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = (MainActivity) reference.get();
            switch (msg.arg1) {
                case ACTIVITYID:
                    Log.d(activity.TAG, "ACTIVITYID 服务端传来了消息=====>>>>>>>");
                    String strActivity = (String) msg.getData().get("content");
                    Log.d(activity.TAG, "zll strActivity:" + strActivity);
                    activity.doSendLocalMessage(ACK_ACTIVITYID, "SOFTWAREINFO:PASS");
                    break;
                case ACK_ACTIVITYID:
                    String mDataSendToService = (String) msg.getData().get("content");
                    activity.doSendMessage(msg, ACK_SERVICEID, mDataSendToService);
                    break;
                case ACK_SAY_HELLO:
                    //客户端接受服务端传来的消息
                    Log.d(activity.TAG, "ACK_SAY_HELLO 服务端传来了消息=====>>>>>>>");
                    String str = (String) msg.getData().get("content");
                    Log.d(activity.TAG, str);
                    break;
            }
        }
    }

    public void doSendMessage(Message msg, int cmdId, String cmdData){
        Message message = Message.obtain();
        message.arg1 = cmdId;
        message.replyTo = mClientMessenger;
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
        Message msg = mClientHandler.obtainMessage();
        msg.arg1 = cmdId;
        msg.replyTo = mServerMessenger;
        Bundle bundle = new Bundle();
        bundle.putString("content", cmdData);
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

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServerMessenger = new Messenger(service);
            doSayHelloToService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "连接Service失败");
        }
    };

    private void bindService() {
        Intent intent = new Intent("com.intent.action.meig.autotest"); // 参数为服务端的Service的action的name参数的值
        intent.setPackage("com.cs.myaidltestservice"); // 参数为服务端的包名
        Log.d(TAG, "zll bindServie");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);// 绑定Service
    }
}