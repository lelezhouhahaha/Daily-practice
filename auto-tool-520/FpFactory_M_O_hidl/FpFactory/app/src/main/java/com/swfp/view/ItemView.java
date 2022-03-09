package com.swfp.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.swfp.factory.R;


/**
 * Created by zhouj on 2016/12/9.
 */

public class ItemView extends RelativeLayout {

    private static final String TAG = "sw-ItemView";

    private static final String KEY_TITLE = "title";
    private static final String KEY_SUMMARY = "summary";
    private static final String KEY_STATE = "state";
    private static final String KEY_ORIGIN = "ItemView";


    private TextView mTvTitle, mTvSummary, mTvResult;
    private int colorPass, colorFail, colorNull;
    private String summary, title;

    public enum State {NULL, PASS, FAIL, NOFUN}

    private State mState = State.NULL;


    public ItemView(Context context) {
        this(context, null);
    }

    public ItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        RelativeLayout container = (RelativeLayout) View.inflate(context, R.layout.layout_item, this);
        setWillNotDraw(false);
        mTvTitle = (TextView) container.findViewById(R.id.title);
        mTvSummary = (TextView) container.findViewById(R.id.summary);
        mTvResult = (TextView) container.findViewById(R.id.result);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ItemView);
        title = ta.getString(R.styleable.ItemView_itv_title);
        summary = ta.getString(R.styleable.ItemView_itv_summary);
        colorFail = ta.getColor(R.styleable.ItemView_itv_fcolor, context.getResources().getColor(R.color.colorFail));
        colorPass = ta.getColor(R.styleable.ItemView_itv_pcolor, context.getResources().getColor(R.color.colorPass));
        colorNull = ta.getColor(R.styleable.ItemView_itv_ncolor, context.getResources().getColor(R.color.colorNull));
        State state = int2State(ta.getInt(R.styleable.ItemView_itv_state, 0));
        ta.recycle();

        mTvTitle.setText(title);
        mTvSummary.setText(summary);
        setResult(state);

    }

    public void setResult(State state) {
        mState = state;
        if (mState == State.FAIL) {
            mTvResult.setTextColor(colorFail);
            mTvResult.setText(R.string.text_fail);
        } else if (mState == State.NULL) {
            mTvResult.setTextColor(colorNull);
            mTvResult.setText(R.string.text_null);
        } else if (mState == State.PASS) {
            mTvResult.setTextColor(colorPass);
            mTvResult.setText(R.string.text_pass);
        } else if (mState == State.NOFUN) {
            mTvResult.setTextColor(colorNull);
            mTvResult.setText(R.string.text_nofun);
        }
    }

    public void setResult(State state, String value) {
        mState = state;
        if (mState == State.FAIL) {
            mTvResult.setTextColor(colorFail);
            mTvResult.setText(value);
        } else if (mState == State.NULL) {
            mTvResult.setTextColor(colorNull);
            mTvResult.setText(value);
        } else if (mState == State.PASS) {
            mTvResult.setTextColor(colorPass);
            mTvResult.setText(value);
        }else if (mState == State.NOFUN) {
            mTvResult.setTextColor(colorNull);
            mTvResult.setText(value);
        }
    }

    private State int2State(int value) {
        switch (value) {
            case -2:
                return State.NOFUN;
            case -1:
                return State.FAIL;
            case 0:
                return State.NULL;
            case 1:
                return State.PASS;
            default:
                throw new RuntimeException("int2State value is out of range[-1, 0, 1]");
        }
    }

    private int state2Int(State value) {
        if (value == State.NOFUN) {
            return -2;
        } else if (value == State.FAIL) {
            return -1;
        } else if (value == State.NULL) {
            return 0;
        } else if (value == State.PASS) {
            return 1;
        } else {
            throw new RuntimeException("state2Int State in unknow");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.GRAY);
    }


    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_STATE, state2Int(mState));
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_SUMMARY, summary);
        bundle.putParcelable(KEY_ORIGIN, super.onSaveInstanceState());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mState = int2State(bundle.getInt(KEY_STATE));
            title = bundle.getString(KEY_TITLE);
            summary = bundle.getString(KEY_SUMMARY);
            super.onRestoreInstanceState(bundle.getParcelable(KEY_ORIGIN));
        } else {
            super.onRestoreInstanceState(state);
        }
        mTvTitle.setText(title);
        mTvSummary.setText(summary);
        setResult(mState);
    }
}
