package com.cs.server.client;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cs.server.R;
import com.cs.server.socket.TCPServerService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketActivity extends AppCompatActivity {

    private TextView mTvChatContent;
    private EditText mEtSendContent;
    private Button msend;
    private Intent mIntent;

    private static final int CONNECT_SERVER_SUCCESS = 0; //与服务端连接成功
    private static final int MESSAGE_RECEIVE_SUCCESS = 1; //接受到服务端的消息
    private static final int MESSAGE_SEND_SUCCESS=2; //消息发送
    @SuppressLint("all")
    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT_SERVER_SUCCESS:
                    //与服务端连接成功
                    mTvChatContent.setText("与聊天室连接成功\n");
                    break;
                case MESSAGE_RECEIVE_SUCCESS:
                    String msgContent = mTvChatContent.getText().toString();
                    mTvChatContent.setText(msgContent+msg.obj.toString()+"\n");
                    break;
                case MESSAGE_SEND_SUCCESS:
                    Log.d(TAG, "MESSAGE_SEND_SUCCESS");
                    mEtSendContent.setText("");
                    mTvChatContent.setText(mTvChatContent.getText().toString()+msg.obj.toString()+"\n");
                    break;
            }
            return false;
        }
    });
    private PrintWriter mPrintWriter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket);
        mTvChatContent = findViewById(R.id.tv_chat_content);
        mEtSendContent = findViewById(R.id.et_send_content);
        msend = (Button) findViewById(R.id.but_send);

        //启动服务
        mIntent = new Intent(this, TCPServerService.class);
        startService(mIntent);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //连接服务端，实现通信交互
                //IO操作必须放在子线程执行
                connectTCPServer();
            }
        }).start();

    }

    private  Socket mSocket=null;
    private void connectTCPServer() {
        //通过循环来判断Socket是否有被创建，若没有则会每隔1s尝试创建，目的是保证客户端与服务端能够连接
        while (mSocket == null) {
            try {
                //创建Socket对象，指定IP地址和端口号
                mSocket = new Socket("localhost", 8954);
                mPrintWriter = new PrintWriter(new OutputStreamWriter(mSocket.getOutputStream()),true);
                if (mSocket.isConnected()) //判断是否连接成功
                    mHandler.sendEmptyMessage(CONNECT_SERVER_SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
                //设计休眠机制，每次重试的间隔时间为1s
                SystemClock.sleep(1000);
            }

        }

        //通过循环来，不断的接受服务端发来的消息
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            while (!SocketActivity.this.isFinishing()){ //当Activity销毁后将不接受
                String msg = reader.readLine();
                if (!TextUtils.isEmpty(msg)){
                    //发消息通知更新UI
                    mHandler.obtainMessage(MESSAGE_RECEIVE_SUCCESS,msg).sendToTarget();
                }

            }
            //关闭流
            mPrintWriter.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }


   /* @SuppressLint("SetTextI18n")
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.but_send:
                //必须开启子线程，不能在UI线程操作网络
                Log.d(TAG, "but_send");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String msg = mEtSendContent.getText().toString();
                        Log.d(TAG, "msg:" + msg);
                        if (mPrintWriter!=null && !TextUtils.isEmpty(msg)){
                            mPrintWriter.println(msg+"\n");
                            //此处可以不用刷新流的方法，因为在创建mPrintWriter对象时，在其构造方法中设置了自动刷新缓存
//                            mPrintWriter.flush();
                            //通知更新UI
                            mHandler.obtainMessage(MESSAGE_SEND_SUCCESS,msg).sendToTarget();
                        }
                    }
                }).start();
                break;
        }
    }*/
   public void onSend(View view) {
       Log.d(TAG, "onSend");
       new Thread(new Runnable() {
           @Override
           public void run() {
               String msg = mEtSendContent.getText().toString();
               Log.d(TAG, "msg:" + msg);
               if (mPrintWriter!=null && !TextUtils.isEmpty(msg)){
                   mPrintWriter.println(msg+"\n");
                   //此处可以不用刷新流的方法，因为在创建mPrintWriter对象时，在其构造方法中设置了自动刷新缓存
//                            mPrintWriter.flush();
                   //通知更新UI
                   mHandler.obtainMessage(MESSAGE_SEND_SUCCESS,msg).sendToTarget();
               }
           }
       }).start();
   }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭输入流和连接
        if (mSocket!=null){
            try {
                mSocket.shutdownInput();
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //停止后台服务
        stopService(mIntent);
    }

    private static final String TAG = "zll TCPClientService";
}