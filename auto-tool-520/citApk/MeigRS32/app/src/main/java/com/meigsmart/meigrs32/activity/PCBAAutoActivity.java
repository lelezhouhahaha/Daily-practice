package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Adapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.PersistResultModel;
import com.meigsmart.meigrs32.model.ResultModel;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.client.MyDiagAutoTestClient;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.DiagCommand;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.util.PreferencesUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;

public class PCBAAutoActivity extends BaseActivity implements View.OnClickListener , CheckListAdapter.OnCallBackCheckFunction {
    private PCBAAutoActivity mContext;
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
    private final String TAG = PCBAAutoActivity.class.getSimpleName();

	private AlertDialog mAlertDialog = null;

    private String projectName = "";
    private String scanType = "";
    private boolean OemScanConnected = false;
    private static int moreClickTimes;
    public MyDiagAutoTestClient mDiagClient = null;
    public MyHandler mHandler = null;
    private Class mCurrentTestClass = null;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_pcba;
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        LogUtil.d(TAG, "onNewIntent finish current activity!");
        mContext.finish();
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        moreClickTimes =0;
       // mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mMore.setSelected(isLayout);
        mTitle.setText(R.string.function_pcba_auto);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

        mDefaultPath = getResources().getString(R.string.pcba_save_log_default_path);
        mFileName = getResources().getString(R.string.pcba_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.pcba_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.pcba_save_log_custom_path);
        LogUtil.d("mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);


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

        List<String> config = getPcbaAutoTestConfig();
        Log.d(TAG, "config:<" + config + ">.");
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        if (list.size()>10)mMore.setVisibility(View.VISIBLE);
        mAdapter.setData(list);
        mHandler = new MyHandler(mContext);
        mDiagClient = new MyDiagAutoTestClient(mContext, mHandler);
        //mHandler.sendEmptyMessageDelayed(1005,100);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MyDiagAutoTestClient.SERVICEID);
        //mHandler.removeMessages(MyDiagAutoTestClient.ACK_ACTIVITYID);
        mHandler.removeMessages(MyDiagAutoTestClient.ACK_SAY_HELLO);
        mDiagClient.Destroy(mContext);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.d(TAG, "requestCode:" + requestCode + " resultCode:" + resultCode);
        if (data!=null){
            int results = data.getIntExtra("results",0);
            String reason = data.getStringExtra("reason");
            if(reason == null){
                reason = "";
                LogUtil.d(TAG, "get fail reason is null");
            }
            LogUtil.d(TAG, "test results:" + results + " requestCode:" + requestCode + " reason:" + reason);
            int mDiagCmdId = getAutoTestDiagCommandId(requestCode);
            Class cls = getClass(PreferencesUtil.getStringData(mContext, "class_" + mDiagCmdId));

            if(checkCurrenTestActivity(cls, mCurrentTestClass)){
                updateDataForUI(cls.getSimpleName(), results);
            }
            if(results == SUCCESS){
                mDiagClient.doSendResultMessage(MyDiagAutoTestClient.ACK_SERVICEID, requestCode, results, null, 0);
            }else {
                mDiagClient.doSendResultMessage(MyDiagAutoTestClient.ACK_SERVICEID, requestCode, results, reason, reason.length());
            }

        }
    }

    private void updateDataForUI(String clsName, int result){
        List<TypeModel> mData = mAdapter.getData();
        int size = mData.size();

        for(int i = 0; i < size; i++){
            TypeModel mod = mData.get(i);
           if(clsName.equals(mod.getCls().getSimpleName())){
               mData.get(i).setType(result);
               break;
           }
        }
        mAdapter.notifyDataSetChanged();
    }

    private boolean checkCurrenTestActivity(Class mCurrentCls, Class mCurrentStartCls){
            String mCurrentClassName = mCurrentCls.getSimpleName();
            if(mCurrentClassName.equals(mCurrentStartCls.getSimpleName())){
                return true;
            }
            return false;
    }

