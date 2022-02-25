package com.meigsmart.meigrs32.activity;

import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.util.ByteUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2;

import butterknife.BindView;

public class CheckSPTouchActivity extends BaseActivity implements OnClickListener, PromptDialog.OnPromptDialogCallBack {

    private String TAG = "CheckSPTouchActivity";

    private CheckSPTouchActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.tv_tp_info)
    public TextView tpInfoTv;
    @BindView(R.id.bt_reset)
    public Button resetBtn;
    private String mFatherName = "";

    private int mConfigTime = 0;
    private Runnable mRun;

    private final int PWD_CONFIRM = 0x02;
    private final int PWD_ERROR = 0x03;
    private final int PWD_FAILED = 0x04;
    private final int PWD_CANCEL = 0x05;

    private static final int PIN_CLICK_CANCEL = 2222;
    private static final int PIN_CLICK_NUMBER = 2;
    private static final int PIN_CLICK_CONFIRM = 3333;
    private static final int PIN_ERROR = 0x03;
    public String pinCipher = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_check_sp_touch;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        resetBtn.setOnClickListener(this);
        mTitle.setText(R.string.CheckSPTouchActivity);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        testTp();
        mHandler.sendEmptyMessage(1001);
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1111);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(PIN_ERROR);
        mHandler.removeMessages(PIN_CLICK_NUMBER);
        mHandler.removeMessages(PIN_CLICK_CONFIRM);
        mHandler.removeMessages(PIN_CLICK_CANCEL);
        try {
            MyApplication.getInstance().pinPadOptV2.cancelInputPin();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_FAILURE);
        }

        if (v == resetBtn) {
            testTp();
        }

    }

    private void testTp() {
        tpInfoTv.setText("请输入4-6位密码");
        setBtnGone();
        PinPadConfigV2 pinPadConfig = new PinPadConfigV2();
        pinPadConfig.setPinPadType(0);
        pinPadConfig.setPinType(1);
        pinPadConfig.setOrderNumKey(false);
        pinPadConfig.setTimeout(60 * 1000);
        pinPadConfig.setPinKeyIndex(3);
        pinPadConfig.setMaxInput(6);
        pinPadConfig.setMinInput(4);
        pinPadConfig.setSupportbypass(false);
        pinPadConfig.setPan("1234567890".getBytes());
        try {
            String result = MyApplication.getInstance().pinPadOptV2.initPinPad(pinPadConfig, mPinPadListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private final PinPadListenerV2 mPinPadListener = new PinPadListenerV2.Stub() {

        @Override
        public void onPinLength(int len) throws RemoteException {
            Log.e(TAG, "onPinLength len:" + len);
            mHandler.obtainMessage(PIN_CLICK_NUMBER, len, 0).sendToTarget();
        }

        @Override
        public void onConfirm(int status, byte[] pinBlock) throws RemoteException {
            Log.e(TAG, "onConfirm status:" + status);
            if (pinBlock != null) {
                String hexStr = ByteUtil.bytes2HexStr(pinBlock);
                Log.e(TAG, "hexStr:" + hexStr);
                boolean equals = TextUtils.equals("00", hexStr);
                if (equals) {
                    pinCipher = "";
                } else {
                    pinCipher = hexStr;
                }
            }

            mHandler.sendEmptyMessage(PIN_CLICK_CONFIRM);

        }

        @Override
        public void onCancel() throws RemoteException {
            Log.e(TAG, "onCancel");
            mHandler.sendEmptyMessage(PIN_CLICK_CANCEL);
        }

        @Override
        public void onError(int code) throws RemoteException {
            Log.e(TAG, "onError code:" + code);
            mHandler.obtainMessage(PIN_ERROR, code, 0).sendToTarget();
        }

    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case PIN_CLICK_CONFIRM:
                    tpInfoTv.setText("当前点击了确定键");
                    deInit(mFatherName, SUCCESS);
                    break;
                case PIN_CLICK_CANCEL:
                    tpInfoTv.setText("当前点击了取消键");
                    setBtnVisible();
                    break;
                case PIN_ERROR:
                    tpInfoTv.setText("密码键盘测失败");
                    setPassGone();
                    break;
                case PWD_FAILED:
                    tpInfoTv.setText("密码键盘测异常");
                    setPassGone();
                    break;
                case 1111:
                    deInit(mFatherName, FAILURE);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };


    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            deInit(mFatherName, result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(mFatherName, result);
        }
    }

    private void setBtnVisible() {
        resetBtn.setVisibility(View.VISIBLE);
        mSuccess.setVisibility(View.VISIBLE);
        mFail.setVisibility(View.VISIBLE);
    }

    /**
     * 设置按钮全部影藏  用于测试过程中
     */
    private void setBtnGone() {
        mSuccess.setVisibility(View.GONE);
        mFail.setVisibility(View.GONE);
        resetBtn.setVisibility(View.GONE);
    }

    /**
     * 设置影藏pass按钮显示reset和failed按钮  用于测试结果不需要测试人员来判断但是测试失败
     */
    private void setPassGone() {
        mFail.setVisibility(View.VISIBLE);
        mSuccess.setVisibility(View.GONE);
        resetBtn.setVisibility(View.VISIBLE);
    }

}
