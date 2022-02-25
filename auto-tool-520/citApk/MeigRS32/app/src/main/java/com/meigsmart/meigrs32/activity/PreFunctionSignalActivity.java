package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;

public class PreFunctionSignalActivity extends BaseActivity implements View.OnClickListener , CheckListAdapter.OnCallBackCheckFunction {
    private PreFunctionSignalActivity mContext;
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

    @Override
    protected int getLayoutId() {
        return R.layout.activity_pre_function;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mMore.setSelected(isLayout);
        mTitle.setText(R.string.function_pre_function_signal);

        mDefaultPath = getResources().getString(R.string.pre_function_signal_save_log_default_path);
        mFileName = getResources().getString(R.string.pre_function_signal_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.pre_function_signal_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.pre_function_signal_save_log_custom_path);
        LogUtil.d("mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        if("MT537".equals(projectName)) {
            //if (SystemProperties.get("persist.custmized.face_select") == null || SystemProperties.get("persist.custmized.face_select").equals("")) {
            if (SystemProperties.get(OdmCustomedProp.getFaceSelectProp()) == null || SystemProperties.get(OdmCustomedProp.getFaceSelectProp()).equals("")) {
                showDialog();
            }
        }
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getIntent().getStringExtra("name");
        LogUtil.d("citapk super.mName:" + super.mName);
        super.mFatherName = getIntent().getStringExtra("fatherName");
        LogUtil.d("citapk super.mFatherName:" + super.mFatherName);

        String Name = getStringFromName(mContext, "PreFunctionActivity");
        if (!TextUtils.isEmpty(Name)){
            super.mList = getFatherData(Name);
        }

        List<String> config = Const.getXmlConfig(this,Const.CONFIG_PRE_FUNCTION_SIGNAL);
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        if (list.size()>10)mMore.setVisibility(View.VISIBLE);
        mAdapter.setData(list);
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String info = getResources().getString(R.string.structured_light_select);
        builder.setMessage(info);
        builder.setCancelable(false);
        builder.setNegativeButton(getResources().getString(R.string.structured_light_front), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //SystemProperties.set("persist.custmized.face_select","camera_front");
                SystemProperties.set(OdmCustomedProp.getFaceSelectProp(),"camera_front");
            }
        });
        builder.setPositiveButton(getResources().getString(R.string.structured_light_rear), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //SystemProperties.set("persist.custmized.face_select","camera_rear");
                SystemProperties.set(OdmCustomedProp.getFaceSelectProp(),"camera_rear");
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
