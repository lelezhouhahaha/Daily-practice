package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.model.PersistResultModel;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;

public class MM2_FunctionActivity extends BaseActivity implements View.OnClickListener, CheckListAdapter.OnCallBackCheckFunction {
    private MM2_FunctionActivity mContext;
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

    private AlertDialog mAlertDialog = null;
    private boolean mStartFailTest = false;
    private int AUTO_TEST_FAIL_ITEM_COUNT = 2;
    private String AUTO_TEST_FAIL_ITEM_COUNT_TAG = "common_auto_test_fail_item_count";
    List<Integer> mFailItems = new ArrayList<>();
    private int failTestPosition = 0;
    private Boolean MMI2_FunctionTestFlag = true;
    private String projectName = "";
    String cit_result = "";
    private String TAG = "MM2_FunctionActivity";
    private WifiManager mWifimanager = null;

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
        mTitle.setText(R.string.mmi_two_test_auto);
        activeBT();
        activeWifi();
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        //if (!SystemProperties.get("persist.custmized.runin_result", "unknown").equals("true")) {
        if (!SystemProperties.get(OdmCustomedProp.getRuninResultProp(), "unknown").equals("true")) {
            showDialog(MyApplication.RuninTestNAME, MyApplication.MMI2_PreName);
            MMI2_FunctionTestFlag = false;
        }
        String face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
        if(projectName.equals("MT537")) {
            if (face_select == null || face_select.equals("") || face_select.length() != 8) {
                showDialog();
                MMI2_FunctionTestFlag = false;
            }
        }

