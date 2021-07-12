package com.cs.server.socket;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServerService extends IntentService {

    private static final String[] defaultMessages = {
            "你好啊，嘻嘻",
            "看了你相片，你好帅哦，很喜欢你这样的",
            "我是江西人，你呢？",
            "你在哪里工作？"};

    private int index = 0;

    private boolean isServiceDestroy = false;
    //需注意，必须传入参数
    public TCPServerService() {
        super("TCP");
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            //1、监听本地端口号
            ServerSocket serverSocket = new ServerSocket(8954);
            Socket socket = serverSocket.accept();


            //2、获取输入流，接受用户发来的消息（Activity）
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            //3、获取输出流，向客户端（Activity）回复消息
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));

            //4、通过循环不断读取客户端发来的消息 ,并发送
            while (!isServiceDestroy) {
                String readLine = reader.readLine();

                if (!TextUtils.isEmpty(readLine)) {
                    String sendMag = index < defaultMessages.length ? defaultMessages[index] : "已离线";
                    SystemClock.sleep(500); //延迟发送
                    writer.println(sendMag+"\r"); // `\r或\n`必须要有，否则会影响客户端接受消息
                    writer.flush();  //刷新流
                    index++;
                }
            }


            //关闭流
            inputStream.close();
            reader.close();
            outputStream.close();
            writer.close();
            socket.close();
            //需关闭，否则再次连接时，会报端口号已被使用
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceDestroy = true;
        Log.d(TAG, "onDestroy: ");
    }

    private static final String TAG = "zll TCPServerService";
}