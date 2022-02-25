package com.meigsmart.meigrs32.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;

/**
 * Created by chenMeng on 2018/4/26.
 */
public class PromptDialog extends Dialog implements View.OnClickListener{
    private TextView mTitle;
    private TextView mUnTest;
    private TextView mFail;
    private TextView mPass;
    private OnPromptDialogCallBack mCallBack;
    private boolean isSuccess;

    public void setSuccess() {
        isSuccess = true;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public void setCallBack(OnPromptDialogCallBack callBack){
        this.mCallBack = callBack;
    }

    public interface OnPromptDialogCallBack{
        void onResultListener(int result);
    }

    public PromptDialog(@NonNull Context context) {
        super(context);
    }

    public PromptDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected PromptDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        setContentView(R.layout.prompt_dialog);
        setCancelable(false);
        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                return;
            }
        });

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.CENTER;
        getWindow().setAttributes(lp);

        mTitle = (TextView) findViewById(R.id.dialogTitle);
        mUnTest = (TextView) findViewById(R.id.unTest);
        mFail = (TextView) findViewById(R.id.fail);
        mPass = (TextView) findViewById(R.id.pass);
        mUnTest.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mPass.setOnClickListener(this);
        if (isSuccess){
            mPass.setVisibility(View.VISIBLE);
        }else {
            mPass.setVisibility(View.GONE);
        }

    }

    public void setTitle(String title){
        if (!TextUtils.isEmpty(title))mTitle.setText(title);
    }

    @Override
    public void onClick(View v) {
        if (v == mUnTest){
            if (this.isShowing())dismiss();
            if (mCallBack!=null)mCallBack.onResultListener(0);
        }
        if (v == mFail){
            if (this.isShowing())dismiss();
            if (mCallBack!=null)mCallBack.onResultListener(1);
        }
        if (v == mPass){
            if (this.isShowing())dismiss();
            if (mCallBack!=null)mCallBack.onResultListener(2);
        }
    }
}
