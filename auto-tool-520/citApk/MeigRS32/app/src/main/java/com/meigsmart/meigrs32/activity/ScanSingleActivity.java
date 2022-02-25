package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.PersistResultModel;
import com.meigsmart.meigrs32.model.ResultModel;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class ScanSingleActivity extends BaseActivity implements View.OnClickListener , CheckListAdapter.OnCallBackCheckFunction {
    private ScanSingleActivity mContext;
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

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mMore.setSelected(isLayout);
        mTitle.setText(R.string.ScanSingleActivity);

        mDefaultPath = getResources().getString(R.string.scan_save_log_default_path);
        mFileName = getResources().getString(R.string.scan_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.scan_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.scan_save_log_custom_path);
        LogUtil.d("mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getIntent().getStringExtra("name");
        LogUtil.d("citapk super.mName:" + super.mName);

        String Name = getStringFromName(mContext, "ScanSingleActivity");
        LogUtil.d("citapk Name:" + Name);
        if (!TextUtils.isEmpty(Name)){
            super.mList = getFatherData(Name);
            LogUtil.d("citapk super.mList:" + super.mList);
        }

        List<String> config = Const.getXmlConfig(this, Const.CONFIG_SCAN_SINGLE);
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        if (list.size()>10)mMore.setVisibility(View.VISIBLE);
        mAdapter.setData(list);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LogUtil.d("handler: " + msg.what);
            switch (msg.what){
                case 1001://save log
//                    initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
                    saveLog();
                    break;
            }
        }
    };

    private void saveLog() {
        List<FunctionBean> list = getFatherData(ScanSingleActivity.super.mName);
        List<ResultModel> resultList = new ArrayList<>();
        List<PersistResultModel> persistResultList = new ArrayList<>();

        boolean isAllSuccess = true;
        for (FunctionBean bean:list){
            ResultModel model = new ResultModel();
            PersistResultModel persistModel = new PersistResultModel();

            switch (bean.getResults()){
                case 0:
                    model.setResult(Const.RESULT_NOTEST);
                    persistModel.setResult(Const.RESULT_NOTEST);
                    isAllSuccess = false;
                    break;
                case 1:
                    model.setResult(Const.RESULT_FAILURE);
                    persistModel.setResult(Const.RESULT_FAILURE);
                    isAllSuccess = false;
                    break;
                case 2:
                    model.setResult(Const.RESULT_SUCCESS);
                    persistModel.setResult(Const.RESULT_SUCCESS);
                    break;
            }

            model.setFatherName(bean.getFatherName());
            model.setSubName(bean.getSubclassName());
            model.setReason(bean.getReason());

            persistModel.setName(bean.getSubclassName());

            resultList.add(model);
            persistResultList.add(persistModel);
        }

       /* PersistResultModel allResultModel = new PersistResultModel();
        allResultModel.setName("pcba_all");
        allResultModel.setResult(isAllSuccess ? Const.RESULT_SUCCESS : Const.RESULT_FAILURE);
        persistResultList.add(allResultModel);*/

        writeSDCardResult(isCustomPath ? mCustomPath : mDefaultPath, mFileName,  JSON.toJSONString(resultList));

        writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE),  Const.SCAN_SINGLE_RESULT_FILE, JSON.toJSONString(persistResultList));
    }

    private boolean writePersistResult(String path, String fileName, String result) {
        File persistPath = new File(Const.getLogPath(Const.TYPE_LOG_PATH_DIR));
        if(persistPath.exists() && persistPath.isDirectory()){
            File dir = FileUtil.mkDir(new File(path));
            return !"".equals(FileUtil.writeFile(dir, fileName, result));
        }
        return false;
    }

    private boolean writeSDCardResult(String path, String fileName, String result){
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)) {
            File dir = FileUtil.mkDir(FileUtil.createRootDirectory(path));
            return !"".equals(FileUtil.writeFile(dir, fileName,result));
        }
        return false;
    }

    private String initPath(String path, String fileName, String result){
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)){
            File f = FileUtil.createRootDirectory(path);
            File file = FileUtil.mkDir(f);
            return FileUtil.writeFile(file,fileName,result);
        }
        return "";
    }

    private String createJsonResult(){
        List<FunctionBean> list = getFatherData(super.mName);
        List<ResultModel> resultList = new ArrayList<>();
        for (FunctionBean bean:list){
            ResultModel model = new ResultModel();
            if (bean.getResults() == 0){
                model.setResult(Const.RESULT_NOTEST);
            } else if (bean.getResults() == 1){
                model.setResult(Const.RESULT_FAILURE);
            } else if (bean.getResults() == 2){
                model.setResult(Const.RESULT_SUCCESS);
            }
            model.setFatherName(bean.getFatherName());
            model.setSubName(bean.getSubclassName());
            model.setReason(bean.getReason());
            resultList.add(model);
        }
        return JSON.toJSONString(resultList);
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

                mHandler.sendEmptyMessageDelayed(1001,2000);
            }
        }
    }
}
