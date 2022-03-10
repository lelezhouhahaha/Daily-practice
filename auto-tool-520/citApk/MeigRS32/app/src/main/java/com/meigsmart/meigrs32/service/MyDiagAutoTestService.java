package com.meigsmart.meigrs32.service;

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

import com.meigsmart.meigrs32.activity.PCBAAutoActivity;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.DiagCommand;
import com.meigsmart.meigrs32.util.DiagJniInterface;


public class MyDiagAutoTestService extends Service {


    public final static String TAG = "MyDiagAutoTestService";
    Messenger mClientMessage = null;
    private DiagJniInterface mDiag = null;
    public final static int HANDLER_DIAG_COMMAND = 10000;
    public final static int HANDLER_DIAG_COMMAND_SET_RESULT = 10010;
    public boolean SAVE_EN_LOG = false;

    private MyHandler mServerHandler = new MyHandler();
    private final Messenger mServerMessenger = new Messenger(mServerHandler);
    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1) {
                case HANDLER_DIAG_COMMAND:
                    Log.d(TAG, "diag command handler");
                {

                    int mDiagCmmdId = msg.getData().getInt(DiagCommand.FTM_SUBCMD_CMD_KEY);
                    if (mDiagCmmdId < DiagCommand.FTM_SUBCMD_SET_RESULT_BASE) {
                        int id = mDiagCmmdId - DiagCommand.FTM_SUBCMD_BASE;
                        switch (id) {
                            case DiagCommand.FTM_SUBCMD_START:
                                Intent intent = new Intent(getBaseContext(), PCBAAutoActivity.class);
                                String title = DataUtil.getStringFromName(getBaseContext(), PCBAAutoActivity.class.getSimpleName());

                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("fatherName", "");
                                intent.putExtra("name", SAVE_EN_LOG ? PCBAAutoActivity.class.getSimpleName() : title);
                                LogUtil.d(TAG, "start PCBAAutoActivity");
                                getApplication().startActivity(intent);
                                mDiag.SendDiagResult(mDiagCmmdId, 2, null, 0);
                                break;
                            case DiagCommand.FTM_SUBCMD_END:
                                Intent intent_finish = new Intent(getBaseContext(), PCBAAutoActivity.class);
                                String title_finish = DataUtil.getStringFromName(getBaseContext(), PCBAAutoActivity.class.getSimpleName());

                                intent_finish.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent_finish.putExtra("fatherName", "");
                                intent_finish.putExtra("name", SAVE_EN_LOG ? PCBAAutoActivity.class.getSimpleName() : title_finish);

                                LogUtil.d(TAG, "finish PCBAAutoActivity");
                                getApplication().startActivity(intent_finish);
                                mDiag.SendDiagResult(mDiagCmmdId, 2, null, 0);
                                mDiag.setmToolStartStatus(false);
                                break;
                            default:
                                //doSendMessage(msg, HANDLER_DIAG_COMMAND, mDiagCmmdId);
                                String mDiagData = (String) msg.getData().get(DiagCommand.FTM_SUBCMD_DATA_KEY);
                                int mDiagDataSize = msg.getData().getInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY);
                                doSendMessage(DiagCommand.SERVICEID, mDiagCmmdId, mDiagData, mDiagDataSize);
                                break;
                        }
                    }
                }
                    break;
                case HANDLER_DIAG_COMMAND_SET_RESULT: {
                    Log.d(TAG, "diag command handler set result");
                    int mDiagCmmdId = msg.getData().getInt(DiagCommand.FTM_SUBCMD_CMD_KEY);
                    int mResult = msg.getData().getInt(DiagCommand.FTM_SUBCMD_RESULT_KEY);
                    String mData = (String)msg.getData().get(DiagCommand.FTM_SUBCMD_DATA_KEY);
                    int mSize = msg.getData().getInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY);
                    Log.d(TAG, "zll HANDLER_DIAG_COMMAND_SET_RESULT mCmmdResultContent:" + mDiagCmmdId + " mResult:[" +  mResult + "] mData:[" + mData + "] mSize:[" + mSize + "].");
                    doSendResultMessage(DiagCommand.SERVICEID_SET_RESULT, mDiagCmmdId, mResult, mData, mSize);
                }
                    break;
                case DiagCommand.ACK_SERVICEID: {
                    int mDiagCmmdId = msg.getData().getInt(DiagCommand.FTM_SUBCMD_CMD_KEY);
                    int mResult = msg.getData().getInt(DiagCommand.FTM_SUBCMD_RESULT_KEY);
                    String mData = (String)msg.getData().get(DiagCommand.FTM_SUBCMD_DATA_KEY);
                    int mSize = msg.getData().getInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY);
                    Log.d(TAG, "zll ACK_SERVICEID mCmmdResultContent:" + mDiagCmmdId + " mResult:[" +  mResult + "] mData:[" + mData + "] mSize:[" + mSize + "].");
                    mDiag.SendDiagResult(mDiagCmmdId, mResult, mData, mSize);
                }
                    break;
                case DiagCommand.ACK_SERVICEID_SET_RESULT: {
                    int mDiagCmmdId = msg.getData().getInt(DiagCommand.FTM_SUBCMD_CMD_KEY);
                    int mResult = msg.getData().getInt(DiagCommand.FTM_SUBCMD_RESULT_KEY);
                    String mData = (String) msg.getData().get(DiagCommand.FTM_SUBCMD_DATA_KEY);
                    int mSize = msg.getData().getInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY);
                    Log.d(TAG, "zll ACK_SERVICEID_SET_RESULT mCmmdResultContent:" + mDiagCmmdId + " mResult:[" + mResult + "] mData:[" + mData + "] mSize:[" + mSize + "].");
                    mDiag.SendDiagResult(mDiagCmmdId, mResult, mData, mSize);
                }
                    break;
                case DiagCommand.SAY_HELLO:
                    Log.d(TAG, "客服端传来的消息===>>>>>>");
                    mDiag.setmToolStartStatus(true);
                    String mDiagCmdIdStr = (String)msg.getData().get("content");
                    Log.d(TAG, "mDiagCmdId:" + mDiagCmdIdStr);
                    if(mClientMessage == null) {
                        mClientMessage = (Messenger) msg.replyTo;
                    }
                    doSendAckSayHelloToClinet();
                    break;
            }
        }
    }

    private void doSendMessage(int msgId, int mDiagId, String mData, int mDataSize){
        Message message = Message.obtain();
        message.arg1 = msgId;
        message.replyTo = mServerMessenger;
        Bundle bundle = new Bundle();
        bundle.putInt(DiagCommand.FTM_SUBCMD_CMD_KEY, mDiagId);
        bundle.putString(DiagCommand.FTM_SUBCMD_DATA_KEY, mData);
        bundle.putInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY, mDataSize);
        message.setData(bundle);
        try {
            //注意，这里把数据从服务器发出了
            mClientMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void doSendResultMessage(int msgId, int mDiagId, int mResult, String mData, int mDataSize){
        Message message = Message.obtain();
        message.arg1 = msgId;
        message.replyTo = mServerMessenger;
        Bundle bundle = new Bundle();
        bundle.putInt(DiagCommand.FTM_SUBCMD_CMD_KEY, mDiagId);
        bundle.putInt(DiagCommand.FTM_SUBCMD_RESULT_KEY, mResult);
        bundle.putString(DiagCommand.FTM_SUBCMD_DATA_KEY, mData);
        bundle.putInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY, mDataSize);
        message.setData(bundle);
        try {
            //注意，这里把数据从服务器发出了
            mClientMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void doSendLocalMessage(int msgId, int mDiagId, String mData, int mDataSize){
        Message msg = mServerHandler.obtainMessage();
        //msg.what = SERVICEID;
        msg.arg1 = msgId;
        msg.replyTo = mClientMessage;
        Bundle bundle = new Bundle();
        bundle.putInt(DiagCommand.FTM_SUBCMD_CMD_KEY, mDiagId);
        bundle.putString(DiagCommand.FTM_SUBCMD_DATA_KEY, mData);
        bundle.putInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY, mDataSize);
        msg.setData(bundle);
        //msg.obj = "AT+SOFTWAREINFO";
        mServerHandler.sendMessage(msg);
    }

    public void doSendAckSayHelloToClinet(){
        Message message = Message.obtain();
        message.arg1 = DiagCommand.ACK_SAY_HELLO;
        //注意这里，把`Activity`的`Messenger`赋值给了`message`中，当然可能你已经发现这个就是`Service`中我们调用的`msg.replyTo`了。
        message.replyTo = mServerMessenger;

        Bundle bundle = new Bundle();
        bundle.putString("content", "say Hello!\n我就是Activity传过来的字符串");
        message.setData(bundle);
        try {
            //消息从客户端发出
            mClientMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onCreate(){
        super.onCreate();
        SAVE_EN_LOG = "true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, "common_result_default_language"));
        if(mDiag == null) {
            mDiag = new DiagJniInterface();
            mDiag.setHandler(mServerHandler);
            mDiag.Diag_Init();
        }
        Log.d(TAG, "onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mServerMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        //mDiag = new DiagJniInterface();
        mClientMessage = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mDiag != null) {
            mDiag.setHandler(null);
            mDiag.Diag_Deinit();
            mDiag = null;
        }
    }
}