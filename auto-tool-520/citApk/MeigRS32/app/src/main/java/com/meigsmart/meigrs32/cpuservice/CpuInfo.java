package com.meigsmart.meigrs32.cpuservice;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.util.DataUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by chenMeng on 2018/4/8.
 */
public class CpuInfo {

    private Context mContext;
    private View mDecor;
    private WindowManager.LayoutParams mDecorLayoutParams;
    private boolean mIsShowing = false;
    private TextView mLevel;
    private TextView mState;
    private TextView mCpuNmae;
    private WindowManager mWindowManager;

    public CpuInfo(Context paramContext) {
        this.mContext = paramContext;
        initFloatingWindowLayout();
        initFloatingWindow();
    }

    private void initContentView(View paramView) {
        this.mState = (TextView) paramView.findViewById(R.id.status);
        this.mLevel = (TextView) paramView.findViewById(R.id.level);
        this.mCpuNmae = (TextView) paramView.findViewById(R.id.cpuname);
        this.mCpuNmae.setText(DataUtil.getCpuName());
    }

    private void initFloatingWindow() {
        this.mWindowManager = ((WindowManager)this.mContext.getSystemService(Context.WINDOW_SERVICE));
        this.mDecor = LayoutInflater.from(mContext).inflate(R.layout.cpu_info,null);
        initContentView(this.mDecor);
    }

    private void initFloatingWindowLayout() {
        this.mDecorLayoutParams = new WindowManager.LayoutParams();
        WindowManager.LayoutParams localLayoutParams = this.mDecorLayoutParams;
        localLayoutParams.gravity = Gravity.CENTER;
        localLayoutParams.width = -2;
        localLayoutParams.height = -2;
        localLayoutParams.x = 0;
        localLayoutParams.y = 0;
        localLayoutParams.format = -3;
        localLayoutParams.type = 2002;
        localLayoutParams.flags |= 0x20038;
        localLayoutParams.token = null;
    }

    public void hide() {
        if (this.mIsShowing) {
            this.mIsShowing = false;
            this.mWindowManager.removeView(this.mDecor);
        }
    }

    public void setText(String paramString) {
        this.mState.setText(paramString);
    }

    public void show() {
        if (!this.mIsShowing) {
            this.mIsShowing = true;
            this.mWindowManager.addView(this.mDecor, this.mDecorLayoutParams);
        }
    }

}
