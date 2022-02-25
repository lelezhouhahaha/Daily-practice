package com.meigsmart.meigrs32.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.model.TypeModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.meigsmart.meigrs32.util.*;
import com.meigsmart.meigrs32.config.*;

/**
 * Created by chenMeng on 2018/4/26.
 */
public class CheckListAdapter extends RecyclerView.Adapter<CheckListAdapter.Holder> {
    private List<TypeModel> mList = new ArrayList<>();
    private OnCallBackCheckFunction mCallBack;
    private boolean isMT537 = false;

    public CheckListAdapter(OnCallBackCheckFunction callBack) {
        this.mCallBack = callBack;
        isMT537 = "MT537".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK));
    }

    public interface OnCallBackCheckFunction {
        void onItemClick(int position);
    }

    public void setData(List<TypeModel> list) {
        this.mList = list;
        this.notifyDataSetChanged();
    }

    public List<TypeModel> getData() {
        return this.mList;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if(isMT537) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.large_pcba_list_item, null);
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pcba_list_item, null);
        }
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.initData(position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        @BindView(R.id.layout)
        public RelativeLayout mLayout;
        @BindView(R.id.name)
        public TextView mName;
        @BindView(R.id.img)
        public CheckBox mImg;

        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void initData(final int position) {
            TypeModel model = mList.get(position);

            mName.setText(model.getName());

            if (model.getType() == 1) {//fail
                mImg.setChecked(false);
                mImg.setSelected(true);
            } else if (model.getType() == 2) {//pass
                mImg.setSelected(false);
                mImg.setChecked(true);
            } else {//unTest
                mImg.setSelected(false);
                mImg.setChecked(false);
            }

            mLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallBack!=null)mCallBack.onItemClick(position);
                }
            });

        }
    }
}