    private static class MyHandler extends Handler {
        WeakReference<Activity> reference;
        public MyHandler(Activity activity) {
            reference = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PCBAAutoActivity activity = (PCBAAutoActivity) reference.get();
            switch (msg.arg1) {
                case MyDiagAutoTestClient.SERVICEID:
                    Log.d(activity.TAG, "ACTIVITYID 服务端传来了消息=====>>>>>>>");
                    int mDiagCommandId = msg.getData().getInt(DiagCommand.FTM_SUBCMD_CMD_KEY);
                    Log.d(activity.TAG, "zll mCmmdContent:" + mDiagCommandId);
                    /*if(mCmmdContent.isEmpty()){
                        String reason = "Diag Command is empty!";
                        activity.mDiagClient.doSendMessage(MyDiagAutoTestClient.ACK_SERVICEID, mCmmdContent, reason, reason.length());
                    }*/
                    activity.startActivityDependOnCommandId(mDiagCommandId);
                    //activity.mDiagClient.doSendLocalMessage(MyDiagAutoTestClient.ACK_ACTIVITYID, "SOFTWAREINFO:PASS");
                    break;
                /*case MyDiagAutoTestClient.ACK_ACTIVITYID:
                    String mDataSendToService = (String) msg.getData().get("content");
                    activity.mDiagClient.doSendMessage(msg, MyDiagAutoTestClient.ACK_SERVICEID, mDataSendToService);
                    break;*/
                case MyDiagAutoTestClient.ACK_SAY_HELLO:
                    //客户端接受服务端传来的消息
                    Log.d(activity.TAG, "ACK_SAY_HELLO 服务端传来了消息=====>>>>>>>");
                    String str = (String) msg.getData().get("content");
                    Log.d(activity.TAG, str);
                    break;
            }
        }
    }

    private int getAutoTestDiagCommandId(int mCmdId){
        int mDiagCmdId = 0;
        if( ( mCmdId < DiagCommand.FTM_SUBCMD_QUERY_BASE ) && ( mCmdId > DiagCommand.FTM_SUBCMD_BASE ) ){
            mDiagCmdId = mCmdId - DiagCommand.FTM_SUBCMD_BASE;
        }
        return mDiagCmdId;
    }

    private Class getClass(String className){
        String clsName = "";
        if(className.contains("/")) {
            //pkgName = className.substring(0, className.indexOf("/"));
            //clsName = pkgName + className.substring(className.indexOf("/") + 1);
            clsName = className.replace("/", "");
        }else if(className.contains("*")){
            //pkgName = className.substring(0, className.indexOf("*"));
            clsName = className.substring(className.indexOf("*")+1);
        }else{
            clsName = "com.meigsmart.meigrs32.activity." + className;
        }

        Class cls = null;
        try {
            cls = Class.forName(clsName);
        } catch (ClassNotFoundException e) {
            LogUtil.d("not found class " + clsName);
        }
        return cls;
    }

    private void startActivityDependOnCommandId(int mDiagCommandId){
        int mDiagCmdId = getAutoTestDiagCommandId(mDiagCommandId);

        String title = "";
        String clsName = "";
        //String pkgName = "";
        Class cls = null;
        String className = PreferencesUtil.getStringData(mContext, "class_" + mDiagCmdId);
        LogUtil.d(TAG, "className:" + className);
        if(className.contains("/")) {
            //pkgName = className.substring(0, className.indexOf("/"));
            //clsName = pkgName + className.substring(className.indexOf("/") + 1);
            clsName = className.replace("/", "");
            title = getStringFromName(mContext, clsName);
        }else if(className.contains("*")){
            //pkgName = className.substring(0, className.indexOf("*"));
            clsName = className.substring(className.indexOf("*")+1);
            title = getStringFromName(mContext, clsName);
        }else{
            title = getStringFromName(mContext, className);
            clsName = "com.meigsmart.meigrs32.activity." + className;
        }

        LogUtil.d(TAG, "zll clsName:" + clsName + " title:" + title);
        //cls = Class.forName(clsName);
        try {
            cls = Class.forName(clsName);
        } catch (ClassNotFoundException e) {
            LogUtil.d("not found class " + clsName);
        }
        LogUtil.d(TAG, "zll get cls end super.mName:" + super.mName + " cls.getSimpleName():" + cls.getSimpleName());

        Intent intent = new Intent(mContext, cls);
        mCurrentTestClass = cls;
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if(!(this.mName==null)){
            intent.putExtra("fatherName",super.mName);
            intent.putExtra("name", SAVE_EN_LOG ? cls.getSimpleName() : title);
        }else{
            intent.putExtra("fatherName",mFatherName);
            intent.putExtra("name", SAVE_EN_LOG ? cls.getSimpleName() : title);
        }
        startActivityForResult(intent, mDiagCommandId);
    }


    private List<String> removeDuplicate(List<String> list)
    {
        Set set = new LinkedHashSet<String>();
        set.addAll(list);
        list.clear();
        list.addAll(set);
        return list;
    }

