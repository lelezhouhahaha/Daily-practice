package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageParser;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.WifiListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import butterknife.BindView;



public class StartCustomerActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack   {
    private StartCustomerActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public  Button mFail;
    private WifiListAdapter mAdapter;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    private WifiManager wifimanager = null;
    private static final int DELAY_TIME = 1000;
    private Map<String, activityName> mCustomerConfigMap = new TreeMap<String, activityName>();
    Iterator<Map.Entry<String, activityName>> iterator = null;

    private List<ScanResult> mList = new ArrayList<>();
    IntentFilter intentFilter = null;
    //private String ACTION_START_CUSTOMER_PROCESS_START = "com.example.activity.MY_BROADCAST_START";
    private String ACTION_START_CUSTOMER_PROCESS_RESULT = "com.example.activity.MY_BROADCAST_RESULT";
    private String mCurrentPackageName = "";
    private String mCurrentClassName = "";
    private int mCount = 0;
    private boolean mStartFlag = false;

    private String TAG_LINE = "test_line";
    private String TAG_TITLE = "title";
    private String TAG_PACKAGE_NAME = "packageName";
    private String TAG_CLASS_NAME = "className";

    private int mBroadcastResult = 0;
    private String mBroadcastReason = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_startcustomer;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_customer);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        mBroadcastResult = getIntent().getIntExtra("result", 0);
        mBroadcastReason = getIntent().getStringExtra("reason");

        MyApplication.CustomerFatherName = mFatherName;
        LogUtil.d("MyApplication.CustomerFatherName:" + MyApplication.CustomerFatherName);
        if(mBroadcastResult != 0 && !mBroadcastReason.equals("") && !mBroadcastReason.isEmpty()){
            updateData(mFatherName, super.mName, mBroadcastResult, mBroadcastReason);
            LogUtil.d(" updateData mFatherName:" + mFatherName);
            LogUtil.d(" updateData super.mName:" + super.mName);
        }

        mConfigResult = getResources().getInteger(R.integer.wifi_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
            //addData(mFatherName,super.mName);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        initConfig();
        iterator  = mCustomerConfigMap.entrySet().iterator();
        mHandler.sendEmptyMessageDelayed(1006,DELAY_TIME);
        intentFilter=new IntentFilter();
        intentFilter.addAction(ACTION_START_CUSTOMER_PROCESS_RESULT);
        registerReceiver(myBroadcastReceiver,intentFilter);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE,Const.RESULT_TIMEOUT);
                    break;
                case 1006://request start opration
                    LogUtil.d("mCurrentPackageName:" + mCurrentPackageName);
                    LogUtil.d("mCurrentClassName:" + mCurrentClassName);
                    if(iterator.hasNext()) {
                        Map.Entry<String, activityName> entry = iterator.next();
                        //check current package is not active.
                        mCurrentPackageName = entry.getValue().getPackageName();
                        mCurrentClassName = entry.getValue().getClassName();
                        LogUtil.d("mCurrentPackageName:" + mCurrentPackageName + " mCurrentClassName:" + mCurrentClassName);

                        if((mCurrentPackageName != null && !mCurrentPackageName.isEmpty() && !mCurrentPackageName.equals("")) &&
                                (mCurrentClassName != null && !mCurrentClassName.isEmpty() && !mCurrentClassName.equals(""))) {
                            //addData(mFatherName,  getSubstring(entry.getKey(), "\\."));
                            addData(mFatherName,  entry.getKey());
                            Intent i = new Intent(Intent.ACTION_MAIN);
                            ComponentName component = new ComponentName(mCurrentPackageName, mCurrentClassName);
                            i.setComponent(component);
                            mContext.startActivity(i);
                        }
                    }else {
                        mHandler.sendEmptyMessage(1001);
                    }
                    break;
                case 1007://check current package is active.
                    if(DataUtil.isForegroundPackage(mContext, mCurrentPackageName)) {
                        LogUtil.d("04 mCount:" + mCount);
                        if (mCount < 3)
                            mHandler.sendEmptyMessageDelayed(1007, DELAY_TIME);
                        else {
                            LogUtil.d("05 mCurrentPackageName:" + mCurrentPackageName);
                            DataUtil.stopActivity(mContext, mCurrentPackageName);
                            mCount = 0;
                            LogUtil.d("05 mCount:" + mCount);
                            mHandler.sendEmptyMessageDelayed(1007, DELAY_TIME);
                        }
                        mCount++;
                    } else mHandler.sendEmptyMessage(1006);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    private void initConfig() {
        File configFile = new File(Const.CIT_CUSTOMER_CONFIG_PATH);
        if(!configFile.exists()){
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found,Const.CIT_CUSTOMER_CONFIG_PATH));
            deInit(mFatherName, 0);//update state to no test
            finish();
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(Const.CIT_CUSTOMER_CONFIG_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            String packageName = null;
            String className = null;
            LinearLayout testLineLayout = null;

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if (TAG_PACKAGE_NAME.equals(startTagName)) {
                            packageName = xmlPullParser.nextText();
                        } else if (TAG_CLASS_NAME.equals(startTagName)) {
                            className = xmlPullParser.nextText();
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String endTagName = xmlPullParser.getName();
                        if (TAG_LINE.equals(endTagName)) {
                            LogUtil.d("packageName:" + packageName + " className:" + className);
                            mCustomerConfigMap.put(className, new activityName(packageName, className));
                        }
                }
                type = xmlPullParser.next();
            }
        }  catch (Exception e) {
            LogUtil.d("====>error = " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(myBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }

    class activityName extends  Object {
        protected String _packageName = null;
        protected String _className = null;

        activityName(String pkg, String cls) {
            _packageName = pkg;
            _className = cls;
        }

        public String getPackageName(){
            return _packageName;
        }

        public String getClassName(){
            return _className;
        }
    };

    public BroadcastReceiver myBroadcastReceiver = new  BroadcastReceiver(){
        String ACTION_START_CUSTOMER_PROCESS_RESULT = "com.example.activity.MY_BROADCAST_RESULT";
        @Override
        public void onReceive(Context context, Intent intent) {
            //接收到广播的处理，注意不能有耗时操作，当此方法长时间未结束，会报错。
            //同时，广播接收器中不能开线程。
            LogUtil.d("intent.getAction():" + intent.getAction());
           if(intent.getAction().equals(ACTION_START_CUSTOMER_PROCESS_RESULT)) {
                String className = intent.getStringExtra("className");
                String resultStr = intent.getStringExtra("result");
                int result = Integer.parseInt(resultStr);
                String reason = intent.getStringExtra("reason");

               if((result > 2 || result < 0) || (reason.length() > 1048 || reason.isEmpty())) {
                    LogUtil.d("The broadcast data format does not meet the requirements.");
               }else{
                   updateData(mFatherName, className, result, reason);
                   mHandler.sendEmptyMessage(1007);
               }
            }
        }
    };
}
