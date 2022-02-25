package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.ContactsContract;
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
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.PersistResultModel;
import com.meigsmart.meigrs32.model.ResultModel;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.util.ToastUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import butterknife.BindView;

public class PreFunctionActivity extends BaseActivity implements View.OnClickListener , CheckListAdapter.OnCallBackCheckFunction {
    private PreFunctionActivity mContext;
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
    private boolean isLayout = true;//true linearLayout  ;false gridLayout

    private String mDefaultPath;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mFileName ;

	private AlertDialog mAlertDialog = null;
    private boolean mStartFailTest = false;
    private int AUTO_TEST_FAIL_ITEM_COUNT = 2;
    private String  AUTO_TEST_FAIL_ITEM_COUNT_TAG = "common_auto_test_fail_item_count";
    List<Integer> mFailItems = new ArrayList<>();
    private int failTestPosition = 0;
    private Boolean PreFunctionTestFlag = true;
    private String projectName = "";
    String PreFunction_step ="";
    String cit_result = "";
    private String scanType = "";
    private boolean OemScanConnected = false;

    private static int moreClickTimes;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_pre_function;
    }

    @Override
    protected void initData() {
        mContext = this;
        moreClickTimes =0;
        super.startBlockKeys = false;
        //mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mMore.setSelected(isLayout);
        mTitle.setText(R.string.pre_function_title);
        String buildType = SystemProperties.get("ro.build.type", "unknown");
        LogUtil.d("PreFunctionActivity buildType:" + buildType);
        if(buildType.equals("userdebug")) {
            //if(!SystemProperties.get("persist.custmized.pcba_result", "unknown").equals("true")) {
            if(!SystemProperties.get(OdmCustomedProp.getPCBAResultProp(), "unknown").equals("true")) {
                showDialog(MyApplication.PCBANAME, MyApplication.PreNAME);
                PreFunctionTestFlag = false;
            }
        }else{
            //if(!SystemProperties.get("persist.custmized.runin_result", "unknown").equals("true")) {
            if(!SystemProperties.get(OdmCustomedProp.getRuninResultProp(), "unknown").equals("true")) {
                showDialog(MyApplication.RuninTestNAME, MyApplication.PreNAME);
                PreFunctionTestFlag = false;
            }
        }
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        if("MT537".equals(projectName)) {
            //if (SystemProperties.get("persist.custmized.face_select") == null || SystemProperties.get("persist.custmized.face_select").equals("")) {
            if (SystemProperties.get(OdmCustomedProp.getFaceSelectProp()) == null || SystemProperties.get(OdmCustomedProp.getFaceSelectProp()).equals("")) {
                showDialog();
            }
        }

        if(PreFunctionTestFlag) {
            String failTestCount = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, AUTO_TEST_FAIL_ITEM_COUNT_TAG);
            if (failTestCount != null && !failTestCount.isEmpty()) {
                try {
                    AUTO_TEST_FAIL_ITEM_COUNT = Integer.parseInt(failTestCount);
                } catch (NumberFormatException e) {
                    LogUtil.e("PreFunctionActivity", "failTestCount=" + failTestCount);
                }

            }

            mDefaultPath = getResources().getString(R.string.pre_function_save_log_default_path);
            mFileName = getResources().getString(R.string.pre_function_save_log_file_name);
            isCustomPath = getResources().getBoolean(R.bool.pre_function_save_log_is_user_custom);
            mCustomPath = getResources().getString(R.string.pre_function_save_log_custom_path);
            LogUtil.d("mDefaultPath:" + mDefaultPath +
                    " mFileName:" + mFileName +
                    " isCustomPath:" + isCustomPath +
                    " mCustomPath:" + mCustomPath);

            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new CheckListAdapter(this);
            mRecyclerView.setAdapter(mAdapter);

            super.mName = getIntent().getStringExtra("name");
            LogUtil.d("citapk super.mName:" + super.mName);
            super.mFatherName = getIntent().getStringExtra("fatherName");
            LogUtil.d("citapk super.mFatherName:" + super.mFatherName);

            if (!TextUtils.isEmpty(super.mName)) {
                super.mList = getFatherData(super.mName);
            }

            List<String> config = Const.getXmlConfig(this, Const.CONFIG_PRE_FUNCTION);
            List<TypeModel> list = getDatas(mContext, config, super.mList);
            if (list.size() > 10) mMore.setVisibility(View.VISIBLE);
            mAdapter.setData(list);
            //ToastUtil.showBottomLong(getResources().getString(R.string.start_tag));
            if ("MT537".equals(projectName)) {
                //if ((SystemProperties.get("persist.custmized.face_select") != null && !SystemProperties.get("persist.custmized.face_select").equals("")))
                if ((SystemProperties.get(OdmCustomedProp.getFaceSelectProp()) != null && !SystemProperties.get(OdmCustomedProp.getFaceSelectProp()).equals("")))
                {
                    mHandler.sendEmptyMessageDelayed(1005, 100);
                }else{
                }
            }else{
                mHandler.sendEmptyMessageDelayed(1005, 100);
            }
        }
    }

    public void showDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        String info = getResources().getString(R.string.structured_light_select);
        builder.setMessage(info);
        builder.setCancelable(false);
        builder.setNegativeButton(getResources().getString(R.string.structured_light_front), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //SystemProperties.set("persist.custmized.face_select","camera_front");
                SystemProperties.set(OdmCustomedProp.getFaceSelectProp(),"camera_front");
                mHandler.sendEmptyMessageDelayed(1005, 100);
            }
        });
        builder.setPositiveButton(getResources().getString(R.string.structured_light_rear), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //SystemProperties.set("persist.custmized.face_select","camera_rear");
                SystemProperties.set(OdmCustomedProp.getFaceSelectProp(),"camera_rear");
                mHandler.sendEmptyMessageDelayed(1005, 100);
            }
        });
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1003://test finish
                    //ToastUtil.showBottomShort(getResources().getString(R.string.pre_function_test_finish));
                    String resultStr = "";
                    String title = getResources().getString(R.string.pre_function_test_result);
                    boolean resultStatus = isAllSuccess();

                    if(!resultStatus && AUTO_TEST_FAIL_ITEM_COUNT > 0 && mFailItems.size() > 0) {
                        mStartFailTest = true;
                        mHandler.sendEmptyMessageDelayed(1006, 100);
                        return;
                    }

                    if(resultStatus)
                        resultStr = getResources().getString(R.string.success);
                    else resultStr = getResources().getString(R.string.fail);
