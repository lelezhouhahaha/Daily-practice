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
import com.swfp.view.ChartView;


public class SensitivityTestActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "sw-SensitivityActivity";

    private TextView mTvTips, mTvSensitivityTips, mTvBase, mTvDef;
    private ChartView mChartView;
    private Button mBtLoadData;

    private boolean isStartLoadData = true;
    private int mBreakPoint = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensitivity_test);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initView();
    }

    private void initView() {
        mTvTips = (TextView) findViewById(R.id.tips_tv);
        mTvTips.setText(getResources().getString(R.string.text_tips_untouch));
        mTvSensitivityTips = (TextView) findViewById(R.id.sensitivity_tips_tv);
        mTvBase = (TextView) findViewById(R.id.base_tv);
        mTvDef = (TextView) findViewById(R.id.def_tv);
        mChartView = (ChartView) findViewById(R.id.chart_view);
        mBtLoadData = (Button) findViewById(R.id.load_data_bt);
        mBtLoadData.setOnClickListener(this);
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
                    if (msg.arg1 == MsgType.FP_MSG_TEST_CMD_START_TEST_SENSITIVITY) {
                        byte[] buf = new byte[67];
                        int[] len = new int[1];
                        len[0] = buf.length;
                        manager.sendCmd(MsgType.FP_MSG_TEST_CMD_GET_SENSITIVITY_DATA, 0, buf, len);
                        handleData(buf);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.load_data_bt:
                if(isStartLoadData){
                    mTvTips.setText(getResources().getString(R.string.msg_loading_data));
                    startTestSensitivity();
                    isStartLoadData = false;
                    mBtLoadData.setClickable(false);
                }
                break;
            default:
                break;
        }
    }

    private void startTestSensitivity(){
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        manager.sendCmd(MsgType.FP_MSG_TEST_CMD_START_TEST_SENSITIVITY, 0, buf, len);
    }

    private void handleData(byte[] buf) {
        String baseHex = getHexString(buf[0]);
        String defHex = getHexString(buf[1]);
        int size = buf[2];
        Log.d(TAG,"base = "+buf[0]+"("+baseHex+") ,def = "+buf[1]+"("+defHex+")"+",size = "+size);

        mTvBase.setText("base : "+baseHex);
        mTvDef.setText("def : "+defHex);
        byte[] datas = new byte[size];
        for (int i = 0; i < size; i++) {
            datas[i] = buf[i+3];
            Log.d(TAG,"datas["+i+"] = "+datas[i]);
        }
        getBreakPoint(datas);
        Log.d(TAG,"breakPoint = "+mBreakPoint);

        mChartView.upDataChart(datas,mBreakPoint);
        judgeSensitivity(buf[1]);
    }

    /**
     * 将字节转成十六进制字符串
     * @param b
     * @return
     */
    private String getHexString(byte b) {
        int baseInt = b;
        baseInt = baseInt&0xff;
        String hexString = Integer.toHexString(baseInt);
        StringBuilder sb = new StringBuilder();
        if(hexString.length() == 1){
            sb.append("0x0").append(hexString);
        }else{
            sb.append("0x").append(hexString);
        }
        return sb.toString();
    }

    /**
     * 获取转折点索引
     * @param datas
     */
    private void getBreakPoint(byte[] datas){
        mBreakPoint = 0;
        for (int i = 0; i < datas.length-1; i++) {
            if(datas[i] > datas[i+1]){
                mBreakPoint = i;
                break;
            }
        }
    }
    /**
     * 判断灵敏度
     */
    private void judgeSensitivity(int def){
        if(mIcId == 0x8202){
            mTvSensitivityTips.setText(String.format(getResources().getString(R.string.text_tips_sensitivity), Integer.toHexString(mIcId), "0x05"));
        }else if(mIcId == 0x8205){
            mTvSensitivityTips.setText(String.format(getResources().getString(R.string.text_tips_sensitivity), Integer.toHexString(mIcId), "0x07"));
        }else if(mIcId == 0x8221){
            mTvSensitivityTips.setText(String.format(getResources().getString(R.string.text_tips_sensitivity), Integer.toHexString(mIcId), "0x03"));
        }else if(mIcId == 0x8231){
            mTvSensitivityTips.setText(String.format(getResources().getString(R.string.text_tips_sensitivity), Integer.toHexString(mIcId), "0x01"));
        }else if(mIcId == 0x8241){
            mTvSensitivityTips.setText(String.format(getResources().getString(R.string.text_tips_sensitivity), Integer.toHexString(mIcId), "0x00"));
        }else{
            mTvSensitivityTips.setText(String.format(getResources().getString(R.string.text_tips_sensitivity), Integer.toHexString(mIcId), "暂不支持"));
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
