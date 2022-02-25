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
import com.meigsmart.meigrs32.model.ResultModel;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;

public class InformationCheckActivity extends BaseActivity implements View.OnClickListener , CheckListAdapter.OnCallBackCheckFunction {
    private InformationCheckActivity mContext;
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
        return R.layout.information_check_list_item;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mMore.setSelected(isLayout);
        mTitle.setText(R.string.function_information_check);

        mDefaultPath = getResources().getString(R.string.information_check_save_log_default_path);
        mFileName = getResources().getString(R.string.information_check_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.information_check_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.information_check_save_log_custom_path);
        LogUtil.d("mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getIntent().getStringExtra("name");

        if (!TextUtils.isEmpty(super.mName)){
            super.mList = getFatherData(super.mName);
        }

        List<String> config = Const.getXmlConfig(this,Const.CONFIG_INFORMATION_CHECK);
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        mAdapter.setData(list);
        mHandler.sendEmptyMessage(1005);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1003://test finish
                    ToastUtil.showBottomShort(getResources().getString(R.string.information_check_test_finish));
                    initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
                    mContext.finish();
                    break;
                case 1004://save log
                    initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
                    break;
                case 1005:
                    startActivity(mAdapter.getData().get(currPosition));
                    break;
            }
        }
    };

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
        //currPosition = position;
        //startActivity(mAdapter.getData().get(position));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1005);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1111){
            if (data!=null){
                int results = data.getIntExtra("results",0);
                LogUtil.d("test results:" + results);
                mAdapter.getData().get(currPosition).setType(results);
                mAdapter.notifyDataSetChanged();

                currPosition++;
                //if ( currPosition == Const.pcbaList.length ){
                if(currPosition == mAdapter.getItemCount()){
                    LogUtil.d("currPosition :" + currPosition);
                    LogUtil.d("mAdapter.getItemCount():" + mAdapter.getItemCount());
                        mHandler.sendEmptyMessageDelayed(1003,Const.DELAY_TIME);
                        return;
                }

                Message msg = mHandler.obtainMessage();
                msg.what = 1004;
                mHandler.sendMessage(msg);
                LogUtil.d("mAdapter.getItemCount():" + mAdapter.getItemCount());
                mHandler.sendEmptyMessageDelayed(1005,Const.DELAY_TIME);
            }
        }
    }
}
