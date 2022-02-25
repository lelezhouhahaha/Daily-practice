package com.meigsmart.meigrs32.activity;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import butterknife.BindView;

public class MeshPlateActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.mesh_plate_prompt)
    public TextView mPromptTextView;
    @BindView(R.id.mesh_plate_pass)
    public TextView mPassNodeTextView;
    @BindView(R.id.mesh_plate_fail)
    public TextView mFailNodeTextView;

    private MeshPlateActivity mContext;
    private String mFatherName = "";

    private String mPassNodes;
    private String mFailNodes;

    private final int TEST_SUCCESS = 1001;
    private final int TEST_FAILED = 1002;
    private final int TEST_END_FAILED = 1003;
    private final int TEST_END_SUCCESS = 1004;
    private final int TEST_UPDATE_FAILED_MSG = 2001;
    private final int TEST_UPDATE_RESULT_MSG = 2002;

    private final int CMD_TIMEOUT_DELAY = 2000;

    private final int CMD_ENTER_TEST = 0;
    private final int CMD_CLEAR_SENSOR = 1;
    private final int CMD_SCAN_SENSOR = 2;
    private final int CMD_LEAVE_TEST = 3;
    private final String TEST_CMD[] = {
            "castles enter_test",
            "castles clear_sensor",
            "castles scan_sensor",
            "castles leave_test",
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_mesh_plate;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.MeshPlateActivity);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        LogUtil.d("init");
        mThread.start();
        mCmdHandler = new MyHandler(mThread.getLooper());
        startTest();
    }

    private void startTest() {
        mCmdHandler.sendEmptyMessageDelayed(CMD_CLEAR_SENSOR, CMD_TIMEOUT_DELAY);
    }

    private String runShellCommand(String command) {
        String result = "";
        try {
            String line;
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = input.readLine()) != null) {
                result += line + "\n";
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d(e.getMessage());
            return null;
        }
        return result;
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case TEST_SUCCESS:
                    deInit(mFatherName, SUCCESS);
                    break;
                case TEST_FAILED:
                    deInit(mFatherName, FAILURE);
                    break;
                case TEST_END_FAILED:
                    mFail.setVisibility(View.VISIBLE);
                    mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                    break;
                case TEST_END_SUCCESS:
                    mSuccess.setVisibility(View.VISIBLE);
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                    break;
                case TEST_UPDATE_FAILED_MSG:
                    mPromptTextView.setText(R.string.mesh_plate_test_failed);
                    mHandler.sendEmptyMessage(TEST_END_FAILED);
                    break;
                case TEST_UPDATE_RESULT_MSG:
                    mPromptTextView.setVisibility(View.INVISIBLE);
                    mPassNodeTextView.setText(mPassNodeTextView.getText() + ":" + mPassNodes);
                    mPassNodeTextView.setVisibility(View.VISIBLE);
                    mFailNodeTextView.setText(mFailNodeTextView.getText() + ":" + mFailNodes);
                    mFailNodeTextView.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    private boolean parseResult(int index, String result) {
        LogUtil.d("cmd:" + index + " result: " + result);
        switch (index) {
            case CMD_ENTER_TEST:
            case CMD_CLEAR_SENSOR:
                String command = TEST_CMD[index].substring("castles ".length());
                if (!TextUtils.isEmpty(result) &&
                        result.matches("\\s*" + command + "\\s+" + "ok\\s*"))
                    return true;
                else
                    return false;
            case CMD_SCAN_SENSOR:
                String ret[] = result.split("\\n");
                for (String str : ret){
                    str = str.trim();
                    if (str.startsWith("pass"))
                        mPassNodes = str.substring("pass".length());
                    if (str.startsWith("fail"))
                        mFailNodes = str.substring("fail".length());
                }
                LogUtil.d("pass:" + mPassNodes + " fail: " + mFailNodes);
                return TextUtils.isEmpty(mFailNodes.trim());
        }
        return false;
    }

    HandlerThread mThread = new HandlerThread("shell_cmd");
    Handler mCmdHandler;
    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            String result;
            boolean ret;
            LogUtil.d("handle cmd:" + msg.what);
            switch (msg.what) {
                case CMD_ENTER_TEST:
                    result = runShellCommand(TEST_CMD[CMD_ENTER_TEST]);
                    ret = parseResult(CMD_ENTER_TEST, result);
                    if (ret)
                        mCmdHandler.sendEmptyMessageDelayed(CMD_CLEAR_SENSOR, CMD_TIMEOUT_DELAY);
                    else
                        mHandler.sendEmptyMessage(TEST_UPDATE_FAILED_MSG);
                    break;
                case CMD_CLEAR_SENSOR:
                    result = runShellCommand(TEST_CMD[CMD_CLEAR_SENSOR]);
                    ret = parseResult(CMD_CLEAR_SENSOR, result);
                    if (ret)
                        mCmdHandler.sendEmptyMessageDelayed(CMD_SCAN_SENSOR, CMD_TIMEOUT_DELAY);
                    else
                        mHandler.sendEmptyMessage(TEST_UPDATE_FAILED_MSG);
                    break;
                case CMD_SCAN_SENSOR:
                    result = runShellCommand(TEST_CMD[CMD_SCAN_SENSOR]);
                    ret = parseResult(CMD_SCAN_SENSOR, result);
                    mHandler.sendEmptyMessage(TEST_UPDATE_RESULT_MSG);
                    // mCmdHandler.sendEmptyMessageDelayed(CMD_LEAVE_TEST, CMD_TIMEOUT_DELAY);
                    if (ret)
                        mHandler.sendEmptyMessageDelayed(TEST_SUCCESS, CMD_TIMEOUT_DELAY);
                    else
                        mHandler.sendEmptyMessageDelayed(TEST_END_FAILED, CMD_TIMEOUT_DELAY);
                    break;
                case CMD_LEAVE_TEST:
                    runShellCommand(TEST_CMD[CMD_LEAVE_TEST]);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(TEST_FAILED);
        mHandler.removeMessages(TEST_SUCCESS);
        mHandler.removeMessages(TEST_END_FAILED);
        mHandler.removeMessages(TEST_END_SUCCESS);
        mHandler.removeMessages(TEST_UPDATE_FAILED_MSG);
        mThread.quit();
    }
}
