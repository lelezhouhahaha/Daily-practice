package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.FileOutputStream;

import butterknife.BindView;

public class LaserLightActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private LaserLightActivity mContext;
    @BindView(com.meigsmart.meigrs32.R.id.title)
    public TextView mTitle;
    @BindView(com.meigsmart.meigrs32.R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    private String Laserswitch = "/sys/cash_drawer/kickstate";
    private String open_value = "1";
    private String close_value = "0";
    private final String COMMON_LASER_NODE_PATH = "common_laser_node_path";
    private final String COMMON_LASER_OPEN_VALUE = "common_laser_open_value";
    private final String COMMON_LASER_CLOSE_VALUE = "common_laser_close_value";

    @BindView(com.meigsmart.meigrs32.R.id.success)
    public Button mSuccess;
    @BindView(com.meigsmart.meigrs32.R.id.fail)
    public Button mFail;

    @Override
    protected int getLayoutId() {
        return com.meigsmart.meigrs32.R.layout.activity_laserlight;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mSuccess.setVisibility(View.GONE);
        mFail.setOnClickListener(this);
        mTitle.setText(com.meigsmart.meigrs32.R.string.LaserLightActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        String laser_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, COMMON_LASER_NODE_PATH);
        String laser_open = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, COMMON_LASER_OPEN_VALUE);
        String laser_close = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, COMMON_LASER_CLOSE_VALUE);
        if(null!=laser_path && !laser_path.isEmpty()) Laserswitch=laser_path;
        if(null!=laser_open && !laser_path.isEmpty()) open_value=laser_open;
        if(null!=laser_close && !laser_path.isEmpty()) close_value=laser_close;


        mHandler.sendEmptyMessage(1001);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mSuccess.setVisibility(View.VISIBLE);
                    writeToFile(Laserswitch,open_value);//turn on laser
                    mHandler.sendEmptyMessageDelayed(1002,5000);
                    break;
                case 1002:
                    writeToFile(Laserswitch,close_value);//turn off laser
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        writeToFile(Laserswitch,close_value);
        super.onDestroy();
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
		
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 11 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }

    private boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
