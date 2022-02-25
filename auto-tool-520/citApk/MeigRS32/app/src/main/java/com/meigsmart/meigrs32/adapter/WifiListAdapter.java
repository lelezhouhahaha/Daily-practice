package com.meigsmart.meigrs32.adapter;

import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by chenMeng on 2018/5/10.
 */
public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.Holder> {
    private List<ScanResult> mList = new ArrayList<>();


    public void setData(List<ScanResult> list) {
        this.mList = list;
        this.notifyDataSetChanged();
    }

    public List<ScanResult> getData() {
        return this.mList;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.function_list_item,parent,false);
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

    public class Holder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)
        public TextView mName;

        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void initData(final int position) {
            ScanResult model = mList.get(position);
            mName.setText(TextUtils.isEmpty(model.SSID) ? "" : model.SSID + "\n" + (TextUtils.isEmpty(model.BSSID) ? "" : model.BSSID));

        }
    }
}
