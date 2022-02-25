package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.os.SystemProperties;
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
import com.meigsmart.meigrs32.util.FileUtil;

import java.util.Iterator;
import java.util.List;

import butterknife.BindView;

public class MMI2_FunctionSignalActivity extends BaseActivity implements View.OnClickListener, CheckListAdapter.OnCallBackCheckFunction {
    private MMI2_FunctionSignalActivity mContext;
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
    private String mDefaultPath;
    private boolean isCustomPath;
    private String mCustomPath;
    private String mFileName;
    private String projectName = "";
    private String TAG = "MMI2_FunctionSignalActivity";

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
        mTitle.setText(R.string.mmi_two_test_manual);

        mDefaultPath = getResources().getString(R.string.mmi2_function_signal_save_log_default_path);
        mFileName = getResources().getString(R.string.mmi2_function_signal_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.mmi2_function_signal_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.mmi2_function_signal_save_log_custom_path);
        Log.d(TAG, "mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName +
                " isCustomPath:" + isCustomPath +
                " mCustomPath:" + mCustomPath);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getIntent().getStringExtra("name");
        Log.d(TAG, "super.mName:" + super.mName);
        super.mFatherName = getIntent().getStringExtra("fatherName");
        Log.d(TAG, "super.mFatherName:" + super.mFatherName);

        String Name = getStringFromName(mContext, getResources().getString(R.string.MM2_FunctionActivity));
        if (!TextUtils.isEmpty(Name)) {
            super.mList = getFatherData(Name);
        }

        List<String> config = Const.getXmlConfig(this, Const.CONFIG_MMI2_PRE_SIGNAL);
        List<TypeModel> list = getDatas(mContext, config, super.mList);
        if(projectName.equals("MT537")) {
            String face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
            if ((face_select != null) && (!face_select.equals("")) && (face_select.length() == 8)) {

                Iterator<TypeModel> iterator = list.iterator();
                while (iterator.hasNext()) {
                    TypeModel item = iterator.next();
                    if (face_select.substring(2, 3).equals("1")) {
                        if (item.getName().equals(getResources().getString(R.string.FrontCameraAutoActivity))
                                || item.getName().equals(getResources().getString(R.string.Scan_Test_537_far))) {
                            iterator.remove();
                            continue;
                        }
                    } else if (face_select.substring(2, 3).equals("0")) {
                        if (item.getName().equals(getResources().getString(R.string.ThreeM_CameraAutoActivity))) {
                            iterator.remove();
                            continue;
                        }
                    }

                    if (face_select.substring(3, 4).equals("1")) {
                        if (item.getName().equals(getResources().getString(R.string.SunMi_FlashLightActivity))
                                || item.getName().equals(getResources().getString(R.string.Scan_Test_537_near))) {
                            iterator.remove();
                            continue;
                        }
                    }

                    if (!face_select.substring(6, 7).equals("1")) {
                        if (item.getName().equals(getResources().getString(R.string.FingerTestSunMiActivity))) {
                            iterator.remove();
                            continue;
                        }
                    }

                    if (!face_select.substring(5, 6).equals("1")) {
                        if (item.getName().equals(getResources().getString(R.string.Id_Test))) {
                            iterator.remove();
                        }
                    }
                }

                /**
                if (face_select.substring(2, 3).equals("1")) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.FrontCameraAutoActivity)) ||
                                list.get(i).getName().equals(getResources().getString(R.string.Scan_Test_537_far))) {
                            list.remove(i);
                        }
                    }
                }
                if (face_select.substring(2, 3).equals("0")) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.ThreeM_CameraAutoActivity))) {
                            list.remove(i);
                        }
                    }
                }
                if (face_select.substring(3, 4).equals("1")) {
                    for (int i = 0; i < list.size(); i++) {
                        Log.d("MM0716", "list.get(i).getName():" + list.get(i).getName());
                        if (list.get(i).getName().equals(getResources().getString(R.string.SunMi_FlashLightActivity)) ||
                                list.get(i).getName().equals(getResources().getString(R.string.Scan_Test_537_near))) {

                            list.remove(i);
                        }
                    }
                }
                if (!face_select.substring(6, 7).equals("1")) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.FingerTestSunMiActivity))) {
                            list.remove(i);
                        }
                    }
                }
                if (!face_select.substring(5, 6).equals("1")) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.Id_Test))) {
                            list.remove(i);
                        }
                    }
                }
                 */
            } else {
                Iterator<TypeModel> iterator = list.iterator();
                while (iterator.hasNext()) {
                    TypeModel item = iterator.next();
                    if (item.getName().equals(getResources().getString(R.string.ThreeM_CameraAutoActivity))
                            || item.getName().equals(getResources().getString(R.string.FingerTestSunMiActivity))
                            || item.getName().equals(getResources().getString(R.string.Id_Test))) {
                        iterator.remove();
                    }
                }

                /**
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getName().equals(getResources().getString(R.string.ThreeM_CameraAutoActivity)) ||
                            list.get(i).getName().equals(getResources().getString(R.string.FingerTestSunMiActivity)) ||
                            list.get(i).getName().equals(getResources().getString(R.string.Id_Test))) {
                        list.remove(i);
                    }
                }
                 */
            }
        }
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
