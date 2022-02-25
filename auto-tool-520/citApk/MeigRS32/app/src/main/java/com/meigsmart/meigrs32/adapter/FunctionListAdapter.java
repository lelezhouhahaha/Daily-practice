package com.meigsmart.meigrs32.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.SystemProperties;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.model.TypeModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.meigsmart.meigrs32.util.*;
import com.meigsmart.meigrs32.config.*;

/**
 * Created by chenMeng on 2018/4/24.
 */
public class FunctionListAdapter extends RecyclerView.Adapter<FunctionListAdapter.Holder>{
    private List<TypeModel> mList = new ArrayList<>();
    private OnFunctionItemClick mCallBack;
    private boolean isMT537 = false;
    Context mContext;

    public FunctionListAdapter( OnFunctionItemClick callBack,Context context){
        this.mCallBack = callBack;
        mContext =context;
        isMT537 = "MT537".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK));
    }

    public interface OnFunctionItemClick{
        void onClickItem(int position);
    }

    public void setData(List<TypeModel> list){
        this.mList = list;
        this.notifyDataSetChanged();
    }

    public List<TypeModel> getData(){
        return this.mList;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if(isMT537) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.large_function_list_item, null);
        }else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.function_list_item, null);
        }
        Holder holder = new Holder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.initData(position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class Holder extends RecyclerView.ViewHolder{
        @BindView(R.id.layout)
        public RelativeLayout mLayout;
        @BindView(R.id.name)
        public TextView mName;

        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

        public void initData(final int position){
            TypeModel model = mList.get(position);
            mName.setText(model.getName());
            if(isMT537) {
                //if (SystemProperties.get("persist.custmized.pcba_result", "unknown").equals("true")) {
                if (SystemProperties.get(OdmCustomedProp.getPCBAResultProp(), "unknown").equals("true")) {
                    if (model.getName().equals(mContext.getResources().getString(R.string.PCBAActivity)) || model.getName().equals(mContext.getResources().getString(R.string.PCBASignalActivity))) {
                        mLayout.setBackgroundColor(Color.GREEN);
                    }
                }
                //if (SystemProperties.get("persist.custmized.pcba_result", "unknown").equals("false")) {
                if (SystemProperties.get(OdmCustomedProp.getPCBAResultProp(), "unknown").equals("false")) {
                    if (model.getName().equals(mContext.getResources().getString(R.string.PCBAActivity)) || model.getName().equals(mContext.getResources().getString(R.string.PCBASignalActivity))) {
                        mLayout.setBackgroundColor(Color.RED);
                    }
                }
                //if (SystemProperties.get("persist.custmized.cit1_result", "unknown").equals("true")) {
                if (SystemProperties.get(OdmCustomedProp.getCit1ResultProp(), "unknown").equals("true")) {
                    if (model.getName().equals(mContext.getResources().getString(R.string.MM1_FunctionActivity)) || model.getName().equals(mContext.getResources().getString(R.string.MMI1_FunctionSignalActivity))) {
                        mLayout.setBackgroundColor(Color.GREEN);
                    }
                }
                //if (SystemProperties.get("persist.custmized.cit1_result", "unknown").equals("false")) {
                if (SystemProperties.get(OdmCustomedProp.getCit1ResultProp(), "unknown").equals("false")) {
                    if (model.getName().equals(mContext.getResources().getString(R.string.MM1_FunctionActivity)) || model.getName().equals(mContext.getResources().getString(R.string.MMI1_FunctionSignalActivity))) {
                        mLayout.setBackgroundColor(Color.RED);
                    }
                }
                //if (SystemProperties.get("persist.custmized.runin_result", "unknown").equals("true")) {
                if (SystemProperties.get(OdmCustomedProp.getRuninResultProp(), "unknown").equals("true")) {
                    if (model.getName().equals(mContext.getResources().getString(R.string.RunInActivity))) {
                        mLayout.setBackgroundColor(Color.GREEN);
                    }
                }
                //if (SystemProperties.get("persist.custmized.runin_result", "unknown").equals("false")) {
                if (SystemProperties.get(OdmCustomedProp.getRuninResultProp(), "unknown").equals("false")) {
                    if (model.getName().equals(mContext.getResources().getString(R.string.RunInActivity))) {
                        mLayout.setBackgroundColor(Color.RED);
                    }
                }
                //if (SystemProperties.get("persist.custmized.cit2_result", "unknown").equals("true")) {
                if (SystemProperties.get(OdmCustomedProp.getCit2ResultProp(), "unknown").equals("true")) {
                    if (model.getName().equals(mContext.getResources().getString(R.string.MM2_FunctionActivity)) || model.getName().equals(mContext.getResources().getString(R.string.MMI2_FunctionSignalActivity))) {
                        mLayout.setBackgroundColor(Color.GREEN);
                    }
                }
                //if (SystemProperties.get("persist.custmized.cit2_result", "unknown").equals("false")) {
                if (SystemProperties.get(OdmCustomedProp.getCit2ResultProp(), "unknown").equals("false")) {
                    if (model.getName().equals(mContext.getResources().getString(R.string.MM2_FunctionActivity)) || model.getName().equals(mContext.getResources().getString(R.string.MMI2_FunctionSignalActivity))) {
                        mLayout.setBackgroundColor(Color.RED);
                    }
                }
            }

            mLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallBack!=null)mCallBack.onClickItem(position);
                }
            });
        }
    }
}