    public List<String> getPcbaAutoTestConfig() {
        String TAG_CONFIG = "pcba_auto_test_config";
        String TAG_LINE = "test_line";
        String TAG_ITEM_CMMD_ID_CONFIG = "item_cmd_id";
        String TAG_ITEM_CLASS_CONFIG = "item_class";
        List<String> config = new ArrayList<String>();
        File configFile = new File(Const.PCBA_AUTO_TEST_CONFIG_XML_PATH);
        if (!configFile.exists()) {
            //ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.PCBA_AUTO_TEST_CONFIG_XML_PATH));
            //deInit(mFatherName, NOTEST);//update state to no test
            //finish();
            return null;
        }
        try {
            InputStream inputStream = new FileInputStream(Const.PCBA_AUTO_TEST_CONFIG_XML_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            int type = xmlPullParser.getEventType();
            String mCmdId = "";
            String mClassName = "";
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if(TAG_LINE.equals(startTagName)) {
                            /*if(!mCmdId.isEmpty() && !mClassName.isEmpty()){
                                //PreferencesUtil.setStringData(mContext, "class_"+mCmdId, mClassName));
                                //Log.d(TAG, "class_"+mCmdId + " :" + PreferencesUtil.getStringData(mContext, "class_"+mCmdId))
                                config.add(mClassName);
                                mClassName = "";
                                mCmdId = "";
                                LogUtil.d("PCBAAutoActivity", " TAG_LINE");
                            }else{
                                LogUtil.d("PCBAAutoActivity", " TAG_LINE 1");
                            }*/
                        }else if(TAG_ITEM_CMMD_ID_CONFIG.equals(startTagName)){
                            mCmdId = xmlPullParser.nextText();
                            LogUtil.d("PCBAAutoActivity", " mCmdId:" + mCmdId);
                        }else if(TAG_ITEM_CLASS_CONFIG.equals(startTagName)){
                            mClassName = xmlPullParser.nextText();
                            LogUtil.d("PCBAAutoActivity", " mClassName:" + mClassName);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String endTagName = xmlPullParser.getName();
                        if(TAG_LINE.equals(endTagName)){
                            if(!mCmdId.isEmpty() && !mClassName.isEmpty()){
                                PreferencesUtil.setStringData(mContext, "class_"+mCmdId, mClassName);
                                //Log.d(TAG, "class_"+mCmdId + " :" + PreferencesUtil.getStringData(mContext, "class_"+mCmdId))
                                config.add(mClassName);
                                mClassName = "";
                                mCmdId = "";
                                LogUtil.d("PCBAAutoActivity", " TAG_LINE end");
                            }
                        }else if(TAG_CONFIG.equals(endTagName)) {
                            //LogUtil.d("PCBAAutoActivity", "tag end");
                            return config;
                        }
                }
                type = xmlPullParser.next();
            }
        }catch (Exception e) {
            //setTestFailReason("Read file:" + Const.PCBA_AUTO_TEST_CONFIG_XML_PATH + " abnormal!");
            Log.d("PCBAAutoActivity", "Read file:" + Const.PCBA_AUTO_TEST_CONFIG_XML_PATH + " abnormal!");
            //e.printStackTrace();
        }
        return null;
    }

    /*private boolean isAllSuccess() {
        List<FunctionBean> list = getFatherData(PCBAAutoActivity.super.mName);
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
    }*/

    private void saveLog() {
        List<FunctionBean> list = getFatherData(PCBAAutoActivity.super.mName);
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
        allResultModel.setName("pcba_auto_all");
        allResultModel.setResult(isAllSuccess ? Const.RESULT_SUCCESS : Const.RESULT_FAILURE);
        persistResultList.add(allResultModel);

        //persistResultList = DataUtil.setListOrder(config_list, persistResultList);

       /* writeSDCardResult(isCustomPath ? mCustomPath : mDefaultPath, mFileName,  JSON.toJSONString(resultList));
        if(SystemProperties.get("persist.sys.db_name_cit").equals("cit2_test")){
            writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE),  Const.PCBA_AUTO_RESULT_CIT2_FILE, JSON.toJSONString(persistResultList));
        }else{
            writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE),  Const.PCBA_AUTO_RESULT_FILE, JSON.toJSONString(persistResultList));
        }
        String atResultPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.LOG_PATH_DIR_AT);
        if (!TextUtils.isEmpty(atResultPath))
            writeATResult(atResultPath, Const.PCBA_AUTO_RESULT_FILE, persistResultList);
        */
        String pcba_result = isAllSuccess?"true":"false";
        SystemProperties.set(OdmCustomedProp.getPCBAResultProp(), pcba_result);
    }

   /* private boolean writePersistResult(String path, String fileName, String result) {
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
    }*/

        private class SaveResult extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
           //createDialog();
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
        boolean resultStatus = false;//isAllSuccess();
        /*String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO_SUPPORT_PROP_KEY);
        if (!TextUtils.isEmpty(temp))
            gpio_support_prop = temp;
        String lang= SystemProperties.get(gpio_support_prop);
         */
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
        /*if (resultStatus && "1".equals(lang)&& mNeedWriteGpioFlag) {
            switchGpio.setVisibility(View.VISIBLE);
            mHandler.sendEmptyMessage(2001);
        } else */{
            mAlertDialog.setCancelable(true);
        }
    }

    private void createDialog(){
        String title = getResources().getString(R.string.pcba_auto_test_result);
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


}
