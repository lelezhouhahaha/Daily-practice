package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.PersistResultModel;
import com.meigsmart.meigrs32.model.ResultModel;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.DiagJniInterface;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;

public class PCBASignalActivity extends BaseActivity implements View.OnClickListener , CheckListAdapter.OnCallBackCheckFunction {
    private PCBASignalActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private CheckListAdapter mAdapter;
    private int currPosition = 0;
    @BindView(R.id.more)
    public LinearLayout mMore;
    private boolean isLayout = true;//true linearLayout  ;false gridLayout

    private String mDefaultPath;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mFileName ;
    private String projectName = "";
    private DiagJniInterface mDiag = null;
    private MyHandler mHandler = null;
    public final static int HANDLER_DIAG_COMMAND = 10000;
    public final static int HANDLER_DIAG_COMMAND_SET_RESULT = 10010;
    private final String TAG = PCBASignalActivity.class.getSimpleName();

    private List<String> config_list = new ArrayList<>();
    @Override
    protected int getLayoutId() {
        return R.layout.activity_pcba;
    }

    private static class MyHandler extends Handler {
        WeakReference<Activity> reference;

        public MyHandler(Activity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PCBASignalActivity activity = (PCBASignalActivity) reference.get();
            switch (msg.what) {
                case HANDLER_DIAG_COMMAND:
                    Log.d(activity.TAG, "diag command handler");
                    try {
                        Thread.sleep(10000);
                        Log.d(activity.TAG, "send msg");
                        String mCmmdContent = (String) msg.getData().get("diag_command");
                        int mDiagCmmdId = Integer.valueOf(mCmmdContent);
                        activity.mDiag.SendDiagResult(mDiagCmmdId,"1", 1);
                        //doSendLocalMessage(SERVICEID, "AT+SOFTWAREINFO");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case HANDLER_DIAG_COMMAND_SET_RESULT:
                    Log.d(activity.TAG, "diag command handler set result");
                    break;
            }
        }
    }
    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mMore.setSelected(isLayout);
        mTitle.setText(R.string.function_pcba_signal);
        mDiag = new DiagJniInterface();
        mDiag.Diag_Init();
        mHandler = new MyHandler(mContext);
        mDiag.setHandler(mHandler);

        mDefaultPath = getResources().getString(R.string.pcba_signal_save_log_default_path);
        mFileName = getResources().getString(R.string.pcba_signal_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.pcba_signal_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.pcba_signal_save_log_custom_path);
        LogUtil.d("mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

        super.mName = getIntent().getStringExtra("name");
        LogUtil.d("citapk super.mName:" + super.mName);
        super.mFatherName = getIntent().getStringExtra("fatherName");
        LogUtil.d("citapk super.mFatherName:" + super.mFatherName);

        String Name = getStringFromName(mContext, "PCBAActivity");
        LogUtil.d("citapk Name:" + Name);
        if (SAVE_EN_LOG)
            super.mList = getFatherData(super.mName);
        else if (!TextUtils.isEmpty(Name)){
            super.mList = getFatherData(Name);
            LogUtil.d("citapk super.mList:" + super.mList);
        }

        List<String> config = Const.getXmlConfig(this, Const.CONFIG_PCBA_SIGNAL);
        /*jicong.wang add for task 5845 start {@ */
        if (CustomConfig.isSLB761X()){
            config = CustomConfig.SLB761XtestItemConfig(config);
        }
        /*jicong.wang add for task 5845 end @}*/
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        String face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
        if("MT537".equals(projectName)){
            if((face_select!=null)&&(!face_select.equals(""))&&(face_select.length()==8)) {
                if(!face_select.substring(6, 7).equals("1")) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.FingerOtgActivity))) {
                            list.remove(i);
                        }
                    }
                }
                if(!face_select.substring(5, 6).equals("1")) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.Id_Test))) {
                            list.remove(i);
                        }
                    }
                }
            }
        }
        if (list.size()>10)mMore.setVisibility(View.VISIBLE);
        mAdapter.setData(list);
        for (TypeModel m:list) {
            config_list.add(m.getName());
        }
        config_list.add("pcba_all");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mDiag.Diag_Deinit();
    }

    @Override
    public void onClick(View v) {
        if (v == mBack)mContext.finish();
        if (v == mMore){
            if (isLayout){
                isLayout = false;
                mRecyclerView.setLayoutManager(new GridLayoutManager(this,2));
            }else {
                isLayout = true;
                mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
            mMore.setSelected(isLayout);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(int position) {
        if(!DataUtil.isFastClick()) {
            currPosition = position;
            LogUtil.d(this.getLocalClassName() + " currPosition: " + currPosition);
            startActivity(mAdapter.getData().get(position));
        }else LogUtil.d(this.getLocalClassName() + " click too fast.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.d("resultCode: " + resultCode);
        if (resultCode == 1111 || resultCode == 1000){
            if (data!=null){
                int results = data.getIntExtra("results",0);
                LogUtil.d("test results:" + results);
                mAdapter.getData().get(currPosition).setType(results);
                mAdapter.notifyDataSetChanged();

            }
        }
    }
}
