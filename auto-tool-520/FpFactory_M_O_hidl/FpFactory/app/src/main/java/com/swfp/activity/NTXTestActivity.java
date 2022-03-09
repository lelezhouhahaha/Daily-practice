package com.swfp.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sunwave.utils.MsgType;
import com.swfp.device.MessageCallBack;
import com.swfp.factory.R;
import com.swfp.utils.MessageType;

public class NTXTestActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "sw-NTXTestActivity";
    private TextView mTvTips, mTvData;
    private Button mBtLoadData;

    private boolean isStartLoadData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ntx_test);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initView();
    }

    private void initView() {
        mTvTips = (TextView) findViewById(R.id.tips_tv);
        mTvTips.setText(getResources().getString(R.string.text_tips_untouch));
        mTvData = (TextView) findViewById(R.id.tv_data);
        mBtLoadData = (Button) findViewById(R.id.load_data_bt);
        mBtLoadData.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected MessageCallBack getMessageCallBack() {
        return new MessageCallBack() {
            @Override
            public void handMessage(int what, int arg1, int arg2) {
                Log.d(TAG, "main msg what = " + what + "(0x" + Integer.toHexString(what) + ")" + " arg1 = "
                        + arg1 + "(0x" + Integer.toHexString(arg1) + ")" + " arg2 = " + arg2);
                mHandler.obtainMessage(what, arg1, arg2).sendToTarget();
            }
        };
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageType.FP_MSG_TEST:
                    if (msg.arg1 == MsgType.FP_MSG_TEST_CMD_START_TEST_DATA) {
                        byte[] buf = new byte[10000];
                        int[] len = new int[1];
                        len[0] = buf.length;
                        manager.sendCmd(MsgType.FP_MSG_TEST_CMD_GET_TEST_DATA, 0, buf, len);
                        showData(buf,len[0]);
                        isStartLoadData = true;
                        mBtLoadData.setClickable(true);
                        mTvTips.setText(getResources().getString(R.string.text_tips_untouch));
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void startTest(){
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        manager.sendCmd(MsgType.FP_MSG_TEST_CMD_START_TEST_DATA, 0, buf, len);
    }

    private void showData(byte[] buf, int len){
        mTvData.setText(new String(buf));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.load_data_bt:
                if(isStartLoadData){
                    mTvTips.setText(getResources().getString(R.string.msg_loading_data));
                    startTest();
                    isStartLoadData = false;
                    mBtLoadData.setClickable(false);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