        if (MMI2_FunctionTestFlag) {
            String failTestCount = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, AUTO_TEST_FAIL_ITEM_COUNT_TAG);
            if (failTestCount != null && !failTestCount.isEmpty()) {
                try {
                    AUTO_TEST_FAIL_ITEM_COUNT = Integer.parseInt(failTestCount);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "failTestCount=" + failTestCount);
                }

            }
            mDefaultPath = getResources().getString(R.string.mmi2_function_save_log_default_path);
            mFileName = getResources().getString(R.string.mmi2_function_save_log_file_name);
            isCustomPath = getResources().getBoolean(R.bool.mmi2_function_save_log_is_user_custom);
            mCustomPath = getResources().getString(R.string.mmi2_function_save_log_custom_path);
            Log.d(TAG, "mDefaultPath:" + mDefaultPath +
                    " mFileName:" + mFileName +
                    " isCustomPath:" + isCustomPath +
                    " mCustomPath:" + mCustomPath);

            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new CheckListAdapter(this);
            mRecyclerView.setAdapter(mAdapter);

            //super.mName = getIntent().getStringExtra("name");
            super.mName = getResources().getString(R.string.mmi_two_test_auto);
            Log.d(TAG, "super.mName:" + super.mName);
            super.mFatherName = getIntent().getStringExtra("fatherName");
            Log.d(TAG, "super.mFatherName:" + super.mFatherName);

            if (!TextUtils.isEmpty(super.mName)) {
                super.mList = getFatherData(super.mName);
            }

            List<String> config = Const.getXmlConfig(this, Const.CONFIG_MMI2_PRE);
            List<TypeModel> list = getDatas(mContext, config, super.mList);
            if(projectName.equals("MT537")) {
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
            mHandler.sendEmptyMessageDelayed(1005, 100);
        }
    }

    public void showDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        String info = getResources().getString(R.string.no_set_sku);
        builder.setMessage(info);
        builder.setCancelable(false);
        builder.setNegativeButton(getResources().getString(R.string.set_sku_go), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1003://test finish
                    String resultStr = "";
                    String title = getResources().getString(R.string.mmi2_pre_function_test_result);
                    boolean resultStatus = isAllSuccess();

                    if (!resultStatus && AUTO_TEST_FAIL_ITEM_COUNT > 0 && mFailItems.size() > 0) {
                        mStartFailTest = true;
                        mHandler.sendEmptyMessageDelayed(1006, 100);
                        return;
                    }

                    if (resultStatus)
                        resultStr = getResources().getString(R.string.success);
                    else resultStr = getResources().getString(R.string.fail);
                    AlertDialog alertDialog1 = new AlertDialog.Builder(mContext)
                            .setTitle(title)
                            .setMessage(resultStr)
                            .setIcon(R.mipmap.ic_launcher)
                            .create();
                    alertDialog1.show();
                    try {
                        Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
                        mAlert.setAccessible(true);
                        Object mAlertController = mAlert.get(alertDialog1);
                        Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
                        mMessage.setAccessible(true);
                        TextView mMessageView = (TextView) mMessage.get(mAlertController);
                        if (resultStatus) {
                            mMessageView.setTextColor(Color.GREEN);
                        } else mMessageView.setTextColor(Color.RED);
                        mMessageView.setGravity(Gravity.CENTER);
                        mMessageView.setTextSize(25);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    //saveLog();
                    new SaveResult().execute();
                    break;
                case 1004://save log
//                    initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
//                    saveLog();
                    break;
                case 1005:
                    startActivity(mAdapter.getData().get(currPosition));
                    break;
                case 1006:
                    startActivity(mAdapter.getData().get(mFailItems.get(failTestPosition)));
                    break;

            }
        }
    };

    private boolean isAllSuccess() {
        List<FunctionBean> list = getFatherData(MM2_FunctionActivity.super.mName);
        boolean isAllSuccess = true;
        for (FunctionBean bean : list) {
            switch (bean.getResults()) {
                case 0:
                    isAllSuccess = false;
                    break;
                case 1:
                    isAllSuccess = false;
                    break;
            }
        }
        return isAllSuccess;
    }

    private void saveLog() {
        List<FunctionBean> list = getFatherData(MM2_FunctionActivity.super.mName);
        List<PersistResultModel> persistResultList = new ArrayList<>();

        boolean isAllSuccess = true;
        for (FunctionBean bean : list) {
            PersistResultModel persistModel = new PersistResultModel();

            switch (bean.getResults()) {
                case 0:
                    persistModel.setResult(Const.RESULT_NOTEST);
                    isAllSuccess = false;
                    break;
                case 1:
                    persistModel.setResult(Const.RESULT_FAILURE);
                    isAllSuccess = false;
                    break;
                case 2:
                    persistModel.setResult(Const.RESULT_SUCCESS);
                    break;
            }
            persistModel.setName(bean.getSubclassName());
            persistResultList.add(persistModel);
        }

        cit_result = isAllSuccess ? "true" : "false";
        if (isAllSuccess) {
            Log.d(TAG,"MeigTest factorymode2_results set success 1:"+isAllSuccess);
            Settings.System.putInt(mContext.getContentResolver(), "key_system_factorymode2_results", 1);
        } else {
            Log.d(TAG,"MeigTest factorymode2_results set success 0:"+isAllSuccess);
            Settings.System.putInt(mContext.getContentResolver(), "key_system_factorymode2_results", 0);
        }
        PersistResultModel allResultModel = new PersistResultModel();
        allResultModel.setName("mmi2_function_all");
        allResultModel.setResult(isAllSuccess ? Const.RESULT_SUCCESS : Const.RESULT_FAILURE);
        persistResultList.add(allResultModel);
        Log.d(TAG, "Const.getLogPath(Const.TYPE_LOG_PATH_FILE):" + Const.getLogPath(Const.TYPE_LOG_PATH_FILE));
        writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE), Const.MMI2_FUNCTION_AUTO_RESULT_FILE, JSON.toJSONString(persistResultList));
        //SystemProperties.set("persist.custmized.cit2_result", cit_result);
        SystemProperties.set(OdmCustomedProp.getCit2ResultProp(), cit_result);
    }

    private boolean writePersistResult(String path, String fileName, String result) {
        File persistPath = new File(Const.getLogPath(Const.TYPE_LOG_PATH_DIR));
        if (persistPath.exists() && persistPath.isDirectory()) {
            File dir = FileUtil.mkDir(new File(path));
            Log.d("PreFunctionActivity", "pre writePersistResult  fileName:" + fileName + " result:" + result);
            return !"".equals(FileUtil.writeFile(dir, fileName, result));
        }
        return false;
    }

    private boolean writeSDCardResult(String path, String fileName, String result) {
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)) {
            File dir = FileUtil.mkDir(FileUtil.createRootDirectory(path));
            return !"".equals(FileUtil.writeFile(dir, fileName, result));
        }
        return false;
    }

    private class SaveResult extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            //<!-- modify for bug 25565 by huangqian,delete pre_function_test_result start.
            //createDialog();
            // modify for bug 25565 by huangqian,delete pre_function_test_result end. -->
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                saveLog();
                Thread.sleep(5000);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void e) {

            Log.d(TAG, "SaveResult -- >onPostExecute");
            //<!-- modify for bug 25565 by huangqian,delete pre_function_test_result start.
            //updateDialg();
            // modify for bug 25565 by huangqian,delete pre_function_test_result end. -->

        }

    }

    private void updateDialg() {
        String resultStr = "";
        boolean resultStatus = isAllSuccess();
        if (resultStatus)
            resultStr = getResources().getString(R.string.success);
        else resultStr = getResources().getString(R.string.fail);
        mAlertDialog.setCancelable(true);
        ProgressBar mProgressBar = (ProgressBar) mAlertDialog.findViewById(R.id.save_loading);
        TextView mMessageView = (TextView) mAlertDialog.findViewById(R.id.result_textView);
        TextView mSaveResult = (TextView) mAlertDialog.findViewById(R.id.save_result);
        mProgressBar.setVisibility(View.GONE);
        mMessageView.setVisibility(View.VISIBLE);
        mMessageView.setText(resultStr);
        mSaveResult.setText(R.string.save_finish);
        mSaveResult.setTextColor(Color.GREEN);
        if (resultStatus) {
            mMessageView.setTextColor(Color.GREEN);
        } else {
            mMessageView.setTextColor(Color.RED);
        }


    }

    private void createDialog() {
        String title = getResources().getString(R.string.pre_function_test_result);
        View view = View.inflate(getApplicationContext(), R.layout.dialog_result, null);
        mAlertDialog = new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setView(view)
                .create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.show();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
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
        //currPosition = position;
        //startActivity(mAdapter.getData().get(position));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
    }
    private void activeBT(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
    }

    private void activeWifi(){
        mWifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!mWifimanager.isWifiEnabled()) {
            mWifimanager.setWifiEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1111 || resultCode == 1000) {
            if (data != null) {
                int results = data.getIntExtra("results", 0);
                Log.d(TAG, "test results:" + results);


                if (!mStartFailTest && results != SUCCESS) {
                    mFailItems.add(currPosition);
                }
                if (mStartFailTest) {
                    mAdapter.getData().get(mFailItems.get(failTestPosition)).setType(results);
                    mAdapter.notifyDataSetChanged();
                    if (results == SUCCESS) {
                        mFailItems.remove(failTestPosition);
                    } else {
                        failTestPosition++;
                    }
                    if (mFailItems.size() <= failTestPosition) {
                        failTestPosition = 0;
                        AUTO_TEST_FAIL_ITEM_COUNT--;
                    }
                } else {
                    mAdapter.getData().get(currPosition).setType(results);
                    mAdapter.notifyDataSetChanged();
                    currPosition++;
                }

                //if ( currPosition == Const.preFunctionList.length ){
                if (currPosition == mAdapter.getItemCount()) {
                    Log.d(TAG, "currPosition :" + currPosition);
                    Log.d(TAG, "mAdapter.getItemCount():" + mAdapter.getItemCount());
                    mHandler.sendEmptyMessageDelayed(1003, Const.DELAY_TIME);
                    // mHandler.sendEmptyMessageDelayed(1004,Const.DELAY_TIME);
                    return;
                }
                Log.d(TAG, "mAdapter.getItemCount():" + mAdapter.getItemCount());
                /*Message msg = mHandler.obtainMessage();
                msg.what = 1004;
                mHandler.sendMessage(msg);*/
                mHandler.sendEmptyMessageDelayed(1005, Const.DELAY_TIME);
            }
        }
    }
}
