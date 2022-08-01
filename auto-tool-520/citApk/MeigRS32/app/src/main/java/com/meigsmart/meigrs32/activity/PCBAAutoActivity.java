package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
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
    private Intent mSaveStartItemIntent = null;
    private int mSaveStartCmmdId = 0;
   // private final String TAG = this.getClass().getSimpleName();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_pcba;
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        boolean flag = intent.getBooleanExtra("finish", false);
        if(flag) {
            LogUtil.d(TAG, "onNewIntent finish current activity!");
            mContext.finish();
        }
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false/*true*/;
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

        List<String> mTmpConfig = getPcbaAutoTestConfig();
        List<String> config =  removeDuplicate(mTmpConfig);
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
        mHandler.removeMessages(DiagCommand.SERVICEID);
        //mHandler.removeMessages(MyDiagAutoTestClient.ACK_ACTIVITYID);
        mHandler.removeMessages(DiagCommand.ACK_SAY_HELLO);
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
            if(isToolAutoJudge(requestCode)){
                LogUtil.d(TAG, "not return result");
                return;
            }

            if(results == SUCCESS){
                mDiagClient.doSendResultMessage(DiagCommand.ACK_SERVICEID, requestCode, results, null, 0);
            }else {
                mDiagClient.doSendResultMessage(DiagCommand.ACK_SERVICEID, requestCode, results, reason, reason.length());
            }

            //update Data UI
            updateDataForUI(requestCode, results);
        }
    }

    private void updateDataForUI(int mDiagCommandId, int results){
        int mMatchIdex = -1;
        mMatchIdex = getMatchData(mDiagCommandId);
        if(mMatchIdex == -1){
            LogUtil.d(TAG, "get Match Adapter data is null");
            return;
        }
        mAdapter.getData().get(mMatchIdex).setType(results);
        mAdapter.notifyDataSetChanged();
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
                case DiagCommand.SERVICEID:
                    Log.d(activity.TAG, "ACTIVITYID 服务端传来了消息=====>>>>>>>");
                    int mDiagCommandId = msg.getData().getInt(DiagCommand.FTM_SUBCMD_CMD_KEY);
                    Log.d(activity.TAG, "zll mCmmdContent:" + mDiagCommandId);
                    if(activity.mSaveStartItemIntent != null){
                        Log.d(activity.TAG, "activity.mSaveStartItemIntent.getClass():" + activity.mSaveStartItemIntent.getClass());
                        Log.d(activity.TAG, "activity.mSaveStartItemIntent.getComponent().getClassName():" + activity.mSaveStartItemIntent.getComponent().getClassName());

                        String mClassName = activity.mSaveStartItemIntent.getComponent().getClassName();
                        String mClassName2 = activity.mSaveStartItemIntent.getStringExtra("className");  //start third party apk
                        Log.d(activity.TAG, "mClassName:" + mClassName);
                        Log.d(activity.TAG, "mClassName2:" + mClassName2);
                        if(activity.isToolAutoJudgementTestRunning(activity.mContext, mClassName) || activity.isToolAutoJudgementTestRunning(activity.mContext, mClassName2)) {
                            activity.mSaveStartItemIntent.putExtra("finish", true);
                            activity.startActivityForResult(activity.mSaveStartItemIntent, mDiagCommandId);
                        }
                    }
                    int ret = activity.startActivityDependOnCommandId(mDiagCommandId);
                    if(ret == -1){
                        String returnData = "there is no test item.";
                        activity.mDiagClient.doSendResultMessage(DiagCommand.ACK_SERVICEID, mDiagCommandId, 0, returnData, returnData.length());
                    }
                    break;
                case DiagCommand.SERVICEID_SET_RESULT: {
                    int mDiagCmmdId = msg.getData().getInt(DiagCommand.FTM_SUBCMD_CMD_KEY);
                    int mResult = msg.getData().getInt(DiagCommand.FTM_SUBCMD_RESULT_KEY);
                    String mData = (String) msg.getData().get(DiagCommand.FTM_SUBCMD_DATA_KEY);
                    int mDataSize = msg.getData().getInt(DiagCommand.FTM_SUBCMD_DATA_SIZE_KEY);
                    LogUtil.d(activity.TAG, "mDiagCmmdId: " + mDiagCmmdId + " mResult:" + mResult + " mData:" + mData + " mDataSize:" + mDataSize);
                    int mMatchIdx = activity.getMatchData(mDiagCmmdId);
                    {
                        TypeModel model = activity.mAdapter.getData().get(mMatchIdx);
                        String mFatherName = activity.mName;
                        String mName = SAVE_EN_LOG ? model.getCls().getSimpleName() : model.getName();
                        LogUtil.d(activity.TAG, "mData:"+ mData);
                        LogUtil.d(activity.TAG, "mDataSize:"+ mDataSize +  " mResult:" + mResult + " mDiagCmmdId:" + mDiagCmmdId);
                        LogUtil.d(activity.TAG, "mFatherName:"+ mFatherName + " mName:" + mName);
                        activity.addData(mFatherName, mName);   //if not exist, create it, if exist, return;
                        if( ( mData == null ) || (mDataSize <= 1)){
                            activity.updateData(mFatherName, mName, mResult, "");
                        }else activity.updateData(mFatherName, mName, mResult, mData);
                        //update result UI
                        activity.updateDataForUI(mDiagCmmdId, mResult);
                        //send ack
                        activity.mDiagClient.doSendResultMessage(DiagCommand.ACK_SERVICEID_SET_RESULT, mDiagCmmdId, activity.SUCCESS, null, 0);
                    }
                }
                    break;
                case DiagCommand.ACK_SAY_HELLO:
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
        }else if( ( mCmdId < DiagCommand.FTM_SUBCMD_SET_RESULT_BASE ) && ( mCmdId > DiagCommand.FTM_SUBCMD_QUERY_BASE ) ){
            mDiagCmdId = mCmdId - DiagCommand.FTM_SUBCMD_QUERY_BASE;
        }else {
            mDiagCmdId = mCmdId - DiagCommand.FTM_SUBCMD_SET_RESULT_BASE;
        }
        return mDiagCmdId;
    }

    private String getTitleName(String str_className){
        String mTitle = "";
        String mClassName = "";
        if(str_className.contains("/")) {
            mClassName = str_className.replace("/", "");
            mTitle = getStringFromName(mContext, mClassName);
        }else if(str_className.contains("*")){
            mClassName = str_className.substring(str_className.indexOf("*")+1);
            mTitle = getStringFromName(mContext, mClassName);
        }else{
            mTitle = getStringFromName(mContext, str_className);
            //mClassName = "com.meigsmart.meigrs32.activity." + str_className;
        }
        return mTitle;
    }

    private boolean isToolAutoJudgementTestRunning(Context context, String mClassName) {
        if( ( mClassName == null ) || ( mClassName.isEmpty() ) ){
            Log.d(TAG, "isToolAutoJudgementTestRunning ( mClassName == null ) || ( mClassName.isEmpty() )");
            return false;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if(list == null){
            Log.d(TAG, "isToolAutoJudgementTestRunning list == null");
            return false;
        }
        ActivityManager.RunningTaskInfo info = list.get(0);
        Log.d(TAG, "isToolAutoJudgementTestRunning mClassName:" + mClassName);
        Log.d(TAG, "isToolAutoJudgementTestRunning info.topActivity.getClassName():" + info.topActivity.getClassName());
        if ( mClassName.equals(info.topActivity.getClassName() ) ) {
            //find it, break
            Log.d(TAG, "isToolAutoJudgementTestRunning is true mClassName:" + mClassName);
            return true;
        }
        return false;
    }

    private Class getClass(String className){
        Class cls = null;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            LogUtil.d("not found class " + className);
        }
        return cls;
    }

    public int getMatchData(int mDiagCommandId){
        String title = "";
        String className = "";
        int mDiagCmdId = -1;
        mDiagCmdId = getAutoTestDiagCommandId(mDiagCommandId);
        className = PreferencesUtil.getStringData(mContext, "class_" + mDiagCmdId);
        title = getTitleName(className);
        LogUtil.d(TAG, "className:" + className +  " title:" + title);

        List<TypeModel> mData = mAdapter.getData();
        int mDataSize = mData.size();
        for(int i = 0; i < mDataSize; i++){
            String mTitleName = mData.get(i).getName();
            LogUtil.d(TAG, "mTitleName:" + mTitleName);
            if(!mTitleName.isEmpty() && mTitleName.equals(title)){
                return i;
            }
        }
        return -1;
    }

    private int startActivityDependOnCommandId(int mDiagCommandId){
        int mMatchIdex = getMatchData(mDiagCommandId);
        if(mMatchIdex == -1){
            LogUtil.d(TAG, "get Match Data idex is -1");
            return -1;
        }
        LogUtil.d(TAG, "mMatchIdex:" +mMatchIdex);
        TypeModel model = mAdapter.getData().get(mMatchIdex);
        if (model.getCls() != null){
            Intent intent = new Intent(this,model.getCls());
            intent.putExtra("StartType", "pcbaautotest");
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if(!(this.mName==null)){
                intent.putExtra("fatherName",this.mName);
                intent.putExtra("name", SAVE_EN_LOG ? model.getCls().getSimpleName() : model.getName());
            }else{
                intent.putExtra("fatherName",mFatherName);
                intent.putExtra("name", SAVE_EN_LOG ? model.getCls().getSimpleName() : model.getName());
            }
            if(model.getStartType() == 1){
                LogUtil.d("citapk 1");
                intent.putExtra("packageName", model.getPackageName());
                intent.putExtra("className", model.getClassName());
            }
            LogUtil.d(TAG, "zll mDiagCommandId:" + mDiagCommandId);
            int mDiagCmdId = getAutoTestDiagCommandId(mDiagCommandId);
            LogUtil.d(TAG, "zll mDiagCmdId:" + mDiagCmdId);

            switch (mDiagCmdId){
                case DiagCommand.FTM_SUBCMD_BACKLIGHT50:
                    intent.putExtra("backlight", "50");
                    break;
                case DiagCommand.FTM_SUBCMD_BACKLIGHT100:
                    intent.putExtra("backlight", "100");
                    break;
                case DiagCommand.FTM_SUBCMD_BACKLIGHT150:
                    intent.putExtra("backlight", "150");
                    break;
                case DiagCommand.FTM_SUBCMD_LEDRED:
                    Log.d(TAG, "zll DiagCommand.FTM_SUBCMD_LEDRED:" + DiagCommand.FTM_SUBCMD_LEDRED);
                    intent.putExtra("ledname", "Red");
                    break;
                case DiagCommand.FTM_SUBCMD_LEDGREEN:
                    Log.d(TAG, "zll DiagCommand.FTM_SUBCMD_LEDGREEN:" + DiagCommand.FTM_SUBCMD_LEDGREEN);
                    intent.putExtra("ledname", "Green");
                    break;
                case DiagCommand.FTM_SUBCMD_LEDBLUE:
                    Log.d(TAG, "zll DiagCommand.FTM_SUBCMD_LEDBLUE:" + DiagCommand.FTM_SUBCMD_LEDBLUE);
                    intent.putExtra("ledname", "Blue");
                    break;
                case DiagCommand.FTM_SUBCMD_LCDRED:
                    intent.putExtra("lcd", "0");
                    break;
                case DiagCommand.FTM_SUBCMD_LCDGREEN:
                    intent.putExtra("lcd", "1");
                    break;
                case DiagCommand.FTM_SUBCMD_LCDBLUE:
                    intent.putExtra("lcd", "2");
                    break;
                case DiagCommand.FTM_SUBCMD_LCDGRAY:
                    intent.putExtra("lcd", "3");
                    break;
                case DiagCommand.FTM_SUBCMD_LCDBLACK:
                    intent.putExtra("lcd", "4");
                    break;
                case DiagCommand.FTM_SUBCMD_LCDWHITE:
                    intent.putExtra("lcd", "5");
                    break;
                case DiagCommand.FTM_SUBCMD_HEADSETLEFTLOOP:
                    intent.putExtra("soundchannel", "left");
                    break;
                case DiagCommand.FTM_SUBCMD_HEADSETRIGHTLOOP:
                    intent.putExtra("soundchannel", "right");
                    break;
                default:
                    break;
            }
            if(isToolAutoJudge(mDiagCommandId)){
                mSaveStartItemIntent = intent;
                mSaveStartCmmdId = mDiagCommandId;
            }else {
                mSaveStartItemIntent = null;
                mSaveStartCmmdId = 0;
            }
            startActivityForResult(intent,mDiagCommandId);
        }else{
            LogUtil.d(TAG, "model.getCls() == null model.getClassName():" + model.getClassName());
            LogUtil.d(TAG, "model.getCls() == null model.getPackageName():" + model.getPackageName());
            LogUtil.d(TAG, "model.getCls() == null model.getStartType():" + model.getStartType());
            ComponentName componentName = new ComponentName(model.getPackageName(), model.getClassName());
            //ComponentName componentName = new ComponentName(mPackageName, mClassName);
            Intent intent = new Intent();
            intent.setComponent(componentName);
            if(!(this.mName==null)){
                intent.putExtra("fatherName",this.mName);
                intent.putExtra("name", SAVE_EN_LOG ? model.getCls().getSimpleName() : model.getName());
            }else{
                intent.putExtra("fatherName",mFatherName);
                intent.putExtra("name", SAVE_EN_LOG ? model.getCls().getSimpleName() : model.getName());
            }
            if(model.getStartType() == 1){
                LogUtil.d("citapk 1");
                intent.putExtra("packageName", model.getPackageName());
                intent.putExtra("className", model.getClassName());
            }
            if(isToolAutoJudge(mDiagCommandId)){
                mSaveStartItemIntent = intent;
                mSaveStartCmmdId = mDiagCommandId;
            }else {
                mSaveStartItemIntent = null;
                mSaveStartCmmdId = 0;
            }
            startActivityForResult(intent,mDiagCommandId);
        }
        return 0;
    }

    private boolean isToolAutoJudge(int cmdId){
        if( ( cmdId >= ( DiagCommand.FTM_SUBCMD_HEADSET + DiagCommand.FTM_SUBCMD_BASE ) ) && ( cmdId <= ( DiagCommand.FTM_SUBCMD_RECEIVER + DiagCommand.FTM_SUBCMD_BASE ) ) )
            return true;
        return false;
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
            Log.d("PCBAAutoActivity", "Read file:" + Const.PCBA_AUTO_TEST_CONFIG_XML_PATH + " abnormal!");
        }
        return null;
    }
}