//                    AlertDialog alertDialog1 = new AlertDialog.Builder(mContext)
//                            .setTitle(title)
//                            .setMessage(resultStr)
//                            .setIcon(R.mipmap.ic_launcher)
//                            .create();
//                    alertDialog1.show();
//                    try {
//                        Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
//                        mAlert.setAccessible(true);
//                        Object mAlertController = mAlert.get(alertDialog1);
//                        Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
//                        mMessage.setAccessible(true);
//                        TextView mMessageView = (TextView) mMessage.get(mAlertController);
//                        if(resultStatus) {
//                            mMessageView.setTextColor(Color.GREEN);
//                        }else mMessageView.setTextColor(Color.RED);
//                        mMessageView.setGravity(Gravity.CENTER);
//                        mMessageView.setTextSize(25);
//                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
//                    } catch (NoSuchFieldException e) {
//                        e.printStackTrace();
//                    }
                    //saveLog();
                    if(null != mBack)mBack.setVisibility(View.VISIBLE);
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
        List<FunctionBean> list = getFatherData(PreFunctionActivity.super.mName);
        boolean isAllSuccess = true;
        for (FunctionBean bean:list) {
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
        List<FunctionBean> list = getFatherData(PreFunctionActivity.super.mName);
        List<PersistResultModel> persistResultList = new ArrayList<>();

        boolean isAllSuccess = true;
        for (FunctionBean bean:list){
            PersistResultModel persistModel = new PersistResultModel();

            switch (bean.getResults()){
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

        cit_result = isAllSuccess?"true":"false";
        String buildType = SystemProperties.get("ro.build.type", "unknown");
        LogUtil.d("PreFunctionActivity buildType:" + buildType);
        PreFunction_step= SystemProperties.get("persist.sys.key_system_factory_mode1","step0");
        if(!PreFunction_step.equals("")&&!PreFunction_step.isEmpty()&&PreFunction_step!=null){
            if(PreFunction_step.equals("step1")){
                if(isAllSuccess){
                    Settings.System.putInt(mContext.getContentResolver(),"key_system_factorymode1_results",1);
                }else{
                    Settings.System.putInt(mContext.getContentResolver(),"key_system_factorymode1_results",0);
                }
            }else if(PreFunction_step.equals("step2")){
                if(isAllSuccess){
                    Settings.System.putInt(mContext.getContentResolver(),"key_system_factorymode2_results",1);
                }else{
                    Settings.System.putInt(mContext.getContentResolver(),"key_system_factorymode2_results",0);
                }
            }
        }


        PersistResultModel allResultModel = new PersistResultModel();
        allResultModel.setName("pre_function_all");
        allResultModel.setResult(isAllSuccess ? Const.RESULT_SUCCESS : Const.RESULT_FAILURE);
        persistResultList.add(allResultModel);
        LogUtil.d("Const.getLogPath(Const.TYPE_LOG_PATH_FILE):" + Const.getLogPath(Const.TYPE_LOG_PATH_FILE));
        writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE),  Const.PRE_FUNCTION_AUTO_RESULT_FILE, JSON.toJSONString(persistResultList));
        if(!"MT537".equals(projectName)) {
        if(buildType.equals("userdebug")) {
            LogUtil.d("PreFunctionActivity userdebug");
            //SystemProperties.set("persist.custmized.cit1_result", cit_result);
            SystemProperties.set(OdmCustomedProp.getCit1ResultProp(), cit_result);
        }else{
            LogUtil.d("PreFunctionActivity not userdebug");
            //SystemProperties.set("persist.custmized.cit2_result", cit_result);
            SystemProperties.set(OdmCustomedProp.getCit2ResultProp(), cit_result);
        }
        }else{
            if(!PreFunction_step.equals("")&&!PreFunction_step.isEmpty()&&PreFunction_step!=null) {
                if (PreFunction_step.equals("step1")) {
                    //SystemProperties.set("persist.custmized.cit1_result", cit_result);
                    SystemProperties.set(OdmCustomedProp.getCit1ResultProp(), cit_result);
                }else if(PreFunction_step.equals("step2")){
                    //SystemProperties.set("persist.custmized.cit2_result", cit_result);
                    SystemProperties.set(OdmCustomedProp.getCit2ResultProp(), cit_result);
                }
            }
        }
    }

    private boolean writePersistResult(String path, String fileName, String result) {
        File persistPath = new File(Const.getLogPath(Const.TYPE_LOG_PATH_DIR));
        if(persistPath.exists() && persistPath.isDirectory()){
            File dir = FileUtil.mkDir(new File(path));
			Log.d("PreFunctionActivity","pre writePersistResult  fileName:" + fileName + " result:" + result);
            return !"".equals(FileUtil.writeFile(dir, fileName, result));
        }
        return false;
    }

    private boolean writeSDCardResult(String path, String fileName, String result){
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)) {
            File dir = FileUtil.mkDir(FileUtil.createRootDirectory(path));
            return !"".equals(FileUtil.writeFile(dir, fileName,result));
        }
        return false;
    }

        private class SaveResult extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
           createDialog();
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
    
            LogUtil.d("SaveResult -- >onPostExecute");
            updateDialg();
    
        }
    
    }

    private void updateDialg(){
        String resultStr = "";
        boolean resultStatus = isAllSuccess();
		if(resultStatus)
		resultStr = getResources().getString(R.string.success);
		else resultStr = getResources().getString(R.string.fail);
        mAlertDialog.setCancelable(true);
        ProgressBar mProgressBar = (ProgressBar)mAlertDialog.findViewById(R.id.save_loading);
        TextView mMessageView = (TextView) mAlertDialog.findViewById(R.id.result_textView);
        TextView mSaveResult = (TextView) mAlertDialog.findViewById(R.id.save_result);
        mProgressBar.setVisibility(View.GONE);
        mMessageView.setVisibility(View.VISIBLE);
        mMessageView.setText(resultStr);
        mSaveResult.setText(R.string.save_finish);
        mSaveResult.setTextColor(Color.GREEN);
        if(resultStatus) {
            mMessageView.setTextColor(Color.GREEN);
        }else{
            mMessageView.setTextColor(Color.RED);
        }

        
    }

    private void createDialog(){
        String title = getResources().getString(R.string.pre_function_test_result);
        View view = View.inflate(getApplicationContext(), R.layout.dialog_result, null);
        mAlertDialog = new AlertDialog.Builder(mContext)
                            .setTitle(title)
                            .setView(view)
                            .create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.show();

    }

