package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import butterknife.BindView;

public class MemoryActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private MemoryActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.progressBar)
    public ProgressBar mProgress;
    @BindView(R.id.result)
    public TextView mResult;
    private int progressValue;
    private int length;//标记文件大小
    private int type = 0;//0  assets中读取
    private String path = "";
    private Thread mWriteThread;
    private Thread mReadThread;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mCustomFileName ;

    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.TV_size)
    public TextView mRSize;

    private String firstPath = "";
    private Boolean mIsRuning = true;
    private Boolean mMemeryTestReadStatus = false;
    private Boolean mMemeryTestWriteStatus = false;
    private final String TAG = MemoryActivity.class.getSimpleName();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_memory;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;

        isCustomPath = getResources().getBoolean(R.bool.memory_default_config_is_user_custom_path);
        mCustomPath = getResources().getString(R.string.memory_default_config_custom_path);
        mCustomFileName = getResources().getString(R.string.memory_default_config_custom_file_name);
        LogUtil.d("mConfigResult:" + mConfigResult +
                " mConfigTime:" + mConfigTime+
                " mCustomPath:" + mCustomPath+
                " mCustomFileName:"+mCustomFileName+
                " isCustomPath:"+isCustomPath);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mTitle.setText(super.mName);
		
		if(mFatherName.equals(MyApplication.RuninTestNAME)){
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
        }else {
            mSuccess.setOnClickListener(this);
            mFail.setOnClickListener(this);
        }

        mConfigResult = getResources().getInteger(R.integer.memory_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        if (super.mName.contains(getResources().getString(R.string.pcba_memory_ram))){
            path = FileUtil.createInnerPath(mContext,mCustomFileName);
        }else if (super.mName.contains(getResources().getString(R.string.pcba_memory_emmc))){
            path = FileUtil.createSDPath(mCustomFileName);
        }
        LogUtil.d(super.mName+" write file path :"+path);

        //mResult.setText(R.string.start_tag);
        mHandler.sendEmptyMessageDelayed(1113,getResources().getInteger(R.integer.start_delay_time));
        if (super.mName.contains(getResources().getString(R.string.pcba_memory_ram))) {
            String ramSize = DataUtil.getTotalMemory(mContext, getResources().getString(R.string.version_default_config_software_version_ram_size_path));
            String availRamSize = DataUtil.getAvailMemory(mContext);
            mRSize.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_default_config_software_version_ram_size) +
                                    "&nbsp;" + "<font color='#0000FF'>" + availRamSize +"/" + ramSize + "</font>"
                    )
            );
        } else if (super.mName.contains(getResources().getString(R.string.pcba_memory_emmc))) {
            String romSize = DataUtil.getRomSpace(mContext);
            mRSize.setText(
                    Html.fromHtml(
                            getResources().getString(R.string.version_default_config_software_version_rom_size) +
                                    "&nbsp;" + "<font color='#0000FF'>" + romSize + "</font>"
                    )
            );
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                if(mIsRuning) {
                    mConfigTime--;
                    updateFloatView(mContext, mConfigTime);
                    if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext) )) {
                        mHandler.sendEmptyMessage(1111);
                    }
                    mHandler.postDelayed(this, 1000);
                }
            }
        };
        mRun.run();
    }

    /*private String getFileVersion(String path, String substr){
        String data = "";
        FileInputStream file = null;
        BufferedReader reader = null;
        try{
            file = new FileInputStream(path);
            reader = new BufferedReader(new InputStreamReader(file));
            while ((data = reader.readLine()) != null) {
                if (data.contains(substr)) {
                    if(file != null) {
                        file.close();
                        file = null;
                    }
                    return data.substring(data.lastIndexOf(":")+1);
                }
            }
            if(file != null) {
                file.close();
                file = null;
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(mReadThread);
        mHandler.removeCallbacks(mWriteThread);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
        mHandler.removeMessages(1111);
        mHandler.removeMessages(9999);
        mIsRuning = true;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    progressValue += msg.arg1;
                    mProgress.setProgress(progressValue);
                    if(progressValue == length){
                        mMemeryTestReadStatus= true;
                        mHandler.sendEmptyMessage(1002);
                    }
                    mResult.setText(msg.obj.toString());
                    break;
                case 1002:
                    initWrite(mResult.getText().toString());
                    break;
                case 1003:
                    progressValue += msg.arg1;
                    mProgress.setProgress(progressValue);
                    if(progressValue == length){
                        mMemeryTestWriteStatus= true;
                        mHandler.sendEmptyMessage(1004);
                    }
                    mResult.setText(msg.obj.toString());
                    break;
                case 1004://start finish
                    mResult.setText("");
                    progressValue = 0;
                    mProgress.setProgress(0);
                    init(path);
                    mDialog.setSuccess();
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1113:
                    if (isCustomPath){
                        if (!TextUtils.isEmpty(mCustomPath) && !TextUtils.isEmpty(mCustomFileName)){
                            File file = FileUtil.createRootDirectory(mCustomPath);
                            File file1 = FileUtil.mkDir(file);
                            File f = new File(file1.getPath(),mCustomFileName);
                            if (f.exists() && FileUtil.getFileSize(f)<=500){
                                firstPath = f.getPath();
                            }else{
                                ToastUtil.showBottomShort("The content of the file is too large，preferably less than 500kb");
                                sendErrorMsgDelayed(mHandler,"The content of the file is too large，preferably less than 500kb");
                            }
                        }else {
                            ToastUtil.showBottomShort("the file path is not null");
                            sendErrorMsgDelayed(mHandler,"the file path is not null");
                        }
                    }
                    init(firstPath);
                    break;
                case 1111:
                    if (MyApplication.RuninTestNAME.equals(mFatherName))deInit(mFatherName, SUCCESS);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
                default:
                    break;
            }
        }
    };

    private void init(final String path) {
        if (TextUtils.isEmpty(path)){
            type =0;
        }else{
            File file = new File(path);
            if (file.exists()) {
                type = 1;
                length = (int) file.length();
                mProgress.setMax(length);//设置进度条最大值
            }else{
                mHandler.sendEmptyMessage(1112);
                return;
            }
        }

        mReadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (type == 1){
                    readFromFile(path);
                }else{
                    readFromResets();
                }
            }
        },"readFile");
        mReadThread.start();

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
    }

    private void stop(){
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
        mHandler.removeMessages(1111);
        mHandler.removeMessages(9999);
        mHandler.removeMessages(1113);
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

    public void readFromFile(String path) {
        try {
            int line;
            FileInputStream fis = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(fis);
            StringBuffer sb = new StringBuffer();
            byte b[] = new byte[1];
            while ((line = dis.read(b)) != -1) {
                String mData = new String(b, 0, line);
                sb.append(mData);
                Message msg = new Message();
                msg.what = 1001;
                msg.arg1 = line;
                msg.obj = sb.toString();
                mHandler.sendMessage(msg);
                Thread.sleep(50);
            }
            dis.close();
            fis.close();
        } catch (Exception e) {
            LogUtil.e(e.getMessage());
            setTestFailReason(getResources().getString(R.string.fail_reson_memory_read));
            Log.d(TAG, getTestFailReason());
            sendErrorMsg(mHandler,e.getMessage());
        }
    }

    private void initWrite(final String msg){
        if (TextUtils.isEmpty(msg)){
            mHandler.sendEmptyMessage(1112);
            return;
        }
        progressValue = 0;
        mProgress.setProgress(0);
        length = (int) msg.length();
        mProgress.setMax(length);//设置进度条最大值

        mWriteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeFile(msg);
            }
        },"writeFile");
        mWriteThread.start();
    }

    private void writeFile(String data){
        try {
            OutputStream out = new FileOutputStream(path);
            InputStream is = new ByteArrayInputStream(data.getBytes());
            StringBuffer sb = new StringBuffer();
            byte[] buff = new byte[1];
            int len = 0;
            while((len=is.read(buff))!=-1){
                out.write(buff, 0, len);

                String mData = new String(buff, 0, len);
                sb.append(mData);

                Message msg = new Message();
                msg.what = 1003;
                msg.arg1 = len;
                msg.obj = data.replace(sb.toString(),"");
                mHandler.sendMessage(msg);
                Thread.sleep(50);
            }
            is.close();
            out.close();
        } catch (Exception e) {
            LogUtil.e(e.getMessage());
            setTestFailReason(getResources().getString(R.string.fail_reson_memory_write));
            Log.d(TAG, getTestFailReason());
            sendErrorMsg(mHandler,e.getMessage());
        }
    }

    public void readFromResets() {
        try {
            int line;
            InputStream is = this.getAssets().open("memory.txt");

            length = is.available();
            mProgress.setMax(length);//设置进度条最大值
            DataInputStream dis = new DataInputStream(is);
            StringBuffer sb = new StringBuffer();
            byte b[] = new byte[1];
            while ((line = dis.read(b)) != -1) {
                String mData = new String(b, 0, line);
                sb.append(mData);
                Message msg = new Message();
                msg.what = 1001;
                msg.arg1 = line;
                msg.obj = sb.toString();
                mHandler.sendMessage(msg);
                Thread.sleep(50);
            }
            dis.close();
            is.close();
        } catch (Exception e) {
            LogUtil.e(e.getMessage());
            setTestFailReason(getResources().getString(R.string.fail_reson_memory_read));
            Log.d(TAG, getTestFailReason());
            sendErrorMsg(mHandler,e.getMessage());
        }
    }

}
