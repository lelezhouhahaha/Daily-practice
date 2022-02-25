package com.meigsmart.meigrs32.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;

public class SoftDialog extends Dialog  {
    private TextView mName;
    private TextView mSure;
    private OnSoftCallBack mCallBack;

    public void setmCallBack(OnSoftCallBack mCallBack) {
        this.mCallBack = mCallBack;
    }

    public interface OnSoftCallBack{
        void onClickSure();
    }

    public SoftDialog(Context context) {
        super(context);
    }

    public SoftDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    protected SoftDialog(Context context, boolean cancelable, Message cancelCallback) {
        super(context, cancelable, cancelCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        setContentView(R.layout.soft_dialog_layout);
        setCancelable(false);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.CENTER;
        getWindow().setAttributes(lp);

        mName = (TextView) findViewById(R.id.name);
        mSure = (TextView) findViewById(R.id.sure);

        mSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallBack!=null)mCallBack.onClickSure();
            }
        });
    }

    public void setContentTitle(String title){
        if (TextUtils.isEmpty(title)){
            return;
        }
        mName.setText(title);
    }

}
