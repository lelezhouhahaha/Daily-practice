package com.meigsmart.meigrs32.activity;

import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.util.ByteUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidl.AidlErrorCode;
import com.sunmi.pay.hardware.aidlv2.tax.TaxOptV2;

import java.util.Arrays;

import butterknife.BindView;

public class TaxTestActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private TaxTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.key_root_view)
    public LinearLayout mRootView;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.handleRWWriteData)
    public Button TaxWriteData;
    @BindView(R.id.handleRWReadData)
    public Button TaxReadData;
    @BindView(R.id.handleRWData_show)
    public TextView TaxDataShow;
    private String TAG = "TaxTestActivity";
    private int mConfigTime = 0;
    private final TaxOptV2 taxOpt = MyApplication.getInstance().taxOptV2;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_tax_layout;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.Tax_Test);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        TaxWriteData.setOnClickListener(this);
        TaxReadData.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        initLayout();
    }

    /**
     * 根据XML动态创建按键测试布局
     */
    private void initLayout() {
    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
        if (v == TaxWriteData) {
            handleRWWriteData();
        }
        if (v == TaxReadData) {
            handleRWReadData();
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:

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

    /**
     * Read/Write-Write tax data
     */
    private void handleRWWriteData() {
        //Write data command：1B 1D 08 00 0B 04 06 00 11 11 03 02 0A 15 39 18
        TaxDataShow.setText(null);
        addTextData("Tax write data:");
        byte[] head = {0x1B, 0x1D};
        byte[] command = {0x08};
        byte[] dataLen = {0x00, 0x0B};
        byte[] data = {0x04, 0x06, 0x00, 0x11, 0x11, 0x03, 0x02, 0x0A, 0x15, 0x39, 0x18};
        byte[] send = ByteUtil.concatByteArrays(head, command, dataLen, data);
        byte[] out = new byte[2048];//Max out buffer length is 1030
        try {
            String msg = "Send >>" + ByteUtil.byte2PrintHex(send, 0, send.length);
            Log.e(TAG, msg);
            addTextData(msg);
            int len = taxOpt.taxDataExchange(send, out);
            if (len < 0) {// Write data error
                msg = "Write data,code:" + len + ",msg:" + AidlErrorCode.valueOf(len).getMsg();
                Log.e(TAG, msg);
                addTextData(msg);
            } else {// Write data success
                byte[] validLen = Arrays.copyOf(out, len);
                msg = "Receive <<" + ByteUtil.byte2PrintHex(validLen, 0, len);
                Log.e(TAG, msg);
                addTextData(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read/Write-Read tax data
     */
    private void handleRWReadData() {
        //读数据命令：1B 1D 09 00 06
        TaxDataShow.setText(null);
        addTextData("Tax read data:");
        byte[] head = {0x1B, 0x1D};
        byte[] command = {0x09};
        byte[] dataLen = {0x00, 0x06};
        byte[] send = ByteUtil.concatByteArrays(head, command, dataLen);
        byte[] out = new byte[1030];//Max out buffer length is 1030
        try {
            String msg = "Send >>" + ByteUtil.byte2PrintHex(send, 0, send.length);
            Log.e(TAG, msg);
            addTextData(msg);
            int len = taxOpt.taxDataExchange(send, out);
            if (len < 0) {// Read data error
                msg = "Read data error,code:" + len + ",msg:" + AidlErrorCode.valueOf(len).getMsg();
                Log.e(TAG, msg);
                addTextData(msg);
            } else { // Read data success
                byte[] validLen = Arrays.copyOf(out, len);
                msg = "Receive <<" + ByteUtil.byte2PrintHex(validLen, 0, len);
                Log.e(TAG, msg);
                addTextData(msg);
                mSuccess.setVisibility(View.VISIBLE);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void addTextData(String data) {
        StringBuilder sb = new StringBuilder(TaxDataShow.getText());
        sb.append(data);
        sb.append("\n");
        TaxDataShow.setText(sb);
    }
}
