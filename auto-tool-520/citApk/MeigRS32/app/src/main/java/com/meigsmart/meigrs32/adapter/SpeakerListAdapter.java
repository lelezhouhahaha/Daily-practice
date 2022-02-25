package com.meigsmart.meigrs32.adapter;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by chenMeng on 2018/5/10.
 */
public class SpeakerListAdapter extends RecyclerView.Adapter<SpeakerListAdapter.Holder>{
    private List<TypeModel> mList = new ArrayList<>();
    private OnSpeakerSound mCallBack;

    public SpeakerListAdapter(OnSpeakerSound callBack){
        this.mCallBack = callBack;
    }

    public interface OnSpeakerSound{
        void onSpeakerItemListener(int pos);
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.speaker_list_item,null);
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

    public class Holder extends RecyclerView.ViewHolder{
        @BindView(R.id.name)
        public Button mName;
        @BindView(R.id.img)
        public ImageView img;
        @BindView(R.id.layout)
        public RelativeLayout layout;

        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

        public void initData(final int position){
            TypeModel model = mList.get(position);
            mName.setText(model.getName());
			img.setVisibility(View.GONE);

            if (model.getType() == 0){
				mName.setBackgroundColor(Color.GRAY);
                //img.setSelected(false);
            }else if (model.getType() == 1){
				mName.setBackgroundColor(Color.BLUE);
                //img.setSelected(true);
            }

            //add by zhaohairuo for zenbug 1085 @2019-05-07 start
            String mscreen = "common_config_screen_size_speaker_adapter";
            String strTmp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH,mscreen );
            if (strTmp.equals("0")){
                ViewGroup.LayoutParams params = layout.getLayoutParams();
                params.height = 60;
                layout.setLayoutParams(params);
            }
            //add by zhaohairuo for zenbug 1085 @2019-05-07 end

            mName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mCallBack!=null)mCallBack.onSpeakerItemListener(position);
                }
            });

        }
    }
}
