package com.cs.server;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private SocketServerTest serverTest;
    private static final String TAG = "zll";
    public class SocketServerTest {

        private static final int PORT = 9999;
        private List<Socket> mList = new ArrayList<Socket>();
        private ServerSocket server = null;
        private ExecutorService mExecutorService = null;
        private String receiveMsg;
        private String sendMsg;

        public SocketServerTest() {
            try {
                server = new ServerSocket(PORT);                         //步骤一
                mExecutorService = Executors.newCachedThreadPool();
                Log.d(TAG, "服务器已启动...");
                Socket client = null;
                while (true) {
                    client = server.accept();         //步骤二，每接受到一个新Socket连接请求，就会新建一个Thread去处理与其之间的通信
                    mList.add(client);
                    mExecutorService.execute(new Service(client));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        class Service implements Runnable {
            private Socket socket;
            private BufferedReader in = null;
            private PrintWriter printWriter=null;

            public Service(Socket socket) {                         //这段代码对应步骤三
                this.socket = socket;
                try {
                    printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter( socket.getOutputStream(), "UTF-8")), true);
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream(),"UTF-8"));
                    Log.d(TAG, "成功连接服务器"+"（服务器发送）");
                    Log.d(TAG, "成功连接服务器");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void run() {
                try {
                    while (true) {                                   //循环接收、读取 Client 端发送过来的信息
                        if ((receiveMsg = in.readLine())!=null) {
                            Log.d(TAG, "receiveMsg:"+receiveMsg);
                            if (receiveMsg.equals("0")) {
                                Log.d(TAG, "客户端请求断开连接");
                                Log.d(TAG, "服务端断开连接"+"（服务器发送）");
                                mList.remove(socket);
                                in.close();
                                socket.close();                         //接受 Client 端的断开连接请求，并关闭 Socket 连接
                                break;
                            } else {
                                sendMsg = "我已接收：" + receiveMsg + "（服务器发送）";
                                Log.d(TAG, sendMsg);           //向 Client 端反馈、发送信息
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serverTest = new SocketServerTest();
    }
}