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

public class TouchCaliActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "sw-TouchCaliActivity";
    private TextView mBaseTv, mUseInfoTv;
    private Button mTestBt;
    private boolean isStartLoadData = true;
    private String sensitivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_cali);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initView();
    }

    private void initView() {
        mBaseTv = (TextView) findViewById(R.id.activity_touch_cali_base_tv);
        mUseInfoTv = (TextView) findViewById(R.id.activity_touch_cali_use_info);
        mBaseTv.setClickable(false);
        mUseInfoTv.setClickable(false);
        mTestBt = (Button) findViewById(R.id.activity_touch_cali_start_bt);
        mTestBt.setOnClickListener(this);
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
                    if (msg.arg1 == MsgType.FP_MSG_TEST_CMD_TOUCH_BASE) {
                        showData(msg.arg2);
                        isStartLoadData = true;
                        mTestBt.setClickable(true);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        sensitivity = getSensitivity();
        mUseInfoTv.setText(String.format(getResources().getString(R.string.text_touch_cali_use_info), sensitivity, sensitivity));

        if(isStartLoadData){
            //mBaseTv.setText(getResources().getString(R.string.msg_loading_data));
            startTest();
            isStartLoadData = false;
            mTestBt.setClickable(false);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.activity_touch_cali_start_bt:
                if(isStartLoadData){
                    //mBaseTv.setText(getResources().getString(R.string.msg_loading_data));
                    startTest();
                    isStartLoadData = false;
                    mTestBt.setClickable(false);
                }
                break;
            default:
                break;
        }
    }

    private void startTest(){
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        manager.sendCmd(MsgType.FP_MSG_TEST_CMD_TOUCH_BASE, 0, buf, len);
    }

    private void showData(int arg){
        int raw = arg & 0xffff;
        int base = (arg >> 16) & 0xffff;
        String hexNum = int2Hex((raw - base));
        mBaseTv.setText("raw:"+raw+"   base:"+base+" \n(raw-base):"+(raw-base)+"("+hexNum+")");
    }

    private String int2Hex(int num){
        String hexNum = Integer.toHexString(num);
        if(hexNum.length()==1){
            hexNum = "0x0"+hexNum;
        }else{
            hexNum = "0x"+hexNum;
        }
        return hexNum;
    }

    /**
     * 获取灵敏度
     */
    private String getSensitivity(){
        Log.d(TAG,"getSensitivity");
        String sensitivity = "unknown";
        byte[] buf = new byte[2];
        int[] len = new int[1];
        len[0] = buf.length;
        int ret =  manager.sendCmd(MsgType.FP_MSG_TEST_CMD_SENSITIVITY, 0, buf, len);
        if(ret==0){
            Log.d(TAG,"buf[0] = "+buf[0]+" buf[1] = "+buf[1]);
            String r = Integer.toHexString(buf[0]);
            String c = Integer.toHexString(buf[1]);
            Log.d(TAG,"buf[0]Hex = "+ r+" buf[1]Hex = "+c);
            if("0".equals(r)){
                if(c.length()==1){
                    sensitivity = "0x0"+c;
                }else{
                    sensitivity = "0x"+c;
                }
            }else{
                if(r.length()==1){
                    r = "0x0"+r;
                }else{
                    r = "0x"+r;
                }

                if(c.length()==1){
                    c = "0x0"+c;
                }else{
                    c = "0x"+c;
                }
                sensitivity = "r = "+r+", c = "+c;
            }
        }else{
            sensitivity = "unknown";
        }
        return sensitivity;
    }
}