//    private String initPath(String path, String fileName, String result){
//        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)){
//            File f = FileUtil.createRootDirectory(path);
//            File file = FileUtil.mkDir(f);
//            return FileUtil.writeFile(file,fileName,result);
//        }
//        return "";
//    }
//
//    private String createJsonResult(){
//        List<FunctionBean> list = getFatherData(super.mName);
//        List<ResultModel> resultList = new ArrayList<>();
//        for (FunctionBean bean:list){
//            ResultModel model = new ResultModel();
//            if (bean.getResults() == 0){
//                model.setResult(Const.RESULT_NOTEST);
//            } else if (bean.getResults() == 1){
//                model.setResult(Const.RESULT_FAILURE);
//            } else if (bean.getResults() == 2){
//                model.setResult(Const.RESULT_SUCCESS);
//            }
//            model.setFatherName(bean.getFatherName());
//            model.setSubName(bean.getSubclassName());
//            model.setReason(bean.getReason());
//            resultList.add(model);
//        }
//        return JSON.toJSONString(resultList);
//    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack)mContext.finish();
        if (v == mMore){
            if (isLayout){
                isLayout = false;
                mRecyclerView.setLayoutManager(new GridLayoutManager(this,2));
            }else {
                isLayout = true;
                mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
            mMore.setSelected(isLayout);
            mAdapter.notifyDataSetChanged();
            moreClickTimes++;
            LogUtil.d("click more:" + moreClickTimes);
            if(moreClickTimes >= 2){
                moreClickTimes =0;
                mBack.setVisibility(View.VISIBLE);
            }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1111 || resultCode == 1000){
            if (data!=null){
                int results = data.getIntExtra("results",0);
                LogUtil.d("test results:" + results);


                if(!mStartFailTest && results != SUCCESS ){
                    mFailItems.add(currPosition);
                }
                if(mStartFailTest){
                    mAdapter.getData().get(mFailItems.get(failTestPosition)).setType(results);
                    mAdapter.notifyDataSetChanged();
                    if(results == SUCCESS) {
                        mFailItems.remove(failTestPosition);
                    }else {
                        failTestPosition++;
                    }
                    if(mFailItems.size() <= failTestPosition ){
                        failTestPosition = 0;
                        AUTO_TEST_FAIL_ITEM_COUNT--;
                    }
                }else {
                    mAdapter.getData().get(currPosition).setType(results);
                    mAdapter.notifyDataSetChanged();
                    currPosition++;
                }

                //if ( currPosition == Const.preFunctionList.length ){
                if(currPosition == mAdapter.getItemCount()){
                    LogUtil.d("currPosition :" + currPosition);
                    LogUtil.d("mAdapter.getItemCount():" + mAdapter.getItemCount());
                    mHandler.sendEmptyMessageDelayed(1003,Const.DELAY_TIME);
                   // mHandler.sendEmptyMessageDelayed(1004,Const.DELAY_TIME);
                    return;
                }
                LogUtil.d("mAdapter.getItemCount():" + mAdapter.getItemCount());
                /*Message msg = mHandler.obtainMessage();
                msg.what = 1004;
                mHandler.sendMessage(msg);*/
                mHandler.sendEmptyMessageDelayed(1005,Const.DELAY_TIME);
            }
        }
    }
}
