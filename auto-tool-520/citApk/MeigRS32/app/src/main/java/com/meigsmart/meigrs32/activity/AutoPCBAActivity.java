package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class AutoPCBAActivity extends BaseActivity implements View.OnClickListener , CheckListAdapter.OnCallBackCheckFunction {
    private AutoPCBAActivity mContext;
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

    private List<String> config_list = new ArrayList<>();

    private boolean mNWGpioFlag = false;
    private final String W_IO_PARTITION_GPIO_FLAG_KEY = "common_nw_iopartition_gpio_flag";
    private final String WRITE_IO_PARTITION_GPIO_PATH_KEY = "common_write_iopartition_gpio_path";
    private final String WRITE_IO_PARTITION_GPIO_FLAG_KEY = "common_write_iopartition_gpio_flag";
    private String mIopartition = "/sys/iopartition/iopartition";
    private boolean mNeedWriteGpioFlag = false;
    private static final String GPIO_SUPPORT_PROP_KEY = "common_cit_gpio_support_prop";
    private String gpio_support_prop = "ro.boot.gpiotestflag";
    private String projectName = "";
    private String scanType = "";
    private boolean OemScanConnected = false;
    private static int moreClickTimes;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_pcba;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = true;
        moreClickTimes =0;
       // mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mMore.setSelected(isLayout);
        mTitle.setText(R.string.function_pcba);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

        String failTestCount =  DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, AUTO_TEST_FAIL_ITEM_COUNT_TAG);
        if(failTestCount != null && !failTestCount.isEmpty()){
            try {
                AUTO_TEST_FAIL_ITEM_COUNT = Integer.parseInt(failTestCount);
            }catch(NumberFormatException e){
                LogUtil.e("PCBActivity","failTestCount="+failTestCount);
            }

        }

        mDefaultPath = getResources().getString(R.string.pcba_save_log_default_path);
        mFileName = getResources().getString(R.string.pcba_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.pcba_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.pcba_save_log_custom_path);
        LogUtil.d("mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);


        mNWGpioFlag = "true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, W_IO_PARTITION_GPIO_FLAG_KEY));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getIntent().getStringExtra("name");
        LogUtil.d("citapk super.mName:" + super.mName);
        super.mFatherName = getIntent().getStringExtra("fatherName");
        LogUtil.d("citapk super.mFatherName:" + super.mFatherName);

        if (!TextUtils.isEmpty(super.mName)){
            super.mList = getFatherData(super.mName);
            LogUtil.d("citapk super.mList:" + super.mList);
        }

        List<String> config = Const.getXmlConfig(this,Const.CONFIG_PCBA);
        /*jicong.wang add for task 5845 start {@ */
        if (CustomConfig.isSLB761X()){
            config = CustomConfig.SLB761XtestItemConfig(config);
        }
        /*jicong.wang add for task 5845 end @*/
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        String face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
        if("MT537".equals(projectName)){
            if((face_select!=null)&&(!face_select.equals(""))&&(face_select.length()==8)) {
                if(!face_select.substring(6, 7).equals("1")) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.FingerOtgActivity))) {
                            list.remove(i);
                        }
                    }
                }
                if(!face_select.substring(5, 6).equals("1")) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.Id_Test))) {
                            list.remove(i);
                        }
                    }
                }
            }
        }
        if (list.size()>10)mMore.setVisibility(View.VISIBLE);
        mAdapter.setData(list);
        for (TypeModel m:list) {
            config_list.add(m.getName());
        }
        config_list.add("pcba_all");
        //ToastUtil.showBottomLong(getResources().getString(R.string.start_tag));
        mHandler.sendEmptyMessageDelayed(1005,100);
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WRITE_IO_PARTITION_GPIO_PATH_KEY);
        if (!TextUtils.isEmpty(temp))
            mIopartition = temp;
        mNeedWriteGpioFlag = "true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WRITE_IO_PARTITION_GPIO_FLAG_KEY));
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1003://test finish
                    if(AUTO_TEST_FAIL_ITEM_COUNT > 0 && mFailItems.size() > 0) {
                        mStartFailTest = true;
                        mHandler.sendEmptyMessageDelayed(1006, 100);
                        return;
                    }

