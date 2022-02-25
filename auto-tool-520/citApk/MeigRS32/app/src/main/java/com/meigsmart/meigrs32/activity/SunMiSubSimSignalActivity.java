package com.meigsmart.meigrs32.activity;


import android.content.Intent;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;

import java.util.List;

import butterknife.BindView;

public class SunMiSubSimSignalActivity extends BaseActivity implements View.OnClickListener, CheckListAdapter.OnCallBackCheckFunction {
    private SunMiSubSimSignalActivity mContext;
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
    private boolean isLayout = true;

    private String projectName = "";
    private String TAG = "SunMiSubSimSignalActivity";

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
        mTitle.setText(R.string.sunmi_subsim_signal);

        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getIntent().getStringExtra("name");
        Log.d(TAG, "super.mName:" + super.mName);
        super.mFatherName = getIntent().getStringExtra("fatherName");
        Log.d(TAG, "super.mFatherName:" + super.mFatherName);

        String Name = getStringFromName(mContext, "SunMiSubSimSignalActivity");
        if (!TextUtils.isEmpty(Name)) {
            super.mList = getFatherData(Name);
        }

        List<String> config = Const.getXmlConfig(this, Const.CONFIG_SUNMI_SIM_SIGNAL);
        List<TypeModel> list = getDatas(mContext, config, super.mList);
        if (list.size() > 10) mMore.setVisibility(View.VISIBLE);
        mAdapter.setData(list);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) mContext.finish();
        if (v == mMore) {
            if (isLayout) {
                isLayout = false;
                mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            } else {
                isLayout = true;
                mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
            mMore.setSelected(isLayout);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(int position) {
        if (!DataUtil.isFastClick()) {
            currPosition = position;
            Log.d(TAG, this.getLocalClassName() + " currPosition: " + currPosition);
            startActivity(mAdapter.getData().get(position));
        } else Log.d(TAG, this.getLocalClassName() + " click too fast.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1111 || resultCode == 1000) {
            if (data != null) {
                int results = data.getIntExtra("results", 0);
                Log.d(TAG, "test results:" + results);
                mAdapter.getData().get(currPosition).setType(results);
                mAdapter.notifyDataSetChanged();

            }
        }
    }
}