//                    initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
                    //mContext.finish();
                    //saveLog();
                    if(null != mBack)mBack.setVisibility(View.VISIBLE);
                    new SaveResult().execute();
                    break;
                case 1004://save log
//                    initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
//                    saveLog(false);
                    break;
                case 1005:
                    if(currPosition != mAdapter.getItemCount()) {
                        startActivity(mAdapter.getData().get(currPosition));
                    }
                    break;
                case 1006:
                    startActivity(mAdapter.getData().get(mFailItems.get(failTestPosition)));
                    break;
                case 2001:
                    //modify by wangjinfeng for task 8863
                    if(mNWGpioFlag) {
                        String sysGpioFlag = "sys.gpiotest.restore";
                        SystemProperties.set(sysGpioFlag, "0");
                    }else {
                        FileUtil.writeToFile(mIopartition, "gpiotest#0");
                    }
                    sendEmptyMessageDelayed(2002, 5000);
                    break;
                case 2002:
                    //add by wangjinfeng for bugid 17273
                    if(mNWGpioFlag) {
                        updateDialogGpioInfo("gpiotest#0".equals(SystemProperties.get("sys.gpiotest.restore.flag")));
                    }else {
                        String value = FileUtil.readFile(mIopartition);
                        updateDialogGpioInfo(value.contains("gpiotest#0"));
                    }
                    break;
            }
        }
    };

    private boolean isAllSuccess() {
        List<FunctionBean> list = getFatherData(AutoPCBAActivity.super.mName);
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
        List<FunctionBean> list = getFatherData(AutoPCBAActivity.super.mName);
        List<ResultModel> resultList = new ArrayList<>();
        List<PersistResultModel> persistResultList = new ArrayList<>();

        boolean isAllSuccess = true;
        for (FunctionBean bean:list){
            ResultModel model = new ResultModel();
            PersistResultModel persistModel = new PersistResultModel();

            switch (bean.getResults()){
                case 0:
                    model.setResult(Const.RESULT_NOTEST);
                    persistModel.setResult(Const.RESULT_NOTEST);
                    isAllSuccess = false;
                    break;
                case 1:
                    model.setResult(Const.RESULT_FAILURE);
                    persistModel.setResult(Const.RESULT_FAILURE);
                    isAllSuccess = false;
                    break;
                case 2:
                    model.setResult(Const.RESULT_SUCCESS);
                    persistModel.setResult(Const.RESULT_SUCCESS);
                    break;
            }

            model.setFatherName(bean.getFatherName());
            model.setSubName(bean.getSubclassName());
            model.setReason(bean.getReason());

            persistModel.setName(bean.getSubclassName());

            resultList.add(model);
            persistResultList.add(persistModel);
        }

        PersistResultModel allResultModel = new PersistResultModel();
        allResultModel.setName("pcba_all");
        allResultModel.setResult(isAllSuccess ? Const.RESULT_SUCCESS : Const.RESULT_FAILURE);
        persistResultList.add(allResultModel);

        persistResultList = DataUtil.setListOrder(config_list, persistResultList);

        writeSDCardResult(isCustomPath ? mCustomPath : mDefaultPath, mFileName,  JSON.toJSONString(resultList));
        if(SystemProperties.get("persist.sys.db_name_cit").equals("cit2_test")){
            writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE),  Const.PCBA_AUTO_RESULT_CIT2_FILE, JSON.toJSONString(persistResultList));
        }else{
            writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE),  Const.PCBA_AUTO_RESULT_FILE, JSON.toJSONString(persistResultList));
        }
        String atResultPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.LOG_PATH_DIR_AT);
        if (!TextUtils.isEmpty(atResultPath))
            writeATResult(atResultPath, Const.PCBA_AUTO_RESULT_FILE, persistResultList);
        String pcba_result = isAllSuccess?"true":"false";
        SystemProperties.set(OdmCustomedProp.getPCBAResultProp(), pcba_result);
    }

    private boolean writePersistResult(String path, String fileName, String result) {
        File persistPath = new File(Const.getLogPath(Const.TYPE_LOG_PATH_DIR));
        if(persistPath.exists() && persistPath.isDirectory()){
            File dir = FileUtil.mkDir(new File(path));
            return !"".equals(FileUtil.writeFile(dir, fileName, result));
        }
        return false;
    }

    private boolean writeSDCardResult(String path, String fileName, String result){
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)) {
            File dir = FileUtil.mkDir(FileUtil.createRootDirectory(path));
            LogUtil.d("PCBA writeSDCardResult fileName:" + fileName + " result:" + result);
            return !"".equals(FileUtil.writeFile(dir, fileName,result));
        }
        return false;
    }

    private boolean writeATResult(String path, String fileName, List<PersistResultModel> lists) {
        StringBuilder sb = new StringBuilder();
        for (PersistResultModel m : lists) {
            sb.append(m.getName());
            sb.append(":");
            if (Const.RESULT_FAILURE.equals(m.getResult()))
                sb.append("f");
            else if (Const.RESULT_NOTEST.equals(m.getResult()))
                sb.append("n");
            else if (Const.RESULT_SUCCESS.equals(m.getResult()))
                sb.append("p");
            sb.append(",");
        }
        if (sb.length() > 0)
            sb.setLength(sb.length() - 1);
        return !"".equals(FileUtil.writeFileWithPermission(
                new File(path),  Const.PCBA_AUTO_RESULT_FILE, sb.toString(), "0644"));
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
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO_SUPPORT_PROP_KEY);
        if (!TextUtils.isEmpty(temp))
            gpio_support_prop = temp;
        String lang= SystemProperties.get(gpio_support_prop);
        if(resultStatus)
            resultStr = getResources().getString(R.string.success);
        else resultStr = getResources().getString(R.string.fail);
        ProgressBar mProgressBar = (ProgressBar)mAlertDialog.findViewById(R.id.save_loading);
        TextView mMessageView = (TextView) mAlertDialog.findViewById(R.id.result_textView);
        TextView mSaveResult = (TextView) mAlertDialog.findViewById(R.id.save_result);
        TextView switchGpio = (TextView) mAlertDialog.findViewById(R.id.switch_gpio);
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
        if (resultStatus && "1".equals(lang)&& mNeedWriteGpioFlag) {
            switchGpio.setVisibility(View.VISIBLE);
            mHandler.sendEmptyMessage(2001);
        } else {
            mAlertDialog.setCancelable(true);
        }

        /*try {
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(alertDialog1);
            Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
            mMessage.setAccessible(true);
            TextView mMessageView = (TextView) mMessage.get(mAlertController);
            if(resultStatus) {
                mMessageView.setTextColor(Color.GREEN);
            }else mMessageView.setTextColor(Color.RED);
                mMessageView.setGravity(Gravity.CENTER);
                mMessageView.setTextSize(25);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }*/
    }

    private void createDialog(){
        String title = getResources().getString(R.string.pcba_test_result);
        View view = View.inflate(getApplicationContext(), R.layout.dialog_result, null);
        mAlertDialog = new AlertDialog.Builder(mContext)
                            .setTitle(title)
                            .setView(view)
                            .create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.show();

    }

    private void updateDialogGpioInfo(boolean state) {
        TextView switchGpio = (TextView) mAlertDialog.findViewById(R.id.switch_gpio);
        switchGpio.setText(state ? R.string.test_success_write_gpio_flag_success
                                 : R.string.test_success_write_gpio_flag_failed);
        switchGpio.setTextColor(state ? Color.GREEN : Color.RED);
        mAlertDialog.setCancelable(true);
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

                //if ( currPosition == Const.pcbaList.length ){
                if(currPosition == mAdapter.getItemCount()){
                    LogUtil.d("currPosition :" + currPosition);
                    LogUtil.d("mAdapter.getItemCount():" + mAdapter.getItemCount());
                    mHandler.sendEmptyMessageDelayed(1003,Const.DELAY_TIME);
                    mHandler.removeMessages(1005);
                       // mHandler.sendEmptyMessageDelayed(1004,Const.DELAY_TIME);
                        return;
                }
                if(currPosition != mAdapter.getItemCount()) {
                    //Message msg = mHandler.obtainMessage();
                    //msg.what = 1004;
                    //mHandler.sendMessage(msg);
                    LogUtil.d("mAdapter.getItemCount():" + mAdapter.getItemCount());
                    mHandler.sendEmptyMessageDelayed(1005, Const.DELAY_TIME);
                }
            }
        }
    }
}
